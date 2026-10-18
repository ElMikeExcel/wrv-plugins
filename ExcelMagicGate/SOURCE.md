# ExcelMagicGate 0.1.0 — source & provenance (deposit record)

Per the WRV deposit-at-build standard (Mike, Oct 17 2026): every plugin
build deposits its full kit — jar, our source, upstream source pinned
by version/commit — with this file recording hashes and ownership.

## This build

| Item | Value |
|---|---|
| Plugin | ExcelMagicGate 0.1.0 |
| Jar | `ExcelMagicGate-0.1.0.jar`, 149,848 bytes |
| Jar SHA256 | `2bc272218d11d8230d54fbf909bd2587e294d6052ba300206e92fd95d237b0ef` |
| Rebuild reproduction | Two clean builds byte-identical (same SHA) |
| Source | `src/ExcelMagicGate/` (6 Java sources, tests, build scripts, bundled stage packets) |
| Automated tests | 107 checks, 0 failures (`run-tests.sh`) |
| Built | 2026-10-17, JDK 25 (`--release 21`), Paper API from the shared WRV build-libs |

## Ownership

- **Ours (original work in this repo):** all of
  `src/ExcelMagicGate/src/com/winkreyes/excelmagicgate/` (the bridge
  plugin + pure-logic core), `tests/`, `build.sh`, `run-tests.sh`,
  `plugin.yml`, `config.yml`, this file, README, INSTALL-RUNBOOK.
- **Ours (WRV-070 packets, bundled as data):** `stages/stage0..stage5/`
  are the Magic config packets authored for WRV (from
  `~/workspace/magic-scratch/phase1-packet` and `seal2..seal5-packet`),
  copied verbatim except: each packet's
  `luckperms-default-group-grants.txt` ships under the name
  `grants.txt` (content untouched), and stage 0 is derived from the
  Phase 1 packet as documented in `stages/stage0/wands.yml` and
  `stages/stage0/grants.txt` (rank wands removed; grants emptied).
  Hidden base templates inside the packets' `wands.yml`/`classes.yml`
  are copied verbatim from Magic's shipped defaults (NathanWolf) — the
  packet headers say so in place.
- **Not ours — upstream dependencies (not vendored into this jar):**
  - **Magic** by NathanWolf (elBukkit/MagicPlugin), version 11.2.5,
    commit `baa3d91540a35de636a44c6241416eff5e861059`. Jar SHA256
    `79e49fe33f9d4d7bc51f1e2061a0faa27cbe7277f04d9bbea6bca0e1ba2b56e87`.
    Upstream source already deposited with the WRV-070 Magic kit
    (Drive "WRV-070 Magic" folder; local copy
    `~/workspace/magic-scratch/drive-source-deposit/`,
    `MagicPlugin-11.2.5-source.zip` SHA256
    `c47ea97141d51a8e40c1e186cfed30f77ea92e6da911e93721894e6b26e04715`).
    The bridge uses only Magic's documented console command
    (`magic reload`) and its on-disk config files.
  - **ExcelGuildGate 0.1.0** — ours, separate project
    (`~/workspace/excelguildgate/`); the bridge reads its public
    `GuildGateAPI` by reflection, exactly as that API's javadoc
    prescribes. No GuildGate code is copied into this jar.
  - **LuckPerms 5.5.87** (upstream, lucko) — driven through its own
    console commands; nothing vendored.
  - Paper API (compile-time only) from the shared WRV build-libs.

## Deposit record (WRV Minecraft file home) — deposited 2026-10-17

- Jar → Drive "Build files" (`1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI`):
  file ID `1l83lCCuzj2S2TwF0O6pYRIcEUnWmzmEM`, 149,848 bytes,
  MD5 `2f4e438397d0499a225b1a589ea1e2d8` — matches local.
- Source zip → Drive "Source" (`1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz`):
  file ID `1maxmVLPLvJICF-dX0uXF4bM6G4HwQbpp`, 149,729 bytes,
  MD5 `4561f5552f1b1a3de76d25cef5d51f7b` — matches local.
  Zip SHA256 `c862a2348cdd7126918ae5a4546c63499dc1af06ad8b35dcc6e20d2f03a3cdcd`.
