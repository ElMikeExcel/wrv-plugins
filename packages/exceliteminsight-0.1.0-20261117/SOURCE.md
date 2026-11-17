# ExcelItemInsight 0.1.0 — SOURCE.md

Ownership: 100% ours. ExcelItemInsight is an original plugin
written for Wink Reyes Valley (no upstream code). Compile-time
link: the Paper API only (26.2). No runtime dependencies, no
soft depends — it enables on a bare Paper server.

## Release state

Version 0.1.0 is a PRE-RELEASE build: built and rig-verified on
2026-10-20, never installed on the live server. It ships as a
fresh entry in the install queue (see INSTALL-PACKET.md). Mike
asked for it on Oct 20, 2026 after Jester (Jester_Turtle) asked
why his Sharpness V diamond sword "only does 7 attack damage" —
the vanilla tooltip shows base damage only; the plugin puts the
real numbers in the item's lore.

## Research credit (Descriptioner, MIT)

The pre-build research (`~/workspace/iteminsight-research/
REPORT-2026-10-20.md`) evaluated Descriptioner (by joshinyx,
MIT License) as the nearest existing plugin. It does not fit:
its explanation lookup never receives the enchantment level, so
it cannot print per-level numbers; it hides the vanilla
enchantment display; and it puts its block on top of the lore.
This plugin rejects all three behaviors by design. The research
pass read Descriptioner's en_US.yml as a plain-language
reference under its MIT license; no code was copied and no
wording was adopted — every ExcelItemInsight line is original
and carries per-level numbers Descriptioner does not have.
Credit is recorded here per the MIT spirit; the license applies
to Descriptioner's own work, none of which is included.

The big enchant plugins (EcoEnchants, AdvancedEnchantments,
ExcellentEnchants) were also evaluated: they describe only their
own custom enchantments, not vanilla ones. Not used.

## Ground truth for the numbers

Every figure was verified against the Minecraft Wiki AND the
pinned data of the server's own jar (Paper 26.2 build 123):
all 43 `data/minecraft/enchantment/*.json` definitions were
extracted from the bundled vanilla jar and kept at
`~/workspace/iteminsight-research/pinned-enchantments-26.2/`;
the per-number citation table is in BUILD.md. Where wiki prose
and pinned data disagreed (Thorns damage range), the pinned
data won. The pinned jar also settled the registry count: 43
enchantments — 26.2's new spear enchantment, Lunge, is the one
Descriptioner's "42" misses.

## Artifacts (built 2026-10-20, America/Chicago)

| Artifact | Size | MD5 | SHA-256 |
|---|---|---|---|
| ExcelItemInsight-0.1.0.jar | 24,137 | d0d3bba427a5bb1dac2e8200424eb574 | 5f300b4be8317ab2e93889be9c30499c23e422bad972825b2953d67add9ccf13 |
| ExcelItemInsight-0.1.0-source.zip | 24,661 | 52d1d4356390dcc00eee22a702f968fe | 4722d84586c583fdb732ea0307a61f3034eff45fed75aac916604434ecb3c5f5 |

Rebuild reproduction: `./build.sh` twice = identical SHA-256;
clean-room rebuild from this source zip = identical again, with
all 960 unit checks passing from the clean room. Tests:
`./run-tests.sh` (plain JVM over the Bukkit-free core; the
dictionary-completeness test reads `pinned-enchantments-26.2.txt`,
which ships in the zip and was generated from the pinned jar
extraction above).

Source layout: `src/ExcelItemInsight/` — `build.sh`,
`run-tests.sh`, `plugin.yml`, `config.yml`,
`pinned-enchantments-26.2.txt`, `src/` (plugin + `core/`:
EnchantDictionary, LoreBlock, PinnedEnchantments), `test/`
(TestMain, TestDictionary, TestLoreBlock, Check). The rig driver
lives in the project folder under `scratch/driver/` and is not
part of this zip; the scratch server and boot logs are under
`scratch/` in the project folder.

<!-- ExcelItemInsight — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel) -->
