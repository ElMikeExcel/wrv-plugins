# LIGHTNING-FIX: caster-credited Lightning (staged only)

Date: Sat Oct 24 2026 (CDT). Nothing was installed, uploaded or pushed. Only staged files changed.

## The problem
Magic's `lightning` spell called `strikeLightning()` with no caster. The vanilla bolt's damage
(LIGHTNING, causing=null) and the fire it started (FIRE_TICK / in_fire) show up in
ExcelBossTrial's `Attribution.dealtPlayer` as NOBODY. As a result, none of Lightning's damage
counted toward the 70% share.

## The change
Files: `staged/plugins/ExcelMagicGate/stages/stage{3,4,5}/spells.yml`, with the same edit in each.

- `lightning`, inside its Spread: `Lightning` gets `effect_only: true`, so it's a visual bolt only:
  no damage, no fire, no block change. After it comes
  `AreaOfEffect radius 3 / y_radius 3 -> Damage damage: 10`. Magic deals this as SPLASH_POTION
  with the player as shooter, so BossTrial credits the caster.
- `lightning|3` gets the same change inside its Repeat/Spread (`repeat: 2` unchanged).
- `require_build: true` is added at the spell's top level. This keeps the build-permission gate
  that the old real strike had (effect_only drops it).
- No other spell or line changed. A key-by-key compare against the .bak shows only `lightning`
  and `lightning|3` differ. `lightning|2` inherits the change and only changes radius 8 to 6, as
  before.
- `Magic/spells.yml`, stage1 and stage2 contain no lightning or smite, so they weren't edited.
  ExcelMagicGate copies stageN over plugins/Magic at seal, so Lightning only exists from stage3 on.
- Boon fix (`WRV boonfix v2`) is present in stages 1-5. Missile fix (4/5/5/6/6) is present in
  stages 3-5. Both are untouched.
- Backups: `bak-lightning-20261024/stage{3,4,5}-spells.yml.bak` and
  `STAGED-SHA256SUMS.txt.bak`.

## Before / after (rig, ExcelBossTrial 0.2.7 from iris-audit/build, sha 7b0392d5...)
"Credited" is the value of BossTrial 0.2.7's own `Attribution.dealtPlayer`, read by the probe for
each damage event.

| Spell / rank | Target | BEFORE damage per bolt | BEFORE credited | AFTER damage per bolt | AFTER credited |
|---|---|---|---|---|---|
| lightning (1), radius 8 spread | elder guardian | 5 or 10 (2x5 bolt hits) + 0-7 fire | NOBODY | 10 | caster |
| lightning\|2, radius 6 spread | elder guardian | 5 or 10 + fire | NOBODY | 10 | caster |
| lightning\|3, 2 bolts | elder guardian | 10 (2nd bolt eaten by hurt i-frames) | NOBODY | 10 (same i-frames) | caster |
| lightning, all ranks | ender dragon | 0 (vanilla dragon ignores lightning) | n/a | 3.5 (dragon body reduction of 10) | caster |
| smite / \|2 / \|3 (unchanged) | elder guardian | 2.07 / 2.76 / 3.45 Damage + ~3 fire ticks | Damage: caster; fire: NOBODY | identical | identical |
| smite (unchanged) | ender dragon | 0 | n/a | 0 | n/a |

Spread casts (the real-random radius) miss about as often before and after. Damage per landed
bolt is the same 10 the vanilla bolt usually did. Ranks are non-decreasing (10/10/10, rank 3 =
two bolts).

Block and fire, exact block diff of the test arena (bot world view, at 1 s and 13 s):
- BEFORE: each real-spread strike placed 1-3 fire blocks (undone by Magic's 8 s undo).
- AFTER: 0 block changes and 0 fire blocks at both times, every rank. No FIRE_TICK on targets.

## Check 1 (rig): PASS
- Rig `/workspace/ss-share/rig` (mine only), BossTrial 0.2.7 jar and its audited config.yml
  (sha 6a1dc981...).
- Every AFTER Lightning hit on the elder (19 hits: 15 aimed plus the 4 spread casts that landed) and on the dragon (12 hits) was credited to the
  caster. Every BEFORE Lightning hit was NOBODY (20 of 20 casts that landed). No AFTER Lightning event was credited to NOBODY.
- Logs in `/workspace/ss-share/tests/`: `lfix27-before.log`, `lfix27-before-dragon.log`,
  `lfix27-after.log`, `lfix27-after-dragon.log`, `blk-before.log`, `blk27-after.log`.
  Earlier 0.2.6 runs gave the same results: `lfix-*.log`, `blk-after.log`.
- The harness's coarse `blocks@1s failed` flags come from the elder, smite's ignite and the
  harness, and appear in BEFORE and in the no-cast control too. The exact diff
  (`blk*.log`) is the authority.

## Check 2 (separate pass): PASS
- Strict YAML with duplicate-key detection: all 6 staged spells.yml files parse with no duplicate
  keys.
- Minimal diff: only the added lines shown above (1 comment, `require_build`, 7 lines x 2 blocks).
- The lightning, lightning|2, lightning|3 and smite blocks are identical across stages 3/4/5.
- `magic reload` on the rig: stage2 81, stage3 142, stage4 157, stage5 203 spells, with no errors.
- ExcelAttunement 0.1.5 classifies by spell key from its built-in `spell-classes.txt`
  (lightning/smite/chainlightning = COMBAT), so its classification is unchanged.
- No names, gamertags, UUIDs, IPs or tokens in the diff.
- `STAGED-SHA256SUMS.txt`: stage3-5 lines updated, other lines unchanged. `sha256sum -c` is OK.
  - stage3 7d43579c269b781ecc0356ebf534914fede33e2f847937d48ee42fd66d659c64
  - stage4 e9e635d2a8bbdc11624b8ec69685932449d51924ab243463a5331bc01f333cc7
  - stage5 46cc3b14881035b86529ff2077103dbcad258ed03a59b3e5dc7cd3e0d6308f13

## Balance vs EliteMobs 5/5
Lightning is 10 per bolt (2 bolts at rank 3) on a 45 s cooldown for 80 mana. That's far below the
kit's sustained peaks (missile 5 at 10 per 0.8 s; powerstone 15 AoE), and the same 10 the vanilla
bolt already landed. EliteMobs treats MAGIC as a non-weapon cause, so elites take raw x
scaledDamageToElite (6.5 live, 5 at S5). Magic still doesn't beat what EliteMobs releases at 5/5.

## Flags for Mike (decide before shipping)
1. New dragon damage. Lightning now does 3.5 per bolt to the dragon, where it did 0 before
   (the vanilla dragon ignores lightning). It's credited and small, but it is a change. To keep 0,
   add `ignore_types: [ender_dragon]` to lightning's parameters.
2. Smite's Ignite is still unattributed, and I left it unchanged. Smite's real damage is already a
   credited Damage action. Its Ignite adds about 3 FIRE_TICK HP per cast credited to NOBODY. In
   the seal fights this is ~0: the dragon and wither are fire-immune and elders fight in water.
   Only the warden would take it. Crediting it would mean replacing Ignite with Damage, which also
   hits fire-immune and underwater targets. That's a balance change, so it wasn't done without a
   go-ahead.
3. Smite does 0 to the dragon. This was true before too. It's a targeting issue, not attribution.
4. AoE radius 3 hits every living entity within 3 blocks of the bolt except the caster
   (target_type LivingEntity means no self-hit). The vanilla bolt could hit the caster. Other
   players are still subject to `pvp_restricted: true`.
5. Other spells checked: anvil does no damage events; arrow is credited (player-shot arrow);
   chainlightning, missile, frost etc. already use Damage (credited). No other uncredited damage
   spell was found.
