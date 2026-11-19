# RIG RESULTS - ExcelShops 1.3.11 (M4)

G-4 co-resident mirror boot: PASS on the Homebase mirror Nov 18, 2026 (candidate + live ExcelItems 0.1.0 jar). M4 section of ~/workspace/board-r560/G4-RECORD.md verbatim below.

---

## M4 — ExcelShops 1.3.11 (boot 22:33:49, java pid 8292)

- Swap: resident `ExcelShops-1.3.9.jar` → `.bak-g4`; candidate + ExcelItems placed (hashes above).
- `Done (23.121s)`; SEVERE **0**.
- Line 368: `Enabling ExcelItems v0.1.0` ×1; line 369: enabled line ×1 (4 registered / next free 5).
- Line 427: `[ExcelShops] Enabling ExcelShops v1.3.11` ×1 — **AFTER ExcelItems** (427 > 368).
- Line 428: `ExcelShops data check: EXISTING_TARGET`; line 429: **`ExcelShops purchases available.`** present; line 430: enabled line clean.
- `Mobile shop descriptions unavailable` count: **0**.
- WARN/ERROR mentioning ExcelItems/ExcelShops: none (the only WARN lines in any boot, found-state included, are two pre-existing ExcelWhatsNew changelog-length warnings unrelated to these plugins).
- RCON `excelitems status` → `4 item(s) registered, next free id 5.`
- Restore: resident 1.3.9 back at `9473f1ed…` EXACT, NO RESIDUE.
