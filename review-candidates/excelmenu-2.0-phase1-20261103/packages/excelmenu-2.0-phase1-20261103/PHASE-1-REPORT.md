# ExcelMenu 2.0 — PHASE 1 REPORT

Date: 2026-10-31 · Build: `ExcelMenu-2.0.0.jar`,
SHA-256 `1419bdb9ddd4b97b32f3fef466e63c305a6c6cf94e443416c8abd513adde0fd3`,
223,472 bytes, byte-identical across three builds.
**No live changes were made. No panel was touched.** The install
packet is filed, not executed.

## What shipped in this build

DESIGN-2.0 Phase 1 + Amendment B, complete:

1. **The two thin renderers.** Java chest GUI (`RegistryMenus` +
   `ChestModel`) and Bedrock Cumulus forms (`FormService` +
   `FormModel`, including the native compass form) consume Phase 0
   resolver output through the unified bridge (`PlatformBridge`,
   which now also carries the Cumulus send surface — transport
   only). All guard/state/date logic resolves in the core; the
   render models are Bukkit-free and unit-tested directly.
2. **Production registry.** 82 entry files: the Server Menu (26,
   converted from the live definition), the Bedrock BUTTONS
   catalogue (the same 26, harness-verified), and the commands
   catalogue (56, Amendment B). Layouts are data (`layouts/*.yml`):
   the server menu reproduces the live chest slot-for-slot —
   including the deliberate empty slot 39 — and the commands
   catalogue is re-laid over two 54-slot pages (see Deviations).
3. **The §3.3 slot mechanics.** The jar ships in the ExcelMenu slot:
   registry menus + the 16 unconverted definition menus + `/dm`,
   while ExcelBedrockMenu 1.5.27 still owns `/menu` (contract below).
   Definition files for converted menus are skipped loudly at load;
   `commands_menu_page2` retires via a config list.

## The Phase 1 coexistence contract (explicit, per the task)

- **A Java player** opening the Server Menu (via the compass item,
  ExcelMenuJoin, or any `[openguimenu] server_menu` door) gets the
  registry chest GUI: same 45-slot chest, same title, same items in
  the same slots as today's 0.1.8 — including the empty slot 39.
  Every other menu (shops, skills, basics, teleport confirms) is the
  unchanged definition engine. `/dm` works exactly as in 0.1.8, plus
  registry listing/reload.
- **A Bedrock player** gets the same doors: the Server Menu renders
  as the native Cumulus compass form (same 25 buttons, same order,
  same group-coloured labels as 1.5.27), subgroup screens (Skills,
  Teleports) render as forms, and definition submenus keep the
  0.1.8 form renderer — now sent through the one bridge.
- **`/menu`** (and `/menus`, `/mainmenu`, `/compass`,
  `/toggles`): still ExcelBedrockMenu's, byte-for-byte today's
  behaviour. ExcelMenu 2.0 registers NONE of the /menu family in
  Phase 1 (plugin.yml declares only `excelmenu` + `dm`).
- **`/servercommands`**: still ExcelBedrockMenu's command showing
  the OLD 50-row catalogue. The new 56-row union catalogue lives
  behind the Server Menu's Server Commands door
  (`[openguimenu] commands_menu`) on both platforms. Two catalogue
  surfaces coexist for Phase 1's duration; they unify when Phase 2
  transfers the command.
- **`/excelmenu`**: the merged admin surface — `list` (definitions +
  registry), `open <menu> [player]`, `entries [menu]`, `reload`
  (config + registry with diff + layouts + definitions).
- ExcelMenuJoin is untouched and keeps working: it runs `menu` for
  the compass trigger, which still lands on ExcelBedrockMenu in
  Phase 1 — whose compass form and the registry form are
  harness-proven identical in content.

## Proven

- 81 JUnit tests green (54 Phase 0 + 27 new), incl. edge cases:
  guard default-variant + standard unavailable message, dated entry
  in/out of window on both renderers, platform overrides, layout
  refusal shapes, chest overflow as an error (never a silent drop),
  form-session expiry/supersede/world-change.
- Golden harness over production entries: 108 viewer states × 2
  platforms, 5,418 rows, **zero unclassified differences**, D1–D9
  all occurring and enforced.
- Catalogue harness: 56 rows, C1–C7 all occurring, zero unclassified
  differences; conversion completeness counts reconcile 53 + 50 → 56
  with every shared row verbatim against the old Java file.
- Rig (Paper 26.3-231 + this jar + BedrockMenu 1.5.27 + Geyser b1277
  + PAPI): 82 entries / 2 layouts / 16 definitions live, 3 converted
  files skipped loudly, bridge PRESENT, console `excelmenu list` +
  `dm list` + `excelmenu reload` (zero-change diff) proven, console
  `menu` answered by BedrockMenu, clean shutdown, no SEVERE.
  Extracted data byte-identical to jar resources.

## Player-owed (not rig-provable — listed in the packet §6)

In-game rendering passes on both platforms; toggle flip re-render;
grouped catalogue use incl. typed-row hint taps; cross-menu doors;
dated-entry disappearance; definition menus in game; MenuJoin flow.

## Deviations and called interpretations (named, with evidence)

1. **Toggle trios dispatch the toggle form now (improvement, harness-
   invisible by design).** Phase 0 recorded that Java's chest
   dispatches ABSOLUTE state writes from the rendered variant while
   Bedrock re-reads at tap — a render→tap race. Evidence that the
   toggle form exists and is live-proven: ExcelBedrockMenu 1.5.27's
   own source dispatches `silkwarnings toggle` (line 1068) and
   `aimassist toggle` (line 1109) as the player. The converted
   entries for `silkwarnings` and `aimassist` now dispatch
   `[player] excelsilkwatch:silkwarnings toggle` /
   `[player] excelaimassist:aimassist toggle` + `[refresh]` on BOTH
   platforms. The harness's dispatch keys were already
   toggle-normalized, so the gate is unaffected; Java behaviour
   changes only in the raced case, where it becomes correct.
2. **Commands catalogue chest re-layout (forced).** The old two-page
   shape (54 + 27 slots, 68 item slots) cannot hold the 56-row union
   under per-group slot lists (union needs 56 entry slots + headers +
   nav = 69). Called reading: pages stay group-contiguous in the old
   group order, page 2 grows 27 → 54 slots, nav/back become layout
   page-turn items (`page:` field — layout chrome, not a new action
   verb). Bedrock is untouched by this (grouped forms don't paginate
   by slots). Player-visible change: Java page 2 is a full chest with
   the same rows in the same group order.
3. **Grouped-form note text adapted.** 1.5.27's group form tells
   players typed rows are "lines in the text" (they weren't buttons).
   In the registry every entry is a button (decision 4), so the note
   now reads "Buttons run the command; rows with <words> in them
   tell you what to type in chat instead." The picker content and
   group body format ("display - description") are verbatim.
4. **Typed rows are buttons on Bedrock (C6).** The six `[message]`
   rows (incl. /ask and /mark, C5) render as buttons that speak the
   Java type-it hint when tapped, instead of body-only text. This
   keeps one-button-per-entry uniform across renderers (the
   modularity law) at the cost of a small Bedrock behaviour change:
   the hint arrives on tap rather than in the form body.
5. **Java wording wins catalogue drift (C3), one flag for Mike.**
   Six rows where the platforms' descriptions differed keep the Java
   text (single canonical-source rule; the converter printed every
   pick). The most visible: **/shop reads "Open the Spawn Shop"** —
   Bedrock's button said "Open the Excel Shop". One-line data edit
   if Mike prefers the other wording (packet §7).
6. **Named staleness, deliberately NOT fixed in conversion:**
   the catalogue's /lostfound rows (both platforms) still say "last
   7 days"; the live Lost & Found rule is 14 days (changed Oct 31).
   The conversion gate requires verbatim reproduction, so the text
   ships as-is and the fix is a one-line data edit for Mike to
   approve with the packet (packet §7).
7. **D1/D5 untouched.** The two Phase-2 awareness items from Phase 0
   (Bedrock Auto-open state text; the "Maker's Mark settings" shared
   label) remain acceptances in the harness and are NOT implemented,
   per the tasking.
8. **ClickStats (§1.3).** Registry clicks/taps forward to
   ExcelMenuStats keyed by entry id via StatsSink (fail-soft
   reflection), and definition clicks pass the item id — the
   unify-on-entry-id path the design leaves open. ExcelMenuStats
   itself is unchanged.
9. **Guarded-five nuance preserved.** On the old Bedrock compass,
   five entries took a guarded path in code; in the registry the
   guard lives in the entries themselves (Phase 0 conversion), which
   both renderers honour — no per-plugin logic in renderers.

## What Phase 1 deliberately does not do (Phase 2+)

`/menu` transfer and ExcelBedrockMenu retirement; date-window and
confirm screens in forms; the teleport code gate; the ExcelClaimFly
menu family; ExcelMagicLevel's menu surface (its registry entry is a
data drop once that plugin builds — the registry already supports
it). The anvil spell's aim profile remains a stage-staging design
call, untouched here.
