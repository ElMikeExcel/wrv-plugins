# INSTALL PACKET #2 — Spell fix pass (Jester-reports: bowpull family, lightning, push)

Applies the fixes from the evidence investigation
`~/workspace/spell-audit/JESTER-REPORTS-20261102.md` under Mike's
Nov 2, 2026 directive "just get everything fixed." Full per-spell
record, bowpull census, math, and judgment calls:
`install-20261102-packet2/FIXES2.md`.

**This packet chains on packet #1** (the spells.yml logic-fix pass,
`install-20261102/`). Its staged files were built FROM packet #1's
outputs. Config-only pass: four staged spells.yml files, five
destinations (the s3 file serves BOTH the live file and the stage-3
template — the same layout as packet #1). No jar changes, no other
config changes.

## ORDERING (hard gate)

**This packet installs ONLY after the logic-fix packet (packet #1). If
the file at any destination does not hash to packet #1's output for
that stage at install time, STOP for that destination** — do not place
packet-2 files over Window #2 bytes; the packet-1 retune (wolf,
familiar, cat, missile/arrow mana, stormshackle) would be silently
reverted in the affected stage.

## What changes (summary — FIXES2.md is the record)

- **Bowpull family de-scaled (RC-1):** `arrow_ice`, `arrow_shock`,
  `arrow_regular` — every `bowpull` expression replaced by its
  full-draw (bowpull = 1.0) constant: speed 3, spread 0, arrow_damage
  1 / 1 / 2; ice freeze radius 4; shock repeat 15. Wand casts now fly
  and hit like a full-draw shot. Item costs (`arrow: 1`) unchanged —
  that is the family's design.
- **arrow_light mechanism conversion (judgment call, FIXES2.md):**
  the degenerate `Repeat(bowpull * 5)` damage wrapper becomes a single
  on-hit `Damage` at the spell's own `entity_damage: 5` /
  `player_damage: 3` (family shape); gravity `(1 - bowpull) * 0.5` → 0
  (the old value acted as effective gravity 50 through
  CustomProjectile's velocity multiplier — the bolt broke the ground at
  the caster's feet). Block-breaking utility untouched.
- **lightning / lightning|2:** `Spread` scatter `radius` 8 / 6 → **0**
  — the strike lands where targeted. Damage 10, AoE radius 3, mana 80,
  cooldowns unchanged; lightning|3 inherits the base radius (0).
- **push family:** `living_entity_speed` 0.5 / 0.6 / 0.7 / 0.75 →
  **1.2 / 1.45 / 1.7 / 2.0** (a real shove with the same rank spacing).
  push|4's alternate_down variant unchanged. Piglin brutes still
  resist via vanilla knockback resistance — expected.
- **NOT changed:** the magic arrow (`arrow`) — pass-through/splash is
  NEEDS-RIG (Attunement RC-2), owned by the parallel rig build;
  packet #1's values; `repairitem`/`repairequipment`; every
  `pvp_restricted` flag.
- **End-state proof at build:** `bowpull` grep = 0 in all four staged
  files; `disable_mana_regeneration` = 0; harness findings identical
  to packet #1's after-state (HIGH 6/7/8/8, 0 CRITICAL).

## Staged files

| staged file | sha256 | bytes |
|---|---|---|
| `install-20261102-packet2/stage-s2/spells.yml` | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` | 86,716 |
| `install-20261102-packet2/stage-s3/spells.yml` | `c49829004d786580107825b0959f1374c9938ca81157243fd560608cd0cf47e6` | 140,111 |
| `install-20261102-packet2/stage-s4/spells.yml` | `936af53b2c23ee726eceeced71e8ec4f89c38328c79592f9d826e9d08f698c77` | 178,352 |
| `install-20261102-packet2/stage-s5/spells.yml` | `bda9559f7c7ac991c8a887290f958af6f8dd00b3fd8d8df387e4c12b3cf13663` | 178,350 |

(The s2 file contains none of the in-scope spells and is byte-identical
to packet #1's s2 output — it is staged anyway so the five-destination
placement stays uniform.)

## Gates (per destination — any mismatch = STOP for that destination)

The current file at each destination MUST hash to **packet #1's
output** for that stage before it is replaced:

| destination | staged file | current file MUST hash |
|---|---|---|
| `plugins/Magic/spells.yml` (LIVE) | stage-s3 | `66986716c0bd69325d480f3613e11fead7f8d55459bb2116ac55d0db86631f31` |
| `plugins/ExcelMagicGate/stages/stage3/spells.yml` | stage-s3 | `66986716c0bd69325d480f3613e11fead7f8d55459bb2116ac55d0db86631f31` |
| `plugins/ExcelMagicGate/stages/stage2/spells.yml` | stage-s2 | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` |
| `plugins/ExcelMagicGate/stages/stage4/spells.yml` | stage-s4 | `a019fa6c447b99d2ecf991a55ab06f872150f3aecda364fc87c79ddacad53daa` |
| `plugins/ExcelMagicGate/stages/stage5/spells.yml` | stage-s5 | `6b317053ee78f9365ba055af3d314c4621ff4168a961c6b57dea601f32d5842b` |

## Steps

1. Confirm packet #1 is INSTALLED (its MANIFEST entry flipped and its
   marker written). Then, for each of the five destinations: verify the
   gate hash above, back up the current file in place as
   `spells.yml.bak-20261102-prefix2`, place the staged file, verify the
   placed hash against the staged table.
2. **Restart the server** (panel Shut Down → Start; a full restart, not
   a `/magic reload`, so the boot-verify below runs against the exact
   lines of record). Do not leave destinations split across windows.
3. Boot-verify from the fresh log:
   - Done line present; TPS 20.0.
   - Magic loads with **"Loaded 167 spells"** and no spell-load errors;
     SEVERE 0 attributable (the lone pre-existing Essentials
     unsupported-version ERROR is not attributable).
   - GuildGate **LOCKED with seal count UNCHANGED (3 of 5)** from the
     pre-window read.
   - Together's awake line still shows the 0.5.2 repair registration +
     confirm gate (proves the repair spells survived the swap in-file).
4. Write the marker `install-20261102-packet2/INSTALLED.txt` with the
   window record (placed hashes, backup names, boot lines) and flip
   this entry in the MANIFEST to INSTALLED.

## Rollback

Restore each destination's `spells.yml.bak-20261102-prefix2` and
restart. All values return to the **packet-#1 state** (packet #1's
retune stays in place; only this packet's changes revert). Nothing
else was changed, so nothing else needs rolling back.

## Player proofs owed (the acceptance that counts)

**Jester (all from a wand, Java):**
1. **Each arrow spell flies and hits:** ice, light, shock, and regular
   arrows leave the wand at speed, fly straight, and register damage on
   a target mob (light arrow: one clean hit, no ground breaking at his
   feet; ice: freeze sphere at visible size on hit; shock: zap repeats
   on the target).
2. **Lightning lands on target:** cast at a mob at range — the strike
   and its damage land on that mob, not scattered blocks away.
3. **Push shoves a zombie:** the cone visibly knocks a zombie back at
   each rank he holds. (A piglin brute resisting more is expected —
   vanilla knockback resistance — not a failure.)

## Same-pass surfaces

No new spells, commands, or menus — nothing to add. If the website or
Guide spell text describes the arrow family's behaviour (bow-draw
scaling, lightning scatter), those descriptions are stale after this
lands; that refresh belongs to the already owed website-edit pass, not
to this packet's placements. The investigation's RC-3 note applies:
the spell book surfacing the family's item costs would prevent these
reports regenerating — also a website-edit-pass item.
