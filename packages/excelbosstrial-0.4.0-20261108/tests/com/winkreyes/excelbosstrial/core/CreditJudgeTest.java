/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The verdict truth table and the presence geometry: the settled
 * rules (a would-have-qualified kill during a cooldown is denied
 * WITHOUT refreshing it; a failed kill during a cooldown keeps its
 * own denial verdict and does refresh; share failure outranks
 * presence; presence edges at exactly 128 / 129 blocks, dead,
 * wrong world, offline).
 */
public final class CreditJudgeTest {

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

    public static void main(String[] args) {
        // The truth table.
        check(CreditJudge.judge(true, true, false) == CreditJudge.Verdict.CREDIT,
                "share + presence + no cooldown = CREDIT");
        check(CreditJudge.judge(false, true, false) == CreditJudge.Verdict.DENY_SHARE,
                "share failure denies even with presence");
        check(CreditJudge.judge(true, false, false) == CreditJudge.Verdict.DENY_PRESENCE,
                "presence failure denies even at full share");
        check(CreditJudge.judge(false, false, false) == CreditJudge.Verdict.DENY_SHARE,
                "share is named when both fail (the headline reason)");
        check(CreditJudge.judge(true, true, true) == CreditJudge.Verdict.DENY_COOLDOWN,
                "a real fight during a cooldown still cannot credit");
        check(CreditJudge.judge(false, true, true) == CreditJudge.Verdict.DENY_SHARE,
                "a cheese kill during a cooldown is still a share failure (and refreshes it)");
        check(CreditJudge.judge(true, false, true) == CreditJudge.Verdict.DENY_PRESENCE,
                "an abandoned kill during a cooldown is still a presence failure (and refreshes it)");
        // Cooldown starts only from the two unearned-kill verdicts.
        check(CreditJudge.startsCooldown(CreditJudge.Verdict.DENY_SHARE), "DENY_SHARE starts a cooldown");
        check(CreditJudge.startsCooldown(CreditJudge.Verdict.DENY_PRESENCE), "DENY_PRESENCE starts a cooldown");
        check(!CreditJudge.startsCooldown(CreditJudge.Verdict.DENY_COOLDOWN),
                "DENY_COOLDOWN never refreshes the cooldown");
        check(!CreditJudge.startsCooldown(CreditJudge.Verdict.CREDIT), "CREDIT starts no cooldown");

        // Presence geometry (boss at origin).
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Map<UUID, CreditJudge.PresenceFact> facts = Map.of(
                a, new CreditJudge.PresenceFact(true, true, true, 128.0),
                b, new CreditJudge.PresenceFact(true, true, true, 129.0),
                c, new CreditJudge.PresenceFact(true, true, true, 12.0));
        check(CreditJudge.countPresent(Set.of(a), facts::get, 128.0) == 1,
                "exactly 128 blocks counts (within range)");
        check(CreditJudge.countPresent(Set.of(b), facts::get, 128.0) == 0,
                "129 blocks does not count");
        check(CreditJudge.countPresent(Set.of(a, b, c), facts::get, 128.0) == 2,
                "mixed distances count only the in-range");
        check(CreditJudge.countPresent(Set.of(), facts::get, 128.0) == 0,
                "no participants = nobody present");
        check(CreditJudge.countPresent(Set.of(a),
                id -> new CreditJudge.PresenceFact(true, false, true, 5.0), 128.0) == 0,
                "a dead participant does not count");
        check(CreditJudge.countPresent(Set.of(a),
                id -> new CreditJudge.PresenceFact(true, true, false, 5.0), 128.0) == 0,
                "wrong world does not count, even standing on the spot");
        check(CreditJudge.countPresent(Set.of(a),
                id -> new CreditJudge.PresenceFact(false, false, false, Double.MAX_VALUE), 128.0) == 0,
                "offline does not count");
        check(CreditJudge.countPresent(Set.of(a), id -> null, 128.0) == 0,
                "a null fact counts as absent, never as present");

        // Distance is true 3D.
        check(Math.abs(CreditJudge.distance(0, 0, 0, 3, 4, 0) - 5.0) < 1e-9,
                "3-4-5 distance");
        check(Math.abs(CreditJudge.distance(0, 64, 0, 0, 64, 128) - 128.0) < 1e-9,
                "flat 128 distance");

        System.out.println("CreditJudgeTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

