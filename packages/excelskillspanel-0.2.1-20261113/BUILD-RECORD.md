# ExcelSkillsPanel 0.2.1 — BUILD RECORD

**Product:** ExcelSkillsPanel 0.2.1 — Menu Programme Items 9 + 10:
(9a) the standalone "Magic" header line is removed from the
panel header block; (9b) progress bars render their FILLED
portion in GOLD on both surfaces; (10) every perk in every
skill's detail ladder gains a short effect description under
its name/unlock line — the fork locale's own perk text,
verified before build, full sweep (82 of 82 perks).
**Commission:** board supervisor run 285 re-dispatch,
Nov 13, 2026 (~03:35 AM CST), after the run-284 dispatch of
this build was interrupted with no output (failure context
carried per the supervision rule; this build started from the
0.2.0 tree as if no prior partial work existed — the 0.2.1
folder did not exist at start). Items read verbatim from
`~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
MENU PROGRAMME section (Items 9 + 10, Mike, Nov 13, 2026,
~2:29–2:31 AM CST).
**Base:** ExcelSkillsPanel 0.2.0
(`~/workspace/skillspanel-020-build-20261113/`, untouched by
this build; jar 53,816 B / `f513eaf5…45`).
**Build:** EXC-SKILLSPANEL-0.2.1-WRV-20261113-01-e2f021
**Built:** 2026-11-13 (America/Chicago)
**Command:** `/skillpanel` (unchanged).

## Base reproduction gate (FIRST STEP — PASS)

Before any editing, the 0.2.0 tree was copied to a scratch
directory, rebuilt with its own `local-build.sh`, and the
result compared to the shipped 0.2.0 jar:

- Rebuilt jar: **53,816 B, SHA-256
  `f513eaf5992aea09c08763d1c653193b3d012f2a2f3b6824dccaa929be22bc45`
  — byte-identical to the shipped 0.2.0 jar**, 29/29 harness
  tests green inside the reproduction, double-build identical,
  fingerprint gate PASS. The 0.2.1 build proceeded on that
  verified base. (The 0.2.0 folder itself was not modified.)

## Item 9a — the standalone "Magic" line is gone

Mike (Nov 13, 2026, ~2:29 AM CST): "No need for the one line
that says magic. Magic level under it already gives it its
title." The 0.2.0 Magic header block carried the title
"Magic"; the Bedrock body and the chat fallback print a
block's title as its own line, directly above "Magic Level
N — …", producing the redundant standalone line in his
screenshot.

The fix is exactly the 0.1.1 Power Level fix, one block over:
`MagicLines.headerBlock` now takes the block's FIRST header
line (normally "Magic Level N — X XP total") as the block's
title and carries the remaining lines after it. Every surface
then renders the full header content exactly once with no
bare "Magic" line: Bedrock body (title line = the Magic Level
line, then Rank/Class/Mana/…), chat fallback (same), and the
Java chest (the header item's NAME is the Magic Level line;
its lore is the remaining lines + bar). `headerLines()` is
unchanged — the Magic DETAIL view (whose own view title is
"Magic", a view title, not the header block) still composes
from the full line list. A one-line Magic read (level only)
yields a title-only block, exactly like the Power Level block.

## Item 9b — progress bars: filled portion GOLD

Mike (Nov 13, 2026, ~2:29 AM CST): "Percentage is clear but
the bar is hard to see. Make the colored in parts gold so it
shows more easily at a glance."

- **Java chest:** `PanelMenus.item(...)` now builds every lore
  line through a bar-aware component builder: filled segments
  (▰) render `NamedTextColor.GOLD` (§6), empty segments (▱)
  and all other text keep the standing GRAY. This covers the
  main chest's skill rows + header items and both detail
  views, which all flow through the same item builder.
- **Bedrock forms:** the model gains the string form of the
  same split — `BarData.coloredBar()` / `lineColored()` /
  `compactColored()` wrap the filled run in legacy `§6` and
  the empty run in `§7`; `Panel.bedrockBody()` uses the
  colored variants, and the two detail forms pass each detail
  line through `PanelModel.colorizeBar(...)` (the same
  filled/empty split located inside an arbitrary line, e.g.
  the detail's bar numbers line and "Magic XP: …"). The main
  form body, skill detail forms, and the Magic detail form are
  all gold-filled.
- The plain model strings (`bar()`, `line()`, `compact()`,
  `chatLines()`) are UNCHANGED — the chat fallback keeps plain
  bars and never carries legacy codes (harness-pinned). Both
  colored forms derive their split from the one
  `BarData.filledCount()`.

## Item 10 — perk descriptions, full sweep, fork-verbatim

Mike (Nov 13, 2026, ~2:30 AM CST): "This is great but let's
add more details. What does it actually mean. What does call
of the wild do what is beast lore what does it do. These
details matter." Scope (2:31 AM CST): "For all of them I need
you to go through" — every perk in every skill.

**Data verification (before build):** the description source
is the live mcMMO fork's own perk text — the
`<Skill>.SubSkill.<Perk>.Description` values in
`locale_en_US.properties`. Verification performed this build:

1. The fork source file
   (`~/workspace/mcmmo-edit/repo/src/main/resources/locale/locale_en_US.properties`,
   89,302 B / `ff6c19c6…`) was parsed key-by-key.
2. The same file was extracted from the LIVE mcMMO jar of
   record (pulled from the live server Nov 12, 2026, run 276;
   jar `4276bd3e…f5c`; entry 91,204 B / `48704fa7…`) and
   **all 93 `.Description` keys compared: zero differences**
   between fork source and live jar. (Whole-file hashes differ
   only by the jar's WRV patch keys — LADDER-DATA check C2
   likewise records all 282 `.SubSkill.` keys identical.)
3. **Coverage: all 82 panel perks have a `.Description` key —
   82 of 82.** No perk needs the fallback. Per-perk key +
   line-number citations for all 82 are in
   `DESCRIPTIONS-SOURCE.md` in this folder.
4. Ladder data (names/unlocks/ranks) is unchanged from the
   0.2.0 verified table of record
   (`~/workspace/skillspanel-020-design-20261113/LADDER-DATA.md`);
   the only new field is the description.

**Rendering:** `PerkLadder.Perk` gains a `description`
component. `ladderLines(...)` now emits, per perk, its
name/unlock line followed by its description as an indented
line (`"  " + text`). Both surfaces consume the same
`detailLines()` source, so Java lore and the Bedrock form show
identical text. Descriptions render under earned AND locked
perks alike (Mike's question was what the locked perks he
could see — Beast Lore / Call of the Wild at his Taming 15 —
actually do).

**Data-rule compliance:** a perk with no verifiable
description keeps name+unlock only — `descriptionLine(...)`
returns null for a null/blank description and `linesFor(...)`
emits no extra line; the fallback is harness-proven with
synthetic description-less perks (no live perk exercises it).
Nothing is invented: texts are byte-verbatim fork locale
values, including the fork's own "Richochet" spelling in
Trick Shot. The single normalization is whitespace — the
properties `\n` escape inside Smelting's Understanding the
Art renders as a space so the description stays one panel
line (documented in DESCRIPTIONS-SOURCE.md). Secondary
`.Description.2` keys are not used (mechanics continuations
with `{0}` placeholders; the primary key is the effect line).
Flux Mining remains omitted entirely per D32 — a locale key
existing for it does not resurrect a perk with no code path.

## Deliverables (this folder)

- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.1.jar` — **57,652 B**
  — SHA-256 `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c`
- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.1-sources.zip` —
  56,537 B — SHA-256 `a67c698beb3174001c2d1fdc348b180e0c2f9370ae5c11824d3df04be4b78462`
  (src/ + tests/ + plugin.yml + local-build.sh, timestamps
  pinned 2026-11-13; zip build is itself deterministic —
  rebuilt zip byte-identical)
- `DESCRIPTIONS-SOURCE.md` — the Item 10 source-of-record:
  all 82 descriptions with their exact locale key + line.
- `TEST-RESULTS.md` — 33/33 harness tests (29 carried from
  0.2.0 + 4 new for Items 9/10; three carried tests updated
  where Items 9/10 changed the pinned shape — see there).
- `RIG-RESULTS.md` — desk rig against 0.2.1: Phase A 46/46
  PASS, Phase B 46/46 PASS, SEVERE 0 both, with composed
  detail readbacks for Acrobatics + Taming + Salvage +
  Smelting.
- `INSTALL-PACKET.md` — DRAFT gated jar-swap packet ON TOP of
  0.2.0 (NOT filed; G1 marked supervisor-to-confirm at
  filing; 0.2.1 installs only after 0.2.0 is live).
- `records-package/` — the staged records package (jar +
  sources + this record + SOURCE.md + INSTALL-PACKET.md +
  SHA256SUMS), mirroring the 0.2.0 package.

## Verification

- **Deterministic double-build: byte-identical** (both jars
  `ce430d1f…45` in one build run).
- **Clean-room rebuild: byte-identical** — the shipped
  sources zip extracted to a fresh directory and built with
  its own local-build.sh reproduces `ce430d1f…45` exactly
  (33/33 tests green inside it).
- **Fingerprint gate: PASS** — excel-build.txt + BuildInfo
  fragments carry EXC-SKILLSPANEL-0.2.1-WRV-20261113-01-e2f021
  in the shipped jar.
- **Rig:** see RIG-RESULTS.md.

## Diff vs 0.2.0 source

Confined to `core/PanelModel.java` (colored bar variants +
`filledCount` + `colorizeBar` + colored body lines),
`core/MagicLines.java` (`headerBlock` title = first line),
`core/PerkLadder.java` (the description field on every perk +
description rendering + `linesFor`/`descriptionLine`),
`bukkit/PanelMenus.java` (bar-aware GOLD lore components +
Bedrock detail-body colorizing), `tests/…/PanelTest.java`
(3 carried tests re-pinned to the Item 9/10 shapes + 4 new
tests), the rig driver/script (candidate 0.2.1 + 7 new checks
+ Taming readback), and version knobs (plugin.yml, BuildInfo,
local-build.sh). Everything else is 0.2.0 verbatim — the
ladder table's names/unlocks/ranks, the live-verification
gate, the providers, and the fail-soft pointer are untouched.

## Honest limits

- No real Bedrock client on a desk rig: that the Bedrock
  client renders `§6` as gold inside Cumulus form bodies, and
  the longer description lines' on-phone look, are Mike's
  live-glance acceptance (the same class as 0.1.1's U+FF05
  percent sign and 0.2.0's ✓/✗ glyphs). The rig proves the
  exact strings/components the plugin sends; the Java side's
  GOLD is proven on real components in the rig.
- Seven Limit Break descriptions are the locale's long form
  (144 chars). They render as single lore/form lines like
  every other description — verbatim fork text per the data
  rule; if Mike finds them heavy on the phone, trimming them
  is a wording pass on top of this build, not a data change.
- Desk build only: NOT installed, NOT filed in the MANIFEST
  (per commission — the board supervisor verifies this
  return, re-bases the packet's G1 gates with a fresh live
  pull at filing time, and files; the failsafe installs).
