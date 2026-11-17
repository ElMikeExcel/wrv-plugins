# ExcelPartyDrops 0.1.0 — Source & Provenance

**Plugin:** ExcelPartyDrops (display name "Excel Party Drops")
**Version:** 0.1.0 — first release
**Author:** WinkReyesValley (Excel signature — MikeExcel)
**Spec:** `~/workspace/mob-scaling/PARTY-DROPS-SPEC-2026-10-20.md` (approved by Mike, Oct 20, 2026)
**Target:** Paper 26.2 (build 123), Java 21+ bytecode, EliteMobs 10.9.x (softdepend)

## Ownership

**This plugin is 100% our own code.** Every class in the jar was written for
Wink Reyes Valley for this plugin. No upstream plugin code is bundled, shaded,
or copied.

Upstream relationships, stated precisely:

- **EliteMobs (MagmaGuy)** — *compile-time API reference only.* The build
  compiles against the public EliteMobs API classes in the pinned jar
  `EliteMobs-10.9.8.jar` (the live-baseline version at build time; the same
  API surface verified identical in 10.9.10 by javap). `plugin.yml` declares
  `softdepend: [EliteMobs]`; at runtime the plugin links against whatever
  EliteMobs the server runs. **No EliteMobs code is inside our jar.** The
  only EM types referenced are `EliteMobDeathEvent`, `EliteEntity`, and
  `CreatureSpawnEvent.SpawnReason` (Bukkit). EliteMobs keeps its author's
  name and credit in full — this is not a fork and not a modification of it.
- **ExcelTogether (ours)** — the 50-block participant-range math mirrors
  ExcelTogether's `PartyBonus.within()` exactly (same-world check +
  squared-distance ≤ radius²). ExcelTogether exposes no callable API, so
  the two-line formula was re-implemented in `core/PartyRange` and a
  conformance unit test pins the boundary semantics (inclusive at exactly
  50.0) so the two implementations cannot drift silently.
- **Paper API + Kyori Adventure 4.20** — compile-time only, provided by the
  server at runtime; nothing shaded.

## Artifact table

| Artifact | Bytes | MD5 | SHA-256 |
|---|---|---|---|
| `ExcelPartyDrops-0.1.0.jar` | 14,731 | `590d97fe78e7b03d74bcfcd5fb8659b8` | `afae735b991d683cf45f4aa30187f51aaf53bf2b7b114761d4279979beba6196` |
| `ExcelPartyDrops-0.1.0-source.zip` | 29,765 | `c65b451ebd39349caaa4c8aea7fc0a4d` | `87ae13d90a136f02ab78a9ad49a4d13ac6880b743770121d8d9f0d1696a64c5d` |

Drive deposits (Minecraft WRV): jar in "Build files", source zip + this
SOURCE.md in "Source" — file IDs recorded in BUILD.md and the manifest entry.

## Source zip layout

- `src/ExcelPartyDrops/` — the plugin project exactly as built:
  `build.sh`, `run-tests.sh`, `plugin.yml`, `config.yml`, `src/…` (plugin +
  pure-logic core), `tests/…` (plain-JVM check harness, no framework).
- `rig-driver/` — the scratch-rig test driver (`PdRigDriver.java` +
  `build-driver.sh`). **It never ships.** It exists so the rig proofs in
  BUILD.md can be re-run; it is not part of the plugin.

## Reproduction

Unzip anywhere, then (scripts may lose their execute bit in the zip —
call them through bash):

```
cd src/ExcelPartyDrops
bash build.sh      # writes ../../ExcelPartyDrops-0.1.0.jar
bash run-tests.sh  # 94 checks, plain JVM
```

Build inputs: JDK 25 (any JDK 21+ works; `--release 21`), the house
build-libs Paper API + Kyori set, and the pinned EliteMobs jar for its API.
The jar is built deterministically (`jar --create --date=2026-10-20T00:00:00-05:00`):
two in-tree builds and a clean-room build from this source zip all produced
SHA-256 `afae735b…` byte-identical (verified Oct 20, 2026 — see BUILD.md).

## What it does (one paragraph)

When a natural EliteMobs elite dies, the players who were alive, in the
same world, and within 50 blocks of it at the moment of death are counted
(distinct real players, cap 5). The elite's vanilla drop list — and only
that list — is scaled by 1 + 0.5 × (participants − 1), capped at ×3.0,
with per-stack probabilistic rounding that is exact in expectation.
Spawner/egg/bred/raid/command/custom-origin elites and anti-exploit-flagged
elites pay vanilla rates. EliteMobs' own generated gear and coin showers
never pass through the vanilla drop list and are untouched, as are dropped
XP, Jobs, and mcMMO. A quiet per-player action-bar hint ("Hunting together
pays off — the group earned extra drops.") shows on the first multiplied
kill and at most once per 10 minutes per player after that. Details and
proof: BUILD.md.

// ExcelPartyDrops — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
