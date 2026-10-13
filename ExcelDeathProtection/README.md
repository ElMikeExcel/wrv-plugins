# ExcelDeathProtection

Created for Wink Reyes Valley.

**0.1.2 two-tier candidate, October 13, 2026.** This is an offline candidate based on PR #13 at `ff224dea03d61e0f2227e65d8015d662c93c8d34`. It has not been published or deployed. Java and Bedrock-mobile player checks are pending. See [the verification checklist](docs/VERIFICATION.md) before treating it as ready for release.

## The two products

| Product | Price (in-game money) | Activation | Extra abilities |
| --- | ---: | --- | --- |
| **Death Protection** | **$15,000** | Activates immediately after the confirmed purchase; no item or potion to drink | None |
| **+Death Protection Potion** | **$30,000** | A real potion to store and drink later | While active: Conduit Power, including underwater breathing, clearer underwater vision, and faster mining. The mining boost also works on land. |

Both products protect inventory through **one real death**. The player still dies and respawns. Protection is then consumed. XP handling is unchanged by this plugin: it does not save XP or change the server's existing XP rules. Protection uses keep-inventory: an isolated native Paper 26.2 test retained a protected sword with **Curse of Vanishing**. Do not claim cursed items necessarily disappear during a protected death. Behavior on the current live server and its plugin combination still needs verification.

There is one shared active-protection slot per player:

- Basic cannot be bought while either tier is active. The check happens before charging.
- Drinking a premium potion while either tier is active is cancelled and leaves the potion unconsumed.
- A stored premium potion does not itself activate protection.
- The active tier persists through ordinary logout/login and server restarts.

Basic uses a boss bar reading **Inventory protected: 1 death**, without boss fog, sky darkening, or music. It adds no potion effects or gameplay abilities. Premium retains its Conduit Power benefits. `/dp status` is the authoritative player check: a Conduit effect can also come from a real conduit or another plugin.

## Commands

- `/dp status`: show the active tier or no active protection.
- `/dp buy basic`: confirm and buy immediate **Death Protection** for $15,000.
- `/dp buy premium`: confirm and buy a stored **+Death Protection Potion** for $30,000.
- Console/non-player sender with the admin permission: `deathprotection buy basic <player>` or `deathprotection buy premium <player>`. Player buy commands always target the sender.
- Admin: `/deathprotection give <player> [amount]` gives premium potions for controlled tests or gifts.

The purchase flow is two-step: the first invocation shows the product and current price; repeat within 20 seconds to confirm. Switching products or prices shows a fresh confirmation, and logging out clears an unused quote. Successful purchases have a one-second repeat-click cooldown. Purchases use the plugin's Vault transaction path.

For compatibility, bare `/dp buy` and the old console form `deathprotection buy <player>` retain **premium potion** intent. They now quote and charge **$30,000** only after confirmation; they do not silently become basic purchases or use the old $15,000 potion price. Replace old menu labels and commands before enabling the new shop choices.

## Configuration

```yaml
basic-price: 15000.0
premium-price: 30000.0
```

The legacy `price` setting is ignored. Existing server configuration files may retain it, so explicitly review both new keys during a future authorized rollout. Keep shop lore in sync with the configured prices. The plugin owns the price, confirmation, eligibility checks, charge, and delivery; a menu must never perform a second independent withdrawal.

## Migration and safety limits

- Existing active legacy byte-1 protection remains **premium**, with no new charge or downgrade. With active byte `1`, a missing or unknown tier also resolves to premium rather than losing the old entitlement.
- Existing recognized legacy potion markers remain valid **premium potions**, with no conversion charge.
- Known original bottle names and lore are refreshed in online player inventories at enable/join and every five seconds. Custom display names and all persistent item data are preserved. This is not a world-wide scan of containers or offline inventories.
- Older releases applied infinite Conduit/Luck effects without reliable ownership metadata. Their source cannot be proven later. This candidate does not blanket-remove Conduit Power or Luck; it applies premium Conduit Power only when missing. Ordinary death remains responsible for vanilla effect clearing. Basic never adds these effects.
- Vault's economy storage and plugin/player storage are separate. A write-ahead transaction journal can block ambiguous retries, but it cannot make a charge and delivery crash-atomic. The plugin data-folder file `purchases.journal` retains the audit. Unresolved transactions lock the affected players from purchases on startup; an unreadable journal disables all purchases pending operator review. Do not automatically charge again or refund an ambiguous transaction.
- Paper's player-save call does not return a durable-write acknowledgment: saving may be disabled, or storage errors may only appear as server warnings. An in-memory grant and a completed journal entry do not prove the player data reached disk. Disk failure or power loss can still lose a paid entitlement. A silently failed rollback save can also leave a refunded grant in persisted data, allowing it to reappear after restart. Reconciliation must include server logs and economy/player-data evidence.

## Shop integration

The verified repository `SpawnShop-DeluxeMenus/spawnshop.yml` has **no Death Protection slot**. It also says it is generated. It is not evidence of the current live menu layout.

[Integration instructions](integration/README.md) and [unassigned item templates](integration/spawnshop-death-protection.template.yml) are provided separately. They require inspection of the live slot assignments and the menu's generator before any authorized installation. No live slots have been selected or verified.

## Build and release evidence

The candidate Maven project requires Java 21 or later and Maven 3.9 or later. Use the checked-in `pom.xml` and the parent workspace's build instructions; from this directory run `./build.sh`, or use the standard build/test command `mvn clean verify`. The script accepts `MAVEN_COMMAND` (Maven executable), `MAVEN_REPO` (dependency-cache directory), and `OFFLINE=1` (use only cached dependencies), forwards extra arguments to Maven, and prints the packaged JAR's SHA-256. Offline mode requires every pinned dependency to be available locally. Dependency access and toolchain availability must be checked separately.

A successful compile or automated test run is not a live installation or a player check. Record the exact candidate commit or artifact hash, test results, authorized installation, and separate Java/Bedrock-mobile outcomes in the release evidence. Historical 0.1.x build hashes and tests do not validate this candidate.

## Files

- `src/com/winkreyes/exceldeathprotection/`: plugin implementation
- `plugin.yml`, `config.yml`, `pom.xml`: metadata, prices, build
- [PLAYER-GUIDE.md](PLAYER-GUIDE.md): candidate player-facing instructions
- [SPEC.md](SPEC.md): behavior and compatibility contract
- [CHANGELOG.md](CHANGELOG.md): candidate changes and clearly marked history
- [PURCHASE-JOURNAL.md](PURCHASE-JOURNAL.md): audit phases, limitations, and required manual reconciliation
- [docs/VERIFICATION.md](docs/VERIFICATION.md): release gates and scenario checklist
- [integration/README.md](integration/README.md): future shop integration and recovery cautions
