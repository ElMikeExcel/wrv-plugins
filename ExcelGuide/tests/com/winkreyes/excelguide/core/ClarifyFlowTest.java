package com.winkreyes.excelguide.core;

import java.util.List;

/**
 * The clarify state machine end-to-end through the engine (SPEC 5.1):
 * ask, choose one, choose Magic in both seal states, choose all, a
 * garbage reply (combined fallback exactly once), timeout, and a fresh
 * question replacing the clarification.
 */
public final class ClarifyFlowTest {

    private static String onlyText(TestSupport.T t, String name, List<GuideEngine.Reply> replies) {
        t.check(name + " produced a reply", replies.size() >= 1);
        return replies.isEmpty() ? "" : replies.get(0).text();
    }

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        GuideEngine engine = TestSupport.engine(stats, clock);
        GuideEngine.PlayerRef mike = TestSupport.MIKE;

        // Ask -> clarify question, private.
        var r1 = engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.eq("clarify channel private", GuideEngine.Channel.PRIVATE, r1.get(0).channel());
        t.check("clarify lists mcMMO, Jobs, time, Magic",
                onlyText(t, "clarify", r1).contains("mcMMO") && r1.get(0).text().contains("Magic"));

        // Reply "mcmmo" in plain chat (AMBIENT transport).
        clock.advanceSeconds(3);
        var r2 = engine.handle(mike, "mcmmo", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("mcmmo choice answers mcMMO", onlyText(t, "mcmmo choice", r2).contains("overall 861"));
        t.eq("stat answer stays private", GuideEngine.Channel.PRIVATE, r2.get(0).channel());

        // Magic choice before Seal 1 -> the lore line.
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r3 = engine.handle(mike, "magic", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("pre-seal magic is the lore line",
                onlyText(t, "lore", r3).contains("not released into the world yet"));
        t.check("lore line names the seals", r3.get(0).text().contains("seals"));

        // Magic choice after Seal 1 -> the real standing.
        stats.seals = 1;
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r4 = engine.handle(mike, "magic", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("post-seal magic is real data",
                onlyText(t, "magic data", r4).contains("Student path"));

        // Direct magic question also follows the seal state.
        clock.advanceSeconds(65);
        var r4b = engine.handle(mike, "what is my magic level", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("direct magic question post-seal", onlyText(t, "direct magic", r4b).contains("Student path"));
        stats.seals = 0;
        clock.advanceSeconds(65);
        var r4c = engine.handle(mike, "what is my magic level", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("direct magic question pre-seal is lore",
                onlyText(t, "direct magic lore", r4c).contains("not released into the world yet"));

        // "all" -> combined line with every part.
        clock.advanceSeconds(65);
        engine.handle(mike, "my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r5 = engine.handle(mike, "all", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        String allText = r5.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("all includes mcMMO", allText.contains("overall 861"));
        t.check("all includes Jobs", allText.contains("Miner 12"));
        t.check("all includes playtime", allText.contains("23 h 40 m"));
        t.check("all includes magic (lore pre-seal)", allText.contains("not released into the world yet"));

        // Garbage reply -> combined fallback, exactly once.
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r6 = engine.handle(mike, "banana", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("garbage gets combined fallback", onlyText(t, "fallback", r6).contains("overall 861"));
        clock.advanceSeconds(2);
        var r7 = engine.handle(mike, "banana", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("no second fallback", r7.isEmpty());

        // Timeout: the reply after the window is just chat again.
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(61);
        var r8 = engine.handle(mike, "mcmmo", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("expired clarify ignores the reply", r8.isEmpty());

        // A fresh question replaces the clarification.
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r9 = engine.handle(mike, "how do shops work", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("new question answered instead of clarify", r9.isEmpty()
                || !r9.get(0).text().contains("overall 861")); // ambient shops stays silent; key point: no clarify answer
        clock.advanceSeconds(21); // out of the per-player gap left by the clarify question
        var r10 = engine.handle(mike, "how do shops work", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("shops answered via command after clarify dropped",
                !r10.isEmpty());
        clock.advanceSeconds(2);
        var r11 = engine.handle(mike, "jobs", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("no clarify pending after replacement", r11.isEmpty());

        // Job choice path.
        clock.advanceSeconds(65);
        engine.handle(mike, "what's my level?", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        clock.advanceSeconds(2);
        var r12 = engine.handle(mike, "jobs", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("jobs choice answers jobs", onlyText(t, "jobs choice", r12).contains("Miner 12"));

        // Spells acquisition (0.1.1): sealed lore before Seal 1, the
        // wand how-to from Seal 1 on - same seal-state read as the
        // Magic standing.
        stats.seals = 0;
        clock.advanceSeconds(65);
        var s1 = engine.handle(mike, "how do i get new spells", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String s1Text = s1.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("spells pre-seal is the sealed lore", s1Text.contains("still sealed"));
        t.check("spells lore names the Quartermaster", s1Text.contains("Quartermaster"));
        t.eq("spells answer is public", GuideEngine.Channel.PUBLIC, s1.get(0).channel());
        stats.seals = 1;
        clock.advanceSeconds(65);
        var s2 = engine.handle(mike, "how do i get new spells", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String s2Text = s2.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("spells post-seal is the wand how-to", s2Text.contains("learned through your wand"));
        t.check("spells post-seal drops the lore", !s2Text.contains("still sealed"));
        stats.seals = 0;

        // Bare "ability" (0.1.1): answered by skill-unlocks, whose text
        // names spells as the other ability system.
        clock.advanceSeconds(65);
        var a1 = engine.handle(mike, "how do i get a new ability", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String a1Text = a1.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("bare ability answered with skill unlocks", a1Text.contains("Skill Trees page"));
        t.check("ability answer names spells as the other kind",
                a1Text.contains("ask me how to get spells"));

        // 0.2.2 (Oct 18, 2026) - Mike's live test of the v2 fix: the
        // amounts answer was right but GENERIC, and his verdict was
        // "I was hoping it would actually tell me the amount of
        // blocks or how to check it myself on the server. Really I
        // want both." Now the first-person shape serves the COMBINED
        // answer: Mike's own numbers + the rule + /claimslist, and
        // PRIVATE (it is a stat) instead of the old public generic.
        // It fired in plain chat, so check the AMBIENT door.
        clock.advanceSeconds(65);
        var c1 = engine.handle(mike, "how much land can i claim?", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        String c1Text = c1.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("combined answer carries Mike's own numbers", c1Text.contains("484 claim blocks left"));
        t.check("combined answer carries the starting 100", c1Text.contains("starts with 100 claim blocks"));
        t.check("combined answer carries the 80,000 cap", c1Text.contains("80,000"));
        t.check("combined answer carries the /claimslist self-check", c1Text.contains("/claimslist"));
        t.check("combined answer is not the how-to", !c1Text.contains("golden shovel"));
        t.eq("combined answer is whispered to the asker", GuideEngine.Channel.PRIVATE, c1.get(0).channel());

        // The how-to itself still answers the how question (Mike's
        // required behavior - claim-amounts must not steal it). Via
        // the ambient door, where its curated line is served as-is.
        clock.advanceSeconds(65);
        var c2 = engine.handle(mike, "how do i claim land", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        String c2Text = c2.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("how-to still answers how do i claim land", c2Text.contains("golden shovel"));

        // (2) "when do i get a new ability ?" got silence live (the
        // screenshot predates the 0.1.1 swap, but the when-form was a
        // real gap). Now: skill-unlocks, both doors.
        clock.advanceSeconds(65);
        var w1 = engine.handle(mike, "when do i get a new ability ?", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        String w1Text = w1.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("when-ability answered in chat with skill unlocks", w1Text.contains("Skill Trees page"));

        // (3) "when can i learn spells" follows the same seal-state
        // split as the get-forms: sealed lore now, wand how-to later.
        stats.seals = 0;
        clock.advanceSeconds(65);
        var w2 = engine.handle(mike, "when can i learn spells", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String w2Text = w2.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("when-spells pre-seal is the sealed lore", w2Text.contains("still sealed"));
        stats.seals = 1;
        clock.advanceSeconds(65);
        var w3 = engine.handle(mike, "when can i learn spells", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String w3Text = w3.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
        t.check("when-spells post-seal is the wand how-to", w3Text.contains("learned through your wand"));
        stats.seals = 0;

        // Vanilla crafting stays an honest non-answer (0.1.1 baseline).
        clock.advanceSeconds(65);
        var g1 = engine.handle(mike, "how do i make a golem", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("golem stays an honest non-answer",
                onlyText(t, "golem", g1).contains("I don't know that one yet"));

        t.finish("ClarifyFlowTest");
    }
}
