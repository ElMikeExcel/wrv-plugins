/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * 0.2.7 retention + re-open rules (Iris audit, Oct 24, 2026): an
 * unloaded boss keeps its ledger; a truly gone boss is pruned; a
 * re-opened ledger sets aside damage no ledger saw so an honest
 * finish can still meet the share; a fresh boss is never affected.
 */
public final class EncounterRetentionTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        long ttl = EncounterRetention.DEFAULT_TTL_MS;
        long now = 10L * ttl;
        var KEEP = EncounterRetention.Decision.KEEP;
        var PRUNE = EncounterRetention.Decision.PRUNE;

        // decide()
        check(EncounterRetention.decide(true, false, true, 0, now, now, ttl) == KEEP,
                "loaded and alive: keep");
        check(EncounterRetention.decide(true, true, true, 0, now, now, ttl) == PRUNE,
                "loaded and dead: prune");
        check(EncounterRetention.decide(false, false, false, 0, now - 60_000L, now, ttl) == KEEP,
                "unloaded a minute: keep (the 0.2.6 bug pruned here)");
        check(EncounterRetention.decide(false, false, false, 0, now - ttl + 1, now, ttl) == KEEP,
                "unloaded just under the TTL: keep");
        check(EncounterRetention.decide(false, false, false, 0, now - ttl - 1, now, ttl) == PRUNE,
                "unloaded past the TTL: prune");
        check(EncounterRetention.decide(false, false, true, 1, now, now, ttl) == KEEP,
                "area loaded, missing once: keep (entity load can trail the chunk)");
        check(EncounterRetention.decide(false, false, true, 2, now, now, ttl) == PRUNE,
                "area loaded, missing twice: gone - prune");
        check(EncounterRetention.decide(false, false, false, 5, now, now, ttl) == KEEP,
                "streak without a loaded area never prunes");
        check(EncounterRetention.decide(false, false, false, 0, now - 1, now, 0) == KEEP,
                "ttl 0 disables the age prune");

        // unseenOnReopen(): elder base 700
        eq(EncounterRetention.unseenOnReopen(700, 80, 700, 700), 0,
                "fresh elder (vanilla 80) is never a re-open");
        eq(EncounterRetention.unseenOnReopen(700, 80, 700, 80), 0,
                "fresh elder, wounded before the open, vanilla bar: not a re-open");
        eq(EncounterRetention.unseenOnReopen(700, 700, 700, 280), 420,
                "re-opened elder at 280/700: 420 set aside");
        eq(EncounterRetention.unseenOnReopen(700, 850, 700, 600), 100,
                "re-opened grown elder clipped to 700, at 600: 100 set aside");
        eq(EncounterRetention.unseenOnReopen(700, 700, 700, 700), 0,
                "re-opened unwounded: nothing set aside");
        eq(EncounterRetention.unseenOnReopen(700, 700, 0, 300), 0,
                "inactive rule (base 0): nothing set aside");
        eq(EncounterRetention.unseenOnReopen(1500, 300, 1500, 290), 0,
                "fresh wither at 290/300: not a re-open");

        // dragonUnseen(): live 400 / 900
        eq(EncounterRetention.dragonUnseen(false, 400, 900, 400, 400), 0, "dragon P1 full");
        eq(EncounterRetention.dragonUnseen(false, 400, 900, 400, 150), 250, "dragon P1 at 150");
        eq(EncounterRetention.dragonUnseen(false, 400, 900, 200, 180), 0,
                "dragon P1 before setup (vanilla 200 bar): nothing set aside");
        eq(EncounterRetention.dragonUnseen(true, 400, 900, 900, 634), 666,
                "dragon P2 at 634: 400 + 266 set aside");
        eq(EncounterRetention.dragonUnseen(true, 400, 900, 400, 300), 0,
                "dragon P2 machine but P1 bar: inconsistent - nothing set aside");

        // ledger denominator
        UUID boss = UUID.randomUUID();
        UUID ana = UUID.randomUUID();
        EncounterLedger l = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 700, 0L);
        l.markUnseenDamage(420);
        eq(l.shareDenominator(), 280, "denominator = pool - unseen");
        l.recordDealt(ana, 280, 1L);
        check(l.shareMet(0.7), "re-opened elder: honest finish of the remaining 280 meets 70%");
        eq(l.playerSharePercent(), 100.0, "share percent on the remaining fight");
        EncounterLedger g = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 700, 0L);
        g.markUnseenDamage(420);
        g.recordDealt(null, 200, 1L);
        g.recordDealt(ana, 80, 2L);
        check(!g.shareMet(0.7), "re-opened elder: mobs dealing most of the rest still deny");
        l.growTo(775);
        eq(l.shareDenominator(), 355, "growth still adds to the denominator after a re-open");
        EncounterLedger f = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 700, 0L);
        eq(f.shareDenominator(), 700, "no unseen: denominator is the pool (0.2.6 behavior)");
        f.markUnseenDamage(-5);
        f.markUnseenDamage(Double.NaN);
        eq(f.unseenDamage(), 0, "negative / NaN unseen ignored");
        EncounterLedger z = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 700, 0L);
        z.markUnseenDamage(700);
        check(z.shareDenominator() >= 1.0, "fully unseen pool clamps the denominator to 1");
        EncounterLedger zero = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 0, 0L);
        check(!zero.shareMet(0.7), "zero pool never meets the share (0.2.6 behavior)");

        System.out.println("EncounterRetentionTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

