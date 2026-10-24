package com.winkreyes.excelguide.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 0.3.0: the first-use tip for "A player said" answers, plus clean
 * loading of the empty / "[]" / "{}" files the Oct 24 clear left.
 *  - The tip shows on every player answer until OK, then never again.
 *  - The OK survives a reload (new instance on the same file).
 *  - The Bedrock pop-up goes out once per session; quitting resets that.
 *  - Admin reset brings the tip back.
 *  - peer-tip.yml, peer-review.yml and learned.yml load with NO
 *    warning when empty, "[]" or "{}" (and the live 0.2.3
 *    "entries:"-format peer-answers.yml is never read by 0.3.0).
 *  - The tip text never contains a long dash.
 */
public final class PeerTipTest {

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();
        Path dir = Files.createTempDirectory("peertip");
        Path f = dir.resolve("peer-tip.yml");
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");

        // which replies trigger it
        t.check("prefix detected", PeerTip.isPeerAnswer("A player said: crouch near the fox", "A player said: "));
        t.check("plain answer is not a player answer", !PeerTip.isPeerAnswer("How to level up: ...", "A player said: "));
        t.check("null safe", !PeerTip.isPeerAnswer(null, "A player said: "));

        PeerTip tip = new PeerTip(f);
        t.check("no file: no warning", tip.loadWarning() == null);
        t.check("shows 1st time", tip.shouldShow(a));
        t.check("shows 2nd time (repeats until OK)", tip.shouldShow(a));
        t.check("shows 3rd time", tip.shouldShow(a));
        t.check("pop-up once per session", tip.takeForm(a));
        t.check("no second pop-up this session", !tip.takeForm(a));
        tip.endSession(a);
        t.check("pop-up again next session", tip.takeForm(a));
        t.check("ack is new", tip.ack(a));
        t.check("ack twice is not new", !tip.ack(a));
        t.check("no tip after OK", !tip.shouldShow(a));
        t.check("no pop-up after OK", !tip.takeForm(a));
        t.check("other player still sees it", tip.shouldShow(b));
        t.check("file written", Files.exists(f));
        String body = Files.readString(f, StandardCharsets.UTF_8);
        t.check("file holds only ids, no names", body.contains(a.toString()) && !body.contains(b.toString()));

        // persistence across a reload / restart
        PeerTip again = new PeerTip(f);
        t.check("reload: no warning", again.loadWarning() == null);
        t.check("reload: OK remembered", again.acked(a) && !again.shouldShow(a));
        t.check("reload: b still unacked", !again.acked(b) && again.shouldShow(b));
        t.check("reset brings it back", again.reset(a) && again.shouldShow(a));
        t.check("reset persisted", !new PeerTip(f).acked(a));

        // disabled
        PeerTip off = new PeerTip(dir.resolve("off.yml"));
        off.enabled = false;
        t.check("disabled never shows", !off.shouldShow(a));

        // empty / [] / {} / broken files
        for (String body2 : new String[] {"", "[]", "{}", "# only a comment\n", "acknowledged: []\n"}) {
            Path p = dir.resolve("tip-" + Math.abs(body2.hashCode()) + ".yml");
            Files.writeString(p, body2, StandardCharsets.UTF_8);
            PeerTip x = new PeerTip(p);
            t.check("tip file [" + body2.trim() + "] loads with no warning", x.loadWarning() == null);
            t.check("tip file [" + body2.trim() + "] shows the tip", x.shouldShow(a));
        }
        Path broken = dir.resolve("broken.yml");
        Files.writeString(broken, "acknowledged: [unclosed\n", StandardCharsets.UTF_8);
        PeerTip br = new PeerTip(broken);
        t.check("broken tip file: warning, no crash", br.loadWarning() != null && br.shouldShow(a));

        for (String body3 : new String[] {"", "[]", "{}", "entries: []\n"}) {
            Path p = dir.resolve("review-" + Math.abs(body3.hashCode()) + ".yml");
            Files.writeString(p, body3, StandardCharsets.UTF_8);
            PeerCandidates q = new PeerCandidates(p, 200);
            t.check("review file [" + body3.trim() + "] loads with no warning", q.loadWarning() == null);
            t.eq("review file [" + body3.trim() + "] is empty", 0, q.size());
        }
        for (String body4 : new String[] {"", "[]", "{}", "learned: []\n"}) {
            LearnedAnswers.LoadResult r = LearnedAnswers.load(body4);
            t.check("learned [" + body4.trim() + "] loads with no warning", r.warning() == null);
            t.eq("learned [" + body4.trim() + "] is empty", 0, r.answers().size());
        }

        // the live 0.2.3-format store next to the new files is never read
        Path peer023 = dir.resolve("peer-answers.yml");
        String old = "entries:\n  - question: how do i x\n    answer: y\n";
        Files.writeString(peer023, old, StandardCharsets.UTF_8);
        PeerCandidates q2 = new PeerCandidates(dir.resolve("peer-review.yml"), 200);
        q2.put(PeerCandidates.Entry.review("how do i tame a fox", "fox tame",
                "crouch near the fox holding sweet berries", "2 players agreed", 2, false, 1000L));
        q2.flush();
        t.eq("0.2.3 store untouched by 0.3.0 writes", old, Files.readString(peer023, StandardCharsets.UTF_8));
        PeerCandidates q3 = new PeerCandidates(dir.resolve("peer-review.yml"), 200);
        t.eq("review queue persists across reload", 1, q3.size());

        // text rules
        for (String s : PeerTip.lines(true)) t.check("no long dash: " + s, !s.contains("\u2014") && !s.contains("\u2013"));
        for (String s : PeerTip.lines(false)) t.check("no long dash: " + s, !s.contains("\u2014") && !s.contains("\u2013"));
        t.check("bedrock lines mention /guide ok", PeerTip.lines(true).get(2).contains("/guide ok"));
        t.check("java lines mention [OK]", PeerTip.lines(false).get(2).contains("[OK]"));
        t.check("tip names no seal mechanics", !PeerTip.formContent().toLowerCase().contains("seal"));

        t.finish("PeerTipTest");
    }
}
