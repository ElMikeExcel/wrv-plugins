# ExcelBedrockMenu 1.5.15 (draft, not installed)

Base: the 1.5.14 source (Drive source zip, sha 936327c7...). The untouched base rebuilds byte-identical to the shipped 1.5.14 jar (046758100b40...).

Delta (full diff in EBM-1.5.14-to-1.5.15.diff):
- New ClickStats.java: forwards each form button tap to ExcelMenuStats.recordTap by reflection. There's no compile or plugin.yml dependency. If ExcelMenuStats is missing, the tap isn't counted, and a failure logs one warning and never throws.
- GeyserForms.java: two one-line calls. The SimpleForm tap (a closed form isn't logged) and the ModalForm choice are logged just before the existing routing.
- plugin.yml 1.5.14 to 1.5.15. MenuLogicTest version pin updated, plus 6 new hook pins: 106/106 ALL PASS.
- Menus, buttons, order and routing are unchanged.

Jar SHA-256: 9204226e5b781f11d11d4152192c48a254184b3efb8711ccbf23acf7cb22fa2e (reproducible). Build: build-1515.sh <tree> 1.5.15.
Needs a restart. Boot: 'Enabling ExcelBedrockMenu v1.5.15', and no 'ExcelMenuStats hook unavailable' warning.
Pairs with ExcelMenuStats 0.2.0 (PR #47).
