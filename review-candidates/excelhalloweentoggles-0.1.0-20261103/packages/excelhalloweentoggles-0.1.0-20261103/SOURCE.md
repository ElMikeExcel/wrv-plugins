# ExcelHalloweenToggles 0.1.0 — SOURCE OF RECORD

**What:** per-player Halloween scare toggles, companion plugin for
Halloween_Softici 3.4.0. Design: `DESIGN.md` + `REQUIREMENTS-ADDENDUM.md`
in this folder. Decision of record: Mike, Oct 31, 2026, 5:15 PM CDT —
"Each player picks their own scares"; menu/moved-up/retirement
requirements Oct 31, 2026, 5:23 PM CDT.

## Artifact

| File | Size | SHA-256 |
|---|---|---|
| `ExcelHalloweenToggles-0.1.0.jar` | 30,016 B | `3b537e87a720f915cb831befb11ad2fda7e18bebcbccd5afc68f597e992d642c` |

**Reproducibility:** `./build.sh` (offline; JDK 25 at
`~/workspace/.jdk25`, pinned local dependency jars, jar entry dates
pinned) rebuilds the artifact byte-identically — verified twice
consecutively at the final hash. No jar ships without this source:
the full tree is `src/` in this folder.

## Dependencies (compile = provided scope; none are bundled)

| Dependency | Version / identity | Source |
|---|---|---|
| Paper API | 26.3.build.231-stable | EliteMobs rig library set (`~/workspace/elitemobs-10.9.12/rig-boot/libraries`) |
| Halloween_Softici | 3.4.0, SHA-256 `a6efe2ca…` | `~/workspace/halloween-event/halloween-plugin-3.4.0.jar` (the live jar) |
| PlaceholderAPI | 2.12.3 | the live server's copy (`~/workspace/minecraft-server/plugins/`) |
| JUnit + hamcrest (tests only) | 4.13.2 / 1.3 | `~/workspace/m2-repo` |

Runtime: `softdepend: [Halloween_Softici, PlaceholderAPI]` — the
companion self-disables if Halloween is absent, and the menu state
bridge quietly degrades if PlaceholderAPI is absent.

## Layout

- `src/main/java/com/winkreyesvalley/excelhalloweentoggles/` — 9 classes:
  `ExcelHalloweenToggles` (lifecycle, boot self-checks), `FeatureDef` +
  `FeatureRegistry` (the config-driven feature registry — modularity
  law), `ToggleStore` (players.yml, defaults all ON), `ScareGate` +
  `EventWindow` (pure decision/window logic), `ScareScheduler`
  (the takeover chains), `JoinPumpkinDriver` (login re-drive),
  `ScaresCommand` (/scares), `ScaresPlaceholders` (%halloweentoggles_*%).
- `src/main/resources/` — `plugin.yml`, `config.yml` (every
  balance-touchable value is a key — adjustability law; the feature
  list itself is data).
- `src/test/java/…` — 4 suites, 23 tests (TEST-RESULTS.md).
- `packet-files/` — the install packet's file artifacts (menu +
  Halloween config surgery, each with its base of record).
- `rig/`, `rig262/` — local test rigs + logs (RIG-REPORT.md).
- `build.sh` — the whole build + test + package path.

## Mechanism (one paragraph)

The Halloween plugin's per-player scare tasks die silently when their
`events.*.enabled` flag is false (bytecode-verified in the design
pass). The install packet's surgery sets the seven flags false; this
companion then runs replacement per-player one-shot chains that read
the same min/max from the Halloween config, compute delays through
the plugin's own `eventDelayTicks` (Witching Hour halving inherited),
re-check the plugin's `isGlobalEnabled()` + `isEligible()` plus the
player's toggle at fire time, and fire through the plugin's public
`triggerEvent` — the admin command's own path. The login pumpkin is
re-driven on join through `canReceiveJoinPumpkin` /
`applyPumpkinIfPossible` / `setLastPumpkinJoin`. Removing the
companion + restoring the seven flags returns the event to stock.

## Known limits (named, not hidden)

- The mineflayer end-to-end rig proof did not run (protocol wall —
  full account in RIG-REPORT.md); the design's §6 player proofs are
  owed post-install and are listed in the install packet.
- One deviation from DESIGN §5, deliberate and recorded: the Java
  submenu buttons use the house state-trio pattern (placeholder-driven
  ON/OFF/unavailable variants, the Aim Assist / Silk Watch pattern)
  instead of single state-blind buttons, so players can SEE their
  current state in the menu. The §5 contract underneath is unchanged:
  every button runs `/scares <feature>` as the player and the chat
  confirmation reports the new state.
- Bedrock gets no native compass button in this build: on today's
  system that is an ExcelBedrockMenu jar release, not a config change
  (menu survey correction, recorded in the design). `/scares` is full
  functional parity on Bedrock from the first minute; the native
  button arrives with ExcelMenu 2.0's single API registration.
