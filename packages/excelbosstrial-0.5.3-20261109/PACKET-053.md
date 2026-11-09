# PACKET — ExcelBossTrial 0.5.3, Item 39 WARDEN RETUNE (R2 + R1)

**For a PARENT hand window.** This packet files the Item 39
Warden retune candidate — test-proven in the retune rounds
(`~/workspace/warden-retune-20261109/TEST-ROUNDS.md`, rounds
1–2) and approved for filing as-is by Mike (Nov 9, 2026) with
the two flags in §Flags carried verbatim — composed onto the
shipped ExcelBossTrial 0.5.2 line. **Jar swap + config MERGE
(warden keys only) in the same window.** Nothing else in this
window: no pack changes, no server.properties changes, no
other plugin.

Candidate: `~/workspace/warden-retune-20261109/build-053/ExcelBossTrial-0.5.3.jar`
— **440,242 B / sha256 09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8**,
deterministic double-build verified byte-identical. Build ID:
**EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b** (fresh tag;
0.5.2's 13447a and 0.5.1's a3aa16 never reused). Build record:
`~/workspace/warden-retune-20261109/build-053/BUILD-RECORD.md`
(base proof byte-identical to the shipped 0.5.2 jar before any
edit; full suite 0 failures; SonicBoomRuleTest 24/24; rig
Step 3 re-run against the shipped jar PASSED — boom 15.0000
flat at minute 0 / mid-ramp / past cap, capped melee exactly
67.5000, SEVERE 0; see BUILD-RECORD.md).

## What changes for players

- The Warden's attrition pressure settles at its natural
  strength: outgoing multiplier 1.25 → **1.0**, ramp cap
  2.0 → **1.5**. Past-cap melee reads base 67.5 — about
  **17.98 HP** through Protection IV netherite, ≈13.1 mid-ramp,
  ≈9.3 at minute 0. Hard hits, no one-shots.
- The Warden's **sonic boom no longer rides the pressure
  ramp**: it reads its exact vanilla **15.0 HP at minute 0,
  mid-ramp, and past the cap** (under R2 alone it read 22.5
  past the cap — a standing one-shot; that is the defect this
  filing closes). Melee keeps scaling; only the boom is exempt.
- Everything else is 0.5.2 exactly: boss voice, dragon crystal
  re-light + phase-2 sprinkle, elder relay, ×1000 display
  scale, all other chapters' tuning.

## Flags (carried verbatim from the test record — read before the window)

1. **Step 4 was MIXED (flagged).** Heal throughput beats
   intake 1.19× (406.98 HP in vs 485.0 HP healed over 180 s at
   cap cadence) and the mana sub-gate passed (longest window
   under 10 usable mana = 1.002 s vs the 10 s gate), but the
   literal above-50% sustain sub-gate was NOT MET under a
   scripted worst-case ordering (a capped melee and a boom
   landing 1 s apart on the same player — an ordering the live
   fight does not naturally produce): 25.0 s below 50% HP and
   4 WOULD-HAVE-DIED clamp events in the sim; recovery from a
   post-cap hit took 3 casts / 4.0 s against a ≤2-cycle line.
   Mike approved filing with this flag noted.
2. **Diamond watch item.** At ramp cap, a Protection IV
   **diamond** wearer takes 67.5 × 0.3024 ≈ **20.4 HP** from
   one capped melee — marginally over the 20 HP line. The
   spec gate is written on the tank (netherite) bracket, which
   passes at ≈17.98; the diamond figure is a melee-axis fact
   R1 does not touch. Flagged, not retuned.

Also on the record: all test rounds ran on the **BRACKET
FALLBACK** gear basis (tank = Prot IV netherite, others =
Prot IV diamond) — the step-1 gear census (the family's actual
gear, parent panel pull) has still not landed. Step 2 PASSED
(kill 695 s = 11 min 35 s at pool 21,600, inside the 8–16 min
band); Step 3 PASSED on the final candidate (boom 15.0000 flat
at minute 0 / mid / past cap; melee ≈9.3 / ≈13.1 / 17.98
post-mitigation on the tank — all < 20 HP, beacons absent).

## Gate (STOP on any mismatch)

1. **Marker discipline:** create
   `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at
   window start; delete at finish. Before opening, confirm no
   other window is in flight.
2. **Live jar identity:** the panel file
   `plugins/ExcelBossTrial-0.4.1.jar` (the 0.5.2 jar lives on
   the server under this filename — canon correction from the
   0.5.2 window) must hash to the installed **0.5.2: 438,791 B
   / sha256 1a866a54840dc510ad78cdbd384b75d49f82a25808a74724a0
   b4fc1e66f0944b**. Any other hash = STOP and re-reconcile.
3. **Live config identity:** pull `plugins/ExcelBossTrial/
   config.yml` — the post-0.5.2 merged config must hash to
   **23,149 B / sha256 33f56982d82edbafa4a35548063440168d030e
   3b479d88f5e98a55ab918c73cd**. Any other hash = STOP (the
   live file moved since the 0.5.2 window; re-verify the
   warden block's current values before merging). GATE-VERIFY
   inside it: `warden.pressure.outgoing-multiplier` reads
   **1.25**, `warden.pressure.ramp-max` reads **2.0**, and NO
   `apply-to-sonic-boom` key exists anywhere yet.
4. **BACKUP FIRST + CAP-AT-6:** fresh Shockbyte backup before
   the swap ("BossTrial 0.5.3 pre-install <date>"); prune the
   OLDEST manual backups first so the post-creation total is
   ≤ 6; never delete the Daily Backup's newest copy or the
   two newest manual restore points; name prunees in the
   install report. (Shockbyte's backup error banner can lie —
   re-read the backup list to confirm creation.)
5. **Emptiness gate:** install only on a verified-empty
   server (`list` = 0 twice) or under the weekend /
   Mike-directed push protocol.

## The swap

- Rename the live jar aside in place:
  `ExcelBossTrial-0.4.1.jar` → `ExcelBossTrial-0.4.1.jar.bak-0.4.1`
  (kept in the same folder until the window closes clean),
  then place the candidate AS `ExcelBossTrial-0.4.1.jar`
  (same-name swap — the live filename does not change).
  Panel-verify the placed file: **440,242 B / 09c5eb0e…**.
- **Config merge — MERGE, NEVER REPLACE.** Keep a pre-merge
  copy as `config.yml.bak-053` in the plugin folder. Touch
  ONLY these keys, all inside the `warden.pressure` block of
  the live config:
  - `outgoing-multiplier: 1.25` → **`outgoing-multiplier: 1.0`**
  - `ramp-max: 2.0` → **`ramp-max: 1.5`**
  - ADD **`apply-to-sonic-boom: false`** (new key, same block;
    place it after `pulse-respect-training-wheels`).
  Every other key — all of Packet A's merges, the whole 0.5.2
  merge (voice keys, flip-crystals, display-scale, relay),
  and all other chapters — stays byte-for-byte. Read the
  merged file back and byte-verify: the post-merge file must
  differ from the gate file by exactly these three lines.
  (Belt-and-braces: even if the new key were missed, the
  0.5.3 code fallback is false — the exemption would still be
  live. The merge is still required: the filed config must
  state the behavior.)
- One restart for the whole window.

## Boot verification (console, in order)

- `Enabling ExcelBossTrial v0.5.3`
- `Build: EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b`
- Damage feedback line contains `display scale x1000.0
  (feedback.display-scale)` (0.5.2 surface intact).
- Elite sprinkle line contains `ender_dragon ARMED (cadence
  180s, cap 40) (phase 2 only)`; wither/elder/warden arms
  unchanged from the 0.5.2 boot.
- Boss voice line unchanged: all four chapters ARMED
  (10 / 7 / 7 / 8 clips).
- NO clamp warning attributable to the warden (no warden
  clamp exists; any `max-per-fight … clamped` line stays the
  pre-existing wither-only one).
- `SEVERE` count 0 attributable to ExcelBossTrial; GuildGate
  still **LOCKED 4 of 5**; plugin count unchanged (79).
- Post-boot read-back: `plugins/ExcelBossTrial/config.yml`
  carries the three merged warden values and nothing else
  changed (diff vs the gate file = the three lines above).

## Post-install proofs (player-proof, scheduled with the family)

- **Warden attempt (the family's next, incl. the possible Sat
  Nov 14 group attempt):** boom reads 15 HP flat early AND
  late in the fight (never the old 22.5 one-shot); capped
  melee ≈18 HP on the Prot IV netherite tank; kill lands in
  the 8–16 min band at the family's real DPS; healer sustains
  through the cap cadence (the step-4 live read — watch the
  melee→boom ordering on the tank).
- Warden voice seams still fire (open / boom / beats /
  verdict) — 0.5.2 regression spot-check.
- No other chapter changes behavior: any seal re-fight reads
  exactly as its 0.5.2 acceptance.

## Rollback

Rename `ExcelBossTrial-0.4.1.jar.bak-0.4.1` back over
`ExcelBossTrial-0.4.1.jar`, restore `config.yml.bak-053` over
the config, restart, confirm the 0.5.2 boot lines
(`Enabling ExcelBossTrial v0.5.2`, Build …-13447a). The fresh
backup from the gate is the second net. (The 0.5.2 jar ignores
the `apply-to-sonic-boom` key entirely, and the restored
config removes it anyway — rollback is clean both ways.)

## Same-pass surfaces owed at install close-out

What's New BOTH surfaces (website section AND in-game
ExcelWhatsNew changelog.yml, from the registry), Guide
knowledge/intents for the warden pressure change if any chunk
names the old 1.25/2.0/22.5 numbers, website pair if any page
names them, tips check, daily log, Route Sheet row, Big Brain
local + Drive, and the records publication for this build
(package under `~/workspace/records-catchup-20261103/packages/`,
jar NOT included, per house rule — records lane).

## Explicitly NOT in this packet

- The step-1 gear census (parent panel pull; the filing is
  bracket-proven and does not wait for it).
- Any retune beyond R2+R1 — the step-4 flags and the diamond
  watch item are filed AS flags, not fixed here.
- ExcelDetails data, the combined panel pull, Paper 245 /
  Geyser b1298 — other windows, other packets.
