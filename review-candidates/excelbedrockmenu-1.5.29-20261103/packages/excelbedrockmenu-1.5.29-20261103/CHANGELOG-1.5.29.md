# ExcelBedrockMenu 1.5.29 — CHANGELOG (Nov 2, 2026)

## Added
- **A direction choice in the player-teleport flow: go to them, or ask
  them to come to you.** The teleport picker (Server Menu → "Teleport to
  a player", or `/tpmenu`) no longer fires a request the moment you pick
  a name. Picking a player now asks **"What do you want to do?"** with
  two choices:
  - **Teleport to player** — you go to them (the way it always worked).
  - **Ask player to come to me** — they come to you.
  On Bedrock the choice is a form titled with the picked player's name;
  on Java it's a small chest with the same two choices. Either way, the
  other player gets an Accept/Decline prompt before anyone moves — for
  the new direction it reads "<name> is asking you to teleport to them."
  Closing the choice without picking sends nothing.
  - Under the hood the menu runs Essentials' own commands as you —
    `/tpa <player>` for the first choice, `/tpahere <player>` for the
    second — so all of Essentials' cooldowns, safe-teleport checks and
    request timeouts apply exactly as before, and the typed commands
    work on their own too (`/tpahere` was enabled for all players
    earlier today; it was Jester's request).

## Notes
- Nothing else changed: the diff against 1.5.28 is confined to the
  teleport class plus the version stamp; the main-menu catalog, the
  Halloween Scares button, and the bundled config are byte-identical.
- If the Bedrock choice form can't be shown (Geyser hiccup), the menu
  falls back to the pre-1.5.29 behavior and sends the plain
  "teleport to player" request — a pick never dead-ends.
