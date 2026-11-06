# ExcelBrainGames 0.3.1 "Letter Tiles" — TEST REPORT

Build: Nov 5, 2026, `~/workspace/brain-games/build-0.3.1/build.sh`.
Toolchain: JDK 25 (`~/workspace/.jdk25`), `javac --release 21`, against the
pinned Paper API 26.3 build 231 jar (SHA-256 `f0df9e7f…`) and Vault 2.0.0
(`8b2ce431…`) — both pin-checked inside build.sh before compiling.

## Logic suites (all checks, 0 failures)

| Suite | Checks | Note |
|---|---|---|
| WordSearchTest | 20,416 | carried — identical to 0.3.0 |
| MemoryTest | 3,039 | carried — identical |
| SudokuTest | 553 | carried — identical |
| SudokuPropertyTest | 15,006 | carried — identical (500 seeds × 5 tiers) |
| TextTest | 103 | carried — identical |
| TextPropertyTest | 83,244 | carried — identical |
| LedgerTest | 55 | carried — identical |
| PropertyTest | 499,811 | carried — identical |
| **Carried subtotal** | **622,227** | **= the 0.3.0 baseline, count for count** |
| TileTest (new) | 14,473 | below (14,429 in the first 0.3.1 build + 44 add-on) |
| **Total** | **636,700** | **0 failures anywhere** |

The carried counts matching 0.3.0 exactly is the behavior-unchanged proof:
every game-logic, payout, generator, and save check that 0.3.0 passed,
0.3.1 passes with the same numbers.

## TileTest (new in 0.3.1)

- **Key space:** TileKeys yields exactly 205 unique sorted keys (26×3 Word
  Search, 9×3 Sudoku, 100 numbers); uppercase letters lowercase; invalid
  letters/digits/values throw.
- **Label mapping:** `numberModelFor` tiles "0", "7", "40", "99"; returns
  null (item left exactly as 0.3.0) for "100", "-3", words, blank, null.
- **Manifest cross-check:** the generated `tiles-manifest.json` names
  exactly TileKeys' 205 keys — the packs and the code cannot drift apart
  without failing this build.
- **Kid Quick Math bound:** 2,000 generated kid rounds — every choice value
  in [0, 99] and tileable; **observed max 40** (the design bound).
- **ChoicePrintTracker:** first prompt announces; re-renders of the same
  prompt stay silent (twice); the next word announces; null never does.
- **Quick Math question print (add-on, +44 checks):** a generated kid
  choice round walked end-to-end — every question's view key
  (`qm|<index>|<question>|<choices>`) announces on first render and stays
  silent on re-render (20 checks); identical question text announces again
  at the NEXT index but not at the same one (3); Standard, Challenge, and
  kid-choices-off rounds carry no choices at any index (21) — the
  choices-branch print cannot fire in typed modes.
- **SudokuTextGrid:** at 4×4, 6×6, and 9×9 — line count, equal line lengths
  (the square holds), EVERY cell verified at its computed square position
  (`[d]` given / ` d ` placed / ` . ` blank), and a placed digit re-renders
  plain at the same slot.

## Pack validation (`packs/validate_packs.py`)

`PACK VALIDATION OK: 205 tiles x2 packs, mappings 205, worst contrast 4.40 (>= 4.0)`

- Every JSON in both packs parses; every Java item definition → model →
  texture reference resolves; every PNG is 64×64.
- Bedrock manifest: format_version 2, fixed header/module UUIDs, resources
  module. item_texture.json carries BOTH key forms per tile
  (full identifier + Geyser-derived shorthand), all resolving.
- Geyser mappings: format_version 2; 205 definitions, each exactly once,
  grouped under the base item the view actually stamps, bedrock identifier
  and explicit icon matching the manifest.
- Java and Bedrock texture PNGs are byte-identical per tile.
- Zips' entry lists match their trees; `java-pack.sha1` matches the zip.
- Contrast: WCAG ratio of glyph on background ≥ 4.0 for all 205 tiles;
  worst is the Quick Math emerald (white on green) at 4.40.

## Reproducibility

- Jar assembled twice inside build.sh → identical SHA-256 (asserted).
- Packs generated twice inside build.sh → identical zip SHA-256s (asserted).
- build.sh run a second time end-to-end → identical jar SHA-256. First
  0.3.1 build: `45b178a7…` (125,602 B). **Add-on build (jar of record):**
  `b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f`
  (125,732 B) — same double-assembly + double-run determinism. The packs
  and Geyser mappings were NOT touched by the add-on: their SHA-256s are
  unchanged (java zip `05942eed…`, bedrock `486bcad6…`, mappings
  `8ad2a15c…`).

## Visual QA (human-eyeball pass on generated tiles)

Opened and inspected: `ws/q_normal.png` (clean Q on cream), `number/42.png`
(white 42 on emerald), `sudoku/7_given.png` (navy 7 on light blue). Glyphs
exact, framed, no floating or garbled pixels.

## Rig boot

Scratch server (`rig/server/`, log `rig/boot.log`): Paper **26.3-235** —
the exact live build, PaperMC-checksum-verified — plus Vault 2.0.0 and
ExcelBrainGames-0.3.1.jar:

- `Loading Paper 26.3-235-main@bb10cf0 … for Minecraft 26.3`
- `ExcelBrainGames 0.3.1 enabled: games=[word-search, memory-match, sudoku,
  quick-math, word-scramble, riddle], kid-tiers=ENABLED,
  daily-cap=$1,500.00, wordlists=10 files / 6 themes, riddles=16`
  (economy reads NONE — the scratch rig has no economy provider plugin;
  ledger-only is the plugin's designed fallback, not a defect)
- `Done (32.352s)`, clean disable on stop, **SEVERE count: 0**.
- **Add-on re-run (jar of record):** same rig, new jar swapped in —
  enable line intact verbatim, `Done (26.803s)`, clean disable on stop,
  **SEVERE count: 0**; `rig/boot.log` now holds this boot.

## What tests cannot prove (→ player proofs in INSTALL-PACKET.md)

Real-client rendering: that a Bedrock client with Geyser's pack actually
draws the tiles, what a pack-DECLINED Bedrock client sees, that a Java
client with the server pack draws them, and the chat square's readability
on a phone. All named as player proofs in the install packet.
