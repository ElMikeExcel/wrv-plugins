# INSTALL PACKET — WRV-058 Commands button + WRV-059 Website button

Status: **BUILT, TESTED, BOOT-VERIFIED LOCALLY. NOT INSTALLED.**
Requested by Mike, Oct 16, ~1:39 AM. This packet is **third in tonight's
install chain**: (1) GuildGate packet, (2) ExcelRewardGuard 1.2, (3) this
one. Do not install it before both earlier markers exist:

- `~/workspace/excelguildgate/install-prep/INSTALLED.txt`
- `~/workspace/excelrewardguard/install-prep-1.2/INSTALLED-1.2.txt`

It is based on the post-GuildGate state on purpose: the jar is built on
the 1.5.3 sources and the menu file starts from the GuildGate packet's
prepared `server_menu.yml`, because that is what will be live when this
packet's turn comes.

## What it adds

- **Server Commands button** (WRV-058): a curated list of the server's
  44 player commands in 7 groups (COMMANDS-CATALOG.md). Java: DeluxeMenus
  `commands_menu` chest menu, opened from server-menu slot 9. Bedrock:
  "Server Commands" button in `/menu` (after Server Info), opening
  two-level Cumulus forms: a group picker, then per group a form whose
  content lists every command in words and whose buttons run the
  bare-safe ones (34 run, 10 are display-only because they need an
  argument). New command `/servercommands`, alias `/cmds` (collision-
  checked against every installed plugin.yml; nothing else claims it).
  Java players running it get the compact ExcelLeaderboard-style chat
  panel (pass a group number or name, e.g. `/cmds jobs`).
- **Server Website button** (WRV-059): server-menu slot 17 (Java) and a
  "Server Website" button right after Server Commands (Bedrock), both
  for https://elmikeexcel.github.io/winkreyesvalley/. Java menu click
  sends a `[json]` clickable link line plus a `[message]` raw-URL line.
  New command `/serverwebsite`: Java players get a clickable chat link;
  Bedrock players get a Cumulus form spelling the address in words plus
  a chat copy. **Bedrock cannot open a browser from in game** — the
  form says so and gives the address to type; nothing fakes otherwise.

## Files in this packet

| File | SHA-256 | Notes |
|---|---|---|
| ExcelBedrockMenu-1.5.4.jar | `17cbec0cf0a953d34eaf1d7e49e401f2ca4d66df99ca3b144ce334d1c5edeb64` | Built twice, byte-identical |
| server_menu.yml | `a04f258b5148a31c0bdfa96dda4500f5464c5fa553779073bb4ff3755633cb4c` | GuildGate packet file + exactly 2 items |
| commands_menu.yml | `10da5097b87fc2303959dc96fb14f94931c265cad123fae5a515ff37ab8c53bf` | NEW file, generated from the catalogue |
| COMMANDS-CATALOG.md | — | The inventory, curation and exclusion record |
| MENU-DIFF-058-059.md | — | Exact menu diffs + the config registration |
| INSTALL-PACKET.md | — | This file |

Base for comparison: ExcelBedrockMenu-1.5.3.jar (GuildGate packet)
SHA-256 `b0801922c85a07bc501d3233a4795ff7713213cb72b6cf4c7c1f4afa4d3fca36`.

## The 1.5.4 build

- Sources: the 1.5.3 source tree
  (`~/workspace/renames-2026-10-15/src/ExcelBedrockMenu`, the same tree
  the GuildGate packet's 1.5.3 was built from — it carries the dynamic
  guild label via GuildGateAPI reflection and the seals button), copied
  to `~/workspace/menubuttons-058-059/src/ExcelBedrockMenu`. No
  decompilation was needed; the jar's compiled main class still matches
  that source (CRC-compared below).
- All new code lives inside the main class: the two buttons, the two
  commands, the catalogue (44 entries), the Bedrock forms, the Java chat
  panels. No new files in the jar; entry set identical to 1.5.3.
- Geyser/Floodgate/Cumulus stay reflection-only, same as the rest of the
  plugin (house rule: never a hard dependency).

## Verification performed (Moon's playbook)

1. **Automated tests: 682 assertions, all passing**, on both builds
   (`tests/run-tests.sh`): catalogue has exactly 44 entries and group
   sizes 8/7/5/5/7/4/8; every entry appears in exactly one group; the
   display-only set is exactly the 10 argument commands; an admin
   denylist (~90 tokens: /op, /stop, /ban, /give, economy-admin, claim
   admin, EliteMobs admin, LuckPerms, /dm, our own admin commands…)
   appears in no display or run token; every tap-to-run command is a
   real player command of an installed plugin (each is listed with its
   source in the catalogue); Bedrock form mapping round-trips (group
   buttons 0..6, per-group run buttons map back to their commands, and
   button order equals catalogue order); Java panel line budget
   (overview 9 lines, widest group 10 lines); Bedrock button order
   source check (…, Server Info, Server Commands, Server Website,
   Auto-open…); group lookup by number and by name prefix.
2. **Rebuild reproduction:** two independent builds of the jar are
   byte-identical (SHA above, both).
3. **Change scope vs 1.5.3:** entry-by-entry CRC comparison — only
   `ExcelBedrockMenu.class` and `plugin.yml` differ; zero entries
   added or removed. `plugin.yml` diff is exactly: version 1.5.3 →
   1.5.4, plus the `servercommands` (aliases `[cmds]`) and
   `serverwebsite` command blocks.
4. **Button order, compiled:** `javap -c` on the shipped jar's main
   class lists the menu buttons in order …, Server Info, Server
   Commands, Server Website, Auto-open on join, and the onCommand cases
   …, motd, servercommands, serverwebsite, menujoin.
5. **Menu cross-check (`build/verify_menus.py`):** both prepared YAML
   files parse; the server_menu diff against the GuildGate packet file
   is exactly the two new items (title, size and all 10 existing items
   compared deep-equal); commands_menu carries the same 44
   (command, group, tap-to-run) triples as the Java catalogue, with lore
   matching the catalogue descriptions; plugin.yml is 1.5.4 with both
   commands and the `cmds` alias.
6. **Scratch boot test** (Paper 26.2-123, JDK 25, the packet jar +
   DeluxeMenus 1.14.1 + PlaceholderAPI 2.12.3 + ExcelGuildGate 0.1.0,
   both prepared menu files): ExcelBedrockMenu enables **as v1.5.4**;
   console `servercommands`, `cmds` and `serverwebsite` each answer
   "Only players can use …"; `dm reload` clean; server reaches Done and
   stops clean. The boot test **caught a real defect in the original
   packet**: DeluxeMenus 1.14.1 only loads menus registered in
   `config.yml`'s `gui_menus:` section, so `commands_menu.yml` alone
   would have been silently ignored (first boot: only the 3 example
   menus loaded, `dm list` showed neither packet file). With the
   registration step added, the same setup loads 5 menus and `dm list`
   lists both `server_menu` and `commands_menu`. That step is now in
   the install steps below and in MENU-DIFF-058-059.md. (Baseline
   noise seen and expected: the DeluxeMenus NMS-hook warning, the
   basics_menu 'data'-option deprecation warning, offline-network
   errors from Paper/PAPI version checks.)
7. **DeluxeMenus `[json]` support** for the website button: verified
   against the installed 1.14.1 jar — the click-action code contains a
   JSON send path (`sendJson`/`fromJson`) and MiniMessage is shaded in;
   `[json]` is also in DeluxeMenus' documented action list. The button
   additionally sends a plain `[message]` raw-URL line, so a working
   link does not depend on the JSON form.

## Install steps (when its chain turn comes; server must be empty)

1. Confirm both chain markers above exist, and confirm the live jar is
   ExcelBedrockMenu 1.5.3 and the live `server_menu.yml` matches the
   GuildGate packet's prepared file. If the menu file has drifted,
   apply the two item blocks from MENU-DIFF-058-059.md by edit instead
   of swapping files.
2. Panel Shut Down (never console `stop`).
3. Backups, kept beside the live files:
   - live jar → `ExcelBedrockMenu-1.5.3.jar.bak-20261016`
     (or rename in place and keep it in the plugins folder, one active
     jar only)
   - `gui_menus/server_menu.yml` → `server_menu.yml.bak-20261016-menu058`
   - `DeluxeMenus/config.yml` → `config.yml.bak-20261016`
4. Upload `ExcelBedrockMenu-1.5.4.jar`; remove/rename the 1.5.3 jar so
   exactly one ExcelBedrockMenu jar is active.
5. Upload the packet `server_menu.yml` over
   `gui_menus/server_menu.yml`; upload `commands_menu.yml` into
   `gui_menus/` (new file).
6. Edit `DeluxeMenus/config.yml`: under `gui_menus:`, after the
   `server_menu` entry, add:
   `commands_menu:` / `file: commands_menu.yml` (2-space style, see
   MENU-DIFF-058-059.md).
7. Verify uploaded file sizes/hashes against the table above.
8. Panel Start. Verify in the log: ExcelBedrockMenu enables as v1.5.4;
   DeluxeMenus menu count is one higher than before the install (live
   8 → 9); no SEVERE; console `servercommands` and `serverwebsite`
   answer the players-only lines; `dm list` shows `commands_menu`.
9. Player checks (Mike, when next on): Java — server menu slots 9 and
   17 render, commands menu opens, a green entry runs its command, a
   grey entry prints its "type it in chat" hint, website button prints
   a clickable link. Bedrock — /menu shows Server Commands and Server
   Website after Server Info; the commands form lists groups, a group
   form runs a bare-safe command; the website form shows the address.

## Rollback (any step fails or checks fail)

1. Panel Shut Down.
2. Remove the 1.5.4 jar; restore `ExcelBedrockMenu-1.5.3.jar` from the
   backup name in step 3.
3. Restore `gui_menus/server_menu.yml` from
   `server_menu.yml.bak-20261016-menu058` (that restores the exact
   GuildGate-packet menu, including the guild lock pair and seals).
4. Restore `DeluxeMenus/config.yml` from `config.yml.bak-20261016`
   (removes the commands_menu registration) and delete
   `gui_menus/commands_menu.yml`.
5. Panel Start; verify 1.5.3 enables and the menu count is back to the
   pre-install number.

## Honest unverified list

- **Bedrock form rendering in game** — no Bedrock client exists in the
  sandbox; the forms use the same Cumulus reflection path and shape as
  the live-proven `/seals` forms, and the button→command mapping is
  unit-tested, but nobody has seen these two forms render yet.
- **Java `[json]` click behaviour in a real client** — the DeluxeMenus
  JSON path is verified in the installed jar's code and docs, and the
  fallback raw-URL line ships on the same button, but the click itself
  has not been clicked by a person yet.
- **DeluxeMenus menu tap-through in game** — `commands_menu` is proven
  registered and loaded headless (`dm list`), and its items mirror the
  `[player]` / `[message]` patterns the live menus use, but no player
  has tapped an entry yet.
- **`/servercommands` and `/serverwebsite` in-game output** — console
  behaviour is boot-verified; the Java chat panel and Bedrock forms are
  unit-tested but not yet seen by a player.
- Player-visible wording (descriptions, group names) is Argus's plain
  wording pass; Mike may want edits after seeing it in game. All of it
  lives in one catalogue table, so wording changes are one rebuild.

## Decisions made while building (flagged for Mike)

- **Groups:** the six suggested groups plus a seventh, **Claims &
  Land** (7 entries) — claims are daily life on this server and the
  GriefPrevention commands would have swamped Getting Around. Order on
  every surface: Getting Around, Money & Jobs, Skills & Levels, Shops
  & Trading, Claims & Land, Guild & Quests, Server Info.
- **Slots:** server-menu slot 9 (Commands) and slot 17 (Website) — the
  only two empty slots in the middle row; the row becomes a full strip
  and nothing existing moved.
- **`/shop` points at the DeluxeMenus spawn shop** (its live
  `open_command`), not the `/excelshop` help card; both are listed,
  labelled "Spawn Shop" and "Player Shops Help".
- **Catalogue judgement calls** (full reasoning in
  COMMANDS-CATALOG.md): `/rules` excluded (no rules file exists, the
  command errors), `/back` excluded (Mike's no-/back design), `/help`
  excluded (unfiltered firehose including admin commands),
  `/deluxemenus` excluded (the menu replaces it), `/kit` excluded (a
  default kit exists but no player permission is granted), kits, homes
  beyond 3 homes and other multi-home Essentials extras not listed.
- **`/cmds` alias:** verified free across every installed plugin.yml
  before registering it.
