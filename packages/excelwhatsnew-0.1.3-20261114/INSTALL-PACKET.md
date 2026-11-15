# INSTALL PACKET - ExcelWhatsNew 0.1.3 (gated jar swap)

- Lint: 7866 B / max line 60 / non-ASCII 0
- Filed by: board supervisor run 335, lane W6 (filing prep),
  Nov 14, 2026. Supervisor files the MANIFEST stub.
- Packet folder: ~/workspace/whatsnew-013-build-20261114/
- Receipts: pull-g1/RECEIPTS.md - 5,586 B /
  a0d961f7ab2760d9ad323b47e756544581cc0301fd40cc2a934
  fdbcabe46b3d5 (same-day G1 pulls, Nov 14, 2026).
- Backlog: P-WHATSNEW-1 (Mike, Nov 14, 2026, ~12:47 AM CST).

## Candidate

- Artifact: ~/workspace/whatsnew-013-build-20261114/
  ExcelWhatsNew-0.1.3.jar
- Size / SHA-256: 41,154 B /
  9c153d3ab4d5024f3f29d409fe2c657be2ccaa576c2b6743ed
  2633e9267baafb
- plugin.yml inside reads version 0.1.3. Deterministic
  double-build byte-identical (BUILD-RECORD.md).
- Tests: 149 passed / 0 failed / 9 skipped (unit);
  desk smoke on the 100-entry changelog SEVERE 0.
- Rig: run 335 W2 PASS - mirror boots SEVERE 0; Java
  29/29 effective, Bedrock 27/27 (By-date oldest-first
  + N/M on both platforms; Oct 26 walks 1/10 to 10/10).
  RIG-REPORT.md 25,527 B / e5310ea563da4c8831c2728be
  6edf2d65c8913db7c17eece34b42872 in
  ~/workspace/whatsnew-summons-rig-20261114/.

## Live-premise receipts (G1 - summary; full in RECEIPTS.md)

- Live jar: plugins/ExcelWhatsNew-0.1.2.jar = 40,535 B /
  f67e8f71...581 (Receipt WN-1). EXACT vs base gate.
- Census: 476 entries, 87 loadable jars; loadable
  ExcelWhatsNew jars = exactly 1 (WN-2).
- Live config: plugins/ExcelWhatsNew/config.yml =
  1,722 B / b3cf8a1f...023, show-count 10 (WN-3).
- Live changelog: 86,183 B / 0bfcb17f...2cc, 100
  entries, newest 2026-11-13-excelwhatsnew-bydate;
  players.yml 1,097 B / 7f73a363...fcc (WN-4).
- GuildGate state 1,015 B / 79e44702...4c0: unlocked
  false, 4 of 5 broken, guild LOCKED (WN-5).

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelWhatsNew-0.1.2.jar must equal
  40,535 B / f67e8f717b02000207d4b9e51a4176ef09243b0
  a1baabe2affc7cd96406e581 at the window (fresh re-pull).
  Any mismatch: STOP, do not swap, report.

## Scope

- SWAP one jar: plugins/ExcelWhatsNew-0.1.2.jar out
  (held aside, see Rollback), ExcelWhatsNew-0.1.3.jar in.
- Behavior delta (only): inside a By-date DAY, entries
  walk oldest -> newest; a day tap opens the day's
  OLDEST entry; every day-walk page shows N/M
  (1/10 ... 10/10) on Java + Bedrock; the typed
  /whatsnew bydate <date> <n> index follows the same
  order (1 = oldest). Flat list Older/Newer top-10,
  months/days order, join announce, seen logic: UNCHANGED.
- CONFIG: NO placement. The live config.yml stays
  byte-untouched. The candidate jar-embedded config
  (1,784 B / d57d4211...a99) is a jar DEFAULT only and
  must never overwrite live: it carries show-count 3
  where live is 10, and 0.1.3 needs no new keys (a
  missing newer-button key uses the built-in default
  'Newer news'). See Receipt WN-3.
- DOES NOT change: changelog.yml, players.yml, any
  other jar, config, or data file. The What's New
  entry for this release is a separate TEXT filing
  (see Same-pass surfaces), not part of this packet.

## Gates

- WINDOW-IN-PROGRESS marker discipline, with marker
  re-checks immediately before Shut Down and before
  Start. A foreign marker under 3 hours old = abort.
- BACKUP FIRST: ONE fresh backup at window open +
  cap-at-6 (name prunees in the outcome record; never
  delete the Daily Backup newest copy or the two
  newest manual restore points).
- Emptiness: list = 0 of 10 twice before the swap.
- Provenance: re-hash the staged candidate fresh at
  the window; STOP on any mismatch with Candidate.

## Install steps (failsafe)

1. Hold aside the live jar (Rollback) and place
   plugins/ExcelWhatsNew-0.1.3.jar.
2. Start the server; run Boot verify below.
3. Write the marker (below) only after Boot verify
   passes. Retain all script inputs in this folder
   (install-pulls-20261114/, listing + list captures,
   post-install latest.log) per ACCEPTANCE-PREP-R333.

## Boot verify (script bundle commands - D52 / P-SCRIPT-1)

Bundle home: ~/workspace/wrv-install-queue/tools/
This packet names bundle commands from the start so
P-SCRIPT-1 acceptance can ride this landing. The lane
records one 4-line result block per step into the
outcome record (OUTCOME.txt / SCRIPT-OUTPUT.txt here).

- live_pull_check.py --remote-path
  plugins/ExcelWhatsNew-0.1.2.jar --fixture <window
  re-pull bytes> --size 40535 --sha256 f67e8f71...581
  (full hash in the Live-jar gate line): live gate.
  Also per G1 pull at the window (config, changelog,
  GuildGate state) against RECEIPTS.md values.
- backup_cap_check.py --fixture <captured Backups
  listing> --max-total 6: post-backup total <= 6.
- emptiness_check.py --fixture <list capture 1> and
  again on <list capture 2> --expected 0: both PASS.
- verify_hash.py --file <placed pull-back jar>
  --size 41154 --sha256 9c153d3a...afb (full hash in
  Candidate): placed jar equals the candidate.
- verify_hash.py --file <post-boot pull-back of
  plugins/ExcelWhatsNew/config.yml> --size 1722
  --sha256 b3cf8a1f...023: live config byte-untouched
  by the swap (scope proof, Receipt WN-3).
- boot_log_scan.py --file <post-install latest.log>
  --expected-severe 0 --guild-locked "4 of 5":
  Done present, SEVERE 0, guild LOCKED 4 of 5
  (native post-fix phrasing, FIX-REPORT-R333).
- verify_counts.py --file <post-install latest.log>
  --pattern "Enabling ExcelWhatsNew v0.1.3"
  --expected 1 --mode lines: enable line + version.
- verify_counts.py --file <post-install latest.log>
  --pattern "ExcelWhatsNew 0.1.3:" --expected 1
  --mode lines: summary line (entry count equals the
  live changelog count at the window; 100 at filing).
- verify_marker.py --file <marker path below>:
  marker present after Boot verify passes.

Expected boot lines (verbatim shapes):
- "Enabling ExcelWhatsNew v0.1.3" - exactly 1.
- "ExcelWhatsNew 0.1.3: <N> news entries (newest
  <id>), <P> players on file, <W> warnings" - the
  long-line warnings class is pre-existing, benign.
- Paper "Done (" line present; SEVERE 0 attributable;
  guild LOCKED 4 of 5 unchanged.

## Rollback

- Held-aside predecessor: at the window, rename the
  live jar to ExcelWhatsNew-0.1.2.jar.held-wn013
  (or copy it aside with its hash recorded) BEFORE
  placing the candidate; it is never deleted in
  this window.
- Rollback: stop the server, remove
  ExcelWhatsNew-0.1.3.jar, restore the held-aside
  jar to plugins/ExcelWhatsNew-0.1.2.jar, restart,
  re-run Boot verify expecting v0.1.2 lines.
- No config or data restore is needed: none was
  touched (config equality is verified above).

## Same-pass surfaces (owed at install; supervisor chain)

- What's New (in-game): the release entry itself -
  draft record in BUILD-RECORD.md (id
  2026-11-14-excelwhatsnew-day-walk; re-date id,
  posted, game date + site group together if the
  install lands on a later date) - filed as a
  separate PENDING (TEXT - LIVE-EDIT EXEMPTION)
  entry at the install pass, never before.
- What's New (website) + website pair: generated
  from the same registry record via the whatsnew
  pipeline; website PR merge + byte-verify + G3
  Pages deploy check at that leg.
- Guide: CHECKED at build (BUILD-RECORD) - no new
  Q&A needed; the existing By-date month -> day
  coverage does not change shape. Re-check at the
  surfaces pass and record NO CHANGE or the delta.
- Broadcaster tips: CHECKED NO CHANGE (no tip names
  the day-walk direction). Menus: CHECKED NO CHANGE.
- Records: wrv-plugins OPEN DRAFT record PR (G2
  duplicate guard first, claim line in close-out)
  + Drive jar / source deposit.

## Proof owed after install

- Mike's glance (player proof, Mike only): open
  What's New, By date, Oct 26 - the day opens at
  entry 1/10 and stepping walks to 10/10.
  Until then this item stays PROOF-OWED.

## Marker on install

- ~/workspace/whatsnew-013-build-20261114/INSTALLED.txt
