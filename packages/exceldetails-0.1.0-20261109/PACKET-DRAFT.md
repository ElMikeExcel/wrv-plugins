# ExcelDetails 0.1.0 — PACKET DRAFT (for the parent to file)

NEW plugin install (nothing live to hash-gate against). Commission:
Mike Reyes, Nov 8, 2026, ~11:22 PM CST (backlog Item 40 — faithful
Details! copy first, LIVE updating core, all combat from v1,
auto segments + manual clear, per-player toggle default ON, Warden
fight tracked incl. Damage Taken). Build record:
`~/workspace/details-meter-20261109/build-0.1.0/BUILD-RECORD.md`.

## Artifacts

- **ExcelDetails-0.1.0.jar** — 28,362 B,
  sha256 `f73e97600ec98863600b2d0b57ce6af4b1cbed943289071343714cae84fa310b`,
  build `EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c`
  (at `~/workspace/details-meter-20261109/build-0.1.0/ExcelDetails/ExcelDetails-0.1.0.jar`;
  repro jar byte-identical alongside it).
- **config.yml** — the jar's bundled default (refresh 10t, segment
  timeout 8s, max-rows 6, announce on) drops in as
  `plugins/ExcelDetails/config.yml` on first boot; no hand-placed
  config needed.

## Gates

- Fresh panel checksum of the uploaded jar must match the sha256
  above before the boot (mismatch = STOP).
- Standard hand-window discipline: WINDOW-IN-PROGRESS marker,
  BACKUP FIRST with cap-at-6 (name prunees), emptiness gate.
- No other plugin, config, or spells file rides this packet.

## Swap

- **Jar only, additive.** Place `plugins/ExcelDetails-0.1.0.jar`;
  nothing is replaced or removed. One boot.

## Boot verify

- `ExcelDetails READY version=0.1.0 build=EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c refresh=10t timeout=8s rows=6`
- Guild LOCKED count unchanged; MagicGate stage line unchanged;
  SEVERE 0 attributable.

## Player proofs (post-install)

1. **Standalone live (any player, Java or Bedrock):** punch a mob —
  the Details! sidebar appears within a second and the Damage Done
  number climbs per hit, live. Stop for 8+ seconds: segment closes.
2. **Damage Taken window:** `/details window taken`, take a mob hit
  (a Warden hit when the family is ready) — the row fills with the
  source's damage.
3. **EM piggyback (a player in an EliteMobs party):** the party box
  stays on top; meter lines render directly underneath inside the
  same box; the party lines never flicker or lose a row. Leave the
  party: the meter stands alone again.
4. **Toggle:** `/details off` — every meter line gone, party box
  (if any) untouched. `/details on` restores it. Relog keeps the
  choice.
5. **Clear:** mid-fight `/details clear` — numbers zero and the
  segment clock restarts on the next hit.

## Same-pass surfaces owed at filing

- Guide knowledge + intent for /details; website plugin list line;
  What's New BOTH surfaces (website + ExcelWhatsNew changelog);
  rotation tips check; ExcelMenu/ExcelBedrockMenu check (no menu
  surface in 0.1.0 — commands only; name it as checked-none).
