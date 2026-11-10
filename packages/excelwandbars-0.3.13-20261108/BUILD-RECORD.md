# Cooldown accuracy — investigation verdict + WandBars 0.3.13 build record

Mike's directive, Nov 8, 2026 (~1:10 PM CST): "We should develop a new
system so cooldowns are accurate." Specimen: the Nov 7 Wither fight —
Magic's on-screen cooldown reminder disagreed with the number on the
hotbar icon; "heal never shows the cooldown accurately."

Desk work only: investigated, built, rig-tested. Nothing installed.

---

## 1. MECHANISM VERDICT — convicted

**Primary mechanism (hypothesis b, convicted): cooldowns live in
per-key stores, a cast writes only the BASE key's store, and the
display path resolves rank variants whose stores stay empty.**

Magic 11.2.5, bytecode-verified (`Mage.updateHotbarStatus`,
`BaseSpell.getTimeToCast/getRemainingCooldown/cast`,
`Mage.handleCooldown`, disassembly in the investigation scratch):

- The icon number is `ceil(getTimeToCast()/1000)` clamped [1, 99],
  written per hotbar slot for `mage.getSpell(icon key)`.
- The cast-failure REMINDER is built from
  `getRemainingCooldown()` of the spell being cast.
- A spell's cooldown state is per spell KEY (`SpellData` per key).

Rig proof (ProbeCd, Moon's lifecycle fixture + Magic 11.2.5 + synthetic
mage, beginner path 60 mana / 5 regen, fixture heal template cooldown
7,000 ms; reproduced identically across five runs — phase A stock
0.3.12 and phase B candidate; evidence in `investigation/evidence/`):

After one real Heal cast, sampled every 10 ticks:

| sample | base key `heal` remaining | variant-resolved (`heal\|2`) remaining | time-to-cast (variant) |
|---|---|---|---|
| H0 | **7,638 ms** | 638 ms | 638 |
| H1 | 7,162 | 162 | 162 |
| H2 | 6,662 | **0** | 0 |
| H8 | 3,626 | 0 | 0 |
| H14 | 461 | 0 | 0 |
| H15 | 0 | 0 | 0 |

- The base key's record (7,000 template + ~1,000 `cooldown_mage`) is
  the one that GATES: a recast attempt at H8 **failed** while it read
  3,626 ms. The reminder reports this record.
- The variant records (`heal|2`, `heal|3`) received **nothing** from
  the cast — they show only the mage-global remainder (~0.6 s), then 0
  for the remaining ~6 s of the real cooldown. Any display lookup that
  resolves to a variant — `wand.getSpell("heal", mage)` does, and it is
  the lookup WandBars' own 0.3.10 tag used — reads "ready" while the
  spell is still gated. That is the specimen: the number said ready,
  the reminder (correctly) said wait.
- Symmetric control: a 240 s cooldown SET on the variant read back
  identically through every lookup (239,999 → 238,947 ms) — the stores
  are per-key, and agreement happens only when the cooldown lives on
  the key being read.

**Secondary mechanism (hypothesis a, convicted): mana-wait conflation.**
`getTimeToCast()` is the MAX of (remaining cooldown, charge recharge,
MANA WAIT). Rig: Flash Heal (template cooldown **0**) at 0 mana showed
time-to-cast **3,800 ms → 3,301 ms** purely from mana refill (cost 19
at 5/s regen), cooldown 0 throughout. In the Wither fight's
mana-starved shape, the icon number for a spell whose cooldown is long
gone counts down the mana refill — "accurate" about the wrong
quantity, indistinguishable from a cooldown.

**Also real, from the bytecode (not separately rig-isolated):**
`getTimeToCast()` returns null when the spell cannot cast at all right
now (e.g. an active spell, or a cost above the mana pool) and Magic
then writes amount **1** for the whole period; Geyser flattens Bedrock
icon numbers at **64** (Nov 3 bytecode finding) and Magic clamps Java
at 99 — so long cooldowns (Familiar/Cat, 240 s) can never be shown by
the number on either platform. Hypothesis (c), refresh cadence, is
exonerated as a primary cause: Magic refreshes per tick; the wrongness
is in the VALUE, not its freshness. Hypothesis (d) holds only as the
64-cap above.

**Heal specifically:** base cooldown 8,000 ms live (7,000 fixture) +
1,000 `cooldown_mage`; an active 10 s HoT (`total_duration 10000`,
`repeat 10`) — but the inaccuracy is the variant-store divergence:
every rank of Heal shares the shape, so at every stage the display
lookup lands on a variant whose store the cast never wrote.

## 2. THE FIX — ExcelWandBars 0.3.13 (on the 0.3.12 tree)

WandBars owns the bars and the name-tag channel (Mike's Nov 3 pick),
so the fix lands there; Magic itself is upstream and unpatched.

- **New pure plan `core/CooldownDisplayPlan`** (no Bukkit/Magic types,
  unit-tested): given the spell's true remaining cooldown (ms), the
  time-to-cast Magic's number is computed from, and the platform cap
  (99 Java / 64 Bedrock), decide whether the name tag shows — and that
  the tag's value is **the cooldown specifically**.
- **`MagicHook.baseCooldownMs(player, baseKey)`**: reads the cooldown
  from the base key's own `MageSpell.getRemainingCooldown()` — the
  same record the cast path gates on and the reminder reports. The
  variant-resolving lookup is kept ONLY for the time-to-cast
  comparison, never as a displayed value.
- **`NameTagHook`** (the proven 0.3.10 annotate/refresh/restore/sweep
  machinery, unchanged): in accuracy mode it tags exactly when the
  icon number cannot be trusted to show the cooldown — the number is
  computed from a different binding wait (mana/charge/null), or the
  cooldown exceeds the platform cap — and restores the clean name the
  moment the number becomes truthful or the cooldown ends. A mana
  wait alone never produces a tag: the tag names cooldown, and mana
  has its own bar.
- **Config**: `cooldown-name-tag.accuracy: true` (new, default on).
  `accuracy: false` reproduces the 0.3.10 rule exactly (legacy mode is
  a config flip, not a rebuild). No per-spell lists; fully generic.
  No boss-bar use. The >60 s behavior players know is preserved as a
  superset: long cooldowns still count down in the name — now with
  the true cooldown value on both platforms.

### Bedrock impossibility + fallback (explicitly requested)

On Bedrock the icon NUMBER can never show a cooldown above 64
(Geyser's flattening), and below that it still shows Magic's
time-to-cast, not the cooldown. An accurate number *on the icon* is
therefore impossible on Bedrock whenever the cooldown is long or
another wait binds. The shipped fallback is the name tag itself —
"Flash Heal · 0:07" in text, which Geyser passes through unflattened —
carrying the true cooldown. On Java the same applies above 99. When
the number IS truthful (short cooldown, no other wait binding), no
tag is added and the number does the talking, exactly as today.

## 3. PROOFS

- **Unit tests: 19 suites, 507 checks, 0 failed** — the 18 carried
  0.3.12 suites (487 checks, all green against the 0.3.13 tree) plus
  new `CooldownDisplayPlanTest` (20 checks): Heal-active/null-ttc,
  mana-wait conflation (Wither shape), Familiar 240 s on both caps,
  the truthful middle (no tag), cooldown-expired-with-mana-wait (tag
  clears), unreadable cooldown (never guess), legacy mode = 0.3.10.
- **Rig (desk, fixture):** five ProbeCd runs. Phase A (stock 0.3.12)
  and phase B (candidate) reproduce the conviction table above; the
  candidate boots clean on Paper 26.3-235 — `Enabling ExcelWandBars
  v0.3.13`, build line `EXC-WANDBARS-0.3.13-WRV-20261108-01-41cd07`,
  SEVERE 0 in every phase-B run.
- **Rig gap, stated plainly:** the synthetic player's wand inventory
  never opened in this fixture (join event, interact event,
  `openInventory()`, sneak flip, and probe0312's exact inventory
  layout all tried across four runs), so the tag was not photographed
  on a live icon in the desk rig. What that leaves unproven is only
  the last inch — the render — and the render machinery is the
  shipped, player-verified 0.3.10 code path; this build changes its
  decision inputs (unit-proven) and its value source (the exact
  series the rig tracked). The deciding evidence is the post-install
  player proof in PACKET-DRAFT.md.

## 4. ARTIFACT

- `ExcelWandBars-0.3.13.jar` — 544,853 B, sha256
  `7278f09ed160236b54c02d9dfaa590feeb1ba25027c901b3a17217848a1327e3`,
  build id `EXC-WANDBARS-0.3.13-WRV-20261108-01-41cd07`.
- Built twice from the same sources with one differing pair of
  entries (the two new plan classes, line-number metadata from a
  comment edit — same sizes, same entry set); the staged jar is the
  final-sources build. Entry delta vs the live 0.3.11-content jar =
  the full 0.3.12 delta + NameTagHook/MagicHook changes + the two new
  plan classes + config/plugin.yml. Sources in `work/src`, tests in
  `tests/`, investigation (probe, prep script, evidence) in
  `investigation/`. See SOURCE.md.
