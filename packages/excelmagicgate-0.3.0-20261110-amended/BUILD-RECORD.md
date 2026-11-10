# ExcelMagicGate 0.3.0 — Build Record (AMENDED build -02)

Date: 2026-11-10. This record covers the AMENDED 0.3.0 build in this
folder (`build-0.3.0-amended/`), which SUPERSEDES the unamended 0.3.0
build (`build-0.3.0/`, jar 237,422 B / `515fe1c3…`) before either was
installed. Design of record: `magic-rank-packet-20261110/DESIGN.md`
plus the amendment requirement below. Base tree: the verified
unamended 0.3.0 build tree, carried whole; the amendment's delta is
itemized under "Amendment delta" and is the only source change.

## The amendment (why this build exists)

Mike's explicit build requirement (Nov 10, 2026, ~1:05–1:08 AM CST,
parent-backlog "SUMMONS INVISIBLE — ROOT CAUSE FOUND" + "SPELLBOOK
AUTO-UPDATE — BUILD CLARIFICATION"; board-supervisor dispatch under
decide-and-log D2): the rank build MUST include a wand
spell-inventory reconcile as a STANDING SYNC on MagicGate's
reconcile sweep — any spell in the player's current-rank path lists
missing from their wand gets granted, so (a) rank-ups fill the book
automatically, (b) spells added later to a rank a player already
holds appear without a manual grant pass, and (c) a one-time grant
pass runs for current players at switch-over/placement (the first
reconcile per player IS that pass — no separate procedure).

Root cause it closes: the shipped 0.3.0 reconciler stepped wand
PATHS without granting the paths' spells (source-verified by the
tame-fix lane, Nov 10: MagicFloorHook "spells untouched"). The Nov 8
summoning tree (archer/bruiser/guardian/revenant/ghast/mount) is
defined in the live stage-4 book but sat on NO player's wand.

## Artifact

- Jar: `ExcelMagicGate-0.3.0.jar` — **243,572 B**, SHA-256
  `4cab2689d073a2a4e7141a31c24fd1a05cd7011fdf846a2fd84193e5a1836384`
- Build ID: `EXC-MAGICGATE-0.3.0-WRV-20261110-02-d2a310`
  (excel-build.txt verified inside the jar; plugin.yml version 0.3.0).
- **Deterministic double-build: PASS** — second clean build
  byte-identical, same SHA-256.
- Stage packets: byte-identical to the unamended 0.3.0 build and to
  0.2.1 (diff -r clean). Placement `earned.yml`: identical.

## Amendment delta (the only changes vs the unamended build)

- **NEW `WandInventorySync.java`** (pure logic, no Bukkit deps):
  base-key normalization, the expected-set computation (path spells
  + extra spells + supplemental grants, accumulated up to the
  target rank), the missing-set computation (sorted, idempotent by
  construction), and grant-slot assignment (next free slot after
  the wand's current max, in sorted order — the tame-fix packet's
  placement rule, applied standing).
- **`MagicFloorHook.syncInventory`** (Bukkit half): runs on every
  reconcile leg that reached a live wand — AT_TARGET, and after a
  step-up/demote keyed to the wand's RESULTING rank (never the
  intended target, so a stalled partial move cannot over-grant).
  The expected set is read from Magic's LIVE path object
  (`controller.getPath(rank)` → `getSpells()` + `getExtraSpells()`,
  which already accumulate through the path inherit chain) plus
  the config supplemental grants. Grants write through Magic's own
  API — `addSpell` (`forceAddSpell` fallback) to register the
  spell, `updateSpellInventory` to place it when the inventory map
  lacks it — the runtime form of the two mage-file representations
  (parsed `spell_inventory` + item `custom_data`) the tame-fix
  packet §4 names. Every grant is verified by re-discovering the
  wand and re-reading its spells + inventory; unverified spells are
  logged by name and retried by the next sweep (the sync is
  standing, so a failure is a delay, never a silent loss). Only
  ever ADDS: demotion still costs only the frame. Parked /
  not-carried wands never reach the sync (the rank leg's deferral
  covers the whole reconcile, sync included).
- **Supplemental grants (config, `earning.inventory-reconcile.
  supplemental-grants`)**: the summoning tree sits in NO path list
  (its packet added spell definitions only), so the path-driven
  sync alone would never see it. Rank placement follows the tree
  design of record (magic-summoning-tree/DESIGN.md §2.2 + the Nov 8
  re-tier correction moving the elite tier to stage 4): `archer`
  at student (stage-2 root; its |2 tier requires apprentice and |3
  master in the live book), `bruiser` at apprentice (stage 3),
  `guardian`/`revenant`/`ghast`/`mount` at master (stage-4 tier).
  Config-carried so a ruling corrects without a rebuild; the code
  defaults mirror the shipped config and the suite pins them equal.
- **`ExcelMagicGate`**: `inventorySyncEnabled()` +
  `supplementalGrants()` accessors, an enable-time log line, and an
  "Inventory sync" line in `/magicgate status` (switch + last sync
  summary; grant counters ride the existing counter line).
- **`config.yml`**: the `earning.inventory-reconcile` section
  (enabled + supplemental map), commented in the packet voice.
- **`plugin.yml`**: description gains the inventory-sync sentence.
  Version stays 0.3.0 — this is the same release, amended before
  first install; the build ID (-02) is the distinguishing stamp.
- **Design safety answer** (the task's named blocker check): Magic's
  wand data CAN be written safely from MagicGate — through Magic's
  own CasterProperties API (`addSpell`/`forceAddSpell`/
  `updateSpellInventory`/`getSpellInventory`, javap-verified on the
  11.2.5 jar and read in the 11.2.5-SNAPSHOT source), the same
  property seam the proven demote leg already writes through
  (`setPath`/`setManaMax`). No raw mage-file or NBT editing anywhere
  in the runtime path. Rig-proven with restart persistence in BOTH
  mage-file representations (see RIG-VERDICT amendment section).

One behavior the rig surfaced, stated plainly: the expected set is
Magic's path object, not the paths.yml text. At master the text
names 65 spells (58 path + aqualung + 6 supplemental); the live
path object carries 64 — `disposal` is `enabled: false` in the
book, so Magic drops it at path load and the sync neither grants
nor retries it. That is the correct authority: a spell the path
itself does not carry is not grantable, and the fixtures corroborate
it (no pulled wand holds disposal either). `wolf` is the mirror
case: held as a class-side spell (in `getSpells()`, not in
`spell_inventory`) and correctly read as held.

## What 0.3.0 adds (carried from the unamended record, unchanged)

- **Earned ranks** (Mike's Nov 9 lock): target = min(earned rank,
  stage ceiling); thresholds Student 100 / Apprentice 400 / Master
  2,000 credited casts, totals only. Pure logic: `EarnedRank`,
  `CeilingPlan`.
- **Credit feed**: `MagicCreditHook` registers Magic's PreCast/Cast/
  PathUpgrade events reflectively. Policy in `CreditPolicy` + config
  `earning:` block: no-credit movement/utility/passive families;
  heal-family effect gates via observed CUSTOM
  `EntityRegainHealthEvent`; damage families strict + correlated +
  player-target pair window (5 credits / pair / 10 min); feed
  credited on regain OR food-increase; daily cap 50 per family per
  player (America/Chicago day); unknown families default credited
  success-only.
- **Demotion** (DESIGN §1 Option A): reflective setPath + explicit
  frame writes, per-step re-read verification, spells never removed.
  Kill switch `earning.demotion_enabled` ships **true** (rig-proven).
- **Wand lore**: `WandLoreHook`, property seam verified on the rig.
- **earned.yml** store (`EarnedStore`): placement rows + live
  accrual, fallback Mage-API seeding, 30 s debounced flush.
- With `earning.enabled: false` the plugin is behavior-identical to
  0.2.1 (the inventory sync is part of the earning reconcile and is
  off with it).

## Tests

- **276 checks, 0 failures** (`run-tests.sh`, full output
  TEST-OUTPUT.txt): all 253 carried 0.3.0 checks (0.2.1 regression,
  EarnedRank, CeilingPlan, CreditPolicy, EarnedStore, config
  agreement, §10 credit-logic simulations, placement reproduction
  EXACT over the 11 Nov 9 fixtures) + **23 amendment checks**:
  - WandInventorySync pure logic: normalization; expected-set
    accumulation (beginner ⊂ student ⊂ apprentice ⊂ master);
    above-rank spells never in the grant set; held tier keys count
    as held; idempotence; slot assignment (max+1 upward; empty wand
    starts at 0).
  - Config agreement: shipped supplemental map == code defaults.
  - **Placement grant pass over the REAL Nov 9 fixtures** (parsed
    `spell_inventory` from each mage .dat, stage-4 path lists):
    Jester (Master) master wand gains exactly the six tree spells +
    disposal (text-level set) and does NOT re-grant tame (already
    held); Marvin's apprentice wand at his PLACED Master rank fills
    with 14 spells incl. `tame` + the six, at slots 80–93 after his
    max 79 — and at an APPRENTICE target gains only
    archer/bruiser/disposal (no tame, no master tree tier — the
    named negative); Mike (placed Apprentice) student wand fills
    with 13 incl. archer/bruiser and no master spells; post-grant
    second sweep is empty in every case.
- Spell logic tests (spell-audit harness): NOT APPLICABLE — no spell
  config touched (stage packets byte-identical); the credit-logic
  simulations carried from 0.3.0 stand.

## Rig verdict (summary — full text in RIG-VERDICT.md)

The unamended build's demotion/credit/lore proofs stand (same code,
carried). The amendment rig (3 boots on Paper 26.2 + Magic 11.2.5,
stage 4, amended jar) PROVES the inventory sync end-to-end:
a Master-target wand granted 46 spells incl. **all six summoning
spells + tame, verified on the wand** (18 → 64 spells); an
Apprentice-target wand granted archer + bruiser and NOT
guardian/ghast/tame; a pouch-parked wand sat untouched through a
full sweep (18 spells, DEFERRED) and filled to Master on
unpark + activation; a second sweep reported "in sync"; and after a
clean restart the wand returned from Magic's saved mage data with
all six in BOTH representations (parsed `spell_inventory` with
slots + item `custom_data`). Zero SEVERE, zero INCOMPLETE, zero
MagicGate WARN/ERROR in the repaired-book boots. One rig-side
finding, recorded in the verdict: the inherited rig carried the
stale 142-key book (0.2.0-era extracted stage templates), under
which the six spells do not exist at all — boot 1's path-grant and
pouch proofs stand, and its supplemental INCOMPLETE lines are that
staleness, repaired before boots 2–3 (the sync's refusal to count
unverifiable grants is itself part of the proof).

## Fingerprint (per FINGERPRINT-STANDARD.md)

See FINGERPRINTS.md. Layer A clean. Layer B build ID above. Layer C:
all carried 0.3.0 markers + new natural markers: the
`inventory-reconcile` config phrasing, the supplemental-grants map,
and the sync message voice ("Wand inventory sync: X granted N
spell(s) at master — … verified on the wand.", "Your spell book
fills in: …"). Registry row: NOT written by this build (left to the
supervisor's records leg with the MANIFEST filing, as before).

## Scope compliance

Built and rig-proven only. NOT installed, NOT published, MANIFEST
entry NOT filed (superseding draft staged as
MANIFEST-ENTRY-DRAFT.md). The amended records package is staged as a
SIBLING of the in-flight unamended package (see PUSH-NOTES.md) and
does not overwrite it. Install packet: INSTALL-PACKET.md (install
steps unchanged vs the unamended packet except the superseded jar
identity, plus the sync's first-reconcile behavior notes).
