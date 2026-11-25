public class TestMain {
    public static void main(String[] args) {
        QuestDestinationsTest.run();
        QuestWhereMenusTest.run();
        TrackerBlankScanTest.run();
        ArcaneMathTest.run();
        IndexTest.run();
        DropGuideMenusTest.run();
        ExpertiseTest.run();
        TrackerCatalogTest.run();
        TrackerMenusTest.run();
        ProtectKillsTest.run();
        TextUtilTest.run();
        FoodTableTest.run();
        WorldGateTest.run();
        HealEngineTest.run();
        BonusPolicyTest.run();
        LeashRemovalPolicyTest.run();
        ReturnPolicyTest.run();
        SealRestoreDataTest.run();
        DummyBossListTest.run();
        EndermanResistanceTest.run();
        FieldMedicTest.run();
        MeleePremiumTest.run();
        SkillNudgeMathTest.run();
        BonusCatalogTest.run();
        RespawnGraceTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
