# FINGERPRINTS — ExcelSummons 0.1.7

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17`
  (SKU SUMMONS · version 0.1.7 · channel WRV · 2026-11-12 ·
  seq 01 of the menu-build lane).
  Previous: `EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a` (seq 07),
  `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315` (seq 06),
  `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19` (seq 05),
  `EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312` (seq 04),
  `EXC-SUMMONS-0.1.2-WRV-20261108-03-a4d217` (seq 03).
- Homes: (1) `excel-build.txt` at the jar root (verified in
  the shipped jar at staging — content is the build ID);
  (2) the `BuildInfo` constant, assembled from string
  fragments in `src/com/winkreyes/excelsummons/BuildInfo.java`
  (verified in the shipped source); (3) the header stamped
  into the data file the plugin writes
  (`plugins/ExcelSummons/summons-data.yml` carries the build
  line — carried pattern, unchanged in 0.1.7).
- The local build's fingerprint gate verifies homes (1) and
  (2) in the shipped jar on every build; it passed for this
  artifact.
- Startup proof: the enable line in the server log reads
  "ExcelSummons 0.1.7 enabled (build
  EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17); registry holds N
  summon(s)." (rig-verified in `rig-0.1.7/boot.log`; live boot
  Nov 12, 2026, 18:32 CST held 272 — OUTCOME.txt).
- Artifact identity: `ExcelSummons-0.1.7.jar` — 90,538 B —
  SHA-256 2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56,
  reproduced by the in-tree double-build, a clean-room
  rebuild, and a rebuild from the shipped source zip
  (SOURCE.md / BUILD-RECORD.md). The jar is not carried in
  this records package (house rule for this package); it
  lives in the build folder and on the live server.

## Semantic markers (Layer C — persist version to version)

All 0.1.6 markers persist unchanged (see the 0.1.6
FINGERPRINTS in `~/workspace/summons-016-build/records-package/FINGERPRINTS.md`):
the registry dialect (including the 0.1.4 `origin` field), the
`summon_cap` / `summon_duration_ms` convention pair, the
dismissal voice, the constants (server combat ceiling **12**,
recent-fight window **15 s**, target tick **40**, sweep tick
**100**, expired-record grace **30 s**, aggressor retaliation
window **30 s**, Guardian taunt radius **16**), the rank
ladder **Beginner 1 · Student 2 · Apprentice 3 · Master 5**
with earned-rank thresholds **100 / 400 / 2,000**, the reserve
dialect and percentages, the 0.1.4 refusal voice, the 0.1.5
archer equipment spec, and the 0.1.6 reserve scope marker
(CAST-origin records hold mana at the cast rates; ADOPTED-
origin records hold **0% permanently**; ceiling **75** / floor
**20** binding every derivation path). 0.1.7 changes none of
them — the policy core is byte-identical to 0.1.6.

Added by 0.1.7 (the compass Summons page):

- The menu dialect: cast summons render on the MAIN page with
  lines byte-identical to `/summons list` (Nameplate.listLine
  over listLineFor's own inputs) plus the applied held total;
  adopted pets render ONLY on the sub-page behind the
  "Adopted pets (N)" door, every row carrying the ADOPTED
  badge + "holds no mana". Pagination is **45 per page** on
  both views (MenuModel PAGE_SIZE); Bedrock form bodies carry
  every line of their view.
- The adopted-dismiss voice: per-pet button ("Dismiss Wolf");
  the confirm step names the specific pet, says it is the
  real tamed animal, warns "permanently" + "cannot be
  re-summoned"; the keep label is "No — keep my pet". The
  dismissal writes the INFO audit line "Summons adopted
  dismiss by <player> (<uuid>): dismissed adopted <type>
  (<key>), entity <id> — permanent removal of a tamed
  animal, confirm-gated menu flow."
- The compass door marker: ExcelMenu entry id `summons`,
  group `me`, order **27** / order-bedrock **62** (after Wand
  controls), icon SPAWNER, label "My Summons",
  requires-plugin ExcelSummons.
