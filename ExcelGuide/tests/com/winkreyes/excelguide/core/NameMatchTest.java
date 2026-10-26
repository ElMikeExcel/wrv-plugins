package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 0.3.4 short-name and nickname matching for the "talking to a player"
 * gate. Made-up names only (no real players).
 */
public final class NameMatchTest {

    static Set<String> tok(String line) {
        return TextNorm.tokenSet(TextNorm.normalize(line));
    }

    static boolean names(String line, String name, String nick) {
        return NameMatch.matches(tok(line), NameMatch.tokens(name, nick));
    }

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();

        // ---------- 1. token building ----------
        Set<String> q = NameMatch.tokens("QuietOtter42");
        t.check("whole squashed", q.contains("quietotter42"));
        t.check("camel part quiet", q.contains("quiet"));
        t.check("camel part otter", q.contains("otter"));
        t.check("run quietotter", q.contains("quietotter"));
        t.check("digits alone never", !q.contains("42"));
        Set<String> boom = NameMatch.tokens("BoomBoomRocketFox");
        t.check("doubled prefix run", boom.contains("boomboom"));
        t.check("game word part screened (rocket)", !boom.contains("rocket"));
        t.check("game word part screened (fox)", !boom.contains("fox"));
        t.check("run with game word ok", boom.contains("rocketfox"));
        Set<String> big = NameMatch.tokens("BigQuietOtter");
        t.check("common part screened (big)", !big.contains("big"));
        t.check("run bigquiet counts", big.contains("bigquiet"));
        Set<String> und = NameMatch.tokens("Pebble_Moth");
        t.check("underscore parts", und.contains("pebble") && und.contains("moth"));
        Set<String> flood = NameMatch.tokens(".MapleDJumper");
        t.check("floodgate dot dropped", flood.contains("maple") && flood.contains("maplede") == false);
        t.check("acronym hump split", flood.contains("jumper") && !flood.contains("d"));
        Set<String> low = NameMatch.tokens(".sunnyleafkid");
        t.check("one-part lowercase: whole", low.contains("sunnyleafkid"));
        t.check("one-part lowercase: leading 5", low.contains("sunny"));
        t.check("one-part lowercase: leading 4", low.contains("sunn"));
        t.check("one-part lowercase: no 3-letter slice", !low.contains("sun"));
        Set<String> cool = NameMatch.tokens(".coolstarkite");
        t.check("leading slice that is a common word screened", !cool.contains("cool"));
        t.check("longer slice ok", cool.contains("coolst"));
        t.check("short name: 2 letters only whole", NameMatch.tokens("Jo").equals(Set.of("jo")));

        // ---------- 2. nickname cleaning ----------
        t.eq("tilde + colours", "Pebs", NameMatch.cleanDisplay("\u00a7b~Pebs\u00a7r"));
        t.eq("ampersand colours", "Pebs", NameMatch.cleanDisplay("&a~&lPebs"));
        t.eq("bracket tag", "Pebs", NameMatch.cleanDisplay("[Admin] ~Pebs"));
        t.eq("hex colour", "Pebs", NameMatch.cleanDisplay("&#ff00aa~Pebs"));
        t.check("nick tokens", NameMatch.tokens("QuietOtter42", "~Pebs").contains("pebs"));
        t.check("nick that is a common word is ignored", !NameMatch.tokens("QuietOtter42", "~Cool").contains("cool"));
        t.check("nick that is a game word is ignored", !NameMatch.tokens("QuietOtter42", "~Fox").contains("fox"));
        t.check("null nick ok", NameMatch.tokens("QuietOtter42", null).contains("quiet"));

        // ---------- 3. matching: whole words, case-insensitive, possessive ----------
        t.check("short form addressed", names("hey Quiet is your map broken?", "QuietOtter42", null));
        t.check("upper case", names("HEY QUIET is your map broken?", "QuietOtter42", null));
        t.check("possessive", names("is that quiet's house?", "QuietOtter42", null));
        t.check("doubled prefix", names("hey boomboom you ready to do the village raid?", "BoomBoomRocketFox", null));
        t.check("nickname", names("pebs where is the nether portal?", "QuietOtter42", "~Pebs"));
        t.check("word boundary: inside a word no", !names("why is it so quietly spawning?", "QuietOtter42", null));
        t.check("word boundary: prefix of a word no", !names("how do i get pebsxyz", "QuietOtter42", "~Pebs"));
        t.check("common word stays a question", !names("is this cool?", ".coolstarkite", null));
        t.check("game word stays a question", !names("how do i tame a fox?", "BoomBoomRocketFox", null));
        t.check("big stays a question", !names("how big is a stronghold?", "BigQuietOtter", null));
        t.check("sun stays a question", !names("does the sun burn zombies?", ".sunnyleafkid", null));

        // ---------- 4. engine: ambient gate and delayed gate ----------
        GuideEngine.PlayerRef asker = PeerReviewTest.ref("f1", "TestAsker");
        GuideEngine.PlayerRef other = PeerReviewTest.ref("f2", "BoomBoomRocketFox");
        List<String> online = List.of("TestAsker", "BoomBoomRocketFox", "QuietOtter42");
        {
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            var out = r.engine().handle(asker, "hey boomboom is your mini map broken?",
                    GuideEngine.Door.AMBIENT, online, false);
            DelayedAnswerTest.wait(r, 60);
            t.check("ambient short form: silent", out.isEmpty());
            t.eq("ambient short form: AI not called", 0, r.ai().invocations);
            t.check("ambient short form: no delayed answer", r.got().deliveries() == 0);
        }
        {
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            r.engine().noteNicknames(Map.of("QuietOtter42", "\u00a7e~Pebs", "TestAsker", "~Asky"));
            var out = r.engine().handle(asker, "pebs do you have a shield for me?",
                    GuideEngine.Door.AMBIENT, online, false);
            DelayedAnswerTest.wait(r, 60);
            t.check("nickname: silent", out.isEmpty());
            t.eq("nickname: AI not called", 0, r.ai().invocations);
            // the asker's own nickname is not "another player"
            var self = r.engine().handle(asker, "asky here, how do i tame a fox?",
                    GuideEngine.Door.AMBIENT, online, false);
            DelayedAnswerTest.wait(r, 60);
            t.eq("own nickname: still reaches the AI", 1, r.ai().invocations);
        }
        {
            // recently seen (no longer online) nickname still counts at the delayed gate
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            r.engine().noteNicknames(Map.of("QuietOtter42", "~Pebs"));
            r.engine().handle(PeerReviewTest.ref("f3", "QuietOtter42"), "hi all",
                    GuideEngine.Door.AMBIENT, online, false);
            List<String> later = List.of("TestAsker", "BoomBoomRocketFox");
            r.engine().handle(asker, "pebs do you have a shield for me to find?",
                    GuideEngine.Door.AMBIENT, later, false);
            DelayedAnswerTest.wait(r, 60);
            t.eq("recently seen nickname: AI not called", 0, r.ai().invocations);
            t.check("recently seen nickname: logged as names-player",
                    DelayedAnswerTest.log(r).contains("q-names-player"));
        }
        {
            // Essentials missing: empty/no nickname map, plain questions unchanged
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            r.engine().noteNicknames(Map.of());
            r.engine().noteNicknames(null);
            r.engine().handle(asker, "how do i tame a fox?", GuideEngine.Door.AMBIENT, online, false);
            DelayedAnswerTest.wait(r, 60);
            t.eq("no nicknames: real question still reaches the AI", 1, r.ai().invocations);
            t.check("no nicknames: answer delivered", r.got().deliveries() == 1);
        }
        {
            // a nickname cleared on the server stops counting
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            r.engine().noteNicknames(Map.of("QuietOtter42", "~Pebs"));
            r.engine().noteNicknames(Map.of("QuietOtter42", "QuietOtter42"));
            r.engine().handle(asker, "pebs how do i tame a fox?", GuideEngine.Door.AMBIENT, online, false);
            DelayedAnswerTest.wait(r, 60);
            t.eq("cleared nickname: no longer a name", 1, r.ai().invocations);
        }
        {
            // /ask about another player's stats by short form: refused privately
            DelayedAnswerTest.Rig r = DelayedAnswerTest.rig(DelayedAnswerTest.config(true));
            var out = r.engine().handle(asker, "what is boomboom's mining level?",
                    GuideEngine.Door.COMMAND, online, false, x -> {});
            t.check("ask other player's level by short form: refused",
                    !out.isEmpty() && out.get(0).channel() == GuideEngine.Channel.PRIVATE
                            && r.ai().invocations == 0);
        }
        {
            // possessive of other's short form in a peer answer is still screened
            PeerFilter f = new PeerFilter(PeerFilter.defaults(), 12, 3);
            t.check("namesPlayer short form", f.namesPlayer(tok("ask boomboom about it"), List.of("BoomBoomRocketFox")));
            f.setNicknames(Map.of("quietotter42", "Pebs"));
            t.check("namesPlayer nickname", f.namesPlayer(tok("pebs knows"), List.of("QuietOtter42")));
            t.check("namesPlayer plain", !f.namesPlayer(tok("crouch near the fox"), List.of("QuietOtter42", "BoomBoomRocketFox")));
        }
        t.finish("NameMatchTest");
    }
}
