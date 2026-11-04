package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.ArrangePlan;
import com.winkreyes.excelwandbars.core.HoldReadPlan;
import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.WandStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 0.3.5: the shared inventory-screen hook for two features (design 2 + 3).
 *
 * Arrange mode (section 2): while armed, clicks and drags on the open spell
 * bar inside the player's own inventory screen are translated into ArrangePlan
 * inputs and cancelled (the icons are virtual; they never move). Intents run
 * through LoadoutHook's arrange methods, the exact model calls /spellbar uses,
 * so chat lines, saves and the layout rebuild are identical by construction.
 * Icon identity is read from the item itself (MagicHook.spellKeyOf, the NBT
 * key): the cooldown-freeze investigation proved the renderer resolves spells
 * per icon the same way, so the tapped icon IS the tapped spell. Slots come
 * from column math against the wand's current page, never from the icon.
 *
 * Hold-to-read (section 3): observe only (MONITOR). A Bedrock player who lifts
 * a bar icon in this screen and commits nothing for the hold window gets the
 * spell's detail form, which never fades (the native tooltip's duration is
 * client-side and cannot be pinned from the server). Any commit cancels the
 * pending read; Java never gets one (its hover already persists).
 */
final class ArrangeHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final ArrangePlan plan = new ArrangePlan();
    private final HoldReadPlan holdRead = new HoldReadPlan();
    private boolean arrangeEnabled = true;
    private int arrangeTimeoutSeconds = 90;
    private boolean holdReadEnabled = true;
    private long holdReadMs = 800;
    private final Map<UUID, Integer> armSerial = new HashMap<>();
    private long armSessions = 0, removes = 0, moves = 0, ignored = 0, readsShown = 0, readsCancelled = 0;

    ArrangeHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    void loadSettings(ConfigurationSection c) {
        arrangeEnabled = c == null || c.getBoolean("arrange-enabled", true);
        arrangeTimeoutSeconds = Math.max(10, c == null ? 90 : c.getInt("arrange-timeout-seconds", 90));
        holdReadEnabled = c == null || c.getBoolean("hold-read", true);
        holdReadMs = Math.max(100, c == null ? 800 : c.getLong("hold-read-ms", 800));
    }

    String summary() {
        return (arrangeEnabled ? "ON (timeout " + arrangeTimeoutSeconds + " s)" : "OFF")
                + ", hold-to-read " + (holdReadEnabled ? "ON (" + holdReadMs + " ms, Bedrock screens)" : "OFF");
    }

    String counters() {
        return "armed " + armSessions + " removes " + removes + " moves " + moves + " ignored " + ignored
                + " hold-reads shown " + readsShown + " cancelled " + readsCancelled;
    }

    void clearAll() { plan.clear(); holdRead.clear(); armSerial.clear(); }

    // ------------------------------------------------------------------ arming

    /** /spellbar arrange and the bar menu's Arrange button. Returns a status word. */
    String arm(Player p) {
        if (!arrangeEnabled) { p.sendMessage(Component.text(LoadoutText.OFF, NamedTextColor.GRAY)); return "arrange-off"; }
        LoadoutHook lo = plugin.loadouts();
        if (lo == null || !lo.usable(p)) { p.sendMessage(Component.text(LoadoutText.NOT_BAR_STYLE, NamedTextColor.GRAY)); return "blocked"; }
        StyleHook s = plugin.styleHook();
        if (s == null || s.styleOf(p) != WandStyle.TAPBAR) { p.sendMessage(Component.text(LoadoutText.NOT_BAR_STYLE, NamedTextColor.GRAY)); return "blocked"; }
        TapBarHook tb = plugin.tapBar();
        if (tb == null) { p.sendMessage(Component.text(LoadoutText.OFF, NamedTextColor.GRAY)); return "no-tapbar"; }
        if (!barOpen(p)) {
            String r = tb.tryOpen(p, "arrange");
            if (!barOpen(p)) { plugin.getLogger().info("Arrange for " + p.getName() + ": bar did not open (" + r + ")"); return "no-bar"; }
        }
        UUID id = p.getUniqueId();
        plan.setArmed(id, true);
        int serial = armSerial.merge(id, 1, Integer::sum);
        armSessions++;
        p.sendMessage(Component.text(LoadoutText.arrangeHint(plugin.isBedrock(p)), NamedTextColor.AQUA));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && plan.isArmed(id) && armSerial.getOrDefault(id, 0) == serial) disarm(p, false);
        }, arrangeTimeoutSeconds * 20L);
        return "armed";
    }

    private void disarm(Player p, boolean silent) {
        UUID id = p.getUniqueId();
        if (!plan.isArmed(id)) return;
        plan.setArmed(id, false);
        if (!silent) p.sendMessage(Component.text("Arrange is off.", NamedTextColor.GRAY));
    }

    // ------------------------------------------------------------------ screen state

    private Wand openWand(Player p) {
        Mage mage = api.getMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        return w != null && w.isInventoryOpen() ? w : null;
    }

    private boolean barOpen(Player p) { return openWand(p) != null; }

    /** The player's own inventory screen (their inventory is what clicks land in). */
    private boolean ownScreen(Player p) {
        InventoryType t = p.getOpenInventory().getType();
        return t == InventoryType.CRAFTING || t == InventoryType.PLAYER || t == InventoryType.CREATIVE;
    }

    private boolean screenActive(Player p) { return barOpen(p) && ownScreen(p); }

    /** Column of a hotbar slot: the non-wand hotbar slots in order (wand slot = the held slot). */
    private int columnOf(Player p, int hotbarSlot) {
        int held = p.getInventory().getHeldItemSlot();
        if (hotbarSlot == held) return -1; // the wand itself
        return hotbarSlot < held ? hotbarSlot : hotbarSlot - 1;
    }

    /** Loadout slot behind a hotbar slot on the current page, or -1 when it is not a spell column. */
    private int loadoutSlotOf(Player p, Wand w, int hotbarSlot) {
        int col = columnOf(p, hotbarSlot);
        if (col < 0) return -1;
        StyleHook s = plugin.styleHook();
        int perPage = s == null ? 7 : s.perPageFor(WandStyle.TAPBAR);
        if (col >= perPage) return -1; // the exit column (and anything past it)
        int page = w instanceof com.elmakers.mine.bukkit.wand.Wand impl ? Math.max(0, impl.getCurrentHotbar()) : 0;
        return page * perPage + col;
    }

    private String iconAt(Player p, int hotbarSlot) {
        MagicHook mh = plugin.magicHook();
        if (mh == null) return null;
        ItemStack it = p.getInventory().getItem(hotbarSlot);
        String k = it == null ? null : mh.spellKeyOf(it);
        return k == null ? null : mh.baseKey(k).toLowerCase(java.util.Locale.ROOT);
    }

    private ArrangePlan.Target targetOf(Player p, Wand w, Inventory clicked, int slot) {
        if (clicked == null) return ArrangePlan.Target.outside(); // clicked outside the window
        if (clicked.equals(p.getInventory())) {
            if (slot >= 0 && slot < 9) {
                int ls = loadoutSlotOf(p, w, slot);
                return ls >= 0 ? ArrangePlan.Target.bar(ls) : ArrangePlan.Target.protectedSlot();
            }
            return ArrangePlan.Target.outside(); // main inventory, armor, offhand: the remove zone
        }
        return ArrangePlan.Target.outside(); // the crafting grid and anything else on screen
    }

    private void execute(Player p, ArrangePlan.Result r) {
        if (r == null || r.spell == null) return;
        LoadoutHook lo = plugin.loadouts();
        switch (r.intent) {
            case REMOVE:
                holdReadCommit(p); // the arrange intent firing cancels any pending read (design 3.4)
                if (lo != null) lo.arrangeRemove(p, r.spell);
                removes++;
                keepScreen(p);
                break;
            case MOVE:
                holdReadCommit(p);
                if (lo != null) lo.arrangeMove(p, r.spell, r.slot);
                moves++;
                keepScreen(p);
                break;
            default: break;
        }
    }

    /**
     * A loadout change applying can close the wand bar's screen state (the same
     * reason /spellbar's own changes reopen theirs). Arrange is a mode, not a
     * one-shot: while the player is still in their own screen with the mode on,
     * restore the bar so the next gesture lands in the same session. Runs a tick
     * after the commit, inside the close-check's settle window, so the mode's
     * lifecycle still judges the result rather than the mid-repaint state.
     */
    private void keepScreen(Player p) {
        UUID id = p.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || !plan.isArmed(id) || barOpen(p) || !ownScreen(p)) return;
            TapBarHook tb = plugin.tapBar();
            if (tb != null) tb.tryOpen(p, "arrange commit");
        });
    }

    // ------------------------------------------------------------------ arrange interpreter (LOWEST)

    private static boolean pickupAction(InventoryAction a) {
        return a == InventoryAction.PICKUP_ALL || a == InventoryAction.PICKUP_HALF
                || a == InventoryAction.PICKUP_ONE || a == InventoryAction.PICKUP_SOME;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClickArrange(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!plan.isArmed(id)) return;
        // Style changed under an armed player: the mode belongs to Tap bar only.
        StyleHook sh = plugin.styleHook();
        if (sh == null || sh.styleOf(p) != WandStyle.TAPBAR) { disarm(p, true); return; }
        if (!screenActive(p)) return;
        Wand w = openWand(p);
        Inventory clicked = e.getClickedInventory();
        boolean inPlayer = clicked != null && clicked.equals(p.getInventory());
        int slot = e.getSlot();
        String icon = inPlayer && slot >= 0 && slot < 9 ? iconAt(p, slot) : null;
        InventoryAction a = e.getAction();
        boolean dropAction = a == InventoryAction.DROP_ALL_SLOT || a == InventoryAction.DROP_ONE_SLOT;

        if (plan.hasLift(id)) {
            // The next click is the terminal commit, classified by where it lands.
            if (dropAction) { e.setCancelled(true); execute(p, plan.drop(id, System.currentTimeMillis())); return; }
            e.setCancelled(true);
            execute(p, plan.place(id, targetOf(p, w, clicked, slot), System.currentTimeMillis()));
            return;
        }
        if (icon == null) return; // not an icon: today's protections keep their meaning
        if (dropAction) {
            e.setCancelled(true);
            execute(p, plan.dropSlot(id, icon, loadoutSlotOf(p, w, slot), System.currentTimeMillis()));
            return;
        }
        if (pickupAction(a)) {
            int from = loadoutSlotOf(p, w, slot);
            if (from < 0) { e.setCancelled(true); ignored++; return; } // exit-column icons are not spells
            e.setCancelled(true);
            plan.pickup(id, icon, from, System.currentTimeMillis());
            return;
        }
        // Any other gesture on an icon (swap, shift-click, number key, collect): the icon
        // is virtual and must not move; swallow it without an intent.
        e.setCancelled(true);
        ignored++;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDragArrange(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!plan.isArmed(id) || !screenActive(p)) return;
        Wand w = openWand(p);
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        // What is being dragged: the old cursor (an icon already lifted), else an icon in the set.
        String spell = null;
        int from = -1;
        ItemStack old = e.getOldCursor();
        String oldKey = old == null ? null : mh.spellKeyOf(old);
        Map<Integer, ItemStack> items = e.getNewItems();
        List<ArrangePlan.Target> targets = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> en : items.entrySet()) {
            int raw = en.getKey();
            Inventory inv = e.getView().getInventory(raw);
            int slot = e.getView().convertSlot(raw);
            boolean hot = inv != null && inv.equals(p.getInventory()) && slot >= 0 && slot < 9;
            String here = hot ? iconAt(p, slot) : null;
            if (here != null && spell == null && oldKey == null) { spell = here; from = loadoutSlotOf(p, w, slot); continue; }
            targets.add(targetOf(p, w, inv, slot));
        }
        if (oldKey != null) {
            spell = mh.baseKey(oldKey).toLowerCase(java.util.Locale.ROOT);
            LoadoutHook lo = plugin.loadouts();
            from = lo == null ? -1 : lo.slotOf(p, spell);
            targets.clear();
            for (Map.Entry<Integer, ItemStack> en : items.entrySet()) {
                int raw = en.getKey();
                targets.add(targetOf(p, w, e.getView().getInventory(raw), e.getView().convertSlot(raw)));
            }
        }
        if (spell == null || from < 0) return; // no icon involved: not ours
        e.setCancelled(true);
        execute(p, plan.drag(id, spell, from, targets, System.currentTimeMillis()));
    }

    /** Q-drop of an item entity while a lift is pending (the cursor-drop dialect). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDropEntity(PlayerDropItemEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (plan.hasLift(id)) execute(e.getPlayer(), plan.drop(id, System.currentTimeMillis()));
    }

    // ------------------------------------------------------------------ hold-to-read (MONITOR, observe only)

    /**
     * The hold-read decision. Like the book gate's check, the tick delay is a hint,
     * not a clock: if the task runs before the wall-clock hold has elapsed (tick
     * catch-up after a stall), it waits out the remainder rather than dropping
     * the read. HoldReadPlan.decide is non-consuming before the threshold, so
     * re-checking is safe.
     */
    private void runReadCheck(Player p, UUID id, long liftAt) {
        if (!p.isOnline()) return;
        long now = System.currentTimeMillis();
        if (now - liftAt < holdReadMs) {
            long restTicks = Math.max(1L, (holdReadMs - (now - liftAt) + 49) / 50);
            Bukkit.getScheduler().runTaskLater(plugin, () -> runReadCheck(p, id, liftAt), restTicks);
            return;
        }
        String spell = holdRead.decide(id, now, holdReadMs);
        if (spell == null) return;
        if (!plugin.isBedrock(p) || !screenActive(p)) return;
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        // The form opening forgets any pending arrange lift for this icon (design 3.4).
        plan.cancelLift(id);
        String r = mh.showSpellInfo(p, spell);
        if (r != null && r.startsWith("detail")) readsShown++;
    }

    private void holdReadCommit(Player p) {
        UUID id = p.getUniqueId();
        if (holdRead.pending(id)) readsCancelled++;
        holdRead.commit(id);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickRead(InventoryClickEvent e) {
        if (!holdReadEnabled) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!plugin.isBedrock(p) || !screenActive(p)) return;
        UUID id = p.getUniqueId();
        Inventory clicked = e.getClickedInventory();
        boolean inPlayer = clicked != null && clicked.equals(p.getInventory());
        int slot = e.getSlot();
        if (inPlayer && slot >= 0 && slot < 9 && pickupAction(e.getAction())) {
            String icon = iconAt(p, slot);
            if (icon != null) {
                long liftAt = System.currentTimeMillis();
                holdRead.lift(id, icon, liftAt);
                long ticks = Math.max(1L, (holdReadMs + 49) / 50);
                Bukkit.getScheduler().runTaskLater(plugin, () -> runReadCheck(p, id, liftAt), ticks);
                return;
            }
        }
        holdReadCommit(p); // any other click is a commit
    }

    // ------------------------------------------------------------------ lifecycle

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        holdRead.screenClosed(id);
        InventoryType t = e.getView().getType();
        boolean own = t == InventoryType.CRAFTING || t == InventoryType.PLAYER || t == InventoryType.CREATIVE;
        if (own) {
            // A loadout change applying repaints this view by closing and reopening
            // it around the change; that is not the player closing the screen.
            // Judge it like the branch below: the mode ends only if the bar is
            // actually gone once the repaint settles. A real close still ends
            // the mode (the next open starts clean).
            scheduleCloseCheck(p);
            return;
        }
        // Some other inventory closed: if it took the bar with it, the mode ends too.
        scheduleCloseCheck(p);
    }

    /**
     * The mode follows the bar, not the close event: check shortly after a close,
     * and once more a few ticks later, disarming only if the bar is gone at both
     * judgements (a repaint's reopen lands between them; a real close does not).
     */
    private void scheduleCloseCheck(Player p) { scheduleCloseCheck(p, 0); }

    private void scheduleCloseCheck(Player p, int attempt) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (barOpen(p)) return;
            if (attempt == 0) { scheduleCloseCheck(p, 1); return; }
            disarm(p, true);
        }, attempt == 0 ? 1L : 3L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        plan.forget(id);
        holdRead.forget(id);
        armSerial.remove(id);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        disarm(e.getPlayer(), true);
        holdRead.barClosed(e.getPlayer().getUniqueId());
    }
}
