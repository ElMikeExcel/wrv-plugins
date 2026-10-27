package com.winkreyes.exceltelemetry.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encounter-integrity flags. DATA ONLY: a flag says "look at this",
 * never "this was cheating". Whether any tactic counts as cheese is
 * the administrator's call (the dragon-cheese rulings stand: beds capped, not
 * banned) - telemetry's job is making the evidence exist.
 *
 * Two signals in v1:
 *  - explosion_share_high: explosions dealt >= threshold of the
 *    boss's total damage taken. Bed/anchor/TNT play is legitimate;
 *    a boss that died ~entirely to explosions is still worth seeing.
 *  - implausibly_fast_kill: boss died faster than the per-kind
 *    floor. Floors are deliberately conservative (a strong party
 *    legitimately kills a wither fast); crossing one means the kill
 *    deserves a human glance, nothing more.
 */
public final class IntegrityFlags {
    private IntegrityFlags() {}

    public record Thresholds(double explosionShare, Map<String, Integer> fastKillSeconds) {
        public static Thresholds defaults() {
            Map<String, Integer> fast = new LinkedHashMap<>();
            fast.put("ender_dragon", 45);
            fast.put("wither", 20);
            fast.put("elder_guardian", 15);
            fast.put("warden", 20);
            fast.put("raid", 120);
            return new Thresholds(0.60, fast);
        }
    }

    public static List<Map<String, Object>> forBossKill(String bossKind, double durationSeconds,
                                                        double explosionShare, double totalDamage,
                                                        Thresholds t) {
        List<Map<String, Object>> flags = new ArrayList<>();
        if (totalDamage > 0 && explosionShare >= t.explosionShare()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("type", "explosion_share_high");
            f.put("explosion_share", Json.round1(explosionShare * 1000) / 1000.0);
            f.put("threshold", t.explosionShare());
            flags.add(f);
        }
        Integer floor = t.fastKillSeconds().get(bossKind);
        if (floor != null && durationSeconds < floor) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("type", "implausibly_fast_kill");
            f.put("duration_s", Json.round1(durationSeconds));
            f.put("floor_s", floor);
            flags.add(f);
        }
        return flags;
    }

    /** Same explosion signal for elite (non-boss) kill lines. */
    public static List<Map<String, Object>> forEliteKill(double explosionShare, double totalDamage,
                                                         Thresholds t) {
        List<Map<String, Object>> flags = new ArrayList<>();
        if (totalDamage > 0 && explosionShare >= t.explosionShare()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("type", "explosion_share_high");
            f.put("explosion_share", Json.round1(explosionShare * 1000) / 1000.0);
            f.put("threshold", t.explosionShare());
            flags.add(f);
        }
        return flags;
    }
}
