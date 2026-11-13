# BUILD RECORD — ExcelSummons 0.1.7 (compass Summons page)

Built Nov 12, 2026 (evening) under Mike's commission of
Nov 12, 2026, ~5:55 PM CST ("Yes, add a Summons page to the
compass menu") with both same-evening refinements: the adopted
sub-page split (~6:01 PM) and the confirm-gated per-pet adopted
dismiss (~6:05 PM). Commission of record:
`~/workspace/summons-menu-017-build/COMMISSION.md`. Design
summary: `DESIGN-0.1.7.md` in the source tree. **Not installed.**
The install packet is `packet-0117-20261112/INSTALL-PACKET.md`;
the parent files the MANIFEST entry and fires the installer
(this lane did NOT touch the MANIFEST, the live server, or the
PC test-server mirror — the rig is a local VM rig).

## Base proof

- Base source: the LIVE 0.1.6 tree at
  `~/workspace/summons-016-build/ExcelSummons/`, copied (never
  modified) into this build. Its jar re-verified before copying:
  66,209 B, SHA-256
  `08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1`
  — the exact live 0.1.6 candidate.
- The 0.1.6 policy core is byte-identical in 0.1.7:
  ReservePolicy, CapPolicy, AdminPolicy, LifetimePolicy,
  TargetRules, Nameplate, SummonRecord, SummonRegistry all hash
  equal to the 0.1.6 tree (verified at close-out). config.yml is
  byte-identical to 0.1.6 (SHA-256 659d9e2c…c3a9) — no config
  change, no new setting.

## What changed (the whole delta)

- NEW `MenuModel` (pure): page split (cast main page / adopted
  sub-page), list-identical rendering, applied-hold rendering,
  pagination (45/page), the dismiss-button/scope table, the
  result text (the command's own), the adopted confirm texts.
- NEW `AdoptedDismissPolicy` (pure): the adopted-dismiss gate
  (record exists + owner match + ADOPTED origin) and the INFO
  audit line format.
- NEW `SummonsMenus` + `SummonsMenuListener` (Bukkit mechanics,
  DummyMenus/DummyListener pattern): Java GUIs (main / adopted /
  confirm views), click routing, dismiss reporting.
- NEW `PlatformDetect` + `BedrockForms`: carried from
  ExcelTrainingDummy (house reflection pattern; package + log
  prefix only). Bedrock main form, adopted form (body = every
  adopted line; buttons = per-pet dismiss + Back), confirm form;
  a form failure falls back to the chat list.
- `ExcelSummons`: registers the menu listener; adds the `menu`
  subcommand (players only) + its name in the two usage lines;
  adds `listener()` and `menuPageFor(owner)` (the HP map is
  computed with listLineFor's own expression — the only
  Bukkit-dependent menu input).
- `SummonsListener`: ONE new method, `dismissOneAdopted` — the
  narrowly-scoped single-record path (owner + entityId,
  AdoptedDismissPolicy-gated, despawn through the existing
  funnel, INFO audit line). No existing method changed.
- `plugin.yml`: version 0.1.7; usage + description name the menu
  form. BuildInfo/local-build.sh re-pinned to build
  **EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17**.

## Candidate

- `ExcelSummons-0.1.7.jar` — **90,538 B** — SHA-256
  `2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56`
- Deterministic double-build byte-identical, AND a clean-room
  rebuild from the shipped source zip reproduced the same hash
  (three independent builds, 89/89 tests in each).
- Fingerprint gate PASS — build
  **EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17**.
- `ExcelSummons-0.1.7-source.zip` — 108,590 B — SHA-256
  `091b63ab00860cb59b1507b8aff602767d990ad2f37a648874750f88069b59c1`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.3…0.1.7.md; timestamps pinned 2026-11-12).
- Packet companions: the staged compass door
  `menu-entry/summons.yml` (ExcelMenu registry 93 → 94 at
  install). No Magic config, no messages addition, no other
  plugin's files.

## Proof summary

- Unit harness: **89/89** (78 carried — 2 re-pinned, named in
  TEST-RESULTS.md — + 11 new menu/dismiss tests).
- Real Paper 26.3 rig (local, Magic present): **22/22 RIGOBS**,
  SEVERE 0, clean boot + shutdown — command-path GUI open,
  GUI/list agreement, adopted split + indicators, confirm flow
  (unconfirmed removes nothing; confirmed removes exactly one;
  audit line in the server log), GUI-vs-command A/B on all three
  dismiss scopes, pagination 45+5, Bedrock send path exercised
  with the client proof explicitly OWED (RIG-RESULTS.md).
- Acceptance after install (the packet's): Mike opens the
  compass Summons page on Bedrock; Jester uses the page instead
  of `/summons list` (tracker row 62's surface).
