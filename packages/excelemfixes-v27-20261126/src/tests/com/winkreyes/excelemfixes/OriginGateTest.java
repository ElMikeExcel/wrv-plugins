import excelemfixes.partydrops.OriginGate;

/** Origin-gate matrix (spec §5, §9): NATURAL only; anti-exploit stands down; EM-absent inert. */
public class OriginGateTest {

    public static void run() {
        System.out.println("OriginGateTest");

        Check.ok(OriginGate.eligible(true, true, "NATURAL", false), "natural elite, no flags -> eligible");

        // every non-NATURAL spawn reason pays 1x
        String[] reasons = {"SPAWNER", "SPAWNER_EGG", "BREEDING", "RAID", "COMMAND", "CUSTOM",
                "BUILD_IRONGOLEM", "BUILD_WITHER", "MOUNT", "DISPENSE_EGG", "EGG", "NATURAL "};
        for (String reason : reasons) {
            Check.ok(!OriginGate.eligible(true, true, reason, false), "reason " + reason.trim() + " -> 1x");
        }
        Check.ok(!OriginGate.eligible(true, true, null, false), "null reason -> 1x");
        Check.ok(!OriginGate.eligible(true, true, "natural", false), "lowercase 'natural' is not the enum name -> 1x");

        Check.ok(!OriginGate.eligible(true, false, "NATURAL", false), "isNaturalEntity false -> 1x");
        Check.ok(!OriginGate.eligible(true, true, "NATURAL", true), "anti-exploit triggered -> 1x (stand-down)");
        Check.ok(!OriginGate.eligible(false, true, "NATURAL", false), "EM absent -> inert, 1x");
        Check.ok(!OriginGate.eligible(false, false, null, true), "everything wrong at once -> 1x");
    }
}
