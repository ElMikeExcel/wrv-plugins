/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class WebhookSenderTest {
    static WebhookSender.Settings settings(int port) {
        WebhookSender.Settings s = new WebhookSender.Settings();
        s.url = "http://127.0.0.1:" + port + "/hook"; s.key = "test-key-123"; s.backoffSeconds = List.of(0, 0, 0, 0); s.maxAttempts = 3; s.timeoutSeconds = 2; s.batchSeconds = 600;
        return s;
    }
    @Test void placeholdersAndHttpsRule() {
        WebhookSender.Settings s = new WebhookSender.Settings();
        s.url = "PASTE-WEBHOOK-URL-HERE"; s.key = "k"; assertNotNull(WebhookSender.problem(s));
        s.url = "http://example.com/x"; assertTrue(WebhookSender.problem(s).contains("https"));
        s.url = "https://example.com/x"; assertNull(WebhookSender.problem(s));
        s.key = "PASTE-WEBHOOK-KEY-HERE"; assertNotNull(WebhookSender.problem(s));
    }
    @Test void outageRetriesHoldsAndRecovers() throws Exception {
        AtomicInteger hits = new AtomicInteger(); AtomicInteger mode = new AtomicInteger(503);
        List<String> bodies = new CopyOnWriteArrayList<>(); List<String> auth = new CopyOnWriteArrayList<>();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/hook", ex -> { hits.incrementAndGet(); bodies.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.add(ex.getRequestHeaders().getFirst("Authorization")); ex.sendResponseHeaders(mode.get(), -1); ex.close(); });
        srv.start();
        File dir = Files.createTempDirectory("sa").toFile(); File ob = new File(dir, "outbox.jsonl");
        try {
            WebhookSender w = new WebhookSender(settings(srv.getAddress().getPort()), ob, Logger.getLogger("t"));
            w.enqueue(new LinkedHashMap<>(Map.of("type", "SEAL_BROKE", "seal", "wither")));
            assertFalse(w.flushNow(true));
            assertEquals(3, hits.get()); assertEquals(1, w.heldCount()); assertTrue(ob.isFile()); assertEquals("HTTP 503", w.lastError);
            assertEquals("Bearer test-key-123", auth.get(0));
            // restart: the held event survives in outbox.jsonl
            WebhookSender w2 = new WebhookSender(settings(srv.getAddress().getPort()), ob, Logger.getLogger("t"));
            assertEquals(1, w2.heldCount());
            w2.failStreak = 1; w2.failingSinceMs = System.currentTimeMillis();
            mode.set(200);
            w2.enqueue(new LinkedHashMap<>(Map.of("type", "heartbeat")));
            assertTrue(w2.flushNow(true));
            String last = bodies.get(bodies.size() - 1);
            assertTrue(last.startsWith("{\"source\": \"wrv-sealwatch\""), last);
            assertTrue(last.indexOf("alerts_recovered") < last.indexOf("SEAL_BROKE") && last.indexOf("SEAL_BROKE") < last.indexOf("heartbeat"), last);
            assertEquals(0, w2.heldCount()); assertFalse(ob.exists());
        } finally { srv.stop(0); }
    }
    @Test void unauthorizedIsNotHammered() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/hook", ex -> { hits.incrementAndGet(); ex.sendResponseHeaders(401, -1); ex.close(); }); srv.start();
        try {
            WebhookSender w = new WebhookSender(settings(srv.getAddress().getPort()), new File(Files.createTempDirectory("sa").toFile(), "o"), Logger.getLogger("t"));
            w.enqueue(new LinkedHashMap<>(Map.of("type", "test")));
            assertFalse(w.flushNow(true)); assertEquals(1, hits.get()); assertEquals("HTTP 401", w.lastError);
        } finally { srv.stop(0); }
    }
    @Test void timeoutIsShortAndErrorsAreScrubbed() throws Exception {
        // a server that accepts but never answers
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/hook", ex -> { try { Thread.sleep(10_000); } catch (InterruptedException ignored) { } ex.close(); });
        srv.setExecutor(Executors.newCachedThreadPool()); srv.start();
        try {
            WebhookSender.Settings s = settings(srv.getAddress().getPort()); s.maxAttempts = 1;
            WebhookSender w = new WebhookSender(s, new File(Files.createTempDirectory("sa").toFile(), "o"), Logger.getLogger("t"));
            w.enqueue(new LinkedHashMap<>(Map.of("type", "test")));
            long t0 = System.currentTimeMillis(); assertFalse(w.flushNow(true));
            assertTrue(System.currentTimeMillis() - t0 < 5000);
            assertFalse(w.lastError.contains("127.0.0.1")); assertFalse(w.lastError.contains("test-key-123"));
        } finally { srv.stop(0); }
    }
    @Test void badRequestRetainsQueueAndOnlySafeDiagnostics() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/hook", ex -> {
            hits.incrementAndGet(); ex.getRequestBody().readAllBytes();
            byte[] body = "{\"code\":\"INVALID_PAYLOAD\",\"path\":\"events.type\",\"message\":\"secret-token private-player\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(400, body.length); ex.getResponseBody().write(body); ex.close();
        }); srv.start();
        try {
            File ob = new File(Files.createTempDirectory("sa-diagnostic").toFile(), "outbox.jsonl");
            WebhookSender.Settings diagnostic = settings(srv.getAddress().getPort());
            WebhookSender w = new WebhookSender(diagnostic, ob, Logger.getLogger("t"));
            w.diagnostics(true);
            w.enqueue(Map.of("type", "test"));
            assertFalse(w.flushNow(true)); assertEquals(1, hits.get()); assertEquals(1,w.heldCount());
            assertEquals("HTTP 400",w.lastError);
            assertFalse(w.lastError.contains("secret-token")); assertFalse(w.lastError.contains("private-player"));
            assertTrue(ob.isFile());
            WebhookSender restarted = new WebhookSender(settings(srv.getAddress().getPort()),ob,Logger.getLogger("t"));
            assertEquals(1,restarted.heldCount());
        } finally { srv.stop(0); }
    }
    @Test void diagnosticsDefaultOffPreserves400StatusOnly() throws Exception {

        AtomicInteger hits = new AtomicInteger();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/hook", ex -> {
            hits.incrementAndGet(); ex.getRequestBody().readAllBytes();
            byte[] body = "{\"code\":\"INVALID_PAYLOAD\",\"message\":\"private-example\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(400, body.length); ex.getResponseBody().write(body); ex.close();
        }); srv.start();
        try {
            WebhookSender w = new WebhookSender(settings(srv.getAddress().getPort()),new File(Files.createTempDirectory("sa-off").toFile(),"outbox.jsonl"),Logger.getLogger("t"));
            w.enqueue(Map.of("type","test")); assertFalse(w.flushNow(true));
            assertEquals("HTTP 400",w.lastError); assertEquals(1,hits.get()); assertEquals(1,w.heldCount());
        } finally { srv.stop(0); }
    }
}
