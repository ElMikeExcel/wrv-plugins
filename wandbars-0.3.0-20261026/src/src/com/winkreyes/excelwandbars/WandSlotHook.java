package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.WandSlotPlan;
import com.winkreyes.excelwandbars.core.WandStyle;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.7: the rank wand lives in the last hotbar slot (slot 9) for the hotbar-icon styles
 * (Tap bar and Tap to pick (original) by default).
 *
 * How a move works: ONE swap of two hotbar slots in the same tick. The wand goes to slot 9 and
 * whatever was in slot 9 (an item, a whole stack, or nothing) goes to the slot the wand came
 * from. Nothing is created, dropped or deleted. Never while Magic's spell bar is up (Magic holds
 * the real hotbar then and puts it back on close), never with a chest or other screen open,
 * never with an item on the cursor, never within a few seconds of an inventory click, never when
 * dead, in spectator or creative, and never in a skip world.
 *
 * Two triggers:
 *  - the player picks the wand in another slot (bar down): the pick is cancelled, the two slots
 *    swap and slot 9 is selected the next tick, so they hold the wand and the Tap bar opens as before;
 *  - a sweep every second (and a few seconds after join or respawn) for a wand sitting elsewhere.
 * If the player is HOLDING the wand in another slot, Magic's wand is put away first (its state is
 * saved onto the item), then the slots swap, slot 9 is selected and Magic picks the wand up again.
 * If they hold the item in slot 9 (not the wand) the sweep waits, so their hand never changes.
 * ExcelAttunement's stance lock runs first (LOW) and a refused pick is left alone (ignoreCancelled).
 */
final class WandSlotHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private boolean enabled = true;
    private int target = 8;
    private final Set<WandStyle> styles = EnumSet.of(WandStyle.TAPBAR, WandStyle.ORIGINAL);
    private long clickQuietMs = 3000;
    private long sweepTicks = 20;
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private long movedPick, movedSweep, movedHeld, pickAborted, sweepSkippedHolding;
    private BukkitTask ticker;

    WandSlotHook(ExcelWandBars plugin, MagicAPI api) { this.plugin = plugin; this.api = api; }

    void loadSettings(ConfigurationSection c) {
        enabled = c == null || c.getBoolean("enabled", true);
        target = WandSlotPlan.targetIndex(c == null ? 9 : c.getInt("slot", 9));
        clickQuietMs = Math.max(0, c == null ? 3000 : c.getLong("quiet-after-click-ms", 3000));
        sweepTicks = Math.max(5, c == null ? 20 : c.getLong("sweep-ticks", 20));
        styles.clear();
        java.util.List<String> list = c == null || !c.isList("styles") ? java.util.List.of("tapbar", "original") : c.getStringList("styles");
        for (String s : list) {
            WandStyle w = WandStyle.parse(s == null ? "" : s.toLowerCase(Locale.ROOT));
            if (w != null) styles.add(w);
        }
    }

    void start() {
        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::sweepAll, 40L, sweepTicks);
    }

    void stop() { if (ticker != null) { ticker.cancel(); ticker = null; } }

    String summary() {
        StringBuilder sb = new StringBuilder();
        for (WandStyle s : styles) { if (sb.length() > 0) sb.append(", "); sb.append(s.key()); }
        return (enabled ? "ON" : "OFF") + " (slot " + (target + 1) + ", styles " + sb + ", quiet after click " + clickQuietMs + " ms, sweep every " + sweepTicks + " ticks)";
    }

    String counters() {
        return "wand-slot pick " + movedPick + " sweep " + movedSweep + " held " + movedHeld + " pick-aborted " + pickAborted + " waits-holding-slot " + sweepSkippedHolding;
    }

    int targetIndex() { return target; }

    boolean applies(Player p) {
        if (!enabled || p == null || !p.isOnline()) return false;
        StyleHook s = plugin.styleHook();
        return s != null && s.enabled() && styles.contains(s.styleOf(p));
    }

    /** A rank wand this player may use. Magic's getWand(item) may write to the item, so it only ever gets a clone. */
    boolean isRankWandItem(Player p, ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        try {
            if (!api.isWand(it)) return false;
            Wand w = api.getWand(it.clone());
            StyleHook s = plugin.styleHook();
            return w != null && s != null && s.isRankWand(w) && w.canUse(p);
        } catch (Throwable t) {
            return false;
        }
    }

    private int rankWandSlot(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) if (isRankWandItem(p, inv.getItem(i))) return i;
        return -1;
    }

    private Wand activeWand(Player p) {
        Mage m = api.getMage(p);
        return m == null ? null : m.getActiveWand();
    }

    private WandSlotPlan.State state(Player p) {
        WandSlotPlan.State s = new WandSlotPlan.State();
        Wand w = activeWand(p);
        s.barOpen = w != null && w.isInventoryOpen();
        InventoryType top = p.getOpenInventory().getTopInventory().getType();
        s.otherScreenOpen = top != InventoryType.CRAFTING && top != InventoryType.PLAYER;
        ItemStack cur = p.getItemOnCursor();
        s.cursorHasItem = cur != null && !cur.getType().isAir();
        s.deadOrSpectator = p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE;
        s.skipWorld = plugin.skipWorld(p.getWorld());
        Long c = lastClick.get(p.getUniqueId());
        s.recentInventoryClick = c != null && System.currentTimeMillis() - c < clickQuietMs;
        s.heldSlot = p.getInventory().getHeldItemSlot();
        s.targetIsRankWand = isRankWandItem(p, p.getInventory().getItem(target));
        return s;
    }

    /** The swap itself: wand to the target, the target's item (or nothing) to the wand's old slot. */
    private void swap(Player p, int from) {
        PlayerInventory inv = p.getInventory();
        ItemStack wand = inv.getItem(from);
        ItemStack other = inv.getItem(target);
        inv.setItem(from, other == null ? null : other.clone());
        inv.setItem(target, wand == null ? null : wand.clone());
        TapBarHook tb = plugin.tapBar();
        if (tb != null) tb.noteServerChange(p);
    }

    /** /wandbars wandslot and the sweep: one check and, if it is safe, one move. Main thread. */
    String check(Player p, String why) {
        if (!applies(p)) return "style";
        WandSlotPlan.State s = state(p);
        s.wandSlot = rankWandSlot(p);
        String r = WandSlotPlan.why(s, target);
        if ("holding the target slot".equals(r)) sweepSkippedHolding++;
        if (!"move".equals(r)) return r;
        int from = s.wandSlot;
        boolean held = s.heldSlot == from;
        if (held) {
            Wand w = activeWand(p);
            if (w != null) {
                if (w.isInventoryOpen()) return "bar open";
                w.deactivate(); // Magic saves the wand's state onto the item in hand, synchronously
            }
            if (!isRankWandItem(p, p.getInventory().getItem(from))) return "wand changed";
            swap(p, from);
            p.getInventory().setHeldItemSlot(target);
            Mage m = api.getMage(p);
            if (m != null) m.checkWand();
            movedHeld++;
        } else {
            swap(p, from);
            movedSweep++;
        }
        if (plugin.debug()) plugin.getLogger().info("Wand slot: moved " + p.getName() + "'s wand " + (from + 1) + " -> " + (target + 1) + " (" + why + (held ? ", held" : "") + ")");
        return "moved " + (from + 1) + "->" + (target + 1) + (held ? " (held)" : "");
    }

    private void sweepAll() {
        if (!enabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { if (applies(p)) check(p, "sweep"); } catch (Throwable t) {
                if (plugin.debug()) plugin.getLogger().warning("Wand slot sweep for " + p.getName() + ": " + t);
            }
        }
    }

    /**
     * Picking the wand in another slot with the bar down. HIGH and ignoreCancelled: after
     * ExcelAttunement (LOW, stance lock) and Magic (NORMAL), and a refused pick is never touched.
     * Picking any other slot is never cancelled here.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if (!applies(p)) return;
        int ns = event.getNewSlot();
        if (ns == target) return;
        ItemStack picked = p.getInventory().getItem(ns);
        if (!isRankWandItem(p, picked)) return;
        WandSlotPlan.State s = state(p);
        s.wandSlot = ns;
        if (WandSlotPlan.onPick(s, ns, target) != WandSlotPlan.Action.SWAP) return;
        // The previous item must not be an active wand that is still up (two wands): leave that to Magic.
        Wand w = activeWand(p);
        if (w != null) return;
        event.setCancelled(true);
        final int prev = event.getPreviousSlot();
        final ItemStack snapshot = picked.clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            PlayerInventory inv = p.getInventory();
            WandSlotPlan.State now = state(p);
            now.wandSlot = ns;
            boolean same = inv.getHeldItemSlot() == prev && snapshot.isSimilar(inv.getItem(ns));
            if (!same || WandSlotPlan.onPick(now, ns, target) != WandSlotPlan.Action.SWAP || activeWand(p) != null) {
                pickAborted++;
                // Nothing moved: give them the slot they picked, as if this plugin was not here.
                if (inv.getHeldItemSlot() == prev && activeWand(p) == null) {
                    inv.setHeldItemSlot(ns);
                    TapBarHook tb = plugin.tapBar();
                    if (tb != null) tb.notePick(p);
                    Mage m = api.getMage(p);
                    if (m != null) m.checkWand();
                }
                return;
            }
            swap(p, ns);
            inv.setHeldItemSlot(target);
            TapBarHook tb = plugin.tapBar();
            if (tb != null) tb.notePick(p); // the Tap bar opener wants "the player picked the wand"
            Mage m = api.getMage(p);
            if (m != null) m.checkWand();
            movedPick++;
            if (plugin.debug()) plugin.getLogger().info("Wand slot: " + p.getName() + " picked the wand in slot " + (ns + 1) + ", moved to " + (target + 1));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) lastClick.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) lastClick.put(p.getUniqueId(), System.currentTimeMillis());
    }

    /** Join and respawn: wait the quiet time first (Magic and other plugins settle the inventory). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { lastClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) { lastClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { lastClick.remove(event.getPlayer().getUniqueId()); }
}
