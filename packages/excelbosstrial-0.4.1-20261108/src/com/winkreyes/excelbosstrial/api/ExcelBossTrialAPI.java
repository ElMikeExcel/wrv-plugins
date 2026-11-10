/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.api;

import org.bukkit.entity.LivingEntity;

/**
 * Public outgoing-scale API for ExcelBossTrial (0.3.7,
 * TW-PRESSURE-CHECK fix shape 2, BossTrial half).
 *
 * ExcelTrainingWheels 0.1.3 reads this by reflection - the class
 * name, the method name and the signature are the contract, and
 * are pinned by BUILD-RECORD.md and OutgoingScaleApiTest:
 *
 *     Class:  com.winkreyes.excelbosstrial.api.ExcelBossTrialAPI
 *     Method: public static double outgoingScaleFor(LivingEntity)
 *
 * The value is BossTrial's CURRENT outgoing scale for the
 * damager: its chapter's outgoing-multiplier under the attrition
 * ramp - the exact number the pressure handler applies to that
 * boss's hits right now. TrainingWheels divides a protected
 * player's factor by it, restoring the absolute line (factor x
 * the boss's UNSCALED damage) the way its RaidDivision seam does
 * against ExcelRaidBuff.
 *
 * The getter returns 1.0 - "no scale" - in every other case:
 * a null damager, a non-boss entity, a chapter that is off, no
 * open encounter for the damager, the chapter's pressure
 * disabled, the plugin not loaded (provider unbound), or any
 * internal failure. A computed scale of exactly 0 (a chapter
 * outgoing-multiplier kill-switch: the boss's hits deal 0, so
 * no compensation applies) also reports as 1.0 - the getter
 * never hands a consumer a zero divisor. 1.0 is the only safe
 * failure value: dividing by it degrades the consumer to
 * proportional coverage, never below the guaranteed line. The
 * getter never throws.
 *
 * Scope note: this is the PRESSURE scale (chapter outgoing x
 * ramp). The dragon chapter's native phase-2 factor is a
 * separate axis and is not reported here; the dragon's pressure
 * outgoing settles at 1.0, so a dragon reads its ramp only.
 *
 * The bind/unbind methods are internal wiring used by the plugin
 * itself, in the same idiom as BossTrialAPI; consumers should
 * only call the getter.
 */
public final class ExcelBossTrialAPI {

    /** Internal provider the plugin binds on enable. */
    @FunctionalInterface
    public interface OutgoingScaleProvider {
        double outgoingScaleFor(LivingEntity damager);
    }

    /**
     * Internal provider for the seal-grant encounter close
     * (0.4.0, PM-12): closes every open encounter of a seal
     * WITHOUT a verdict - the operation the Nov 7 manual
     * {@code guildgate grant wither} was missing (the encounter
     * kept running over a broken seal until the party gave up
     * and an admin reset it, deleting the fight record).
     * Returns true when at least one encounter closed.
     */
    @FunctionalInterface
    public interface SealCloseProvider {
        boolean closeEncounterForSeal(String sealId, String cause);
    }

    private static volatile OutgoingScaleProvider provider;
    private static volatile SealCloseProvider sealCloseProvider;

    private ExcelBossTrialAPI() {
    }

    /** Internal: called by ExcelBossTrial once its state is loaded. */
    public static void bind(OutgoingScaleProvider loadedProvider) {
        provider = loadedProvider;
    }

    /** Internal: called by ExcelBossTrial on disable. */
    public static void unbind() {
        provider = null;
    }

    /** True while a provider is bound (diagnostics). */
    public static boolean bound() {
        return provider != null;
    }

    /**
     * BossTrial's current outgoing scale for {@code damager}
     * (chapter multiplier x ramp) while it is a seal boss in an
     * open encounter; 1.0 when none applies. Never throws.
     */
    public static double outgoingScaleFor(LivingEntity damager) {
        if (damager == null) {
            return 1.0;
        }
        OutgoingScaleProvider p = provider;
        if (p == null) {
            return 1.0;
        }
        try {
            double scale = p.outgoingScaleFor(damager);
            return Double.isNaN(scale) || scale <= 0.0 ? 1.0 : scale;
        } catch (Throwable failure) {
            return 1.0;
        }
    }

    /** Internal: called by ExcelBossTrial once its state is loaded. */
    public static void bindSealClose(SealCloseProvider loadedProvider) {
        sealCloseProvider = loadedProvider;
    }

    /** Internal: called by ExcelBossTrial on disable. */
    public static void unbindSealClose() {
        sealCloseProvider = null;
    }

    /**
     * Close every open encounter of {@code sealId} without a
     * verdict (0.4.0, PM-12): the fight record is written with
     * its SEAL_GRANTED label, the feedback bar and the sprinkle
     * close, and the boss entity is restored to vanilla
     * strength. No verdict, no cooldown, no loot. Returns false
     * when the plugin is not loaded, the seal id is unknown, or
     * no encounter of that seal is open. Never throws.
     */
    public static boolean closeEncounterForSeal(String sealId, String cause) {
        SealCloseProvider p = sealCloseProvider;
        if (p == null || sealId == null) {
            return false;
        }
        try {
            return p.closeEncounterForSeal(sealId, cause);
        } catch (Throwable failure) {
            return false;
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
