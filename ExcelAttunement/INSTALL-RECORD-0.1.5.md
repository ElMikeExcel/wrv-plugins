# ExcelAttunement 0.1.5 install (STAGED, NOT LIVE)

Status: built + tested on the box Oct 24, 2026 about 1:15 AM CDT. Mike said "Yes, stage it that way".
Our plugin only. Magic jar and EliteMobs untouched (EliteMobs is only read, by reflection).
Supersedes 0.1.4 (includes everything in it). Install INSTEAD of 0.1.4 at magic-heal STEPS.md step 4b.
Jar swap = one server restart, in the empty-server window, BEFORE `magic load`.

## What 0.1.5 adds: Magic is off in the EliteMobs worlds ("the guild ward")
- Warded world = EliteMobs registry says so (EliteMobsWorld.isEliteMobsWorld, read-only, by reflection;
  verified present in EM 10.9.8, live is 10.9.10) OR the name starts with em_ OR the name is listed
  (em_adventurers_guild, GoblinKingdom_vf). Case-insensitive. The registry can only ADD a world.
- In a warded world: every Magic cast is cancelled (PreCastEvent, heals too) and the wand will not
  open (WandPreActivateEvent). Message: "The guild's halls are warded. Magic falls silent inside."
  In chat + action bar on arrival; on blocked tries action bar only, max once per 10 s.
- On arrival (world change, join, respawn): stance and weapon/wand lock cleared, running Magic spells
  stopped and the wand closed, Resistance / Absorption / Regeneration with 15 s or less left removed
  (longer potions and golden apples kept; infinite effects kept).
- Attunement is ignored there: no stance, no lock, no 0-damage. EliteMobs owns combat in the guild.
- Magic teleports INTO a warded world are refused (Recall, world-change, warp spells: any teleport with
  Magic's classes on the call stack). Message: "The guild's wards turn your spell aside."
  Normal teleports (EliteMobs portals, /spawn, commands) are not touched.
- Admin bypass permission: excelattunement.ward.bypass (default false, ops do NOT get it automatically).
- Off switch: magic-ward.enabled: false in config.yml (restart). No config edit needed to install:
  missing keys use the defaults above.
- New command: /attunement ward [world] (status, counters).

## Artifact
- ExcelAttunement-0.1.5.jar  49364 bytes
  SHA-256 69d4afc53a0b7687e65a699266833d41961cf44ec199cf40e57a5c2d1c57d6b8 (two consecutive builds byte-identical)
- ExcelAttunement-0.1.5-source.zip  SHA-256 cf7894f086a8239af78d5b38368721f452346047142f0ade7d58f0d0b83b9c9b
- rollback/ExcelAttunement-0.1.4.jar (626d565c...), rollback/ExcelAttunement-0.1.3.jar (a650d630...)
- Check: `cd /workspace/batch-next/magic-heal/excelattunement-0.1.5 && sha256sum -c SHA256SUMS`

## Install (live paths)
1. plugins/ExcelAttunement-0.1.2.jar -> rename to ExcelAttunement-0.1.2.jar.bak-20261024 (never overwrite a .bak; add -2).
2. plugins/ExcelAttunement/config.yml -> copy to config.yml.bak-20261024-ea015 (kept as-is; no edit needed).
3. Upload ExcelAttunement-0.1.5.jar to plugins/. Only ONE ExcelAttunement jar may be in plugins/.
4. Restart (empty server). Console must show:
   "ExcelAttunement enabled: ON, window 30s, combat context 10s, spell table 105 spells."
   "Stance lock: weapon-lock ON, wand-lock ON, actionbar ON (live ON), sounds ON."
   "Guild ward: EliteMobs world registry found (read-only), plus the world-name rules."
   "Guild ward (Magic off in the EliteMobs worlds): ON."
   If it says "registry not found", the ward still works by world name (em_ prefix + list).
5. Console: attunement ward em_adventurers_guild -> "This world (em_adventurers_guild): ..." not NONE,
   and "Warded worlds loaded:" lists the EM worlds.
6. Proof (owed, live): in the guild, right click a wand = no bar + the ward line; outside, the wand works.

## Rollback
Delete ExcelAttunement-0.1.5.jar, upload rollback/ExcelAttunement-0.1.4.jar (or restore the 0.1.2 .bak),
restart. Fast off without a jar swap: magic-ward.enabled: false + restart.

## Tests
- Plain-JVM suites: 465 passed, 0 failed (379 from 0.1.4 + 86 ward rules, worlds, notice, strip pins).
- Display + lock pins: 0 failed.
- Box rig (Paper 26.3-214, Magic 11.2.5, test worlds em_adventurers_guild, GoblinKingdom_vf,
  em_id_test_dungeon_3, plain_test): 23 of 23 PASS (RIG-TEST.txt, script rig-test-t_ward.js).
- Full-batch boot (heal kit combined + 0.1.5 + TW 0.1.3 + Together 0.4.0 + CoreProtect): 0 SEVERE.
- NOT verified: real EliteMobs worlds/registry on a live boot (EM not on the rig), Bedrock client.

## Known behavior (not changed, document only)
- Boss fights (dragon, elder) outside the guild: TAKING hostile damage refreshes an active stance.
  A player who heals once in combat can stay in caster stance (weapons locked, 0.1.4+) for the whole
  fight while the boss keeps hitting them. Decide before the fight who heals. Possible later knob:
  "incoming damage does not refresh caster stance".
- Magic re-tries opening a held wand every tick, so the "wand opens blocked" counter in
  /attunement ward climbs fast. Harmless (cheap cancel, debug log limited to once per 10 s).
