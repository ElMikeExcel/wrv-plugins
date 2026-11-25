# ExcelProtect 0.1.13 "Event Time" — BUILD RECORD

**Lane:** build lane (desk + mirror rig only — no installs, no live-server
changes, no messages). **Date:** Tue Nov 24, 2026.
**Spec of record:** `~/workspace/excelprotect-invisible-heal-20261124/DIAGNOSIS.md`
seam B + §5 fix 2 (Protect half). Seams A (audit dedupe) and C (EliteMobs
boss-kill setHealth) are NOT in this build — separate lanes by design.
**Deliverable folder:** `~/workspace/excelprotect-eventts-20261124/`
(receipts: RECEIPTS.md `4e7acaab…`; packet: INSTALL-PACKET.md `f5dc10d2…`;
hashes: SHA256SUMS).

## Provenance — the base tree IS the live source (verified, not assumed)

- Live is ExcelProtect 0.1.12 (MANIFEST: installed Nov 16, 2026, candidate
  304,634 B / `6e3cf398…` from `~/workspace/board-r432/protect-build/`).
- Fresh G1 pull Nov 24, 2026 04:10 CST (Homebase scp, no fallback):
  live jar = 304,634 B / `6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b`,
  plugin.yml '0.1.12', fingerprint `EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12`;
  live config = `c1720401…` (RECEIPTS.md PULL-01/02).
- Base gate: a scratch copy of the board-r432 build tree, built unmodified,
  reproduced the live jar BYTE-IDENTICAL (`6e3cf398…`) with suite 125/125.
  The 0.1.13 work was done in a fresh copy
  (`excelprotect-eventts-20261124/build-tree/`); the 0.1.12 tree is untouched.

## The defect (diagnosis seam B, restated)

`CombatCore.onHeal` holds every CUSTOM / MAGIC_REGEN regain in a `pending`
buffer for the 6,500 ms Magic cast-claim window. When a held heal is finally
written (claim-resolved via `creditResolved`, or window-expired via
`genericHeal`), `Ledger.envelope()` stamps ts/ts_ms = FLUSH time; the
original event time (`PendingHeal.ms`) is never written. The dominant
EM-world heal source is thus stamped 6.5–16.5 s late in the combat stream,
and the nightly visibility audit scores flush-crossing heals "invisible"
(≈14.1 HP of Jester's Nov 24 excess; the Nov 23 + Nov 24 bot intervals
similarly). Corollary: pending heals were never flushed on plugin disable —
a restart dropped them from the combat stream entirely (the healing stream,
which has no hold, kept them).

## The fix (exactly the tasked scope, 2 production files)

`src/com/winkreyes/excelprotect/CombatCore.java` (39,981 → 42,442 B):
- Both late writers gained an `eventTsMs` overload: the with-location
  `genericHeal(...)` and `creditResolved(...)` now accept the held heal's
  event time and, when > 0, add ONE field — `event_ts_ms` — to the
  combat_heal record. The pre-existing signatures delegate with 0, so every
  immediate (never-held) call site is untouched and its records are
  byte-identical to 0.1.12.
- `resolvePending()` passes `p.ms()` (the PendingHeal's stored event time)
  on BOTH flush paths: claim-resolved SPELL credits and window-expired
  generic heals.
- New `public void flushAllPending()`: runs the normal resolution pass,
  then writes any still-held heal (unexpired, unmatched) as a generic heal
  carrying its event_ts_ms. Entries leave the buffer as written —
  exactly-once; a second call writes nothing.
- The envelope is NOT modified: ts/ts_ms remain the flush time (existing
  readers see no break); `event_ts_ms` is additive and sits with the other
  optional record fields (after `source` on generic heals, after `cast_id`
  on credited heals).

`src/com/winkreyes/excelprotect/ExcelProtect.java` (77,901 → 78,635 B):
- `onDisable()` now calls `combat.flushAllPending()` (fail-soft try/catch,
  the auditWatch.flushAll pattern) immediately before `ledger.flushAll()`.
- Version stamps: READY line + `/excelprotect status` report 0.1.13; class
  javadoc gains the 0.1.13 entry. (One adjacent observation, NOT changed —
  out of scope: onDisable also never called the existing
  `combat.flushPendingDamage()`; spell-shaped damage records held at
  shutdown have the same drop shape. Flagged for the parent.)

Version furniture (no behavior): `plugin.yml` (3,574 → 3,834 B) version
'0.1.13' + description clause; `local-build.sh` fingerprint
`EXC-PROTECT-0.1.13-WRV-20261124-01-e24a13`, jar name, pinned dates
2026-11-24. `config.yml` byte-identical to 0.1.12 AND to live (`c1720401…`)
— no config change, no new keys, no new dependencies.

## Tests — full suite + 4 focused (tests/.../ProtectTest.java 164,086 → 172,566 B)

Suite: **129 found / 129 successful / 0 failed / 0 skipped** on both builds
(125 pre-existing, none modified, + 4 new). The new tests drive the REAL
CombatCore + REAL Ledger and read the emitted combat-stream bytes back from
disk (pending buffer + claims seeded by reflection — the suite's existing
idiom; fight summaries off in these configs because the unit JVM has no
Bukkit server and fight bookkeeping is not what is pinned):

1. `heldHealClaimFlushCarriesEventTime` (task a): a CUSTOM heal held 3 s,
   claim lands → SPELL credit with `event_ts_ms` == the event time and
   envelope `ts_ms` strictly later (flush time preserved).
2. `heldHealExpiryFlushCarriesEventTime` (task b): a heal held 60 s with no
   claim → generic flush with `event_ts_ms` == the event time, amount split
   intact.
3. `disableFlushEmitsPendingExactlyOnce` (task c): `flushAllPending()` with
   one expired + one still-fresh unmatched heal writes BOTH, each with its
   own event_ts_ms; a second call writes nothing (file line count stable).
4. `immediateHealRecordsHaveNoEventTs` (task d): the immediate generic and
   credited writers emit records with NO `event_ts_ms` anywhere, and the
   generic record's field tail matches the pre-0.1.13 shape exactly
   (`"amount":2,"effective":2,"overheal":0,"source":"SATIATED"`).

No pre-existing failures: the pristine-tree base gate ran the same suite at
125/125 before any edit, and the candidate runs 129/129.

## Candidate

| Artifact | Size (B) | SHA-256 |
|---|---:|---|
| `ExcelProtect-0.1.13.jar` | 305,079 | `e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28` |
| `excelprotect-0.1.13-source.zip` | 217,705 | `14b779332fe27e92e6e0ee8ec56df4022ca023839dbf1de65657621c403f6281` |

Deterministic double-build PASS (`e9fcefc6…` twice); fingerprint gate PASS;
plugin.yml inside the jar reads version '0.1.13'; bundled config `c1720401…`.

## Mirror rig — PASS (Homebase test-server mirror, Nov 24, 2026)

Full evidence in RECEIPTS.md; harness + scripts + logs under `rig/`.
One synthetic CUSTOM regain (2.0 HP) fired for an NMS fake player with
Magic hooked (boot log: `Magic CastEvent feed hooked`), server shut down
2 s later — inside the claim window, before any expiry path could run.
The combat stream gained exactly one record for the fake player:
`event_ts_ms=1795515631386` (the event, 17 ms after the harness's
pre-call stamp) with envelope `ts_ms=1795515634617` (the disable flush,
3,231 ms later) — written ONLY because onDisable flushed the held heal.
Boot: Enabling v0.1.13 x1, READY version=0.1.13 x1, SEVERE 0, SERVER-EXIT 0.
(The one stack trace in the boot log is DecentHolograms failing to modify
the fake player's network pipeline — a rig-fake artifact, not Protect.)
Mirror restored byte-exact afterward (0.1.8 jar `73373B90…`, config
`7A423378…`, combat stream `3BC0C333…` — all equal to pre-rig hashes;
candidate + harness removed), and the LIVE server on the same PC was
verified still running, untouched, after the rig.

## Filing

Filed per D212 as a pointer-stub PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md` (packet + receipts + this record
in the deliverable folder). NOT installed by this lane. Records close-out
(wrv-plugins draft PR + Drive deposit + website plugin-row bump) is owed at
install time per the packet, supervisor chain.

VERDICT: BUILD COMPLETE — candidate proven by suite (129/129, record bytes
read back), deterministic double-build, and a mirror rig boot that exercised
the held-heal → disable-flush path end to end.
