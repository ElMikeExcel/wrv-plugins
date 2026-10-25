package com.winkreyes.excelguide.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.3.1 observability + ambient hint. Proves: a provider failure
 * reaches the console warn sink (kind + HTTP status only, never the
 * key or the question), warnings are throttled to one per 10 min,
 * an ai.log write failure is reported instead of swallowed, success
 * stays quiet, and ai.ambient-hint points a plain-chat asker at /ask
 * once per window, privately, only while the AI is on.
 */
public final class AiObservabilityTest {

    private static GuideConfig cfg(boolean aiOn, boolean hint) {
        String yaml = TestSupport.readProj("config.yml");
        if (aiOn) {
            yaml = yaml.replace("ai:\n  enabled: false", "ai:\n  enabled: true");
            yaml = yaml.replace("api-key: \"\"", "api-key: \"test-key-not-real\"");
        }
        if (hint) {
            yaml = yaml.replace("ambient-hint: false", "ambient-hint: true");
        }
        return GuideConfig.fromYaml(yaml);
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();
        Path dir = Files.createTempDirectory("excelguide-obs");
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        GuideEngine.PlayerRef mike = TestSupport.MIKE;
        GuideEngine.PlayerRef avery = TestSupport.AVERY;
        String q = "will the claim blocks cover a 30x30 base";

        t.check("bundled config ships the hint OFF", !GuideConfig.fromYaml(TestSupport.readProj("config.yml")).aiAmbientHint);
        t.eq("bundled hint window", 24, GuideConfig.fromYaml(TestSupport.readProj("config.yml")).aiAmbientHintHours);
        t.check("hint text has no em dash", GuideEngine.AMBIENT_HINT.indexOf('\u2014') < 0
                && GuideEngine.AMBIENT_HINT.contains("/ask"));

        // ---- 1. provider failure -> one safe console warning, throttled ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            mock.failure = new AiProvider.AiException(AiProvider.AiException.Kind.HTTP, 401, "provider HTTP 401");
            AiLog aiLog = new AiLog(dir.resolve("ai1.log"), 100);
            GuideEngine e = new GuideEngine(cfg(true, false), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, new UnansweredLog(dir.resolve("u1.log"), 100), mock,
                    new AiFallbackTest.FakeFacts(), aiLog, Runnable::run);
            List<String> warns = new ArrayList<>();
            e.setWarnSink(warns::add);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.eq("provider called", 1, mock.invocations);
            t.eq("one warning on first failure", 1, warns.size());
            String w = warns.isEmpty() ? "" : warns.get(0);
            t.check("warning names kind + status", w.contains("HTTP (HTTP 401)"));
            t.check("warning never carries the key", !w.contains("test-key-not-real"));
            t.check("warning never carries the question", !w.contains("30x30") && !w.contains("claim blocks"));
            t.eq("failure counted", 1L, e.aiFailedCount());
            t.check("ai.log still records the failed engagement", aiLog.count() == 1
                    && aiLog.readAll().get(0).contains("answered=false"));
            t.check("player still got the honest non-answer",
                    sink.got.size() == 1 && sink.got.get(0).text().equals(GuideEngine.NON_ANSWER));

            mock.failure = new AiProvider.AiException(AiProvider.AiException.Kind.TIMEOUT, 0, "timeout");
            clock.now += 61_000; // past the AI per-player cooldown, inside the 10 min warn window
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.eq("second failure inside window", 2, mock.invocations);
            t.eq("second failure is throttled", 1, warns.size());
            t.eq("but still counted", 2L, e.aiFailedCount());
            clock.now += 10L * 60_000;
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.eq("after 10 min a new warning", 2, warns.size());
            t.check("timeout warning has no status suffix", warns.size() == 2
                    && warns.get(1).contains("TIMEOUT.") && !warns.get(1).contains("HTTP 0"));
        }

        // ---- 2. success stays quiet ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            GuideEngine e = new GuideEngine(cfg(true, false), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, new UnansweredLog(dir.resolve("u2.log"), 100), mock,
                    new AiFallbackTest.FakeFacts(), new AiLog(dir.resolve("ai2.log"), 100), Runnable::run);
            List<String> warns = new ArrayList<>();
            e.setWarnSink(warns::add);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.check("success answered", sink.got.size() == 1 && sink.got.get(0).text().contains("Foxes"));
            t.eq("success: no warnings", 0, warns.size());
            t.eq("success: no failures", 0L, e.aiFailedCount());
        }

        // ---- 3. ai.log write failure -> warned, answer still delivered ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            Path blocker = dir.resolve("not-a-dir");
            Files.writeString(blocker, "x");
            AiLog broken = new AiLog(blocker.resolve("ai.log"), 100); // parent is a file: append must fail
            GuideEngine e = new GuideEngine(cfg(true, false), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, new UnansweredLog(dir.resolve("u3.log"), 100), mock,
                    new AiFallbackTest.FakeFacts(), broken, Runnable::run);
            List<String> warns = new ArrayList<>();
            e.setWarnSink(warns::add);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.check("answer delivered despite broken log", sink.got.size() == 1 && sink.got.get(0).text().contains("Foxes"));
            t.eq("one ai.log warning", 1, warns.size());
            t.check("ai.log warning names the file setting", !warns.isEmpty() && warns.get(0).contains("could not write the AI log (ai.log)"));
            clock.now += 61_000;
            e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            t.eq("ai.log warning throttled", 1, warns.size());
            // a sink that throws never breaks chat
            e.setWarnSink(m -> { throw new IllegalStateException("boom"); });
            clock.now += 11L * 60_000;
            boolean ok = true;
            try {
                e.handle(mike, q, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            } catch (RuntimeException ex) {
                ok = false;
            }
            t.check("throwing warn sink is contained", ok);
        }

        // ---- 4. ambient hint ----
        String amb = "how do i use the player map";
        {
            // hint ON + AI ON: one private pointer per window
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            UnansweredLog un = new UnansweredLog(dir.resolve("u4.log"), 100);
            GuideEngine e = new GuideEngine(cfg(true, true), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, un, mock, new AiFallbackTest.FakeFacts(),
                    new AiLog(dir.resolve("ai4.log"), 100), Runnable::run);
            AiFallbackTest.Sink sink = new AiFallbackTest.Sink();
            var r1 = e.handle(mike, amb, GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, sink);
            t.eq("ambient miss logged", 1, un.count());
            t.check("hint served privately", r1.size() == 1 && r1.get(0).channel() == GuideEngine.Channel.PRIVATE
                    && r1.get(0).text().equals(GuideEngine.AMBIENT_HINT));
            t.eq("hint never calls the provider", 0, mock.invocations);
            clock.now += 60L * 60_000;
            var r2 = e.handle(mike, amb + " please", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, sink);
            t.check("second miss inside window: silent", r2.isEmpty());
            var r3 = e.handle(avery, amb, GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, sink);
            t.check("other player gets their own hint", r3.size() == 1 && r3.get(0).text().equals(GuideEngine.AMBIENT_HINT));
            clock.now += 24L * 3_600_000L;
            var r4 = e.handle(mike, amb, GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, sink);
            t.check("after the window: hint again", r4.size() == 1);
            var r5 = e.handle(mike, "nice build", GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, sink);
            t.check("non-question chat never hinted", r5.isEmpty());
        }
        {
            // hint ON + AI OFF: silent (no pointer to a door that cannot help)
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            GuideEngine e = new GuideEngine(cfg(false, true), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, new UnansweredLog(dir.resolve("u5.log"), 100), mock,
                    new AiFallbackTest.FakeFacts(), new AiLog(dir.resolve("ai5.log"), 100), Runnable::run);
            var r = e.handle(mike, amb, GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, new AiFallbackTest.Sink());
            t.check("AI off: no hint", r.isEmpty());
        }
        {
            // hint OFF (shipped default) + AI ON: identical to 0.3.0
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
            GuideEngine e = new GuideEngine(cfg(true, false), TestSupport.realIntents(), TestSupport.realKnowledge(),
                    stats, clock, new UnansweredLog(dir.resolve("u6.log"), 100), mock,
                    new AiFallbackTest.FakeFacts(), new AiLog(dir.resolve("ai6.log"), 100), Runnable::run);
            var r = e.handle(mike, amb, GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false, new AiFallbackTest.Sink());
            t.check("hint off: ambient stays silent", r.isEmpty());
        }
        t.finish("AiObservabilityTest");
    }
}
