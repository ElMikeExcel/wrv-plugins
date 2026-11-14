# INSTALL PACKET - ExcelExpansions 0.1.1 (NEW plugin, first live install)

- Lint: 8232 B / max line 93 / non-ASCII 0
- Filed by: board supervisor run 335, lane W5 (filing prep), Nov 14 2026.
- Packet folder: ~/workspace/excelexpansions-011-build-20261114/filing/
- Receipts: filing/pull-g1/RECEIPTS.md (same-day G1 pulls, Nov 14 2026).

## Candidate

- Artifact: ~/workspace/excelexpansions-011-build-20261114/ExcelExpansions-0.1.1.jar
- Size / SHA-256: 61,736 B / b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91
- Build: EXC-EXPANSIONS-0.1.1-WRV-20261114-01 (excel-build.txt in jar).
- Tests: 91/91 PASS (WatcherTest 51/0 + ReleaseTest 40/0, TEST-RESULTS-011.txt).
- Rig: mirror rig run 335 PASS - tap-to-flip 8/8, section 7.8 SETTLED.
  RIG-REPORT.md 41,303 B / b0bb2ea2e092443df0c84dd2e59c755900ed72edea669312d6
  d6d33ef7c6755817 (in ~/workspace/excelexpansions-011-rig-20261114/).
- Barred jar: work/pkg011-A.jar 61,081 B / 203464a6... is NOT the candidate.
  Never place it. STOP if the staged jar hash differs from b9d26233... above.

## Live-premise receipts (G1 - summary; full receipts in pull-g1/RECEIPTS.md)

- ExcelExpansions ABSENT live: no ExcelExpansions jar and no
  plugins/ExcelExpansions/ folder in the Nov 14 2026 census pull
  (plugins-top.txt 16,754 B / 6b66cd2cec6c50b702b56a57241e0d37857e009303c
  6bc1e43d243af6b0e959b); rclone lsf of the folder returned
  "directory not found" (exit 3). This is a NEW placement: there is no
  live predecessor jar, so there is no live-jar swap gate.
- Live plugin count: 476 census entries, 87 loadable jars (name ends .jar).
- EM level-store premise: live plugins/EliteMobs/data/player_data.db =
  28,672 B / 541cebeb2d72db1e316fb9dbb0fd6c8b6b5ef6b1ffca03ecf3c45e1785
  edbe3e - the post-wipe DB (11 rows; HighestLevelKilled, Score, Kills,
  CurrencyV2 all zero, read-only sqlite check in RECEIPTS.md). The level
  store seeds from these zeros at first enable.
- GuildGate premise: live plugins/ExcelGuildGate/state.yml = 1,015 B /
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0 with
  unlocked: false; seals broken: ender_dragon, wither, elder_guardian,
  raid (4 of 5); warden not broken. Corroborated by latest.log line
  "ExcelGuildGate enabled: 4 of 5 seals broken, guild LOCKED".

## Staged artifacts (all hashes verified at filing prep)

1. Jar (placed at plugins/ExcelExpansions-0.1.1.jar):
   61,736 B / b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91
2. Loose config (placed at plugins/ExcelExpansions/config.yml BEFORE
   first boot - see section 7.8 note below):
   filing/config.yml.settled - 3,062 B /
   3c2ab3308d682bbaedc4ab06099ada5e546964f7974b496813b80fedcecce9eb
   Identical to the jar-embedded config except the releaser note now
   carries the settled 7.8 wording. Placing it loose means the jar is
   untouched and the candidate hash above stays valid: the host calls
   saveDefaultConfig(), which never overwrites an existing live config.

## Scope

- PLACE one new jar: plugins/ExcelExpansions-0.1.1.jar (no predecessor).
- PLACE one new folder + file: plugins/ExcelExpansions/config.yml
  (the settled loose config, artifact 2 above), before first boot.
- releases.yml data: NO live file is placed. The watcher manifest is an
  IN-JAR resource (host: getResource("releases.yml") in onEnable). The
  FINAL-manifest data swap is already packaged inside the candidate:
  embedded releases.yml = 55,651 B /
  9fa280f3f854f9fb353986db278d9e481cc348cd151ca51e744ce37fa325cefd,
  generated from ~/workspace/em-storyline-20261113/
  EXCELEXPANSIONS-MANIFEST-FINAL.md (38,932 B /
  d51e0111191f05cfe9d4050a8aad8d70778fdf0b0e56aa017adac6fa913e5fd6),
  57 rows. Verify by unzipping the placed jar and hashing releases.yml.
- On first enable the plugin creates its own data files beside the
  config: level-store.txt and release-state.txt (both plugin-owned).
- DOES NOT change: any other jar, config, or data file; the EM database
  (read-only access by the plugin); GuildGate state; any content pack.
  No EM progression is wiped or altered by this install (the wipe
  already landed Nov 14 04:28 CST under P-SEALFREEZE-1).

## Gates

- Standard gated window: WINDOW-IN-PROGRESS marker discipline with
  re-checks before Shut Down and before Start; ONE fresh backup at
  window open + cap-at-6 (name prunees; never the Daily Backup newest
  copy or the two newest manual restore points); emptiness
  list = 0/10 twice before the swap.
- Provenance gate at install: re-hash the staged jar fresh; STOP on
  any mismatch with the Candidate line. Re-hash the loose config too.

## Install steps (failsafe)

1. Place plugins/ExcelExpansions-0.1.1.jar.
2. Create plugins/ExcelExpansions/ and place config.yml (artifact 2).
3. Start the server; run Boot verify below.
4. Write the marker (below) only after Boot verify passes.

## Boot verify (script-output lines; grep the boot log)

- grep "Enabling ExcelExpansions v0.1.1" -> exactly 1 line.
- grep "57 manifest rows loaded" -> the enable summary line:
  "ExcelExpansions 0.1.1: 57 manifest rows loaded" (full line also
  names the level store + proposer cadence 6h; wording per rig Boot 1).
- Level store refresh line: "EM PlayerData read OK: 11 rows applied
  from plugins\EliteMobs\data\player_data.db" (via /excelexpansions
  store or the boot log); store seeds at post-wipe zeros.
- Watcher tick: within ~2 minutes of enable, the first proposer tick
  runs (1,200 ticks). With the post-wipe zero roster, NO proposal is
  expected at this tick (nobody in band yet); a PROPOSAL line for M-02
  at first tick would be a defect - record and report it.
- grep -c "SEVERE" attributable to ExcelExpansions -> 0.
- Paper "Done (" line present; guild state unchanged: latest boot log
  still shows "4 of 5 seals broken, guild LOCKED" from ExcelGuildGate.
- Console check: /excelexpansions status -> 57 rows, 0 proposed.
- Placed-jar readback: hash of live plugins/ExcelExpansions-0.1.1.jar
  equals the Candidate hash; live config.yml equals artifact 2 hash.

## Rollback

- Stop the server; delete plugins/ExcelExpansions-0.1.1.jar and the
  whole plugins/ExcelExpansions/ folder (config.yml, level-store.txt,
  release-state.txt - all created by this install); restart.
- No other file was touched, so nothing else needs restoring. If a tap
  flip has already happened by rollback time (none can have: proof is
  still owed), restore each flipped pack from its sibling
  <file>.bak-excelexpansions before restarting.

## Same-pass surfaces

- None owed at install: this plugin is infrastructure (watcher +
  proposer + releaser); it changes no player-facing content by itself.
- Owed at the FIRST actual release (named here, per the player
  explanation rule): Guide + What's New BOTH surfaces + website pair
  for the released pack, filed by the supervisor chain when Mike taps.
  The plugin's released-state note already names these as owed.

## Proof owed after install

- Mike's tap on the first real proposal (player proof, Mike only).
  The mirror rig proved the mechanism; the live tap is the proof.
  Until then this item stays PROOF-OWED, not fully closed.

## Marker on install

- ~/workspace/excelexpansions-011-build-20261114/filing/INSTALLED.txt

## Section 7.8 config-note verdict (filing prep, lane W5)

- config.yml is BOTH an in-jar resource AND the live config file.
  Evidence: build.sh copies src/main/resources/config.yml into the jar;
  the jar-embedded copy is 2,991 B /
  3a2de1b3b5fe0b02cad2d4462525efd7db00c31ac8f078b9164a0eb26493e17a,
  byte-identical to src/main/resources/config.yml and to the folder
  root config.yml. The host's onEnable() calls saveDefaultConfig(),
  which materialises that resource as plugins/ExcelExpansions/
  config.yml on first enable and never overwrites an existing file;
  all runtime reads use the live file (getConfig()).
- Consequence: editing the note in src and rebuilding WOULD change
  the candidate hash and void the run-335 rig stamp - do not do it.
  Instead this packet places the settled-wording config as a LOOSE
  file at install (artifact 2). The jar and its rig proof stay intact;
  the live note reads settled from the first boot. Full verdict:
  filing/CONFIG-NOTE-VERDICT.md.
