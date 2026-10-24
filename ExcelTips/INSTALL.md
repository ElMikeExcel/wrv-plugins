# Install ExcelTips 0.1.0 on the live server

NOT done. Needs Mike's go in the moment. Test server only so far.

Needs: Paper 26.3, Floodgate (for the Bedrock OK pop-up; without it Bedrock
players get the chat tip and `/tips ok`). Works with or without ExcelTrash
0.2.0, ExcelWandBars 0.2.1, ExcelMakersMark. Edits no other plugin's files.

1. Check the jar: `sha256sum ExcelTips-0.1.0.jar` must equal the value in
   `SHA256SUMS`.
2. Pick a time nobody is online (or the 2 AM window). Give the reason and a
   countdown first (`restartwarn`). If it is inside the 2-5 AM CDT window, set
   the server back to Offline afterwards for the 5 AM job.
3. Live `plugins/`: if an `ExcelTips-*.jar` is already there, download it and
   keep it as `.bak` (never overwrite an existing backup). Upload
   `ExcelTips-0.1.0.jar`. Do NOT upload a `plugins/ExcelTips/` folder; the
   plugin writes its own `config.yml` and `tips.yml` on first start.
4. Full restart (not `/reload`). It can share the restart with ExcelTrash
   0.2.0 and the spellbook batch; load order does not matter.
5. Console checks:
   - Start log: `ExcelTips 0.1.0: 22 tips (... switched on), N players on file.`
   - `/tips check`. Expected notes only:
     `trash: (off while ExcelTrash>=0.2.0)` once ExcelTrash 0.2.0 is in, or
     `trash-bin: needs ExcelTrash>=0.2.0` while 0.1.0 is still live;
     `autosort` / `makersmark` off if those plugins are not installed.
     Anything else: fix in `plugins/ExcelTips/tips.yml`, then `/tips reload`.
   - No `wandstyle` or `magic-wand` line (the wand tip belongs to ExcelWandBars).
6. Smoke test with your own account: `/jobs` shows the jobs tip with a
   clickable [OK] on Java; on Bedrock a chat tip, then an OK pop-up about 8 s
   later at a calm moment (never in a fight, a menu, or the first 45 s after
   joining). `/tips ok` hides it. Then `/tips reset <you>` so you see the tips
   like everyone else.
7. Use a wand once: only the ExcelWandBars wand tip shows (no second one).

Shipped config timings are the real ones (pop-up 45 s after join, 5 min
between pop-ups, only after moving). The test server ran looser ones.
