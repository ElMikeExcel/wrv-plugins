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
        reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
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
        reg.register(rec(OWNER, "mount", SummonRecord.Branch.MOUNT, null));
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
        reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
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
            reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
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
        reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
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
        reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        reg.register(rec(OWNER, "familiar", SummonRecord.Branch.COMPANION, null));
        reg.register(rec(OWNER, "archer", SummonRecord.Branch.COMBAT, NOW + 1));
        assertEquals(CapPolicy.Refusal.NONE,
                policy.check(reg, OWNER, "bruiser", rule(SummonRecord.Branch.COMBAT, 2, true), "master"),
                "the 5th counted summon is legal for a master");
        reg.register(rec(OWNER, "bruiser", SummonRecord.Branch.COMBAT, NOW + 1));
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
        for (int i = 0; i < 5; i++) reg.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        reg.register(rec(OWNER, "cat", SummonRecord.Branch.COMPANION, null));
        assertEquals(6, reg.countCounted(OWNER), "6 counted: over even a master's 5");
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "master"));
        assertEquals(CapPolicy.Refusal.TOTAL_CAP,
                policy.check(reg, OWNER, "familiar", rule(SummonRecord.Branch.COMPANION, 3, true), "student"),
                "the same set is over a student's 2 as well — refused, not trimmed");
        assertEquals(6, reg.size(), "the check itself removes nothing, ever");
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
    void pluginYmlPins016() throws Exception {
        // 0.1.6 re-pin of pluginYmlPins015: version + fingerprint
        // lineage only — every other assertion is carried verbatim.
        // local-build.sh runs the suite from the project root
        String yml = java.nio.file.Files.readString(java.nio.file.Path.of("plugin.yml"));
        assertTrue(yml.contains("version: 0.1.6"), "plugin.yml declares version 0.1.6");
        assertTrue(yml.contains("excelsummons.admin:"), "the existing admin node stays registered");
        int at = yml.indexOf("excelsummons.admin:");
        String block = yml.substring(at, Math.min(yml.length(), at + 300));
        assertTrue(block.contains("default: op"), "excelsummons.admin still defaults to op");
        assertTrue(yml.contains("admin list <player>") && yml.contains("admin dismiss <player> [all|pets]"),
                "the usage line names both admin forms, pets scope included");
        assertTrue(yml.contains("dismiss [all|pets]"), "the usage line names the self dismiss pets form");
        assertEquals("0.1.6", BuildInfo.VERSION);
        assertTrue(BuildInfo.BUILD_ID.startsWith("EXC-SUMMONS-0.1.6-WRV-20261112-07-")
                        && BuildInfo.BUILD_ID.endsWith("-" + BuildInfo.TAG),
                "fingerprint lineage: EXC-SUMMONS semantic marker, seq 07, tag fragment carried in the id");
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
        // TOTAL_CAP adopted-pets variant: no counted summon can be
        // dismissed (all adopted pets), so the message must NOT
        // say "dismiss one"
        SummonRegistry adopted = new SummonRegistry();
        adopted.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        adopted.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        assertEquals("Summon limit reached — your rank (Student) allows 2 summons, "
                        + "and you have 2 out: Wolf, Wolf. Your summons are pets "
                        + "that can only leave by death.",
                policy.refusalMessage(CapPolicy.Refusal.TOTAL_CAP, adopted, OWNER, "cat",
                        rule(SummonRecord.Branch.COMPANION, 2, true), "student"));
        // ...while one CAST pet in the set makes the dismiss advice true
        SummonRegistry mixed = new SummonRegistry();
        mixed.register(rec(OWNER, "wolf", SummonRecord.Branch.PACK, null));
        mixed.register(new SummonRecord(UUID.randomUUID(), OWNER, "cat",
                SummonRecord.Branch.COMPANION, null, "world", NOW, SummonRecord.Origin.CAST));
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
    void configPins016() throws Exception {
        // 0.1.6 rename of configPins015: config.yml is byte-identical
        // to 0.1.5 (the adopted exemption + sync clamp are code, not
        // config), so every assertion below is carried verbatim.
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
}
