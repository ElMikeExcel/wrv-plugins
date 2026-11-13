# ExcelMenu 2.0.1 — TEST REPORT

Built Nov 13, 2026 (desk build; no live contact). Artifact under
test: `ExcelMenu-2.0.1.jar` SHA-256
`c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a`
(287,906 B), built twice byte-identical (`./build.sh`, offline,
JDK 25, jar date pinned 2026-10-31T00:00:00Z). Base proof:
BUILD-RECORD.md (2.0.0 reproduced byte-identical first).

## Unit + harness suite — OK (128 tests)

127 carried from the proven 2.0.0 base + 1 new. The design's
FormModelTest changes (DESIGN §3.3):

- `groupFormListsRowsAsBodyAndButtons`: buttons 9 → 10; the entry
  id assertions now run over the first 9 buttons; trailing button
  asserted label `"Back"`, label == `FormModel.BACK_LABEL`, target
  `PickerTarget`.
- `groupFormForJavaOnlyGroupRenders`: buttons 4 → 5, same tail
  assertion.
- NEW `groupedServerMenuGroupFormEndsWithBackAndPickerHasNone`:
  a grouped variant of the production `server_menu` layout is
  constructed (form=GROUPED + groups map — the bundled layout is
  FLAT; the grouped shape is what the live data folder carries
  since menu-grouped-20261112); the `me` group form's size equals
  its resolved entry count + 1, every earlier button is an
  EntryTarget, the last is Back → PickerTarget; the picker itself
  carries NO Back button (all GroupTargets, no "Back" label).

Golden harness: 5,526 rows, D1–D11 all occurring, zero
unclassified (unchanged — the harness models the resolver, not
FormModel.group; the flat form is untouched by construction).
Catalogue harness: C1–C8 all occurring.

## Jar diff vs the proven 2.0.0 base (scope proof)

Only: `META-INF/MANIFEST.MF` (Implementation-Version 2.0.1),
`plugin.yml` (version 2.0.1), the `FormModel*` classes (incl. the
new `FormModel$Target$PickerTarget.class`), and `FormService`
(+ its session inner class). `javap` bytecode for
`FormModel$Button` / `FormModel$FormPage` is identical to base —
their class-file byte diff is nest/permitted-subclasses metadata
only. **No bundled data file differs**: entries/, layouts/,
menus/, config.yml are byte-identical to the 2.0.0 jar, so no
live data file changes at install and no data `.bak` set is
needed; rollback is the prior jar.

## Rig — Paper 26.3-235 + Geyser, desk rig (phase-2 rig pattern)

Rig: `rig/` in this build folder (the phase-2 rig skeleton +
cache, seeded with the Nov 2 live-end-state data folder: 84
entries, 2 layouts, 16 menu files incl. the 4 confirm files;
Geyser-Spigot 2.12.1-b1277, PlaceholderAPI 2.12.3, NO
ExcelBedrockMenu). Boot log: `rig/boot-console.txt`. Verified:

- Plugin list: `ExcelMenu (2.0.1)`, Geyser-Spigot, PlaceholderAPI.
- `This server is running Paper version 26.3-235` — Done (38.048s).
- `ExcelMenu registry: 84 entries live` — diff +84 / -0 / ~0.
- `ExcelMenu self-check: loaded 12 menus` + the four
  teleport_confirm_* skip lines.
- Enable line: "ExcelMenu 2.0.1 enabled (Phase 2: unified …)
  84 entries, 2 registry menus, 12 definition menus live."
- `ExcelMenu platform bridge self-check: Geyser reflection
  surface PRESENT — Bedrock detection live`.
- Console proofs: `menu`, `servercommands`, `serverwebsite` →
  "Only players can use …"; `tpmenu`, `childproofmenu` → "Only
  players can use this."
- **SEVERE: 0.** The 4 ERROR lines are the standing sandbox-network
  noise (PlaceholderAPI hash-list download, Paper version fetcher
  ×3) — the same class the accepted 2.0.0 rig recorded.

## NOT claimed here — filing-time rig (DESIGN §3, owed at filing)

The design's behavioural rig steps (a)–(e) run on the PC
test-server mirror with the live-pulled companion jars at filing
time: Bedrock client picker → Me → Back → picker returns → a
second section works; same in one commands_menu group; entry taps
from a group form still dispatch (one screen-hook door, one plain
door); Java compass unchanged; SEVERE 0 with live counts. The
desk rig above proves boot/load only; the form-model behaviour
is proven by the unit suite. Player proof after install is
DESIGN §7 (Mike, Bedrock phone, ~1 minute).

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
