# ExcelItemInsight 0.1.0 — BUILD RECORD

Built 2026-10-20 (America/Chicago) for Wink Reyes Valley
(Paper 26.2 build 123, Java 25). Status: **BUILT, rig-verified,
NOT installed** — install only via INSTALL-PACKET.md inside a
standard empty-server window.

## Why this plugin exists

Jester (Jester_Turtle) asked why his Sharpness V diamond sword
"only does 7 attack damage." The vanilla tooltip shows the sword's
BASE damage; Sharpness is never added to it. The sword really
hits for 10 (15 on a crit). Mike's call: every enchantment on an
item gets explained in the item's own lore, at its exact level, in
plain words with real numbers, so new players can see what an
enchantment actually does — and how strong geared items are.

Research first (`~/workspace/iteminsight-research/REPORT-2026-10-20.md`):
no existing plugin fits. The nearest, Descriptioner (MIT), has no
per-level numbers at all (its text lookup never receives the
level), hides the vanilla enchantment display, and puts its block
on TOP of the lore. EcoEnchants / AdvancedEnchantments /
ExcellentEnchants only describe their own custom enchantments.
So: built our own.

## Design (as locked)

- Any item with enchantments (including stored enchants on
  enchanted books) carries a lore block appended BELOW existing
  lore: a quiet dark-gray header `— What the enchantments do —`,
  one plain-words line per enchantment with that level's real
  Java-edition numbers, and a small `ExcelItemInsight` footer.
  Server combat math is Java for every client (Geyser/Floodgate),
  so Java numbers are the right numbers for Bedrock players too.
  Plain-text lines only — no exotic glyphs (Bedrock parity).
- Numbers are computed BY FORMULA wherever the game is
  formulaic, so over-level books (Sharpness X) read right.
  Anything without a clean verified number ships as plain words,
  never a guessed figure (Knockback, Punch, Soul Speed, Riptide,
  Loyalty, Channeling are worded, not numbered).
- Melee weapons with Sharpness also get a Total-hit line at the
  top of the block: `Total hit: 10 (base 7 + 3 from Sharpness)`.
  Only the unconditional Sharpness bonus folds in; Smite and Bane
  stay conditional in their own lines. Base damage is read from
  the item type's own default attribute modifiers (the game's
  data — a probe on the rig printed every weapon: swords 4–8,
  axes 7–10, trident 9, mace 6, spears 1–5), with a small verified
  fallback table if an API ever returns nothing.
- The vanilla enchantment display is NOT hidden (no
  HIDE_ENCHANTS) — Descriptioner's defect, explicitly rejected.
- The plugin owns only its block: it is found by header line +
  footer signature (+ a PDC marker `exceliteminsight:managed`),
  replaced wholesale on refresh, never stacked. Foreign lore
  lines keep their original Adventure component objects — they
  are preserved exactly (signature-loot safety: BossTrial's
  "Gravewhisper" class of items). A config header change cannot
  strand an old block: the finder falls back to the footer and
  walks up over our own lines.
- Refresh triggers: EnchantItemEvent (next tick),
  PrepareAnvilEvent, PrepareGrindstoneEvent,
  PrepareSmithingEvent, PrepareItemCraftEvent,
  LootGenerateEvent, plus catch-up scans on join, inventory
  open/close/click, pickup, and held-item swap. Refresh skips
  items whose computed block already matches (no churn).
- Config: `enabled` toggle + `header` text. Command
  `/iteminsight [status|reload]`; reload needs
  `exceliteminsight.admin` (default op). On enable the plugin
  iterates the live enchantment registry and logs its dictionary
  coverage; any vanilla enchantment without an explanation is a
  SEVERE, never a silent gap.

## The dictionary — 43 of 43

The pinned registry has **43** enchantments, not the 42
Descriptioner claims: 26.2 added **Lunge** (spear). The
completeness test derives the list from the pinned jar itself —
all 43 `data/minecraft/enchantment/*.json` files extracted from
the bundled vanilla jar inside paper-26.2-123
(`~/workspace/iteminsight-research/pinned-enchantments-26.2/`) —
and the build's test suite FAILS if any of them lacks an
explanation. A runtime self-check repeats the proof against the
live registry on every enable (rig log: "registry coverage
43/43").

## Per-number sources

Every number verified 2026-10-20 against the Minecraft Wiki
enchantment pages AND the pinned definition JSONs from the
server's own jar; where they disagreed, the pinned jar won.

| Enchantment | Ships | Source |
|---|---|---|
| Sharpness | +0.5×lvl + 0.5 dmg | Wiki formula 0.5·(lvl+1); pinned damage linear base 1.0 +0.5/lvl |
| Smite | +2.5×lvl vs undead | Pinned linear 2.5/2.5, #sensitive_to_smite |
| Bane of Arthropods | +2.5×lvl vs arthropods + slows them | Pinned linear 2.5/2.5; slowness duration is a random range in the data, so the slow ships as words |
| Looting | up to +lvl extra drops; rare/gear +1%/lvl | Wiki drop tables; pinned equipment_drops +0.01/lvl |
| Fire Aspect | burns 4s×lvl | Pinned ignite linear base 4.0 +4.0/lvl |
| Knockback / Punch | words only | Pinned knockback +1/lvl has no clean player-facing blocks figure — no guessed number |
| Sweeping Edge | lvl/(lvl+1) of full damage (50/67/75%) | Pinned fraction numerator +1/lvl, denominator base 2 +1/lvl |
| Efficiency | +(lvl²+1) mining speed | Pinned `levels_squared` + 1.0 |
| Fortune | up to (lvl+1)× per ore; ~1.33/1.75/2.2× average | Wiki ore multiplier table; average computed from the same weights (formula-checked at I/II/III) |
| Silk Touch | words only | Pinned (block XP set 0 noted; behavior is loot-code) |
| Unbreaking | "about (lvl+1)× longer on average" | Honest one-liner by design — pinned remove_binomial differs for armor vs tools; the average holds for both |
| Mending | 2 durability per XP | Pinned repair_with_xp factor 2.0 |
| Protection | 4%×lvl less damage | Pinned damage_protection 1/lvl = 1 EPF/lvl; 80% total cap noted on the line |
| Fire Protection | 8%×lvl vs fire; stop burning 15%×lvl sooner | Pinned 2/lvl; burning_time −0.15/lvl |
| Blast Protection | 8%×lvl vs explosions + less knockback | Pinned 2/lvl + knockback-resistance attribute 0.15/lvl (worded) |
| Projectile Protection | 8%×lvl vs projectiles | Pinned 2/lvl |
| Feather Falling | 12%×lvl less fall damage | Pinned 3/lvl |
| Respiration | air lasts ~(lvl+1)× (≈16/32/48/64s) | Wiki breath table; pinned oxygen_bonus +1/lvl. (No vision bonus — removed in 1.13.) |
| Aqua Affinity | words only (full mining speed underwater) | Pinned submerged_mining_speed ×5 add_multiplied_total |
| Depth Strider | water slowdown cut ~⅓ per lvl; III = full speed | Pinned water_movement_efficiency +0.3333/lvl |
| Frost Walker | ice circle radius (lvl+2); magma/campfire can't hurt you | Wiki prose radius = level + 2; pinned replace_disk radius 3.0 +1.0 per level above first = lvl+2 at every level; pinned damage_immunity #burn_from_stepping |
| Thorns | 15%×lvl chance, 1–5 damage back; armor wears faster | Pinned random_chance 0.15/lvl; damage_entity min 1.0 max 5.0 (older docs say 1–4 — pinned data wins); change_item_damage 2.0 |
| Soul Speed | words only | Pinned speed/wear curves have no clean player number (0.0405 base +0.0105/lvl; wear chance 0.04/lvl) — judged too murky to print |
| Swift Sneak | sneak at (30+15×lvl)% of walk speed, cap 100% | Wiki + pinned sneaking_speed +0.15/lvl |
| Power | arrows +25%×(lvl+1) (I +50%, V +150%) | Wiki; pinned damage base 1.0 +0.5/lvl on the arrow's base |
| Flame | fire for 5 seconds | Pinned ignite duration 100 ticks |
| Infinity | words only | Pinned ammo_use set 0 for arrows |
| Piercing | hits up to (lvl+1) in a line | Wiki ("total of level + 1 entities"); pinned projectile_piercing = lvl |
| Multishot | 3 arrows, middle one recoverable | Pinned projectile_count +2, spread 10° |
| Quick Charge | reload 0.25s×lvl faster (1.25s base) | Pinned crossbow_charge_time −0.25/lvl |
| Impaling | +2.5×lvl vs aquatic mobs (Java list) | Pinned linear 2.5/2.5 vs #sensitive_to_impaling (drowned NOT in the Java tag — not claimed) |
| Loyalty | words only (returns, faster per level) | Pinned trident_return_acceleration +1/lvl |
| Riptide | words only | Pinned spin strength has no clean blocks figure |
| Channeling | words only | Pinned summon lightning on hit (thunderstorm + open sky are the vanilla conditions) |
| Luck of the Sea | treasure ≈5% + ~2.1%/lvl (7.1/9.2/11.3) | Wiki fishing table; pinned fishing luck +1/lvl |
| Lure | bite 5s×lvl sooner | Wiki + pinned fishing_time_reduction 5/5 |
| Density | +0.5×lvl dmg per block fallen | Wiki + pinned smash_damage_per_fallen_block 0.5/0.5 |
| Breach | armor treated 15%×lvl weaker | Pinned armor effectiveness −0.15/lvl |
| Wind Burst | launch ≈8 blocks × lvl | Wiki ≈8/16/24 at I/II/III |
| Lunge (new in 26.2) | words only (dash after a spear charge hit; costs hunger) | Pinned lunge.json read in full: impulse 0.458×lvl, exhaustion 4.0×lvl, +1 durability — no blocks figure exists |
| Curse of Binding | words only | Pinned prevent_armor_change |
| Curse of Vanishing | words only | Pinned prevent_equipment_drop |

## Tests

Plain-JVM suites over the Bukkit-free core (`run-tests.sh`):
**960 checks, 0 failed.** Coverage: dictionary completeness vs
the pinned registry list (file + generated class + dictionary,
all three compared), a render sanity pass over every entry at
levels 1–10, ~60 formula spot-checks (Sharpness V = +3,
Sharpness X = +5.5 by formula, Power V = +150%, Sweeping
50/67/75%, Unbreaking III ≈ 4×, Protection IV = 16%, Thorns
1–5, Fortune averages, Frost Walker radii, Quick Charge totals,
Luck of the Sea 7.1/9.2/11.3, Respiration 32/48/64s…),
replace-not-stack, stale block replaced, foreign lore preserved
line-for-line, grindstone strip, stored-enchant books, curses,
unenchanted items untouched, unknown/custom enchantments left
alone, ordering stability, Total-line rules (no total without
Sharpness, no total without a priced melee base, Smite never
folds in), and the header-change fallback (a block written
under an old header is still found by its footer and replaced).

## Rig (scratch Paper 26.2-123, the pinned jar)

Driver: ItemInsightRigDriver (Carpet-style fake player RigJava +
real station menus + real event bus), kept under `scratch/`,
never ships. Boots 1–2 were exploratory (boot 1: 39 pass / 5
"fail" — all five were driver-side expectation typos: the
driver's own foreign-lore fixture carried a literal `§7` inside
its text component and the checks expected it stripped; the
plugin had in fact preserved the lines byte-for-byte, which is
the required behavior. Fixed the expectations, not the plugin).
Boot 2: **RIGSUMMARY pass=48 fail=0, zero SEVERE** in the whole
log. After a one-word grammar fix (Quick Charge I "about 1
second"), the jar was rebuilt and **boot 3 re-ran the full
sequence against the final shipped jar: RIGSUMMARY pass=48
fail=0, zero SEVERE** (log: `scratch/rig-boot3.log`).

Proven on the rig:
- Enable line: "ExcelItemInsight 0.1.0 enabled — dictionary: 43
  enchantments, registry coverage 43/43."
- Join/open scan: Sharpness V diamond sword →
  `— What the enchantments do —` / `Total hit: 10 (base 7 + 3
  from Sharpness)` / `Sharpness 5: +3 damage on every hit.` /
  footer. HIDE_ENCHANTS verified NOT set.
- Second scan: lore identical (replace-not-stack, no churn).
- "Gravewhisper" signature sword: both foreign lore lines
  verbatim at the top, block below, total 9.5 (Sharpness IV).
- Enchanted book (stored Sharpness V + Looting III): both lines,
  no Total line. Power V bow: "+150%" line, no Total. Dirt:
  untouched, no meta written.
- Anvil (real menu): sword + Unbreaking I book → merged result
  carries exactly one fresh block with both lines, total still
  10, PDC marker present, our lines italic:false.
- Grindstone (real menu): Gravewhisper result — block gone,
  enchants gone, both foreign lines intact.
- Enchanting table: EnchantItemEvent (applied Sharpness III as
  the table does, then fired the event) → block appears, total 9.
- LootGenerateEvent: generated book gets its Power V block.
- EntityPickupItemEvent: ground bow gets its block.
- `/iteminsight status` → "v0.1.0 — enabled: true — dictionary
  covers 43/43 enchantments." `/iteminsight reload` works.
- Weapon probe (Material default attribute modifiers, main
  hand): every sword/axe/trident/mace total matched the verified
  table exactly; spears priced 1/1/2/2/3/4/5 by tier, so the
  Total line covers spears too.
- Only ERRORs in any boot log are Paper's own version fetcher
  and Mojang key fetch failing on sandbox SSL — vanilla noise,
  present with or without this plugin.

## Rebuild reproduction

`build.sh` (deterministic jar: fixed zip timestamps, sorted
entries): run twice → identical SHA-256. Clean-room rebuild
from the deposited source zip → identical SHA-256 again, and
the 960 checks pass from the clean room. The jar in the rig's
plugins folder hashes to the same value as the shipped jar.

| Artifact | Size | SHA-256 |
|---|---|---|
| ExcelItemInsight-0.1.0.jar | 24,137 | 5f300b4be8317ab2e93889be9c30499c23e422bad972825b2953d67add9ccf13 |
| ExcelItemInsight-0.1.0-source.zip | 24,661 | 4722d84586c583fdb732ea0307a61f3034eff45fed75aac916604434ecb3c5f5 |

## Known limits (stated plainly)

- The Total-hit line uses the item TYPE's base damage. An item
  with custom attribute modifiers (admin-made gear) still gets
  correct per-enchantment lines, but its Total line reflects the
  vanilla base for its type.
- Spear charge attacks scale damage by speed/tier in ways the
  base number doesn't capture; the Total line is the spear's
  plain melee hit, same convention as every other weapon.
- Smithing-table and crafting results ride the same
  prepare-event handler proven on the anvil/grindstone; they
  were not separately rig-driven.
- If another plugin registers NEW vanilla-namespace
  enchantments (none does on this server), the enable self-check
  SEVEREs and those stay unexplained until the dictionary grows.
- With `enabled: false`, blocks already on items stay as they
  are (the toggle stops writing, it does not strip).

## Drive deposits (MD5-verified local == Drive)

- Jar → Minecraft WRV / Build files: 1GcohtKn8Dh7lJQ4CqTOJ8Q2N1_kByy3L
- Source zip → Minecraft WRV / Source: 1fjCzjh4mmN0Mhic28TIzKA_YQCi6uM4A
- SOURCE.md → Minecraft WRV / Source: 1s_Pqmp8Q4bWQmaJAjf5adCdJAH5hwiAn

<!-- ExcelItemInsight — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel) -->
