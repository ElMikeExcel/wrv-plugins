# Melee Mitigation Module — ExcelEMFixes (D206 as amended)

- Status: BUILT + PROVEN (D122 ship-it gates met: deterministic build, automated suites, rig/mirror load proof SEVERE 0, toggle-off proof). NOT installed, NOT filed to MANIFEST (per hard limits — filing/install are later lanes).
- Dispatched: 2026-11-23 ~1:28 PM CST by movable-work audit (STUCK-MOVABLE)
- Approved: Mike, Nov 23 2026 ~12:14 PM CT; amended ~12:18 PM and ~12:40 PM (off-hand split)
- Lane: ~/workspace/melee-mitigation-20261123/ — desk/mirror work only
- Hard limits observed: NO MANIFEST.md edit, NO live install, NO player messages

## Spec implemented (D206 as amended — supersedes older melee-premium notes; note melee-premium is a separate, still-live module that boosts melee damage DEALT ×1.15 — this module is damage TAKEN)
- 15% damage mitigation keyed on MELEE WEAPON IN HAND (explicitly NOT class-based) — `base-percent: 0.15`
- Off-hand bonus SPLIT: TOTEM = 2% (`totem-bonus-percent: 0.02`); SHIELDS / shield-like = 7% (`shield-bonus-percent: 0.07`)
- Off-hand bonus pays ONLY while main hand holds a melee weapon (sword+totem yes 2%; wand+totem NO — enforced in glue: bonus is only ever queried after the melee test passes)
- Off-hand item shows on-item indicator with its own % ("7% mitigation activated" / "2% mitigation activated") while active; nothing when inactive (lore reconcile adds/removes exactly that one line, original lore restored byte-for-byte — unit-proven)
- ALL numbers are CONFIG knobs (base/totem/shield/max-total, material lists, causes, worlds, indicator ticks)
- Reuses TrainingWheels scaling bones: victim-axis damage scaling at HIGH, damage × (1 − total), ShieldRule.scaled shape, never a cancel/side-channel (see MitigationMath javadoc)
- NO lifesteal/self-heal anywhere in the module
- Documentation rule satisfied: plain-language explanation in BOTH the config section comments (src + deploy config.yml, `melee-mitigation` section) AND README (`melee-mitigation` section)
- Public fix pack discipline: expansion-generic, data-driven material lists, ZERO WRV-specific hardcoding (WRV runs shipped defaults; config comments note that), individually toggleable (`modules.melee-mitigation.enabled`)

## Phases
- [x] DISPATCHED — record created write-first
- [x] Audit (weapons + off-hand)
- [x] Build
- [x] Automated tests
- [x] Mirror/rig load proof (module on + toggle off)
- [x] Final record / jar hash

## Base / provenance
- Base tree: quest-beacon lane's verified ExcelEMFixes 0.6.0 tree (~/workspace/quest-beacon-20261123/build-root/), copied to build-root/ (base untouched).
- PROVENANCE GATE PASS: unmodified rebuild of the copied tree produced ExcelEMFixes-0.6.0.jar SHA-256 845d010be47281b142e7309735be14ef6931a809024acfb66ac4de3afcbc20f4, 376,523 B — byte-identical to the 0.6.0 jar. New pack version = 0.7.0 (17th module added), version stamped in ExcelEMFixes.VERSION, plugin.yml, build.sh, README install line.

## AUDIT — every melee weapon + shield-like off-hand item on the server
Source corpus: the live-content EliteMobs custom-items tree in the rig (build-root/rig/plugins/EliteMobs/customitems/), 849 .yml files, enumerated by material; plus ExcelItems registry (BuiltinItems.java) and the pack's own WeaponSkills/WeaponIdentityResolver usage.

### Melee families found (195 melee-material files)
- Swords (*_SWORD): DIAMOND 28, NETHERITE 17, IRON 11, STONE 5, GOLDEN 3, WOODEN 2 — incl. Primis gladius/blood-temple/starter swords, Wood League arena swords, event drops.
- Axes (*_AXE): DIAMOND 24, NETHERITE 2, IRON 2, WOODEN 2 + adventurer axe.
- MACE: 31 (adventurer mace, CraftenMine persuader, Primis blood-temple/catacomb/pirate/mines maces, Tommyknockers warhammer/cane/lantern on MACE).
- Spears (*_SPEAR): DIAMOND 20, WOODEN 18, IRON 5, NETHERITE 3 — see staff trap below.
- Hoes (*_HOE = EM scythes): DIAMOND 19, IRON 2, GOLDEN 1 — EM sells scythes on the hoe family (ag_adventurer_scythe = DIAMOND_HOE; catacomb scythe = IRON_HOE), so _HOE counts as melee.
- TRIDENT: 24 files — held trident is a melee weapon; while thrown it is not in hand, so it pays nothing then (by construction).

### NOT melee (excluded by design)
- BOW 43, CROSSBOW 26 (ranged); BLAZE_ROD 19 = EM wands; STICK charms; tools (pickaxes/shovels/fishing rods).
- THE STAFF TRAP (audit's key finding): EM staves are built on WOODEN_SPEAR (ag_adventurer_staff, every Primis "staves" loot file = WOODEN_SPEAR 18). A material-only classifier would pay mitigation to staff users. The module therefore checks EliteMobs' own WeaponIdentityResolver.isMagicWeapon FIRST — a magic weapon is never melee, whatever its material. Unit-tested explicitly ("staff (wooden spear, magic) is NOT melee").

### Off-hand / shield-like audit (39 files)
- SHIELD: 37 files — every EM shield in the tree is the vanilla SHIELD material (Primis artifex/old/final-quest shields, blood-temple/catacomb/raiders loot shields, event drops, goblin shields). No custom shield-like item on any other material exists today.
- TOTEM_OF_UNDYING: 2 files (berserker_charm, primis escaped-prisoner body) — the totem family.
- No other shield-like off-hand material exists in the content tree; both families are config lists (`shield-like-materials`, `totem-materials`) so a future custom defensive item joins by data.
- ExcelItems registry (4 builtins: veil/death potions, revive potion, one activation) contains NO weapons or off-hand items — nothing to classify there.

## Implementation (build-root/, branched from verified 0.6.0)
- NEW meleemitigation/MitigationMath.java (pure): percent sanitize [0,0.90] w/ per-piece fallback, total = min(base+bonus, cap), factor = 1 − total, mitigatedDamage (TW ShieldRule shape).
- NEW meleemitigation/MeleeWeaponClassifier.java (pure): material/suffix lists from config, exclusion wins, magic flag wins over everything.
- NEW meleemitigation/OffHandClassifier.java (pure): TOTEM vs SHIELD_LIKE vs NONE + split bonus.
- NEW meleemitigation/IndicatorText.java (pure): "% mitigation activated" line format (whole percents clean, 2.5 stays 2.5), indicator-line detection, lore reconcile (append/remove/swap; null = no change; original lore preserved exactly).
- NEW meleemitigation/MeleeMitigationModule.java (glue): HIGH-priority EntityDamageEvent victim scaling for players in EM-scope worlds (registry/list/prefix, pack-standard MeleeScope; apply-in-all-worlds switch; VOID/SUICIDE excluded by config); 10-tick indicator refresh task; clean strip of indicator lines on disable; EM-absent fallbacks logged.
- Registry +1 line; config sections in shipped + deploy config (deploy = shipped values, WRV runs defaults); README module section; version 0.6.0 → 0.7.0.
- Scope decision (documented in config + README): EM-scope worlds by default, following the pack's standard rule for its balance modules (melee-premium precedent); `apply-in-all-worlds: true` is the one-switch widening if Mike wants it everywhere.

## Tests
- Suites: 934 passed, 1 failed — the 1 failure is SkillNudgeMathTest "Jester-shaped: armor named first on the tie", PROVEN PRE-EXISTING: pristine unmodified 0.6.0 baseline tree run in this same pass = 888 passed, 1 failed (identical check; quest-beacon record notes it as Map.of iteration-order flaky). New MeleeMitigationTest: 46 checks, ALL PASS (math/defaults/cap, classifier families + staff trap + config add/exclude, off-hand split 2%/7%, indicator format + lore reconcile add/no-change/remove/swap).

## Build
- Deterministic: two consecutive builds byte-identical.
- ExcelEMFixes-0.7.0.jar — 389,940 B — SHA-256 a063e90942ebb689dfdfc81cd3b6974caf71926128c55d25552a888dac1780c1 (copies at lane root and build-root/).

## Rig / mirror load proof (house rig in build-root/rig, EM 10.9.10 + full live plugin set; jar + deploy config swapped in)
- Boot 1 (module ON): "Melee Mitigation ready - players holding a melee weapon take 15% less damage (off hand: shield-like +7%, totem +2%, off-hand bonus only with a melee weapon in the main hand)", "module enabled: Melee Mitigation (melee-mitigation)", pack ready line lists all 17 modules incl. melee-mitigation, /emfixes lists it, Done (48.473s), SEVERE 0, clean disable on stop. Log: rig-boot1-module-on.log (lane root).
- Boot 2 (melee-mitigation enabled:false): module absent from the 0.7.0 ready list (16 modules), Done (47.038s), SEVERE 0 — toggle discipline proven. Log: rig-boot2-module-off.log (lane root).
- D122 ship-it gates: loads clean on mirror/rig ✓ + automated tests pass (except the one proven pre-existing baseline flake, unrelated to this module) ✓. Desk/mirror only — NOT installed, NOT filed.

## Open notes for parent
- Filing (MANIFEST) and live install are later lanes per hard limits; surfaces (What's New etc.) ride that filing pass.
- If Mike intended mitigation in ALL worlds rather than EM-scope, flip `apply-in-all-worlds: true` in the deploy config at filing — no rebuild needed.
