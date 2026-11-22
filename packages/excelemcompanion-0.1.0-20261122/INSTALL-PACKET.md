# INSTALL PACKET — ExcelEMCompanion 0.1.0 (NEW plugin)

Filed Nov 21, 2026. One combined plugin (D186): drop-guide module
(D185) + progress-tracker module (D186/D187). New plugin — no jar
swap, no config merge, no game-state writes (both modules are
read-only: menus over generated indexes, EM data and ExcelProtect
records).

## Candidate

- Jar: ~/workspace/companion-build-20261121/ExcelEMCompanion-0.1.0.jar
  — 134,742 B / SHA-256
  200ae1fd4056379a10bfe69858fb932ef5e8c17c98abf7fa4fc44d40d70fb7b6
- Unit tests 137/137 PASS. Mirror rig 36/36 PASS, SEVERE 0 across
  three boots (full proof + each module toggled off).
  Record: ~/workspace/companion-build-20261121/BUILD-RECORD.md

## Live-premise gate (STOP on any mismatch)

- NO file matching plugins/ExcelEMCompanion*.jar exists live, and
  no plugins/ExcelEMCompanion/ data folder exists. (New plugin;
  if either exists, someone already installed it — STOP and
  report.)
- Live EliteMobs jar present (softdepend; tracker reads its data),
  ExcelProtect present (tracker per-boss counts read its entities
  records; absence degrades counts to downed display, not a stop).

## Window steps (standard gates: backup-first, verified empty
twice, marker re-checks before stop/start)

1. Place ExcelEMCompanion-0.1.0.jar into plugins/.
2. No config to place: the plugin writes its default config.yml
   on first enable (both modules enabled, page sizes 45/40 — the
   shipped defaults).
3. Start; verify in the boot log, SEVERE 0:
   - "ExcelEMCompanion 0.1.0 ready — modules: [drop-guide, progress-tracker]"
   - "Drop Guide ready — 38 places, 407 mobs, 4066 drops."
   - "Progress Tracker ready — 111 quests, 14 dungeons, 38 places."
4. Append the window row to
   ~/workspace/wrv-install-queue/INSTALL-TIMING-LOG.md.
5. Write the marker (below) with the boot lines + timing.

## Same-pass surfaces (owed in the install pass)

What's New, BOTH surfaces (website section + in-game
ExcelWhatsNew changelog), one entry:

"NEW: Drop Guide & Progress Tracker — /drops shows what each boss
and dungeon drops (pick a place, pick a boss, see its full drop
list). /progress shows your own record: quests, dungeons, boss
kill counts and dungeon clears — the count's color is your
expertise, from gray Novice to light-blue Mastered. Works on Java
and Bedrock."

Guide/website detail is NOT owed for this one (menus are
self-explanatory in game; the Guide hibernation stands).

## Proofs owed after install (post-release, per ship-it)

- First player (or bot) opens /drops and /progress on Java.
- First Bedrock player tap-through of both modules incl. the
  expertise legend detail screen (Cumulus form path — the one
  surface the mirror could not exercise without Geyser).
- Jester's live /progress should read 63+ completions / 61
  distinct (his numbers keep moving as he plays; the shape is
  the proof, the mirror already proved exactness).

## Rollback

Remove plugins/ExcelEMCompanion-0.1.0.jar and the generated
plugins/ExcelEMCompanion/ folder; restart. Nothing else was
touched; no player or world state depends on this plugin.

## Marker

~/workspace/companion-build-20261121/INSTALLED.txt
