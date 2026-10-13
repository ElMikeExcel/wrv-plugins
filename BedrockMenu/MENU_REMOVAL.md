# Menu-only BedrockMenu 1.4.2 hotfix

## Status

Prepared and verified offline on 2026-10-13; not deployed. Deployment is pending. Runtime branding changes were canceled; this candidate
retains the BedrockMenu plugin identity, data folder and command namespace.
The canonical source tree includes separate, undeployed 1.4.3 spawn repairs;
those repairs are not included in this menu-only candidate.

## Change

Remove only the `Jobs & Money` button and its matching `jobs browse` command
from the two native-menu arrays. Remaining entries keep their original order:

| Index | Button | Action |
| --- | --- | --- |
| 0 | Spawn | `spawn` |
| 1 | Teleport to a player | `tpmenu` |
| 2 | Spawn Shop | `shop` |
| 3 | mcMMO Stats | `mcstats` |
| 4 | Leaderboard | `leaderboard` |
| 5 | Server Info | `motd` |
| 6 | Auto-open on join | `menujoin` |

Jobs, enrollment, economy, permissions, spawn behavior and teleport behavior
are unchanged. The paired Java DeluxeMenus `server_menu.yml` removal is a
separate configuration change; the Bedrock plugin JAR alone does not edit it.
The Java YAML removal was saved, reread and successfully reloaded at
2026-10-13 02:22:39 UTC, with the original preserved as
`server_menu.yml.pre-remove-jobs-20261013.bak`. Java-client rendering remains
unverified.

## Exact payload provenance

The live `BedrockMenu-1.4.2.jar` archive hash, verified in the host panel, is:
`68eb426b83f827a9fbf92f83cac4f4922f9904ba3aafc2dc9c1a0f27667fc841`.

A deterministic reconstruction from the archived 1.4.1 classes and historical
1.4.2 patch has archive hash:
`50c7d8cbd3095a3f420c342421b97f8200c581a45069b3baffdb11863cf487b3`.
The archive bytes differ. A complete host-panel extraction/inventory check and
per-file SHA-256 comparison verified that all six live payload files exactly
match the reconstruction:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `META-INF/MANIFEST.MF` | 56 | `9e078ae6bed9538e69ae118a88314cf1ce0b29164e713a2574ec9041b044c9ca` |
| `com/winkreyes/bedrockmenu/BedrockMenu.class` | 6391 | `79ba8f402521e0822b86fe322375b396c477cfda381e55ab0acd5e93a2cfd9cb` |
| `com/winkreyes/bedrockmenu/GeyserForms.class` | 8176 | `446bf5db742d008a995d53d88e225d6a46006263cb25f01b0cf7ba45edc843fe` |
| `com/winkreyes/bedrockmenu/TeleportMenu$PickHolder.class` | 687 | `9ab6500f87e7b6448d56de25b15d9284f0fe1ffb2b7a754a59b2226ed002b9bb` |
| `com/winkreyes/bedrockmenu/TeleportMenu.class` | 12963 | `bfb5accd2713aff340f39a91f878a39ce2d829b5b52075db1079dab4d0712ba0` |
| `plugin.yml` | 452 | `513d2e9616b41c7edd5adfbc7249b577302b94e81bee742652cce5e073069335` |

There are no other extracted files. ZIP packaging is not asserted to match.

## Candidate and isolated verification

`BedrockMenu-1.4.2-menu.1.jar`, 13,664 bytes:
`fef68e9a06053767fa8223811e4ca2b1294a05daefa0e7bb11346ceba730f276`.

Only `BedrockMenu.<clinit>()V`'s `Code` attribute changes in the class payload.
All constant-pool bytes, fields, other attributes, eight other methods, three
other classes and the manifest are byte-identical. `plugin.yml` changes only
the version and description. Unused job strings remain in the constant pool
to preserve all unrelated class bytes; the runtime arrays and all active menu
routes exclude them.

Seventeen Python regression/guard tests pass, including deterministic output,
input hash/version/shape rejection, duplicate/signed archive rejection and
unrelated-change detection. JVM `-Xverify:all` tests run the actual original
and candidate main classes with Bukkit, GeyserForms and TeleportMenu test
doubles: 38 baseline and 36 hotfix assertions pass. These cover every ordered
button, scheduling, original spawn fallback/picker behavior, shifted command
routes, invalid indexes, offline responses, repeated selections, native-form
reopening and Java/form-failure fallback. These are isolated tests, not a
Paper/Geyser/Essentials or device integration test. The focused canonical BedrockMenu Maven suite passed all 12 tests, including
the new ordered-array regression test. Its 1.4.3 output is not the narrow live
candidate and must not be substituted for the verified 1.4.2-menu.1 JAR.

The full repository aggregate also passed: 734 Java tests with zero failures,
errors or skips. The existing packaging/repository/shop Python suites passed;
the final Java server-menu/shop suite passed 12 tests. These maintained-source
checks do not certify a different deployable hotfix JAR.

All four hotfix classes also linked and verified against the real Paper
26.2 build 123 API on Temurin 25.0.4+7, including the seven-entry array checks,
without Bukkit test doubles. Actual server startup remains pending; this
linkage check is not a Paper server or Geyser/client integration test.

## Repeat from the exact live JAR

The standard-library patcher refuses a mismatched input hash and unexpected
class/array shape, and does not overwrite an existing output:

```bash
python3 BedrockMenu/tools/remove_jobs_menu.py build \
  /path/to/BedrockMenu-1.4.2.jar /new/output/BedrockMenu-1.4.2-menu.1.jar \
  --input-sha256 68eb426b83f827a9fbf92f83cac4f4922f9904ba3aafc2dc9c1a0f27667fc841 \
  --report /new/output/menu-removal-audit.json
```

Patching the actual live archive will retain its entry metadata and can
produce a different complete output archive hash from the payload-equivalent
candidate above. Always record the final output hash and compare its payload.

Historical reconstruction material (`classes-1.4.1`, `decompiled-1.4.1`,
`history`, the original `tools/Patch.java` and `tools/plugin-1.4.2.yml`) remains
unchanged. Do not build/deploy the canonical 1.4.3 source as a substitute for
this narrow hotfix.

## Re-run patcher regression tests

The 17 fail-closed Python checks are included and need an unmodified 1.4.2
baseline. Use the exact installed JAR, or reconstruct the historical payload
with `BedrockMenu/tools/rebuild-1.4.2.sh`. The six payload hashes are recorded in
`verification/menu-removal/baseline-payload-sha256.json`. A different ZIP hash
alone does not establish a payload mismatch or a payload match.

```bash
BEDROCKMENU_BASELINE=/path/to/BedrockMenu-1.4.2.jar \
  python3 BedrockMenu/verification/menu-removal/test_remove_jobs_menu.py
./mvnw -pl BedrockMenu -am verify
```

## Before and after deployment

Obtain deployment/restart approval. Keep the original BedrockMenu runtime name.
Back up the live JAR and Java menu YAML. Replace the old enabled JAR rather
than leaving two BedrockMenu JARs. Check startup for the expected version and
errors. Java and Bedrock players should each see no Jobs & Money entry; verify
all remaining buttons, including the four shifted native entries, and close
and reopen both menus. Restore the saved JAR/YAML and use the approved restart
procedure if rollback is needed. No deployment is recorded by this document.
