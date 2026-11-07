package com.winkreyes.excelclaims;

import com.winkreyes.excelprotect.api.ExcelProtectAPI;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ExcelClaims — the ExcelProtect sink, isolated in the one class that
 * touches Protect's API (house isolation pattern: instantiated only
 * when ExcelProtect is enabled, fail-soft end to end). Claim creates
 * and deletes do NOT go through here — Protect hears those itself
 * through its own claim-event adapter listening to our claim events.
 * This sink carries the one claim fact that has no event: trust
 * changes. A failed publish is a lost record line, never a failed
 * trust change. Excel — Iris / Mike.
 */
public final class ProtectGateway {

    private final Logger log;
    private int failures;

    public ProtectGateway(Logger log) {
        this.log = log;
    }

    public void trustChanged(String type, Map<String, Object> fields) {
        try {
            ExcelProtectAPI.publish("system", type, fields);
        } catch (Throwable t) {
            failures++;
            if (failures <= 3) {
                log.warning("ExcelClaims: a record (" + type + ") was not published to ExcelProtect ("
                        + t + ") — the trust change itself is unaffected.");
            }
        }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
