package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelProtect — WATCH / ALERTS (Amendment 3): real-time pattern detection over the
 * event stream. Patterns: burst block-breaks inside a claim, valuable item
 * destruction, non-owner container access inside a claim. Delivery: online staff
 * in-game + the webhook outbox (outbox.jsonl held queue — SealAlerts' pattern;
 * POSTed when watch.webhook-url is set, held otherwise). Every alert is also a
 * record in the system stream. Excel — Wink Reyes Valley
 */
public final class Watch {
    private final Plugin plugin;
    private final Ledger ledger;
    private final Settings cfg;
    private final File outbox;
    private final Map<String, Deque<Long>> bursts = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newHttpClient();

    public Watch(Plugin plugin, Ledger ledger, Settings cfg, File dataDir) {
        this.plugin = plugin; this.ledger = ledger; this.cfg = cfg;
        this.outbox = new File(dataDir, "outbox.jsonl");
    }

    public void alert(String pattern, String detail, Location loc, Player actor) {
        if (!cfg.watchOn()) return;
        Json.Obj o = new Json.Obj().put("type", "alert").put("pattern", pattern).put("detail", detail);
        if (actor != null) o.put("player", actor.getName()).put("player_uuid", actor.getUniqueId().toString());
        ledger.emit("system", loc, o);
        String line = ledger.envelope(loc, o);
        // held outbox (the queue is the file; drained when a webhook URL exists)
        synchronized (outbox) {
            try {
                Files.writeString(outbox.toPath(), line + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) { }
        }
        String url = cfg.webhookUrl();
        if (url != null && !url.isBlank()) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"[ExcelProtect] "
                                + Json.esc(pattern + ": " + detail).replace("\"", "'") + "\"}"))
                        .build();
                http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) { /* held in the outbox file */ }
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.hasPermission("excelprotect.admin"))
                    p.sendMessage("§c[ExcelProtect Watch] §f" + pattern + ": " + detail);
        });
    }

    /** Burst block-breaks inside a claim (called from Blocks on player breaks). */
    public void blockBreak(Player p, Location loc) {
        if (!cfg.watchOn() || p == null) return;
        String owner = ledger.claimAt(loc);
        if (owner == null) return;
        String key = p.getUniqueId() + "|" + owner + "|" + loc.getWorld().getName();
        Deque<Long> dq = bursts.computeIfAbsent(key, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (dq) {
            dq.addLast(now);
            while (!dq.isEmpty() && now - dq.peekFirst() > cfg.watchBurstWindowSec() * 1000L) dq.pollFirst();
            if (dq.size() == cfg.watchBurstCount()) {
                alert("burst_breaks_in_claim", p.getName() + " broke " + dq.size() + " blocks in "
                        + owner + "'s claim within " + cfg.watchBurstWindowSec() + "s", loc, p);
            }
        }
        if (bursts.size() > 5000) bursts.clear();
    }

    /** Non-owner container access inside a claim (called from Containers on close). */
    public void containerAccess(Player p, Location loc) {
        if (!cfg.watchOn() || !cfg.watchContainerAccess() || p == null || loc == null) return;
        String owner = ledger.claimAt(loc);
        if (owner == null || owner.equalsIgnoreCase(p.getName())) return;
        alert("non_owner_container_access", p.getName() + " transacted in a container inside "
                + owner + "'s claim", loc, p);
    }

    /** Valuable item destruction (called from Recovery when a payload was captured). */
    public void valuableDestroyed(Player owner, String cause, ItemFact item, Location loc) {
        if (!cfg.watchOn() || !cfg.watchValuableDestruction()) return;
        alert("valuable_item_destroyed", (owner == null ? "unknown owner" : owner.getName())
                + "'s " + item.fingerprint() + " destroyed (" + cause + ")", loc, owner);
    }
}
