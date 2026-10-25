package com.winkreyes.excelguide.core;

import java.util.List;

/**
 * The 0.2.2 claim-blocks checks (Oct 18, 2026 - Mike's live test).
 * Mike asked in chat "how much land can I claim?" and got the generic
 * claim-amounts text; his verdict: "I was hoping it would actually
 * tell me the amount of blocks or how to check it myself on the
 * server. Really I want both." These checks pin the fix end to end:
 *
 *  - FIRST-PERSON amount questions get the COMBINED answer - the
 *    asker's own live GP numbers, the general rule, and the
 *    /claimslist self-check - whispered to the asker only (the stat
 *    layer's privacy rule, unchanged).
 *  - IMPERSONAL questions keep the generic answer (which gained the
 *    /claimslist pointer), publicly, with no personal numbers in it.
 *  - A NAMED OTHER player's numbers are never served to anyone: the
 *    existing other-player refusal answers instead (0.2.2 taught the
 *    refusal that claims/blocks/land are stat words).
 *  - No GP data (stat source null) falls back to the generic answer:
 *    never an error line, never a guessed number.
 *
 * Fake GP numbers are the real ones read off the live server Oct 18,
 * 2026 (~/workspace/claim-blocks/): Mike {484, 0, 0, 484},
 * Avery {3492, 0, 1099, 2393}.
 */
public final class ClaimBlocksTest {

    private static GuideEngine engine(TestSupport.FakeStats stats) {
        return TestSupport.engine(stats, new TestSupport.FakeClock());
    }

    private static String joined(List<GuideEngine.Reply> replies) {
        return replies.stream().map(GuideEngine.Reply::text).reduce("", (a, b) -> a + " " + b);
    }

    private static boolean allPrivate(List<GuideEngine.Reply> replies) {
        return !replies.isEmpty()
                && replies.stream().allMatch(r -> r.channel() == GuideEngine.Channel.PRIVATE);
    }

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();

        // ---------- the formatter, exact strings ----------
        t.eq("combined, Mike's real numbers",
                "You have 484 claim blocks left: you earned 484 from playtime and used 0 on claims. "
                        + "Everyone starts with 100 claim blocks and earns 100 more for every hour of play, "
                        + "up to 80,000 - blocks come from playtime only, they can't be bought. "
                        + "Check yours any time with /claimslist.",
                ClaimText.combined(484, 0, 0, 484));
        String averyLine = ClaimText.combined(3492, 0, 1099, 2393);
        t.check("combined formats thousands with commas",
                averyLine.startsWith("You have 2,393 claim blocks left: you earned 3,492 from playtime "
                        + "and used 1,099 on claims."));
        t.check("combined shows a bonus when there is one",
                ClaimText.combined(6000, 500, 1000, 5500).contains("plus 500 bonus"));
        t.check("combined hides a zero bonus",
                !ClaimText.combined(100, 0, 0, 100).contains("bonus"));
        t.check("combined clamps a negative remaining at 0",
                ClaimText.combined(100, 0, 150, -50).startsWith("You have 0 claim blocks left"));

        // ---------- engine: first person, asked door ----------
        var mikeAsk = engine(new TestSupport.FakeStats()).handle(TestSupport.MIKE,
                "how much land can i claim", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        String mikeText = joined(mikeAsk);
        t.check("Mike gets his own numbers", mikeText.contains("484 claim blocks left"));
        t.check("Mike's answer carries the rule", mikeText.contains("starts with 100 claim blocks"));
        t.check("Mike's answer carries the self-check", mikeText.contains("/claimslist"));
        t.check("Mike's answer is private", allPrivate(mikeAsk));
        t.check("Mike's answer fits the 2-line style", mikeAsk.size() <= 2);
        t.check("Mike never sees Avery's numbers", !mikeText.contains("2,393"));

        var averyAsk = engine(new TestSupport.FakeStats()).handle(TestSupport.AVERY,
                "how many claim blocks do i have", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        String averyText = joined(averyAsk);
        t.check("Avery gets HER numbers, not the asker's template",
                averyText.contains("2,393 claim blocks left")
                        && averyText.contains("3,492") && averyText.contains("1,099"));
        t.check("Avery never sees Mike's numbers", !averyText.contains("484 claim blocks"));
        t.check("Avery's answer is private", allPrivate(averyAsk));

        // ---------- engine: the live-test reproduction, ambient door ----------
        var ambient = engine(new TestSupport.FakeStats()).handle(TestSupport.MIKE,
                "how much land can i claim?", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("ambient first-person fires the combined answer",
                joined(ambient).contains("484 claim blocks left"));
        t.check("ambient combined answer is whispered, never public", allPrivate(ambient));

        // ---------- engine: impersonal keeps the generic answer ----------
        var impersonal = engine(new TestSupport.FakeStats()).handle(TestSupport.MIKE,
                "how much land can a player claim", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        String impersonalText = joined(impersonal);
        t.check("impersonal gets the generic rule", impersonalText.contains("starts with 100 claim blocks"));
        t.check("impersonal generic carries the /claimslist pointer", impersonalText.contains("/claimslist"));
        t.check("impersonal generic keeps the first-chest fact", impersonalText.contains("9x9"));
        t.check("impersonal answer carries NO personal numbers", !impersonalText.contains("484"));
        t.eq("impersonal answer stays public", GuideEngine.Channel.PUBLIC, impersonal.get(0).channel());

        var impersonalAmbient = engine(new TestSupport.FakeStats()).handle(TestSupport.MIKE,
                "how much land can a player claim", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("impersonal ambient still fires the public generic",
                joined(impersonalAmbient).contains("starts with 100 claim blocks")
                        && impersonalAmbient.get(0).channel() == GuideEngine.Channel.PUBLIC);

        // ---------- privacy: a named OTHER player is refused ----------
        var aboutMike = engine(new TestSupport.FakeStats()).handle(TestSupport.AVERY,
                "how many claim blocks does OwnerTest have", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String aboutMikeText = joined(aboutMike);
        t.check("asking about Mike by name is refused", aboutMikeText.contains("theirs to share"));
        t.check("the refusal leaks none of Mike's numbers", !aboutMikeText.contains("484"));
        t.check("the refusal does not substitute the asker's numbers", !aboutMikeText.contains("2,393"));

        var aboutAvery = engine(new TestSupport.FakeStats()).handle(TestSupport.MIKE,
                "how many claim blocks does testkite have", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        String aboutAveryText = joined(aboutAvery);
        t.check("asking about Avery by name is refused", aboutAveryText.contains("theirs to share"));
        t.check("the refusal leaks none of Avery's numbers", !aboutAveryText.contains("2,393"));

        // ---------- no GP data: generic fallback, never an error ----------
        TestSupport.FakeStats noGp = new TestSupport.FakeStats();
        noGp.nulls = true;
        var fallback = engine(noGp).handle(TestSupport.MIKE,
                "how much land can i claim", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        String fallbackText = joined(fallback);
        t.check("no-GP fallback still answers", !fallback.isEmpty());
        t.check("no-GP fallback is the generic rule + pointer",
                fallbackText.contains("starts with 100 claim blocks")
                        && fallbackText.contains("/claimslist"));
        t.check("no-GP fallback invents no numbers", !fallbackText.contains("484"));
        t.check("no-GP fallback is not the can't-see error",
                !fallbackText.contains("can't see that"));

        t.finish("ClaimBlocksTest");
    }
}
