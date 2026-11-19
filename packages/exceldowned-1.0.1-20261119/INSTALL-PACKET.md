# PENDING - ExcelDowned 1.0.1 (ExcelItems migration M1, ID 4)

- Lint: 4808 B / max line 76 / non-ASCII 0 (P3, run 567)
- Packet: ~/workspace/board-r567/migration-filing/M1-PACKET.md
- Receipts: ~/workspace/board-r567/migration-filing/M1-RECEIPTS.md
  (SHA-256 cde72d416dd36fb25c1096922db56c7ec92adbe464bb7b83e432a01d8c090324)
- Build record: ~/workspace/board-r559/m1-downed/BUILD-RECORD.md
- Filed by: board supervisor run 567 (filing prep worker W3;
  MANIFEST write is the supervisor's alone)

## Live-premise receipts (G1, fresh Nov 18, 2026, 10:50 PM CST)

- Live plugins/ExcelDowned-1.0.0.jar exists at 1.0.0 -
  receipt: M1-RECEIPTS.md R1 - 140,683 B /
  fcfa96d2036926b0eee5eaf338196ceee55680ca56e9df415563e35b1990e57c
  (Homebase rclone pull, PC + VM hashes agree)
- Live plugins/ExcelDowned/config.yml - receipt:
  M1-RECEIPTS.md R2 - 4,916 B /
  94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf
- Live plugins/ExcelItems-0.1.0.jar installed - receipt:
  M1-RECEIPTS.md R3 - 20,214 B /
  85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d

## Candidate

- ~/workspace/board-r559/m1-downed/ExcelDowned-1.0.1.jar -
  140,323 B / SHA-256
  9e8ae0f5b297045a5e913e2c8e7c3e2cccb858c1fe32bbed6e8bb2f7566c32b3
- Build: EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d
- Tests: 77/77 PASS (71 baseline + 6 M1), two runs,
  candidate byte-identical to repro jar both runs.
- Source: ExcelDowned-1.0.1-source.zip - 108,373 B /
  13390e265444cd543f584cbf2a934429776339264295b08c5e30630d88d902b0
  (in ~/workspace/board-r559/m1-downed/, SHA256SUMS.txt)

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelDowned-1.0.0.jar must equal
  140,683 B / fcfa96d2...e57c (R1) at install provenance;
  re-pull fresh at the gate, never cite this receipt as
  the gate itself.

## Scope

- Changes: jar swap 1.0.0 -> 1.0.1 only. plugin.yml gains
  hard depend on ExcelItems; revive potion (registry
  ID 4) mint fully delegates to ExcelItemsAPI.create(4);
  identify is primary-first / legacy-fallback via the
  API; revive behavior registered for ID 4 at enable.
  Minted potion is the registry definition, dual-stamped
  (excelitems:id 4 + legacy exceldowned marks).
- Does NOT change: config.yml (byte-identical, never
  replaced), data files, $5,000 config price, buy/give
  verb split, Cleric Raise Ally, menus, cooldowns, any
  other plugin. ExcelItems itself is NOT swapped (live
  0.1.0 jar stays, R3).

## Pre-install gate (carried openly, per supervisor instruction)

- PRE-INSTALL GATE: the Homebase-mirror boot of this
  candidate + live ExcelItems jar on wrv-testserver was
  NOT run. The VM-local Paper rig passed in substance
  (board-r559 BUILD-RECORD G-4: Done, SEVERE 0, depend
  order proven, behavior line x1, no journal WARN) but
  it does not include the RCON excelitems status read.
  Before install, EITHER run the mirror leg (candidate +
  live ExcelItems 0.1.0 co-resident; expect enabling
  x1 each, Downed AFTER ExcelItems, SEVERE 0, RCON
  excelitems status = 4 registered / next free 5) OR the
  supervisor records a D122 waiver of the mirror form.
  Filing does not waive this gate.

## Gates (install window, failsafe-owned)

- Marker discipline per D67 v2 with re-checks before
  Shut Down and before Start.
- BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manual restore points).
- Emptiness: RCON list = 0/10 twice before the swap.

## Boot verify (verification by script output)

- grep -c "Enabling ExcelItems v0.1.0" latest.log = 1
- grep -c "Enabling ExcelDowned v1.0.1" latest.log = 1,
  line number greater than the ExcelItems line
- grep "ExcelItems enabled:" latest.log =
  "4 custom item(s) registered, next free id 5." x1
- grep "registered revive behavior for ExcelItems ID 4"
  latest.log = 1
- grep "ExcelDowned 1.0.1 ready" latest.log shows build
  EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d
- grep -c "purchase journal unavailable" latest.log = 0
- grep -c SEVERE latest.log attributable to this swap = 0
- RCON: excelitems status =
  "4 item(s) registered, next free id 5."
- Marker: ~/workspace/board-r559/m1-downed/INSTALLED.txt

## Same-pass surfaces

- Player-facing change: NONE (build record G-6).
- What's New (website + in-game changelog): no entry
  owed - no player-visible behavior change.
- Guide: hibernated; owed on wake: none expected.
- Tips: checked, revive tip wording still true, no change.
- Menus (ExcelMenu + ExcelBedrockMenu): no change -
  Downed owns no shop menu.

## Proof owed after install

- Post-release only (D122, not a gate): in-game revive
  potion buy at $5,000, walk-up Revive flow, and a
  pre-migration (legacy-stamped) held potion still
  identifying - via the board's Needs Testing, any
  player. Marker on install:
  ~/workspace/board-r559/m1-downed/INSTALLED.txt

## D125 GATE DISCHARGE (Nov 19, 2026, ~1:55 AM CST)

- The Homebase-mirror leg named in the Pre-install gate above
  HAS NOW RUN and PASSED: candidate ExcelDowned 1.0.1
  (140,323 B / 9e8ae0f5...) co-resident with the live
  ExcelItems 0.1.0 jar (85dbb9ea...) on wrv-testserver -
  Done (22.965s), SEVERE 0, Enabling ExcelItems v0.1.0 x1
  (log line 429) BEFORE Enabling ExcelDowned v1.0.1 x1
  (line 541), "registered revive behavior for ExcelItems
  ID 4" x1, ready line carries build
  EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d, "purchase
  journal unavailable" x0, RCON excelitems status =
  "4 item(s) registered, next free id 5."
- Mirror restored to its exact found state afterward
  (84-jar census identical pre/post) and left booted.
- Record: ~/workspace/board-r571-m1-g4/G4-M1-RECORD.md
- The D125 pre-install gate is MET. No D122 waiver was
  needed or recorded.
