/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * THE feel-patch regression (0.3.6, DESIGN section 2): the
 * display changes NOTHING. One scripted hit sequence - plain,
 * crit-sized, a per-event-cap trim, a same-tick explosion-pool
 * starve, a banded exclusion, taken-side hits including a
 * TrainingWheels-shaped quarter and the 0.3.5 reflect value at
 * multiplier 0.0 - runs through BossGovernor + EncounterLedger
 * twice: once with a feedback observer computing every title
 * and action-bar payload after every record, once without.
 * The ledgers must come out IDENTICAL in every field the fight
 * is judged on. A third run with feedback settings fully OFF
 * (the observer short-circuits, as the adapter does) must match
 * too - the off path is a no-op, not a second math.
 */
public final class FeedbackNoDamageChangeTest {

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

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID RYLIE = UUID.fromString("00000000-0000-0000-0009-01f05d0d739d");
    private static final long T0 = 2_000_000L;

    /** The observer: exactly what the adapter computes after a
     *  record - the view, the title, both players' lines - and
     *  nothing else. Returns a digest so the computation is
     *  observable (it cannot be optimized into a no-op). */
    private static String observe(EncounterLedger ledger, FeedbackSettings settings) {
        if (settings.dark()) {
            return "";
        }
        FeedbackTotals view = FeedbackTotals.fromLedger(ledger);
        StringBuilder sb = new StringBuilder();
        sb.append(view.title(settings.titleFormat(), BossKind.ELDER_GUARDIAN)).append('|');
        sb.append(FeedbackTotals.actionBarLine(
                settings.damageTotalFormat(), 1.0, ledger, A)).append('|');
        sb.append(FeedbackTotals.actionBarLine(
                settings.damageTotalFormat(), 1.0, ledger, B)).append('|');
        sb.append(view.fraction());
        return sb.toString();
    }

    /** Length of the observer digest from the latest run -
     *  proves the observer actually computed (kept OUT of the
     *  compared snapshot: the digest is the observer's output,
     *  not ledger state). */
    private static int lastDigestLength;

    private static String run(boolean withObserver, FeedbackSettings settings) {
        EncounterLedger ledger = new EncounterLedger(
                BossKind.ELDER_GUARDIAN, UUID.randomUUID(), 10_900, T0);
        BossGovernor governor = new BossGovernor();
        BossCaps elderCaps = new BossCaps(45, 45);
        StringBuilder digest = new StringBuilder();
        long t = T0;

        // 1. A plain governed hit (wood-tier 10 after fractions).
        double d1 = governor.govern(10.0, false, 100L, elderCaps).damage;
        ledger.recordDealt(A, d1, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 2. A crit-sized hit.
        double d2 = governor.govern(16.5, false, 101L, elderCaps).damage;
        ledger.recordDealt(A, d2, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 3. A burst explosion: the per-event cap trims 100 -> 45,
        // the same-tick pool (45) lets it all through.
        double d3 = governor.govern(100.0, true, 102L, elderCaps).damage;
        ledger.recordDealt(A, d3, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 4. A second same-tick explosion: trimmed to 45, then the
        // drained pool starves it to 0 - the record is a no-op.
        double d4 = governor.govern(100.0, true, 102L, elderCaps).damage;
        ledger.recordDealt(A, d4, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 5. A capped melee burst from B.
        double d5 = governor.govern(60.0, false, 103L, elderCaps).damage;
        ledger.recordDealt(B, d5, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 6. A banded exclusion (the warden shape, judged here on
        // the elder ledger - the band facts are ledger mechanics).
        ledger.recordDealtBanded(B, 20.0, ++t, false, true);
        if (withObserver) digest.append(observe(ledger, settings));
        // 7. B lands an honest Power-V-sized hit.
        ledger.recordDealt(B, 23.8, ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 8. Taken side: a boss hit on A, a TrainingWheels-shaped
        // quarter hit on Rylie (8.0 x 0.25), and the 0.3.5 reflect
        // value - at multiplier 0.0 the spike records NOTHING.
        ledger.recordTaken(A, 5.0, ++t);
        ledger.recordTaken(RYLIE, 8.0 * 0.25, ++t);
        ledger.recordTaken(A, ReflectRule.scaledDamage(3.0, 0.0), ++t);
        if (withObserver) digest.append(observe(ledger, settings));
        // 9. Unattributed environment damage.
        ledger.recordDealt(null, 7.0, ++t);
        if (withObserver) digest.append(observe(ledger, settings));

        lastDigestLength = digest.length();
        return snapshot(ledger, governor);
    }

    /** Every field the fight is judged on, as one string. */
    private static String snapshot(EncounterLedger ledger, BossGovernor governor) {
        StringBuilder sb = new StringBuilder();
        sb.append("total=").append(ledger.totalDamage());
        sb.append(";player=").append(ledger.playerDamage());
        sb.append(";other=").append(ledger.otherDamage());
        sb.append(";dealtA=").append(ledger.dealtBy(A));
        sb.append(";dealtB=").append(ledger.dealtBy(B));
        sb.append(";takenA=").append(ledger.takenBy(A));
        sb.append(";takenR=").append(ledger.takenBy(RYLIE));
        sb.append(";exclHits=").append(ledger.shareExcludedHits());
        sb.append(";exclDmg=").append(ledger.shareExcludedDamage());
        sb.append(";exclH=").append(ledger.heightExcludedHits());
        sb.append(";exclR=").append(ledger.rangeExcludedHits());
        sb.append(";damagers=").append(ledger.damagerCount());
        sb.append(";participants=").append(ledger.participants().size());
        sb.append(";pool=").append(ledger.maxHealth());
        sb.append(";share=").append(ledger.playerSharePercent());
        sb.append(";govPool=").append(governor.poolRemaining());
        sb.append(";govTick=").append(governor.poolTick());
        double remainingFraction = 1.0 - ledger.totalDamage() / ledger.maxHealth();
        sb.append(";beatsPending=").append(ledger.milestoneBeats().pending(remainingFraction));
        sb.append(";snapshot=").append(ledger.dealtSnapshot());
        return sb.toString();
    }

    public static void main(String[] args) {
        FeedbackSettings on = FeedbackSettings.defaults();
        FeedbackSettings off = FeedbackSettings.sanitized(
                false, "off", null, null, 64, 500, 1000);

        String bare = run(false, on);
        check(lastDigestLength == 0, "the bare run computed no feedback");
        String observed = run(true, on);
        check(lastDigestLength > 0,
                "the observed run really computed titles + lines after every record");
        int onDigest = lastDigestLength;
        String observedOff = run(true, off);
        check(lastDigestLength == 0,
                "the OFF run's observer short-circuited (the adapter's off path)");
        check(onDigest > 0, "the ON digest was non-empty");

        check(bare.equals(observed),
                "feedback ON (observer after every record): ledger identical to no feedback\n"
                        + "  bare:     " + bare + "\n  observed: " + observed);
        check(bare.equals(observedOff),
                "feedback OFF at parse level: ledger identical (the off path is a no-op)\n"
                        + "  bare: " + bare + "\n  off:  " + observedOff);

        // The governed values themselves, pinned so the equality
        // above can never be equality-of-two-wrongs:
        // dealt = 10 + 16.5 + 45 + 0 + 45 + 20(banded) + 23.8 + 7 = 167.3
        check(bare.contains("total=167.3"), "sequence total is the hand-computed 167.3");
        check(bare.contains("dealtA=71.5"), "A dealt 10 + 16.5 + 45 + 0 = 71.5");
        check(bare.contains("dealtB=68.8"), "B dealt 45 + 23.8 = 68.8 (banded 20 excluded)");
        check(bare.contains("takenA=5.0"), "A took the 5.0 boss hit; the 0.0 reflect recorded nothing");
        check(bare.contains("takenR=2.0"), "Rylie's taken is the TW-shaped 2.0 (8.0 x 0.25)");
        check(bare.contains("exclHits=1"), "exactly one banded exclusion");

        // --- Dragon shape: phase views computed across the flip
        // boundary leave the battle ledger untouched.
        EncounterLedger dragon = new EncounterLedger(
                BossKind.ENDER_DRAGON, UUID.randomUUID(), 20_000, T0);
        dragon.recordDealt(A, 4_000, T0 + 1);
        String before = snapshot(dragon, new BossGovernor());
        FeedbackTotals p1 = FeedbackTotals.forPhase(4_000, 4_000);
        FeedbackTotals p2 = FeedbackTotals.forPhase(16_000, 0);
        dragon.recordDealt(A, 11.0, T0 + 2);
        FeedbackTotals p2After = FeedbackTotals.forPhase(16_000, 11.0);
        String after = snapshot(dragon, new BossGovernor());
        check(p1.remaining() == 0.0 && p2.remaining() == 16_000,
                "phase views flip 4,000 -> 16,000 at the boundary");
        check(p2After.remaining() == 15_989,
                "phase 2 view follows phase damage only");
        check(!before.equals(after) && after.contains("total=4011.0"),
                "the dragon ledger moved only by the recorded hit (4,000 + 11)");
        check(FeedbackTotals.fromLedger(dragon).dealt() == 4_011.0,
                "computing phase views never wrote to the ledger");

        System.out.println("FeedbackNoDamageChangeTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
