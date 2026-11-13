# INSTALL PACKET — ExcelSkillsPanel 0.2.1 (gated jar-only swap, ON TOP of 0.2.0) — DRAFT

**Status: DRAFT — NOT filed.** Desk build Nov 13, 2026 (menu
programme Items 9 + 10). The board supervisor verifies this
return, re-bases the G1 live-premise gates below with a FRESH
live pull at filing time, and files the MANIFEST entry; the
failsafe owns the install. Do not install from this draft
directly.

**SEQUENCING (hard): 0.2.1 installs only after 0.2.0 is
live.** 0.2.1 is built on the 0.2.0 tree and its swap premise
is the 0.2.0 jar. If 0.2.0 has not landed when this packet is
picked up, this packet waits — do not swap 0.2.1 onto a live
0.1.1 or 0.1.0 jar under this packet (the premise gate below
will STOP it).

**Item:** replace `plugins/ExcelSkillsPanel-0.2.0.jar` with
`plugins/ExcelSkillsPanel-0.2.1.jar` — 57,652 B, SHA-256
`ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c`,
build `EXC-SKILLSPANEL-0.2.1-WRV-20261113-01-e2f021`.
Jar-only swap: no config, no data (the plugin is read-only
and writes nothing), no other plugin touched.
**Class:** gated jar install — emptiness gate, one window
backup + cap-at-6, restart. The window's risk surface is the
restart itself.

**What the player sees change:** (a) the panel header no
longer shows a standalone "Magic" line — the Magic block is
titled by its "Magic Level N — X XP total" line, with Rank /
Class / Next rank / Mana / Boosts / casts / seals following
exactly once, on both surfaces. (b) Every progress bar's
FILLED portion is gold: on Java the filled ▰ segments render
in gold (empty segments stay gray); on Bedrock the same split
rides the form text as gold/gray codes. (c) Every skill's
detail ladder gains, under each perk's name/unlock line, a
short effect description in the mcMMO fork's own words — all
82 perks across all 19 skills (e.g. Taming → Beast Lore:
"Bone-whacking inspects wolves & ocelots"; Call of the Wild:
"Summon an animal to your side"). Earned/locked markers,
unlocks, and ranks are unchanged. Everything else is 0.2.0.

**Fail-soft the installer should know about:** unchanged from
0.2.0 — the panel verifies each skill's ladder against the
LIVE mcMMO data (RankConfig / SubSkillType) at panel-open
time. If the live skillranks have drifted from the verified
table, that skill alone falls back to the website pointer
line and a WARNING logs — that is designed behaviour, not an
install failure; report any such warning in the install
report verbatim. (Descriptions are part of the verified
table; a drifted skill shows neither ladder nor descriptions.)

## Live-premise gates (G1 — SUPERVISOR TO CONFIRM with a fresh pull at filing; STOP on mismatch)

1. **Live panel jar is the shipped 0.2.0:**
   `plugins/ExcelSkillsPanel-0.2.0.jar` = 53,816 B,
   SHA-256
   `f513eaf5992aea09c08763d1c653193b3d012f2a2f3b6824dccaa929be22bc45`
   (the 0.2.0 build of record, Nov 13, 2026). This gate IS
   the sequencing gate: a live 0.1.1/0.1.0 jar (or no panel
   jar) means 0.2.0 has not landed — STOP, install 0.2.0
   first.
2. **No 0.2.1 jar already live:** no
   `plugins/ExcelSkillsPanel-0.2.1.jar` present before the
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
4. **Description premises (new in 0.2.1, verified at build
   time):** the descriptions compiled into 0.2.1 are the live
   fork's own locale values — all 93
   `<Skill>.SubSkill.<Perk>.Description` keys in the fork
   source locale are byte-equal to the same keys inside the
   live mcMMO jar gated in (3) (per-perk citations:
   DESCRIPTIONS-SOURCE.md in the build folder). No separate
   live pull is needed beyond gate (3)'s jar identity: if the
   live jar is `4276bd3e…f5c`, the description source is the
   live fork's text by construction.
5. **Provider premises unchanged since 0.2.0's filing:**
   Magic live jar `Magic-11.2.5-wrvnpe1.jar`, ExcelMagicLevel
   0.2.0 + PlaceholderAPI enabled, ExcelGuildGate live,
   Geyser-Spigot + Floodgate enabled — all as in the 0.2.0
   packet. Supervisor confirms by the same fresh pull; drift
   in any of them does not block the swap by itself (the
   panel reads them fail-soft) but must be RECORDED.
6. **Command premise:** `/skillpanel` still registered by
   ExcelSkillsPanel alone on the live plugin list.

## Install steps (for the filing/installer)

1. Emptiness gate ×2 (verified empty), window backup first,
   cap-at-6 (name prunees in the install report).
2. Remove `plugins/ExcelSkillsPanel-0.2.0.jar`; place
   `ExcelSkillsPanel-0.2.1.jar` into `plugins/` (PC rclone
   route primary; panel fallback per lane rules). Verify the
   placed file's SHA-256 ==
   `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c`.
3. Restart. Verify in the boot log: `Enabling
   ExcelSkillsPanel v0.2.1`; the enable line
   `ExcelSkillsPanel 0.2.1 awake (build
   EXC-SKILLSPANEL-0.2.1-WRV-20261113-01-e2f021); providers:
   mcmmo, magic`; plugin count unchanged vs the pre-window
   census (a swap, not an addition); SEVERE 0; and NO
   "does not match the verified table" WARNING from
   ExcelSkillsPanel (its presence means premise 3 drifted —
   record it verbatim in the install report).
4. Player proof (Mike's glance — the items' acceptance):
   `/skillpanel` → the header shows NO standalone "Magic"
   line (the Magic Level line titles the block); bars show
   gold filled portions on Bedrock AND Java; a skill detail
   (Taming is the quick read) shows each perk's description
   under its line — Beast Lore and Call of the Wild named in
   Item 10 — with earned/locked states still correct at his
   levels.
5. Surfaces: this swap changes in-panel content only. The
   website Skill Trees page remains the long-form
   descriptions surface and needs no change for the swap
   itself; the filing supervisor names any Guide / What's New
   surface it judges owed at filing time. KNOWN SEPARATE
   DEFECT (not this packet): the website Skill Trees page and
   INVENTORY.md still list Flux Mining as working —
   LADDER-DATA §5 flags that correction as its own close-out
   item for the supervisor.

## Rollback

Remove `plugins/ExcelSkillsPanel-0.2.1.jar`, restore
`plugins/ExcelSkillsPanel-0.2.0.jar` (53,816 B /
`f513eaf5…45`, from the 0.2.0 build folder / records /
Drive), restart. The plugin writes no data and changes no
other plugin's state; there is nothing else to restore.
