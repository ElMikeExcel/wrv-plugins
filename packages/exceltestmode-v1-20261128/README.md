# ExcelTestMode v1

**A fleet testing tool. Not a gameplay feature.**

ExcelTestMode is a small standalone Paper plugin that gives an
operator a *damage floor* for proof and test sessions: a player on
its list takes full, real damage — every hit fires and counts
exactly as stock, and combat state, aggro and heal mechanics
engage untouched — but the portion of a hit that would carry them
below the configured health floor (shipped 2.0 HP, one heart) is
trimmed, so the hit lands them exactly **on** the floor. A listed
player can be hurt, never killed, while a mechanic is being
measured. It exists so a staged test character can survive its own
measurement.

## Why it exists

On Nov 27, 2026, a live-proof session on the Wink Reyes Valley
server staged the owner's own character against an EliteMobs boss
to measure the boss's combat-end heal. The character **died
mid-test**, the session lane driving the test was cut by a
scheduler timeout, and the account had to be restored from
baselines before it was safe to use again. The owner's proposal
the same night: a test mode where the test player still takes
damage but cannot die. A proof session's whole value is real
numbers — real hits, real combat state, real heal timings — so
the mode must not soften the fight; it may only refuse the death.
That is why the adjustment is a floor clamp and not a damage
reduction: damage that fits above the floor passes completely
untouched, and only the lethal excess is trimmed.

## History

This code shipped for one evening as the *test-mode module*
inside **ExcelEMFixes v31** (Nov 27, 2026). Mike's ruling the
same night: ExcelEMFixes is the EliteMobs fix pack, and test mode
fixes nothing — it is a fleet testing tool, so it does not belong
in the fix pack. **ExcelEMFixes v32** removed the module, and
this standalone plugin is the code's home from v1 on. Behavior is
identical to the module's; only the packaging changed (its own
plugin, its own config file with the same keys at the root, the
permission node renamed to `exceltestmode.admin`).

## How it works

- A victim-axis `EntityDamageEvent` handler at **HIGHEST**
  priority (the TrainingWheels shield shape — the D208
  per-player mitigation precedent): it prices the event's
  **final** damage, after armour and resistance have settled
  it, and when a hit would break the floor it rescales the
  event's base damage through the event's own final/base ratio
  (plus one settle pass) so the settled hit lands the player
  exactly on the floor.
- The event is **never cancelled**. A player already at or below
  the floor takes 0-damage hits — the events still fire — until
  they heal back above it.
- No resistance/absorption effects are granted, no side
  channels, and **outgoing damage is never touched**.
- Causes `VOID` and `SUICIDE` are excluded by shipped default
  (configurable): no floor holds the void, and `/kill` is an
  operator's own act.
- There is no world scope: the player list is the scope.

## Use

The plugin ships **OFF** (`enabled: false`) with an **empty
list**: it loads, logs one line, and does nothing at all — no
handler, no command — until an operator enables it for a test
session.

1. Set `enabled: true` in `plugins/ExcelTestMode/config.yml`
   and restart the server.
2. `/testmode <player> on` — list a player (console, or in game
   with `exceltestmode.admin`, default op).
3. Run the test. `/testmode status` shows the floor and the
   listed players; `/testmode <player>` reads one player.
4. `/testmode <player> off` — unlist. Toggles write through to
   `config.yml` (the `players` list, stored as UUIDs) so the
   list survives restarts, and every toggle logs one line
   (player, on/off, floor, ISO timestamp, issuer).

Remove the last player, or set `enabled: false` and restart, and
everyone is back to stock behavior exactly.

## Config (config.yml, shipped defaults)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `false` | Master switch. Off = fully inert. |
| `floor-hp` | `2.0` | Health floor in HP (one heart). Valid (0, 100]; anything else falls back to 2.0. |
| `players` | `[]` | Protected players (UUIDs; a plain name also resolves at load). Managed by `/testmode`. |
| `excluded-causes` | `[VOID, SUICIDE]` | Damage causes the floor never applies to. |
| `debug` | `false` | Log every clamped hit. |

No shipped default names any server, world, player or content —
the list starts empty on purpose.

## Dependencies

None. Plain Bukkit/Paper API only — no EliteMobs dependency, no
softdepends. Paper 26.3 (API 1.13+ declaration), Java 21.
