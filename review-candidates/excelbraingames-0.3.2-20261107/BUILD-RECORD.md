# ExcelBrainGames 0.3.2 — BUILD RECORD

**Build ID:** EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f
**Built:** Nov 7, 2026 (America/Chicago), from `~/workspace/brain-games/build-0.3.2/`
**Jar:** `ExcelBrainGames-0.3.2.jar` — 130,850 bytes,
SHA-256 `8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511`
**Commission:** Mike's Bedrock Word Search report (Nov 7, 2026, in-game)
+ design ruling (same day). Spec: `WORDSEARCH-LAYOUT-INVESTIGATION.md`
+ `SPEC-DELTA.md` in this folder.
**Status at build close:** NOT installed. Install packet filed as a
gated MANIFEST entry (see INSTALL-PACKET.md); this build does not
touch the panel or the running server.

## What this build is

Word Search board geometry (the investigation's Option A). A round
now records the geometry it was dealt in (`core/WsGeometry`): JAVA
(the 0.3.1 map, proven cell-for-cell identical) or BEDROCK (a 5-wide
contiguous stream — cell at slot = its index — so the phone's Geyser
wrap displays the board as dealt). Bedrock starters get config-dealt
5-wide shapes (Kid 5x8, Standard 5x10, Challenge 5x10, in
`games.wordsearch.bedrock-tiers`; shapes inherit every tier term
except label/cols/rows), and the generator enforces Mike's length
ruling under Bedrock geometry (words longer than 5 never place E/W —
vertical/diagonal only, in every placement path). Rendering keys off
the ROUND's recorded geometry, so rounds resume unscrambled across
platforms and pre-0.3.2 saves load as JAVA (one `geometry` key
appended to the save map; old jars ignore it). Platform detection is
a Floodgate softdep behind a guarded bind (fleet pattern, one
touchpoint class); unknown platform deals JAVA. Everything else —
checker, payouts, the $1,500/day cap, Sudoku, the other four games,
the tile packs — is unchanged, and the packs prove it: both pack
hashes reproduce 0.3.1's exactly.

Authorship: `authors: [Iris, Mike]` per the settled credit rule.

## Baseline verification (0.3.1 tree → this build's starting point)

The build-0.3.1 tree was copied to build-0.3.2/; all work happened
there. Before copying, the tree was re-proven as the 0.3.1 source:

- The on-disk 0.3.1 jar re-hashed to the add-on build of record
  (125,732 B / b51d4c14…).
- A clean rebuild of the copied 0.3.1 tree (in /tmp) ran the full
  suite at exactly the recorded counts (636,700 checks, 0 failures)
  and produced a jar whose CLASSES are byte-identical to the jar of
  record; the single differing entry is plugin.yml's authors line
  (`author: Steve` in the shipped jar → `authors: [Iris, Mike]` in
  the deposited source — the Nov 5, 2026 authorship normalization,
  which postdated the jar build; +8 B descriptor, +4 B jar). 0.3.2
  ships the normalized line.

## Source delta vs the 0.3.1 tree

- NEW `core/WsGeometry.java` — the two slot maps + control-void rule.
- NEW `core/BuildInfo.java` — house fingerprint carrier (first
  fingerprinted BrainGames build; see FINGERPRINTS.md).
- NEW `bedrock/BedrockHook.java` — the only Floodgate touchpoint.
- `games/wordsearch/WordSearchRound.java` — geometry field +
  persistence (key appended last), generate() overload, per-word
  direction ruling (`dirsFor`) threaded through placeAll /
  completionAttacks / swapAttacks. The no-geometry generate()
  signature delegates with JAVA: carried tests compile and behave
  unchanged.
- `GameConfig.java` — parses `bedrock-tiers` (label/cols/rows only,
  everything else inherited), adds `wsTierFor(tierId, geometry)`.
- `GameService.java` — controlVoids delegates to the geometry (JAVA
  branch = the legacy rule); startWordSearch deals the starter's
  platform geometry + shape and logs both.
- `ExcelBrainGames.java` — guarded Floodgate bind in onEnable,
  `platformOf(Player)` (unknown → JAVA), enable line gains
  `platform-detection=` + `build=`.
- `gui/Views.java` — renderWs iterates the round's slot map (JAVA:
  the same slots in the same order as 0.3.1); the Word Search tier
  picker names the board the viewing player will be dealt.
- `plugin.yml` — version 0.3.2, `softdepend: [floodgate]`, 0.3.2
  description sentence.
- `config.yml` — one additive section: `games.wordsearch.bedrock-tiers`
  (the install packet merges ONLY this section into the live config).
- Tests: NEW `GeometryTest.java`, NEW `BedrockPropertyTest.java`.
  No carried test file modified.
- `build.sh` — 0.3.2 descriptor gate (version + softdepend +
  bedrock-tiers shape asserts), Floodgate input pinned by SHA-256,
  the two new suites, the fingerprint gate, jar name.

## Build inputs (all pinned in build.sh)

- Paper API 26.3 build 231 — SHA-256 f0df9e7f… (as 0.3.1).
- Vault 2.0.0 — SHA-256 8b2ce431… (as 0.3.1).
- Floodgate API jar (compile-only) — SHA-256
  21570aff9ce17d6983928e8552777760e1ede5050026b04c686b0ae112e6fd7e,
  copied from `~/workspace/autosort-plugin/libs/floodgate.jar` (the
  fleet's pinned copy) into `libs/floodgate.jar`.
- JDK: ~/workspace/.jdk25 (javac --release 21), as 0.3.1.

## Tests

1,869,695 checks, 0 failures — see TEST-REPORT.md. Carried suite at
EXACTLY its 0.3.1 counts (622,227) + TileTest (14,473): the
behavior-unchanged proof. New: GeometryTest (9,202) and
BedrockPropertyTest (1,223,793 — saturation 500/500 at full word
count on all 15 Bedrock tier × theme combos, stream traceability and
checker solves in every round).

## Determinism

build.sh ran to completion twice end-to-end (plus its internal
double assembly and double pack generation on every run): both runs
produced the identical jar (SHA-256 above) and identical suite
counts. Packs on every run: java 05942eed… / bedrock 486bcad6… —
0.3.1's recorded hashes, unchanged.

## Rig (Paper 26.3-235, the exact live build; logs in rig/)

- **Rig A (no Floodgate present)** — `rig/boot.log`: boot Done
  (21.964s), SEVERE 0; enable line reads `platform-detection=unavailable
  (Java geometry for all), build=EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f`.
  The unknown-platform default, proven at plugin level.
- **Rig B (live platform pair present)** — `rig/boot-floodgate.log`:
  Geyser 2.12.1 (the Nov 6, 2026 download, live line) + Floodgate
  2.2.5-b141 (the live-confirmed build) added to the same rig: boot
  Done (31.188s), SEVERE 0; log carries `floodgate detected —
  Bedrock players get the 5-wide Word Search board.` and the enable
  line reads `platform-detection=floodgate`. The bind path, proven
  against the live Floodgate build.
- Rig WARN noise is the sandbox's offline Mojang session service
  (present in 0.3.1's rig too); economy=NONE is a rig artifact
  (no economy provider in the rig), not a defect.

## Records

Package staged at
`~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.2-20261107/`
with PUSH-NOTES.md (draft PR + Drive legs are the parent/board-manager's).
FILE-LIST.txt + SHA256SUMS in this folder cover the build tree
(excluding work/ and the rig server internals; SHA256SUMS hashes
every file FILE-LIST.txt names).
