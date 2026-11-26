# SURFACES — ExcelEMFixes 0.23.0 (wither-skulls) — staged, land in the install pass

Same-pass surfaces rule: every item below lands in the install
window's surfaces pass, or is explicitly named owed here.

## In-game What's New (ExcelWhatsNew changelog.yml) — STAGED TEXT

Entry to add at install (version 0.23.0):

    - "Elite wither skeletons: skull chance top-up moved into ExcelEMFixes (no change to the rate — still ~25% on elite wither skeletons killed by a player). /witherskulls works as before."

## Website What's New + plugin row — STAGED TEXT

- What's New entry (winkreyesvalley): "ExcelEMFixes 0.23.0 —
  Wither Skulls: the elite wither skeleton skull top-up (formerly
  the standalone ExcelEMWitherSkulls plugin) now lives inside
  ExcelEMFixes as the wither-skulls module. Same rate, same
  /witherskulls command; the standalone jar is retired."
- Plugin row update: ExcelEMFixes version 0.22.0 → 0.23.0,
  module count 24 → 25, add wither-skulls to the module list;
  remove/retire the standalone ExcelEMWitherSkulls row if one
  exists (mark "folded into ExcelEMFixes 0.23.0").

## Records draft PR — OWED (close-out leg, do NOT push from build lane)

- wrv-plugins records draft PR for the 0.23.0 build (jar + source
  + this packet) — opened by the install close-out lane per the
  standing pattern (duplicate check first, Homebase git lane).
  Publication claim line goes in the close-out record.

## Guide — VERIFY-NO-CHANGE

- Guide is hibernated (since Nov 16, 2026): no knowledge/intent
  changes. Verified: the fold changes no player-facing behavior,
  command, or rate, so no Guide content is stale. Noted per
  programme gate 5.

## Tips + menus — CHECKED, NO CHANGE

- Broadcaster tips: checked — no tip references WitherSkulls or
  skull rates in a way the fold changes (the behavior is
  unchanged). No tip edit owed.
- ExcelMenu / ExcelBedrockMenu: checked — no menu entry names
  the standalone plugin; /witherskulls is an admin command with
  no menu surface. No menu edit owed.
