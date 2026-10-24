# ExcelWandBars 0.2.2: Spell Book screen for Bedrock, rank progress line, 6-row spell list

STAGED, NOT INSTALLED. Not in the Oct 25 2 AM batch unless Mike approves it. Built and tested on the box test rig only (Oct 24, 2026). No live edits.
Builds on ExcelWandBars 0.2.1 (draft PR #62). Magic (elmakers) 11.2.5; nothing touches EliteMobs (MagmaGuy).

* Magic `wands.yml` (live copy + the 5 ExcelMagicGate stage copies): `inventory_rows: 6` under
  `base_wand`, so Geyser no longer pads Magic's 45-slot spell chest with a barrier row.
* Bedrock + Classic style: crouch + press and hold with a wand opens a Spell Book form
  (each spell: name, rank, "Use this spell") instead of Magic's chest of vanilla icons.
  A rank header sits on top. Java keeps Magic's chest (now 6 rows).
* After a cast: an action-bar progress line, only where casts really rank a spell up
  (wands with a Magic path). WRV rank wands have no path, so there the screen says
  "Next rank: comes with Student rank" and no cast count is shown (see CHANGELOG).
* First-use Spell Book tip that repeats until OK. Handed to ExcelTips (draft PR #64)
  when it is installed; its own copy otherwise; never both.
* Bedrock wording everywhere in ExcelWandBars: tap / press and hold, never "click".

Files: CHANGELOG.md, INSTALL.md, ROLLBACK.md, TEST-RESULTS.md, TIPS-INTEGRATION.md (ExcelTips hand-over), SHA256SUMS (jar hash; the jar
itself is not in the repo), files/ (the 6 wands.yml), src/ExcelWandBars (source + unit tests),
tests/ (rig bot test and round drivers).
