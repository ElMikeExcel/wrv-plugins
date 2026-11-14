# INSTALL PACKET -- ExcelMakersMark 0.1.6 (Windows-safe directory sync) -- Nov 14, 2026

- Backlog: P-FSYNC-1. Desk assembly: board supervisor run 332
  worker (desk only -- no install, no MANIFEST edit, no live
  contact, no live pulls; G1 slots blank for filing day).
- Lint: 10445 B / max line 104 / non-ASCII 0
- Candidate filename note: the candidate carries the lineage
  suffix: ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar;
  its plugin.yml version is 0.1.6-lookup-prefilter-OFFLINE.

## Why

Diagnosis: ~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md
section 4. Every HistoryLedger save ends by fsync-ing the
containing directory (FileChannel.open(directory) + force()).
Legal on Linux (the live server); impossible via Java NIO on
Windows, where the open throws AccessDeniedException -- so on the
PC test-server mirror, history recovery failed ("History recovery
unavailable; archive left untouched") while history/history.mmh2
sat present and intact. In 0.1.6 the single directory-force site
(HistoryLedger.forceDirectory, called from the constructor
recovery path and both checkpoint publishes) becomes best-effort:
an IOException from the directory step is caught and the save
proceeds on the file-level fsync, which runs before it and stays
mandatory. No message or log-line changes at all; both mirror
ERROR lines disappear on Windows only because recovery now
succeeds. Linux behaviour is unchanged.

## Candidate

- Artifact: ~/workspace/makersmark-fsync-fix-20261114/ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar
  -- 209,158 B / SHA-256
  ba4e6bdedf422a8cd8e35bf4bbdf15f58d93ef6103091009c0c9b536c57d9423
- Tests: 44/44 (40 carried recorded tests, unmodified + 4 new
  HistoryLedgerFsyncTest checks; the new suite fails 2/4 against
  the original throwing body). The 4 new tests also pass with the
  candidate jar itself on the classpath. Detail: TEST-RESULTS.md
  in the build folder.
- Build method (lineage): surgical patch FROM the shipped 0.1.5
  jar itself (patch.py, hash-asserted input), swapping in the
  freshly compiled HistoryLedger class set. Changed jar entries:
  exactly 3 -- HistoryLedger.class, plugin.yml, CHANGELOG.md;
  every other byte is a shipped byte by construction. Double
  patch run byte-identical. Gate B lineage proof: the unmodified
  tree's HistoryLedger.java recompiles byte-identical to all 10
  shipped HistoryLedger entries. Gate A (full-jar reproduction of
  0.1.5 via the original route) is NOT reproducible in this VM
  (the composed Magic jar is absent) -- recorded honestly in
  BUILD-RECORD.md; the patch-from-shipped method is the
  mitigation.
- Base (desk lineage only -- NOT a live premise): shipped 0.1.5
  jar 208,757 B / SHA-256
  5cb6a71771f1bde24eafa5d7ac8ce7a4f046de4da79445012ed6db3ef1104319
  (inputs/ copies in the build folder, hash-verified).
- Source: NO source zip exists for this lineage build. The source
  of record is the ExcelMakersMark/ tree in the build folder +
  HistoryLedger.java.diff + patch.py (hashes in SHA256SUMS
  there). A records-deposit source zip assembled from that tree
  is OWED at close-out (see Close-out record).
- Rig acceptance: MET Nov 14, 2026 (mirror boot acceptance,
  ~/workspace/fsync-mirror-proof-20261114/PROOF.md): signatures
  S6/S7 ("History recovery unavailable", "Configured shared Magic
  history is unavailable") each 1 -> 0, AccessDeniedException 0
  in the full boot log, SEVERE 0, enable line "Enabling
  ExcelMakersMark v0.1.6-lookup-prefilter-OFFLINE" present, ending
  "enabled (signing on, stats on, Magic check on, tips via
  built-in)". Mirror evidence, not a live premise.

## Live-premise receipts (G1) -- BLANK, fill at filing

RULE: every slot below is filled ONLY from a same-day live pull at
filing time (rclone pull via the PC route; if the PC route fails
its probes -- 3 attempts over ~2 minutes -- a panel File Manager
pull labeled FALLBACK (panel)). Disk lineage, build folders, and
older records are NEVER a premise source. The live jar FILENAME
in particular must come from the pull: lineage filenames on the
mirror and in older records differ, so no filename is asserted
here. Full receipts go in
~/workspace/makersmark-fsync-fix-20261114/pull-g1/RECEIPTS.md
(path + size + SHA-256 + pull timestamp per slot), cited from the
MANIFEST stub by path + hash. If any slot cannot be filled, STOP:
do not file.

- L1 live jar: plugins/<LIVE JAR FILENAME: ______> = <SIZE: ______>
  B / SHA-256 <______> -- receipt: <______>
- L2 exactly-one-jar census: live plugins/ listing (command:
  `ls plugins/ | grep -i makersmark`) shows exactly ONE loadable
  ExcelMakersMark jar: <LISTING / COUNT: ______> -- receipt: <______>
- L3 live history file: plugins/ExcelMakersMark/history/history.mmh2
  present: <SIZE: ______> B / SHA-256 <______> -- receipt: <______>

## Live-jar gate (STOP on mismatch)

At the window, re-pull the live jar fresh and compare to slot L1.
STOP on any mismatch of filename, size, or SHA-256, and STOP if
the census (slot L2, re-run) shows anything other than exactly one
loadable ExcelMakersMark jar. Do not proceed on this packet's text
alone: the gate values live in the G1 receipts, not here.

## Scope

- JAR ONLY. Place the candidate under its own filename:
  plugins/ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar.
  Hold the predecessor (the live jar named in slot L1) aside in
  plugins/ as <live jar filename>.bak-20261114-pre016. After the
  swap the census must again show exactly one loadable
  ExcelMakersMark jar.
- NOT changed: no config, data, or player files. The live
  plugins/ExcelMakersMark/ folder (history archive, marks data)
  is untouched. Linux behaviour is unchanged except that a
  refused directory sync no longer fails a history save.

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
  ba4e6bdedf422a8cd8e35bf4bbdf15f58d93ef6103091009c0c9b536c57d9423
  (command: `sha256sum ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar`);
  the placed jar is pull-back verified to the same hash after
  placement.

## Swap steps

1. Gates above; fill and check G1 slots L1-L3.
2. Rename the live jar (slot L1) to
   <live jar filename>.bak-20261114-pre016.
3. Place ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar
   (hash per Hash STOP); re-run the census: exactly one loadable
   ExcelMakersMark jar.
4. Start; run Boot verify. On any failure, Rollback.

## Boot verify (by script output, from latest.log)

- Boot Done line present: `grep -c "Done (" latest.log` >= 1.
- Enable line:
  `grep "Enabling ExcelMakersMark v0.1.6" latest.log` = 1 hit.
- `grep -c "History recovery unavailable" latest.log` = 0.
- `grep -c "Configured shared Magic history is unavailable" latest.log`
  = 0.
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
plugins/ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar,
rename <live jar filename>.bak-20261114-pre016 back to the slot
L1 filename, start, and verify: Enabling ExcelMakersMark v0.1.5,
history-recovery ERROR count noted (expected 0 on Linux), SEVERE
0 attributable. The .bak stays in plugins/ until the live proof
passes, then is removed by a later cleanup entry.

## Same-pass surfaces -- OWED, named

A player-facing close-out is not done until each surface below is
done in the same pass or carried here as owed (they are all OWED
at filing; none rides this packet's jar swap):

- What's New, website surface: 0.1.6 entry or an explicit
  no-change determination, recorded in the close-out.
- What's New, in-game surface (ExcelWhatsNew changelog): same
  entry / determination as the website surface -- both or neither.
- Guide: knowledge + intents check against 0.1.6 (no command or
  mechanic changes; expected no-change, but the check is owed and
  its result recorded).
- Website pair: index + ai-knowledge ExcelMakersMark row catch-up
  to 0.1.6 (row version), via the normal website leg (G2
  duplicate guard + claim line before any push; G3 Pages deploy
  check after any merge).
- Broadcaster tips: checked; result recorded.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked; result
  recorded.

## Proof owed after install

A live marks / history flow by Mike or a family player (award or
lookup that writes history) completing with history recovery
clean in the live log. Discharged only by the tester's own
report. Until it passes, the predecessor .bak stays in place and
the surfaces close-out stays open.

## Marker on install

~/workspace/makersmark-fsync-fix-20261114/INSTALLED.txt
(+ OUTCOME.txt in the same folder: window, lane, backup name +
prunees, gate results, boot verify counts, end state).

## Close-out record (at conversion)

The MANIFEST keeps a one-line tombstone; the full record is
appended to ~/workspace/wrv-install-queue/MANIFEST-ARCHIVE.md.
Required lines: Publication claim per G2 for each repo published
(wrv-plugins records draft; winkreyesvalley website) with the
pr-exists.sh duplicate-check timestamp + result; Drive deposits
(jar + source ids, or "none") -- NOTE: this lineage has no source
zip yet; assemble the records source zip from the build folder's
ExcelMakersMark/ tree + HistoryLedger.java.diff + patch.py at
close-out and record its size + SHA-256 here; any
baseline-strength hash labeled "baseline <date>" per G4 (none is
used as a premise here).
