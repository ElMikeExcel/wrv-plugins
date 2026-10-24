package com.winkreyes.excelbosstrial.core;

import java.util.Set;
import java.util.UUID;

/**
 * The credit verdict for one seal-boss death, as pure logic (the
 * adapter supplies the world facts). Settled rules, Mike, Oct 18,
 * 2026 (SEAL-INTEGRITY-RESEARCH.md, DECISIONS ADDENDUM):
 *
 * 1. SHARE GATE: player-attributed damage must reach the threshold
 *    share (default 50%) of the boss's max HP. No near-miss mercy.
 * 2. PRESENCE: at the death, at least one encounter participant
 *    (dealt damage to the boss or took damage from it) must be
 *    alive, in the same world, within the presence range
 *    (default 128 blocks) of the boss.
 * 3. COOLDOWN: a seal on credit cooldown cannot credit at all,
 *    however honest the kill.
 *
 * The verdict names WHY a kill did not credit, because the
 * consequences differ: a failed share/presence kill STARTS (or
 * refreshes) the cooldown; a would-have-credited kill during an
 * active cooldown changes nothing but the explanation.
 */
public final class CreditJudge {

    public enum Verdict {
        /** Credit the seal. */
        CREDIT,
        /** Player share below the gate: no credit, cooldown starts. */
        DENY_SHARE,
        /** Share met but no participant in range: no credit, cooldown starts. */
        DENY_PRESENCE,
        /** The fight qualified, but the seal is on credit cooldown: no credit, no refresh. */
        DENY_COOLDOWN
    }

    private CreditJudge() {
    }

    /**
     * Judge one death.
     *
     * @param shareMet          the ledger's share verdict (threshold of max HP)
     * @param participantInRange at least one participant alive, same
     *                          world, within range at the death
     * @param cooldownActive    the seal's credit cooldown is running
     */
    public static Verdict judge(boolean shareMet, boolean participantInRange,
                                boolean cooldownActive) {
        if (cooldownActive) {
            // A genuinely fought kill during the cooldown is the only
            // one that does not extend it; a cheese kill during the
            // cooldown is still a non-crediting kill and refreshes.
            return (shareMet && participantInRange) ? Verdict.DENY_COOLDOWN
                    : (shareMet ? Verdict.DENY_PRESENCE : Verdict.DENY_SHARE);
        }
        if (!shareMet) {
            return Verdict.DENY_SHARE;
        }
        if (!participantInRange) {
            return Verdict.DENY_PRESENCE;
        }
        return Verdict.CREDIT;
    }

    /** True for verdicts that start/refresh the credit cooldown. */
    public static boolean startsCooldown(Verdict v) {
        return v == Verdict.DENY_SHARE || v == Verdict.DENY_PRESENCE;
    }

    /**
     * The warden height rule (Mike, Oct 19, 2026 evening amendment):
     * a hit counts toward the seal share only when the attacker
     * stood within {@code bandBlocks} vertically of the boss when
     * the hit landed. A band of 0 (or negative) disables the rule -
     * every hit counts, as before.
     */
    public static boolean withinHeightBand(double attackerY, double bossY,
                                           double bandBlocks) {
        if (bandBlocks <= 0) {
            return true;
        }
        return Math.abs(attackerY - bossY) <= bandBlocks;
    }

    /**
     * Whether one player-attributed hit counts toward the seal
     * share. The height band applies to the WARDEN ONLY (the
     * ground-bound boss whose threat model is proximity; the wither
     * flies to its attackers and the elder guardians live
     * underwater, so neither carries a band). For every other boss
     * kind every attributed hit counts, exactly as before.
     *
     * The adapter resolves {@code attackerY} from the RESPONSIBLE
     * PLAYER's position at hit time - for a projectile that is the
     * shooter's position, never the projectile's (the arrow is
     * already at the boss when it lands, so its own Y would always
     * read in-band).
     */
    public static boolean countsTowardShare(BossKind kind, double attackerY,
                                            double bossY, double wardenBandBlocks) {
        return kind != BossKind.WARDEN
                || withinHeightBand(attackerY, bossY, wardenBandBlocks);
    }

    /**
     * The warden range rule (0.2.1, Mike, Oct 20, 2026 late - the
     * seal-difficulty audit's horizontal shooting gallery): a hit
     * counts toward the warden share only when the attacker stood
     * within {@code bandBlocks} HORIZONTALLY of the warden when
     * the hit landed. The settled value (14) mirrors the sonic
     * boom's horizontal reach exactly - if it could not boom you,
     * it does not count. A band of 0 (or negative) disables the
     * rule, like the height band.
     */
    public static boolean withinRangeBand(double attackerX, double attackerZ,
                                          double bossX, double bossZ,
                                          double bandBlocks) {
        if (bandBlocks <= 0) {
            return true;
        }
        return horizontalDistance(attackerX, attackerZ, bossX, bossZ) <= bandBlocks;
    }

    /** Horizontal (XZ-plane) distance between two positions. */
    public static double horizontalDistance(double x1, double z1,
                                            double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * The full warden share predicate (0.2.1): BOTH bands must
     * pass. The height band keeps its original meaning; the range
     * band is its horizontal twin. WARDEN ONLY, as before - every
     * other boss kind counts every attributed hit.
     */
    public static boolean countsTowardShare(BossKind kind,
                                            double attackerX, double attackerY, double attackerZ,
                                            double bossX, double bossY, double bossZ,
                                            double wardenHeightBand, double wardenRangeBand) {
        return kind != BossKind.WARDEN
                || (withinHeightBand(attackerY, bossY, wardenHeightBand)
                    && withinRangeBand(attackerX, attackerZ, bossX, bossZ, wardenRangeBand));
    }

    /**
     * The wither "fought free" rule (0.2.1) - the CAGED-AT-DEATH
     * test, Mike's strict variant, decided Oct 20, 2026 late
     * ("caged for the last stretch = no credit"), on top of the
     * seal-difficulty audit's cage + pocket-melee hole and the
     * adversarial triple check's FLAG 2 hardening.
     *
     * The judged window is the fight's LAST STRETCH, defined in
     * damage space: the final {@code wither.fought-free-window}
     * fraction (settled 0.25) of the boss's rallied health pool,
     * measured in damage dealt. The kill is a CAGE kill when,
     * across that whole final window, BOTH hold (the facts come
     * from the ledger's damage-stamped samples - see
     * EncounterLedger.windowBossHitPlayer /
     * windowMaxDisplacement):
     *
     * - the wither dealt no damage to any PLAYER in the window
     *   (taken-records are player victims only, by adapter
     *   contract; a skull clipping a wolf never counts); and
     * - it never strayed beyond {@code radiusBlocks} (3D) of
     *   where it stood when the window began.
     *
     * A wither caged for its whole life trivially fails the
     * test (the window covers its entire fight), so the base
     * never-fought case needs no separate rule. A wither that
     * fought early but was caged for the final stretch ALSO
     * fails - that is the strict variant Mike picked over the
     * whole-life test. And the guards hold: a caged wither that
     * lands one skull inside the window fought; a free wither
     * crosses the radius in seconds.
     *
     * (The audit's original shape also required suffocation
     * damage; the triple check evaded it with a bedrock pocket
     * that immobilizes the wither with its head in AIR - zero
     * suffocation, same pocket melee - so that requirement is
     * gone: immobilized + never fought back IS the cage,
     * however it was built.)
     *
     * @param windowBossHitPlayer whether the boss dealt damage
     *                            to a player inside the window
     * @param windowMaxDisplacement farthest the boss strayed
     *                            from its window-start position
     */
    public static boolean witherNeverFought(boolean windowBossHitPlayer,
                                            double windowMaxDisplacement,
                                            double radiusBlocks) {
        return !windowBossHitPlayer
                && windowMaxDisplacement <= radiusBlocks + 1e-9;
    }

    /** The elder approval latch's standing (0.2.1, triple-check FLAG 1). */
    public enum LatchState {
        /** No latch is set - nothing to validate. */
        NOT_SET,
        /** Latched, and GuildGate agrees the seal stands broken. */
        VALID,
        /** Latched, but GuildGate shows the seal UNBROKEN (a
         *  GuildGate-side revoke or reset): the latch must be
         *  cleared and the kill judged normally. */
        STALE,
        /** Latched, and GuildGate's state cannot be read (absent,
         *  or a pre-0.3.3 GuildGate without the isBroken API):
         *  trust the latch - the pre-fix behavior - rather than
         *  pretend to knowledge we do not have. */
        UNVERIFIED
    }

    /**
     * The elder latch truth table. BossTrial's elderSealApproved
     * latch short-circuits the per-kill verdict once the trio has
     * completed - correct while GuildGate agrees the seal stands.
     * A /guildgate revoke elder_guardian touches GuildGate only,
     * so without this cross-check the next elder kill of ANY kind
     * (even a 0%-share conduit kill) would re-break the seal on
     * the latch's say-so.
     */
    public static LatchState elderLatch(boolean latchSet, Boolean guildGateBroken) {
        if (!latchSet) {
            return LatchState.NOT_SET;
        }
        if (guildGateBroken == null) {
            return LatchState.UNVERIFIED;
        }
        return guildGateBroken ? LatchState.VALID : LatchState.STALE;
    }

    /**
     * 3D distance between two positions (the presence geometry; the
     * adapter only calls this for same-world pairs).
     */
    public static double distance(double x1, double y1, double z1,
                                  double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** One player's presence snapshot, as plain facts for the judge. */
    public record PresenceFact(boolean online, boolean alive, boolean sameWorld,
                               double distance) {
    }

    /**
     * Count the participants who are present at a death: online,
     * alive, same world, and within range. This count is BOTH the
     * presence rule's input (count >= 1) and the spoils multiplier's
     * participant count (SPOILS RULE: participants in range at the
     * death), so the two rules can never drift apart.
     */
    public static int countPresent(Set<UUID> participants,
                                   java.util.function.Function<UUID, PresenceFact> facts,
                                   double range) {
        return presentParticipants(participants, facts, range).size();
    }

    /**
     * The present participants themselves (0.2.5): the same rule
     * as {@link #countPresent}, returning WHO stands at the fall
     * - the champion list the fairness rule pays (one signature
     * copy each). Sorted by UUID so grants and logs are
     * deterministic; the count is this list's size, so the
     * presence rule, the spoils count and the grant list can
     * never drift apart.
     */
    public static java.util.List<UUID> presentParticipants(
            Set<UUID> participants,
            java.util.function.Function<UUID, PresenceFact> facts,
            double range) {
        java.util.List<UUID> present = new java.util.ArrayList<>();
        for (UUID id : participants) {
            PresenceFact f = facts.apply(id);
            if (f != null && f.online() && f.alive() && f.sameWorld()
                    && f.distance() <= range + 1e-9) {
                present.add(id);
            }
        }
        present.sort(java.util.Comparator.comparing(UUID::toString));
        return java.util.List.copyOf(present);
    }
}
