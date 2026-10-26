# TEST RESULTS: ExcelBedrockMenu 1.5.21 (Oct 25, 2026, 7:17 PM CDT)

Jar: staged/ExcelBedrockMenu-1.5.21.jar, 33,797 B, sha256
837424dd1a2afb7730e562b5f3e14f9ce4c9c765799cc94d578997eef80f1404. Built twice, same hash.

## Unit checks: MenuLogicTest 186 ok, 0 failed, ALL PASS (tests/MenuLogicTest-1.5.21.out)
New 1.5.21 checks (14): BUTTONS stay plain, every one of the 20 commands has a group, color per group
(Travel 1, Me 5, Shops 2, Help n, Settings 8), unknown command gets no color, menuLabels keeps 20 labels
locked or not, each sent label = group color + exact plain label, locked Guild label plain underneath,
toggle labels unchanged, stripCodes removes Java and Bedrock-only codes, stripCodes(null), openMenu
sends the colored labels, routing still by index, menu stats log the plain label, other forms not colored.
All 172 1.5.20 checks still pass (plugin.yml pin moved to 1.5.21).

## Box rig: 17 passed, 0 failed (tests/rig/bed121.out, phases a-d)
Throwaway copy of whatsnew-rig with ExcelDeathRecord 0.3.0, own ports, removed afterwards; whatsnew-rig
untouched. Fake Bedrock player only. Boot: Enabling ExcelBedrockMenu v1.5.21, 0 SEVERE.
* /menu: 20 buttons in the same order; raw labels start with §1/§5/§2/§n/§8 exactly per group; the text
  after the 2-char code equals the plain label (tests/rig/bedrock-menu-form-captured.json).
* Lost & Found opens ExcelDeathRecord's form; Trash Bin opens the bin; no guard message.
* Denied path (LuckPerms false): plain "You can't use ..." messages, no form, no bin.
* Leaderboard, What's New still open; Auto-sort toggle (colored label) toggles on and off.
* ExcelMenuStats clicks.csv got plain labels ("Lost & Found", "Auto-sort my chests: on/off"), no codes
  (tests/rig/bed121-clicks-labels.txt).
* Bonus: the first boot accidentally had ExcelDeathRecord 0.2.0 (no /lostfound); the button said
  "Lost & Found isn't available right now. Ask an adult to check the server." (seen on the rig; that
  run's log was not kept).
* Test client note: the bedrock-protocol client hit "Checksum mismatch" (its own decryption) on longer
  sessions while another box server was busy, so the flow was split into 4 short sessions (phases).
  Phase b needed 5 tries; the server never logged an error. Failed tries: tests/rig/bed121-attempts/.

## NOT tested
* A real Bedrock device: how the colors look, especially Shops green (1.8:1) and Help copper (2.4:1),
  and whether every device renders §n as copper. The mockup is drawn from the code RGB values.

---

# ExcelBedrockMenu 1.5.20: test results (box, Oct 25, 2026, 6:27 to 6:33 PM CDT)

## Base proof
* base/ebm-1.5.19-src rebuilt with its own build.sh -> 178b03b7c3d2...2175, byte-identical to the live
  ExcelBedrockMenu-1.5.19-whatsnew.jar (149 ok, ALL PASS on the base tests).

## Build
* src/ExcelBedrockMenu/build.sh (JDK 25.0.5, javac --release 21, jar --date fixed) ->
  staged/ExcelBedrockMenu-1.5.20.jar 0f8eec0ff691c9acbac1b38854d15391fb3e9f9a8e58b8edb836d73919e6c6bd.
  Built twice, same hash. plugin.yml and config.yml parse (the rig loaded them).

## Unit checks: MenuLogicTest 172 ok, 0 failed, ALL PASS (tests/MenuLogicTest-1.5.20.out)
New 1.5.20 checks (23): positions 11/12 (0-based), button/command line up, adjacent with Lost & Found
first, right after Leaderboard, Server Info follows, each label/command once, removing the two leaves the
exact live 18-button and 18-command order, no /recover or /bin alias, plain /trash (no chest/undo),
no teleport confirm for either, both guarded, guard uses the command's own permission node,
guardOutcome missing/denied/run, messages name the feature, ASCII labels, catalogue unchanged (45).
Existing pins updated in place for 20 buttons (no old check removed).

## Box rig: 14 passed, 0 failed (tests/rig/bed120.out; first try tests/rig/bed120.try1.out)
Rig: a throwaway copy of whatsnew-rig (Paper, Geyser, Floodgate, LuckPerms, ExcelDeathRecord 0.3.0,
ExcelTrash 0.2.0, ExcelWhatsNew 0.1.0), own ports, removed afterwards. whatsnew-rig itself untouched.
Fake Bedrock player through bedrock-protocol (no real names). Boot: Enabling ExcelBedrockMenu v1.5.20,
0 SEVERE (tests/rig/rig-boot.log).
* /menu opens "Server Menu" with exactly the 20 buttons in the 1.5.20 order; Lost & Found 12th,
  Trash Bin 13th; no button images (tests/rig/bedrock-menu-form-captured.json).
* Tap Lost & Found -> ExcelDeathRecord's "Lost & Found" form (price, money, 7 days, Close). No
  unknown-command or guard message.
* Tap Trash Bin -> a container opens (the /trash bin). No unknown-command or guard message.
* With exceldeathrecord.lostfound and exceltrash.use set false (LuckPerms, then unset): "You can't use
  Lost & Found right now..." with no form, and "You can't use the Trash Bin right now..." with no bin.
* Unchanged buttons still work: Leaderboard (index 10) opens Leaderboards; What's New (moved to 17th)
  opens the news.
* First try: the permission part failed because LuckPerms did not match the Floodgate name form on the
  rig (the fake player has the all-zero UUID); rerun with the UUID form passed. The first try's client
  also crashed on a hand-made container_close packet (test client issue); the rerun rejoins instead.

## NOT tested
* A real Bedrock device (phone, console, Windows): look of the longer list, scrolling. The test client
  reads the form JSON only. bedrock-before.png / bedrock-after.png are mockups drawn from the source.
* The Bedrock "Ask an adult" text path when ExcelDeathRecord or ExcelTrash is missing (unit-proven only).
* The live LuckPerms setup and live player data.
