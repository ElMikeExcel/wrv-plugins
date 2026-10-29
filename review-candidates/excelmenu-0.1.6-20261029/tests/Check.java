// Excel signature - WRV maintained source; existing authorship notices preserved.
/** Tiny check harness (house pattern: plain-JVM suites, no framework). */
public final class Check {
    public static int passed = 0;
    public static int failed = 0;
    public static String suite = "";
    private static final java.util.Map<String, Integer> perSuite = new java.util.LinkedHashMap<>();

    public static void beginSuite(String name) { suite = name; perSuite.put(name, 0); }

    public static void printSuites() {
        perSuite.forEach((k, v) -> System.out.println("SUITE " + k + ": " + v + " checks"));
    }

    public static void ok(boolean cond, String name) {
        if (cond) { passed++; perSuite.merge(suite, 1, Integer::sum); System.out.println("  PASS " + name); }
        else { failed++; System.out.println("  FAIL " + name); }
    }

    public static void eq(Object expected, Object actual, String name) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        if (!same) System.out.println("    expected=" + expected + " actual=" + actual);
        ok(same, name);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
