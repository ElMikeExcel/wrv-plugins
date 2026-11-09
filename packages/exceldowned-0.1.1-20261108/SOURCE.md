# ExcelDowned 0.1.1 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** lethal damage becomes a DOWNED state instead of death
  (frozen half-heart, no function, deaggroed, bleed-out clock), and the
  Resurrection spell revives a downed party member at 50% health.
  Design: `~/workspace/downed-revive/DESIGN.md`.
- **0.1.1 delta (buttons are the house way — Mike, Nov 8, 2026):** the
  first `/giveup` while downed opens a button confirm instead of only
  arming in chat — a Java inventory menu (red "✝ LET GO — die now" /
  green "Keep fighting", ExcelMenu holder pattern) or the Bedrock
  Cumulus modal form ("✝ Let go" / "Keep fighting", ExcelBedrockMenu
  GeyserForms reflection pattern; form API absent → the 0.1.0 chat
  arm/confirm). Menu/form and typed paths share one arm state; closing
  the UI cancels; a stale "Let go" after revive/lapse does nothing.
  No 0.1.0 gameplay values or paths changed.
- **Source:** this folder (`src/com/winkreyes/exceldowned/`), built by
  `local-build.sh` (deterministic double-build, JUnit first, fingerprint
  gate). Tests: 17/17 (`DownedTest` — the 0.1.0 state machine plus the
  0.1.1 arm/confirm/disarm state sharing and the platform routing
  matrix).
- **Candidate jar:** `ExcelDowned-0.1.1.jar`, sha256 recorded in
  `~/workspace/downed-revive/BUILD-RECORD-0.1.1.md`.
- **Build ID:** EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
- **Desk rig:** `~/workspace/downed-revive/build/rig/` — Paper 26.3 +
  Magic 11.2.5 + NMS fake players; results in BUILD-RECORD-0.1.1.md.
- **Status:** BUILT at desk, NOT installed (0.1.0 is live). Companion
  spell packet: `~/workspace/spell-audit/packet-resurrection-20261108/`
  (unchanged by 0.1.1 — jar-only swap).
