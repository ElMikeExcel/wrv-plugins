# ExcelBedrockMenu 1.5.4 — "Server Commands" and "Server Website" buttons (WRV-058 / WRV-059)

> **PUBLISHED RECORD — wrv-plugins (open draft PR)**
>
> Prepared: October 16, 2026, for Wink Reyes Valley (WRV-058 +
> WRV-059). **1.5.4 is INSTALLED LIVE** — installed October 16,
> 2026, ~2:36 AM CDT by the empty-server auto-watch (install
> marker `INSTALLED-1.5.4.txt`, gate `list` 0/10 twice at
> 2:17:52 AM and 2:20:29 AM CDT; §7).
>
> **Checks stated as pending, here and in the website entry:**
> the Bedrock form rendering and the Java click behaviour have
> **not** been seen by any player yet. They stay **NOT DONE** in
> every field below until they happen in game: Bedrock —
> Server Commands form render and Server Website form page;
> Java — commands menu opens/runs entries and the website
> button gives a clickable link.

## Status at a glance

Statuses are kept separate, per the server rule. Do not collapse them.

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026, local build only |
| Automated tests | **682 assertions, all passing**, on both builds (catalogue, denylist, form mapping, panel budgets, button order) — see §5 |
| Test server (scratch boot) | **Passed** — packet jar + DeluxeMenus 1.14.1 + PlaceholderAPI + ExcelGuildGate 0.1.0, both prepared menu files; caught one real defect (menu registration), fixed in the packet (§5) |
| Installed live | **YES — October 16, 2026, ~2:36 AM CDT**, by the empty-server auto-watch, third in the Oct 16 chain (after the GuildGate campaign ~1:41 AM and ExcelRewardGuard 1.2 at 1:57 AM). Gate 0/10 twice; drift check passed; backups kept; `dm list` = 9 menus incl. `commands_menu`; both new console commands answer players-only; final `list` 0/10 (§7) |
| Java check | **NOT DONE YET** — no player has tapped the slot-9 / slot-17 items or a commands-menu entry (§10) |
| Bedrock check | **NOT DONE YET** — no player has seen the Server Commands forms or the Server Website form render (§10) |

## 1. Artifact

- **Plugin:** ExcelBedrockMenu (documentation label: ExcelBedrockMenu (BedrockMenu))
- **Version in this packet:** 1.5.4
- **Previous live version:** 1.5.3 (GuildGate campaign packet, live since Oct 16, 2026, ~1:41 AM CDT)
- **Jar:** `ExcelBedrockMenu-1.5.4.jar`
- **SHA-256:** `17cbec0cf0a953d34eaf1d7e49e401f2ca4d66df99ca3b144ce334d1c5edeb64`
  - Re-verified locally during this draft's preparation by hashing
    the packet jar — matches the install packet exactly.
  - Two independent builds produced byte-identical jars.
- **Main class:** `com.winkreyes.excelbedrockmenu.ExcelBedrockMenu`
  (unchanged; all new code lives inside it — no new jar entries)
- **Commands in `plugin.yml`:** `menu`, `tpmenu` (unchanged), plus
  **new** `servercommands` (aliases: `cmds`) and `serverwebsite`.
- **Data folder:** unchanged — `plugins/ExcelBedrockMenu/`; no
  migration or config change is involved in this bump.
- **Companion files (not in the jar):** a prepared
  `server_menu.yml` (GuildGate packet file + exactly 2 items,
  SHA-256 `a04f258b5148a31c0bdfa96dda4500f5464c5fa553779073bb4ff3755633cb4c`)
  and a **new** `commands_menu.yml`
  (SHA-256 `10da5097b87fc2303959dc96fb14f94931c265cad123fae5a515ff37ab8c53bf`),
  plus one registration edit to DeluxeMenus `config.yml` (§6).

## 2. What changed vs 1.5.3

Two new buttons, two new commands, one catalogue. Nothing else.

- **"Server Commands" button (WRV-058)** — in the Bedrock `/menu`,
  immediately after **Server Info**. It dispatches the new
  `/servercommands` command:
  - *Bedrock players* get two-level Cumulus forms: a group picker,
    then per-group forms whose content lists every command in words
    and whose buttons run the bare-safe ones.
  - *Java players* running the command get the compact
    ExcelLeaderboard-style chat panel (a group number or name
    narrows it, e.g. `/cmds jobs`).
- **"Server Website" button (WRV-059)** — immediately after
  Server Commands. It dispatches the new `/serverwebsite` command
  for https://elmikeexcel.github.io/winkreyesvalley/:
  - *Java players* get a clickable chat link.
  - *Bedrock players* get a Cumulus form spelling the address in
    words, plus a chat copy. **Bedrock cannot open a browser from
    in game** — the form says so and gives the address to type;
    nothing fakes otherwise.
- **The catalogue:** 44 curated player commands in 7 groups —
  Getting Around (8), Money & Jobs (7), Skills & Levels (5),
  Shops & Trading (5), Claims & Land (7), Guild & Quests (4),
  Server Info (8). 34 entries are tap-to-run; 10 are display-only
  because they need an argument. Admin/moderation/setup commands
  are excluded by design and pinned by a denylist test (~90 tokens).
  The full inventory, curation, and exclusion record is the
  packet's `COMMANDS-CATALOG.md`.
- **Button order in 1.5.4** (Bedrock menu): Spawn, Teleport to
  a player, Spawn Shop, Road to the Guild (1.5.3), Adventurers
  Guild (lock-aware label, 1.5.3), Jobs & Money, mcMMO Stats,
  Leaderboard, Server Info, **Server Commands** *(new)*,
  **Server Website** *(new)*, Auto-open on join. (The packet's §5
  records the compiled `javap` verification of the tail of this
  order: Server Info, Server Commands, Server Website,
  Auto-open on join.)
- **Files changed in source:** the main class only (the two
  buttons, the two commands, the 44-entry catalogue, the Bedrock
  forms, the Java chat panels) + `plugin.yml` (version 1.5.3 →
  1.5.4 and the two command blocks). Geyser/Floodgate/Cumulus
  access stays reflection-only (house rule).

## 3. Change-scope proof

- Entry-by-entry CRC comparison against the 1.5.3 jar
  (SHA-256 `b0801922c85a07bc501d3233a4795ff7713213cb72b6cf4c7c1f4afa4d3fca36`):
  **only** `ExcelBedrockMenu.class` and `plugin.yml` differ; zero
  entries added or removed. The jar's entry set is identical to
  1.5.3's.
- The prepared `server_menu.yml` was deep-compared against the
  GuildGate packet's file: title, size, and all 10 existing items
  are equal; exactly two items were added (§6).

## 4. Source of record

- **Source tree:** `~/workspace/menubuttons-058-059/src/ExcelBedrockMenu`
  — copied from the 1.5.3 tree
  (`~/workspace/renames-2026-10-15/src/ExcelBedrockMenu`). No
  decompilation was needed; the 1.5.3 jar's compiled main class
  matches that source (CRC-compared in the packet).
- **Install packet:** `~/workspace/menubuttons-058-059/install-prep/INSTALL-PACKET.md`
  (build, verification, install/rollback steps, honest unverified
  list, and the builder's flagged decisions).
- **Catalogue:** `~/workspace/menubuttons-058-059/install-prep/COMMANDS-CATALOG.md`
- **Menu diffs:** `~/workspace/menubuttons-058-059/install-prep/MENU-DIFF-058-059.md`
- **Boot logs:** `~/workspace/menubuttons-058-059/smoke-boot.log`
  and `smoke-boot-final.log`.

## 5. Build & verification (Moon's playbook)

From the install packet, which was written before any install:

- **Automated tests: 682 assertions, all passing, on both builds**
  (`tests/run-tests.sh`): catalogue has exactly 44 entries with
  group sizes 8/7/5/5/7/4/8; every entry in exactly one group; the
  display-only set is exactly the 10 argument commands; the admin
  denylist appears in no display or run token; every tap-to-run
  command is a real player command of an installed plugin (each is
  listed with its source in the catalogue); Bedrock form mapping
  round-trips and button order equals catalogue order; Java panel
  line budget (overview 9 lines, widest group 10); Bedrock button
  order source check; group lookup by number and by name prefix.
- **Rebuild reproduction:** two independent builds byte-identical
  (SHA-256 in §1).
- **Change scope:** §3.
- **Compiled button order:** `javap -c` on the shipped jar lists
  the buttons in the §2 order and the onCommand cases …, `motd`,
  `servercommands`, `serverwebsite`, `menujoin`.
- **Scratch boot test** (Paper 26.2-123, JDK 25, packet jar +
  DeluxeMenus 1.14.1 + PlaceholderAPI 2.12.3 + ExcelGuildGate
  0.1.0, both prepared menu files): ExcelBedrockMenu enables **as
  v1.5.4**; console `servercommands`, `cmds`, and `serverwebsite`
  each answer their players-only lines; `dm reload` clean; server
  reaches Done and stops clean.
  - **The boot test caught a real defect in the original packet:**
    DeluxeMenus 1.14.1 only loads menus registered in
    `config.yml`'s `gui_menus:` section, so the new
    `commands_menu.yml` alone would have been silently ignored
    (first boot: `dm list` showed neither packet file). With the
    registration step added, the same setup loads 5 menus and
    `dm list` lists both `server_menu` and `commands_menu`. The
    registration step is in the packet's install steps and in §6
    below — it is load-bearing, not optional.
- **DeluxeMenus `[json]` support** (the Java website button)
  verified against the installed 1.14.1 jar: the click-action code
  contains a JSON send path and MiniMessage is shaded in; `[json]`
  is in DeluxeMenus' documented action list. The button also sends
  a plain `[message]` raw-URL line, so a working link does not
  depend on the JSON form.

### Not verified by the build/test pass

- Bedrock form rendering in game (no Bedrock client in the
  sandbox) — the forms use the same Cumulus reflection path and
  shape as the live-proven `/seals` forms, and the
  button→command mapping is unit-tested, but nobody has seen
  these two forms render yet.
- The Java `[json]` click in a real client, and any DeluxeMenus
  tap-through in game (`commands_menu` is proven registered and
  loaded headless via `dm list`, but no player has tapped an
  entry yet).
- Player-visible wording (descriptions, group names) is a plain
  first pass; the owner may want edits after seeing it in game.
  All wording lives in one catalogue table, so changes are one
  rebuild.

## 6. Companion changes — Java menus (same packet, not in the jar)

- **`server_menu.yml`:** slot **9** gains the Server Commands item
  (KNOWLEDGE_BOOK, opens `commands_menu`) and slot **17** gains
  the Server Website item (COMPASS; click sends a `[json]`
  clickable link line plus a `[message]` raw-URL line). These were
  the only two empty slots in the middle row; nothing existing
  moved.
- **`commands_menu.yml` (NEW):** the DeluxeMenus chest menu
  carrying the same 44 (command, group, tap-to-run) triples as the
  catalogue, with lore matching the catalogue descriptions.
- **DeluxeMenus `config.yml` registration (required):** under
  `gui_menus:`, after the `server_menu` entry, add a
  `commands_menu:` / `file: commands_menu.yml` entry. Without it
  the new menu file is silently ignored (see the boot-test catch
  in §5). Exact formatting is in the packet's
  `MENU-DIFF-058-059.md`.

## 7. Install — EXECUTED, live October 16, 2026

- **Chain position:** third in the Oct 16 chain — (1) GuildGate
  packet (~1:41 AM CDT), (2) ExcelRewardGuard 1.2 (1:57 AM CDT),
  (3) this packet. Sequencing markers for both earlier installs
  were present before this install ran.
- **Route taken (the packet's plan, via the auto-watch):**
  empty-server gate — panel `list` 0/10 at 2:17:52 AM and again
  at 2:20:29 AM CDT (plus two extra 0/10 checks between);
  panel Shut Down; backups kept beside the live files
  (`ExcelBedrockMenu-1.5.3.jar.bak-20261016`,
  `server_menu.yml.bak-20261016-154`,
  DeluxeMenus `config.yml.bak-20261016-154`); live
  `server_menu.yml` passed the drift check against the
  GuildGate packet file before the swap; the 1.5.4 jar and both
  menu files uploaded (panel sizes confirmed: jar 21,047 B,
  server_menu.yml 3,675 B, commands_menu.yml 11,440 B); the
  `config.yml` registration applied and re-read verified
  (769 B = 724 original + 45 added); panel Start.
- **Live verification (panel, Oct 16 CDT):**
  `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.4`;
  "ExcelBedrockMenu enabled: /menu uses native Bedrock
  dialogs."; server Done (17.197s); `dm list` (2:36:48 AM) =
  9 menus — basics_menu, **commands_menu**, server_menu,
  spawnshop, spawnshop_building, spawnshop_enchanting,
  spawnshop_food, spawnshop_potions_protection,
  spawnshop_travel_animals — one more than the pre-install 8,
  with both `server_menu` and `commands_menu` present;
  console `servercommands` (2:37:24 AM) and `serverwebsite`
  (2:37:31 AM) each answered "Only players can use …"; final
  `list` (2:37:38 AM) 0/10; no SEVERE or stack trace naming
  ExcelBedrockMenu or DeluxeMenus, no new YAML errors —
  baseline noise only. Server left running.
- **Install record:** `INSTALLED-1.5.4.txt` in the packet
  folder (date/time, all three SHA-256s, `dm list` output,
  key enable lines).

## 8. Rollback (from the packet)

1. Panel Shut Down.
2. Remove the 1.5.4 jar; restore `ExcelBedrockMenu-1.5.3.jar`
   from its backup name.
3. Restore `gui_menus/server_menu.yml` from
   `server_menu.yml.bak-20261016-154` (the exact
   GuildGate-packet menu, lock pair and seals item included).
4. Restore DeluxeMenus `config.yml` from
   `config.yml.bak-20261016-154` (removes the `commands_menu`
   registration) and delete `gui_menus/commands_menu.yml`.
5. Panel Start; verify 1.5.3 enables and the menu count is back
   to the pre-install number.

## 9. What this release does not do

- It does not change any existing button, the guild lock label,
  the seals button, or any 1.5.3 behaviour — the CRC scope proof
  (§3) is the guarantee.
- It does not add a general `/help` replacement: `/help` itself is
  deliberately excluded from the catalogue (an unfiltered listing
  that includes admin commands). The catalogue is the curated
  list, and its exclusions (`/rules`, `/back`, `/kit`,
  `/deluxemenus`, …) are reasoned in `COMMANDS-CATALOG.md`.
- It does not make Bedrock open a browser — impossible from in
  game; the website form gives the address in words instead.
- It does not touch Jobs, mcMMO, EliteMobs, claims, or any
  economy state. The new commands only display information and
  dispatch existing player commands.

## 10. Pre-publish gates

1. **Install marker — SATISFIED.** 1.5.4 went live October 16,
   2026, ~2:36 AM CDT and its install record (`INSTALLED-1.5.4.txt`)
   is written; this record is published on that basis.
2. **In-game checks — NOT DONE YET** (they do not block the
   record, which states them as open, but they block calling the
   release *checked*): Java — slots 9 and 17 render, the commands
   menu opens, a green entry runs its command, a grey entry prints
   its "type it in chat" hint, the website button prints a
   clickable link. Bedrock — `/menu` shows Server Commands and
   Server Website after Server Info; the commands form lists the
   groups and a group form runs a bare-safe command; the website
   form shows the address.
3. Publishing itself follows the standing authorization (GitHub
   writes are pre-authorized; wrv-plugins records go as **open
   draft PRs**), and the §11 website delta is applied in the same
   pass, then verified byte-for-byte against the live page.

---

## 11. Website delta (applied in the same publishing pass)

**Status: the two edits below are applied to `index.html` in
the same pass as this record, via the web-upload route, and
the live page is verified byte-for-byte afterwards.**

### What was inspected

- The live page's source as published Oct 16, 2026
  (`~/workspace/overnight-2026-10-16/website-prep/index-campaign.html`,
  verified byte-identical to the live page that night).
- How this plugin is listed there, as found:
  - The **"All plugins" table** row for ExcelBedrockMenu currently
    reads — quoted exactly:
    > `<tr><td>ExcelBedrockMenu (BedrockMenu)<small class="plugin-next">Created by us</small></td><td>1.5.3</td><td>Shows the server menu on phones, tablets, and consoles. Version 1.5.3 adds a Road to the Guild button, right before Adventurers Guild, that runs /seals and shows the five-seals quest page, and the Adventurers Guild button now wears a 🔒 lock in its label while the guild is sealed.</td></tr>`
  - The **Change log**'s newest block is Oct 16, 2026, whose newest
    entry is "The Road to the Guild is on". A 1.5.4 entry belongs
    in that same Oct 16 block, after it, if applied the same day —
    or in a new day block if applied later.

### The exact delta proposed for 1.5.4 — two edits, `index.html` only

**Edit 1 — plugin table row: version 1.5.3 → 1.5.4, description
gains the two buttons.**

Replace the row quoted above with exactly:

> `<tr><td>ExcelBedrockMenu (BedrockMenu)<small class="plugin-next">Created by us</small></td><td>1.5.4</td><td>Shows the server menu on phones, tablets, and consoles. Version 1.5.4 adds two buttons after Server Info: Server Commands, which opens the full list of player commands (44 commands in 7 groups; type /servercommands or /cmds anywhere), and Server Website, which shows this site's address (type /serverwebsite).</td></tr>`

**Edit 2 — Change log: one new entry in the Oct 16, 2026 block,
after "The Road to the Guild is on".**

Add exactly this list item:

> `<li><b>ExcelBedrockMenu 1.5.4:</b> the server menu gains <b>Server Commands</b> and <b>Server Website</b> buttons on both menu styles. Server Commands opens a curated list of the server's 44 player commands in 7 groups — Getting Around, Money &amp; Jobs, Skills &amp; Levels, Shops &amp; Trading, Claims &amp; Land, Guild &amp; Quests, Server Info — and most entries run their command with one tap; the ones that need more typing say so. You can also type <code>/servercommands</code> (or <code>/cmds</code>) anywhere. Server Website shows this page's address; on Java it prints a clickable link, and on phones and consoles it shows the address to type, because Bedrock cannot open a browser from in game. The Java menu gets the two new items in slots 9 and 17 and a matching commands menu. <b>Still to check in game:</b> the commands and website forms on a real phone or console, and tapping the new Java menu items — those checks have not been done yet.</li>`

That is the whole 1.5.4 website delta: two edits, one file. The
"Still to check in game" sentence in Edit 2 must stay as long as
the §10 checks are open; if they pass before the delta is applied,
update that sentence — and only that sentence — and this record to
match.

### Flag — compass panel wording

The "Server menu compass" guide panel describes the menu contents
and was reworded by the 1.5.1 and campaign passes. Whether its
current wording needs a 1.5.4 sentence (it does not enumerate every
button) is a judgement call to make **at apply time against the
then-live text** — this draft deliberately does not pre-write a
panel edit from a possibly-stale quote. The two edits above are
complete without it.

---

## Sources consulted for this draft

Local files only; no GitHub, website, or server access was used.

- `~/workspace/menubuttons-058-059/install-prep/INSTALL-PACKET.md`
  — build, verification battery, install/rollback steps, honest
  unverified list, flagged decisions.
- `~/workspace/menubuttons-058-059/install-prep/COMMANDS-CATALOG.md`
  and `MENU-DIFF-058-059.md` — the catalogue and the exact menu
  diffs + registration.
- `~/workspace/menubuttons-058-059/install-prep/ExcelBedrockMenu-1.5.4.jar`
  — SHA-256 re-hashed locally for this draft; matches the packet.
- `~/workspace/wrv-055/PLUGIN-RECORD-DRAFT.md` — the 1.5.1 record
  whose structure this draft mirrors.
- `~/workspace/overnight-2026-10-16/website-prep/index-campaign.html`
  — the live page's exact 1.5.3 table row quoted in §11.
- `~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md` — the 1.5.3 /
  campaign rows and the separate-status rule.

*End of record.*
