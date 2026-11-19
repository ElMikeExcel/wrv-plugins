public class TestMain {
    public static void main(String[] args) throws Exception {
        RegistryTest.run();
        BuiltinItemsTest.run();
        AssignmentsTest.run();
        PluginYmlGuardTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
