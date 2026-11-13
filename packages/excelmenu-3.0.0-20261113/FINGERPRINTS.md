# FINGERPRINTS — ExcelMenu 3.0.0

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity — NO Layer B build ID (stated plainly)

ExcelMenu has never carried the Layer B fingerprint scheme:
there is no BuildInfo class in the source tree and no
excel-build.txt in the shipped jar (both verified at staging,
Nov 13, 2026, against
`~/workspace/excelmenu-30-build-20261113/ExcelMenu-3.0.0.jar`
and its src/ tree). No build ID was invented for this record.
Artifact identity for 3.0.0 is instead:

- `ExcelMenu-3.0.0.jar` — 329,425 B — SHA-256
  306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51,
  MD5 21a21ac0cbb7dce0a74a7852191dcfa7 — reproduced by the
  in-tree double-build and by two clean-room builds, one of
  them from the shipped sources zip itself (BUILD-REPORT.md).
  The jar is not carried in this records package (house rule);
  it lives in the build folder and on the live server under
  the filename plugins/ExcelMenu-2.0.0.jar.
- plugin.yml (in the jar and at this package's top level):
  name ExcelMenu, version 3.0.0, main
  com.winkreyes.excelmenu.bukkit.ExcelMenu.
- Startup proof: the enable line in the server log reads
  "ExcelMenu 3.0.0 enabled (Phase 2: unified - registry,
  definitions and the /menu family in one plugin;
  ExcelBedrockMenu is retired). 93 entries, 2 registry menus,
  13 definition menus live." (rig-verified on the R2 desk rig
  and the R3 PC mirror; live boot Nov 13, 2026 — OUTCOME.txt).
- Base identity: the 2.0.1 jar this build succeeds is
  287,906 B / SHA-256
  c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a,
  itself reproduced byte-identically from its sources zip at
  this build's base gate (BUILD-REPORT.md).

## Semantic markers (Layer C — persist version to version)

Carried from 2.0.x (see the excelmenu-2.0-phase2-20261103
package's SOURCE.md): the registry dialect (entries/ +
layouts/ resolved per viewer; Java chest + Bedrock Cumulus
renderers over one model), the /menu family contract
(/menu, /servercommands alias /cmds, /serverwebsite, /tpmenu,
/childproofmenu, /excelmenu, /dm), the teleport-confirmation
gate service, the form session-token discipline (DESIGN-2.0
§2.5), the retired-definitions list, and the Excel signature
line closing plugin.yml, config.yml and the record files.

Added by 3.0.0:

- The pins dialect: a layout-level `pinned:` list (entry ids,
  duplicates refused, data-driven cap 4); pins render first in
  the Bedrock picker as EntryTarget buttons with FLAT-rule
  (group-colour) labels, are excluded from their group form,
  and render first with no duplication in the flat model; the
  Java chest model carries no pins. A pinned id with no
  registry entry logs one SEVERE at reload; the menu still
  serves.
- The rearrange dialect: per-player prefs at
  playerdata/<uuid>.yml (version 1; ids-only record; corrupt /
  wrong-type / version≠1 → no-prefs + surfaced error, never a
  failed open); OrderMerge semantics (stored order, new
  entries at end, vanished ids drop, unknown groups ignored,
  idempotent); editor doors = picker EditorTarget chrome
  button (no stats tap), the `/rearrange` player command, and
  the live-data Rearrange fixed item at server_menu page-2
  slot 17 (COMPARATOR, action `[player] rearrange` — slot 17
  is DESIGN §9 Q7's fallback, adopted as decision D35 after
  slot 8 failed the layout loader on both rigs); knobs
  rearrange.enabled / rearrange.menus (defaults on,
  [server_menu]); reset = empty prefs = the default order
  exactly, by construction.
- The skills delegation marker: with ExcelSkillsPanel present
  (plugin.yml softdepend), the `skills` entry dispatches its
  own actions and SkillsPerksScreen does not open; with it
  absent the 2.0.1 fallback surface serves unchanged.
- The magiclevel retirement marker: no bundled
  entries/magiclevel.yml (bundled census 83), no magiclevel
  registry entry live (census 93), and the typed /magiclevel
  command still answers via ExcelMagicLevel.
