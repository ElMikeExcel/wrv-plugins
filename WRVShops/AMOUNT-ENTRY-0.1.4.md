# ExcelShops 0.1.4: type exact amounts

Status: off-server candidate. No live deployment or publication is authorized by this document.

## Player flow

- Owner: open your chest/sign → Edit item prices → choose an item → **Enter Amount** → type `0.50` → **Use amount**. The price is fifty cents for one item. Review the shop, then separately confirm publication. New shop setup uses the same price field.
- Buyer: compass menu → Spawn Shop → choose an item → **Enter Amount** → type a whole number such as `19` → **Use amount**. Native chest shops use the same quantity field. Spawn quantities remain bundles where labeled; chest-shop quantities are individual items.
- Review the quantity and total, then continue to the separate final confirmation. Only that final purchase button can move money/items.
- Cancel the entry to restore the previous amount. Invalid input shows an explanation and another entry field. Typed entry times out after 60 seconds. Reopening a shop or another inventory, disconnecting, teleporting, and plugin shutdown invalidate an old response.
- Existing plus/minus controls still update the same inventory in place. Typing is optional and does not reintroduce cursor resets for those controls.

Prices accept 0.01 through 100000000.00 with at most two decimal places, without a dollar sign or commas. Quantities are positive whole numbers within available stock, inventory/bundle limits and the existing total ceiling. Surrounding whitespace is trimmed. Prices and totals remain integer cents.

## Native screens and compatibility

Java uses Paper's private native text dialog with **Use amount** and **Cancel**. Escape is deliberately disabled so canceling always invokes the explicit safe navigation path. The text is not sent to public chat, and no command, anvil, XP, sign placement, or extra plugin is needed. Bedrock uses the existing Geyser numeric form first.

The reflective adapter retains the Java 21 / Paper 1.21.4 compile baseline and uses only public APIs. Native Java dialog support was added in Paper 1.21.7 and exists in the installed 26.2 build 123. Missing/failed APIs return to the existing amount buttons with a message. The Paper API does not establish that a protocol-translated older client can render the dialog; such clients require a human check. A successful server show call is not proof of client display.

Reference: [Paper dialog documentation](https://docs.papermc.io/paper/dev/dialogs/) and [Paper 26.2 custom callback API](https://jd.papermc.io/paper/26.2/io/papermc/paper/registry/data/dialog/action/DialogAction.html).

## Safety and scope

Each typed entry replaces the previous page token. Queued old buttons, duplicated responses and expired callbacks cannot apply to a newer page. Dialog actions are bound to the player's UUID and share a one-shot response gate. A failed send disarms its callbacks. Entry submission only advances to purchase review or saves an owner draft; it cannot execute checkout or publish a shop.

The original native/Spawn checkout, mandatory review/final confirmation, prices/catalog, economy settings, stock, permissions, registry format and journals are preserved. Optional legacy ChestShop UI is unchanged; that plugin is not installed on this standalone server.

## Verification

- Root `./build.sh`: **686 Java tests + 55 Python checks**, zero failures/errors/skips. Shops account for 587 Java cases, including native-dialog API fixtures and typed-entry integration, interruption and bounds cases.
- Repeated local builds produce the identical standalone JAR: SHA-256 `d4c1a0f8e21ad536577242f5b6522e24d8e03255985b359d765da2886ec2374c`.
- Exact Paper 26.2 build 123 / Temurin 25.0.4+7: **4 complete isolated boots / 1,104 assertions / zero failures**. Real Paper native dialog construction/callbacks, fifty-cent draft entry, 19-bundle review, retries, cancellation, stale/foreign/duplicate callbacks, real timeout, restart and missing economy/Vault startup were exercised. All child servers exited cleanly; loopback port was free.
- Production source separately compiles against the exact runtime API with `javac --release 21`: 26 source files / 75 classes, deprecated-API notes only.
- Independent source review found no blocking issue. Test suites retain the existing plus/minus cursor/window and one-shot checkout checks.
- [Build/source identity](verification/runtime-harness/evidence-excel-0.1.4/build-summary.json) and [runtime evidence](verification/runtime-harness/evidence-excel-0.1.4/summary.json).

These use synthetic players/economy/claims on real Paper. Human Java/Bedrock rendering, real client packets and the exact live plugin stack remain unverified. Historical tests for older versions are not counted as new runtime verification.

## Small player check after separately approved installation

1. Open your shop price field. Choose Enter Amount, type `0.50`, choose Use amount. Confirm review shows `$0.50 each`; cancel before publication unless you intend the change.
2. Re-enter, try `0.501` and `0`, then correct to `0.50`. Cancel from the text dialog once and confirm the previous price remains.
3. From compass → Spawn Shop, type a quantity. Review its bundle/item count and total; Cancel must not charge or deliver. Make an agreed small purchase only after final confirmation, checking exactly one charge and matching stock/items.
4. Reopen an entry and cancel/repeat it; wait for one timeout. Check no old response changes a newer screen.
5. Check the same price/quantity/review/cancel flow from Bedrock's native forms. Check plus/minus controls still preserve the pointer on Java.

## Installation boundary

Only install the reviewed ExcelShops JAR during separately approved stopped-server maintenance with coordinated backup and startup verification. Preserve the full ExcelShops data folder and both checkout journals. Do not hot-reload, change money/stock or reinstall optional ChestShop. Runtime harness JARs are test-only and must never be installed on a real server.
