# ExcelBuildMode 1.3 (Oct 16, 2026) - for Marvin's WorldEdit request
Base: PR #18 branch plugin/excelbuildmode-1.2-2026-10-14 (7617af5).
Changes: allow //cyl //hcyl //sphere //hsphere //stack and //schem (save, load, list only) in Build Mode; undo/redo already allowed (single, double slash, worldedit: forms). 8 new session nodes (worldedit.generation.cylinder/sphere, region.stack, schematic.save/load/list, clipboard.save/load) added to BuildPolicy.WORLD_EDIT_PERMISSIONS (49 total), so the outside-BuildMode check and exit-cleanup loop cover them. Excluded: schem delete/share/formats/unload, .other nodes, worldedit.*.
Statuses:
- Built: YES (jar SHA in SHA256SUMS)
- Tests: PASS (all 5 suites, see test-log.txt; 153 pilot-gate assertions incl. every new command, args, uppercase, worldedit: namespace, denial when WE off, excluded nodes)
- Scratch boot: PARTIAL. Paper 26.2 build 133 loads and enables 1.3; no WorldEdit/LuckPerms in scratch and recovery storage unprovisioned (same as 1.2 by design), so Build entry disabled. No in-game command run.
- Installed live: YES, 7:57 PM CDT Oct 16, 2026. In-game UNTESTED
- Java check: UNCONFIRMED
- Bedrock check: UNCONFIRMED
Unverified: real WorldEdit permission-node names on live WE 7.4.5 (from docs, not runtime); //schem inside Build Mode end to end; exit removes new nodes (covered by existing loop, not run live); Marvin's 5,000 block cap still limits big shapes.
Live install notes: needs panel Shut Down restart, empty server, keep ExcelBuildMode-1.2.jar as .bak. Marvin must not hold any worldedit nodes outside Build Mode (already removed).
