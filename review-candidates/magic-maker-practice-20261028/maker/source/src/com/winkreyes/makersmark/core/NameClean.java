package com.winkreyes.makersmark.core;

import java.util.List;

/** Turns a nickname or username into the plain name shown on a maker's mark. */
public final class NameClean {
    private NameClean() {}

    /** Removes legacy color codes (section sign or ampersand), hex codes and MiniMessage-like tags. */
    public static String stripColors(String s) {
        if (s == null) return "";
        String out = s.replaceAll("(?i)[\u00a7&]x([\u00a7&][0-9a-f]){6}", "");
        out = out.replaceAll("(?i)[\u00a7&][0-9a-fk-or]", "");
        out = out.replaceAll("(?i)<#[0-9a-f]{6}>", "");
        return out;
    }

    /**
     * nick = Essentials nickname (may be null), username = account name (Floodgate names start with '.').
     * Prefixes are stripped repeatedly ("~.Name" becomes "Name"); falls back to the username, then "Someone".
     */
    public static String clean(String nick, String username, List<String> prefixes, int maxLen) {
        String n = tidy(nick, prefixes);
        if (n.isEmpty()) n = tidy(username, prefixes);
        if (n.isEmpty()) n = "Someone";
        if (maxLen > 0 && n.length() > maxLen) n = n.substring(0, maxLen).trim();
        return n;
    }

    private static String tidy(String s, List<String> prefixes) {
        if (s == null) return "";
        String n = stripColors(s).replaceAll("[\\p{Cntrl}]", "").trim();
        boolean changed = true;
        while (changed && !n.isEmpty()) {
            changed = false;
            for (String p : prefixes) {
                if (p != null && !p.isEmpty() && n.startsWith(p)) { n = n.substring(p.length()).trim(); changed = true; }
            }
        }
        return n;
    }
}
