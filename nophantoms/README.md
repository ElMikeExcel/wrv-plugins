# NoPhantoms

**Version:** 1.0.0

Prevents phantoms from spawning, period. Cancels `CreatureSpawnEvent` for `EntityType.PHANTOM`.

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf NoPhantoms-1.0.0.jar -C classes .
```
