# PACKET DRAFT — ㉚ the Wither-night fix train: ExcelBossTrial 0.4.0 + ExcelProtect 0.1.8

**Status: DRAFT for the supervisor to file.** Build record:
`~/workspace/bosstrial-fix-20261108/build/BUILD-RECORD.md`. Design of record:
`~/workspace/bosstrial-fix-20261108/DESIGN.md`. Two jar swaps, one boot,
BossTrial first (Protect is independent; order matters only for the boot
count). Both are gated installs (jar swaps) — BACKUP-FIRST + cap-at-6 +
verified-empty gate + panel marker discipline, per standing rules.

## Live gates (STOP if either does not match — never skip a version)

| Live file | Must be |
|---|---|
| `plugins/ExcelBossTrial-0.2.9-team-r2.jar` (live filename; content 0.3.7) | 375,780 B / SHA-256 `6ad5b5cb2a33705dc55980e9c3b6ee963235a38629311cc2491bfd8b1041efd1` |
| `plugins/ExcelProtect-0.1.7.jar` | 246,038 B / SHA-256 `d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124` |
| `plugins/ExcelBossTrial/config.yml` (live) | 21,245 B / SHA-256 `07f76c95…` (0.3.7 staging; carries `wither.sprinkle.enabled: true` — DO NOT replace the live config; merge the new keys below) |

## Candidates

| Candidate | Place as | Size / SHA-256 |
|---|---|---|
| `build/ExcelBossTrial-0.4.0.jar` | `plugins/ExcelBossTrial-0.2.9-team-r2.jar` (same-name swap; live jar aside to `.bak-<date>-pre040`) | **401,269 B / `043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`** (REBUILT Nov 8, 2026 — clamp raise + taunts + loud sanitize; the original `0638c158…` / 393,906 B candidate is SUPERSEDED, never install it) |
| `build/protect-src/ExcelProtect-0.1.8.jar` | `plugins/ExcelProtect-0.1.8.jar` (live 0.1.7 jar aside to `.bak-<date>-pre018`) | ⚠️ SUPERSEDED — the `ba8b58c1…` build instance named here was built on the WRONG BASE and is DISCARDED / never installable. The pair's Protect half of record is the REBUILT 0.1.8: 268,894 B / `73373b90…` (parent-verified) — the MANIFEST ㉚ entry carries the operative hash; verify against it at staging time. |

## Config merge (BossTrial — merge, never replace)

The live config keeps every WRV value (incl. `wither.sprinkle.enabled: true`,
Rylie's TrainingWheels keys, all chapter tuning). Add/refresh ONLY:

- `wither.sprinkle` — **AMENDED Nov 8, 2026, ~5:57 PM CST (Mike's ruling:
  the automatic waves must exceed what was hand-summoned in the Nov 7 Wither
  fight — 84 mobs individually tallied in the console record, ~90 per the
  post-mortem's rounding, 75 of which actually reached the party).** For the
  wither block ONLY, the merge writes the values below — they deliberately
  DIFFER from the candidate's bundled config.yml texts (the bundled defaults
  `max-alive: 5` / `max-per-fight: 24` / `cadence-seconds: 150` / waves
  ×1/×3/×2 are superseded for this chapter):
  - `enabled: true` — the live value SURVIVES the merge (hard rule, unchanged).
  - `cadence-seconds: 120` (was bundled default 150).
  - `max-alive: 16` (was 5; 16 is exactly the shipped jar's sanitizer ceiling).
  - `max-per-fight: 140` (was 24) — **MIKE'S RULED TARGET, NOW DELIVERABLE:**
    the rebuilt 0.4.0 jar (`043c1558…`, see BUILD-RECORD REBUILD section)
    raised the `SprinkleSettings` ceiling from 48 to exactly 140, so 140
    loads unsanitized-down and the boot line reads `cap 140`. (The earlier
    config-only amendment's 48 was the old jar's hard clamp; that gap is
    CLOSED by the rebuild, and the old "do not write 140" instruction is
    retired with it.) Rig-proven: a config value of 999 produced the boot
    WARNING `sprinkle config [wither] max-per-fight 999 above ceiling —
    clamped to 140` (sanitizing is LOUD now — any clamp that fires logs
    one), and the fight's wave counters ran `…/140 spawned this fight`.
  - `waves:` rotation (bundled YAML shape; per-wave counts sit inside the
    jar's per-wave clamp [1, 8] — the piglin wave's 8 is the ceiling exactly):
    ```yaml
    waves:
      - types: [GHAST]
        count: 3
        placement: aerial
      - types: [ZOMBIFIED_PIGLIN]
        count: 8
        placement: ring
      - types: [WITHER_SKELETON]
        count: 6
        placement: ring
    ```
    (was ghast aerial ×1 → zombified piglin ×3 ring → wither skeleton ×2 ring.)
  - ⚠️ **DUPLICATE-KEY CAUTION (rig finding, Nov 8, 2026):** before merging,
    check the live config for DUPLICATE top-level `wither:` keys. The rig's
    config carried two; SnakeYAML reads the LAST one — a merge written into
    the shadowed first block silently does nothing (this lane's first rig
    boot read the shadowed block's stale values). If the live config has
    duplicates, merge into the LAST `wither:` block (and prefer removing the
    shadowed duplicate in the same edit, backing up first).
- `wither` taunts — **NEW Nov 8, 2026 (Mike's request; rebuilt jar):** merge
  the bundled keys into the (operative) wither block: `taunt-cooldown-seconds:
  25` and the `taunts:` list (the ten settled lines, verbatim in the
  candidate's bundled config.yml). Merge-only: if the live config already
  carries edited lines, Mike's live text wins — do not overwrite. Other
  chapters get NO taunt keys (they ship silent by default).
- Other chapters' `sprinkle` blocks: `max-alive: 5`, `max-per-fight: 24`,
  `cadence-seconds: 150`, their single `waves:` entry (bundled config).
- `feedback.damage-total-format: "{last} hit · {total} pool damage total"`,
  add `feedback.exhausted-format: "{name} — FALLING…"`.
- `fight-tracking.unattributed-log-threshold: 10`.
- `messages.rewards-waiting: "&eYour boss rewards are waiting in &f/bossrewards&e."` (pure information wording).

Protect config: one new gate `log.items.breaks` (defaults ON in code — no
config edit required) plus optional `combat.heal-claim-window-ms` (6500),
`combat.heal-proximity-blocks` (50), `combat.magic-heal-spells` — all code-
defaulted; add only if Mike wants them visible.

## Boot-verify lines

- `Enabling ExcelBossTrial v0.4.0`; sprinkle boot summary reads
  `wither ARMED (cadence 120s, cap 140)` (other chapters off until armed).
  The boot line prints the jar's SANITIZED values (SprinkleService
  bootSummary) — if it reads anything else, the config merge did not land
  as amended; stop and reconcile before proceeding.
- Taunt awake-line reads `Boss taunts: The Wither ARMED (10 lines, 25s
  cooldown)` (other chapters `silent`). **Clamp WARNING check:** the
  rebuilt jar logs a `sprinkle config […] … — clamped to …` WARNING for
  every live value its sanitizer had to change. After this merge every
  value is in range, so NO such warning may appear — if one does (above
  all one naming `max-per-fight`), the merge landed in a shadowed block
  or a live value is out of range; stop and reconcile.
- `Enabling ExcelProtect v0.1.8` + its READY line with `version=0.1.8`.
- GuildGate LOCKED count UNCHANGED from the pre-install read (4 of 5 at
  filing time — never hardcode), 76 plugins, SEVERE 0 attributable,
  TPS nominal.

## Post-install verification (in order)

1. **Seal-governance re-acceptance:** a BossTrial jar swap re-opens chapter
   acceptance — re-run the acceptance fights per chapter before the seals
   are trusted again (standing rule). **THE WARDEN RULE still stands: no
   Warden attempt until this install + the readiness audit + earlier-
   chapter re-acceptance are all done.**
2. Wither re-acceptance fight is itself the live proof of PM-1/PM-2/PM-6:
   the boss heals (REGEN lines in log), the bar visibly rises on heals,
   milestone waves arrive guaranteed (ghast first), and if the party's
   net damage spends the pool the verdict line fires and the boss dies
   through the normal death path with CREDIT + a complete fight record
   (`healed-pool` / `net-damage` / `other-damage` present in fights/).
   **Taunt proof (rig-proven Nov 8, 2026 — expected live too):** when
   the Wither kills an add and heals (+5.0 WITHER heal in the log), it
   speaks a taunt line in chat to the participants (`BossTrial taunt
   [The Wither]: "…" (eat #N this fight)` in the log); the first eat
   of the fight always speaks, later eats at most once per 25 s. Also
   confirm PM-3 in passing: past 50% net the boss is in its descended/
   armored phase (rig-proven; the entity rides the net fraction).
3. Protect: a healer's spell heal on a partner mid-fight lands non-zero
   `healing_done` in the fight record (㉚.10's live close-out; the rig
   proof is recorded in the BUILD-RECORD).
4. grantclaim signature form: `bosstrial grantclaim <player> signature
   wither` files a Gravewhisper set into claims + the recipient gets the
   chat alert.
5. Mid-fight record: during any fight > 60 s, `fights/<seal>-open-*.yml`
   exists and refreshes; it disappears when the ending's record lands.

## Rollback

Restore the two `.bak` jars (and the pre-merge config backup) + restart.
No data migration: 0.4.0 writes only additive record fields and additive
config keys; state.yml format unchanged.

## Same-pass surfaces owed at install (supervisor's pass, not this build)

- Guide knowledge + intents: bar FALLING state, pool-damage naming,
  sprinkle cadence behavior, grantclaim signature form, rewards-waiting
  behavior change.
- What's New: registry entry (website + in-game ExcelWhatsNew) — player-
  facing: "Boss fights fixed: the bar is the boss" release note.
- Website: plugin version pins (BossTrial 0.4.0, Protect 0.1.8) in both
  index.html and ai-knowledge.json.
- Records: SOURCE.md + record PRs per the records workflow; Protect
  HEAL-ATTRIBUTION-FINDINGS.md rides its record package.
- Tips rotation: no tip text changes required (no tip names the old
  sprinkle odds — verify at filing).
- Menus: no menu changes (grantclaim is console-side).

## NOT in this packet

- GuildGate event patch (PM-12 second half) — spec in BUILD-RECORD; needs
  the 0.4.0 GuildGate source, which is not in the workspace.
- ㉚.6 DeathRecord L&F menu and the DeathRecord BREAK-cause half of ㉚.5
  (phase 2). ㉚.9 stats surface (gate now open — separate build). ㉚.11
  Warden readiness package (separate; Warden rule unchanged).
