/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.protect;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The ExcelProtect gateway's fail-soft contract (0.3.4): this
 * harness runs with Protect's API jar on the classpath but NO
 * server and NO Protect plugin - the exact shape of a boot
 * where the adapter bound against a Protect that then failed.
 * Every publish must come back quietly: no throw, ever, however
 * often it is called. (The landing side - the records actually
 * arriving in Protect's streams - is rig-proven; a bare
 * classpath has no event bus to land on, by design.)
 */
public final class ProtectEmitGatewayTest {

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
        ProtectEmitGateway gateway = new ProtectEmitGateway(
                Logger.getLogger("ProtectEmitGatewayTest"));
        boolean quiet = true;
        try {
            gateway.fightTable("wither", "CREDIT", "Kailey", 480.0, 60.0,
                    List.of(Map.of("name", "Kailey", "damage_dealt", 300.0)));
            gateway.fightTable("wither", "DENY_SHARE", "", 0.0, 0.0, List.of());
            gateway.record("combat", "boss_state",
                    Map.of("schema", 1, "state", "fight_started"));
            // Repeated failures stay quiet too (the warning cap
            // is an implementation detail; the contract is only
            // that nothing ever throws into a fight).
            for (int i = 0; i < 6; i++) {
                gateway.record("combat", "boss_state",
                        Map.of("schema", 1, "state", "respawned"));
            }
        } catch (Throwable t) {
            quiet = false;
            System.out.println("gateway threw: " + t);
        }
        check(quiet, "every publish shape fails soft with no server bus");

        System.out.println("ProtectEmitGatewayTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
