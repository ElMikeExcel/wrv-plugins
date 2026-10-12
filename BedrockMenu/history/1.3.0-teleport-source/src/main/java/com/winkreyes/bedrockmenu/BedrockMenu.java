package com.winkreyes.bedrockmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.api.GeyserApi;

import java.util.List;
import java.util.UUID;

public class BedrockMenu extends JavaPlugin implements Listener {

    private record Entry(String label, String action) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("Spawn", "spawn"),
            new Entry("Set Home", "sethome"),
            new Entry("Home", "home"),
            new Entry("Teleport to a player", "tpmenu"),
            new Entry("Jobs & Money", "jobs browse"),
            new Entry("mcMMO Stats", "mcstats"),
            new Entry("Server Info", "motd"),
            new Entry("Auto-open on join", "menujoin"));

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("menu").setExecutor(this);
        TeleportMenu tp = new TeleportMenu(this);
        getCommand("tpmenu").setExecutor(tp);
        getServer().getPluginManager().registerEvents(tp, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BedrockMenu enabled (Geyser " + (geyserPresent() ? "found" : "not found") + ").");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /menu.");
            return true;
        }
        openMenu(player);
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (getConfig().getBoolean("auto-open." + p.getUniqueId(), false)) {
            Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) openMenu(p); }, 60L);
        }
    }

    private boolean geyserPresent() {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return g != null && g.isEnabled();
    }

    private void openMenu(Player player) {
        if (geyserPresent()) {
            try {
                if (sendBedrockForm(player)) return;
            } catch (Throwable t) {
                getLogger().warning("Bedrock menu failed for " + player.getName() + ": " + t);
            }
        }
        // Java player, or Geyser missing
        Plugin dm = Bukkit.getPluginManager().getPlugin("DeluxeMenus");
        if (dm != null && dm.isEnabled()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open server_menu " + player.getName());
        } else {
            player.sendMessage(ChatColor.GOLD + "The menu is for Bedrock players. Try "
                    + ChatColor.YELLOW + "/spawn, /home, /sethome, /jobs, /mcstats" + ChatColor.GOLD + " instead!");
        }
    }

    /** Isolated so Geyser classes are only touched when Geyser is loaded. */
    private boolean sendBedrockForm(Player player) {
        UUID id = player.getUniqueId();
        GeyserApi api = GeyserApi.api();
        if (!api.isBedrockPlayer(id)) return false;

        SimpleForm.Builder b = SimpleForm.builder()
                .title("Server Menu")
                .content("What would you like to do?");
        for (Entry e : ENTRIES) b.button(e.label());
        b.validResultHandler(response -> {
            int i = response.clickedButtonId();
            if (i < 0 || i >= ENTRIES.size()) return;
            String action = ENTRIES.get(i).action();
            // Form responses arrive on a Geyser network thread; hop to the main thread.
            Bukkit.getScheduler().runTask(this, () -> { if (player.isOnline()) runAction(player, action); });
        });
        boolean ok = api.sendForm(id, b.build());
        if (!ok) getLogger().warning("sendForm returned false for " + player.getName());
        return ok;
    }

    private void runAction(Player player, String action) {
        switch (action) {
            case "spawn" -> teleportToSpawn(player);
            case "tpmenu" -> TeleportMenu.open(this, player);
            case "menujoin" -> {
                String key = "auto-open." + player.getUniqueId();
                boolean now = !getConfig().getBoolean(key, false);
                getConfig().set(key, now);
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "Menu auto-open on join: " + (now ? "ON" : "OFF"));
            }
            default -> player.performCommand(action);
        }
    }

    private void teleportToSpawn(Player player) {
        Location loc = essentialsSpawn(player);
        if (loc == null) {
            World w = Bukkit.getWorlds().get(0);
            loc = w.getSpawnLocation().add(0.5, 0, 0.5);
        }
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teleported to spawn!");
    }

    /** EssentialsX Spawn's /setspawn location, if set. Optional, so done via a guarded lookup. */
    private Location essentialsSpawn(Player player) {
        Plugin sp = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
        if (sp == null || !sp.isEnabled()) return null;
        try {
            Object l = sp.getClass().getMethod("getSpawn", String.class).invoke(sp, "default");
            return (l instanceof Location loc && loc.getWorld() != null) ? loc : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
