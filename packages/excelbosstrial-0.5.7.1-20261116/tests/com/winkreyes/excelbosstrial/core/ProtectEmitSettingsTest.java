/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The ExcelProtect emitter switches (0.3.4): the settled
 * defaults (all ON - the shadow-live Protect is the suite's
 * record system and its seal-verdict slot exists for this
 * publish), the master's collapse (a master-off settings object
 * can never report a live sub-switch, so no consumer re-checks
 * the master), and dark() - the single "publish nothing"
 * predicate. The switches gate publishing ONLY; nothing in this
 * class is read by any fight rule.
 */
public final class ProtectEmitSettingsTest {

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
        ProtectEmitSettings d = ProtectEmitSettings.defaults();
        check(d.enabled() && d.fightTables() && d.stateChanges(),
                "defaults are all ON");
        check(!d.dark(), "defaults are not dark");

        ProtectEmitSettings off = ProtectEmitSettings.sanitized(false, true, true);
        check(!off.enabled() && !off.fightTables() && !off.stateChanges(),
                "master off collapses both sub-switches off");
        check(off.dark(), "master off is dark");

        ProtectEmitSettings tablesOnly = ProtectEmitSettings.sanitized(true, true, false);
        check(tablesOnly.enabled() && tablesOnly.fightTables() && !tablesOnly.stateChanges()
                && !tablesOnly.dark(), "tables only: live, state changes off");

        ProtectEmitSettings statesOnly = ProtectEmitSettings.sanitized(true, false, true);
        check(statesOnly.enabled() && !statesOnly.fightTables() && statesOnly.stateChanges()
                && !statesOnly.dark(), "states only: live, tables off");

        ProtectEmitSettings husk = ProtectEmitSettings.sanitized(true, false, false);
        check(husk.enabled() && husk.dark(),
                "master on with both sub-switches off is dark (bound but silent)");

        System.out.println("ProtectEmitSettingsTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
