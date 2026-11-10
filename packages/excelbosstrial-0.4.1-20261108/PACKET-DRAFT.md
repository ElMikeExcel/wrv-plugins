# PACKET DRAFT — ㉚.9 post-fight stats surface: ExcelBossTrial 0.4.1

**Status: DRAFT for the supervisor to file — SEQUENCED STRICTLY
AFTER THE ㉚ PACKET.** Build record:
`~/workspace/bosstrial-fix-20261108/build-stats/BUILD-RECORD.md`.
Design of record: `~/workspace/bosstrial-fix-20261108/DESIGN.md`
§㉚.9. One jar swap, one boot, gated (BACKUP-FIRST + cap-at-6 +
verified-empty gate + panel marker discipline, per standing rules).

## ⛔ THE GATE — read first

This build was made **on the ㉚ code phase's staged 0.4.0 tree**,
not on live 0.3.7. It may only be filed once the ㉚ packet
(`build/PACKET-DRAFT.md`: BossTrial 0.4.0 + Protect 0.1.8) has
**installed and boot-verified** on the live server. Installing
0.4.1 over live 0.3.7 — or over any 0.4.0 other than the staged
candidate below — is a STOP.

| Gate | Must be |
|---|---|
| Staged base of record (this build's parent) | `build/ExcelBossTrial-0.4.0.jar` = **401,269 B / SHA-256 `043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`** (the REBUILT 0.4.0 — clamp raise + taunts + loud sanitize, Nov 8, 2026; this 0.4.1 was rebased onto it the same evening) |
| Live jar at THIS packet's install window | `plugins/ExcelBossTrial-0.2.9-team-r2.jar` (live filename) whose content hashes to that same `043c1558…` — i.e. the ㉚ packet's REBUILT candidate, placed and boot-verified (the jar's dates are pinned at build, so a faithfully placed 0.4.0 hashes identically; a mismatch means the live server is NOT running the staged 0.4.0 — STOP and reconcile). The original `0638c158…` 0.4.0 is SUPERSEDED — a live jar hashing to it does NOT satisfy this gate. |
| Live config at this window | the post-㉚ merged config (the ㉚ packet's merge already landed); this packet adds only the two keys below |

### Rebase rule (if the 0.4.0 staged jar changes)

If `build/ExcelBossTrial-0.4.0.jar` is ever rebuilt (any hash
other than `043c1558…`), **this 0.4.1 candidate is void** — do not
install it, do not re-hash-and-ship it. Rebase instead: copy the
new 0.4.0 tree to a fresh staging dir, re-apply exactly the ㉚.9
delta enumerated in BUILD-RECORD.md ("The delta"), re-run the full
suite (0 failures against the new baseline), re-package, and
re-file this packet with the new gate hash. The delta is
deliberately small and file-scoped so the rebase is mechanical.
**This rule already fired once:** the Nov 8, 2026 evening rebuild
of 0.4.0 (`0638c158…` → `043c1558…`) voided the first 0.4.1
candidate (`8eb5a004…`); the candidate below is the rebased one
(BUILD-RECORD, REBASE section).

## Candidate

| Candidate | Place as | Size / SHA-256 |
|---|---|---|
| `build-stats/ExcelBossTrial-0.4.1.jar` | `plugins/ExcelBossTrial-0.2.9-team-r2.jar` (same-name swap; the 0.4.0 jar aside to `.bak-<date>-pre041`) | **411,565 B / `f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900da20f1`** (rebased onto the rebuilt 0.4.0; the first candidate `8eb5a004…` / 404,260 B is VOID) |

## Config merge (merge, never replace)

Add under `fight-tracking:` (both keys are code-defaulted `true`;
adding them is for visibility — a config without them behaves
identically):

- `stats-popup: true`
- `stats-command: true`

Nothing else changes. No state migration: 0.4.1 reads the same
`fights/` files 0.4.0 writes and writes nothing new.

## Boot-verify lines

- `Enabling ExcelBossTrial v0.4.1`.
- The rebuilt-0.4.0 awake-lines still read true after the swap:
  sprinkle summary `wither ARMED (cadence 120s, cap 140)`, taunt line
  `Boss taunts: The Wither ARMED (10 lines, 25s cooldown)`, and NO
  `sprinkle config […] — clamped to …` WARNING (every live value is
  in range post-㉚-merge; a warning means the merge or the swap did
  not land as filed — stop and reconcile).
- GuildGate LOCKED count UNCHANGED from the pre-install read
  (never hardcode), plugin count unchanged, SEVERE 0
  attributable, TPS nominal.

## Post-install verification (in order)

1. **Chapter acceptance note:** a BossTrial jar swap re-opens
   seal chapter acceptance under the standing rule. This build
   changes no fight math (display + record-reading only — the
   suite's fight harnesses are untouched and green), but the
   governance rule is the rule: the supervisor schedules the
   re-acceptance read alongside the ㉚ packet's own re-acceptance
   plan rather than treating 0.4.1 as exempt. **THE WARDEN RULE
   is unaffected and still stands.**
2. **Java pop-up proof:** a scripted close (any chapter) with a
   Java participant online — at the close they receive the
   5-line pop-up; their dealt/share/healing/taken match the
   `fights/` record the close wrote, line for line.
3. **Command proof:** that participant runs `/bosstrial stats` —
   the table matches the same record; a second player running it
   sees THEIR most recent record, not the first player's.
4. **Bedrock proof (the owed live proof from the build):** a
   Bedrock participant at a close receives the pop-up as a
   Floodgate form; their `/bosstrial stats` returns the table as
   a form. If the form seam fails live, chat must carry both —
   record which happened.
5. **Opt-out proof:** a player with `/bosstrial feedback off`
   receives NO pop-up at a close, and `/bosstrial stats` still
   answers them.
6. **Verdict-less proof:** an admin `bosstrial reset` during an
   open encounter writes the ADMIN_RESET record AND the online
   participants receive its pop-up (the Nov 7 data-destruction
   shape, closed on both ends: the record survives, and the
   party sees its numbers).

## Rollback

Restore the `.bak-<date>-pre041` jar + restart; remove the two
config keys or leave them (0.4.0 ignores unknown keys). No data
was migrated; `fights/` records written under either version
read identically under both.

## Same-pass surfaces owed at install (supervisor's pass, not this build)

- Guide knowledge + intents: the post-fight pop-up exists; what
  `/bosstrial stats` shows; the numbers are pool damage (the
  bar's space); healing done counts named-source heals.
- What's New: registry entry (website + in-game ExcelWhatsNew) —
  player-facing: "See your boss fight numbers: after every seal
  fight you get your line, and /bosstrial stats replays the
  whole table."
- Website: BossTrial version pin 0.4.0 → 0.4.1 in both
  index.html and ai-knowledge.json.
- Menus: no menu change in this build; if the family asks for a
  menu door to stats, that's a separate ExcelMenu item.
- Records: SOURCE.md + record PR per the records workflow, with
  this build record riding the package.

## NOT in this packet

- Any fight-math, sprinkle, verdict, or rewards change (none
  exists in the delta — rendering and record-reading only).
- A live (in-progress) fight readout — stats answers closed
  records only, by design (§5 of the build record).
- Raid stats — no BossTrial raid fight record exists to read.
- ㉚.6 / ㉚.11 / the GuildGate PM-12 event patch — separate
  items, unchanged by this build.
