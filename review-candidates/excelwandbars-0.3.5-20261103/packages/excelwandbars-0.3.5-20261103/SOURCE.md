# ExcelWandBars 0.3.5 — combined WandBars UX release (source & provenance)

## What this is
Version **0.3.5** of ExcelWandBars (Wink Reyes Valley, family Minecraft server):
the four WandBars UX features built as one combined release, per
`~/workspace/wandbars-drag-remove/DESIGN.md`:

1. **§6 — BookHoldPlan gesture fix** (design step 1): a pure hold-gesture state
   machine (sneak-first burst semantics, 450 ms gap window, 500 ms re-arm
   silence, deferred-fire grace), press-based sneak-first ordering in TapBarHook,
   a shared `dispatchBook` with honest failure semantics (Bukkit form → chat
   fallback → one exact player failure line), and the corrected B_OPEN tip text
   ("press and hold **on air** with your wand").
2. **§5 — Spell Book New tab + place-from-book** (step 2): a durable per-player
   `unacked` set on the loadout model (persisted in loadouts.yml), NEW badges
   with announce + pointer lines, a New group/tab in the Spell Book (Bedrock
   button + Java line), [Got it] acknowledgement from the Java chat detail and
   the Bedrock detail form, [Mark all as seen], a "First free slot" entry on the
   place-from-book pick, and persistence-only migration (pre-feature loadout
   files load with zero badges).
3. **§2 + §3 — Arrange mode + hold-to-read** (step 3, shared inventory-screen
   hook `ArrangeHook`): pointer-first Arrange entry with Java "Arrange by
   dragging" and the Bedrock arrange button; drag-off / drop-with-lift removal
   and in-bar moves planned by a pure `ArrangePlan` over NBT-keyed icon identity;
   Bedrock hold-to-read via a pure `HoldReadPlan` (lift → ≥800 ms silence opens
   the §5 detail form; any other click commits/cancels).

The design's freeze caveats are retired: the cooldown-freeze investigation
(`~/workspace/wandbars-cooldown-freeze/INVESTIGATION.md`) proved the freeze was
Magic's mana-wait glyph and refuted the slot-mis-paint theory (the renderer
resolves spells by item NBT key), so icon-identity reads in Arrange use the
NBT-keyed `spellKeyOf` path throughout and were exercised in the rig.

## Delta base (install-time rule)
Built directly on the **currently shipped** build: ExcelWandBars 0.3.4 with the
Oct 30, 2026 fake-tap fix.

- Shipped jar (local copy): `~/workspace/wandbars-fix/ExcelWandBars-0.3.4-faketapfix.jar`
- Shipped SHA-256: `d8a38fc637bc3e21be9fec6ef30f0e091aba738831ae40a8e1383474cf3179de`
- Live server filename: `ExcelWandBars-0.3.4.jar`
  (panel backup `.bak-20261030-prefaketapfix` is the pre-fix "1915" jar —
  NOT the delta base.)

## Base-tree verification (before any change)
Every class this release modifies was first proven to reproduce the shipped jar
byte-for-byte from source (full record: `work/BASE-VERIFICATION.md`):

- Base tree = the fake-tap packet's coordinator sources
  (`~/workspace/wandbars-fix/work/packet/source/coordinator/src`) with the
  fake-tap fix's own `TapBarHook.java` and `core/WandSlotPlan.java` laid over
  them from `~/workspace/wandbars-fix/source/`.
- Toolchain: Temurin 25.0.4.1 (`~/workspace/.jdk25/bin/javac`), flags
  `--release 25 -encoding UTF-8 -g:source,lines -proc:none`.
- Result: **all** `com.winkreyes.excelwandbars` classes reproduce byte-for-byte,
  with two documented exceptions:
  - The 5 Geyser hook classes (GeyserTapHook/GeyserHoldHook + inners) reproduce
    byte-for-byte only at `--release 21` (the shipped build compiled them at
    class-file v65; everything else is v69). This release does not modify them;
    the candidate **pins their shipped bytes** in the repack.
  - 26 shipped classes have no coordinator source (FirstObservationFloor ×11,
    `wrv/magic/eligibility` ×15). This release does not touch either family;
    they carry over byte-for-byte in the repack.
- `core/WandSlotPlan.java` (the fake-tap fix's planner) is **unmodified** by
  this release; its class is byte-identical in the candidate.

## Candidate
- File: `ExcelWandBars-0.3.5.jar` (in this folder)
- SHA-256: `3a5c0d0db3493375759f11688d2356ddd52bed8d0a7fffb8472e5a52763cd622`
- 211 entries: 20 changed + 14 added vs the shipped jar; 5 Geyser classes pinned
  to shipped bytes; full rebuild reproduces the identical SHA-256.
- Repack method: shipped jar as the base archive, entries sorted, timestamps
  pinned to 2026-01-01, deflate-9 (`work/build-candidate.py`).
- Versioning: the plugin's own line — 0.3.4 (fake-tap fix) → **0.3.5**
  (plugin.yml version + description updated; config.yml gains the documented
  `tap-bar` keys: book-hold, book-hold-ms, book-hold-java, arrange-enabled,
  arrange-timeout-seconds, hold-read, hold-read-ms; and `bar-loadout.new-tab`).

## Sources
Working tree: `work/src` (this folder). New pure planners (no Bukkit imports,
unit-tested in isolation): `core/BookHoldPlan.java`, `core/ArrangePlan.java`,
`core/HoldReadPlan.java`. New hook: `ArrangeHook.java`. Modified:
`TapBarHook.java` (§6), `LoadoutHook.java`, `MagicHook.java`, `ExcelWandBars.java`,
`core/BarLoadout.java`, `core/LoadoutText.java`, `core/SpellbookText.java`,
`core/SpellTip.java`, `config.yml`, `plugin.yml`.

## Qualification fixes (found by the rig, fixed in this release)
Three defects surfaced during rig qualification and are fixed in the candidate
(each fix is in the §6/§2 hook layer only; the pure planners are unchanged):

1. **§6 press chain survived a sneak release.** The hook's press chain
   (`bookPressStart`/`bookLastSignal`) was only cleared on quit, so a second
   crouch-hold started within the 700 ms chain window of a fired burst was
   judged against the old press and could fail sneak-first. The sneak-release
   edge now clears the chain alongside `bookHold.sneakUp`.
2. **Tick-scheduled threshold checks vs wall-clock thresholds.** Both the §6
   scheduled check and the §3 hold-read decision were scheduled by ticks but
   judged by wall clock; after a server stall, tick catch-up could run the
   check before the hold had actually elapsed and the gesture was dropped.
   Both checks now wait out the remainder of the window and re-judge
   (the plans' pre-threshold verdicts are non-consuming, so re-checking is
   safe by construction).
3. **Arrange mode died after its first commit.** A loadout change applying
   closes the wand bar's screen state (the same reason /spellbar's changes
   reopen theirs), and the close judgment disarmed the mode after a single
   removal/move. Two-part fix: the close judgment now settles over two
   judgements (disarm only if the bar is gone at both), and a successful
   arrange commit restores the bar screen (`keepScreen`, via TapBarHook's
   tryOpen) while the player is still in their own screen — so a session
   supports repeated removes/moves, as designed (§2.2).

## Tests (all run against the candidate)
Pure suites (sources under `tests/`):

| Suite | Checks |
|---|---|
| BookHoldPlanTest (§6) | 51 |
| ArrangePlanTest (§2) | 25 |
| HoldReadPlanTest (§3) | 11 |
| BarLoadoutNewTest (§5 model) | 35 |
| SpellbookNewTest (§5 groups) | 13 |
| **New total** | **135** |

Pre-existing suites, re-run unchanged against the candidate (regression gates):
core 99, consumer 74, TapWandEchoTest 14 (fake-tap behavior gate),
resource-failure 10 — all pass.

Rig: Paper 26.3-231 + Geyser 2.12.1 build 1277, Moon's lifecycle-closure
fixture, synthetic Bedrock-flagged player — see INSTALL-PACKET.md for the
verdicts and the player-proof list.
