/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.api;

import com.winkreyes.excelbosstrial.core.PressureMath;
import java.lang.reflect.Proxy;
import org.bukkit.entity.LivingEntity;

/**
 * The outgoing-scale API contract (0.3.7, TW-PRESSURE-CHECK fix
 * shape 2): ExcelTrainingWheels 0.1.3 consumes
 * ExcelBossTrialAPI.outgoingScaleFor(LivingEntity) reflectively,
 * so the class name, method name, signature and the 1.0 failure
 * value are pinned here. Inside an encounter the getter reports
 * chapter x ramp (composed by the same PressureMath the handler
 * applies); outside, or on any failure, it reports exactly 1.0.
 */
public final class OutgoingScaleApiTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void close(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    /** A no-op LivingEntity stand-in: the API never calls it when
     *  the provider is a stub, and must not call it when unbound. */
    private static LivingEntity fakeEntity() {
        return (LivingEntity) Proxy.newProxyInstance(
                OutgoingScaleApiTest.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(
                            "the API must not interrogate the entity itself");
                });
    }

    public static void main(String[] args) {
        LivingEntity boss = fakeEntity();

        // Unbound (plugin not loaded): 1.0, for a boss and for null.
        ExcelBossTrialAPI.unbind();
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "unbound: 1.0");
        close(ExcelBossTrialAPI.outgoingScaleFor(null), 1.0, "unbound + null damager: 1.0");
        check(!ExcelBossTrialAPI.bound(), "unbound reports not bound");

        // Bound, inside an encounter: chapter x ramp, composed by
        // PressureMath exactly as the pressure handler applies it
        // (wither 1.25 at minute 10 under the 0.06/min, x2.0 ramp).
        double composed = PressureMath.outgoingScale(1.25, 10.0, 0.06, 2.0);
        close(composed, 2.0, "PressureMath composition: 1.25 x ramp(10 min) = 2.0");
        ExcelBossTrialAPI.bind(damager -> composed);
        check(ExcelBossTrialAPI.bound(), "bound reports bound");
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 2.0,
                "in an encounter: chapter x ramp reaches the consumer");

        // Bound, outside any encounter: the provider's 1.0 passes through.
        ExcelBossTrialAPI.bind(damager -> 1.0);
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "no encounter: 1.0");

        // A provider scale of exactly 0 (a chapter outgoing kill-switch:
        // the boss's hits deal 0, so no compensation applies) reports
        // as 1.0 - the getter never hands out a zero divisor.
        ExcelBossTrialAPI.bind(damager -> 0.0);
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "provider 0.0: reports 1.0");

        // Failures degrade to exactly 1.0 - never an exception, never
        // a value that could push a protected player below the line.
        ExcelBossTrialAPI.bind(damager -> {
            throw new IllegalStateException("provider blew up");
        });
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "throwing provider: 1.0");
        ExcelBossTrialAPI.bind(damager -> Double.NaN);
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "NaN provider: 1.0");
        ExcelBossTrialAPI.bind(damager -> -2.5);
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "negative provider: 1.0");
        ExcelBossTrialAPI.bind(damager -> composed);
        close(ExcelBossTrialAPI.outgoingScaleFor(null), 1.0,
                "bound + null damager: 1.0 (provider not consulted)");

        // Unbind restores the not-loaded behavior.
        ExcelBossTrialAPI.unbind();
        close(ExcelBossTrialAPI.outgoingScaleFor(boss), 1.0, "after unbind: 1.0");

        System.out.println("OutgoingScaleApiTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
