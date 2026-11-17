# ExcelZombieCharge 0.1.0 - BUILD RECORD

Built Oct 22, 2026 (just after midnight, CDT) from the rename
commission: ExcelProvoke 0.1.0 (LIVE since Oct 21, ~10:52 PM
CDT) renamed to ExcelZombieCharge, the name Mike picked Oct
21, 11:50 PM CDT under his new plugin naming rule (discuss
names with him before fixing them; a name must describe what
the plugin does - "hit it from range, it charges you"; the
mechanic and its design source are unchanged, see
`~/workspace/excelprovoke/` and
`~/workspace/provoke-speed/FINDINGS.md`). STAGED ONLY - not
installed on the live server.

## What changed (identity surface, the whole delta)
- plugin.yml: name ExcelProvoke -> ExcelZombieCharge; main
  com.winkreyes.excelprovoke.ExcelProvoke ->
  com.winkreyes.excelzombiecharge.ExcelZombieCharge;
  permission excelprovoke.admin -> excelzombiecharge.admin;
  description gained the rename note ("Renamed from
  ExcelProvoke 0.1.0 ...").
- Jar: ExcelZombieCharge-0.1.0.jar. Data folder:
  plugins/ExcelZombieCharge/ - it regenerates the SAME approved
  defaults (multiplier 0.5, 60 ticks, the seven victim types,
  debug off), so the rename loses nothing; the live
  plugins/ExcelProvoke/ folder holds only those defaults.
- Package com.winkreyes.excelprovoke ->
  com.winkreyes.excelzombiecharge; classes renamed
  (ExcelProvoke -> ExcelZombieCharge, ProvokeSettings ->
  ZombieChargeSettings, ProvokeListener -> ZombieChargeListener,
  ProvokeCommand -> ZombieChargeCommand); log prefix and all
  user-facing strings say ExcelZombieCharge.
- Modifier key: excelprovoke:speed -> excelzombiecharge:speed.
- KEPT on purpose: the admin command is still /provoke. It is
  the existing command name from ExcelProvoke (not a
  plugin-name reference that needed renaming), and keeping it
  preserves admin muscle memory at zero benefit to changing it.
  The rig proves it answers under the new plugin.
- ONE behavior-adjacent addition, forced by the rename (below).

## The migration duty: the legacy key
Attribute modifiers serialize with entity data; ExcelProvoke is
LIVE; after the jar swap the old plugin cannot clean up its own
leftovers. So ExcelZombieCharge strips BOTH keys wherever it
strips its own: ChargeManager's single removal helper
(apply-refresh, strip, sweep expiry, clear-on-disable) removes
excelzombiecharge:speed AND excelprovoke:speed, and the
stale-strip (EntitiesLoad + boot pass) treats an untracked
modifier under EITHER key as stale, logging legacy removals
with a distinct "stripped a LEGACY ExcelProvoke charge
modifier" line. Charges are only ever APPLIED under the new
key, and a live charge always has a tracker entry, so a legacy
modifier is never a live charge of this plugin and removing it
is always safe. The strip set lives as pure data in
core/ChargeKeys (new file) so the JVM suite can pin it.

## Unit tests (plain JVM, house Check harness)
93 checks, 0 failures (also re-run green on the clean-room
tree). The four predecessor suites (76 checks) carried over
with updated identity expectations; additions:
- ChargeKeysTest (14, NEW): the current key is
  excelzombiecharge:speed, the legacy key is excelprovoke:speed,
  **the LEGACY key is in the strip set** (the migration pin),
  the set is exactly those two keys with no duplicates, and
  isStripKey accepts both while rejecting unrelated keys in
  either namespace, vanilla keys, null and empty.
- PluginYmlGuardTest (6 -> 9): adds the main-class pin, the
  description's rename note, and a no-live-ExcelProvoke-identity
  check (no "name: ExcelProvoke", no excelprovoke.admin).
- HitClassifierTest (28), ChargeTrackerTest (21),
  ZombieChargeSettingsTest (19): unchanged logic, renamed
  imports - the full classification table, the charge book's
  scripted-tick semantics, and the settings defaults/clamps all
  re-proven against the renamed build.

## Rig proof (scratch rig, Paper 26.2 build 123 + EliteMobs
## 10.9.10 live-mirror config + Magic 11.2.5 - the full stack)
Two-boot rename rig (`scratch/run-rig.sh`, driver
ZombieChargeRigDriver, `scratch/driver/`), run once against
the shipped jar; every proof PASS:

- BOOT 1 (NO charge plugin installed): the driver planted
  ZC-LEGACY, a persistent zombie in a fenced, roofed pen,
  carrying a +0.5 MOVEMENT_SPEED modifier under the LEGACY key:
  "ZC-SEED ... value=0.3450 oldCount=1 newCount=0". The server
  then stopped cleanly, serializing the modifier into the
  zombie's saved data - the exact post-swap migration state.
- SWAP: only ExcelZombieCharge-0.1.0.jar added.
- BOOT 2: **SEVERE 0**; enable line: "ExcelZombieCharge
  enabled: charge ON, speed x1.50 (+0.5) for 60 ticks,
  refreshed per qualifying hit, victim types [the seven]";
  zero ExcelZombieCharge WARN/ERROR lines.
- LEGACY STRIP: the pen chunk was not in the initial load set
  (stage1 found=false); when the driver force-loaded it, the
  plugin's EntitiesLoad strip fired on the persisted modifier:
  "ExcelZombieCharge: stripped a LEGACY ExcelProvoke charge
  modifier (excelprovoke:speed) from ZC-LEGACY (ZOMBIE) -
  leftover from before the rename" - and stage2 read
  "found=true value=0.2300 newCount=0 oldCount=0": the legacy
  modifier survived the restart in entity data and the NEW
  plugin removed it with the old plugin nowhere on the server.
- KEPT COMMAND: `/provoke status` answered live under the new
  plugin ("charge ON, speed x1.50 for 60 ticks ... charges
  live now: 0").
- ARROW SPOT PROOF: a fresh zombie, player arrow, first try:
  "ZC-ARROW base=0.2300 value=0.3450 newCount=1 oldCount=0" -
  exactly one modifier, under the NEW key.

## Why the full 7-proof suite was not re-run
The predecessor's suite (live Magic Missile signature, arrow,
sword, witch potion, refresh-no-stack, elite native + lua
readbacks, SEVERE 0) proved BEHAVIOR, and the behavior code
here is the renamed predecessor's, byte-for-byte apart from
identity strings and the dual-key removal fence - which cannot
alter classification, charge math, or refresh semantics (the
JVM suites re-prove those against this build). What is NEW in
this build is identity and the legacy strip; those are exactly
what the rename rig proves (boot clean, strip, kept command,
one live arrow charge under the new key). Re-running all seven
scenarios would re-prove unchanged code at rig cost without
new information.

## Artifact
- `ExcelZombieCharge-0.1.0.jar` - 21,919 bytes
  SHA-256 `0a173132295e26d7012094958d4d306cd5f7ee2c62bc9332257ed4fcb1142084`
  MD5 `12ed0f4a95951a4b2c37de16b1d87747`
- `ExcelZombieCharge-0.1.0-source.zip` - SHA-256
  `1a96df95389b8d570f3ac8d8fa08db7b89f067fef082470bbae72a7150680e72`,
  MD5 `237dd5881c95877273707bea8b2e8f91`
- Rebuild reproduction: two consecutive project builds
  byte-identical; a clean-room rebuild from the source zip
  byte-identical (93/93 tests green on the clean-room tree
  too); and both identical to the jar the rename rig tested
  (the rig's own copy hashes the same). Three-way match.
- Drive deposit (Minecraft WRV folders, MD5-verified against
  local after upload): jar -> jars folder; source.zip +
  SOURCE.md -> sources folder. Drive MD5s match local exactly.
  The ExcelProvoke 0.1.0 deposit stays untouched, as history.

## Install (when Mike calls it)
Next empty-server window: remove `ExcelProvoke-0.1.0.jar` and
the stale `plugins/ExcelProvoke/` folder, drop
`ExcelZombieCharge-0.1.0.jar` into `plugins/`, restart, verify
the ExcelZombieCharge enable line. No config migration needed
(the new folder regenerates the approved defaults); any mob
still carrying a legacy modifier is stripped at boot/load by
the fence above. Rollback = remove the new jar, restore
ExcelProvoke-0.1.0.jar (and its folder), restart. Post-install
player proofs owed (one at a time): a ranged hit each on Java
and Bedrock during natural play - the same proofs the
predecessor owed; the rename changes none of them.
