# Disposable real-ChestShop integration harness

**Never install this harness on a real server.** It creates synthetic accounts, changes their disposable Essentials balances, creates a disposable GriefPrevention claim and world blocks, and stops its own server. It uses actual ChestShop, EssentialsX and GriefPrevention plugins on exact Paper. It never opens a network client or accesses a production server.

## Final accepted result

The frozen ExcelShops 0.1.2 / guarded ChestShop pair passed **10 full server boots, 557 groups, 6,136 assertions, zero failures** on 2026-10-12 UTC.

- ExcelShops JAR SHA-256: `009f321a99f30024bcaf6742e9b2dc9dfba23571eaddddad1eee9699d6fe9edc`
- Guarded ChestShop JAR SHA-256: `5e7bbf6014484ff87b5decced2df32e36b05b3aaa6c26ba03d57c72314cdd814`
- Exact Paper 26.2 build 123 and Temurin 25.0.4+7-LTS
- Actual EssentialsX 2.22.0 provider: `com.earth2me.essentials.economy.vault.VaultEconomyProvider`
- Actual GriefPrevention 16.18.7 claims and ChestShop build474 listeners
- Every child exited0; `127.0.0.1:25580` was free after every boot and the final run

`evidence-0.1.2/accepted-build.json` binds the artifacts, run counts and concise evidence hashes. Each boot has its own plugin manifest and case results. The guard also compiled against the exact Paper API with Java25 `--release 21`; copied source hashes and compiler output are included.

### Boots

1. Full paired suite: 267 groups /2,788 assertions
2. Full clean restart: 267 groups /2,788 assertions
3. ExcelShops absent, guarded ChestShop present: 105 assertions
4. Original unmodified ChestShop with active ExcelShops event fence: 42 assertions
5. Guard with deliberately mismatched runtime API buildId: 105 assertions
6. Injected inventory failure after the actual game-currency phase: 76 assertions
7. Restart of that genuine ambiguous transaction: 106 assertions
8. Another recovery restart: 106 assertions
9. ChestShop entirely absent with legacy recovery evidence: 10 assertions
10. Another absent-ChestShop recovery restart: 10 assertions

In the last two boots, the disposable native checkout marker was preserved separately and moved aside before startup. Only legacy evidence could cause the native checkout pause. The legacy journal and active-session marker remained byte-identical across all four recovery boots. Restart checks also preserved the actual incomplete-trade balances and physical chest state, without resetting them.

The injected failure intentionally lets the synthetic money phase finish, then a harness listener removes the disposable stock before delivery. The plugin emits no success receipt, records durable recovery evidence, pauses all checkout and refuses replay. It does not claim to roll back arbitrary changes by another plugin or a server crash.

### Verified behavior

- Actual BUY/SELL, free BUY/SELL, finite/infinite admin shops, native double chests, partial stock, partial funds and explicit item quantities
- Chooser, quantity, review and final-preview pages preserve both balances and both inventories; only final confirmation trades
- Real item name, original lore and enchantment metadata survive delivery; command quantity entry and right-tap final confirmation work
- Cancel, Back, close, disconnect, actual timeout, newer offer, wrong player, repeated/stale final callbacks and direct cancelled-quote replay cannot trade
- Changed price, sign quantity/item/owner, stock metadata, balances, capacity, chest shape, permissions, distance, sign or container invalidate confirmation
- All `ClickType` and `InventoryAction` enums, drag, outside clicks, main/offhand, left/right and sneaking/bulk settings preserve the final-confirmation boundary
- Registered ordinary commands do not trade directly
- Raw pre-events, transaction constructors, economic/item handlers, currency-transfer paths, consumed event identity and cancelled stock-counter updates cannot bypass the paired gate
- Missing/disabled ExcelShops and mismatched gate stay closed while ChestShop sign/chest/break/automated-extraction protection remains active
- Success receipts wait until verified delivery and durable completion; ambiguous delivery never reports success

Original unmodified ChestShop plus the add-on fence is an unsupported installation tested only for normal event-dispatch denial. Without the embedded gate, its directly invoked static mutation handlers cannot acquire the independent protection provided by the paired artifact. Do not interpret that negative boot as permission to deploy without the guard.

## Provenance and limitations

See `dependency-provenance.json` for official upstream URLs and verified hashes. ChestShop, EssentialsX and GriefPrevention match observed live versions; byte identity to live files was not established.

**Vault is an explicit compatibility substitute:** official MilkBowl Vault1.7.3-b131, not the unidentified live Vault2.0.0 14.09KB artifact. Essentials' generic “Vault Compatibility Layer” wording does not identify that fork. This is not a byte-exact whole-production-stack test.

Players are synthetic proxies backed by actual native Paper inventory storage. Plugin-level account/name lookups use a test server proxy. Actual EssentialsX handles account persistence, debit and payout. Actual GP creates and resolves claims. Actual ChestShop parses signs, validates offers, moves items and integrates economy.

Unverified: real Java/phone rendering/input, Geyser/Floodgate packet routing, authentication, actual-player saves, exact live Vault and unrelated production plugin interactions. No permissions, live inventory, real money or server files were changed. Server-side success receipt checks directly observe the synthetic customer's messages; owner delivery to a real online client is not a network-client test.

Expected logs include blocked public update/authentication lookups, the already-known Essentials unsupported-version warning for Paper26.2, and intentional denial/recovery diagnostics. No unhandled event-dispatch errors occurred in the accepted ordinary suite.

## Safe reproduction

Supply a new disposable runtime with verified official Paper/libraries, the listed dependencies, `server.properties.loopback`, an already accepted Minecraft EULA file, annotations26.0.2 and an explicit `dependency-manifest.json`. The runners create no acceptance and download no executable. They reject unexpected plugins, wrong artifact hashes, unsafe server settings or an occupied loopback port.

Run `run_all_paired.py` with `--server-dir`, `--java-home`, `--plugin`, `--plugin-sha256`, `--guard`, `--guard-sha256`, `--original-chestshop` and a new `--prefix`. `--resume` verifies already accepted manifests and resumes at the first unfinished stage. `run_paired.py` runs one named mode. The fixture uses a five-second quote expiry to exercise a real timeout quickly. Each child has a bounded timeout and only that child is stopped if needed.

Use frozen copies of both input JARs when another build may replace target files. The final disposable fixture intentionally remains recovery-locked and has ChestShop absent; it is evidence, not a reusable fresh acceptance fixture.

## Historical baseline reproduction

The unmodified0.1.1 candidate with original ChestShop474 passed27 baseline assertions that deliberately reproduced the unsafe behavior: one right-click immediately debited the synthetic customer$10, credited the seller$10 and transferred four diamonds. Those assertions prove the original gap; they are excluded from the6,136 accepted assertions. Historical evidence remains unchanged under `evidence/`.

Initial development test corrections concerned synthetic permission casing/material inheritance, an inappropriate cancelled-event expectation after Bukkit unregisters disabled listeners, and a mismatch fixture that first altered only displayed plugin metadata instead of the runtime API contract. Rejected fixture evidence is preserved in the disposable runtime. Production artifacts remained frozen throughout final acceptance.
