# INSTALL PACKET — ExcelEliteLadder 0.2.0 (ladder table → ladder.yml)

Filed Nov 5, 2026 by the 0.2.0 build lane. Mike's approval: 2.0 decision
sheet item 3, Nov 5, 2026. **GATED jar swap** (BACKUP-FIRST + cap-at-6 +
verified-empty / weekend protocol as standing). No config or data placement:
the data folder carries over untouched, and `ladder.yml` generates itself
from the bundled default on the first 0.2.0 boot.

## Artifact

- `~/workspace/eliteladder-config-build/ExcelEliteLadder-0.2.0.jar` —
  40,475 B, SHA-256 `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`
  (built twice byte-identical; full evidence in TEST-REPORT.md:
  231 plain-JVM checks 0 failed incl. the migration equality suite and the
  25-case malformed battery; three-boot Paper 26.3-235 rig, SEVERE 0).

## What it does

The mob ladder table moves from hardcoded Java into
`plugins/ExcelEliteLadder/ladder.yml` under strict validation (see
CHANGELOG-0.2.0.md). **The values are unchanged** — the shipped default
equals the 0.1.2 table value-for-value, so a fresh 0.2.0 boot behaves
identically to 0.1.2 (baseline adoption, up-only applies, empty-server
gate). Any malformed ladder.yml is refused whole with the defect named
(WARN, never SEVERE) and the last-good table keeps running; at first boot
the fallback is the built-in copy of the same table. New console surface:
`/eliteladder reload`; `/eliteladder status` shows the table source, the
last load outcome, and all five effective rows.

## Gates (STOP and report on any mismatch)

1. Exactly ONE active ExcelEliteLadder jar in `plugins/`, and it MUST hash
   `7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb`
   (31,403 B — the live 0.1.2 jar of record). Any other hash or a second
   jar = STOP.
2. `plugins/ExcelEliteLadder/state.yml` reads `appliedStep: 3` (the live
   ladder position, guild LOCKED 3 of 5). If it reads anything else, STOP
   and report — the ladder moved since filing.
3. No `ladder.yml` in `plugins/ExcelEliteLadder/` yet is EXPECTED (0.1.2
   never had one). If one is already there, STOP and report — someone
   placed it by hand and its contents must be reconciled first.

## Steps

1. BACKUP FIRST (Shockbyte, verified complete pre-swap) with cap-at-6
   pruning per the house rule; name prunees in the report.
2. Verified-empty gate per standing (or the weekend countdown protocol).
3. Rename the live jar aside:
   `ExcelEliteLadder-0.1.2.jar` → `ExcelEliteLadder-0.1.2.jar.held-<install date>-pre020`
   (never delete). Place the candidate as `plugins/ExcelEliteLadder-0.2.0.jar`.
   Exactly one active ExcelEliteLadder jar after the swap.
4. Do NOT touch `plugins/ExcelEliteLadder/` contents (config.yml, state.yml,
   backups/, apply-log.txt all carry over). Note: the live config.yml still
   carries a stale comment header claiming the table is built into the jar —
   comment-only, values identical; 0.2.0 does not overwrite an existing
   config.yml, so the stale comment stays until someone refreshes it by
   hand. Accepted and recorded here so nobody chases it later.
5. Restart (in-window).

## Boot-verify (all must hold)

- `Enabling ExcelEliteLadder v0.2.0` and the enable line reads
  `ExcelEliteLadder enabled: applied step S3, ladder table: ladder.yml (config) — default generated on first boot, watching ExcelGuildGate seals every 30s, …`
- `plugins/ExcelEliteLadder/ladder.yml` now EXISTS (generated on this boot).
- Plugin count unchanged from the pre-window read; guild LOCKED 3 of 5
  unchanged; TPS 20.0; SEVERE 0 attributable.
- Seal ledger: an ExcelEliteLadder swap does NOT re-open chapter
  acceptance (only BossTrial/GuildGate jar swaps and Paper changes do) —
  no ledger action.
- Console proofs (nobody online needed):
  - `eliteladder status` → `Ladder table: ladder.yml (config)`,
    `Table load: loaded from ladder.yml at boot…`, five rows with
    `S4: spawn 0.11, cap 150, toElite 0.4, toPlayer 2.35…`, applied S3,
    live file S3-norm.
  - `eliteladder reload` → accepted reply (the table re-reads cleanly).

## Same-pass surfaces

- **Player-facing change: NONE** (values and behavior identical) — no
  What's New entry on either surface, no broadcaster tip, no menu change.
- **Website (parent-owned publish):** the plugin-table row for
  ExcelEliteLadder goes 0.1.2 → 0.2.0, WITH the ai-knowledge.json regen
  (the standing two-artifact rule); the Guide's plugin-table knowledge
  rides the same regen chain on its next surfaces pass.
- **Records (pre-approved standing workflow):** 0.2.0 record package +
  Drive jar/source deposits + draft record PR in wrv-plugins after install.

## Player proofs owed

None — no player-visible behavior changes. The console proofs above are
the acceptance. (The next natural ladder event — a seal break or an
operator retune via ladder.yml + reload — exercises the new path in
production; the rig already proved both directions.)

## Rollback

Remove `ExcelEliteLadder-0.2.0.jar`, rename the held 0.1.2 jar back,
restart. `ladder.yml` left in the data folder is ignored by 0.1.2;
state.yml is format-identical across both versions.
