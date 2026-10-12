# Offline purchase-path pause specification

The owner approved preparing a reversible pause of the Jobs item shop and both purchase-bearing DeluxeMenus examples for the later approved installation. **No live write is performed or newly authorized by this file.** The candidate contains this specification and a generator, not exported production configuration or credential-bearing defaults.

## Exact intended changes

1. In a fresh exported `plugins/Jobs/generalConfig.yml`, change the unique top-level `jobs-shop-enabled: true` to `false`.
2. In a fresh exported `plugins/DeluxeMenus/config.yml`, remove only the complete `gui_menus.advanced_menu` and `gui_menus.requirements_menu` registration blocks. Keep comments, blank lines and every unrelated byte. Keep the actual example menu files unchanged.
3. Apply the reviewed staged files only during separately approved stopped-server maintenance and a normal restart. Removing registrations means neither menu is loaded, including its aliases and `dm open` target. Merely removing permission grants or setting `register_command: false` does not close console/API menu opens and is not this change.
4. Do not change LuckPerms, any other permissions, Jobs jobs/points/balances/offers, unrelated menus or the matched Spawn Shop catalog/menu. Recheck fresh inputs against the live files immediately before an approved installation.

## Prepare private staged copies

Run `python3 tools/prepare_purchase_pauses.py` from this installation pack (or the same script under `WRVShops/tools/` in source):

```sh
python3 tools/prepare_purchase_pauses.py \
  --jobs-config /private/export/Jobs/generalConfig.yml \
  --deluxemenus-config /private/export/DeluxeMenus/config.yml \
  --output-dir /private/staging/purchase-pauses-NEW
```

Inputs must be regular nonlinked UTF-8 exports with simple one-file block-map registrations naming exactly `advanced_menu.yml` and `requirements_menu.yml`; another registration of either example file is also refused. Duplicate/missing/ambiguous keys, anchors/aliases in target entries, flow mappings, malformed targets, symlinks/hardlinks and any pre-existing output are refused. Sources are read only. No network, plugin command, file replacement, live reload or deployment occurs. The new output directory and its files are private; four files are written under `backups/` and `staged/`, with original/staged sizes and SHA-256 hashes in `MANIFEST.json`. Original LF/CRLF endings and all unrelated bytes are preserved. If an interrupted run lacks its final manifest, retain it as incomplete evidence and use a new output path after review.

**Never commit or upload exported/generated full configs.** Jobs config can contain credentials. Generate outside every Git checkout/worktree (the tool rejects Git-controlled output paths), retain in private operator storage, review only the bounded diff, and transfer only through the separately authorized installation channel. The installation ZIP intentionally has no Jobs/generalConfig.yml or replacement DeluxeMenus/config.yml.

## Verification and reversal

On staging after a clean startup, verify `/jobs shop` cannot purchase, both example menu names/aliases/console opens are unavailable, all three retained preflight menu registrations (including Spawn Shop) still load unchanged, and source economy data is unchanged. Confirm the examples were not automatically restored in config or memory; preserve the `gui_menus` section and its other registrations rather than removing that whole section. The inspected official DeluxeMenus loader creates examples only when that entire section is absent, but exact live 1.14.1 source lineage is not yet confirmed, so a real post-restart check is mandatory. A changed menu inventory requires fresh review. This closes only the enumerated paths; repeat the whole-server purchase audit before claiming complete coverage.

The backup copies exactly reproduce the exported input bytes. Their presence is a recovery option, not permission to reopen unconfirmed purchases. Restoring the Jobs flag or example registrations needs a separate explicit decision. Keep the guarded ChestShop JAR or leave the server stopped with all trading disabled; never silently restore instant purchasing as rollback.
