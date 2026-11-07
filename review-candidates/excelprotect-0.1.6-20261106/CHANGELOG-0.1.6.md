# ExcelProtect 0.1.6 "Recovery" — changelog

The completion build for recovery: everything Protect recorded can
now be brought back — blocks, containers, entities, and player
inventories — and WorldEdit edits are recorded too. Closes the
rollback gap list from the Nov 6 capability audit
(`protect-rollback-proof/VERDICT.md`).

## Fixed

- **Absolute-time filters work again.** `from=` / `to=` on export and
  rollback were silently dead: the timestamp parser stripped the `T`
  and then required it, every value threw, and the exception was
  swallowed — an export or rollback with absolute bounds quietly did
  the wrong thing. The parser now accepts `2026-11-06T17:41:56`,
  `2026-11-06 17:41:56`, and bare dates.
- **Malformed filter values fail loudly.** A bad value on a known
  filter key (from=, to=, hours=, …) now aborts the command with a
  clear message to the admin and an error record in the admin stream,
  instead of a silent no-op.
- **Rollback undo now covers containers.** Before 0.1.6, undoing a
  rollback restored the blocks but left any container contents the
  rollback had changed exactly as the rollback left them. Undo files
  now capture the pre-rollback container contents (byte-exact) and
  undo restores them too. Old undo files still work.

## Added

- **`/excelprotect restore-entity [preview] [force] [filters]`** —
  bring back a recorded dead entity (armor stands, pets, named mobs,
  anything in the entities stream): same type, same spot, same name,
  same equipment, same owner for tamed animals. Once-only per death
  record; staff can force. Honest limit: health/AI state isn't in
  the records, so the restored entity is a fresh spawn (new UUID,
  full health, default AI).
- **`/excelprotect restore-inventory <player> [preview] [force]
  [filters]`** — restore a player's inventory and ender chest from
  a death snapshot, slot for slot (a true point-in-time REPLACE,
  complementing the deaths flow's give-back). Preview shows exactly
  how many stacks would land. Shares the deaths flow's once-only
  mark, so one death can never be materialized twice across both
  paths without staff force.
- **WorldEdit logging** (`log.blocks.worldedit`, default on) — when
  WorldEdit is installed, every block a WorldEdit session actually
  changes is recorded into the blocks stream with the session's
  actor, in the same shape as hand edits — so `//set`, `//replace`
  and friends are inspectable, exportable, and rollback-able like
  everything else. No-op sets record nothing. Fail-soft: no
  WorldEdit, no hook, no problem.
- Boot + status now report the WorldEdit hook state (`we=on/off`),
  and `rollback_op` records include `undo_containers`.

## Notes

- Rollback semantics are unchanged (window-revert per position);
  0.1.6 makes the time targets for that rollback actually work.
- plugin.yml authors are now **Iris / Mike**, per the house
  authorship ruling.
