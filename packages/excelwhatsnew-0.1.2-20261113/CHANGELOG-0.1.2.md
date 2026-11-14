# ExcelWhatsNew 0.1.2 (Nov 13, 2026): By date browsing

Approved by Mike Nov 13, 2026 (parent-backlog item: "I want what's new to have everything
organized by date"); supervisor decisions D41: By date reaches ALL entries (flat list stays
at show-count 10); days inside a month newest first; By date button on the list screen AND
on the single-entry view (the compass opens the newest entry directly, confirmed against
source); website month-jump index; day labels carry the weekday.
Base: ExcelWhatsNew 0.1.1, live jar sha256 ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5
(30,471 B). The source tree fetched from wrv-plugins PR #84 rebuilds that jar byte for byte
after LF-normalizing the CRLF line endings the fetch introduced (see BUILD-RECORD.md), so
the diff below is the only change. Live version confirmed 0.1.1 from the Nov 13 boot log
line "ExcelWhatsNew (0.1.1)" and plugin.yml.

## Player-facing
- **By date button.** On `/whatsnew list` (Java: clickable `[By date]` line at the top;
  Bedrock: first button of the list form) and on the single-entry view (Java: `[By date]`
  after `[Older news]`; Bedrock: `By date` after `Older news`, before `Close`).
- **Months, then days, then entries.** By date opens the months list
  (`November 2026 — 72 changes`, `October 2026 — 24 changes`, newest first). A month
  opens its days (`Fri, Nov 13 — 5 changes`, newest first, weekday included). A day
  opens that day's newest entry in the normal entry view; Older/Newer walk only that
  day, and Back returns to the days list. Every by-date level has a Back button to the
  level above; Back is always exactly one level up, never out of What's New.
- **By date reaches everything.** The flat list and Older/Newer paging still stop at
  show-count (live 10), but by-date months/days reach all 96 entries, including
  October. No entry is unreachable in game any more.
- **Chat commands.** `/whatsnew bydate`, `/whatsnew bydate 2026-11`,
  `/whatsnew bydate 2026-11-13`, `/whatsnew bydate 2026-11-13 2` (the day's second
  entry). A date that matches nothing shows the months list again plus one line
  saying so; it never errors. `/whatsnew N` numbering is unchanged, and a number
  past show-count still clamps to the last shown entry, as before.
- **Bedrock list is now a form.** `/whatsnew list` on Bedrock used to print chat text
  (there was no list form in 0.1.1); it now opens a form with By date first, one
  button per entry, and Close, matching Java's tappable rows. If the form cannot
  open, the chat list prints as before, with a typed `By date: /whatsnew bydate` line.
- **Bedrock form button cap: none found.** The Cumulus SimpleForm used here sets no
  limit, so a month's days scroll in one form (at most 31 rows plus Back). No
  pagination was added; the Spell Book 12-a-page shape is a choice, not a form limit.

## Config
- New keys `text.bydate-button` (default `By date`) and `text.back-button` (default
  `Back`). The code falls back to those defaults when the keys are missing, so the
  live config.yml (which does not have them) keeps working untouched. Do not edit the
  live config for this update.
- The default config.yml inside the jar has the new keys (only used if config.yml is
  ever deleted). `show-count` default stays 3 in the jar; the live config's 10 is
  untouched.

## Code
- New `core/ByDate.java`: pure grouping logic (months newest first, days newest
  first, entries newest first within a day) from each entry's `posted` date, with
  the id's YYYY-MM-DD prefix as fallback and a load warning when neither parses.
  Grouping is computed fresh from the loaded changelog each time a by-date view
  opens; no new saved player data, no new config file, no migration.
- `core/Nav.java`: new acts `BYDATE` and `BACK`; the global entry view gains By
  date; new day-bounded button lists (`formDayButtons` / `javaDayButtons`) and the
  typed day hint (both directions by global `/whatsnew N`, plus the Back line).
  Both platforms use the same lists, so Java and Bedrock cannot drift.
- `ExcelWhatsNew.java`: `byDateMonths` / `byDateDays` / `openDay` views (Bedrock
  form, Java clickable chat, Bedrock chat fallback, console text), the `bydate`
  command, the Bedrock list form, and By date warnings in `/whatsnew check`.
- Version 0.1.2 in plugin.yml, config header and class doc. Jar date 2026-11-13T00:00:00Z
  (deterministic build, same toolchain as 0.1.1: JDK 25 javac --release 21, Paper API
  compile libs; bungeecord-chat 1.20-R0.2 stood in for 1.21-R0.2-deprecated on the
  build VM — the jar is byte-identical across those compile-classpath variants).

## Not changed
- Join announcement, calm-moment logic, OK/seen storage (players.yml format), reload/check/status/
  reset/push, changelog.yml format and validation, max-age, show-count handling, `/whatsnew N`
  clamping, the join popup (still at most one per join, still the newest un-OK'd entry).
