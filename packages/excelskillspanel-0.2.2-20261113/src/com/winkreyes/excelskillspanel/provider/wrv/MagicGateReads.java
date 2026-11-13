package com.winkreyes.excelskillspanel.provider.wrv;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The rank-driving credited-cast count for the Magic header's
 * "Credited casts: N" line (0.2.2, Item 13). The number lives in
 * ExcelMagicGate's ledger — NOT in ExcelMagicLevel's store, whose
 * PlayerRecord.totalCasts (694 for Mike) is a different counter
 * with a different window, and is never read here again.
 *
 * Read order (first success wins; any failure omits, never
 * substitutes):
 * 1) the LIVE in-memory ledger, reflectively: plugin
 *    ExcelMagicGate -> public earnedStore() -> public
 *    creditedTotal(String uuid) (the same reflective family as
 *    the GuildGate pattern — no compile dependency on
 *    MagicGate);
 * 2) MagicGate's persisted earned.yml in its data folder
 *    (the EarnedStore codec: players.&lt;uuid&gt;.credited_total;
 *    at most one write-behind stale);
 * 3) the resolved wand's own lore line "Credited casts: N"
 *    (the number MagicGate itself writes on the wand the player
 *    sees — parseCreditedFromLore is pure and harness-tested).
 *
 * When MagicGate cannot answer at all, the casts line is
 * omitted — the panel never falls back to ExcelMagicLevel's
 * totalCasts. A different number under any label would
 * reintroduce the 694-vs-1,215 confusion this build retires.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicGateReads {
    private MagicGateReads() {}

    /** Credited casts for a player, or null when no read succeeds. */
    public static Long creditedCasts(UUID uuid, Logger log, List<String> wandLore) {
        Plugin gate = gatePlugin();
        if (gate != null) {
            Long live = liveLedgerCasts(gate, uuid);
            if (live != null) return live;
            Long file = fileLedgerCasts(gate, uuid, log);
            if (file != null) return file;
        }
        Long lore = parseCreditedFromLore(wandLore);
        if (lore != null) return lore;
        if (gate != null) {
            log.warning("ExcelSkillsPanel: MagicGate credited casts unreadable for "
                    + uuid + " (live store + earned.yml + wand lore all missed)"
                    + " — casts line omitted");
        }
        return null;
    }

    /** The wand lore's "Credited casts: N" line, parsed. Pure:
     *  strip legacy codes, find the line, parse the number
     *  (thousands commas tolerated). Null when no such line. */
    public static Long parseCreditedFromLore(List<String> lore) {
        if (lore == null) return null;
        for (String raw : lore) {
            if (raw == null) continue;
            String line = stripLegacy(raw).trim();
            int at = line.indexOf("Credited casts:");
            if (at < 0) continue;
            String number = line.substring(at + "Credited casts:".length())
                    .trim().replace(",", "").replace(" ", "");
            if (number.isEmpty()) continue;
            try {
                long value = Long.parseLong(number);
                return value < 0 ? null : value;
            } catch (NumberFormatException ignored) {
                // a malformed count on the wand is not a count
            }
        }
        return null;
    }

    /** Reflective read of the live in-memory ledger. Any mismatch
     *  (a future MagicGate reshaping its internals) falls back
     *  to the persisted file. */
    private static Long liveLedgerCasts(Plugin gate, UUID uuid) {
        try {
            Method earnedStore = gate.getClass().getMethod("earnedStore");
            Object store = earnedStore.invoke(gate);
            if (store == null) return null;
            Method creditedTotal = store.getClass().getMethod("creditedTotal", String.class);
            Object value = creditedTotal.invoke(store, uuid.toString());
            return value instanceof Number ? ((Number) value).longValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The persisted ledger (EarnedStore codec). */
    private static Long fileLedgerCasts(Plugin gate, UUID uuid, Logger log) {
        try {
            File file = new File(gate.getDataFolder(), "earned.yml");
            if (!file.isFile()) return null;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String path = "players." + uuid + ".credited_total";
            return yaml.contains(path) ? yaml.getLong(path) : null;
        } catch (Throwable t) {
            log.warning("ExcelSkillsPanel: ExcelMagicGate earned.yml casts read failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage()
                    + " — casts line omitted");
            return null;
        }
    }

    /** Remove legacy (§) formatting codes. */
    static String stripLegacy(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) { i++; continue; }
            out.append(c);
        }
        return out.toString();
    }

    private static Plugin gatePlugin() {
        Plugin gate = Bukkit.getPluginManager().getPlugin("ExcelMagicGate");
        return gate != null && gate.isEnabled() ? gate : null;
    }
}
