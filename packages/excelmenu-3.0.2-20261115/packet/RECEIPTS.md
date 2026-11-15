# BB-01 RECEIPTS — ExcelMenu Back fixes (board-manager run 354)

All timestamps America/Chicago (CST, UTC-6) unless marked UTC.

## R1. G1 live-premise pull — BLOCKED (Homebase route down; panel fallback needs a live-browser leg)

Homebase SSH probes (host mike-pc via the Tailscale proxy, the lane's
standard route), retry pattern per the probe rule:

| # | Time (CST) | Result |
|---|---|---|
| 1 | 2026-11-14 16:53 | FAIL — `kex_exchange_identification: read: Connection reset by peer` / `Connection reset by 198.19.0.1 port 3128` |
| 2 | 2026-11-14 16:54 | FAIL — same reset |
| 3 | 2026-11-14 16:55 | FAIL — same reset |
| 4 | 2026-11-14 17:13 | FAIL — same reset |

Reading per the Nov 11 alert-clarity rule: my connection path to
Homebase failed its probes; the PC's own state is unknowable while
they fail. This is a relay-path failure, not evidence the PC is down.

Consequence: NO same-day live pull of `plugins/ExcelMenu*.jar` or of
the live `plugins/ExcelMenu/layouts/commands_menu.yml` exists for
this unit. The FALLBACK (panel) receipt requires the Shockbyte panel
File Manager in a live browser, which a desk worker cannot operate;
it is owed by the supervisor/failsafe lane before this packet may be
filed or installed. The install provenance gate must re-pull fresh
in any case (a baseline hash never satisfies it, G4).

Same-day baseline check: `~/workspace/nightly-pull-20261114/
LIVE-BASELINE-HASHES.txt` records that the Nov 14 nightly pull did
NOT run; the newest baseline (2026-11-12) expired 2026-11-14 03:22
CST. No usable baseline exists. Nothing in this packet cites a live
hash.

## R2. Audited-tree jar (disk lineage — NOT a live premise)

- Path: `~/workspace/menu-colors-20261113/excelmenu-301-build/ExcelMenu-3.0.1.jar`
- Size: 330214 B
- SHA-256: `5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939`
- Role: the audit's source-of-record build (AUDIT.md line 17). The
  3.0.2 build was made from a byte copy of this tree. Whether the
  LIVE jar equals these bytes is exactly what R1's owed pull must
  confirm; if live has moved past 3.0.1, the unit re-bases before
  filing (FIXBATCH-SPLIT-R348 §3/§7 stop condition).

## R3. Candidate build

- Artifact: `~/workspace/back-button-wave1-r354/BB-01/ExcelMenu-3.0.2.jar`
  (also at `BB-01/build-tree/ExcelMenu-3.0.2.jar`, identical bytes)
- Size: 337069 B
- SHA-256: `0dacc1364851f716aaefd67aa138b259f2eca34ca6cf51e3aeb14228c72e7359`
- Build: `BB-01/build-tree/build.sh` (offline, pinned deps), run
  2026-11-14 ~17:08 CST. Compile: 1 pre-existing deprecation warning
  (ActionDispatcher Sound.valueOf, present in 3.0.1 too).
- Tests: `OK (174 tests)` — baseline 3.0.1 was `OK (169 tests)`
  (build-301-first/second.log); +5 new test methods, 0 failures.
  New/changed coverage: FormModelTest.pickerWithServerMenuBackEndsWithBack,
  ChestModelTest.commandsMenuPagesCarryEveryRow (slot-49 Back +
  count 39->40), SkillsPerksCoreTest.backButtonsAppendedLast,
  ChildproofCoreTest.backNavigationLabels,
  TeleportCoreTest.backNavigationLabels,
  TeleportConfirmationGateTest.isPendingTracksTheTicketLifecycle,
  ConfirmTargetsTest.settledButtonLabels (CANCEL_LORE pin).
- Script: `./build.sh` output tail recorded in BB-01-RESULT.md.

## R4. Staged live-layout artifact (install places this file)

- Path: `BB-01/packet/staged-layouts/commands_menu.yml`
- Size: 3707 B
- SHA-256: `0005e5dcd6e40bfa8c6b6c78c920db1dd1a916c6ca75651452090841175466a9`
- Why staged: jar extraction is fill-missing only, so the live
  `plugins/ExcelMenu/layouts/commands_menu.yml` never updates from
  the jar. The Java Server Commands Back item (slot 49) ships ONLY
  in this file. Install replaces the live file (live kept as .bak).
- Gate hash for the live file: OWED with R1's pull.

## R5. Local boot rig (Java-side verification; disposable local Paper)

Rig: local Paper 26.3 (rig-boot paper.jar + cache) in BB-01/rig
(removed after evidence capture; logs kept in BB-01/rig-evidence/).
Plugins: ExcelMenu-3.0.2.jar + PlaceholderAPI-2.12.3.jar only.

- `[ExcelMenu] ExcelMenu layouts: 2 live: commands_menu, server_menu`
- `[ExcelMenu] ExcelMenu self-check: loaded 12 menus: basics_menu,
  makersmark_settings, skills_perks, spawnshop, spawnshop_building,
  spawnshop_cosmetics_colors, spawnshop_cosmetics_sets,
  spawnshop_enchanting, spawnshop_food,
  spawnshop_potions_protection, spawnshop_safe_blocks,
  spawnshop_travel_animals`
- `[ExcelMenu] ExcelMenu 3.0.2 enabled ... 83 entries, 2 registry
  menus, 12 definition menus live.`
- `Done (92.648s)!`
- SEVERE count in rig log: 0 (`grep -c SEVERE` = 0).
- Extracted `plugins/ExcelMenu/layouts/commands_menu.yml` in the rig
  contains slot 49 `Back to Server Menu` (grep evidence, R4 file).
- Rig log: `BB-01/rig-evidence/rig-latest.log` — 35193 B /
  SHA-256 `636f36fee4612bde4a3dd88ad25e463794640507170f2ef8e62c94b58897bcdf`

## R6. Mirror walk — OWED (blocked by the same route failure as R1)

The PC test-server mirror lives on Homebase; the end-to-end walk of
every touched flow (Java + Bedrock, Back out level by level) could
not run while the route is down. The render-shape half of the walk
IS done: the unit tests exercise the production layouts/entries
through ChestModel and FormModel (the renderers' brains) for every
registry screen in scope, and the core label/index pins cover every
bespoke form's button list. The Bukkit click/form handlers and the
mirror walk remain proof-owed before install, and Mike's spot taps
remain the batch acceptance end after install (§2/§7 of the split).

## G1 RECEIPT (supervisor G1 dispatch, Nov 14)

Re-dispatch context: board-manager run 354, combined BB-01 + BB-04
G1 pull. Supervisor reported a Homebase probe PASS at ~17:21 CST
Nov 14, 2026. This worker probed from 17:23:32 CST.

- Route attempted: Homebase (SSH alias mike-pc, HostName
  100.72.156.1, ProxyCommand ts-connect.py via the runtime
  Tailscale relay; on-PC rclone remote shockbyte:, root
  "1. Michael's Server").
- Probe outcome: FAIL — relay down for the whole window.
  33 probes, 2026-11-14 17:23:32–17:33:55 CST, all exit 255:
  32x "kex_exchange_identification: read: Connection reset by
  peer / Connection reset by 198.19.0.1 port 3128", 1x
  "Connection closed by 198.19.0.1 port 3128" (17:28:24 CST).
  The 3-probe/~2-min pattern was run and exceeded many times
  over. Full probe log: ../G1-PULLS-RESULT.md.
- Live ExcelMenu jar — live path `plugins/ExcelMenu-*.jar`:
  exact filename NOT OBTAINED; size NOT OBTAINED; SHA-256 NOT
  OBTAINED; pull timestamp NONE (no connection, no bytes).
- **Live version verdict: CANNOT BE DETERMINED. Whether live
  ExcelMenu is still 3.0.1 is UNCONFIRMED by this run** — if it
  has moved past 3.0.1, BB-01 needs re-basing; that question
  stays open and must not be filed on lineage (R2's audited-tree
  jar 330,214 B / 5bb1c1a3… is disk lineage, not a live premise).
- Live layout — live path
  `plugins/ExcelMenu/layouts/commands_menu.yml`: size NOT
  OBTAINED; SHA-256 NOT OBTAINED; pull timestamp NONE. The
  fill-missing premise for the staged layout (R4: 3,707 B /
  0005e5dc…) remains UNCONFIRMED; its gate hash is still owed.
- Retained pulls: NONE —
  ~/workspace/back-button-wave1-r354/g1-pulls/excelmenu-jar/ and
  .../excelmenu-commands-menu/ are empty. PC-side temp files:
  none created (never connected); nothing to clean up.
- Per the Nov 11 alert-clarity rule: relay-path evidence only;
  the PC's own state is unknowable while the probes fail.
- Gate state: BB-01 G1 remains OPEN / NOT SATISFIED.

## G1 RECEIPT — FALLBACK (panel) downloads, supervisor leg, Nov 14, 2026, 5:35–5:37 PM CST

Homebase relay down/flapping all evening; byte-exact evidence taken via labeled panel downloads (see BB-01/packet/FILING-AMENDMENT.md for full receipts + the commands_menu install-step amendment). Live ExcelMenu = 3.0.1 in-jar (jar filename ExcelMenu-2.0.0.jar), 330,214 B / 5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939. Live commands_menu.yml 4,956 B / 146db1d91c4d34c49d7f43c1fd55f2eb80a6979215365b29442848ea47403bcb. BB-01 G1 gate is CLOSED on this receipt; no re-base.
