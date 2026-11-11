# FINGERPRINTS — ExcelBossTrial 0.5.5

- Candidate jar: `ExcelBossTrial-0.5.5.jar` — 455,307 B /
  sha256 `eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb`
- Build ID: `EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`
- Layer B homes, all verified in the built jar:
  1. `excel-build.txt` at the jar root carries the build ID.
  2. `core/BuildInfo.class` fragment-assembled constant:
     887e6f present, 5371d5 (0.5.4) absent (string scan).
  3. Boot surface line `Build: EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`
     observed in the desk rig boot (`work/rig-boot.log`).
- Deterministic double-build: `work/pkg1.jar` = `work/pkg2.jar`
  = the candidate, byte-identical.
- Base: pristine rebuild of the untouched 0.5.4 tree =
  the shipped 0.5.4 jar (443,874 B /
  `aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2`).
- Live gates this build installs against (see INSTALL-PACKET.md):
  live jar `plugins/ExcelBossTrial-0.4.1.jar` = the 0.5.4 jar
  above; live config 23,232 B (post-0.5.4 merge,
  `config.yml.bak-054` in place) — merge-only per
  `live-config-merge/MERGE.md`, predicted post-merge 25,297 B.
