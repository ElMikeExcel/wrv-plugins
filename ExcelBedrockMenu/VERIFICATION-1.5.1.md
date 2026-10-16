# ExcelBedrockMenu 1.5.1 — Source Reconstruction Verification

**Result: MATCH — the reconstructed source in this folder compiles and
packages to the exact live 1.5.1 jar, byte for byte.**

## Hashes

- **Target SHA-256** (live jar,
  `~/workspace/excelbedrockmenu-guild/install-prep/ExcelBedrockMenu-1.5.1.jar`):
  `0242d830d29d38d93cf320102582bd6ddd89cea8a5284e6013bfa38c70855c2e`
- **Produced SHA-256** (built from the source in this folder):
  `0242d830d29d38d93cf320102582bd6ddd89cea8a5284e6013bfa38c70855c2e`
- Two independent clean from-source builds produced that SHA.
- `cmp` of the produced jar against the live jar: **byte-identical**
  (not merely CRC-identical entries — the whole file matches).
- Per-entry CRC comparison (all 10 entries, entry order also identical):
  `META-INF/MANIFEST.MF`, `ExcelBedrockMenu.class`, `GeyserForms.class`,
  `TeleportMenu.class`, `TeleportMenu$PickHolder.class`, `plugin.yml`,
  and all directory entries — **every entry SAME**.
- `javap -p -c -constants` on the produced `ExcelBedrockMenu.class` vs
  the class extracted from the live jar: **identical output**.

## How the reconstruction was made

Copied from `~/workspace/renames-2026-10-15/src/ExcelBedrockMenu` (which
had advanced to 1.5.3) and removed exactly the documented 1.5.3 surface
(per `~/workspace/wrv-055/PLUGIN-RECORD-DRAFT.md` §§2, 11 and
`~/workspace/excelguildgate/install-prep/INSTALL-PACKET.md`):

- `ExcelBedrockMenu.java`
  - `BUTTONS[]`: removed `"Road to the Guild"` — "Adventurers Guild"
    back at index 3 (9 buttons).
  - `COMMANDS[]`: removed `"seals"` — `"ag"` back at index 3.
  - `openMenu`: passes `BUTTONS` directly again (the 1.5.3 label-clone /
    lock-prefix block removed). Confirmed against the live jar before
    building: `javap` of the real 1.5.1 class shows `getstatic BUTTONS`
    fed straight into `GeyserForms.sendSimpleForm`, and no lock helper.
  - Removed the `isGuildLocked()` reflection helper and its javadoc.
  - Removed the v1.5.3 javadoc paragraph (the v1.5.1 note stays).
  - `GeyserForms.java` and `TeleportMenu.java` untouched — their class
    CRCs in the live 1.5.1 jar already matched the 1.5.3 tree, and the
    rebuild confirms it.
- `plugin.yml`: `version: 1.5.3` → `1.5.1` (the only line that differs
  from the current tree; read-back from the live jar agrees).
- `build.sh` (per-plugin): output name back to
  `ExcelBedrockMenu-1.5.1.jar`; header comment drops the 1.5.3 lines.
  Note: `build.sh` is not packaged into the jar, so the hash proof
  covers the `.java` files and `plugin.yml`; the `build.sh` wording is
  reconstructed by reversing the 1.5.3 header edit in the same style.

## Build commands used (toolchain of record)

JDK 25 at `~/workspace/.jdk25` (Temurin 25.0.4.1 — matches the live
jar's manifest `Created-By`). Classpath identical to the renames
top-level harness `~/workspace/renames-2026-10-15/build.sh` (ExcelShops
`build-libs` Paper API + runtime libraries listed there), `LANG=C.UTF-8`:

```bash
export JAVA_HOME=~/workspace/.jdk25
# CP built exactly as in renames-2026-10-15/build.sh:
#   $LIBS/paper-api.jar + adventure-*/examination-*/option/gson/guava/snakeyaml
#   from ~/workspace/excelshops/build-libs, plus joml, json-simple, fastutil,
#   brigadier, bungeecord-chat, jspecify, slf4j-api, failureaccess, log4j-api
#   from ~/workspace/excelbuildmode-pilot/runtime-server/libraries
find src -name '*.java' > sources.txt
$JAVA_HOME/bin/javac --release 21 -proc:none -nowarn -cp "$CP" -d classes @sources.txt
cp plugin.yml classes/
(cd classes && $JAVA_HOME/bin/jar --date=2026-10-16T00:00:00-05:00 \
    --create --file ExcelBedrockMenu-1.5.1.jar .)
sha256sum ExcelBedrockMenu-1.5.1.jar
# 0242d830d29d38d93cf320102582bd6ddd89cea8a5284e6013bfa38c70855c2e
```

Compile output: clean, only the pre-existing TeleportMenu deprecation
note (same as the 1.5.0/1.5.1 builds of record). The `--date` pin is
what fixes the zip entry timestamps (2026-10-16T00:00:00-05:00, stored
as 05:00 UTC in the jar); without it, entry contents/CRCs are identical
and only timestamps vary — same as documented in the 1.5.1 install
packet. (This JDK's `jar` tool wants the long-form `--create --file`
spelling alongside `--date`; the short `cf` form after `--date` was
rejected. Semantics are unchanged.)

Local only: no GitHub writes, no server/panel access were used.
