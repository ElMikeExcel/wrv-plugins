/** ExcelDeathRecord plain-JVM suites entry point (house pattern). */
public class TestMain {
    public static void main(String[] args) {
        RecordFormatTest.run();
        RecordNameTest.run();
        RecordIndexTest.run();
        BurnCauseTest.run();
        AttributionTest.run();
        DestructionGuardTest.run();
        BurnFormatTest.run();
        RestoreTest.run();
        LostFoundTest.run();
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
