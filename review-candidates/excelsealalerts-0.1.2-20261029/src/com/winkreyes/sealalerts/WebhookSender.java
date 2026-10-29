/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Posts events to the seal watch webhook. Everything network-side runs on one
 * daemon thread (never the server main thread): batching, retries with
 * backoff, short timeouts. Events that cannot be delivered are held in
 * outbox.jsonl and go out with the next successful post. Never logs the URL
 * or key. Pure Java (no Bukkit) so it can be unit-tested.
 */
final class WebhookSender {
    static final class Settings {
        String url = "", key = "", header = "Authorization", prefix = "Bearer ", source = "wrv-sealwatch", sender = "ExcelSealAlerts";
        int timeoutSeconds = 10, maxAttempts = 5, batchSeconds = 600;
        List<Integer> backoffSeconds = List.of(5, 15, 45, 120, 300);
        Set<String> immediate = Set.of("server_started", "server_stopping", "heartbeat", "alerts_recovered", "test");
        ZoneId zone = ZoneId.of("America/Chicago");
    }
    record Item(String type, String json, long at) {}

    static final int HOLD_CAP = 1000;
    private final ConcurrentLinkedQueue<Item> pending = new ConcurrentLinkedQueue<>();
    private final Deque<String> held = new ArrayDeque<>(); // serialized events awaiting delivery (guarded by this)
    private volatile Settings s;
    private final File outbox; private final Logger log;
    private ScheduledExecutorService exec;
    private volatile HttpClient http;
    private volatile boolean flushRequested;
    // Excel runtime-only diagnostics: never persisted, never triggers a send.
    private boolean responseDiagnostics;
    private long diagnosticGeneration;
    synchronized boolean diagnosticsEnabled() { return responseDiagnostics; }
    synchronized void diagnostics(boolean enabled) {
        responseDiagnostics = enabled; diagnosticGeneration++;
    }
    private synchronized long diagnosticTicket() {
        return responseDiagnostics ? diagnosticGeneration : -1;
    }
    private synchronized void logDiagnostic(long ticket, int status, String body) {
        if (responseDiagnostics && ticket == diagnosticGeneration)
            log.warning("ExcelSealAlerts rejection hint HTTP " + status + " " + ResponseDiagnostics.summary(body));
    }
    // health (read by status/heartbeat)
    volatile long postsOk, postsFailed, lastOkMs, failingSinceMs; volatile int failStreak, dropped; volatile String lastError, configProblem;
    volatile boolean warnedConfig;

    WebhookSender(Settings s, File outbox, Logger log) { this.s = s; this.outbox = outbox; this.log = log; loadOutbox(); }

    void settings(Settings ns) { this.s = ns; this.http = null; warnedConfig = false; }
    Settings settings() { return s; }

    /** Null when the URL and key are usable, else why not (never echoes the values). */
    static String problem(Settings s) {
        if (s.url == null || s.url.isBlank() || s.url.contains("PASTE-")) return "webhook.url is still the placeholder";
        if (s.key == null || s.key.isBlank() || s.key.contains("PASTE-")) return "webhook.key is still the placeholder";
        URI u;
        try { u = URI.create(s.url.trim()); } catch (Exception e) { return "webhook.url is not a valid URL"; }
        String h = u.getHost() == null ? "" : u.getHost();
        boolean local = h.equals("localhost") || h.equals("127.0.0.1") || h.equals("[::1]") || h.equals("::1");
        if (!"https".equalsIgnoreCase(u.getScheme()) && !(local && "http".equalsIgnoreCase(u.getScheme())))
            return "webhook.url must start with https:// (plain http is only allowed to localhost for testing)";
        return null;
    }

    void start() {
        exec = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "ExcelSealAlerts-sender"); t.setDaemon(true); return t; });
        exec.scheduleWithFixedDelay(this::tickSafe, 2, 2, TimeUnit.SECONDS);
    }

    void enqueue(Map<String, Object> ev) {
        String type = String.valueOf(ev.get("type"));
        pending.add(new Item(type, Json.of(ev), System.currentTimeMillis()));
        if (s.immediate.contains(type)) flushRequested = true;
    }
    void requestFlush() { flushRequested = true; }
    int pendingCount() { return pending.size(); }
    synchronized int heldCount() { return held.size(); }

    private void tickSafe() {
        try { tick(); } catch (Throwable t) { lastError = clean(String.valueOf(t)); }
    }
    private void tick() {
        Item head = pending.peek();
        boolean due = flushRequested || (head != null && System.currentTimeMillis() - head.at() >= s.batchSeconds * 1000L);
        boolean retryHeld; synchronized (this) { retryHeld = !held.isEmpty() && failStreak > 0
                && System.currentTimeMillis() - lastAttemptEndMs >= Math.min(1800_000L, 300_000L * failStreak); }
        if (!due && !retryHeld) return;
        flushRequested = false;
        flushNow(true);
    }
    private volatile long lastAttemptEndMs;

    /** Drain pending into held, then try to deliver everything held. Returns true on delivery. */
    boolean flushNow(boolean withRetries) {
        List<String> batch;
        synchronized (this) {
            Item it; while ((it = pending.poll()) != null) held.addLast(it.json());
            while (held.size() > HOLD_CAP) { held.removeFirst(); dropped++; }
            if (held.isEmpty()) return true;
            batch = new ArrayList<>(held);
        }
        Settings cur = s;
        String why = problem(cur);
        if (why != null) {
            configProblem = why;
            if (!warnedConfig) { log.warning("Not posting: " + why + ". Events are held in outbox.jsonl until it is set (then /sealalerts reload)."); warnedConfig = true; }
            saveOutbox(); return false;
        }
        configProblem = null;
        boolean recovering = failStreak > 0;
        if (recovering) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "alerts_recovered"); r.put("detail", "seal alerts are reaching the webhook again");
            r.put("failed_posts", failStreak); r.put("down_since", fmt(failingSinceMs)); r.put("held_events", batch.size());
            if (dropped > 0) r.put("dropped_events", dropped);
            batch.add(0, Json.of(r));
        }
        String body = envelope(batch);
        int attempts = withRetries ? Math.max(1, cur.maxAttempts) : 1;
        int attemptsMade = 0;
        for (int a = 1; a <= attempts; a++) {
            attemptsMade = a;
            int code = post(cur, body, withRetries ? cur.timeoutSeconds : Math.min(3, cur.timeoutSeconds));
            if (code >= 200 && code < 300) {
                synchronized (this) { for (int i = 0; i < batch.size() - (recovering ? 1 : 0); i++) held.pollFirst(); }
                postsOk++; lastOkMs = System.currentTimeMillis(); failStreak = 0; failingSinceMs = 0; dropped = 0; lastAttemptEndMs = lastOkMs;
                saveOutbox();
                if (recovering) log.info("Webhook reachable again; delivered " + (batch.size() - 1) + " held event(s).");
                return true;
            }
            boolean permanent = code >= 400 && code < 500 && code != 408 && code != 429;
            if (permanent || a == attempts) break;
            long wait = cur.backoffSeconds.isEmpty() ? 5 : cur.backoffSeconds.get(Math.min(a - 1, cur.backoffSeconds.size() - 1));
            try { Thread.sleep(wait * 1000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        postsFailed++; if (failStreak == 0) failingSinceMs = System.currentTimeMillis(); failStreak++; lastAttemptEndMs = System.currentTimeMillis();
        log.warning("Webhook post failed (" + lastError + ", " + attemptsMade + " attempt(s)); " + heldCount() + " event(s) held for the next try.");
        saveOutbox();
        return false;
    }

    /** One HTTP POST. Returns the status code, or -1 on a network error / timeout. */
    private int post(Settings cur, String body, int timeoutS) {
        try {
            HttpClient c = http;
            if (c == null) { c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(1, timeoutS))).followRedirects(HttpClient.Redirect.NEVER).build(); http = c; }
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cur.url.trim())).timeout(Duration.ofSeconds(Math.max(1, timeoutS)))
                    .header("Content-Type", "application/json").header("User-Agent", cur.sender)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            String hdr = cur.header == null || cur.header.isBlank() ? "Authorization" : cur.header.trim();
            b.header(hdr, (cur.prefix == null ? "" : cur.prefix) + cur.key.trim());
            long diagnosticTicket = diagnosticTicket();
            if (diagnosticTicket < 0) {
                HttpResponse<Void> r = c.send(b.build(), HttpResponse.BodyHandlers.discarding());
                if (r.statusCode() < 200 || r.statusCode() >= 300) lastError = "HTTP " + r.statusCode();
                return r.statusCode();
            }
            HttpResponse<String> r = c.send(b.build(), ResponseDiagnostics.handler());
            if (r.statusCode() < 200 || r.statusCode() >= 300) {
                lastError = "HTTP " + r.statusCode(); // Preserve outgoing heartbeat health.
                logDiagnostic(diagnosticTicket, r.statusCode(), r.body());
            }
            return r.statusCode();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); lastError = "interrupted"; return -1;
        } catch (Exception e) {
            lastError = clean(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage())); return -1;
        }
    }

    String envelope(List<String> eventsJson) {
        Settings cur = s;
        StringBuilder b = new StringBuilder();
        b.append("{\"source\": ").append(Json.of(cur.source)).append(", \"sender\": ").append(Json.of(cur.sender))
         .append(", \"polled_at\": ").append(Json.of(ZonedDateTime.now(cur.zone).format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))))
         .append(", \"events\": [").append(String.join(", ", eventsJson)).append("]}");
        return b.toString();
    }

    /** Server stop: stop the sender thread, then one short synchronous try for everything still queued. */
    void shutdown(Map<String, Object> lastEvent) {
        if (exec != null) {
            exec.shutdownNow();
            try { exec.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (lastEvent != null) enqueue(lastEvent);
        if (!flushNow(false)) log.info("Could not deliver on shutdown; " + heldCount() + " event(s) saved to outbox.jsonl for the next start.");
    }

    String clean(String m) {
        if (m == null) return null;
        Settings cur = s;
        if (cur.url != null && !cur.url.isBlank()) m = m.replace(cur.url.trim(), "<webhook>");
        if (cur.key != null && !cur.key.isBlank()) m = m.replace(cur.key.trim(), "<redacted>");
        try { String h = URI.create(cur.url.trim()).getHost(); if (h != null && !h.isBlank()) m = m.replace(h, "<webhook-host>"); } catch (Exception ignored) { }
        m = m.replaceAll("\\b\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?\\b", "<addr>");
        return m.length() > 200 ? m.substring(0, 200) : m;
    }
    String fmt(long ms) { return ms <= 0 ? null : Instant.ofEpochMilli(ms).atZone(s.zone).format(DateTimeFormatter.ofPattern("h:mm a z MMM d", Locale.US)); }

    private synchronized void saveOutbox() {
        try {
            if (held.isEmpty()) { Files.deleteIfExists(outbox.toPath()); return; }
            File tmp = new File(outbox.getPath() + ".tmp");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) { for (String j : held) w.write(j + "\n"); }
            Files.move(tmp.toPath(), outbox.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) { log.warning("Could not save outbox.jsonl: " + e.getMessage()); }
    }
    private synchronized void loadOutbox() {
        if (!outbox.isFile()) return;
        try { for (String l : Files.readAllLines(outbox.toPath(), StandardCharsets.UTF_8)) if (l.startsWith("{")) held.addLast(l); }
        catch (IOException e) { log.warning("Could not read outbox.jsonl: " + e.getMessage()); }
        while (held.size() > HOLD_CAP) held.removeFirst();
        if (!held.isEmpty()) log.info(held.size() + " held event(s) from before the restart will go out with the next post.");
    }
}
