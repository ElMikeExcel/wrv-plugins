package com.winkreyes.excelguide.core;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The learn-cache privacy gate (0.2.1). A cached AI answer is served
 * to OTHER players later, so an answer may only enter the cache when
 * it cannot carry the asker's private facts. Two independent checks,
 * both required (belt and suspenders):
 *
 * (a) FIRST-PERSON QUESTIONS NEVER CACHE. The stat layer treats a
 * question as personal through first-person phrase patterns (the
 * [my ...] groups on the stat intents, the my/mine tokens in the
 * pronoun set); this is the same test at token level: a normalized
 * question containing i / me / my / mine / im / ive / myself is
 * personal, and its answers - however general they sound - stay
 * between the asker and the AI.
 *
 * (b) FACT-QUOTING ANSWERS NEVER CACHE. If the answer text contains
 * any numeric value that appeared in the ASKER's injected fact
 * sheet, the answer may be quoting the asker's live numbers - by
 * design OR by coincidence (a general question whose correct answer
 * happens to equal the asker's remaining blocks). Numbers are
 * compared canonicalized (commas stripped, trailing zeros folded),
 * so "1,234.56", "1234.56" and "1234.560" are the same value.
 * PUBLIC facts (seal progress, board leaders) are deliberately NOT
 * part of this check: those numbers are on the public boards and
 * safe to repeat to anyone.
 *
 * Everything here is pure string work - no state, no I/O - so the
 * whole gate is unit-testable, adversarial cases included.
 */
public final class LearnPrivacy {

    private LearnPrivacy() {}

    /** First-person tokens: the personal-question marker set. */
    static final Set<String> PERSONAL_TOKENS = Set.of(
            "i", "me", "my", "mine", "im", "ive", "myself");

    /** Numeric literals: 2393, 1,234.56, 33 (of "33%"), 0.5. */
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");

    /** True when the normalized question is first-person/personal (check a). */
    public static boolean isPersonalQuestion(Set<String> normalizedTokens) {
        if (normalizedTokens == null) {
            return false;
        }
        for (String t : normalizedTokens) {
            if (PERSONAL_TOKENS.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /** All numeric values in one text, canonicalized for comparison. */
    public static Set<String> numbersIn(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) {
            return out;
        }
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            out.add(canonical(m.group()));
        }
        return out;
    }

    /** The numeric values across a fact sheet (one "Label: value" line each). */
    public static Set<String> numbersInFacts(List<String> factLines) {
        Set<String> out = new LinkedHashSet<>();
        if (factLines == null) {
            return out;
        }
        for (String line : factLines) {
            out.addAll(numbersIn(line));
        }
        return out;
    }

    /** True when the answer quotes any of the given fact values (check b). */
    public static boolean answerQuotesAny(String answer, Set<String> factNumbers) {
        if (answer == null || factNumbers == null || factNumbers.isEmpty()) {
            return false;
        }
        for (String n : numbersIn(answer)) {
            if (factNumbers.contains(n)) {
                return true;
            }
        }
        return false;
    }

    /** Canonical numeric form: commas out, trailing zeros folded ("1234.560" -> "1234.56", "100.0" -> "100"). */
    static String canonical(String rawNumber) {
        try {
            return new BigDecimal(rawNumber.replace(",", ""))
                    .stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return rawNumber.replace(",", "");
        }
    }
}
