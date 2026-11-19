# RIG RESULTS - ExcelDowned 1.0.1 (M1)

G-4 co-resident mirror boot: PASS on the Homebase mirror Nov 19, 2026, ~1:54-1:57 AM CST (D125 gate discharged), plus the VM-local Paper rig recorded in BUILD-RECORD.md. Full Homebase-mirror record verbatim below (source: ~/workspace/board-r571-m1-g4/G4-M1-RECORD.md).

---

# G4-M1-RECORD — ExcelItems migration G-4 mirror rig: M1 (ExcelDowned 1.0.1) — D125 GATE DISCHARGED

- **Lane:** background SSH lane on Homebase (parent dispatch, Nov 19, 2026, ~1:54–1:57 AM CST). SSH only, zero console input. Mirror ONLY (`C:\Users\mikeexcel\wrv-testserver\server`, game 25566 / RCON 25511); live server never touched.
- **Scope:** the D125 pre-install gate carried in the M1 filing (`~/workspace/board-r567/migration-filing/M1-PACKET.md`): Homebase-mirror co-resident boot of the M1 candidate + the live ExcelItems 0.1.0 jar — enabling ×1 each, Downed AFTER ExcelItems, SEVERE 0, RCON `excelitems status` = 4 registered / next free 5. Same per-piece discipline as the M2/M3/M4 rigs (`~/workspace/board-r560/G4-RECORD.md`).
- **Boot procedure used (VM-side documented, r555/G-4 convention):** WMI `Win32_Process.Create` → `cmd.exe /c C:\Users\mikeexcel\wrv-testserver\start-testserver.cmd`, wait for `Done (` in `logs\latest.log`; stops via RCON (`list`/`stop`) through the established `stage-r512\rcon-mirror.py` helper (invoked as a black box). Rig scripts for this run: `g4-rig/` beside this record (VM) and `C:\Users\mikeexcel\wrv-testserver\stage-g4-rig\` (PC; `m1-swap.ps1` / `m1-check.ps1` / `m1-restore.ps1` are the M1 variants of the G-4 scripts).

## Verdict

| Piece | Candidate | Verdict |
|---|---|---|
| M1 — ExcelDowned 1.0.1 | 140,323 B / `9e8ae0f5…` | **G-4 PASS — D125 gate DISCHARGED** |

Co-resident jar: `ExcelItems-0.1.0.jar` 20,214 B / `85dbb9ea…` — the live build of record (M1 packet receipt R3). Candidate hash re-verified on the VM before transfer and on the mirror after placement — EXACT both times.

## Idle-queue coordination (claim state observed)

- Before touching the mirror: queue `running\` EMPTY; `pending\` = `iq-20261119-001` (calibration re-run) + `iq-20261119-002` (Arrow proof). Runner log showed iq-20261119-001 had claimed at 00:36:36 (idle 1801 s) and ABORTED-HUMAN-PRESENT at 01:16:38 (physical input), re-parked with required_idle reset to 30 min; no claim in flight at rig start.
- To remove the mid-rig claim race, the queue's own STOP flag was set for the rig's duration: runner logged `2026-11-19 01:54:22 | STOP-FLAG | claiming paused by STOP flag`. STOP removed at rig end (verified absent); both jobs remain in `pending\` untouched, `running\` empty. No job claimed, ran, or failed against this rig.

## Found state (before any change)

Mirror was BOOTED in found state (java pid 14672, started Nov 18 22:34:40 — the G-4 run's final boot). Pre-run jar census (84 jars, SHA-256 each — `g4-rig/snapshot-m1pre.txt`) is IDENTICAL to the board-r560 G-4 pre-run census; resident set includes `ExcelDowned-0.1.4.jar` 59,998 B / `703d7f9a…`, NO ExcelItems jar/folder, no `.bak` files. RCON `list` = 0 players.

## M1 — ExcelDowned 1.0.1 (boot 01:55:09, java pid 22360)

- Swap: resident `ExcelDowned-0.1.4.jar` → `.bak-g4`; candidate + ExcelItems placed (hashes above, verified post-placement on the mirror).
- `Done (22.965s)`; SEVERE **0**.
- Line 429: `[ExcelItems] Enabling ExcelItems v0.1.0` ×1; line 430: `ExcelItems enabled: 4 custom item(s) registered, next free id 5.` ×1.
- Line 541: `[ExcelDowned] Enabling ExcelDowned v1.0.1` ×1 — **AFTER ExcelItems** (541 > 429; hard-depend order proven on the mirror).
- Line 542 (registry line, present as owed): `ExcelDowned: registered revive behavior for ExcelItems ID 4 (exceldowned:revive-potion).`
- Line 544: `ExcelDowned 1.0.1 ready — build EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d; sessions restored: 8; bleed-out 300s; downed factor 0.25`.
- `purchase journal unavailable` count: **0**.
- WARN/ERROR lines mentioning ExcelItems/ExcelDowned: **NONE**.
- RCON `excelitems status` → `ExcelItems: 4 item(s) registered, next free id 5.`; `excelitems list` → IDs 1–4 (veil_potion / deathprotection_basic ACTIVATION / deathpotion / **revive_potion**). This is the one check the VM-local rig could not cover; it passes on the mirror.
- Evidence: full boot log pulled to `g4-rig/m1-boot-log.gz` (SHA-256 `ebc5764d…`) and independently re-grepped on the VM — all counts and line numbers agree.
- Restore: resident 0.1.4 back at `703d7f9a…` EXACT, candidate + ExcelItems jar + ExcelItems data folder removed, NO RESIDUE.

## Final state — mirror LEFT BOOTED in found state

- Final found-state boot 01:56:14, **java pid 12128, left RUNNING**: `Done (22.743s)`, SEVERE 0, residents enabling ×1 each (Downed v0.1.4, DeathProtection v0.1.3, MobVeil v0.1.0, Shops v1.3.9), ExcelItems mentions 0, RCON `list` = 0 players — the mirror answers RCON in its found state, so idle-queue jobs `iq-20261119-001` / `iq-20261119-002` can claim once their deep-idle gate passes.
- Post-run census (`g4-rig/snapshot-m1post.txt`) vs pre-run census: **84 jars, diff IDENTICAL** (name + size + SHA-256 every jar); flags: ExcelItems jar/folder absent, no candidate jar, no `.bak-g4` files.

**G-4 is now discharged for M1.** With M2/M3/M4 already installed (Nov 18, 11:36 PM), the D125 pre-install gate on the M1 MANIFEST entry is MET — the failsafe may install ExcelDowned 1.0.1 at the first empty window, and ExcelDowned 1.0.2 chains behind it per its sequencing gate. M5 remains last per the scope order.
