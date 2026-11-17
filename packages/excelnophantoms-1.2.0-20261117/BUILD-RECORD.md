# ExcelNoPhantoms 1.2.0 — BUILD record

Built Oct 18, 2026 (Argus). Design: Mike, approved Oct 18, 2026 ~8:01 PM
CDT, with the story-rotation upgrade approved ~8:04 PM CDT (superseding
the original once-per-evening dusk line for haunted nights).
Status: BUILT + tested + rig-proven. NOT installed — rides the pending
install window (INSTALL-PACKET.md in this folder).

## Provenance
- Live plugin: ExcelNoPhantoms 1.1.0, our own Excel plugin from the
  Oct 16 renames bundle. Source copied from
  ~/workspace/renames-2026-10-16/src/ExcelNoPhantoms/; package and class
  names unchanged (com.winkreyes.excelnophantoms).
- LegacyDataMigration.java carried over BYTE-UNTOUCHED (SHA-256
  2bc0e7d4c5705edd088ca52bbd4ad0c1ac7b289950f61d6f4e0e1bdf3ae380d1 in
  both trees). Live 1.1.0 jar backup:
  ~/workspace/renames-2026-10-16/ExcelNoPhantoms-1.1.0.jar (the rollback).
- 1.1.0 behaviour being extended: one listener cancelling EVERY phantom
  CreatureSpawnEvent, no config. Player-verified live Oct 18 (WRV-060).

## Semantics (the PhantomGate)
Pure gate (PhantomGate + GateConfig, no Bukkit imports) decides
SUPPRESS vs ALLOW for a phantom spawn from mode, event-active flag,
dragon-seal state, and the America/Chicago weekday:
- mode always-off → SUPPRESS always. The bundled default config.yml
  ships mode: always-off, so a bare 1.2.0 install reproduces 1.1.0
  exactly.
- mode event + event-active + dragon seal not broken:
  Mon/Tue/Wed/Thu (Chicago) → ALLOW (the plugin force-spawns nothing;
  vanilla insomnia rules do the rest); Fri/Sat/Sun → SUPPRESS.
- event-active=false OR dragon seal broken → SUPPRESS always (ENDED).
- The gate applies to phantom CreatureSpawnEvents of ANY spawn reason —
  parity with 1.1.0's blanket cancel, narrowed only by the gate.
- dragonSealBroken=null (unreadable) → operator flag + weekday govern.
- Malformed gate config (bad mode string, missing mode/event-active,
  wrong value types; also an unparseable config file, which Bukkit
  surfaces as missing keys) → effective mode always-off + console
  warnings. Fail soft = always-suppress, the kids'-server safe default.
- "Tonight" in the boot line and /nophantoms status means TODAY's
  Chicago weekday (documented simplification).

## Dragon-seal auto-check (reflection, fail-soft)
Mirrors ExcelMagicGate's house pattern: get plugin "ExcelGuildGate",
load com.winkreyes.guildgate.api.GuildGateAPI from its classloader,
read its private static field `state` (setAccessible), invoke the public
synchronized SealState.isBroken("ender_dragon"). Handles are cached;
resolution is lazy and retries until first success, so load order is
safe (plugin.yml also declares softdepend: [ExcelGuildGate]). ANY
failure — plugin absent/disabled, class/field missing, state not yet
bound, invocation error — returns null (unknown). Errors warn at most
once and can never break the spawn listener (the read is fully guarded;
the listener's own path catches Throwable around the reader).
Rig-proven live: /nophantoms status read "Dragon seal (ender_dragon):
not broken" from a real GuildGate 0.3.0 co-boot (see Rig evidence).

## Broadcasts (Mike's Oct 18 ~8:04 PM upgrade)
Two chat behaviours, both checked by a 1-minute repeating task AND on
PlayerJoinEvent, both requiring ≥1 player online and the primary world
inside the night window — world time 13000–23000 ticks (the round
nightfall-to-late-night span of the vanilla phantom hours):
- STORY ROTATION (haunted nights only: event live + Chicago Mon–Thu):
  one line from config `story-lines` every `story-interval-minutes`
  (default 25), cycling in order and wrapping. The rotation's first
  line of an evening is that evening's opener — there is no separate
  haunted dusk line. State discipline: the rotation INDEX persists in
  the data folder's state.yml (a restart resumes the story mid-cycle);
  the last-fired instant is in-memory only, so after a restart the next
  line is due on the first eligible check. No reload command exists —
  config edits apply on restart, which rebuilds the rotator from the
  persisted index, so reload-by-restart can neither double-fire within
  an interval after its first line nor stall the story.
- WARD LINE (ward evenings only: event live + Chicago Fri/Sat/Sun):
  ONE line per evening (config `wards-line`, gated by
  `wards-line-enabled: true`), fired at first nightfall. The
  last-fired Chicago date persists in state.yml
  (key last-wards-line-date), so restarts never double-fire it.
- Malformed story config (story-lines empty / not a list / non-string
  entry) fails soft to NO broadcasts while the gate keeps working; a
  bad interval falls back to 25 minutes with a warning.

Final story lines as drafted (Argus, valley voice, Mike edits freely):
1. "&5🌙 &dThe Phantom Debt &7— long ago, in the Long Day, the valley skipped its nights... and the sky kept count."
2. "&5🌙 &7At the edge of the End, the dragon stirs in its sleep — &dits nightmares leak into our sky &7and take wing as phantoms."
3. "&5🌙 &7The guild's wards strain to hold them back: &dthey fail Monday through Thursday&7, but hold fast on the weekend."
4. "&5🌙 &7They hunt the sleepless. &fSleeping is your shield &7— one slept night, and they forget you."
5. "&5🌙 &7And when the dragon falls, the debt is paid — &dthe sky clears for good."
Ward line: "&5🌙 &dThe Phantom Debt &7— the guild's wards hold tonight. &fSleep easy, valley."

## Boot line + /nophantoms status
Boot (examples from the rig):
- always-off: "ExcelNoPhantoms 1.2.0 enabled: mode=always-off — phantoms suppressed (mode always-off)."
- event, wards day: "ExcelNoPhantoms 1.2.0 enabled: mode=event, event ACTIVE — Phantom Debt: WARDS HOLD tonight (Sunday)."
- event, haunted day: "…event ACTIVE — Phantom Debt: HAUNTED tonight (Tuesday); wards hold Fri–Sun."
- ended: "…event ENDED — Phantom Debt: ENDED — wards hold for good."
/nophantoms status (permission excelnophantoms.admin, default op,
read-only) prints mode (+malformed note), event-active, seal state
(broken / not broken / unreadable), tonight's label via the SAME gate
code path as the listener, the story rotation's next line index, and
the ward line's last-fired date.

## Automated tests — 35/35 passed, 0 failed
House harness (JUnit 5 + Mockito, JUnit platform console):
- ExcelNoPhantomsTest (6): the two 1.1.0 tests adapted (no config →
  fail-soft suppress == the old blanket cancel; non-phantoms untouched)
  + event-mode delegation: haunted Tuesday ALLOW, ward Friday SUPPRESS,
  broken seal SUPPRESS on Tuesday, non-phantom never cancelled in event.
- PhantomGateTest (11): all-7-weekday truth table; midnight boundaries
  (Thu 23:59:59 ALLOW, Fri 00:00:00 SUPPRESS, Sun 23:59:59 SUPPRESS,
  Mon 00:00:00 ALLOW); event-active=false all week; always-off all
  week; seal broken on Tuesday; seal null governed by flag+weekday;
  malformed config ×3 (bad mode string, missing keys, wrong types) →
  always-suppress with warnings; null config/time fail soft; tonight
  labels.
- BroadcastTest (13): rotation advances through all lines and wraps;
  interval respected (24 min no, 25 min yes); restart resumes the
  persisted index and fires promptly; silent in daytime (incl. window
  edges 12999/23001, fires at 13000); silent with 0 players; silent on
  ward nights; silent when event-active=false / always-off / seal
  broken; custom interval + custom lines from config are what's used;
  malformed story-lines → no broadcasts but the gate still ALLOWs;
  empty story-lines → no broadcasts; ward line fires once per ward
  evening incl. across a simulated restart, stays silent on haunted
  nights / when ended / daytime / empty server / when disabled.
- LegacyDataMigrationTest (5): the 1.1.0 suite, unchanged.

## Rig evidence (scratch Paper 26.2-123, the live build)
Rig: scratch/server/ in this folder, ExcelGuildGate-0.3.0.jar alongside
(logs rig-always-off.log, rig-event.log).
PROVEN:
1. Boot A (bundled default config): "ExcelNoPhantoms 1.2.0 enabled:
   mode=always-off — phantoms suppressed (mode always-off).";
   GuildGate enabled at 0 of 5. Config generated by saveDefaultConfig.
2. Boot B (packet-config, mode: event): "ExcelNoPhantoms 1.2.0 enabled:
   mode=event, event ACTIVE — Phantom Debt: WARDS HOLD tonight
   (Sunday)."
3. `version ExcelNoPhantoms` reports 1.2.0; console /nophantoms status
   works in both modes and reads the dragon seal from the live
   GuildGate by reflection ("not broken", not "unreadable"); in event
   mode it also reports "Story rotation: next line 1 of 5, every 25
   min on haunted nights (world time 13000-23000)".
4. Zero SEVERE naming either plugin in either boot.
HONEST LIMIT: Oct 18, 2026 is a Sunday — a wards day — and the rig has
no players, so the rig proves the WARDS boot/status path only. The
ALLOW path is proven by the unit truth table (and the listener
delegation tests), NOT by the rig; no day-override knob was added to
fake it. First haunted night after the live install (Mon Oct 19) is
the live proof — the install packet names it as a player-pass check.
Broadcast firing likewise rests on the unit suite (the rig never has a
player online at night).

## Artifact
- ExcelNoPhantoms-1.2.0.jar — 40,631 bytes
  SHA-256 f705ac0e657645c09ed20427289c738dd25b5eb6c3983f3a383c825a057d94f8
  MD5 2848050ef92e19866bc9af3a98fed1fa
- Rebuild reproduction: a second clean build (build.sh) produced the
  identical SHA-256. ✔
- Sources: 6 main (ExcelNoPhantoms, PhantomGate, GateConfig,
  StoryRotator, WardLineTracker, LegacyDataMigration) + plugin.yml +
  config.yml in src/ExcelNoPhantoms/.

## Drive deposit (deposit-at-build, MD5-verified Oct 18, 2026)
- Jar → Drive "Minecraft WRV" / Build files: `ExcelNoPhantoms-1.2.0.jar`
  — file ID 1aCC7KQuazWht42NnzG420ycvPx9wePUT — 40,631 B,
  MD5 2848050ef92e19866bc9af3a98fed1fa (matches local).
- Source zip → Drive "Minecraft WRV" / Source: `ExcelNoPhantoms-1.2.0-source.zip`
  — file ID 1QLD41G8jxo8-DdpbGdytKf-lUnalz7z3 — 88,665 B,
  MD5 7dee6d4d182afd670ec50f2602249d91 (matches local).
- SOURCE.md → Drive "Minecraft WRV" / Source: `ExcelNoPhantoms-1.2.0-SOURCE.md`
  — file ID 1MBtmnwX8FYtrsAjrEsmUVe7h8Gbl2a5H — 3,409 B,
  MD5 d7a1b20cb0d4c3915eaa9164fae3283f (matches local).
- Note: this deposit section was appended to the project copy of this
  doc after the source zip was packed (the sibling-project pattern);
  the zip's copy of BUILD-1.2.0.md predates it by these few lines.
