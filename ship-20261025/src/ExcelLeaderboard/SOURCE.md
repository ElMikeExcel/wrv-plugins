# ExcelLeaderboard 1.3.0 — Source & Build Record

Built Oct 18, 2026 (WRV-080, Mike's request: GriefPrevention claim
stats on the leaderboard + website claims explainer).

## Artifact

- `ExcelLeaderboard-1.3.0.jar` — SHA-256
  `8cf1343e3712191092428326d4a9329fc54f6af0eb31bad18bda3068f388b8ce`
- Rebuild reproduction: two consecutive clean `./build.sh` runs
  produced the identical SHA-256 (fixed jar entry timestamps).
- Compiled with JDK 25 against the exact Paper API 26.2 build 123 the
  live server runs (runtime-server libraries).

## What changed vs 1.2

- New board: **Land Claimed** (Category.CLAIMS), a soft dependency on
  GriefPrevention, probed reflectively in SoftAdapters exactly like
  the mcMMO/Jobs/EliteMobs boards — no compile-time GP dependency, and
  the board hides itself if the API does not verify.
- New pure core `ClaimBlocksMath`: claimed = summed area of top-level
  claims in claims-enabled worlds (subdivisions excluded, matching how
  GP itself counts); total = accrued + bonus; available = total -
  claimed.
- Snapshot gained a per-category details map so a board row can carry
  a second number (still-claimable). All other boards render
  byte-identically to 1.2.
- plugin.yml: version 1.3.0, `GriefPrevention` added to softdepend.

## Upstream credit

- **GriefPrevention 16.18.7** by RobbieTech / the GP team — upstream
  plugin, used unmodified. ExcelLeaderboard only READS its public API
  (GriefPrevention.instance, DataStore.getPlayerData,
  PlayerData.getAccruedClaimBlocks / getBonusClaimBlocks / getClaims,
  Claim.getArea / getLesserBoundaryCorner / parent,
  GriefPrevention.claimsEnabledForWorld). API use only; no GP code is
  copied or shaded. GriefPrevention stays credited under its own name;
  ExcelLeaderboard is our own Excel plugin.
- API signatures verified by javap against the installed
  GriefPrevention-16.18.7.jar and guarded at build time by
  LiveApiSignatureTests.

## Tests (all passing, 188 assertions)

BoardMathTests 25 · LedgerTests 10 · VaultModeTests 6 ·
LiveApiSignatureTests 15 (mcMMO + Jobs + GriefPrevention probes
checked against the real installed jars) · LayoutTests 42 ·
ChatMenuTests 31 · FormContentTests 28 · ClaimBlocksMathTests 18 ·
ClaimsPresentationTests 13.

## Scratch-rig boot

Paper 26.2-123 + GriefPrevention 16.18.7 (seeded with a copy of real
GP data: config, 5 player records, 6 claims) + ExcelLeaderboard
1.3.0: clean boot, zero SEVERE, GP loaded its data in File Mode, and
the first refresh logged "ExcelLeaderboard: Land Claimed category
available (GriefPrevention)." Rig kept at
`work/ExcelLeaderboard-1.3/scratch-rig/` (boot2.log).

## Status

Built, tested, rig-booted, deposited. NOT installed live (installs
on the next empty-server window per Mike's standing rule; jar swap +
restart, rollback = the live 1.2 jar).
