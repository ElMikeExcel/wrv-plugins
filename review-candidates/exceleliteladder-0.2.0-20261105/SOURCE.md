# ExcelEliteLadder 0.2.0 — source notes

- **Base:** the live 0.1.2 build tree (`~/workspace/excel-eliteladder-0.1.2/build-0.1.2/`),
  copied verbatim into this folder before any edit; the 0.1.2 jar of record is
  31,403 B / SHA-256 `7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb`
  (installed live Nov 1, 2026).
- **Functional delta (and nothing else):**
  - `core/LadderTable.java` — statics → instance (`builtin()` = the exact
    0.1.x values; step/matchStep now instance methods with identical
    semantics; matches/differences/describeOffLadder/clampSeals/OLD_S0/
    OLD_S1 untouched statics).
  - `core/LadderConfig.java` + `core/LadderConfigException.java` — NEW:
    strict ladder.yml validation (see CHANGELOG-0.2.0.md).
  - `core/StepPlan.java`, `core/FileApplier.java` — the active table is a
    parameter now; logic untouched.
  - `ExcelEliteLadder.java` — ladder.yml generate/load/reload, table state
    on the enable line + status, `/eliteladder reload`.
  - `ladder.yml` — NEW bundled default (= the 0.1.2 table).
  - `config.yml` — header comment only; keys/values identical.
  - `plugin.yml` — version 0.2.0, usage/description mention reload.
- **Toolchain (this VM):** JDK 25 (Temurin 25.0.4.1, `~/workspace/.jdk25`),
  Paper API 26.3 build 231 (`~/workspace/magic-los-fix/cp/`), runtime
  libraries from `~/workspace/excelbuildmode-pilot/runtime-server/libraries`
  (SnakeYAML 2.6 also drives the equality suite). The 0.1.2 build scripts
  referenced another machine's paths and were rewritten for this VM
  (`build.sh`, `run-tests.sh`); an unmodified-0.1.2 rebuild under this
  toolchain differs from the jar of record only in class bytes — see
  TEST-REPORT.md "Base proof".
- **Ownership:** all first-party WinkReyesValley code (as 0.1.x). Compile-only
  deps: Paper API + its library set; runtime deps: EliteMobs 10.9.12 +
  ExcelGuildGate 0.4.0 (softdepend, reflective API read — unchanged).
