# ExcelTogether 0.5.1 — SOURCE / provenance

Build folder: `~/workspace/party-mesh/build-0.5.1/` (the party-mesh project's convention:
build-0.5.0 sits beside it; parent paperwork in `~/workspace/party-mesh/PLACEMENT.md`).

## What 0.5.1 is

0.5.0 (the party mesh) plus the Repair Equipment redesign from
`~/workspace/spell-audit/REPAIREQUIP-REDESIGN.md` (its "Flavor B"): ExcelTogether registers
two custom Magic actions so the `repairequipment` spell repairs the whole party at once.

- `ExcelPartyRepairAction` (registered as **ExcelPartyRepair**) — a CompoundEntityAction whose
  entity set is the caster's ExcelTogether party roster (caster included; solo = caster alone).
- `ExcelRepairItemAction` (registered as **ExcelRepairItem**) — the write-back twin of Magic's
  stock RepairItemAction: same parameters (`armor` / `held_item`), same slot scope (four armor
  slots + main hand, never the offhand), same wand exclusion — but each repaired item is read,
  zeroed, and **set back through the equipment setter**, which is the only path that changes
  worn gear on Paper 26.3 (the stock action mutates the copies `CraftEntityEquipment` hands
  out and so repairs nothing; study §4, bytecode-verified).
- Supporting cores: `EquipmentRepair` (the repair itself, behind a DamageModel seam),
  `PartyRoster` (online / same-world / optional-range selection), `PartyRepairBridge`
  (binds the PartyManager for Magic-instantiated actions), `ReflectMagicLink` (reflective
  ActionFactory registration, house fail-soft style).

Registration happens in `ExcelTogether.onLoad` — every plugin's onLoad runs before any
onEnable, and Magic resolves spell action classes in its own onEnable, so the classes are in
the registry before Magic loads its spells (rig-proven, TEST-REPORT L1). An onEnable retry
covers a load order where onLoad could not see Magic. Magic stays a **softdepend**: absent,
the actions never register, one awake-line segment reads OFF, and every party feature works
exactly as in 0.5.0 (rig-proven, TEST-REPORT L2).

## Base proof (delta discipline)

The unmodified 0.5.0 tree was copied into this folder (`src/`, `tests/`, `plugin.yml`,
`config.yml`, `build.sh`, `work/pulled-0.4.3.jar`) and built BEFORE any 0.5.1 edit:

    ExcelTogether-0.5.0.jar: sha256 f4490814a9d4ec2e33540126adf6b418a0b58a50b27172119e45950d67353529
    (149,568 bytes) — byte-identical to the shipped live jar, identical across two builds.

The 0.5.1 delta was then applied to that proven tree. Nothing was ported from anywhere else.

## Candidate

    ExcelTogether-0.5.1.jar
    sha256  f2e5b7d3284474c250852dabde708efe3fea2f1dc1109ad6c763e564de2f517f
    size    159,821 bytes — built twice by build.sh, hashes identical (reproducible).

## Change scope vs the shipped 0.5.0 jar (entry-by-entry)

- ADDED (8): `EquipmentRepair.class`, `EquipmentRepair$1.class`,
  `EquipmentRepair$DamageModel.class`, `ExcelPartyRepairAction.class`,
  `ExcelRepairItemAction.class`, `PartyRepairBridge.class`, `PartyRoster.class`,
  `ReflectMagicLink.class`.
- CHANGED (3): `ExcelTogether.class` (onLoad registration, setupMagicLink, awake-line
  segment, bridge unbind), `plugin.yml` (version 0.5.1, Magic added to softdepend,
  description sentence), `config.yml` (one appended toggle, `magic-party-repair: true`).
- REMOVED: none. The other **73 shared entries are byte-identical** to the shipped jar
  (the 0.5.0 hybrid assembly against `work/pulled-0.4.3.jar` is unchanged).

## Build inputs

- JDK 25.0.5+8 (`~/workspace/.jdk25`), `javac --release 21`, deterministic assembly
  (sorted entries, fixed 2026-11-01 timestamps, built twice + hash-asserted) — unchanged
  from 0.5.0's build.sh except: the version pins, the PartyRepairTest run line, and one new
  pinned input:
- **Magic 11.2.5** (`work/magic-11.2.5.jar`, sha256
  `79e49fe32fdc4e25375c0099d2bd10665b9792bfd9b35fab8d64a498ebfc3532`, copied from the
  spell-audit rig; build.sh asserts the hash) — compile-time ONLY, for the two action
  classes' Magic base classes. No Magic bytes are bundled into the jar.
- Test classpath additionally uses Jobs/CMILib, floodgate, both mcMMO jars — same as 0.5.0.

## Companion deliverables in this folder

- `staging/upload-spells-{s2,s3,s4,s5}.yml` — the Flavor B `repairequipment` block staged
  onto the Fling-pass bases (see INSTALL-PACKET-REPAIREQUIP.md for hashes and sequencing).
- `INSTALL-PACKET.md` (jar swap), `INSTALL-PACKET-REPAIREQUIP.md` (spells reload),
  `TEST-REPORT.md` (unit + rig evidence), `CHANGELOG-0.5.1.md`.
- `rig/` — the scratch Paper 26.3-235 rig (server clone + PartyRepairRig driver source)
  and the three boot logs `boot-L1/L2/L3-console.log`. Rig-only; never shipped.
