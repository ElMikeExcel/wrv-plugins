package com.winkreyes.excelquestxp;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * D159 target-choice suite (1.1.0): pure JVM, no EliteMobs jar needed —
 * the same discipline as GrantPolicy. Covers the four proof points:
 * (1) a pinned grant resolves to the pinned skill even when another
 * skill is higher; (2) a pin survives a save/load round-trip of
 * targets.yml (the restart path); (3) an invalid skill name is refused
 * (resolves to null — the command layer turns that into the helpful
 * refusal message); (4) the Armor 1/3 split is identical in pinned mode.
 * HIGHEST mode itself is covered by the pre-existing highest-* checks in
 * QuestXpTestMain, which exercise the exact function HIGHEST defers to.
 * Exits non-zero on any failure. Run via run-tests.sh (Runs A and B).
 */
public final class TargetTestMain {

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

    // The EliteMobs 10.9.14 weapon table (enum order pinned by EmApiTestMain
    // against the real jar; display names as EM reports them).
    private static final String[] ENUMS = {
            "SWORDS", "AXES", "BOWS", "CROSSBOWS", "TRIDENTS",
            "HOES", "MACES", "SPEARS", "STAVES", "WANDS"};
    private static final String[] DISPLAYS = {
            "Swords", "Axes", "Bows", "Crossbows", "Tridents",
            "Hoes", "Maces", "Spears", "Staves", "Wands"};
    private static final Set<String> VALID = Set.of(ENUMS);

    public static void main(String[] args) throws Exception {
        // --- (1) resolveTargetIndex: the grant-time selection SkillGrant uses ---
        long[] xp = {500, 0, 30};
        check("resolve-pinned-beats-higher", GrantPolicy.resolveTargetIndex(xp, 1) == 1,
                "pinned idx1 wins over idx0=500");
        check("resolve-pinned-first", GrantPolicy.resolveTargetIndex(xp, 0) == 0,
                "pinned idx0");
        check("resolve-highest-mode", GrantPolicy.resolveTargetIndex(xp, -1) == 0,
                "no pin -> highest (idx0=500), pre-1.1.0 rule");
        check("resolve-stale-pin-falls-back", GrantPolicy.resolveTargetIndex(xp, 99) == 0,
                "out-of-range pin -> highest");
        check("resolve-empty", GrantPolicy.resolveTargetIndex(new long[]{}, -1) == -1,
                "empty -> -1");
        check("resolve-fresh-tie", GrantPolicy.resolveTargetIndex(new long[]{0, 0, 0}, -1) == 0,
                "fresh player tie -> first skill");

        // --- (4) Armor split is target-independent ---
        boolean armorStable = true;
        for (int pin = -1; pin < 3; pin++) {
            int idx = GrantPolicy.resolveTargetIndex(xp, pin);
            armorStable &= idx >= 0 && GrantPolicy.armorXp(1815) == 605;
        }
        check("armor-split-pinned-mode", armorStable,
                "1815 -> 605 armor regardless of resolved target");

        // --- (3) typed input resolution (the command's accept/refuse surface) ---
        check("input-wand", "WANDS".equals(TargetPolicy.resolveSkill("wand", ENUMS, DISPLAYS)), "wand");
        check("input-wands-display", "WANDS".equals(TargetPolicy.resolveSkill("Wands", ENUMS, DISPLAYS)), "Wands");
        check("input-wands-enum", "WANDS".equals(TargetPolicy.resolveSkill("WANDS", ENUMS, DISPLAYS)), "WANDS");
        check("input-sword", "SWORDS".equals(TargetPolicy.resolveSkill("sword", ENUMS, DISPLAYS)), "sword");
        check("input-swords", "SWORDS".equals(TargetPolicy.resolveSkill("Swords", ENUMS, DISPLAYS)), "Swords");
        check("input-staff", "STAVES".equals(TargetPolicy.resolveSkill("staff", ENUMS, DISPLAYS)), "staff");
        check("input-stave", "STAVES".equals(TargetPolicy.resolveSkill("stave", ENUMS, DISPLAYS)), "stave");
        check("input-staves", "STAVES".equals(TargetPolicy.resolveSkill("staves", ENUMS, DISPLAYS)), "staves");
        check("input-crossbow", "CROSSBOWS".equals(TargetPolicy.resolveSkill("crossbow", ENUMS, DISPLAYS)), "crossbow");
        check("input-axe", "AXES".equals(TargetPolicy.resolveSkill("axe", ENUMS, DISPLAYS)), "axe");
        check("input-spear", "SPEARS".equals(TargetPolicy.resolveSkill("spear", ENUMS, DISPLAYS)), "spear");
        check("input-hoe", "HOES".equals(TargetPolicy.resolveSkill("hoe", ENUMS, DISPLAYS)), "hoe");
        check("input-armor-refused", TargetPolicy.resolveSkill("armor", ENUMS, DISPLAYS) == null,
                "ARMOR is not a weapon target");
        check("input-garbage-refused", TargetPolicy.resolveSkill("banana", ENUMS, DISPLAYS) == null,
                "unknown name -> null (command refuses helpfully)");
        check("input-empty-refused", TargetPolicy.resolveSkill("", ENUMS, DISPLAYS) == null, "empty -> null");
        check("input-null-refused", TargetPolicy.resolveSkill(null, ENUMS, DISPLAYS) == null, "null -> null");
        check("word-highest", TargetPolicy.isHighestWord("highest"), "highest");
        check("word-auto", TargetPolicy.isHighestWord("AUTO"), "AUTO");
        check("word-default", TargetPolicy.isHighestWord("default"), "default");
        check("word-skill-not-highest", !TargetPolicy.isHighestWord("sword"), "sword is not a mode word");

        // --- (2) TargetStore: default, persistence, clearing, corrupt entries ---
        UUID alice = UUID.fromString("7401bc22-d42a-4492-a451-374bd12d9e2c");
        UUID bob = UUID.fromString("8399c623-786f-448c-8b11-f54504721819");
        TargetStore store = new TargetStore();
        check("store-default-highest", store.pinnedSkill(alice) == null,
                "no entry = HIGHEST (pre-1.1.0 behavior)");
        store.setPin(alice, "WANDS");
        store.setPin(bob, "SWORDS");
        check("store-pin-read", "WANDS".equals(store.pinnedSkill(alice)), "pin reads back");

        File tmp = File.createTempFile("targets-test", ".yml");
        tmp.deleteOnExit();
        store.save(tmp);
        String text = Files.readString(tmp.toPath());
        check("store-file-names-skill", text.contains("WANDS") && text.contains(alice.toString()),
                "targets.yml carries uuid + enum name");
        TargetStore reloaded = TargetStore.load(tmp, VALID);
        check("store-pin-survives-reload",
                "WANDS".equals(reloaded.pinnedSkill(alice))
                        && "SWORDS".equals(reloaded.pinnedSkill(bob))
                        && reloaded.size() == 2,
                "both pins survive a save/load round-trip (the restart path)");

        reloaded.clearPin(alice);
        reloaded.save(tmp);
        TargetStore afterClear = TargetStore.load(tmp, VALID);
        check("store-clear-survives", afterClear.pinnedSkill(alice) == null
                        && "SWORDS".equals(afterClear.pinnedSkill(bob)),
                "cleared pin stays cleared after reload; other pin untouched");

        Map<String, Object> targets = new LinkedHashMap<>();
        targets.put(alice.toString(), "AXES");
        targets.put("not-a-uuid", "SWORDS");
        targets.put(bob.toString(), "BANANAS");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("targets", targets);
        TargetStore parsed = TargetStore.parse(root, VALID);
        check("store-corrupt-skipped", parsed.size() == 1 && parsed.skipped() == 2
                        && "AXES".equals(parsed.pinnedSkill(alice)),
                "bad UUID + unknown skill dropped and counted; valid entry kept");
        check("store-missing-file-empty",
                TargetStore.load(new File(tmp.getParent(), "no-such-targets.yml"), VALID).size() == 0,
                "missing targets.yml = empty store, not an error");

        System.out.println("TargetTestMain: " + pass + " passed, " + fail + " failed.");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
