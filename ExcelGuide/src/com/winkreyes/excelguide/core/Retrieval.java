package com.winkreyes.excelguide.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Chunk retrieval, deliberately the same plain approach as the website
 * ask (SPEC 6.2, worker-v3 pickChunks): tokenize minus stopwords; a
 * query term found in a chunk title weighs 3; body occurrences weigh 1
 * each, capped per term. Occurrences are SUBSTRING matches, like the
 * site's own picker, so "party" also matches "parties". Chunks whose
 * id matches a hint prefix get a 1.5x boost (used by retrieval-shaped
 * intents to scope the search to their topic).
 *
 * Two measured deviations from the site's picker (tuning, Oct 18,
 * 2026; both forced by the fact that the site feeds its top 3 chunks
 * to a model while the Guide answers from the top chunk alone, so its
 * ranking has to be sharper):
 *  1. Terms appearing in more than 40% of chunks are dropped from
 *     scoring - on this corpus "server" (41 of 67 chunks) or "use"
 *     (32) cannot discriminate between chunks at all.
 *  2. Counting is stemmed and word-start anchored: a term's stem
 *     (trailing -ies -> -y, trailing -s dropped) matches at the start
 *     of a body word, so "party" finds "parties" and "claim" finds
 *     "claimed" - but mid-word accidents like "won" inside "one" or
 *     "two" no longer score.
 *  3. The site's own join special case is kept: questions mentioning
 *     joining/connecting or a console/phone get the "top" chunk (the
 *     join page) boosted 2x, mirroring worker-v3's pickChunks rule.
 * With these, the shipped gate (score >= 8 and a 1.5x margin) is met
 * exactly where a topic genuinely dominates the corpus; mid-pack
 * questions fall back to curated intent text or the non-answer + log,
 * which is the designed behavior.
 *
 * The answer text is the chunk's own words: the first sentences that
 * contain a query term (falling back to the chunk's first sentences),
 * at most 2 sentences, capped - SPEC guardrail 10 (no invented facts).
 */
public final class Retrieval {

    private Retrieval() {}

    /** Outcome of one search. best is null when nothing scored at all. */
    public record Result(KnowledgeBase.Chunk best, double bestScore,
                         KnowledgeBase.Chunk second, double secondScore,
                         String answerText) {}

    static final double HINT_BOOST = 1.5;
    private static final int BODY_CAP_PER_TERM = 3;
    private static final int TITLE_WEIGHT = 3;
    private static final int MAX_ANSWER_CHARS = 380;
    /** Terms in more than this share of chunks are non-discriminating. */
    private static final double DF_CUTOFF = 0.40;

    public static double score(Set<String> queryTerms, KnowledgeBase.Chunk chunk) {
        double score = 0;
        String titleNorm = TextNorm.normalize(chunk.title());
        String bodyNorm = TextNorm.normalize(chunk.text());
        for (String term : queryTerms) {
            String stem = stem(term);
            if (anchoredContains(titleNorm, stem)) {
                score += TITLE_WEIGHT;
            }
            score += occurrences(bodyNorm, stem);
        }
        return score;
    }

    /** Light stem: parties -> party, shops -> shop, classes -> class. */
    static String stem(String term) {
        return TextNorm.stem(term);
    }

    /** True when stem occurs at the start of a word in normalized text. */
    private static boolean anchoredContains(String normText, String stem) {
        return anchoredOccurrences(normText, stem, 1) > 0;
    }

    /** Word-start-anchored occurrences of stem, capped. */
    private static int anchoredOccurrences(String normText, String stem, int cap) {
        int count = 0;
        int idx = 0;
        while (count < cap && (idx = normText.indexOf(stem, idx)) >= 0) {
            if (idx == 0 || !Character.isLetterOrDigit(normText.charAt(idx - 1))) {
                count++;
            }
            idx += stem.length();
        }
        return count;
    }

    private static int occurrences(String bodyNorm, String stem) {
        return anchoredOccurrences(bodyNorm, stem, BODY_CAP_PER_TERM);
    }

    /**
     * Drop query terms that appear in too much of the corpus to
     * discriminate (see class doc). A term "appears" in a chunk when
     * its stem matches at the start of a word in the chunk's title or
     * body.
     */
    static Set<String> discriminatingTerms(KnowledgeBase base, Set<String> queryTerms) {
        Set<String> out = new java.util.LinkedHashSet<>();
        int limit = (int) Math.ceil(DF_CUTOFF * base.size());
        for (String term : queryTerms) {
            String stem = stem(term);
            int df = 0;
            for (KnowledgeBase.Chunk c : base.chunks()) {
                if (anchoredContains(TextNorm.normalize(c.title()), stem)
                        || anchoredContains(TextNorm.normalize(c.text()), stem)) {
                    df++;
                }
            }
            if (df <= limit) {
                out.add(term);
            }
        }
        return out;
    }

    /**
     * Search the base for the best chunk. hints (may be null/empty) are
     * chunk-id prefixes: a chunk whose id equals a hint or whose key
     * starts with it is boosted.
     */
    /** Words that mark a "how do I join" question (mirrors worker-v3). */
    private static final Set<String> JOIN_WORDS = Set.of(
            "join", "connect", "playstation", "xbox", "switch", "phone", "ipad", "address");

    public static Result search(KnowledgeBase base, String normalizedQuery, List<String> hints) {
        Set<String> terms = discriminatingTerms(base, TextNorm.queryTerms(normalizedQuery));
        boolean joinQuestion = false;
        for (String tok : normalizedQuery.split(" ")) {
            if (JOIN_WORDS.contains(tok)) {
                joinQuestion = true;
                break;
            }
        }
        KnowledgeBase.Chunk best = null;
        KnowledgeBase.Chunk second = null;
        double bestScore = 0;
        double secondScore = 0;
        for (KnowledgeBase.Chunk chunk : base.chunks()) {
            double s = score(terms, chunk);
            if (hints != null && !hints.isEmpty() && hinted(chunk, hints)) {
                s *= HINT_BOOST;
            }
            if (joinQuestion && chunk.id().equals("top")) {
                s *= 2.0;
            }
            if (s > bestScore) {
                second = best;
                secondScore = bestScore;
                best = chunk;
                bestScore = s;
            } else if (s > secondScore) {
                second = chunk;
                secondScore = s;
            }
        }
        String answer = best == null ? "" : answerText(best, terms);
        return new Result(best, bestScore, second, secondScore, answer);
    }

    private static boolean hinted(KnowledgeBase.Chunk chunk, List<String> hints) {
        String titleNorm = null;
        for (String h : hints) {
            if (chunk.id().equals(h) || chunk.key().startsWith(h)) {
                return true;
            }
            // A hint also matches a chunk whose TITLE carries the hint's
            // words ("builder-mode" -> the chunk titled "Builder Mode:
            // build big..."), because section splits scatter one topic
            // across several chunk ids on this site.
            if (titleNorm == null) {
                titleNorm = TextNorm.normalize(chunk.title());
            }
            if (titleNorm.contains(h.replace('-', ' '))) {
                return true;
            }
        }
        return false;
    }

    /** The margin rule (SPEC 6.2): clear bar + clear lead over runner-up. */
    public static boolean clearsBar(Result r, double minScore, double margin) {
        if (r.best() == null || r.bestScore() < minScore) {
            return false;
        }
        return r.secondScore() <= 0 || r.bestScore() >= margin * r.secondScore();
    }

    /**
     * The top-K chunks by score, best first (0.2.0): the grounding
     * excerpts for the AI fallback. Same scoring as {@link #search}
     * (DF filter, hint-less, the join special case applies), but no
     * bar and no margin - the model, not the ranker, decides whether
     * the excerpts hold the answer, and it is fenced to say so when
     * they do not. Only chunks that scored above zero are returned.
     */
    public static List<KnowledgeBase.Chunk> topK(KnowledgeBase base, String normalizedQuery, int k) {
        if (k <= 0) {
            return List.of();
        }
        Set<String> terms = discriminatingTerms(base, TextNorm.queryTerms(normalizedQuery));
        boolean joinQuestion = false;
        for (String tok : normalizedQuery.split(" ")) {
            if (JOIN_WORDS.contains(tok)) {
                joinQuestion = true;
                break;
            }
        }
        List<KnowledgeBase.Chunk> chunks = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for (KnowledgeBase.Chunk chunk : base.chunks()) {
            double s = score(terms, chunk);
            if (joinQuestion && chunk.id().equals("top")) {
                s *= 2.0;
            }
            if (s > 0) {
                chunks.add(chunk);
                scores.add(s);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        List<KnowledgeBase.Chunk> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, order.size()); i++) {
            out.add(chunks.get(order.get(i)));
        }
        return out;
    }

    /** Trim a chunk to its most relevant 1-2 sentences. */
    public static String answerText(KnowledgeBase.Chunk chunk, Set<String> queryTerms) {
        List<String> sentences = sentences(chunk.text());
        List<String> picked = new ArrayList<>();
        for (String s : sentences) {
            if (picked.size() >= 2) {
                break;
            }
            String normSentence = TextNorm.normalize(s);
            for (String term : queryTerms) {
                if (anchoredContains(normSentence, stem(term))) {
                    picked.add(s);
                    break;
                }
            }
        }
        if (picked.isEmpty()) {
            for (String s : sentences) {
                if (picked.size() >= 2) {
                    break;
                }
                picked.add(s);
            }
        }
        String joined = String.join(" ", picked).trim();
        if (joined.length() > MAX_ANSWER_CHARS) {
            joined = joined.substring(0, MAX_ANSWER_CHARS).trim();
            int lastSpace = joined.lastIndexOf(' ');
            if (lastSpace > MAX_ANSWER_CHARS - 60) {
                joined = joined.substring(0, lastSpace);
            }
        }
        return joined;
    }

    /** Split running text into sentences on ., !, ? boundaries. */
    static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String flat = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        for (String part : flat.split("(?<=[.!?])\\s+")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}
