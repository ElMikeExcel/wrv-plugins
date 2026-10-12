# BedrockMenu
/menu that opens native Geyser forms on Bedrock and a chest GUI on Java.
1.4.2 buttons: Spawn, Teleport to a player, Spawn Shop (`dm open spawnshop`), Jobs & Money, mcMMO Stats, Leaderboard, Server Info, Auto-open on join. No Home/Set Home.

## History
- 1.2.x: Argus. 1.3.0: Steve, teleport menu (history/1.3.0-teleport-source, Maven, compiled against Geyser API). 1.4.0/1.4.1: Argus, teleport flow ported to reflection-based Geyser forms (no Geyser API compile dep), Java 25 bytecode. 1.4.2: Steve, ASM patch on 1.4.1 classes.

## Layout
- classes-1.4.1/ – Argus's compiled 1.4.1 (the only original artifact; Argus's .java was never on the box).
- decompiled-1.4.1/ – CFR 0.152 decompile, for reading/porting. Not guaranteed to recompile cleanly. If you edit code, start here (or ask Argus for the real source) and turn it into a proper project.
- tools/Patch.java + tools/plugin-1.4.2.yml – how 1.4.2 was made.
- build.sh – reproduces BedrockMenu-1.4.2.jar byte-identical to the staged jar (verified).
