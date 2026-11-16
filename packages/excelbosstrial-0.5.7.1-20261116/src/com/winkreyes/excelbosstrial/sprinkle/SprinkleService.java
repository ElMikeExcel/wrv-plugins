/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.sprinkle;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.MilestoneBeats;
import com.winkreyes.excelbosstrial.core.SprinkleBook;
import com.winkreyes.excelbosstrial.core.SprinkleCadence;
import com.winkreyes.excelbosstrial.core.SprinkleMelee;
import com.winkreyes.excelbosstrial.core.SprinkleSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * The elite sprinkle's Bukkit adapter (ExcelBossTrial 0.3.3,
 * study sections 1.3, 3, 6): owns the per-encounter add
 * registry, the milestone-stamp rolls, the spawns (through
 * EliteSprinkleGateway - the only EliteMobs seam), the 2-second
 * leash, and cleanup at every encounter close.
 *
 * Fight integrity (study section 6): adds are tracked ONLY
 * here, keyed by the boss's UUID in SprinkleBook. They never
 * enter the encounter ledger, the share math, the participant
 * sets, or the fight records - damage to an add is an event
 * whose victim is no tracked boss, and damage from an add
 * resolves to no governed boss kind in the host's handlers.
 * Adds keep EM's ordinary scaled-combat path with
 * damageMultiplier 1.0 (never set here), so TrainingWheels'
 * ladder compensation prices them exactly like natural
 * elites.
 *
 * Fail-soft throughout: master switch off, a chapter dark, or
 * the EliteMobs seam unavailable all reduce every entry point
 * to a no-op, and the seal fight runs exactly as it did in
 * 0.3.2.
 */
public final class SprinkleService {

    private final JavaPlugin host;
    private final EliteSprinkleGateway gateway;
    private final Function<UUID, Set<UUID>> participantsByBoss;
    private final SprinkleBook book = new SprinkleBook();
    private final Map<UUID, BossKind> kinds = new HashMap<>();
    private final Map<UUID, Location> fixedAnchors = new HashMap<>();
    /** Bosses whose deferred wave already logged its block line
     *  (0.4.0: a defer logs once per block, not once per tick). */
    private final Set<UUID> deferLogged = new HashSet<>();
    /** 0.5.4: adds a staggered wave has committed but not yet
     *  spawned, per boss. They count against the alive cap in
     *  the cadence judge, so a second wave can never fire into
     *  room the staggered ring is already promised to. */
    private final Map<UUID, Integer> pendingSpawns = new HashMap<>();
    private final Random rng = new Random();
    private NamespacedKey addKey;

    /** 0.5.2: the host's wave listener - told, fenced, when a
     *  wave actually spawns (never on a defer, a spent cap, or
     *  a failed conversion). The dragon's addwave voice line
     *  rides it; the listener can never reach the wave. */
    private java.util.function.BiConsumer<BossKind, LivingEntity> waveListener;

    /** Host wiring (0.5.2): where actually-spawned waves go. */
    public void setWaveListener(
            java.util.function.BiConsumer<BossKind, LivingEntity> listener) {
        this.waveListener = listener;
    }

    private boolean masterEnabled = true;
    private List<Double> stampMultipliers = SprinkleSettings.DEFAULT_STAMP_MULTIPLIERS;
    private double chanceCeiling = SprinkleSettings.DEFAULT_CHANCE_CEILING;
    private String announceText = SprinkleSettings.DEFAULT_ANNOUNCE_TEXT;
    private final Map<BossKind, SprinkleSettings> settings = new EnumMap<>(BossKind.class);

    /**
     * @param participantsByBoss the encounter's participant set
     *        (the host's ledger), used only to retarget a leashed
     *        add onto someone actually fighting its boss
     */
    public SprinkleService(JavaPlugin host, Function<UUID, Set<UUID>> participantsByBoss) {
        this.host = host;
        this.gateway = new EliteSprinkleGateway(host.getLogger());
        this.participantsByBoss = participantsByBoss;
        for (BossKind kind : BossKind.values()) {
            settings.put(kind, SprinkleSettings.defaults(kind));
        }
    }

    /** (Re)read the sprinkle settings - called from the host's
     *  loadSettings, so /bosstrial reload re-arms live. */
    public void configure(boolean masterEnabled, List<Double> stampMultipliers,
                          double chanceCeiling, String announceText,
                          Map<BossKind, SprinkleSettings> chapterSettings) {
        this.masterEnabled = masterEnabled;
        if (stampMultipliers != null && !stampMultipliers.isEmpty()) {
            this.stampMultipliers = List.copyOf(stampMultipliers);
        }
        this.chanceCeiling = (Double.isNaN(chanceCeiling) || chanceCeiling < 0.0)
                ? SprinkleSettings.DEFAULT_CHANCE_CEILING
                : Math.min(1.0, chanceCeiling);
        if (announceText != null) {
            this.announceText = announceText;
        }
        if (chapterSettings != null) {
            settings.putAll(chapterSettings);
        }
    }

    /** True when this chapter could actually roll (master on +
     *  chapter armed). The host asks before spending milestone
     *  bookkeeping on a dark chapter. */
    public boolean armed(BossKind kind) {
        SprinkleSettings s = settings.get(kind);
        return masterEnabled && s != null && s.enabled();
    }

    /** The boot awake line: per-chapter state, in the open. */
    public String bootSummary() {
        StringBuilder sb = new StringBuilder("Elite sprinkle: master ")
                .append(masterEnabled ? "ON" : "OFF (sprinkle.enabled)");
        for (BossKind kind : BossKind.values()) {
            SprinkleSettings s = settings.get(kind);
            sb.append("; ").append(kind.sealId()).append(' ')
                    .append(s != null && s.enabled()
                            ? "ARMED (cadence " + s.cadenceSeconds() + "s, cap "
                                    + s.maxPerFight() + ")"
                            : "off");
            if (s != null && (SprinkleMelee.knockbackSet(s.knockbackResistance())
                    || s.staggerTicks() > 0)) {
                sb.append(" [melee: kb ").append(s.knockbackResistance())
                        .append(" stagger ").append(s.staggerTicks()).append("t]");
            }
            if (kind == BossKind.ENDER_DRAGON) {
                sb.append(" (phase 2 only)");
            }
        }
        sb.append(" - chapters ship dark until armed in <chapter>.sprinkle; ")
                .append("EliteMobs seam: ")
                .append(gateway.available() ? "resolved" : "unavailable (sprinkle inert)");
        return sb.toString();
    }

    /**
     * One milestone stamp for one encounter (0.4.0, PM-6): a
     * stamp OWES a wave - no roll, no silent miss (the Nov 7
     * ruling). The wave fires immediately when there is room;
     * when the alive cap is full it stays owed and fires the
     * moment room opens (the cadence tick re-checks every
     * second). Called by the host for the ledger chapters
     * (fixedAnchor null - the anchor follows the boss) and by
     * the dragon chapter's sink for phase 2 (fixedAnchor = the
     * fountain, the End island's arena centre).
     */
    public void onMilestone(BossKind kind, LivingEntity boss, int stampIndex,
                            Location fixedAnchor) {
        SprinkleSettings s = settings.get(kind);
        if (!masterEnabled || s == null || !s.enabled() || boss == null) {
            return;
        }
        UUID bossUuid = boss.getUniqueId();
        kinds.put(bossUuid, kind);
        if (fixedAnchor != null) {
            fixedAnchors.put(bossUuid, fixedAnchor.clone());
        }
        book.touch(bossUuid);
        book.markWaveOwed(bossUuid);
        host.getLogger().info("BossTrial sprinkle [" + kind.displayName() + "]: "
                + MilestoneBeats.thresholdPercent(stampIndex)
                + "% stamp - a wave is owed (guaranteed since 0.4.0; no roll).");
        tryFireWave(kind, boss, "milestone");
    }

    /**
     * Register an encounter's FIXED anchor at battle
     * registration (Packet B, D-4): the dragon's fountain,
     * registered when the battle registers - not at the first
     * milestone stamp - so every wave this encounter ever fires
     * (stamp, cadence, leash) anchors on the island, never on
     * the flying dragon. Idempotent; a null anchor or boss is a
     * no-op. Works whether or not the chapter is armed (the
     * anchor is bookkeeping; arming decides firing).
     */
    public void registerFixedAnchor(BossKind kind, LivingEntity boss, Location anchor) {
        if (kind == null || boss == null || anchor == null) {
            return;
        }
        UUID bossUuid = boss.getUniqueId();
        kinds.put(bossUuid, kind);
        fixedAnchors.put(bossUuid, anchor.clone());
        book.touch(bossUuid);
    }

    /**
     * The trio relay's wave (Packet B, E-1): this encounter
     * OWES a wave because a sibling elder of its monument fell.
     * Same owed-wave machinery as a milestone stamp - the wave
     * fires immediately when there is room and DEFERs (never
     * spent) when the alive cap is full. Returns true when the
     * chapter is armed and the wave was marked owed.
     */
    public boolean relayWave(BossKind kind, LivingEntity boss) {
        SprinkleSettings s = settings.get(kind);
        if (!masterEnabled || s == null || !s.enabled() || boss == null) {
            return false;
        }
        UUID bossUuid = boss.getUniqueId();
        kinds.put(bossUuid, kind);
        book.touch(bossUuid);
        book.markWaveOwed(bossUuid);
        host.getLogger().info("BossTrial sprinkle [" + kind.displayName()
                + "]: the monument answers - a relay wave is owed "
                + "(guaranteed; no roll).");
        tryFireWave(kind, boss, "relay");
        return true;
    }

    /**
     * The timed cadence (0.4.0, PM-6), called by the host's
     * 1-second walk of the open encounters: when the cadence
     * clock says a wave is due it becomes owed, and any owed
     * wave (milestone or cadence) fires as soon as it can.
     */
    public void cadenceTick(BossKind kind, LivingEntity boss, long nowMs) {
        SprinkleSettings s = settings.get(kind);
        if (!masterEnabled || s == null || !s.enabled() || boss == null) {
            return;
        }
        UUID bossUuid = boss.getUniqueId();
        kinds.put(bossUuid, kind);
        book.touch(bossUuid);
        if (book.lastWaveMs(bossUuid) <= 0) {
            // The cadence clock starts at the encounter's first
            // tick - the first timed wave belongs a full cadence
            // into the fight, not at the opening bell.
            book.stampWave(bossUuid, nowMs);
        }
        if (SprinkleCadence.cadenceDue(book.lastWaveMs(bossUuid), nowMs,
                s.cadenceSeconds())) {
            book.markWaveOwed(bossUuid);
        }
        tryFireWave(kind, boss, "cadence");
    }

    /**
     * Fire the owed wave when the caps allow (0.4.0, PM-6). The
     * judge is SprinkleCadence: FIRE spawns the rotation's next
     * wave (partial when the remaining room is smaller than the
     * wave), DEFER keeps the wave owed (logged once per block),
     * SPENT retires the owed wave at the per-fight cap.
     */
    private void tryFireWave(BossKind kind, LivingEntity boss, String source) {
        SprinkleSettings s = settings.get(kind);
        UUID bossUuid = boss.getUniqueId();
        if (!gateway.available()) {
            return; // the gateway logged the seam once at first failure
        }
        pruneDead(bossUuid);
        int alive = book.aliveCount(bossUuid, aliveAdds(bossUuid))
                + pendingSpawns.getOrDefault(bossUuid, 0);
        SprinkleSettings.Wave wave = s.waveFor(book.waveIndex(bossUuid));
        SprinkleCadence.Outcome d = SprinkleCadence.judge(book.waveOwed(bossUuid),
                wave.count(), alive, s.maxAlive(),
                book.spawnedTotal(bossUuid), s.maxPerFight());
        switch (d.decision()) {
            case IDLE -> {
                return;
            }
            case SPENT -> {
                book.clearWaveOwed(bossUuid);
                host.getLogger().info("BossTrial sprinkle [" + kind.displayName()
                        + "]: per-fight cap reached (" + book.spawnedTotal(bossUuid)
                        + "/" + s.maxPerFight() + " spawned) - the sprinkle retires "
                        + "for this fight.");
                return;
            }
            case DEFER -> {
                if (deferLogged.add(bossUuid)) {
                    host.getLogger().info("BossTrial sprinkle [" + kind.displayName()
                            + "]: a wave is owed but the arena is full (alive " + alive
                            + "/" + s.maxAlive() + ") - deferred, not spent; it fires "
                            + "when room opens.");
                }
                return;
            }
            case FIRE -> {
                // fall through to the spawn below
            }
        }
        Location fixed = fixedAnchors.get(bossUuid);
        Location anchor = fixed != null ? fixed : boss.getLocation();
        // 0.5.4: a chapter with stagger-ticks spreads the ring
        // across the window instead of landing it in one tick.
        // The wave is committed at schedule time (owed cleared,
        // rotation advanced, announced at the planned count);
        // each scheduled spawn re-checks the encounter is still
        // open and simply never lands if it closed, and a failed
        // conversion inside the stagger skips that one add -
        // the same fail-soft the instant ring has always had.
        if (s.staggerTicks() > 0 && d.spawnCount() > 1) {
            scheduleStaggeredWave(kind, bossUuid, s, wave, d.spawnCount(), anchor);
            waveFired(kind, boss, source, d.spawnCount(), true);
            return;
        }
        int spawned = spawnWave(kind, bossUuid, s, wave, d.spawnCount(), anchor);
        if (spawned <= 0) {
            // Nothing converted: the wave stays owed and the
            // cadence clock restarts, so a broken seam retries
            // on the cadence rather than every tick.
            book.stampWave(bossUuid, System.currentTimeMillis());
            host.getLogger().warning("BossTrial sprinkle [" + kind.displayName()
                    + "]: the owed wave spawned nothing (every conversion "
                    + "failed) - it stays owed and retries on the cadence.");
            return;
        }
        waveFired(kind, boss, source, spawned, false);
    }

    /** The bookkeeping of a wave that fired (0.5.4: shared by
     *  the instant ring and the staggered ring): the owed flag
     *  clears, the cadence clock stamps, the rotation advances,
     *  the wave is logged and announced, and the host's wave
     *  listener is told. For a staggered wave {@code count} is
     *  the planned count - the announcement is the wave's
     *  arrival, and the ring completes over the window. */
    private void waveFired(BossKind kind, LivingEntity boss, String source,
                           int count, boolean staggered) {
        UUID bossUuid = boss.getUniqueId();
        SprinkleSettings s = settings.get(kind);
        book.clearWaveOwed(bossUuid);
        book.stampWave(bossUuid, System.currentTimeMillis());
        book.advanceWave(bossUuid);
        deferLogged.remove(bossUuid);
        host.getLogger().info("BossTrial sprinkle [" + kind.displayName()
                + "]: wave fired (" + source + ") - " + count + " elite(s), "
                + book.spawnedTotal(bossUuid) + "/" + s.maxPerFight()
                + (staggered
                        ? " spawned so far this fight (the ring lands over "
                                + s.staggerTicks() + " ticks)."
                        : " spawned this fight."));
        if (!announceText.isBlank()) {
            Bukkit.broadcastMessage(color(announceText
                    .replace("{chapter}", kind.displayName())
                    .replace("{count}", Integer.toString(count))));
        }
        if (waveListener != null) {
            try {
                waveListener.accept(kind, boss);
            } catch (Throwable listenerFailure) {
                host.getLogger().warning("BossTrial sprinkle [" + kind.displayName()
                        + "]: wave listener failed (the wave stands): "
                        + listenerFailure);
            }
        }
    }

    /** Schedule one staggered wave (0.5.4): the adds land one
     *  by one across the chapter's stagger window (SprinkleMelee
     *  delay table - first add immediate, last at the window's
     *  end). The level is captured now, before the rotation
     *  advances. A spawn whose encounter has closed by its tick
     *  never lands - closeEncounter has already removed exactly
     *  the adds that exist, and a late spawn would orphan. */
    private void scheduleStaggeredWave(BossKind kind, UUID bossUuid,
                                       SprinkleSettings s,
                                       SprinkleSettings.Wave wave, int n,
                                       Location anchor) {
        int level = s.levelFor(book.waveIndex(bossUuid));
        pendingSpawns.merge(bossUuid, n, Integer::sum);
        for (int i = 0; i < n; i++) {
            final int index = i;
            int delay = SprinkleMelee.delayTicks(index, n, s.staggerTicks());
            Runnable spawn = () -> {
                try {
                    if (!kinds.containsKey(bossUuid)
                            || !book.openBosses().contains(bossUuid)) {
                        return; // the encounter closed mid-stagger
                    }
                    spawnOne(kind, bossUuid, s, wave, index, level, anchor);
                } finally {
                    pendingSpawns.computeIfPresent(bossUuid,
                            (k, v) -> v > 1 ? v - 1 : null);
                }
            };
            if (delay <= 0) {
                spawn.run();
            } else {
                Bukkit.getScheduler().runTaskLater(host, spawn, delay);
            }
        }
    }

    /** Spawn + convert + register up to n adds of one wave. */
    private int spawnWave(BossKind kind, UUID bossUuid, SprinkleSettings s,
                          SprinkleSettings.Wave wave, int n, Location anchor) {
        int level = s.levelFor(book.waveIndex(bossUuid));
        int spawned = 0;
        for (int i = 0; i < n; i++) {
            if (spawnOne(kind, bossUuid, s, wave, i, level, anchor)) {
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * Spawn + convert + register ONE add of a wave (0.5.4:
     * extracted from spawnWave so the staggered scheduler
     * lands the same add, one tick-scheduled spawn at a time).
     * After a proven conversion, a chapter that set the melee
     * knockback key has it forced onto the add - the
     * conversion's own knockback stamp never survives it - and
     * the read-back rides the spawn log line.
     *
     * @return true when the add converted and registered
     */
    private boolean spawnOne(BossKind kind, UUID bossUuid, SprinkleSettings s,
                             SprinkleSettings.Wave wave, int i, int level,
                             Location anchor) {
        World world = anchor.getWorld();
        if (world == null) {
            return false;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(wave.typeFor(i));
        } catch (IllegalArgumentException bad) {
            host.getLogger().warning("BossTrial sprinkle [" + kind.displayName()
                    + "]: unknown mob type '" + wave.typeFor(i)
                    + "' in sprinkle.waves - skipped.");
            return false;
        }
        Location loc = wave.aerial() ? placementAerial(anchor)
                : placement(kind, anchor);
        Entity raw = world.spawnEntity(loc, type);
        if (!(raw instanceof LivingEntity add)) {
            raw.remove();
            return false;
        }
        EliteSprinkleGateway.Conversion conv = gateway.convertAndContain(add, level);
        if (!conv.ok()) {
            // A sprinkle that cannot convert does not get to
            // change the fight: the vanilla spawn goes away.
            add.remove();
            return false;
        }
        // 0.5.4 melee fix: force the chapter's knockback
        // resistance onto the converted add (SprinkleMelee).
        double kbReadback = Double.NaN;
        if (SprinkleMelee.knockbackSet(s.knockbackResistance())) {
            AttributeInstance kbAttr = add.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) {
                kbAttr.setBaseValue(
                        SprinkleMelee.appliedKnockback(s.knockbackResistance()));
                kbReadback = kbAttr.getValue();
            }
        }
        add.getPersistentDataContainer().set(addKey(), PersistentDataType.BYTE, (byte) 1);
        book.recordSpawn(bossUuid, add.getUniqueId());
        host.getLogger().info("BossTrial sprinkle [" + kind.displayName()
                + "]: spawned " + type + " elite level " + conv.level()
                + (conv.preConverted()
                        ? " (EliteMobs' natural conversion pre-empted the stamp level)"
                        : "")
                + ", damageMultiplier " + trim(conv.damageMultiplier())
                + (SprinkleMelee.knockbackSet(s.knockbackResistance())
                        ? ", knockbackResistance " + trim(kbReadback)
                                + " (chapter key " + s.knockbackResistance() + ")"
                        : "")
                + ", loot flags closed (elite/random/vanilla read back false).");
        return true;
    }

    /** Where an add lands: on a ring around the anchor. The
     *  dragon's adds snap to the island top like the chapter's
     *  own Sealbound waves; the elder's stay at the boss's depth
     *  (the fight is underwater inside the monument); the land
     *  chapters stay at arena level, stepping up out of a solid
     *  block rather than snapping to the surface far above. */
    private Location placement(BossKind kind, Location anchor) {
        double angle = rng.nextDouble() * Math.PI * 2.0;
        double radius = 2.5 + rng.nextDouble() * 2.0;
        Location loc = anchor.clone().add(Math.cos(angle) * radius, 0.0,
                Math.sin(angle) * radius);
        World world = anchor.getWorld();
        if (kind == BossKind.ENDER_DRAGON && world != null) {
            loc.setY(world.getHighestBlockYAt(loc) + 1);
        } else if (kind != BossKind.ELDER_GUARDIAN && world != null) {
            for (int up = 0; up < 3 && loc.getBlock().getType().isSolid(); up++) {
                loc.add(0.0, 1.0, 0.0);
            }
        }
        return loc;
    }

    /** Where an AERIAL add enters (0.4.0, PM-6): above the
     *  arena on a wide ring - the ghast's entry. Height 8-12
     *  over the anchor, radius 6-10: inside its fireball reach
     *  of the fight, never inside the boss's own space, and
     *  high enough that it pressures the party instead of
     *  body-blocking it. Outdoor arenas only by design: the
     *  wither chapter's arena is the open nether roof / the
     *  overworld field where the family fights it. */
    private Location placementAerial(Location anchor) {
        double angle = rng.nextDouble() * Math.PI * 2.0;
        double radius = 6.0 + rng.nextDouble() * 4.0;
        return anchor.clone().add(Math.cos(angle) * radius,
                8.0 + rng.nextDouble() * 4.0, Math.sin(angle) * radius);
    }

    /**
     * The 2-second leash (study section 6): an add farther than
     * its chapter's leash-radius from the encounter anchor is
     * teleported back to the spawn ring and retargeted onto a
     * random encounter participant. The anchor follows the boss,
     * except the dragon's, which stays at the fountain - the
     * dragon flies, and an anchor chasing it would drop
     * endermen into the void.
     */
    public void leashTick() {
        if (!masterEnabled || book.isEmpty()) {
            return;
        }
        for (UUID bossUuid : new ArrayList<>(book.openBosses())) {
            pruneDead(bossUuid);
            BossKind kind = kinds.get(bossUuid);
            SprinkleSettings s = kind == null ? null : settings.get(kind);
            if (s == null) {
                continue;
            }
            Location anchor = fixedAnchors.get(bossUuid);
            if (anchor == null) {
                Entity boss = Bukkit.getEntity(bossUuid);
                if (boss == null) {
                    continue; // closing soon via the host's prune
                }
                anchor = boss.getLocation();
            }
            for (UUID addUuid : book.adds(bossUuid)) {
                Entity e = Bukkit.getEntity(addUuid);
                if (!(e instanceof LivingEntity add) || add.isDead()) {
                    continue;
                }
                Location at = add.getLocation();
                if (at.getWorld() == null || !at.getWorld().equals(anchor.getWorld())
                        || at.distance(anchor) > s.leashRadius()) {
                    add.teleport(placement(kind, anchor));
                    if (add instanceof Mob mob) {
                        Player target = randomParticipant(bossUuid);
                        if (target != null) {
                            mob.setTarget(target);
                        }
                    }
                }
            }
        }
    }

    private Player randomParticipant(UUID bossUuid) {
        Set<UUID> parts = participantsByBoss.apply(bossUuid);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        List<Player> online = new ArrayList<>();
        for (UUID id : parts) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online.isEmpty() ? null : online.get(rng.nextInt(online.size()));
    }

    /**
     * Close one encounter: remove exactly its adds - through
     * EliteMobs' own removal where the handle answers (removed,
     * never killed: no death event, no drops, no XP), plain
     * Entity.remove() as the fallback.
     */
    public void closeEncounter(UUID bossUuid, String reason) {
        int total = book.spawnedTotal(bossUuid);
        Set<UUID> adds = book.close(bossUuid);
        kinds.remove(bossUuid);
        fixedAnchors.remove(bossUuid);
        deferLogged.remove(bossUuid);
        pendingSpawns.remove(bossUuid);
        int removed = 0;
        for (UUID addUuid : adds) {
            Entity e = Bukkit.getEntity(addUuid);
            if (e == null) {
                continue;
            }
            if (!gateway.removeElite(e)) {
                e.remove();
            }
            removed++;
        }
        if (total > 0) {
            host.getLogger().info("BossTrial sprinkle: encounter closed (" + reason
                    + ") - " + removed + " elite add(s) removed, " + total
                    + " spawned this fight.");
        }
    }

    /** Close every open encounter (plugin disable, admin reset). */
    public void closeAll(String reason) {
        for (UUID bossUuid : new ArrayList<>(book.openBosses())) {
            closeEncounter(bossUuid, reason);
        }
    }

    /** This plugin's sprinkle-add PDC key (the rig and any future
     *  tooling identify sprinkle adds by it). */
    public NamespacedKey addKey() {
        if (addKey == null) {
            addKey = new NamespacedKey(host, "sprinkle-add");
        }
        return addKey;
    }

    private Set<UUID> aliveAdds(UUID bossUuid) {
        Set<UUID> alive = new HashSet<>();
        for (UUID addUuid : book.adds(bossUuid)) {
            Entity e = Bukkit.getEntity(addUuid);
            if (e != null && !e.isDead() && e.isValid()) {
                alive.add(addUuid);
            }
        }
        return alive;
    }

    private void pruneDead(UUID bossUuid) {
        book.pruneDead(bossUuid, aliveAdds(bossUuid));
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String trim(double v) {
        if (Double.isNaN(v)) {
            return "n/a";
        }
        return Double.toString(Math.round(v * 1000.0) / 1000.0);
    }
}
