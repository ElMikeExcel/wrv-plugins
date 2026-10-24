package com.winkreyes.excelraidbuff;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * The EliteMobs gateway seam (reflective; resolved lazily against
 * the EliteMobs classloader, cached; TrainingWheels' precedent).
 * Everything EliteMobs in this plugin flows through here, and the
 * seam FAILS SAFE in one direction only: any failure — plugin
 * absent, class moved, invoke error — means "no conversion, no
 * elite read", so the raider simply gets the attribute buff (or
 * stays vanilla) and the raid goes on. The seam never throws into
 * an event handler, and it never touches EliteMobs CONFIG or its
 * global custom-spawn flag: conversion is the public
 * {@code EliteEntity(LivingEntity, int, SpawnReason)} constructor
 * EliteMobs' own natural-spawn handler uses, reads are
 * PersistentTagger and the public config/level statics.
 */
final class EmGateway {

    private final Logger log;
    private boolean seamDead;
    private boolean seamLogged;
    private int convertFailures;

    private Method isEliteMethod;
    private Method getEliteMethod;
    private Method getLevelMethod;
    private Constructor<?> eliteCtor;
    private Method naturalLevelMethod;
    private Method capMethod;

    EmGateway(Logger log) {
        this.log = log;
    }

    boolean available() {
        return !seamDead && resolveTagger();
    }

    boolean seamDead() {
        return seamDead;
    }

    /** True when EliteMobs itself tags this entity as an elite. */
    boolean isElite(Entity entity) {
        if (entity == null || !resolveTagger()) return false;
        try {
            return Boolean.TRUE.equals(isEliteMethod.invoke(null, entity));
        } catch (Throwable t) {
            markDead("EliteMobs elite check failed: " + t);
            return false;
        }
    }

    /** The entity's EliteMobs level, or -1 when not an elite / unreadable. */
    int eliteLevelOf(Entity entity) {
        if (entity == null || !resolveTagger()) return -1;
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

    /**
     * Convert an already-spawned, already-in-its-raid raider into
     * an EliteMobs elite IN PLACE, at the chosen level — the exact
     * call EliteMobs' NaturalMobSpawnEventHandler makes for a
     * natural spawn (construct, then require a non-null living
     * entity back), followed by a tagger read-back so "converted"
     * is proven, not assumed. The entity object, its raid
     * membership, position and gear are untouched by the call.
     */
    boolean convert(LivingEntity entity, int level) {
        if (entity == null || !resolveAll()) return false;
        try {
            Object elite = eliteCtor.newInstance(entity, level, CreatureSpawnEvent.SpawnReason.RAID);
            Object living = elite.getClass().getMethod("getLivingEntity").invoke(elite);
            if (living == null) {
                convertFailure("EliteEntity came back with no living entity for "
                        + entity.getType() + " (conversion refused by EliteMobs)");
                return false;
            }
            if (!isElite(entity)) {
                convertFailure("EliteEntity constructed for " + entity.getType()
                        + " but the tagger does not read it as an elite");
                return false;
            }
            return true;
        } catch (Throwable t) {
            // A constructor that throws part-way (observed on the
            // rig when a conversion was attempted while EliteMobs
            // was still initializing: an NPE inside its power
            // randomization) can leave EM's tag on the entity. The
            // tag is the truth: if EM claims it, EM owns it — the
            // caller must not attribute-buff it (no double-scaling).
            boolean tagged = isElite(entity);
            convertFailure("EliteMobs conversion threw for " + entity.getType() + ": " + t
                    + (tagged ? " (the entity reads as an elite anyway; EliteMobs owns it)" : ""));
            return tagged;
        }
    }

    /**
     * The level EliteMobs would roll for a natural elite spawning
     * at this spot in front of these players (its public ruler,
     * which prices the players' gear and skill). -1 on any failure;
     * the caller treats -1 as "do not convert" rather than guessing.
     */
    int naturalLevel(Location location, List<Player> players) {
        if (location == null || players == null || players.isEmpty() || !resolveAll()) return -1;
        try {
            Object lvl = naturalLevelMethod.invoke(null, location, players);
            return lvl instanceof Number n ? n.intValue() : -1;
        } catch (Throwable t) {
            markDead("EliteMobs natural-level read failed: " + t);
            return -1;
        }
    }

    /** EliteMobs' configured natural elite level cap, live-read; fallback on any failure. */
    int levelCap(int fallback) {
        if (!resolveAll()) return fallback;
        try {
            Object cap = capMethod.invoke(null);
            return cap instanceof Number n && n.intValue() >= 1 ? n.intValue() : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    // ---------- resolution ----------

    private ClassLoader emLoader() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) {
            markDead("EliteMobs plugin not present");
            return null;
        }
        return em.getClass().getClassLoader();
    }

    private boolean resolveTagger() {
        if (seamDead) return false;
        if (isEliteMethod != null) return true;
        ClassLoader loader = emLoader();
        if (loader == null) return false;
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
        if (!resolveTagger()) return false;
        if (eliteCtor != null && naturalLevelMethod != null && capMethod != null) return true;
        ClassLoader loader = emLoader();
        if (loader == null) return false;
        try {
            Class<?> eliteEntity = Class.forName("com.magmaguy.elitemobs.mobconstructor.EliteEntity",
                    true, loader);
            eliteCtor = eliteEntity.getConstructor(LivingEntity.class, int.class,
                    CreatureSpawnEvent.SpawnReason.class);
            Class<?> natural = Class.forName("com.magmaguy.elitemobs.mobspawning.NaturalMobSpawnEventHandler",
                    true, loader);
            naturalLevelMethod = natural.getMethod("getNaturalMobLevel", Location.class, List.class);
            Class<?> combatCfg = Class.forName("com.magmaguy.elitemobs.config.MobCombatSettingsConfig",
                    true, loader);
            capMethod = combatCfg.getMethod("getNaturalEliteMobLevelCap");
            return true;
        } catch (Throwable t) {
            markDead("EliteMobs conversion API resolution failed: " + t);
            return false;
        }
    }

    private void convertFailure(String reason) {
        convertFailures++;
        if (convertFailures <= 3) {
            log.warning("ExcelRaidBuff: elite conversion skipped - " + reason
                    + " (the raider keeps the attribute buff instead)");
        }
    }

    private void markDead(String reason) {
        seamDead = true;
        if (!seamLogged) {
            seamLogged = true;
            log.warning("ExcelRaidBuff: EliteMobs seam unavailable (" + reason
                    + ") - raid elites are OFF for this boot; attribute buffs continue");
        }
    }
}
