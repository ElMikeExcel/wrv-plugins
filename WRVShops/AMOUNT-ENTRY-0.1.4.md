# ExcelShops 0.1.4: type exact amounts

Status: installed on Wink Reyes Valley at about 8:13 p.m. Central on October 12, 2026 (01:13 UTC October 13). The server-computed artifact hash, loaded version, economy connection and startup checks passed; see the [deployment record](DEPLOYMENT-2026-10-12.md). Human Java/Bedrock touch/controller/Windows acceptance remains pending. This documentation authorizes no further deployment or settings change.

## Player flow

- Owner: open your chest/sign → Edit item prices → choose an item → **Enter Amount** → type `0.50` → **Use amount**. The price is fifty cents for one item. Review the shop, then separately confirm publication. New shop setup uses the same price field.
- Buyer: compass menu → Spawn Shop → choose an item → **Enter Amount** → type a whole number such as `19` → **Use amount**. Native chest shops use the same quantity field. Spawn quantities remain bundles where labeled; chest-shop quantities are individual items.
- Review the quantity and total, then continue to the separate final confirmation. Only that final purchase button can move money/items.
- Cancel the Java text entry to restore the previous amount; closing a Bedrock form retains its existing safe cancellation behavior. Invalid input shows an explanation and another entry field. Java text entry times out after 60 seconds. Reopening a shop or another inventory, disconnecting, teleporting, and plugin shutdown invalidate an old response.
- Existing plus/minus controls still update the same inventory in place. Typing is optional and does not reintroduce cursor resets for those controls.

Prices accept 0.01 through 100000000.00 with at most two decimal places, without a dollar sign or commas. Typing `70000` sets a valid $70,000.00 unit price. Zero, negative values (including `-0`), non-finite text, scientific notation and overflow are rejected. Quantities are positive whole numbers within available stock, inventory/bundle limits and the existing total ceiling. A quantity of `70000` is rejected: the delivery limit is at most 2,304 individual items, with smaller stock/bundle/total limits where applicable. Surrounding whitespace is trimmed. Prices and totals remain integer cents.

## Native screens and compatibility

Java uses Paper's private native text dialog with **Use amount** and **Cancel**. Escape is deliberately disabled so canceling always invokes the explicit safe navigation path. The text is not sent to public chat, and no command, anvil, XP, sign placement, or extra plugin is needed. Bedrock uses the existing Geyser numeric form first. Before any Java-dialog fallback, Geyser and Floodgate classify the player independently of the forms setting; known Bedrock or uncertain results keep shared buttons instead.

The reflective adapter retains the Java 21 / Paper 1.21.4 compile baseline and uses only public APIs. Native Java dialog support was added in Paper 1.21.7 and exists in the installed 26.2 build 123. Missing/failed APIs return to the existing amount buttons with a message. The Paper API does not establish that a protocol-translated older client can render the dialog; such clients require a human check. An upstream bridge with neither a detectable local Geyser nor Floodgate plugin is not distinguishable from Java through these APIs. A successful server show call is not proof of client display.

Reference: [Paper dialog documentation](https://docs.papermc.io/paper/dev/dialogs/) and [Paper 26.2 custom callback API](https://jd.papermc.io/paper/26.2/io/papermc/paper/registry/data/dialog/action/DialogAction.html).

## Safety and scope

Each typed entry replaces the previous page token. Queued old buttons, duplicated responses and expired callbacks cannot apply to a newer page. Dialog actions are bound to the player's UUID and share a one-shot response gate. A failed send disarms its callbacks. Entry submission only advances to purchase review or saves an owner draft; it cannot execute checkout or publish a shop.

The original native/Spawn checkout, mandatory review/final confirmation, prices/catalog, economy settings, stock, permissions, registry format and journals are preserved. Optional legacy ChestShop UI is unchanged; that plugin is not installed on this standalone server.

## Verification

- Local verification: **733 Java tests + 55 Python checks**, zero failures/errors/skips. The full aggregate passed 710 Java cases, followed by all 140 updated UI cases after test-only additions (733 unique Java cases across the final reports). Shops account for 634 Java cases. Coverage includes native dialogs, Bedrock/Floodgate routing, typed entry, interruption and bounds. Both Java and Bedrock explicitly accept `70000` as a price and reject negative/zero, non-finite, scientific-notation and overflowing input.
- Repeated local builds produce the identical standalone JAR: SHA-256 `565ae9e91e7a4516908e2d245f87ef1936aae23d055f084dc4159112e445f83c`.
- Exact Paper 26.2 build 123 / Temurin 25.0.4+7: **4 complete isolated boots / 1,104 assertions / zero failures**. Real Paper native dialog construction/callbacks, fifty-cent draft entry, 19-bundle review, retries, cancellation, stale/foreign/duplicate callbacks, real timeout, restart and missing economy/Vault startup were exercised. All child servers exited cleanly; loopback port was free.
- Production source separately compiles against the exact runtime API with `javac --release 21`: 26 source files / 75 classes, deprecated-API notes only.
- Independent source review found no blocking issue. Test suites retain the existing plus/minus cursor/window and one-shot checkout checks.
- [Build/source identity](verification/runtime-harness/evidence-excel-0.1.4/build-summary.json) and [runtime evidence](verification/runtime-harness/evidence-excel-0.1.4/summary.json).

One preceding concurrent-runtime attempt timed out during cloud-host scheduling stalls before completing the expiry case; it is excluded from acceptance. Its dirty synthetic fixture and logs were archived intact, a previously clean accepted test fixture was restored, and the complete serial rerun passed. No live recovery marker or data was changed.

These use synthetic players/economy/claims on real Paper. Human Java/Bedrock rendering, real client packets and the exact live plugin stack remain unverified. Historical tests for older versions are not counted as new runtime verification.

## Small player check after installation

1. Open your shop price field. Choose Enter Amount, type `0.50`, choose Use amount. Confirm review shows `$0.50 each`; cancel before publication unless you intend the change.
2. Re-enter, try `0.501` and `0`, then correct to `0.50`. Cancel from the text dialog once and confirm the previous price remains.
3. From compass → Spawn Shop, type a quantity. Review its bundle/item count and total; Cancel must not charge or deliver. Make an agreed small purchase only after final confirmation, checking exactly one charge and matching stock/items.
4. Reopen an entry and cancel/repeat it; wait for one timeout. Check no old response changes a newer screen.
5. Check the same price/quantity/review/cancel flow from Bedrock's native forms. Check plus/minus controls still preserve the pointer on Java.

## Installation boundary

The approved standalone 0.1.4 installation is complete. The following applies to any future separately approved replacement, not a request to repeat the installation.

Only install the reviewed ExcelShops JAR during separately approved stopped-server maintenance with coordinated backup and startup verification. Preserve the full ExcelShops data folder and both checkout journals. Do not hot-reload, change money/stock or reinstall optional ChestShop. Runtime harness JARs are test-only and must never be installed on a real server.

## Cross-platform acceptance matrix

The same shop actions, exact amounts, review and final confirmation are required on every supported device. Controls may differ. Server/API fixture tests do not count as a real-device pass.

| Client/control | Intended entry | Automated coverage | Human acceptance |
| --- | --- | --- | --- |
| Java keyboard/mouse | Enter Amount → private native text field | Paper construction/callbacks and amount/cancel/timeout tests | Pending |
| Bedrock Android/iOS touch | Native Geyser number form and touch buttons | Geyser/Cumulus API fixtures, fifty-cent price and quantity/review regression tests | Pending |
| Bedrock controller/console, where connected | Native form with controller navigation and on-screen keyboard | Same Bedrock API path; no physical controller simulated | Pending |
| Bedrock Windows keyboard/mouse | Native Geyser form | Same Bedrock API path | Pending |
| Older Java via protocol translation | Shared inventory controls when text API fails; dialog display is not detectable reliably | Server fallback tests only | Pending; verify compatibility before relying on typed entry |

For every row, check `0.50`, an exact whole quantity, invalid input/retry, Cancel/Back, repeated entry and a stale response after reopening a shop. Also check the Java text-entry timeout and the shared purchase-quote expiry. Verify totals and separate final confirmation. A small authorized test purchase must charge once and deliver the exact count; every cancellation must leave money/items unchanged. On touch/controller, check field focus, keyboard entry, button legibility and no reliance on hover or right-click.

Client routing checks Geyser and Floodgate independently of the forms setting. Known Bedrock players, disabled bridge APIs and uncertain classification never receive the Java-only dialog; shared inventory controls remain available if native Bedrock forms cannot be sent. No test here certifies the exact live Geyser/Floodgate builds or an actual phone/controller.
