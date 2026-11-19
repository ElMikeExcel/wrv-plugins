# INSTALL PACKET — ExcelItems 0.1.0 (new plugin)

- Filed by: board supervisor run 555, worker W5 (filing-prep).
- Date prepared: Wed Nov 18, 2026.
- Lint: 6880 B / max line 76 / non-ASCII 9
- Packet folder: ~/workspace/excelitems-build-20261118/filing/
- Receipts: ~/workspace/excelitems-build-20261118/filing/RECEIPTS.md
  — SHA-256 7061807528ef39776022dcdba42e776780f110f0f54e2834dc120fe97491d637
  (referenced, not inlined).

## Candidate

- Artifact: ~/workspace/excelitems-build-20261118/ExcelItems-0.1.0.jar
  20,214 B / SHA-256
  85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d
- Build: ExcelItems-0.1.0, deterministic (double-build +
  clean-room rebuild from the source zip byte-identical).
  Unit tests 94/94. See ../BUILD-REPORT.md.
- Rig: mirror load proof PASS, 17 pass / 0 fail, SEVERE 0 on
  both boots and across the with-plugin boot's full lifecycle;
  enabled line and console status/list verified; mirror
  restored to its found state. See ../RIG-REPORT.md.
- Source zip: ../ExcelItems-0.1.0-source.zip, 22,709 B,
  SHA-256 0c5f20b9e2a1cd8b6894296df5880bd977ebbccb36bd040c9d6dd6042d027001

## Live-premise receipts (G1) — summary, full text in RECEIPTS.md

- ExcelItems ABSENT live: full rclone lsf of live plugins/,
  537 entries, EXCELITEMS_MATCHES=0, case-sensitive and
  case-insensitive alike (RECEIPTS.md R1, pulled
  2026-11-18 18:02-18:03 CST via Homebase/rclone, during the
  in-flight TDGuard install window; read-only listing only).
- Census context: 537 total entries / 91 plain .jar live
  (RECEIPTS.md R2). The +2/+1 delta vs the TDGuard filing
  (535/90, earlier today) is exactly that in-flight install.

## Live-jar gate

- There is NO live ExcelItems jar (R1). STOP condition instead:
  if a file named ExcelItems*.jar or a plugins/ExcelItems/
  folder exists live at the install gate, STOP — the
  new-plugin premise is void; re-base before proceeding.
- No softdepend or depend premise: ExcelItems IS the
  dependency plugin and declares no hard depend itself; no
  live plugin depends on it yet.

## Scope

- ONE gated unit: place ONE new jar,
  plugins/ExcelItems-0.1.0.jar (bytes per Candidate above).
- No config replacement: the plugin creates
  plugins/ExcelItems/config.yml itself on first enable,
  carrying the permanent ID assignments 1-4
  (1: veil_potion, 2: deathprotection_basic, 3: deathpotion,
  4: revive_potion). No live file is edited, swapped, or
  deleted by this packet.
- Does NOT change: any other plugin's jar, config, or data;
  world data; or player data. Nothing depends on ExcelItems
  yet — migrating ExcelShops / ExcelDowned onto it (their
  depend lines and mint re-routing) is a later unit.
- Behaviour once live: the registry loads its 4 built-in item
  definitions and answers the op-only /excelitems admin
  command. There is no player-facing surface in 0.1.0.

## Gates (failsafe, standing discipline)

- Marker discipline per D67 v2, with marker re-checks
  immediately before Shut Down and again before Start.
- BACKUP FIRST + cap-at-6: take the pre-install backup before
  the swap; if pruning is needed, name the prunees in the
  close-out; never prune the Daily Backup's newest copy or
  the two newest manual restore points.
- Emptiness gate: console `list` = 0 of 10, twice, before the
  shutdown/swap. Players on = countdown path per standing
  rule, never a silent swap.
- Provenance gate at swap: the placed jar's SHA-256 must
  equal the Candidate hash above; mismatch = STOP, do not
  start.

## Install steps (script-shaped)

1. Backup per Gates; record backup name.
2. Upload ExcelItems-0.1.0.jar to plugins/ (panel or Homebase
   rclone — installer's lane choice); verify placed size
   20,214 B and SHA-256 equals the Candidate hash (sha256sum /
   Get-FileHash output is the evidence).
3. Shut Down (after the second marker re-check), Start.
4. Boot verify (below). On failure: rollback (below).

## Boot verify (by script output)

- Log grep: `Enabling ExcelItems v0.1.0` count = exactly 1.
- Log grep: `ExcelItems enabled: 4 custom item(s) registered,
  next free id 5` count = exactly 1 (verbatim shape proven on
  the mirror, RIG-REPORT.md).
- SEVERE count attributable to the boot = 0 (grep count).
- Console/RCON `/excelitems status` returns the status line
  `ExcelItems: 4 item(s) registered, next free id 5.`
  (verbatim shape proven on the mirror, RIG-REPORT.md).
- Boot reaches `Done (...)`; plugins census after boot =
  539 entries / 92 plain .jar in plugins/ (R2 baseline
  537/91 + 1 jar + the plugins/ExcelItems/ folder the plugin
  creates on first enable, its config.yml carrying the
  assignments 1-4 above).
- Marker: write ~/workspace/excelitems-build-20261118/INSTALLED.txt
  with window, lane, backup name, gate results, and the
  verify counts above.

## Rollback

- Remove plugins/ExcelItems-0.1.0.jar and the plugin-created
  folder plugins/ExcelItems/ (its config.yml is its own data;
  nothing else was touched and nothing else depends on it),
  restart, confirm the enabling line is gone and SEVERE = 0.
  The pre-install backup is the second net if anything else
  looks wrong. Deleting jar + data folder is a full rollback.

## Same-pass surfaces (owed AT INSTALL)

- What's New, website section: one entry — a custom-item
  registry (ExcelItems) now runs under the hood, the shared
  foundation future shop items will be built on; players will
  see it in the /plugins list.
- What's New, in-game: same entry in
  plugins/ExcelWhatsNew/changelog.yml, verified from the live
  file after placement (never assumed from the website pass).
- Website pair (winkreyesvalley): the matching page/section
  update lands in the same pass as the website What's New.
- Guide: HELD under the Guide-development hibernation
  (Mike, Nov 16, 2026) — no knowledge/intent changes; named
  here as the explicit exception, not an omission.
- Tips check + menus check: confirm no broadcaster tip or
  menu text contradicts the install; /excelitems is op-only
  and no player menu changes, so no change expected.

## Proof owed AFTER install (post-install, NOT gates — D122)

- NONE player-facing yet, stated plainly: the registry has
  no player surface until a product migrates onto it (the
  ExcelShops / ExcelDowned migration is a later unit), so
  there is no player proof to owe for this install. The
  in-game give + identify proof lands with that migration
  unit. Ship gate for THIS unit per D122 is already met:
  mirror load clean + automated tests pass; a clean live
  load in the install window ships it.

## Close-out owed (installer/failsafe)

- MANIFEST conversion: this entry becomes a one-line
  tombstone; full record to MANIFEST-ARCHIVE.md.
- Publication claim lines (G2) for any records/website
  pushes made with the surfaces, with pr-exists.sh
  duplicate checks.
- Pages deploy check (G3) after any website PR merge.
