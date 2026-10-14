# ExcelMobVeil 0.1.0

**Installed October 13, 2026 (America/Chicago).** Exact JAR and menu readback, loaded version, $2,000 price and startup were verified. Real Java/Bedrock screens, live purchases and gameplay remain unverified. See [installation evidence](INSTALLATION-0.1.0.md). Source [PR 14](https://github.com/ElMikeExcel/wrv-plugins/pull/14) remains draft and unmerged.

Created for Wink Reyes Valley. This is a fresh reconstruction after an earlier local workspace became unavailable. Its new build and test evidence stand on their own; old hashes and test results do not validate this reconstruction. See VERIFICATION.md for current evidence.

## Player behavior

- A confirmed purchase costs **$2,000 in-game money** and gives one stored **Veil Potion**. Buying or carrying it does not activate protection.
- Each successfully drunk potion adds **8 real-time minutes** of ordinary body invisibility and standard mob-target suppression. Time stacks: two fresh drinks give about sixteen minutes. There is no gameplay stacking cap; an unrepresentable 64-bit deadline is rejected before another bottle is used.
- Mobs using standard targeting ignore the player even while wearing armor. Armor and held items remain visible to other human players.
- Ordinary farming keeps the effect: harvesting, planting, tilling, chopping, mining and air swings do not count as attacking. Passive Thorns retaliation also keeps it.
- Attacking an entity, firing an offensive projectile, or supported player-attributed damage reveals the player and ends **all** accumulated time. Utility pearls, XP bottles and fishing casts do not end it on launch; attributed damage can still do so.
- Milk, death, expiry or an external removal/replacement of invisibility ends the veil. Normal sequential drinks extend an active veil; duplicate pending events do not grant extra time or consume another bottle.
- The absolute deadline persists across ordinary logout/restart and world changes. Offline time counts. A backward clock jump before initial activation invalidates the record rather than giving extra time. Visual cleanup runs on server ticks, normally within half a second of expiry.
- Existing unrelated ordinary invisibility prevents a new Veil Potion drink. Do not brew these potions: vanilla brewing removes their custom properties.

## Accepted first-version limits

This plugin does not cancel incoming damage. Existing projectiles, explosions, collision, environmental hazards and area attacks can still hurt. **EliteMobs custom boss scripts and independently scripted dragon behavior are outside the targeting guarantee.** Boss coverage can be expanded later based on feedback.

Wardens have their anger cleared and new anger suppressed, but can still investigate sounds or apply nearby darkness. Enderman stare targeting, slime targeting and Wither head targets receive additional handling. This is not a guarantee about every custom AI or plugin.

Effect ownership uses the plugin's state and invisibility signature. Bukkit has no durable per-effect owner identifier. Other plugins can veto effects, override targeting or apply indistinguishable effects. Changes to tagged potion metadata by other plugins are outside the tested contract. Test the actual server plugin combination and Java/Bedrock clients before claiming live compatibility.

## Commands

- `/veil buy`: open the quantity-one purchase review
- `/veil status`: show remaining time
- `/mobveil` is the full command name
- Console/admin `mobveil buy <player>` opens that player's review without charging
- Admin `/mobveil give <player> [amount]`, amount 1–64; permission `excelmobveil.admin` defaults to op

The review shows price, balance and remaining balance/shortfall. Opening or repeating a buy command never charges. Confirm alone purchases. Closing or Back cancels, and Back requests `/shop`. Quotes expire after twenty seconds; changed prices and stale/disabled buttons require a fresh distinct confirmation. Double clicks do not withdraw twice.

The plugin owns the withdrawal, delivery, rollback and any verified refund. Do not add another menu charge or give a plain invisibility potion. Uncertain transactions lock further purchases pending manual reconciliation; the journal never blindly replays a charge or refund. See PURCHASE-JOURNAL.md.

## Drink and storage failure handling

A thrown save error during the pre-consumption check cancels the drink before the bottle is used. An observed effect/save failure after native consumption rolls back the old timer and restores the original single potion only when the exact hand glass bottle is still provable. If the hand changed or restoration is uncertain, no blind item grant is made; the case is logged for an administrator. Failure to restore prior invisibility also ends mob-ignore.

Paper's void save API can swallow disk errors, and rollback saves can fail. Neither economy/playerdata nor bottle/timer persistence is crash-atomic. A disconnect, death or attack in the one-tick finalization interval invalidates pending activation; do not promise automatic restitution through every interruption.

## Configuration and dependencies

`price: 2000.0` is authoritative. `duration-seconds: 480` documents the fixed increment per bottle; this release warns and still uses 480 if changed. Vault is required, and Essentials/DeluxeMenus are soft dependencies for load order. The module is separate from ExcelDeathProtection and does not change its commands, items, state or prices. No MobIgnore code or dependency is bundled.

Build with Java 21+ and Maven 3.9+, using `./build.sh`. Existing tools/cache can be selected with JAVA_HOME, MAVEN_COMMAND, MAVEN_REPO and OFFLINE=1. The script does not bootstrap or install software. The tested native target is Paper 26.2-123 / Java 25; the compiler emits Java 21 bytecode. The cached Mockito/Byte Buddy test agent needs a test-only experimental compatibility flag on Java 25; the production plugin does not.

## Durable release gate

Follow RELEASE-CHECKLIST.md. `release_checkpoint.py` creates hash-indexed source checkpoints and verifies downloaded Library bytes. Save during development and save the final source/JAR/report/checksum bundle before calling it ready. Download the saved version into another directory and verify both bundle and JAR hashes before installation. Workspace paths and chat summaries are not backups.
