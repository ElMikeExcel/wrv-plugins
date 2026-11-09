# ExcelClaims — source & provenance

**Product:** ExcelClaims 0.1.0 (Wink Reyes Valley)
**Authors:** Iris / Mike
**Build:** EXC-CLAIMS-0.1.0-WRV-20261107-01-7c1a3f
**License/ownership:** Original work of the authors, written for the
Wink Reyes Valley server. ExcelClaims is the house claims plugin; it
replaces the third-party GriefPrevention plugin (by RoboMWM /
TechFortress and contributors) in the server's plugin set.

## Clean-room basis (read this first)

ExcelClaims is a **clean-room reimplementation**. It was written from:

1. **Observable behavior** of the claims system as configured and used
   on Wink Reyes Valley — what players and staff actually experience:
   the shovel/stick workflow, the trust tiers, claim-block accrual,
   expiration, and the protection rules (see INVENTORY.md).
2. **The public API surface** our own plugins link against — method
   and field *signatures* verified with `javap` against the pinned
   GriefPrevention 16.18.7 jar the server ran (SHA-256
   45e9907c… — the same signature-verification method the house uses
   for ExcelLeaderboard's live-API tests), plus the call sites in our
   own source code (ExcelLandmarks, ExcelAutoSort, ExcelProtect,
   ExcelAuditLog, TreeCleanup, Guide, Leaderboard, Shops, BossTrial,
   the menus, and Magic's published integration class).
3. **The live configuration** captured from the running server
   (Oct 15, 2026 full capture; Oct 18, 2026 data pull) — every parity
   value in BUILD-RECORD.md traces to those captures.
4. **The on-disk data formats** — `ClaimData/<id>.yml`,
   `ClaimData/_nextClaimID`, and the plain-text `PlayerData/<uuid>`
   files — as read from the server's own backups. Formats are facts,
   and the importer/exporter implement them so migration is lossless
   in both directions.

**No GriefPrevention source code was copied, decompiled, or adapted.**
No GriefPrevention source tree exists in this build's working area;
the only GP artifact consulted is the compiled 16.18.7 jar, used for
`javap` signature dumps (its public API surface) — signature
reference only. Every method body in this source tree — the engine
(`com.winkreyes.excelclaims`) and the compatibility facade
(`me.ryanhamshire.GriefPrevention`) alike — is original code written
for this project from the references above. Where behavior had to be
interpreted rather than read (e.g. the precedence
`Claim.getPermission` reports for a player holding several trust
grants at once), the interpretation and its evidence are documented
in BUILD-RECORD.md.

## Why the facade package exists

Eleven house/third-party consumers link, probe, or reflect against
the `me.ryanhamshire.GriefPrevention` API (INVENTORY.md: 35
touchpoints). ExcelClaims ships original facade classes in that
package, and its plugin.yml registers the Bukkit plugin name
"GriefPrevention", so every consumer keeps working with **zero
rebuilds** — including Magic, whose integration activates on
`plugin instanceof GriefPrevention`. The facade is a compatibility
layer over the ExcelClaims engine, not a port of anyone's code.
BUILD-RECORD.md documents the approach per consumer and the rejected
alternative.

## Layout

- `src/com/winkreyes/excelclaims/` — the engine: claim/player model,
  flat-file store (the GP core keys, plus clearly-separated Excel
  bookkeeping keys), trust resolution, claim service, protection and
  tool listeners, commands, config, importer/exporter, accrual.
- `src/me/ryanhamshire/GriefPrevention/` — the API facade: plugin
  main class, Claim, DataStore, PlayerData, ClaimPermission, claim
  events.
- `tests/` — JUnit suite (trust matrix, claim rules, accrual,
  import/export round-trip, store round-trip, facade signatures).
- `linkcheck/stubs/` — compile-only stand-ins for two consumers'
  internal classes, so their real GP adapter files can be
  compile-checked unchanged against the facade (see local-build.sh).
- `rig/` — the Paper 26.3 test server, console driver, and the
  boot-2 facade-smoke driver plugin source.
- `local-build.sh` — deterministic build: YAML gate → JUnit →
  consumer link-check → double-built jar (byte-identical) →
  fingerprint + identity gates → source zip.
- `libs/` — compile-time dependencies only (Paper API is referenced
  from its house location; the ExcelProtect API jar is Protect's own
  published API surface, used for the audit-sink call).

## Build

`./local-build.sh` — requires the house JDK (`~/workspace/.jdk25`)
and the Paper 26.3 API jar; prints SHA-256s for the jar and the
source zip.

## 0.1.3 addendum (Nov 8, 2026)

**Product version:** ExcelClaims 0.1.3 (corner pillars — taller, more visible claim corners)
**Build:** EXC-CLAIMS-0.1.3-WRV-20261108-04-bd9ff4
**Jar:** ExcelClaims-0.1.3.jar — 102,334 B, SHA-256 21f3f7999c36259b6db9f3cc79208501b4ebbf47372a06db567ce2d3a406fbd7 (Drive holds the binary; not committed to git)
**Src zip:** ExcelClaims-0.1.3-src.zip — 119,864 B, SHA-256 99d2150b4765d85685c842505baa5a40ac42f2d68bf54cc3556abf5d2a1bdac9 (Drive holds the binary)
**Ownership:** Iris / Mike, same clean-room basis as above.
**Repo:** ElMikeExcel/wrv-plugins, package packages/excelclaims-0.1.3-20261108/ (draft record PR, never merged)
