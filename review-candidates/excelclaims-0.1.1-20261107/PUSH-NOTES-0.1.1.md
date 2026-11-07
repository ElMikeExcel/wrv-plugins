# PUSH-NOTES — ExcelClaims 0.1.1 records (for the parent / board-manager)

Staged locally by the build agent per the standing rule (generic
build agents stage records; the parent runs all GitHub legs).
Nothing here has been pushed.

## Publish these (records workflow — pre-approved, Mike Nov 4, 2026)

1. **wrv-plugins repo — one draft record PR**, batched in ONE
   `push_files` call (connector route while Mike is awake; browser
   route if asleep/busy, per the Oct 25, 2026 routing rule):
   - `ExcelClaims-0.1.1.jar` (97,503 B, SHA-256
     `ff5ea0810d1df5866a423ac126e3145354bca25d2600cfc42a5536362b540706`)
   - `ExcelClaims-0.1.1-src.zip` (99,103 B, SHA-256
     `3f958c4ae96ef358b72fa75b149edd5f3cb1323b5f2f70c6bc1df837fe86f560`)
   - `SOURCE.md` (unchanged from 0.1.0 — the clean-room basis is
     the same tree), `BUILD-RECORD.md` (now carries the 0.1.1
     section), `INVENTORY.md` (unchanged), `FINGERPRINTS.md`
     (0.1.1 addendum appended), `INSTALL-PACKET-0.1.1.md`,
     `SHA256SUMS` (0.1.1 lines appended) from
     `~/workspace/excelclaims-build/`
   - PR stays an OPEN DRAFT (record PRs are never merged).
2. **Drive deposit** (AI Shared Files / Minecraft WRV folder, per
   the standing layout): the same jar + source zip + a `SOURCE.md`
   alongside them, matching the repo copies byte-for-byte.
3. **Daily log / Route Sheet / Big Brain** updates ride with the
   parent's close-out of this build (the build agent appended its
   lines to `~/memory/2026-11-07.md`).

## Already filed by the build agent

- The swap packet is PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
  ("PENDING — ExcelClaims 0.1.1 (added Nov 7, 2026)"). It restores
  the expiration clocks frozen by the Nov 7 recovery — the packet
  is the only place that restore should ever be executed from.

## Do NOT publish

- The rigs (`~/workspace/excelclaims-build/rig/` — the 0.1.0 rig
  server, `server-0.1.1/`, `server-expire/`, probe/driver sources
  and jars, staging scripts, fixture values, boot logs) stay
  local. BUILD-RECORD.md references them by local path, as with
  prior builds.
- No GitHub release (house rule: releases carry a full changelog
  or don't ship; 0.1.1's record is the draft PR above).
