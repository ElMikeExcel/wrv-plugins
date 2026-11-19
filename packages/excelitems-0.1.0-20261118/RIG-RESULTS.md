# RIG RESULTS - ExcelItems 0.1.0

Mirror rig: PASS (17 pass / 0 fail, SEVERE 0 on both boots and across the Boot A full lifecycle). Full rig report verbatim below (source: ~/workspace/excelitems-build-20261118/RIG-REPORT.md).

---

# ExcelItems 0.1.0 — MIRROR RIG REPORT

Run: Wed Nov 18, 2026, 17:54–17:59 CST — board supervisor run 555, worker W4.
Rig: Homebase mirror ONLY (`C:\Users\MikeExcel\wrv-testserver\server`, port
25566, RCON 25511) via `ssh -F ~/.ssh/config mike-pc`, following the
ExcelTDGuard rig convention of earlier today (WMI-detached boots, jar-census
snapshots, RCON console checks, restore-and-reboot). Live server, panel,
MANIFEST, backlog, state, and board were never touched.

## Candidate (verified before placement, re-verified at rig time)

- `ExcelItems-0.1.0.jar` — 20,214 B
  SHA-256 `85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d`
  (matches BUILD-REPORT.md; re-verified locally at rig time and again on the
  mirror after scp placement into `plugins/` — identical hash in both places).

## Found state (snapshot before any change)

Mirror was RUNNING (java pid 17140, started 17:20:09 — the TDGuard rig's
restored found-state boot): 0 players online (RCON `list`). Full pre-run
snapshot (jar census with SHA-256 per jar, dimensions, flags) saved on the
mirror at `stage-excelitems-r555\snapshot-pre.txt` (copy: `rig/snapshot-pre.txt`
beside this report) and the prior `latest.log` copied to
`boot-pre-excelitems-r555.log`. Found flags: ExcelItems jar ABSENT,
ExcelItems data folder ABSENT.

## Boots

### Boot A — WITH ExcelItems 0.1.0 — 17:56:16, pid 13076

- `Done (24.806s)!` (latest.log: `[17:56:41] [Server thread/INFO]: Done (24.806s)! For help, type "help"`)
- Loading line (line 122):
  `[17:56:27] [Server thread/INFO]: [ExcelItems] Loading server plugin ExcelItems v0.1.0`
- Enabling line (line 529) — count **exactly 1**:
  `[17:56:35] [Server thread/INFO]: [ExcelItems] Enabling ExcelItems v0.1.0`
- Enabled / registry line (line 530):
  `[17:56:35] [Server thread/INFO]: [ExcelItems] ExcelItems enabled: 4 custom item(s) registered, next free id 5.`
- **SEVERE count: 0** for the whole boot AND the whole lifecycle including
  the graceful shutdown (`[ExcelItems] Disabling ExcelItems v0.1.0` at
  line 749, 17:57:34; full-lifecycle SEVERE recount after stop: **0**).
- **ExcelItems WARN/ERROR/SEVERE/Exception lines: NONE.** The boot log
  contains exactly 5 ExcelItems-mentioning lines in total — the plugin-list
  line, Loading, Enabling, Enabled, Disabling — all INFO.
- Plugin list (boot log line 6) includes `ExcelItems (0.1.0)`.
- Data folder created on enable: `plugins/ExcelItems/config.yml` (863 B)
  carrying the shipped ID assignments verbatim:
  `1: veil_potion`, `2: deathprotection_basic`, `3: deathpotion`,
  `4: revive_potion` — the fail-closed assignment validation passed.

### Boot B — WITHOUT ExcelItems (baseline / control = restored found state) — 17:58:27, pid 31772 (left running)

- `Done (24.990s)!`
- Enabling ExcelItems count: **0**; ExcelItems mentions in the whole log:
  **0** (case-insensitive) — the candidate jar's presence is the only delta
  between Boot A and Boot B.
- **SEVERE count: 0** for the whole boot.
- No ExcelItems jar, no ExcelItems data folder; RCON `list`: 0 players;
  `excelitems status` via RCON returns `Unknown or incomplete command`
  (command absent with the plugin absent, as expected).

## Console smoke checks (RCON, Boot A) — the rig convention's hook

The TDGuard convention's console hook exists for this plugin:
`/excelitems status` and `/excelitems list` run from the server console
(permission `excelitems.admin`, default op). Run on Boot A:

Status output (verbatim):
`ExcelItems: 4 item(s) registered, next free id 5.`

List output (verbatim — the registry's 4 built-in items, IDs 1–4):

```
#1 veil_potion (ITEM, POTION)
#2 deathprotection_basic (ACTIVATION, CHEST)
#3 deathpotion (ITEM, POTION)
#4 revive_potion (ITEM, POTION)
```

NOT run (no console hook exists — stated plainly, not invented):
`give` and `identify` require an online player (`give` needs a target
player; `identify` reads the sender's held item and rejects non-players),
and the services API (`ExcelItemsAPI.create` / `identify`) has no console
surface in 0.1.0. Create + identify-by-ID round-trips are covered by the
build's plain-JVM harness (94 checks / 0 failures, BUILD-REPORT.md,
including the clean-room re-run); per the D122 ship-it rule the in-game
give+identify proof is post-release player testing, not a gate on this
load proof.

## Client-less rig checks — 17 pass / 0 fail

| # | Check | Result |
|---|---|---|
| 1 | Boot A reaches `Done (...)` | PASS (24.806s) |
| 2 | Boot A `Enabling ExcelItems v0.1.0` exactly once | PASS (1) |
| 3 | Boot A enabled line: 4 items registered, next free id 5 | PASS |
| 4 | Boot A SEVERE count = 0 (whole boot) | PASS (0) |
| 5 | Boot A SEVERE = 0 full lifecycle incl. graceful shutdown; Disabling line present | PASS (0; line 749) |
| 6 | Boot A no ExcelItems WARN/ERROR/SEVERE/Exception lines | PASS (NONE; 5 ExcelItems lines, all INFO) |
| 7 | Boot A plugin list includes `ExcelItems (0.1.0)` | PASS |
| 8 | Boot A jar hash in `plugins/` identical to candidate | PASS (`85dbb9ea…58685d`, 20,214 B) |
| 9 | Boot A data folder + `config.yml` created with the 4 ID assignments | PASS (863 B) |
| 10 | Boot A `/excelitems status` via console | PASS (`4 item(s) registered, next free id 5.`) |
| 11 | Boot A `/excelitems list` via console = IDs 1–4 as built | PASS |
| 12 | Boot B (control) reaches `Done (...)` | PASS (24.990s) |
| 13 | Boot B SEVERE count = 0 | PASS (0) |
| 14 | Boot B ExcelItems mentions = 0 (jar is the only delta) | PASS (0) |
| 15 | Boot B `/excelitems` command absent; 0 players online | PASS (Unknown command) |
| 16 | Post-run census diff vs pre-run snapshot identical | PASS |
| 17 | Mirror left running in its found state | PASS (Boot B, pid 31772, 0 players) |

## Incident during the run (rig mechanics, not the candidate)

`check.ps1`'s jar-census line threw one PowerShell
"cannot call a method on a null-valued expression" error because
`Get-ChildItem -Filter *ExcelItems*` also matched the `ExcelItems` data
folder (no Length/hash) — helper-side only, same class as the TDGuard
rig's RCON-helper crash; the jar's own line printed immediately after
with the correct size and hash, and no check depended on the failed
expression.

## Mirror restoration — CONFIRMED

- Stopped gracefully (RCON `stop`, 0 players) after Boot A; removed:
  `plugins/ExcelItems-0.1.0.jar` and `plugins/ExcelItems/` (the only two
  additions this rig made). Verified absent, with no ExcelItems residue
  under `plugins/`.
- Post-run census diff vs the pre-run snapshot: **jar census, flags,
  dimensions, and server-root listings identical** (only the snapshot
  timestamp and the java pid differ). Post snapshot: `rig/snapshot-post.txt`.
- Mirror left RUNNING in its found state (Boot B, pid 31772, 0 players),
  exactly as found. Rig scripts are in `rig/` beside this report and on
  the mirror under `stage-excelitems-r555\`; Boot A's full log is kept on
  the mirror at `stage-excelitems-r555\boot-A-with-excelitems.log`.

## Verdict

**LOAD PROOF PASS (D122 ship gate).** ExcelItems 0.1.0 loads clean on the
mirror: enabling line exactly once, registry loads its 4 built-in items
(IDs 1–4) with the enabled line and console `status`/`list` agreeing,
SEVERE 0 on both boots and across Boot A's full lifecycle, no ExcelItems
WARN/ERROR lines, and the no-jar control boot is equally clean — the
jar's presence is the only delta. Automated tests: 94/0 at build time
(BUILD-REPORT.md, incl. clean-room). Checks this rig: **17 pass / 0 fail**
(client-less subset).
