public class TestMain {
    public static void main(String[] args) {
        PouchCompassCheckTest.run();
        PluginYmlGuardTest.run();
        CompassTapTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
