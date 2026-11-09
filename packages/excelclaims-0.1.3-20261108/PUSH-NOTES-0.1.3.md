# ExcelClaims 0.1.3 — records publication (parent leg)

The build lane does not push. Everything below is STAGED and
verified locally in `~/workspace/excelclaims-build/`; the
parent publishes (recording work is PRE-APPROVED standard
workflow per Mike, Nov 4, 2026 — never listed as waiting on
Mike; report only as done).

## What ships in the record

- `ExcelClaims-0.1.3.jar` — 102,334 B, SHA-256
  21f3f7999c36259b6db9f3cc79208501b4ebbf47372a06db567ce2d3a406fbd7,
  build `EXC-CLAIMS-0.1.3-WRV-20261108-04-bd9ff4`.
- `ExcelClaims-0.1.3-src.zip` — 119,864 B, SHA-256
  99d2150b4765d85685c842505baa5a40ac42f2d68bf54cc3556abf5d2a1bdac9
  (src/ + tests/ + resources/ + build scripts incl.
  `local-build-0.1.3.sh` + records).
- `BUILD-RECORD.md` (0.1.3 section), `FINGERPRINTS.md`
  (0.1.3 addendum), `INSTALL-PACKET-0.1.3.md`, this file.
- Records: unit suite 79/79 (66 of 0.1.2 + 13 new
  CornerPillarTest), deterministic double-build byte-identical,
  4-adapter link-check PASS, fingerprint gate PASS, identity
  gate PASS; rig boot smoke over the 0.1.2 rig store
  (enable line build 0.1.3-04-bd9ff4, 8 claims / 5 accounts,
  FACADE-SMOKE 14/14, log `rig/boot-0.1.3.log`).

## Publication steps (parent)

1. **wrv-plugins record PR (draft, never merged):** package
   directory `packages/excelclaims-0.1.3-20261108/` following
   the 0.1.2 package's shape: SOURCE.md (repo, build id,
   checksums, ownership split — Iris/Mike), the jar + src zip
   are NOT committed to git (Drive holds binaries per standing
   practice — follow the 0.1.2 package exactly), records
   copies (BUILD-RECORD 0.1.3 section, FINGERPRINTS addendum,
   INSTALL-PACKET-0.1.3, PUSH-NOTES-0.1.3).
2. **Drive deposit:** jar + src zip + records into the shared
   Drive "Minecraft WRV" folder beside the 0.1.2 deposits,
   with the same SOURCE.md. No passwords, tokens, or IPs in
   anything uploaded (the jar contains none).
3. **Route Sheet + Big Brain:** one line each — ExcelClaims
   line now reads 0.1.3 built/staged (corner pillars), install
   queued as MANIFEST PENDING (gated, over 0.1.2 only).
4. **At install time (failsafe lane):** packet =
   `INSTALL-PACKET-0.1.3.md`; marker
   `~/workspace/excelclaims-build/INSTALLED-0.1.3.txt` written
   by the executor; same-pass surfaces per the packet's
   closing section (Guide "glowstone corner" wording →
   pillar shape; ExcelWhatsNew entry on BOTH surfaces;
   broadcaster tips check).

## What does NOT ship

- The rig trees (`rig/server-0.1.3/` etc.) stay local.
- No GitHub release (this line records via draft PR + Drive,
  same as 0.1.0–0.1.2).
- The 0.1.2 aside jar on the live server is the rollback —
  it is never deleted by this window and never published.
