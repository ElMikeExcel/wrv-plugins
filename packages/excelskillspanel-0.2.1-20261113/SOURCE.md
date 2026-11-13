# ExcelSkillsPanel 0.2.1 — SOURCE

0.2.1 source = the 0.2.0 tree plus the Items 9 + 10 build
(BUILD-RECORD.md has the full account): `core/MagicLines.java`
(the Magic header block's title is now its first header line —
Item 9a), `core/PanelModel.java` (gold bar variants:
`BarData.filledCount` / `coloredBar` / `lineColored` /
`compactColored`, `HeaderBlock.allLinesColored`,
`SkillRow.compactLineColored`, `Panel.colorizeBar`, and the
Bedrock body composed from the colored variants — Item 9b),
`core/PerkLadder.java` (every perk gains the fork locale's
verified description + description rendering — Item 10),
`bukkit/PanelMenus.java` (bar-aware lore components with
NamedTextColor.GOLD filled segments on the Java chest +
colorized Bedrock detail bodies), plus version knobs. The
layout below is otherwise 0.2.0's.

Package `com.winkreyes.excelskillspanel`, Java 21, Paper API
26.3 compile surface (house lib recipe — see local-build.sh).
No third-party code is copied into the jar: mcMMO and Magic
are read through their public APIs; PlaceholderAPI, Geyser/
Cumulus, Floodgate, ExcelGuildGate and ExcelMagicLevel's store
are reached reflectively in their own class loaders.

## Layout

- `ExcelSkillsPanel.java` — plugin root: provider registration
  behind softdepend presence checks, `/skillpanel` command,
  panel assembly (`panelFor`).
- `BuildInfo.java` — Layer B fingerprint fragments.
- `core/PanelModel.java` — the pure model: BarData (block-bar
  rendering, capped sentinel; 0.2.1: the gold-filled colored
  variants + `colorizeBar` for string surfaces), HeaderBlock,
  SkillRow, Panel (Bedrock body — composed from the colored
  bar variants since 0.2.1 — + chat fallback lines, which keep
  plain bars), and (0.1.1) bedrockSafe — the U+FF05 percent
  rendering applied only to Bedrock form bodies. No Bukkit
  types.
- `core/SkillMath.java` — pure mcMMO composition: a normal row,
  a child-skill row (level, no bar), the Power Level header
  (0.1.1: a single title line; the cap is never rendered).
  0.2.0: both row builders accept a detail tail (the verified
  ladder lines); the no-tail builders keep the website pointer
  — the fail-soft shape for an unverified skill.
- `core/PerkLadder.java` — the verified perk ladders for all
  19 skills (82 perks; LADDER-DATA §3 of record, cross-checked
  at build time against the G1 live skillranks pull, the spec
  tables, and SubSkillType numRanks). Pure: `rankAt`
  (RankUtils.getRank semantics, −1 for numRanks = 0),
  `earnedAt`, the one-line rendering (name + unlock + rank
  state), and `matchesLive` — the gate a skill's live data
  must pass before its ladder may render. Flux Mining is
  absent from the table entirely (D32). 0.2.1: each Perk also
  carries `description` — the fork locale's own
  `<Skill>.SubSkill.<Perk>.Description` text, verified against
  both the fork source and the live jar (all 93 keys compared,
  zero differences; per-perk citations in the build folder's
  DESCRIPTIONS-SOURCE.md) — rendered by `linesFor` /
  `descriptionLine` as an indented line under the perk's
  name/unlock line; a null/blank description renders no extra
  line (the Item 10 fallback — never an invented effect).
- `core/MagicLines.java` — pure Magic header composition from
  a nullable-field DTO (omit-on-unread; casts labelled
  "casts", never "credited"; percent values are
  ExcelMagicLevel fractions × 100, rendered at the panel's
  scale-2 display rounding since 0.1.1). 0.2.1 (Item 9a):
  `headerBlock` titles the block with its first header line
  ("Magic Level N — …") and carries the remaining lines —
  no standalone "Magic" line on any surface; `headerLines`
  (the detail view's source) is unchanged.
- `provider/SkillsProvider.java` — the provider seam
  (DESIGN §7.3): header blocks + skill rows + optional detail
  lines per player.
- `provider/mcmmo/McMMoProvider.java` — provider #1 (the
  commercial core): the 19 PrimarySkillTypes via mcMMO's
  public ExperienceAPI (getLevel enum form; getXP /
  getXPToNextLevel name forms — semantics verified against
  the fork's source), Power Level header, optional rank label
  via mcMMO's PAPI surface. Child skills render level-only.
  0.2.0: per skill, the ladder tail — every perk's live rank
  count (SubSkillType.getNumRanks) and unlock levels
  (RankConfig.getSubSkillUnlockLevel, the fork's own surface
  over the live skillranks.yml in the server's current mode)
  are compared with the verified table via
  PerkLadder.matchesLive; a match renders the ladder at the
  player's level (the derived level for child skills), any
  mismatch or failed read logs a WARNING and keeps the
  website pointer for that skill alone.
- `provider/papi/PapiReads.java` — PlaceholderAPI reflection
  (an unchanged "%...%" return reads as absent).
- `provider/wrv/MagicHeaderProvider.java` — provider #2
  (WRV-only module): assembles the MagicData DTO from the
  ExcelMagicLevel PAPI surface, MagicApiReads, the store
  reads, and the seals read; composition is MagicLines'.
- `provider/wrv/MagicApiReads.java` — the Magic API reads:
  controller via the MagicAPI service (ExcelMagicLevel's
  MagicBridge route); rank from the ACTIVE WAND's path
  (rig-settled — see RIG-RESULTS.md), class-path and
  properties-path fallbacks; absolute mana via
  getMana/getEffectiveManaMax/getEffectiveManaRegeneration;
  legacy § codes stripped from names.
- `provider/wrv/MagicLevelStoreReads.java` — total casts from
  ExcelMagicLevel's live store (reflective; players.yml
  fallback) + the reward table's per-level fractions from its
  live config.
- `provider/wrv/GuildSealsReads.java` — the GuildGate
  reflective read (ExcelMenu's GuildGateReads pattern),
  adapted to omit-on-failure instead of deriving a number.
- `bukkit/PanelMenus.java` — the two presentations: Java
  54-slot chest (headers top row, skills middle, Close bottom;
  27-slot skill + Magic detail views) and the Bedrock forms
  (panel form → per-skill / Magic detail forms, chat fallback
  on any send failure). Form bodies pass through
  PanelModel.bedrockSafe at the send boundary (0.1.1). The
  ladder lands as extra lines in these SAME two shapes
  (detailLines is the single source for both) — no new UI
  machinery in 0.2.0. 0.2.1: lore lines are built by a
  bar-aware component builder (filled ▰ segments in
  NamedTextColor.GOLD, everything else GRAY) for every Java
  item; the Bedrock main body comes from PanelModel's colored
  variants and the detail forms colorize each line via
  `colorizeBar` before bedrockSafe.
- `bukkit/PanelMenuListener.java` — click/drag cancellation +
  routing (house menu-listener pattern).
- `bukkit/BedrockForms.java` — the live-proven Cumulus shape
  carried from ExcelSummons 0.1.8 (Geyser class loader only,
  NEW Cumulus, FormBuilder.resultHandler, every failure logged
  at WARNING — the header comment preserves the 0.1.7 defect
  history that dictates the shape).
- `bukkit/PlatformDetect.java` — per-player platform detection
  (house pattern: Floodgate API, Geyser API, UUID-prefix
  heuristic).

## Provenance of adapted code

BedrockForms and PlatformDetect are adapted from ExcelSummons
0.1.8's same-named classes (same authors — Iris/Mike, WRV),
with plugin-name strings changed; their design comments are
carried verbatim where they record why the shape is what it
is. GuildSealsReads follows ExcelMenu's GuildGateReads
pattern. Everything else is new for this commission. The
PerkLadder table's provenance is the LADDER-DATA.md spec of
record (see BUILD-RECORD.md); the 0.2.1 description field's
provenance is DESCRIPTIONS-SOURCE.md in the build folder.
