# ExcelDeathProtection: two-tier specification

**Version 0.1.2 installed October 13, 2026; player acceptance pending.** Installed source identity: draft PR #13, commit `a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`. The earlier `ff224dea03d61e0f2227e65d8015d662c93c8d34` baseline is historical input. This contract describes intended 0.1.2 behavior; installation/startup verification is not proof of real Java/Bedrock-mobile UI, purchase, or gameplay acceptance. See [installation evidence](docs/INSTALLATION-0.1.2.md).

## Product contract

### Basic: Death Protection

- Price: **$15,000** in-game money, configured by `basic-price`.
- Confirmed purchase activates protection immediately. No bottle or other consumable is delivered.
- One real death preserves inventory and consumes protection. Death is never prevented.
- No Conduit Power, Luck, resistance, underwater benefit, faster mining, or other extra gameplay ability is granted.
- Uses a boss bar titled exactly **Inventory protected: 1 death**. No boss fog, sky darkening, or music flags.
- Reject a basic purchase while either tier is active, before any withdrawal. Recheck eligibility at the confirmation/charge boundary.

### Premium: +Death Protection Potion

- Price: **$30,000** in-game money, configured by `premium-price`.
- Confirmed purchase gives a storable/drinkable marked potion. Buying alone does not activate protection.
- Drinking activates premium protection in the shared active slot.
- Retains the existing Conduit Power benefits: underwater breathing, clearer underwater vision, and faster mining, including mining on land. Describe these benefits in potion lore, relevant messages, menu lore, and the player guide.
- Inventory capacity and potion delivery must be checked before charging. Never drop a paid potion on the ground as an unnoticed delivery fallback.
- Drinking while either tier is active is cancelled; the second potion is unconsumed, does not refresh/upgrade protection, and does not create a second slot.

### Shared death behavior

- At most one active tier per player: none, basic, or premium.
- A protected death keeps inventory, clears item drops to avoid duplication, and consumes the active tier exactly once.
- Protection covers ordinary death causes and dimensions, including void death; live behavior still requires testing against the server's other plugins.
- Protection uses the death event's keep-inventory behavior; it does not separately delete Curse of Vanishing items. An isolated native Paper 26.2 test retained a protected Curse of Vanishing sword. This is the observed fixture behavior, not a promise about all server/plugin combinations. Record actual cursed-item outcomes for basic-protected, premium-protected, and unprotected deaths on the current server before release. Do not require protected cursed items to vanish or change gameplay to force that earlier assumption.
- The plugin does not save XP and does not alter XP event fields or the server's existing XP policy. Do not force a claim that XP drops if the server's gamerules or another plugin says otherwise.
- Normal relogs and restarts retain the active tier. A purchase confirmation is not an entitlement.
- Status output distinguishes basic, premium, and inactive. An externally granted Conduit effect is not proof of protection.

## Purchases and commands

- Player commands: `/dp buy basic`, `/dp buy premium`, `/dp status`.
- Console/non-player sender forms (admin permission required): `deathprotection buy basic <player>`, `deathprotection buy premium <player>`.
- Compatibility aliases: bare player `/dp buy` and old console `deathprotection buy <player>` mean **premium**, with a fresh confirmation at the new $30,000 price. They must not retain an old underpriced quote or silently change product intent.
- Player buy commands always target the sender; a player cannot use the console target-player form.
- Admin `give <player> [amount]` continues to give premium potions; it is not a paid basic activation command.
- First purchase invocation displays exact product and price. A repeat for the same product within 20 seconds confirms. A product/price change requires a fresh confirmation. Logout clears an unused quote; a successful purchase has a one-second repeat-click cooldown.
- The plugin owns the authoritative price, validation, confirmation, withdrawal, and delivery. Menu entries call the plugin; they do not independently withdraw money or manufacture the bottle.
- Missing economy provider, invalid price, insufficient funds, blocked state, or delivery precondition failure must not silently debit the player or report success.
- New defaults are `basic-price: 15000.0` and `premium-price: 30000.0`. The legacy `price` key is ignored.

## Compatibility and migration

### Active player protection

Legacy `exceldeathprotection:active` byte value `1` means the player already owns premium protection. Preserve it as **premium**. Do not charge, downgrade it to basic, reset the one-death entitlement, or require a new bottle. With active byte `1`, a missing or unknown tier also resolves to premium.

### Legacy bottles

Recognized legacy `exceldeathprotection:potion` markers remain premium. Migration is metadata-only and does not charge. Refresh online player-inventory lore and known original display names at plugin enable/join and every five seconds, preserving custom names, stack contents, and all existing PersistentDataContainer values. The refresh adds a lore-version marker and is not a world-wide migration. A newly updated name is **+Death Protection Potion**. Items in unopened containers or offline inventories are not claimed to have been scanned.

Do not identify a protection potion solely by display name. Do not replace an entire item with a newly constructed bottle merely to update its label and thereby lose metadata.

### Conduit and Luck ownership limits

Older versions used infinite-duration effects with no reliable source ownership. Duration, amplifier, or a matching-looking effect is not proof that this plugin owns it.

- Basic never adds an effect.
- Premium applies Conduit Power only if missing. It must not replace a stronger/external Conduit effect or blanket-remove Luck.
- Do not blanket-clear Conduit or Luck on migration, logout, plugin shutdown, a basic purchase, or loss of protection.
- Vanilla death handles ordinary death-time effect clearing. Test servers with altered death/effect behavior separately.
- Legacy effects may remain until ordinary removal because their provenance cannot safely be recovered. Do not promise perfect source attribution or retroactive cleanup.

## Consumption event and persistence boundary

A recognized, uncancelled drink tentatively grants premium at the high-priority consume handler so immediately following damage can see the entitlement. The monitor handler rolls that tentative grant back if another listener cancelled the drink or replaced it with an unrecognized item. Only a finalized drink applies presentation/effects. Saving is scheduled for the next tick, after vanilla actually consumes the bottle, to avoid deliberately persisting both the original bottle and the active entitlement. Interruption during this window is still a persistence risk, not an exactly-once guarantee. Test cancellation, item replacement, immediate death, and save failure on the real server. Death-event interaction with other plugins, particularly other `HIGHEST` handlers, is a separate release gate. Plugins that mutate events at `MONITOR` violate the event contract and are outside the supported interaction model.

## Economy persistence and interrupted transactions

Vault's economy provider persists independently from this plugin and player data. There is no shared transaction manager. A journal improves detection and retry safety; it does **not** provide a crash-atomic purchase or an unconditional exactly-once guarantee.

The plugin-data-folder file `purchases.journal` retains the transaction audit. On startup, unresolved records lock purchases for the affected players. An unreadable journal disables all purchases rather than starting with a blank history. Journal a purchase before the charge boundary. If interruption makes withdrawal or delivery uncertain, retain an unresolved transaction lock and require operator reconciliation. Do not automatically re-charge, refund, deliver a replacement, or clear the lock based on guesswork. A failure reported by an external economy provider can still require checking its transaction history.

Paper's `Player.saveData()` is a void API, not a durable-write acknowledgment. The underlying save can be disabled or catch storage exceptions and report only a server warning. Catching a runtime exception from the API cannot detect all failed writes. Verifying an in-memory grant and syncing the plugin journal still cannot prove that entitlement or bottle delivery was durably stored. A purchase reported completed can therefore need manual recovery after a silent player-save failure or power loss, even without an unresolved journal entry. Check server settings and logs, and test this residual failure path explicitly.

Operator review must correlate the player UUID, product, quoted amount, transaction record, economy history, server save warnings/settings, and entitlement/delivery evidence. Any correction must be an explicitly authorized action. Preserve the journal and relevant player/economy data before recovery; do not erase evidence to unblock a purchase.

## Shop integration boundaries

The October 13 installed menu uses verified slot 31 for basic and slot 34 for premium, with one tier-aware plugin command for each left/right click route. Its exact checksum and preservation checks are in [installation evidence](docs/INSTALLATION-0.1.2.md). The owner reported both entries look good on his phone; Java rendering and remaining confirmation/tap behavior checks are pending.

The older repository `SpawnShop-DeluxeMenus/spawnshop.yml` and manual `make_spawnshop.py` generator remain unchanged. Regeneration can overwrite the hand-integrated choices. Resolve that maintenance gap in a separately scoped update; do not substitute the older repository menu for the live one. The supplied unassigned template is not a ready-to-load menu. No menu `[takemoney]` command belongs to either product.

## Verification and release gates

[docs/VERIFICATION.md](docs/VERIFICATION.md) lists automated and in-game scenarios. At minimum record separately:

1. Exact source baseline and candidate artifact identity.
2. Compile/package and automated tests, including what was not run.
3. Authorized deployment, verified installed version, and backups.
4. Java player check for both tiers.
5. Bedrock-mobile player check for both tiers and visible UI.
6. Legacy migration, custom metadata/effect preservation, and interrupted-economy recovery checks.

Any test purchase using real in-game funds needs approval for the product and cost. Prefer isolated fixtures/test balances for automated and staging checks. Existing 0.1.x phone-test results are historical and cannot be reused as evidence for this candidate.

## Historical context, not current acceptance evidence

The baseline documentation records that 0.1.0 was installed and tested on Bedrock mobile on October 13, 2026: purchase messaging, drinking, status, one-death item retention, XP behavior, and consumed protection passed that historical test. Its Luck icon was absent on Bedrock. The 0.1.1 revision changed the effect to Conduit Power, disclosed its benefits, introduced on-screen buy titles, and used a single $15,000 potion. The installed two-tier version supersedes that single-product contract. Its limited phone entry-appearance check was reported good; remaining client, purchase, and gameplay checks remain pending.
