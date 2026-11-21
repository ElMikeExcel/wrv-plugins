# ExcelQuestXP 1.1.0 — BUILD RECORD

**Feature (D159, Mike's direction Nov 20, 2026):** player-chosen quest XP target.
Players pick where their earned quest XP goes — a command and a popup menu —
instead of the plugin always paying their current highest weapon skill.

**Base:** ExcelQuestXP 1.0.1 (live, installed Nov 20 window 0029), source at
`~/workspace/questxp-101-20261119/src/ExcelQuestXP`.

## Plan (phases)

1. **Read the base.** SkillGrant.java + GrantPolicy.java (shared grant path:
   live `QuestCompleteEvent` listener and `/questxp backpay` use it verbatim —
   1.1.0 must keep that property). Read the 1.0.1 command map, plugin.yml,
   tests, and the rig.
2. **Survey a sibling Excel plugin** for the house menu pattern (Java chest
   GUI + Floodgate form for Bedrock), e.g. ExcelDowned / ExcelShops /
   ExcelAtlas builds in the workspace. Match its approach/libraries.
3. **Design + implement:**
   - Per-player target: mode `HIGHEST` (default, exactly today's behavior) or
     a pinned weapon skill (any of `SkillType.getWeaponSkills()`).
   - Storage: `plugins/ExcelQuestXP/targets.yml`, per UUID; loaded on enable,
     saved on change, survives restarts.
   - `SkillGrant` resolves the target at grant time; Armor stays floor(xp/3);
     grant messages/audit lines name the skill as today, with the pinned
     choice visible in the audit line.
   - Commands: `/questxp target` (show), `/questxp target highest`,
     `/questxp target <skillname>`; `/questxp menu` (and bare `/questxp` if
     no collision) opens the popup. Player-level permission only.
   - Popup: Java chest GUI ("Highest weapon skill (auto)" + one entry per
     weapon skill, current level/XP shown, active choice marked, Close
     affordance); Bedrock = Floodgate form with the same choices as buttons.
     Selecting sets the target, confirms in chat, closes.
4. **Proof:** full existing suite passes (old always-highest assertions get a
   HIGHEST-mode framing, not deletion); new tests for pinned grant routing,
   targets-file persistence across reload, invalid skill name refusal, Armor
   split unchanged in pinned mode. Mirror-rig proof via the 1.0.1 quest rig
   (one HIGHEST grant + one PINNED grant, log lines cited) if the rig can
   drive a grant. Double-build byte-identical.
5. **Deliverables:** `ExcelQuestXP-1.1.0.jar`, source zip, this record,
   INSTALL-PACKET.md (written early). Build only — no MANIFEST filing, no
   install; the supervisor chain files it.

## RENAME — D160 (Mike, Nov 20, 2026, 10:43 AM CT)

EliteMobs-related plugins are named with an **ExcelEM** prefix. This build
ships as **ExcelEMQuestXP**, not ExcelQuestXP:

- plugin.yml `name:` = `ExcelEMQuestXP`; jar = `ExcelEMQuestXP-1.1.0.jar`;
  BuildInfo/build identity strings updated; boot line names ExcelEMQuestXP 1.1.0.
- The player command STAYS `/questxp` (plugin renamed, command not).
- Java package stays `com.winkreyes.excelquestxp` (internal, invisible).
- **Data-folder migration (critical):** Bukkit keys the data folder off the
  plugin name — the new plugin reads `plugins/ExcelEMQuestXP/` while ALL live
  state sits in `plugins/ExcelQuestXP/` (backpay.yml — restaged Nov 20
  morning, sha256 24274c55…; backpay-paid.yml — the exactly-once paid ledger
  for BOTH players; grants.log; config). The install packet carries the
  migration: copy the whole live folder byte-verified at swap time, remove
  the old 1.0.1 jar in the same swap, and the post-install proof includes
  the paid-ledger check (Marvin 13, Jester 33 already paid; 0 owed for
  both) so the rename can never cause a double payment. The old folder
  stays in place as migration source/backup for the window.
- `targets.yml` (this build's new per-player target store) lives in the NEW
  folder: `plugins/ExcelEMQuestXP/targets.yml`.

**Migration proof plan:** (a) the mirror rig boots the 1.1.0 jar as
ExcelEMQuestXP against a data folder copied from the 1.0.1 rig's
`plugins/ExcelQuestXP/` (which holds a real paid ledger: RigA 2 quests,
RigB 1 quest) and re-running `/questxp backpay RigA` pays 0 — the migrated
ledger is honored; (b) the packet's post-install proof repeats the same
check live for Marvin (13) and Jester (33).

## Phase log

- [x] Phase 1 — base read (SkillGrant/GrantPolicy/main/listener/plugin.yml/
  build + test scripts; command map = reload + backpay, both admin; bare
  /questxp prints a status line, no player subcommands exist in 1.0.1)
- [x] Phase 2 — sibling menu survey (ExcelDowned 1.0.3: Java chest GUI via
  InventoryHolder slot→action map + click/drag/close listeners; Bedrock via
  reflection Geyser/Cumulus with chat fallback. ExcelShops 1.3.5:
  BedrockForms.choices() — Cumulus SimpleForm button list over the same
  reflection bridge. 1.1.0 follows: Downed holder pattern for Java,
  Shops choices() shape for Bedrock SimpleForm.)
- [x] Phase 3 — implementation (TargetStore + TargetPolicy + TargetMenu +
  BedrockForms new; GrantPolicy/SkillGrant/ExcelQuestXP/BuildInfo/
  plugin.yml modified; command map extended, admin surfaces untouched)
- [x] Phase 4 — tests + rig + double-build (267/267; rig A 21/0, B 12/0;
  double-build byte-identical)
- [x] Phase 5 — packet + record close-out

## Live premise (pulled Nov 20, 2026, ~10:43 AM CT, Homebase route)

- Live `plugins/ExcelQuestXP-1.0.1.jar` = 31,728 B / SHA-256
  `28e5dbf122423947034c356b0616a71e259bf1be3aef2044e090f6f223ae37da`
  (rclone copyto via mike-pc → scp → local sha256sum; byte-identical to
  the 1.0.1 build of record in `~/workspace/questxp-101-20261119/`).

## Design notes

**Storage.** `plugins/ExcelEMQuestXP/targets.yml` (the NEW data folder —
see the rename section), SnakeYAML like the other tables:
`targets: { <uuid>: <SKILL_ENUM_NAME> }`. Loaded on enable, saved on
every change. No entry = HIGHEST. Bad UUID keys / unknown skill names
are dropped at load and counted; the player falls back to HIGHEST. A
load failure never disables the plugin (logged SEVERE, everyone on
HIGHEST) — a preference file must not take the grant bridge down.

**Grant resolution (at grant time, in SkillGrant — the one shared path,
so live grants and backpay grants resolve identically):**
`GrantPolicy.resolveTargetIndex(weaponXp, pinnedIndex)` — a pin (index
into `SkillType.getWeaponSkills()`) wins even over a higher skill;
no/stale pin defers to `highestWeaponIndex`, the pre-1.1.0 function,
unchanged. Armor stays `floor(xp/3)` in both modes. Audit visibility:
the console grant line and the grants.log line both carry
`target=highest` or `target=pinned:<ENUM>` (inside the matchedBy parens
on the console line, as its own `| target=` field in grants.log).

**Command syntax (exact):**
- `/questxp target` — show the sender's current target.
- `/questxp target highest` (also `auto` / `default`) — clear the pin.
- `/questxp target <skill>` — pin. Accepted input (case-insensitive,
  spaces/underscores ignored): enum names (`WANDS`), display names
  (`Wands`), singular aliases (`wand`, `sword`, `axe`, `bow`,
  `crossbow`, `trident`, `hoe`, `mace`, `spear`, `staff`/`stave`).
  Anything else is refused with the full skill list + a pointer to the
  menu; nothing is stored. `armor` never resolves (not a weapon skill).
- `/questxp menu` — the popup. Bare `/questxp` from a player also opens
  it (1.0.1's bare command printed a status line; that line now serves
  console senders and the unknown-subcommand fallback). From console,
  `target`/`menu` answer with a short in-game-only note.
- `/questxp reload` / `/questxp backpay <player>` — unchanged, still
  `excelquestxp.admin` (op). Reload now also reloads targets.yml.
- Permissions: the 1.0.1 plugin.yml put `excelquestxp.admin` on the
  COMMAND itself, which locked players out of /questxp entirely. 1.1.0
  removes the command-level permission; `target`/`menu` check the new
  `excelquestxp.target` node (default: true — every player), admin
  subcommands keep their in-code admin checks exactly as 1.0.1.

**Popup contents (both doors, same order).** Title "Quest XP Target".
Entry 0: "Highest weapon skill (auto)". Entries 1–10: one per weapon
skill (SWORDS, AXES, BOWS, CROSSBOWS, TRIDENTS, HOES, MACES, SPEARS,
STAVES, WANDS) showing the player's current level (and XP on Java).
Last: Close. The active choice is marked "(current)" on Bedrock;
on Java with a ✔ name prefix, a "► Currently selected" lore line and
an enchant glint. Picking a choice applies it immediately
(`applyTargetChoice` — the same method the typed command uses),
confirms in chat, and closes. Java door: 27-slot chest GUI, Downed
holder pattern (slot→action map, clicks/drags cancelled). Bedrock
door: Cumulus SimpleForm via the Shops-pattern reflection bridge
(public API interfaces through Geyser's own classloader; NO compile-
time Geyser/Cumulus dependency; softdepend Geyser-Spigot + floodgate
added). Bedrock detection: Geyser connection, else Floodgate
isFloodgatePlayer; any failure falls back to a chat message naming the
typed commands — the typed path never breaks. Skill icons (Java):
iron sword/axe, bow, crossbow, trident, iron hoe, mace, spear
(matchMaterial — present on the live version), stick (staves),
blaze rod (wands).

**Rename mechanics in code.** plugin.yml `name: ExcelEMQuestXP`,
`version: 1.1.0`; BuildInfo VERSION 1.1.0 / BUILD_ID
EXC-EMQUESTXP-1.1.0-WRV-20261120-01; boot line "ExcelEMQuestXP 1.1.0
ready — ..." (rig-cited below). Main class and package unchanged
(`com.winkreyes.excelquestxp.ExcelQuestXP`) per D160 instructions.
The bundled backpay.yml is unchanged from 1.0.1 — saveResource never
overwrites, and the packet's migration copy is what the live plugin
actually reads.

## Test totals

Full suite via run-tests.sh (log: tests-110.log), exit 0 — **267/267**:
- Run A (no EliteMobs jar at runtime): QuestXpTestMain 42/42 (the
  pre-existing suite unmodified except a framing comment over the
  highest-* checks — they now pin HIGHEST mode, the function
  resolveTargetIndex defers to), BackpayTestMain 35/35,
  TargetTestMain 34/34 (new: pinned-beats-higher resolution, stale-pin
  fallback, Armor split target-independent, 16 input-resolution checks
  incl. armor/garbage refusal, store default/persist/clear/corrupt).
- Run B (pinned EliteMobs jar): EmApiTestMain 45/45 (39 pre-existing +
  6 new pinning the live weapon order and TargetPolicy against the
  REAL enum/display names), QuestXpTestMain 42/42, BackpayTestMain
  35/35, TargetTestMain 34/34.

**Rig** (rig/, TargetRig driver; the 1.0.1 rig server with the
packet's migration performed on it — old jar out, 1.1.0 jar in,
plugins/ExcelQuestXP/ copied byte-identical to
plugins/ExcelEMQuestXP/, paid-ledger hash match e8a5efa8…):
- Phase A — **RIGOBS TALLY pass=21 fail=0** (rig-phaseA.log). Boot:
  "ExcelEMQuestXP 1.1.0 ready — build EXC-EMQUESTXP-1.1.0-WRV-20261120-01;
  quests in table: 112; total XP across table: 130355; backpay players:
  4; backpay already paid: 3; XP targets pinned: 0; pinned EliteMobs
  10.9.14." Migration proof: console `/questxp backpay RigA` → both
  quests "already paid — skipping", "paid 0 quest(s), +0 XP", RigA
  still 90/28, no new BACKPAY lines — the renamed plugin honored the
  migrated ledger. Grants: HIGHEST main_quest_1 → SWORDS 20/armor 6,
  grants.log `target=highest`; pin as the (non-op) player via
  `/questxp target wands` → targets.yml holds RigT→WANDS;
  `/questxp target banana` refused, pin unchanged; PINNED main_quest_2
  → WANDS 20 (SWORDS highest at 20 untouched), armor 12, grants.log
  `target=pinned:WANDS`. Menu dispatch (informational): `/questxp menu`
  and bare `/questxp` both returned true for the fake player.
- Phase B (restart) — **RIGOBS TALLY pass=12 fail=0**
  (rig-phaseB.log). Boot shows "XP targets pinned: 1" (pin loaded from
  targets.yml). RigT totals + WANDS pin survived; main_quest_3 (480)
  still routed PINNED → WANDS 500, armor 172; `/questxp target highest`
  cleared the pin from targets.yml; ag_welcome_quest_1 (50) then paid
  HIGHEST → WANDS (the highest at 500) 550, armor 188, `target=highest`.
- The only WARN lines in either boot are the expected saveResource
  "already exists" notices for the migrated quest-xp.yml/backpay.yml.
  SEVERE 0 attributable to ExcelEMQuestXP in both phases.

## Hashes

- ExcelEMQuestXP-1.1.0.jar — 50,603 B — SHA-256
  `cab1574471c50ace591bc12603afbd42272c06aec8c4cc78a3b660ae112b670e`
  (double-build byte-identical; SHA256SUMS.txt in this folder)
- ExcelEMQuestXP-1.1.0-source.zip — 53,486 B — SHA-256
  `279baa56d2a83b9c79027c9deb3a1ba233083a0f5ded412a2de2a5dce34fb9cb`

## Deliverables in this folder

ExcelEMQuestXP-1.1.0.jar · ExcelEMQuestXP-1.1.0-source.zip ·
BUILD-RECORD.md (this file) · INSTALL-PACKET.md · SHA256SUMS.txt ·
tests-110.log · src/ExcelEMQuestXP/ (full source + build/test scripts) ·
rig/ (TargetRig driver source + scripts + rig-phaseA.log +
rig-phaseB.log + the rig server as run). NOT filed in the MANIFEST and
NOT installed — the supervisor chain files it from the packet's
pointer stub.
