import com.winkreyes.excelwandbars.core.BarLoadout;
import com.winkreyes.excelwandbars.core.ControlsText;
import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.PageLayout;
import com.winkreyes.excelwandbars.core.TipGate;
import com.winkreyes.excelwandbars.core.WandStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 0.2.9: each player's own spell bar (BarLoadout + LoadoutText). */
public final class LoadoutTest {
    static List<String> spells(String prefix, int n) { List<String> l = new ArrayList<>(); for (int i = 1; i <= n; i++) l.add(prefix + i); return l; }

    public static void run() {
        System.out.println("LoadoutTest");
        // ---- norm
        Check.eq("heal", BarLoadout.norm("Heal|3"), "norm strips level and lower-cases");
        Check.eq(null, BarLoadout.norm("  "), "norm blank = null");

        // ---- seed: first 14 of the current order, everything seen
        List<String> s17 = spells("s", 17);
        BarLoadout l = BarLoadout.seed(s17, 14);
        Check.eq(14, l.size(), "seed size 14");
        Check.eq(14, l.filled(), "seed fills 14");
        Check.eq("s1", l.get(0), "seed slot 1 = first spell");
        Check.eq("s14", l.get(13), "seed slot 14 = 14th spell");
        Check.eq(17, l.seen().size(), "seed: all 17 seen (no new spell lines)");
        Check.eq(-1, l.indexOf("s15"), "seed: 15th in the Spellbook");
        BarLoadout small = BarLoadout.seed(spells("b", 6), 14);
        Check.eq(6, small.filled(), "beginner 6 spells: 6 filled");
        Check.eq(6, small.firstEmpty(), "first empty slot is 7");
        Check.eq(2, BarLoadout.seed(List.of("heal|2", "Heal", "boon"), 14).filled(), "seed drops duplicate levels");

        // ---- seed equals the 0.2.8 page layout for the same wand (nobody's bar moves on first load)
        Map<String, Integer> cur = new HashMap<>();
        for (int i = 0; i < 10; i++) cur.put("s" + (i + 1), (i / 7) * 8 + i % 7);
        List<String> order = PageLayout.order(cur, cur.keySet());
        BarLoadout ten = BarLoadout.seed(order, 14);
        BarLoadout.Layout lay10 = ten.layout(order, 7);
        Map<String, Integer> old = PageLayout.pack(cur, cur.keySet(), 7);
        Check.eq(old, lay10.slots, "seeded layout = 0.2.8 packed layout (10 spells)");
        Check.eq(2, lay10.pages, "10 spells = 2 pages");
        Check.eq(0, lay10.inBook, "10 spells: none in the Spellbook");

        // ---- sync: seal or rank-up adds spells, empty slots first
        BarLoadout.Sync r = small.sync(List.of("b1", "b2", "b3", "b4", "b5", "b6", "n1", "n2"));
        Check.eq(2, r.placed.size(), "2 new spells placed");
        Check.eq(Integer.valueOf(6), r.placed.get("n1"), "n1 into slot 7");
        Check.eq(Integer.valueOf(7), r.placed.get("n2"), "n2 into slot 8");
        Check.ok(r.toBook.isEmpty(), "none to the Spellbook");
        Check.ok(!small.sync(List.of("b1", "n1", "n2")).changed(), "sync again: nothing new");

        // full bar: new spells go to the Spellbook
        BarLoadout full = BarLoadout.seed(spells("m", 17), 14);
        BarLoadout.Sync r2 = full.sync(spells("m", 20));
        Check.eq(List.of("m18", "m19", "m20"), r2.toBook, "full bar: 3 new to the Spellbook");
        Check.ok(r2.placed.isEmpty(), "full bar: none placed");
        // a removed slot gets the next new spell
        full.remove(4);
        BarLoadout.Sync r3 = full.sync(spells("m", 21));
        Check.eq(Integer.valueOf(4), r3.placed.get("m21"), "removed slot 5 gets the next new spell");

        // a spell seen and then taken off is NOT put back by sync
        BarLoadout t = BarLoadout.seed(List.of("a", "b", "c"), 4);
        t.remove(1);
        t.sync(List.of("a", "b", "c", "d"));
        Check.eq(-1, t.indexOf("b"), "removed spell stays off the bar");
        Check.eq(1, t.indexOf("d"), "new spell d fills the freed slot 2");

        // ---- seal stage growth 17 -> 37 -> 63 -> 105 (Master), 14 slots
        BarLoadout seal = BarLoadout.seed(spells("x", 17), 14);
        int book = 3;
        for (int n : new int[]{37, 63, 105}) { BarLoadout.Sync s = seal.sync(spells("x", n)); book += s.toBook.size(); Check.ok(s.placed.isEmpty(), "seal " + n + ": bar full, nothing moved"); }
        Check.eq(91, book, "105 spells: 14 on bar, 91 in the Spellbook");
        BarLoadout.Layout sl = seal.layout(spells("x", 105), 7);
        Check.eq(2, sl.pages, "105 spells still 2 pages");
        Check.eq(14, sl.onBar, "14 on bar");
        Check.eq(91, sl.inBook, "91 parked");
        Check.eq(105, sl.slots.size(), "every known spell keeps a slot (Magic keeps all)");
        int minParked = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : sl.slots.entrySet()) if (seal.indexOf(e.getKey()) < 0) minParked = Math.min(minParked, e.getValue());
        Check.eq(16, minParked, "parked from slot 16 (after 2 pages of 8)");
        Check.eq(Integer.valueOf(8), sl.slots.get("x8"), "slot 8 = page 2 first icon (global 8)");
        Check.eq(Integer.valueOf(6), sl.slots.get("x7"), "slot 7 = page 1 seventh icon (global 6)");
        boolean noCol8 = true;
        for (Map.Entry<String, Integer> e : sl.slots.entrySet()) { int g = e.getValue(); if (g < 16 && g % 8 == 7) noCol8 = false; }
        Check.ok(noCol8, "Tap bar: 8th column always empty on the bar");

        // ---- layout skips spells the wand does not know
        BarLoadout k = BarLoadout.seed(List.of("a", "b", "c"), 14);
        BarLoadout.Layout kl = k.layout(List.of("a", "c"), 7);
        Check.ok(!kl.slots.containsKey("b"), "unknown spell b never on the bar (cannot be cast)");
        Check.eq(Integer.valueOf(2), kl.slots.get("c"), "c keeps its slot 3");
        Check.eq(1, kl.pages, "1 page");
        Check.eq(1, k.indexOf("b"), "b keeps its place for a wand that knows it");
        BarLoadout e = new BarLoadout(14);
        BarLoadout.Layout el = e.layout(List.of("a", "b"), 7);
        Check.eq(1, el.pages, "empty loadout: 1 page");
        Check.eq(Integer.valueOf(8), el.slots.get("a"), "empty loadout: a parked at 8");
        // last filled slot drives page count
        BarLoadout far = new BarLoadout(14); far.put("a", 12);
        Check.eq(2, far.layout(List.of("a"), 7).pages, "spell in slot 13 = 2 pages");
        Check.eq(Integer.valueOf(13), far.layout(List.of("a"), 7).slots.get("a"), "slot 13 -> global 13 (page 2 col 6)");
        // sneak bars 8 per page
        BarLoadout sb = BarLoadout.seed(spells("q", 14), 14);
        BarLoadout.Layout sbl = sb.layout(spells("q", 14), 8);
        Check.eq(Integer.valueOf(7), sbl.slots.get("q8"), "Sneak bars: slot 8 = page 1 col 8");
        Check.eq(2, sbl.pages, "Sneak bars 14 = 2 pages");

        // ---- manual: put, swap, move, remove
        BarLoadout m = BarLoadout.seed(List.of("a", "b", "c", "d", "e"), 4); // e in book
        BarLoadout.Change c1 = m.put("e", 1);
        Check.eq("put", c1.kind, "put from book");
        Check.eq("b", c1.other, "b goes back to the book");
        Check.eq("e", m.get(1), "e in slot 2");
        Check.eq(-1, m.indexOf("b"), "b off the bar");
        BarLoadout.Change c2 = m.put("a", 2);
        Check.eq("swap", c2.kind, "put an on-bar spell = swap");
        Check.eq("a", m.get(2), "a in slot 3"); Check.eq("c", m.get(0), "c in slot 1");
        BarLoadout.Change c3 = m.remove(3);
        Check.eq("remove", c3.kind, "remove");
        Check.eq(null, m.get(3), "slot 4 empty");
        BarLoadout.Change c4 = m.put("a", 3);
        Check.eq("move", c4.kind, "move to an empty slot");
        Check.eq(null, m.get(2), "old slot empty after move");
        Check.eq("same", m.put("a", 3).kind, "same slot = no change");
        Check.eq("bad", m.put("a", 99).kind, "slot out of range");
        Check.eq("bad", m.remove(2).kind, "remove empty = bad");
        BarLoadout.Change c5 = m.swap(0, 1);
        Check.eq("swap", c5.kind, "swap slots");
        Check.eq("e", m.get(0), "e now slot 1");
        Check.eq("move", m.swap(0, 2).kind, "swap with empty = move");
        Check.ok(m.seen().contains("b"), "put does not forget seen");

        // ---- resize: shrink loses nothing (wand still knows them), grow adds empty
        BarLoadout rz = BarLoadout.seed(spells("r", 14), 14);
        rz.resize(7);
        Check.eq(7, rz.size(), "shrink to 7");
        Check.eq(14, rz.seen().size(), "shrink keeps seen");
        Check.eq(7, rz.layout(spells("r", 14), 7).inBook, "shrink: 7 to the Spellbook");
        rz.resize(21);
        Check.eq(7, rz.firstEmpty(), "grow: new empty slots at the end");
        Check.ok(!rz.sync(spells("r", 14)).changed(), "grow does not re-announce old spells");
        Check.eq(BarLoadout.MAX_SLOTS, new BarLoadout(1000).size(), "size clamp max");
        Check.eq(1, new BarLoadout(0).size(), "size clamp min");

        // ---- storage round trip
        BarLoadout st = BarLoadout.seed(List.of("a", "b", "c"), 5);
        st.remove(1);
        List<String> stored = st.storedSlots();
        Check.eq(List.of("a", "", "c", "", ""), stored, "stored slots with blanks");
        BarLoadout back = BarLoadout.restore(stored, st.seen(), 5);
        Check.eq(st.slots(), back.slots(), "restore slots");
        Check.eq(st.seen(), back.seen(), "restore seen");
        BarLoadout junk = BarLoadout.restore(List.of("a", "A", "", "b|2"), List.of("a", "b"), 3);
        Check.eq(java.util.Arrays.asList("a", null, null), junk.slots(), "restore drops duplicates and cuts to size");
        Check.ok(junk.seen().contains("b"), "cut slot spell still seen");

        // ---- placement order: template list first, then current slot
        Map<String, Integer> pc = new LinkedHashMap<>(); pc.put("z", 0); pc.put("y", 1); pc.put("w", 2);
        Check.eq(List.of("y", "z", "w"), BarLoadout.placementOrder(List.of("y", "z", "gone"), pc, List.of("z", "y", "w")), "placement: template order, then slot");

        // ---- text: lines, buttons <= 38, no em dashes
        Check.eq("New spell: Heal added to your bar", LoadoutText.newOnBar("Heal"), "on-bar line (brief wording)");
        Check.eq("New spell: Heal is in your Spellbook", LoadoutText.newInBook("Heal"), "book line (brief wording)");
        List<String> ann = LoadoutText.announce(List.of("A", "B"), List.of("C", "D", "E"), 3);
        Check.eq(5, ann.size(), "announce: 3 named + more + how to change");
        Check.eq("2 more new spells. See /spellbook", ann.get(3), "announce more line");
        Check.eq(LoadoutText.HOW_TO_CHANGE, ann.get(4), "announce: how to change when some are in the book");
        Check.eq(1, LoadoutText.announce(List.of("A"), List.of(), 3).size(), "one placed = one line");
        List<String> ui = new ArrayList<>(List.of(LoadoutText.BAR_TITLE, LoadoutText.BOOK_BUTTON, LoadoutText.PUT_BUTTON, LoadoutText.MOVE_BUTTON, LoadoutText.OFF_BUTTON,
                LoadoutText.PICK_BUTTON, LoadoutText.BACK_BAR, LoadoutText.BACK_BOOK, LoadoutText.CLOSE,
                LoadoutText.barTitleCount(14, 14), LoadoutText.slotButton(13, "Recall to Last Death Point Again"), LoadoutText.slotTitle(13, null),
                LoadoutText.moveButton("Greater Heal"), LoadoutText.offButton("Summon a Very Long Named Thing"),
                LoadoutText.pickSpellButton("Greater Heal", 13), LoadoutText.pickTitle("Greater Heal")));
        boolean fits = true;
        for (String u : ui) if (u.length() > LoadoutText.MAX) { fits = false; System.out.println("    too long: " + u); }
        Check.ok(fits, "every menu line <= 38 chars");
        Check.eq("14: Recall to Last Death Point Again", LoadoutText.slotButton(13, "Recall to Last Death Point Again"), "slot button");
        Check.ok(LoadoutText.slotButton(1, "An Extremely Long Spell Name That Goes On").endsWith("..."), "long name trimmed with ...");
        List<String> all = new ArrayList<>(ui);
        all.addAll(ann);
        all.add(LoadoutText.barContent(true, 7, 14, 3)); all.add(LoadoutText.barContent(false, 7, 14, 0));
        all.add(LoadoutText.pickSlotContent("Heal")); all.add(LoadoutText.pickSpellContent(2));
        all.add(LoadoutText.NOT_KNOWN); all.add(LoadoutText.NO_WAND); all.add(LoadoutText.NOT_BAR_STYLE); all.add(LoadoutText.OFF);
        all.add(LoadoutText.onePage(6, 6)); all.add(LoadoutText.onePage(6, 9));
        Map<String, String> nm = Map.of("a", "Heal", "b", "Boon");
        for (BarLoadout.Change ch : List.of(c1, c2, c3, c4, c5)) all.add(LoadoutText.changed(ch, nm));
        all.addAll(ControlsText.howTo(WandStyle.TAPBAR, true)); all.addAll(ControlsText.howTo(WandStyle.TAPBAR, false));
        all.addAll(ControlsText.howTo(WandStyle.ORIGINAL, true)); all.addAll(ControlsText.howTo(WandStyle.ORIGINAL, false));
        all.add(TipGate.L_TAPBAR);
        boolean clean = true;
        for (String a : all) if (a.contains("\u2014") || a.contains("\u2013")) { clean = false; System.out.println("    dash: " + a); }
        Check.ok(clean, "no em or en dashes in any 0.2.9 text");
        Check.eq("Page 1/1: all 6 of your spells are on the bar", LoadoutText.onePage(6, 6), "one page, all on bar (0.2.8 wording)");
        Check.eq("Page 1/1: 6 of your 9 spells. More: /spellbar", LoadoutText.onePage(6, 9), "one page, some in book");
        Check.eq("Heal is in slot 2 now. Boon went back to your Spellbook.", LoadoutText.changed(c1.kind.equals("put") ? c1 : null, Map.of("e", "Heal", "b", "Boon")), "put line");
        Check.eq("Slots 1 to 7 are page 1, 8 to 14 page 2.", LoadoutText.pagesLine(7, 14), "pages line 14");
        Check.eq("Slots 1 to 7 are page 1, 8 to 14 page 2, 15 to 21 page 3.", LoadoutText.pagesLine(7, 21), "pages line 21");
        Check.eq("Slots 1 to 8 are page 1, 9 to 14 page 2.", LoadoutText.pagesLine(8, 14), "pages line Sneak bars");
        Check.eq("All 5 slots are on page 1.", LoadoutText.pagesLine(7, 5), "pages line 1 page");
        Check.eq("7 slots per page, 5 pages.", LoadoutText.pagesLine(7, 35), "pages line many");
        boolean bedrockNoClick = true;
        for (String a : ControlsText.howTo(WandStyle.TAPBAR, true)) if (a.toLowerCase().contains("click")) bedrockNoClick = false;
        Check.ok(bedrockNoClick, "Bedrock Tap bar how-to never says click");
        Check.ok(!LoadoutText.barContent(true, 7, 14, 0).toLowerCase().contains("click"), "Bedrock bar menu never says click");
            // ---- default 21 slots (Mike, Oct 26, 2026: 3 pages of 7)
        Check.eq(21, BarLoadout.DEFAULT_SLOTS, "default bar size 21");
        BarLoadout d17 = BarLoadout.seed(spells("s", 17), BarLoadout.DEFAULT_SLOTS);
        Check.eq(21, d17.size(), "default seed size 21");
        Check.eq(17, d17.filled(), "Seal 1 Master 17 spells: all 17 on the bar");
        Check.eq("s17", d17.get(16), "slot 17 = 17th spell");
        Check.eq(17, d17.firstEmpty(), "first empty slot is 18");
        Map<String, Integer> cur17 = new HashMap<>();
        for (int i = 0; i < 17; i++) cur17.put("s" + (i + 1), (i / 7) * 8 + i % 7);
        List<String> order17 = PageLayout.order(cur17, cur17.keySet());
        BarLoadout.Layout lay17 = BarLoadout.seed(order17, BarLoadout.DEFAULT_SLOTS).layout(order17, 7);
        Check.eq(PageLayout.pack(cur17, cur17.keySet(), 7), lay17.slots, "21 slots: seeded 17 = 0.2.8 packed layout");
        Check.eq(3, lay17.pages, "17 spells on 21 slots = 3 pages (same as 0.2.8)");
        Check.eq(0, lay17.inBook, "17 spells on 21 slots: 0 in the Spellbook");
        Check.eq(17, lay17.onBar, "17 on bar");
        BarLoadout.Sync d1 = d17.sync(spells("s", 38));
        Check.eq(4, d1.placed.size(), "rank-up +21: 4 fill slots 18 to 21");
        Check.eq(Integer.valueOf(17), d1.placed.get("s18"), "s18 into slot 18");
        Check.eq(Integer.valueOf(20), d1.placed.get("s21"), "s21 into slot 21");
        Check.eq(17, d1.toBook.size(), "rank-up +21: 17 to the Spellbook");
        BarLoadout.Sync d2 = d17.sync(spells("s", 45));
        Check.ok(d2.placed.isEmpty() && d2.toBook.size() == 7, "full 21 bar: next 7 to the Spellbook");
        BarLoadout d105 = BarLoadout.seed(spells("y", 17), BarLoadout.DEFAULT_SLOTS);
        int dbook = 0;
        for (int n : new int[]{37, 63, 105}) dbook += d105.sync(spells("y", n)).toBook.size();
        Check.eq(84, dbook, "105 spells: 21 on bar, 84 in the Spellbook");
        BarLoadout.Layout dl = d105.layout(spells("y", 105), 7);
        Check.eq(3, dl.pages, "105 spells on 21 slots: 3 pages");
        Check.eq(21, dl.onBar, "21 on bar");
        int dMin = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> en : dl.slots.entrySet()) if (d105.indexOf(en.getKey()) < 0) dMin = Math.min(dMin, en.getValue());
        Check.eq(24, dMin, "parked from slot 24 (after 3 pages of 8)");
        Check.eq(Integer.valueOf(16), dl.slots.get("y15"), "slot 15 = page 3 first icon (global 16)");
        BarLoadout.Layout dsb = BarLoadout.seed(spells("z", 21), BarLoadout.DEFAULT_SLOTS).layout(spells("z", 21), 8);
        Check.eq(3, dsb.pages, "Sneak bars 21 = 8 + 8 + 5 = 3 pages");
        Check.eq("Your spell bar (17 of 21)", LoadoutText.barTitleCount(17, 21), "title 17 of 21");
        Check.ok(LoadoutText.barTitleCount(21, 21).length() <= LoadoutText.MAX && LoadoutText.slotButton(20, "Recall to Last Death Point Again").length() <= LoadoutText.MAX
                && LoadoutText.slotTitle(20, null).length() <= LoadoutText.MAX, "21-slot buttons and titles <= 38");
        Check.eq("21: (empty)", LoadoutText.slotButton(20, null), "slot 21 empty button");
        Check.ok(LoadoutText.barContent(true, 7, 21, 0).contains("15 to 21 page 3") && !LoadoutText.barContent(true, 7, 21, 0).toLowerCase().contains("click"), "Bedrock bar menu names page 3, no click");
        try {
            String cfg = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("config.yml")), java.nio.charset.StandardCharsets.UTF_8);
            Check.ok(cfg.contains("\nbar-loadout:\n  enabled: true\n") && cfg.contains("\n  slots: 21\n") && !cfg.contains("slots: 14"), "bundled config.yml: bar-loadout slots 21");
        } catch (java.io.IOException ex) { Check.ok(false, "read config.yml: " + ex); }
    }
}

