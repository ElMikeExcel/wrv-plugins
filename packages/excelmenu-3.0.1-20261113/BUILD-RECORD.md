# ExcelMenu 3.0.1 — BUILD RECORD (desk build)

**Commission:** board supervisor dispatch, Nov 13, 2026 — backlog Item 11.
Mike's direct, Nov 13, 2026, ~12:27 PM CST: "Can we give this some
color again. Don't use blue for travel though since we have that for
both spawn and teleport." Palette approved by Mike ~12:38 PM CST.
Screenshot of record: `~/workspace/user/files/13985_8_0s5n.png`
(Bedrock Server Menu picker: only the "Teleport to a player" pin
blue, Travel / Me / Shops / Help plain gray).

**Desk only** — no MANIFEST edits, no panel/server contact, no live
pulls, no GitHub/Drive publication were made by this build. The
supervisor does fresh G1 pulls at filing.

## Base gate — PASSED

Base = the ExcelMenu 3.0.0 tree at
`~/workspace/excelmenu-30-build-20261113/`, copied to
`~/workspace/menu-colors-20261113/base-3.0.0/` and rebuilt untouched:

| Artifact | Size | SHA-256 |
|---|---|---|
| Rebuilt `ExcelMenu-3.0.0.jar` | 329,425 B | `306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51` |

Byte-identical to the live 3.0.0 jar (landed Nov 13, 2026, 08:03 CST).
Suite `OK (163 tests)`. Log: `base-3.0.0/base-gate-build.log`.
No 2.0.1 code was used: 3.0's rearrange/pins are intact.

## The result

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelMenu-3.0.1.jar` | 330,214 B | `5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939` |
| `ExcelMenu-3.0.1-sources.zip` (212 files) | — | `3fc9872e05b8e5d43d01461c6b0bf956b768c4bd7b7a5fc28c623673a199ea92` |

Double-build byte-identical (two consecutive `./build.sh` runs,
same hash both times; logs `packet/build.log` + `build-301-first.log`).
Plugin version: `plugin.yml` `version: 3.0.1`; `build.sh`
`VERSION="3.0.1"`. Suite `OK (169 tests)` = 163 carried + 6 new.
One javac warning, pre-existing and untouched
(`ActionDispatcher.java:109`, `Sound.valueOf` deprecation).

## Design — where the colours ride (decided at build time)

**Both entry data AND FormModel code changes** — neither alone
covers the screenshot:

- **Entry data (`config.yml`, `group-colors`):** the palette itself
  is data, per the adjustability law. The FLAT compass form and the
  old pin rule already read `group-colors`, so changing the values
  re-colours flat buttons and (before the code change below) the pins.
- **FormModel code changes** (the gap the screenshot shows): the
  GROUPED picker group buttons and the group-form entry buttons
  never read `group-colors` at all — the picker emitted the plain
  display name, the group form emitted plain stripped labels. So:
  - `FormModel.picker` now prefixes each section button with its
    `group-colors` prefix (`§` form) when a config is present;
    a null config or an uncoloured group stays plain.
  - `FormModel.group` gains a config overload; entry buttons use
    the FLAT rule (`EntryResolver.bedrockButtonLabel`) when a config
    is present. The body listing stays plain (only buttons colour).
    The Back button stays plain (navigation chrome, not a door).
  - `FormService.openGroup` passes `entries.config()` (the picker
    already did). No other callers exist.
- **Pins keep blue — new `pin-color` config knob:** with Travel
  going green, the old rule (pins = group colour) would have turned
  the Spawn/Teleport pins green too, contradicting Mike's "blue is
  [for] both spawn and teleport" and the standing "do not touch
  Spawn/Teleport pin blue". Pins now read `pin-color` (default
  `"&1"`, dark blue — the exact pre-3.0.1 rendering, preserved
  byte-for-byte in label terms: `§1Spawn`,
  `§1Teleport to a player`). `RegistryConfig` gains the `pinColor`
  component (blank/missing → `"&1"`); `ExcelMenu.readConfig`
  reads `pin-color` from config.yml (default `"&1"`); `FormModel`
  renders pins via the new `pinLabel` helper in both `flat` and
  `picker`. The colour rides as config data, not a hardcoded value
  (proven by a test that renders pins `§9` under a `pin-color:
  "&9"` config).

## Palette

| Section | Colour | Source |
|---|---|---|
| Travel | `§a` green | Mike-approved |
| Me | `§d` light purple | Mike-approved |
| Shops | `§6` gold | Mike-approved |
| Help | `§c` RED (not yellow) | Mike-approved |
| Settings | `§e` yellow | **Build pick — for Mike's veto** |
| Chest Organization | `§8` dark gray | **Build pick — for Mike's veto** |
| Spawn + Teleport pins | `§1` dark blue | Preserved (was Travel's old colour) |

Build picks (Settings, Chest Organization): the only remaining
distinct non-blue colours with usable contrast on Bedrock's
light-gray buttons. Yellow is the weakest contrast of the set
(bright yellow on light gray); the alternative would be sharing
the gray family with Chest Organization, which breaks
"each section its own distinct color". If Mike vetoes yellow,
the next candidate is `§7` gray for Settings with Chest
Organization staying `§8` — near-identical grays, not recommended.

Notes:
- The commands catalogue's groups intentionally have NO colour:
  its picker and group forms render exactly as before (proven by
  test + rig harness). Only `server_menu` sections are coloured.
- `help` was `&n` (underline, not a colour) in the old palette —
  that was never a colour at all; Help buttons were effectively
  underlined plain text. Now `§c` red.
- The bundled `src/main/resources/config.yml` was updated to the
  new palette + `pin-color` (it has no `chest_tools` line — that
  group is live-only; the staged live config carries it). The
  bundled `src/main/resources/layouts/server_menu.yml` was NOT
  touched: it is the stale FLAT pre-grouped layout and the jar
  never overwrites live data files; the live GROUPED layout
  (7,182 B / `9af8ddef…`) is unchanged by this packet.

## Java parity check — done, Java untouched (decision for veto)

Checked: on Java, entry item names already carry per-entry colours
from their entry files (e.g. Spawn `&a&lSpawn`), and the section
names render as the fixed header panes in the live layout file
(Travel `&b`, Me `&a`, Shops `&e`, Help `&6`, Settings `&7`,
Toggles `&f`, Chest Organization `&e`). Java is already coloured;
its header scheme is material-based (LIGHT_BLUE / LIME / YELLOW /
ORANGE panes) and does not map 1:1 onto the Bedrock text palette
(Java Travel is blue by pane, Bedrock Travel is green by Mike's
rule — the blue rule is Bedrock-specific, because on Bedrock blue
is the pins). **Decision: Java headers and pane materials are NOT
recoloured in this packet.** Parity here means both platforms
colour their sections, not identical codes. If Mike wants Java
headers aligned to the same palette, that is a live-data layout
edit (a follow-up packet), not a jar change. Rig evidence: both
Java chest pages build and the header names print unchanged
(`rig/harness-301.log`).

## Files changed (vs the 3.0.0 tree)

- `src/main/java/com/winkreyes/excelmenu/core/render/FormModel.java`
  — picker section colours, group-form config overload + colours,
  `pinLabel`/`groupPrefix` helpers, javadoc.
- `src/main/java/com/winkreyes/excelmenu/bukkit/FormService.java`
  — `openGroup` passes `entries.config()`.
- `src/main/java/com/winkreyes/excelmenu/core/registry/RegistryConfig.java`
  — `pinColor` component (default `&1`).
- `src/main/java/com/winkreyes/excelmenu/bukkit/ExcelMenu.java`
  — reads `pin-color` from config.yml.
- `src/main/resources/config.yml` — new palette + `pin-color: "&1"`.
- `src/main/resources/plugin.yml` — `version: 3.0.1`.
- `build.sh` — `VERSION="3.0.1"`, `SectionColorsTest` in the suite.
- `src/test/java/com/winkreyes/excelmenu/core/render/SectionColorsTest.java`
  — NEW, 6 tests (see below).
- `src/test/java/com/winkreyes/excelmenu/core/render/PinsModelTest.java`
  — one comment line only (pin labels now follow the pin-colour
  rule; assertions unchanged, still `§1Spawn` / `§1Teleport to a
  player`).

## Tests — OK (169 tests)

`OK (169 tests)` on both builds. New `SectionColorsTest` (6):

1. Picker section buttons carry the approved colours; pins stay
   `§1`; Rearrange chrome plain; no Back on the picker.
2. Every section form carries its section colour; Back plain; body
   listing plain; Travel form excludes the root-only pins.
3. Flat form: pins blue first, entries section-coloured.
4. Pin colour rides as config data (`pin-color "&9"` → `§9` pins,
   sections unchanged).
5. Commands catalogue picker + group form stay plain.
6. Null-config overloads keep the pre-3.0.1 plain shape.

Carried suites unchanged and green, including `PinsModelTest`
(pins `§1` under the old fixture palette), `FormModelTest`,
`ChestModelTest`, and both harnesses (golden 5,526 rows,
catalogue 56 rows).

## Rig proof — PASS

`rig/RigHarness301.java` drives the 3.0.1 core against `rig/seed-301`
(the post-3.0.0 live data folder + the staged 3.0.1 config), reading
the palette from the seed's own config.yml. `rig/harness-301.log`:
**12 CHECKs, 12 PASS, 0 FAIL.** Picker renders
`§1Spawn, §1Teleport to a player, §aTravel, §dMe, §6Shops, §cHelp,
§eSettings, §8Chest Organization, Rearrange Menu`; every section
form carries its colour; flat form 35 buttons pins-first blue;
commands catalogue plain; both Java chest pages build.

Paper boot (`rig/boot-301.sh`, `rig/boot-301.log`): Paper 26.3
with the 3.0.1 jar + seed-301 + live companions (Geyser,
PlaceholderAPI, Magic, Vault, mcMMO, ExcelMagicLevel,
ExcelSkillsPanel 0.2.1). **"ExcelMenu 3.0.1 enabled … 93 entries,
2 registry menus, 13 definition menus live."** Done (56.526s).
**SEVERE 0**, 0 ExcelMenu ERRORs, clean stop. (The log's ERROR/WARN
lines are sandbox-network noise — Mojang/PlaceholderAPI update
checks failing on SSL in this offline sandbox, not plugin errors.)
Console smokes: `plugins` → 8 plugins; `excelmenu list` → 13
definition menus; `excelmenu entries server_menu` from console
answers the expected console-limitation line.

## Packet (supervisor files it; the failsafe installs)

`packet/INSTALL-PACKET.md` — gated jar-swap packet: jar under the
same live filename `plugins/ExcelMenu-2.0.0.jar` + place
`packet/filing-data/config.yml` (3,215 B / `c0467b5f…`) →
`plugins/ExcelMenu/config.yml`, in-place .baks first, ONE restart.
Premise (supervisor re-verifies with fresh G1 pulls at filing;
STOP on mismatch): live jar 3.0.0, 329,425 B / `306607ba…`; live
config.yml 2,564 B / `910c2f83…` (the 3.0.0-placed file). No layout
or entries change rides in this packet. Rollback: restore the .bak
jar + .bak config, restart, verify the 3.0.0 enable line.

## Gates NOT passed here (owed, not hidden)

- **Mike's Bedrock acceptance** (player proof): the colours can
  only be truly seen in-game — picker + one section form on his
  Bedrock client, confirming Travel green / Me purple / Shops gold /
  Help red / Settings yellow / pins still blue. This lane has no
  Bedrock client; the proof is owed after install.
- **Fresh G1 live pulls at filing** (supervisor's leg): the packet
  premise hashes above are desk-side records from the 3.0.0 install
  + the staged config; the installing window re-pulls and stops on
  any mismatch.
- **Same-pass surfaces + records** (supervisor legs): Guide,
  website pair, What's New BOTH surfaces, tips check, records
  publication (G2 duplicate check + claim line, Drive deposit,
  draft PR in wrv-plugins). This build supplies the records-package
  folder (`records-package/`, no jar inside) for those legs.
