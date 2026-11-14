# RECORDS STAGING - ExcelBuildMode 1.3.5
Staged: 2026-11-14 (board supervisor run 336, desk worker). Desk only - no pushes, no PRs, no MANIFEST edit, no Drive upload. Publication is a separate supervisor browser leg.
Install: INSTALLED Nov 14, 2026, 06:03 CST chained failsafe window (see INSTALLED.txt / OUTCOME.txt).
Package: ~/workspace/buildmode-fsync-fix-20261114/records-package/
File count: 43 files total (41 covered by SHA256SUMS + SHA256SUMS + this file). Contents: BUILD-RECORD.md, INSTALL-PACKET.md, INSTALLED.txt, OUTCOME.txt, TEST-RESULTS.md, RECEIPTS.md (G1 pull), SOURCE.md, README.md, build.sh, fsync-fix.diff, sources zip, src/ (main + test trees); sources zip ExcelBuildMode-1.3.5-source.zip (145,180 B / c8be342cca0a51cabcfc2badee125b88d66f1b4e0b3191e22c4101e1483b8c9b, 49 entries).
Jar: NOT in package per house rule - referenced by hash in SOURCE.md; Drive deposit carries it (publication leg).
sha256sum -c result: 41/41 OK (exit 0).
G2 duplicate guard: ~/workspace/workflow-tools/pr-exists.sh wrv-plugins - broad slug 'excelbuildmode' exit 3 - matches are PRIOR-version record PRs only (#180 [DRAFT] record/excelbuildmode-1.3.4-20261110); version fragment 'excelbuildmode-1.3.5' exit 0 NO MATCH. No open/draft PR exists for THIS version; the publication leg creates a new draft record PR (it must not reuse a prior-version PR).
Publication claim: wrv-plugins PR #___ (duplicate check run 2026-11-14T12:44:08+00:00, result NO MATCH for this version) - no PR exists; claim completes at publication.
