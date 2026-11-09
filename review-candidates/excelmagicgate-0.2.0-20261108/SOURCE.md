# ExcelMagicGate 0.2.0 — SOURCE

- Repo: ElMikeExcel/wrv-plugins (record PR is a parent leg — draft, never merged)
- Source of record: this build folder, ~/workspace/magicgate-floor-build/src/ExcelMagicGate/
  (src/ + tests/ + stages/ + build.sh + run-tests.sh + plugin.yml + config.yml + excel-build.txt)
- Base: ExcelMagicGate 0.1.0 tree (~/workspace/excelmagicgate-0.1.0/), live jar
  151,312 B / afc282803282b2cf3e48729801a9b0bc2ea7e3d7c51c025f7fbc52d76b289370
- Candidate: ExcelMagicGate-0.2.0.jar — 173,699 B,
  SHA-256 2e576bf4055905ac1b76b5550e3c5efe6f88df8fe1b2a4c7f222a30a96fcfa4f
- Build ID: EXC-MAGICGATE-0.2.0-WRV-20261108-01-7c4e19 · Channel: WRV (never distributed)
- Build: `src/ExcelMagicGate/build.sh [output.jar]` — JDK 25 javac, --release 21,
  classpath = ExcelShops build-libs Paper API + runtime-server libraries (same
  harness as the other Excel plugins); no compile-time Magic / GuildGate /
  LuckPerms dependency (all reflective, house pattern). Deterministic:
  pinned zip timestamps + sorted entries; double-build byte-identical
  (verified Nov 8, 2026, and identical to the first lane's build).
- Tests: `src/ExcelMagicGate/run-tests.sh` — 158 checks, 0 failures (Nov 8, 2026).
- Ownership: Mike's own work (ExcelMagicGate is an original WRV plugin;
  no upstream fork). Magic (ElMakers) is a runtime dependency only, driven
  through its public API reflectively; nothing of Magic's ships in this jar.
- Delta vs 0.1.0: + RankFloor.java, FloorPlan.java, MagicFloorHook.java,
  BuildInfo.java; ExcelMagicGate.java + StageApplier.java extended
  (reconcileFloor apply step, join/activation/sweep legs, /magicgate rank,
  status floor line); config.yml gains the floor-reconcile section;
  plugin.yml gains the rank command + excelmagicgate.rank permission.
  All six stage packets byte-identical to 0.1.0 (42/42 files).
