# Offline Excel runtime-name candidates

These tools never upload, publish, deploy, rename a server file, or start/stop a server.

Requirements: Python 3.12 and PyYAML 6.0.3 (pinned in requirements.txt) (available in this execution environment). Input must be the exact verified live JAR, not a rebuild from recovered source. A hash match does not by itself establish provenance: retain the separate panel/production verification receipt.

## Exact scope

| Original runtime name | Pinned descriptor version | Candidate |
|---|---|---|
| BedrockMenu | 1.4.2-menu.1 | ExcelBedrockMenu-1.4.2-menu.1-excel.1.jar |
| MenuJoin | 1.2.0 | ExcelMenuJoin-1.2.0-excel.1.jar |
| WeekendXP | 1.0.2 | ExcelWeekendXP-1.0.2-excel.1.jar |
| NoPhantoms | 1.0.0 | ExcelNoPhantoms-1.0.0-excel.1.jar |
| RestartCountdown | 1.1.0 | ExcelRestartCountdown-1.1.0-excel.1.jar |
| Leaderboard | 1.0.0 | ExcelLeaderboard-1.0.0-excel.1.jar |
| WealthBoard | 1.3.0 | ExcelWealthBoard-1.3.0-excel.1.jar |
| ShopVillager | 1.0.0 | ExcelShopVillager-1.0.0-excel.1.jar |

BedrockMenu deliberately starts from the separately verified menu-only patch, preserving the removed redundant Jobs & Money menu entry. The version string retains that provenance.

## Transform

```
cd /path/to/excel_renames
python rename_plugins.py --input /exact/verified.jar --sha256 LOWERCASE_SHA256 --old-name WeekendXP --output-dir /existing/output-directory
```

Only plugin.yml name, version, and provides may change, except MenuJoin which must also receive the separately guarded namespace patch. The old runtime name is added to provides; command names, permission nodes, placeholder identifiers, dependencies, author, main class, resources, and all other payload bytes remain unchanged. Existing provides aliases are retained. Output files are never overwritten. Each candidate receives a JSON entry-by-entry SHA-256 audit. ZIP order, timestamps, entry comments, permissions, compression methods, extra fields, and archive comment are retained; compressed container bytes can differ because Python recompresses entries.

Checks reject wrong hashes, out-of-scope identities/versions, unexpected YAML or duplicate keys, duplicate/case-colliding ZIP entries, unsafe paths, special files, encrypted/unsupported compression, signature/digest metadata, alternative loader metadata, multi-release entries, missing main classes, existing outputs, and an unpatched MenuJoin. Original baseline bytes are rechecked before and after output.

```
python -m unittest discover -s tests -v
```

## Read-only deployment preflight

Create a plan JSON with schema 1 and exactly eight plugins entries. Every entry contains old_name, baseline_sha256, candidate_path, and candidate_sha256. Run against a freshly captured complete plugins-directory snapshot:

```
python deployment_preflight.py --plugins-dir /snapshot/plugins --plan plan.json > preflight-receipt.json
```

The check independently reconstructs candidates from their baseline bytes and compares the complete result, refusing unapproved class/resource/archive changes even when a plan supplies their hashes. Real eight-plugin preflight cannot pass while the MenuJoin namespace patch is blocked. It also rejects pending JARs in plugins/update. Specify all other server-configured update directories in additional_update_directories and other plugin scan locations in additional_plugin_directories; unlisted custom locations are not observable. It rejects old/new duplicate JARs even when filenames were changed, competing provides aliases, hash drift, missing baselines, mismatched candidate identities, and any existing Excel configuration directory. It hashes the entire original data directory recursively and preserves all baseline paths/hashes in its read-only receipt. No symlinks or case-conflicting names are permitted. It does not merge or overwrite config directories.

Before any later deployment, the server must be fully stopped, all original artifacts/data backed up outside plugins, and hashes plus destination absence rechecked immediately. Remove each old JAR before installing its replacement; copy its entire existing old-name data directory to the exact Excel name, verify every copied file hash, and retain the original unchanged. Do not leave old and new JARs together. Exact rollback requires restoring the old JAR and complete original data directory while stopped.

## Verification boundary

Passing offline checks establishes payload preservation and guarded metadata intent. It does not establish actual Paper provides lookup/dependency behavior, active plugin enablement, command ownership, configuration migration effects, or gameplay persistence. The panel server is Paper 26.2 build 123; its runtime is not currently available locally. These runtime checks must be reported separately, never inferred from YAML semantics or mock tests.

The original live BedrockMenu 1.4.2 is accepted only at SHA-256 68eb426b83f827a9fbf92f83cac4f4922f9904ba3aafc2dc9c1a0f27667fc841 with the exact reviewed combined candidate b84f9fc0f118c330a9e8c9fd807660db6016fedd834a241a6242b1b4b26a0b33. This special case preserves the separate original → menu.1 → excel.1 provenance chain. All other candidates are recreated byte-for-byte from their baseline during preflight.

Additional plugin directories are duplicate-detection inputs only; all eight baseline JARs must be in the primary plugins directory. Data-root remapping is not inferred. Each scanned directory is checked for case-colliding names.
