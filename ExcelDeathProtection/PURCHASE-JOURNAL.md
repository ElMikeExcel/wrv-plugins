# Purchase journal and reconciliation

Installed version 0.1.2 and source candidate 0.1.3 use the same append-only `purchases.journal` in the plugin data folder. It reduces duplicate charges and unsafe refunds, but does **not** make Vault balances and Minecraft playerdata atomic. A completed Bukkit `saveData()` call does not prove a successful filesystem write or fsync. A silent delivery save failure may lose a paid grant; a silent rollback save failure may let a refunded grant reappear after restart. **Operational gap:** 0.1.2 was installed October 13, 2026, but an isolated manual reconciliation rehearsal with the actual economy provider has not been evidenced. Installation and an empty startup journal do not validate this procedure. The candidate review window leaves `PurchaseJournal.java` and the financial transaction core from transaction UUID creation onward unchanged. See [0.1.3 verification and operations](docs/VERIFICATION-RESULTS-0.1.3.md); later backup completion does not establish transaction atomicity or a tested restore.

## What is recorded

Each UTF-8 line contains eight tab-separated fields:

1. Format version (`1`)
2. Transaction UUID
3. Player UUID
4. Product (`BASIC` or `PREMIUM`)
5. Price encoded using Java hexadecimal floating-point notation
6. Phase
7. Timestamp in milliseconds from the configured clock (normally Unix epoch)
8. CRC32 of the preceding tab-separated fields

The log contains no player names or credentials. A CRC detects accidental corruption, not malicious alteration. Each append is forced to disk before it returns. Creation also forces the parent directory entry. Unsupported or failed durability operations fail closed.

## Phase meanings

- `WITHDRAW_INTENT`: written before calling the economy provider. After interruption, withdrawal may or may not have happened.
- `WITHDRAWN`: the withdrawal response and a fresh balance read agreed. Delivery may subsequently have been attempted.
- `REFUND_INTENT`: rollback was observed and its save call returned, and a refund was about to be requested. After interruption, refund may or may not have happened.
- `RECONCILE`: payment, delivery, rollback, refund, or journal completion was ambiguous. No automatic retry is allowed.
- `COMPLETED`: expected delivery was observed and the adapter's save call returned; journal completion was then forced. This is not proof of atomic or durable playerdata persistence.
- `REFUNDED`: rollback was observed and the refund response and fresh balance agreed.
- `DECLINED`: provider failure was accompanied by zero reported transfer and an unchanged observed balance.

On startup, the latest nonterminal record for **any** transaction locks that player across both products. Completed, refunded, and declined transactions remain available for audit. Nothing is replayed, delivered, withdrawn, or refunded during journal loading. A malformed record or truncated tail prevents purchases globally rather than guessing which players are affected. A write failure also disables further purchases through that journal instance.

## Required manual response

There is deliberately no in-game unlock command or automatic repair. The required manual reconciliation rehearsal in an isolated copy with the actual economy provider remains pending. Do not improvise corrections or use production balances to test this procedure without explicit authorization.

1. Stop purchases and stop the server before changing the journal. Preserve the entire journal, playerdata, provider data/logs, and server log in a timestamped backup.
2. Identify every unresolved transaction and its player, product, price, and last known phase. For corrupt logs, preserve the corrupt bytes and inspect all potentially affected recent transactions; do not simply truncate the tail.
3. Compare provider transaction evidence with the player's actual entitlement or potion inventory. The player's current balance alone cannot determine whether a transaction was charged or refunded. A premium bottle might have been moved, consumed, or transferred, so absence from current inventory is not proof of nondelivery.
4. Have an authorized administrator make any needed correction once, recording the evidence and correction alongside the transaction UUID. Do not retry the purchase, blindly deposit the price, or remove protection to guess a resolution.
5. Only after **all** unresolved transactions have been reconciled and any journal-integrity uncertainty resolved, archive the original journal outside the active filename. Keep the archive and the resolution record. Restart to create a fresh journal, then verify that purchases are available. If some transactions remain unresolved, keep the active journal and those locks in place; this version has no supported per-player repair tool.

Do not hand-edit checksums, mark ambiguous transactions completed, delete the only audit copy, or rotate the journal merely to clear an error. Production financial or playerdata changes require the server owner's authorization; these instructions do not grant it.

## Verified offline behavior

`PurchaseCoordinatorTest` covers both prices, product/price/player-bound expiring single-use confirmation, eligibility and balance rechecks, repeated clicks, concurrent/reentrant calls, malformed/throwing economy providers, verified rollback before refund, no blind refund retry, journal failures around side effects, and restart locks. `PurchaseJournalTest` covers forced append/reload, terminal and unfinished records, integrity failure, identity/phase enforcement, immutable snapshots, and disk-write failure.

These tests use fake economy and delivery ports. They do not validate the live economy provider, live disk failure behavior, Bedrock UI, or an atomic transaction across Vault and playerdata.
