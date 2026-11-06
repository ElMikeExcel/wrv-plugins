# ExcelProtect 0.1.3 — changelog

Released as a build Nov 5, 2026 (install gated; see INSTALL-PACKET.md).
Mike's commission, Nov 5, 2026: track damage to mobs (investigate
too-little-damage claims, see who's doing what, calculate damage
contributions) + track players' self-healing and healing of other players.

## Added

- **Damage DEALT incidents** — new `dealt` stream
  (`dealt-YYYY-MM-DD.jsonl`, type `damage_dealt_incident`). Consecutive
  hits by one player on the same non-player victim with the same cause +
  delivery (hand / projectile type / claimed spell) merge inside the
  existing 2000 ms incident window: hit count, total + max-single-hit
  FINAL damage, the victim's health arc, victim identity (type + stable
  per-victim key + custom name when carried). Victim death closes every
  attacker's incident on it; `died:true` marks the killer's.
- **Elite victims** — 0.1.2's reflective EliteMobs probe now also reads
  the VICTIM side: `elite:true` + `elite_level` on records against
  EliteMobs elites (probe-unavailable omits the claim, as 0.1.2).
- **HEALING incidents** — new `healing` stream
  (`healing-YYYY-MM-DD.jsonl`, type `healing_incident`). Self-healing
  (regen, food, own potions) vs player-to-player healing (Magic spell
  heals attributed through the CastEvent claim feed). Both sums recorded:
  event amounts (`total_healed`) and cap-adjusted effective healing
  (`total_effective`); target health arc included.
- **Console queries** — `/excelprotect dealt <player> [hours]` (totals,
  elite share, per-victim breakdown, latest incidents) and
  `/excelprotect healing <player> [hours]` (self / done-to-others /
  received split), self-logging like the 0.1.2 `damage` form.
- **Config toggles** — `log.combat.damage-dealt` and
  `log.combat.healing-incidents`, both default ON; shed order gains
  `dealt, healing` after `damage`. Live config is not replaced at
  install; absent keys default ON.

## Notes / boundaries

- Player-vs-player damage is NOT duplicated into `dealt` — 0.1.2's
  victim-side `damage` stream already holds it; `dealt` is non-player
  victims only, so contribution sums never double-count.
- Vanilla's `EntityRegainHealthEvent` names no potion thrower: a splash
  potion thrown by another player records as the target's self-healing.
  Spell heals are the attributable player-to-player path.
- HealingWatch/DealtWatch PEEK Magic cast claims (new
  `CombatCore.peekClaim`) and are registered before CombatCore, so
  per-event combat attribution consumes exactly as before — no behavior
  change to the 0.1.1/0.1.2 streams.

## Carried

- All 0.1.2 behavior unchanged. Unit suite: 37 tests (26 carried + 11
  new), all green; deterministic double-build byte-identical.
