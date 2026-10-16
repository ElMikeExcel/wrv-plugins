package com.winkreyes.excelwaypoints;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class ExcelWaypoints extends JavaPlugin implements Listener, Env {
    private PlaceService svc;
    private Controller ctl;

    @Override public void onEnable() {
        saveDefaultConfig();
        PlaceStore store = new PlaceStore(new File(getDataFolder(), "places.yml"), m -> getLogger().warning(m));
        store.load();
        svc = new PlaceService(store, getConfig().getInt("max-places-per-player", 5), getConfig().getStringList("blocked-worlds"),
                getConfig().getLong("visit-request-cooldown-seconds", 30) * 1000L, System::currentTimeMillis);
        Forms forms = new SyncForms(new GeyserForms(getLogger()), r -> Bukkit.getScheduler().runTask(this, r));
        ctl = new Controller(svc, this, forms);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ExcelWaypoints 0.1.0 enabled. " + store.all().size() + " saved places loaded. Bedrock forms: "
                + (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null ? "Geyser found" : "no Geyser, chat menu only"));
    }

    // ---- Env ----
    @Override public PlayerView online(UUID id) { Player p = Bukkit.getPlayer(id); return p == null ? null : new BukkitPlayer(p); }
    @Override public String worldEnvironment(String name) {
        World w = Bukkit.getWorld(name);
        return w == null ? null : w.getEnvironment().name();
    }
    @Override public boolean tpaAvailable() {
        return Bukkit.getPluginManager().getPlugin("Essentials") != null && Bukkit.getPluginManager().isPluginEnabled("Essentials")
                && Bukkit.getPluginCommand("tpa") != null;
    }
    @Override public long now() { return System.currentTimeMillis(); }

    // ---- events ----
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        if (!ctl.waitingForName(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message());
        Player pl = e.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> { if (pl.isOnline()) ctl.chatInput(new BukkitPlayer(pl), msg); });
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) { ctl.playerLeft(e.getPlayer().getUniqueId()); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { svc.refreshOwnerName(e.getPlayer().getUniqueId(), Names.display(e.getPlayer().getName())); }

    // ---- command ----
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("admin")) { admin(s, a); return true; }
        if (!(s instanceof Player pl)) { s.sendMessage("Players only. Staff can use: /places admin list <player> and /places admin delete <id>"); return true; }
        if (!pl.hasPermission("excelwaypoints.use")) { pl.sendMessage("You can't use places."); return true; }
        if (pl.isDead()) { pl.sendMessage("Wait until you respawn, then try again."); return true; }
        ctl.command(new BukkitPlayer(pl), a);
        return true;
    }
    private void admin(CommandSender s, String[] a) {
        if (!s.hasPermission("excelwaypoints.admin")) { s.sendMessage("Staff only."); return; }
        if (a.length >= 3 && a[1].equalsIgnoreCase("list")) {
            String want = Names.display(a[2]).toLowerCase(Locale.ROOT);
            int n = 0;
            for (Place p : svc.store().all()) if (Names.display(p.ownerName).toLowerCase(Locale.ROOT).equals(want)) {
                s.sendMessage(p.id + "  " + p.type.label + " " + p.name + "  " + p.world + " " + Math.round(p.x) + " " + Math.round(p.y) + " " + Math.round(p.z) + (p.shareable ? "  shared" : ""));
                n++;
            }
            s.sendMessage(n + " place(s) for " + a[2]);
        } else if (a.length >= 3 && a[1].equalsIgnoreCase("delete")) {
            PlaceService.Result r = svc.adminDelete(a[2]);
            s.sendMessage(r.ok() ? "Deleted " + r.place.name + " (owner " + r.place.ownerName + ")." : "No place with id " + a[2]);
        } else s.sendMessage("Staff: /places admin list <player> | /places admin delete <id>");
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            List<String> o = new ArrayList<>(List.of("save", "mine", "family", "help"));
            if (s.hasPermission("excelwaypoints.admin")) o.add("admin");
            o.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
            return o;
        }
        return List.of();
    }
}
