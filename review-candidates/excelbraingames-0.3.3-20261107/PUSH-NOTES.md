# PUSH NOTES — ExcelBrainGames 0.3.3 records publication

For the parent / board-manager (the builder subagent stages locally
only; connector writes belong to the parent lane per house routing).

## Staged and complete (builder)

- This package: `~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.3-20261107/`
  (FILE-LIST.txt + SHA256SUMS inside verify it).
- Build folder: `~/workspace/brain-games/build-0.3.3/` (jar + records
  + FILE-LIST/SHA256SUMS).
- MANIFEST: the gated PENDING entry for the install is filed by the
  builder (points at the build folder's packet + jar; its live gate
  names the 0.3.2 jar — 130,850 B, SHA-256
  8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511 —
  as the required predecessor).

## Legs for the parent / board-manager

1. **GitHub — draft record PR in `wrv-plugins`**, mirroring the
   excelbraingames-0.3.2 package's route (0.3.2's package shape is
   this package's shape): push this package's text files under the
   records path for ExcelBrainGames 0.3.3. The connector is UTF-8
   text only — the jar does NOT go through it.
2. **Drive — jar + source deposit** in the shared Minecraft WRV
   folder, byte-verified after upload: `ExcelBrainGames-0.3.3.jar`
   (135,705 B, SHA-256 64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1)
   alongside this package (source of record per SOURCE.md).
3. **Install** runs off the build folder via the MANIFEST entry +
   INSTALL-PACKET.md (gated jar swap ONLY — config.yml is
   byte-identical to 0.3.2's, so there is NO config merge).
   Same-pass surfaces owed at install are listed in the packet
   (What's New registry → both surfaces; website pair index.html +
   ai-knowledge.json full regen; Guide chunk; tips check).
4. After the install lands, flip this package's state in the records
   (the packet's INSTALLED marker lands at
   `~/workspace/brain-games/build-0.3.3/INSTALLED.txt`).

Recording work is pre-approved standard workflow (Mike, Nov 4, 2026)
— report as done, never as waiting on Mike.
