public class TestMain {
    public static void main(String[] args) throws Exception {
        FlipGateTest.run();
        TextTest.run();
        StyleTest.run();
        CompassTest.run();
        CumulusFormTest.run(System.getProperty("floodgate", ""));
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
