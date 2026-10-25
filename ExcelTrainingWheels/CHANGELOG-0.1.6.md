# ExcelTrainingWheels 0.1.6: seal ladder, live divisor, Bedrock grace (STAGED, not live)

Built Sun Oct 25, 2026, about 12:50-12:55 PM CDT on the box. Base: the 0.1.5 tree
(live jar fbe4b1e8...; the box toolchain rebuilds that jar byte-for-byte, re-checked today).
Deterministic: two clean builds give the same sha (see SHA256SUMS.txt).

## 1. Seal ladder table (the Elite Seal Ladder, approved Oct 25, 2026)
- CompensationTable.DEFAULT_DEALT and the bundled config table are now
  0:1.60, 1:1.75, 2:1.90, 3:2.05, 4:2.20, 5:2.20 (was 1.50 .. 2.25).
  Seal 4 is the strength peak; Seal 5 is spawn-only, so it repeats 2.20.

## 2. Live divisor (the shield follows what EliteMobs actually applied)
- New config key shield.compensation.source: live | table (missing = live).
- live: each elite hit on a protected player is divided by the multiplier EliteMobs
  10.9.10 used for that hit, read live by reflection (MobCombatSettingsConfig statics), with
  the same classification as EliteMobs' own PDBEFilter (line 110):
  - natural elites with scaled combat -> scaledDamageToPlayerMultiplier (the ladder);
  - custom bosses with normalized combat (dungeon / instanced / regional bosses) ->
    normalizedDamageToPlayerMultiplier, read live (not a hardcoded value);
  - any other custom boss -> damageToPlayerMultiplier.
- Result: a protected player takes base x the mob's UNSCALED damage on every path.
  Before 0.1.6 a normalized boss was divided by the ladder value, so with normalized
  at 1.0 (today) the shield over-protected (0.25/1.75 = 0.143 instead of 0.25).
- Fallbacks never guess weaker than 0.1.5: an unreadable or out-of-range live value
  (EliteMobs reports 0.0 until its configs load, and briefly during /em reload) or an
  unclassified elite uses the seal table, exactly the 0.1.5 divisor. The divisor never
  drops below 1.0, so the shield is never weaker than the flat base factor.
- Table sync: on every seal refresh (join, every 30 s, /tw reload) TW compares the table row
  for the current seal with the live scaledDamageToPlayerMultiplier and logs one WARNING per
  change when they differ. With source: live the shield already follows EliteMobs; the
  warning means the fallback table needs the same edit.
- Boot line "shield compensation divisor source: LIVE EliteMobs - live scaled x.., normalized
  x.., other custom bosses x..". /tw reload shows "divisor LIVE scaled x.. / normalized x.. /
  other x..". The debug SHIELD line now shows the path and divisor (e.g. "elite true SCALED /1.75").
- excluded-worlds still honored (flat base factor there). With source: live the EliteMobs
  worlds do not need to be listed.

## 3. Bedrock loading-screen grace window (new, approved Oct 25, 2026, 12:47 PM CDT)
- Fixes the Bedrock loading-screen hit: Bedrock players are in the world while still on
  a loading screen after join, respawn or a world change, and can be hit before they see.
- Who: Floodgate (Bedrock) players only. Detection: FloodgateApi.isFloodgatePlayer(uuid)
  by reflection; if Floodgate is absent, Floodgate's UUID scheme (most-significant bits 0).
  Java players are never affected. plugin.yml softdepends on floodgate.
- When: PlayerJoinEvent, PlayerRespawnEvent, PlayerChangedWorldEvent, and cross-world
  PlayerTeleportEvent. A new trigger restarts the window.
- What: for bedrock-grace.seconds (default 4, clamp 0..30) incoming damage is cancelled at
  HIGHEST priority (cancel only, never un-cancel). VOID, KILL and SUICIDE always apply;
  LAVA applies unless bedrock-grace.allow-lava is false. Mobs within 32 blocks that target
  the player drop the target, and EntityTargetLivingEntityEvent onto the player is cancelled
  for the window.
- Ends early when the player hits an entity (melee or own projectile) or breaks a block;
  cleared on quit.
- Optional grey action bar "Loading in... protected" (bedrock-grace.show-actionbar,
  bedrock-grace.actionbar-message).
- Shield / lifesteal: untouched. The shield (HIGH, ignoreCancelled) only scales damage and
  keeps no state; a cancelled hit simply never lands. Lifesteal fires on kills only, and a
  kill means the player attacked, which already ended the window.
- Boot line "bedrock loading-screen grace: ON (4s ..., detection Floodgate API)".
  Debug lines "BEDROCK GRACE <player>: ..." (console only).

## New core classes (pure, unit-tested)
- core/CombatDivisor (path -> divisor, fallbacks, floor 1.0, table-sync check).
- core/BedrockGrace (window clock, early end, allowed causes, Floodgate UUID scheme).

## Tests
run-tests.sh: 353 passed, 0 failed (CombatDivisorTest and BedrockGraceTest new;
CompensationTableTest keeps the old v4 ladder as a math fixture and asserts the new defaults).

## Scratch boot (box rig /workspace/tw016-rig, Paper 26.3 + EliteMobs 10.9.10 + floodgate)
- Clean enable, no exceptions. Floodgate API bound.
- With the S1 MobCombatSettings file: /tw reload shows "divisor LIVE scaled x1.75 /
  normalized x1.00 / other x1.00".
- Swapped to the S5 recommended file + "em reload" + "/tw reload": "scaled x2.20 /
  normalized x2.00". Swapped back to S1 + em reload: back to 1.75 / 1.00. So TW follows
  EliteMobs live across /em reload with no TW restart.
- Not exercised on the rig: an actual Bedrock client join (no Bedrock client on the box).
  The window logic is unit-tested; the event glue is plain Bukkit API.

## Files changed vs 0.1.5 (diff-0.1.5-to-0.1.6.patch)
build.sh (name), plugin.yml (0.1.6, softdepend floodgate), config.yml (source key, ladder
table, bedrock-grace section), ExcelTrainingWheels.java (seam + grace glue),
core/CompensationTable.java (defaults), core/CombatDivisor.java (new),
core/BedrockGrace.java (new), tests (2 new suites, 1 updated, TestMain).
