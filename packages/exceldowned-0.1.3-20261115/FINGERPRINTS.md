# FINGERPRINTS — ExcelDowned 0.1.3

- Jar (NOT in this git package; Drive only): ExcelDowned-0.1.3.jar —
  59,875 B / SHA-256 31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93 /
  MD5 3fa49b0396cb74b08020231007e82522.
- Build ID: EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d.
- Fingerprint gate: PASS at build (see BUILD-RECORD.md). The build ID is
  carried in BuildInfo.java fingerprint fragments and printed in the
  ready line at boot; the installed boot log carried it exactly once.
- Determinism: deterministic double-build byte-identical (candidate jar
  and repro jar both hash 31f278bd…9e93; two independent local-build.sh
  runs produced the same hash).
- Base gate: unmodified 0.1.2 tree rebuilt byte-identical to the shipped
  0.1.2 jar — 56,778 B / SHA-256
  2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396,
  suite 27/27, before any change was applied.
- Sources zip (Drive only): ExcelDowned-0.1.3-sources.zip — 49,118 B /
  SHA-256 3c929d80f07cc60f3bae634c428dcc37cd70f10872f6c1182eaced99c230400e /
  MD5 24b22c1bedbd7b4fd399c357caf8139f (24 files: src + tests +
  config.yml + plugin.yml + local-build.sh + DESIGN-0.1.2.md +
  DESIGN-0.1.3.md; build-of-record name ExcelDowned-0.1.3-source.zip,
  identical bytes).
- Config: config.yml 1,727 B / SHA-256
  8f3671e7c27ba4cac1afecedc4cb2bd2b7438226d4df5e159a21c4881e627156 —
  placed at install (gate-range 24 -> 20, damage-eats-window: false,
  revive-range 20, bleed-out 300 unchanged); pull-back EXACT.
- TogetherPartyProvider.java (0.1.3, pre-fix): 6,975 B / SHA-256
  bc31a59f3508c772c8de21aa1df8c5b9f4d2baeb608d6826ca5d672206580b3b.
  The 0.1.4 provider fix supersedes this file; it is recorded here as
  the 0.1.3 lineage of record.
