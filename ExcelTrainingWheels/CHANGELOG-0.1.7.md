# ExcelTrainingWheels 0.1.7: Night Hearing (STAGED, not live)

First built Sun Oct 25, 2026, about 2:45 PM CDT; final jar rebuilt 3:25 PM CDT (adds door steering, below) on the box. Base: the staged 0.1.6 tree (draft PR #69,
jar 974d159f...). The box toolchain rebuilds 0.1.6 with every class and resource byte-identical
to the shipped jar; only the jar MANIFEST "Created-By" line differs (JDK 25.0.4.1 on the box vs
25.0.5 where 0.1.6 was built). So the 0.1.7 jar sha depends on the JDK patch version.
Deterministic on the box: two clean builds give the same sha (see SHA256SUMS.txt).

Approved by Mike Oct 25, 2026, 2:34 PM CDT. The server stays on Hard permanently.

## 1. Night Hearing (new)
From Seal 2 on, at night, zombies hear loud player actions inside a base and come for the
player who made the noise. The door rules that already exist do the rest:
every zombie breaks wooden doors (vanilla Hard, about 12-15 s), copper doors fall to the 45 s
siege, iron doors to the 60 s siege, and door guard keeps any door shut for a protected player.
Hearing never breaks blocks itself and never spawns mobs. It only gives nearby idle zombies a target.

When it is on (all must hold; anything unknown = OFF):
- night-hearing.enabled (default true), and the TW master switch;
- ExcelGuildGate seals broken >= night-hearing.min-seals (default 2). If the seal count cannot be
  read, hearing stays OFF (fail closed). The count refreshes on join, every 30 s and on /tw reload;
- an overworld (NORMAL environment) world, not in night-hearing.excluded-worlds, and not starting
  with an excluded prefix (default "em_", the EliteMobs dungeon / instanced worlds);
- world time 13000 to 23000.

What counts as a noise (vanilla game events, the same vibrations a sculk sensor hears, by a player):
- doors, trapdoors and fence gates (block_open, block_close);
- breaking and placing blocks (block_destroy, block_place);
- hitting (entity_damage, plus EntityDamageByEntityEvent for melee and own projectiles);
- shooting (projectile_shoot);
- containers (container_open / close, shulker_open / close);
- note blocks and jukeboxes (note_block_play, jukebox_play).
- Footsteps and sprinting are silent unless night-hearing.hear-footsteps: true (off).

Range: night-hearing.range blocks (default 16, clamp 1..48) from the noise, halved while sneaking.

Who hears: only ZOMBIE, HUSK and ZOMBIE_VILLAGER (night-hearing.types; drowned are always
stripped) that have no current target and are not EliteMobs elites. Nearest first.

Limits: max-per-noise 3, max-per-player 6 (zombie-family mobs within 64 blocks already hunting
that player count), cooldown-seconds 20 per player (starts only when a noise actually pulled a
zombie), give-up-seconds 60.

Give-up rule: a pulled zombie lets go after 60 s without reaching the player AND without working
on a door toward that player. A zombie busy at a door keeps its hunt, so the 45 s copper and 60 s
iron sieges always finish (rig-tested on iron). Reaching the player hands it to normal combat.
"Reached" means within 3 blocks AND in line of sight: a zombie pressed against the outside of a
wall is close but has not reached anyone, so it still gives up after 60 s.

Door steering (night-hearing.steer-to-doors, default true; steer-radius 12, clamp 4..24):
rig testing showed that a vanilla zombie with no open way in does not walk to a closed door. It
goes to the outside spot closest to the player (usually a bare wall) and stands there, so the
door rules never got a chance. Now, when a heard zombie has no walkable path to the player, TW
looks for closed doors within 12 blocks of the player, picks the outside face the zombie can walk
to (short walk first, then the door closest to the player) and sends it there. At the door it
works that door's own siege: wooden doors doors.wood-fallback-seconds (12 s, about vanilla Hard),
copper 45 s, iron 60 s. When the door is open or down, or the zombie sees the player, it goes back
to a plain hunt. A zombie with a walkable way in is left to vanilla. Steering never targets a
protected player (they are never heard) and a steered siege stops if the hunted player is
protected. A steered zombie counts toward max-per-player. steer-to-doors: false gives the plain
vanilla walk-to-the-wall behavior.

Targeting goes through Mob.setTarget, which fires EntityTargetLivingEntityEvent, so the Bedrock
loading-screen grace, ExcelMobVeil and any other plugin can still refuse; TW checks the target
stuck before counting a zombie.

Never heard (silently, nothing shown to anyone): protected players, creative and spectator, a
Bedrock player inside the loading-screen grace, a MobVeil-veiled player, a sleeping or dead player.

At dawn, when the seal count drops, or on /tw reload, every zombie TW pulled is released.

## 2. Beds always work while Night Hearing is live
- PlayerBedEnterEvent: if Night Hearing is live in that world and the only reason the bed refuses
  is "monsters nearby" (NOT_SAFE), the bed is allowed. Daytime, too far, obstructed, nether/end
  refusals are never overridden. night-hearing.bed-always-works (default true).
- Getting into bed makes the player silent: every zombie-family mob within 64 blocks hunting them
  (non-elite) drops the target, and while they sleep those types cannot target them.

## 3. Wood-door safety net off Hard (new, config, default on)
- (Also used by door steering: a zombie that Night Hearing sent to a wooden door works it on the
  same 12 s wood siege on any difficulty, so a steered zombie at a wooden door behaves the same on
  Hard and Normal.)
- Vanilla zombies break wooden doors only on Hard. The server is on Hard permanently, so this
  normally does nothing (rig-confirmed: no WOOD SIEGE line on Hard).
- If a world is on Easy or Normal, a door-breaking zombie hunting an unprotected player at a wooden
  door brings it down after doors.wood-fallback-seconds (default 12), on the same siege machinery as
  copper and iron (cracks, banging, no drop). Door guard still wins for protected players.
- Keys: doors.wood-fallback-off-hard (true), doors.wood-fallback-seconds (12).

## 4. Public tip (ExcelTips)
- One tip, "night-hearing": "The dead can hear you at night. Sneak, or sleep."
- TW calls ExcelTipsAPI.tip at most once per player per night while hearing is live in their world
  (by reflection; if ExcelTips is missing the feature still works and one warning is logged).
  TW registers default words (title "Night hearing"); the staged ExcelTips tips.yml entry wins and
  caps it at 3 shows. Shown to everyone in a live world, protected players too.
  Words also in night-hearing.tip.* (enabled, title, text).

## 5. Visibility
- Boot line: "night hearing: ON from seal 2 (seal now N, ARMED|waiting), night 13000-23000, ...,
  steer to doors ON (12.0 blocks), wood fallback off Hard ON (12s)".
- /tw reload appends the same state.
- Console: "NIGHT HEARING: <event> by <player> at <pos> (range R[, sneaking]) pulled N zombie(s)"
  (door-log throttled per player when debug is off). Debug adds gave-up, released-at-dawn,
  bed-opened and "no idle zombie in range" lines. night-hearing.log-event-keys: true logs every
  vibration key while live (diagnostic, off).

## 6. Config (all keys optional; missing = these defaults)
    doors:
      wood-fallback-off-hard: true
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

## 7. Code
- New core/NightHearing.java (pure, unit-tested): night window, gate, noise keys, range, who is heard,
  which zombies, pull caps, per-player cooldown, pulled-zombie clocks (give-up with door progress),
  bed rule, wood-fallback rule, door steering helpers (steerKind, steerScore, outsideFace, onSpot,
  sanitizeSteerRadius), sanitizers.
- ExcelTrainingWheels.java: GenericGameEvent + EntityDamageByEntityEvent listeners, 1 s hearing tick,
  door steering (Paper Pathfinder findPath/moveTo, per-zombie steer book, steered sieges on the
  siege cadence),
  bed listeners, sleeper target guard, quit cleanup, tip seam, MobVeil seam, wood siege book, the
  unclamped seal count (cachedSealsRaw; the compensation cache stays clamped to the table).
  observeSiege gained an overload with the siege label; iron and copper output is unchanged.
- plugin.yml: version 0.1.7, softdepend adds ExcelTips and ExcelMobVeil (both optional).
- build.sh: jar name 0.1.7.
- Unchanged: shield, compensation, door guard, copper and iron sieges, all-zombies-break-doors,
  lifesteal, log throttle, Bedrock grace.

## 8. Tests
- run-tests.sh: 497 passed, 0 failed (NightHearingTest new, 144 checks incl. 23 door-steering checks;
  all 353 earlier checks unchanged and passing). Log: unit-tests.txt.
- Rig (Paper 26.3 build 214, two offline test players, Hard, GuildGate 0.3.3, ExcelTips 0.1.0,
  GriefPrevention 16.18.7): see RIG-TEST-RESULTS-0.1.7.md for every case and the exact jar.

## 9. ExcelTips tips.yml (staged, separate part)
- Built on top of the ClaimFly staged tips.yml (sha256 f69e7283...), which itself sits on the Bedrock
  port warning staged tips.yml (ef893938...). One entry added after build-flight; nothing else changed.
- Staged file sha256: see SHA256SUMS.txt (ea1364b6...). Install order: port warning (C), then
  ClaimFly (F), then this.
