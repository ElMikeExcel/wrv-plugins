import com.winkreyes.excelwandbars.core.*;

/** Rank-up by casting (2026-10-26). */
public final class RankUpTest {
    public static void run() {
        System.out.println("RankUpTest");
        RankLadder l = RankLadder.wrvDefault();
        Check.eq("student", l.next("beginner"), "next beginner");
        Check.eq("master", l.next("Apprentice"), "next case-insensitive");
        Check.eq(null, l.next("master"), "master is top");
        Check.eq(null, l.next("wand"), "non-ladder wand");
        Check.eq(800L, l.cost("student"), "student cost");
        Check.eq(4200L, l.total("master"), "total to master");
        Check.eq(RankLadder.Step.SAVING, l.step("beginner", 799, 1), "799 saving");
        Check.eq(RankLadder.Step.READY, l.step("beginner", 800, 1), "800 ready");
        Check.eq(RankLadder.Step.TOP, l.step("master", 9999, 1), "top");
        l.minStage("apprentice", 3);
        Check.eq(RankLadder.Step.NOT_OPEN, l.step("student", 5000, 2), "stage gate closed");
        Check.eq(RankLadder.Step.READY, l.step("student", 5000, 3), "stage gate open");
        Check.eq(RankLadder.Step.READY, l.step("student", 5000, -1), "unknown stage = open");

        // Magic's own throttle formula
        Check.eq(2, EarnMath.throttled(2, 0, 0), "no cooldown full");
        Check.eq(0, EarnMath.throttled(2, 1000, 60000), "mend spam 1s -> 0");
        Check.eq(1, EarnMath.throttled(2, 30000, 60000), "half way -> 1");
        Check.eq(20, EarnMath.throttled(20, 61000, 60000), "full after cooldown");
        Check.eq(0, EarnMath.throttled(0, 99999, 60000), "earns 0");

        // bonuses
        Check.eq("", EarnMath.bonus(1, 1, 1.5).tag(), "no bonus");
        Check.eq("SP x1.2 (weekend)", EarnMath.bonus(1.2, 1, 1.5).tag(), "weekend tag");
        Check.eq("SP x1.25 (group)", EarnMath.bonus(1, 1.25, 1.5).tag(), "group tag");
        Check.eq("SP x1.5 (weekend + group)", EarnMath.bonus(1.2, 1.25, 1.5).tag(), "both = cap");
        Check.eq(1.5, EarnMath.bonus(2.0, 1.25, 1.5).multiplier, "admin xprate 2 capped");
        Check.eq(1.0, EarnMath.bonus(1.2, 1.25, 1.0).multiplier, "cap 1 = bonuses off");
        Check.eq(1.0, EarnMath.bonus(0.5, 1, 1.5).multiplier, "never below 1");

        // daily soft cap
        Check.eq(5.0, EarnMath.daily(5, 0, 800, 1600, 0.25), "under soft cap");
        Check.eq(3.0 + 0.5, EarnMath.daily(5, 797, 800, 1600, 0.25), "crosses soft cap");
        Check.eq(1.25, EarnMath.daily(5, 1000, 800, 1600, 0.25), "slow rate");
        Check.eq(0.0, EarnMath.daily(5, 1600, 800, 1600, 0.25), "hard cap");
        Check.eq(1.0, EarnMath.daily(8, 1599, 800, 1600, 0.25), "clamped at hard cap");
        Check.eq(5.0, EarnMath.daily(5, 99999, 0, 0, 0.25), "caps off");

        // text
        Check.eq("Student: 340/1,600 SP", RankText.progress("Student", 340, 1600, false), "progress");
        Check.eq("Master: top rank", RankText.progress("Master", 50, 0, true), "top");
        Check.eq("+2 SP (x1.25 group) | Student 340/1,600", RankText.earned(2, "group", 1.25, "Student", 340, 1600, false), "earned line");
        Check.eq("+3 SP | Beginner 800/800", RankText.earned(3, "", 1, "Beginner", 900, 800, false), "earned clamps");
        java.util.List<String> h = RankText.header("Student", "Apprentice", 340, 1600, RankLadder.Step.SAVING, "SP x1.2 (weekend)", "Cast spells to earn SP.");
        Check.eq("Student: 340/1,600 SP to Apprentice", h.get(0), "header 1");
        Check.eq("Cast spells to earn SP.", h.get(1), "header 2");
        Check.eq("Bonus now: SP x1.2 (weekend)", h.get(2), "header 3");
        h = RankText.header("Student", "Apprentice", 5000, 1600, RankLadder.Step.NOT_OPEN, "", "x");
        Check.eq("Apprentice rank is not open yet.", h.get(1), "header not open");
        for (String s : h) Check.ok(s.length() <= SpellInfoText.LINE && !s.contains("\u2014") && !s.toLowerCase().contains("seal"), "line ok: " + s);
        Check.eq("Student rank! 6 new spells", RankText.fill("{rank} rank! {spells} new spells", "Student", 6), "fill");
    
        // 0.2.8 shipped config: on, broadcast on, Master display name, no seal cap, no em dashes, no "seal" in player text
        try {
            String cfg = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("config.yml")), java.nio.charset.StandardCharsets.UTF_8);
            String sec = cfg.substring(cfg.indexOf("rank-up-casting:"), cfg.indexOf("\nrank-names:"));
            Check.ok(sec.contains("\n  enabled: true"), "0.2.8 ships rank-up enabled");
            Check.ok(sec.contains("\n    broadcast: true"), "broadcast on");
            Check.ok(sec.contains("\n  max-rank-by-stage: {}"), "no seal cap");
            Check.ok(sec.contains("student: 800") && sec.contains("apprentice: 1600") && sec.contains("master: 1800"), "normal pacing 800/1600/1800");
            Check.ok(cfg.contains("\n  master: Master\n"), "master shows as Master");
            Check.ok(!cfg.contains("\u2014"), "no em dash in config");
            for (String line : sec.split("\n")) {
                String t = line.trim();
                if (t.startsWith("title:") || t.startsWith("subtitle:") || t.startsWith("chat:") || t.startsWith("broadcast: \"") || t.startsWith("how:"))
                    Check.ok(!t.toLowerCase().contains("seal") && t.length() < 120, "player text ok: " + t);
            }
        } catch (java.io.IOException e) { Check.ok(false, "read config.yml: " + e); }
        Check.eq("Pal reached Master rank!", RankText.fill("{player} reached {rank} rank!", "Master", 2).replace("{player}", "Pal"), "broadcast text");
    }
}

