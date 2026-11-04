# ExcelBossTrial — source of record (in-tree)

This tree is the complete, buildable source of **ExcelBossTrial 0.3.5**, a
delta release on 0.3.4 that retires guardian spike reflect for tracked
seal elders (Mike's playstyle-parity ruling, Nov 2, 2026). Built and
proven 2026-11-02; see `../TEST-REPORT.md` and `../INSTALL-PACKET.md` in
the build root.

Ownership: written from scratch for Wink Reyes Valley (the ExcelDragonTrial
0.1.2 port included, as in 0.3.3). It depends on **Paper API 26.3**,
**ExcelGuildGate** (seal credit seam), **Magic** (heal-claim feed,
softdepend), **EliteMobs** (sprinkle seam, softdepend), and
**ExcelProtect 0.1.0** (compile-time API only, softdepend at runtime).
0.3.5 adds no dependency.

## Base proof (delta-build law)

Before any 0.3.5 edit, this tree (an unmodified copy of the 0.3.4 tree)
was rebuilt with the 0.3.4 classpath and produced a jar **byte-identical**
to the shipped 0.3.4 candidate:

- Artifact: `ExcelBossTrial-0.3.4.jar`, 348,104 B
- SHA-256: `14d5d06c2c3cd1d151cdb82dd8d6e069a19f10c0fd95ec82dca141ead8b38ad0`
- Record: `../base-verify/BASE-PROOF.md`

## The 0.3.5 delta (reflect suppression, elder chapter only)

Design of record: `~/workspace/seal-audit/REFLECT-FIX-FEASIBILITY-20261102.md`
§§1–2 (the file on disk ends mid-§3; the interception design, config-key
naming, and core-class shape in §§1–2 are complete and were followed
exactly; the shipped value follows Mike's later ruling, below).

- `src/com/winkreyes/excelbosstrial/core/ReflectRule.java` (new) — the
  pure rule: the identification predicate (`isSpikeReflect`: THORNS
  cause + tracked-boss damager + elder kind), the multiplier clamp
  (negative/NaN → 0; > 1 allowed), `scaledDamage` (finite, ≥ 0
  always), and `cancels` (a scaled 0 cancels the event outright).
- `src/com/winkreyes/excelbosstrial/core/BossTrialConfig.java` (edited)
  — a `Reflect` record with `settled()` = 0.0, the `elderReflect`
  field, a `reflect(BossKind)` accessor (neutral 1.0 for non-elder
  kinds), a new canonical constructor (the 0.3.0 shape delegates at
  the settled default), and `reflectFrom(...)` parsing
  `elder-guardian.reflect.multiplier` beside `pressureFrom(...)`.
- `src/com/winkreyes/excelbosstrial/ExcelBossTrial.java` (edited) —
  the flat-key whitelist gains `elder-guardian.reflect.multiplier`;
  `onBossPressureDamage` (HIGH) judges spike reflect FIRST through
  ReflectRule and applies the chapter multiplier INSTEAD of the
  pressure scale (reflect is never ramped; at 0 the event is
  cancelled, so the MONITOR recorder skips it and the fight table's
  damage-taken stays clean). The branch runs ahead of the
  pressure-enabled gate by design: retiring reflect must not depend
  on the attrition layer being on.
- `config.yml` — new `elder-guardian.reflect` block, `multiplier: 0.0`.
- `plugin.yml` — version 0.3.5; description gains the 0.3.5 sentence.
- `tests/.../core/ReflectRuleTest.java` (new, 31 checks) and
  `tests/.../core/ReflectConfigTest.java` (new, 9 checks).
- `FUNCTIONAL-CHANGE.diff` — the full source diff vs the 0.3.4 tree
  (762 lines, 9 file entries incl. the changelog swap).

**Settled-value note:** the feasibility doc recommended a 0.25 code
default. Mike's ruling later the same night settled the value at
**0.0** (full cancel), so 0.0 is both the shipped config value and
the code settled default — the adjustability law's "settled value as
default" applied to the value Mike actually settled.

## Build

Same deterministic recipe as 0.3.3/0.3.4 (`package.sh`, fixed zip
timestamps 2026-10-28T00:00:00Z): Temurin JDK 25, classpath = the
0.3.4 `classpath.txt` verbatim (no dependency change). Build output:

- `../jars/ExcelBossTrial-0.3.5.jar`, 350,952 B
- SHA-256: `3364c152202eb864c16240dc48efc68241a5de7ab4ef25419247867443bf5dc0`
- Built twice, byte-identical (`../jars/repro-0.3.5.jar`).
- Test suite: 26,949 checks, 0 failures (see `../TEST-REPORT.md`).
