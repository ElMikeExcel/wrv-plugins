/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Healing and support attribution (ExcelBossTrial 0.3.0, SPEC
 * section 4.6): the seam behind which support recognition is
 * computed, kept as an interface because the honest current state
 * of the evidence is thin - NOTHING on the server today records
 * healing done. BossTrial hears damage DEALT to bosses and damage
 * TAKEN from them; it never sees a heal.
 *
 * The stakes rule the design answers to: recognition may never
 * gate PROGRESSION. This seam feeds a title and a cosmetic only,
 * and whatever implementation stands behind it can be swapped -
 * a real healing feed (a future Magic adapter event) - without
 * the reward path changing shape.
 */
public interface HealingAttribution {

    /**
     * The encounter's support champion, if any: the participant
     * the implementation judges to have carried the group's
     * sustain. Empty when no participant reads as support -
     * recognition is never forced onto a fight that had none.
     */
    Optional<UUID> supportChampion(EncounterLedger ledger, long nowMs);

    /** The shipped implementation: the presence proxy below. */
    static HealingAttribution presenceProxy() {
        return new PresenceHealingAttribution();
    }
}

/**
 * The shipped implementation (SPEC section 4.6, "the presence
 * proxy"): support weight = presence fraction of the encounter
 * x (1 - dealt share). A participant who stood in the fight the
 * whole way and dealt little of the damage was, by the only
 * evidence the ledger holds, doing something other than damage -
 * on this server, in a boss arena, that something is support.
 *
 * Stated limits (the spec's own, kept with the code): crude by
 * construction - a tank who holds the boss and deals little also
 * scores; a solo clear has dealt share 1 and scores 0, correctly
 * naming nobody. Recognition-only stakes are what make a proxy
 * acceptable here.
 */
final class PresenceHealingAttribution implements HealingAttribution {

    @Override
    public Optional<UUID> supportChampion(EncounterLedger ledger, long nowMs) {
        if (ledger == null) {
            return Optional.empty();
        }
        Map<UUID, Double> dealt = ledger.dealtSnapshot();
        double playerDamage = ledger.playerDamage();
        UUID best = null;
        double bestWeight = 0.0;
        for (UUID participant : ledger.participants()) {
            double share = playerDamage > 0
                    ? dealt.getOrDefault(participant, 0.0) / playerDamage : 0.0;
            double weight = weight(ledger.presenceFraction(participant, nowMs), share);
            if (weight > bestWeight) {
                bestWeight = weight;
                best = participant;
            }
        }
        return bestWeight > 0.0 ? Optional.ofNullable(best) : Optional.empty();
    }

    /** The proxy itself, unit-testable without a ledger. */
    static double weight(double presenceFraction, double dealtShare) {
        double presence = clamp01(presenceFraction);
        double share = clamp01(dealtShare);
        return presence * (1.0 - share);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || v < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, v);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
