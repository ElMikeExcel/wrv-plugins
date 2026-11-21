# INSTALL-PACKET - ExcelEMFoodHeal 0.1.0 (food heal-over-time in EliteMobs worlds, decision D162)

Lint: 5508 B (incl. this line) / max line 118 / non-ASCII 0 (targets <= ~12 KB, lines < ~300 chars, minimal non-ASCII)

## PENDING - ExcelEMFoodHeal 0.1.0 (Nov 20, 2026)

- **Packet:** `~/workspace/emfoodheal-build-20261120/INSTALL-PACKET.md`
- **Live-premise receipts (G1):** NEW plugin - no live jar exists and
  none is expected; nothing to conflict with. EliteMobs premise: live
  jar `plugins/EliteMobs-10.9.10.jar` (internal version 10.9.14) =
  14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`
  (audit pull 20261120-062811; the build compiles and rigs against
  these exact bytes). Full receipts:
  `~/workspace/emfoodheal-build-20261120/RECEIPTS.md`.
- **Candidate:** `~/workspace/emfoodheal-build-20261120/ExcelEMFoodHeal-0.1.0.jar`
  - 15,564 B / SHA-256 `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`;
    deterministic double-build byte-identical; unit tests 137/137;
    rig proof on Paper + real EliteMobs 10.9.14 - all cases PASS
    (eat-and-heal in an EM-gated world, combat pause + resume at
    exactly 20 s, overworld no-op, clamp at max, replace-not-stack;
    SEVERE 0). Build record:
    `~/workspace/emfoodheal-build-20261120/BUILD-RECORD.md`.
- **Live-jar gate (STOP on mismatch):** none - new plugin. STOP if a
  file named `ExcelEMFoodHeal*.jar` already exists in live `plugins/`
  (would mean an unrecorded install).
- **Staged artifacts:** `~/workspace/emfoodheal-build-20261120/`
  (SHA256SUMS in the build folder) - candidate jar + source zip +
  build record + receipts + this packet.
- **Scope:** adds ONE new plugin. In EliteMobs worlds only (gate:
  EliteMobs' own `EliteMobsWorld.isEliteMobsWorld` registry OR the
  config world list, default `em_primis` + `em_adventurers_guild`),
  eating food grants +1% of max health instantly plus a heal budget
  totalling `clamp(saturation x 0.75, 1%, 10%)` of max health
  (steak 9.6%, bread 4.5%), delivered at 0.5%/s while out of combat;
  the budget waits during combat (20 s window, mirroring ACS),
  expires 90 s after the meal, is replaced (never stacked) by
  re-eating, and never heals above max. Self-only healing; no
  cleanse/shield/absorption/DR. EliteMobs' own flat ACS food heal
  is untouched and still applies. No commands, no UI, no changes to
  any existing plugin, config, or data. First boot generates
  `plugins/ExcelEMFoodHeal/config.yml` with `debug: true` (proof
  logging on; flip to false after the post-install proof).
  Rollback: remove the jar and the generated config folder, restart.
- **Gates:** marker discipline with re-checks before Shut Down and
  before Start; BACKUP FIRST + cap-at-6 (name prunees; never the
  Daily newest or the two newest manuals); emptiness `list` = 0/10
  twice. Runner lane is primary per D156.
- **Boot verify:** `[EliteMobs] Enabling EliteMobs v10.9.14` precedes
  `[ExcelEMFoodHeal] Enabling ExcelEMFoodHeal v0.1.0`;
  `[ExcelEMFoodHeal] ExcelEMFoodHeal enabled: food heal-over-time active in EliteMobs worlds (debug=true).`;
  `Done` line; SEVERE 0 attributable to ExcelEMFoodHeal. Marker:
  `~/workspace/emfoodheal-build-20261120/INSTALLED.txt` (written at install).
- **Same-pass surfaces:** PLAYER-FACING - food behavior changes in
  EliteMobs worlds. What's New line (drafted here, lands on BOTH
  surfaces in the install pass): "Food now heals you gradually in
  EliteMobs worlds: eating gives a small heal right away, then keeps
  healing you slowly while you are out of combat - better food heals
  more. Cleric heals are still the way to heal in a fight and to
  heal your friends." Owed in the install pass (supervisor lane):
  website pair (page + ai-knowledge) gains the food-healing note;
  broadcaster tips check (no existing tip scopes food healing in EM
  worlds - confirm); in-game menus check (no menu or command
  changes - confirm none needed). Guide knowledge/intents leg is
  suspended under the Nov 16 Guide hibernation; the website stays
  canon. Record the change in the wrv-plugins record PR + Drive
  deposits at close-out.
- **Proof owed after install:** a player at partial health in Primis
  eats a steak: the log shows a FOODHEAL meal line (9.6% budget) and
  the budget delivering over ~17 s while out of combat; a hit taken
  mid-budget pauses delivery (FOODHEAL pause line) and it resumes
  ~20 s after the last combat action. Eating in the overworld
  produces no FOODHEAL line. After the proof, set `debug: false` in
  `plugins/ExcelEMFoodHeal/config.yml` and reload/restart per the
  text-edit exemption. Testers: Jester or Marvin (D154 testing
  pool); Java + Bedrock both (mechanic is server-side, identical).

## Verification by script output

- `sha256sum ~/workspace/emfoodheal-build-20261120/ExcelEMFoodHeal-0.1.0.jar`
  -> `8c7f1cec...` (must match the Candidate line above).
- `ls live plugins/ | grep -i foodheal` before install -> no match
  (STOP if a match exists).
- `unzip -l <candidate jar>` -> lists `plugin.yml` with
  `name: ExcelEMFoodHeal`, `version: 0.1.0`,
  `softdepend: [EliteMobs]`, and the three
  `com/winkreyes/excelemfoodheal/*.class` entries.
- `grep -c "ExcelEMFoodHeal enabled" <live latest.log>` -> >= 1 after
  the install boot.
- `grep -c SEVERE <live latest.log>` -> 0 attributable to
  ExcelEMFoodHeal.
- Post-install proof: `grep FOODHEAL <live latest.log>` -> meal /
  pause / resume / complete lines for the Primis steak test.
