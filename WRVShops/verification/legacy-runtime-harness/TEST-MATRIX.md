# Legacy ChestShop verification map

Acceptance evidence: `evidence-0.1.2/accepted-build.json` and per-case result files. All accepted runs use the same final production hashes.

## Passed on actual plugin implementations

- All trade directions and free prices; finite/infinite admin shops; native double chests; partial quantities; exact quantity and item metadata
- Separate chooser, quantity, review and final confirmation with no earlier money/items mutation
- Cancel/Back/close/disconnect/actual expiry/newer offer/wrong player, duplicate and stale final callbacks, consumed event and cancelled-quote replay
- Sign owner/quantity/price/item changes; stock and metadata changes; customer/owner funds; customer/chest capacity; chest shape; permission and distance changes; removed sign/container
- All inventory click/action enums, right-tap confirmation, drag/outside input, sign left/right/main/offhand/sneaking/bulk settings and ordinary command paths
- Direct pre-event/transaction constructors/economic and item handlers/currency-transfer paths cannot execute without the one-shot admission
- Cancelled transactions cannot alter the stock counter; protected signs/chests resist normal buyer edits, breaks, opening and automated extraction
- Missing/disabled ExcelShops, unmodified ChestShop event-fence fallback, mismatched runtime guard API
- No premature success receipt; actual money-then-item-failure evidence locks checkout and prevents replay
- Two genuine recovery restarts preserve journal/marker, incomplete balances and physical stock state
- Two entirely absent-ChestShop boots independently detect legacy recovery evidence and pause native checkout

## Supported fail-closed restrictions

Guard1 deliberately refuses unsupported provider identities, nonzero taxes, currency precision other than2 and configured server economy accounts. Runtime tests prove nonzero-tax and provider-removal closure; focused guard tests cover the remaining constraints. These are not silently supported purchase modes.

Unmodified ChestShop without the embedded guard is not a supported deployment: an active add-on can fence dispatched events, but it cannot independently guard original directly invoked mutation methods or survive its own complete absence.

## Not claimed by these server-side tests

- Real Java/phone screens, Geyser/Floodgate packets or real player authentication/persistence
- Exact installed live Vault2.0.0 binary (official Vault1.7.3 is an explicit substitute)
- Every unrelated production plugin or a malicious privileged JVM plugin directly mutating economy/inventories
- Atomic rollback across arbitrary external-plugin changes or a server crash
- Real online-owner notification delivery

The original27-assertion baseline is a reproduction of the old gap and is not acceptance evidence. The final ten-boot6,136-assertion suite is distinct from the existing native-shop eleven-boot852-assertion migration suite.
