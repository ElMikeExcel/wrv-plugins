package com.winkreyes.rewardguard;

import java.util.List;
import java.util.UUID;

/**
 * Policy tests for the 1.2 fall-softening signature (pure logic, no server).
 * The rules under test, as documented on FallDamageTracker:
 *   - cumulative fall damage vs the max health at the most recent fall;
 *   - healing never reduces the total (there is deliberately no heal API);
 *   - the comparison is >= the threshold;
 *   - entries exist only for entities that took real fall damage, and are
 *     dropped on death / removal / chunk unload / plugin clear;
 *   - classification composition: origin verdict wins the label, a kill is
 *     farmed at most once.
 * The threshold under test is the shipped default, 50, on a 20-health mob
 * (a zombie / skeleton health bar) unless a case says otherwise.
 */
public final class FallDamageTrackerTests {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("check " + assertions); }

    private static void checkThresholdParse(Object raw, int percent, boolean adjusted) {
        RewardScale.ParsedPercent parsed = RewardScale.parsePercent(raw, FallDamageTracker.DEFAULT_THRESHOLD_PERCENT);
        check(parsed.percent() == percent);
        check(parsed.adjusted() == adjusted);
    }

    public static void main(String[] args) {
        UUID zombie = UUID.randomUUID();
        UUID skeleton = UUID.randomUUID();

        // Full-health cave kill: no fall history at all -> not softened, no
        // record, and the composed classification is "not farmed".
        var t = new FallDamageTracker();
        check(!t.hasRecord(zombie));
        check(t.fallDamage(zombie) == 0.0);
        check(t.fallDamagePercent(zombie) == 0.0);
        check(!t.isSoftened(zombie, 50));
        check(t.size() == 0);
        check(FallDamageTracker.farmedReason(false, "unclassified_origin", t.isSoftened(zombie, 50)) == null);

        // The sky-farm shape: a 12-damage fall on a 20-health mob (60%) ->
        // softened at the default threshold.
        t.recordFall(zombie, 12.0, 20.0);
        check(t.hasRecord(zombie));
        check(t.fallDamage(zombie) == 12.0);
        check(t.fallDamagePercent(zombie) == 60.0);
        check(t.isSoftened(zombie, 50));
        check("fall_softened".equals(FallDamageTracker.farmedReason(false, "unclassified_origin", t.isSoftened(zombie, 50))));

        // Exactly at the threshold counts (the comparison is >=).
        var tEdge = new FallDamageTracker();
        tEdge.recordFall(zombie, 10.0, 20.0);
        check(tEdge.fallDamagePercent(zombie) == 50.0);
        check(tEdge.isSoftened(zombie, 50));

        // Just below the threshold does not count.
        var tBelow = new FallDamageTracker();
        tBelow.recordFall(zombie, 9.9, 20.0);
        check(!tBelow.isSoftened(zombie, 50));
        check(tBelow.fallDamagePercent(zombie) == 49.5);

        // Multiple smaller falls accumulate across the mob's tracked life.
        var tMulti = new FallDamageTracker();
        tMulti.recordFall(zombie, 6.0, 20.0);
        check(!tMulti.isSoftened(zombie, 50));       // 30% so far
        tMulti.recordFall(zombie, 6.0, 20.0);
        check(tMulti.fallDamage(zombie) == 12.0);    // 60% total
        check(tMulti.isSoftened(zombie, 50));

        // Fractional accumulation stays honest around the boundary:
        // 3 x 3.3 = 9.9 (49.5%, not softened); one more small fall tips it.
        var tFrac = new FallDamageTracker();
        tFrac.recordFall(zombie, 3.3, 20.0);
        tFrac.recordFall(zombie, 3.3, 20.0);
        tFrac.recordFall(zombie, 3.3, 20.0);
        check(!tFrac.isSoftened(zombie, 50));
        tFrac.recordFall(zombie, 0.2, 20.0);
        check(tFrac.isSoftened(zombie, 50));

        // HEALED-MOB RULE: the total is cumulative and healing does not wash
        // it off. The tracker deliberately has no heal/regen input, so a mob
        // that fell for 60%, healed back to full, and is then killed is
        // still softened at kill time. A second fall after healing stacks on
        // top of the first, so cumulative damage can exceed max health.
        var tHeal = new FallDamageTracker();
        tHeal.recordFall(zombie, 12.0, 20.0);        // falls to 8/20, then heals to full (no tracker event)
        check(tHeal.isSoftened(zombie, 50));         // still softened after the heal
        check(tHeal.fallDamage(zombie) == 12.0);     // total unchanged by healing
        tHeal.recordFall(zombie, 12.0, 20.0);        // healed mob falls again
        check(tHeal.fallDamage(zombie) == 24.0);
        check(tHeal.fallDamagePercent(zombie) == 120.0);
        check(tHeal.isSoftened(zombie, 100));        // softened even at a 100 threshold

        // LATEST-MAX-HEALTH RULE: the ratio uses the max health observed at
        // the most recent fall. A mob buffed from 20 to 40 max health after
        // its fall is judged against 40.
        var tMax = new FallDamageTracker();
        tMax.recordFall(zombie, 10.0, 20.0);
        check(tMax.isSoftened(zombie, 50));          // 10/20 = 50%
        tMax.recordFall(zombie, 0.5, 40.0);          // small fall after a max-health buff
        check(tMax.fallDamage(zombie) == 10.5);
        check(tMax.fallDamagePercent(zombie) == 26.25);
        check(!tMax.isSoftened(zombie, 50));         // 10.5/40 = 26.25%

        // Threshold endpoints: 0 means any recorded fall damage counts;
        // 100 needs the cumulative total to reach the whole health bar.
        var tZero = new FallDamageTracker();
        tZero.recordFall(zombie, 0.5, 20.0);
        check(tZero.isSoftened(zombie, 0));
        var tHundred = new FallDamageTracker();
        tHundred.recordFall(zombie, 19.9, 20.0);
        check(!tHundred.isSoftened(zombie, 100));
        tHundred.recordFall(zombie, 0.1, 20.0);
        check(tHundred.isSoftened(zombie, 100));

        // No entry is ever created for damage that did not really happen:
        // zero, negative and NaN damage, non-positive or NaN max health,
        // and null ids are all ignored (the map stays bounded to real falls).
        var tNone = new FallDamageTracker();
        tNone.recordFall(zombie, 0.0, 20.0);
        tNone.recordFall(zombie, -3.0, 20.0);
        tNone.recordFall(zombie, Double.NaN, 20.0);
        tNone.recordFall(zombie, 5.0, 0.0);
        tNone.recordFall(zombie, 5.0, -20.0);
        tNone.recordFall(zombie, 5.0, Double.NaN);
        tNone.recordFall(null, 5.0, 20.0);
        check(tNone.size() == 0);
        check(!tNone.hasRecord(zombie));
        check(!tNone.isSoftened(null, 50));
        check(tNone.fallDamage(null) == 0.0);
        check(tNone.fallDamagePercent(null) == 0.0);

        // Entities are independent: falls on one never soften another.
        var tTwo = new FallDamageTracker();
        tTwo.recordFall(zombie, 15.0, 20.0);
        check(tTwo.isSoftened(zombie, 50));
        check(!tTwo.isSoftened(skeleton, 50));
        check(tTwo.size() == 1);

        // DEATH CLEANUP: the death handler removes the victim's record for
        // EVERY death, player-caused or not (a non-player kill pays no Jobs
        // or mcMMO rewards to anyone, so nothing can be scaled for it, and
        // the record must not survive to soften a future same-UUID check).
        var tDeath = new FallDamageTracker();
        tDeath.recordFall(zombie, 15.0, 20.0);
        check(tDeath.isSoftened(zombie, 50));
        tDeath.remove(zombie);                        // the MONITOR death-cleanup call
        check(!tDeath.hasRecord(zombie));
        check(!tDeath.isSoftened(zombie, 50));
        check(tDeath.size() == 0);
        tDeath.remove(zombie);                        // removing twice is a no-op
        tDeath.remove(null);                          // and so is removing null
        check(tDeath.size() == 0);

        // DESPAWN / CHUNK-UNLOAD CLEANUP: bulk removal drops exactly the
        // listed entities and leaves the rest tracked.
        var tUnload = new FallDamageTracker();
        UUID third = UUID.randomUUID();
        tUnload.recordFall(zombie, 15.0, 20.0);
        tUnload.recordFall(skeleton, 15.0, 20.0);
        tUnload.recordFall(third, 15.0, 20.0);
        check(tUnload.size() == 3);
        tUnload.removeAll(List.of(zombie, skeleton)); // one chunk's entities unload
        check(tUnload.size() == 1);
        check(!tUnload.hasRecord(zombie));
        check(!tUnload.hasRecord(skeleton));
        check(tUnload.isSoftened(third, 50));
        tUnload.removeAll(null);                      // null bulk input tolerated
        tUnload.removeAll(List.of());                 // empty bulk input tolerated
        check(tUnload.size() == 1);

        // PLUGIN DISABLE / RELOAD: clear() empties the map so no fall
        // history leaks across a reload.
        var tClear = new FallDamageTracker();
        tClear.recordFall(zombie, 15.0, 20.0);
        tClear.clear();
        check(tClear.size() == 0);
        check(!tClear.isSoftened(zombie, 50));

        // OVERLAP (spawner-origin + fall): the kill is farmed ONCE and the
        // origin verdict wins the label, so logs never show a spawner kill
        // as a fall kill and no path can scale it twice.
        check("spawn_origin_spawner".equals(
            FallDamageTracker.farmedReason(true, "spawn_origin_spawner", true)));
        check("spawn_origin_spawner".equals(
            FallDamageTracker.farmedReason(true, "spawn_origin_spawner", false)));
        check("fall_softened".equals(
            FallDamageTracker.farmedReason(false, "unclassified_origin", true)));
        check(FallDamageTracker.farmedReason(false, "unclassified_origin", false) == null);

        // Threshold config parsing (same defensive parser as the reward
        // percent, with the fall default of 50): missing -> 50 unflagged;
        // valid -> itself; out of range -> clamped and flagged; fractional
        // -> truncated and flagged; garbage / wrong type / NaN -> default
        // and flagged.
        check(FallDamageTracker.DEFAULT_THRESHOLD_PERCENT == 50);
        checkThresholdParse(null, 50, false);
        checkThresholdParse(50, 50, false);
        checkThresholdParse(0, 0, false);
        checkThresholdParse(100, 100, false);
        checkThresholdParse("40", 40, false);
        checkThresholdParse(150, 100, true);
        checkThresholdParse(-5, 0, true);
        checkThresholdParse(49.9, 49, true);
        checkThresholdParse("abc", 50, true);
        checkThresholdParse(Boolean.TRUE, 50, true);
        checkThresholdParse(Double.NaN, 50, true);
        // The 1.1 entry point keeps its own default: missing still means 65.
        check(RewardScale.parsePercent(null).percent() == 65);

        System.out.println("PASS " + assertions + " fall-tracker assertions (sky-farm signature, >= threshold, accumulation, healed-mob cumulative rule, latest-max-health rule, no-entry guards, death/despawn/unload/disable cleanup, overlap composition, threshold parsing)");
    }
}
