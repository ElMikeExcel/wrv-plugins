public class TestMain {
    public static void main(String[] args) throws Exception {
        FlipGateTest.run();
        TextTest.run();
        ControlsTest.run();
        StyleTest.run();
        CompassTest.run();
        TipTest.run();
        ProgressTest.run();
        CumulusFormTest.run(System.getProperty("floodgate", ""));
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
