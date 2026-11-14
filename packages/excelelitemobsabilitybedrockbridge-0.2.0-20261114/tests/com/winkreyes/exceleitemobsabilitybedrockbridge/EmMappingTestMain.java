package com.winkreyes.exceleitemobsabilitybedrockbridge;

import com.magmaguy.elitemobs.advancedcombat.abilities.AbilityFailureReason;
import com.magmaguy.elitemobs.advancedcombat.classes.AbilitySlot;

/**
 * Mapping pins that only make sense against the REAL pinned EliteMobs
 * jar (run-tests.sh run B). If a future EliteMobs adds or renames a
 * failure reason or ability slot, this suite fails loudly - that is
 * the tripwire that turns an upstream change into a scheduled repair
 * instead of a silent degradation.
 */
public final class EmMappingTestMain {

    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("PASS " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name + " :: " + detail);
        }
    }

    public static void main(String[] args) {
        // Failure reasons: every current EliteMobs constant maps to the
        // same-named bridge failure - never UNKNOWN.
        check("EM failure reason count pinned (16)",
                AbilityFailureReason.values().length == 16,
                "was " + AbilityFailureReason.values().length);
        for (AbilityFailureReason reason : AbilityFailureReason.values()) {
            BridgeFailure mapped = EmAcsGateway.mapReason(reason);
            check("mapReason " + reason,
                    mapped == BridgeFailure.valueOf(reason.name()),
                    "mapped to " + mapped);
        }
        check("mapReason null -> UNKNOWN",
                EmAcsGateway.mapReason(null) == BridgeFailure.UNKNOWN, "");

        // Slots: exactly three, name-for-name both ways.
        check("EM slot count pinned (3)",
                AbilitySlot.values().length == 3,
                "was " + AbilitySlot.values().length);
        check("BridgeSlot count pinned (3)",
                BridgeSlot.values().length == 3,
                "was " + BridgeSlot.values().length);
        for (BridgeSlot slot : BridgeSlot.values()) {
            AbilitySlot em = EmAcsGateway.toEmSlot(slot);
            check("toEmSlot " + slot, em.name().equals(slot.name()), "was " + em);
        }
        for (AbilitySlot em : AbilitySlot.values()) {
            check("EM slot has bridge twin " + em,
                    BridgeSlot.valueOf(em.name()) != null, "");
        }

        System.out.println("----");
        System.out.println(checks + " checks, " + failures + " failures");
        System.exit(failures == 0 ? 0 : 1);
    }
}
