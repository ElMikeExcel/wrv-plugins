# ExcelWhatsNew (Wink Reyes Valley)

Server news on join, once per entry, until the player says OK. Bedrock gets a Floodgate form,
Java gets a chat box with clickable buttons. `/whatsnew` (alias `/news`) reads the news any time,
`/whatsnew list` shows the titles, `/whatsnew <n>` opens one entry. Since 0.1.1 the pages go both
ways (Newer news and Older news). Since 0.1.2 there is By date browsing: months, then days,
then that day's entries, reaching ALL entries (the flat list and Older/Newer paging still
stop at show-count, live 10). Since 0.1.3 a day opens its OLDEST entry first and walks
toward the newest, and every day page shows its position (1/5, 2/5, ...) on both platforms.

Build: `./build.sh` (JDK 25 javac --release 21 against the Paper API compile jars; deterministic
jar date). It also runs the unit checks in tests/ (TestMain).

News lives in `plugins/ExcelWhatsNew/changelog.yml`; edit it and run `/whatsnew reload` (no restart).

## By date (0.1.2)

- `/whatsnew bydate` lists the months (`November 2026 — 72 changes`), newest first.
- `/whatsnew bydate 2026-11` lists that month's days (`Fri, Nov 13 — 5 changes`), newest first.
- `/whatsnew bydate 2026-11-13` opens that day's OLDEST entry (0.1.3); Newer news walks
  toward the day's newest entry, Older news steps back, and Back returns to the days
  list. Every day page shows its position: Java gets a `Fri, Nov 13 (1/5)` line above
  the buttons, Bedrock gets `(1/5)` in the form title, and the typed fallback line
  leads with it too. `/whatsnew bydate 2026-11-13 2` opens the day's second-oldest
  entry (walk position 2). A date that matches nothing shows the months list again
  with one line saying so; it never errors.
- The By date button sits at the top of `/whatsnew list` (Java: clickable line; Bedrock:
  first button of the list form) and on the single-entry view (Java: after Older news;
  Bedrock: after Older news, before Close), because the compass opens the newest entry
  directly. Every by-date level has a Back button to the level above.
- Grouping uses each entry's `posted` date; an entry with no usable date is left out of
  By date only (with a load warning in `/whatsnew check`) and stays in the flat list.
- Bedrock forms have no button cap in the Cumulus SimpleForm used here, so a month's days
  (at most 31 rows plus Back) scroll in one form; no pagination. Spell Book paginates at
  12 a page by choice, not by a form limit.
