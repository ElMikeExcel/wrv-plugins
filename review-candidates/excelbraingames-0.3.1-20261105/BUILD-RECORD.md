# ExcelBrainGames 0.3.1 "Letter Tiles" — BUILD RECORD

Built: Nov 5, 2026. Builder: Iris subagent (build task 4340f920). Status:
**BUILT + TESTED + RIG-PROVEN — filed PENDING install** (gated; see
INSTALL-PACKET.md). Nothing was installed, published, or sent by this build.

## Baseline

- Source tree: `~/workspace/brain-games/build-phase3/` — verified as the tree
  that produced the live 0.3.0 jar (on-disk jar re-hashed before copying:
  118,307 B, SHA-256 `acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63`).
  Copied to `~/workspace/brain-games/build-0.3.1/`; all work happened there.
- Mechanism research from primary sources, before code:
  - Paper API 26.3 build 231 jar (pinned `f0df9e7f…`): `ItemMeta.setItemModel`
    exists → modern `minecraft:item_model` component chosen over legacy
    CustomModelData.
  - Geyser wiki, Custom Items page (geysermc.org/wiki/geyser/custom-items/,
    read Nov 5, 2026): v2 JSON mappings (`format_version: 2`, `definition`
    entries keyed on `item_model`), mappings live in
    `plugins/Geyser-Spigot/custom_mappings/`, the Bedrock pack goes in
    Geyser's `packs` folder and is served to joining Bedrock players,
    `enable-custom-content: true` required, restart required. The mapping
    route EXISTS for the live Geyser 2.12.1 line — no improvisation needed.
  - Paper 26.3-235 server jar (the live build) downloaded from PaperMC's
    fill-data CDN and SHA-256-verified against PaperMC's published checksum
    (`4ebaf5c9da98c654bb8f17245799943dfb05a285830be4307bfdf7bc9ca024bf`);
    its version.json gives resource pack_version 97.1 → pack_format 97.

## What changed (source delta vs 0.3.0)

- `core/TileKeys.java` (new): the 205-key tile space + defensive label mapping.
- `core/ChoicePrintTracker.java` (new): once-per-prompt tracker.
- `games/sudoku/SudokuTextGrid.java` (new): the bordered chat square (pure).
- `gui/Views.java`: tile stamps on Word Search cells (normal/selected/found),
  Sudoku cells (given/wrong) + digit items (placed; board + palette), Quick
  Math kid choice emeralds (number tiles); Scramble kid once-per-word chat
  print; Sudoku board prints its chat square at open; "Show board in chat"
  control at slot 44 on every Sudoku board. **Add-on:** Quick Math kid
  choice rounds also print the QUESTION once per question in chat
  (index-keyed on the same tracker) — the §8 residual, closed.
- `gui/BrainHolder.java`: transient `choicePrint` field (never persisted).
- `gui/GuiListener.java`: `spal:show` dispatch → re-print the chat square.
- `plugin.yml`: version 0.3.1 + description note. Nothing else. No config,
  payout, save-format, or word-list changes.

## Artifacts

| Artifact | Size | SHA-256 |
|---|---|---|
| ExcelBrainGames-0.3.1.jar | 125,732 B | `b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f` (add-on build — jar of record; first build was 125,602 B / `45b178a7…`) |
| packs/dist/braingames-tiles-java.zip | 189,943 B | `05942eed39e0eed0e7bfc9538641c033fda420c5019484fef768e81ef20bb0d4` (SHA-1 `d110eadec60cf07e7c6f0d5c27623de4a7ee729d` — the server.properties value) |
| packs/dist/braingames-tiles-bedrock.mcpack | 85,596 B | `486bcad6e1452ffa8e6e83e267112d6edad693c2de68f4e1057c3bc9c2221286` |
| packs/dist/geyser/braingames-tiles.json | 46,768 B | (in SHA256SUMS) |

- Determinism: jar assembled twice inside build.sh and hash-asserted; packs
  generated twice and hash-asserted; the WHOLE build.sh was then run a
  second time and produced the identical jar SHA-256.
- Tile inventory: 205 tiles per pack — Word Search 78 (26 letters × 3
  states), Sudoku 27 (9 digits × 3 states), Quick Math numbers 100 (0–99).
  Generator + validator: `packs/generate_tiles.py`, `packs/validate_packs.py`
  (reproducible: fixed font, fixed pack UUIDs, fixed zip timestamps).

## Tests & rig (detail in TEST-REPORT.md)

- Carried suite: **622,227 checks, 0 failed** — identical per-suite counts
  to 0.3.0 (behavior unchanged). TileTest: **14,473 checks, 0 failed**
  (key space, manifest cross-check, kid-math bound with observed max 40,
  once-per-prompt, Sudoku grid cell-by-cell at all three sizes, plus the
  add-on's 44 once-per-question Quick Math print checks). Total across all
  suites: **636,700 checks, 0 failures**.
- Pack validation: OK — all JSON parses, all references resolve, Java and
  Bedrock PNGs byte-identical, worst glyph contrast 4.40 (≥ 4.0 required).
- Visual QA: tiles `ws/q_normal`, `number/42`, `sudoku/7_given` opened and
  eyeballed — glyphs exact, no artifacts.
- Rig: Paper **26.3-235** (live build) + Vault + the 0.3.1 jar, scratch
  server in `rig/server/`, log at `rig/boot.log`: Done (32.352s), enable
  line intact (`games=[word-search, memory-match, sudoku, quick-math,
  word-scramble, riddle], kid-tiers=ENABLED, daily-cap=$1,500.00`), clean
  disable, **SEVERE 0**. (The scratch rig has no economy provider, so the
  enable line reads `economy=NONE (ledger-only payouts!)` — a rig artifact,
  not a build defect; the live server runs EssentialsX Economy.)

## Deviations & notes

- Rig bootstrap quirk (rig-only): this VM's direct Java TLS to Mojang
  endpoints fails ("Unsupported or unrecognized SSL message"), so the
  paperclip's Mojang download was pre-seeded into `rig/server/cache/` via
  curl with SHA-1 verification against Mojang's own version JSON
  (`33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c`). No product impact.
- The Java pack is NOT published anywhere by this build — hosting is a
  stated requirement in INSTALL-PACKET.md (parent publishes at install).
- Defect evidence logged: Mike's Nov 5, 2026 screenshots
  (`13580_29_x6d4.png` Word Search, `13582_30_n86w.png` Sudoku,
  `13581_31_9ryd.png` Quick Math) stand as player-proof FAILURES against
  0.3.0 — see SPEC-DELTA.md §1.
- Records package staged at
  `~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.1-20261105/`
  (staging only — the parent publishes). MANIFEST entry intentionally NOT
  filed by this build; the parent files it after verifying this record.

## Add-on build (Nov 5, 2026, later the same day)

- **Change:** ONE source file — `gui/Views.java`: in `fillQuestion`'s
  choices branch, a Quick Math round with choices on now prints its
  question once per question to the player's chat
  (`Quick Math — question N of 10: <question verbatim>`), gated by the
  holder's existing `ChoicePrintTracker` under key
  `qm|<index>|<question>|<choices>`. Plus tests
  (`TileTest.quickMathPrint`, +44 checks) and these records. Nothing else:
  the packs, the Geyser mappings, config, plugin.yml, and every other
  source file are untouched (pack/mapping hashes re-verified identical).
- **Modes, code-verified:** the print fires ONLY in choice rounds (kid
  tier, 3-choice on) — the one mode whose question never left the card.
  Typed rounds (Standard/Challenge; kid with choices off) already receive
  every question via `TextInputListener.promptInChat` at card open and
  after each answer, and carry no choices, so the print cannot fire there.
- **Rebuild:** build.sh run twice end-to-end → identical jar both times
  (and double-assembly asserted inside each run): **125,732 B, SHA-256
  `b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f`**.
  The shipped class file was byte-checked for the new key recipe
  (`qm|<idx>|<question>|<choices>`) and message text.
- **Rig re-run:** Paper 26.3-235 scratch rig with the add-on jar — enable
  line intact, `Done (26.803s)`, clean disable, **SEVERE 0**
  (`rig/boot.log` refreshed to this boot).
