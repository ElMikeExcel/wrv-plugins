package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.PagerPlan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.3.8: pager buttons in the spell inventory (Mike's settled design, Nov 2
 * 2026 — the buttons AND the wand-click page cycle both stay).
 *
 * While a Tap bar / Tap to pick player's spell bar is open in their own
 * inventory screen, the last two slots of the grid's bottom row (player
 * inventory slots 34 and 35, bottom-right) hold a Back page and a Next page
 * button. The forward button's lore carries the "Page X of Y" state. The
 * buttons flip the wand's page with the same wraparound as the wand click
 * (MagicHook.flipPage), so paging works the same on Java and Bedrock from
 * this one implementation.
 *
 * Safety contract:
 *  - Buttons are OURS: each carries a PDC marker (pager_button = back /
 *    forward, plus the page state it was rendered for). Nothing else is ever
 *    written or removed by this hook: a foreign item found in a button slot
 *    is left exactly where it is and that button simply does not render.
 *  - Hard-protected from the write paths: ArrangeHook's interpreters return
 *    early on any event touching a button slot, and this hook cancels every
 *    click or drag that touches one, so a button can never be lifted,
 *    carried, counted as a spell or written into the loadout. A plain tap on
 *    a button flips the page; every other click type on it is just stopped.
 *  - Presentation only: the buttons exist while the bar is open and the
 *    screen is up, and are swept out (marker-checked) when the bar closes.
 *    Nothing about the loadout model, the wand's spell map or the player's
 *    real items changes.
 *  - Fail-soft: any error disables the pager for that player for the session
 *    and logs once; the wand click cycle always remains.
 *
 * 0.3.9 changes (live reports from Mike, Nov 3 2026):
 *  - EVERY PAGE (defect #1): the buttons now also page the SPELL LIST (the
 *    grid's own pages — the Nov 2 design's original target; 0.3.8 flipped
 *    only hotbar pages). The axis follows the paging the player last used
 *    (config tap-bar.pager-buttons.page-target: auto / bar / list), and a
 *    button whose slot is covered by a list spell icon DISPLACES the icon
 *    for as long as that page is on screen (the design always priced the
 *    two slots against page capacity) — the icon is restored when the
 *    button leaves, and the wand's paint is rebuilt from its spell map
 *    afterward, so no spell can be lost by being covered. Non-spell items
 *    still block a button exactly as in 0.3.8.
 *  - OVERLAYS ONLY (defect #3): a rendered button that was still on screen
 *    when Magic saved the open inventory (any page cycle, any close) was
 *    ejected by Magic into the player's REAL inventory — Mike found a
 *    "◀ Back page" arrow sitting in a live hotbar slot. Two defenses:
 *    every WandBars-driven cycle sweeps the buttons BEFORE Magic saves
 *    (MagicHook.sweepPager), and this hook runs a standing cleanup: any
 *    item carrying the pager marker found in a player's real inventory
 *    (or in the wand's stored inventory while the bar is open) is removed
 *    on sight — on join, after closes, and on the render poll. A button
 *    never overwrote a stored spell: it only ever sat in display slots
 *    Magic itself refuses to record (Magic's save ejects foreign items
 *    rather than storing them), and its landing slot in the real inventory
 *    was chosen by add-item semantics (first free slot), so removing the
 *    button restores the exact pre-leak state; nothing else is touched.
 *
 * 0.3.12 changes (Mike's Nov 7 pager reports; TAPBAR-PAGING-INVESTIGATION):
 *  - THE LIST IS THE DEFAULT: with no explicit page-target the buttons
 *    page the spell list (PagerPlan.resolveTarget / axisForTarget) —
 *    the "auto defaults to the hotbar" trap is retired, in the config
 *    resolution, in autoAxis's starting axis, and in flip's fallback.
 *    An explicit page-target still wins verbatim.
 *  - AXIS-HONEST LORE: the BAR state line now reads "Hotbar page X of
 *    Y" (PagerPlan.stateLine); the LIST line already read "List page
 *    X of Y". A player is never told one axis while the button turns
 *    the other. Bar paging itself is unchanged and still lives on the
 *    sneak flip and the wand controls, exactly as designed.
 */
public class PagerHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final NamespacedKey markerKey, pageKey, pagesKey;
    private boolean enabled = true;
    // 0.3.12: the code default is "list" (see PagerPlan.resolveTarget) —
    // the field starts there too, so even a hook that never saw a config
    // section pages the spell list, never the hotbar.
    private String pageTarget = "list";
    private boolean displace = true;
    private boolean cleanup = true;
    private BukkitTask task;
    private final Map<UUID, String> renderedState = new HashMap<>();
    private final Set<UUID> disabledFor = new HashSet<>();
    // 0.3.9 axis tracking: the axis each player's buttons currently page,
    // the last hotbar page seen, a fingerprint of the list page on screen,
    // and the list page the buttons believe is showing.
    private final Map<UUID, PagerPlan.Axis> axisOf = new HashMap<>();
    private final Map<UUID, Integer> lastBarPage = new HashMap<>();
    private final Map<UUID, Long> lastGridPrint = new HashMap<>();
    private final Map<UUID, Integer> listPageOf = new HashMap<>();
    // 0.3.9 displacement: grid slot -> the list icon a button is covering.
    private final Map<UUID, Map<Integer, ItemStack>> displaced = new HashMap<>();
    private final Set<UUID> repairNeeded = new HashSet<>();
    private final Set<UUID> cleanupNotified = new HashSet<>();
    private long renders, flips, protectedEvents, cleanups, blockedRenders, cursorClears;
    private long displacements, restores, repairs, leaksStripped;

    PagerHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.markerKey = new NamespacedKey(plugin, "pager_button");
        this.pageKey = new NamespacedKey(plugin, "pager_page");
        this.pagesKey = new NamespacedKey(plugin, "pager_pages");
    }

    void loadSettings(ConfigurationSection tapBar) {
        enabled = tapBar == null || tapBar.getBoolean("pager-buttons.enabled", true);
        ConfigurationSection pb = tapBar == null ? null : tapBar.getConfigurationSection("pager-buttons");
        // 0.3.12: an absent section or an unset key resolves to the LIST
        // axis in PagerPlan.resolveTarget (the code default — this is the
        // fix that makes the Nov 8 config stopgap permanent: the live
        // config carried no tap-bar section at all for most of 0.3.11's
        // life and the old "auto" code default paged the hotbar). An
        // explicitly set page-target still wins, verbatim.
        String target = pb == null ? null : pb.getString("page-target");
        pageTarget = PagerPlan.resolveTarget(target);
        displace = pb == null || pb.getBoolean("displace-icons", true);
        cleanup = pb == null || pb.getBoolean("cleanup-leaks", true);
    }

    void start() {
        if (task != null) task.cancel();
        if (!enabled && !cleanup) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L, 10L);
    }

    String summary() {
        return enabled
                ? "ON (grid slots " + PagerPlan.BACK_SLOT + "/" + PagerPlan.FORWARD_SLOT
                + ", target " + pageTarget + ", flips " + flips + ", protected " + protectedEvents
                + ", sweeps " + cleanups + ", displaced " + displacements + ", leaks stripped " + leaksStripped + ")"
                : "OFF (leak cleanup " + (cleanup ? "ON, stripped " + leaksStripped : "OFF") + ")";
    }

    // ------------------------------------------------------------------ context

    private boolean uses(Player p) {
        StyleHook s = plugin.styleHook();
        return s != null && s.enabled() && s.styleOf(p).opensOnHold();
    }

    private com.elmakers.mine.bukkit.wand.Wand implWand(Player p) {
        Mage m = api.getMage(p);
        Wand w = m == null ? null : m.getActiveWand();
        return w instanceof com.elmakers.mine.bukkit.wand.Wand impl ? impl : null;
    }

    private boolean ownScreen(Player p) {
        InventoryType t = p.getOpenInventory().getTopInventory().getType();
        return t == InventoryType.CRAFTING || t == InventoryType.PLAYER || t == InventoryType.CREATIVE;
    }

    /** Full render context: pager on, style pages on hold, bar open, own screen up. */
    private boolean active(Player p) {
        if (!enabled || disabledFor.contains(p.getUniqueId())) return false;
        if (plugin.skipWorld(p.getWorld()) || !uses(p)) return false;
        TapBarHook tb = plugin.tapBar();
        return tb != null && tb.barOpen(p) && ownScreen(p);
    }

    // ------------------------------------------------------------------ buttons

    private ItemStack button(PagerPlan.Button which, PagerPlan.Axis axis, int page, int pages) {
        boolean forward = which == PagerPlan.Button.FORWARD;
        ItemStack it = new ItemStack(forward ? Material.SPECTRAL_ARROW : Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.displayName(Component.text(forward ? PagerPlan.forwardName() : PagerPlan.backName(),
                NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text(axis == PagerPlan.Axis.LIST
                        ? PagerPlan.listStateLine(page, pages) : PagerPlan.stateLine(page, pages),
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Tap to turn the page.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, forward ? "forward" : "back");
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        meta.getPersistentDataContainer().set(pagesKey, PersistentDataType.INTEGER, pages);
        it.setItemMeta(meta);
        return it;
    }

    /** 0.3.12: is this item one of our pager buttons? (PresentedSweep's exclusion check.) */
    boolean isPagerButton(ItemStack it) { return ours(it) != null; }

    /** "back" / "forward" when the item is one of our buttons, else null. */
    private String ours(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        return it.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
    }

    private boolean stateMatches(ItemStack it, String which, int page, int pages) {
        if (!which.equals(ours(it))) return false;
        ItemMeta meta = it.getItemMeta();
        Integer pg = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
        Integer ps = meta.getPersistentDataContainer().get(pagesKey, PersistentDataType.INTEGER);
        return pg != null && ps != null && pg == page && ps == pages;
    }

    /** Is this item a spell icon of the open presentation? (Only those may be displaced.) */
    private boolean spellIcon(ItemStack it) {
        MagicHook mh = plugin.magicHook();
        return mh != null && it != null && mh.spellKeyOf(it) != null;
    }

    // ------------------------------------------------------------------ axis tracking

    /** A cheap fingerprint of the list page on screen (spell keys in the grid, button slots excluded). */
    private long gridPrint(Player p) {
        MagicHook mh = plugin.magicHook();
        if (mh == null) return 0L;
        long h = 1125899906842597L;
        for (int slot = 9; slot <= 33; slot++) {
            ItemStack it = p.getInventory().getItem(slot);
            String k = it == null ? null : mh.spellKeyOf(it);
            h = 31 * h + (k == null ? 0 : k.hashCode());
        }
        return h;
    }

    /**
     * Learn the axis from the player's own paging: a hotbar page change means
     * they are paging the bar; a list-page fingerprint change with the bar
     * page standing still means they are paging the list (the wand click).
     */
    private void trackAxis(Player p, int barPage) {
        UUID id = p.getUniqueId();
        Integer lastBar = lastBarPage.put(id, barPage);
        long print = gridPrint(p);
        Long lastPrint = lastGridPrint.put(id, print);
        if (lastBar != null && lastBar != barPage) {
            axisOf.put(id, PagerPlan.Axis.BAR);
        } else if (lastPrint != null && lastPrint != print && lastBar != null) {
            axisOf.put(id, PagerPlan.Axis.LIST);
        }
    }

    private PagerPlan.Axis axisFor(Player p, int barPages, int listPages) {
        // 0.3.12: the resolution (pin wins when it can page; "auto" starts
        // on the list) lives in PagerPlan.axisForTarget, unit-tested.
        return PagerPlan.axisForTarget(pageTarget, axisOf.get(p.getUniqueId()), barPages, listPages);
    }

    // ------------------------------------------------------------------ render / sweep

    private void poll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                if (cleanup) cleanupPlayer(p);
                if (active(p)) render(p);
                else sweepIfBarDown(p);
            } catch (Throwable t) {
                if (disabledFor.add(p.getUniqueId())) {
                    plugin.getLogger().warning("Pager buttons off for " + p.getName() + " this session (" + t + "); the wand click still flips pages.");
                }
            }
        }
    }

    private void render(Player p) {
        com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
        if (w == null) return;
        int barPages = w.getHotbars() == null ? 0 : w.getHotbars().size();
        MagicHook mh = plugin.magicHook();
        int listPages = mh == null ? 1 : mh.listPageCount(p);
        if (!PagerPlan.shows(barPages, listPages)) { sweep(p); return; }
        int barPage = PagerPlan.clamp(w.getCurrentHotbar(), barPages);
        // Stale displacement: the list page turned under the buttons (a
        // repaint or a wand-click cycle — our own flips restore first, in
        // sweep). The covered icons belong to a page no longer on screen;
        // their spells are safe in the wand's map, so drop the memory and
        // rebuild the paint from the map instead of restoring wrongly.
        Long prevPrint = lastGridPrint.get(p.getUniqueId());
        if (prevPrint != null && prevPrint != gridPrint(p) && displaced.containsKey(p.getUniqueId())) {
            displaced.remove(p.getUniqueId());
            repairNeeded.add(p.getUniqueId());
            repairIfNeeded(p);
        }
        trackAxis(p, barPage);
        PagerPlan.Axis axis = axisFor(p, barPages, listPages);
        axisOf.put(p.getUniqueId(), axis);
        int page = axis == PagerPlan.Axis.LIST
                ? PagerPlan.clamp(listPageOf.getOrDefault(p.getUniqueId(), 0), listPages) : barPage;
        int pages = axis == PagerPlan.Axis.LIST ? listPages : barPages;
        if (axis == PagerPlan.Axis.LIST) listPageOf.put(p.getUniqueId(), page);
        String state = axis + ":" + page + "/" + pages;
        boolean fresh = !state.equals(renderedState.get(p.getUniqueId()));
        place(p, PagerPlan.BACK_SLOT, PagerPlan.Button.BACK, axis, page, pages, fresh);
        place(p, PagerPlan.FORWARD_SLOT, PagerPlan.Button.FORWARD, axis, page, pages, fresh);
        renderedState.put(p.getUniqueId(), state);
    }

    private void place(Player p, int slot, PagerPlan.Button which, PagerPlan.Axis axis, int page, int pages, boolean fresh) {
        ItemStack cur = p.getInventory().getItem(slot);
        String want = which == PagerPlan.Button.FORWARD ? "forward" : "back";
        if (stateMatches(cur, want, page, pages)) return;
        boolean empty = cur == null || cur.getType().isAir();
        boolean our = ours(cur) != null;
        PagerPlan.SlotFate fate = PagerPlan.classify(empty, our, !empty && !our && spellIcon(cur));
        switch (fate) {
            case FREE -> {
                p.getInventory().setItem(slot, button(which, axis, page, pages));
                if (fresh) renders++;
            }
            case DISPLACE -> {
                if (!displace) { blockedRenders++; return; }
                // Cover the list icon for as long as this page is on screen;
                // it is restored when the button leaves (sweep), and the
                // wand's paint is rebuilt from its spell map afterward.
                displaced.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(slot, cur.clone());
                displacements++;
                p.getInventory().setItem(slot, button(which, axis, page, pages));
                if (fresh) renders++;
            }
            case BLOCKED -> {
                // A foreign (non-spell) item sits in the button's slot: never
                // overwrite it. The button simply does not render (counted).
                blockedRenders++;
                if (plugin.debug()) plugin.getLogger().info("Pager: slot " + slot + " occupied for " + p.getName() + "; button not rendered.");
            }
        }
    }

    /**
     * Put back any list icons the buttons are covering. An icon whose button
     * is still on screen is restored in place; one whose button a repaint
     * already wiped cannot be (the display no longer shows its page) — the
     * spell itself is safe in Magic's spell map, and the paint is rebuilt
     * from the map (repairInventoryPaint) so the icon returns.
     */
    private void restoreDisplaced(Player p) {
        Map<Integer, ItemStack> mine = displaced.remove(p.getUniqueId());
        if (mine == null || mine.isEmpty()) return;
        for (Map.Entry<Integer, ItemStack> e : mine.entrySet()) {
            if (ours(p.getInventory().getItem(e.getKey())) != null) {
                p.getInventory().setItem(e.getKey(), e.getValue());
                restores++;
            } else {
                repairNeeded.add(p.getUniqueId());
            }
        }
    }

    private void repairIfNeeded(Player p) {
        if (!repairNeeded.remove(p.getUniqueId())) return;
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        repairs++;
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) mh.repairInventoryPaint(p); });
    }

    /** Remove our buttons (marker-checked only) — used when the bar closes or pages drop below two. */
    private void sweep(Player p) {
        restoreDisplaced(p);
        boolean any = false;
        for (int slot : new int[]{PagerPlan.BACK_SLOT, PagerPlan.FORWARD_SLOT}) {
            if (ours(p.getInventory().getItem(slot)) != null) {
                p.getInventory().setItem(slot, null);
                any = true;
            }
        }
        if (any) cleanups++;
        renderedState.remove(p.getUniqueId());
        repairIfNeeded(p);
    }

    /**
     * 0.3.9: sweep NOW — called before every WandBars-driven Magic cycle, so
     * Magic's save never sees a button (defect #3). Safe to call at any
     * time; the poll re-renders on the next pass.
     */
    void sweepNow(Player p) {
        if (p != null) sweep(p);
    }

    private void sweepIfBarDown(Player p) {
        TapBarHook tb = plugin.tapBar();
        if (tb != null && tb.barOpen(p)) return; // bar up: render context decides, never sweep mid-session
        if (ours(p.getInventory().getItem(PagerPlan.BACK_SLOT)) != null
                || ours(p.getInventory().getItem(PagerPlan.FORWARD_SLOT)) != null
                || displaced.containsKey(p.getUniqueId())) sweep(p);
        // Bar down and no buttons on screen: forget the session's axis state,
        // so the next open starts on page 1 of the list like Magic does.
        axisOf.remove(p.getUniqueId());
        lastBarPage.remove(p.getUniqueId());
        lastGridPrint.remove(p.getUniqueId());
        listPageOf.remove(p.getUniqueId());
    }

    // ------------------------------------------------------------------ leak cleanup (0.3.9, defect #3)

    /**
     * Remove pager-marker items from a player's REAL inventory: the wand's
     * stored inventory while the bar is open (where Magic's save ejects
     * them), or the player's own inventory when no wand inventory is open.
     * Only marker-carrying items are touched; the slot is left empty. The
     * display inventory of an open bar is NOT scanned (the buttons showing
     * there are the legitimate render, owned by render/sweep).
     */
    int cleanupPlayer(Player p) {
        int removed = 0;
        com.elmakers.mine.bukkit.wand.Wand w = implWand(p);
        if (w != null && w.hasStoredInventory()) {
            Inventory stored = w.getStoredInventory();
            if (stored != null) removed += strip(stored);
        } else if (w == null || !w.isInventoryOpen()) {
            removed += strip(p.getInventory());
        }
        if (removed > 0) {
            leaksStripped += removed;
            if (cleanupNotified.add(p.getUniqueId())) {
                plugin.getLogger().info("Pager cleanup: removed " + removed + " leaked pager button(s) from " + p.getName()
                        + "'s real inventory (0.3.8 leak; buttons are screen-only now).");
            } else if (plugin.debug()) {
                plugin.getLogger().info("Pager cleanup: removed " + removed + " more leaked button(s) from " + p.getName() + ".");
            }
        }
        return removed;
    }

    private int strip(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            if (ours(inv.getItem(i)) != null) {
                inv.setItem(i, null);
                n++;
            }
        }
        return n;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!cleanup) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!cleanup || !(e.getPlayer() instanceof Player p)) return;
        // A close is a save point: anything ejected lands in the real
        // inventory as it is restored — strip it a tick later (and again
        // once the restore has fully settled).
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) cleanupPlayer(p); }, 10L);
    }

    // ------------------------------------------------------------------ interception

    /** True when this click touches a button slot of the player's own inventory. */
    static boolean touchesButton(InventoryClickEvent e) {
        return e.getClickedInventory() != null
                && e.getClickedInventory().getType() == InventoryType.PLAYER
                && PagerPlan.isButtonSlot(e.getSlot());
    }

    static boolean touchesButton(InventoryDragEvent e) {
        int topSize = e.getView().getTopInventory().getSize();
        for (int raw : e.getRawSlots()) {
            if (raw < topSize) continue;
            if (PagerPlan.isButtonSlot(e.getView().convertSlot(raw))) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!enabled || !(e.getWhoClicked() instanceof Player p)) return;
        boolean onButton = touchesButton(e);
        boolean cursorOurs = ours(e.getCursor()) != null;
        if (!onButton && !cursorOurs) return;
        if (!active(p)) return;
        e.setCancelled(true);
        protectedEvents++;
        if (cursorOurs && !onButton) {
            // A button should never be on a cursor (every touch is cancelled);
            // if one ever gets there, take it back rather than let it travel.
            Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) { p.setItemOnCursor(null); cursorClears++; } });
            return;
        }
        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT) {
            // A bare tap flips. A tap carrying an icon is a place attempt that
            // landed on a protected slot: stopped above, never a page flip.
            ItemStack cursor = e.getCursor();
            if (cursor == null || cursor.getType().isAir()) flip(p, PagerPlan.buttonAt(e.getSlot()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (!enabled || !(e.getWhoClicked() instanceof Player p)) return;
        if (!touchesButton(e) || !active(p)) return;
        e.setCancelled(true);
        protectedEvents++;
    }

    private void flip(Player p, PagerPlan.Button which) {
        if (which == PagerPlan.Button.NONE) return;
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        int dir = which == PagerPlan.Button.FORWARD ? 1 : -1;
        // 0.3.12: a flip with no rendered axis yet (a tap the same tick
        // the bar opened) defaults to the LIST, matching the code
        // default — 0.3.11 fell back to BAR here, the trap's last door.
        PagerPlan.Axis axis = axisOf.getOrDefault(p.getUniqueId(), PagerPlan.Axis.LIST);
        boolean flipped;
        if (axis == PagerPlan.Axis.LIST) {
            int pages = mh.listPageCount(p);
            flipped = mh.flipListPage(p, dir);
            if (flipped) {
                listPageOf.put(p.getUniqueId(),
                        PagerPlan.target(listPageOf.getOrDefault(p.getUniqueId(), 0), pages, dir));
            }
        } else {
            flipped = mh.flipPage(p, dir);
        }
        if (flipped) {
            flips++;
            renderedState.remove(p.getUniqueId()); // force a state refresh next poll
            lastGridPrint.remove(p.getUniqueId()); // our own flip is not the player paging by hand
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        renderedState.remove(id);
        disabledFor.remove(id);
        axisOf.remove(id);
        lastBarPage.remove(id);
        lastGridPrint.remove(id);
        listPageOf.remove(id);
        displaced.remove(id);
        repairNeeded.remove(id);
        cleanupNotified.remove(id);
    }
}
