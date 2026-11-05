# ExcelLandmarks 0.1.0 — source record

Plugin: ExcelLandmarks
Version: 0.1.0 (first release)
Record assembled: Nov 5, 2026 (records close-out pass)
Local build folder: `landmarks-build`

## What this is

Spawn landmarks for the Wink Reyes Valley server (WRV-100; Mike's GO
Nov 5, 2026 — "Yes to landmark"; design of record:
`~/workspace/village-identity/REMAP-DIRECTION-20261103.md`). Spawn is a
huge premade pasted build that players only land on and teleport out of;
Landmarks give its places a purpose. An admin writes `[Landmark]` on a
sign at spawn and the spot registers — name + TYPE:TIER from the sign,
prices from config (config-first fleet pattern, never hardcoded):
rent $500 / $1,500 / $3,000 per month, buy $10,000 / $25,000 / $50,000 —
Mike's Nov 3 price sketch, adopted as config defaults Nov 5, 2026.

Players TAP the sign and the spot menu pops (Mike's Nov 5 directives,
SPEC §6): one core menu model rendered as a native Floodgate form on
Bedrock and a chest inventory on Java — the ExcelMenu 2.0 dual stack.
The menu shows only what the viewer can do: available → Rent/Buy; own
rental → Renew / Buy Outright / Release; grace → Renew first, labelled
with the deadline; someone else's → view-only (Close is the ONLY
button). Every charge sits behind a confirm step. Two product types:
LANDMARKS (naming rights only) and PLOTS/AREAS (naming + build rights
via a GriefPrevention subdivision the plugin creates automatically at
purchase). Rentals run a 30-day clock + 7-day grace; on lapse the
finalize packs container contents + loose items, tallies the renter's
placed-block materials from ExcelProtect attribution, clears exactly
those blocks, deletes the subdivision, and delivers a redemption box
beside the sign that NEVER expires (settled Nov 4, 2026; explicitly
different from ExcelProtect Lost & Found's 14-day window). Full detail:
SPEC.md + CHANGELOG-0.1.0.md.

Two rig-found facts shaped the shipped design: (1) GP subdivisions
CANNOT change owners (`changeClaimOwner` throws NoTransferException),
so the rights model is TRUST — the buyer gets Build + manager on the
subdivision, verified by re-read, and the subdivision is deleted
wholesale at finalize (SPEC §5 amended); (2) the ExcelProtect logs-dir
path resolved one folder too high and would have read nothing live —
fixed at both call sites before ship.

## Built artifact (not included in this record — text only)

- Jar: `ExcelLandmarks-0.1.0.jar` — 77,996 bytes, SHA-256 `466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc`
  (deposited in Drive 'Minecraft WRV' > Build files; deterministic
  build — build.sh assembles twice per run with a hash assert, and two
  full builds came out byte-identical).

## Install / live status

Installed live Nov 5, 2026, 7:05–7:23 AM CST by wrv-install-failsafe
(backup "Landmarks pre-0.1.0 20261105" first, cap-at-6 prune of one
oldest manual "ExcelProtect pre-0.1.1 20261104"; both files NEW — jar +
`plugins/ExcelMenu/entries/landmarks.yml`, no existing file modified;
boot Done 20.082s; enable line verbatim "ExcelLandmarks 0.1.0 enabled:
spots=0, gp=ON (subdivisions), protect-attribution=ON,
floodgate-forms=ON, rent-period=30d, grace=7d"; plugins 78 → 79;
excelmenu reload registry 86 entries 0 failed; `landmarks admin status`
verified live; guild LOCKED 3 of 5 unchanged; TPS 20.0×3; SEVERE 0 —
INSTALLED.txt). The plugin is INERT at spots=0 until activation.
ACTIVATION is a later in-game step (parent/Mike): the one-time admin
sign-placement pass + the spawn-landmark tier inventory. Player proofs
owed after activation: Mike (Bedrock) sign-tap native form + rent flow;
Jester (Java) chest menu + view-only on another's spot; redemption-box
owner-only open. Same-pass surfaces (Guide/website/What's New/tip
check) are the parent's follow-on, tracked outside this record.

## Record contents

- `src/` — the version's source tree, including `src/plugin.yml` and
  `src/config.yml` as they live in the build tree (root-level
  `plugin.yml` / `config.yml` are the same two files, placed at the
  package root per record anatomy)
- `SPEC.md` — the build contract (from REMAP-DIRECTION-20261103.md +
  Mike's Nov 5 menu directives); `menu-entry/landmarks.yml` — the
  "My Landmarks" ExcelMenu compass door (registry 85 → 86 at install)
- `TEST-REPORT.md` — the build lane's full test report;
  `TEST-RESULTS.txt` — the close-out summary
- `BUILD-RECORD.md`, `INSTALL-PACKET.md` (as installed), `INSTALLED.txt`
  (the install record), `MANIFEST-ENTRY.md` (the filed queue entry)
- `rig/` — the rig driver (drive.py + driver source/plugin.yml) and the
  two Paper 26.3-235 boot evidence logs. The two rig BINARIES
  (`rig/driver/LmRig.jar`, `rig/driver/lib/annotations-24.1.0.jar`)
  are excluded from this text record; their sources are included.
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — compile inputs, hash-pinned
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
