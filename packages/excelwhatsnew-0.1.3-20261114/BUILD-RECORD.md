# BUILD RECORD — ExcelWhatsNew 0.1.3 "By-date day walk, oldest first"

- **Feature:** Mike directed Nov 14, 2026, ~12:47 AM CST (backlog P-WHATSNEW-1):
  inside a By-date DAY, entries walk OLDEST → NEWEST (a day tap opens the oldest
  entry of that day, not the newest), and every day-walk page shows its position
  N/M (1/10 … 10/10), both platforms (Java + Bedrock). The flat list's Older/Newer
  top-10 behavior is UNCHANGED.
- **Build date:** Sat Nov 14, 2026. **Build folder:** `~/workspace/whatsnew-013-build-20261114/`
  (new folder; the 0.1.2 folder `~/workspace/whatsnew-bydate-20261113/build-0.1.2/` was not modified).
- **Constraints kept:** build + desk tests + local smoke only. No MANIFEST entry
  filed, no install, no GitHub pushes, no messages or posts, no panel access.
  The supervisor files after the player rig (rig steps owed below).

## Jar

- `ExcelWhatsNew-0.1.3.jar` — sha256
  `9c153d3ab4d5024f3f29d409fe2c657be2ccaa576c2b6743ed2633e9267baafb`, **41,154 B**
- Build fingerprint: deterministic — two consecutive builds byte-identical
  (both `9c153d3a…`; the two unit-test logs are byte-identical too,
  sha256 `c59e64362534ae82c78e57620bcb6a90c7bbd4dd548ad6e24dc212219ad5b3ab`).
- Toolchain: same as 0.1.2 — JDK Temurin 25.0.5+8 at
  `~/workspace/.jdk25/jdk-25.0.5+8`; compile libs `~/workspace/excelshops/build-libs/`;
  `RT=/tmp/rt-wn` symlink farm recreated (bungeecord-chat 1.20-R0.2 +
  jspecify 1.0.0 from `~/workspace/m2-repo/` mapped to the 1.21-R0.2-deprecated
  paths `build.sh` expects). Builds run with env overrides `JDK`, `LIBS`, `RT`.
  `plugin.yml` inside the jar reads version 0.1.3 (verified via unzip).

## Base gate — PASSED (byte-identical rebuild of 0.1.2 before any delta)

- Untouched 0.1.2 source copied to `/tmp/wn-basecheck/` and rebuilt with the
  same overrides: `f67e8f717b02000207d4b9e51a4176ef09243b0aa1baabe2affc7cd96406e581`,
  **40,535 B — byte-identical (`cmp`) to the shipped 0.1.2 jar.** No CRLF in the
  0.1.2 tree (checked), so no normalization was needed this time.
- Base unit tests on the rebuild: **124 passed, 4 failed, 9 skipped**
  (0.1.2 record: 128/0/9). All 4 failures are the live-changelog fixture checks
  and are explained by fixture drift, not code: the pipeline changelog grew
  96 → 100 entries after the 0.1.2 build (Nov 13 day 5 → 9 entries; file now
  sha256 `0bfcb17f594ba3f8d43986e3ef0be22ed801a7821312f3e84774b431a0e3d2cc`,
  86,183 B, `~/workspace/whatsnew-pipeline/build/changelog.yml`). The failing
  checks are exactly: "live changelog has 96 entries", "groups to Nov 72 /
  Oct 24", "live Nov 13 opens the section-colours entry first", "live Nov 13
  oldest is the skillspanel entry". Log: `test-results-0.1.3/base-0.1.2-rebuild.out`
  (sha256 `3b05ad89812b2b74fbd0e228c2cc30c65472ff37401c736a9a2d6e74aa9b0b08`).
  Consequence for 0.1.3: the live-changelog test block now derives its
  expectations from the file itself instead of pinning counts (see Tests).

## What was built (0.1.3 delta vs 0.1.2)

- **`core/ByDate.java`**: at grouping time each day's entry list is reversed, so
  `Day.entries` is OLDEST first. Months newest first and days newest first are
  unchanged; the date rules (posted, else id YYYY-MM-DD prefix, else left out of
  By date with a warning) are unchanged; grouping copies before reversing, so
  the loaded changelog order (the flat list) is never mutated (test-pinned).
- **`core/Nav.java`**: new day-walk helpers — `dayHasNewer(idx, size)` =
  `idx + 1 < size`, `dayHasOlder(idx)` = `idx > 0`, `dayNewerWalkNumber(idx)` =
  `idx + 2`, `dayOlderWalkNumber(idx)` = `idx`, `dayPosition(idx, size)` =
  `"N/M"` (1-based from the oldest). `formDayButtons` / `javaDayButtons` now
  bound Newer/Older by the walk: at the day's oldest entry there is no Older
  button; at its newest there is no Newer button; button slot order
  (OK, Newer, Older, Back, Close) is unchanged. `typedDayHint` leads with
  `dayLabel + " " + N/M + "."`, picks Newer/Older by the walk, and still prints
  global `/whatsnew N` numbers (a day's entries are consecutive in the global
  list, so the newer neighbour is globalIdx-1 and the older is globalIdx+1,
  exactly as the formulas already said). Global Nav (`formButtons`,
  `javaButtons`, `typedHint`, `listHint`, numbers) is untouched.
- **`core/Render.java`**: new `formDayTitle(e, idx, size)` = form title +
  `" (N/M)"` (Bedrock), and `dayPositionLine(day, idx, size)` =
  `"&7" + dayLabel + " (N/M)"` (Java chat + console).
- **`ExcelWhatsNew.java`**: `openDay` semantics — idxInDay 0 is now the day's
  OLDEST entry (the days-list tap and `/whatsnew bydate <day>` already pass 0 /
  N-1, so no call-site change was needed beyond meaning); the Bedrock day form
  opens with `formDayTitle` and its callbacks step Newer → `fi + 1`, Older →
  `fi - 1`; the Java day view sends `dayPositionLine` above the button row and
  its buttons run `/whatsnew bydate <day> <walk number>` via the new Nav
  helpers; the console day view prints the position line before the entry.
  `/whatsnew bydate <day> N` therefore names walk positions (N=1 = oldest;
  past-the-end still clamps, now to the day's newest).
- **`plugin.yml`**: version 0.1.3. **`config.yml`**: header 0.1.3 + a comment
  noting the walk (no new keys; live config untouched by this update).
  **`build.sh`**: jar name 0.1.3, jar date 2026-11-14T00:00:00Z, OUT default now
  the build folder itself. **`README.md`** updated (By date section).
  **New `CHANGELOG-0.1.3.md`** following the 0.1.2 format.

## Tests — 0.1.3 unit tests: 149 passed, 0 failed, 9 skipped

Full log: `test-results-0.1.3/unit-tests-0.1.3.out` (identical for both builds).
Skips are the same class as 0.1.2: 8 external-jar checks + 1 live-config
readback, files absent on this VM. Coverage of the three required areas:

- **Day walk order oldest-first:** synthetic live-shape fixture — Oct 26 day
  list starts at e10 (its oldest) and ends at e1; Nov 13 opens e5 (oldest),
  ends e1; grouping leaves the input list newest-first (flat list unmutated).
  Against the real pipeline changelog (100 entries at build time): every dated
  entry grouped, months newest first, and the Nov 13 walk opens the entry that
  is LAST in file order for that day and ends at the FIRST — derived from the
  file, so the check survives changelog growth.
- **N/M numbering on every day-walk page:** `dayPosition` labels (1/10, 10/10,
  5/5, 1/1); Bedrock `formDayTitle` carries "(1/5)" / "(10/10)"; Java
  `dayPositionLine` is "&7Fri, Nov 13 (1/5)" / "&7Mon, Oct 26 (10/10)"; the
  typed fallback leads with the position and shows 1/5 at walk start, 5/5 at
  walk end; source checks pin that the plugin actually opens the day form with
  the position title, sends the position line on Java, and steps the walk
  +1/-1 from the form callbacks.
- **Flat list unchanged:** all 0.1.1/0.1.2 global checks carried over verbatim
  (global form/java button orders, callback mapping, Java/Bedrock parity loops,
  typed hints, list hint, `/whatsnew N` numbers, Seen logic, validation) and
  passing; plus the new no-mutation check above.

## Local smoke server — PASSED (desk harness; player rig still owed)

- Paper 26.2-123 (from `~/workspace/excelshops/smoke-runtime/`, staged in
  `/tmp/wn-smoke3/`), port 25599, online-mode=false, 0.1.3 jar + the current
  pipeline changelog (100 entries) + config show-count 10.
- Boot: `Done (43.761s)`; enable line: `ExcelWhatsNew 0.1.3: 100 news entries
  (newest 2026-11-13-excelwhatsnew-bydate), 0 players on file, 65 warnings
  (/whatsnew check)` — the 65 are the pre-existing changelog long-line class,
  zero By-date warnings.
- Console commands verified against the live 100-entry changelog:
  `bydate` → months; `bydate 2026-11` → days newest first with weekday labels;
  `bydate 2026-11-13` → position line `Fri, Nov 13 (1/9)` + "New: one Skills
  panel for skills and magic" (the day's OLDEST entry, last in file order);
  `bydate 2026-11-13 5` → `(5/9)` + the section-colours entry (file-order #5,
  the chronological middle); `bydate 2026-11-13 99` → clamped to `(9/9)` +
  "New: browse old news by date" (the day's NEWEST entry); `bydate 2026-10-26`
  → `Mon, Oct 26 (1/10)`; `bydate 2026-10-26 10` → `(10/10)`; `list` → flat
  list newest first, unchanged; `check` prints the warnings.
- Shutdown: clean `stop`. Log: **SEVERE 0**; the only ERROR lines are Paper's
  version fetcher + authlib key fetch (sandbox network noise, same class as the
  0.1.2 smoke, unrelated to the plugin). Full log:
  `test-results-0.1.3/smoke-server-0.1.3.log`
  (sha256 `5c9e55ac1f6e4378c31771910e3267627d642b64de9c0b85aac4ad52a7fee90d`).

## Rig steps OWED (later leg — supervisor files after rig; do not install before)

1. **Player rig on the PC test-server mirror** (Java mineflayer + Bedrock
   protocol client, per the 0.1.2 rig pattern): install the 0.1.3 jar on the
   mirror with the then-current live changelog + live config (show-count 10),
   reset the test players, and run `rig-tests/javabydate.js` +
   `rig-tests/bedrockbydate.js` (both copied into this folder and passing
   `node --check`; their headers carry the 0.1.3 expectation deltas).
2. **Re-pin rig expectations to the rig-day snapshot first**: the scripts were
   written for 0.1.2 against the Nov 13 snapshot (96 entries, Nov 13 = 5,
   newest = section-colours) — counts and ids must be refreshed, and the day
   walk flipped: day tap opens the day's OLDEST entry; Java text carries the
   `(1/M)` position line; Bedrock form titles carry `(1/M)` → `(M/M)`; the
   walk advances via Newer news (M-1 steps); at walk start there is no Older,
   at walk end no Newer; the Bedrock pending-OK button set `[OK, Older, Back]`
   now appears at the walk END (the day's newest entry is the pending global
   newest), not at the day tap.
3. Rig must eyeball one full 10-entry day (Oct 26 shape) on both platforms:
   positions 1/10 … 10/10 in order, Back chain day → days → months → list
   intact, and the flat list + `/whatsnew 99` clamp regression checks in the
   scripts passing unchanged.

## Changelog entry draft (What's New registry record — for the supervisor to file at install time)

Re-date rule (per the 0.1.2 surfaces discipline): if the install lands on a
later date than the draft below, re-date id + posted + game date + site group
together before filing; never rename after filing. File at the TOP of the
registry at install time, never before.

```yaml
  - id: 2026-11-14-excelwhatsnew-day-walk
    date: Nov 14
    posted: 2026-11-14
    title: "Update: By date now reads a day from the beginning"
    changes:
      - "In What's New, By date now opens a day at its first change of the day and reads forward: Newer news moves to the next change, and every page shows where you are, like 1/5."
      - "Before, a day opened at its newest change and read backwards. Months, days and the recent news list work the same as before."
```

## Same-pass surfaces — state for the supervisor

- What's New (in-game changelog): the draft record above is the entry; the
  in-game `changelog.yml` placement is a live-edit at filing time.
- What's New (website): follows from the same registry record through the
  whatsnew pipeline at filing; no generator patch needed for this change
  (the 0.1.2 month-jump patch is a separate, already-staged item).
- Guide: no new Q&A needed — the existing "how do I see old news?" coverage
  describes By date month → day browsing, which does not change shape; the
  day-reading direction is covered by the What's New entry itself.
- Broadcaster tips / menus: no tip or menu names the day-walk direction
  (checked against the 0.1.2 surfaces pass); no change.
