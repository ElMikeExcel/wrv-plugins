// SPDX-License-Identifier: MIT
package com.winkreyes.menustats;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Read-only: watches clicks in ExcelMenu chest menus and appends them to clicks.csv. Never cancels or alters an event. */
public final class ExcelMenuStats extends JavaPlugin implements Listener {
    private ClickLog log;
    private MenuIndex idx;

    @Override public void onEnable() {
        log = new ClickLog(getDataFolder().toPath(), ZoneId.of("America/Chicago"), Clock.systemUTC(), getLogger()::warning);
        reindex();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[MenuStats] READY version=" + getDescription().getVersion() + " menus=" + idx.menus().size() + " failed=" + idx.failed().size());
    }
    @Override public void onDisable() { if (log != null) log.close(); }

    private void reindex() {
        Path dir = getDataFolder().toPath().getParent().resolve("ExcelMenu").resolve("menus");
        idx = MenuIndex.load(dir);
        for (String f : idx.failed()) getLogger().warning("Could not index menu " + f);
    }

    private boolean isExcelMenu(Inventory top) {
        InventoryHolder h = top.getHolder();
        if (h == null) return false;
        try { Plugin p = JavaPlugin.getProvidingPlugin(h.getClass()); return p != null && p.getName().equals("ExcelMenu"); }
        catch (Throwable notPlugin) { return false; }
    }
    private static String reflectId(Object holder) {
        for (String m : new String[]{"getMenuId", "menuId", "getId", "id", "getName", "name"}) {
            try { java.lang.reflect.Method mm = holder.getClass().getMethod(m); if (mm.getParameterCount() == 0) { Object v = mm.invoke(holder); if (v instanceof String s && !s.isBlank()) return s; } }
            catch (Throwable ignored) { }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent e) {
        try {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            Inventory top = e.getView().getTopInventory();
            int raw = e.getRawSlot();
            if (raw < 0 || raw >= top.getSize()) return;
            ItemStack it = e.getCurrentItem();
            if (it == null || it.getType().isAir()) return;
            boolean ours = isExcelMenu(top);
            MenuIndex.Menu menu = null;
            if (ours) { String id = reflectId(top.getHolder()); if (id != null) menu = idx.byId(id); }
            @SuppressWarnings("deprecation") String title = e.getView().getTitle();
            if (menu == null) menu = idx.byTitle(title, top.getSize());
            if (menu == null) { if (!ours) return; }
            String shown = null;
            if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) { @SuppressWarnings("deprecation") String d = it.getItemMeta().getDisplayName(); shown = d; }
            String menuId = menu == null ? "unknown:" + MenuIndex.plain(title) : menu.id();
            String button = menu == null ? "slot" + raw : MenuIndex.button(menu, raw, shown);
            log.record(new ClickLog.Click(java.time.Instant.now(), p.getName(), p.getUniqueId(), platform(p.getUniqueId()), menuId, button, raw, e.getClick().name()));
        } catch (Throwable t) { getLogger().warning("Click not logged: " + t); }
    }

    static String platform(UUID id) {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(inst, id) ? "bedrock" : "java";
        } catch (Throwable noFloodgate) {
            return id.getMostSignificantBits() == 0L ? "bedrock" : "java";
        }
    }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("excelmenustats.admin")) { s.sendMessage(ChatColor.RED + "You cannot view menu stats."); return true; }
        if (a.length == 1 && a[0].equalsIgnoreCase("reload")) { reindex(); s.sendMessage(ChatColor.GREEN + "Menu index reloaded: " + idx.menus().size() + " menus, " + idx.failed().size() + " failed."); return true; }
        int days = 7;
        if (a.length == 1) { try { days = Integer.parseInt(a[0]); } catch (NumberFormatException x) { days = -1; } }
        if (a.length > 1 || days < 1 || days > 3650) { s.sendMessage(ChatColor.YELLOW + "Usage: /menustats [days 1-3650] | /menustats reload"); return true; }
        final int d = days;
        log.stats(d, 15).whenComplete((st, err) -> getServer().getScheduler().runTask(this, () -> {
            if (err != null) { s.sendMessage(ChatColor.RED + "Could not read clicks.csv: " + err.getMessage()); return; }
            s.sendMessage(ChatColor.GOLD + "Menu clicks, last " + d + " day" + (d == 1 ? "" : "s") + ": " + st.clicks() + " clicks by " + st.players() + " players");
            s.sendMessage(ChatColor.GRAY + "Java " + st.java() + " | Bedrock " + st.bedrock());
            int i = 1; for (ClickLog.ButtonStat b : st.top()) s.sendMessage(ChatColor.WHITE + "" + (i++) + ". " + b.key() + ": " + b.clicks() + " clicks (" + b.players() + " players)");
            if (st.top().isEmpty()) s.sendMessage(ChatColor.GRAY + "No clicks logged yet.");
            if (st.badLines() > 0) s.sendMessage(ChatColor.YELLOW + "" + st.badLines() + " unreadable line(s) skipped.");
        }));
        return true;
    }
}
