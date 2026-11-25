# BUILD RECORD — ExcelEMFixes 0.15.0 · skills-menu (native dual-format skill-bonus screens)

**Built:** Nov 24, 2026 · **Status: BUILT + FILED (PENDING in MANIFEST) — NOT INSTALLED**
**Commission:** Mike, Nov 24, 2026, 3:19 PM CT. **Decision:** D225.
**Design source:** `~/workspace/em-menu-replacement-20261124/FEASIBILITY-DESIGN.md` (Tier 3, commissioned after Mike declined the `onlyUseBedrockMenus` flip — that flip is NOT filed or applied anywhere).

## What was built

A new toggleable module **skills-menu** in ExcelEMFixes, plus one additive method in the shared renderer and a clarity-text update:

- `skillsmenu/SkillsMenuModel.java` — pure view model (no Bukkit, no EM types): overview + per-skill views from plain records; the toggle-plan truth table mirroring EM's handler order (locked → active-off → capped → on); bonus sort = tier, required level, name; state readable from button names alone (the Bedrock-form rule).
- `skillsmenu/EmSkillsBridge.java` — the ONLY class touching EM skill types. Reads via EM's public statics; toggle performs EM's exact click sequence (bytecode-verified against `SkillBonusMenu$SkillBonusMenuEvents` in the live jar): level via `SkillXPCalculator.levelFromTotalXP(PlayerData.getSkillXP)`; EM's own messages from `SkillBonusMenuConfig` with EM's own substitutions; `PlayerSkillSelection.addActiveSkill/removeActiveSkill`; `SkillBonus.onActivate/onDeactivate` via the registry.
- `skillsmenu/SkillsMenuModule.java` — `/skillmenu` (alias `/skills`), action handler `skillsmenu`, fallback chain native → EM chest claim screens (clarity's 0.13.0 opener) → printed manual path; enables without EM and guards every bridge call with try/catch(Throwable).
- `shared/MenuRenderer.openReplace` — additive: replaces the stack's top view for post-toggle refresh (Back still lands below; on Bedrock the re-render is what keeps the form alive). No existing caller changed.
- Wiring: ModuleRegistry +1 module (21 total); VERSION + plugin.yml → 0.15.0 (description gains the module sentence); config block `modules.skills-menu` (REASON comments, no knobs) in src + deploy configs; README section after food-stack-normalize.
- Clarity text (bundled `clarity-menus.yml`, src + deploy copies identical): new route `my-bonuses` (command `skillmenu`) → routes 10 → **11**; Skills page leads with a "My skill bonuses" button; the How-to detail now teaches the native screen first and keeps the alt-path (with the "click the skill itself" step) as the fallback note; the EM Claim button stays, relabeled "(EliteMobs' screens)". Boot line becomes "Clarity Menus ready — 5 pages, 11 routes."

**Mechanism (as tasked to state):** the same mechanism as every Excel menu — screens are built ONCE as the shared `Views` view model and rendered by `shared/MenuRenderer`: a Bukkit chest inventory for Java, or a Cumulus `SimpleForm` through `shared/GeyserForms` (reflection into Geyser's Cumulus, the ExcelBedrockMenu pattern) when the player's UUID is a floodgate UUID. Chosen because it is the pack's production-proven split, needs no new dependency, and one definition guarantees both platforms show the same skills, states, and actions.

## Base tree + version

- **Base: the food lane's ExcelEMFixes 0.14.0 tree** (`~/workspace/emfood-stacksplit-20261124/build-root`), copied Nov 24, 2026 ~21:22 CT (their jar at copy time: 474,138 B / 7c6753ef…). **Version: 0.15.0.**
- **End-of-build re-sync (required by the task):** the food lane changed `foodstack/FoodStackNormalizeModule.java` after the copy (rig-found fix: restore the food component via the classic meta API — a data-component removal would have recorded an explicit deletion and made items inedible). The whole `foodstack` package was re-diffed and re-synced from their final tree before the final build; every other diff vs their tree is this build's own files. Their final filed jar is 474,197 B / aa466af7… — 0.15.0's provenance gates chain on exactly that (see packet).

## Verification

- **Suites:** final tree **1,119 passed / 0 failed** (`run-tests.sh`; 43 new SkillsMenuModelTest checks). Other runs intermittently show 1 failure — the pre-existing SkillNudgeMath tie-break check, root-caused during this build: the test feeds `Map.of(...)` (salted iteration order per JVM launch) into a first-wins tie-break. Proven not-mine: the pristine 0.14.0 tree failed it 3 of 4 consecutive runs. Left unfixed here to keep this build's diff clean; flagged for the parent.
- **Deterministic build:** two consecutive `build.sh` runs byte-identical.
  - Jar: **ExcelEMFixes-0.15.0.jar — 490,593 B — SHA-256 f93678f0ed586d2cb20036d3506941c31ca5f38a3975411c1f7d39c66a86619c**
  - Deploy config: 69,861 B — SHA-256 68602aa85027063a55964626f074972ec6e8c5a59aca2e3743443577dabfdc87 (= 0.14.0 packet config + skills-menu section only, diff-verified)
- **Rig proof — Java (PASS, run twice; second run against the final jar):** own rig clone (Paper 26.1.2, live EM 10.9.14 jar, port 25597 — the food lane's rig on 25598 untouched). Boot: "Clarity Menus ready — 5 pages, 11 routes." + "Skills Menu ready — native skill-bonus screens (Java GUI + Bedrock form)." Mineflayer bot, ARMOR set to 30 via EM's own `/em skill set`:
  1. `/skillmenu` → overview "My Skills & Skill Bonuses": all 11 skills with levels (Armor — Lv 30), Close at the nav slot.
  2. Armor → "Armor Bonuses": all 10 real Armor bonuses with EM descriptions and formatted effects ("32.0% Damage Reduction" at Lv 30), tiers 10/25 open, "(level 50)"/"(level 75)" locked, Back present.
  3. Tap Evasion → chat **"Activated skill: Evasion"** (EM's own message), screen refreshed in place: "[ON] Evasion", lime dye.
  4. **Server-side read-back (EM's own SQLite, `plugins/EliteMobs/data/player_data.db`): `SkillBonusSelections = {"ARMOR":["armor_evasion"]}`** — EM registered it through its own persistence.
  5. Tap again → "Deactivated skill: Evasion", selections back to `{}`.
  6. Back → overview; `/emguide` → Skills → "My skill bonuses" (route hint printed) → overview opens.
- **Rig proof — Bedrock (COULD NOT RUN in this VM; proof owed to Marvin's live walk):** Geyser 2.12.1-1305 + floodgate were added to the rig clone. Geyser refuses the rig's Paper 26.1.2 ("requires 26.3") → obtained Paper 26.3-256 locally (the migration rig's cached jars) and booted 26.3 cleanly with the plugin's ready lines → Geyser then fails its startup encryption/discovery step: `EncryptionUtils.getDiscoveryData` cannot reach the Microsoft discovery endpoint from the JVM (endpoint answers HTTP 200 via the sandbox proxy with curl; Geyser's fetch ignores JVM proxy properties and direct egress is blocked in this sandbox). On Mike's PC Geyser runs normally, so this is a build-VM limit, not a build defect. The form branch is the pack's shared production renderer (the same code serving Marvin native forms for /emguide etc. on live today), and the model is unit-tested for form-readable state. **Named proof-owed in the packet + SURFACES: Marvin's post-install Bedrock form walk.** The scripted Bedrock client built for the attempt (bedrock-protocol + locally compiled raknet-native) is kept at `rig/bedrock-bot/formbot.js` if a future lane wants it where Geyser can start.

## Rig incident (disclosed)

While swapping the rig's server jar, `cp` followed a symlink chain (`rig/paper.jar` → foodheal-ooc rig → `emfoodheal-build-20261120/rig/paper.jar`) and overwrote that Nov 20 lane's Paper 26.1.2 launcher with the 26.3 jar. **Restored byte-exact within the same session:** three independent fleet rigs carry the identical 26.1.2 launcher (52,900,210 B, SHA-256 8bb8e8ec…); a copy was restored to `emfoodheal-build-20261120/rig/paper.jar` and the hash verified. My rig now holds its own real 26.3 jar file (no symlink). No live system was touched (all rigs are offline clones).

## Filing

- Packet: `packet/INSTALL-PACKET.md` + `packet/SURFACES.md` + jar + deploy-config.yml (this folder).
- MANIFEST: PENDING entry appended after the 0.14.0 entry (chains on it). **Not installed** — the failsafe/supervisor installs at the next verified-empty window in chain order.
- Decision log: **D225**. Daily log: one line in `~/memory/2026-11-24.md`.

## Proof owed after install

Jester — Java click-through via /emguide (both screens agree). Marvin — Bedrock form walk (the owed Bedrock proof). Records close-out (draft PR + Drive + website row) — supervisor chain.

## Addendum — Bedrock proof plan changed (Nov 24, 2026, 4:18 PM CT)

Mike pointed out that he has Minecraft Bedrock on his PC, so the owed Bedrock form proof does **not** wait on Marvin. New plan: post-install, Iris drives the Bedrock client on Mike's PC (PC agent), joins the live server as .MikeExcel, opens /skillmenu, and verifies the Cumulus form renders and a bonus toggle performs the EM activation and reads back active server-side in EM's player_data.db — the same read-back standard as the Java rig proof above. Marvin's own form walk becomes a bonus confirmation, not the gating proof. Packet (INSTALL-PACKET.md, SURFACES.md) and the MANIFEST pointer phrase amended the same day to match; the rig-proof section above stands as the record of why the build VM could not run this proof.
