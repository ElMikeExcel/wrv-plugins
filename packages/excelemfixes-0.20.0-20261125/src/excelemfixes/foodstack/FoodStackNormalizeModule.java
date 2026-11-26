package excelemfixes.foodstack;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Food Stack Normalize module.
 *
 * <p>The defect (player report: Jester, Nov 24, 2026 — his
 * Fresh Rats sat in four piles that would not merge): EliteMobs'
 * Advanced Combat system rewrites food items while a player is
 * in advanced combat — it backs the item's real food values up
 * into {@code elitemobs:advanced_combat_food*} data keys and
 * swaps in a combat form (always-edible, slower to eat). It is
 * meant to restore every item when combat ends, and it does
 * restore the ones its own passes touch — but copies that slip
 * past those passes (stored in a chest mid-combat, saved in a
 * logout, picked up at the wrong moment) keep the combat form
 * forever. Minecraft stacks only identical items, so one food
 * becomes two permanent piles.
 *
 * <p>This module finishes the restore EliteMobs started. Any
 * stack still carrying EliteMobs' combat-food stamp is
 * returned to its resting form — the exact transformation
 * EliteMobs' own restore performs, backup values and all (see
 * {@link RestorePlanner}) — at the points a stack changes hands
 * or comes to rest: when it appears in the world, when a player
 * picks it up, when a player joins, and when a container is
 * closed. After a sweep, identical copies in the same inventory
 * are merged into full stacks. Unstamped stacks are never
 * touched, and nothing about the food itself changes: the
 * stamp only ever held a copy of the item's own values, so a
 * normalized copy nourishes exactly as much as a stamped one.
 *
 * <p>Self-contained: the module keys only on EliteMobs' stamp
 * (present on any food EliteMobs adapted — custom or vanilla),
 * needs no EliteMobs API, and changes no EliteMobs file.
 */
public final class FoodStackNormalizeModule implements FixModule, Listener {

    private static final Set<InventoryType> CONTAINER_TYPES = Set.of(
            InventoryType.CHEST, InventoryType.ENDER_CHEST,
            InventoryType.BARREL, InventoryType.SHULKER_BOX,
            InventoryType.HOPPER, InventoryType.DROPPER,
            InventoryType.DISPENSER, InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE, InventoryType.SMOKER);

    private ModuleContext ctx;
    private BukkitTask joinTask;

    private boolean onPickup = true;
    private boolean onSpawn = true;
    private boolean onJoin = true;
    private long joinDelayTicks = 40;
    private boolean onClose = true;
    private boolean mergeStacks = true;
    private boolean debug = false;

    private NamespacedKey keyMarker;
    private NamespacedKey keyDefault;
    private NamespacedKey keyNutrition;
    private NamespacedKey keySaturation;
    private NamespacedKey keyAlwaysEdible;
    private NamespacedKey keyConsumption;

    @Override
    public String id() {
        return "food-stack-normalize";
    }

    @Override
    public String displayName() {
        return "Food Stack Normalize";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        keyMarker = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_MARKER);
        keyDefault = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_DEFAULT);
        keyNutrition = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_NUTRITION);
        keySaturation = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_SATURATION);
        keyAlwaysEdible = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_ALWAYS_EDIBLE);
        keyConsumption = new NamespacedKey(StampView.NAMESPACE, StampView.KEY_CONSUMPTION_SECONDS);
        reload();
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        if (onJoin) {
            // Cover a reload with players already online.
            joinTask = Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    sweepPlayer(p);
                }
            }, Math.max(1, joinDelayTicks));
        }
        ctx.logger().info("Food Stack Normalize ready — stamped EliteMobs food "
                + "returns to its resting form on pickup/join/container close.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (joinTask != null) {
            joinTask.cancel();
            joinTask = null;
        }
    }

    @Override
    public void reload() {
        ConfigurationSection cfg = ctx.moduleConfig();
        if (cfg == null) {
            return;
        }
        onPickup = cfg.getBoolean("normalize-on-pickup", true);
        onSpawn = cfg.getBoolean("normalize-ground-items", true);
        onJoin = cfg.getBoolean("sweep-on-join", true);
        joinDelayTicks = cfg.getLong("join-sweep-delay-ticks", 40);
        onClose = cfg.getBoolean("sweep-on-inventory-close", true);
        mergeStacks = cfg.getBoolean("merge-stacks", true);
        debug = cfg.getBoolean("debug", false);
    }

    // ------------------------------------------------ events

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!onSpawn) {
            return;
        }
        Item entity = event.getEntity();
        ItemStack normalized = normalize(entity.getItemStack());
        if (normalized != null) {
            entity.setItemStack(normalized);
            debug("normalized a stamped ground item: "
                    + normalized.getType() + " x" + normalized.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!onPickup || !(event.getEntity() instanceof Player)) {
            return;
        }
        Item entity = event.getItem();
        ItemStack normalized = normalize(entity.getItemStack());
        if (normalized != null) {
            entity.setItemStack(normalized);
            debug("normalized a stamped pickup: "
                    + normalized.getType() + " x" + normalized.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!onJoin) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(ctx.plugin(),
                () -> {
                    if (player.isOnline()) {
                        sweepPlayer(player);
                    }
                }, Math.max(1, joinDelayTicks));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!onClose || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getInventory();
        if (top != null && CONTAINER_TYPES.contains(top.getType())) {
            int changed = sweepSlots(top.getContents(), top::setItem);
            if (changed > 0) {
                debug("container close sweep for " + player.getName()
                        + ": " + changed + " stamped stack(s) normalized in "
                        + top.getType());
            }
        }
        sweepPlayer(player);
    }

    // ------------------------------------------------ sweeps

    private void sweepPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        int changed = sweepSlots(inv.getStorageContents(), inv::setItem);
        int ender = sweepSlots(player.getEnderChest().getContents(),
                player.getEnderChest()::setItem);
        if (debug && changed + ender > 0) {
            debug("join/close sweep for " + player.getName() + ": "
                    + changed + " stamped stack(s) in inventory, "
                    + ender + " in ender chest");
        }
    }

    private interface SlotWriter {
        void set(int slot, ItemStack stack);
    }

    /**
     * Normalize every stamped stack in the given contents and
     * (when enabled) merge identical stacks. Returns how many
     * stacks carried the stamp. The writer applies one slot;
     * contents are read once and written back per changed slot.
     */
    private int sweepSlots(ItemStack[] contents, SlotWriter writer) {
        int n = contents.length;
        ItemStack[] slots = new ItemStack[n];
        int stamped = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType().isAir()) {
                slots[i] = null;
                continue;
            }
            ItemStack normalized = normalize(s);
            if (normalized != null) {
                slots[i] = normalized;
                stamped++;
            } else {
                slots[i] = s;
            }
        }
        if (stamped == 0 && !mergeStacks) {
            return 0;
        }
        int[] amounts = new int[n];
        if (mergeStacks) {
            List<ItemStack> representatives = new ArrayList<>();
            int[] groups = new int[n];
            int[] max = new int[n];
            for (int i = 0; i < n; i++) {
                ItemStack s = slots[i];
                if (s == null) {
                    groups[i] = -1;
                    amounts[i] = 0;
                    max[i] = 1;
                    continue;
                }
                int g = -1;
                for (int r = 0; r < representatives.size(); r++) {
                    if (representatives.get(r).isSimilar(s)) {
                        g = r;
                        break;
                    }
                }
                if (g < 0) {
                    g = representatives.size();
                    representatives.add(s);
                }
                groups[i] = g;
                amounts[i] = s.getAmount();
                max[i] = s.getMaxStackSize();
            }
            amounts = StackMerger.merge(groups, amounts, max);
        } else {
            for (int i = 0; i < n; i++) {
                amounts[i] = slots[i] == null ? 0 : slots[i].getAmount();
            }
        }
        for (int i = 0; i < n; i++) {
            ItemStack s = slots[i];
            if (s == null) {
                continue;
            }
            if (amounts[i] <= 0) {
                writer.set(i, null);
            } else if (s != contents[i] || amounts[i] != s.getAmount()) {
                ItemStack out = s.clone();
                out.setAmount(amounts[i]);
                writer.set(i, out);
            }
        }
        return stamped;
    }

    // ------------------------------------------------ the fix

    /**
     * Return a stamped stack to its resting form, or null when
     * the stack carries no stamp (never touched). Replicates
     * EliteMobs' own restore step for step — see
     * {@link RestorePlanner}.
     */
    ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keyMarker, PersistentDataType.BYTE)) {
            return null;
        }
        StampView view = new StampView(
                true,
                byteValue(pdc, keyDefault),
                pdc.get(keyNutrition, PersistentDataType.INTEGER),
                pdc.get(keySaturation, PersistentDataType.FLOAT),
                byteValue(pdc, keyAlwaysEdible),
                pdc.get(keyConsumption, PersistentDataType.FLOAT));
        RestorePlanner.Plan plan = RestorePlanner.plan(view);

        ItemStack out = stack.clone();
        ItemMeta outMeta = out.getItemMeta();
        PersistentDataContainer outPdc = outMeta.getPersistentDataContainer();
        outPdc.remove(keyMarker);
        outPdc.remove(keyDefault);
        outPdc.remove(keyNutrition);
        outPdc.remove(keySaturation);
        outPdc.remove(keyAlwaysEdible);
        outPdc.remove(keyConsumption);
        // The food component goes through the classic meta API,
        // the same calls EliteMobs' own restore makes: a null
        // food component is how a minted item carries "no food
        // of its own" (the material's defaults then apply) —
        // NOT a data-component removal, which would record an
        // explicit deletion and make the item inedible.
        if (plan.removeFood()) {
            outMeta.setFood(null);
        } else if (plan.setFood()) {
            org.bukkit.inventory.meta.components.FoodComponent food = outMeta.getFood();
            if (food != null) {
                food.setNutrition(plan.nutrition());
                food.setSaturation(plan.saturation());
                food.setCanAlwaysEat(plan.alwaysEdible());
                outMeta.setFood(food);
            }
        }
        out.setItemMeta(outMeta);

        if (plan.setFood() && out.getData(DataComponentTypes.FOOD) == null) {
            // Defensive: a meta whose getFood() gave nothing to
            // rebuild (not seen on Paper 26 — EliteMobs' restore
            // relies on the same call). Fall back to writing the
            // component directly so the plan still lands.
            out.setData(DataComponentTypes.FOOD, FoodProperties.food()
                    .nutrition(plan.nutrition())
                    .saturation(plan.saturation())
                    .canAlwaysEat(plan.alwaysEdible())
                    .build());
        }
        if (plan.consumptionSeconds() != null) {
            Consumable current = out.getData(DataComponentTypes.CONSUMABLE);
            if (current != null) {
                out.setData(DataComponentTypes.CONSUMABLE, current.toBuilder()
                        .consumeSeconds(plan.consumptionSeconds())
                        .build());
            }
        }
        return out;
    }

    private static boolean byteValue(PersistentDataContainer pdc, NamespacedKey key) {
        Byte b = pdc.get(key, PersistentDataType.BYTE);
        return b != null && b != 0;
    }

    private void debug(String message) {
        if (debug) {
            ctx.logger().info("[food-stack-normalize] " + message);
        }
    }
}
