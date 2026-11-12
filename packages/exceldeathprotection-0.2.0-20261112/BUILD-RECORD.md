# ExcelDeathProtection 0.2.0 — build record

Commission: board supervisor dispatch (Nov 12, 2026) — build
ExcelDeathProtection 0.2.0 (premium banked-credit seam) per
`~/workspace/deathprotection-premium-seam-20261110/BUILD-BRIEF.md`
and `DESIGN.md`, semantics per decision **D9** (banked credit) in
the decision ledger.

Desk work only: built, unit-tested, rig-proven. Nothing installed,
nothing published, no MANIFEST edit (a draft entry is staged for
the board supervisor to file — `MANIFEST-ENTRY-DRAFT.md`), no
messages sent.

---

## 1. BASE PROOF (before any edit)

Base = the recovered **0.1.3** tree at
`~/workspace/deathprotection-source-recovery-20261112/recovered-0.1.3/ExcelDeathProtection/`
(wrv-plugins PR #13, commit `18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd`),
copied whole into this build tree. Proof, run on the copy before
editing:

- All **13/13** files in SOURCE-MANIFEST-0.1.3.json's
  `exact_tested_implementation_build_resources_and_tests` list
  match on size + SHA-256 (main class 30,266 B /
  `4a1a33cc…ad3`, PurchaseCoordinator, PurchaseJournal,
  PurchaseReviewMenu, the four test files, plugin.yml, config.yml,
  pom.xml, build.sh, .gitignore).
- Full-tree hash comparison copy-vs-base: **no differing files**.
- The brief/design were written against the 0.1.1 state model;
  the recovered 0.1.3 model differs (two-tier BASIC/PREMIUM with a
  shared active slot). No conflict with the brief's scope was
  found — the one adaptation it forces is recorded as a desk
  decision in §3.1, not improvised silently.

## 2. WHAT WAS BUILT — brief scope item by item

| Brief § | Implementation |
|---|---|
| 1. Exact API class + static methods BossTrial probes | `ExcelDeathProtectionAPI` (first probed name) with public static `creditPremium(Player)` and `creditPremium(Player, int)` — int is exact, return is boolean, never throws (C1–C8 of the design all satisfied; probe-replica tests pin them) |
| 2. Durable per-player credit store | INTEGER count in the player's PDC under `premium-credits`, beside 0.1.3's `active`/`tier` keys — persists with player data (rig-proven across a restart, §4) |
| 3. D9 semantics | `PremiumCredits` (pure logic): no active protection → first unit activates PREMIUM, remainder banks; any active tier → full amount banks, never overwrites; banked credits re-arm PREMIUM automatically in `onRespawn` after a death consumed the protection; never an item, cannot be lost/dropped/traded |
| 4. Idempotency | No DP-side dedup (per design §3.5: the seam carries no verdict id and a time-window dedup would eat real rewards); BossTrial-side flush-before-pay is the dedup of record. Every credit writes an INFO audit line (`Premium credit: <name> +<n> (active=…, banked=…, source=…)`) |
| 5. Console-admin surface | `/dp credit <player> [amount]` (grant, same code path as the API, log source=admin), `/dp uncredit <player> [amount\|all]` (revoke), `/dp status <player>` for console/admin senders (tier + banked count), player `/dp status` gains the banked line when non-zero, `/dp version`. All admin-gated on the existing `exceldeathprotection.admin` permission; tab-completion updated |
| 6. No money changes | `basic-price: 15000.0` / `premium-price: 30000.0` byte-preserved; a unit test pins both |

Also per design §3.4/§3.6: the `premium:` config block
(`enabled: true`, `auto-rearm: true`, `max-per-call: 5`,
`max-banked: 0` = uncapped — all defaults, so a 0.1.x config with
no block runs the seam ON), the boot line
`ExcelDeathProtection API: premium-credit seam active (creditPremium)`,
the plugin singleton bridge the static API needs, and same-pass
doc surfaces inside the tree (CHANGELOG 0.2.0, README API section,
PLAYER-GUIDE banked-credit note).

**Preserved 0.1.3 behaviour (regression-pinned by its own suite):**
two-tier purchase flow with the one-item review menu, the purchase
journal + unresolved-transaction locks, legacy PDC migration
(legacy `active` byte with missing/unknown tier resolves to
PREMIUM in `getTier` — untouched), premium-potion drinking rules,
basic boss bar, XP untouched on protected deaths, keep-inventory
with Curse of Vanishing behaviour as 0.1.3 recorded it.

## 3. DESIGN DECISIONS MADE AT THE DESK (named)

1. **Credit while BASIC is active banks, it does not upgrade.**
   The design was written against 0.1.1's binary flag ("if
   protected, bank"). In 0.1.3's two-tier model the faithful
   reading is: any active tier (BASIC or PREMIUM) banks the full
   credit. Overwriting a basic protection the player paid $15,000
   for with the credited premium would silently consume the basic
   entitlement; banking loses nothing, and at the death the basic
   protection covers, the banked premium re-arms in its place.
   One-line reversal if the supervisor prefers upgrade-on-credit:
   the branch lives in `PremiumCredits.applyCredit` callers'
   `active` flag — pass `getTier(player) == Tier.PREMIUM` instead
   of `isProtected(player)` and add the basic-consumption note.
2. **Revoke command spelled `uncredit`**, with `[amount]` default
   1 and an `all` form that clears the bank. (Also serves as a
   Layer C fingerprint marker.)
3. **Build recipe:** the tree's Maven build cannot run offline
   here (no cached Paper/Vault Maven artifacts in this VM), so —
   following the house pattern of the ExcelTour/TrainingDummy
   builds — `build.sh` is a deterministic javac recipe and the
   original Maven script is preserved as `build-maven.sh`;
   `pom.xml` is retained and version-bumped only.
4. **Test runtime:** the suite runs under Mockito 5.21.0 +
   ByteBuddy 1.18.3 (local Maven cache) instead of pom.xml's
   pinned 5.15.2 / 1.15.11, whose inline mock-maker cannot modify
   classes on this VM's JDK 25 (no JDK 21 exists here; the pinned
   pair failed all 34 PurchaseReviewMenu tests with a
   mock-maker init error while the other 84 passed — an
   environment failure, not a code failure). Test sources are
   unmodified by this choice except the one version pin below.
   With the newer runtime pair: **118/118**.
5. **One existing test edited:** `ResourceConfigurationTest`
   pins the plugin.yml version string; updated `0.1.3` → `0.2.0`.
   Nothing else in the carried suite was touched.

## 4. PROOFS

- **Unit tests: 118 checks, 0 failed** (`TEST-RESULTS.txt`) —
  99 carried from 0.1.3 (51 PurchaseCoordinator + 11
  PurchaseJournal + 34 PurchaseReviewMenu + 3
  ResourceConfiguration) and 19 new: `PremiumCreditsTest` (16 —
  activation, banking while premium/basic active, re-arm cycle,
  disabled seam, amount rejection/clamp, bank cap, revoke,
  bundled-config defaults + price pins) and
  `PremiumApiProbeTest` (3 — BossTrial's probe predicate verbatim
  resolves the class/method; the method is public static with an
  exact `int` parameter; both invoke shapes return
  `Boolean.FALSE` cleanly when the plugin is down).
- **Rig (Paper 26.3, local): phase 1 14/14 PASS, phase 2 3/3
  PASS, SEVERE 0 on both boots** — probe-shaped invoke activates
  PREMIUM, second invoke banks, console grant/revoke, three real
  death/respawn cycles (sword kept on protected deaths, re-arm
  from bank twice, bank-empty death stays off), the bank present
  in the saved `.dat`, and tier PREMIUM + banked 1 read back
  after a full server restart. Full account + the fake-player
  data-load driver artifact found en route: `rig-0.2.0/RIG-RESULTS.md`.
- **Double-build:** two clean compiles → two repacks →
  **byte-identical** jars (build.sh `cmp` gate).
- **Fingerprint gate:** `excel-build.txt` in the jar =
  `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348` (see FINGERPRINTS.md).

## 5. ARTIFACT

- `ExcelDeathProtection-0.2.0.jar` — **59,390 B**, SHA-256
  `ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719`,
  build id `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348`.
- `ExcelDeathProtection-0.2.0-source.zip` — **266,425 B**, SHA-256
  `8540c301912aa9de67382d96787fae9fc66e94ecd0596caf3c76f61910ca842e`.
- Fingerprints: Layer A — name ExcelDeathProtection, version
  0.2.0 real, `author: Argus` carried from the 0.1.3 lineage.
  Layer B — build id in `BuildInfo` (fragment-assembled),
  `excel-build.txt`, the enable line, and `/dp version`.
  Layer C — six markers, see FINGERPRINTS.md. Layer D — channel WRV.

## 6. NOT PROVEN HERE (named, not claimed)

- **Player proof owed after install:** a real BossTrial
  repeat/first-break verdict paying through the live seam (the
  rig used a verbatim probe replica, not BossTrial itself);
  `/dp status` banked line read in-game on Java + Bedrock;
  premium Conduit icon after a credit re-arm on a real client.
- **Purchase flow with a live economy provider** (rig had Vault
  but no provider): unchanged 0.1.3 code, unit-covered; a real
  basic + premium purchase remains the same player check 0.1.3
  already owed.
- No live-server boot was run (desk rig only); every live-state
  claim in the draft MANIFEST entry is marked GATE-VERIFY.
