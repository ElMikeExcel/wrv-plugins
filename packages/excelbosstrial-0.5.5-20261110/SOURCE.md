# SOURCE — ExcelBossTrial 0.5.5 (Warden first-break layer)

- **Artifact:** `ExcelBossTrial-0.5.5.jar` — 455,307 B / sha256
  `eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb`
- **Build ID:** `EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`
- **Source tree:** `bosstrial/` in this folder (the full
  portable source-record tree: src, tests, config.yml,
  plugin.yml, package.sh, build-test.sh).
- **Base:** the shipped ExcelBossTrial 0.5.4 tree
  (`~/workspace/elder-sprinkle-melee-fix-20261110/build-054/bosstrial/`),
  first proven to rebuild byte-identical to the shipped
  0.5.4 jar (443,874 B / sha256
  `aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2`)
  before any edit.
- **Change vs base:** the Warden first-break layer (Mike,
  Nov 10, 2026, APPROVED; design
  `~/workspace/warden-firstbreak-20261110/DESIGN-AND-BUILDSCOPE.md`) —
  new `core/FirstBreakRewards.java` (eligibility matrix,
  slate spec, EM-kit table, title/trophy text);
  `BossTrialConfig` gains the `first-break.warden.*` spec
  (settled defaults = the commissioned table, cash $10,000
  TOTAL per decision-log D6); the host gains the persisted
  once-flag + plaque record in state.yml (flushed BEFORE any
  first-break reward moves), `applyFirstBreakLayer` /
  `storeFirstBreakClaims` on the repeat layer's rails, the
  first-break jackpot branch in `applySealLoot`, the
  PotionMeta kit builder, and the boot line; bundled config
  ships the `first-break:` block + headline message; version
  surfaces (plugin.yml 0.5.5, BuildInfo rotation,
  excel-build.txt) + tests (new FirstBreakRewardsTest 56,
  FirstBreakConfigTest 21; PluginYmlParseTest +9). Repeat
  breaks and all other chapters unchanged. Full list and
  verification: `BUILD-RECORD.md` in this folder.
- **Install packet:**
  `~/workspace/bosstrial-055-build/INSTALL-PACKET.md`
- **Ownership:** Wink Reyes Valley Excel plugin (Mike Reyes /
  Iris); WRV channel build, never distributed.
