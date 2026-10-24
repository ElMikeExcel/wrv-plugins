# COMMANDS-CATALOG — the server's player command list (WRV-058)

**What this is:** the curated catalogue behind the "Server Commands" menu
button, the DeluxeMenus `commands_menu`, and ExcelBedrockMenu 1.5.4's
`/servercommands`. One catalogue, three surfaces, kept in sync by an
automated cross-check (the Java `CATALOG` literal, the menu generator
table and this document all carry the same entries).

**Install note for the Java surface:** DeluxeMenus 1.14.1 only loads
menus registered in `plugins/DeluxeMenus/config.yml` under `gui_menus:`;
the install adds a `commands_menu` entry there (see
MENU-DIFF-058-059.md). Proven in the scratch boot test: unregistered,
the file is ignored; registered, `dm list` shows it.

**How it was built:** the `plugin.yml` of every installed plugin was read
and every declared command reviewed (jars from the Oct 15 pre-guild full
backup, `~/workspace/backups/pre-guild-20261015/root/plugins/`), plus our
own plugins' current sources (ExcelBedrockMenu 1.5.3, ExcelGuildGate
0.1.0, ExcelMenuJoin 1.3.0, ExcelShops 1.2, ExcelRewardGuard 1.1/1.2) and
the live DeluxeMenus menu files (the `/shop` entry is a DeluxeMenus
open-command, proven at `plugins/DeluxeMenus/gui_menus/spawnshop.yml`
line 3: `open_command: shop`).

**Player commands only.** Anything requiring an admin permission,
moderation, economy-admin or setup is excluded. **Tap** below means the
entry is safe to run with no typed arguments (menus run it for you);
**List** means display-only (type it in chat with its arguments).

## The catalogue: 42 entries (32 tap, 10 list-only)

### Getting Around (8)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/menu` | ExcelBedrockMenu | Open the server menu | Tap |
| `/spawn` | EssentialsXSpawn | Teleport back to the village spawn | Tap |
| `/tpmenu` | ExcelBedrockMenu | Pick a player to send a teleport request to | Tap |
| `/tpa <player>` | EssentialsX | Ask to teleport to a player | List |
| `/tpaccept` | EssentialsX | Accept a request (/tpdeny says no) | Tap |
| `/warps` | EssentialsX | List the server warps (/warp <name> to go) | Tap |

### Money & Jobs (7)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/balance` | EssentialsX | See how much money you have | Tap |
| `/pay <player> <amount>` | EssentialsX | Send money to another player | List |
| `/balancetop` | EssentialsX | The richest players on the server | Tap |
| `/jobs` | Jobs Reborn | Browse jobs and join one | Tap |
| `/jobs stats` | Jobs Reborn | Your job levels, XP and pay rates | Tap |
| `/jobs join <job>` | Jobs Reborn | Join a job by name (or browse with /jobs) | List |

(`/jobs` in the menus runs `jobs browse`, the same dispatch the main
menu's Jobs & Money button has always used.)

### Skills & Levels (5)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/mcstats` | mcMMO | Your mcMMO skill levels | Tap |
| `/mctop` | mcMMO | The top mcMMO players | Tap |
| `/<skill name>` | mcMMO | Inspect one skill: /mining, /swords, /axes, /fishing and more | List |
| `/party` | mcMMO | Your mcMMO party: members, chat and invites | Tap |
| `/leaderboard` | ExcelLeaderboard | Top players on every board | Tap |

### Shops & Trading (5)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/shop` | DeluxeMenus | Open the Spawn Shop | Tap |
| `/excelshop setup` | ExcelShops | Start the guided setup for your own chest shop | Tap |
| `/excelshop manage` | ExcelShops | Manage your own chest shop | Tap |
| `/deathprotection status` | ExcelDeathProtection | See if your items are protected from death | Tap |
| `/mobveil status` | ExcelMobVeil | See how much Mob Veil time you have left | Tap |

### Claims & Land (7)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/claim` | GriefPrevention | Claim the land around you | Tap |
| `/claimslist` | GriefPrevention | Your claim blocks and where your claims are | Tap |
| `/trust <player>` | GriefPrevention | Give a player full access to your claims | List |
| `/extendclaim <blocks>` | GriefPrevention | Grow the claim you are standing in | List |
| `/buyclaimblocks <amount>` | GriefPrevention | Buy more claim blocks with server money | List |
| `/trapped` | GriefPrevention | Escape if you are stuck inside a claim | Tap |
| `/abandonclaim` | GriefPrevention | Delete the claim you stand in (no undo) | List |

(`/abandonclaim` is list-only on purpose: deleting land protection
should always be a typed decision, never a stray tap.)

### Guild & Quests (4)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/ag` | EliteMobs | Teleport to the Adventurers Guild | Tap |
| `/seals` | ExcelGuildGate | Road to the Guild: the five seals quest (also /road) | Tap |
| `/em` | EliteMobs | EliteMobs: your elite level, gear skills and menus | Tap |
| `/em party` | EliteMobs | Dungeon parties: add create, invite or ready | List |

### Server Info (9)
| Command | Plugin | What it does | |
|---|---|---|---|
| `/motd` | EssentialsX | Server info and news | Tap |
| `/list` | EssentialsX | See who is online right now | Tap |
| `/msg <player> <message>` | EssentialsX | Send a private message (/r replies) | List |
| `/playermap` | ExcelPlayerMap | How player maps work and who is on them | Tap |
| `/compass` | ExcelMenuJoin | Get a new server menu compass | Tap |
| `/pouch` | ExcelPouch | Park your menu compass and Magic wand; contents survive death | Tap |
| `/menujoin` | ExcelMenuJoin | Turn the auto-open menu on or off | Tap |
| `/servercommands` | ExcelBedrockMenu | This command list | Tap |
| `/serverwebsite` | ExcelBedrockMenu | Get the serve
...[truncated 5014 chars]

**Oct 18, 2026 (1.5.9):** `/sethome` and `/home` removed from the catalogue on all
surfaces - homes were removed server-wide on Oct 11, 2026, so both entries were
dead ends. `/sell hand` removed by Mike's decision the same day: an Essentials
sell-to-server faucet would bypass the player-shops economy and Jobs balance.
The catalogue now carries 41 entries. `/menujoin` stays - the auto-open toggle
is unchanged everywhere.

**Oct 21, 2026 (1.5.13):** `/pouch` (ExcelPouch) added to Server Info,
immediately after `/compass`, runnable on all surfaces. The Utility Pouch
main-menu button shipped in 1.5.11 (Bedrock) alongside the Java menu's own
Pouch button, but the command never joined the catalogue, so a player
browsing Server Commands found no pouch anywhere (found live by Mike).
The catalogue now carries 42 entries.
