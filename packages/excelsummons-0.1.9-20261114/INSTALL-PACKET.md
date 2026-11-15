# INSTALL PACKET - ExcelSummons 0.1.9 seq-02 (gated jar swap)

- Lint: 9244 B / max line 61 / non-ASCII 0
- Filed by: board supervisor run 335, lane W6 (filing prep),
  Nov 14, 2026. Supervisor files the MANIFEST stub.
- Packet folder: ~/workspace/summons-cap-build-20261114/
- Receipts: pull-g1/RECEIPTS.md - 5,902 B /
  70186570a23b3e1ff727e2e4c91a8337e2eb448ac6281b31d88
  d895e80093578 (same-day G1 pulls, Nov 14, 2026).
- Backlog: P-SUMMONS-CAP-1 (Mike, Nov 14, 2026,
  ~12:48 AM CST - CHANGES his locked Nov 10 cap rule).

## Candidate

- Artifact: ~/workspace/summons-cap-build-20261114/
  ExcelSummons-0.1.9.jar - the seq-02 candidate of
  record per the run-249 reconciliation.
- Size / SHA-256: 102,260 B /
  ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90
  df115759e6df46
- Build: EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114
  (plugin.yml reads 0.1.9; excel-build.txt matches).
- BARRED: the jar in summons-019-rebuild-20261114/
  (102,034 B, parent Lane C tree) is NOT the candidate.
  Never place it. STOP if the staged jar hash differs
  from ead3beec... above. This packet supersedes any
  stale FILING-PREP naming that jar.
- Tests: 107/107 PASS (104 carried + 3 cap-fix);
  deterministic double-build byte-identical;
  base gate: the unmodified seq-01 tree rebuilt
  byte-identical before any edit (TEST-RESULTS.md).
- Rig: run 335 W2 PASS - harness 12/12 run against
  the mirror-PLACED seq-02 jar (adopted pets hold no
  cap slot and no mana; cast counts bind the 1/2/3/5
  ladder; refusal lists cast only; per-spell + mount
  rules unchanged); boot H Done (21.235s), SEVERE 0.
  RIG-REPORT.md 25,527 B / e5310ea563da4c8831c2728be
  6edf2d65c8913db7c17eece34b42872 in
  ~/workspace/whatsnew-summons-rig-20261114/.
  The 0.1.9 tree also carries seq-01 pet names +
  rename unchanged (desk-tested; live proof owed).

## Live-premise receipts (G1 - summary; full in RECEIPTS.md)

- Live jar: plugins/ExcelSummons-0.1.8.jar = 92,387 B /
  835419aa...841 (Receipt S-1). Live version proven
  0.1.8 from this pull (enable line, S-5).
- Census: 476 entries, 87 loadable jars; loadable
  ExcelSummons jars = exactly 1 (S-2).
- Live config: plugins/ExcelSummons/config.yml =
  5,878 B / 659d9e2c...3a9; stripped of comments it
  is value-identical to the candidate config (S-3).
- Live data: summons-data.yml = 54,284 B /
  4c1538a9...f78, header build 0.1.8 (S-4).
- GuildGate state 1,015 B / 79e44702...4c0: unlocked
  false, 4 of 5 broken, guild LOCKED (S-5).

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelSummons-0.1.8.jar must equal
  92,387 B / 835419aaeff434b61e6be2f5cda1340233299e1
  1262feebf96206cdd00bb8841 at the window (fresh
  re-pull). Any mismatch: STOP, do not swap, report.

## Scope

- SWAP one jar: plugins/ExcelSummons-0.1.8.jar out
  (held aside, see Rollback), ExcelSummons-0.1.9.jar in.
- Behavior delta (cap fix, P-SUMMONS-CAP-1): the
  rank-scaled total summon cap (Beginner 1 / Student
  2 / Apprentice 3 / Master 5) counts CAST-origin
  summons only. Adopted records (tamed/bred
  originals) occupy NO cap slot and hold NO mana;
  the refusal message's count + name list name cast
  summons only. Carried in the same jar (seq-01,
  unchanged by seq-02): pet names in menus/forms +
  in-menu Rename for adopted pets.
- UNCHANGED by design: per-spell family caps, the
  combat server ceiling, mount exclusion from the
  total count, all dismiss scopes (typed dismiss
  never touches adopted; menu per-pet confirm stays
  the only adopted release), rank ladder + thresholds.
- CONFIG + DATA: NO placement, NO edits. Live
  config.yml (S-3) and summons-data.yml (S-4) stay
  byte-untouched by the install steps; the candidate
  config delta is comment text only. The seq-02 jar
  booted read-clean over mirror 0.1.5-era data on
  the rig (config + data hashes unchanged after
  that boot). Data drifts with normal play - no
  post-boot data hash equality is claimed.

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
   plugins/ExcelSummons-0.1.9.jar.
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
  plugins/ExcelSummons-0.1.8.jar --fixture <window
  re-pull bytes> --size 92387 --sha256 835419aa...841
  (full hash in the Live-jar gate line): live gate.
  Also per G1 pull at the window (config, GuildGate
  state) against RECEIPTS.md values.
- backup_cap_check.py --fixture <captured Backups
  listing> --max-total 6: post-backup total <= 6.
- emptiness_check.py --fixture <list capture 1> and
  again on <list capture 2> --expected 0: both PASS.
- verify_hash.py --file <placed pull-back jar>
  --size 102260 --sha256 ead3beec...f46 (full hash
  in Candidate): placed jar equals the candidate.
- verify_hash.py --file <post-boot pull-back of
  plugins/ExcelSummons/config.yml> --size 5878
  --sha256 659d9e2c...3a9: live config byte-untouched
  by the swap (scope proof, Receipt S-3).
- boot_log_scan.py --file <post-install latest.log>
  --expected-severe 0 --guild-locked "4 of 5":
  Done present, SEVERE 0, guild LOCKED 4 of 5
  (native post-fix phrasing, FIX-REPORT-R333).
- verify_counts.py --file <post-install latest.log>
  --pattern "Enabling ExcelSummons v0.1.9"
  --expected 1 --mode lines: enable line + version.
- verify_counts.py --file <post-install latest.log>
  --pattern "EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114"
  --expected 1 --mode lines: seq-02 build id in the
  enabled line (this also proves the barred jar was
  not placed - its build id differs).
- verify_marker.py --file <marker path below>:
  marker present after Boot verify passes.

Expected boot lines (verbatim shapes):
- "Enabling ExcelSummons v0.1.9" - exactly 1.
- "ExcelSummons 0.1.9 enabled (build
  EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114);
  registry holds <N> summon(s)." - N is live state.
- Paper "Done (" line present; SEVERE 0 attributable;
  guild LOCKED 4 of 5 unchanged.

## Rollback

- Held-aside predecessor: at the window, rename the
  live jar to ExcelSummons-0.1.8.jar.held-sum019
  (or copy it aside with its hash recorded) BEFORE
  placing the candidate; it is never deleted in
  this window.
- Rollback: stop the server, remove
  ExcelSummons-0.1.9.jar, restore the held-aside
  jar to plugins/ExcelSummons-0.1.8.jar, restart,
  re-run Boot verify expecting v0.1.8 lines.
- No config or data restore is needed: none was
  touched (config equality is verified above);
  the 0.1.9 jar writes the same data format the
  rig read cleanly (registry header build id will
  read 0.1.9 after live use - expected, harmless).

## Same-pass surfaces (owed at install; supervisor chain)

- What's New (in-game) + (website): ONE registry
  record covering pet names + rename (seq-01) AND
  the cap rule in player words ("adopted pets do
  not count toward your summon limit - only summons
  you cast do"); draft wording in
  ~/workspace/summons019-surfaces-20261113/
  SURFACES-PREP.md, re-dated + group seq recomputed
  at filing; in-game changelog via the pipeline,
  filed as a separate PENDING (TEXT - LIVE-EDIT
  EXEMPTION) entry at the install pass.
- Guide: knowledge regen + intents amendment
  (summons-rules) per the surfaces prep.
- Website pair: index.html + ai-knowledge.json,
  incl. the ExcelSummons plugin-row catch-up to
  0.1.9; website PR merge + byte-verify + G3
  Pages deploy check at that leg.
- Broadcaster tips: CHECK at the surfaces pass.
  Menus: VERIFY-ONLY (the 0.1.7 summons door in
  plugins/ExcelMenu/entries/summons.yml stays live).
- Records: wrv-plugins OPEN DRAFT record PR (G2
  duplicate guard first, claim line in close-out)
  + Drive deposit (jar Drive-only, never GitHub).
- FEEDBACK-TRACKER row added when this packet files
  (per the seq-01 BUILD-RECORD).

## Proof owed after install

- Mike casts (player proof, Mike only): with his
  8 adopted wolves out, casts work up to his cast
  cap (3 at Apprentice) and the refusal at the cap
  names cast summons only.
- Mike's pack proof (seq-01 carry-over): the pack
  shows distinct names; each Dismiss confirm names
  its own wolf; a menu rename shows over the
  wolf's head. Until these land this item stays
  PROOF-OWED.

## Marker on install

- ~/workspace/summons-cap-build-20261114/INSTALLED.txt
