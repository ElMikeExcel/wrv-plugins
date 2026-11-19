# RIG RESULTS - ExcelDeathProtection 0.2.1 (M2)

G-4 co-resident mirror boot: PASS on the Homebase mirror Nov 18, 2026 (candidate + live ExcelItems 0.1.0 jar). M2 section of ~/workspace/board-r560/G4-RECORD.md verbatim below.

---

## M2 — ExcelDeathProtection 0.2.1 (boot 22:30:38, java pid 4768)

- Swap: resident `ExcelDeathProtection-0.1.3.jar` → `.bak-g4`; candidate + ExcelItems placed (hashes above).
- `Done (23.793s)`; SEVERE **0**.
- Line 197: `[ExcelItems] Enabling ExcelItems v0.1.0` ×1; line 198: `ExcelItems enabled: 4 custom item(s) registered, next free id 5.` ×1.
- Line 232: `[ExcelDeathProtection] Enabling ExcelDeathProtection v0.2.1` ×1 — **AFTER ExcelItems** (232 > 197).
- Line 233 (registry line, present as owed): `ExcelItems registry: premium id 3 (deathpotion) mint delegated to ExcelItemsAPI.create(3); basic id 2 (deathprotection_basic) ACTIVATION - display stack only, no giveable item.`
- Line 234: version/build line — `basic $15,000.00, premium $30,000.00 (build EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e)`; line 235: `ExcelDeathProtection API: premium-credit seam active (creditPremium)`.
- WARN/ERROR lines mentioning ExcelItems/ExcelDeathProtection: **NONE**.
- RCON `excelitems status` → `ExcelItems: 4 item(s) registered, next free id 5.`; `excelitems list` → IDs 1–4 (veil_potion / deathprotection_basic ACTIVATION / deathpotion / revive_potion).
- Restore: resident 0.1.3 back at `f0224d18…` EXACT, candidate + ExcelItems jar + ExcelItems data folder removed, NO RESIDUE; found-state reboot verified clean (Done 23.810s, SEVERE 0).
