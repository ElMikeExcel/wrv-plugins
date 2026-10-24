# ExcelDeathRecord 0.2.0

The death ledger + Burn Ledger for Wink Reyes Valley (Paper 26.2,
Java 21 bytecode). Owner's purpose, in his words: *"a plugin that
logs what people have at the point of death before it hits the
ground."*

## The two ledgers

**Death ledger** — on every `PlayerDeathEvent` (MONITOR, read-only,
fully guarded so a capture error can never break a death), a
snapshot is built from the event itself: the final drop list plus
the player's inventory/armor/offhand read AT the event — never
from item entities in the world. A death that destroys its drops
(lava, fire, void, explosion) therefore produces a record exactly
as complete as a normal death; the last damage cause (DamageCause
name, e.g. `LAVA`) is labeled in every record. One file per death:

```
deaths/<player-uuid>/<yyyy-MM-dd_HHmmss>.yml
```

Each item is stored byte-exact (`ItemStack.serializeAsBytes` as
Base64 — enchantments, damage, names, components all reproduce)
beside a human-readable summary (id, count, enchants, damage,
custom name).

**Burn Ledger** — item stacks destroyed in the world by LAVA,
FIRE, or VOID, attributed to their owner as far as the platform
knows (`Item#getOwner`, else the plugin's `PlayerDropItemEvent`
tracking, else honestly *unknown*). One file per destruction:

```
burned/<owner-uuid|unknown>/<yyyy-MM-dd_HHmmss>.yml
```

Capture paths, deduped to exactly one record per item by the
core `DestructionGuard`: `EntityRemoveEvent` (DEATH removals
take their cause from the item's last damage; OUT_OF_WORLD is
VOID) · a lethal-damage follow-up (`EntityDamageEvent` at
MONITOR serializes the stack at the blow; a next-tick check
records it if the entity is gone) · a half-second sweep for
items below their world's minimum height (VOID). Natural
despawn is recorded only for enchanted or custom-named items;
plain despawn stays noise.


## Restore (0.2.0)

Ops only (`exceldeathrecord.restore`, default op; console always
allowed). Gives back the byte-exact copies saved in a record.

### Typed

```
/deathrecord <player> [page N]                 list deaths, 10 a page (1 = newest)
/deathrecord <player> <n>                       one death, with item numbers #1, #2, ...
/deathrecord burned [player] [page N]           Burn Ledger (numbers are ledger-wide)
/deathrecord restore <player> <n> <items> [to <player>] [xp] [force]
/deathrecord restore burned <n> to <player>
```

`<items>` is `all`, `3`, `2,3,7`, `1-5`, or a mix like `1-3,7`.
`<n>` may also be the record id (`2026-10-23_221412`), which never
shifts when a newer death arrives. Examples:

```
/deathrecord restore Alex 1 all              everything from Alex's newest death, back to Alex
/deathrecord restore Alex 1 all xp           same, plus the XP Alex had
/deathrecord restore .BedrockAlex 1 3 to Sam item 3 goes to Sam instead
/deathrecord restore Alex 1 2,3,7 to Sam
/deathrecord restore Alex 2 1-5
/deathrecord restore burned 4 to Sam
```

Names: the real player name works best (Bedrock names start with
a dot). Online players can also be matched by display name or
Essentials nickname, and an offline player by a unique Essentials
nickname.

### Tap menu

`/deathrecord restore <player>` (in game, as an op) opens a picker:
1. deaths (time in `display-zone`, cause, item count),
2. items (tap to select/unselect: glint + "Selected"; already-restored
   items are shown and can't be picked; "Restore all"),
3. who gets them (the dead player by default, or anyone online),
4. confirm: "Give Netherite Shovel to Sam?", the item list, the
   slots needed vs free, an XP switch when it applies, Yes / Cancel.

Java gets chest screens; Bedrock (Floodgate) gets forms, with a
toggle per item. Every screen shows the typed equivalent, e.g.
`Typed: /deathrecord restore Alex 1 3 to Sam (1 = newest death, 3 = item number)`.

### Rules

- **All or nothing.** Before giving, the plugin works out the empty
  slots needed (counting merges into stacks they already hold, and
  armor/offhand slots on a same-player restore). If it won't fit,
  nothing is given or marked: `Can't restore: Sam needs 4 empty
  inventory slots and has 1. Ask them to clear 3 more, then try again.`
- **Once only.** Each restored item is marked inside its record
  (`restored-times/-to/-to-uuid/-by/-epoch/-at`); views show
  `[restored to Sam 2026-10-24 01:32:40 UTC by OpSam]`. Restoring it
  again needs `force` (typed only). The mark is written before the
  give.
- Same-player restore puts armor and offhand back into those slots
  when they are empty. XP (optional, same player only) gives back
  at least the level they had at death; it is marked too.
- What is restored is the drop list. If nothing dropped
  (keepInventory), the carried list is offered, but only with `force`,
  since the player may still have it.
- The record is re-read at confirm time, so two staff can't double-give.

### restores.csv

`plugins/ExcelDeathRecord/restores.csv`, one row per item (plus one
for XP): `time_utc,staff,source_player,death_file,item,target,item_no,count,source,forced`.
Fields are RFC-4180 quoted and formula-guarded. ExcelAuditLog 0.2.0
does not read this file yet.

### Rollback safety

0.1.0 reads 0.2.0 records unchanged: restore marks are extra item
keys it ignores (`run-compat.sh` proves it against the 0.1.0 source).

### Also fixed

The Burn Ledger no longer logs vanilla `/give`'s throw-away display
item as a DESPAWN (anything under 5 s old is skipped).

### Config

`config.yml`: `display-zone: America/Chicago` (times shown in chat and menus).

### Build / test

```
./build.sh                # JDK=/path/jdk25 LIBS=/path/paper-26.x-libs
./run-tests.sh            # plain-JVM suites
./run-compat.sh <0.1.0-source>
```

## Lookup commands (permission `exceldeathrecord.admin`, default op)

```
/deathrecord <player>        recent death snapshots for a player
/deathrecord <player> <n>    the n-th most recent snapshot, item by item
/deathrecord <player> page N older deaths (10 a page)
/deathrecord burned [player] [page N]  Burn Ledger destructions
```

Restoring is covered above (`/deathrecord restore ...`).

## Layout

- `src/com/winkreyes/exceldeathrecord/` — Bukkit adapter (plugin
  main + death listener + command, `BurnWatch`, `ItemCapture`,
  `RecordStore`, 0.2.0: `RestoreService`, `RestoreMenu`,
  `GeyserForms`, `Names`)
- `src/com/winkreyes/exceldeathrecord/core/` — pure logic, no
  Bukkit: record models, file formats, index/summary lines, the
  snapshot builder, burn-cause mapping, attribution resolution,
  the exactly-once guard; 0.2.0: `Selection`, `RestoreArgs`,
  `SlotPlanner`, `SlotMatch`, `RestoreCsv`, `XpMath`
- `tests/` — plain-JVM suites (`./run-tests.sh`)
- `./build.sh [out.jar]` — deterministic build (fixed jar
  timestamps; A/B rebuilds are byte-identical)

Serialization (reading item state + bytes) happens on the main
thread at the event; only the file write of already-formatted
text is async, serialized under one lock so same-second records
can never collide on a file name.

Live proofs were run on a scratch Paper 26.2 server with
fake players; that tooling never ships.
