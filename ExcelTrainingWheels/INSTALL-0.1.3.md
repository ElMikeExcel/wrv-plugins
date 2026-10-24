# ExcelTrainingWheels 0.1.3: every zombie can break wooden doors (same restart as the rest of batch-next)

Approved by Mike Oct 23, 2026 11:14 PM CDT. Built and rig-tested on box Paper 26.3-214, 11:43 PM CDT. NOT installed live.
Jar: ExcelTrainingWheels-0.1.3.jar  37,916 B  sha256 42b27d0199b5d404ccd77a70c77621875af4d940bfbdf16ec803b7bfb98a0781
Source: ExcelTrainingWheels-0.1.3-source.zip (draft PR in ElMikeExcel/wrv-plugins, see STEPS.md line).

What changes: zombies, husks and zombie villagers get door-breaking when they spawn or load
(vanilla gives it to at most 10%). Drowned are never touched. EliteMobs elites are skipped.
Unchanged: the door guard (any player on the protected list), copper ~45s siege, iron ~60s siege, shield, raid compensation.

## A. Install (server stopped / at the batch restart)
1. In plugins/, rename ExcelTrainingWheels-0.1.2.jar -> ExcelTrainingWheels-0.1.2.jar.bak-20261023.
   If that .bak name already exists, do NOT overwrite it: use .bak-20261023-tw013 instead.
2. Upload ExcelTrainingWheels-0.1.3.jar to plugins/. Check sha256 matches above.
3. Keep plugins/ExcelTrainingWheels/config.yml as it is. The new key doors.all-zombies-break-doors is not
   in the live config; a missing key means ON (verified on the rig with a copy of the live config).
   Optional, only to make it visible/editable, under "doors:" next to copper-target-seconds:
     all-zombies-break-doors: true
     break-door-types: [ZOMBIE, HUSK, ZOMBIE_VILLAGER]

## B. Boot lines (console)
  ExcelTrainingWheels enabled: shield ON (N protected), door guard ON, iron siege ON (threshold 60s, cap 8), door tiers ON (copper 45s).
  all zombies break doors: ON [ZOMBIE, HUSK, ZOMBIE_VILLAGER] (EliteMobs elites left alone; N loaded zombie(s) granted at enable)
  No SEVERE from ExcelTrainingWheels.

## C. Proof (console)
  /tw reload   -> message includes "all zombies break doors ON (N granted)"
  Near any normal zombie: /data get entity @e[type=zombie,limit=1,sort=nearest] CanBreakDoors  -> 1b
  Optional in-game: close an oak door with a normal zombie chasing you on Hard -> it breaks the door
  (vanilla ~12s of banging once it starts; rig saw 15s and 25s from spawn).

## Rollback
  Delete ExcelTrainingWheels-0.1.3.jar, rename the .bak back to ExcelTrainingWheels-0.1.2.jar, restart.
  Fast off without a restart: set doors.all-zombies-break-doors: false and run /tw reload
  (stops new grants; zombies already granted keep 1b until they despawn - harmless, vanilla rules).

## Website (publish AFTER the install is confirmed, not before)
  website-draft/README.md + diffs: "Every zombie can now break doors" in the plugin row and Family protections
  paragraph; times unchanged (wood ~14s, copper ~45s, iron ~60s); bump version cell to 0.1.3.

## Rig results (rig-run6.txt, 11:35-11:43 PM CDT)
  Grant: zombie/husk/zombie villager 0b -> 1b within 2 ticks; drowned 0b; elite-tagged zombie 0b.
  Oak, unprotected target: broke at 24.8s and 15.0s after summon (3rd zombie sat at the door 45s without
    starting: vanilla door-goal start timing, our code does not touch oak breaks). Once started, a break takes ~12s.
  Oak, protected Kid (from config): held 60s, 3 DOOR GUARD cancels. Oak, Kid2 added at runtime by /tw add: held 60s, 3 cancels.
  Copper: siege start -> destroyed 46s. Iron: siege start -> destroyed 60s.
  Off via config + /tw reload: new zombie stays 0b; back on: re-sweep grants it. Restart: a 0b zombie saved while off is granted on chunk load.
  0 SEVERE, 0 exceptions. Rig note: zombies drop a player target without line of sight (closed door), so the rig
  re-set the target every 0.5s with a rig-only helper (RigTarget, never shipped) to mirror a zombie actively chasing.
