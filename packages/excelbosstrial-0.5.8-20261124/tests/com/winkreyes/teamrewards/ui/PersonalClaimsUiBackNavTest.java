/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

/**
 * 0.5.7 (BB-18): the claims-chest Back contract, pinned plain-JVM.
 * The chest is a top-level screen, so its Back returns to the
 * ExcelMenu Server Menu through the fleet pattern (BB-14 BackNav,
 * verbatim): ARROW in a free bottom-row slot, the fleet label/lore,
 * performCommand("menu"), and the fleet failure line. This harness
 * pins the constants and the slot geometry only - the Bukkit render
 * and click wiring are proven by the bytecode check on the built jar
 * and by the in-game rig after install.
 */
public final class PersonalClaimsUiBackNavTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // Fleet contract, verbatim from BB-14's BackNav.
        check("menu".equals(PersonalClaimsUi.MENU_COMMAND),
                "menu command is \"menu\"");
        check("§eBack to Server Menu".equals(PersonalClaimsUi.BACK_LABEL),
                "back label is the fleet label");
        check("§7Return to the Server Menu".equals(PersonalClaimsUi.BACK_LORE),
                "back lore is the fleet lore");
        check("§cThe Server Menu could not open.".equals(PersonalClaimsUi.BACK_FAILURE_LINE),
                "failure line is the fleet failure line");

        // Slot geometry: 48 is free in the 54-slot chest - claim
        // entries fill 0-44, paging sits at 45 / 49 / 53.
        check(PersonalClaimsUi.BACK_SLOT == 48, "back slot is 48");
        check(PersonalClaimsUi.BACK_SLOT >= 45 && PersonalClaimsUi.BACK_SLOT < 54,
                "back slot is in the bottom row");
        check(PersonalClaimsUi.BACK_SLOT != 45
                && PersonalClaimsUi.BACK_SLOT != 49
                && PersonalClaimsUi.BACK_SLOT != 53,
                "back slot collides with no paging control");
        check(PersonalClaimsUi.isBackSlot(48), "isBackSlot(48)");
        for (int slot = 0; slot < 54; slot++) {
            if (slot != 48) check(!PersonalClaimsUi.isBackSlot(slot),
                    "isBackSlot(" + slot + ") is false");
        }
        check(!PersonalClaimsUi.isBackSlot(-1), "isBackSlot(-1) is false");
        check(!PersonalClaimsUi.isBackSlot(54), "isBackSlot(54) is false");

        System.out.println("PersonalClaimsUiBackNavTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) throw new AssertionError(failed + " checks failed");
    }
}
