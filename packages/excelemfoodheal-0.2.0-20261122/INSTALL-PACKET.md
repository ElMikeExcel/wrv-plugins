# INSTALL-PACKET - ExcelEMFoodHeal 0.2.0 (out-of-combat base heal to 2%/s in EliteMobs worlds, decision D184)

Lint: 7111 B (incl. this line) / max line 192 / non-ASCII 0 (targets <= ~12 KB, lines < ~300 chars, minimal non-ASCII)

## PENDING - ExcelEMFoodHeal 0.2.0 (Nov 21, 2026)

- **Packet:** `~/workspace/foodheal-ooc-2pct-20261121/INSTALL-PACKET.md`
- **Live-premise receipts (G1):** pulled fresh this run (Nov 21,
  2026, 8:22 PM CST, Homebase SSH + scp). Live jar
  `plugins/ExcelEMFoodHeal-0.1.0.jar` = 15,564 B / SHA-256
  `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`
  (PC-side hash matches the pull; identical to the 0.1.0 build
  artifact). Live config `plugins/ExcelEMFoodHeal/config.yml` =
  1,301 B / SHA-256
  `cd9428299cf69150046825c08b2b216727080edbe3aa093a308e377c18466079`,
  byte-identical to the 0.1.0 bundled default, `debug: true`, and
  it does NOT yet contain the D184 keys. Full receipts:
  `~/workspace/foodheal-ooc-2pct-20261121/RECEIPTS.md` (the pulled
  live files sit in that folder's `receipts/`).
- **Candidate:** `~/workspace/foodheal-ooc-2pct-20261121/ExcelEMFoodHeal-0.2.0.jar`
  - 16,948 B / SHA-256 `787198b6859eb91bc3a3e452c03cd3b1cb650c035bb678e5129509e6dd971680`;
    deterministic double-build byte-identical; unit tests 145/145
    (137 carried from 0.1.0 + 8 new BonusPolicy checks); rig proof
    on Paper 26.2-123 + real EliteMobs 10.9.14: 16/16 cases PASS,
    SEVERE 0 (bonus alone measures exactly +1%/s out of combat
    with no food; no bonus ticks in combat or inside the 20 s
    window; none in the overworld; food HoT stacks at a combined
    1.5%/s; 0.1.0 food cases re-pass under 0.2.0). Build record:
    `~/workspace/foodheal-ooc-2pct-20261121/BUILD-RECORD.md`.
- **Live-jar gate (STOP on mismatch):** live file
  `plugins/ExcelEMFoodHeal-0.1.0.jar` = 15,564 B /
  `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`
  - sourced from the G1 receipt above, never from lineage. STOP
  if the live jar differs, if a file named
  `ExcelEMFoodHeal-0.2.0.jar` already exists live, or if the live
  config already contains the key `ooc-bonus-enabled`.
- **Staged artifacts:** `~/workspace/foodheal-ooc-2pct-20261121/`
  (SHA256SUMS in the build folder, verified clean) - candidate
  jar + source zip + build record + receipts + this packet.
- **Scope:** jar swap 0.1.0 -> 0.2.0 (old jar kept as .bak) PLUS
  one additive config append (below). Nothing else changes.
  Behavior delta, EliteMobs worlds only: a player who is out of
  combat (the plugin's existing 20 s combat tracking) regenerates
  +1% of max health per second with no food required, so the
  total base out-of-combat heal is 2%/s (EliteMobs' hardcoded
  ACS 1%/s + this 1%/s). The food heal-over-time is byte-for-byte
  the 0.1.0 logic and stacks on top (combined 1.5%/s while a
  budget runs). In-combat healing unchanged; overworld/nether/end
  unchanged (vanilla); no commands, no UI, no data files touched.
  Rollback: restore the .bak jar and the .bak config, restart.
- **Config step (EXPLICIT - append, never replace):** the live
  `plugins/ExcelEMFoodHeal/config.yml` is KEPT. Back it up
  (`.bak` beside it), then append exactly this block to its end;
  every existing line stays byte-identical (in particular
  `debug: true` stays as the live file has it):
  ```
  # D184: base out-of-combat regen bonus in EliteMobs worlds.
  # EliteMobs' own ACS trickle heals 1%/s out of combat and is
  # hardcoded (no config in EM 10.9.14); this bonus adds ours on top
  # under the same out-of-combat rule (combat-window-seconds above),
  # so the total base out-of-combat heal is 2%/s. No food required.
  # Food budgets are unchanged and stack on top of the bonus.
  ooc-bonus-enabled: true
  ooc-bonus-percent-per-second: 1.0
  ```
  (The plugin's in-code defaults equal these values, so the
  append documents the feature; behavior is the same either way.)
- **Gates:** marker discipline with re-checks before Shut Down
  and before Start; BACKUP FIRST + cap-at-6 (name prunees; never
  the Daily newest or the two newest manuals); emptiness
  `list` = 0/10 twice. Runner lane is primary per D156. The
  installing lane appends this window's row to
  `~/workspace/wrv-install-queue/INSTALL-TIMING-LOG.md` (Mike's
  Nov 21 timing rule).
- **Boot verify:** `[EliteMobs] Enabling EliteMobs v10.9.14`
  precedes `[ExcelEMFoodHeal] Enabling ExcelEMFoodHeal v0.2.0`;
  `[ExcelEMFoodHeal] ExcelEMFoodHeal enabled: food heal-over-time active in EliteMobs worlds (debug=true). Out-of-combat bonus (D184): +1.00%/s (total base 2%/s with EliteMobs' own trickle).`;
  `Done` line; SEVERE 0 attributable to ExcelEMFoodHeal. Marker:
  `~/workspace/foodheal-ooc-2pct-20261121/INSTALLED.txt` (written at install).
- **Same-pass surfaces:** PLAYER-FACING - out-of-combat healing
  rate changes in EliteMobs worlds. What's New entry owed on
  BOTH surfaces in the install pass (website section + in-game
  ExcelWhatsNew changelog), drafted here: "Healing between
  fights is faster in EliteMobs worlds: once you have been out
  of combat for 20 seconds, you now recover 2% of your health
  per second (up from 1%). Food still adds its own gradual
  healing on top." Also owed in the pass: website pair
  (page + ai-knowledge) food-healing numbers updated to the 2%/s
  base; broadcaster tips check (no tip quotes an out-of-combat
  rate - confirm); in-game menus check (no menu or command
  changes - confirm none needed). Guide knowledge/intents leg is
  suspended under the Nov 16 Guide hibernation; the website
  stays canon. Record the change in the wrv-plugins record PR +
  Drive deposits at close-out.
- **Proof owed after install:** the first natural case - a
  player below full health in an EliteMobs world, out of combat:
  the live log shows `FOODHEAL bonus start player=<name>
  rate=1.00%/s` and health climbs at the combined 2%/s - or a
  bot proof of the same. After install, the parent seat sends
  Jester a Discord follow-up (his suggestion, P-JESTER-OOC-HEAL-1)
  telling him the 2%/s rate is live. This item closes on the
  proof, not on the install.

## Verification by script output

- `sha256sum ~/workspace/foodheal-ooc-2pct-20261121/ExcelEMFoodHeal-0.2.0.jar`
  -> `787198b6...` (must match the Candidate line above).
- Live premise re-check at the window: `sha256sum` of the pulled
  live jar -> `8c7f1cec...` and of the live config ->
  `cd942829...` (STOP on mismatch; a config whose only delta is
  the appended D184 block is a POST-install state, not a
  premise - if seen pre-install, STOP and report).
- `unzip -l <candidate jar>` -> lists `plugin.yml` with
  `name: ExcelEMFoodHeal`, `version: 0.2.0`,
  `softdepend: [EliteMobs]`, and the
  `com/winkreyes/excelemfoodheal/*.class` entries including
  `BonusPolicy.class`.
- `grep -c "Out-of-combat bonus (D184): +1.00%/s" <live latest.log>`
  -> >= 1 after the install boot.
- `grep -c SEVERE <live latest.log>` -> 0 attributable to
  ExcelEMFoodHeal.
- Post-install proof: `grep "FOODHEAL bonus start" <live latest.log>`
  -> at least one line for a real player in an EliteMobs world.
