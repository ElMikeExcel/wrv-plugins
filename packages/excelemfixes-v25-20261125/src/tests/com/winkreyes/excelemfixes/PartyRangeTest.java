import excelemfixes.partydrops.PartyRange;

/**
 * Range conformance vs ExcelTogether's PartyBonus.within semantics
 * (spec §2, §9): squared distance, INCLUSIVE boundary, cross-world
 * never. ExcelTogether's own test corpus used the same canonical
 * cases (axis 50, diagonal 30/40, just-past 50).
 */
public class PartyRangeTest {

    public static void run() {
        System.out.println("PartyRangeTest");
        double r = PartyRange.DEFAULT_RADIUS;
        Check.close(50.0, r, "DEFAULT_RADIUS mirrors ExcelTogether (50.0)");

        Check.ok(PartyRange.within(true, 0, 0, 0, r), "same point counts");
        Check.ok(PartyRange.within(true, 50.0, 0, 0, r), "exactly 50.0 on +x counts (inclusive)");
        Check.ok(PartyRange.within(true, -50.0, 0, 0, r), "exactly 50.0 on -x counts");
        Check.ok(PartyRange.within(true, 0, 50.0, 0, r), "exactly 50.0 vertical counts");
        Check.ok(PartyRange.within(true, 0, 0, -50.0, r), "exactly 50.0 on -z counts");
        Check.ok(!PartyRange.within(true, 50.0001, 0, 0, r), "50.0001 does not count");
        Check.ok(!PartyRange.within(true, 51, 0, 0, r), "51 does not count");
        Check.ok(PartyRange.within(true, 30, 40, 0, r), "diagonal 30/40/0 = exactly 50 counts");
        Check.ok(PartyRange.within(true, 30, 40, 0.0001, r) == false, "diagonal a hair past 50 does not count");
        Check.ok(PartyRange.within(true, 10, 10, 10, r), "clearly inside counts");
        Check.ok(!PartyRange.within(false, 0, 0, 0, r), "cross-world never counts, even at zero distance");
        Check.ok(!PartyRange.within(false, 10, 0, 0, r), "cross-world never counts in range either");

        // a different configured radius obeys the same rule (config plumbing)
        Check.ok(PartyRange.within(true, 10, 0, 0, 10.0), "radius 10: exactly 10 counts");
        Check.ok(!PartyRange.within(true, 10.0001, 0, 0, 10.0), "radius 10: 10.0001 does not");
    }
}
