package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.CooldownTagPlan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 0.3.10: the name-tag cooldown countdown (Mike's pick, Nov 3 2026).
 *
 * While a wand-mode hotbar spell icon's true time-to-cast (Magic's own
 * value: the max of remaining cooldown, charge recharge and mana wait —
 * {@code BaseSpell.getTimeToCast()}, the same number Magic clamps to 99
 * for its stack amount and Geyser flattens to 64 on Bedrock) exceeds the
 * configured threshold, the icon's display name carries the true time:
 * "Familiar · 3:24". At or below the threshold the name is left exactly as
 * painted and Magic's number does the talking; when the spell becomes
 * castable the annotation clears on the next poll (10 ticks).
 *
 * Why this is stable (bytecode-verified against Magic 11.2.5): Magic's
 * per-tick hotbar refresh ({@code Mage.updateHotbarStatus}) writes only
 * the stack AMOUNT and the icon art — it never rewrites a spell icon's
 * display name. Names are painted when the wand inventory is built
 * ({@code Wand.updateSpellName}) and, for Tap bar, by WandBars' own icon
 * lore on open/cast. So the annotation composes with both painters: it
 * appends to whatever name is current, and after any repaint the poll
 * simply re-appends.
 *
 * Safety contract (the 0.3.9 pager-leak lessons, applied in full):
 *  - DISPLAY COPIES ONLY. The annotation edits the live hotbar display
 *    item in place and stamps it with a PDC marker
 *    ({@code cooldown_name_tag} = the item's original display name,
 *    serialized; {@code cooldown_name_spell} = the spell it was painted
 *    for). Restore is exact: the recorded original name goes back and the
 *    marker comes off — we never pattern-guess a name we recorded.
 *  - Magic's record of the bar is its spell MAP (key -&gt; slot), built by
 *    reading each display item's spell key — a key our edit never touches
 *    (identification is Magic's own tagging, not the name). Nothing about
 *    the loadout model or the map can be polluted by a name.
 *  - The one place a raw display stack can land is a bar page's painted
 *    contents (Magic's saveInventory stores the current page's stacks).
 *    Those contents are runtime paint (the wand persists the map, not the
 *    stacks), and the standing cleanup restores any marked stack found in
 *    page contents, the wand's stored inventory, the player's real
 *    inventory or on the cursor — on the poll, on join, after closes, and
 *    before every WandBars-driven Magic cycle (MagicHook calls
 *    sweepNameTag next to sweepPager), so a save never has to see an
 *    annotated copy from any path WandBars initiates.
 *  - Fail-soft: any error disables the tag for that player for the
 *    session (after restoring their icons) and logs once. With
 *    enabled: false the hook annotates nothing and sweeps/cleans what a
 *    previous state left behind, so turning it off cleans up after
 *    itself.
 */
public class NameTagHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final NamespacedKey markerKey, spellKey;
    private boolean enabled = true;
    private int thresholdSeconds = 60;
    private BukkitTask task;
    private final Set<UUID> disabledFor = new HashSet<>();
    private final Set<UUID> cleanupNotified = new HashSet<>();
    private long tags, refreshes, restores, sweeps, cleanups, stale;

    NameTagHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.markerKey = new NamespacedKey(plugin, "cooldown_name_tag");
        this.spellKey = new NamespacedKey(plugin, "cooldown_name_spell");
    }

    void loadSettings(ConfigurationSection section) {
        enabled = section == null || section.getBoolean("enabled", true);
        int t = section == null ? 60 : section.getInt("threshold-seconds", 60);
        thresholdSeconds = Math.max(0, Math.min(3600, t));
    }

    void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L, 10L);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    String summary() {
        return (enabled ? "ON" : "OFF") + " (threshold " + thresholdSeconds + "s, tagged " + tags
                + ", refreshed " + refreshes + ", restored " + restores + ", sweeps " + sweeps
                + ", storage cleanups " + cleanups + ", stale " + stale + ")";
    }

    // ------------------------------------------------------------------ context

    private com.elmakers.mine.bukkit.wand.Wand implWand(Player p) {
        Mage m = api.getMage(p);
        Wand w = m == null ? null : m.getActiveWand();
        return w instanceof com.elmakers.mine.bukkit.wand.Wand impl ? impl : null;
    }

    /** Render context: the feature is on, the wand inventory is open, not a skip world. */
    private boolean active(Player p) {
        if (!enabled || disabledFor.contains(p.getUniqueId())) return false;
        if (plugin.skipWorld(p.getWorld())) return false;
        com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
        return w != null && w.isInventoryOpen();
    }

    // ------------------------------------------------------------------ marker

    /** {recorded original name JSON, spell base key} when the item is one of our annotated copies, else null. */
    private String[] mark(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        String json = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
        if (json == null) return null;
        String spell = meta.getPersistentDataContainer().get(spellKey, PersistentDataType.STRING);
        return new String[]{json, spell == null ? "" : spell};
    }

    private boolean marked(ItemStack it) { return mark(it) != null; }

    private static String plain(Component c) {
        return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
    }

    private static Component suffixComponent(long seconds) {
        return Component.text(CooldownTagPlan.suffix(seconds), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    // ------------------------------------------------------------------ restore

    /**
     * Put an annotated copy's original name back and take the marker off.
     * Exact when the recorded name parses (the normal path); a pattern
     * strip of our own suffix shape is the fallback for an unreadable
     * payload. Returns true when the item carried our marker.
     */
    private boolean restore(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        String json = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
        if (json == null) return false;
        Component base = null;
        try { base = GsonComponentSerializer.gson().deserialize(json); } catch (Throwable ignored) { }
        if (base != null) {
            meta.displayName(base);
        } else {
            String cur = plain(meta.displayName());
            if (CooldownTagPlan.hasOurSuffix(cur)) {
                meta.displayName(Component.text(CooldownTagPlan.stripSuffix(cur)));
            }
        }
        meta.getPersistentDataContainer().remove(markerKey);
        meta.getPersistentDataContainer().remove(spellKey);
        it.setItemMeta(meta);
        restores++;
        return true;
    }

    /** Restore every marked item in an inventory (in place; stacks keep their slot and count). */
    private int restoreAll(Inventory inv) {
        if (inv == null) return 0;
        int n = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (marked(contents[i]) && restore(contents[i])) {
                inv.setItem(i, contents[i]);
                n++;
            }
        }
        return n;
    }

    /** Restore every marked stack in a wand page / list inventory (Magic's own inventory type). */
    private int restoreAll(com.elmakers.mine.bukkit.wand.WandInventory inv) {
        if (inv == null) return 0;
        int n = 0;
        ItemStack[] contents = inv.getContents();
        if (contents == null) return 0;
        for (int i = 0; i < contents.length; i++) {
            if (marked(contents[i]) && restore(contents[i])) {
                inv.setItem(i, contents[i]);
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ render

    private void poll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                cleanupPlayer(p);
                if (active(p)) render(p);
                else if (!enabled || disabledFor.contains(p.getUniqueId())) sweepDisplay(p);
            } catch (Throwable t) {
                if (disabledFor.add(p.getUniqueId())) {
                    sweepDisplay(p);
                    plugin.getLogger().warning("Cooldown name tag off for " + p.getName() + " this session (" + t + "); Magic's number still shows.");
                }
            }
        }
    }

    private void render(Player p) {
        com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
        if (w == null) return;
        Mage mage = api.getMage(p);
        MagicHook mh = plugin.magicHook();
        if (mage == null || mh == null) return;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack it = p.getInventory().getItem(slot);
            if (it == null || it.getType().isAir()) continue;
            process(p, w, mage, mh, it);
        }
    }

    private void process(Player p, com.elmakers.mine.bukkit.wand.Wand w, Mage mage, MagicHook mh, ItemStack it) {
        String key = mh.spellKeyOf(it);
        String[] mk = mark(it);
        if (key == null) {
            // Not a live spell icon any more — if it is one of our copies, clean it.
            if (mk != null) { restore(it); stale++; }
            return;
        }
        String base = mh.baseKey(key);
        if (mk != null && !base.equals(mk[1])) {
            // Our copy now reads as a different spell (a repaint reused the
            // stack): restore the recorded name, drop the marker, and let a
            // later pass annotate the fresh paint if it still qualifies.
            restore(it);
            stale++;
            return;
        }
        com.elmakers.mine.bukkit.api.spell.Spell spell = null;
        Long ttc = null;
        try {
            spell = w.getSpell(base, mage);
            if (spell instanceof com.elmakers.mine.bukkit.spell.BaseSpell bs) ttc = bs.getTimeToCast();
        } catch (Throwable ignored) { }
        if (!CooldownTagPlan.shouldTag(ttc, thresholdSeconds)) {
            if (mk != null) restore(it);
            return;
        }
        long secs = CooldownTagPlan.secondsShown(ttc);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        if (mk != null) {
            // Already ours: re-render from the RECORDED base name, so a
            // repaint or the tap-bar icon lore can never stack suffixes.
            Component baseComp = null;
            try { baseComp = GsonComponentSerializer.gson().deserialize(mk[0]); } catch (Throwable ignored) { }
            if (baseComp == null) { restore(it); stale++; return; }
            String want = CooldownTagPlan.annotated(plain(baseComp), secs);
            if (!want.equals(plain(meta.displayName()))) {
                meta.displayName(baseComp.append(suffixComponent(secs)));
                it.setItemMeta(meta);
                refreshes++;
            }
            return;
        }
        // First annotation of this display copy: record the current name
        // exactly, then append. A nameless icon (never happens with Magic's
        // paint) falls back to the spell's own name.
        Component baseComp = meta.displayName();
        if (baseComp == null) baseComp = Component.text(spell != null ? spell.getName() : base);
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                GsonComponentSerializer.gson().serialize(baseComp));
        meta.getPersistentDataContainer().set(spellKey, PersistentDataType.STRING, base);
        meta.displayName(baseComp.append(suffixComponent(secs)));
        it.setItemMeta(meta);
        tags++;
    }

    // ------------------------------------------------------------------ sweeps and cleanup

    /** Restore the annotated hotbar display (slots 0-8) in place. */
    private void sweepDisplay(Player p) {
        boolean any = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack it = p.getInventory().getItem(slot);
            if (marked(it)) { restore(it); any = true; }
        }
        if (any) sweeps++;
    }

    /**
     * Sweep NOW — called before every WandBars-driven Magic cycle (next to
     * the pager sweep), so Magic's save only ever sees clean icons from any
     * path WandBars initiates. The poll re-annotates on its next pass.
     */
    void sweepNow(Player p) {
        if (p != null) sweepDisplay(p);
    }

    /**
     * Restore annotated copies found anywhere they must never persist:
     * the wand's stored (real) inventory, the bar pages' painted contents
     * and the open list page (restored once the bar is closed — while it
     * is open those are display-adjacent and the render pass owns the
     * display itself), the player's real inventory while no wand inventory
     * is open, and the cursor. Only marker-carrying items are touched.
     */
    int cleanupPlayer(Player p) {
        int n = 0;
        com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
        boolean open = w != null && w.isInventoryOpen();
        if (w != null) {
            if (w.hasStoredInventory()) n += restoreAll(w.getStoredInventory());
            if (w.getHotbars() != null) {
                // While the bar is open, every page EXCEPT the one on
                // screen is storage, not display: a page the player turned
                // away from was saved by that turn (a Magic wand-click
                // cycle saves without our sweep) and is restored here. The
                // current page's contents are display-adjacent — the
                // render pass and the pre-save sweeps own those, and the
                // moment the bar closes this same cleanup takes them too.
                // (The open list page needs no scan: it only ever holds
                // list icons, which this hook never annotates.)
                int current = open ? w.getCurrentHotbar() : -1;
                int b = 0;
                for (com.elmakers.mine.bukkit.wand.WandInventory bar : w.getHotbars()) {
                    if (b++ != current) n += restoreAll(bar);
                }
            }
        }
        if (!open) n += restoreAll(p.getInventory());
        ItemStack cursor = p.getItemOnCursor();
        if (marked(cursor) && restore(cursor)) { p.setItemOnCursor(cursor); n++; }
        if (n > 0) {
            cleanups += n;
            if (cleanupNotified.add(p.getUniqueId())) {
                plugin.getLogger().info("Cooldown name tag cleanup: restored " + n + " annotated icon(s) found outside the hotbar display for " + p.getName() + ".");
            } else if (plugin.debug()) {
                plugin.getLogger().info("Cooldown name tag cleanup: restored " + n + " more annotated icon(s) for " + p.getName() + ".");
            }
        }
        return n;
    }

    /** Plugin disable: leave no annotation behind on any online player. */
    void sweepAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { sweepDisplay(p); cleanupPlayer(p); } catch (Throwable ignored) { }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        // A close is a save point: restore anything that landed outside the
        // display a tick later (and again once the restore has settled).
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 10L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        // An annotated copy must never enter the world as an item: if one
        // is ever dropped (a drag out of the screen mid-arrange), its name
        // is restored before anyone can pick the annotation up.
        ItemStack it = e.getItemDrop().getItemStack();
        if (marked(it) && restore(it)) e.getItemDrop().setItemStack(it);
    }

    // LOWEST: restore our display copies before anyone else's quit
    // handling (Magic's included) can save the open inventory.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        // Runs during the quit dispatch, before the player data is saved:
        // leave the inventory (and the wand's stored state) clean. The
        // page contents are restored here too, whatever state the bar is
        // in. Ordering note (rig-verified): if Magic's own quit handling
        // runs first, it saves the open inventory and deactivates the
        // wand before this hook can reach its page paint — the copies it
        // saved sit only in that deactivated wand object's in-memory
        // paint, which is never persisted (a wand saves its spell MAP,
        // not its painted stacks) and is rebuilt from the map at the
        // next activation. Everything that is persisted or displayed —
        // the player's real inventory, the wand's stored inventory, the
        // spell map — is clean in every ordering.
        // Each step in its own try: one failing step must never skip the others.
        try { sweepDisplay(p); } catch (Throwable ignored) { }
        try { cleanupPlayer(p); } catch (Throwable ignored) { }
        try {
            com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
            if (w != null && w.getHotbars() != null) {
                for (com.elmakers.mine.bukkit.wand.WandInventory bar : w.getHotbars()) restoreAll(bar);
            }
        } catch (Throwable ignored) { }
        disabledFor.remove(p.getUniqueId());
        cleanupNotified.remove(p.getUniqueId());
    }
}
