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
        VendorFlowTest.run();
        MeleePremiumTest.run();
        MeleeMitigationTest.run();
        SkillNudgeMathTest.run();
        BonusCatalogTest.run();
        RespawnGraceTest.run();
        KseaTest.run();
        EmCeilingTest.run();
        QuestBeaconTest.run();
        QuestGlowTest.run();
        DialogDerivationTest.run();
        ClarityMenusTest.run();
        FoodStackNormalizeTest.run();
        SkillsMenuModelTest.run();
        QuestChainCoreTest.run();
        CasterXpTest.run();
        ClassFormTableTest.run();
        QuestXpModuleTest.run();
        WitherSkullsModuleTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
