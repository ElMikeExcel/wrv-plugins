# Excel custom-plugin identity migration

## SOURCE BUILD IS NOT A RENAME-ONLY LIVE DEPLOYMENT

This change prepares maintained source and migration instructions. It does not
record a server deployment, restart, or successful runtime test. **Do not use these
source-built JARs as substitutes for a narrow rename of the installed binaries.**
The source base is `0322d8ade7fe2a1a29ea20e70e1c8c9ebda6a465` and already contains
uninstalled repairs. WeekendXP's module is brought forward from exact commit
`55fcc2fe19afc649508927436db4bf222bbb83f6` so the installed 1.0.2 behavior is not
silently downgraded. Its six Java source/test files are protected by Git-blob
fingerprints in the repository tests. No mcMMO source or binary is changed here.

The separately prepared Bedrock menu-removal patch is integrated, including its
provenance and tests. Its live-compatible 1.4.2-menu.1 patch is distinct from the
canonical 1.4.3 source. See [MENU_REMOVAL.md](BedrockMenu/MENU_REMOVAL.md). That
historical preparation record retains its old candidate identity and version; an
Excel-branded live artifact needs its own exact input/output receipt.

| Existing runtime name | New runtime/artifact name | Installed baseline reported for this work | Maintained source version |
| --- | --- | --- | --- |
| BedrockMenu | ExcelBedrockMenu | 1.4.2 | 1.4.3 |
| MenuJoin | ExcelMenuJoin | 1.2.0 | 1.2.1 |
| WeekendXP | ExcelWeekendXP | 1.0.2 | 1.0.2 |
| NoPhantoms | ExcelNoPhantoms | 1.0.0 | 1.0.0 |
| RestartCountdown | ExcelRestartCountdown | 1.1.0 | 1.1.1 |
| Leaderboard | ExcelLeaderboard | 1.0.0 | 1.0.0 |
| WealthBoard | ExcelWealthBoard | 1.3.0 | 1.3.1 |
| ShopVillager | ExcelShopVillager | 1.0.0 | 1.0.1 |

Matching version numbers alone do not prove matching bytecode. Every live
replacement must be based on its verified exact installed archive, with an audit
showing only the approved identity change and necessary MenuJoin persistent-key
compatibility change. BedrockMenu additionally includes only the separately
approved Jobs & Money button/route removal. Record both archive and per-entry
SHA-256 values, final names/versions, and the exact changed entries. Do not deploy
any source repair or downgrade as part of this rename.

## What changes and what stays stable

- `plugin.yml` uses the new `name` and `provides: [OldName]`.
- Maven artifact IDs and CI output paths use `Excel` names. Source module
  directories, Java packages/main classes, command declarations, permissions,
  dependencies, authors and license material are unchanged.
- MenuJoin constructs both keys using the literal `menujoin` namespace:
  `menujoin:menu_compass` and `menujoin:autojoin_disabled`. Renaming must not
  create `excelmenujoin:*` data, rewrite player data, or replace existing items.
  Its PlaceholderAPI identifier remains `menujoin`, including `%menujoin_status%`.
- Existing villager tags, Jobs membership, balances, permissions, world/player
  data and behavior remain unchanged. Only the paired Java and Bedrock Jobs & Money entries/routes in the
  separately approved menu removal are removed.
- ExcelShops already has its name; its runtime/plugin files are unchanged here.
  mcMMO, Jobs, Geyser, Essentials, Vault, DeluxeMenus, ChestShop and all other
  third-party plugin names and branding remain unchanged. Do not rename them.
- Historical decompiles, archived classes, original patch tools, source recovery
  artifacts, old verification hashes and third-party notices stay intact. Old
  verification hashes are historical records, not checksums for new Excel JARs.

`provides` advertises a legacy plugin identity for dependency resolution; it is
not a data-folder migration or a promise of legacy qualified-command aliases.
See the [Paper descriptor documentation](https://docs.papermc.io/paper/dev/plugin-yml/).
Test actual plugin lookup/dependency behavior on the installed Paper version.

## Inventory before changing anything

1. Obtain the bounded deployment/restart approval and record the exact approved
   artifact list. Stop the server normally and verify it is fully stopped before
   copying data or changing enabled JARs. Do not hot-swap/reload these plugins.
2. Take a restorable, offline snapshot of the original eight JARs, data folders,
   affected command aliases/configs, and relevant player/world persistent data.
   Preserve all original files. Record exact bytes and SHA-256 hashes privately;
   never publish credential-bearing configs or player data.
3. Inspect every enabled plugin JAR's descriptor, not just its filename. Find
   duplicate old/new identities, alternate filenames, update-folder copies and
   pending host-managed replacements. Check dependency/provider lookups and all
   configs/command blocks/scripts that name the old plugins.
4. Search specifically for qualified commands such as `bedrockmenu:menu`,
   `menujoin:compass`, `restartcountdown:restartwarn`, `leaderboard:leaderboard`
   and `shopvillager:shopvillager`. Preserve every found old qualified route with
   an explicitly reviewed equivalent alias or narrow caller update. Retain
   arguments and sender/permission semantics and test them on the actual server.
   Do not assume `provides` preserves command namespaces. Do not invent or apply
   broad alias rewrites. The coordinator's verified inventory is the migration
   input; unresolved references block startup of the renamed set.
5. Inventory all old/new data directories, including apparently empty ones.
   The known config-bearing folders are `BedrockMenu` and `ShopVillager`.
   If other populated custom-plugin folders exist, preserve and review them
   before proceeding rather than assuming they are disposable.

## Offline data-folder copies, fail on conflict

Copy, do not move, the stopped snapshot's entire `BedrockMenu` directory to a
new `ExcelBedrockMenu` directory and `ShopVillager` to `ExcelShopVillager`.
Preserve hidden files, subdirectories, empty directories, and exact file bytes.
Do not merge, overwrite, reset defaults, or rename keys inside YAML.

The optional standard-library helper prepares a package from an already offline
snapshot. It requires both old directories, rejects symlinks/special files,
rejects any existing new-name directory in the snapshot, rejects case-conflicting
folder/data paths, and refuses an existing output path (including case variants). It verifies identical relative paths, types, byte counts and SHA-256
hashes, then rechecks that neither source changed during copying:

```sh
python3 tools/stage_excel_data.py \
  --offline-plugins /path/to/stopped-snapshot/plugins \
  --output /path/to/new-private-data-package
```

A successful `data-copy-manifest.json` is written only after every check passes.
A failure may leave an incomplete new package; never install it or merge it on
retry. Keep it for diagnosis and use a distinct empty destination after resolving
the cause. The helper cannot prove that the snapshot was taken while stopped and
does not deploy or change the live server. Its manifest remains private.

Before uploading the verified copies, fail if either destination already exists
on the server, even if it appears empty. Investigate its provenance; do not
silently overwrite or adopt it. Upload while stopped and read back/re-hash every
file, comparing exact inventory and bytes against the offline manifest. Leave the
original old-name directories unchanged for rollback.

## Reversible JAR replacement

1. With the server still stopped, place each old JAR in a dated rollback directory
   outside all plugin/update scan paths, or give it an unambiguously disabled
   non-JAR suffix verified not to load. Preserve its bytes and original path in
   the private receipt. Do not delete the old JARs.
2. Add exactly one audited Excel replacement per original plugin. Re-inventory
   all enabled descriptors and ensure none of the eight old providers or their
   new identities can load twice. A renamed filename with an unchanged descriptor
   is not a runtime rename. A renamed descriptor with stale MenuJoin keys is not
   a safe migration.
3. Apply only the reviewed qualified-command compatibility changes, preserving
   any unrelated aliases. Recheck copied data and pending update files before
   the approved startup. Never add the third-party plugins to this rename set.
4. Start normally once using the approved procedure. Save the startup log and
   verify the new eight names, intended versions, dependencies and no duplicate
   registration/missing-provider errors. Verify mcMMO/Jobs/other names are unchanged.

## Runtime acceptance remains required

- Existing player with an old menu compass: item is still recognized, right-click
  opens the menu, and `/compass` does not duplicate it. Test inventory/offhand/cursor.
- Existing player with auto-open disabled: join/rejoin remains disabled; toggling
  uses the old persistent key. Confirm `%menujoin_status%` remains accurate.
- Config readback matches the snapshot; existing shop villager still opens the
  intended menu with the same permissions and tags. No villagers are recreated.
- Exercise normal and discovered old-qualified commands with representative
  permissions on Java and Bedrock. Confirm `/menu`, `/tpmenu`, `/menujoin`,
  `/compass`, `/leaderboard`, `/shopvillager` and safe restart-warning/cancel
  behavior. Do not trigger a second production shutdown merely for a test.
- The Jobs & Money button is absent from both menus. All remaining button order
  and routes work, particularly the shifted native indices. Keep the paired Java
  menu backup and its separate deployment history.
- WeekendXP remains 1.0.2-compatible with the unchanged installed mcMMO bridge,
  Chicago schedule and skill/green-bar multiplier behavior. Confirm current
  weekday/weekend rate, not a new forced XP event. Jobs and balances are unchanged.
- Confirm the expected data directories are read without default resets and
  no unexpected `excelmenujoin:*` keys appear. Offline/static checks do not replace
  these live-stack and player checks.

## Rollback

Stop normally before any rollback. Preserve the newly created folders/logs and
current data as a dated private snapshot so no post-migration changes are lost.
Disable all eight Excel replacement JARs outside plugin/update scan paths, restore
all eight exact original JARs under their recorded filenames and re-verify hashes.
Restore only this change's reviewed command-alias edits and, if requested as part
of the same rollback, the separately backed-up menu YAML. Confirm no new/old
provider duplicate remains before starting normally once.

The original `BedrockMenu` and `ShopVillager` data folders were never overwritten.
If configuration or data changed after migration, reconcile those particular
changes while stopped rather than blindly replacing either snapshot. Do not
restore whole player/world snapshots merely for a rename: that can discard
unrelated progress. MenuJoin uses the same original persistent keys in both
directions, so this migration must not require a player-data conversion.

Recheck original plugin names/versions, menu compass/toggle, aliases, villager,
XP scheduler and unchanged third-party plugins. Record the actual terminal state
and remaining player checks; do not describe a failed startup as deployed.

## Current candidate status, October 13, 2026 UTC

Two narrow candidates are prepared and uninstalled:

- `ExcelBedrockMenu-1.4.2-menu.1-excel.1.jar`: verified original live payload,
  then the separately tested menu-only removal, then descriptor branding.
- `ExcelWeekendXP-1.0.2-excel.1.jar`: exact installed 1.0.2 JAR verified by
  whole-archive SHA-256, then descriptor branding only.

The other six original JARs have not yet been transferred to the build workspace.
Their exact live SHA-256 values are recorded; no source-built replacements have
been substituted. The MenuJoin bytecode compatibility patch deliberately refuses
unverified inputs. The complete eight-plugin deployment preflight cannot pass
until that patch and all remaining originals are verified.

See [candidate status and hashes](verification/excel-plugin-renames/candidate-status.json)
and [offline artifact tooling](tools/excel_renames/README.md). Passing metadata/payload checks
is not an actual Paper boot or player test. The entire rollout remains on hold;
there has been no live plugin rename or server restart for this batch.
