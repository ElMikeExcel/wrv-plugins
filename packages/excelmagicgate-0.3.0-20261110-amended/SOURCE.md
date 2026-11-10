# ExcelMagicGate 0.3.0 — Source Note (AMENDED build -02)

## What this is
ExcelMagicGate is the family's own Bukkit/Paper plugin that maps guild-seal
progress (read from ExcelGuildGate) onto the Magic plugin's five WRV stage
config packets. Version 0.3.0 adds the Magic Rank earning system of
`magic-rank-packet-20261110/DESIGN.md`: per-player ranks earned by credited
cast count (Student 100 / Apprentice 400 / Master 2,000), seals as the
ceiling, effect-gated credit, demotion to the earned target, and rank
progress in wand lore. With earning disabled it behaves exactly as 0.2.1.

This AMENDED build (-02) adds the standing wand spell-inventory
reconcile (Mike's Nov 10, 2026 build requirement; board-supervisor
dispatch, decide-and-log D2): on every reconcile leg that reaches a
live wand, any spell in the player's current-rank path lists missing
from their wand is granted — path spells from Magic's live path
object plus config-carried supplemental grants for the Nov 8
summoning tree (archer student / bruiser apprentice / guardian,
revenant, ghast, mount master), which sits in no path list. Grants
write through Magic's CasterProperties API and are verified by
re-read. See BUILD-RECORD.md "Amendment delta".

## Provenance
- Author: the Reyes family's own tooling (Muse, for Mike Reyes /
  Wink Reyes Valley). No third-party plugin code is included or derived.
- Built from the verified unamended ExcelMagicGate 0.3.0 build tree
  (`magicgate-rank-build/build-0.3.0/`, build
  `EXC-MAGICGATE-0.3.0-WRV-20261110-01-5af7d1`), itself built from the
  family's LIVE 0.2.1 source tree. The amendment's delta on top of
  0.3.0: `WandInventorySync.java` (new, pure logic),
  `MagicFloorHook.syncInventory` (new grant seam in the carried
  hook), the `earning.inventory-reconcile` section in `config.yml`,
  accessors + status line in `ExcelMagicGate.java`, the plugin.yml
  description sentence, and the build ID rotation. The five stage
  config packets under `stages/` are byte-identical to 0.2.1
  (diff-verified against the unamended build).
- Tests: `ExcelMagicGateTest` (276 checks incl. the fixture-level
  placement grant pass) + `PlacementTool` (a test-classpath utility
  that generates a placement earned.yml from mage.dat fixtures; not
  in the shipped jar).

## Build
- Toolchain: JDK 25 (`~/workspace/.jdk25`), hand-rolled `build.sh` exactly as
  0.2.1's (explicit javac file list, no build system), deterministic
  (fixed zip timestamps in `pack.py`): two clean builds are byte-identical.
- Compile classpath only: the family's own Spigot-API shim sources (under
  `magicgate-floor-build/shim/`, Aaron Holder / elMakers-style API surface
  re-implemented locally for compilation — API signatures only, no Magic or
  Bukkit code) and the ExcelGuildGate API jar. The plugin talks to Magic
  purely by reflection at runtime (no Magic code is compiled in or shaded).
- Build ID: `EXC-MAGICGATE-0.3.0-WRV-20261110-02-d2a310` (`excel-build.txt`,
  embedded in the jar; the tag is per-build entropy per the fingerprint
  standard, everything else in the jar is a deterministic function of the
  sources in the records package — rebuild with `build.sh` from a JDK 25 +
  the same shim and compare against SHA256SUMS).

## License / distribution
Private family software for the Wink Reyes Valley server. Not published, not
sold, not redistributed. Records package is deposited in the family's own
Drive/GitHub records by the supervisor; this build itself performed no
publishing.
