// Excel signature — tests for ExcelAutoSortBridge. (Excel — MikeExcel)
package com.winkreyes.excelautosortbridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Plain-main test suite for AutoSortStatus (no framework — house style). */
public final class AutoSortStatusTest {

    private static int checks = 0;
    private static int failed = 0;

    private static void check(boolean cond, String name) {
        checks++;
        if (!cond) {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        // parseActives: the owner's file shape (one UUID per line)
        Set<UUID> s = AutoSortStatus.parseActives(List.of(a.toString(), b.toString()));
        check(s.size() == 2 && s.contains(a) && s.contains(b), "parse two uuids");

        // blank / whitespace lines skipped
        s = AutoSortStatus.parseActives(List.of("", "   ", a.toString(), " "));
        check(s.size() == 1 && s.contains(a), "blank lines skipped");

        // malformed lines skipped, good lines survive
        s = AutoSortStatus.parseActives(List.of("not-a-uuid", a.toString(), "12345"));
        check(s.size() == 1 && s.contains(a), "malformed lines skipped");

        // surrounding whitespace tolerated
        s = AutoSortStatus.parseActives(List.of("  " + a + "  "));
        check(s.contains(a), "whitespace trimmed");

        // duplicates collapse
        s = AutoSortStatus.parseActives(List.of(a.toString(), a.toString()));
        check(s.size() == 1, "duplicates collapse");

        // empty + null inputs
        check(AutoSortStatus.parseActives(List.of()).isEmpty(), "empty list -> empty set");
        check(AutoSortStatus.parseActives(null).isEmpty(), "null list -> empty set");

        // statusOf: membership is ON, non-membership is OFF (owner default)
        Set<UUID> actives = Set.of(a);
        check("ON".equals(AutoSortStatus.statusOf(actives, a)), "member -> ON");
        check("OFF".equals(AutoSortStatus.statusOf(actives, b)), "non-member -> OFF");
        check("OFF".equals(AutoSortStatus.statusOf(Set.of(), a)), "empty set -> OFF");

        // unknown must stay unknown (never guessed)
        check(AutoSortStatus.statusOf(null, a) == null, "null set -> null");
        check(AutoSortStatus.statusOf(actives, null) == null, "null player -> null");

        // readActivesFile: round-trip a real file in the owner's format
        Path dir = Files.createTempDirectory("autosortbridge-test");
        Path f = dir.resolve("active.txt");
        Files.writeString(f, a + System.lineSeparator() + b + System.lineSeparator());
        Set<UUID> read = AutoSortStatus.readActivesFile(f);
        check(read != null && read.size() == 2 && read.contains(a), "file round-trip");

        // an existing-but-empty file is a KNOWN state: everyone OFF
        Path empty = dir.resolve("empty.txt");
        Files.writeString(empty, "");
        read = AutoSortStatus.readActivesFile(empty);
        check(read != null && read.isEmpty(), "empty file -> empty set (known OFF)");

        // a MISSING file is UNKNOWN (null), not OFF
        check(AutoSortStatus.readActivesFile(dir.resolve("nope.txt")) == null,
                "missing file -> null (unknown)");

        System.out.println("AutoSortStatusTest: " + checks + " checks, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
