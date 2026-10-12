# Together + Weekend XP

Custom mcMMO build **2.3.003-WRV-XP-1**, paired with **WeekendXP 1.0.2**.
This is a review/staging candidate. Nothing here installs plugins, changes the live server, or restarts it.

## Gameplay rules

- Together: **1.25×** XP while at least **one other online member of the same mcMMO party** is strictly less than **32 blocks** away in the same world. The existing `mcmmo.perks.partybonus` permission is still required.
- WeekendXP: **1.20×** on Saturday/Sunday in `America/Chicago`, using the existing immediate-on-enable and every-30-minute check. The transition can lag midnight by up to 30 minutes at normal tick rate.
- Both active: **1.20 × 1.25 = 1.50×**. The factors multiply; they are not added and neither overwrites the other.
- Eligible mcMMO skill XP and eligible vanilla green-bar XP are separate awards. Earning Mining XP boosts Mining, never every skill at once.
- Minecraft levels have nonlinear XP costs. These are XP-point multipliers, not level multipliers.

### mcMMO coverage

All 17 directly earnable skills use the same existing path, with no skill whitelist: Acrobatics, Alchemy, Archery, Axes, Crossbows, Excavation, Fishing, Herbalism, Maces, Mining, Repair, Spears, Swords, Taming, Tridents, Unarmed, and Woodcutting.

Smelting and Salvage remain derived child skills. Smelting uses Mining/Repair; Salvage uses Repair/Fishing. Any child XP routed through mcMMO's gain pipeline splits into its existing parents once, subject to their skill permissions.

Normal `SELF` and `PASSIVE` gameplay gains qualify. Admin commands, external/custom API awards, raw XP APIs, vampirism, and already-shared XP do not independently receive Together again. The earner's Together factor is carried through party sharing exactly once; Together never boosts the party's separate leveling XP. Weekend/global rates and other existing skill formulas/perks retain their normal behavior.

The original custom patch correctly calculated fractional values but the upstream pre-XP event getter truncated them before storage. The new patch adds raw float getter/setter methods, retains deprecated integer signatures for compatibility, and honors pre-event cancellation. Real profile-balance tests cover fractions, sharing, external modifications and cancellation.

### Green-bar coverage

A single event handler boosts **positive XP-orb pickup awards after Mending**. It combines Weekend and Together before one rounding step and saves fractional bonuses in the player's persistent data. Four eligible 1-XP pickups at 1.25× give 5 total XP; two at 1.50× give 3. Leaving the party or temporarily losing eligibility does not erase a previously earned fraction.

The collector's eligibility at pickup determines Together. It need not be the player who originally mined, killed, traded or threw the bottle. The handler never changes the orb's stored value, creates bonus orbs, calls `giveExp`, or assigns vanilla XP to an mcMMO skill.

Allowed identified sources: mob/entity deaths, mining/block breaks, furnace extraction, breeding, villager trading, fishing, ordinary bottles o' enchanting, grindstones, and explicitly marked mcMMO-created gameplay orbs (e.g. Excavation/Woodcutting rewards). Known player-death refunds and unmarked custom/unknown sources are excluded. Creative and Spectator awards are excluded. World blacklist, WorldGuard main flag and loaded mcMMO profile gates apply.

Direct `/experience` commands, direct `giveExp`/`setExp`/level changes, direct advancement XP rewards and third-party direct grants do **not** pass through this hook and are not boosted. Mending itself is not boosted; only the points left for the green bar are.

#### Platform and provenance limits

The vanilla extension requires **non-Folia Paper 1.21.8 or newer** with `PlayerExpChangeEvent.getSource` and `ExperienceOrb.getSpawnReason`. The plugin still compiles against Spigot 1.20.5 / Java 17, and ordinary mcMMO skill behavior is retained on other platforms. Missing/incompatible APIs, older Minecraft versions, or Folia disable the vanilla extension with a startup warning; WeekendXP warns/retries rather than claiming green-bar success.

The guard does **not** provide a complete XP-bank or death-recycling guarantee. Paper's creation-time orb merging can combine different sources without a merge event, preserving only the surviving orb's reason. A death refund can therefore inherit an eligible gameplay label, and a legitimate orb can inherit an excluded one. Ordinary later merges have the same provenance caveat. Fixing this fully requires upstream orb accounting changes; this patch deliberately does not rewrite death or orb mechanics.

An XP-bank plugin that pays withdrawals using real thrown XP bottles can likewise look like ordinary gameplay. Review any XP-bank/bottle plugin integration before installing it. Direct API withdrawals bypass this handler; unmarked custom-orb withdrawals fail closed. Other plugins can change the event before or after this handler, so actual plugin-stack tests remain required.

## Configuration and status

Existing keys under `Party.Together_Bonus` are preserved: `Enabled` (default false), `Multiplier` (1.25), `Range` (32.0), and `Min_Nearby_Members` (1). Keep the server's intended enabled flag when deploying the paired builds. The new defaults do not silently enable Together on unrelated installs.

WeekendXP controls mcMMO's existing global skill rate through the same namespaced commands, plus the separate runtime vanilla event factor through a capability-checked API. It never listens to pickup events or independently rounds a bonus. Its setter result is read back before a schedule state is cached as successful. The vanilla rate starts at 1.0 when mcMMO starts and WeekendXP reapplies the current schedule afterward.

`/party info` retains the existing Together skill-bonus status. A dedicated green boss bar shows the current XP bonus factors, including the base ×1.00 state, and refreshes roughly once per second. It uses its own bar so it does not overwrite mcMMO action-bar alerts or temporary skill progress bars.

- `/xpbonus` or `/xpbonus status`: current factors, permitted per-skill rates and relevant restrictions
- `/xpbonus hide`, `/xpbonus show`, `/xpbonus toggle`: change only this player's indicator visibility, saved in player persistent data
- `General.XP_Bonus_Indicator.Enabled` (default true): server-wide display switch, independent of awarding XP

When mcMMO and green-bar rates differ, the display shows each separately. Per-skill event overrides appear as a range in the bar and exact values in the command. Unsupported green-bar bonuses say unavailable; Spectator mode marks green-bar bonuses inactive without claiming that pending mcMMO passive gains are impossible. A denied WorldGuard gameplay-XP flag is identified separately from the independently eligible green-bar channel. The displayed numbers are bonus factors; action-specific base XP, formula modifiers, perks, caps and plugin award restrictions still apply.

The indicator cleans up on quit/disable and refreshes immediately after a world change. On Folia it is disabled to avoid unsafe cross-region party queries. Java and Geyser/Bedrock use the standard Bukkit bossbar path, but actual client rendering and overlap with other bossbars remain staging checks.

## Reproducible source base and build

Upstream: https://github.com/mcMMO-Dev/mcMMO

Pinned upstream commit: `058279c148a65bd708dc85067d3858f6adde1173` ("back to dev", immediately after 2.3.002). Apply the patches in filename order; the first two historical patches are unchanged, and the third is the feature extension.

```sh
git clone https://github.com/mcMMO-Dev/mcMMO.git
cd mcMMO
git checkout -b wrv-xp 058279c148a65bd708dc85067d3858f6adde1173
git am /path/to/mcMMO-TogetherBonus/patches/*.patch
mvn clean verify
```

The normal upstream test profile excludes tests tagged `skip`, `stress`, and `docker`; no database implementation is changed. Docker/stress suites and live-server/client tests are separate checks. Build WeekendXP from the `wrv-plugins` root with JDK 21: `./mvnw -pl WeekendXP -am verify`.

Do not drop the custom mcMMO jar alongside an older mcMMO jar. Deployment needs one version of each plugin, backups, an approved restart, startup/capability checks, and the staging checklist in `VERIFICATION.md`.

## Primary-source implementation evidence

- [Paper 26.2 pickup and merge implementation, inspected commit](https://github.com/PaperMC/Paper/blob/58e6e45d67c998909581f9079189ef51eb6f1267/paper-server/patches/sources/net/minecraft/world/entity/ExperienceOrb.java.patch)
- [Paper 26.2 event factory, post-Mending amount](https://github.com/PaperMC/Paper/blob/58e6e45d67c998909581f9079189ef51eb6f1267/paper-server/src/main/java/org/bukkit/craftbukkit/event/CraftEventFactory.java#L1268-L1274)
- [Paper source event API](https://jd.papermc.io/paper/1.21.4/org/bukkit/event/player/PlayerExpChangeEvent.html#getSource()) and [orb spawn reasons](https://jd.papermc.io/paper/1.21.4/org/bukkit/entity/ExperienceOrb.SpawnReason.html)
- [Paper direct player XP APIs](https://github.com/PaperMC/Paper/blob/58e6e45d67c998909581f9079189ef51eb6f1267/paper-server/src/main/java/org/bukkit/craftbukkit/entity/CraftPlayer.java#L1718-L1772)
- [Folia threading rules](https://github.com/PaperMC/Folia#the-new-rules)

The inspected Paper branch commit is not proof that a filename such as `Paper26.2-123.jar` contains that exact implementation. Runtime build/version verification remains required.
