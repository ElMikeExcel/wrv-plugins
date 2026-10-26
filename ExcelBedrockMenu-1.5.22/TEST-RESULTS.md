# TEST RESULTS: ExcelBedrockMenu 1.5.22 (box, Oct 26, 2026, 12:14 to 12:52 AM CDT)

Jar: staged/ExcelBedrockMenu-1.5.22.jar, 35,840 B, sha256
1d860fbb640d188a831b7da93595ce0a08ebcd83018a63fcc31b6201e960e2ab. Built twice, same hash.

## Base proof
base/ebm-1.5.21-src rebuilt with its own build.sh -> 837424dd...0f1404, byte-identical to the LIVE
ExcelBedrockMenu-1.5.21.jar (186 ok, ALL PASS on the base tests). So the diff is the only change.

## Unit checks: MenuLogicTest 224 ok, 0 failed, ALL PASS (tests/MenuLogicTest-1.5.22.out)
New 1.5.22 checks (38): positions (My Cosmetics 6 after Excel Shop, Ask the Guide 17 after What's New,
Maker's mark 20 between Auto-sort and Auto-open), mcMMO Stats gone from the main menu and present in
Skills & Perks (runs /mcstats), group colors for the three new buttons, same commands as the live Java
server_menu.yml ([player] cosmetics, mark on/off + mark stats on/off, /ask hint), guards (cosmetics, mark,
ask incl. excelguide.ask), no teleport confirm, ask sanitizer (plain, blank, /command, //op, color codes,
newlines, 200-char cap), fallback = the Java hint line, ASCII form text, CustomForm + asInput(0), stats log
only "Send", Magic line (0 seals keeps sealed text; 1+ says awake + count + Old Quartermaster + Spellbook;
capped at 5), colored sent labels, 22 ASCII labels. Existing order pins updated in place for the new
buttons and the mcMMO Stats move (no old check removed; plugin.yml pin moved to 1.5.22).

## Box rig: 24 passed, 0 failed (tests/rig/bed122.out = phases a to e)
Throwaway copy of whatsnew-rig at /workspace/ebm122-rig (own ports: Java 25991, RCON 25992, Bedrock UDP
19592), removed afterwards. whatsnew-rig and the other agent's ss022-rig untouched. Plugins: EBM 1.5.22,
ExcelDeathRecord 0.3.0, ExcelMakersMark 0.1.0 (the LIVE jar 7490006e, swapped in for the rig's older
c73d7656 build), ExcelCosmetics 0.1.2 (live sha 45fc556c), ExcelGuide 0.2.3-scrubbed (rig copy, has /ask),
ExcelGuildGate 0.3.3 with 1 of 5 seals broken (same as live), ExcelMenu 0.1.5 with the live Java
server_menu.yml e18910e4. Fake Bedrock player (bedrock-protocol, offline name, all-zero Floodgate UUID).
Boot: Enabling ExcelBedrockMenu v1.5.22, 0 SEVERE (tests/rig/rig-boot-1.5.22.log).
* a (8): /menu opens "Server Menu" with exactly the 22 buttons in the 1.5.22 order; raw labels start
  with §1/§5/§2/§n/§8 per group (My Cosmetics §2, Ask the Guide §n, Maker's mark §8); text under the code
  is the plain label; no mcMMO Stats button. My Cosmetics runs /cosmetics: with nothing owned
  ExcelCosmetics says so in chat; after a rig grant (Pink color, 1 week) it opens its own "Cosmetics"
  Bedrock form, no chest (tests/rig/bedrock-cosmetics-form-captured.json).
* b (5): Ask the Guide opens a custom_form titled "Ask the Guide" with ONE input and the example
  placeholder (bedrock-ask-form-captured.json). Send "How do I claim land?" -> "[Valley Guide] Hold a golden
  shovel ..." in chat. Blank Send -> "No question typed ..." and nothing runs. Closing -> nothing.
* c (6): Maker's mark opens ExcelMakersMark's own form (Maker's mark: ON / Gear stats: ON / Close,
  bedrock-mark-form-captured.json); tapping the switch flips it ("Maker's mark is OFF ...") and the reopened
  form shows the new state (flipped back afterwards). Skills & Perks shows "Magic is awake: 1 of 5 seals
  broken. Get your first wand from the Old Quartermaster in the spawn village..." with buttons My mcMMO
  stats / Skill Trees on the Website / Close; My mcMMO stats prints the mcMMO stats page.
* d (2): with excelguide.ask false: "You can't ask the Guide right now..." and no text box; with
  excelmakersmark.use false: "You can't use Maker's mark right now..." and no form. (excelcosmetics.use
  false changes nothing: ExcelCosmetics does not check it, so My Cosmetics behaves like the Java button;
  logged as info.) Permissions unset afterwards.
* e (3, optional Java fix): fixed skills_perks.yml loaded by `excelmenu reload` (skills_perks 6 items);
  ExcelMenu's own Bedrock render of that Java menu shows "Magic (Awake)", "Seals broken: 1 of 5" (placeholder
  resolved), "First wand: the Old Quartermaster" (excelmenu-skills-perks-form-captured.json).
* ExcelMenuStats clicks.csv (tests/rig/bed122-clicks.txt): new taps logged as server_menu/cosmetics and
  server_menu/ask (same item ids as the Java buttons), "Ask the Guide"/"Send"; the typed question never
  appears (0 hits for the question text). "Maker's mark and Gear stats" logs under its plain label (the
  ExcelMenuStats bedrock-map has no entry for it yet: stats-only follow-up, see README).

### Honest notes
* First runs found one real bug, fixed before the final jar: ExcelGuide's /ask has no permission on the
  command (it checks excelguide.ask inside), so the first guard let a refused player open the text box.
  1.5.22 now checks excelguide.ask too (unit + rig phase d).
* First phase c failed because the rig had an older ExcelMakersMark build (c73d7656) with no Bedrock form,
  and my test expected toggles; the LIVE jar (7490006e) uses a 3-button form. Rerun with the live jar passed.
* The test client hit "Checksum mismatch" and the server's login throttle on some sessions, so each phase is
  a short separate session with retries (failed tries kept in tests/rig/bed122-attempts/, none of them a
  server error). Final passing phases: a, c and d at 00:29-00:40 and b at 00:47 and e at 00:51 AM CDT, all
  on jar 1d860fbb.

## NOT tested
* A real Bedrock device: the text box and on-screen keyboard, how the longer 22-button list scrolls, and
  the colors (Shops green and Help copper are weak on gray, same as 1.5.21). Mockups are drawn from source.
* ExcelGuide 0.3.2 (live) on the rig: the rig has 0.2.3-scrubbed. Both register /ask with the same name,
  usage and excelguide.ask node (plugin.yml compared); 0.3.2 answers the same command.
* Live LuckPerms setup and live player data.
