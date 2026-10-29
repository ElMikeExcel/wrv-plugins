/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** How player names appear in posts: gamertag (as sealwatch.py), label, or hidden. */
final class Names {
    final String mode; final Map<String, String> labels;
    Names(String mode, Map<String, String> labels) {
        this.mode = mode == null ? "gamertag" : mode.toLowerCase(Locale.ROOT);
        Map<String, String> l = new HashMap<>();
        if (labels != null) labels.forEach((k, v) -> l.put(k.toLowerCase(Locale.ROOT), v));
        this.labels = l;
    }
    String name(String n) {
        if (n == null || n.isEmpty() || n.equals("-")) return n;
        switch (mode) {
            case "hidden": return "a player";
            case "label": {
                String v = labels.get(n.toLowerCase(Locale.ROOT));
                return v != null ? v : "Player-" + hash4(n);
            }
            default: return n;
        }
    }
    /** Comma list as SealLog writes it ("-" or "a,b"). */
    String list(String csv) {
        if (csv == null || csv.isEmpty() || csv.equals("-")) return csv;
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) out.add(name(s));
        return String.join(",", out);
    }
    static String hash4(String n) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(n.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return String.format("%02x%02x", d[0], d[1]);
        } catch (Exception e) { return "0000"; }
    }
}
