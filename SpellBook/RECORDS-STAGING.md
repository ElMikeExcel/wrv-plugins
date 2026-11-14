# RECORDS STAGING — SpellBook regroup fix (config, 14 files)
Staged: 2026-11-14 (board supervisor run 334, re-dispatch W4). Desk only — no pushes, no MANIFEST edit.
Install: INSTALLED Nov 14, 2026, 03:33 CST, wrv-install-failsafe, PC lane (OUTCOME.txt; full record in MANIFEST-ARCHIVE.md).
Decision: YES — config-only fixes DO get a wrv-plugins records package.
Precedent evidence: ~/workspace/records-catchup-20261103/JOBS.md row 27 — branch record/magic-spell-packets-20261103, "Draft source record: Magic spell packets #1-#3 + Familiar/Cat cooldown config (Nov 2-3, 2026)", package zips/magic-spell-packets-20261103.zip, 43 files — a config-only record (upstream Magic plugin, spells/cooldown config only, per JOBS.md note) packaged as an open draft record PR in wrv-plugins. Corroborating: Magic stage files record PR #91 in the same census.
Package: ~/workspace/spellbook-fix-20261114/records-package/
File count: 38 files total (36 covered by SHA256SUMS + SHA256SUMS + RECORDS-STAGING.md staged at package root in the fix folder — see note). Contents: INSTALL-PACKET.md, RECEIPTS.md, OUTCOME.txt, INSTALLED.txt, STAGING-RECORD.md, VERIFY-OUTPUT.txt, PULLS-REPORT.md, SOURCE.md, diffs/ (14), placed-files/ (14 placed files: magic paths/wands 2, magicgate stage3/4/5 paths/wands 6, spells live+stage2-5 5, wandbars/spellbook.yml 1 — spellbook.yml 2,698 B / 756c5e3a63fbafd01348fc5ee7d031ccb3b2c22e819ab63c190a913a5e415db1 matches OUTCOME headline).
sha256sum -c result (in records-package/): 36/36 OK (exit 0).
G2 duplicate guard: ~/workspace/workflow-tools/pr-exists.sh wrv-plugins spellbook — exit code 0 — output verbatim: NO MATCH: no open/draft PR in ElMikeExcel/wrv-plugins matches 'spellbook'
Publication claim: wrv-plugins PR #___ (duplicate check run 2026-11-14T10:34:49+00:00, result NO MATCH) — no PR exists; claim completes at publication.
Note: this RECORDS-STAGING.md lives in ~/workspace/spellbook-fix-20261114/ per task deliverable path; the package-internal staging copy is SOURCE.md + SHA256SUMS inside records-package/.
