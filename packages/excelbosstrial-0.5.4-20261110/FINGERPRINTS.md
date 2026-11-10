# FINGERPRINTS — ExcelBossTrial 0.5.4

- Candidate jar: `ExcelBossTrial-0.5.4.jar` — 443,874 B /
  sha256 `aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2`
- Build ID: `EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5`
- Layer B homes, all verified in the built jar:
  1. `excel-build.txt` at the jar root carries the build ID.
  2. `core/BuildInfo.class` fragment-assembled constant:
     5371d5 present, 35bd2b (0.5.3) absent.
  3. Boot surface line `Build: EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5`
     observed in the desk rig boot (`build-054/work/rig-boot.log`).
- Deterministic double-build: `work/pkg1.jar` = `work/pkg2.jar`
  = the candidate, byte-identical.
- Base: pristine rebuild of the untouched 0.5.3 tree =
  the shipped 0.5.3 jar (440,242 B /
  `09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8`).
- Live gates this build installs against (see PACKET.md):
  live jar `plugins/ExcelBossTrial-0.4.1.jar` = the 0.5.3
  jar above; live config 23,179 B (post-0.5.3 merge,
  `config.yml.bak-053` in place; no config sha256 on record
  for the merged file — the packet gates on size + the
  elder key spot-checks, never on an invented hash).
