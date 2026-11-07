# PUSH-NOTES — ExcelClaims 0.1.2 records (for the parent / board-manager)

Staged locally by the build agent per the standing rule (generic
build agents stage records; the parent runs all GitHub legs).
Nothing here has been pushed.

## Publish these (records workflow — pre-approved, Mike Nov 4, 2026)

1. **wrv-plugins repo — one draft record PR**, batched in ONE
   `push_files` call (connector route while Mike is awake; browser
   route if asleep/busy, per the Oct 25, 2026 routing rule):
   - `ExcelClaims-0.1.2.jar` (99,370 B, SHA-256
     `b4663fa3d377bafa06803f8b4d21294ac1ea6b720614a263cc5fb3d7ff120c5d`)
   - `ExcelClaims-0.1.2-src.zip` (113,714 B, SHA-256
     `2f3d8c474c85e42328ba848599018d7f5cdf432b1c5e94cf8da77036792dab4a`)
   - `SOURCE.md` (unchanged from 0.1.0 — the clean-room basis is
     the same tree), `BUILD-RECORD.md` (now carries the 0.1.2
     section), `INVENTORY.md` (0.1.2 addendum appended —
     expiration rows 13–16 marked superseded by design),
     `FINGERPRINTS.md` (0.1.2 addendum appended),
     `INSTALL-PACKET-0.1.2.md`, `SHA256SUMS` (0.1.2 lines
     appended) from `~/workspace/excelclaims-build/`
   - PR stays an OPEN DRAFT (record PRs are never merged).
2. **Drive deposit** (AI Shared Files / Minecraft WRV folder, per
   the standing layout): the same jar + source zip + a `SOURCE.md`
   alongside them, matching the repo copies byte-for-byte.
3. **Daily log / Route Sheet / Big Brain** updates ride with the
   parent's close-out of this build (the build agent appended its
   lines to `~/memory/2026-11-07.md`).

## Already filed by the build agent

- The swap packet is PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
  ("PENDING — ExcelClaims 0.1.2 (added Nov 7, 2026)"). Its
  pre-window gate (live build must be the 0.1.1 build id) is
  satisfiable — 0.1.1 landed Nov 7, 10:04–10:37 AM CST — and its
  config step supersedes the 7/14/60 clocks that window restored:
  at 0.1.2 install the live expiration section is rewritten to
  `default-days: never` per Mike's ruling.

## Do NOT publish

- The rigs (`~/workspace/excelclaims-build/rig/` — `server-0.1.2/`,
  `server-0.1.2-expire/`, the staging scripts, probe sources/jars,
  fixture values, boot logs) stay local. BUILD-RECORD.md
  references them by local path, as with prior builds.
- No GitHub release (house rule: releases carry a full changelog
  or don't ship; 0.1.2's record is the draft PR above).
