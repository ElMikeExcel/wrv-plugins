import com.winkreyes.exceltrainingwheels.core.SiegeBook;
import static com.winkreyes.exceltrainingwheels.core.SiegeBook.Event.*;

import java.util.List;
import java.util.UUID;

public class SiegeBookTest {
    static final UUID Z1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID Z2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID P1 = UUID.fromString("00000000-0000-0000-0009-000000000001");
    static final UUID VET = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID VET2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

    public static void run() {
        System.out.println("SiegeBookTest");

        // Claim, progress, completion exactly at the threshold.
        SiegeBook book = new SiegeBook(8, 4, 60);
        Check.eq(STARTED, book.observe("doorA", Z1, VET, false, 0), "first observation starts the siege");
        Check.eq(1L, book.steps("doorA"), "start counts as step 1");
        Check.eq(PROGRESS, book.observe("doorA", Z1, VET, false, 10), "second observation progresses");
        Check.close(0.5, book.progress("doorA"), "progress 2/4 = 0.5 (crack stage)");
        Check.eq(PROGRESS, book.observe("doorA", Z1, VET, false, 20), "third observation progresses");
        Check.eq(COMPLETED, book.observe("doorA", Z1, VET, false, 30), "fourth observation completes at threshold");
        Check.eq(0, book.size(), "completed siege leaves the book");
        Check.ok(!book.active("doorA"), "door free after completion");

        // Threshold of 1 completes on the first observation.
        SiegeBook instant = new SiegeBook(8, 1, 60);
        Check.eq(COMPLETED, instant.observe("doorA", Z1, VET, false, 0), "threshold 1 completes immediately");
        Check.eq(0, instant.size(), "instant completion leaves nothing tracked");

        // Target change mid-siege: full reset, fresh claim next cadence.
        SiegeBook swap = new SiegeBook(8, 10, 60);
        swap.observe("doorA", Z1, VET, false, 0);
        swap.observe("doorA", Z1, VET, false, 10);
        Check.eq(RESET, swap.observe("doorA", Z1, VET2, false, 20), "same zombie, new target: reset");
        Check.eq(0, swap.size(), "reset clears the siege");
        Check.eq(STARTED, swap.observe("doorA", Z1, VET2, false, 30), "new hunt can claim the door fresh");
        Check.eq(1L, swap.steps("doorA"), "fresh claim starts from step 1 (no carried progress)");

        // Protected target: never starts.
        SiegeBook prot = new SiegeBook(8, 10, 60);
        Check.eq(IGNORED, prot.observe("doorA", Z1, P1, true, 0), "protected target never starts a siege");
        Check.eq(0, prot.size(), "nothing tracked for the protected target");

        // Target becomes protected mid-siege: stops cold.
        SiegeBook protMid = new SiegeBook(8, 10, 60);
        protMid.observe("doorA", Z1, VET, false, 0);
        protMid.observe("doorA", Z1, VET, false, 10);
        Check.eq(RESET, protMid.observe("doorA", Z1, VET, true, 20), "target gains protection mid-siege: reset");
        Check.eq(0, protMid.size(), "siege gone the moment the target is protected");
        Check.eq(IGNORED, protMid.observe("doorA", Z1, VET, true, 30), "protected observation at a busy-free door still ignored");

        // Multi-zombie same door: no stacking, single rate.
        SiegeBook duo = new SiegeBook(8, 10, 60);
        Check.eq(STARTED, duo.observe("doorA", Z1, VET, false, 0), "first zombie claims");
        Check.eq(IGNORED, duo.observe("doorA", Z2, VET, false, 0), "second zombie at the same door ignored (no stack)");
        Check.eq(PROGRESS, duo.observe("doorA", Z1, VET, false, 10), "credited zombie progresses");
        Check.eq(2L, duo.steps("doorA"), "two cadences of observations = 2 steps, not 3");

        // Credited zombie vanishes: sweep after grace frees the door.
        SiegeBook stale = new SiegeBook(8, 10, 60);
        stale.observe("doorA", Z1, VET, false, 100);
        Check.eq(List.of(), stale.sweep(160), "at exactly grace (60) the siege still stands");
        Check.eq(List.of("doorA"), stale.sweep(161), "past grace the siege resets (zombie died / lost the door)");
        Check.eq(0, stale.size(), "swept clean");
        Check.eq(STARTED, stale.observe("doorA", Z2, VET, false, 170), "another zombie may claim after the sweep");

        // Observations keep a siege alive across sweeps.
        SiegeBook alive = new SiegeBook(8, 100, 60);
        alive.observe("doorA", Z1, VET, false, 0);
        alive.observe("doorA", Z1, VET, false, 50);
        Check.eq(List.of(), alive.sweep(100), "recently-observed siege survives the sweep");
        Check.eq(2L, alive.steps("doorA"), "progress intact");

        // Hard cap on concurrent sieges.
        SiegeBook capped = new SiegeBook(2, 10, 60);
        Check.eq(STARTED, capped.observe("doorA", Z1, VET, false, 0), "cap: door 1 claims");
        Check.eq(STARTED, capped.observe("doorB", Z1, VET, false, 0), "cap: door 2 claims");
        Check.eq(IGNORED, capped.observe("doorC", Z1, VET, false, 0), "cap: door 3 refused at cap 2");
        for (int i = 0; i < 9; i++) capped.observe("doorA", Z1, VET, false, 10L * (i + 1));
        Check.eq(STARTED, capped.observe("doorC", Z1, VET, false, 100), "cap frees up once a siege completes");

        // Junk input never throws and never tracks.
        SiegeBook junk = new SiegeBook(8, 10, 60);
        Check.eq(IGNORED, junk.observe(null, Z1, VET, false, 0), "null door ignored");
        Check.eq(IGNORED, junk.observe("doorA", null, VET, false, 0), "null besieger ignored");
        Check.eq(IGNORED, junk.observe("doorA", Z1, null, false, 0), "null target ignored");
        Check.eq(0, junk.size(), "junk tracked nothing");
        Check.close(0.0, junk.progress("missing"), "progress of unknown door is 0");
    }
}
