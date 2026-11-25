# Potion registration + vendor buy flows — BUILD RECORD

Lane home: `~/workspace/potion-vendor-flow-20261123/`
Date: Nov 23, 2026. Desk work only — no live install, no
MANIFEST.md edit, no player messages. Working stamp **0.4.1**
(parent re-stamps at filing).

## 1. Why this build exists

The 0.4.0 merge lane grep-verified that the field-medic build
never registered the Strong Healing potion and provides no buy
flows (`~/workspace/excelemfixes-040-merged-20261123/RECORD.md`
§2). Three filed listings were gated on it:

- **Catalog Unit 2** (MANIFEST PENDING, HARD GATE): the
  ExcelShops special-products listing `strong_healing_potion`
  names ExcelItems registry ID 7; ExcelShops' parse is
  fail-closed, so placing the listing while ID 7 is
  unregistered breaks the shop config load.
- The **guild vendor NPC rows**: potion $112.50
  (`fieldmedic buy %player_name% npc`), ender pearls ×16 $300
  (`guildvendor buy pearl %player_name%`), per
  `~/workspace/shop-catalog-pass-20261122/staged/vendor-npc-listings.yml`.
- The **PlayerMap EMPTY_MAP vendor row** $75
  (`guildvendor buy emptymap %player_name%`), per
  `~/workspace/playermap-teaching-20261123/RECORD.md`.

This build supplies all three: the registration, the
`/fieldmedic` delivery flow the ExcelShops listing calls, and
the generic `/guildvendor` flow.

## 2. What was built

Placement: **inside the field-medic module**, exactly where
both staged records name the owner ("ExcelEMFixes field-medic
module" / "generic vendor flow" in the field-medic module). No
deviation. The guild vendor's vanilla goods stand down with
field-medic when ExcelItems is absent — consistent with the
staged ownership and the pack's item-module posture.

New classes (all under
`build-root/src/src/excelemfixes/fieldmedic/`):

- `PotionItem` — the registry definition, pinned in code:
  **ID 7, key `strong_healing_potion`**, SPLASH_POTION with
  base potion type `STRONG_HEALING` (vanilla Instant Health
  II — splash heals 8 HP at point blank), handler
  `excelemfixes:field-medic`. The base type is what makes the
  delivered potion real; a SPLASH_POTION minted without it is
  an effect-less dud. **No `7:` line goes in ExcelItems'
  config.yml** — the documented 0.1.0 trap (that line reads as
  an unknown ID at ExcelItems' own enable and disables it);
  the trap is pinned by a unit test for ID 7, mirroring the
  bandage's.
- `PotionItems` — the registration bridge, mirroring
  `BandageItems`: by-ID + by-key match → ALREADY_OURS; either
  claimed by another item → CLASH and the module stands down
  (fail-closed, same as the bandage).
- `VendorGoods` — the pure goods/price table. POTION
  (registry mint of ID 7; shop $150.00, vendor $112.50),
  PEARL (ENDER_PEARL ×16; vendor $300.00), EMPTYMAP (MAP ×1;
  vendor $75.00). Vendor prices are the D200 25%-off figures.
  The vanilla goods are NPC-venue only (at list price they are
  ordinary ExcelShops catalog offers). Prices are config
  knobs on the module (`potion-shop-price`,
  `potion-npc-price`, `vendor-pearl-npc-price`,
  `vendor-emptymap-npc-price`), defaults = the batch prices.
- `VendorArms` — the pure arm ledger: one armed review per
  player, 30-second window, price snapshotted at arm, arming
  replaces any live review, consume-once.
- `VendorBuyFlow` — the Vault/Bukkit glue both commands
  share, mirroring summon-mount's proven BuyFlow: arm →
  confirm does room pre-check → withdraw → deliver → leftover
  check → auto-refund; a failed refund logs SEVERE with the
  amount and the word REFUND (never silently lost). One flow
  + one ledger across both commands, so a player can never
  hold two armed prices at once.
- `FieldMedicCommand` — `/fieldmedic buy [npc]` (player),
  console `fieldmedic buy <player> [npc]` (the exact form the
  ExcelShops listing's delivery and the vendor NPC template
  run), `/fieldmedic confirm`, admin `/fieldmedic give`.
- `GuildVendorCommand` — console
  `guildvendor buy <good> <player>` / player
  `guildvendor buy <good>`, `/guildvendor confirm`. The
  potion is refused here with a pointer to
  `/fieldmedic buy npc` (its staged delivery).

Changed classes:

- `FieldMedicService` — registers the potion beside the
  bandage at start (CLASH → stand down), hooks the Vault
  economy (kept out of the module class, per house pattern),
  builds the buy flow when an economy exists, reads the four
  price knobs with per-knob sanitizing, pushes price updates
  on reload. Without Vault: bandage + both registrations
  stand, purchases report unavailable.
- `FieldMedicModule` — registers both commands dynamically
  after a successful service start (they do not exist while
  the module is off or standing down; the pack auto-unregisters
  them on disable), exposes service/buyFlow/economy/potion
  accessors.

ExcelShops semantics honored (verified in its source,
`~/workspace/board-r559/m4-shops/work/migrated/`): ExcelShops
never charges for special products — its menus dispatch the
listing's delivery command as console, and the owning
plugin's flow does review/confirm + charge + delivery +
refund (the mobveil / deathprotection / mountcharm pattern).
The staged listing's delivery is `fieldmedic buy
%player_name%` (shop venue, $150) — implemented exactly.

Documentation rule (Mike, Nov 23, 2026): the field-medic
config section in BOTH `build-root/src/config.yml` and
`build-root/deploy/config.yml` carries the full
defect→fix→behavior explanation for the potion registration
and both buy flows, and the pack README's field-medic section
gained matching paragraphs (the second gap, the solution, the
generic vendor flow, WRV's shipped prices = the defaults).

## 3. Jar + config

- Jar: `ExcelEMFixes-0.4.1.jar` (lane home top level and
  `build-root/`), **316,356 bytes, SHA-256
  `bb5067d82bf6cd7060dbdd57285aded75fb7808020ac1c1e9ff57def3ac43217`**,
  built twice byte-identical (deterministic, house build.sh).
  (First build was 316,339 B / `241ed57a…`; the rig caught a
  real funds-check defect — §4.2 — and the jar was rebuilt
  once with the fix. The hash above is the final jar.)
  Version stamps moved 0.4.0 → 0.4.1 in all five places
  (ExcelEMFixes.VERSION, plugin.yml, build.sh, README install
  line, rig classpath).
- Deploy config: `deploy/config.yml` in the lane home =
  build-root deploy config (0.4.0 deploy + the field-medic
  buy-flow block; the module runs shipped defaults).

## 4. Proof

### 4.1 Unit suites

- Main suites (`run-tests.sh`): **785 checks total** (735
  merged baseline + **50 new `VendorFlowTest` checks** — potion
  definition fields; registry contract against the real
  ExcelItems core: builtins + bandage 6 + potion 7 → 6
  definitions, nextId 8, duplicate ID/key claims rejected; the
  ID-7 config-trap pin; the full D200 price matrix; Prices
  validation; the arm ledger's snapshot / replace / expiry /
  consume-once semantics). Clean runs: **785 passed, 0
  failed**. All 50 new checks pass in every run.
  - **Pre-existing flake, NOT this build:** the skill-nudge
    check "Jester-shaped: armor named first on the tie" fails
    intermittently (~2 of 3 runs) in BOTH this tree and the
    pristine merged 0.4.0 tree (verified by running the merge
    lane's own suite 3×: 735/0, 734/1, 734/1). Cause:
    `SkillNudgeMathTest` feeds `Map.of(...)` inputs, whose
    iteration order Java deliberately randomizes per JVM
    launch, into a tie-break that follows iteration order.
    Flagged for the parent — a one-line test fix (ordered
    map) in the skill-nudge lane; out of this build's scope.
- Mount suites (`run-mount-tests.sh`): **50 passed, 0 failed**
  (regression — the merged tree is untouched outside
  field-medic).

### 4.2 Rig (Paper 26.2 + EliteMobs 10.9.10 + ExcelItems
0.1.0 + ExcelShops 1.3.11 with the STAGED catalog config +
GriefPrevention + Vault/EssentialsX economy)

Rig home: `rig/` (driver `rig-src/VendorRig.java`, harness
`rig/run-scenarios.py`, receipts `receipts/rig-vendor-*.json`).
All three scenarios **SEVERE 0** (excluding driver lines).

**full — 21/21 driver checks, 3/3 log checks.** Registry:
id 6 = field_bandage, id 7 = strong_healing_potion, 7
definitions, nextId 8; both commands registered. ExcelShops'
own `SpecialProducts.parse` (run reflectively against the
staged config + live registry — see the protection-only note
below) resolves the potion listing: delivery
`fieldmedic buy RigRunner`, price 15000 minor units, registry
id 7. Purchases through the real commands against real Vault
money, $20,000 start: shop potion armed $150 → confirm →
$19,850 + a splash potion whose PotionMeta base type is
STRONG_HEALING and which ExcelItems identifies as id 7;
vendor potion armed $112.50 → $19,737.50 + second potion;
`guildvendor buy pearl` armed $300 → $19,437.50 + 16 ender
pearls; `guildvendor buy emptymap` armed $75 → $19,362.50 +
1 MAP. Insufficient funds ($50 vs $150): refused, balance
untouched, nothing delivered. Full inventory: refused before
charging. Bandage regression: identifies as id 6, heal
50.0 → 85.0 (exactly the 35% trickle).

**modoff — 4/4 driver checks.** field-medic disabled:
registry holds only builtins + mount charm (5), ids 6/7
unclaimed, neither command exists, ExcelShops still enables.
**The gate A/B:** ExcelShops' own parse, run the same way
with the module off, throws `Special product
strong_healing_potion names unknown ExcelItems registry id 7`
— the exact failure the Unit 2 hard gate protects against,
present without this build and gone with it.

**noitems — 2/2 driver checks, 2/2 log checks.** ExcelItems
jar held: pack enables, field-medic stands down with its
named-dependency line, neither command exists, food-heal
unaffected.

**Rig-caught defect, fixed before filing:** the first full
run's insufficient-funds fixture FAILED — a $50 balance
confirmed a $150 potion and ended at −$100. EssentialsX's
Vault economy withdraws into negative balances, so the mount
pattern's "withdraw, then infer from transactionSuccess"
shape is not a funds check. `VendorBuyFlow.confirm` now
checks the balance BEFORE withdrawing (post-withdrawal
mapping kept as backstop); the fixture passes. **The
summon-mount module's BuyFlow carries the same latent hole**
(same shape, never fixture-tested for funds) — flagged for
the parent as a follow-up patch; not changed here (its rig
proof is complete and this lane's scope is the vendor
flows).

**Protection-only note (honesty about coverage):** on this
rig ExcelShops enters protection-only mode ("Session proof
not eligible" — the proof lives in the live data folder,
which the rig deliberately does not carry), so its boot-time
MobileCatalog load never runs here. Consequences: (1) the
parse proof above is reflective, using ExcelShops' own code
against the staged config and the live registry — equivalent
for the parse question; (2) the boot-time mobile-catalog
behavior with Unit 2 placed is NOT rig-proven either way.
The §5 note's ExcelShops repackaging step stands on source
evidence (exact key-set validation + the shipped jar's
bundled menu lacking the potion block), not on a rig boot.

## 5. GATE-RELEASE NOTE (for the parent)

**Catalog Unit 2's hard gate can be released** once the
parent re-stamps and files this build, on these conditions:

1. **The Unit 2 window must install three things together:**
   (a) this pack jar (0.4.1 as stamped here; parent
   re-stamps) — it registers registry ID 7 and provides
   `fieldmedic buy`; (b) the staged ExcelShops config with
   the potion listing (catalog pass staged files, Unit 2 as
   filed); (c) **an ExcelShops jar repackaged with the
   updated potions menu as its bundled `mobile-menus/`
   copy** — an ExcelShops-side step, NOT part of this build.
   Without (c), placing (b) leaves the Bedrock shop catalog
   unavailable (MobileCatalog's exact key-set check fails on
   the bundled menu, which ships only the 4 baseline
   specials) while ExcelShops itself stays enabled and Java
   is unaffected. The repackaging uses ExcelShops' own
   packaging tool (`~/workspace/excelshops/project/tools/package_candidate.py`
   bundles the installed menus); the updated menu file
   already exists in the catalog pass's staged ExcelMenu
   files (`spawnshop_potions_protection.yml` with the
   `strong_healing_potion` block, lore $150).
2. **NPC rows can be placed with the same window** (data of
   record, no EM file edits): potion
   `fieldmedic buy %player_name% npc` $112.50; pearls
   `guildvendor buy pearl %player_name%` $300; EMPTY_MAP
   `guildvendor buy emptymap %player_name%` $75 — all three
   delivery commands exist and are rig-proven at those
   prices once (a) is installed.
3. **Ordering inside the window:** jar swap before the
   ExcelShops config placement (the parse resolves ID 7 at
   shop load; jar-first is the safe order regardless).
4. The module's price knobs ship at the batch defaults
   ($150 / $112.50 / $300 / $75); the ExcelShops listing
   price and `potion-shop-price` must stay in agreement —
   the shop menu refuses to load on a lore/listing mismatch
   by ExcelShops' own validation.

## 6. Records written

- This RECORD.
- Daily log: one line appended to `~/memory/2026-11-23.md`.
- Backlog: note appended to P-EM-COMBAT-PACKAGE-20261122 in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
  (NOT closed — install + closing announcement still ride
  after).

## 7. Addendum — combined follow-up lane (Nov 23, 2026)

Full record: `~/workspace/mount-fix-excelshops-repack-20261123/FIX-RECORD.md`.

- **The jar in §3 is superseded.** The summon-mount BuyFlow
  carried the same withdraw-then-infer hole §4.2 caught here
  (at the $7,500 price point); it now checks the balance
  BEFORE withdrawing, mirroring `VendorBuyFlow.confirm`. The
  0.4.1 jar at this lane's top level is now **316,540 B,
  SHA-256 `0ce10eee6e9b3c3ae8abcffd5addffba31d267d60eedcdffe8c340a204bbd81a`**
  (built twice byte-identical) — file THIS jar, not
  `bb5067d8…`. Mount rig with a new insufficient-funds
  fixture: full 30/30 (balance $50 → $50, no charm, no
  dangling arm), modoff 3/3, noitems 4/4, SEVERE 0. This
  lane's vendor rig re-run against the fixed jar: 21/21 +
  3/3, 4/4, 2/2 + 2/2, unchanged.
- **D202 price ruling folded in:** Mount Charm $15,000 /
  $11,250 NPC in the deploy config (§3's deploy config is
  now `1838b627…`); shipped defaults stay 7500 / 5625 with
  both numbers documented (src + deploy comments, README
  table).
- **§5 condition (c) is produced:** ExcelShops candidate jar
  `ExcelShops-1.3.11-unit2-candidate.jar` (275,652 B,
  `f8b73d74…`) — live jar with exactly one entry changed
  (the bundled potions menu gains the potion block;
  `package_candidate.py` cannot process 1.3.11 — see the
  FIX-RECORD). ExcelShops' own MobileCatalog validation,
  run against the candidate's bytes + the Unit 2 config:
  ACCEPTED with all 5 specials. **Pairing constraint: the
  candidate jar is valid ONLY with the Unit 2 config —
  install together before any boot; rollback restores
  both.**
