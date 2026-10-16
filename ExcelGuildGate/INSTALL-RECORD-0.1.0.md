# ExcelGuildGate 0.1.0 + ExcelBedrockMenu 1.5.3 — "The Road to the Guild" campaign

> **INSTALL RECORD — published to `wrv-plugins` as a draft PR,
> October 16, 2026** (this repo's live-version-record pattern; draft
> PRs here stay unmerged).
>
> Prepared October 16, 2026, for Wink Reyes Valley (WRV-056) from
> local packet records, and published under the owner's explicit
> GitHub authorization for the Oct 16, 2026 overnight queue. The
> campaign went live the same night (§8): installed ~1:41 AM CDT by
> the install auto-watch, the gate verified LOCKED at 0 of 5 seals.
> The in-game checks in §10 are still open and are stated as open
> wherever they apply.

## Status at a glance

Statuses are kept separate, per the server rule. Do not collapse them.

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026, local build only; combined packet at `~/workspace/excelguildgate/install-prep/` |
| Automated tests | **Passed — 110/110 checks** (SealState 95 + GuildGateAPI 15; see §6) |
| Test server (scratch boot) | **Passed** — scratch copy of the Paper 26.2-123 runtime, both packet jars + PlaceholderAPI 2.12.3; clean enables, PAPI expansion registered, console grant cycle, persistence across restart, reset back to locked (see §6) |
| Installed live | **Yes** — October 16, 2026, ~1:41 AM CDT, via the install auto-watch on an empty server (§8). Post-install console check: Gate enabled, Guild **LOCKED**, **0 of 5** seals broken |
| Java check | **NOT DONE YET** — no Java-client check of the slot-12 locked variant, the slot-13 seals item, or the locked-variant flip after unlock has been done. See §10 |
| Bedrock check | **NOT DONE YET** — no real Bedrock-client check of the "Road to the Guild" button, the Cumulus `/seals` form render, or the dynamic 🔒 lock label has been done. See §10 |

## 1. Artifacts

### 1a. ExcelGuildGate 0.1.0 (new plugin — the campaign itself)

- **Plugin:** ExcelGuildGate
- **Version:** 0.1.0 — first release; no predecessor jar
- **Jar:** `ExcelGuildGate-0.1.0.jar`
- **SHA-256:** `7ada5c62950e78869da9a1368b3d9924bfead69cb88bb0fcfc8d5645b9871b2e`
  - Verified again locally during this draft's preparation by hashing
    `~/workspace/excelguildgate/install-prep/ExcelGuildGate-0.1.0.jar`
    — the hash matches the install packet exactly.
- **Main class:** `com.winkreyes.excelguildgate.ExcelGuildGate`
  (from `plugin.yml` in the source tree)
- **`plugin.yml` description:** "The Road to the Guild - the Adventurers
  Guild stays locked until the family breaks five boss seals together."
- **Author (per `plugin.yml`):** Argus
- **API version (per `plugin.yml`):** `1.13`
- **Softdepends (per `plugin.yml`):** `Geyser-Spigot`, `floodgate`,
  `EliteMobs`, `PlaceholderAPI`. The plugin runs fine with EliteMobs
  absent — it checks the guild world NAME only (see §3).
- **Commands in `plugin.yml`:** `seals` (alias `road`), `guildgate`
- **Permissions in `plugin.yml`:**
  - `excelguildgate.admin` — allows `/guildgate` admin commands; default `op`
  - `excelguildgate.bypass` — allows entering the guild while it is still
    sealed; default `false`
- **Data folder:** `plugins/ExcelGuildGate/` — created on first enable.
  Holds `config.yml` (created on first enable) and `state.yml` (the
  persisted seal state; see §2).
- **Rollback:** no predecessor. Rollback is deletion of the jar and its
  data folder (see §9).

### 1b. ExcelBedrockMenu 1.5.3 (companion bump — the Bedrock menu half)

- **Plugin:** ExcelBedrockMenu (documentation label: ExcelBedrockMenu (BedrockMenu))
- **Version in this packet:** 1.5.3
- **Previous live version at packet build time:** 1.5.1
- **Jar:** `ExcelBedrockMenu-1.5.3.jar`
- **SHA-256:** `b0801922c85a07bc501d3233a4795ff7713213cb72b6cf4c7c1f4afa4d3fca36`
  - Verified again locally during this draft's preparation by hashing
    `~/workspace/excelguildgate/install-prep/ExcelBedrockMenu-1.5.3.jar`
    — the hash matches the install packet exactly.
- **Main class:** `com.winkreyes.excelbedrockmenu.ExcelBedrockMenu`
- **Commands in `plugin.yml`:** `menu`, `tpmenu` (unchanged)
- **Data folder:** unchanged — the plugin keeps using
  `plugins/ExcelBedrockMenu/`; no migration or config change is involved
  in this bump.
- **Rollback artifact (planned, per the install packet):**
  `ExcelBedrockMenu-1.5.1.jar.bak` kept on the server (see §9). This is
  a planned install-time step, **not** a confirmed live fact at prep
  time — the packet had not installed yet (see the status table above).
- **Version note, stated plainly:** 1.5.2 was an unshipped intermediate
  build and is **NOT** in this packet. This packet ships only 1.5.3.
  Nothing in this record claims 1.5.2 was ever built for release,
  installed, or live.

### 1c. Third packet piece — DeluxeMenus menu file (not a jar)

- **File:** `server_menu.yml` (+ its `MENU-DIFF.md`), the DeluxeMenus
  server menu for Java players
  (live path: `plugins/DeluxeMenus/gui_menus/server_menu.yml`).
- **SHA:** n/a (menu file; applied per `MENU-DIFF.md`, drift check first)
- The Java menu half is detailed in §5a. It is part of the same packet
  and **must install together with ExcelGuildGate** — its locked/open
  variants depend on the GuildGate PlaceholderAPI expansion (see §5a).

## 2. What the campaign is — "The Road to the Guild"

The owner's decision (October 16, 2026, ~1:02 AM CDT, recorded in
`~/memory/2026-10-16.md`): **family-shared unlock**, not per-player.
The guild stays locked until the family **collectively** breaks all
five seals. The owner also directed that nobody but the owner had
entered the guild before this campaign was designed, so locking it at
campaign start has zero takeaway cost — this is stated as the owner's
recorded rationale in the nightly notes, not re-verified from the
server for this draft.

- **The five seals (family-shared, server-wide):**

  | Seal id (config / admin name) | Seal | Crediting event |
  |---|---|---|
  | `ender_dragon` | Ender Dragon | `EntityDeathEvent` |
  | `wither` | Wither | `EntityDeathEvent` |
  | `elder_guardian` | Elder Guardian | `EntityDeathEvent` |
  | `warden` | Warden | `EntityDeathEvent` |
  | `raid` | Raid victory | `RaidFinishEvent`, **VICTORY only** |

  Roster basis (recorded October 16, 2026): vanilla Minecraft has
  exactly two true boss-bar bosses (Ender Dragon and Wither); raids,
  elder guardians, and the Warden are boss-adjacent. That roster is the
  basis for this five-seals list.

- **Shared, not per-player:** progress is one server-wide count. A seal
  broken by anyone counts for everyone. There is no individual quest
  state to complete separately.
- **State persisted:** seal state is persisted in
  `plugins/ExcelGuildGate/state.yml` and survives restarts. The scratch
  boot test proved persistence across a restart (see §6).
- **Credit rules (forgiving by design, per the install packet):**
  - A boss kill credits its seal if a player landed the kill **OR**
    any player was online.
  - A death with nobody online (void, despawn edge cases) never credits.
  - Duplicate credits are no-ops; the original record is kept.
- **First-break broadcasts:** the first break of each seal broadcasts
  a two-line celebration — the seal's flavor line plus a progress line
  ("3 of 5" style: `Road to the Guild: {broken} of {total} seals broken`).
  Completing the set broadcasts the unlock
  ("The Road to the Guild is complete!" / "Every seal is broken. The
  Adventurers Guild is open! Type /ag to visit.") and persists it.
- **Unlock celebration / unlock state:** when the fifth enabled seal
  falls, the guild unlocks, the unlock is persisted, and — with no
  file edits, ever — both menus flip themselves to the open variant on
  their next open (see §5).
- **Config flavor (per-seal, from `config.yml` in the source tree):**
  each seal carries an editable `name`, `hint`, and `flavor`, e.g.
  Ender Dragon — hint "Waits in the End, past the stronghold portal.",
  flavor "The Ender Dragon has fallen! The End is free."; Warden —
  hint "Sleeps in the deep dark and hunts by sound. Sneak, or bring
  everyone." All five names, hints, and flavors are in the shipped
  `config.yml`.

## 3. How the gate works (while locked)

- **`/ag` and `/adventurersguild` blocked while locked:** from players
  without `excelguildgate.bypass`, the command is cancelled while the
  guild is sealed.
- **Tap-while-locked / type-while-locked behaviour (owner decision,
  October 16, 2026, ~1:07 AM CDT):** a locked attempt must not
  dead-end. The player gets **both**:
  1. the one-line locked notice — `messages.lockedCommandMessage`,
     default "The Adventurers Guild is still sealed." — **AND**
  2. the quest view opens (the same renderer as `/seals`; see §4).
  This decision was folded into the build the same minute it was made,
  per the nightly notes.
- **Guild-world teleports blocked while locked:** teleports **into**
  the world named `em_adventurers_guild` are cancelled for non-bypass
  players (with a throttled chat-panel notice). Console-issued teleports
  of a player into the guild world are blocked too; admins use
  `/guildgate` or the bypass permission.
- **What the gate never touches:** the guild world itself, EliteMobs,
  and its NPCs are never touched by this plugin. It checks the world
  **name** only, and runs fine with EliteMobs absent.
- **Master switch:** `gateEnabled` in `config.yml`. If `false`, `/ag`
  and guild-world teleports are never blocked (seals still get tracked
  and celebrated).
- **Bypass:** `excelguildgate.bypass` (default `false`) lets a holder
  enter while sealed. `excelguildgate.admin` (default `op`) gates
  `/guildgate`.

### `/seals` — the quest page

- **Command:** `/seals`, alias `/road`.
- **Bedrock (Floodgate/Geyser) players:** a Cumulus SimpleForm — one
  line per **enabled** seal (✔ broken / ✘ name + hint), a progress
  footer, and a single Close button. If the form cannot be sent, it
  falls back to chat.
- **Java players:** a compact clickable-style chat panel in the house
  style, headed by `messages.lockedHeader`
  ("— The Road to the Guild —") with the `messages.progressLine`
  and the locked/open footer (`questFooterLocked` /
  `questFooterOpen`).

### `/guildgate` — admin controls

- **Permission:** `excelguildgate.admin`, default `op`.
- **Subcommands:**
  - `status` — gate enabled/disabled, guild LOCKED/OPEN, progress
    ("0 of 5" style)
  - `grant <seal>` — break one seal by id
  - `revoke <seal>` — restore one seal (relocks the guild if it was open)
  - `reset` — restore all seals; guild back to locked
  - `open` — breaks all seals and unlocks **quietly** (console-logged,
    no broadcast spam)
- Unknown seal names return a helpful error listing the valid ids
  (`ender_dragon, wither, elder_guardian, warden, raid`).
- Grants, revokes, and resets are all console-logged.

### Per-seal config toggles

From `config.yml` (created on first enable):

- `gateEnabled` — master switch (above).
- Per seal: `enabled`, `name`, `hint`, `flavor`.
- Disabling a seal (e.g. the Warden) drops it out of the required set
  **and** off the quest page — no rebuild needed. Progress counts only
  enabled seals (see the API totals in §5c).

## 4. Source layout

### ExcelGuildGate source

- **Source tree:** `~/workspace/excelguildgate/src/ExcelGuildGate/`
  - `plugin.yml`, `config.yml`, `build.sh`, `run-tests.sh`
  - `src/com/winkreyes/excelguildgate/`
    - `ExcelGuildGate.java` — main class
    - `GateForms.java` — the `/seals` quest view (Bedrock form / Java chat)
    - `GuildGateExpansion.java` — the PlaceholderAPI expansion (§5c)
    - `SealState.java` — the persisted, server-wide seal state
  - `src/com/winkreyes/guildgate/api/`
    - `GuildGateAPI.java` — the public read-only API (§5c)
  - `tests/com/winkreyes/excelguildgate/SealStateTest.java` (95 checks)
  - `tests/com/winkreyes/guildgate/api/GuildGateAPITest.java` (15 checks)
  - Re-run the tests any time:
    `~/workspace/excelguildgate/src/ExcelGuildGate/run-tests.sh`
- **GuildGate jar contents (per the install packet):** exactly its
  5 sources' classes (incl. `GuildGateAPI` + `GuildGateExpansion`)
  + `plugin.yml` + `config.yml`.
- **Build scripts for the packet:** `~/workspace/excelguildgate/build/`
  (`build-bedrock-153.sh`, plus the superseded intermediate
  `build-bedrock-152.sh`, and `smoke-test.sh`).

### ExcelBedrockMenu 1.5.3 source

- **Source tree:** `~/workspace/renames-2026-10-15/src/ExcelBedrockMenu`
  — the renamed source tree that produced the live 1.5.0 and 1.5.1.
  Its `plugin.yml` reads `version: 1.5.3` and its per-plugin `build.sh`
  names a 1.5.3 output (both read directly during the WRV-055 draft's
  preparation and re-checked for this draft's BedrockMenu facts).
- **Files changed in source for 1.5.3 (change-scope proof is §6):**
  the main class `ExcelBedrockMenu.java` and `plugin.yml` only.

## 5. The menu halves, the API, and the placeholders

### 5a. Java half — DeluxeMenus `server_menu.yml` locked/open pair + seals item

Full detail, exact blocks, and the drift-check procedure are in
`~/workspace/excelguildgate/install-prep/MENU-DIFF.md`. Summary:

- Slot 12's single live `guild` block (BELL, `'&a&lAdventurers Guild'`,
  `[player] ag` — added live October 16) is replaced by a **priority
  pair keyed on `%guildgate_unlocked%`**, mirroring the file's existing
  `autojoin_on` / `autojoin_off` pattern:
  - **`guild_locked` (priority 1, slot 12)** — shown while sealed:
    material `BELL`, display `'&8🔒 Adventurers Guild'`, lore
    "Sealed by the five challenges" / "Tap to see the Road to the Guild",
    view requirement `string equals` on `%guildgate_unlocked%` = `false`,
    left-click runs `[player] seals` — so a Java tap while locked opens
    the quest page directly.
  - **`guild_open` (priority 2, slot 12)** — shown once open: exactly
    tonight's live block, unchanged except for the key name and the view
    requirement (`%guildgate_unlocked%` = `true`); display
    `'&a&lAdventurers Guild'`, lore "Teleport to the EliteMobs" /
    "guild hall", left-click runs `[player] ag`.
- **New `seals` item at slot 13** — between guild (12) and mcMMO (14):
  material `WRITABLE_BOOK`, display `'&a&lRoad to the Guild'`, lore
  "The five seals quest" / "See what is left to break", left-click
  runs `[player] seals`.
- Nothing else changes: same title, same size (27), same slots for
  every other item (spawn 10, shop 11, mcMMO 14, info 15, autojoin 16).
  Slot 13 was empty, so nothing moves.
- **Hard dependency warning (from MENU-DIFF.md, kept verbatim in
  substance):** both variants key off `%guildgate_unlocked%`, which
  only exists because ExcelGuildGate registers its PlaceholderAPI
  expansion. **Install this menu only together with ExcelGuildGate.**
  If the placeholder is missing, neither requirement passes and
  **slot 12 renders empty**.
- Once installed, the menu flips itself the night the fifth seal
  breaks. No file edits, ever.

### 5b. Bedrock half — ExcelBedrockMenu 1.5.3

- New button **"Road to the Guild"** (runs `seals`), immediately
  **before** the "Adventurers Guild" button.
- The **"Adventurers Guild" button label is dynamic / lock-aware**
  (owner decision, October 16, 2026, ~1:07 AM CDT: while sealed the
  button must show a lock hint): at menu-open time the plugin asks
  `GuildGateAPI` **via reflection, with no hard dependency** on
  ExcelGuildGate, and shows **"🔒 Adventurers Guild"** while the guild
  is locked. Unlocked, or GuildGate absent/unreadable for any reason,
  it shows the plain label.
- Dispatch is `ag` either way; tapped while locked, the **gate** (not
  this plugin) answers with the locked notice + the quest view (§3).
- Everything else is 1.5.1, unchanged (CRC proof in §6).
- Full button order in 1.5.3 (bytecode-verified, §6):

  1. Spawn
  2. Teleport to a player
  3. Spawn Shop
  4. **Road to the Guild** *(new)*
  5. **Adventurers Guild** *(label now lock-aware)*
  6. Jobs & Money
  7. mcMMO Stats
  8. Leaderboard
  9. Server Info
  10. Auto-open on join

- Full command array in 1.5.3, index-aligned with the buttons above:
  `spawn`, `tpmenu`, `shop`, **`seals`**, **`ag`**, `jobs browse`,
  `mcstats`, `leaderboard`, `motd`, `menujoin`.
- Bytecode note (from the install packet): the stored `BUTTONS` array
  keeps the **PLAIN** "Adventurers Guild" label; the 🔒 prefix is
  computed at runtime.

Spelling note, kept exact on purpose: the button labels are
**"Adventurers Guild"** and **"Road to the Guild"** (no apostrophe in
"Adventurers"), as they appear in the source arrays and as players see
them. The website's prose name for the place is "The Adventurer's
Guild" (with apostrophe).

### 5c. Public API + PlaceholderAPI expansion

- **Public API:** `com.winkreyes.guildgate.api.GuildGateAPI`
  - `isUnlocked()`
  - `sealsBrokenCount()` — enabled seals broken so far
  - `sealsTotalCount()` — enabled seals only
  - Null-safe before state load / after disable (reads as locked, zero
    progress). The `bind`/`unbind` methods are internal wiring used by
    the plugin itself; consumers should only call the three getters.
  - ExcelBedrockMenu 1.5.3 consumes this API by reflection (§5b), so
    BedrockMenu keeps no hard dependency on GuildGate.
- **PlaceholderAPI expansion** (softdepend; registers only if
  PlaceholderAPI is present):
  - Expansion identifier: **`guildgate`**
    (class `GuildGateExpansion`, author Argus, `persist()` = true so
    it survives `/papi reload`)
  - `%guildgate_unlocked%` → `true` / `false`
  - `%guildgate_seals_broken%` → enabled seals broken so far
  - `%guildgate_seals_total%` → enabled seals required
  - These placeholders drive the DeluxeMenus menu variants in §5a.

## 6. Build & verification (Moon's playbook)

All of the following is from the combined install packet
(`~/workspace/excelguildgate/install-prep/INSTALL-PACKET.md`), which was
written before any install, plus the two scratch boot logs beside it.

- **Automated tests — 110/110 checks:**
  - `SealStateTest` (**95**) — credit paths, duplicates keep the
    original record, unknown/null/disabled no-ops, unlock triggers
    exactly at completion, grant/revoke/reset/openAll, disabled seals
    excluded from counts, vacuous unlock with zero enabled seals,
    snapshot/restore round-trips.
  - `GuildGateAPITest` (**15**) — unbound null-safety, live readouts,
    enabled-subset totals.
- **Rebuild reproduction:**
  - ExcelGuildGate: built twice from clean, identical SHA-256
    (`7ada5c62…`).
  - ExcelBedrockMenu 1.5.3: **one early build produced a differing
    hash that never recurred — owned plainly here, not hidden.** The
    shipped jar was then rebuilt **four consecutive times** with
    identical SHA-256 (`b0801922…`), and the full verification battery
    below was re-run against those exact bytes.
- **Change scope (BedrockMenu vs live 1.5.1 jar):** per-entry CRC diff
  — **ONLY** `ExcelBedrockMenu.class` and `plugin.yml` differ;
  `GeyserForms`, `TeleportMenu`, `PickHolder`, and the manifest are
  byte-identical to 1.5.1. This is the narrow-change proof for the
  Bedrock half: one main class and the version file, nothing else moved.
- **Bytecode (`javap`):** `BUTTONS` order Spawn, Teleport to a player,
  Spawn Shop, **Road to the Guild**, **Adventurers Guild**, Jobs &
  Money, mcMMO Stats, Leaderboard, Server Info, Auto-open on join;
  `COMMANDS` index aligned (…, shop, **seals**, **ag**, …). The stored
  array keeps the PLAIN "Adventurers Guild" label; the 🔒 prefix is
  computed at runtime.
- **GuildGate jar contents:** exactly its 5 sources' classes (incl.
  `GuildGateAPI` + `GuildGateExpansion`) + `plugin.yml` + `config.yml`.
- **Boot smoke test (scratch / test server):** scratch copy of the
  Paper 26.2-123 runtime, **BOTH** packet jars + PlaceholderAPI 2.12.3;
  EliteMobs absent. Full logs:
  `~/workspace/excelguildgate/smoke-boot1.log` and
  `~/workspace/excelguildgate/smoke-boot2.log`. Result:
  - Both plugins enable clean; the PlaceholderAPI hook registers —
    log line (boot 1): "Successfully registered internal expansion:
    guildgate [0.1.0]", followed by ExcelGuildGate's hook line naming
    `%guildgate_unlocked%`, `%guildgate_seals_broken%`,
    `%guildgate_seals_total%`, and its status line
    "ExcelGuildGate enabled: 0 of 5 seals broken, guild LOCKED".
  - `/seals` and `/guildgate` exercised from console, including the
    **full grant cycle LOCKED 0→5 with unlock**: grants of
    `ender_dragon` (1 of 5), `wither` (2 of 5), `elder_guardian`
    (3 of 5), `warden` (4 of 5), `raid` (5 of 5) → "All seals broken:
    the Adventurers Guild is now OPEN." An unknown-seal probe
    (`bogus`) returned the valid-ids error, and a revoke relocked
    (4 of 5), followed by a `/guildgate open` test.
  - **Persistence across restart:** boot 2 came up
    "5 of 5 seals broken, guild OPEN" from the persisted state.
  - **Reset back to locked:** boot 2 then ran the console reset —
    "Road to the Guild reset. All seals restored, guild is LOCKED."
    → status "Gate: enabled | Guild: LOCKED | Progress: 0 of 5".
  - The scratch runtime has **no** Geyser/Floodgate and no Bedrock
    client — the Bedrock form path was not exercised there (see §10).

## 7. What this release does not do

- It does not pre-break any seals. The campaign starts clean: **0 of 5**
  (install step 6 in §8, stated in the packet in bold).
- It does not add, move, rename, or remove any other menu button or
  menu item, on either menu, beyond §5.
- It does not change `/ag` itself, the guild world, EliteMobs, or any
  NPC — GuildGate only gates access to them while sealed, and
  BedrockMenu only dispatches existing commands as the player.
- It does not install, prepare, or claim ExcelBedrockMenu 1.5.2.
  1.5.2 was an unshipped intermediate build (per the 1.5.3 packet).
- It does not make the unlock per-player. The unlock is family-shared
  and server-wide (§2) — any per-player reading of this record is wrong.

## 8. Install — route, steps, and live execution

### Route

- **Route:** auto-watch cron on an **empty server only**, under the
  standing install rule (gate checked twice, marker file against
  re-runs, rollback rules written in advance, expiry with a stand-down
  message).
- **Watch (as recorded locally):** cron `excelguildgate-install-watch`,
  15-minute interval, empty-server gate, expiry **October 16, 2026,
  5:21 PM CDT** as written in that cron record (per the WRV-055 draft
  §11 flag and `~/memory/2026-10-16.md`). The watch's planned menu
  backup name, per the same nightly record, is
  `server_menu.yml.bak-20261016`.
- **Marker:** `~/workspace/excelguildgate/install-prep/INSTALLED.txt`
  — written by the watch on a successful install.
- **Timing note, stated plainly:** the install packet's own header,
  written at build time, says "LOCAL ONLY. Nothing on the live server
  was touched, no watch is armed." That was true at packet build time.
  The watch was armed afterwards (~1:16 AM CDT, per the nightly notes).
  Both statements are kept here so the record is not misread later.

### Install — live execution (from the install marker)

The watch fired the same night. Facts below are from its install
record, `~/workspace/excelguildgate/install-prep/INSTALLED.txt`:

- **Gate:** panel `list` twice, both **0/10** — 1:32:17 AM and
  1:34:56 AM CDT, October 16, 2026. No players online.
- **Sequence:** panel **Shut Down**; `ExcelGuildGate-0.1.0.jar`
  uploaded (new); `ExcelBedrockMenu-1.5.1.jar` renamed to
  `ExcelBedrockMenu-1.5.1.jar.bak` (rollback) and
  `ExcelBedrockMenu-1.5.3.jar` uploaded; the live menu file passed
  the drift check (it matched the expected pre-packet state), was
  backed up as `server_menu.yml.bak-20261016`, and the packet
  `server_menu.yml` was swapped in and verified to contain
  `guild_locked`, `guild_open`, and `seals`; panel Start.
- **Upload caveat, stated plainly:** the panel confirms uploads by
  byte size, not by hash — the uploaded sizes matched the packet
  (22,929 and 15,738 bytes) but the jar SHA-256 values in §1 were
  **not** verified in-panel.
- **Live log evidence (from the install record):**
  - ExcelGuildGate: fresh start (no `state.yml` existed yet);
    `[PlaceholderAPI] Successfully registered internal expansion:
    guildgate`; enabled with **0 of 5 seals broken, guild LOCKED**;
    placeholders `unlocked` / `seals_broken` / `seals_total` hooked.
  - `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.3` plus its
    enabled confirmation.
  - DeluxeMenus: hooked PlaceholderAPI/Vault, **8 GUI menus
    loaded**, no YAML errors (only the known baseline `teststone`
    'data' warning in basics_menu).
  - `Done (16.894s)` at **1:41:11 AM CDT**; post-start `list` at
    1:42:24 AM CDT: **0/10**. No SEVERE or stack traces naming
    ExcelGuildGate, ExcelBedrockMenu, or DeluxeMenus; no rollback
    triggered. Remaining log noise is the documented baseline.
- **Console `guildgate status` at 1:42:31 AM CDT:**
  "Gate: enabled | Guild: LOCKED | Progress: 0 of 5", with all
  five seals listed unbroken (Ender Dragon, Wither, Elder
  Guardian, Warden, Raid Victory).
- **No seals were pre-broken.** The campaign started clean: 0 of 5.

### Install steps (empty-server window only, per the packet)

1. Confirm nobody is online (panel player list, `list` twice).
2. Shut the server down from the panel (Shut Down, not console `stop`).
3. In `plugins/`:
   - Upload `ExcelGuildGate-0.1.0.jar` (new; no predecessor).
   - Rename `ExcelBedrockMenu-1.5.1.jar` → `ExcelBedrockMenu-1.5.1.jar.bak`
     (rollback), then upload `ExcelBedrockMenu-1.5.3.jar`.
   - Apply `server_menu.yml` per `MENU-DIFF.md` (drift check first;
     swap or by-edit).
4. Verify both uploaded jars' SHA-256 against §1.
5. Start the server. Confirm in the log: `Enabling ExcelGuildGate v0.1.0`,
   its status line (0 of 5, LOCKED on a fresh `state.yml`), the
   PlaceholderAPI hook line, `Enabling ExcelBedrockMenu v1.5.3` and its
   enabled line, no new errors, `list` shows 0/10.
6. **Do not pre-break any seals.** The campaign starts clean: 0 of 5.

Menu drift check (from `MENU-DIFF.md`, load-bearing): read the live
`server_menu.yml` first and confirm it matches the pre-packet state
(the Oct 15 prepared menu plus the single `guild` block at slot 12).
If it matches, swap the packet file wholesale or apply the same edit by
hand — both routes land on identical content. If the live file has
drifted, do **NOT** swap; apply only the block replacement by edit.
After applying: `dm reload` from the console (no restart needed for the
menu itself; it rides along with the packet's restart window anyway).

## 9. Rollback

Per the install packet, verbatim in substance:

1. Shut the server down from the panel.
2. Delete `ExcelGuildGate-0.1.0.jar` **and its data folder** — the gate
   vanishes, `/ag` is free again, EliteMobs untouched.
3. Delete `ExcelBedrockMenu-1.5.3.jar` and rename the 1.5.1 `.bak`
   back to `ExcelBedrockMenu-1.5.1.jar`.
4. Restore the pre-packet `server_menu.yml` — or remove the two variant
   blocks + the seals item and restore the single `guild` block per
   `MENU-DIFF.md`.
5. Start the server.

No economy, balance, world, or EliteMobs change is involved in either
direction. Seal progress lives only in the GuildGate data folder, so
deleting that folder in step 2 is what removes the campaign state —
that deletion is part of the rollback, not an accident.

## 10. NOT verified yet (in-game checks for the owner, install day)

**Nothing in this section is claimed as checked. A clean build, a
110/110 test pass, and a passed scratch boot do not close any of these.**

- **Real boss-kill and raid-victory crediting on live** — the Ender
  Dragon, Wither, Elder Guardian, and Warden death crediting, and the
  raid-victory (`RaidFinishEvent`, VICTORY only) crediting, on the
  **live** server. The state machine is unit-tested and the admin grant
  cycle is boot-tested, but no real boss has died under the plugin yet.
- **Bedrock Cumulus form render of `/seals`** on a real Floodgate
  client (the scratch runtime has no Geyser/Floodgate or Bedrock client).
- **The dynamic lock label on a real Bedrock client** — the
  "🔒 Adventurers Guild" label computed at menu-open time, seen on an
  actual phone/console menu.
- **Java menu locked-variant flip after unlock** — slot 12 flipping
  from the 🔒 sealed variant to the plain guild button on its own when
  the fifth seal breaks (and, in the locked state first: Java `/menu`
  showing the 🔒 sealed variant at slot 12 and the seals item at
  slot 13).
- **The gate blocking a real non-bypass player** — a non-bypass player
  typing `/ag` while locked, a tap on either locked guild button giving
  the notice + quest view, and a teleport attempt into
  `em_adventurers_guild` (e.g. a stray portal, or a console teleport
  of a non-bypass player), all blocked with the quest shown.
- **Lock glyph rendering on the family's devices** — the 🔒 lock
  glyph inside DeluxeMenus item names and inside Bedrock form buttons,
  on the family's actual devices.
- **Menu checks, after unlock (packet's test route):** run
  `/guildgate open` as a test (or actually finish the campaign) and
  confirm BOTH menus show the plain guild button again with no file
  edits; then `/guildgate reset` to put the campaign back to 0 of 5
  if the open was only a test.

## 11. Publication status

1. **Install — LANDED.** The marker exists and §8 is written from
   it and the watch's install record (gate checks, log lines, the
   console status read-back). The campaign is live and locked at
   0 of 5.
2. **The owner's in-game checks (§10) remain NOT DONE** and are
   stated that way in this record, per this repo's pattern.
3. **GitHub authorization — given for this publish.** The owner's
   standing ask-first rule for GitHub writes was lifted for the
   Oct 16, 2026 overnight queue ("Yes you can do the GitHub
   writes", ~1:33 AM CDT); that is the authority under which this
   record and its draft PR exist.

Still owed after this record: the website delta in the companion
draft (`WEBSITE-DELTA-DRAFT.md`, local publishing prep) —
application route: browser web-upload, because the connector
CLI's ~128 KB single-argument cap cannot carry the ~192 KB
`index.html` — followed by a byte-for-byte read-back of the
published page, and an update to the local version/rollback map
(`~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md`).

---

## Sources consulted for this draft

Local files only; no GitHub, website, or server access was used.

- `~/workspace/excelguildgate/install-prep/INSTALL-PACKET.md`
  — packet facts, SHAs, what each piece does, verification battery,
  install steps, rollback, and the packet's own NOT-verified list.
- `~/workspace/excelguildgate/install-prep/MENU-DIFF.md`
  — the slot-12 locked/open pair, the slot-13 seals item, the PAPI
  requirement, and the drift-check procedure.
- `~/workspace/excelguildgate/install-prep/server_menu.yml`
  — the packet menu file, read directly (blocks match MENU-DIFF.md).
- `~/workspace/excelguildgate/install-prep/ExcelGuildGate-0.1.0.jar`
  and `ExcelBedrockMenu-1.5.3.jar`
  — SHA-256 re-hashed locally for this draft; both match §1.
- `~/workspace/excelguildgate/src/ExcelGuildGate/` (`plugin.yml`,
  `config.yml`, the five source files under `src/`, the two test
  files under `tests/`) — commands, permissions, seal names/hints/
  flavors, API surface, and expansion identifiers, read directly.
- `~/workspace/renames-2026-10-15/src/ExcelBedrockMenu/` (`plugin.yml`,
  `build.sh`, main class) — the 1.5.3 BedrockMenu source facts in §4/§5b.
- `~/workspace/excelguildgate/smoke-boot1.log` and `smoke-boot2.log`
  — the scratch boot evidence quoted in §6.
- `~/workspace/wrv-055/PLUGIN-RECORD-DRAFT.md`
  — the detail/style standard this record mirrors; also the source of
  the install-watch flag (cron name, 15-minute interval, 5:21 PM CDT
  expiry) quoted in §8.
- `~/memory/2026-10-16.md`
  — tonight's decisions: family-shared five seals (~1:02 AM CDT),
  lock-label menus + tap-while-locked opens the quest page
  (~1:07 AM CDT), the packet-built entry with its verification summary
  and NOT-verified list, and the auto-watch-armed entry (~1:16 AM CDT).
- `~/workspace/excelguildgate/install-prep/INSTALLED.txt`
  — the install auto-watch's own record of the live install
  (gate checks, swap results, log evidence, console status),
  written October 16, 2026, ~1:42 AM CDT. §8 is built from it.
  (At this record's first preparation the marker was still absent;
  it landed during the same overnight run.)

### Could not be verified from local files

- **Resolved at publish time:** the install watch DID fire; the
  live facts in §8 rest on the watch's install record
  (`INSTALLED.txt`) — gate checks, the menu drift check MATCHING,
  startup log evidence, and the console `guildgate status`
  read-back. Beyond that record, the live server's jars, log, and
  player state were not independently re-checked during this
  publishing pass (no server access was in scope for it). The one
  in-panel gap is noted in §8: uploaded jars were size-confirmed,
  not hash-confirmed.
- **The BedrockMenu 1.5.3 toolchain note** (JDK 25, the renames
  top-level harness classpath mirrored for these builds) is taken
  from the packet-era build script
  (`~/workspace/excelguildgate/build/build-bedrock-153.sh`) and the
  WRV-055 record's toolchain pattern; the GuildGate packet itself
  does not restate a full toolchain section, and none is invented here.

*End of record.*
