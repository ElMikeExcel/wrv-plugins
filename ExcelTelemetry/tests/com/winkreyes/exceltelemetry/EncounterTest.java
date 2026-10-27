package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.Encounter;
import java.util.Map;

/**
 * Boss-encounter ledgers: the dragon phase split, and - the whole
 * point of the plugin - a FAILED dragon attempt must close as a
 * failed attempt with its data intact.
 */
public class EncounterTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }
    static void eq(double a, double b, String m) { check(Math.abs(a - b) < 0.001, m + " (" + a + " vs " + b + ")"); }

    public static void main(String[] args) {
        // --- a WINNING dragon fight with a phase split ---
        Encounter win = new Encounter("boss:d1", "ender_dragon", "d1", "world_the_end", 10_000);
        win.recordDamageToBoss("player:playerA", "playerA", "PlayerA", "ENTITY_ATTACK", 100, "phase1", 11_000);
        win.recordDamageToBoss("player:playerA", "playerA", "PlayerA", "ENTITY_ATTACK", 150, "phase1", 20_000);
        win.recordDamageToBoss("player:playerB", "playerB", "PlayerB", "PROJECTILE", 40, "phase1", 21_000);
        win.recordDamageToBoss("player:playerA", "playerA", "PlayerA", "ENTITY_ATTACK", 300, "phase2", 40_000);
        win.recordDamageTaken("playerA", "PlayerA", 55, 41_000);
        win.recordDamageTaken("playerB", "PlayerB", 12, 42_000);
        win.close(Encounter.Outcome.KILLED);
        check(win.success(), "killed dragon counts as success");
        eq(win.totalDamageToBoss(), 590, "boss damage total");
        eq(win.phaseDamage().get("phase1"), 290, "phase 1 damage split");
        eq(win.phaseDamage().get("phase2"), 300, "phase 2 damage split");
        Map<String, Object> s = win.summary(50_000);
        check("killed".equals(s.get("outcome")), "summary outcome killed");
        eq(((Number) s.get("duration_s")).doubleValue(), 40, "summary duration");
        @SuppressWarnings("unchecked") Map<String, Object> parts = (Map<String, Object>) s.get("participants");
        @SuppressWarnings("unchecked") Map<String, Object> k = (Map<String, Object>) parts.get("playerA");
        eq(((Number) k.get("damage_dealt")).doubleValue(), 550, "PlayerA carried the fight");
        eq(((Number) k.get("damage_taken")).doubleValue(), 55, "PlayerA's damage taken");
        @SuppressWarnings("unchecked") Map<String, Object> dmg = (Map<String, Object>) s.get("damage");
        @SuppressWarnings("unchecked") Map<String, Object> ph = (Map<String, Object>) dmg.get("phases");
        check(ph != null && ph.containsKey("phase1") && ph.containsKey("phase2"),
                "summary carries the phase split for the tuning report");
        check(s.get("markers") != null, "phase flip marker recorded");

        // --- a FAILED dragon attempt: party wipes in phase 2 ---
        Encounter fail = new Encounter("boss:d2", "ender_dragon", "d2", "world_the_end", 100_000);
        fail.recordDamageToBoss("player:playerC", "playerC", "PlayerC", "ENTITY_ATTACK", 250, "phase1", 110_000);
        fail.recordDamageToBoss("player:playerC", "playerC", "PlayerC", "ENTITY_ATTACK", 80, "phase2", 130_000);
        fail.recordDamageTaken("playerC", "PlayerC", 90, 131_000);
        check(!fail.allParticipantsDead(), "not wiped while the only fighter lives");
        fail.recordParticipantDeath("playerC", "PlayerC");
        check(fail.allParticipantsDead(), "wipe detected when every participant is dead");
        fail.close(Encounter.Outcome.PARTY_WIPED);
        check(!fail.success(), "a wiped attempt is NOT a success - failed attempts are data");
        Map<String, Object> fs = fail.summary(140_000);
        check("party_wiped".equals(fs.get("outcome")), "failed attempt closes as party_wiped");
        eq(fail.phaseDamage().get("phase1"), 250, "failed attempt keeps its phase 1 damage");
        eq(fail.phaseDamage().get("phase2"), 80, "failed attempt keeps its phase 2 damage (how deep they got)");

        // --- phantom participants: a stranger dying elsewhere changes nothing ---
        Encounter raid = new Encounter("raid:w:1", "raid", null, "world", 0);
        raid.recordDamageToBoss("player:a", "a", "A", "ENTITY_ATTACK", 10, null, 100);
        raid.recordParticipantDeath("stranger", "Stranger");
        check(!raid.hasParticipant("stranger"), "non-participant death does not register");
        check(!raid.allParticipantsDead(), "stranger death cannot wipe someone else's fight");
        raid.recordParticipantAlive("a");
        raid.close(Encounter.Outcome.RAID_WON);
        check(raid.success(), "raid won counts as success");

        // --- abandoned: closes once, stays closed ---
        Encounter ab = new Encounter("boss:w1", "wither", "w1", "world", 0);
        ab.recordDamageToBoss("player:a", "a", "A", "ENTITY_ATTACK", 30, null, 100);
        ab.close(Encounter.Outcome.ABANDONED);
        ab.close(Encounter.Outcome.KILLED); // late duplicate close must not rewrite history
        check(ab.outcome() == Encounter.Outcome.ABANDONED, "an encounter closes exactly once");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}
