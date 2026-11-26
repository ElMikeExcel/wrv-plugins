import excelemfixes.partydrops.HintGate;

import java.util.UUID;

/** Hint cadence (spec §7): first showing teaches; cooldown keeps it rare; never spammy. */
public class HintGateTest {

    static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static void run() {
        System.out.println("HintGateTest");
        long cd = 600_000L; // 600 s
        HintGate gate = new HintGate(cd);
        long t0 = 1_000_000L;

        Check.ok(gate.tryShow(A, t0), "first multiplied kill shows the hint");
        Check.ok(!gate.tryShow(A, t0 + 1), "1 ms later: suppressed");
        Check.ok(!gate.tryShow(A, t0 + cd - 1), "just inside cooldown: suppressed");
        Check.ok(gate.tryShow(A, t0 + cd), "exactly at cooldown end: shows again");
        Check.ok(!gate.tryShow(A, t0 + cd + 1), "clock restarts from the latest showing");

        Check.ok(gate.tryShow(B, t0 + 1), "another player has their own clock");
        Check.ok(!gate.tryShow(null, t0), "null player never shows");

        HintGate zero = new HintGate(0L);
        Check.ok(zero.tryShow(A, t0) && zero.tryShow(A, t0), "cooldown 0 = every multiplied kill may show (config choice)");

        HintGate g2 = new HintGate(cd);
        Check.ok(g2.tryShow(A, t0), "fresh gate shows");
        g2.forget(A);
        Check.ok(g2.tryShow(A, t0 + 1), "forget() re-arms the teaching showing (rejoin)");

        // spamminess bound: 100 multiplied kills inside one cooldown show exactly once
        HintGate g3 = new HintGate(cd);
        int shows = 0;
        for (int i = 0; i < 100; i++) if (g3.tryShow(A, t0 + i * 1000L)) shows++;
        Check.eq(1, shows, "100 multiplied kills in 100 s show the hint exactly once");
    }
}
