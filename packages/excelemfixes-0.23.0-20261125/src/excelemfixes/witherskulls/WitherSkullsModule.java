package excelemfixes.witherskulls;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * The wither-skulls module (module id wither-skulls): elite wither
 * skeletons killed by a player get their skull chance topped up
 * toward a configured target — the fold of the standalone
 * ExcelWitherSkulls 0.1.0 into this pack (consolidation programme
 * Wave 2, ruling D245, Nov 25, 2026). Behavior is 0.1.0's, ported
 * class for class; the /witherskulls command name, its
 * status|reload shape and its excelwitherskulls.admin permission
 * node are exactly the standalone's (frozen per programme gate 8).
 *
 * WHY THIS MODULE EXISTS (player feedback — the REASON): Jester's
 * Oct 21, 2026 feedback — "elite rewards aren't worth it."
 * EliteMobs neither suppresses nor boosts vanilla drops for
 * natural elites (proven from EM bytecode in the Oct 21, 2026
 * rewards audit: its drop-clearing path is gated on a
 * custom-boss-only state and its bonus path on a multiplier this
 * server runs at 0.0), leaving elite wither skeletons at the
 * vanilla 2.5% skull rate. This module is the bonus: it reads
 * EliteMobs' own tagger to know an elite, reads the final vanilla
 * drops list, and when vanilla's roll left no skull it rolls the
 * difference toward the configured target and adds exactly one.
 *
 * OWNER-USABILITY (D246): the module is toggleable
 * (modules.wither-skulls.enabled) and config-driven
 * (target-chance-percent, debug-log). Shipped defaults are
 * target 25.0 / debug off — a generic balance choice any owner
 * can change; no server-specific names, paths or content IDs
 * appear in code or shipped defaults. EliteMobs is reached only
 * through the reflective EmGateway seam (the pack's existing
 * EliteMobs softdepend): if EliteMobs is absent the seam reads
 * "not elite", the top-up stays inert, and the pack boots clean
 * (programme gate 7 — no new hard depend).
 *
 * STATE: none. The module writes no files; uninstall of the fold
 * is the jar swap back plus restoring the standalone jar.
 */
public final class WitherSkullsModule implements FixModule, CommandExecutor, TabCompleter {

    public static final String PERM_ADMIN = "excelwitherskulls.admin";

    private ModuleContext ctx;
    private volatile SkullSettings settings = SkullSettings.defaults();
    private EmGateway gateway;
    private SkullListener listener;

    @Override
    public String id() {
        return "wither-skulls";
    }

    @Override
    public String displayName() {
        return "Wither Skulls";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        settings = loadSettings();
        gateway = new EmGateway(ctx.logger());
        listener = new SkullListener(this::settingsSnapshot, gateway, ctx.logger());
        Bukkit.getPluginManager().registerEvents(listener, ctx.plugin());
        ctx.registerCommand("witherskulls", List.of("wskulls"),
                "/witherskulls <status|reload>", this);
        boolean seam = gateway.available();
        ctx.logger().info("Wither Skulls ready: "
                + (settings.enabled() ? "top-up ON" : "top-up DISABLED by config")
                + ", target skull chance " + settings.targetChancePercent() + "%"
                + " (realized " + String.format("%.2f",
                        SkullMath.realizedTotalPercent(settings.targetChancePercent(), 0))
                + "% with no Looting)"
                + ", EliteMobs seam " + (seam ? "BOUND" : "UNAVAILABLE (top-up inert)"));
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
    }

    @Override
    public void reload() {
        settings = loadSettings();
    }

    /** The live settings snapshot (swapped whole on reload). */
    public SkullSettings settingsSnapshot() {
        return settings;
    }

    boolean seamAvailable() {
        return gateway != null && gateway.available();
    }

    private SkullSettings loadSettings() {
        ConfigurationSection sec = ctx.moduleConfig();
        if (sec == null) return SkullSettings.defaults();
        Boolean enabled = sec.contains("enabled") ? sec.getBoolean("enabled") : null;
        Double target = sec.contains("target-chance-percent")
                ? sec.getDouble("target-chance-percent") : null;
        Boolean debug = sec.contains("debug-log") ? sec.getBoolean("debug-log") : null;
        return SkullSettings.of(enabled, target, debug);
    }

    // -------------------------------------------------- /witherskulls

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("ExcelWitherSkulls: you need the excelwitherskulls.admin permission.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        switch (sub) {
            case "status" -> {
                SkullSettings s = settings;
                sender.sendMessage("ExcelWitherSkulls: "
                        + (s.enabled() ? "top-up ON" : "top-up DISABLED by config")
                        + ", target " + s.targetChancePercent() + "%"
                        + ", debug log " + (s.debugLog() ? "on" : "off")
                        + ", EliteMobs seam " + (seamAvailable() ? "BOUND" : "UNAVAILABLE"));
            }
            case "reload" -> {
                // Re-read the pack config, then swap the snapshot.
                ctx.plugin().reloadConfig();
                settings = loadSettings();
                SkullSettings s = settings;
                sender.sendMessage("ExcelWitherSkulls: reloaded - top-up "
                        + (s.enabled() ? "ON" : "DISABLED") + ", target "
                        + s.targetChancePercent() + "%.");
            }
            default -> sender.sendMessage("ExcelWitherSkulls: usage /witherskulls <status|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERM_ADMIN)) {
            return List.of("status", "reload").stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
