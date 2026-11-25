/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.sprinkle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * The EliteMobs gateway for the elite sprinkle (ExcelBossTrial
 * 0.3.3) - a mirror of ExcelRaidBuff's proven EmGateway (study
 * section 1.1), extended with the containment and removal calls
 * the sprinkle needs. This is the ONLY class in BossTrial that
 * touches EliteMobs, and it does so reflectively against EM's
 * classloader: BossTrial compiles and runs with EliteMobs
 * absent, and any failure - plugin missing, class moved, invoke
 * error - degrades to "no conversion" and never throws into a
 * fight. The fight proceeds exactly as if the sprinkle were off.
 *
 * Conversion is EM's public constructor
 * {@code EliteEntity(LivingEntity, int, SpawnReason)} - the call
 * EM's own natural-spawn handler makes - with
 * {@code SpawnReason.CUSTOM} (RaidBuff converts existing raiders
 * with RAID; BossTrial spawns its own adds, so CUSTOM). A
 * conversion is PROVEN, not assumed: the living entity must come
 * back non-null and PersistentTagger must read the entity as an
 * elite afterwards.
 *
 * Containment (study section 4, bytecode-verified gates): a
 * fresh EliteEntity defaults elite/random/vanilla loot TRUE, so
 * immediately after conversion the adapter sets
 * setEliteLoot / setRandomLoot / setVanillaLoot false and READS
 * THEM BACK. isEliteLoot false is the single gate that covers
 * both the coin shower and the elite item rolls; isRandomLoot
 * gates procedural loot; isVanillaLoot gates vanilla drops. The
 * one residual - EM skill XP has no per-entity flag on this
 * route - is accepted by precedent (RaidBuff's conversions carry
 * the same) and named in the changelog.
 *
 * TrainingWheels constraint (study section 5): the add's
 * damageMultiplier is READ at conversion and reported; the
 * sprinkle never sets one (an authored multiplier is invisible
 * to TrainingWheels' ladder division). EM's constructor leaves
 * it 1.0; the read exists so the rig measurement and any future
 * EM change are visible in the log, not silent.
 */
public final class EliteSprinkleGateway {

    private final Logger log;
    private boolean seamDead;
    private boolean seamLogged;
    private int convertFailures;

    private Method isEliteMethod;
    private Method getEliteMethod;
    private Constructor<?> eliteCtor;
    private Method getLivingMethod;
    private Method getLevelMethod;
    private Method getDamageMultiplierMethod;
    private Method setEliteLootMethod;
    private Method setRandomLootMethod;
    private Method setVanillaLootMethod;
    private Method isEliteLootMethod;
    private Method isRandomLootMethod;
    private Method isVanillaLootMethod;
    private Method removeMethod;
    private Object removalOther;

    public EliteSprinkleGateway(Logger log) {
        this.log = log;
    }

    public boolean available() {
        return !seamDead && resolveTagger();
    }

    public boolean seamDead() {
        return seamDead;
    }

    /** True when EliteMobs itself tags this entity as an elite. */
    public boolean isElite(Entity entity) {
        if (entity == null || !resolveTagger()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isEliteMethod.invoke(null, entity));
        } catch (Throwable t) {
            markDead("EliteMobs elite check failed: " + t);
            return false;
        }
    }

    /** The entity's EliteMobs level, or -1 when not an elite / unreadable. */
    public int eliteLevelOf(Entity entity) {
        Object elite = eliteOf(entity);
        if (elite == null) {
            return -1;
        }
        try {
            if (getLevelMethod == null) {
                getLevelMethod = elite.getClass().getMethod("getLevel");
            }
            Object lvl = getLevelMethod.invoke(elite);
            return lvl instanceof Number n ? n.intValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** The EliteEntity handle for an entity, or null. */
    public Object eliteOf(Entity entity) {
        if (entity == null || !resolveTagger()) {
            return null;
        }
        try {
            return getEliteMethod.invoke(null, entity);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Convert an already-spawned vanilla mob into an EliteMobs
     * elite IN PLACE at the fixed level, then close the three
     * loot flags and verify them by read-back. If EM's natural
     * handler already converted the mob in its spawn event (the
     * ladder's spawn chance), the constructor is skipped - the
     * mob is an elite either way - but the flags are still set
     * and verified, and the caller is told the level is EM's,
     * not the chapter's (the outcome records it).
     *
     * @return the conversion outcome; {@code ok()} false means
     *         the caller must remove the vanilla spawn - a
     *         sprinkle that cannot convert does not get to
     *         change the fight at all
     */
    public record Conversion(boolean ok, boolean preConverted, int level,
                             double damageMultiplier) {
    }

    public Conversion convertAndContain(LivingEntity entity, int level) {
        Conversion fail = new Conversion(false, false, -1, Double.NaN);
        if (entity == null || !resolveAll()) {
            return fail;
        }
        try {
            boolean pre = isElite(entity);
            Object elite;
            if (pre) {
                elite = eliteOf(entity);
                if (elite == null) {
                    convertFailure("entity reads as an elite but no EliteEntity "
                            + "handle came back for " + entity.getType());
                    return fail;
                }
            } else {
                Object built = eliteCtor.newInstance(entity, level,
                        CreatureSpawnEvent.SpawnReason.CUSTOM);
                Object living = getLivingMethod.invoke(built);
                if (living == null) {
                    convertFailure("EliteEntity came back with no living entity for "
                            + entity.getType() + " (conversion refused by EliteMobs)");
                    return fail;
                }
                if (!isElite(entity)) {
                    convertFailure("EliteEntity constructed for " + entity.getType()
                            + " but the tagger does not read it as an elite");
                    return fail;
                }
                elite = eliteOf(entity);
                if (elite == null) {
                    convertFailure("converted " + entity.getType()
                            + " but no EliteEntity handle came back");
                    return fail;
                }
            }
            // Containment: close the three loot flags, then prove
            // them by read-back (study section 4).
            setEliteLootMethod.invoke(elite, false);
            setRandomLootMethod.invoke(elite, false);
            setVanillaLootMethod.invoke(elite, false);
            boolean eliteLoot = Boolean.TRUE.equals(isEliteLootMethod.invoke(elite));
            boolean randomLoot = Boolean.TRUE.equals(isRandomLootMethod.invoke(elite));
            boolean vanillaLoot = Boolean.TRUE.equals(isVanillaLootMethod.invoke(elite));
            if (eliteLoot || randomLoot || vanillaLoot) {
                convertFailure("loot flags did not read back closed for "
                        + entity.getType() + " (eliteLoot=" + eliteLoot
                        + ", randomLoot=" + randomLoot
                        + ", vanillaLoot=" + vanillaLoot + ")");
                return fail;
            }
            double mult = Double.NaN;
            try {
                Object m = getDamageMultiplierMethod.invoke(elite);
                if (m instanceof Number n) {
                    mult = n.doubleValue();
                }
            } catch (Throwable ignored) {
                // The read is evidence, not a gate: a future EM
                // without the accessor must not kill conversions.
            }
            int actualLevel = eliteLevelOf(entity);
            return new Conversion(true, pre, actualLevel, mult);
        } catch (Throwable t) {
            // A constructor that throws part-way can leave EM's
            // tag on the entity (RaidBuff observed this). The tag
            // is the truth: report it so the caller keeps the mob
            // OUT of the sprinkle registry - EliteMobs owns it -
            // rather than tracking an add we cannot contain.
            boolean tagged = isElite(entity);
            convertFailure("EliteMobs conversion threw for " + entity.getType() + ": " + t
                    + (tagged ? " (the entity reads as an elite anyway; "
                    + "not tracked as a sprinkle add)" : ""));
            return fail;
        }
    }

    /**
     * Remove an elite through EM's own removal path
     * ({@code EliteEntity.remove(RemovalReason.OTHER)}: untags,
     * unregisters, fires EM's remove event - removed, never
     * killed: no death event, no drops, no XP).
     *
     * @return false when the handle or the call is unavailable;
     *         the caller falls back to a plain Entity.remove()
     */
    public boolean removeElite(Entity entity) {
        Object elite = eliteOf(entity);
        if (elite == null || !resolveRemove(elite)) {
            return false;
        }
        try {
            removeMethod.invoke(elite, removalOther);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------- resolution ----------

    private ClassLoader emLoader() {
        try {
            Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null || !em.isEnabled()) {
                markDead("EliteMobs plugin not present");
                return null;
            }
            return em.getClass().getClassLoader();
        } catch (Throwable t) {
            markDead("EliteMobs lookup failed: " + t);
            return null;
        }
    }

    private boolean resolveTagger() {
        if (seamDead) {
            return false;
        }
        if (isEliteMethod != null) {
            return true;
        }
        ClassLoader loader = emLoader();
        if (loader == null) {
            return false;
        }
        try {
            Class<?> tagger = Class.forName("com.magmaguy.elitemobs.tagger.PersistentTagger",
                    true, loader);
            isEliteMethod = tagger.getMethod("isEliteEntity", Entity.class);
            getEliteMethod = tagger.getMethod("getEliteEntity", Entity.class);
            return true;
        } catch (Throwable t) {
            markDead("EliteMobs tagger resolution failed: " + t);
            return false;
        }
    }

    private boolean resolveAll() {
        if (!resolveTagger()) {
            return false;
        }
        if (eliteCtor != null) {
            return true;
        }
        ClassLoader loader = emLoader();
        if (loader == null) {
            return false;
        }
        try {
            Class<?> eliteEntity = Class.forName(
                    "com.magmaguy.elitemobs.mobconstructor.EliteEntity", true, loader);
            eliteCtor = eliteEntity.getConstructor(LivingEntity.class, int.class,
                    CreatureSpawnEvent.SpawnReason.class);
            getLivingMethod = eliteEntity.getMethod("getLivingEntity");
            setEliteLootMethod = eliteEntity.getMethod("setEliteLoot", boolean.class);
            setRandomLootMethod = eliteEntity.getMethod("setRandomLoot", boolean.class);
            setVanillaLootMethod = eliteEntity.getMethod("setVanillaLoot", boolean.class);
            isEliteLootMethod = eliteEntity.getMethod("isEliteLoot");
            isRandomLootMethod = eliteEntity.getMethod("isRandomLoot");
            isVanillaLootMethod = eliteEntity.getMethod("isVanillaLoot");
            getDamageMultiplierMethod = eliteEntity.getMethod("getDamageMultiplier");
            return true;
        } catch (Throwable t) {
            markDead("EliteMobs conversion API resolution failed: " + t);
            return false;
        }
    }

    private boolean resolveRemove(Object elite) {
        if (removeMethod != null) {
            return true;
        }
        ClassLoader loader = emLoader();
        if (loader == null) {
            return false;
        }
        try {
            Class<?> reason = Class.forName(
                    "com.magmaguy.elitemobs.api.internal.RemovalReason", true, loader);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object other = Enum.valueOf((Class<Enum>) reason, "OTHER");
            removalOther = other;
            removeMethod = elite.getClass().getMethod("remove", reason);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void convertFailure(String reason) {
        convertFailures++;
        if (convertFailures <= 3) {
            log.warning("ExcelBossTrial sprinkle: conversion skipped - " + reason
                    + " (the vanilla spawn is removed; the fight is unchanged)");
        }
    }

    private void markDead(String reason) {
        seamDead = true;
        if (!seamLogged) {
            seamLogged = true;
            log.warning("ExcelBossTrial sprinkle: EliteMobs seam unavailable (" + reason
                    + ") - the elite sprinkle is OFF for this boot; "
                    + "seal fights run exactly as before");
        }
    }
}
