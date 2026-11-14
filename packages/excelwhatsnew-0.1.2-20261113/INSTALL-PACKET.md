# INSTALL PACKET — ExcelWhatsNew 0.1.2 "By date" browsing

Staged Nov 13, 2026, ~4:00 PM CST — SUPERVISOR-WRITTEN (the build lane timed out
after completing build/tests/smoke/surfaces but before writing these three
deliverables; no rebuild was needed — every fact below is taken from
`BUILD-RECORD.md`, `DESIGN.md`, and the on-disk build artifacts, all re-verified
by the supervisor this run). No install, no MANIFEST edit, no panel access, no
pushes, no messages sent. The supervisor files the MANIFEST entry from
`MANIFEST-ENTRY-DRAFT.md` only after ALL filing gates below clear.

## Decision (Mike, Nov 13, 2026, ~2:25 PM CST)

- "I want what's new to have everything organized by date. People can click on
  a by date button and see what news or changes happened per day. If someone
  wants to skip to the beginning of the month they can."
- Design questions decided by the supervisor under DECIDE-AND-LOG as D41
  (decision-log.md, Nov 13, ~3:15 PM CST; Mike vetoes by reply): (1) By date
  sees ALL entries (96, incl. October); flat list stays at show-count 10.
  (2) Days newest first inside a month. (3) By date button on the list screen
  always, AND on the single-entry view because B1 confirmed the compass opens
  the newest entry directly. (4) Website month-jump index included.
  (5) Day labels carry the weekday ("Fri, Nov 13 — 5 changes").

## Candidate jar (verified by the supervisor this run)

- `ExcelWhatsNew-0.1.2.jar` — **40,535 B**, SHA-256
  `f67e8f717b02000207d4b9e51a4176ef09243b0aa1baabe2affc7cd96406e581`
  (re-hashed Nov 13, ~3:55 PM CST — EXACT vs BUILD-RECORD.md).
- Build fingerprint: deterministic — two consecutive builds byte-identical.
- Toolchain: JDK Temurin 25.0.5+8; compile libs `~/workspace/excelshops/build-libs/`;
  bungeecord-chat 1.20-R0.2 + jspecify 1.0.0 from `~/workspace/m2-repo/`.
  Build script `build-0.1.2/build.sh` (jar name 0.1.2, jar date 2026-11-13T00:00:00Z).
- Source base: the 0.1.1 tree fetched from wrv-plugins PR #84 (branch
  `feature/excelwhatsnew-0.1.1-newer-news`, OPEN draft; the source PR was NOT
  touched). No whatsnew record PR exists.

## Base gate — PASSED (byte-identical rebuild of 0.1.1)

- Fetched source tree has CRLF line endings on all files (PR fetch artifact).
  First rebuild: `72c5553c...`, 30,499 B — NOT identical (28 B delta; invariant
  across 3 paper-api copies and bungeecord variants, proving the delta was not
  the compile classpath). Root cause: CRLF in the 3 YAML files copied verbatim
  into the jar. After LF-normalizing `plugin.yml`, `config.yml`,
  `changelog.yml`, the rebuild is
  `ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5`,
  **30,471 B — byte-identical to the recorded 0.1.1 jar**.
- Base unit tests (LF tree): **76 passed, 0 failed** (8 external-jar checks +
  2 live-config checks skipped — jars absent on the build VM; 86 − 10 = 76).
- Base-gate work dir: `~/workspace/whatsnew-bydate-20261113/build-0.1.2-basecheck/`.

## What was built (0.1.2)

- New `src/com/winkreyes/excelwhatsnew/core/ByDate.java`: pure grouping —
  months newest first → days newest first → entries newest first;
  `dateOf()` = posted, else id YYYY-MM-DD prefix, else null; warnings for
  posted/id disagreement ("posted wins") and unusable date ("left out of By
  date"); labels `dayLabel` = "Fri, Nov 13" (`EEE, MMM d`, Locale.ENGLISH),
  `monthLabel` = "November 2026", `countLabel` = "1 change" / "N changes".
- `core/Nav.java` rewritten: new acts `BYDATE`, `BACK`; global
  `formButtons`/`javaButtons` include BYDATE after OLDER (before CLOSE on
  Bedrock); new `formDayButtons`/`javaDayButtons`; new `typedDayHint`.
- `ExcelWhatsNew.java` rewritten (0.1.2): new settings
  `text.bydate-button` (default "By date"), `text.back-button` (default
  "Back"); `byDateWarnings` at load + `/whatsnew check`; `byDateMonths`,
  `byDateDays`, `openDay`, `chatDay`, `openFormDay`, `sendSimpleForm`;
  `bydate` command (`bydate`, `bydate YYYY-MM`, `bydate YYYY-MM-DD [N]`
  with in-day clamping, garbage → months + did-not-match line); console paths
  print typed/entry lines; `list()` gains Java clickable `[By date]` first
  line + Bedrock list form + typed `By date: /whatsnew bydate` line on chat
  fallback; tab-complete adds `bydate`; help line mentions bydate. Join popup,
  OK/seen storage, reload/check/status/reset/push, max-age, show-count,
  `/whatsnew N` clamping all unchanged.
- `plugin.yml`: version 0.1.2; usage
  `/whatsnew [ok | list | bydate [YYYY-MM[-DD] [N]] | <number>]`.
  `config.yml`: header 0.1.2; new keys `bydate-button: 'By date'`,
  `back-button: 'Back'`; show-count default stays 3 (live 10 untouched).
- `README.md` updated (By date section incl. B2 note); new `CHANGELOG-0.1.2.md`.

## Deviations from the design (named, as required by BUILD-RECORD.md)

1. **Bedrock list form added (B5).** The design assumed a Bedrock list form
   existed in 0.1.1; it did not. Built one (By date first, one button per
   entry, Close; chat-list fallback if the form cannot open). An addition, not
   a behavior change to the flat list.
2. **Day labels carry the weekday (D41-5).** "Fri, Nov 13 — 5 changes" instead
   of the design's "Nov 13 — 5 changes".
3. **Em dash (—) in UI labels.** Design §3 exact labels use —; the house ban
   on em dashes covers *entry text* only and is untouched. Console log renders
   — as `?` (log encoding); in-game UTF-8 unaffected.
4. **Bedrock did-not-match months list renders in chat, not as a form.**
   Garbage typed date shows the months as chat lines with the "That date did
   not match any news. Pick a month:" line above them. Design §3 requires "the
   months list again, plus one line" without specifying the surface; the chat
   rendering keeps the line visible above the list.
5. **Primary shape is month → day → entry** (design scope). The permitted
   alternative (flat day list with month-jump buttons) was not needed on
   Bedrock forms — no decide-and-log choice was taken.

## Tests — 0.1.2 unit tests: 128 passed, 0 failed, 9 skipped

- Full log: `build-0.1.2/test-results-0.1.2/unit-tests-0.1.2.out`. Skips are
  the 8 external-jar checks + 1 live-config readback, all absent on the build
  VM — same class as the base gate.
- Coverage: synthetic live-96 fixture (all 20 per-day counts byte-checked vs
  design §5); live-changelog check against
  `~/workspace/whatsnew-pipeline/build/changelog.yml` (96 entries; Nov 13
  first entry `2026-11-13-excelmenu-section-colours`, 5th
  `2026-11-13-excelskillspanel`); edge fixtures (one-entry month, empty
  changelog, unparseable posted + non-date id excluded + warned but still in
  flat list, id-prefix fallback, posted-wins disagreement); weekday labels
  (Fri Nov 13, Mon Oct 26, Sat Oct 24); day paging bounds; typed day hint.
- Live changelog verified before build: 96 entries, months Nov 72 / Oct 24,
  20 days, per-day counts match design §5 exactly; file SHA-256
  `70fbb728fe99d331f3e433233a62d6619c1b3eead0e6895286c2ee4614a8c43d`,
  82,033 B.

## Rig — PLAYER RIG GATED (filing gate, NOT yet run)

- Rig scripts written + syntax-checked, NOT yet run on a player rig:
  `build-0.1.2/rig-tests/javabydate.js` and `bedrockbydate.js` (follow the
  0.1.1 `javanewer.js`/`bedrocknewer.js` patterns; box paths
  `/workspace/batch-next/whatsnew/tests/rig/lib.js`, OUT under
  `/workspace/batch-next/whatsnew-bydate-20261113/tests/rig/`). Both pass
  `node --check` (node v24.22.1).
- Java script covers: list [By date] first + 10 Read rows; months; Nov/Oct
  days + no Oct 27/Nov 14; Nov 13 day-bounded Older walk (4 steps, stops);
  Oct 26 10-entry walk; Oct 24 single entry; garbage date; entry-view
  [By date]; `/whatsnew 99` clamp; reload rendering.
- Bedrock script covers: newest entry form `['OK','Older news','By date']`
  (pending after reset); months form; days forms; Nov 13 walk; full Back
  chain days → months → list form (12 buttons: By date + 10 entries + Close);
  list-form entry button; Oct 26 walk (9 steps); Oct 24 `['Back','Close']`;
  garbage date opens NO form (chat rendering) + did-not-match line;
  `/whatsnew 99` clamp.
- **Why not run:** the build lane's PC-route probes failed 3/3
  (`kex_exchange_identification: read: Connection reset by peer / Connection
  reset by 198.19.0.1 port 3128`). Per the PC-route alert clarity rule
  (Nov 11, 2026): this is evidence about *that lane's connection path*, never
  proof the PC is down; the PC's own state is unknowable while probes fail.
  Design §10.2 states there is NO fallback for a test-server rig — the player
  rig gates on the PC route and is reported as a gate, not worked around.
- **Filing gate:** the rig runs (supervisor dispatches a rig leg, or runs it
  in-chain) on the PC test-server mirror AFTER the in-flight SkillsPanel
  0.2.2 window closes and the PC route probes healthy. The rig result is
  recorded in `build-0.1.2/RIG-RESULTS.md` (to be written by the rig leg)
  BEFORE this packet is filed. Do NOT file on smoke alone.

## Local smoke server — PASSED (in place of the gated player rig)

- Paper 26.2-123 (from `~/workspace/excelshops/smoke-runtime/`, copied to
  `/tmp/wn-smoke2/`; Paper 26.3 jar could not boot here — Paperclip download
  of mojang_26.3.jar fails, sandbox has no direct internet). Port 25599,
  online-mode=false, 0.1.2 jar + live 96-entry changelog + config show-count 10.
- Boot: `Done (48.040s)`; enable line: `ExcelWhatsNew 0.1.2: 96 news entries
  (newest 2026-11-13-excelmenu-section-colours), 0 players on file, 57 warnings
  (/whatsnew check)` — 57 warnings matches the Nov 13 live TEXT placement
  (pre-existing long-line/em-dash class), zero By-date warnings.
- Console commands verified: `bydate` → Nov 72 / Oct 24; `bydate 2026-11` →
  13 days newest first with correct weekday labels/counts; `bydate 2026-10`
  → 7 days, no Oct 27; `bydate 2026-11-13` → section-colours entry;
  `bydate 2026-11-13 5` → skillspanel entry; `bydate 2026-10-24` → Oct 24
  entry; `bydate 2026-11-13 99` → clamped to 5th entry; `bydate 2026-13` →
  did-not-match + months; `list` → 10 entries unchanged; `check` prints the
  57 warnings.
- Reload test: synthetic entry `2026-11-14-smoke-test` added to the smoke
  changelog; `whatsnew reload` → "97 entries, newest 2026-11-14-smoke-test";
  `bydate 2026-11` showed "Sat, Nov 14 — 1 change" first. (Smoke entry existed
  only in `/tmp/wn-smoke2`, never in the real changelog.)
- Shutdown: clean `stop`, exit 0. Log: **SEVERE 0, ExcelWhatsNew ERROR 0,
  zero exceptions from `com.winkreyes`** (only ERROR lines are Paper's version
  fetcher + yggdrasil key fetch — sandbox network noise). Full log saved:
  `build-0.1.2/test-results-0.1.2/smoke-server-0.1.2.log`.

## Live-premise expectations (G1 — TO-BE-PULLED at filing, never filed on these)

Expected values are from today's records only; the filed entry must cite fresh
same-day live-pull receipts (PC lane preferred; FALLBACK (panel) receipts per
PACKET-TEMPLATE.md if the PC route fails its 3-probe retry):

1. Live jar `plugins/ExcelWhatsNew-0.1.1.jar` — expected 30,471 B /
   `ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5`
   (byte-identical base-gate rebuild of the recorded 0.1.1 jar; live version
   confirmed two ways: plugin.yml in the source tree AND the Nov 13 boot log
   line "ExcelWhatsNew (0.1.1)").
2. Exactly one live ExcelWhatsNew jar.
3. Live changelog `plugins/ExcelWhatsNew/changelog.yml` — 96 entries,
   82,033 B / `70fbb728fe99d331f3e433233a62d6619c1b3eead0e6895286c2ee4614a8c43d`
   (same-day receipt from the Nov 13 14:07 TEXT placement; re-pull at filing).
4. Live config `plugins/ExcelWhatsNew/config.yml` — show-count 10 (live
   value; the 0.1.2 config default stays 3, live 10 untouched).
5. Live plugin count at window time (record it; do NOT hardcode 87 — the
   ResurrectionChest removal may land first and move 87 → 86).

## Same-pass surfaces — STAGED (see `build-0.1.2/surfaces/README-SURFACES.md`)

- What's New entry for the feature: staged registry record
  `surfaces/registry-record-2026-11-13-excelwhatsnew-bydate.yml`
  (id `2026-11-13-excelwhatsnew-bydate`, posted 2026-11-13; game + site
  blocks; one `[INSTALL DETAILS]` sentence for the supervisor to replace at
  filing). File the record at the TOP of the registry at install time, never
  before. RE-DATE RULE: if the install lands later, re-date id + posted +
  game date + site group together before filing; never rename after filing.
- Website month-jump index (D41-4): staged patch
  `surfaces/generate-month-jump.patch` against
  `whatsnew-pipeline/tool/generate.py` (`render_site_region` only). Emits
  `<p class="month-jump">Jump to: ...</p>` + `id="month-YYYY-MM"` on the first
  day-group of each month. No CSS required. PIPELINE BASELINE NOTE: the
  pipeline selftest Part 1c pins generated index.html byte-identical to
  published — the month-jump line changes the output BY DESIGN, so the selftest
  baseline must be re-pinned by the pipeline owner after the patch lands.
- Guide: automatic — the feature's site item lands in the Nov 13 day-group
  chunk (`changelog-1`), which answers "how do I see old news?". No extra Q&A
  line. Older how-to chunks `changelog-18`, `changelog-19`, `cl-whats-new`
  byte-unchanged. Scratch verification in
  `~/workspace/whatsnew-bydate-20261113/scratch-pipeline/` (registry with the
  staged record filed at top + patched generator + live inputs → 97-entry
  changelog, month-jump + anchors + feature site item present, ai-knowledge
  221 chunks, none added/removed).
- Broadcaster tips: no tip mentions What's New (0 hits) — none added. Menus:
  no change (doors already run `/whatsnew`). Commands catalogue
  (`si_whatsnew` row): NOT staged here — an ExcelMenu item, for the supervisor
  to file separately if wanted; the in-game help line already names
  `/whatsnew bydate` and tab-complete offers `bydate`.
- Surfaces legs (supervisor, AFTER the jar lands): file the registry record as
  a TEXT entry (its own MANIFEST entry — the 97th entry comes from the TEXT
  placement, not the jar install), website leg with the month-jump patch,
  Guide regen rides the TEXT placement.

## Gates (standard, failsafe window — the FAILSAFE installs, never the desk lane)

- Marker discipline with re-checks immediately before Shut Down and again
  before Start (a foreign marker under 3 hours old = coordination abort, entry
  stays PENDING).
- BACKUP FIRST + cap-at-6: ONE fresh window backup when the window opens;
  prune the OLDEST manual backups until the post-creation total is ≤ 6; name
  every prunee in the close-out; never delete the Daily Backup's newest copy
  or the two newest manual restore points.
- Emptiness gate: RCON `list` = 0/10 TWICE before Shut Down (players on =
  countdown then push, per weekend protocol).
- Hash gate: live jar SHA-256 must equal the filed live-premise receipt before
  the swap. STOP on mismatch.
- Predecessor held as `plugins/ExcelWhatsNew-0.1.1.jar.bak-<date>` (pull-back
  EXACT) before the new jar is placed under its own filename.

## Install steps (failsafe window)

1. Marker up; window backup (cap-at-6, prunees named); emptiness `list` 0/10 twice.
2. Fresh G1 pulls (receipts above) + hash gate on the live 0.1.1 jar.
3. Hold the live 0.1.1 jar aside as `.bak-<date>` (pull-back EXACT); place
   `ExcelWhatsNew-0.1.2.jar` (40,535 B / `f67e8f71…`) under its own filename.
   Config: place the 0.1.2 `config.yml` ONLY if its non-version content matches
   the live config apart from the two new keys + header — otherwise keep the
   live config (show-count 10) and add the two new keys in place. Record which
   path was taken.
4. Panel Shut Down (Offline verified), panel Start. Console stop never used.
5. Boot verify (see below). Marker deleted at finish.

## Boot verify (after restart)

- `Done (…s)!` line present (record the time verbatim).
- `ExcelWhatsNew 0.1.2: 96 news entries (newest
  2026-11-13-excelmenu-section-colours)` enable line (96 until the surfaces
  TEXT entry lands — the 97th entry is NOT expected at jar-install time).
- Plugin count unchanged vs the pre-window count (record both numbers; do NOT
  assert 87 — see live-premise expectation 5).
- SEVERE 0 attributable; no stack traces from `com.winkreyes`.
- TPS 20.0 / 20.0 / 20.0 after settle (RCON `tps`).
- Guild: ExcelGuildGate enabled, 4 of 5 seals broken, guild LOCKED — unchanged.
- Console smoke: `whatsnew bydate` → Nov 72 / Oct 24; `whatsnew bydate
  2026-11-13` → section-colours entry; `whatsnew list` → 10 entries unchanged.
- Post-boot pull-back: placed jar 40,535 B / `f67e8f71…` EXACT.

## Rollback (if boot verify fails, or on Mike's veto)

1. Shut Down (panel; Offline verified).
2. Restore the held-aside 0.1.1 jar (`plugins/ExcelWhatsNew-0.1.1.jar.bak-<date>`
   → `plugins/ExcelWhatsNew-0.1.1.jar`); remove the 0.1.2 jar; restore the
   pre-window config if it was replaced.
3. Start; verify the 0.1.1 enable line returns (96 entries); SEVERE 0; guild
   LOCKED 4 of 5.

## Close-out record (written when the entry converts to INSTALLED — supervisor / failsafe)

- Install facts: window, lane, backup name + prunees, emptiness results, G1
  receipts, hash-gate result, placed pull-back EXACT, boot Done line, SEVERE
  count, plugin count, guild LOCKED 4 of 5, marker path.
- Rig result: `build-0.1.2/RIG-RESULTS.md` (written by the rig leg before filing).
- **Publication claim:** records + website legs per the surfaces section —
  carry the G2 line per repo: `<repo> PR #___ (duplicate check run
  <timestamp>, result ___)`, one per repo. No whatsnew record PR exists today
  (open-PR search + code search 0 hits) — the 0.1.2 records publication is a
  supervisor leg after install.
- Drive deposits: jar + source zip + SOURCE.md into the Drive Minecraft WRV
  folder (supervisor leg after install).
- Baseline citations: the live changelog hash `70fbb728…` above is cited from
  the Nov 13 14:07 live TEXT placement receipt, NOT from a
  LIVE-BASELINE-HASHES.txt file — label it by its receipt, and re-pull fresh
  at filing regardless.

## Open questions (for the supervisor at filing — NOT for Mike)

1. **Rig result:** the player rig is the filing gate. If the rig leg reports
   failures, the packet does NOT file until they are fixed + re-rigged.
2. **Install date vs registry record date:** if the install lands after Nov 13,
   re-date the staged registry record (id + posted + game date + site group)
   BEFORE filing the surfaces TEXT entry; never rename after filing.
3. **Config placement path:** see install step 3 — record which path was taken.
4. **Ordering vs ResurrectionChest removal:** if the removal lands first, the
   plugin-count baseline moves 87 → 86; the boot-verify count is recorded at
   window time, never hardcoded.

## Staged files in this packet folder

- `INSTALL-PACKET.md` (this file)
- `MANIFEST-ENTRY-DRAFT.md` (filing draft — DO NOT FILE until the rig leg
  returns + G1 pulls are run)
- `SHA256SUMS` (hashes of the files below)
- `ExcelWhatsNew-0.1.2.jar` (40,535 B / `f67e8f71…`)
- `BUILD-RECORD.md`, `CHANGELOG-0.1.2.md`, `README.md`
- `surfaces/` (README-SURFACES.md, registry record, month-jump patch)
- `rig-tests/` (javabydate.js, bedrockbydate.js + 0.1.1 patterns)
- `test-results-0.1.2/` (unit-tests-0.1.2.out, base-0.1.1-unit-tests.out,
  smoke-server-0.1.2.log)
