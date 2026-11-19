# PENDING - ExcelDeathProtection 0.2.1 (ExcelItems migration M2, IDs 3/2)

- Lint: 4846 B / max line 76 / non-ASCII 0 (P3, run 567)
- Packet: ~/workspace/board-r567/migration-filing/M2-PACKET.md
- Receipts: ~/workspace/board-r567/migration-filing/M2-RECEIPTS.md
  (SHA-256 af41738c676716927f3dc8d54ad3cd153bfa246d72cfb4ca5979547519de1b35)
- Build record: ~/workspace/board-r559/m2-deathprotection/BUILD-RECORD.md
- Rig record: ~/workspace/board-r560/G4-RECORD.md (M2: G-4 PASS)
- Filed by: board supervisor run 567 (filing prep worker W3;
  MANIFEST write is the supervisor's alone)

## Live-premise receipts (G1, fresh Nov 18, 2026, 10:50 PM CST)

- Live plugins/ExcelDeathProtection-0.2.0.jar exists at
  0.2.0 - receipt: M2-RECEIPTS.md R1 - 59,390 B /
  ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719
  (Homebase rclone pull, PC + VM hashes agree)
- Live plugins/ExcelDeathProtection/config.yml -
  receipt: M2-RECEIPTS.md R2 - 150 B /
  d6e3208761c5b08dee1687237946b85d4821dd9f3020aa39b54bfbf082115efc
  (minimal live config: basic 15000.0 / premium 30000.0)
- Live plugins/ExcelItems-0.1.0.jar installed - receipt:
  M2-RECEIPTS.md R3 - 20,214 B /
  85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d

## Candidate

- ~/workspace/board-r559/m2-deathprotection/ExcelDeathProtection-0.2.1.jar -
  61,855 B / SHA-256
  deb7b40ae3fdfa74534dc8d1aabfa0434c267275f0db430861a7221f64a92749
- Build: EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e
- Tests: 127/127 PASS (118 baseline + 9 registry tests,
  incl. ID-2 ACTIVATION negative test and premium
  credit-seam test), double-build byte-identical.
- Source: ExcelDeathProtection-0.2.1-source.zip -
  62,231 B /
  310c80d5dddceeb6c2816d07f65897c19f822c609215f427ff1b504120b8714f
  (in ~/workspace/board-r559/m2-deathprotection/, SHA256SUMS)
- Rig: G-4 PASS on the Homebase mirror (G4-RECORD M2:
  Done 23.793s, SEVERE 0, dependent after ExcelItems,
  RCON status 4 registered / next free 5).

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelDeathProtection-0.2.0.jar must
  equal 59,390 B / ed819bb6...0719 (R1) at install
  provenance; re-pull fresh at the gate, never cite
  this receipt as the gate itself.

## Scope

- Changes: jar swap 0.2.0 -> 0.2.1 only. plugin.yml
  gains hard depend on ExcelItems; premium potion
  (registry ID 3) mint fully delegates to
  ExcelItemsAPI.create(3); identify via the API,
  primary-first / legacy-fallback; basic (registry
  ID 2) is ACTIVATION - resolved from the registry,
  display stack only, never a giveable item (negative
  test pinned); behaviors registered for IDs 3 and 2
  at enable; enable fails closed on a drifted registry
  definition. Premium credit seam code unchanged.
- Does NOT change: config.yml (live config never
  replaced), data files, prices ($15,000 basic /
  $30,000 premium from live config), commands, credit
  math/re-arm, any other plugin. ExcelItems itself is
  NOT swapped (live 0.1.0 jar stays, R3).

## Gates (install window, failsafe-owned)

- Marker discipline per D67 v2 with re-checks before
  Shut Down and before Start.
- BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manual restore points).
- Emptiness: RCON list = 0/10 twice before the swap.
- No pre-install rig gate owed: G-4 discharged (above).

## Boot verify (verification by script output)

- grep -c "Enabling ExcelItems v0.1.0" latest.log = 1
- grep -c "Enabling ExcelDeathProtection v0.2.1"
  latest.log = 1, line number greater than the
  ExcelItems line
- grep "ExcelItems enabled:" latest.log =
  "4 custom item(s) registered, next free id 5." x1
- grep "ExcelItems registry: premium id 3" latest.log =
  the registry line from G4-RECORD line 233 x1
- grep "ExcelDeathProtection" latest.log shows build
  EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e and the
  premium-credit seam active line
- grep -c SEVERE latest.log attributable to this swap = 0
- RCON: excelitems status =
  "4 item(s) registered, next free id 5."
- Marker: ~/workspace/board-r559/m2-deathprotection/INSTALLED.txt

## Same-pass surfaces

- Player-facing change: NONE (build record surfaces check).
- What's New (website + in-game changelog): no entry
  owed - no player-visible behavior change.
- Guide: hibernated; owed on wake: none expected.
- Tips: checked, no DeathProtection tip wording affected.
- Menus (ExcelMenu + ExcelBedrockMenu): no change -
  DeathProtection owns no shop menu; shop entries still
  call deathprotection buy basic|premium.

## Proof owed after install

- Post-release only (D122, not a gate): in-game premium
  buy-and-drink, basic buy activation, banked-credit
  re-arm with a real credit, and a pre-migration
  (legacy-stamped) held potion drink - via the board's
  Needs Testing, any player. Marker on install:
  ~/workspace/board-r559/m2-deathprotection/INSTALLED.txt
