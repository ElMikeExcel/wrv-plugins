# TEST RESULTS — ExcelSummons 0.1.4 (Nov 10, 2026)

## Unit / logic harness (SummonsTest, via local-build.sh)

**69/69 tests successful, 0 failed** — reproduced identically
in three independent builds (in-tree double-build, clean-room
rebuild, rebuild from the shipped source zip; all three jars
byte-identical at SHA-256 a34d7bdf…cc25a).

Composition: the 53 carried 0.1.3 tests, of which 5 were
re-pinned or replaced under the design/amendments (each named
in BUILD-RECORD.md), plus 16 new 0.1.4 tests:

- §2 reserve: reserveCountsCompanionsAndMount,
  reserveTierKeysForCompanions (allKeys == the 18 shipped
  templates exactly; grandfathered 5-wolf set rides wolf5),
  reserveCeilingCountsMountAndCompanions,
  reserveReleasesOnUnregisterForEveryBranch.
- §3 cap (rank-scaled): totalCapCountsAcrossBranches,
  totalCapBindsWolfDespiteEnforceCapFalse, spellCapStillBindsLower,
  serverCeilingUnchanged, wolfExemptionPinReplaced (both halves),
  rankLadderIsLockedAndFailsLowest, earnedRankThresholdsMirrorMagicGate,
  studentIsRefusedAtTheirRankCap, masterFiveCheapSummonsLegalSixthRefused,
  masterFifthExpensiveSummonRefusedOnCeilingNotCount,
  overCapAtLoginIsGrandfatheredNeverTrimmed.
- §2.4: dismissPetsScope (selection + routing + audit line),
  recordCodecOriginRoundTrips (legacy row → ADOPTED).
- §4: refusalMessagesNameTheirReason (one pin per bridge row of
  the §4.2 table + the adopted-pets TOTAL_CAP variant).
- §5.1 tests 14–15: sustainabilityAcrossPoolTiers (every family
  payable from an empty registry at pools 100/150/200; the full
  combat set's 70% hold reported as the named LOW, not a
  failure), configPins014 (ladder values, retired keys absent,
  every reserve row > 0 — no zero-cost enabled keys; spell caps
  and durations unchanged).

## Desk rig (Paper 26.3 + Magic 11.2.5)

**14/14 behavior checks PASS, SEVERE 0, reserve-template
warnings 0.** Full results: `rig-0.1.4/RIG-RESULTS.md`; log:
`rig-0.1.4/boot.log`.

## Not in this run (by design)

§5.3's in-game controlled-cast matrix (the §1 suspect
convictions) requires the live-shaped spells packet / a live
maintenance window; it is an install-window proof, named in
INSTALL-PACKET.md. This desk run makes no claim about
WolfSpell's internal behavior.
