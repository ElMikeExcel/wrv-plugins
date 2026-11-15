package com.winkreyes.excelsummons;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelSummons unit tests — registry, caps, lifetime, target rules,
 * the 0.1.1 party-protection / retaliation rules, the aggressor
 * memory, and the persistence codec. Pure-logic classes only; the
 * Bukkit glue is exercised by the live ship gates in the build
 * record.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public class SummonsTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    private static final long NOW = 1_700_000_000_000L;

    private static SummonRecord rec(UUID owner, String spell, SummonRecord.Branch branch,
                                    Long expiresAt) {
        return new SummonRecord(UUID.randomUUID(), owner, spell, branch, expiresAt,
                "world", NOW);
    }

    /** 0.1.6: a CAST-origin record (the only origin that holds reserve). */
    private static SummonRecord recCast(UUID owner, String spell, SummonRecord.Branch branch,
                                        Long expiresAt) {
        return new SummonRecord(UUID.randomUUID(), owner, spell, branch, expiresAt,
                "world", NOW, SummonRecord.Origin.CAST);
    }

    /** 0.1.6: an ADOPTED-origin record (holds NO reserve — Mike's ruling). */
    private static SummonRecord recAdopted(UUID owner, String spell, SummonRecord.Branch branch,
                                           Long expiresAt) {
        return new SummonRecord(UUID.randomUUID(), owner, spell, branch, expiresAt,
                "world", NOW, SummonRecord.Origin.ADOPTED);
    }

    // ------------------------------------------------------------ registry

    @Test
    void registryAttributesDamageToTheSummoner() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord skeleton = rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000);
        reg.register(skeleton);
        assertEquals(OWNER, reg.summonerOf(skeleton.entityId()));
        assertNull(reg.summonerOf(UUID.randomUUID()));
        assertTrue(reg.isRegistered(skeleton.entityId()));
    }

    @Test
    void registryCountsFoldTiersIntoTheFamilyCap() {
        SummonRegistry reg = new SummonRegistry();
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(new SummonRecord(UUID.randomUUID(), OWNER, "archer",
                SummonRecord.Branch.COMBAT, NOW + 1, "world", NOW));
        assertEquals(2, reg.countSpell(OWNER, "archer"));
        assertEquals(0, reg.countSpell(OTHER, "archer"));
        assertEquals(2, reg.countBranch(OWNER, SummonRecord.Branch.COMBAT));
        assertEquals(2, reg.countBranchServerWide(SummonRecord.Branch.COMBAT));
        assertEquals(0, reg.countBranchServerWide(SummonRecord.Branch.MOUNT));
    }

    @Test
    void registryExpiryDueSelectsOnlyTimedOutRecords() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord due = rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW - 1);
        SummonRecord live = rec(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 60_000);
        SummonRecord pet = rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        reg.register(due);
        reg.register(live);
        reg.register(pet);
        List<SummonRecord> dueList = reg.expiryDue(NOW);
        assertEquals(1, dueList.size());
        assertEquals(due.entityId(), dueList.get(0).entityId());
    }

    @Test
    void registrySweepDropsOnlyMissingEntries() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord a = rec(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord b = rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        reg.register(a);
        reg.register(b);
        List<SummonRecord> dropped = reg.sweepMissing(Set.of(a.entityId()));
        assertEquals(1, dropped.size());
        assertEquals(b.entityId(), dropped.get(0).entityId());
        assertEquals(1, reg.size());
    }

    @Test
    void registryCodecRoundTripsEveryBranch() {
        SummonRegistry reg = new SummonRegistry();
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000));
        reg.register(rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        reg.register(rec(OTHER, "wolf", SummonRecord.Branch.PACK, null));
        List<Map<String, Object>> snap = reg.snapshot();
        SummonRegistry back = new SummonRegistry();
        back.restore(snap);
        assertEquals(3, back.size());
        assertEquals(OWNER, back.summonerOf(
                snap.stream().filter(m -> "archer".equals(m.get("spell")))
                        .map(m -> UUID.fromString((String) m.get("entity")))
                        .findFirst().orElseThrow()));
        SummonRecord mount = back.all().stream()
                .filter(r -> r.branch() == SummonRecord.Branch.MOUNT).findFirst().orElseThrow();
        assertNull(mount.expiresAtMillis()); // a mount never gains an expiry in the wash
    }

    @Test
    void registryCodecDropsCorruptRowsQuietly() {
        SummonRegistry reg = new SummonRegistry();
        reg.restore(List.of(Map.of("entity", "not-a-uuid", "owner", "also-not")));
        assertEquals(0, reg.size());
    }

    @Test
    void baseKeyFoldsTiers() {
        assertEquals("archer", SummonRecord.baseKey("archer|2"));
        assertEquals("archer", SummonRecord.baseKey("archer"));
        assertEquals("", SummonRecord.baseKey(null));
    }

    // ------------------------------------------------------------ caps

    private static CapPolicy.SpellRule rule(SummonRecord.Branch b, int cap, boolean enforce) {
        return new CapPolicy.SpellRule(b, cap, 0L, enforce);
    }

    private static CapPolicy policy() {
        return new CapPolicy(RankCap.lockedLadder(), 12);
    }

    @Test
    void capPolicyRefusesAtTheSpellCap() {
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(CapPolicy.Refusal.SPELL_CAP,
                policy.check(reg, OWNER, "archer", rule(SummonRecord.Branch.COMBAT, 2, true), "master"));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OTHER, "archer", rule(SummonRecord.Branch.COMBAT, 2, true), "master"));
    }

    @Test
    void spellCapStillBindsLower() {
        // Design §5.1 test 8: per-spell caps bind below the total.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(rec(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        // guardian at its own cap of 1 (total is only 1 of master's 5)
        assertEquals(CapPolicy.Refusal.SPELL_CAP,
                policy.check(reg, OWNER, "guardian", rule(SummonRecord.Branch.COMBAT, 1, true), "master"));
        // a different family is fine
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "archer", rule(SummonRecord.Branch.COMBAT, 2, true), "master"));
    }

    @Test
    void capPolicyRefusesAtTheServerCeiling() {
        CapPolicy policy = new CapPolicy(RankCap.lockedLadder(), 4);
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 4; i++) {
            reg.register(rec(OTHER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        }
        assertEquals(CapPolicy.Refusal.SERVER_CEILING,
                policy.check(reg, OWNER, "guardian", rule(SummonRecord.Branch.COMBAT, 1, true), "master"));
    }

    @Test
    void serverCeilingUnchanged() {
        // Design §5.1 test 9: the ceiling is combat-only, as in 0.1.3.
        CapPolicy policy = new CapPolicy(RankCap.lockedLadder(), 4);
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 4; i++) {
            reg.register(rec(OTHER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        }
        assertEquals(CapPolicy.Refusal.SERVER_CEILING,
                policy.check(reg, OWNER, "guardian", rule(SummonRecord.Branch.COMBAT, 1, true), "master"));
        // a companion cast by a fresh owner at counted total 0 is untouched by it
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "master"));
    }

    // --------------------------------- 0.1.4 rank-scaled total cap (§3)

    @Test
    void totalCapCountsAcrossBranches() {
        // Design §5.1 test 6: wolf (PACK) + cat (COMPANION) +
        // archer (COMBAT) all count in ONE total; the mount does not.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(3, reg.countCounted(OWNER));
        // an apprentice (cap 3) is full: any counted branch is refused
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "apprentice"));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "bruiser", rule(SummonRecord.Branch.COMBAT, 2, true), "apprentice"));
        // a master (cap 5) still has room in the same registry state
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "master"));
        // the mount is not counted and is never total-cap-refused
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "mount", rule(SummonRecord.Branch.MOUNT, 1, true), "apprentice"));
        // ...but its own per-spell cap of 1 still binds
        reg.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        assertEquals(3, reg.countCounted(OWNER), "the mount never enters the counted total");
        assertEquals(CapPolicy.Refusal.SPELL_CAP,
                policy.check(reg, OWNER, "mount", rule(SummonRecord.Branch.MOUNT, 1, true), "master"));
    }

    @Test
    void totalCapBindsWolfDespiteEnforceCapFalse() {
        // Design §5.1 test 7 (§3.2's trap, pinned): enforce-cap
        // false exempts the wolf's FAMILY cap (Magic owns it) — it
        // must never exempt Mike's rank-scaled total cap.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "wolf", rule(SummonRecord.Branch.PACK, 5, false), "apprentice"));
    }

    @Test
    void wolfExemptionPinReplaced() {
        // Design §5.1 test 10 — re-pin of 0.1.3's
        // capPolicyHonorsTheWolfExemption. BOTH halves asserted:
        // (a) the spell-cap check in isolation still exempts the
        // wolf (enforce-cap false): under a cap map whose rungs
        // cannot bind, 6 wolves refuse nothing; (b) the unified
        // check refuses the same registry at the real ladder.
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 6; i++) {
            reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        }
        CapPolicy unbound = new CapPolicy(Map.of("master", 99), 12);
        assertEquals(CapPolicy.Refusal.NONE,
                unbound.check(reg, OWNER, "wolf", rule(SummonRecord.Branch.PACK, 5, false), "master"),
                "half (a): no SPELL_CAP for the wolf, ever — Magic's class owns that cap");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy().check(reg, OWNER, "wolf", rule(SummonRecord.Branch.PACK, 5, false), "master"),
                "half (b): 6 counted wolves are over even a master's 5 — TOTAL_CAP");
    }

    @Test
    void rankLadderIsLockedAndFailsLowest() {
        // Mike's LOCKED ladder (Nov 10, 2026, 10:37 AM):
        // Beginner 1 · Student 2 · Apprentice 3 · Master 5.
        CapPolicy policy = policy();
        assertEquals(1, policy.capForRank("beginner"));
        assertEquals(2, policy.capForRank("student"));
        assertEquals(3, policy.capForRank("apprentice"));
        assertEquals(5, policy.capForRank("master"));
        assertEquals(5, policy.capForRank("MASTER"), "rank keys are case-insensitive");
        // an unreadable rank fails to the LOWEST cap, never higher
        assertEquals(1, policy.capForRank(null));
        assertEquals(1, policy.capForRank("bogus"));
        assertEquals(1, new CapPolicy(Map.of(), 12).capForRank("master"),
                "an empty cap map fails to 1, never to unlimited");
        // ...and the check honors the fail-safe: counted 1 + unknown rank = full
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "cat", rule(SummonRecord.Branch.COMPANION, 2, true), null));
    }

    @Test
    void earnedRankThresholdsMirrorMagicGate() {
        // The derivation mirrors ExcelMagicGate EarnedRank's locked
        // thresholds: Student 100, Apprentice 400, Master 2,000
        // credited casts; below 100 is beginner.
        assertEquals("beginner", RankCap.rankForCredited(0));
        assertEquals("beginner", RankCap.rankForCredited(99));
        assertEquals("student", RankCap.rankForCredited(100));
        assertEquals("student", RankCap.rankForCredited(399));
        assertEquals("apprentice", RankCap.rankForCredited(400));
        assertEquals("apprentice", RankCap.rankForCredited(1999));
        assertEquals("master", RankCap.rankForCredited(2000));
        assertEquals("master", RankCap.rankForCredited(17821));
    }

    @Test
    void studentIsRefusedAtTheirRankCap() {
        // A student (cap 2) with a wolf + cat out is refused a
        // third counted summon, and the message names the rank,
        // the limit, and what is currently summoned (10:34 ruling).
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        // CAST-origin pets: dismissible, so the normal variant of
        // the message (with the dismissal advice) is the true one
        reg.register(recOrigin(OWNER, "wolf", SummonRecord.Branch.PACK,
                SummonRecord.Origin.CAST));
        reg.register(recOrigin(OWNER, "cat", SummonRecord.Branch.COMPANION,
                SummonRecord.Origin.CAST));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OTHER, "archer", rule(SummonRecord.Branch.COMBAT, 2, true), "student"),
                "another student with nothing out casts freely");
        CapPolicy.Refusal r = policy.check(reg, OWNER, "archer",
                rule(SummonRecord.Branch.COMBAT, 2, true), "student");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP, r);
        assertEquals("Summon limit reached — your rank (Student) allows 2 summons, "
                        + "and you have 2 out: Wolf, Cat. Dismiss one (or let a combat "
                        + "summon expire) before summoning another.",
                policy.refusalMessage(r, reg, OWNER, "archer",
                        rule(SummonRecord.Branch.COMBAT, 2, true), "student"));
    }

    @Test
    void masterFiveCheapSummonsLegalSixthRefused() {
        // Mike's 10:35 shape: a master running 5 cheap summons is
        // legal; the 6th counted cast hits TOTAL_CAP (not a
        // per-branch rule).
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "bruiser", rule(SummonRecord.Branch.COMBAT, 2, true), "master"),
                "the 5th counted summon is legal for a master");
        reg.register(recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(5, reg.countCounted(OWNER));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "master"));
    }

    @Test
    void overCapAtLoginIsGrandfatheredNeverTrimmed() {
        // §3.3: a player over their CURRENT rank cap keeps every
        // summon; only NEW counted casts are refused until the
        // total falls below the cap. The bridge never trims.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 5; i++) reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        assertEquals(6, reg.countCounted(OWNER), "6 counted: over even a master's 5");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "master"));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "student"),
                "the same set is over a student's 2 as well — refused, not trimmed");
        assertEquals(6, reg.size(), "the check itself removes nothing, ever");
    }

    // ----------------------- 0.1.9 cap fix: CAST-only total cap
    // (P-SUMMONS-CAP-1, Mike, Nov 14, 2026, ~12:48 AM CST — the
    // rank-scaled total cap counts CAST summons only; adopted
    // pets occupy no cap slot and hold no mana.)

    @Test
    void castSummonsAtCapStillBlocked() {
        // (a) A player AT the cast cap is still refused: an
        // apprentice (cap 3) with 3 CAST summons out — across
        // branches — is TOTAL_CAP-refused for any further counted
        // cast, and the message counts/names CAST summons only.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(3, reg.countCounted(OWNER));
        CapPolicy.Refusal r = policy.check(reg, OWNER, "familiar",
                rule(SummonRecord.Branch.COMPANION, 3, true), "apprentice");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP, r);
        assertEquals("Summon limit reached — your rank (Apprentice) allows 3 summons, "
                        + "and you have 3 out: Wolf, Cat, Archer. Dismiss one (or let a "
                        + "combat summon expire) before summoning another.",
                policy.refusalMessage(r, reg, OWNER, "familiar",
                        rule(SummonRecord.Branch.COMPANION, 3, true), "apprentice"));
    }

    @Test
    void adoptedPetsOverCapDoNotBlockCasts() {
        // (b) THE Nov 14 incident, inverted: Mike's 8 adopted
        // wolves against an Apprentice cap of 3 blocked every
        // cast. Under the new law the adopted pack occupies ZERO
        // slots: the player casts freely up to the CAST cap (3),
        // and only the 4th CAST summon is refused. The refusal
        // message names the cast summons, never the adopted pack.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 8; i++) {
            reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        }
        assertEquals(8, reg.size(), "all 8 adopted wolves stay registered");
        assertEquals(0, reg.countCounted(OWNER),
                "adopted pets occupy no cap slot");
        assertEquals(List.of(), CapPolicy.countedNames(reg, OWNER),
                "adopted pets are never named as occupying the cap");
        // casts 1..3 are legal (each check runs at the live count)
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "archer", rule(SummonRecord.Branch.COMBAT, 2, true), "apprentice"),
                "cast 1 of 3: legal despite 8 adopted wolves");
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        assertEquals(2, reg.countCounted(OWNER));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "apprentice"),
                "cast 3 of 3: still legal");
        reg.register(recCast(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        assertEquals(3, reg.countCounted(OWNER), "the CAST cap is what fills");
        CapPolicy.Refusal r = policy.check(reg, OWNER, "bruiser",
                rule(SummonRecord.Branch.COMBAT, 2, true), "apprentice");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP, r,
                "the 4th CAST summon is refused — by cast count, not by the pack");
        String msg = policy.refusalMessage(r, reg, OWNER, "bruiser",
                rule(SummonRecord.Branch.COMBAT, 2, true), "apprentice");
        assertTrue(msg.contains("you have 3 out: Archer, Cat, Familiar"),
                "the message names the 3 cast summons only: " + msg);
        assertFalse(msg.contains("Wolf"),
                "the adopted wolves are never blamed for the refusal: " + msg);
    }

    @Test
    void adoptedPetsConsumeNoMana() {
        // (c) Mana behavior UNCHANGED by the cap fix (0.1.6
        // ruling): adopted records hold 0% — raw total, applied
        // total, and desired modifiers all empty — even for a
        // pack far over the count cap; a CAST wolf beside them
        // still holds its 10%.
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 8; i++) {
            reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        }
        assertEquals(0, ReservePolicy.totalPercent(reg, OWNER, PCT14),
                "adopted wolves hold no mana (raw)");
        assertEquals(0, ReservePolicy.appliedPercent(reg, OWNER, PCT14),
                "adopted wolves hold no mana (applied)");
        assertTrue(ReservePolicy.desiredModifiers(reg, OWNER, PCT14).isEmpty(),
                "adopted wolves produce no reserve modifiers");
        assertTrue(ReservePolicy.reserveCounts(reg, OWNER, PCT14).isEmpty(),
                "adopted wolves produce no reserve counts");
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals(10, ReservePolicy.totalPercent(reg, OWNER, PCT14),
                "the one CAST wolf still holds its 10% — cast behavior unchanged");
        assertEquals(10, ReservePolicy.appliedPercent(reg, OWNER, PCT14));
    }

    // ------------------------------------------------------------ lifetime

    @Test
    void lifetimeRemovesAnExpiredCombatSummon() {
        SummonRecord rec = rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW - 1);
        assertEquals(LifetimePolicy.Decision.REMOVE,
                LifetimePolicy.decide(rec, NOW, false, false));
    }

    @Test
    void lifetimeNeverRemovesWhileRidden() {
        SummonRecord rec = rec(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW - 1);
        assertEquals(LifetimePolicy.Decision.DEFER_RIDDEN,
                LifetimePolicy.decide(rec, NOW, true, false));
    }

    @Test
    void lifetimeKeepsPetsAndMountsForever() {
        assertEquals(LifetimePolicy.Decision.KEEP, LifetimePolicy.decide(
                rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null), NOW, false, false));
        assertEquals(LifetimePolicy.Decision.KEEP, LifetimePolicy.decide(
                rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null), NOW, false, false));
        // even at shutdown: pets and mounts persist in the world
        assertEquals(LifetimePolicy.Decision.KEEP, LifetimePolicy.decide(
                rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null), NOW, false, true));
    }

    @Test
    void lifetimeRemovesCombatSummonsAtShutdownButNotBeforeTheirTime() {
        assertEquals(LifetimePolicy.Decision.REMOVE, LifetimePolicy.decide(
                rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000),
                NOW, false, true));
        assertEquals(LifetimePolicy.Decision.KEEP, LifetimePolicy.decide(
                rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000),
                NOW, false, false));
        assertNull(LifetimePolicy.expiresAt(NOW, 0));
        assertEquals(NOW + 90_000, LifetimePolicy.expiresAt(NOW, 90_000));
        assertTrue(LifetimePolicy.leavesWithCaster(
                rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1)));
        assertFalse(LifetimePolicy.leavesWithCaster(
                rec(OWNER, "wolf", SummonRecord.Branch.PACK, null)));
    }

    // ------------------------------------------------------------ target rules

    private static TargetRules.Candidate cand(boolean hostile, boolean player, boolean alive,
                                              boolean sameWorld, double dist) {
        return new TargetRules.Candidate(UUID.randomUUID(), hostile, player, alive,
                sameWorld, dist);
    }

    @Test
    void targetRulesNeverEngageAPlayer() {
        // The MOB priorities still never engage a player (0.1.1 kept
        // this half of the 0.1.0 absolute rule): a player candidate
        // fails engageable() by construction and choose() picks none.
        // The single exception lives in chooseWithRetaliation and is
        // tested there — it opens only for the recorded aggressor.
        TargetRules.Candidate player = cand(false, true, true, true, 5);
        assertFalse(TargetRules.engageable(player, 32));
        assertNull(TargetRules.choose(player, player, player, 32));
    }

    @Test
    void targetRulesRequireALivingNearbyHostile() {
        assertTrue(TargetRules.engageable(cand(true, false, true, true, 10), 32));
        assertFalse(TargetRules.engageable(cand(true, false, false, true, 10), 32)); // dead
        assertFalse(TargetRules.engageable(cand(true, false, true, false, 10), 32)); // other world
        assertFalse(TargetRules.engageable(cand(true, false, true, true, 40), 32));  // too far
        assertFalse(TargetRules.engageable(cand(false, false, true, true, 10), 32)); // passive
    }

    @Test
    void targetRulesPriorityIsAttackerThenOwnTargetThenParty() {
        TargetRules.Candidate attacker = cand(true, false, true, true, 8);
        TargetRules.Candidate own = cand(true, false, true, true, 8);
        TargetRules.Candidate party = cand(true, false, true, true, 8);
        assertEquals(attacker.entityId, TargetRules.choose(attacker, own, party, 32));
        assertEquals(own.entityId, TargetRules.choose(null, own, party, 32));
        assertEquals(party.entityId, TargetRules.choose(null, null, party, 32));
        assertNull(TargetRules.choose(null, null, null, 32));
    }

    @Test
    void targetRulesLeashOnlyWhenTargetlessAndFar() {
        assertTrue(TargetRules.shouldLeashHome(40, 32, false));
        assertFalse(TargetRules.shouldLeashHome(40, 32, true));
        assertFalse(TargetRules.shouldLeashHome(10, 32, false));
    }

    @Test
    void damageScrubProtectsPlayersPetsAndPassiveAnimals() {
        assertFalse(TargetRules.damageAllowed(true, false, false));  // player
        assertFalse(TargetRules.damageAllowed(false, true, false));  // passive animal
        assertFalse(TargetRules.damageAllowed(false, false, true));  // owned pet
        assertTrue(TargetRules.damageAllowed(false, false, false));  // hostile mob
    }

    // -------------------------------------------- 0.1.1 party rules

    private static final UUID PARTY_MEMBER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    @Test
    void relationMappingProtectsOwnerPartyAndUnknown() {
        // The fail-soft invariant: only a KNOWN non-party answer makes
        // a stranger. Owner, party member, and unreadable party state
        // all protect the player.
        assertEquals(TargetRules.Relation.PROTECTED,
                TargetRules.relationFor(true, PartyProvider.Membership.NOT_PARTY));
        assertEquals(TargetRules.Relation.PROTECTED,
                TargetRules.relationFor(false, PartyProvider.Membership.PARTY));
        assertEquals(TargetRules.Relation.PROTECTED,
                TargetRules.relationFor(false, PartyProvider.Membership.UNKNOWN));
        assertEquals(TargetRules.Relation.STRANGER,
                TargetRules.relationFor(false, PartyProvider.Membership.NOT_PARTY));
    }

    @Test
    void partyHitIsCancelledAndNeverRecorded() {
        // The listener's composition for a party member's hit (Mike's
        // rule): the incoming-hit rule says CANCEL, and the aggressor
        // memory is written ONLY on the LAND_AND_RECORD branch — so
        // after a protected hit the memory is still empty, the summon
        // cannot retaliate, and its damage to that player stays barred.
        AggressorMemory mem = new AggressorMemory(30_000);
        UUID summon = UUID.randomUUID();
        TargetRules.Relation rel = TargetRules.relationFor(false,
                PartyProvider.Membership.PARTY);
        assertEquals(TargetRules.IncomingHit.CANCEL_PROTECTED,
                TargetRules.incomingPlayerHit(rel));
        if (TargetRules.incomingPlayerHit(rel) == TargetRules.IncomingHit.LAND_AND_RECORD) {
            mem.record(summon, PARTY_MEMBER, NOW);
        }
        assertEquals(0, mem.size());
        assertFalse(mem.isAggressor(summon, PARTY_MEMBER, NOW));
        assertFalse(TargetRules.playerDamageAllowed(rel,
                mem.isAggressor(summon, PARTY_MEMBER, NOW)));
        // An UNKNOWN-status hitter gets exactly the party treatment.
        assertEquals(TargetRules.IncomingHit.CANCEL_PROTECTED,
                TargetRules.incomingPlayerHit(TargetRules.relationFor(false,
                        PartyProvider.Membership.UNKNOWN)));
    }

    @Test
    void strangerHitLandsRecordsAndUnlocksRetaliation() {
        AggressorMemory mem = new AggressorMemory(30_000);
        UUID summon = UUID.randomUUID();
        TargetRules.Relation rel = TargetRules.relationFor(false,
                PartyProvider.Membership.NOT_PARTY);
        assertEquals(TargetRules.IncomingHit.LAND_AND_RECORD,
                TargetRules.incomingPlayerHit(rel));
        mem.record(summon, STRANGER, NOW);
        assertTrue(mem.isAggressor(summon, STRANGER, NOW));
        // retaliation target allowed, and the summon's damage to that
        // same stranger is allowed — and to nobody else.
        assertTrue(TargetRules.playerDamageAllowed(rel, true));
        assertFalse(TargetRules.playerDamageAllowed(rel, false));
    }

    @Test
    void retaliationChoosesTheRecordedAggressorFirst() {
        TargetRules.Candidate aggressor = cand(false, true, true, true, 6);
        TargetRules.Candidate attacker = cand(true, false, true, true, 8);
        TargetRules.Candidate own = cand(true, false, true, true, 8);
        TargetRules.Candidate party = cand(true, false, true, true, 8);
        assertEquals(aggressor.entityId, TargetRules.chooseWithRetaliation(
                aggressor, attacker, own, party, 32));
        // with no aggressor, the mob priorities decide as before
        assertEquals(attacker.entityId, TargetRules.chooseWithRetaliation(
                null, attacker, own, party, 32));
    }

    @Test
    void retaliationFallsThroughWhenTheAggressorIsNotEngageable() {
        TargetRules.Candidate dead = cand(false, true, false, true, 6);
        TargetRules.Candidate far = cand(false, true, true, true, 40);
        TargetRules.Candidate otherWorld = cand(false, true, true, false, 6);
        TargetRules.Candidate own = cand(true, false, true, true, 8);
        assertEquals(own.entityId, TargetRules.chooseWithRetaliation(dead, null, own, null, 32));
        assertEquals(own.entityId, TargetRules.chooseWithRetaliation(far, null, own, null, 32));
        assertNull(TargetRules.chooseWithRetaliation(otherWorld, null, null, null, 32));
        // a player standing in a MOB priority slot is never picked,
        // even while a valid aggressor exists elsewhere unrecorded
        TargetRules.Candidate playerInMobSlot = cand(false, true, true, true, 5);
        assertNull(TargetRules.choose(playerInMobSlot, playerInMobSlot, playerInMobSlot, 32));
    }

    @Test
    void bystanderIsNeverTargetedAndNeverDamaged() {
        // A bystander never hit the summon: no memory entry exists, so
        // every door stays shut — retaliation candidate fails (the
        // engine passes a candidate in the aggressor slot ONLY from
        // the memory), and playerDamageAllowed is false even for a
        // known stranger while they are not the recorded aggressor.
        AggressorMemory mem = new AggressorMemory(30_000);
        UUID summon = UUID.randomUUID();
        UUID bystander = UUID.randomUUID();
        mem.record(summon, STRANGER, NOW); // someone ELSE hit it
        assertFalse(mem.isAggressor(summon, bystander, NOW));
        assertFalse(TargetRules.playerDamageAllowed(TargetRules.Relation.STRANGER,
                mem.isAggressor(summon, bystander, NOW)));
    }

    @Test
    void aggressorMemoryExpiresAndRefreshes() {
        AggressorMemory mem = new AggressorMemory(30_000);
        UUID summon = UUID.randomUUID();
        mem.record(summon, STRANGER, NOW);
        assertEquals(STRANGER, mem.aggressorOf(summon, NOW + 29_000));
        assertNull(mem.aggressorOf(summon, NOW + 31_000)); // window passed
        assertEquals(0, mem.size()); // expired entries drop as read
        // a fresh hit inside the window extends the memory
        mem.record(summon, STRANGER, NOW);
        mem.record(summon, STRANGER, NOW + 20_000);
        assertEquals(STRANGER, mem.aggressorOf(summon, NOW + 45_000));
        assertNull(mem.aggressorOf(summon, NOW + 51_000));
    }

    @Test
    void aggressorMemoryClearPaths() {
        AggressorMemory mem = new AggressorMemory(30_000);
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        mem.record(s1, STRANGER, NOW);
        mem.record(s2, STRANGER, NOW);
        // the latest hit wins: one aggressor per summon
        mem.record(s1, second, NOW + 1000);
        assertEquals(second, mem.aggressorOf(s1, NOW + 1000));
        assertFalse(mem.isAggressor(s1, STRANGER, NOW + 1000));
        // the summon dies/despawns -> its memory goes with it
        mem.clear(s1);
        assertNull(mem.aggressorOf(s1, NOW));
        assertEquals(STRANGER, mem.aggressorOf(s2, NOW));
        // the aggressor dies/logs off -> forgotten by every summon
        mem.clearAggressor(STRANGER);
        assertEquals(0, mem.size());
    }

    // -------------------------------------------- nameplate (0.1.1)

    @Test
    void nameplateFormatsWholeAndFractionalHealth() {
        assertEquals("Archer — 18/20", Nameplate.display("Archer", 18.0, 20.0));
        assertEquals("Bruiser — 17.5/24", Nameplate.display("Bruiser", 17.5, 24.0));
        assertEquals("18", Nameplate.formatHp(18.0));
        assertEquals("17.5", Nameplate.formatHp(17.5));
        assertEquals("0", Nameplate.formatHp(0.0));
    }

    @Test
    void nameplateStripsOnlyItsOwnSuffix() {
        assertEquals("Archer", Nameplate.stripHealthSuffix("Archer — 18/20"));
        assertEquals("Archer", Nameplate.stripHealthSuffix("Archer — 17.5/20"));
        assertEquals("Sir Barksalot", Nameplate.stripHealthSuffix("Sir Barksalot"));
        assertEquals("Odd — name", Nameplate.stripHealthSuffix("Odd — name"));
        assertNull(Nameplate.stripHealthSuffix(null));
    }

    @Test
    void nameplateBaseComposesWithExistingNames() {
        // a name that is already there (Magic's or a name tag) wins
        assertEquals("Sir Barksalot", Nameplate.baseName("Sir Barksalot", "wolf"));
        // our own previous render: the base survives under the suffix
        assertEquals("Archer", Nameplate.baseName("Archer — 3/20", "archer"));
        // unnamed summons fall back to the spell's display name
        assertEquals("Archer", Nameplate.baseName(null, "archer"));
        assertEquals("Happy Ghast", Nameplate.baseName(null, "mount"));
        assertEquals("Revenant", Nameplate.baseName("", "revenant"));
    }

    @Test
    void nameplateDisplayNamesCoverTheTree() {
        assertEquals("Guardian", Nameplate.spellDisplayName("guardian"));
        assertEquals("Ghast", Nameplate.spellDisplayName("ghast"));
        assertEquals("Familiar", Nameplate.spellDisplayName("familiar"));
        assertEquals("Zeta", Nameplate.spellDisplayName("zeta")); // fallback capitalizes
        assertEquals("Summon", Nameplate.spellDisplayName(null));
    }

    // -------------------------------------------- 0.1.2 Guardian taunt

    @Test
    void tauntStealsHostilesOffTheOwnerAndPartyMembers() {
        // engaged with the owner, in radius: stolen onto the Guardian
        assertTrue(TauntRules.shouldTaunt(true, true, true, 10, 16, true, true, false));
        // engaged with a party member, edge of radius: stolen too
        assertTrue(TauntRules.shouldTaunt(true, true, true, 16, 16, true, true, false));
    }

    @Test
    void tauntNeverPullsUnengagedOrStrangerFights() {
        // no target at all — an unengaged pack is never pulled
        assertFalse(TauntRules.shouldTaunt(true, true, true, 10, 16, false, false, false));
        // chasing a NON-party player: that fight is not ours to steal
        assertFalse(TauntRules.shouldTaunt(true, true, true, 10, 16, true, false, false));
        // fighting another mob (not a player): untouched
        assertFalse(TauntRules.shouldTaunt(true, true, true, 10, 16, false, true, false));
        // already on the Guardian: nothing to do
        assertFalse(TauntRules.shouldTaunt(true, true, true, 10, 16, true, true, true));
        // out of radius / dead / other world / not a Mob: untouched
        assertFalse(TauntRules.shouldTaunt(true, true, true, 20, 16, true, true, false));
        assertFalse(TauntRules.shouldTaunt(true, false, true, 10, 16, true, true, false));
        assertFalse(TauntRules.shouldTaunt(true, true, false, 10, 16, true, true, false));
        assertFalse(TauntRules.shouldTaunt(false, true, true, 10, 16, true, true, false));
    }

    @Test
    void guardianTankSkinReducesEveryHit() {
        assertEquals(60.0, TauntRules.guardianDamageTaken(100, 0.40), 1e-9);
        assertEquals(100.0, TauntRules.guardianDamageTaken(100, 0.0), 1e-9);
        assertEquals(0.0, TauntRules.guardianDamageTaken(0, 0.40), 1e-9);
        // a nonsense reduction is clamped, never a heal or a one-shot
        assertEquals(5.0, TauntRules.guardianDamageTaken(100, 2.0), 1e-9);
    }

    // -------------------------------------------- 0.1.2 mana reserve

    private static final Map<String, Integer> PCT = Map.of(
            "archer", 10, "bruiser", 15, "guardian", 25, "revenant", 20, "ghast", 25);

    /** The 0.1.4 percent table (config.yml summon-reserve-percent):
     *  the 0.1.2 combat rows plus wolf 10 / familiar 5 / cat 5 /
     *  mount 10 (design §2.2). */
    private static final Map<String, Integer> PCT14 = Map.of(
            "archer", 10, "bruiser", 15, "guardian", 25, "revenant", 20, "ghast", 25,
            "wolf", 10, "familiar", 5, "cat", 5, "mount", 10);

    @Test
    void reserveAppliesOnCastAndStacksAcrossSummons() {
        SummonRegistry reg = new SummonRegistry();
        assertEquals(0, ReservePolicy.totalPercent(reg, OWNER, PCT));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000));
        assertEquals(10, ReservePolicy.totalPercent(reg, OWNER, PCT));
        reg.register(recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 120_000));
        // two summons stack additively — the same way Magic sums boosts
        assertEquals(25, ReservePolicy.totalPercent(reg, OWNER, PCT));
        assertEquals(Map.of("wrv_summon_reserve_archer", 10,
                        "wrv_summon_reserve_bruiser", 15),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT));
    }

    @Test
    void reserveCountTiersCarryTwoOfAFamily() {
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(20, ReservePolicy.totalPercent(reg, OWNER, PCT));
        // one count-tiered key stands for both archers (Magic refuses
        // a second copy of a duration-less modifier key)
        assertEquals(Map.of("wrv_summon_reserve_archer2", 20),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT));
        assertEquals("wrv_summon_reserve_archer", ReservePolicy.modifierKey("archer", 1));
        assertEquals("wrv_summon_reserve_archer2", ReservePolicy.modifierKey("archer", 2));
        assertNull(ReservePolicy.modifierKey("archer", 0));
    }

    @Test
    void reserveReleasesOnEveryDespawnPath() {
        // Every despawn path — expiry, death, dismissal, logout,
        // chunk re-check — ends in registry.unregister; the reserve
        // is re-derived from the registry after each mutation, so
        // release is proven by unregistering the way each path does.
        SummonRegistry reg = new SummonRegistry();
        SummonRecord g = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord gh = recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 1);
        reg.register(g);
        reg.register(gh);
        assertEquals(50, ReservePolicy.totalPercent(reg, OWNER, PCT));
        reg.unregister(g.entityId()); // guardian dies / expires / is dismissed
        assertEquals(25, ReservePolicy.totalPercent(reg, OWNER, PCT));
        assertEquals(Map.of("wrv_summon_reserve_ghast", 25),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT));
        reg.unregister(gh.entityId()); // the caster's last summon leaves
        assertEquals(0, ReservePolicy.totalPercent(reg, OWNER, PCT));
        assertTrue(ReservePolicy.desiredModifiers(reg, OWNER, PCT).isEmpty());
    }

    @Test
    void reserveFullSetLandsInTheToggleBand() {
        // The heaviest legal set (guardian + ghast + revenant) = 70%
        // of the pool held — inside the 50–75 band Mike's anti-spam
        // ruling prices at roughly a running toggle.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "revenant", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(70, ReservePolicy.totalPercent(reg, OWNER, PCT));
    }

    @Test
    void reserveCeilingRefusesCleanly() {
        // 70 already held; one more bruiser (15) would make 85 > 75
        assertEquals(ReservePolicy.Refusal.RESERVE_CEILING,
                ReservePolicy.check(70, 15, 75, 200, 200, 20));
        // exactly at the ceiling is allowed
        assertEquals(ReservePolicy.Refusal.NONE,
                ReservePolicy.check(50, 25, 75, 200, 200, 20));
    }

    @Test
    void reserveFloorRespectsTogglesTheBridgeDoesNotOwn() {
        // Cloak is running: effective max is 50 of a 200 base (25%).
        // A ghast (25% of base = 50) would leave 0 usable — refused.
        assertEquals(ReservePolicy.Refusal.RESERVE_FLOOR,
                ReservePolicy.check(0, 25, 75, 50, 200, 20));
        // Boon running instead (100 of 200 usable): an archer (20)
        // leaves 80 of 200 = 40% usable — allowed.
        assertEquals(ReservePolicy.Refusal.NONE,
                ReservePolicy.check(0, 10, 75, 100, 200, 20));
        // no toggles at all: a guardian leaves 75% usable — allowed.
        assertEquals(ReservePolicy.Refusal.NONE,
                ReservePolicy.check(0, 25, 75, 200, 200, 20));
    }

    // -------------------------------------------- 0.1.3 console admin

    @Test
    void adminGateMatrix() {
        assertTrue(AdminPolicy.mayUseAdmin(true, false), "console is explicitly allowed for the admin forms");
        assertTrue(AdminPolicy.mayUseAdmin(true, true), "console with the node allowed");
        assertTrue(AdminPolicy.mayUseAdmin(false, true), "an operator holding excelsummons.admin is allowed");
        assertFalse(AdminPolicy.mayUseAdmin(false, false), "a player without the admin node is refused");
        assertEquals("excelsummons.admin", AdminPolicy.PERMISSION,
                "the gate is the EXISTING admin node (audit §4), not a new one");
    }

    @Test
    void adminRoutingBothVerbs() {
        var list = AdminPolicy.route(new String[]{"admin", "list", "Patient"}, true, false);
        assertEquals(AdminPolicy.Route.ADMIN_LIST, list.route());
        assertEquals("Patient", list.target(), "the target is carried verbatim");
        var dis = AdminPolicy.route(new String[]{"admin", "dismiss", "Patient"}, true, false);
        assertEquals(AdminPolicy.Route.ADMIN_DISMISS, dis.route());
        assertEquals("Patient", dis.target());
        assertFalse(dis.all(), "no scope token means mount only, exactly the self dismiss");
        var disAll = AdminPolicy.route(new String[]{"admin", "dismiss", "Patient", "all"}, true, false);
        assertEquals(AdminPolicy.Route.ADMIN_DISMISS, disAll.route());
        assertTrue(disAll.all(), "the all token carries the combat scope");
        // operator player with the node routes identically; case-insensitive throughout
        assertEquals(AdminPolicy.Route.ADMIN_LIST,
                AdminPolicy.route(new String[]{"ADMIN", "List", "Patient"}, false, true).route());
        assertEquals(AdminPolicy.Route.ADMIN_DISMISS,
                AdminPolicy.route(new String[]{"Admin", "DISMISS", "Patient", "ALL"}, false, true).route());
        assertTrue(AdminPolicy.route(new String[]{"Admin", "DISMISS", "Patient", "ALL"},
                false, true).all());
    }

    @Test
    void adminDenialPrecedesUsageAndTargets() {
        // a non-admin player is DENIED even for a well-formed call, and
        // even for a malformed one — the denial leaks no usage shape
        // and no target is ever resolved for them
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"admin", "list", "Patient"}, false, false).route());
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"admin", "dismiss", "Patient"}, false, false).route());
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"admin", "dismiss", "Patient", "all"}, false, false).route());
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"admin", "list"}, false, false).route());
    }

    @Test
    void adminUsageErrors() {
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "list"}, true, false).route(), "missing target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "dismiss"}, true, false).route(), "missing target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "list", "Patient", "extra"}, true, false).route(),
                "extra arguments are a usage error, never a partial act");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "list", " "}, true, false).route(), "blank target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "dismiss", " "}, true, false).route(), "blank target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "dismiss", "Patient", "some"}, true, false).route(),
                "a dismiss scope token other than 'all' is a usage error, never a silent mount-only act");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"admin", "dismiss", "Patient", "all", "extra"}, true, false).route(),
                "anything past the all token is a usage error");
    }

    @Test
    void legacySurfaceRoutesUnchanged() {
        // every pre-0.1.3 /summons shape stays LEGACY for every sender kind
        String[][] shapes = {{}, {"list"}, {"dismiss"}, {"dismiss", "all"},
                {"admin"}, {"admin", "reload"}, {"admin", "bogus"},
                {"bogus"}, {"listx"}, {"adminx", "list", "Patient"}};
        for (String[] shape : shapes) {
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, true, false).route(),
                    "console keeps legacy for " + java.util.Arrays.toString(shape));
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, false, false).route(),
                    "plain player keeps legacy for " + java.util.Arrays.toString(shape));
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, false, true).route(),
                    "operator keeps legacy for " + java.util.Arrays.toString(shape));
        }
        assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(null, true, false).route(),
                "a null arg array is legacy (defensive)");
        assertEquals(AdminPolicy.Route.LEGACY,
                AdminPolicy.route(new String[]{"admin"}, true, false).route(),
                "a bare admin with no verb is legacy (the pre-0.1.3 admin usage)");
    }

    @Test
    void adminTargetNameMatchingIsExactCaseInsensitive() {
        assertTrue(AdminPolicy.nameMatches("Patient", "Patient"));
        assertTrue(AdminPolicy.nameMatches("Patient", "patient"));
        assertTrue(AdminPolicy.nameMatches("PATIENT", "patient"));
        assertFalse(AdminPolicy.nameMatches("Patient", "Pat"), "no prefix matching");
        assertFalse(AdminPolicy.nameMatches("Patient", "Patient2"), "no partial matching");
        assertFalse(AdminPolicy.nameMatches("Patient", "Other"));
        assertFalse(AdminPolicy.nameMatches(null, "Patient"));
        assertFalse(AdminPolicy.nameMatches("Patient", null));
        assertFalse(AdminPolicy.nameMatches(null, null));
    }

    @Test
    void adminDismissSemanticsOnRealRegistry() {
        // The exercised state proof: AdminPolicy.dismissTargets is the
        // selection SummonsListener.dismiss runs live. Mount-only takes
        // the mount and nothing else; all takes mount + combat;
        // companions and pack are family and survive both; the other
        // owner's records are never in scope (ofOwner scoping).
        SummonRegistry reg = new SummonRegistry();
        SummonRecord mount = rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null);
        SummonRecord archer = rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000);
        SummonRecord guardian = rec(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 180_000);
        SummonRecord cat = rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        SummonRecord wolf = rec(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord otherMount = rec(OTHER, "mount", SummonRecord.Branch.MOUNT, null);
        for (SummonRecord r : List.of(mount, archer, guardian, cat, wolf, otherMount)) reg.register(r);

        assertEquals(List.of(mount), AdminPolicy.dismissTargets(reg.ofOwner(OWNER), false),
                "mount-only dismiss takes exactly the mount");
        assertEquals(List.of(mount, archer, guardian),
                AdminPolicy.dismissTargets(reg.ofOwner(OWNER), true),
                "dismiss all takes mount + every combat summon, in registry order");
        assertFalse(AdminPolicy.dismisses(SummonRecord.Branch.COMPANION, true),
                "companions survive even dismiss all");
        assertFalse(AdminPolicy.dismisses(SummonRecord.Branch.PACK, true),
                "pack summons survive even dismiss all");

        // act it the way the listener does: unregister each selected record
        for (SummonRecord r : AdminPolicy.dismissTargets(reg.ofOwner(OWNER), false)) {
            reg.unregister(r.entityId());
        }
        assertEquals(5, reg.size(), "only the mount left the registry");
        assertEquals(List.of(archer, guardian), AdminPolicy.dismissTargets(reg.ofOwner(OWNER), true),
                "a following dismiss all finds the combat pair and no mount");
        for (SummonRecord r : AdminPolicy.dismissTargets(reg.ofOwner(OWNER), true)) {
            reg.unregister(r.entityId());
        }
        assertEquals(3, reg.size(), "the cat, the wolf, and the other owner's mount remain");
        assertTrue(AdminPolicy.dismissTargets(reg.ofOwner(OWNER), true).isEmpty(),
                "a redundant dismiss is an empty selection — a clean zero-count no-op");
        assertTrue(AdminPolicy.dismissTargets(null, true).isEmpty(), "null-safe");
    }

    @Test
    void adminListLineFormatPins() {
        // The 0.1.0–0.1.2 self-list strings, byte-for-byte — the admin
        // list renders through the same formatter, so these pins are
        // also the self-list regression pins.
        assertEquals("- archer (combat, 90s left, HP 18/20, holds 10% mana)",
                Nameplate.listLine("archer", "combat", "90s left", "HP 18/20", 10));
        assertEquals("- guardian (combat, 170s left, HP 60/100, holds 25% mana)",
                Nameplate.listLine("guardian", "combat", "170s left", "HP 60/100", 25));
        assertEquals("- mount (mount, persistent, HP 100/100)",
                Nameplate.listLine("mount", "mount", "persistent", "HP 100/100", 0),
                "the mount holds no reserve: no mana clause");
        assertEquals("- wolf (pack, persistent, not loaded)",
                Nameplate.listLine("wolf", "pack", "persistent", "not loaded", 0),
                "an unloaded entity reads 'not loaded', as the self list has since 0.1.0");
        assertEquals("- bruiser (combat, 5s left, not loaded, holds 15% mana)",
                Nameplate.listLine("bruiser", "combat", "5s left", "not loaded", 15));
    }

    @Test
    void adminAuditLineNamesTheOperatorActor() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals("Summons admin dismiss by CONSOLE for Patient (" + id
                        + "): dismissed 2 summon(s) (mount + all combat).",
                AdminPolicy.auditLine("CONSOLE", "Patient", id, 2, true));
        assertEquals("Summons admin dismiss by OperatorName for Patient (" + id
                        + "): dismissed 1 summon(s) (mount only).",
                AdminPolicy.auditLine("OperatorName", "Patient", id, 1, false),
                "an in-game operator is named as themselves — the act is never "
                        + "recorded as the target player's own dismiss");
        assertTrue(AdminPolicy.auditLine(null, "Patient", id, 0, false)
                        .startsWith("Summons admin dismiss by CONSOLE"),
                "a blank actor falls back to CONSOLE, never to the target's name");
    }

    @Test
    void pluginYmlPins0110() throws Exception {
        // 0.1.10 re-pin of pluginYmlPins019: version + fingerprint
        // lineage only — every other assertion is carried verbatim.
        // local-build.sh runs the suite from the project root
        String yml = java.nio.file.Files.readString(java.nio.file.Path.of("plugin.yml"));
        assertTrue(yml.contains("version: 0.1.10"), "plugin.yml declares version 0.1.10");
        assertTrue(yml.contains("list|menu|dismiss"), "the usage line names the 0.1.7 menu form");
        assertTrue(yml.contains("excelsummons.admin:"), "the existing admin node stays registered");
        int at = yml.indexOf("excelsummons.admin:");
        String block = yml.substring(at, Math.min(yml.length(), at + 300));
        assertTrue(block.contains("default: op"), "excelsummons.admin still defaults to op");
        assertTrue(yml.contains("admin list <player>") && yml.contains("admin dismiss <player> [all|pets]"),
                "the usage line names both admin forms, pets scope included");
        assertTrue(yml.contains("dismiss [all|pets]"), "the usage line names the self dismiss pets form");
        assertEquals("0.1.10", BuildInfo.VERSION);
        assertTrue(BuildInfo.BUILD_ID.startsWith("EXC-SUMMONS-0.1.10-WRV-20261114-01-")
                        && BuildInfo.BUILD_ID.endsWith("-" + BuildInfo.TAG),
                "fingerprint lineage: EXC-SUMMONS semantic marker, seq 01 (BB-11 back-button build), tag fragment carried in the id");
    }

    // ----------------------------- 0.1.4 companion reserve (§2)

    @Test
    void reserveCountsCompanionsAndMount() {
        // Design §5.1 test 1 — REPLACES 0.1.3's
        // reserveScopeExcludesCompanionsAndTheMount (deleted: Mike's
        // Nov 10, 2026, 10:08 AM ruling — "companions hold mana
        // too" — reverses the 0.1.2 scope). Wolf 10 + familiar 5 +
        // cat 5 + mount 10 = 30; combat-only totals are unchanged.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        assertEquals(30, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        SummonRegistry combatOnly = new SummonRegistry();
        combatOnly.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        combatOnly.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(35, ReservePolicy.totalPercent(combatOnly, OWNER, PCT14),
                "combat totals read identically under the widened scope");
        // a family with NO configured percent still holds nothing
        assertEquals(ReservePolicy.Refusal.NONE,
                ReservePolicy.check(75, 0, 75, 10, 200, 20));
    }

    @Test
    void reserveTierKeysForCompanions() {
        // Design §5.1 test 2: tier keys for the new families, and
        // allKeys == the shipped template list exactly (the 0.1.2
        // seven + the 0.1.4 eleven of §2.5/§3.3).
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 3; i++) reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals(Map.of("wrv_summon_reserve_wolf3", 30),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT14));
        SummonRegistry cats = new SummonRegistry();
        cats.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        cats.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        assertEquals(Map.of("wrv_summon_reserve_cat2", 10),
                ReservePolicy.desiredModifiers(cats, OWNER, PCT14));

        Map<String, Integer> caps = Map.of(
                "archer", 2, "bruiser", 2, "guardian", 1, "revenant", 1, "ghast", 1,
                "wolf", 5, "familiar", 3, "cat", 2, "mount", 1);
        List<String> keys = ReservePolicy.allKeys(PCT14, caps);
        assertEquals(18, keys.size(), "7 combat templates + 11 new = 18 keys, no more");
        assertEquals(Set.of(
                        "wrv_summon_reserve_wolf", "wrv_summon_reserve_wolf2",
                        "wrv_summon_reserve_wolf3", "wrv_summon_reserve_wolf4",
                        "wrv_summon_reserve_wolf5",
                        "wrv_summon_reserve_familiar", "wrv_summon_reserve_familiar2",
                        "wrv_summon_reserve_familiar3",
                        "wrv_summon_reserve_cat", "wrv_summon_reserve_cat2",
                        "wrv_summon_reserve_mount"),
                keys.stream().filter(k -> !Set.of(
                                "wrv_summon_reserve_archer", "wrv_summon_reserve_archer2",
                                "wrv_summon_reserve_bruiser", "wrv_summon_reserve_bruiser2",
                                "wrv_summon_reserve_guardian", "wrv_summon_reserve_revenant",
                                "wrv_summon_reserve_ghast").contains(k))
                        .collect(java.util.stream.Collectors.toSet()),
                "the new-family keys are exactly the §2.5 + §3.3 template list");
        // a grandfathered 5-wolf set (§3.3) is held EXACTLY, by wolf5
        SummonRegistry five = new SummonRegistry();
        for (int i = 0; i < 5; i++) five.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals(Map.of("wrv_summon_reserve_wolf5", 50),
                ReservePolicy.desiredModifiers(five, OWNER, PCT14));
    }

    @Test
    void reserveCeilingCountsMountAndCompanions() {
        // Design §5.1 test 3: the mount counts toward the CEILING
        // total even though it never counts toward the count cap.
        // Guardian+Ghast+Revenant = 70; a mount (10) on top = 80.
        assertEquals(ReservePolicy.Refusal.RESERVE_CEILING,
                ReservePolicy.check(70, 10, 75, 200, 200, 20));
        // 3 wolves (30) + Guardian (25) + Archer (10) = 65: legal;
        // one more Bruiser (15) = 80: refused at the ceiling.
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 3; i++) reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        int held = ReservePolicy.totalPercent(reg, OWNER, PCT14);
        assertEquals(65, held);
        assertEquals(ReservePolicy.Refusal.NONE,
                ReservePolicy.check(held, 5, 75, 200, 200, 20), "a cat (5) still fits at 70");
        assertEquals(ReservePolicy.Refusal.RESERVE_CEILING,
                ReservePolicy.check(held, 15, 75, 200, 200, 20), "a bruiser (15) would make 80");
    }

    @Test
    void reserveReleasesOnUnregisterForEveryBranch() {
        // Design §5.1 test 4: the reserveReleasesOnEveryDespawnPath
        // pattern extended per branch — every despawn path ends in
        // unregister, and the re-derived total must fall to 0 for
        // companion, mount, and mixed sets alike.
        SummonRegistry reg = new SummonRegistry();
        SummonRecord wolf = recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord mount = recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null);
        SummonRecord guard = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1);
        reg.register(wolf);
        reg.register(mount);
        reg.register(guard);
        assertEquals(45, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        reg.unregister(wolf.entityId());
        assertEquals(35, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        reg.unregister(mount.entityId());
        assertEquals(25, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        reg.unregister(guard.entityId());
        assertEquals(0, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        assertTrue(ReservePolicy.desiredModifiers(reg, OWNER, PCT14).isEmpty());
    }

    @Test
    void masterFifthExpensiveSummonRefusedOnCeilingNotCount() {
        // Supervisor-directed pin (rank amendment): a master with
        // four summons out (count 4 of 5 — the count cap does NOT
        // bind) whose holds total 70% casts a ghast (25%): the
        // COUNT check passes and the RESERVE CEILING refuses, with
        // the ceiling message. The mana penalty applies at every
        // rank — five expensive summons price out.
        CapPolicy policy = policy();
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "revenant", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(4, reg.countCounted(OWNER));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "ghast", rule(SummonRecord.Branch.COMBAT, 1, true), "master"),
                "count: 4 of a master's 5 — no cap refusal");
        int held = ReservePolicy.totalPercent(reg, OWNER, PCT14);
        assertEquals(70, held, "15 + 25 + 20 + 10");
        ReservePolicy.Refusal r = ReservePolicy.check(held, 25, 75, 200, 200, 20);
        assertEquals(ReservePolicy.Refusal.RESERVE_CEILING, r);
        assertEquals("Ghast would hold 25% mana, but your summons already hold 70% "
                        + "of 75% allowed. Dismiss a summon first.",
                ReservePolicy.refusalMessage(r, "ghast", 25, held, 75, 20));
    }

    // ----------------------------- 0.1.4 refusal messaging (§4)

    @Test
    void refusalMessagesNameTheirReason() {
        // Design §5.1 test 13: one pin per bridge row of the §4.2
        // table, numbers interpolated.
        CapPolicy policy = policy();
        // SPELL_CAP
        assertEquals("Your Guardian limit is reached — 1 of 1 already out.",
                CapPolicy.spellCapMessage("Guardian", 1, 1));
        SummonRegistry reg = new SummonRegistry();
        reg.register(rec(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals("Your Guardian limit is reached — 1 of 1 already out.",
                policy.refusalMessage(CapPolicy.Refusal.SPELL_CAP, reg, OWNER, "guardian",
                        rule(SummonRecord.Branch.COMBAT, 1, true), "master"));
        // SERVER_CEILING
        assertEquals("The valley is holding as many combat summons as it can right now "
                + "(12). Try again shortly.", CapPolicy.serverCeilingMessage(12));
        // RESERVE_CEILING / RESERVE_FLOOR
        assertEquals("Happy Ghast would hold 10% mana, but your summons already hold 70% "
                        + "of 75% allowed. Dismiss a summon first.",
                ReservePolicy.refusalMessage(ReservePolicy.Refusal.RESERVE_CEILING,
                        "mount", 10, 70, 75, 20),
                "<Spell> is Nameplate.spellDisplayName output (§4.2) — the mount's is Happy Ghast");
        assertEquals("Too much of your mana is already committed (toggles and summons) "
                        + "— a Familiar would leave you under 20% usable. "
                        + "Free some mana first.",
                ReservePolicy.refusalMessage(ReservePolicy.Refusal.RESERVE_FLOOR,
                        "familiar", 5, 70, 75, 20));
        // TOTAL_CAP message under the Nov 14, 2026 cast-only law
        // (P-SUMMONS-CAP-1): adopted pets are NOT counted, so an
        // adopted-only set produces NO total-cap refusal at all —
        // the pre-fix "pets that can only leave by death" variant
        // is unreachable through check() (pinned in
        // adoptedPetsOverCapDoNotBlockCasts below). The legacy
        // text shape itself is still pinned directly on
        // totalCapMessage so the retained branch cannot drift.
        assertEquals("Summon limit reached — your rank (Student) allows 2 summons, "
                        + "and you have 2 out: Wolf, Wolf. Your summons are pets "
                        + "that can only leave by death.",
                CapPolicy.totalCapMessage("student", 2, 2,
                        List.of("Wolf", "Wolf"), false));
        SummonRegistry adopted = new SummonRegistry();
        adopted.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        adopted.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(adopted, OWNER, "cat",
                        rule(SummonRecord.Branch.COMPANION, 2, true), "student"),
                "adopted pets alone never refuse a cast (Nov 14 law)");
        // ...and a CAST pet in a mixed set is counted (the adopted
        // one is not), and the dismiss advice is true for it
        SummonRegistry mixed = new SummonRegistry();
        mixed.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        mixed.register(new SummonRecord(UUID.randomUUID(), OWNER, "cat",
                SummonRecord.Branch.COMPANION, null, "world", NOW, SummonRecord.Origin.CAST));
        assertEquals(1, mixed.countCounted(OWNER),
                "only the CAST cat occupies a slot; the adopted wolf does not");
        assertTrue(policy.refusalMessage(CapPolicy.Refusal.TOTAL_CAP, mixed, OWNER, "wolf",
                        rule(SummonRecord.Branch.PACK, 5, false), "student")
                .contains("Dismiss one (or let a combat summon expire)"));
    }

    // ----------------------------- 0.1.4 dismiss pets + origin (§2.4)

    private static SummonRecord recOrigin(UUID owner, String spell,
                                          SummonRecord.Branch branch,
                                          SummonRecord.Origin origin) {
        return new SummonRecord(UUID.randomUUID(), owner, spell, branch, null,
                "world", NOW, origin);
    }

    @Test
    void dismissPetsScope() {
        // Design §5.1 test 11: the pets scope selects exactly
        // COMPANION+PACK records with origin == CAST; adopted
        // records, mount, and combat survive; all / mount-only
        // selections stay byte-identical to the 0.1.3 pins.
        SummonRecord castWolf = recOrigin(OWNER, "wolf", SummonRecord.Branch.PACK,
                SummonRecord.Origin.CAST);
        SummonRecord adoptedWolf = recOrigin(OWNER, "wolf", SummonRecord.Branch.PACK,
                SummonRecord.Origin.ADOPTED);
        SummonRecord castCat = recOrigin(OWNER, "cat", SummonRecord.Branch.COMPANION,
                SummonRecord.Origin.CAST);
        SummonRecord adoptedCat = recOrigin(OWNER, "cat", SummonRecord.Branch.COMPANION,
                SummonRecord.Origin.ADOPTED);
        SummonRecord castFamiliar = recOrigin(OWNER, "familiar", SummonRecord.Branch.COMPANION,
                SummonRecord.Origin.CAST);
        SummonRecord mount = rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null);
        SummonRecord archer = rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1);
        List<SummonRecord> all = List.of(castWolf, adoptedWolf, castCat, adoptedCat,
                castFamiliar, mount, archer);
        assertEquals(List.of(castWolf, castCat, castFamiliar),
                AdminPolicy.dismissTargets(all, AdminPolicy.DismissScope.PETS),
                "pets takes cast pets only — adopted pets, mount, combat survive");
        assertEquals(List.of(mount), AdminPolicy.dismissTargets(all,
                AdminPolicy.DismissScope.MOUNT_ONLY));
        assertEquals(List.of(mount, archer), AdminPolicy.dismissTargets(all,
                AdminPolicy.DismissScope.ALL), "all is unchanged: mount + combat, never pets");
        assertEquals(List.of(mount), AdminPolicy.dismissTargets(all, false),
                "the boolean form still delegates: bare = mount only");
        assertEquals(List.of(mount, archer), AdminPolicy.dismissTargets(all, true),
                "the boolean form still delegates: all = mount + combat");
        // routing: the pets token routes for admin AND is a usage
        // error nowhere else; the audit line names the scope
        var pets = AdminPolicy.route(new String[]{"admin", "dismiss", "Patient", "pets"},
                true, false);
        assertEquals(AdminPolicy.Route.ADMIN_DISMISS, pets.route());
        assertEquals(AdminPolicy.DismissScope.PETS, pets.scope());
        assertFalse(pets.all(), "pets is not the all scope");
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals("Summons admin dismiss by CONSOLE for Patient (" + id
                        + "): dismissed 2 summon(s) (cast pets only).",
                AdminPolicy.auditLine("CONSOLE", "Patient", id, 2,
                        AdminPolicy.DismissScope.PETS));
    }

    @Test
    void recordCodecOriginRoundTrips() {
        // Design §5.1 test 12: origin survives the codec; a legacy
        // row with no origin key restores as ADOPTED (the safe
        // direction — never dismiss-removable until proven cast).
        SummonRecord cast = recOrigin(OWNER, "wolf", SummonRecord.Branch.PACK,
                SummonRecord.Origin.CAST);
        Map<String, Object> map = cast.toMap();
        assertEquals("cast", map.get("origin"));
        assertEquals(SummonRecord.Origin.CAST, SummonRecord.fromMap(map).origin());
        SummonRecord adopted = recOrigin(OWNER, "cat", SummonRecord.Branch.COMPANION,
                SummonRecord.Origin.ADOPTED);
        assertEquals(SummonRecord.Origin.ADOPTED,
                SummonRecord.fromMap(adopted.toMap()).origin());
        Map<String, Object> legacy = new java.util.LinkedHashMap<>(adopted.toMap());
        legacy.remove("origin");
        assertEquals(SummonRecord.Origin.ADOPTED, SummonRecord.fromMap(legacy).origin(),
                "a pre-0.1.4 row restores as ADOPTED");
        // the 7-arg constructor (every pre-0.1.4 call site) also defaults ADOPTED
        assertEquals(SummonRecord.Origin.ADOPTED,
                rec(OWNER, "wolf", SummonRecord.Branch.PACK, null).origin());
    }

    // ----------------------------- 0.1.4 sustainability (§5.1 tests 14–15)

    @Test
    void sustainabilityAcrossPoolTiers() {
        // Design §5.1 test 14 (logic harness, pure form): at every
        // pool tier (100 / 150 / 200), every family's single cast
        // from an empty registry passes BOTH reserve checks — no
        // legal first summon is ever refused. Reserve is a hold,
        // not a drain: nothing here changes any cost or cooldown
        // (test 15 pins the config side).
        for (int pool : new int[]{100, 150, 200}) {
            for (Map.Entry<String, Integer> e : PCT14.entrySet()) {
                assertEquals(ReservePolicy.Refusal.NONE,
                        ReservePolicy.check(0, e.getValue(), 75, pool, pool, 20),
                        e.getKey() + " at pool " + pool);
            }
        }
        // Companion-heavy play stays cheap at the smallest pool:
        // 3 wolves (30) + cat (5) + familiar (5) = 40 held, 60
        // usable of 100 — every step legal under ceiling + floor.
        int held = 0;
        for (int pct : new int[]{10, 10, 10, 5, 5}) {
            assertEquals(ReservePolicy.Refusal.NONE,
                    ReservePolicy.check(held, pct, 75, 100 - held, 100, 20));
            held += pct;
        }
        assertEquals(40, held);
        // The explicitly intended LOW (named, not a failure): the
        // full combat set (Guardian 25 + Ghast 25 + Revenant 20 =
        // 70 held) is legal stepwise and leaves 30% usable at any
        // pool — summons are cooldown-paced, not mana-paced, so a
        // 100-cost wolf is NOT payable from that state until a
        // summon leaves. The harness reports this as the packet-#1
        // precedent LOW; the ceiling/floor refusal texts are what
        // the player sees if they try.
        held = 0;
        for (int pct : new int[]{25, 25, 20}) {
            assertEquals(ReservePolicy.Refusal.NONE,
                    ReservePolicy.check(held, pct, 75, 200 - 2 * held, 200, 20));
            held += pct;
        }
        assertEquals(70, held);
    }

    @Test
    void configPins018() throws Exception {
        // 0.1.8 rename of configPins017: config.yml is byte-identical
        // to 0.1.7 (both 0.1.8 changes are code, not config), so
        // every assertion below is carried verbatim.
        // Design §5.1 test 15 + the packet's config law, read from
        // the shipped config.yml (local-build.sh runs the suite
        // from the project root, the pluginYmlPins precedent).
        String cfg = java.nio.file.Files.readString(java.nio.file.Path.of("config.yml"));
        // rank ladder, LOCKED values (Mike, Nov 10, 2026, 10:37 AM)
        assertTrue(cfg.contains("summon-cap-by-rank:"), "the cap is a per-rank map");
        assertTrue(cfg.contains("beginner: 1") && cfg.contains("student: 2")
                        && cfg.contains("apprentice: 3") && cfg.contains("master: 5"),
                "Beginner 1 · Student 2 · Apprentice 3 · Master 5");
        assertFalse(cfg.contains("\nper-caster-combat-cap:"),
                "the retired key is gone as a SETTING (the header comment may name it)");
        assertFalse(cfg.contains("\nper-caster-summon-cap:"),
                "no single-value cap setting remains");
        assertTrue(cfg.contains("server-combat-ceiling: 12"), "the ceiling is unchanged");
        // reserve rows: every family present, every percent > 0
        // (no zero-cost enabled keys in this packet's config)
        for (String row : new String[]{"archer: 10", "bruiser: 15", "guardian: 25",
                "revenant: 20", "ghast: 25", "wolf: 10", "familiar: 5", "cat: 5",
                "mount: 10"}) {
            assertTrue(cfg.contains(row), "reserve row " + row);
        }
        assertTrue(cfg.contains("reserve-ceiling-percent: 75")
                && cfg.contains("reserve-floor-percent: 20"), "ceiling/floor unchanged");
        // spell rows unchanged from 0.1.3 (caps + durations are
        // spells-packet numbers; this packet moves none of them)
        assertTrue(cfg.contains("duration-ms: 90000") && cfg.contains("duration-ms: 180000"),
                "combat durations unchanged");
        assertTrue(cfg.contains("enforce-cap: false"), "wolf's enforce-cap stays false (§3.2)");
    }

    // ----------------- 0.1.5 archer equipment guarantee (diagnosis layer 2)

    @Test
    void archerEquipmentCoversEveryTier() {
        // The spell definitions of record (summons-archer delta):
        // a bow in hand at EVERY tier; iron helmet at the base
        // tier, chainmail at tiers 2 and 3.
        for (String key : new String[]{"archer", "archer|2", "archer|3"}) {
            assertTrue(ArcherEquipment.guarantees(key), key + " is covered");
            assertEquals("BOW", ArcherEquipment.handMaterialName(key),
                    key + " holds a bow");
        }
        assertEquals("IRON_HELMET", ArcherEquipment.helmetMaterialName("archer"));
        assertEquals("CHAINMAIL_HELMET", ArcherEquipment.helmetMaterialName("archer|2"));
        assertEquals("CHAINMAIL_HELMET", ArcherEquipment.helmetMaterialName("archer|3"));
        assertEquals(1, ArcherEquipment.tierOf("archer"));
        assertEquals(2, ArcherEquipment.tierOf("archer|2"));
        assertEquals(3, ArcherEquipment.tierOf("archer|3"));
    }

    @Test
    void archerEquipmentIgnoresOtherFamilies() {
        // The guarantee is archer-only: every other family — and
        // null / near-miss keys — gets no spec and no application.
        for (String key : new String[]{"bruiser", "bruiser|2", "guardian", "revenant",
                "ghast", "mount", "wolf", "familiar", "cat", "archerx", "", null}) {
            assertFalse(ArcherEquipment.guarantees(key), String.valueOf(key));
            assertNull(ArcherEquipment.handMaterialName(key));
            assertNull(ArcherEquipment.helmetMaterialName(key));
        }
    }

    @Test
    void archerEquipmentTierParsingFailsSafeToBase() {
        // An unknown tier suffix reads as the BASE tier (iron
        // helmet), never as a guessed upgrade; a future tier above
        // the book continues the tiered (chainmail) spec.
        assertEquals(1, ArcherEquipment.tierOf("archer|x"));
        assertEquals(1, ArcherEquipment.tierOf("archer|"));
        assertEquals(1, ArcherEquipment.tierOf("archer|0"));
        assertEquals(1, ArcherEquipment.tierOf(null));
        assertEquals("IRON_HELMET", ArcherEquipment.helmetMaterialName("archer|x"));
        assertEquals("CHAINMAIL_HELMET", ArcherEquipment.helmetMaterialName("archer|4"));
        // the family fold is SummonRecord.baseKey's own, tiers included
        assertTrue(ArcherEquipment.guarantees("archer|99"));
    }

    @Test
    void archerEquipmentVerifyMatchesExactSpec() {
        // Read-back verification: both slots must equal the spec.
        assertTrue(ArcherEquipment.matches("BOW", "IRON_HELMET", "archer"));
        assertTrue(ArcherEquipment.matches("BOW", "CHAINMAIL_HELMET", "archer|2"));
        assertTrue(ArcherEquipment.matches("BOW", "CHAINMAIL_HELMET", "archer|3"));
    }

    @Test
    void archerEquipmentVerifyRejectsMissingOrWrongGear() {
        // The reported defect itself: bowless (empty hand) fails.
        assertFalse(ArcherEquipment.matches(null, "IRON_HELMET", "archer"));
        assertFalse(ArcherEquipment.matches("AIR", "IRON_HELMET", "archer"));
        // helmetless fails (the helmet is the sun-proofing)
        assertFalse(ArcherEquipment.matches("BOW", null, "archer"));
        assertFalse(ArcherEquipment.matches("BOW", "AIR", "archer|2"));
        // the wrong tier's helmet fails: base gear on a tier-2 cast
        // (and vice versa) is not the spec the spell defines
        assertFalse(ArcherEquipment.matches("BOW", "IRON_HELMET", "archer|2"));
        assertFalse(ArcherEquipment.matches("BOW", "CHAINMAIL_HELMET", "archer"));
        // a non-archer family never verifies, whatever it holds
        assertFalse(ArcherEquipment.matches("BOW", "IRON_HELMET", "bruiser"));
        assertFalse(ArcherEquipment.matches("BOW", "IRON_HELMET", null));
    }

    // ----------------- 0.1.6 adopted exemption + sync clamp (Mike's ruling, Nov 12, 2026)

    @Test
    void adoptedMenagerieHoldsNoMana() {
        // Regression fixture (a) — Jester's exact Nov 12, 2026 set:
        // 5 wolves + 2 cats + 3 familiars + 1 mount, ALL
        // origin=adopted (his hand-tamed/bred menagerie, live
        // registry of record). Under 0.1.5 this applied 85% of his
        // 200 pool (wolf5 50 + cat2 10 + familiar3 15 + mount 10).
        // Mike's ruling: adopted pets hold NO mana — the applied
        // total is 0% and the full pool stays usable.
        SummonRegistry reg = new SummonRegistry();
        SummonRecord aWolf = null;
        for (int i = 0; i < 5; i++) {
            SummonRecord w = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
            if (aWolf == null) aWolf = w;
            reg.register(w);
        }
        for (int i = 0; i < 2; i++) reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        for (int i = 0; i < 3; i++) reg.register(recAdopted(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        reg.register(recAdopted(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        assertEquals(11, reg.ofOwner(OWNER).size(), "the whole menagerie stays registered — nothing is trimmed");
        assertEquals(0, ReservePolicy.totalPercent(reg, OWNER, PCT14),
                "adopted records contribute 0% to the raw total");
        assertTrue(ReservePolicy.reserveCounts(reg, OWNER, PCT14).isEmpty(),
                "adopted records never enter the reserve counts");
        assertTrue(ReservePolicy.desiredModifiers(reg, OWNER, PCT14).isEmpty(),
                "the sync desired state is empty: every stale adopted modifier clears");
        assertEquals(0, ReservePolicy.appliedPercent(reg, OWNER, PCT14),
                "applied total 0% — the full 200 pool is usable again");
        assertFalse(ReservePolicy.recordHolds(reg, OWNER, aWolf.entityId(), PCT14, 75, 20),
                "no adopted record claims a hold in the list lines");
        // a CAST summon alongside the menagerie still holds its own rate, alone
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000));
        assertEquals(10, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        assertEquals(Map.of("wrv_summon_reserve_archer", 10),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT14));
        assertEquals(10, ReservePolicy.appliedPercent(reg, OWNER, PCT14));
    }

    @Test
    void adoptedAndCastOfSameFamilyCountSeparately() {
        // Mixed origins, same families: only the CAST records count.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        assertEquals(Map.of("wolf", 1, "cat", 1), ReservePolicy.reserveCounts(reg, OWNER, PCT14));
        assertEquals(15, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        assertEquals(Map.of("wrv_summon_reserve_wolf", 10, "wrv_summon_reserve_cat", 5),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT14),
                "bare keys for the single cast wolf/cat — the adopted pair adds no tier");
    }

    @Test
    void castSetClampedAtSyncToCeilingAndFloor() {
        // Regression fixture (b) — an over-ceiling CAST set:
        // guardian 25 + ghast 25 + revenant 20 + bruiser 15 + cat 5
        // = 90 raw. The sync re-derivation must clamp to <= 75%
        // with usable >= 20%: holds apply in registry order, the
        // bruiser (which would pass the cap at 85) holds nothing,
        // and the smaller cat behind it still fits at exactly 75.
        SummonRegistry reg = new SummonRegistry();
        SummonRecord guardian = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord ghast = recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord revenant = recCast(OWNER, "revenant", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord bruiser = recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord cat = recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        for (SummonRecord r : List.of(guardian, ghast, revenant, bruiser, cat)) reg.register(r);
        assertEquals(90, ReservePolicy.totalPercent(reg, OWNER, PCT14),
                "the RAW cast total is the unclamped registry truth");
        Map<String, Integer> desired = ReservePolicy.desiredModifiers(reg, OWNER, PCT14);
        assertEquals(Map.of("wrv_summon_reserve_guardian", 25,
                        "wrv_summon_reserve_ghast", 25,
                        "wrv_summon_reserve_revenant", 20,
                        "wrv_summon_reserve_cat", 5),
                desired, "bruiser's key is absent — it holds nothing beyond the cap");
        int applied = ReservePolicy.appliedPercent(reg, OWNER, PCT14);
        assertEquals(75, applied, "clamped exactly at the 75% ceiling");
        assertTrue(100 - applied >= 20, "usable stays at or above the 20% floor");
        assertEquals(desired, ReservePolicy.desiredModifiers(reg, OWNER, PCT14, 75, 20),
                "the explicit live-config form agrees with the default form");
        assertEquals(5, reg.ofOwner(OWNER).size(), "the excess summon STAYS — nothing is trimmed");
        assertTrue(ReservePolicy.recordHolds(reg, OWNER, guardian.entityId(), PCT14, 75, 20));
        assertFalse(ReservePolicy.recordHolds(reg, OWNER, bruiser.entityId(), PCT14, 75, 20),
                "the clamped-out bruiser claims no hold in its list line");
        assertTrue(ReservePolicy.recordHolds(reg, OWNER, cat.entityId(), PCT14, 75, 20),
                "the cat behind the bruiser still holds — registry order, skip-and-continue");
    }

    @Test
    void syncClampFloorBindsBelowTheCeiling() {
        // With a hypothetical ceiling of 90, the FLOOR-derived cap
        // (100 - 20 = 80) is the binding limit: guardian 25 +
        // ghast 25 + revenant 20 = 70 applied; a bruiser (15)
        // would reach 85 > 80 and holds nothing.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "revenant", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(85, ReservePolicy.totalPercent(reg, OWNER, PCT14));
        assertEquals(70, ReservePolicy.appliedPercent(reg, OWNER, PCT14, 90, 20),
                "the floor cap binds at 80 even when the ceiling would allow 90");
        assertEquals(Map.of("wrv_summon_reserve_guardian", 25,
                        "wrv_summon_reserve_ghast", 25,
                        "wrv_summon_reserve_revenant", 20),
                ReservePolicy.desiredModifiers(reg, OWNER, PCT14, 90, 20));
    }

    // ----------------- 0.1.7 menu model (compass Summons page, Mike Nov 12, 2026)

    private static MenuModel.Page menuOf(SummonRegistry reg, Map<UUID, String> hp) {
        return MenuModel.build(reg, OWNER, PCT14, 75, 20, NOW, hp, Map.of());
    }

    /** 0.1.9: the names-aware twin — baseNameByEntity is the map
     *  menuPageFor computes from the live entities. */
    private static MenuModel.Page menuOfNames(SummonRegistry reg, Map<UUID, String> hp,
                                              Map<UUID, String> baseNames) {
        return MenuModel.build(reg, OWNER, PCT14, 75, 20, NOW, hp, baseNames);
    }

    @Test
    void menuMainPageShowsCastOnlyWithListFacts() {
        // Mixed fixture: 2 cast combat + 2 adopted. The main page
        // carries the CAST records only, each rendered with the
        // exact /summons list line (Nameplate.listLine over the
        // same inputs listLineFor computes: time left from the
        // expiry, the pre-rendered HP text or "not loaded", the
        // APPLIED hold).
        SummonRegistry reg = new SummonRegistry();
        SummonRecord guardian = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 180_000);
        SummonRecord archer = recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000);
        reg.register(guardian);
        reg.register(archer);
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        MenuModel.Page page = menuOf(reg, Map.of(guardian.entityId(), "HP 40/40"));
        assertEquals(4, page.totalRegistered());
        assertEquals(2, page.castEntries().size());
        assertEquals(2, page.adoptedEntries().size());
        MenuModel.Entry g = page.castEntries().get(0);
        assertEquals(Nameplate.listLine("guardian", "combat", "180s left", "HP 40/40", 25),
                g.listLine(), "the menu line IS the list line, byte-for-byte");
        assertEquals(25, g.holdPercent());
        assertFalse(g.adopted());
        MenuModel.Entry a = page.castEntries().get(1);
        assertEquals(Nameplate.listLine("archer", "combat", "90s left", "not loaded", 10),
                a.listLine(), "an entity with no HP text renders 'not loaded', as the list does");
        assertEquals(35, page.heldPercent(), "guardian 25 + archer 10 applied; adopted hold nothing");
        String body = page.mainBody();
        assertTrue(body.contains(g.listLine()) && body.contains(a.listLine()));
        assertTrue(body.contains("Mana held by your summons: 35% of your pool."));
        for (MenuModel.Entry e : page.adoptedEntries()) {
            assertFalse(body.contains(e.listLine()),
                    "no adopted line ever renders on the main page");
        }
    }

    @Test
    void menuAdoptedSubPageCarriesTheIndicators() {
        // Mike's refinement: adopted pets render ONLY on the
        // sub-page, each row carrying ADOPTED + holds no mana +
        // protected — can't be dismissed.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals("Adopted pets (2)", page.adoptedButtonLabel(),
                "the main page shows the adopted count button");
        String body = page.adoptedBody();
        for (MenuModel.Entry e : page.adoptedEntries()) {
            assertTrue(e.adopted());
            assertEquals(0, e.holdPercent(), "an adopted record's row claims no hold");
            assertFalse(e.listLine().contains("holds"),
                    "the adopted list line carries no mana clause");
            assertTrue(body.contains(e.bedrockLine()), "every adopted line is in the sub-page body");
            assertTrue(e.bedrockLine().contains(MenuModel.ADOPTED_BADGE)
                            && e.bedrockLine().contains(MenuModel.ADOPTED_NO_MANA),
                    "ADOPTED badge + holds no mana indicators on every adopted row");
        }
        assertFalse(page.mainBody().contains(MenuModel.ADOPTED_BADGE),
                "the main page body never renders an adopted row");
    }

    @Test
    void menuAppliedHoldRenderingSkipsClampedCast() {
        // The 0.1.6 clamp at the menu layer: guardian 25 + ghast
        // 25 + revenant 20 = 70 applied; bruiser 15 would pass
        // the 75 ceiling and is SKIPPED (holds nothing, claims
        // nothing); the cat behind it still fits at 5 -> 75.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 1));
        reg.register(recCast(OWNER, "revenant", SummonRecord.Branch.COMBAT, NOW + 1));
        SummonRecord bruiser = recCast(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1);
        reg.register(bruiser);
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals(75, page.heldPercent());
        MenuModel.Entry b = page.castEntries().stream()
                .filter(e -> e.record() == bruiser).findFirst().orElseThrow();
        assertEquals(0, b.holdPercent(), "a clamp-skipped cast summon shows no hold");
        assertFalse(b.listLine().contains("holds"), "and its line claims none");
        assertEquals(5, page.castEntries().get(4).holdPercent(),
                "the smaller cast cat behind the skipped bruiser still holds");
    }

    @Test
    void menuHeldTotalIgnoresAdoptedMenagerie() {
        // Jester's fixture at the model layer: 11 adopted animals
        // hold 0%; the main page is empty of rows but counts them.
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 5; i++) reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        for (int i = 0; i < 2; i++) reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        for (int i = 0; i < 3; i++) reg.register(recAdopted(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        reg.register(recAdopted(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals(0, page.heldPercent());
        assertEquals(0, page.castEntries().size());
        assertEquals(11, page.adoptedEntries().size());
        assertEquals("Adopted pets (11)", page.adoptedButtonLabel());
        assertTrue(page.mainBody().contains("You have 11 summon(s) registered."));
        assertFalse(page.mainBody().contains("Mana held"),
                "no held line when nothing is held, exactly as /summons list omits it");
    }

    @Test
    void menuPaginationSplitsBothPages() {
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 50; i++) reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        for (int i = 0; i < 47; i++) reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals(45, MenuModel.PAGE_SIZE);
        assertEquals(2, page.castPageCount());
        assertEquals(45, page.castPage(0).size());
        assertEquals(5, page.castPage(1).size());
        assertEquals(2, page.adoptedPageCount());
        assertEquals(45, page.adoptedPage(0).size());
        assertEquals(2, page.adoptedPage(1).size());
        assertEquals(5, page.castPage(99).size(), "an over-large page index clamps to the last page");
        assertEquals(45, page.castPage(-1).size(), "a negative page index clamps to the first page");
        assertEquals(page.castEntries(), java.util.stream.Stream
                        .concat(page.castPage(0).stream(), page.castPage(1).stream()).toList(),
                "the pages tile the full cast list with nothing dropped or repeated");
    }

    @Test
    void menuBedrockBodiesAreComplete() {
        // No pagination in Bedrock text: the main body carries
        // EVERY cast line, the adopted body EVERY adopted line.
        SummonRegistry reg = new SummonRegistry();
        for (int i = 0; i < 60; i++) reg.register(recCast(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        for (int i = 0; i < 12; i++) reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        String main = page.mainBody();
        for (MenuModel.Entry e : page.castEntries()) {
            assertTrue(main.contains(e.listLine()), "main body carries every cast line");
        }
        String adopted = page.adoptedBody();
        for (MenuModel.Entry e : page.adoptedEntries()) {
            assertTrue(adopted.contains(e.bedrockLine()), "adopted body carries every adopted line");
            assertFalse(main.contains(e.listLine()), "no adopted line leaks into the main body");
        }
    }

    @Test
    void menuDismissScopeRoutingMatchesTheCommands() {
        // Exactly the three existing scopes, in command order:
        // bare dismiss (mount only), pets, all. The adopted
        // button sits after them and routes to NO scope.
        assertEquals(3, MenuModel.DISMISS_BUTTONS.size());
        assertEquals(AdminPolicy.DismissScope.MOUNT_ONLY, MenuModel.scopeForButton(0));
        assertEquals(AdminPolicy.DismissScope.PETS, MenuModel.scopeForButton(1));
        assertEquals(AdminPolicy.DismissScope.ALL, MenuModel.scopeForButton(2));
        assertNull(MenuModel.scopeForButton(3), "index 3 is the adopted sub-page door, not a dismiss");
        assertNull(MenuModel.scopeForButton(-1));
        assertEquals(3, MenuModel.ADOPTED_BUTTON_INDEX);
        assertEquals("Dismiss mount", MenuModel.DISMISS_BUTTONS.get(0).label());
        assertEquals("Dismiss cast pets", MenuModel.DISMISS_BUTTONS.get(1).label());
        assertEquals("Dismiss all (mount + combat)", MenuModel.DISMISS_BUTTONS.get(2).label());
        // The result text is the command's own (ExcelSummons
        // dismiss branch): menu and command report identically.
        assertEquals("Nothing to dismiss.", MenuModel.resultMessage(0));
        assertEquals("Dismissed 1 summon(s).", MenuModel.resultMessage(1));
        assertEquals("Dismissed 7 summon(s).", MenuModel.resultMessage(7));
    }

    // ----------------- 0.1.10 main-page Back (BB-11 FIX-2, 2 rows)

    @Test
    void mainFormButtonsEndWithBackToServerMenu() {
        // BB-11 Bedrock row: the main form's buttons are the
        // three dismiss labels, the adopted door, then Back to
        // Server Menu LAST — every pre-0.1.10 index unchanged.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        java.util.List<String> buttons = MenuModel.mainFormButtons(page);
        assertEquals(5, buttons.size());
        assertEquals("Dismiss mount", buttons.get(0));
        assertEquals("Dismiss cast pets", buttons.get(1));
        assertEquals("Dismiss all (mount + combat)", buttons.get(2));
        assertEquals(page.adoptedButtonLabel(), buttons.get(3));
        assertEquals("Back to Server Menu", buttons.get(4));
        assertEquals(MenuModel.MAIN_BACK_LABEL, buttons.get(MenuModel.MAIN_BACK_INDEX));
        assertEquals(4, MenuModel.MAIN_BACK_INDEX);
        assertEquals(MenuModel.ADOPTED_BUTTON_INDEX + 1, MenuModel.MAIN_BACK_INDEX);
        // Back is not a dismiss: it routes to NO scope.
        assertNull(MenuModel.scopeForButton(MenuModel.MAIN_BACK_INDEX),
                "the Back index routes to the Server Menu, never to a dismiss scope");
    }

    @Test
    void mainBackSourceAnchorsPinBothPlatforms() throws Exception {
        // BB-11 Java + Bedrock rows, pinned at the Bukkit layer
        // (the pluginYmlPins source-reading precedent: the suite
        // runs from the project root). Java: the Back item sits
        // at SLOT_BACK_TO_MENU in fillMain and its click closes
        // the view and opens the Server Menu. Bedrock: the main
        // form is built from mainFormButtons and MAIN_BACK_INDEX
        // routes to the same opener. The opener is the fleet
        // performCommand("menu") pattern.
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/com/winkreyes/excelsummons/SummonsMenus.java"));
        assertTrue(src.contains("SLOT_BACK_TO_MENU = 52"),
                "Java main Back slot is 52 (49 is the adopted Back / dismiss-pets slot)");
        assertTrue(src.contains("inv.setItem(SLOT_BACK_TO_MENU, item(Material.ARROW, MenuModel.MAIN_BACK_LABEL"),
                "fillMain places the ARROW Back item labelled from MenuModel");
        assertTrue(src.contains("if (slot == SLOT_BACK_TO_MENU)"),
                "the MainMenu click branch handles the Back slot");
        assertTrue(src.contains("MenuModel.mainFormButtons(model)"),
                "the Bedrock main form is built from MenuModel.mainFormButtons");
        assertTrue(src.contains("if (idx == MenuModel.MAIN_BACK_INDEX)"),
                "the Bedrock main-form handler routes the Back index");
        assertTrue(src.contains("player.performCommand(\"menu\")"),
                "the Server Menu opens via the fleet performCommand menu opener");
        assertTrue(src.contains("The Server Menu could not open."),
                "a failed opener says so instead of stranding the player");
        // Out-of-scope proof: the adopted page's own Back and
        // the confirm's keep control are still present verbatim.
        assertTrue(src.contains("inv.setItem(SLOT_BACK, item(Material.ARROW, \"Back to your summons\""),
                "the adopted sub-page Back is untouched");
        assertTrue(src.contains("MenuModel.KEEP_BUTTON_LABEL"),
                "the dismiss confirm keep control is untouched");
    }

    @Test
    void menuAdoptedDismissConfirmNamesThePetAndWarns() {
        // The confirm step (Mike's ~6:05 PM ruling): per-pet
        // button, a confirm body that NAMES the specific pet and
        // warns the removal is permanent + not re-summonable, and
        // a keep label that is the no-op way out.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        MenuModel.Entry wolf = menuOf(reg, Map.of()).adoptedEntries().get(0);
        assertEquals("Dismiss Wolf", MenuModel.dismissButtonLabel(wolf),
                "the sub-page button is per-pet and names the pet type");
        String body = MenuModel.confirmBody(wolf);
        assertTrue(body.contains("Dismiss your Wolf?"), "the confirm names the specific pet");
        assertTrue(body.contains("real tamed animal"), "it says what the pet IS");
        assertTrue(body.contains("permanently") && body.contains("cannot be re-summoned"),
                "it warns the removal is permanent and not re-summonable");
        assertTrue(MenuModel.confirmButtonLabel(wolf).contains("Wolf"),
                "even the confirm action names the pet");
        assertEquals("No — keep my pet", MenuModel.KEEP_BUTTON_LABEL);
    }

    @Test
    void adoptedDismissGateIsNarrowAndAudited() {
        // AdoptedDismissPolicy: ONLY an adopted record owned by
        // the actor passes. A cast record is refused (cast has
        // its scopes), another owner's pet is refused, unknown
        // records are refused.
        SummonRecord adopted = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord cast = recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        assertTrue(AdoptedDismissPolicy.eligible(adopted, OWNER));
        assertFalse(AdoptedDismissPolicy.eligible(cast, OWNER),
                "the adopted path refuses CAST records — they dismiss through their scopes");
        assertFalse(AdoptedDismissPolicy.eligible(adopted, OTHER),
                "another player's pet can never be dismissed through your menu");
        assertFalse(AdoptedDismissPolicy.eligible(null, OWNER));
        assertFalse(AdoptedDismissPolicy.eligible(adopted, null));
        String line = AdoptedDismissPolicy.auditLine("Jester_Turtle", OWNER, adopted);
        assertTrue(line.contains("Jester_Turtle") && line.contains("wolf"),
                "the audit line names the player and the pet's spell/type key");
        assertTrue(line.startsWith("Summons adopted dismiss by "));
    }

    @Test
    void commandScopesStillNeverSelectAdopted() {
        // The menu flow is the ONLY adopted-dismiss door. Adopted
        // records exist only on the wolf/cat families (the sweep
        // registers tamed wolves and cats; "horses and parrots are
        // not tree summons"), so the invariant is: no command
        // scope's selection (AdminPolicy.dismissTargets — the
        // selection SummonsListener.dismiss runs, byte-identical
        // to 0.1.6) ever contains an adopted pet. PETS is
        // origin-checked explicitly; MOUNT_ONLY / ALL select by
        // branch (mount / combat), where adopted pets never sit.
        SummonRecord aWolf = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord aCat = recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        SummonRecord cWolf = recCast(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord cGuard = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 1);
        SummonRecord cMount = recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null);
        List<SummonRecord> all = List.of(aWolf, aCat, cWolf, cGuard, cMount);
        for (AdminPolicy.DismissScope scope : AdminPolicy.DismissScope.values()) {
            List<SummonRecord> targets = AdminPolicy.dismissTargets(all, scope);
            assertTrue(targets.stream().noneMatch(r -> r == aWolf || r == aCat),
                    "scope " + scope + " never selects an adopted pet");
        }
        assertEquals(List.of(cWolf), AdminPolicy.dismissTargets(all, AdminPolicy.DismissScope.PETS),
                "PETS takes the cast wolf only — the adopted wolf and cat survive");
        assertEquals(List.of(cGuard, cMount),
                AdminPolicy.dismissTargets(all, AdminPolicy.DismissScope.ALL),
                "ALL is unchanged from 0.1.6: mount + combat (registry order), never pets");
        assertEquals(List.of(cMount), AdminPolicy.dismissTargets(all,
                AdminPolicy.DismissScope.MOUNT_ONLY), "bare dismiss is unchanged: mount only");
        // And no bulk adopted control exists in the menu model:
        // the only scope buttons are the three command scopes.
        assertEquals(3, MenuModel.DISMISS_BUTTONS.size());
        assertTrue(MenuModel.DISMISS_BUTTONS.stream()
                        .allMatch(b -> b.label().startsWith("Dismiss")),
                "the main page's buttons are the three scopes; adopted dismissal is per-pet only");
    }

    @Test
    void menuEmptyRegistryRendersClean() {
        MenuModel.Page page = menuOf(new SummonRegistry(), Map.of());
        assertEquals(0, page.totalRegistered());
        assertEquals(0, page.heldPercent());
        assertEquals(1, page.castPageCount());
        assertTrue(page.castPage(0).isEmpty());
        assertEquals("Adopted pets (0)", page.adoptedButtonLabel());
        assertTrue(page.mainBody().contains("No cast summons out right now."));
        assertTrue(page.adoptedBody().contains("No adopted pets registered."));
    }

    // ----------------- 0.1.8 out-now counts (Mike, Nov 12, 2026, evening)

    @Test
    void menuOutCountsDeriveFromCastEntries() {
        // The counts group the page's CAST entries by base key:
        // mount = mounts; wolf / cat / familiar = cast pets;
        // every other cast key = combat. Tiered keys fold into
        // their base (wolf|2 is a wolf). Adopted records — even
        // an adopted mount — are NEVER counted as out.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, NOW + 60_000));
        reg.register(recCast(OWNER, "wolf|2", SummonRecord.Branch.PACK, NOW + 60_000));
        reg.register(recCast(OWNER, "cat", SummonRecord.Branch.COMPANION, NOW + 60_000));
        reg.register(recCast(OWNER, "familiar", SummonRecord.Branch.COMPANION, NOW + 60_000));
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 60_000));
        reg.register(recCast(OWNER, "archer|3", SummonRecord.Branch.COMBAT, NOW + 60_000));
        reg.register(recCast(OWNER, "ghast", SummonRecord.Branch.COMBAT, NOW + 60_000));
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        MenuModel.OutCounts c = page.outCounts();
        assertEquals(1, c.mountsOut());
        assertEquals(4, c.castPetsOut(), "wolf + wolf|2 + cat + familiar; the adopted wolf is excluded");
        assertEquals(3, c.combatOut(), "guardian + archer|3 + ghast");
        assertEquals(8, c.totalCastOut());
        assertEquals(page.castEntries().size(), c.totalCastOut(),
                "the total IS the cast list the page renders — the numbers can never disagree with the rows");
    }

    @Test
    void menuOutCountsZeroCases() {
        // Empty registry: every count is zero and the line says
        // nothing is out (the 0.1.7 "No cast summons out right
        // now." closer still renders after it).
        MenuModel.Page empty = menuOf(new SummonRegistry(), Map.of());
        assertEquals(new MenuModel.OutCounts(0, 0, 0, 0), empty.outCounts());
        assertEquals("Nothing out right now.", empty.outNowLine());
        assertTrue(empty.mainBody().contains("Nothing out right now."));
        assertTrue(empty.mainBody().contains("No cast summons out right now."));
        // Adopted-only registry (Mike's own shape after 0.1.6:
        // a menagerie, nothing cast): the counts stay zero —
        // adopted animals are not "out" casts.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals(new MenuModel.OutCounts(0, 0, 0, 0), page.outCounts());
        assertEquals("Nothing out right now.", page.outNowLine());
        assertTrue(page.mainBody().contains("Nothing out right now."));
    }

    @Test
    void menuOutNowLineFormatsAndPlacement() {
        // Singulars at 1, plurals elsewhere, and ALL THREE groups
        // always printed — a zero group shows its zero.
        SummonRegistry one = new SummonRegistry();
        one.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        assertEquals("Out now: 1 mount \u00B7 0 cast pets \u00B7 0 combat summons (1 total)",
                menuOf(one, Map.of()).outNowLine());
        SummonRegistry two = new SummonRegistry();
        two.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        two.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        assertEquals("Out now: 2 mounts \u00B7 0 cast pets \u00B7 0 combat summons (2 total)",
                menuOf(two, Map.of()).outNowLine());
        // The mixed fixture (the rig's shape): the line sits in
        // the Bedrock body between the registered total and the
        // held line — and never on the adopted body.
        SummonRegistry reg = new SummonRegistry();
        reg.register(recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 180_000));
        reg.register(recCast(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 90_000));
        reg.register(recCast(OWNER, "wolf", SummonRecord.Branch.PACK, NOW + 90_000));
        reg.register(recCast(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
        MenuModel.Page page = menuOf(reg, Map.of());
        assertEquals("Out now: 1 mount \u00B7 1 cast pet \u00B7 2 combat summons (4 total)",
                page.outNowLine());
        String body = page.mainBody();
        int registered = body.indexOf("summon(s) registered.");
        int outLine = body.indexOf(page.outNowLine());
        int heldLine = body.indexOf("Mana held by your summons:");
        assertTrue(registered >= 0 && outLine > registered, "the counts line follows the registered total");
        assertTrue(heldLine > outLine, "the counts line precedes the held line");
        assertFalse(page.adoptedBody().contains("Out now"),
                "the counts line belongs to the main page only");
    }

    // ----------------- 0.1.8 Bedrock form fix (Geyser-only, new Cumulus)

    @Test
    void bedrockFormsHandlerShape018() throws Exception {
        // Structural pin on the compiled BedrockForms: the NEW
        // Cumulus + Geyser-only path is what ships, and the dead
        // 0.1.7 shape (old Cumulus, Floodgate, validResultHandler)
        // is gone. Javadoc does not reach class bytes, so these
        // needles see only real constant-pool references.
        byte[] bytes;
        try (var in = SummonsTest.class.getResourceAsStream(
                "/com/winkreyes/excelsummons/BedrockForms.class")) {
            assertNotNull(in, "BedrockForms.class is on the test classpath");
            bytes = in.readAllBytes();
        }
        String pool = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pool.contains("org.geysermc.cumulus.form.SimpleForm"), "the NEW Cumulus SimpleForm");
        assertTrue(pool.contains("org.geysermc.cumulus.form.util.FormBuilder"), "the new FormBuilder");
        assertTrue(pool.contains("resultHandler"), "the handler attaches via FormBuilder.resultHandler");
        assertTrue(pool.contains("clickedButtonId"), "the index unwraps via SimpleFormResponse.clickedButtonId");
        assertTrue(pool.contains("org.geysermc.geyser.api.GeyserApi"), "connections come from GeyserApi");
        assertTrue(pool.contains("connectionByUuid"), "the connection is looked up by player uuid");
        assertFalse(pool.contains("org.geysermc.cumulus.SimpleForm"),
                "the OLD Cumulus SimpleForm is gone (its response API is what swallowed 0.1.7's taps)");
        assertFalse(pool.contains("validResultHandler"),
                "the 0.1.7 handler hunt is gone");
        assertFalse(pool.contains("floodgate"),
                "the Floodgate attempt is gone — Geyser class loader only");
    }

    @Test
    void bedrockFormsHandlerDeliversEveryIndex018() throws Exception {
        // Behavioral proof against the REAL Cumulus classes in
        // the staged Geyser jar (local-build.sh passes its path;
        // the jar is loaded in its own URLClassLoader, exactly
        // how the rig loads it too). For each of the three form
        // shapes the menu sends, a synthetic response is driven
        // through the handler BedrockForms.buildForm attached —
        // via FormImpl.callResultHandler (the same call Geyser's
        // FormCache makes) AND via the raw-payload entry
        // (SimpleFormDefinition.handleFormResponse with the
        // client's raw "<index>" / "null" strings). A closed
        // form must report -1. This is the proof 0.1.7 lacked:
        // it proved forms SEND, never that taps ARRIVE.
        String geyserJar = System.getProperty("summons.test.geyserJar");
        String guavaJar = System.getProperty("summons.test.guavaJar");
        String gsonJar = System.getProperty("summons.test.gsonJar");
        assertNotNull(geyserJar, "local-build.sh passes -Dsummons.test.geyserJar");
        java.net.URLClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{
                java.nio.file.Path.of(geyserJar).toUri().toURL(),
                java.nio.file.Path.of(guavaJar).toUri().toURL(),
                java.nio.file.Path.of(gsonJar).toUri().toURL()},
                SummonsTest.class.getClassLoader());
        Class<?> formImpl = Class.forName("org.geysermc.cumulus.form.impl.FormImpl", true, l);
        Class<?> resultT = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, l);
        Class<?> validT = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, l);
        Class<?> respImpl = Class.forName("org.geysermc.cumulus.response.impl.SimpleFormResponseImpl", true, l);
        Class<?> buttonT = Class.forName("org.geysermc.cumulus.component.ButtonComponent", true, l);
        Class<?> responseT = Class.forName("org.geysermc.cumulus.response.FormResponse", true, l);
        java.lang.reflect.Method respOf = respImpl.getMethod("of", int.class, buttonT);
        java.lang.reflect.Method validOf = validT.getMethod("of", responseT);
        java.lang.reflect.Method callHandler = formImpl.getMethod("callResultHandler", resultT);
        java.lang.reflect.Method closedOf = resultT.getMethod("closed");
        Object definition = Class.forName("org.geysermc.cumulus.form.impl.simple.SimpleFormDefinition", true, l)
                .getMethod("instance").invoke(null);
        java.lang.reflect.Method handleRaw = definition.getClass().getMethod("handleFormResponse",
                Class.forName("org.geysermc.cumulus.form.Form", true, l), String.class);
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("summons-test");
        java.util.Map<String, List<String>> shapes = new java.util.LinkedHashMap<>();
        shapes.put("main", List.of("Dismiss mount", "Dismiss cast pets",
                "Dismiss all (mount + combat)", "Adopted pets (2)"));
        shapes.put("adopted", List.of("Dismiss Wolf", "Dismiss Cat", "Back to your summons"));
        shapes.put("confirm", List.of(MenuModel.confirmButtonLabel(
                new MenuModel.Entry(null, "", "Wolf", "", "", "", "", 0, true)),
                MenuModel.KEEP_BUTTON_LABEL));
        for (var shape : shapes.entrySet()) {
            java.util.concurrent.atomic.AtomicInteger got =
                    new java.util.concurrent.atomic.AtomicInteger(Integer.MIN_VALUE);
            Object form = BedrockForms.buildForm(l, "Your Summons", "body",
                    shape.getValue(), got::set, log);
            List<?> buttons = (List<?>) form.getClass().getMethod("buttons").invoke(form);
            assertEquals(shape.getValue().size(), buttons.size(),
                    shape.getKey() + " form carries its buttons");
            for (int i = 0; i < buttons.size(); i++) {
                got.set(Integer.MIN_VALUE);
                callHandler.invoke(form, validOf.invoke(null, respOf.invoke(null, i, buttons.get(i))));
                assertEquals(i, got.get(),
                        shape.getKey() + " button " + i + " reaches the callback via callResultHandler");
                got.set(Integer.MIN_VALUE);
                handleRaw.invoke(definition, form, String.valueOf(i));
                assertEquals(i, got.get(),
                        shape.getKey() + " button " + i + " reaches the callback via the raw payload path");
            }
            got.set(Integer.MIN_VALUE);
            callHandler.invoke(form, closedOf.invoke(null));
            assertEquals(-1, got.get(), shape.getKey() + ": a closed form reports -1");
            got.set(Integer.MIN_VALUE);
            handleRaw.invoke(definition, form, "null");
            assertEquals(-1, got.get(), shape.getKey() + ": the client's close payload reports -1");
        }
    }

    // ----------------- 0.1.9 pet names + rename (Mike, Nov 13, 2026)

    @Test
    void menuDisplayNamesComeFromThePetsOwnNames() {
        // Mike's pack: two adopted wolves and a cat. The base-name
        // map — what menuPageFor computes from the live entities —
        // names the wolves; the cat carries no name and falls back
        // to its type. Every surface that reads displayName must
        // tell the animals apart now (through 0.1.8 all three were
        // their type, so 8 wolves read as 8 identical rows).
        SummonRegistry reg = new SummonRegistry();
        SummonRecord wolfA = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord wolfB = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord cat = recAdopted(OWNER, "cat", SummonRecord.Branch.COMPANION, null);
        reg.register(wolfA);
        reg.register(wolfB);
        reg.register(cat);
        MenuModel.Page page = menuOfNames(reg, Map.of(),
                Map.of(wolfA.entityId(), "Buddy", wolfB.entityId(), "Rex"));
        List<MenuModel.Entry> pets = page.adoptedEntries();
        assertEquals("Buddy", pets.get(0).displayName());
        assertEquals("Rex", pets.get(1).displayName());
        assertEquals("Cat", pets.get(2).displayName(),
                "an unnamed pet falls back to its type");
        // The per-pet buttons and the confirm step name the animal.
        assertEquals("Dismiss Buddy", MenuModel.dismissButtonLabel(pets.get(0)));
        assertEquals("Dismiss Rex", MenuModel.dismissButtonLabel(pets.get(1)));
        assertTrue(MenuModel.confirmBody(pets.get(0)).contains("Dismiss your Buddy?"),
                "the confirm step names the specific pet it would remove");
        assertTrue(MenuModel.confirmButtonLabel(pets.get(1)).contains("Rex"));
        assertEquals("Rename Buddy", MenuModel.renameButtonLabel(pets.get(0)));
        // The /summons list line itself is UNTOUCHED by the name:
        // it stays byte-for-byte the command's line (spell key).
        assertTrue(pets.get(0).listLine().startsWith("- wolf ("),
                "the list line keeps the spell key; the name lives in displayName");
    }

    @Test
    void menuDisplayNameFallsBackOnBlankOrMissingNames() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord wolf = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        reg.register(wolf);
        assertEquals("Wolf", menuOfNames(reg, Map.of(), Map.of(wolf.entityId(), "  "))
                .adoptedEntries().get(0).displayName(),
                "a blank base name is no name");
        assertEquals("Wolf", MenuModel.build(reg, OWNER, PCT14, 75, 20, NOW, Map.of(), null)
                .adoptedEntries().get(0).displayName(),
                "a null name map renders every entry under its type");
    }

    @Test
    void menuCastEntriesCarryNamesTheSameWay() {
        // The commission's scope check: the display-name fix is
        // not adopted-only — a cast summon that carries a name
        // shows it too (the map covers every record).
        SummonRegistry reg = new SummonRegistry();
        SummonRecord guardian = recCast(OWNER, "guardian", SummonRecord.Branch.COMBAT, NOW + 60_000);
        reg.register(guardian);
        MenuModel.Entry e = menuOfNames(reg, Map.of(),
                Map.of(guardian.entityId(), "Sir Bounces")).castEntries().get(0);
        assertEquals("Sir Bounces", e.displayName());
        assertEquals(e.listLine(), e.bedrockLine(),
                "a cast summon's Bedrock line stays its list line, name or not");
    }

    @Test
    void adoptedBedrockLineLeadsWithTheName() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord wolf = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        reg.register(wolf);
        MenuModel.Entry named = menuOfNames(reg, Map.of(wolf.entityId(), "HP 20/20"),
                Map.of(wolf.entityId(), "Buddy")).adoptedEntries().get(0);
        assertEquals("- Buddy (pack, persistent, HP 20/20) — ADOPTED, holds no mana",
                named.bedrockLine());
        MenuModel.Entry unnamed = menuOf(reg, Map.of(wolf.entityId(), "HP 20/20"))
                .adoptedEntries().get(0);
        assertEquals("- Wolf (pack, persistent, HP 20/20) — ADOPTED, holds no mana",
                unnamed.bedrockLine(),
                "an unnamed pet's line leads with its type instead");
    }

    @Test
    void adoptedAndPetFormButtonLayouts019() {
        SummonRegistry reg = new SummonRegistry();
        SummonRecord a = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord b = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        reg.register(a);
        reg.register(b);
        MenuModel.Page page = menuOfNames(reg, Map.of(),
                Map.of(a.entityId(), "Buddy", b.entityId(), "Rex"));
        assertEquals(List.of("Buddy", "Rex", "Back to your summons"),
                MenuModel.adoptedFormButtons(page),
                "one button per pet under its own name, then Back");
        assertEquals(List.of("Rename Buddy", "Dismiss Buddy", "Back to adopted pets"),
                MenuModel.petFormButtons(page.adoptedEntries().get(0)),
                "the pet form is rename, dismiss, back — in that order");
        assertEquals(0, MenuModel.PET_RENAME_INDEX);
        assertEquals(1, MenuModel.PET_DISMISS_INDEX);
        assertEquals(2, MenuModel.PET_BACK_INDEX);
    }

    @Test
    void renamePolicySanitizesNames() {
        assertEquals("", RenamePolicy.sanitize(null));
        assertEquals("Buddy", RenamePolicy.sanitize("  Buddy  "));
        assertEquals("Buddy", RenamePolicy.sanitize("§aBuddy"),
                "section-sign colour codes are stripped");
        assertEquals("Rex", RenamePolicy.sanitize("&cRex"),
                "ampersand colour codes are stripped");
        assertEquals("A B", RenamePolicy.sanitize("A \n  B"),
                "whitespace runs (newlines included) collapse to one space");
        assertEquals("Bo", RenamePolicy.sanitize("B\u0007o"),
                "control characters are removed");
        assertEquals("Sir Bounces", RenamePolicy.sanitize("Sir  Bounces"));
    }

    @Test
    void renamePolicyAcceptance() {
        assertTrue(RenamePolicy.acceptable("Buddy"));
        assertFalse(RenamePolicy.acceptable(""));
        assertFalse(RenamePolicy.acceptable(null));
        assertTrue(RenamePolicy.acceptable("x".repeat(RenamePolicy.MAX_NAME_LENGTH)),
                "a name at the cap is allowed");
        assertFalse(RenamePolicy.acceptable("x".repeat(RenamePolicy.MAX_NAME_LENGTH + 1)),
                "one character over the cap is refused");
        assertFalse(RenamePolicy.acceptable("Buddy — 12/20"),
                "a name ending in the plate's own health suffix would be eaten "
                        + "by the composition on the next refresh");
        assertFalse(RenamePolicy.acceptable("Buddy — 12.5/20"));
        assertTrue(RenamePolicy.acceptable("Buddy — 12"),
                "a dash number that is not the suffix shape is a name, not a suffix");
        assertTrue(RenamePolicy.acceptableRaw("  §aBuddy  "),
                "raw input is cleaned before it is judged");
        assertFalse(RenamePolicy.acceptableRaw("§a"),
                "a name that cleans to nothing is refused");
    }

    @Test
    void renamePolicyGateIsAdoptedOnlyAndOwned() {
        SummonRecord adopted = recAdopted(OWNER, "wolf", SummonRecord.Branch.PACK, null);
        SummonRecord cast = recCast(OWNER, "wolf", SummonRecord.Branch.PACK, NOW + 60_000);
        assertTrue(RenamePolicy.eligible(adopted, OWNER));
        assertFalse(RenamePolicy.eligible(cast, OWNER),
                "cast summons are refused — their names are not the menu's to set");
        assertFalse(RenamePolicy.eligible(adopted, OTHER),
                "another player's pet is not yours to rename");
        assertFalse(RenamePolicy.eligible(null, OWNER));
        assertFalse(RenamePolicy.eligible(adopted, null));
        String line = RenamePolicy.auditLine("MikeExcel", OWNER, adopted, "Buddy");
        assertTrue(line.contains("MikeExcel") && line.contains("wolf")
                && line.contains("'Buddy'")
                && line.contains(adopted.entityId().toString()),
                "the audit line names the actor, the pet, and the new name");
    }

    @Test
    void bedrockFormsCarriesTheRenameCustomForm019() throws Exception {
        // Structural pin, the 0.1.8 needle discipline applied to
        // the rename form: the NEW Cumulus CustomForm + asInput
        // are what ship, and the dead shapes stay gone.
        byte[] bytes;
        try (var in = SummonsTest.class.getResourceAsStream(
                "/com/winkreyes/excelsummons/BedrockForms.class")) {
            assertNotNull(in, "BedrockForms.class is on the test classpath");
            bytes = in.readAllBytes();
        }
        String pool = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pool.contains("org.geysermc.cumulus.form.CustomForm"),
                "the rename form is the NEW Cumulus CustomForm");
        assertTrue(pool.contains("asInput"), "the submitted text unwraps via asInput");
        assertFalse(pool.contains("org.geysermc.cumulus.CustomForm"),
                "the OLD Cumulus CustomForm is not used");
        assertFalse(pool.contains("floodgate"), "Geyser loader only, as 0.1.8 set");
        assertFalse(pool.contains("validResultHandler"), "the 0.1.7 handler hunt stays gone");
    }

    @Test
    void bedrockRenameFormHandlerDeliversText019() throws Exception {
        // Behavioral proof for the rename input form against the
        // REAL Cumulus classes in the staged Geyser jar — the
        // 0.1.8 discipline: a synthetic submitted response is
        // driven through the handler buildCustomForm attached,
        // via FormImpl.callResultHandler AND via the definition's
        // raw-payload entry (the client's JSON array string). A
        // closed form must report null (rename nothing).
        String geyserJar = System.getProperty("summons.test.geyserJar");
        String guavaJar = System.getProperty("summons.test.guavaJar");
        String gsonJar = System.getProperty("summons.test.gsonJar");
        assertNotNull(geyserJar, "local-build.sh passes -Dsummons.test.geyserJar");
        java.net.URLClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{
                java.nio.file.Path.of(geyserJar).toUri().toURL(),
                java.nio.file.Path.of(guavaJar).toUri().toURL(),
                java.nio.file.Path.of(gsonJar).toUri().toURL()},
                SummonsTest.class.getClassLoader());
        Class<?> formImpl = Class.forName("org.geysermc.cumulus.form.impl.FormImpl", true, l);
        Class<?> resultT = Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, l);
        Class<?> validT = Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, l);
        Class<?> respImpl = Class.forName("org.geysermc.cumulus.response.impl.CustomFormResponseImpl", true, l);
        Class<?> responseT = Class.forName("org.geysermc.cumulus.response.FormResponse", true, l);
        Class<?> componentType = Class.forName("org.geysermc.cumulus.component.util.ComponentType", true, l);
        Class<?> jsonArrayT = Class.forName("com.google.gson.JsonArray", true, l);
        Class<?> jsonPrimitiveT = Class.forName("com.google.gson.JsonPrimitive", true, l);
        java.lang.reflect.Method respOf = null;
        for (java.lang.reflect.Method m : respImpl.getMethods()) {
            if (m.getName().equals("of") && m.getParameterCount() == 3) respOf = m;
        }
        assertNotNull(respOf, "CustomFormResponseImpl.of(List, JsonArray, List) exists");
        java.lang.reflect.Method validOf = validT.getMethod("of", responseT);
        java.lang.reflect.Method callHandler = formImpl.getMethod("callResultHandler", resultT);
        java.lang.reflect.Method closedOf = resultT.getMethod("closed");
        Object definition = Class.forName("org.geysermc.cumulus.form.impl.custom.CustomFormDefinition", true, l)
                .getMethod("instance").invoke(null);
        java.lang.reflect.Method handleRaw = definition.getClass().getMethod("handleFormResponse",
                Class.forName("org.geysermc.cumulus.form.Form", true, l), String.class);
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("summons-test");
        Object inputType = componentType.getMethod("valueOf", String.class).invoke(null, "INPUT");

        java.util.concurrent.atomic.AtomicReference<String> got =
                new java.util.concurrent.atomic.AtomicReference<>("UNSET");
        Object form = BedrockForms.buildCustomForm(l, "Rename Wolf", "New name",
                "Type the new name", "Wolf", got::set, log);

        // The handler path Geyser's FormCache runs: a submitted
        // input value reaches the callback verbatim.
        Object json = jsonArrayT.getConstructor().newInstance();
        Object prim = jsonPrimitiveT.getConstructor(String.class).newInstance("Buddy");
        jsonArrayT.getMethod("add", Class.forName("com.google.gson.JsonElement", true, l))
                .invoke(json, prim);
        Object response = respOf.invoke(null, List.of("Buddy"), json, List.of(inputType));
        callHandler.invoke(form, validOf.invoke(null, response));
        assertEquals("Buddy", got.get(),
                "a submitted name reaches the callback via callResultHandler");

        // The raw-payload path: the client's JSON array string.
        got.set("UNSET");
        handleRaw.invoke(definition, form, "[\"Rex\"]");
        assertEquals("Rex", got.get(),
                "a submitted name reaches the callback via the raw payload path");

        // A closed form reports null — rename nothing.
        got.set("UNSET");
        callHandler.invoke(form, closedOf.invoke(null));
        assertNull(got.get(), "a closed rename form reports null");
        got.set("UNSET");
        handleRaw.invoke(definition, form, "null");
        assertNull(got.get(), "the client's close payload reports null");
    }
}
