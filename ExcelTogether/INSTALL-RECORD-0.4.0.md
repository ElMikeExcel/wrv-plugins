# ExcelTogether 0.4.0 — install record (STAGED, NOT INSTALLED)

Asked by Mike, Oct 23, 2026: "while in a party of more than 2 drops should be at least doubled" and
"if we are in a party we shouldn't be able to hit each other" (PvP stays on for everyone else).
Plus a tiny hp scoreboard refresh on join. Built Oct 23, 2026 by Steve from the 0.3.0 source
(PR #37 branch); that source rebuilds byte-identical to the live 0.3.0 jar (SHA-256 55a6b3ea...).

## Status at a glance
| Status | State |
|---|---|
| Built | Yes, Oct 23, 2026 (reproducible: two builds, same SHA) |
| Automated tests | 550/550 pass (all 0.3.0 suites unchanged + new PartyRulesTest 58 checks) |
| Scratch server | Paper 26.2 + ViaVersion bots, 4 players: all checks below passed, zero SEVERE |
| Installed live | NO. Install at an empty-server restart (jar swap only) |
| Java check | Scratch bots only (Mineflayer 1.21.11 via ViaVersion) |
| Bedrock check | UNCONFIRMED (chat line is plain ASCII; nothing Bedrock-specific is used) |

## Artifact
- ExcelTogether-0.4.0.jar, 113,739 bytes, SHA-256 c5d94bf52916bc42b3f1fc5fd30be148508e2cda3736c54a403672d39babf656

## What changed vs 0.3.0 (new files PartyRules.java, PartyCombatDrops.java; 6 lines wired in ExcelTogether.java)
1. Party friendly fire (config `friendly-fire.*`, default on): members of the same ExcelTogether party
   cannot hurt each other: melee, arrows/tridents/fireworks (shooter), TNT they lit, any damage whose
   Paper DamageSource names the player (Magic spells that credit the caster), flame-arrow fire,
   harmful splash/lingering potions, and each other's tamed pets (both directions). Healing potions
   still land. PvP vs non-members unchanged. mcMMO's own `Party.FriendlyFire: false` only covers
   players who are also in the same mcMMO party; this covers every ExcelTogether party.
2. Party drops (config `party-drops.*`): normal mob killed by a player (vanilla killer credit) while
   3+ members of that player's party (killer included) are alive, same world, within 50 blocks:
   plain loot x2. Never boosted: EliteMobs mobs (any `elitemobs:*` PDC key; ExcelPartyDrops owns
   elites), spawner mobs (fromMobSpawner or reason SPAWNER/TRIAL_SPAWNER), any spawn reason outside the
   allow list (breeding, eggs, commands, plugins/Magic CUSTOM, raids, portals, golem builds...),
   bosses (Wither, Dragon, Warden, Elder Guardian: BossTrial spoils), villagers/traders/golems, tamed
   pets, players (Death Record), items the mob carried (picked-up items, chests, saddles, armor),
   and any named/enchanted/unstackable item. Cap 150 boosted kills per player per rolling hour.
   Dropped XP, Jobs pay and mcMMO XP untouched. Gold chat line to the party in range, max once per
   10 min per player: "[Party] 3 of you are together: mob drops x2!". Console audit line per boost.
3. hp refresh (`hp-refresh-objective: hp`): 2 and 40 ticks after join, set the player's score on the
   main-scoreboard objective `hp` to ceil(health + absorption). No-op if the objective is missing.

## Scratch results (Oct 23, 2026, ~9:55 to 10:02 PM CDT)
- Melee Alex->Bea (party) 20 -> 20; Alex->Dex (no party) 20 -> 19; Dex->Alex 20 -> 19.5
- Arrow owned by Alex: Bea 20 -> 20, Dex 20 -> 11. Harming splash by Alex: Bea 20 -> 20, Dex 20 -> 14
- Alex's wolf: bite on Bea 20 -> 20, on Dex 12 -> 9; Bea hits the wolf 8 -> 8, Dex hits it 8 -> 5
- Same results with mcMMO removed (so our guard does it, not mcMMO's party link)
- Cows/zombies: 2 together 1x; 3 together 2x (BEEF 3 -> 3+3, LEATHER 2 -> 2+2); SPAWNER 1x;
  BREEDING 1x; elitemobs-tagged 1x; zombie carrying 5 flesh 1x; non-party killer 1x
- Chat line shown to killer and partied players in range only
- `gamerule pvp` -> true on Paper 26.2 even with `pvp=false` in server.properties

## Install (empty server only)
Panel Shut Down; rename plugins/ExcelTogether-0.3.0.jar to ExcelTogether-0.3.0.jar.bak-<date>;
upload ExcelTogether-0.4.0.jar; Start. Expect the enable line to end with
"Party friendly fire: BLOCKED (pets on, potions on). Party drops: x2 when 3+ party members are
within 50 blocks, cap 150 boosted kills/hour. hp refresh: objective 'hp'." The live config.yml has
no 0.4.0 keys; the jar defaults apply (that is the plan). To change a number, add the key.

## Rollback
Panel Shut Down, delete ExcelTogether-0.4.0.jar, rename the .bak back to ExcelTogether-0.3.0.jar, Start.
Or without a jar swap: set friendly-fire.block-party-damage / party-drops.enabled false + restart.
