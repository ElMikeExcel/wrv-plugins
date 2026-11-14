package com.winkreyes.exceleitemobsabilitybedrockbridge;

import java.util.List;

/**
 * The console-admin half of /abilities (0.1.1, Mike's Nov 3, 2026
 * console-admin rule; the console-admin coverage audit of Nov 9,
 * 2026 found 0.1.0 a FAIL because /abilities was player-only, so
 * an operator could not inspect the bridge's link state from the
 * console):
 *
 *   /abilities status
 *
 * gated by the exceleitemobsabilitybedrockbridge.admin permission (default:
 * op). The server console is an admin by construction (it
 * satisfies op-default permissions in Bukkit/Paper, and the glue
 * also lets a console sender through explicitly, the
 * Landmarks/Cosmetics shape), so the form runs from the
 * Shockbyte console.
 *
 * The bridge holds NO per-player state of its own — class and
 * ability state lives in EliteMobs — so a global status is the
 * whole passing surface, exactly as the audit sized it: it
 * reports the EliteMobs link (plugin present/enabled, its
 * version, and whether the Advanced Combat System answers
 * through the bridge's own gateway seam) and the Geyser link
 * (Geyser-Spigot and floodgate present/enabled, the Bedrock
 * form transport). READ-ONLY by construction: it probes state
 * and fires nothing.
 *
 * No Bukkit dependencies: the gate arithmetic and the exact
 * reply texts live here and are unit-tested on a plain JVM; the
 * glue only gathers the live facts and delivers the lines.
 */
public final class StatusCommand {

    /** The admin permission node (plugin.yml, default: op). */
    public static final String PERMISSION = "exceleitemobsabilitybedrockbridge.admin";

    /** Sent to a non-console sender who lacks the admin node. */
    public static final String DENIED =
            "You do not have permission to view ExcelEliteMobsAbilityBedrockBridge status (" + PERMISSION + ").";

    private StatusCommand() {}

    /**
     * The admin gate: the console always administers (it is the
     * operator channel this surface exists for); any other sender
     * needs the permission node (op by default).
     */
    public static boolean isAdmin(boolean senderIsConsole, boolean hasPermission) {
        return senderIsConsole || hasPermission;
    }

    /** True only for the exact status subcommand (case-insensitive). */
    public static boolean isStatus(String arg) {
        return arg != null && arg.trim().equalsIgnoreCase("status");
    }

    /**
     * The status lines, built from live facts the glue gathers:
     * the EliteMobs plugin's presence and version, whether the
     * bridge's gateway reports the ACS module ready, and the
     * Geyser/Floodgate presence. Pure formatting — every fact is
     * passed in, so the text can never claim a link state the
     * glue did not observe.
     */
    public static List<String> statusLines(String version, boolean eliteMobsEnabled,
                                           String eliteMobsVersion, boolean acsReady,
                                           boolean geyserEnabled, boolean floodgateEnabled,
                                           boolean debugLogFires) {
        String em;
        if (!eliteMobsEnabled) {
            em = "EliteMobs link: DOWN - EliteMobs not found or not enabled "
                    + "(the bridge stands down in this state).";
        } else if (acsReady) {
            em = "EliteMobs link: UP - EliteMobs " + orUnknown(eliteMobsVersion)
                    + " enabled, Advanced Combat System READY.";
        } else {
            em = "EliteMobs link: STANDING BY - EliteMobs " + orUnknown(eliteMobsVersion)
                    + " enabled, Advanced Combat System not ready yet "
                    + "(the bridge comes online automatically when ACS is up).";
        }
        String geyser = "Geyser link: Geyser-Spigot " + (geyserEnabled ? "enabled" : "NOT enabled")
                + ", floodgate " + (floodgateEnabled ? "enabled" : "NOT enabled")
                + " - Bedrock form transport " + ((geyserEnabled && floodgateEnabled)
                        ? "available." : "incomplete; Bedrock players may be unable to open the form.");
        return List.of(
                "ExcelEliteMobsAbilityBedrockBridge status: enabled (v" + version + ")",
                em,
                geyser,
                "Config: debug-log-fires " + (debugLogFires ? "ON" : "OFF")
                        + ". Permissions: " + PERMISSION + " (default op) for this status; "
                        + "/abilities itself stays open to every player.");
    }

    private static String orUnknown(String version) {
        return (version == null || version.isBlank()) ? "(unknown version)" : version;
    }
}
