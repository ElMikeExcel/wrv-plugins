# INSTALL PACKET — ExcelSkillsPanel 0.1.1 (gated jar-only swap) — DRAFT

**Status: DRAFT — NOT filed.** Desk build Nov 13, 2026 (menu
programme Item 7). The board supervisor verifies this return,
re-bases the G1 live-premise gates below with a FRESH live
pull at filing time, and files the MANIFEST entry; the
failsafe owns the install. Do not install from this draft
directly.

**Item:** replace `plugins/ExcelSkillsPanel-0.1.0.jar` with
`plugins/ExcelSkillsPanel-0.1.1.jar` — 45,739 B, SHA-256
`084b110ddcc044f32f837e355f54d54ee274a80899d0375cad85bb1535e643c2`,
build `EXC-SKILLSPANEL-0.1.1-WRV-20261113-01-e1f011`.
Jar-only swap: no config, no data (the plugin is read-only
and writes nothing), no other plugin touched.
**Class:** gated jar install — emptiness gate, one window
backup + cap-at-6, restart. The window's risk surface is the
restart itself.

**What the player sees change (Mike's Nov 13 rulings):**
the Power Level header is one line ("Power Level 579"); the
"Power Level cap: 2,147,483,647" line is gone; the boosts
line rounds to 2 decimals ("+0.59%" not "+0.5943%"); the
percent sign renders on Bedrock forms (U+FF05 in form bodies;
Java/chat keep ASCII "%"). Everything else is 0.1.0.

## Live-premise gates (G1 — SUPERVISOR TO CONFIRM with a fresh pull at filing; STOP on mismatch)

1. **Live panel jar is the shipped 0.1.0:**
   `plugins/ExcelSkillsPanel-0.1.0.jar` = 45,497 B,
   SHA-256
   `4f9f819d75a4e9fba098cbc1ade42e2d01265d678948993f918083125d23cb19`
   (the installed build of record, Nov 12, 2026, 11:48 PM
   CST). Carried premise from the build record — the
   supervisor re-pulls fresh at filing; a different live jar
   means the swap premise is wrong — STOP and re-verify.
2. **No 0.1.1 jar already live:** no
   `plugins/ExcelSkillsPanel-0.1.1.jar` present before the
   swap (a duplicate-plugin load otherwise).
3. **Provider premises unchanged since 0.1.0's filing:**
   mcMMO live jar is the patched
   `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` (3,695,180 B /
   `4276bd3e…f5c`), Magic live jar is
   `Magic-11.2.5-wrvnpe1.jar` (7,662,464 B / `b78f03b8…999`),
   ExcelMagicLevel 0.2.0 + PlaceholderAPI enabled,
   ExcelGuildGate live (0.3.4 of record), Geyser-Spigot +
   Floodgate enabled — all as re-based in the 0.1.0 packet
   (run 276). Supervisor confirms by the same fresh pull;
   drift in any of them does not block the swap by itself
   (the panel reads them fail-soft) but must be RECORDED.
4. **Command premise:** `/skillpanel` still registered by
   ExcelSkillsPanel alone on the live plugin list.

## Install steps (for the filing/installer)

1. Emptiness gate ×2 (verified empty), window backup first,
   cap-at-6 (name prunees in the install report).
2. Remove `plugins/ExcelSkillsPanel-0.1.0.jar`; place
   `ExcelSkillsPanel-0.1.1.jar` into `plugins/` (PC rclone
   route primary; panel fallback per lane rules). Verify the
   placed file's SHA-256 ==
   `084b110ddcc044f32f837e355f54d54ee274a80899d0375cad85bb1535e643c2`.
3. Restart. Verify in the boot log: `Enabling
   ExcelSkillsPanel v0.1.1`; the enable line
   `ExcelSkillsPanel 0.1.1 awake (build
   EXC-SKILLSPANEL-0.1.1-WRV-20261113-01-e1f011); providers:
   mcmmo, magic`; plugin count unchanged vs the pre-window
   census (a swap, not an addition); SEVERE 0.
4. Player proof (Mike's glance — the item's acceptance):
   Bedrock — `/skillpanel` shows ONE "Power Level 579" line,
   no cap line, boosts like "+0.59%", and a visible percent
   sign after the bar numbers (main panel + a skill detail);
   Java — the chest shows the same cleaned header (Java pass
   still owed from 0.1.0; this glance can close both).
5. Surfaces: this is a display-only cleanup of an existing
   panel — no Guide/website/What's New content changes are
   owed by the swap itself; the filing supervisor names any
   surface it judges owed at filing time.

## Rollback

Remove `plugins/ExcelSkillsPanel-0.1.1.jar`, restore
`plugins/ExcelSkillsPanel-0.1.0.jar` (45,497 B /
`4f9f819d…cb19`, from the 0.1.0 build folder / records /
Drive), restart. The plugin writes no data and changes no
other plugin's state; there is nothing else to restore.
