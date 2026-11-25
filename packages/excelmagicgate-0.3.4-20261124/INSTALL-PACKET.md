# PENDING - ExcelMagicGate 0.3.4 (wand-owner repair at join, re-based on 0.3.3)

Filed Nov 24, 2026 (D212). Parent seat installs; this lane did NOT install.
Lint: 8749 B / max line 79 / non-ASCII 54

## What this fixes

The repair half of the ExcelDowned x Magic wand-deletion fix
(D214), re-based onto the live 0.3.3 after the 0.2.1 packet
STOPPED at its gate in the Nov 24 hand window: live was already
running 0.3.3 (the rank system) under the 0.2.0 filename, and
0.2.1 would have downgraded it. Evidence:
~/workspace/wand-restore-20261123/RECORD.md (defect),
~/workspace/downed-wand-fix-20261123/RECORD.md (the 0.2.1 fix),
~/workspace/magicgate-rebase-20261124/RECORD.md (this re-base).
ExcelDowned 1.0.4 (prevention) is already installed; 0.3.4 heals
any wand already carrying the downed-marked owner — and one is
still on record: MikeExcel's mage save still holds a stored wand
copy with owner "✝ DOWNED MikeExcel" (see RECEIPTS.md).

## Changes (jar swap + one config insertion)

- New join-time repair (PlayerJoinEvent, EventPriority.LOWEST,
  synchronous): scans the joining player's inventory and ender
  chest for real Magic wands (the plugin's existing reflective
  getIfWand + real-wand checks — never getWand, which wand-ifies
  any item). For each wand whose owner string is exactly the
  downed-marked form of THE CARRIER'S OWN name ("✝ DOWNED <name>",
  color codes stripped before comparing), it re-stamps the wand
  through Magic's own takeOwnership(player) and verifies the
  owner afterwards. Every heal is logged; a wand that cannot be
  healed logs a WARN. Foreign wands (another player's name,
  marked or not) are never touched.
- The same repair also runs ahead of the existing delayed join
  reconcile and in the activation reconcile leg (defense in
  depth). No other 0.3.3 behavior changes: the rank system,
  credit hooks, grant verification, wand inventory sync, seal
  watch and stage applies are byte-identical code (class-diff in
  RECORD.md).
- Config: ONE new section inserted into the live config (below).
  Toggle `wand-owner-repair.enabled` (default true — the repair
  is active even if the insertion is skipped, but insert it so
  the toggle is visible to an operator).
- No data-file format change; earned.yml untouched.

## Candidate

- ~/workspace/magicgate-rebase-20261124/ExcelMagicGate-0.3.4.jar
  — 252,773 B — SHA-256
  624f6f00193dcdda36d92c5bcb1c510ddcc4e8993a706dfaf9ece89b016e1eae
- Build: EXC-MAGICGATE-0.3.4-WRV-20261124-01-wandfix
- Tests: 315/315 PASS (305 carried from the 0.3.3 suite incl.
  placement fixtures + 10 ported wand-owner matcher checks; one
  carried build-fingerprint pin rolled forward to 0.3.4).
  Double-build byte-identical.
- Source tree: ~/workspace/magicgate-rebase-20261124/build-0.3.4/
  ExcelMagicGate/ ; source zip:
  ~/workspace/magicgate-rebase-20261124/
  ExcelMagicGate-0.3.4-source.zip — SHA-256
  91b126d966067df97dc0d26dbceddec0ecb177dbf731d7a512490b748e3064f9

## Live-premise / live-jar gate (STOP on mismatch)

Receipts: ~/workspace/magicgate-rebase-20261124/RECEIPTS.md
(same-day Homebase pulls, Nov 24, 2026; re-pull fresh at the
gate — never cite the receipt as the gate itself).

- Live `plugins/ExcelMagicGate-0.2.0.jar` must equal 247,765 B /
  4894969672002da96ee3b1220efb7564b194ef3cf60f4aaacea8ce73cf7d066f
  (its plugin.yml reads version 0.3.3). If live runs anything
  else, STOP and re-base.
- Live `plugins/ExcelMagicGate/config.yml` must equal 2,926 B /
  6c46a7070d07de9c7c369e3a46f69e1573f1f16316b4f078950b66a9b7addf2e
  and must NOT already contain a `wand-owner-repair` section
  (if it does, skip the insertion step only).

## Filename recommendation (read before the swap)

Keep the standing convention: place the candidate UNDER THE
LIVE FILENAME `ExcelMagicGate-0.2.0.jar`. The filename/content
split is deliberate fleet history, not an accident: the Nov 9,
2026 install first placed a newer build "under the live
filename", and the 0.3.0 / 0.3.0-amended / 0.3.1 / 0.3.2 / 0.3.3
installs all repeated the step verbatim — exactly one MagicGate
jar ever exists in plugins/, rollback copies are suffix variants
of the same base name, and every gate is hash-based, so the
filename carries no version semantics. Do NOT rename at this
install: a rename buys nothing functionally (Bukkit reads
plugin.yml, not the filename) and breaks the uniform rollback
bookkeeping five records deep. If filename honesty is ever
wanted, it is a separate fleet-wide rename decision for Mike,
not a side effect of this repair. Full investigation in
RECORD.md.

## Config insertion (pure insertion into live config.yml)

Insert this block immediately AFTER the live `floor-reconcile:`
block (its four indented keys) and BEFORE the
"# The unlock announcement" comment. No existing line changes:

```yaml
# Wand-owner repair (0.3.4): ExcelDowned marks a downed player's
# display name ("✝ DOWNED <name>"), and Magic records a wand's
# owner from that display name - so a wand carried through a
# downed logout can come back stamped with the marked name and
# fail Magic's owner check at login, which deletes it silently.
# With this on, then at every join (before Magic validates
# anything) ExcelMagicGate re-stamps any of YOUR wands whose
# owner is exactly your downed-marked name back to you, using
# Magic's own ownership stamping. Wands owned by anyone else
# are never touched.
wand-owner-repair:
  enabled: true
```

## Swap steps (standard empty-server window, backup first)

1. Verify server empty (list = 0 twice); verify live jar hash =
   48949696... and config hash = 6c46a707... (STOP if not).
2. Snapshot/backup per standing window procedure.
3. Rename live `plugins/ExcelMagicGate-0.2.0.jar` to
   `plugins/ExcelMagicGate-0.2.0.jar.bak-20261124-pre034`
   (rollback copy, kept in place).
4. Copy the candidate in AS `plugins/ExcelMagicGate-0.2.0.jar`
   (the live filename — see the recommendation above).
5. Insert the config block above into
   `plugins/ExcelMagicGate/config.yml` (back up the config
   first, e.g. config.yml.bak-20261124-pre034).
6. Start server.

## Rollback

Remove the placed `ExcelMagicGate-0.2.0.jar`, rename
`ExcelMagicGate-0.2.0.jar.bak-20261124-pre034` back to
`ExcelMagicGate-0.2.0.jar`, restore the backed-up config.yml,
restart. The repair writes no data files of its own (a healed
wand is re-stamped by Magic itself, exactly as a normal
ownership stamp).

## Boot-verify lines to check

- `Enabling ExcelMagicGate v0.3.4`; the enabled line carries
  build `EXC-MAGICGATE-0.3.4-WRV-20261124-01-wandfix`.
- The `Earning: ON` line and `/magicgate status` answer as on
  0.3.3 (stage, ceiling, reconcile, credit counters unchanged).
- SEVERE count = 0.
- No player-facing behavior change — no What's New / website /
  Guide surface owed.

## Proof owed after install

- MikeExcel's next natural login is the live proof (his mage
  save still carries the contaminated stored wand copy): watch
  that join — either the log shows
  `[ExcelMagicGate] Wand-owner repair: healed a wand ...` for
  MikeExcel, or his wand loads clean with no deletion. After
  his next logout, re-pull
  `plugins/Magic/data/players/7401bc22-d42a-4492-a451-374bd12d9e2c.dat`
  and confirm the stored owner no longer reads
  "✝ DOWNED MikeExcel".
- No other contaminated wand is expected (data check in
  RECEIPTS.md: 11 mage saves + all playerdata searched, Mike's
  stored copy was the only hit).

## Proof on record (build lane)

- Provenance: the 0.3.3 baseline was proven three ways before
  any edit — the pristine 0.3.3 source tree rebuilds
  byte-identical to the shipped 0.3.3 candidate AND to a fresh
  pull of the live jar (all 48949696...). The 0.3.4 diff against
  that baseline is the repair and nothing else (source diff +
  class diff in RECORD.md; the only other byte-differing classes
  are two nested classes whose bytecode is identical — their
  line-number tables shifted with the insertion).
- Unit suite 315/315; deterministic double-build byte-identical.
- Mirror validation caveat (inherited from the 0.2.1 fix): the
  Homebase mirror's Magic is the WRV-patched build whose
  wand-load gate refuses command-minted wands on fresh mages,
  so the repair cannot be exercised end-to-end there.
  Substitute proof: the ported unit suite pins the contamination
  matcher to the incident string; the join-order trace (repair
  runs synchronously at EventPriority.LOWEST, before Magic's
  next-tick wand checks) and Magic 11.2.5 bytecode semantics
  (boundDisplayName defaults true; takeOwnership re-stamps
  owner + ownerId from the carrier) are recorded in
  ~/workspace/downed-wand-fix-20261123/RECORD.md and carry over
  unchanged — the ported code is byte-for-byte the 0.2.1 logic.
