## PENDING — ExcelSkillsPanel 0.2.2 (Menu Programme Items 12 + 13: no Class line; header follows the carried wand) — DRAFT for the board supervisor to file

**Status of this draft:** written by the re-dispatch build lane Nov 13,
2026. **NOT YET FILED — the G1 live-premise receipts below are LABELED
TO-BE-PULLED.** The supervisor runs the pulls at filing (same-day rclone
pull, or labeled `FALLBACK (panel)` per PACKET-TEMPLATE.md) and fills
the receipt lines before filing. Do NOT file on the carried 03:54-window
premise alone; the installing window re-pulls fresh and STOPs on any
mismatch.

- **Packet:** `~/workspace/skillspanel-022-build-20261113/INSTALL-PACKET.md`
- **Build record:** `~/workspace/skillspanel-022-build-20261113/BUILD-RECORD.md`
- **Candidate:** `~/workspace/skillspanel-022-build-20261113/ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar` — 62,142 B / SHA-256 `374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e`; build `EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9`; JUnit 38/38; rig phase A 71/0, phase B 71/0 (real Paper 26.3 + Magic 11.2.5; held / not-held / two-wand scenarios; Bedrock body checks). Repro jar identical hash. `SHA256SUMS` in the build folder (6/6 verified).
- **Live-premise receipts (G1) — TO BE PULLED AT FILING:**
  - Live `plugins/ExcelSkillsPanel-0.2.1.jar` is 57,652 B / `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c` (expected from the 03:54 CST Nov 13 landing pull-back) — receipt: `<same-day pull path + size + SHA-256 + pull timestamp — SUPERVISOR TO FILL>`.
  - Exactly one ExcelSkillsPanel jar live; no `ExcelSkillsPanel-0.2.2.jar` live before the swap — receipt: `<live plugins/ listing — SUPERVISOR TO FILL>`.
  - Live Magic is 11.2.5; ExcelMagicLevel 0.2.0; ExcelMagicGate 0.3.2; mcMMO 2.3.003-WRV-XP-2; ExcelGuildGate live; PlaceholderAPI 2.12.3 — receipt: `<same-day pulls — SUPERVISOR TO FILL>`.
  - Live `plugins/ExcelMagicLevel/config.yml` readable (reward table) — receipt: `<hash on pull — SUPERVISOR TO FILL>`.
- **Live-jar gate (STOP on mismatch):** outgoing live jar must equal the G1 receipt above at window start (fresh pull); placed jar pull-back must equal 62,142 B / `374ad117…`; outgoing preserved in place as `ExcelSkillsPanel-0.2.1.jar.bak-20261113-pre022`.
- **Scope:** jar swap only — no config/data/player/world changes. Item 12 removes the "Class: Mage" line on all surfaces; Item 13 reads rank/next/mana/regen from the carried wand (WRV order: Apprentice → Master, never "Wizard"), mana as the effective pool, casts from MagicGate's ledger labelled "Credited casts". No plugin config of its own.
- **Gates:** marker discipline with re-checks before Shut Down and before Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or the two newest manuals); emptiness `list` = 0/10 twice.
- **Boot verify:** `Enabling ExcelSkillsPanel v0.2.2` + awake line for build `EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9` (`providers: mcmmo, magic`); plugins 84 (swap, no count change); SEVERE 0; guild LOCKED 4 of 5 unchanged; NO "does not match the verified table" WARNING.
- **Same-pass surfaces (supervisor legs, owed):** Guide skills-panel chunk, website pair, What's New BOTH surfaces (website + in-game changelog entry for 0.2.2), broadcaster tips check, in-game menus check. Records publication (wrv-plugins draft PR) is a supervisor leg — G2 duplicate check before any push.
- **Proof owed after install:** Mike — open `/skillpanel` while holding food (wand NOT in hand) on Java + Bedrock: Rank Apprentice, Next rank Master, Mana 150 / 150, regen 25, Credited casts 1,215, no Class line. (At his level 26 the effective pool is 150/150; the rig's 154/154 is the level-136 fixture's boosted pool.) No live two-wand check owed by this packet (rig guard only; registry question is a separate supervisor check).
- **Marker on install:** `~/workspace/skillspanel-022-build-20261113/INSTALLED.txt`
