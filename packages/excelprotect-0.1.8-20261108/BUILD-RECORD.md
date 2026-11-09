# ExcelProtect 0.1.8 — REBUILD RECORD (2026-11-08)

**Candidate:** `ExcelProtect-0.1.8.jar` — 268,894 B — sha256 `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a`
**Fingerprint (Layer B):** `EXC-PROTECT-0.1.8-WRV-20261108-01-a018d8` (gate PASS in build)
**Determinism:** built twice from the staged tree → identical sha256 both times.
**Resolves:** the MANIFEST STOP on the first staged 0.1.8 jar (`ba8b58c1…`, built from a 0.1.1-era tree, 46 classes, missing DealtWatch/DamageWatch/HealingWatch/EliteProbe + ~15 more). That jar must never install; this candidate replaces it in the BossTrial 0.4.0 + Protect 0.1.8 packet.
**Spec:** `~/workspace/protect-rollout-item13/SPELL-ATTRIBUTION-GAP-20261108.md` §S0–S3 (§S4 summons-heal records out of scope — summons phase 2).

## Base verification (§S0 gate)

- Base tree: `~/workspace/protect-elites-build/` copied verbatim into this folder; the elites tree and the old staged tree were **not modified**.
- Pristine copy, unmodified build script: **86/86 tests green** and the jar came out **byte-identical to the live gate** — 246,038 B / `d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124`. The base is proven to be the live 0.1.7 tree before any edit.

## What was merged (port code, not tree)

Onto the elites base, from the staged ㉚.10/㉚.5 tree:
- `HealAttribution.java` (pure matcher/split math) + one addition: `Claim` gains a cast `id`; new pure `bestDamageClaim(...)` and `isPotionProjectile(...)`.
- `CombatCore`: the ㉚.10 heal build (list claims with target sets, bidirectional matching, pending-heal buffer, splash capture at PotionSplashEvent, effective/overheal splits, ent-anchored fight joining) **plus** elites' 0.1.7 kill-parity elite note (kept — the old tree lacked it), **plus** §S1 damage labeling (below), **plus** `cast_id` on every credited heal (`c…` = claim id, `p…` = throw id), **plus** the §S2 `healing_spells` per-spell split on fight_summary rows.
- `peekClaim(UUID)` **restored** on the new claim model (the staged tree dropped it; DealtWatch + HealingWatch both require it). The 1 s consume model is retired — claims are a shared list, never consumed.
- `ItemEvents.onItemBreak` + `ItemFact.durabilityRemaining` (㉚.5 item_break capture, verbatim port); elites' Chains stamping untouched.
- Settings/config: `log.items.breaks` (default true), `combat.heal-claim-window-ms` (6500, floor 1000), `combat.heal-proximity-blocks` (50), `combat.magic-heal-spells` (default heal/superheal/cure/blessing/boon, base-folded).

§S1–S2 damage labeling (new in this rebuild):
- Magic's DamageAction delivers through a cached fake ThrownPotion (shooter = caster). Labeling is **claim-first**: `CombatCore.labelDamage` consults the claim list — a claim whose target set contains the victim labels the hit `spell:<key>`, `delivery="spell"`, and the potion costume is dropped from the record. A thrown-potion hit with **no** matching claim is a real potion (`delivery="potion"`). Plain projectiles keep `delivery="projectile"`. **Melee causes (ENTITY_ATTACK / SWEEP_ATTACK) are never spell-labelled** (`delivery="hand"`) — not even on the claimed victim.
- Because CastEvent fires *after* the damage, a spell-shaped hit (MAGIC cause or potion costume) with no claim yet is **buffered up to 2.5 s** and resolved when the claim lands (swept by onCast, the 1 s watch timer, and combat tick); the dealt incident layer additionally re-checks claims at incident close (`lateDamageClaim`, exact-target only) and relabels.
- Damage claim matching uses the heal claim window (6500 ms) with target-matching as the guard — `HealAttribution.bestDamageClaim`: caster must match, heal spells never label damage, exact-target beats targetless, targetless labels only with no projectile-item evidence.
- Every damage record (combat stream + dealt incidents) now carries an explicit `delivery` field.

§S3 report: new pure `HealingSpellReport`; `/excelprotect healing <player> [hours]` keeps its 0.1.3 incident section and gains a **by-spell section** over the persisted `combat_heal` records — per spell/potion → per target: casts (grouped by `cast_id`; gap-grouping fallback for pre-0.1.8 records), raw, effective, overheal; unattributed residue renders as its own line.

## Test suite: 86 → 97, all green

- Baseline (live tree): **86/86**. Final: **97/97** (deterministic build runs the suite every build).
- Ported from the staged tree (7): healSplitCapsAtMissingHealth, instantHealthAmounts, spellKeyFoldingAndHealSet, claimMatchingIsBidirectionalAndTargetSpecific, claimMatchingTargetlessNeedsProximity, healAttributionSettingsDefaults, itemBreakRecordShape.
- New in the rebuild (4): damageClaimMatchingBeatsThePotionCostume, dealtDeliveryFieldAndLateRelabel, healingSpellReportAggregatesPerCast, damageLabelShapeRules.
- **One baseline test adapted, named:** `peekClaimDoesNotConsume` reflected into 0.1.7's private `Map<UUID, CombatCore$Claim>` and asserted consume-after-peek. The claim model it pinned was retired by design (§S0); the test now seeds a `HealAttribution.Claim` into the list and asserts the surviving contract — peek returns the freshest in-window claim, twice, and removes nothing; a stale claim does not peek. Intent preserved, mechanics updated.

## Class-inventory gate

Live 0.1.7 jar: **91 classes**. Candidate: **101 classes**. Diff:

- **Present:** every live class except one (below) — all top-level classes (DealtWatch, DamageWatch, HealingWatch, EliteProbe, Chains, ItemChainReport, LiveInventory, LocationWatch, PlaytimeReport, Timelines, RgOrigin, ImportedLfState, …) verified by name.
- **The one exception, named plainly:** `com/winkreyes/excelprotect/CombatCore$Claim` — the *private nested record of 0.1.7's retired claim model* (a `(String spell, long ms)` pair in a per-caster map). It cannot survive the merge because the model it implements is the defect being fixed; its replacement is `HealAttribution$Claim` (caster, casterName, target, spell, ms, world, x, z, id). No code outside CombatCore could reference it (private). Everything else is a strict superset.
- **Added (11):** HealAttribution, HealAttribution$Claim, HealAttribution$Split, HealingSpellReport, HealingSpellReport$Report, HealingSpellReport$Group, CombatCore$DamageLabel, CombatCore$PendingDamage, CombatCore$PendingHeal, CombatCore$SplashHit, CombatCore$SplashRegen.

## Desk-rig proofs (scratch Paper 26.2 + Magic 11.2.5, fake players; evidence in `rig-evidence/`)

Rig: clone of the ㉚.10 stage at `rig/stage/` (port 25601; the original stage's server still occupies 25599 and was not touched). Test spells' mana/cooldown zeroed **in the rig copy only** (the proof exercises Protect attribution, not Magic's economy); a rig-adapted `flashheal` block was appended (live block minus its `wrv_focus` modifier, which the rig spell file doesn't define; Heal action + amount 5 + range 16 unchanged). Driver: `rig/driver/src/hrig/SpellRigDriver.java`.

Three runs; runs 1–2 were diagnostic and changed the build/scenario, run 3 is the verdict run:
- Run 1 caught a real defect: current Paper names the thrown-potion entity **`minecraft:splash_potion`**, not `minecraft:thrown_potion` — real splash potions were reading `delivery="projectile"`. Fixed via `HealAttribution.isPotionProjectile` (accepts both; Magic's fake potion and older records use the other name) + unit coverage.
- Run 1 also taught: splash **harming heals the undead** (no damage event) — (c) uses a splash of healing against a zombie instead (same ThrownPotion shape, damages undead). And the flat rig world breeds slimes by the hundred (141 purged before one cast) — they ate the chains in runs 1–2; run 3 purges strays before each cast and stages witnesses 2.5 blocks out.
- Run 2 proved the flash→super spacing rule: an exact-target claim inside the 6500 ms window outranks a fresher *targetless* claim for a regain (the commissioned matching model, test-pinned). Run 3 spaces the two casts past the window.

**Run-3 verdicts, read back from the persisted streams (`rig-evidence/combat-2026-11-08.jsonl`, `dealt-2026-11-08.jsonl`):**

- **(a) PASS** — one Storm Shackle cast at zombie RigShackle: dealt incident `victim RigShackle, cause MAGIC, delivery "spell", spell "stormshackle", 2 hits, total 1.0` (+ a later stormshackle incident, total 6); combat records carry `spell stormshackle, delivery spell` with amounts. No projectile field — the costume is off.
- **(b) PASS** — one Chain Lightning cast: RigChain1/2/3 incidents all `delivery "spell", spell "chainlightning"` (3 hits each, totals 6/4/4). Cross-hits from the shackle's own chains on the cluster are labelled stormshackle — each hit wears the spell that actually dealt it.
- **(c) PASS** — a real thrown splash potion (Healing II) at zombie RigMelee: `delivery "potion", projectile "minecraft:splash_potion", spell absent, total 12` — reads as a potion, NOT a spell, with live spell claims open nearby. Bystander zombies splashed by real potions likewise read potion.
- **(d) PASS** — with a fresh stormshackle claim open on RigShackle, melee hits on RigShackle itself (3.33) and on never-targeted RigMelee (3.94) both read `delivery "hand"`, no spell.
- **(e) PASS** — console readback `excelprotect healing RigCaster 24` rendered from the persisted records:
  - `heal → RigTarget: 1 cast, 10 healed (10 effective, 0 overheal)`
  - `flashheal → RigTarget: 1 cast, 5 healed (5 effective, 0 overheal)`
  - `superheal → RigTarget: 1 cast, 20 healed (17 effective, 3 overheal)` (target at 3 HP: split exact)
  - splash throws render as their own potion casts per throw.
- **Heal re-proofs PASS** — HoT: 10 tick records under ONE `cast_id` (c17), 1.0 effective each (deep-wound setup: every tick effective); splash Healing II credited **once** per affected player (target 7 effective; the caster's own splash 5, overheal-split) — no double count with the vanilla MAGIC regain. Fight summary carries the §S2 split: `healing_spells {flashheal: 5, heal: 10, superheal: 17}`, healing_done 42 = 10 + 3 + 7 (splashes) + 5 + 17 exactly.
- Boot lines on the rig: `combat: Magic CastEvent feed hooked (spell attribution on)`, `READY version=0.1.8`.

## Known properties (observed, not defects against the spec)

- A spell whose damage lands **after the claim window** (stormshackle's full zap sequence runs ~25 s) cannot be claim-labelled; late dealt ticks fall back to the potion-costume label. This is the claim-window horizon the spec sets (BossTrial's model has the same property); the primary hits — the ones a reader checks — land inside it.
- Within the window, an exact-target claim outranks a fresher targetless claim (see run 2). Rapid-fire aimed heal → area heal on the same target inside 6.5 s can fold the area heal's target regain into the aimed spell's cast. The matching model is the commissioned ㉚.10 one and is test-pinned; changing the preference is a design decision, not a rebuild fix.
- The 0.1.3 healing *incident* stream (HealingWatch) has its own peek-based pairing, unchanged by this rebuild; the §S3 combat-record report is the authoritative per-spell surface.
- `mage learn` is not a verb in Magic 11.2.5 (`/mage add` is) — the rig mage already knew the five test spells from the stage's earlier Magic work, so casts fired; noted so a future rig on a fresh stage uses `mage add`.

## Files staged here

- `ExcelProtect-0.1.8.jar` (candidate, above) + full merged source in `src/`, tests in `tests/`, `plugin.yml` (version 0.1.8), `config.yml` (merged), `local-build.sh` (0.1.8 fingerprint/date).
- `PACKET-DRAFT.md` — install packet draft.
- `rig/` (stage clone + driver source/build), `rig-evidence/` (run-3 streams + server log).
