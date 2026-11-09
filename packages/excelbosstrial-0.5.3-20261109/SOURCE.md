# SOURCE — ExcelBossTrial 0.5.3 (Item 39 Warden retune filing)

- **Artifact:** `ExcelBossTrial-0.5.3.jar` — 440,242 B / sha256
  `09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8`
- **Build ID:** `EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b`
- **Source tree:** `bosstrial/` in this folder (the full
  portable source-record tree: src, tests, config.yml,
  plugin.yml, package.sh, build-test.sh).
- **Base:** the shipped ExcelBossTrial 0.5.2 tree
  (`~/workspace/bosstrial-052-build/bosstrial/`), first proven
  to rebuild byte-identical to the shipped 0.5.2 jar
  (438,791 B / 1a866a54…) before any edit.
- **Change vs base:** R2 (warden pressure defaults
  outgoing-multiplier 1.0 / ramp-max 1.5 in the bundled
  config) + R1 (new `core/SonicBoomRule.java`; `Pressure`
  gains `applyToSonicBoom`, code fallback false; handler
  exemption in `onBossPressureDamage`; four flat keys) +
  version surfaces (plugin.yml 0.5.3, BuildInfo rotation,
  excel-build.txt) + tests (new SonicBoomRuleTest 24;
  PluginYmlParseTest re-pinned 37 → 44). Full list and
  verification: `BUILD-RECORD.md` in this folder.
- **Install packet:** `~/workspace/warden-retune-20261109/PACKET-053.md`
- **Ownership:** Wink Reyes Valley Excel plugin (Mike Reyes /
  Iris); WRV channel build, never distributed.
