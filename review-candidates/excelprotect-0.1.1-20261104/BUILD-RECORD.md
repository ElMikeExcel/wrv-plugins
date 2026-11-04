# ExcelProtect 0.1.1 — BUILD RECORD

**Built 2026-11-04 (America/Chicago). Status: BUILT + RIG-PROVEN, NOT INSTALLED — filed PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`; install is the failsafe's job under the standing gates.**

## Why this build exists

ExcelTogether 0.5.3's fleet coverage audit (`~/workspace/party-mesh/build-0.5.3/COVERAGE.md`) checked every Excel plugin against Mike's standing rule (Nov 3, 2026): every plugin ships console-runnable admin commands for support operations — "We should always have a way to help the players." ExcelProtect had exactly **one** gap: `/excelprotect inspect` toggles a click-to-inspect mode on the sender, so from the console it answered "players only." Every other Protect command (`export`, `rollback`, `restore-stand`, `deaths`, `import`, `status`) already routes for any sender.

Scope was kept tight: ONLY the inspect gap. No other Protect changes.

## What changed

- **New pure class `InspectReport`** (`src/com/winkreyes/excelprotect/InspectReport.java`): report assembly over ledger lines — `parseLimit` (default 10, clamp 1–25, non-numeric → 10), `build(scannedLines, limit)` (sorts by `ts_ms` descending, caps, reports the true total), and `summarize(line)` producing one compact line per event for `block_change` (incl. `sign_edit` first sign lines), `container_transaction` (deposits/withdrawals as item fingerprints — **never payloads**), `item_drop` / `item_pickup` / `item_craft`, `item_despawn` / `item_destroy`, `death_snapshot`, `item_destroyed`; unknown types are skipped.
- **`inspect player <name> [n]`** (new handler in `ExcelProtect.java`): async scan of the containers/items/deaths/blocks streams with `player=<name>` Exporter queries; prints the latest N events for that player. Empty → `no recorded activity for <name>`. Self-logs `inspect_player` to the admin stream.
- **`inspect at <world> <x> <y> <z> [n]`** (new handler): headed by the same one-line summary click-inspect shows for that block (`Blocks.inspect(loc)`, when the world is loaded; the in-memory index is session-scoped), then an async scan of blocks + containers at the exact position (`world=` + `at=x,y,z` + `radius=0`). Empty → `no recorded events at …`. Self-logs `inspect_at`.
- **Routing**: the two query forms are matched BEFORE the click-mode toggle, so any sender — console included — can use them. The command's existing permission behavior is unchanged (whole command gated by `excelprotect.admin`, default op). Bare `inspect` from a player keeps the exact 0.1.0 toggle behavior; bare `inspect` from the console now prints the query-form usage instead of "players only."
- Version strings 0.1.0 → 0.1.1 (javadoc, READY log, status, usage lines in all three places, `plugin.yml`). `local-build.sh`: jar name `ExcelProtect-0.1.1.jar`, pinned entry dates 2026-11-04.
- **Nothing else**: no recording, rollback, recovery, importer, API, config, or data-format changes. The 0.1.0 data folder and day files are read as-is; no new config keys.

## Candidate

- File: `~/workspace/excelprotect/build-0.1.1/ExcelProtect-0.1.1.jar`
- Size: **122,081 B** · SHA-256: **`b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b`**
- Built twice; the two builds are byte-identical (`ExcelProtect-0.1.1-repro.jar` alongside).
- Live jar it replaces (the packet's gate): `plugins/ExcelProtect-0.1.0.jar`, 115,549 B, SHA-256 `648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594` — verified three ways (build-0.1.0/SOURCE.md, build-0.1.0/INSTALL-PACKET.md, and the download-back hash in build-0.1.0/INSTALLED.txt).

## Verification summary (detail: TEST-RESULTS.txt)

- Unit suite: **16/16 pass** — the 11 tests from 0.1.0 unchanged, plus 5 new inspect tests (summary shapes incl. a seeded payload that must never print, sort/limit, limit parsing, and both query forms end-to-end over real ledger day files incl. empty cases).
- Rig: console-driven Paper 26.3 sandbox with seeded 2026-11-04 ledger day files. READY `version=0.1.1`; all eight console commands behaved as designed (both reports correct, both empty cases answered in words, bare console `inspect` answered the query usage — never a "players only" refusal); **SEVERE 0**. Transcript: `rig/evidence/boot-0.1.1.log`.

## Records in this folder

SOURCE.md · CHANGELOG-0.1.1.md · BUILD-RECORD.md (this file) · TEST-RESULTS.txt · INSTALL-PACKET.md · rig/ (driver + sandbox + evidence). Top-level `~/workspace/excelprotect/SOURCE.md` current-build block updated to 0.1.1 in the same pass.
