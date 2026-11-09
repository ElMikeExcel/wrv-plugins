# INSTALL PACKET (DRAFT) — ExcelProtect 0.1.8 "Attribution" — 2026-11-08

Rides the existing **ExcelBossTrial 0.4.0 + ExcelProtect 0.1.8** packet slot; the
BossTrial half is unchanged. This draft supplies the corrected Protect half.

## STOP resolution

The MANIFEST STOP on the first staged 0.1.8 jar (`ba8b58c1…` — 0.1.1-era base,
46 classes, missing DealtWatch/DamageWatch/HealingWatch/EliteProbe + ~15 more)
is resolved by replacement: **that jar is discarded; this candidate is the
0.1.8 that installs.** Do not lift the STOP for the old jar under any reading.

## Gate (verify before any swap)

- Live `ExcelProtect` jar on the server must hash to
  `d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124` (246,038 B, 0.1.7).
  Any other value = wrong base; stop and re-pull.
- Standard gates apply unchanged: BACKUP-FIRST, cap-at-6, verified-empty
  (or the weekend protocol), one panel actor, WINDOW-IN-PROGRESS marker rules.

## Swap

- Replace the live ExcelProtect jar with `ExcelProtect-0.1.8.jar`
  (268,894 B, sha256 `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a`,
  fingerprint `EXC-PROTECT-0.1.8-WRV-20261108-01-a018d8`).
- Config: ship the merged `config.yml` from this folder, OR merge just the
  four new keys into the live config (all default to the shipped values, so a
  jar-only swap is also safe — the code defaults match):
  `log.items.breaks: true`, `combat.heal-claim-window-ms: 6500`,
  `combat.heal-proximity-blocks: 50`,
  `combat.magic-heal-spells: [heal, superheal, cure, blessing, boon]`.
- Class inventory: candidate 101 classes ⊇ live 91, with ONE named exception —
  `CombatCore$Claim`, the private nested record of 0.1.7's retired claim model,
  replaced by `HealAttribution$Claim` (see BUILD-RECORD.md). Every player-facing
  class (DealtWatch, DamageWatch, HealingWatch, EliteProbe, …) is present.
- Records compatibility: 0.1.8 only ADDS fields (`delivery`, `cast_id`,
  `healing_spells`, `item_break` type). Existing streams stay readable by
  0.1.7, so rollback needs no data migration.

## Boot verify (all three lines, in the startup log)

1. `[ExcelProtect] combat: Magic CastEvent feed hooked (spell attribution on)`
2. `[ExcelProtect] READY version=0.1.8 …`
3. `/excelprotect status` → `version=0.1.8`

## Post-install player proofs (~2 minutes, any player with the wand)

1. Cast **Storm Shackle** (or Chain Lightning) at a mob → `/excelprotect dealt <player>`
   shows the incident as `[spell: stormshackle]` — not `(splash_potion)`.
2. Throw a **real splash potion** at a mob → the incident reads `(splash_potion)`,
   no spell label.
3. **Flash Heal** a teammate → `/excelprotect healing <player>` shows the
   by-spell line: `flashheal → <teammate>: 1 cast, 5 healed (… effective, … overheal)`.

## Rollback

Restore the 0.1.7 jar (hash `d7c852b5…`) and the pre-swap config; restart.
No data rollback needed (additive record fields only).
