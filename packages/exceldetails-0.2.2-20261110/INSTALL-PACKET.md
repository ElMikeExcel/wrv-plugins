# ExcelDetails 0.2.2 — INSTALL PACKET (gated; for the install lane)

Jar-swap upgrade over LIVE ExcelDetails 0.2.1. Commission: Mike,
Nov 9, 2026 — boss segments scale by BossTrial's display-scale
so meter numbers match the boss pop-ups, and the meter is
prepared for the EliteMobs world (EM simulated numbers never
render as meaningless millions). Design:
`~/workspace/details-meter-20261109/build-0.2.2/DESIGN.md`.
Build record: `.../build-0.2.2/BUILD-RECORD.md`.

## Artifacts

- **ExcelDetails-0.2.2.jar** — 50,024 B,
  sha256 `7d02bcf9c5a2de501e7ac0d1d86cb44a01f9b96e598f5a1fd17f5f8cf5c34b4b`,
  build `EXC-DETAILS-0.2.2-WRV-20261110-01-b22e10`
  (at `~/workspace/details-meter-20261109/build-0.2.2/ExcelDetails/ExcelDetails-0.2.2.jar`;
  repro jar byte-identical alongside it; a second full build run
  reproduced the same sha256).

## Gates (fresh panel downloads before swap, STOP on any mismatch)

- **Live-jar gate:** `plugins/ExcelDetails-0.2.0.jar` (the live
  filename for this plugin; plugin identity comes from
  plugin.yml) must read **41,116 B, sha256
  `7c5d56f3a7e70f147b14101b15d224144ae732735e7b5159efb5aba9d074251c`**
  (build EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a). Any other
  value = STOP, the live jar is not the 0.2.1 this packet was
  built against.
- Live `plugins/ExcelDetails/data.yml` STAYS — format unchanged
  in 0.2.2 (the EM bucket is session-scoped and never persists);
  no gate hash (it mutates with play), backed up in the swap.
- Live `plugins/ExcelDetails/config.yml`: 0.2.2's bundled config
  adds two keys (`boss-display-scale: true`,
  `elite-bucket: true`). Both have CODE DEFAULTS equal to the
  shipped values, so the live config may STAY unmerged and
  behavior is identical; merging the bundled config (or adding
  the two keys) is optional operator surface, not a gate.
- Standard hand-window discipline: WINDOW-IN-PROGRESS marker,
  **BACKUP FIRST with cap-at-6** (name prunees; never delete the
  Daily Backup's newest copy or the two newest manual restore
  points), **emptiness gate** (list = 0 twice) for the restart.
- No other plugin, config, or data file rides this packet.
  ExcelBossTrial and EliteMobs are READ by the new seams at
  runtime; neither is modified, restarted separately, or gated
  here beyond being present at boot.

## Swap

- Rename the live jar (gate-verified above) →
  `ExcelDetails-0.2.0.jar.bak-0.2.1`; place the 0.2.2 candidate
  UNDER THE SAME LIVE FILENAME `ExcelDetails-0.2.0.jar`. Verify
  the placed file's panel checksum reads `7d02bcf9…` under that
  name before boot. In-place backups before the boot:
  `config.yml.bak-0.2.2`, `data.yml.bak-0.2.2` (copies; the
  files themselves stay live). ONE restart.
- **Rollback:** remove the placed jar, restore
  `ExcelDetails-0.2.0.jar.bak-0.2.1` to
  `ExcelDetails-0.2.0.jar`, restart. Jar-only rollback is safe:
  data.yml format is identical in both versions and 0.2.2
  writes nothing new to disk.

## Boot verify

- `ExcelDetails READY version=0.2.2 build=EXC-DETAILS-0.2.2-WRV-20261110-01-b22e10 refresh=10t timeout=8s rows=6`
- `ExcelDetails: PlaceholderAPI expansion registered (%details_show_damage%, %details_show_taken%, %details_show_healing%).`
  (absence = the Details Settings menu bridge is down;
  investigate before player proofs.)
- The 0.2.1 party seams still log BOUND on first render
  (EliteMobs party seam + ExcelTogether party seam), unchanged.
- New seams log lazily, once, on first use:
  - On the first BOSS hit (intake lookup):
    `ExcelDetails: BossTrial intake seam BOUND (cfg.intakeEntityCap + encounters + EncounterLedger.maxHealth) — boss numbers recover the live intake factor.`
    An UNBOUND line instead = over-cap bosses (the wither) read
    low by the intake factor until a desk pass fixes the seam;
    sub-cap chapters (elders, dragon) stay exact. Do NOT call
    the boss-scale proof complete on an UNBOUND boot.
  - On the first damage event (elite check):
    `ExcelDetails: EliteMobs seam BOUND (EntityTracker.isEliteMob) — elite damage routes to the EM bucket.`
    An UNBOUND line instead = bucketing relies on the
    simulated-amount floor only; note it for a desk pass.
- Guild LOCKED count unchanged; MagicGate stage line unchanged;
  SEVERE 0 attributable.

## Player proofs (post-install)

1. **Boss-scale match, sub-cap (exact):** fight an elder
   guardian (or the dragon). Compare the meter's Damage Done
   against BossTrial's action-bar `{last}` / running total and
   the post-fight stats pop-up for the same hits: the numbers
   MATCH (e.g. a hit the pop-up shows as 7,000 reads 7k in the
   meter). Before 0.2.2 the meter showed the raw ~7.
2. **Boss-scale match, over-cap (the wither):** summon the
   wither with 2+ champions. The meter's per-player damage
   totals track the pop-up's pool-space totals (hits sum
   toward pool × 1000 on the boss-bar title), NOT the ~0.2-sized
   raw figures and NOT 1000× the bar's HP loss. This proof is
   the one that convicts the intake seam: if the seam logged
   UNBOUND, expect meter ≈ pop-up × ~0.047 and stop — desk pass.
3. **Taken/healing stay true HP (same fights):** Damage Taken
   rows read single/double-digit HP (a wither pulse reads ~2),
   healing reads real HP — never ×1000 figures.
4. **EM bucket (needs an elite):** hit an EliteMobs elite (a
   sprinkle add beside a seal boss, or any natural elite). The
   sidebar's ranked rows do NOT gain the elite damage;
   `/details status` shows the "EliteMobs (tracked separately)"
   line with the bucketed amount. Damage TAKEN from the elite
   still appears in Damage Taken as real HP.
5. **0.2.1 scoping regression (fast):** two players fighting
   apart, not partied — each sees only their own rows; partied
   — party rows with within-group shares.
6. **Mix + piggyback regression (fast):** /details show taken +
   healing stack as before; in an EM party the combined box
   stays ≤ 15 lines.

## Same-pass surfaces owed at install

- Guide knowledge: the exceldetails-meter chunk gains the
  boss-scale + EM-bucket sentences (draft in BUILD-RECORD /
  supervisor's surfaces pass).
- What's New BOTH surfaces (website + ExcelWhatsNew changelog)
  — registry-first, never hand-edited.
- Website plugin-table line: version 0.2.2.
- Rotation tips check (a "boss damage in the meter now matches
  the boss pop-ups" tip is a candidate — parent's call);
  ExcelMenu/BedrockMenu check (no menu change in this build;
  placeholders unchanged; the Details Settings section stands).
- FEEDBACK-TRACKER: add the boss-scale proofs (1–2) and the EM
  bucket proof (4) as testing rows per Mike's rule — testing
  always goes on the list.
