package com.winkreyes.excelguide.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Persistence for per-player Guide toggles (/guide off), in the same
 * tiny line format family as ExcelQuartermaster's TaleStore:
 *
 *   &lt;uuid&gt; off
 *   &lt;uuid&gt; on
 *
 * Only explicit states are stored. Serialization is sorted by UUID
 * string so the file is stable and diff-friendly; parsing is forgiving
 * (blank lines, comments, and malformed lines are skipped).
 */
public final class PlayerToggles {

    private PlayerToggles() {}

    /** Serialize the off-set (players NOT in the set default to on). */
    public static String serialize(Map<UUID, Boolean> offByPlayer) {
        if (offByPlayer == null || offByPlayer.isEmpty()) {
            return "";
        }
        TreeMap<String, Boolean> sorted = new TreeMap<>();
        for (Map.Entry<UUID, Boolean> e : offByPlayer.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                sorted.put(e.getKey().toString(), e.getValue());
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Boolean> e : sorted.entrySet()) {
            out.append(e.getKey()).append(' ').append(e.getValue() ? "off" : "on").append('\n');
        }
        return out.toString();
    }

    /** Parse the on-disk text form. Never throws on bad content. */
    public static Map<UUID, Boolean> parse(String text) {
        Map<UUID, Boolean> out = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 2) {
                continue;
            }
            try {
                UUID id = UUID.fromString(parts[0]);
                if (parts[1].equals("off")) {
                    out.put(id, Boolean.TRUE);
                } else if (parts[1].equals("on")) {
                    out.put(id, Boolean.FALSE);
                }
            } catch (IllegalArgumentException ignored) {
                // forgiving parse, same as the rest of the family
            }
        }
        return out;
    }
}
