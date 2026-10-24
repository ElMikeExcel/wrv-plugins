/** ExcelCosmetics plain-JVM suite entry point. */
public final class TestMain {
    public static void main(String[] args) {
        Check.beginSuite("TestCatalog"); TestCatalog.run();
        Check.beginSuite("TestGrants"); TestGrants.run();
        Check.beginSuite("TestFormat"); TestFormat.run();
        Check.beginSuite("TestPurchase"); TestPurchase.run();
        Check.printSuites();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
