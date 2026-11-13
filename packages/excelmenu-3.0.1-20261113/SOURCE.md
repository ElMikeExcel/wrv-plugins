# SOURCE — ExcelMenu 3.0.1 (Bedrock Server Menu section colours)

**Plugin:** ExcelMenu 3.0.1 — Bedrock Server Menu section colours
(backlog Item 11; Mike's direct Nov 13, 2026, palette approved
~12:38 PM CST).

**Built from:** the ExcelMenu 3.0.0 tree at
`~/workspace/excelmenu-30-build-20261113/`, base gate PASSED
(rebuilt 3.0.0 byte-identical: 329,425 B /
`306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51`,
163 tests OK).

**Candidate jar:** `packet/ExcelMenu-3.0.1.jar` — 330,214 B /
SHA-256 `5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939`
(double-build byte-identical; `plugin.yml` `version: 3.0.1`).

**Sources:** `packet/ExcelMenu-3.0.1-sources.zip` (212 files) —
SHA-256 `3fc9872e05b8e5d43d01461c6b0bf956b768c4bd7b7a5fc28c623673a199ea92`
(`build.sh` + `fixtures/` + `src/` + `tools/`, fixed 2026-10-31
timestamps, same layout as the 3.0.0 sources zip).

**Build record:** `BUILD-RECORD.md` (design, palette incl. the two
build picks for veto, tests, rig proof, packet scope).

**Tests:** `OK (169 tests)` — 163 carried + 6 new
(`SectionColorsTest`).

**Rig proof:** `rig/harness-301.log` (12 CHECKs, 12 PASS) +
`rig/boot-301.log` (Paper boot: "ExcelMenu 3.0.1 enabled … 93
entries, 2 registry menus, 13 definition menus", Done 56.526s,
SEVERE 0).

**Install packet:** `packet/INSTALL-PACKET.md` — gated jar-swap
(jar under the live filename `plugins/ExcelMenu-2.0.0.jar` + place
`packet/filing-data/config.yml`, 3,215 B /
`c0467b5f04a2c77424eab776e02b3076caa4d2f20be62eff3aa949692050eef7`,
ONE restart). No jar is inside the records package.

**What changed:** Bedrock picker section buttons + section-form
entry buttons carry the approved section colours; Spawn/Teleport
pins keep dark blue via the new `pin-color` config knob; the
commands catalogue stays plain; Java untouched.
