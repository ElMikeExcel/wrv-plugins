/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.core.PulseShield;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * The ExcelTrainingWheels shield seam (ExcelBossTrial 0.3.7,
 * TW-PRESSURE-CHECK fix shape 1): reads a player's TrainingWheels
 * damage factor so the pressure pulse - source-less MAGIC damage
 * TrainingWheels' own listener can never see - can honor it.
 *
 * The shape is the one TrainingWheels itself uses for its
 * ExcelRaidBuff seam, run in the other direction: resolve
 * reflectively against the partner plugin's classloader, cache
 * the surface, guard every read, and fail soft. Fail-soft here
 * means a factor of 1.0 (no shield): ExcelTrainingWheels absent,
 * disabled, its master shield switch off, the player not on its
 * protected list, or any reflective step failing - in every one
 * of those states BossTrial behaves exactly as it did before
 * 0.3.7, so the plugin works identically standalone (the
 * modularity law: a replaceable adapter, never a hard weld).
 *
 * Resolution tries, in order:
 *  1. ExcelTrainingWheelsAPI.shieldFactorOf(UUID) - a public API
 *     surface, preferred if a future TrainingWheels ships one.
 *  2. The plugin's ProtectedRegistry (the 0.1.2 ground truth):
 *     the declared registry field, re-read on every lookup
 *     because /trainingwheels reload replaces the registry
 *     object; the declared master-switch field is honored when
 *     present. OptionalDouble from registry.factor(UUID) is the
 *     factor; empty means not protected.
 *
 * A lookup that throws marks the seam dead for the boot (logged
 * once); every later read returns 1.0 without consulting the
 * partner again.
 *
 * Resolution itself distinguishes "not yet" from "never":
 * ExcelTrainingWheels 0.1.3 softdepends on ExcelBossTrial
 * while BossTrial softdepends on ExcelTrainingWheels, so the
 * enable order under the cycle is arbitrary - the 0.3.7 rig
 * (boot P5) caught Paper enabling BossTrial FIRST, and a
 * one-shot resolution there reported the partner "not present"
 * and latched the shield off for the whole boot. A partner
 * that is absent or not yet enabled is therefore RETRYABLE:
 * resolution is re-attempted on later calls until it binds
 * (and the late bind is logged when it lands after an UNBOUND
 * state was already reported). Only a partner that IS enabled
 * but exposes no factor surface latches UNBOUND - retrying
 * could never change that answer.
 */
final class TwShieldSeam {

    /** The factor source; the production implementation is the
     *  reflective read below, tests bind stubs. */
    @FunctionalInterface
    interface Lookup {
        OptionalDouble lookup(UUID playerId) throws Exception;
    }

    private static final String TW_PLUGIN = "ExcelTrainingWheels";
    private static final String TW_API_CLASS =
            "com.winkreyes.exceltrainingwheels.api.ExcelTrainingWheelsAPI";

    private final Logger logger;
    private final Lookup lookup;
    private final boolean production;

    private boolean dead;
    private boolean deadLogged;

    // Reflective surface (production only; resolved lazily and
    // eagerly at enable for the boot line, then re-attempted on
    // later calls while the state is the retryable not-yet -
    // see the class note on the softdepend cycle).
    private boolean latchedUnbound;
    private boolean reportedUnbound;
    private boolean lateBindLogged;
    private boolean bound;
    private String stateLine = "pulse shield: ExcelTrainingWheels seam not resolved yet";
    private Plugin twPlugin;
    private Method apiMethod;
    private Field registryField;
    private Method registryFactor;
    private Field masterField;

    TwShieldSeam(Logger logger) {
        this.logger = logger;
        this.production = true;
        this.lookup = this::reflectiveLookup;
    }

    /** Tests: bind a stub factor source. */
    TwShieldSeam(Logger logger, Lookup lookup) {
        this.logger = logger;
        this.production = false;
        this.lookup = lookup;
    }

    /**
     * The player's TrainingWheels shield factor, sanitized to
     * [0, 1]; 1.0 (no shield) whenever no factor applies. Never
     * throws.
     */
    double factorFor(UUID playerId) {
        if (playerId == null || dead) {
            return 1.0;
        }
        try {
            OptionalDouble factor = lookup.lookup(playerId);
            return factor.isPresent()
                    ? PulseShield.sanitizeFactor(factor.getAsDouble()) : 1.0;
        } catch (Throwable failure) {
            dead = true;
            if (!deadLogged) {
                deadLogged = true;
                logger.log(Level.WARNING, "pulse shield: TrainingWheels factor read failed ("
                        + failure + ") - seam dead for this boot, pulse at full amount.");
            }
            return 1.0;
        }
    }

    /** Resolve the seam (production) and report its state for the
     *  boot log; the caller logs the returned line. An UNBOUND
     *  report is remembered, so a later successful bind logs its
     *  own line - the boot record never claims the shield is off
     *  for a boot where it came on seconds later. */
    String describeState() {
        if (!production) {
            return "pulse shield: seam stubbed (test)";
        }
        ensureResolved();
        if (!bound) {
            reportedUnbound = true;
        }
        return stateLine;
    }

    /** True when a live factor surface is bound (production). */
    boolean bound() {
        if (!production) {
            return true;
        }
        ensureResolved();
        return bound;
    }

    // --- the reflective read ------------------------------------

    private OptionalDouble reflectiveLookup(UUID playerId) throws Exception {
        ensureResolved();
        if (!bound) {
            return OptionalDouble.empty();
        }
        if (apiMethod != null) {
            return asFactor(apiMethod.invoke(null, playerId));
        }
        if (masterField != null && !masterField.getBoolean(twPlugin)) {
            return OptionalDouble.empty();
        }
        Object registry = registryField.get(twPlugin);
        if (registry == null) {
            return OptionalDouble.empty();
        }
        return asFactor(registryFactor.invoke(registry, playerId));
    }

    private static OptionalDouble asFactor(Object out) {
        if (out instanceof OptionalDouble od) {
            return od;
        }
        if (out instanceof Number n) {
            return OptionalDouble.of(n.doubleValue());
        }
        return OptionalDouble.empty();
    }

    private void ensureResolved() {
        if (bound || latchedUnbound) {
            return;
        }
        try {
            Plugin tw = Bukkit.getPluginManager().getPlugin(TW_PLUGIN);
            if (tw == null || !tw.isEnabled()) {
                // Retryable (the softdepend cycle - see the
                // class note): do NOT latch; the next call
                // re-checks, and the shield comes on the moment
                // the partner finishes enabling.
                stateLine = "pulse shield: ExcelTrainingWheels seam UNBOUND (plugin not present)"
                        + " - pulse at full amount";
                return;
            }
            ClassLoader cl = tw.getClass().getClassLoader();
            try {
                Class<?> api = Class.forName(TW_API_CLASS, true, cl);
                Method m = api.getMethod("shieldFactorOf", UUID.class);
                if (Modifier.isStatic(m.getModifiers())) {
                    twPlugin = tw;
                    apiMethod = m;
                    bound = true;
                    stateLine = "pulse shield: ExcelTrainingWheels seam BOUND (API)"
                            + " - the pressure pulse honors protected players' shield factors";
                    noteBound();
                    return;
                }
            } catch (Throwable noApiSurface) {
                // Fall through to the registry read (the 0.1.2 surface).
            }
            Field rf = findField(tw.getClass(), "registry");
            if (rf != null) {
                rf.setAccessible(true);
                Object registry = rf.get(tw);
                if (registry != null) {
                    Method fm = registry.getClass().getMethod("factor", UUID.class);
                    Field mf = findField(tw.getClass(), "enabledMaster");
                    if (mf != null) {
                        mf.setAccessible(true);
                    }
                    twPlugin = tw;
                    registryField = rf;
                    registryFactor = fm;
                    masterField = mf;
                    bound = true;
                    stateLine = "pulse shield: ExcelTrainingWheels seam BOUND (registry)"
                            + " - the pressure pulse honors protected players' shield factors";
                    noteBound();
                    return;
                }
            }
            // Enabled but no usable surface: retrying could
            // never change this answer, so it latches.
            latchedUnbound = true;
            stateLine = "pulse shield: ExcelTrainingWheels seam UNBOUND (no factor lookup surface)"
                    + " - pulse at full amount";
        } catch (Throwable failure) {
            // Retryable, like the not-yet-enabled state: a
            // transient classloader moment must not latch the
            // shield off either.
            bound = false;
            stateLine = "pulse shield: ExcelTrainingWheels seam UNBOUND (resolution failed: "
                    + failure + ") - pulse at full amount";
        }
    }

    /** Log the bind when it lands after an UNBOUND state was
     *  already reported at boot (the softdepend-cycle order):
     *  the boot record then carries both lines and the later
     *  one governs. Called on each successful bind; logs once. */
    private void noteBound() {
        if (reportedUnbound && !lateBindLogged) {
            lateBindLogged = true;
            logger.info(stateLine + " (bound on a later check - the partner"
                    + " enabled after BossTrial; the shield has been honored"
                    + " from that moment on)");
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException notHere) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
