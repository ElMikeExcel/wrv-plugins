# ExcelMenuStats 0.2.0 (draft, not installed)

Read-only click counter for ExcelMenu buttons, plus Bedrock form taps from ExcelBedrockMenu 1.5.15+.

- Log: `plugins/ExcelMenuStats/clicks.csv` with `timestamp,player,uuid,platform,menu,button,slot,click` (America/Chicago; ExcelShops 1.3.5 purchases.csv style).
- Java chest menus: button key from `plugins/ExcelMenu/menus/*.yml` (slot to item key; same-slot variants split by shown name).
- Bedrock forms: ExcelBedrockMenu calls the public `recordTap(Player, menu, button, index, platform)` by reflection. Rows have platform=bedrock and click=FORM, and the menu/button are the form title and button label.
- Platform from the Floodgate API.
- Admin: `/menustats [days]` (default 7, top 15) and `/menustats reload`. Permission `excelmenustats.admin` (op).
- Never cancels or changes a click. Writes on its own thread.

0.2.0 jar SHA-256: `e292c08c50757aff764457856b3290d90ca4b36ffbeba12d88bace0e4caa4d64` (0.1.0 was never installed and is superseded).
Install needs a restart. Boot line: `[MenuStats] READY version=0.2.0 menus=18 failed=0`.
