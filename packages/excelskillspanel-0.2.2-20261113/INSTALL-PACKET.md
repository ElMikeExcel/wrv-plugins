# INSTALL PACKET — ExcelSkillsPanel 0.2.2 (Menu Programme Items 12 + 13)

**Work folder:** `~/workspace/skillspanel-022-build-20261113/`
**Build record:** `BUILD-RECORD.md` (this folder). **Candidate hash:**
SHA-256 `374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e`.
**Filed by:** re-dispatch build lane, Nov 13, 2026 — desk work only; this
packet is a DRAFT for the board supervisor to file after the G1 pulls in
§G1 are run. **No install, MANIFEST edit, panel access, purchase, or
message was performed by this lane.**

## What changes

- **Item 12:** the "Class: Mage" line is gone from the Skills panel on
  every surface (Java chest lore, Bedrock form body, Magic detail view,
  chat fallback). The field is removed from the DTO end-to-end, not
  merely nulled.
- **Item 13:** the Magic header reads rank / next rank / mana / regen
  from the player's CARRIED wand (held → cached-active-if-carried →
  offhand → inventory scan, highest path wins → bound wands → class
  path last, only when the player owns no wand). Next rank comes from
  the WRV order (Apprentice → Master, never Magic's "Wizard"). Mana
  displays as the effective pool (live current / effective max, regen
  effective). Casts come from MagicGate's ledger, labelled "Credited
  casts"; the panel never falls back to ExcelMagicLevel's `totalCasts`
  again (line omitted if MagicGate cannot answer).

## What explicitly does NOT change

- Jar swap only. No config of its own (the plugin ships no config.yml
  and reads no plugin data files of its own); it only READS
  ExcelMagicLevel's live `config.yml` reward table for the regen/cost
  fractions. No player data, earned files, or world content touched.
- Magic Level / XP bar / total XP, Boosts line, seals, mcMMO rows, perk
  ladders, Bedrock `%` handling, gold bars — all unchanged.
- Plugin count unchanged (84 — swap, not an add).

## Candidate

- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar` — **62,142 B /
  SHA-256 `374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e`**;
  build `EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9`.
- Repro jar identical hash (`ExcelSkillsPanel-0.2.2-repro.jar`).
- Tests: JUnit **38/38 pass**. Rig: phase A **71/0**, phase B **71/0**
  (real Paper 26.3 + Magic 11.2.5; Mike's real mage fixture; held /
  not-held / two-wand scenarios; Bedrock body checks; see
  `BUILD-RECORD.md` §4).
- Base gate: 0.2.1 rebuilt byte-identical to live (57,652 B /
  `ce430d1f…`) before this build.

## G1 — Live-premise pulls the supervisor MUST run at filing (NOT yet pulled — desk lane does no panel pulls)

Every claim below needs a SAME-DAY live pull receipt (rclone pull, or
labeled `FALLBACK (panel)` per PACKET-TEMPLATE.md) before the entry is
filed. Expected values are carried from the 03:54 CST Nov 13 landing
record for the 0.2.1 jar; they are EXPECTATIONS to verify, not receipts.
**Do NOT file this packet on the carried premise alone — the installing
window re-pulls fresh and STOPs on any mismatch.**

1. `plugins/ExcelSkillsPanel-0.2.1.jar` — expected **57,652 B /
   `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c`**
   (placed pull-back EXACT in the 03:54 window; MANIFEST INSTALLED
   0.2.1 entry). Receipt: same-day rclone pull path + size + SHA-256 +
   pull timestamp.
2. **Exactly one ExcelSkillsPanel jar live** — no
   `ExcelSkillsPanel-0.2.2.jar` live before the swap (list the live
   `plugins/` ExcelSkillsPanel* names in the receipt).
3. `plugins/Magic-*.jar` live is **Magic 11.2.5** (the version the rig
   proved against; confirm the live filename + hash on pull).
4. `plugins/ExcelMagicLevel-*.jar` live is **0.2.0** (PAPI surface +
   reward-table config the provider reads).
5. `plugins/ExcelMagicGate-*.jar` live is **0.3.2** (credited-casts
   ledger source).
6. mcMMO live is **2.3.003-WRV-XP-2**; ExcelGuildGate live (seals read);
   PlaceholderAPI live is **2.12.3**.
7. Live `plugins/ExcelMagicLevel/config.yml` is readable (reward table
   the provider reads for the regen/cost fractions) — hash on pull;
   the install does not modify it.

## Live-jar gate (STOP on mismatch)

- Outgoing live jar must equal the G1 receipt for (1) above at window
  start (fresh pull); no 0.2.2 jar live before the swap.
- Placed jar pull-back must equal 62,142 B /
  `374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e`.
- Outgoing 0.2.1 jar preserved in place as
  `ExcelSkillsPanel-0.2.1.jar.bak-20261113-pre022` (house pattern).

## Gates

- Marker discipline with re-checks before Shut Down and before Start
  (`~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt`; a foreign
  marker under 3 hours old, or a failsafe run starting within ~5
  minutes, = hands off).
- BACKUP FIRST + cap-at-6 (prune oldest manual backups, name prunees;
  never delete the Daily Backup's newest copy or the two newest manual
  restore points).
- Emptiness: `list` = 0/10 twice (RCON or panel, per lane).

## Boot verify

- `Done (...)!` with no attributable SEVERE.
- `[ExcelSkillsPanel] Enabling ExcelSkillsPanel v0.2.2`
- `[ExcelSkillsPanel] ExcelSkillsPanel 0.2.2 awake (build
  EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9); providers: mcmmo, magic`
- Plugins 84 (swap, no count change); guild LOCKED 4 of 5 unchanged.
- NO "does not match the verified table" WARNING from ExcelSkillsPanel.
- Marker: `~/workspace/skillspanel-022-build-20261113/INSTALLED.txt`
  (+ OUTCOME.txt).

## Same-pass surfaces (supervisor legs — owed, not in this packet)

- Guide skills-panel chunk (rank-from-carried-wand + credited-casts
  wording), website pair, What's New BOTH surfaces (website + in-game
  changelog entry for 0.2.2), broadcaster tips check, in-game menus
  check. Each lands in the same pass as the install or is explicitly
  named as owed in the close-out.
- Records publication (wrv-plugins draft PR) is a supervisor leg —
  G2 duplicate check (`pr-exists.sh`) BEFORE any push; publication
  claim line in the close-out record.

## Proof owed after install (named tester: Mike)

- Open `/skillpanel` **while holding food (wand NOT in hand)** on Java +
  Bedrock and confirm: Rank Apprentice, Next rank Master, Mana 150 /
  150, Mana regen 25, Credited casts 1,215, no Class line. (At his
  level 26 the effective pool IS 150/150 — the rig's 154/154 is the
  level-136 fixture's boosted pool.)
- A live two-wand check is NOT owed by this packet: the two-wand rig
  fixture is a guard only; Mike disputes carrying two wands and the
  registry question is a separate supervisor check (fresh playerdata
  pull).

## Staged artifacts

- `~/workspace/skillspanel-022-build-20261113/` — `SHA256SUMS` in the
  build folder (6/6 verified): candidate jar + repro jar (62,142 B /
  `374ad117…`), `rig/PanelRig.jar` (20,399 B / `52fa620c…`),
  `rig/phase-a-results.txt` (16,717 B / `775c96b7…`),
  `rig/phase-b-results.txt` (16,716 B / `f4816a43…`),
  `base-gate/ExcelSkillsPanel-0.2.1.jar` (57,652 B / `ce430d1f…`).
