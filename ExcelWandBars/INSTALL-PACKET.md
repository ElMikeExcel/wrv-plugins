# INSTALL PACKET -- ExcelWandBars 0.3.18 (Windows-safe directory sync) -- Nov 14, 2026

- Backlog: P-WANDBARS-FSYNC-1. Desk assembly: board supervisor run 331
  worker (desk only -- no install, no MANIFEST edit, no live contact).
- Lint: 9707 B / max line 86 / non-ASCII 0

## SUPERSEDES -- read first

This packet SUPERSEDES the ExcelWandBars 0.3.17 gated entry filed
earlier (run 267). At filing, the supervisor converts that 0.3.17
entry: it must not install alongside or after this one. One packet =
one install unit: ExcelWandBars 0.3.17 -> 0.3.18 only.

## Why

Diagnosis: ~/workspace/fsync-mirror-followon-20261114/FOLLOWON.md
section W. wrv.magic.retention.AuthenticatedWindow.commit() did an
unguarded directory fsync after its atomic move. On Windows Java NIO
the directory force throws AccessDeniedException AFTER the move
lands; the catch-all wrapped it as "durable issuance uncertain;
close/reconcile", PracticeBootstrap fenced managed scopes and
disabled the plugin on the mirror (3 consecutive boots; issuer file
valid every time, epoch advancing). The live Linux server is not at
risk today (directory fsync succeeds there); any Windows run of
WandBars practice issuance is. The build sweep found the same
post-move pattern at 3 directory-force sites, all fixed in 0.3.18:
AuthenticatedWindow.commit(), BoundedPractice.persist(),
PracticeIdentityJournal.persist(). The file-level fsync before the
move stays mandatory; only a platform refusal
(AccessDeniedException | UnsupportedOperationException) from the
directory force is now best-effort. Any other IOException keeps the
existing semantics.

## Candidate

- Artifact: ~/workspace/wandbars-fsync-fix-20261114/ExcelWandBars-0.3.18.jar
  -- 554,089 B / SHA-256
  cadb16a6e70ebcb4236302dfa6c2b67ae23a0ba1b03ac277f57ead9a9d6e1f11
- Build: EXC-WANDBARS-0.3.18-WRV-20261114-01-e17db9. Deterministic:
  two independent builds byte-identical (candidate-a.jar /
  candidate-b.jar, same hash).
- Tests: 679/679 (24 suites: 669 carried + 10 new
  DirectorySyncBestEffortTest checks covering both refusal types at
  all 3 sites). The new suite FAILS on the unmodified 0.3.17 classes
  with the exact production symptom. Detail: TEST-RESULTS.md in the
  build folder.
- Base gate: the unmodified 0.3.17 tree rebuilt byte-identical to
  the shipped 0.3.17 jar -- 553,827 B / SHA-256
  949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b
  (cmp clean). Detail: BUILD-RECORD.md in the build folder.
- Class delta vs shipped 0.3.17: exactly 6 jar entries (the 3 fixed
  classes, BuildInfo, plugin.yml, excel-build.txt); the other 245
  entries are byte-identical. config.yml / spellbook.yml inside the
  candidate are byte-identical to 0.3.17.
- Source: ExcelWandBars-0.3.18-src.zip -- 285,206 B / SHA-256
  b63e39db321fcc798749f4daeee7bb721bfbd0b54c94113867548002d9db6a2f
  (same build folder; hashes in SHA256SUMS there).
- Rig acceptance: MET Nov 14, 2026, ~2:30 AM CST (run 329 W7 Part A):
  PC test-server mirror boot with 0.3.18 placed -- count of
  "durable issuance uncertain" = 0, AccessDenied = 0, SEVERE = 0,
  plugin enabled. This discharges the BUILD-RECORD section 5 rig
  item; it is mirror evidence, not a live premise.

## Live-premise receipts (G1) -- BLANK, fill at filing

RULE: every slot below is filled ONLY from a same-day live pull at
filing time (rclone pull via the PC route; if the PC route fails
its probes -- 3 attempts over ~2 minutes -- a panel File Manager
pull labeled FALLBACK (panel)). Disk lineage, build folders, and
older records are NEVER a premise source. Full receipts go in
~/workspace/wandbars-fsync-fix-20261114/pull-g1/RECEIPTS.md
(path + size + SHA-256 + pull timestamp per slot), cited from the
MANIFEST stub by path + hash. If any slot cannot be filled, STOP:
do not file.

- L1 live jar: plugins/<LIVE JAR FILENAME: ______> = <SIZE: ______>
  B / SHA-256 <______> -- receipt: <______>
- L2 exactly-one-jar census: live plugins/ listing (command:
  `ls plugins/ | grep -i wandbars`) shows exactly ONE loadable
  ExcelWandBars jar: <LISTING / COUNT: ______> -- receipt: <______>
- L3 live Magic version: live Magic plugin version =
  <______> (from the live Magic jar plugin.yml or the boot `list`
  output pulled the same day) -- receipt: <______>

## Live-jar gate (STOP on mismatch)

At the window, re-pull the live jar fresh and compare to slot L1.
STOP on any mismatch of filename, size, or SHA-256, and STOP if
the census (slot L2, re-run) shows anything other than exactly one
loadable ExcelWandBars jar. Do not proceed on this packet's text
alone: the gate values live in the G1 receipts, not here.

## Scope

- JAR ONLY. Place the candidate under its own filename:
  plugins/ExcelWandBars-0.3.18.jar. Hold the predecessor (the live
  jar named in slot L1) aside in plugins/ as
  <live jar filename>.bak-20261114-pre0318. After the swap the
  census must again show exactly one loadable ExcelWandBars jar.
- NOT changed: no config, data, or player files. The live
  plugins/ExcelWandBars/ folder (config, spellbook, loadouts,
  styles, practice data) is untouched. No Magic config changes.
  Linux behaviour is unchanged except that a refused directory
  sync no longer fails a practice save.

## Gates (all mandatory, none waivable)

- Marker discipline: create
  ~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt at window
  start; re-check for a foreign marker immediately before Shut
  Down and again before Start (a foreign marker under 3 hours old
  = coordination abort); delete the marker at window end.
- BACKUP FIRST: ONE fresh Shockbyte backup when the window opens,
  then cap-at-6: prune the OLDEST manual backups until the
  post-creation total is 6 or fewer; name prunees in the outcome;
  never delete the Daily Backup's newest copy or the two newest
  manual restore points.
- Emptiness: `list` = 0 twice before Shut Down.
- Hash STOP: candidate SHA-256 re-verified at the window against
  cadb16a6e70ebcb4236302dfa6c2b67ae23a0ba1b03ac277f57ead9a9d6e1f11
  (command: `sha256sum ExcelWandBars-0.3.18.jar`); the placed jar
  is pull-back verified to the same hash after placement.

## Swap steps

1. Gates above; fill and check G1 slots L1-L3.
2. Rename the live jar (slot L1) to
   <live jar filename>.bak-20261114-pre0318.
3. Place ExcelWandBars-0.3.18.jar (hash per Hash STOP); re-run the
   census: exactly one loadable ExcelWandBars jar.
4. Start; run Boot verify. On any failure, Rollback.

## Boot verify (by script output, from latest.log)

- Boot Done line present: `grep -c "Done (" latest.log` >= 1.
- Enable line: `grep "Enabling ExcelWandBars v0.3.18" latest.log`
  = 1 hit, and the build line reads
  EXC-WANDBARS-0.3.18-WRV-20261114-01-e17db9.
- `grep -c "durable issuance uncertain" latest.log` = 0.
- `grep -c "Managed spell practice unavailable" latest.log` = 0
  (the plugin stays enabled; no disabling line for ExcelWandBars).
- SEVERE attributable to this install = 0:
  `grep -c "SEVERE" latest.log` = 0, or every hit named and shown
  unrelated in the outcome.
- Guild state recorded, not asserted: the ExcelGuildGate LOCKED
  <n> of 5 line from the boot log is copied into the outcome.
- Plugin count recorded from `list`; any drop vs the pre-window
  count is explained in the outcome or the window rolls back.

## Rollback

Shut down (emptiness not required to restore a known-bad swap if
players were let back in -- announce first), delete
plugins/ExcelWandBars-0.3.18.jar, rename
<live jar filename>.bak-20261114-pre0318 back to the slot L1
filename, start, and verify: Enabling ExcelWandBars v0.3.17,
"durable issuance uncertain" count noted (expected 0 on Linux),
SEVERE 0 attributable. The .bak stays in plugins/ until Mike's
live proof passes, then is removed by a later cleanup entry.

## Same-pass surfaces -- OWED, named

A player-facing close-out is not done until each surface below is
done in the same pass or carried here as owed (they are all OWED
at filing; none rides this packet's jar swap):

- What's New, website surface: 0.3.18 entry or an explicit
  no-change determination, recorded in the close-out.
- What's New, in-game surface (ExcelWhatsNew changelog): same
  entry / determination as the website surface -- both or neither.
- Guide: knowledge + intents check against 0.3.18 (no command or
  mechanic changes; expected no-change, but the check is owed and
  its result recorded).
- Website pair: index + ai-knowledge ExcelWandBars row catch-up
  to 0.3.18 (row version), via the normal website leg (G2
  duplicate guard + claim line before any push; G3 Pages deploy
  check after any merge).
- Broadcaster tips: checked; result recorded.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked; result
  recorded.

## Proof owed after install

Mike's live Bedrock proof from P0 -- the same proof the 0.3.17
install awaited: on Bedrock, hold the wand and the wand bar pops
on hold, and spells are selectable. Discharged only by Mike's own
report. Until it passes, the predecessor .bak stays in place and
the surfaces close-out stays open.

## Marker on install

~/workspace/wandbars-fsync-fix-20261114/INSTALLED.txt
(+ OUTCOME.txt in the same folder: window, lane, backup name +
prunees, gate results, boot verify counts, end state).

## Close-out record (at conversion)

The MANIFEST keeps a one-line tombstone; the full record is
appended to ~/workspace/wrv-install-queue/MANIFEST-ARCHIVE.md.
Required lines: Publication claim per G2 for each repo published
(wrv-plugins records draft; winkreyesvalley website) with the
pr-exists.sh duplicate-check timestamp + result; Drive deposits
(jar + source zip ids, or "none"); any baseline-strength hash
labeled "baseline <date>" per G4 (none is used as a premise here).
