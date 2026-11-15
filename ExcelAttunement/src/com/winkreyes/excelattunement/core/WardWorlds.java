package com.winkreyes.excelattunement.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 0.1.5 guild ward: which worlds silence Magic (Mike, Oct 24, 2026).
 * Pure logic, no Bukkit. A world is warded when EliteMobs' own world
 * registry says so (looked up by the Bukkit glue, read-only), OR its
 * name starts with a configured prefix (default {@code em_}), OR it
 * matches a configured exact name (default {@code GoblinKingdom_vf},
 * the one premium pack world without the em_ prefix).
 * Name matches are case-insensitive. Instanced dungeon copies
 * ({@code em_id_x_3}) are caught by the prefix even when the registry
 * is not available.
 */
public final class WardWorlds {
    public enum Reason { NONE, ELITEMOBS_REGISTRY, PREFIX, NAME }

    private final List<String> prefixes = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    public WardWorlds(Collection<String> prefixes, Collection<String> names) {
        if (prefixes != null) for (String p : prefixes) {
            if (p != null && !p.isBlank()) this.prefixes.add(p.trim().toLowerCase(Locale.ROOT));
        }
        if (names != null) for (String n : names) {
            if (n != null && !n.isBlank()) this.names.add(n.trim().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * @param worldName  the Bukkit world name (null = not warded)
     * @param emRegistry EliteMobs' answer: TRUE / FALSE, or null when the
     *                   registry is unavailable (EliteMobs missing, class
     *                   moved, lookup threw). The registry can only ADD a
     *                   world; FALSE never overrides a config match.
     */
    public Reason reason(String worldName, Boolean emRegistry) {
        if (Boolean.TRUE.equals(emRegistry)) return Reason.ELITEMOBS_REGISTRY;
        if (worldName == null) return Reason.NONE;
        String w = worldName.toLowerCase(Locale.ROOT);
        for (String n : names) if (w.equals(n)) return Reason.NAME;
        for (String p : prefixes) if (w.startsWith(p)) return Reason.PREFIX;
        return Reason.NONE;
    }

    public boolean isWarded(String worldName, Boolean emRegistry) {
        return reason(worldName, emRegistry) != Reason.NONE;
    }

    public List<String> prefixes() { return List.copyOf(prefixes); }

    public List<String> names() { return List.copyOf(names); }
}
