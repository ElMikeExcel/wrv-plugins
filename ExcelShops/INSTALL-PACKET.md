# INSTALL PACKET -- ExcelShops 1.3.9 (Windows-safe directory sync) -- Nov 14, 2026

- Backlog: P-FSYNC-1. Desk assembly: board supervisor run 332
  worker (desk only -- no install, no MANIFEST edit, no live
  contact, no live pulls; G1 slots blank for filing day).
- Lint: 9336 B / max line 82 / non-ASCII 0

## Why

Diagnosis: ~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md.
The checkout store's durable fence fsyncs the containing
directory (FileChannel.open(directory) + force()). That open
throws AccessDeniedException under Windows Java NIO, so on the PC
test-server mirror ExcelShops dropped to protection-only mode at
enable even though its data check read the store fine
(EXISTING_TARGET). The live Linux server is unaffected. In 1.3.9
every directory-force site treats a platform refusal
(AccessDeniedException | UnsupportedOperationException) as
best-effort (checkout/FileJournal.syncDirectory,
checkout/GlobalSessionGuard.syncDirectory + archive path,
LegacyDataMigration.syncDirectory); the file-level fsync stays
mandatory and any other IOException keeps the existing semantics.
No offers, prices, catalog, transaction logic, log lines, or
player-visible text changed anywhere in this build.

## Candidate

- Artifact: ~/workspace/shops-fsync-fix-20261114/ExcelShops-1.3.9.jar
  -- 266,677 B / SHA-256
  9473f1ed369a09512b689eed5a23c588fb3e9c20e8c251558a1ac56ed7ea8e73
  -- 108 classes.
- Tests: 433/433 on both builds (430 carried + 3 new
  DirectorySyncBestEffortTest checks; the new suite fails 0/3 on
  the unmodified 1.3.8 classes with the simulated refusal).
  Detail: TEST-RESULTS.md in the build folder.
- Deterministic: two independent builds (work/out-139-a,
  work/out-139-b) packaged byte-identical.
- Base gate: the unmodified copied 1.3.8 tree rebuilt
  byte-identical to the shipped 1.3.8 jar (266,417 B / SHA-256
  b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9),
  which INSTALLED.txt in the 1.3.8 folder records as installed
  live Nov 11, 2026. Desk lineage only -- NOT a live premise.
- Source: ExcelShops-1.3.9-source.zip -- 193,405 B / SHA-256
  e2c3559617d820249ba9e72ada5c7e5c5f6b7bec8794ee8ede4d3dd7559156af
  (same build folder; hashes in SHA256SUMS there).
- Rig acceptance: MET Nov 14, 2026 (mirror boot acceptance,
  ~/workspace/fsync-mirror-proof-20261114/PROOF.md, plus the
  follow-on in ~/workspace/fsync-mirror-followon-20261114/FOLLOWON.md):
  the fsync form of the protection-only signature occurs 0 times
  and AccessDeniedException 0 in the boot log; after the mirror's
  stale foreign session state was cleared (backed up first), 1.3.9
  enabled fully with "ExcelShops purchases available." and
  "ExcelShops enabled." The residual session-proof failure was a
  mirror-carried live envelope bound to the Linux root; on the
  live server the envelope root matches, so no clearing applies
  live. Mirror evidence, not a live premise.

## Live-premise receipts (G1) -- BLANK, fill at filing

RULE: every slot below is filled ONLY from a same-day live pull at
filing time (rclone pull via the PC route; if the PC route fails
its probes -- 3 attempts over ~2 minutes -- a panel File Manager
pull labeled FALLBACK (panel)). Disk lineage, build folders, and
older records are NEVER a premise source. Full receipts go in
~/workspace/shops-fsync-fix-20261114/pull-g1/RECEIPTS.md
(path + size + SHA-256 + pull timestamp per slot), cited from the
MANIFEST stub by path + hash. If any slot cannot be filled, STOP:
do not file.

- L1 live jar: plugins/<LIVE JAR FILENAME: ______> = <SIZE: ______>
  B / SHA-256 <______> -- receipt: <______>
- L2 exactly-one-jar census: live plugins/ listing (command:
  `ls plugins/ | grep -i shops`) shows exactly ONE loadable
  ExcelShops jar: <LISTING / COUNT: ______> -- receipt: <______>
- L3 live enable version: same-day pull of latest.log shows
  "Enabling ExcelShops v<______>" as the live version:
  <LINE: ______> -- receipt: <______>

## Live-jar gate (STOP on mismatch)

At the window, re-pull the live jar fresh and compare to slot L1.
STOP on any mismatch of filename, size, or SHA-256, and STOP if
the census (slot L2, re-run) shows anything other than exactly one
loadable ExcelShops jar. Do not proceed on this packet's text
alone: the gate values live in the G1 receipts, not here.

## Scope

- JAR ONLY. Place the candidate under its own filename:
  plugins/ExcelShops-1.3.9.jar. Hold the predecessor (the live jar
  named in slot L1) aside in plugins/ as
  <live jar filename>.bak-20261114-pre139. After the swap the
  census must again show exactly one loadable ExcelShops jar.
- NOT changed: no config, data, catalog, or player files. The
  live plugins/ExcelShops/ folder (checkout journals, session
  envelope/fence, legacy-checkout) is untouched -- in particular
  do NOT clear or re-mint any live session state; the mirror
  clearing in the follow-on was mirror-only. Linux behaviour is
  unchanged except that a refused directory sync no longer drops
  the plugin to protection-only mode.

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
  9473f1ed369a09512b689eed5a23c588fb3e9c20e8c251558a1ac56ed7ea8e73
  (command: `sha256sum ExcelShops-1.3.9.jar`); the placed jar is
  pull-back verified to the same hash after placement.

## Swap steps

1. Gates above; fill and check G1 slots L1-L3.
2. Rename the live jar (slot L1) to
   <live jar filename>.bak-20261114-pre139.
3. Place ExcelShops-1.3.9.jar (hash per Hash STOP); re-run the
   census: exactly one loadable ExcelShops jar.
4. Start; run Boot verify. On any failure, Rollback.

## Boot verify (by script output, from latest.log)

- Boot Done line present: `grep -c "Done (" latest.log` >= 1.
- Enable line: `grep "Enabling ExcelShops v1.3.9" latest.log`
  = 1 hit.
- `grep -c "ExcelShops purchases available" latest.log` = 1.
- `grep -c "protection-only" latest.log` = 0.
- `grep -c "Session proof not eligible" latest.log` = 0.
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
plugins/ExcelShops-1.3.9.jar, rename
<live jar filename>.bak-20261114-pre139 back to the slot L1
filename, start, and verify: Enabling ExcelShops v1.3.8,
purchases-available count noted, SEVERE 0 attributable. The .bak
stays in plugins/ until the live proof passes, then is removed by
a later cleanup entry.

## Same-pass surfaces -- OWED, named

A player-facing close-out is not done until each surface below is
done in the same pass or carried here as owed (they are all OWED
at filing; none rides this packet's jar swap):

- What's New, website surface: 1.3.9 entry or an explicit
  no-change determination, recorded in the close-out.
- What's New, in-game surface (ExcelWhatsNew changelog): same
  entry / determination as the website surface -- both or neither.
- Guide: knowledge + intents check against 1.3.9 (no command or
  mechanic changes; expected no-change, but the check is owed and
  its result recorded).
- Website pair: index + ai-knowledge ExcelShops row catch-up to
  1.3.9 (row version), via the normal website leg (G2 duplicate
  guard + claim line before any push; G3 Pages deploy check after
  any merge).
- Broadcaster tips: checked; result recorded.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked; result
  recorded.

## Proof owed after install

A live purchase / shop setup flow by Mike or Kailey completing
outside protection-only mode. Discharged only by the tester's own
report. Until it passes, the predecessor .bak stays in place and
the surfaces close-out stays open.

## Marker on install

~/workspace/shops-fsync-fix-20261114/INSTALLED.txt
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
