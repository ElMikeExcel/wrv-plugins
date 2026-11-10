# ExcelDetails — fingerprint sheet (categories + locations only)

- Layer A: plugin.yml name/description/authors; READY line; jar name.
- Layer B: build ID in excel-build.txt (jar root) + BuildInfo
  constant fragments (com.winkreyes.exceldetails.BuildInfo); surfaced
  by /details status. 0.2.2 build ID:
  EXC-DETAILS-0.2.2-WRV-20261110-01-b22e10. Semantic marker
  EXC-DETAILS persists from 0.1.0/0.2.0/0.2.1 per the standard
  (BUILD-RECORD.md, fingerprint gate PASS).
- Layer C: carried from 0.2.1 unchanged where the 0.2.2 tree diff
  did not touch the file — (1) default pair refresh-ticks 10 x
  combat-timeout-seconds 8 (config.yml keys unchanged; the file
  gains two new keys + the stamp comment in 0.2.2); (2) LineBuilder
  shed order + PartyScope/PartyLookup (files untouched in 0.2.2).
- Layer D: channel WRV (never distributed).
- Single build entry point: ExcelDetails/local-build.sh (fingerprint
  gate inside). Persistence state: build-time injected (excel-build.txt)
  + hand markers in source.
