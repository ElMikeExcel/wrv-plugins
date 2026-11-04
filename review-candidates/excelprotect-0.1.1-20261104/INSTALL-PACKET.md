# ExcelProtect 0.1.1 — INSTALL PACKET (jar swap over shadow-live 0.1.0)

**Status: BUILT + RIG-PROVEN (TEST-RESULTS.txt), NOT INSTALLED.** Install only through the standing gates (verified-empty server or weekend protocol; panel Shut Down). This packet changes nothing about the 0.1.0 shadow posture: Protect keeps running alongside CoreProtect, ExcelDeathRecord, ExcelTelemetry and ExcelAuditLog; nothing is removed, disabled, or repointed.

## What this swap does

Closes the one console-admin gap from the ExcelTogether 0.5.3 coverage audit: 0.1.0's `/excelprotect inspect` only toggles a player click-to-inspect mode, so the console got "players only." 0.1.1 adds two query forms routed before the toggle, usable by any sender under the existing `excelprotect.admin` permission:

- `/excelprotect inspect player <name> [n]` — the player's latest recorded activity (containers/items/deaths/blocks), fingerprints only, never item payloads.
- `/excelprotect inspect at <world> <x> <y> <z> [n]` — recorded history at one block/container position.

Bare `/excelprotect inspect` keeps the exact 0.1.0 behavior for players; from the console it prints the query usage. Queries self-log to the admin stream (`inspect_player` / `inspect_at`). No recording, rollback, recovery, importer, API, config, or data-format changes; no new config keys — **do NOT replace the live `config.yml`**, and the live data folder is read as-is.

## Candidate

- File: `~/workspace/excelprotect/build-0.1.1/ExcelProtect-0.1.1.jar`
- Size: 122,081 B · SHA-256: `b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b` (built twice byte-identical)

## LIVE GATE — STOP if it does not match

The live jar `plugins/ExcelProtect-0.1.0.jar` MUST be **115,549 B** with SHA-256 **`648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594`** before anything is swapped. Verified three ways at build time: build-0.1.0/SOURCE.md, build-0.1.0/INSTALL-PACKET.md, and the download-back hash recorded in build-0.1.0/INSTALLED.txt. Hash the live jar fresh from a panel download; any mismatch = STOP and report.

## Steps

1. **BACKUP FIRST** (standing rule): fresh Shockbyte backup, verified completed before any swap. **Cap-at-6 house rule:** before creating it, prune the OLDEST manual backups until the post-creation total is ≤ 6; never delete the Daily Backup's newest copy or the two newest manual restore points; name any prunees in the install report.
2. Verified-empty gate (panel `list` 0/10 twice) or weekend protocol. Panel Shut Down.
3. Rename the live jar aside — never delete: `plugins/ExcelProtect-0.1.0.jar` → `plugins/ExcelProtect-0.1.0.jar.held-<date>-pre011`.
4. Upload the candidate as `plugins/ExcelProtect-0.1.1.jar`; verify size after upload (122,081 B). After the swap there is exactly ONE active ExcelProtect jar.
5. Start. Boot-verify (below). Write the marker `~/workspace/excelprotect/build-0.1.1/INSTALLED.txt` with window, backup identity, gate hash, placed hash, and boot lines.

## Boot verification (all must hold)

- `[ExcelProtect] READY version=0.1.1 … gp=on vault=on mcmmo=on retention-days=30` (live has GP + Vault economy + mcMMO — all three adapters should read on; any `off` is fail-soft-safe but must be reported).
- Combat line: "Magic CastEvent feed hooked" (Magic is live).
- Console `excelprotect inspect player <anyone>` answers a report or "no recorded activity for <name>" — **not** "players only." Console bare `excelprotect inspect` answers the query-form usage line.
- `excelprotect status` shows version=0.1.1, dropped-total=0.
- Guild LOCKED 3 of 5 unchanged; plugin count 77; TPS 20.0; no new SEVERE attributable to ExcelProtect.

## Player proof owed after install (one, quick)

One player runs bare `/excelprotect inspect` in game and confirms click-inspect mode still toggles exactly as on 0.1.0 (0.1.1 changed only the console/query routing; the toggle code path is untouched, but the proof is owed because the rig cannot click).

## Same-pass surfaces — checked, none owed

Console/admin-only surface; no player-facing text changes. Guide knowledge/intents, website, What's New (website + in-game), broadcaster tips, and in-game menus need nothing for this swap. The 0.1.1 record package joins the next records catch-up set.

## Rollback

Panel Shut Down → remove `plugins/ExcelProtect-0.1.1.jar` → rename `ExcelProtect-0.1.0.jar.held-<date>-pre011` back to `plugins/ExcelProtect-0.1.0.jar` → Start → repeat the 0.1.0 boot checks. The data folder needs no action: 0.1.1 writes no new file formats (its only new records are ordinary admin-stream lines, which 0.1.0 reads past harmlessly).

## Notes for the executor

- The 0.1.0 packet's shadow-soak proof list (field-diff day vs Telemetry, player proofs, importer run vs the real DeathRecord folders, consumer repoint, volume week) is unaffected by this swap and remains owed on its own schedule.
- Rig adapters read `off` because the sandbox has no GP/Vault/mcMMO — that is a sandbox artifact, not a build defect; live must read `on` per the boot checks above.
