package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Text-trio property tests: 500 seeds per Quick Math tier and per
 * Scramble tier, generated from the SHIPPED wordlists.
 *
 * Quick Math strength: every generated prompt is RE-PARSED from its
 * display string and evaluated by an independent mini evaluator in
 * this test (tokenizer + precedence parser, no shared code with the
 * generator), and the stored answer must equal the evaluated result.
 * Tier invariants are checked from the parsed tokens: kid stays within
 * 20 with no negatives; standard results stay within 100 and division
 * is exact; challenge is always a two-step problem.
 */
public final class TextPropertyTest {

    public static void main(String[] args) throws Exception {
        Check ck = new Check("TextPropertyTest");
        Path dir = Path.of(args.length > 0 ? args[0] : "wordlists");
        List<String> kidPool = scrapeBand(dir, "kid");
        List<String> stdPool = scrapeBand(dir, "standard");
        ck.that(kidPool.size() >= 30, "shipped kid pool is big enough (" + kidPool.size() + ")");
        ck.that(stdPool.size() >= 40, "shipped standard pool is big enough (" + stdPool.size() + ")");

        final int seeds = 500;
        for (String tier : new String[]{"kid", "standard", "challenge"}) {
            boolean kid = tier.equals("kid");
            for (int s = 0; s < seeds; s++) {
                QuickMathRound r = QuickMathRound.generate(tier, kid, kid, new Random(31L * s + tier.hashCode()), s);
                String tag = "qm " + tier + " seed " + s;
                ck.eq(10, r.prompts().size(), tag + ": 10 questions");
                for (int i = 0; i < 10; i++) {
                    String prompt = r.prompts().get(i);
                    int stored = r.answers().get(i);
                    int evaluated = Eval.eval(prompt);
                    ck.eq(stored, evaluated, tag + " q" + (i + 1) + ": prompt '" + prompt + "' evaluates to the stored answer");
                    int[] nums = Eval.numbers(prompt);
                    switch (tier) {
                        case "kid" -> {
                            ck.that(!prompt.contains("x") && !prompt.contains("\u00f7"), tag + ": kid uses only + and -");
                            ck.that(stored >= 0 && stored <= 20, tag + ": kid result within 20, never negative");
                            for (int n : nums) ck.that(n >= 0 && n <= 20, tag + ": kid operands within 20");
                        }
                        case "standard" -> {
                            ck.that(stored >= 0 && stored <= 100, tag + ": standard result within 100");
                            ck.that(Eval.opCount(prompt) == 1, tag + ": standard is one operation");
                            if (prompt.contains("\u00f7")) {
                                ck.that(nums[0] % nums[1] == 0 && nums[1] != 0, tag + ": standard division is exact");
                            }
                        }
                        default -> {
                            ck.that(Eval.opCount(prompt) == 2, tag + ": challenge is a two-step problem");
                            ck.that(stored >= 1 && stored <= 999, tag + ": challenge result within 999");
                        }
                    }
                }
                // Perfect play pays the full tier amount (via the 6.3 formula on 10/10).
                QuickMathRound played = QuickMathRound.fromMap(r.toMap());
                while (!played.finished()) played.answer(played.currentAnswer());
                ck.eq(10, played.correct(), tag + ": perfect play scores 10");
            }
            System.out.println("    quickmath " + tier + ": 500 seeds OK");
        }

        for (String tier : new String[]{"kid", "standard"}) {
            boolean kid = tier.equals("kid");
            List<String> pool = kid ? kidPool : stdPool;
            long perWord = kid ? 1_000 : 2_000;
            for (int s = 0; s < seeds; s++) {
                ScrambleRound r = ScrambleRound.generate(tier, kid, perWord, pool, new Random(17L * s + tier.hashCode()), s);
                String tag = "scramble " + tier + " seed " + s;
                ck.that(r != null, tag + ": round generates");
                if (r == null) continue;
                Set<String> seen = new HashSet<>(r.words());
                ck.eq(5, seen.size(), tag + ": 5 distinct words");
                ScrambleRound played = ScrambleRound.fromMap(r.toMap());
                for (int i = 0; i < 5; i++) {
                    String word = r.words().get(i);
                    int len = word.length();
                    ck.that(kid ? (len >= 3 && len <= 4) : (len >= 5 && len <= 8),
                            tag + ": word '" + word + "' in the tier length band");
                    String scr = r.scrambled().get(i);
                    ck.that(!scr.equals(word), tag + ": shuffle differs from the word");
                    char[] a = scr.toCharArray(), b = word.toCharArray();
                    java.util.Arrays.sort(a); java.util.Arrays.sort(b);
                    ck.that(java.util.Arrays.equals(a, b), tag + ": shuffle is a permutation");
                    if (kid) {
                        ck.eq(3, played.currentChoices().size(), tag + ": kid shows 3 choices");
                        ck.that(played.currentChoices().contains(word), tag + ": kid choices contain the word");
                    }
                    played.answer(played.currentWord());
                }
                // Perfect play banks the full round amount.
                ck.that(played.finished(), tag + ": perfect play finishes the round");
                ck.eq(5 * perWord, played.earnedCents(), tag + ": perfect play banks the full round");
            }
            System.out.println("    scramble " + tier + ": 500 seeds OK");
        }
        ck.finish();
    }

    /** Scrape a tier band's words out of the shipped wordlist YAMLs (text scrape, no YAML lib). */
    static List<String> scrapeBand(Path dir, String band) throws Exception {
        List<String> out = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path f : stream.filter(p -> p.toString().endsWith(".yml")).sorted().toList()) {
                if (f.getFileName().toString().equals("riddles.yml")) continue;
                boolean inBand = false;
                for (String line : Files.readAllLines(f)) {
                    String t = line.strip();
                    if (t.equals(band + ":")) { inBand = true; continue; }
                    if (inBand) {
                        if (t.startsWith("- ")) {
                            String w = t.substring(2).strip().toUpperCase();
                            if (!out.contains(w)) out.add(w);
                        } else if (!t.isEmpty() && !t.startsWith("#")) inBand = false;
                    }
                }
            }
        }
        return out;
    }

    /** Independent prompt evaluator: tokenizes "7 + 5 x 2 = ?" style strings and applies precedence. */
    static final class Eval {
        static int[] numbers(String prompt) {
            List<Integer> out = new ArrayList<>();
            for (String tok : prompt.replace("(", " ").replace(")", " ").split(" ")) {
                try { out.add(Integer.parseInt(tok)); } catch (NumberFormatException ignored) { }
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        }

        static int opCount(String prompt) {
            int n = 0;
            for (char c : prompt.toCharArray()) if (c == '+' || c == '-' || c == 'x' || c == '\u00f7') n++;
            return n;
        }

        static int eval(String prompt) {
            String s = prompt.replace("= ?", "").trim();
            // Tokenize into numbers and ops/parens.
            List<Object> toks = new ArrayList<>();
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) {
                    int j = i;
                    while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                    toks.add(Integer.parseInt(s.substring(i, j)));
                    i = j;
                } else if (c == '(' || c == ')' || c == '+' || c == '-' || c == 'x' || c == '\u00f7') {
                    toks.add(c);
                    i++;
                } else i++;
            }
            int[] pos = {0};
            int v = expr(toks, pos);
            return v;
        }

        static int expr(List<Object> toks, int[] pos) {
            int v = term(toks, pos);
            while (pos[0] < toks.size() && (toks.get(pos[0]).equals('+') || toks.get(pos[0]).equals('-'))) {
                char op = (Character) toks.get(pos[0]++);
                int rhs = term(toks, pos);
                v = op == '+' ? v + rhs : v - rhs;
            }
            return v;
        }

        static int term(List<Object> toks, int[] pos) {
            int v = factor(toks, pos);
            while (pos[0] < toks.size() && (toks.get(pos[0]).equals('x') || toks.get(pos[0]).equals('\u00f7'))) {
                char op = (Character) toks.get(pos[0]++);
                int rhs = factor(toks, pos);
                v = op == 'x' ? v * rhs : v / rhs;
            }
            return v;
        }

        static int factor(List<Object> toks, int[] pos) {
            Object t = toks.get(pos[0]++);
            if (t.equals('(')) {
                int v = expr(toks, pos);
                pos[0]++; // ')'
                return v;
            }
            return (Integer) t;
        }
    }
}
