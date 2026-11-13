# ExcelMenu 2.0.1 — BUILD RECORD

Built Nov 13, 2026 (desk build only; no MANIFEST filing, no
panel/server contact, no GitHub/Drive). Builder: Iris subagent,
board supervisor run 278 continuation dispatch. Spec:
`~/workspace/menu-items56-20261113/item6-back-button/DESIGN.md` §3
(Item 6 — Back button fix for grouped Bedrock section forms;
Mike's direct Nov 13, 2026, standing rule: every menu/submenu
always carries a Back button).

## Final artifact

- `ExcelMenu-2.0.1.jar` — SHA-256
  `c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a`,
  287,906 bytes. Built twice, byte-identical (both builds
  recorded: build-1.log / build-2.log, each `OK (128 tests)`;
  hashes `c6da29ba…` on both runs).
- `ExcelMenu-2.0.1-sources.zip` — SHA-256
  `620f7af0b4ad2c12e9e3cc0224a85bd173711b167d0ea306faaf4b572324d824`,
  257,218 bytes, 197 files (src + fixtures + tools + build.sh,
  entry dates pinned 2026-10-31T00:00:00Z, house pattern).

## Part 1 — BASE GATE (read this first: the on-disk tree drifted)

The commission's first step was to rebuild 2.0.0 from the phase-2
tree (`~/workspace/menu-2.0/phase-2/`) and confirm byte-identity
with the live jar `d1c6e771…` / 286,829 B before editing.

**The on-disk working tree does NOT reproduce.** Copied verbatim
and built, it produced 299,066 B / `22b12e11…` with 9 of 127
tests failing. Cause, verified file by file: after the Nov 2,
2026 2.0.0 build, the tree was deliberately mutated by the Nov 7,
2026 org-decisions foldback (see the header note in its
`layouts/commands_menu.yml`: "this seed copy is now the LIVE
file") — new entries (excelshop, landmarks, braingames, maxorg),
changed shop / wandstyle / autosort entries, and `.bak-*` files
under `src/main/resources/` that `build.sh` copies into the jar.
That tree is live-data lineage, not the frozen 2.0.0 source.

**The tree's own frozen snapshot DOES reproduce, exactly.** The
phase-2 folder carries `ExcelMenu-2.0.0-sources.zip`, SHA-256
`f16f3e32e05bc89e2d35805b563378939f3fb5b01c066d3b66610f9e6061eda1`
/ 260,275 B — matching BUILD-RECORD.md and SOURCE.md of the
2.0.0 build byte for byte. Unpacked to a clean folder and built
with its own `build.sh`:

- jar SHA-256 `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2`
- size 286,829 B — **byte-identical to the live 2.0.0 jar**
- suite `OK (127 tests)` (golden harness D1–D11, catalogue C1–C8)

Receipts kept in `base-reproduction/` (the rebuilt jar + the
base build log). **The 2.0.1 build is based on that proven
snapshot**, unpacked into this build folder — not on the drifted
working tree — so the 2.0.1 jar's diff against the live jar is
exactly the patch (see TEST-REPORT.md, jar-diff section). No
unproven base was built on. The supervisor should note the
drift finding for the phase-2 tree's records: any future build
commission naming "the phase-2 tree" must name the sources zip
as the base, or re-freeze the tree first.

## Part 2 — the 2.0.1 patch (DESIGN §3, applied as specified)

1. `core/render/FormModel.java`
   - `Target` sealed interface gains a third member:
     `record PickerTarget() implements Target {}`.
   - `public static final String BACK_LABEL = BedrockDetail.BACK_LABEL;`
     — single wording source shared with the detail forms.
   - `group()` appends exactly one
     `new Button(BACK_LABEL, new Target.PickerTarget())` after the
     entry buttons — Back always LAST. Picker and flat
     compositions unchanged (the picker is the root; FLAT has no
     navigation depth).
2. `bukkit/FormService.java` — `answer()` handles `PickerTarget`:
   re-resolves fresh (`entries.entriesFor(layout.id(), …)`, the
   session `refresh()` freshness) and calls `open(player, layout,
   fresh)`, which re-opens the picker through the existing `send()`
   path (new session token, group = null, generation bumped). No
   stats tap for Back (chrome, not a door), matching GroupTarget.
3. Tests — `FormModelTest`: `groupFormListsRowsAsBodyAndButtons`
   9 → 10 buttons with the Back/PickerTarget tail assertion;
   `groupFormForJavaOnlyGroupRenders` 4 → 5 with the same tail
   assertion; NEW `groupedServerMenuGroupFormEndsWithBackAndPickerHasNone`
   (a server_menu group form ends with Back → PickerTarget; the
   picker itself carries NO Back). Suite: **OK (128 tests)**.
4. `plugin.yml`: `version: 2.0.1` — the only resource change.
   `build.sh`: `VERSION="2.0.1"` (build knob only).

**No data files change.** Bundled entries/layouts/menus/config
in the 2.0.1 jar are byte-identical to the proven 2.0.0 jar
(jar diff in TEST-REPORT.md). Java side: no change (DESIGN §4
verified — no Java rendering code touched).

Scope landed for free (renderer-level): all 6 grouped
`server_menu` sections AND all 9 `commands_menu` groups, plus any
future grouped layout.

## Rig

Desk rig boot (phase-2 rig pattern, Paper 26.3-235 + Geyser,
seeded Nov 2 live-end-state data): ExcelMenu 2.0.1 enabled,
84 entries / 2 registry menus / 12 definition menus, bridge
PRESENT, Done (38.048s), **SEVERE 0** — full account in
TEST-REPORT.md; boot log at `rig/boot-console.txt`. The DESIGN
§3 behavioural rig (a)–(e) on the PC test-server mirror with
live-pulled jars is a filing-time step and is NOT claimed here.

## ⚠ Warning carried from DESIGN §6 (verbatim)

"Item 3 (ExcelMenu 3.0) must branch from the 2.0.1 tree or it regresses this fix."

Design §6, in full: "**3.0 collision:** Item 3's build must branch from the 2.0.1 tree;
if 3.0's design starts from the 2.0.0 tree it would silently
regress this fix — the supervisor carries this line into Item 3's
design gate." This build folder (with its sources zip) is the
2.0.1 tree of record for that branch.

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
