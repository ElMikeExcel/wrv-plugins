import excelemfixes.casterxp.ClassFormTable;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassFormTable: the probed EliteMobs catalog (76 forms)
 * resolves — every form id maps to the foundation pair
 * and class tree the Nov 25, 2026 hidden-mechanics audit
 * probed from BuiltInClassCatalog (persistenceVersion=1).
 * This is the regression net for the audit's F2
 * corrections: Berserker's root pair is AXES+MACES (not
 * SWORDS+MACES), Cleric's root pair is MACES+TRIDENTS
 * (HOES+TRIDENTS is the Shaman branch), and pairs change
 * per form in every tree except Spellcaster. Unknown ids
 * resolve to nothing — the module stands down for forms
 * it does not know.
 */
public final class ClassFormTableTest {

    // {formId, skillA, skillB, tree} — the audit's probed
    // catalog, all 76 forms.
    private static final String[][] EXPECTED = {
            {"adventurer", "ARMOR", "SWORDS", "adventurer"},
            {"paladin", "ARMOR", "SWORDS", "paladin"},
            {"guardian", "ARMOR", "SPEARS", "paladin"},
            {"aegis", "ARMOR", "SWORDS", "paladin"},
            {"bulwark", "ARMOR", "MACES", "paladin"},
            {"shieldbearer", "ARMOR", "SWORDS", "paladin"},
            {"justicar", "ARMOR", "MACES", "paladin"},
            {"templar", "ARMOR", "MACES", "paladin"},
            {"inquisitor", "SWORDS", "MACES", "paladin"},
            {"warlord", "SWORDS", "SPEARS", "paladin"},
            {"marshal", "ARMOR", "SPEARS", "paladin"},
            {"bannerlord", "ARMOR", "SPEARS", "paladin"},
            {"strategist", "SWORDS", "SPEARS", "paladin"},
            {"conqueror", "SWORDS", "AXES", "paladin"},
            {"tyrant", "AXES", "ARMOR", "paladin"},
            {"champion", "SWORDS", "AXES", "paladin"},
            {"berserker", "AXES", "MACES", "berserker"},
            {"bloodrager", "AXES", "MACES", "berserker"},
            {"reaver", "AXES", "ARMOR", "berserker"},
            {"bloodstorm", "AXES", "HOES", "berserker"},
            {"deathless", "AXES", "ARMOR", "berserker"},
            {"slayer", "AXES", "SWORDS", "berserker"},
            {"headsman", "AXES", "SWORDS", "berserker"},
            {"harvester", "AXES", "HOES", "berserker"},
            {"juggernaut", "MACES", "ARMOR", "berserker"},
            {"crusher", "MACES", "AXES", "berserker"},
            {"siegebreaker", "MACES", "AXES", "berserker"},
            {"titanbane", "MACES", "SPEARS", "berserker"},
            {"dreadnought", "MACES", "ARMOR", "berserker"},
            {"warmonger", "AXES", "ARMOR", "berserker"},
            {"colossus", "MACES", "ARMOR", "berserker"},
            {"ranger", "BOWS", "CROSSBOWS", "ranger"},
            {"sniper", "BOWS", "CROSSBOWS", "ranger"},
            {"bowmaster", "BOWS", "SPEARS", "ranger"},
            {"deadeye", "BOWS", "CROSSBOWS", "ranger"},
            {"raincaller", "BOWS", "HOES", "ranger"},
            {"arbalist", "CROSSBOWS", "ARMOR", "ranger"},
            {"artillerist", "CROSSBOWS", "MACES", "ranger"},
            {"dragonslayer", "CROSSBOWS", "SPEARS", "ranger"},
            {"skirmisher", "BOWS", "CROSSBOWS", "ranger"},
            {"windrunner", "BOWS", "SPEARS", "ranger"},
            {"pathfinder", "BOWS", "SPEARS", "ranger"},
            {"tempest_archer", "BOWS", "TRIDENTS", "ranger"},
            {"saboteur", "CROSSBOWS", "HOES", "ranger"},
            {"trapper", "CROSSBOWS", "HOES", "ranger"},
            {"demolitionist", "CROSSBOWS", "MACES", "ranger"},
            {"cleric", "MACES", "TRIDENTS", "cleric"},
            {"priest", "ARMOR", "MACES", "cleric"},
            {"hierophant", "ARMOR", "MACES", "cleric"},
            {"saint", "ARMOR", "TRIDENTS", "cleric"},
            {"exorcist", "MACES", "SWORDS", "cleric"},
            {"oracle", "TRIDENTS", "SPEARS", "cleric"},
            {"fateweaver", "TRIDENTS", "SPEARS", "cleric"},
            {"seraph", "ARMOR", "SPEARS", "cleric"},
            {"shaman", "HOES", "TRIDENTS", "cleric"},
            {"lifewarden", "HOES", "TRIDENTS", "cleric"},
            {"grovekeeper", "HOES", "ARMOR", "cleric"},
            {"shepherd", "HOES", "SPEARS", "cleric"},
            {"spiritcaller", "HOES", "MACES", "cleric"},
            {"mistweaver", "HOES", "TRIDENTS", "cleric"},
            {"soulwarden", "HOES", "MACES", "cleric"},
            {"spellcaster", "STAVES", "WANDS", "spellcaster"},
            {"mage", "STAVES", "WANDS", "spellcaster"},
            {"elementalist", "STAVES", "WANDS", "spellcaster"},
            {"pyromancer", "STAVES", "WANDS", "spellcaster"},
            {"cryomancer", "STAVES", "WANDS", "spellcaster"},
            {"battlemage", "STAVES", "WANDS", "spellcaster"},
            {"spellblade", "STAVES", "WANDS", "spellcaster"},
            {"arcane_knight", "STAVES", "WANDS", "spellcaster"},
            {"occultist", "STAVES", "WANDS", "spellcaster"},
            {"necromancer", "STAVES", "WANDS", "spellcaster"},
            {"lich", "STAVES", "WANDS", "spellcaster"},
            {"plaguebringer", "STAVES", "WANDS", "spellcaster"},
            {"summoner", "STAVES", "WANDS", "spellcaster"},
            {"demonologist", "STAVES", "WANDS", "spellcaster"},
            {"spiritbinder", "STAVES", "WANDS", "spellcaster"},
    };

    public static void run() {
        System.out.println("ClassFormTableTest");

        Check.eq(76, ClassFormTable.size(),
                "table: 76 forms probed");
        Check.eq(76, EXPECTED.length,
                "table: expectation covers all 76 forms");

        Set<String> trees = new HashSet<>();
        for (String[] row : EXPECTED) {
            String id = row[0];
            String[] pair = ClassFormTable.pairOf(id);
            Check.ok(pair != null, "table: pair resolves for " + id);
            if (pair == null) continue;
            Check.eq(row[1], pair[0], "table: " + id + " skill A");
            Check.eq(row[2], pair[1], "table: " + id + " skill B");
            Check.eq(row[3], ClassFormTable.treeOf(id),
                    "table: " + id + " tree");
            trees.add(row[3]);
            ClassFormTable.Form form = ClassFormTable.form(id);
            Check.ok(form != null && form.inPair(row[1])
                            && form.inPair(row[2]),
                    "table: " + id + " form carries its pair");
            Check.eq(row[2], form.other(row[1]),
                    "table: " + id + " other of A is B");
            Check.eq(null, form.other("NOPE"),
                    "table: " + id + " other of a foreign skill is null");
        }
        Check.eq(Set.of("adventurer", "paladin", "berserker", "ranger",
                        "cleric", "spellcaster"), trees,
                "table: exactly the six class trees");

        // The audit's F2 corrections, named outright.
        Check.eq("AXES", ClassFormTable.pairOf("berserker")[0],
                "F2: berserker root pair starts AXES");
        Check.eq("MACES", ClassFormTable.pairOf("berserker")[1],
                "F2: berserker root pair ends MACES");
        Check.eq("MACES", ClassFormTable.pairOf("cleric")[0],
                "F2: cleric root pair starts MACES");
        Check.eq("TRIDENTS", ClassFormTable.pairOf("cleric")[1],
                "F2: cleric root pair ends TRIDENTS");
        Check.eq("HOES", ClassFormTable.pairOf("shaman")[0],
                "F2: HOES+TRIDENTS belongs to shaman");
        Check.eq("ARMOR", ClassFormTable.pairOf("paladin")[0],
                "F2: paladin root pair starts ARMOR");
        Check.eq("SWORDS", ClassFormTable.pairOf("paladin")[1],
                "F2: paladin root pair ends SWORDS");

        // Pair changes per form inside a tree (spellcaster
        // is the only constant-pair tree).
        Check.eq("SPEARS", ClassFormTable.pairOf("guardian")[1],
                "table: guardian pair is ARMOR+SPEARS, not the root pair");
        Check.eq("MACES", ClassFormTable.pairOf("inquisitor")[1],
                "table: inquisitor pair is SWORDS+MACES");
        Check.eq("ARMOR", ClassFormTable.pairOf("tyrant")[1],
                "table: tyrant pair is AXES+ARMOR");
        Check.eq("TRIDENTS", ClassFormTable.pairOf("tempest_archer")[1],
                "table: tempest archer pair is BOWS+TRIDENTS");
        Check.eq("WANDS", ClassFormTable.pairOf("lich")[1],
                "table: spellcaster pair constant deep in the tree");
        Check.eq("spellcaster", ClassFormTable.treeOf("lich"),
                "table: lich belongs to the spellcaster tree");
        Check.eq("adventurer", ClassFormTable.treeOf("adventurer"),
                "table: the starter form is its own tree");

        // Unknown forms stand down.
        Check.eq(null, ClassFormTable.pairOf("not-a-form"),
                "table: unknown form has no pair");
        Check.eq(null, ClassFormTable.treeOf("not-a-form"),
                "table: unknown form has no tree");
        Check.eq(null, ClassFormTable.pairOf(null),
                "table: null form has no pair");
        Check.eq(null, ClassFormTable.treeOf(null),
                "table: null form has no tree");
    }
}
