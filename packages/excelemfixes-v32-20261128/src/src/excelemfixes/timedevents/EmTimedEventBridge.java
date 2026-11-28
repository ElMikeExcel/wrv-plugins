package excelemfixes.timedevents;

import com.magmaguy.elitemobs.api.CustomEventStartEvent;
import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.api.EliteMobRemoveEvent;
import com.magmaguy.elitemobs.api.EliteMobSpawnEvent;
import com.magmaguy.elitemobs.config.customevents.CustomEventsConfigFields;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.events.CustomEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * All EliteMobs-touching code for the timed-event-guide
 * module. This class is only loaded when EliteMobs is present
 * (the module checks first), so its EM imports never resolve
 * against a server without EliteMobs.
 *
 * HOW ELITEMOBS TIMED EVENTS ACTUALLY WORK (read from the
 * pinned EliteMobs jar's bytecode, Nov 26, 2026 — see the
 * module class javadoc for the full finding): when the timed
 * event timer picks an event, EliteMobs fires
 * CustomEventStartEvent BEFORE any spawn location exists,
 * then its CustomSpawn searches — retrying every tick — for a
 * surface spot 24-128 blocks per axis from a RANDOM eligible
 * online player (loaded chunk required), spawns the boss
 * there, and only then starts the event. The event config's
 * own announcement (startMessage) is the pack author's to
 * set; the goblin pack sets none, and the boss's own spawn
 * message carries no location. The event ends when its
 * duration (minutes) lapses or — endEventWithBossDeath,
 * default true — when every primary boss stops existing,
 * which is what a despawn looks like from the watchdog's
 * chair.
 *
 * THIS BRIDGE, per event:
 *  - start signal (CustomEventStartEvent, TIMED only):
 *    open a TrackedEvent;
 *  - boss spawn (EliteMobSpawnEvent): bind the boss to its
 *    event by config filename, disarm the vanilla far-away
 *    despawn, relocate it into the configured band around
 *    the nearest player when EliteMobs' scatter left it
 *    further out (never for a stationary boss — AI off or
 *    zero movement speed at spawn means a placed setup, and
 *    placed setups are only ever indicated, never moved),
 *    and send the one broadcast with each player's own
 *    direction + distance;
 *  - refresh poll: sweep bosses that died / were removed /
 *    went invalid, close finished events, and draw the
 *    beacon pillar on each live event boss for the players
 *    in its world.
 */
public final class EmTimedEventBridge implements Listener {

    private final TimedEventGuideModule module;
    private final EventBeacon beacon = new EventBeacon();
    private final List<TrackedEvent> tracked = new ArrayList<>();

    public EmTimedEventBridge(TimedEventGuideModule module) {
        this.module = module;
    }

    /** Tracked events right now (for /eventguide status). */
    public int trackedCount() {
        int n = 0;
        for (TrackedEvent event : tracked) if (event.hasBound()) n++;
        return n;
    }

    public int pendingCount() {
        int n = 0;
        for (TrackedEvent event : tracked) if (!event.hasBound()) n++;
        return n;
    }

    // ------------------------------------------------------------ start

    /**
     * EliteMobs is about to run a custom event. Only TIMED
     * events are this module's scope (action events — break
     * block, fish, till soil — happen at the player's own
     * hands and need no guide).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomEventStart(CustomEventStartEvent event) {
        try {
            CustomEvent customEvent = event.getCustomEvent();
            if (customEvent == null) return;
            CustomEventsConfigFields fields = customEvent.getCustomEventsConfigFields();
            if (fields == null) return;
            if (fields.getEventType() != CustomEvent.EventType.TIMED) return;
            tracked.add(new TrackedEvent(fields.getFilename(),
                    fields.getBossFilenames(), System.currentTimeMillis()));
            module.debug("TIMED EVENT tracked: " + fields.getFilename()
                    + " (bosses " + fields.getBossFilenames() + ")");
        } catch (Throwable t) {
            module.logger().warning("Timed Event Guide: start hook failed: " + t);
        }
    }

    // ------------------------------------------------------------ spawn

    /**
     * A boss spawned. If it belongs to a tracked timed event,
     * bind it, run the spawn leg, and — for the event's first
     * boss — send the broadcast.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteSpawn(EliteMobSpawnEvent event) {
        try {
            EliteEntity elite = event.getEliteMobEntity();
            if (!(elite instanceof CustomBossEntity)) return;
            CustomBossesConfigFields fields =
                    ((CustomBossEntity) elite).getCustomBossesConfigFields();
            if (fields == null) return;
            TrackedEvent match = null;
            for (TrackedEvent candidate : tracked) {
                if (candidate.matchesBoss(fields.getFilename())
                        && !candidate.isBound(event.getEntity().getUniqueId())) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) return;
            LivingEntity living = event.getEntity();
            match.bind(living.getUniqueId());
            TimedEventSettings settings = module.settingsSnapshot();

            if (settings.preventFarDespawn()) {
                try {
                    living.setRemoveWhenFarAway(false);
                } catch (Throwable t) {
                    module.debug("TIMED EVENT: setRemoveWhenFarAway refused for "
                            + fields.getFilename() + ": " + t);
                }
            }

            boolean stationary = isStationary(living);
            if (settings.relocate() && !stationary) {
                relocate(elite, living, fields.getFilename());
            } else if (stationary) {
                module.debug("TIMED EVENT: " + fields.getFilename()
                        + " is stationary at spawn (AI off or zero movement)"
                        + " — indicator only, never moved");
            }

            if (!match.broadcastSent()) {
                match.markBroadcastSent();
                if (settings.broadcast()) {
                    broadcast(match, living, fields);
                }
            }
        } catch (Throwable t) {
            module.logger().warning("Timed Event Guide: spawn hook failed: " + t);
        }
    }

    /**
     * A placed setup declares itself: a boss whose AI is off
     * at spawn (a sleeping or scripted boss, a display piece)
     * or whose movement speed is zero is stationary by design.
     * Mobile event bosses — every goblin in the pack that
     * prompted this module — walk at full speed with AI on.
     */
    private boolean isStationary(LivingEntity living) {
        if (living instanceof Mob && !((Mob) living).isAware()) return true;
        AttributeInstance speed = living.getAttribute(Attribute.MOVEMENT_SPEED);
        return speed != null && speed.getValue() <= 0.0;
    }

    // ------------------------------------------------------------ relocation

    /**
     * The spawn leg: if EliteMobs' scatter left the boss
     * further than the configured maximum from the nearest
     * player in its world, move it to a safe surface spot
     * inside the configured band around that player. Any
     * failure — no player, no safe spot in the attempts
     * allowed — leaves the boss exactly where EliteMobs put
     * it; this leg never cancels or delays a spawn.
     */
    private void relocate(EliteEntity elite, LivingEntity living, String bossFilename) {
        TimedEventSettings settings = module.settingsSnapshot();
        World world = living.getWorld();
        Location bossLoc = living.getLocation();
        Player anchor = null;
        double nearest = -1;
        for (Player player : world.getPlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;
            Location pl = player.getLocation();
            double d = Math.hypot(pl.getX() - bossLoc.getX(), pl.getZ() - bossLoc.getZ());
            if (nearest < 0 || d < nearest) {
                nearest = d;
                anchor = player;
            }
        }
        if (RelocationPolicy.decide(nearest, settings.relocationMaxDistance())
                != RelocationPolicy.Decision.MOVE || anchor == null) {
            module.debug("TIMED EVENT: " + bossFilename + " stays — nearest player "
                    + (nearest < 0 ? "none" : String.format("%.0f", nearest))
                    + " blocks (max " + settings.relocationMaxDistance() + ")");
            return;
        }
        Location spot = findSafeSpot(world, anchor.getLocation(), settings);
        if (spot == null) {
            module.debug("TIMED EVENT: " + bossFilename + " stays — no safe spot in "
                    + settings.safeSpotAttempts() + " attempts around "
                    + anchor.getName());
            return;
        }
        living.teleport(spot);
        try {
            // Keep EliteMobs' own record of where this boss
            // spawned truthful — its leash/escape bookkeeping
            // reads the spawn location, and the boss's home
            // is where it now stands.
            elite.setSpawnLocation(spot.clone());
        } catch (Throwable t) {
            module.debug("TIMED EVENT: setSpawnLocation refused: " + t);
        }
        module.debug("TIMED EVENT: " + bossFilename + " relocated from "
                + String.format("%.0f", nearest) + " blocks out to "
                + String.format("%.0f", Math.hypot(
                        spot.getX() - anchor.getLocation().getX(),
                        spot.getZ() - anchor.getLocation().getZ()))
                + " blocks from " + anchor.getName());
    }

    /**
     * A safe surface spot in the band around the anchor:
     * solid, dry, non-burning ground with two passable blocks
     * above it, inside the world border. The band hugs the
     * anchor, whose chunks are loaded by definition — the
     * highest-block reads below never reach into unloaded
     * terrain in practice.
     */
    private Location findSafeSpot(World world, Location anchor,
                                  TimedEventSettings settings) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < settings.safeSpotAttempts(); attempt++) {
            double distance = RelocationPolicy.targetDistance(random.nextDouble(),
                    settings.relocationMinDistance(), settings.relocationMaxDistance());
            double angle = random.nextDouble() * Math.PI * 2.0;
            int x = (int) Math.floor(anchor.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(anchor.getZ() + Math.sin(angle) * distance);
            Block ground = world.getHighestBlockAt(x, z);
            // The highest block is often a plant, tall grass
            // or a tree canopy — walk down through non-solid
            // blocks and leaves to the real ground (rig
            // evidence, Nov 26, 2026: on plains terrain all
            // 12 first-pass candidates died on foliage).
            int guard = 0;
            while ((!ground.getType().isSolid()
                    || ground.getType().name().endsWith("_LEAVES"))
                    && guard++ < 8) {
                ground = ground.getRelative(0, -1, 0);
            }
            if (!isSafeGround(ground)) continue;
            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);
            if (!feet.isPassable() || feet.isLiquid()
                    || !head.isPassable() || head.isLiquid()) continue;
            Location spot = new Location(world, x + 0.5,
                    ground.getY() + 1.0, z + 0.5);
            if (!world.getWorldBorder().isInside(spot)) continue;
            return spot;
        }
        return null;
    }

    private boolean isSafeGround(Block ground) {
        Material type = ground.getType();
        if (!type.isSolid()) return false;
        return type != Material.LAVA && type != Material.WATER
                && type != Material.MAGMA_BLOCK && type != Material.CACTUS
                && type != Material.FIRE && type != Material.SOUL_FIRE
                && type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE;
    }

    // ------------------------------------------------------------ broadcast

    /**
     * The one announcement, at the event's first boss spawn:
     * the event's plain name for everyone in scope, and —
     * for players in the boss's world — their own direction
     * and distance to it. EliteMobs' boss spawn message (the
     * pack's "A Bone Seeker Goblin has appeared!") may also
     * fire; it carries no location, which is the gap this
     * line exists to close.
     */
    private void broadcast(TrackedEvent event, LivingEntity boss,
                           CustomBossesConfigFields fields) {
        TimedEventSettings settings = module.settingsSnapshot();
        String liveName = boss.getCustomName();
        // The configured name is the pack author's canonical
        // name and cleans deterministically; the live entity
        // name is the fallback (rig evidence, Nov 26, 2026:
        // the live name came back badge-prefixed and
        // truncated — "「☠」 Bone Seeker Gobl").
        String name = EventNames.displayName(fields.getName(), liveName,
                fields.getFilename(), event.eventFilename());
        Location bossLoc = boss.getLocation();
        List<Player> recipients = settings.broadcastServerWide()
                ? new ArrayList<>(Bukkit.getOnlinePlayers())
                : boss.getWorld().getPlayers();
        for (Player player : recipients) {
            StringBuilder line = new StringBuilder("§6[Event] §e")
                    .append(name).append(" §fhas appeared!");
            if (player.getWorld() == boss.getWorld()) {
                Location pl = player.getLocation();
                line.append(" §7It is ").append(CompassText.guidance(
                        bossLoc.getX() - pl.getX(),
                        bossLoc.getZ() - pl.getZ()))
                        .append(" — follow the pillar of light.");
            } else {
                line.append(" §7It appeared in ").append(boss.getWorld().getName()).append(".");
            }
            player.sendMessage(line.toString());
        }
        module.debug("TIMED EVENT: broadcast for " + event.eventFilename()
                + " (\"" + name + "\") to " + recipients.size() + " players");
    }

    // ------------------------------------------------------------ end

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteDeath(EliteMobDeathEvent event) {
        markGone(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteRemove(EliteMobRemoveEvent event) {
        markGone(event.getEntity());
    }

    private void markGone(Entity entity) {
        if (entity == null) return;
        for (TrackedEvent event : tracked) event.markGone(entity.getUniqueId());
    }

    // ------------------------------------------------------------ poll

    /**
     * The refresh tick (module-owned timer): close events
     * whose bosses are all gone, drop pendings that never
     * spawned and anything past the failsafe age, then draw
     * the beacon pillar on each live event's first living
     * boss for the players in its world.
     */
    public void poll() {
        TimedEventSettings settings = module.settingsSnapshot();
        beacon.configure(settings.columnHeight(), settings.columnStep());
        long now = System.currentTimeMillis();
        Iterator<TrackedEvent> it = tracked.iterator();
        while (it.hasNext()) {
            TrackedEvent event = it.next();
            if (event.hasBound()) {
                for (UUID id : event.boundBosses()) {
                    if (event.isGone(id)) continue;
                    Entity entity = Bukkit.getEntity(id);
                    if (entity == null || entity.isDead() || !entity.isValid()) {
                        event.markGone(id);
                    }
                }
                if (event.allBoundGone()) {
                    module.debug("TIMED EVENT closed: " + event.eventFilename()
                            + " (all bosses gone)");
                    it.remove();
                    continue;
                }
            }
            if (event.pendingExpired(now, settings.pendingTimeoutSeconds() * 1000L)) {
                module.debug("TIMED EVENT dropped: " + event.eventFilename()
                        + " (no boss ever spawned)");
                it.remove();
                continue;
            }
            if (event.expired(now, settings.maxTrackMinutes() * 60_000L)) {
                module.debug("TIMED EVENT closed: " + event.eventFilename()
                        + " (failsafe age)");
                it.remove();
                continue;
            }
            if (settings.beacon() && event.hasBound()) drawBeacon(event, settings);
        }
    }

    private void drawBeacon(TrackedEvent event, TimedEventSettings settings) {
        LivingEntity boss = null;
        for (UUID id : event.boundBosses()) {
            if (event.isGone(id)) continue;
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity && entity.isValid() && !entity.isDead()) {
                boss = (LivingEntity) entity;
                break;
            }
        }
        if (boss == null) return;
        Location anchor = boss.getLocation();
        double view = settings.beaconViewDistance();
        for (Player player : boss.getWorld().getPlayers()) {
            Location pl = player.getLocation();
            double d = Math.hypot(pl.getX() - anchor.getX(), pl.getZ() - anchor.getZ());
            if (d <= view) beacon.draw(player, anchor);
        }
    }
}
