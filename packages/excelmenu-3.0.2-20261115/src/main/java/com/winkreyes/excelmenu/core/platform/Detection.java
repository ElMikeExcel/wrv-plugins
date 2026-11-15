// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.platform;

import java.util.UUID;

/**
 * The unified platform bridge's decision core (DESIGN-2.0 §2.2).
 *
 * One detector answers one question — is this connection Bedrock? —
 * and the answer is TRI-STATE: BEDROCK, JAVA, or UNKNOWN. UNKNOWN is
 * the honest third state the two old bridges could not express (both
 * collapsed every failure to "Java"); 2.0 keeps it distinct so a
 * broken bridge is visible in logs, while the RENDER decision still
 * fails safe: UNKNOWN renders the Java chest GUI, which Geyser
 * translates for a Bedrock player anyway. A detection failure degrades
 * the presentation, never strands a player without a menu.
 *
 * This class is deliberately platform-free (no Bukkit): the Bukkit
 * layer supplies a {@link Detector} backed by Geyser reflection, and
 * the unit tests supply fakes. Detection is asked per menu open,
 * never cached at join.
 */
public final class Detection {

    public enum State { BEDROCK, JAVA, UNKNOWN }

    /** What the renderer should serve for a detection state. */
    public enum RenderTarget { FORM, CHEST }

    /**
     * The raw bridge probe. Implementations return Boolean.TRUE for a
     * Bedrock connection, Boolean.FALSE for a confirmed Java
     * connection, and signal "cannot tell" by returning null or
     * throwing — both become UNKNOWN here, never an exception out.
     */
    @FunctionalInterface
    public interface Detector {
        Boolean probe(UUID playerId) throws Exception;
    }

    private Detection() {}

    public static State detect(Detector detector, UUID playerId) {
        if (detector == null || playerId == null) return State.UNKNOWN;
        try {
            Boolean result = detector.probe(playerId);
            if (result == null) return State.UNKNOWN;
            return result ? State.BEDROCK : State.JAVA;
        } catch (Exception | LinkageError e) {
            return State.UNKNOWN;
        }
    }

    /** BEDROCK → Cumulus form; JAVA → chest; UNKNOWN → chest (fail-safe). */
    public static RenderTarget renderTarget(State state) {
        return state == State.BEDROCK ? RenderTarget.FORM : RenderTarget.CHEST;
    }

    /**
     * The boot self-check verdict line (DESIGN-2.0 §2.2: "a failed
     * check warns at boot instead of surprising at first tap"). The
     * Bukkit bridge reports whether the reflective Geyser surface was
     * found; the wording lives here so it is testable and identical
     * everywhere it is printed.
     */
    public static String selfCheckLine(boolean reflectiveSurfacePresent) {
        return reflectiveSurfacePresent
                ? "ExcelMenu platform bridge self-check: Geyser reflection surface PRESENT"
                        + " — Bedrock detection live; fallback is the chest GUI."
                : "ExcelMenu platform bridge self-check: Geyser reflection surface ABSENT"
                        + " — Bedrock detection UNKNOWN for every viewer; all menus render"
                        + " as chest GUIs (Geyser translates them). This is expected when"
                        + " Geyser is not installed.";
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
