# ExcelBedrockMenu 1.5.28 — source record

Plugin: ExcelBedrockMenu  
Version: 1.5.28  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `excelbedrockmenu-1.5.28`

## What this is

Built Nov 1, 2026 (Iris, local work only — no live-server contact, nothing installed). Closes the gap Mike caught Nov 1, 2026: his Oct 31 requirement (`~/workspace/halloween-toggles/REQUIREMENTS-ADDENDUM.md`) put the /scares toggles in the Server Menu on BOTH platforms, moved up, retiring after Nov 5. The Java side landed (ExcelMenu 2.0 registry entry `halloween_scares`, Help group, label "Halloween Scares — your switches", CARVED_PUMPKIN icon, visible 2026-10-31 → 2026-11-05), but `/menu` and the compass still belong to ExcelBedrockMenu 1.5.27 (Menu 2.0 Phase 1 coexistence contract; the transfer is Phase 2), and 1.5.27's hardcoded button catalog had NO scares button — a Bedrock player opening the menu found nothing.

## Built artifact (not included in this record — text only)

- Jar: `ExcelBedrockMenu-1.5.28.jar` — 41839 bytes, SHA-256 `b9a8abd44fbb82b4a05923e362df6d8e0e9cc8b80a9fdf1d6dcbd47808cb1dc9`

## Install / live status

Installed live Nov 1, 2026; retired Nov 2, 2026 by the Menu 2.0 Phase 2 unification swap.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
