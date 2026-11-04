package com.winkreyes.excelguildgate;

/**
 * Plain main-method test harness for NarratorPack (the pure constants
 * and mappings behind 0.3.2's Java narrator delivery; same style as
 * the other ExcelGuildGate harnesses: no test framework, every check
 * counted, any failure prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh.
 */
public final class NarratorPackTest {

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
        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        constants();
        promptComposition();
        statusMapping();
        sha1Parsing();
        System.out.println("NarratorPackTest: " + (checks - failures) + "/" + checks
                + " checks passed" + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void constants() {
        // The pack id is fixed forever and must never collide with the
        // Quartermaster pack's id (status events are matched by id).
        checkEq("pack id is the fixed narrator id",
                java.util.UUID.fromString("a3fd20f6-9cad-4f73-9f97-aeb77b54e6f3"),
                NarratorPack.PACK_ID);
        check("pack id differs from the Quartermaster pack id",
                !NarratorPack.PACK_ID.equals(
                        java.util.UUID.fromString("7d3f9a41-5c2e-4b8f-a1d6-3e5b7c9f2a08")));
        // The pack itself is unchanged from 0.3.0: same hosted zip the
        // server.properties prompt has pointed at since then.
        checkEq("default url is the live narrator pack",
                "https://elmikeexcel.github.io/winkreyesvalley/packs/wrv-guildgate-narrator-pack.zip",
                NarratorPack.DEFAULT_URL);
        checkEq("default sha1 is the 0.3.0 pack sha1",
                "e1ef3b835d59280e613ae435f6efca760389045e",
                NarratorPack.DEFAULT_SHA1);
        check("default prompt body is non-blank",
                NarratorPack.DEFAULT_PROMPT_BODY != null
                        && !NarratorPack.DEFAULT_PROMPT_BODY.isBlank());
    }

    /** The prompt shows the counter on every offer, with the plain
     *  end-of-asking line, in TestPlayerA's Oct 19, 2026 form. */
    private static void promptComposition() {
        String body = NarratorPack.DEFAULT_PROMPT_BODY;
        checkEq("offer 1 of 5, exact text",
                "Narrator voice pack — offer 1 of 5. " + body
                        + " After 5 offers, or once you download it, it will not ask again.",
                NarratorPack.buildPrompt(body, 1, 5));
        checkEq("offer 5 of 5, exact text",
                "Narrator voice pack — offer 5 of 5. " + body
                        + " After 5 offers, or once you download it, it will not ask again.",
                NarratorPack.buildPrompt(body, 5, 5));
        check("counter tracks a different cap",
                NarratorPack.buildPrompt(body, 2, 3).startsWith("Narrator voice pack — offer 2 of 3. ")
                        && NarratorPack.buildPrompt(body, 2, 3).contains("After 3 offers"));
        check("a custom body is used verbatim",
                NarratorPack.buildPrompt("Custom words here.", 3, 5)
                        .contains(". Custom words here. After 5 offers"));
        checkEq("offer number clamps at the cap (no offer 6 of 5 text)",
                NarratorPack.buildPrompt(body, 5, 5),
                NarratorPack.buildPrompt(body, 6, 5));
        checkEq("offer number clamps at 1",
                NarratorPack.buildPrompt(body, 1, 5),
                NarratorPack.buildPrompt(body, 0, 5));
        checkEq("null body falls back to the default body",
                NarratorPack.buildPrompt(body, 1, 5),
                NarratorPack.buildPrompt(null, 1, 5));
        checkEq("blank body falls back to the default body",
                NarratorPack.buildPrompt(body, 1, 5),
                NarratorPack.buildPrompt("   ", 1, 5));
    }

    private static void statusMapping() {
        checkEq("loaded", NarratorPack.Status.LOADED,
                NarratorPack.mapStatus("SUCCESSFULLY_LOADED"));
        checkEq("declined", NarratorPack.Status.DECLINED,
                NarratorPack.mapStatus("DECLINED"));
        checkEq("failed download", NarratorPack.Status.FAILED,
                NarratorPack.mapStatus("FAILED_DOWNLOAD"));
        checkEq("failed reload", NarratorPack.Status.FAILED,
                NarratorPack.mapStatus("FAILED_RELOAD"));
        checkEq("invalid url", NarratorPack.Status.FAILED,
                NarratorPack.mapStatus("INVALID_URL"));
        checkEq("discarded", NarratorPack.Status.FAILED,
                NarratorPack.mapStatus("DISCARDED"));
        checkEq("accepted is progress noise", NarratorPack.Status.IGNORED,
                NarratorPack.mapStatus("ACCEPTED"));
        checkEq("downloaded is progress noise", NarratorPack.Status.IGNORED,
                NarratorPack.mapStatus("DOWNLOADED"));
        checkEq("unknown name ignored", NarratorPack.Status.IGNORED,
                NarratorPack.mapStatus("SOMETHING_NEW"));
        checkEq("null ignored", NarratorPack.Status.IGNORED,
                NarratorPack.mapStatus(null));
        checkEq("empty ignored", NarratorPack.Status.IGNORED,
                NarratorPack.mapStatus(""));
    }

    private static void sha1Parsing() {
        byte[] bytes = NarratorPack.parseSha1(NarratorPack.DEFAULT_SHA1);
        check("default sha1 parses to 20 bytes", bytes != null && bytes.length == 20);
        check("first byte matches e1", bytes != null && bytes[0] == (byte) 0xe1);
        check("last byte matches 5e", bytes != null && bytes[19] == (byte) 0x5e);
        check("uppercase hex parses",
                NarratorPack.parseSha1("E1EF3B835D59280E613AE435F6EFCA760389045E") != null);
        check("surrounding whitespace is trimmed",
                NarratorPack.parseSha1("  e1ef3b835d59280e613ae435f6efca760389045e  ") != null);
        checkEq("null sha1 -> null", null, NarratorPack.parseSha1(null));
        checkEq("blank sha1 -> null", null, NarratorPack.parseSha1("   "));
        checkEq("short sha1 -> null", null, NarratorPack.parseSha1("e1ef3b83"));
        checkEq("long sha1 -> null", null,
                NarratorPack.parseSha1("e1ef3b835d59280e613ae435f6efca760389045e00"));
        checkEq("non-hex sha1 -> null", null,
                NarratorPack.parseSha1("zzef3b835d59280e613ae435f6efca760389045e"));
    }
}
