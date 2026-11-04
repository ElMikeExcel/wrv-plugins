# ExcelHalloweenToggles 0.1.1 — SOURCE OF RECORD

**What:** interface-only update to the per-player Halloween scare
toggles (companion to Halloween_Softici 3.4.0). Directive of record:
Mike, Nov 1, 2026, 5:35 PM CST — the /scares interface was a chat text
list, which breaks the house style (every other surface on this server
is tappable: menus, shop) and is not tappable at all on Bedrock. 0.1.1
makes bare /scares open a tappable surface on both platforms. The
seven flags, their storage, and the toggle semantics do NOT change.

## Artifact

| File | Size | SHA-256 |
|---|---|---|
| `ExcelHalloweenToggles-0.1.1.jar` | 42,783 B | `d98ee3160f27150cae42b6096ae1a7d2829c5610855ff76fb2e4d03335bc880b` |

**Reproducibility:** `./build.sh` in this folder (offline; JDK 25 at
`~/workspace/.jdk25`, same pinned local dependency jars as 0.1.0)
rebuilds the artifact byte-identically — verified twice consecutively
at the final hash (full build + `jar-only` rebuild).

**Base proof:** before the delta, the unmodified 0.1.0 tree
(`~/workspace/halloween-toggles/src/`, built by its own `build.sh`)
was rebuilt and compared against the shipped 0.1.0 jar: **byte-identical**
(SHA-256 `3b537e87a720f915cb831befb11ad2fda7e18bebcbccd5afc68f597e992d642c`,
30,016 B), 23/23 inherited tests green. The 0.1.1 delta stands on that
proven base. The 0.1.1 source tree is `src-work/` in this folder —
the 0.1.0 tree is untouched.

## Delta scope (jar-entry proof, 0.1.0 → 0.1.1)

- **Byte-identical classes (behavior untouched):** `ToggleStore`
  (storage), `FeatureRegistry`, `FeatureDef`, `ScareGate`,
  `EventWindow`, `ScareScheduler`, `JoinPumpkinDriver`,
  `ScaresPlaceholders`. The seven flags, players.yml format, defaults
  (all ON) and the scheduler takeover are provably unchanged.
- **Changed:** `ScaresCommand` (bare /scares opens the surface;
  the flip body extracted to `flipFeature`, now the single flip path
  for command + form + GUI), `ExcelHalloweenToggles` (GUI wiring
  only), `config.yml` (four new message keys + the list-header
  wording), `plugin.yml` (version stamp, description, `Geyser-Spigot`
  added to softdepend for load order — the form helper is reflective
  and fail-soft either way).
- **Added:** `ScareView` (the pure render model both surfaces share),
  `ScaresGui` (Java chest GUI), `GeyserForms` (Bedrock form helper,
  mirrored from ExcelBedrockMenu's proven reflection helper).

## The surfaces

- **Bedrock (GeyserForms SimpleForm):** title/content from config
  (`messages.form-title` / `messages.form-content` — the live 0.1.0
  config lacks these keys and Bukkit resolves them from the jar
  default; no live config edit is needed or made). Eight buttons: one
  per scare, labeled `"<Display>: ✔ ON"` / `"<Display>: ✘ OFF"` from
  the player's CURRENT state, then `Close`. A tap flips through
  `ScaresCommand.flipFeature` (on the server thread — the Cumulus
  callback can fire off-thread) and **re-opens the form with the
  updated states**; Close or dismissing ends it. If the player is not
  a Geyser session, or the form cannot be sent, the chest GUI opens
  instead; if the GUI is somehow unavailable, the chat list prints —
  /scares can never dead-end.
- **Java (chest GUI, ExcelMenu state style):** 27-slot inventory
  (marker `InventoryHolder`, never title-matched), header-less layout
  mirroring the ExcelMenu `halloween_scares` definition: scares at
  slots 10–16 with the SAME materials (CARVED_PUMPKIN, JACK_O_LANTERN,
  ENDER_EYE, INK_SAC, LIGHTNING_ROD, CAMPFIRE, SKELETON_SKULL), the
  same name pattern (`§a§l<Name>: ✔ ON` / `§7§l<Name>: ✘ OFF`), the
  same description lore + "Click to turn it OFF/ON" line, BARRIER
  Close at slot 22. All clicks in the panel are cancelled (nothing can
  be taken or inserted); a scare click flips through the same
  `flipFeature` path and the GUI **re-renders in place**.
- **One model, two renderers:** labels, icons, lore and slot/button
  order all come from `ScareView` (pure functions of registry + state
  lookup) — the platforms cannot drift apart in wording or state
  display (house build style). Registry order = config order.
- **Fallbacks preserved verbatim:** `/scares list` prints the 0.1.0
  chat list; `/scares <feature>` flips directly with the same chat
  confirmation; `/scares test <feature>` is untouched; console still
  gets the player-only guidance (rig-verified).

## Menu dispatch — verified, no menu change needed

- **ExcelBedrockMenu 1.5.28** (`~/workspace/excelbedrockmenu-1.5.28/src/…/ExcelBedrockMenu.java`,
  case `"scares"`, line ~1222): `runGuarded(player, "scares", …)` —
  dispatches **bare `/scares`** as the player. Under 0.1.1 that lands
  on the Bedrock form directly (the button's player IS a Geyser
  session by construction).
- **ExcelMenu 2.0** `halloween_scares` definition (packet file
  `~/workspace/halloween-toggles/packet-files/halloween_scares.yml`):
  its entry opens the state-trio submenu whose buttons run
  `[player] scares <key>` — the direct-flip path, unchanged — and a
  player who instead types bare /scares now gets the GUI. No registry
  or menu file changes in this packet.

## Tests

- Unit: 30/30 green via `./build.sh` — the 23 inherited suites plus
  `ScareViewTest` (7 tests): state→label mapping, form payload shape
  (7 state buttons + Close, ids = registry order, Close/-1/out-of-range
  resolve to no feature), icon map vs the ExcelMenu definition, GUI
  model (slots 10–16, ON green/OFF gray at a glance, flip-line lore,
  slot→key round-trip), and flip round-trips through the REAL
  ToggleStore into both rebuilt payloads (the re-open/re-render
  guarantee), incl. both surfaces reading one store.
- Rig: Paper **26.3-235** boot (extracted layout borrowed from the
  WandBars 0.3.6 rig; Halloween_Softici 3.4.0 + surgery config +
  PlaceholderAPI 2.12.3): plugin list shows ExcelHalloweenToggles
  (0.1.1); awake line exact — "ExcelHalloweenToggles 0.1.1 awake:
  7 per-player toggles (6 ambient scares under scheduler takeover +
  login pumpkin); event window through 2026-11-05 (America/Chicago),
  then auto-retired."; **SEVERE 0**; no SURGERY warning; the only
  ERROR lines are the sandbox's offline noise (PlaceholderAPI
  hash-check download, PaperVersionFetcher) — none attributable to
  this plugin; console `/scares` answered with the player-only line;
  clean disable on stop. Log: `RIG-BOOT-0.1.1.log` in this folder.
- **Player proofs OWED (not claimed):** the Bedrock form tap-through
  (Mike: /menu → Halloween Scares, or type /scares — form opens, a tap
  flips and the re-opened form shows the new state, Close ends it) and
  the Java GUI click-through (Jester or Marvin: /scares opens the
  chest, a click flips and the item re-renders). The rig has no real
  client (the mineflayer protocol wall from the 0.1.0 rig report
  stands), so neither tap path is rig-provable.

## Retirement

Unchanged: the companion auto-retires after Nov 5, 2026. The Nov 6
Halloween cleanup packet removes this plugin wholesale (jar + data
folder); 0.1.1 installs UNDER the live filename
(`ExcelHalloweenToggles-0.1.0.jar`, per the install packet), so the
cleanup packet's named targets stay exact and **no extra cleanup
step is owed for the form/GUI** — the surfaces live and die inside
this one jar.
