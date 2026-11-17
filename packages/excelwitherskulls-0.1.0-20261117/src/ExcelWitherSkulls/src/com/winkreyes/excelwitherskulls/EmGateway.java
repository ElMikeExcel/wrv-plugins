package com.winkreyes.excelwitherskulls;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * The EliteMobs read seam (reflective; resolved lazily against the
 * EliteMobs classloader, cached) — the same pattern ExcelRaidBuff
 * and ExcelTrainingWheels use: EliteMobs' own
 * {@code PersistentTagger} is the single source of "is this entity
 * an elite", so this plugin can never disagree with EliteMobs
 * about what an elite is.
 *
 * <p>THE SEAM FAILS SAFE IN ONE DIRECTION ONLY: any failure —
 * EliteMobs absent or disabled, class moved, invoke error — reads
 * as "not an elite", so the mob simply keeps its vanilla drops and
 * the plugin never adds a skull it cannot prove was earned. The
 * seam never throws into an event handler and never touches
 * EliteMobs state or config; it only reads.
 */
final class EmGateway {

    private final Logger log;
    private boolean seamDead;
    private boolean seamLogged;
    private Method isEliteMethod;
    private Method getEliteMethod;
    private Method getLevelMethod;

    EmGateway(Logger log) {
        this.log = log;
    }

    boolean available() {
        return !seamDead && resolve();
    }

    boolean seamDead() {
        return seamDead;
    }

    /** True only when EliteMobs itself tags this entity as an elite. */
    boolean isElite(Entity entity) {
        if (entity == null || !resolve()) return false;
        try {
            return Boolean.TRUE.equals(isEliteMethod.invoke(null, entity));
        } catch (Throwable t) {
            markDead("EliteMobs elite check failed: " + t);
            return false;
        }
    }

    /** The entity's EliteMobs level, or -1 when unreadable (debug aid only). */
    int eliteLevelOf(Entity entity) {
        if (entity == null || !resolve()) return -1;
        try {
            Object elite = getEliteMethod.invoke(null, entity);
            if (elite == null) return -1;
            if (getLevelMethod == null) {
                getLevelMethod = elite.getClass().getMethod("getLevel");
            }
            Object lvl = getLevelMethod.invoke(elite);
            return lvl instanceof Number n ? n.intValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private boolean resolve() {
        if (seamDead) return false;
        if (isEliteMethod != null) return true;
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) {
            markDead("EliteMobs plugin not present or not enabled");
            return false;
        }
        try {
            Class<?> tagger = Class.forName("com.magmaguy.elitemobs.tagger.PersistentTagger",
                    true, em.getClass().getClassLoader());
            isEliteMethod = tagger.getMethod("isEliteEntity", Entity.class);
            getEliteMethod = tagger.getMethod("getEliteEntity", Entity.class);
            return true;
        } catch (Throwable t) {
            markDead("EliteMobs tagger resolution failed: " + t);
            return false;
        }
    }

    private void markDead(String reason) {
        seamDead = true;
        if (!seamLogged) {
            seamLogged = true;
            log.warning("ExcelWitherSkulls: EliteMobs seam unavailable (" + reason
                    + ") - the skull top-up is OFF for this boot; vanilla drops are untouched");
        }
    }
}
