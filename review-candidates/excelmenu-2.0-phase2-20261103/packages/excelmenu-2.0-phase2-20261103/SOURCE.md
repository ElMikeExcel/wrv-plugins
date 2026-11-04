# ExcelMenu 2.0.0 — Phase 2 SOURCE.md

Artifact: `ExcelMenu-2.0.0.jar` (this folder)
SHA-256: `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2`
Size: 286,829 bytes. Two consecutive `./build.sh` runs
byte-identical (reproducible build; jar entry date pinned
2026-10-31T00:00:00Z).
Sources: `ExcelMenu-2.0.0-sources.zip`, SHA-256
`f16f3e32e05bc89e2d35805b563378939f3fb5b01c066d3b66610f9e6061eda1`.

## What Phase 2 is

The unification (DESIGN-2.0 §3.3): ExcelBedrockMenu retires and
everything it owned moves into this jar —

- `/menu`, `/servercommands` (alias `/cmds`), `/serverwebsite`,
  `/tpmenu`, `/childproofmenu` registered here. The /menu contract
  ExcelMenuJoin depends on (one player command, no args, opens the
  Server Menu) is preserved letter-for-letter; Join is untouched
  and still funnels through `performCommand("menu")`.
- The dynamic screens ported as a shared core + thin renderers:
  pure logic in `core.screens`, Bukkit transport beside the other
  renderers. Sources of record: ExcelBedrockMenu **1.5.28**
  (`~/workspace/excelbedrockmenu-1.5.28/src/`, jar SHA-256
  `b9a8abd4…`) for the main screens and the confirmation gate, and
  **1.5.29** (`~/workspace/excelbedrockmenu-1.5.29/src/`, jar
  SHA-256 `15c3ed77…`) for the teleport flow (direction choice,
  Mike's labels "Teleport to player" / "Ask player to come to me").
- The teleport-confirmation gate is a core service
  (`core.screens.TeleportConfirmationGate`, ported semantics
  unchanged); the four `teleport_confirm_*` definition files are
  retired — removed from the jar, named in the bundled config's
  `registry.retired-definitions`, and routed by MenuService to the
  gate before any definition lookup.
- PlatformBridge gained the Cumulus modal + input sends
  (sendModalForm / sendInputForm) in its own reflection idiom;
  FormService gained the screen-hook seam for the Ask text box,
  the Skills & Perks form, and the Maker's Mark owner form
  (Bedrock form taps only; Java is untouched).
- The scares-door repair (D11): the halloween_scares entry runs
  the owner's `/scares` command under a requires-plugin guard.

## The build base (baseline statement)

The Phase 2 build base is the **remediated phase-1 tree**, recorded
as the PHASE 2 BASELINE jar (`baseline/ExcelMenu-2.0.0-phase2-
baseline.jar`, SHA-256 `72c9ff0a…`, 225,617 B, suite 81/81). It
**supersedes the shipped Phase 1 jar `29b66799…` as the build
base**, because the tree carries live truth the shipped jar lacks:
the Nov 1 party-mesh wording in three entry files (placed live as
data files), the magiclevel entry (placed live Nov 1), and the
'me'-group layout fix (placed live Nov 1). **The live jar remains
`29b66799…` until the Phase 2 install; the live data folder
carries that drift.** Full remediation record: BUILD-RECORD.md.

## Tree layout

    phase-2/
    ├── ExcelMenu-2.0.0.jar          the artifact (hash above)
    ├── ExcelMenu-2.0.0-sources.zip  src + fixtures + tools + build.sh
    ├── baseline/                    the Phase 2 baseline jar (record)
    ├── build.sh                     one-command build + test (offline)
    ├── fixtures/                    conversion inputs of record:
    │     server_menu.yml              the live definition fixture
    │     bedrock-arrays.json          BUTTONS/COMMANDS machine-extracted
    │                                  from the 1.5.28 source (tools/
    │                                  extract_bedrock_arrays.py) +
    │                                  the Phase 0/1 catalogue record
    │     commands_menu.yml            preserved live catalogue p1
    │     commands_menu_page2.yml      preserved live catalogue p2
    ├── rig/                         Paper 26.3-235 + Geyser boot rig
    │     boot-console.txt             the verification boot (see TEST-REPORT)
    ├── src/main/java/com/winkreyes/excelmenu/
    │     core/…                       Phase 0 core (registry, render, model)
    │     core/screens/                Phase 2 screen logic (pure)
    │     bukkit/…                     renderers, bridge, services, and the
    │                                  Phase 2 Bukkit screens (flat, house style)
    └── src/main/resources/          plugin.yml, config.yml,
                                     entries/ (84), layouts/ (2), menus/ (12)

Ownership: all code in this tree is WRV-maintained (Excel
signature); the ported screens derive from ExcelBedrockMenu
1.5.28/1.5.29, also WRV's own plugins — provenance is noted in
each ported class's javadoc. No third-party code is bundled.

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
