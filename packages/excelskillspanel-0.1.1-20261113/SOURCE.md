# ExcelSkillsPanel 0.1.1 — SOURCE

0.1.1 source = the 0.1.0 tree with four confined changes
(BUILD-RECORD.md has the full account): `core/SkillMath.java`
(the Power Level header is one line, cap never rendered),
`core/MagicLines.java` (boost display rounding — percentOf
at scale 2 + displayPct for provider-formatted strings),
`core/PanelModel.java` (bedrockSafe / BEDROCK_PERCENT — the
Bedrock form percent rendering), `bukkit/PanelMenus.java`
(bedrockSafe applied at the three form-send sites), plus
version knobs. The layout below is otherwise 0.1.0's.

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
  rendering, capped sentinel), HeaderBlock, SkillRow, Panel
  (Bedrock body + chat fallback lines), and (0.1.1)
  bedrockSafe — the U+FF05 percent rendering applied only to
  Bedrock form bodies. No Bukkit types.
- `core/SkillMath.java` — pure mcMMO composition: a normal row,
  a child-skill row (level, no bar), the Power Level header
  (0.1.1: a single title line; the cap is never rendered).
- `core/MagicLines.java` — pure Magic header composition from
  a nullable-field DTO (omit-on-unread; casts labelled
  "casts", never "credited"; percent values are
  ExcelMagicLevel fractions × 100, rendered at the panel's
  scale-2 display rounding since 0.1.1).
- `provider/SkillsProvider.java` — the provider seam
  (DESIGN §7.3): header blocks + skill rows + optional detail
  lines per player.
- `provider/mcmmo/McMMoProvider.java` — provider #1 (the
  commercial core): the 19 PrimarySkillTypes via mcMMO's
  public ExperienceAPI (getLevel enum form; getXP /
  getXPToNextLevel name forms — semantics verified against
  the fork's source), Power Level header, optional rank label
  via mcMMO's PAPI surface. Child skills render level-only.
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
  reflective read (ExcelMenu's proven GuildGateReads pattern),
  adapted to omit-on-failure instead of deriving a number.
- `bukkit/PanelMenus.java` — the two presentations: Java
  54-slot chest (headers top row, skills middle, Close bottom;
  27-slot skill + Magic detail views) and the Bedrock forms
  (panel form → per-skill / Magic detail forms, chat fallback
  on any send failure). Form bodies pass through
  PanelModel.bedrockSafe at the send boundary (0.1.1).
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
pattern. Everything else is new for this commission.
