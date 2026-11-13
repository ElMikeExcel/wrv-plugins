# FINGERPRINTS — ExcelSummons 0.1.8

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba`
  (SKU SUMMONS · version 0.1.8 · channel WRV · 2026-11-12 ·
  seq 01 of the 0.1.8 build lane).
  Previous: `EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17`,
  `EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a` (seq 07),
  `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315` (seq 06),
  `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19` (seq 05),
  `EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312` (seq 04),
  `EXC-SUMMONS-0.1.2-WRV-20261108-03-a4d217` (seq 03).
- Homes: (1) `excel-build.txt` at the jar root (verified in
  the shipped jar at staging — content is the build ID);
  (2) the `BuildInfo` constant, assembled from string
  fragments in `src/com/winkreyes/excelsummons/BuildInfo.java`
  (verified in the shipped source at staging); (3) the header
  stamped into the data file the plugin writes
  (`plugins/ExcelSummons/summons-data.yml` carries the build
  line — carried pattern, unchanged in 0.1.8).
- The local build's fingerprint gate verifies homes (1) and
  (2) in the shipped jar on every build; it passed for this
  artifact.
- Startup proof: the enable line in the server log reads
  "ExcelSummons 0.1.8 enabled (build
  EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba); registry holds N
  summon(s)." (rig-verified in both rig phases; live boot
  Nov 12, 2026, 19:32 CST held 272 — OUTCOME.txt).
- Artifact identity: `ExcelSummons-0.1.8.jar` — 92,387 B —
  SHA-256 835419aaeff434b61e6be2f5cda1340233299e11262feebf96206cdd00bb8841,
  reproduced by the in-tree double-build and a clean-room
  rebuild from the shipped sources zip (SOURCE.md /
  BUILD-RECORD.md). The jar is not carried in this records
  package (house rule for this package); it lives in the
  build folder and on the live server.

## Semantic markers (Layer C — persist version to version)

All 0.1.7 markers persist unchanged (see the 0.1.7
FINGERPRINTS.md in the excelsummons-0.1.7-20261112 package):
the registry dialect (including the 0.1.4 `origin` field), the
`summon_cap` / `summon_duration_ms` convention pair, the
dismissal voice, the constants (server combat ceiling **12**,
recent-fight window **15 s**, target tick **40**, sweep tick
**100**, expired-record grace **30 s**, aggressor retaliation
window **30 s**, Guardian taunt radius **16**), the rank
ladder **Beginner 1 · Student 2 · Apprentice 3 · Master 5**
with earned-rank thresholds **100 / 400 / 2,000**, the reserve
dialect and percentages, the 0.1.4 refusal voice, the 0.1.5
archer equipment spec, the 0.1.6 reserve scope marker
(CAST-origin records hold mana at the cast rates; ADOPTED-
origin records hold **0% permanently**; ceiling **75** / floor
**20** binding every derivation path), and the 0.1.7 menu
dialect (cast-only main page with list-identical lines,
adopted pets only on the sub-page behind the "Adopted pets
(N)" door with the ADOPTED badge + "holds no mana",
pagination **45 per page**, the adopted-dismiss voice and its
INFO audit line, and the compass door marker — ExcelMenu
entry id `summons`, group `me`, order **27** / order-bedrock
**62**, icon SPAWNER, label "My Summons", requires-plugin
ExcelSummons; the door itself is unchanged by 0.1.8 and no
menu entry ships in this package). The policy core is
byte-identical to 0.1.7 — sources differ in exactly four
files (BedrockForms, MenuModel, SummonsMenus, BuildInfo +
plugin.yml version).

Added by 0.1.8:

- The Bedrock response dialect: forms are built ONLY through
  the Geyser class loader against the NEW Cumulus
  (`org.geysermc.cumulus.form.SimpleForm`); the response
  handler attaches via `FormBuilder.resultHandler` and reads
  `SimpleFormResponse.clickedButtonId()`; a closed form
  reports **−1**. The OLD Cumulus
  (`org.geysermc.cumulus.SimpleForm`), `validResultHandler`,
  and any Floodgate form attempt are absent — their absence
  is pinned by test. Handler/response failures log at
  WARNING; nothing is swallowed silently.
- The out-now line: `Out now: M mount(s) · P cast pet(s) ·
  C combat summon(s) (T total)` derived from the page's CAST
  entries by base key (mount → mounts; wolf / cat / familiar
  → cast pets; every other cast key → combat), all three
  groups always printed, or `Nothing out right now.` when no
  cast summon is out. Adopted pets are never counted. The
  line sits in the Bedrock main body after the registered
  line and leads the Java info book's lore.
