package com.winkreyes.excelskillspanel;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.winkreyes.excelskillspanel.bukkit.BedrockForms;
import com.winkreyes.excelskillspanel.core.MagicLines;
import com.winkreyes.excelskillspanel.core.PanelModel;
import com.winkreyes.excelskillspanel.core.PerkLadder;
import com.winkreyes.excelskillspanel.core.SkillMath;
import com.winkreyes.excelskillspanel.provider.mcmmo.McMMoProvider;
import com.winkreyes.excelskillspanel.provider.wrv.MagicApiReads;
import com.winkreyes.excelskillspanel.provider.wrv.MagicGateReads;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelSkillsPanel harness: the pure model (bars, rows, the Magic
 * header composition), the provider seam's mcMMO skill census,
 * and the Bedrock form handler proof against the REAL Cumulus
 * classes in the staged Geyser jar (the Summons 0.1.8 pattern —
 * forms must not only SEND, taps must ARRIVE).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
class PanelTest {

    // ----------------- bars -----------------

    @Test
    void barMath() {
        PanelModel.BarData half = new PanelModel.BarData(1000, 2000);
        assertEquals(0.5, half.fraction());
        assertEquals(50, half.percent());
        assertEquals("\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1\u25B1", half.bar());
        assertEquals("\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1\u25B1 50% \u2014 1,000 / 2,000 XP",
                half.line());
        assertEquals("\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1\u25B1 50%", half.compact());
    }

    @Test
    void barClampsAndCaps() {
        PanelModel.BarData over = new PanelModel.BarData(3000, 2000);
        assertEquals(1.0, over.fraction(), "over-full clamps to 100%");
        assertEquals(100, over.percent());
        PanelModel.BarData negative = new PanelModel.BarData(-50, 2000);
        assertEquals(0.0, negative.fraction(), "negative into clamps to 0");
        PanelModel.BarData capped = new PanelModel.BarData(500, 0);
        assertTrue(capped.capped(), "span 0 is the cap");
        assertEquals(1.0, capped.fraction());
        assertTrue(capped.line().endsWith("MAX"), "capped line says MAX, no invented fraction");
        assertTrue(capped.compact().endsWith("MAX"));
        PanelModel.BarData empty = new PanelModel.BarData(0, 2000);
        assertEquals(0, empty.percent());
        assertEquals("\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1", empty.bar());
    }

    @Test
    void numberFormatting() {
        assertEquals("1,240", PanelModel.fmt(1240));
        assertEquals("0", PanelModel.fmt(0));
        assertEquals("17,821", PanelModel.fmt(17821));
    }

    // ----------------- mcMMO rows (pure composition) -----------------

    @Test
    void mcmmoRowComposition() {
        PanelModel.SkillRow row = SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                214, 1240, 2000, null);
        assertEquals("Mining 214 \u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1 62%",
                row.compactLine());
        assertEquals("Mining \u2014 Lv 214", row.buttonLabel());
        assertEquals(List.of(
                "Level: 214",
                "\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1 62% \u2014 1,240 / 2,000 XP",
                "XP into this level: 1,240",
                "XP to next level: 760",
                SkillMath.PERK_POINTER), row.detailLines());
    }

    @Test
    void mcmmoRowRankAndCap() {
        PanelModel.SkillRow ranked = SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                10, 5, 100, "Adept");
        assertTrue(ranked.detailLines().contains("Rank: Adept"),
                "a verified rank label shows in the detail view");
        PanelModel.SkillRow capped = SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                1000, 0, 0, null);
        assertTrue(capped.detailLines().get(3).contains("at the cap"),
                "a capped skill's detail says so instead of a fake remainder");
        PanelModel.SkillRow blank = SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                10, 5, 100, " ");
        assertFalse(blank.detailLines().toString().contains("Rank:"),
                "a blank rank label is treated as absent");
    }

    @Test
    void childSkillRowComposition() {
        // Salvage / Smelting: a level but no XP and no bar —
        // mcMMO's XP reads throw for child skills (rig-proven).
        PanelModel.SkillRow row = SkillMath.childRow("salvage", "Salvage", "GOLD_INGOT",
                12, null);
        assertNull(row.bar(), "a child row carries no bar");
        assertEquals("Salvage 12 (child skill)", row.compactLine());
        assertEquals("Salvage \u2014 Lv 12", row.buttonLabel());
        assertEquals("Level: 12", row.detailLines().get(0));
        assertTrue(row.detailLines().get(1).contains("parent skills"));
        assertEquals(SkillMath.PERK_POINTER,
                row.detailLines().get(row.detailLines().size() - 1));
    }

    @Test
    void powerHeaderComposition() {
        // 0.1.1 (Mike, Nov 13, 2026): ONE line — the value lives in
        // the block title; the block carries no lines, so no
        // surface can print heading + value as two lines.
        PanelModel.HeaderBlock header = SkillMath.powerHeader(579, Integer.MAX_VALUE);
        assertEquals("Power Level 579", header.title(),
                "the single Power Level line, no colon, value in the title");
        assertEquals(List.of(), header.lines(), "no separate value line remains");
        assertEquals(List.of(), header.allLines(), "no lore/body lines duplicate the title");
        PanelModel.HeaderBlock big = SkillMath.powerHeader(1234, 5000);
        assertEquals("Power Level 1,234", big.title(), "thousands formatting kept");
        // The cap line is gone ENTIRELY — even a configured cap,
        // and above all the fork's uncapped Integer.MAX_VALUE.
        for (PanelModel.HeaderBlock h : List.of(header, big, SkillMath.powerHeader(42, 0))) {
            String all = h.title() + "|" + h.allLines();
            assertFalse(all.contains("cap"), "no cap text anywhere: " + all);
            assertFalse(all.contains("2,147,483,647"),
                    "Integer.MAX_VALUE noise never renders: " + all);
        }
    }

    @Test
    void mcmmoSkillCensusIsThe19() {
        assertEquals(19, McMMoProvider.SKILLS.size(), "the panel maps all 19 primary skills");
        assertEquals(19, PrimarySkillType.values().length,
                "the fork's PrimarySkillType census the design verified");
        for (McMMoProvider.SkillDef def : McMMoProvider.SKILLS) {
            assertNotNull(def.type());
            assertFalse(def.displayName().isBlank());
            assertFalse(def.iconKey().isBlank());
        }
        // declaration order preserved end to end
        assertEquals(PrimarySkillType.ACROBATICS, McMMoProvider.SKILLS.get(0).type());
        assertEquals(PrimarySkillType.WOODCUTTING, McMMoProvider.SKILLS.get(18).type());
    }

    // ----------------- Magic header composition -----------------

    private static MagicLines.MagicData fullMagic() {
        return new MagicLines.MagicData(
                12, 1240L, 760L, 54321L,
                "Apprentice", "apprentice", "Master",
                150.0f, 150, 25,
                "13.75", "13.75", "5.5",
                1234L, 4);
    }

    @Test
    void magicHeaderFullComposition() {
        MagicLines.MagicData d = fullMagic();
        List<String> lines = MagicLines.headerLines(d);
        assertEquals(List.of(
                "Magic Level 12 \u2014 54,321 XP total",
                "Rank: Apprentice",
                "Next rank: Master",
                "Mana: 150 / 150",
                "Mana regen: 25",
                "Boosts: mana pool +13.75%, mana regen +13.75%, cast cost -5.5%",
                "Credited casts: 1,234",
                "Guild seals: 4 of 5"), lines);
        PanelModel.HeaderBlock block = MagicLines.headerBlock(d);
        assertNotNull(block);
        assertTrue(block.hasDetail(), "the Magic block opens the detail view");
        assertEquals(new PanelModel.BarData(1240, 2000), block.bar(),
                "the Magic bar spans into + to-next");
        // 0.2.1 (Item 9a): the first header line IS the block's
        // title; the standalone "Magic" title line is gone and
        // the remaining lines follow it exactly once.
        assertEquals("Magic Level 12 \u2014 54,321 XP total", block.title(),
                "0.2.1: the Magic Level line titles the block (Item 9a)");
        assertEquals(lines.subList(1, lines.size()), block.lines(),
                "0.2.1: the block's lines are the header lines after the first");
        assertFalse(block.title().equals("Magic"),
                "0.2.1: no standalone Magic title remains");
    }

    @Test
    void magicHeaderOmitsWhatDidNotRead() {
        MagicLines.MagicData sparse = new MagicLines.MagicData(
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null);
        assertNull(MagicLines.headerBlock(sparse),
                "nothing read -> no Magic block at all, never an empty or faked one");
        MagicLines.MagicData levelOnly = new MagicLines.MagicData(
                3, 10L, 90L, 250L,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null);
        assertEquals(List.of("Magic Level 3 \u2014 250 XP total"),
                MagicLines.headerLines(levelOnly));
        // rank falls back to the KEY only when the name did not read
        MagicLines.MagicData keyOnly = new MagicLines.MagicData(
                null, null, null, null,
                null, "apprentice", null,
                null, null, null,
                null, null, null,
                null, null);
        assertEquals(List.of("Rank: apprentice"), MagicLines.headerLines(keyOnly));
    }

    @Test
    void magicBoostsLineLevels() {
        MagicLines.MagicData one = new MagicLines.MagicData(
                1, 0L, 100L, 0L, null, null, null, null, null, null,
                "0", "0", "0", null, null);
        assertEquals("Boosts: none yet \u2014 they start at Magic Level 2",
                MagicLines.boostsLine(one), "level 1 mirrors ExcelMagicLevel's own wording");
        MagicLines.MagicData poolOnly = new MagicLines.MagicData(
                5, 0L, 100L, 400L, null, null, null, null, null, null,
                "5", null, null, null, null);
        assertEquals("Boosts: mana pool +5%", MagicLines.boostsLine(poolOnly),
                "unread boost parts are omitted, not zero-filled");
    }

    @Test
    void magicBarAtCap() {
        MagicLines.MagicData capped = new MagicLines.MagicData(
                20, 500L, 0L, 99999L, null, null, null, null, null, null,
                null, null, null, null, null);
        PanelModel.BarData bar = MagicLines.magicBar(capped);
        assertNotNull(bar);
        assertTrue(bar.line().endsWith("MAX"),
                "to-next 0 with XP banked renders the capped bar (span = into)");
        assertNull(MagicLines.magicBar(new MagicLines.MagicData(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null)), "no level surface -> no bar");
    }

    @Test
    void magicCastsLabelledCredited() {
        // 0.2.2 (Item 13): the casts line carries MagicGate's
        // rank-driving CREDITED count — and the label says so.
        // The old "Total casts" label (ExcelMagicLevel's different
        // counter, 694 for Mike) is gone from every surface.
        assertTrue(MagicLines.headerLines(fullMagic()).contains("Credited casts: 1,234"),
                "the header carries the credited count: " + MagicLines.headerLines(fullMagic()));
        for (String line : MagicLines.headerLines(fullMagic())) {
            assertFalse(line.startsWith("Total casts:"),
                    "the old total-casts label is gone: " + line);
        }
        for (String line : MagicLines.detailLines(fullMagic())) {
            assertFalse(line.startsWith("Total casts:"),
                    "the detail view keeps the same label discipline: " + line);
        }
        assertTrue(MagicLines.detailLines(fullMagic()).contains("Credited casts: 1,234"));
    }

    // ----------------- 0.2.2 Item 12: no Class line -----------------

    @Test
    void magicHeaderHasNoClassLine() {
        // Mike, Nov 13, 2026: "Maybe remove the class mage line
        // since everyone is technically a mage." No header line,
        // detail line, Bedrock body line, or chat line may start
        // with "Class:" — the field is gone from the record, not
        // merely nulled.
        for (String line : MagicLines.headerLines(fullMagic())) {
            assertFalse(line.startsWith("Class:"),
                    "no Class line in the header: " + line);
        }
        for (String line : MagicLines.detailLines(fullMagic())) {
            assertFalse(line.startsWith("Class:"),
                    "no Class line in the detail view: " + line);
        }
        PanelModel.Panel panel = new PanelModel.Panel(
                List.of(SkillMath.powerHeader(100, 0),
                        MagicLines.headerBlock(fullMagic())),
                List.of(SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                        214, 1240, 2000, null)),
                MagicLines.detailLines(fullMagic()));
        for (String line : panel.bedrockBody().split("\n")) {
            assertFalse(line.startsWith("Class:"),
                    "no Class line in the Bedrock body: " + line);
        }
        for (String line : panel.chatLines()) {
            assertFalse(line.startsWith("Class:"),
                    "no Class line in the chat fallback: " + line);
        }
    }

    // ----------------- 0.2.2 Item 13: credited casts + rank order -----------------

    @Test
    void magicGateLoreParse() {
        // The wand lore's own "Credited casts" line, as MagicGate
        // writes it (legacy codes, thousands commas) — the
        // fallback read when the ledger cannot answer.
        assertEquals(1215L, MagicGateReads.parseCreditedFromLore(List.of(
                "\u00a70\u00a7r\u00a76Rank: Apprentice \u00a77(ceiling: Master)",
                "\u00a77Credited casts: \u00a7f1,215",
                "\u00a77Next: Master \u2014 785 to go")));
        assertEquals(400L, MagicGateReads.parseCreditedFromLore(List.of(
                "Credited casts: 400")));
        assertEquals(0L, MagicGateReads.parseCreditedFromLore(List.of(
                "Credited casts: 0")));
        assertNull(MagicGateReads.parseCreditedFromLore(List.of(
                "Rank: Apprentice (ceiling: Master)",
                "Next: Master \u2014 785 to go")),
                "no credited line -> null, never a guessed count");
        assertNull(MagicGateReads.parseCreditedFromLore(null));
        assertNull(MagicGateReads.parseCreditedFromLore(List.of()));
        assertNull(MagicGateReads.parseCreditedFromLore(List.of(
                "Credited casts: many")),
                "a malformed count is not a count");
        assertNull(MagicGateReads.parseCreditedFromLore(List.of(
                "Credited casts: -5")),
                "a negative count is not a count");
    }

    @Test
    void magicRankOrder() {
        // The inventory-scan tiebreak: highest path wins, so a
        // carried beginner wand can never outrank the Renewal
        // wand. Unknown keys rank below beginner and are never
        // guessed at.
        assertTrue(MagicApiReads.rankOrder("master")
                > MagicApiReads.rankOrder("apprentice"));
        assertTrue(MagicApiReads.rankOrder("apprentice")
                > MagicApiReads.rankOrder("student"));
        assertTrue(MagicApiReads.rankOrder("student")
                > MagicApiReads.rankOrder("beginner"));
        assertEquals(0, MagicApiReads.rankOrder("beginner"));
        assertEquals(2, MagicApiReads.rankOrder("Apprentice"),
                "rank keys match case-insensitively");
        assertEquals(-1, MagicApiReads.rankOrder("quartermaster"));
        assertEquals(-1, MagicApiReads.rankOrder(null));
        assertEquals(-1, MagicApiReads.rankOrder(""));
    }

    @Test
    void magicNextRankIsWrvOrderNotMagicsWizard() {
        // Phase A finding: Magic's own display name for the
        // master path is "Wizard" (messages.defaults.yml,
        // paths.master.name '&6Wizard'; "Survival Progression:
        // ... Apprentice, Wizard (AKA Master)"), so
        // path.getNextPath().getName() on an apprentice wand
        // returns Wizard — the 0.2.1 read would have shown
        // "Next rank: Wizard" even with the wand held. Rank and
        // next rank come from the WRV order instead: the ladder
        // has exactly four ranks and its wand lore writes
        // "Next: Master".
        assertEquals("Master", MagicApiReads.wrvDisplayName("master"),
                "master displays as Master, never Wizard");
        assertEquals("Apprentice", MagicApiReads.wrvDisplayName("apprentice"));
        assertEquals("Student", MagicApiReads.wrvDisplayName("student"));
        assertEquals("Beginner", MagicApiReads.wrvDisplayName("beginner"));
        assertEquals("Master", MagicApiReads.wrvDisplayName("MASTER"),
                "display names match case-insensitively");
        assertNull(MagicApiReads.wrvDisplayName("wizard"),
                "wizard is not a WRV rank — never displayed");
        assertNull(MagicApiReads.wrvDisplayName(null));
        assertEquals("master", MagicApiReads.wrvNextKey("apprentice"),
                "apprentice's next rank is master");
        assertEquals("student", MagicApiReads.wrvNextKey("beginner"));
        assertEquals("apprentice", MagicApiReads.wrvNextKey("student"));
        assertNull(MagicApiReads.wrvNextKey("master"),
                "master is the top rank — the Next line is omitted");
        assertNull(MagicApiReads.wrvNextKey("wizard"));
        assertNull(MagicApiReads.wrvNextKey(null));
    }

    @Test
    void magicEffectiveManaIsFramePlusBoost() {
        // Phase A finding: the live wand's mana is synced to the
        // EFFECTIVE pool (frame x (1 + boost)), truncated as
        // Magic truncates — the frame alone understates it.
        // Rig level 136 (mana boost 3.2095%): 150 -> 154.
        assertEquals(154, MagicLines.effectiveManaMax(150, "3.2095"));
        // The no-wand class base reads the same way: 50 -> 51.
        assertEquals(51, MagicLines.effectiveManaMax(50, "3.2095"));
        // At Mike's level 26 (boost 0.5943%) the formula is a
        // no-op: 150 -> 150, 60 -> 60 — his "150 mana" pin holds.
        assertEquals(150, MagicLines.effectiveManaMax(150, "0.5943"));
        assertEquals(60, MagicLines.effectiveManaMax(60, "0.5943"));
        // No boost surface -> the frame, never a guessed boost.
        assertEquals(150, MagicLines.effectiveManaMax(150, null));
        assertEquals(150, MagicLines.effectiveManaMax(150, "not-a-number"));
        // Regen: frame 25 x (1 + fraction), truncated — 25 at
        // both rig level 136 (0.032095) and Mike's level 26
        // (0.005943).
        assertEquals(25, MagicLines.effectiveManaRegen(25, 0.032095));
        assertEquals(25, MagicLines.effectiveManaRegen(25, 0.005943));
        assertEquals(5, MagicLines.effectiveManaRegen(5, 0.032095));
        assertEquals(35, MagicLines.effectiveManaRegen(35, 0.0),
                "no boost -> the frame");
    }

    @Test
    void magicDetailLeadsWithExplainer() {
        List<String> detail = MagicLines.detailLines(fullMagic());
        assertEquals(MagicLines.EXPLAINER, detail.get(0),
                "the detail view leads with the canonical explainer");
        assertTrue(detail.contains("Magic XP: "
                        + "\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1 62% \u2014 1,240 / 2,000 XP"),
                "the detail view carries the bar numbers line");
        assertTrue(MagicLines.detailLines(new MagicLines.MagicData(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null)).isEmpty(),
                "no reads -> no detail view");
    }

    @Test
    void percentFormattingMatchesExcelMagicLevel() {
        // ExcelMagicLevel Percents.format's own documented examples
        // (fraction -> percent number); all are exact at the panel's
        // scale-2 display rounding too, so they still agree.
        assertEquals("1.25", MagicLines.percentOf(0.0125));
        assertEquals("9.5", MagicLines.percentOf(0.095));
        assertEquals("23.75", MagicLines.percentOf(0.2375));
        assertEquals("0", MagicLines.percentOf(0.0));
        // the reward-table formula at level 12: per-level x 11
        assertEquals("13.75", MagicLines.percentOf(0.0125 * 11));
        assertEquals("5.5", MagicLines.percentOf(0.005 * 11));
    }

    @Test
    void boostsRenderRoundedToTwoDecimals() {
        // 0.1.1 (Mike, Nov 13, 2026): the values are FRACTIONS in
        // ExcelMagicLevel's Boost record; Percents.format turns
        // them into percent numbers at scale 4. On the shipped
        // 1000-level curve (per-level = cap / 999), Mike's Magic
        // Level 26 carries 25 steps: mana 25 x 0.0002377377... =
        // 0.5943%, cost 25 x 0.0000950951... = 0.2377% — his live
        // screenshot's exact numbers. The panel renders scale 2.
        double perLevelMana = 0.2375 / 999.0;
        double perLevelCost = 0.095 / 999.0;
        assertEquals("0.59", MagicLines.percentOf(perLevelMana * 25),
                "the computed (regen-style) path rounds the same way");
        assertEquals("0.24", MagicLines.percentOf(perLevelCost * 25));
        assertEquals("0.59", MagicLines.displayPct("0.5943"),
                "the PAPI-string (mana pool) path re-renders at scale 2");
        assertEquals("0.24", MagicLines.displayPct("0.2377"));
        assertEquals("3.21", MagicLines.displayPct("3.2095"),
                "the rig's level-136 value rounds up cleanly");
        assertEquals("1.28", MagicLines.displayPct("1.2838"));
        assertEquals("13.75", MagicLines.displayPct("13.75"),
                "already-clean values are untouched");
        assertEquals("0", MagicLines.displayPct("0"));
        assertEquals("n/a", MagicLines.displayPct("n/a"),
                "an unparseable provider string passes through, never hidden");
        // Whole-percent rounding was rejected on purpose: it would
        // print 0.59% as 1% and 0.24% as 0%. Scale 2 keeps the
        // smallest real boost visible (level 2: 0.02% / 0.01%).
        assertEquals("0.02", MagicLines.percentOf(perLevelMana));
        assertEquals("0.01", MagicLines.percentOf(perLevelCost));
        // Mike's exact live DTO — CORRECTED by 0.2.2 (Item 13):
        // level 26, PAPI-formatted strings, but the rank/mana/
        // casts are now the Renewal wand's own values (Apprentice
        // / 150 / 25 / 1,215 credited) instead of the class-base
        // fallback the live 0.2.1 panel showed (Beginner / 60 / 5
        // / 694). The boosts are unchanged — they were never wrong.
        MagicLines.MagicData mike = new MagicLines.MagicData(
                26, 10L, 19L, 579L,
                "Apprentice", "apprentice", "Master",
                150.0f, 150, 25,
                "0.5943", "0.5943", "0.2377",
                1215L, 4);
        assertEquals("Boosts: mana pool +0.59%, mana regen +0.59%, cast cost -0.24%",
                MagicLines.boostsLine(mike));
        assertEquals(List.of(
                "Magic Level 26 \u2014 579 XP total",
                "Rank: Apprentice",
                "Next rank: Master",
                "Mana: 150 / 150",
                "Mana regen: 25",
                "Boosts: mana pool +0.59%, mana regen +0.59%, cast cost -0.24%",
                "Credited casts: 1,215",
                "Guild seals: 4 of 5"),
                MagicLines.headerLines(mike),
                "0.2.2: Mike's corrected live header — the panel reads what his wand says");
    }

    // ----------------- perk ladders (0.2.0, Item 8 / D32) -----------------

    @Test
    void ladderCensusPerSkillRowCounts() {
        // Per-skill display-perk counts from LADDER-DATA §3 — 82
        // perks rendered across all 19 skills (83 display rows
        // minus Flux Mining, omitted entirely per D32).
        java.util.Map<String, Integer> expected = new java.util.LinkedHashMap<>();
        expected.put("acrobatics", 3); expected.put("alchemy", 2);
        expected.put("archery", 4); expected.put("axes", 6);
        expected.put("crossbows", 3); expected.put("excavation", 2);
        expected.put("fishing", 6); expected.put("herbalism", 7);
        expected.put("maces", 3); expected.put("mining", 6);
        expected.put("repair", 3); expected.put("salvage", 2);
        expected.put("smelting", 3); expected.put("spears", 3);
        expected.put("swords", 5); expected.put("taming", 10);
        expected.put("tridents", 2); expected.put("unarmed", 7);
        expected.put("woodcutting", 5);
        assertEquals(19, PerkLadder.skillIds().size(), "all 19 skills carry a ladder");
        assertEquals(expected.keySet().toString(), PerkLadder.skillIds().toString(),
                "ladder skill order is the panel's skill order");
        for (var e : expected.entrySet()) {
            assertEquals(e.getValue().intValue(), PerkLadder.perksFor(e.getKey()).size(),
                    e.getKey() + " renders its verified perk count");
        }
        assertEquals(82, PerkLadder.totalPerks(), "82 perks total rendered");
        // the ladder order matches the provider's 19-skill order
        for (int i = 0; i < McMMoProvider.SKILLS.size(); i++) {
            assertEquals(McMMoProvider.SKILLS.get(i).type().name().toLowerCase(),
                    PerkLadder.skillIds().get(i));
        }
    }

    @Test
    void ladderSpotValuesMatchVerifiedTable() {
        PerkLadder.Perk concoctions = perkNamed("alchemy", "Concoctions");
        assertArrayEquals(new int[]{0, 100, 200, 350, 500, 750, 900, 1000},
                concoctions.retroUnlocks());
        assertEquals(8, concoctions.numRanks());
        assertArrayEquals(new int[]{1, 100, 150, 200, 250, 300, 350, 400, 450, 500,
                        550, 600, 650, 700, 750, 800, 850, 900, 950, 1000},
                perkNamed("archery", "Skill Shot").retroUnlocks());
        assertArrayEquals(new int[]{300, 600},
                perkNamed("woodcutting", "Knock on Wood").retroUnlocks());
        assertArrayEquals(new int[]{750, 1000},
                perkNamed("swords", "Stab").retroUnlocks());
        assertArrayEquals(new int[]{100, 500, 750},
                perkNamed("smelting", "Fuel Efficiency").retroUnlocks());
        assertEquals(100, perkNamed("taming", "Environmentally Aware").unlock(),
                "the longest perk name carries its verified unlock");
        assertEquals(1000, perkNamed("herbalism", "Verdant Bounty").unlock());
        assertEquals("SALVAGE_SCRAP_COLLECTOR",
                perkNamed("salvage", "Scrap Collector").subSkillEnum());
        assertNull(perkNamed("acrobatics", "Graceful Roll").subSkillEnum(),
                "Graceful Roll is a facet of Roll — no SubSkillType of its own");
    }

    private static PerkLadder.Perk perkNamed(String skillId, String name) {
        for (PerkLadder.Perk perk : PerkLadder.perksFor(skillId)) {
            if (perk.name().equals(name)) return perk;
        }
        throw new AssertionError("no perk " + name + " in " + skillId);
    }

    @Test
    void ladderEarnedStateFixtures() {
        // RankUtils.getRank semantics: highest rank whose unlock
        // is at or below the level; 0 = not yet earned.
        PerkLadder.Perk dodge = perkNamed("acrobatics", "Dodge");
        assertEquals(0, PerkLadder.rankAt(dodge, 0));
        assertFalse(PerkLadder.earnedAt(dodge, 0));
        assertEquals("\u2717 Dodge \u2014 Lv 1 \u00b7 Rank 0 of 1",
                PerkLadder.line(dodge, 0));
        assertEquals(1, PerkLadder.rankAt(dodge, 1));
        assertTrue(PerkLadder.earnedAt(dodge, 1));
        assertEquals("\u2713 Dodge \u2014 Lv 1 \u00b7 Rank 1 of 1",
                PerkLadder.line(dodge, 1));
        // multi-rank boundaries walk the full ladder
        PerkLadder.Perk concoctions = perkNamed("alchemy", "Concoctions");
        assertEquals(1, PerkLadder.rankAt(concoctions, 0),
                "a Rank_1 unlock of 0 is earned at level 0");
        assertEquals(3, PerkLadder.rankAt(concoctions, 349));
        assertEquals(4, PerkLadder.rankAt(concoctions, 350));
        assertEquals(8, PerkLadder.rankAt(concoctions, 1000));
        assertEquals(8, PerkLadder.rankAt(concoctions, 5000),
                "rank never exceeds the perk's rank count");
        // a level-1000-only perk at 999 vs 1000
        PerkLadder.Perk verdant = perkNamed("herbalism", "Verdant Bounty");
        assertFalse(PerkLadder.earnedAt(verdant, 999));
        assertTrue(PerkLadder.earnedAt(verdant, 1000));
    }

    @Test
    void ladderNoRankPerksAlwaysEarned() {
        // numRanks = 0: RankUtils returns -1 and
        // hasUnlockedSubskill is true at ANY level (the fixture
        // the spec's earned-state rule names explicitly).
        for (String[] fixture : new String[][]{
                {"acrobatics", "Roll"}, {"archery", "Daze"},
                {"herbalism", "Hylian Luck"}, {"herbalism", "Shroom Thumb"},
                {"smelting", "Second Smelt"}, {"unarmed", "Block Cracker"},
                {"acrobatics", "Graceful Roll"}}) {
            PerkLadder.Perk perk = perkNamed(fixture[0], fixture[1]);
            assertEquals(0, perk.numRanks(), fixture[1] + " has no ranks");
            assertEquals(-1, PerkLadder.rankAt(perk, 0),
                    fixture[1] + ": the -1 no-ranks sentinel");
            assertTrue(PerkLadder.earnedAt(perk, 0));
            assertEquals("\u2713 " + fixture[1] + " \u2014 always available",
                    PerkLadder.line(perk, 0));
        }
    }

    @Test
    void ladderChildSkillDerivedLevelFixture() {
        // Salvage's derived level is (Repair + Fishing) / 2 —
        // the provider passes that derived level in, and the
        // SAME rank rule applies with no special case (§4).
        int derived = (0 + 3) / 2; // Repair 0, Fishing 3 -> 1 (the rig fixture)
        List<String> salvage = PerkLadder.ladderLines("salvage", derived);
        assertEquals(List.of(
                "Perks:",
                "\u2713 Scrap Collector \u2014 Lv 1 \u00b7 Rank 1 of 8",
                "  Salvage materials from an item, a perfect salvage depends on skill and luck.",
                "\u2717 Arcane Salvage \u2014 Lv 100 \u00b7 Rank 0 of 8",
                "  Extract enchantments from items"), salvage);
        // Smelting derived from (Mining 25 + Repair 0) / 2 = 12
        List<String> smelting = PerkLadder.ladderLines("smelting", (25 + 0) / 2);
        assertEquals(List.of(
                "Perks:",
                "\u2713 Second Smelt \u2014 always available",
                "  Double the resources gained from smelting",
                "\u2717 Fuel Efficiency \u2014 Lv 100 \u00b7 Rank 0 of 3",
                "  Increase the burn time of fuel used in furnaces when smelting",
                "\u2717 Understanding the Art \u2014 Lv 100 \u00b7 Rank 0 of 8",
                "  Maybe you're spending a bit too much time smelting in the caves. Powers up various properties of Smelting."), smelting);
        // a higher derived level earns deeper ranks on the same rule
        assertTrue(PerkLadder.ladderLines("salvage", 100)
                        .contains("\u2713 Arcane Salvage \u2014 Lv 100 \u00b7 Rank 1 of 8"));
    }

    @Test
    void fluxMiningIsAbsentEntirely() {
        // D32: Flux Mining has no code path in this fork — it is
        // omitted entirely, never shown as earnable or at all.
        for (String skillId : PerkLadder.skillIds()) {
            for (PerkLadder.Perk perk : PerkLadder.perksFor(skillId)) {
                assertFalse(perk.name().contains("Flux"),
                        "no Flux perk in " + skillId + ": " + perk.name());
            }
            for (String line : PerkLadder.ladderLines(skillId, 1000)) {
                assertFalse(line.contains("Flux Mining"),
                        "Flux Mining never renders, even at level 1000: " + line);
            }
        }
        assertEquals(3, PerkLadder.perksFor("smelting").size(),
                "Smelting renders its 3 verified perks only");
    }

    @Test
    void ladderLiveVerificationGate() {
        // The provider's gate, proven pure: a live map copied
        // from the verified table matches; ANY drift fails.
        for (String skillId : PerkLadder.skillIds()) {
            java.util.Map<String, int[]> live = new java.util.HashMap<>();
            for (PerkLadder.Perk perk : PerkLadder.perksFor(skillId)) {
                if (perk.subSkillEnum() != null) {
                    live.put(perk.subSkillEnum(), perk.retroUnlocks());
                }
            }
            assertTrue(PerkLadder.matchesLive(skillId, live),
                    skillId + " matches its own verified values");
        }
        java.util.Map<String, int[]> drifted = new java.util.HashMap<>();
        for (PerkLadder.Perk perk : PerkLadder.perksFor("acrobatics")) {
            if (perk.subSkillEnum() != null) drifted.put(perk.subSkillEnum(), perk.retroUnlocks());
        }
        drifted.put("ACROBATICS_DODGE", new int[]{2});
        assertFalse(PerkLadder.matchesLive("acrobatics", drifted),
                "one drifted unlock fails the whole skill to the pointer");
        drifted.put("ACROBATICS_DODGE", new int[]{1});
        drifted.remove("ACROBATICS_ROLL");
        assertFalse(PerkLadder.matchesLive("acrobatics", drifted),
                "a missing live read is unverified, not a pass");
        assertFalse(PerkLadder.matchesLive("acrobatics", null));
        assertFalse(PerkLadder.matchesLive("nope",
                java.util.Map.of()), "an unknown skill never verifies");
    }

    @Test
    void ladderDetailComposition() {
        // A normal skill: the ladder REPLACES the pointer (D32 —
        // no skill keeps the pointer once its ladder verifies).
        PanelModel.SkillRow acro = SkillMath.mcmmoRow("acrobatics", "Acrobatics",
                "FEATHER", 1, 0, 100, null, PerkLadder.ladderLines("acrobatics", 1));
        assertFalse(acro.detailLines().contains(SkillMath.PERK_POINTER),
                "a verified ladder leaves no pointer line");
        assertEquals(List.of(
                "Perks:",
                "\u2713 Roll \u2014 always available",
                "  Land strategically to avoid damage.",
                "\u2713 Graceful Roll \u2014 always available",
                "  Twice as effective as a normal Roll",
                "\u2713 Dodge \u2014 Lv 1 \u00b7 Rank 1 of 1",
                "  Reduce attack damage by half"),
                acro.detailLines().subList(acro.detailLines().size() - 7,
                        acro.detailLines().size()));
        // The unverified shape keeps the pointer exactly (the
        // carried no-ladder builder IS the fail-soft shape).
        PanelModel.SkillRow unverified = SkillMath.mcmmoRow("acrobatics",
                "Acrobatics", "FEATHER", 1, 0, 100, null);
        assertEquals(SkillMath.PERK_POINTER,
                unverified.detailLines().get(unverified.detailLines().size() - 1));
        // A child skill: the derived-level explainer stays ABOVE
        // its ladder (D32), and no bar appears.
        PanelModel.SkillRow salvage = SkillMath.childRow("salvage", "Salvage",
                "GOLD_INGOT", 1, null, PerkLadder.ladderLines("salvage", 1));
        assertNull(salvage.bar());
        int explainer = salvage.detailLines().indexOf(
                "Child skill \u2014 its level comes from its parent skills.");
        int ladderHead = salvage.detailLines().indexOf("Perks:");
        assertTrue(explainer >= 0 && ladderHead > explainer,
                "child explainer above the ladder: " + salvage.detailLines());
        assertFalse(salvage.detailLines().contains(SkillMath.PERK_POINTER));
    }

    // ----------------- panel assembly -----------------

    @Test
    void panelBodyAndChatLines() {
        PanelModel.Panel panel = new PanelModel.Panel(
                List.of(SkillMath.powerHeader(100, 0),
                        MagicLines.headerBlock(fullMagic())),
                List.of(SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                        214, 1240, 2000, null)),
                MagicLines.detailLines(fullMagic()));
        String body = panel.bedrockBody();
        assertTrue(body.startsWith("Power Level 100\n\nMagic Level 12 \u2014 54,321 XP total\n"),
                "0.1.1: the Power Level header is ONE line leading the Bedrock body; "
                        + "0.2.1 (Item 9a): the Magic block follows under its Magic Level line, "
                        + "with no standalone Magic line");
        assertEquals(1, body.split("Power Level", -1).length - 1,
                "Power Level is named exactly once in the whole body (no cap line either)");
        assertFalse(List.of(body.split("\n")).contains("Magic"),
                "0.2.1 (Item 9a): no standalone Magic line anywhere in the Bedrock body");
        assertTrue(body.contains("Guild seals: 4 of 5"));
        assertTrue(body.endsWith("Mining 214 "
                        + "\u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77\u25B1\u25B1\u25B1\u25B1 62%"),
                "0.2.1 (Item 9b): the compact skill lines close the body, gold-filled");
        List<String> chat = panel.chatLines();
        assertEquals("Skills", chat.get(0));
        assertTrue(chat.contains("Mining 214 "
                + "\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1 62%"),
                "the chat fallback carries the same compact lines");
        assertTrue(panel.hasMagicDetail());
        assertNotNull(panel.header("magic"));
        assertNull(panel.header("nope"));
    }

    // ----------------- Bedrock form handler (real Cumulus) -----------------

    @Test
    void bedrockFormsHandlerShape() throws Exception {
        // Structural pin on the compiled BedrockForms: the NEW
        // Cumulus + Geyser-only path is what ships (the Summons
        // 0.1.8 shape); the dead 0.1.7 shape stays out.
        byte[] bytes;
        try (var in = PanelTest.class.getResourceAsStream(
                "/com/winkreyes/excelskillspanel/bukkit/BedrockForms.class")) {
            assertNotNull(in, "BedrockForms.class is on the test classpath");
            bytes = in.readAllBytes();
        }
        String pool = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pool.contains("org.geysermc.cumulus.form.SimpleForm"), "the NEW Cumulus SimpleForm");
        assertTrue(pool.contains("org.geysermc.cumulus.form.util.FormBuilder"), "the new FormBuilder");
        assertTrue(pool.contains("resultHandler"), "the handler attaches via FormBuilder.resultHandler");
        assertTrue(pool.contains("clickedButtonId"), "the index unwraps via SimpleFormResponse.clickedButtonId");
        assertTrue(pool.contains("connectionByUuid"), "the connection is looked up by player uuid");
        assertFalse(pool.contains("org.geysermc.cumulus.SimpleForm"),
                "the OLD Cumulus SimpleForm is gone");
        assertFalse(pool.contains("validResultHandler"), "the 0.1.7 handler hunt is gone");
        assertFalse(pool.contains("floodgate"), "the Floodgate attempt is gone — Geyser class loader only");
    }

    @Test
    void bedrockFormsHandlerDeliversEveryIndex() throws Exception {
        // Behavioral proof against the REAL Cumulus classes in
        // the staged Geyser jar (local-build.sh passes its path):
        // for each form shape the panel sends, a synthetic
        // response is driven through the handler
        // BedrockForms.buildForm attached — via
        // FormImpl.callResultHandler AND via the raw-payload
        // entry (SimpleFormDefinition.handleFormResponse). A
        // closed form must report -1.
        String geyserJar = System.getProperty("skillspanel.test.geyserJar");
        String guavaJar = System.getProperty("skillspanel.test.guavaJar");
        String gsonJar = System.getProperty("skillspanel.test.gsonJar");
        assertNotNull(geyserJar, "local-build.sh passes -Dskillspanel.test.geyserJar");
        java.net.URLClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{
                java.nio.file.Path.of(geyserJar).toUri().toURL(),
                java.nio.file.Path.of(guavaJar).toUri().toURL(),
                java.nio.file.Path.of(gsonJar).toUri().toURL()},
                PanelTest.class.getClassLoader());
        Class<?> formImpl = Class.forName("org.geysermc.cumulus.form.impl.FormImpl", true, l);
        Class<?> resultT = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, l);
        Class<?> validT = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, l);
        Class<?> respImpl = Class.forName("org.geysermc.cumulus.response.impl.SimpleFormResponseImpl", true, l);
        Class<?> buttonT = Class.forName("org.geysermc.cumulus.component.ButtonComponent", true, l);
        Class<?> responseT = Class.forName("org.geysermc.cumulus.response.FormResponse", true, l);
        java.lang.reflect.Method respOf = respImpl.getMethod("of", int.class, buttonT);
        java.lang.reflect.Method validOf = validT.getMethod("of", responseT);
        java.lang.reflect.Method callHandler = formImpl.getMethod("callResultHandler", resultT);
        java.lang.reflect.Method closedOf = resultT.getMethod("closed");
        Object definition = Class.forName("org.geysermc.cumulus.form.impl.simple.SimpleFormDefinition", true, l)
                .getMethod("instance").invoke(null);
        java.lang.reflect.Method handleRaw = definition.getClass().getMethod("handleFormResponse",
                Class.forName("org.geysermc.cumulus.form.Form", true, l), String.class);
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("skillspanel-test");
        java.util.Map<String, List<String>> shapes = new java.util.LinkedHashMap<>();
        shapes.put("panel", List.of("Magic \u2014 details", "Mining \u2014 Lv 214",
                "Fishing \u2014 Lv 3", "Close"));
        shapes.put("skill-detail", List.of("Back to Skills"));
        shapes.put("magic-detail", List.of("Back to Skills"));
        for (var shape : shapes.entrySet()) {
            java.util.concurrent.atomic.AtomicInteger got =
                    new java.util.concurrent.atomic.AtomicInteger(Integer.MIN_VALUE);
            Object form = BedrockForms.buildForm(l, "Skills", "body",
                    shape.getValue(), got::set, log);
            List<?> buttons = (List<?>) form.getClass().getMethod("buttons").invoke(form);
            assertEquals(shape.getValue().size(), buttons.size(),
                    shape.getKey() + " form carries its buttons");
            for (int i = 0; i < buttons.size(); i++) {
                got.set(Integer.MIN_VALUE);
                callHandler.invoke(form, validOf.invoke(null, respOf.invoke(null, i, buttons.get(i))));
                assertEquals(i, got.get(),
                        shape.getKey() + " button " + i + " reaches the callback via callResultHandler");
                got.set(Integer.MIN_VALUE);
                handleRaw.invoke(definition, form, String.valueOf(i));
                assertEquals(i, got.get(),
                        shape.getKey() + " button " + i + " reaches the callback via the raw payload path");
            }
            got.set(Integer.MIN_VALUE);
            callHandler.invoke(form, closedOf.invoke(null));
            assertEquals(-1, got.get(), shape.getKey() + ": a closed form reports -1");
            got.set(Integer.MIN_VALUE);
            handleRaw.invoke(definition, form, "null");
            assertEquals(-1, got.get(), shape.getKey() + ": the client's close payload reports -1");
        }
    }

    // ----------------- Bedrock percent sign (0.1.1) -----------------

    @Test
    void bedrockPercentSafeRendering() {
        // The MODEL keeps the ASCII sign — the Java chest lore and
        // the chat fallback render it directly and must not change.
        PanelModel.BarData bar = new PanelModel.BarData(1240, 2000);
        assertTrue(bar.compact().endsWith("62%"), "model compact keeps ASCII %: " + bar.compact());
        assertTrue(bar.line().contains("62% \u2014"), "model line keeps ASCII %: " + bar.line());
        // The form-bound rendering swaps U+0025 for U+FF05 only.
        String safe = PanelModel.bedrockSafe(bar.line());
        assertFalse(safe.contains("%"), "no ASCII % survives into a form body: " + safe);
        assertTrue(safe.contains("62\uFF05 \u2014 1,240 / 2,000 XP"),
                "the value + fullwidth sign survive: " + safe);
        assertEquals(safe, PanelModel.bedrockSafe(safe), "bedrockSafe is idempotent");
        assertNull(PanelModel.bedrockSafe(null), "null stays null");
        assertEquals("no percent here", PanelModel.bedrockSafe("no percent here"),
                "text without a percent is untouched");
    }

    @Test
    void bedrockFormContentRetainsPercentSigns() throws Exception {
        // Root-cause isolation against the REAL Cumulus classes:
        // a form built through BedrockForms.buildForm RETAINS an
        // ASCII "%" in its content exactly — the plugin -> Cumulus
        // leg does NOT eat the glyph (Mike's live loss is
        // downstream, in the Bedrock client's form rendering).
        // The 0.1.1 fullwidth sign is likewise retained exactly,
        // so the form the client receives carries a percent sign
        // its renderer passes through.
        String geyserJar = System.getProperty("skillspanel.test.geyserJar");
        String guavaJar = System.getProperty("skillspanel.test.guavaJar");
        String gsonJar = System.getProperty("skillspanel.test.gsonJar");
        assertNotNull(geyserJar, "local-build.sh passes -Dskillspanel.test.geyserJar");
        java.net.URLClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{
                java.nio.file.Path.of(geyserJar).toUri().toURL(),
                java.nio.file.Path.of(guavaJar).toUri().toURL(),
                java.nio.file.Path.of(gsonJar).toUri().toURL()},
                PanelTest.class.getClassLoader());
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("skillspanel-test");
        java.util.function.IntConsumer noop = i -> {};
        Object ascii = BedrockForms.buildForm(l, "Skills", "Bar 62% \u2014 done",
                List.of("Close"), noop, log);
        Object content = ascii.getClass().getMethod("content").invoke(ascii);
        assertEquals("Bar 62% \u2014 done", content,
                "Cumulus retains the ASCII % — the loss is NOT in the plugin or Cumulus");
        String safeBody = PanelModel.bedrockSafe("Bar 62% \u2014 done");
        Object safe = BedrockForms.buildForm(l, "Skills", safeBody,
                List.of("Close"), noop, log);
        assertEquals(safeBody, safe.getClass().getMethod("content").invoke(safe),
                "Cumulus retains the fullwidth sign the 0.1.1 forms actually send");
    }

    // ----------------- 0.2.1 Item 9a: no standalone Magic line -----------------

    @Test
    void magicStandaloneLineAbsentBothSurfaces() {
        // Bedrock body + chat fallback: neither carries a line
        // that is exactly "Magic"; the Magic Level line titles
        // the block and appears exactly once.
        PanelModel.Panel panel = new PanelModel.Panel(
                List.of(SkillMath.powerHeader(100, 0),
                        MagicLines.headerBlock(fullMagic())),
                List.of(SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                        214, 1240, 2000, null)),
                MagicLines.detailLines(fullMagic()));
        for (String line : panel.bedrockBody().split("\n")) {
            assertFalse(line.equals("Magic"),
                    "Bedrock body carries no standalone Magic line: " + line);
        }
        for (String line : panel.chatLines()) {
            assertFalse(line.equals("Magic"),
                    "chat fallback carries no standalone Magic line: " + line);
        }
        assertEquals(1, panel.bedrockBody().split("Magic Level 12", -1).length - 1,
                "the Magic Level line appears exactly once in the Bedrock body");
        assertEquals(1, panel.chatLines().stream()
                        .filter(l -> l.startsWith("Magic Level 12")).count(),
                "the Magic Level line appears exactly once in the chat fallback");
        // A single-line Magic read (level only) still titles the
        // block with that line and carries no extra lines.
        PanelModel.HeaderBlock levelOnly = MagicLines.headerBlock(new MagicLines.MagicData(
                3, 10L, 90L, 250L, null, null, null, null, null, null,
                null, null, null, null, null));
        assertNotNull(levelOnly);
        assertEquals("Magic Level 3 \u2014 250 XP total", levelOnly.title());
        assertTrue(levelOnly.lines().isEmpty(),
                "a one-line header is title-only, like the Power Level block");
    }

    // ----------------- 0.2.1 Item 9b: gold filled bars -----------------

    @Test
    void goldFilledBarsModelAndStringSurfaces() {
        PanelModel.BarData half = new PanelModel.BarData(1000, 2000);
        assertEquals(5, half.filledCount());
        assertEquals("\u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77\u25B1\u25B1\u25B1\u25B1\u25B1",
                half.coloredBar(), "filled half gold, empty half gray");
        assertEquals("\u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77\u25B1\u25B1\u25B1\u25B1\u25B1 50%",
                half.compactColored());
        assertEquals("\u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77\u25B1\u25B1\u25B1\u25B1\u25B1 50% \u2014 1,000 / 2,000 XP",
                half.lineColored());
        // the plain model strings are unchanged (Java/chat source)
        assertEquals("\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1\u25B1", half.bar());
        // empty and full edges: codes still balanced, nothing lost
        assertEquals("\u00a76\u00a77\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1\u25B1",
                new PanelModel.BarData(0, 2000).coloredBar());
        PanelModel.BarData capped = new PanelModel.BarData(500, 0);
        assertEquals("\u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77",
                capped.coloredBar(), "a capped (full) bar is all gold, reset after");
        assertTrue(capped.lineColored().endsWith(" MAX"));
        // colorizeBar: the detail-line form (prefix text kept plain)
        assertEquals("Magic XP: \u00a76\u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u00a77\u25B1\u25B1\u25B1\u25B1 62% \u2014 1,240 / 2,000 XP",
                PanelModel.colorizeBar("Magic XP: \u25B0\u25B0\u25B0\u25B0\u25B0\u25B0\u25B1\u25B1\u25B1\u25B1 62% \u2014 1,240 / 2,000 XP"));
        assertEquals("no bar here", PanelModel.colorizeBar("no bar here"),
                "lines without a bar pass through unchanged");
        assertNull(PanelModel.colorizeBar(null));
        // the Bedrock panel body carries the gold codes end-to-end
        PanelModel.Panel panel = new PanelModel.Panel(
                List.of(MagicLines.headerBlock(fullMagic())),
                List.of(SkillMath.mcmmoRow("mining", "Mining", "IRON_PICKAXE",
                        214, 1240, 2000, null)),
                MagicLines.detailLines(fullMagic()));
        assertTrue(panel.bedrockBody().contains("\u00a76\u25B0"),
                "the Bedrock body renders filled segments gold: " + panel.bedrockBody());
        assertTrue(panel.chatLines().stream().noneMatch(l -> l.contains("\u00a76")),
                "the chat fallback keeps plain bars (no legacy codes leak)");
    }

    // ----------------- 0.2.1 Item 10: perk descriptions -----------------

    @Test
    void perkDescriptionsCensusAndSpotValues() {
        // Full sweep: EVERY one of the 82 perks carries a
        // verifiable fork-locale description (DESCRIPTIONS-SOURCE.md).
        int withDesc = 0;
        for (String skillId : PerkLadder.skillIds()) {
            for (PerkLadder.Perk perk : PerkLadder.perksFor(skillId)) {
                assertTrue(perk.hasDescription(),
                        skillId + " / " + perk.name() + " carries its locale description");
                withDesc++;
            }
        }
        assertEquals(82, withDesc, "all 82 perks render a description");
        // Spot values are the fork locale's own words, verbatim.
        assertEquals("Bone-whacking inspects wolves & ocelots",
                perkNamed("taming", "Beast Lore").description());
        assertEquals("Summon an animal to your side",
                perkNamed("taming", "Call of the Wild").description());
        assertEquals("Land strategically to avoid damage.",
                perkNamed("acrobatics", "Roll").description());
        assertEquals("Brew potions with more ingredients",
                perkNamed("alchemy", "Concoctions").description());
        assertEquals("Richochet arrows with steep angles",
                perkNamed("crossbows", "Trick Shot").description(),
                "the fork's own spelling is kept verbatim, typo included");
        assertEquals("Maybe you're spending a bit too much time smelting in the caves. Powers up various properties of Smelting.",
                perkNamed("smelting", "Understanding the Art").description(),
                "the properties \\n escape renders as a space (whitespace only)");
        assertFalse(perkNamed("smelting", "Understanding the Art").description().contains("\\n"),
                "no raw escape sequence reaches a panel line");
    }

    @Test
    void perkDescriptionRenderingAndFallback() {
        // Taming at Mike's acceptance level (15): each perk's
        // description sits directly under its name/unlock line.
        List<String> taming = PerkLadder.ladderLines("taming", 15);
        int beast = taming.indexOf("\u2713 Beast Lore \u2014 Lv 1 \u00b7 Rank 1 of 1");
        assertTrue(beast >= 0);
        assertEquals("  Bone-whacking inspects wolves & ocelots", taming.get(beast + 1),
                "Beast Lore's description renders under its line");
        int cotw = taming.indexOf("\u2713 Call of the Wild \u2014 Lv 1 \u00b7 Rank 1 of 1");
        assertTrue(cotw >= 0);
        assertEquals("  Summon an animal to your side", taming.get(cotw + 1),
                "Call of the Wild's description renders under its line");
        assertEquals(1 + 10 * 2, taming.size(),
                "Taming renders Perks: + 10 perk lines + 10 description lines");
        // Every skill: one description line per perk, in order.
        for (String skillId : PerkLadder.skillIds()) {
            List<String> lines = PerkLadder.ladderLines(skillId, 0);
            assertEquals(1 + PerkLadder.perksFor(skillId).size() * 2, lines.size(),
                    skillId + " renders a description under every perk");
        }
        // FALLBACK (Item 10 data rule): a perk with NO verifiable
        // description keeps name+unlock only \u2014 no invented line.
        PerkLadder.Perk noDesc = new PerkLadder.Perk(
                "Mystery Perk", "TEST_MYSTERY", new int[]{5}, null);
        assertFalse(noDesc.hasDescription());
        assertNull(PerkLadder.descriptionLine(noDesc));
        assertEquals(List.of("\u2717 Mystery Perk \u2014 Lv 5 \u00b7 Rank 0 of 1"),
                PerkLadder.linesFor(List.of(noDesc), 0),
                "a description-less perk renders its name/unlock line alone");
        PerkLadder.Perk blankDesc = new PerkLadder.Perk(
                "Blank Perk", "TEST_BLANK", new int[]{5}, "  ");
        assertEquals(List.of("\u2717 Blank Perk \u2014 Lv 5 \u00b7 Rank 0 of 1"),
                PerkLadder.linesFor(List.of(blankDesc), 0),
                "a blank description is treated as no description");
        PerkLadder.Perk withDesc = new PerkLadder.Perk(
                "Known Perk", "TEST_KNOWN", new int[]{5}, "Does a known thing");
        assertEquals(List.of("\u2717 Known Perk \u2014 Lv 5 \u00b7 Rank 0 of 1",
                        "  Does a known thing"),
                PerkLadder.linesFor(List.of(withDesc), 0),
                "a described perk renders name/unlock then its indented description");
    }
}
