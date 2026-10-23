# ExcelMenuStats 0.1.0 (draft, not installed)

Read-only companion to ExcelMenu. Logs every button click in an ExcelMenu chest menu to
`plugins/ExcelMenuStats/clicks.csv` and shows the most-used buttons.

- CSV: `timestamp,player,uuid,platform,menu,button,slot,click` (timestamps America/Chicago, same style as ExcelShops 1.3.5 `purchases.csv`).
- Button key comes from `plugins/ExcelMenu/menus/*.yml` (slot -> item key; view_requirement variants on one slot are told apart by the shown name).
- Platform: Floodgate API when present (Java / Bedrock).
- Admin: `/menustats [days]` (default 7, top 15) and `/menustats reload` after menu edits. Permission `excelmenustats.admin` (op).
- Never cancels or changes a click. Writes on its own thread.
- Limit: Bedrock form taps served by ExcelBedrockMenu are not chest-inventory clicks and are not counted.

Build: `local-build.sh` (JDK 25, --release 21, Paper API). SHA-256 of the jar:
`ded469e3bad46163672504df807bb723438a4c61b97c4397d2c8a6eff74b0780`
Install: drop the jar in plugins/ at the next empty-server restart. Needs a restart (no hot-load).
