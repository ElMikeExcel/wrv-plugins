# ExcelWealthBoard (WealthBoard)

**Display name:** ExcelWealthBoard
**Installed plugin name:** WealthBoard (unchanged, no rename)
**Category:** Created by us
**Live version:** 1.3.0-mobile.6, installed October 13, 2026
**Live file:** `WealthBoard-1.3.0-mobile.6.jar` (5,066 bytes)
**SHA256:** `928454280588f33c9048a5fcf5f01bea7c0ea9030ca0b96c8ee11141f84693ab`

## What players see

When a player joins Wink Reyes Valley, a welcome popup appears about 10
seconds after joining (once the player is actually in the world):

- Title: **Wink Reyes Valley**
- Underneath it, the three richest players cycle one at a time, about
  3 seconds each, for example: `Richest: 1. GumGumJetPistol $6,481.24`,
  then `2. .coolgirlkail $4,706.23`, then `3. Jester_Turtle $1,078.97`.

There is no permanent sidebar. For the full board at any time, use
`/leaderboard`.

The plugin also enrolls every joining player in all Jobs automatically, so
players earn money and XP from ordinary play with no sign-up step.

## Java and Bedrock

The popup works the same on Java (PC) and Bedrock (phone, tablet, console).
The one-entry-at-a-time design exists because of two Bedrock limits found
while testing on a real phone:

- Extra lines inside a title subtitle never render on Bedrock through
  Geyser, and all three entries on one line run off the sides of a phone
  screen.
- Titles sent in the first seconds after joining are lost behind the
  Bedrock loading screen, which is why the popup starts 10 seconds after
  joining.

## Verification status

- Built: yes
- Installed live: yes, version confirmed with `version WealthBoard`
- Bedrock/mobile player check: passed (owner's phone, October 13, 2026)
- Java player check: not yet done
- Automated tests: this plugin has no automated test suite yet; live
  behavior was verified by the owner joining on mobile

## Rollback

The installer keeps the previous JARs on the server renamed with a
`.rollback-20261013` suffix (inactive). Restoring means renaming one back
to its original `.jar` name while no other WealthBoard JAR is active, then
restarting.
