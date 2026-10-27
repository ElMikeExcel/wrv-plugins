package com.winkreyes.exceltelemetry.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One boss encounter: a dragon / wither / elder guardian / warden
 * fight, or a whole raid. Fed by damage events; closed exactly once
 * with an outcome. FAILED attempts (party wipe, abandoned) close with
 * success=false - recording them is the entire point: before this
 * plugin, a failed boss attempt left no trace anywhere.
 *
 * Dragon damage is split by phase ("phase1" / "phase2" / "unknown")
 * as attributed by the Bukkit layer from the dragon's max-health
 * signature (250 phase 1 / 300 phase 2 under ExcelDragonTrial).
 */
public final class Encounter {
    public enum Outcome { OPEN, KILLED, PARTY_WIPED, ABANDONED, RAID_WON, RAID_STOPPED }

    public static final class Participant {
        public final String uuid;
        public String name;
        public double damageDealt;
        public double damageTaken;
        public boolean died;
        Participant(String uuid, String name) { this.uuid = uuid; this.name = name; }
    }

    public record Marker(String kind, long atMs, String detail) {}

    private final String key;
    private final String bossKind;      // ender_dragon | wither | elder_guardian | warden | raid
    private final String bossUuid;      // null for raids
    private final String world;
    private final long startedMs;
    private long lastActivityMs;
    private Outcome outcome = Outcome.OPEN;

    private final Map<String, Participant> participants = new LinkedHashMap<>();
    private final Set<String> deadParticipants = new LinkedHashSet<>();
    private final DamageLedger bossDamage = new DamageLedger(0); // damage dealt TO the boss
    private double damageTakenTotal;
    private final Map<String, Double> phaseDamage = new LinkedHashMap<>();
    private String currentPhase;
    private final List<Marker> markers = new ArrayList<>();

    public Encounter(String key, String bossKind, String bossUuid, String world, long startedMs) {
        this.key = key;
        this.bossKind = bossKind;
        this.bossUuid = bossUuid;
        this.world = world;
        this.startedMs = startedMs;
        this.lastActivityMs = startedMs;
    }

    public String key() { return key; }
    public String bossKind() { return bossKind; }
    public long startedMs() { return startedMs; }
    public synchronized long lastActivityMs() { return lastActivityMs; }
    public synchronized Outcome outcome() { return outcome; }
    public synchronized boolean isOpen() { return outcome == Outcome.OPEN; }

    private Participant participant(String uuid, String name) {
        return participants.computeIfAbsent(uuid, k -> new Participant(k, name));
    }

    /** A player hurt the boss. attackerKey is "player:<uuid>"; phase may be null. */
    public synchronized void recordDamageToBoss(String attackerKey, String playerUuid, String playerName,
                                                String cause, double amount, String phase, long nowMs) {
        bossDamage.record(attackerKey, cause, amount, nowMs);
        if (playerUuid != null) participant(playerUuid, playerName).damageDealt += amount;
        if (phase != null) {
            phaseDamage.merge(phase, amount, Double::sum);
            if (currentPhase == null) {
                currentPhase = phase;
            } else if (!currentPhase.equals(phase)) {
                markers.add(new Marker("phase_change", nowMs, currentPhase + "->" + phase));
                currentPhase = phase;
            }
        }
        lastActivityMs = nowMs;
    }

    /** The boss (or a raid mob) hurt a player. */
    public synchronized void recordDamageTaken(String playerUuid, String playerName, double amount, long nowMs) {
        participant(playerUuid, playerName).damageTaken += amount;
        damageTakenTotal += amount;
        lastActivityMs = nowMs;
    }

    public synchronized boolean hasParticipant(String playerUuid) {
        return participants.containsKey(playerUuid);
    }

    /** Marks a death ONLY for an existing participant - a player dying
     *  across the world must not become a phantom participant here. */
    public synchronized void recordParticipantDeath(String playerUuid, String playerName) {
        Participant p = participants.get(playerUuid);
        if (p == null) return;
        p.died = true;
        deadParticipants.add(playerUuid);
    }

    public synchronized void recordParticipantAlive(String playerUuid) {
        deadParticipants.remove(playerUuid);
    }

    /** True when at least one participant exists and none is currently alive. */
    public synchronized boolean allParticipantsDead() {
        if (participants.isEmpty()) return false;
        for (String uuid : participants.keySet()) {
            if (!deadParticipants.contains(uuid)) return false;
        }
        return true;
    }

    public synchronized void addMarker(String kind, long atMs, String detail) {
        markers.add(new Marker(kind, atMs, detail));
    }

    public synchronized Outcome close(Outcome newOutcome) {
        if (outcome == Outcome.OPEN) outcome = newOutcome;
        return outcome;
    }

    public synchronized boolean success() {
        return outcome == Outcome.KILLED || outcome == Outcome.RAID_WON;
    }

    public synchronized double durationSeconds(long endMs) {
        return Math.max(0, (endMs - startedMs) / 1000.0);
    }

    public synchronized double totalDamageToBoss() { return bossDamage.total(); }
    public synchronized double explosionShare() { return bossDamage.explosionShare(); }
    public synchronized Map<String, Double> phaseDamage() { return new LinkedHashMap<>(phaseDamage); }

    /** Full JSON-shaped summary; the Bukkit layer adds ts + seal stamps + flags. */
    public synchronized Map<String, Object> summary(long endMs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("boss_kind", bossKind);
        if (bossUuid != null) m.put("boss_uuid", bossUuid);
        m.put("world", world);
        m.put("started_ms", startedMs);
        m.put("ended_ms", endMs);
        m.put("duration_s", Json.round1(durationSeconds(endMs)));
        m.put("outcome", outcome.name().toLowerCase());
        m.put("success", success());
        Map<String, Object> parts = new LinkedHashMap<>();
        for (Participant p : participants.values()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("name", p.name);
            pm.put("damage_dealt", Json.round1(p.damageDealt));
            pm.put("damage_taken", Json.round1(p.damageTaken));
            pm.put("died", p.died);
            parts.put(p.uuid, pm);
        }
        m.put("participants", parts);
        Map<String, Object> dmg = bossDamage.snapshot();
        dmg.put("damage_taken_by_players", Json.round1(damageTakenTotal));
        if (!phaseDamage.isEmpty()) {
            Map<String, Object> ph = new LinkedHashMap<>();
            phaseDamage.forEach((k, v) -> ph.put(k, Json.round1(v)));
            dmg.put("phases", ph);
        }
        m.put("damage", dmg);
        if (!markers.isEmpty()) {
            List<Object> ms = new ArrayList<>();
            for (Marker mk : markers) {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("kind", mk.kind());
                mm.put("at_ms", mk.atMs());
                mm.put("after_s", Json.round1((mk.atMs() - startedMs) / 1000.0));
                if (mk.detail() != null) mm.put("detail", mk.detail());
                ms.add(mm);
            }
            m.put("markers", ms);
        }
        return m;
    }
}
