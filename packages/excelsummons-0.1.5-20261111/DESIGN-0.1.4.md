# ExcelSummons 0.1.4 — Wolf / summons fix (design summary)

An Excel plugin note for Wink Reyes Valley. November 10, 2026.
Design of record: `~/workspace/wolf-summon-diagnosis-20261110/
FIX-PACKET-DESIGN.md` §§2–4 (read with DIAGNOSIS-INTERIM.md).
Two supervisor-directed amendments landed during the build:
Mike's Nov 10, 2026, 10:34 AM refusal ruling (every refusal
explains itself, with numbers — extended to the Magic-side
summon messages, staged as `messages-addition-0.1.4.yml`) and
his 10:35 AM rank-scaled cap ruling (ladder LOCKED 10:37 AM:
Beginner 1 · Student 2 · Apprentice 3 · Master 5), superseding
the design's flat cap of 3.

## What changes vs 0.1.3

- **Companion mana reserve (§2).** The reserve exemption is
  removed at all seven sites the design names: any record whose
  family has a configured percent > 0 now holds reserve — wolf
  10, familiar 5, cat 5, mount 10, on top of the combat rows.
  One 75% ceiling and one 20% floor govern every branch; a
  refusal at PreCast spends nothing. Release stays re-derived
  from the registry after every mutation, on every despawn path;
  relog RE-APPLIES companion holds (combat summons release on
  logout, pets persist). `ReservePolicy.combatCounts` is renamed
  `reserveCounts` for the widened scope.
- **Dismiss pets (§2.4).** `/summons dismiss pets` and
  `/summons admin dismiss <player> pets` release CAST-origin
  companion + pack pets — the player-controlled release the new
  companion reserve requires. `SummonRecord` gains an `origin`
  field (CAST at cast capture, ADOPTED at sweep adoption; legacy
  rows restore as ADOPTED), and the pets scope selects CAST
  records only: a hand-tamed pet is never dismiss-deleted. Bare
  dismiss and `dismiss all` keep their exact 0.1.3 meanings.
- **Rank-scaled total cap (§3 as amended).** One counted total —
  COMBAT + COMPANION + PACK (the wolf's branch is PACK; the
  mount is never counted, its per-spell cap of 1 still binds) —
  capped by the caster's EARNED rank: `summon-cap-by-rank` in
  config (Beginner 1 · Student 2 · Apprentice 3 · Master 5,
  locked values; a rung change is a config edit, never a
  rebuild). The rank is read from MagicGate's earned.yml
  credited total through the locked thresholds (RankCap mirrors
  ExcelMagicGate's EarnedRank: 100 / 400 / 2,000); an unreadable
  rank fails to the LOWEST rung. The total cap is not gated on
  the wolf's enforce-cap: false. Over-cap sets at login are
  GRANDFATHERED — never trimmed; new counted casts refuse until
  the total falls below the player's cap. COMBAT_CAP and the
  per-caster-combat-cap key are retired (subsumed).
- **Refusal messaging (§4 + the 10:34 ruling).** Every
  bridge-owned refusal names its reason with live numbers: the
  total cap names the rank, its limit, the count, and what is
  currently summoned (with the adopted-pets variant when nothing
  counted can be dismissed); spell cap, server ceiling, reserve
  ceiling, and reserve floor likewise. The Magic-side summon
  failures are covered by per-spell message overrides staged in
  `messages-addition-0.1.4.yml` (no_target / living target,
  insufficient mana with $cost, cooldown with $time, and the
  generic permission bucket rewritten per spell to name its own
  limit). Known remainders are named in the build record.
- **Modifier templates.** +11 tiered reserve templates
  (wolf…wolf5, familiar…familiar3, cat, cat2, mount) staged in
  `modifiers-addition-0.1.4.yml`; wolf4/wolf5 exist so
  grandfathered 4–5-wolf sets are held exactly (§3.3).
- **§3.4 reconciliation: NOT wired.** Records carry a home
  world but no position, and Bukkit.getEntity == null is also
  the unloaded-chunk answer — no test available to this packet
  proves "chunk loaded, entity absent" for a persistent pet.
  Per the design's own gate, `SummonRegistry.sweepMissing`
  stays uncalled rather than risk unregistering live pets; the
  stale-record behavior (cosmetic in /summons list) is kept.
  Full reasoning in the build record.

## What does not change

No spells.yml values (costs, cooldowns, caps annotations).
No Magic message KEYS are altered — the messages addition adds
per-spell overrides only. The §1 Magic-side suspects (WolfSpell
internal counting, target semantics, cooldown persistence) are
not "fixed" by this packet; §5.3's controlled-cast matrix on
the live-shaped rig / live server is what convicts them.
