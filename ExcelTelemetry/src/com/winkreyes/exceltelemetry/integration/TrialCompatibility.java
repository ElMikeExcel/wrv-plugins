package com.winkreyes.exceltelemetry.integration;

import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/** Observation-only compatibility decisions; no commands or gameplay mutations. */
public final class TrialCompatibility {
    private TrialCompatibility() {}
    public static String provider(boolean bossTrialEnabled, boolean legacyEnabled) {
        return bossTrialEnabled ? "ExcelBossTrial" : legacyEnabled ? "ExcelDragonTrial" : null;
    }
    public static boolean chapterActive(boolean rootEnabled, boolean chapterEnabled) {
        return rootEnabled && chapterEnabled;
    }
    public static String statePath(String provider) {
        return "ExcelBossTrial".equals(provider) ? "dragon/state.yml" : "state.yml";
    }
    public static String phase(double max, double p1, double p2) {
        boolean one = Double.isFinite(p1) && p1 > 0 && Math.abs(max - p1) <= 15;
        boolean two = Double.isFinite(p2) && p2 > 0 && Math.abs(max - p2) <= 15;
        // Ambiguous signatures must not invent a phase.
        return one == two ? "unknown" : one ? "phase1" : "phase2";
    }
    public static String savedPhase(YamlConfiguration state, String world, UUID dragon) {
        if (state == null || world == null || dragon == null) return null;
        // Section lookup avoids interpreting a dot in a world name as a YAML path.
        var worlds = state.getConfigurationSection("worlds");
        if (worlds == null) return null;
        Object raw = worlds.getValues(false).get(world);
        if (!(raw instanceof org.bukkit.configuration.ConfigurationSection section)) return null;
        String recorded = section.getString("dragon-uuid");
        if (!dragon.toString().equals(recorded)) return null;
        String phase = section.getString("phase");
        return "PHASE1".equals(phase) || "PHASE2".equals(phase) || "TRANSITION".equals(phase)
                ? phase : null;
    }
    public static boolean isUnload(String cause) {
        return "UNLOAD".equals(cause) || "CHUNK_UNLOAD".equals(cause);
    }
}
