package com.winkreyes.excelquartermaster;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plain main-method test harness for TaleStore (same style as the
 * ExcelGuildGate harnesses: no test framework, every check counted, any
 * failure prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class TaleStoreTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

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

    public static void main(String[] args) throws Exception {
        roundTrip();
        emptyMapSerializesEmpty();
        parseSkipsMalformedLines();
        parseClampsNegatives();
        serializeSortsByUuid();
        fileRoundTripAndMissingFile();
        System.out.println("TaleStoreTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void roundTrip() {
        Map<UUID, TaleRotation.PlayerTale> in = new LinkedHashMap<>();
        in.put(A, new TaleRotation.PlayerTale(3, 123456789L));
        in.put(B, new TaleRotation.PlayerTale(0, 0L));
        Map<UUID, TaleRotation.PlayerTale> out = TaleStore.parse(TaleStore.serialize(in));
        checkEq("roundtrip: A", in.get(A), out.get(A));
        checkEq("roundtrip: B", in.get(B), out.get(B));
        checkEq("roundtrip: size", 2, out.size());
    }

    private static void emptyMapSerializesEmpty() {
        checkEq("empty: serialize", "", TaleStore.serialize(Map.of()));
        checkEq("null: serialize", "", TaleStore.serialize(null));
        check("empty text: parse empty", TaleStore.parse("").isEmpty());
        check("null text: parse empty", TaleStore.parse(null).isEmpty());
        check("blank text: parse empty", TaleStore.parse("  \n \n").isEmpty());
    }

    private static void parseSkipsMalformedLines() {
        String text = "# a comment line\n"
                + "\n"
                + "not-a-uuid 1 2\n"
                + A + " two 2\n"
                + A + " 1\n"
                + A + " 1 2 3\n"
                + B + " 4 500\n";
        Map<UUID, TaleRotation.PlayerTale> out = TaleStore.parse(text);
        checkEq("malformed: only the good line survives", 1, out.size());
        checkEq("malformed: good line intact", new TaleRotation.PlayerTale(4, 500L), out.get(B));
    }

    private static void parseClampsNegatives() {
        Map<UUID, TaleRotation.PlayerTale> out = TaleStore.parse(A + " -3 -50\n");
        checkEq("clamp: negatives to zero", new TaleRotation.PlayerTale(0, 0L), out.get(A));
        Map<UUID, TaleRotation.PlayerTale> ser = Map.of(A, new TaleRotation.PlayerTale(-1, -1L));
        checkEq("clamp: serialize too", A + " 0 0\n", TaleStore.serialize(ser));
    }

    private static void serializeSortsByUuid() {
        Map<UUID, TaleRotation.PlayerTale> in = new LinkedHashMap<>();
        in.put(B, new TaleRotation.PlayerTale(1, 10L));
        in.put(A, new TaleRotation.PlayerTale(2, 20L));
        checkEq("sorted output", A + " 2 20\n" + B + " 1 10\n", TaleStore.serialize(in));
    }

    private static void fileRoundTripAndMissingFile() throws Exception {
        Path dir = Files.createTempDirectory("talestore-test");
        Path file = dir.resolve("nested").resolve("player-tales.txt");
        Map<UUID, TaleRotation.PlayerTale> in = Map.of(A, new TaleRotation.PlayerTale(5, 42L));
        TaleStore.save(file, in);
        check("file: created with parents", Files.exists(file));
        checkEq("file: roundtrip", in.get(A), TaleStore.load(file).get(A));
        check("file: missing reads empty",
                TaleStore.load(dir.resolve("does-not-exist.txt")).isEmpty());
    }
}
