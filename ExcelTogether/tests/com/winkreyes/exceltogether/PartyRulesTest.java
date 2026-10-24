package com.winkreyes.exceltogether;

import java.util.Set;
import java.util.UUID;

/** 0.4.0 plain-JVM checks for PartyRules (friendly fire, party drops verdicts, drop math, kill cap). */
public final class PartyRulesTest {
    static int pass, fail;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    static PartyRules.Kill good() {
        PartyRules.Kill k = new PartyRules.Kill();
        k.killerIsPlayer = true; k.type = "ZOMBIE"; k.spawnReason = "NATURAL"; k.killerInParty = true; k.togetherCount = 3;
        return k;
    }
    static PartyRules.Verdict j(PartyRules.Kill k) { return PartyRules.judge(true, k, 3, PartyRules.DEFAULT_ALLOWED_REASONS, PartyRules.DEFAULT_EXCLUDED_TYPES); }

    public static void main(String[] a) {
        UUID x = UUID.randomUUID(), y = UUID.randomUUID(), z = UUID.randomUUID();
        java.util.function.BiPredicate<UUID, UUID> party = (p, q) -> !(p.equals(z) || q.equals(z));   // x,y partied; z alone
        check("ff blocks same party", PartyRules.blockFriendlyFire(true, x, y, party));
        check("ff allows outsider", !PartyRules.blockFriendlyFire(true, x, z, party));
        check("ff allows self", !PartyRules.blockFriendlyFire(true, x, x, party));
        check("ff null attacker", !PartyRules.blockFriendlyFire(true, null, y, party));
        check("ff disabled", !PartyRules.blockFriendlyFire(false, x, y, party));

        check("boost natural zombie 3 together", j(good()) == PartyRules.Verdict.BOOST);
        PartyRules.Kill k;
        k = good(); k.togetherCount = 2; check("2 together no", j(k) == PartyRules.Verdict.TOO_FEW_TOGETHER);
        k = good(); k.togetherCount = 5; check("5 together yes", j(k).boost());
        k = good(); k.killerIsPlayer = false; check("no player kill", j(k) == PartyRules.Verdict.NO_KILLER);
        k = good(); k.eliteMobs = true; check("elitemobs skipped", j(k) == PartyRules.Verdict.ELITEMOBS);
        k = good(); k.fromSpawner = true; check("spawner flag", j(k) == PartyRules.Verdict.SPAWNER);
        for (String r : new String[]{"SPAWNER", "TRIAL_SPAWNER", "SPAWNER_EGG", "EGG", "BREEDING", "COMMAND", "CUSTOM", "RAID", "NETHER_PORTAL", "BUILD_IRONGOLEM", "VILLAGE_DEFENSE", "DUPLICATION", "SPELL", "OMINOUS_ITEM_SPAWNER", "DISPENSE_EGG", "BUCKET", "REANIMATE"}) {
            k = good(); k.spawnReason = r; check("reason " + r + " skipped", j(k) == PartyRules.Verdict.SPAWN_REASON);
        }
        k = good(); k.spawnReason = null; check("unknown reason skipped", j(k) == PartyRules.Verdict.SPAWN_REASON);
        for (String t : new String[]{"PLAYER", "WITHER", "ENDER_DRAGON", "WARDEN", "ELDER_GUARDIAN", "VILLAGER", "IRON_GOLEM"}) {
            k = good(); k.type = t; check("type " + t + " skipped", j(k) == PartyRules.Verdict.EXCLUDED_TYPE);
        }
        k = good(); k.tamed = true; check("tamed skipped", j(k) == PartyRules.Verdict.TAMED);
        k = good(); k.killerInParty = false; check("not in party", j(k) == PartyRules.Verdict.NOT_IN_PARTY);
        k = good(); k.underKillCap = false; check("kill cap", j(k) == PartyRules.Verdict.KILL_CAP);
        check("disabled", PartyRules.judge(false, good(), 3, PartyRules.DEFAULT_ALLOWED_REASONS, PartyRules.DEFAULT_EXCLUDED_TYPES) == PartyRules.Verdict.DISABLED);
        check("lowercase config reasons", PartyRules.judge(true, good(), 3, PartyRules.upperSet(java.util.List.of("natural"), Set.of()), Set.of()).boost());

        java.util.function.DoubleSupplier boom = () -> { throw new AssertionError("rng used"); };
        check("x2 of 1 = +1", PartyRules.extraItems(1, 2.0, boom) == 1);
        check("x2 of 7 = +7", PartyRules.extraItems(7, 2.0, boom) == 7);
        check("x3 of 2 = +4", PartyRules.extraItems(2, 3.0, boom) == 4);
        check("x1 none", PartyRules.extraItems(5, 1.0, boom) == 0);
        check("NaN none", PartyRules.extraItems(5, Double.NaN, boom) == 0);
        check("x1.5 of 1 roll hit", PartyRules.extraItems(1, 1.5, () -> 0.2) == 1);
        check("x1.5 of 1 roll miss", PartyRules.extraItems(1, 1.5, () -> 0.8) == 0);
        java.util.Random r = new java.util.Random(7); long sum = 0;
        for (int i = 0; i < 100000; i++) sum += PartyRules.extraItems(1, 1.5, r::nextDouble);
        check("x1.5 expectation", Math.abs(sum / 100000.0 - 0.5) < 0.01);

        PartyRules.KillCap cap = new PartyRules.KillCap(3);
        long t = 1_000_000L;
        check("cap 1", cap.tryUse(x, t)); check("cap 2", cap.tryUse(x, t + 1)); check("cap 3", cap.tryUse(x, t + 2));
        check("cap full", !cap.wouldAllow(x, t + 3) && !cap.tryUse(x, t + 3));
        check("cap other player", cap.tryUse(y, t + 3));
        check("cap rolls after an hour", cap.wouldAllow(x, t + 3_600_000L) && cap.tryUse(x, t + 3_600_000L));
        check("no cap", new PartyRules.KillCap(0).tryUse(x, t));

        check("line", PartyRules.dropsOnLine(3, 2.0).equals("[Party] 3 of you are together: mob drops x2!"));
        check("line ascii", PartyRules.dropsOnLine(4, 2.5).chars().allMatch(c -> c < 128));
        System.out.println("PartyRulesTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
