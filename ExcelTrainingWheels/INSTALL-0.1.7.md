# Install ExcelTrainingWheels 0.1.7 (parent does this; nothing here was applied)

0.1.7 stacks on 0.1.6. Install 0.1.6 first (draft PR #69) and let it boot cleanly; then 0.1.7
on a later restart. One plugin change per boot, at a verified-empty moment (two 0-player reads).

INSTALL ORDER for today's pending packets (they share ExcelTips tips.yml):
  C. Bedrock port warning   (tips.yml -> sha256 ef893938...)
  F. ClaimFly part C tip    (tips.yml -> sha256 f69e7283...)
  then this packet: TW 0.1.6 jar boot, TW 0.1.7 jar boot, then the Night Hearing tip (step 7).
The jar steps do not touch tips.yml and may go in any time after 0.1.6; only step 7 must wait for C and F.

1. Panel: confirm plugins/ExcelTrainingWheels-0.1.6.jar is the live TW jar
   (sha256 974d159fd9f4cf9b750914f57424ebe21a736a8b846746bdd6dfe5b4bf1244e4) and that the boot log
   showed "Enabling ExcelTrainingWheels v0.1.6".
2. Back up plugins/ExcelTrainingWheels/config.yml (download a copy) and keep the 0.1.6 jar.
3. Move the 0.1.6 jar OUT of plugins/ (e.g. plugins/_disabled/), upload
   ExcelTrainingWheels-0.1.7.jar (sha256 6e38061e06b6d212e5ad17ccd4213ed2afdd9fb32433d15018f5704ddec35cae,
   74,733 bytes; also in SHA256SUMS.txt). Only one TW jar in plugins/.
4. Restart (a jar swap needs a restart, no /reload). The live config.yml is kept; every new key
   falls back to its default, so Night Hearing is ON from Seal 2 with no config edit.
5. Boot log must show:
   - "Enabling ExcelTrainingWheels v0.1.7"
   - "night hearing: ON from seal 2 (seal now N, ARMED|waiting), night 13000-23000, overworld,
     range 16.0 (sneak 8.0), footsteps OFF, ... bed ALWAYS OPENS, steer to doors ON (12.0 blocks),
     wood fallback off Hard ON (12s)"
     ("seal now unknown - stays off" means GuildGate could not be read: hearing stays off.)
   - no SEVERE / exception mentioning ExcelTrainingWheels.
6. Console "tw reload" ends with the same night hearing state.
7. Tip (separate part, after step 5 AND after packets C and F are live):
   a. Drift guard: the live plugins/ExcelTips/tips.yml must have sha256
      f69e72832f1c65e4688d770bf4a443948db38803b325bc3a7e30d7c0802baeb6 (= ClaimFly staged file,
      which is built on the port warning file ef893938...). If it differs, do NOT upload; re-apply
      staged/ExcelTips/tips.yml.diff (one 12-line block after the build-flight entry) to the live file.
   b. Back up as plugins/ExcelTips/tips.yml.bak-20261025-nighthearing (never overwrite an existing .bak).
   c. Upload staged/ExcelTips/tips.yml under a temp name, rename into place, confirm sha256
      ea1364b676a75d85a9213b8c41ddbcbe7601a7c656dcbdbc4fce64b9c554cc5f, then "tips reload".
   d. "tips check" should list night-hearing (needs ExcelTrainingWheels>=0.1.7, api trigger, max 3).
   Without this step TW registers the same words itself (no show cap). Tip text approved by Mike
   (Oct 25, 2:34 PM CDT): "The dead can hear you at night. Sneak, or sleep." 

## Optional live config additions (append; never replace protected.players)
    doors:
      wood-fallback-off-hard: true   # does nothing on Hard
      wood-fallback-seconds: 12
    night-hearing:
      enabled: true
      min-seals: 2
      range: 16
      hear-footsteps: false
      types: [ZOMBIE, HUSK, ZOMBIE_VILLAGER]
      max-per-noise: 3
      max-per-player: 6
      cooldown-seconds: 20
      give-up-seconds: 60
      steer-to-doors: true
      steer-radius: 12
      bed-always-works: true
      excluded-worlds: []
      excluded-world-prefixes: ["em_"]
      log-event-keys: false
      tip:
        enabled: true
        title: "Night hearing"
        text: "The dead can hear you at night. Sneak, or sleep."
Check the live EliteMobs world names: if any overworld-type EliteMobs world does NOT start with
"em_", add it to night-hearing.excluded-worlds.

## Requirements already live (checked Oct 20-25)
Difficulty Hard, mobGriefing true, GriefPrevention HardModeZombiesBreakDoors true (GP cancels every
zombie door break server-wide when it is false). No other plugin change is needed.

## Turning only the door steering off
night-hearing.steer-to-doors: false, then "tw reload" (heard zombies then walk to the nearest wall,
vanilla style; doors are only broken when a zombie happens to path to one).

## Turning it off without a jar swap
night-hearing.enabled: false, then "tw reload". Protections, door tiers and sieges are untouched.
