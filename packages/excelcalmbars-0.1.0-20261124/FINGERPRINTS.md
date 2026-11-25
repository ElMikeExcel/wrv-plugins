# excelcalmbars-0.1.0-20261124 — FINGERPRINTS

| Artifact | Bytes | SHA-256 | MD5 |
|---|---|---|---|
| ExcelCalmBars-0.1.0.jar (installed; Drive deposit, NOT in git) | 19,006 | 111794a81c830f6886a5fe5881d358ccfc0387b83ab2d7a58c0ca6f877c899e0 | 85a8d10c6d283319ed64532bd54dcaeb |
| config.yml (bundled = deploy config, placed as plugins/ExcelCalmBars/config.yml) | 2,868 | dede5592eded53bfa2f045d712c06d6d577aad80fd9d416abc383f5dd741a8f4 | 06833bc50df4c18c9cb0e7ccb63c4777 |
| ExcelCalmBars-0.1.0-source.zip (Drive deposit; 11 files: src + tests + bundled ymls + build.sh + README.md, fixed timestamps) | 55,086 | d1652940526a36593abe3d0026f07ca258f2305a5b1aa16cb6c7b1bbb7a25b00 | 349d2a0aee836a8b7e8efd23654692f1 |

Gates verified at install (INSTALLED.txt): live absence premise re-verified twice in the window (fresh 608-entry positive listing of live plugins/, grep calmbars = 0; PC-side verify-staged.ps1 CALMBARS_LIVE_COUNT 0); staged jar + config hashes verified ON the PC before the swap and again on the placed files after boot — all exact. No outgoing jar existed (new plugin; rollback = delete jar + ExcelCalmBars folder). Window backup snapshot-11242026.1926.

Tests: pattern unit test 22/22 checks on the final sources ("ALL PATTERN CHECKS PASSED"); mirror proof cases (a)–(e) all PASS (RIG-RESULTS.md). Jar deterministic double-build byte-identical.
