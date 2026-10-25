package com.winkreyes.excelguide.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The peer-learning guardrails (0.2.3, Mike's Variant A pick,
 * Oct 18, 2026: live-learn WITH guardrails). Every chat line that
 * wants to become a Guide answer passes through here first. The
 * bias is conservative on purpose: a rejected candidate costs the
 * valley one teachable moment; a bad learned answer is spoken by
 * the Guide to children with a player's name on it.
 *
 * The checks (see {@link #check}):
 * - SHAPE: an answer is at least a short sentence of substance, is
 *   not itself a question, and is not a rant (many times the cap).
 * - LANGUAGE: a profanity screen over normalized tokens.
 * - PRIVACY, reusing LearnPrivacy's approach: peer answers teach
 *   how the valley works, never someone's numbers. An answer with
 *   a number AND a first-person marker is a personal stat; a
 *   number alongside a stat word (blocks, balance, levels...) is
 *   stat content whoever it describes; a number alongside another
 *   online player's name is that player's data. A first-person
 *   marker with a stat word is personal even without a number.
 *   Plain game facts with numbers (a recipe's "5 string", "takes
 *   2 hours") carry no stat word and no first person, and pass -
 *   the screen targets player stats, not arithmetic.
 *
 * Also here: the debate-signal heuristics the tracker uses once a
 * candidate exists - disagreement openers, agreement, and asker
 * confirmations. Heuristics are fallible; the design absorbs that:
 * mistaking agreement for a competing answer only sends the pair
 * to adjudication, never straight into the Guide's mouth.
 *
 * Pure string work, no state, no I/O - fully unit-testable.
 */
public final class PeerGuard {

    private PeerGuard() {}

    /** Why a candidate was refused (the slug is what the peer log records). */
    public enum Reject {
        TOO_SHORT("too-short"),
        QUESTION("question"),
        TOO_LONG("too-long"),
        PROFANITY("profanity"),
        PERSONAL("personal"),
        STAT_CONTENT("stat-content"),
        OTHER_PLAYER("other-player");

        private final String slug;

        Reject(String slug) {
            this.slug = slug;
        }

        public String slug() {
            return slug;
        }

        /** Rejections worth a peer-log line (the rest is ordinary chatter misfiring). */
        public boolean substantive() {
            return this != TOO_SHORT && this != QUESTION;
        }
    }

    /**
     * The profanity screen: whole normalized tokens. Chat
     * normalization lowercases and strips punctuation, so "sh*t"
     * arrives as "sht" - the common masked spellings are listed
     * alongside the plain ones. Family-server list: the words a
     * captured answer must never carry into the Guide's mouth.
     */
    private static final Set<String> PROFANITY = Set.of(
            "damn", "dammit", "hell", "crap", "crappy", "piss", "pissed",
            "shit", "shitty", "sht", "bullshit", "fuck", "fucking", "fucked",
            "fucker", "fck", "bitch", "bitches", "bastard", "ass", "asshole",
            "dick", "cock", "pussy", "whore", "slut", "nigger", "nigga",
            "faggot", "fag", "retard", "retarded", "kys", "stfu", "wtf",
            "porn", "sex", "sexy", "nude", "nudes", "boobs", "penis", "vagina");

    /** True when one normalized token is on the profanity list (0.3.0: shared with PeerFilter). */
    public static boolean isProfane(String token) {
        return token != null && PROFANITY.contains(token);
    }

    /** Stat words that turn a number into player-stat content. */
    private static final Set<String> STAT_NUMBER_WORDS = Set.of(
            "balance", "money", "cash", "coin", "coins", "block", "blocks",
            "claim", "claims", "level", "levels", "playtime", "sp", "jobs");

    /** Openers that mark a line as pushing back on the current candidate. */
    private static final Set<String> DISAGREE_OPENERS = Set.of(
            "no", "nope", "nah", "wrong", "actually", "incorrect", "false");

    private static final List<String> DISAGREE_PHRASES = List.of(
            "thats wrong", "that is wrong", "not true", "isnt right",
            "not right", "youre wrong", "your wrong", "dont think so");

    /** First tokens that mark a line as backing the current candidate/side. */
    private static final Set<String> AGREE_OPENERS = Set.of(
            "yes", "yeah", "yep", "yup", "right", "exactly", "correct", "true");

    private static final List<String> AGREE_PHRASES = List.of(
            "thats right", "that is right", "sounds right", "is right");

    /** Asker words that mean "the answer worked" (checked with {@link #isConfirmation}). */
    private static final Set<String> THANKS_TOKENS = Set.of(
            "thanks", "thank", "thx", "ty", "thankyou");

    private static final List<String> WORKED_PHRASES = List.of(
            "that worked", "it worked", "it works", "got it", "works now");

    /**
     * Run every guardrail over one candidate answer.
     *
     * @param raw         the chat line as typed
     * @param cleaned     the line after AiPrompts.sanitize (null = nothing usable)
     * @param maxChars    the configured answer cap
     * @param onlineNames display names online (other-player data screen)
     * @param speakerName the candidate author's own display name (exempt from the name screen)
     * @return the refusal, or null when the candidate passes
     */
    public static Reject check(String raw, String cleaned, int maxChars,
                               Collection<String> onlineNames, String speakerName) {
        if (cleaned == null || cleaned.isBlank()) {
            return Reject.TOO_SHORT;
        }
        String norm = TextNorm.normalize(cleaned);
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (cleaned.length() < 15 || tokens.size() < 3) {
            return Reject.TOO_SHORT;
        }
        if (TextNorm.questionShaped(raw, norm)) {
            return Reject.QUESTION;
        }
        if (norm.length() > Math.max(maxChars, 1) * 3) {
            return Reject.TOO_LONG;
        }
        for (String tok : tokens) {
            if (PROFANITY.contains(tok)) {
                return Reject.PROFANITY;
            }
        }
        boolean firstPerson = LearnPrivacy.isPersonalQuestion(tokens);
        boolean hasNumber = !LearnPrivacy.numbersIn(cleaned).isEmpty();
        boolean statWord = false;
        for (String tok : tokens) {
            if (STAT_NUMBER_WORDS.contains(tok) || STAT_NUMBER_WORDS.contains(TextNorm.stem(tok))) {
                statWord = true;
                break;
            }
        }
        if (firstPerson && (hasNumber || statWord)) {
            return Reject.PERSONAL;
        }
        if (hasNumber && statWord) {
            return Reject.STAT_CONTENT;
        }
        if (hasNumber && onlineNames != null) {
            for (String name : onlineNames) {
                if (name == null || name.equalsIgnoreCase(speakerName)) {
                    continue;
                }
                String n = TextNorm.normalize(name);
                if (!n.isBlank() && tokens.contains(n)) {
                    return Reject.OTHER_PLAYER;
                }
            }
        }
        return null;
    }

    /** True when the line opens by pushing back on the standing candidate. */
    public static boolean isDisagreement(String norm, Set<String> tokens) {
        if (norm == null || norm.isBlank()) {
            return false;
        }
        String first = norm.split(" ", 2)[0];
        if (DISAGREE_OPENERS.contains(first)) {
            return true;
        }
        String padded = " " + norm + " ";
        for (String phrase : DISAGREE_PHRASES) {
            if (padded.contains(" " + phrase + " ") || norm.startsWith(phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    /** True when the line reads as backing an existing answer. */
    public static boolean isAgreement(String norm, Set<String> tokens) {
        if (norm == null || norm.isBlank()) {
            return false;
        }
        String first = norm.split(" ", 2)[0];
        if (AGREE_OPENERS.contains(first)) {
            return true;
        }
        String padded = " " + norm + " ";
        for (String phrase : AGREE_PHRASES) {
            if (padded.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    /** True when the ASKER's line confirms an answer worked (thanks / it worked / got it). */
    public static boolean isConfirmation(String norm, Set<String> tokens) {
        if (norm == null || norm.isBlank()) {
            return false;
        }
        for (String tok : tokens) {
            if (THANKS_TOKENS.contains(tok)) {
                return true;
            }
        }
        String padded = " " + norm + " ";
        for (String phrase : WORKED_PHRASES) {
            if (padded.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Which side an agreement/confirmation line backs: the side
     * whose AUTHOR the line names, else the most recently posted
     * side. -1 when there are no sides. (Sides are capped at two by
     * the tracker, so an index is all the bookkeeping needs.)
     */
    public static int backedSide(String norm, List<PeerAnswers.Side> sides) {
        if (sides == null || sides.isEmpty()) {
            return -1;
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        for (int i = 0; i < sides.size(); i++) {
            String author = TextNorm.normalize(sides.get(i).author());
            if (!author.isBlank() && (tokens.contains(author) || tokens.contains(author + "s"))) {
                return i;
            }
        }
        return sides.size() - 1;
    }
}
