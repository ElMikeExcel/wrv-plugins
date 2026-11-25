/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The guaranteed sprinkle cadence (0.4.0, PM-6): occurrence is
 * never rolled. An owed wave fires when there is room, defers
 * (unspent) when the arena is full, and retires only when the
 * per-fight budget is spent. The timed cadence is due 150 s
 * after the last fired wave - and immediately when none has
 * fired yet.
 */
public final class SprinkleCadenceTest {

    private static int passed;
    private static int failed;

    private static void check(boolean ok, String label) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // --- the cadence clock
        check(!SprinkleCadence.cadenceDue(0, 1_000, 150),
                "clock not started (stamp 0): not due - the service stamps it at the first tick");
        check(!SprinkleCadence.cadenceDue(1_000, 1_000 + 149_999, 150), "149.999 s: not yet due");
        check(SprinkleCadence.cadenceDue(1_000, 1_000 + 150_000, 150), "150 s: due");
        check(SprinkleCadence.cadenceDue(1_000, 1_000 + 600_000, 150), "long past: due");
        check(!SprinkleCadence.cadenceDue(0, 999_999_999, 0), "a zero cadence retires the timer");
        check(!SprinkleCadence.cadenceDue(0, 999_999_999, -5), "a negative cadence retires the timer");

        // --- nothing owed is idle, whatever the caps say
        check(SprinkleCadence.judge(false, 3, 0, 5, 0, 24).decision()
                == SprinkleCadence.Decision.IDLE, "not owed: IDLE");
        check(SprinkleCadence.judge(false, 3, 5, 5, 24, 24).decision()
                == SprinkleCadence.Decision.IDLE, "not owed at the caps: still IDLE");

        // --- an owed wave fires, sized by the tighter room
        SprinkleCadence.Outcome full = SprinkleCadence.judge(true, 3, 0, 5, 0, 24);
        check(full.decision() == SprinkleCadence.Decision.FIRE && full.spawnCount() == 3,
                "owed with room: FIRE the whole wave");
        SprinkleCadence.Outcome aliveBound = SprinkleCadence.judge(true, 3, 4, 5, 0, 24);
        check(aliveBound.decision() == SprinkleCadence.Decision.FIRE && aliveBound.spawnCount() == 1,
                "alive cap trims the wave to the room (partial wave)");
        SprinkleCadence.Outcome budgetBound = SprinkleCadence.judge(true, 3, 0, 5, 23, 24);
        check(budgetBound.decision() == SprinkleCadence.Decision.FIRE && budgetBound.spawnCount() == 1,
                "the per-fight budget trims the wave to the room");

        // --- a full arena defers; the wave is NOT spent
        check(SprinkleCadence.judge(true, 3, 5, 5, 0, 24).decision()
                == SprinkleCadence.Decision.DEFER, "arena full: DEFER (still owed next tick)");
        check(SprinkleCadence.judge(true, 3, 7, 5, 0, 24).decision()
                == SprinkleCadence.Decision.DEFER, "over-full arena: DEFER");

        // --- only the spent budget retires a wave
        check(SprinkleCadence.judge(true, 3, 0, 5, 24, 24).decision()
                == SprinkleCadence.Decision.SPENT, "budget spent: SPENT (retires)");
        check(SprinkleCadence.judge(true, 3, 5, 5, 24, 24).decision()
                == SprinkleCadence.Decision.SPENT, "budget spent beats a full arena: SPENT");
        check(SprinkleCadence.judge(true, 0, 0, 5, 0, 24).decision()
                == SprinkleCadence.Decision.SPENT, "an empty wave template retires");
        check(SprinkleCadence.judge(true, 3, 0, 5, 0, 0).decision()
                == SprinkleCadence.Decision.SPENT, "a zero per-fight cap retires");

        // --- the Nov 7 counterfactual: three stamps + cadence over
        // a 54-minute fight at the settled wither numbers can never
        // produce a zero-add fight: the first stamp alone fires.
        SprinkleCadence.Outcome firstStamp = SprinkleCadence.judge(true, 1, 0, 5, 0, 24);
        check(firstStamp.decision() == SprinkleCadence.Decision.FIRE,
                "the first milestone stamp always fires (no zero-add fight)");

        System.out.println("SprinkleCadenceTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
