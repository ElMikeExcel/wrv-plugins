# ExcelDowned 0.1.1 — PACKET NOTES (for the parent to file)

Jar-only upgrade of the LIVE ExcelDowned 0.1.0: /giveup gains the
button confirm (Java menu / Bedrock form). Commission: Mike Reyes,
Nov 8, 2026, ~10:14 PM CST ("I want buttons."). Build record:
`~/workspace/downed-revive/BUILD-RECORD-0.1.1.md`.

## Artifacts

- **ExcelDowned-0.1.1.jar** — 47,199 B,
  sha256 `331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f`,
  build `EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d`
  (at `~/workspace/downed-revive/build/ExcelDowned/ExcelDowned-0.1.1.jar`;
  repro jar byte-identical alongside it).

## Gates

- Live `plugins/ExcelDowned-0.1.0.jar` must hash to
  `0347d1a09fa2023d8a0bb405076af6279adb7b5a282be604025f8423bda82f3a`
  before the swap (fresh panel checksum; mismatch = STOP).
- No other plugin or file changes ride this packet: no spells delta
  (the Resurrection packet is untouched), no Guide/website surfaces
  of its own (a What's New line for "give-up is now buttons" is the
  parent's call at filing).

## Swap

- **Jar only.** Replace `plugins/ExcelDowned-0.1.0.jar` with
  `ExcelDowned-0.1.1.jar` in the same stopped window discipline as any
  jar swap (backup first, emptiness gate, one boot).
- **Config unchanged:** 0.1.1 ships a byte-identical `config.yml`
  (no new keys; the 10 s confirm window is the existing
  `giveup-window-seconds`). Do not overwrite the live config.
- plugin.yml softdepend gains `Geyser-Spigot, floodgate` (load order
  only — the form route is reflection and fail-soft without them).

## Boot verify

- `ExcelDowned: custom Magic action 'Resurrect' registered.`
- Ready line names **0.1.1** and build
  **EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d**, bleed-out 300s,
  downed factor 0.25; sessions restored count sane.
- Guild LOCKED count unchanged; MagicGate stage line unchanged;
  SEVERE 0 attributable.

## Player proofs (post-install)

1. **Java menu (volunteer, Java client):** take a lethal mob hit in a
   controlled test → DOWN; `/giveup` → the confirm menu opens
   (red "✝ LET GO — die now" / green "Keep fighting"). "Keep
   fighting" → still downed, timer running. `/giveup` again, then
   "Let go" → exactly one real death with drops.
2. **Bedrock form (a Bedrock player — e.g. Kailey):** same flow → the
   form appears with "✝ Let go" / "Keep fighting"; each button does
   what it says; dismissing the form cancels (still downed).
3. **Stale menu after revive:** volunteer downs, opens the menu; a
   party member casts Resurrection → volunteer back at 50%, menu
   gone; if any confirm surface lingers, choosing "Let go" does
   NOTHING (no death).
4. **Typed regression:** `/giveup` twice within 10 s (menu ignored /
   chat path) still dies exactly once; `/giveup` while not downed
   still answers "You are not downed."
