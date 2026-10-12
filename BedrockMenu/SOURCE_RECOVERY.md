# Source recovery and verification

## Provenance

Readable Java source was recovered from `ElMikeExcel/wrv-plugins` commit
[`113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/bedrockmenu),
under the earlier lowercase `bedrockmenu/` directory. Retrieval verified every
Git blob hash in that snapshot. The active source now lives only in canonical
`BedrockMenu/src/`; lowercase duplicates were not recreated.

SHA-256 hashes of the original recovered source files:

| File | SHA-256 |
| --- | --- |
| `BedrockMenu.java` | `5add7c3a2e2d1d92b4b30c89091db38f5c70385d0a8bffcf038481312b628b0a` |
| `GeyserForms.java` | `337179a00dfa1acb7ce7a6b729971816ae3ff9176ea186b4efcf4a64f9c3ce36` |
| `TeleportMenu.java` | `f16009ae46e3d54760f2ac0b8dc7f394eaf75fd03e2506dfcc58ed30dc7ee360` |

The active `GeyserForms.java` and `TeleportMenu.java` still match those hashes
exactly. `BedrockMenu.java` intentionally differs for the 1.4.2 menu addition and
1.4.3 repairs; `SpawnLocations.java` is new.

## Comparison performed on 2026-10-12

1. Inspected the original readable source against the checked-in CFR decompile.
   Methods, behavior, strings, coordinates, and menu order agree. The readable
   original avoids CFR artifacts such as `List<CallSite>` used for item lore and
   variables inferred as `Plugin` where the real types are `World`/`Location`.
2. Compiled the **unmodified recovered 1.4.1 source** with Temurin JDK 21.0.12.1,
   `--release 21`, against the shared Maven dependency classpath.
3. Compared its four class files with `classes-1.4.1/` using
   `tools/compare-recovered-classes.py`. This compares `javap` method signatures,
   descriptors, constant fields, instructions and exception tables, plus
   bootstrap method arguments (including lambda targets and concatenation
   recipes). It normalizes constant-pool indexes and javac's generated lambda
   numeric suffixes only. Lambda descriptors remain distinct.
4. **All four normalized disassemblies matched:** `BedrockMenu`, `GeyserForms`,
   `TeleportMenu`, and `TeleportMenu$PickHolder`.
5. Verified the active Spawn Shop button and command are the same ordered entry
   as the historical `tools/Patch.java`: `Spawn Shop` -> `shop`.
6. Ran the historical reconstruction script with its default pinned Maven/ASM
   resolution. Inspected the resulting JAR: exactly four version-65 class files,
   the byte-identical original `tools/plugin-1.4.2.yml` descriptor, and the exact
   expected eight button/command pairs including `Spawn Shop` -> `shop`. The
   missing `ASM` override path also fails early with a readable error.

This demonstrates the recovered starting source reproduces the compared
executable instructions, API signatures, constants, and bootstrap arguments.
It is **not a byte-for-byte class/JAR identity claim**, a comparison of every
class-file attribute, or proof of runtime integration. The archived classes are
class-file version 69 (Java 25); the rebuilt source targets version 65 (Java 21).
Debug metadata, lambda names, and archive metadata can differ. The repaired
1.4.3 source deliberately changes behavior and is not expected to match 1.4.1.

To repeat the comparison, extract the historical lowercase source from the
linked commit, compile it into a separate class directory with the project's
Paper dependency classpath, then run:

```bash
python3 BedrockMenu/tools/compare-recovered-classes.py --baseline-classes /path/to/compiled-original-1.4.1
```

Do not use the active repaired 1.4.3 output for that comparison.

## Grounds for spawn corrections

The EssentialsX module exposes
[`EssentialsSpawn.getSpawn(String group)`](https://github.com/EssentialsX/Essentials/blob/2.x/EssentialsSpawn/src/main/java/com/earth2me/essentials/spawn/EssentialsSpawn.java).
Its [spawn storage](https://github.com/EssentialsX/Essentials/blob/2.x/EssentialsSpawn/src/main/java/com/earth2me/essentials/spawn/SpawnStorage.java)
uses `default` when appropriate. These upstream sources were inspected on
2026-10-12. The earlier code instead tried `Essentials.getInstance().getSpawn()`.

The old fallback explicitly constructed the village location in
`player.getWorld()`, so opening the menu in the Nether or End applied the
village coordinates in the wrong dimension. The new fallback selects a loaded
NORMAL overworld, optionally by explicit configured name, and fails closed if
none matches. It keeps the original village x/z and surface-height behavior.

## Verification limits and staging checklist

Focused Maven `verify` passed on 2026-10-12: 11 tests, zero failures, errors, or
skips, and `target/BedrockMenu-1.4.3.jar` produced.

Unit tests exercise the reflection entry point with an API-shaped mock, missing
and disabled EssentialsSpawn, null locations/worlds, overworld selection,
explicit configuration, original coordinates, and world-height limits. The
shared Maven build compiles and packages the actual source and resources.

Before deployment, test on a staging Paper server with the server's actual
Geyser, Essentials, and DeluxeMenus versions:

- Java chest fallback and Bedrock forms, including closed forms.
- Spawn from Overworld, Nether, and End; configured Essentials default spawn;
  missing EssentialsSpawn; optional explicit fallback world; cancelled teleports.
- Spawn Shop opens the existing `/shop` menu for a normal-permission player.
- Teleport picker, acceptance/decline, logout, and expired requests.

The restored TeleportMenu logic retains its existing command-dispatch behavior.
A handled `/tpa` command is not proof that Essentials accepted a new request, so
a cooldown/permission rejection may still be followed by its optional prompt.
Essentials remains authoritative when `/tpaccept` or `/tpdeny` is executed. A
pending-request API rewrite and Java picker pagination beyond 54 players are
outside this source-recovery repair and were not silently introduced.
