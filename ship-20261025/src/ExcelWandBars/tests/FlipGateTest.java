import com.winkreyes.excelwandbars.core.FlipGate;
import com.winkreyes.excelwandbars.core.FlipGate.Result;
import java.util.UUID;

public final class FlipGateTest {
    static FlipGate.State open() {
        FlipGate.State s = new FlipGate.State();
        s.sneakStart = true; s.hasActiveWand = true; s.inventoryMode = true; s.barOpen = true; s.barCount = 3;
        return s;
    }
    public static void run() {
        System.out.println("FlipGateTest");
        FlipGate.Options o = new FlipGate.Options();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        FlipGate g = new FlipGate();
        Check.eq(Result.FLIP, g.decide(a, open(), o, 1000), "open bar + sneak press flips");
        Check.eq(Result.DEBOUNCED, g.decide(a, open(), o, 1100), "second press within 250 ms ignored");
        Check.eq(Result.DEBOUNCED, g.decide(a, open(), o, 1249), "249 ms still debounced");
        Check.eq(Result.FLIP, g.decide(a, open(), o, 1250), "250 ms later flips again");
        Check.eq(Result.FLIP, g.decide(b, open(), o, 1100), "debounce is per player");
        FlipGate.State s = open(); s.sneakStart = false;
        Check.eq(Result.NOT_SNEAK_START, g.decide(a, s, o, 5000), "sneak release never flips");
        s = open(); s.hasActiveWand = false;
        Check.eq(Result.NO_WAND, g.decide(a, s, o, 6000), "no wand in hand: nothing");
        s = open(); s.barOpen = false;
        Check.eq(Result.BAR_CLOSED, g.decide(a, s, o, 7000), "bar closed (controller players): nothing");
        s = open(); s.inventoryMode = false;
        Check.eq(Result.WRONG_MODE, g.decide(a, s, o, 8000), "chest-mode wand (per-wand override): nothing");
        s = open(); s.barCount = 1;
        Check.eq(Result.ONE_BAR, g.decide(a, s, o, 9000), "single bar: nothing");
        s = open(); s.skipWorld = true;
        Check.eq(Result.SKIP_WORLD, g.decide(a, s, o, 10000), "EliteMobs world: nothing");
        s = open(); s.inVehicle = true;
        Check.eq(Result.SKIP_VEHICLE, g.decide(a, s, o, 11000), "boat/horse dismount: nothing");
        s = open(); s.swimming = true;
        Check.eq(Result.SKIP_SWIMMING, g.decide(a, s, o, 12000), "swim down: nothing");
        s = open(); s.gliding = true;
        Check.eq(Result.SKIP_GLIDING, g.decide(a, s, o, 13000), "elytra: nothing");
        s = open(); s.flying = true;
        Check.eq(Result.SKIP_FLYING, g.decide(a, s, o, 14000), "creative fly down: nothing");
        o.skipFlying = false;
        Check.eq(Result.FLIP, g.decide(a, s, o, 15000), "skip-flying false: flying flips");
        o.enabled = false;
        Check.eq(Result.DISABLED, g.decide(a, open(), o, 20000), "disabled in config: nothing");
        o.enabled = true;
        // skipped presses do not start a debounce window
        FlipGate g2 = new FlipGate();
        FlipGate.State c = open(); c.barOpen = false;
        g2.decide(a, c, o, 100);
        Check.eq(Result.FLIP, g2.decide(a, open(), o, 150), "a skipped press does not debounce the next real one");
        Check.eq(1, g2.tracked(), "one player tracked");
        g2.forget(a);
        Check.eq(0, g2.tracked(), "forget on quit clears state");
        // clock going backwards never blocks
        FlipGate g3 = new FlipGate();
        g3.decide(a, open(), o, 5000);
        Check.eq(Result.FLIP, g3.decide(a, open(), o, 4000), "clock step back does not lock flips");
        Check.eq(1, FlipGate.next(0, 3), "bar 1 -> 2");
        Check.eq(2, FlipGate.next(1, 3), "bar 2 -> 3");
        Check.eq(0, FlipGate.next(2, 3), "bar 3 -> 1");
        Check.eq(0, FlipGate.next(0, 0), "no bars -> 0");
    }
}
