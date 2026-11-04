# ExcelBossTrial 0.3.4 — CHANGELOG

Release date: built 2026-11-02 (pending install). Base: ExcelBossTrial 0.3.3
(shipped live jar, SHA-256
`6bdeefb3b92ea09f1d67615950f6bd4ba2a4337bda9bac2b43501d98a52d0cbe`, rebuilt
byte-identical from this release's starting tree before any change — see
`base-verify/BASE-PROOF.md`).

0.3.4 is an **emit-only delta**: it teaches BossTrial to publish its fight
tables, seal verdicts, and boss state changes to ExcelProtect 0.1.0. No seal,
fight, tuning, sprinkle, gate, reward, or cooldown behavior changes in any
way. Every value in every chapter is exactly as 0.3.3.

## What players and staff see

- Nothing changes in game. Fights, verdicts, spoils, cooldowns, fight
  records in `fights/`, and every chat/console line behave exactly as
  0.3.3.
- Behind the scenes, ExcelProtect can now answer boss questions from its
  own record: the last verdict per seal with the full per-player table
  (damage dealt/taken, healing done/received), when a trial opened, when
  a fight ended, and when an elder trio respawned (boss available again).

## Added — the ExcelProtect emitter adapter

- **Verdict publish (fight tables).** At every chapter fight verdict —
  CREDIT, REPEAT_CREDIT, or any DENY — BossTrial fires Protect's
  `FightTableEvent` with the same record it writes to `fights/`: chapter,
  verdict, credit (the killer's name), the pool, the duration, and one row
  per participant with exactly the contract keys `name`, `uuid`,
  `damage_dealt`, `damage_taken`, `healing_done`, `healing_received`
  (one-decimal values). Protect's receiving side turns that one event into
  a combat `fight_summary` line and an audit `seal_verdict` line.
  Elder progress points (per-elder kills inside a trio) are NOT verdicts,
  so they travel the general door instead as combat `fight_progress`
  records — a progress point can never pose as a seal verdict in the
  audit family.
- **State-change publishes (boss status).** Through Protect's general
  record door (`ExcelProtectAPI.publish`, stream `combat`), BossTrial
  publishes `boss_state` records, schema v1 (fields: `schema`, `source`
  = `bosstrial`, `chapter`, `boss`, `state`, plus per-state fields):
  - `fight_started` — a chapter encounter opens (`boss_uuid`, `pool`).
  - `fight_ended` — a verdict lands (`boss_uuid`, `verdict`, `record`
    = the `fights/` file name, `duration_s`, `champions`).
  - `respawn_scheduled` — elder kills deny on share/presence/cooldown or
    a trio flips (`monument`, `respawn_due_ms`, `elders`).
  - `respawned` — the maintenance loop respawns an elder at its monument
    (`monument`, fresh `boss_uuid`, `world`).
- **Config gates (all default ON).** New `protect-emit` section:
  `protect-emit.enabled` (master), `protect-emit.fight-tables`,
  `protect-emit.state-changes`. Turning the master off is a complete
  no-op: zero publish attempts, proven on the rig from the receiving
  side (Boot C in TEST-REPORT.md).
- **Boot line.** One new console line reports the adapter state:
  `ExcelProtect emit: ON (adapter bound; fight tables ON, state changes ON; verdicts and boss state publish to ExcelProtect)`,
  or the OFF variants (`not installed` / `by config` / bind failure), so
  a boot log always says whether publishing is live.

## House-style guarantees (fail-soft adapter)

- The connection is a replaceable adapter, not a weld: every Protect type
  is referenced from exactly one gateway class
  (`protect/ProtectEmitGateway.java`); the rest of BossTrial talks to a
  plain in-house interface whose default standing instance is a no-op.
- BossTrial runs **identically with Protect absent, disabled, or
  erroring**: publishes fire after the fight record is safely on disk,
  every call is guarded so a failure can never reach fight logic, and a
  failing adapter logs at most three quiet warnings per boot (no stack
  traces in normal operation, no repeated spam).
- `plugin.yml` gains `ExcelProtect` as a **softdepend only** — never a
  hard dependency — so load order resolves the shared event classes when
  Protect is present and nothing changes when it is not.
- Honest scope note: the fight record carries no per-player death count
  (BossTrial has never tracked one), so the published table has no deaths
  column rather than an invented one. Protect's own death ledger remains
  the deaths record.

## Verification

- Delta-build law honored: unmodified 0.3.3 sources rebuilt byte-identical
  to the shipped live jar before the first edit.
- Full test suite: 25,414 checks, 0 failures (25,250 inherited + 164 new
  across the suite, including 31 checks dedicated to the emitter:
  settings collapse rules, table shaping/rounding/zero-player edge, all
  four `boss_state` shapes, the `fight_progress` shape, gateway
  never-throws with no server present).
- Built twice from the same tree: byte-identical jars (348,104 B,
  SHA-256 `14d5d06c2c3cd1d151cdb82dd8d6e069a19f10c0fd95ec82dca141ead8b38ad0`).
- Jar-level change scope vs 0.3.3: only the host class family
  (recompiled), the four new adapter classes, `plugin.yml`, and
  `config.yml` differ; every other class is byte-identical.
- Rig on Paper 26.3-235, three boots (see TEST-REPORT.md):
  (A) Protect present — a wither verdict's table + seal verdict +
  fight_started/fight_ended + elder respawned all landed in Protect's
  JSONL stores; (B) Protect absent — the identical fight, number for
  number, with the not-installed boot line and zero Protect errors;
  (C) emit disabled by config — verdict and fight record normal, Protect
  received zero BossTrial lines.
