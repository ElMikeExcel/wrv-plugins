# ExcelBedrockMenu 1.5.28 — CHANGELOG (Nov 1, 2026)

## Added
- **Halloween Scares button in the Bedrock Server Menu.** A 26th main-menu
  button, "Halloween Scares — your switches", in the Help group
  immediately after "Ask the Guide" (the slot the ExcelMenu 2.0 catalogue
  reserves for it, order-bedrock 186). Tapping it runs `/scares` as the
  player through the standard guarded dispatch, listing the player's
  seven Halloween scare switches. This closes the parity gap Mike caught
  Nov 1, 2026: the Oct 31 requirement put the toggles in the Server Menu
  on both platforms and the Java chest menu had the entry, but the
  Bedrock form — which still owns /menu and the compass — did not.

## Notes
- The label matches the Java registry entry verbatim. The Bedrock form
  catalog has no icon support (text buttons only), so the Java entry's
  carved-pumpkin icon has no Bedrock counterpart.
- Retirement: ExcelHalloweenToggles auto-retires `/scares` after
  Nov 5, 2026 (the command answers "retired"); the button itself is
  removed in the Nov 6 Halloween cleanup window (cleanup packet §3C —
  swap back to the preserved 1.5.27 jar).
- Nothing else changed: jar-entry diff vs 1.5.27 is exactly
  `ExcelBedrockMenu.class` + the `plugin.yml` version stamp; the bundled
  `config.yml` is byte-identical; the /servercommands catalogue is
  untouched.
