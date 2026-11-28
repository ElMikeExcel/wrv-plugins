package excelemfixes.casterxp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The EliteMobs class-form table the generalized
 * class-xp-sharing rules resolve against: every form in
 * EliteMobs' built-in Advanced Combat catalog, its parent
 * form, and the two foundation skills whose LOWER level
 * caps the form (EliteMobs' FoundationSkillPair rule).
 *
 * SOURCE OF TRUTH: runtime-probed from
 * BuiltInClassCatalog.catalog() (catalog
 * persistenceVersion=1) by the Nov 25, 2026
 * hidden-mechanics audit — see the audit's catalog-full
 * listing. The probe CORRECTED the pair table the 0.20.0
 * build lane had worked from (Berserker root is
 * AXES+MACES, Cleric root is MACES+TRIDENTS), and it
 * showed the pair CHANGES from form to form in every tree
 * except Spellcaster (STAVES+WANDS on all 15 forms) — so
 * the pair must be read from the player's selected form,
 * never assumed from the tree. If EliteMobs ships a new
 * catalog version, this table must be re-probed; a form
 * id this table does not know resolves to no pair and the
 * module stands down for that player (never intervenes on
 * data it does not understand).
 *
 * A form's TREE is its root ancestor: the ROOT-band form
 * above it (Paladin, Berserker, Ranger, Cleric,
 * Spellcaster), or Adventurer itself for the starter
 * form. Catch-ups are recorded per tree.
 */
public final class ClassFormTable {

    private ClassFormTable() {}

    /** One catalog form: id, display name, parent form id
     *  (null for the Adventurer starter), and the two
     *  foundation skills in catalog order. */
    public record Form(String id, String displayName, String parentId,
                       String skillA, String skillB) {
        /** The foundation skill that is not {@code skill},
         *  or null when {@code skill} is not in this pair. */
        public String other(String skill) {
            if (skillA.equals(skill)) return skillB;
            if (skillB.equals(skill)) return skillA;
            return null;
        }

        public boolean inPair(String skill) {
            return skillA.equals(skill) || skillB.equals(skill);
        }
    }

    // {id, displayName, parentId, skillA, skillB} — probed
    // catalog order (see the class javadoc for provenance).
    private static final String[][] ROWS = {
        {"adventurer", "Adventurer", null, "ARMOR", "SWORDS"},
        {"paladin", "Paladin", "adventurer", "ARMOR", "SWORDS"},
        {"guardian", "Guardian", "paladin", "ARMOR", "SPEARS"},
        {"aegis", "Aegis", "guardian", "ARMOR", "SWORDS"},
        {"bulwark", "Bulwark", "aegis", "ARMOR", "MACES"},
        {"shieldbearer", "Shieldbearer", "aegis", "ARMOR", "SWORDS"},
        {"justicar", "Justicar", "guardian", "ARMOR", "MACES"},
        {"templar", "Templar", "justicar", "ARMOR", "MACES"},
        {"inquisitor", "Inquisitor", "justicar", "SWORDS", "MACES"},
        {"warlord", "Warlord", "paladin", "SWORDS", "SPEARS"},
        {"marshal", "Marshal", "warlord", "ARMOR", "SPEARS"},
        {"bannerlord", "Bannerlord", "marshal", "ARMOR", "SPEARS"},
        {"strategist", "Strategist", "marshal", "SWORDS", "SPEARS"},
        {"conqueror", "Conqueror", "warlord", "SWORDS", "AXES"},
        {"tyrant", "Tyrant", "conqueror", "AXES", "ARMOR"},
        {"champion", "Champion", "conqueror", "SWORDS", "AXES"},
        {"berserker", "Berserker", "adventurer", "AXES", "MACES"},
        {"bloodrager", "Bloodrager", "berserker", "AXES", "MACES"},
        {"reaver", "Reaver", "bloodrager", "AXES", "ARMOR"},
        {"bloodstorm", "Bloodstorm", "reaver", "AXES", "HOES"},
        {"deathless", "Deathless", "reaver", "AXES", "ARMOR"},
        {"slayer", "Slayer", "bloodrager", "AXES", "SWORDS"},
        {"headsman", "Headsman", "slayer", "AXES", "SWORDS"},
        {"harvester", "Harvester", "slayer", "AXES", "HOES"},
        {"juggernaut", "Juggernaut", "berserker", "MACES", "ARMOR"},
        {"crusher", "Crusher", "juggernaut", "MACES", "AXES"},
        {"siegebreaker", "Siegebreaker", "crusher", "MACES", "AXES"},
        {"titanbane", "Titanbane", "crusher", "MACES", "SPEARS"},
        {"dreadnought", "Dreadnought", "juggernaut", "MACES", "ARMOR"},
        {"warmonger", "Warmonger", "dreadnought", "AXES", "ARMOR"},
        {"colossus", "Colossus", "dreadnought", "MACES", "ARMOR"},
        {"ranger", "Ranger", "adventurer", "BOWS", "CROSSBOWS"},
        {"sniper", "Sniper", "ranger", "BOWS", "CROSSBOWS"},
        {"bowmaster", "Bowmaster", "sniper", "BOWS", "SPEARS"},
        {"deadeye", "Deadeye", "bowmaster", "BOWS", "CROSSBOWS"},
        {"raincaller", "Raincaller", "bowmaster", "BOWS", "HOES"},
        {"arbalist", "Arbalist", "sniper", "CROSSBOWS", "ARMOR"},
        {"artillerist", "Artillerist", "arbalist", "CROSSBOWS", "MACES"},
        {"dragonslayer", "Dragonslayer", "arbalist", "CROSSBOWS", "SPEARS"},
        {"skirmisher", "Skirmisher", "ranger", "BOWS", "CROSSBOWS"},
        {"windrunner", "Windrunner", "skirmisher", "BOWS", "SPEARS"},
        {"pathfinder", "Pathfinder", "windrunner", "BOWS", "SPEARS"},
        {"tempest_archer", "Tempest Archer", "windrunner", "BOWS", "TRIDENTS"},
        {"saboteur", "Saboteur", "skirmisher", "CROSSBOWS", "HOES"},
        {"trapper", "Trapper", "saboteur", "CROSSBOWS", "HOES"},
        {"demolitionist", "Demolitionist", "saboteur", "CROSSBOWS", "MACES"},
        {"cleric", "Cleric", "adventurer", "MACES", "TRIDENTS"},
        {"priest", "Priest", "cleric", "ARMOR", "MACES"},
        {"hierophant", "Hierophant", "priest", "ARMOR", "MACES"},
        {"saint", "Saint", "hierophant", "ARMOR", "TRIDENTS"},
        {"exorcist", "Exorcist", "hierophant", "MACES", "SWORDS"},
        {"oracle", "Oracle", "priest", "TRIDENTS", "SPEARS"},
        {"fateweaver", "Fateweaver", "oracle", "TRIDENTS", "SPEARS"},
        {"seraph", "Seraph", "oracle", "ARMOR", "SPEARS"},
        {"shaman", "Shaman", "cleric", "HOES", "TRIDENTS"},
        {"lifewarden", "Lifewarden", "shaman", "HOES", "TRIDENTS"},
        {"grovekeeper", "Grovekeeper", "lifewarden", "HOES", "ARMOR"},
        {"shepherd", "Shepherd", "lifewarden", "HOES", "SPEARS"},
        {"spiritcaller", "Spiritcaller", "shaman", "HOES", "MACES"},
        {"mistweaver", "Mistweaver", "spiritcaller", "HOES", "TRIDENTS"},
        {"soulwarden", "Soulwarden", "spiritcaller", "HOES", "MACES"},
        {"spellcaster", "Spellcaster", "adventurer", "STAVES", "WANDS"},
        {"mage", "Mage", "spellcaster", "STAVES", "WANDS"},
        {"elementalist", "Elementalist", "mage", "STAVES", "WANDS"},
        {"pyromancer", "Pyromancer", "elementalist", "STAVES", "WANDS"},
        {"cryomancer", "Cryomancer", "elementalist", "STAVES", "WANDS"},
        {"battlemage", "Battlemage", "mage", "STAVES", "WANDS"},
        {"spellblade", "Spellblade", "battlemage", "STAVES", "WANDS"},
        {"arcane_knight", "Arcane Knight", "battlemage", "STAVES", "WANDS"},
        {"occultist", "Occultist", "spellcaster", "STAVES", "WANDS"},
        {"necromancer", "Necromancer", "occultist", "STAVES", "WANDS"},
        {"lich", "Lich", "necromancer", "STAVES", "WANDS"},
        {"plaguebringer", "Plaguebringer", "necromancer", "STAVES", "WANDS"},
        {"summoner", "Summoner", "occultist", "STAVES", "WANDS"},
        {"demonologist", "Demonologist", "summoner", "STAVES", "WANDS"},
        {"spiritbinder", "Spiritbinder", "summoner", "STAVES", "WANDS"},
    };

    private static final Map<String, Form> FORMS = new LinkedHashMap<>();
    static {
        for (String[] r : ROWS) {
            FORMS.put(r[0], new Form(r[0], r[1], r[2], r[3], r[4]));
        }
    }

    /** Every form id in the probed catalog (76). */
    public static Set<String> formIds() {
        return FORMS.keySet();
    }

    public static int size() {
        return FORMS.size();
    }

    /** The form, or null for an id the catalog probe did
     *  not contain. */
    public static Form form(String formId) {
        return formId == null ? null : FORMS.get(formId);
    }

    /** The form's foundation pair {skillA, skillB} in
     *  catalog order, or null for an unknown form. */
    public static String[] pairOf(String formId) {
        Form f = form(formId);
        return f == null ? null : new String[]{f.skillA(), f.skillB()};
    }

    /**
     * The tree a form belongs to: its ROOT-band ancestor's
     * id, or "adventurer" for the starter form itself.
     * Null for an unknown form (or a broken parent chain,
     * which the probed catalog does not contain).
     */
    public static String treeOf(String formId) {
        Form f = form(formId);
        if (f == null) return null;
        String cur = f.id();
        Set<String> seen = new java.util.HashSet<>();
        while (true) {
            Form c = FORMS.get(cur);
            if (c == null || !seen.add(cur)) return null;
            if (c.parentId() == null || "adventurer".equals(c.parentId())) {
                return cur;
            }
            cur = c.parentId();
        }
    }
}
