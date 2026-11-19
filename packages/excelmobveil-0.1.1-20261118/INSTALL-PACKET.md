# INSTALL PACKET - ExcelMobVeil 0.1.1 (ExcelItems migration M3)

- Filed by: board supervisor run 567, worker W4 (filing prep).
- Date prepared: Wed Nov 18, 2026.
- Lint: 8215 B / max line 273 / non-ASCII 0
- Packet folder: ~/workspace/board-r567/migration-filing/
- Build folder: ~/workspace/board-r559/m3-mobveil/
- Receipts: ~/workspace/board-r567/migration-filing/M3-RECEIPTS.md
  - 3,700 B / SHA-256
  407794d2d1272b486e73c2ee119653ce67aff37de3c5f2e6f590deabc84c45fd
  (referenced, not inlined; pulls in the same folder's pulls/).

## Candidate

- Artifact: ~/workspace/board-r559/m3-mobveil/ExcelMobVeil-0.1.1.jar
  61,819 B / SHA-256
  42ffa15514513366e973a4cefdd106a108bd31004ed166cbbbac3fca1863560d
- Build: ExcelMobVeil 0.1.0 -> 0.1.1 (patch bump for the
  ExcelItems hard depend + mint/identify rewire). Tests
  195/195 PASS (180 base + 15 new, incl. the M3 stacking
  gate). Double-build byte-identical. See the build folder's
  BUILD-RECORD.md.
- Rig: G-4 mirror PASS Nov 18, 2026 (co-resident with live
  ExcelItems 0.1.0, SEVERE 0, dependent enables AFTER
  ExcelItems). See ~/workspace/board-r560/G4-RECORD.md
  (6,460 B / 5a72391f7f14f0ed61a816ebc01a0edf1750ebab9fdb6bdf396f5ea942f342e4).
- Source zip: ../board-r559/m3-mobveil/ExcelMobVeil-0.1.1-source.zip,
  64,529 B, SHA-256
  480749132098aaf551abc2bf7992eb7d7a66f6404083d8c4904fe2e821ee26a9

## Live-premise receipts (G1) - summary, full text in RECEIPTS.md

- Live jar plugins/ExcelMobVeil-0.1.0.jar - receipt F1:
  pulled Nov 18, 2026, 22:52 CST via Homebase/rclone;
  61,085 B / 66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc
  - EXACT vs build-day G-1. Extracted plugin.yml: version
  0.1.0, depend [Vault].
- Live config plugins/ExcelMobVeil/config.yml - receipt F2:
  333 B / fe5ceea01561f3e9e929dfdbd956346abf76e43cd358b4c91d87379b7e2ec331
  - EXACT vs build-day G-1. Not replaced by this install.
- Live ExcelItems plugins/ExcelItems-0.1.0.jar - receipt F3:
  20,214 B / 85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d
  - EXACT vs build of record; the hard-depend premise.

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelMobVeil-0.1.0.jar must equal
  61,085 B / 66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc
  at the install gate (fresh pull, sha256sum output is the
  evidence). Mismatch = STOP, re-base, do not swap.
- Dependency gate: live plugins/ExcelItems-0.1.0.jar must
  equal 20,214 B / 85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d.
  Absent or mismatched = STOP (the candidate hard-depends
  on ExcelItems and will not enable without it).

## Scope

- ONE gated unit: swap ONE jar -
  plugins/ExcelMobVeil-0.1.0.jar out,
  plugins/ExcelMobVeil-0.1.1.jar in (bytes per Candidate).
- What changes: plugin.yml gains depend [Vault, ExcelItems]
  and version 0.1.1; veil potions (registry ID 1) are minted
  via ExcelItemsAPI.create(1) and identified primary-first
  via the registry (legacy mark fallback inside the API);
  the veil-drink behavior registers against ID 1 at enable.
- Does NOT change: any config or data file (live config.yml
  and purchases.journal untouched; the candidate carries no
  config replacement), any other plugin, world or player
  data. Price ($2,000), name, lore, drink/stacking behavior
  are unchanged - the change is internal (mint/identify).

## Gates (failsafe, standing discipline)

- Marker discipline per D67 v2, with marker re-checks
  immediately before Shut Down and again before Start.
- BACKUP FIRST + cap-at-6: take the pre-install backup
  before the swap; if pruning is needed, name the prunees
  in the close-out; never prune the Daily Backup's newest
  copy or the two newest manual restore points.
- Emptiness gate: console list = 0 of 10, twice, before the
  shutdown/swap. Players on = countdown path per standing
  rule, never a silent swap.
- Provenance gate at swap: the placed jar's SHA-256 must
  equal the Candidate hash above; mismatch = STOP.

## Install steps (script-shaped)

1. Backup per Gates; record backup name.
2. Verify both gate hashes above by fresh pull + sha256sum.
3. Upload ExcelMobVeil-0.1.1.jar to plugins/ (installer's
   lane choice); verify placed size 61,819 B and SHA-256
   equals the Candidate hash. Remove/rename the 0.1.0 jar
   per the standing swap pattern (kept as the backup-side
   copy named in the close-out, not left loadable).
4. Shut Down (after the second marker re-check), Start.
5. Boot verify (below). On failure: rollback (below).

## Boot verify (by script output)

- Log grep: Enabling ExcelItems v0.1.0 count = exactly 1.
- Log grep: ExcelItems enabled: 4 custom item(s)
  registered, next free id 5. count = exactly 1.
- Log grep: Enabling ExcelMobVeil v0.1.1 count = exactly 1,
  at a HIGHER line number than the ExcelItems enabling line
  (dependent AFTER dependency; mirror rig: 507 > 424).
- Log grep: the 0.1.1 enable line beginning
  ExcelMobVeil 0.1.1: $2,000.00; each drink adds eight
  real-time minutes. count = exactly 1 (full line per
  G4-RECORD.md M3 section).
- SEVERE count attributable to the boot = 0 (grep count).
- Console/RCON excelitems status returns
  ExcelItems: 4 item(s) registered, next free id 5.
- Boot reaches Done (...); guild state unchanged by this
  unit (no guild files touched).
- Marker: write ~/workspace/board-r559/m3-mobveil/INSTALLED.txt
  with window, lane, backup name, gate results, and the
  verify counts above.

## Rollback

- Remove plugins/ExcelMobVeil-0.1.1.jar, restore
  plugins/ExcelMobVeil-0.1.0.jar (gate hash above), restart,
  confirm the 0.1.0 enabling line is back and SEVERE = 0.
  No config/data rollback is needed (none was touched).
  The pre-install backup is the second net.

## Same-pass surfaces (checked at build, G-6)

- Player-facing change: NONE - same potion, same name,
  lore, price, and drink/stacking behavior.
- What's New (website + in-game changelog): NO entry owed
  (internal rewire only), stated per the same-pass rule.
- Website pair: no change owed.
- Guide: HELD under the Guide-development hibernation
  (Mike, Nov 16, 2026); owed on wake: none expected (no
  behavior change to teach).
- Tips check: checked at build - no veil wording changes,
  no tip edit owed.
- Menus check (ExcelMenu + ExcelBedrockMenu): no change -
  the shop still reaches the potion via the unchanged
  mobveil buy console command; no slot/price/reference
  changes in M3.

## Proof owed AFTER install (post-install, NOT gates - D122)

- In-game veil flow: buy a veil potion, drink it, confirm
  the veil timer applies and stacks (+8 real-time minutes
  per drink, legacy-held and registry-minted alike). Any
  player; report to the board's Needs Testing. Ship gate
  per D122 is already met: mirror load clean + tests pass.

## Close-out owed (installer/failsafe)

- MANIFEST conversion: this entry becomes a one-line
  tombstone; full record to MANIFEST-ARCHIVE.md.
- Publication claim lines (G2) for the records close-out
  (wrv-plugins draft PR + Drive deposits land with the M5
  migration close-out; pr-exists.sh duplicate check first).
- Records staging for this piece: build folder
  ~/workspace/board-r559/m3-mobveil/ (jar + source zip +
  BUILD-RECORD.md + SHA256SUMS).

## MANIFEST stub (supervisor files verbatim)

## PENDING - ExcelMobVeil 0.1.1 (ExcelItems migration M3) (Nov 18, 2026)
- Packet: ~/workspace/board-r567/migration-filing/M3-PACKET.md
- Candidate: ~/workspace/board-r559/m3-mobveil/ExcelMobVeil-0.1.1.jar - 61,819 B / 42ffa15514513366e973a4cefdd106a108bd31004ed166cbbbac3fca1863560d; tests 195/195; G-4 mirror PASS Nov 18, 2026
- Live-jar gate: plugins/ExcelMobVeil-0.1.0.jar = 61,085 B / 66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc (G1 filing pull Nov 18, 2026, 22:52 CST, Homebase/rclone; receipts M3-RECEIPTS.md 407794d2d1272b486e73c2ee119653ce67aff37de3c5f2e6f590deabc84c45fd)
- Scope: one jar swap only; hard depend on live ExcelItems 0.1.0 added; no config/data change; no player-facing change
- Marker: ~/workspace/board-r559/m3-mobveil/INSTALLED.txt
- Proof owed after install: in-game veil buy/drink/stack check (any player, post-install per D122, not a gate). Surfaces: none owed (Guide held under hibernation)
