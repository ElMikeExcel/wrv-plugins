# ExcelMagicGate 0.3.1 — Source Note

## What this is
ExcelMagicGate is the family's own Bukkit/Paper plugin that maps
guild-seal progress (read from ExcelGuildGate) onto the Magic
plugin's five WRV stage config packets, plus the 0.3.0 Magic Rank
earning system: per-player ranks earned by credited cast count
(Student 100 / Apprentice 400 / Master 2,000), seals as the
ceiling, effect-gated credit, demotion to the earned target, the
standing wand spell-inventory reconcile (amended -02), and rank
progress in wand lore. With earning disabled it behaves exactly
as 0.2.1.

Version 0.3.1 is a LORE-ONLY fix (Mike's ruling, Nov 10, 2026):
the wand lore additionally lists the milestone numbers themselves
— "Milestones: Student 100 · Apprentice 400" / "· Master 2,000" —
alongside the existing credited count and next-rank gap, which
the live 0.3.0 lore showed without the milestone numbers. The
milestone numbers are read from the same EarnedRank thresholds
the earning logic uses, so the wand can never drift from the
ranks it earns. No rank logic, thresholds, reconcile, config, or
data-format changes. See BUILD-RECORD.md "Diff summary".

## Provenance
- Author: the Reyes family's own tooling (Muse, for Mike Reyes /
  Wink Reyes Valley). No third-party plugin code is included or derived.
- Built from the verified amended ExcelMagicGate 0.3.0 build tree
  (`magicgate-rank-build/build-0.3.0-amended/`, build
  `EXC-MAGICGATE-0.3.0-WRV-20261110-02-d2a310`, SHA256SUMS verified
  on disk before copying), itself built from the unamended 0.3.0
  build from the family's LIVE 0.2.1 source tree. The 0.3.1 delta
  on top of amended 0.3.0: `WandLoreHook.java` (milestone lines +
  pure block form), the version/build-ID rotation
  (`BuildInfo.java`, `plugin.yml`, `excel-build.txt`, `build.sh`
  default output), and the lore test in the suite. The five stage
  config packets under `stages/` and the shipped `config.yml` are
  byte-identical to the amended build (diff-verified).
- Tests: `ExcelMagicGateTest` (284 checks incl. the new
  `testWandLore` milestone checks and the fixture-level placement
  grant pass) + `PlacementTool` (a test-classpath utility that
  generates a placement earned.yml from mage.dat fixtures; not in
  the shipped jar).

## Build
- Toolchain: JDK 25 (`~/workspace/.jdk25`), hand-rolled `build.sh`
  exactly as carried (explicit javac file list, no build system),
  deterministic (fixed zip timestamps): two clean builds are
  byte-identical.
- Compile classpath only: the family's own Spigot-API shim sources
  and Paper-API/library jars used by the carried harness; the
  plugin talks to Magic purely by reflection at runtime (no Magic
  or Bukkit code is compiled in or shaded beyond the API surface).
- Build ID: `EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311` (`excel-build.txt`,
  embedded in the jar; the tag is per-build entropy per the
  fingerprint standard, everything else in the jar is a
  deterministic function of the sources in the records package —
  rebuild with `build.sh` from a JDK 25 + the same classpath and
  compare against SHA256SUMS).

## License / distribution
Private family software for the Wink Reyes Valley server. Not
published, not sold, not redistributed. Records package is
deposited in the family's own Drive/GitHub records by the
supervisor; this build itself performed no publishing.
