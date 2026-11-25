/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.sprinkle;

import java.util.logging.Logger;

/**
 * Elite sprinkle gateway, fail-soft (0.3.3, study sections 1.1
 * and 1.3): with EliteMobs ABSENT - this harness's classpath has
 * no EliteMobs jar, and no server is running - the gateway must
 * report unavailable, refuse every operation, and never throw.
 * That is the adapter contract the whole feature rests on:
 * BossTrial works standalone, and a missing or moved EliteMobs
 * leaves seal fights exactly as they were.
 *
 * (The live conversion path - constructor, tag read-back, loot
 * flags, removal - is proven on the Paper rig against the real
 * EliteMobs 10.9.12 jar; see the build's TEST-REPORT.)
 */
public final class EliteSprinkleGatewayTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        Logger log = Logger.getLogger("sprinkle-gateway-test");
        EliteSprinkleGateway gw = new EliteSprinkleGateway(log);

        boolean threw = false;
        boolean available = true;
        try {
            available = gw.available();
        } catch (Throwable t) {
            threw = true;
        }
        check(!threw, "available() never throws with no server / no EliteMobs");
        check(!available, "the gateway reports unavailable when EliteMobs is absent");
        check(gw.seamDead(), "the seam marks itself dead after the failed resolution");

        check(!gw.isElite(null), "isElite(null) is false, not a throw");
        check(gw.eliteLevelOf(null) == -1, "eliteLevelOf(null) is -1");
        check(gw.eliteOf(null) == null, "eliteOf(null) is null");
        check(!gw.removeElite(null), "removeElite(null) is false (caller falls back)");

        EliteSprinkleGateway.Conversion conv = null;
        try {
            conv = gw.convertAndContain(null, 20);
        } catch (Throwable t) {
            threw = true;
        }
        check(!threw && conv != null && !conv.ok(),
                "convertAndContain(null) refuses cleanly");

        // A second gateway over the same absent plugin agrees -
        // resolution state is per-gateway and deterministic.
        EliteSprinkleGateway gw2 = new EliteSprinkleGateway(log);
        check(!gw2.available() && gw2.seamDead(),
                "a fresh gateway reaches the same unavailable verdict");

        System.out.println("EliteSprinkleGatewayTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
