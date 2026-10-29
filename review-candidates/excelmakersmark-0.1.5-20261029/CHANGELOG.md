# ExcelMakersMark changelog

Every release lists every change. Newest first.

## 0.1.0 (2026-10-24) - first release

### Maker's mark (signing)
- New: tools, weapons, armor, shields, bows, crossbows, tridents, maces, spears and
  elytra that a player crafts get one gray lore line: "Crafted by <name>".
- New: signing works for normal crafting, shift-click crafting (every item made in a
  shift-click batch is signed) and smithing table results.
- New: only gear on the allowlist is signed. Anything that stacks (torches, buckets,
  food, blocks and so on) is never signed.
- New: the mark is stored in a hidden item tag: crafter UUID, their name at craft time
  and the craft date (America/Chicago). The lore line is always drawn from this tag.
- New: names use the EssentialsX nickname when set. The Essentials "~" prefix and the
  Bedrock (Floodgate) "." prefix are removed. Names are cut at 24 characters.
- New: items from Magic (wands, upgrades, spells, brushes), EliteMobs and shop plugins
  are never signed or counted (checked by Magic's own API and by item tag namespace).
- New: anvil, smithing and grindstone results rebuild the line from the tag. Lore that
  imitates the mark or the stat lines is removed, so a mark cannot be faked, copied by
  renaming, or removed.
- New: a netherite upgrade keeps the original crafter. An unsigned item upgraded at a
  smithing table is signed for the player who upgraded it (config: sign-smithing-upgrades).
- New: a crafting grid repair (two damaged items combined) copies the mark of the first
  marked input.
- New: /mark on and /mark off (per player, default on, saved in players.yml).

### Gear stats (counters)
- New: weapons (swords, axes, maces, tridents, bows, crossbows, spears) count kills,
  shown as "Kills: N". Player kills are counted separately and "(Players: M)" is added
  only when M is above 0.
- New: arrow, bolt and trident kills credit the bow, crossbow or trident that fired them.
- New: armor counts hits taken while worn. Each hit from a mob, player or projectile adds
  1 to every worn piece, shown as "Hits blocked: N". Fall, fire and other damage with no
  attacker is not counted.
- New: counts are saved on every hit. Lore is redrawn at most every 5 seconds per piece
  (stats.redraw-seconds), and right away when the player opens an inventory or leaves.
- New: boss trophies. Weapons record bosses slain, shown as "Slain: Ender Dragon, Wither x2"
  (one line, wrapping to a second only when long). Default bosses: Ender Dragon, Wither,
  Elder Guardian, Warden. Every tracked weapon that hurt the boss during the fight gets
  credit, not only the killing blow. The damager list is cleared when the boss dies or
  despawns.
- New: optional trophy extras, off by default: raid captains, and named bosses (for example
  EliteMobs bosses) matched by their name tag. The boss name is remembered when the boss
  spawns and when it is first hit, because EliteMobs swaps the name tag for hit messages
  during a fight (without this, named trophies never matched EliteMobs bosses).
- New: counters apply to all eligible gear, signed or not, except Magic and EliteMobs items.
- New: counters and trophies survive anvils, smithing and grindstones. A grid repair, or an
  anvil or grindstone combine of two items, keeps the first mark, the higher counters
  and, per boss, the higher trophy count.
- New: config master switch (stats.enabled, plus kills, hits and trophies switches) and a
  per-player toggle, /mark stats on|off. Turning stats off hides the lines and stops
  counting. The saved numbers stay and come back when turned on.

### Tip, menu and placeholders
- New: a first-use tip. A built-in tip shows on use (at most every 20 seconds) until the
  player acknowledges it: [OK] in chat on Java, /mark ok or a form on Bedrock.
  Hand-over to ExcelTips (tip.use-exceltips) is OFF by default, because ExcelTips 0.1.0
  stops a tip after 3 shows. Turn it on only if ExcelTips' makersmark entry gets max-shows: 0.
- New: on Bedrock, /mark (or a compass button that runs it) opens a pop-up with two switches,
  Maker's mark and Gear stats, plus Close (bedrock-toggle-form). Java gets chat lines.
- New: PlaceholderAPI placeholders %makersmark_mark% and %makersmark_stats% (ON or OFF)
  for the compass menu buttons.
- New: compass menu entries are provided separately (MENU-ENTRY.yml), not installed by the jar.

### Admin
- New: /mark reload (reloads config.yml, no restart), /mark inspect <player> [slot]
  (prints an item's mark and counters), /mark tipreset <player> (shows the tip again).
- New: permissions excelmakersmark.use (everyone) and excelmakersmark.admin (op).

## 0.1.2-color-credit-candidate (offline reconstruction, 2026-10-28)
- Actual weapon kill counts remain displayed; independent integer-fifths milestone credit discounts ordinary spawner/fall-softened kills five-to-one.
- Approved cosmetic text tiers: gray below100; green100; blue500; purple1000; gold5000. Armor uses existing hits blocked.
- Legacy grandfathering and per-item fractional max-merge; repair previews cannot inflate credit. No XP/drop/protection/damage changes.
- Newly reconstructed from verified0.1.1 after workspace reset; new evidence and limitations documented separately.
