# ExcelMenu 2.0.0 — Phase 1 SOURCE.md

Artifact: `ExcelMenu-2.0.0.jar` (this folder)
SHA-256: `1419bdb9ddd4b97b32f3fef466e63c305a6c6cf94e443416c8abd513adde0fd3`
Size: 223,472 bytes. Three consecutive `./build.sh` runs byte-identical
(reproducible-build verified 2026-10-31).

## What Phase 1 is

Phase 0 shipped the platform-neutral core (entry schema, resolver,
per-platform sort) with no renderers and nothing registered but
`/excelmenu`. Phase 1 ships the PRODUCTION build in the ExcelMenu
slot (§3.3 of DESIGN-2.0.md):

- the two thin renderers (Java chest GUI + Bedrock Cumulus forms,
  including the native compass form) consuming resolver output through
  the unified bridge — renderers contain no per-plugin logic;
- the production registry: the Server Menu converted from the live
  definition, the ExcelBedrockMenu BUTTONS catalogue, and the
  commands catalogue (Amendment B) — 82 entry files;
- the 16 not-yet-converted definition menus carried over verbatim, so
  the jar is a drop-in ExcelMenu-slot replacement for 0.1.8;
- `/dm` (0.1.8's admin surface) merged with `/excelmenu` into ONE
  executor (`list | open | entries | reload`).

ExcelBedrockMenu 1.5.27 is untouched and keeps `/menu`, the compass
trigger, `/servercommands`, `/compass` and the whole /menu family
until the Phase 2 unification window (DESIGN-2.0 §3.3, Phase 2).

## Tree layout

    phase-1/
    ├── ExcelMenu-2.0.0.jar        the artifact (above hash)
    ├── build.sh                   one-command build + test (reproducible)
    ├── fixtures/                  conversion INPUTS of record:
    │     server_menu.yml            the live definition (SHA 86b51650…)
    │     bedrock-arrays.json        machine-extracted 1.5.27 arrays
    │     commands_menu.yml          preserved live catalogue p1 (e230a10b…)
    │     commands_menu_page2.yml    preserved live catalogue p2 (4fe2562f…)
    ├── tools/
    │     make_fixtures.py           server-menu converter (of record)
    │     make_catalogue.py          catalogue converter (of record)
    ├── src/main/java/com/winkreyes/excelmenu/
    │   ├── api/                     the Phase 0 public API (unchanged)
    │   ├── core/                    Phase 0 core + the 0.1.8 definition core
    │   │     (ActionParser, DefinitionLoader, MenuValidator,
    │   │     RequirementEngine, Selector, Text, …) — Bukkit-free
    │   ├── core/registry/           Phase 0 registry (unchanged)
    │   ├── core/render/             NEW Phase 1, Bukkit-free:
    │   │     LayoutDef / LayoutLoader   menu layout data (chest slots,
    │   │                                form shape, groups, pages)
    │   │     ChestModel                 layout + resolutions -> placed items
    │   │     FormModel                  layout + resolutions -> form steps
    │   │     FormSession                form session lifecycle (§2.5)
    │   └── bukkit/
    │         PlatformBridge           Phase 0 detection + the Cumulus
    │                                  send surface (transport only)
    │         BedrockMenus             0.1.8 definition forms on the bridge
    │         RegistryMenus            registry chest renderer + clicks
    │         FormService              registry form renderer + session store
    │         MenuService / MenuListener / ActionDispatcher /
    │         BukkitContext / …        the 0.1.8 definition engine, ported
    │         StatsSink                ExcelMenuStats tap forwarder (§1.3)
    │         MenuCommands             merged /excelmenu + /dm executor
    │         ExcelMenu                plugin main (Excel signature)
    ├── src/main/resources/
    │   ├── plugin.yml                 commands: excelmenu, dm ONLY
    │   ├── config.yml                 registry/form settings (adjustable)
    │   ├── entries/                   82 production entry files
    │   ├── layouts/                   server_menu.yml, commands_menu.yml
    │   └── menus/                     the 16 unconverted definitions
    ├── src/test/java/…                81 tests (see TEST-RESULTS.md)
    └── rig/                           Paper 26.3-231 + this jar +
                                       ExcelBedrockMenu 1.5.27 + Geyser
                                       b1277 + PAPI; boot.log is the
                                       final-jar run.

## Ownership split

- Phase 0 core (api/, core/platform/, core/registry/, EntryService,
  BukkitViewerContext, harness): carried from `~/workspace/menu-2.0/`
  unchanged except Placeholders (verbatim 0.1.8, as Phase 0 left it).
- Definition engine (core definition classes, MenuService and friends):
  ported from the 0.1.8 candidate source
  (`~/workspace/moon-packets-audit/inspect/menu018/candidate-source/`)
  with three recorded changes: the dispatcher takes the bridge's
  detection (one platform source), the validator accepts registry
  menu ids as known `[openguimenu]` targets, and definition loading
  can skip registry-owned/retired files loudly.
- Render models, layouts, converters, RegistryMenus, FormService,
  StatsSink, MenuCommands: new Phase 1 code (this build).
- Upstream at runtime (NOT bundled): Paper API, PlaceholderAPI,
  Vault (provided scope), Cumulus/Geyser/Floodgate classes reached
  reflectively through the bridge — the plugin loads and every Java
  surface works with Geyser absent.

## Mechanism (one paragraph)

At enable, entries load into the Phase 0 registry and layouts into
the layout registry; definition files whose stem is registry-owned
(server_menu, commands_menu) or retired (commands_menu_page2, a
config list) are skipped with a log line, the other 16 definitions
load as before. A registry menu renders from the SAME resolver
output on both platforms: ChestModel places entries into layout
slots for Java; FormModel builds flat or grouped form steps for
Bedrock through the bridge's Cumulus surface, with form sessions
bound to player + menu + group + world and a 300 s TTL (config
`forms.session-ttl-seconds`). Clicks/taps re-resolve before
dispatch — a door whose doorplate stopped being true explains
itself instead of firing a stale action. Stats taps forward to
ExcelMenuStats by entry/item id, fail-soft (§1.3: ClickStats keyed
on entry id; the unify-on-entry-id absorb path stays open).

## Known limits (honest)

- The conversion sources are the newest local copies of the live
  definition files; the install packet gates on LIVE hashes pulled at
  install time and stops on drift.
- Registry chest clicks and form rendering are proven at model level
  and by the rig boot; in-game rendering is player-proof (packet §6).
- Phase 2 work is NOT in this jar: /menu transfer, date-window and
  confirm screens, the code gate, ExcelBedrockMenu retirement.
