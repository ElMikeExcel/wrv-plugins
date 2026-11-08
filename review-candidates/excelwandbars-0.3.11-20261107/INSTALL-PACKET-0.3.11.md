# ExcelWandBars 0.3.11 — Install Packet (compass-on-wand-bar fix)

**Build:** `EXC-WANDBARS-0.3.11-WRV-20261107-01-08465c`
**Candidate:** `~/workspace/wandbars-ux-build/build-0.3.11/ExcelWandBars-0.3.11.jar` — **533,344 B, SHA-256 `08465c1403d080915fd13a2173ad5e2001f0a1d208b60484be1ed2aac200cd30`**, 237 entries, built twice byte-identical.
**Record:** `~/workspace/wandbars-ux-build/build-0.3.11/BUILD-RECORD.md` (design, ordering evidence, tests, rig).

## What it does (exactly two changes, nothing else)

1. **Quit close (B):** a player who logs out with the spell bar open has the bar closed as they leave — from Paper's connection-close event, which fires BEFORE `PlayerQuitEvent` and therefore before Magic's own quit handling reads the wand's open state for its next-join restore decision (Magic enables first and at its default `logout_delay: 0` deactivates the mage inside its quit handler, so no `PlayerQuitEvent` handler can reach the wand in time — see BUILD-RECORD). Belt: the same close runs at the top of WandBars' quit handler for `logout_delay > 0` setups. Same `closeInventory()` call `stop()` uses; policy switch is the existing `join-starts-on-items` (no new config key).
2. **Join sweep (A-lite):** when the join guard's FINAL check (160 ticks) leaves a bar open, the existing ExitSweep runs once for that join — compasses (or any foreign item) already baked into saved wand-bar pages by pre-0.3.11 logouts are moved into the player's real (stored) inventory, never deleted. Rides the existing `exit-sweep` switch.

ExcelMenuJoin is NOT modified. The occupied-slot swap path is NOT touched (different defect layer, tracked separately).

## Window content — JAR ONLY

- **Gate (STOP on any mismatch):** live `plugins/ExcelWandBars-0.3.4.jar` must be the 0.3.10 candidate — **529,841 B, SHA-256 `0afbf993282df6447533e3b3b753eca3c96fb0a4dcf741078b7c7b90973a8461`**.
- **BACKUP FIRST** (fresh Shockbyte backup, cap-at-6 prune of the oldest manual per house rule, prunees named in the install report) + verified-empty / weekend protocol as standing.
- Swap: candidate jar → live filename `plugins/ExcelWandBars-0.3.4.jar`; keep an in-place backup of the outgoing jar as `ExcelWandBars-0.3.4.jar.bak-20261107-pre0311`.
- **NO config step:** 0.3.11 adds no config keys; shipped `config.yml` is byte-identical to 0.3.10's (`946f2e8b…`). Live config untouched.
- Marker on landing: `~/workspace/wandbars-ux-build/build-0.3.11/INSTALLED.txt`.

## Boot verification

- Enable line: `Enabling ExcelWandBars v0.3.11`; no "Quit close hook off" warning.
- Plugin count 76 unchanged; guild LOCKED 3 of 5 unchanged; TPS ~20; SEVERE 0 attributable.
- `/excelwandbars` status counters include the new segments `join-sweeps …` and `quit-closes …` (both 0 on a fresh boot).

## Post-install proofs (player-side, hard)

1. **Mike (Bedrock):** open the wand bar, log out with it open, log back in → spawns on his **normal hotbar** (not the wand bar); exactly **one** compass in his inventory; open the wand bar → **no compass in any bar slot**. (Pre-fix this repro produced the duplicate compass on the wand bar — his Nov 7, 5:18 PM report.)
2. **Marvin (Java parity spot-check):** same logout-with-bar-open → relog → normal hotbar, one compass.
3. Anyone with a pre-0.3.11 baked compass: on their first join + bar restore, the sweep moves it to their real inventory (watch for `Exit sweep: moved …` / `join-sweeps 1` in counters).

## Rollback

Restore `ExcelWandBars-0.3.4.jar.bak-20261107-pre0311` over the live filename and restart. No config or data rollback exists (no config change; the build writes no new data files — its only state is in-memory counters/sets).

## Surfaces / records (parent legs, not the install window)

- Records package staged at `~/workspace/records-catchup-20261103/packages/excelwandbars-0.3.11-20261107/` with PUSH-NOTES (Drive deposit + draft PR are parent/board-manager legs).
- Player-facing change → same-pass surfaces owed at install close-out: What's New note (both surfaces) + Guide knowledge line for the compass fix; website canon if the WandBars page describes the join behavior. Tips check: no tip change needed.
