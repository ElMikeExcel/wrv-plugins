package com.winkreyes.excelguide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.winkreyes.excelguide.core.AiProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * The AI fallback's HTTP client (0.2.0): one OpenAI-shaped chat
 * completion call. Groq, OpenRouter, Cloudflare Workers AI, and
 * Gemini's OpenAI-compatible endpoint all accept this shape, so the
 * provider is pure config (endpoint + key + model in config.yml).
 * The key is read from the config at build time and is never
 * logged, never put in an exception message, and never echoed.
 *
 * Blocking by design - the engine only calls this on its async
 * executor thread, with the request's hard timeout applied to the
 * HTTP call itself.
 */
public final class HttpAiProvider implements AiProvider {

    private final String endpoint;
    private final String apiKey;
    private final HttpClient client;

    public HttpAiProvider(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Completion complete(Request request) throws AiException {
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", request.systemPrompt());
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", request.userPrompt());
        JsonArray messages = new JsonArray();
        messages.add(system);
        messages.add(user);
        JsonObject body = new JsonObject();
        body.addProperty("model", request.model());
        body.add("messages", messages);
        body.addProperty("temperature", 0.2);
        body.addProperty("max_tokens", 220);

        long started = System.nanoTime();
        HttpResponse<String> response;
        try {
            HttpRequest http = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(Math.max(1000, request.timeoutMillis())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            response = client.send(http, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new AiException(AiException.Kind.TIMEOUT, 0, "provider timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException(AiException.Kind.IO, 0, "interrupted");
        } catch (Exception e) {
            throw new AiException(AiException.Kind.IO, 0, "provider call failed: " + e.getClass().getSimpleName());
        }
        long latency = (System.nanoTime() - started) / 1_000_000L;
        int status = response.statusCode();
        if (status == 429) {
            throw new AiException(AiException.Kind.RATE_LIMIT, 429, "provider rate limit");
        }
        if (status != 200) {
            throw new AiException(AiException.Kind.HTTP, status, "provider HTTP " + status);
        }
        try {
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            JsonElement content = message.get("content");
            if (content == null || !content.isJsonPrimitive()) {
                throw new AiException(AiException.Kind.BAD_RESPONSE, 0, "no text content in response");
            }
            return new Completion(content.getAsString(), latency);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException(AiException.Kind.BAD_RESPONSE, 0, "unparseable provider response");
        }
    }
}
