# Install ExcelTrainingWheels 0.1.7 (parent does this; nothing here was applied)

0.1.7 REPLACES 0.1.6. Do not install 0.1.6 (draft PR #69) separately: 0.1.7 contains every 0.1.6
change (seal ladder table, live EliteMobs divisor, Bedrock loading-screen no-damage window) plus
Night Hearing. It installs straight over the live 0.1.5 jar, in the slot the routine had for
"TW 0.1.6" (same boot as / right after the S1 MobCombatSettings step). One plugin change per boot,
at a verified-empty moment (two 0-player reads).

Jar: ExcelTrainingWheels-0.1.7.jar
  sha256 580f0d19f171d81b418044fa3d03c7d8ab674f7d0cd0e59435a347bcd24df65f, 74,660 bytes
  (also in SHA256SUMS.txt). Rebuilt Oct 25 2026 4:33 PM CDT with a neutral plugin.yml
  description; every .class file is byte-identical to the rig-tested 6e38061e... jar, only the
  plugin.yml description and config.yml header comments changed. The 6e38061e... jar is
  superseded (superseded/), never upload it.

## 0. Already-done check (run first; skip the jar part if it passes)
Skip steps 1-6 if ALL of these are true on a fresh panel read:
  - plugins/ contains ExcelTrainingWheels-0.1.7.jar with sha256 580f0d19f171d81b418044fa3d03c7d8ab674f7d0cd0e59435a347bcd24df65f
    (or exactly 74,660 bytes if the panel shows no checksum), and it is the ONLY ExcelTrainingWheels*.jar in plugins/;
  - the latest boot log shows "Enabling ExcelTrainingWheels v0.1.7" and the "night hearing: ON from seal 2" line.
If plugins/ has ExcelTrainingWheels-0.1.7.jar with ANY other hash (e.g. the superseded 6e38061e... jar):
not done; treat it as the live jar in step 1 (back it up, replace it).
Step 7 (tip) has its own already-done check.

## 1. Drift guard (live jar)
Panel: the live TW jar must be ONE of:
  - plugins/ExcelTrainingWheels-0.1.5.jar sha256 fbe4b1e826ff87ac0294aabc51074720471aeee7c7f9c80a137e8d01a4e89afa (expected), or
  - plugins/ExcelTrainingWheels-0.1.6.jar sha256 974d159fd9f4cf9b750914f57424ebe21a736a8b846746bdd6dfe5b4bf1244e4 (only if 0.1.6 went in by mistake), or
  - plugins/ExcelTrainingWheels-0.1.7.jar sha256 6e38061e06b6d212e5ad17ccd4213ed2afdd9fb32433d15018f5704ddec35cae (superseded build).
Anything else: SKIP this part (drift), keep going with the rest of the window, report it.

## 2. Backups
  - Download a copy of plugins/ExcelTrainingWheels/config.yml (keep it in the job folder). The live
    config is NOT edited by this install.
  - Rename the live jar in place by appending .bak-20261025, e.g.
    plugins/ExcelTrainingWheels-0.1.5.jar -> plugins/ExcelTrainingWheels-0.1.5.jar.bak-20261025
    (Paper loads only *.jar, so the .bak is inert). Never overwrite an existing .bak: if that name
    exists, use .bak-20261025-2.

## 3. Upload
Upload ExcelTrainingWheels-0.1.7.jar as plugins/ExcelTrainingWheels-0.1.7.jar.part, rename to
plugins/ExcelTrainingWheels-0.1.7.jar, confirm sha256 580f0d19... (or 74,660 bytes) BEFORE the boot.
Exactly one loadable TW jar in plugins/.

## 4. Restart
A jar swap needs a restart (no /reload). Panel Start (server was shut down from the panel for the window).

## 5. Boot check
Zero SEVERE in this boot, no new WARN/ERROR versus the baseline, plugin count unchanged, and:
  - "Enabling ExcelTrainingWheels v0.1.7"
  - "bedrock loading-screen grace: ON (4s after join/respawn/world change, Bedrock only, ..., detection Floodgate API)"   (the 0.1.6 part)
  - "shield compensation divisor source: LIVE EliteMobs" (values may read "?" at enable; they bind by themselves)   (the 0.1.6 part)
  - "night hearing: ON from seal 2 (seal now N, ARMED|waiting), night 13000-23000, overworld,
    range 16.0 (sneak 8.0), footsteps OFF, types [ZOMBIE, HUSK, ZOMBIE_VILLAGER], max 3/noise 6/player,
    cooldown 20s, give up 60s, bed ALWAYS OPENS, steer to doors ON (12.0 blocks), wood fallback off Hard ON (12s)"
    ("seal now unknown - stays off" means GuildGate could not be read: hearing stays off.)
  - guildgate status unchanged before/after.

## 6. Console checks
  - "tw reload": ends with the same night hearing state; after S1 the divisor reads
    "divisor LIVE scaled x1.75 / normalized x1.00 / other x1.00" (x1.60 before S1).
  - "tw list" prints the protected list (console only; never paste it anywhere public).

## Config
No live config edit. Every 0.1.6 and 0.1.7 key falls back to its default when missing, so
Bedrock grace is ON (4 s) and Night Hearing is ON from Seal 2 with the live file untouched. The
live debug setting is kept as live has it. The jar's bundled config.yml only differs in header
comments; saveDefaultConfig never overwrites an existing config. Optional additions are listed
at the end; none is needed.

## 7. Tip (separate part, after step 5 AND after packets C and F are live)
Order in tips.yml: C. Bedrock port warning (-> ef893938...), F. ClaimFly tip (-> f69e7283...), then this.
  0. Already done if live plugins/ExcelTips/tips.yml sha256 = ea1364b676a75d85a9213b8c41ddbcbe7601a7c656dcbdbc4fce64b9c554cc5f
     and "tips check" lists night-hearing. Skip.
  a. Drift guard: the live plugins/ExcelTips/tips.yml must have sha256
     f69e72832f1c65e4688d770bf4a443948db38803b325bc3a7e30d7c0802baeb6 (= ClaimFly staged file, built on
     the port warning file ef893938...). If it differs, do NOT upload the staged file: re-compose by
     applying staged/ExcelTips/tips.yml.diff (one 12-line night-hearing block, inserted right after the
     build-flight entry) to the CURRENT live file, YAML-parse it, confirm the only change versus live
     is that block, and upload that composed file instead (record its sha256).
  b. Back up as plugins/ExcelTips/tips.yml.bak-20261025-nighthearing (never overwrite an existing .bak).
  c. Upload under a temp name, rename into place, confirm sha256 ea1364b6... (or the composed file's
     sha256), then "tips reload".
  d. "tips check" should list night-hearing (needs ExcelTrainingWheels>=0.1.7, api trigger, max 3).
  Without this step TW registers the same words itself (no show cap). Tip text approved by Mike
  (Oct 25, 2:34 PM CDT): "The dead can hear you at night. Sneak, or sleep."

## Optional live config additions (append; never replace the protected list)
    shield:
      compensation:
        source: live            # 0.1.6; missing = live
    bedrock-grace:              # 0.1.6; missing = these defaults
      enabled: true
      seconds: 4
      allow-lava: true
      show-actionbar: true
      actionbar-message: "Loading in... protected"
    doors:
      wood-fallback-off-hard: true   # 0.1.7; does nothing on Hard
      wood-fallback-seconds: 12
    night-hearing:                   # 0.1.7
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
The 0.1.6 dealt-multipliers table (fallback only) may be updated to 0:1.60 1:1.75 2:1.90 3:2.05
4:2.20 5:2.20 (live row 5 reads 2.35); with source: live the shield follows EliteMobs anyway.
Check the live EliteMobs world names: if any overworld-type EliteMobs world does NOT start with
"em_", add it to night-hearing.excluded-worlds.

## Requirements already live (checked Oct 20-25)
Difficulty Hard, mobGriefing true, GriefPrevention HardModeZombiesBreakDoors true (GP cancels every
zombie door break server-wide when it is false). No other plugin change is needed.

## Turning parts off without a jar swap ("tw reload" after each)
  - night-hearing.enabled: false (Night Hearing off; everything else unchanged)
  - night-hearing.steer-to-doors: false (only the door steering)
  - bedrock-grace.enabled: false (only the Bedrock loading-screen window)
