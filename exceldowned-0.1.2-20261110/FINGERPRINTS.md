# FINGERPRINTS — ExcelDowned 0.1.2

ExcelDowned carries the house fingerprint standard
(`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`) Layer B
in-jar marker, as in 0.1.0/0.1.1:

- Build ID: **EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d** — the
  EXC-DOWNED semantic marker and the `d0wn3d` tag persist version
  to version (persistence law); the version/date/sequence
  fragments rotate per distributed build (BuildInfo.java
  fragments + `excel-build.txt` in the jar, both verified by the
  build's fingerprint gate on every build).
- Startup proof: the ready line in the server log reads
  "ExcelDowned 0.1.2 ready — build
  EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d; sessions restored: N;
  bleed-out 300s; downed factor 0.25" (rig-verified in
  `rig-0.1.2/boot.log`).
- Artifact identity: `ExcelDowned-0.1.2.jar` — 56,778 B —
  SHA-256 2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396,
  reproduced by the in-tree double-build, a clean-room rebuild,
  and a rebuild from the shipped source zip (SOURCE.md /
  BUILD-RECORD.md).
- Semantic markers added by 0.1.2, greppable as operator acts in
  the records stream and the server log: record types
  `admin_revive` / `admin_clear` (with the actor field), and the
  INFO lines "Downed admin revive by <CONSOLE|operator> for …"
  and "Downed admin clear by <CONSOLE|operator> for …" —
  distinct from the spell `revive` record and from every death
  record type.
