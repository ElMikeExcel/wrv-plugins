package excelemfixes.questdest;

import excelemfixes.shared.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Time-limited glow guidance, built on the server's existing glow
 * approach — a tracked reveal (the "thing where we
 * made mobs glow"): the glow is a plain GLOWING potion effect plus
 * an in-memory record of exactly which entities THIS feature lit,
 * and it only ever clears its own. Nothing is persisted; a restart
 * or a disable takes every light back off.
 *
 * Two shapes, per destination row:
 * - ENTITY: the target entity itself (an NPC or boss near the
 *   destination point) gets the GLOWING effect until it expires
 *   or we clear it.
 * - MARKER (a place, e.g. an entry wormhole): a temporary armor
 *   stand is spawned at the spot — invisible, invulnerable,
 *   non-persistent — carrying the same GLOWING effect, and is
 *   removed outright at expiry. One live marker per player;
 *   asking again replaces it.
 *
 * Guidance only: this class never moves a player or an entity.
 */
public final class GlowGuide {

    public enum Kind { MARKER, ENTITY, WORLD_MISSING, DISABLED }

    /** What a guide request lit. name = what is glowing. */
    public record Outcome(Kind kind, String name) {}

    private record Marker(UUID entityId, long expiryMs,
                          org.bukkit.Chunk chunk) {}

    private final JavaPlugin plugin;
    private final Logger log;

    private boolean enabled = true;
    private int durationSeconds = 90;
    private int searchRadius = 32;

    /** Entities we lit directly, UUID -> expiry (tracked set). */
    private final Map<UUID, Long> ourGlow = new HashMap<>();
    /** Live place-markers, by owning player. */
    private final Map<UUID, Marker> markers = new HashMap<>();
    private BukkitTask sweepTask;

    public GlowGuide(JavaPlugin plugin, Logger log) {
        this.plugin = plugin;
        this.log = log;
    }

    public void configure(boolean enabled, int durationSeconds,
                          int searchRadius) {
        this.enabled = enabled;
        this.durationSeconds = durationSeconds;
        this.searchRadius = searchRadius;
    }

    public boolean enabled() {
        return enabled;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public void start() {
        stop();
        sweepTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::sweep, 20L, 20L);
    }

    /** Takes every light back off (disable / reload). */
    public void stop() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        for (Marker m : markers.values()) {
            removeMarker(m);
        }
        markers.clear();
        clearOurGlow();
    }

    /** Removes a player's marker (quit / replaced). */
    public void clearPlayer(UUID playerUuid) {
        Marker m = markers.remove(playerUuid);
        if (m != null) removeMarker(m);
    }

    /** Removes a marker entity and releases its chunk ticket. */
    private void removeMarker(Marker m) {
        Entity e = Bukkit.getEntity(m.entityId());
        if (e != null) e.remove();
        if (m.chunk() != null) {
            try {
                m.chunk().removePluginChunkTicket(plugin);
            } catch (Throwable ignored) {
                // Chunk already gone with its world; nothing to release.
            }
        }
    }

    // ------------------------------------------------------------- guiding

    public Outcome guide(Player player, QuestDestinations.Destination d) {
        if (!enabled) return new Outcome(Kind.DISABLED, null);
        World world = Bukkit.getWorld(d.world());
        if (world == null) return new Outcome(Kind.WORLD_MISSING, null);
        Location spot = new Location(world, d.x(), d.y(), d.z());
        if (d.glowIsEntity()) {
            LivingEntity target = findEntity(world, spot, d.glowEntityName());
            if (target != null) {
                lightEntity(target);
                return new Outcome(Kind.ENTITY,
                        TextUtil.cleanNameplate(
                                target.getCustomName() == null
                                        ? d.glowEntityName()
                                        : target.getCustomName()));
            }
            // Not nearby right now: light the spot instead of
            // leaving the player with nothing.
        }
        spawnMarker(player, spot, d.entry());
        return new Outcome(Kind.MARKER, d.entry());
    }

    /**
     * General surface for any caller (the /questwhere glow
     * command, and other plugins through GlowApi): light a marker
     * at an arbitrary point. Returns false when glow is off or
     * the world is not loaded.
     */
    public boolean glowMarkerAt(Player player, String worldName,
                                double x, double y, double z,
                                String label) {
        if (!enabled) return false;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        spawnMarker(player, new Location(world, x, y, z),
                label == null || label.isBlank() ? "Destination" : label);
        return true;
    }

    /**
     * General surface: light the named entity near an arbitrary
     * point; when it is not there, light a marker at the point
     * instead. Returns what was lit (or why not).
     */
    public Outcome glowEntityAt(Player player, String worldName,
                                double x, double y, double z,
                                String entityName) {
        if (!enabled) return new Outcome(Kind.DISABLED, null);
        World world = Bukkit.getWorld(worldName);
        if (world == null) return new Outcome(Kind.WORLD_MISSING, null);
        Location spot = new Location(world, x, y, z);
        LivingEntity target = findEntity(world, spot, entityName);
        if (target != null) {
            lightEntity(target);
            return new Outcome(Kind.ENTITY,
                    TextUtil.cleanNameplate(
                            target.getCustomName() == null
                                    ? entityName
                                    : target.getCustomName()));
        }
        spawnMarker(player, spot, entityName);
        return new Outcome(Kind.MARKER, entityName);
    }

    private LivingEntity findEntity(World world, Location spot,
                                    String entityName) {
        String want = TextUtil.cleanNameplate(entityName);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : world.getNearbyEntities(spot, searchRadius,
                searchRadius, searchRadius)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e instanceof Player) continue;
            String name = e.getCustomName();
            if (name == null) continue;
            if (!TextUtil.cleanNameplate(name).equalsIgnoreCase(want)) {
                continue;
            }
            double dist = e.getLocation().distanceSquared(spot);
            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }

    /** The reveal's exact move: effect on, UUID recorded. */
    private void lightEntity(LivingEntity entity) {
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING, durationSeconds * 20, 0,
                false, false, false));
        ourGlow.put(entity.getUniqueId(),
                System.currentTimeMillis() + durationSeconds * 1000L);
    }

    private void spawnMarker(Player player, Location spot, String label) {
        clearPlayer(player.getUniqueId());
        // Hold the destination chunk for the marker's lifetime:
        // without a ticket an unloaded chunk takes the marker
        // with it (and the glow with it) long before expiry.
        org.bukkit.Chunk chunk = spot.getChunk();
        chunk.addPluginChunkTicket(plugin);
        ArmorStand stand;
        try {
            stand = spot.getWorld().spawn(spot, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setInvulnerable(true);
                s.setMarker(true);
                s.setBasePlate(false);
                s.setArms(false);
                s.setPersistent(false);
                s.customName(TextUtil.component("&e" + label));
                s.setCustomNameVisible(true);
            });
        } catch (Throwable t) {
            try {
                chunk.removePluginChunkTicket(plugin);
            } catch (Throwable ignored) {
            }
            throw t;
        }
        stand.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING, durationSeconds * 20, 0,
                false, false, false));
        markers.put(player.getUniqueId(), new Marker(stand.getUniqueId(),
                System.currentTimeMillis() + durationSeconds * 1000L,
                chunk));
    }

    // -------------------------------------------------------------- expiry

    private void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Marker>> mit = markers.entrySet().iterator();
        while (mit.hasNext()) {
            Map.Entry<UUID, Marker> e = mit.next();
            Entity entity = Bukkit.getEntity(e.getValue().entityId());
            if (entity == null || entity.isDead()
                    || now >= e.getValue().expiryMs()) {
                removeMarker(e.getValue());
                mit.remove();
            }
        }
        Iterator<Map.Entry<UUID, Long>> git = ourGlow.entrySet().iterator();
        while (git.hasNext()) {
            Map.Entry<UUID, Long> e = git.next();
            Entity entity = Bukkit.getEntity(e.getKey());
            if (entity == null || entity.isDead()
                    || now >= e.getValue()) {
                if (entity instanceof LivingEntity living
                        && living.hasPotionEffect(PotionEffectType.GLOWING)) {
                    living.removePotionEffect(PotionEffectType.GLOWING);
                }
                git.remove();
            }
        }
    }

    /** The clear discipline: only ours, only the effect. */
    private void clearOurGlow() {
        Set<UUID> mine = new HashSet<>(ourGlow.keySet());
        ourGlow.clear();
        for (UUID id : mine) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity living && !living.isDead()
                    && living.hasPotionEffect(PotionEffectType.GLOWING)) {
                living.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }
}
