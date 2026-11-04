# ExcelMagicLevel 0.2.0 — SOURCE

**What this is:** our own plugin, written for Wink Reyes Valley from
`~/workspace/magic-level/SPEC.md` (Phases 0–2). No upstream code is
vendored; the build compiles against three provided jars only.

Phase history: 0.1.0 (Phases 0–1 — 20-level table, earn/levels/boosts),
jar 52,048 bytes, SHA-256
`bb2e9d8fd8c60c7075dfe968018eb7e5a38b80a8ca9028aade7794fc33735516`,
fragment SHA-256
`898e6c0264a4aa551435839ac0afe959af66072d4a3dc8f7fc42430d27f228d3`.
0.2.0 (this deposit) adds the 1000-level even-spread table, the Together
party share, and the Phase 2 surfacing per SPEC §10 as amended
Oct 31, 2026.

## Ownership split

| Piece | Owner | Source |
|---|---|---|
| ExcelMagicLevel (all of `src/`) | Excel (Wink Reyes Valley) | this folder |
| Magic API (compile-time only) | elmakers (Nathan Wolf) | Magic 11.2.5 jar, SHA-256 `79e49fe32fdc4e25375c0099d2bd10665b9792bfd9b35fab8d64a498ebfc3532`, from `~/workspace/magic-scratch/downloads/Magic-11.2.5.jar`; API surface verified against the 11.2.5 source tree at `~/workspace/magic-bedrock-aim-audit/src/MagicPlugin-baa3d91540a35de636a44c6241416eff5e861059/` |
| Paper API (compile-time + tests) | PaperMC | 26.3.build.231-stable, from the EliteMobs rig library set (`~/workspace/elitemobs-10.9.12/rig-boot/libraries`) |
| PlaceholderAPI (compile-time only) | HelpChat (extendedclip) | 2.12.3 jar from `~/workspace/menu-2.0/rig/plugins/PlaceholderAPI-2.12.3.jar`; runtime stays optional (`softdepend`, expansion registered only when present) |
| JUnit 4.13.2 + hamcrest 1.3 (tests only) | JUnit | `~/workspace/m2-repo` |
| ExcelTogether 0.4.3 (**examined, not linked**) | Excel (Wink Reyes Valley) | jar SHA-256 `2bd4d99832ef06e03352cec8467e2fb530f5ba4094db3e0ff831a1d2a59f68aa` (Shared-XP bundle deposit); surface verdict + the designed API addition: `TOGETHER-API-NOTE.md` |

Runtime: `depend: [Magic]` — the plugin does not enable without Magic
(the one correct hard edge, SPEC §9.2). No mcMMO code is linked, loaded,
or imported anywhere; `core`/`store` are mechanically forbidden from
referencing it (PackageDependencyTest). ExcelTogether is read through
Bukkit's ServicesManager only (a PartyLookup service registration);
no Together class is referenced.

## Layout

- `src/main/java/.../core/` — pure engine: curve derivation, XP payment
  math, anti-abuse windows, weight table, share copy math + share
  recipient rules, the modifier template GENERATOR, the mcMMO-shaped
  export. No Bukkit/Magic imports.
- `src/main/java/.../store/` — players.yml persistence (debounced saves,
  .bak rotation, corrupt-file preservation).
- `src/main/java/.../adapter/magic/` — every Magic API touchpoint:
  CastEvent listener, MageLoad reconcile, boost applier, drift check.
- `src/main/java/.../adapter/together/` — the party-share seam:
  `PartyLookup` (the service contract), `BukkitServicePartyLookup`
  (real party mode), `ProximityPartyLookup` (the labelled degraded
  fallback, alive until ExcelTogether publishes the service),
  `NoOpPartyLookup`.
- `src/main/java/.../adapter/papi/` — the PlaceholderAPI expansion
  (%excelmagiclevel_*), loaded only when PAPI is present.
- `src/main/java/.../adapter/mcmmo/`, `adapter/weekend/` — replaceable
  seams (house build style). mcMMO is a no-op in v1 by design (SPEC §9);
  the weekend signal mirrors ExcelWeekendXP's actual Sat/Sun
  America/Chicago window.
- `src/main/java/.../surface/` — /magiclevel command (status incl. the
  share line, quiet, top, admin export/strip/strip-all) + feedback.
- `src/gen/java/...` — build-time template generator entry point; parses
  the shipped config.yml and emits `modifiers-magiclevel-fragment.yml`.
  NOT packaged in the jar.
- `rig-driver/` — rig-only fake-player driver, extended in Phase 2 with
  a second fake player (RigBuddy) for the share proofs (never ships).
- `rig2-stub/` — rig-only PartyLookup provider proving the service
  branch selects party mode (never ships).
- `rig/` — the Phase 0–1 Paper 26.3 + Magic 11.2.5 proof rig.
- `rig2/` — the Phase 2 proof rig (same stage + PlaceholderAPI, Jobs,
  CMILib, ExcelTogether 0.4.3; boot logs kept).

## Build

`./build.sh` — offline, pinned local jars, fixed jar timestamp.
Reproducible: consecutive builds are byte-identical (verified ×2).
Jar: `ExcelMagicLevel-0.2.0.jar`, 63,270 bytes,
SHA-256 `892e05fc5d4176b82fd30117098f8c037cb4e3e56325b9cdc0d1f70cd80231c3`.
Generated fragment: `modifiers-magiclevel-fragment.yml`, 257,337 bytes,
SHA-256 `f41553d72d92ee28fe18e45a50760da887fb88ab16ea5467c8c0b38dc7732140`.

Excel — Wink Reyes Valley.
