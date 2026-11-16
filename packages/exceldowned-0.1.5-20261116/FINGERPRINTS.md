# FINGERPRINTS — ExcelDowned 0.1.5

- Jar (NOT in this git package; Drive only): ExcelDowned-0.1.5.jar —
  63,701 B / SHA-256 50d7514934c7f71eaa4840a3024d3cb0594436a6a0409c6c7b2cb21e048b9710 /
  MD5 df6794b3c4e33d6168d259774e90d581.
- Build ID: EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d.
- Fingerprint gate: PASS at build (see BUILD-RECORD.md). The build ID is
  carried in BuildInfo.java fingerprint fragments and printed in the
  ready line at boot; the installed boot log carried it exactly once.
- Determinism: deterministic double-build byte-identical (candidate jar
  and repro jar both hash 50d75149…9710).
- Base gate: unmodified 0.1.4 tree rebuilt byte-identical to the
  installed 0.1.4 jar — 59,998 B / SHA-256
  703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339,
  suite 39/39, fingerprint PASS (build
  EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d).
- Fix fingerprints: ExcelDowned.java 26,602 B / SHA-256
  c48721a8aa09d5a7016c3ceb00621b55536c81810d089252661c9be21a89ce10
  (deferred + bounded-retry self-check); new pure policy class
  TemplateCheckPolicy.java 4,615 B / SHA-256
  08d6b30fd60a75dfe2eb286f9c6372d0e5b870fb120672ff4c91e6cc055c664a.
  Full diff vs the 0.1.4 tree carried in this package as
  ExcelDowned-0.1.5.diff (23,512 B / SHA-256
  d1f842a0b54ff3fbcf44720b4a592c7b61107840d88b26efff43f17ea689d661).
- Config: config.yml 1,727 B / SHA-256
  8f3671e7c27ba4cac1afecedc4cb2bd2b7438226d4df5e159a21c4881e627156 —
  byte-identical to 0.1.4 and to the live config at install.
