# DIFF NOTE — duplicate ExcelExpansions 0.1.1 candidates (for the supervisor reconciliation)

Written by the parent-dispatched continuation lane after the stand-down
arrived. Candidate of record per BUILD-RECORD-011.md (supervisor run 330):
ExcelExpansions-0.1.1.jar 61,081 B / sha256 203464a6… — **its bytes survive
intact** at work/pkg011-A.jar and work/pkg011-B.jar (both re-hashed
203464a6… during this check). The root-level ExcelExpansions-0.1.1.jar is
currently the OTHER lane's build (61,736 B / b9d26233…, deterministic
double-build at work/pkgA.jar / work/pkgB.jar); restoring the candidate of
record is a rename from work/pkg011-A.jar. Nothing else was built or
replaced after the stand-down.

## Scope match
- Core classes (Proposer / Releaser / ReleaseService / ReleaseState,
  mtimes 02:36–02:37 CST) are the preserved originals — untouched by this
  lane. releases.yml is IDENTICAL in both builds (55,651 B / 9fa280f3…,
  regenerated from the LOCKED FINAL manifest, 57 rows, 91 content files
  across 47 rows). Both candidates implement proposer + releaser with the
  same locked watcher law; the differences are host wiring and tests.

## What this lane's tree has that the shipped candidate lacks
1. **src/test/.../ReleaseTest.java — a second, independent 40-check
   harness** (the shipped suite is one 51-check harness). Coverage the
   51 lack: §3.6 headline-slot recording (second DUE row recorded `due`,
   proposes after the tap); majority-lapse note on a standing card
   (DESIGN 2.3); proposer-level floor-of-3; flipText CRLF preservation,
   comment-only → null, non-boolean flag → null; console tokenless tap
   honoured; in-game tokenless tap rejected; NO_CONTENT outcome; seasonal
   full cycle incl. re-proposal at the NEXT window; manifest smoke
   (47 rows carry files, every path under a known host plugin).
   Combined suite on this tree: 91 PASS / 0 FAIL (TEST-RESULTS.txt).
   (Theirs-only coverage: the 51 include an end-to-end flip of the real
   catacombs_lair.yml fixture; this lane's service tests flip synthetic
   temp-dir packs.)
2. **Host wiring differences (decisions for the reconciler, not fixes):**
   - Commands: this tree has `rundown` (§3.4 re-surfacing list + pending
     cards re-delivered to Mike); the shipped host has `check` (manual
     tick) + `pending` instead.
   - Card audience: this tree delivers cards to Mike only; the shipped
     host broadcasts to all online players + console (their D4).
   - Tap auth defaults: this tree ships proposal.mike-uuids POPULATED
     with Mike's two UUIDs as keyed in EM PlayerData (Java
     7401bc22-d42a-4492-a451-374bd12d9e2c; Bedrock Floodgate
     00000000-0000-0000-0009-01feb74ea7be — both verified against the G4
     pull this session) plus an allow-op knob; the shipped config ships
     the list EMPTY with op fallback (their D2).
   - Release announcement: the shipped host broadcasts it in chat; this
     tree messages the tapper + logs only.
   - First proposer tick ~1 min after enable + an automatic follow-up
     tick ~5 s after each successful tap (this tree) vs 5 min + manual
     /check (shipped). New Year turn reading is IDENTICAL (Dec 31 +
     Jan 1, America/Chicago).
3. **build.sh on disk** builds both harnesses and the 0.1.1 jar name /
   build ID (EXC-EXPANSIONS-0.1.1-WRV-20261114-01, same ID as shipped).

## Collateral overwrites by this lane (all recoverable)
- Root ExcelExpansions-0.1.1.jar — see above; candidate bytes in work/.
- Root TEST-RESULTS.txt and SHA256SUMS now hold this lane's 0.1.1 set;
  the 0.1.0 originals are verbatim in
  ~/workspace/excelexpansions-build-20261114/ (same for the root
  plugin.yml / config.yml / releases.yml record copies, which this lane
  synced to the 0.1.1 resources). BUILD-RECORD.md was renamed, contents
  intact, to BUILD-RECORD-0.1.0-base.md. BUILD-RECORD-011.md,
  TEST-RESULTS-011.txt and SHA256SUMS-011.txt are untouched.
- src host (ExcelExpansions.java, mtime 02:46:49 CST) and resources
  config.yml / plugin.yml (02:47:06) were overwritten AFTER the shipped
  jar was built (02:46:02) — the shipped host survives only inside its
  jar. The on-disk src tree is therefore a mix and is NOT byte-identical
  to the shipped candidate's build tree.
