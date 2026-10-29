/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Backstop diff of the seal plugins' saved state (read off-thread every
 * statePollSeconds): GuildGate plugins/ExcelGuildGate/state.yml (seals.*.broken)
 * and BossTrial plugins/ExcelBossTrial/state.yml (cooldowns.<seal>: untilMs).
 * Catches anything the console taps missed and is the source of
 * COOLDOWN_STARTED (when no verdict line announced it) and COOLDOWN_ENDED.
 * Pure Java, no Bukkit. First observation sets the baseline silently.
 */
final class StateWatch {
    static final class Snapshot {
        final Map<String, String> broken; final Map<String, Long> cooldowns; final int total;
        Snapshot(Map<String, String> broken, Map<String, Long> cooldowns, int total) { this.broken = broken; this.cooldowns = cooldowns; this.total = total; }
    }
    private Set<String> lastBroken; private Set<String> lastCooling;
    volatile Snapshot latest;
    private volatile Names names; private final ZoneId zone;
    /** /sealalerts reload: a players.mode change applies to the next event. */
    void names(Names n) { this.names = n; }
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("h:mm a z MMM d", Locale.US);

    StateWatch(Names names, ZoneId zone) { this.names = names; this.zone = zone; }

    /** Restore the baseline saved at the last shutdown (null = none). */
    synchronized void baseline(Collection<String> broken, Collection<String> cooling) {
        lastBroken = broken == null ? null : new TreeSet<>(broken);
        lastCooling = cooling == null ? null : new TreeSet<>(cooling);
    }
    synchronized Set<String> brokenNow() { return lastBroken == null ? Set.of() : new TreeSet<>(lastBroken); }
    synchronized Set<String> coolingNow() { return lastCooling == null ? Set.of() : new TreeSet<>(lastCooling); }

    /**
     * @param s current files (broken/cooldowns may be null when a file is missing or unreadable: that side is skipped)
     * @param recentSealBroke true when a verified SEAL_BROKE was sent in the last few minutes
     * @param recentAdmin true when an ADMIN_SEAL_CHANGE line was seen in the last few minutes
     */
    synchronized void observe(Snapshot s, long now, boolean recentSealBroke, boolean recentAdmin,
                              Map<String, Long> cooldownAnnounced, boolean whileOff, Consumer<Map<String, Object>> out) {
        latest = s;
        if (s.broken != null) {
            Set<String> cur = new TreeSet<>(s.broken.keySet());
            if (lastBroken != null && !cur.equals(lastBroken)) {
                for (String id : cur) if (!lastBroken.contains(id) && !recentSealBroke && !recentAdmin) {
                    Map<String, Object> e = new LinkedHashMap<>(); e.put("type", "SEAL_BROKE"); e.put("seal", id);
                    String k = s.broken.get(id); e.put("killer", k == null ? null : names.name(k));
                    e.put("count", cur.size() + "/" + s.total); e.put("source", "state.yml");
                    if (whileOff) e.put("note", "changed while the server was off");
                    out.accept(e);
                }
                for (String id : lastBroken) if (!cur.contains(id) && !recentAdmin) {
                    Map<String, Object> e = new LinkedHashMap<>(); e.put("type", "ADMIN_SEAL_CHANGE");
                    e.put("detail", "seal " + id + " is no longer broken (" + cur.size() + " of " + s.total + ")" + (whileOff ? ", changed while the server was off" : ""));
                    e.put("source", "state.yml"); out.accept(e);
                }
            }
            lastBroken = cur;
        }
        if (s.cooldowns != null) {
            Set<String> cool = new TreeSet<>();
            s.cooldowns.forEach((k, until) -> { if (until != null && until > now) cool.add(SealParser.fightKey(k)); });
            if (lastCooling != null) {
                for (String id : cool) if (!lastCooling.contains(id)) {
                    Long ann = cooldownAnnounced.get(id);
                    if (ann != null && now - ann < 600_000L) continue; // the verdict line already sent COOLDOWN_STARTED
                    Map<String, Object> e = new LinkedHashMap<>(); e.put("type", "COOLDOWN_STARTED"); e.put("seal", id);
                    e.put("until", until(s.cooldowns, id)); e.put("source", "state.yml"); out.accept(e);
                }
                for (String id : lastCooling) if (!cool.contains(id)) {
                    Map<String, Object> e = new LinkedHashMap<>(); e.put("type", "COOLDOWN_ENDED"); e.put("seal", id);
                    e.put("detail", "the " + id + " seal can be earned again"); e.put("source", "state.yml"); out.accept(e);
                }
            }
            lastCooling = cool;
        }
    }
    private String until(Map<String, Long> cds, String fightId) {
        for (Map.Entry<String, Long> e : cds.entrySet())
            if (SealParser.fightKey(e.getKey()).equals(fightId) && e.getValue() != null)
                return Instant.ofEpochMilli(e.getValue()).atZone(zone).format(WHEN);
        return null;
    }
    static String left(long ms) {
        long m = Math.max(0, ms) / 60_000L; return (m / 60) + "h " + (m % 60) + "m";
    }
}
