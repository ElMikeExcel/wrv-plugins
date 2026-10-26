import java.nio.file.Path;

public class TestMain {
    public static void main(String[] args) throws Exception {
        Path stages = Path.of(System.getProperty("stages",
                "/home/hatch/workspace/excelmagicgate-0.1.0/src/ExcelMagicGate/stages"));
        Path table = Path.of(System.getProperty("table", "spell-classes.txt"));
        Path signals = Path.of(System.getProperty("signals", "tests/spell-signals.txt"));
        StanceStateTest.run();
        WeaponsTest.run();
        RangedMartialTest.run();
        LockTest.run();
        WardTest.run();
        ArenaLockTest.run();
        HotbarSkipTest.run();
        SpellClassifierTest.run(stages, table, signals);
        System.out.println("TOTAL: " + Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }
}
