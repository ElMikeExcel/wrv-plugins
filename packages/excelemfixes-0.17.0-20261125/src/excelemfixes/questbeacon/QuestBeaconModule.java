package excelemfixes.questbeacon;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.api.QuestDestinationApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * Fix module 16: quest beacon guide (Mike's idea, approved
 * Nov 22, 2026, 11:20 PM CT; edge-riding refinement 11:44 PM).
 *
 * THE GAP: EliteMobs' tracker tells a player WHAT to kill but,
 * for the tracker-blank quests (upstream issue #241), not WHERE
 * — and even where the tracker works, a destination on a compass
 * bar is easy to lose. The quest-destinations module answers
 * with text, coordinates and a short glow; this module adds the
 * always-on-while-tracked visual: while a player has a quest
 * selected/tracked, a temporary pillar of light marks that
 * quest's destination — the same destination the
 * quest-destinations module computes from its data table.
 *
 * The beam is PER-PLAYER and purely visual: particle packets
 * sent only to the owning player (see BeamGuide for why a
 * particle column, not a fake beacon block, is the
 * parity-honest mechanism on Java AND Bedrock). No real beacon
 * or pyramid blocks are ever placed, nothing persists in the
 * world, and other players never see your beam. It ends on
 * untrack, on completion (a finished quest leaves the tracker),
 * or on arrival within a small radius. From far away the beam
 * rides at the edge of the player's view in the destination's
 * direction and settles onto the destination once it is in
 * range; the far leg's text guidance (/questwhere + glow) is
 * unchanged — the beam complements it, never replaces it.
 *
 * Data-driven and expansion-generic: the module knows no quest,
 * place or pack. A quest gets a beam when the quest-destinations
 * table covers it (a data row), and stands down cleanly when
 * EliteMobs or that module is absent.
 *
 * Command surface:
 *   /questbeacon          your beam's current state
 *   /questbeacon off|on   your own beam, this session
 */
public final class QuestBeaconModule
        implements FixModule, CommandExecutor, Listener {

    private ModuleContext ctx;
    private BeamGuide beam;
    private boolean emAvailable;

    @Override
    public String id() {
        return "quest-beacon";
    }

    @Override
    public String displayName() {
        return "Quest Beacon";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        emAvailable = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        if (!emAvailable) {
            ctx.logger().warning("Quest Beacon: EliteMobs not found — "
                    + "the beam idles until EliteMobs is present.");
        }
        if (!QuestDestinationApi.isAvailable()) {
            ctx.logger().warning("Quest Beacon: the quest-destinations "
                    + "module is off or has no table loaded — the beam "
                    + "has no destinations to point at and idles.");
        }
        beam = new BeamGuide(ctx.plugin(), ctx.logger(), emAvailable);
        loadSettings();
        beam.start();
        ctx.registerCommand("questbeacon", List.of(),
                "/questbeacon [on|off]", this);
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Quest Beacon ready — a tracked quest's "
                + "destination gets a per-player pillar of light.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (beam != null) beam.stop();
    }

    @Override
    public void reload() {
        loadSettings();
        if (beam != null) beam.start();
    }

    private void loadSettings() {
        int updateTicks = 10;
        double edgeDistance = 48;
        double arrivalRadius = 8;
        int columnHeight = 40;
        int columnStep = 2;
        ConfigurationSection c = ctx.moduleConfig();
        if (c != null) {
            updateTicks = Math.max(2, Math.min(40,
                    c.getInt("update-ticks", updateTicks)));
            edgeDistance = Math.max(16, Math.min(128,
                    c.getDouble("edge-distance", edgeDistance)));
            arrivalRadius = Math.max(2, Math.min(32,
                    c.getDouble("arrival-radius", arrivalRadius)));
            columnHeight = Math.max(16, Math.min(80,
                    c.getInt("column-height", columnHeight)));
            columnStep = Math.max(1, Math.min(4,
                    c.getInt("column-step", columnStep)));
        }
        beam.configure(updateTicks, edgeDistance, arrivalRadius,
                columnHeight, columnStep);
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The quest beacon is for players.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            beam.setOff(player.getUniqueId(), true);
            player.sendMessage("§7Your quest beacon is off for this "
                    + "session. §f/questbeacon on§7 brings it back.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
            beam.setOff(player.getUniqueId(), false);
            player.sendMessage("§eYour quest beacon is on.");
            return true;
        }
        if (args.length == 0) {
            BeamGuide.BeamState s = beam.state(player.getUniqueId());
            if (beam.isOff(player.getUniqueId())) {
                player.sendMessage("§7Your quest beacon is off "
                        + "(/questbeacon on).");
            } else if (s == null || s.mode() == null) {
                player.sendMessage("§7No beam yet — track a quest and "
                        + "the light appears.");
            } else {
                switch (s.mode()) {
                    case SHOW_EDGE -> player.sendMessage("§eBeacon: §f"
                            + "leading you toward " + s.place() + " (§7"
                            + Math.round(s.distance()) + " blocks away§f)"
                            + " — the pillar rides at the edge of your "
                            + "view in its direction.");
                    case SHOW_SETTLED -> player.sendMessage("§eBeacon: §f"
                            + "standing on " + s.place() + " (§7"
                            + Math.round(s.distance()) + " blocks away§f).");
                    case HIDE_NO_TRACK -> player.sendMessage("§7No beam — "
                            + "no quest is tracked right now.");
                    case HIDE_UNCOVERED -> player.sendMessage("§7No beam — "
                            + "your tracked quest has no destination in "
                            + "the guide table yet. /questwhere may still "
                            + "help.");
                    case HIDE_WRONG_WORLD -> player.sendMessage("§7No beam "
                            + "— " + s.place() + " is in another world.");
                    case HIDE_ARRIVED -> player.sendMessage("§eYou have "
                            + "arrived at §f" + s.place() + "§e — the "
                            + "beacon's job is done.");
                    case HIDE_PLAYER_OFF -> player.sendMessage("§7Your "
                            + "quest beacon is off (/questbeacon on).");
                }
            }
            return true;
        }
        player.sendMessage("§7Usage: /questbeacon [on|off]");
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (beam != null) beam.clearPlayer(event.getPlayer().getUniqueId());
    }
}
