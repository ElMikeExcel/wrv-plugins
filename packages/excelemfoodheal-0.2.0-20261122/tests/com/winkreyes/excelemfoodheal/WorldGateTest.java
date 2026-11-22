import com.winkreyes.excelemfoodheal.WorldGate;

/** Gate truth table: registry OR list; neither = out of scope. */
public final class WorldGateTest {

    public static void run() {
        System.out.println("WorldGateTest");
        Check.ok(WorldGate.isEliteMobsWorld(true, true), "registry + list -> in scope");
        Check.ok(WorldGate.isEliteMobsWorld(true, false), "registry only (instanced dungeon) -> in scope");
        Check.ok(WorldGate.isEliteMobsWorld(false, true), "list only (permanent world) -> in scope");
        Check.ok(!WorldGate.isEliteMobsWorld(false, false), "neither (overworld/nether/end) -> out of scope");
    }
}
