# ExcelBrainGames 0.3.2 — source of record

**Plugin:** ExcelBrainGames · **Version:** 0.3.2
**Build ID:** EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f
**Jar:** ExcelBrainGames-0.3.2.jar — 130,850 bytes — SHA-256
`8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511`
(the jar itself is NOT inside this package — it is deposited
separately per the publication route in PUSH-NOTES.md; its hash is
recorded here and in SOURCE-PROVENANCE.json)
**Authors:** Iris / Mike
**Build folder (complete, self-contained):** `~/workspace/brain-games/build-0.3.2/`

## What this package is

The complete 0.3.2 build record: BUILD-RECORD.md (what/why/proof),
SPEC-DELTA.md (the design), TEST-REPORT.md (the results),
INSTALL-PACKET.md (the gated install), FINGERPRINTS.md (the house
fingerprint — 0.3.2 is BrainGames' first fingerprinted build),
build.sh (the reproducible build), src/ + tests/ (the full trees),
config.yml + plugin.yml (the shipped descriptors), wordlists/ (the
shipped lists), packs/ (the tile-pack generator + validator —
unchanged from 0.3.1; the packs themselves are byte-identical to
0.3.1's and hash-recorded in SOURCE-PROVENANCE.json),
menu-entry/braingames.yml (the compass door — unchanged), and rig/
(both boot logs: no-Floodgate and live-platform-pair).

## Baseline and delta

Copied from `~/workspace/brain-games/build-0.3.1/` after the baseline
re-verification recorded in BUILD-RECORD.md (full suite at recorded
counts; classes byte-identical to the 0.3.1 jar of record; the only
delta is plugin.yml's authors line — the Nov 5, 2026 authorship
normalization). Source delta vs 0.3.1: NEW core/WsGeometry.java,
core/BuildInfo.java, bedrock/BedrockHook.java; changed
WordSearchRound, GameConfig, GameService, ExcelBrainGames,
Views, plugin.yml, config.yml (one additive section); NEW tests
GeometryTest + BedrockPropertyTest; no carried test modified.

## Build inputs (hashes in SOURCE-PROVENANCE.json)

Paper API 26.3 build 231 (compile), Vault 2.0.0 (compile), and the
Floodgate API jar (compile-only) at `build-0.3.2/libs/floodgate.jar`
— pinned by SHA-256 in build.sh, sourced from the fleet's pinned
copy at `~/workspace/autosort-plugin/libs/floodgate.jar`. Like the
other inputs it lives outside this package, per the 0.3.1 pattern.
