package com.winkreyes.excelguide.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Delayed plain-chat answers (0.3.2, Mike's design Oct 25, 2026) on the
 * pure core with a fake clock and a MOCK AI (no network, no real key):
 * research silently, wait the delay, hold off when another player
 * answers, bank a thanked player answer, reuse it next time, post the
 * prepared answer when nobody answers. Also: AI late/early, AI failure
 * and budget, non-questions, limits, cancel, kill switch, disputes,
 * ambiguous thanks, third-party thanks, no names in peer.log.
 * Test players are made-up names (no real players).
 */
public final class DelayedAnswerTest {

    static final GuideEngine.PlayerRef ASKER = PeerReviewTest.ref("a1", "TestAsker");
    static final GuideEngine.PlayerRef ANS = PeerReviewTest.ref("b2", "Pebble_Fox9");
    static final GuideEngine.PlayerRef AG1 = PeerReviewTest.ref("c3", "MossyCart");
    static final GuideEngine.PlayerRef AG2 = PeerReviewTest.ref("d4", "Lantern_Owl");
    static final GuideEngine.PlayerRef NEXT = PeerReviewTest.ref("e5", ".BirchKite");
    static final List<String> ONLINE = PeerReviewTest.ONLINE;

    static final String KNOW_Q = "how do i make a potion?";       // knowledge clears the bar
    static final String INTENT_Q = "how do i set a home?";         // an /ask-only curated answer
    static final String FOX_Q = "how do i tame a fox?";            // nothing local knows it
    static final String FOX_A = PeerReviewTest.FOX_A;
    static final String AI_FOX = "Crouch near a fox while holding sweet berries and it will trust you.";

    static final class Got implements GuideEngine.DelayedSink {
        final List<UUID> to = new ArrayList<>();
        final List<GuideEngine.Reply> replies = new ArrayList<>();

        @Override
        public void deliver(UUID asker, List<GuideEngine.Reply> r) {
            to.add(asker);
            replies.addAll(r);
        }

        String text() {
            StringBuilder sb = new StringBuilder();
            for (GuideEngine.Reply x : replies) {
                sb.append(x.text()).append('\n');
            }
            return sb.toString();
        }

        int deliveries() {
            return to.size();
        }
    }

    record Rig(GuideEngine engine, PeerCandidates store, Path dir, TestSupport.FakeClock clock,
               AiFallbackTest.MockProvider ai, Got got, List<String> warns,
               AiFallbackTest.QueueExecutor queue) {}

    static GuideConfig config(boolean ai, String... replacements) {
        String yaml = TestSupport.readProj("config.yml");
        if (ai) {
            yaml = yaml.replace("ai:\n  enabled: false", "ai:\n  enabled: true");
            yaml = yaml.replace("api-key: \"\"", "api-key: \"test-key-not-real\"");
        }
        for (String r : replacements) {
            int eq = r.indexOf('=');
            String from = r.substring(0, eq);
            if (!yaml.contains(from)) {
                throw new IllegalStateException("config replacement not found: " + from);
            }
            yaml = yaml.replace(from, r.substring(eq + 1));
        }
        return GuideConfig.fromYaml(yaml);
    }

    static Rig rig(GuideConfig cfg) throws Exception {
        return rig(cfg, false, true);
    }

    static Rig rig(GuideConfig cfg, boolean queued, boolean withSink) throws Exception {
        Path dir = Files.createTempDirectory("excelguide-delayed");
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        PeerCandidates store = new PeerCandidates(dir.resolve("peer-review.yml"), 200);
        PeerLog log = new PeerLog(dir.resolve("peer.log"), 2000);
        AiFallbackTest.MockProvider mock = new AiFallbackTest.MockProvider();
        mock.behavior = r -> AI_FOX;
        AiFallbackTest.QueueExecutor q = new AiFallbackTest.QueueExecutor();
        GuideEngine e = new GuideEngine(cfg, TestSupport.realIntents(), TestSupport.realKnowledge(),
                new TestSupport.FakeStats(), clock, new UnansweredLog(dir.resolve("unanswered.log"), 100),
                mock, new AiFallbackTest.FakeFacts(), new AiLog(dir.resolve("ai.log"), 100),
                queued ? q : Runnable::run,
                new LearnCache(dir.resolve("ai-cache.yml"), 100), null, store, log);
        Got got = new Got();
        if (withSink) {
            e.setDelayedSink(got);
        }
        List<String> warns = new ArrayList<>();
        e.setWarnSink(warns::add);
        return new Rig(e, store, dir, clock, mock, got, warns, q);
    }

    static List<GuideEngine.Reply> say(Rig r, GuideEngine.PlayerRef who, String line) {
        return r.engine().handle(who, line, GuideEngine.Door.AMBIENT, ONLINE, false);
    }

    static void wait(Rig r, long seconds) {
        for (long i = 0; i < seconds; i++) {
            r.clock().advanceSeconds(1);
            r.engine().tick(); // the glue ticks every second
        }
    }

    static String file(Path p) throws Exception {
        return Files.exists(p) ? Files.readString(p) : "";
    }

    static String log(Rig r) throws Exception {
        return file(r.dir().resolve("peer.log"));
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();

        // ---------- 1. config: defaults, clamps, off switch ----------
        {
            GuideConfig d = GuideConfig.fromYaml(null);
            t.check("default: on", d.delayedEnabled);
            t.eq("default delay 45s", 45, d.delayedDelaySeconds);
            t.eq("default ai timing late", "late", d.delayedAiTiming);
            t.eq("default reply PUBLIC (Mike, Oct 25 5:18 PM)", "public", d.delayedReplyChannel);
            t.check("default say-dont-know", d.delayedSayDontKnow);
            t.eq("default cooldown 90", 90, d.delayedPerPlayerCooldownSeconds);
            t.eq("default max pending 4", 4, d.delayedMaxPending);
            t.check("default auto-approve-thanked ON (Mike, Oct 25 5:18 PM)", d.peerAutoApproveThanked);
            t.check("default others-thanks-count on", d.peerOthersThanksCount);
            GuideConfig real = TestSupport.realConfig();
            t.eq("bundled config delay", 45, real.delayedDelaySeconds);
            t.check("bundled config auto-approve-thanked on", real.peerAutoApproveThanked);
            t.eq("bundled config reply public", "public", real.delayedReplyChannel);
            t.eq("explicit private parsed", "private",
                    GuideConfig.fromYaml("delayed-answers:\n  reply-channel: PRIVATE\n").delayedReplyChannel);
            t.check("explicit auto-approve-thanked false parsed",
                    !GuideConfig.fromYaml("peer-learning:\n  auto-approve-thanked: false\n").peerAutoApproveThanked);
            GuideConfig c = GuideConfig.fromYaml("delayed-answers:\n  delay-seconds: 2\n  max-pending: 99\n"
                    + "  ai-timing: sideways\n  reply-channel: shout\n  per-player-cooldown-seconds: -5\n");
            t.eq("delay clamped up to 15", 15, c.delayedDelaySeconds);
            t.eq("max pending clamped to 20", 20, c.delayedMaxPending);
            t.eq("bad ai timing -> late", "late", c.delayedAiTiming);
            t.eq("bad channel (typo) -> private, the safer side", "private", c.delayedReplyChannel);
            t.eq("negative cooldown -> 0", 0, c.delayedPerPlayerCooldownSeconds);
            GuideConfig big = GuideConfig.fromYaml("delayed-answers:\n  delay-seconds: 9999\n");
            t.eq("delay clamped down to 300", 300, big.delayedDelaySeconds);
            GuideConfig off = GuideConfig.fromYaml("delayed-answers:\n  enabled: false\n");
            t.check("off switch parsed", !off.delayedEnabled);
        }

        // ---------- 2. knowledge answer: silent now, posted at 45s, once ----------
        {
            Rig r = rig(config(false));
            List<GuideEngine.Reply> now = say(r, ASKER, KNOW_Q);
            t.eq("nothing said at once", 0, now.size());
            t.eq("one question waiting", 1, r.engine().delayedPendingCount());
            wait(r, 44);
            t.eq("not before 45s", 0, r.got().deliveries());
            wait(r, 1);
            t.eq("posted at 45s", 1, r.got().deliveries());
            t.eq("to the asker", ASKER.id(), r.got().to.get(0));
            t.check("the knowledge answer", r.got().text().contains("nether wart"));
            t.check("in public chat (default)", r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
            wait(r, 120);
            t.eq("only once", 1, r.got().deliveries());
            t.eq("nothing waiting after", 0, r.engine().delayedPendingCount());
            t.eq("counter: posted", 1L, r.engine().delayedPostedCount());
            t.check("log: scheduled from knowledge", log(r).contains("delayed-scheduled | source=knowledge"));
            t.check("log: posted", log(r).contains("delayed-posted | source=knowledge"));
            t.eq("AI never called for a knowledge answer", 0, r.ai().invocations);
        }
        {
            Rig r = rig(config(false, "  reply-channel: public=  reply-channel: private"));
            say(r, ASKER, KNOW_Q);
            wait(r, 45);
            t.check("reply-channel private: knowledge goes to the asker only",
                    r.got().replies.size() > 0 && r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, INTENT_Q);
            wait(r, 45);
            t.check("an /ask-only curated answer is used", r.got().text().contains("/sethome"));
        }

        // ---------- 3. a player answers: the Guide holds, thanks banks it ----------
        {
            Rig r = rig(config(true, "auto-approve-thanked: true=auto-approve-thanked: false"));
            say(r, ASKER, FOX_Q);
            wait(r, 10);
            say(r, ANS, FOX_A);
            wait(r, 5);
            say(r, ASKER, "thanks that worked");
            wait(r, 60);
            t.eq("held: the Guide said nothing", 0, r.got().deliveries());
            t.eq("held: AI (late) never called", 0, r.ai().invocations);
            t.eq("counter: held", 1L, r.engine().delayedHeldCount());
            t.check("log: held", log(r).contains("delayed-held"));
            wait(r, 300);
            t.eq("auto-approve-thanked false: banked for review", 1, r.store().count(PeerCandidates.REVIEW));
            t.eq("auto-approve-thanked false: not approved", 0, r.store().count(PeerCandidates.APPROVED));
            // next asker: review queue is never served
            say(r, NEXT, "how do you tame a fox?");
            wait(r, 45);
            t.check("unapproved answer not served", !r.got().text().contains("A player said"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks");
            wait(r, 400);
            t.eq("default: thanked answer approved right away", 1, r.store().count(PeerCandidates.APPROVED));
            t.eq("default: nothing left for review", 0, r.store().count(PeerCandidates.REVIEW));
            t.eq("auto-approve-thanked: Guide held", 0, r.got().deliveries());
            say(r, NEXT, "how do you tame a fox?");
            wait(r, 44);
            t.eq("reuse: still waiting for players", 0, r.got().deliveries());
            wait(r, 1);
            t.check("reuse: next plain-chat asker gets it", r.got().text().contains("A player said: " + FOX_A));
            t.eq("reuse: to the new asker", NEXT.id(), r.got().to.get(0));
            t.check("reuse: in public chat", r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
            t.check("reuse: no player name", !r.got().text().contains("Pebble") && !r.got().text().contains("TestAsker")
                    && !r.got().text().contains("BirchKite"));
            t.check("log: served", log(r).contains("peer=served"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "no thats wrong");
            say(r, ASKER, "thanks");
            wait(r, 400);
            t.eq("auto-approve-thanked: a dispute blocks it", 0, r.store().count(PeerCandidates.APPROVED));
        }

        // ---------- 4. nobody answers: AI late, called once at the due time ----------
        {
            Rig r = rig(config(true));
            say(r, ASKER, FOX_Q);
            wait(r, 44);
            t.eq("AI late: not called during the wait", 0, r.ai().invocations);
            wait(r, 1);
            t.eq("AI late: called once at 45s", 1, r.ai().invocations);
            t.check("AI answer posted", r.got().text().contains(AI_FOX));
            t.check("AI answer in public chat", r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
            wait(r, 60);
            t.eq("AI late: still one call", 1, r.ai().invocations);
            t.eq("AI late: one delivery", 1, r.got().deliveries());
            t.eq("AI budget counted", 1L, r.engine().aiCallsToday());
        }
        {
            // Async executor: the answer lands when the call returns.
            Rig r = rig(config(true), true, true);
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.eq("async: nothing until the call lands", 0, r.got().deliveries());
            r.queue().drain();
            t.check("async: posted when it lands", r.got().text().contains(AI_FOX));
        }
        {
            Rig r = rig(config(true, "  ai-timing: late=  ai-timing: early"), true, true);
            say(r, ASKER, FOX_Q);
            t.eq("AI early: submitted at once", 1, r.queue().queue.size());
            r.queue().drain();
            t.eq("AI early: called", 1, r.ai().invocations);
            t.eq("AI early: still waiting", 0, r.got().deliveries());
            wait(r, 45);
            t.check("AI early: posted at 45s", r.got().text().contains(AI_FOX));
        }
        {
            Rig r = rig(config(true, "  ai-timing: late=  ai-timing: early"), true, true);
            say(r, ASKER, FOX_Q);
            r.queue().drain();
            say(r, ANS, FOX_A);
            wait(r, 60);
            t.eq("AI early + player answered: held anyway", 0, r.got().deliveries());
        }
        {
            // The cached AI answer is reused for the next asker without a call
            // (non-personal wording; "how do i" questions are never cached, as in 0.3.1).
            Rig r = rig(config(true));
            say(r, ASKER, "how do you tame a fox?");
            wait(r, 45);
            say(r, NEXT, "how do you tame a fox?");
            wait(r, 45);
            t.eq("cache: second asker served", 2, r.got().deliveries());
            t.eq("cache: no second AI call", 1, r.ai().invocations);
        }

        // ---------- 5. AI failure, budget, say-dont-know ----------
        {
            Rig r = rig(config(true));
            r.ai().failure = new AiProvider.AiException(AiProvider.AiException.Kind.HTTP, 500, "boom");
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.check("AI failure: the standard non-answer", r.got().text().contains("I don't know that one yet"));
            t.eq("AI failure: one warn line", 1, r.warns().size());
            t.check("AI failure: warn has no key", r.warns().size() == 1 && !r.warns().get(0).contains("test-key-not-real"));
            t.check("AI failure: written down", file(r.dir().resolve("unanswered.log")).contains("tame a fox"));
            t.eq("AI failure counted", 1L, r.engine().aiFailedCount());
        }
        {
            Rig r = rig(config(true, "daily-budget: 50=daily-budget: 1"));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            say(r, NEXT, "where do i find a trident?");
            wait(r, 45);
            t.eq("budget: only one AI call", 1, r.ai().invocations);
            t.check("budget spent: don't-know", r.got().text().contains("I don't know that one yet"));
            t.eq("budget: both askers answered", 2, r.got().deliveries());
        }
        {
            Rig r = rig(config(false, "  say-dont-know: true=  say-dont-know: false"));
            say(r, ASKER, FOX_Q);
            wait(r, 60);
            t.eq("say-dont-know false: silence", 0, r.got().deliveries());
            t.check("say-dont-know false: still written down",
                    file(r.dir().resolve("unanswered.log")).contains("tame a fox"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.check("no AI: don't-know", r.got().text().contains("I don't know that one yet"));
        }

        // ---------- 6. never scheduled ----------
        {
            Rig r = rig(config(true));
            String[] lines = {
                    "i just found a fox in the forest",          // not a question
                    "do you wanna see my diamond house?",         // social
                    "where did mossycart put the chest?",        // names a player
                    "how much health does the dragon have?",     // spoiler
                    "how the hell do i craft a bed?",            // profanity
                    "what?",                                      // too short
                    "lol",
            };
            for (String l : lines) {
                say(r, ASKER, l);
                r.clock().advanceSeconds(100);
            }
            t.eq("non-questions/social/names/spoilers: nothing waiting", 0, r.engine().delayedPendingCount());
            t.eq("never scheduled", 0L, r.engine().delayedScheduledCount());
            wait(r, 60);
            t.eq("never answered", 0, r.got().deliveries());
            t.eq("no AI", 0, r.ai().invocations);
            say(r, ASKER, "how much money do i have?");
            say(r, ANS, "what is my mining level?");
            t.eq("personal stat questions: never delayed (stat intents own them)", 0,
                    r.engine().delayedPendingCount());
            wait(r, 60);
            t.eq("personal stat questions: no delayed post", 0, r.got().deliveries());
        }

        // ---------- 7. limits ----------
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ASKER, KNOW_Q);
            t.eq("one per asker", 1, r.engine().delayedPendingCount());
            wait(r, 45);
            say(r, ASKER, KNOW_Q);
            t.eq("per-player cooldown 90s", 0, r.engine().delayedPendingCount());
            wait(r, 50);
            say(r, ASKER, KNOW_Q);
            t.eq("after the cooldown: waits again", 1, r.engine().delayedPendingCount());
        }
        {
            Rig r = rig(config(false, "  max-pending: 4=  max-pending: 2"));
            say(r, ASKER, FOX_Q);
            say(r, ANS, KNOW_Q);
            say(r, AG1, "where do i find a trident?");
            t.eq("max-pending 2", 2, r.engine().delayedPendingCount());
            t.check("log: busy", log(r).contains("reason=busy"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ANS, "how do you tame a fox?");
            t.eq("same question twice: one wait", 1, r.engine().delayedPendingCount());
            wait(r, 45);
            t.eq("same question twice: one answer", 1, r.got().deliveries());
        }

        // ---------- 8. cancel ----------
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ASKER, "nvm found it");
            wait(r, 60);
            t.eq("nvm: no post", 0, r.got().deliveries());
            t.eq("nvm: counted", 1L, r.engine().delayedCancelledCount());
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            r.engine().handle(ASKER, FOX_Q, GuideEngine.Door.COMMAND, ONLINE, false);
            wait(r, 60);
            t.eq("asked by /ask: the delayed one is dropped", 0, r.got().deliveries());
            t.check("log: asked-directly", log(r).contains("reason=asked-directly"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            r.engine().forgetPlayer(ASKER.id());
            wait(r, 60);
            t.eq("asker left: no post", 0, r.got().deliveries());
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            r.engine().setPlayerOff(ASKER.id(), true);
            wait(r, 60);
            t.eq("toggled off before due: no post", 0, r.got().deliveries());
        }

        // ---------- 9. kill switch and no sink = 0.3.1 behavior ----------
        {
            Rig r = rig(config(false, "delayed-answers:\n  enabled: true=delayed-answers:\n  enabled: false"));
            t.check("off: inactive", !r.engine().delayedActive());
            say(r, ASKER, KNOW_Q);
            wait(r, 60);
            t.eq("off: nothing waiting", 0, r.engine().delayedPendingCount());
            t.eq("off: nothing posted", 0, r.got().deliveries());
        }
        {
            Rig r = rig(config(false), false, false);
            t.check("no sink: inactive", !r.engine().delayedActive());
            say(r, ASKER, KNOW_Q);
            t.eq("no sink: nothing waiting", 0, r.engine().delayedPendingCount());
        }
        {
            Rig r = rig(config(false, "ambient-hint: false=ambient-hint: true"));
            List<GuideEngine.Reply> now = say(r, ASKER, KNOW_Q);
            t.eq("ambient hint suppressed while an answer is scheduled", 0, now.size());
        }

        // ---------- 10. holds and disputes ----------
        {
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            say(r, ANS, "you need nether wart and a brewing stand");
            wait(r, 60);
            t.eq("player reply holds the Guide", 0, r.got().deliveries());
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "no thats wrong");
            wait(r, 60);
            t.eq("disputed player answer: the Guide posts", 1, r.got().deliveries());
        }
        {
            Rig r = rig(config(false, "  hold-on-any-reply: true=  hold-on-any-reply: false"));
            say(r, ASKER, KNOW_Q);
            say(r, ANS, "lol potions are hard");
            wait(r, 60);
            t.eq("hold-on-any-reply false: chatter does not hold", 1, r.got().deliveries());
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            say(r, ANS, "lol");
            say(r, AG1, "yeah");
            wait(r, 60);
            t.eq("bare chatter does not hold", 1, r.got().deliveries());
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ASKER, "thanks guys");
            wait(r, 60);
            t.eq("asker thanked someone: hold", 0, r.got().deliveries());
        }

        {
            // Two questions waiting: an answer that names its topic holds only that one.
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            say(r, NEXT, FOX_Q);
            say(r, ANS, "crouch near the fox holding sweet berries until it trusts you");
            wait(r, 60);
            t.eq("two waiting: the fox one is held, the potion one posts", 1, r.got().deliveries());
            t.check("two waiting: the potion answer went to its asker",
                    r.got().to.size() == 1 && r.got().to.get(0).equals(ASKER.id()));
        }
        {
            // One question waiting: an on-topic game-word reply holds it.
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            say(r, ANS, "grab a brewing stand from the shop");
            wait(r, 60);
            t.eq("one waiting: a game-word reply holds", 0, r.got().deliveries());
        }

        // ---------- 11. thanks after the Guide posted; third-party thanks ----------
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.eq("Guide posted", 1, r.got().deliveries());
            say(r, ANS, FOX_A); // a player answers after the Guide
            say(r, ASKER, "thanks");
            wait(r, 400);
            t.eq("thanks after the Guide posted: not counted", 0, r.store().count(PeerCandidates.APPROVED));
            t.check("log: thanks-ambiguous", log(r).contains("thanks-ambiguous"));
        }
        {
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "thanks");
            say(r, AG2, "yep");
            wait(r, 400);
            t.eq("third-party thanks + yep: 2 agreements, queued", 1, r.store().count(PeerCandidates.REVIEW));
        }
        {
            Rig r = rig(config(false, "others-thanks-count: true=others-thanks-count: false"));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "thanks");
            say(r, AG2, "yep");
            wait(r, 400);
            t.eq("others-thanks-count false: not enough", 0, r.store().size());
        }

        // ---------- 12. privacy: no names in peer.log ----------
        {
            Rig r = rig(config(true));
            say(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks");
            say(r, NEXT, KNOW_Q);
            wait(r, 400);
            String l = log(r);
            for (String n : List.of("TestAsker", "Pebble_Fox9", "MossyCart", "Lantern_Owl", "BirchKite")) {
                t.check("peer.log has no name " + n, !l.toLowerCase().contains(n.toLowerCase()));
            }
            t.check("peer.log has no uuid", !l.contains(ASKER.id().toString()) && !l.contains(NEXT.id().toString()));
            t.check("peer.log has no key", !l.contains("test-key-not-real"));
            t.check("ai.log has no key", !file(r.dir().resolve("ai.log")).contains("test-key-not-real"));
        }

        // ---------- 13. public replies never carry the asker's private details ----------
        // (Mike, Oct 25 5:18 PM CDT: public by default; website knowledge,
        // banked answers and general AI answers keep working; only the
        // asker's personal details are stripped.)
        {
            Rig r = rig(config(true));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            String sys = r.ai().lastSystem == null ? "" : r.ai().lastSystem;
            String user = r.ai().lastUser == null ? "" : r.ai().lastUser;
            String prompt = sys + "\n" + user;
            t.eq("public AI: called once", 1, r.ai().invocations);
            t.check("public AI: general answer still served", r.got().text().contains(AI_FOX));
            t.check("public AI: posted publicly", r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
            t.check("public AI prompt: no asker name", !prompt.contains("TestAsker"));
            t.check("public AI prompt: no asker uuid", !prompt.contains(ASKER.id().toString()));
            t.check("public AI prompt: no asker fact sheet", !prompt.contains("FACTS ABOUT THE PERSON ASKING"));
            for (String secret : List.of("3492", "2393", "1,234.56", "Balance", "Claim blocks", "mcMMO overall 861")) {
                t.check("public AI prompt: no asker fact '" + secret + "'", !prompt.contains(secret));
            }
            t.check("public AI prompt: website knowledge excerpts kept", user.contains("SERVER KNOWLEDGE EXCERPTS:")
                    && !user.contains("(no excerpts matched"));
            t.check("public AI prompt: public facts kept", user.contains("Guild seals broken: 0 of 5"));
            t.check("public AI prompt: the question kept", user.contains("tame a fox"));
            t.check("public AI prompt: told the answer is public", sys.contains("shown to EVERYONE"));
            t.check("public AI prompt: /ask pointer for own numbers", sys.contains("/ask for their own numbers"));
        }
        {
            // Private mode keeps the 0.3.1 prompt (the asker's own facts, privately).
            Rig r = rig(config(true, "  reply-channel: public=  reply-channel: private"));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            String user = r.ai().lastUser == null ? "" : r.ai().lastUser;
            t.check("private AI prompt: asker facts present", user.contains("FACTS ABOUT THE PERSON ASKING (TestAsker)")
                    && user.contains("2393"));
            t.check("private AI answer: private", r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
        }
        {
            // The AI answer quotes one of the asker's own numbers (by coincidence):
            // the asker still gets it, but privately.
            Rig r = rig(config(true));
            r.ai().behavior = q -> "Use sweet berries; you have 2393 blocks left to claim, too.";
            say(r, ASKER, FOX_Q);
            wait(r, 60);
            t.check("AI quotes asker number: still answered", r.got().text().contains("2393"));
            t.check("AI quotes asker number: kept private",
                    r.got().replies.size() > 0 && r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
            t.eq("kept-private counted", 1L, r.engine().delayedKeptPrivateCount());
            t.check("log: kept-private, no text", log(r).contains("delayed-kept-private | reason=asker-detail")
                    && !log(r).contains("2393"));
        }
        {
            // The AI answer names the asker: private.
            Rig r = rig(config(true));
            r.ai().behavior = q -> "Sure TestAsker, crouch near a fox holding sweet berries.";
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.check("AI names asker: kept private",
                    r.got().replies.size() > 0 && r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
        }
        {
            // A Floodgate asker named without the dot: private too.
            Rig r = rig(config(true));
            r.ai().behavior = q -> "BirchKite, crouch near a fox holding sweet berries.";
            say(r, NEXT, FOX_Q);
            wait(r, 45);
            t.check("AI names Floodgate asker (no dot): kept private",
                    r.got().replies.size() > 0 && r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
        }
        {
            // A single-digit number shared with nothing private stays public; so does
            // a general answer with a number the asker does not have.
            Rig r = rig(config(true));
            r.ai().behavior = q -> "Give a fox 3 sweet berries while crouching; it takes about 400 ticks.";
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.check("AI general numbers: public",
                    r.got().replies.size() > 0 && r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
        }
        {
            // Website knowledge with numbers is never pushed private by number coincidence.
            Rig r = rig(config(false));
            say(r, ASKER, KNOW_Q);
            wait(r, 45);
            t.check("knowledge answer: public", r.got().replies.get(0).channel() == GuideEngine.Channel.PUBLIC);
            t.check("knowledge answer: website text", r.got().text().contains("nether wart"));
            t.check("knowledge answer: no asker name", !r.got().text().contains("TestAsker"));
            t.eq("knowledge answer: nothing kept private", 0L, r.engine().delayedKeptPrivateCount());
        }
        {
            // Same question again inside the 60s public question cooldown: the
            // second asker still gets it (from the cache), privately, no repeat line.
            Rig r = rig(config(true));
            say(r, ASKER, "how do you tame a fox?");
            wait(r, 45);
            say(r, NEXT, "how do you tame a fox?");
            wait(r, 45);
            t.eq("repeat in cooldown: both answered", 2, r.got().deliveries());
            t.check("repeat in cooldown: second one private", r.got().replies.size() >= 2
                    && r.got().replies.get(r.got().replies.size() - 1).channel() == GuideEngine.Channel.PRIVATE);
            t.check("log: repeat-private", log(r).contains("delayed-repeat-private"));
        }
        {
            // Cached AI answer reused publicly for the next asker after the cooldown.
            Rig r = rig(config(true));
            say(r, ASKER, "how do you tame a fox?");
            wait(r, 45);
            wait(r, 120);
            say(r, NEXT, "how do you tame a fox?");
            wait(r, 45);
            t.eq("cache reuse: no second AI call", 1, r.ai().invocations);
            t.check("cache reuse: public", r.got().replies.size() >= 2
                    && r.got().replies.get(r.got().replies.size() - 1).channel() == GuideEngine.Channel.PUBLIC);
        }
        {
            // The don't-know line stays private (it is not an answer anyone else needs).
            Rig r = rig(config(false));
            say(r, ASKER, FOX_Q);
            wait(r, 45);
            t.check("don't-know: private", r.got().replies.size() > 0
                    && r.got().replies.get(0).channel() == GuideEngine.Channel.PRIVATE);
        }
        {
            // publicSafe() edge cases: whole-word name match only, uuid, null numbers.
            Rig r = rig(config(false));
            GuideEngine e = r.engine();
            t.check("publicSafe: plain text", e.publicSafe("Use a brewing stand.", ASKER, null));
            t.check("publicSafe: name inside a longer word is a different word",
                    e.publicSafe("TestAskers are fun", ASKER, null));
            t.check("publicSafe: name, any case", !e.publicSafe("hi testasker", ASKER, null));
            t.check("publicSafe: uuid", !e.publicSafe("id " + ASKER.id(), ASKER, null));
            t.check("publicSafe: asker number", !e.publicSafe("you have 1234.56", ASKER,
                    LearnPrivacy.numbersInFacts(new AiFallbackTest.FakeFacts().asker)));
            t.check("publicSafe: digit only ignored", e.publicSafe("wait 0 seconds", ASKER,
                    LearnPrivacy.numbersInFacts(new AiFallbackTest.FakeFacts().asker)));
        }

        t.finish("DelayedAnswerTest");
    }
}
