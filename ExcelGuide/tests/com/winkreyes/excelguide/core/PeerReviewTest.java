package com.winkreyes.excelguide.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Peer learning 0.3.0 on the pure core: the question gate, the answer
 * gate (chatter, hedges, name calls, spoilers, profanity, off-topic),
 * one line to one window, real corroboration (2+ players or the
 * asker's thanks; a lone "yeah" and the asker's "yeah" are not
 * enough), disputes, the review queue (never served until approved),
 * approve/reject, "A player said: ..." serving with no names, the
 * serve-time re-check, decay, auto-approve (off by default), the
 * private /ask rule, persistence, and the kill switch.
 * Test players are made-up names (no real players).
 */
public final class PeerReviewTest {

    static final GuideEngine.PlayerRef ASKER = ref("a1", "TestAsker");
    static final GuideEngine.PlayerRef ANS = ref("b2", "Pebble_Fox9");
    static final GuideEngine.PlayerRef AG1 = ref("c3", "MossyCart");
    static final GuideEngine.PlayerRef AG2 = ref("d4", "Lantern_Owl");
    static final GuideEngine.PlayerRef AG3 = ref("e5", ".BirchKite");
    static final List<String> ONLINE = List.of("TestAsker", "Pebble_Fox9", "MossyCart",
            "Lantern_Owl", ".BirchKite");

    static final String FOX_Q = "how do i tame a fox?";
    static final String FOX_A = "crouch near a fox holding sweet berries until it trusts you";
    static final String TRIDENT_Q = "where do i find a trident?";
    static final String TRIDENT_A = "drowned zombies in rivers and oceans sometimes drop a trident";

    static GuideEngine.PlayerRef ref(String hex, String name) {
        String h = (hex + "000000000000000000000000000000").substring(0, 32);
        return new GuideEngine.PlayerRef(UUID.fromString(h.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), name);
    }

    static GuideConfig config(String... replacements) {
        return configFrom(true, replacements);
    }

    /**
     * reviewFirst = true pins peer-learning.auto-approve-thanked: false, the
     * 0.3.0/0.3.1 review-queue flow this harness was written for. 0.3.2's
     * default (true, Mike Oct 25 2026) is checked in section 12 with false.
     */
    static GuideConfig configFrom(boolean reviewFirst, String... replacements) {
        String yaml = TestSupport.readProj("config.yml");
        if (reviewFirst) {
            String from = "auto-approve-thanked: true ";
            if (!yaml.contains(from)) {
                throw new IllegalStateException("config replacement not found: " + from);
            }
            yaml = yaml.replace(from, "auto-approve-thanked: false");
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

    record Rig(GuideEngine engine, PeerCandidates store, PeerLog log, TestSupport.FakeClock clock,
               Path dir, List<String> notices) {}

    static Rig rig(GuideConfig cfg) throws Exception {
        Path dir = Files.createTempDirectory("excelguide-review");
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        PeerCandidates store = new PeerCandidates(dir.resolve("peer-review.yml"), 200);
        PeerLog log = new PeerLog(dir.resolve("peer.log"), 2000);
        GuideEngine e = new GuideEngine(cfg, TestSupport.realIntents(), TestSupport.realKnowledge(),
                new TestSupport.FakeStats(), clock, null, null, null, null, Runnable::run,
                null, null, store, log);
        List<String> notices = new java.util.ArrayList<>();
        e.setPeerNotifier(notices::add);
        return new Rig(e, store, log, clock, dir, notices);
    }

    static List<GuideEngine.Reply> say(Rig r, GuideEngine.PlayerRef who, String line) {
        return r.engine().handle(who, line, GuideEngine.Door.AMBIENT, ONLINE, false);
    }

    static List<GuideEngine.Reply> summon(Rig r, GuideEngine.PlayerRef who, String line) {
        return r.engine().handle(who, line, GuideEngine.Door.SUMMON, ONLINE, false);
    }

    static List<GuideEngine.Reply> ask(Rig r, GuideEngine.PlayerRef who, String line) {
        return r.engine().handle(who, line, GuideEngine.Door.COMMAND, ONLINE, false);
    }

    static void close(Rig r) {
        r.clock().advanceSeconds(301);
        r.engine().tick();
    }

    static String text(List<GuideEngine.Reply> replies) {
        StringBuilder sb = new StringBuilder();
        for (GuideEngine.Reply x : replies) {
            sb.append(x.text()).append('\n');
        }
        return sb.toString();
    }

    static String file(Path p) throws Exception {
        return Files.exists(p) ? Files.readString(p) : "";
    }

    static String q(String raw) {
        return new PeerFilter(PeerFilter.defaults(), 20, 4).questionReject(raw, TextNorm.normalize(raw), ONLINE);
    }

    static String a(String question, String raw) {
        PeerFilter f = new PeerFilter(PeerFilter.defaults(), 20, 4);
        return f.answerReject(raw, AiPrompts.sanitize(raw, 300),
                PeerFilter.meaningful(TextNorm.normalize(question)), 300, ONLINE, "Someone");
    }

    static void eqn(TestSupport.T t, String name, String expected, String actual) {
        t.check(name + " (expected=" + expected + " actual=" + actual + ")",
                java.util.Objects.equals(expected, actual));
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();

        // ---------- 1. question gate ----------
        eqn(t, "fox question opens", null, q(FOX_Q));
        eqn(t, "saddle question opens (one game word is enough)", null, q("how do i get a saddle?"));
        eqn(t, "statement is not a question", "q-not-a-question",
                q("wait so u aimed with the bow and at full charge it hits yourself"));
        eqn(t, "social question", "q-social", q("do you wanna see my diamond house?"));
        eqn(t, "social: have you seen", "q-social", q("have you seen the new shop?"));
        eqn(t, "no game word", "q-no-game-term", q("are we ever getting a structure finder"));
        eqn(t, "spoiler: dragon health", "q-spoiler", q("how much health does the dragon have?"));
        eqn(t, "spoiler: raid timer", "q-spoiler", q("how long is the raid timer?"));
        eqn(t, "spoiler phrase: second phase", "q-spoiler", q("what happens in the second phase of the fight?"));
        eqn(t, "names a player", "q-names-player", q("where did mossycart put the chest?"));
        eqn(t, "names a player by name part", "q-names-player", q("where is owl's iron farm?"));
        eqn(t, "profanity", "q-profanity", q("how the hell do i craft a bed?"));
        eqn(t, "too short", "q-too-short", q("what?"));
        eqn(t, "plain dragon question is not a spoiler", null, q("where do i find the dragon egg?"));

        // ---------- 2. answer gate (the 0.2.3 junk) ----------
        eqn(t, "a real answer passes", null, a(FOX_Q, FOX_A));
        eqn(t, "name call", "too-short", a("why does the bow hit me?", "pebble come back"));
        eqn(t, "name call, longer", "chatter", a("why does the bow hit me?", "pebble come back here right now please"));
        eqn(t, "my brain is fried", "too-short", a("how long did 12 bastions take?", "my brain is fried"));
        eqn(t, "my brain is fried, longer", "chatter", a("how long did 12 bastions take?", "my brain is fried from all those bastions"));
        eqn(t, "answer in other words (2 game words) passes", null,
                a("how do i find a stronghold?", "throw an eye of ender and follow where the eyes go"));
        eqn(t, "i dont think so", "too-short", a("do you have access to the shop?", "i dont think so"));
        eqn(t, "hedge maybe", "chatter", a("how do i find a stronghold?", "uhh maybe the compass works for it"));
        eqn(t, "narration i found", "chatter", a("are we getting a structure finder?", "i found a glitch in the floor"));
        eqn(t, "off-topic", "off-topic", a(FOX_Q, "because spiders like the day very much"));
        eqn(t, "question is not an answer", "question", a(FOX_Q, "did you try sweet berries on it?"));
        eqn(t, "names a player", "names-player", a(FOX_Q, "mossycart knows how to tame foxes with berries"));
        eqn(t, "spoiler answer", "spoiler", a("how do i beat the wither?",
                "the wither gets stronger in its second phase so bring potions"));
        eqn(t, "profanity", "profanity", a(FOX_Q, "just feed the damn fox some sweet berries"));
        eqn(t, "short", "too-short", a(FOX_Q, "berries"));
        eqn(t, "personal stats", "personal", a("how many claim blocks do you get?",
                "you get claim blocks over time, my claim has 400 blocks"));
        t.check("agreement detector: yeah thats right", PeerFilter.isAgreement("yeah thats right", "yeah thats right"));
        t.check("agreement detector: long line is not agreement",
                !PeerFilter.isAgreement("yeah but you also need to crouch next to it for a while",
                        "yeah but you also need to crouch next to it for a while"));
        t.check("asker confirmation: thanks", PeerFilter.isAskerConfirmation("thanks!", "thanks"));
        t.check("asker confirmation: that worked", PeerFilter.isAskerConfirmation("omg that worked", "omg that worked"));
        t.check("asker confirmation: yeah is NOT", !PeerFilter.isAskerConfirmation("yeah", "yeah"));

        // ---------- 3. uncorroborated answer: nothing queued ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            close(r);
            t.eq("lone answer: queue empty", 0, r.store().size());
            t.check("lone answer: log says uncorroborated",
                    file(r.dir().resolve("peer.log")).contains("peer=dropped | reason=uncorroborated"));
        }

        // ---------- 4. one agreement is not enough; two is ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            close(r);
            t.eq("one agreement: queue empty", 0, r.store().size());
        }
        Rig main = rig(config());
        {
            Rig r = main;
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            say(r, ANS, "yes"); // the author's own yes does not count twice
            close(r);
            t.eq("two agreements: one review entry", 1, r.store().count(PeerCandidates.REVIEW));
            t.eq("two agreements: none approved", 0, r.store().count(PeerCandidates.APPROVED));
            t.eq("notifier fired once", 1, r.notices().size());
            PeerCandidates.Entry e = r.engine().peerReviewQueue().get(0);
            t.eq("entry agreements", 2, e.agreements());
            t.eq("entry answer", FOX_A, e.answer());
            // never served while waiting
            String before = text(ask(r, AG3, "how do i tame a fox"));
            t.check("review entry NOT served: " + before, !before.contains("A player said"));
            // approve, then served without a name
            t.check("approve works", r.engine().peerApprove(e.id()) != null);
            r.clock().advanceSeconds(120);
            String after = text(ask(r, AG3, "how do i tame a fox"));
            t.check("approved served as 'A player said': " + after,
                    after.startsWith("A player said: " + FOX_A));
            for (String n : ONLINE) {
                t.check("served text has no name " + n,
                        !after.toLowerCase().contains(TextNorm.normalize(n).replace(" ", "")));
            }
            r.clock().advanceSeconds(120);
            String plural = text(ask(r, AG1, "how do you tame foxes"));
            t.check("re-phrased question matches the same topic: " + plural, plural.startsWith("A player said:"));
            r.engine().flushPeer();
            String yml = file(r.dir().resolve("peer-review.yml"));
            String log = file(r.dir().resolve("peer.log"));
            for (String n : ONLINE) {
                String low = n.toLowerCase();
                t.check("review file has no name " + n, !yml.toLowerCase().contains(low));
                t.check("peer log has no name " + n, !log.toLowerCase().contains(low));
            }
            for (GuideEngine.PlayerRef p : List.of(ASKER, ANS, AG1, AG2, AG3)) {
                t.check("review file has no uuid", !yml.contains(p.id().toString()));
                t.check("peer log has no uuid", !log.contains(p.id().toString()));
            }
            t.check("log: queued + approved + served", log.contains("peer=queued")
                    && log.contains("peer=approved") && log.contains("by=owner") && log.contains("peer=served"));
            // persistence
            PeerCandidates again = new PeerCandidates(r.dir().resolve("peer-review.yml"), 200);
            t.eq("reloaded store keeps the approved entry", 1, again.count(PeerCandidates.APPROVED));
            t.check("reloaded store load clean", again.loadWarning() == null);
            // reject (retire) deletes
            t.check("reject approved works", r.engine().peerReject(e.id()) != null);
            r.clock().advanceSeconds(120);
            t.check("rejected no longer served",
                    !text(ask(r, AG3, "how do i tame a fox")).contains("A player said"));
        }

        // ---------- 5. the asker's thanks counts; the asker's yeah does not ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "yeah");
            close(r);
            t.eq("asker yeah: nothing queued", 0, r.store().size());
        }
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks that worked");
            close(r);
            t.eq("asker thanks: queued", 1, r.store().count(PeerCandidates.REVIEW));
            t.check("how says the asker said it worked",
                    r.engine().peerReviewQueue().get(0).how().contains("asker said it worked"));
        }

        // ---------- 6. disputes kill it ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            say(r, AG3, "no thats wrong");
            close(r);
            t.eq("disputed: nothing queued", 0, r.store().size());
            t.check("log says disputed", file(r.dir().resolve("peer.log")).contains("reason=disputed"));
        }
        {
            // "nope ..." with no answer standing yet is not a dispute: it may be the answer
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, "nope you have to crouch near the fox holding sweet berries");
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            close(r);
            t.eq("a 'nope' first answer is a candidate", 1, r.store().size());
        }

        // ---------- 7. independent similar answers corroborate ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "hold sweet berries and crouch near the fox");
            say(r, AG2, "crouch with sweet berries near the fox slowly");
            close(r);
            t.eq("similar answers count as agreement", 1, r.store().count(PeerCandidates.REVIEW));
        }

        // ---------- 8. ambient question, private /ask, late answers, the asker ----------
        {
            Rig r = rig(config());
            say(r, ASKER, TRIDENT_Q);
            say(r, ASKER, "drowned zombies drop a trident sometimes in rivers"); // asker can't teach
            say(r, ANS, TRIDENT_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "true");
            close(r);
            t.eq("ambient question queued", 1, r.store().count(PeerCandidates.REVIEW));
            t.eq("ambient: the queued answer is the other player's", TRIDENT_A,
                    r.engine().peerReviewQueue().get(0).answer());
        }
        {
            Rig r = rig(config());
            ask(r, ASKER, FOX_Q); // private /ask: nobody else can see it
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            close(r);
            t.eq("private /ask opens no window", 0, r.store().size());
            t.eq("private /ask: no windows", 0, r.engine().peerOpenWindows());
        }
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            r.clock().advanceSeconds(121);
            say(r, ANS, FOX_A); // after the 120 s answer window
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            close(r);
            t.eq("late answer ignored", 0, r.store().size());
        }

        // ---------- 9. one line answers one question ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            summon(r, AG3, "how do i repair my elytra?");
            say(r, ANS, "use an anvil with phantom membrane to repair the elytra");
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            close(r);
            List<PeerCandidates.Entry> qd = r.engine().peerReviewQueue();
            t.eq("only one question got the answer", 1, qd.size());
            t.check("and it is the elytra one", qd.size() == 1 && qd.get(0).question().contains("elytra"));
        }

        // ---------- 10. decay ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks");
            close(r);
            t.eq("decay: queued", 1, r.store().size());
            r.clock().advanceSeconds(10L * 86400);
            say(r, AG3, "how do you tame a fox?"); // a re-ask refreshes the clock
            r.clock().advanceSeconds(10L * 86400);
            r.engine().tick();
            t.eq("decay: re-asked entry survives 20 days", 1, r.store().size());
            r.clock().advanceSeconds(15L * 86400);
            r.engine().tick();
            t.eq("decay: expired after 14 quiet days", 0, r.store().size());
            t.check("log says expired", file(r.dir().resolve("peer.log")).contains("peer=expired"));
        }

        // ---------- 11. serve-time re-check ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks");
            close(r);
            r.engine().peerApprove(r.engine().peerReviewQueue().get(0).id());
            r.engine().flushPeer();
            // Same files, new config: "sweet berries" is now a spoiler term.
            GuideConfig strict = config("  spoiler-terms: []=  spoiler-terms: [\"sweet berries\"]");
            PeerCandidates store = new PeerCandidates(r.dir().resolve("peer-review.yml"), 200);
            GuideEngine e2 = new GuideEngine(strict, TestSupport.realIntents(), TestSupport.realKnowledge(),
                    new TestSupport.FakeStats(), r.clock(), null, null, null, null, Runnable::run,
                    null, null, store, r.log());
            String out = text(e2.handle(AG3, "how do i tame a fox", GuideEngine.Door.COMMAND, ONLINE, false));
            t.check("serve-time spoiler check blocks: " + out, !out.contains("A player said"));
            t.check("log says serve-blocked", file(r.dir().resolve("peer.log")).contains("peer=serve-blocked"));
        }

        // ---------- 12. auto-approve: off by default, opt-in strict ----------
        {
            Rig r = rig(config());
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            say(r, ASKER, "thanks");
            close(r);
            t.eq("default: strong backing still waits for review", 1, r.store().count(PeerCandidates.REVIEW));
        }
        {
            Rig r = rig(config("  auto-approve: false=  auto-approve: true"));
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            say(r, ASKER, "thanks");
            close(r);
            t.eq("auto-approve on + thanks + 2 agreements: approved", 1, r.store().count(PeerCandidates.APPROVED));
        }
        {
            Rig r = rig(config("  auto-approve: false=  auto-approve: true"));
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "yeah thats right");
            say(r, AG2, "yep");
            close(r);
            t.eq("auto-approve on but no thanks: waits", 1, r.store().count(PeerCandidates.REVIEW));
        }

        {
            // 0.3.2 default (Mike, Oct 25 5:18 PM CDT): the asker's thanks approves at once.
            Rig r = rig(configFrom(false));
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks that worked");
            close(r);
            t.eq("0.3.2 default: asker thanks approves right away", 1, r.store().count(PeerCandidates.APPROVED));
            t.eq("0.3.2 default: nothing waits for review", 0, r.store().count(PeerCandidates.REVIEW));
            r.clock().advanceSeconds(120);
            String served = text(ask(r, AG3, "how do i tame a fox"));
            t.check("0.3.2 default: reused on /ask", served.contains("A player said"));
            t.check("0.3.2 default: reused with no names", !served.contains("Pebble") && !served.contains("TestAsker"));
        }
        {
            Rig r = rig(configFrom(false));
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, AG1, "no thats wrong");
            say(r, ASKER, "thanks");
            close(r);
            t.eq("0.3.2 default: a dispute still blocks auto-approve", 0, r.store().count(PeerCandidates.APPROVED));
        }

        // ---------- 13. config: min-agreements floor, defaults, kill switch ----------
        {
            GuideConfig c = config("  min-agreements: 2 =  min-agreements: 1 ");
            t.eq("min-agreements cannot go below 2", 2, c.peerMinAgreements);
            GuideConfig bare = GuideConfig.fromYaml("peer-learning:\n  enabled: true\n");
            t.eq("default review file", "peer-review.yml", bare.peerReviewFile);
            t.check("default auto-approve off", !bare.peerAutoApprove);
            t.eq("default answer window", 120, bare.peerAnswerWindowSeconds);
            t.eq("default expire days", 14, bare.peerCandidateExpireDays);
            t.eq("default prefix", "A player said: ", bare.peerServePrefix);
            Rig r = rig(config("peer-learning:\n  enabled: true=peer-learning:\n  enabled: false"));
            summon(r, ASKER, FOX_Q);
            say(r, ANS, FOX_A);
            say(r, ASKER, "thanks");
            close(r);
            t.eq("kill switch: nothing", 0, r.store().size());
        }

        // ---------- 14. review id lookup ----------
        {
            PeerCandidates.Entry e = PeerCandidates.Entry.review("q", "fox tame", "x", "", 0, true, 1);
            t.eq("id is 6 hex", 6, e.id().length());
            t.eq("id is stable", PeerCandidates.idOf("fox tame"), e.id());
        }

        t.finish("PeerReviewTest");
    }
}
