package com.winkreyes.excelquestxp;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Run A test suite (plain JVM, no EliteMobs jar at runtime): the quest table
 * as shipped, lookup normalization, and the pure grant policy.
 * Exits non-zero on any failure. Run via run-tests.sh.
 */
public final class QuestXpTestMain {

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
        InputStream in = QuestXpTestMain.class.getResourceAsStream("/quest-xp.yml");
        if (in == null) {
            System.out.println("FAIL table-resource — quest-xp.yml not on test classpath");
            System.exit(1);
        }
        Map<String, Object> yaml = new Yaml().load(in);
        Map<String, Number> fileRows = (Map<String, Number>) yaml.get("quests");
        Map<String, Number> nameRows = (Map<String, Number>) yaml.get("names");
        Map<String, Long> byFile = new HashMap<>();
        fileRows.forEach((k, v) -> byFile.put(k, v.longValue()));
        Map<String, Long> byName = new HashMap<>();
        nameRows.forEach((k, v) -> byName.put(k, v.longValue()));
        QuestXpTable table = new QuestXpTable(byFile, byName);

        // --- Table shape (W3 §2d, re-derived from the TSV by W6) ---
        check("table-rows", table.size() == 112, "rows=" + table.size());
        check("table-total", table.totalXp() == 130355, "total=" + table.totalXp());
        long positive = byFile.values().stream().filter(x -> x > 0).count();
        check("table-paying", positive == 110, "paying=" + positive);
        check("table-zeros", table.zeroFiles().equals(
                Set.of("test_quest.yml", "xmas_quest_0.yml")),
                "zeros=" + table.zeroFiles());
        long max = byFile.values().stream().mapToLong(Long::longValue).max().orElse(-1);
        long minPositive = byFile.values().stream().mapToLong(Long::longValue)
                .filter(x -> x > 0).min().orElse(-1);
        check("table-range", max == 3630 && minPositive == 5, "max=" + max + " min+=" + minPositive);
        // 110 unambiguous name rows, minus 1 duplicate key: "Goblins in the
        // mines!" appears twice with the same XP (1920), so YAML holds one key.
        check("table-names", byName.size() == 109, "name fallbacks=" + byName.size());
        // Dotted display names survive SnakeYAML verbatim — and would BREAK
        // Bukkit's YamlConfiguration (it splits keys on '.', so a trailing
        // "..." is an empty path segment). The plugin therefore parses
        // quest-xp.yml with SnakeYAML, not Bukkit. Regression pin:
        check("names-dotted-survive", byName.get("Strangers in need...") == 20
                && byName.get("Into the next hole...") == 320,
                "trailing-... names load with their XP");
        try {
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.StringReader("names:\n  'A...': 5\n"));
            check("bukkit-dotted-throws", false, "Bukkit loader accepted a dotted key (unexpected)");
        } catch (IllegalArgumentException e) {
            check("bukkit-dotted-throws", e.getMessage().contains("empty path"),
                    "Bukkit loader rejects dotted keys: " + e.getMessage());
        }

        // --- Spot values (K x tier^2) ---
        check("spot-main15", table.xpForFile("primis/primis_main_quest_15.yml") == 1815, "15x121");
        check("spot-main1", table.xpForFile("primis/primis_main_quest_1.yml") == 20, "5x4");
        check("spot-welcome1", table.xpForFile("ag_welcome_quest_1.yml") == 50, "50x1");
        check("spot-sewage", table.xpForFile("primis/primis_pontis_mille_serious_sewage.yml") == 3200, "50x64");
        check("spot-offmyback", table.xpForFile("primis/primis_ice_village_get_them_off_my_back.yml") == 3630, "30x121");
        check("spot-main30", table.xpForFile("primis/primis_main_quest_30.yml") == 605, "5x121");
        check("spot-test-zero", table.xpForFile("test_quest.yml") == 0, "excluded D-W3-3");
        check("spot-xmas-zero", table.xpForFile("xmas_quest_0.yml") == 0, "held D-W3-4");

        // --- Lookup normalization ---
        check("lookup-raw", table.lookup("primis/primis_main_quest_1.yml", "Shipwrecked!").xp() == 20,
                "raw path matches");
        check("lookup-basename", table.lookup("primis_main_quest_1.yml", "Shipwrecked!").xp() == 20,
                "bare filename matches via primis- prefix");
        check("lookup-backslash", table.lookup("primis\\primis_main_quest_1.yml", "?").xp() == 20,
                "backslash path matches");
        check("lookup-top-level", table.lookup("ag_welcome_quest_1.yml", "Welcome to the AG!").xp() == 50,
                "top-level file matches");
        check("lookup-name", table.lookup("unknown-quest.yml", "Shipwrecked!").xp() == 20,
                "name fallback matches");
        check("lookup-name-amp", table.lookup(null, "&2Shipwrecked!").xp() == 20,
                "& color code stripped");
        check("lookup-name-sect", table.lookup(null, "\u00a72Shipwrecked!").xp() == 20,
                "section-sign code stripped");
        check("lookup-ambiguous-name", table.lookup("no-such-file.yml", "Make them go ouch").xp() == 0,
                "ambiguous name never guessed");
        check("lookup-ambiguous-file-still-pays",
                table.lookup("primis/primis_ice_village_make_them_go_ouch.yml", "Make them go ouch").xp() == 3630
                        && table.lookup("primis/primis_ice_village_silence.yml", "Make them go ouch").xp() == 1815,
                "both files pay their own row by filename");
        QuestXpTable.Result unknown = table.lookup("no-such-file.yml", "No Such Quest");
        check("lookup-unknown", unknown.xp() == 0 && unknown.matchedBy().equals("none"),
                "unknown quest grants 0, matchedBy=none");
        check("lookup-nulls", table.lookup(null, null).xp() == 0, "null/null grants 0");

        // --- Grant policy: armor at 1/3, integer division ---
        check("armor-1815", GrantPolicy.armorXp(1815) == 605, "1815/3");
        check("armor-5", GrantPolicy.armorXp(5) == 1, "5/3 floors to 1");
        check("armor-20", GrantPolicy.armorXp(20) == 6, "20/3 floors to 6");
        check("armor-0", GrantPolicy.armorXp(0) == 0, "0/3");

        // --- Grant policy: highest weapon skill, ties -> first ---
        // 1.1.0 framing (D159): these checks now pin the HIGHEST mode —
        // the default target, and the exact function GrantPolicy
        // .resolveTargetIndex defers to when a player has no pin. The
        // pinned-mode resolution itself is covered in TargetTestMain.
        check("highest-first", GrantPolicy.highestWeaponIndex(new long[]{0, 0, 0}) == 0,
                "all zero -> first (deterministic fresh-player default)");
        check("highest-max", GrantPolicy.highestWeaponIndex(new long[]{100, 50, 0}) == 0, "max wins");
        check("highest-tie", GrantPolicy.highestWeaponIndex(new long[]{0, 300, 300}) == 1,
                "tie -> lowest index");
        check("highest-last", GrantPolicy.highestWeaponIndex(new long[]{10, 20, 999}) == 2, "last max");
        check("highest-empty", GrantPolicy.highestWeaponIndex(new long[]{}) == -1, "empty -> -1");

        // --- Claim ledger: exactly once per player per quest ---
        GrantPolicy.ClaimLedger ledger = new GrantPolicy.ClaimLedger();
        java.util.UUID alice = java.util.UUID.randomUUID();
        java.util.UUID bob = java.util.UUID.randomUUID();
        check("ledger-first", ledger.tryClaim(alice, "filename:a.yml"), "first claim ok");
        check("ledger-double", !ledger.tryClaim(alice, "filename:a.yml"), "second claim refused");
        check("ledger-other-player", ledger.tryClaim(bob, "filename:a.yml"), "other player ok");
        check("ledger-other-quest", ledger.tryClaim(alice, "filename:b.yml"), "other quest ok");
        check("ledger-claimed", ledger.isClaimed(alice, "filename:a.yml"), "isClaimed true");
        ledger.release(alice, "filename:a.yml");
        check("ledger-release", ledger.tryClaim(alice, "filename:a.yml"), "released claim retaken");

        System.out.println("QuestXpTestMain: " + pass + " passed, " + fail + " failed.");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
