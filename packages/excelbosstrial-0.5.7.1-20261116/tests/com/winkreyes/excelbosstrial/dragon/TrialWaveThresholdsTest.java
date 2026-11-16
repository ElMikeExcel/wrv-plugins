/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dragon add-wave thresholds as CONFIG (0.3.2): before 0.3.2 the
 * 75/50/25% phase-2 thresholds were TrialStateMachine code
 * constants. At the defaults the machine must behave EXACTLY as
 * the pre-0.3.2 code did (the static mask semantics are pinned
 * by TrialStateMachineTest and re-checked here against a live
 * machine), and a changed value must MOVE the wave: with
 * {0.9, 0.4} the first wave is pending at 85% health, where the
 * defaults spawn nothing.
 */
public final class TrialWaveThresholdsTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eq(int actual, int expected, String label) {
        check(actual == expected, label + " (expected " + expected + ", got " + actual + ")");
    }

    /** A machine driven into PHASE2 the honest way. */
    private static TrialStateMachine phase2Machine() {
        TrialStateMachine m = new TrialStateMachine();
        m.startBattle("dragon-1");
        TrialStateMachine.Verdict v = m.onDamage(100.0, 100.0);
        check(v == TrialStateMachine.Verdict.FLIP, "setup: lethal phase-1 hit flips");
        m.completeTransition();
        check(m.phase() == BattlePhase.PHASE2, "setup: machine is in phase 2");
        return m;
    }

    public static void main(String[] args) {
        // --- defaults reproduce the pre-0.3.2 behavior exactly
        TrialStateMachine def = phase2Machine();
        check(java.util.Arrays.equals(def.waveThresholds(), TrialStateMachine.WAVE_THRESHOLDS),
                "a fresh machine carries the settled defaults");
        eq(def.pendingWaveMask(0.85), 0, "defaults: no wave at 85%");
        eq(def.pendingWaveMask(0.74), 0b001, "defaults: first wave at 74%");
        def.commitWaves(0b001);
        eq(def.pendingWaveMask(0.74), 0, "defaults: the wave fires once");
        eq(def.pendingWaveMask(0.49), 0b010, "defaults: second wave at 49%");
        eq(def.pendingWaveMask(0.20), 0b110, "defaults: second + third pending at 20%");

        // --- a changed value MOVES the wave
        TrialStateMachine custom = phase2Machine();
        custom.setWaveThresholds(new double[] {0.9, 0.4});
        eq(custom.pendingWaveMask(0.95), 0, "custom {0.9, 0.4}: nothing at 95%");
        eq(custom.pendingWaveMask(0.85), 0b001,
                "custom {0.9, 0.4}: the first wave moved UP to 85% (defaults spawn nothing there)");
        custom.commitWaves(0b001);
        eq(custom.pendingWaveMask(0.45), 0, "custom: no second wave at 45%");
        eq(custom.pendingWaveMask(0.39), 0b010, "custom: the second wave moved DOWN to 39%");

        // --- waves never fire outside phase 2, whatever the keys say
        TrialStateMachine p1 = new TrialStateMachine();
        p1.setWaveThresholds(new double[] {0.99});
        p1.startBattle("dragon-2");
        eq(p1.pendingWaveMask(0.10), 0, "phase 1 never spawns add waves");

        // --- sanitization: junk degrades to the defaults, never to chaos
        check(java.util.Arrays.equals(TrialStateMachine.sanitizeThresholds(null),
                TrialStateMachine.WAVE_THRESHOLDS), "null thresholds -> defaults");
        check(java.util.Arrays.equals(TrialStateMachine.sanitizeThresholds(new double[0]),
                TrialStateMachine.WAVE_THRESHOLDS), "empty thresholds -> defaults");
        check(java.util.Arrays.equals(
                TrialStateMachine.sanitizeThresholds(new double[] {1.5, 0.5, 0.5, -0.2, 0.25}),
                new double[] {0.5, 0.25}),
                "out-of-range values drop, duplicates merge, order is high to low");
        check(java.util.Arrays.equals(
                TrialStateMachine.sanitizeThresholds(new double[] {0.25, 0.9}),
                new double[] {0.9, 0.25}), "an unsorted list is sorted high to low");

        // --- TrialConfig carries the keys (fromMap list handling)
        Map<String, Object> flat = new HashMap<>();
        flat.put("adds.wave-thresholds", List.of(0.9, 0.4));
        TrialConfig cfgCustom = TrialConfig.fromMap(flat);
        check(java.util.Arrays.equals(cfgCustom.addsWaveThresholds, new double[] {0.9, 0.4}),
                "fromMap reads the YAML list into the config");
        TrialConfig cfgDefault = TrialConfig.fromMap(new HashMap<>());
        check(java.util.Arrays.equals(cfgDefault.addsWaveThresholds,
                TrialStateMachine.WAVE_THRESHOLDS), "fromMap without the key -> defaults");
        Map<String, Object> flatJunk = new HashMap<>();
        flatJunk.put("adds.wave-thresholds", List.of(2.0, -1.0));
        check(java.util.Arrays.equals(TrialConfig.fromMap(flatJunk).addsWaveThresholds,
                TrialStateMachine.WAVE_THRESHOLDS),
                "a list of junk values sanitizes back to the defaults");

        System.out.println("TrialWaveThresholdsTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
