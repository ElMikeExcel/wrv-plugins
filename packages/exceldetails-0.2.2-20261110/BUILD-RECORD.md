# ExcelDetails 0.2.2 — BUILD RECORD (Nov 10, 2026)

Design: `~/workspace/details-meter-20261109/build-0.2.2/DESIGN.md`.
Commission: Mike, Nov 9, 2026 — (1) boss segments scale by
BossTrial's display-scale (x1000) so meter numbers match the
boss pop-ups; (2) the meter is prepared for the EliteMobs
world: EM simulated-damage numbers must not render as
meaningless millions.

**Status: BUILT + UNIT-PROVEN at desk. NOT installed. The
supervisor files the MANIFEST entry after verifying this
return. Records package staged (below); GitHub/Drive are
supervisor lanes.**

## Base proof

The build tree is `build-0.2.1/` copied whole into
`build-0.2.2/`. BEFORE any edit, the unmodified copy was
rebuilt with its own local-build.sh: 35/35 tests green and the
jar came out **byte-identical to the shipped live 0.2.1** —
41,116 B, sha256
`7c5d56f3a7e70f147b14101b15d224144ae732735e7b5159efb5aba9d074251c`
(build EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a). The 0.2.2
deltas below are therefore the whole difference from live.

## Candidate

- `ExcelDetails/ExcelDetails-0.2.2.jar` — **50,024 B**,
  sha256 `7d02bcf9c5a2de501e7ac0d1d86cb44a01f9b96e598f5a1fd17f5f8cf5c34b4b`
- Deterministic double-build byte-identical inside the build
  run, AND a second full build run reproduced the same sha256.
  Fingerprint gate PASS (build
  EXC-DETAILS-0.2.2-WRV-20261110-01-b22e10, Layer B in-jar:
  excel-build.txt + BuildInfo fragments; semantic marker
  EXC-DETAILS persists per the standard).
- Unit tests: **46/46** — the 35 from 0.2.1, unmodified +
  passing, plus 11 new tests (below).

## What 0.2.2 changes

1. **BossScale (new, pure).** `isSealBossType` (the exact type
   set of BossTrial's Attribution.kindOf), `sanitizedScale`
   (a value-for-value mirror of BossTrial's
   DisplayScale.sanitized), `intakeFactor` (mirror of
   IntakeFactor.factor), and `forDisplay(finalDamage, scale,
   f) = finalDamage / f × scale` with a junk factor reading as
   identity (never amplifies). Evidence for the formula —
   governor HIGH scaling, GovernorStash ledger space, pop-up
   DisplayScale.apply sites — is laid out in DESIGN.md §1,
   including the trap avoided: DisplayScale.poolHit /
   displayedHit are called by NO production surface in 0.5.3;
   the shipped pop-ups scale the pre-intake ledger value.
2. **BossScaleLookup (new, Bukkit seam).** Display scale read
   LIVE from BossTrial's own plugin config
   (`feedback.display-scale`, public getConfig() API, 5 s
   cache) — the exact scale source, not a hardcoded copy;
   1000 appears only as BossTrial's own settled fail-soft
   default. Intake factor by PartyLookup-pattern reflection
   into BossTrial's live state (fields `cfg` + `encounters`,
   public `BossTrialConfig.intakeEntityCap`,
   `EncounterLedger.maxHealth()`), field handles cached and
   values re-read per call so a BossTrial reload cannot go
   stale; BOUND/UNBOUND logged once; every failure fails soft
   to f = 1. Scaling is gated on BossTrial being present.
3. **DetailsListener routing.** Damage done: EM route first,
   then seal-boss scaling, else as-is. Damage taken: as-is
   except the simulated-amount floor. Healing untouched.
   Only damage DONE to a seal boss ever scales — taken and
   healing stay true HP by design (DESIGN §1a).
4. **EmCombat (new, pure) + EliteLookup (new seam).**
   Routing: damage done to an EM-tracked elite
   (EntityTracker.isEliteMob, javap-verified on the shipped
   10.9.12 jar, reflection seam, fail-soft) OR any event over
   SIMULATED_FLOOR = 100,000 goes to the EM bucket; damage
   taken buckets only over the floor.
5. **SegmentTracker EM bucket.** Separate per-segment
   CombatStats map + event count: touches the segment clock,
   archives with the segment, clears/resets with it, is never
   ranked and never absorbed into overall at close.
   `/details status` gains one additive line naming the
   bucket when non-empty (DetailsCommands).
6. **Config:** bundled config.yml gains `boss-display-scale:
   true` and `elite-bucket: true` (code defaults identical, so
   an unmerged live config behaves the same) + the build
   stamp comment. plugin.yml 0.2.2, BuildInfo fragments,
   DetailsPlaceholders.getVersion() "0.2.2", build knobs.
   data.yml format untouched.

## New unit tests (11)

BossScale: sealBossTypeSetMatchesBossTrialKindOf,
displayScaleSanitizesLikeBossTrial,
intakeFactorMirrorsBossTrialIntake,
bossDisplayRecoversThePopUpNumber (sub-cap exact 4→4,000;
wither f=1024/21900 recovery 0.187→4,000; ladder order kept),
bossDisplayNeverAmplifiesOnJunk,
bossScaledAmountsRankInPopUpSpace (tracker-level: scaled boss
row ranks at 4k over a 12-HP trash row; sidebar renders "4k").
EmCombat + bucket: emRoutingBucketsEliteVictimsAndSimulatedAmounts,
simulatedFloorClearsEveryRealPool (2048 real max vs the floor;
NaN safe), emBucketNeverRanksAndNeverMergesIntoOverall,
emBucketKeepsTheSegmentClockAlive (EM-only segment closes on
its own quiet clock and announces as a real fight),
emBucketClearsAndResetsWithTheSegment.

## NOT re-run / unverified (stated plainly)

- The Paper rig was NOT run (same posture as 0.2.0/0.2.1):
  the seams' first live binds (BossTrial intake BOUND,
  EliteMobs BOUND), a real wither fight compared hit-by-hit
  against the action bar, and an elite fight exercising the
  bucket all ride the install window's proofs in
  INSTALL-PACKET.md (proofs 1–4 are the convicting ones).
- The exact finalDamage a MONITOR listener sees for a
  high-level EM scaled-combat hit is NOT desk-verifiable from
  the jar (DESIGN §2); the bucket + floor handling is correct
  under either answer, and proof 4 observes it live.
- Chapter-off edge: scaling keys off boss type + BossTrial
  present (BossTrial's own kindOf test); a chapter disabled
  in BossTrial's config would still scale in the meter.
  Chapters ship enabled; the install boot check covers it.
- The killing blow is counted by the meter but excluded from
  BossTrial's fight record (PM-2) — one hit of divergence in
  totals vs the post-fight pop-up, by design (DESIGN §1c).

## Files

- Source + tests + build script: `build-0.2.2/ExcelDetails/`
  (delta vs 0.2.1, tree-diff verified: +BossScale.java,
  +BossScaleLookup.java, +EmCombat.java, +EliteLookup.java;
  SegmentTracker, DetailsListener, DetailsCommands,
  ExcelDetails, BuildInfo, DetailsPlaceholders, plugin.yml,
  config.yml, local-build.sh, SOURCE.md, tests touched —
  nothing else).
- Packet: `build-0.2.2/INSTALL-PACKET.md`. Design:
  `build-0.2.2/DESIGN.md`. Fingerprints:
  `build-0.2.2/FINGERPRINTS/SHA256SUMS`.
- Records package staged:
  `~/workspace/records-catchup-20261103/packages/exceldetails-0.2.2-20261110/`
  (SHA256SUMS self-verified). NOT published — supervisor lane.
