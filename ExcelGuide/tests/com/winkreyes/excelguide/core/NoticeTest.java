package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.UUID;

/**
 * The 0.2.2 answer-acknowledgment checks (Oct 18, 2026 - Mike's
 * rule: "I need it to respond with a timer or something to let them
 * know it'll answer soon - at least silence is never good with
 * kids"). A direct question (/ask or the "Guide," summon) is never
 * met with silence:
 *
 *  - Blocked by a chat cooldown (per-player 20 s, per-question
 *    60 s, global 6/min) or by the AI bucket (60 s), the asker gets
 *    ONE whispered notice quoting the REAL remaining wait from the
 *    same fake clock the limiter runs on, rounded up to a friendly
 *    number of seconds.
 *  - A question handed to the AI fallback gets the thinking line
 *    synchronously, once per engagement; the answer still arrives
 *    through the sink, and a failed AI still serves the don't-know.
 *  - Ambient chat-watch NEVER notices: blocked ambient stays silent.
 *  - Notices self-throttle (30 s per player, config): a blocked
 *    streak earns one notice, then silence, then a fresh notice
 *    once the throttle lapses (with the wait re-measured).
 *  - Notices off (or a malformed notices block, fail-soft) restores
 *    exactly the pre-0.2.2 behavior: blocked means silent.
 */
public final class NoticeTest {

    private static final String THINKING = "Hmm, let me think about that one…";
    private static final String AI_QUESTION = "will the claim blocks cover a 30x30 base";
    private static final String AI_QUESTION_2 = "how do i tame a fox";

    private static GuideEngine chatEngine(TestSupport.FakeClock clock) {
        return TestSupport.engine(new TestSupport.FakeStats(), clock);
    }

    private static GuideEngine chatEngine(GuideConfig cfg, TestSupport.FakeClock clock) {
        return new GuideEngine(cfg, TestSupport.realIntents(), TestSupport.realKnowledge(),
                new TestSupport.FakeStats(), clock, null);
    }

    /** The AiFallbackTest config recipe, plus optional extra replacements. */
    private static GuideConfig aiConfig(String... replacements) {
        String yaml = TestSupport.readProj("config.yml");
        yaml = yaml.replace("ai:\n  enabled: false", "ai:\n  enabled: true");
        yaml = yaml.replace("api-key: \"\"", "api-key: \"test-key-not-real\"");
        for (String r : replacements) {
            int eq = r.indexOf('=');
            yaml = yaml.replace(r.substring(0, eq), r.substring(eq + 1));
        }
        return GuideConfig.fromYaml(yaml);
    }

    private static GuideEngine aiEngine(GuideConfig cfg, TestSupport.FakeClock clock,
                                        AiFallbackTest.MockProvider mock,
                                        GuideEngine.AiExecutor exec) {
        return new GuideEngine(cfg, TestSupport.realIntents(), TestSupport.realKnowledge(),
                new TestSupport.FakeStats(), clock, null, mock,
                new AiFallbackTest.FakeFacts(), null, exec);
    }

    private static String notice(long seconds) {
        return "One moment, I can answer again in about " + seconds + " seconds.";
    }

    private static boolean isNotice(List<GuideEngine.Reply> out, long seconds) {
        return out.size() == 1
                && out.get(0).channel() == GuideEngine.Channel.PRIVATE
                && out.get(0).text().equals(notice(seconds));
    }

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        var mike = TestSupport.MIKE;
        var avery = TestSupport.AVERY;

        // ---------- friendly rounding, exact ----------
        t.eq("round 1 ms up to 5", 5, GuideEngine.friendlySeconds(1));
        t.eq("round 4999 ms to 5", 5, GuideEngine.friendlySeconds(4999));
        t.eq("round 5000 ms to 5", 5, GuideEngine.friendlySeconds(5000));
        t.eq("round 5001 ms up to 10", 10, GuideEngine.friendlySeconds(5001));
        t.eq("round 12.3 s up to 15", 15, GuideEngine.friendlySeconds(12_300));
        t.eq("round 13 s up to 15", 15, GuideEngine.friendlySeconds(13_000));
        t.eq("round 20 s stays 20", 20, GuideEngine.friendlySeconds(20_000));
        t.eq("round 47.2 s up to 50", 50, GuideEngine.friendlySeconds(47_200));
        t.eq("round 60 s stays 60", 60, GuideEngine.friendlySeconds(60_000));

        // ---------- per-player bucket: notice quotes the real wait ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            var first = e.handle(mike, "what is my balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("first question answered", first.size() == 1
                    && first.get(0).text().contains("Balance"));
            var blocked = e.handle(mike, "what is my playtime", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("per-player block notices 20 seconds", isNotice(blocked, 20));
            var averyOk = e.handle(avery, "what is my balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("the block is per-player (Avery still answered)",
                    averyOk.size() == 1 && averyOk.get(0).text().contains("Balance"));
            clock.advanceSeconds(21);
            var after = e.handle(mike, "what is my playtime", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("the notice did not extend the cooldown (answer after 21 s)",
                    after.size() == 1 && after.get(0).text().contains("Played"));
        }
        {
            // Same bucket, 7 s in: 13 s remain, quoted as a friendly 15.
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            e.handle(mike, "what is my balance", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
            clock.advanceSeconds(7);
            var blocked = e.handle(mike, "what is my playtime", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("13 s remaining quotes as about 15", isNotice(blocked, 15));
        }

        // ---------- per-question bucket: another player, same public question ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            var pub = e.handle(mike, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("public question answered publicly", !pub.isEmpty()
                    && pub.get(0).channel() == GuideEngine.Channel.PUBLIC);
            var blocked = e.handle(avery, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("per-question block notices 60 seconds, whispered to the asker only",
                    isNotice(blocked, 60));
        }

        // ---------- global bucket: server-wide cap ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            GuideEngine.PlayerRef[] players = {
                    mike, avery, TestSupport.ROWAN,
                    new GuideEngine.PlayerRef(UUID.randomUUID(), "TestFour"),
                    new GuideEngine.PlayerRef(UUID.randomUUID(), "TestFive"),
                    new GuideEngine.PlayerRef(UUID.randomUUID(), "TestSix")};
            for (GuideEngine.PlayerRef p : players) {
                var a = e.handle(p, "what is my balance", GuideEngine.Door.COMMAND,
                        TestSupport.ONLINE, false);
                t.check("global fill: " + p.name() + " answered", a.size() == 1
                        && a.get(0).text().contains("Balance"));
            }
            var seventh = new GuideEngine.PlayerRef(UUID.randomUUID(), "TestSeven");
            var blocked = e.handle(seventh, "what is my balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("global block notices about 60 seconds", isNotice(blocked, 60));
        }

        // ---------- summon door notices too ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            e.handle(mike, "what is my balance", GuideEngine.Door.SUMMON, TestSupport.ONLINE, false);
            var blocked = e.handle(mike, "what is my playtime", GuideEngine.Door.SUMMON,
                    TestSupport.ONLINE, false);
            t.check("summon block notices 20 seconds", isNotice(blocked, 20));
        }

        // ---------- ambient NEVER notices ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            var wand = e.handle(mike, "i lost my wand", GuideEngine.Door.AMBIENT,
                    TestSupport.ONLINE, false);
            t.check("ambient answer fires", !wand.isEmpty());
            var blocked = e.handle(mike, "how much land can i claim", GuideEngine.Door.AMBIENT,
                    TestSupport.ONLINE, false);
            t.check("ambient block stays silent (no notice)", blocked.isEmpty());
        }

        // ---------- throttle: one notice per 30 s, then a fresh one ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(clock);
            e.handle(mike, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            var n1 = e.handle(avery, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("throttle: first blocked ask notices", isNotice(n1, 60));
            clock.advanceSeconds(10);
            var n2 = e.handle(avery, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("throttle: a repeat inside 30 s is silent", n2.isEmpty());
            clock.advanceSeconds(21);
            var n3 = e.handle(avery, "what is the claim limit", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("throttle: after 31 s a fresh notice quotes the new wait (29 s -> 30)",
                    isNotice(n3, 30));
        }

        // ---------- AI engagement: thinking line, once, answer follows ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            AiFallbackTest.QueueExecutor queue = new AiFallbackTest.QueueExecutor();
            GuideEngine e = aiEngine(aiConfig(), clock, mock, queue);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            var sync = e.handle(mike, AI_QUESTION, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("thinking line is the only synchronous reply", sync.size() == 1
                    && sync.get(0).channel() == GuideEngine.Channel.PRIVATE
                    && sync.get(0).text().equals(THINKING));
            t.check("no answer before the AI task runs", sink.got.isEmpty());
            queue.drain();
            t.check("the real answer follows via the sink", sink.got.size() == 1
                    && sink.got.get(0).text().contains("Foxes settle"));
            clock.advanceSeconds(61);
            var sync2 = e.handle(mike, AI_QUESTION_2, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("second engagement thinks exactly once too", sync2.size() == 1
                    && sync2.get(0).text().equals(THINKING));
            queue.drain();
            t.eq("two engagements, two provider calls", 2, mock.invocations);
            t.eq("two engagements, two answers", 2, sink.got.size());
        }

        // ---------- AI failure: thinking line, then the standard don't-know ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            mock.failure = new AiProvider.AiException(AiProvider.AiException.Kind.HTTP, 500, "x");
            GuideEngine e = aiEngine(aiConfig(), clock, mock, Runnable::run);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            var sync = e.handle(mike, AI_QUESTION, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("failed AI still thinks first", sync.size() == 1
                    && sync.get(0).text().equals(THINKING));
            t.check("failed AI still serves the don't-know", sink.got.size() == 1
                    && sink.got.get(0).text().contains("I don't know that one yet"));
        }

        // ---------- AI bucket: notice quotes the AI wait, not a don't-know ----------
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            GuideEngine e = aiEngine(aiConfig(), clock, mock, Runnable::run);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            e.handle(mike, AI_QUESTION, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.eq("first AI question engaged", 1, mock.invocations);
            var blocked = e.handle(mike, AI_QUESTION_2, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("AI-bucket block notices 60 seconds", isNotice(blocked, 60));
            t.eq("AI-bucket block never reaches the provider", 1, mock.invocations);
            t.eq("AI-bucket block delivers nothing async", 1, sink.got.size());
            clock.advanceSeconds(45);
            var blocked2 = e.handle(mike, AI_QUESTION_2, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("AI-bucket wait re-measured (15 s left)", isNotice(blocked2, 15));
            clock.advanceSeconds(16);
            var sync3 = e.handle(mike, AI_QUESTION_2, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("after the quoted wait the question engages again",
                    sync3.size() == 1 && sync3.get(0).text().equals(THINKING));
            t.eq("provider called again after the wait", 2, mock.invocations);
        }

        // ---------- notices OFF: exactly the old silence ----------
        {
            GuideConfig off = aiConfig("notices:\n  enabled: true=notices:\n  enabled: false");
            t.check("notices-off config parses off", !off.noticesEnabled);
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            GuideEngine e = chatEngine(off, clock);
            e.handle(mike, "what is my balance", GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
            var blocked = e.handle(mike, "what is my playtime", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("notices off: cooldown block is silent, as before", blocked.isEmpty());

            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            GuideEngine ae = aiEngine(off, new TestSupport.FakeClock(), mock, Runnable::run);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            var sync = ae.handle(mike, AI_QUESTION, GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("notices off: engagement returns nothing synchronously", sync.isEmpty());
            t.check("notices off: the AI answer still arrives", sink.got.size() == 1);
        }

        // ---------- malformed notices config: fail-soft OFF, answers unaffected ----------
        {
            t.check("notices as a bare word is malformed -> off",
                    !GuideConfig.fromYaml("notices: soon\n").noticesEnabled);
            t.check("cooldown line without %seconds% is malformed -> off",
                    !GuideConfig.fromYaml(
                            "notices:\n  cooldown-line: \"no wait quoted here\"\n").noticesEnabled);
            t.check("throttle of the wrong type is malformed -> off",
                    !GuideConfig.fromYaml(
                            "notices:\n  throttle-seconds: \"thirty\"\n").noticesEnabled);
            GuideConfig good = TestSupport.realConfig();
            t.check("the shipped config parses notices ON with the defaults",
                    good.noticesEnabled && good.noticeThrottleSeconds == 30
                            && good.noticeCooldownLine.contains("%seconds%")
                            && good.noticeThinkingLine.equals(THINKING));
            String yaml = TestSupport.readProj("config.yml").replace(
                    "  enabled: true\n  cooldown-line:", "  enabled: \"yes\"\n  cooldown-line:");
            GuideConfig broken = GuideConfig.fromYaml(yaml);
            t.check("a mistyped enabled is malformed -> off", !broken.noticesEnabled);
            GuideEngine e = chatEngine(broken, new TestSupport.FakeClock());
            var ans = e.handle(mike, "what is my balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("malformed notices: answers unaffected", ans.size() == 1
                    && ans.get(0).text().contains("Balance"));
            var blocked = e.handle(mike, "what is my playtime", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false);
            t.check("malformed notices: blocks are silent", blocked.isEmpty());
        }

        t.finish("NoticeTest");
    }
}
