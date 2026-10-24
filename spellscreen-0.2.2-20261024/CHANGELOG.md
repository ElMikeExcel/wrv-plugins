# Changelog: ExcelWandBars 0.2.1 -> 0.2.2 (Spell Book screen, progress line, Bedrock wording) + Magic wands.yml 6 rows

Built Sat Oct 24, 2026 (CDT) on the box TEST server only (/workspace/ss022-rig, port 25941).
NOT installed. No live edits. Approved by Mike at 1:47 PM CDT Oct 24 (build + test only).
Built on top of ExcelWandBars 0.2.1 from the approved spellbook batch (jar sha d25cf1e8...ecd26),
so the two never conflict: 0.2.2 contains every 0.2.1 change and replaces the 0.2.1 jar.
EliteMobs (MagmaGuy) is not touched. Magic (elmakers) gets one config line per wands.yml.

Jar sha256 and file hashes: see SHA256SUMS.

## 1. Magic (elmakers) wands.yml: `inventory_rows: 6` (Magic + all 5 ExcelMagicGate stage copies)
* What was wrong: Magic's chest-mode spell list is 5 rows (45 slots). Bedrock has no 45-slot
  chest, so Geyser shows a 54-slot double chest and fills the last row with 9 red barrier blocks.
* Change: one line, `inventory_rows: 6`, added under `base_wand:` (plus a 5-line comment) in
  - plugins/Magic/wands.yml
  - plugins/ExcelMagicGate/stages/stage1..stage5/wands.yml (so a seal stage apply does not undo it)
* Nothing else in those files changes (checked with diff against the live classic set).
* Spell slots 0 to 44 keep their saved places. Row 6 (slots 45 to 53) starts empty. Tested.
* Java players now see a 6-row spell list. Bedrock players on Classic get the Spell Book form
  instead (section 2), but if the form can't be sent, the chest they fall back to has no barriers.

## 2. Bedrock Spell Book form (Classic / chest mode)
* On Bedrock with Classic controls, crouch + press and hold (controller: crouch + LT) with a wand
  used to open Magic's chest of vanilla icons (melon = Mend, apple = Heal, chorus fruit = Boon,
  bread = Feed, snowball = Extinguish, bottle = Aqualung). It now opens the **Spell Book** form.
* The form lists each spell as a button with its name and rank, e.g. `Boon (Rank 2 of 4)`; the
  spell the wand is using now is marked `(in use)`.
* Tapping a spell opens its page (what it does, cost, cooldown, range, rank, next rank) with
  **Use this spell**, **Back to Spell Book** and **Close**. Use this spell makes it the wand's
  active spell and the action bar says `Now using: Boon`. The active spell's page says
  "You are using this spell now" with no Use button.
* A `Wand controls: Classic (change)` button opens the /wandstyle chooser.
* One press = one form (600 ms debounce, `spell-screen.debounce-ms`).
* `/spellbook` (= `/spellinfo`) on Bedrock sends the same form.
* Java is unchanged: sneak + right click still opens Magic's chest (now 6 rows).
* Sneak bars (Bedrock opt-in) is unchanged: press and hold still opens the spell bar.
* If the form can't be sent (no Geyser form connection), Magic's chest opens as before.
* Config `spell-screen.bedrock-form: true` (set false to turn the form off).

## 3. Rank header and progress line
* The Spell Book (Bedrock form and Java `/spellbook` chat) now starts with a rank header:
  `Your rank: Beginner. Next rank: Student.` and the rule line
  `Your spells move up a rank when you reach Student rank.` Top rank shows
  `Your rank: Wizard (top rank).`
* Action-bar progress line after a counted cast, using the same data as `/spellinfo`
  (Magic's spell template and the wand's cast count): `Boon: 12/50 casts to Rank 3`, or with a
  rank lock `Boon: 12/50 casts to Rank 3 (opens at Student rank)`. A rank-up shows
  `Boon reached Rank 3!`. At most one line per 4 s (`progress-line.min-gap-seconds`). No line
  during an ExcelAttunement stance countdown (`skip-during-stance`). The XP bar (mana) is untouched.
* **Important finding (checked in Magic 11.2.5 code and on the rig):** Magic only ranks a spell up
  by casts on a wand that has a Magic *path*. WRV's rank wands (beginner, student, apprentice,
  master) have no path and `default_wand_path` is empty, so on WRV casts never rank a spell up.
  Spell ranks follow the wand rank. So with today's live Magic config:
  - no cast-count progress line shows (it would promise a rank that never comes),
  - the spell page and `/spellinfo` say `Next rank: comes with Student rank` instead of a cast count,
  - the line works with no change if wands ever get a path (tested on the rig with a path).
  This is a change from 0.2.1, whose `/spellinfo` said `Next rank: cast it 50 times ...`, which never leads to a rank on WRV.
* No seal mechanics are mentioned anywhere (learn-by-dying rule).

## 4. Spell Book first-use tip (repeats until OK)
* Java: `[Tip] Spell Book: type /spellbook to see your rank and what each spell's next rank needs.`
  plus a clickable `[OK]`. Shows when the spell chest opens, the book opens or a progress line shows.
* Bedrock: the tip sits at the top of the Spell Book form with an `OK, got it! (hide the tip)` button.
  Wording uses tap / press and hold only.
* Repeats until OK, at most once per 60 s (`spell-tip.show-gap-seconds`, `max-shows: 0` = until OK).
  `/spellbook ok` also hides it. OKs are saved in plugins/ExcelWandBars/spellscreen-tips.yml (a new,
  separate file, so a rollback to 0.2.1 just ignores it).
* Never two tips at once: right after the wand tip shows, the Spell Book tip waits for the next trigger.
* **ExcelTips hand-over:** when ExcelTips (0.1.0, draft PR wrv-plugins #64) is running, 0.2.2
  registers the tip with ExcelTips as feature `spellscreen` (ExcelTipsAPI.register / tip /
  acknowledge / isFinished, by reflection, no compile dependency) and does not show its own copy,
  so a player never gets it twice. `/spellbook ok` and the form OK also acknowledge it in ExcelTips.
  Without ExcelTips, 0.2.2 shows its own tip. ExcelTips is never required.

## 5. Bedrock wording: tap / press and hold, never "click" (added 2:42 PM CDT)
* ExcelWandBars' own wand tip (0.2.1, first 15 wand uses until OK) said
  `Classic: right-click to pick the next spell, left-click to cast.` to everyone. Bedrock players now get
  `Classic: tap to cast, press and hold to pick the next spell (controller: RT casts, LT picks).`
  in chat and in the pop-up. Java wording is unchanged.
* `/wandstyle` replies, the Wand controls form text and its chat copy are now worded per platform. Bedrock:
  - Classic: `Press and hold (controller: LT) to pick the next spell.` / `Crouch and press and hold to open
    your Spell Book.` / `Tap (controller: RT) to cast.`
  - Sneak bars: `Press and hold (controller: LT) to open your spell bar.` / `Tap a spell on the bar to cast it.` /
    `Sneak once to flip to the next bar (1, 2, 3, then back to 1). Press and hold again to close it.`
  - Chat copies: `Tap one, or type /wandstyle ...`, `Spell Book (tap a spell)`, `[Wand controls: Classic - tap to change]`.
* New pure class core/ControlsText.java with unit tests that fail if a Bedrock line contains "click".

## 6. Admin / test commands (op only, `excelwandbars.admin`)
* New: `/wandbars spelltip <player> [reset]`, `progress <player>`, `use <player> <spell>`,
  `lastform <player>`, `formtap <player> <n>`. `simbedrock` (from 0.2.1) is memory-only and clears on quit.

## 7. Config (plugins/ExcelWandBars/config.yml)
* New sections `spell-screen`, `progress-line`, `spell-tip`. All have built-in defaults, so the live
  config file can stay as it is (tested: missing sections = defaults).

## 8. ExcelTips hand-over note (TIPS-INTEGRATION.md, added 3:40 PM CDT)
* No tips.yml change is needed for 0.2.2: ExcelTips 0.1.0 has no wand tip, and the Spell Book tip
  registers itself as `spellscreen` with tap / press and hold wording on Bedrock.
* Suggested (not device-tested) tips.yml rewording for one Bedrock vocabulary: the `compass-menu`
  Bedrock line "Tap with it" -> "Press and hold with it (controller: LT)". Details in TIPS-INTEGRATION.md.
  Nothing in ExcelTips was changed by this build.

## Status
* Staged only. NOT part of the Oct 25 2 AM batch unless Mike approves it separately.

## Files
* ExcelWandBars-0.2.2.jar (replaces ExcelWandBars-0.2.1.jar, or 0.2.0 if the 0.2.1 batch is not in yet)
* plugins/Magic/wands.yml, plugins/ExcelMagicGate/stages/stage1..5/wands.yml

## Known limits / not verified
* Real Geyser + real Bedrock device not tested (the box rig has no Bedrock client). Bedrock paths
  were tested with the rig's simulated-Bedrock player and the real Cumulus form classes from the
  floodgate jar. A real-device check is still owed after install.
* Stance skip of the progress line: covered by unit tests; the rig bot could not start a stance.
* Never two tips at once: on Java, the Spell Book tip waits while ExcelWandBars' own wand tip
  is still repeating (until that one gets OK or its 15 uses run out). Same rule with ExcelTips.
* With ExcelTips installed, a real Bedrock player sees the Spell Book tip inside the form (with
  its OK button, which also tells ExcelTips). ExcelTips only sends its chat copy when there is no
  form open (Java chest, `/spellbook` in chat, or a progress line). On the rig the simulated Bedrock player gets ExcelTips'
  Java wording, because ExcelTips asks floodgate, which does not know the simulated player.
* Rejoining within about a minute can be refused by GriefPrevention ("You must wait N seconds
  before logging-in again"). Seen on the rig after `magic load`; it is not from ExcelWandBars.
