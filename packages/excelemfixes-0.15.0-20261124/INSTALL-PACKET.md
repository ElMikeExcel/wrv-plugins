# INSTALL PACKET — ExcelEMFixes 0.15.0 (skills-menu: native dual-format skill-bonus screens)

**Filed:** Nov 24, 2026 · **Lane:** em-native-skills-menu-20261124 · **Decision:** D225
**Commission:** Mike, Nov 24, 2026, 3:19 PM CT — "Chest menus don't really work for bedrock well. Can't we do the same we did for our Excel menu where the appropriate format shows for both?" The proposed `onlyUseBedrockMenus` config flip is DECLINED by Mike — do NOT file or apply that change anywhere. Design source: `~/workspace/em-menu-replacement-20261124/FEASIBILITY-DESIGN.md` (Tier 3).

## What this ships

One new module, **skills-menu** (`/skillmenu`, alias `/skills`): our own native screen for EliteMobs' skill bonuses, following the pack's menu law — ONE menu definition rendered per platform by the shared renderer (Java chest GUI; Bedrock native Cumulus SimpleForm via the pack's Geyser reflection bridge — the ExcelMenu / ExcelBedrockMenu pattern; no new dependency).

- **Overview:** every skill (Armor, Swords, Axes, Bows, Crossbows, Tridents, Hoes, Maces, Spears, Staves, Wands) with the player's level, XP, and active-bonus count; Close chrome (root).
- **Per-skill screen:** every bonus with unlock tier (10 / 25 / 50 / 75), locked vs unlocked by level, active state carried in the button name itself (Bedrock forms have no hover lore); Back on every screen; after a toggle the screen refreshes in place (the renderer's new `openReplace` — Back still lands on the overview).
- **Toggles are EliteMobs' own:** the bridge performs EM's exact menu-click sequence (bytecode-read from the live jar): level from `SkillXPCalculator.levelFromTotalXP(PlayerData.getSkillXP)` → locked: EM's requires-level message → active: `PlayerSkillSelection.removeActiveSkill` + EM's deactivated message + `SkillBonus.onDeactivate` → full (3, EM's `MAX_ACTIVE_SKILLS`): EM's max-active message → else `PlayerSkillSelection.addActiveSkill` + EM's activated message + `SkillBonus.onActivate`. State lives in EM's own store; nothing is forged.
- **Entry points:** `/skillmenu` (alias `/skills`), plus a new "My skill bonuses" button + route on `/emguide`'s Skills page (bundled clarity-menus.yml: routes 10 → 11; the How-to text now leads with the native screen and keeps the alt-path as the fallback note; the old Claim button stays, relabeled "Claim skill bonuses (EliteMobs' screens)").
- **Fallback:** EM absent or its classes refuse at runtime → the menu degrades to EM's own chest claim screens (the 0.13.0 clarity route), then to a printed manual path. Never an error in a player's face.
- README + config carry the REASON (Jester's Nov 24 report + Mike's Bedrock-format direction), per the module workflow rule. No tuning knobs — levels, tiers, cap, and messages are read from EM live.

## Base + ordering (READ FIRST)

**Base tree:** ExcelEMFixes **0.14.0** (the food-stack-normalize build). The tree was copied from the food lane's build tree on Nov 24, 2026 (~21:22 CT), and at end of build the `foodstack` package was **re-diffed and re-synced** from the food lane's final tree — including the food lane's late fix that restores the food component through the classic meta API instead of a data-component removal. 0.15.0 therefore contains 0.14.0 entire (food-stack-normalize included) plus skills-menu.

**ORDERING: chains on 0.14.0 — install the 0.14.0 entry FIRST (which itself chains on 0.13.0). If 0.14.0 is not live, STOP. Do not re-base onto 0.13.0 or 0.12.0.**

## Artifacts

| File | Size | SHA-256 |
|---|---|---|
| `ExcelEMFixes-0.15.0.jar` (this folder) | 490,593 B | `f93678f0ed586d2cb20036d3506941c31ca5f38a3975411c1f7d39c66a86619c` |
| `deploy-config.yml` (this folder) | 69,861 B | `68602aa85027063a55964626f074972ec6e8c5a59aca2e3743443577dabfdc87` |

Jar: deterministic double-build byte-identical. Deploy config = the 0.14.0 packet's deploy-config (562e6cd8…) + exactly the appended skills-menu section (42 lines; diff-verified — no other delta).

## Gates (STOP on any mismatch; re-pull fresh — hashes are the gate)

1. Live jar is `ExcelEMFixes-0.14.0.jar`, SHA-256 `aa466af721f0f7930441973fee87dc7bb14e1d0d04145fed32fd264836b46cf0` (474,197 B) — the 0.14.0 packet's recorded hash.
2. Live `plugins/ExcelEMFixes/config.yml` SHA-256 `562e6cd8b24775cc17c08741fda189c7a52177d02a080488e953a2dc754f1053` (67,766 B) — the 0.14.0 packet's deploy-config as placed.
   - **Fallback if this gate fails:** do NOT place the deploy config. Hand-append ONLY the skills-menu section (the block at the end of this packet's deploy-config.yml, from the `# MODULE: skills-menu` banner to the end of the file) to the live config, and record the deviation.
3. Live `plugins/ExcelEMFixes/` contains **NO** `clarity-menus.yml` (positive listing check — the bundled text carries this build's wording; if an override appeared since the 0.13.0 window, STOP and reconcile the text before proceeding).

## Steps

1. Verified-empty window (standing rule). Backup: copy the live jar + `config.yml` to the window's backup names (`*.bak-<date>-pre0150`).
2. Server down. **MOVE** (never copy) the outgoing `ExcelEMFixes-0.14.0.jar` to its backup name; place `ExcelEMFixes-0.15.0.jar`. Exactly-one-ExcelEMFixes-jar census before Start.
3. Place `deploy-config.yml` as `plugins/ExcelEMFixes/config.yml` (or the hand-append fallback above). No other file placements — the clarity text is bundled in the jar.
4. Start. **Acceptance (all must hold):**
   - Boot line: `ExcelEMFixes 0.15.0 ready` with **21 modules** (0.14.0's 20 + skills-menu).
   - `Clarity Menus ready — 5 pages, 11 routes.`
   - `Skills Menu ready — native skill-bonus screens (Java GUI + Bedrock form).`
   - `emfixes reload` → ZERO reload-failed; SEVERE count 0; server reaches Done and stays up.
5. Write the marker: `~/workspace/em-native-skills-menu-20261124/packet/INSTALLED.txt` (window id, timestamp, end-state player count).

## Rollback

Restore the backup jar (MOVE back) + backup config, restart. The module is additive: with 0.14.0 restored, `/skillmenu` ceases to exist and nothing else changes. Skill-bonus selections players made through the new screen live in EliteMobs' own store and remain valid under EM's own screens — no data rollback exists or is needed.

## Proof behind this packet

- **Suites:** 1,119 passed / 0 failed on the final tree (43 new SkillsMenuModel checks: toggle-plan truth table mirroring EM's handler order, overview/per-skill view shape, Back/Close law, sort order, Bedrock-readable state in button names, action codec). One known intermittent failure may appear in other runs: the pre-existing SkillNudgeMath tie-break flake (salted `Map.of` iteration in untouched code; the pristine 0.14.0 tree fails it ~3 of 4 runs; root cause identified Nov 24, 2026 — not this build's change, not fixed here to keep the diff clean).
- **Rig proof (Java, Mineflayer bot, Paper 26.1.2 rig clone + live EM jar; re-run against the final jar):** boot lines exact (21 modules incl. both ready lines above); bot (ARMOR set to 30 via EM's own admin command) opened `/skillmenu` → overview listed all 11 skills with levels → Armor screen listed all 10 Armor bonuses with real EM descriptions/effects, tiers 10/25 unlocked, 50/75 locked with levels named → tapped Evasion → EM's own chat message "Activated skill: Evasion", button refreshed in place to "[ON] Evasion" → **server-side read-back in EM's own database: `SkillBonusSelections = {"ARMOR":["armor_evasion"]}`** → tapped again → "Deactivated skill: Evasion", selections back to `{}`. Back navigation and the `/emguide` Skills-page route (hint + overview opens) also exercised.
- **Bedrock form — PROOF OWED (do not mark proven):** the rig could not run Geyser inside this VM (Geyser 1305 requires Paper 26.3 — obtained and booted — but its Microsoft discovery fetch cannot traverse this sandbox's egress: the endpoint answers via the proxy with curl, Geyser's in-JVM fetch ignores proxy settings and direct egress is blocked; on Mike's PC, Geyser runs normally). The form path itself is the pack's shared production renderer — the same code that renders /emguide, /progress and every other Excel menu as native forms on the live server today — and the skills views are unit-tested with state readable from button names alone. **Post-install proof: Iris drives the Bedrock client on Mike's PC (PC agent), joins as .MikeExcel, and verifies the form renders and a bonus toggle reads back active in EM's player_data.db; Marvin's live form walk is a bonus confirmation, not the gating proof** (see SURFACES.md).

## Proof owed after install

1. **Jester (Java):** /emguide → Skills → "My skill bonuses" → toggle one bonus on and off; confirm EM's own alt-menu screen shows the same state (the two screens must agree — they read the same store).
2. **Bedrock form (Iris, via Mike's PC):** drive the PC's Bedrock client (PC agent), join the live server as .MikeExcel, open `/skillmenu` — a native form (not a container) renders; buttons show levels and [ON] state; a toggle performs the EM activation and reads back active server-side in EM's player_data.db. Marvin's own form walk afterwards is a bonus confirmation, not gating.
3. Records close-out (wrv-plugins draft PR + Drive deposit + website row) per the standing pattern — supervisor chain.
