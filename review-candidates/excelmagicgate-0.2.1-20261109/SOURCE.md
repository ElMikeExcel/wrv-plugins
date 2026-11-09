# ExcelMagicGate 0.2.1 — SOURCE

- Repo: ElMikeExcel/wrv-plugins (record PR is a supervisor publication leg — draft, never merged)
- Source of record: the template-refresh packet folder, ~/workspace/magicgate-template-refresh-20261109/ExcelMagicGate/
  (src/ + tests/ + stages/ + build.sh + run-tests.sh + plugin.yml + config.yml + excel-build.txt)
- Base: ExcelMagicGate 0.2.0 tree (~/workspace/magicgate-floor-build/), live jar before this install
  173,699 B / 2e576bf4055905ac1b76b5550e3c5efe6f88df8fe1b2a4c7f222a30a96fcfa4f
- Candidate: ExcelMagicGate-0.2.1.jar — 192,747 B,
  SHA-256 7110bfdcff48ef1bb6f68ff779276eb25a1118680ccaddbe6dc629fdc8121c5a
- Build ID: EXC-MAGICGATE-0.2.1-WRV-20261109-01-d34112 · Channel: WRV (never distributed)
- Build: `ExcelMagicGate/build.sh` in the packet folder — JDK 25 javac, --release 21,
  same harness as 0.2.0; deterministic: pinned zip timestamps + sorted entries;
  double-build byte-identical (verified Nov 9, 2026, packet lane).
- Tests: `ExcelMagicGate/run-tests.sh` — 158 checks, 0 failures (Nov 9, 2026; TEST-OUTPUT.txt).
- Ownership: Mike's own work (ExcelMagicGate is an original WRV plugin; no upstream fork).
  Magic (ElMakers) is a runtime dependency only, driven reflectively; nothing of Magic's ships in this jar.
- Delta vs 0.2.0 (backlog Item 41, spec = ~/workspace/magicgate-template-check-20261109/VERDICT.md §5.2):
  NO Java code changes except the version/build-ID stamp (0.2.0 → 0.2.1) and the two test
  assertions that encode the refreshed packet values. The jar-embedded stage templates are
  refreshed to the Nov 8, 2026 composed-install finals: stage{2,3,4,5} spells.yml replaced
  byte-for-byte (stage2 8bb39f04…, stage3 70c568b2…, stage4 32f1f91a…, stage5 d34112ba…)
  and stage{2,3,4,5} grants.txt refreshed cumulatively (33→40, 59→68, 67→82, 100→115).
  paths.yml / wands.yml: NO EDIT at any stage (verified, not assumed — PACKET.md §2).
  stage0 / stage1 packets unchanged.
- SUPERSESSION (PACKET.md §5, carried in writing per the packet's own instruction): the 0.2.0
  BUILD-RECORD claim "Stage packets byte-identical to 0.1.0 — PASS (42/42)" is now false BY
  DESIGN for 0.2.1: 8 of the 42 packet files differ (spells.yml stage2–5 + grants.txt stage2–5);
  the other 34 remain byte-identical to 0.1.0. The 0.2.0 record itself is not edited.
- Live effect by design: NONE. The refreshed jar templates govern first extraction /
  data-folder reset / fresh install only; the live data-folder stage files were not touched,
  and the Nov 14 stage-5 flip reads the data-folder packet files (pre-flip gate satisfied
  board-manager run 100: stage5 spells 189,295 B / d34112ba… EXACT).
