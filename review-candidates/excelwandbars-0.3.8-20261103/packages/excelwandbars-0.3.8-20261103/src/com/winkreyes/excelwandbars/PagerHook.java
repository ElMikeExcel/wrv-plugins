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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 */
public class PagerHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final NamespacedKey markerKey, pageKey, pagesKey;
    private boolean enabled = true;
    private BukkitTask task;
    private final Map<UUID, Long> renderedState = new HashMap<>();
    private final Set<UUID> disabledFor = new HashSet<>();
    private long renders, flips, protectedEvents, cleanups, blockedRenders, cursorClears;

    PagerHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.markerKey = new NamespacedKey(plugin, "pager_button");
        this.pageKey = new NamespacedKey(plugin, "pager_page");
        this.pagesKey = new NamespacedKey(plugin, "pager_pages");
    }

    void loadSettings(ConfigurationSection tapBar) {
        enabled = tapBar == null || tapBar.getBoolean("pager-buttons.enabled", true);
    }

    void start() {
        if (task != null) task.cancel();
        if (!enabled) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L, 10L);
    }

    String summary() {
        return enabled
                ? "ON (grid slots " + PagerPlan.BACK_SLOT + "/" + PagerPlan.FORWARD_SLOT
                + ", flips " + flips + ", protected " + protectedEvents + ", sweeps " + cleanups + ")"
                : "OFF";
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

    private ItemStack button(PagerPlan.Button which, int page, int pages) {
        boolean forward = which == PagerPlan.Button.FORWARD;
        ItemStack it = new ItemStack(forward ? Material.SPECTRAL_ARROW : Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.displayName(Component.text(forward ? PagerPlan.forwardName() : PagerPlan.backName(),
                NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(PagerPlan.stateLine(page, pages), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Tap to turn the page.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, forward ? "forward" : "back");
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        meta.getPersistentDataContainer().set(pagesKey, PersistentDataType.INTEGER, pages);
        it.setItemMeta(meta);
        return it;
    }

    /** "back" / "forward" when the item is one of our buttons, else null. */
    private String ours(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
    }

    private boolean stateMatches(ItemStack it, String which, int page, int pages) {
        if (!which.equals(ours(it))) return false;
        ItemMeta meta = it.getItemMeta();
        Integer pg = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
        Integer ps = meta.getPersistentDataContainer().get(pagesKey, PersistentDataType.INTEGER);
        return pg != null && ps != null && pg == page && ps == pages;
    }

    // ------------------------------------------------------------------ render / sweep

    private void poll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
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
        int pages = w.getHotbars() == null ? 0 : w.getHotbars().size();
        if (!PagerPlan.shows(pages)) { sweep(p); return; }
        int page = PagerPlan.clamp(w.getCurrentHotbar(), pages);
        long state = ((long) page << 32) | (pages & 0xffffffffL);
        boolean fresh = renderedState.getOrDefault(p.getUniqueId(), -1L) != state;
        place(p, PagerPlan.BACK_SLOT, PagerPlan.Button.BACK, page, pages, fresh);
        place(p, PagerPlan.FORWARD_SLOT, PagerPlan.Button.FORWARD, page, pages, fresh);
        renderedState.put(p.getUniqueId(), state);
    }

    private void place(Player p, int slot, PagerPlan.Button which, int page, int pages, boolean fresh) {
        ItemStack cur = p.getInventory().getItem(slot);
        String want = which == PagerPlan.Button.FORWARD ? "forward" : "back";
        if (stateMatches(cur, want, page, pages)) return;
        if (cur == null || cur.getType().isAir() || ours(cur) != null) {
            p.getInventory().setItem(slot, button(which, page, pages));
            if (fresh) renders++;
            return;
        }
        // A foreign item sits in the button's slot: never overwrite it. The
        // button for this page state simply does not render (counted, not silent).
        blockedRenders++;
        if (plugin.debug()) plugin.getLogger().info("Pager: slot " + slot + " occupied for " + p.getName() + "; button not rendered.");
    }

    /** Remove our buttons (marker-checked only) — used when the bar closes or pages drop below two. */
    private void sweep(Player p) {
        boolean any = false;
        for (int slot : new int[]{PagerPlan.BACK_SLOT, PagerPlan.FORWARD_SLOT}) {
            if (ours(p.getInventory().getItem(slot)) != null) {
                p.getInventory().setItem(slot, null);
                any = true;
            }
        }
        if (any) cleanups++;
        renderedState.remove(p.getUniqueId());
    }

    private void sweepIfBarDown(Player p) {
        TapBarHook tb = plugin.tapBar();
        if (tb != null && tb.barOpen(p)) return; // bar up: render context decides, never sweep mid-session
        if (ours(p.getInventory().getItem(PagerPlan.BACK_SLOT)) != null
                || ours(p.getInventory().getItem(PagerPlan.FORWARD_SLOT)) != null) sweep(p);
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
        if (mh.flipPage(p, which == PagerPlan.Button.FORWARD ? 1 : -1)) {
            flips++;
            renderedState.remove(p.getUniqueId()); // force a state refresh next poll
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        renderedState.remove(e.getPlayer().getUniqueId());
        disabledFor.remove(e.getPlayer().getUniqueId());
    }
}
