package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.MaxOrgCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.store.ChestRecord;
import com.winkreyes.excelautosort.store.ItemCodec;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Max Organization (SPEC §15) — the shared semantics behind both
 * renderers (Java GUI, Bedrock forms) and the text path, the same
 * one-flow-two-skins discipline as SelectorFlow:
 *
 *   - the unlock: rent-or-buy through the Vault economy, one charge
 *     path, confirm handled by the renderers (§15.2);
 *   - the network: every chest/barrel in radius the player holds
 *     CONFIGURE rights over (§15.3, the exact §6.4 rule — no second
 *     trust model), resolved at open and re-verified for every move;
 *   - the moves: takes and smart deposits executed as verified
 *     transfers between real inventories (§15.4's window invariant),
 *     Protect-logged on the §6.3 discipline (§15.5).
 *
 * Fail-closed throughout: any check that cannot be made — rights
 * gone, chest gone, chunk unloaded, destination full — leaves every
 * item exactly where it was.
 */
public final class MaxOrgService {

    public enum PurchaseOutcome { RENTED, BOUGHT, ALREADY_OWNED, ECONOMY_DOWN, INSUFFICIENT, DISABLED }

    /** One network chest: its canonical key, its block keys, its filter record (if any). */
    public record NetChest(String key, List<String> keys, ChestRecord record,
                           String world, int x, int y, int z) {
        public String ownerUuid() {
            return record == null || record.owner == null ? null : record.owner.toString();
        }
    }

    /** The outcome of one smart deposit, for the caller's report lines. */
    public record DepositReport(int chests, int stacksMoved, int itemsMoved,
                                List<String> unplaced, boolean networkEmpty) {}

    private final ExcelAutoSort plugin;

    public MaxOrgService(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ config (§15.7)

    public boolean enabled() { return plugin.getConfig().getBoolean("max-org.enabled", true); }
    public double rentPrice() { return plugin.getConfig().getDouble("max-org.rent-price", 1000); }
    public int rentDays() { return plugin.getConfig().getInt("max-org.rent-days", 30); }
    public double buyPrice() { return plugin.getConfig().getDouble("max-org.buy-price", 15000); }
    public int radius() { return plugin.getConfig().getInt("max-org.radius", 16); }
    public int pageSize() { return plugin.getConfig().getInt("max-org.page-size", 45); }

    // ------------------------------------------------------------ entitlement (§15.2)

    public MaxOrgCore.Entitlement entitlement(UUID player) {
        return plugin.store().maxOrgOf(player);
    }

    public MaxOrgCore.Access access(UUID player) {
        return MaxOrgCore.access(entitlement(player), System.currentTimeMillis());
    }

    public boolean entitled(UUID player) {
        return access(player) != MaxOrgCore.Access.NONE;
    }

    /** The message key naming the player's CURRENT state (menu-toggle standard). */
    public String stateKey(UUID player) {
        MaxOrgCore.Entitlement e = entitlement(player);
        long now = System.currentTimeMillis();
        return switch (MaxOrgCore.access(e, now)) {
            case OWNED -> "maxorg-state-owned";
            case RENTED -> "maxorg-state-rented";
            case NONE -> MaxOrgCore.lapsed(e, now) ? "maxorg-state-lapsed" : "maxorg-state-none";
        };
    }

    /** Placeholder values for the state line: {time} = rent time left. */
    public Map<String, String> stateVars(UUID player) {
        MaxOrgCore.Entitlement e = entitlement(player);
        return Map.of("time", BufferCore.timeLeft(e.rentUntilMs(), System.currentTimeMillis()));
    }

    /** The unlock pitch lines (both prices + lapse behavior, up front — §15.2). */
    public List<String> pitchLines() {
        return plugin.messages().rawList("maxorg-pitch", List.of(
                "Open and manage all your chests around you at once.",
                "Rent: {rent} for {days} days — when it runs out the ability just turns off; re-rent anytime. Nothing is ever auto-charged.",
                "Buy: {buy} once — yours forever. Rent you already paid does not count toward buying."),
                pitchVars());
    }

    public Map<String, String> pitchVars() {
        return Map.of(
                "rent", MaxOrgCore.money(rentPrice()),
                "days", String.valueOf(rentDays()),
                "buy", MaxOrgCore.money(buyPrice()));
    }

    /** The confirm wording for one purchase (§15.2: option, price, duration/permanence, lapse). */
    public List<String> confirmLines(boolean buy) {
        Map<String, String> vars = new LinkedHashMap<>(pitchVars());
        vars.put("price", MaxOrgCore.money(buy ? buyPrice() : rentPrice()));
        vars.put("time", BufferCore.timeLeft(
                MaxOrgCore.rentExpiry(System.currentTimeMillis(), rentDays()),
                System.currentTimeMillis()));
        return plugin.messages().rawList(buy ? "maxorg-confirm-buy" : "maxorg-confirm-rent", List.of(
                buy ? "Buy Max Organization for {price} — yours forever, no expiry."
                    : "Rent Max Organization for {price} — {days} days from now.",
                buy ? "Rent you already paid does not count toward this price."
                    : "When it runs out the ability turns off. Nothing is held or lost — it just stops until you rent again."),
                vars);
    }

    // ------------------------------------------------------------ purchase (§15.2)

    /**
     * THE charge path — the only code that moves money for Max
     * Organization. Renderers call it only after their confirm step;
     * it re-checks everything at charge time: already-owned refuses,
     * insufficient funds charges nothing and changes nothing, a failed
     * withdrawal changes no state, and a successful one applies the
     * entitlement, persists it, and writes the Protect record in the
     * same pass. Exactly one charge per call, by construction.
     */
    public PurchaseOutcome purchase(Player player, boolean buy) {
        if (!enabled()) return PurchaseOutcome.DISABLED;
        UUID id = player.getUniqueId();
        MaxOrgCore.Entitlement before = entitlement(id);
        if (MaxOrgCore.access(before, System.currentTimeMillis()) == MaxOrgCore.Access.OWNED) {
            return PurchaseOutcome.ALREADY_OWNED;
        }
        if (!plugin.economy().available()) return PurchaseOutcome.ECONOMY_DOWN;
        double price = buy ? buyPrice() : rentPrice();
        double balance = plugin.economy().balance(id);
        if (balance < 0) return PurchaseOutcome.ECONOMY_DOWN;
        if (balance < price) return PurchaseOutcome.INSUFFICIENT;
        if (!plugin.economy().withdraw(id, price)) return PurchaseOutcome.ECONOMY_DOWN;
        long now = System.currentTimeMillis();
        MaxOrgCore.Entitlement after = buy
                ? MaxOrgCore.afterBuy(before)
                : MaxOrgCore.afterRent(before, now, rentDays());
        plugin.store().setMaxOrg(id, after);
        plugin.sink().record(ProtectEvents.MAXORG_PURCHASE, ProtectEvents.maxOrgPurchase(
                id.toString(), player.getName(),
                buy ? ProtectEvents.PURCHASE_BUY : ProtectEvents.PURCHASE_RENT,
                price, after.owned(), after.rentUntilMs()));
        return buy ? PurchaseOutcome.BOUGHT : PurchaseOutcome.RENTED;
    }

    /** The plain outcome line for a purchase attempt (shared by all three paths). */
    public void tellOutcome(Player player, PurchaseOutcome outcome, boolean buy) {
        switch (outcome) {
            case RENTED -> plugin.messages().send(player, "maxorg-rented",
                    "&aMax Organization is yours for {time}. Stand among your chests and open it any time.",
                    Map.of("time", BufferCore.timeLeft(entitlement(player.getUniqueId()).rentUntilMs(),
                            System.currentTimeMillis())));
            case BOUGHT -> plugin.messages().send(player, "maxorg-bought",
                    "&aMax Organization is yours forever. Stand among your chests and open it any time.");
            case ALREADY_OWNED -> plugin.messages().send(player, "maxorg-already-owned",
                    "&eYou already own Max Organization — it's yours forever.");
            case ECONOMY_DOWN -> plugin.messages().send(player, "maxorg-economy-down",
                    "&cThe server money system isn't answering right now — nothing was charged. Try again in a bit.");
            case INSUFFICIENT -> plugin.messages().send(player, "maxorg-insufficient",
                    "&cYou don't have {price} — nothing was charged.",
                    Map.of("price", MaxOrgCore.money(buy ? buyPrice() : rentPrice())));
            case DISABLED -> plugin.messages().send(player, "maxorg-disabled",
                    "&eMax Organization is turned off on this server right now.");
        }
    }

    // ------------------------------------------------------------ the network (§15.3)

    /**
     * The point the radius is measured FROM: the center of the block
     * the player stands in. (§15.3 says "from the player's position";
     * the rig's boundary case pinned the reading — a same-floor chest
     * exactly {@code radius} blocks away must be IN, and measuring
     * from the feet point (y = the block's floor) puts every
     * same-floor chest half a block farther than its block distance,
     * silently shrinking the reach to radius−1. Block-center to
     * block-center is the distance players mean.)
     */
    private static double[] centerOf(Player player) {
        Location loc = player.getLocation();
        return new double[]{loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5};
    }

    /**
     * Resolve the network from where the player stands NOW: every
     * chest/barrel whose canonical block is within the radius, in a
     * loaded chunk, not filter-suspended, and where the player holds
     * CONFIGURE rights (§6.4). Sorted by canonical key so the view is
     * stable between opens.
     */
    public List<NetChest> resolveNetwork(Player player) {
        List<NetChest> out = new ArrayList<>();
        if (!entitled(player.getUniqueId())) return out;
        World world = player.getWorld();
        Location loc = player.getLocation();
        double[] center = centerOf(player);
        int r = radius();
        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();
        Map<String, NetChest> seen = new LinkedHashMap<>();
        for (int bx = cx - r; bx <= cx + r; bx++) {
            for (int bz = cz - r; bz <= cz + r; bz++) {
                if (!world.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                for (int by = Math.max(world.getMinHeight(), cy - r);
                     by <= Math.min(world.getMaxHeight() - 1, cy + r); by++) {
                    Block b = world.getBlockAt(bx, by, bz);
                    Material t = b.getType();
                    if (t != Material.CHEST && t != Material.TRAPPED_CHEST && t != Material.BARREL) continue;
                    List<String> keys = FilterEngine.keysOfBlock(b);
                    if (keys.isEmpty()) continue;
                    String canonical = keys.get(0);
                    if (seen.containsKey(canonical)) continue;
                    ChestKeys.BlockKey bk = ChestKeys.parse(canonical);
                    if (bk == null) continue;
                    if (!MaxOrgCore.withinRadius(center[0], center[1], center[2],
                            bk.x(), bk.y(), bk.z(), r)) continue;
                    ChestRecord record = null;
                    for (String k : keys) {
                        record = plugin.store().byKey(k);
                        if (record != null) break;
                    }
                    if (record != null && record.suspended) continue;
                    if (!plugin.rights().canConfigure(player,
                            new Location(world, bk.x(), bk.y(), bk.z()),
                            record == null ? null : record.owner)) continue;
                    NetChest nc = new NetChest(canonical, keys, record, bk.world(), bk.x(), bk.y(), bk.z());
                    if (liveInventory(nc) == null) continue;
                    seen.put(canonical, nc);
                }
            }
        }
        out.addAll(seen.values());
        out.sort(Comparator.comparing(NetChest::key));
        return out;
    }

    /** The live inventory behind a network chest, or null when it cannot be reached now. */
    public Inventory liveInventory(NetChest nc) {
        if (nc.record() != null) return plugin.filter().liveInventory(nc.record());
        return FilterEngine.inventoryOf(nc.keys());
    }

    /**
     * Re-verify one chest at move time (§15.3): entitlement current,
     * rights still held, the chest still within reach of where the
     * player stands now, the record not suspended since the view
     * opened, and a live inventory behind it. Any failure = the move
     * does not happen.
     */
    public Inventory verifyForMove(Player player, NetChest nc) {
        if (!entitled(player.getUniqueId())) return null;
        ChestRecord record = nc.record();
        if (record != null) {
            ChestRecord current = plugin.store().byKey(nc.key());
            if (current == null || current.suspended) return null;
            record = current;
        }
        double[] center = centerOf(player);
        if (!MaxOrgCore.withinRadius(center[0], center[1], center[2],
                nc.x(), nc.y(), nc.z(), radius())) return null;
        if (!plugin.rights().canConfigure(player,
                new Location(player.getWorld(), nc.x(), nc.y(), nc.z()),
                record == null ? null : record.owner)) return null;
        return liveInventory(record == null ? nc
                : new NetChest(nc.key(), nc.keys(), record, nc.world(), nc.x(), nc.y(), nc.z()));
    }

    // ------------------------------------------------------------ stack describing

    /** Describe an inventory's stacks for the view model / planner (slot order). */
    public static List<MaxOrgCore.SlotStack> describe(Inventory inv) {
        List<MaxOrgCore.SlotStack> out = new ArrayList<>();
        if (inv == null) return out;
        for (ItemStack s : inv.getContents()) out.add(describeOne(s));
        return out;
    }

    /** Describe the player's 36 storage slots (moves never touch armor or the offhand). */
    public static List<MaxOrgCore.SlotStack> describeStorage(Player player) {
        List<MaxOrgCore.SlotStack> out = new ArrayList<>();
        for (ItemStack s : player.getInventory().getStorageContents()) out.add(describeOne(s));
        return out;
    }

    private static MaxOrgCore.SlotStack describeOne(ItemStack s) {
        if (s == null || s.getType() == Material.AIR || s.getAmount() <= 0) {
            return new MaxOrgCore.SlotStack(null, null, 0, 0);
        }
        return new MaxOrgCore.SlotStack(s.getType().name(), metaKeyOf(s), s.getAmount(),
                s.getMaxStackSize());
    }

    /** The stack's meta fingerprint: its serialized form at amount 1 (type + all meta). */
    public static String metaKeyOf(ItemStack s) {
        ItemStack one = s.clone();
        one.setAmount(1);
        return ItemCodec.toB64(one);
    }

    /** The view over a resolved network: flattened rows naming real chests + slots. */
    public List<MaxOrgCore.ViewEntry> viewOf(List<NetChest> network) {
        List<MaxOrgCore.ChestView> views = new ArrayList<>();
        for (NetChest nc : network) {
            Inventory inv = liveInventory(nc);
            if (inv == null) continue;
            views.add(new MaxOrgCore.ChestView(nc.key(), nc.world(), nc.x(), nc.y(), nc.z(),
                    describe(inv)));
        }
        return MaxOrgCore.flatten(views);
    }

    // ------------------------------------------------------------ take (§15.5)

    /**
     * Take from one real chest slot into the player's inventory: the
     * whole stack, or what fits — a partial fit moves what fits and
     * leaves the rest (§15.5). Returns the count moved; 0 when nothing
     * could move (full hands, stale slot); -1 when the move was
     * refused outright (rights/radius/entitlement — the chest is out
     * of the network now). The taken stack is restored to its slot if
     * the player's inventory cannot hold what the fit-check promised,
     * so an item is never deleted by a disagreement between the model
     * and the game.
     */
    public int take(Player player, NetChest nc, int slot) {
        Inventory inv = verifyForMove(player, nc);
        if (inv == null) return -1;
        if (slot < 0 || slot >= inv.getSize()) return 0;
        ItemStack cur = inv.getItem(slot);
        if (cur == null || cur.getType() == Material.AIR || cur.getAmount() <= 0) return 0;
        int fits = MaxOrgCore.fitsCount(describeStorage(player), describeOne(cur));
        int move = Math.min(cur.getAmount(), fits);
        if (move <= 0) return 0;
        ItemStack taken = cur.clone();
        taken.setAmount(move);
        if (move == cur.getAmount()) inv.setItem(slot, null);
        else {
            ItemStack rest = cur.clone();
            rest.setAmount(cur.getAmount() - move);
            inv.setItem(slot, rest);
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(taken);
        if (!leftover.isEmpty()) {
            int back = 0;
            for (ItemStack l : leftover.values()) back += l.getAmount();
            // Put back exactly what would not fit — into the same slot.
            ItemStack now = inv.getItem(slot);
            if (now == null || now.getType() == Material.AIR) {
                ItemStack restore = taken.clone();
                restore.setAmount(back);
                inv.setItem(slot, restore);
            } else {
                ItemStack restore = now.clone();
                restore.setAmount(now.getAmount() + back);
                inv.setItem(slot, restore);
            }
            move -= back;
        }
        if (move > 0) {
            plugin.sink().record(ProtectEvents.NETWORK_TAKE, ProtectEvents.networkTake(
                    nc.world(), nc.x(), nc.y(), nc.z(), nc.key(),
                    player.getUniqueId().toString(), player.getName(),
                    nc.ownerUuid(), ItemCodec.materialKeyOf(taken), move));
        }
        return move;
    }

    // ------------------------------------------------------------ smart deposit (§15.5)

    /**
     * Route the player's storage contents into the network, planner
     * first (§15.5): whitelist chests receive only listed items and
     * are preferred for them; blacklist chests refuse listed items;
     * unmarked chests accept everything. Execution re-verifies every
     * chest and every stack against the live inventories — what no
     * chest will take stays in the player's hands and is reported,
     * never voided.
     */
    public DepositReport smartDeposit(Player player) {
        List<NetChest> network = resolveNetwork(player);
        if (network.isEmpty()) return new DepositReport(0, 0, 0, List.of(), true);
        Map<String, NetChest> byKey = new LinkedHashMap<>();
        List<MaxOrgCore.DepositChest> dest = new ArrayList<>();
        for (NetChest nc : network) {
            Inventory inv = liveInventory(nc);
            if (inv == null) continue;
            byKey.put(nc.key(), nc);
            Mode mode = nc.record() == null ? Mode.OFF : nc.record().mode;
            Set<String> list = nc.record() == null ? Set.of() : nc.record().items;
            dest.add(new MaxOrgCore.DepositChest(nc.key(), mode, list, describe(inv)));
        }
        PlayerInventory pinv = player.getInventory();
        ItemStack[] storage = pinv.getStorageContents();
        List<MaxOrgCore.PlayerStack> mine = new ArrayList<>();
        for (int i = 0; i < storage.length; i++) {
            MaxOrgCore.SlotStack d = describeOne(storage[i]);
            if (!d.empty()) mine.add(new MaxOrgCore.PlayerStack(i, d));
        }
        MaxOrgCore.DepositPlan plan = MaxOrgCore.planDeposit(mine, dest);
        int stacks = 0, items = 0;
        Map<Integer, Integer> placedByIndex = new LinkedHashMap<>();
        for (MaxOrgCore.DepositMove mv : plan.moves()) {
            NetChest nc = byKey.get(mv.chestKey());
            if (nc == null) continue;
            int placed = executeDepositMove(player, nc, mv);
            if (placed > 0) {
                stacks++;
                items += placed;
                placedByIndex.merge(mv.playerIndex(), placed, Integer::sum);
            }
        }
        // What stayed: original stacks minus what actually landed.
        Map<String, Integer> unplacedByMat = new LinkedHashMap<>();
        for (MaxOrgCore.PlayerStack ps : mine) {
            int left = ps.stack().count() - placedByIndex.getOrDefault(ps.index(), 0);
            if (left > 0) unplacedByMat.merge(ps.stack().material(), left, Integer::sum);
        }
        List<String> unplaced = new ArrayList<>();
        for (Map.Entry<String, Integer> e : unplacedByMat.entrySet()) {
            unplaced.add(com.winkreyes.excelautosort.Names.friendly(e.getKey()) + " ×" + e.getValue());
        }
        return new DepositReport(byKey.size(), stacks, items, unplaced, false);
    }

    /**
     * Execute one planned placement, verified live: the chest must
     * still pass the move checks, the player's slot must still hold
     * the planned stack, and the chest must still have room for it.
     * Removal happens only after the room is confirmed, placement
     * mirrors the planner's fill order (partials first, then empties),
     * and the Protect record carries the count that actually moved.
     */
    private int executeDepositMove(Player player, NetChest nc, MaxOrgCore.DepositMove mv) {
        Inventory inv = verifyForMove(player, nc);
        if (inv == null) return 0;
        PlayerInventory pinv = player.getInventory();
        ItemStack[] storage = pinv.getStorageContents();
        if (mv.playerIndex() < 0 || mv.playerIndex() >= storage.length) return 0;
        ItemStack held = storage[mv.playerIndex()];
        if (held == null || held.getType() == Material.AIR || held.getAmount() <= 0) return 0;
        // The plan's premise, re-checked live: the destination filter
        // must still accept this material (§15.5 — filters are law).
        ChestRecord record = nc.record();
        Mode mode = record == null ? Mode.OFF : record.mode;
        Set<String> list = record == null ? Set.of() : record.items;
        if (!MaxOrgCore.accepts(new MaxOrgCore.DepositChest(
                nc.key(), mode, list, List.of()), held.getType().name())) return 0;
        MaxOrgCore.SlotStack want = describeOne(held);
        int room = MaxOrgCore.fitsCount(describe(inv), want);
        int move = Math.min(mv.count(), Math.min(held.getAmount(), room));
        if (move <= 0) return 0;
        ItemStack placing = held.clone();
        placing.setAmount(move);
        // Remove from the player's storage slots (this stack, then any
        // similar ones — the plan counts stacks by slot, but a settle
        // between plan and execution must not strand the move).
        int removed = removeSimilar(pinv, held, move);
        if (removed <= 0) return 0;
        int placedNow = placeInto(inv, placing, removed);
        if (placedNow < removed) {
            // The chest changed under us: hand the difference back.
            ItemStack back = placing.clone();
            back.setAmount(removed - placedNow);
            Map<Integer, ItemStack> left = pinv.addItem(back);
            if (!left.isEmpty()) {
                // Hands full AND chest short — drop at the feet rather
                // than void (the buffer rule's spirit: never lose it).
                for (ItemStack l : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), l);
                }
            }
        }
        if (placedNow > 0) {
            plugin.sink().record(ProtectEvents.NETWORK_DEPOSIT, ProtectEvents.networkDeposit(
                    nc.world(), nc.x(), nc.y(), nc.z(), nc.key(),
                    player.getUniqueId().toString(), player.getName(),
                    nc.ownerUuid(), ItemCodec.materialKeyOf(placing), placedNow));
        }
        return placedNow;
    }

    /** Remove up to {@code count} of a stack (similarity-exact) from storage; returns removed. */
    private static int removeSimilar(PlayerInventory inv, ItemStack template, int count) {
        int left = count;
        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length && left > 0; i++) {
            ItemStack s = storage[i];
            if (s == null || !s.isSimilar(template)) continue;
            int take = Math.min(left, s.getAmount());
            if (take == s.getAmount()) storage[i] = null;
            else {
                ItemStack rest = s.clone();
                rest.setAmount(s.getAmount() - take);
                storage[i] = rest;
            }
            left -= take;
        }
        if (left < count) inv.setStorageContents(storage);
        return count - left;
    }

    /** Place a stack into a chest inventory: partials first, then empties. Returns placed. */
    private static int placeInto(Inventory inv, ItemStack stack, int count) {
        int left = count;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack s = contents[i];
            if (s != null && s.isSimilar(stack) && s.getAmount() < s.getMaxStackSize()) {
                int add = Math.min(left, s.getMaxStackSize() - s.getAmount());
                ItemStack grown = s.clone();
                grown.setAmount(s.getAmount() + add);
                contents[i] = grown;
                left -= add;
            }
        }
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() == Material.AIR) {
                int add = Math.min(left, stack.getMaxStackSize());
                ItemStack fresh = stack.clone();
                fresh.setAmount(add);
                contents[i] = fresh;
                left -= add;
            }
        }
        if (left < count) inv.setContents(contents);
        return count - left;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
