# PUSH NOTES — ExcelMenu 3.0.1 (Bedrock Server Menu section colours)

**For the supervisor / records legs. Desk build only — nothing was
pushed, filed, or published by this lane.**

- **What:** ExcelMenu 3.0.1 — Bedrock Server Menu picker + section
  forms carry the approved section colours; Spawn/Teleport pins
  keep dark blue via the new `pin-color` config knob; commands
  catalogue stays plain; Java untouched.
- **Jar:** `packet/ExcelMenu-3.0.1.jar`, 330,214 B /
  `5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939`.
  Sources zip (212 files): `3fc9872e…`.
- **Install:** gated jar-swap packet (`packet/INSTALL-PACKET.md`) —
  jar under live filename `plugins/ExcelMenu-2.0.0.jar` + place
  `packet/filing-data/config.yml` (3,215 B / `c0467b5f…`), ONE
  restart. Supervisor files the MANIFEST entry
  (`MANIFEST-ENTRY-DRAFT.md`); the failsafe installs.
- **Publication claim:** NOT YET RUN — no PR exists for this
  package. Records legs run the G2 duplicate check
  (`pr-exists.sh wrv-plugins ExcelMenu-3.0.1` or the package slug)
  before any push, then: Drive jar/source deposit + draft PR in
  wrv-plugins (stays OPEN DRAFT); website PR merged + G3 deploy
  check if the website pair changes. Claim line to add at
  close-out: "Publication claim: wrv-plugins PR #___ (duplicate
  check run <timestamp>, result ___)" — one line per repo published.
- **Same-pass surfaces owed (supervisor legs):** Guide (menu
  colours note if documented), website pair, What's New BOTH
  surfaces, tips check. Menus = this packet.
- **Vetoes owed (Mike):** Settings `§e` yellow + Chest Organization
  `§8` dark gray are build picks; Java headers NOT recoloured
  (see BUILD-RECORD.md). Veto by reply; a vetoed colour is a
  config-only change (no rebuild needed — `group-colors` is data).
- **Player proof owed (Mike, Bedrock, after install):** picker +
  one section form on his client, pins still blue.
