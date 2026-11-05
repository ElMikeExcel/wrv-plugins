# ExcelEliteLadder 0.2.1 — staged record package

Source-side record for the 0.2.1 build (Nov 5, 2026), mirroring the
0.2.0 package layout. Contents:

- `BUILD-RECORD.md` — what/when/hash/staging scope (start here)
- `CHANGELOG-0.2.1.md` — the release notes (speed feature + retuned
  default table + the two build-time confirmations)
- `CHANGELOG-0.2.0.md` — the previous release's notes (carried)
- `TEST-REPORT.md` / `TEST-RESULTS.txt` — 272 checks, 0 failed; rig
  evidence summary incl. the honest proof-shape note
- `SOURCE.md` — provenance and the exact functional delta from 0.2.0
- `DEPENDENCIES.md` — Paper API, SnakeYAML, and EliteMobs 10.9.12
  (compile-only API) provenance
- `INSTALL-PACKET.md` — the combined gated window (canonical copy in
  `~/workspace/eliteladder-retune-20261105/INSTALL-PACKET-BUNDLE.md`)
- `src/`, `tests/` — full source and suite (incl. the retuned fixtures)
- `build.sh`, `run-tests.sh`, `config.yml`, `ladder.yml`, `plugin.yml`
- `rig/drive-0.2.1.py`, `rig/evidence/` — rig driver + the three proof
  boots (fresh install, exclusion-gate proof, toggle-off boot)

The jar itself is NOT in this package (Drive/jar deposit is the
close-out lane's step at install time).
