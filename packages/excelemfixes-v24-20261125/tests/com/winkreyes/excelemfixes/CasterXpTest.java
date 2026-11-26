import excelemfixes.casterxp.CasterXpMath;
import excelemfixes.casterxp.CasterXpState;
import excelemfixes.casterxp.CasterXpView;
import excelemfixes.shared.Views;
import excelemfixes.skillsmenu.SkillsMenuModel;
import excelemfixes.skillsmenu.SkillsMenuModel.SkillState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class-xp-sharing (module id caster-xp-sharing): the mode
 * rules (parse falls back to STOCK on anything unknown —
 * never intervene on a value we do not understand), the
 * SHARED split (ceiling half stays, floor half moves, total
 * preserved), the CARRY mirror (only the gain's contribution
 * to a positive gap mirrors), the one-time catch-up plan
 * (Jester's live numbers: WANDS 240,760 vs STAVES 41,791 ->
 * STAVES +198,969; Marvin's: ARMOR 24,853 vs SWORDS 54,766
 * -> ARMOR +29,913), the per-player state store (stored
 * choice beats the configured default; catch-ups are
 * per-TREE since 0.21.0 and fire exactly once per tree),
 * the 0.20.0 -> 0.21.0 state migration (a fired
 * spellcaster catch-up never refires; an owed one stays
 * owed; the legacy flat fields mirror back on save), the
 * sharing screen (three mode buttons, current marked, Back
 * chrome), and the skills-menu row (on EVERY skill page
 * since 0.21.0 while the module's flag is up, absent
 * otherwise — existing screens unchanged by default).
 */
public final class CasterXpTest {

    private static List<Views.Button> content(Views.View v) {
        return v.buttons().stream().filter(b -> !b.nav()).toList();
    }

    private static boolean hasAction(Views.View v, String action) {
        return v.buttons().stream().anyMatch(b -> action.equals(b.action()));
    }

    public static void run() {
        System.out.println("CasterXpTest");

        // -------------------------------------------------- mode parse
        Check.eq(CasterXpMath.Mode.STOCK,
                CasterXpMath.Mode.parse("STOCK"), "parse STOCK");
        Check.eq(CasterXpMath.Mode.SHARED,
                CasterXpMath.Mode.parse("shared"), "parse shared (case-free)");
        Check.eq(CasterXpMath.Mode.CARRY,
                CasterXpMath.Mode.parse(" Carry "), "parse carry (trimmed)");
        Check.eq(CasterXpMath.Mode.STOCK,
                CasterXpMath.Mode.parse("bogus"), "parse unknown -> STOCK");
        Check.eq(CasterXpMath.Mode.STOCK,
                CasterXpMath.Mode.parse(null), "parse null -> STOCK");
        Check.ok(CasterXpMath.isCasterSkill("WANDS"), "WANDS is a caster skill");
        Check.ok(CasterXpMath.isCasterSkill("STAVES"), "STAVES is a caster skill");
        Check.ok(!CasterXpMath.isCasterSkill("SWORDS"), "SWORDS is not");
        Check.eq("STAVES", CasterXpMath.otherSkill("WANDS"), "other of WANDS");
        Check.eq("WANDS", CasterXpMath.otherSkill("STAVES"), "other of STAVES");
        Check.eq("SWORDS", CasterXpMath.otherInPair("ARMOR", "SWORDS", "ARMOR"),
                "pair: other of ARMOR in the paladin pair");
        Check.eq("ARMOR", CasterXpMath.otherInPair("ARMOR", "SWORDS", "SWORDS"),
                "pair: other of SWORDS in the paladin pair");
        Check.eq(null, CasterXpMath.otherInPair("ARMOR", "SWORDS", "WANDS"),
                "pair: a foreign skill has no other in this pair");

        // ------------------------------------------------ SHARED split
        long[] s = CasterXpMath.splitGain(100);
        Check.eq(50L, s[0], "split 100: kept 50");
        Check.eq(50L, s[1], "split 100: moved 50");
        s = CasterXpMath.splitGain(101);
        Check.eq(51L, s[0], "split 101: kept ceiling 51");
        Check.eq(50L, s[1], "split 101: moved floor 50");
        Check.eq(101L, s[0] + s[1], "split preserves the total");
        s = CasterXpMath.splitGain(1);
        Check.eq(1L, s[0], "split 1: kept 1 (never halved into nothing)");
        Check.eq(0L, s[1], "split 1: moved 0");
        s = CasterXpMath.splitGain(0);
        Check.eq(0L, s[0] + s[1], "split 0: nothing");
        s = CasterXpMath.splitGain(-5);
        Check.eq(0L, s[0] + s[1], "split negative: nothing");

        // ------------------------------------------------- CARRY mirror
        Check.eq(10L, CasterXpMath.carryMirror(1000, 900, 10),
                "carry: gain on the leading track mirrors in full");
        Check.eq(0L, CasterXpMath.carryMirror(900, 1000, 10),
                "carry: gain on the lagging track mirrors nothing");
        Check.eq(0L, CasterXpMath.carryMirror(900, 1000, 100),
                "carry: lagging gain that only reaches a tie mirrors nothing");
        Check.eq(5L, CasterXpMath.carryMirror(995, 1000, 10),
                "carry: lagging gain overshooting by 5 mirrors the overshoot");
        Check.eq(10L, CasterXpMath.carryMirror(1000, 1000, 10),
                "carry: gain from a tie mirrors in full (tracks stay tied)");
        Check.eq(0L, CasterXpMath.carryMirror(1000, 900, 0),
                "carry: no gain, no mirror");
        Check.eq(0L, CasterXpMath.carryMirror(1000, 900, -3),
                "carry: negative gain, no mirror");

        // ---------------------------------------------------- catch-up
        CasterXpMath.CatchUp cu = CasterXpMath.catchUp(240_760, 41_791);
        Check.eq("STAVES", cu.lagging(), "catch-up: Jester case lags STAVES");
        Check.eq(198_969L, cu.amount(), "catch-up: Jester case amount 198,969");
        cu = CasterXpMath.catchUp(10, 250);
        Check.eq("WANDS", cu.lagging(), "catch-up: staves ahead lags WANDS");
        Check.eq(240L, cu.amount(), "catch-up: staves ahead amount");
        cu = CasterXpMath.catchUp(500, 500);
        Check.eq(null, cu.lagging(), "catch-up: equal tracks lag nothing");
        Check.eq(0L, cu.amount(), "catch-up: equal tracks amount 0");
        cu = CasterXpMath.catchUp(0, 0);
        Check.eq(0L, cu.amount(), "catch-up: fresh player amount 0");
        // Marvin's live paladin numbers (audit, Nov 25 2026):
        // ARMOR 24,853 (Lv 10) vs SWORDS 54,766 (Lv 13).
        cu = CasterXpMath.catchUp(24_853, 54_766, "ARMOR", "SWORDS");
        Check.eq("ARMOR", cu.lagging(), "catch-up: Marvin case lags ARMOR");
        Check.eq(29_913L, cu.amount(), "catch-up: Marvin case amount 29,913");
        cu = CasterXpMath.catchUp(54_766, 24_853, "SWORDS", "ARMOR");
        Check.eq("ARMOR", cu.lagging(),
                "catch-up: pair order does not change the lagging track");

        // -------------------------------------------------------- state
        CasterXpState state = new CasterXpState();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Check.eq(CasterXpMath.Mode.STOCK, state.modeOf(a),
                "state: unrecorded player gets the STOCK default");
        state.setDefaultMode(CasterXpMath.Mode.SHARED);
        Check.eq(CasterXpMath.Mode.SHARED, state.modeOf(a),
                "state: unrecorded player gets the configured default");
        state.choose(a, CasterXpMath.Mode.CARRY);
        Check.eq(CasterXpMath.Mode.CARRY, state.modeOf(a),
                "state: stored choice beats the configured default");
        Check.eq(CasterXpMath.Mode.SHARED, state.modeOf(b),
                "state: other players still on the default");
        Check.ok(!state.record(a).catchUpDone,
                "state: catch-up not done before it fires");
        state.recordCatchUp(a, "2026-11-25T22:50:00Z", "STAVES",
                198_969, 240_760, 41_791);
        Check.ok(state.record(a).catchUpDone,
                "state: catch-up recorded done");
        Check.eq(198_969L, state.record(a).catchUpAmount,
                "state: catch-up amount recorded");
        Check.eq("STAVES", state.record(a).catchUpTrack,
                "state: catch-up track recorded");
        Check.eq(240_760L, state.record(a).catchUpWandsBefore,
                "state: catch-up wands-before recorded");

        Map<String, Object> map = state.toMap();
        CasterXpState loaded = CasterXpState.fromMap(map);
        Check.eq(CasterXpMath.Mode.CARRY, loaded.modeOf(a),
                "state: mode survives the map round-trip");
        Check.ok(loaded.record(a).catchUpDone,
                "state: catch-up-done survives the round-trip (fires once)");
        Check.eq(198_969L, loaded.record(a).catchUpAmount,
                "state: catch-up amount survives the round-trip");

        Map<String, Object> messy = new HashMap<>(map);
        messy.put("not-a-uuid", Map.of("mode", "CARRY"));
        messy.put(UUID.randomUUID().toString(), "garbage-row");
        CasterXpState tolerant = CasterXpState.fromMap(messy);
        Check.eq(CasterXpMath.Mode.CARRY, tolerant.modeOf(a),
                "state: malformed rows are skipped, good rows still load");
        Check.eq(1, tolerant.size(),
                "state: only the well-formed row loaded");

        // --------------------------------------- per-tree catch-ups
        CasterXpState trees = new CasterXpState();
        UUID p = UUID.randomUUID();
        Check.ok(!trees.catchUpDone(p, "paladin"),
                "trees: nothing consumed before any catch-up");
        CasterXpState.CatchUpRecord pal = new CasterXpState.CatchUpRecord();
        pal.at = "2026-11-25T23:00:00Z";
        pal.form = "paladin";
        pal.track = "ARMOR";
        pal.amount = 29_913;
        pal.skillA = "ARMOR";
        pal.skillB = "SWORDS";
        pal.beforeA = 24_853;
        pal.beforeB = 54_766;
        trees.recordCatchUp(p, "paladin", pal);
        Check.ok(trees.catchUpDone(p, "paladin"),
                "trees: paladin catch-up consumed");
        Check.ok(!trees.catchUpDone(p, "spellcaster"),
                "trees: paladin's catch-up does not consume spellcaster's");
        Check.ok(!trees.catchUpDone(p, "berserker"),
                "trees: paladin's catch-up does not consume berserker's");
        Check.eq(29_913L, trees.catchUpRecord(p, "paladin").amount,
                "trees: paladin record keeps its amount");
        Check.eq("paladin", trees.catchUpRecord(p, "paladin").form,
                "trees: paladin record keeps its form");
        CasterXpState treesLoaded = CasterXpState.fromMap(trees.toMap());
        Check.ok(treesLoaded.catchUpDone(p, "paladin"),
                "trees: per-tree record survives the round-trip");
        Check.eq(24_853L, treesLoaded.catchUpRecord(p, "paladin").beforeA,
                "trees: before-totals survive the round-trip");
        Check.ok(!treesLoaded.record(p).catchUpDone,
                "trees: a non-spellcaster record does not light the legacy flag");

        // ----------------------------------- 0.20.0 -> 0.21.0 migration
        UUID jester = UUID.randomUUID();
        Map<String, Object> legacyFired = new HashMap<>();
        legacyFired.put("mode", "SHARED");
        legacyFired.put("catch-up-done", true);
        legacyFired.put("catch-up-at", "2026-11-25T22:50:00Z");
        legacyFired.put("catch-up-track", "STAVES");
        legacyFired.put("catch-up-amount", 162_150L);
        legacyFired.put("catch-up-wands-before", 199_173L);
        legacyFired.put("catch-up-staves-before", 37_023L);
        CasterXpState migrated = CasterXpState.fromMap(
                Map.of(jester.toString(), legacyFired));
        Check.eq(CasterXpMath.Mode.SHARED, migrated.modeOf(jester),
                "migration: the 0.20.0 mode carries over");
        Check.ok(migrated.catchUpDone(jester, "spellcaster"),
                "migration: a fired 0.20.0 catch-up is consumed (never refires)");
        Check.eq(162_150L,
                migrated.catchUpRecord(jester, "spellcaster").amount,
                "migration: the fired amount is preserved");
        Check.eq("STAVES",
                migrated.catchUpRecord(jester, "spellcaster").track,
                "migration: the fired track is preserved");
        Check.eq(199_173L,
                migrated.catchUpRecord(jester, "spellcaster").beforeA,
                "migration: wands-before lands in the pair's before-a");
        Check.ok(migrated.record(jester).catchUpDone,
                "migration: the legacy mirror reads done after load");
        CasterXpState remigrated = CasterXpState.fromMap(migrated.toMap());
        Check.ok(remigrated.catchUpDone(jester, "spellcaster"),
                "migration: save + reload keeps the spellcaster record");
        Check.eq(162_150L,
                remigrated.catchUpRecord(jester, "spellcaster").amount,
                "migration: save + reload keeps the fired amount");

        UUID owed = UUID.randomUUID();
        Map<String, Object> legacyOwed = new HashMap<>();
        legacyOwed.put("mode", "CARRY");
        legacyOwed.put("catch-up-done", false);
        legacyOwed.put("catch-up-at", "");
        legacyOwed.put("catch-up-track", "");
        legacyOwed.put("catch-up-amount", 0L);
        legacyOwed.put("catch-up-wands-before", -1L);
        legacyOwed.put("catch-up-staves-before", -1L);
        CasterXpState migratedOwed = CasterXpState.fromMap(
                Map.of(owed.toString(), legacyOwed));
        Check.eq(CasterXpMath.Mode.CARRY, migratedOwed.modeOf(owed),
                "migration: an owed row keeps its mode");
        Check.ok(!migratedOwed.catchUpDone(owed, "spellcaster"),
                "migration: an unfired 0.20.0 catch-up stays owed");

        // A catch-up recorded in the catalog's STAVES-first
        // pair order still mirrors its before totals into
        // the legacy WANDS/STAVES fields by skill name, so
        // a 0.20.0 rollback shows the full audit line.
        CasterXpState stavesFirst = new CasterXpState();
        UUID sfPlayer = UUID.randomUUID();
        CasterXpState.CatchUpRecord sfRec =
                new CasterXpState.CatchUpRecord();
        sfRec.at = "2026-11-26T01:07:21Z";
        sfRec.form = "spellcaster";
        sfRec.track = "STAVES";
        sfRec.amount = 161_949;
        sfRec.skillA = "STAVES";
        sfRec.skillB = "WANDS";
        sfRec.beforeA = 37_023;
        sfRec.beforeB = 198_972;
        stavesFirst.recordCatchUp(sfPlayer, "spellcaster", sfRec);
        Check.eq(198_972L,
                stavesFirst.record(sfPlayer).catchUpWandsBefore,
                "mirror: STAVES-first record maps wands-before by name");
        Check.eq(37_023L,
                stavesFirst.record(sfPlayer).catchUpStavesBefore,
                "mirror: STAVES-first record maps staves-before by name");

        // --------------------------------------------------------- view
        CasterXpView.Snapshot snap = new CasterXpView.Snapshot(
                CasterXpMath.Mode.SHARED, "Spellcaster",
                "WANDS", 240_760, 18, "STAVES", 41_791, 12, false);
        Views.View view = CasterXpView.view(snap);
        Check.eq("Class XP Sharing", view.title(), "view: title");
        Check.eq(3, content(view).size(), "view: three mode buttons");
        Check.ok(view.buttons().stream().filter(Views.Button::nav).count() == 1,
                "view: one nav button (Back)");
        Check.ok(view.intro().contains("Spellcaster"),
                "view: intro names the active form");
        Check.ok(view.intro().contains("240,760"),
                "view: intro carries the Wands XP (Bedrock rule)");
        Check.ok(view.intro().contains("41,791"),
                "view: intro carries the Staves XP (Bedrock rule)");
        CasterXpView.Snapshot palSnap = new CasterXpView.Snapshot(
                CasterXpMath.Mode.CARRY, "Paladin",
                "ARMOR", 24_853, 10, "SWORDS", 54_766, 13, false);
        Views.View palView = CasterXpView.view(palSnap);
        Check.ok(palView.intro().contains("Paladin")
                        && palView.intro().contains("24,853")
                        && palView.intro().contains("54,766"),
                "view: a paladin snapshot shows the paladin pair");
        CasterXpView.Snapshot noForm = new CasterXpView.Snapshot(
                CasterXpMath.Mode.STOCK, null, "", 0, 0, "", 0, 0, false);
        Check.ok(CasterXpView.view(noForm).intro().contains("No class"),
                "view: no form selected says so instead of showing zeros");
        Views.Button current = content(view).stream()
                .filter(bt -> bt.name().contains("[ON]")).findFirst()
                .orElseThrow();
        Check.ok(current.name().contains("Shared pool"),
                "view: the current mode is the marked one");
        Check.eq("casterxp:set:STOCK",
                content(view).get(0).action(), "view: stock button action");
        Check.eq(CasterXpMath.Mode.CARRY,
                CasterXpView.parseSet("casterxp:set:CARRY"),
                "view: parseSet CARRY");
        Check.eq(null, CasterXpView.parseSet("casterxp:set:BOGUS"),
                "view: parseSet unknown -> null");
        Check.eq(null, CasterXpView.parseSet("casterxp:open"),
                "view: parseSet open -> null");
        Check.ok(CasterXpView.isOpen("casterxp:open"), "view: isOpen");
        Check.ok(!CasterXpView.isOpen("casterxp:set:STOCK"),
                "view: isOpen rejects a set action");
        Check.eq("240,760", CasterXpView.formatXp(240_760),
                "view: XP formatting with separators");
        Check.eq("999", CasterXpView.formatXp(999),
                "view: XP formatting below a thousand");

        // -------------------------------------------- skills-menu row
        SkillState wands = new SkillState("WANDS", "Wands", 18, 240_760,
                3, List.of());
        SkillState staves = new SkillState("STAVES", "Staves", 12, 41_791,
                3, List.of());
        SkillState swords = new SkillState("SWORDS", "Swords", 9, 17_740,
                3, List.of());
        try {
            SkillsMenuModel.setCasterSharingRowVisible(false);
            Check.ok(!hasAction(SkillsMenuModel.skillView(wands),
                            "casterxp:open"),
                    "skills row: hidden while the module flag is down");
            SkillsMenuModel.setCasterSharingRowVisible(true);
            Check.ok(hasAction(SkillsMenuModel.skillView(wands),
                            "casterxp:open"),
                    "skills row: shown on the Wands screen");
            Check.ok(hasAction(SkillsMenuModel.skillView(staves),
                            "casterxp:open"),
                    "skills row: shown on the Staves screen");
            Check.ok(hasAction(SkillsMenuModel.skillView(swords),
                            "casterxp:open"),
                    "skills row: shown on EVERY screen since 0.21.0"
                            + " (the setting is global; the pair follows"
                            + " the selected form)");
            Views.Button row = SkillsMenuModel.skillView(swords).buttons()
                    .stream().filter(bt -> "casterxp:open".equals(bt.action()))
                    .findFirst().orElseThrow();
            Check.ok(row.name().contains("Class XP Sharing"),
                    "skills row: labeled Class XP Sharing");
        } finally {
            SkillsMenuModel.setCasterSharingRowVisible(false);
        }
        Check.ok(!hasAction(SkillsMenuModel.skillView(wands),
                        "casterxp:open"),
                "skills row: flag reset leaves screens as before");
    }
}
