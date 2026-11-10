# WandBars form-contrast fix — ExcelWandBars 0.3.14 build record

Commission (Mike, Nov 9, 2026, ~8:12 PM CST, from Bedrock screenshots;
board supervisor dispatch Nov 10, 2026): in the WandBars Summon form,
the "Tame Pet - unlocks at Apprentice" row renders pale text on the
green selected fill — unreadable; a second locked row renders
near-invisible pale-on-light even unselected. This is the Oct 20, 2026
defect class (ExcelMenu Bedrock forms, pale text on light buttons;
fixed at renderer level in ExcelMenu 0.1.1 by darkening content text)
recurring in WandBars' OWN form renderer, which never got that
treatment.

Desk work only: built, unit-tested, renderer-probed. Nothing
installed, nothing published, no MANIFEST edit (a draft entry is
staged for the board supervisor to file).

---

## 1. MECHANISM — convicted from source

Every WandBars Bedrock form is built at ONE construction point:
`BedrockForms.buildSimple` (Cumulus SimpleForm via Geyser reflection).
All callers reach it — Spell Book home/groups/New/detail/locked pages
and the flat spell list via `MagicHook.sendForm`, the bar loadout
menus via `LoadoutHook.menu` → `MagicHook.sendForm`, and the Wand
controls chooser (`StyleHook`) and wand tip (`TipHook`) via
`BedrockForms.simple` directly.

The audit of every label-color construction feeding those forms found
exactly ONE colored construction in the whole form surface:

- `core/SpellbookText.java:25` — `GREY = "§7"` (light grey, #AAAAAA),
  applied at `SpellbookText.java:260` in `bedrockButton(Entry)`: every
  LOCKED spell's group-page button (the Summon group's locked rows are
  the specimen) was prefixed with §7.

Everything else is plain text: group buttons, nav buttons, detail and
loadout buttons, chooser buttons, tip buttons, all form titles, and all
form content carry NO color codes (verified by grep over the full
0.3.13 tree: the only `\u00a7` constructions are the GREY constant, its
one use, and two strip/visible-length regexes that REMOVE codes).

Why §7 fails, in numbers (WCAG relative luminance, approx.):
- §7 #AAAAAA, luminance ≈ 0.402. On the idle light button fill
  (≈ #ECECEC, lum ≈ 0.85): ≈ (0.85+0.05)/(0.402+0.05) ≈ **2.0:1**.
- On the green selected fill (Bedrock focused-button green,
  lum ≈ 0.36): ≈ (0.36+0.05)/(0.402+0.05) ≈ **1.1:1** — pale on green,
  exactly the photographed selected row.
- §8 dark grey (#555555, lum ≈ 0.091) was considered and REJECTED:
  ≈ 7.0:1 on the light fill but only ≈ 2.9:1 on the green fill — it
  fixes the unselected row and still fails the selected one.
- §0 black (lum 0): ≈ **17:1** on the light fill, ≈ **8:1** on the
  green fill. The only legacy ink that survives BOTH states, and the
  Oct 20 standard's outcome (dark text on light fills).

## 2. THE FIX — two layers, one rule

**Source layer** (`core/SpellbookText.java`):
- `GREY` now holds `FormContrast.INK` (§0) instead of §7 — locked rows
  print in dark ink; the words "- unlocks at <rank>" carry the locked
  meaning (they always did; the hue was decoration). The constant name
  is kept so no other reference breaks; its javadoc records the change.
- `LOCKED_HINT` wording follows the rendering it describes:
  "Gray spells unlock at a higher rank." → "Locked spells unlock at a
  higher rank." (group-page content, both platforms).
- `bedrockButton` javadoc: "grey" → "dark-ink".

**Renderer layer** (`BedrockForms.java` + new `core/FormContrast.java`):
- New pure class `core/FormContrast`: `buttonLabel(String)` rewrites
  EVERY legacy foreground color code (§ or & + 0-9a-f) in a button
  label to `INK` (§0), in place; formatting codes (k, l, m, n, o, r)
  and all visible text pass through byte-identical; null/empty pass
  through. Rule rationale, in the class javadoc: a button's text must
  survive both fills, black is the only ink that does, and meaning is
  never carried by button hue alone.
- `BedrockForms.buildSimple` applies `FormContrast.buttonLabel` to
  every button it puts on a form. Titles and content are NOT rewritten:
  they do not sit on button fills, and no WandBars title or content
  line carries a color code today (audit above). This is the
  renderer-level treatment ExcelMenu got on Oct 20, 2026 and WandBars
  never did — no present or future caller can ship a pale button.

## 3. BEFORE/AFTER ACCOUNTING — every label-color construction

| # | Construction (0.3.13 location) | Before | After (0.3.14) |
|---|---|---|---|
| 1 | `core/SpellbookText.java:25` `GREY` constant | `"\u00a77"` (#AAAAAA) | `FormContrast.INK` = `"\u00a70"` (black) |
| 2 | `core/SpellbookText.java:260` `bedrockButton` locked branch | `GREY + fit(s)` → e.g. `§7Tame Pet - unlocks at Apprentice` | `§0Tame Pet - unlocks at Apprentice` (visible text identical, 32 chars) |
| 3 | `BedrockForms.java:47` button loop in `buildSimple` | buttons passed to Cumulus verbatim | each button passed through `FormContrast.buttonLabel` (§/& color codes → §0; formatting + text untouched) |
| 4 | NEW `core/FormContrast.java` | — | the renderer rule + `INK` constant, pure/unit-tested |
| 5 | `core/SpellbookText.java:225` `LOCKED_HINT` (content wording, not a color) | "Gray spells unlock at a higher rank." | "Locked spells unlock at a higher rank." |

Audited and deliberately UNCHANGED (no color construction exists):
Spell Book home buttons (`groupButton`, `newButton`, ALL/BAR/CLOSE,
"Wand controls: …"), flat-list buttons (`SpellInfoText.button`),
detail-form buttons (USE/PUT/MOVE/OFF/GOT_IT/back/Close), locked-page
buttons, loadout menu labels (`LoadoutText.*`), chooser buttons
(`ControlsText.button`), tip buttons (`SpellTip.OK_BUTTON`,
`TipGate.OK_BUTTON`), ALL form titles, ALL form content. Java chat
rendering (Adventure `NamedTextColor`) is a different surface (dark
chat background) and is untouched. Item names/lore (hotbar icons,
pager buttons) are not forms and are out of scope.

Jar entry delta vs shipped 0.3.13 (repack report): changed —
`BedrockForms.class`, `BuildInfo.class`, `SpellbookText.class`
(+ `$Entry`/`$GroupCount`, proven bytecode-identical to 0.3.13 via
javap; only line-number debug metadata moved with the edited file),
`plugin.yml`, `excel-build.txt`; added — `core/FormContrast.class`;
`config.yml` NOT changed; total entries 246 → 247.

## 4. PROOFS

- **Base gate:** 0.3.13 rebuilt from `src0313` (byte-identical tree to
  the 0.3.13 build tree) with the 0.3.13 toolchain (Temurin JDK
  25.0.5+8, `work/cp.txt`, `repack.py` from `ref-0.3.11.jar`) —
  rebuilt jar is **byte-identical** to the shipped 0.3.13:
  544,853 B, SHA-256 `7278f09e…` both. (`basecheck/`)
- **Unit tests: 20 suites, 577 checks, 0 failed** — the 19 carried
  0.3.13 suites (507 checks, test files byte-identical, all green
  against the 0.3.14 tree) plus new `FormContrastTest` (70 checks):
  the photographed Summon rows rebuilt exactly (locked label =
  `§0Tame Pet - unlocks at Apprentice`, no §7, visible length
  unchanged), long-name fallback dark, unlocked rows byte-identical,
  all 22 color-code spellings (§/& × 0-9a-fA-F) rewritten to §0,
  formatting codes preserved, plain labels untouched, idempotence.
- **Renderer probe (desk, real Cumulus):** `rig-probe/FormProbe`
  builds a real `SimpleFormImpl` through the patched
  `BedrockForms.buildSimple` with a caller still passing the OLD §7
  labels; the built form's buttons read `§0Tame Pet - unlocks at
  Apprentice`, `§0Cat - unlocks at Apprentice`, plain rows unchanged.
  (Probe output recorded in TEST-RESULTS.txt; probe source kept in
  `rig-probe/`, not packaged — house pattern for investigation code.)
- **Double-build:** two clean compiles → class trees identical; two
  repacks → **byte-identical** jars.
- **Data non-conflict:** the candidate's `spellbook.yml` is
  byte-identical to shipped 0.3.13's (still lists `- tamepet` in
  Summon), and repack does not swap `spellbook.yml` at all; the
  separate tame-fix packet's data change
  (`~/workspace/tame-fix-packet-20261110/`) neither overlaps nor is
  overwritten by this jar. `config.yml` byte-identical to 0.3.13's —
  the install is JAR-ONLY.

## 5. ARTIFACT

- `ExcelWandBars-0.3.14.jar` — **545,907 B**, SHA-256
  `c87f6005f92539993d05ec8f9f52726a0b3c99a129c36f94153f7fd723774d63`,
  build id `EXC-WANDBARS-0.3.14-WRV-20261110-01-5a68bd`
  (suffix = first 6 hex of SHA-256 of "EXC-WANDBARS-0.3.14-form-contrast").
- Fingerprints: Layer A — name ExcelWandBars, author WinkReyesValley,
  version 0.3.14 real. Layer B — the build id above in `BuildInfo`,
  `excel-build.txt` (verified in the candidate jar), and the enable
  log line. Layer C — house voice in the plugin.yml 0.3.14 note; the
  38-char form limit discipline kept (`visible()` counts unchanged).
  Layer D — channel WRV.

## 6. NOT PROVEN HERE (named, not claimed)

- **Player proof (the deciding evidence):** Mike reopens the Summon
  form on Bedrock after install — the locked rows ("Tame Pet",
  "Cat" — unlocks at Apprentice) must read clearly BOTH idle and
  while highlighted/selected, and a group page with no locked rows
  must look unchanged. No desk rig has a Bedrock client; this build
  does not claim it.
- No live-server boot was run (desk-only commission); boot behavior is
  unchanged code paths plus one pure text function on the form path.
