# ExcelWandBars 0.3.7 — source & build record

## What this is

0.3.7 makes plain drag-and-drop a real spell-bar write path (Mike's live
verdict on 0.3.6, Nov 1, 2026, 7:25 PM CST: Arrange works, but dragging the
Ward spell onto a hotbar slot with the wand in hand did not stay, and neither
did dragging Wolf from the double-tap all-spells view — dragging a spell onto
the bar must just work, his original Oct 31 expectation), and it fixes the
spell position labels to name the bar AND the slot (Mike, same evening,
7:29 PM CST: "bar 17" for Cure is a flattened index, not a bar; Mend is
Bar 1 · Slot 1).

## Base (delta law)

- Base: shipped **0.3.6**, jar SHA-256
  `8335db03f015adc4a6a2464eee268fe7306f09d1abe96dec137f6474ba42a98f`,
  488,426 bytes, 216 entries (live on the server as `ExcelWandBars-0.3.4.jar`).
- Base source: the 0.3.6 tree at `~/workspace/wandbars-0.3.6/work/`
  (`src/`, `shipped/`, build files), copied to `~/workspace/wandbars-0.3.7/work/`.
- **Base proof (run before any change):** the unmodified copied tree was
  compiled with the 0.3.6 build command (Temurin 25,
  `javac --release 25 -encoding UTF-8 -g:source,lines -proc:none`) and
  repacked from the 0.3.5 jar with the 0.3.6 repack procedure → **byte-identical
  to the shipped 0.3.6 jar** (same SHA-256, same changed/added entry set as the
  0.3.6 build record: changed ArrangeHook, ArrangeHook$1, TapBarHook,
  core/LoadoutText, config.yml, plugin.yml; added ExitSweep, ExitSweep$Result,
  core/ExitSweepPlan, core/UnarmedDragPlan, core/UnarmedDragPlan$Verdict).
  The 0.3.7 delta therefore starts from a proven tree.

## Candidate

- `ExcelWandBars-0.3.7.jar`, SHA-256
  `fc77e22f40e4719da86ea639efb91a0e46ddbf3ee02db37b12bf09ae5d770c59`,
  499,456 bytes, 223 entries.
- Built twice from the same inputs; the two jars are byte-identical.
- Build: compile the full tree (as above), then `work/repack.py` repacks from
  `base-0.3.6.jar`: every unchanged entry carries over byte-for-byte, the five
  Geyser hook classes stay pinned to their shipped v65 bytes (never recompiled),
  `plugin.yml` / `config.yml` come from `work/shipped/`, entries sorted,
  2026-01-01 timestamps, deflate-9 — the same procedure as 0.3.5/0.3.6.

## Change scope vs 0.3.6 (jar-entry diff)

Changed (15): ArrangeHook (+ its $1), LoadoutHook, MagicHook (+ $Ctx,
$LastForm, $View — nested types re-emitted with their outer class),
core/LoadoutText, core/SpellFacts, core/SpellInfoText, core/SpellbookText
(+ $Entry, $GroupCount), config.yml, plugin.yml.
Added (7): core/BarPosition, core/DragWritePlan (+ $Intent, $Result, $Lift,
$Done, $1).
Nothing else moved. Every changed class sits in the drag/write path or the
position-label surfaces, plus the version stamp.

## Source delta

- NEW `core/DragWritePlan.java` — the unarmed drag interpreter's pure plan:
  pickup / place / placeWith / drop / dropSlot / drag producing PUT / REMOVE /
  RETURN intents against the loadout model (off-bar lifts carry from = −1; a
  place outside the bar row removes an on-bar spell and returns an off-bar
  one), the 500 ms same-spell commit dedupe (Arrange parity), the
  confirmation throttle, and the recent-write window the screen restore uses.
- NEW `core/BarPosition.java` — the position formatter: flat loadout slot +
  bar width → "Bar {bar} · Slot {slot}" (both 1-based), template held in one
  place, set from the `tap-bar.position-format` config key.
- `ArrangeHook.java` — the drag-write interpreter (LOWEST click + drag
  handlers beside Arrange's): any spell icon in the player's own inventory
  (bar row, main grid, all-spells presentation) can be picked up, click-carried
  or dragged; commits execute through LoadoutHook's new drag methods; the
  icon-protection swallows from 0.3.6 carry over (real item onto an icon,
  number-key swaps, shift-clicks, collects never move an icon); a throttled
  action-bar confirmation replaces chat; `keepScreenDrag` restores the bar
  after a commit's repaint (Arrange's keepScreen pattern, gated on a write
  having just landed); the 0.3.6 signpost gate stands down while drag-write
  is on and is byte-for-byte the behavior when it is off (kill switch);
  hold-to-read opening a spell page cancels a pending drag lift, exactly as
  it cancels an Arrange lift; counters and summary report the new state.
- `LoadoutHook.java` — `dragPut` / `dragRemove`: the Arrange execution path
  (same usable/wand/known checks, same BarLoadout write, save and repaint)
  minus the chat line, returning the Change; `done()` gained a quiet variant
  (the chat path is unchanged for /spellbar and Arrange); the picker and bar
  menus pass the bar width into the label functions.
- `MagicHook.java` — facts are annotated with the loadout position
  (barFlat/barPerPage) where every book is built, so the detail page and both
  flat lists agree; Spell Book entries carry the bar width beside the slot;
  the flat-list prefixes use the same BarPosition format (position labels come
  from the loadout only; the legacy Magic-page fallback is untouched where no
  own bar exists).
- `core/SpellbookText.java` — Entry gains `perPage`; the bar mark and the
  Bedrock button print `BarPosition.format(...)` instead of `"bar " + (index+1)`;
  the picker content/title take the bar width.
- `core/LoadoutText.java` — slot buttons/titles, pick-spell buttons/content,
  the pages line and the change result lines all name positions via
  BarPosition (no bare "slot N" remains); new `dragPutLine` / `dragOffLine`
  for the action-bar confirmations.
- `core/SpellFacts.java` — `barFlat` / `barPerPage` beside the legacy page.
- `core/SpellInfoText.java` — the detail page prints "On Bar n · Slot m" from
  the loadout, falling back to the Magic page line only when the loadout does
  not know the spell.
- `work/shipped/plugin.yml` — version 0.3.7 + one appended description
  sentence (validated: no colon-space anywhere in the scalar; YAML-parsed).
- `work/shipped/config.yml` — under `tap-bar`: `position-format` and the
  `drag-write` subsection (`enabled` / `confirm` / `confirm-gap-ms`), plus a
  note on the 0.3.6 signpost block that it now applies only when drag-write
  is off. The live server config is NOT overwritten at install (Bukkit keeps
  the existing file); every new key has the settled default in code, so the
  behavior is live with or without the file update.

## Ownership

All 0.3.7 code is ours (WinkReyesValley), same as the rest of the plugin.
Upstream Magic is untouched; the drag-write path consumes only its public
icon identity (`spellKeyOf`) and the wand's own inventory state.
