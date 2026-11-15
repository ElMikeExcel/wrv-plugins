# ExcelAttunement 0.1.8 (Fix A) — TEST REPORT

Nov 1, 2026 (CST). Base: the proven 0.1.7 tree (see SOURCE.md). Candidate
jar SHA-256 `971292ede865b0a0ae6fd436ce90d48fa3a96e58cc73d26f8e152b2e4822262a`
(70,796 B; two builds byte-identical).

## Unit suites (plain JVM, `run-tests.sh`)

0.1.8 tree: **982 passed, 2 failed.** Control run, pristine recovered
0.1.7 tree in the same environment: **970 passed, 2 failed** — the same
two. The delta is exactly the 12 new Fix A checks, all passing; nothing
that passed on the base fails on 0.1.8.

The 2 failures (pre-existing, environmental, untouched by this change):
`SpellClassifierTest` — "grant universe across stages 0–5 is 105 spells"
and "table keys == grant universe". The test compares the jar-bundled
table against the MagicGate stage grant files found on this box, which
are the older 100-spell corpus; the check encodes the post-heal-kit
105-spell corpus. It fails identically on the unmodified 0.1.7 tree, and
spell classification is outside Fix A's scope (the classifier class is
byte-identical in both jars).

New pins in `RangedMartialTest` (Fix A, §8 of the investigation):
- `kindOfProjectile("ARROW","STICK")` → NONE; `("ARROW", null)` → NONE;
  `("SPECTRAL_ARROW","BLAZE_ROD")` → NONE; `("ARROW","AIR")` → NONE;
  `("ARROW","DIAMOND_SWORD")` → NONE (an arrow is not loosed from a sword).
- Unchanged behavior pinned again beside them: `("ARROW","BOW")` → BOW;
  `("SPECTRAL_ARROW","CROSSBOW")` → CROSSBOW.
- End-to-end mirror of the glue (`spellArrowSetsNothing`): a spell arrow
  is never recorded in the ProjectileTracker; its landing deals full
  damage and sets NO stance; under an active CASTER stance it is not
  zeroed either (the zeroing branch only ever sees tracked shots).
- All pre-existing pins pass unchanged: bow/crossbow/trident set martial,
  caster zeroing of real shots, judged-at-landing, missed shots, tracker
  grace/sweep, stance state machine, HotbarSkip (0.1.7), wards, arena locks.

## Scratch rig (Paper 26.3-235, Magic 11.2.5, Attunement debug ON)

Rig: fresh server cloned from the 0.3.3 sprinkle rig's Paper 26.3-235
install (port moved to 39177 — 39101 was held by another rig), plugins =
Magic 11.2.5 + ExcelAttunement 0.1.8 + RigDriver2 (fake ServerPlayer
"RigPlayer" + stationary 1000 HP zombie, the Oct 19 driver's pattern).
Magic's `spells.yml` in the rig is the live post-Arrow-fix file
(SHA-256 `70ecebe0…`, the window-staged stage-3/live file). The live
106-row spell table override sat in the data folder; the boot printed
"Using spell table override: plugins/ExcelAttunement/spell-classes.txt"
and "ExcelAttunement enabled: ON, window 30s, combat context 10s, spell
table 106 spells". **SEVERE 0 in both preserved run logs** (only the
sandbox's usual no-network version-fetch/authlib noise). Logs preserved:
`rig/rig-run1-latest.log`, `rig/rig-run3-latest.log`.

Driver probes log every ProjectileLaunchEvent (type, shooter, hand,
velocity) and every projectile hit + damage event on the zombie
(damager form, cause, damage/final), and phases read the stance through
the plugin's own `activeStance(Player)`.

### Results

- **(a) Spell Arrow DIRECT hit, wand in hand — PASS** (run 1).
  Probe sequence, same second, in log order:
  `PROBE launch: type=ARROW shooter=RigPlayer hand=STICK` → cast →
  `PROBE damage: damager=PROJ ARROW cause=PROJECTILE final=4.92` →
  `RigPlayer attuned: CASTER` → `RigPlayer cast arrow (COMBAT) -> SET`.
  Zombie damage 4.92, stance after = CASTER (the cast's own attunement),
  **not MARTIAL**. On 0.1.7 this exact sequence sets MARTIAL at the
  damage step (tracker record: kind BOW) and the later CastEvent is
  ignored — that is Jester's live report, reproduced in shape and now
  closed.
- **(b) Spell Arrow ground SPLASH beside the mob, wand in hand — PASS**
  (run 3, re-scored from the preserved log; see the detector note).
  9 casts aimed at the ground 1.5 blocks from the zombie: arrow struck
  stone at the aim point (`PROBE hit … hitBlock=STONE`), and the zombie
  took the radius splash every time — damage event
  `damager=PROJ SPLASH_POTION shooter=RigPlayer cause=MAGIC dmg=2.5 final=2.5`.
  Stance after these casts: CASTER only. **Zero "attuned: MARTIAL" lines
  in the whole run-3 log** (7 CASTER attunements, 15 splash events).
- **(c) Real BOW shot (control) — PASS** (run 1): arrow hit, 5.90 damage,
  `attuned: MARTIAL`, stance MARTIAL.
- **(d) Real CROSSBOW shot (control) — PASS** (run 1): arrow hit, 5.90
  damage, `attuned: MARTIAL`, stance MARTIAL.
- **(e) Melee sword hit (control) — PASS** (run 1): hit lands,
  `attuned: MARTIAL`, stance MARTIAL.
- **(f) The §6 launch-event question — ANSWERED, both halves:**
  1. *Does ProjectileLaunchEvent fire for a Magic-spawned arrow?* **Yes.**
     The probe caught it for every spell cast: type=ARROW, shooter = the
     caster, hand = STICK (the wand). Paper fires the event for Magic's
     NMS `shoot()` spawn path, so Attunement's tracker sees exactly the
     launches the investigation predicted.
  2. *Does the tracker now ignore it?* **Yes, by construction and by
     outcome:** with Fix A the launch classifies NONE, `onLaunch` records
     nothing, and the landing produced full damage + no martial across
     every spell-arrow phase, while real bow/crossbow launches (same
     event, same tracker) still set martial in the control phases.
  Bonus, the §3 ordering question is also settled by the (a) sequence:
  the projectile's damage event is processed **before** the cast's
  CastEvent attunement is applied (`PROBE damage` precedes
  `attuned: CASTER` / `cast arrow (COMBAT) -> SET` in the log) — the
  exact ordering the investigation derived by elimination.

### The Fix B probe (splash with a sword in the main hand) — NOT reproduced; mechanism evidence against the Arrow-splash variant

Run 3, 6 attempts: cast Arrow with the wand, swap a netherite sword into
the main hand 1 tick later, arrow ground-splashes beside the zombie.
**No martial set in any attempt** (stance stayed CASTER from the cast).
More informative than the negative: in all 15 ground-splash damage events
across the run (wand in hand and sword in hand alike), the event's
damager was **never the player** — it was a `SPLASH_POTION` projectile
entity with the caster as its shooter, cause MAGIC. Attunement's
`resolveAttack` therefore takes the projectile branch, looks the potion
up in the tracker, finds nothing (a potion is never a martial projectile,
before or after Fix A), and returns no attack — the direct-player branch,
the only place a sword-in-hand splash could attune, is never reached for
this spell's splash. Per the task instruction, **Fix B is NOT built**:
the one shape the investigation flagged as conditionally vulnerable
(Arrow-family ground splash with a martial weapon in the main hand at
splash time) does not produce a player-attributed damage event on the
live spell configuration. Caveat, stated plainly: this evidence covers
the Arrow family's Projectile→AreaOfEffect→Damage chain as configured
live. It does not prove anything about other Damage-action spells with
different action shapes (e.g. a direct-target Damage spell); if a false
martial is ever reported from one of those, that is the moment to scope
Fix B with a reproduction in hand.

### Detector note + a discarded reading (own mistake, recorded)

Two honesty items from the rig work itself:

1. My driver's A2/A3 "splash" detector required the damage event's
   damager to be the PLAYER (the investigation's §2 attribution claim).
   The real event arrives via the SPLASH_POTION proxy, so the driver
   scored both phases "no splash landed / FAIL" while the log shows the
   splashes landing every time. The phases above are re-scored from the
   preserved log lines themselves (hit block, damage event, stance), not
   from the driver's verdict strings. Run 1's A2 was additionally
   confounded: Bukkit `teleport()` silently no-ops for the
   connection-less fake player, so the "ground aim" casts actually flew
   from the boot post and direct-hit — discovered from the new aim/hit
   probes in run 3 (`aimAt … yaw=180.0 pitch=0.0` despite a computed
   side aim), fixed by placing the player at the NMS level. Run 2's log
   is preserved as the confounded run.
2. One backgrounded grep completion during this work quoted an "A2 PASS"
   with a player-attributed splash (final=2.416) that **does not exist
   in any preserved log** (verified by direct grep of all three run logs
   after the fact: no such lines). I discarded that reading and re-derived
   every rig claim in this report from the preserved log files directly.
   Nothing in this report rests on it.

## Not proven here (owed after install)

- A real player's Arrow cast on a real client (the rig's fake player
  casts through the same Magic API path a wand click uses, but the
  player proof is Jester's — in the install packet).
- Bedrock: stance behavior is server-side and platform-blind in this
  code; no Bedrock-specific check exists to run. Jester's retest is on
  Java, where he hit the bug.
