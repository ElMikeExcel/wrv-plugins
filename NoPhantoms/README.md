# ExcelNoPhantoms (NoPhantoms)

**Created by us (custom add-on).** This is a documentation label; the runtime plugin name remains `NoPhantoms`. Recorded installed version: **1.0.0**; this branch's source version: **1.0.0**. The source/build details below describe this branch, not proof of the installed JAR's behavior. See the [plugin inventory](../PLUGIN-INVENTORY.md).

Version: **1.0.0**. Cancels phantom `CreatureSpawnEvent`s; other creatures are unaffected. No commands or configuration.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl NoPhantoms -am verify
```

Output: `NoPhantoms/target/NoPhantoms-1.0.0.jar`. The Paper API is provided by the server and is not bundled.

## Source recovery

Readable source and `plugin.yml` were restored without behavioral changes from historical commit [`113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/nophantoms), using the canonical `NoPhantoms/` folder. That history is the source provenance; it does not establish byte-for-byte equality with an installed JAR.
