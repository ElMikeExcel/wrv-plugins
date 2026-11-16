# FINGERPRINTS — ExcelDowned 0.1.4

- Jar (NOT in this git package; Drive only): ExcelDowned-0.1.4.jar —
  59,998 B / SHA-256 703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339 /
  MD5 128cf1b78101d56baecb6b3a1dcc7104.
- Build ID: EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d.
- Fingerprint gate: PASS at build (see BUILD-RECORD.md). The build ID is
  carried in BuildInfo.java fingerprint fragments and printed in the
  ready line at boot; the installed boot log carried it exactly once.
- Determinism: deterministic double-build byte-identical (candidate jar
  and repro jar both hash 703d7f9a…2339).
- Base gate: unmodified 0.1.3 tree rebuilt byte-identical to the live
  0.1.3 jar — 59,875 B / SHA-256
  31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93,
  suite 34/34, fingerprint PASS (build
  EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d).
- Fix fingerprint: TogetherPartyProvider.java 8,218 B / SHA-256
  c1403123f653b5007132e5a9501a0c54d3f632500faadfe59fcb2deb5818561f
  (0.1.3 provider was bc31a59f3508c772c8de21aa1df8c5b9f4d2baeb608d6826ca5d672206580b3b).
  Patch carried in this package as TogetherPartyProvider.patch.
- Config: config.yml 1,727 B / SHA-256
  8f3671e7c27ba4cac1afecedc4cb2bd2b7438226d4df5e159a21c4881e627156 —
  byte-identical to 0.1.3 and to the live config at install.
