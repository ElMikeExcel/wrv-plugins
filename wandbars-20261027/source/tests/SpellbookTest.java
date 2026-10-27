import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.SpellbookGroups;
import com.winkreyes.excelwandbars.core.SpellbookText;
import com.winkreyes.excelwandbars.core.SpellbookText.Entry;
import com.winkreyes.excelwandbars.core.SpellbookText.GroupCount;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 0.3.0 organized Spell Book: groups file, counts per seal, sorting, paging, text limits. */
public final class SpellbookTest {
    static final String[] RANKS = {"beginner", "student", "apprentice", "master"};
    static final String[] BANDS = {"Beginner", "Student", "Apprentice", "Master"};

    record Row(String key, String name, int band, String cat, int seal) {}

    @SuppressWarnings("unchecked")
    static SpellbookGroups load(String path) throws Exception {
        Object o = new org.yaml.snakeyaml.Yaml().load(Files.readString(Path.of(path)));
        return SpellbookGroups.parse((Map<String, Object>) o);
    }

    @SuppressWarnings("unchecked")
    static SpellbookGroups parseText(String yml) {
        return SpellbookGroups.parse((Map<String, Object>) new org.yaml.snakeyaml.Yaml().load(yml));
    }

    static List<Row> rows(String csv) throws Exception {
        List<Row> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(csv));
        for (String l : lines.subList(1, lines.size())) {
            if (l.isBlank()) continue;
            String[] c = l.split(",", -1);
            out.add(new Row(c[0], c[1], Arrays.asList(RANKS).indexOf(c[2]), c[6], Integer.parseInt(c[3])));
        }
        return out;
    }

    /** A wand at rank r knowing `known`; locked = spells of higher bands in `pool`. */
    static List<Entry> book(List<Row> pool, int rank, boolean withLocked) {
        List<Entry> out = new ArrayList<>();
        int slot = 0;
        for (Row r : pool) {
            if (r.band <= rank) {
                Entry e = new Entry(r.key, r.name, r.band, BANDS[r.band], false).rank("Rank 1 of 3");
                if (slot < 21) e.barSlot = slot++;
                out.add(e);
            } else if (withLocked) out.add(new Entry(r.key, r.name, r.band, BANDS[r.band], true));
        }
        return out;
    }

    static Map<String, Integer> countMap(List<GroupCount> gc, boolean total) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (GroupCount c : gc) m.put(c.key, total ? c.total() : c.known);
        return m;
    }

    static final List<String> bedrockStrings = new ArrayList<>();
    static void br(String s) { bedrockStrings.add(s); }

    public static void run(String ymlPath, String csvPath) throws Exception {
        System.out.println("SpellbookTest (0.3.0 organized Spell Book)");
        if (ymlPath.isEmpty()) ymlPath = "spellbook.yml";
        if (csvPath.isEmpty()) csvPath = "tests/data/spells-by-seal.csv";

        // ---- shipped spellbook.yml
        SpellbookGroups g = load(ymlPath);
        List<String> keys = new ArrayList<>(), names = new ArrayList<>();
        for (SpellbookGroups.Group x : g.groups()) { keys.add(x.key); names.add(x.name); }
        Check.eq(List.of("healing", "defense", "damage", "movement", "summon", "building", "world", "utility"), keys, "yml: 8 groups in order");
        Check.eq(List.of("Healing", "Defense", "Damage", "Movement", "Summon", "Building", "World", "Utility"), names, "yml: group names");
        Check.eq(104, g.spellCount(), "yml: 104 spells listed");
        Check.eq(0, g.duplicates().size(), "yml: no duplicates");
        Check.eq(0, g.problems().size(), "yml: no problems");
        Check.eq("other", g.groupOf("disposal"), "yml: disposal is not listed (taken off the wands)");
        Check.eq("Other", g.otherName(), "yml: other-name");
        Check.eq("other", g.order().get(g.order().size() - 1), "order: Other last");
        Check.eq("healing", g.groupOf("heal"), "groupOf heal");
        Check.eq("healing", g.groupOf("Heal|2"), "groupOf strips level and case");
        Check.eq("healing", g.groupOf("healing"), "spell 'healing' is in group healing");
        Check.eq("damage", g.groupOf("stormshackle"), "groupOf stormshackle");
        Check.eq("summon", g.groupOf("tame"), "groupOf tame");
        Check.eq("world", g.find("World"), "find by name");
        Check.eq("movement", g.find("MOVEMENT"), "find any case");
        Check.eq(null, g.find("nope"), "find unknown = null");

        // ---- every CSV spell (except disposal) is in the right group
        List<Row> all = rows(csvPath);
        List<Row> pool = new ArrayList<>();
        int wrong = 0;
        for (Row r : all) {
            if (r.key.equals("disposal")) continue;
            pool.add(r);
            if (!g.groupOf(r.key).equals(r.cat.toLowerCase(Locale.ROOT))) { wrong++; System.out.println("    group mismatch " + r.key); }
        }
        Check.eq(104, pool.size(), "inventory: 104 spells without disposal");
        Check.eq(0, wrong, "inventory: every spell in its planned group");
        List<String> poolKeys = new ArrayList<>();
        for (Row r : pool) poolKeys.add(r.key);
        Check.eq(0, g.unlisted(poolKeys).size(), "inventory: nothing unlisted");

        // ---- Seal 1 (17 spells), Master wand
        List<Row> seal1 = new ArrayList<>();
        for (Row r : pool) if (r.seal == 1) seal1.add(r);
        Check.eq(17, seal1.size(), "seal 1: 17 spells");
        List<Entry> s1 = book(seal1, 3, true);
        List<GroupCount> gc1 = SpellbookText.counts(g, s1, true);
        Check.eq(Map.of("healing", 7, "defense", 4, "damage", 1, "movement", 2, "utility", 3), new HashMap<>(countMap(gc1, false)), "seal 1 master: 7/4/1/2/3");
        Check.eq(List.of("healing", "defense", "damage", "movement", "utility"), new ArrayList<>(countMap(gc1, false).keySet()), "seal 1: empty groups hidden, file order");
        Check.eq(0, SpellbookText.locked(s1), "seal 1 master: nothing locked");
        Check.eq("Master Wand: 17 spells", SpellbookText.wandLine("Master Wand", 17), "wandLine");
        Check.eq("17 on your bar, 0 in the book.", SpellbookText.barLine(17, 17), "barLine");
        Check.eq(null, SpellbookText.lockedLine(0), "lockedLine none");

        // Seal 1, Beginner wand: locked = the higher-rank seal 1 spells only (no spoilers)
        List<Entry> s1b = book(seal1, 0, true);
        int b0 = 0; for (Row r : seal1) if (r.band == 0) b0++;
        Check.eq(b0, SpellbookText.known(s1b), "seal 1 beginner: known = beginner band");
        Check.eq(17 - b0, SpellbookText.locked(s1b), "seal 1 beginner: locked = rest of seal 1");
        boolean spoiler = false;
        for (Entry e : s1b) if (!poolKeysSeal(seal1).contains(e.key)) spoiler = true;
        Check.ok(!spoiler, "seal 1 beginner: no later-seal spell shown");
        List<GroupCount> gc1b = SpellbookText.counts(g, s1b, true);
        Check.eq(countMap(gc1, false), countMap(gc1b, true), "seal 1 beginner: known+locked per group = master counts");
        List<GroupCount> gc1bNo = SpellbookText.counts(g, s1b, false);
        int sumNo = 0; for (GroupCount c : gc1bNo) { sumNo += c.total(); }
        Check.eq(b0, sumNo, "show-locked off: only known spells counted");
        Check.eq((17 - b0) + " more unlock at a higher rank.", SpellbookText.lockedLine(17 - b0), "lockedLine");
        Check.eq("1 more unlocks at a higher rank.", SpellbookText.lockedLine(1), "lockedLine one");

        // ---- Simulated Seal 4 full set (104), Master wand
        List<Entry> s4 = book(pool, 3, true);
        List<GroupCount> gc4 = SpellbookText.counts(g, s4, true);
        Map<String, Integer> want4 = new LinkedHashMap<>();
        want4.put("healing", 10); want4.put("defense", 8); want4.put("damage", 30); want4.put("movement", 14);
        want4.put("summon", 5); want4.put("building", 9); want4.put("world", 6); want4.put("utility", 22);
        Check.eq(want4, countMap(gc4, false), "seal 4 master: 10/8/30/14/5/9/6/22 in order");
        Check.eq(21, SpellbookText.onBar(s4), "seal 4 master: 21 on the bar");
        Check.eq("83 in the book.", SpellbookText.barLine(21, 104).substring(SpellbookText.barLine(21, 104).indexOf("83")), "barLine 21/104");

        // Seal 4, Student wand
        List<Entry> s4s = book(pool, 1, true);
        Check.eq(104, s4s.size(), "seal 4 student: known + locked = 104");
        Check.eq(want4, countMap(SpellbookText.counts(g, s4s, true), true), "seal 4 student: totals per group unchanged");

        // ---- sorting
        List<Entry> heal = SpellbookText.list(g, s4s, "healing", true);
        boolean sorted = true;
        for (int i = 1; i < heal.size(); i++) if (SpellbookText.BY_RANK_THEN_NAME.compare(heal.get(i - 1), heal.get(i)) > 0) sorted = false;
        Check.ok(sorted, "group list sorted by rank then name");
        Check.ok(heal.get(0).band == 0, "group list: beginner spells first");
        Check.ok(heal.get(heal.size() - 1).band >= heal.get(0).band, "group list: higher ranks last");
        List<Entry> az = SpellbookText.list(g, s4, SpellbookText.ALL, true);
        Check.eq(104, az.size(), "A to Z: every spell");
        boolean azSorted = true;
        for (int i = 1; i < az.size(); i++) if (az.get(i - 1).name.compareToIgnoreCase(az.get(i).name) > 0) azSorted = false;
        Check.ok(azSorted, "A to Z sorted by name");
        Check.eq(SpellbookText.known(s4s), SpellbookText.list(g, s4s, SpellbookText.ALL, false).size(), "A to Z without locked = known");
        // Mixed: a Beginner locked spell never sorts before a known Beginner spell with a later name? (rank first, then name)
        Entry a = new Entry("a", "Zed", 0, "Beginner", false), b = new Entry("b", "Abe", 1, "Student", true);
        Check.ok(SpellbookText.BY_RANK_THEN_NAME.compare(a, b) < 0, "rank before name");

        // ---- paging
        List<Entry> dmg = SpellbookText.list(g, s4, "damage", true);
        Check.eq(30, dmg.size(), "damage 30");
        Check.eq(3, SpellbookText.pages(30, SpellbookText.JAVA_PER_PAGE), "java pages 30/10 = 3");
        Check.eq(3, SpellbookText.pages(30, SpellbookText.BEDROCK_PER_PAGE), "bedrock pages 30/12 = 3");
        Check.eq(12, SpellbookText.page(dmg, 0, 12).size(), "bedrock page 1 = 12");
        Check.eq(6, SpellbookText.page(dmg, 2, 12).size(), "bedrock page 3 = 6");
        Check.eq(10, SpellbookText.page(dmg, 2, 10).size(), "java page 3 = 10");
        Check.eq(6, SpellbookText.page(dmg, 99, 12).size(), "page past end clamps to last");
        Check.eq(0, SpellbookText.clampPage(-4, 30, 10), "negative page clamps to first");
        Check.eq(1, SpellbookText.pages(0, 10), "empty list = 1 page");
        Check.eq(9, SpellbookText.pages(104, 12), "A to Z bedrock 9 pages");
        Check.eq(11, SpellbookText.pages(104, 10), "A to Z java 11 pages");
        Set<String> seen = new HashSet<>();
        for (int pg = 0; pg < 3; pg++) for (Entry e : SpellbookText.page(dmg, pg, 12)) seen.add(e.key);
        Check.eq(30, seen.size(), "pages cover every spell once");

        // ---- unlisted / Other / broken files
        List<Entry> extra = new ArrayList<>(s1);
        extra.add(new Entry("newthing", "New Thing", 3, "Master", false));
        List<GroupCount> gcx = SpellbookText.counts(g, extra, true);
        Check.eq("other", gcx.get(gcx.size() - 1).key, "unlisted spell: Other group, last");
        Check.eq("Other (1)", SpellbookText.groupButton(gcx.get(gcx.size() - 1)), "Other button");
        Check.eq(List.of("newthing"), g.unlisted(List.of("heal", "newthing", "mend|3")), "unlisted() for the boot warning");
        SpellbookGroups dup = parseText("categories:\n  a: {name: A, spells: [heal, mend]}\n  b: {name: B, spells: [heal, x]}\n");
        Check.eq("a", dup.groupOf("heal"), "duplicate: first group wins");
        Check.eq(1, dup.duplicates().size(), "duplicate reported");
        SpellbookGroups bad = parseText("categories:\n  other: {spells: [heal]}\n  all: {spells: [mend]}\n  ok: {spells: [cure]}\n");
        Check.eq(1, bad.groups().size(), "reserved keys other/all rejected");
        Check.eq("Ok", bad.nameOf("ok"), "name defaults to key");
        Check.eq(2, bad.problems().size(), "reserved keys reported");
        SpellbookGroups none = parseText("foo: bar\n");
        Check.eq(0, none.groups().size(), "no categories: no groups");
        Check.ok(!none.problems().isEmpty(), "no categories: problem reported");
        Check.eq("other", none.groupOf("heal"), "no groups: everything in Other");
        Check.eq(0, SpellbookGroups.parse(null).groups().size(), "null root never throws");

        // ---- text
        Entry ss = new Entry("stormshackle", "Storm Shackle", 1, "Student", false).rank("Rank 2 of 3").bar(3);
        Check.eq("Storm Shackle (Rank 2 of 3) bar 4", SpellbookText.javaLine(ss), "javaLine bar mark");
        Check.eq("Storm Shackle (Rank 2 of 3) - bar 4", SpellbookText.bedrockButton(ss), "bedrock button bar mark");
        Entry tame = new Entry("tame", "Tame", 3, "Master", true);
        Check.eq("Tame: unlocks at Master", SpellbookText.javaLine(tame), "javaLine locked");
        Check.eq(SpellbookText.GREY + "Tame - unlocks at Master", SpellbookText.bedrockButton(tame), "bedrock locked: gray, unlocks at");
        Entry pg = new Entry("frost", "Frost", 1, "Student", false).rank("Rank 1 of 2");
        pg.page = 2;
        Check.eq("Frost (Rank 1 of 2) page 2", SpellbookText.javaLine(pg), "no own bar: page mark");
        Entry longName = new Entry("recall_death", "Recall to Last Death", 3, "Master", false).rank("Rank 3 of 3").bar(20);
        longName.active = true;
        String lb = SpellbookText.bedrockButton(longName);
        Check.ok(lb.endsWith(" - bar 21 (in use)") && lb.length() <= 38, "long name: bar mark kept, fits (" + lb + ")");
        Check.eq("Healing (7)", SpellbookText.groupButton(gc1.get(0)), "group button");
        GroupCount hb = SpellbookText.counts(g, s1b, true).get(0);
        Check.ok(SpellbookText.groupButton(hb).contains("locked"), "group button shows locked count");
        GroupCount dmg1 = null; for (GroupCount c : SpellbookText.counts(g, s1b, true)) if (c.key.equals("damage")) dmg1 = c;
        Check.eq("Damage (1 locked)", SpellbookText.groupButton(dmg1), "group button: none known yet");
        Check.eq("Damage +1", SpellbookText.groupJava(dmg1), "java group: none known yet");
        Check.eq("Healing (3, +4 locked)", SpellbookText.groupButton(hb), "group button known + locked");
        Check.eq("Healing (10) page 1/2", SpellbookText.groupTitle("Healing", 10, 0, 2), "group title paged");
        Check.eq("Healing (7)", SpellbookText.groupTitle("Healing", 7, 0, 1), "group title one page");
        Check.eq("Healing (10), page 2 of 2", SpellbookText.groupHeading("Healing", 10, 1, 2), "java heading");
        Check.eq("Back to Healing", SpellbookText.backTo("Healing"), "back label");
        Check.eq("Slot 4: Damage 2/3", SpellbookText.pickListTitle(3, "Damage", 1, 3), "picker title");
        List<String> ll = SpellbookText.lockedLines(tame, "Tame an animal.");
        Check.eq("Unlocks at Master rank.", ll.get(2), "locked detail: unlocks at");

        // ---- every Bedrock string: <= 38 visible, no em dash, no "click"
        for (List<Entry> set : List.of(s1, s1b, s4, s4s)) {
            for (GroupCount c : SpellbookText.counts(g, set, true)) {
                br(SpellbookText.groupButton(c));
                br(SpellbookText.pickGroupButton(c));
                br(SpellbookText.backTo(c.name));
                List<Entry> l = SpellbookText.list(g, set, c.key, true);
                int pages = SpellbookText.pages(l.size(), 12);
                for (int p = 0; p < pages; p++) {
                    br(SpellbookText.groupTitle(c.name, l.size(), p, pages));
                    br(SpellbookText.pickListTitle(20, c.name, p, pages));
                }
            }
            for (Entry e : set) {
                br(SpellbookText.bedrockButton(e));
                Entry act = new Entry(e.key, e.name, e.band, e.bandName, e.locked).rank("Rank 10 of 10").bar(20);
                act.active = true;
                br(SpellbookText.bedrockButton(act));
                br(SpellbookText.lockedTitle(e));
                br(LoadoutText.pickSpellButton(e.name, 20));
            }
        }
        br(SpellbookText.groupTitle("A very long group name for testing", 104, 8, 9));
        br(SpellbookText.wandLine("An Extremely Long Wand Name Of Many Words", 104));
        br(SpellbookText.wandLine("Master Wand", 104));
        br(SpellbookText.barLine(21, 104));
        br(SpellbookText.lockedLine(87));
        for (String s : List.of(SpellbookText.HOME_TITLE, SpellbookText.ALL_BUTTON, SpellbookText.BAR_BUTTON, SpellbookText.CLOSE,
                SpellbookText.NEXT, SpellbookText.PREV, SpellbookText.GROUPS, SpellbookText.allName(), SpellbookText.HOME_HINT_BEDROCK,
                SpellbookText.backTo(SpellbookText.allName()))) br(s);
        int over = 0, dash = 0, click = 0;
        for (String s : bedrockStrings) {
            if (SpellbookText.visible(s) > 38) { over++; System.out.println("    too long: " + s); }
            if (s.contains("\u2014") || s.contains("\u2013")) dash++;
            if (s.toLowerCase(Locale.ROOT).contains("click")) click++;
        }
        Check.ok(bedrockStrings.size() > 1000, "bedrock strings checked: " + bedrockStrings.size());
        Check.eq(0, over, "bedrock: every title and button <= 38 visible characters");
        Check.eq(0, dash, "bedrock: no em or en dashes");
        Check.eq(0, click, "bedrock: no 'click'");
        // Bedrock body text (no length limit, but no click / dashes)
        List<String> bodies = new ArrayList<>(List.of(SpellbookText.groupContent(true, true), SpellbookText.groupContent(true, false),
                SpellbookText.pickGroupContent(20), LoadoutText.pickSpellContent(20)));
        bodies.addAll(SpellbookText.lockedLines(tame, "x"));
        int bodyBad = 0;
        for (String s : bodies) if (s.toLowerCase(Locale.ROOT).contains("click") || s.contains("\u2014")) { bodyBad++; System.out.println("    bad body: " + s); }
        Check.eq(0, bodyBad, "bedrock bodies: no 'click', no em dash");
        // Java text: no em dashes either
        List<String> java = new ArrayList<>(List.of(SpellbookText.groupContent(false, true), SpellbookText.HOME_HINT_JAVA, SpellbookText.ALL_JAVA,
                SpellbookText.JAVA_PREV, SpellbookText.JAVA_NEXT, SpellbookText.JAVA_GROUPS));
        for (Entry e : s4s) java.add(SpellbookText.javaLine(e));
        for (GroupCount c : SpellbookText.counts(g, s4s, true)) { java.add(SpellbookText.groupJava(c)); java.add(SpellbookText.groupHover(c)); }
        int jd = 0; for (String s : java) if (s.contains("\u2014")) jd++;
        Check.eq(0, jd, "java text: no em dashes");
    }

    static Set<String> poolKeysSeal(List<Row> rows) { Set<String> s = new HashSet<>(); for (Row r : rows) s.add(r.key); return s; }
}

