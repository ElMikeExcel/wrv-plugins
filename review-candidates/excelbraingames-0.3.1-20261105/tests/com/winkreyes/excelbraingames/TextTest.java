package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Payouts;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.RiddleText;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;

import java.util.List;
import java.util.Random;

/**
 * Text-trio unit/logic tests (SPEC §6–§8): Quick Math payout rounding
 * and one-attempt flow, Scramble earning/hint/kid-retry rules and
 * finish-early banking, Riddle fuzzy matching + hint-cost math, the
 * kid-tier half rule on text-game values, and mid-round serialization.
 * Generator invariants across seeds live in TextPropertyTest.
 */
public final class TextTest {

    public static void main(String[] args) {
        Check ck = new Check("TextTest");
        quickMathFlow(ck);
        quickMathPayoutMath(ck);
        scrambleFlow(ck);
        scrambleHintAndFinish(ck);
        riddleMatching(ck);
        riddlePayoutMath(ck);
        kidHalfOnTextGames(ck);
        serialization(ck);
        ck.finish();
    }

    static List<String> pool() {
        return List.of("CAT", "DOG", "FISH", "BIRD", "BEAR",
                "APPLE", "BANANA", "ORANGE", "CASTLE", "DRAGON", "PUZZLE", "ROCKET");
    }

    static void quickMathFlow(Check ck) {
        QuickMathRound r = QuickMathRound.generate("standard", false, false, new Random(42), 1_000L);
        ck.eq(10, r.prompts().size(), "qm: a round is 10 questions");
        ck.that(!r.finished() && r.index() == 0 && r.correct() == 0, "qm: fresh round state");
        int firstAnswer = r.currentAnswer();
        ck.that(r.answer(firstAnswer), "qm: the right answer scores");
        ck.eq(1, r.correct(), "qm: correct counter moves");
        ck.eq(1, r.index(), "qm: index advances on a right answer");
        int wrongValue = r.currentAnswer() + 1;
        ck.that(!r.answer(wrongValue), "qm: a wrong answer does not score");
        ck.eq(2, r.index(), "qm: index advances on a wrong answer too (one attempt, SPEC 6.2)");
        ck.eq(1, r.correct(), "qm: correct counter unchanged by the miss");
        while (!r.finished()) r.answer(r.currentAnswer());
        ck.eq(9, r.correct(), "qm: finished with 9 of 10");
        ck.that(!r.answer(0), "qm: answers after the round ends are refused");
        // Kid choices: three distinct options, one of them the answer.
        QuickMathRound kid = QuickMathRound.generate("kid", true, true, new Random(7), 1_000L);
        for (int i = 0; i < 10; i++) {
            List<Integer> ch = kid.currentChoices();
            ck.eq(3, ch.size(), "qm kid: three choices on question " + (i + 1));
            ck.that(ch.contains(kid.currentAnswer()), "qm kid: choices contain the answer");
            ck.eq(3, ch.stream().distinct().count(), "qm kid: choices are distinct");
            kid.answer(kid.currentAnswer());
        }
    }

    static void quickMathPayoutMath(Check ck) {
        // SPEC 6.3: tier x correct/10, rounded DOWN to whole dollars.
        ck.eq(5_000, QuickMathRound.payoutCents(5_000, 10), "qm pay: 10/10 on $50 = $50");
        ck.eq(4_500, QuickMathRound.payoutCents(5_000, 9), "qm pay: 9/10 on $50 = $45");
        ck.eq(1_700, QuickMathRound.payoutCents(2_500, 7), "qm pay: 7/10 on $25 = $17.50 -> $17");
        ck.eq(0, QuickMathRound.payoutCents(9_000, 0), "qm pay: 0/10 pays $0");
        ck.eq(8_100, QuickMathRound.payoutCents(9_000, 9), "qm pay: 9/10 on $90 = $81");
        ck.eq(200, QuickMathRound.payoutCents(2_500, 1), "qm pay: 1/10 on $25 = $2.50 -> $2");
    }

    static void scrambleFlow(Check ck) {
        ScrambleRound std = ScrambleRound.generate("standard", false, 2_000, pool(), new Random(3), 1_000L);
        ck.that(std != null, "scramble: standard round generates from the pool");
        ck.eq(5, std.words().size(), "scramble: a round is 5 words");
        String w0 = std.currentWord();
        ck.that(!std.currentScrambled().equals(w0), "scramble: the shuffle is never the word");
        ck.that(std.currentScrambled().length() == w0.length(), "scramble: the shuffle keeps the length");
        char[] a = std.currentScrambled().toCharArray(), b = w0.toCharArray();
        java.util.Arrays.sort(a); java.util.Arrays.sort(b);
        ck.that(java.util.Arrays.equals(a, b), "scramble: the shuffle is a letter permutation of the word");
        ck.eq(0, std.answer("definitely wrong"), "scramble std: a wrong typed answer scores 0");
        ck.eq(1, std.index(), "scramble std: the wrong answer consumed the word");
    }

    static void scrambleHintAndFinish(Check ck) {
        // Standard: one attempt per word; hint halves that word; partial credit banks.
        ScrambleRound std = ScrambleRound.generate("standard", false, 2_000, pool(), new Random(11), 1_000L);
        ck.eq(0, std.answer("zzzz"), "scramble std: wrong answer earns nothing");
        ck.eq(1, std.index(), "scramble std: a wrong answer still advances (one attempt)");
        ck.that(std.useHint(), "scramble std: hint arms on a fresh word");
        ck.that(!std.useHint(), "scramble std: hint does not double-arm");
        ck.eq(std.currentWord().charAt(0), std.hintLetter(), "scramble std: hint letter is the first letter");
        ck.eq(1, std.answer(std.currentWord()), "scramble std: solving the hinted word scores");
        ck.eq(1_000, std.earnedCents(), "scramble std: hinted word pays HALF ($10 of $20)");
        ck.eq(1, std.answer(std.currentWord().toLowerCase()), "scramble std: answers are case-insensitive");
        ck.eq(3_000, std.earnedCents(), "scramble std: clean word pays full $20 (total $30 banked)");
        int idxAtFinish = std.index();
        ck.that(idxAtFinish < 5, "scramble: finish-early happens mid-round");
        ck.eq(3_000, std.earnedCents(), "scramble: finish-now banks exactly what was earned (SPEC 7.3)");
        // Kid: retries until correct, full value, no hint control.
        ScrambleRound kid = ScrambleRound.generate("kid", true, 1_000, pool(), new Random(5), 1_000L);
        ck.that(kid != null, "scramble kid: round generates (3-4 letter band)");
        ck.eq(3, kid.currentChoices().size(), "scramble kid: three choices shown");
        ck.that(kid.currentChoices().contains(kid.currentWord()), "scramble kid: choices contain the word");
        ck.that(!kid.useHint(), "scramble kid: no hint control on the kid tier");
        ck.eq(0, kid.answer("WRONGONE"), "scramble kid: a wrong tap earns nothing");
        ck.eq(0, kid.index(), "scramble kid: a wrong tap does NOT advance (retries, SPEC 7.2)");
        ck.eq(1, kid.answer(kid.currentWord()), "scramble kid: the right tap scores");
        ck.eq(1_000, kid.earnedCents(), "scramble kid: full $10 for the word after retries");
        // Pool too small for the band -> null, never a broken round.
        ck.that(ScrambleRound.generate("kid", true, 1_000, List.of("APPLE", "BANANA"), new Random(1), 0L) == null,
                "scramble: an undersized pool refuses cleanly");
    }

    static void riddleMatching(Check ck) {
        RiddleText.Riddle clock = new RiddleText.Riddle("What has a face and two hands?",
                List.of("clock", "a clock"), "It tells time.", true);
        ck.that(RiddleText.matches("clock", clock), "riddle: exact answer matches");
        ck.that(RiddleText.matches("Clock", clock), "riddle: case-insensitive");
        ck.that(RiddleText.matches("a clock", clock), "riddle: leading article in the guess is ignored");
        ck.that(RiddleText.matches("THE CLOCK!", clock), "riddle: article + punctuation ignored");
        ck.that(RiddleText.matches("clocks", clock), "riddle: plural of the answer tolerated");
        ck.that(!RiddleText.matches("watch", clock), "riddle: a different word does not match");
        ck.that(!RiddleText.matches("", clock), "riddle: empty guess never matches");
        ck.that(!RiddleText.matches("clo", clock), "riddle: a prefix does not match");
        RiddleText.Riddle name = new RiddleText.Riddle("What belongs to you...?",
                List.of("name", "your name", "my name"), "People say it.", false);
        ck.that(RiddleText.matches("your name", name), "riddle: listed variants match");
        ck.that(RiddleText.matches("my name", name), "riddle: second listed variant matches");
        ck.that(!RiddleText.matches("names", clock), "riddle: plural tolerance doesn't cross riddles");
    }

    static void riddlePayoutMath(Check ck) {
        ck.eq(10_000, RiddleText.payoutCents(0), "riddle pay: no hints = $100");
        ck.eq(7_500, RiddleText.payoutCents(1), "riddle pay: 1 hint = $75");
        ck.eq(5_000, RiddleText.payoutCents(2), "riddle pay: 2 hints = $50");
        ck.eq(2_500, RiddleText.payoutCents(3), "riddle pay: 3 hints = $25 (floor)");
        ck.eq(2_500, RiddleText.payoutCents(9), "riddle pay: floor holds past 3 hints");
    }

    static void kidHalfOnTextGames(Check ck) {
        // Kid tiers: Quick Math Kid and Scramble Kid — adults earn HALF (Q6 amendment).
        ck.eq(1_250, Payouts.adjust(2_500, 0, true, false), "qm kid: adult 10/10 earns half of $25 = $12.50");
        ck.eq(2_500, Payouts.adjust(2_500, 0, true, true), "qm kid: kid-flagged 10/10 earns full $25");
        ck.eq(2_500, Payouts.adjust(5_000, 0, true, false), "scramble kid: adult full round earns half of $50 = $25");
        ck.eq(5_000, Payouts.adjust(5_000, 0, true, true), "scramble kid: kid-flagged full round earns $50");
        ck.eq(5_000, Payouts.adjust(5_000, 0, false, false), "qm standard is not a kid tier: full $50");
        ck.eq(10_000, Payouts.adjust(10_000, 0, false, false), "riddle is not a kid tier: full $100");
    }

    static void serialization(Check ck) {
        QuickMathRound qm = QuickMathRound.generate("challenge", false, false, new Random(9), 5_000L);
        qm.answer(qm.currentAnswer());
        qm.answer(qm.currentAnswer() + 1);
        QuickMathRound qmBack = QuickMathRound.fromMap(qm.toMap());
        ck.eq(2, qmBack.index(), "qm serde: index survives a mid-round restart");
        ck.eq(1, qmBack.correct(), "qm serde: correct count survives");
        ck.that(qmBack.prompts().equals(qm.prompts()) && qmBack.answers().equals(qm.answers()),
                "qm serde: questions + answers survive exactly");
        ck.eq(qm.currentAnswer(), qmBack.currentAnswer(), "qm serde: the current question is the same after reload");

        ScrambleRound sc = ScrambleRound.generate("standard", false, 2_000, pool(), new Random(13), 5_000L);
        sc.answer(sc.currentWord());
        sc.useHint();
        ScrambleRound scBack = ScrambleRound.fromMap(sc.toMap());
        ck.eq(1, scBack.index(), "scramble serde: index survives");
        ck.eq(2_000, scBack.earnedCents(), "scramble serde: banked earnings survive");
        ck.that(scBack.hintUsed(), "scramble serde: the armed hint survives");
        ck.that(scBack.words().equals(sc.words()) && scBack.scrambled().equals(sc.scrambled()),
                "scramble serde: words + shuffles survive exactly");
    }
}
