/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ExcelProtect publish SHAPES (ExcelBossTrial 0.3.4): pure,
 * Bukkit-free shaping from BossTrial's own fight record
 * ({@link FightTracker.FightRecord}) into the exact maps the
 * ExcelProtect contract consumes. Nothing here knows Protect's
 * classes exist - the gateway in the {@code protect} package
 * carries these maps across the seam unchanged, so the shapes
 * are unit-testable on a bare classpath and the contract can be
 * re-pointed at a future Protect API by touching one class.
 *
 * THE VERDICT TABLE - fired as the contract's FightTableEvent:
 * encounter = the seal id ("wither", "elder_guardian", "warden",
 * "ender_dragon"); verdict = the CreditJudge verdict name;
 * credit = the killing player's name (the player GuildGate
 * credits or vetoes for the kill; "" when no player landed it);
 * pool + durationSeconds from the record; rows carry EXACTLY the
 * contract keys name / uuid / damage_dealt / damage_taken /
 * healing_done / healing_received, numbers at the fight record's
 * own one-decimal precision. BossTrial tracks no per-player
 * death count inside a fight, so no deaths key is invented -
 * Protect's combat stream attributes deaths from its own entity
 * ledger.
 *
 * ELDER PROGRESS - a trio progress point is not a verdict, so it
 * does NOT ride FightTableEvent (Protect writes an audit
 * seal_verdict from every FightTableEvent; a progress point must
 * never pose as one). It rides the general publish door instead:
 * stream "combat", type "fight_progress", the same rows, plus
 * progress_now / progress_of and the verdict label the fight
 * record carries ("ELDER_PROGRESS n of 3").
 *
 * BOSS STATE SCHEMA (stream "combat", type "boss_state",
 * schema 1) - the feed Protect answers boss status from. Every
 * record carries: schema (1), source ("bosstrial"), chapter
 * (seal id), boss (display name), state. Per state:
 *   fight_started      + boss_uuid, pool
 *   fight_ended        + boss_uuid, verdict (the verdict name,
 *                        or the ELDER_PROGRESS label), record
 *                        (the fights/ file name - the verdict
 *                        reference), duration_s, champions
 *                        (standing champions' names, may be empty)
 *   respawn_scheduled  + monument (elder only), respawn_due_ms,
 *                        elders (how many elders the schedule
 *                        covers; 1 for a single unearned kill)
 *   respawned          + monument (elder only), boss_uuid (the
 *                        fresh elder), world
 * Only elder guardians respawn on a plugin clock (the other
 * chapters are player-summoned), so respawn states exist only
 * for elder_guardian. Protect stamps its own ts/ts_ms on every
 * stored line; the event ms below ride along for consumers that
 * read the fields raw.
 */
public final class ProtectTableShape {

    /** boss_state / fight_progress schema version. */
    public static final int SCHEMA = 1;
    /** The stream every 0.3.4 publish lands in. */
    public static final String STREAM = "combat";
    /** The boss-state record type. */
    public static final String TYPE_BOSS_STATE = "boss_state";
    /** The elder-progress record type. */
    public static final String TYPE_FIGHT_PROGRESS = "fight_progress";

    private ProtectTableShape() {
    }

    /** One decimal, the fight record's own precision; NaN and
     *  infinities read 0 (a governed value is never either). */
    public static double round1(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        return Math.round(v * 10.0) / 10.0;
    }

    /** The contract rows for one fight record (possibly empty -
     *  a verdict on a boss no player ever touched still has a
     *  record, with zero rows). */
    public static List<Map<String, Object>> rows(FightTracker.FightRecord rec) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rec == null) {
            return out;
        }
        for (FightTracker.FightRecord.Row r : rec.rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.name);
            m.put("uuid", r.uuid.toString());
            m.put("damage_dealt", round1(r.damageDealt));
            m.put("damage_taken", round1(r.damageTaken));
            m.put("healing_done", round1(r.healingDone));
            m.put("healing_received", round1(r.healingReceived));
            out.add(m);
        }
        return out;
    }

    /** The credit string: who landed the kill the seal judges. */
    public static String creditOf(FightTracker.FightRecord rec) {
        return rec != null && rec.killerName != null ? rec.killerName : "";
    }

    private static Map<String, Object> base(BossKind kind, String state) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema", SCHEMA);
        m.put("source", "bosstrial");
        m.put("chapter", kind.sealId());
        m.put("boss", kind.displayName());
        m.put("state", state);
        return m;
    }

    /** fight_started: a fresh encounter ledger opened. */
    public static Map<String, Object> fightStarted(BossKind kind, String bossUuid,
                                                   double pool) {
        Map<String, Object> m = base(kind, "fight_started");
        m.put("boss_uuid", bossUuid);
        m.put("pool", round1(pool));
        return m;
    }

    /**
     * fight_ended: the encounter reached its death handler -
     * a verdict, or an elder progress point (verdictLabel is the
     * ELDER_PROGRESS label then). {@code recordRef} is the
     * fights/ file name the full table was written to.
     */
    public static Map<String, Object> fightEnded(FightTracker.FightRecord rec,
                                                 String verdictLabel,
                                                 String recordRef) {
        Map<String, Object> m = base(rec.kind, "fight_ended");
        m.put("boss_uuid", rec.bossUuid.toString());
        m.put("verdict", verdictLabel);
        if (recordRef != null) {
            m.put("record", recordRef);
        }
        m.put("duration_s", rec.durationSeconds());
        m.put("champions", new ArrayList<>(rec.championsStandingNames));
        return m;
    }

    /** respawn_scheduled (elder only): a kill that did not count
     *  put one elder on the respawn clock, or a counted trio
     *  turned over (elders = the trio size flipped). */
    public static Map<String, Object> respawnScheduled(String monumentId, long dueMs,
                                                       int elders) {
        Map<String, Object> m = base(BossKind.ELDER_GUARDIAN, "respawn_scheduled");
        if (monumentId != null) {
            m.put("monument", monumentId);
        }
        m.put("respawn_due_ms", dueMs);
        m.put("elders", elders);
        return m;
    }

    /** respawned (elder only): the maintenance loop put a fresh
     *  elder back at its home - the boss is available again. */
    public static Map<String, Object> respawned(String monumentId, String newBossUuid,
                                                String world) {
        Map<String, Object> m = base(BossKind.ELDER_GUARDIAN, "respawned");
        if (monumentId != null) {
            m.put("monument", monumentId);
        }
        m.put("boss_uuid", newBossUuid);
        if (world != null) {
            m.put("world", world);
        }
        return m;
    }

    /**
     * The fight_progress record for one elder progress point:
     * the progress point's own table, shaped like the combat
     * fight summaries Protect writes for verdicts, plus the
     * progress counters. {@code recordRef} as in fightEnded.
     */
    public static Map<String, Object> fightProgress(FightTracker.FightRecord rec,
                                                    String verdictLabel,
                                                    int progressNow, int progressOf,
                                                    String recordRef) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema", SCHEMA);
        m.put("source", "bosstrial");
        m.put("encounter", rec.kind.sealId());
        m.put("boss", rec.kind.displayName());
        m.put("verdict", verdictLabel);
        m.put("progress_now", progressNow);
        m.put("progress_of", progressOf);
        m.put("credit", creditOf(rec));
        m.put("pool", round1(rec.pool));
        m.put("duration_s", rec.durationSeconds());
        m.put("total_damage", round1(rec.totalDamage));
        m.put("player_damage", round1(rec.playerDamage));
        m.put("share_percent", round1(rec.sharePercent));
        if (recordRef != null) {
            m.put("record", recordRef);
        }
        m.put("players", rows(rec));
        return m;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
