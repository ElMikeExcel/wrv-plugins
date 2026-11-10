package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * 0.2.2 EliteMobs prep — the seam half. Asks EliteMobs whether
 * an entity is one of its tracked elites, by reflection, so
 * ExcelDetails keeps NO compile or load-order dependency on it:
 *
 *   EliteMobs 10.9.12 — com.magmaguy.elitemobs.entitytracker
 *   .EntityTracker, public static isEliteMob(Entity)
 *   (javap-verified against the shipped jar,
 *   ~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar).
 *
 * SOFT SEAM, house discipline (PartyLookup pattern): the handle
 * resolves lazily, the bind outcome is logged ONCE (BOUND /
 * UNBOUND), and any failure fails soft to "not elite" — the
 * meter then treats the event normally, and EmCombat's
 * simulated-amount floor remains as the seam-independent
 * second layer. A dead seam can never manufacture a million;
 * it can only miss the bucket for ordinary-sized elite hits.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class EliteLookup {

    private final Logger log;

    private Method isEliteMob;
    private boolean bound;
    private boolean logged;
    private boolean callWarned;

    public EliteLookup(Logger log) { this.log = log; }

    /** True when EliteMobs tracks this entity as an elite. */
    public boolean isElite(Entity entity) {
        if (entity == null) return false;
        try {
            if (!bound && !bind()) return false;
            Object r = isEliteMob.invoke(null, entity);
            return r instanceof Boolean b && b;
        } catch (Exception e) {
            warnOnce(e);
            return false;
        }
    }

    private boolean bind() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null) { logOnce(false, "plugin absent"); return false; }
        try {
            Class<?> tracker = Class.forName(
                    "com.magmaguy.elitemobs.entitytracker.EntityTracker",
                    true, em.getClass().getClassLoader());
            isEliteMob = tracker.getMethod("isEliteMob", Entity.class);
            bound = true;
            logOnce(true, "EntityTracker.isEliteMob");
            return true;
        } catch (Exception e) {
            logOnce(false, e.toString());
            return false;
        }
    }

    private void logOnce(boolean ok, String detail) {
        if (logged) return;
        logged = true;
        log.info(ok
                ? "ExcelDetails: EliteMobs seam BOUND (" + detail + ") — elite damage routes to the EM bucket."
                : "ExcelDetails: EliteMobs seam UNBOUND (" + detail + ") — EM bucketing relies on the simulated-amount floor only.");
    }

    private void warnOnce(Exception e) {
        if (callWarned) return;
        callWarned = true;
        log.warning("ExcelDetails: EliteMobs isEliteMob lookup failed once (" + e
                + ") — that check reads not-elite until restart; the amount floor still guards.");
    }
}
