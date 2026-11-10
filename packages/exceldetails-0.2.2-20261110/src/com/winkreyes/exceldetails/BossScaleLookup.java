package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 0.2.2 boss scaling — the seam half. Supplies the two live
 * values BossScale's arithmetic needs, from ExcelBossTrial
 * itself, so the meter's boss numbers track BossTrial's
 * pop-ups rather than a copied constant:
 *
 *   Display scale — BossTrial's own live config value
 *     (feedback.display-scale), read through the public Bukkit
 *     Plugin.getConfig() API. No reflection; a BossTrial
 *     config reload is picked up within the cache TTL.
 *   Intake factor — per boss, from BossTrial's live encounter
 *     state by reflection (the PartyLookup pattern): plugin
 *     field `cfg` (BossTrialConfig; its intakeEntityCap is a
 *     public double field) + field `encounters`
 *     (Map<UUID, EncounterLedger>) -> EncounterLedger
 *     .maxHealth() (public). f = BossScale.intakeFactor(cap,
 *     pool). Verified against the shipped 0.5.3 source in the
 *     records package (excelbosstrial-0.5.3-20261109).
 *
 * SOFT SEAM, house discipline: handles resolve lazily and are
 * cached; the bind outcome is logged ONCE (BOUND / UNBOUND);
 * every failure fails soft — BossTrial absent means "not a
 * governed boss fight" (no scaling at all), an unbound intake
 * seam means f = 1 (exact for sub-cap chapters; over-cap
 * bosses read low by f, never high, never junk).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class BossScaleLookup {

    private static final long SCALE_CACHE_MS = 5_000L;

    private final Logger log;

    private double cachedScale = BossScale.DEFAULT_SCALE;
    private long cachedScaleAtMs = Long.MIN_VALUE;

    // Intake seam handles (null until bound). Fields (not values)
    // are cached, and cfg / encounters are re-read through them
    // per call, so a BossTrial reload that reassigns either one
    // is picked up instead of going stale.
    private Plugin bossTrialPlugin;
    private Field cfgField;
    private Field encountersField;
    private Field intakeCapField;
    private Method ledgerMaxHealth;
    private boolean intakeBound;
    private boolean intakeLogged;
    private boolean callWarned;

    public BossScaleLookup(Logger log) { this.log = log; }

    /** True while ExcelBossTrial is present — the master gate
     *  for scaling (a vanilla boss fought without BossTrial
     *  keeps true-HP numbers). */
    public boolean bossTrialPresent() {
        Plugin bt = Bukkit.getPluginManager().getPlugin("ExcelBossTrial");
        return bt != null && bt.isEnabled();
    }

    /** BossTrial's live display scale, sanitized. Fail-soft:
     *  the settled default when BossTrial or its config value
     *  is unavailable. */
    public double displayScale() {
        long now = System.currentTimeMillis();
        if (now - cachedScaleAtMs < SCALE_CACHE_MS) return cachedScale;
        cachedScaleAtMs = now;
        try {
            Plugin bt = Bukkit.getPluginManager().getPlugin("ExcelBossTrial");
            if (bt == null) return cachedScale;
            double raw = bt.getConfig().getDouble("feedback.display-scale",
                    BossScale.DEFAULT_SCALE);
            cachedScale = BossScale.sanitizedScale(raw);
        } catch (Exception e) {
            warnOnce("display scale", e);
        }
        return cachedScale;
    }

    /** The live intake factor for one boss entity (1.0 when the
     *  boss has no open governed encounter, the chapter pool
     *  fits under the entity cap, or the seam is unbound). */
    public double intakeFactorFor(Entity boss) {
        if (boss == null) return 1.0;
        try {
            if (!intakeBound && !bindIntake()) return 1.0;
            Object cfg = cfgField.get(bossTrialPlugin);
            Object map = encountersField.get(bossTrialPlugin);
            if (cfg == null || !(map instanceof Map<?, ?> encounters)) return 1.0;
            Object ledger = encounters.get(boss.getUniqueId());
            if (ledger == null) return 1.0;
            double pool = (double) ledgerMaxHealth.invoke(ledger);
            double cap = intakeCapField.getDouble(cfg);
            return BossScale.intakeFactor(cap, pool);
        } catch (Exception e) {
            warnOnce("intake factor", e);
            return 1.0;
        }
    }

    private boolean bindIntake() {
        Plugin bt = Bukkit.getPluginManager().getPlugin("ExcelBossTrial");
        if (bt == null) { logOnce(false, "plugin absent"); return false; }
        try {
            cfgField = bt.getClass().getDeclaredField("cfg");
            cfgField.setAccessible(true);
            encountersField = bt.getClass().getDeclaredField("encounters");
            encountersField.setAccessible(true);
            Object cfg = cfgField.get(bt);
            Object map = encountersField.get(bt);
            if (cfg == null || !(map instanceof Map)) {
                logOnce(false, "cfg/encounters not ready");
                return false;
            }
            intakeCapField = cfg.getClass().getField("intakeEntityCap");
            Class<?> ledgerClass = Class.forName(
                    "com.winkreyes.excelbosstrial.core.EncounterLedger",
                    true, bt.getClass().getClassLoader());
            ledgerMaxHealth = ledgerClass.getMethod("maxHealth");
            bossTrialPlugin = bt;
            intakeBound = true;
            logOnce(true, "cfg.intakeEntityCap + encounters + EncounterLedger.maxHealth");
            return true;
        } catch (Exception e) {
            logOnce(false, e.toString());
            return false;
        }
    }

    private void logOnce(boolean bound, String detail) {
        if (intakeLogged) return;
        intakeLogged = true;
        log.info(bound
                ? "ExcelDetails: BossTrial intake seam BOUND (" + detail + ") — boss numbers recover the live intake factor."
                : "ExcelDetails: BossTrial intake seam UNBOUND (" + detail + ") — boss scaling uses f=1 (exact for sub-cap chapters).");
    }

    private void warnOnce(String what, Exception e) {
        if (callWarned) return;
        callWarned = true;
        log.warning("ExcelDetails: BossTrial " + what + " lookup failed once (" + e
                + ") — failing soft; boss scaling never amplifies on error.");
    }
}
