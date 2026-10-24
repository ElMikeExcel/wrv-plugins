package com.winkreyes.excelguide.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Config parsing and the knowledge pipeline's provenance: the shipped
 * knowledge.yml must be exactly the website's ai-knowledge.json run
 * through site-ai/make-guide-knowledge.py (same chunk count, and its
 * stamped SHA-256 must equal the source file's real hash).
 */
public final class ConfigKnowledgeTest {

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();

        // Shipped defaults are the spec's values.
        GuideConfig cfg = TestSupport.realConfig();
        t.eq("persona default", "Valley Guide", cfg.personaName);
        t.check("all doors on", cfg.doorCommand && cfg.doorSummon && cfg.doorAmbient);
        t.eq("intent threshold", 0.72, cfg.intentMinScore);
        t.eq("retrieval threshold", 8.0, cfg.retrievalMinScore);
        t.eq("retrieval margin", 1.5, cfg.retrievalMargin);
        t.eq("per-player cooldown", 20, cfg.perPlayerSeconds);
        t.eq("per-question cooldown", 60, cfg.perQuestionSeconds);
        t.eq("global per minute", 6, cfg.globalPerMinute);
        t.eq("clarify window", 60, cfg.clarifyWindowSeconds);
        t.check("wand flag off at launch", !cfg.wandSelfRestoreEnabled);
        t.check("unanswered log on", cfg.unansweredEnabled);

        // AI fallback (0.2.0): ships off, key EMPTY (a real key lives
        // only in the live data-folder config - a key in the shipped
        // file is a leak and fails this harness), provider = config.
        t.check("ai ships disabled", !cfg.aiEnabled);
        t.eq("ai key ships empty", "", cfg.aiApiKey);
        t.check("ai not configured as shipped", !cfg.aiConfigured());
        t.eq("ai provider default", "Groq", cfg.aiProviderName);
        t.eq("ai endpoint default", "https://api.groq.com/openai/v1/chat/completions", cfg.aiEndpoint);
        t.eq("ai model default", "llama-3.3-70b-versatile", cfg.aiModel);
        t.eq("ai timeout", 8, cfg.aiTimeoutSeconds);
        t.eq("ai top chunks", 5, cfg.aiTopChunks);
        t.eq("ai per-player cooldown", 60, cfg.aiPerPlayerCooldownSeconds);
        t.eq("ai daily budget", 50, cfg.aiDailyBudget);
        t.eq("ai answer cap", 380, cfg.aiMaxAnswerChars);
        GuideConfig aiOn = GuideConfig.fromYaml(
                "ai:\n  enabled: true\n  api-key: \"test-key-not-real\"\n");
        t.check("ai configured once enabled + keyed", aiOn.aiConfigured());
        t.eq("ai custom key parsed", "test-key-not-real", aiOn.aiApiKey);
        GuideConfig aiNoKey = GuideConfig.fromYaml("ai:\n  enabled: true\n");
        t.check("enabled without a key stays off", !aiNoKey.aiConfigured());

        // Null config -> same defaults; partial config overrides cleanly.
        GuideConfig dflt = GuideConfig.fromYaml(null);
        t.eq("null config defaults persona", "Valley Guide", dflt.personaName);
        GuideConfig custom = GuideConfig.fromYaml("persona-name: \"Test Guide\"\nthresholds:\n  intent-min-score: 0.5\n");
        t.eq("custom persona", "Test Guide", custom.personaName);
        t.eq("custom threshold", 0.5, custom.intentMinScore);
        t.eq("untouched default kept", 6, custom.globalPerMinute);

        // Knowledge provenance: stamped hash == real source hash.
        KnowledgeBase kb = TestSupport.realKnowledge();
        Path siteJson = TestSupport.projDir().resolve("../../../site-ai/ai-knowledge.json")
                .normalize();
        t.check("site source found", Files.exists(siteJson));
        byte[] raw = Files.readAllBytes(siteJson);
        String realSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        t.eq("knowledge stamped with the source hash", realSha, kb.sourceSha256());
        t.eq("version is hash prefix", realSha.substring(0, 8), kb.version());
        // Site chunk count, parsed with SnakeYAML (JSON is a YAML subset).
        Object parsed = new org.yaml.snakeyaml.Yaml().load(new String(raw, StandardCharsets.UTF_8));
        int siteCount = ((java.util.List<?>) parsed).size();
        t.eq("chunk count matches the website", siteCount, kb.size());
        t.check("chunks carry text", kb.chunks().stream().allMatch(c -> !c.text().isBlank()));
        t.check("chunk keys unique", kb.chunks().stream().map(KnowledgeBase.Chunk::key).distinct().count() == kb.size());

        // Malformed input fails loudly (the plugin then refuses to enable).
        boolean threw = false;
        try {
            KnowledgeBase.fromYaml("chunks: [unclosed");
        } catch (RuntimeException e) {
            threw = true;
        }
        t.check("malformed knowledge throws", threw);
        threw = false;
        try {
            IntentSet.fromYaml("intents: 42");
        } catch (RuntimeException e) {
            threw = true;
        }
        t.check("malformed intents throw", threw);

        t.finish("ConfigKnowledgeTest");
    }
}
