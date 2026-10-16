package com.winkreyes.exceltogether;

import java.util.*;

/**
 * Finds a player from what someone typed. Handles Floodgate dot prefixes (".coolgirlkail" can be typed as
 * "coolgirlkail"), nicknames with color codes, case, and unique prefixes. Pure logic, no Bukkit.
 */
public final class NameMatch {
    private NameMatch() {}

    public static final class Cand {
        public final UUID id; public final String real; public final String nick;
        public Cand(UUID id, String real, String nick) { this.id = id; this.real = real == null ? "" : real; this.nick = nick == null ? "" : nick; }
    }
    public static final class Result {
        public final Cand found; public final List<Cand> ambiguous;
        Result(Cand f, List<Cand> a) { found = f; ambiguous = a; }
        public boolean isFound() { return found != null; }
        public boolean isAmbiguous() { return found == null && !ambiguous.isEmpty(); }
    }

    /** Lowercase, color codes removed, leading "." "*" "~" removed, trimmed. */
    public static String norm(String s) {
        if (s == null) return "";
        String x = s.replaceAll("(?i)[\u00a7&][0-9a-fk-or]", "").trim().toLowerCase(Locale.ROOT);
        while (x.startsWith(".") || x.startsWith("*") || x.startsWith("~")) x = x.substring(1);
        return x;
    }

    public static Result find(String query, Collection<Cand> cands) {
        if (query == null || query.trim().isEmpty()) return new Result(null, List.of());
        String raw = query.trim().replaceAll("(?i)[\u00a7&][0-9a-fk-or]", "").toLowerCase(Locale.ROOT);
        String q = norm(query);
        if (q.isEmpty()) return new Result(null, List.of());
        // 1 exact real name, dot included (".kail" only matches the Bedrock "Kail")
        Result r = tier(cands, c -> c.real.toLowerCase(Locale.ROOT).equals(raw));
        if (r != null) return r;
        // 2 exact real name without dot, 3 exact nickname
        r = tier(cands, c -> norm(c.real).equals(q));
        if (r != null) return r;
        r = tier(cands, c -> norm(c.nick).equals(q));
        if (r != null) return r;
        // 4 unique prefix of real name or nickname
        r = tier(cands, c -> norm(c.real).startsWith(q) || norm(c.nick).startsWith(q));
        return r != null ? r : new Result(null, List.of());
    }

    private static Result tier(Collection<Cand> cands, java.util.function.Predicate<Cand> p) {
        List<Cand> hit = new ArrayList<>();
        for (Cand c : cands) if (p.test(c)) hit.add(c);
        if (hit.isEmpty()) return null;
        if (hit.size() == 1) return new Result(hit.get(0), List.of());
        return new Result(null, hit);
    }
}
