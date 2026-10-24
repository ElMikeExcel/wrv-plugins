# ExcelMenuStats 0.2.2 (draft, not installed)

Read-only click counter for ExcelMenu buttons, plus Bedrock form taps from ExcelBedrockMenu 1.5.15+.

- Log: `plugins/ExcelMenuStats/clicks.csv` with `timestamp,player,uuid,platform,menu,button,slot,click` (America/Chicago; ExcelShops 1.3.5 purchases.csv style).
- Java chest menus: button key from `plugins/ExcelMenu/menus/*.yml` (slot to item key; same-slot variants split by shown name).
- Bedrock forms: ExcelBedrockMenu calls the public `recordTap(Player, menu, button, index, platform)` by reflection. Rows have platform=bedrock and click=FORM, and the menu/button are the form title and button label.
- Platform from the Floodgate API.
- Admin: `/menustats [days]` (default 7, top 15) and `/menustats reload`. Permission `excelmenustats.admin` (op).
- Never cancels or changes a click. Writes on its own thread.

0.2.2 jar SHA-256: `602829524a2cf938be993e8d6cb667829d94042ab1f8289948fb45bf05692448` (0.1.0, 0.2.0 and 0.2.1 were never installed and are superseded).
Install needs a restart. Boot line: `[MenuStats] READY version=0.2.2 menus=18 failed=0`.

## 0.2.1: Java / Bedrock / Total, merged rows
- Output: `N. server_menu > shop: Java 30 | Bedrock 12 | Total 42 (5 players)`, plus the same split on the summary line. Rows are sorted by total.
- Merge: a Bedrock tap is mapped to the Java `menu_id > button_key` when its form title matches an ExcelMenu menu title (colour codes ignored) and its button label matches an item name. A prefix match is allowed too (e.g. "Auto-open on join"). Same-name variants (guild_locked/guild_open) go to the first in file order.
- Overrides: `plugins/ExcelMenuStats/bedrock-map.txt`, with lines like `Form Title > Button Label: menu_id > button_key`, then `/menustats reload`.
- Bedrock-only buttons (e.g. Teleport to a player) keep their readable `Form Title > Label` row.
- Pairs unchanged with ExcelBedrockMenu 1.5.15 (PR #48): same `recordTap` signature.

## 0.2.2: ExcelMenu 0.1.4 confirm screen
Clicks on the Java "Confirm purchase?" chest (ExcelMenu 0.1.4 ConfirmHolder, PR #49) are logged as `menuId > itemId:confirm` (slot 11) or `:cancel` (slot 15), e.g. `spawnshop_cosmetics_colors > pink:confirm`. The values are read from the holder's menuId()/itemId() by reflection. The preview slot isn't counted. Without 0.1.4 nothing changes.
