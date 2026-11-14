# INSTALL PACKET -- ExcelBuildMode 1.3.5 (Windows-safe directory sync) -- Nov 14, 2026

- Backlog: P-FSYNC-1. Desk assembly: board supervisor run 332
  worker (desk only -- no install, no MANIFEST edit, no live
  contact, no live pulls; G1 slots blank for filing day).
- Lint: 8933 B / max line 86 / non-ASCII 0

## Why

Diagnosis: ~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md.
The durable stores' directory fsync (FileChannel.open(directory)
+ force()) throws AccessDeniedException under Windows Java NIO
and degraded the plugin on the PC test-server mirror; the live
Linux server is unaffected. In 1.3.5 a new helper,
core/DirectorySync.bestEffort, makes the directory force
best-effort at four sites (RecoveryJournal, ActiveSessionIndex,
ReviewHoldStore, BuildReturnStore); the file-level
channel.force(true) stays mandatory everywhere. Any platform
refusal from the directory force is tolerated; Linux behaviour is
otherwise unchanged. PlayerDataProof.forceVerifiedMarker was
deliberately NOT converted (fail-closed verification proof; see
BUILD-RECORD.md). No player-facing text, config, permission, or
policy changed beyond the two version banner lines.

## Candidate

- Artifact: ~/workspace/buildmode-fsync-fix-20261114/ExcelBuildMode-1.3.5.jar
  -- 99,879 B / SHA-256
  aa2d35fd4c2fb2a1839c33d2153f96be52c06c02e5e75ec144d27f1911ab8a66
- Tests: 11,699 assertions, 9 suites green (11,685 carried at the
  exact 1.3.4 totals + 14 new DirectorySyncTests). Detail:
  TEST-RESULTS.md in the build folder.
- Deterministic: three full builds (in-place x2 + clean-room)
  reproduce the candidate hash exactly.
- Base gate: the unmodified copied 1.3.4 tree rebuilt
  byte-identical to the on-record 1.3.4 candidate (99,073 B /
  SHA-256 39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3),
  the jar INSTALLED.txt in the 1.3.4 folder records as installed
  live Nov 10, 2026. Desk lineage only -- NOT a live premise.
- Source: ExcelBuildMode-1.3.5-source.zip -- 145,180 B / SHA-256
  c8be342cca0a51cabcfc2badee125b88d66f1b4e0b3191e22c4101e1483b8c9b
  (same build folder; hashes in SHA256SUMS there).
- Rig acceptance: MET Nov 14, 2026 (mirror boot acceptance,
  ~/workspace/fsync-mirror-proof-20261114/PROOF.md): signatures
  S1/S2/S3 (review-hold / build-return / recovery storage
  unavailable) each 1 -> 0, AccessDeniedException 0 in the full
  boot log, SEVERE 0, "Enabling ExcelBuildMode v1.3.5" present.
  The mirror run provisioned plugins/ExcelBuildMode/review-holds
  and build-return first (they were absent on the mirror); the
  live installation provisions them -- slot L3 verifies that live.
  Mirror evidence, not a live premise.

## Live-premise receipts (G1) -- BLANK, fill at filing

RULE: every slot below is filled ONLY from a same-day live pull at
filing time (rclone pull via the PC route; if the PC route fails
its probes -- 3 attempts over ~2 minutes -- a panel File Manager
pull labeled FALLBACK (panel)). Disk lineage, build folders, and
older records are NEVER a premise source. Full receipts go in
~/workspace/buildmode-fsync-fix-20261114/pull-g1/RECEIPTS.md
(path + size + SHA-256 + pull timestamp per slot), cited from the
MANIFEST stub by path + hash. If any slot cannot be filled, STOP:
do not file.

- L1 live jar: plugins/<LIVE JAR FILENAME: ______> = <SIZE: ______>
  B / SHA-256 <______> -- receipt: <______>
- L2 exactly-one-jar census: live plugins/ listing (command:
  `ls plugins/ | grep -i buildmode`) shows exactly ONE loadable
  ExcelBuildMode jar: <LISTING / COUNT: ______> -- receipt: <______>
- L3 live data dirs: plugins/ExcelBuildMode/ contains review-holds,
  build-return, and recovery directories: <LISTING: ______>
  -- receipt: <______>

## Live-jar gate (STOP on mismatch)

At the window, re-pull the live jar fresh and compare to slot L1.
STOP on any mismatch of filename, size, or SHA-256, and STOP if
the census (slot L2, re-run) shows anything other than exactly one
loadable ExcelBuildMode jar. Do not proceed on this packet's text
alone: the gate values live in the G1 receipts, not here.

## Scope

- JAR ONLY. Place the candidate under its own filename:
  plugins/ExcelBuildMode-1.3.5.jar. Hold the predecessor (the live
  jar named in slot L1) aside in plugins/ as
  <live jar filename>.bak-20261114-pre135. After the swap the
  census must again show exactly one loadable ExcelBuildMode jar.
- NOT changed: no config, data, or player files. The live
  plugins/ExcelBuildMode/ folder (sessions, holds, returns,
  recovery data) is untouched. Linux behaviour is unchanged except
  that a refused directory sync no longer disables storage.

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
  aa2d35fd4c2fb2a1839c33d2153f96be52c06c02e5e75ec144d27f1911ab8a66
  (command: `sha256sum ExcelBuildMode-1.3.5.jar`); the placed jar
  is pull-back verified to the same hash after placement.

## Swap steps

1. Gates above; fill and check G1 slots L1-L3.
2. Rename the live jar (slot L1) to
   <live jar filename>.bak-20261114-pre135.
3. Place ExcelBuildMode-1.3.5.jar (hash per Hash STOP); re-run the
   census: exactly one loadable ExcelBuildMode jar.
4. Start; run Boot verify. On any failure, Rollback.

## Boot verify (by script output, from latest.log)

- Boot Done line present: `grep -c "Done (" latest.log` >= 1.
- Enable line: `grep "Enabling ExcelBuildMode v1.3.5" latest.log`
  = 1 hit.
- `grep -c "storage is unavailable" latest.log` = 0.
- `grep -c "AccessDeniedException" latest.log` = 0.
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
plugins/ExcelBuildMode-1.3.5.jar, rename
<live jar filename>.bak-20261114-pre135 back to the slot L1
filename, start, and verify: Enabling ExcelBuildMode v1.3.4,
storage-unavailable count noted (expected 0 on Linux), SEVERE 0
attributable. The .bak stays in plugins/ until the live proof
passes, then is removed by a later cleanup entry.

## Same-pass surfaces -- OWED, named

A player-facing close-out is not done until each surface below is
done in the same pass or carried here as owed (they are all OWED
at filing; none rides this packet's jar swap):

- What's New, website surface: 1.3.5 entry or an explicit
  no-change determination, recorded in the close-out.
- What's New, in-game surface (ExcelWhatsNew changelog): same
  entry / determination as the website surface -- both or neither.
- Guide: knowledge + intents check against 1.3.5 (no command or
  mechanic changes; expected no-change, but the check is owed and
  its result recorded).
- Website pair: index + ai-knowledge ExcelBuildMode row catch-up
  to 1.3.5 (row version), via the normal website leg (G2
  duplicate guard + claim line before any push; G3 Pages deploy
  check after any merge).
- Broadcaster tips: checked; result recorded.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked; result
  recorded.

## Proof owed after install

A live build-mode session by Mike or Kailey exercising a store
save (hold / return / recovery path) with no storage ERRORs in
the live log. Discharged only by the tester's own report. Until
it passes, the predecessor .bak stays in place and the surfaces
close-out stays open.

## Marker on install

~/workspace/buildmode-fsync-fix-20261114/INSTALLED.txt
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
