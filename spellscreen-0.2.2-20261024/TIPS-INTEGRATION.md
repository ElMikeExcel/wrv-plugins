# ExcelWandBars 0.2.2 + ExcelTips 0.1.0: hand-over note

Written 3:40 PM CDT Oct 24, 2026. Nothing in /workspace/batch-next/tips/ was edited.

## Who shows which tip (never both)
* Wand tip (`wandstyle`): ExcelWandBars' own TipGate, first 15 wand uses until OK.
  ExcelTips 0.1.0 has no wand tip (removed on purpose). Keep it that way.
* Spell Book tip (`spellscreen`, new in 0.2.2): when ExcelTips is running, 0.2.2 registers it
  through `ExcelTipsAPI.register(...)` and ExcelTips shows it; ExcelWandBars then does not show
  its own copy. Without ExcelTips, ExcelWandBars shows it itself. Repeats until OK either way.

## Wording fixed in 0.2.2 (inside ExcelWandBars, not tips.yml)
* Wand tip, Bedrock: was `Classic: right-click to pick the next spell, left-click to cast.`
  now `Classic: tap to cast, press and hold to pick the next spell (controller: RT casts, LT picks).`
  Java keeps the right-click / left-click line.
* Spell Book tip lines sent to ExcelTips (Bedrock list):
  `Spell Book: crouch and press and hold with your wand (controller: crouch and press LT) to open it.`
  `Tap a spell to read it, then tap Use this spell to put it on your wand.`
  Java list: `Spell Book: type /spellbook to see your rank and what each spell's next rank needs.`
  No "click" in any Bedrock line (unit tested).

## tips.yml: what should change
* No `wandstyle`, `magic-wand` or `spellscreen` entry needed. Do not add one (an entry there
  would win over the registered 0.2.2 text and bring back old wording).
* The comment at tips.yml line 57-63 is already correct for 0.2.2.
* Suggested (not required, not device-tested), for one consistent Bedrock vocabulary with the wand:
  - `compass-menu` bedrock line 1: `Tap with it to open ...` -> 
    `Your compass is the Server Menu. Press and hold with it (controller: LT) to open Spawn, the shop, Party and more.`
    Reason: on Bedrock touch, using an item in the air is press and hold; a tap in the air is a swing
    (that is the same mapping 0.2.2 uses for the wand). Check on a real phone before changing.
  - `claims` bedrock line 1 is fine on touch (tapping a block uses it). Optional controller hint:
    `... tap (controller: LT) one corner of your land, then the opposite corner.`
* ExcelTips' own Bedrock OK hint (`Tap OK on the pop-up, or type /tips ok, to hide this tip.`) is fine.
