# WRVWandDiagnostic 0.1.0 — SOURCE (recovery record)

Recovered Nov 17, 2026 by board-manager run 486 W5 (B11 GAP-closure) from the
Google Drive deposit of Oct 26, 2026. No source existed anywhere else searched
(see the run's coverage list in W5-B11-GAPS.md): no workspace folder at any
searched depth, no wrv-plugins branch or PR in any state (246 PRs checked),
no GitHub code hit in any ElMikeExcel repo.

## Chain of custody (all hashes verified Nov 17, 2026, download-back)

- Drive file `WRVWandDiagnostic-0.1.0-source-20261026.tar.gz`
  id `1tfkNJNOGdEHRvLwgiuMrnFnd4KiLRdLa`, 17,562 B,
  SHA-256 `d841e24cf3c79c7db96a4d47ea094241622984904fbe47673e8ce960671feba4`,
  Drive-modified 2026-10-26T22:53:22 CDT. This package's
  `WRVWandDiagnostic-0.1.0-source-20261026.tar.gz` is that file, byte-exact.
- Drive file `WRVWandDiagnostic-010-CANDIDATE-20261026.zip`
  id `1579r67AW-0ONTqRYH8RKPMrayF_gMAiE`, 34,685 B,
  SHA-256 `8ce05215a9b715ffaa8c140c1c7582863191a72067c42d5244d73dfb3402234c`.
  Its internal `SHA256SUMS.txt` (kept here as `CANDIDATE-SHA256SUMS.txt`)
  lists the source tar at `d841e24c…` and the candidate jar at
  `a234c934c4e7da1a1f8788d7336081c59705b955d4f9139ac95aea11967d34f1` —
  both verified byte-exact against the extracted files Nov 17, 2026.
- The extracted tree in this package (`src/`, `test/`, `probe/`,
  `plugin.yml`, `config.yml`, `build.sh`, `test.sh`, `run_smoke.py`,
  `README-CANDIDATE.md`, `build-result.txt`, `test-result.txt`,
  `smoke-result.json`, `smoke.log`) comes from that tar, unmodified.
- Candidate jar kept here as `WRVWandDiagnostic-0.1.0-offline.jar`
  (13,858 B / `a234c934…`, per `build-result.txt` built from this source).

## Identity check

`plugin.yml`: name `WRVWandDiagnostic`, version `0.1.0-offline-diagnostic`,
main `com.winkreyes.wanddiagnostic.WandDiagnostic` — the version string
matches the live server's boot line exactly (see INSTALL-RECORD.md).
