# Pinned third-party ChestShop confirmation guard

This module modifies ChestShop build 474. It is separate from the custom
ExcelShops plugin. Deploying both JARs is required to enable existing legacy shops
with confirmation. Replace the old ChestShop JAR, but retain the complete
ChestShop data directory, signs, chests, inventory, and protection settings.
Never leave both the original and guarded ChestShop JARs installed.

## Build

Use JDK 21+ and the repository Maven wrapper:

1. `./ChestShop-ConfirmationGuard/bootstrap.sh` verifies the official original JAR
   and complete upstream source snapshot, and installs the pinned provided API.
   `CHESTSHOP_ORIGINAL_JAR=/path/to/original.jar` can supply an already downloaded
   original. Hash mismatch stops the build.
2. `./mvnw -B -ntp clean verify` builds the source-only overlay and custom plugins.
3. `python3 ChestShop-ConfirmationGuard/package.py` reproducibly merges the
   compiled overlay into the verified original. It emits the guarded full JAR,
   complete corresponding-source ZIP, and SHA-256 manifest under `target/`.
4. Distribute the complete corresponding-source ZIP and LGPL notice alongside
   the guarded JAR. The overlay Maven JAR is a build API artifact, not a plugin.

The bootstrap script accepts ordinary Maven flags, e.g. `-Dmaven.repo.local=...`
or `-s settings.xml`. Downloads are pinned to upstream commit
`dde7ee2155303714c2d5054deeccef4618e3939c`. The original JAR SHA-256 is
`b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2`.
All original ZIP resources remain; only listed classes, modified version text,
and guard notices/manifest change. ZIP order/timestamps/modes are deterministic.

## Supported initial deployment and deliberate restrictions

Guard revision 1 supports Paper's synchronous main server thread, the verified
EssentialsX 2.22.0 Vault provider, and zero ChestShop TAX_AMOUNT and
SERVER_TAX_AMOUNT, PRICE_PRECISION=2, and no configured server economy account.
Other providers, precision, server accounts or nonzero taxes are refused before validation.
They must be separately reviewed and tested, not silently accepted. The original
tax handler remains but cannot mutate money for an unapproved transfer. Preview
blocks Vault add/subtract calls and mutation-capable hold probes.

Signs retain their original item syntax, metadata parsing, account lookup,
permissions, access rules, admin inventories, discounts and partial transactions.
The UI explicitly chooses BUY/SELL and item count, so legacy mouse direction and
sneaking bulk shortcuts do not trade. The bound is 1..2304 requested items.
Requests equal to the sign bundle preserve the exact sign price; other quantities
use upstream DECIMAL128 unit price and PRICE_PRECISION/HALF_UP rounding. The
original NO_PRICE sentinel is retained. Discounts and partial validation run once
per preparation. Final UI confirmation must fresh-prepare and compare the quote.

## API and fail-closed lifecycle

`com.Acrobot.ChestShop.Confirmation.ConfirmationGate` exposes API_VERSION=1,
BUILD_ID, register(Plugin,OfferHandler), and runtime apiVersion()/buildId()/unavailableReason(). Only the actual
enabled ExcelShops plugin object can bind a Controller. Controller.prepare returns
a PreparedTrade; event() is inspection-only. Controller.execute consumes an exact
handle prepared in the same server tick and returns CommitResult. The adapter must
fresh-prepare after review and compare every displayed quantity/item/price and
relevant shop/configuration state before calling execute. It must journal intent
before effects and reconcile ambiguous results without automatic replay.

No sign interaction dispatches a TransactionEvent. Raw PreTransactionEvent objects
also remain cancelled outside the exact Controller preparation scope. Missing, failed, disabled or
closed ExcelShops leaves legacy trades denied while ChestShop's own protection
and management listeners stay enabled. A raw TransactionEvent is always cancelled
outside its one synchronous permit, even if another listener uncancels it. The
money, currency-transfer, tax and item phases are bound to exact object identity
and consumed once. Unknown API-generated transactions are denied. Controller
closure/disable blocks new trades but an admitted synchronous commit may finish.

Prepared handles cannot be reused, constructed by an API caller, or used by a
second controller. Mutable event fields are checked against a frozen snapshot.
Live sign identity/block data/lines, online player/world/six-block distance, access
and direction-specific permissions are checked again before money. Economy provider
identity is frozen; a pre-debit replacement is refused and an admitted provider is
retained for debit/deposit/refund after money starts. Full inventory snapshots are checked before money and before item transfer;
pre-payment stock/storage fit is rechecked with upstream utilities. After items,
exact item-metadata multiset deltas must match (virtual AdminInventory is a special
source/sink). Any leftovers, callback mutation or error is an unsuccessful,
possibly partial result; there is no automatic refund or rollback. Read the
CommitResult captured during the permit, not event.isCancelled() after return.

Upstream buyer/owner success messages are deferred. After a successful receipt,
verified balances/items and a durable committed journal entry, the paired adapter
calls Controller.acknowledgeSuccess(prepared). This consumes a separate receipt-only
capability once and emits the unchanged upstream message text/settings. It cannot
perform another trade. Failed or incomplete trades never receive success messages;
a notification failure does not undo completed economics or trigger a retry.

This prevents ordinary player/API bypass; privileged JVM plugins can still
mutate Vault/inventories directly and are not sandboxed. Upstream currency
withdraw/deposit/refund handling is not ACID. Actual balance checks and durable
recovery reporting belong to the paired adapter. Preserve backups and verify
in staging before installation. No production deployment is performed here.
