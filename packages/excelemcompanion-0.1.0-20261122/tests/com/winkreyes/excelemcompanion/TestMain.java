public class TestMain {
    public static void main(String[] args) {
        IndexTest.run();
        DropGuideMenusTest.run();
        TextUtilTest.run();
        TrackerCatalogTest.run();
        ExpertiseTest.run();
        TrackerMenusTest.run();
        ProtectKillsTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
