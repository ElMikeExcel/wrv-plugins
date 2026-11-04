# ExcelHalloweenToggles 0.1.1 — CHANGELOG (Nov 1, 2026)

## Changed
- **Bare `/scares` now opens a tappable surface instead of printing a
  chat list** (Mike, Nov 1, 2026, 5:35 PM CST: the chat list breaks the
  house style — menus and the shop are tappable surfaces — and on
  Bedrock the chat list isn't tappable at all). Bedrock players get a
  GeyserForms list: one button per scare showing its current state
  ("Slenderman: ✔ ON"), a tap flips it and the form **re-opens showing
  the new state**, plus a Close button. Java players get a 27-slot
  chest GUI in the ExcelMenu state style: same icons, names and lore
  as the `halloween_scares` menu definition, state readable at a
  glance (green ✔ ON / gray ✘ OFF), a click flips and the GUI
  re-renders in place, BARRIER Close at slot 22.
- The `/scares list` chat output's header now points at the tappable
  surface ("tap one with /scares") instead of the Server Menu.

## Added
- `ScareView` — the single pure render model (labels, icons, lore,
  slot/button order) both surfaces are built from, so Bedrock and
  Java wording can never drift apart.
- `GeyserForms` — reflection-based Geyser form helper mirrored from
  ExcelBedrockMenu 1.5.27/1.5.28's proven helper (no compile-time
  Geyser dependency; fail-soft — any form failure falls back to the
  chest GUI, then the chat list).
- Four config message keys for the surfaces (`messages.form-title`,
  `messages.form-content`, `messages.gui-title`, `messages.gui-close`);
  on upgrade they resolve from the jar default — the live config file
  needs no edit.
- `ScareViewTest` — 7 unit tests over the render model and the
  flip/re-open round-trip through the real ToggleStore.

## Unchanged (byte-identical classes vs 0.1.0, jar-entry diff proven)
- The seven toggles, players.yml storage and defaults, the scheduler
  takeover, the login-pumpkin driver, the PlaceholderAPI bridge, and
  the gate/window logic.
- `/scares list`, `/scares <feature>` (same chat confirmation),
  `/scares test <feature>`, tab completion, and the auto-retirement
  after Nov 5, 2026.
- Both menu entries (ExcelBedrockMenu "Halloween Scares" button,
  ExcelMenu 2.0 `halloween_scares`): they dispatch `/scares` /
  `/scares <key>`, which now land on the new surfaces — no menu jar
  or registry change.

## Notes
- Base proof: the unmodified 0.1.0 tree rebuilds byte-identical to
  the shipped 0.1.0 jar (`3b537e87…`) before the delta is applied.
- Rig-proven on Paper 26.3-235 (SEVERE 0, awake line exact). The
  Bedrock form tap-through and the Java GUI click-through are player
  proofs, owed post-install — the rig has no real client.
- The Nov 6 Halloween cleanup is unaffected: 0.1.1 installs under the
  live filename, and the cleanup removes this jar + folder wholesale.
