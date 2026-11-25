# excelprotect-0.1.13-20261124 — SOURCE record

ExcelProtect 0.1.13 "Event Time", built and installed Nov 24, 2026.

- **What it is:** the Protect half of diagnosis seam B (spec: excelprotect-invisible-heal-20261124/DIAGNOSIS.md §5 fix 2). Held CUSTOM/MAGIC_REGEN heals were stamped with flush time instead of the original event time in the combat ledger, so the nightly visibility audit scored flush-crossing heals invisible; pending heals were also dropped on plugin disable. 0.1.13 writes the original event time and flushes pending heals on disable. Exactly 2 production files changed (full detail: BUILD-RECORD.md in this package).
- **Provenance:** build folder of record = `~/workspace/excelprotect-eventts-20261124/build-tree/` (fleet workspace). Base = the live 0.1.12 tree (board-r432), base-gate verified: an unmodified rebuild reproduced the live 0.1.12 jar byte-identical before the 0.1.13 work began.
- **This package's source tree** (src/ + tests/ + bundled ymls + build script) was verified byte-exact (full recursive diff) against the build tree of record at records time, Nov 24, 2026.
- **Install of record:** wrv-install-failsafe-20261124-0434, Nov 24, 2026 — placed ExcelProtect-0.1.13.jar 305,079 B / e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28; boot Done 04:41:18 CT, READY version=0.1.13, SEVERE 0, config untouched. Full record: INSTALLED.txt (copied into this package).
- **Records note:** this package was published Nov 24, 2026 by the fleet-audit Lane 4 backlog clearance (item A14) — the release was installed Nov 24 but its record PR + Drive deposits were owed; jar NOT in git (Drive only, hash-referenced in FINGERPRINTS.md).
- Publication claim: PENDING (filled by amend push after the PR number exists)
