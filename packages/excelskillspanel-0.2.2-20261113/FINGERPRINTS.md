# FINGERPRINTS — ExcelSkillsPanel 0.2.2

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9`
  (SKU SKILLSPANEL · version 0.2.2 · channel WRV · 2026-11-13 ·
  seq 01 of the 0.2.2 build lane).
- Homes: (1) `excel-build.txt` at the jar root; (2) the
  `BuildInfo` constant, assembled from string fragments in
  `src/com/winkreyes/excelskillspanel/BuildInfo.java`
  (`"EXC-SKILLSPANEL-", "0.2.2-", "WRV-", "20261113-", "01-",
  "c7d2e9"` — verified in the shipped source at staging).
  The local build's fingerprint gate verifies homes (1) and
  (2) in the shipped jar on every build; it passed for this
  artifact (BUILD-RECORD.md §3: `excel-build.txt` +
  `BuildInfo` fragments verified in-jar).
- Startup proof: the enable line in the server log reads
  "ExcelSkillsPanel 0.2.2 awake (build
  EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9); providers:
  mcmmo, magic" (rig-verified in both rig phases; live boot
  Nov 13, 2026, 3:48:09 PM CST — OUTCOME.txt).
- Artifact identity: `ExcelSkillsPanel-0.2.2.jar` — 62,142 B —
  SHA-256 374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e,
  reproduced by the in-tree double-build (repro jar identical
  hash). The jar is not carried in this records package
  (house rule for this package); it lives in the build folder
  and on the live server.
- Previous: 0.2.1, 57,652 B /
  SHA-256 ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c,
  rebuilt byte-identical to live by the base gate before this
  build (BUILD-RECORD.md §3). Its jar is preserved live as
  `ExcelSkillsPanel-0.2.1.jar.bak-20261113-pre022`.

## Semantic markers (Layer C — persist version to version)

Added / changed by 0.2.2:

- The Magic header reads from the CARRIED wand: rank / next
  rank / mana / regen resolve held → cached-active-if-carried
  → offhand → inventory scan (highest path wins) → bound wands
  → class path last, only when the player owns no wand.
- The WRV rank order: beginner → student → apprentice →
  master. Next rank is always the WRV successor (Apprentice →
  Master); Magic's chain name "Wizard" never appears on the
  panel.
- Mana displays as the EFFECTIVE pool: live current mana /
  effective max, regen effective. The Boosts line still carries
  the percentages (mana pool / mana regen / cast cost).
- Casts are labelled "Credited casts" and come from
  MagicGate's ledger (1,215 for Mike); the panel never falls
  back to ExcelMagicLevel's `totalCasts` (the line is omitted
  if MagicGate cannot answer). The "Total casts" label is
  absent everywhere.
- No "Class: Mage" line on any surface (header, detail,
  Bedrock body, chat fallback); the field is gone from the DTO
  end-to-end.
- The WRV rank ladder the panel follows: Student 100 ·
  Apprentice 400 · Master 2,000 (wand-lore milestones, per the
  diagnosis screenshots).

Carried from 0.2.1 unchanged: Magic Level / XP bar / total XP,
Boosts line, seals (Guild seals: 0 of 5), mcMMO rows (19 rows,
verified against direct ExperienceAPI reads), perk ladders +
descriptions, Bedrock `%` handling, gold-filled bars in main +
detail. No config of its own; only reads ExcelMagicLevel's
live `config.yml` reward table.
