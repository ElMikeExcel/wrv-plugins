public class TestMain {
    public static void main(String[] args) throws Exception {
        SkullMathTest.run();
        TopUpDeciderTest.run();
        SkullSettingsTest.run();
        PluginYmlGuardTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
