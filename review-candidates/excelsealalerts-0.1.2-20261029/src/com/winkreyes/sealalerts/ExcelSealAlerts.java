/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * ExcelSealAlerts: the server pushes seal events to the seal watch webhook.
 * Sources: console taps on ExcelSealLog / ExcelGuildGate / ExcelBossTrial
 * (their own log lines, as they happen), plus an off-thread re-read of the
 * GuildGate and BossTrial state files every statePollSeconds. Never changes
 * gameplay, never cancels events, never posts on the main thread.
 */
public final class ExcelSealAlerts extends JavaPlugin implements Listener {
    static final List<String> TAPPED = List.of("ExcelSealLog", "ExcelGuildGate", "ExcelBossTrial");
    private final Map<String, Handler> taps = new LinkedHashMap<>();
    private WebhookSender sender; private SealParser parser; private StateWatch watch; private Names names;
    private ZoneId zone; private LocalTime hbTime; private boolean hbEnabled;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private YamlConfiguration data; private File dataFile;
    private long startedMs; private boolean wasUnclean; private String lastAliveBefore;
    private volatile boolean firstPollDone;
    private boolean firstRun;

    @Override public void onEnable() {
        startedMs = System.currentTimeMillis();
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        firstRun = !dataFile.exists();
        data = YamlConfiguration.loadConfiguration(dataFile);
        wasUnclean = data.getBoolean("running", false);
        lastAliveBefore = data.getString("lastAlive");
        sender = new WebhookSender(readSettings(), new File(getDataFolder(), "outbox.jsonl"), getLogger());
        applyConfig();
        watch.baseline(data.contains("baseline.broken") ? data.getStringList("baseline.broken") : null,
                       data.contains("baseline.cooling") ? data.getStringList("baseline.cooling") : null);
        sender.start();
        getServer().getPluginManager().registerEvents(this, this);
        for (String n : TAPPED) { Plugin p = getServer().getPluginManager().getPlugin(n); if (p != null && p.isEnabled()) tap(p); }
        setData("running", true);
        long poll = 20L * Math.max(30, Math.min(60, getConfig().getInt("statePollSeconds", 30)));
        tasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::pollState, 40L, poll));
        tasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::minuteTick, 600L, 600L)); // every 30 s
        getLogger().info("READY version=" + getPluginMeta().getVersion() + " taps=" + taps.keySet()
                + " webhook=" + (WebhookSender.problem(sender.settings()) == null ? "configured" : "NOT configured (" + WebhookSender.problem(sender.settings()) + ")")
                + " batch=" + sender.settings().batchSeconds + "s heartbeat=" + (hbEnabled ? hbTime + " " + zone : "off"));
    }

    @Override public void onDisable() {
        for (BukkitTask t : tasks) t.cancel(); tasks.clear();
        untapAll();
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("type", "server_stopping"); ev.put("time", now());
        ev.put("uptime", StateWatch.left(System.currentTimeMillis() - startedMs));
        List<String> open = parser == null ? List.of() : parser.openFights();
        if (!open.isEmpty()) ev.put("open_fights", open);
        if (sender != null) sender.shutdown(ev);
        if (data != null) synchronized (data) {
            data.set("running", false); data.set("lastAlive", now());
            if (watch != null) { data.set("baseline.broken", new ArrayList<>(watch.brokenNow())); data.set("baseline.cooling", new ArrayList<>(watch.coolingNow())); }
            try { data.save(dataFile); } catch (Exception e) { getLogger().warning("Could not save data.yml: " + e.getMessage()); }
        }
    }

    private WebhookSender.Settings readSettings() {
        WebhookSender.Settings s = new WebhookSender.Settings();
        s.url = getConfig().getString("webhook.url", ""); s.key = getConfig().getString("webhook.key", "");
        s.header = getConfig().getString("webhook.header", "Authorization");
        s.prefix = getConfig().getString("webhook.prefix", "Authorization".equalsIgnoreCase(s.header) ? "Bearer " : "");
        s.timeoutSeconds = Math.max(2, Math.min(30, getConfig().getInt("webhook.timeoutSeconds", 10)));
        s.maxAttempts = Math.max(1, Math.min(10, getConfig().getInt("webhook.maxAttempts", 5)));
        List<Integer> bo = getConfig().getIntegerList("webhook.backoffSeconds"); if (!bo.isEmpty()) s.backoffSeconds = bo;
        s.batchSeconds = Math.max(0, getConfig().getInt("batchSeconds", 600));
        List<String> im = getConfig().getStringList("immediateTypes"); if (!im.isEmpty()) s.immediate = new HashSet<>(im);
        s.source = getConfig().getString("source", "wrv-sealwatch");
        s.sender = "ExcelSealAlerts 0.1.0 (server plugin)"; // Excel: preserve deployed wire identity during diagnostics.
        try { s.zone = ZoneId.of(getConfig().getString("heartbeat.zone", "America/Chicago")); } catch (Exception e) { s.zone = ZoneId.of("America/Chicago"); }
        return s;
    }
    private void applyConfig() {
        WebhookSender.Settings s = sender.settings(); zone = s.zone;
        hbEnabled = getConfig().getBoolean("heartbeat.enabled", true);
        try { hbTime = LocalTime.parse(getConfig().getString("heartbeat.time", "08:00")); } catch (Exception e) { hbTime = LocalTime.of(8, 0); }
        Map<String, String> labels = new HashMap<>();
        ConfigurationSection ls = getConfig().getConfigurationSection("players.labels");
        if (ls != null) for (String k : ls.getKeys(false)) labels.put(k, ls.getString(k));
        names = new Names(getConfig().getString("players.mode", "gamertag"), labels);
        if (parser == null) parser = new SealParser(names, zone); else parser.names(names);
        if (watch == null) watch = new StateWatch(names, zone); else watch.names(names);
    }

    // ---------------------------------------------------------------- taps
    private void tap(Plugin p) {
        String name = p.getName();
        if (!TAPPED.contains(name) || taps.containsKey(name)) return;
        Handler h = new Handler() {
            @Override public void publish(LogRecord r) {
                try {
                    String msg = r.getMessage();
                    if (msg == null) return;
                    if (r.getParameters() != null && r.getParameters().length > 0) msg = java.text.MessageFormat.format(msg, r.getParameters());
                    parser.accept(name, msg, r.getMillis(), sender::enqueue);
                } catch (Throwable ignored) { } // never disturb the tapped plugin's logging
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        h.setLevel(java.util.logging.Level.ALL);
        p.getLogger().addHandler(h);
        taps.put(name, h);
    }
    private void untap(String name) {
        Handler h = taps.remove(name); if (h == null) return;
        Plugin p = getServer().getPluginManager().getPlugin(name);
        if (p != null) p.getLogger().removeHandler(h);
    }
    private void untapAll() { for (String n : new ArrayList<>(taps.keySet())) untap(n); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent e) { if (TAPPED.contains(e.getPlugin().getName())) tap(e.getPlugin()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent e) { if (TAPPED.contains(e.getPlugin().getName())) untap(e.getPlugin().getName()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("type", "server_started"); ev.put("time", now());
        ev.put("previous_stop", wasUnclean ? "unclean: the server stopped without shutting this plugin down (crash, kill or host restart); last seen alive "
                + (lastAliveBefore == null ? "unknown" : lastAliveBefore) : (firstRun ? "none (first start with ExcelSealAlerts)" : "clean"));
        ev.put("plugins_ok", taps.keySet().containsAll(TAPPED));
        List<String> missing = new ArrayList<>(TAPPED); missing.removeAll(taps.keySet());
        if (!missing.isEmpty()) ev.put("not_watching", missing);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            pollState(); // fills seal counts
            StateWatch.Snapshot s = watch.latest;
            if (s != null && s.broken != null) ev.put("seals_broken", s.broken.size() + "/" + s.total);
            sender.enqueue(ev);
        }, 20L);
    }

    // ---------------------------------------------------------------- state poll (async)
    private synchronized void pollState() {
        try {
            File gg = new File(getDataFolder().getParentFile(), "ExcelGuildGate/state.yml");
            File bt = new File(getDataFolder().getParentFile(), "ExcelBossTrial/state.yml");
            Map<String, String> broken = null; int total = 5;
            if (gg.isFile()) {
                YamlConfiguration y = new YamlConfiguration(); y.load(gg);
                ConfigurationSection seals = y.getConfigurationSection("seals");
                if (seals != null) {
                    broken = new TreeMap<>(); total = Math.max(1, seals.getKeys(false).size());
                    for (String k : seals.getKeys(false)) {
                        Object b = seals.get(k + ".broken");
                        if (Boolean.TRUE.equals(b) || (b instanceof Number n && n.intValue() != 0)) broken.put(k, seals.getString(k + ".killer"));
                    }
                }
            }
            Map<String, Long> cds = null;
            if (bt.isFile()) {
                YamlConfiguration y = new YamlConfiguration(); y.load(bt);
                cds = new TreeMap<>();
                ConfigurationSection c = y.getConfigurationSection("cooldowns");
                if (c != null) for (String k : c.getKeys(false)) cds.put(k, c.getLong(k));
            }
            long now = System.currentTimeMillis();
            boolean whileOff = !firstPollDone;
            watch.observe(new StateWatch.Snapshot(broken, cds, total), now,
                    now - parser.lastSealBrokeMs < 180_000L, now - parser.lastAdminMs < 180_000L,
                    parser.cooldownAnnounced, whileOff, sender::enqueue);
            firstPollDone = true;
        } catch (Exception ex) {
            getLogger().fine("state poll skipped: " + ex.getMessage()); // a file mid-write; next poll catches up
        }
    }

    // ---------------------------------------------------------------- every 30 s (async)
    private void minuteTick() {
        long now = System.currentTimeMillis();
        parser.sweep(now, sender::enqueue);
        setData("lastAlive", now());
        if (!hbEnabled) return;
        ZonedDateTime z = ZonedDateTime.now(zone);
        String today = z.toLocalDate().toString(); String lastHb; synchronized (data) { lastHb = data.getString("lastHeartbeatDate"); }
        if (z.toLocalTime().isBefore(hbTime) || today.equals(lastHb)) return;
        setData("lastHeartbeatDate", today);
        boolean late = z.toLocalTime().isAfter(hbTime.plusMinutes(5));
        Bukkit.getScheduler().runTask(this, () -> { // read the online count / TPS on the main thread, then post off it
            int online = Bukkit.getOnlinePlayers().size(); double tps = Bukkit.getTPS()[0];
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> sender.enqueue(heartbeat(online, tps, late)));
        });
    }
    Map<String, Object> heartbeat(int online, double tps, boolean late) {
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("type", "heartbeat"); ev.put("time", now());
        if (late) ev.put("note", "sent late: the server was not running at " + hbTime);
        StateWatch.Snapshot s = watch.latest;
        if (s != null && s.broken != null) { ev.put("seals_broken", s.broken.size() + "/" + s.total); ev.put("broken", new ArrayList<>(s.broken.keySet())); ev.put("guild", s.broken.size() >= s.total ? "OPEN" : "LOCKED"); }
        if (s != null && s.cooldowns != null) {
            Map<String, Object> cd = new TreeMap<>(); long n = System.currentTimeMillis();
            s.cooldowns.forEach((k, v) -> { if (v != null && v > n) cd.put(SealParser.fightKey(k), StateWatch.left(v - n) + " left"); });
            ev.put("cooldowns", cd);
        }
        ev.put("open_fights", parser.openFights());
        ev.put("players_online", online); ev.put("tps", Math.round(tps * 10) / 10.0);
        ev.put("uptime", StateWatch.left(System.currentTimeMillis() - startedMs));
        ev.put("watching", new ArrayList<>(taps.keySet()));
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("posts_ok", sender.postsOk); h.put("posts_failed", sender.postsFailed); h.put("held_events", sender.heldCount());
        if (sender.lastError != null) h.put("last_error", sender.lastError);
        ev.put("alerts_health", h);
        ev.put("next_heartbeat", ZonedDateTime.now(zone).toLocalDate().plusDays(1).atTime(hbTime).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a z MMM d", Locale.US)));
        return ev;
    }

    private String now() { return ZonedDateTime.now(zone).format(DateTimeFormatter.ofPattern("h:mm a z MMM d", Locale.US)); }
    private void setData(String k, Object v) {
        String snapshot; synchronized (data) { data.set(k, v); snapshot = data.saveToString(); }
        Runnable w = () -> { synchronized (dataFile) { try { java.nio.file.Files.writeString(dataFile.toPath(), snapshot); } catch (Exception ignored) { } } };
        if (isEnabled() && Bukkit.isPrimaryThread()) Bukkit.getScheduler().runTaskAsynchronously(this, w); else w.run();
    }

    // ---------------------------------------------------------------- command
    @Override public boolean onCommand(CommandSender cs, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "status" : a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("diagnostics")) {
            // Console only, not players, command blocks, or remote-console senders.
            if (!(cs instanceof ConsoleCommandSender)) {
                cs.sendMessage("ExcelSealAlerts diagnostics is console-only."); return true;
            }
            if (a.length != 2 || !Set.of("enable", "disable", "status").contains(a[1].toLowerCase(Locale.ROOT))) {
                cs.sendMessage("Usage: sealalerts diagnostics <enable|disable|status>"); return true;
            }
            String action = a[1].toLowerCase(Locale.ROOT);
            if (action.equals("enable")) sender.diagnostics(true);
            if (action.equals("disable")) sender.diagnostics(false);
            cs.sendMessage("ExcelSealAlerts response hints: " + (sender.diagnosticsEnabled() ? "ON" : "OFF")
                    + " (runtime only; no send, flush, replay or config write).");
            return true;
        }
        switch (sub) {
            case "test" -> {
                Map<String, Object> ev = new LinkedHashMap<>(); ev.put("type", "test"); ev.put("detail", "manual test from /sealalerts test"); ev.put("time", now());
                sender.enqueue(ev); cs.sendMessage("§aTest event queued; it posts within a few seconds. Check /sealalerts status.");
            }
            case "flush" -> { sender.requestFlush(); cs.sendMessage("§aFlush requested."); }
            case "reload" -> {
                reloadConfig(); sender.settings(readSettings()); applyConfig();
                String p = WebhookSender.problem(sender.settings());
                cs.sendMessage(p == null ? "§aReloaded. Webhook configured; held events will go out now." : "§eReloaded. Not posting: " + p + ".");
                if (p == null) sender.requestFlush();
            }
            default -> {
                String p = WebhookSender.problem(sender.settings());
                cs.sendMessage("§6ExcelSealAlerts " + getPluginMeta().getVersion());
                cs.sendMessage("§7Webhook: " + (p == null ? "§aconfigured" : "§c" + p));
                cs.sendMessage("§7Watching: §f" + taps.keySet() + "§7, open fights: §f" + parser.openFights());
                cs.sendMessage("§7Queued: §f" + sender.pendingCount() + "§7, held: §f" + sender.heldCount() + "§7, posts ok/failed: §f" + sender.postsOk + "/" + sender.postsFailed
                        + (sender.lastOkMs > 0 ? "§7, last ok " + sender.fmt(sender.lastOkMs) : ""));
                if (sender.lastError != null) cs.sendMessage("§7Last error: §f" + sender.lastError);
                cs.sendMessage("§7Heartbeat: §f" + (hbEnabled ? hbTime + " " + zone : "off") + "§7, batch: §f" + sender.settings().batchSeconds + "s");
            }
        }
        return true;
    }
}
