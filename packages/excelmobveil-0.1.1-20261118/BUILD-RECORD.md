# M3 BUILD RECORD — ExcelMobVeil -> ExcelItems migration (ID 1, veil potion)

- **Worker:** M3 build worker, board supervisor run 559
- **Date:** Wed Nov 18, 2026, ~19:26-19:45 CST (America/Chicago)
- **Scope:** W3-MIGRATION-SCOPE.md §3 M3 + §4.1 gates G-1..G-6 + §4.2
  M3 gate. Pattern-proof piece: first plugin to hard-depend on
  ExcelItems and mint/identify through the registry.
- **Supervisor decisions applied (D124):** (1) mint = FULL
  delegation to ExcelItemsAPI.create(id); (2) version bump per
  house convention, stated here: **ExcelMobVeil 0.1.0 -> 0.1.1**
  (patch bump for the depend + mint/identify rewire; plugin.yml,
  pom.xml, the enable log line, and the jar name all carry 0.1.1).
- **Not done (per dispatch):** no MANIFEST filing, no install, no
  GitHub writes. State: **Built** (rig owed — see G-4).

## Candidate

| Artifact | Size | SHA-256 |
|---|---|---|
| ExcelMobVeil-0.1.1.jar | 61,819 B | `42ffa15514513366e973a4cefdd106a108bd31004ed166cbbbac3fca1863560d` |
| ExcelMobVeil-0.1.1-source.zip (migrated tree, 28 files) | 64,529 B | `480749132098aaf551abc2bf7992eb7d7a66f6404083d8c4904fe2e821ee26a9` |
| Compiled against: ExcelItems-0.1.0.jar (live build of record) | 20,214 B | `85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d` |

- **Double-build:** fresh javac compile + repack produced a
  byte-identical jar (same SHA-256) — deterministic build via
  `work/pack.py` (fixed entry order = live jar's order, fixed DOS
  timestamp 2026-11-18T00:00:00Z, fixed container metadata).
- **Delta vs live jar, per entry:** only the classes compiled from
  the one edited source file (ExcelMobVeil.java: the outer class +
  its inner/anonymous classes) plus plugin.yml, pom.xml,
  pom.properties (version), and MANIFEST.MF (Created-By line names
  the house packer instead of the Maven jar plugin). Every other
  entry is byte-identical to live. Spot proof: ExcelMobVeil$
  ConsumedSlot.class differs only in debug line metadata —
  `javap -c` bytecode output identical to live.

## What changed (source delta, ExcelMobVeil.java + descriptors only)

1. **plugin.yml:** version 0.1.1; `depend: [Vault, ExcelItems]`
   (hard depend added; softdepend unchanged).
2. **Mint — full delegation:** `createPotion(amount)` validates
   the amount, then returns `ExcelItemsAPI.create(1, amount)`.
   The registry mint dual-stamps primary `excelitems:id` INTEGER 1
   + legacy `excelmobveil:potion` BYTE 1 (ItemFactory rule), so
   the dual-stamp is preserved by construction. The pre-migration
   hand construction remains ONLY as the fallback for environments
   where the API cannot resolve (unit tests run serverless; with
   the hard depend, a live server always resolves the API).
3. **Identify — API primary-first / legacy fallback:**
   `isVeilPotion` keeps its null/type guard, then returns
   `ExcelItemsAPI.isItem(item, 1)`; the API reads the primary mark
   first and falls back to the legacy mark only when no primary
   mark is present (a forged/stale primary never falls through).
   Same serverless fallback as mint when the API is unreachable.
   Every drink/veil path (consume handler + its scheduled recheck)
   goes through `isVeilPotion`, so all of them are migrated.
4. **Behavior:** `onEnable` calls `registerVeilBehavior()`, which
   registers an `ItemBehavior` with handler id
   `excelmobveil:veil-drink` against ID 1 via
   `ExcelItemsAPI.registerBehavior(1, ...)` — matching the ID 1
   definition's pinned handlerId. Veil timing/stacking logic
   (VeilTiming, player-PDC deadline state) stays in MobVeil,
   untouched.
5. **pom.xml:** version 0.1.1 + ExcelItems 0.1.0 `provided`
   dependency (with the install-file step documented in a pom
   comment; the house javac route puts the jar on the classpath
   directly). Build route note: Maven Central is unreachable from
   this VM tonight (offline cache lacks the enforcer plugin; the
   online attempt failed on SSL through the egress proxy), so the
   build uses the house javac + deterministic-packer route — the
   same route that reproduced the live jar byte-for-byte (G-2).
6. **Tests:** one existing test updated for the intended
   descriptor change (ResourceConfigurationTest now pins version
   0.1.1 and depend [Vault, ExcelItems]); all other existing test
   files untouched. One new test class added (below).

## Gates

### G-1 — Live-premise fresh pull: PASS
Homebase attempt 1; live jar + config pulled same-day, plugin.yml
extracted from the pulled jar; hashes in RECEIPTS.md. Live jar is
byte-identical to the canonical source tree's verified-rebuild
jar, so the build bases on that tree with its premise proven.

### G-2 — Base byte-identical rebuild: PASS
Unmodified tree compiled with the house javac route (JDK 25,
--release 21, -g, build-libs Paper/Vault classpath): **all 27
class files byte-identical** to the live jar's classes. Packed
with the deterministic packer + container-metadata alignment:
**the rebuilt jar is byte-identical to the live jar**
(61,085 B, SHA-256 `66e7e508...80fc` = live; work/base-rebuild2.jar).
The migration delta is therefore fully isolatable to the edits
listed above.

### G-3 — Tests (incl. M3 stacking gate): PASS — 195/195
Harness: JUnit platform console + Mockito (javaagent), run from
the tree root. Control: the unmodified base suite passes 180/180
under the same harness (work/test-run-base.log). Migrated suite:
**195 found / 195 successful / 0 failed** (work/test-run-3.log):
- 180 existing (incl. the 1 descriptor test updated per above).
- 15 new in RegistryMigrationTest, backed by a fake API over the
  REAL registry core (BuiltinItems.registry()):
  - identify x8: primary-only PASS, legacy-only PASS,
    dual-stamped PASS, plain potion negative, forged primary (3)
    + legacy veil = NOT veil (no fallthrough), unknown primary
    (99) + legacy = NOT veil, other item (4) negative, splash
    shape with primary mark negative.
  - mint x2: createPotion delegates to create(1, amount) exactly
    once and the minted stack carries BOTH marks (dual-stamp);
    amount 0/65 throws before any delegation.
  - behavior x1: registerBehavior lands handler
    `excelmobveil:veil-drink` on ID 1, equal to the definition's
    pinned handlerId.
  - definition x1: ID 1 definition equals the legacy mint
    construction field-for-field (material POTION, colour
    0x63A8AA, base WATER, glint, translated display name + all 6
    lore lines string-identical to the old hand-built potion).
  - **M3 gate — stacking/time x3:** legacy-held drink adds
    exactly 480,000 ms (deadline 481,000 at clock 1,000);
    registry-minted drink identical; stacking sequences
    legacy+legacy, registry+registry, legacy+registry and
    registry+legacy ALL land on the identical 961,000 deadline
    with identical remainingMillis — no split identity.

### G-4 — Mirror load clean: RIG OWED (desk smoke PASS instead)
The mirror (Homebase C:\Users\MikeExcel\wrv-testserver\server)
is RUNNING its restored found state (java pid 31772) and holds
ExcelMobVeil-0.1.0.jar (61,085 B — matches live) but NO
ExcelItems jar (the r555 rig restored it out). A co-resident
boot requires stopping the mirror, and the established rig
convention stops it via RCON (`list`/`stop`). Tonight's interim
rule is ZERO console inputs from Iris while Jesús is on the PC
(console session Active on Homebase), so the mirror is **not
available without console inputs** and no boot was attempted; no
game client was driven. Desk smoke performed instead, all PASS:
- Bytecode linkage: candidate ExcelMobVeil.class invokes
  ExcelItemsAPI.get(), create(II), isItem(ItemStack,I) and
  registerBehavior(I,ItemBehavior) with descriptors EXACT vs the
  live ExcelItems-0.1.0.jar (javap both sides).
- JVM link smoke: candidate main class loads and verifies
  against ExcelItems-0.1.0.jar + the full Paper runtime
  classpath in a plain JVM (LinkSmoke: loaded+verified,
  createPotion/isVeilPotion resolved).
- Co-residency shape: candidate depend [Vault, ExcelItems] vs
  ExcelItems' own plugin.yml (name ExcelItems, v0.1.0); jar
  entry overlap is only each jar's own plugin.yml/config.yml.
**Rig owed before filing (exact):** mirror boot with
ExcelMobVeil-0.1.1.jar + ExcelItems-0.1.0.jar co-resident —
ExcelItems enabling x1, ExcelMobVeil enabling x1, MobVeil
enable line at 0.1.1, `ExcelItems enabled: 4 custom item(s)
registered, next free id 5.` x1, SEVERE 0, dependent enables
AFTER ExcelItems; then restore the mirror to found state.

### G-5 — Install discipline: N/A this run (no install; failsafe
window owns it at filing time).

### G-6 — Same-pass surfaces: CHECKED, no player-facing change
- What's New (in-game changelog + website section): NO entry —
  same potion, same name/lore/price ($2,000), same drink/stacking
  behavior; the change is internal (where the potion is minted
  and how it is recognized). Check result stated here per G-6.
- Website pair: no change (same reasoning).
- Guide: HELD under hibernation (Mike, Nov 16, 2026); owed on
  wake: none expected (no behavior change to teach).
- Tips: checked — no veil wording changes; no tip edit owed.
- Menus (ExcelMenu + ExcelBedrockMenu): no change — the shop
  still reaches the potion via the unchanged `mobveil buy`
  console command; no slot/price/reference on any menu changes
  in M3 (the by-ID shop reference is M4's work).

## Known semantic note (recorded, accepted by design)

The registry's legacy fallback (ItemFactory) tests the legacy
mark's PRESENCE; the pre-migration isVeilPotion additionally
required the BYTE value to equal 1. Every legitimately minted
veil potion carries BYTE 1, so no real item changes identity;
the registry rule is the design-law identification going
forward, and the API path is what runs on a live server. The
serverless fallback in isVeilPotion keeps the exact old
value==1 check for test environments.

## Files

- Candidate + source zip + SHA256SUMS: this folder.
- RECEIPTS.md: G-1 pull receipts. live-pull/: the pulled live
  jar + config (evidence).
- work/base/: pristine tree copy; work/base-rebuild2.jar: the
  byte-identical base rebuild (G-2 proof). work/migrated/: the
  migrated tree the candidate was built from. work/pack.py: the
  deterministic packer. work/test-run-base.log (180/180 control),
  work/test-run-3.log (195/195 candidate), work/base-build*.log:
  the Maven attempts that document why the javac route was used.
