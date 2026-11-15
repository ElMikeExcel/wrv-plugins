/** Tiny check harness (house pattern: plain-JVM suites, no framework). */
public final class Check {
    public static int passed = 0;
    public static int failed = 0;

    public static void ok(boolean cond, String name) {
        if (cond) { passed++; System.out.println("  PASS " + name); }
        else { failed++; System.out.println("  FAIL " + name); }
    }

    public static void eq(Object expected, Object actual, String name) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        if (!same) System.out.println("    expected=" + expected + " actual=" + actual);
        ok(same, name);
    }
}
