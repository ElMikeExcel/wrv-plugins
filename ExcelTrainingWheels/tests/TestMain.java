public class TestMain {
    public static void main(String[] args) {
        ShieldRuleTest.run();
        RaidDivisionTest.run();
        CompensationTableTest.run();
        ProtectedRegistryTest.run();
        DoorGuardRuleTest.run();
        CopperDoorsTest.run();
        SiegeBookTest.run();
        ZombieDoorRuleTest.run();
        LifestealRuleTest.run();
        LogThrottleTest.run();
        CombatDivisorTest.run();
        BedrockGraceTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
