package com.winkreyes.excelbraingames.games.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Quick Math round (SPEC §6) — pure logic, no Bukkit. A round is 10
 * generated questions; one attempt each (a wrong answer shows the
 * correct one and moves on). Payout = tier amount x (correct / 10),
 * rounded DOWN to whole dollars (SPEC §6.3), settled by GameService
 * through the shared ledger.
 *
 * Tiers: Kid — addition/subtraction within 20, no negatives, optional
 * 3-choice tap answers (config, default ON for kid). Standard — + - x
 * and simple division, results within 100. Challenge — two-step
 * problems with explicit parentheses, answers within 999.
 */
public final class QuickMathRound {

    public static final int QUESTIONS = 10;

    private final String tierId;
    private final boolean kid;
    private final boolean choices; // 3-choice tap answers for this round
    private final List<String> prompts;
    private final List<Integer> answers;
    private final List<List<Integer>> choiceSets; // per question when choices on, else empty lists
    private int index;
    private int correct;
    private boolean paid;
    private final long startedMillis;

    private QuickMathRound(String tierId, boolean kid, boolean choices, List<String> prompts,
                           List<Integer> answers, List<List<Integer>> choiceSets,
                           int index, int correct, boolean paid, long startedMillis) {
        this.tierId = tierId;
        this.kid = kid;
        this.choices = choices;
        this.prompts = prompts;
        this.answers = answers;
        this.choiceSets = choiceSets;
        this.index = index;
        this.correct = correct;
        this.paid = paid;
        this.startedMillis = startedMillis;
    }

    public static QuickMathRound generate(String tierId, boolean kid, boolean choices,
                                          Random rng, long startedMillis) {
        List<String> prompts = new ArrayList<>();
        List<Integer> answers = new ArrayList<>();
        List<List<Integer>> choiceSets = new ArrayList<>();
        for (int i = 0; i < QUESTIONS; i++) {
            int[] q = kid ? kidQuestion(rng) : tierId.equals("challenge") ? challengeQuestion(rng) : standardQuestion(rng);
            // q = {a, op, b, answer, c, op2} — c/op2 only for challenge two-steppers
            prompts.add(format(q));
            answers.add(q[3]);
            choiceSets.add(choices ? distractors(q[3], rng) : List.of());
        }
        return new QuickMathRound(tierId, kid, choices, prompts, answers, choiceSets, 0, 0, false, startedMillis);
    }

    private static int[] kidQuestion(Random rng) {
        if (rng.nextBoolean()) { // a + b <= 20
            int a = 1 + rng.nextInt(19);
            int b = 1 + rng.nextInt(20 - a);
            return new int[]{a, 0, b, a + b, 0, -1};
        }
        int a = 2 + rng.nextInt(19); // a - b >= 0
        int b = 1 + rng.nextInt(a);
        return new int[]{a, 1, b, a - b, 0, -1};
    }

    private static int[] standardQuestion(Random rng) {
        return switch (rng.nextInt(4)) {
            case 0 -> { int a = 5 + rng.nextInt(90), b = 5 + rng.nextInt(95 - a); yield new int[]{a, 0, b, a + b, 0, -1}; }
            case 1 -> { int a = 10 + rng.nextInt(90), b = 1 + rng.nextInt(a); yield new int[]{a, 1, b, a - b, 0, -1}; }
            case 2 -> { int a = 2 + rng.nextInt(11), b = 2 + rng.nextInt(Math.min(9, 100 / a - 1)); yield new int[]{a, 2, b, a * b, 0, -1}; }
            default -> { int b = 2 + rng.nextInt(11), q = 2 + rng.nextInt(11); yield new int[]{b * q, 3, b, q, 0, -1}; }
        };
    }

    private static int[] challengeQuestion(Random rng) {
        return switch (rng.nextInt(3)) {
            case 0 -> { // (a + b) x c
                int a = 3 + rng.nextInt(40), b = 3 + rng.nextInt(40), c = 2 + rng.nextInt(8);
                yield new int[]{a, 0, b, (a + b) * c, c, 2};
            }
            case 1 -> { // a x b + c
                int a = 3 + rng.nextInt(11), b = 3 + rng.nextInt(11), c = 5 + rng.nextInt(90);
                yield new int[]{a, 2, b, a * b + c, c, 0};
            }
            default -> { // a x b - c (stays positive)
                int a = 4 + rng.nextInt(11), b = 4 + rng.nextInt(11), c = 1 + rng.nextInt(a * b - 1);
                yield new int[]{a, 2, b, a * b - c, c, 1};
            }
        };
    }

    static String format(int[] q) {
        String[] sym = {"+", "-", "x", "\u00f7"};
        if (q[5] < 0) return q[0] + " " + sym[q[1]] + " " + q[2] + " = ?";
        if (q[1] == 0 && q[5] == 2) return "(" + q[0] + " + " + q[2] + ") x " + q[4] + " = ?";
        return q[0] + " " + sym[q[1]] + " " + q[2] + " " + sym[q[5]] + " " + q[4] + " = ?";
    }

    /** Two distinct plausible wrong answers around the right one. */
    static List<Integer> distractors(int answer, Random rng) {
        List<Integer> out = new ArrayList<>();
        int[] deltas = {1, -1, 2, -2, 10, -10, 3, -3};
        int tries = 0;
        while (out.size() < 2 && tries < 40) {
            int d = answer + deltas[rng.nextInt(deltas.length)] * (1 + rng.nextInt(2));
            tries++;
            if (d == answer || d < 0 || out.contains(d)) continue;
            out.add(d);
        }
        while (out.size() < 2) { int d = answer + out.size() + 5; if (!out.contains(d)) out.add(d); }
        List<Integer> all = new ArrayList<>(out);
        all.add(answer);
        Collections.shuffle(all, rng);
        return all;
    }

    // ------------------------------------------------------------ play

    public boolean finished() { return index >= QUESTIONS; }
    public String currentPrompt() { return finished() ? "" : prompts.get(index); }
    public int currentAnswer() { return finished() ? Integer.MIN_VALUE : answers.get(index); }
    public List<Integer> currentChoices() { return finished() ? List.of() : choiceSets.get(index); }

    /** One attempt. Returns true when correct. Advances either way (SPEC §6.2). */
    public boolean answer(int value) {
        if (finished()) return false;
        boolean ok = value == answers.get(index);
        if (ok) correct++;
        index++;
        return ok;
    }

    /** SPEC §6.3: tier x correct/10, rounded down to whole dollars. */
    public static long payoutCents(long tierPayoutCents, int correctCount) {
        long dollars = (tierPayoutCents / 100) * correctCount / QUESTIONS;
        return dollars * 100;
    }

    public long payoutCents(long tierPayoutCents) { return payoutCents(tierPayoutCents, correct); }

    // ------------------------------------------------------------ accessors

    public String tierId() { return tierId; }
    public boolean kid() { return kid; }
    public boolean choices() { return choices; }
    public int index() { return index; }
    public int correct() { return correct; }
    public boolean paid() { return paid; }
    public void markPaid() { paid = true; }
    public long startedMillis() { return startedMillis; }
    public List<String> prompts() { return Collections.unmodifiableList(prompts); }
    public List<Integer> answers() { return Collections.unmodifiableList(answers); }

    // ------------------------------------------------------------ persistence

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tier", tierId);
        m.put("kid", kid);
        m.put("choices", choices);
        m.put("prompts", new ArrayList<>(prompts));
        m.put("answers", new ArrayList<>(answers));
        List<List<Integer>> cs = new ArrayList<>();
        for (List<Integer> c : choiceSets) cs.add(new ArrayList<>(c));
        m.put("choiceSets", cs);
        m.put("index", index);
        m.put("correct", correct);
        m.put("paid", paid);
        m.put("started", startedMillis);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static QuickMathRound fromMap(Map<String, Object> m) {
        List<String> prompts = (List<String>) m.getOrDefault("prompts", List.of());
        List<Integer> answers = new ArrayList<>();
        for (Object o : (List<Object>) m.getOrDefault("answers", List.of())) answers.add(((Number) o).intValue());
        List<List<Integer>> cs = new ArrayList<>();
        for (Object o : (List<Object>) m.getOrDefault("choiceSets", List.of())) {
            List<Integer> one = new ArrayList<>();
            for (Object v : (List<Object>) o) one.add(((Number) v).intValue());
            cs.add(one);
        }
        while (cs.size() < prompts.size()) cs.add(List.of());
        return new QuickMathRound(
                (String) m.getOrDefault("tier", "?"),
                Boolean.TRUE.equals(m.get("kid")),
                Boolean.TRUE.equals(m.get("choices")),
                prompts, answers, cs,
                ((Number) m.getOrDefault("index", 0)).intValue(),
                ((Number) m.getOrDefault("correct", 0)).intValue(),
                Boolean.TRUE.equals(m.get("paid")),
                ((Number) m.getOrDefault("started", 0L)).longValue());
    }
}
