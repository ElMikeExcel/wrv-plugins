# SOURCE.md — Menu toggle fix (Auto-sort state label + Auto-open reorder)

Built Nov 3, 2026 by Excel (Iris) for Wink Reyes Valley, at Mike's
direction (Nov 3, 2026, 1:24 PM CST). Desk/staging only — nothing in
this folder has touched the live server.

## The defect and its root cause

Mike's screenshot (Nov 3) shows the Bedrock Server Menu's Settings
group: `Auto-sort my chests: on/off` — a static literal — beside the
state-showing `Silk Touch Warnings: ON`.

**Root cause, in three parts:**

1. **The entry was never converted.** `entries/autosort.yml` is
   byte-identical (503 B, SHA-256 `c1a45e8f…`) in the pre-2.0 base
   tree, the Phase 1 tree, the Phase 2 source tree, the Phase 2 rig,
   and the live-record copy from the data folder. Menu 2.0's registry
   conversion (Oct 31 – Nov 2) was a mechanical conversion of the
   then-live menu data; the old fixture's static entry rode through
   unchanged. The Nov 1 analysis correctly designed the fix (mirror
   `silkwarnings.yml`) and recorded it as "folded into Menu 2.0
   Phase 2 work" — but no Phase 2 artifact ever contained the
   conversion; Phase 2 shipped data conversion + code, not this fix.
2. **A data-only fix was impossible anyway.** The states pattern
   needs a status feed. AutoSort's owner is the third-party plugin
   **AutoSort 0.1.17 by nothub** (installed Oct 24, 2026) and it
   exposes **no feed at all**: verified in the upstream source
   (GitHub `nothub/AutoSort`, tag `v0.1.17` = commit
   `5349a47980866359428a63121c77260ac0685cf3`, cloned to
   `work/autosort-upstream/`) and in the shipped jar's bytecode
   (Modrinth file `autosort-0.1.17+26.3-paper.jar`, 6,677 B,
   SHA-1 `0a385f05e7991150f5fef92dca4eb1d62fd36fe6` — matches
   Modrinth's published hash exactly; SHA-256 `448a5fff…`, kept at
   `work/autosort-0.1.17+26.3-paper.jar`). Its full state contract:
   a private `Set<UUID> actives` (membership = sorting ON; absence =
   OFF, the owner's default), persisted synchronously on every toggle
   to `plugins/autosort/active.txt` (lowercase folder, one UUID per
   line), toggled by `/autosort` (perm `autosort.toggle`, default
   true). No PlaceholderAPI expansion, no public status method, no
   status subcommand.
3. **So the missing piece was a build, and it was never built** —
   until now: **ExcelAutoSortBridge 0.1.0** (below), following the
   AimAssist 0.1.4 precedent (`%aimassist_status%` was added to that
   owner for exactly this pattern) and the SilkWatch precedent.

## What is in this packet

| Artifact | Bytes | SHA-256 | Notes |
|---|---|---|---|
| `ExcelAutoSortBridge-0.1.0.jar` | 5,183 | `634efc5963ce037a04139ee4861c4aa361f12f77758449f960f07629fae1b22a` | new plugin; built twice, byte-identical |
| `entries/autosort.yml` | 1,994 | `36276320f836a4865cc31f592eccc7301f986058dc7de9f436007dedfd41dba8` | converted state-driven entry |
| `entries/autojoin.yml` | 1,071 | `57269d2da77cfc43a5a4dd51e7e534729fc5a12acb4766c5c8ed55d516433eee` | reorder only: `order: 30→70`, `order-bedrock: 220→250` (diff vs base = exactly those 2 lines) |

### ExcelAutoSortBridge 0.1.0

- Source: `bridge/src/com/winkreyes/excelautosortbridge/` —
  `ExcelAutoSortBridge` (plugin + read path), `AutoSortExpansion`
  (PlaceholderAPI expansion, identifier `autosort`), `AutoSortStatus`
  (pure core: file parsing + ON/OFF mapping). `bridge/plugin.yml`,
  `bridge/build.sh`, tests in `bridge/tests/`.
- Registers `%autosort_status%` → `ON` / `OFF` / null. Read path,
  fail-soft in order: (1) AutoSort's **live in-memory `actives` set**
  read reflectively from its plugin instance (same set its toggle
  flips — never stale); (2) **`plugins/autosort/active.txt`** parsed
  by the pure core (rewritten on every toggle, so it agrees);
  (3) **null** — ExcelMenu then resolves the entry to its
  "unavailable" default variant. The bridge never writes state and
  never guesses one.
- House modular law: this is a replaceable adapter. If AutoSort ever
  ships its own expansion, delete the bridge; the menu entry is
  untouched. If the bridge is absent at runtime, the entry fails
  closed to "unavailable" (proven in core validation).
- Build: `bridge/build.sh` — JDK 25 (`~/workspace/.jdk25`), Paper API
  26.3 (build 231 library set), PlaceholderAPI 2.12.3 compile-time
  only; jar timestamp pinned 2026-11-03. Two consecutive builds
  byte-identical (SHA-256 above).

### Converted `autosort.yml`

Mirrors `silkwarnings.yml` structure exactly: base = ON presentation
(LIME_DYE, `Auto-sort my chests: ON`), `status-placeholder:
'%autosort_status%'`, states `ON` / `OFF` (GRAY_DYE) / `default`
("unavailable"), each state carrying its own label/lore/action;
action `[player] autosort` + `[refresh]` (the old static entry's
`[close]` is dropped — a toggle that closes the menu can't show its
new state; both reference toggles use `[refresh]`). Added
`requires-plugin: AutoSort` + `requires-permission: autosort.toggle`
(matching the owner's own gate for its command).
`BUNDLED_ENTRIES` note: the Phase 2 jar already lists `autosort` and
`autojoin` as bundled entries and extraction is fill-missing only, so
**no ExcelMenu jar change is needed or wanted** — the live data-folder
files are the whole change. The NEXT ExcelMenu jar build must lift
these two files into `src/main/resources/entries/` verbatim or the
bundled copies regress (they only surface on a fresh data folder, but
the discipline stands).

### Reorder `autojoin.yml`

Mike (Nov 3): "Auto-open on join" goes to the BOTTOM of its list.
This supersedes the Oct 18, 2026 record (remove the entry) — the
entry is KEPT, reordered only. Settings group after the change:
Java order — autosort 10, makersmark_settings 20, wandstyle 40,
silkwarnings 50, aimassist 60, **autojoin 70 (last)**; Bedrock —
autosort 200, makersmark_settings 210, wandstyle 190, silkwarnings
230, aimassist 240, **autojoin 250 (last)**. (Wandstyle's Bedrock
order 190 puts it first on Bedrock — pre-existing, matches Mike's
screenshot, untouched.) Java layout `server_menu` settings slots
[46,47,49,50,51,52,53] → autojoin takes slot 53, the last settings
slot. Proven by resolver output (see Validation).

## Validation (all Nov 3, 2026, local)

1. **Bridge unit tests:** 15 checks, 0 failed (`AutoSortStatusTest` —
   parse tolerance, ON/OFF mapping, missing-file = unknown ≠ OFF,
   empty-file = known OFF, round-trip).
2. **Reproducible build:** two `build.sh` runs → identical SHA-256.
3. **Entry validation through the real Phase 2 core** (fresh-compiled
   `excelmenu.core` from the phase-2 source; merged registry = the 84
   live entries with the two staging files swapped in): **84 loaded,
   0 failed.** Resolver outcomes (recorded in
   `entry-validation-20261103.txt`):
   - `%autosort_status%` = ON → label `Auto-sort my chests: ON`
   - = OFF → `Auto-sort my chests: OFF`
   - placeholder unresolvable (bridge absent) → `Auto-sort my
     chests: unavailable`, GUARDED (standard unavailable action)
   - Settings group, Bedrock order: …aimassist, **autojoin last**;
     Java order: …aimassist, **autojoin last**.
4. **Rig boot** (Paper 26.3, local): REAL AutoSort 0.1.17 jar (hash-
   verified genuine file) + PlaceholderAPI 2.12.3 + live ExcelMenu
   2.0.0 jar + bridge + converted entries in its data folder.
   `Done (46.312s)`, **SEVERE 0**. Lines of record
   (`rig-boot-console-20261103.txt`):
   - `[ExcelMenu] ExcelMenu registry: 84 entries live` (no load errors)
   - `[ExcelAutoSortBridge] ExcelAutoSortBridge enabled:
     %autosort_status% expansion registered (source: live actives set).`
     — the reflective bind against the real owner succeeded.
   (The console `/version` command NPE'd — a vanilla Paper console
   quirk in this sandbox, unrelated; `plugins` listed all four
   plugins correctly.)
5. **Not proven here (needs a player client — owed as post-install
   proof):** the in-game tap flow — open menu → label shows YOUR
   state → tap → `/autosort` flips → `[refresh]` re-renders with the
   new state, on Bedrock and Java. Also `/papi parse <player>
   %autosort_status%` spot-check after a real toggle.

## Bases (verified before any delta)

| Base | Bytes | SHA-256 |
|---|---|---|
| live-record `entries/autosort.yml` (phase-2 src = rig = live copy, all identical) | 503 | `c1a45e8fd7b879c2a3eeb69d5f717dcadade661a3a141fea7d83d890c8d557a9` |
| live-record `entries/autojoin.yml` (same, all copies identical) | 1,071 | `3361a58febceec27f6230ccf019b69efbc403c88d9dfc47ff06783d3aab331f2` |
| live ExcelMenu jar (Phase 2, per its INSTALLED.txt) | — | `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2` |
| AutoSort jar (Modrinth, genuine) | 6,677 | SHA-256 `448a5fff8e9cfcf8d1152d6f0f13cd5f81964b3ea64e6cedd9407d8d314a6439`, SHA-1 `0a385f05e7991150f5fef92dca4eb1d62fd36fe6` = Modrinth's published hash |

Upstream AutoSort source: GitHub `nothub/AutoSort`, tag `v0.1.17`,
commit `5349a47980866359428a63121c77260ac0685cf3`, MIT license —
cloned at `work/autosort-upstream/` for the record. The bridge is
our own code; AutoSort itself is **not modified and not
redistributed** by this packet.

## Ownership

ExcelAutoSortBridge is ours (WinkReyesValley / Excel). The converted
entries are ours (ExcelMenu registry data). AutoSort remains
nothub's, untouched.
