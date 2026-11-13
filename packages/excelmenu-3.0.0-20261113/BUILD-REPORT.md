# ExcelMenu 3.0.0 — BUILD REPORT (desk build, re-dispatch of run 280)

**Commission:** board supervisor run 281 re-dispatch, Nov 13, 2026.
Design of record: `~/workspace/excelmenu-30-design-20261113/DESIGN.md`
(74,533 B, SHA-256 `bb82539e143c914d7be8982d776eecef3fba50876001cab8be42d2b2b5e55db1`),
including the §9 Q1–Q10 recommended defaults, all adopted as written
(root-only pins; server_menu-only rearrange v1; no cross-group moves
or hide/show; group order included; by-construction default; §6
skillpanel door kept; Java editor door slot 8 accepted as a live-data
filing gate; per-group reset included on both platforms; all players;
data-driven `pinned:` cap 4).
**Desk only** — no MANIFEST edits, no panel/server contact, no
GitHub/Drive publication were made by this build.

## Packet

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelMenu-3.0.0.jar` | 329,425 B | `306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51` |
| `ExcelMenu-3.0.0-sources.zip` (211 files) | 286,127 B | `792a2ec8c6bd54762ad4f40d592c14f43975f1a04fd3b0fa51f11382c5e382df` |

Packet folder: `~/workspace/excelmenu-30-build-20261113/packet/`
(jar + sources zip + `build.log` + this report + `SHA256SUMS`).
Plugin version: `plugin.yml` `version: 3.0.0`; `build.sh` `VERSION="3.0.0"`.

## Base gate (resumed, not redone)

Run 280's base gate stood and was confirmed on disk: the rebuilt
2.0.1 jar in `base-reproduction/` hashes to
`c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a`
(287,906 B), suite `OK (128 tests)`.
**Discrepancy noted:** `base-build.log` names the built jar path as
`~/workspace/excelmenu-30-build-20261113/ExcelMenu-2.0.1.jar`, but no
jar exists at the build root — the artifact on disk lives only at
`base-reproduction/ExcelMenu-2.0.1-rebuilt-from-sources-zip.jar`
(same hash). The root jar was evidently moved/renamed into
`base-reproduction/` after the log was written; the log text was not
updated. Hash and test evidence are unaffected.

## What was implemented (DESIGN §2–§5, Appendix A inventory)

- **Pins (§2):** `LayoutDef.pinned` + `LayoutLoader` `pinned:` key
  (list of non-blank ids, duplicates refused, cap 4 refused above,
  absent = empty). `FormModel.picker` renders pins first as
  `EntryTarget` buttons with FLAT-rule (group-colour) labels, then
  group buttons (groups with only pinned entries left are skipped),
  then the Rearrange button; `FormModel.group` excludes pinned
  entries (root-only, Q1); `FormModel.flat` renders pins first with
  no duplication. `ChestModel` untouched — pins are a Bedrock-picker
  feature by design. Reload-time cross-check in `RegistryMenus`
  logs one SEVERE per pinned id with no registry entry; the menu
  still serves.
- **Rearrange (§3):** pure core `PlayerMenuPrefs` (ids-only record),
  `OrderMerge` (stored ++ new-at-end; vanished ids drop; unknown
  groups ignored; idempotent), `PrefsCodec` (YAML round-trip;
  corrupt/wrong-type/version≠1 → no-prefs + error), `MenuEditorCore`
  (Up/Down/Top/Bottom + insert algebra, boundary no-ops with notes).
  Bukkit: `PrefsStore` (`playerdata/<uuid>.yml`, lazy load + cache,
  save-on-change via temp + atomic rename, corrupt → SEVERE once +
  default order + file left in place, cache dropped on quit),
  `EntryService.entriesFor(menu, viewer, prefs)` overload (resolver
  untouched; Java takes within-group order only, Bedrock also takes
  group order), `FormService` EditorTarget handling + prefs-aware
  picker/fresh-resolves, `RegistryMenus` prefs-aware opens/clicks.
  Editors: `MenuEditorForms` (Bedrock E1–E5, §2.5 token discipline,
  Back last on every form, per-move saves, E3 re-opens for chained
  moves, E5 global + per-section reset) and `MenuEditorMenus` +
  `MenuEditorHolder` (Java J1–J3, select-and-insert, per-move saves,
  J4 two-step reset on the item, Back on every screen, 36-entry
  group cap asserted loudly). Doors: picker chrome button
  (`EditorTarget`, no stats tap), `/rearrange` command
  (`RearrangeCommand`; collision-checked desk-side — no other
  plugin.yml in the workspace declares it), and the Java fixed item
  is a **live-data placement** (see "Not in the jar" below).
  Config knobs (§3.7): `rearrange.enabled` / `rearrange.menus` read
  with code defaults (on, `[server_menu]`) via `RegistryConfig`;
  `enabled: false` removes the button, refuses the command, and
  ignores (never deletes) stored prefs. Editor universe = registry
  entries of the group for the player's platform, guards/dates not
  applied, so guarded doors keep their ordered place (§3.5/§7 #18
  semantics; merge-level, see rig note below).
- **Skills hook delegation (§4):** `ScreenHooks` gains a tap-time
  `BooleanSupplier` wired in `ExcelMenu.onEnable` to
  `isPluginEnabled("ExcelSkillsPanel")`; when present the `skills`
  case returns `false` and the entry's own actions dispatch (single
  source of truth = entry data); when absent, `SkillsPerksScreen`
  opens exactly as in 2.0.1. `plugin.yml` softdepend gains
  `ExcelSkillsPanel`. Java path unchanged (already delegated via
  Item 5 data). `SkillsPerksScreen/Core` and the `skills_perks`
  definition remain as the fallback surface.
- **magiclevel retirement (§5, jar side):** `"magiclevel"` removed
  from `BUNDLED_ENTRIES` (comment block updated with the Phase 3
  retirement note) and `src/main/resources/entries/magiclevel.yml`
  deleted from the tree. Registry effect is a **live-data** outcome
  (94 → 93) at install; the bundled census goes 84 → 83 files.

## Tests — OK (163 tests)

`build.log` (packet): suite `OK (163 tests)` = 128 carried + 35 new.

- **Carried 128:** all green after three *enumerated intentional
  deltas*, each the magiclevel retirement (DESIGN §7 R1):
  `FormModelTest` flat sequence 28 → 27 buttons (magiclevel out),
  out-of-window 26 → 25; `ChestModelTest` Java slot map — the `me`
  row compacts one slot from spellbook on (32 total slots),
  out-of-window 31 → 30; `GoldenHarnessTest` acceptance **D10
  removed** — D10 existed solely to classify the magiclevel row;
  with the entry retired the harness's no-stale-acceptances rule
  requires its removal (annotated in the test's javadoc). Golden
  harness otherwise unchanged (D1–D9, D11 occurred; 5,526 rows),
  catalogue harness C1–C8 unchanged (56 rows).
- **New suites (35 tests):** `PinsModelTest` (8 — picker pin order/
  labels/targets, group exclusion + Back last, flat dedupe,
  guard-failed pin still targets its entry, empty-pins = 2.0.1
  shape, commands_menu scope, group-order override),
  `LayoutLoaderPinsTest` (6 — parse/order, absent = empty, duplicate
  refused, >4 refused, non-list refused, blank refused),
  `OrderMergeTest` (8 — §3.3 properties incl. Bedrock-only group
  order and idempotence), `PrefsCodecTest` (7 — round-trips full/
  partial/empty, corrupt, wrong types, version 99, unknown ids
  survive verbatim), `MenuEditorCoreTest` (6 — moves, boundary
  notes, unknown id, insert=self no-op, and the 500-trial property
  that insert-move ≡ the Up/Down step sequence).

## Determinism

- **Double-build:** two consecutive clean `build.sh` runs in the
  build folder produced byte-identical jars
  (`306607ba…` both times, `cmp` clean).
- **Clean-room:** (a) a fresh copy of the tree built in /tmp —
  byte-identical; (b) the **sources zip itself** unpacked to a
  fresh directory and built — byte-identical jar, `OK (163 tests)`.
  The sources zip is therefore a proven reproduction of the jar.
- **Jar diff vs the proven 2.0.1 jar:** exactly the §6.3 expected
  scope — added classes only for the named features (editor,
  prefs, OrderMerge, PrefsCodec, EditorTarget), `entries/magiclevel.yml`
  removed; no other resource or class additions/removals.

## NOT implemented / not proven desk-side (exact reasons)

1. **Desk rig behaviour matrix (DESIGN §7 R2/R3) — NOT RUN.**
   This commission was desk-build-only with no server contact; no
   Paper/Geyser rig was booted. The Bukkit-layer editors, the hook
   bypass, and the prefs plumbing are compile-proven and their pure
   cores are unit-proven, but boot/load (93/2/13 counts, SEVERE 0),
   the R2 18-case matrix (pin taps, panel present/absent, relog
   persistence, corrupt-prefs-in-game, kill-switch in-game), and
   the R3 mirror pass remain **filing/install-phase gates**, as do
   Mike's five player proofs (§7). Nothing in this report should be
   read as rig proof.
2. **§3.2 platform-rank STOP gate — NOT CLEARED desk-side.**
   The gate is defined over the filing-time live pull. Run
   mechanically over the *bundled seed* entries instead, the ranks
   DIVERGE in 4 of 5 groups — expected and not blocking: the bundled
   seeds are the pre-grouped FLAT data (§6.3: bundled data lags live
   by design; Appendix B's live grouped table is rank-unified).
   The build therefore does **not** claim the gate: the merge model
   stores one shared id list per group while each editor derives
   its *default* from its own platform's ranks, so a live divergence
   would surface as differing defaults, not corruption — but the
   R0 step-3 check over the G1 live pull is still owed at filing
   and remains a STOP gate there.
3. **Live-data placements are NOT in the jar (by design, §6.2/§6.3)
   and are NOT done:** live `layouts/server_menu.yml` gaining
   `pinned: [spawn, tpmenu]` + the page-2 Rearrange fixed item
   (slot 8, Q7 — with its filing-time `chest_tools` re-count),
   the `config.yml` `rearrange:` merge-edit, the `menus/skills_perks.yml`
   `magic:` item → Magic Level door (§4.3/§9 Q6), and the live
   `entries/magiclevel.yml` deletion with its `.bak` + post-restart
   resurrection check (§5.3). All are install-window data items for
   the supervisor's packet (G1 fresh pulls first); the jar carries
   engine support + code defaults only. Bundled data inside the jar
   is byte-identical to 2.0.1's except the single magiclevel delta,
   per §6.3.
4. **Java editor group header items (minor deviation):** J1/J3
   group items take material/name from the layout page's fixed
   items preceding the group's first slot (the bundled layout
   idiom) rather than a per-group header registry; on a layout
   whose headers don't precede their group slots the item falls
   back to PAPER + the group display name. Cosmetic only; no
   behaviour depends on it. J2's pinned-members line is one note
   item (slot 44) naming the pins, where §3.6 sketches display
   panes in a header line — same information, chest-shaped.

## Filing notes for the supervisor

- Gates G-a…G-e (§8) are untouched by this build and all still
  apply; G1 receipts must establish the live `skills_perks.yml`
  hash and exact live jar filename, and R0 step 3 (rank check)
  must be run over that pull before filing.
- `/rearrange` collision: desk grep over the workspace's plugin.yml
  files found no other declaration; the live plugin set check is
  still owed at filing per §3.4.

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
