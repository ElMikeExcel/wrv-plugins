# INSTALL PACKET - ExcelShops 1.3.11 (ExcelItems migration M4)

- Filed by: board supervisor run 567, worker W4 (filing prep).
- Date prepared: Wed Nov 18, 2026.
- Lint: 9344 B / max line 273 / non-ASCII 0
- Packet folder: ~/workspace/board-r567/migration-filing/
- Build folder: ~/workspace/board-r559/m4-shops/
- Receipts: ~/workspace/board-r567/migration-filing/M4-RECEIPTS.md
  - 4,546 B / SHA-256
  759376f4a733f13e328819c3ef3436a24fae7455e50d6b11933b9d19983cfa7c
  (referenced, not inlined; pulls in the same folder's pulls/).

## Candidate

- Artifact: ~/workspace/board-r559/m4-shops/ExcelShops-1.3.11.jar
  277,494 B / SHA-256
  91b8559bf63cf2b03aff53a1d7180cffd881d505852ee3c08e0721becc735102
- Build: ExcelShops 1.3.10 -> 1.3.11 (patch bump for the
  ExcelItems hard depend + catalog rewire). Tests 453/453
  PASS (443 base + 10 new M4 tests), in two independent
  build runs, jars byte-identical across both. See the
  build folder's BUILD-RECORD.md.
- Rig: G-4 mirror PASS Nov 18, 2026 (co-resident with live
  ExcelItems 0.1.0, SEVERE 0, dependent enables AFTER
  ExcelItems, purchases available, no Mobile shop
  descriptions unavailable). See
  ~/workspace/board-r560/G4-RECORD.md
  (6,460 B / 5a72391f7f14f0ed61a816ebc01a0edf1750ebab9fdb6bdf396f5ea942f342e4).
- Source zip: ../board-r559/m4-shops/ExcelShops-1.3.11-source.zip,
  663,538 B, SHA-256
  496e8791c71fd1b91e21ed31ae34da91b32750c267013d27f70d5db15ca511a7

## Live-premise receipts (G1) - summary, full text in RECEIPTS.md

- Live jar plugins/ExcelShops-1.3.10.jar - receipt F1:
  pulled Nov 18, 2026, 22:52 CST via Homebase/rclone;
  268,988 B / 0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a
  - EXACT vs build-day G-1. Extracted plugin.yml: version
  1.3.10, no ExcelItems depend.
- Live config plugins/ExcelShops/config.yml - receipt F2:
  443 B / 4eac2df56f4b0c16342f3077a226ad35ee1c01f4867ad199b02023425532468b
  - EXACT vs build-day G-1; no special-products section, so
  the bundled resource governs. Not replaced.
- Live catalog plugins/ExcelShops/spawn-catalog.yml -
  receipt F3: 5,963 B /
  7f9a41cca38390e9bf3aed1102009d6c8f5ece8ff2ddf498c75559a96fc15360
  - EXACT vs build-day G-1. Not replaced.
- Live ExcelItems plugins/ExcelItems-0.1.0.jar - receipt F4:
  20,214 B / 85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d
  - EXACT vs build of record; the hard-depend premise.

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelShops-1.3.10.jar must equal
  268,988 B / 0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a
  at the install gate (fresh pull, sha256sum output is the
  evidence). Mismatch = STOP, re-base, do not swap. (A
  sibling ExcelShops-1.3.10.jar.bak-prerevive sits live
  from the Nov 18 revive window; it is not loadable and is
  not part of this gate - leave it untouched.)
- Dependency gate: live plugins/ExcelItems-0.1.0.jar must
  equal 20,214 B / 85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d.
  Absent or mismatched = STOP (the candidate hard-depends
  on ExcelItems and will not enable without it).

## Scope

- ONE gated unit: swap ONE jar -
  plugins/ExcelShops-1.3.10.jar out,
  plugins/ExcelShops-1.3.11.jar in (bytes per Candidate).
- What changes: plugin.yml gains depend [ExcelItems] and
  version 1.3.11; the four special products (registry IDs
  1-4) become config listings (special-products) resolved
  against the ExcelItems registry instead of code-pinned
  entries; delivery stays the owner console command from
  the listing; the catalog invariant is re-expressed
  against registry IDs (22-product baseline preserved;
  a config-only listing of another registered ID now
  loads - the design law working).
- Does NOT change: any data-folder file (live config.yml
  and spawn-catalog.yml untouched), prices, slots, names,
  or purchase flow; bundled mobile menus inside the jar
  are byte-identical to live; no other plugin, world, or
  player data.
- One player-visible text delta (named at build): the
  mobile details screen for a special now shows the item's
  own registry lore instead of the old menu copy. Names,
  prices, slots, and flow are unchanged.

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
3. Upload ExcelShops-1.3.11.jar to plugins/ (installer's
   lane choice); verify placed size 277,494 B and SHA-256
   equals the Candidate hash. Remove/rename the 1.3.10 jar
   per the standing swap pattern (kept as the backup-side
   copy named in the close-out, not left loadable). Do not
   touch the .bak-prerevive sibling.
4. Shut Down (after the second marker re-check), Start.
5. Boot verify (below). On failure: rollback (below).

## Boot verify (by script output)

- Log grep: Enabling ExcelItems v0.1.0 count = exactly 1.
- Log grep: ExcelItems enabled: 4 custom item(s)
  registered, next free id 5. count = exactly 1.
- Log grep: Enabling ExcelShops v1.3.11 count = exactly 1,
  at a HIGHER line number than the ExcelItems enabling
  line (dependent AFTER dependency; mirror rig: 427 > 368).
- Log grep: ExcelShops purchases available. count = 1.
- Log grep: Mobile shop descriptions unavailable
  count = 0.
- SEVERE count attributable to the boot = 0 (grep count).
  (Two pre-existing ExcelWhatsNew changelog-length WARN
  lines are known and unrelated - see G4-RECORD.md.)
- Console/RCON excelitems status returns
  ExcelItems: 4 item(s) registered, next free id 5.
- Boot reaches Done (...).
- Marker: write ~/workspace/board-r559/m4-shops/INSTALLED.txt
  with window, lane, backup name, gate results, and the
  verify counts above.

## Rollback

- Remove plugins/ExcelShops-1.3.11.jar, restore
  plugins/ExcelShops-1.3.10.jar (gate hash above), restart,
  confirm the 1.3.10 enabling line and ExcelShops
  purchases available. are back and SEVERE = 0. No
  config/data rollback is needed (none was touched).
  The pre-install backup is the second net.

## Same-pass surfaces (checked at build, G-6)

- Player-facing change: none to prices, slots, names, or
  purchase flow; one details-screen lore alignment (above).
- What's New (website + in-game changelog): NO entry owed
  per the build record (presentation byte-identical; the
  lore alignment is an internal consistency fix). If the
  supervisor prefers an entry, it is a one-line text
  placement, not part of this jar unit.
- Website pair: no change owed.
- Guide: HELD under the Guide-development hibernation
  (Mike, Nov 16, 2026); owed on wake: shop chunk refresh
  only if a taught shop flow changes - none does.
- Tips check: checked at build - shop tips remain true,
  no tip edit owed.
- Menus check (ExcelMenu + ExcelBedrockMenu): verified at
  build - bundled menus byte-identical to live (Revive
  Bedrock slot 5); Java live menu files untouched (Revive
  slot 6, Lost & Found untouched).

## Proof owed AFTER install (post-install, NOT gates - D122)

- Bedrock form rendering of the migrated special shelf;
  in-game buy flow per special (IDs 1-4); /excelshopadmin
  reload with a data-folder special-products section
  present. Via the board's Needs Testing, post-release.
  Ship gate per D122 is already met: mirror load clean +
  tests pass.

## Close-out owed (installer/failsafe)

- MANIFEST conversion: this entry becomes a one-line
  tombstone; full record to MANIFEST-ARCHIVE.md.
- Publication claim lines (G2) for the records close-out
  (wrv-plugins draft PR + Drive deposits land with the M5
  migration close-out; pr-exists.sh duplicate check first).
- Records staging for this piece: build folder
  ~/workspace/board-r559/m4-shops/ (jar + repro jar +
  source zip + BUILD-RECORD.md + SHA256SUMS.txt).

## MANIFEST stub (supervisor files verbatim)

## PENDING - ExcelShops 1.3.11 (ExcelItems migration M4) (Nov 18, 2026)
- Packet: ~/workspace/board-r567/migration-filing/M4-PACKET.md
- Candidate: ~/workspace/board-r559/m4-shops/ExcelShops-1.3.11.jar - 277,494 B / 91b8559bf63cf2b03aff53a1d7180cffd881d505852ee3c08e0721becc735102; tests 453/453; G-4 mirror PASS Nov 18, 2026
- Live-jar gate: plugins/ExcelShops-1.3.10.jar = 268,988 B / 0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a (G1 filing pull Nov 18, 2026, 22:52 CST, Homebase/rclone; receipts M4-RECEIPTS.md 759376f4a733f13e328819c3ef3436a24fae7455e50d6b11933b9d19983cfa7c)
- Scope: one jar swap only; specials become registry-ID config listings; no data-folder config change; prices/slots/flow unchanged
- Marker: ~/workspace/board-r559/m4-shops/INSTALLED.txt
- Proof owed after install: Bedrock shelf rendering + buy flow per special + reload check (post-install per D122, not a gate). Surfaces: none owed (Guide held under hibernation)
