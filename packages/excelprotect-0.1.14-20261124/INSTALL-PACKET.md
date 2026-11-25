# INSTALL PACKET — ExcelProtect 0.1.14 "Stack-Safe Provenance" (rid stamp only on unstackable items)

- Lint: 4634 B / max line 100 / non-ASCII 12
- **Packet:** ~/workspace/excelprotect-ridfix-20261124/INSTALL-PACKET.md
- **Receipts:** ~/workspace/excelprotect-ridfix-20261124/RECEIPTS.md
- **Record:** ~/workspace/excelprotect-ridfix-20261124/BUILD-RECORD.md
- **Backlog:** P-PROTECT-RID-STACKSPLIT-20261124 in
  ~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md
- **Player report of record:** Jester, Nov 24, 2026 (Discord DM,
  Fresh Rat stacking) — see the backlog entry and
  ~/workspace/jester-stacking-check-20261124/RECORD.md

## Live-premise receipts (G1 — see RECEIPTS.md for full lines)

- Live jar `plugins/ExcelProtect-0.1.13.jar` = 305,079 B /
  `e9fcefc61491b3f4aea6ec2ba672fa0695e20ddcc0f3dcfbae54b507093bbe28`
  — fresh Homebase pull Nov 24, 2026, 15:33 CST (PULL-01).
- Live config `plugins/ExcelProtect/config.yml` =
  `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b`
  — fresh pull Nov 24, 2026 (PULL-02); byte-identical to the
  candidate tree's bundled config. This packet changes NO config.

## Candidate

- `~/workspace/excelprotect-ridfix-20261124/ExcelProtect-0.1.14.jar`
  — 305,260 B / SHA-256
  `b4b7fb0cac741f4b86853c575e72ea7b611b9653fda4d95e5d55913d7bf1dd85`;
  build `EXC-PROTECT-0.1.14-WRV-20261124-01-e14114`; tests 133/133;
  deterministic double-build byte-identical; mirror rig PASS
  (RECEIPTS.md — stackable drop carries no rid and re-merges;
  gear drop still stamped with its record written; legacy
  stamped stackable keeps its rid).

## Live-jar gate (STOP on mismatch)

- Live file `plugins/ExcelProtect-0.1.13.jar` must hash to
  305,079 B / `e9fcefc6…` (PULL-01) at install or STOP and re-base.
- MOVE the outgoing jar aside (never copy, never co-resident);
  exactly-one ExcelProtect jar in plugins/ at Start (jar census).

## Scope

- GATED jar swap only: 0.1.13 jar held aside as an inert .bak,
  candidate placed as `plugins/ExcelProtect-0.1.14.jar`.
- What changes: the Amendment 3.4 provenance stamp
  (`excelprotect:rid`) is applied ONLY to unstackable
  fingerprint-class items (max stack size 1 — weapons, tools,
  armor, wands, totems, elytra, shulker boxes). Stackable items
  are never stamped at drop or craft, so a dropped stackable can
  always stack with its identical copies again. One gate inside
  `ItemEvents.stamp()` covers both call sites (drop + craft).
- What does NOT change: config (none placed, none edited), data
  files, record shapes (unstamped items simply carry no `rid`
  field, as non-fingerprint items never did), the Chains tracker
  (it never used the rid), every other stream, every other
  plugin. Already-stamped items keep their rid — nothing is
  stripped, so existing `export match=<rid>` timelines stay
  intact; a legacy stamped stackable remains a solo stack until
  it is used, but no new splits are created. No new
  dependencies; no new config keys. Rollback: restore the
  held-aside 0.1.13 jar (0.1.14 writes no new record fields, so
  rollback is clean).

## Gates

- Marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0/10 twice.

## Boot verify (script-named lines, from the rig boot of this jar)

- `grep -c 'Enabling ExcelProtect v0.1.14' latest.log` = 1
- `grep -c 'READY version=0.1.14' latest.log` = 1
- `grep -c 'Magic CastEvent feed hooked' latest.log` = 1
- SEVERE 0 attributable to ExcelProtect; guild state unchanged.
- Console `/excelprotect status` reports `version=0.1.14`.

## Same-pass surfaces

- See SURFACES.md in this folder: no player-facing text, menu,
  price, or command changes; the one surface with content is the
  records/website plugin-row bump at install time (supervisor
  chain) plus an optional player note to Jester (his report
  caused this fix — the ExcelEMFixes module rule's reason
  discipline, applied here to Protect).

## Proof owed after install

- Live re-proof (player ride, Jester): drop a named stackable
  (a Fresh Rat or any named food/quest item), pick it back up,
  confirm it stacks with the copies already in his inventory.
  Discharged by Jester's own report. The EM food-variant split
  (P-EMFOOD-STACKSPLIT-20261124) is a SEPARATE cause with its
  own filed fix — this packet closes only the Protect cause;
  if Jester's rats still split after both fixes, the remaining
  variant is the EM one.

## Marker on install

- `~/workspace/excelprotect-ridfix-20261124/INSTALLED.txt`
