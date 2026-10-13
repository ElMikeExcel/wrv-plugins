# Spawn Shop integration: installed state and maintenance

**0.1.2 installed October 13, 2026.** The active menu uses basic slot 31 and premium slot 34. Exact file checks and read-only startup checks passed; the owner reports both entries look good on his phone, while Java, remaining phone UI, real purchases, and gameplay acceptance remain pending. See [installation evidence](../docs/INSTALLATION-0.1.2.md). Do not copy the unassigned template into a live DeluxeMenus directory as a complete menu.

## What is known

The installed JAR corresponds to PR #13 source commit `a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`. The older `ff224dea03d61e0f2227e65d8015d662c93c8d34` baseline and repository `SpawnShop-DeluxeMenus/spawnshop.yml` are historical inputs, not the installed menu.

The installed `deathprotection_basic` entry is a CHEST button at slot 31 for **Death Protection / $15,000**, with immediate activation. The `deathpotion` entry is a POTION at slot 34 for **+Death Protection Potion / $30,000**, to drink later. Both left/right routes use the corresponding `deathprotection buy basic|premium %player_name%` command and explain confirmation within 20 seconds. Structural comparison preserved all 20 unrelated items and every non-item top-level setting.

**Generator gap:** the historical manual `make_spawnshop.py` workflow and repository menu are unchanged. Future regeneration can overwrite these hand-integrated entries. A separately scoped generator/integration update must preserve ordinary products and keep these tier prices outside general price scaling. Until then, do not regenerate or replace the live menu from the older repository template.

`spawnshop-death-protection.template.yml` contains only the two item definitions, with deliberately unassigned slot placeholders. It is valid YAML for review, but it is **not a valid ready-to-load DeluxeMenus menu**. The source shop file has been left unchanged by this documentation task.

The owner accepts the single-item purchase flow without an ordinary-item quantity window. Exact-shortfall feedback visibility remains a reported phone usability concern; it has not been resolved by this documentation update.

## For any future authorized rollout

1. Obtain authorization for installation and any test purchases using real in-game balances. These instructions do not authorize either action.
2. Back up the existing plugin JAR, plugin data/config, relevant player data, economy data, live shop YAML, and generator/source configuration. Coordinate a consistent server backup rather than copying mutable files blindly.
3. Inspect the current live menu, its size, occupied slots, actual old potion entry, command hooks, and the generator that owns the generated YAML. Record what was inspected. Do not overwrite unrelated live changes with the older repository copy.
4. Update the existing premium listing to **+Death Protection Potion**, **$30,000**, using `deathprotection buy premium %player_name%`. Add **Death Protection**, **$15,000**, using `deathprotection buy basic %player_name%` in a verified non-conflicting slot. If there is no old listing, add both to verified slots. Change the generator/source too so regeneration will retain the integration.
5. Replace both slot placeholders with inspected integer slots. Merge the entries into the correct `items` map. Keep price lore synchronized with `basic-price` and `premium-price` in the plugin's effective configuration.
6. Check every relevant click/tap route. Each route calls the plugin exactly once. Do not add `[takemoney]`, a separate Vault withdrawal, a plain-item `give`, or independent delivery commands. Avoid menu money/space requirements that bypass the plugin's own purchase messages or incorrectly require inventory space for immediate basic activation.
7. Validate YAML, item keys, slot conflicts, command permissions, plugin startup/version, the economy provider, and menu reload using the supported server workflow. The October 13 rollout used a controlled server stop/start; it did not use a global reload.
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
4. This version does not expose an automatic player/admin recovery command. Do not edit or delete journal lines as an improvised unlock. A controlled operator-resolution rehearsal with the actual provider remains pending and must be reviewed and tested alongside any correction; until that is available and the evidence is clear, leave the purchase locked and seek implementation guidance.

## Rollback is also a migration

Restoring 0.1.1 after 0.1.2 transactions can interpret new active byte-1 basic records as premium. The old JAR also does not understand the new transaction journal. Do not assume a downgrade is safe or that a backup restore cannot duplicate items or reverse legitimate economy activity. Preserve all evidence and plan a consistent, authorized rollback or reconciliation before changing the installed version. The unresolved-transaction lock must not be bypassed by reverting only part of the system.
