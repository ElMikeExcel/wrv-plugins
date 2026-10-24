# Roll back ExcelTips 0.1.0

1. Reason and countdown first (`restartwarn`), or wait until nobody is online.
2. Live `plugins/`: delete (or rename to `.bak`) `ExcelTips-0.1.0.jar`.
   If an older ExcelTips `.bak` jar was saved at install, put it back.
3. Optional: download `plugins/ExcelTips/` first. `players.yml` remembers who
   said OK to what; leaving the folder in place does no harm, and putting the
   jar back later picks up where it left off.
4. Full restart.

What happens without ExcelTips (tested on the test server):
- No `[Tip]` lines and no `/tips` command. No errors from other plugins.
- ExcelMakersMark goes back to its own built-in tip (`/mark ok` hides it).
- ExcelWandBars, ExcelTrash and every other plugin work exactly as before
  (ExcelWandBars 0.2.1 never relied on ExcelTips).
- Putting the jar back restores everyone's tip progress from `players.yml`.
