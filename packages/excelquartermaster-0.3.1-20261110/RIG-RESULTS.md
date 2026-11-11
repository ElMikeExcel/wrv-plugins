# RIG RESULTS — ExcelQuartermaster 0.3.1 (console status)

Rig: minimal local Paper 26.2-123 server (the unpacked rig
server from the 0.3.0 QM scratch rig — paper.jar + its
versions/libraries/cache, copied to /tmp; the paperclip stub
cannot download in this sandbox, so the pre-unpacked tree was
used) with ONLY ExcelQuartermaster-0.3.1.jar in plugins/
(SHA-256 verified a11aa0fc…f9c4 in the rig plugins folder
before boot). Offline mode, flat world, no other plugins —
every softdepend is absent, which the plugin handles by design
(GuildGate not answering → seals unknown; Magic ABSENT → tales
only; Shopkeepers redirect unavailable). Full log: `boot.log`
in this folder; the exact console commands: `cmdfile.txt`.

The pack state was seeded before boot with three records
(`pack-state-seed.txt`): DECLINED at 2 offers, DECLINED at 5
(exhausted), LOADED at 3 (accepted). Every command was typed
on the server's stdin — the REAL console sender
(`CONSOLE`), exactly the caller this packet exists for.

## Boot

- "Enabling ExcelQuartermaster v0.3.1" present; enable lines
  report "3 players with saved pack state" (the seed loaded).
- SEVERE: 0 in the whole boot + exercise + shutdown. WARN lines
  are Paper/environment generics (root user, terminal, offline
  mode), none attributable to ExcelQuartermaster.
- Clean stop: "Disabling ExcelQuartermaster v0.3.1".

## Behavior exercise — 12/12 PASS (verbatim lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Boot reaches Done with v0.3.1 enabled | PASS |
| 2 | SEVERE count for the whole run | PASS — 0 |
| 3 | Seeded pack state loaded (3 players on the enable line) | PASS |
| 4 | `qm status <uuid>` DECLINED mid-count → "offers used 2 of 5, 3 remaining; pack state: DECLINED; last change 1000." | PASS |
| 5 | `qm status <uuid>` LOADED → "offers used 3 of 5, 0 remaining; pack state: LOADED (accepted - has the voice pack; terminal); last change 4000." | PASS |
| 6 | `qm status <uuid>` DECLINED exhausted → "offers used 5 of 5, 0 remaining; pack state: DECLINED (offers exhausted - never offered again unless an admin runs /qm packreset); last change 2000." | PASS |
| 7 | `qm status <unknown uuid>` → the no-record line (0 of 5, next join offers 1 of 5) | PASS |
| 8 | `qm status NobodyHereXYZ` → "No online player named 'NobodyHereXYZ', and it is not a UUID. Nothing was looked up." (packreset's resolution, unchanged) | PASS |
| 9 | Bare `qm` → both usage lines (packreset + status), mutates nothing | PASS |
| 10 | packreset regression: `qm packreset <uuid>` on the status-read target → RESET with the 0.3.0 reply text — proves the earlier status reads left the record intact | PASS |
| 11 | `qm status <same uuid>` after the reset → the no-record line | PASS |
| 12 | Read-only on disk: after shutdown, java-pack-state.txt (`pack-state-after.txt`) holds exactly the two untouched records — the status reads created no record for the unknown UUID and changed none | PASS |

## Limitations, stated plainly

- Offline targets were addressed by UUID, because that is the
  only offline resolution the existing packreset path supports
  (online name → raw UUID); 0.3.1 deliberately matches it, so
  the rig proves the same shape. An online target addressed by
  name uses the identical code path up to `getPlayerExact`,
  shared with packreset, which is live-proven 0.2.2 behaviour.
- No fake/real player was joined: `status` is read-only and
  console-facing, and no player-facing path changed, so there
  is no player surface to exercise. The non-op denial is
  Bukkit's plugin.yml gate (default-op admin node), pinned by
  the PackStatusTest / PackResetTest plugin.yml checks rather
  than observed in this log.
