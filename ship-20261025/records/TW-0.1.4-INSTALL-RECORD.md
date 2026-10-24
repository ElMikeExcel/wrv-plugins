# ExcelTrainingWheels 0.1.4: lifesteal on kill (STAGED, not live)

Asked by Mike Oct 24, 2026, 8:33 AM CDT: "Give training wheel protocol some lifesteal when they kill
something so they have passive sustain. Just a little bit. Maybe a kill gives 1 or 2 hearts".
Built + rig-tested on box Paper 26.3-214, 8:36-8:43 AM CDT. NOT installed live. Jar swap = restart.

Jar:    ExcelTrainingWheels-0.1.4.jar  41,141 B  sha256 4d9ca503546ea7ad69843f6b6e376de18ef824e3a8062b4933dfe4292f48bfda
Source: ExcelTrainingWheels-0.1.4-source.zip  sha256 936e51d60f5dfae15435773d09833a2bec9c71d02ebc45135c26df1c23babb59
Base:   0.1.3 source (/tmp/tw = trainingwheels-0.1.3/ExcelTrainingWheels-0.1.3-source.zip, identical); a rebuild
        of that source reproduces the staged/live 0.1.3 jar byte-for-byte (42b27d01...), so the diff is the whole change.
Build:  JDK=/home/box/tools/jdk-25.0.5+8 LIBS=/workspace/build-libs-wrv/paper1.21.4 RT=/workspace/build-libs-wrv/runtime-libraries ./build.sh
        (deterministic: two builds byte-identical). Also compiles clean against paper-api 26.3.build.214 + Adventure 5.2.

## What it does
- Only players on the TW protected list. Killing a hostile mob (Bukkit Enemy) = +2 HP (1 heart).
  Killing an EliteMobs (MagmaGuy) elite = +4 HP (2 hearts). Elite detection = same as 0.1.3
  (elitemobs:eliteentity PDC tag, then the EliteMobs PersistentTagger seam). EliteMobs itself untouched.
- Capped at max health (MAX_HEALTH attribute). Nothing at full health.
- Nothing for killing players, passive animals, or mobs from a spawner cage (spawn reason SPAWNER).
  Trial spawner mobs DO count. Natural mob farms (dark rooms) can't be told apart and DO count.
- Kill credit = vanilla getKiller(): melee and the player's own arrows count; environment kills do not.
- EntityDeathEvent at MONITOR, ignoreCancelled. Heal is a direct setHealth (no EntityRegainHealthEvent).
- Action bar: small green "+1 heart" / "+2 hearts" / "+0.5 heart" (actual amount after the cap).
  NOTE: TW's original design says nothing is ever shown to a protected player. This bar is the first
  visible TW effect. Set lifesteal.show-actionbar: false to keep TW fully invisible.
- debug: true logs LIFESTEAL lines, e.g.
  "LIFESTEAL <player>: killed ZOMBIE (hostile) +2.0 HP 10.0 -> 12.0 (max 20.0, configured +2.0)".

## Install (server stopped, at a batch restart)
1. plugins/: rename ExcelTrainingWheels-0.1.3.jar -> ExcelTrainingWheels-0.1.3.jar.bak-20261024.
   If that name exists, do NOT overwrite it: use .bak-20261024-tw014.
2. Upload ExcelTrainingWheels-0.1.4.jar. Check sha256 above.
3. Live config: DO NOT replace plugins/ExcelTrainingWheels/config.yml (it holds the protected list and
   currently debug: true; its backup is config.yml.bak-20261024-debug - leave that file alone).
   Missing lifesteal keys = defaults (ON, 2 HP, 4 HP, action bar ON) - verified on the rig with a
   live-shaped config. Optional, to make it editable: back up the config first
   (config.yml.bak-20261024-tw014, never overwrite an existing .bak), then append at top level:
     lifesteal:
       enabled: true
       mob-hp: 2.0
       elite-hp: 4.0
       show-actionbar: true
   Values clamp to 0..20 HP.

## Boot line
  [ExcelTrainingWheels] lifesteal on kill: ON (hostile +2.0 HP, elite +4.0 HP, spawner mobs excluded, action bar ON)
  /tw reload message now includes "lifesteal ON (...)". No SEVERE.

## Rollback
  Delete 0.1.4 jar, rename the .bak back to ExcelTrainingWheels-0.1.3.jar, restart.
  Off with no restart: lifesteal.enabled: false + /tw reload.

## Check 1 (build + tests): PASS
  run-tests.sh 272/272 (was 241; +31 LifestealRuleTest: who earns it, caps, NaN/negative, labels). tests-output.txt.
  Rig /workspace/tw014/rig (copy of the 0.1.3 rig, live-shaped config with no lifesteal keys), bots via mineflayer
  (rig-run2.txt + rig-run2-caseI.txt): 12/12
   A zombie melee 10->12 | B elite-tagged zombie 10->14 | C cap 19->20 (+1) | D full 20->20 | E cow 10->10
   F skeleton 10->12 | G arrow owned by the protected player, kill credited, 10->12 | H unprotected killer: no heal
   I protected player kills a player: no heal (victim confirmed dead) | J spawner-cage zombie: no heal ("spawner mob" log)
   K enabled:false + /tw reload: no heal | L mob-hp 3.0 + show-actionbar false via reload: 10->13, no bar
   Action bar packet with "heart" text received in A/B/C/F/G, none in D/L. 0 SEVERE, 0 exceptions.
## Check 2 (separate diff review): PASS
  Priority MONITOR + ignoreCancelled (cancelled deaths pay nothing; event not modified). Null killer -> return.
  Projectile kills via getKiller (rig G). Dead/invalid killer -> no heal. Cap from the attribute, falls back to 20.
  Player victims excluded before elite check. Spawn-reason read guarded. Debug-only logging.
  Not covered: a real EliteMobs elite (rig has no EliteMobs; PDC tag path tested, seam path same code as 0.1.3).
