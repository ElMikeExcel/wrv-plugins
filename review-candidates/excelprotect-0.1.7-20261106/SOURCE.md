# ExcelProtect 0.1.7 — source provenance

- **Plugin:** ExcelProtect 0.1.7 "Parity" — original house code
  (Wink Reyes Valley), Mike's IP, MIT (LICENSE in the plugin lineage
  since 0.1.0). Authors: Iris / Mike.
- **Base:** ExcelProtect 0.1.6 sources (`~/workspace/protect-recovery-build/`),
  built Nov 6, 2026 and queued PENDING ahead of this build; jar of
  record 234,451 B, SHA-256
  `097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`.
  0.1.7 is cumulative: it contains 0.1.2 + 0.1.3 + 0.1.4 + 0.1.5 + 0.1.6
  whole.
- **This build:** `~/workspace/protect-elites-build/` — full source
  in `src/`, tests in `tests/`, build via `local-build.sh`
  (deterministic: sorted entries, timestamps pinned
  2026-11-06T00:00:00Z, `--no-manifest`; the Layer B fingerprint
  marker `excel-build.txt` is injected at build time with the pinned
  build ID `EXC-PROTECT-0.1.7-WRV-20261106-01-c017f1` and
  gate-verified post-build).
- **Jar of record:** `ExcelProtect-0.1.7.jar` — 246,038 B, SHA-256
  `d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124`;
  double-built byte-identical (`ExcelProtect-0.1.7-repro.jar`
  alongside, same hash).
- **New files over 0.1.6:** `RgOrigin.java` (RewardGuard origin
  taxonomy — a deliberate field-for-field port of ExcelTelemetry's
  `RewardGuardOrigin`, same PDC keys and class table, so gate G3 can
  compare mechanically), `ImportedLfState.java` (the importer's
  Lost & Found state delta book).
  **Modified:** `Importer.java` (honors DeathRecord restored marks —
  burns pre-marked via Recovery, restored death items excluded,
  xp-restored deaths import without xp_level; migrates
  lostfound/players state by delta; extended Result + system import
  line; fail-soft world pin), `Recovery.java` (`markLineRestored`,
  `applyImportedLfState`), `Ledger.java` (`emit` returns the rendered
  line), `Entities.java` (kill-parity assembly on entity_death +
  pure `killParity` half), `DamageLedger.java` (fall/explosion share
  math, damage-time elite cache on the book, book accessor),
  `CombatCore.java` (victim elite probe cached at damage time),
  `ExcelProtect.java` (importer wiring, extended import reply, 0.1.7
  boot lines, status version), `plugin.yml` (0.1.7, description),
  `local-build.sh` (jar name, fingerprint).
  **Tests:** 9 new (86 total, all green).
- **Compile/probe dependencies (see DEPENDENCIES.md):** Paper API
  26.3, WorldEdit API (softdepend hook, unchanged). EliteMobs is read
  reflectively at runtime via the existing `EliteProbe` (no compile
  dependency); RewardGuard is read via its public PDC keys only (no
  dependency at all). The rig ran the genuine EliteMobs 10.9.12 jar
  (`~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar`, the live
  version) and WorldEdit 7.4.6.
- **Design evidence:** `~/workspace/protect-rollout-item13a/`
  (CENSUS-20261106 + IMPORTER-PACKET + EVIDENCE — census counts
  R-burns=4 / LF-state=1 and the F1/F2 format findings this build
  closes) and `~/workspace/protect-rollout-item13b/`
  (TELEMETRY-CONSUMER-AUDIT + EVIDENCE F3 — the kill-line field diff;
  the mapping table in BUILD-RECORD.md is the G3 reference).
