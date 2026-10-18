# ExcelBedrockMenu 1.5.8 — changelog

**Teleport buttons ask first.** Players were teleporting by accident
on stray menu taps, so every menu action that immediately teleports
the player now opens a confirmation form first (Cumulus ModalForm,
the same form style as the teleport-request prompt):

- Title "Teleport to <destination>?", buttons **Yes, teleport** /
  **Cancel**. Closing the form without choosing does nothing.
- Yes runs exactly the action the button ran in 1.5.7, exactly once.

Covered:

- Main menu **Spawn** button (Yes runs the same console-spawn +
  village-fallback path as before).
- Main menu **Adventurers Guild** button — only while the guild is
  unlocked. While the guild is sealed, the button opens the seals
  quest page, which is not a teleport, so no confirmation is shown.
- /servercommands catalogue entries **/spawn**, **/home** and **/ag**
  (same guild-state rule for /ag).

Deliberately unchanged: the Teleport-to-a-player picker (its
teleports already complete through the other player's Accept/Decline
prompt), /tpaccept (answering a request is itself the consent step)
and /trapped (an emergency escape that only works when genuinely
trapped). All other buttons, labels, order and actions are identical
to 1.5.7; at the jar level only the main class and plugin.yml changed.

This is Mike's standing menu standard from Oct 17, 2026: any menu
button whose action immediately teleports the player gets a
confirmation step first, on both the Bedrock and Java menus.
