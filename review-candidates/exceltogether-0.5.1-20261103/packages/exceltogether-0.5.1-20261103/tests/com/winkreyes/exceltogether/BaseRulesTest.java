package com.winkreyes.exceltogether;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * §11A base-parity tests for the classes the 0.4.x line added (DESIGN party-mesh §11A):
 * BarPolicy show/hold/repeat + gain Gap, PartyRules.judge verdict order, KillCap rolling hour,
 * blockFriendlyFire matrix, extraItems fractions, GeyserForms.bedrockText escaping, PartyText.
 * Pure logic only — no server. Run: java -cp <classes> com.winkreyes.exceltogether.BaseRulesTest
 */
public final class BaseRulesTest {
    private static int passed, failed;

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void eq(String name, Object want, Object got) {
        check(name + " (want " + want + ", got " + got + ")", want == null ? got == null : want.equals(got));
    }

    private static PartyRules.Kill kill() {
        PartyRules.Kill k = new PartyRules.Kill();
        k.killerIsPlayer = true;
        k.type = "ZOMBIE";
        k.eliteMobs = false;
        k.fromSpawner = false;
        k.spawnReason = "NATURAL";
        k.tamed = false;
        k.killerInParty = true;
        k.togetherCount = 3;
        k.underKillCap = true;
        return k;
    }

    private static PartyRules.Verdict judge(PartyRules.Kill k) {
        return PartyRules.judge(true, k, 2, PartyRules.DEFAULT_ALLOWED_REASONS, PartyRules.DEFAULT_EXCLUDED_TYPES);
    }

    public static void main(String[] args) {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();

        // ---------- BarPolicy.Mode.parse ----------
        eq("parse null", BarPolicy.Mode.CHANGES, BarPolicy.Mode.parse(null));
        eq("parse always", BarPolicy.Mode.ALWAYS, BarPolicy.Mode.parse("always"));
        eq("parse ALWAYS padded", BarPolicy.Mode.ALWAYS, BarPolicy.Mode.parse("  Always "));
        eq("parse constant", BarPolicy.Mode.ALWAYS, BarPolicy.Mode.parse("constant"));
        eq("parse on", BarPolicy.Mode.ALWAYS, BarPolicy.Mode.parse("on"));
        eq("parse true", BarPolicy.Mode.ALWAYS, BarPolicy.Mode.parse("true"));
        eq("parse off", BarPolicy.Mode.OFF, BarPolicy.Mode.parse("off"));
        eq("parse never", BarPolicy.Mode.OFF, BarPolicy.Mode.parse("never"));
        eq("parse false", BarPolicy.Mode.OFF, BarPolicy.Mode.parse("false"));
        eq("parse none", BarPolicy.Mode.OFF, BarPolicy.Mode.parse("none"));
        eq("parse changes", BarPolicy.Mode.CHANGES, BarPolicy.Mode.parse("changes"));
        eq("parse junk", BarPolicy.Mode.CHANGES, BarPolicy.Mode.parse("junk"));
        eq("parse empty", BarPolicy.Mode.CHANGES, BarPolicy.Mode.parse(""));

        // ---------- BarPolicy CHANGES: show once, hold the change, repeat as a reminder ----------
        BarPolicy bp = new BarPolicy(BarPolicy.Mode.CHANGES, 2000, 30000);
        check("first sighting shows", bp.show(a, 2, 0));
        eq("shown recorded", 2, bp.shown(a));
        check("same count does not re-show", !bp.show(a, 2, 100));
        check("change is held", !bp.show(a, 3, 100));
        check("still held before holdMs", !bp.show(a, 3, 2099));
        check("change lands at holdMs", bp.show(a, 3, 2100));
        eq("shown updated", 3, bp.shown(a));
        check("flap back starts a new hold", !bp.show(a, 2, 2200));
        check("flap back to shown clears pending silently", !bp.show(a, 3, 3000));
        check("reminder not yet due", !bp.show(a, 3, 32099));
        check("reminder fires at repeatMs", bp.show(a, 3, 32100));
        bp.touch(a, 40000);
        check("touch restarts the reminder clock", !bp.show(a, 3, 69999));
        check("reminder fires after touch + repeatMs", bp.show(a, 3, 70000));
        bp.forget(a);
        eq("forget clears shown", null, bp.shown(a));
        check("after forget the next line shows at once", bp.show(a, 1, 70001));

        // a different pending value restarts the hold
        BarPolicy bp2 = new BarPolicy(BarPolicy.Mode.CHANGES, 2000, 0);
        check("bp2 first", bp2.show(a, 1, 0));
        check("bp2 pending 2", !bp2.show(a, 2, 100));
        check("bp2 pending switches to 3, hold restarts", !bp2.show(a, 3, 1500));
        check("bp2 old hold expiry does not land the new value", !bp2.show(a, 3, 2100));
        check("bp2 new value lands after its own hold", bp2.show(a, 3, 3500));
        check("repeatMs 0 means no reminders", !bp2.show(a, 3, 999999));

        // hold 0: a change shows immediately
        BarPolicy bp3 = new BarPolicy(BarPolicy.Mode.CHANGES, 0, 0);
        check("bp3 first", bp3.show(a, 1, 0));
        check("bp3 change immediate at hold 0", bp3.show(a, 2, 1));

        // ALWAYS / OFF
        BarPolicy always = new BarPolicy(BarPolicy.Mode.ALWAYS, 2000, 30000);
        check("ALWAYS shows every tick", always.show(a, 1, 0) && always.show(a, 1, 1) && always.show(a, 9, 2));
        BarPolicy off = new BarPolicy(BarPolicy.Mode.OFF, 0, 0);
        check("OFF never shows", !off.show(a, 1, 0) && !off.show(a, 2, 100000));

        // ---------- BarPolicy.Gap ----------
        BarPolicy.Gap gap = new BarPolicy.Gap(5000);
        check("gap first is due", gap.due(a, 1000));
        check("gap inside window not due", !gap.due(a, 2000));
        check("gap just inside window not due", !gap.due(a, 5999));
        check("gap at window edge due", gap.due(a, 6000));
        check("gap is per-player", gap.due(b, 6001));
        gap.forget(a);
        check("gap forget makes due", gap.due(a, 6002));
        BarPolicy.Gap zero = new BarPolicy.Gap(0);
        check("gap 0 always due", zero.due(a, 5) && zero.due(a, 5));

        // ---------- PartyRules.judge: every verdict + the order is the contract ----------
        eq("clean kill boosts", PartyRules.Verdict.BOOST, judge(kill()));
        eq("disabled beats everything", PartyRules.Verdict.DISABLED,
                PartyRules.judge(false, new PartyRules.Kill(), 2, PartyRules.DEFAULT_ALLOWED_REASONS, PartyRules.DEFAULT_EXCLUDED_TYPES));
        PartyRules.Kill k = kill(); k.killerIsPlayer = false;
        eq("no killer", PartyRules.Verdict.NO_KILLER, judge(k));
        k = kill(); k.type = null;
        eq("null type excluded", PartyRules.Verdict.EXCLUDED_TYPE, judge(k));
        k = kill(); k.type = "wither";
        eq("excluded type is case-insensitive", PartyRules.Verdict.EXCLUDED_TYPE, judge(k));
        k = kill(); k.type = "VILLAGER"; k.eliteMobs = true;
        eq("excluded type beats elite", PartyRules.Verdict.EXCLUDED_TYPE, judge(k));
        k = kill(); k.eliteMobs = true; k.fromSpawner = true;
        eq("elite beats spawner", PartyRules.Verdict.ELITEMOBS, judge(k));
        k = kill(); k.fromSpawner = true; k.spawnReason = null;
        eq("spawner beats spawn reason", PartyRules.Verdict.SPAWNER, judge(k));
        k = kill(); k.spawnReason = null;
        eq("null spawn reason refused", PartyRules.Verdict.SPAWN_REASON, judge(k));
        k = kill(); k.spawnReason = "SPAWNER";
        eq("unlisted spawn reason refused", PartyRules.Verdict.SPAWN_REASON, judge(k));
        k = kill(); k.spawnReason = "natural";
        eq("allowed reason is case-insensitive", PartyRules.Verdict.BOOST, judge(k));
        k = kill(); k.tamed = true; k.killerInParty = false;
        eq("tamed beats not-in-party", PartyRules.Verdict.TAMED, judge(k));
        k = kill(); k.killerInParty = false; k.togetherCount = 0;
        eq("not in party beats too few", PartyRules.Verdict.NOT_IN_PARTY, judge(k));
        k = kill(); k.togetherCount = 1; k.underKillCap = false;
        eq("too few beats kill cap", PartyRules.Verdict.TOO_FEW_TOGETHER, judge(k));
        k = kill(); k.underKillCap = false;
        eq("kill cap is the last refusal", PartyRules.Verdict.KILL_CAP, judge(k));
        check("boost() only on BOOST", PartyRules.Verdict.BOOST.boost() && !PartyRules.Verdict.KILL_CAP.boost());

        // ---------- KillCap: rolling hour ----------
        PartyRules.KillCap cap = new PartyRules.KillCap(3);
        check("cap use 1", cap.tryUse(a, 0));
        check("cap use 2", cap.tryUse(a, 1000));
        check("cap use 3", cap.tryUse(a, 2000));
        check("cap use 4 refused", !cap.tryUse(a, 3000));
        check("cap wouldAllow agrees", !cap.wouldAllow(a, 3000));
        check("cap is per-player", cap.tryUse(b, 3000));
        check("cap frees when the oldest kill ages out", cap.tryUse(a, 3_600_000));
        check("cap wouldAllow does not consume", cap.wouldAllow(a, 3_600_001) == cap.wouldAllow(a, 3_600_001));
        PartyRules.KillCap unlim = new PartyRules.KillCap(0);
        check("cap 0 is unlimited", unlim.tryUse(a, 0) && unlim.tryUse(a, 0) && unlim.tryUse(a, 0));
        cap.forget(a);
        check("cap forget resets", cap.tryUse(a, 3000));

        // ---------- blockFriendlyFire matrix ----------
        check("ff off never blocks", !PartyRules.blockFriendlyFire(false, a, b, (x, y) -> true));
        check("ff null attacker", !PartyRules.blockFriendlyFire(true, null, b, (x, y) -> true));
        check("ff null victim", !PartyRules.blockFriendlyFire(true, a, null, (x, y) -> true));
        check("ff self never blocks", !PartyRules.blockFriendlyFire(true, a, a, (x, y) -> true));
        check("ff same party blocks", PartyRules.blockFriendlyFire(true, a, b, (x, y) -> true));
        check("ff different parties pass", !PartyRules.blockFriendlyFire(true, a, b, (x, y) -> false));

        // ---------- extraItems ----------
        eq("extra whole only", 3, PartyRules.extraItems(3, 2.0, () -> 0.99));
        eq("extra fraction hit", 3, PartyRules.extraItems(10, 1.25, () -> 0.4));
        eq("extra fraction miss", 2, PartyRules.extraItems(10, 1.25, () -> 0.6));
        eq("extra none at mult 1", 0, PartyRules.extraItems(10, 1.0, () -> 0.0));
        eq("extra none for empty stack", 0, PartyRules.extraItems(0, 2.0, () -> 0.0));
        eq("extra none for NaN mult", 0, PartyRules.extraItems(10, Double.NaN, () -> 0.0));
        eq("extra none for infinite mult", 0, PartyRules.extraItems(10, Double.POSITIVE_INFINITY, () -> 0.0));

        // ---------- upperSet ----------
        eq("upperSet null keeps defaults", Set.of("X"), PartyRules.upperSet(null, Set.of("X")));
        eq("upperSet empty keeps defaults", Set.of("X"), PartyRules.upperSet(List.of(), Set.of("X")));
        eq("upperSet normalises + skips blanks", Set.of("A", "B"), PartyRules.upperSet(List.of(" a ", "", "B"), Set.of("X")));
        eq("upperSet all-blank keeps defaults", Set.of("X"), PartyRules.upperSet(List.of(" ", ""), Set.of("X")));

        // ---------- GeyserForms.bedrockText ----------
        GeyserForms.setEscapePercent(true);
        eq("escape doubles %", "a%%b", GeyserForms.bedrockText("a%b"));
        eq("escape doubles every %", "%%%%", GeyserForms.bedrockText("%%"));
        eq("escape leaves plain text", "abc", GeyserForms.bedrockText("abc"));
        eq("escape null stays null", null, GeyserForms.bedrockText((String) null));
        eq("escape empty stays empty", "", GeyserForms.bedrockText(""));
        eq("escape list form", List.of("a%%b", "c"), GeyserForms.bedrockText(List.of("a%b", "c")));
        eq("escape null list stays null", null, GeyserForms.bedrockText((List<String>) null));
        GeyserForms.setEscapePercent(false);
        eq("escape off is identity", "a%b", GeyserForms.bedrockText("a%b"));
        GeyserForms.setEscapePercent(true);

        // ---------- PartyText ----------
        eq("NOT_IN_PARTY wording", "You are not in a party. Party up to earn more!", PartyText.NOT_IN_PARTY);
        eq("bonusNote no share", "+25% Jobs money and XP when a party member is within 50 blocks.",
                PartyText.bonusNote(25, 50, 0, false));
        eq("bonusNote share without mcMMO XP", "+25% Jobs money and XP when a party member is within 50 blocks. Nearby party members also share 50% of each other's Jobs gains.",
                PartyText.bonusNote(25, 50, 50, false));
        eq("bonusNote share with mcMMO XP", "+25% Jobs money and XP when a party member is within 50 blocks. Nearby party members also share 50% of each other's Jobs gains and mcMMO skill XP.",
                PartyText.bonusNote(25, 50, 50, true));

        System.out.println("BaseRulesTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
