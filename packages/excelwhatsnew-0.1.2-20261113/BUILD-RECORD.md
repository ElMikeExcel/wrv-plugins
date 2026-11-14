# BUILD RECORD — ExcelWhatsNew 0.1.2 "By date"

- **Feature:** By date browsing for What's New (Mike's request, Nov 13, 2026,
  ~2:25 PM CST): "I want what's new to have everything organized by date.
  People can click on a by date button and see what news or changes happened
  per day. If someone wants to skip to the beginning of the month they can."
- **Build date:** Fri Nov 13, 2026. **Build folder:** `~/workspace/whatsnew-bydate-20261113/build-0.1.2/`
- **Design:** `~/workspace/whatsnew-bydate-20261113/DESIGN.md` (all 12 sections).
- **Decisions built to:** supervisor D41 (decision-log.md; Mike vetoes by reply).
- **Constraints kept:** build + rig + packet only. No install, no MANIFEST edit,
  no GitHub pushes, no messages or posts, no panel access. The source PR #84
  (branch `feature/excelwhatsnew-0.1.1-newer-news`, OPEN draft) was not touched.

## Jar

- `ExcelWhatsNew-0.1.2.jar` — sha256
  `f67e8f717b02000207d4b9e51a4176ef09243b0aa1baabe2affc7cd96406e581`, **40,535 B**
- Build fingerprint: deterministic — two consecutive builds byte-identical.
- Toolchain: JDK Temurin 25.0.5+8 at `~/workspace/.jdk25/jdk-25.0.5+8`;
  compile libs `~/workspace/excelshops/build-libs/` (paper-api.jar etc.);
  bungeecord-chat 1.20-R0.2 + jspecify 1.0.0 from `~/workspace/m2-repo/`.
  `build.sh` box defaults do not exist on this VM; builds used env overrides
  `JDK`, `LIBS`, `RT=/tmp/rt-wn` (symlink farm mapping the expected
  1.21-R0.2-deprecated paths to the 1.20 jar + jspecify). No `java` on PATH;
  use the full JDK path. `build.sh` needed `chmod +x` after copy.

## Base gate — PASSED (byte-identical rebuild of 0.1.1)

- Fetched source tree has CRLF line endings on all files (PR fetch artifact).
  First rebuild: `72c5553c...`, 30,499 B — NOT identical (28 B delta; invariant
  across 3 paper-api copies and bungeecord 1.16/1.20/no-jspecify variants,
  proving the delta was not the compile classpath).
- Root cause: CRLF in the 3 YAML files copied verbatim into the jar. After
  LF-normalizing `plugin.yml`, `config.yml`, `changelog.yml`, the rebuild is
  `ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5`,
  **30,471 B — byte-identical to the recorded 0.1.1 jar**.
- Base unit tests (LF tree, TestMain patched to skip absent external jars):
  **76 passed, 0 failed** (8 external-jar checks + 2 live-config checks
  skipped — `/workspace/batch-next/...` jars absent on this VM; 86 − 10 = 76).
- Base-gate work dir: `~/workspace/whatsnew-bydate-20261113/build-0.1.2-basecheck/`
  (rebuilt jar `/tmp/bc-lf.jar`).

## §9 confirmations (all resolved against the 0.1.1 source before editing)

- **B1 — CONFIRMED: the compass opens the newest entry directly.**
  `onCommand` case `""` → `open(p, 0, false)`; the door action is bare
  `whatsnew`. Therefore per D41-3 the entry-view By date button IS required
  and was built (list screen + single-entry view).
- **B2 — No cap in the Cumulus SimpleForm path.** `Bedrock.java` builds buttons
  in a loop with no limit. Spell Book precedent: `BEDROCK_PER_PAGE = 12` in
  `~/workspace/wandbars-ux-build/work/src/com/winkreyes/excelwandbars/core/SpellbookText.java`
  line 19 — a UX choice, not a protocol cap. Days list scrolls in one form
  (max 31 rows + Back); no pagination implemented.
- **B3 — Live version 0.1.1**, confirmed two ways: `plugin.yml` in the source
  tree AND the Nov 13 boot log line "ExcelWhatsNew (0.1.1)" (panel latest.log
  captures under `~/workspace/browser_downloads/`). Next version 0.1.2.
- **B4 — `/whatsnew N` beyond show-count clamps:** `if (n > shown()) n = shown();
  open(p, n-1, false)` — `/whatsnew 11` opens entry 10. Preserved unchanged.
- **B5 — Flat-list anchor: chat rows on BOTH platforms.** `list()` sends chat
  lines; Java rows get clickable [Read]; Bedrock gets plain lines + typed hint.
  **There is NO Bedrock list form in 0.1.1** (design §3 assumed one existed).
  Recorded deviation/decision: built a Bedrock list form (SimpleForm: By date
  first, one button per entry, Close; falls back to the chat list if the form
  cannot open) so Bedrock gets the tappable By date button the design requires
  on Level 0 and tappable entry rows matching Java. Revert = delete the form
  branch in `list()`.

## What was built (0.1.2)

- **New `src/com/winkreyes/excelwhatsnew/core/ByDate.java`**: pure grouping
  logic — `group(entries)` → months newest first → days newest first →
  entries newest first; `dateOf()` = posted, else id YYYY-MM-DD prefix, else
  null; warnings for posted/id disagreement ("posted wins") and unusable date
  ("left out of By date"); labels `dayLabel` = "Fri, Nov 13" (`EEE, MMM d`,
  Locale.ENGLISH), `monthLabel` = "November 2026", `countLabel` = "1 change" /
  "N changes".
- **`core/Nav.java` rewritten**: new acts `BYDATE`, `BACK`. Global
  `formButtons`/`javaButtons` now include BYDATE after OLDER (before CLOSE on
  Bedrock). New `formDayButtons`/`javaDayButtons`; new `typedDayHint` — Newer/
  Older by global `/whatsnew N` + `Back to Fri, Nov 13: /whatsnew bydate
  2026-11-13.` line; always non-empty (Back part always present). `typedHint`
  unchanged.
- **`ExcelWhatsNew.java` rewritten (0.1.2)**: new settings
  `text.bydate-button` (default "By date"), `text.back-button` (default
  "Back"); `byDateWarnings` logged at load and shown in `/whatsnew check`;
  `byDateMonths(s, didNotMatch)`, `byDateDays(s, YearMonth)`,
  `openDay(p, day, idxInDay, fromJoin)`, `chatDay`, `openFormDay`,
  `sendSimpleForm` helper; `bydate` command (`bydate`, `bydate YYYY-MM`,
  `bydate YYYY-MM-DD [N]` with in-day clamping, garbage → months +
  did-not-match line); console paths print typed lines / entry lines;
  `list()` gains Java clickable `[By date]` first line + Bedrock list form +
  typed `By date: /whatsnew bydate` line on chat fallback; tab-complete adds
  `bydate`; help line mentions bydate. Day-view Java buttons run
  `/whatsnew bydate <date> <n>` (preserves day context); Bedrock form
  callbacks call `openDay` directly. Grouping recomputed fresh per by-date
  open. Join popup, OK/seen storage, reload/check/status/reset/push, max-age,
  show-count handling, `/whatsnew N` clamping all unchanged.
- **`plugin.yml`**: version 0.1.2; usage
  `/whatsnew [ok | list | bydate [YYYY-MM[-DD] [N]] | <number>]`.
  **`config.yml`**: header 0.1.2; new keys `bydate-button: 'By date'`,
  `back-button: 'Back'`; show-count default stays 3 (live 10 untouched).
  **`build.sh`**: jar name 0.1.2, jar date 2026-11-13T00:00:00Z.
  **`README.md`** updated (By date section incl. B2 note).
  **New `CHANGELOG-0.1.2.md`** following the 0.1.1 format.
- **`tests/.../TestMain.java` rewritten for 0.1.2**: all 0.1.1 checks updated
  for BYDATE (incl. parity loops for global + day views, clicked mappings);
  external-jar and live-config checks skip when files absent (counted as
  skipped, not passed); new ByDate tests — synthetic live-96 fixture (all 20
  per-day counts byte-checked vs design §5), live-changelog check against
  `~/workspace/whatsnew-pipeline/build/changelog.yml` when present (96
  entries; Nov 13 first entry `2026-11-13-excelmenu-section-colours`, 5th
  `2026-11-13-excelskillspanel`), edge fixtures (one-entry month, empty
  changelog, unparseable posted + non-date id excluded + warned but still in
  flat list, id-prefix fallback, posted-wins disagreement), weekday labels
  (Fri Nov 13, Mon Oct 26, Sat Oct 24), day paging bounds, typed day hint.

## Tests — 0.1.2 unit tests: 128 passed, 0 failed, 9 skipped

Full log: `build-0.1.2/test-results-0.1.2/unit-tests-0.1.2.out`
(also `/tmp/build-0.1.2-first.log`). Skips are the 8 external-jar checks + 1
live-config readback, all absent on this VM — same class as the base gate.

Live changelog verified before build: 96 entries, months Nov 72 / Oct 24,
20 days, per-day counts match design §5 exactly; file sha256
`70fbb728fe99d331f3e433233a62d6619c1b3eead0e6895286c2ee4614a8c43d`,
82,033 B, `~/workspace/whatsnew-pipeline/build/changelog.yml`.

## Rig — player rig GATED (PC route down); local smoke server PASSED

### Rig scripts (written, syntax-checked, NOT yet run on a player rig)

- `rig-tests/javabydate.js` and `rig-tests/bedrockbydate.js` (follow the 0.1.1
  `javanewer.js`/`bedrocknewer.js` patterns; box paths
  `/workspace/batch-next/whatsnew/tests/rig/lib.js`, OUT under
  `/workspace/batch-next/whatsnew-bydate-20261113/tests/rig/`). Both pass
  `node --check` (node v24.22.1).
- Java script covers: list [By date] first + 10 Read rows; months; Nov/Oct
  days + no Oct 27/Nov 14; Nov 13 day-bounded Older walk (4 steps, stops);
  Oct 26 10-entry walk; Oct 24 single entry; garbage date; entry-view
  [By date]; `/whatsnew 99` clamp; reload rendering.
- Bedrock script covers: newest entry form `['OK','Older news','By date']`
  (pending after reset); months form; days forms; Nov 13 walk (newest is
  still the pending global newest → `['OK','Older news','Back']`, oldest
  `['Newer news','Back','Close']`); full Back chain days → months → list
  form (12 buttons: By date + 10 entries + Close); list-form entry button;
  Oct 26 walk (9 steps); Oct 24 `['Back','Close']`; garbage date opens NO
  form (chat rendering) + did-not-match line; `/whatsnew 99` clamp.
- **PC route DOWN from this VM:** 3 SSH probes to `mike-pc` (100.72.156.1,
  `~/.ssh/config` via `ts-connect.py`) all failed with
  `kex_exchange_identification: read: Connection reset by peer / Connection
  reset by 198.19.0.1 port 3128` (probes at ~21:2x, 45 s apart). Per the
  PC-route alert clarity rule (Nov 11, 2026): this is evidence about *my
  connection path*, never proof the PC is down; the PC's own state is
  unknowable while probes fail. Tailscale reports Connected. Design §10.2
  states there is NO fallback for a test-server rig — the player rig gates on
  the PC route and is reported as a gate, not worked around. The rig scripts
  are staged for when the route is back.

### Local smoke server — PASSED (in place of the gated player rig)

- Paper 26.2-123 (from `~/workspace/excelshops/smoke-runtime/`, copied to
  `/tmp/wn-smoke2/`; Paper 26.3 jar could not boot here — Paperclip download of
  mojang_26.3.jar fails, sandbox has no direct internet). Port 25599,
  online-mode=false, 0.1.2 jar + live 96-entry changelog + config show-count 10.
- Boot: `Done (48.040s)`; enable line: `ExcelWhatsNew 0.1.2: 96 news entries
  (newest 2026-11-13-excelmenu-section-colours), 0 players on file, 57 warnings
  (/whatsnew check)` — 57 warnings matches the Nov 13 live TEXT placement
  (pre-existing long-line/em-dash class), zero By-date warnings.
- Console commands verified: `bydate` → Nov 72 / Oct 24; `bydate 2026-11` →
  13 days newest first with correct weekday labels/counts; `bydate 2026-10`
  → 7 days, no Oct 27; `bydate 2026-11-13` → section-colours entry;
  `bydate 2026-11-13 5` → skillspanel entry; `bydate 2026-10-24` → Oct 24
  entry; `bydate 2026-13` → did-not-match + months; `bydate 2026-11-13 99`
  → clamped to 5th entry; `list` → 10 entries unchanged; `check` prints the
  57 warnings.
- Reload test: added synthetic entry `2026-11-14-smoke-test` (Nov 14) to the
  smoke changelog; `whatsnew reload` → "97 entries, newest
  2026-11-14-smoke-test"; `bydate 2026-11` showed "Sat, Nov 14 — 1 change"
  first; `list` row 1 = Smoke test news. (Smoke entry existed only in
  `/tmp/wn-smoke2`, never in the real changelog.)
- Shutdown: clean `stop`, exit 0. Log: **SEVERE 0, ExcelWhatsNew ERROR 0,
  zero exceptions from `com.winkreyes`** (the only ERROR lines are Paper's
  version fetcher + yggdrasil key fetch — sandbox network noise, unrelated to
  the plugin). Full log saved:
  `build-0.1.2/test-results-0.1.2/smoke-server-0.1.2.log`.

## Same-pass surfaces — STAGED (see `surfaces/README-SURFACES.md`)

- What's New entry for the feature: staged registry record
  `surfaces/registry-record-2026-11-13-excelwhatsnew-bydate.yml`
  (id `2026-11-13-excelwhatsnew-bydate`, posted 2026-11-13; game + site
  blocks; one `[INSTALL DETAILS]` sentence for the supervisor to replace at
  filing). File at the TOP of the registry at install time, never before.
  Re-date rule: if the install lands later, re-date id + posted + game date +
  site group together before filing; never rename after filing.
- Website month-jump index (D41-4): staged patch
  `surfaces/generate-month-jump.patch` against
  `whatsnew-pipeline/tool/generate.py` (`render_site_region` only). Emits
  `<p class="month-jump">Jump to: <a href="#month-2026-11">November 2026</a> ·
  <a href="#month-2026-10">October 2026</a></p>` + `id="month-YYYY-MM"` on the
  first day-group of each month. No CSS required (renders as a plain paragraph
  with working links). NOTE: the pipeline selftest Part 1c pins generated
  index.html byte-identical to published — the month-jump line changes the
  output BY DESIGN, so the selftest baseline must be re-pinned by the pipeline
  owner after the patch lands.
- Guide: verified automatic — the feature's site item lands in the Nov 13
  day-group chunk (`changelog-1`), which answers "how do I see old news?".
  No extra Q&A line. Older how-to chunks `changelog-18`, `changelog-19`,
  `cl-whats-new` byte-unchanged (they describe paging/list behavior that does
  not change).
- Broadcaster tips: no tip mentions What's New (0 hits) — none added. Menus:
  no change (doors already run `/whatsnew`). Commands catalogue
  (`si_whatsnew` row): NOT staged here — an ExcelMenu item, for the
  supervisor to file separately if wanted; the in-game help line already names
  `/whatsnew bydate` and tab-complete offers `bydate`.
- Scratch verification: `~/workspace/whatsnew-bydate-20261113/scratch-pipeline/`
  (registry with the staged record filed at top + patched generator + live
  inputs → `scratch-build/` outputs). Changelog 97 entries (diff vs live = the
  one new entry, first); index.html month-jump + anchors + feature site item
  present; ai-knowledge 221 chunks, none added/removed.

## Deviations from the design (named, as required)

1. **Bedrock list form added (B5).** The design assumed a Bedrock list form
   existed in 0.1.1; it did not. Built one (By date first, one button per
   entry, Close; chat-list fallback if the form cannot open). This is an
   addition, not a behavior change to the flat list.
2. **Day labels carry the weekday (D41-5).** "Fri, Nov 13 — 5 changes" instead
   of the design's "Nov 13 — 5 changes". Supervisor decision, Mike vetoes
   by reply.
3. **Em dash (—) in UI labels.** Design §3 exact labels use — (e.g. "November
   2026 — 72 changes"); the house ban on em dashes covers *entry text* only
   and is untouched. Console log renders — as `?` (log encoding); in-game
   UTF-8 unaffected.
4. **Bedrock did-not-match months list renders in chat, not as a form.**
   `byDateMonths(s, didNotMatch)` opens the months form on Bedrock only when
   `didNotMatch` is false; a garbage typed date shows the months as chat lines
   with the "That date did not match any news. Pick a month:" line above them.
   Design §3 requires "the months list again, plus one line" without
   specifying the surface for this case; the chat rendering keeps the line
   visible above the list. Not a deviation from any stated requirement.
5. **Primary shape is month → day → entry** (design scope). The permitted
   alternative (flat day list with month-jump buttons) was not needed on
   Bedrock forms — no decide-and-log choice was taken.
