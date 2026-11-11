# FINGERPRINTS — ExcelBuildMode 1.3.4

ExcelBuildMode predates the house fingerprint standard
(`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`) and carries
no in-jar build-id layer: no `excel-build.txt`, no BuildInfo constant
exists in the 1.0–1.3.2 lineage, and neither 1.3.3 build retrofitted
one; 1.3.4 — the combined rebuild of those two — follows the same
precedent rather than inventing a marker mid-collision. Provenance
for this artifact is therefore the record chain, stated plainly
rather than dressed up:

- plugin.yml inside the jar declares version '1.3.4' (harness-pinned
  by AdminTests + ReviewedTests against the shipped resources) and
  registers BOTH delta nodes: `excelbuildmode.reviewed` (default
  false) and `excelbuildmode.admin` (default op).
- The startup banner in the server log reads "ExcelBuildMode 1.3.4
  PILOT:" with the 1.3.3 console-admin line AND the 1.3.4
  combined-rebuild line (rig-verified in `rig-1.3.4/boot.log`).
- Artifact identity: `ExcelBuildMode-1.3.4.jar` — 99,073 B —
  SHA-256 39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3,
  reproduced by a clean-room double-build and by a rebuild from the
  shipped source zip (SOURCE.md / BUILD-RECORD.md).
- Recovery identity: the reviewed half of this jar is not a
  re-implementation — a control build of the recovered delta alone
  on the genuine 1.3.2 tree matches the preserved live reviewed
  jar's two differing classes with zero normalized-javap diff
  (REVIEWED-DELTA-RECOVERY.md; control diffs in recovery-evidence/
  are 0 bytes).
- Semantic markers carried from the lineage: the admin audit line an
  `admin off` writes to the server log — "BuildMode admin off by
  <CONSOLE|operator> for <player> (<uuid>): <outcome>." — greppable
  as an operator act, distinct from the player/join/sweep recovery
  lines.
