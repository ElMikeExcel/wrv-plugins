ExcelAcrobatics 1.1 (fix; installed live at 7:57 PM CDT Oct 16, 2026. In-game UNTESTED. Java and Bedrock UNCONFIRMED.)
Root cause 1.0: McMMOAward probed ExperienceAPI.addExperience(...), which does not exist in mcMMO (real: addModifiedXP/addRawXP/addXP). Probe failed, awards off, logged lazily so startup showed "enabled".
Also: puddle landing could lose the fall peak; now checks destination block first and keeps highest airborne Y.
