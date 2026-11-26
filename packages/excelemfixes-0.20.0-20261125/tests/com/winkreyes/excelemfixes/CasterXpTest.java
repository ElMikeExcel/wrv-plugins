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
 * Caster-xp-sharing: the mode rules (parse falls back to
 * STOCK on anything unknown — never intervene on a value we
 * do not understand), the SHARED split (ceiling half stays,
 * floor half moves, total preserved), the CARRY mirror (only
 * the gain's contribution to a positive gap mirrors), the
 * one-time catch-up plan (Jester's live numbers: WANDS
 * 240,760 vs STAVES 41,791 -> STAVES +198,969), the per-player
 * state store (stored choice beats the configured default,
 * the catch-up record round-trips and fires exactly once),
 * the sharing screen (three mode buttons, current marked,
 * Back chrome), and the skills-menu row (present on Wands
 * and Staves only while the module's flag is up, absent
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

        // --------------------------------------------------------- view
        CasterXpView.Snapshot snap = new CasterXpView.Snapshot(
                CasterXpMath.Mode.SHARED, 240_760, 18, 41_791, 12, false);
        Views.View view = CasterXpView.view(snap);
        Check.eq("Caster XP Sharing", view.title(), "view: title");
        Check.eq(3, content(view).size(), "view: three mode buttons");
        Check.ok(view.buttons().stream().filter(Views.Button::nav).count() == 1,
                "view: one nav button (Back)");
        Check.ok(view.intro().contains("240,760"),
                "view: intro carries the Wands XP (Bedrock rule)");
        Check.ok(view.intro().contains("41,791"),
                "view: intro carries the Staves XP (Bedrock rule)");
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
            Check.ok(!hasAction(SkillsMenuModel.skillView(swords),
                            "casterxp:open"),
                    "skills row: never on a non-caster screen");
        } finally {
            SkillsMenuModel.setCasterSharingRowVisible(false);
        }
        Check.ok(!hasAction(SkillsMenuModel.skillView(wands),
                        "casterxp:open"),
                "skills row: flag reset leaves screens as before");
    }
}
