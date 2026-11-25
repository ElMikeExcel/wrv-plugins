# INSTALL PACKET — ExcelProtect 0.1.13 "Event Time" (held-heal event_ts_ms + onDisable flush)

- Lint: 4340 B / max line 93 / non-ASCII 12
- **Packet:** ~/workspace/excelprotect-eventts-20261124/INSTALL-PACKET.md
- **Receipts:** ~/workspace/excelprotect-eventts-20261124/RECEIPTS.md
  (4,304 B / 4e7acaab0d53a3c562522cbad69d08023aafc6fe3c2652480358c6137c600710)
- **Record:** ~/workspace/excelprotect-eventts-20261124/RECORD.md
- **Diagnosis of record:** ~/workspace/excelprotect-invisible-heal-20261124/DIAGNOSIS.md
  (seam B — this packet fixes seam B only; seams A and C are other lanes)

## Live-premise receipts (G1 — see RECEIPTS.md for full lines)

- Live jar `plugins/ExcelProtect-0.1.12.jar` = 304,634 B /
  `6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b`
  — fresh Homebase pull Nov 24, 2026, 04:10 CST (PULL-01).
- Live config `plugins/ExcelProtect/config.yml` =
  `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b`
  — fresh pull Nov 24, 2026 (PULL-02); byte-identical to the candidate
  tree's bundled config. This packet changes NO config.

## Candidate

- `~/workspace/excelprotect-eventts-20261124/ExcelProtect-0.1.13.jar`
  — 305,079 B / SHA-256
  `e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28`;
  build `EXC-PROTECT-0.1.13-WRV-20261124-01-e24a13`; tests 129/129;
  deterministic double-build byte-identical; mirror rig PASS
  (RECEIPTS.md — held CUSTOM heal flushed at disable carries
  event_ts_ms = event time, envelope ts_ms = flush time).

## Live-jar gate (STOP on mismatch)

- Live file `plugins/ExcelProtect-0.1.12.jar` must hash to
  304,634 B / `6e3cf398…` (PULL-01) at install or STOP and re-base.

## Scope

- GATED jar swap only: 0.1.12 jar held aside as an inert .bak
  (never co-resident), candidate placed as
  `plugins/ExcelProtect-0.1.13.jar`.
- What changes: combat_heal records written after CombatCore's
  heal-attribution hold (claim-resolved or window-expired flushes)
  gain ONE additive field, `event_ts_ms` = the regain's original
  event time (the envelope ts/ts_ms stay the flush time); and
  ExcelProtect.onDisable now flushes every still-held heal through
  the same writers before the ledger flush, instead of dropping
  them from the combat stream at restart.
- What does NOT change: config (none placed, none edited), data
  files, immediate-heal records (byte-identical shape — pinned by
  unit test), every other stream, every other plugin. No new
  dependencies; no new config keys. Rollback: restore the held-aside
  0.1.12 jar (records written by 0.1.13 stay readable — the field is
  additive and 0.1.12 readers ignore unknown fields).

## Gates

- Marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0/10 twice.

## Boot verify (script-named lines, from the rig boot of this jar)

- `grep -c 'Enabling ExcelProtect v0.1.13' latest.log` = 1
- `grep -c 'READY version=0.1.13' latest.log` = 1
- `grep -c 'Magic CastEvent feed hooked' latest.log` = 1
- SEVERE 0 attributable to ExcelProtect; guild LOCKED state unchanged.
- Console `/excelprotect status` reports `version=0.1.13`.

## Same-pass surfaces

- CHECKED-NO-CHANGE, all of them: no player-facing behavior, text,
  menu, price, or command changes — Guide knowledge/intents, website
  pair, What's New (website + in-game), broadcaster tips, in-game
  menus all untouched by this fix.
- Records owed at install (supervisor chain, not this window):
  wrv-plugins OPEN DRAFT record PR + Drive deposit (jar + source
  zip) + website plugin-row version bump 0.1.12 -> 0.1.13
  (G2 duplicate check before any push).

## Proof owed after install

- Live re-proof (parent/nightly lane): in the first live day with a
  held heal flushed across a damage anchor, the combat-stream record
  carries `event_ts_ms` close to the heal's true time while `ts_ms`
  is the later flush time; and a restart with a heal inside its
  claim window leaves that heal in the combat stream (pre-0.1.13 it
  vanished). The nightly visibility audit's reconciliation upgrade
  (prefer event_ts_ms) is a separate audit-lane item — this packet
  only supplies the field.

## Marker on install

- `~/workspace/excelprotect-eventts-20261124/INSTALLED.txt`
