# ExcelSummons 0.1.5 — Archer equipment guarantee (design summary)

An Excel plugin note for Wink Reyes Valley. November 11, 2026.
Diagnosis of record:
`~/workspace/goals/family-minecraft-server-build/hidden_files/archer-bow-diagnosis-20261110.md`
— Jester's Nov 10, 2026 report (the archer summon spawns without
a bow), layer-1 desk check DONE the same night, layer 2 (this
packet) the chosen fix route. Base: the installed 0.1.4 tree.

## The defect

The archer spell definitions specify the gear at every tier —
`item_in_hand: bow` at all tiers; `helmet: iron_helmet` at the
base tier, `chainmail_helmet` at tiers 2/3 (packet-summons-
20261108 summons-archer delta; DESIGN §2.2–2.3 — the helmet is
also the skeleton's sun-proofing). The layer-1 desk check
verified the applied spells output carries exactly those values
at every tier: the config is CORRECT. Magic's SpawnEntity layer
does not apply `item_in_hand` on spawn, so the archer takes the
field bowless (a vanilla skeleton would normally spawn holding
a bow — the spawn path is clearing / never applying hand
equipment). Whether the helmet applies in-game is still open
(Jester's screenshot is supporting evidence, not a gate); the
guarantee below sets and verifies BOTH slots regardless, so the
answer no longer matters.

## What changes vs 0.1.4

- **Archer equipment guarantee (new `ArcherEquipment` + one
  listener hook).** When the bridge registers a CAST archer at
  cast capture (`SummonsListener.onCast` — the only registration
  path an archer can take; sweep adoption registers wolves and
  cats only), it sets the skeleton's main hand to a bow and its
  helmet to the tier helmet, then reads both slots back and
  verifies them against the spec. The application mirrors
  `applyHarness` exactly: materials resolved by name, wrapped in
  a catch-all, one INFO line on failure — soft-fail only. An
  equipment failure can never break registration or the cast.
- **The spec is pure.** `ArcherEquipment` holds the family /
  tier / material-name logic with no Bukkit types, unit-tested
  off-server like every other house policy: bow at every tier;
  iron helmet at tier 1; chainmail at tiers 2+; every other
  family uncovered (null spec, no application); the read-back
  `matches` check both slots must pass.
- **Tier knowledge — exact, with one named limit.** The registry
  record folds tiers into the family base key (`archer|2` →
  `archer`), but the guarantee runs at cast capture, where the
  CAST-TIME spell key is in hand — so the tier is known exactly
  and no record field was added. The limit, stated plainly: the
  guarantee exists only at that moment. It does not re-apply to
  an already-registered archer (none can predate the cast that
  made it — combat summons are temporary and none persist across
  a restart), and it does not police later equipment changes by
  other plugins or mechanics. An unparsable tier suffix fails
  safe to the BASE spec (iron helmet), never a guessed upgrade.

## What does not change

Everything else is 0.1.4, byte-for-byte where untouched: caps,
reserve, dismiss, messaging, nameplates, the target engine,
lifetimes. No config change (config.yml is byte-identical to
0.1.4 — the guarantee is code, not configuration). No spells.yml
edit of any kind: this is a bridge plugin change; the spell
config was exonerated by the layer-1 check and is not touched.
Equipment drop chances are untouched (vanilla) — the guaranteed
gear is a bow and a basic helmet on a 60–150 s temporary summon;
a drop-chance hardening is a possible future packet, not this
one's scope.

## Proof shape

SummonsTest carries the spec (5 new tests over the 69 carried).
The desk rig spawns real skeletons through the bridge path —
registered as CAST archers and geared by the listener's own
`applyArcherEquipment`, the exact method `onCast` calls — and
asserts the live equipment at each tier, plus the archer-only
guard and the soft-fail shape on a non-living entity. The
post-install player proof stays Jester's (an in-game archer cast
after the fix lands), per the diagnosis.
