# ExcelDetails — fingerprint sheet (categories + locations only)

- Layer A: plugin.yml name/description/authors; READY line; jar name.
- Layer B: build ID in excel-build.txt (jar root) + BuildInfo
  constant fragments (com.winkreyes.exceldetails.BuildInfo); surfaced
  by /details status. 0.2.1 build ID:
  EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a. Semantic marker
  EXC-DETAILS persists from 0.1.0/0.2.0 per the standard
  (BUILD-RECORD.md, fingerprint gate PASS).
- Layer C: carried from 0.2.0 unchanged where the 0.2.1 tree diff
  did not touch the file — (1) default pair refresh-ticks 10 x
  combat-timeout-seconds 8 (config.yml byte-identical to 0.2.0's,
  sha256 29cb1903…); (2) LineBuilder shed order (file untouched
  in 0.2.1). SidebarRenderer WAS touched in 0.2.1 (party-scope
  filtering); no Layer C claim is made for it here.
- Layer D: channel WRV (never distributed).
- Single build entry point: ExcelDetails/local-build.sh (fingerprint
  gate inside). Persistence state: build-time injected (excel-build.txt)
  + hand markers in source.
