package com.winkreyes.excelautosort.hook;

import java.util.Map;

/**
 * The ExcelProtect seam (SPEC §6.3), BossTrial-style: plugin code talks
 * only to this interface, in plain JDK types. The one implementation
 * that touches Protect's API lives in the {@code protect} package and
 * is bound only when ExcelProtect is present and enabled.
 *
 * Contract for every implementation: NEVER throws, NEVER blocks the
 * action that produced the record. {@link #NOOP} is the standing sink
 * whenever the adapter is not bound — the buffer remains the record of
 * the removal itself, and the console says the sink is unbound, loudly,
 * once (SPEC §6.3, §11 T10).
 */
public interface ProtectSink {

    /** Publish one record to Protect's {@code items} stream. */
    void record(String type, Map<String, Object> fields);

    /** Whether a real Protect adapter is bound (for the enable line). */
    boolean bound();

    ProtectSink NOOP = new ProtectSink() {
        @Override public void record(String type, Map<String, Object> fields) {}
        @Override public boolean bound() { return false; }
    };
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
