public class TestMain {
    public static void main(String[] args) {
        FoodTableTest.run();
        HealEngineTest.run();
        WorldGateTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
