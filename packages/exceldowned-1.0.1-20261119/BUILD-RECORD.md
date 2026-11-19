# BUILD RECORD — M1: ExcelDowned → ExcelItems migration (ID 4, Revive Potion)

- Worker: M1 build worker, board supervisor run 559
- Date: Wed Nov 18, 2026, ~7:27–7:36 PM CST (America/Chicago)
- Scope read: `~/workspace/board-r558/W3-MIGRATION-SCOPE.md` §3 M1, §4.1
  G-1..G-6, §4.2 M1 gates; supervisor decisions D124 (mint = FULL
  delegation to `ExcelItemsAPI.create(4)`; version bump per house
  convention); `~/workspace/downed-100-filing/INSTALL-PACKET.md` (D109/D110).
- Deliverable: this folder. NOT filed, NOT installed, no GitHub writes.

## Verdict

```
BUILT — ExcelDowned 1.0.1 (M1 migration candidate)
  Candidate: ExcelDowned-1.0.1.jar — 140,323 B /
    9e8ae0f5b297045a5e913e2c8e7c3e2cccb858c1fe32bbed6e8bb2f7566c32b3
  Build ID: EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d
  Tests: 77/77 PASS (71 baseline 1.0.0 suite + 6 new M1 tests),
    in TWO independent local-build.sh runs, jars byte-identical
    across both runs and within each run (jar == repro).
  G-1 PASS EXACT · G-2 PASS EXACT · G-3 PASS · G-4 PASS in
    substance on a VM-local Paper rig (see G-4 note) · M1 gates PASS
  Status: Built (not Filed). Filing is the supervisor's dispatch.
```

## Version bump (D124 house convention, stated per instruction)

ExcelDowned **1.0.0 → 1.0.1** (patch bump: a depend + mint/identify
rewire with zero player-facing behavior change — the Shops revivepatch
no-bump precedent was a presentation patch on an unchanged plugin.yml;
this build changes `plugin.yml` itself, so it bumps). `plugin.yml`
version, `BuildInfo` fragments, and `local-build.sh` knobs all carry
1.0.1 / build date 2026-11-18 / build ID above. The two test pins that
asserted 1.0.0 (D108/D110 lineage) were updated to 1.0.1 with their
messages rewritten to cite D124. ExcelItems does NOT bump (no API gap
was found — `create(int)`, `identify`, `registerBehavior` all exist in
0.1.0 as scoped).

## G-1 — live-premise fresh pull (Nov 18, 2026, ~7:27 PM CST)

Homebase route, probe PASS attempt 1 (`ssh -F ~/.ssh/config mike-pc`,
host DESKTOP-IL05DQR, no fallback): on-PC rclone copyto from
`shockbyte:1. Michael's Server/…` to PC staging
`C:\Users\mikeexcel\muse-temp\g1-m1-r559\`, then scp to `pulls/` in
this folder; local sha256sum after download:

| Live file | Pulled | Of record | Verdict |
|---|---|---|---|
| `plugins/ExcelDowned-1.0.0.jar` | 140,683 B / `fcfa96d2036926b0eee5eaf338196ceee55680ca56e9df415563e35b1990e57c` | 140,683 B / `fcfa96d2…` | **EXACT — PASS** |
| `plugins/ExcelDowned/config.yml` | 4,916 B / `94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf` | 4,916 B / `94eda94a…` | **EXACT — PASS** |
| `plugins/ExcelItems-0.1.0.jar` | 20,214 B / `85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d` | 20,214 B / `85dbb9ea…` | **EXACT — PASS** |

Live `plugin.yml` extracted from the pulled jar (`pulls/live-plugin.yml`,
1,898 B / `7185c2c1…`): version 1.0.0, `depend: [Magic]`, no ExcelItems
entry — the migration premise holds. The extracted plugin.yml and the
pulled config are byte-identical to the canonical build tree's copies
(`diff` clean both).

## G-2 — base-gate byte-identical rebuild

Canonical tree = `~/workspace/board-r510/f1-fix/build-1.0.0/` (the
F1-fixed 1.0.0 tree the installed jar was built from; run-510 W7
record). Copied unmodified to `work/base-gate/`, jars deleted, tree's
own `local-build.sh` run with zero edits:

- Rebuilt: **140,683 B / `fcfa96d2…`** — EXACT vs the G-1 live pull
  and the of-record candidate; jar == repro byte-identical;
  tests **71/71**; fingerprint gate PASS
  (`EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d`).
- Log: `work/base-gate-build.log`. **G-2 PASS.**

## The migration delta (vs the base-gate tree; 4 files touched)

1. **`plugin.yml`** — `version: 1.0.1`; `depend: [Magic, ExcelItems]`
   (hard depend). Everything else byte-identical.
2. **`RevivePotionItem.java`** — rewritten mint/identify:
   - `create(plugin)` now returns `ExcelItemsAPI.get().create(4)`
     (REGISTRY_ID = 4) — FULL delegation per D124. The local potion
     construction (colour/name/lore/PDC stamping) is GONE from
     Downed; the one construction of record is the registry's, which
     dual-stamps primary `excelitems:id` INTEGER 4 + legacy
     `exceldowned:revive_potion` BYTE 1 + `revive_potion_lore`
     INTEGER 1 — dual-stamp preserved (D123 D2).
   - `isRevivePotion` identifies through the API (primary-first,
     legacy fallback — the registry's own rule), with a fail-soft
     legacy-PDC fallback if the API lookup itself throws, so held
     potions stay recognizable. The pure rule is factored as
     `isReviveFromMarks(primaryId, legacyTagged)` for the harness:
     a present primary decides alone (a forged primary never falls
     through to legacy); legacy decides only with no primary.
   - `countPotions` / `consumeOne` unchanged except they flow
     through the new `isRevivePotion` (every Downed call site —
     walk-up prompt, consumption, cooldown path, purchase delivery
     verification — therefore identifies via the API).
   - Noted consequence of full delegation (D123 D3): the minted
     lore's price line is now the registry definition's pinned
     default "$5,000" text instead of a config-formatted line. The
     CHARGE price is untouched: it is read from ExcelDowned config
     (`revive-potion.price`) by the purchase coordinator, never
     from the item.
3. **`ExcelDowned.java`** — `onEnable` registers the revive behavior
   for ID 4: `ExcelItemsAPI.get().registerBehavior(REGISTRY_ID,
   () -> "exceldowned:revive-potion")` (handler id matches the
   registry definition), INFO line on success, Throwable-guarded
   WARN fail-soft (house Cleric-seam shape). The handler BODY stays
   in Downed (RevivePotionUi / walk-up flow) — registration only,
   per scope §3 M1 item 4. Nothing else in the file changed.
4. **`local-build.sh`** — version knobs above + the ExcelItems
   0.1.0 jar pinned on the compile classpath by SHA-256
   (`85dbb9ea…`, build refuses on mismatch, same discipline as the
   EliteMobs pin). No ExcelItems classes are bundled into the
   candidate jar (verified: `unzip -l` shows no excelitems entries).

**Unchanged by design (verified):** `config.yml` byte-identical to
the live pull (all tunables — price, cooldowns, HP fraction, Cleric
settings — untouched); purchase coordinator/journal, buy/give verb
split routing (`RevivePotionPolicy` untouched), Cleric Raise Ally,
and every other source file are byte-identical to the base tree
(delta is exactly the 4 files above + tests below).

Tests delta (`DownedTest.java`): 2 version pins updated (above) +
6 new M1 tests (next section). No pre-existing test was weakened or
removed; the 71 baseline tests pass unmodified in behavior.

## G-3 + M1 gates — automated tests (77/77, two runs)

`work/build-1.log` + `work/build-2.log`: each run 77 found / 77
successful / 0 failed; candidate jars byte-identical across runs.

New M1 tests (all PASS):
- `m1PluginYmlHardDependsOnExcelItems` — plugin.yml carries
  `depend: [Magic, ExcelItems]` + version 1.0.1.
- `m1MintFullyDelegatesToRegistry` — source pin: `REGISTRY_ID = 4`,
  `ExcelItemsAPI.get().create(REGISTRY_ID)` present; `setColor`,
  `new ItemStack(Material.POTION`, `0xFF` all ABSENT (no surviving
  local construction).
- `m1IdentifyPrimaryFirstLegacyFallback` — mark matrix: (4,·)=true;
  (1,true)=false and (3,true)=false (primary decides alone, forged
  primary never falls through); (null,true)=true (legacy-held
  potions still identify); (null,false)=false.
- `m1BehaviorRegisteredForId4` — source pin for the registerBehavior
  call + handler id in `ExcelDowned.java`.
- **`m1VerbSplitRegressionBuyChargesAndJournalsGiveStaysAdminOnly`
  (M1 gate 1)** — through the real coordinator + journal with fake
  ports: quote ARMED with 0 withdraws; confirm → PURCHASED, exactly
  1 withdraw of exactly $5,000 (the config price), exactly 1
  delivery, journal phase COMPLETED. Verb split: `routeBuy` on the
  give spelling = NOT_OURS; `routeGive` on the buy spelling ≠ GIVE
  even for an admin sender; give from a non-admin = DENIED; console
  give still = GIVE (admin seam unchanged); neither Downed UI class
  (`RevivePurchaseUi`, `RevivePotionUi`) contains a
  `revivepotion give` dispatch (never menu/catalog reachable from
  Downed's own surfaces). The baseline suite's route matrices
  (lines ~608–660, `buyRouteMatrix`) also pass unchanged.
- **`m1PurchaseJournalBootCheckFreshJournalHealthy` (M1 gate 2)** —
  a fresh `RevivePurchaseJournal` in a fresh nested directory
  constructs `healthy()` with zero entries / zero unresolved — the
  exact first-boot state that must not produce the F1 WARN.
  Corroborated at server level by the G-4 rig boot below (fresh
  data folder, `purchases.journal` created, NO
  `purchase journal unavailable` line in the boot log).

## G-4 — co-resident load check

Homebase mirror boot was NOT run tonight: the task bars console/
client inputs tonight (Jesús on the PC), and the mirror
(`C:\Users\mikeexcel\wrv-testserver`, currently running its restored
found-state boot, java pid 31772) was left completely untouched —
no SSH writes to it, no restarts.

Substitute executed instead (same checks, VM-local): a throwaway
Paper 26.2 server in `/tmp` on this VM (rig copy since discarded;
log preserved at `work/vm-rig-boot.log`, 3c04ed4f…) with exactly
three plugins co-resident: the G-1-pulled live `ExcelItems-0.1.0.jar`
+ Magic 11.2.5 (Downed's other hard depend) + the candidate
`ExcelDowned-1.0.1.jar`, fresh data folders:

- `Done (31.490s)` reached; **SEVERE count 0** (whole boot log).
- `Enabling ExcelItems v0.1.0` ×1 at log line 167, BEFORE
  `Enabling ExcelDowned v1.0.1` ×1 at line 169 — hard-depend order
  proven; `ExcelItems enabled: 4 custom item(s) registered, next
  free id 5.` ×1.
- `ExcelDowned: registered revive behavior for ExcelItems ID 4
  (exceldowned:revive-potion).` present ×1.
- `ExcelDowned 1.0.1 ready — build
  EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d` present.
- **No `purchase journal unavailable` WARN**; fresh
  `plugins/ExcelDowned/purchases.journal` created (0 B).
- Expected bare-rig WARNs (not migration findings): Vault absent →
  the standing "Vault economy unavailable — purchases disabled"
  fail-soft WARN; Magic spell-template WARNs for
  resurrection/grouprenewal (the bare rig carries no live Magic
  spells data — the same WARN class any bare-rig Downed boot
  produces; the live premise carries the real spells data).

**Owed (stated plainly):** the literal G-4 Homebase-mirror boot
(candidate + live ExcelItems jar on `wrv-testserver`, RCON
`excelitems status` still 4/5) remains owed as a desk/rig leg
before filing if the supervisor wants the mirror form of the gate;
the VM-rig boot above covers every G-4 check line except the RCON
status read. Under D122 the filing gate is mirror-loads-clean +
tests pass — the supervisor decides whether the VM rig satisfies
it or the mirror leg runs first on a free-PC window.

## G-6 — surfaces check (named, per scope §4.4 M1)

- **Player-facing change: NONE.** Same potion (registry definition
  is pinned to the 1.0.0 mint, dual-stamped), same $5,000 config
  price, same buy/give verbs, same menus, same cooldowns.
- What's New (in-game + website): **no entry owed** — no
  player-visible behavior change (check result stated here per
  G-6; the filing packet should carry this line).
- Menus (ExcelMenu + ExcelBedrockMenu): no change — Downed owns no
  shop menu; the Shops shelf still dispatches
  `downed revivepotion buy` (M4's piece, untouched here).
- Tips: Revive tip wording remains true post-migration (carry,
  walk up, tap Revive, $5,000) — no change.
- Guide: HELD under hibernation; owed on wake: none expected (no
  behavior change).

## Artifacts (this folder; full hashes in SHA256SUMS.txt)

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelDowned-1.0.1.jar` (candidate) | 140,323 B | `9e8ae0f5b297045a5e913e2c8e7c3e2cccb858c1fe32bbed6e8bb2f7566c32b3` |
| `ExcelDowned-1.0.1-repro.jar` | 140,323 B | `9e8ae0f5…` (identical) |
| `ExcelDowned-1.0.1-source.zip` (src+tests+plugin.yml+config.yml+local-build.sh) | 108,373 B | `13390e265444cd543f584cbf2a934429776339264295b08c5e30630d88d902b0` |
| `pulls/` | G-1 live pulls (jar, config, ExcelItems jar, extracted live plugin.yml) | in SHA256SUMS.txt |
| `work/` | base-gate tree + build tree (`build-1.0.1/`), build logs ×3, VM-rig boot log | — |

No filing, no install, no GitHub writes, no panel action, no
Homebase writes of any kind were performed by this worker (the PC
was read-only: probe + rclone pulls to a temp staging dir).
