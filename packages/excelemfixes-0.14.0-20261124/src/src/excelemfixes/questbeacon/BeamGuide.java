package excelemfixes.questbeacon;

import excelemfixes.api.QuestDestinationApi;
import excelemfixes.questdest.QuestDestinations;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The beam itself: a per-player pillar of light, refreshed on a
 * timer while a player has a quest tracked.
 *
 * WHY PARTICLES, NOT A REAL BEACON BLOCK: a beacon's beam is
 * rendered by its block entity after the pyramid validates; a
 * per-player fake block (this pack is Bukkit-API-only — no packet
 * library, by its public zero-dependency design) carries no
 * ticking block entity, so no beam renders from it, and Geyser
 * translation of a fake beacon block entity is not a provable
 * Bedrock path. An end crystal's beam target renders only on
 * Java. The parity-honest mechanism — the one that behaves
 * identically for Java and Bedrock players — is a particle
 * column sent ONLY to the owning player through
 * Player.spawnParticle (per-player by API contract; Geyser
 * translates the particle packets, and both particle types used
 * here have Bedrock equivalents). Nothing is placed in the
 * world: no blocks, no entities, nothing persists, and no other
 * player ever sees your beam.
 *
 * The column stands where BeamGeometry puts it: riding the edge
 * of the player's view toward the destination from far away,
 * settled on the destination itself once it is in range. The
 * far leg is still served by /questwhere + the glow — the beam
 * complements that guidance, it does not replace it.
 */
public final class BeamGuide {

    /** One player's live beam state, for /questbeacon status. */
    public record BeamState(String questFile, String questName,
                            String place, BeamPolicy.Mode mode,
                            double distance) {}

    private final JavaPlugin plugin;
    private final Logger log;
    private final boolean emAvailable;

    private int updateTicks = 10;
    private double edgeDistance = 48;
    private double arrivalRadius = 8;
    private int columnHeight = 40;
    private int columnStep = 2;

    private final Set<UUID> optedOut = new HashSet<>();
    private final Map<UUID, BeamState> states = new HashMap<>();
    private BukkitTask task;

    public BeamGuide(JavaPlugin plugin, Logger log, boolean emAvailable) {
        this.plugin = plugin;
        this.log = log;
        this.emAvailable = emAvailable;
    }

    public void configure(int updateTicks, double edgeDistance,
                          double arrivalRadius, int columnHeight,
                          int columnStep) {
        this.updateTicks = updateTicks;
        this.edgeDistance = edgeDistance;
        this.arrivalRadius = arrivalRadius;
        this.columnHeight = columnHeight;
        this.columnStep = columnStep;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(
                plugin, this::refreshAll, 20L, updateTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        states.clear();
    }

    /** A player's own beam switch (/questbeacon off|on). */
    public void setOff(UUID uuid, boolean off) {
        if (off) optedOut.add(uuid);
        else optedOut.remove(uuid);
    }

    public boolean isOff(UUID uuid) {
        return optedOut.contains(uuid);
    }

    public BeamState state(UUID uuid) {
        return states.get(uuid);
    }

    /** Drops a player's beam state (quit). Particles need no
     *  cleanup — they are already only in that player's client. */
    public void clearPlayer(UUID uuid) {
        states.remove(uuid);
    }

    // ------------------------------------------------------------- refresh

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                refresh(player);
            } catch (Throwable t) {
                log.warning("ExcelEMFixes quest-beacon: refresh failed "
                        + "for " + player.getName() + ": " + t);
            }
        }
    }

    private void refresh(Player player) {
        UUID uuid = player.getUniqueId();
        EmTrackedQuest.Tracked tracked = emAvailable
                ? EmTrackedQuest.read(uuid, log) : null;
        QuestDestinations.Destination dest = tracked == null ? null
                : QuestDestinationApi.destinationFor(tracked.file());
        Location loc = player.getLocation();
        boolean sameWorld = dest != null
                && loc.getWorld() != null
                && loc.getWorld().getName().equals(dest.world());
        double dist = dest == null ? 0
                : BeamGeometry.horizontalDistance(
                        loc.getX(), loc.getZ(), dest.x(), dest.z());
        BeamPolicy.Mode mode = BeamPolicy.decide(
                optedOut.contains(uuid),
                tracked == null ? null : tracked.file(),
                dest != null, sameWorld, dist,
                arrivalRadius, edgeDistance);
        BeamState previous = states.get(uuid);
        BeamState current = new BeamState(
                tracked == null ? null : tracked.file(),
                tracked == null ? null : tracked.name(),
                dest == null ? null : dest.place(), mode, dist);
        states.put(uuid, current);
        if (!BeamPolicy.shows(mode)) return;

        BeamGeometry.Anchor anchor = BeamGeometry.anchor(
                loc.getX(), loc.getZ(), dest.x(), dest.z(), edgeDistance);
        double baseY = anchor.settled() ? dest.y() : loc.getY();
        drawColumn(player, anchor.x(), baseY, anchor.z());

        // One quiet line when a beam first appears for a quest, and
        // one when it settles onto the destination — never per tick.
        boolean newBeam = previous == null
                || !BeamPolicy.shows(previous.mode())
                || !java.util.Objects.equals(
                        previous.questFile(), current.questFile());
        if (newBeam) {
            player.sendMessage("§eA pillar of light marks the way to §f"
                    + dest.place() + "§e — follow it while your quest "
                    + "is tracked.");
        } else if (!previous.mode().equals(mode)
                && mode == BeamPolicy.Mode.SHOW_SETTLED) {
            player.sendMessage("§eYou are in sight of §f" + dest.place()
                    + "§e — the light now stands on it.");
        }
    }

    /**
     * The column: bright END_ROD motes with a gold DUST core,
     * drawn only in the owning player's client.
     */
    private void drawColumn(Player player, double x, double baseY,
                            double z) {
        Particle.DustOptions gold = new Particle.DustOptions(
                Color.fromRGB(255, 200, 60), 1.6f);
        int i = 0;
        for (double y = baseY; y < baseY + columnHeight; y += columnStep) {
            player.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            if (i % 2 == 0) {
                player.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0,
                        gold);
            }
            i++;
        }
    }
}
