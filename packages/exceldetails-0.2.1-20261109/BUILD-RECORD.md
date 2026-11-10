# ExcelDetails 0.2.1 — BUILD RECORD (Nov 9, 2026)

Design: `~/workspace/details-meter-20261109/build-0.2.1/DESIGN.md`.
Commission: Mike's live report + ruling, Nov 9, 2026 (~5:11 PM CST) —
the meter showed him players who were not in his party; it should
show only party members + yourself (solo: only yourself). Parent
source-verified the defect in the shipped 0.2.0: one server-wide
SegmentTracker, SidebarRenderer rendering tracker.ranked() with no
party filter.

**Status: BUILT + UNIT-PROVEN at desk. NOT installed. MANIFEST
entry draft alongside (MANIFEST-ENTRY-DRAFT.md); install is a
parent hand window. Records/GitHub/Drive are supervisor lanes.**

## Base proof

The build tree is `build-0.2.0/` copied whole into `build-0.2.1/`.
BEFORE any edit, the unmodified copy was rebuilt with its own
local-build.sh: 28/28 tests green and the jar came out
**byte-identical to the shipped live 0.2.0** — sha256
`f216f81eefd8d09cc6abfc14efbd2b884803f6c3717d9d9946766b836cd538cb`
(37,130 B), build EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19. The
0.2.1 deltas below are therefore the whole difference from live.

## Candidate

- `ExcelDetails/ExcelDetails-0.2.1.jar` — 41,116 B,
  sha256 `7c5d56f3a7e70f147b14101b15d224144ae732735e7b5159efb5aba9d074251c`
- Deterministic double-build byte-identical, AND a second full
  build run reproduced the same sha256. Fingerprint gate PASS
  (build EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a, Layer B in-jar:
  excel-build.txt + BuildInfo fragments; semantic marker
  EXC-DETAILS persists from 0.1.0/0.2.0 per the standard).
- Unit tests: **35/35** — the 28 from 0.2.0, unmodified + passing,
  plus 7 new scope tests (below).

## What 0.2.1 changes (display scoping ONLY)

1. **PartyScope (new, pure).** scopeOf(viewer, togetherMembers,
   emMembers) = {viewer} ∪ both member sets (null-safe);
   visibleRows(ranked, viewer, scope) filters ranked rows to the
   scope, ranked order preserved, own row unconditionally visible;
   totalOf(rows) sums the FILTERED rows.
2. **PartyLookup (new, Bukkit seam).** Reflection-only soft seams,
   resolved lazily at render time, handles cached, outcome logged
   once per seam (BOUND / UNBOUND), every failure fails soft to an
   empty member set for that system:
   - EliteMobs 10.9.12 — `PartyManager.getParty(UUID)` static →
     `Party.getMembers()`. Verified by javap against the actual
     jar on disk (~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar,
     plugin.yml version 10.9.12): both members public + static as
     required.
   - ExcelTogether 0.5.3 — plugin instance field `mgr` →
     `PartyManager.partyOf(UUID)` → public `Party.members` roster.
     Surface taken from the shipped records package source
     (~/workspace/records-catchup-20261103/packages/exceltogether-0.5.3-20261104/src/
     com/winkreyes/exceltogether/PartyManager.java + ExcelTogether.java:
     the manager is a private field with no public accessor, hence
     the field reflection; partyOf/members are public).
   A dead seam can only HIDE party rows — the scope floor is the
   viewer alone, so no failure mode re-opens the server-wide view.
3. **SidebarRenderer.linesFor** computes the viewer's scope once
   per render, filters each ticked section's rows through
   PartyScope, and builds each Section with the recomputed
   (filtered) total — share % is within-group. Own-row pinning,
   budget shedding, churn and no-data behavior are 0.2.0's code
   operating on the filtered set; a viewer with no in-scope rows
   gets no box.
4. **Version stamps:** plugin.yml 0.2.1, BuildInfo fragments,
   DetailsPlaceholders.getVersion() "0.2.1", build script knobs.
   Bundled config.yml is byte-identical to 0.2.0's (sha256
   29cb1903… in both trees) — the fix needs no config, and data.yml
   format is untouched (jar swaps either direction need no data
   migration).

## Scope audit — every other cross-player surface (task item 3)

- `/details` status readout: sender's OWN current stats only —
  already private, unchanged.
- Drill-down: NONE EXISTS in 0.2.0/0.2.1 — the by-target/by-source
  breakdown maps are tracked but never rendered to any viewer.
  Nothing to scope.
- `%details_show_*%` placeholders: the requesting player's own
  checkbox prefs — already private.
- Per-player overall totals: keyed per player, read back only to
  that player — already private (verified in DetailsCommands +
  DataFile paths).
- Console segment-close announce: no player rows.
- **The segment itself is NOT split per party** (by design — see
  DESIGN.md): tracking, segment clock, close/merge, and overall
  totals are 0.2.0's shared behavior, byte-for-byte.

## New unit tests (7, all on the pure PartyScope filter)

- scopeSoloShowsOnlyOwnRows — solo scope → only the viewer's row,
  total = own amount.
- scopePartyMemberVisibleNonPartyFiltered — party member visible,
  non-party combatant filtered, ranked order preserved.
- scopeIsTheUnionOfBothPartySystems — Together ∪ EM, null sets
  never shrink below the viewer.
- scopeFloorIsSelfOnlyWhenScopeMissing — null scope → own row only;
  a scope missing the viewer never hides the viewer's own row.
- sectionTotalRecomputedOnFilteredRows — healing section total
  100 (30+70) not 1000 with a 900 stranger excluded; rendered
  share reads (30%), stranger's name nowhere in the lines.
- scopedViewerWithNoInScopeRowsGetsNoBox — only a stranger fought
  → zero lines (the existing no-data behavior, per-viewer).
- scopedOwnRowStillPinnedUnderCut — viewer rank 6 in his own
  party keeps his row at maxRows 2; a 999-healing stranger neither
  appears nor displaces him.

One test was corrected mid-build (scopeFloor…): its first draft
expected a {B} scope to yield 1 row; the function's contract is
additive visibility over the own-row guarantee, so {B} correctly
yields 2 (own + B). The test now asserts the contract; production
scopes always contain the viewer anyway (PartyScope.scopeOf).

## NOT re-run / unverified (stated plainly)

- The Paper rig was NOT run (same posture as 0.2.0): Bukkit-level
  proof — both seams binding live (the two BOUND log lines), a real
  two-party + one-stranger fight read from three seats, Bedrock
  rendering — rides the install window's player proofs in
  PACKET-DRAFT.md.
- The reflection seams are compile-clean but their first live bind
  is the install boot/render: if a seam reads UNBOUND in the boot
  log, the meter still works (party rows from the other system +
  own rows) — investigate before calling the fix complete.
- ExcelTogether's `mgr` field name is source-verified for 0.5.3
  (the live version); a future Together release renaming it would
  fail that seam soft (UNBOUND line), never crash the meter.

## Files

- Source + tests + build script: `build-0.2.1/ExcelDetails/`
  (delta vs 0.2.0: +PartyScope.java, +PartyLookup.java;
  SidebarRenderer, ExcelDetails, BuildInfo, DetailsPlaceholders,
  plugin.yml, local-build.sh, SOURCE.md, tests touched — nothing
  else; verified by tree diff).
- Packet: `build-0.2.1/PACKET-DRAFT.md`. Surfaces:
  `build-0.2.1/SURFACES-DRAFT.md`. MANIFEST entry draft:
  `build-0.2.1/MANIFEST-ENTRY-DRAFT.md`.
