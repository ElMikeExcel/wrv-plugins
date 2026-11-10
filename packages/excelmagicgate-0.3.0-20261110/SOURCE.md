# ExcelMagicGate 0.3.0 — Source Note

## What this is
ExcelMagicGate is the family's own Bukkit/Paper plugin that maps guild-seal
progress (read from ExcelGuildGate) onto the Magic plugin's five WRV stage
config packets. Version 0.3.0 adds the Magic Rank earning system of
`magic-rank-packet-20261110/DESIGN.md`: per-player ranks earned by credited
cast count (Student 100 / Apprentice 400 / Master 2,000), seals as the
ceiling, effect-gated credit, demotion to the earned target, and rank
progress in wand lore. With earning disabled it behaves exactly as 0.2.1.

## Provenance
- Author: the Reyes family's own tooling (Muse, for Mike Reyes /
  Wink Reyes Valley). No third-party plugin code is included or derived.
- Built from the family's LIVE ExcelMagicGate 0.2.1 source tree
  (`magicgate-template-refresh-20261109/ExcelMagicGate/`; 0.2.1 build
  `EXC-MAGICGATE-0.2.1-WRV-20261109-01-fb0d91`). Every 0.2.1 source file is
  carried; the five stage config packets under `stages/` are byte-identical
  to 0.2.1 (diff-verified).
- This build adds: `EarnedRank.java`, `CeilingPlan.java`, `CreditPolicy.java`,
  `EarnedStore.java`, `PlacementCalculator.java` (pure logic, no Bukkit),
  `MagicCreditHook.java`, `WandLoreHook.java` (Bukkit/Magic seams), a full
  rewrite of `MagicFloorHook.java` (step-up retained, demote added), the
  `earning:` section in `config.yml`, earned-rank wiring in
  `ExcelMagicGate.java`, and `RankFloor.stepsTo`. Tests: `ExcelMagicGateTest`
  (253 checks) + `PlacementTool` (a test-classpath utility that generates a
  placement earned.yml from mage.dat fixtures; not in the shipped jar).

## Build
- Toolchain: JDK 25 (`~/workspace/.jdk25`), hand-rolled `build.sh` exactly as
  0.2.1's (explicit javac file list, no build system), deterministic
  (fixed zip timestamps in `pack.py`): two clean builds are byte-identical.
- Compile classpath only: the family's own Spigot-API shim sources (under
  `magicgate-floor-build/shim/`, Aaron Holder / elMakers-style API surface
  re-implemented locally for compilation — API signatures only, no Magic or
  Bukkit code) and the ExcelGuildGate API jar. The plugin talks to Magic
  purely by reflection at runtime (no Magic code is compiled in or shaded).
- Build ID: `EXC-MAGICGATE-0.3.0-WRV-20261110-01-5af7d1` (`excel-build.txt`,
  embedded in the jar; the tag is per-build entropy per the fingerprint
  standard, everything else in the jar is a deterministic function of the
  sources in the records package — rebuild with `build.sh` from a JDK 25 +
  the same shim and compare against SHA256SUMS).

## License / distribution
Private family software for the Wink Reyes Valley server. Not published, not
sold, not redistributed. Records package is deposited in the family's own
Drive/GitHub records by the supervisor; this build itself performed no
publishing.
