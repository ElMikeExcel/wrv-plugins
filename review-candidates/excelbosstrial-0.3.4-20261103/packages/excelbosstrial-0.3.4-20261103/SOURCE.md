# ExcelBossTrial — source of record (in-tree)

This tree is the complete, buildable source of **ExcelBossTrial 0.3.4**, a
delta release on 0.3.3 that adds the ExcelProtect emitter adapter (verdict
fight tables + seal verdicts + boss state changes published to ExcelProtect
0.1.0). Built and proven 2026-11-02; see `../TEST-REPORT.md` and
`../INSTALL-PACKET.md` in the build root.

Ownership: written from scratch for Wink Reyes Valley (the ExcelDragonTrial
0.1.2 port included, as in 0.3.3). It depends on **Paper API 26.3**,
**ExcelGuildGate** (seal credit seam), **Magic** (heal-claim feed,
softdepend), **EliteMobs** (sprinkle seam, softdepend), and — new in
0.3.4, compile-time API only, softdepend at runtime — **ExcelProtect
0.1.0** (its publish API jar; the plugin is never bundled).

## Base proof (delta-build law)

Before any 0.3.4 edit, this tree (an unmodified copy of the 0.3.3 tree)
was rebuilt with the 0.3.3 classpath and produced a jar **byte-identical**
to the shipped live 0.3.3 artifact:

- Artifact: `ExcelBossTrial-0.3.3.jar`, 339,544 B
- SHA-256: `6bdeefb3b92ea09f1d67615950f6bd4ba2a4337bda9bac2b43501d98a52d0cbe`
- Record: `../base-verify/BASE-PROOF.md`

## The 0.3.4 delta (emit-only)

- `src/com/winkreyes/excelbosstrial/core/ProtectEmitSettings.java` (new)
  — the three config switches as a value record; master-off collapses the
  sub-switches.
- `src/com/winkreyes/excelbosstrial/core/ProtectEmitSink.java` (new) —
  the in-house publish interface + the no-op standing sink. No Protect
  types appear here or anywhere outside the gateway.
- `src/com/winkreyes/excelbosstrial/core/ProtectTableShape.java` (new) —
  pure shaping: fight-record rows to the FightTableEvent contract keys,
  and the `boss_state` / `fight_progress` record builders (schema v1,
  documented in the class javadoc).
- `src/com/winkreyes/excelbosstrial/protect/ProtectEmitGateway.java`
  (new) — the ONLY class importing
  `com.winkreyes.excelprotect.api.*`; fires `FightTableEvent` and calls
  `ExcelProtectAPI.publish`; every call guarded, failures counted and
  logged at most 3× per boot.
- `src/com/winkreyes/excelbosstrial/ExcelBossTrial.java` (edited) —
  binds the gateway at enable when `protect-emit.enabled` and the
  ExcelProtect plugin is present; publishes at five sites (verdict
  snapshot, encounter open, both elder deny branches, elder trio flip,
  elder respawn completion). All sites sit AFTER the corresponding
  0.3.3 behavior (the fight record write, the state change itself) and
  cannot alter it.
- `plugin.yml` — version 0.3.4; `ExcelProtect` added to `softdepend`.
- `config.yml` — new `protect-emit` section (three keys, all default
  true) ahead of `fight-tracking`.
- `FUNCTIONAL-CHANGE.diff` — the full source diff vs the 0.3.3 tree.
- `DEPENDENCIES.sha256` — gains the ExcelProtect 0.1.0 jar line
  (`648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594`,
  compile-only).

## Build

Same deterministic recipe as 0.3.3 (`package.sh`, fixed zip timestamps
2026-10-28T00:00:00Z): Temurin JDK 25, classpath = the 0.3.3
`classpath.txt` verbatim **plus** the ExcelProtect 0.1.0 jar (recorded in
`../classpath.txt`). Build output:

- `../jars/ExcelBossTrial-0.3.4.jar`, 348,104 B
- SHA-256: `14d5d06c2c3cd1d151cdb82dd8d6e069a19f10c0fd95ec82dca141ead8b38ad0`
- Built twice, byte-identical (`../jars/repro-0.3.4.jar`).
- Test suite: 25,414 checks, 0 failures (see `../TEST-REPORT.md`).
