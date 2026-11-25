# BUILD RECORD — ExcelCalmBars 0.1.0 (combat-calm XP bars)

Lane: FEEDBACK-TRACKER row 92 — Marvin's stacked XP boss bars
(email Nov 24, 2026, 6:08 PM CT). Spec: Mike's ruling, Nov 24, 2026,
6:30 PM CT — XP-gain boss bars are held back while the player is in
combat and for a quiet period after the last combat event, then
shown/released normally. Quiet period = 30 s as a config value.
Decision log: D227. Investigation: ~/workspace/marvin-bars-feedback-
20261124/FINDINGS.md. This lane did NOT install on live and did NOT
contact Marvin or Mike.

## Home + name (choice and reasoning)

NEW standalone plugin **ExcelCalmBars** (package
com.winkreyes.excelcalmbars), per the FINDINGS recommendation. Not an
ExcelEMFixes module: the gate covers Jobs and mcMMO bars as well as
EliteMobs skill bars, and EMFixes' charter is EliteMobs-facing fixes;
a cross-plugin packet gate belongs in its own tiny plugin with no
depend on any of the three bar owners (it must keep working — fail-
open — if any one of them is absent or changes). Name follows the
fleet Excel* style and says what it does.

## Mechanism

- Combat state: Bukkit EntityDamageByEntityEvent (MONITOR,
  ignoreCancelled), damage dealt or taken, projectile shooters
  resolved — the fleet D184 out-of-combat pattern. No dependency on
  any plugin's combat API.
- Gate: one Netty ChannelDuplexHandler per player connection,
  injected on the head side of the pipeline entry that holds the
  server's Connection object (found by identity/class match, with
  "packet_handler" and "encoder" as fallback anchors; the anchor and
  full pipeline are logged at INFO on every injection). It sees
  ClientboundBossEventPacket objects before encoding/translation.
- Identification is POSITIVE only: a bar is an XP bar only when its
  plain title matches the Jobs pattern (live locale template), an
  mcMMO skill-name pattern (the 19 live skill names, "Name Lv.{n}",
  plus the early-game title "Learning a new skill..."), or an
  EliteMobs skill-bar pattern (live skills.yml formats over the 11
  live EM skill types, incl. the LEVEL UP variant). Anything else —
  boss bars, quest bars, unknown bars — always passes. A never-hold
  substring list ("inventory protected") is a second belt over the
  ExcelDeathProtection bar, which matches no XP pattern anyway.
- Hold semantics: while in combat or inside the quiet window, XP-bar
  packets are merged into per-bar state and swallowed (the write
  promise is completed so senders see success). REMOVE always passes
  and drops state. When combat starts, XP bars already on screen are
  retracted (a bridge-sent REMOVE). Release rebuilds the bar from
  merged state via ServerBossEvent + the packet factory methods
  (raw-packet replay as fallback); bridge-sent packets are identity-
  marked so the gate never re-holds its own releases.
- Fail-open everywhere: every decision path catches Throwable and
  passes the packet; a bar is released at quiet-period end or the
  max-hold cap (default 120 s), whichever first; death, world
  change, logout (state dropped with the connection) and plugin
  disable (flush) all release/drop cleanly. All NMS access is
  reflection through BossBridge; if the bridge cannot resolve, the
  plugin reports itself unavailable and gates nothing.
- Config (packet/config.yml): enabled (true), quiet-period-seconds
  (**30** — the shipped default; rigs overrode to 5),
  max-hold-seconds (120), debug-logging, never-hold substrings, the
  three pattern families. /calmbars status|flush|reload.

## Bugs found BY the mirror proof (why the rig mattered)

1. **Reflection access**: BossBridge resolved
  ClientboundBossEventPacket$Operation.getType() without
  setAccessible — Operation is a package-private interface, so every
  tryHold threw IllegalAccessException and failed open (bars flowed,
  nothing held, silently). Found via a first-failure warning added
  for exactly this; fixed with setAccessible(true).
2. **Injection observability/anchoring**: the first build anchored
  by handler name with a silent catch. Rebuilt to anchor on the
  Connection-holding pipeline entry, log the anchor + pipeline at
  INFO, and log injection failure as a warning (still fail-open).
  A "boss packets seen" counter in /calmbars status now proves
  interception at a glance.
3. Join-time sync: bars added before injection are first seen as
  updates; the gate now treats a first-contact update as client-
  held state (the server only updates bars the client has), so
  combat-start retraction also covers join-synced bars.

## Mirror proof (Paper 26.1.2 rig, Mineflayer bot "CalmBot")

Rig note: the behavioral rig ran Paper **26.1.2**, not 26.3 —
minecraft-data (latest published, 3.119.0) carries no 26.3 dataset,
so Mineflayer cannot speak protocol 777; the fleet's skills-menu
lane hit the same cap and proved on 26.1.2. Mitigations: (i) the
bridge's reflection targets were javap-verified field-for-field
identical on the 26.3 and 26.1.2 server jars; (ii) the plugin was
also booted on a Paper 26.3 rig with the exact live plugin set —
"ExcelCalmBars 0.1.0 ready — quiet period 5s, max hold 120s, bridge
ok", Done, SEVERE 0 (rig/boot3.log + boot4.log, Nov 24). A 26.3
bot-join through ViaVersion was attempted and the bot was kicked
("Invalid move player packet") — a Via/mineflayer quirk, recorded,
not worked around further. Rig config: quiet-period-seconds 5,
debug-logging true; all other plugins were the LIVE jars (mcMMO,
Jobs, ExcelDeathProtection 0.2.2, ExcelItems, CMILib, Vault,
EssentialsX). All timestamps CST, Nov 24, 2026, from
rig2612/bot/bot-out*.log (bot packet log), rig2612/boot4.log
(server + [calmbars] debug), cross-checked.

- **(a) Out of combat, bars arrive promptly — PASS.**
  Jobs: dig grass 01:54:02 → BOSS_CREATED "Lvl 1 Digger: 0.30/14 xp
  (+0.29)" 01:54:06 (the ~4 s is Jobs' own Batched delay; zero gate
  holds logged). mcMMO: mine stone → BOSS_CREATED "Learning a new
  skill..." on the first gain, updates flowing, Mining 132 XP on
  /mcstats. Repeat on the final build: dig 01:13:00 → bar created
  same second, gate "boss packets seen: 4" (ADD+updates+REMOVE).
- **(b) In combat, XP bars held, released after the quiet period —
  PASS.** Verified combat (bot swinging at a tanked zombie, damage
  events both directions; zombie Health read 0.82 under fire).
  01:13:24 server: "held ADD 'Learning a new skill...'" (mcMMO
  Mining gained by digging mid-fight) — bot received nothing.
  01:13:28 server: "held ADD 'Lvl 2 Hunter: 15.70/36 xp (+14.85)'"
  then "released 'Lvl 2 Hunter…' (quiet period over, 1 packets)";
  bot BOSS_CREATED the Hunter bar 01:13:28.827, ~5 s after the
  killing blow ended combat. Round 2 (unkillable zombie): 01:14:01
  "held ADD 'Learning a new skill...'", 01:14:13 "held ADD 'Lvl 2
  Hunter: 30.55/36 xp (+14.85)'" — no XP bar reached the bot in
  either round while fighting. Retraction: Digger bar visible
  01:14:58.522 → combat starts → server "retracted 'Lvl 1 Digger:
  1.68/14 xp (+0.24)' for CalmBot" 01:15:03, bot BOSS_DELETED
  01:15:03.125 (Jobs' own delete would have come ~5 s later).
  Owner-withdrawal interaction (honest note): mcMMO hides its bar
  ~3 s after creation even while held; its REMOVE passes and drops
  the held state, so a short-lived mcMMO bar gained mid-fight may
  never display — the XP is unaffected, only the pop-up is gone,
  which is the player's ask. Jobs bars (longer owner timer) survive
  the hold and release normally, as proven above.
- **(c) Real boss bars never held — PASS.** Vanilla /bossbar bars
  delivered promptly three times, including the rigorous case:
  "Test Boss Three" BOSS_CREATED 01:14:04.991 DURING verified
  combat, in the same window where an mcMMO bar was being held.
- **(d) ExcelDeathProtection bar never held — PASS (rig, real
  bar).** Bytecode first: EDP 0.2.2's standing bar belongs to the
  BASIC tier (refreshPresentation: basic → basicBars boss bar
  "Inventory protected: 1 death"; premium → a potion effect, no
  bar). The bot bought BASIC through the real purchase GUI
  ($15,000): BOSS_CREATED "Inventory protected: 1 death"
  01:00:48.589 out of combat; across both combat rounds the count
  of BOSS_DELETED for that title is 0 — never held, never
  retracted. Title also matches no XP pattern and is on the
  never-hold list (unit-tested).
- **(e) Clean enable/disable, no leak across relog — PASS.**
  Relog mid-hold: bot quit while the Hunter bar was held
  (01:14:13) and rejoined 01:14:33 — dump shows only the four
  legitimate persistent bars (3 test bossbars + EDP), no XP bar,
  fresh gate state (held: 0, last combat: none). Boots: with jar —
  ready line + Done + SEVERE 0 (rig2612/boot4.log, boot6-final.log;
  also the 26.3 rig boots); without jar — Done, SEVERE 0, zero
  ExcelCalmBars mentions (boot5-noplugin.log). Pattern unit test:
  22/22 checks on the final sources ("ALL PATTERN CHECKS PASSED")
  — real titles from Marvin's screenshot identified; EDP title,
  Wither/Ender Dragon, boss and quest titles, and near-misses all
  rejected. Jar build is deterministic (two builds byte-identical).

## Rig quirks recorded for the next lane

- Paper 26.3 rig RCON auth failed despite matching credentials —
  console FIFO used instead; do not rely on rcon.py there.
- Flat-world staging at y<40 spawns slimes that killed the bot
  twice; combat staging moved to a sealed stone room at y=70.
- /summon NBT in the old ArmorItems/Attributes format is silently
  ignored on 26.x — tank mobs via /effect (regeneration/resistance)
  instead. mcMMO paid no Swords XP against the summoned zombie
  (damage registered; Jobs Hunter paid per damage batch) — the
  (b) proof therefore used Mining/Digger/Hunter gains during
  verified combat, which is also the real-world stacking case.

## Candidate

ExcelCalmBars-0.1.0.jar — 19,006 B — SHA-256
111794a81c830f6886a5fe5881d358ccfc0387b83ab2d7a58c0ca6f877c899e0
(staged in packet/ with config.yml + SURFACES.md + SHA256SUMS).
Companion, NOT a dependency: the Jobs boss-bar Timer trim 5→2 was
filed separately and is already INSTALLED as a TEXT live-edit
(Nov 24, 2026); ExcelCalmBars installs and works with or without it.
