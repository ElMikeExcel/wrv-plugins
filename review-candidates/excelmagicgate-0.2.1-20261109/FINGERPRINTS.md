# ExcelMagicGate — fingerprint sheet (0.2.1)

Per ~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md §2: marker CATEGORIES and
WHERE TO LOOK only — never raw values. Values live in the registry
(~/workspace/plugin-publishing/FINGERPRINT-REGISTRY.md) and the sealed reference copy
(the records package).

- SKU: ExcelMagicGate · Version: 0.2.1 · Channel: WRV
- Build ID: EXC-MAGICGATE-0.2.1-WRV-20261109-01-d34112
- Artifact: ExcelMagicGate-0.2.1.jar, 192,747 B,
  SHA-256 7110bfdcff48ef1bb6f68ff779276eb25a1118680ccaddbe6dc629fdc8121c5a
- Layer A (visible brand): plugin.yml name/version/author/description/website;
  enable line names plugin + version + build.
- Layer B (per-build ID): excel-build.txt at jar root; BuildInfo constant
  (fragment-assembled) in com.winkreyes.excelmagicgate; printed by /magicgate status.
- Layer C (semantic, ≥4 — categories): (1) default-value signature shape —
  the stage-floor rank table in RankFloor (carried from 0.2.0); (2) default config
  phrasing — config.yml floor-reconcile section comments (carried); (3) data-file
  schema keys — state.yml key dialect (carried); (4) message-string voice —
  the floor-step player message in MagicFloorHook (carried); (5) permission-node
  dialect — excelmagicgate.* nodes (carried).
- Layer D: WRV channel segment in the build ID; sealed copy = records
  package excelmagicgate-0.2.1-20261109.
- Lineage: ALL of 0.2.0's semantic markers carried forward unchanged; only the
  build ID rotated. No marker vanished in this update — the refresh touched
  stage-packet DATA (spells/grants templates), not marker-bearing code or text.
- Persistence state: hand-markers + build-script stamping (excel-build.txt
  is written by the source tree and copied by build.sh); single build
  entry point = ExcelMagicGate/build.sh in the packet tree.
