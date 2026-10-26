# ExcelBedrockMenu 1.5.21 changelog (Oct 25, 2026)

Includes everything in 1.5.20 below (Lost & Found and Trash Bin). 1.5.20 was staged and pushed to the
draft PR but never installed; 1.5.21 replaces it so two different "1.5.20" jars never exist.
Asked for by Mike, Oct 25, 2026, 6:42 PM CDT: "Yes color the text."

## Changed
* The Bedrock compass (Server Menu form) button TEXT is colored by group, matching the Java compass
  rows and the map legend. The buttons stay Bedrock's light gray; only the text color changes.
  | Group | Buttons | Code | Contrast on #C6C6C6 |
  |---|---|---|---|
  | Travel | Spawn, Teleport to a player, Party, Adventurers Guild | §1 dark blue | 7.8:1 |
  | Me | Utility Pouch, Spellbook, Jobs & Money, mcMMO Stats, Skills & Perks, Leaderboard, Lost & Found, Trash Bin | §5 dark purple | 3.7:1 |
  | Shops | Excel Shop | §2 dark green | 1.8:1 (weak) |
  | Help | Server Info, Server Commands, Server Website, What's New | §n copper (Bedrock-only material color) | 2.4:1 (weak) |
  | Settings | Wand controls, Auto-sort my chests, Auto-open on join | §8 dark gray | 4.4:1 |
  Default text before was about 5.0:1.
* Why these codes: gold §6 is 1.1:1 on the gray (unreadable), so Help uses copper §n, the darkest
  orange Bedrock has (§n is underline on Java, but this form only shows on Bedrock). There is no darker
  green code than §2 (emerald §q is about 2.0), so Shops stays §2. Lighter codes (§9, §d, §a, §e) were
  worse. The two weak ones are flagged for the real-device check.
* Label words are unchanged. The color is added only when the form is sent (new `groupColor`,
  `coloredLabel`, `menuLabels`); BUTTONS and COMMANDS keep plain text. Taps still route by button
  index, so colors can't break a button. The locked Guild label and both toggles (Auto-sort, Auto-open
  on join) keep their exact text under the color.
* ExcelMenuStats tap logging strips the codes (`stripCodes`, removes Java and Bedrock-only codes), so
  clicks.csv keeps the plain label and history lines up (rig: "Lost & Found", not "§5Lost & Found").
* Other forms (Server Commands groups, Spawn confirm, etc.) are not colored.
* plugin.yml version 1.5.21. No config, no new permission, no new file written.

# ExcelBedrockMenu 1.5.20 changelog (Oct 25, 2026)

Base: the LIVE jar ExcelBedrockMenu-1.5.19-whatsnew.jar, sha256
178b03b7c3d202f08cfecf2471a1a9aa9b4cbd6f4397c048988b22be27cc2175 (live since Oct 25, 10:41 AM CDT).
The base source (base/ebm-1.5.19-src, copied from batch-next/whatsnew/ebm-1.5.19-src) rebuilds to that
jar byte for byte (base/repro-1.5.19-whatsnew.jar), so the diff below is the only change.
Asked for by Mike, Oct 25, 2026, 6:24 PM CDT: give Bedrock the Lost & Found and Trash Bin buttons the
Java compass has.

## Added
* **Lost & Found** button on the Bedrock compass (Server Menu form). 12th button from the top.
  Runs `/lostfound` as the player, the same command as the Java compass button (`[player] lostfound`,
  ExcelDeathRecord 0.3.0, live). ExcelDeathRecord shows its own Bedrock form with the price before you
  pay. Label copied from Java: "Lost & Found".
* **Trash Bin** button, right after Lost & Found (13th). Runs `/trash` as the player, the same command
  as the Java button (`[player] trash`, ExcelTrash 0.2.0, live). Opens the plain bin, not chest mode
  or undo. Label copied from Java: "Trash Bin".
* Placement: the two buttons close the Bedrock "Me" group (Jobs & Money, mcMMO Stats, Skills & Perks,
  Leaderboard), in the Java Me row order (Lost & Found slot 14 before Trash Bin slot 16). Utility Pouch
  and Spellbook stay where they are on Bedrock (Mike moved the Pouch up in 1.5.14).
* Guard (new helper `runGuarded`, `guardOutcome`): if the owning plugin's command is missing, the
  player gets "Lost & Found isn't available right now..." / "The Trash Bin isn't available right
  now..." instead of an unknown command (same pattern as Spellbook, Auto-sort, What's New). If the
  player lacks the command's own permission node (`exceldeathrecord.lostfound`, `exceltrash.use`, both
  default true), they get "You can't use Lost & Found right now..." / "You can't use the Trash Bin right
  now...". Java checks nothing extra (the command itself checks), so normal players see the same result
  on both editions.
* Neither button is a teleport, so neither opens the Yes/Cancel teleport confirm.

## Icons
* None. This plugin's Server Menu form has never used button images (no `FormImage` anywhere; the
  live form JSON has no `image` fields), so Bedrock shows text-only buttons, as all 18 existing ones
  do. Java keeps BUNDLE (Lost & Found) and CAULDRON (Trash Bin). Adding images would be a change to
  every button and was left out on purpose.

## Changed (side effect only)
* Server Info, Server Commands, Server Website, What's New, Wand controls, Auto-sort and Auto-open
  each move down 2 places in the list. Their labels, commands and behavior are unchanged.
* plugin.yml version 1.5.19-whatsnew -> 1.5.20. Nothing else in plugin.yml or config.yml.
* build.sh: output name 1.5.20, jar entry date 2026-10-25 (deterministic build).
* Class doc comment: v1.5.20 paragraph.

## Not changed
* All 18 existing buttons: same labels, same commands, same relative order, same guards.
* Server Commands catalogue (45 entries; /lostfound and /trash are not added to it, a possible
  follow-up). Teleport picker, childproof picker, Skills & Perks form, website form, ClickStats hook.
* The EliteMobs `villageFallback` fix (compass-elitemobs-followup, a different 1.5.19, cb8bf9ec) is
  still NOT in this line, same as the live 1.5.19-whatsnew.
* Bedrock still has no Ask the Guide, My Cosmetics or Maker's mark buttons (separate follow-up).

## Tests
* MenuLogicTest: 172 ok, ALL PASS (was 149). 23 new 1.5.20 checks plus existing pins updated in
  place for the 2 extra buttons (20 buttons, removal-leaves-old-order checks now also remove the two
  new ones). See tests/MenuLogicTest-1.5.20.out.
* Box rig (Paper + Geyser + Floodgate, fake Bedrock player): 14 of 14. See TEST-RESULTS.md.

## Jar
* staged/ExcelBedrockMenu-1.5.20.jar, sha256
  0f8eec0ff691c9acbac1b38854d15391fb3e9f9a8e58b8edb836d73919e6c6bd (33,230 B;
  rebuilt twice, same hash).
