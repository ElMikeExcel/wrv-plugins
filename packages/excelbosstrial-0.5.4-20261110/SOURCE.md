# SOURCE — ExcelBossTrial 0.5.4 (Elder sprinkle melee fix)

- **Artifact:** `ExcelBossTrial-0.5.4.jar` — 443,874 B / sha256
  `aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2`
- **Build ID:** `EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5`
- **Source tree:** `bosstrial/` in this folder (the full
  portable source-record tree: src, tests, config.yml,
  plugin.yml, package.sh, build-test.sh).
- **Base:** the shipped ExcelBossTrial 0.5.3 tree
  (`~/workspace/warden-retune-20261109/build-053/bosstrial/`),
  first proven to rebuild byte-identical to the shipped
  0.5.3 jar (440,242 B / sha256
  `09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8`)
  before any edit.
- **Change vs base:** the elder sprinkle melee fix (Mike,
  Nov 9, 2026) as two per-chapter sprinkle keys — new
  `core/SprinkleMelee.java` (knockback set/clamp/applied
  table + stagger delay table, pure logic); `SprinkleSettings`
  gains `knockbackResistance` (NaN = unset) + `staggerTicks`,
  sanitized loud, with the ELDER_GUARDIAN defaults alone
  carrying the fix (-1.0 / 40); `SprinkleService` forces the
  knockback value onto each converted add (applied at the
  0.0 floor), spreads staggered ring spawns across the
  window via the scheduler (pending spawns count against
  the alive cap; a spawn whose encounter closed never
  lands), and names both knobs on the boot line; the host
  reads the two keys per chapter (absent knockback key =
  chapter default); bundled config ships the elder keys +
  comments; version surfaces (plugin.yml 0.5.4, BuildInfo
  rotation, excel-build.txt) + tests (new SprinkleMeleeTest
  23; SprinkleSettingsTest +9; PluginYmlParseTest +4).
  NO damage number changes anywhere. Full list and
  verification: `BUILD-RECORD.md` in this folder.
- **Install packet:**
  `~/workspace/elder-sprinkle-melee-fix-20261110/PACKET.md`
- **Ownership:** Wink Reyes Valley Excel plugin (Mike Reyes /
  Iris); WRV channel build, never distributed.
