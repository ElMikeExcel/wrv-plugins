package com.winkreyes.excelautosort.protect;

import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.hook.ProtectSink;
import com.winkreyes.excelprotect.api.ExcelProtectAPI;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The ExcelProtect adapter — the ONLY class in ExcelAutoSort that
 * touches Protect's API. Instantiated by the host only when the
 * ExcelProtect plugin is present and enabled, so the rest of the
 * plugin loads and runs cleanly with Protect absent (the isolation
 * pattern). Fail-soft end to end: a failed publish is a lost record
 * line — at most three warnings per boot — never a broken sort or a
 * lost item; the recovery buffer remains the removal's own record.
 */
public final class ProtectGateway implements ProtectSink {

    private final Logger log;
    private int failures;

    public ProtectGateway(Logger log) {
        this.log = log;
    }

    @Override
    public void record(String type, Map<String, Object> fields) {
        try {
            ExcelProtectAPI.publish(ProtectEvents.STREAM, type, fields);
        } catch (Throwable t) {
            failures++;
            if (failures <= 3) {
                log.warning("ExcelAutoSort: a record (" + type + ") was not published to ExcelProtect ("
                        + t + ") — the removal itself and its buffer entry are unaffected.");
            }
        }
    }

    @Override
    public boolean bound() {
        return true;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
