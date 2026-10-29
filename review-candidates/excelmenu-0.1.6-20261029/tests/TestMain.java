/** ExcelMenu plain-JVM suite entry point. */
public final class TestMain {
    public static void main(String[] args) {
        Check.beginSuite("TestDefinitions"); TestDefinitions.run();
        Check.beginSuite("TestSelector"); TestSelector.run();
        Check.beginSuite("TestRequirements"); TestRequirements.run();
        Check.beginSuite("TestActions"); TestActions.run();
        Check.beginSuite("TestBroken"); TestBroken.run();
        Check.beginSuite("TestText"); TestText.run();
        Check.beginSuite("TestBedrockText"); TestBedrockText.run();
        Check.beginSuite("TestFourLabel"); TestFourLabel.run();
        Check.beginSuite("TestBedrockRender"); TestBedrockRender.run();
        Check.beginSuite("TestJavaConfirm"); TestJavaConfirm.run();
        Check.printSuites();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
