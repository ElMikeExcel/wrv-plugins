package com.winkreyes.excelquestxp;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Backpay test suite (plain JVM, no EliteMobs jar needed at runtime):
 * the staged backpay table as shipped, the persisted exactly-once ledger
 * (double-run + restart-reload), the counter guard, and the zero-XP
 * exclusions. Exits non-zero on any failure. Run via run-tests.sh.
 */
public final class BackpayTestMain {

    private static int pass;
    private static int fail;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("PASS " + name + " — " + detail);
        } else {
            fail++;
            System.out.println("FAIL " + name + " — " + detail);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        InputStream in = BackpayTestMain.class.getResourceAsStream("/backpay.yml");
        if (in == null) {
            System.out.println("FAIL backpay-resource — backpay.yml not on test classpath");
            System.exit(1);
        }
        Map<String, Object> yaml = new Yaml().load(in);
        BackpayTable table = BackpayTable.parse(yaml);

        // --- Staged table shape (BACKPAY-RECORD.md §3) ---
        check("backpay-players", table.playerCount() == 2, "players=" + table.playerCount());
        check("backpay-skipped-none", table.skipped() == 0, "skipped=" + table.skipped());
        UUID jesterUuid = UUID.fromString("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e");
        UUID marvinUuid = UUID.fromString("8399c623-786f-448c-8b11-f54504721819");
        BackpayTable.Entry jester = table.forUuid(jesterUuid);
        BackpayTable.Entry marvin = table.forUuid(marvinUuid);
        check("backpay-jester-present", jester != null, "Jester_Turtle entry present");
        check("backpay-marvin-present", marvin != null, "GumGumJetPistol entry present");
        check("backpay-jester-counter", jester != null && jester.questsCompleted() == 33,
                "Jester counter=" + (jester == null ? "?" : jester.questsCompleted()));
        check("backpay-marvin-counter", marvin != null && marvin.questsCompleted() == 15,
                "Marvin counter=" + (marvin == null ? "?" : marvin.questsCompleted()));
        check("backpay-jester-quests", jester != null && jester.quests().size() == 33,
                "Jester quests=" + (jester == null ? "?" : jester.quests().size()));
        check("backpay-marvin-quests", marvin != null && marvin.quests().size() == 13,
                "Marvin quests=" + (marvin == null ? "?" : marvin.quests().size()));
        check("backpay-jester-weapon", jester != null && jester.weaponTotal() == 22060,
                "Jester weapon=" + (jester == null ? "?" : jester.weaponTotal()));
        check("backpay-jester-armor", jester != null && jester.armorTotal() == 7344,
                "Jester armor=" + (jester == null ? "?" : jester.armorTotal()));
        check("backpay-marvin-weapon", marvin != null && marvin.weaponTotal() == 1825,
                "Marvin weapon=" + (marvin == null ? "?" : marvin.weaponTotal()));
        check("backpay-marvin-armor", marvin != null && marvin.armorTotal() == 603,
                "Marvin armor=" + (marvin == null ? "?" : marvin.armorTotal()));
        // Per-quest armor floors (spot values from the staged table).
        check("backpay-armor-spots",
                jester != null
                        && GrantPolicy.armorXp(jester.xpForFile("primis_main_quest_14.yml")) == 1000
                        && GrantPolicy.armorXp(jester.xpForFile("primis_main_quest_8.yml")) == 245
                        && marvin != null
                        && GrantPolicy.armorXp(marvin.xpForFile("primis_ogre_mines_lost_bretheren.yml")) == 41
                        && GrantPolicy.armorXp(marvin.xpForFile("primis_main_quest_1.yml")) == 6,
                "3000->1000, 735->245, 125->41, 20->6");

        // --- Name resolution (command input) ---
        check("backpay-name-jester", table.forName("Jester_Turtle") == jester,
                "exact name resolves");
        check("backpay-name-case", table.forName("jester_turtle") == jester,
                "name lookup is case-insensitive");
        check("backpay-name-marvin", table.forName("GumGumJetPistol") == marvin,
                "Marvin name resolves");
        check("backpay-name-unknown", table.forName("Nobody_Here") == null,
                "unknown name resolves to nothing");

        // --- Zero-XP quests grant 0: absent from the backpay table ---
        check("backpay-test-absent",
                (jester == null || jester.xpForFile("test_quest.yml") == 0)
                        && (marvin == null || marvin.xpForFile("test_quest.yml") == 0),
                "test_quest pays 0 (absent)");
        check("backpay-xmas-absent",
                (jester == null || jester.xpForFile("xmas_quest_0.yml") == 0)
                        && (marvin == null || marvin.xpForFile("xmas_quest_0.yml") == 0),
                "xmas_quest_0 pays 0 (absent)");

        // --- Backpay amounts agree with the quest-XP table (cross-check) ---
        InputStream qin = BackpayTestMain.class.getResourceAsStream("/quest-xp.yml");
        Map<String, Object> qyaml = new Yaml().load(qin);
        Map<String, Number> fileRows = (Map<String, Number>) qyaml.get("quests");
        Map<String, Long> byFile = new HashMap<>();
        fileRows.forEach((k, v) -> byFile.put(k, v.longValue()));
        boolean agree = true;
        StringBuilder disagree = new StringBuilder();
        for (BackpayTable.Entry e : new BackpayTable.Entry[]{jester, marvin}) {
            if (e == null) {
                agree = false;
                continue;
            }
            for (Map.Entry<String, Long> q : e.quests().entrySet()) {
                Long fromTable = byFile.get(q.getKey());
                if (fromTable == null) {
                    fromTable = byFile.get("primis/" + q.getKey());
                }
                if (fromTable == null || fromTable.longValue() != q.getValue()) {
                    agree = false;
                    disagree.append(e.name()).append("/").append(q.getKey()).append(" ");
                }
            }
        }
        check("backpay-agrees-with-table", agree,
                agree ? "all 46 backpay rows match quest-xp.yml" : "mismatch: " + disagree);

        // --- Counter guard (pure decision) ---
        check("guard-match", BackpayTable.counterMatches(33, 33), "33 == 33 pays");
        check("guard-moved", !BackpayTable.counterMatches(34, 33), "34 != 33 refuses");
        check("guard-moved-down", !BackpayTable.counterMatches(32, 33), "32 != 33 refuses");
        check("guard-marvin-gap", BackpayTable.counterMatches(15, 15),
                "Marvin 15 == 15 (13 ledger + 2 dynamic) pays");

        // --- Persisted exactly-once ledger: double-run + restart-reload ---
        File tmpDir = Files.createTempDirectory("backpay-ledger-test").toFile();
        File paidFile = new File(tmpDir, "backpay-paid.yml");
        BackpayLedger ledger = new BackpayLedger();
        check("ledger-empty-start", ledger.paidCountTotal() == 0, "fresh ledger is empty");
        // Simulate run 1: pay every Jester quest, write-through per quest.
        int run1 = 0;
        for (Map.Entry<String, Long> q : jester.quests().entrySet()) {
            if (!ledger.isPaid(jesterUuid, q.getKey())) {
                ledger.markPaid(jesterUuid, q.getKey(), q.getValue(),
                        GrantPolicy.armorXp(q.getValue()), "2026-11-19T21:00:00-06:00");
                ledger.save(paidFile);
                run1++;
            }
        }
        check("ledger-run1-pays-33", run1 == 33 && ledger.paidCount(jesterUuid) == 33,
                "run 1 paid " + run1 + " quests");
        check("ledger-file-exists", paidFile.isFile() && paidFile.length() > 0,
                "backpay-paid.yml written, " + paidFile.length() + " B");
        // Simulate run 2 (same session): nothing new pays.
        int run2 = 0;
        for (Map.Entry<String, Long> q : jester.quests().entrySet()) {
            if (!ledger.isPaid(jesterUuid, q.getKey())) {
                run2++;
            }
        }
        check("ledger-run2-zero", run2 == 0, "run 2 would pay " + run2 + " quests");
        // Simulate RESTART: fresh ledger object loaded from the same file.
        BackpayLedger reloaded = BackpayLedger.load(paidFile);
        int run3 = 0;
        for (Map.Entry<String, Long> q : jester.quests().entrySet()) {
            if (!reloaded.isPaid(jesterUuid, q.getKey())) {
                run3++;
            }
        }
        check("ledger-restart-zero", run3 == 0 && reloaded.paidCount(jesterUuid) == 33,
                "after reload, run 3 would pay " + run3 + " quests");
        check("ledger-payment-amounts",
                reloaded.payment(jesterUuid, "primis_main_quest_14.yml") != null
                        && reloaded.payment(jesterUuid, "primis_main_quest_14.yml").xp() == 3000
                        && reloaded.payment(jesterUuid, "primis_main_quest_14.yml").armor() == 1000,
                "main_quest_14 recorded as 3000/1000");
        // Marvin untouched by Jester's run; one Marvin quest pays, then persists.
        check("ledger-marvin-untouched", reloaded.paidCount(marvinUuid) == 0,
                "Marvin still 0 paid after Jester's run");
        reloaded.markPaid(marvinUuid, "primis_main_quest_3.yml", 480, 160,
                "2026-11-19T21:00:00-06:00");
        reloaded.save(paidFile);
        BackpayLedger reloaded2 = BackpayLedger.load(paidFile);
        check("ledger-marvin-persists",
                reloaded2.isPaid(marvinUuid, "primis_main_quest_3.yml")
                        && !reloaded2.isPaid(marvinUuid, "primis_main_quest_4.yml")
                        && reloaded2.paidCount(jesterUuid) == 33,
                "Marvin main3 paid, main4 unpaid, Jester intact after reload");
        // Missing file loads empty (fresh server); corrupt file throws (never "empty").
        check("ledger-missing-empty",
                BackpayLedger.load(new File(tmpDir, "no-such-file.yml")).paidCountTotal() == 0,
                "missing paid file loads as empty");
        File corrupt = new File(tmpDir, "corrupt.yml");
        Files.writeString(corrupt.toPath(), "players:\n  - [\n");
        boolean threw = false;
        try {
            BackpayLedger.load(corrupt);
        } catch (java.io.IOException expected) {
            threw = true;
        }
        check("ledger-corrupt-throws", threw, "corrupt paid file throws, never reads as empty");
        // Malformed table entries are skipped, not fatal.
        Map<String, Object> badYaml = new Yaml().load(
                "players:\n  not-a-uuid:\n    name: Bad\n    questsCompleted: 1\n    quests:\n      a.yml: 5\n");
        check("table-bad-entry-skipped", BackpayTable.parse(badYaml).playerCount() == 0
                        && BackpayTable.parse(badYaml).skipped() == 1,
                "bad UUID entry skipped");

        System.out.println("BackpayTestMain: " + pass + " passed, " + fail + " failed.");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
