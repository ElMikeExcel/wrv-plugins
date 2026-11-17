# ExcelZombieCharge 0.1.0 - SOURCE

## What this is
Our own plugin, written from scratch for Wink Reyes Valley (no
upstream). **This release is ExcelProvoke 0.1.0 RENAMED** - Mike
picked the name "ExcelZombieCharge" Oct 21, 2026, under his
plugin naming rule (names are discussed with him before they
are fixed, and a name must describe what the plugin does:
"ExcelProvoke" read like a taunt ability - EliteMobs literally
has a Provoke power - while the mechanic is "hit it from range,
it charges you"). Identity changes only; the behavior code is
the predecessor's, untouched. It SUPERSEDES ExcelProvoke 0.1.0
by rename; the ExcelProvoke deposit stays in Drive as history.

The mechanic: the anti-kiting charge Mike approved Oct 21,
2026. When a zombie-family mob or a piglin takes a ranged hit
from a player - a projectile, or a Magic spell carrying the
verified blank-potion signature - it gains +50% movement speed
for 3 seconds, refreshed per qualifying hit, never stacking.
Melee never triggers it. Design source (verified from the Magic
11.2.5 jar and EliteMobs 10.9.10 movement code):
`~/workspace/provoke-speed/FINDINGS.md`; design:
`~/workspace/excelzombiecharge/SPEC.md`; verification:
`~/workspace/excelzombiecharge/BUILD-RECORD.md` (including the
rename rig and the legacy-key strip proof).

## Identity changes vs ExcelProvoke 0.1.0 (the whole delta)
- plugin.yml: name ExcelZombieCharge, main
  com.winkreyes.excelzombiecharge.ExcelZombieCharge, permission
  excelzombiecharge.admin; description carries the rename note.
- Jar ExcelZombieCharge-0.1.0.jar; data folder
  plugins/ExcelZombieCharge/ (same approved defaults, so the
  renamed plugin regenerating its config there is lossless).
- Package/classes renamed (ProvokeSettings ->
  ZombieChargeSettings, etc.); log prefix ExcelZombieCharge.
- Modifier key excelprovoke:speed -> excelzombiecharge:speed.
- KEPT on purpose: the admin command is still /provoke (the
  existing command name; admin muscle memory - noted in the
  build record).
- ONE behavior-adjacent addition, required by the rename: the
  cleanup strips the LEGACY key (excelprovoke:speed) alongside
  the current one. Attribute modifiers serialize with entity
  data, ExcelProvoke is live, and after the swap the old plugin
  cannot clean up its own leftover modifiers. The strip set is
  pinned in core/ChargeKeys and proved by JVM test + rig.

## Artifact
- Jar: `ExcelZombieCharge-0.1.0.jar` - 21,919 bytes
- SHA-256: `0a173132295e26d7012094958d4d306cd5f7ee2c62bc9332257ed4fcb1142084`
- MD5: `12ed0f4a95951a4b2c37de16b1d87747`
- Rebuild reproduction: consecutive `build.sh` runs
  byte-identical, AND a clean-room rebuild from this source zip
  byte-identical, AND identical to the jar the rename rig
  tested (three-way match, 2026-10-22; see BUILD-RECORD.md).
- This source zip: `ExcelZombieCharge-0.1.0-source.zip`
  SHA-256: `1a96df95389b8d570f3ac8d8fa08db7b89f067fef082470bbae72a7150680e72`
  MD5: `237dd5881c95877273707bea8b2e8f91`

## Source layout
- `core/HitClassifier.java` - the hit classification table as
  one verdict function (QUALIFY_PROJECTILE / QUALIFY_MAGIC /
  NOT_VICTIM_TYPE / PROJECTILE_NOT_PLAYER /
  MAGIC_SIGNATURE_MISMATCH / MAGIC_NOT_POTION_DAMAGER /
  MAGIC_POTION_NOT_BLANK / MAGIC_NO_PLAYER_CASTER /
  NOT_RANGED), pure JVM. Unchanged from ExcelProvoke.
- `core/ChargeTracker.java` - the charge book (UUID -> expiry
  tick) with caller-supplied ticks. Unchanged.
- `core/ZombieChargeSettings.java` - the immutable config
  snapshot (renamed from ProvokeSettings); values clamp, never
  throw (reload safety).
- `core/ChargeKeys.java` - NEW for the rename: the modifier key
  strings as pure data - current key, LEGACY key, and the strip
  set containing both, pinned by ChargeKeysTest.
- `ZombieChargeListener.java` - the Bukkit hooks: the damage
  listener at MONITOR extracting the event shape for the
  classifier (incl. the potion item's instant-damage check over
  base type + custom effects), plus death/remove/unload/load
  cleanup hooks.
- `ChargeManager.java` - every attribute write: remove-then-add
  under the fixed key `excelzombiecharge:speed`
  (MULTIPLY_SCALAR_1); every removal also removes the legacy
  `excelprovoke:speed`; the 10-tick sweep; the stale-modifier
  strip for load/boot/disable with a distinct LEGACY log line.
- `ZombieChargeCommand.java` - `/provoke status|reload` (perm
  `excelzombiecharge.admin`, default op). Command name kept
  from ExcelProvoke on purpose.
- `ExcelZombieCharge.java` - main class; volatile settings
  snapshot, fail-safe reload, sweep scheduling, the boot
  stale-strip pass (both keys), boot line.
- `tests/` - 5 plain-JVM suites, 93 checks, all passing
  (HitClassifier full table incl. every exclusion,
  ChargeTracker scripted-tick semantics, ZombieChargeSettings,
  ChargeKeys strip-set incl. the legacy-key pin, PluginYmlGuard:
  no depend lines at all + identity fields pinned).

## Build dependencies (compile-time only)
House classpath (excelshops/build-libs Paper API + adventure
4.20 set + the runtime-server libraries + JetBrains annotations
from the workspace .m2), JDK 25, `--release 21`. No runtime
dependencies beyond Paper; Magic and EliteMobs are deliberately
NOT dependencies in any form - detection is by damage-event
signature and the effect is a vanilla attribute modifier.

## Data
None of its own. The plugin's config is the standard
`plugins/ExcelZombieCharge/config.yml`, read at enable and on
`/provoke reload`. The only footprint is the transient
attribute modifier itself, fenced by expiry/death/remove/
unload/load/disable cleanup (SPEC "Cleanup") - plus, during
the migration, removal of the predecessor's legacy modifiers.
Uninstall = remove the jar.
