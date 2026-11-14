# SOURCE — ExcelWhatsNew 0.1.2 (By date browsing)

**Plugin:** ExcelWhatsNew 0.1.2 — By date browsing for What's New
(Mike's request, Nov 13, 2026, ~2:25 PM CST; supervisor decisions
D41 — see BUILD-RECORD.md).

**Source origin:** the ExcelWhatsNew 0.1.1 source tree fetched from
wrv-plugins PR #84 (branch `feature/excelwhatsnew-0.1.1-newer-news`,
the 0.1.1 "Newer news" record PR; that PR was not touched by this
build). Base gate PASSED before any 0.1.2 editing: after
LF-normalizing the CRLF line endings the fetch introduced in the
3 YAML files, the fetched tree rebuilds the recorded 0.1.1 jar
byte for byte — 30,471 B / SHA-256
`ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5`.
Live version 0.1.1 was confirmed two ways (source `plugin.yml` and
the Nov 13, 2026 boot log line "ExcelWhatsNew (0.1.1)"). The 0.1.2
working tree of record is
`~/workspace/whatsnew-bydate-20261113/build-0.1.2/`.

**Version:** 0.1.2 (`plugin.yml` `version: 0.1.2`, main
`com.winkreyes.excelwhatsnew.ExcelWhatsNew`, api-version '1.13';
package `com.winkreyes.excelwhatsnew`, Java 21 — JDK Temurin
25.0.5+8 `javac --release 21` against the Paper API compile jars;
see `build.sh` in the sources zip and BUILD-RECORD.md for the
toolchain notes). No third-party code is copied into the jar:
Floodgate/Geyser are reached reflectively only (softdepend).

**Built jar (NOT inside this records package):**
`build-0.1.2/ExcelWhatsNew-0.1.2.jar` — 40,535 B / SHA-256
`f67e8f717b02000207d4b9e51a4176ef09243b0aa1baabe2affc7cd96406e581`
(deterministic build — two consecutive builds byte-identical; jar
date 2026-11-13T00:00:00Z). Following the standing records pattern
(the ExcelMenu 3.0.1 records package states it explicitly: "No jar
is inside the records package"), the jar is referenced here by
hash only; the jar itself stays in the build folder and its Drive
deposit is a separate supervisor leg at publication time.

**Sources:** `ExcelWhatsNew-0.1.2-sources.zip` in this package —
30,159 B / SHA-256
`4994bb4f9e42ead84d2ea03bc970fa21cdff9217c71f2690b9d132371899a642`.
13 entries, sorted file list, all timestamps pinned to
2026-11-13 00:00:00, ZIP_DEFLATED: `build.sh`, `changelog.yml`,
`config.yml`, `plugin.yml`, the 8 files under `src/` listed below,
and `tests/com/winkreyes/excelwhatsnew/TestMain.java`.
Determinism verified at staging: an independent re-creation of the
zip from the same tree reproduced the hash exactly.

**Install marker:** INSTALLED — see `INSTALLED.txt` in this
package (copy of `build-0.1.2/INSTALLED.txt`): landed Nov 13, 2026
in the 19:33 CST wrv-install-failsafe golden-shovel window (PC
lane); placed jar pull-back EXACT pre- and post-boot at the build
hash above; boot `Done (23.251s)!`, 97 news entries, plugins
86 → 86, SEVERE 0, TPS 20.0/20.0/20.0.

**Build record:** `BUILD-RECORD.md` in this package (feature,
base gate, §9 confirmations, what was built, deviations).
**Changelog:** `CHANGELOG-0.1.2.md` in this package.

**Tests:** 0.1.2 unit tests 128 passed, 0 failed, 9 skipped (the
8 external-jar checks + 1 live-config check, files absent on the
build VM — same class as the base gate); local smoke server
PASSED; player-rig outputs in
`build-0.1.2/test-results-0.1.2/` (see BUILD-RECORD.md).

## Layout (the 8 `src/` files)

- `ExcelWhatsNew.java` — plugin root: load/reload, join popup,
  `/whatsnew` command incl. the 0.1.2 `bydate` paths
  (`byDateMonths` / `byDateDays` / `openDay`), the Bedrock list
  form, tab-complete, check/status/reset/push.
- `Bedrock.java` — the Floodgate/Cumulus form surface (reflection
  only; no Floodgate/Geyser classes compiled in).
- `core/ByDate.java` — NEW in 0.1.2: pure grouping logic —
  months newest first, then days newest first, then entries
  newest first, from each entry's `posted` date with the id's
  YYYY-MM-DD prefix as fallback; day/month/count labels.
- `core/Changelog.java` — changelog.yml loading and validation.
- `core/Entry.java` — one news entry (id, posted date, text).
- `core/Nav.java` — navigation button lists for both platforms;
  0.1.2 adds the `BYDATE` and `BACK` acts and the day-bounded
  button lists / typed day hint.
- `core/Render.java` — entry and list rendering shared by the
  Java chat and Bedrock form surfaces.
- `core/Seen.java` — per-player OK/seen storage (players.yml
  format, unchanged in 0.1.2).

## What 0.1.2 changes vs 0.1.1

By date browsing: a By date button on `/whatsnew list` and on the
single-entry view (both platforms); months → days → that day's
entries, reaching ALL entries (the flat list still stops at
show-count); `/whatsnew bydate [YYYY-MM[-DD] [N]]`; day labels
carry the weekday; new config keys `text.bydate-button` and
`text.back-button` (code falls back to the defaults, live config
untouched); Bedrock `/whatsnew list` is now a form (chat fallback
unchanged). Full account in CHANGELOG-0.1.2.md and
BUILD-RECORD.md; nothing else moves (join popup, OK/seen storage,
reload/check, changelog format, `/whatsnew N` clamping).
