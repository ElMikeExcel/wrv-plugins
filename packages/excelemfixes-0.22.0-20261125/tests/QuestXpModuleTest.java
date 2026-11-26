import excelemfixes.questxp.BackpayLedger;
import excelemfixes.questxp.BackpayTable;
import excelemfixes.questxp.GrantPolicy;
import excelemfixes.questxp.QuestXpTable;
import excelemfixes.questxp.QuestXpView;
import excelemfixes.questxp.TargetPolicy;
import excelemfixes.questxp.TargetStore;
import excelemfixes.shared.Views;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests for the quest-xp module (0.22.0, the fold of
 * ExcelEMQuestXP 1.1.0 under ruling D244). Adapted from the
 * standalone's three test mains (QuestXpTestMain 42 checks,
 * BackpayTestMain 35, TargetTestMain 34) onto the pack's
 * Check harness: the quest table (all lookup forms +
 * configured zeros), the Armor 1/3 split, highest/pinned
 * resolution, the exactly-once ledgers (session claim ledger
 * + the durable paid ledger, incl. corrupt-file refusal),
 * the backpay table + counter guard, target policy/store,
 * and the popup view model.
 */
public final class QuestXpModuleTest {

    private static final UUID A = UUID.fromString("fca3305a-80d2-352b-80d4-c98389397ea4");
    private static final UUID B = UUID.fromString("115d5965-c5c4-3efe-97e6-d8112a674d39");

    public static void run() {
        try {
            runAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runAll() throws Exception {
        tableFromBundledResource();
        tableLookupForms();
        grantPolicy();
        claimLedger();
        backpayTableParse();
        backpayLedgerRoundTrip();
        targetPolicy();
        targetStoreRoundTrip();
        viewModel();
    }

    // ------------------------------------------------------------- table

    private static QuestXpTable loadBundledTable() throws Exception {
        Map<String, Object> yaml;
        // House pattern: suites run from the project root and read src/ files.
        try (InputStream in = Files.newInputStream(Path.of("src/quest-xp.yml"))) {
            yaml = new org.yaml.snakeyaml.Yaml().load(in);
        }
        Check.ok(yaml != null, "src/quest-xp.yml parses (the bundled table source)");
        @SuppressWarnings("unchecked")
        Map<String, Object> quests = (Map<String, Object>) yaml.get("quests");
        @SuppressWarnings("unchecked")
        Map<String, Object> names = (Map<String, Object>) yaml.get("names");
        Map<String, Long> byFile = new LinkedHashMap<>();
        quests.forEach((k, v) -> byFile.put(k, ((Number) v).longValue()));
        Map<String, Long> byName = new LinkedHashMap<>();
        names.forEach((k, v) -> byName.put(k, ((Number) v).longValue()));
        return new QuestXpTable(byFile, byName);
    }

    private static void tableFromBundledResource() throws Exception {
        QuestXpTable table = loadBundledTable();
        Check.eq(112, table.size(), "bundled table carries all 112 quests");
        Check.eq(130_355L, table.totalXp(), "bundled table totals 130,355 XP");
    }

    private static void tableLookupForms() throws Exception {
        QuestXpTable table = loadBundledTable();
        Check.eq(20L, table.lookup("primis_main_quest_1.yml", null).xp(),
                "bare filename lookup pays main quest 1 = 20");
        Check.eq(20L, table.lookup("primis/primis_main_quest_1.yml", null).xp(),
                "primis/-prefixed filename lookup pays the same row");
        Check.eq(20L, table.lookup(null, "Strangers in need...").xp(),
                "display-name lookup (dotted name) pays its row = 20");
        Check.eq(0L, table.lookup("test_quest.yml", null).xp(),
                "test_quest is a configured zero");
        Check.ok(table.hasFileKey("test_quest.yml"),
                "test_quest row exists (configured zero, not missing)");
        Check.eq(0L, table.lookup("xmas_quest_0.yml", null).xp(),
                "xmas_quest_0 (repeatable) is a configured zero");
        QuestXpTable.Result unknown = table.lookup("no_such_quest.yml", "Nope");
        Check.eq(0L, unknown.xp(), "unknown quest pays 0");
        Check.eq("none", unknown.matchedBy(), "unknown quest matches nothing");
    }

    // ------------------------------------------------------ grant policy

    private static void grantPolicy() {
        Check.eq(6L, GrantPolicy.armorXp(20), "Armor share of 20 is floor(20/3) = 6");
        Check.eq(26L, GrantPolicy.armorXp(80), "Armor share of 80 is 26");
        Check.eq(160L, GrantPolicy.armorXp(480), "Armor share of 480 is 160");
        Check.eq(0L, GrantPolicy.armorXp(1), "Armor share of 1 is 0");
        Check.eq(0L, GrantPolicy.armorXp(0), "Armor share of 0 is 0");
        Check.eq(1, GrantPolicy.highestWeaponIndex(new long[]{10, 50, 30}),
                "highest resolution picks the max index");
        Check.eq(0, GrantPolicy.highestWeaponIndex(new long[]{50, 50, 10}),
                "highest resolution breaks a tie to the first skill");
        Check.eq(-1, GrantPolicy.highestWeaponIndex(new long[]{}),
                "highest resolution of an empty table is -1");
        Check.eq(0, GrantPolicy.resolveTargetIndex(new long[]{10, 50, 30}, 0),
                "a pinned skill wins even when another skill is higher (D159)");
        Check.eq(1, GrantPolicy.resolveTargetIndex(new long[]{10, 50, 30}, -1),
                "no pin defers to highest");
        Check.eq(1, GrantPolicy.resolveTargetIndex(new long[]{10, 50, 30}, 99),
                "an out-of-range pin defers to highest");
    }

    private static void claimLedger() {
        GrantPolicy.ClaimLedger ledger = new GrantPolicy.ClaimLedger();
        Check.ok(ledger.tryClaim(A, "filename:a.yml"), "first claim succeeds");
        Check.ok(!ledger.tryClaim(A, "filename:a.yml"), "second claim is refused (exactly once)");
        Check.ok(ledger.tryClaim(B, "filename:a.yml"), "claims are per-player");
        Check.ok(ledger.tryClaim(A, "filename:b.yml"), "claims are per-quest");
        ledger.release(A, "filename:a.yml");
        Check.ok(ledger.tryClaim(A, "filename:a.yml"), "a released claim can be retaken (grant-failure path)");
    }

    // ---------------------------------------------------------- backpay

    private static Map<String, Object> backpayFixture() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> players = new LinkedHashMap<>();
        players.put(A.toString(), playerEntry("RigA", 3,
                Map.of("primis_main_quest_1.yml", 20, "primis_main_quest_2.yml", 20)));
        players.put(B.toString(), playerEntry("RigB", 1,
                Map.of("ag_welcome_quest_1.yml", 50)));
        root.put("players", players);
        return root;
    }

    private static Map<String, Object> playerEntry(String name, int counter,
                                                   Map<String, Integer> quests) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("questsCompleted", counter);
        entry.put("quests", new LinkedHashMap<>(quests));
        return entry;
    }

    private static void backpayTableParse() {
        BackpayTable table = BackpayTable.parse(backpayFixture());
        Check.eq(2, table.playerCount(), "backpay fixture parses 2 players");
        Check.eq(0, table.skipped(), "backpay fixture skips nothing");
        BackpayTable.Entry a = table.forUuid(A);
        Check.ok(a != null, "backpay entry resolves by UUID");
        Check.eq("RigA", a.name(), "backpay entry carries the staged name");
        Check.eq(20L, a.quests().get("primis_main_quest_1.yml").longValue(),
                "backpay quest amount parses");
        Check.ok(table.forName("rigb") != null, "backpay entry resolves by name, case-insensitive");
        Check.ok(BackpayTable.counterMatches(3, 3), "counter guard passes on an exact match");
        Check.ok(!BackpayTable.counterMatches(4, 3), "counter guard refuses a moved counter");
        Check.ok(BackpayTable.parse(null).playerCount() == 0,
                "a null backpay document parses as an empty table");
    }

    private static void backpayLedgerRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("questxp-ledger-test");
        File paid = dir.resolve("backpay-paid.yml").toFile();
        BackpayLedger ledger = new BackpayLedger();
        Check.ok(!ledger.isPaid(A, "primis_main_quest_1.yml"), "nothing is paid in a fresh ledger");
        ledger.markPaid(A, "primis_main_quest_1.yml", 20, 6, "2026-11-20T00:00:00-06:00");
        ledger.markPaid(A, "primis_main_quest_2.yml", 20, 6, "2026-11-20T00:00:00-06:00");
        ledger.markPaid(B, "ag_welcome_quest_1.yml", 50, 16, "2026-11-20T00:00:00-06:00");
        ledger.save(paid);
        BackpayLedger reloaded = BackpayLedger.load(paid);
        Check.ok(reloaded.isPaid(A, "primis_main_quest_1.yml"),
                "a paid quest stays paid across a save/load (no double pay)");
        Check.eq(3, reloaded.paidCountTotal(), "paid ledger totals 3 quests");
        Check.eq(2, reloaded.paidCount(A), "paid ledger counts per player");
        Check.ok(!reloaded.isPaid(B, "primis_main_quest_1.yml"),
                "paid state is per player AND per quest");
        // Corrupt ledger: load must throw, never read as empty.
        File corrupt = dir.resolve("corrupt.yml").toFile();
        Files.writeString(corrupt.toPath(), "paid: [this is not: a map", StandardCharsets.UTF_8);
        boolean threw = false;
        try {
            BackpayLedger.load(corrupt);
        } catch (Exception expected) {
            threw = true;
        }
        Check.ok(threw, "a corrupt paid ledger throws (backpay refuses, never reads as empty)");
        // Missing ledger: a legitimately fresh state.
        BackpayLedger fresh = BackpayLedger.load(dir.resolve("absent.yml").toFile());
        Check.eq(0, fresh.paidCountTotal(), "a missing paid ledger loads empty (fresh state)");
    }

    // ----------------------------------------------------------- targets

    private static final String[] ENUMS = {"SWORDS", "AXES", "BOWS", "CROSSBOWS", "TRIDENTS",
            "HOES", "MACES", "SPEARS", "STAVES", "WANDS"};
    private static final String[] DISPLAYS = {"Swords", "Axes", "Bows", "Crossbows", "Tridents",
            "Hoes", "Maces", "Spears", "Staves", "Wands"};

    private static void targetPolicy() {
        Check.eq("WANDS", TargetPolicy.resolveSkill("wands", ENUMS, DISPLAYS),
                "target resolves by enum name, case-insensitive");
        Check.eq("STAVES", TargetPolicy.resolveSkill("Staff", ENUMS, DISPLAYS),
                "target resolves the staff alias to STAVES");
        Check.eq("SWORDS", TargetPolicy.resolveSkill("sword", ENUMS, DISPLAYS),
                "target resolves the singular alias");
        Check.ok(TargetPolicy.resolveSkill("armor", ENUMS, DISPLAYS) == null,
                "Armor can never be a target (it always gains its 1/3 share)");
        Check.ok(TargetPolicy.resolveSkill("fishing", ENUMS, DISPLAYS) == null,
                "an unknown skill resolves to null (refused, nothing stored)");
        Check.ok(TargetPolicy.isHighestWord("auto"), "'auto' is a highest word");
        Check.ok(TargetPolicy.isHighestWord("HIGHEST"), "'highest' is a highest word");
        Check.ok(!TargetPolicy.isHighestWord("wands"), "a skill name is not a highest word");
    }

    private static void targetStoreRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("questxp-targets-test");
        File file = dir.resolve("targets.yml").toFile();
        Set<String> valid = Set.of(ENUMS);
        TargetStore store = new TargetStore();
        Check.ok(store.pinnedSkill(A) == null, "a fresh store has no pin (HIGHEST)");
        store.setPin(A, "WANDS");
        store.setPin(B, "SWORDS");
        store.save(file);
        TargetStore reloaded = TargetStore.load(file, valid);
        Check.eq("WANDS", reloaded.pinnedSkill(A), "a pin survives save/load");
        Check.eq(2, reloaded.size(), "the store counts both pins");
        reloaded.clearPin(A);
        Check.ok(reloaded.pinnedSkill(A) == null, "clearing a pin returns the player to HIGHEST");
        // A pin naming a non-weapon skill is skipped on load.
        Files.writeString(file.toPath(),
                "targets:\n  " + A + ": FISHING\n", StandardCharsets.UTF_8);
        TargetStore guarded = TargetStore.load(file, valid);
        Check.ok(guarded.pinnedSkill(A) == null && guarded.skipped() == 1,
                "a stored pin for a non-weapon skill is skipped on load");
    }

    // -------------------------------------------------------------- view

    private static void viewModel() {
        List<QuestXpView.SkillRow> rows = List.of(
                new QuestXpView.SkillRow("SWORDS", "Swords", 3, 120),
                new QuestXpView.SkillRow("WANDS", "Wands", 5, 900));
        Views.View highest = QuestXpView.view(null, rows);
        Check.eq("Quest XP Target", highest.title(), "popup title matches 1.1.0");
        Check.eq(QuestXpView.ACTION_HIGHEST, highest.buttons().get(0).action(),
                "the popup leads with Highest (auto), as 1.1.0");
        Check.eq("questxp:set:WANDS", highest.buttons().get(2).action(),
                "skill buttons carry questxp:set:<ENUM>");
        Check.eq("core:close", highest.buttons().get(3).action(),
                "the popup ends with Close, as 1.1.0");
        Views.View pinned = QuestXpView.view("WANDS", rows);
        Check.ok(pinned.buttons().get(2).name().contains("✔"),
                "the pinned skill is marked in the popup");
        Check.eq("WANDS", QuestXpView.parseSet("questxp:set:WANDS"),
                "a set action parses back to the enum name");
        Check.ok(QuestXpView.parseSet("questxp:highest") == null,
                "the highest action is not a set action");
        Check.eq("130,355", QuestXpView.formatXp(130_355), "XP formats with separators");
    }
}
