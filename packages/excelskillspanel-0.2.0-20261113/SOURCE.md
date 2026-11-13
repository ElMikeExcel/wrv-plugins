# ExcelSkillsPanel 0.2.0 — SOURCE

0.2.0 source = the 0.1.1 tree plus the perk-ladder build
(BUILD-RECORD.md has the full account): `core/PerkLadder.java`
(NEW — the verified ladder table + the pure rank/earned-state
math + the live-match gate), `core/SkillMath.java` (row
builders take a detail tail: verified ladder lines, or the
pointer by default), `provider/mcmmo/McMMoProvider.java`
(live ladder verification per skill against the fork's own
RankConfig / SubSkillType surfaces), plus version knobs. The
layout below is otherwise 0.1.1's.

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
  0.2.0: both row builders accept a detail tail (the verified
  ladder lines); the no-tail builders keep the website pointer
  — the fail-soft shape for an unverified skill.
- `core/PerkLadder.java` — NEW in 0.2.0: the verified perk
  ladders for all 19 skills (82 perks; LADDER-DATA §3 of
  record, cross-checked at build time against the G1 live
  skillranks pull, the spec tables, and SubSkillType
  numRanks). Pure: `rankAt` (RankUtils.getRank semantics,
  −1 for numRanks = 0), `earnedAt`, the one-line rendering
  (name + unlock + rank state; no descriptions), and
  `matchesLive` — the gate a skill's live data must pass
  before its ladder may render. Flux Mining is absent from
  the table entirely (D32).
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
  machinery in 0.2.0.
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
record (see BUILD-RECORD.md).
