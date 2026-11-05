package com.winkreyes.excellandmarks.menu;

import com.winkreyes.excellandmarks.ExcelLandmarks;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.core.SpotMenuModel;
import com.winkreyes.excellandmarks.service.LandmarkService;
import com.winkreyes.excellandmarks.store.Store;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dual renderer (SPEC §6): ONE menu structure per open, drawn as a
 * native Floodgate form for Bedrock players and as a chest inventory
 * for Java players — the ExcelMenu 2.0 presentation pattern. Renderers
 * contain no decisions; every label and button comes from the core
 * model (SpotMenuModel) or, for My Landmarks, the same structure built
 * here from registry state.
 */
public final class Menus implements Listener {

    private enum Kind { SPOT_BUTTON, OPEN_SPOT, CLAIM }

    private record MenuAction(Kind kind, SpotMenuModel.Btn btn, String spotId) { }

    private static final class Open {
        String title;
        List<String> lines = List.of();
        final List<String> labels = new ArrayList<>();
        final List<MenuAction> actions = new ArrayList<>();
        String spotId;                    // spot screens: the spot
        SpotMenuModel.Btn pending;        // confirm screens: the pending action
        boolean confirm;                  // confirm screen flag
    }

    public static final class LmHolder implements InventoryHolder {
        final Open open;
        final Map<Integer, Integer> slotToAction = new HashMap<>();
        LmHolder(Open open) { this.open = open; }
        @Override public Inventory getInventory() { return null; }
    }

    private final ExcelLandmarks plugin;
    private final Map<UUID, Open> formSessions = new HashMap<>();

    public Menus(ExcelLandmarks plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ opens

    public void openSpot(Player player, Spot spot) {
        openSpot(player, spot, null);
    }

    public void openSpot(Player player, Spot spot, String notice) {
        SpotMenuModel.Screen screen = SpotMenuModel.mainScreen(spot, player.getUniqueId(),
                System.currentTimeMillis(), plugin.settings().graceDays, notice);
        Open open = fromScreen(screen, spot.id, null, false);
        present(player, open);
    }

    public void openConfirm(Player player, Spot spot, SpotMenuModel.Btn action) {
        SpotMenuModel.Screen screen = SpotMenuModel.confirmScreen(spot, action,
                System.currentTimeMillis(), plugin.settings().rentPeriodDays);
        Open open = fromScreen(screen, spot.id, action, true);
        present(player, open);
    }

    public void openMyLandmarks(Player player) {
        Open open = new Open();
        open.title = "My Landmarks";
        List<Spot> mine = plugin.registry().ownedBy(player.getUniqueId());
        List<String> lines = new ArrayList<>();
        if (mine.isEmpty()) {
            lines.add("You don't hold any spots yet.");
            lines.add("Tap a [Landmark] sign at spawn to rent or buy one.");
        }
        open.lines = lines;
        for (Spot s : mine) {
            String state = s.mode == Spot.Mode.OWNED ? "Owned"
                    : "Rented til " + SpotMenuModel.date(s.paidThroughMs);
            open.labels.add(s.name + " — " + state);
            open.actions.add(new MenuAction(Kind.OPEN_SPOT, null, s.id));
        }
        Store.Virtual v = plugin.store().virtual.get(player.getUniqueId());
        if (v != null && !v.itemBytesB64.isEmpty()) {
            open.labels.add("Claim redemption items (" + v.itemBytesB64.size() + " stacks)");
            open.actions.add(new MenuAction(Kind.CLAIM, null, null));
        }
        open.labels.add("Close");
        open.actions.add(new MenuAction(Kind.SPOT_BUTTON, SpotMenuModel.Btn.CLOSE, null));
        present(player, open);
    }

    private Open fromScreen(SpotMenuModel.Screen screen, String spotId,
                            SpotMenuModel.Btn pending, boolean confirm) {
        Open open = new Open();
        open.title = screen.title();
        open.lines = screen.lines();
        open.spotId = spotId;
        open.pending = pending;
        open.confirm = confirm;
        for (SpotMenuModel.Button b : screen.buttons()) {
            open.labels.add(b.label());
            open.actions.add(new MenuAction(Kind.SPOT_BUTTON, b.id(), null));
        }
        return open;
    }

    // ----------------------------------------------------------- render

    private void present(Player player, Open open) {
        FormPort forms = plugin.forms();
        if (forms.isBedrock(player.getUniqueId())) {
            List<SpotMenuModel.Button> buttons = new ArrayList<>();
            for (int i = 0; i < open.actions.size(); i++) {
                MenuAction a = open.actions.get(i);
                buttons.add(new SpotMenuModel.Button(
                        a.btn() == null ? SpotMenuModel.Btn.CLOSE : a.btn(), open.labels.get(i)));
            }
            SpotMenuModel.Screen screen = new SpotMenuModel.Screen(open.title, open.lines, buttons);
            formSessions.put(player.getUniqueId(), open);
            boolean sent = forms.send(player.getUniqueId(), screen, idx -> {
                if (!player.isOnline()) return;
                if (idx < 0 || idx >= open.actions.size()) return;
                dispatch(player, open, open.actions.get(idx));
            });
            if (sent) return;
            // fall through to the inventory renderer
        }
        openInventory(player, open);
    }

    private void openInventory(Player player, Open open) {
        boolean list = open.spotId == null && !open.confirm;
        int size = list ? 54 : 27;
        LmHolder holder = new LmHolder(open);
        Inventory inv = Bukkit.createInventory(holder, size, trimTitle(open.title));
        if (!list) {
            ItemStack info = item(Material.OAK_SIGN, "§f" + open.title, open.lines);
            inv.setItem(0, info);
        }
        int slot = list ? 0 : 9;
        for (int i = 0; i < open.actions.size(); i++) {
            if (slot >= size) break;
            MenuAction a = open.actions.get(i);
            inv.setItem(slot, item(iconFor(open, a), "§e" + open.labels.get(i),
                    a.kind() == Kind.OPEN_SPOT ? List.of("§7Tap to open") : List.of()));
            holder.slotToAction.put(slot, i);
            slot++;
        }
        player.openInventory(inv);
    }

    private Material iconFor(Open open, MenuAction a) {
        if (a.kind() == Kind.OPEN_SPOT) {
            Spot s = plugin.registry().byId(a.spotId());
            return s != null && s.isPlot() ? Material.GRASS_BLOCK : Material.OAK_SIGN;
        }
        if (a.kind() == Kind.CLAIM) return Material.CHEST;
        return switch (a.btn()) {
            case RENT -> Material.EMERALD;
            case BUY -> Material.GOLD_INGOT;
            case RENEW -> Material.CLOCK;
            case RELEASE -> Material.OAK_DOOR;
            case CONFIRM -> Material.LIME_CONCRETE;
            case BACK -> Material.ARROW;
            case CLOSE -> Material.BARRIER;
            case CLAIM -> Material.CHEST;
        };
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                List<String> coloured = new ArrayList<>();
                for (String l : lore) coloured.add("§7" + l);
                meta.setLore(coloured);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String trimTitle(String t) {
        return t.length() <= 32 ? t : t.substring(0, 32);
    }

    // ---------------------------------------------------------- dispatch

    private void dispatch(Player player, Open open, MenuAction action) {
        switch (action.kind()) {
            case OPEN_SPOT -> {
                Spot s = plugin.registry().byId(action.spotId());
                if (s != null) openSpot(player, s);
            }
            case CLAIM -> claimVirtual(player);
            case SPOT_BUTTON -> spotButton(player, open, action.btn());
        }
    }

    private void spotButton(Player player, Open open, SpotMenuModel.Btn btn) {
        if (btn == SpotMenuModel.Btn.CLOSE) {
            player.closeInventory();
            return;
        }
        Spot spot = plugin.registry().byId(open.spotId);
        if (spot == null) {
            player.closeInventory();
            return;
        }
        LandmarkService svc = plugin.service();
        if (!open.confirm) {
            switch (btn) {
                case RENT, BUY, RENEW, RELEASE -> openConfirm(player, spot, btn);
                default -> player.closeInventory();
            }
            return;
        }
        // confirm screen
        if (btn == SpotMenuModel.Btn.BACK) {
            openSpot(player, spot);
            return;
        }
        if (btn != SpotMenuModel.Btn.CONFIRM) return;
        LandmarkService.Outcome out = switch (open.pending) {
            case RENT -> svc.rent(player, spot);
            case BUY -> svc.buy(player, spot);
            case RENEW -> svc.renew(player, spot);
            case RELEASE -> svc.release(spot);
            default -> new LandmarkService.Outcome(false, "Nothing to confirm.");
        };
        if (out.ok()) {
            player.closeInventory();
            player.sendMessage("§a" + out.message());
        } else {
            openSpot(player, spot, "§c" + out.message());
        }
    }

    private void claimVirtual(Player player) {
        Store.Virtual v = plugin.store().virtual.get(player.getUniqueId());
        if (v == null || v.itemBytesB64.isEmpty()) return;
        List<String> left = new ArrayList<>();
        for (String b64 : v.itemBytesB64) {
            try {
                ItemStack it = Store.decodeItem(b64);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(it);
                for (ItemStack o : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), o);
                }
            } catch (Throwable t) {
                left.add(b64);
                plugin.getLogger().warning("ExcelLandmarks: a virtual redemption item failed to decode (" + t + ")");
            }
        }
        v.itemBytesB64.clear();
        v.itemBytesB64.addAll(left);
        plugin.store().saveRedemptions();
        player.closeInventory();
        player.sendMessage("§aRedemption items claimed. They never expire — anything left is still waiting.");
    }

    // ------------------------------------------------------ inv listeners

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LmHolder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory().getHolder() != holder) return;
        Integer idx = holder.slotToAction.get(e.getRawSlot());
        if (idx == null) return;
        dispatch(player, holder.open, holder.open.actions.get(idx));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof LmHolder) e.setCancelled(true);
    }
}
