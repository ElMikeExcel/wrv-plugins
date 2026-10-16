# ExcelBedrockMenu 1.5.1 — "Adventurers Guild" menu button

> **INSTALL RECORD — published to `wrv-plugins` as a draft PR,
> October 16, 2026** (this repo's live-version-record pattern; draft
> PRs here stay unmerged).
>
> Prepared October 16, 2026, for Wink Reyes Valley (WRV-055) from
> local install records only; no server access was used to prepare
> it. Published under the owner's explicit GitHub authorization for
> the Oct 16, 2026 overnight queue. One check is still open and is
> stated as open wherever it applies: the owner's in-game Bedrock
> tap test (§10).

## Status at a glance

Statuses are kept separate, per the server rule. Do not collapse them.

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026, local build only at build time |
| Automated tests | **N/A as a test suite** — no automated test suite is claimed for this plugin. Verification was rebuild reproduction, CRC change-scope, `javap` bytecode read-back, `plugin.yml` read-back from the jar, and a scratch boot smoke test (see §5) |
| Test server (scratch boot) | **Passed** — staged jar only plugin present; clean enable, probes, and disable |
| Installed live | **Yes** — October 16, 2026, via auto-watch cron on an empty server (see §7 for the exact times in the local install record) |
| Java check | **Covered by the companion change, not by this jar** — the Java menu is DeluxeMenus, not this plugin. Its guild item was added live separately and `dm reload` was clean with 8 menus loaded (see §6). No Java-client tap check of that item is claimed in this record |
| Bedrock check (this button) | **NOT DONE YET** — the owner's in-game Bedrock tap test (open the menu, tap Adventurers Guild, land at the guild) has **not** been done yet. See the pre-publish gates in §10 |

## 1. Artifact

- **Plugin:** ExcelBedrockMenu (documentation label: ExcelBedrockMenu (BedrockMenu))
- **Live version after this install:** 1.5.1
- **Previous live version:** 1.5.0
- **Jar:** `ExcelBedrockMenu-1.5.1.jar`
- **SHA-256:** `0242d830d29d38d93cf320102582bd6ddd89cea8a5284e6013bfa38c70855c2e`
  - Verified again locally during this draft's preparation by hashing
    `~/workspace/excelbedrockmenu-guild/install-prep/ExcelBedrockMenu-1.5.1.jar`
    — the hash matches the install packet and the install record exactly.
- **Main class:** `com.winkreyes.excelbedrockmenu.ExcelBedrockMenu`
- **Commands in `plugin.yml`:** `menu`, `tpmenu` (unchanged)
- **Data folder:** unchanged — the plugin keeps using
  `plugins/ExcelBedrockMenu/`; no migration or config change is involved
  in this bump.
- **Rollback artifact:** `ExcelBedrockMenu-1.5.0.jar.bak` kept on the
  server (see §8).

## 2. What changed vs 1.5.0

One new button, plus the version bump. Nothing else.

- New Bedrock main-menu button **"Adventurers Guild"** at **button
  index 3**, immediately after **"Spawn Shop"**, mirroring the Java menu
  where the guild item sits after the shop item.
- Full button order in 1.5.1:

  1. Spawn
  2. Teleport to a player
  3. Spawn Shop
  4. **Adventurers Guild** *(new)*
  5. Jobs & Money
  6. mcMMO Stats
  7. Leaderboard
  8. Server Info
  9. Auto-open on join

- Full command array in 1.5.1, index-aligned with the buttons above:
  `spawn`, `tpmenu`, `shop`, **`ag`**, `jobs browse`, `mcstats`,
  `leaderboard`, `motd`, `menujoin`.
- Dispatch: the button goes through the exact path the existing buttons
  use. The form handler maps the tapped index into `COMMANDS[]` and
  schedules `runAction`, whose default branch is
  `player.performCommand(command)` — so the Guild button runs **`ag`
  as the player** (EliteMobs' `/adventurersguild` alias), the same way
  the Shop button runs `shop`. The SimpleForm closes on selection, as
  with every other button.
- Files changed in source:
  - `src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java` —
    `BUTTONS[]` gains `"Adventurers Guild"` at index 3; `COMMANDS[]`
    gains `"ag"` at index 3; a javadoc note records the 1.5.1 change.
  - `plugin.yml` — `version: 1.5.0` → `1.5.1`.
  - `build.sh` (per-plugin) — output jar name updated to
    `ExcelBedrockMenu-1.5.1.jar` for this build. See the known wart in
    §9: the renames tree's **top-level** harness was not updated.
- This plugin only dispatches `ag`. `/ag` itself is EliteMobs' command
  on the live server (the guild world went live October 16, 2026, as
  EliteMobs Phase 2 — a separate record, `EliteMobs/GUILD-INSTALL-RECORD.md`
  in wrv-plugins draft PR #23, per the local version/rollback map).

Spelling note, kept exact on purpose: the button label is
**"Adventurers Guild"** (no apostrophe), as it appears in the source
arrays and as players see it. The website's prose name for the place is
"The Adventurer's Guild" (with apostrophe). Both forms are used in this
draft only in those respective places.

## 3. Change-scope proof

- Per-entry CRC diff against the live 1.5.0 jar showed **only**
  `ExcelBedrockMenu.class` and `plugin.yml` differ.
- `GeyserForms.class`, `TeleportMenu.class`,
  `TeleportMenu$PickHolder.class`, and the manifest are byte-identical
  to 1.5.0.
- This is the narrow-change proof for this release: one main class and
  the version file, nothing else in the jar moved.

## 4. Source of record

- **Source tree:** `~/workspace/renames-2026-10-15/src/ExcelBedrockMenu`
  — the renamed source tree that produced the live 1.5.0.
- **Install packet (build/install source document):**
  `~/workspace/excelbedrockmenu-guild/install-prep/INSTALL-PACKET.md`
- **Install record (live execution):**
  `~/workspace/excelbedrockmenu-guild/install-prep/INSTALLED-1.5.1.txt`
- **Smoke-test log:**
  `~/workspace/excelbedrockmenu-guild/smoke-test-boot.log`

### Known wart — flag, do not hide

- The renames tree's **top-level** build harness,
  `~/workspace/renames-2026-10-15/build.sh`, **still names its
  ExcelBedrockMenu output `ExcelBedrockMenu-1.5.0.jar`** (verified in
  the local file during this draft's preparation:
  `build_one ExcelBedrockMenu "" "ExcelBedrockMenu-1.5.0.jar"`).
  Anyone rebuilding through that top-level harness will get a jar
  *named* 1.5.0 regardless of the version inside it. The 1.5.1 build
  did not rely on that name — the install packet documents the
  per-plugin build/output for this release.
- Related, stated plainly so the record is not misread later: that same
  source tree has **already moved forward past 1.5.1** — at the time
  this draft was prepared, its `plugin.yml` reads `version: 1.5.3` and
  its per-plugin `build.sh` names a 1.5.3 output, because the 1.5.3
  packet (§11) was built from it after 1.5.1 shipped. The 1.5.1 facts
  in this record are taken from the 1.5.1 install packet and install
  record, which were written at 1.5.1 build/install time, not
  re-derived from the tree as it stands now.

## 5. Build & verification (Moon's playbook)

All of the following is from the 1.5.1 install packet, which was
written before the install.

- **Toolchain:** JDK 25 (`~/workspace/.jdk25`),
  `javac --release 21 -proc:none`, classpath identical to the renames
  top-level harness that built 1.5.0 (ExcelShops `build-libs` Paper API
  + runtime libraries). Clean compile; only the pre-existing
  TeleportMenu deprecation note (same as 1.5.0).
  Note in the packet: the per-plugin `build.sh` defaults to a `~/.m2`
  Paper API path that does not exist in this environment; the renames
  top-level harness is the toolchain of record and was mirrored for
  these builds.
- **Rebuild reproduction:** two clean from-source builds produced
  byte-identical jars, SHA-256
  `0242d830d29d38d93cf320102582bd6ddd89cea8a5284e6013bfa38c70855c2e`
  both times. Packaging used `jar --date=2026-10-16T00:00:00-05:00` to
  pin zip entry timestamps; without it, entry contents/CRCs are
  identical and only zip timestamps vary between builds.
- **Bytecode check:** `javap` on the compiled main class confirms the
  `BUTTONS` and `COMMANDS` arrays in the order listed in §2, with
  `ag` at index 3.
- **`plugin.yml` read-back from the built jar:** name
  `ExcelBedrockMenu`, version `1.5.1`, main
  `com.winkreyes.excelbedrockmenu.ExcelBedrockMenu`, both commands
  present.
- **Boot smoke test:** scratch copy of the local Paper 26.2 build 123
  runtime; the staged jar was the only plugin jar present. Result:
  - `Loading server plugin ExcelBedrockMenu v1.5.1` →
    `Enabling ExcelBedrockMenu v1.5.1` →
    `ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.`
  - Server reached `Done`. Console probes: `menu` → "Only players can
    use /menu.", `tpmenu` → "Only players can use this.",
    `version ExcelBedrockMenu` → "ExcelBedrockMenu version 1.5.1",
    `plugins` lists `ExcelBedrockMenu`. Clean disable on `stop`.
  - Geyser/Floodgate were **not** present in the smoke runtime: the
    plugin enables cleanly without them (by design — softdepend;
    Geyser access is reflection-based and only exercised for real
    Bedrock players).
  - The only ERROR lines in the smoke log are sandbox network
    artifacts (yggdrasil public-key fetch, Paper version fetcher) —
    the same noise documented in the 1.5.0 rename smoke test; none
    mention this plugin.
- **Checksum discipline at install:** the install steps required
  verifying the uploaded jar's SHA-256 against the full value in §1
  before starting the server.

### Not verified by the build/test pass

- Tapping the new button on a real Bedrock client and landing in the
  guild via `/ag`. That is an in-game check, not a build check. It is
  still outstanding — see §10.

## 6. Companion change — Java menu (already live, same feature)

This is a separate edit, already handled, and is recorded here only so
the feature record is complete. It is **not** part of the 1.5.1 jar.

- **File:** DeluxeMenus `gui_menus/server_menu.yml` (the Java
  `server_menu`).
- **Change:** a new `guild` item at **slot 12**:
  - material: `BELL`
  - display: `'&a&lAdventurers Guild'`
  - lore: `'&7Teleport to the EliteMobs'` / `'&7guild hall'`
  - click: `[player] ag`
- **Apply/verify:** `dm reload` clean, **8 menus loaded**.
- No DeluxeMenus file was included in, or needed to change with, the
  1.5.1 Bedrock packet.

## 7. Install — live execution

- **Route:** auto-watch cron, under the standing install rule: only on
  an empty server, with a restart.
- **Gate:** player list checked twice — both **0/10**:
  - 12:49:43 AM CDT — 0/10
  - 12:52:39 AM CDT — 0/10
- **Sequence (per the install packet's steps, executed by the watch):**
  panel **Shut Down** (not console `stop`); in `plugins/`, the live
  `ExcelBedrockMenu-1.5.0.jar` was renamed to
  `ExcelBedrockMenu-1.5.0.jar.bak`; `ExcelBedrockMenu-1.5.1.jar` was
  uploaded; the server was started from the panel.
- **Live log evidence (verbatim from the local install record,**
  `INSTALLED-1.5.1.txt`**):**
  - `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.1`
  - `[ExcelBedrockMenu] ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.`
  - `Done (17.438s)! For help, type "help"`
  - Install record times: enabling at **12:54:18 AM CDT**, server
    `Done` at **12:54:21 AM CDT**, October 16, 2026.
  - Final player list: **0 of a maximum of 10 players online** (0/10).
- **Time note, stated plainly:** the brief for this draft said
  "installed LIVE ~12:57 AM Oct 16". The local install record — the
  primary source — says enabling at 12:54:18 AM CDT and `Done` at
  12:54:21 AM CDT. This record uses the install record's times. "~12:57
  AM" should be treated as an approximate handoff time, not the
  verified log time.

## 8. Rollback

If anything is off with the button in game:

1. Shut the server down from the panel.
2. Delete `ExcelBedrockMenu-1.5.1.jar`.
3. Rename `ExcelBedrockMenu-1.5.0.jar.bak` back to
   `ExcelBedrockMenu-1.5.0.jar`.
4. Start the server and confirm 1.5.0 enables.

The rollback jar was kept at install time:
**`ExcelBedrockMenu-1.5.0.jar.bak`** (per `INSTALLED-1.5.1.txt`:
"Rollback jar kept: ExcelBedrockMenu-1.5.0.jar.bak").
No data-folder, config, menu-file, or economy change is involved in
either direction.

## 9. What this release does not do

- It does not add, move, rename, or remove any other menu button.
- It does not change `/ag`, the guild world, EliteMobs, or any NPC —
  it only dispatches the existing command as the player.
- It does not change the Java menu — that companion edit is §6 and was
  done separately.
- It does not install, prepare, or claim ExcelBedrockMenu 1.5.2 or
  1.5.3. 1.5.2 was an unshipped intermediate build (per the 1.5.3
  packet). 1.5.3 is a separate, not-yet-installed packet — see §11.

## 10. Publication status and the one open check

1. **Owner's in-game Bedrock tap test — NOT DONE YET.**
   Open `/menu` (or use the compass) on a real Bedrock client, confirm
   "Adventurers Guild" appears immediately after "Spawn Shop", tap it,
   and confirm arrival at the guild. Until that happens, every
   Bedrock-check field for this button stays **NOT DONE**, however
   clean the build, smoke test, and live startup log are. (A passed
   `/ag` check for the guild itself — the owner landing at the guild
   via the typed command during EliteMobs Phase 2 — is a different
   check and does **not** close this one.) Per this repo's pattern
   (the ExcelLeaderboard 1.2 record was published with its in-game
   checks stated as pending), this record publishes with the check
   open and named, not silently.
2. **GitHub authorization — given for this publish.** The owner's
   standing ask-first rule for GitHub writes was lifted for the
   Oct 16, 2026 overnight queue ("Yes you can do the GitHub writes",
   ~1:33 AM CDT); that is the authority under which this record and
   its draft PR exist.

Still owed after this record, in the usual order: the website delta
in §11 (application route: browser web-upload — the connector CLI's
~128 KB single-argument cap cannot carry the ~192 KB `index.html`),
a byte-for-byte read-back of the published website page against the
applied file, and an update to the local version/rollback map
(`~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md`), which at draft
time had no ExcelBedrockMenu 1.5.1 row and still grouped this plugin
under its pre-rename record line.

---

## 11. Website delta (prepared, not applied)

**Status: PREPARED ONLY as of this record's publication. Nothing in
this section has been applied to any website file, branch, or page;
application is a separate step (browser web-upload route, then
byte-for-byte live verification).**

### What was inspected

- Prepared website copy: `~/workspace/wrv-006/publishing/index.html`
  (191,783 bytes; the file the WEBSITE-PR-BODY beside it describes —
  the EliteMobs Phase 2 / guild-open website update).
- Its companion note: `~/workspace/wrv-006/publishing/WEBSITE-PR-BODY.md`.
- How plugins/versions are listed on that page, as found:
  - An **"All plugins" table** (`<section id="all-plugins">`) with
    columns *Plugin / documentation name | Live version | What it does*.
    The ExcelBedrockMenu row currently reads — quoted exactly:
    > `<tr><td>ExcelBedrockMenu (BedrockMenu)<small class="plugin-next">Created by us</small></td><td>1.4.2</td><td>Shows the server menu on phones, tablets, and consoles.</td></tr>`
    Note the lag this exposes: the table still says **1.4.2** even
    though the renamed 1.5.0 went live October 15 and 1.5.1 is now
    live. The delta below goes straight 1.4.2 → 1.5.1 in the table;
    1.5.0 was a rename release that was never listed in this table, and
    the changelog wording below does not invent a 1.5.0 website entry.
  - A **"Server menu compass" guide panel**
    (`id="panel-server-menu-compass"`) which currently says — quoted
    exactly, in part:
    > "Both menus include Spawn and Spawn Shop; the phone/console menu also includes Leaderboard and Teleport to a player."
    It does not mention a guild button on either menu.
  - A **"Change log"** (`<section id="changelog">`), newest first,
    one `<details>` block per day. The current newest block is
    **Oct 16, 2026** (`<details open>`), containing the single
    "The Adventurer's Guild is open" entry from the Phase 2 website
    update. It says nothing about a menu button, because at the time
    that file was prepared the button was not live yet.

### The exact delta proposed for 1.5.1 — three edits, `index.html` only

**Edit 1 — plugin table row: version 1.4.2 → 1.5.1, description gains
the guild button.**

Replace the row quoted above with exactly:

> `<tr><td>ExcelBedrockMenu (BedrockMenu)<small class="plugin-next">Created by us</small></td><td>1.5.1</td><td>Shows the server menu on phones, tablets, and consoles. Version 1.5.1 adds an Adventurers Guild button, right after Spawn Shop, that runs /ag and takes you to the Adventurer's Guild hall.</td></tr>`

**Edit 2 — "Server menu compass" panel: one sentence updated.**

Replace the sentence quoted above with exactly:

> "Both menus include Spawn and Spawn Shop, and both now include Adventurers Guild — tap it to run /ag to the guild hall. The phone/console menu also includes Leaderboard and Teleport to a player."

No other sentence in that panel changes. In particular, its existing
note about the Jobs & Money shortcut ("The Bedrock menu still has that
button…") stays as written — it is still true for 1.5.1, whose button
list in §2 still includes Jobs & Money.

**Edit 3 — Change log: one new entry, added to the existing
Oct 16, 2026 block, after the guild-open entry.**

Add exactly this list item:

> `<li>Updated <b>ExcelBedrockMenu to 1.5.1</b>: the phone and console menu gains an <b>Adventurers Guild</b> button right after Spawn Shop. Tap it and it runs <code>/ag</code> for you, the same way the Spawn Shop button runs <code>/shop</code>, so Bedrock players can get to the guild hall from the menu instead of typing the command. The Java menu got the matching guild item at the same time (a bell in slot 12, tap to run <code>/ag</code>). Only the menu changed; no data, balances, or other buttons moved, and the 1.5.0 jar is kept on the server as the rollback. <b>Still to check in game:</b> tapping the new button on a real phone or console and landing at the guild — that tap test has not been done yet.</li>`

That is the whole 1.5.1 website delta: three edits, one file. The
"Still to check in game" sentence in Edit 3 must stay in the text as
long as the §10 tap test is still open; if the tap test passes before
the delta is applied, that sentence — and only that sentence — is the
part to update before publishing, and the record above must be updated
to match.

### Flag — 1.5.3 follow-up delta must WAIT

- An **ExcelBedrockMenu 1.5.3** packet is already built, locally, at
  `~/workspace/excelguildgate/install-prep/` (jar SHA-256
  `b0801922c85a07bc501d3233a4795ff7713213cb72b6cf4c7c1f4afa4d3fca36`,
  verified locally during this draft's preparation). Per its install
  packet, 1.5.3 is part of the combined "Road to the Guild" campaign
  packet with ExcelGuildGate 0.1.0 and a DeluxeMenus menu update. It:
  - adds a **"Road to the Guild"** button immediately before the
    "Adventurers Guild" button, running **`seals`**; and
  - makes the "Adventurers Guild" button **label lock-aware**, tied to
    the ExcelGuildGate campaign — it shows a 🔒 prefix
    ("🔒 Adventurers Guild") while the guild is sealed, computed at
    menu-open time, with dispatch still `ag` either way.
- **1.5.3 is NOT installed.** An install watch for that combined
  packet is armed (local cron record
  `excelguildgate-install-watch`, 15-minute interval, empty-server
  gate, expiry October 16, 2026, 5:21 PM CDT as written in that cron),
  but armed is not installed: at the time of this draft there is no
  `INSTALLED.txt` for that packet in its install-prep folder, and the
  live jar is the 1.5.1 jar in §1.
- **Therefore: no website wording for 1.5.3 is proposed in this
  draft, and none should be applied yet.** Drafting it now would put
  a "Road to the Guild" button and a lock label on the public page
  for a menu players do not have. A follow-up website delta for
  1.5.3 — plugin-table version/description, compass-panel wording,
  and a new changelog entry — is owed **only after 1.5.3 is actually
  live** and its own install record exists, and it belongs in that
  release's own publishing pass, not folded into this 1.5.1 delta.

---

## Sources consulted for this draft

Local files only; no GitHub, website, or server access was used.

- `~/workspace/excelbedrockmenu-guild/install-prep/INSTALL-PACKET.md`
  — build, scope proof, bytecode, smoke test, install steps.
- `~/workspace/excelbedrockmenu-guild/install-prep/INSTALLED-1.5.1.txt`
  — live install times, gate checks, log lines, rollback jar.
- `~/workspace/excelbedrockmenu-guild/install-prep/ExcelBedrockMenu-1.5.1.jar`
  — SHA-256 re-hashed locally for this draft; matches.
- `~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md` — live-version /
  rollback pattern, the EliteMobs Phase 2 row, and the separate-status
  rule this record follows.
- `~/workspace/wrv-006/GUILD-INSTALL-RUNBOOK.md` §10 — the guild
  install record pattern (PR #23 reference) and its honest
  done/pending split.
- `~/workspace/wrv-006/publishing/index.html` and
  `~/workspace/wrv-006/publishing/WEBSITE-PR-BODY.md` — website listing
  structure and the exact current lines quoted in §11.
- `~/workspace/excelguildgate/install-prep/INSTALL-PACKET.md` and
  `MENU-DIFF.md` — the 1.5.3 packet facts flagged in §11.
- `~/workspace/renames-2026-10-15/build.sh` and
  `~/workspace/renames-2026-10-15/src/ExcelBedrockMenu/` (`build.sh`,
  `plugin.yml`) — the §4 wart and the tree's move to 1.5.3, read
  directly.

### Could not be verified from local files

- **`EliteMobs/GUILD-INSTALL-RECORD.md` itself was not found locally**
  under `~/workspace/wrv-records/` (or elsewhere in the workspace
  trees checked). Only references to it exist locally: the version
  map and the WRV-006 runbook both cite it as the file in wrv-plugins
  draft PR #23, branch `record/elitemobs-guild-phase2-2026-10-16`.
  This draft mirrors the *structure and detail level* of the local
  records that are present (install packets, install records, the
  version/rollback map, website content drafts) — it does not claim
  to have read the guild record file itself.
- The live server's current jar, log, and player state were **not**
  re-checked for this draft (no server access was in scope). "Live"
  in this record rests on `INSTALLED-1.5.1.txt` and the task's
  verified facts, not on a fresh panel read.
- Whether the 1.5.3 install watch has fired since the cron record was
  written was not checked against the server; §11 relies on the
  absence of an `INSTALLED.txt` in the 1.5.3 packet folder at draft
  time and on the task's statement that 1.5.3 is not yet installed.

*End of draft.*
