/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/**
 * Turns the seal plugins' own console messages into seal watch events.
 * The ExcelSealLog and ExcelGuildGate rules are a line-for-line port of
 * sealwatch.py parse_lines() (same event types, same fields, same order),
 * so the existing routine reads them unchanged. ExcelBossTrial verdict
 * lines add TRIAL_* events. Pure Java (no Bukkit), thread-safe.
 */
final class SealParser {
    static final Pattern SEAL_RE = Pattern.compile("^Seal broken: (\\S+)(?: \\(killer: ([^)]+)\\))? - (\\d+) of (\\d+)");
    static final Pattern UNLOCK_RE = Pattern.compile("^All seals broken");
    static final Pattern ADMIN_RE = Pattern.compile("^\\[GuildGate\\] (.+ (granted seal|revoked seal|reset the Road to the Guild|opened the guild with).*)$");
    static final Pattern RAID_OMEN_RE = Pattern.compile("^Raid victory under Bad Omen level (\\d+) - raid seal NOT credited \\(the seal requires Bad Omen level (\\d+)");
    static final Pattern RAID_NOWIN_RE = Pattern.compile("^Raid victory with NO player among its winners");
    static final Pattern SL_RE = Pattern.compile("^([A-Z_]+) (.*)$");
    static final Pattern BT_ATTEMPT = Pattern.compile("^BossTrial announce \\[attempt\\] (\\S+) challenged (.+)\\.$");
    static final Pattern BT_CREDIT = Pattern.compile("^BossTrial verdict CREDIT: (.+) seal earned \\(([\\d.]+)% player damage, (\\d+) champion");
    static final Pattern BT_DENY = Pattern.compile("^BossTrial verdict (DENY_SHARE|DENY_PRESENCE): (.+) seal NOT credited \\(([\\d.]+)% player damage, (\\d+) present\\)\\. Cooldown until (.+?) from now");
    static final Pattern BT_DENY_CD = Pattern.compile("^BossTrial verdict DENY_COOLDOWN: (.+) kill was fought for but the seal is cooling down \\((.+) left\\)");
    static final Pattern BT_BURROW = Pattern.compile("^BossTrial burrow \\[(.+?)\\]");
    static final Pattern BT_CLOSED = Pattern.compile("^BossTrial encounter \\[(.+?)\\] closed without a verdict: (.+?) \\(no cooldown\\)");
    static final Pattern BT_ELDER = Pattern.compile("^BossTrial elder progress: monument trio now (\\d) of 3");

    static final class Fight {
        final TreeSet<String> players = new TreeSet<>(); int deaths; final String since; long last;
        Fight(String since, long now) { this.since = since; this.last = now; }
    }

    private final Map<String, Fight> fights = new LinkedHashMap<>();
    private final Map<String, Long> ended = new HashMap<>();
    private volatile Names names; private final ZoneId zone;
    /** /sealalerts reload: a players.mode change applies to the next event. */
    void names(Names n) { this.names = n; }
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    SealParser(Names names, ZoneId zone) { this.names = names; this.zone = zone; }

    /** GuildGate / BossTrial seal ids to the SealLog fight key ("ender_dragon" -> "dragon"). */
    static String fightKey(String sealId) { return "ender_dragon".equals(sealId) ? "dragon" : sealId; }
    static String sealOfDisplay(String display) {
        String d = display.toLowerCase(Locale.ROOT);
        if (d.contains("dragon")) return "dragon";
        if (d.contains("wither")) return "wither";
        if (d.contains("elder")) return "elder_guardian";
        if (d.contains("warden")) return "warden";
        if (d.contains("raid")) return "raid";
        return d.replace(' ', '_');
    }
    static Map<String, String> kv(String rest) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String x : rest.split(" ")) { int i = x.indexOf('='); if (i > 0) m.put(x.substring(0, i), x.substring(i + 1)); }
        return m;
    }
    private List<String> plist(String v) {
        if (v == null || v.isEmpty() || v.equals("-")) return List.of();
        List<String> o = new ArrayList<>(); for (String s : v.split(",")) o.add(names.name(s)); return o;
    }
    private static Map<String, Object> ev(String type) { Map<String, Object> e = new LinkedHashMap<>(); e.put("type", type); return e; }
    private String nm(String raw, String key) {
        return switch (key) { case "player" -> names.name(raw); case "nearby", "players", "winners" -> names.list(raw); default -> raw; };
    }

    /** Feed one console message from a plugin logger. Events go to out. */
    synchronized void accept(String plugin, String msg, long epochMs, Consumer<Map<String, Object>> out) {
        if (msg == null) return;
        String t = Instant.ofEpochMilli(epochMs).atZone(zone).format(HMS);
        long now = epochMs;
        if ("ExcelSealLog".equals(plugin)) { sealLog(msg, t, now, out); return; }
        if ("ExcelGuildGate".equals(plugin)) { guildGate(msg, t, now, out); return; }
        if ("ExcelBossTrial".equals(plugin)) bossTrial(msg, t, now, out);
    }

    private void sealLog(String msg, String t, long now, Consumer<Map<String, Object>> out) {
        Matcher m = SL_RE.matcher(msg);
        if (!m.find()) return;
        String kind = m.group(1); Map<String, String> d = kv(m.group(2)); String sid = d.get("seal");
        if (sid == null || kind.equals("READY")) return;
        List<String> ps = new ArrayList<>();
        ps.addAll(plist(d.get("players") != null && !d.get("players").isEmpty() ? d.get("players") : d.get("nearby")));
        ps.addAll(plist(d.get("player"))); ps.addAll(plist(d.get("winners")));
        Fight f = fights.get(sid);
        switch (kind) {
            case "FIGHT_START", "BOSS_SPAWN", "BOSS_EMERGE", "RAID_START" -> {
                if (f == null) {
                    f = new Fight(t, now); fights.put(sid, f);
                    Map<String, Object> e = ev("FIGHT_START"); e.put("fight", sid); e.put("signal", kind); e.put("time", t); e.put("verified", true);
                    for (String k : List.of("player", "nearby", "at", "village", "health", "waves", "omen")) if (d.containsKey(k)) e.put(k, nm(d.get(k), k));
                    out.accept(e);
                }
                f.players.addAll(ps); f.last = now;
            }
            case "BOSS_HEALTH", "RAID_WAVE", "PLAYER_JOINED" -> {
                if (f == null) { f = new Fight(t, now); fights.put(sid, f); }
                f.players.addAll(ps); f.last = now;
                Map<String, Object> e = ev("FIGHT_PROGRESS"); e.put("fight", sid); e.put("signal", kind); e.put("players", new ArrayList<>(f.players)); e.put("time", t); e.put("verified", true);
                for (String k : List.of("milestone", "health", "wave", "raiders", "player", "village")) if (d.containsKey(k)) e.put(k, nm(d.get(k), k));
                out.accept(e);
            }
            case "BOSS_DEATH", "RAID_WON", "RAID_LOST", "RAID_ENDED" -> {
                String killer = d.get("killer");
                if (kind.equals("BOSS_DEATH") && f == null && (killer == null || killer.equals("-")) && ps.isEmpty()) return; // stray boss death, no fight
                Fight g = fights.remove(sid);
                String res = switch (kind) { case "BOSS_DEATH", "RAID_WON" -> "won"; case "RAID_LOST" -> "lost"; default -> "ended (" + d.getOrDefault("reason", "?") + ")"; };
                String k = (killer == null || killer.equals("-")) ? null : names.name(killer);
                if (kind.equals("BOSS_DEATH") && k == null) res = "boss died, no player killer";
                TreeSet<String> all = new TreeSet<>(ps); if (g != null) all.addAll(g.players);
                Map<String, Object> e = ev("FIGHT_END"); e.put("fight", sid); e.put("result", res); e.put("killer", k); e.put("players", new ArrayList<>(all)); e.put("time", t); e.put("verified", true);
                out.accept(e); ended.put(sid, now);
            }
            default -> { }
        }
    }

    private void guildGate(String msg, String t, long now, Consumer<Map<String, Object>> out) {
        Matcher m = SEAL_RE.matcher(msg);
        if (m.find()) {
            String sid = m.group(1); String k = m.group(2) == null ? null : names.name(m.group(2));
            Map<String, Object> e = ev("SEAL_BROKE"); e.put("seal", sid); e.put("killer", k); e.put("count", m.group(3) + "/" + m.group(4)); e.put("time", t); e.put("verified", true);
            out.accept(e); lastSealBrokeMs = now;
            String fk = fightKey(sid);
            Fight f = fights.remove(fk);
            if (f == null && now - ended.getOrDefault(fk, 0L) < 900_000L) return; // SealLog already reported the end
            Map<String, Object> e2 = ev("FIGHT_END"); e2.put("fight", fk); e2.put("result", "won"); e2.put("killer", k); e2.put("players", f == null ? List.of() : new ArrayList<>(f.players)); e2.put("time", t);
            out.accept(e2); ended.put(fk, now); return;
        }
        if (UNLOCK_RE.matcher(msg).find()) { Map<String, Object> e = ev("GUILD_UNLOCKED"); e.put("time", t); out.accept(e); return; }
        m = ADMIN_RE.matcher(msg);
        if (m.find()) { Map<String, Object> e = ev("ADMIN_SEAL_CHANGE"); e.put("detail", m.group(1)); e.put("time", t); out.accept(e); lastAdminMs = now; return; }
        m = RAID_OMEN_RE.matcher(msg);
        if (m.find()) {
            Map<String, Object> e = ev("TRIAL_FAILED"); e.put("fight", "raid"); e.put("reason", "raid won under Bad Omen " + m.group(1) + ", the seal needs Bad Omen " + m.group(2) + " (no cooldown)"); e.put("time", t); e.put("verified", true);
            out.accept(e); return;
        }
        if (RAID_NOWIN_RE.matcher(msg).find()) {
            Map<String, Object> e = ev("TRIAL_FAILED"); e.put("fight", "raid"); e.put("reason", "raid won with no player among the winners (no cooldown)"); e.put("time", t); e.put("verified", true);
            out.accept(e);
        }
    }

    private void bossTrial(String msg, String t, long now, Consumer<Map<String, Object>> out) {
        Matcher m;
        if ((m = BT_ATTEMPT.matcher(msg)).find()) {
            Map<String, Object> e = ev("TRIAL_STARTED"); e.put("fight", sealOfDisplay(m.group(2))); e.put("player", names.name(m.group(1))); e.put("time", t); e.put("verified", true); out.accept(e); return;
        }
        if ((m = BT_CREDIT.matcher(msg)).find()) {
            Map<String, Object> e = ev("TRIAL_WON"); e.put("fight", sealOfDisplay(m.group(1))); e.put("player_damage", m.group(2) + "%"); e.put("champions_standing", Integer.parseInt(m.group(3))); e.put("time", t); e.put("verified", true); out.accept(e); return;
        }
        if ((m = BT_DENY.matcher(msg)).find()) {
            String fight = sealOfDisplay(m.group(2));
            Map<String, Object> e = ev("TRIAL_FAILED"); e.put("fight", fight);
            e.put("reason", m.group(1).equals("DENY_SHARE") ? "players did not do enough of the damage" : "not enough champions present at the kill");
            e.put("player_damage", m.group(3) + "%"); e.put("present", Integer.parseInt(m.group(4))); e.put("cooldown", m.group(5)); e.put("time", t); e.put("verified", true); out.accept(e);
            // Two kills of the same boss in the same moment give two verdicts but one cooldown: report it once.
            Long prev = cooldownAnnounced.get(fight);
            if (prev == null || now - prev >= 60_000L) {
                Map<String, Object> c = ev("COOLDOWN_STARTED"); c.put("seal", fight); c.put("length", m.group(5)); c.put("time", t); c.put("verified", true); out.accept(c);
                cooldownAnnounced.put(fight, now);
            }
            return;
        }
        if ((m = BT_DENY_CD.matcher(msg)).find()) {
            Map<String, Object> e = ev("TRIAL_FAILED"); e.put("fight", sealOfDisplay(m.group(1))); e.put("reason", "seal is on cooldown"); e.put("cooldown_left", m.group(2)); e.put("time", t); e.put("verified", true); out.accept(e); return;
        }
        if ((m = BT_BURROW.matcher(msg)).find()) {
            Map<String, Object> e = ev("TRIAL_ENDED"); e.put("fight", sealOfDisplay(m.group(1))); e.put("result", "the Warden burrowed away: the fight never came to it (no verdict, no cooldown)"); e.put("time", t); e.put("verified", true); out.accept(e); return;
        }
        if ((m = BT_CLOSED.matcher(msg)).find()) {
            Map<String, Object> e = ev("TRIAL_ENDED"); e.put("fight", sealOfDisplay(m.group(1))); e.put("result", "closed without a verdict: " + m.group(2) + " (no cooldown)"); e.put("time", t); e.put("verified", true); out.accept(e); return;
        }
        if ((m = BT_ELDER.matcher(msg)).find()) {
            Map<String, Object> e = ev("FIGHT_PROGRESS"); e.put("fight", "elder_guardian"); e.put("signal", "ELDER_TRIO"); e.put("elders_down", m.group(1) + "/3"); e.put("time", t); e.put("verified", true); out.accept(e);
        }
    }

    volatile long lastSealBrokeMs, lastAdminMs;
    /** seal -> when a COOLDOWN_STARTED was already sent from a verdict line (so the state poll does not repeat it). */
    final Map<String, Long> cooldownAnnounced = new java.util.concurrent.ConcurrentHashMap<>();

    /** sealwatch.py progress(): close fights idle for 60 min. */
    synchronized void sweep(long now, Consumer<Map<String, Object>> out) {
        for (Iterator<Map.Entry<String, Fight>> it = fights.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Fight> en = it.next();
            if (now - en.getValue().last > 3_600_000L) {
                Map<String, Object> e = ev("FIGHT_END"); e.put("fight", en.getKey()); e.put("result", "no seal after 60 min of no boss activity (lost/abandoned?)"); e.put("players", new ArrayList<>(en.getValue().players));
                out.accept(e); it.remove();
            }
        }
        ended.values().removeIf(v -> now - v > 3_600_000L);
    }
    synchronized List<String> openFights() { return new ArrayList<>(fights.keySet()); }
}
