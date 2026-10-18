# ExcelQuartermaster 0.1.0 — the Old Quartermaster's voice

Built Oct 18, 2026, at Mike's ask (~9:18 AM CDT: "can't we just make it so
when people interact with him he tells the story?"). This is Phase 2 (the
VOICE) of the village quartermaster packet
(~/workspace/village-quartermaster/DESIGN.md).

## What it does

The Old Quartermaster is a Shopkeepers admin-shop villager in the spawn
village (world, about -783, 81, -299) with deliberately empty trades. On
his own he can only open that empty trade window. With this plugin:

- A player right-clicks him (not sneaking): the interaction is cancelled,
  so the empty trade window never opens, and the player instead gets the
  next of his tales in chat, under a gold "Old Quartermaster says:"
  header.
- Tales rotate PER PLAYER: everyone walks the list in their own order
  and loops to the top. Progress is saved across restarts in
  `plugins/ExcelQuartermaster/player-tales.txt`.
- Per-player cooldown (default 8 seconds): clicks during the wait are
  swallowed silently — no tale, no trade window, and clicking does not
  extend the wait.
- A SNEAK-click is never touched: that is the Shopkeepers editor gesture,
  and it passes through exactly as if this plugin were not there.
- Long tales are split into at most two chat lines, so one click never
  floods the chat.

He is found by NAME + ANCHOR, not by any Shopkeepers internals: a villager
whose custom name matches the configured name (color codes and case
ignored), within the configured radius of the configured spot in the
configured world. The anchor can be switched off in config, but it exists
so a renamed villager anywhere else in the world can never start telling
tales. The plugin is dependency-free (no hard or soft depend on
Shopkeepers) and has no commands.

The default tales are the 11 pre-seal rotation lines from
~/workspace/village-quartermaster/DIALOGUE.md (chat lines carry no
wrapping quote marks; the "says:" header does the framing). Seal 1 and
later-stage lines from DIALOGUE.md are NOT in v1: when Seal 1 falls, his
job changes to wand issue, which is its own spec
(WAND-ISSUE-SPEC.md), and the stage lines can be swapped into the config
tales list by hand at that point if wanted.

## Configuration (plugins/ExcelQuartermaster/config.yml)

- `npc-name` — the name over his head (default "Old Quartermaster").
- `header` — line above each tale; `{name}` is replaced by npc-name.
- `cooldown-seconds` — per-player wait between tales (default 8; 0 = off).
- `anchor.enabled/world/x/y/z/radius` — the safety anchor (defaults to his
  real spot in world "world", radius 10).
- `tales` — the rotation list; edit freely, saved player indexes wrap
  safely if the list grows or shrinks.

## Design

All rules live in Bukkit-free classes so they are unit-tested on a plain
JVM; the main class is wiring only.

- `TaleRotation` — the decide() rules: sneak passthrough, cooldown,
  rotation order and wrap, per-player independence, index safety across
  config edits (stored index is taken modulo the current list size).
- `TaleStore` — the player-tales.txt format (one `uuid nextIndex
  lastTaleMillis` line per player, sorted; malformed lines are skipped,
  never fatal).
- `NpcMatcher` — name match (legacy § codes stripped, case-insensitive)
  and the 3D anchor check (boundary counts as inside).
- `TaleText` — visible-length measurement (color codes are free) and the
  balanced at-most-two-lines split (the second line re-applies the first
  line's leading color codes).

## Tests

`src/ExcelQuartermaster/run-tests.sh` compiles src + tests and runs four
plain-main harnesses: TaleRotationTest 44 checks, TaleStoreTest 16,
NpcMatcherTest 27, TaleTextTest 22 — **109/109 checks pass** (Oct 18,
2026). Coverage includes rotation order/wrap, per-player independence,
cooldown block/no-advance/no-extend/expiry boundary, sneak passthrough
(in and out of cooldown), empty-tale and null-player paths, persistence
round-trips and malformed-line tolerance, name/anchor matching edges,
and chat splitting (balance, word preservation, color inheritance).

## Build

`src/ExcelQuartermaster/build.sh` — manual javac (JDK 25, --release 21,
same harness classpath as ExcelGuildGate), reproducible packaging
(pinned entry dates, sorted entries). Two clean builds verified
byte-identical on Oct 18, 2026.

- ExcelQuartermaster-0.1.0.jar — 16,221 bytes
- SHA-256: b27f8648f3db0116ab1207c4105681fe9cc06d2c9cd143cdacade231471e0d01

## Scratch verification (Oct 18, 2026)

Throwaway Paper 26.2-123 server in /tmp (never the live server), with
Shopkeepers-2.28.0.jar alongside (from the Oct 15 backup set):

- Boot clean: `Done (38.470s)`, ZERO `SEVERE` lines in the full log. The
  only ERROR lines were environmental (sandbox blocks Mojang/Paper
  version-fetch SSL calls) plus a flat-world generator notice; none were
  from either plugin.
- Ready line logged exactly as designed: "ExcelQuartermaster enabled:
  'Old Quartermaster' will tell his tales (11 tales, anchor on: world
  -783,81,-299 r10, cooldown 8s, 0 players with saved progress)."
- Default config copied into plugins/ExcelQuartermaster/ byte-identical;
  player-tales.txt created on disable (empty — no players joined).
- A villager was summoned at the anchor and verified by console:
  CustomName '{"text":"Old Quartermaster"}', Pos [-782.5, 82.0, -298.5]
  (inside the radius). Clean shutdown, plugin disabled without error.
- Boot log kept at scratch/boot-0.1.0.console.log.

What scratch could NOT prove (honest list): a real click flowing through
the event. There was no client or bot in the scratch world, so the
cancel-vs-Shopkeepers interplay (our listener runs at HIGH priority and
cancels; Shopkeepers must honor that cancellation for the trade window to
stay shut) and the Bedrock/Geyser tap path remain LIVE checks for install
day, alongside the tale actually appearing in a player's chat. The sneak
passthrough is safe by construction (we simply never touch sneak events),
and all decision logic behind the click is unit-tested.

## Status

Built + unit-tested + scratch-verified Oct 18, 2026. INSTALLED LIVE on
Wink Reyes Valley Oct 18, 2026, ~11:37 AM CDT (boot-verified; see
INSTALLED.txt for the full install record). Still owed: the install-day
player click check (a real tap on the Old Quartermaster — tale appears,
empty trade window stays shut, cooldown and per-player rotation behave),
plus publishing (this record).
