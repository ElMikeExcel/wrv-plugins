package com.winkreyes.excelguide.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The AI fallback (0.2.0), end to end against a MOCK provider - no
 * network, no real key. Covers: grounded success, the model's own
 * don't-know, provider failure shapes (500 / timeout / 429), the
 * kill switch and the missing key, the AI bucket (per-player
 * cooldown, daily budget, in-flight cap, day roll), markdown
 * stripping, length capping, stat non-interference, ambient
 * exclusion, prompt assembly (fences, facts, pointer rule), the
 * plugin-missing fact sheet, the async delivery shape, and the
 * privacy line (fact fetches only ever name the asker).
 */
public final class AiFallbackTest {

    /** Programmable provider double. Captures the last request. */
    static final class MockProvider implements AiProvider {
        int invocations;
        String lastSystem;
        String lastUser;
        Function<Request, String> behavior = r -> "Foxes settle when you crouch near them with sweet berries in hand.";
        AiException failure;
        TestSupport.FakeClock clockToAdvance;
        long advanceMillis;

        @Override
        public Completion complete(Request request) throws AiException {
            invocations++;
            lastSystem = request.systemPrompt();
            lastUser = request.userPrompt();
            if (clockToAdvance != null) {
                clockToAdvance.now += advanceMillis;
            }
            if (failure != null) {
                throw failure;
            }
            return new Completion(behavior.apply(request), advanceMillis);
        }
    }

    /** Fact source double: preset lines, records every UUID it is asked about. */
    static final class FakeFacts implements FactsView {
        final List<UUID> requested = new ArrayList<>();
        List<String> asker = List.of(
                "Claim blocks: accrued 3492, bonus 0, used by claims 1099, remaining 2393",
                "mcMMO overall 861 (Mining 142, Acrobatics 88)",
                "Balance: $1,234.56");
        List<String> pub = List.of(
                "Guild seals broken: 0 of 5",
                "Land Claimed board leaders: .testkite 1099 blocks claimed");

        @Override
        public List<String> askerFacts(UUID askerId) {
            requested.add(askerId);
            return asker;
        }

        @Override
        public List<String> publicFacts() {
            return pub;
        }
    }

    /** Executor that holds tasks until the test drains it (proves the async shape). */
    static final class QueueExecutor implements GuideEngine.AiExecutor {
        final List<Runnable> queue = new ArrayList<>();

        @Override
        public void submit(Runnable task) {
            queue.add(task);
        }

        void drain() {
            List<Runnable> tasks = new ArrayList<>(queue);
            queue.clear();
            tasks.forEach(Runnable::run);
        }
    }

    /** Sink that records what the async path delivered. */
    static final class Sink implements GuideEngine.AsyncSink {
        final List<GuideEngine.Reply> got = new ArrayList<>();

        @Override
        public void accept(List<GuideEngine.Reply> replies) {
            got.addAll(replies);
        }
    }

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

    private static GuideEngine aiEngine(GuideConfig cfg, TestSupport.FakeStats stats,
                                        TestSupport.FakeClock clock, MockProvider mock,
                                        FakeFacts facts, AiLog aiLog, UnansweredLog unanswered,
                                        GuideEngine.AiExecutor exec) {
        return new GuideEngine(cfg, TestSupport.realIntents(), TestSupport.realKnowledge(),
                stats, clock, unanswered, mock, facts, aiLog, exec);
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();
        Path dir = Files.createTempDirectory("excelguide-ai");
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        GuideEngine.PlayerRef mike = TestSupport.MIKE;
        GuideEngine.PlayerRef avery = TestSupport.AVERY;
        GuideEngine.PlayerRef rowan = TestSupport.ROWAN;

        // The rig proof question must genuinely fall through the
        // deterministic engine on the real corpus (claim-land's
        // [claim, my] group catches most phrasings of it).
        String baseQ = "will the claim blocks cover a 30x30 base";
        String norm = TextNorm.normalize(baseQ);
        Retrieval.Result rr = Retrieval.search(TestSupport.realKnowledge(), norm, null);
        IntentSet.Match bm = TestSupport.realIntents().match(norm, TextNorm.tokenSet(norm), false, false);
        t.check("proof question clears no intent and no retrieval bar",
                (bm == null || bm.score() < 0.72)
                        && !Retrieval.clearsBar(rr, 8, 1.5));

        // ---- 1. Grounded success, inline executor ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            MockProvider mock = new MockProvider();
            mock.clockToAdvance = clock;
            mock.advanceMillis = 1500;
            FakeFacts facts = new FakeFacts();
            AiLog aiLog = new AiLog(dir.resolve("ai1.log"), 100);
            UnansweredLog unanswered = new UnansweredLog(dir.resolve("un1.log"), 100);
            GuideEngine e = aiEngine(aiConfig(), stats, clock, mock, facts, aiLog, unanswered,
                    Runnable::run);
            Sink sink = new Sink();
            var sync = e.handle(mike, baseQ, GuideEngine.Door.COMMAND, TestSupport.ONLINE, false, sink);
            // 0.2.2: engagement returns the thinking line synchronously
            // (the answer itself still arrives via the sink).
            t.check("ai engaged: thinking line served synchronously", sync.size() == 1
                    && sync.get(0).channel() == GuideEngine.Channel.PRIVATE
                    && sync.get(0).text().equals("Hmm, let me think about that one…"));
            t.eq("provider called once", 1, mock.invocations);
            t.check("answer delivered via sink", sink.got.size() == 1
                    && sink.got.get(0).text().contains("Foxes settle"));
            t.eq("ai answer is private", GuideEngine.Channel.PRIVATE, sink.got.get(0).channel());
            t.eq("unanswered log untouched on success", 0, unanswered.count());
            t.eq("ai log has the engagement", 1, aiLog.count());
            t.check("ai log records answered + latency",
                    aiLog.readAll().get(0).contains("answered=true")
                            && aiLog.readAll().get(0).contains("ms=1500"));
            t.check("answers served counted the ai answer", e.answersServed() >= 1);
            t.eq("ai answered counter", 1L, e.aiAnsweredCount());

            // Prompt assembly: fences, facts, pointer rule, the exact don't-know line.
            t.check("system prompt fences invention", mock.lastSystem.contains("Never invent numbers"));
            t.check("system prompt carries the exact dont-know line",
                    mock.lastSystem.contains(GuideEngine.NON_ANSWER));
            t.check("system prompt carries the pointer rule",
                    mock.lastSystem.contains("point there first")
                            && mock.lastSystem.contains("Leaderboard"));
            t.check("user prompt fences the fact sheet",
                    mock.lastUser.contains("FACTS ABOUT THE PERSON ASKING (.OwnerTest)"));
            t.check("fact sheet carries the claim numbers", mock.lastUser.contains("remaining 2393"));
            t.check("public facts present", mock.lastUser.contains("Guild seals broken: 0 of 5")
                    && mock.lastUser.contains("Land Claimed board leaders"));
            t.check("question present", mock.lastUser.contains("QUESTION FROM .OwnerTest: " + baseQ));
            t.check("excerpts section present", mock.lastUser.contains("SERVER KNOWLEDGE EXCERPTS:"));
            t.eq("facts fetched for the asker only", List.of(mike.id()), facts.requested);
        }

        // ---- 2. The model's own don't-know (plain and dressed) ----
        int iter = 0;
        for (String dressed : new String[]{GuideEngine.NON_ANSWER,
                "**I don't know that one yet.** It is probably on the website."}) {
            iter++;
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            MockProvider mock = new MockProvider();
            mock.behavior = r -> dressed;
            AiLog aiLog = new AiLog(dir.resolve("ai2-" + iter + ".log"), 100);
            UnansweredLog unanswered = new UnansweredLog(dir.resolve("un2-" + iter + ".log"), 100);
            GuideEngine e = aiEngine(aiConfig(), stats, clock, mock, new FakeFacts(), aiLog,
                    unanswered, Runnable::run);
            Sink sink = new Sink();
            e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("dont-know model reply serves the standard non-answer",
                    sink.got.size() == 1 && sink.got.get(0).text().equals(GuideEngine.NON_ANSWER));
            t.eq("dont-know still lands in unanswered log", 1, unanswered.count());
            t.check("ai log marks it unanswered", aiLog.readAll().get(0).contains("answered=false"));
        }

        // ---- 3. Provider failure shapes: 500, timeout, 429 ----
        int fiter = 0;
        for (AiProvider.AiException failure : new AiProvider.AiException[]{
                new AiProvider.AiException(AiProvider.AiException.Kind.HTTP, 500, "x"),
                new AiProvider.AiException(AiProvider.AiException.Kind.TIMEOUT, 0, "x"),
                new AiProvider.AiException(AiProvider.AiException.Kind.RATE_LIMIT, 429, "x")}) {
            fiter++;
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            MockProvider mock = new MockProvider();
            mock.failure = failure;
            AiLog aiLog = new AiLog(dir.resolve("ai3-" + fiter + ".log"), 100);
            UnansweredLog unanswered = new UnansweredLog(dir.resolve("un3-" + fiter + ".log"), 100);
            GuideEngine e = aiEngine(aiConfig(), stats, clock, mock, new FakeFacts(), aiLog,
                    unanswered, Runnable::run);
            Sink sink = new Sink();
            e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("failure " + failure.kind() + " serves the standard non-answer",
                    sink.got.size() == 1 && sink.got.get(0).text().equals(GuideEngine.NON_ANSWER));
            t.eq("failure " + failure.kind() + " logged as unanswered", 1, unanswered.count());
            t.check("failure " + failure.kind() + " in ai log as unanswered",
                    aiLog.readAll().get(0).contains("answered=false"));
        }

        // ---- 4. Kill switch / no key / no sink: provider never invoked ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            MockProvider mock = new MockProvider();
            GuideConfig off = GuideConfig.fromYaml(TestSupport.readProj("config.yml")
                    .replace("api-key: \"\"", "api-key: \"test-key-not-real\"")); // key but enabled:false
            GuideEngine e1 = aiEngine(off, stats, clock, mock, new FakeFacts(), null, null,
                    Runnable::run);
            Sink sink = new Sink();
            var r1 = e1.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("kill switch: sync non-answer", r1.size() == 1
                    && r1.get(0).text().equals(GuideEngine.NON_ANSWER));
            t.eq("kill switch: provider untouched", 0, mock.invocations);

            GuideConfig noKey = GuideConfig.fromYaml(TestSupport.readProj("config.yml")
                    .replace("ai:\n  enabled: false", "ai:\n  enabled: true")); // enabled but no key
            GuideEngine e2 = aiEngine(noKey, stats, clock, mock, new FakeFacts(), null, null,
                    Runnable::run);
            var r2 = e2.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            t.check("no key: sync non-answer", r2.size() == 1);
            t.eq("no key: provider untouched", 0, mock.invocations);

            GuideEngine e3 = aiEngine(aiConfig(), stats, clock, mock, new FakeFacts(), null, null,
                    Runnable::run);
            var r3 = e3.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false); // no sink overload
            t.check("no sink: sync non-answer", r3.size() == 1);
            t.eq("no sink: provider untouched", 0, mock.invocations);
        }

        // ---- 5. The AI bucket: cooldown, budget, in-flight, day roll ----
        {
            TestSupport.FakeClock clock = new TestSupport.FakeClock();
            MockProvider mock = new MockProvider();
            GuideEngine e = aiEngine(aiConfig(), stats, clock, mock, new FakeFacts(), null, null,
                    Runnable::run);
            Sink sink = new Sink();
            e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.eq("first question engages", 1, mock.invocations);
            var second = e.handle(mike, "how do i breed turtles", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.eq("same player inside the ai cooldown does not engage", 1, mock.invocations);
            // 0.2.2 (was: "the plain sync non-answer"): an AI-bucket
            // refusal on the player's own cooldown now serves the
            // cooldown notice quoting the real wait - the don't-know
            // is not true yet, and silence is never good with kids.
            // NoticeTest pins the notice behavior end to end.
            t.check("cooldown refusal is the timed notice, not the non-answer",
                    second.size() == 1 && second.get(0).text().equals(
                            "One moment — I can answer again in about 60 seconds."));
            clock.advanceSeconds(61);
            e.handle(mike, "how do i breed turtles", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.eq("after the ai cooldown it engages again", 2, mock.invocations);

            // Daily budget = 2, three different players.
            MockProvider mock2 = new MockProvider();
            GuideEngine eb = aiEngine(aiConfig("daily-budget: 50=daily-budget: 2"), stats,
                    new TestSupport.FakeClock(), mock2, new FakeFacts(), null, null, Runnable::run);
            eb.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            eb.handle(avery, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            var third = eb.handle(rowan, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            t.eq("daily budget caps engagements", 2, mock2.invocations);
            t.check("budget refusal is the plain sync non-answer", third.size() == 1
                    && third.get(0).text().equals(GuideEngine.NON_ANSWER));

            // In-flight cap: queued executor, two engaged, third refused.
            MockProvider mock3 = new MockProvider();
            QueueExecutor queue = new QueueExecutor();
            GuideEngine eq = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mock3,
                    new FakeFacts(), null, null, queue);
            eq.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            eq.handle(avery, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            var refused = eq.handle(rowan, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, new Sink());
            t.check("third concurrent call refused while two in flight", refused.size() == 1);
            queue.drain();
            t.eq("the two in flight completed after drain", 2, mock3.invocations);

            // Gate day roll: budget frees on the next local day.
            AiGate gate = new AiGate(60, 1, java.time.ZoneId.of("UTC"));
            long t0 = 1_700_000_000_000L;
            t.check("gate first reserve ok", gate.tryReserve(mike.id(), t0));
            gate.release();
            t.check("gate budget spent", !gate.tryReserve(avery.id(), t0 + 1000));
            t.eq("gate resets next day", 0, gate.usedToday(t0 + 25 * 3600_000L));
        }

        // ---- 6. Output hygiene: markdown stripped, length capped ----
        {
            MockProvider mock = new MockProvider();
            mock.behavior = r -> "## Foxes\n**Crouch** with [sweet berries](https://example.com/x) "
                    + "and *wait* - see https://example.com for more.";
            GuideEngine e = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mock,
                    new FakeFacts(), null, null, Runnable::run);
            Sink sink = new Sink();
            e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            String text = sink.got.get(0).text();
            t.check("markdown stripped", !text.contains("**") && !text.contains("#")
                    && !text.contains("[") && !text.contains("http") && !text.contains("*"));
            t.check("link text kept, target gone", text.contains("sweet berries"));

            MockProvider mockLong = new MockProvider();
            mockLong.behavior = r -> "word ".repeat(400);
            GuideEngine e2 = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mockLong,
                    new FakeFacts(), null, null, Runnable::run);
            Sink sink2 = new Sink();
            e2.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink2);
            int total = sink2.got.stream().mapToInt(rp -> rp.text().length()).sum();
            t.check("overlong answer capped", total <= 400
                    && sink2.got.stream().allMatch(rp -> rp.text().length() <= 200));
        }

        // ---- 7. Stat non-interference + the privacy line ----
        {
            MockProvider mock = new MockProvider();
            mock.behavior = r -> "INVENTED NONSENSE 999999";
            FakeFacts facts = new FakeFacts();
            TestSupport.FakeClock clock7 = new TestSupport.FakeClock();
            GuideEngine e = aiEngine(aiConfig(), stats, clock7, mock,
                    facts, null, null, Runnable::run);
            Sink sink = new Sink();
            var balance = e.handle(mike, "what is my balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("stat question answered deterministically", balance.size() == 1
                    && balance.get(0).text().contains("Balance"));
            t.eq("stat question never reaches the provider", 0, mock.invocations);
            t.check("no async delivery for a stat question", sink.got.isEmpty());
            t.check("no facts fetched for a stat question", facts.requested.isEmpty());

            clock7.advanceSeconds(65); // past the per-player chat limiter
            var refusal = e.handle(mike, "what is testkite's balance", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("other-player stat still refused", refusal.size() == 1
                    && refusal.get(0).text().contains("theirs to share"));
            t.eq("refusal never reaches the provider", 0, mock.invocations);
            t.check("facts never fetched for another player",
                    !facts.requested.contains(avery.id()));
        }

        // ---- 8. Ambient exclusion ----
        {
            MockProvider mock = new MockProvider();
            GuideEngine e = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mock,
                    new FakeFacts(), null, null, Runnable::run);
            Sink sink = new Sink();
            var ambient = e.handle(mike, "how do i use the player map", GuideEngine.Door.AMBIENT,
                    TestSupport.ONLINE, false, sink);
            t.check("ambient unknown stays silent", ambient.isEmpty() && sink.got.isEmpty());
            t.eq("ambient never reaches the provider", 0, mock.invocations);
            var summon = e.handle(mike, "how do i tame a fox", GuideEngine.Door.SUMMON,
                    TestSupport.ONLINE, false, sink);
            t.check("summon door does engage", summon.size() == 1
                    && summon.get(0).text().equals("Hmm, let me think about that one…")
                    && sink.got.size() == 1);
            t.eq("summon reached the provider", 1, mock.invocations);
        }

        // ---- 9. Plugin-missing fact sheet still assembles ----
        {
            MockProvider mock = new MockProvider();
            FakeFacts empty = new FakeFacts();
            empty.asker = List.of();
            empty.pub = List.of();
            GuideEngine e = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mock,
                    empty, null, null, Runnable::run);
            Sink sink = new Sink();
            e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            t.check("empty fact sheet still answers", sink.got.size() == 1);
            t.check("empty sheet marked in the prompt",
                    mock.lastUser.contains("(no live facts available right now)"));
        }

        // ---- 10. Async shape: nothing until the executor runs ----
        {
            MockProvider mock = new MockProvider();
            QueueExecutor queue = new QueueExecutor();
            GuideEngine e = aiEngine(aiConfig(), stats, new TestSupport.FakeClock(), mock,
                    new FakeFacts(), null, null, queue);
            Sink sink = new Sink();
            var sync = e.handle(mike, "how do i tame a fox", GuideEngine.Door.COMMAND,
                    TestSupport.ONLINE, false, sink);
            // 0.2.2: the thinking line is synchronous; the ANSWER is not.
            t.check("engaged call returns only the thinking line synchronously",
                    sync.size() == 1
                            && sync.get(0).text().equals("Hmm, let me think about that one…"));
            t.check("nothing delivered before the task runs", sink.got.isEmpty());
            t.eq("provider not called before the task runs", 0, mock.invocations);
            queue.drain();
            t.check("answer arrives when the task runs", sink.got.size() == 1);
        }

        t.finish("AiFallbackTest");
    }
}
