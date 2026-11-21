# INSTALL PACKET -- ExcelTrainingWheels 0.1.4 (D158 shield widening)
Lint: 04918 B / max line 099 / non-ASCII 0

## PENDING -- ExcelTrainingWheels 0.1.4 (added 2026-11-20)
- **Packet:** `~/workspace/trainingwheels-elitefix-build-20261120/INSTALL-PACKET.md`
- **Live-premise receipts:** `~/workspace/trainingwheels-elitefix-build-20261120/RECEIPTS.md`
  (G1 receipts from the Nov 20, 2026 live check: pull/latest.log 249,869 B /
  e85ed89a87dae9dc447030ca7f12f322c2eaea2abdf5e64f2b3519df1a8eb076; em-files.txt
  283,192 B / 08bcf31ad9c72277ebc43421e3be606230a9a140c3fc840418ead09a9ab757d0;
  same-day, labeled in RECEIPTS.md).
- **Candidate:** `~/workspace/trainingwheels-elitefix-build-20261120/ExcelTrainingWheels-0.1.4.jar`
  -- 41,632 B / SHA-256 `d60416f3de432bc2c8ac618b425510565681d3a8b29222f222fd20bec165abd0`;
  build: deterministic double-build, cmp byte-identical; tests 303/303;
  rig: Paper 26.3-235 + EliteMobs boot, RIGRESULT 10/10 PASS incl. elite-rabbit
  compensated composition (see RECEIPTS.md for the stated rig limit).
- **Live-jar gate (STOP on mismatch):** live file `plugins/ExcelTrainingWheels-0.1.7.jar`
  -- the FILE is named 0.1.7 but the running code is internal version 0.1.3 (boot
  line in the receipt log). The installer pulls the live jar first and confirms
  its internal plugin.yml version reads 0.1.3 before swapping. If the internal
  version is anything else, STOP -- the premise has changed. (No live-jar hash is
  claimed; the live jar bytes were not pulled during the Nov 20 live check.)
- **Staged artifacts:** `~/workspace/trainingwheels-elitefix-build-20261120/`
  (SHA256SUMS in that folder) -- jar above; source zip
  `ExcelTrainingWheels-0.1.4-source.zip` 51,590 B /
  SHA-256 `959eb9ed1e8be9fe122c1d7889994fd7303139ab3e89c3209f1086045a2cfb42`.
- **Scope:** jar-only swap. The shield now scales ALL incoming damage to protected
  players by their factor (0.25 for Rylie) except the environmental family
  (FALL, SUFFOCATION, DROWNING, STARVATION, FIRE, FIRE_TICK, LAVA, CONTACT,
  HOT_FLOOR, FREEZE, CRAMMING, VOID, FLY_INTO_WALL, plus code-held WORLD_BORDER,
  CAMPFIRE, DRYOUT, MELTING). EliteMobs-elite damagers keep the compensation
  (effective factor = base / live dealt multiplier), so a protected player takes
  a quarter of the UNSCALED hit; armor reduces afterwards as vanilla does.
  Explicitly NOT changed: the live config file (untouched -- no config edit in
  this packet), plugin data, door hold, iron siege, commands, factor semantics.
  Rollback: restore the .bak of the previous jar (internal 0.1.3).
- **Gates:** standard gated install. Lane: Runner first per D156, Homebase
  failsafe fallback, Panel last. Marker discipline with re-checks before Shut
  Down and before Start; BACKUP FIRST + cap-at-6 (never delete the Daily newest
  or the two newest manual backups); emptiness `list` = 0 twice before swapping.
  Swap shape: remove `ExcelTrainingWheels-0.1.7.jar` to .bak-20261120 (name the
  old file exactly -- do not leave both jars in plugins/), place
  `ExcelTrainingWheels-0.1.4.jar`.
- **Boot verify:** log shows "Loading server plugin ExcelTrainingWheels v0.1.4",
  then "ExcelTrainingWheels enabled: shield ON (1 protected), door hold ON, iron
  siege ON (threshold 60s, cap 8), door tiers ON (copper 45s).", then
  "shield compensation: seal 5, elite factor 0.1064 (dealt x2.35, base factor
  0.25, source: GuildGate)"; guild LOCKED 5 of 5 (guild is open -- expect the
  standing open state line, no regression); SEVERE 0 attributable.
  Verify by: grep of the post-boot latest.log for those three lines + SEVERE
  count. Marker: `~/workspace/trainingwheels-elitefix-build-20261120/INSTALLED.txt`.
- **Same-pass surfaces (supervisor chain owns, same pass as install):** website
  pair (index.html + ai-knowledge.json -- the shield rule text changes to the
  D158 cover-everything rule with the environmental exceptions), What's New
  BOTH surfaces (website section + in-game ExcelWhatsNew changelog, verified
  from the live file), broadcaster tips check, in-game menus check. Guide
  knowledge/intents: Guide development is hibernated (Nov 16 ruling) -- named
  here as intentionally not updated. GitHub record + Drive deposit ride the
  close-out.
- **Proof owed after install:** a Crazed Rabbit hit on a protected account
  logging a SHIELD line at the compensated factor -- i.e. in em_primis, a
  primis Crazed Rabbit (or any Primis rabbit boss) hits .RylieDSunflower (or a
  protected test account) and latest.log shows "SHIELD ... ENTITY_ATTACK from
  RABBIT ... (factor 0.1064..., elite true, ...)" with the damage reduced to
  the compensated quarter of the unscaled hit. Tester: fleet proof lane
  (Iris-run or Jester/Marvin per the standing split). The proof is only
  discharged by that log line being read back, not by the hit alone.
- **Marker on install:** `~/workspace/trainingwheels-elitefix-build-20261120/INSTALLED.txt`
