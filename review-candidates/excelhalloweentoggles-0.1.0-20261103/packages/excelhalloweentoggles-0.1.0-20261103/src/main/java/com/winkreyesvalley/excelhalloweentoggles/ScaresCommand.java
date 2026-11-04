package com.winkreyesvalley.excelhalloweentoggles;

import cz.softici.server.minecraft.HalloweenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ExcelHalloweenToggles — /scares, the both-platforms floor.
 *
 * Chat is identical on Java and Bedrock, so this command is the
 * guaranteed surface from the first minute; the Server Menu rows ride
 * on top of it (every menu button simply runs /scares &lt;feature&gt;
 * as the player).
 *
 *   /scares                  list your seven switches
 *   /scares &lt;feature&gt;      flip one (key or Halloween event name)
 *   /scares test &lt;feature&gt; op-only: fire one at yourself through the real
 *                            gate + fire path (bypasses only your own
 *                            toggle and the login cooldown)
 */
public final class ScaresCommand implements CommandExecutor, TabCompleter {

    // Pre-translated state labels: they are substituted into messages
    // AFTER color translation, so they carry section signs, not '&'.
    private static final String ON_TXT = "§a✔ ON";
    private static final String OFF_TXT = "§c✘ OFF";

    private final ExcelHalloweenToggles plugin;
    private final HalloweenPlugin halloween;
    private final FeatureRegistry registry;
    private final ToggleStore store;
    private final ScareScheduler scheduler;
    private final JoinPumpkinDriver loginDriver;

    public ScaresCommand(ExcelHalloweenToggles plugin, HalloweenPlugin halloween,
                         FeatureRegistry registry, ToggleStore store,
                         ScareScheduler scheduler, JoinPumpkinDriver loginDriver) {
        this.plugin = plugin;
        this.halloween = halloween;
        this.registry = registry;
        this.store = store;
        this.scheduler = scheduler;
        this.loginDriver = loginDriver;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.text("messages.player-only",
                    "&6[Scares] &7Scare toggles are per-player — run &e/scares &7in game. "
                            + "Admins: &e/halloween trigger &7fires events directly."));
            return true;
        }
        if (!plugin.isEventActive()) {
            player.sendMessage(plugin.text("messages.retired",
                    "&6[Scares] &7The Halloween event has ended — scare toggles are retired. 🎃"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendList(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("test")) {
            return handleTest(player, args);
        }
        FeatureDef feature = registry.resolve(args[0]);
        if (feature == null) {
            player.sendMessage(plugin.text("messages.unknown-feature",
                    "&6[Scares] &cNo scare called '&e%token%&c'. Type &e/scares &cfor the list.")
                    .replace("%token%", args[0]));
            return true;
        }
        boolean now = store.toggle(player.getUniqueId(), feature.key());
        if (!feature.isLogin()) {
            scheduler.ensureFeature(player, feature);
        }
        player.sendMessage(plugin.text("messages.flipped",
                "&6[Scares] &e%feature% &7is now %state%&7.")
                .replace("%feature%", feature.displayName())
                .replace("%state%", now ? ON_TXT : OFF_TXT));
        return true;
    }

    private void sendList(Player player) {
        player.sendMessage(plugin.text("messages.list-header",
                "&6&l🎃 Your Halloween scares &7— tap one in the Server Menu, or &e/scares <name>"));
        for (FeatureDef f : registry.all()) {
            boolean on = store.isEnabled(player.getUniqueId(), f.key());
            player.sendMessage(plugin.text("messages.list-line",
                    "&7- &e%feature%&7: %state%")
                    .replace("%feature%", f.displayName())
                    .replace("%state%", on ? ON_TXT : OFF_TXT));
        }
        player.sendMessage(plugin.text("messages.list-footer",
                "&7Turning one off stops it for you only — everyone else keeps the full event."));
    }

    private boolean handleTest(Player player, String[] args) {
        if (!player.hasPermission("excelhalloweentoggles.test")) {
            player.sendMessage(plugin.text("messages.no-permission",
                    "&6[Scares] &cYou don't have permission for test fires."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.text("messages.test-usage",
                    "&6[Scares] &7Usage: &e/scares test <feature>"));
            return true;
        }
        FeatureDef feature = registry.resolve(args[1]);
        if (feature == null) {
            player.sendMessage(plugin.text("messages.unknown-feature",
                    "&6[Scares] &cNo scare called '&e%token%&c'. Type &e/scares &cfor the list.")
                    .replace("%token%", args[1]));
            return true;
        }
        if (feature.isLogin()) {
            JoinPumpkinDriver.ApplyResult r = loginDriver.tryApply(player, true);
            player.sendMessage(plugin.text("messages.test-login",
                    "&6[Scares] &7Test login pumpkin: &e%result% &7(gate passed; the plugin itself still "
                            + "declines on a full inventory, by its design).")
                    .replace("%result%", r.name()));
            return true;
        }
        if (!halloween.getData().isGlobalEnabled()) {
            player.sendMessage(plugin.text("messages.test-blocked",
                    "&6[Scares] &cTest fire blocked: &e%reason%")
                    .replace("%reason%", "Halloween is globally OFF."));
            return true;
        }
        if (!halloween.isEligible(player)) {
            player.sendMessage(plugin.text("messages.test-blocked",
                    "&6[Scares] &cTest fire blocked: &e%reason%")
                    .replace("%reason%", "you are not eligible here (/halloween disable, world or location rules)."));
            return true;
        }
        boolean fired = halloween.triggerEvent(feature.halloweenEvent(), player, Bukkit.getConsoleSender());
        player.sendMessage(plugin.text("messages.test-fired",
                "&6[Scares] &7Test-fired &e%feature% &7through the real path — plugin accepted: &e%result%&7. "
                        + "(Test fires bypass your toggle on purpose.)")
                .replace("%feature%", feature.displayName())
                .replace("%result%", Boolean.toString(fired)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            for (FeatureDef f : registry.all()) {
                if (f.key().startsWith(p)) {
                    out.add(f.key());
                }
            }
            if ("test".startsWith(p) && sender.hasPermission("excelhalloweentoggles.test")) {
                out.add("test");
            }
            if ("list".startsWith(p)) {
                out.add("list");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            String p = args[1].toLowerCase(Locale.ROOT);
            for (FeatureDef f : registry.all()) {
                if (f.key().startsWith(p)) {
                    out.add(f.key());
                }
            }
        }
        return out;
    }
}
