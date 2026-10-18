package com.winkreyes.excelquartermaster;

/**
 * Plain main-method test harness for NpcMatcher (same style as the
 * ExcelGuildGate harnesses: no test framework, every check counted, any
 * failure prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class NpcMatcherTest {

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        nameMatching();
        anchorMatching();
        fullMatch();
        System.out.println("NpcMatcherTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void nameMatching() {
        check("name: exact", NpcMatcher.nameMatches("Old Quartermaster", "Old Quartermaster"));
        check("name: case-insensitive", NpcMatcher.nameMatches("old quartermaster", "Old Quartermaster"));
        check("name: color codes stripped",
                NpcMatcher.nameMatches("§6Old Quartermaster", "Old Quartermaster"));
        check("name: multi-code + trim",
                NpcMatcher.nameMatches("  §e§lOld Quartermaster  ", "Old Quartermaster"));
        check("name: different name", !NpcMatcher.nameMatches("Old Quartermaster Jr", "Old Quartermaster"));
        check("name: plain Villager", !NpcMatcher.nameMatches("Villager", "Old Quartermaster"));
        check("name: null entity", !NpcMatcher.nameMatches(null, "Old Quartermaster"));
        check("name: null config", !NpcMatcher.nameMatches("Old Quartermaster", null));
        check("name: blank entity", !NpcMatcher.nameMatches("   ", "Old Quartermaster"));
        checkEq("normalize: strips and trims", "Old Quartermaster",
                NpcMatcher.normalizeName(" §aOld Quartermaster "));
        checkEq("normalize: null", "", NpcMatcher.normalizeName(null));
        checkEq("normalize: lone section sign kept as trimmed empty",
                "", NpcMatcher.normalizeName("§"));
    }

    private static void anchorMatching() {
        // Anchor at the quartermaster's spot: world, -783, 81, -299, r10.
        check("anchor off: anywhere matches",
                NpcMatcher.anchorMatches(false, "world", -783, 81, -299, 10, "nether", 0, 0, 0));
        check("anchor: same spot",
                NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world", -783, 81, -299));
        check("anchor: inside radius",
                NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world", -780, 82, -295));
        check("anchor: exactly on boundary counts",
                NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world", -773, 81, -299));
        check("anchor: outside radius",
                !NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world", -700, 81, -299));
        check("anchor: wrong world",
                !NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world_nether", -783, 81, -299));
        check("anchor: null entity world",
                !NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, null, -783, 81, -299));
        check("anchor: null anchor world",
                !NpcMatcher.anchorMatches(true, null, -783, 81, -299, 10, "world", -783, 81, -299));
        check("anchor: negative radius = same point only",
                NpcMatcher.anchorMatches(true, "world", 0, 64, 0, -5, "world", 0, 64, 0));
        check("anchor: negative radius rejects 1 block away",
                !NpcMatcher.anchorMatches(true, "world", 0, 64, 0, -5, "world", 1, 64, 0));
        check("anchor: vertical distance counts",
                !NpcMatcher.anchorMatches(true, "world", -783, 81, -299, 10, "world", -783, 95, -299));
    }

    private static void fullMatch() {
        check("full: name + spot",
                NpcMatcher.matches("Old Quartermaster", "Old Quartermaster",
                        true, "world", -783, 81, -299, 10, "world", -784, 81, -298));
        check("full: right name, wrong place",
                !NpcMatcher.matches("Old Quartermaster", "Old Quartermaster",
                        true, "world", -783, 81, -299, 10, "world", 0, 81, 0));
        check("full: right name, anchor off, anywhere",
                NpcMatcher.matches("Old Quartermaster", "Old Quartermaster",
                        false, "world", -783, 81, -299, 10, "world", 5000, 81, 5000));
        check("full: wrong name at the spot",
                !NpcMatcher.matches("Wandering Trader", "Old Quartermaster",
                        true, "world", -783, 81, -299, 10, "world", -783, 81, -299));
    }
}
