# BUILD RECORD — ExcelSummons 0.1.5 (Archer equipment guarantee)

Built Nov 11, 2026 (supervised desk BUILD unit for the WRV board
supervisor). Diagnosis of record:
`~/workspace/goals/family-minecraft-server-build/hidden_files/archer-bow-diagnosis-20261110.md`
— Jester's Nov 10, 2026 report (the archer summon spawns without
a bow); layer-1 desk check DONE that night (the applied spells
config is CORRECT — `item_in_hand: bow` + tier helmet specified
at every tier; Magic's SpawnEntity layer does not apply
item_in_hand on spawn); this packet is the diagnosis's layer-2
fix route: the bridge-side equipment guarantee. **Not
installed.** The install packet is `INSTALL-PACKET.md` in this
folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file (this lane
did NOT touch the MANIFEST, the parent-backlog, or board
state). Records package staged in this folder at
`records-package/` and at
`~/workspace/records-catchup-20261103/packages/excelsummons-0.1.5-20261111/`
(shape mirrors the 0.1.4 package); GitHub/Drive legs are the
parent's (see `PUSH-NOTES.md`). The 0.1.4 build folder is
untouched — its jar still hashes to its record value (verified
at close-out).

## Base proof

- Authoritative base: the INSTALLED 0.1.4 tree at
  `~/workspace/summons-014-build/` — its BUILD-RECORD/SOURCE
  name `ExcelSummons-0.1.4.jar` (63,642 B, SHA-256
  `a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a`,
  build `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19`), re-verified
  on disk Nov 11, 2026; 0.1.4 is live since Nov 11 ~2 AM
  (INSTALLED.txt in the 0.1.4 folder).
- DELTA RULE, verified before the change: a clean-room copy of
  the untouched 0.1.4 tree rebuilt ExcelSummons-0.1.4.jar
  **byte-identical** to the shipped jar (a34d7bdf…25a), 69/69
  tests green at the 0.1.4 totals. The 0.1.5 delta was applied
  to a copy of that verified base (0.1.5 therefore includes
  everything 0.1.4 has). The build was NOT made on the 0.1.3
  tree.

## Candidate

- `ExcelSummons-0.1.5.jar` — **65,253 B** — SHA-256
  `5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e`
  — MD5 `f19b51d7b75966e799897bc4ebabb4b0`
- Deterministic double-build byte-identical, AND a clean-room
  rebuild and a rebuild from the shipped source zip each
  reproduced the same hash (three independent builds, 74/74
  tests in each).
- Fingerprint gate PASS — build
  **EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315**.
- `ExcelSummons-0.1.5-source.zip` — 82,417 B — SHA-256
  `9662de9e9d680528ef0a54edf392fcf27c58a2c947aa5f127fd6dca44c1c7ca4`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.5.md + DESIGN-0.1.4.md + DESIGN-0.1.3.md;
  timestamps pinned 2026-11-11; zip creation itself verified
  deterministic — two creations byte-identical).
- Packet companions: NONE. No modifiers addition, no messages
  addition, no config change — 0.1.5 is a jar-only swap (see
  Install-critical differences).

## What shipped

Change-scope diff vs the verified 0.1.4 tree — only:
ArcherEquipment.java (new), SummonsListener.java, BuildInfo.java,
SummonsTest.java, plugin.yml, local-build.sh, DESIGN-0.1.5.md
(new). Everything else byte-for-byte 0.1.4 (diff-proven) —
including config.yml, byte-identical.

### The guarantee (diagnosis layer 2)

- **Spec — `ArcherEquipment` (new, pure).** Family / tier /
  material-name logic with no Bukkit types, in the house policy
  shape: `guarantees(key)` is true only for the archer family
  (via SummonRecord.baseKey's own fold); `handMaterialName` is
  BOW at every tier; `helmetMaterialName` is IRON_HELMET at the
  base tier and CHAINMAIL_HELMET at tier 2+ — exactly the
  `entity_parameters` the spell definitions carry (summons-archer
  delta; DESIGN §2.2–2.3). `matches(hand, helmet, key)` is the
  read-back verification: both slots must equal the spec.
- **Application — `SummonsListener.applyArcherEquipment`.**
  Called from `onCast` immediately after registration when the
  cast family is archer — the same position and shape as the
  ghast's `applyHarness` call beside it. It sets the main hand
  and head slots (materials resolved BY NAME, like the harness),
  then reads both slots back and verifies them against the
  spec; a mismatch logs one INFO line. The whole body is wrapped
  in a catch-all: **soft-fail only** — an equipment failure can
  never break registration or the cast, and the archer-only /
  living-entity guards live inside the method, so no caller can
  gear the wrong summon. The method is public so the desk rig
  exercises this exact code on real spawned entities.
- **Tier knowledge — exact, and its limit named.** The registry
  record folds tiers into the family base key, so the record
  alone cannot say which helmet a registered archer wears. The
  guarantee does not need it to: it runs at cast capture, where
  the CAST-TIME spell key (`archer|2`) is in hand, so the tier
  is known exactly and no record field was added. The honest
  limit: the guarantee exists only at that registration moment.
  It does not re-apply to an already-registered archer — none
  can predate the cast that made it (combat summons are
  temporary; none persist across a restart, so every live archer
  after install was registered by 0.1.5's own capture) — and it
  does not police later equipment changes by other plugins or
  mechanics. An unparsable tier suffix fails safe to the BASE
  spec (iron helmet), never a guessed upgrade; a hypothetical
  tier above the book continues the tiered (chainmail) spec.

### Deliberately NOT in scope

- **No spells.yml edit of any kind.** The spell config was
  exonerated by the diagnosis's layer-1 check; this is a bridge
  plugin change only (the build's spell-logic gate: nothing to
  re-run, config pins confirm the bridge config unchanged).
- Whether Magic applies the HELMET in-game is still open
  (Jester's screenshot pending) — and now moot: the guarantee
  sets and verifies both slots regardless of which half Magic
  dropped.
- Equipment drop chances stay vanilla (untouched, like every
  other behavior): the guaranteed gear is a bow and a basic
  helmet on a 60–150 s temporary summon. A drop-chance hardening
  is a possible future packet, not this one's scope.
- Caps, reserve, dismiss, messaging, nameplates, the target
  engine, lifetimes: unchanged from 0.1.4.

## Tests

- **SummonsTest 74/74** (three builds): 69 carried 0.1.4 tests
  + 5 new ArcherEquipment tests (tier coverage, family
  exclusions, fail-safe tier parsing, verify-matches,
  verify-rejects — including a pin of the reported defect
  itself: the empty hand fails verification). Carried tests
  changed, each a version re-pin: `pluginYmlPins014` →
  `pluginYmlPins015` (version + fingerprint lineage assertions
  only); `configPins014` → `configPins015` (rename only —
  config.yml is byte-identical, assertions verbatim). All other
  carried tests pass unmodified at their 0.1.4 totals.
- **Rig** (Paper 26.3-235 + Magic 11.2.5, `rig-0.1.5/`): clean
  0.1.5 enable, **SEVERE 0**, **reserve-template warnings 0**,
  **archer-equipment soft-fail lines 0**, **19/19 behavior
  checks** — the 14 carried 0.1.4 checks at their 0.1.4 verdicts
  + phase F: base / tier-2 / tier-3 equipment asserted on real
  spawned skeletons (equipment cleared first to reproduce
  Magic's bowless spawn) through the bridge's own
  registration-path method, the archer-only guard on a bruiser
  husk, and the non-living soft-fail no-op. Full results:
  `rig-0.1.5/RIG-RESULTS.md`; log: `rig-0.1.5/boot.log`.
- **Sustainability**: no cost, cooldown, cap, or reserve value
  moved anywhere in this packet; the guarantee spends nothing
  and holds nothing, so there is no new mana math to gate. The
  0.1.4 sustainability pins ride inside the carried suite
  (sustainabilityAcrossPoolTiers, configPins015) and pass
  unmodified.

## Install-critical differences from 0.1.4

0.1.5 is a **jar-only swap**: the live config is already the
0.1.5 config (byte-identical file), Magic's modifiers.yml and
messages.yml need no merge, and summons-data.yml gains no new
fields (no record change) — rollback is the plain jar swap in
both directions with no data note. The live-jar gate is the
installed 0.1.4 jar (63,642 B, SHA-256 a34d7bdf…25a). Full
steps: INSTALL-PACKET.md.
