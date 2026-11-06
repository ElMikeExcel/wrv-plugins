# INSTALL PACKET — ExcelCosmetics 0.1.3 (STAGED Nov 6, 2026)

State: BUILT + TESTED + STAGED. Not installed. Install rides the failsafe empty-server window.

- Jar: `ExcelCosmetics-0.1.3.jar` in this folder — 82,562 B, SHA-256 e18d5af59657ca44f0d2747e62ffe09a71e2056e32225f069ccc9e0b0ad3162b
- Source: `ExcelCosmetics-0.1.3-source.tar.gz` — 45,123 B, SHA-256 a1ebbc881ad67e9b94c0d09539af853f7dd5fcde4c7bab236a13d71095ada957
- Tests: 316 checks, 0 failed (285 baseline + 31 new TestCompensation). Deterministic double-build byte-identical.
- Fix: snapshot+compensate in grantAndPersist (false ⇒ nothing changed) + atomic players.yml save. Full record: ExcelCosmetics-0.1.3-SOURCE.md in this folder.

## Pre-swap gate (packet time)
1. Re-pull live `plugins/ExcelCosmetics-0.1.2.jar` from the panel; SHA-256 must equal 45fc556ca0f84f77209faa1fb13a540f39e5a31248b0b75467d6d5586636509b before swapping. If it differs, STOP and reconcile.
2. BACKUP FIRST (Shockbyte), cap-at-6 house rule (prune oldest manuals, name prunees, keep newest Daily + two newest manual restore points).
3. Emptiness gate: console `list` 0 players twice, once more before Shut Down.

## Swap
Replace plugins/ExcelCosmetics-0.1.2.jar with ExcelCosmetics-0.1.3.jar (keep the .bak rollback naming used by prior windows). No config/data changes — players.yml format unchanged.

## Boot verify
- Done line, zero SEVERE attributable to ExcelCosmetics.
- Enable line family: "ExcelCosmetics enabled: 13 colors, 7 effects, 5 sets, emote pack ready; economy linked via Vault".
- Final `list` check; server Online.

## Player proof (post-install, player-proof class)
One real purchase per shape (rent + own) on the live server. Forced-save-failure cases stay in the rig/tests, never in production.
