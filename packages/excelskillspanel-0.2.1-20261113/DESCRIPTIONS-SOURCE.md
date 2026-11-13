# ExcelSkillsPanel 0.2.1 — DESCRIPTIONS SOURCE (Menu Programme Item 10)

Every perk description rendered by ExcelSkillsPanel 0.2.1, cited to its exact source.

## Source of record

- **Fork source locale:** `~/workspace/mcmmo-edit/repo/src/main/resources/locale/locale_en_US.properties`
  (mcMMO 2.3.003 WRV fork source tree) — 89,302 B, SHA-256 `ff6c19c66638dc4a27072ad73ba97116e121cbe9568c0cf3725c70c47a73ed8f`.
  Line numbers in the tables below are line numbers in THIS file.
- **Live jar cross-check:** the same file extracted from the live mcMMO jar of record
  (`~/workspace/skillspanel-build-20261112/live-pulls-20261112/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar`,
  3,695,180 B, SHA-256 `4276bd3e07bdaf60c4edbb5461c1c4a61c3252e24f15f324faaa58d7300dff5c`, pulled from the live
  server Nov 12, 2026 via the PC route, board supervisor run 276; entry
  `com/gmail/nossr50/locale/locale_en_US.properties`, 91,204 B, SHA-256
  `48704fa7519f6fa9ec8e6abca622e0ffeef8ef78305d3c529c1397f728277d29`). The two files differ as whole files
  (the jar carries the WRV patch keys — LADDER-DATA check C2), but **all 93
  `<Skill>.SubSkill.<Perk>.Description` keys were compared key-by-key between the fork source and the
  live jar: zero differences.** Every description below therefore verifies against BOTH the fork source
  and the live fork build. (Independently, LADDER-DATA check C2 records all 282 `.SubSkill.` keys
  identical between the same pair.)
- **Key shape:** `<Skill>.SubSkill.<PerkCamelCase>.Description` — the fork locale's own per-perk effect text.
- **Coverage: 82 of 82 panel perks have a verifiable description. None renders without one, so the
  Item 10 fallback (name+unlock only for an unverifiable perk) is not exercised by any live perk; it is
  harness-proven with synthetic description-less perks (TEST-RESULTS.md).** Smelting's Flux Mining stays
  omitted entirely per D32 — its locale key (`Smelting.SubSkill.FluxMining.Description`, line 1157) exists
  but the perk has no code path in this fork (LADDER-DATA §5); a description does not resurrect it.
- **Normalization (whitespace only):** Smelting → Understanding the Art's raw value contains the properties
  `\n` escape (`...caves.\nPowers up...`); it renders as a single space so the description stays one panel
  line. All other values render byte-verbatim — including the fork's own spelling "Richochet" in
  Crossbows → Trick Shot. Secondary keys (`.Description.2`, e.g. Green Thumb, Call of the Wild) are NOT
  used: the primary `.Description` is the perk's effect line; `.2` keys carry mechanics continuations with
  format placeholders.
- **Ladder source of record:** `~/workspace/skillspanel-020-design-20261113/LADDER-DATA.md` (the 0.2.0
  verified ladders — names, unlocks, rank ladders — unchanged by this build; only the description field
  is new).

## Acrobatics (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Roll | `Acrobatics.SubSkill.Roll.Description` | 126 | Land strategically to avoid damage. |
| Graceful Roll | `Acrobatics.SubSkill.GracefulRoll.Description` | 131 | Twice as effective as a normal Roll |
| Dodge | `Acrobatics.SubSkill.Dodge.Description` | 133 | Reduce attack damage by half |

## Alchemy (2 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Catalysis | `Alchemy.SubSkill.Catalysis.Description` | 140 | Increases potion brewing speed |
| Concoctions | `Alchemy.SubSkill.Concoctions.Description` | 143 | Brew potions with more ingredients |

## Archery (4 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Daze | `Archery.SubSkill.Daze.Description` | 154 | Disorients foes and deals extra DMG |
| Skill Shot | `Archery.SubSkill.SkillShot.Description` | 151 | Increases damage done with bows |
| Arrow Retrieval | `Archery.SubSkill.ArrowRetrieval.Description` | 157 | Chance to retrieve arrows from corpses |
| Archery Limit Break | `Archery.SubSkill.ArcheryLimitBreak.Description` | 160 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |

## Axes (6 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Critical Strikes | `Axes.SubSkill.CriticalStrikes.Description` | 190 | Double Damage |
| Armor Impact | `Axes.SubSkill.ArmorImpact.Description` | 198 | Strike with enough force to shatter armor |
| Axe Mastery | `Axes.SubSkill.AxeMastery.Description` | 193 | Adds bonus DMG |
| Skull Splitter | `Axes.SubSkill.SkullSplitter.Description` | 187 | Deal AoE Damage |
| Axes Limit Break | `Axes.SubSkill.AxesLimitBreak.Description` | 195 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |
| Greater Impact | `Axes.SubSkill.GreaterImpact.Description` | 200 | Deal bonus damage to unarmored foes |

## Crossbows (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Powered Shot | `Crossbows.SubSkill.PoweredShot.Description` | 414 | Increases damage done with crossbows |
| Trick Shot | `Crossbows.SubSkill.TrickShot.Description` | 420 | Richochet arrows with steep angles |
| Crossbows Limit Break | `Crossbows.SubSkill.CrossbowsLimitBreak.Description` | 417 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |

## Excavation (2 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Archaeology | `Excavation.SubSkill.Archaeology.Description` | 215 | Unearth the secrets of the land! High skill levels increase your odds of finding experience orbs when you find treasure! |
| Giga Drill Breaker | `Excavation.SubSkill.GigaDrillBreaker.Description` | 212 | 3x Drop Rate, 3x EXP, +Speed |

## Fishing (6 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Treasure Hunter | `Fishing.SubSkill.TreasureHunter.Description` | 234 | Fish up misc. objects |
| Master Angler | `Fishing.SubSkill.MasterAngler.Description` | 247 | Fish are caught more frequently, works better when fishing from a boat. |
| Ice Fishing | `Fishing.SubSkill.IceFishing.Description` | 251 | Allows you to fish in icy biomes |
| Shake | `Fishing.SubSkill.Shake.Description` | 241 | Shake items off of mobs or players w/ fishing pole |
| Magic Hunter | `Fishing.SubSkill.MagicHunter.Description` | 238 | Find Enchanted Items |
| Fisherman's Diet | `Fishing.SubSkill.FishermansDiet.Description` | 244 | Improves hunger restored from fished foods |

## Herbalism (7 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Hylian Luck | `Herbalism.SubSkill.HylianLuck.Description` | 285 | Gives a small chance of finding rare items |
| Shroom Thumb | `Herbalism.SubSkill.ShroomThumb.Description` | 288 | Spread mycelium to dirt & grass |
| Double Drops | `Herbalism.SubSkill.DoubleDrops.Description` | 279 | Skillfully harvest double the loot |
| Green Terra | `Herbalism.SubSkill.GreenTerra.Description` | 267 | Spread the Terra, 3x Drops, Boosts Green Thumb |
| Farmer's Diet | `Herbalism.SubSkill.FarmersDiet.Description` | 276 | Improves hunger restored from farmed foods |
| Green Thumb | `Herbalism.SubSkill.GreenThumb.Description` | 270 | Auto-Plants crops when harvesting with hoe |
| Verdant Bounty | `Herbalism.SubSkill.VerdantBounty.Description` | 282 | Masterfully harvest triple the loot |

## Maces (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Cripple | `Maces.SubSkill.Cripple.Description` | 454 | Adds a chance to cripple your target. |
| Crush | `Maces.SubSkill.Crush.Description` | 449 | Adds bonus damage to your attacks. |
| Maces Limit Break | `Maces.SubSkill.MacesLimitBreak.Description` | 446 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |

## Mining (6 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Double Drops | `Mining.SubSkill.DoubleDrops.Description` | 307 | Skillfully mine double the loot |
| Super Breaker | `Mining.SubSkill.SuperBreaker.Description` | 304 | Speed+, Triple Drop Chance |
| Blast Mining | `Mining.SubSkill.BlastMining.Description` | 313 | Bonuses to mining with TNT |
| Bigger Bombs | `Mining.SubSkill.BiggerBombs.Description` | 317 | Increases TNT explosion radius |
| Demolitions Expertise | `Mining.SubSkill.DemolitionsExpertise.Description` | 319 | Decreases damage from TNT explosions |
| Mother Lode | `Mining.SubSkill.MotherLode.Description` | 310 | Masterfully mine triple the loot |

## Repair (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Repair Mastery | `Repair.SubSkill.RepairMastery.Description` | 345 | Increased repair amount |
| Arcane Forging | `Repair.SubSkill.ArcaneForging.Description` | 353 | Repair magic items |
| Super Repair | `Repair.SubSkill.SuperRepair.Description` | 348 | Double effectiveness |

## Salvage (2 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Scrap Collector | `Salvage.SubSkill.ScrapCollector.Description` | 379 | Salvage materials from an item, a perfect salvage depends on skill and luck. |
| Arcane Salvage | `Salvage.SubSkill.ArcaneSalvage.Description` | 382 | Extract enchantments from items |

## Smelting (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Second Smelt | `Smelting.SubSkill.SecondSmelt.Description` | 1152 | Double the resources gained from smelting |
| Fuel Efficiency | `Smelting.SubSkill.FuelEfficiency.Description` | 1149 | Increase the burn time of fuel used in furnaces when smelting |
| Understanding the Art | `Smelting.SubSkill.UnderstandingTheArt.Description` | 1144 | Maybe you're spending a bit too much time smelting in the caves. Powers up various properties of Smelting. |

## Spears (3 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Momentum | `Spears.SubSkill.Momentum.Description` | 468 | Adds a chance to increase movement speed for a short duration when attacking. |
| Spear Mastery | `Spears.SubSkill.SpearMastery.Description` | 473 | Adds bonus damage to your attacks. |
| Spears Limit Break | `Spears.SubSkill.SpearsLimitBreak.Description` | 464 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |

## Swords (5 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Rupture | `Swords.SubSkill.Rupture.Description` | 494 | A damage over time effect that ends explosively |
| Serrated Strikes | `Swords.SubSkill.SerratedStrikes.Description` | 491 | Deal partial damage in an AOE with a chance to apply Rupture! |
| Swords Limit Break | `Swords.SubSkill.SwordsLimitBreak.Description` | 499 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |
| Counter Attack | `Swords.SubSkill.CounterAttack.Description` | 488 | Reflect a portion of damage when attacked! |
| Stab | `Swords.SubSkill.Stab.Description` | 496 | Adds bonus damage to your attacks. |

## Taming (10 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Beast Lore | `Taming.SubSkill.BeastLore.Description` | 535 | Bone-whacking inspects wolves & ocelots |
| Call of the Wild | `Taming.SubSkill.CallOfTheWild.Description` | 539 | Summon an animal to your side |
| Environmentally Aware | `Taming.SubSkill.EnvironmentallyAware.Description` | 550 | Cactus/Lava Phobia, Fall DMG Immune |
| Gore | `Taming.SubSkill.Gore.Description` | 546 | Critical Strike that applies Rupture |
| Fast Food Service | `Taming.SubSkill.FastFoodService.Description` | 542 | Chance for wolves to heal on attack |
| Pummel | `Taming.SubSkill.Pummel.Description` | 554 | Your Wolves have a chance of knocking back foes |
| Thick Fur | `Taming.SubSkill.ThickFur.Description` | 552 | DMG Reduction, Fire Resistance |
| Holy Hound | `Taming.SubSkill.HolyHound.Description` | 544 | Healed by Magic & Poison |
| Shock Proof | `Taming.SubSkill.ShockProof.Description` | 537 | Explosive Damage Reduction |
| Sharpened Claws | `Taming.SubSkill.SharpenedClaws.Description` | 548 | Damage Bonus |

## Tridents (2 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Impale | `Tridents.SubSkill.Impale.Description` | 432 | Increases damage done with tridents |
| Tridents Limit Break | `Tridents.SubSkill.TridentsLimitBreak.Description` | 435 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |

## Unarmed (7 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Block Cracker | `Unarmed.SubSkill.BlockCracker.Description` | 592 | Break rock with your fists |
| Steel Arm Style | `Unarmed.SubSkill.SteelArmStyle.Description` | 584 | Hardens your arm over time |
| Berserk | `Unarmed.SubSkill.Berserk.Description` | 575 | +50% DMG, Breaks weak materials |
| Unarmed Limit Break | `Unarmed.SubSkill.UnarmedLimitBreak.Description` | 581 | Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE. |
| Arrow Deflect | `Unarmed.SubSkill.ArrowDeflect.Description` | 586 | Deflect arrows |
| Disarm | `Unarmed.SubSkill.Disarm.Description` | 578 | Drops the foes item held in hand |
| Iron Grip | `Unarmed.SubSkill.IronGrip.Description` | 589 | Prevents you from being disarmed |

## Woodcutting (5 perks)

| Perk | Locale key | Line | Description (as rendered) |
|---|---|---|---|
| Harvest Lumber | `Woodcutting.SubSkill.HarvestLumber.Description` | 615 | Skillfully extract up to double the Lumber |
| Tree Feller | `Woodcutting.SubSkill.TreeFeller.Description` | 605 | Make trees explode |
| Leaf Blower | `Woodcutting.SubSkill.LeafBlower.Description` | 608 | Blow Away Leaves |
| Knock on Wood | `Woodcutting.SubSkill.KnockOnWood.Description` | 610 | Find additional goodies when using Tree Feller |
| Clean Cuts | `Woodcutting.SubSkill.CleanCuts.Description` | 618 | Masterfully extract up to triple the Lumber |

**Total: 82 perks, 82 descriptions, 0 unverifiable.**
