# ExcelDetails — fingerprint sheet (categories + locations only)

- Layer A: plugin.yml name/description/authors; READY line; jar name.
- Layer B: build ID in excel-build.txt (jar root) + BuildInfo
  constant fragments (com.winkreyes.exceldetails.BuildInfo); surfaced
  by /details status.
- Layer C: (1) default pair refresh-ticks 10 x combat-timeout-seconds 8
  (config.yml defaults); (2) config comment phrasing + section order;
  (3) team dialect edt<i> + entry prefix dialect (SidebarRenderer);
  (4) message voice in DetailsCommands; (5) shed order (0.2.0: footer -> rows from the fullest section -> whole sections from the end of canonical order; 0.1.0 form:
  footer -> lowest rows -> header/own-row last (LineBuilder).
- Layer D: channel WRV (never distributed).
- Single build entry point: ExcelDetails/local-build.sh (fingerprint
  gate inside). Persistence state: build-time injected (excel-build.txt)
  + hand markers in source.
