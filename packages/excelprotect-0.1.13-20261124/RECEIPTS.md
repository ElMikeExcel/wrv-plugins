# ExcelProtect 0.1.13 — RECEIPTS (live-premise pulls + build + rig evidence)

All SHA-256 full-length. Times America/Chicago (CST). Pull route: Homebase
scp via ~/workspace/server-gui-20261121/work/ssh-pc.sh (no fallback used).

## G1 live-premise pulls — Nov 24, 2026, 04:10:03–04:11:22 CST

- PULL-01 live jar: `D:\muse-servers\wrv\plugins\ExcelProtect-0.1.12.jar`
  pulled to `~/workspace/excelprotect-eventts-20261124/pull/live-ExcelProtect-0.1.12.jar`
  — 304,634 B / `6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b`.
  plugin.yml inside reads version '0.1.12'; excel-build.txt =
  `EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12`. This is the install GATE.
- PULL-02 live config: `D:\muse-servers\wrv\plugins\ExcelProtect\config.yml`
  pulled to `~/workspace/excelprotect-eventts-20261124/pull/live-config.yml`
  — `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b`,
  byte-identical to the candidate tree's bundled config.yml (same hash).
  This packet changes NO config; the pull is premise evidence only.

## Base gate — pristine 0.1.12 rebuild (Nov 24, 2026)

- Scratch copy of `~/workspace/board-r432/protect-build/build-tree/` built
  unmodified with its own local-build.sh: suite 125/125, jar byte-identical
  to the 0.1.12 candidate AND to PULL-01 — 304,634 B / `6e3cf398…`.
  Conclusion: the board-r432 tree is the live 0.1.12 source of truth, and
  the 0.1.13 tree is a copy of it (the 0.1.12 tree was not mutated).

## Candidate

- Jar: `~/workspace/excelprotect-eventts-20261124/ExcelProtect-0.1.13.jar`
  (also at `build-tree/ExcelProtect-0.1.13.jar`, identical) — 305,079 B /
  `e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28`.
  plugin.yml version '0.1.13'; excel-build.txt =
  `EXC-PROTECT-0.1.13-WRV-20261124-01-e24a13`; fingerprint gate PASS.
- Deterministic double-build: PASS — two consecutive local-build.sh runs
  produced byte-identical jars (`e9fcefc6…` both times).
- Suite: 129 tests found, 129 successful, 0 failed, 0 skipped (125
  pre-existing + 4 new), on both builds.
- Source zip: `~/workspace/excelprotect-eventts-20261124/excelprotect-0.1.13-source.zip`
  — 217,705 B / `14b779332fe27e92e6e0ee8ec56df4022ca023839dbf1de65657621c403f6281`.
- Config: unchanged — tree config.yml `c1720401…`, byte-identical to live.
- Full per-file hashes: `~/workspace/excelprotect-eventts-20261124/SHA256SUMS`.

## Mirror rig (Homebase test-server mirror, Nov 24, 2026, boot 04:20 CST)

- Venue: `C:\Users\MikeExcel\wrv-testserver\server\` (port 25566; the live
  server on the same PC was never touched — verified still running after).
- Harness: HealTsRig 1.0 (source `rig/src/HealTsRig.java` `35c687a3…`;
  compiled on the PC against the mirror's Paper jars; placed jar 4,579 B /
  `3C18F4E0…`). It fires ONE synthetic CUSTOM EntityRegainHealthEvent
  (2.0 HP) for an NMS fake player while Magic is hooked, then shuts the
  server down 2 s later — inside the 6,500 ms claim window, before any
  expiry flush — so only the onDisable flush can write the record.
- Boot evidence (`rig/evidence/boot-eventts.log` `28e0f448…`):
  `Enabling ExcelProtect v0.1.13` x1; `combat: Magic CastEvent feed hooked
  (spell attribution on)` x1 (the hold path was genuinely armed);
  `READY version=0.1.13` x1; `HEALTS-FIRED ms=1795515631369`;
  `HEALTS-SHUTDOWN ms=1795515633164`; `Disabling ExcelProtect v0.1.13`;
  SEVERE 0; SERVER-EXIT 0.
- Combat-stream evidence (`rig/evidence/combat-2026-11-24.jsonl`
  `68386cf3…`): exactly ONE RigHealTs combat_heal line —
  `{"ts":"2026-11-24T04:20:34.617-06:00","ts_ms":1795515634617,...,
  "type":"combat_heal","target":"RigHealTs","amount":2,"effective":2,
  "overheal":0,"source":"CUSTOM","event_ts_ms":1795515631386}`.
  event_ts_ms is 17 ms after the harness's pre-call timestamp (the moment
  CombatCore.onHeal stamped the event) and 3,231 ms BEFORE the envelope
  ts_ms (the shutdown flush) — the field carries the event time, and the
  record exists only because onDisable flushed the held heal.
- Mirror restored byte-exact (cleanup output in RECORD.md): Protect 0.1.8
  jar `73373B90…` / 268,894 B, config `7A423378…` / 7,991 B, today's
  combat stream `3BC0C333…` / 46,054 B — all equal to pre-rig state;
  candidate + harness jars removed.
