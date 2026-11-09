# ExcelMagicGate — fingerprint sheet (0.2.0)

Per ~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md §2: this sheet
records marker CATEGORIES and WHERE TO LOOK only — never raw values.
Values live in the registry (~/workspace/plugin-publishing/FINGERPRINT-REGISTRY.md)
and the sealed reference copy (the records package).

- SKU: ExcelMagicGate · Version: 0.2.0 · Channel: WRV
- Build ID: EXC-MAGICGATE-0.2.0-WRV-20261108-01-7c4e19
- Artifact: ExcelMagicGate-0.2.0.jar, 173,699 B,
  SHA-256 2e576bf4055905ac1b76b5550e3c5efe6f88df8fe1b2a4c7f222a30a96fcfa4f
- Layer A (visible brand): plugin.yml name/version/author/description/website;
  enable line names plugin + version + build.
- Layer B (per-build ID): excel-build.txt at jar root; BuildInfo constant
  (fragment-assembled) in com.winkreyes.excelmagicgate; printed by
  /magicgate status.
- Layer C (semantic, ≥4 — categories): (1) default-value signature shape —
  the stage-floor rank table in RankFloor; (2) default config phrasing —
  config.yml floor-reconcile section comments; (3) data-file schema keys —
  state.yml key dialect (carried from 0.1.0); (4) message-string voice —
  the floor-step player message in MagicFloorHook; (5) permission-node
  dialect — excelmagicgate.* nodes.
- Layer D: WRV channel segment in the build ID; sealed copy = records
  package excelmagicgate-0.2.0-20261108.
- Lineage: 0.1.0's markers (state schema, message voice, permission
  dialect) carried forward unchanged; only the build ID rotated, plus
  the new floor-table marker added by this version's feature. No marker
  vanished in this update.
- Persistence state: hand-markers + build-script stamping (excel-build.txt
  is written by the source tree and copied by build.sh); single build
  entry point = src/ExcelMagicGate/build.sh.
