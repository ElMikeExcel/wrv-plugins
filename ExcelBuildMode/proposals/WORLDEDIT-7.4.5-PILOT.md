# WorldEdit Tools: prepared recommendation, not installed

Use official WorldEdit for Bukkit **7.4.5**, `worldedit-bukkit-7.4.5.jar`. Its vendor release page explicitly lists Minecraft 26.2 and recommends Paper. No WorldGuard, FAWE or multiworld dependency is proposed. No hub boundary is needed for this trusted, location-undecided build.

Official release: https://dev.bukkit.org/projects/worldedit/files/8607421

## Minimum practical access

Grant only the private builder's approved `excelbuildmode.use` entry permission. WorldEdit nodes are a temporary attachment during the verified Build session, removed on exit, recovery, death and quit. Do not grant a persistent WorldEdit group or OP. The current candidate's fixed nodes are:

- worldedit.wand
- worldedit.selection.pos, worldedit.selection.hpos
- worldedit.selection.expand, worldedit.selection.contract, worldedit.selection.shift
- worldedit.region.set, worldedit.region.replace, worldedit.region.walls, worldedit.region.faces
- worldedit.clipboard.copy, worldedit.clipboard.paste, worldedit.clipboard.rotate, worldedit.clipboard.flip
- worldedit.history.undo.self, worldedit.history.redo.self

Selection plus ordinary fill/replace/shell and clipboard tools cover a practical hub workflow. The helper permits the corresponding command roots, including both single/double-slash undo/redo and WorldEdit namespaced variants. Self history does not grant another player's history; 7.4.5 source explicitly checks `.other` separately. No wildcard, schematic file/network loading or saving, scripts, navigation, brushes, region regeneration, entity removal, limit override or global administration is included.

Permissions reference: https://worldedit.enginehub.org/en/latest/permissions/
Pinned history implementation: https://github.com/EngineHub/WorldEdit/blob/7.4.5/worldedit-core/src/main/java/com/sk89q/worldedit/command/HistoryCommands.java

## Conservative initial settings

Apply the accompanying fragment into the generated 7.4.5 config only after reviewing the complete file:

- Default and maximum change limit: 5,000 blocks; no permission to raise it
- Ten history entries per session; preserve default expiration
- Radius cap 16 and brush radius 4, although these tool permissions are not granted
- Command logging enabled, normal disallowed-block list retained, symbolic links disabled

The pinned clipboard implementation checks bounding-box volume before allocating the clipboard and rejects volume greater than or equal to the active block-change limit. Therefore start with selections below 5,000 blocks for copy. Undo/redo can walk multiple history entries, so use one step at a time; ten history entries are not a ten-second or fixed-memory budget.

Clipboard permissions also expose entity/biome flags and can reproduce block-entity contents such as a stocked chest. The helper's ordinary container-open guard does not sanitize a WorldEdit clipboard. For trusted building, copy clean structures with empty containers; do not describe this setup as preventing every cross-mode item transfer. A stricter guarantee would require removing clipboard access or a separately tested clipboard-content policy.

This is a starting operational budget, not a proof of maximum CPU, chunk reads, item/entity effects or tick time. Change limits count changed blocks; a huge sparse selection or complex pattern may examine much more. Use modest rectangular selections and test large empty/sparse selections before enabling this pilot. The existing helper does not yet enforce a separate scanned-volume cap. Do not claim that changing the limit alone contains every expensive operation.

Pinned clipboard source: https://github.com/EngineHub/WorldEdit/blob/7.4.5/worldedit-core/src/main/java/com/sk89q/worldedit/command/ClipboardCommands.java
Pinned config keys: https://github.com/EngineHub/WorldEdit/blob/7.4.5/worldedit-core/src/main/java/com/sk89q/worldedit/util/YAMLConfiguration.java
Configuration guide: https://worldedit.enginehub.org/en/latest/config/

## Explicit release/approval gate

Before installation/access: obtain and hash the official JAR, test exact Paper 26.2-123/Java25 with LuckPerms5.5.87 attachment behavior, self undo/redo, clipboard limits, cancellation, quit/death/failed recovery, and operation completion before return to normal play. Confirm commands and hand selection cannot remain usable after attachment removal. Never infer claim protection or region restriction from WorldEdit being installed; this proposal intentionally has neither.

Verify a usable world/plugin/playerdata backup and available host quota before world edits. The older 19.9/21 GB quota and failed backup schedule are stale observations, not a current verified backup. CoreProtect logging/undo coverage must be tested; it does not replace a backup.

The owner must approve the exact WorldEdit and helper artifacts, reward-provider patch set, private one-builder configuration and access grant, restart/rollback plan and any claimed limits. No installation or permission command has been executed. Setting the completed hub as live spawn is a later separate approval. Same-world Creative blocks remain ordinary harvestable world blocks.
