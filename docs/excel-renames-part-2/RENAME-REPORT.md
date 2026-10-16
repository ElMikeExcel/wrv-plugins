# WRV Plugin Renames Part 2 — Build Report (2026-10-16)

> **Live update.** The four jars in this report were installed live
> October 16, 2026, ~8:46 AM CDT via auto-watch (empty-server gates
> 8:41 AM and 8:43 AM CDT). Old jars were kept as `.bak-20261016`.
> The "staged only" wording below is the state when this build report
> was written, before that install. This draft pull request on branch
> `record/excel-renames-part-2-2026-10-16` is the record. In-game
> checks (WealthBoard popup, phantoms, first weekend-boundary
> WeekendXP line) and website name updates are still open. Jars are
> not in this commit; they live in Google Drive Build files. The
> PR #1 RestartCountdown 1.1.1 line is ABANDONED / UNUSED. `/restartwarn`
> was kept as an alias.

**Scope:** Full identity renames for the four remaining homemade plugins, as ONE
combined suite with ONE install packet, per Mike's order of October 16, 2026
(~7:58 AM CDT). This order supersedes the Oct 13 do-not-rename stance for these
four, exactly as his Oct 15 order did for the menu trio.
**Staged only. Nothing was installed, the live server/panel was not touched,
and nothing was pushed to GitHub.** Route Sheet: WRV-060, row 66.
(That sentence is the pre-install build state. See the live update above.)

| Old | New | Old version | New version | Jar |
|---|---|---|---|---|
| WealthBoard | **ExcelWealthBoard** | 1.3.0-mobile.7 | **1.4.0** | `ExcelWealthBoard-1.4.0.jar` |
| WeekendXP | **ExcelWeekendXP** | 1.0.2 | **1.1.0** | `ExcelWeekendXP-1.1.0.jar` |
| NoPhantoms | **ExcelNoPhantoms** | 1.0.0 | **1.1.0** | `ExcelNoPhantoms-1.1.0.jar` |
| RestartCountdown | **ExcelRestartCountdown** | 1.1.0 | **1.2.0** | `ExcelRestartCountdown-1.2.0.jar` |

SHA-256 of the staged (final) jars:

```
f989a82431e0a5fb509bd8cff9f15b9452abbd4dfae332cdf816e5b6517cfee2  ExcelWealthBoard-1.4.0.jar       (13,844 B)
e9150198dd3260ad5ae829067f84131cc2dabc135e495e6c5d7e41f228417fa1  ExcelWeekendXP-1.1.0.jar         (19,226 B)
2c8f3a7688df3f11f295b29d220de74d4c08f37b0ba4d6a103f5d53128529609  ExcelNoPhantoms-1.1.0.jar        (7,294 B)
50dfa7b89b6bad50ce368bda198314d6b3d429522c77c8f811e95124f04530cd  ExcelRestartCountdown-1.2.0.jar  (10,801 B)
```

SHA-256 of the live jars being replaced (from the pre-guild backup,
`~/workspace/backups/pre-guild-20261015/root/plugins/`):

```
1bcfd3d1679d0fe2551188a2e53905ca42bac0e8a071a1ee4fe995a3d205ddcd  WealthBoard-1.3.0-mobile.7.jar  (7,541 B)
4452ccd90bea409fca37740d3e4e9967ef6dd56e9280000521eec868cf0254d0  WeekendXP-1.0.2.jar               (9,568 B)
240e2cf7660a7cf515bf341bd8ed1ed7581099b933060e01c11c5c926b186195  NoPhantoms-1.0.0.jar              (1,704 B)
5ed80156802ee4303c719dcd7e2dba245786a50b81f2d99384ff0d6bbb2d08d0  RestartCountdown-1.1.0.jar        (3,335 B)
```

Renamed sources are staged under `src/ExcelWealthBoard/`, `src/ExcelWeekendXP/`,
`src/ExcelNoPhantoms/`, `src/ExcelRestartCountdown/` (sources + tests +
`plugin.yml` each). `build.sh` in this folder is the harness that produced the
jars (JDK 25 at `~/workspace/.jdk25`, `javac --release 21`); `make-jar.py` is
the deterministic packager; `run-tests.sh` runs the suites. Boot logs are in
`verification/`; per-class scope diffs are in `build/scope-diff/`.

---

## 0. Source-of-record proof (before any renaming)

Each rename was built from the source that provably IS the live build. For
every plugin, the candidate source was compiled **under its original
identity** and its normalized `javap -c` output compared against the live jar
from the pre-guild backup: **0 diff lines for all four** (constant-pool
indices and lambda numbering normalized). Only then was the rename applied.

- **WealthBoard 1.3.0-mobile.7** = wrv-plugins PR #12 branch
  `fix/wealthboard-popup-cycle-2026-10-13`. The LOCAL tree
  (`~/workspace/wealthboard/`) is STALE mobile.6 and was **not** used. The
  mobile.6→mobile.7 delta is exactly the Mike-approved title-timing fix:
  intermediate popup frames fadeOut 0 / stay 70 (was 5 / 55), last frame
  fadeOut 15 / stay 55.
- **WeekendXP 1.0.2** = wrv-plugins PR #2 branch
  `feature/together-weekend-xp-2026-10-12` (three classes: WeekendXP,
  WeekendSchedule, VanillaXpBridge). The LOCAL tree is STALE 1.0.0 (a
  different, mcMMO-only implementation) and was **not** used.
- **NoPhantoms 1.0.0** = local tree `~/workspace/nophantoms/` (repo Java
  identical; local `plugin.yml` matches the live jar).
- **RestartCountdown 1.1.0** = local tree `~/workspace/restartcountdown/`.
  **Flag:** the wrv-plugins PR #1 "canonical" source is a different,
  **uninstalled 1.1.1 build** (api-version 1.21, title announcements,
  onDisable cancellation, separate cancellable shutdown task). This rename
  is built from live 1.1.0, NOT PR #1, so no unshipped behavior sneaks in
  under cover of a rename. The PR #1 RestartCountdownTest targets 1.1.1 and
  does not apply to this line; a new behavior suite was written for 1.1.0
  (see §5).
  **DECIDED Oct 16, 2026:** Mike marked the divergent 1.1.1 line
  ABANDONED / UNUSED — never installed, do not build or ship it. The
  live line was 1.1.0, renamed here to ExcelRestartCountdown 1.2.0
  (WRV-060). Marking comment on PR #1:
  https://github.com/ElMikeExcel/wrv-plugins/pull/1#issuecomment-6216545722

---

## 1. Carry-over audit (all four, before building)

Method: read each live jar's `plugin.yml` and bytecode; grep the entire
pre-guild backup plugin tree (configs, menus, data files) and all local
source trees for the old names, main classes, commands, permissions, and
placeholder identifiers.

| Item | WealthBoard | WeekendXP | NoPhantoms | RestartCountdown |
|---|---|---|---|---|
| Old main class | `com.winkreyes.wealthboard.WealthBoard` | `com.winkreyes.weekendxp.WeekendXP` | `com.winkreyes.nophantoms.NoPhantoms` | `com.winkreyes.restartcountdown.RestartCountdown` |
| New main class | `com.winkreyes.excelwealthboard.ExcelWealthBoard` | `com.winkreyes.excelweekendxp.ExcelWeekendXP` | `com.winkreyes.excelnophantoms.ExcelNoPhantoms` | `com.winkreyes.excelrestartcountdown.ExcelRestartCountdown` |
| Data folder (old → new) | `plugins/WealthBoard` → `plugins/ExcelWealthBoard` | `plugins/WeekendXP` → `plugins/ExcelWeekendXP` | `plugins/NoPhantoms` → `plugins/ExcelNoPhantoms` | `plugins/RestartCountdown` → `plugins/ExcelRestartCountdown` |
| Live data folder exists? | **No** (none of the four has one in the backup) | No | No | No |
| Config keys | None (no config file; behavior compiled in) | None | None | None |
| Commands | None | None | None | `/restartwarn` (admin, console/op) |
| Permissions | None | None | None | `restartcountdown.warn` (default op) |
| PlaceholderAPI expansions | None | None | None | None |
| depend / softdepend | `depend: [Vault]`, `softdepend: [Jobs]` | `depend: [mcMMO]` | none | none |
| api-version (preserved from live jar) | `"1.13"` | `"1.21"` | `"1.13"` | `"1.13"` |

Cross-reference findings:

- **No functional references** to any of the four old names exist anywhere
  in the backup plugin tree's configs/menus. The only hit is a descriptive
  comment in mcMMO's default config ("Stacks multiplicatively with
  WeekendXP…") — prose about behavior, not a lookup; it needs no change.
- **WealthBoard** looks up the third-party **Jobs** plugin by its real name
  (`getPlugin("Jobs")`) and Vault's economy service — both unaffected by
  our rename. Its leaderboard state is in-memory only (rebuilt from
  Vault/Jobs on enable), so there is **no persisted player data to
  migrate**. Its popup/welcome layout is Mike-approved current behavior —
  **rename only, zero behavior changes** (proven in §4/§5).
- **WeekendXP** looks up **mcMMO** by name and dispatches the namespaced
  console commands `mcmmo:xprate 1.2 true` / `mcmmo:xprate reset` — mcMMO
  keeps its name, so both keep working (proven live in the boot test, §5).
  The vanilla-XP half reaches the custom mcMMO build through a reflection
  bridge (`VanillaXpBridge`); no compile-time dependency.
- **RestartCountdown's `/restartwarn`** is deliberately excluded from the
  WRV-058/059 player command catalog (admin-only); the ExcelBedrockMenu
  test denylist names it — a test string in another plugin's test tree,
  not a runtime dependency.
- **LuckPerms** stores its data in an H2 database (not decoded). No config
  file in the backup grants any of these nodes; `ops.json` is empty and
  admin runs through the console, so no stored grant can strand. As
  insurance the renamed plugin still accepts the legacy permission node
  (§3).
- ExcelLeaderboard sources mention "WealthBoard-style" in comments only —
  cosmetic, no action.
- None of the four registers a PlaceholderAPI expansion, so no
  `%placeholder%` in any menu can break.

**Persisted player data:** none of the four has a live data folder, so in
practice there is nothing to migrate on install day. The migration helper
(§2) is still built into all four per the playbook standard, and is fully
tested (§5): if a legacy folder ever exists (e.g. created by a future
config-bearing build, or present on a restored backup), the first boot
under the new name carries it over.

---

## 2. Migration design (identical in all four)

One helper class per plugin, `LegacyDataMigration` in the plugin's own new
package — byte-identical across the four except package and the plugin name
in its javadoc (verified by normalized hash). Wired as the **first step of
`onEnable`** in all four mains (through a null-guarded
`migrateLegacyDataFolder()`, so unit tests driving `onEnable` on mocks are
unaffected; on a real server the guard never triggers).

Semantics:

- **Copy, never move or delete.** The legacy folder is left in place as
  part of the rollback record.
- Copies recursively, byte-for-byte, with `COPY_ATTRIBUTES`, only while
  the new folder contains **no files at all** — a returning install is
  never overwritten.
- No-op when the legacy folder is missing (fresh install) or empty.
- Best-effort by design: per-file copy failures are logged and skipped;
  an unreadable legacy folder is logged and skipped. A mid-walk I/O
  failure (surfaces as `UncheckedIOException`) is also caught — migration
  can never stop a plugin from enabling. (This catch was widened during
  the cold review pass, §6.)
- On a real migration it logs exactly:
  `Migrated legacy data folder plugins/<Old> -> plugins/<New> (N files)`.

---

## 3. What changed per plugin (the complete change set)

### ExcelWealthBoard 1.4.0 (was WealthBoard 1.3.0-mobile.7)

- Package `com.winkreyes.wealthboard` → `com.winkreyes.excelwealthboard`;
  class `WealthBoard` → `ExcelWealthBoard` (inner `Entry` class along).
- `plugin.yml`: name `ExcelWealthBoard`, version `1.4.0`, description text
  carried over; `depend`/`softdepend`/api-version unchanged.
- Version choice: the `-mobile.N` suffix chain is dropped. 1.4.0 is a
  clean minor bump in the Oct 15 pattern (renames ship as new minor
  versions); the suffix was a build-channel tag, not a version line any
  tooling depends on (nothing reads it — audit §1).
- Log/identity strings that embed the plugin name updated ("WealthBoard
  enabled…" style lines now read ExcelWealthBoard where the name is the
  plugin's own; the enable line was already name-generic in mobile.7's
  successor strings — see scope diff).
- Added: `LegacyDataMigration` + the `migrateLegacyDataFolder()` wiring.
- **Nothing else.** Popup frames, timing, Jobs auto-enroll, welcome
  behavior: byte-proven unchanged (§4, §5).

### ExcelWeekendXP 1.1.0 (was WeekendXP 1.0.2)

- Package `com.winkreyes.weekendxp` → `com.winkreyes.excelweekendxp`;
  classes `WeekendXP` → `ExcelWeekendXP`; `WeekendSchedule` and
  `VanillaXpBridge` keep their class names inside the new package.
- `plugin.yml`: name `ExcelWeekendXP`, version `1.1.0`; `depend: [mcMMO]`
  and api-version `"1.21"` unchanged.
- One identity string inside `VanillaXpBridge`: the bridge-missing
  warning now reads "ExcelWeekendXP 1.1.0 requires the matching custom
  mcMMO vanilla XP bridge…" (was "WeekendXP 1.0.2 requires…"). Verified
  at the constant-pool level; it is the only string change in that class.
- Added: `LegacyDataMigration` + wiring.
- Schedule logic, 1.2x rates, retry behavior, dispatched commands:
  unchanged (byte-proven, §4).

### ExcelNoPhantoms 1.1.0 (was NoPhantoms 1.0.0)

- Package `com.winkreyes.nophantoms` → `com.winkreyes.excelnophantoms`;
  class `NoPhantoms` → `ExcelNoPhantoms`.
- `plugin.yml`: name `ExcelNoPhantoms`, version `1.1.0`.
- Enable log line updates to the new name (it already read
  "ExcelNoPhantoms enabled…" form in the renamed source — the string is
  the plugin's own identity line).
- Added: `LegacyDataMigration` + wiring.
- The phantom-cancel handler is instruction-for-instruction identical.

### ExcelRestartCountdown 1.2.0 (was RestartCountdown 1.1.0)

- Package `com.winkreyes.restartcountdown` →
  `com.winkreyes.excelrestartcountdown`; class `RestartCountdown` →
  `ExcelRestartCountdown`.
- `plugin.yml`: name `ExcelRestartCountdown`, version `1.2.0`.
- **Command:** canonical name is now **`excelrestartwarn`**, with
  **`restartwarn` kept as a registered alias** — anything (console habit,
  notes, runbooks) using `/restartwarn` keeps working. The handler checks
  the canonical name; Bukkit resolves the alias to the same command.
  In-chat help/usage strings still show `/restartwarn` (the alias works;
  strings left untouched to keep the rename scope minimal).
- **Permission:** canonical node is now **`excelrestartcountdown.warn`**
  (declared, default op). The legacy **`restartcountdown.warn`** node is
  **also declared** (default op) and the handler accepts **either** node,
  so any grant made under the old name keeps working.
- Added: `LegacyDataMigration` + wiring.
- Countdown timing (2 minutes), broadcasts, cancel semantics, shutdown
  call: unchanged (byte-proven §4, behavior-tested §5).

Identifiers summary — kept vs changed:

| Identifier | Fate |
|---|---|
| `/restartwarn` | **Kept working** (alias of `/excelrestartwarn`) |
| `restartcountdown.warn` | **Kept working** (declared + dual-accepted) |
| `mcmmo:xprate …` dispatches | Unchanged (mcMMO is third-party, not renamed) |
| Jobs/Vault lookups by name | Unchanged (third-party names) |
| Plugin names / data folders / jar names / main classes | Changed (the rename itself), with first-boot data-folder migration |
| Placeholder identifiers | None existed; nothing to keep |

---

## 4. Change-scope proof (per jar)

Normalized `javap -p -c` diff of every class, old live jar vs new jar
(class/package names normalized to the new identity; constant-pool
indices, instruction addresses, and lambda numbers normalized). Full
reports: `build/scope-diff/<Plugin>.txt`.

| Class | Diff | Reading |
|---|---|---|
| WealthBoard$Entry → ExcelWealthBoard$Entry | **0 lines** | untouched |
| WealthBoard → ExcelWealthBoard | 47 lines | only the added migration call, `migrateLegacyDataFolder()` + its log lambda |
| VanillaXpBridge | **0 lines** bytecode (+ the one §3 string in the constant pool) | untouched logic |
| WeekendSchedule, WeekendSchedule$Result | **0 lines** | untouched |
| WeekendXP → ExcelWeekendXP | 47 lines | migration wiring only |
| NoPhantoms → ExcelNoPhantoms | 44 lines | migration wiring only |
| RestartCountdown → ExcelRestartCountdown | 59 lines | migration wiring + canonical command-name check + dual permission check |

New-only class in every jar: `LegacyDataMigration` (the §2 helper) — and
nothing else. No class was removed. `plugin.yml` in each jar read back
after the build shows the new name, version, and main class.

---

## 5. Tests and verification (Moon's playbook)

Toolchain: JDK 25 (`~/workspace/.jdk25`), `javac --release 21`, the
Oct 15 classpath (ExcelShops build-libs: Paper API + runtime libraries,
VaultAPI; the backup's own `Jobs.jar` for WealthBoard's compile).

**Automated tests — 63 passing, 0 failing** (`run-tests.sh`; JUnit
platform console 1.11.4 + Mockito 5.15.2):

| Suite | Tests | Covers |
|---|---|---|
| ExcelWealthBoard | 16 | the repo's mobile.7 behavior contract (popup frame sequence incl. the exact mobile.7 timings fadeOut 0/stay 70, last frame 15/55; welcome/Jobs-enroll paths) renamed only in package/class names + 5 migration tests |
| ExcelWeekendXP | 30 | the repo's suites renamed (WeekendSchedule day logic, VanillaXpBridge reflection behavior, main plugin enable/dispatch) + 5 migration tests |
| ExcelNoPhantoms | 7 | the repo's phantom-cancel tests renamed + 5 migration tests |
| ExcelRestartCountdown | 10 | **new** suite written for the live 1.1.0 line (the PR #1 suite targets uninstalled 1.1.1): permission refusal, **legacy node alone works**, new node works, unknown command ignored, start/refuse-second/cancel flow + 5 migration tests |

The 20 migration tests (5 per plugin) prove: nested legacy tree copies
byte-for-byte with the legacy tree untouched; skip when the new folder
already has any file; missing-legacy no-op; empty-legacy copies nothing;
an existing-but-empty new folder still migrates.

**Rebuild reproduction:** two consecutive from-source builds produce
**byte-identical jars** for all four (SHA-256 in the header table).
Packaging note, stated plainly: the JDK `jar` tool stamps entries with
build time and did not honor `SOURCE_DATE_EPOCH` in this environment, so
`make-jar.py` writes the zip deterministically instead (sorted entries,
fixed 2026-01-01 timestamps, and the same manifest bytes the jar tool
emits). Entry contents are the compiler's output either way.

**Scratch-Paper boot tests** (Paper 26.2 build 123 from the local
runtime; support plugins from the backup: Vault, EssentialsX +
EssentialsXSpawn, Jobs, CMILib, the custom mcMMO build, PlaceholderAPI).
Logs in `verification/`:

- **Boot A** (`bootA-migration.log`, pre-hardening build) and **Boot C**
  (`bootC-final-jars-migration.log`, **final jars**): legacy folders
  seeded with a marker file + nested data file for all four plugins.
  Result, all four: `Migrated legacy data folder plugins/<Old> ->
  plugins/<New> (2 files)`; migrated files **byte-identical** (`cmp`),
  legacy folders intact; clean enable lines; `version <NewName>`
  answers the new version for all four; **both** `excelrestartwarn
  cancel` and `restartwarn cancel` answer "No countdown is running.";
  zero SEVERE anywhere in the final-jars log; no error/warning naming
  any of the four plugins.
- **WeekendXP against the real custom mcMMO (Boot A + C):**
  `Weekday detected: dispatched mcMMO XP-rate reset command; verified
  vanilla XP event multiplier 1.0x.` — the reflection bridge confirms
  support on the actual installed mcMMO build, and the `mcmmo:xprate`
  dispatch path works end-to-end. (Oct 16, 2026 is a Friday — the
  weekday branch is the correct one.)
- **Boot B** (`bootB-fresh-and-both-present.log`, pre-hardening build):
  ExcelWealthBoard with BOTH folders present (new folder already holding
  a file) → migration correctly skipped, pre-existing file untouched,
  legacy file not copied over; the other three with no folders at all
  (fresh install) → clean enables, zero migration lines, zero SEVERE.

**Not proven headless (install-day in-game checks, named honestly):**

- A real player join under ExcelWealthBoard (popup actually rendering +
  Jobs auto-enroll firing). The frame contract is pinned by the 16-test
  suite and enable + Vault/Jobs hookups are boot-proven; the visual
  check needs a client.
- Phantom suppression in a live world (the handler is byte-identical
  and unit-tested; no headless player exists to trigger a spawn cycle).
- A real `/excelrestartwarn` countdown start (never fired in probes on
  purpose — it ends in a server shutdown; the start path is covered by
  the unit suite's start/refuse-second/cancel flow instead).

---

## 6. Cold self-review pass

Re-read cold after the builds, hunting edge cases:

1. **Mid-walk migration failure** — found and fixed: `Files.walk` can
   throw `UncheckedIOException` during iteration, which the original
   `catch (IOException)` would have missed, letting a corrupt legacy
   folder fail the whole plugin enable. The catch in all four helpers
   now covers `IOException | RuntimeException`, with a comment saying
   why; helpers re-verified byte-identical across the four (modulo
   package/name), full suite re-run (63/63), jars rebuilt, and the
   migration boot re-run on the final jars (Boot C).
2. **Legacy path is a regular file, not a folder** — `isDirectory()`
   guard returns 0 before any walk. Safe.
3. **Both folders present** — skip-if-any-file rule; proven in Boot B
   and by unit tests.
4. **Empty new folder + full legacy folder** — migrates (unit-tested).
5. **Old jar and new jar both present at install** (operator error) —
   both would enable (different plugin names) and WealthBoard would
   double-fire its join popup. The install packet prevents this by
   renaming all four old jars to `.bak-20261016` **before** the new jars
   go in; the packet's verification step lists the expected plugin set.
6. **Countdown running during the install shutdown** — the install runs
   with the server panel-stopped; no countdown can be running.
7. **LuckPerms stored grants** — none exist for these nodes (audit §1);
   the legacy node is dual-accepted anyway.
8. **`getDataFolder()` null under test mocks** — the wiring is
   null-guarded; production behavior on a real server is unchanged
   (proven by Boots A–C).
9. **Symlinks inside a data folder** — `Files.walk` without
   FOLLOW_LINKS does not descend into linked directories; plugin data
   folders on this server contain no symlinks. Accepted, noted.
10. **Version-string drift** — VanillaXpBridge's warning embeds the
    version; updated to 1.1.0 with the rename and verified in the
    constant pool.

## 7. Mistakes made during this build (owned plainly)

- The first `migrateLegacyDataFolder()` insertion into
  `ExcelNoPhantoms.java` landed **after** the class's closing brace
  (method outside the class + a stray brace). The compiler caught it;
  the file was restructured and all four mains re-verified. No jar was
  ever built from the broken file.
- The first test run showed 16 failures (WealthBoard 11, WeekendXP 5),
  all one cause: the repo behavior suites drive `onEnable()` on Mockito
  mocks whose `getDataFolder()` returns null, and the new migration
  wiring dereferenced it. Fixed with the null-guarded helper call
  (§2) rather than by weakening any test; 63/63 after.
- The first two builds were not byte-identical (zip entry timestamps).
  Root-caused to the `jar` tool ignoring `SOURCE_DATE_EPOCH` here;
  fixed with the deterministic packager (§5) and reproduction re-proven
  on the final jars.
- Boot C's first attempt died on a **full /tmp** (512 MB tmpfs; three
  250 MB runtime copies). Cleaned up and re-run lean (no world/logs in
  the copy); the failed attempt produced no evidence and is not counted.

## 8. Files in this packet

- The four jars (header table) — staged, NOT installed.
- `src/` — renamed sources + tests; `build.sh`, `make-jar.py`,
  `run-tests.sh` — the build/test harness.
- `build/scope-diff/` — per-plugin normalized javap scope reports.
- `verification/` — boot logs A (migration, pre-hardening build),
  B (fresh + both-present, pre-hardening build), C (migration, final
  jars).
- `INSTALL-PACKET.md` — the combined install + rollback + auto-watch
  wording.
- `records/` — four PLUGIN-RECORD drafts (publishing happens at install
  time, not now).

*End of report.*
