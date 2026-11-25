# ExcelProtect 0.1.14 — RECEIPTS (live-premise pulls + build + rig evidence)

## Live-premise pulls (G1, fresh, Homebase scp — no fallback)

- PULL-01 (Nov 24, 2026, 15:33 CST): live jar
  `D:\muse-servers\wrv\plugins\ExcelProtect-0.1.13.jar` pulled to
  `~/workspace/excelprotect-ridfix-20261124/pull/ExcelProtect-0.1.13.jar`
  = 305,079 B / SHA-256
  `e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28`
  — byte-identical to the installed 0.1.13 candidate of record.
- PULL-02 (Nov 24, 2026, 15:33 CST): live config
  `plugins/ExcelProtect/config.yml` pulled to
  `~/workspace/excelprotect-ridfix-20261124/pull/config.yml`
  = 16,002 B / SHA-256
  `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b`
  — byte-identical to the candidate tree's bundled config.
  This packet changes NO config.

## Base gate

- A scratch copy of the 0.1.13 build tree
  (`~/workspace/excelprotect-eventts-20261124/build-tree/`), built
  unmodified in this lane's tree, reproduced the live jar
  BYTE-IDENTICAL (`e9fcefc6…`, 305,079 B) with suite 129/129.
  The 0.1.14 work was done in that fresh copy
  (`~/workspace/excelprotect-ridfix-20261124/build-tree/`);
  the 0.1.13 tree is untouched.

## Build

- Candidate `ExcelProtect-0.1.14.jar` = 305,260 B / SHA-256
  `b4b7fb0cac741f4b86853c575e72ea7b611b9653fda4d95e5d55913d7bf1dd85`;
  fingerprint `EXC-PROTECT-0.1.14-WRV-20261124-01-e14114`
  (fingerprint gate PASS inside the build).
- Deterministic double-build: `b4b7fb0c…` twice.
- Suite: 133 found / 133 successful / 0 failed (129 pre-existing,
  none modified, + 4 new stamp-gate tests).
- plugin.yml inside the jar reads version '0.1.14'; bundled config
  `c1720401…` (unchanged from 0.1.13 / live).
- Source zip `excelprotect-0.1.14-source.zip` = 219,466 B / SHA-256
  `7426ba7af2e1085e97c726ed6b966aa7fc3362d8eca9d768bb78bfe8f139718f`.

## Mirror rig (Homebase test-server mirror, Nov 24, 2026)

Venue: `C:\Users\MikeExcel\wrv-testserver\server\` (port 25566; the
live server on the same PC was never touched — verified still
running afterwards via the panel, uptime 3h 6m from its 12:26 start).
Harness: RidFixRig 1.0 (source `rig/src/RidFixRig.java`; run-3 jar
6,773 B / `1CA015EF…`, compiled on the PC against the mirror's own
Paper jars). One NMS fake player ("RigRid") runs three drop ->
re-pick cases through the real Bukkit event path. Evidence files:
`rig/evidence/boot-ridfix-run3.log`, `rig/evidence/items-run3.jsonl`
(runs 1–2 logs also kept — see BUILD-RECORD.md for what they showed
and why run 3 is the proof of record).

Run 3 (boot 15:29 CST; Protect READY version=0.1.14; SEVERE 0;
SERVER-EXIT 0):

- Case A (named STACKABLE — rabbit "Fresh Rat", the Jester case):
  dropped entity `A-ENTITY amt=1 rid=no`; the items-stream drop
  record carries NO rid field
  (`"item":{"id":"minecraft:rabbit","count":1,"name":"Fresh Rat"}`);
  after re-entry the copy MERGED with the clean copies:
  `A-INV [slot 10: minecraft:rabbit x4 name=Fresh Rat rid=no]` —
  one stack of 4 where 0.1.13 would have left a split singleton.
  (This run's re-entry used the labeled addItem simulation after
  the fake's real pickup did not fire in 240 ticks; run 1's real
  pickup consumed the same unstamped entity. addItem is the exact
  merge operation the pickup path performs.)
- Case B (named UNSTACKABLE — iron sword "Rig Blade"): dropped
  entity `B-ENTITY amt=1 rid=yes(epmwv7kew1-c5b874)`; the drop
  record carries `"rid":"epmwv7kew1-c5b874"` — provenance
  stamping and the drop record both intact for gear.
- Case C (LEGACY already-stamped stackable — rabbit x2 carrying
  rid `eplegacy00-test`): the drop record for the dropped stack
  reads `"count":2,...,"rid":"eplegacy00-test"` — the legacy rid
  is NOT stripped and NOT re-stamped; the stack drops whole.
  (Design consequence, stated in the packet: legacy stamped
  stackables stay solo until used — vanilla similarity rules —
  but no new splits are created.)

Mirror restored byte-exact after the final run (cleanup output):
Protect 0.1.8 jar `73373B90…` / 268,894 B, config `7A423378…` /
7,991 B, today's items stream `B1D293D2…` / 45,511 B — all equal
to pre-rig state; candidate + harness jars removed.
