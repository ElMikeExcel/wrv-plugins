# ExcelTrash 0.1.0 — INSTALL-RECORD (BUILT, NOT INSTALLED)
Built Oct 23, 2026 ~4:47 PM CDT on the box. Mike approved the build; install waits for an empty-server window (new plugin = restart).
| Artifact | Size | SHA-256 |
|---|---|---|
| ExcelTrash-0.1.0.jar | 13806 B | 9d5c6a30d8c325e9e905c2c781ce41817209262f661ba281e9eeca6e4be6570f |
| ExcelTrash-0.1.0-source.zip | 9077 B | 42b53e29e1d4053c9b512c58110a2113aa0437f171644e1b9ba9daf8ccebe55b |
Toolchain: OpenJDK 25.0.4.1, javac --release 21 (same as ExcelShops), Paper API 26.2.build.133 (api-version 1.21). Rebuild x2 byte-identical.
Tests: 8/8 JUnit (store one-shot, replace, 5-min expiry, restart persistence incl. no-dupe after take, empty-close ignore, purge, slot policy).
Rig: Paper 26.2 scratch boot clean — "ExcelTrash 0.1.0 enabled", Done 12.776s, zero SEVERE. Not tested in-game (no player client on the box).
## Install (empty server only)
1. Upload jar to /plugins/. 2. Restart. 3. Console: look for "ExcelTrash 0.1.0 enabled". 4. Player test Java + Bedrock: trash a dirt stack, /trash undo, button recover, shift-click, drag into bottom row.
Rollback: delete jar (+ plugins/ExcelTrash/ folder) and restart.
## Menu entry (ExcelMenu, live reload)
Add to a free slot in a compass menu file under plugins/ExcelMenu (DeluxeMenus-style item, same as gq_bosstrial):
  trash:
    material: LIME_DYE ... (see MENU-ENTRY.yml)
then /excelmenu reload (proven live Oct 23 11:11 AM, "18 menus, 0 failed"). Do this only after the jar is installed.
ExcelBedrockMenu: catalogue is compiled code (Big Brain 1252), so a Trash button there = rebuild + restart; ExcelMenu renders Bedrock forms from the same menu definitions, so likely unnecessary.
