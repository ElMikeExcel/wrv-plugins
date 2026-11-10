# ExcelDowned 0.1.2 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commissioned from the console-admin coverage audit (Nov 9, 2026)
under Mike's Nov 3, 2026 console-admin rule.

## Provenance

- Project tree: `~/workspace/downed-012-build/ExcelDowned/`
  (plugin.yml version 0.1.2, main
  com.winkreyes.exceldowned.ExcelDowned, api-version '1.21').
- Base: the records-package 0.1.1 tree
  `~/workspace/records-catchup-20261103/packages/exceldowned-0.1.1-20261108/`,
  verified byte-identical to `~/workspace/downed-revive/build/ExcelDowned/`
  (src, plugin.yml, local-build.sh diffed clean) before the change.
  Shipped 0.1.1 jar SHA-256
  331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f
  (47,199 B) re-verified on disk Nov 10, 2026, and a clean-room
  copy of the untouched tree rebuilt that jar byte-identical
  (17/17 tests) BEFORE the delta was applied to a copy.
  0.1.1 is the live version (installed Nov 8, 2026 —
  `~/workspace/downed-revive/INSTALLED-0.1.1.txt`).
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.1
  (house lib set + Paper API 26.3 + Magic 11.2.5 API).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a depend (integration point only, never
  shaded); Geyser/floodgate access stays reflection-only via the
  0.1.1 BedrockForms pattern, untouched.

## What 0.1.2 changes vs 0.1.1 (the whole delta)

CONSOLE ADMIN. Design + root cause: DESIGN-0.1.2.md in this tree
and in the source zip. In short:

- NEW AdminPolicy: the pure decision layer — routing
  (`revive <player>` / `clear <player>` on the existing /downed
  command), the gate (console explicitly allowed; everyone else
  needs the EXISTING exceldowned.admin node), target
  classification (online-downed / online-not-downed /
  offline-durable / unknown), and the revive/clear decisions.
  No Bukkit types.
- DownedCommands routes the two admin verbs through AdminPolicy
  before the legacy surface (denial precedes all target
  handling); ExcelDowned gains adminRevive (the existing revive
  path with an operator-actor record/log, no fake spell cast),
  adminClear (session removal, online or offline-durable, no
  health written, no death run), and sessionByName (durable
  lookup). The spell revive's record fields and messages are
  unchanged.
- plugin.yml: version 0.1.2; the downed usage and the
  exceldowned.admin description name the new forms. The node
  itself (default op) is unchanged. config.yml byte-identical.
- NOTHING else: the downed state machine, the /giveup button
  confirm, restrictions, records shapes for existing event
  types, and every Mike-ruled value are untouched.

Change-scope diff vs the verified 0.1.1 tree — only:
AdminPolicy.java (new), DownedCommands.java, ExcelDowned.java,
BuildInfo.java, plugin.yml, local-build.sh, DownedTest.java,
DESIGN-0.1.2.md (new).

## Artifacts

- ExcelDowned-0.1.2.jar — 56,778 bytes
  SHA-256 2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396
  MD5 d2bcc275d60cda833d0125a8d847d5ee
  Build EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d (fingerprint gate
  PASS). Clean-room rebuild and source-zip rebuild each
  reproduced this hash exactly.
- ExcelDowned-0.1.2-source.zip — 42,200 bytes
  SHA-256 3994ea7ace118c244d59a3ce0d8b5166d392b518685621256ba2ecd26f5ba519
  MD5 2fba04dc1a691033038e7791e8160e4d
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.2.md; timestamps pinned 2026-11-10.)

## Verification summary

DownedTest 27/27 (17 carried unmodified + 10 new AdminPolicy
tests incl. acts exercised on a real session registry). Desk rig
on Paper 26.3-235 + Magic 11.2.5 with NMS fake players
(rig-0.1.2 in the build folder): clean 0.1.2 enable, Resurrect
registered, ready line naming 0.1.2 + bleed-out 300 s + factor
0.25, zero SEVERE, clean stop, 17/17 behavior checks through the
real console sender (revive at 50% / clear restoring nothing /
offline revive-refusal + clear / non-admin denials by state /
legacy status + /giveup rejection unchanged).
NOT installed — built + staged only.
