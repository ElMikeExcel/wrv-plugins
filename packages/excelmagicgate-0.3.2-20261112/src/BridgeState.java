package com.winkreyes.excelmagicgate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ExcelMagicGate's persisted state as a pure value with a tolerant
 * map codec (no Bukkit deps). The plugin class owns the actual
 * state.yml file handling; this class owns the fields and the
 * parse/serialize rules so they are unit-testable.
 *
 * Fields:
 * - appliedStage: the Magic stage currently live (0 = dormant). Only
 *   ever moves up, and only after a full successful apply.
 * - lastKnownSeals: the last GuildGate broken-seal count observed
 *   (-1 = never observed). Used for change logging and revoke
 *   detection; eligibility itself is always computed fresh.
 * - downgradeWarnedFor: the appliedStage value a revoke warning was
 *   last issued for (-1 = none), so a standing revoke warns once per
 *   stage instead of every watch tick.
 * - lastApplyStage / lastApplyResult: a one-line memory of the most
 *   recent apply attempt, for /magicgate status.
 */
public final class BridgeState {

    private int appliedStage = 0;
    private int lastKnownSeals = -1;
    private int downgradeWarnedFor = -1;
    private int lastApplyStage = -1;
    private String lastApplyResult = "";

    public int appliedStage() {
        return appliedStage;
    }

    public void setAppliedStage(int stage) {
        this.appliedStage = StagePlan.clampStage(stage);
    }

    public int lastKnownSeals() {
        return lastKnownSeals;
    }

    public void setLastKnownSeals(int seals) {
        this.lastKnownSeals = Math.max(-1, Math.min(StagePlan.MAX_STAGE, seals));
    }

    public int downgradeWarnedFor() {
        return downgradeWarnedFor;
    }

    public void setDowngradeWarnedFor(int stage) {
        this.downgradeWarnedFor = stage;
    }

    public int lastApplyStage() {
        return lastApplyStage;
    }

    public String lastApplyResult() {
        return lastApplyResult;
    }

    public void recordApply(int stage, String result) {
        this.lastApplyStage = stage;
        this.lastApplyResult = result == null ? "" : result;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appliedStage", appliedStage);
        m.put("lastKnownSeals", lastKnownSeals);
        m.put("downgradeWarnedFor", downgradeWarnedFor);
        m.put("lastApplyStage", lastApplyStage);
        m.put("lastApplyResult", lastApplyResult);
        return m;
    }

    /** Tolerant restore: missing or garbage values fall back to defaults. */
    public static BridgeState fromMap(Map<String, Object> m) {
        BridgeState s = new BridgeState();
        if (m == null) {
            return s;
        }
        s.appliedStage = StagePlan.clampStage(asInt(m.get("appliedStage"), 0));
        s.lastKnownSeals = Math.max(-1, Math.min(StagePlan.MAX_STAGE, asInt(m.get("lastKnownSeals"), -1)));
        s.downgradeWarnedFor = asInt(m.get("downgradeWarnedFor"), -1);
        s.lastApplyStage = asInt(m.get("lastApplyStage"), -1);
        Object r = m.get("lastApplyResult");
        s.lastApplyResult = r == null ? "" : String.valueOf(r);
        return s;
    }

    private static int asInt(Object v, int fallback) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }
}
