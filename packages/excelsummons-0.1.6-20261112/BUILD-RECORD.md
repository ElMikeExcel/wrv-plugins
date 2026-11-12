# BUILD RECORD — ExcelSummons 0.1.6 (Adopted pets hold no mana + sync clamp)

Built Nov 12, 2026 (supervised desk BUILD unit for the WRV board
supervisor). Defect of record: the unnumbered item at the end of
`~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
— "Summons reserve over-ceiling on re-derivation (Jester report,
Nov 12, 2026, ~2:55 PM CST)": Jester's all-adopted menagerie
(5 wolves + 2 cats + 3 familiars + 1 mount, all origin=adopted)
held **85%** of his 200 Master pool (wolf5 50 + cat2 10 +
familiar3 15 + mount 10) because the 75% ceiling + 20% floor were
enforced at PreCast only, while the login/sync re-derivation
applied the raw sum. Binding spec: **Mike's ruling, Nov 12, 2026,
~3:08 PM CST** — adopted-origin summons contribute 0% reserve
permanently; cast summons keep their rates; the sync clamp to the
75% ceiling + 20% floor still applies to cast sets. Design
summary: `DESIGN-0.1.6.md` in this tree. **Not installed.** The
install packet is `INSTALL-PACKET.md` in this folder; the MANIFEST
entry is drafted in `MANIFEST-ENTRY-DRAFT.md` for the supervisor
to file (this lane did NOT touch the MANIFEST, the
parent-backlog, or board state). Records package staged in this
folder at `records-package/` (shape mirrors the 0.1.5 package);
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.5 build folder is untouched — its jar still hashes to its
record value (verified at close-out).

## Base proof (the mandatory gate)

- Authoritative base: the SHIPPED 0.1.5 tree at
  `~/workspace/summons-015-build/` — its jar
  `ExcelSummons-0.1.5.jar` (65,253 B, SHA-256
  `5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e`,
  build `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315`), re-verified
  on disk Nov 12, 2026; 0.1.5 is the live build (installed
  Nov 11, 2026).
- BASE-REPRODUCTION GATE, run before any edit: a clean-room copy
  of the untouched 0.1.5 tree rebuilt ExcelSummons-0.1.5.jar
  **byte-identical** to the shipped jar (5cdf16a0…f61e), 74/74
  tests green at the 0.1.5 totals, double-build byte-identical,
  fingerprint gate PASS. The 0.1.6 delta was applied to a copy of
  that verified base (0.1.6 therefore includes everything 0.1.5
  has, including the archer equipment guarantee).

## Candidate

- `ExcelSummons-0.1.6.jar` — **66,209 B** — SHA-256
  `08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1`
  — MD5 `937fb7c6498bdfaf26e0a7e523bbaaad`
- Deterministic double-build byte-identical, AND a clean-room
  rebuild and a rebuild from the shipped source zip each
  reproduced the same hash (three independent builds, 78/78
  tests in each).
- Fingerprint gate PASS — build
  **EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a**.
- `ExcelSummons-0.1.6-source.zip` — 89,260 B — SHA-256
  `f717a0b5422a53f8ef4c6982f03c4e51dfb6d878fda4a17af4048cdd239a2b3b`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.6.md + DESIGN-0.1.5.md + DESIGN-0.1.4.md +
  DESIGN-0.1.3.md; timestamps pinned 2026-11-12; zip creation
  itself verified deterministic — two creations byte-identical).
- Packet companions: NONE. No modifiers addition, no messages
  addition, no config change — 0.1.6 is a jar-only swap (see
  Install-critical differences).

## What shipped

Change-scope diff vs the verified 0.1.5 tree — only:
ReservePolicy.java, SummonsListener.java, ExcelSummons.java,
BuildInfo.java, SummonsTest.java, plugin.yml, local-build.sh,
DESIGN-0.1.6.md (new). Everything else byte-for-byte 0.1.5
(diff-proven) — including config.yml, byte-identical.

### The adopted exemption (Mike's ruling)

- **One funnel.** `ReservePolicy.holdsReserve` is CAST-only;
  `reserveCounts`, `totalPercent`, and `desiredModifiers` all
  skip `origin=adopted` records through it, so an adopted pet
  contributes 0% at PreCast, at login/sync, at the drift sweep,
  and in the list lines alike — permanently, with no config
  switch. Cast rates are untouched (combat 10/15/25/20/25,
  wolf 10, familiar 5, cat 5, mount 10). Adopted records still
  count toward the rank count cap (CapPolicy is not in this
  diff) and are still never `dismiss pets` targets; the sweep
  adoption comment in SummonsListener now states the pet holds
  nothing (its sync call remains, as a re-derivation).
- **Legacy rows** with no origin key restore as ADOPTED (the
  0.1.4 safe default) — under 0.1.6 that also means they hold
  nothing: the same safe direction.

### The sync clamp (the re-derivation hole, closed)

- `desiredModifiers` gained a ceiling/floor overload: the
  applied state is built per record **in registry order**; a
  cast record's hold is included only while the running total
  stays within `min(ceiling, 100 − floor)`; a record that would
  pass the cap is skipped — it stays registered and alive, it
  simply holds nothing — and a smaller later record that still
  fits is still included (**skip-and-continue**, a build-time
  decision pinned by test; the alternative, stop-at-first,
  would leave a fitting later summon unheld for no player
  benefit, and reversing it later is a one-method change).
  Included per-family counts ride the existing count-tiered
  modifier keys, so a clamped set's keys are always a subset of
  the keys the raw set could produce — no new Magic templates.
- The listener's `syncReserve` passes the LIVE config values
  (reserveCeilingPct / reserveFloorPct); the three-argument
  form clamps at the shipped defaults (75/20). Because the sync
  diff removes any held key the desired state lacks, an
  over-ceiling or adopted modifier left by a pre-0.1.6 sync is
  actively removed at the first 0.1.6 sync — for Jester, all
  four of his modifiers clear at his next login and the full
  200 pool returns.
- **One truth for the numbers.** `appliedPercent` (the desired
  sum) is what the PreCast gate now reads as "already held" and
  what `/summons list` + the admin list print as the held
  total; `recordHolds` replays the clamp walk per record so a
  list line claims a hold only when that summon actually
  carries one. `totalPercent` remains the RAW cast total
  (unclamped) — the registry truth the diagnosis and tests
  read — and its javadoc says exactly that.

### Deliberately NOT in scope

- No config edit: config.yml is byte-identical to 0.1.5 — the
  exemption and the clamp are code, not configuration, and the
  live config's ceiling/floor values (75/20) are read as-is.
- No spells.yml or Magic-file edit of any kind.
- Count-cap treatment of adopted pets is unchanged (they still
  count; Mike's ruling was about mana only).
- Caps, dismiss, messaging texts, nameplates, the target
  engine, lifetimes, the archer guarantee: unchanged from
  0.1.5.

## Tests

- **SummonsTest 78/78** (three builds): the 74 carried 0.1.5
  tests + 4 new 0.1.6 tests. Carried changes, named: the reserve
  fixtures in 9 tests were re-based to CAST origin
  (`reserveAppliesOnCastAndStacksAcrossSummons`,
  `reserveCountTiersCarryTwoOfAFamily`,
  `reserveReleasesOnEveryDespawnPath`,
  `reserveFullSetLandsInTheToggleBand`,
  `reserveCountsCompanionsAndMount`,
  `reserveTierKeysForCompanions`,
  `reserveCeilingCountsMountAndCompanions`,
  `reserveReleasesOnUnregisterForEveryBranch`,
  `masterFifthExpensiveSummonRefusedOnCeilingNotCount`) — under
  the ruling, the old fixtures' records ARE adopted-origin (the
  bare `rec()` helper defaults ADOPTED, the legacy safe
  direction) and must hold nothing, so a reserve fixture that
  means a cast set now says so; every assertion in those tests
  is carried verbatim. Plus the two version re-pins:
  `pluginYmlPins015` → `pluginYmlPins016` (version +
  fingerprint lineage only) and `configPins015` →
  `configPins016` (rename only — config.yml byte-identical,
  assertions verbatim). All other carried tests pass
  unmodified.
- **New 0.1.6 tests:** `adoptedMenagerieHoldsNoMana` —
  regression fixture (a), Jester's exact set (5 wolves + 2 cats
  + 3 familiars + 1 mount, all adopted): raw total 0, counts
  empty, desired state empty, applied 0%, all 11 animals still
  registered, and a cast archer alongside holds its 10% alone;
  `castSetClampedAtSyncToCeilingAndFloor` — regression fixture
  (b), a 90% raw cast set (guardian 25 + ghast 25 + revenant 20
  + bruiser 15 + cat 5): applied exactly 75, usable 25 ≥ the
  20 floor, the bruiser holds nothing, the cat behind it still
  fits, nothing trimmed; `adoptedAndCastOfSameFamilyCountSeparately`
  — mixed origins share no tiers; `syncClampFloorBindsBelowTheCeiling`
  — at a hypothetical 90 ceiling the floor-derived cap (80)
  binds.
- **Rig** (Paper 26.3-235 + Magic 11.2.5, `rig-0.1.6/`): clean
  0.1.6 enable, **SEVERE 0**, **reserve-template warnings 0**,
  **archer-equipment soft-fail lines 0**, **21/21 behavior
  checks** — the 19 carried 0.1.5 checks with phases A–D reserve
  expectations corrected to the ruling (adopted holds nothing)
  + phase G: Jester's menagerie applying NO reserve against real
  Magic modifier state, and the 90% cast set clamped by the real
  drift sync to applied 75 (bruiser modifier absent, the rest
  present). One rig-driver correction mid-run is recorded in
  RIG-RESULTS.md (a stale phase-C expectation in the first run;
  the server was reset to its pristine start state and the full
  run repeated — the 21/21 tally is from the clean full run).
  Full results: `rig-0.1.6/RIG-RESULTS.md`; log:
  `rig-0.1.6/boot.log`.
- **Sustainability**: no cost, cooldown, cap, ceiling, floor, or
  reserve RATE moved anywhere in this packet; what moved is who
  holds (cast only) and the applied cap on re-derivation. The
  carried sustainability pins (sustainabilityAcrossPoolTiers,
  configPins016) pass; adopted pets holding nothing is a mana
  RELIEF by Mike's direct ruling, not a tuning drift.

## Install-critical differences from 0.1.5

0.1.6 is a **jar-only swap**: the live config is already the
0.1.6 config (byte-identical file), Magic's modifiers.yml and
messages.yml need no merge (the clamped key set is a subset of
the existing 18 templates), and summons-data.yml gains no new
fields (origin has been written since 0.1.4) — rollback is the
plain jar swap in both directions with no data note. Behavior
note for the install report: at the first login sync after
install, any player whose adopted pets held reserve under
0.1.5 (Jester above all) gets those modifiers removed and the
mana back — that is the fix landing, not a regression. The
live-jar gate is the installed 0.1.5 jar (65,253 B, SHA-256
5cdf16a0…f61e). Full steps: INSTALL-PACKET.md.
