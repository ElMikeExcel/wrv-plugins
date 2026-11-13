# ExcelMenu 3.0.0 — FILING GATE 2 RIG REPORT (R2 desk rig + R3 mirror rig)

**Lane:** board supervisor run 292 dispatch, Nov 13, 2026.
**Candidate:** `ExcelMenu-3.0.0.jar` — 329,425 B / SHA-256
`306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51`
(re-verified on the PC after transfer: hash exact).
**Design of record:** `~/workspace/excelmenu-30-design-20261113/DESIGN.md`
§7 (R2 = 18-case matrix; R3 = cases 1, 4, 5, 7, 8, 10, 11 in brief on
the PC test-server mirror). Rig work only: nothing was installed on,
edited on, or restarted on the live server; no MANIFEST entry was
filed or touched.

**Headline finding (read first):** the §6.2 data set **as designed
(J2 Rearrange fixed item at page-2 slot 8) does not load** — the
layout loader refuses it statically, the whole `server_menu` layout
fails, and four definitions cascade-fail (§3 below). The design's
own pre-named fallback — **§9 Q7: slot 17** — was applied to the rig
seed and every subsequent boot/harness result in this report is
green. **The filing must place the Rearrange fixed item at page-2
slot 17, not slot 8** (or the loader/build must change — a separate
build decision; this lane did not patch the jar).

---

## 1. Fresh-pull receipts (G1-grade, PC route)

Pulled Nov 13, 2026, **06:34–06:39 CST** via rclone on Mike's PC
(DESKTOP-IL05DQR, SSH attempt 1) from the live server
(`shockbyte:1. Michael's Server/plugins/…`) into
`C:\Users\MikeExcel\muse-pulls\bm292-20261113\`, then copied to this
VM (`rig-bm292/pulls/`) and **re-hashed locally — every hash below
matches on both machines**. One transient SFTP auth retry occurred
during the data-folder copy (rclone attempt 2/3 succeeded); no data
was affected. Full per-file receipts for all 94 entries are in the
pull listing kept with the run record; key receipts:

| Live path | Size | SHA-256 |
|---|---|---|
| `plugins/ExcelMenu-2.0.0.jar` (live jar; 2.0.1 content) | 287,906 | `c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a` |
| `plugins/ExcelMenu/config.yml` | 2,218 | `d14d9cb14f3bf53552a57bdda25b5d13ad813ea3ce492bd31ef07e4958283eb7` |
| `plugins/ExcelMenu/layouts/server_menu.yml` | 6,959 | `fceb14331f02036dd0d9e6283b061a7d4acbd3094d658bd1ab9f37425a76e510` |
| `plugins/ExcelMenu/layouts/commands_menu.yml` | 4,956 | `146db1d91c4d34c49d7f43c1fd55f2eb80a6979215365b29442848ea47403bcb` |
| `plugins/ExcelMenu/entries/skills.yml` | 438 | `ccc527640bbc3f142be9fd2a5fc6cc85b1ebae4a2181a218bc8b77d3915743f1` |
| `plugins/ExcelMenu/entries/magiclevel.yml` | 2,778 | `6d367005f4d91f3dcbbcb6ee634e4e036b9c279ec9ed4a8e1b5849f81d0e81c1` |
| `plugins/ExcelMenu/menus/skills_perks.yml` | 1,957 | `5df3f0a3fbe5b73ca69047c387cc1e293ce3882a2705d4cb199b6aee591e4c72` |
| `plugins/ExcelSkillsPanel-0.2.1.jar` (live panel) | 57,652 | `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c` |
| `plugins/ExcelMagicLevel-0.2.0.jar` | 63,270 | `892e05fc5d4176b82fd30117098f8c037cb4e3e56325b9cdc0d1f70cd80231c3` |
| `plugins/geyser.jar` (live Geyser-Spigot) | 52,209,051 | `4e5740267f8f7e1c0b5d56213e047cfdba57a93c322ace63c05286a5d486c66c` |
| `plugins/floodgate.jar` | 11,561,096 | `21570aff9ce17d6983928e8552777760e1ede5050026b04c686b0ae112e6fd7e` |
| `plugins/PlaceholderAPI-2.12.3.jar` | 1,160,690 | `fde03259f5af6938f3c33eeb4d814000a1adabf1d2304ce14970be81f609a437` |
| `plugins/Magic-11.2.5-wrvnpe1.jar` (MagicLevel hard depend) | 7,662,464 | `b78f03b83fe7a298f3d7cfde100392ba693cfc2e312322827d3dd556454b3999` |
| `plugins/Vault-2.0.0.jar` | 14,425 | `8b2ce431c88741fcae6d4de6032ab2ab0cca6aa86e8af375bdfe654321822a68` |
| `plugins/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` | 3,695,180 | `4276bd3e07bdaf60c4edbb5461c1c4a61c3252e24f15f324faaa58d7300dff5c` |

All G-e premises re-proved byte-exact by this pull (live jar =
2.0.1 content per G-a; both layouts, config, skills at their
recorded hashes). Entries census: **94 live `.yml`** in the pull.
Note for the filing: the live panel is now **ExcelSkillsPanel
0.2.1** (the design's presence-only hook is version-blind per G-c;
no interaction).

## 2. Rig seeds (the §6.2 data set)

`make-seeds.py` built two seeds from the pull:

- **seed-r0** — the live data folder, unedited (94 entries) — the
  R0 control seed.
- **seed-r2** — seed-r0 + the DESIGN §6.2 data set:
  1. `layouts/server_menu.yml`: `pinned: [spawn, tpmenu]` added;
     page-2 Rearrange fixed item (COMPARATOR, `&b&lRearrange Menu`,
     action `['[player] rearrange']`) — at **slot 17** (see §3;
     the as-designed slot-8 variant is preserved as
     `slot8-failing-server_menu.yml`). Final: 7,182 B /
     `9af8ddefa386837c1a6e8073be5e1a14285e0679f8bc4158571e819e13a27f6d`.
  2. `config.yml`: merge-edit appending only the §3.7 `rearrange:`
     knob block. Final: 2,564 B /
     `910c2f834215ded57a4f24576f06eba6f8f5fb8017227f63106fba2d5f31c3be`.
  3. `menus/skills_perks.yml`: `magic:` item → Magic Level door
     (name `&d&lMagic Level`, §4.3 lore, clicks
     `[player] skillpanel` + `[close]`). Final: 1,969 B /
     `236f1cd26bbb1294d3f342d9fa93b3c5a34565689eb4da94e3e87bf5da08bfe0`.
  4. `entries/magiclevel.yml` **deleted** (93 entries remain).

## 3. The slot-8 finding (as-designed placement FAILS to load)

Desk-rig boot `boot-r2-panel.log` (3.0.0 jar + §6.2 seed with the
Rearrange item at page-2 **slot 8**, exactly as DESIGN §3.4/§6.2
specify):

```
[ExcelMenu] ExcelMenu layout FAILED [server_menu]: server_menu page 2:
  slot 8 for group 'chest_tools' collides with a fixed item
[ExcelMenu] ExcelMenu definition FAILED [details_settings]: … target
  'server_menu' is not a loaded menu …
[ExcelMenu] ExcelMenu definition FAILED [makersmark_settings]: (same)
[ExcelMenu] ExcelMenu definition FAILED [server_menu]: (same class)
[ExcelMenu] ExcelMenu definition FAILED [skills_perks]: (same)
[ExcelMenu] ExcelMenu 3.0.0 enabled … 93 entries, 1 registry menus,
  10 definition menus live.
```

Cause: `LayoutLoader` validates fixed-vs-group slots **statically**
— page 2's `chest_tools` slot list is `[1..8]`, so a fixed item at
slot 8 collides at load time no matter how few entries resolve.
DESIGN §3.4's reasoning ("the fixed item does not reserve slot 8
against the cursor; the collision materialises only if an 8th
chest_tools entry ever resolves") describes `ChestModel`'s runtime
paint order, but the loader refuses the layout before ChestModel
ever runs. §9 Q7 pre-named the fallback — *"fallback slot 17 if it
has grown at filing"* — and slot 17 is in no page-2 slot list.
Re-seeded at slot 17, the identical data set loads perfectly
(§4). This is a **data-placement correction inside the design's own
fallback**, not a jar defect: no code change is required to file,
but the filing packet must write slot 17 (and should record why).

## 4. Boot evidence (all rigs)

Paper 26.3 (desk: 26.3-235 skeleton from the 2.0.1 rig; mirror:
26.3-245 from the PC test-server mirror), Geyser + floodgate,
live-pulled companions (ExcelSkillsPanel 0.2.1, ExcelMagicLevel
0.2.0, Magic, mcMMO, Vault, PlaceholderAPI).

| Boot | Log | Enable line | SEVERE |
|---|---|---|---|
| R0 control — desk, 2.0.1 jar (live-pulled), seed-r0, panel present | `boot-r0-control.log` | `ExcelMenu 2.0.1 enabled … 94 entries, 2 registry menus, 13 definition menus live.` Done (50.256s) | **0** |
| R2 as-designed slot 8 — desk, 3.0.0, seed slot-8 | `boot-r2-panel.log` | layout FAILED (§3); `93 entries, 1 registry menus, 10 definition menus` | 0 (errors are ERROR-level layout/definition failures) |
| **R2 — desk, 3.0.0, seed-r2 (slot 17), panel present** | `boot-r2-panel-slot17.log` | **`ExcelMenu 3.0.0 enabled … 93 entries, 2 registry menus, 13 definition menus live.`** Done (48.395s); bridge **PRESENT**; reload re-verified 93/0-failed, 13 menus | **0** |
| R2 restart-2 — same desk data folder booted again, no reseed | `boot-r2-restart2.log` | `ExcelMenu 3.0.0 enabled … 93 / 2 / 13`; **`entries/magiclevel.yml` still absent after the restart** (no extraction resurrection, §5.3) | **0** |
| R2 panel-absent — desk, 3.0.0, seed-r2, panel jar removed | `boot-r2-nopanel.log` | `ExcelMenu 3.0.0 enabled … 93 / 2 / 13`, bridge PRESENT (7 plugins) | **0** |
| **R3 — PC mirror, 3.0.0, seed-r2, panel present, real pulled geyser.jar + floodgate.jar** | `r3-panelpresent-rotated.log.gz`, `r3-boot-out.log` | **`ExcelMenu 3.0.0 enabled … 93 entries, 2 registry menus, 13 definition menus live.`** Done (14.828s); bridge **PRESENT**; plugin list (9) all green; magiclevel.yml absent after boot | **0** |
| R3 panel-absent — PC mirror, panel jar held aside | `r3-nopanel-latest.log` | `ExcelMenu 3.0.0 enabled … 93 / 2 / 13`, self-check loads 13 incl. `skills_perks` (fallback surface), bridge PRESENT, Done (13.699s); `skillpanel` → *Unknown or incomplete command* (command genuinely absent with the panel) | **0** |

Console/RCON proofs (desk slot-17 boot + mirror, identical):
`/rearrange` → *"§e/rearrange is a player command."* (the 3.0
command is registered; **no collision** with any live-pulled plugin
in the set — gate 4's plugin-set half CLOSED); `skillpanel` →
*"The skills panel is a player screen — run /skillpanel in game."*
(panel present); `magiclevel` → ExcelMagicLevel usage line (the
typed command still answers, case 10). Mirror rig was stopped via
RCON after the runs; no java process remains on the PC.

R0↔R2 model diff (harness, §5): picker gains the two pins +
Rearrange and loses nothing else; Travel form 5 entries + Back →
3 entries + Back; Me 14 entries + Back → 13 + Back; Java page 1
identical except the Me row compacts one slot from `magiclevel`'s
old slot 11 on; page 2 gains only the slot-17 Rearrange item.
**No other slot, button, or order changed.**

## 5. R2 case-by-case (DESIGN §7 R2)

Method key: **boot** = real Paper rig boot/console; **model** =
`RigHarness.java` driving the 3.0.0 pure core against the live
seed (`harness-r2.log`: **26 PASS / 0 FAIL**; R0 control
`harness-r0.log`: all PASS); **client** = needs a real
Java/Bedrock client — none exists on either rig, so those halves
are NOT EXERCISED rather than fabricated (they are exactly
DESIGN §7's post-install Mike proofs).

| # | Case | Verdict | Evidence |
|---|---|---|---|
| 1 | Bedrock picker open | **PASS** | Model: picker buttons = `[§1Spawn→Entry(spawn)], [§1Teleport to a player→Entry(tpmenu)], [Travel], [Me], [Shops], [Help], [Settings], [Chest Organization], [Rearrange Menu→Editor]` — pins first with FLAT-rule group-colour labels, groups in layout order, Rearrange LAST, **no Back**. Exactly the §7 shape. |
| 2 | Pin tap — Spawn | **NOT EXERCISED** (client) | Model half proven: the pin is an `EntryTarget` for the same resolved `spawn` entry whose action is `[[openguimenu] teleport_confirm_spawn]` — identical dispatch object to the Travel-form button in R0, so the tap path is the R0-proven flow by construction. The in-game tap + confirmation flow needs a Bedrock client. |
| 3 | Pin tap — Teleport | **NOT EXERCISED** (client) | Same construction: pin `EntryTarget(tpmenu)`, resolved action `[[close], [player] tpmenu]` = the R0 Travel-form action verbatim. In-game tap not exercised. |
| 4 | Travel group form | **PASS** | Model: `[Party, Adventurers Guild, EliteMobs, Back→Picker]` — Spawn/Teleport absent, Back LAST. R0 control: `[Spawn, Teleport, Party, Guild, EliteMobs, Back]` (the designed delta, nothing else). |
| 5 | Every other group form + one commands_menu group | **PASS** | Model: me=14 btn (13+Back), shops=4, help=6, settings=7, chest_tools=4 — all end `Back→PickerTarget`; commands_menu `getting_around`=7 btn, Back last; commands_menu picker carries **no pins and no Rearrange** (scope proof). Matches R0 control except the designed Me delta. |
| 6 | Java chest, pages 1–2 | **PASS** | Model slot maps printed in `harness-r2.log`: page 1 identical to R0 except Me compaction (magiclevel out); page 2 = chestfilter/filterrecover/maxorg at 1–3, **slot 17 = Rearrange fixed item** (COMPARATOR, action `[player] rearrange`), Back arrow at 53. (Slot 8 as designed FAILS — §3; slot 17 is the Q7 fallback.) |
| 7 | Bedrock `skills` tap, panel present | **NOT EXERCISED** (client) | Supporting evidence, all real: panel jar (live-pulled 0.2.1) enabled on both rigs; `skills` entry data = `[close]` + `[player] skillpanel` with `requires-plugin: ExcelSkillsPanel`; console `skillpanel` proves the command exists and is player-bound; the hook bypass itself is the build's unit-covered code path. The Bedrock form tap that exercises `ScreenHooks.tryOpen` needs a Bedrock client. |
| 8 | Bedrock `skills` tap, panel removed | **NOT EXERCISED** (client) | Fallback state proven at boot on **both** rigs: panel-absent boots are 93/2/13, SEVERE 0, `skills_perks` definition loaded (the fallback surface), and on the mirror `skillpanel` is an unknown command (panel genuinely gone). The in-game tap landing on `SkillsPerksScreen` needs a Bedrock client. |
| 9 | Java `skills` click, both panel states | **NOT EXERCISED** (client) | Model half proven (`case9.log`): with ExcelSkillsPanel absent, `skills` resolves guarded with the standard unavailable action (`plugin ExcelSkillsPanel absent` → `[message] &eThat isn't available right now.`) — Item 5 guard behaviour intact. Present-state dispatch is the Item 5 data path (case 7 evidence). The chest click itself needs a Java client. |
| 10 | `magiclevel` retired | **PASS** | Registry 93 on every 3.0.0 boot (diff lists no magiclevel); absent from Bedrock + Java resolution (model); `entries/magiclevel.yml` absent after boot **and after a second restart** on the desk rig (resurrection check, §5.3) and absent after the mirror boot; typed `/magiclevel` still answers (ExcelMagicLevel usage line on desk + mirror). |
| 11 | Rearrange — Bedrock move + relog persistence + cross-platform | **PASS (core)** / relog NOT EXERCISED (client) | Model on live data: `MenuEditorCore.moveToTop` moves Summons to the head of Me; `PrefsCodec` YAML round-trips the record verbatim (the exact on-disk format `PrefsStore` writes); `OrderMerge` on a fresh resolve shows Summons first — a relog open is precisely codec-load + merge, both proven; the **same** prefs produce the **same** within-group order over the Java resolution (single store, cross-platform). The E1→E2→E3 click-through and a real relog need clients. |
| 12 | Rearrange — Java insert + group order | **PASS (core)** / editor screens NOT EXERCISED (client) | Model: Java insert-move algebra on Travel's movable entries (`insertBefore(elitemobs, party)` → `[elitemobs, party, guild]`); group-order preference reorders the picker (Me before Travel, pins unmoved, Rearrange still last). J2/J3 screen interaction needs a Java client. |
| 13 | Rearrange — merge (new + vanished entries) | **PASS** | Model with a dummy entry file added to Me (order 42): customised player sees `rig_dummy` **last** in Me; default player sees it in default position (after spellbook). After deleting a stored entry's file (`trash.yml`): merge does not throw, the vanished id drops, order holds (Summons still first). |
| 14 | Rearrange — reset | **PASS (core)** / editor confirm NOT EXERCISED (client) | Model: empty prefs (the reset state) merge = the default order **exactly**; codec/merge treat absence as default by construction (§3.2/§3.6 semantics). The E5/J4 two-step confirm screens and the chat line need clients. (Prefs-file deletion is `PrefsStore` Bukkit code — compile-proven in the build, not rig-exercised.) |
| 15 | Rearrange — corrupt prefs | **PASS (core)** / in-game SEVERE line NOT EXERCISED | Model: `PrefsCodec.parse` of hand-corrupted YAML returns no-prefs + a surfaced parse error; `version: 99` returns no-prefs + "unsupported version 99" — the §3.8 contract (default order, never fail an open). The in-game half (menu opens at default + one SEVERE naming the file, from `PrefsStore`) is Bukkit-layer and needs a player open. |
| 16 | Editor Back discipline | **NOT EXERCISED** (client) | What is proven: every *menu* form's Back discipline (cases 4/5 — Back last, `PickerTarget`) and the picker's no-Back root shape (case 1). The editor forms E1–E5 / J1–J3 and their Back landings exist only in the Bukkit editor services; no client exists on the rigs to open them. Build-side: editors were compile-proven only (BUILD-REPORT says the same). |
| 17 | `rearrange.enabled: false` | **PASS (core)** / command refusal NOT EXERCISED (client) | Model: kill-switch config makes `rearrangeServes(server_menu)` false; picker rendered with the switch off carries **no Rearrange button** (8 buttons = pins + groups) and is otherwise the case-1 shape. The `/rearrange` polite refusal is Bukkit-layer (console shows the player-only line in both states). |
| 18 | Guarded-entry ordering | **PASS** | Model: with ExcelAutoSort absent, `autosort` resolves guarded; a stored Settings order naming it merges without crashing and the unavailable door **keeps its stored place** (`settings=[makersmark_settings, details_settings, autosort, silkwarnings, aimassist, autojoin]`, action = standard unavailable message). |

**R2 tally: PASS 7 (cases 1, 4, 5, 6, 10, 13, 18) · PASS at
core/model level with the client half owed 5 (11, 12, 14, 15, 17)
· NOT EXERCISED (client-only) 6 (2, 3, 7, 8, 9, 16 — all
Bedrock/Java *tap* paths) · FAIL 0 in the 3.0.0 jar and in the
corrected (slot-17) data set · 1 FAIL recorded against the
as-designed slot-8 data placement (§3), resolved by the design's
own Q7 fallback.**

## 6. R3 case-by-case (PC test-server mirror, brief repeat)

Mirror: `C:\Users\MikeExcel\wrv-testserver\excelmenu30-rig\`
(Paper 26.3-245 from the mirror; **live-pulled** geyser.jar +
floodgate.jar + companions per §1; candidate jar hash-verified on
the PC; seed = seed-r2). Logs: `r3-panelpresent-rotated.log.gz`
(first boot), `r3-nopanel-latest.log` + `r3-boot-out.log`.

| # | Case (brief) | Verdict | Evidence |
|---|---|---|---|
| 1 | Picker shape | **PASS** (mirror load + same model) | Mirror boot 93/2/13 with the identical seed the desk harness proved case 1 against; layout loads with `pinned=[spawn, tpmenu]` (no layout error, contrast §3). |
| 4 | Travel form | **PASS** (same basis) | Identical seed + jar as desk case 4 PASS; mirror registry diff lists the same 93 ids. |
| 5 | Group forms / commands group | **PASS** (same basis) | Mirror self-check loads all 13 definitions incl. `skills_perks`; registry 93/0-failed on `excelmenu list`. |
| 7 | `skills`, panel present | **NOT EXERCISED** (client) | Mirror plugin list (9) shows ExcelSkillsPanel green alongside ExcelMenu; RCON `skillpanel` → player-screen line (command live). Bedrock tap owed to a client (post-install Mike proof 3). |
| 8 | `skills`, panel absent | **NOT EXERCISED** (client) | Mirror panel-absent boot: 93/2/13, SEVERE 0, `skills_perks` loaded (fallback surface), `skillpanel` unknown command. Bedrock tap owed to a client. |
| 10 | `magiclevel` | **PASS** | Mirror registry 93 with no magiclevel; `entries/magiclevel.yml` verified **absent on disk after the mirror boot**; RCON `magiclevel` → ExcelMagicLevel usage (typed command answers). |
| 11 | Rearrange persistence | **PASS (core)** (same basis as R2) | Identical jar + seed as desk case 11; the codec/merge proof carries to the mirror unchanged. In-game relog owed to a client. |

**R3 boot gates: SEVERE 0 on both mirror boots; counts 93 / 2 /
13 on both; bridge PRESENT on both (against the real pulled
Geyser/floodgate).**

## 7. What this rig does NOT prove (carried, not hidden)

- Every real-client interaction: Bedrock form taps (pins, skills
  hook both states, editor E1–E5), Java chest clicks (skills,
  editor J1–J3), an actual relog, and the corrupt-prefs in-game
  SEVERE line. These are DESIGN §7's five post-install **player
  proofs (Mike)** plus the editor click-throughs; the desk/mirror
  rigs have no client harness and this report does not pretend
  otherwise.
- `PrefsStore`'s on-disk file IO in-game (temp + atomic rename) —
  its format (`PrefsCodec`) is proven; the Bukkit file layer is
  compile-proven only, as the BUILD-REPORT already states.

## 8. Verdict

No case failed against the 3.0.0 jar. Every boot gate the design
sets (93 / 2 / 13, SEVERE 0, bridge PRESENT, magiclevel absent
post-restart) held on the desk rig (×4 boots) and on the PC
mirror with live-pulled jars (×2 boots). The one failure found —
the §6.2 Rearrange item at page-2 slot 8 — is a **data-placement**
failure with the design's own fallback (Q7 slot 17) proven in its
place; **the filing packet must place the item at slot 17** and
record the §3 evidence. The client-only cases remain owed as
Mike's post-install player proofs, exactly where DESIGN §7 puts
them.

**GATE 2 PASS** (condition carried into the filing: Rearrange
fixed item at page-2 slot 17 per DESIGN §9 Q7, evidenced in §3;
client-tap cases 2, 3, 7, 8, 9, 16 and the in-game halves of 11,
12, 14, 15, 17 remain NOT EXERCISED and ride the post-install
player proofs).

## Evidence files (this folder)

- `pulls/` — the fresh live pull (jars + ExcelMenu data folder),
  hashes in §1. `make-seeds.py`, `seed-r0/`, `seed-r2/`,
  `seed-r2.zip` (the seed as transferred to the PC),
  `slot8-failing-server_menu.yml` (the as-designed placement).
- `boot-desk.sh`, `boot-r0-control.log`, `boot-r2-panel.log`
  (slot-8 failure), `boot-r2-panel-slot17.log`,
  `boot-r2-restart2.log`, `boot-r2-nopanel.log`.
- `RigHarness.java`, `harness-r2.log` (26 PASS / 0 FAIL),
  `harness-r0.log` (control, 0 FAIL), `case9.log`.
- `r3-panelpresent-rotated.log.gz`, `r3-nopanel-latest.log`,
  `r3-boot-out.log` (PC mirror). Mirror rig left stopped on the PC
  at `C:\Users\MikeExcel\wrv-testserver\excelmenu30-rig\`.

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
