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
    private java.util.Map<String, String> overrides = java.util.Map.of();

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
        java.nio.file.Path map = getDataFolder().toPath().resolve("bedrock-map.txt");
        if (!java.nio.file.Files.exists(map)) try { java.nio.file.Files.createDirectories(map.getParent()); java.nio.file.Files.writeString(map, "# Optional: map a Bedrock form tap to a Java menu button so both count on one row.\n# Form Title > Button Label: menu_id > button_key\n# Example: Server Menu > Adventurers Guild: server_menu > guild_open\n"); } catch (java.io.IOException ignored) { }
        overrides = MenuIndex.loadOverrides(map);
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

    /**
     * Public hook for other plugins (ExcelBedrockMenu 1.5.15+ calls this by reflection, no compile link).
     * Thread-safe; never throws. menu/button are human-readable names; index is the button position.
     */
    public boolean recordTap(Player p, String menu, String button, int index, String platform) {
        try {
            if (log == null || p == null) return false;
            String m = MenuIndex.plain(menu == null ? "" : menu); String b = MenuIndex.plain(button == null ? "" : button);
            int nl = b.indexOf('\n'); if (nl >= 0) b = b.substring(0, nl).trim();
            String[] mapped = idx.mapForm(m, b, overrides);
            if (mapped != null) { m = mapped[0]; b = mapped[1]; }
            if (m.isEmpty()) m = "bedrock_form"; if (b.isEmpty()) b = "button" + index;
            String pf = platform == null || platform.isBlank() ? platform(p.getUniqueId()) : platform.toLowerCase(java.util.Locale.ROOT);
            log.record(new ClickLog.Click(java.time.Instant.now(), p.getName(), p.getUniqueId(), pf, m, b, index, "FORM"));
            return true;
        } catch (Throwable t) { getLogger().warning("Tap not logged: " + t); return false; }
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
            s.sendMessage(ChatColor.GOLD + "Menu clicks, last " + d + " day" + (d == 1 ? "" : "s") + ": " + ClickLog.split(st.java(), st.bedrock()) + " (" + st.players() + " players)");
            int i = 1; for (ClickLog.ButtonStat b : st.top()) s.sendMessage(ChatColor.WHITE + "" + (i++) + ". " + b.key() + ": " + ClickLog.split(b.java(), b.bedrock()) + " (" + b.players() + " player" + (b.players() == 1 ? "" : "s") + ")");
            if (st.top().isEmpty()) s.sendMessage(ChatColor.GRAY + "No clicks logged yet.");
            if (st.badLines() > 0) s.sendMessage(ChatColor.YELLOW + "" + st.badLines() + " unreadable line(s) skipped.");
        }));
        return true;
    }
}
