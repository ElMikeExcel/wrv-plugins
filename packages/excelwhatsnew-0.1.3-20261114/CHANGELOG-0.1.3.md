# ExcelWhatsNew 0.1.3 (Nov 14, 2026): By-date day walk, oldest first + position

Directed by Mike Nov 14, 2026, ~12:47 AM CST (backlog P-WHATSNEW-1): inside a By-date
day, entries walk OLDEST to NEWEST (a day tap opens the oldest entry, not the newest)
and every day-walk page shows its position N/M (1/10 ... 10/10), both platforms. The
flat list's Older/Newer top-10 behavior is unchanged.
Base: ExcelWhatsNew 0.1.2, live jar sha256 f67e8f717b02000207d4b9e51a4176ef09243b0aa1baabe2affc7cd96406e581
(40,535 B). The 0.1.2 source tree in whatsnew-bydate-20261113/build-0.1.2 rebuilds that
jar byte for byte on this VM before any 0.1.3 edit (base gate, see BUILD-RECORD.md),
so the diff below is the only change.

## Player-facing
- **A day opens at its beginning.** Tapping a day in By date (or typing
  `/whatsnew bydate 2026-11-13`) now opens that day's OLDEST entry instead of its
  newest. "Newer news" walks forward through the day toward the newest entry;
  "Older news" steps back. Back still returns to the days list.
- **Every day page shows its place.** Position N/M (1 = the day's oldest entry,
  M = the day's newest): Java shows a `Fri, Nov 13 (1/5)` line above the buttons;
  Bedrock shows `(1/5)` in the form title; the Bedrock typed fallback line leads
  with the same position; the console day view prints it too.
- **Day command numbers follow the walk.** `/whatsnew bydate 2026-11-13 2` opens
  the day's second-oldest entry (walk position 2); a number past the day's count
  still clamps to the last entry of the day (now its newest), as before.
- **Nothing else moves.** Months and days still list newest first; the flat list,
  `/whatsnew N` numbering, Older/Newer paging over the recent entries, the join
  announcement, and OK/seen behavior are all exactly as in 0.1.2.

## Config
- No new keys, no config change needed. The default config.yml inside the jar only
  gains a comment noting the 0.1.3 walk; the live config keeps working untouched.

## Code
- `core/ByDate.java`: a day's entry list is reversed at grouping time, so
  `Day.entries` is oldest first. Months/days ordering and the date rules
  (posted, else id prefix, else left out with a warning) are unchanged, and the
  grouping never mutates the loaded changelog order.
- `core/Nav.java`: new day-walk helpers (`dayHasNewer` / `dayHasOlder`,
  `dayNewerWalkNumber` / `dayOlderWalkNumber`, `dayPosition`). `formDayButtons` /
  `javaDayButtons` now bound Newer/Older by the walk (at the oldest entry there is
  no Older button; at the newest there is no Newer button). `typedDayHint` leads
  with the day label + N/M and picks its directions by the walk; the global
  `/whatsnew N` numbers it prints are unchanged in meaning. Global Nav is untouched.
- `core/Render.java`: new `formDayTitle` (form title + "(N/M)") and
  `dayPositionLine` (chat/console position line).
- `ExcelWhatsNew.java`: day form opens with the position title and its Newer/Older
  callbacks step +1/-1 through the walk; the Java day view sends the position line
  and its buttons run the walk numbers; the console day view prints the position
  line. Version 0.1.3 in plugin.yml, config header and class doc. Jar date
  2026-11-14T00:00:00Z (deterministic build, same toolchain as 0.1.2).

## Not changed
- Flat list and its Older/Newer paging (show-count, live 10), `/whatsnew N`
  clamping, join announcement, calm-moment logic, OK/seen storage (players.yml
  format), reload/check/status/reset/push, changelog.yml format and validation,
  max-age, months/days list order, By date button placement.
