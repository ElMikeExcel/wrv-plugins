# FREEZE SCOPING — can EliteMobs lock its own progression gains? (P-SEALFREEZE-1, half 2)

**Verdict: NO native EliteMobs lock covers the freeze. Build the enforcer.**
EliteMobs offers feature-level off-switches (kill a whole system) and
single-source taps (stop one income stream), but nothing that holds all
three accrual classes — level (HighestLevelKilled / kill stats), skill XP,
and currency — while play continues. The enforcer is built:
`ExcelSealFreeze-0.1.0.jar` (see BUILD-RECORD.md).

**Evidence base:** the live config set (`~/workspace/elitemobs-update-10.9.10/live-config-set/EliteMobs/`,
52 entries — the config surface of the live EM 10.9.x line; the G4 pull's own
EM files — `AdventurersGuild.yml`, `dungeons.yml`, `initialize.yml`,
`ValidWorlds.yml` — agree with it), plus the live `player_data.db` schema
(G4 pull, sha256 `b9d251a0…`: `PlayerData` has NO combat-level column; the
"level" signal is `HighestLevelKilled`, and EM's combat level is a derived
display value — skills.yml: "Combat level is the average of the two highest
weapon skills and armor" — computed, never stored).

## Candidate-by-candidate coverage

| # | Candidate (file → key) | What it actually does | Level | Skill XP | Currency | Play cost | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | `skills.yml` → `skillSystemEnabled: false` | Disables the whole skill leveling system: no skill XP — and no skill bonuses (armor health bonus, perks) either | ✗ (HLK/Kills are mob-death stats, recorded outside the skill system) | ✓ | ✗ | HIGH — strips EM skill mechanics from the seal fights themselves | Not the freeze |
| 2 | `skills.yml` → `skillWorldExclusions: []` | Skill system inactive per listed world | Partial, per-world only | Partial, per-world only | ✗ | The fights happen in the main worlds; would need every world listed = candidate 1 with extra steps | Not the freeze |
| 3 | `EconomySettings.yml` → `enableEconomy: false` | EM economy off (coins, shops, guild ranks). Its own comment: "If disabled, players will not be able to progress in the plugin!" | ✗ | ✗ | ✓ | HIGH — EM shops and guild-rank surfaces close during the exact window the guild hub matters | Not the freeze |
| 4 | `EconomySettings.yml` → `enableCurrencyShower: false` (+ `currencyShowerTierMultiplier`) | Elites stop dropping pickup coins | ✗ | ✗ | Partial — coin drops only; selling elite loot to EM shops still credits (materialWorth), gambling still moves money | None | A tap, not a lock — see the Vault note below for its one legitimate use |
| 5 | `MobCombatSettings.yml` → `doNaturalEliteMobSpawning: false` | Natural elites stop spawning | Partial — removes the main HLK source, but spawner/custom/dungeon elites remain and still accrue | Partial (same) | Partial (same) | HIGH — the world's elites vanish; play itself is gutted, violating Mike's "play is NOT blocked" | Not the freeze |
| 6 | `initialize.yml` → "No Progression Preset" | Preset: "Disables elite spawns, loot drops, the skill system, and leveling" + "Most features disabled" | ✓ by demolition | ✓ by demolition | ✓ by demolition | TOTAL — it is EM's own everything-off mode | Not the freeze |
| 7 | `AdvancedCombatSystem.yml` → `enabled: false` | EM class/combat system off (abilities, HUD) | ✗ not provable (kill stats are recorded by mob-death handling, not the ACS) | ✗ not provable | ✗ | HIGH — class abilities are play | Not the freeze |
| 8 | `AntiExploit.yml` → farming protection etc. | Suppresses *special loot* under farm conditions (kills/min/level-delta thresholds) | ✗ | ✗ | ✗ | None, but it is a loot filter, not an accrual lock | Not the freeze |

No combination of the partial toggles closes the set either: 1+3 together
still leave HLK/Score/Kills accruing from every elite kill, and that pair
already costs more play (skills + shops + guild ranks) than Mike's law
allows. **Config route rejected on evidence; enforcer built.**

## The unlock flag — authoritative source (named for the enforcer)

**`plugins/ExcelGuildGate/state.yml`, top-level key `unlocked` (boolean).**
- GuildGate's own code (both the live-lineage source trees on disk,
  `~/workspace/excelguildgate/src/…/ExcelGuildGate.java` and `SOURCE-0.3.4`)
  writes `y.set("unlocked", state.isUnlocked())` on EVERY state save, with
  `unlockTimestamp` beside it and per-seal `seals.<id>.broken` records below.
- Derivation (GuildGate `SealState`): unlocked exactly when every enabled
  seal is broken. The Warden is the fifth seal; at 5/5 the flag flips in the
  same save that credits the Warden break — so the enforcer's read is never
  ahead of or behind GuildGate itself.
- BossTrial state was considered and rejected as the source: BossTrial
  tracks encounters/rewards; GuildGate is the seal authority the whole
  server (menus, PlaceholderAPI `%guildgate_unlocked%`) already reads.
- Fail-safe reading rule: only a column-0 `unlocked: true` counts; a
  missing/unreadable file reads as still locked (the enforcer's SealFlag,
  unit-tested, including the nested-lookalike case).

## The Vault caveat (currency honesty)

Live `EconomySettings.yml` has `useVault: true`, yet EM's DB still carries
`CurrencyV2`/`CurrencyCents` balances (Marvin: 7.0 / 700 at the pull). Two
readings are possible — EM dual-writes its columns while Vault is the spend
path, or the columns remain EM's own ledger with Vault used for interop.
The desk evidence cannot settle which; **the rig plan settles it
empirically** (RIG-ACCEPTANCE-PLAN step C-3: pick up a coin during the
freeze and watch both the EM columns and the Vault/Essentials balance).

Consequences, stated plainly:
- The enforcer clamps EM's currency columns — that part of the freeze is
  exact regardless.
- If the rig shows coin pickups ALSO crediting the Vault balance, the
  freeze's currency class has a leak that a database clamp cannot close
  (a Vault-wide clamp would freeze ALL server money, including BossTrial
  seal cash — which Mike's design keeps paying during this very window,
  Warden night included). The play-safe complement for that case is
  candidate 4 (`enableCurrencyShower: false` for the freeze window only:
  coins stop dropping, everything else — shops included — keeps working),
  recommended to the supervisor as a config rider IF the rig proves the
  leak. It is deliberately not bundled into this build unproven.

## Enforcer design (as built — BUILD-RECORD.md)

Standalone plugin `ExcelSealFreeze 0.1.0`, no dependency on ExcelExpansions
(its level store simply seeds from the wiped zeros and reads clamped values
while the freeze runs — no watcher change needed):
- **Arm** (`/sealfreeze arm`, op): snapshots every row's progression columns
  into `plugins/ExcelSealFreeze/baseline.txt`. Armed in the wipe window,
  the snapshot IS the post-wipe zeros.
- **Clamp**: async cycle (default 60 s) + join/quit single-row passes,
  restoring any progression column that EXCEEDS baseline back TO baseline —
  never below it (a death-driven negative Score is left alone), never
  touching Deaths/gambling/quest columns, all writes in one transaction
  with rollback, SQLite busy-timeout skip-on-busy.
- **Auto-disarm**: the first cycle that reads GuildGate `unlocked: true`
  disarms permanently and logs the lift. Manual `/sealfreeze disarm` and
  `/sealfreeze status` for operators.
- **Fail-safe**: EM DB missing/busy/corrupt → log + skip the cycle; seal
  file unreadable → still locked; baseline missing while armed → disarm
  rather than clamp blind. The plugin cancels no events and does no
  main-thread database work: it cannot lock or lag the server.
- **Windows-safe writes**: own files are written with file-level fsync
  only — never directory fsync (the P-FSYNC-1 lesson), so it boots clean
  on the PC mirror rig.
