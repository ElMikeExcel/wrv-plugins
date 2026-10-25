package com.winkreyes.excelguide.core;

/**
 * One AI completion call (0.2.0). The provider is a pure config
 * choice - endpoint, key, and model live in config.yml, never in
 * code - so Groq today can become Gemini, OpenRouter, or Cloudflare
 * tomorrow without a rebuild (all four speak the same chat shape;
 * see AI-PROVIDER-RESEARCH.md). Implementations: the HTTP client in
 * the Bukkit glue, and programmable mocks in the test harnesses.
 * The call BLOCKS its caller for up to the request timeout; the
 * engine only ever invokes it on its AI executor thread, never on a
 * chat or command thread.
 */
public interface AiProvider {

    /** One grounded request: the fenced system prompt + the assembled user prompt. */
    record Request(String systemPrompt, String userPrompt, String model, int timeoutMillis) {}

    /** A finished call: the model's raw text and the wall-clock latency. */
    record Completion(String text, long latencyMillis) {}

    /** A failed call, classified so the engine can log the reason shape. */
    final class AiException extends Exception {
        public enum Kind { HTTP, RATE_LIMIT, TIMEOUT, IO, BAD_RESPONSE }

        private final Kind kind;
        private final int status;

        public AiException(Kind kind, int status, String message) {
            super(message);
            this.kind = kind;
            this.status = status;
        }

        public Kind kind() {
            return kind;
        }

        /** HTTP status when kind is HTTP or RATE_LIMIT, else 0. Never carries the key. */
        public int status() {
            return status;
        }
    }

    Completion complete(Request request) throws AiException;
}
