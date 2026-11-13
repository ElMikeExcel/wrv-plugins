# ExcelMenu 3.0 — DETAILED DESIGN (Menu Programme Item 3)

**Commission:** board supervisor run 279 dispatch, Nov 13, 2026 (~1:15 AM CST).
Menu Programme Item 3 — the ExcelMenu 3.0 jar window. Desk only: this
document is the design; no build, no MANIFEST filing, no panel/server
contact was made for it.

**Feature set (DECISIONS.md, Mike, Nov 12, 2026):**
1. True one-tap Spawn/Teleport root pins (Q1 = A, Mike 21:35 CST).
2. Per-player menu rearrange — Mike's arrangement is the default every
   player starts from, with a reset back to it (Mike 21:36 CST).
3. Skills-panel hook delegation + retirement of the bundled Magic Level
   (`magiclevel`) entry (Mike 21:38 CST — the window is named 3.0
   because this set is a step change in what the menu can do).

**Design gate — CLEARED:** ExcelSkillsPanel 0.1.0 installed Nov 12, 2026,
23:48 CST; Mike's Bedrock panel acceptance done Nov 13, 2026, ~12:06 AM
CST ("it actually looks pretty nice"). The Java panel pass is still owed
as a player-class checklist line; it is **not** a gate on this design or
on the 3.0 build (see §8, gate G-b).

**Standing rules this design is bound by:**
- **Branch from 2.0.1.** The Back-button fix ships as ExcelMenu 2.0.1
  (Item 6); 3.0 branches from the verified 2.0.1 tree and must not
  regress it (§1, §7).
- **Every menu/submenu carries a Back button** (Mike, Nov 13, 2026) —
  including the rearrange editor itself, on both platforms (§3.5, §3.6).
- **No data-only fakes for engine behaviour.** The grouped DESIGN §3 and
  the Item 6 DESIGN §2 both rejected pseudo-entries / duplicate files as
  substitutes for renderer features. Pins, the editor entry point, and
  the hook delegation below are all engine-level for the same reason.

**Sources of record read in full for this design:**
- `~/workspace/menu-grouped-20261112/DECISIONS.md` (Q1=A; rearrange
  commissioned; window named 3.0; Q2 = FULL Magic header).
- `~/workspace/menu-grouped-20261112/DESIGN.md` (programme design of
  record; §3 pins gap, §6 the fold, §7 the panel, §8 the grouped census).
- `~/workspace/menu-items56-20261113/item6-back-button/DESIGN.md`
  (the 2.0.1 fix design; §6 carries the 3.0-collision warning).
- `~/workspace/excelmenu-201-build-20261113/BUILD-RECORD.md` (the
  base-drift finding, §1.1 below) + its `TEST-REPORT.md` and
  `INSTALL-PACKET.md`.
- Parent backlog, MENU PROGRAMME section (Items 1–8; Item 3 line;
  Item 5 landed state; Item 6 line + the run-278 continuation notes).
- The 2.0.1 source tree itself
  (`~/workspace/excelmenu-201-build-20261113/src/`) — every class named
  below was read at that tree, not from memory.

---

## 0. Verdicts up front

- **One jar, one window, four feature legs.** Pins, rearrange, hook
  delegation, and the magiclevel retirement ship together as
  ExcelMenu 3.0.0 in one gated jar window with a small data set riding
  it (§8). Nothing in the set needs a second window.
- **Pins are a layout key + two renderer touches.** A new closed-set
  layout key `pinned: [spawn, tpmenu]`; `FormModel.picker()` renders
  the pinned entries as `EntryTarget` buttons above the group buttons;
  `FormModel.group()` excludes pinned entries from their home group on
  Bedrock; `ChestModel` (Java) ignores `pinned` entirely — the two
  doors already sit at Travel slots 1–2 on Java (§2).
- **Rearrange is an override layer merged at resolve time, exactly
  where the grouped DESIGN said it would fit.** ExcelMenu already
  renders per-player (`EntryService.entriesFor(menuId, viewer)` →
  `EntryResolver.resolve(...)` per open, never cached). 3.0 adds one
  pure core step — `OrderMerge` — applied to the resolved list when the
  viewer has stored preferences, plus a per-player YAML store and an
  editor on both platforms (§3).
- **The default needs no snapshot file.** Mike's arrangement *is* the
  live data (entry `order` / `order-bedrock` weights + the layout
  `groups:` map + config group order — that is how the grouped packet
  encoded it). A player with no stored preferences sees the resolver's
  default order; Reset = delete the preferences. §3.2 gives the
  reasoning and the rejected alternative.
- **The skills "hook delegation" is a presence-gated bypass, and Item 5
  made it urgent, not moot.** Item 5 (landed Nov 13, 2026, 00:23 CST
  window) repointed the `skills` entry's *actions* to
  `[close]` + `[player] skillpanel`. On Java that works today. On
  Bedrock it does **not**: `FormService.answer()` asks
  `ScreenHooks.tryOpen(...)` *before* running an entry's actions, and
  the `skills` case still opens the old `SkillsPerksScreen` — so a
  Bedrock tap on Skills & Perks never reaches the new action. 3.0's
  hook change: when ExcelSkillsPanel is enabled, the `skills` hook
  returns `false` and the entry's own actions dispatch (single source
  of truth = the entry data); when it is absent, today's screen opens
  exactly as in 2.0.1 (§4).
- **The magiclevel retirement is jar + data in the same window, and is
  data-reversible.** Remove `magiclevel` from `BUNDLED_ENTRIES` and
  from the jar's `resources/entries/`, and delete the live entry file
  in the same window (kept as an in-place `.bak`). Reversal does not
  need a jar rollback: restoring the entry file revives the door on
  the 3.0 jar, because the registry loads files regardless of the
  bundled list — the list only drives first-run extraction (§5).
- **Registry 94 → 93, layouts 2, definitions 13 — all else unchanged.**
  No other entry file changes. `commands_menu` is untouched by every
  leg (pins key absent, editor scoped to `server_menu`, hook scoped to
  `server_menu` + `skills` — all pre-existing scopings) (§6).
- **No open question in §9 crosses a Mike hard line.** Every one is a
  judgment call inside commissioned bounds, each with a recommended
  default the supervisor can decide under DECIDE-AND-LOG and log.

### Feature → mechanism map

| Feature | Core (pure) | Bukkit layer | Data |
|---|---|---|---|
| Pins | `LayoutDef.pinned`, `LayoutLoader` key + validation, `FormModel.picker/group/flat` | none new — `FormService` already dispatches `EntryTarget` | `layouts/server_menu.yml`: `pinned: [spawn, tpmenu]` |
| Rearrange | `PlayerMenuPrefs` model, `OrderMerge`, `MenuEditorCore` (move ops) | `PrefsStore` (YAML per player), `MenuEditorForms` (Bedrock), `MenuEditorMenus` (Java chest), `/rearrange` command | new `playerdata/<uuid>.yml` at runtime; `config.yml` knobs; one fixed item on layout page 2 (Java editor door) |
| Skills hook | none | `ScreenHooks` presence gate; `ExcelMenu` wires a presence supplier | none (Item 5 already placed the data) |
| magiclevel retirement | `BundledEntriesTest` census update | `ExcelMenu.BUNDLED_ENTRIES` −1 | jar resources −1 file; live `entries/magiclevel.yml` deleted (`.bak` kept); `menus/skills_perks.yml` `magic:` item becomes the Magic Level door |

---

## 1. Base + branching plan (from the verified 2.0.1 tree)

### 1.1 The base-drift finding (binding)

The 2.0.1 BUILD-RECORD, Part 1, proved:

- The on-disk phase-2 working tree
  (`~/workspace/menu-2.0/phase-2/`) **does not reproduce** the live
  2.0.0 jar: built verbatim it produced 299,066 B / `22b12e11…` with
  9 of 127 tests failing. After the Nov 2, 2026 2.0.0 build, that tree
  was deliberately mutated by the Nov 7, 2026 org-decisions foldback
  (new entries, changed entries, `.bak-*` files under
  `src/main/resources/` that `build.sh` copies into the jar). It is
  **live-data lineage, not a build base, and must NOT be used as one.**
- The frozen snapshot **does** reproduce, exactly:
  `ExcelMenu-2.0.0-sources.zip`, SHA-256
  `f16f3e32e05bc89e2d35805b563378939f3fb5b01c066d3b66610f9e6061eda1` /
  260,275 B, unpacked clean and built with its own `build.sh` →
  jar `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2`
  / 286,829 B, **byte-identical to the live 2.0.0 jar**, suite
  `OK (127 tests)`.
- 2.0.1 was built on that proven snapshot. Its artifacts of record, in
  `~/workspace/excelmenu-201-build-20261113/`:
  - `ExcelMenu-2.0.1.jar` — 287,906 B / SHA-256
    `c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a`,
    built twice byte-identical, suite `OK (128 tests)`.
  - `ExcelMenu-2.0.1-sources.zip` — 257,218 B / SHA-256
    `620f7af0b4ad2c12e9e3cc0224a85bd173711b167d0ea306faaf4b572324d824`,
    197 files. **This zip is the 2.0.1 tree of record.**

### 1.2 Branch procedure for the 3.0 build (commission terms)

1. Create the 3.0 build folder (suggested
   `~/workspace/excelmenu-30-build-<date>/`). Unpack
   `ExcelMenu-2.0.1-sources.zip` into it. **Never** copy from
   `~/workspace/menu-2.0/phase-2/` and never from a live data folder.
2. **Base gate before any edit:** run the tree's own `build.sh`;
   require jar SHA-256 = `c6da29ba…` / 287,906 B and suite
   `OK (128 tests)`. Any mismatch = STOP; the base moved and the
   build does not proceed on an unproven base (the Item 6 lesson).
   Keep the rebuilt jar + log in a `base-reproduction/` subfolder,
   house pattern.
3. All 3.0 work happens in that folder. Version knobs: `plugin.yml`
   `version: 3.0.0`; `build.sh` `VERSION="3.0.0"`. Jar output
   `ExcelMenu-3.0.0.jar`; sources zip pinned-date house pattern.
4. Determinism: double-build byte-identical is a release requirement,
   as for 2.0.1; the jar diff vs the proven 2.0.1 jar is produced and
   reviewed (TEST-REPORT scope proof) — the diff must contain only
   the classes/resources this design names (§6.3).

### 1.3 Sequencing vs the 2.0.1 install

At design time the live jar is still 2.0.0 (`d1c6e771…` / 286,829 B);
ExcelMenu 2.0.1 is **PENDING in the MANIFEST** (filed Nov 13, 2026).
The 3.0 *build* branches from the 2.0.1 tree regardless of install
order. The 3.0 *install* gates on 2.0.1 having landed first (§8,
gate G-a) — the natural order, since 2.0.1 is small, filed, and
already in the failsafe lane. If scheduling ever inverted that, the
3.0 packet must explicitly **supersede** the 2.0.1 PENDING entry in
the MANIFEST (3.0 contains the Back fix by construction) rather than
leaving two ExcelMenu jar entries racing; the supervisor owns that
call at filing and records it in the packet.

### 1.4 What 3.0 inherits from 2.0.1 and must not regress

- `FormModel.Target` is a sealed trio after 2.0.1:
  `EntryTarget | GroupTarget | PickerTarget`.
- `FormModel.group()` appends exactly one
  `Button(BACK_LABEL, PickerTarget)` LAST; `BACK_LABEL` is
  `BedrockDetail.BACK_LABEL` (single wording source).
- `FormService.answer()` handles `PickerTarget` by re-resolving fresh
  (`entries.entriesFor(layout.id(), …)`) and re-opening the picker
  through `send()` (new token, group = null, generation bumped);
  Back records no stats tap.
- Suite baseline: 128 tests (127 carried + 1 new in 2.0.1), golden
  harness D1–D11, catalogue harness C1–C8.

3.0 extends `Target` (§2.3, §3.4) — it does not reshape it. Every
2.0.1 behaviour above is a named non-regression case in §7.

---

## 2. Pins architecture — true one-tap Spawn/Teleport (Q1 = A)

### 2.1 The gap, restated from the grouped DESIGN §3

`FormModel.picker()` emits only `GroupTarget` buttons; no
`pinned`/`root` key exists in the layout schema (closed `LAYOUT_KEYS`,
`LayoutLoader.java:32–34`) or the entry schema. Fixed items are chest
chrome (`FormModel` never reads pages/fixed for forms). Duplicate
entry files were rejected (two sources of truth for one door). The
Phase 1 data approximation (a `quick` group, "Spawn & Teleport" as
first picker button, 2 taps) shipped as the interim state; Q1 = A
retires it in favour of the engine feature designed here. Note the
live consequence of the interim state: today `spawn` and `tpmenu`
carry `group: travel` and render inside the Travel form on Bedrock;
there is no `quick` group in the landed grouped packet (the §8 delta
was not staged), so no data cleanup for a quick group is owed.

### 2.2 Schema: one new layout key

`layouts/server_menu.yml` gains one top-level key:

```yaml
pinned: [spawn, tpmenu]
```

- `LayoutLoader`: `LAYOUT_KEYS` gains `"pinned"`. Value must be a
  list of non-blank strings; duplicates are a load error (loud, house
  posture — a layout that fails is refused while other menus serve).
  Absent key = empty list (back-compat: `commands_menu` and any FLAT
  layout are unchanged without edits).
- `LayoutDef`: new component `List<String> pinned` (defensive copy,
  declaration order = render order). All `LayoutDef` constructions in
  tests/fixtures gain the component; `RenderFixtures` and
  `FormModelTest`'s grouped-variant construction are the known sites.
- Cap: at most **4** pins per layout; a longer list is a load error.
  (Two are commissioned; the cap keeps the picker a picker. See §9,
  Q10.)
- The loader is pure and registry-blind by design, so *existence* of
  a pinned id is **not** a loader check. The render layer (§2.3)
  intersects `pinned` with the viewer's resolved entries; an id that
  resolves for nobody is caught by a reload-time cross-check:
  `RegistryMenus.reloadLayouts` (or `EntryService` after both loads)
  logs one SEVERE per pinned id with no registry entry, and the menu
  still serves (founding rule: a dead pin must never kill the menu;
  a dead *menu* must never be silent — the SEVERE is the loudness).

### 2.3 Rendering

**Bedrock — GROUPED picker (`FormModel.picker`).** New signature:
`picker(layout, resolved)` already has everything it needs
(`resolved` is the viewer's full resolved list). Composition order:

1. **Pin buttons** — for each id in `layout.pinned()` order, if a
   resolved entry with that id exists, one
   `Button(bedrockButtonLabel-equivalent, EntryTarget(id))`.
   Label rule: group-form buttons use plain stripped labels; the
   picker is the compass root, whose FLAT ancestor coloured buttons
   by group. Pins keep their identity by using the FLAT rule —
   `EntryResolver.bedrockButtonLabel(r, config)` (group colour +
   stripped label) — so Spawn/Teleport read exactly as they did on
   the pre-grouped flat form. `picker()` therefore also needs the
   `RegistryConfig` (for group colours); signature becomes
   `picker(layout, resolved, config)`, mirroring `flat(...)`.
2. **Group buttons** — as today, one per non-empty group, in layout
   order — **except** the per-player group-order preference (§3.3)
   applies here when present.
3. **Rearrange button** — the editor's chrome entry (§3.4), appended
   LAST. The picker is the root: it carries **no Back button**
   (2.0.1 rule, unchanged).

Pin taps need **no new dispatch code**: `FormService.answer()`
already handles `EntryTarget` — stats tap recorded, screen hook
consulted, actions dispatched through the session path. The Spawn
pin therefore runs spawn's real action (the teleport-confirmation
gate flow) and the Teleport pin runs `[close]` + `[player] tpmenu`
(the TeleportScreen picker) — identical behaviour to tapping them
inside Travel today, one tap sooner. Guard/date resolution is
inherited for free: a pinned entry whose guard fails arrives in
`resolved` carrying the standard unavailable action (Amendment A),
and a date-windowed pin simply isn't in `resolved` out of season.

**Bedrock — GROUPED group form (`FormModel.group`).** The home group
of a pinned entry **excludes** pinned entries: root-only on Bedrock
(the faithful read of Mike's words; grouped DESIGN §3 names
root-only as the faithful option — §9, Q1 carries the decision with
this as the recommended default). Travel's form after 3.0 lists
Party, Guild, EliteMobs, then Back (2.0.1, still last). The form's
body list follows the same exclusion (body rows are built in the
same loop). Non-home groups are untouched.

**Bedrock — FLAT (`FormModel.flat`).** No live layout is FLAT after
the grouped landing, but the renderer must stay coherent: pins
render first (in `pinned` order), then every *remaining* resolved
entry in bedrock order — pinned entries are **not duplicated**.
This is a dedupe, three lines, and it keeps a future flat revert
honest.

**Java — chest (`ChestModel`).** **Unchanged, by design.**
`ChestModel.build` never reads `pinned`; spawn/tpmenu fill Travel
slots 1–2 from the layout slot list exactly as today. Java's menu is
a single grid where the two doors already sit top-left under the
Travel pane — the pin concept (rescue from a two-step picker) has
no Java referent. The design states this plainly rather than
inventing a Java pin visual: **pins are a Bedrock-picker feature;
Java's arrangement is already one-tap.** (The per-player rearrange,
§3, *does* affect Java — within-group fill order — because that is a
different feature with a real Java referent.)

### 2.4 Interaction with the section picker and rearrange

- Pins sit **above** the grouped sections and are **not** part of
  any group: they do not appear in the rearrange editor's group
  lists (§3.5 shows them as locked header lines in the Travel editor
  so their absence from Travel is explained, not mysterious), they
  cannot be reordered or moved by players, and the group-order
  preference never reorders them.
- Picker with zero resolvable pins (e.g. both guards failing) renders
  exactly the 2.0.1 picker plus the Rearrange button — no empty gap,
  no placeholder button.
- `commands_menu` has no `pinned` key: its picker is byte-for-byte
  the 2.0.1 picker (no pins, and no Rearrange button — the editor is
  scoped to `server_menu`, §3.1; `FormModel.picker` adds the
  Rearrange button only when `layout.id()` is rearrange-enabled per
  config, §3.7 — not merely whenever the layout is grouped).

### 2.5 Tests owed for pins (detail in §7)

`FormModelTest`: picker with pins (order pins → groups; labels carry
group colour; pin targets are `EntryTarget` with the right ids);
group form excludes pins; flat dedupe; pins with a guard-failed
entry (unavailable action still targets the entry); empty-pins
picker identical to 2.0.1 shape. `LayoutLoaderTest`: `pinned`
parses; duplicate pin refused; >4 refused; unknown key still
refused. `BundledEntriesTest`/fixtures: the bundled `server_menu`
layout resource is **not** changed (§6.3), so fixture expectations
for it stand; the grouped shape is exercised via constructed
`LayoutDef`s, the same pattern 2.0.1's Back test used.

---

## 3. Per-player rearrange — data model, editor, reset

### 3.1 Scope (v1, decided here; §9 Q2/Q3 carry the alternatives)

- **Menu in scope: `server_menu` only** — the compass menu players
  live in. `commands_menu` is a reference catalogue; personalising it
  is not commissioned and its 9-group / 56-entry surface would double
  the editor's proof burden for no player ask.
- **What a player can change:** (a) the order of entries **within
  each group** of `server_menu`; (b) the **picker order of the
  groups** themselves (Bedrock-visible; §3.3 states the Java truth).
- **What a player cannot change in v1:** moving an entry to a
  different group (groups are semantic sections and own fixed Java
  slot lists — a cross-group move can overflow another group's slots
  and trip ChestModel's loud refusal for that player alone; also the
  editor would have to teach group semantics it has no need of);
  hiding/showing entries (not commissioned; guards and date windows
  already own visibility); pins (§2.4); entry labels/icons/actions
  (data is the server's, not the player's).
- **Who:** every player, no permission (family server; DECISIONS
  frames it as a player feature). A config kill-switch exists (§3.7)
  so the supervisor can disable the feature without a rebuild.

### 3.2 The default = Mike's arrangement, by construction

Mike's arrangement exists today as data, in three places that
together are "the arrangement":

1. each entry's `order` (Java within-group weight) and
   `order-bedrock` (Bedrock weight) in `entries/*.yml`;
2. the layout `groups:` map declaration order in
   `layouts/server_menu.yml` (picker order);
3. `config.yml`'s `groups.server_menu` list (Java packing group
   sequence).

The grouped packet's desk verification (§8 of the grouped DESIGN)
proved the within-group sequences encoded there *are* Mike's order
on both platforms (Appendix B reproduces the table, updated for
Items 5/3). Therefore:

- **A player with no stored preferences gets exactly the resolver's
  default order** — Mike's arrangement, with no snapshot, no copy,
  and nothing to drift.
- **Reset = delete the player's preference record** (§3.6). The next
  open renders the default.
- When Mike later re-arranges the *server* default (a data edit, the
  way the grouped packet itself was made), players who never
  customised follow automatically; customised players keep theirs,
  merged by §3.3's rules. That is the correct semantics for "Mike's
  arrangement is the default": a default, not a freeze.

**Rejected alternative — a frozen snapshot file** (e.g. a
`default-order.yml` shipped in the jar capturing Nov 13's order):
it creates a second source of truth for one fact, the exact defect
the programme has rejected twice (duplicate pin entries, grouped
DESIGN §3; fake Back entries, Item 6 DESIGN §2). The first data edit
to any entry's weights would silently desync it. Not built.

**One assumption, made a build-phase STOP gate:** a single stored
id-list per group (§3.3) can serve as both platforms' order only if
the default **rank order** within each group is the same on Java
(`order`) and Bedrock (`order-bedrock`) today. The grouped packet
was built to make that true, and its verification says it is — but
the 3.0 build's desk phase must re-verify it mechanically over the
filing-time live pull (all six `server_menu` groups, rank-compare
the two weightings; §7 step R0). **If any group diverges, STOP and
escalate to the supervisor** — the merge model needs a decision
(two lists per group vs one platform winning), and the build does
not silently pick one.

### 3.3 Data model + merge (pure core)

**Model — `core/registry/PlayerMenuPrefs` (new, pure):**

```text
PlayerMenuPrefs
  version: 1                       (schema version, §3.8)
  menu: server_menu                (the only menu in v1; field exists
                                    so a second menu is data, not surgery)
  groupOrder: [me, travel, ...]    (optional; picker order override —
                                    group keys, may be partial/absent)
  groups:                          (optional per group; absent group =
    travel: [party, guild, ...]      default order for that group)
    me:     [summons, skills, ...] (entry ids in the player's order)
```

One ordered id-list per touched group — **ids only**, never weights,
labels, or copies of entry data. The store is deliberately small:
a fully-customised record is six short lists plus one group list.

**Merge — `core/registry/OrderMerge` (new, pure, unit-tested):**
applied in `EntryService.entriesFor(...)`'s new overload (below) to
the resolver's output for a viewer with prefs:

```text
merge(resolved, prefs):
  for each group g present in resolved:
    stored = prefs.groups[g]            (may be absent)
    if stored absent: group order = resolver order (default)
    else:
      known   = stored ids that exist in resolved's g, in stored order
      newOnes = resolved's g entries whose ids are not in stored,
                in resolver (default) order
      group order = known ++ newOnes
  group sequence (for picker + Java comparator):
    if prefs.groupOrder present:
      known groups in stored order, then unlisted groups in
      layout/config order
    else: default sequence
```

Merge properties the tests pin (§7): vanished ids drop silently
(the file is *not* rewritten on merge — the stale id costs nothing
and disappears at the player's next editor save, which writes the
merged list); new entries append at their group's end for customised
players and land in default position for everyone else; a prefs
record naming an unknown *group* is ignored for rendering and
dropped at next save; merge is idempotent and never reorders a
group the player didn't touch.

**Where it plugs in.** `EntryResolver.resolve(...)` stays pure and
unchanged. `EntryService` gains:

```java
entriesFor(menuId, viewer, prefs)   // prefs may be null
```

which resolves as today, then — when `prefs != null` and the menu
is rearrange-enabled — re-sorts the resolved list with
`OrderMerge`'s comparator:

- **Bedrock:** the flat bedrock sort interleaves groups, but the
  GROUPED renderers consume per-group slices (`group()` filters by
  group in list order; `picker()` walks `orderedGroups`), so a
  **stable re-sort by (effective group sequence, position within
  the player's group list)** reproduces exactly the player's
  within-group order in every group form and their group order in
  the picker. FLAT Bedrock rendering ignores prefs (documented in
  §3.7 — no live layout is FLAT; if an admin reverts one, prefs
  pause, they are not lost).
- **Java:** the resolver's Java comparator already sorts by (config
  group sequence, `order`, id) and `ChestModel` fills each group's
  slot list in resolved order — so the same re-sort (group sequence
  from config, *not* the player's picker preference; within-group
  position from prefs) changes Java fill order and nothing else.
  The player's **group-order preference has no Java visual effect**
  (groups own fixed pages/slots) — stated here so no one "fixes" it
  later: Java shows all sections at once; there is no sequence to
  personalise.

**Call sites** that must pass prefs: `RegistryMenus.openMenu /
openChest` and `FormService`'s fresh re-resolves (`answer()`
PickerTarget path + session `refresh()`), plus the editor's own
opens. `BukkitViewerContext` is *not* extended — prefs are passed
alongside, keeping the resolver's viewer contract untouched.
Console/`/excelmenu entries` resolution stays default-order (it is
an admin view of the server's menu, not a player's).

### 3.4 Editor entry points

DECISIONS: "an extra button lets each player rearrange the menu."
Three doors, one editor, in priority order:

1. **Bedrock picker chrome button (primary).** `FormModel.picker()`
   appends `Button("Rearrange Menu", new Target.EditorTarget())`
   LAST, only for rearrange-enabled layouts (§2.4). `Target` gains
   its fourth member, `record EditorTarget() implements Target {}`.
   `FormService.answer()` handles it by handing the player to the
   editor service (fresh re-resolve, editor root form). Chrome, not
   a door: no entry id, no stats tap — the Back-button precedent.
2. **`/rearrange` command (universal).** New player-only command in
   `plugin.yml` (`rearrange`, usage `/rearrange`), executor routes
   Bedrock players to the forms editor and Java players to the chest
   editor. Registered in `ExcelMenu.onEnable` alongside the /menu
   family. Build-phase collision check owed (the SkillsPanel pattern:
   grep the live plugin set's commands; `skillpanel` was checked the
   same way). Any player who can open the compass can find the
   editor without hunting through sections.
3. **Java fixed item (discoverability).** `layouts/server_menu.yml`
   page 2 gains one fixed item at **slot 8** (end of the
   Chest Organization row). Page 2's `chest_tools` slots are
   `[1..8]` and only three entries resolve today, so the slot is
   free in practice. One precision the builder must hold:
   `ChestModel` paints fixed items into the slot map first, then
   fills group slots by cursor — the fixed item does **not**
   reserve slot 8 against the cursor, so the collision
   materialises only if an 8th `chest_tools` entry ever resolves
   (3 today, 5 slots of headroom). §8's filing gate re-counts the
   group, and §9 Q7 names the fallback (page-2 slot 17) if it has
   grown. Material `COMPARATOR`, name `&b&lRearrange Menu`, lore
   naming the `/rearrange` command,
   `action: ['[player] rearrange']` — the fixed-item action
   vocabulary already includes `player`
   (`LayoutLoader.ACTION_TAGS`), and `RegistryMenus`' click path
   already dispatches fixed-item actions like an entry's. No code
   change is needed for this door beyond the command existing.

**Rejected:** a registry *entry* for Rearrange (e.g. in Settings).
Settings' Java slot list `[46, 47, 49, 50, 51, 52]` is exactly full
(6 entries, 6 slots — the layout comment records that a seventh
fails LOUDLY by design). Adding the door there means slot surgery
on the most capacity-stressed row of the menu to hold navigation
chrome — the Item 6 reasoning applies: chrome belongs in the
renderer/layout, not the registry census. (It would also inflate
the census 93 → 94 post-retirement and record editor opens as door
stats.)

### 3.5 Bedrock editor UI (forms; every form carries Back)

Implemented as `bukkit/MenuEditorForms` over the same
`PlatformBridge`, under the **same §2.5 session discipline** as
`FormService` (single-use tokens bound to player + world + TTL;
closed = cancelled; stale answers dropped). The pure step logic
(what a move does to a list) lives in `core/MenuEditorCore` so the
move algebra is unit-tested without Bukkit. **Saves happen per
move** (§3.8) — closing any editor form loses nothing.

- **E1 — Editor root.** Title `Rearrange Menu`. Body: one line of
  explanation + the player's state (`You're using your own order.`
  / `You're using the default order.`). Buttons, in effective
  picker order: one per group (`Travel`, `Me`, … — display names
  from the layout `groups:` map), then `Section Order`, then
  `Reset to Default`, then **`Back`** LAST → returns to the
  `server_menu` picker (fresh re-resolve, the PickerTarget path).
  Standing rule: satisfied (Back present; E1's parent is the
  picker).
- **E2 — Group editor** (one group). Title `Rearrange — <Group>`.
  Body: the group's entries as a numbered list in current effective
  order. If the group has pinned members (Travel), the body heads
  with `Pinned at the top of the menu: Spawn, Teleport` and those
  ids are **not** buttons. Buttons: one per movable entry, label =
  plain stripped entry label, in current order; then
  `Reset This Section` (only when this group has a stored order —
  opens the E5 confirm shape scoped to this one group, §9 Q8);
  **`Back`** LAST → E1.
- **E3 — Move form** (one entry). Title = entry label. Body:
  `<Label> is #N of M in <Group>.` Buttons: `Move Up`, `Move Down`,
  `Move to Top`, `Move to Bottom`, **`Back`** LAST → E2. A move
  applies + saves, then re-opens **E3** with the updated position
  (so repeated Move Up taps chain without re-walking the list).
  At a boundary the button still renders (forms cannot disable
  buttons) and the move is a no-op that re-opens E3 with the body
  note `Already at the top.` / `Already at the bottom.` — honest,
  no fake affordance.
- **E4 — Section order.** E2/E3's shape over the group list: root
  list of groups in effective picker order (tap a group → move
  form with Up/Down/Top/Bottom/**Back**). Applies to the picker
  preference only; the body says so (`This changes the order of
  sections on the section picker.`).
- **E5 — Reset confirm.** From E1's `Reset to Default` (global) or
  E2's `Reset This Section` (scoped to one group; the body names
  the group and says only that section's order is removed). Global
  body: `This removes your own order everywhere and goes back to
  the default menu order. Your other settings don't change.`
  Buttons: `Reset to Default` (executes §3.6), **`Back`** LAST →
  E1 (or E2 for the scoped variant).
  Reset is only offered when prefs exist; a default-state player
  reaching E1 sees no Reset button (nothing to reset — E1's state
  line already told them).

Editor forms never render an entry the player cannot see: the
editor's lists are built from the same per-player resolved set the
menu renders (guards/date windows applied), so a guarded entry the
player currently fails appears in its unavailable form in the menu
but **is still a member of its group for ordering** — precision:
`OrderMerge` operates on ids, and the editor edits the *stored +
default id universe* (all registry entries of the group), not the
momentary resolved slice; otherwise a player could never order a
door that is temporarily unavailable, and a merge against the
resolved slice alone would drop stored positions for guarded
entries. Implementation note for the builder: the editor's group
universe = registry entries for (`server_menu`, group) filtered by
the player's *platform* only (not guards/dates); rendering merge
(§3.3) intersects with the resolved slice as specified. The rig
plan (§7) covers ordering a guard-failed entry.

### 3.6 Java editor UI (chest; Back on every screen)

Implemented as `bukkit/MenuEditorMenus` with its own holder
(`MenuEditorHolder`: player, screen kind, group, selected id,
generation — the `RegistryMenuHolder` pattern). **Every click is
cancelled; no item ever leaves an editor inventory** (the
`RegistryMenus.ClickListener` discipline: cancel, re-resolve,
dispatch by holder state).

- **J1 — Editor root** (27 slots). Six group items using the layout
  pages' own header materials/names (Travel pane material, Me pane,
  … — read from the layout's fixed items, not re-typed), a
  `Section Order` item, a `Reset to Default` item (shown in its
  armed state only after a first click, §J4), and a **`Back`**
  item (ARROW, `Back to Server Menu`) that re-opens `server_menu`
  page 1 through `RegistryMenus`. Standing rule: satisfied.
- **J2 — Group editor** (54 slots). The group's entries in current
  effective order fill slots 0… in order, with their real icons,
  labels, and lore plus one appended lore line: `Click to select`
  (or, when another entry is selected, `Click to move <Selected>
  here`). Selecting marks the entry (lore line swaps to `Selected —
  click where it should go`; enchantment glint optional, builder's
  choice, must not depend on it for meaning). Clicking a target
  entry **inserts** the selected entry at that position (move, not
  swap — insertion matches the Bedrock Up/Down algebra, so the two
  editors cannot produce divergent orders from the same gestures).
  Clicking the selected entry again deselects. Control row
  (slots 45–53): `Move to Top`, `Move to Bottom`, `Reset This
  Section` (§9 Q8 — recommended include), **`Back`** (→ J1) LAST
  in the row. Pinned members of Travel render as display panes in
  a header line above the fill (slots are plentiful: the largest
  group, Me, has 13 entries post-retirement; the design cap is 36
  movable entries per group before pagination is needed — far
  above any live group; the builder asserts the cap loudly rather
  than truncating).
  Saves happen per completed move, as on Bedrock; closing the
  inventory at any point loses nothing.
- **J3 — Section order** (27 slots). The six group items in
  effective picker order with the J2 select-and-insert mechanic;
  **`Back`** → J1. (Java visual no-op for the actual menu, §3.3 —
  J3's body/lore says the order applies to the section picker,
  so a Java player editing it for their Bedrock play — Mike's own
  cross-platform case — understands what they're changing.)
- **J4 — Reset.** J1's Reset item is two-step (`core/JavaConfirm`
  pattern, the definition engine's existing confirm idiom): first
  click renames it in place to `Click again to confirm reset` with
  warning lore; second click executes §3.6-reset; any other click
  disarms. No separate confirm inventory — one screen fewer to
  carry a Back button on, and the idiom is already house-proven.

**Reset semantics (both platforms):** delete the player's stored
record (file removed, cache entry dropped), send one chat line
(`Your menu is back to the default order.`), and land the player
back at the freshly-resolved default menu (Bedrock: picker;
Java: `server_menu` page 1). Reset touches nothing else — not
toggles, not ExcelAutoSort state, not any other plugin's data;
ExcelMenu owns no other per-player state to confuse it with.

### 3.7 Config knobs (adjustability law)

`config.yml` gains (read with code defaults, so an un-merged live
config still behaves — the data placement in §8 is documentation,
not a hard dependency):

```yaml
rearrange:
  enabled: true        # master switch for the editor + prefs merge
  menus: [server_menu] # layouts the editor serves (v1: one)
```

`enabled: false` ⇒ no picker Rearrange button, `/rearrange` replies
that rearranging is off, stored prefs are ignored (not deleted) at
render, and the menu is byte-for-byte the 2.0.1 menu plus pins.
`FormModel.picker` learns "rearrange-enabled" as a parameter from
the caller (`FormService` reads config), keeping the core pure.

### 3.8 Storage (`PrefsStore`)

- **Location/shape:** `plugins/ExcelMenu/playerdata/<uuid>.yml`,
  one file per player (family scale; per-file writes can't corrupt
  a neighbour). SnakeYAML through the house loaders' posture.
  Fields: `version: 1`, `menu: server_menu`, `group-order: [...]`,
  `groups: { <group>: [<entry id>, …], … }`. Written only for
  groups the player actually reordered (plus `group-order` only
  when touched) — absence is meaningful (§3.3).
- **Lifecycle:** lazy-load on first need (menu open for a player
  with a file, editor open), cached in a `ConcurrentHashMap<UUID,
  PlayerMenuPrefs>`; **save-on-change** (every editor move/reset
  writes immediately: temp file + atomic rename in the same folder,
  the crash-safe pattern); cache dropped on quit; a `version` other
  than 1, an unparseable file, or wrong types ⇒ log SEVERE once,
  treat as **no prefs** (default order), never fail a menu open —
  and the bad file is left in place for diagnosis (the editor's
  next save overwrites it via the temp+rename path).
- **Threading:** all reads/writes on the main thread (menu opens
  and clicks already are); no async IO in v1 — at five players the
  file is ~1 KB and the write is noise. Stated so nobody
  "optimises" it into an async race later.
- **Reload interplay:** `/excelmenu reload` does not touch the
  cache (prefs are player data, not registry data). Resolution is
  per-open, so a `/excelmenu reload` that adds an entry is picked
  up by §3.3's merge on the very next open with no prefs reload.

---

## 4. Skills-panel hook delegation

### 4.1 What the hook is today (2.0.1 tree, read verbatim)

- `bukkit/ScreenHooks.java` implements `FormService.ScreenHook`. Its
  javadoc scopes it exactly: *"Java chest taps never come here (the
  hook lives in the form service)"* — it is a **Bedrock-form-layer**
  interception. `tryOpen(player, menuId, entry)` fires only for
  `server_menu`, switching on entry id: `ask` → `AskScreen`,
  `skills` → `SkillsPerksScreen`, `makersmark_settings` →
  `GuardedRuns.runMark`. A hook that fires **always handles the
  tap** — the entry's actions must not also run.
- `bukkit/FormService.answer()` (EntryTarget path): records the
  stats tap, then `if (screenHook != null && screenHook.tryOpen(...))
  return;` — **before** `dispatcher.runAll(...)`. The hook therefore
  *pre-empts* entry data on Bedrock.
- `bukkit/SkillsPerksScreen.java` + `core/screens/SkillsPerksCore.java`:
  the ported ExcelBedrockMenu form (buttons `My mcMMO stats` /
  `Skill Trees on the Website` / `Close`, live seal count in the
  Magic line via `GuildGateReads`); if the form send fails, the same
  copy goes to chat. The `skills_perks` definition stays the Java
  surface and the form's fallback (ScreenHooks javadoc).
- **ExcelSkillsPanel** (installed 0.1.0, Nov 12, 2026, 23:48 CST) is
  a standalone plugin registering **`/skillpanel`** — a Java chest +
  Bedrock Cumulus panel over one `PanelModel` (Power Level + FULL
  Magic header block + 19 per-skill rows with bars; BUILD-RECORD at
  `~/workspace/skillspanel-build-20261112/`). Item 7 (0.1.1 cleanup)
  is PENDING in the MANIFEST at design time; the hook design below
  depends only on the plugin's *presence*, never its version.

### 4.2 The post-Item-5 divergence this leg fixes

Item 5 repointed the `skills` **entry data**: live
`entries/skills.yml` (placed Nov 13, 2026; 438 B /
`ccc527640bbc3f142be9fd2a5fc6cc85b1ebae4a2181a218bc8b77d3915743f1`)
now carries actions `[close]` + `[player] skillpanel` and
`requires-plugin: ExcelSkillsPanel`.

- **Java:** chest click → `RegistryMenus` click path → dispatcher
  runs the actions → the panel opens. **Delegation already works.**
- **Bedrock:** form tap → `FormService.answer()` → stats tap →
  `tryOpen` fires the `skills` case → `SkillsPerksScreen` opens;
  the entry's actions never run. **Bedrock still lands on the old
  screen.** This is not a hypothetical: it is the current live
  behaviour of the landed Item 5 packet on Bedrock, and closing it
  is precisely the "ExcelMenu's Phase 3 touch" the grouped DESIGN
  §7.3/§7.4 scoped ("the Bedrock `skills` hook delegates to the
  panel command when the panel plugin is enabled, falling back to
  SkillsPerksScreen + the definition exactly as today when it is
  not").

### 4.3 The delegation design (presence-gated bypass)

**One case changes.** In `ScreenHooks.tryOpen`, the `skills` case
becomes:

```java
case "skills" -> {
    if (skillsPanelPresent.getAsBoolean()) {
        return false;   // delegate: let the entry's actions run
    }
    skillsPerks.open(player);
    return true;
}
```

- `skillsPanelPresent` is a `BooleanSupplier` wired in
  `ExcelMenu.onEnable` as
  `() -> Bukkit.getPluginManager().isPluginEnabled("ExcelSkillsPanel")`,
  evaluated **at tap time** (one map lookup; the panel being
  disabled later must restore the fallback without a restart of
  ExcelMenu). `ScreenHooks`' constructor gains the supplier;
  construction site is the existing
  `formService.setScreenHook(new ScreenHooks(...))` call.
- **Why bypass, not dispatch:** the alternative — the hook itself
  runs `player.performCommand("skillpanel")` and returns `true` —
  hard-codes the destination in code a second time. The bypass
  keeps **one source of truth**: the entry data. If the door is
  ever repointed again (a data edit, like Item 5 was), Bedrock
  follows automatically; if the entry is reverted to
  `[openguimenu] skills_perks`, Bedrock opens the definition —
  coherent in both directions with zero code change. It also
  composes with Amendment A for free: when the panel is absent, the
  resolver has already swapped the entry's action to the standard
  unavailable message, but the hook fires first and serves the
  *better* fallback (the real screen) — which is today's behaviour,
  preserved bit-for-bit.
- **Java: no change.** There is no chest-path hook to delegate;
  §4.2's Java flow is already the target behaviour. The design
  names this so the build doesn't invent a Java hook.
- **`plugin.yml`:** `softdepend` gains `ExcelSkillsPanel` (load-order
  courtesy when present; harmless when absent — the tap-time check
  is the real gate, not load order).
- **What stays:** `SkillsPerksScreen`, `SkillsPerksCore`, and the
  `skills_perks` definition all remain in the jar and on disk —
  the fallback surface for at least one release (grouped DESIGN
  §7.3). Removing them is a future item, explicitly out of scope.
  The `ask` and `makersmark_settings` hook cases are untouched.
- **The one data edit in this leg** rides §5's window (grouped
  DESIGN §6/§7.4): the `skills_perks` definition's display-only
  `magic:` item (slot 13, `Magic (Sealed)`, stale `0 of 5` lore)
  becomes the Magic Level door — name `&d&lMagic Level`, lore
  *path/seals decide what you can cast; Magic Level decides how
  strong*, clicks left: `[player] skillpanel` + `[close]`. §9 Q6
  flags the degraded-fallback caveat with a recommended default.

### 4.4 Failure honesty

If ExcelSkillsPanel is enabled but `/skillpanel` somehow fails for
a Bedrock player (command unregistered by a broken panel build),
the tap closes the form and the command's own error reaches the
player — the same failure shape as any `[player]` door, and the
player still has `/magiclevel` typed and `/dm open skills_perks`.
The rig (§7) verifies the real dispatch against the live-pulled
panel jar; no silent-swallow is added here because none exists in
the dispatch path to remove.

---

## 5. Magic Level (`magiclevel`) entry retirement

### 5.1 What is removed

| # | Artifact | Change |
|---|---|---|
| 1 | `ExcelMenu.BUNDLED_ENTRIES` (`bukkit/ExcelMenu.java`) | `"magiclevel"` removed from the array; the Nov 2 comment block above the list is updated to record the Phase 3 retirement (the comment already narrates this list's history — keep the habit). |
| 2 | Jar resources `entries/magiclevel.yml` | File deleted from `src/main/resources/entries/` — the jar no longer bundles the door. `BundledEntriesTest` pins list ↔ resources; its expectations update (bundled census −1). |
| 3 | Live data `plugins/ExcelMenu/entries/magiclevel.yml` | **Deleted in the install window**, kept in place as `magiclevel.yml.bak-<window date>-pre30retire` (the Item 5 packet's in-place-net pattern), plus the window backup covers it. Deletion rides the same window as the jar (§5.3). |

Nothing else named magiclevel is touched: **ExcelMagicLevel the
plugin is not modified**; its typed **`/magiclevel` command is not
retired** — it stays the chat surface and becomes the panel's text
fallback (grouped DESIGN §6/§7.4), exactly the status the skills
definition/screen pair has today.

### 5.2 What replaces its function

The door's job was "show me my Magic Level." After 3.0 that job has
three graphical homes and one text fallback:

1. **The Skills panel's Magic header block** (ExcelSkillsPanel —
   Magic Level, XP bar to next, total XP, boosts, casts, seals
   line; FULL header per Q2) — reached by the **Skills & Perks**
   door, which Item 5 already repointed on Java and §4 delegates
   on Bedrock.
2. **The `skills_perks` definition's `magic:` item**, converted to
   the named Magic Level door into the panel (§4.3) — so the
   fallback surface keeps a labelled Magic door, per grouped
   DESIGN §6.
3. **`/skillpanel` typed** — the panel is directly command-reachable
   on both platforms.
4. **`/magiclevel` typed** — unchanged chat fallback.

Census effects: registry **94 → 93**; `me` group **14 → 13**
(magiclevel's bedrock weight 35 simply leaves the band — weights
are sparse by design, no renumber, grouped DESIGN §6/§8). Java:
`me` fills 13 of its 16 slots; no layout change, no overflow.
Bedrock Me form loses one button; Back still last (§7
non-regression).

### 5.3 The extraction trap — why jar and deletion are one window

`ExcelMenu.onEnable` calls `extractBundled("entries",
BUNDLED_ENTRIES)` (fill-missing: any listed file absent on disk is
re-extracted from the jar). Deleting the live file while any
pre-3.0 jar is live **resurrects the door at the next restart**.
Hence the packet order (§8): jar swap and data deletion land in the
**same window**, and the boot verification includes the
post-restart proof — registry diff shows `−1 removed (magiclevel)`,
**and `entries/magiclevel.yml` is still absent from disk after the
restart** (the resurrection check). A reload-only application is
**not** acceptable for this leg: only a restart with the 3.0 jar
proves extraction stays quiet.

### 5.4 Reversibility

Fully reversible, in two grades:

- **Door-level (no jar rollback):** restore
  `magiclevel.yml.bak-…-pre30retire` to `entries/magiclevel.yml`
  and `/excelmenu reload`. The registry loads entry *files*
  regardless of the bundled list (the list only drives extraction),
  so the door returns even on the 3.0 jar — Me back to 14, both
  platforms, no restart strictly required (reload suffices; the
  file then also survives restarts because it exists on disk —
  fill-missing never deletes).
- **Full rollback:** the window's `.bak` data set + the 2.0.1 jar
  (§8) restores the exact pre-3.0 state, bundled extraction and
  all.

Surfaces consequence (packet §8 names them as same-pass work):
Guide `/ask` "magic level" answers re-point from the compass door
to Skills & Perks → the panel's Magic block; a What's New line on
**both** surfaces (site section + in-game `ExcelWhatsNew`
changelog). Tips/menus checks: the packet itself is the menus
surface; broadcaster tips carry no magiclevel-door copy (verified
at packet staging, not assumed).

---

## 6. Registry / layout / data migration (from live 2.0.x state)

### 6.1 Live state at design time (evidence-stamped)

Desk facts, each from a record read for this design — **premises to
re-prove by fresh pull at filing (G1), not receipts**:

- Live jar: ExcelMenu **2.0.0**, 286,829 B /
  `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2`
  (2.0.1 built and PENDING; §1.3).
- Registry **94 entries / 0 failed**, layouts **2 / 0**, definitions
  **13 / 0** (Item 5 OUTCOME, Nov 13, 2026, 00:23 CST window —
  the most recent reload verification).
- `layouts/server_menu.yml` (grouped, landed Nov 12): 6,959 B /
  `fceb14331f02036dd0d9e6283b061a7d4acbd3094d658bd1ab9f37425a76e510`.
- `layouts/commands_menu.yml`: 4,956 B /
  `146db1d91c4d34c49d7f43c1fd55f2eb80a6979215365b29442848ea47403bcb`.
- `config.yml` (grouped): 2,218 B /
  `d14d9cb14f3bf53552a57bdda25b5d13ad813ea3ce492bd31ef07e4958283eb7`.
- `entries/skills.yml` (post-Item-5): 438 B /
  `ccc527640bbc3f142be9fd2a5fc6cc85b1ebae4a2181a218bc8b77d3915743f1`.
- ExcelSkillsPanel 0.1.0 live (jar 45,497 B /
  `4f9f819d75a4e9fba098cbc1ade42e2d01265d678948993f918083125d23cb19`);
  0.1.1 PENDING at design time (independent of this design).

### 6.2 Migration table (the whole window's data movement)

| Live artifact | 3.0 action | Class |
|---|---|---|
| ExcelMenu jar | swap → 3.0.0 jar, **same live filename** (name from the G1 `plugins/` listing) | gated jar swap |
| `layouts/server_menu.yml` | edit: add `pinned: [spawn, tpmenu]`; add page-2 fixed Rearrange item (slot 8, §3.4). `.bak-<date>-pre30` kept; pull-back verified | data placement in jar window |
| `layouts/commands_menu.yml` | **no change** | — |
| `config.yml` | **merge-edit only**: append the `rearrange:` knob block (§3.7). Live config carries server-specific values below the menu sections (e.g. the `spawn:` village-fallback coordinates visible in the staged copy's tail) — **never wholesale-replace this file**; the placement is a diff against the G1 pull, and §3.7's code defaults make even a skipped merge harmless | data placement in jar window |
| `entries/magiclevel.yml` | delete (`.bak` kept) — §5 | data placement in jar window |
| `entries/skills.yml` | **no change** (Item 5 already placed it; §4 is code) | — |
| all other `entries/*.yml` (92) | **no change** | — |
| `menus/skills_perks.yml` | edit: `magic:` item → Magic Level door (§4.3). `.bak` kept; pull-back verified. Live landed hash is **not** in desk evidence — the filing G1 pull establishes it (§8) | data placement in jar window |
| other `menus/*.yml` (12) | **no change** | — |
| `playerdata/` | **new folder**, created by the plugin at enable; no migration (absence = default). Survives rollback (a 2.0.1 jar ignores it) | runtime-created |

Post-window expected counts: registry **93 / 0 failed**, layouts
**2 / 0**, definitions **13 / 0**, enable line reads
`ExcelMenu 3.0.0 enabled …`, SEVERE 0, bridge self-check PRESENT.

### 6.3 Bundled-resources discipline (jar contents)

The 2.0.1 jar-diff proved the house pattern: bundled
entries/layouts/menus/config inside the jar are **first-run seeds**
that lag live data by design (the 2.0.1 jar shipped the FLAT bundled
layout while live data was grouped). 3.0 keeps that discipline:

- Bundled data in the 3.0 jar is byte-identical to the 2.0.1 jar's
  **except** the single retirement delta: `entries/magiclevel.yml`
  removed (+ the `BUNDLED_ENTRIES` line). The bundled
  `server_menu` layout does **not** gain `pinned` (its bundled form
  is FLAT; pins land via the live layout placement, §6.2 — a fresh
  install elsewhere is out of this programme's scope).
- Expected jar-diff classes vs 2.0.1: `plugin.yml` (version +
  `/rearrange` command + softdepend), `FormModel*` (pins + editor
  target), `LayoutDef/LayoutLoader` (pinned), `EntryService` +
  new `PlayerMenuPrefs/OrderMerge` classes, `FormService`
  (EditorTarget + prefs plumbing), `ScreenHooks` (delegation),
  `ExcelMenu` (wiring + bundled list), new editor classes
  (`MenuEditorCore/Forms/Menus`, `PrefsStore`), `MANIFEST.MF`
  version, minus `entries/magiclevel.yml`. Anything outside this
  list in the diff is a scope defect — stop and reconcile before
  filing (the 2.0.1 TEST-REPORT jar-diff standard).

---

## 7. Rig + verification plan

House pattern throughout: pure-core unit suites first, desk rig
(Paper 26.3 + Geyser, the phase-2 rig skeleton) second, PC
test-server mirror with live-pulled jars at filing time third,
Mike's player proofs last. **Desk rig proves boot/load and model
behaviour; a real Bedrock client's form *feel* is Mike's live pass** —
the SkillsPanel build record's honest limit, carried forward.

### R0 — Base + defaults reproduction (before any 3.0 behaviour is credited)

1. §1.2 base gate: 2.0.1 rebuilt byte-identical (`c6da29ba…`,
   287,906 B), suite `OK (128 tests)`.
2. Desk rig on the **unmodified 2.0.1 jar**, seeded with the
   filing-time live data pull (grouped layout, 94 entries incl.
   Item 5's `skills.yml`, 13 definitions): record the control
   matrix — picker shape, each group form's buttons **with Back
   last**, `commands_menu` picker + one group form, Java chest
   page 1 + page 2 slot maps, Bedrock `skills` tap → old screen.
   This matrix is the non-regression baseline every later step
   diffs against.
3. **Platform-rank check (§3.2's STOP gate):** script the
   rank-compare of `order` vs `order-bedrock` within every
   `server_menu` group over the same live pull. Divergence in any
   group = STOP, escalate, do not build the merge on a guess.

### R1 — Unit + harness suites (build folder)

- **Carried suite:** the 128 tests stay green except the
  *enumerated intentional deltas*: `BundledEntriesTest` (bundled
  census −1 for magiclevel), any `FormModelTest` picker assertion
  that must now account for pins/Rearrange on the constructed
  grouped `server_menu` variant. Every delta is listed in the
  build's TEST-REPORT with its reason; an unlisted red is a stop.
- **Golden harness D1–D11 + catalogue C1–C8:** unchanged counts
  (the harness models the resolver, not the new render steps —
  the 2.0.1 precedent).
- **New suites:**
  - `PinsModelTest` — §2.5's cases (picker order/labels/targets,
    group exclusion, flat dedupe, guard-failed pin, empty pins =
    2.0.1 shape).
  - `LayoutLoaderPinsTest` — parse, duplicate refused, >4 refused,
    unknown key still refused, absent = empty.
  - `OrderMergeTest` — §3.3's properties: untouched group =
    default; stored order honoured; new entry appends; vanished
    id drops; unknown group ignored; group-order merge;
    idempotence; prefs for a *different* menu ignored.
  - `PrefsStoreTest` (core-side codec) — round-trip; partial
    record (one group) round-trips; corrupt text → no-prefs +
    error surfaced; `version: 99` → no-prefs; ids with unknown
    entries survive a save/load cycle verbatim (dropping is
    merge's job, not the codec's).
  - `MenuEditorCoreTest` — move up/down/top/bottom at both
    boundaries (no-op + boundary flag), insert algebra (the Java
    editor's move = the Bedrock editor's Up/Down sequence —
    property test: random move sequences converge to the same
    list under both algebras), reset produces the empty record.
  - `FormModelTest` additions — picker on the grouped
    `commands_menu` fixture carries **no** pins and **no**
    Rearrange button (scope proof); group forms on `server_menu`
    still end with Back → `PickerTarget` (2.0.1 non-regression,
    now with pins present in the layout).

### R2 — Desk rig behaviour matrix (3.0 jar, same seed as R0)

Boot: `ExcelMenu 3.0.0` in plugin list, counts **93 / 2 / 13**
(after the §6.2 data set is applied to the rig's data folder),
SEVERE 0, bridge PRESENT. Then, diffing against R0's control:

| # | Case | Expected |
|---|---|---|
| 1 | Bedrock picker open | buttons = Spawn, Teleport (pins), Travel, Me, Shops, Help, Settings, Chest Organization, Rearrange Menu; **no Back** |
| 2 | Pin tap — Spawn | the spawn confirmation flow, exactly as from Travel in R0 (stats tap recorded once) |
| 3 | Pin tap — Teleport | TeleportScreen picker opens, as in R0 |
| 4 | Travel group form | Party, Guild, EliteMobs, **Back** last; Spawn/Teleport absent |
| 5 | Every other group form + one `commands_menu` group | identical to R0 control, **Back** last (2.0.1 non-regression) |
| 6 | Java chest, pages 1–2 | slot maps identical to R0 except page-2 slot 8 = Rearrange fixed item |
| 7 | Bedrock `skills` tap, panel jar present on rig | **panel opens**; old screen not shown |
| 8 | Bedrock `skills` tap, panel jar removed from rig | `SkillsPerksScreen` exactly as R0 (fallback proof) |
| 9 | Java `skills` click, both panel states | present → panel (Item 5 behaviour); absent → standard unavailable message (Item 5 guard behaviour) |
| 10 | `magiclevel` | no door on either platform; `entries/magiclevel.yml` absent after a **restart**; typed `/magiclevel` still answers (ExcelMagicLevel jar on rig) |
| 11 | Rearrange — Bedrock | E1→E2→E3 move Me's Summons to top; save; **relog**; Me form shows new order (persistence across relog); Java chest for the same account shows the same within-group order (cross-platform single store) |
| 12 | Rearrange — Java | J2 insert-move on Travel's non-pin entries; Bedrock group form reflects it; picker group-order change via E4 shows on next picker open |
| 13 | Rearrange — merge | with prefs stored: add a dummy entry file to a touched group + reload → customized player sees it **last** in that group, default player sees it in default position; delete a stored entry's file + reload → order holds, no error, menu opens |
| 14 | Rearrange — reset | E5/J4 reset → prefs file gone, next open = default order, chat confirmation received; editor from a default state offers no Reset |
| 15 | Rearrange — corrupt prefs | hand-corrupt the player's file → menu opens at default, one SEVERE naming the file, editor's next save repairs it |
| 16 | Editor Back discipline | every editor form (E1–E5) ends with Back and lands where §3.5 says; J1/J2/J3 Back items likewise; closing any editor screen mid-edit loses nothing (per-move saves) |
| 17 | `rearrange.enabled: false` | picker loses the Rearrange button, `/rearrange` refuses politely, stored prefs ignored, menu otherwise = R2 case 1 minus editor |
| 18 | Guarded-entry ordering | store an order naming a guard-failed entry (rig: an entry whose plugin is absent) → no crash, unavailable door keeps its ordered place |

### R3 — Filing-time mirror rig

On the PC test-server mirror with **live-pulled companion jars**
(the 2.0.1 INSTALL-PACKET pattern): repeat R2 cases 1, 4, 5, 7, 8,
10, 11 in brief against the real Geyser/floodgate + ExcelSkillsPanel
+ ExcelMagicLevel jars as pulled. SEVERE 0. This is the last gate
before the MANIFEST filing.

### Player proofs owed after install (Mike)

1. Bedrock: compass → **Spawn is one tap** (pin) and lands the spawn
   confirm; **Teleport is one tap**; Travel section no longer lists
   them; Back still returns from every section (2.0.1 proof rides).
2. Bedrock: Rearrange Menu → move one door in Me → close → relog →
   order kept; Reset → default returns.
3. Bedrock: Skills & Perks → the **Skills panel** (not the old
   screen) — this also closes Item 5's Bedrock proof gap.
4. Java: compass unchanged except the page-2 Rearrange item; Skills
   & Perks → panel (closes the owed Java panel pass); Rearrange via
   `/rearrange` → one move → Bedrock shows it.
5. Magic Level: no compass door; Skills panel shows the Magic
   block; `/magiclevel` typed still answers in chat.

---

## 8. Install packet outline + sequencing gates

**Class: gated jar swap** (jar + data placements; NOT a text
live-edit — a mixed entry is fully gated by house rule). The
failsafe owns the install; the supervisor files; the parent fires
on filing. One window, one backup, one restart.

### Sequencing gates (all must hold before filing)

- **G-a — 2.0.1 landed first.** Live jar at filing = 2.0.1
  (`c6da29ba…` / 287,906 B) via its PENDING entry, *or* the packet
  explicitly supersedes that entry (§1.3). STOP if the live jar is
  anything else.
- **G-b — panel gate (satisfied).** ExcelSkillsPanel installed
  (Nov 12, 23:48 CST) + Mike's Bedrock acceptance (Nov 13,
  ~12:06 AM). The owed **Java** panel pass does **not** gate 3.0 —
  R2 case 9 + player proof 4 exercise the Java panel path inside
  this programme instead.
- **G-c — no dependency on SkillsPanel 0.1.1.** The hook checks
  plugin *presence* only; 0.1.1 (PENDING at design time) may land
  before or after 3.0 with no interaction.
- **G-d — battle/weekend discipline.** No jar swap over an open
  battle state (seal governance), and the window avoids the Sat
  Nov 14, 2026 Warden attempt evening (~8:30 PM, flexible to
  9–10 PM — Kay's birthday dinner day; do not crowd it). Earliest
  sensible slot: the first verified-empty window after the build +
  R3 are green; the weekend protocol governs if it lands on a
  weekend (empty → push immediately; players on → countdown, then
  push and bring the server back).
- **G-e — G1 fresh pulls at filing** (PC route; panel fallback per
  lane rules; each receipted path/size/SHA-256/timestamp per
  PACKET-TEMPLATE): `plugins/` jar listing (exact live filename);
  the live ExcelMenu jar (expect per G-a); `layouts/server_menu.yml`
  (expect `fceb1433…` / 6,959 B); `layouts/commands_menu.yml`
  (expect `146db1d9…` / 4,956 B — premise for "untouched");
  `config.yml` (expect `d14d9cb1…` / 2,218 B — the merge-edit base);
  `entries/skills.yml` (expect `ccc52764…` / 438 B — the hook's
  fall-through target); `entries/magiclevel.yml` (its live hash is
  the deletion premise + the `.bak` must pull-back-match it);
  `menus/skills_perks.yml` (live copy — **no desk hash exists**;
  this pull establishes the edit base). STOP/re-base on any
  mismatch.

### Packet steps (outline — the filing writes the full packet)

0. **Backup first + cap-at-6:** one fresh Shockbyte backup for the
   window; prune oldest manual backups to ≤6 total, naming prunees
   (never the Daily Backup's newest copy or the two newest manual
   restore points). Marker discipline: `WINDOW-IN-PROGRESS.txt` at
   start, deleted at end, re-checked before Shut Down and before
   Start (foreign marker = coordination abort). Emptiness ×2 for
   the gated swap.
1. **Stage data placements** from the G1 pulls: edited
   `server_menu.yml`, merge-edited `config.yml`, edited
   `skills_perks.yml` — each with size + SHA-256 recorded; diffs
   reviewed against §6.2 (any extra hunk = stop).
2. **Swap the jar** (live jar kept as `.bak-<date>-pre30` beside it;
   placed jar hash-verified = the build's recorded 3.0.0 hash).
3. **Place the data set; delete `entries/magiclevel.yml`** (in-place
   `.bak` kept; the three edited files get `.bak-<date>-pre30`).
4. **One restart** (panel Shut Down → Start).
5. **Boot verification** (all must hold, else rollback): plugin list
   shows ExcelMenu **3.0.0**, ExcelBedrockMenu absent; enable line
   with registry **93**, layouts **2**, definitions **13**; registry
   diff vs pre-window shows `−1 (magiclevel)` and no surprises;
   `entries/magiclevel.yml` **still absent post-restart** (§5.3);
   bridge self-check PRESENT; SEVERE 0 attributable to ExcelMenu;
   TPS 20.0 after settle; placed data files pull-back EXACT vs
   their staged hashes.
6. **Smoke (console/RCON):** `/excelmenu list` shows 93 entries and
   no `magiclevel`; a console `skillpanel` attempt reports
   player-only (the panel command exists).
7. **Same-pass surfaces** (not a follow-up): Guide knowledge +
   intents re-point for the Magic Level door (§5.4) and a rearrange
   how-to chunk; website pair in the same pass; What's New on
   **both** surfaces (site section + in-game `ExcelWhatsNew`
   changelog, verified by pulling the live in-game file); rotation
   tips checked (no menu-door copy expected — verified, not
   assumed); in-game menus check = this packet itself.
8. **Records:** build folder's BUILD-RECORD / TEST-REPORT /
   SHA256SUMS; Drive deposit (jar + sources zip + SOURCE.md) into
   the Minecraft WRV folder; wrv-plugins **draft** record PR
   (stays open) — with the **G2 duplicate check run first**
   (`pr-exists.sh`) and the claim line in the close-out record;
   website PR merged + byte-verified with the **G3 Pages deploy
   check** run after merge.
9. **Player proofs:** §7's five Mike proofs; the item closes on
   them, not on the boot log.

**Rollback:** Shut Down → restore the pre-3.0 jar from its `.bak`
→ restore the three edited data files from `.bak-<date>-pre30` →
restore `entries/magiclevel.yml` from its `.bak` → Start → verify
the 2.0.1 enable line and 94/2/13 counts. `playerdata/` may stay
on disk (ignored by 2.0.1). The step-0 backup is the deeper net.

---

## 9. Open questions — each with a recommended default

None of these crosses a DECIDE-AND-LOG hard line (no spend, no send
in Mike's name, no one outside the family server, nothing
irreversible, nothing contradicting recorded design law). Each is
therefore **decidable by the supervisor**, logged in the decision
ledger, and reversible as noted. They are questions because the
design should not pretend Mike already answered them.

| # | Question | Recommended default (decide-and-log) | Why / reversal |
|---|---|---|---|
| Q1 | Pins on Bedrock: **root-only** (removed from Travel's form) or **duplicated** (root + Travel)? | **Root-only.** | Grouped DESIGN §3 calls root-only "the faithful read of Mike's words" (Spawn/Teleport stay *outside* the sections). Duplication gives one door two homes and two stats rows' worth of confusion. Reversal: one `FormModel.group` filter line. |
| Q2 | Rearrange scope: `server_menu` only, or `commands_menu` too? | **`server_menu` only in v1.** | The catalogue is reference material; nobody asked to personalise it, and its 56 entries double the editor proof. The prefs model's `menu` field leaves the door open. Reversal: a config list entry + editor enablement, no redesign. |
| Q3 | Cross-group moves and/or hide/show entries? | **Neither in v1.** | Not commissioned; cross-group moves collide with Java's fixed slot lists (per-player ChestModel refusals) and group semantics; hiding duplicates guard/date visibility. Revisit only on a player ask. |
| Q4 | Is **group (picker) order** part of "rearrange", or entries only? | **Include it** (E4/J3). | "Rearrange the menu to their own liking" plausibly includes which section comes first; the cost is one more list in the same model. Its Java no-op is documented in-editor (§3.6 J3) so it never reads as broken. |
| Q5 | Default semantics: **by-construction** (live data order) or a frozen snapshot of Nov 13's order? | **By-construction** (§3.2). | A snapshot is a second source of truth for one fact — the defect this programme has rejected twice. By-construction also matches "default" semantics when Mike next re-arranges the server menu himself. |
| Q6 | The `skills_perks` `magic:` door (§4.3) targets `[player] skillpanel` per grouped §6 — but on the *fallback* path (panel absent) that door dead-ends, since the definition is then the live surface. Keep §6, or make the fallback door run `[player] magiclevel`? | **Keep §6 (`skillpanel`).** | §6 is Mike-approved design of record; the panel-absent state is already degraded (Bedrock serves the old screen, Java shows the guard message), and `/magiclevel` typed remains for that state. Re-litigating an approved spec inside a build design is how scope drifts. Reversal: one data line. |
| Q7 | Java editor door: the page-2 fixed item sits at slot 8, which an 8-entry `chest_tools` group could one day collide with (§3.4). Accept, or move it pre-emptively? | **Accept slot 8, with the filing gate re-counting `chest_tools`** (3 entries today, 5 slots of headroom); fallback slot 17 if the group has grown at filing. | Page 2 is where screen space actually exists; inventing a Settings-row slot would mean surgery on a full row. The gate makes the risk a checked premise, not a hope. |
| Q8 | Per-group reset in the Java editor (J2's `Reset This Section`), or global reset only? | **Include per-group reset** (both platforms' group editors get the affordance; Bedrock: a `Reset This Section` button on E2, same two-step care — it sits behind E3-style confirm: place it on E2 as a button that opens the E5 shape scoped to the group). | Players experiment per-section; a global-only reset punishes one section's experiment with everyone's. Cheap: reset = drop one key from the record. |
| Q9 | Editor availability: all players, or permission-gated? | **All players**, with the §3.7 config kill-switch as the supervisor's lever. | DECISIONS frames rearranging as a player feature on a family server; a permission would need a reason that doesn't exist. |
| Q10 | Pins mechanism: data-driven `pinned:` list (any entry, cap 4) or hard-coded spawn/tpmenu? | **Data-driven list, cap 4.** | The layout already owns chrome-as-data; a future third pin (e.g. a seasonal door) becomes a data edit, not a rebuild. The cap keeps the picker a picker. Hard-coding two ids in the renderer is the kind of special case the registry exists to prevent. |

**Explicitly NOT questions for Mike:** window timing (G-d is a
scheduling gate the supervisor works under the weekend protocol),
the 2.0.1-first ordering (G-a is lane discipline), and every §9 row
above (in-bounds judgment, logged). If the supervisor judges any
row differently at build time, the decision ledger entry — not a
parked item — is the mechanism.

**Unverified items this design carries honestly** (not questions —
build/filing-phase checks): the live `menus/skills_perks.yml`
landed hash (G1 pull, §8); the exact live jar filename (G1
listing); the Java/Bedrock default-rank equality (§3.2 STOP gate,
R0 step 3); `/rearrange` command collision (build-phase check,
§3.4).

---

## Appendix A — File-change inventory (build)

**Modified (production):** `core/render/LayoutDef.java` (+`pinned`),
`core/render/LayoutLoader.java` (+key/validation),
`core/render/FormModel.java` (pins, group exclusion, flat dedupe,
Rearrange button, `EditorTarget`), `core/registry/EntryService.java`
(prefs overload), `bukkit/FormService.java` (`EditorTarget`
handling, prefs plumbing, picker config param), `bukkit/ScreenHooks.java`
(skills delegation), `bukkit/ExcelMenu.java` (wiring, `BUNDLED_ENTRIES`
−1, `/rearrange` registration, editor services lifecycle),
`bukkit/RegistryMenus.java` (prefs-aware opens; pinned cross-check
SEVERE), `plugin.yml` (3.0.0, `/rearrange`, softdepend
+ExcelSkillsPanel), `build.sh` (VERSION).

**New (production):** `core/registry/PlayerMenuPrefs.java`,
`core/registry/OrderMerge.java`, `core/MenuEditorCore.java`,
`bukkit/PrefsStore.java`, `bukkit/MenuEditorForms.java`,
`bukkit/MenuEditorMenus.java` (+ its holder, either nested or
`bukkit/MenuEditorHolder.java`).

**Removed (resources):** `src/main/resources/entries/magiclevel.yml`.

**Tests:** deltas + new suites per §7 R1. **Unchanged by design:**
`ChestModel`, `EntryResolver`, `EntryLoader`, `BedrockDetail`,
`SkillsPerksScreen/Core` (fallback kept), the definition engine,
`commands_menu` everything.

## Appendix B — The default arrangement (Mike's, as encoded)

Post-grouped, post-Item-5, **pre-3.0** Bedrock within-group
sequences (grouped DESIGN §8, updated); 3.0 changes only the two
marked rows' *Bedrock reachability* (pins leave Travel's form;
magiclevel leaves Me) — no weight changes anywhere:

| Group (picker order) | Sequence |
|---|---|
| travel | **spawn 0\*, tpmenu 5\***, party 10, guild 15, elitemobs 25 |
| me | skills 30, ~~magiclevel 35~~ (retired §5), spellbook 40, wandstyle 45, summons 50, utility_pouch 55, leaderboard 65, lostfound 75, jobs 85, trash 100, boss_rewards 110, landmarks 120, braingames 130, trainingdummy 140 |
| shops | shop 150, excelshop 155, cosmetics 160 |
| help | whatsnew 170, ask 175, commands 180, info 185, website 190, halloween 195†, halloween_scares 198† |
| settings | makersmark_settings 200, details_settings 205, autosort 210, silkwarnings 215, aimassist 220, autojoin 225 |
| chest_tools | chestfilter 230, filterrecover 235, maxorg 240 |

\* rendered as root pins on Bedrock after 3.0 (§2); unchanged in
Travel on Java. † date-locked (Oct 31 – Nov 5 window).

Picker order (groups): travel, me, shops, help, settings,
chest_tools — from the layout `groups:` map; Java group sequence
from `config.yml` matches it.

## Appendix C — Evidence index

- Decisions + programme design: `~/workspace/menu-grouped-20261112/`
  (`DECISIONS.md`, `DESIGN.md`, `INSTALLED.txt`, `OUTCOME.txt`,
  `staged/` — the landed grouped data set).
- Back-button fix: `~/workspace/menu-items56-20261113/item6-back-button/DESIGN.md`;
  build: `~/workspace/excelmenu-201-build-20261113/`
  (`BUILD-RECORD.md`, `TEST-REPORT.md`, `INSTALL-PACKET.md`,
  `ExcelMenu-2.0.1.jar` `c6da29ba…`, sources zip `620f7af0…`).
- Item 5 landing: `~/workspace/menu-items56-20261113/item5-skills-door/`
  (`INSTALL-PACKET.md`, `INSTALLED.txt`, `OUTCOME.txt`, live pull +
  staged `skills.yml`).
- Panel: `~/workspace/skillspanel-build-20261112/` (`BUILD-RECORD.md`
  — `/skillpanel`, jar `4f9f819d…`; `INSTALLED.txt`, `OUTCOME.txt`).
- Backlog of record: `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`,
  MENU PROGRAMME section (Items 1–8 + run-278 continuation notes).

*Method note: every engine claim above cites the 2.0.1 source tree,
a build record, or a landed packet record read for this design.
Live-state figures are labelled premises-to-re-prove where the G1
rule requires it. Desk only — nothing was filed, built, or touched
on the live server.*

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
