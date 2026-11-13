# ExcelSkillsPanel 0.1.1 — BUILD RECORD

**Product:** ExcelSkillsPanel 0.1.1 — the 0.1.0 cleanup build
(menu programme Item 7): the Power Level header collapses to
one line, the cap line is removed, the Magic boosts line
renders rounded, and the percent sign is made to actually
display on Bedrock. Everything else is 0.1.0 behavior,
unchanged.
**Commission:** Mike, Nov 13, 2026, ~12:01–12:04 AM CST (parent
backlog MENU PROGRAMME Item 7), from his live Bedrock panel
pass on 0.1.0 (screenshots `13945_12_mxhw.png` — the main
panel — and `13944_14_p5jr.png` — the Acrobatics detail).
**Base:** ExcelSkillsPanel 0.1.0
(`~/workspace/skillspanel-build-20261112/`, untouched by this
build; jar 45,497 B / `4f9f819d…cb19`).
**Build:** EXC-SKILLSPANEL-0.1.1-WRV-20261113-01-e1f011
**Built:** 2026-11-13 (America/Chicago)
**Command:** `/skillpanel` (unchanged).

## Deliverables (this folder)

- `ExcelSkillsPanel/ExcelSkillsPanel-0.1.1.jar` — **45,739 B**
  — SHA-256 `084b110ddcc044f32f837e355f54d54ee274a80899d0375cad85bb1535e643c2`
- `ExcelSkillsPanel/ExcelSkillsPanel-0.1.1-sources.zip` —
  40,574 B — SHA-256 `474303ed81228507176a0bde88c848127915f8b3dd441482677fc0a349adcd28`
  (src/ + tests/ + plugin.yml + local-build.sh, timestamps
  pinned 2026-11-13)
- `TEST-RESULTS.md` — 21/21 harness tests (18 carried from
  0.1.0, 3 of them updated to the new shapes, + 3 new).
- `RIG-RESULTS.md` — desk rig re-run against 0.1.1:
  Phase A 34/34 PASS, SEVERE 0 (Phase B in the same file).
- `INSTALL-PACKET.md` — DRAFT gated jar-swap packet (NOT
  filed; the board supervisor re-bases its G1 live-premise
  gates with a fresh pull at filing time and files).
- `records-package/` — the staged records package (jar +
  sources + this record + SOURCE.md + INSTALL-PACKET.md +
  SHA256SUMS), mirroring the 0.1.0 package.

## Verification

- **Deterministic double-build: byte-identical** (both jars
  `084b110d…c2` in one build run).
- **Clean-room rebuild: byte-identical** — the shipped
  sources zip extracted to a fresh directory and built with
  its own local-build.sh reproduces `084b110d…c2` exactly
  (tests re-run green inside it).
- **Fingerprint gate: PASS** — excel-build.txt + BuildInfo
  fragments carry EXC-SKILLSPANEL-0.1.1-WRV-20261113-01-e1f011
  in the shipped jar.
- **Rig:** see RIG-RESULTS.md.

## The four changes (exactly the commissioned set)

### 1. Power Level header — one line

0.1.0's header block carried the title "Power Level" AND a
first line "Power Level: 579"; the Bedrock body prints title
then lines, so both printed stacked. In 0.1.1 the value lives
in the block's title itself — `SkillMath.powerHeader` returns
title `"Power Level 579"` (thousands-formatted, no colon) and
NO lines. Every surface shows it exactly once: the Bedrock
body prints the title line alone, the chat fallback prints it
once, and the Java chest shows it as the header item's name
with no duplicating lore line. Proven by harness
(`powerHeaderComposition`, `panelBodyAndChatLines`) and rig
(`panel-power-header`, `panel-bedrock-power-single-line`).

### 2. Cap line removed entirely

The fork's `getPowerLevelCap()` returns `Integer.MAX_VALUE`
when uncapped, so 0.1.0 printed "Power Level cap:
2,147,483,647". 0.1.1 never renders a cap line for any value
(the provider still passes the API read; `powerHeader`
deliberately ignores it). Proven by harness + rig
(`panel-power-no-cap-line`): no "cap" text and no
"2,147,483,647" anywhere in the header or body.

### 3. Boosts line — values verified, then rounded

**Finding (verified from the ExcelMagicLevel provider code
before choosing a format):** the values are FRACTIONS at the
source and PERCENT NUMBERS by the time the panel prints them.

- `com.winkreyesvalley.excelmagiclevel.core.Boost` stores
  fractions — its javadoc: "0.0125 = +1.25%". `RewardTable.
  boostAt(level)` = per-level fraction × (level − 1).
- `core.Percents.format(fraction)` = fraction × 100 at
  scale 4. The PlaceholderAPI surface (`%excelmagiclevel_
  mana_boost%`, MagicLevelExpansion) returns `Percents.
  format(boost.manaMaxBoost())`; the panel's computed
  regen/cost parts go through the identical formula in
  `MagicLines.percentOf`. So "+0.5943" on Mike's panel was
  already 0.5943 PERCENT — not a 59.43% fraction misprint.
- Why it looked raw: the shipped 1000-level curve slices the
  settled cap totals over 999 steps (config.yml: mana
  0.2375 / 999 = 0.0002377377…, cost 0.095 / 999 =
  0.0000950951…). Mike is Magic Level 26 → 25 steps:
  25 × 0.02377377% = 0.5943% and 25 × 0.00950951% = 0.2377%
  — his screenshot's exact numbers, reproduced in the
  harness from the config fractions.

**Chosen format: scale 2, HALF_UP, trailing zeros stripped**
(0.5943 → "0.59", 0.2377 → "0.24", 3.2095 → "3.21",
13.75 → "13.75"). All three parts are normalized through the
one path (`MagicLines.percentOf` at scale 2 for computed
parts; the new `MagicLines.displayPct` re-renders the
PAPI-delivered string at the same scale — an unparseable
provider string passes through unchanged rather than hiding
the boost). **Whole-percent rounding was considered and
rejected:** it would print Mike's 0.59% as 1% (+68% error)
and his real 0.24% cost reduction as 0%. Scale 2 never
zeroes a real boost on the shipped curve (the smallest
step, level 2, renders 0.02% / 0.01% — harness-pinned).
/magiclevel's chat output keeps ExcelMagicLevel's own
4-decimal format, unchanged; the rounding is this panel's
display choice only. Proven by harness
(`boostsRenderRoundedToTwoDecimals`,
`percentFormattingMatchesExcelMagicLevel`) and rig
(`panel-boosts-rounded`: the level-136 fixture renders
"+3.21% / -1.28%", was "+3.2095% / -1.2838%").

### 4. Percent sign on Bedrock — root cause + fix

**Symptom (both of Mike's screenshots):** every "%" in the
Bedrock form BODY is missing — the bar line reads "34 — 10 /
29 XP" for the model's "34% — …", the detail reads "17 —
1,201 / 7,200 XP", the boosts read "+0.5943," — while every
neighbouring character (digits, commas, the "—") survives.
The model's `PanelModel.compact()`/`line()` append "%" in
source, and the harness proves the composed strings carry it.

**Root cause (isolated desk-side as far as a desk can):**
the loss is NOT in this plugin and NOT in Cumulus. New
harness proof (`bedrockFormContentRetainsPercentSigns`):
a form built through the production `BedrockForms.
buildForm` against the REAL Cumulus classes in the staged
Geyser jar returns `content()` with the ASCII "%" intact,
byte for byte. The glyph is therefore dropped downstream of
Cumulus — in the Bedrock client's form-text rendering,
where U+0025 PERCENT SIGN introduces format/translation
substitutions and a bare sign is consumed without rendering
(the same behaviour family as Bedrock lang-format strings).
The Java chest never passes through that renderer (Adventure
components straight to the vanilla client), and the model +
Java lore strings keep the ASCII sign — the harness pins
both, and the rig's Java presentation checks pass on the
same strings. **Residual honesty:** no real Bedrock client
exists on a desk rig, so the client's exact internal step
cannot be observed from here; the isolation above (present
in model → present in built Cumulus form → absent on the
client, in body text only) is the evidence the fix stands
on, and Mike's live glance remains the acceptance.

**Fix:** form-bound body text swaps U+0025 for U+FF05
FULLWIDTH PERCENT SIGN — a distinct codepoint the Bedrock
renderer passes through, drawn as a percent sign. Applied
ONLY at the Bedrock send boundary (`PanelModel.bedrockSafe`
called in PanelMenus on the main body, the skill-detail
body, and the Magic-detail body); the model, the Java lore,
and the chat fallback keep ASCII "%" untouched, so the bar
+ percent both stay on both platforms. `bedrockSafe` is
pure and idempotent; the harness proves Cumulus retains
U+FF05 exactly, and the rig proves the composed body
converts cleanly (`panel-bedrock-percent-safe`).
**Residual risk (flagged for Mike's glance):** if the
Bedrock client's font fell back poorly for U+FF05 the sign
could render wide or substituted on his phone — one glance
at the installed panel settles it; if it looks wrong, the
fallback is spelling the sign U+066A or the word "percent",
a one-line change in `PanelModel.BEDROCK_PERCENT`.

## What did NOT change

Magic Level + XP total, Rank, Class, Next rank, Mana, Mana
regen, Total casts, Guild seals lines; per-skill rows with
bars (bar glyphs, counts, MAX sentinel); child-skill rows;
the Magic detail explainer; providers, command, permissions,
softdepends; the BedrockForms handler shape (Summons 0.1.8
pattern). The diff against 0.1.0 source is confined to
`core/SkillMath.java` (power header), `core/MagicLines.java`
(boost rounding), `core/PanelModel.java` (bedrockSafe), and
`bukkit/PanelMenus.java` (three bedrockSafe call sites),
plus version knobs (plugin.yml, BuildInfo, local-build.sh).

## Honest limits

- No real Bedrock client on a desk rig: the U+FF05 sign's
  on-phone look is Mike's live-glance acceptance (see change
  4's residual risk). The Java panel pass was already owed
  from 0.1.0 and remains the Java-side live check.
- Desk build only: NOT installed, NOT filed in the MANIFEST
  (per commission — the board supervisor verifies this
  return, re-bases the packet's G1 gates with a fresh live
  pull, and files; the failsafe installs).
