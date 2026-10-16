# ExcelShops 1.3.1 (wording cleanup): INSTALLED LIVE Oct 16, 2026, 4:06 PM CDT. Java check UNCONFIRMED, Bedrock check UNCONFIRMED
| Status | State |
|---|---|
| Built | Yes, Oct 16, 2026 (JDK 25, release 21), source = 1.3 plus wording-only changes |
| Automated tests | 377/377 (374 from 1.3, 3 new regression tests); regression test verified to fail when the fix is removed |
| Test server (scratch boot) | Passed: Paper 26.2 build 133, ExcelShops v1.3.1 enabled, guard connected, Essentials economy. (Essentials "unsupported server version" error is the same as the 1.3 scratch boot; the /stop NPE is from piping stop to the console in the harness.) |
| Installed live | NO |
| Java check | UNCONFIRMED |
| Bedrock check | UNCONFIRMED |
| GitHub / website / version map / Big Brain records | NOT DONE (publish at install time) |
SHA-256 ExcelShops-1.3.1.jar: 7cd2c370918543dbd3d6a057e623e910ae4fafb379d540ddb15cb864a066209d
SHA-256 ExcelShops-1.3.1-source.zip: baff8957cf9d8c8da39f4922c211dd2ca11b9f3d825de147d37aad942a0b1a46
What changed (wording only; no price, quantity, 2,304 cap, or logic changes; balance lines keep cents):
- Form bodies no longer include the Java-menu button hints ("Tap to browse. No charge yet.", "Tap to choose quantity. No charge yet.", "Tap to review 1. No charge yet."). Java menu items are unchanged.
- Category form line is now "Tap an item to see its details. You are not charged until you confirm." (replaces "Choose an item to read its own details. No charge yet.")
- Detail screen keeps one pay statement: "You pay only after you tap Confirm."
- Change-quantity form: "Pick how many." (removed the repeated "You pay later, after you confirm.")
- "Purchase cost: -$40.00" is now "Purchase cost: $40.00" (Java chat panels, forms, legacy ChestShop panel). Sale lines still show "Sale proceeds: +".
- Confirm screen keeps exactly one "You pay when you tap Confirm." line (tested).
Version note: repo has two-part versions for ExcelShops (1.2, 1.3); 1.3.1 as requested is three-part (plugin.yml and pom.xml updated).
Install plan (when Mike says go): players check twice, save-all, panel Shut Down, replace jar, keep ExcelShops-1.3.jar as .bak outside plugins, start. Rollback: shut down, restore 1.3 jar (SHA a83a4d79...d68), remove 1.3.1, start.
Files: jar, source zip, ExcelShops-1.3-to-1.3.1.diff, junit-1.3.1.txt, scratch-boot-1.3.1.log, SHA256SUMS.txt. Working tree: /workspace/excelshops131/.
