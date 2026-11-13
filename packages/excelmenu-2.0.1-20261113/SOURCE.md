# ExcelMenu 2.0.1 — SOURCE.md

Artifact: `ExcelMenu-2.0.1.jar` (this folder)
SHA-256: `c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a`
Size: 287,906 bytes. Two consecutive `./build.sh` runs
byte-identical (reproducible build; jar entry date pinned
2026-10-31T00:00:00Z).
Sources: `ExcelMenu-2.0.1-sources.zip`, SHA-256
`620f7af0b4ad2c12e9e3cc0224a85bd173711b167d0ea306faaf4b572324d824`.

## Base

**ExcelMenu 2.0.0, as frozen in the phase-2 sources zip**
(`~/workspace/menu-2.0/phase-2/ExcelMenu-2.0.0-sources.zip`,
SHA-256 `f16f3e32…`, hash-verified against the 2.0.0
BUILD-RECORD/SOURCE.md and re-proven by rebuild: byte-identical
`d1c6e771…` / 286,829 B, suite 127 green — see BUILD-RECORD.md
Part 1). NOT the phase-2 on-disk working tree, which drifted in
the Nov 7, 2026 org-decisions foldback and does not reproduce
the live jar (299,066 B / `22b12e11…`, 9 test failures when
rebuilt verbatim).

## What 2.0.1 changes (complete list)

- `core/render/FormModel.java` — sealed `Target` gains
  `PickerTarget`; `BACK_LABEL` defined as
  `BedrockDetail.BACK_LABEL` (single wording source); `group()`
  appends one Back button (PickerTarget) LAST, after the group's
  entry buttons. Picker/flat unchanged.
- `bukkit/FormService.java` — `answer()` answers `PickerTarget`
  by re-resolving fresh and re-opening the picker via the
  existing `open()`/`send()` path; no stats tap for Back.
- `src/main/resources/plugin.yml` — `version: 2.0.1` only.
- `build.sh` — `VERSION="2.0.1"` only.
- Tests: `core/render/FormModelTest.java` — counts 9→10 / 4→5
  with Back tail assertions + one new server_menu grouped case
  (picker carries NO Back). Suite 128 green.

Everything else — every bundled entry, layout, menu and the
config — is byte-identical to the 2.0.0 jar. No Java rendering
code changes. Ownership/provenance as in the 2.0.0 SOURCE.md:
all code WRV-maintained (Excel signature).

⚠ Item 3 (ExcelMenu 3.0) must branch from THIS 2.0.1 tree or it
regresses the Back-button fix (DESIGN §6).

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
