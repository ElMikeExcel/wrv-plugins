# ExcelBrainGames 0.3.1 "Letter Tiles" — source package

Text-only records package (staging copy; the parent publishes). The jar and
the two pack archives are NOT inside this package — they are pinned by hash
in SOURCE-PROVENANCE.json and live in the build folder:
~/workspace/brain-games/build-0.3.1/ (jar at its root; packs under
packs/dist/, the Geyser mappings at packs/dist/geyser/braingames-tiles.json).

Contents: the complete 0.3.1 source tree (src/), the full test tree
(tests/, including TileTest), the tile generator + pack validator
(packs/*.py — the 205 tile PNGs in both packs are reproducible byte-for-byte
by running generate_tiles.py; the font is defined in that file), build.sh
(pinned Paper API + Vault inputs, deterministic double assembly), the
shipped plugin.yml/config.yml, the rig boot log (rig/boot.log, Paper
26.3-235), and the build's BUILD-RECORD / SPEC-DELTA / TEST-REPORT /
INSTALL-PACKET.

Rebuild: run build.sh in a copy of the build folder; the jar must come out
at SHA-256 b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f
(125,732 B — the add-on build, which added the Quick Math kid question
chat print to gui/Views.java and its TileTest checks; the first 0.3.1
build's 45b178a7… jar is superseded).
