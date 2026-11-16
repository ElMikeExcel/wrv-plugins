/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;

/**
 * The BossTrial side of the ExcelProtect emitter seam
 * (ExcelBossTrial 0.3.4). The host talks ONLY to this interface,
 * in plain JDK types; the one implementation that touches
 * ExcelProtect's API classes lives in the {@code protect}
 * package and is instantiated only when the ExcelProtect plugin
 * is present and enabled (the SealCreditHook isolation pattern -
 * BossTrial's own classes must load cleanly with Protect absent).
 *
 * Contract for every implementation: NEVER throws, NEVER blocks
 * a fight. A failed publish is a lost record line, nothing more;
 * the fight record in fights/ remains the encounter's own proof.
 * {@link #NOOP} is the standing sink whenever the adapter is not
 * bound (Protect absent, disabled by config, or bind failure).
 */
public interface ProtectEmitSink {

    /** Publish one verdict fight table (Protect writes its
     *  combat fight_summary + audit seal_verdict from this). */
    void fightTable(String encounter, String verdict, String credit, double pool,
                    double durationSeconds, List<Map<String, Object>> rows);

    /** Publish one general record through Protect's publish API
     *  (stream is a Protect stream name, e.g. "combat"). */
    void record(String stream, String type, Map<String, Object> fields);

    /** The sink that drops everything: the adapter-off state. */
    ProtectEmitSink NOOP = new ProtectEmitSink() {
        @Override
        public void fightTable(String encounter, String verdict, String credit,
                               double pool, double durationSeconds,
                               List<Map<String, Object>> rows) {
        }

        @Override
        public void record(String stream, String type, Map<String, Object> fields) {
        }
    };
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
