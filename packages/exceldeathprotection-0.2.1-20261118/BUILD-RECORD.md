# M2 — ExcelDeathProtection → ExcelItems migration (IDs 3 then 2) — BUILD RECORD

- **Worker:** M2 build worker, board supervisor run 559
- **Date:** Wed Nov 18, 2026, ~7:26–7:35 PM CST (America/Chicago)
- **Scope:** ~/workspace/board-r558/W3-MIGRATION-SCOPE.md §3 M2, §4.1, §4.2 (M2 gates), §6 + supervisor decision D124 (mint = FULL delegation to `ExcelItemsAPI.create(id)` for ID 3; ID 2 is ACTIVATION — `create(2)` never wired as a giveable item; version bump stated in record).
- **Version bump (D124 item 3):** ExcelDeathProtection **0.2.0 → 0.2.1**. Build fingerprint: `EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e`. ExcelItems itself NOT bumped (no API gap found).
- **Candidate:** `ExcelDeathProtection-0.2.1.jar` — **61,855 B / SHA-256 `deb7b40ae3fdfa74534dc8d1aabfa0434c267275f0db430861a7221f64a92749`**, double-build byte-identical.
- **Source:** `ExcelDeathProtection-0.2.1-source.zip` — 62,231 B / `310c80d5dddceeb6c2816d07f65897c19f822c609215f427ff1b504120b8714f`.
- **NOT filed, NOT installed, NO GitHub write** (per dispatch).

## G-1 — Live-premise fresh pull (PASS)

Lane: Homebase SSH (`ssh -F ~/.ssh/config mike-pc`, DESKTOP-IL05DQR, probe PASS attempt 1) + rclone remote `shockbyte:` on the PC into `C:\Users\mikeexcel\g1-pull-m2-dp\`, then scp to `live-pull/` on the VM. Pulled Nov 18, 2026 ~7:26 PM CST.

| Live file | Size | SHA-256 | Verdict |
|---|---|---|---|
| `plugins/ExcelDeathProtection-0.2.0.jar` | 59,390 B | `ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719` | EXACT vs scope §2 of record (`59,390 B / ed819bb6…`) |
| `plugins/ExcelDeathProtection/config.yml` (live data-folder config) | 150 B | `d6e3208761c5b08dee1687237946b85d4821dd9f3020aa39b54bfbf082115efc` | Minimal live config: `basic-price: 15000.0`, `premium-price: 30000.0` only. The premium-credit block is absent live; the code's config defaults (enabled/auto-rearm/max-per-call 5/max-banked 0) govern — unchanged by this migration. Candidate ships the source-tree default config unchanged (config.yml byte-identical base → migrated), and installs never replace a live config. |
| `plugin.yml` (extracted from the pulled live jar) | — | inside jar above | `version: 0.2.0`, `depend: [Vault]` — EXACT vs the 0.2.0 source tree; jar `excel-build.txt` = `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348` |

No mismatch — build proceeded on the pulled bytes' lineage (the local 0.2.0 build jar is byte-identical to the pulled live jar).

## G-2 — Base-gate byte-identical rebuild (PASS)

Unmodified 0.2.0 source tree (copied to `work/base/`) rebuilt with its own `build.sh`: **118 tests found / 118 successful / 0 failed**, double-build byte-identical, and the rebuilt jar is **byte-identical to the live jar** (`ed819bb6…719`, 59,390 B). The migration delta is therefore fully isolable.

## What changed (0.2.0 → 0.2.1) — complete delta

Files changed vs base: `plugin.yml`, `BuildInfo.java`, `ExcelDeathProtection.java`; new: `RegistryItems.java`, test `RegistryMigrationTest.java`; test pin updated in `ResourceConfigurationTest.java` (version 0.2.0 → 0.2.1 + new hard-depend assertion). `config.yml` and every other source file byte-identical.

1. **plugin.yml:** `version: 0.2.1`; `depend: [Vault, ExcelItems]` (hard depend).
2. **M2a — premium (ID 3) first:**
   - `createPotion(amount)` is now **full delegation**: `ExcelItemsAPI.create(3, amount)` (D124). No owner-side construction remains. The registry mint dual-stamps primary `excelitems:id=3` + legacy `exceldeathprotection:potion` BYTE 1 + `lore_version` INTEGER 2, and the registry definition was test-pinned field-for-field against the 0.2.0 mint (POTION, colour 0x7D3C98, glint, `+Death Protection Potion`, same 8 lore lines) — no player-visible change.
   - `isProtectionPotion` identifies **via the API** (`api.isItem(stack, 3)` — primary-first, legacy fallback); if the API call itself fails it falls back to reading the item's own marks directly through `RegistryItems.resolvePremiumIdentity` (same rule: primary present ⇒ that ID only, never falls through to legacy; legacy BYTE alone ⇒ 3, preserving 0.2.0 recognition of pre-lore bottles, which `refreshPotionLore` still upgrades in place — `updatePotionText`/`refreshPotionLore` unchanged).
   - Behavior registered at enable: `registerBehavior(3, "exceldeathprotection:premium-drink")`.
   - All existing give/deliver paths (`/dp give`, `deliverPotion`) call `createPotion` unchanged — they now mint through the registry with no call-site edits.
3. **M2b — basic (ID 2) second:**
   - `deliverBasic` first resolves definition 2 from the registry and requires it to be the ACTIVATION definition (`RegistryItems.requireActivation`, fail-closed → `FAILED_ROLLED_BACK` before any grant). Name/lore/display for basic now have their single source in the registry definition. The Tier.BASIC grant/rollback logic itself is **unchanged**.
   - Behavior registered at enable: `registerBehavior(2, "exceldeathprotection:basic-activation")`.
   - `basicDisplayStack()` is the only `create(2)` caller in the codebase and is documented + named display-only; grep-verified it has **no caller** in any give/delivery/menu path. The CHEST stack remains display-only.
4. **Enable-time registry wiring (`initializeRegistry`, fail-closed):** resolves the API via Bukkit services (hard depend ⇒ ExcelItems enabled first), shape-checks both definitions (ID 3 must be giveable ITEM, ID 2 must be ACTIVATION), registers both behaviors, logs one registry line. Any failure ⇒ severe log + plugin disables itself rather than minting/activating against a drifted definition; `onEnable` returns early in that case.
5. **Premium credit seam (`creditPremium`):** code **unchanged** (API class byte-identical; `creditPremiumInternal`, bank math, re-arm untouched). Its premium product identity is now registry ID 3, resolved through the same identify rule — pinned by the credit-seam test below.

## G-3 + M2 gates — automated suite (PASS)

Full suite on the migrated tree (`work/migrated`, `build.sh` runs it before packing): **127 tests found / 127 successful / 0 failed** = 118 pre-existing (all green, incl. PremiumApiProbeTest, PremiumCreditsTest, PurchaseCoordinatorTest, PurchaseJournalTest, PurchaseReviewMenuTest) + **9 new** in `RegistryMigrationTest`, which run against the REAL ExcelItems `BuiltinItems` registry from the live 0.1.0 jar on the classpath:

- `premiumIsRegistryId3GiveableItem`, `premiumDefinitionMatchesThe020MintExactly` (M2a fidelity).
- **ID-2 negative test (M2 gate 1):** `basicIsRegistryId2ActivationResolvedFromRegistry`, `id2NegativeTest_activationCanNeverBeGiven` (`requireGiveable(def2)` throws ACTIVATION; reverse guards fail closed), `id2HasNoIdentifiableItem` (no marks resolve to 2). Shop basic still activates Tier.BASIC: `deliverBasic` accepts ID 2 only through `requireActivation` and its grant path is the unchanged 0.2.0 code (existing PurchaseCoordinator/Premium suites green).
- **Premium credit-seam test (M2 gate 2):** `creditSeamReflectiveShapesUnchanged` (BossTrial's reflective shapes intact; plugin-down calls return clean false/0), `creditSeamPremiumIdentityResolvesThroughRegistry` (registry-minted marks primary 3 + legacy + lore_version 2 and legacy-only marks both resolve to ID 3 through the identify path; one credit activates-now under the untouched PremiumCredits math).
- Identify rule: `premiumIdentity_primaryMarkWinsAndNeverFallsThrough` (primary 4/1/99 + legacy mark ⇒ NOT premium), `premiumIdentity_legacyFallbackOnlyWithoutPrimary`.

Desk smoke (in place of execution): candidate jar listing verified — `plugin.yml` inside jar shows `version: 0.2.1` + `depend: [Vault, ExcelItems]`; `excel-build.txt` = new fingerprint; `RegistryItems.class` present; grep over migrated sources confirms the only `itemsApi.create` calls are `create(3)` in `createPotion` and `create(2)` in the uncalled-by-delivery `basicDisplayStack()`.

## G-4 — Mirror load (OWED — not run tonight)

Per dispatch: mirror load via SSH/command line only, zero console/client inputs (Jesús on the PC). The mirror rig scripts (`wrv-testserver` on Homebase) abort if any `java.exe` is running, and a java process (PID 31772, ~4.4 GB — the client in use) was present at check time (~7:33 PM CST). Booting the mirror would have required interfering with the PC in use, so **G-4 is owed**: co-resident mirror boot of candidate 0.2.1 + live ExcelItems 0.1.0 jar (enabling lines ×1 each, dependent enables AFTER ExcelItems, SEVERE 0, `excelitems status` still 4 registered / next free 5, registry log line present) remains to be run before filing. Desk smoke above substitutes for this run only; it does not satisfy G-4.

## Surfaces check (G-6 — named, per dispatch expectation)

- **Player-facing behavior change: NONE.** Same potion (field-for-field definition), same prices ($15,000 basic / $30,000 premium, live config untouched), same commands, same messages, same credit seam. Internal dependency/identity migration only.
- What's New (in-game + website): **no entry owed** — no player-visible change (check result stated here per §4.4).
- Guide: HELD under hibernation (standing exception); owed-on-wake: none expected.
- Tips: checked — no DeathProtection tip wording affected (no behavior change).
- Menus (ExcelMenu + ExcelBedrockMenu): no menu change — DeathProtection owns no shop menu; shop entries still call `deathprotection buy basic|premium %player_name%` (template test still green).
- Records (wrv-plugins draft PR + Drive deposit): owed at close-out/filing time, not this build run (no filing per dispatch).

## Proof owed AFTER install (not gates, per D122)

In-game buy-and-drink premium flow, basic buy activation, banked-credit re-arm with a real credit, legacy-held (pre-migration) potion drink — all post-release checks via the board's Needs Testing.

## Layout

- `ExcelDeathProtection-0.2.1.jar` (candidate), `ExcelDeathProtection-0.2.1-source.zip`, `SHA256SUMS` — this folder.
- `live-pull/` — G-1 pulled live jar + live config (+ extracted `live-plugin.yml`).
- `work/base/` — pristine 0.2.0 tree; `work/base-build.log` holds its 118/118 + byte-identical output. `work/TEST-RESULTS.txt` is the migrated run's JUnit tree (127/127); `work/migrated-build.log` holds its build tail.
- `work/migrated/` — the 0.2.1 source tree that produced the candidate.
