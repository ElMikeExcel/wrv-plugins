# ExcelDeathProtection

Created by us for Wink Reyes Valley.

A one-use death protection potion. The Spawn Shop sells the potion for **$10,000** (in-game money). Drink it and a buff activates: the next time you die, you **keep all of your items** and the buff is used up.

- The death itself is **not** prevented. You die and respawn as normal.
- **XP is NOT saved.** Levels are lost and XP orbs drop exactly like a normal death. This is stated on the potion itself, in game messages, and in the player guide.
- While the buff is active it shows as the vanilla **Luck** status effect: a small green clover icon beside the HUD, and it is listed inside the inventory screen, on Java and Bedrock alike. A watchdog re-applies the icon if something removes it (milk, relogs) while the buff is still active.
- Buff state is stored per player, so it survives logging out and server restarts.
- Drinking a second potion while protected does nothing and does not consume the potion.
- Items with Curse of Vanishing still vanish when you die (normal game rule).

## Why the shop sells it through this plugin

ExcelShops delivers plain material items, so it cannot hand out a custom potion. The Spawn Shop menu (DeluxeMenus `spawnshop.yml`) therefore has a Death Protection slot whose tap runs this plugin's console command `deathprotection buy <player>`, which handles the price, a two-tap confirm, the Vault charge, and giving the real potion. Players can also use `/dp buy` directly.

## Commands

- `/deathprotection status` (alias `/dp status`): is your protection active?
- `/deathprotection buy`: arm the purchase; run again within 20 seconds to confirm and pay.
- `/deathprotection give <player> [amount]`: admin (OP), for testing and gifts.

## Files

- `src/com/winkreyes/exceldeathprotection/ExcelDeathProtection.java`: the whole plugin
- `plugin.yml`, `config.yml` (just the price)
- `PLAYER-GUIDE.md`: what players see on the website
- `SPEC.md`: the design as agreed with Mike on Oct 13, 2026

## Build

Compiled with `javac` against the Paper API set used by the other Wink Reyes Valley plugins plus Vault. Version 0.1.0 JAR SHA256: `e7d13bbcae95c5c49dbaf3351d0971309917f84d58e5d34eff69fa7d2dcb60a0`.

## Status (Oct 13, 2026)

Built. NOT installed on the live server yet. Java player check and Bedrock-mobile player check still to come after installation.
