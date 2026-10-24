package com.winkreyes.excelguide.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * The 0.2.1 learn-cache harness: cache hits avoid the provider and
 * the budget, staleness drops on a knowledge-stamp change, don't-knows
 * and failures never cache, the LRU cap evicts by last hit, the two
 * privacy checks (first-person questions; answers quoting the asker's
 * own fact numbers - including the coincidence case) keep private
 * content out of a cache other players are served from, promoted
 * learned.yml answers outrank cache + AI and survive the AI kill
 * switch, malformed learned files fail soft, and reloads re-read
 * everything. Plus unit coverage of LearnCache, LearnedAnswers,
 * LearnPrivacy, and the new config knobs.
 */
public final class LearnCacheTest {

    static final class MockProvider implements AiProvider {
        final Queue<Object> script = new ArrayDeque<>();
        int calls;

        void reply(String text) {
            script.add(text);
        }

        void fail() {
            script.add(new AiProvider.AiException(
                    AiProvider.AiException.Kind.IO, 0, "scripted failure"));
        }

        @Override
        public AiProvider.Completion complete(AiProvider.Request request)
                throws AiProvider.AiException {
            calls++;
            Object next = script.poll();
            if (next == null) {
                throw new AiProvider.AiException(
                        AiProvider.AiException.Kind.IO, 0, "no scripted reply");
            }
            if (next instanceof AiProvider.AiException e) {
                throw e;
            }
            return new AiProvider.Completion((String) next, 5);
        }
    }

    /** Asker facts WITH digits - the privacy-(b) tripwire set. */
    static final class FakeFacts implements FactsView {
        @Override
        public List<String> askerFacts(UUID player) {
            return List.of(
                    "Claim blocks: accrued 3492, bonus 0, used by claims 1099, remaining 2393",
                    "Balance: $1,234.56",
                    "Playtime: 23 h 40 m",
                    "mcMMO overall 861");
        }

        @Override
        public List<String> publicFacts() {
            return List.of("Guild seals broken: 0 of 5");
        }
    }

    /** Asker facts with NO digits; the numbers live only in public facts. */
    static final class PublicFacts implements FactsView {
        @Override
        public List<String> askerFacts(UUID player) {
            return List.of("Claim blocks: plenty banked for now", "Balance: healthy");
        }

        @Override
        public List<String> publicFacts() {
            return List.of("Guild seals broken: 2 of 5");
        }
    }

    static final class QueueExecutor implements GuideEngine.AiExecutor {
        final List<Runnable> queue = new ArrayList<>();

        @Override
        public void submit(Runnable task) {
            queue.add(task);
        }

        void runAll() {
            List<Runnable> run = new ArrayList<>(queue);
            queue.clear();
            run.forEach(Runnable::run);
        }
    }

    static final class Sink implements GuideEngine.AsyncSink {
        final List<GuideEngine.Reply> got = new ArrayList<>();

        @Override
        public void accept(List<GuideEngine.Reply> replies) {
            got.addAll(replies);
        }

        String text() {
            StringBuilder sb = new StringBuilder();
            for (GuideEngine.Reply r : got) {
                sb.append(r.text()).append('\n');
            }
            return sb.toString();
        }

        void clear() {
            got.clear();
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

    private static GuideEngine engine(GuideConfig cfg, TestSupport.FakeStats stats,
                                      TestSupport.FakeClock clock, KnowledgeBase kb,
                                      MockProvider mock, FactsView facts, AiLog aiLog,
                                      UnansweredLog unanswered, QueueExecutor exec,
                                      LearnCache cache, LearnedAnswers learned) {
        return new GuideEngine(cfg, TestSupport.realIntents(), kb, stats, clock, unanswered,
                mock, facts, aiLog, exec, cache, learned);
    }

    private static String ask(GuideEngine e, GuideEngine.PlayerRef p, String q,
                              GuideEngine.Door door, QueueExecutor exec, Sink sink) {
        sink.clear();
        List<GuideEngine.Reply> direct = e.handle(p, q, door, TestSupport.ONLINE, false, sink);
        exec.runAll();
        StringBuilder sb = new StringBuilder();
        for (GuideEngine.Reply r : direct) {
            sb.append(r.text()).append('\n');
        }
        return sb + sink.text();
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();
        Path dir = Files.createTempDirectory("excelguide-learn");
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        GuideEngine.PlayerRef mike = TestSupport.MIKE;
        GuideEngine.PlayerRef avery = TestSupport.AVERY;
        KnowledgeBase realKb = TestSupport.realKnowledge();
        String q1 = "will the claim blocks cover a 30x30 base";
        String a1 = "Yes - a 30 by 30 base takes 900 claim blocks, comfortably"
                + " inside a normal budget.";

        // ---------- config knobs ----------
        GuideConfig plain = TestSupport.realConfig();
        t.check("cache defaults on", plain.aiCacheEnabled);
        t.eq("cache default file", "ai-cache.yml", plain.aiCacheFile);
        t.eq("cache default max entries", 500, plain.aiCacheMaxEntries);
        t.eq("learned default file", "learned.yml", plain.learnedFile);
        GuideConfig over = GuideConfig.fromYaml(TestSupport.readProj("config.yml")
                .replace("max-entries: 500", "max-entries: 42")
                .replace("    enabled: true\n    file: ai-cache.yml",
                        "    enabled: false\n    file: ai-cache.yml"));
        t.check("cache enabled override parses", !over.aiCacheEnabled);
        t.eq("cache max-entries override parses", 42, over.aiCacheMaxEntries);

        // ---------- LearnPrivacy units ----------
        t.check("personal: my", LearnPrivacy.isPersonalQuestion(Set.of("where", "is", "my", "house")));
        t.check("personal: i", LearnPrivacy.isPersonalQuestion(Set.of("what", "did", "i", "name")));
        t.check("personal: im (i'm stemmed)", LearnPrivacy.isPersonalQuestion(Set.of("im", "lost")));
        t.check("personal: mine", LearnPrivacy.isPersonalQuestion(Set.of("is", "this", "mine")));
        t.check("personal: 'how do i...' counts as first-person (conservative by design)",
                LearnPrivacy.isPersonalQuestion(Set.of("how", "do", "i", "tame", "a", "fox")));
        t.check("no-first-person phrasing is not personal",
                !LearnPrivacy.isPersonalQuestion(Set.of("how", "to", "tame", "a", "fox")));
        t.eq("numbers canonicalize thousands + decimals",
                Set.of("1234.56", "2393"), LearnPrivacy.numbersIn("paid 1,234.56 of 2,393 total"));
        t.check("answer quoting a fact number is caught",
                LearnPrivacy.answerQuotesAny("You have 2393 left", Set.of("2393")));
        t.check("clean answer passes the quote check",
                !LearnPrivacy.answerQuotesAny("Plenty left, traveler", Set.of("2393")));
        t.check("empty fact set never blocks",
                !LearnPrivacy.answerQuotesAny("Any 42 numbers at all", Set.of()));

        // ---------- LearnCache units: LRU + round-trip ----------
        Path cacheFile = dir.resolve("unit-cache.yml");
        LearnCache unit = new LearnCache(cacheFile, 3);
        unit.put("alpha q", "answer a", "s1", 1_000);
        unit.put("bravo q", "answer b", "s1", 2_000);
        unit.put("charlie q", "answer c", "s1", 3_000);
        unit.recordHit("alpha q", 4_000);          // alpha is now freshest
        unit.put("delta q", "answer d", "s1", 5_000); // must evict bravo (oldest last-hit)
        t.eq("cap holds at 3", 3, unit.size());
        t.check("LRU evicted the least-recently-hit", unit.get("bravo q") == null);
        t.check("hit entry survived", unit.get("alpha q") != null);
        t.check("newest entry present", unit.get("delta q") != null);
        t.eq("hit counter rides the entry", 1, unit.get("alpha q").hits());
        unit.recordHit("delta q", 6_000);
        unit.flush();
        LearnCache reloaded = new LearnCache(cacheFile, 3);
        t.eq("round-trip size", 3, reloaded.size());
        t.eq("round-trip answer", "answer d", reloaded.get("delta q").answer());
        t.eq("round-trip stamp", "s1", reloaded.get("delta q").stamp());
        t.eq("round-trip flushed hits", 1, reloaded.get("delta q").hits());
        reloaded.remove("delta q");
        t.eq("remove drops the entry", 2, reloaded.size());
        t.check("remove persisted", new LearnCache(cacheFile, 3).get("delta q") == null);

        Path brokenFile = dir.resolve("broken-cache.yml");
        Files.writeString(brokenFile, "entries: [unclosed", StandardCharsets.UTF_8);
        LearnCache broken = new LearnCache(brokenFile, 10);
        t.eq("malformed cache file loads empty", 0, broken.size());
        t.check("malformed cache file warns", broken.loadWarning() != null);
        t.check("missing cache file is clean",
                new LearnCache(dir.resolve("nope.yml"), 10).loadWarning() == null);

        // ---------- LearnedAnswers units ----------
        String learnedYaml = """
                learned:
                  - id: moon
                    phrases:
                      - "what is the moon made of"
                    answer: "Green cheese, traveler."
                  - phrases:
                      - "how do i feed cats"
                    answer: "Sweet berries and patience."
                  - id: no-answer
                    phrases:
                      - "a phrase with no answer"
                  - id: no-phrases
                    answer: "An answer with no phrases."
                  - "just a string"
                """;
        LearnedAnswers.LoadResult lr = LearnedAnswers.load(learnedYaml);
        t.eq("learned: good entries load", 2, lr.answers().size());
        t.check("learned: skips are warned", lr.warning() != null && lr.warning().contains("3"));
        t.eq("learned: exact phrase matches", "Green cheese, traveler.",
                lr.answers().match("what is the moon made of"));
        t.eq("learned: word order does not matter", "Green cheese, traveler.",
                lr.answers().match("made of moon the is what"));
        t.eq("learned: simple plurals stem together", "Sweet berries and patience.",
                lr.answers().match("how do i feed cat"));
        t.check("learned: different words do not match",
                lr.answers().match("what is the sun made of") == null);
        LearnedAnswers.LoadResult badYaml = LearnedAnswers.load("learned: [unclosed");
        t.eq("learned: syntax-broken file is empty", 0, badYaml.answers().size());
        t.check("learned: syntax-broken file warns", badYaml.warning() != null);
        t.check("learned: blank file is clean-empty",
                LearnedAnswers.load("").answers().size() == 0
                        && LearnedAnswers.load("").warning() == null);
        t.check("learned: comments-only file is clean-empty",
                LearnedAnswers.load("# nothing yet\n").warning() == null);
        t.check("learned: non-list learned key warns",
                LearnedAnswers.load("learned: 42").warning() != null);
        t.check("learned: non-map root warns",
                LearnedAnswers.load("- a\n- b").warning() != null);
        String bundled = TestSupport.readProj("learned.yml");
        LearnedAnswers.LoadResult bundledLoad = LearnedAnswers.load(bundled);
        t.eq("bundled learned.yml ships empty", 0, bundledLoad.answers().size());
        t.check("bundled learned.yml parses clean", bundledLoad.warning() == null);

        // ---------- engine: AI answer caches; a second player is served free ----------
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        GuideConfig cfg = aiConfig();
        Path aiLogPath = dir.resolve("ai.log");
        AiLog aiLog = new AiLog(aiLogPath, 2000);
        UnansweredLog unanswered = new UnansweredLog(dir.resolve("unanswered.log"), 500);
        Path mainCacheFile = dir.resolve("ai-cache.yml");
        LearnCache cache = new LearnCache(mainCacheFile, 500);
        QueueExecutor exec = new QueueExecutor();
        Sink sink = new Sink();
        MockProvider mock = new MockProvider();
        mock.reply(a1);
        GuideEngine e1 = engine(cfg, stats, clock, realKb, mock, new FakeFacts(), aiLog,
                unanswered, exec, cache, LearnedAnswers.empty());

        String first = ask(e1, mike, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("AI answered the first ask", first.contains("900 claim blocks"));
        t.eq("provider called once", 1, mock.calls);
        t.eq("answer cached", 1, e1.aiCacheSize());
        t.eq("one AI call on the budget", 1L, e1.aiCallsToday());
        LearnCache.Entry stored = cache.get(TextNorm.normalize(q1));
        t.check("cache entry keyed by the normalized question", stored != null);
        t.eq("cache entry carries the live knowledge stamp", realKb.version(), stored.stamp());
        t.eq("fresh entry has no hits yet", 0, stored.hits());

        clock.advanceSeconds(61);
        String second = ask(e1, avery, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("second player served the cached answer", second.contains("900 claim blocks"));
        t.eq("cache hit made no provider call", 1, mock.calls);
        t.eq("cache hit spent no AI budget", 1L, e1.aiCallsToday());
        t.eq("cache serves counted", 1L, e1.aiCacheServedCount());
        t.eq("entry hit counter advanced", 1, cache.get(TextNorm.normalize(q1)).hits());

        // Avery's own AI cooldown was NOT burned by the cache hit:
        // a fresh question from her reaches the provider right away.
        clock.advanceSeconds(61);
        mock.reply("Crouch with sweet berries and wait -"
                + " a wild fox that trusts you will eat from your hand.");
        String fox = ask(e1, avery, "how do i tame a fox", GuideEngine.Door.COMMAND, exec, sink);
        t.check("cache hit left the player's AI cooldown untouched",
                fox.contains("sweet berries") && mock.calls == 2);
        // BUT: "how do i..." contains the first-person token, so the
        // privacy gate keeps it OUT of the cache - conservative by
        // design (the stat layer's own personal-question pattern).
        t.eq("first-person phrasing never caches, even a tame fox", 1, e1.aiCacheSize());
        // The same question without a first-person token caches fine,
        // and then serves a third asking with no provider call.
        clock.advanceSeconds(61);
        mock.reply("Crouch with sweet berries and wait -"
                + " a wild fox that trusts you will eat from your hand.");
        ask(e1, avery, "how to tame a fox", GuideEngine.Door.COMMAND, exec, sink);
        t.eq("the impersonal phrasing cached", 2, e1.aiCacheSize());
        String foxCached = ask(e1, mike, "how to tame a fox", GuideEngine.Door.COMMAND, exec, sink);
        t.check("impersonal question serves from cache cross-player",
                foxCached.contains("sweet berries"));
        t.eq("that serve was free", 3, mock.calls);

        // ---------- doors: ambient never serves the cache; /guide off blocks it ----------
        String ambient = ask(e1, avery, q1, GuideEngine.Door.AMBIENT, exec, sink);
        t.check("ambient never serves the cache", !ambient.contains("900 claim blocks"));
        t.eq("ambient made no provider call either", 3, mock.calls);
        e1.setPlayerOff(mike.id(), true);
        String off = ask(e1, mike, q1, GuideEngine.Door.SUMMON, exec, sink);
        t.check("/guide off blocks the cache like everything else",
                !off.contains("900 claim blocks"));
        t.eq("off-toggle serve made no provider call", 3, mock.calls);
        e1.setPlayerOff(mike.id(), false);

        // ---------- don't-knows and failures never cache ----------
        Path cache2File = dir.resolve("cache2.yml");
        GuideEngine e2 = engine(cfg, stats, clock, realKb, mock, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(cache2File, 500), LearnedAnswers.empty());
        mock.reply(GuideEngine.NON_ANSWER);
        String dk = ask(e2, mike, "where is the hidden moon temple", GuideEngine.Door.COMMAND, exec, sink);
        t.check("don't-know falls back to the honest non-answer", dk.contains("don't know that one yet"));
        t.eq("don't-know never caches", 0, e2.aiCacheSize());
        mock.fail();
        clock.advanceSeconds(61);
        String failed = ask(e2, mike, "where is the hidden moon temple", GuideEngine.Door.COMMAND, exec, sink);
        t.check("provider failure falls back too", failed.contains("don't know that one yet"));
        t.eq("failures never cache", 0, e2.aiCacheSize());

        // ---------- privacy (a): first-person questions never cache ----------
        MockProvider mockP = new MockProvider();
        GuideEngine eP = engine(cfg, stats, clock, realKb, mockP, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(dir.resolve("cacheP.yml"), 500),
                LearnedAnswers.empty());
        mockP.reply("You named him Biscuit, after the stable snack.");
        String horse = ask(eP, mike, "what did i name my horse", GuideEngine.Door.COMMAND, exec, sink);
        t.check("personal question still gets its AI answer", horse.contains("Biscuit"));
        t.eq("the AI really was asked (not an intent)", 1, mockP.calls);
        t.eq("first-person answers never cache", 0, eP.aiCacheSize());
        mockP.reply("You named him Biscuit, after the stable snack.");
        String horse2 = ask(eP, avery, "what did i name my horse", GuideEngine.Door.COMMAND, exec, sink);
        t.check("another player re-asks: provider again, no shared cache",
                horse2.contains("Biscuit") && mockP.calls == 2);
        t.eq("still nothing cached", 0, eP.aiCacheSize());

        // ---------- privacy (b): answers quoting the asker's numbers never cache ----------
        MockProvider mockB = new MockProvider();
        GuideEngine eB = engine(cfg, stats, clock, realKb, mockB, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(dir.resolve("cacheB.yml"), 500),
                LearnedAnswers.empty());
        String qB = "will claim blocks cover a 30x30 base";
        mockB.reply("You have 2393 blocks left, so yes, easily.");
        String b1 = ask(eB, mike, qB, GuideEngine.Door.COMMAND, exec, sink);
        t.check("the quoting answer is still served to its asker", b1.contains("2393"));
        t.eq("an answer quoting asker facts never caches", 0, eB.aiCacheSize());
        mockB.reply("You have 2393 blocks left, so yes, easily.");
        ask(eB, avery, qB, GuideEngine.Door.COMMAND, exec, sink);
        t.eq("no cache means the next player pays a provider call", 2, mockB.calls);

        // Adversarial: a GENERAL question whose answer happens to
        // quote the asker's number by coincidence of injection.
        clock.advanceSeconds(61);
        mockB.reply(
                "Worth it - players often bank around 2393 blocks before expanding.");
        String adv = ask(eB, mike, "are big claims worth it", GuideEngine.Door.COMMAND, exec, sink);
        t.check("adversarial answer served", adv.contains("Worth it"));
        t.eq("coincidence quoting still never caches", 0, eB.aiCacheSize());

        // Control: numbers that live ONLY in public facts do not block.
        MockProvider mockPub = new MockProvider();
        GuideEngine ePub = engine(cfg, stats, clock, realKb, mockPub, new PublicFacts(), aiLog,
                unanswered, exec, new LearnCache(dir.resolve("cachePub.yml"), 500),
                LearnedAnswers.empty());
        mockPub.reply("2 of the 5 seals are broken, traveler.");
        String pub = ask(ePub, mike, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("public-facts answer served", pub.contains("seals are broken"));
        t.eq("the AI really was asked", 1, mockPub.calls);
        t.eq("public numbers do not block caching", 1, ePub.aiCacheSize());

        // ---------- staleness: a knowledge-stamp change drops the entry ----------
        Path staleCacheFile = dir.resolve("stale-cache.yml");
        Path staleLogPath = dir.resolve("stale-ai.log");
        MockProvider mockS = new MockProvider();
        mockS.reply(a1);
        GuideEngine eS1 = engine(cfg, stats, clock, realKb, mockS, new FakeFacts(),
                new AiLog(staleLogPath, 2000), unanswered, exec,
                new LearnCache(staleCacheFile, 500), LearnedAnswers.empty());
        ask(eS1, mike, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.eq("stale rig: answer cached under the old stamp", 1, eS1.aiCacheSize());

        KnowledgeBase newKb = new KnowledgeBase(
                List.of(new KnowledgeBase.Chunk("misc", "x", "Trivia", "Nothing relevant here.")),
                "ffffffffffffffffffffffffffffffffffffffff");
        MockProvider mockS2 = new MockProvider();
        mockS2.reply("Fresh answer under the new knowledge.");
        GuideEngine eS2 = engine(cfg, stats, clock, newKb, mockS2, new FakeFacts(),
                new AiLog(staleLogPath, 2000), unanswered, exec,
                new LearnCache(staleCacheFile, 500), LearnedAnswers.empty());
        clock.advanceSeconds(61);
        String fresh = ask(eS2, mike, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("stale entry not served; the AI answers fresh",
                fresh.contains("Fresh answer under the new knowledge"));
        t.eq("stale path called the provider", 1, mockS2.calls);
        LearnCache.Entry recached = new LearnCache(staleCacheFile, 500)
                .get(TextNorm.normalize(q1));
        t.check("fresh answer re-cached", recached != null);
        t.eq("re-cached under the new stamp", newKb.version(), recached.stamp());
        String staleLog = Files.readString(staleLogPath, StandardCharsets.UTF_8);
        t.check("stale drop is logged in ai.log", staleLog.contains("cache=stale-dropped"));
        t.check("cache lines never pose as AI engagements",
                !staleLog.lines().filter(l -> l.contains("cache=")).anyMatch(l -> l.contains("answered=")));
        clock.advanceSeconds(61);
        String hitAgain = ask(eS2, avery, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("re-cached entry serves under the new stamp",
                hitAgain.contains("Fresh answer under the new knowledge"));
        t.eq("that serve was free", 1, mockS2.calls);
        t.check("the hit is logged too",
                Files.readString(staleLogPath, StandardCharsets.UTF_8).contains("cache=hit"));

        // ---------- the AI kill switch stops cache serves; learned still serves ----------
        GuideConfig offCfg = TestSupport.realConfig(); // bundled default: AI off
        GuideEngine eOff = engine(offCfg, stats, clock, realKb, new MockProvider(),
                new FakeFacts(), aiLog, unanswered, exec,
                new LearnCache(mainCacheFile, 500), LearnedAnswers.empty());
        t.check("rig: AI is off in this engine", !eOff.aiActive());
        clock.advanceSeconds(61);
        String offServe = ask(eOff, avery, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.check("kill switch stops cache serves too", !offServe.contains("900 claim blocks"));

        LearnedAnswers moonLearned = LearnedAnswers.load("""
                learned:
                  - phrases:
                      - "what is the moon made of"
                    answer: "Green cheese, traveler."
                """).answers();
        GuideEngine eOffLearned = engine(offCfg, stats, clock, realKb, new MockProvider(),
                new FakeFacts(), aiLog, unanswered, exec,
                new LearnCache(dir.resolve("cacheNone.yml"), 500), moonLearned);
        String moonOff = ask(eOffLearned, avery, "what is the moon made of",
                GuideEngine.Door.COMMAND, exec, sink);
        t.check("promoted answers serve even with the AI off", moonOff.contains("Green cheese"));

        // ---------- promotion outranks cache and AI ----------
        Path promoCacheFile = dir.resolve("promo-cache.yml");
        MockProvider mockPromo = new MockProvider();
        mockPromo.reply("The AI's first draft of the moon answer.");
        GuideEngine ePromo1 = engine(cfg, stats, clock, realKb, mockPromo, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(promoCacheFile, 500), LearnedAnswers.empty());
        ask(ePromo1, mike, "what is the moon made of", GuideEngine.Door.COMMAND, exec, sink);
        t.eq("promotion rig: AI answer cached", 1, ePromo1.aiCacheSize());
        GuideEngine ePromo2 = engine(cfg, stats, clock, realKb, mockPromo, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(promoCacheFile, 500), moonLearned);
        clock.advanceSeconds(61);
        String promoted = ask(ePromo2, avery, "what is the moon made of",
                GuideEngine.Door.COMMAND, exec, sink);
        t.check("promoted answer outranks the cache", promoted.contains("Green cheese"));
        t.check("the cached AI draft was not served",
                !promoted.contains("first draft"));
        t.eq("promotion made no provider call", 1, mockPromo.calls);
        t.eq("learned count reports for status", 1, ePromo2.learnedCount());
        String moonAmbient = ask(ePromo2, avery, "what is the moon made of",
                GuideEngine.Door.AMBIENT, exec, sink);
        t.check("promoted answers never serve ambient", !moonAmbient.contains("Green cheese"));

        // ---------- reload picks up learned.yml edits ----------
        LearnedAnswers v2 = LearnedAnswers.load("""
                learned:
                  - phrases:
                      - "what is the moon made of"
                    answer: "Stardust and valley stone, mostly."
                """).answers();
        GuideEngine eV2 = engine(cfg, stats, clock, realKb, new MockProvider(), new FakeFacts(),
                aiLog, unanswered, exec, new LearnCache(dir.resolve("cacheV2.yml"), 500), v2);
        String edited = ask(eV2, avery, "what is the moon made of",
                GuideEngine.Door.COMMAND, exec, sink);
        t.check("a rebuilt engine serves the edited learned answer",
                edited.contains("Stardust and valley stone"));

        // ---------- cache disabled in config: AI answers, nothing stored ----------
        GuideConfig noCacheCfg = GuideConfig.fromYaml(TestSupport.readProj("config.yml")
                .replace("ai:\n  enabled: false", "ai:\n  enabled: true")
                .replace("api-key: \"\"", "api-key: \"test-key-not-real\"")
                .replace("    enabled: true\n    file: ai-cache.yml",
                        "    enabled: false\n    file: ai-cache.yml"));
        MockProvider mockNC = new MockProvider();
        mockNC.reply(a1);
        GuideEngine eNC = engine(noCacheCfg, stats, clock, realKb, mockNC, new FakeFacts(), aiLog,
                unanswered, exec, new LearnCache(dir.resolve("cacheNC.yml"), 500),
                LearnedAnswers.empty());
        ask(eNC, mike, q1, GuideEngine.Door.COMMAND, exec, sink);
        t.eq("cache disabled: the AI still answers", 1, mockNC.calls);
        t.eq("cache disabled: nothing stored", 0, eNC.aiCacheSize());

        // ---------- ai.log purity for the Monday report ----------
        String mainLog = Files.readString(aiLogPath, StandardCharsets.UTF_8);
        t.check("cache hits are noted in ai.log", mainLog.contains("cache=hit"));
        t.check("real AI answers are logged as answered",
                mainLog.lines().anyMatch(l -> l.contains("answered=true")));
        t.check("no cache line poses as an AI engagement",
                mainLog.lines().filter(l -> l.contains("cache="))
                        .noneMatch(l -> l.contains("answered=")));

        t.finish("LearnCacheTest");
    }
}
