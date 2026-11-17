// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import java.io.IOException;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExcelSilkWatch extends JavaPlugin {
    private StorageObserver observer;
    private PreferenceStore preferences;
    private PapiHook papi;
    @Override public void onEnable() {
        saveDefaultConfig();
        try { preferences = new PreferenceStore(getDataFolder().toPath().resolve("players.yml")); }
        catch (IOException failure) {
            getLogger().severe("Cannot safely load player settings; disabling without overwriting data: " + failure.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        long cooldown = Math.max(0, Math.min(3600, getConfig().getLong("cooldown-seconds", 3))) * 1000;
        Component message = Component.text(getConfig().getString("message",
                "Warning: you stored an item containing Silk Touch in your ender chest."), NamedTextColor.YELLOW);
        observer = new StorageObserver(task -> getServer().getScheduler().runTask(this, task),
                player -> player.sendActionBar(message), () -> System.nanoTime() / 1_000_000, cooldown,
                preferences::enabled);
        getServer().getPluginManager().registerEvents(observer, this);
        Objects.requireNonNull(getCommand("silkwarnings")).setExecutor(this);
        Objects.requireNonNull(getCommand("silkwarnings")).setTabCompleter(this);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papi = PapiHook.install(this);
            if (papi == null) getLogger().warning("Could not register silkwarnings placeholder; menu state unavailable.");
        }
        getLogger().info("Warning-only observer enabled: native ender chests, bounded shulker contents, persistent player toggle.");
    }

    /** Shared state for menus; no independent menu-side preference cache. */
    public String warningStatus(UUID playerId) {
        return preferences == null || !isEnabled() ? "UNAVAILABLE" : preferences.enabled(playerId) ? "ON" : "OFF";
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This setting is player-only; use /silkwarnings in game."); return true;
        }
        if (!player.hasPermission("excelsilkwatch.toggle")) {
            player.sendMessage(Component.text("You cannot change Silk Touch warnings.", NamedTextColor.RED)); return true;
        }
        if (preferences == null || !isEnabled()) {
            player.sendMessage(Component.text("Silk Touch warning settings are unavailable.", NamedTextColor.RED)); return true;
        }
        String action = args.length == 0 ? "toggle" : args[0].toLowerCase(Locale.ROOT);
        if (args.length > 1 || !Set.of("on", "off", "toggle", "status").contains(action)) {
            player.sendMessage(Component.text("Use /silkwarnings [on|off|toggle|status]", NamedTextColor.YELLOW)); return true;
        }
        UUID id = player.getUniqueId();
        if (!action.equals("status")) {
            boolean next = action.equals("toggle") ? !preferences.enabled(id) : action.equals("on");
            try { preferences.setEnabled(id, next); }
            catch (IOException failure) {
                getLogger().warning("Could not save Silk Touch setting for " + id + ": " + failure.getMessage());
                player.sendMessage(Component.text("Could not save your setting. Silk Touch warnings remain " + warningStatus(id) + ".", NamedTextColor.RED));
                return true;
            }
        }
        player.sendMessage(Component.text("Silk Touch storage warnings: " + warningStatus(id), NamedTextColor.YELLOW));
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("excelsilkwatch.toggle") || args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("on", "off", "toggle", "status").stream().filter(s -> s.startsWith(prefix)).toList();
    }
    @Override public void onDisable() {
        if (observer != null) observer.clear();
        if (papi != null) { papi.unregister(); papi = null; }
    }
}
