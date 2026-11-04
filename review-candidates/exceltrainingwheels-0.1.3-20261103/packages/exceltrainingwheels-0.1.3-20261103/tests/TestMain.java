public class TestMain {
    public static void main(String[] args) {
        ShieldRuleTest.run();
        RaidDivisionTest.run();
        BossDivisionTest.run();
        CompensationTableTest.run();
        ProtectedRegistryTest.run();
        DoorHoldRuleTest.run();
        CopperDoorsTest.run();
        SiegeBookTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
