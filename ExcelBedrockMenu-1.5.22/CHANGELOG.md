# ExcelBedrockMenu 1.5.22 changelog (Oct 26, 2026)

Base: the LIVE jar ExcelBedrockMenu-1.5.21.jar, sha256
837424dd1a2afb7730e562b5f3e14f9ce4c9c765799cc94d578997eef80f1404, 33,797 B (live since Oct 25, ~8:19 PM CDT).
The base source (base/ebm-1.5.21-src, copied from batch-next/bedrock-menu-lostfound-trash-20261025) rebuilds
to that jar byte for byte (base/repro-1.5.21.jar), so the diff (ExcelBedrockMenu-1.5.21-to-1.5.22.diff) is
the only change.

Why: Mike's parity rule, Oct 26, 2026, 12:10 AM CDT: "Rule is to always do onto one as you would do the
other. All our plugins should work both bedrock and java." The Bedrock compass now matches the live Java
compass (ExcelMenu server_menu.yml e18910e4, live since Oct 25, 11:18 PM CDT).

## Added (Bedrock compass, each next to its Java row-mates, same group color)
* **My Cosmetics** (7th, right after Excel Shop, Shops green §2). Runs `/cosmetics`, the same command as
  the Java button. ExcelCosmetics 0.1.2 answers a Bedrock player with its own "Cosmetics" form (no chest);
  with nothing owned it says "You don't have any cosmetics yet" in chat, same as Java.
* **Ask the Guide** (18th, right after What's New, the Java Help row order, Help copper §n). Java's button
  prints "Type /ask and your question in chat, or start a message with Guide,". Bedrock gets that as a
  form: one text box ("Type your question. The Guide answers from the server's own pages.", example
  "How do I claim land?"). Send runs `/ask <question>` as the player, the exact command Java players type,
  and the Valley Guide (ExcelGuide) answers in chat.
  * The typed text is cleaned first: color codes and line breaks removed, spaces collapsed, leading
    slashes dropped (so "/spawn" is only asked about, never run), capped at 200 characters.
  * Blank Send: "No question typed. Tap Ask the Guide again, or type /ask and your question in chat."
  * Closing the box does nothing. If the form can't be sent, the player gets the Java button's hint line.
  * The question is never logged. ExcelMenuStats gets only the fixed label "Send".
  * New `GeyserForms.sendInputForm` (Cumulus CustomForm with one input, read with `asInput(0)`, by
    reflection like the other two form helpers).
* **Maker's mark and Gear stats** (21st, between Auto-sort and Auto-open on join, the Java Settings
  order, Settings gray §8). Runs bare `/mark`, which ExcelMakersMark 0.1.0 answers on Bedrock with its own
  two-switch form ("Maker's mark: ON/OFF", "Gear stats: ON/OFF", Close). Those switches run
  `/mark on|off` and `/mark stats on|off`, the same commands as the two Java switches.
* Guards (same pattern as Lost & Found and Trash Bin): a missing plugin gives "... isn't available right
  now. Ask an adult to check the server."; no permission gives "You can't use ... right now." Ask the
  Guide also checks `excelguide.ask`, because ExcelGuide checks that node inside /ask rather than on the
  command, so a refused player never gets a text box. (ExcelCosmetics puts no permission on /cosmetics,
  so My Cosmetics behaves exactly like the Java button there.)
* None of the three is a teleport, so no Yes/Cancel confirm.

## Changed
* **mcMMO Stats is no longer its own main button**, matching the Java compass (removed there Oct 25; Mike:
  "Having it where it is, is good enough"). The Skills & Perks form gains a **My mcMMO stats** button
  (first), which runs `/mcstats` like the Java Skills & Perks menu's mcMMO Skills and Check Your Skills
  items. Skills & Perks buttons are now: My mcMMO stats, Skill Trees on the Website, Close.
* **Skills & Perks Magic text fixed.** It said "Magic is sealed for now ... (N of 5 today)" even though
  Seal 1 broke Oct 23, 2026 at 5:46 PM CDT and Magic (elmakers) is awake at stage 1 (Healer). Now, from
  1 seal on: "Magic is awake: N of 5 seals broken. Get your first wand from the Old Quartermaster in the
  spawn village. More spells open as each seal falls. Tap Spellbook in the menu to see your wand's
  spells." The old sealed line is kept only for 0 seals. Count is read live from ExcelGuildGate.
* Menu is 22 buttons (was 20). Order otherwise unchanged (Pouch stays 4th, per Mike's Oct 21 choice).
* `server_menu.yml` mirror in this folder refreshed to the live Java menu (e18910e4); it is a reference
  copy, not in the jar.
* plugin.yml version 1.5.22. No config change, no new permission, no new file written, no new command.

## Group colors (unchanged codes, new members in bold)
| Group | Buttons | Code |
|---|---|---|
| Travel | Spawn, Teleport to a player, Party, Adventurers Guild | §1 |
| Me | Utility Pouch, Spellbook, Jobs & Money, Skills & Perks, Leaderboard, Lost & Found, Trash Bin | §5 |
| Shops | Excel Shop, **My Cosmetics** | §2 |
| Help | Server Info, Server Commands, Server Website, What's New, **Ask the Guide** | §n |
| Settings | Wand controls, Auto-sort my chests, **Maker's mark and Gear stats**, Auto-open on join | §8 |

## Optional Java side (separate file, separate step, see java-optional/)
* ExcelMenu `skills_perks.yml`: the Java Magic item still says "Magic (Sealed)" / "Coming with the
  Adventurers Guild seals (0 of 5 today)". Staged fix: "Magic (Awake)", "Seals broken:
  %guildgate_seals_broken% of 5", "First wand: the Old Quartermaster / in the spawn village", "More spells
  open as seals fall". Same facts as the Bedrock form. Drift guard on live 5df3f0a3; `/excelmenu reload`,
  no restart. Not part of the jar.

## Not changed
* Server Commands catalogue (45 entries) and its Java twin commands_menu.yml: neither lists /cosmetics,
  /mark, /ask, /lostfound or /trash yet. Left for a paired Java + Bedrock change (see the parity audit).
