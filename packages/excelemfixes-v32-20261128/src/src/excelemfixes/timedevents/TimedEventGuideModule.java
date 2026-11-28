package excelemfixes.timedevents;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.logging.Logger;

/**
 * The timed-event-guide module (module id timed-event-guide):
 * when an EliteMobs TIMED custom event fires, its boss is
 * guaranteed to spawn close enough to a player to actually
 * reach, and every player nearby is told what appeared and
 * where — a chat line with their own direction and distance,
 * plus a pillar of light standing on the boss while the
 * event lives.
 *
 * WHY THIS MODULE EXISTS (the REASON — observed live,
 * Nov 26, 2026): the Nightbreak Free Goblin Events pack sat
 * installed and enabled on the reference server for ~11
 * days, its ten timed events fired 17 times — and not one
 * player ever fought one. Zero player damage, zero kills,
 * zero of its signature loot in the full server logs and the
 * ExcelProtect record. Reading EliteMobs' own bytecode (the
 * pinned jar) showed why, and none of it is a pack bug:
 *
 *  - EliteMobs DOES anchor a timed event on a player — a
 *    random eligible one — but scatters the boss 24-128
 *    blocks per axis from them (up to ~180 blocks away, in
 *    an unknown direction, through any terrain), and the
 *    anchor can be whoever happens to be online, including
 *    someone AFK or a test bot.
 *  - The event config's announcement is the pack author's
 *    startMessage; this pack sets none, so the event itself
 *    is silent. The boss's own spawn message IS broadcast
 *    ("A Trophy Collector Goblin has appeared!") — with no
 *    location, no direction, no distance. A player who read
 *    it had no way to act on it.
 *  - The event's 30-minute duration was never the problem:
 *    with endEventWithBossDeath at its default true, the
 *    event ends the moment its boss stops existing — and a
 *    boss nobody finds despawns (chunk unload, nobody near)
 *    in as little as 30 seconds.
 *
 * WHAT IT DOES: hooks EliteMobs' own public signals —
 * CustomEventStartEvent to notice a TIMED event, then
 * EliteMobSpawnEvent / EliteMobDeathEvent /
 * EliteMobRemoveEvent for its bosses — and (1) if the boss
 * landed further than relocation-max-distance from the
 * nearest player in its world, re-places it on a safe
 * surface spot inside the [min, max] band around that
 * player (a boss stationary by design at spawn — AI off or
 * zero movement speed — is never moved, only indicated);
 * (2) disarms the vanilla far-away despawn so a found boss
 * is not eaten while a player walks to it; (3) broadcasts
 * one line naming the event, with each player's own
 * direction + distance; (4) stands the pack's established
 * particle pillar on the boss until the event ends.
 * EliteMobs' files, configs and event state are never
 * edited; the only EliteMobs write is the spawned boss's
 * own spawn-location record after a relocation, so its
 * bookkeeping stays truthful.
 *
 * DURATION, DELIBERATELY UNTOUCHED: an event instance
 * shares its config object with EliteMobs' blueprint for
 * that event, so "extending" one running event's duration
 * from outside would rewrite the blueprint for every future
 * firing — there is no clean per-instance lever, and at
 * 30 minutes the cap was never the binding constraint
 * anyway. The despawn guard + close spawn + live guidance
 * close the real gap.
 *
 * OWNER-USABILITY (D246): the module is toggleable
 * (modules.timed-event-guide.enabled) and config-driven
 * (relocation band, broadcast scope, beacon, timers — see
 * config.yml). It scopes to EliteMobs timed custom events
 * GENERALLY: no event, boss, world or pack is named in code
 * or shipped defaults; any content pack's timed events are
 * covered. EliteMobs is the pack's existing softdepend:
 * absent, the module logs one line, stays inert, and the
 * pack boots clean. No state is kept on disk; with the
 * module off, /eventguide does not exist and timed events
 * behave exactly as stock EliteMobs.
 */
public final class TimedEventGuideModule implements FixModule, CommandExecutor, TabCompleter {

    public static final String PERM_ADMIN = "excelemfixes.eventguide.admin";

    private ModuleContext ctx;
    private volatile TimedEventSettings settings = TimedEventSettings.defaults();
    private EmTimedEventBridge bridge;
    private BukkitTask pollTask;
    private boolean emHooked = false;

    @Override
    public String id() {
        return "timed-event-guide";
    }

    @Override
    public String displayName() {
        return "Timed Event Guide";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        settings = loadSettings();
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em != null && em.isEnabled()) {
            // Only now is EmTimedEventBridge loaded — it is
            // the single class that references EliteMobs API
            // types.
            bridge = new EmTimedEventBridge(this);
            Bukkit.getPluginManager().registerEvents(bridge, ctx.plugin());
            emHooked = true;
            startPoll();
        }
        ctx.registerCommand("eventguide", List.of(),
                "/eventguide <status|reload>", this);
        ctx.logger().info("Timed Event Guide ready: " + describe()
                + (emHooked ? ", EliteMobs hooked"
                            : ", EliteMobs NOT found - inert (timed events stay stock)"));
    }

    @Override
    public void onDisable() {
        stopPoll();
        if (bridge != null) {
            HandlerList.unregisterAll(bridge);
            bridge = null;
        }
        emHooked = false;
    }

    @Override
    public void reload() {
        settings = loadSettings();
        stopPoll();
        if (emHooked) startPoll();
    }

    /** The live settings snapshot (swapped whole on reload). */
    public TimedEventSettings settingsSnapshot() {
        return settings;
    }

    Logger logger() {
        return ctx.logger();
    }

    void debug(String line) {
        if (settings.debug()) ctx.logger().info("[timed-event-guide] " + line);
    }

    boolean emHooked() {
        return emHooked;
    }

    int trackedCount() {
        return bridge != null ? bridge.trackedCount() : 0;
    }

    int pendingCount() {
        return bridge != null ? bridge.pendingCount() : 0;
    }

    private void startPoll() {
        pollTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(),
                () -> {
                    if (bridge != null) {
                        try {
                            bridge.poll();
                        } catch (Throwable t) {
                            ctx.logger().warning("Timed Event Guide: poll failed: " + t);
                        }
                    }
                },
                settings.refreshTicks(), settings.refreshTicks());
    }

    private void stopPoll() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
    }

    private String describe() {
        TimedEventSettings s = settings;
        return "relocate " + (s.relocate()
                        ? "ON (band " + trim(s.relocationMinDistance())
                                + "-" + trim(s.relocationMaxDistance()) + " blocks)"
                        : "OFF")
                + ", far-despawn guard " + (s.preventFarDespawn() ? "ON" : "OFF")
                + ", broadcast " + (s.broadcast()
                        ? "ON (" + (s.broadcastServerWide() ? "server" : "world") + ")"
                        : "OFF")
                + ", beacon " + (s.beacon() ? "ON" : "OFF");
    }

    private static String trim(double value) {
        return value == Math.floor(value)
                ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    private TimedEventSettings loadSettings() {
        ConfigurationSection sec = ctx.moduleConfig();
        if (sec == null) return TimedEventSettings.defaults();
        return TimedEventSettings.of(
                sec.contains("enabled") ? sec.getBoolean("enabled") : null,
                sec.contains("relocate") ? sec.getBoolean("relocate") : null,
                sec.contains("relocation-min-distance") ? sec.getDouble("relocation-min-distance") : null,
                sec.contains("relocation-max-distance") ? sec.getDouble("relocation-max-distance") : null,
                sec.contains("safe-spot-attempts") ? sec.getInt("safe-spot-attempts") : null,
                sec.contains("prevent-far-despawn") ? sec.getBoolean("prevent-far-despawn") : null,
                sec.contains("broadcast") ? sec.getBoolean("broadcast") : null,
                sec.contains("broadcast-server-wide") ? sec.getBoolean("broadcast-server-wide") : null,
                sec.contains("beacon") ? sec.getBoolean("beacon") : null,
                sec.contains("beacon-view-distance") ? sec.getDouble("beacon-view-distance") : null,
                sec.contains("column-height") ? sec.getInt("column-height") : null,
                sec.contains("column-step") ? sec.getInt("column-step") : null,
                sec.contains("refresh-ticks") ? sec.getInt("refresh-ticks") : null,
                sec.contains("pending-timeout-seconds") ? sec.getInt("pending-timeout-seconds") : null,
                sec.contains("max-track-minutes") ? sec.getInt("max-track-minutes") : null,
                sec.contains("debug") ? sec.getBoolean("debug") : null);
    }

    // ------------------------------------------------------------ command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("§cYou do not have permission to use /eventguide.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        switch (sub) {
            case "reload" -> {
                reload();
                sender.sendMessage("§aTimed Event Guide reloaded: " + describe()
                        + ", EliteMobs " + (emHooked ? "hooked" : "not hooked (inert)"));
            }
            case "status" -> sender.sendMessage("§eTimed Event Guide: " + describe()
                    + ", EliteMobs " + (emHooked ? "hooked" : "not hooked (inert)")
                    + ", tracking " + trackedCount() + " active / "
                    + pendingCount() + " pending timed events");
            default -> sender.sendMessage("§eUsage: /eventguide <status|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) return List.of("status", "reload");
        return List.of();
    }
}
