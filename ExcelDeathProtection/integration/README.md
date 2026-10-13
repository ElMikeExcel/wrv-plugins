# Spawn Shop integration: candidate instructions

**Offline preparation only. No menu has been installed, and no live slot has been verified.** Do not copy the template into a live DeluxeMenus directory as a complete menu.

## What is known

The verified baseline is PR #13 at `ff224dea03d61e0f2227e65d8015d662c93c8d34`. Its `SpawnShop-DeluxeMenus/spawnshop.yml` has no Death Protection item and begins with an auto-generated-file warning. The live server may differ. An apparently empty slot in this source file is not proof of a safe live slot.

`spawnshop-death-protection.template.yml` contains only the two item definitions, with deliberately unassigned slot placeholders. It is valid YAML for review, but it is **not a valid ready-to-load DeluxeMenus menu**. The source shop file has been left unchanged by this documentation task.

## Before a future authorized rollout

1. Obtain authorization for installation and any test purchases using real in-game balances. This candidate does not authorize either action.
2. Back up the existing plugin JAR, plugin data/config, relevant player data, economy data, live shop YAML, and generator/source configuration. Coordinate a consistent server backup rather than copying mutable files blindly.
3. Inspect the current live menu, its size, occupied slots, actual old potion entry, command hooks, and the generator that owns the generated YAML. Record what was inspected. Do not overwrite unrelated live changes with the older repository copy.
4. Update the existing premium listing to **+Death Protection Potion**, **$30,000**, using `deathprotection buy premium %player_name%`. Add **Death Protection**, **$15,000**, using `deathprotection buy basic %player_name%` in a verified non-conflicting slot. If there is no old listing, add both to verified slots. Change the generator/source too so regeneration will retain the integration.
5. Replace both slot placeholders with inspected integer slots. Merge the entries into the correct `items` map. Keep price lore synchronized with `basic-price` and `premium-price` in the plugin's effective configuration.
6. Check every relevant click/tap route. Each route calls the plugin exactly once. Do not add `[takemoney]`, a separate Vault withdrawal, a plain-item `give`, or independent delivery commands. Avoid menu money/space requirements that bypass the plugin's own purchase messages or incorrectly require inventory space for immediate basic activation.
7. Validate YAML, item keys, slot conflicts, command permissions, plugin startup/version, the economy provider, and menu reload using the supported server workflow. No reload or deployment has been performed for this candidate.
8. Perform the [verification checklist](../docs/VERIFICATION.md), recording Java and Bedrock-mobile results separately. Capture the actual menu, price/confirmation titles, basic bar, premium potion lore and effect UI, and `/dp status`.

Do not leave an old **$15,000 Death Protection Potion** label calling the legacy premium alias. The alias deliberately keeps potion intent but quotes the new **$30,000** price; the displayed menu must say so before release.

## Purchase and migration cautions

- A basic purchase is immediately active only after successful confirmation and delivery. Its menu icon is a shop button, not an item the player receives.
- Premium is a stored potion until drunk. Both products share one active-protection slot.
- Legacy active byte-1 state and recognized legacy bottles remain premium at no conversion charge.
- Do not advertise that protected Curse of Vanishing items necessarily disappear. The isolated native Paper 26.2 test retained a protected cursed sword under keep-inventory; check basic, premium, and unprotected deaths on the actual server/plugin combination.
- Do not sweep player effects during installation. The source of a legacy infinite Conduit/Luck effect cannot be established from its appearance or duration. Avoid deleting effects belonging to a real conduit or another plugin.

## Interrupted transaction review

The plugin stores its audit in `purchases.journal` inside its data folder. On startup, unresolved entries lock purchases for the affected players. If that journal cannot be read, all purchases are disabled. Never substitute an empty journal to make the shop open.

A purchase can cross separate economy, plugin-journal, and player-storage boundaries. No claim of crash-atomic withdrawal/delivery is justified. An unresolved transaction is a safety lock, not permission to try charging again.

Player-data saving has an additional limit: Paper's save call returns no durable-write acknowledgment and may produce only a server warning on disk failure, or do nothing if saving is disabled. Even a completed journal plus an in-memory grant does not prove durable delivery. Inspect server save settings and warnings; a claimed successful purchase can require reconciliation after disk failure or power loss without an unresolved journal entry.

If a purchase is blocked for review or delivery durability is in doubt:

1. Preserve the transaction record, full server logs including save warnings, player data/save settings, and economy evidence. Record UUID, product, amount, and time.
2. Establish whether the economy provider recorded a withdrawal and whether protection or a bottle was delivered. Account for possible consumption, movement, logout, and restarts; an item missing now does not prove it was never delivered.
3. Escalate unclear evidence for an explicitly authorized correction. Never automatically re-charge, refund, grant a duplicate item, or delete the journal to clear the block.
4. This candidate does not expose an automatic player/admin recovery command. Do not edit or delete journal lines as an improvised unlock. A controlled operator-resolution procedure must be reviewed and tested alongside any correction before release; until that is available and the evidence is clear, leave the purchase locked and seek implementation guidance.

## Rollback is also a migration

Restoring the old JAR alone may not understand new basic state or transaction records. Do not assume a downgrade is safe or that a backup restore cannot duplicate items or reverse legitimate economy activity. Preserve all evidence and plan a consistent, authorized rollback before installation. The unresolved-transaction lock must not be bypassed by reverting only part of the system.
