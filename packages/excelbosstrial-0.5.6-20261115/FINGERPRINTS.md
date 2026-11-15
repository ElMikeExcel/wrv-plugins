# FINGERPRINTS — ExcelBossTrial 0.5.6

- Candidate jar: `ExcelBossTrial-0.5.6.jar` — 469,839 B /
  sha256 `5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c`
- Build ID: `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
- Layer B homes, all verified in the built jar:
  1. `excel-build.txt` at the jar root carries the build ID.
  2. `core/BuildInfo.class` fragment-assembled constant:
     3c9e56 present, 887e6f (0.5.5) absent (string scan).
  3. Boot surface line `Build: EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
     observed in the desk rig boot (`work/rig-boot.log`).
- Deterministic double-build: `work/pkg1.jar` = `work/pkg2.jar`
  = the candidate, byte-identical.
- Base: pristine rebuild of the untouched 0.5.5 tree =
  the shipped 0.5.5 jar (455,307 B /
  `eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb`).
