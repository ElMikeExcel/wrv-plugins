# BUILD RECORD — ExcelExpansions 0.1.1 (tap-card proposer + releaser)

Built Nov 14, 2026 (desk worker, board supervisor run 330 — RE-DISPATCH of
run 329 W8, which timed out in its resolve phase after the base gate).
Desk only: no install, no MANIFEST edit, no live contact.
Design: ~/workspace/excelexpansions-design-20261111/DESIGN.md (§3 proposal
flow, §4.2 seasonal, §5 release flip, §7.6/7.8/7.12 mechanics).
Gap source: ~/workspace/expansion-rig-20261114/RIG-REPORT.md Part B-3/B-4
(0.1.0 watcher core PASSED the mirror rig; in-chat tap-card and the
plugin releaser/flip were NOT BUILT in 0.1.0).

## Base gate (re-verified this run, not inherited)
- Pristine 0.1.0 tree (~/workspace/excelexpansions-build-20261114) rebuilt
  in /tmp/base010: jar SHA-256 3a36001dde845164a5619f4e637ca3b85e8839a510d98a14475962fe3e7f0b55
  — byte-identical to the 0.1.0 build of record in this folder; its suite
  ran 31/31 PASS during that rebuild.
- The verified 0.1.0 artifacts in this folder are untouched:
  ExcelExpansions-0.1.0.jar still hashes 3a36001d…b55 (re-checked after
  the 0.1.1 builds); BUILD-RECORD.md / TEST-RESULTS.txt / SHA256SUMS
  (0.1.0 set) unmodified.

## Candidate jar
- ExcelExpansions-0.1.1.jar — 61,081 B / sha256 203464a63d39c6c96fde7cac21ce0fcaeadf57c9b7db457056bbaca0ac7f3e7c
- Build ID: EXC-EXPANSIONS-0.1.1-WRV-20261114-01 (excel-build.txt in jar)
- Deterministic double-build: work/pkg011-A.jar == work/pkg011-B.jar ==
  candidate (cmp clean, byte-identical)
- Jar entries: 36 (0.1.0 set + core Proposer / ReleaseState /
  ReleaseService / Releaser classes); jar read-back: plugin.yml version
  0.1.1, releases.yml inside the jar = 57 rows.

## Data — FINAL manifest (state: FINAL, not draft)
- src/main/resources/releases.yml was REGENERATED from
  ~/workspace/em-storyline-20261113/EXCELEXPANSIONS-MANIFEST-FINAL.md
  (38,932 B / d51e0111…) by the resumed worker's work/gen_releases.py:
  header names the FINAL manifest; watcher fields identical to the draft
  (per the rig report's data-swap note); FINAL prose carried (lock
  header, #16 wording, #10 seasonal lock wording on M-30..M-33).
- New per-row field: content_files — each row's content_package paths
  relative to plugins/, cross-checked by the generator against the G4
  pull parsed_content_packages.csv (every listed file exists in the
  pull). Rows with no captured content (M-33, M-37, M-39, M-40) carry an
  empty list and can never flip. releases.yml = 55,651 B / sha256 in
  SHA256SUMS-011.txt.

## What was built (the two missing mechanisms)
1. **In-chat tap-card proposal (DESIGN §3)** — core/Proposer.java (pure
   logic) + host rendering in ExcelExpansions.java. The watcher tick
   (first pass 5 min after enable, then watcher.cadence-hours, plus
   /excelexpansions check) evaluates all rows; each NEW due episode
   produces exactly ONE proposal, posted in Minecraft chat as an
   Adventure click card ([ TAP GO ] runs the tap command) with the pack
   name/phase, band + band source text, the watcher's reason (in-band
   counts), fixed-band urgency note, and band note — plus the same tap
   command as a plain typed line for Bedrock (no click events). Rules:
   untapped stays due SILENTLY (a proposed row never re-posts; a lapsed
   majority is only noted on its state entry, the card stands — §2.3);
   one non-seasonal headline in flight at a time (a second DUE row is
   recorded `due`, proposed when the tap lands — §3.6); seasonal rows
   are independent and fire on their LOCKED window only; a released
   seasonal pack out of window gets ONE retirement proposal (§4.2).
   On Mike's join, pending cards are re-delivered with their stored
   token (delivery of the same episode, not a new proposal).
2. **Releaser / instant flip (DESIGN §5 step 1)** — core/Releaser.java +
   core/ReleaseService.java + core/ReleaseState.java. A tap is honoured
   only against a live proposal, only from Mike (see decision D2), and
   only with the episode's token (console may omit it; a wrong token is
   rejected from anyone — the stale-card guard, §7.12). The flip sets
   the anchored column-0 `isEnabled:` flag in each of the row's
   content-package files — the exact mechanism the W7 rig proved by
   hand on the mirror (catacombs_lair.yml false→true, next boot clean).
   Per changed file: sibling backup `<file>.bak-excelexpansions`,
   temp-file + atomic move; already-at-target is idempotent; a missing
   file or missing anchored flag fails the flip as a whole and the row
   stays proposed (never half-released). Success advances the persistent
   release-state overlay (release-state.txt: state, due/proposed/
   released timestamps, released-by "Mike tap <date>", token, note) and
   broadcasts the release announcement in chat; the released entry's
   note names the same-pass surfaces as owed to the supervisor (Guide +
   What's New both + website + menus — not plugin-writable), per Mike's
   player-explanation rule. Double-tap returns ALREADY_RELEASED /
   ALREADY_RETIRED and flips nothing. Retirement tap is the mirror flip
   (isEnabled → false, state retired-seasonal).
   Commands: /excelexpansions [status|list|store|check|pending|tap].

## Tests — 51 PASS / 0 FAIL (TEST-RESULTS-011.txt)
- All 31 pre-existing 0.1.0 checks stay green (incl. the 4 EM-DB checks
  against the real G4 pull).
- 20 new 0.1.1 checks: content_files wiring (M-02 = 4 files; M-03 =
  catacombs_lair.yml; M-33 empty); proposer DUE → exactly one card;
  card carries typed tap command + 10-char token; proposed state records
  the same token; second tick posts nothing (silent); pendingCard
  re-delivers the same token; flipText flips the anchored flag only
  (decoy `# isEnabled:` comment untouched), already-set identity,
  no-flag → null; tap with no proposal rejected; wrong-token tap
  rejected; **tap on a fixture parked pack (catacombs_lair.yml,
  isEnabled: false) flips it to true with backup written and decoy
  comment intact**; state advances to released with the Mike-tap audit
  line; double-tap ALREADY_RELEASED; missing-file flip fails whole and
  the row stays proposed; seasonal retirement proposal + tap flips the
  fixture back to false (retired-seasonal); release-state save/load
  round-trip.

## Design decisions made (decide-and-log style; all config-reversible)
- D1 — Live enable mechanism (§7.6/7.8): the releaser is built against
  the rig-proven mechanism exactly as the task directed — the anchored
  isEnabled flip in the content-package files (W7 Part B manual proof).
  §7.8 (does EM apply a flipped flag live or only at next restart?) is
  NOT proven by anything on record, so the tap announcement does NOT
  promise instant in-world access; config.yml's releaser note states
  this plainly. Reverse/decide: a mirror rig boot after a plugin-driven
  flip settles it; wording updates in one line.
- D2 — Tap authentication (§7.12): console always; a player only if his
  UUID is in config proposal.mike-uuids — shipped EMPTY, in which case
  op players may tap (WRV is a family server where ops are Mike's
  accounts). Easier-to-reverse option: one config line locks taps to
  Mike's UUID alone; no code change.
- D3 — New Year turn (§7.13): implemented as Dec 31 + Jan 1
  (America/Chicago), the narrowest reading of "the days straddling
  Dec 31 → Jan 1" in DESIGN §4. One-line change in the host if Mike
  wants a wider turn.
- D4 — Card audience: proposals post to all online players + console
  (the DESIGN §3.1 "posts in the Minecraft Server chat" reading);
  only Mike can tap. Pending-card re-delivery on join goes to Mike only.

## Still owed (not built here, by scope)
- Mirror rig proof of the plugin-driven path: watcher tick → card →
  tap → flip on the PC test server, incl. the §7.8 live-apply question
  and EM's reaction to a flipped flag at next boot.
- Player-explanation surfaces for any actual release (supervisor's
  same-pass leg at release time; the released-state note names them).
- Dynamic watcher floor remains unset (config) — dynamic rows stay
  NOT_EVALUABLE until Mike/the build sets it; no floor invented.

## RECONCILIATION — SHA256SUMS-011 (supervisor run 330 verification worker, Nov 14, 2026)

Supervisor verification found `sha256sum -c SHA256SUMS-011.txt` passed
4/6 but FAILED on src/main/resources/config.yml (sums f47dce6a…, actual
3a2de1b3…) and src/main/resources/plugin.yml (sums 86e8e520…, actual
4be4101f…). Cause: substantive edits landed AFTER the 02:46:02 CST
packaging and the 02:46:42 sums — src config.yml, src plugin.yml and
build.sh all carry mtime 02:47:06 CST, ExcelExpansions.java 02:46:49.

What the post-packaging edits to the two src files were (diff of the
original jar's embedded copies — byte-identical to the folder-root
copies and to the sums values — against current src; SUBSTANTIVE, not
comment/whitespace-only):
- config.yml (2,685 B f47dce6a… → 2,991 B 3a2de1b3…): proposal.mike-uuids
  changed from [] (empty) to Mike's two EM-PlayerData UUIDs
  (7401bc22-d42a-4492-a451-374bd12d9e2c Java; 00000000-0000-0000-0009-
  01feb74ea7be Bedrock/Floodgate); new key proposal.allow-op: true;
  sections release-state{file} + releaser{backup-suffix} replaced by
  release{state-file} (the key the host code actually reads). This
  SUPERSEDES decision D2 above ("shipped EMPTY") — the shipped config
  now locks taps to Mike's UUIDs (op also allowed via allow-op).
- plugin.yml (609 B 86e8e520… → 740 B 4be4101f…): description rewritten
  (watcher + tap-card proposer + releaser; no longer "read-only");
  command description/usage changed from [status|list|store|check|
  pending|tap] to [status|list|store|rundown|tap <row id> <token>].
Correlated post-packaging edits found in the same wave (outside the
two files, recorded for completeness): ExcelExpansions.java (reads
release.state-file + proposal.allow-op, implements rundown); build.sh
(ReleaseTest added to the suite); ReleaseTest.java (new 40-check
suite — its first run at 02:48 CST failed 2 checks, both TEST bugs:
proposedAt compared at nanosecond precision against a milli-precision
store, and a manifest predicate that excluded the seasonal M-33
empty-content row; fixed by the builder at 02:49:56 CST, milli-
truncated t0 + corrected predicate). The builder was still iterating
when this reconciliation began; files were stable from 02:51:08 CST
and reconciliation proceeded on that final state. Folder-root
config.yml/plugin.yml copies were re-synced to src by the builder at
02:51:08 CST (hashes now equal src).

TWO-LANE DISCLOSURE (per DIFF-NOTE.md in this folder, written by the
parent-dispatched continuation lane): the post-packaging wave was a
SECOND build lane, not the original builder fixing its own tree. The
shipped candidate of record above (61,081 B / 203464a6…) is that
lane's jar — commands check+pending, broadcast cards (D4), EMPTY
mike-uuids (D2), 5-min first tick, single 51-check harness — and its
host source survives only inside its jar. The on-disk src tree is the
continuation lane's tree (rundown command, Mike-only card delivery,
populated mike-uuids + allow-op, ~1-min first tick + post-tap tick,
second 40-check harness). DIFF-NOTE.md lists the wiring differences
as decisions for the reconciler. The original candidate's bytes are
PRESERVED at work/pkg011-A.jar / work/pkg011-B.jar (both 203464a6…)
and can be restored by rename if the supervisor/Mike picks that lane.

Resolution (per this reconciliation's assigned rule — current src is
authoritative; the jar's embedded files differed in substance from
current src, so the jar was rebuilt from current src): the
continuation lane rebuilt the jar at 02:50:55 CST; this worker then
independently rebuilt deterministically from current src twice more
via build.sh — all four jars byte-identical (cmp clean):
- FINAL candidate (current-src build): ExcelExpansions-0.1.1.jar —
  61,736 B / sha256
  b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91
  (supersedes 61,081 B / 203464a6… at the root path; 203464a6… is
  preserved in work/ as above, NOT deleted).
- Jar embedded config.yml = src config.yml = 3a2de1b3… (2,991 B);
  embedded plugin.yml = src plugin.yml = 4be4101f… (740 B);
  embedded releases.yml unchanged 9fa280f3… (55,651 B); 36 entries.
- Tests (full suite, TEST-RESULTS-011.txt regenerated): WatcherTest
  RESULT pass=51 fail=0 + ReleaseTest RESULT pass=40 fail=0 — total
  91 PASS / 0 FAIL. This supersedes the "51 PASS" line above.
- SHA256SUMS-011.txt regenerated AFTER this note; `sha256sum -c`
  passes all 6 lines.
- 0.1.0 preservation: ExcelExpansions-0.1.0.jar (3a36001d…) in this
  folder untouched; the 0.1.0 build record's CONTENT is intact
  (ee68665c…) but the continuation lane renamed the file to
  BUILD-RECORD-0.1.0-base.md. NOTE: this folder's TEST-RESULTS.txt
  is build.sh's output file and was overwritten by the continuation
  lane's post-packaging runs (not by this reconciliation); the
  pristine 0.1.0 set in ~/workspace/excelexpansions-build-20261114/
  still verifies 4/4 against its SHA256SUMS (incl. TEST-RESULTS.txt
  dcb6162e…).
