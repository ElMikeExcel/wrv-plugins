package com.winkreyes.auditlog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExcelAuditLog extends JavaPlugin implements Listener {
    private AuditWriter writer;
    private MoneyWatcher money;
    private final Map<UUID, Map<String, Integer>> enderOpen = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> recent = new ConcurrentHashMap<>(); // {millis, command}

    @Override public void onEnable() {
        writer = new AuditWriter(getDataFolder());
        getServer().getPluginManager().registerEvents(this, this);
        String eco = "none";
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            try { money = MoneyWatcher.create(); if (money != null) eco = money.name(); } catch (Throwable t) { money = null; }
        }
        if (money != null) {
            for (Player p : Bukkit.getOnlinePlayers()) money.seed(p);
            Bukkit.getScheduler().runTaskTimer(this, () -> money.poll(this), 200L, 200L); // 10 s
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::flushQuiet, 100L, 100L); // 5 s
        getLogger().info("[AuditLog] READY version=" + getDescription().getVersion() + " economy=" + eco
                + " dir=plugins/ExcelAuditLog");
    }

    @Override public void onDisable() {
        if (money != null) for (Player p : Bukkit.getOnlinePlayers()) money.forget(p, this);
        flushQuiet();
    }

    private void flushQuiet() { try { writer.flush(); } catch (Exception e) { getLogger().warning("flush failed: " + e); } }

    static String platform(UUID u) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                Class<?> c = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object api = c.getMethod("getInstance").invoke(null);
                return (Boolean) c.getMethod("isFloodgatePlayer", UUID.class).invoke(api, u) ? "Bedrock" : "Java";
            } catch (Throwable ignored) {}
        }
        return u.getMostSignificantBits() == 0 ? "Bedrock" : "Java";
    }

    void log(Player p, String event, String details) {
        writer.add(p.getName(), p.getUniqueId().toString(), platform(p.getUniqueId()), event, details);
    }

    String recentContext(UUID u) {
        String[] r = recent.get(u);
        if (r == null || System.currentTimeMillis() - Long.parseLong(r[0]) > 15000) return "";
        return r[1];
    }

    static String loc(Location l) {
        if (l == null || l.getWorld() == null) return "?";
        return l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
    }

    static String key(ItemStack it) {
        StringBuilder b = new StringBuilder(it.getType().name());
        ItemMeta m = it.hasItemMeta() ? it.getItemMeta() : null;
        if (m != null) {
            if (m.hasDisplayName()) b.append(" \"").append(m.getDisplayName().replaceAll("\u00a7.", "")).append('"');
            if (!m.getEnchants().isEmpty()) {
                b.append(" {");
                boolean first = true;
                for (Map.Entry<Enchantment, Integer> e : m.getEnchants().entrySet()) {
                    if (!first) b.append(' ');
                    b.append(e.getKey().getKey().getKey()).append(' ').append(e.getValue());
                    first = false;
                }
                b.append('}');
            }
        }
        return b.toString();
    }

    static Map<String, Integer> snapshot(ItemStack[] items) {
        Map<String, Integer> m = new HashMap<>();
        for (ItemStack it : items) if (it != null && !it.getType().isAir()) m.merge(key(it), it.getAmount(), Integer::sum);
        return m;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) { if (money != null) money.seed(e.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (money != null) money.forget(e.getPlayer(), this);
        recent.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        recent.put(e.getPlayer().getUniqueId(), new String[] { Long.toString(System.currentTimeMillis()), msg });
        String low = msg.toLowerCase();
        if (low.startsWith("/pay ") || low.startsWith("/eco ") || low.startsWith("/essentials:pay ")
                || low.startsWith("/essentials:eco ") || low.startsWith("/tpa") || low.startsWith("/tpaccept")
                || low.startsWith("/tpdeny") || low.startsWith("/spawn") || low.startsWith("/warp") || low.startsWith("/home"))
            log(e.getPlayer(), "command", msg);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Location f = e.getFrom(), t = e.getTo();
        if (t == null) return;
        if (f.getWorld() == t.getWorld() && f.distanceSquared(t) < 4) return; // skip tiny nudges
        log(e.getPlayer(), "teleport", "cause=" + e.getCause() + " from=" + loc(f) + " to=" + loc(t)
                + (recentContext(e.getPlayer().getUniqueId()).isEmpty() ? "" : " recent=" + recentContext(e.getPlayer().getUniqueId())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.ENDER_CHEST || !(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        enderOpen.put(p.getUniqueId(), snapshot(p.getEnderChest().getContents()));
        log(p, "enderchest_open", "at=" + loc(p.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() != InventoryType.ENDER_CHEST || !(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        Map<String, Integer> before = enderOpen.remove(p.getUniqueId());
        if (before == null) return;
        String d = ItemDiff.diff(before, snapshot(p.getEnderChest().getContents()));
        log(p, "enderchest_close", d.isEmpty() ? "no change" : d);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent e) {
        log(e.getPlayer(), "xp_level", e.getOldLevel() + " -> " + e.getNewLevel());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        StringBuilder b = new StringBuilder(e.getItem().getType().name()).append(" cost=").append(e.getExpLevelCost()).append(" {");
        boolean first = true;
        for (Map.Entry<Enchantment, Integer> x : e.getEnchantsToAdd().entrySet()) {
            if (!first) b.append(' ');
            b.append(x.getKey().getKey().getKey()).append(' ').append(x.getValue()); first = false;
        }
        log(e.getEnchanter(), "enchant", b.append('}').toString());
    }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length < 1) { s.sendMessage("Usage: /audit <player> [hours] [type]"); return true; }
        int hours = 24; String type = null;
        try { if (a.length > 1) hours = Math.max(1, Math.min(720, Integer.parseInt(a[1]))); }
        catch (NumberFormatException ex) { type = a[1]; }
        if (a.length > 2) type = a[2];
        final String player = a[0]; final int h = hours; final String ty = type;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                writer.flush();
                List<List<String>> rows = writer.query(player, h, ty, 20);
                Bukkit.getScheduler().runTask(this, () -> {
                    s.sendMessage("Audit " + player + " last " + h + "h" + (ty == null ? "" : " type=" + ty) + ": " + rows.size() + " shown (max 20)");
                    for (List<String> r : rows) s.sendMessage(r.get(0).substring(11) + " " + r.get(4) + " " + r.get(5));
                });
            } catch (Exception ex) { getLogger().warning("audit query failed: " + ex); }
        });
        return true;
    }
}
