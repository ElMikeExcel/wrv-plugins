/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Diagnostic evidence only: no response text, URL, headers, or arbitrary values escape. */
final class ResponseDiagnostics {
    static final int LIMIT = 4096;
    private static final Set<String> CODES = Set.of("INVALID_JSON", "INVALID_PAYLOAD", "VALIDATION_ERROR",
            "SCHEMA_VALIDATION_FAILED", "UNAUTHORIZED", "FORBIDDEN", "BAD_REQUEST", "PAYLOAD_TOO_LARGE");
    private static final Set<String> PATHS = Set.of("source", "sender", "polled_at", "events", "events.type",
            "events.time", "events.detail", "events.seal", "events.failed_posts", "events.down_since", "events.held_events");
    private static final Pattern FIELD = Pattern.compile("\"(code|error|path|field)\"\\s*:\\s*\"([A-Za-z_.]{1,64})\"");
    static String summary(String body) {
        if (body == null) return "diagnostic=unavailable";
        if (body.length() > LIMIT) body = body.substring(0, LIMIT);
        String code = null, path = null;
        Matcher m = FIELD.matcher(body);
        while (m.find()) {
            String key=m.group(1), value=m.group(2);
            if (code == null && (key.equals("code") || key.equals("error")) && CODES.contains(value)) code=value;
            if (path == null && (key.equals("path") || key.equals("field")) && PATHS.contains(value)) path=value;
        }
        return (code == null && path == null) ? "diagnostic=not-allowlisted"
                : (code == null ? "" : "code="+code) + (path == null ? "" : (code == null ? "" : " ")+"path="+path);
    }
    static HttpResponse.BodyHandler<String> handler() {
        return info -> info.statusCode() >= 200 && info.statusCode() < 300
                ? HttpResponse.BodySubscribers.replacing("") : new Bounded();
    }
    /** Complete and cancel at the byte cap. Never buffer an unbounded/streaming error body. */
    static final class Bounded implements HttpResponse.BodySubscriber<String> {
        final byte[] bytes = new byte[LIMIT]; int size;
        final CompletableFuture<String> result = new CompletableFuture<>();
        Flow.Subscription subscription;
        public CompletionStage<String> getBody() { return result; }
        public void onSubscribe(Flow.Subscription s) { subscription=s; s.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            if (result.isDone()) return;
            for (ByteBuffer b: buffers) {
                int count=Math.min(b.remaining(), LIMIT-size); b.get(bytes,size,count); size+=count;
                if (size == LIMIT) { subscription.cancel(); finish(); return; }
            }
            subscription.request(1);
        }
        public void onError(Throwable ignored) { result.complete(null); }
        public void onComplete() { finish(); }
        private void finish() { result.complete(new String(bytes,0,size,StandardCharsets.UTF_8)); }
    }
}
