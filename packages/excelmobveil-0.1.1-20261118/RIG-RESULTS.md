# RIG RESULTS - ExcelMobVeil 0.1.1 (M3)

G-4 co-resident mirror boot: PASS on the Homebase mirror Nov 18, 2026 (candidate + live ExcelItems 0.1.0 jar). M3 section of ~/workspace/board-r560/G4-RECORD.md verbatim below.

---

## M3 — ExcelMobVeil 0.1.1 (boot 22:32:18, java pid 1336)

- Swap: resident `ExcelMobVeil-0.1.0.jar` → `.bak-g4`; candidate + ExcelItems placed (hashes above).
- `Done (23.325s)`; SEVERE **0**.
- Line 424: `Enabling ExcelItems v0.1.0` ×1; line 425: enabled line ×1 (4 registered / next free 5).
- Line 507: `[ExcelMobVeil] Enabling ExcelMobVeil v0.1.1` ×1 — **AFTER ExcelItems** (507 > 424).
- Line 508 (enable line at 0.1.1): `ExcelMobVeil 0.1.1: $2,000.00; each drink adds eight real-time minutes. Attacking ends all time. …`
- WARN/ERROR mentioning ExcelItems/ExcelMobVeil: **NONE**.
- RCON `excelitems status` → `4 item(s) registered, next free id 5.`
- Restore: resident 0.1.0 back at `66e7e508…` EXACT, NO RESIDUE; found-state reboot verified clean (Done 23.964s, SEVERE 0).
