# SOURCE — ExcelBedrockMenu 1.5.28 (Halloween Scares button)

Built Nov 1, 2026 (Iris, local work only — no live-server contact, nothing installed).
Closes the gap Mike caught Nov 1, 2026: his Oct 31 requirement
(`~/workspace/halloween-toggles/REQUIREMENTS-ADDENDUM.md`) put the /scares
toggles in the Server Menu on BOTH platforms, moved up, retiring after
Nov 5. The Java side landed (ExcelMenu 2.0 registry entry
`halloween_scares`, Help group, label "Halloween Scares — your switches",
CARVED_PUMPKIN icon, visible 2026-10-31 → 2026-11-05), but `/menu` and the
compass still belong to ExcelBedrockMenu 1.5.27 (Menu 2.0 Phase 1
coexistence contract; the transfer is Phase 2), and 1.5.27's hardcoded
button catalog had NO scares button — a Bedrock player opening the menu
found nothing.

## Source recovery — where the live 1.5.27 tree actually was

The workspace's newest standalone tree was 1.5.14
(`~/workspace/excelbedrockmenu-1.5.14`). The 1.5.27 tree was recovered from
the Aim Assist trio build's working folder:
`~/workspace/aimassist-toggle/work/bedrockmenu-1.5.27/` — the tree Iris
built 1.5.27 from on Oct 30, 2026 (itself a delta on Moon's 1.5.26 packet
source, `~/workspace/moon-packets-audit/inspect/bedrock1526/`). No GitHub
or Drive recovery was needed.

## Base proof — HARD GATE, PASS (byte-identical)

The unmodified recovered tree, built with the packet `build-offline.sh`
(Temurin 25.0.4.1 at `~/workspace/.jdk25`, Paper libraries
`~/workspace/paper231-rerun/bootstrap/libraries`, jar date pinned
2026-10-30T00:00:00Z), reproduces the live 1.5.27 jar **byte-identically**:

- Rebuild SHA-256: `a6e445e11a027adee5f0357130fa6ded77064fec69ef6ab38f62b9d2461ad529`
- Live jar of record (`~/workspace/aimassist-toggle/ExcelBedrockMenu-1.5.27.jar`,
  installed Oct 30, 2026): same SHA-256, 41,741 B. **Exact match.**

Toolchain note: a first rebuild attempt with Temurin 25.0.5+8 produced a
jar differing ONLY in `META-INF/MANIFEST.MF` (`Created-By: 25.0.5` vs the
shipped `25.0.4.1`); every class was already byte-identical. The delta was
applied only after the 25.0.4.1 rebuild matched exactly.

## The delta (exactly one button)

All changes are in `src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java`
plus the `plugin.yml` version stamp:

1. `BUTTONS`: `"Halloween Scares \u2014 your switches"` inserted after
   `"Ask the Guide"` (index 19 of 26). Label matches the Java registry
   entry verbatim; stored plain, Help group color added at send time.
2. `COMMANDS`: `"scares"` inserted at the parallel index.
3. `groupColor`: `"scares"` added to the `COLOR_HELP` row (§n copper,
   the Help group color).
4. `runAction`: `case "scares"` → `runGuarded(player, "scares", …)` —
   the same guarded dispatch the Lost & Found / Trash / Cosmetics /
   Maker's mark buttons use: missing command → plain unavailable line;
   permission failure → plain denied line; otherwise
   `player.performCommand("scares")`. Bare `/scares`
   (ExcelHalloweenToggles) lists the player's seven scare switches —
   the both-platforms floor its design specifies. No teleport, no
   confirmation.
5. Class javadoc gains the v1.5.28 history block, per project convention.

Placement reasoning: the ExcelMenu 2.0 catalogue already reserves this
entry's Bedrock position — `order-bedrock: 186`, immediately after
"Ask the Guide" (185 is the seasonal `halloween` stats entry, which
remains a Java-only surface and is OUT of this build's scope). Help group
placement also mirrors the Java entry's `group: help`. "Moved up" is
satisfied relative to the alternative (bottom of the form / Settings):
the button sits in the Help cluster directly under What's New / Ask the
Guide, not buried with the settings toggles.

Icons: the Bedrock form path (`GeyserForms.sendSimpleForm`) sends text
buttons only (`button(String)`); the catalog has NO icon support, so the
Java entry's CARVED_PUMPKIN icon has no Bedrock counterpart. Noted, not
worked around.

Date windows: the hardcoded catalog has no visible-from/until mechanism
(that lives in the ExcelMenu 2.0 registry, which does not own /menu yet).
Retirement follows the addendum's settled honest split: (a) functional —
ExcelHalloweenToggles auto-retires after Nov 5; `/scares` answers
"The Halloween event has ended — scare toggles are retired", so a tap in
the gap is harmless; (b) the button itself is removed in the Nov 6
Halloween cleanup window — cleanup packet §3C (added with this build)
swaps the live jar back to the preserved 1.5.27 backup.

Deliberately out of scope: the `/servercommands` CATALOG (the requirement
was the Server Menu button); the seasonal `halloween` stats button
(order-bedrock 185); any config change (bundled `config.yml`
byte-identical to 1.5.27).

## Candidate of record

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelBedrockMenu-1.5.28.jar` | 41,839 B | `b9a8abd44fbb82b4a05923e362df6d8e0e9cc8b80a9fdf1d6dcbd47808cb1dc9` |
| `ExcelBedrockMenu-1.5.28-source.zip` | 35,179 B | `e665121e1b9676239a8553dde64c8a3f81c075ee47aa373d33f0ea86ab57055c` |

- **Deterministic build**: built twice from the filed tree → byte-identical
  both times (same hash above). Build: `build-offline.sh` in this folder
  (JDK `~/workspace/.jdk25`, Paper libraries as above, jar date pinned
  2026-11-01T00:00:00Z).
- **Change scope (jar-entry diff vs the base-proven 1.5.27 jar)**: exactly
  TWO entries differ — `com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.class`
  and `plugin.yml` (version line 1.5.27 → 1.5.28 only). Every other entry,
  including the bundled `config.yml`, is byte-identical.
- **Change scope (source diff vs the 1.5.27 tree)**: only
  `ExcelBedrockMenu.java` (javadoc block + 4 functional lines) and
  `plugin.yml` (version). `AdapterLogicTest.java` extended (test-only,
  not in the jar).

## Tests

- **AdapterLogicTest: `ADAPTER_LOGIC_PASS checks=185`** — 148 carried from
  1.5.27 (Silk + Aim Assist label/guard mirrors) + 37 new: `scares` Help
  group color, no teleport confirmation (locked/unlocked), menu length 26,
  the FULL 26-label sequence asserted in order (the catalog diff is exactly
  the one insertion), the seasonal label byte-exact including its Help
  color prefix, both neighbors ("Ask the Guide" before, "Wand controls"
  after), and color spot-checks that Spawn / Excel Shop / Aim Assist kept
  their group colors after the index shift.
- No Paper/Geyser rig was run for this delta: the button dispatches
  through `runGuarded`, a code path already live and player-proven under
  four other buttons, and the class-scope proof above bounds the blast
  radius to the one class. The live boot-verify + the Bedrock player
  proof in the install packet are the verification of record (named,
  not silent).

— Excel (Iris), for Wink Reyes Valley
