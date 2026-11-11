# BUILD RECORD — ExcelBossTrial 0.5.5 (Warden first-break layer)

Built Nov 10, 2026 (desk lane, board supervisor run 146 dispatch).
Commission: Mike, Nov 10, 2026, APPROVED — the Warden first-break
package (daily log lines 487 + 543): the live repeat package PLUS
first-break-only extras, live before the Sat Nov 14 Warden
attempt. Design of record:
`~/workspace/warden-firstbreak-20261110/DESIGN-AND-BUILDSCOPE.md`
(followed exactly; deviations listed at the end of this record).
Cash settled by decision-log D6: **$10,000 per champion TOTAL —
it replaces the $5,000 repeat rate on the first break, NOT
additive** (implemented as one first-break slate with
`first-break.warden.cash-per-champion: 10000`; the repeat layer
never runs on a CREDIT verdict, so nothing can stack).

Desk only: no panel access, no installs, no MANIFEST edits
(a draft entry is staged beside this record), no sends.

## Candidate jar (FINAL)

- `~/workspace/bosstrial-055-build/ExcelBossTrial-0.5.5.jar`
  — **455,307 B / sha256
  eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb**
- Build ID: **EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f**
  (fresh tag; 0.5.4's 5371d5, 0.5.3's 35bd2b, 0.5.2's 13447a
  and 0.5.1's a3aa16 never reused).

## Base proof

- Tree copied tree-identical from the shipped 0.5.4 build tree
  (`~/workspace/elder-sprinkle-melee-fix-20261110/build-054/bosstrial/`).
- Pristine `package.sh` rebuild of the untouched copy is
  byte-identical to the shipped 0.5.4 jar: 443,874 B / sha256
  aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2
  (`work/base-pristine.jar`, log `work/base-build.log`), suite
  green (2,502 checks / 90 harnesses / 0 failures by this lane's
  tally method — see TEST-RESULTS.txt for the method note)
  before any 0.5.5 edit.

## The problem this build fixes (design §1.1, verified in source)

In 0.5.4, `applyRepeatLayer(...)` runs ONLY on REPEAT_CREDIT.
A first break (verdict CREDIT) pays the signature pool +
ordinary bonus rolls and repeat *bookkeeping* only — no cash,
no accelerants, no war chest, no cosmetic, no healer
recognition, and the jackpot table is repeat-only. The
commission's "repeat package PLUS extras" is therefore a real
layer, not a config tweak. 0.5.5 adds exactly that layer,
Warden-only.

## The change (complete list — tree diff vs the 0.5.4 tree)

Modified: `config.yml`, `plugin.yml`, `excel-build.txt`,
`src/.../ExcelBossTrial.java`, `src/.../core/BossTrialConfig.java`,
`src/.../core/BuildInfo.java`,
`tests/.../core/PluginYmlParseTest.java`.
New: `src/.../core/FirstBreakRewards.java`,
`tests/.../core/FirstBreakRewardsTest.java`,
`tests/.../core/FirstBreakConfigTest.java`.
Nothing else differs.

**Core (new `core/FirstBreakRewards.java`, pure logic):**
- `eligible(verdict, kind, elderProgress, alreadyPaid, spec)` —
  the design §2.1 matrix: CREDIT + WARDEN + not elder-progress
  + layer enabled + once-flag unpaid, and nothing else, ever.
  REPEAT_CREDIT never fires it; non-Warden chapters never fire
  it; a set flag silences even a fresh CREDIT after an admin
  revoke + re-break (the exactly-once belt over the verdict's
  braces).
- `Spec` — the first-break slate, config-shaped under
  `first-break.warden.*` mirroring the `repeat:` key names;
  `Spec.settled()` IS the commissioned table (cash 10000,
  jackpot +2, mcMMO 5000 SWORDS rail, MagicLevel 250,
  claimfly 2.0, premium on, healer on, cosmetic ECHO_SHARD,
  title "Sealbreakers", the §2.4 kit).
- `KitItem` + `Spec.settledKit()` — the EM starter kit exactly
  as designed: GOLDEN_CARROT ×16, ENCHANTED_GOLDEN_APPLE ×1,
  GOLDEN_APPLE ×4, TOTEM_OF_UNDYING ×1, SPLASH_POTION Healing II
  ×4, POTION Strength II ×2, POTION Swiftness II ×2,
  MILK_BUCKET ×2, EXPERIENCE_BOTTLE ×16. Vanilla consumables
  only, no gear. Rows clamp loud-free in the record (count ≥ 1,
  tier 1–2, effect keys upper-cased).
- `titleLore(date)` / `trophyLore(earner, date, party)` /
  `partyLine(...)` — the item text of record: title lore
  "Awarded for the first breaking of the Warden seal." +
  "The Deep listened — and the party answered. <date>.";
  trophy = `ProvenanceLore` earner lines + "First break party:
  <names>". Trophy constants: material ECHO_SHARD, name
  "Heart of the Deep — Warden Trophy", distribution id
  `first-break-v1`.

**Config (`core/BossTrialConfig.java`):** new canonical
constructor adds `firstBreakWarden` (the 0.3.5-shape
constructor delegates with `Spec.settled()`, so every existing
construction site is unchanged); `fromMap` gains
`firstBreakFrom` — flat keys + the EM-kit list-of-maps in the
`titlesFrom` shape; malformed kit rows (no material) are
skipped, never fatal; a `str` helper joins `bool`/`num`.

**Host (`ExcelBossTrial.java`):**
- State: `firstBreakWardenPaid` + the plaque record (date,
  party UUIDs, party names), inline-initialized (bare-instance
  safe). `loadState` restores `first-break-paid.warden` +
  `first-break.warden.*` defensively and logs the record when
  paid; `stateDocument` writes the keys only when paid
  (additive — a 0.5.4 rollback ignores them, and a paid flag
  surviving a rollback is what keeps a later 0.5.5 return from
  re-paying).
- Kill path: eligibility is computed and
  `markFirstBreakWarden(outcome)` sets the flag + record and
  `saveState()`-flushes **before `applySealLoot`** — i.e.
  before ANY first-break reward moves, the jackpot branch
  included (house pay-order discipline: a crash after the
  flush can underpay, never double-pay). The method logs the
  design's plaque line verbatim in shape:
  `FIRST-BREAK WARDEN: party=[…] date=… (once-flag flushed
  before payment).`
- `applySealLoot` gains the first-break branch: a fresh Warden
  CREDIT draws `FirstBreakRewards.rolls(...)` (bonus + jackpot
  extras) over the combined bonus + jackpot table with
  Underdog count scaling — the repeat shape, log label
  "first-break jackpot". All other kills take the exact 0.5.4
  roll paths.
- `applyFirstBreakLayer(...)` beside `applyRepeatLayer`,
  rail-for-rail: cash via `depositCash` to every
  `presentChampions` member at the first-break rate;
  accelerants via `grantMcMmoXp`/`grantMagicLevelXp`
  (Underdog-scaled) to online roster champions; then
  `storeFirstBreakClaims(...)`; then the server-wide headline
  (`messages.first-break-headline`, config-owned text).
- `storeFirstBreakClaims(...)` modeled on `storeRepeatClaims`
  (run suffix `:first-break`, distribution `first-break-v1`):
  per roster champion — the Warden Tribute cosmetic
  (ECHO_SHARD, same rail as the repeat cosmetic), the
  Sealbreakers NAME_TAG, the provenance-stamped Heart of the
  Deep trophy, every EM-kit row, and the Support Commendation
  for the `HealingAttribution` support champion; war-chest
  adapter services (DeathProtection premium, ClaimFly hours)
  pay directly to online roster champions exactly as in the
  repeat store. The L&F voucher stays OFF (no row exists in
  the first-break store — same load-guard reason as repeat).
  Auto-present rides `presentToOwners` like the repeat store.
- `buildKitStack(...)` + `potionTypeFor(...)` — the one
  genuinely new item-building code (design §2.4): PotionMeta
  base potion types for kit rows (tier 2 → STRONG_ variants),
  with a custom-effect fallback so an unmappable config row
  can never silently pay a plain water bottle.
- Boot line (after the Repeat layer line):
  `First-break layer: ON (warden only, cash $10000/champion
  replacing the repeat rate, jackpot +2 rolls, title
  "Sealbreakers", EM kit 9 rows; warden unpaid).` — the paid
  state reads from the restored record.
- The existing first-break bookkeeping is untouched:
  `recordRepeatClear` still runs on CREDIT (24 h window +
  clear #1 toward Hushwalker), and `applySealLoot` still pays
  signature generation 0. No command surface is added; the
  layer is verdict-gated, not player-triggerable (the
  "hold the button down" answer: there is no button).

**Guild-hall plaque (design §2.3):** deliberately NOT coded —
no guild-hall coordinates exist in any source. The plugin's
FIRST-BREAK record (state.yml + the log line) is the
deliverable the post-fight manual plaque step in
INSTALL-PACKET.md reads. No coordinates were invented.

**Bundled config:** the `first-break:` block + the
`messages.first-break-headline` key. Install shape is a
MERGED config (never wholesale-replace): see
INSTALL-PACKET.md + `live-config-merge/`.

**Version surfaces:** `plugin.yml` 0.5.5 + the 0.5.5
description sentence (no colon-space; parse-pinned);
`core/BuildInfo.java` rotation; `excel-build.txt`.

## Tests

- Full suite via `package.sh` (both builds): **0 failures**.
  Tally (this lane's stated method, TEST-RESULTS.txt):
  **2,588 checks / 92 harnesses**, vs the base proof's
  2,502 / 90 with the same tool — delta **+86 checks /
  +2 harnesses** = FirstBreakRewardsTest 56 (new) +
  FirstBreakConfigTest 21 (new) + PluginYmlParseTest +9
  (48 → 57), exactly the new pins.
- FirstBreakRewardsTest pins: the full eligibility matrix
  (CREDIT+unpaid fires; REPEAT_CREDIT never; CREDIT+flag
  never — including the simulated revoke + re-break sequence;
  non-Warden never; elder-progress never; disabled never),
  the settled slate (cash exactly 2× the repeat rate as a
  TOTAL), roll counts, the kit contents row-for-row exact +
  the no-gear pin, title/trophy lore with a fixed date.
- FirstBreakConfigTest pins: code defaults == `Spec.settled()`
  whole; flat-map overrides parse (cash/jackpot/kit);
  material-less kit rows skip; the repeat block's defaults
  are untouched by the new parse.
- PluginYmlParseTest pins: version 0.5.5, the 0.5.5 sentence,
  and the bundled first-break keys (enabled, cash 10000,
  jackpot 2, accelerants 5000/250, title, kit = 9 rows,
  headline present).

## Deterministic double-build

`package.sh` twice → byte-identical: `work/pkg1.jar` =
`work/pkg2.jar` = the candidate above (455,307 B /
eade52aa…). Jar contents: 234 entries (0.5.4: 231; +3 =
`core/FirstBreakRewards.class` + its `Spec`/`KitItem`
inners); name-level class diff vs the 0.5.4 jar is exactly
those three additions (host/config/BuildInfo classes changed
in place under their existing names); `excel-build.txt` at
the jar root carries the 0.5.5 build ID; bundled
`plugin.yml` reads version 0.5.5; bundled `config.yml`
carries the first-break block.

## Fingerprint verification (release gate)

Layer B homes all carry the new ID, verified in the built
jar: (1) `excel-build.txt` at the jar root; (2) the
fragment-assembled constant in `core/BuildInfo.class`
(887e6f present, 5371d5 absent — verified by string scan of
the class); (3) the boot surface line
`Build: EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`,
observed in the rig boot below.

## Rig boot verification (desk rig, candidate jar)

A copy of the 0.5.4 desk rig (`rig-055/`, Paper 26.3-235)
with the candidate jar swapped in and the rig config
untouched (the rig config predates the first-break block —
the layer runs on its code defaults, which is itself part of
the proof). Boot log `work/rig-boot.log` (35,339 B / sha256
371169ef277e4f9f4270868719e74a7a70b1591459837d787b7d265816a5eee5),
**SEVERE 0**:
- `Enabling ExcelBossTrial v0.5.5`
- `Build: EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`
- `Repeat layer: ON (window 24.0h base, signature generations
  on, cash $5000/champion, jackpot +2 rolls).` — unchanged.
- `First-break layer: ON (warden only, cash $10000/champion
  replacing the repeat rate, jackpot +2 rolls, title
  "Sealbreakers", EM kit 9 rows; warden unpaid).` — exact.
- The rig's standing RigDriver warden probes ran (boom/melee
  bracket checks) with no BossTrial error; the only BossTrial
  WARN is the standing EliteMobs-seam-unavailable fail-soft
  (no EliteMobs in the rig set), same as the 0.5.4 rig.
  Remaining ERROR lines in the log are offline-environment
  noise (Mojang services / Paper version fetch), none from
  BossTrial.

**Rig scope limit, stated plainly:** this rig set carries no
verdict-driving script — RigDriver0211 probes warden damage,
it does not fight a seal-crediting kill (its wardens closed
as SERVER_STOP probe fights at 0.4% share). The first-break
PAYMENT path (verdict → flag → claims) is therefore not
rig-exercised; it is unit-proven (eligibility matrix +
exactly-once sequence) and rides the repeat layer's proven
rails. The Nov 14 Warden fight itself is the behavior proof,
and the flag + FIRST-BREAK record make any miss auditable
and hand-fixable from the log — the division the design
§3.2 itself settles ("a live-player proof is NOT owed
before Nov 14").

## Design deviations

None in substance. Two implementation notes, neither
changing a designed behavior:
1. The headline text ships with an em dash rendered from the
   config's `\u2014` YAML escape (SnakeYAML double-quoted
   scalar) — the parsed string is byte-identical to the
   design's intent; PluginYmlParseTest pins the parsed key.
2. The trophy's provenance lines are built through
   `buildNamedPaper` with the `ProvenanceLore` line TEXT
   (earner + date) styled `&7&o` exactly as `stampProvenance`
   renders them, plus the `&7` party line appended — the
   design's "provenance-stamped lore (via ProvenanceLore)"
   with the party line the same item must also carry.
