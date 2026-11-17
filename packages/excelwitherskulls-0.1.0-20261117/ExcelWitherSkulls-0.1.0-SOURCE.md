# ExcelWitherSkulls 0.1.0 — SOURCE

## What this is
Our own plugin, written from scratch for Wink Reyes Valley (no
upstream). The skull piece of the trimmed rewards package Mike
approved Oct 21, 2026 (Jester's elite-rewards feedback; audit at
`~/workspace/elitemobs-feedback-2026-10-21/FEEDBACK-REVIEW.md`
Part 2): an ELITE wither skeleton that dies to a player kill
gets its skull chance topped up toward a configured target
(default 25%). Vanilla already rolls 2.5% + 1% per Looting
level; when that roll leaves no skull in the drops, this plugin
rolls the difference once and adds exactly one skull. Design:
`~/workspace/excelwitherskulls/SPEC.md`; verification:
`~/workspace/excelwitherskulls/BUILD-RECORD.md`.

## Artifact
- Jar: `ExcelWitherSkulls-0.1.0.jar` — 15,034 bytes
- SHA-256: `6797ce0dce6afcc451d486d1907b9d47171f5d83f1abce3063057bc243b56d9a`
- Rebuild reproduction: consecutive `build.sh` runs
  byte-identical, AND a clean-room rebuild from this source zip
  byte-identical, AND identical to the jar the scratch rig
  tested (three-way match, 2026-10-21; see BUILD-RECORD.md).
- This source zip: `ExcelWitherSkulls-0.1.0-source.zip`
  SHA-256: `3059de776e594b018e20623d2fc9dc6df6748aa9e6cc6be12cbe09c8ba2a2270`

## Source layout
- `core/SkullMath.java` — the probability math, pure JVM
  (vanilla table, difference formula, realized totals, roll
  boundary). No Bukkit imports.
- `core/TopUpDecider.java` — the whole drop decision as one
  verdict table (ADD_SKULL / DISABLED / NOT_WITHER_SKELETON /
  NOT_ELITE / NOT_PLAYER_KILL / SKULL_ALREADY_PRESENT /
  TARGET_NOT_ABOVE_VANILLA / ROLL_MISS), pure JVM.
- `core/SkullSettings.java` — the immutable config snapshot;
  values clamp, never throw (reload safety).
- `EmGateway.java` — the EliteMobs read seam: reflective
  `PersistentTagger.isEliteEntity / getEliteEntity` resolved
  against EM's classloader, cached, fails safe to "not elite"
  and never throws into a handler. The ExcelRaidBuff pattern.
- `SkullListener.java` — the one Bukkit hook: EntityDeathEvent
  at HIGHEST (final vanilla drops list; EM's own loot pass runs
  on its EliteMobDeathEvent and cannot clear natural-elite
  vanilla drops — bytecode-verified in the audit). Only write:
  append at most one skull.
- `WitherSkullsCommand.java` — `/witherskulls status|reload`
  (perm `excelwitherskulls.admin`, default op).
- `ExcelWitherSkulls.java` — main class; volatile settings
  snapshot, fail-safe reload, boot-time seam probe.
- `tests/` — 4 plain-JVM suites, 65 checks, all passing
  (SkullMath, TopUpDecider incl. every spec edge case,
  SkullSettings, PluginYmlGuard).

## Build dependencies (compile-time only)
House classpath (excelshops/build-libs Paper API + adventure
4.20 set + the runtime-server libraries + JetBrains annotations
from the workspace .m2), JDK 25, `--release 21`. No runtime
dependencies beyond Paper; EliteMobs is a softdepend and is
only ever read reflectively at runtime.

## Data
None. The plugin writes no files of its own; its config is the
standard `plugins/ExcelWitherSkulls/config.yml`, read at enable
and on `/witherskulls reload`. Uninstall = remove the jar.
