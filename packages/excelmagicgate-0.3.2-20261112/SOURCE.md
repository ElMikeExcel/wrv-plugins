# ExcelMagicGate 0.3.2 — Source Note

## What this is
ExcelMagicGate is the family's own Bukkit/Paper plugin that maps
guild-seal progress (read from ExcelGuildGate) onto the Magic
plugin's five WRV stage config packets, plus the 0.3.0 Magic Rank
earning system: per-player ranks earned by credited cast count
(Student 100 / Apprentice 400 / Master 2,000), seals as the
ceiling, effect-gated credit, demotion to the earned target, the
standing wand spell-inventory reconcile, and rank progress in wand
lore (0.3.1 added the milestone numbers to that lore). With
earning disabled it behaves exactly as 0.2.1.

Version 0.3.2 is the P0 "Magic broken" defensive fix (Nov 12,
2026; diagnosis §5 Fix 1): before the wand inventory sync calls
Magic's addSpell for a missing spell key, the key is resolved
against Magic's spell registry (the controller's getSpellTemplate,
reflectively). A key with no template — a wand data/config
mismatch — is skipped and logged BY NAME at WARNING instead of
being fed to addSpell, whose inventory rebuild
(Wand.buildInventory) dereferences the null template and NPEs.
Skipped keys are reported in the sync's completion accounting as
skipped, never as granted; every other key syncs exactly as
before (add-only, verified by re-read). The probe is fail-open:
if it cannot answer, grants proceed as they did before 0.3.2.
No rank logic, thresholds, reconcile, config, or data-format
changes. See BUILD-RECORD.md "Diff summary".

## Provenance
- Author: the Reyes family's own tooling (Muse, for Mike Reyes /
  Wink Reyes Valley). No third-party plugin code is included or derived.
- Built from the shipped ExcelMagicGate 0.3.1 build tree
  (`magicgate-031-build/`, build
  `EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311`, SHA256SUMS verified
  on disk before copying, and the base tree rebuilt byte-identical
  to the shipped 0.3.1 jar before any edit). The 0.3.2 delta on top
  of 0.3.1: the new pure `SpellTemplateGate` class, the gate in
  `MagicFloorHook.syncInventory` + the `addSpellQuietly` seam, the
  version/build-ID rotation (`BuildInfo.java`, `plugin.yml`,
  `excel-build.txt`, `build.sh` default output), and the gate
  tests in the suite. The five stage config packets under
  `stages/` and the shipped `config.yml` are byte-identical to
  0.3.1 (diff-verified).
- Tests: `ExcelMagicGateTest` (293 checks incl. the new
  `testSpellTemplateGate` dead-key checks and the fixture-level
  placement grant pass) + `PlacementTool` (a test-classpath
  utility that generates a placement earned.yml from mage.dat
  fixtures; not in the shipped jar).

## Build
- Toolchain: JDK 25 (`~/workspace/.jdk25`), hand-rolled `build.sh`
  exactly as carried (explicit javac file list, no build system),
  deterministic (fixed zip timestamps): two clean builds are
  byte-identical.
- Compile classpath only: the family's own Spigot-API shim sources
  and Paper-API/library jars used by the carried harness; the
  plugin talks to Magic purely by reflection at runtime (no Magic
  or Bukkit code is compiled in or shaded beyond the API surface).
- Build ID: `EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b`
  (`excel-build.txt`, embedded in the jar; the tag is per-build
  entropy per the fingerprint standard, everything else in the jar
  is a deterministic function of the sources in the records
  package — rebuild with `build.sh` from a JDK 25 + the same
  classpath and compare against SHA256SUMS).

## License / distribution
Private family software for the Wink Reyes Valley server. Not
published, not sold, not redistributed. Records package is
deposited in the family's own Drive/GitHub records by the
supervisor; this build itself performed no publishing.
