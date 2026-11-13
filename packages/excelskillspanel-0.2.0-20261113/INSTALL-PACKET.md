# INSTALL PACKET — ExcelSkillsPanel 0.2.0 (gated jar-only swap, ON TOP of 0.1.1) — DRAFT

**Status: DRAFT — NOT filed.** Desk build Nov 13, 2026 (menu
programme Item 8). The board supervisor verifies this return,
re-bases the G1 live-premise gates below with a FRESH live
pull at filing time, and files the MANIFEST entry; the
failsafe owns the install. Do not install from this draft
directly.

**SEQUENCING (hard): 0.2.0 installs only after 0.1.1 is
live.** 0.2.0 is built on the 0.1.1 tree and its swap premise
is the 0.1.1 jar. If 0.1.1 has not landed when this packet is
picked up, this packet waits — do not swap 0.2.0 onto a live
0.1.0 jar under this packet (the premise gate below will
STOP it).

**Item:** replace `plugins/ExcelSkillsPanel-0.1.1.jar` with
`plugins/ExcelSkillsPanel-0.2.0.jar` — 53,816 B, SHA-256
`f513eaf5992aea09c08763d1c653193b3d012f2a2f3b6824dccaa929be22bc45`,
build `EXC-SKILLSPANEL-0.2.0-WRV-20261113-01-e2f020`.
Jar-only swap: no config, no data (the plugin is read-only
and writes nothing), no other plugin touched.
**Class:** gated jar install — emptiness gate, one window
backup + cap-at-6, restart. The window's risk surface is the
restart itself.

**What the player sees change:** every skill's detail view
(Java chest lore + Bedrock form) now ends with its perk
ladder — "Perks:" then one line per perk: "✓ Dodge — Lv 1 ·
Rank 1 of 1" when earned, "✗ … — Lv 100 · Rank 0 of 10" when
not, "✓ Roll — always available" for no-rank perks — 82
perks across all 19 skills, computed at the player's level
(the derived level for Salvage/Smelting, whose explainer
lines stay above their ladders). The website pointer line no
longer appears on a verified ladder. Full perk descriptions
stay on the website Skill Trees page, unchanged. Flux Mining
appears nowhere. Everything else is 0.1.1.

**Fail-soft the installer should know about:** the panel
verifies each skill's ladder against the LIVE mcMMO data
(RankConfig / SubSkillType) at panel-open time. If the live
skillranks have drifted from the verified table, that skill
alone falls back to the website pointer line and a WARNING
logs — that is designed behaviour, not an install failure;
report any such warning in the install report verbatim.

## Live-premise gates (G1 — SUPERVISOR TO CONFIRM with a fresh pull at filing; STOP on mismatch)

1. **Live panel jar is the shipped 0.1.1:**
   `plugins/ExcelSkillsPanel-0.1.1.jar` = 45,739 B,
   SHA-256
   `084b110ddcc044f32f837e355f54d54ee274a80899d0375cad85bb1535e643c2`
   (the 0.1.1 build of record, Nov 13, 2026). This gate IS
   the sequencing gate: a live 0.1.0 jar (or no panel jar)
   means 0.1.1 has not landed — STOP, install 0.1.1 first.
2. **No 0.2.0 jar already live:** no
   `plugins/ExcelSkillsPanel-0.2.0.jar` present before the
   swap (a duplicate-plugin load otherwise).
3. **Ladder premises (receipted Nov 13, 2026, bm278 —
   supervisor re-confirms at filing):** live mcMMO jar is
   `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` (3,695,180 B /
   `4276bd3e…f5c`); live `plugins/mcMMO/skillranks.yml`
   VALUE-EQUAL to the jar-embedded `fd7cc0f1…` table the
   ladders were built from; live `plugins/mcMMO/config.yml`
   carries `General.RetroMode.Enabled: true`. A drift in the
   skillranks/mode premises does not corrupt anything (the
   runtime gate fails soft per skill) but changes what
   players will see — STOP and re-verify with the supervisor
   before filing, because the packet's player-visible claims
   assume the verified table is the live table.
4. **Provider premises unchanged since 0.1.1's filing:**
   Magic live jar `Magic-11.2.5-wrvnpe1.jar`, ExcelMagicLevel
   0.2.0 + PlaceholderAPI enabled, ExcelGuildGate live,
   Geyser-Spigot + Floodgate enabled — all as in the 0.1.1
   packet. Supervisor confirms by the same fresh pull; drift
   in any of them does not block the swap by itself (the
   panel reads them fail-soft) but must be RECORDED.
5. **Command premise:** `/skillpanel` still registered by
   ExcelSkillsPanel alone on the live plugin list.

## Install steps (for the filing/installer)

1. Emptiness gate ×2 (verified empty), window backup first,
   cap-at-6 (name prunees in the install report).
2. Remove `plugins/ExcelSkillsPanel-0.1.1.jar`; place
   `ExcelSkillsPanel-0.2.0.jar` into `plugins/` (PC rclone
   route primary; panel fallback per lane rules). Verify the
   placed file's SHA-256 ==
   `f513eaf5992aea09c08763d1c653193b3d012f2a2f3b6824dccaa929be22bc45`.
3. Restart. Verify in the boot log: `Enabling
   ExcelSkillsPanel v0.2.0`; the enable line
   `ExcelSkillsPanel 0.2.0 awake (build
   EXC-SKILLSPANEL-0.2.0-WRV-20261113-01-e2f020); providers:
   mcmmo, magic`; plugin count unchanged vs the pre-window
   census (a swap, not an addition); SEVERE 0; and NO
   "does not match the verified table" WARNING from
   ExcelSkillsPanel (its presence means premise 3 drifted —
   record it verbatim in the install report).
4. Player proof (Mike's glance — the item's acceptance):
   `/skillpanel` → a skill detail shows the "Perks:" ladder
   with ✓/✗ lines at his levels (Acrobatics is the quick
   read); Salvage + Smelting details show the explainer above
   the ladder and NO Flux Mining under Smelting; Bedrock —
   the same detail form renders the ladder lines and the
   ✓/✗/· glyphs legibly on the phone (the glyphs' on-phone
   look is this glance's new surface); Java — the chest lore
   shows the same ladder (the Java pass can also close the
   0.1.0/0.1.1 Java glance still owed).
5. Surfaces: this swap changes in-panel content only. The
   website Skill Trees page remains the descriptions surface
   and needs no change for the swap itself; the filing
   supervisor names any Guide / What's New surface it judges
   owed at filing time. KNOWN SEPARATE DEFECT (not this
   packet): the website Skill Trees page and INVENTORY.md
   still list Flux Mining as working — LADDER-DATA §5 flags
   that correction as its own close-out item for the
   supervisor.

## Rollback

Remove `plugins/ExcelSkillsPanel-0.2.0.jar`, restore
`plugins/ExcelSkillsPanel-0.1.1.jar` (45,739 B /
`084b110d…c2`, from the 0.1.1 build folder / records /
Drive), restart. The plugin writes no data and changes no
other plugin's state; there is nothing else to restore.
