# INSTALL PACKET — ExcelSkillsPanel 0.1.0 (NEW plugin, gated jar install)

**Status: STAGED, NOT FILED.** The board supervisor verifies
this build's return and files the MANIFEST entry. Do not
install from this packet directly.

**Item:** `plugins/ExcelSkillsPanel-0.1.0.jar` — NEW file (no
live file is replaced). 45,497 B, SHA-256
`4f9f819d75a4e9fba098cbc1ade42e2d01265d678948993f918083125d23cb19`,
build `EXC-SKILLSPANEL-0.1.0-WRV-20261112-01-c3d9a1`.
**Class:** gated jar install — emptiness gate, one window
backup + cap-at-6, restart. The plugin is READ-ONLY (writes
no data files, keeps no state), so the window's risk surface
is the restart itself.
**Sequencing note:** DESIGN §10 lands Phases 2+3 in ONE gated
window — this jar alongside the ExcelMenu 3.0 jar (skills-hook
delegation + magiclevel bundled retirement) and the data edits
(skills entry action → `[player] skillpanel`, skills_perks
`magic:` item → the panel door, entries/magiclevel.yml
deletion). Those are the menu programme's own packet pieces;
this packet covers ONLY the panel jar. The panel is fully
usable via `/skillpanel` on its own if the window splits.

## Live-premise gates (G1 — pull fresh at filing/install; STOP on mismatch)

None of these were verified against the live server by this
build; each needs a same-day live pull receipt (PC rclone
route, or a labelled FALLBACK (panel) receipt) before filing:

1. **mcMMO live-jar identity:** the live mcMMO jar is the WRV
   fork build `2.3.003-WRV-XP-2` this build compiles and
   rig-proves against (build-side copy: 3,730,752 B, SHA-256
   `5f6279995c7b30240d2a7e07cb916b6e28b82537826f9ef0f2702b080cf20978`).
   A different mcMMO build changes the API surface the
   provider reads — STOP and re-verify.
2. **Magic live jar:** Magic 11.2.5 (build-side copy:
   7,641,369 B, SHA-256
   `79e49fe32fdc4e25375c0099d2bd10665b9792bfd9b35fab8d64a498ebfc3532`).
3. **ExcelMagicLevel live:** 0.2.0 enabled, with PlaceholderAPI
   live so its `%excelmagiclevel_*%` expansion registers (the
   panel's Magic Level/XP surface depends on it).
4. **ExcelGuildGate live:** a build whose GuildGateAPI carries
   `sealsBrokenCount` (rig-proven against 0.4.0). If absent,
   the panel still installs — the seals line simply omits —
   but the premise should be RECORDED either way.
5. **Command collision re-check:** no live plugin registers
   `/skillpanel` (build-side check covered the Magic, mcMMO,
   ExcelMagicLevel, ExcelGuildGate jars + ExcelMenu sources;
   re-check the live plugin list at filing).
6. **Bedrock lane live:** Geyser-Spigot + Floodgate enabled
   (the form presentation's premise; Java is unaffected).

## Install steps (for the filing/installer)

1. Emptiness gate ×2 (verified empty), window backup first,
   cap-at-6 (name prunees in the install report).
2. Place `ExcelSkillsPanel-0.1.0.jar` into `plugins/`
   (PC rclone route primary; panel fallback per lane rules).
   Verify the placed file's SHA-256 ==
   `4f9f819d75a4e9fba098cbc1ade42e2d01265d678948993f918083125d23cb19`.
3. Restart. Verify in the boot log: `Enabling ExcelSkillsPanel
   v0.1.0`; the enable line `ExcelSkillsPanel 0.1.0 awake
   (build EXC-SKILLSPANEL-0.1.0-WRV-20261112-01-c3d9a1);
   providers: mcmmo, magic`; plugin count +1 vs the pre-window
   census; SEVERE 0.
4. Player proofs (Mike's live pass): Java — `/skillpanel`
   opens the chest; header shows Power Level + the Magic
   block (Magic Level, "Rank: <his wand's path name>",
   absolute mana, boosts, "Total casts", "Guild seals: N of
   5"); the 19 skill rows show levels + bars matching
   /mcstats, Salvage/Smelting show as child skills; a skill
   tap opens its detail; the Magic header tap opens the
   explainer detail. Bedrock — the panel form opens with the
   header body + compact lines; a skill button opens its
   detail form; Back returns; the Magic details button works.
   Cross-check one skill's bar numbers against /mcstats and
   the Magic block against /magiclevel.
5. Surfaces in the same pass (menu programme packet): Guide,
   website pair, What's New BOTH surfaces, tips check, menus
   check — owned by the Phase 2+3 packet, named here so the
   panel's landing is not reported done without them.

## Rollback

Remove `plugins/ExcelSkillsPanel-0.1.0.jar` and restart. The
plugin writes no data and changes no other plugin's state;
there is nothing else to restore. If it was installed with
the Phase 2+3 packet, the packet's own .bak set + prior
ExcelMenu jar govern the rest of the rollback.
