package com.winkreyes.excelguide.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * The Valley Guide's brain: given one chat line (or /ask), decide
 * whether to answer, with what, and through which channel - or to
 * stay silent. Pure logic: all server data arrives through StatsView,
 * all time through the clock supplier, so the whole behaviour is
 * unit-testable on a plain JVM (SPEC 10.1).
 *
 * Implements SPEC section 3's guardrails: ambient answers only through
 * the marked tight intent groups; addressed-to-a-player wins; one
 * clarifying question, then the combined fallback; per-player /
 * per-question / global cooldowns; per-player off switch (the /ask
 * command always works); personal stats always private; ambient
 * answers never right after the asker took damage.
 *
 * One documented refinement of guardrail 5: the per-question cooldown
 * key is the normalized question for PUBLIC answers (the anti-pile-on
 * case the rule exists for) but is scoped to the asker for PRIVATE
 * answers, so Avery asking her own balance 30 s after Mike asked his
 * still gets her answer - she cannot see his, and nothing piles on.
 */
public final class GuideEngine {

    public enum Door { COMMAND, SUMMON, AMBIENT }

    public enum Channel { PUBLIC, PRIVATE }

    /** One message to deliver. text has no persona prefix (glue adds it). */
    public record Reply(Channel channel, String text) {}

    /** Who is speaking. */
    public record PlayerRef(UUID id, String name) {}

    /**
     * Where AI work runs (0.2.0). The glue backs this with the
     * server's async scheduler; tests run tasks inline or from a
     * queue they drain by hand. The engine never calls the provider
     * anywhere else, so a chat or command thread never blocks on it.
     */
    @FunctionalInterface
    public interface AiExecutor {
        void submit(Runnable task);
    }

    /**
     * Where an asynchronous answer goes when it is ready (0.2.0).
     * The glue's sink schedules delivery onto the main thread and
     * checks the asker is still online.
     */
    @FunctionalInterface
    /**
     * Delivery hook for delayed plain-chat answers (0.3.2): the reply
     * lands seconds after the chat line, so it is addressed by the
     * asker's id; the glue looks the player up and delivers on the
     * main thread (or drops it when they left). Null = the delayed
     * answer layer is off (exactly 0.3.1 behavior).
     */
    public interface DelayedSink {
        void deliver(UUID asker, List<Reply> replies);
    }

    public interface AsyncSink {
        void accept(List<Reply> replies);
    }

    /** The Magic lore line (SPEC 5 amendment, Mike's wording call). */
    static final String MAGIC_LORE =
            "Magic is not released into the world yet. It sleeps behind the guild's seals, "
            + "like everything the old days left us. Break the first seal, then ask me again.";

    static final String CLARIFY_QUESTION = "Which level - mcMMO, Jobs, time played, or Magic?";

    static final String NON_ANSWER =
            "I don't know that one yet. It's probably on the website's Ask page - "
            + "and I've written your question down so we can teach me.";

    static final String CANT_SEE = "I can't see that right now - try again in a little while.";

    static final String OTHER_PLAYER_REFUSAL =
            "That one's theirs to share - the leaderboard shows the top spots.";

    /** mcMMO skill names, normalized-token form. */
    public static final List<String> SKILLS = List.of(
            "acrobatics", "alchemy", "archery", "axes", "crossbows", "excavation",
            "fishing", "herbalism", "maces", "mining", "repair", "smelting",
            "swords", "taming", "unarmed", "woodcutting");

    /** Words that mark a question as being about someone's personal stats. */
    private static final Set<String> STAT_WORDS = Set.of(
            "level", "levels", "balance", "money", "cash", "played", "playtime",
            "hours", "job", "jobs", "magic", "rich",
            // 0.2.2: claim-block questions about a named player are
            // personal too - the refusal, not the asker's own numbers,
            // is the answer to "how many claim blocks does X have".
            "claim", "claims", "blocks", "land");

    private enum Choice { MCMMO, JOBS, PLAYTIME, MAGIC }

    private final GuideConfig cfg;
    private final IntentSet intents;
    private final KnowledgeBase knowledge;
    private final StatsView stats;
    private final LongSupplier clock;
    private final UnansweredLog unansweredLog; // may be null (logging off)
    private final RateLimiter limiter;
    private final AiProvider aiProvider;   // 0.2.0; null = no AI fallback
    private final FactsView aiFacts;       // 0.2.0; null = no fact sheet
    private final AiLog aiLog;             // 0.2.0; may be null
    private final AiExecutor aiExecutor;   // 0.2.0; null = AI cannot run
    private final AiGate aiGate;           // 0.2.0; the AI bucket
    private final LearnCache aiCache;      // 0.2.1; null = no learn-cache
    private final LearnedAnswers learned;  // 0.2.1; promoted answers (never null)
    private final PeerCandidates peerStore; // 0.3.0 review queue; null = peer learning absent
    private final PeerLog peerLog;          // 0.2.3; may be null
    private final Map<UUID, Long> clarifyPending = new HashMap<>();
    private final Map<UUID, Boolean> togglesOff = new HashMap<>();
    private final Map<UUID, Long> lastNoticeAt = new HashMap<>(); // 0.2.2 notice throttle
    private final Map<String, PeerWindow> peerWindows = new java.util.LinkedHashMap<>(); // by topic key
    private long answersServed;
    private long unansweredCount;
    private long aiAnswered;
    // 0.3.1 observability: AI failures and ai.log write failures reach the
    // console (throttled) instead of vanishing. Never carries the key or prompt.
    private volatile java.util.function.Consumer<String> warnSink;
    private long aiFailed;
    private long lastAiFailWarnAt = Long.MIN_VALUE / 2;
    private long lastAiLogWarnAt = Long.MIN_VALUE / 2;
    private static final long WARN_EVERY_MILLIS = 10L * 60L * 1000L;
    // 0.3.1 ambient hint: last time each player got the /ask pointer.
    private final Map<UUID, Long> ambientHintAt = new HashMap<>();
    /** The private pointer sent to a plain-chat asker (ai.ambient-hint). No em dashes. */
    public static final String AMBIENT_HINT =
            "I might know that one. Type /ask and your question, and I will look it up for you.";
    private long cacheServed;
    private long peerServed;

    /** Concurrent capture windows cap: a busy night cannot fan out unbounded state. */
    private static final int MAX_PEER_WINDOWS = 8;

    public GuideEngine(GuideConfig cfg, IntentSet intents, KnowledgeBase knowledge,
                       StatsView stats, LongSupplier clock, UnansweredLog unansweredLog) {
        this(cfg, intents, knowledge, stats, clock, unansweredLog, null, null, null, null);
    }

    /** Full constructor (0.2.0): the AI pieces are all nullable - any gap disables the fallback. */
    public GuideEngine(GuideConfig cfg, IntentSet intents, KnowledgeBase knowledge,
                       StatsView stats, LongSupplier clock, UnansweredLog unansweredLog,
                       AiProvider aiProvider, FactsView aiFacts, AiLog aiLog,
                       AiExecutor aiExecutor) {
        this(cfg, intents, knowledge, stats, clock, unansweredLog, aiProvider, aiFacts, aiLog,
                aiExecutor, null, null);
    }

    /**
     * Full constructor (0.2.1): adds the learn-cache (nullable - null
     * disables caching, AI behavior unchanged) and the promoted
     * answers (null means none loaded).
     */
    public GuideEngine(GuideConfig cfg, IntentSet intents, KnowledgeBase knowledge,
                       StatsView stats, LongSupplier clock, UnansweredLog unansweredLog,
                       AiProvider aiProvider, FactsView aiFacts, AiLog aiLog,
                       AiExecutor aiExecutor, LearnCache aiCache, LearnedAnswers learned) {
        this(cfg, intents, knowledge, stats, clock, unansweredLog, aiProvider, aiFacts, aiLog,
                aiExecutor, aiCache, learned, null, null);
    }

    /**
     * Full constructor (0.3.0): adds peer learning - the review queue
     * (nullable: null or the config kill switch disables the whole
     * feature, restoring 0.2.2 behavior) and the peer event log
     * (nullable). 0.2.3's AI adjudicator is gone: nothing a player
     * says is served until Mike approves it, so there is nothing
     * left for the AI to decide.
     */
    public GuideEngine(GuideConfig cfg, IntentSet intents, KnowledgeBase knowledge,
                       StatsView stats, LongSupplier clock, UnansweredLog unansweredLog,
                       AiProvider aiProvider, FactsView aiFacts, AiLog aiLog,
                       AiExecutor aiExecutor, LearnCache aiCache, LearnedAnswers learned,
                       PeerCandidates peerStore, PeerLog peerLog) {
        this.cfg = cfg;
        this.intents = intents;
        this.knowledge = knowledge;
        this.stats = stats;
        this.clock = clock;
        this.unansweredLog = unansweredLog;
        this.limiter = new RateLimiter(cfg);
        this.aiProvider = aiProvider;
        this.aiFacts = aiFacts;
        this.aiLog = aiLog;
        this.aiExecutor = aiExecutor;
        this.aiCache = aiCache;
        this.learned = learned == null ? LearnedAnswers.empty() : learned;
        this.peerStore = peerStore;
        this.peerLog = peerLog;
        this.aiGate = new AiGate(cfg.aiPerPlayerCooldownSeconds, cfg.aiDailyBudget,
                java.time.ZoneId.systemDefault());
    }

    // ---------- toggles (glue persists via togglesText/loadToggles) ----------

    public synchronized boolean isPlayerOff(UUID player) {
        return Boolean.TRUE.equals(togglesOff.get(player));
    }

    public synchronized void setPlayerOff(UUID player, boolean off) {
        togglesOff.put(player, off);
        if (off) {
            clarifyPending.remove(player);
        }
    }

    public synchronized String togglesText() {
        return PlayerToggles.serialize(togglesOff);
    }

    public synchronized void loadToggles(String text) {
        togglesOff.clear();
        togglesOff.putAll(PlayerToggles.parse(text));
    }

    // ---------- status ----------

    public synchronized long answersServed() {
        return answersServed;
    }

    public synchronized long unansweredCount() {
        return unansweredCount;
    }

    public RateLimiter limiter() {
        return limiter;
    }

    // ---------- AI fallback status (0.2.0) ----------

    /** True when the fallback can engage: provider wired AND config switch on AND a key present. */
    public synchronized boolean aiActive() {
        return aiProvider != null && aiExecutor != null && cfg.aiConfigured();
    }

    /** AI calls engaged today (the daily budget's spent count). */
    public synchronized long aiCallsToday() {
        return aiGate.usedToday(clock.getAsLong());
    }

    /** AI engagements that produced an answer this run. */
    public synchronized long aiAnsweredCount() {
        return aiAnswered;
    }

    /** AI engagements that failed (timeout, HTTP error, network) this run (0.3.1). */
    public synchronized long aiFailedCount() {
        return aiFailed;
    }

    /** Where console warnings go (0.3.1). Null = silent, as before. */
    public void setWarnSink(java.util.function.Consumer<String> sink) {
        this.warnSink = sink;
    }

    private void warn(String message) {
        java.util.function.Consumer<String> w = warnSink;
        if (w == null) {
            return;
        }
        try {
            w.accept(message);
        } catch (RuntimeException ignored) {
            // a broken logger never breaks chat
        }
    }

    /** Safe one-line description of a provider failure: kind and HTTP status only. */
    static String describeAiFailure(Exception e) {
        if (e instanceof AiProvider.AiException ae) {
            return ae.kind() + (ae.status() > 0 ? " (HTTP " + ae.status() + ")" : "");
        }
        return e.getClass().getSimpleName();
    }

    // ---------- learn status (0.2.1) ----------

    /** Promoted (learned) answers currently loaded. */
    public synchronized int learnedCount() {
        return learned.size();
    }

    /** Entries currently in the AI learn-cache (0 when caching is off/absent). */
    public synchronized int aiCacheSize() {
        return aiCache == null ? 0 : aiCache.size();
    }

    /** Cache answers served this run. */
    public synchronized long aiCacheServedCount() {
        return cacheServed;
    }

    /** Persist pending cache hit counters (glue: on disable / before a reload swap). */
    public synchronized void flushCache() {
        if (aiCache != null) {
            aiCache.flush();
        }
    }

    // ---------- peer-learning status (0.2.3) ----------

    /** True when peer learning can capture, learn, and serve. */
    public synchronized boolean peerEnabled() {
        return peerStore != null && cfg.peerEnabled;
    }

    /** Approved player answers (the only ones ever served). */
    public synchronized int peerLearnedCount() {
        return peerStore == null ? 0 : peerStore.count(PeerCandidates.APPROVED);
    }

    /** Player answers waiting for /guide review (never served). */
    public synchronized int peerReviewCount() {
        return peerStore == null ? 0 : peerStore.count(PeerCandidates.REVIEW);
    }

    /** Peer answers served this run. */
    public synchronized long peerServedCount() {
        return peerServed;
    }

    /** Persist pending peer hit counters (glue: on disable / before a reload swap). */
    public synchronized void flushPeer() {
        if (peerStore != null) {
            peerStore.flush();
        }
    }

    /**
     * Periodic housekeeping (0.2.3; 0.3.0): the glue calls this on a
     * timer so capture windows close - and the decay sweep runs -
     * even when chat goes quiet. handle() runs the same close-out
     * lazily on every message, so a busy chat never waits on the
     * timer either.
     */
    public synchronized void tick() {
        processDueDelayed(clock.getAsLong());
        if (peerEnabled()) {
            settlePeerWindows(clock.getAsLong());
        }
    }

    // ---------- main entry ----------

    /**
     * Handle one message. {@code onlineNames} are the display names of
     * everyone online (including the asker). {@code recentlyDamaged} is
     * true when the asker took damage within the combat-quiet window.
     * Returns the replies to deliver (usually 0 or 1; long answers wrap
     * to at most 2 lines, each its own Reply with the same channel).
     */
    public synchronized List<Reply> handle(PlayerRef asker, String rawMessage, Door door,
                                           Collection<String> onlineNames, boolean recentlyDamaged) {
        return handle(asker, rawMessage, door, onlineNames, recentlyDamaged, null);
    }

    /**
     * Handle one message, with an async sink for the AI fallback
     * (0.2.0). When the fallback engages, this returns immediately -
     * with the thinking line, when notices are on (0.2.2) - and the
     * answer - or the standard non-answer, if the AI cannot ground
     * one - arrives later through {@code sink}.
     * A null sink disables the fallback (the non-answer is served
     * synchronously, exactly as before 0.2.0).
     */
    public synchronized List<Reply> handle(PlayerRef asker, String rawMessage, Door door,
                                           Collection<String> onlineNames, boolean recentlyDamaged,
                                           AsyncSink sink) {
        if (!cfg.enabled || asker == null || rawMessage == null) {
            return List.of();
        }
        if (door == Door.COMMAND && !cfg.doorCommand) {
            return List.of();
        }
        if (door == Door.SUMMON && !cfg.doorSummon) {
            return List.of();
        }
        if (door == Door.AMBIENT && !cfg.doorAmbient) {
            return List.of();
        }
        String norm = TextNorm.normalize(rawMessage);
        if (norm.isBlank()) {
            return List.of();
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        long now = clock.getAsLong();

        // Peer learning (0.2.3; gates rewritten in 0.3.0): expired
        // capture windows settle first (a line arriving after the
        // bell is not evidence), a question on a queued/approved
        // topic refreshes its decay clock, then this line - plain
        // public chat only; /ask and summon lines are questions, not
        // teaching - is offered to the tracker. It never produces a
        // reply; the tracker enforces who may teach and every gate.
        if (peerEnabled()) {
            notePeerSpeaker(asker, onlineNames);
            settlePeerWindows(now);
            notePeerAsk(rawMessage, norm, now);
            if (door == Door.AMBIENT) {
                feedPeer(asker, rawMessage, norm, tokens, now);
            }
        }

        // Delayed answers (0.3.2): this line may hold the Guide (another
        // player replied), cancel a waiting question ("nvm", or the asker
        // asked it directly), or record the asker's thanks. Then anything
        // that came due is settled. Neither step produces a reply here;
        // delayed answers go out through the DelayedSink.
        if (!pendingAnswers.isEmpty()) {
            noteDelayedChat(asker, rawMessage, norm, tokens, door, now);
            processDueDelayed(now);
        }

        // A pending clarification outranks every ambient guard: the
        // player is mid-conversation with the Guide, on purpose.
        Long pendingExpiry = clarifyPending.get(asker.id());
        if (pendingExpiry != null) {
            if (now > pendingExpiry) {
                clarifyPending.remove(asker.id());
            } else {
                List<Reply> r = handleClarifyReply(asker, norm, tokens, rawMessage, now);
                if (r != null) {
                    return serveOrNotice(asker, door, now, r);
                }
                // null = the line is really a new question; fall through.
            }
        }

        if (door != Door.COMMAND && isPlayerOff(asker.id())) {
            return List.of();
        }

        if (door == Door.AMBIENT) {
            if (recentlyDamaged) {
                return List.of();
            }
            // Addressed to another player wins (guardrail 2).
            if (mentionsOtherPlayer(norm, tokens, asker, onlineNames)) {
                return List.of();
            }
        } else {
            // Asked the Guide directly about somebody else's stats.
            if (mentionsOtherPlayer(norm, tokens, asker, onlineNames)
                    && !java.util.Collections.disjoint(tokens, STAT_WORDS)) {
                return serveOrNotice(asker, door, now, deliver(asker, norm, false, now,
                        List.of(new Reply(Channel.PRIVATE, OTHER_PLAYER_REFUSAL))));
            }
        }

        String skillToken = findSkillToken(norm);
        IntentSet.Match match = intents.match(norm, tokens, door == Door.AMBIENT, skillToken != null);
        if (match != null && match.score() >= cfg.intentMinScore) {
            return serveOrNotice(asker, door, now,
                    answerIntent(asker, match.intent(), norm, skillToken, door, now));
        }

        // Promoted answers (0.2.1): human-reviewed AI answers that
        // graduated into the deterministic tier. Asked doors only -
        // like the AI answers they came from, never ambient - and
        // ahead of retrieval, the cache, and the AI itself.
        if (door != Door.AMBIENT) {
            String promoted = learned.match(norm);
            if (promoted != null) {
                return serveOrNotice(asker, door, now, deliver(asker, norm, false, now,
                        wrap(Channel.PRIVATE, substitute(promoted, asker))));
            }
        }

        // No intent cleared its bar.
        if (door == Door.AMBIENT) {
            // Silence - but remember plausible server questions
            // (question-shaped, some topic overlap) for the growth list.
            if (TextNorm.questionShaped(rawMessage, norm)) {
                Retrieval.Result r = Retrieval.search(knowledge, norm, null);
                // 0.3.2 delayed answers: a real game question is
                // researched now, answered after the delay unless
                // another player answers first. Silent right now.
                if (scheduleDelayed(asker, rawMessage, norm, r, now)) {
                    openPeerWindow(asker, rawMessage, norm, door, now);
                    return List.of();
                }
                if (r.best() != null && r.bestScore() >= 4) {
                    logUnanswered(asker, rawMessage, "AMBIENT", r);
                    // Peer learning (0.2.3): an unanswered question
                    // in open chat opens a capture window too - the
                    // valley may answer what the Guide could not.
                    openPeerWindow(asker, rawMessage, norm, door, now);
                    // 0.3.1 ambient hint (ai.ambient-hint, off by default):
                    // plain chat never reaches the AI, so point this asker
                    // at /ask - privately, at most once per window.
                    List<Reply> hint = ambientHint(asker, now);
                    if (hint != null) {
                        return hint;
                    }
                }
            }
            return List.of();
        }

        // Command / summon: try open retrieval, privately.
        Retrieval.Result r = Retrieval.search(knowledge, norm, null);
        if (Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin)) {
            List<Reply> replies = wrap(Channel.PRIVATE, r.answerText());
            List<Reply> out = deliver(asker, norm, false, now, replies);
            if (out.isEmpty() && limiterVerdictWasGlobal(asker, norm, now)) {
                logUnanswered(asker, rawMessage, door.name(), r);
            }
            return serveOrNotice(asker, door, now, out);
        }
        // The learn-cache (0.2.1): an AI answer given before, still
        // fresh against the current knowledge stamp, serves here -
        // instantly, with no AI call and no budget spent.
        List<Reply> cached = serveCache(asker, rawMessage, norm, door, now);
        if (cached != null) {
            return serveOrNotice(asker, door, now, cached);
        }
        // Player answers (0.2.3; 0.3.0: approved by Mike only, served
        // as "A player said: ...", never with a name). Ranked below
        // intents, promoted answers, retrieval, and the AI cache -
        // peer answers only fill gaps the valley itself cannot.
        List<Reply> peer = servePeer(asker, norm, now);
        if (peer != null) {
            return serveOrNotice(asker, door, now, peer);
        }
        // The would-be non-answer path (0.2.0): the AI fallback gets
        // one fenced attempt before the Guide gives up honestly. Only
        // the asked doors reach this point - ambient returned above,
        // and the tryAiFallback door check keeps it that way.
        if (tryAiFallback(asker, rawMessage, norm, door, r, sink)) {
            // Engaged (0.2.2): the thinking line returns synchronously
            // so the seconds before the async answer lands are never
            // silence. The answer itself still arrives via the sink.
            return thinkingNotice(asker);
        }
        // Turned away by the AI bucket on this player's OWN cooldown
        // (0.2.2): quote the real wait instead of a don't-know that
        // is not true yet - after the wait, the same question gets
        // its AI attempt. The budget and in-flight blocks have no
        // per-player wait to quote, so they keep the plain
        // non-answer below. A throttled or disabled notice also
        // falls through to the non-answer: never silence either way.
        long aiWait = (sink != null && door != Door.AMBIENT && aiActive())
                ? aiGate.cooldownRemainingMillis(asker.id(), now) : 0;
        if (aiWait > 0) {
            List<Reply> notice = cooldownNotice(asker, door, now, aiWait);
            if (!notice.isEmpty()) {
                return notice;
            }
        }
        logUnanswered(asker, rawMessage, door.name(), r);
        // Peer learning (0.2.3): this question is officially
        // unanswered - open the capture window and let the valley
        // try. (The AI-cooldown notice path above deliberately does
        // NOT open one: that question was gated, not unanswered.)
        // 0.3.0: public summons only - a private /ask opens nothing.
        openPeerWindow(asker, rawMessage, norm, door, now);
        return serveOrNotice(asker, door, now, deliver(asker, norm, false, now,
                List.of(new Reply(Channel.PRIVATE, NON_ANSWER))));
    }

    // ---------- learn-cache (0.2.1) ----------

    /**
     * Serve a cached AI answer for this question, or return null to
     * let the normal path continue (no entry, caching off, the AI
     * feature off, or the entry went stale). A stale entry - its
     * knowledge stamp no longer matches the live knowledge - is
     * dropped and noted in the AI log, and the question continues to
     * the AI exactly as if it had never been cached; a fresh answer
     * re-caches under the new stamp. A fresh hit serves through the
     * standard private-answer limiter like any deterministic answer:
     * no AiGate reservation, no budget, no provider call.
     */
    private List<Reply> serveCache(PlayerRef asker, String rawMessage, String norm,
                                   Door door, long now) {
        if (aiCache == null || !cfg.aiCacheEnabled || !aiActive()) {
            return null;
        }
        LearnCache.Entry entry = aiCache.get(norm);
        if (entry == null) {
            return null;
        }
        if (!knowledge.version().equals(entry.stamp())) {
            aiCache.remove(norm);
            logCacheEvent(asker, rawMessage, door, "stale-dropped");
            return null;
        }
        List<Reply> out = deliver(asker, norm, false, now,
                wrap(Channel.PRIVATE, entry.answer()));
        if (!out.isEmpty()) {
            aiCache.recordHit(norm, now);
            cacheServed++;
            logCacheEvent(asker, rawMessage, door, "hit");
        }
        return out;
    }

    private void logCacheEvent(PlayerRef asker, String rawMessage, Door door, String kind) {
        if (aiLog == null) {
            return;
        }
        try {
            aiLog.appendCacheEvent(Instant.ofEpochMilli(clock.getAsLong()), asker.name(),
                    asker.id().toString(), door.name(), kind, rawMessage);
        } catch (RuntimeException ignored) {
            // The Guide never breaks chat because a log failed.
        }
    }

    // ---------- peer learning (0.3.0: review queue) ----------

    /*
     * How the valley teaches the Guide (0.3.0). Rewritten after 0.2.3
     * learned 65 chat lines - most of them chatter ("<name> come
     * back", "my brain is fried") - with a player's name on each.
     *
     * 1. A WINDOW opens only for a PUBLIC question (plain chat, or a
     *    "Guide, ..." summon - never a private /ask nobody else can
     *    see) that the Guide could not answer AND that passes the
     *    question gate (PeerFilter: a real game question, not social
     *    chat, no player names, no profanity, no seal-fight spoiler).
     * 2. ANSWERS: for answer-window-seconds (default 120) the next
     *    plain-chat lines from OTHER players are offered to the
     *    answer gate (PeerFilter). One line goes to ONE window only -
     *    the open question it shares the most words with - and each
     *    player gets one candidate per question. At most two
     *    candidates per window.
     * 3. CORROBORATION: until the window closes (capture-window-
     *    minutes, default 5), a candidate needs EITHER min-agreements
     *    (default 2) distinct other players backing it - a short
     *    "yeah thats right", or their own similar answer - OR the
     *    asker's own thanks / "that worked". The asker's "yeah" no
     *    longer counts, and any "no / actually / thats wrong" disputes
     *    the candidate: a disputed candidate is never queued.
     * 4. REVIEW: a corroborated candidate goes to the review queue
     *    (peer-review.yml) and is NEVER served from there. Mike
     *    approves or rejects it with /guide review. auto-approve is
     *    off by default.
     * 5. SERVING: approved answers only, on the asked doors, below
     *    intents, promoted answers, retrieval and the AI cache, as
     *    "A player said: ...". The spoiler, profanity and name
     *    screens run again at serve time.
     * 6. DECAY: a review entry that nobody asks about again within
     *    candidate-expire-days (default 14) expires.
     *
     * No player names or UUIDs are stored in the review file or the
     * peer log, and none are ever served.
     */

    /** One candidate answer inside a live window (in memory only). */
    private static final class PeerCand {
        final UUID author;
        final String text;
        final Set<String> terms;
        final long at;
        final Set<UUID> agreers = new java.util.LinkedHashSet<>();
        boolean askerConfirmed;
        boolean disputed;

        PeerCand(UUID author, String text, Set<String> terms, long at) {
            this.author = author;
            this.text = text;
            this.terms = terms;
            this.at = at;
        }
    }

    /** One open capture window (0.3.0): in memory only; nothing persists until it settles. */
    private static final class PeerWindow {
        final String key;          // PeerFilter.topicKey of the question
        final String question;     // normalized
        final Set<String> qTerms;  // meaningful words of the question
        final UUID asker;
        final long openedAt;
        final long answerUntil;
        final long closesAt;
        final List<PeerCand> cands = new ArrayList<>();
        boolean guideAnswered;     // 0.3.2: the Guide posted its own answer to this question

        PeerWindow(String key, String question, Set<String> qTerms, UUID asker,
                   long openedAt, long answerUntil, long closesAt) {
            this.key = key;
            this.question = question;
            this.qTerms = qTerms;
            this.asker = asker;
            this.openedAt = openedAt;
            this.answerUntil = answerUntil;
            this.closesAt = closesAt;
        }

        PeerCand candBy(UUID who) {
            for (PeerCand c : cands) {
                if (c.author.equals(who)) {
                    return c;
                }
            }
            return null;
        }

        long lastActivity() {
            long t = openedAt;
            for (PeerCand c : cands) {
                t = Math.max(t, c.at);
            }
            return t;
        }
    }

    /** Names seen in chat this run (on top of who is online): the name screen's memory. */
    private final java.util.Set<String> peerSeenNames = new java.util.LinkedHashSet<>();
    private Collection<String> peerLastOnline = List.of();
    private java.util.function.Consumer<String> peerNotifier; // glue: tells online ops
    private PeerFilter peerFilter;

    /** The glue's hook for "a new answer is waiting for review" (may be null). */
    public synchronized void setPeerNotifier(java.util.function.Consumer<String> notifier) {
        this.peerNotifier = notifier;
    }

    private PeerFilter filter() {
        if (peerFilter == null) {
            peerFilter = new PeerFilter(PeerFilter.lists(cfg.peerGameTerms, cfg.peerSpoilerTerms,
                    cfg.peerBlockedWords, cfg.peerBlockedNames, cfg.peerSocialPhrases,
                    cfg.peerChatterPhrases), cfg.peerMinAnswerChars, cfg.peerMinAnswerWords);
        }
        return peerFilter;
    }

    private Collection<String> knownNames() {
        List<String> all = new ArrayList<>(peerLastOnline);
        all.addAll(peerSeenNames);
        return all;
    }

    private void notePeerSpeaker(PlayerRef who, Collection<String> onlineNames) {
        if (who != null && who.name() != null) {
            peerSeenNames.add(who.name());
            if (peerSeenNames.size() > 500) {
                peerSeenNames.remove(peerSeenNames.iterator().next());
            }
        }
        if (onlineNames != null) {
            peerLastOnline = List.copyOf(onlineNames);
        }
    }

    /** A question matching a queued or approved topic refreshes its decay clock. */
    private void notePeerAsk(String raw, String norm, long now) {
        if (!TextNorm.questionShaped(raw, norm)) {
            return;
        }
        String key = PeerFilter.topicKey(norm);
        if (key.isBlank()) {
            return;
        }
        PeerCandidates.Entry e = peerStore.byKey(key);
        if (e != null) {
            peerStore.touch(e.asked(now));
        }
    }

    /**
     * Open a capture window for a public question the Guide could not
     * answer. Private /ask questions (Door.COMMAND) never open one:
     * nobody else can see them, so any "answer" in chat is about
     * something else (0.2.3 learned "i found a glitch" that way).
     */
    private void openPeerWindow(PlayerRef asker, String raw, String norm, Door door, long now) {
        if (!peerEnabled() || norm == null || norm.isBlank() || door == Door.COMMAND) {
            return;
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (LearnPrivacy.isPersonalQuestion(tokens)
                && !java.util.Collections.disjoint(tokens, STAT_WORDS)) {
            return;
        }
        String reject = filter().questionReject(raw, norm, knownNamesExcept(asker));
        String key = PeerFilter.topicKey(norm);
        if (reject == null && key.isBlank()) {
            reject = "q-too-short";
        }
        if (reject != null) {
            logPeer("skipped", norm, "reason=" + reject);
            return;
        }
        if (peerWindows.containsKey(key) || peerWindows.size() >= MAX_PEER_WINDOWS) {
            return;
        }
        PeerCandidates.Entry onFile = peerStore.byKey(key);
        if (onFile != null && onFile.isApproved()) {
            return; // already taught and approved
        }
        peerWindows.put(key, new PeerWindow(key, norm, PeerFilter.meaningful(norm), asker.id(), now,
                now + cfg.peerAnswerWindowSeconds * 1000L,
                now + cfg.peerCaptureWindowMinutes * 60_000L));
        logPeer("window", norm);
    }

    /** Known names minus the speaker's own (the asker naming themself is not naming a player). */
    private Collection<String> knownNamesExcept(PlayerRef self) {
        List<String> out = new ArrayList<>();
        for (String n : knownNames()) {
            if (self == null || n == null || !n.equalsIgnoreCase(self.name())) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * Offer one plain-chat line to the open windows. In order: the
     * asker's own signals (thanks / disagreement), a third player's
     * disagreement or short agreement, and finally a possible answer.
     * Never produces a reply.
     */
    private void feedPeer(PlayerRef speaker, String raw, String norm, Set<String> tokens, long now) {
        if (peerWindows.isEmpty()) {
            return;
        }
        UUID who = speaker.id();
        boolean disagree = PeerGuard.isDisagreement(norm, tokens);

        // 1) The asker of an open window: thanks confirms, "no" disputes, "yeah" counts for nothing.
        PeerWindow asked = latest(w -> w.asker.equals(who) && !w.cands.isEmpty() && now < w.closesAt);
        if (asked != null) {
            if (disagree) {
                disputeLatest(asked, who);
                return;
            }
            if (PeerFilter.isAskerConfirmation(raw, norm)) {
                if (asked.guideAnswered) {
                    // 0.3.2: the Guide already answered this asker, so a
                    // "thanks" now may be for the Guide, not the player.
                    logPeer("thanks-ambiguous", asked.question);
                    return;
                }
                List<PeerCand> live = new ArrayList<>();
                for (PeerCand c : asked.cands) {
                    if (!c.disputed) {
                        live.add(c);
                    }
                }
                if (live.size() == 1 && !live.get(0).askerConfirmed) {
                    live.get(0).askerConfirmed = true;
                    logPeer("confirmed", asked.question);
                }
                return;
            }
            if (PeerFilter.isAgreement(raw, norm)) {
                return; // the asker's "yeah" is not confirmation
            }
        }

        // 2) A third player's disagreement or short agreement goes to
        //    the most recently answered window they did not answer.
        //    With no answer standing yet, a "nope, you need..." line
        //    falls through and may itself be the answer.
        boolean agree = PeerFilter.isAgreement(raw, norm)
                // 0.3.2: a third player's "thanks" backs the answer too
                // (one agreement; min-agreements still applies).
                || (cfg.peerOthersThanksCount && !disagree && PeerFilter.isAskerConfirmation(raw, norm));
        if (disagree || agree) {
            PeerWindow w = latest(x -> !x.cands.isEmpty() && now < x.closesAt
                    && !x.asker.equals(who) && x.candBy(who) == null);
            if (w != null && disagree) {
                disputeLatest(w, who);
                return;
            }
            if (w != null && w.cands.size() == 1) { // with two rival answers a bare "yeah" backs nobody
                PeerCand c = w.cands.get(0);
                if (!c.disputed && c.agreers.add(who)) {
                    logPeer("agreed", w.question, "count=" + c.agreers.size());
                }
            }
            if (w != null || agree) {
                return;
            }
            // a disagreement with nothing to dispute: try it as an answer below
        }

        // 3) A possible answer: pick the ONE best window for it.
        String cleaned = AiPrompts.sanitize(raw, cfg.peerMaxAnswerChars);
        Collection<String> names = knownNamesExcept(speaker);
        PeerWindow best = null;
        int bestOverlap = -1;
        String firstReject = null;
        PeerWindow firstRejectWindow = null;
        for (PeerWindow w : peerWindows.values()) {
            if (now > w.answerUntil || w.asker.equals(who) || w.candBy(who) != null) {
                continue;
            }
            String reject = filter().answerReject(raw, cleaned, w.qTerms, cfg.peerMaxAnswerChars,
                    names, speaker.name());
            if (reject != null) {
                if (firstReject == null || w.openedAt > firstRejectWindow.openedAt) {
                    firstReject = reject;
                    firstRejectWindow = w;
                }
                continue;
            }
            int ov = PeerFilter.overlap(PeerFilter.meaningful(TextNorm.normalize(cleaned)), w.qTerms);
            if (ov > bestOverlap || (ov == bestOverlap && w.openedAt > best.openedAt)) {
                best = w;
                bestOverlap = ov;
            }
        }
        if (best == null) {
            if (firstReject != null && SUBSTANTIVE_REJECTS.contains(firstReject)) {
                logPeer("rejected", firstRejectWindow.question, "reason=" + firstReject);
            }
            return;
        }
        Set<String> terms = PeerFilter.meaningful(TextNorm.normalize(cleaned));
        // An independent answer that says the same thing is agreement, not a rival.
        for (PeerCand c : best.cands) {
            if (!c.disputed && similar(c.terms, terms)) {
                if (c.agreers.add(who)) {
                    logPeer("agreed", best.question, "count=" + c.agreers.size());
                }
                return;
            }
        }
        if (best.cands.size() >= 2) {
            return;
        }
        best.cands.add(new PeerCand(who, cleaned, terms, now));
        logPeer("candidate", best.question);
    }

    /** Refusals worth a log line; chatter and off-topic lines are ordinary chat, not news. */
    private static final Set<String> SUBSTANTIVE_REJECTS = Set.of(
            "profanity", "names-player", "spoiler", "personal", "stat-content", "other-player",
            "too-long");

    private static boolean similar(Set<String> a, Set<String> b) {
        int ov = PeerFilter.overlap(a, b);
        return ov >= 2 && ov * 2 >= Math.min(a.size(), b.size());
    }

    private void disputeLatest(PeerWindow w, UUID who) {
        PeerCand target = null;
        for (PeerCand c : w.cands) {
            if (!c.author.equals(who) && (target == null || c.at >= target.at)) {
                target = c;
            }
        }
        if (target != null && !target.disputed) {
            target.disputed = true;
            logPeer("disputed", w.question);
        }
    }

    private PeerWindow latest(java.util.function.Predicate<PeerWindow> test) {
        PeerWindow best = null;
        for (PeerWindow w : peerWindows.values()) {
            if (test.test(w) && (best == null || w.lastActivity() > best.lastActivity())) {
                best = w;
            }
        }
        return best;
    }

    /** Settle every window whose time is up, then run the decay sweep. */
    private void settlePeerWindows(long now) {
        if (peerStore == null) {
            return;
        }
        List<PeerWindow> done = new ArrayList<>();
        for (PeerWindow w : peerWindows.values()) {
            if (now >= w.closesAt) {
                done.add(w);
            }
        }
        for (PeerWindow w : done) {
            peerWindows.remove(w.key);
            settlePeerWindow(w, now);
        }
        if (now - lastPeerSweep >= 60_000L) {
            lastPeerSweep = now;
            for (PeerCandidates.Entry gone : peerStore.expire(now,
                    cfg.peerCandidateExpireDays * 86_400_000L,
                    cfg.peerApprovedExpireDays * 86_400_000L)) {
                logPeer("expired", gone.question(), "id=" + gone.id());
            }
        }
    }

    private long lastPeerSweep = Long.MIN_VALUE / 2;

    private void settlePeerWindow(PeerWindow w, long now) {
        if (w.cands.isEmpty()) {
            logPeer("no-answer", w.question);
            return;
        }
        List<PeerCand> backed = new ArrayList<>();
        boolean anyDisputed = false;
        for (PeerCand c : w.cands) {
            anyDisputed |= c.disputed;
            if (!c.disputed && (c.askerConfirmed || c.agreers.size() >= cfg.peerMinAgreements)) {
                backed.add(c);
            }
        }
        if (backed.isEmpty()) {
            logPeer("dropped", w.question, "reason=" + (anyDisputed ? "disputed" : "uncorroborated"));
            return;
        }
        if (backed.size() > 1) {
            logPeer("dropped", w.question, "reason=contested");
            return;
        }
        PeerCand c = backed.get(0);
        // Serve-time screens, once more at the door of the queue.
        String late = filter().serveReject(c.text, knownNames());
        if (late != null) {
            logPeer("dropped", w.question, "reason=" + late);
            return;
        }
        PeerCandidates.Entry onFile = peerStore.byKey(w.key);
        if (onFile != null && onFile.isApproved()) {
            return;
        }
        StringBuilder how = new StringBuilder();
        if (c.askerConfirmed) {
            how.append("the asker said it worked");
        }
        if (!c.agreers.isEmpty()) {
            if (how.length() > 0) {
                how.append(", ");
            }
            how.append(c.agreers.size()).append(c.agreers.size() == 1 ? " player agreed" : " players agreed");
        }
        if (w.cands.size() > 1) {
            how.append(" (a different answer was also given)");
        }
        PeerCandidates.Entry entry = PeerCandidates.Entry.review(w.question, w.key, c.text,
                how.toString(), c.agreers.size(), c.askerConfirmed, now);
        if (onFile != null) { // keep the decay history of a topic already waiting
            entry = new PeerCandidates.Entry(entry.id(), entry.question(), entry.key(), entry.state(),
                    entry.answer(), entry.how(), entry.agreements(), entry.askerConfirmed(),
                    onFile.created(), now, onFile.asks() + 1, 0L, "", 0, 0L);
        }
        boolean auto = (cfg.peerAutoApprove && c.askerConfirmed
                && c.agreers.size() >= cfg.peerAutoApproveMinAgreements)
                // 0.3.2: Mike's "bank it for next time" - the asker's
                // thanks alone approves, when this switch is on. Every
                // filter above already ran; nobody disputed it.
                || (cfg.peerAutoApproveThanked && c.askerConfirmed && !anyDisputed);
        if (auto) {
            entry = entry.approved("auto", now);
        }
        peerStore.put(entry);
        logPeer("queued", w.question, "id=" + entry.id());
        if (auto) {
            logPeer("approved", w.question, "id=" + entry.id(), "by=auto");
        }
        if (peerNotifier != null && cfg.peerNotifyOps) {
            try {
                peerNotifier.accept(auto
                        ? "Guide auto-approved a player answer (#" + entry.id() + "). /guide review approved"
                        : "Guide: a player answer is waiting for review (#" + entry.id()
                                + "). /guide review");
            } catch (RuntimeException ignored) {
                // A notice failure never breaks chat.
            }
        }
    }

    /**
     * Serve an APPROVED player answer, or null to let the normal path
     * continue. Review entries are never served. The screens run
     * again here, so a spoiler term added later also blocks an
     * answer approved earlier.
     */
    private List<Reply> servePeer(PlayerRef asker, String norm, long now) {
        if (!peerEnabled()) {
            return null;
        }
        String key = PeerFilter.topicKey(norm);
        if (key.isBlank()) {
            return null;
        }
        PeerCandidates.Entry e = peerStore.byKey(key);
        if (e == null || !e.isApproved()) {
            return null;
        }
        String blocked = filter().serveReject(e.answer(), knownNames());
        if (blocked != null) {
            logPeer("serve-blocked", e.question(), "reason=" + blocked, "id=" + e.id());
            return null;
        }
        List<Reply> out = deliver(asker, norm, false, now,
                wrap(Channel.PRIVATE, cfg.peerServePrefix + e.answer()));
        if (!out.isEmpty()) {
            peerStore.touch(e.hit(now));
            peerServed++;
            logPeer("served", e.question(), "id=" + e.id());
        }
        return out;
    }

    // ---------- review queue API (0.3.0; the glue's /guide review) ----------

    public synchronized List<PeerCandidates.Entry> peerReviewQueue() {
        return peerStore == null ? List.of() : peerStore.inState(PeerCandidates.REVIEW);
    }

    public synchronized List<PeerCandidates.Entry> peerApprovedList() {
        return peerStore == null ? List.of() : peerStore.inState(PeerCandidates.APPROVED);
    }

    public synchronized PeerCandidates.Entry peerFind(String id) {
        return peerStore == null ? null : peerStore.byId(id);
    }

    /** Approve a review entry. Returns the approved entry, or null when no such review entry. */
    public synchronized PeerCandidates.Entry peerApprove(String id) {
        PeerCandidates.Entry e = peerFind(id);
        if (e == null || e.isApproved()) {
            return null;
        }
        PeerCandidates.Entry ok = e.approved("owner", clock.getAsLong());
        peerStore.put(ok);
        logPeer("approved", ok.question(), "id=" + ok.id(), "by=owner");
        return ok;
    }

    /** Reject (review) or retire (approved) an entry: it is deleted. Returns it, or null. */
    public synchronized PeerCandidates.Entry peerReject(String id) {
        PeerCandidates.Entry e = peerFind(id);
        if (e == null) {
            return null;
        }
        peerStore.remove(e.key());
        logPeer("owner-rejected", e.question(), "id=" + e.id());
        return e;
    }

    /** True when a stored answer would be blocked at serve time right now (shown in review). */
    public synchronized String peerServeCheck(PeerCandidates.Entry e) {
        return e == null ? null : filter().serveReject(e.answer(), knownNames());
    }

    /** Open capture windows right now (status line). */
    public synchronized int peerOpenWindows() {
        return peerWindows.size();
    }

    private void logPeer(String kind, String question, String... fields) {
        if (peerLog == null) {
            return;
        }
        try {
            peerLog.event(Instant.ofEpochMilli(clock.getAsLong()), kind, question, fields);
        } catch (RuntimeException ignored) {
            // The Guide never breaks chat because its notebook did.
        }
    }

    // ---------- AI fallback (0.2.0) ----------

    /**
     * Engage the AI fallback for a question the deterministic engine
     * could not answer. Returns true when a call was engaged: the
     * gate reservation is taken here, synchronously, so a second
     * question from the same player (or a spent daily budget) falls
     * back to the plain non-answer instead of queueing calls. The
     * prompt is assembled here too - top-K excerpts plus the fact
     * sheet the PLUGIN fetched for this asker - and only the provider
     * call itself runs on the executor.
     */
    private boolean tryAiFallback(PlayerRef asker, String rawMessage, String norm, Door door,
                                  Retrieval.Result retrieval, AsyncSink sink) {
        if (sink == null || door == Door.AMBIENT || !aiActive()) {
            return false;
        }
        if (!aiGate.tryReserve(asker.id(), clock.getAsLong())) {
            return false;
        }
        List<KnowledgeBase.Chunk> excerpts = Retrieval.topK(knowledge, norm, cfg.aiTopChunks);
        List<String> askerFacts = safeAskerFacts(asker);
        AiPrompts.Prompt prompt = AiPrompts.build(cfg.personaName, NON_ANSWER, asker.name(),
                rawMessage, excerpts, askerFacts, safePublicFacts());
        AiProvider.Request request = new AiProvider.Request(prompt.system(), prompt.user(),
                cfg.aiModel, Math.max(1000, cfg.aiTimeoutSeconds * 1000));
        // The learn-cache privacy gate is decided HERE, while the
        // asker's fact sheet is in hand: a first-person question, or
        // an answer quoting the asker's own fact numbers, never
        // enters the cache (LearnPrivacy - both checks tested).
        boolean personal = LearnPrivacy.isPersonalQuestion(TextNorm.tokenSet(norm));
        java.util.Set<String> factNumbers = LearnPrivacy.numbersInFacts(askerFacts);
        aiExecutor.submit(() -> runAiCall(asker, rawMessage, norm, door, retrieval, request, sink,
                personal, factNumbers));
        return true;
    }

    /** The provider call itself, on the executor thread. Never throws. */
    private void runAiCall(PlayerRef asker, String rawMessage, String norm, Door door,
                           Retrieval.Result retrieval, AiProvider.Request request,
                           AsyncSink sink, boolean personalQuestion,
                           java.util.Set<String> factNumbers) {
        long started = clock.getAsLong();
        String answer = null;
        long latency = 0;
        try {
            AiProvider.Completion completion = aiProvider.complete(request);
            latency = Math.max(0, clock.getAsLong() - started);
            if (completion != null) {
                String cleaned = AiPrompts.sanitize(completion.text(), cfg.aiMaxAnswerChars);
                if (cleaned != null && !AiPrompts.isDontKnow(cleaned, NON_ANSWER)) {
                    answer = cleaned;
                }
            }
        } catch (Exception e) {
            // Timeout, 429, 500, network - every failure shape lands
            // in the same honest fallback; the ai log keeps the count.
            latency = Math.max(0, clock.getAsLong() - started);
            answer = null;
            noteAiFailure(e);
        } finally {
            aiGate.release();
        }
        boolean answered = answer != null;
        logAi(asker, rawMessage, door, answered, latency);
        if (!answered) {
            // The don't-know path lands in the unanswered log exactly
            // as it did before the AI existed, then the player hears
            // the standard non-answer - just a few seconds later.
            // Peer learning (0.2.3): the window opens HERE, when the
            // don't-know actually lands - an AI answer opens none.
            synchronized (this) {
                logUnanswered(asker, rawMessage, door.name(), retrieval);
                openPeerWindow(asker, rawMessage, norm, door, clock.getAsLong());
            }
            sink.accept(List.of(new Reply(Channel.PRIVATE, NON_ANSWER)));
            return;
        }
        List<Reply> replies = wrap(Channel.PRIVATE, answer);
        synchronized (this) {
            answersServed += replies.size();
            aiAnswered++;
        }
        // Learn from the answer (0.2.1): cache it against the
        // normalized question under the CURRENT knowledge stamp -
        // unless the privacy gate said no at engagement time, or the
        // answer quotes the asker's own fact numbers after all.
        // Don't-knows and failures never reach this line.
        if (aiCache != null && cfg.aiCacheEnabled && !personalQuestion
                && !LearnPrivacy.answerQuotesAny(answer, factNumbers)) {
            try {
                aiCache.put(norm, answer, knowledge.version(), clock.getAsLong());
            } catch (RuntimeException ignored) {
                // Caching is a bonus; a cache failure never eats an answer.
            }
        }
        sink.accept(replies);
    }

    /** 0.3.1 console line for a failed AI call (kind + status only, throttled). Never the key or question. */
    private void noteAiFailure(Exception e) {
        boolean say;
        long failNow = clock.getAsLong();
        synchronized (this) {
            aiFailed++;
            say = failNow - lastAiFailWarnAt >= WARN_EVERY_MILLIS;
            if (say) {
                lastAiFailWarnAt = failNow;
            }
        }
        if (say) {
            warn("AI fallback call failed: " + describeAiFailure(e)
                    + ". The player got the standard non-answer. (failures this run: "
                    + aiFailedCount() + "; next warning in 10 min at most)");
        }
    }

    private List<String> safeAskerFacts(PlayerRef asker) {
        if (aiFacts == null) {
            return List.of();
        }
        try {
            List<String> facts = aiFacts.askerFacts(asker.id());
            return facts == null ? List.of() : facts;
        } catch (RuntimeException e) {
            return List.of(); // a broken fact source never blocks the answer path
        }
    }

    private List<String> safePublicFacts() {
        if (aiFacts == null) {
            return List.of();
        }
        try {
            List<String> facts = aiFacts.publicFacts();
            return facts == null ? List.of() : facts;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private void logAi(PlayerRef asker, String rawMessage, Door door, boolean answered, long latency) {
        if (aiLog == null) {
            return;
        }
        try {
            aiLog.append(new AiLog.Entry(
                    Instant.ofEpochMilli(clock.getAsLong()), asker.name(),
                    asker.id().toString(), door.name(), answered, latency, rawMessage));
        } catch (RuntimeException e) {
            // The Guide never breaks chat because a log failed - but
            // since 0.3.1 it says so on the console (throttled).
            boolean say;
            long n = clock.getAsLong();
            synchronized (this) {
                say = n - lastAiLogWarnAt >= WARN_EVERY_MILLIS;
                if (say) {
                    lastAiLogWarnAt = n;
                }
            }
            if (say) {
                warn("could not write the AI log (" + cfg.aiLogFile + "): "
                        + e.getClass().getSimpleName() + ". AI answers still work; only the record is missing.");
            }
        }
    }

    private List<Reply> ambientHint(PlayerRef asker, long now) {
        if (!cfg.aiAmbientHint || !aiActive() || !cfg.doorCommand) {
            return null;
        }
        Long last = ambientHintAt.get(asker.id());
        if (last != null && now - last < cfg.aiAmbientHintHours * 3_600_000L) {
            return null;
        }
        ambientHintAt.put(asker.id(), now);
        return List.of(new Reply(Channel.PRIVATE, AMBIENT_HINT));
    }

    // ---------- delayed plain-chat answers (0.3.2) ----------

    /*
     * Mike's design (Oct 25, 2026): "a player asks a question in chat.
     * It reads it, does the research and then watches to see if
     * another player answers. If another player answers and someone
     * follows up with thanks or something of that sort it'll bank
     * that answer for next time that question is asked."
     *
     * Layers, in order:
     * 1. DETECT: plain chat, question-shaped, no intent answered it,
     *    and the peer question gate passes (a real game question, not
     *    social chat, names no player, no profanity, no seal-fight
     *    spoiler). Personal stat questions are left to the stat
     *    intents. Combat-quiet, /guide off and "addressed to a player"
     *    already returned earlier in handle().
     * 2. RESEARCH (silent): promoted answers, the curated /ask answers
     *    (non-stat intents), knowledge retrieval, the AI cache, approved
     *    player answers. If none has it, the AI is
     *    the last resort: called at once (ai-timing: early) or only
     *    when the delay runs out with nobody answering (late, the
     *    default, so a question the valley answers costs no budget).
     * 3. WATCH: for delay-seconds another player's on-topic reply, a
     *    captured peer candidate, or the asker's own thanks holds the
     *    Guide. "nvm" / "found it" or asking the same thing by /ask
     *    or "Guide," cancels. A held answer whose player candidate is
     *    disputed ("no thats wrong") before the delay ends is posted.
     * 4. BANK: unchanged peer learning (windows, agreements, thanks,
     *    review queue). 0.3.2 adds: a third player's thanks counts as
     *    an agreement, and peer-learning.auto-approve-thanked lets the
     *    asker's thanks alone approve.
     * 5. REUSE: an approved player answer is part of step 2, so the
     *    next plain-chat asker of that topic gets "A player said: ..."
     *    after the delay if nobody answers them first.
     * 6. ANSWER: in public chat by default (reply-channel: public,
     *    Mike Oct 25, 2026 5:18 PM CDT; private = the asker only).
     *    Every source still answers in public: website knowledge,
     *    learned and curated answers, the AI cache, approved player
     *    answers ("A player said: ...", no names) and the AI.
     *    PRIVACY (public mode): the asker's private details never
     *    reach a public line.
     *      a. The AI is asked with AiPrompts.buildPublic: knowledge
     *         excerpts + public facts + the question, and NO asker
     *         fact sheet, name or UUID. It cannot leak what it never
     *         received.
     *      b. publicSafe(): any answer (any source) that names the
     *         asker, or quotes a number from the asker's own fact
     *         sheet (fetched locally for the check, never sent), is
     *         delivered to the asker privately instead.
     *      c. Intents marked private, and the don't-know line, stay
     *         private. Personal stat questions never reach this layer.
     *    Through the normal rate limiter; one short retry if it is the
     *    only thing in the way. Nothing found and no AI = the standard
     *    non-answer, privately (say-dont-know).
     */

    /** One question waiting for its delayed answer (in memory only). */
    private static final class Pending {
        final String key;
        final PlayerRef asker;
        final String raw;
        final String norm;
        final Set<String> qTerms;
        final Retrieval.Result retrieval;
        final long askedAt;
        long dueAt;
        List<Reply> prepared;      // the researched answer, null while none
        String source;             // learned | intent | knowledge | cache | peer | ai
        PeerCandidates.Entry peerEntry;
        boolean needsAi;           // the AI is still to be asked (late timing)
        boolean aiInFlight;
        boolean askerThanked;
        boolean looseHeld;
        boolean retried;
        boolean done;

        Pending(String key, PlayerRef asker, String raw, String norm, Set<String> qTerms,
                Retrieval.Result retrieval, long askedAt, long dueAt) {
            this.key = key;
            this.asker = asker;
            this.raw = raw;
            this.norm = norm;
            this.qTerms = qTerms;
            this.retrieval = retrieval;
            this.askedAt = askedAt;
            this.dueAt = dueAt;
        }
    }

    /** Built-in "never mind" lines from the asker (delayed-answers.cancel-phrases adds more). */
    static final List<String> DEFAULT_CANCEL_PHRASES = List.of(
            "nvm", "never mind", "nevermind", "nm", "found it", "figured it out", "i figured it out",
            "i got it", "got it now", "dont worry", "no worries");

    private final Map<String, Pending> pendingAnswers = new java.util.LinkedHashMap<>(); // by topic key
    private final Map<UUID, Long> delayedLastAt = new HashMap<>();
    private volatile DelayedSink delayedSink;
    private long delayedScheduled;
    private long delayedPosted;
    private long delayedHeld;
    private long delayedCancelled;

    /** The glue's delivery hook for delayed answers. Null turns the layer off. */
    public synchronized void setDelayedSink(DelayedSink sink) {
        this.delayedSink = sink;
    }

    /** True when plain-chat questions get a delayed answer. */
    public synchronized boolean delayedActive() {
        return cfg.delayedEnabled && cfg.doorAmbient && delayedSink != null;
    }

    public synchronized int delayedPendingCount() {
        return pendingAnswers.size();
    }

    public synchronized long delayedScheduledCount() {
        return delayedScheduled;
    }

    public synchronized long delayedPostedCount() {
        return delayedPosted;
    }

    public synchronized long delayedHeldCount() {
        return delayedHeld;
    }

    public synchronized long delayedCancelledCount() {
        return delayedCancelled;
    }

    /** The asker left: their waiting question is dropped (glue: on quit). */
    public synchronized void forgetPlayer(UUID player) {
        for (Pending p : new ArrayList<>(pendingAnswers.values())) {
            if (p.asker.id().equals(player)) {
                endPending(p);
                delayedCancelled++;
                logPeer("delayed-cancelled", p.norm, "reason=left");
            }
        }
    }

    private boolean scheduleDelayed(PlayerRef asker, String raw, String norm,
                                    Retrieval.Result r, long now) {
        if (!delayedActive()) {
            return false;
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (LearnPrivacy.isPersonalQuestion(tokens)
                && !java.util.Collections.disjoint(tokens, STAT_WORDS)) {
            return false; // "what's my balance" belongs to the stat intents, privately
        }
        String reject = filter().questionReject(raw, norm, knownNamesExcept(asker));
        String key = PeerFilter.topicKey(norm);
        if (reject == null && key.isBlank()) {
            reject = "q-too-short";
        }
        if (reject != null) {
            logPeer("delayed-skipped", norm, "reason=" + reject);
            return false;
        }
        if (pendingAnswers.containsKey(key)) {
            return true; // the same question is already waiting: one answer, no echo
        }
        for (Pending p : pendingAnswers.values()) {
            if (p.asker.id().equals(asker.id())) {
                logPeer("delayed-skipped", norm, "reason=one-at-a-time");
                return false;
            }
        }
        if (pendingAnswers.size() >= cfg.delayedMaxPending) {
            logPeer("delayed-skipped", norm, "reason=busy");
            return false;
        }
        Long last = delayedLastAt.get(asker.id());
        if (last != null && now - last < cfg.delayedPerPlayerCooldownSeconds * 1000L) {
            logPeer("delayed-skipped", norm, "reason=cooldown");
            return false;
        }
        Pending p = new Pending(key, asker, raw, norm, PeerFilter.meaningful(norm), r, now,
                now + cfg.delayedDelaySeconds * 1000L);
        pendingAnswers.put(key, p);
        delayedLastAt.put(asker.id(), now);
        delayedScheduled++;
        prepareDelayed(p, now);
        logPeer("delayed-scheduled", norm, "source=" + (p.source != null ? p.source
                : p.aiInFlight ? "ai-early" : p.needsAi ? "ai-late" : "none"),
                "delay=" + cfg.delayedDelaySeconds);
        return true;
    }

    /** Step 2, research: the Guide's own sources in their usual order, the AI last. */
    private void prepareDelayed(Pending p, long now) {
        String promoted = learned.match(p.norm);
        if (promoted != null) {
            String text = substitute(promoted, p.asker);
            p.prepared = wrap(delayedChannel(p, text, null), text);
            p.source = "learned";
            return;
        }
        // The curated answers /ask would give (intents not marked for
        // plain chat). Stat and clarify intents are skipped: personal
        // numbers and a follow-up question do not belong in a delayed reply.
        IntentSet.Match m = intents.match(p.norm, TextNorm.tokenSet(p.norm), false,
                findSkillToken(p.norm) != null);
        if (m != null && m.score() >= cfg.intentMinScore) {
            String text = delayedIntentText(m.intent(), p.norm, p.asker);
            if (text != null) {
                Channel ch = m.intent().isPrivate() ? Channel.PRIVATE : delayedChannel(p, text, null);
                p.prepared = wrap(ch, text);
                p.source = "intent";
                return;
            }
        }
        if (Retrieval.clearsBar(p.retrieval, cfg.retrievalMinScore, cfg.retrievalMargin)) {
            String text = p.retrieval.answerText();
            p.prepared = wrap(delayedChannel(p, text, null), text);
            p.source = "knowledge";
            return;
        }
        if (aiCache != null && cfg.aiCacheEnabled && aiActive()) {
            LearnCache.Entry entry = aiCache.get(p.norm);
            if (entry != null) {
                if (knowledge.version().equals(entry.stamp())) {
                    p.prepared = wrap(delayedChannel(p, entry.answer(), null), entry.answer());
                    p.source = "cache";
                    return;
                }
                aiCache.remove(p.norm);
                logCacheEvent(p.asker, p.raw, Door.AMBIENT, "stale-dropped");
            }
        }
        if (preparePeer(p)) {
            return;
        }
        if (aiActive()) {
            p.needsAi = true;
            if ("early".equals(cfg.delayedAiTiming)) {
                startDelayedAi(p, now);
            }
        }
    }

    /** answerIntent's text for a delayed reply, or null for stat / clarify intents. */
    private String delayedIntentText(IntentSet.IntentDef intent, String norm, PlayerRef asker) {
        String text;
        switch (intent.type()) {
            case "stat", "clarify" -> {
                return null;
            }
            case "wand" -> text = cfg.wandSelfRestoreEnabled ? intent.textPrimary() : intent.textFallback();
            case "spells" -> {
                text = stats.sealsBroken() >= 1 ? intent.textPrimary() : intent.textFallback();
                if (text == null) {
                    text = intent.text();
                }
            }
            case "seals" -> text = sealsText();
            case "retrieval" -> {
                text = intent.text();
                if (text != null) {
                    Retrieval.Result r = Retrieval.search(knowledge, norm, intent.hints());
                    if (Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin)) {
                        text = r.answerText();
                    }
                }
            }
            default -> text = intent.text();
        }
        if (text == null || text.isBlank()) {
            return null;
        }
        return substitute(text, asker);
    }

    private boolean preparePeer(Pending p) {
        if (!peerEnabled()) {
            return false;
        }
        PeerCandidates.Entry e = peerStore.byKey(p.key);
        if (e == null || !e.isApproved() || filter().serveReject(e.answer(), knownNames()) != null) {
            return false;
        }
        String text = cfg.peerServePrefix + e.answer();
        p.prepared = wrap(delayedChannel(p, text, null), text);
        p.source = "peer";
        p.peerEntry = e;
        return true;
    }

    /** Reserve and submit one AI call for a waiting question. False when the AI bucket said no. */
    private boolean startDelayedAi(Pending p, long now) {
        p.needsAi = false;
        if (aiExecutor == null || !aiActive() || !aiGate.tryReserve(p.asker.id(), now)) {
            logPeer("delayed-ai-refused", p.norm);
            return false;
        }
        List<KnowledgeBase.Chunk> excerpts = Retrieval.topK(knowledge, p.norm, cfg.aiTopChunks);
        // The asker's facts are read either way: private mode sends them,
        // public mode only uses their numbers for the publicSafe() check.
        List<String> askerFacts = safeAskerFacts(p.asker);
        AiPrompts.Prompt prompt = delayedPublic()
                ? AiPrompts.buildPublic(cfg.personaName, NON_ANSWER, p.raw, excerpts, safePublicFacts())
                : AiPrompts.build(cfg.personaName, NON_ANSWER, p.asker.name(),
                        p.raw, excerpts, askerFacts, safePublicFacts());
        AiProvider.Request request = new AiProvider.Request(prompt.system(), prompt.user(),
                cfg.aiModel, Math.max(1000, cfg.aiTimeoutSeconds * 1000));
        boolean personal = LearnPrivacy.isPersonalQuestion(TextNorm.tokenSet(p.norm));
        Set<String> factNumbers = LearnPrivacy.numbersInFacts(askerFacts);
        p.aiInFlight = true;
        try {
            aiExecutor.submit(() -> runDelayedAi(p, request, personal, factNumbers));
        } catch (RuntimeException e) {
            p.aiInFlight = false;
            aiGate.release();
            return false;
        }
        return true;
    }

    /** The delayed AI call, on the executor thread. Never throws. */
    private void runDelayedAi(Pending p, AiProvider.Request request, boolean personal,
                              Set<String> factNumbers) {
        long started = clock.getAsLong();
        String answer = null;
        long latency = 0;
        try {
            AiProvider.Completion completion = aiProvider.complete(request);
            latency = Math.max(0, clock.getAsLong() - started);
            if (completion != null) {
                String cleaned = AiPrompts.sanitize(completion.text(), cfg.aiMaxAnswerChars);
                if (cleaned != null && !AiPrompts.isDontKnow(cleaned, NON_ANSWER)) {
                    answer = cleaned;
                }
            }
        } catch (Exception e) {
            latency = Math.max(0, clock.getAsLong() - started);
            answer = null;
            noteAiFailure(e);
        } finally {
            aiGate.release();
        }
        logAi(p.asker, p.raw, Door.AMBIENT, answer != null, latency);
        synchronized (this) {
            p.aiInFlight = false;
            if (answer != null) {
                aiAnswered++;
                p.prepared = wrap(delayedChannel(p, answer, factNumbers), answer);
                p.source = "ai";
                if (aiCache != null && cfg.aiCacheEnabled && !personal
                        && !LearnPrivacy.answerQuotesAny(answer, factNumbers)) {
                    try {
                        aiCache.put(p.norm, answer, knowledge.version(), clock.getAsLong());
                    } catch (RuntimeException ignored) {
                        // Caching is a bonus.
                    }
                }
            }
            long n = clock.getAsLong();
            if (!p.done && n >= p.dueAt) {
                finishDelayed(p, n);
            }
        }
    }

    /** Step 3, watch: one chat line checked against every waiting question. */
    private void noteDelayedChat(PlayerRef speaker, String raw, String norm, Set<String> tokens,
                                 Door door, long now) {
        for (Pending p : new ArrayList<>(pendingAnswers.values())) {
            if (p.done) {
                continue;
            }
            if (speaker.id().equals(p.asker.id())) {
                if (door != Door.AMBIENT) {
                    // Asked the Guide directly: that door answers now.
                    if (p.key.equals(PeerFilter.topicKey(norm))
                            || PeerFilter.overlap(PeerFilter.meaningful(norm), p.qTerms) * 2 >= Math.max(1, p.qTerms.size())) {
                        endPending(p);
                        delayedCancelled++;
                        logPeer("delayed-cancelled", p.norm, "reason=asked-directly");
                    }
                } else if (isCancelPhrase(norm)) {
                    endPending(p);
                    delayedCancelled++;
                    logPeer("delayed-cancelled", p.norm, "reason=asker-never-mind");
                } else if (now <= p.dueAt && PeerFilter.isAskerConfirmation(raw, norm)) {
                    p.askerThanked = true; // somebody helped them already
                }
                continue;
            }
            if (door != Door.AMBIENT || now > p.dueAt || p.looseHeld) {
                continue;
            }
            if (cfg.delayedHoldOnAnyReply && looksLikeReply(raw, norm, tokens, p)) {
                p.looseHeld = true;
                logPeer("delayed-reply-seen", p.norm);
            }
        }
    }

    /**
     * Loose "another player is answering" test (hold-on-any-reply):
     * not a question, not a bare "yeah", at least two words, and on
     * topic - it shares a word with the question, talks to the asker
     * by name, or (only when one question is waiting) names a game word. Deliberately looser than the
     * peer answer gate: holding costs one Guide line, talking over a
     * player who is helping costs more.
     */
    private boolean looksLikeReply(String raw, String norm, Set<String> tokens, Pending p) {
        if (TextNorm.questionShaped(raw, norm) || tokens.size() < 2 || PeerFilter.isAgreement(raw, norm)) {
            return false;
        }
        if (PeerFilter.overlap(PeerFilter.meaningful(norm), p.qTerms) >= 1) {
            return true;
        }
        // A game word with no shared word only counts when this is the
        // one question waiting: with several waiting, it cannot tell
        // which one it answers (rig run Oct 25: a fox answer held a
        // potion question).
        if (pendingAnswers.size() == 1 && filter().namesGameTerm(tokens)) {
            return true;
        }
        return p.asker.name() != null
                && filter().namesPlayer(tokens, List.of(p.asker.name()));
    }

    private boolean isCancelPhrase(String norm) {
        String padded = " " + norm + " ";
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (tokens.size() > 8) {
            return false;
        }
        List<String> all = new ArrayList<>(DEFAULT_CANCEL_PHRASES);
        for (String extra : cfg.delayedCancelPhrases) {
            all.add(TextNorm.normalize(extra));
        }
        for (String ph : all) {
            if (!ph.isBlank() && padded.contains(" " + ph + " ")) {
                return true;
            }
        }
        return false;
    }

    /** Is the Guide holding off for this question right now? Returns the reason, or null. */
    private String holdReason(Pending p) {
        if (p.askerThanked) {
            return "asker-thanked";
        }
        PeerWindow w = peerWindows.get(p.key);
        if (w != null) {
            for (PeerCand c : w.cands) {
                if (!c.disputed) {
                    return "player-answered";
                }
            }
        }
        if (p.looseHeld) {
            // A captured candidate that was then disputed lets the Guide answer after all.
            if (w != null && !w.cands.isEmpty()) {
                return null;
            }
            return "player-replied";
        }
        return null;
    }

    /** Settle every waiting question whose delay ran out. */
    private void processDueDelayed(long now) {
        if (pendingAnswers.isEmpty()) {
            return;
        }
        for (Pending p : new ArrayList<>(pendingAnswers.values())) {
            if (!p.done && now >= p.dueAt) {
                finishDelayed(p, now);
            }
        }
    }

    /** Step 6: hold, ask the AI, or post. Runs under the engine lock. */
    private void finishDelayed(Pending p, long now) {
        if (p.done) {
            return;
        }
        String hold = holdReason(p);
        if (hold != null) {
            endPending(p);
            delayedHeld++;
            logPeer("delayed-held", p.norm, "reason=" + hold);
            return;
        }
        if (p.aiInFlight) {
            if (now < p.dueAt + Math.max(1000, cfg.aiTimeoutSeconds * 1000L) + 5000L) {
                return; // the call lands in seconds; runDelayedAi finishes it
            }
            p.aiInFlight = false; // overdue: answer without it
        }
        if (isPlayerOff(p.asker.id())) {
            endPending(p);
            logPeer("delayed-dropped", p.norm, "reason=guide-off");
            return;
        }
        if ("peer".equals(p.source) && p.peerEntry != null
                && filter().serveReject(p.peerEntry.answer(), knownNames()) != null) {
            p.prepared = null; // a filter added since research blocks it now
            p.source = null;
            p.peerEntry = null;
            p.needsAi = aiActive();
        }
        if (p.prepared == null && p.needsAi) {
            if (startDelayedAi(p, now) && p.aiInFlight) {
                return; // async: runDelayedAi posts when it lands
            }
            if (p.done) {
                return; // an inline executor already finished it
            }
        }
        List<Reply> out;
        String source;
        if (p.prepared != null) {
            out = p.prepared;
            source = p.source;
        } else if (cfg.delayedSayDontKnow) {
            out = List.of(new Reply(Channel.PRIVATE, NON_ANSWER));
            source = "dont-know";
        } else {
            endPending(p);
            logUnanswered(p.asker, p.raw, "AMBIENT", p.retrieval);
            logPeer("delayed-dropped", p.norm, "reason=no-answer");
            return;
        }
        List<Reply> served = deliver(p.asker, p.norm, false, now, out);
        if (served.isEmpty() && lastBlockVerdict == RateLimiter.Verdict.QUESTION_COOLDOWN
                && !out.isEmpty() && out.get(0).channel() == Channel.PUBLIC) {
            // The same answer went to public chat moments ago (the public
            // question cooldown): this asker still gets it, privately,
            // instead of a repeat line for everyone.
            List<Reply> mine = new ArrayList<>();
            for (Reply x : out) {
                mine.add(new Reply(Channel.PRIVATE, x.text()));
            }
            served = deliver(p.asker, p.norm, false, now, mine);
            if (!served.isEmpty()) {
                logPeer("delayed-repeat-private", p.norm);
            }
        }
        long wait = lastBlockRemainingMillis;
        lastBlockVerdict = null;
        lastBlockRemainingMillis = 0;
        lastDropWasGlobal = false;
        if (served.isEmpty()) {
            if (!p.retried && wait > 0 && wait <= 30_000L) {
                p.retried = true;
                p.dueAt = now + wait;
                return;
            }
            endPending(p);
            logPeer("delayed-dropped", p.norm, "reason=rate-limit");
            return;
        }
        endPending(p);
        delayedPosted++;
        if ("dont-know".equals(source)) {
            logUnanswered(p.asker, p.raw, "AMBIENT", p.retrieval);
        } else if ("peer".equals(source) && p.peerEntry != null) {
            peerStore.touch(p.peerEntry.hit(now));
            peerServed++;
            logPeer("served", p.peerEntry.question(), "id=" + p.peerEntry.id());
        } else if ("cache".equals(source) && aiCache != null) {
            aiCache.recordHit(p.norm, now);
            cacheServed++;
            logCacheEvent(p.asker, p.raw, Door.AMBIENT, "hit");
        }
        PeerWindow w = peerWindows.get(p.key);
        if (w != null) {
            w.guideAnswered = true;
        }
        logPeer("delayed-posted", p.norm, "source=" + source);
        DelayedSink sink = delayedSink;
        if (sink != null) {
            try {
                sink.deliver(p.asker.id(), served);
            } catch (RuntimeException ignored) {
                // A delivery failure never breaks chat.
            }
        }
    }

    /** True when delayed replies go to public chat (the 0.3.2 default). */
    private boolean delayedPublic() {
        return "public".equals(cfg.delayedReplyChannel);
    }

    /**
     * The channel for one delayed answer: PUBLIC in public mode unless
     * publicSafe() says the text may carry the asker's private details,
     * then PRIVATE (logged as delayed-kept-private, no text, no name).
     */
    private Channel delayedChannel(Pending p, String text, Set<String> askerNumbers) {
        if (!delayedPublic()) {
            return Channel.PRIVATE;
        }
        if (publicSafe(text, p.asker, askerNumbers)) {
            return Channel.PUBLIC;
        }
        delayedKeptPrivate++;
        logPeer("delayed-kept-private", p.norm, "reason=asker-detail");
        return Channel.PRIVATE;
    }

    /**
     * Privacy gate for a public delayed answer: false when the text
     * names the asker (whole word, any case) or quotes a number from
     * the asker's OWN fact sheet (balance, claim blocks, levels...).
     * The fact sheet is read here locally and never leaves the engine.
     * The number check runs on AI answers only ({@code askerNumbers},
     * read once when the AI call was set up; null = no number check):
     * website knowledge, curated, learned, cached and player answers
     * are fixed text that never saw this asker's facts, so a shared
     * number there is a coincidence, not a leak (and checking them
     * would push most knowledge answers private). Single-digit
     * integers are ignored (levels like 3 are everywhere in answers
     * and identify nobody); balances, claim blocks, coords and big
     * levels are checked. Public facts (seal progress, board leaders)
     * are not checked: those numbers are on the public boards already.
     * A false positive only costs a public line (the asker still gets
     * the answer privately).
     */
    boolean publicSafe(String text, PlayerRef asker, Set<String> askerNumbers) {
        if (text == null) {
            return true;
        }
        String name = asker.name();
        if (name != null && !name.isBlank()) {
            String bare = name.startsWith(".") ? name.substring(1) : name; // Floodgate prefix
            String low = text.toLowerCase(java.util.Locale.ROOT);
            for (String n : List.of(name, bare)) {
                if (n.length() >= 2 && java.util.regex.Pattern.compile(
                        "(?<![a-z0-9_])" + java.util.regex.Pattern.quote(n.toLowerCase(java.util.Locale.ROOT))
                                + "(?![a-z0-9_])").matcher(low).find()) {
                    return false;
                }
            }
        }
        if (text.contains(asker.id().toString())) {
            return false;
        }
        if (askerNumbers == null || askerNumbers.isEmpty()) {
            return true;
        }
        Set<String> mine = new java.util.HashSet<>(askerNumbers);
        mine.removeIf(x -> x.matches("\\d"));
        return !LearnPrivacy.answerQuotesAny(text, mine);
    }

    private long delayedKeptPrivate;

    public synchronized long delayedKeptPrivateCount() {
        return delayedKeptPrivate;
    }

    private void endPending(Pending p) {
        p.done = true;
        pendingAnswers.remove(p.key, p);
    }


    // ---------- clarify flow ----------

    /**
     * Returns the replies for a clarify reply, or null when the line is
     * not a clarify answer at all (a fresh question - caller continues
     * normal processing with the pending clarification dropped).
     */
    private List<Reply> handleClarifyReply(PlayerRef asker, String norm, Set<String> tokens,
                                           String rawMessage, long now) {
        EnumSet<Choice> choices = parseChoices(tokens);
        if (!choices.isEmpty()) {
            clarifyPending.remove(asker.id());
            String text;
            if (choices.size() == 1) {
                text = switch (choices.iterator().next()) {
                    case MCMMO -> orCantSee(stats.mcMMOSummary(asker.id()));
                    case JOBS -> orCantSee(stats.jobsSummary(asker.id()));
                    case PLAYTIME -> orCantSee(stats.playtimeSummary(asker.id()));
                    case MAGIC -> magicAnswer(asker);
                };
            } else {
                text = combinedLevels(asker, choices);
            }
            return deliver(asker, norm, true, now, wrap(Channel.PRIVATE, text));
        }
        if (TextNorm.questionShaped(rawMessage, norm)) {
            clarifyPending.remove(asker.id());
            return null; // a new question - normal processing takes it
        }
        // Neither a choice nor a new question: combined fallback, once.
        clarifyPending.remove(asker.id());
        return deliver(asker, norm, true, now,
                wrap(Channel.PRIVATE, combinedLevels(asker, EnumSet.allOf(Choice.class))));
    }

    private EnumSet<Choice> parseChoices(Set<String> tokens) {
        EnumSet<Choice> out = EnumSet.noneOf(Choice.class);
        if (tokens.contains("all") || tokens.contains("everything") || tokens.contains("both")) {
            return EnumSet.allOf(Choice.class);
        }
        if (tokens.contains("mcmmo") || tokens.contains("skills") || tokens.contains("skill")) {
            out.add(Choice.MCMMO);
        }
        if (tokens.contains("job") || tokens.contains("jobs")) {
            out.add(Choice.JOBS);
        }
        if (tokens.contains("time") || tokens.contains("played") || tokens.contains("playtime")
                || tokens.contains("hours")) {
            out.add(Choice.PLAYTIME);
        }
        if (tokens.contains("magic") || tokens.contains("mage")) {
            out.add(Choice.MAGIC);
        }
        return out;
    }

    private String combinedLevels(PlayerRef asker, EnumSet<Choice> which) {
        List<String> parts = new ArrayList<>();
        if (which.contains(Choice.MCMMO)) {
            parts.add(orCantSeeShort(stats.mcMMOSummary(asker.id()), "mcMMO: n/a"));
        }
        if (which.contains(Choice.JOBS)) {
            parts.add(orCantSeeShort(stats.jobsSummary(asker.id()), "Jobs: n/a"));
        }
        if (which.contains(Choice.PLAYTIME)) {
            parts.add(orCantSeeShort(stats.playtimeSummary(asker.id()), "Played: n/a"));
        }
        if (which.contains(Choice.MAGIC)) {
            parts.add(magicAnswer(asker));
        }
        return String.join(" - ", parts);
    }

    // ---------- intent answers ----------

    private List<Reply> answerIntent(PlayerRef asker, IntentSet.IntentDef intent,
                                     String norm, String skillToken, Door door, long now) {
        Channel channel = intent.isPrivate() ? Channel.PRIVATE : Channel.PUBLIC;
        String text;
        switch (intent.type()) {
            case "wand" -> {
                text = cfg.wandSelfRestoreEnabled ? intent.textPrimary() : intent.textFallback();
                channel = Channel.PRIVATE;
            }
            case "spells" -> {
                // Stage-aware like the Magic standing (0.1.1): while
                // the guild is sealed the lore answer (text-fallback),
                // from Seal 1 on the real how-to (text-primary).
                text = stats.sealsBroken() >= 1 ? intent.textPrimary() : intent.textFallback();
                if (text == null) {
                    text = intent.text();
                }
            }
            case "seals" -> text = sealsText();
            case "clarify" -> {
                clarifyPending.put(asker.id(), now + cfg.clarifyWindowSeconds * 1000L);
                return deliverKeyed(asker, questionKey(asker, norm, Channel.PRIVATE), false, now,
                        List.of(new Reply(Channel.PRIVATE, CLARIFY_QUESTION)));
            }
            case "stat" -> {
                text = statAnswer(intent.stat(), asker, skillToken);
                channel = Channel.PRIVATE;
            }
            case "retrieval" -> {
                text = intent.text();
                // Ambient door always serves the curated line; the
                // chunk-scoped retrieval runs only for asked questions.
                if (door != Door.AMBIENT && intent.text() != null) {
                    Retrieval.Result r = Retrieval.search(knowledge, norm, intent.hints());
                    if (Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin)) {
                        text = r.answerText();
                    }
                }
            }
            default -> text = substitute(intent.text(), asker);
        }
        if (text == null || text.isBlank()) {
            text = CANT_SEE;
        }
        List<Reply> replies = wrap(channel, substitute(text, asker));
        return deliverKeyed(asker, questionKey(asker, norm, channel), false, now, replies);
    }

    private String statAnswer(String stat, PlayerRef asker, String skillToken) {
        return switch (stat) {
            case "mcmmo-skill" -> orCantSee(skillToken == null
                    ? null : stats.mcMMOSkill(asker.id(), skillToken));
            case "jobs" -> orCantSee(stats.jobsSummary(asker.id()));
            case "playtime" -> orCantSee(stats.playtimeSummary(asker.id()));
            case "money" -> orCantSee(stats.moneySummary(asker.id()));
            case "claims" -> claimsAnswer(asker);
            case "magic" -> magicAnswer(asker);
            default -> CANT_SEE;
        };
    }

    /**
     * The claim-blocks answer (0.2.2, Mike's live-test call): the
     * asker's OWN live numbers plus the general rule plus the
     * /claimslist self-check, composed by ClaimText. When GP data is
     * unavailable the generic claim-amounts intent text is served
     * instead - the rule and the pointer, never a guessed number and
     * never a bare error (the one deliberate exception to the stats
     * layer's can't-see convention: the generic answer fully answers
     * the question's general half, and the numbers return when GP
     * does). Served privately like every stat (the caller forces the
     * PRIVATE channel for type stat).
     */
    private String claimsAnswer(PlayerRef asker) {
        long[] c = stats.claimBlocks(asker.id());
        if (c != null && c.length >= 4) {
            return ClaimText.combined(c[0], c[1], c[2], c[3]);
        }
        IntentSet.IntentDef generic = intents.byId("claim-amounts");
        if (generic != null && generic.text() != null && !generic.text().isBlank()) {
            return generic.text();
        }
        return CANT_SEE;
    }

    /**
     * The Magic answer, both seal states (SPEC 5 amendment): before
     * the first seal breaks, the lore line; from Seal 1 on, the
     * player's real Magic standing from their own stats source.
     */
    String magicAnswer(PlayerRef asker) {
        if (stats.sealsBroken() >= 1) {
            return orCantSee(stats.magicStanding(asker.id()));
        }
        return MAGIC_LORE;
    }

    private String sealsText() {
        int broken = stats.sealsBroken();
        int total = stats.sealsTotal();
        if (total > 0 && broken >= total) {
            return "All " + total + " seals are broken - the Adventurers Guild stands open!";
        }
        int left = Math.max(0, total - broken);
        return "Seal progress: " + broken + " of " + total + " seals broken, " + left
                + " to go. The five seals: the Ender Dragon, the Wither, the Elder Guardian, "
                + "the Warden, and a village defended in a raid.";
    }

    private String substitute(String text, PlayerRef asker) {
        if (text == null) {
            return null;
        }
        return text.replace("%seals_broken%", String.valueOf(stats.sealsBroken()))
                .replace("%seals_total%", String.valueOf(stats.sealsTotal()))
                .replace("%seals_left%", String.valueOf(Math.max(0, stats.sealsTotal() - stats.sealsBroken())))
                .replace("%player%", asker.name());
    }

    // ---------- helpers ----------

    private List<Reply> deliver(PlayerRef asker, String norm, boolean continuation,
                                long now, List<Reply> replies) {
        return deliverKeyed(asker, questionKey(asker, norm, replies.isEmpty()
                ? Channel.PRIVATE : replies.get(0).channel()), continuation, now, replies);
    }

    /**
     * Run the rate limiter for a composed answer; on OK the replies
     * count as served. On a global-limit refusal the caller may log
     * the question as unanswered (SPEC 3 guardrail 5: dropped
     * silently, log still records it).
     */
    private List<Reply> deliverKeyed(PlayerRef asker, String questionKey, boolean continuation,
                                     long now, List<Reply> replies) {
        lastDropWasGlobal = false;
        lastBlockVerdict = null;
        lastBlockRemainingMillis = 0;
        if (replies.isEmpty()) {
            return List.of();
        }
        RateLimiter.Verdict v = limiter.tryAcquire(asker.id(), questionKey,
                replies.size(), continuation, now);
        if (v != RateLimiter.Verdict.OK) {
            if (v == RateLimiter.Verdict.GLOBAL_LIMIT) {
                lastDropWasGlobal = true;
            }
            // 0.2.2: remember the block and its real remaining wait so
            // handle() can trade the silence for a cooldown notice on
            // the direct doors (serveOrNotice consumes these).
            lastBlockVerdict = v;
            lastBlockRemainingMillis = limiter.remainingMillis(asker.id(), questionKey,
                    replies.size(), continuation, now);
            return List.of();
        }
        answersServed += replies.size();
        return replies;
    }

    private boolean lastDropWasGlobal;
    private RateLimiter.Verdict lastBlockVerdict;
    private long lastBlockRemainingMillis;

    // ---------- answer acknowledgments (0.2.2) ----------

    /**
     * A composed answer the limiter just dropped becomes, on the
     * direct doors, a cooldown notice quoting the real remaining
     * wait (Mike's rule, Oct 18, 2026: a direct question is never
     * met with silence - "silence is never good with kids"). Any
     * other outcome passes through untouched: a served answer as-is,
     * an ambient drop as silence, a non-limiter empty as silence.
     */
    private List<Reply> serveOrNotice(PlayerRef asker, Door door, long now, List<Reply> out) {
        if (!out.isEmpty()) {
            return out;
        }
        RateLimiter.Verdict v = lastBlockVerdict;
        long remaining = lastBlockRemainingMillis;
        lastBlockVerdict = null;
        lastBlockRemainingMillis = 0;
        if (v == null) {
            return List.of();
        }
        return cooldownNotice(asker, door, now, remaining);
    }

    /**
     * The cooldown notice itself: one whispered line to the asker
     * only, the wait rounded up to a friendly number, computed from
     * the same clock and limiter state that produced the block.
     * Self-throttled per player (config, default 30 s) so a blocked
     * streak earns one notice, not a drumbeat. The notice never goes
     * through the limiter - a global-limit block must still be able
     * to send it - and a notice is never itself answered with one.
     */
    private List<Reply> cooldownNotice(PlayerRef asker, Door door, long now, long remainingMillis) {
        if (!cfg.noticesEnabled || door == Door.AMBIENT || remainingMillis <= 0) {
            return List.of();
        }
        Long last = lastNoticeAt.get(asker.id());
        if (last != null && now - last < cfg.noticeThrottleSeconds * 1000L) {
            return List.of();
        }
        lastNoticeAt.put(asker.id(), now);
        String line = cfg.noticeCooldownLine.replace("%seconds%",
                String.valueOf(friendlySeconds(remainingMillis)));
        return List.of(new Reply(Channel.PRIVATE, substitute(line, asker)));
    }

    /**
     * The AI thinking line (0.2.2): returned synchronously the
     * moment a fallback call engages, so the up-to-8-second wait
     * for the async answer is not silence either. Once per
     * engagement by construction (the engagement itself is what
     * returns it), and the AI bucket's own 60 s per-player cooldown
     * keeps engagements - and therefore thinking lines - unspammable.
     */
    private List<Reply> thinkingNotice(PlayerRef asker) {
        if (!cfg.noticesEnabled) {
            return List.of();
        }
        return List.of(new Reply(Channel.PRIVATE, substitute(cfg.noticeThinkingLine, asker)));
    }

    /** Round a wait UP to a friendly number of seconds: whole multiples of 5, minimum 5. */
    static int friendlySeconds(long remainingMillis) {
        long secs = (remainingMillis + 999) / 1000;
        long rounded = ((secs + 4) / 5) * 5;
        return (int) Math.max(5, rounded);
    }

    private boolean limiterVerdictWasGlobal(PlayerRef asker, String norm, long now) {
        boolean g = lastDropWasGlobal;
        lastDropWasGlobal = false;
        return g;
    }

    /** Per-question cooldown key: global for public, asker-scoped for private. */
    private String questionKey(PlayerRef asker, String norm, Channel channel) {
        return channel == Channel.PUBLIC ? norm : asker.id() + "|" + norm;
    }

    private void logUnanswered(PlayerRef asker, String rawMessage, String door, Retrieval.Result r) {
        unansweredCount++;
        if (unansweredLog != null && cfg.unansweredEnabled) {
            try {
                unansweredLog.append(new UnansweredLog.Entry(
                        Instant.ofEpochMilli(clock.getAsLong()), asker.name(),
                        asker.id().toString(), door, rawMessage,
                        r == null || r.best() == null ? null : r.best().key(),
                        r == null ? 0 : r.bestScore()));
            } catch (RuntimeException ignored) {
                // The Guide never breaks chat because its notebook failed.
            }
        }
    }

    private boolean mentionsOtherPlayer(String norm, Set<String> tokens,
                                        PlayerRef asker, Collection<String> onlineNames) {
        if (onlineNames == null) {
            return false;
        }
        for (String name : onlineNames) {
            if (name == null || name.equalsIgnoreCase(asker.name())) {
                continue;
            }
            String n = TextNorm.normalize(name);
            if (n.isBlank()) {
                continue;
            }
            if (n.indexOf(' ') < 0) {
                // Also catch the possessive form ("testkite's" ->
                // token "testkites" once the apostrophe drops).
                if (tokens.contains(n) || tokens.contains(n + "s")) {
                    return true;
                }
            } else if ((" " + norm + " ").contains(" " + n + " ")) {
                return true;
            }
        }
        return false;
    }

    private String findSkillToken(String norm) {
        for (String t : norm.split(" ")) {
            if (SKILLS.contains(t)) {
                return t;
            }
            String stem = TextNorm.stem(t);
            for (String skill : SKILLS) {
                if (TextNorm.stem(skill).equals(stem)) {
                    return skill;
                }
            }
        }
        return null;
    }

    private static String orCantSee(String s) {
        return s == null || s.isBlank() ? CANT_SEE : s;
    }

    private static String orCantSeeShort(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    /**
     * Wrap one answer into at most 2 chat lines of at most 200 visible
     * characters (SPEC guardrail 7). Overflow past line 2 is cut at a
     * word boundary and closed with the website pointer.
     */
    public static List<Reply> wrap(Channel channel, String text) {
        List<Reply> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String flat = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (flat.length() <= 200) {
            out.add(new Reply(channel, flat));
            return out;
        }
        int split = splitPoint(flat, 200);
        String line1 = flat.substring(0, split).trim();
        String rest = flat.substring(split).trim();
        String line2;
        if (rest.length() <= 200) {
            line2 = rest;
        } else {
            int cut = splitPoint(rest, 165);
            line2 = rest.substring(0, cut).trim() + "... More: the website's Ask page.";
        }
        out.add(new Reply(channel, line1));
        out.add(new Reply(channel, line2));
        return out;
    }

    /** Best split index at or before limit: sentence end, else word gap. */
    private static int splitPoint(String s, int limit) {
        int upto = Math.min(limit, s.length());
        int sentence = -1;
        for (int i = 0; i < upto; i++) {
            char c = s.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < s.length() && s.charAt(i + 1) == ' ') {
                sentence = i + 1;
            }
        }
        if (sentence > 40) {
            return sentence;
        }
        int space = s.lastIndexOf(' ', upto);
        return space > 0 ? space : upto;
    }
}
