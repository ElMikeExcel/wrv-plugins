# ExcelEliteLadder 0.2.1 — source notes

- **Base:** the 0.2.0 build tree (`~/workspace/eliteladder-config-build/`),
  copied verbatim into this folder before any edit (copy verified
  byte-identical: src/ and tests/ diff-clean, rig jars hash-equal). The
  0.2.0 jar of record is 40,475 B / SHA-256
  `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`
  (installed live Nov 5, 2026).
- **Functional delta (and nothing else):**
  - `EliteSpeedListener.java` — NEW: EliteMobSpawnEvent (1-tick delayed
    application) + EliteMobEnterCombatEvent (re-assert) for natural
    elites only; registered only when EliteMobs is enabled.
  - `core/EliteSpeedSettings.java` — NEW (pure Java): parse/clamp of the
    config.yml `elite-speed:` section, the shouldBuff gate, labels, and
    the hidden-effect spec constants.
  - `ExcelEliteLadder.java` — elite-speed settings load (enable +
    `/eliteladder reload`), listener registration, the enable-line and
    status lines for the feature; refusal/baseline messages re-pointed
    from "the exact 0.1.2 values" / old S1-norm sha to the retuned
    wording / new sha (`ff017c5d…`).
  - `core/LadderTable.java` — `builtin()` now carries the Nov 5, 2026
    RETUNED table (the fallback moves with the shipped default, so it
    can never resurrect pre-retune tuning); class doc updated.
  - `ladder.yml` — bundled default = the retune packet's candidate
    values, header adapted for the bundled resource.
  - `config.yml` — header updated to the retuned table; NEW `elite-speed:`
    section appended; every pre-existing key/value untouched.
  - `plugin.yml` — version 0.2.1; description notes the speed buff and
    the retuned default.
  - `tests/` — fixtures S1–S5 regenerated to the retuned -norm files;
    historical blocks re-pointed to the pre-retune 0.2.0 values; NEW
    `tests/EliteSpeedSuite.java` (39 checks); TestMain runs it.
  - `build.sh` — output name 0.2.1; EliteMobs 10.9.12 jar added as a
    COMPILE-ONLY classpath entry (the tree's own rig copy); nothing of
    EM is packaged into the product jar (verified by entry list).
- **Toolchain (this VM):** JDK 25 (Temurin 25.0.4.1, `~/workspace/.jdk25`),
  Paper API 26.3 build 231 (`~/workspace/magic-los-fix/cp/`), runtime
  libraries from `~/workspace/excelbuildmode-pilot/runtime-server/libraries`
  (SnakeYAML 2.6 also drives the equality suite) — the 0.2.0 toolchain,
  unchanged, plus the EM jar above.
- **Ownership:** all first-party WinkReyesValley code (as 0.1.x/0.2.0).
  Compile-only deps: Paper API + its library set + EliteMobs 10.9.12
  (API classes only); runtime deps: EliteMobs 10.9.12 + ExcelGuildGate
  0.4.0 (softdepend; GuildGate read stays reflective — unchanged).
