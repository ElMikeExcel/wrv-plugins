# ExcelBedrockMenu 1.5.16 (draft, not installed): kid-safe trust picker

Base: the 1.5.15 tree (PR #48). The untouched 1.5.15 tree rebuilds byte-identical (9204226e...).

## What it adds
- `/childproof <player>` and `/unchildproof <player>`: **Bukkit commands.yml aliases**, no code
  (see `commands.yml-with-childproof-aliases.yml`). Run as the player inside their claim:
  `untrust $1` -> `accesstrust $1` -> `childproofmenu done $1`. Result: doors, buttons, levers and beds only.
  Example: `/childproof Rylie` (type the player's real name; GriefPrevention does not resolve Essentials nicknames).
- `/childproofmenu` (EBM): Bedrock form picker (online players shown as "Nick (realname)") with a Yes/Cancel
  confirm, then runs `/childproof <realname>`. Java players get clickable names. Refuses outside a claim and if the alias is missing.
- `/childproofmenu help`: usage with concrete examples.
- `/childproofmenu done|undone <name>`: the alias's last line. It reads GriefPrevention back (reflection, no compile dependency)
  and prints one checked line: "Done! X can open doors... Undo: /unchildproof X", or a warning when typed outside a claim
  (GriefPrevention then changes ALL the player's claims), or "check the messages above" when the name didn't resolve.
- Server Commands catalog: two rows after `/trust` in Claims & Land (42 -> 44).

## Restart
commands.yml aliases are read only at startup -> next restart. Note: Bukkit drops an alias whose commands don't exist,
so the aliases need EBM 1.5.16 loaded (both ride the same batch).

## Proof
- MenuLogicTest: ALL PASS (126 ok).
- Rig (Paper 26.2, GriefPrevention 16.18.7, fake players, `rig/`): 24/24 PASS, 0 SEVERE. Before: build trust, enchanting
  table breakable. After /childproof: access only, table break and chest BLOCKED, door and button ALLOWED, other claim untouched;
  /unchildproof -> no trust; no-arg changes nothing; a non-manager can't change someone else's claim; picker refuses outside a claim.
