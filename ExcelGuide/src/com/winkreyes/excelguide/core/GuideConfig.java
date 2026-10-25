package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * ExcelGuide's config (config.yml in the plugin data folder), parsed
 * into plain fields with the SPEC section 9 defaults. Parsing is
 * forgiving in the Excel-family way: any missing or mistyped value
 * falls back to its default rather than failing the load.
 */
public final class GuideConfig {

    public boolean enabled = true;
    public String personaName = "Valley Guide";

    public boolean doorCommand = true;
    public boolean doorSummon = true;
    public boolean doorAmbient = true;

    public double intentMinScore = 0.72;
    public double retrievalMinScore = 8;
    public double retrievalMargin = 1.5;

    public int perPlayerSeconds = 20;
    public int perQuestionSeconds = 60;
    public int globalPerMinute = 6;
    public int clarifyWindowSeconds = 60;
    public int combatQuietSeconds = 10;

    public String knowledgeSource = "file";
    public String knowledgeFile = "knowledge.yml";

    public boolean wandSelfRestoreEnabled = false;

    public boolean unansweredEnabled = true;
    public String unansweredFile = "unanswered.log";
    public int unansweredMaxEntries = 2000;

    // AI fallback (0.2.0). Ships OFF: the fallback engages only when
    // enabled is true AND an api-key is present. The key lives ONLY
    // in the live data-folder config - never in the jar, the source
    // deposits, git, or Drive. Endpoint/model are plain config so the
    // provider can be swapped without a rebuild.
    public boolean aiEnabled = false;
    public String aiProviderName = "Groq";
    public String aiEndpoint = "https://api.groq.com/openai/v1/chat/completions";
    public String aiApiKey = "";
    public String aiModel = "llama-3.3-70b-versatile";
    public int aiTimeoutSeconds = 8;
    public int aiTopChunks = 5;
    public int aiPerPlayerCooldownSeconds = 60;
    public int aiDailyBudget = 50;
    public int aiMaxAnswerChars = 380;
    public String aiLogFile = "ai.log";
    public int aiLogMaxEntries = 2000;
    /** 0.3.1: privately point a plain-chat asker at /ask when the AI could try. Off by default. */
    public boolean aiAmbientHint = false;
    public int aiAmbientHintHours = 24;

    // Answer acknowledgments (0.2.2, Mike's rule Oct 18, 2026: "a
    // direct question must never be met with silence - silence is
    // never good with kids"). On the DIRECT doors only (/ask and the
    // "Guide," summon), a question blocked by a cooldown gets one
    // whispered line quoting the real wait, and a question handed to
    // the AI fallback gets an immediate "thinking" line while the
    // answer is on its way. Ambient chat-watch stays silent when
    // blocked, exactly as before. Defaults are ON with the persona
    // lines below; a MALFORMED notices block (wrong types, a
    // cooldown line without %seconds%) turns notices OFF - fail-soft,
    // answers are never affected. A missing block is not malformed:
    // the defaults apply.
    public boolean noticesEnabled = true;
    public String noticeCooldownLine = "One moment, I can answer again in about %seconds% seconds.";
    public String noticeThinkingLine = "Hmm, let me think about that one…";
    public int noticeThrottleSeconds = 30;

    // Learn-cache (0.2.1): successful AI answers are cached against
    // their normalized question and re-served with no AI call. The
    // cache is part of the AI feature - it only serves while the AI
    // fallback itself is active; flipping the AI kill switch stops
    // cache serves too. Promoted answers (learned.yml) are separate:
    // they are deterministic and serve regardless.
    public boolean aiCacheEnabled = true;
    public String aiCacheFile = "ai-cache.yml";
    public int aiCacheMaxEntries = 500;

    // Promoted answers file (0.2.1): human-reviewed AI answers in
    // the plugin data folder; loaded fail-soft on every engine build.
    public String learnedFile = "learned.yml";

    // Peer learning (0.2.3, Mike's idea + Variant A pick, Oct 18,
    // 2026): when a question goes unanswered, the Guide listens for
    // a few minutes - another player's answer in chat is captured,
    // guardrail-filtered (PeerGuard), and learned with that
    // player's name on it. A disagreement marks the capture
    // contested and the Guide settles it down the evidence ladder
    // (knowledge, corroboration, the asker's word, the AI) before
    // anything is learned. Default ON per Mike's pick; enabled:
    // false restores 0.2.2 behavior exactly - no windows, no
    // captures, no peer serves. Parsing is forgiving like the rest
    // of this file; the numeric knobs are clamped to sane ranges.
    public boolean peerEnabled = true;
    public String peerFile = "peer-answers.yml";
    public String peerLogFile = "peer.log";
    public int peerLogMaxEntries = 2000;
    public int peerCaptureWindowMinutes = 5;
    public int peerMaxAnswerChars = 300;
    public int peerMaxEntries = 200;

    // Peer learning 0.3.0 (Mike, Oct 24, 2026: "Can we improve this
    // system" - 0.2.3 learned 65 chat lines, most of them chatter).
    // Every player-taught answer now waits in a review queue
    // (peer-review.yml, /guide review) and is never served until Mike
    // approves it. All keys are optional: the live config.yml is not
    // replaced by a jar swap, so every default below must be safe on
    // its own. capture-window-minutes (above) is now the CONFIRM
    // window: how long agreements and the asker's thanks still count.
    public String peerReviewFile = "peer-review.yml";
    public int peerAnswerWindowSeconds = 120;      // answers must arrive this soon after the question
    public int peerMinAnswerChars = 20;
    public int peerMinAnswerWords = 4;
    public int peerMinAgreements = 2;              // distinct other players, OR the asker's thanks
    public int peerCandidateExpireDays = 14;       // review entries nobody asks again (0 = never)
    public int peerApprovedExpireDays = 0;         // approved answers nobody asks/uses (0 = never)
    public boolean peerAutoApprove = false;        // OFF: Mike approves every answer by hand
    public int peerAutoApproveMinAgreements = 2;   // auto-approve also needs the asker's thanks
    public boolean peerNotifyOps = true;           // tell online ops when something is queued
    public String peerServePrefix = "A player said: ";
    public boolean peerTipEnabled = true;          // first-use tip on "A player said" answers, until OK
    public String peerTipFile = "peer-tip.yml";
    public List<String> peerGameTerms = List.of();       // ADDED to the built-in game words
    public List<String> peerSpoilerTerms = List.of();    // ADDED to the built-in spoiler phrases
    public List<String> peerBlockedWords = List.of();    // ADDED to the built-in profanity list
    public List<String> peerBlockedNames = List.of();    // names never stored or served (offline too)
    public List<String> peerSocialPhrases = List.of();   // ADDED: social questions that open no window
    public List<String> peerChatterPhrases = List.of();  // ADDED: chatter that is never an answer

    // Delayed plain-chat answers (0.3.2, Mike's design Oct 25, 2026:
    // "a player asks a question in chat. It reads it, does the research
    // and then watches to see if another player answers"). A real game
    // question in plain chat that no intent answered is researched
    // silently (learned answers, knowledge, AI cache, approved player
    // answers; the AI last), then the Guide waits delay-seconds. If
    // another player replies in that window it holds off and lets the
    // peer-learning layer bank the player's answer; if nobody does,
    // the Guide answers. Every key is
    // optional: the live config is never replaced by a jar swap, so
    // the defaults below must be safe on their own.
    public boolean delayedEnabled = true;
    public int delayedDelaySeconds = 45;            // 15..300
    public String delayedAiTiming = "late";         // late = AI only if nobody answered; early = AI at once
    public boolean delayedSayDontKnow = true;       // nothing found -> the standard non-answer, privately
    // 0.3.2 (Mike, Oct 25, 2026 5:18 PM CDT): replies are PUBLIC by
    // default. Public replies never carry the asker's private details:
    // the AI is asked with no asker facts and no asker name, and any
    // answer that names the asker or quotes one of their own numbers
    // goes to the asker privately instead. See GuideEngine step 6.
    public String delayedReplyChannel = "public";   // public | private (typo -> private)
    public int delayedPerPlayerCooldownSeconds = 90; // between delayed answers to the same player
    public int delayedMaxPending = 4;               // questions waiting at once, server-wide
    public boolean delayedHoldOnAnyReply = true;    // any on-topic reply from another player holds the Guide
    public List<String> delayedCancelPhrases = List.of(); // ADDED to the built-in "nvm", "found it"...

    // Peer-learning 0.3.2 additions.
    public boolean peerOthersThanksCount = true;    // a third player's "thanks" counts as one agreement
    // 0.3.2 (Mike, Oct 25, 2026 5:18 PM CDT): ON by default - "since it's
    // a thanked answer it's probably right". Every filter and the dispute
    // block still apply; false = Mike reviews each one in /guide review.
    public boolean peerAutoApproveThanked = true;   // the asker's thanks alone approves (no review)

    /** True when the AI fallback may engage: switch on AND a key present. */
    public boolean aiConfigured() {
        return aiEnabled && aiApiKey != null && !aiApiKey.isBlank()
                && aiEndpoint != null && !aiEndpoint.isBlank()
                && aiModel != null && !aiModel.isBlank();
    }

    /** Parse config.yml text; null text yields all defaults. */
    @SuppressWarnings("unchecked")
    public static GuideConfig fromYaml(String yamlText) {
        GuideConfig c = new GuideConfig();
        if (yamlText == null) {
            return c;
        }
        Object root = new Yaml().load(yamlText);
        if (!(root instanceof Map)) {
            return c;
        }
        Map<String, Object> m = (Map<String, Object>) root;
        c.enabled = bool(m.get("enabled"), c.enabled);
        if (m.get("persona-name") != null) {
            c.personaName = String.valueOf(m.get("persona-name"));
        }
        Map<String, Object> doors = sub(m, "doors");
        c.doorCommand = bool(doors.get("command"), c.doorCommand);
        c.doorSummon = bool(doors.get("summon"), c.doorSummon);
        c.doorAmbient = bool(doors.get("ambient"), c.doorAmbient);
        Map<String, Object> th = sub(m, "thresholds");
        c.intentMinScore = num(th.get("intent-min-score"), c.intentMinScore);
        c.retrievalMinScore = num(th.get("retrieval-min-score"), c.retrievalMinScore);
        c.retrievalMargin = num(th.get("retrieval-margin"), c.retrievalMargin);
        Map<String, Object> cd = sub(m, "cooldowns");
        c.perPlayerSeconds = (int) num(cd.get("per-player-seconds"), c.perPlayerSeconds);
        c.perQuestionSeconds = (int) num(cd.get("per-question-seconds"), c.perQuestionSeconds);
        c.globalPerMinute = (int) num(cd.get("global-per-minute"), c.globalPerMinute);
        c.clarifyWindowSeconds = (int) num(cd.get("clarify-window-seconds"), c.clarifyWindowSeconds);
        c.combatQuietSeconds = (int) num(cd.get("combat-quiet-seconds"), c.combatQuietSeconds);
        Map<String, Object> kn = sub(m, "knowledge");
        if (kn.get("source") != null) {
            c.knowledgeSource = String.valueOf(kn.get("source"));
        }
        if (kn.get("file") != null) {
            c.knowledgeFile = String.valueOf(kn.get("file"));
        }
        c.wandSelfRestoreEnabled = bool(m.get("wand-self-restore-enabled"), c.wandSelfRestoreEnabled);
        Map<String, Object> ul = sub(m, "unanswered-log");
        c.unansweredEnabled = bool(ul.get("enabled"), c.unansweredEnabled);
        if (ul.get("file") != null) {
            c.unansweredFile = String.valueOf(ul.get("file"));
        }
        c.unansweredMaxEntries = (int) num(ul.get("max-entries"), c.unansweredMaxEntries);
        Map<String, Object> ai = sub(m, "ai");
        c.aiEnabled = bool(ai.get("enabled"), c.aiEnabled);
        if (ai.get("provider-name") != null) {
            c.aiProviderName = String.valueOf(ai.get("provider-name"));
        }
        if (ai.get("endpoint") != null) {
            c.aiEndpoint = String.valueOf(ai.get("endpoint"));
        }
        if (ai.get("api-key") != null) {
            c.aiApiKey = String.valueOf(ai.get("api-key"));
        }
        if (ai.get("model") != null) {
            c.aiModel = String.valueOf(ai.get("model"));
        }
        c.aiTimeoutSeconds = (int) num(ai.get("timeout-seconds"), c.aiTimeoutSeconds);
        c.aiTopChunks = (int) num(ai.get("top-chunks"), c.aiTopChunks);
        c.aiPerPlayerCooldownSeconds = (int) num(ai.get("per-player-cooldown-seconds"),
                c.aiPerPlayerCooldownSeconds);
        c.aiDailyBudget = (int) num(ai.get("daily-budget"), c.aiDailyBudget);
        c.aiMaxAnswerChars = (int) num(ai.get("max-answer-chars"), c.aiMaxAnswerChars);
        if (ai.get("log-file") != null) {
            c.aiLogFile = String.valueOf(ai.get("log-file"));
        }
        c.aiLogMaxEntries = (int) num(ai.get("log-max-entries"), c.aiLogMaxEntries);
        c.aiAmbientHint = bool(ai.get("ambient-hint"), c.aiAmbientHint);
        c.aiAmbientHintHours = Math.max(1, (int) num(ai.get("ambient-hint-hours"), c.aiAmbientHintHours));
        Map<String, Object> cache = sub(ai, "cache");
        c.aiCacheEnabled = bool(cache.get("enabled"), c.aiCacheEnabled);
        if (cache.get("file") != null) {
            c.aiCacheFile = String.valueOf(cache.get("file"));
        }
        c.aiCacheMaxEntries = (int) num(cache.get("max-entries"), c.aiCacheMaxEntries);
        Map<String, Object> learned = sub(m, "learned");
        if (learned.get("file") != null) {
            c.learnedFile = String.valueOf(learned.get("file"));
        }
        Map<String, Object> peer = sub(m, "peer-learning");
        c.peerEnabled = bool(peer.get("enabled"), c.peerEnabled);
        if (peer.get("file") != null) {
            c.peerFile = String.valueOf(peer.get("file"));
        }
        if (peer.get("log-file") != null) {
            c.peerLogFile = String.valueOf(peer.get("log-file"));
        }
        c.peerLogMaxEntries = (int) num(peer.get("log-max-entries"), c.peerLogMaxEntries);
        c.peerCaptureWindowMinutes = clamp(
                (int) num(peer.get("capture-window-minutes"), c.peerCaptureWindowMinutes), 1, 30);
        c.peerMaxAnswerChars = clamp(
                (int) num(peer.get("max-answer-chars"), c.peerMaxAnswerChars), 60, 1000);
        c.peerMaxEntries = clamp(
                (int) num(peer.get("max-entries"), c.peerMaxEntries), 10, 2000);
        if (peer.get("review-file") != null) {
            c.peerReviewFile = String.valueOf(peer.get("review-file"));
        }
        c.peerAnswerWindowSeconds = clamp(
                (int) num(peer.get("answer-window-seconds"), c.peerAnswerWindowSeconds), 15, 600);
        c.peerAnswerWindowSeconds = Math.min(c.peerAnswerWindowSeconds,
                c.peerCaptureWindowMinutes * 60);
        c.peerMinAnswerChars = clamp(
                (int) num(peer.get("min-answer-chars"), c.peerMinAnswerChars), 10, 200);
        c.peerMinAnswerWords = clamp(
                (int) num(peer.get("min-answer-words"), c.peerMinAnswerWords), 3, 40);
        c.peerMinAgreements = clamp(
                (int) num(peer.get("min-agreements"), c.peerMinAgreements), 2, 10);
        c.peerCandidateExpireDays = clamp(
                (int) num(peer.get("candidate-expire-days"), c.peerCandidateExpireDays), 0, 365);
        c.peerApprovedExpireDays = clamp(
                (int) num(peer.get("approved-expire-days"), c.peerApprovedExpireDays), 0, 3650);
        c.peerAutoApprove = bool(peer.get("auto-approve"), c.peerAutoApprove);
        c.peerAutoApproveMinAgreements = clamp((int) num(peer.get("auto-approve-min-agreements"),
                c.peerAutoApproveMinAgreements), 2, 20);
        c.peerNotifyOps = bool(peer.get("notify-ops"), c.peerNotifyOps);
        c.peerTipEnabled = bool(peer.get("first-use-tip"), c.peerTipEnabled);
        if (peer.get("tip-file") != null) {
            c.peerTipFile = String.valueOf(peer.get("tip-file"));
        }
        if (peer.get("serve-prefix") instanceof String sp && !sp.isBlank()) {
            c.peerServePrefix = sp.endsWith(" ") ? sp : sp + " ";
        }
        c.peerGameTerms = strList(peer.get("game-terms"));
        c.peerSpoilerTerms = strList(peer.get("spoiler-terms"));
        c.peerBlockedWords = strList(peer.get("blocked-words"));
        c.peerBlockedNames = strList(peer.get("blocked-names"));
        c.peerSocialPhrases = strList(peer.get("social-phrases"));
        c.peerChatterPhrases = strList(peer.get("chatter-phrases"));
        c.peerOthersThanksCount = bool(peer.get("others-thanks-count"), c.peerOthersThanksCount);
        c.peerAutoApproveThanked = bool(peer.get("auto-approve-thanked"), c.peerAutoApproveThanked);
        Map<String, Object> da = sub(m, "delayed-answers");
        c.delayedEnabled = bool(da.get("enabled"), c.delayedEnabled);
        c.delayedDelaySeconds = clamp((int) num(da.get("delay-seconds"), c.delayedDelaySeconds), 15, 300);
        if (da.get("ai-timing") instanceof String at
                && (at.equalsIgnoreCase("early") || at.equalsIgnoreCase("late"))) {
            c.delayedAiTiming = at.toLowerCase(java.util.Locale.ROOT);
        }
        c.delayedSayDontKnow = bool(da.get("say-dont-know"), c.delayedSayDontKnow);
        if (da.get("reply-channel") instanceof String rc) {
            // A typo falls back to private, the safer side.
            c.delayedReplyChannel = rc.equalsIgnoreCase("public") ? "public" : "private";
        }
        c.delayedPerPlayerCooldownSeconds = clamp((int) num(da.get("per-player-cooldown-seconds"),
                c.delayedPerPlayerCooldownSeconds), 0, 3600);
        c.delayedMaxPending = clamp((int) num(da.get("max-pending"), c.delayedMaxPending), 1, 20);
        c.delayedHoldOnAnyReply = bool(da.get("hold-on-any-reply"), c.delayedHoldOnAnyReply);
        c.delayedCancelPhrases = strList(da.get("cancel-phrases"));
        // Notices (0.2.2): parsed strictly - any present-but-mistyped
        // value marks the block malformed and notices go OFF
        // (fail-soft; see the field comment). Absent keys keep their
        // defaults; an absent block keeps notices ON.
        Object noticesRaw = m.get("notices");
        if (noticesRaw != null) {
            if (!(noticesRaw instanceof Map)) {
                c.noticesEnabled = false;
            } else {
                Map<String, Object> nz = (Map<String, Object>) noticesRaw;
                boolean bad = false;
                if (nz.get("enabled") != null) {
                    if (nz.get("enabled") instanceof Boolean b) {
                        c.noticesEnabled = b;
                    } else {
                        bad = true;
                    }
                }
                if (nz.get("cooldown-line") != null) {
                    if (nz.get("cooldown-line") instanceof String s && s.contains("%seconds%")) {
                        c.noticeCooldownLine = s;
                    } else {
                        bad = true;
                    }
                }
                if (nz.get("thinking-line") != null) {
                    if (nz.get("thinking-line") instanceof String s && !s.isBlank()) {
                        c.noticeThinkingLine = s;
                    } else {
                        bad = true;
                    }
                }
                if (nz.get("throttle-seconds") != null) {
                    if (nz.get("throttle-seconds") instanceof Number n) {
                        c.noticeThrottleSeconds = Math.max(0, n.intValue());
                    } else {
                        bad = true;
                    }
                }
                if (bad) {
                    c.noticesEnabled = false;
                }
            }
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sub(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    /** A YAML list of strings; anything else (absent, a scalar, a map) is an empty list. */
    private static List<String> strList(Object o) {
        if (!(o instanceof List<?> l)) {
            return List.of();
        }
        List<String> out = new java.util.ArrayList<>();
        for (Object x : l) {
            if (x != null && !String.valueOf(x).isBlank()) {
                out.add(String.valueOf(x).trim());
            }
        }
        return List.copyOf(out);
    }

    private static boolean bool(Object o, boolean dflt) {
        return o instanceof Boolean b ? b : dflt;
    }

    private static double num(Object o, double dflt) {
        return o instanceof Number n ? n.doubleValue() : dflt;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
