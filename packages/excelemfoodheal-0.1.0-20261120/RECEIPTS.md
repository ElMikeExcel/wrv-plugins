# RECEIPTS - ExcelEMFoodHeal 0.1.0 (D162)

## Pinned EliteMobs premise

- Build/rig compile target: `libs/EliteMobs-10.9.10.jar`, copied from
  the Nov 20 audit pull
  (`~/workspace/plugin-audit-2026-11-20-rerun/pull/20261120-062811/plugins/`)
  and re-hashed on copy: 14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`
  — the live jar's bytes (file name 10.9.10, internal version
  10.9.14). The API used, verified on these bytes via javap:
  `com.magmaguy.elitemobs.dungeons.EliteMobsWorld.isEliteMobsWorld(java.util.UUID)`.
- New plugin: no live ExcelEMFoodHeal jar exists; no live-premise
  conflict is possible. The install packet's only live gate is the
  STOP-if-present check on the file name.

## Candidate artifacts (build folder)

- `ExcelEMFoodHeal-0.1.0.jar` — 15,564 B / SHA-256
  `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`;
  deterministic double-build byte-identical (`cmp` clean).
- `ExcelEMFoodHeal-0.1.0-source.zip` — 16,436 B / SHA-256
  `2535d7aa2fcd40e41b21f5c36e5b45b7e5ed542c8aff57774e4fd8099df0a635`.
- `SHA256SUMS` in the build folder carries all three hashes.

## Proof summary

- Unit tests: 137/137 pass (`run-tests.sh`, plain JVM).
- Rig (Paper 26.2-123 + the pinned EliteMobs jar, boot order
  EliteMobs -> ExcelEMFoodHeal, SEVERE 0): all driver cases PASS —
  API, BOOT, S3 overworld no-op (health flat, no FOODHEAL line),
  S1-instant (51.0 exactly), S1 HoT completion (59.5 read at the
  completion poll, FOODHEAL complete line logged), S2-pause (health
  frozen at 42.5 across the combat window; pause line at
  remaining=7.10%), S2-resume (resume line exactly 20 s after the
  hit, remainder undrained), S2 completion (49.5), S4 clamp
  (100.0 / 100.0, never above), S5 replace-not-stack (46.5, inside
  the 45.4-47.0 band; stacking would approach 54.1).
  Evidence: `rig/runtime.log` (RIGRESULT + FOODHEAL lines),
  driver source in `rig/driver/`.
- Expiry (90 s) is proven at unit level on the injected clock
  (including expiry mid-pause); it was not re-proven in rig real
  time.
