# RECORDS STAGING — BB-09 / ExcelTour 1.0.1

Staged: 2026-11-15 (board supervisor run 409, records worker
W10). Recording work pre-approved (Mike, Nov 4, 2026).
Install: INSTALLED Nov 15, 2026, wrv-install-failsafe run
scheduled 05:33 CST (see INSTALLED.txt / OUTCOME.txt).
Package: ~/workspace/board-r409/tour-records/package/
(prep staging, 17 files incl. jar). GitHub package published
by W11: 16 files — the same set minus the jar (see Jar note
below), with SOURCE.md / this file / README.md / FILE-LIST.txt
adjusted for the jar-out shape and SHA256SUMS regenerated
(15 entries) in the staging folder
~/workspace/board-r409/tour-records/staging/exceltour-1.0.1-20261115/.
Contents (GitHub): source zip + README.md + SOURCE.md + this
file + FILE-LIST.txt + SHA256SUMS + byte-copies of the build /
packet / install record (BUILD-RECORD.md, TEST-REPORT.md,
FINGERPRINTS.md, SPEC-DELTA.md, INSTALL-PACKET.md,
RECEIPTS.md, SRC-DIFF-1.0.0-to-1.0.1.txt, INSTALLED.txt,
OUTCOME.txt, SCRIPT-OUTPUT.txt).

Jar: ExcelTour-1.0.1.jar — 50,908 B /
ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
— NOT in this GitHub package. The BB-01 precedent shape was
applied by the run-409 W11 publication leg: the jar is held
out of the GitHub package, the Drive deposit carries it, and
this line + SOURCE.md carry its full hash for the record.
(The W10 prep staging package on disk included the jar; the
GitHub package published from it does not.)

G2 duplicate guard (run this staging, NOT cited from an
earlier run):
- ~/workspace/workflow-tools/pr-exists.sh wrv-plugins exceltour-1.0.1
  Run: 2026-11-16T01:27:43Z–01:27:45Z (2026-11-15 19:27 CST)
  Exit: 0 — verbatim: NO MATCH: no open/draft PR in
  ElMikeExcel/wrv-plugins matches 'exceltour-1.0.1'
- ~/workspace/workflow-tools/pr-exists.sh wrv-plugins exceltour
  Run: 2026-11-16T01:27:45Z–01:27:47Z (2026-11-15 19:27 CST)
  Exit: 0 — verbatim: NO MATCH: no open/draft PR in
  ElMikeExcel/wrv-plugins matches 'exceltour'
Neither fragment exits 3, so no existing PR supersedes this
package.

Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-16T01:27:43Z, result NO MATCH for 'exceltour-1.0.1'
and NO MATCH for 'exceltour') — no PR exists; claim completes
at publication.

Proposed publication shape (supervisor leg, NOT done here):
- Repo path: packages/exceltour-1.0.1-20261115/
- Branch: record/exceltour-1.0.1-20261115
- PR title: Record: ExcelTour 1.0.1 — BB-09 back-button
  fixes (2026-11-15)
- Lane: Homebase native git (default); OPEN DRAFT, never
  merged. No push was made by this worker.
