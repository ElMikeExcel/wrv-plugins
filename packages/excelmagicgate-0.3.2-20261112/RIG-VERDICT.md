# ExcelMagicGate 0.3.2 — Rig Verdict

Rig: the 0.3.1 rig copied whole into this folder
(`magicgate-032-build-20261112/build-0.3.2/rig/server/`), with the
paperclip jar replaced by **Paper 26.3-214** (the live server's
version; the Mojang 26.3 server jar + patched Paper jar were taken
from the halloween-event rig cache because this VM's Java runtime
cannot reach Mojang's download host directly — paperclip verified
and used them locally). Magic 11.2.5 co-loaded, ExcelGuildGate
0.1.0, ExcelWandBars 0.3.11, LuckPerms; stage 4 applied (ceiling
master). ExcelMagicGate jar under test:
`EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b` (247078 B,
SHA-256 895cf91d…).

Rig driver: the carried FloorRigDriver, recompiled for Paper 26.3
with two rig-only commands added (`seeddead`, `materialize`); it
never ships. Fake players via the driver's Carpet-style maker.

**SEVERE count is 0 in every boot below. `getMode` (the NPE
signature from the diagnosis) appears 0 times in every boot.**

## Boot C — the gate proof (bootC-gate.log) — HEADLINE

Fixture: a rig-only CONFIG mismatch. The rig's ExcelMagicGate
config (never the shipped config — the shipped config.yml is
byte-identical to 0.3.1) gained one supplemental grant,
`zzdeadspell`, under apprentice — a key with NO spell definition
in Magic's spells.yml, i.e. exactly a grant candidate that
resolves to no SpellTemplate. RigGate (500 credited = apprentice)
was issued a beginner wand and held it (activation leg).

- Rank reconcile: RigGate stepped beginner → apprentice, pool
  150/25, verified on the wand (carried behaviour unchanged).
- **The gate fired, naming the key at WARN:**
  `Wand inventory sync: spell 'zzdeadspell' for RigGate has no
  template in Magic — skipped, wand data/config mismatch. The key
  stays off the wand until the wand data or spells.yml is fixed;
  the rest of the sync continues.`
- **Valid keys still granted:** 35 spells granted at apprentice —
  archer, arrow_ice, …, wolf — verified on the wand. `zzdeadspell`
  is NOT among them.
- **Completion accounting (WARN):** `Wand inventory sync accounting
  for RigGate at apprentice: 35 granted, 0 unverified, 1 skipped
  (no template in Magic: zzdeadspell) — skipped keys were not
  attempted and are not counted as granted.`
- Later sweeps re-ran the gate over the still-missing dead key:
  each completed through the all-skipped path, and
  `/magicgate status` reported `Last sync: RigGate @apprentice:
  0 granted, 0 unverified, 1 skipped (no template in Magic:
  zzdeadspell) (nothing grantable was missing)`. (Standing-sync
  behaviour: the WARNING repeats per sweep while the mismatch
  stands — the key stays a named, skipped candidate until the
  data/config is fixed; it is never granted and never NPEs.)
- Zero `getMode` lines; zero SEVERE; clean `stop`.

## Boot B — the Mike fixture replay (bootB-mike-fixture.log)

Fixture: a MUTATED COPY of .MikeExcel's pulled mage data
(`~/workspace/magic-break-diagnosis-20261112/pull-bm228/mike-mage.dat`,
original SHA-256 8478a1ab…; the original pull was never edited).
Mutations (fixture copy in `rig/fixtures/mike-mage-deadkey.dat`,
SHA-256 e041db7c…): (1) a `zzdeadspell` entry added to the mage's
learned-spells section; (2) `zzdeadspell:25` added to the bound
wand item's spell_inventory in its custom_data. The copy was
placed as the mage data for Mike's UUID; earned row seeded at
1,210 credited (his live placement shape, apprentice).

- Join leg deferred (bound wand not carried), then `frig
  materialize` put the bound wand item in the fake player's
  inventory and `frig hold` fired the activation leg.
- Rank reconcile: stepped beginner → apprentice (2 upgrades),
  pool 150/25, verified on the wand.
- Inventory sync granted **34 spells** at apprentice, verified on
  the wand; final wand read: path apprentice, 52 spells.
- Zero `getMode` lines; zero SEVERE.
- FINDING (recorded, not hidden): the planted `zzdeadspell:25`
  spell_inventory entry did NOT surface in the wand's runtime
  spell set — Magic rebuilds a wand's spell set from resolvable
  templates when the mage loads, so an inventory-only dead entry
  is dropped before the sync ever sees it, and the learned-spells
  entry alone is not a grant candidate. A dead key therefore
  reaches the sync's gate only as a grant CANDIDATE (a path list
  or supplemental grant naming a key Magic has no template for —
  the Boot C shape), which is exactly the seam Fix 1 guards. A
  dead key that survives INTO a wand's live spell set through
  some other representation remains Magic's own buildInventory
  hazard and is the data-cure's business (diagnosis Fix 2), not
  this build's scope.

## Boot A — seeding probe (bootA-heldkey.log)

`frig seeddead RigProbe zzdeadspell` called Magic's own
`wand.forceAddSpell("zzdeadspell")` directly: it returned **false,
no throw, key not added** — forceAddSpell null-checks the template
for the key being added (diagnosis §1.2), so a dead key cannot be
planted through the API. Dead keys arrive via data/config only,
which is why both fixtures above plant them in data/config.

## Boot 0 — smoke (boot-smoke-263.log)

First 26.3 boot of the shipped jar: plugin list carries
ExcelMagicGate (0.3.2), enable line carries the 0.3.2 build ID,
Earning ON, sync ON, fake player + mgive + wand read all work on
Paper 26.3 with the recompiled driver. Zero SEVERE.
