# ExcelAttunement 0.1.8 — a spell arrow is not a bow shot

Built Nov 1, 2026 (CST). Base: the LIVE 0.1.7 jar
(sha256 `4b5d8837…`, installed Oct 26, 2026) — its source tree was
recovered from the wrv-plugins record PR #79 and proven to rebuild the
live jar class-for-class before this fix was built on it (SOURCE.md).
Candidate jar sha256 `971292ed…` (70,796 B; two builds byte-identical).

## The bug (live since Attunement shipped, Oct 20, 2026; first bit Nov 1, 2026)

- Fighter/Martial stance is set by landing a hit with a martial weapon.
  For projectiles, Attunement records each launch and classifies it:
  what weapon does this arrow "belong" to?
- The classifier answered "a bow" for **every** arrow a player launched,
  whatever was actually in the shooter's hand — bow, crossbow, or
  anything else.
- Magic's **Arrow** spell fires a real arrow with the caster as its
  shooter. Wand in hand, the hit therefore landed in the books as a bow
  hit: MARTIAL stance, wand stilled for 30 s — and because taking and
  dealing damage in the ensuing fight refreshes the stance, it felt
  permanent for as long as the fight lasted.
- It stayed hidden while the Arrow spell itself was broken (spread/speed
  made it miss essentially always, and it had no splash). The Nov 1,
  2026 Arrow fix gave the spell a working direct hit and a radius-2
  splash on the same night; Jester's first solid hit on a spider locked
  him into fighter stance, and he reported it within the hour.

## The fix (Fix A, one function)

`Weapons.kindOfProjectile`: an arrow is judged by the hand that loosed it.

- Bow in hand → BOW (unchanged). Crossbow in hand → CROSSBOW (unchanged).
- Anything else in hand — wand, stick, sword, empty hand — → **NONE**:
  the launch is never recorded, so the hit can neither set martial nor be
  zeroed as a "weapon hit" under caster stance. A real arrow in real
  play is always loosed from a bow or crossbow in hand; an arrow from
  any other hand was conjured, and on this server that means a spell.
- Thrown tridents are unchanged (a TRIDENT entity only exists as a
  thrown trident). Snowballs, eggs, potions, pearls were already NONE
  and stay NONE.

Nothing else changes: melee classification, the stance state machine,
the 0.1.7 scroll hop, the item locks, the guild ward, the arena locks,
the spell table (bundled and the live data-folder override) and the
config are all byte-identical to 0.1.7 — proven by an entry-by-entry
jar diff: only `Weapons.class`, the `plugin.yml` version line, and the
jar manifest's build-JDK stamp differ.

## Tests

- Unit: 982 checks pass (970 on the unmodified 0.1.7 tree in the same
  environment + 12 new Fix A pins: wand/stick/empty/blaze-rod/sword
  hands all read NONE, bow/crossbow pins unchanged, and the end-to-end
  "spell arrow is never recorded, lands full, sets nothing" mirror).
  Two pre-existing SpellClassifierTest failures against this box's older
  stage-grant corpus fail identically on the base tree; unrelated to
  this change, recorded in TEST-REPORT.md.
- Rig (Paper 26.3-235 + Magic 11.2.5 + the live spell files + the live
  106-row table override, fake player + probe listeners, SEVERE 0):
  spell Arrow direct hit → damage lands, stance = CASTER (the cast's
  own), never MARTIAL; spell Arrow ground splash → splash lands, no
  martial; real bow → MARTIAL; real crossbow → MARTIAL; sword hit →
  MARTIAL. The rig also answered the investigation's two open questions:
  Paper DOES fire ProjectileLaunchEvent for Magic-spawned arrows (hand
  reads as the wand), and the cast's attunement lands after the
  projectile's damage — the ordering that made the bug possible.
- The splash-with-a-sword-in-hand shape (the investigation's residual
  "Fix B" candidate) was probed and did NOT reproduce: the splash damage
  rides a potion-proxy projectile, never a player-attributed event, so
  there is nothing for the hand-read to misjudge. Fix B is not built;
  the evidence and its limits are in TEST-REPORT.md.

## Still to check in game (player proof, in the install packet)

- Jester (Java): cast Arrow, hit a spider (or any mob) — damage lands
  and NO fighter stance; then a real bow shot still sets fighter stance.
- Any player: scroll-hop and the stance locks behave exactly as 0.1.7
  (untouched code, but it rides the same jar).
