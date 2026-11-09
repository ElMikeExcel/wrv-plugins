# ExcelDetails 0.2.0 — PACKET DRAFT (for the parent to run)

Jar-swap upgrade over LIVE ExcelDetails 0.1.0. Commission: Mike's
checkbox-mix ruling, Nov 9, 2026 (MENU-PACKET-DETAILS-SETTINGS.md §0).
Build record: `~/workspace/details-meter-20261109/build-0.2.0/BUILD-RECORD.md`.
Design: `~/workspace/details-meter-20261109/build-0.2.0/DESIGN.md`.

## Artifacts

- **ExcelDetails-0.2.0.jar** — 37,130 B,
  sha256 `f216f81eefd8d09cc6abfc14efbd2b884803f6c3717d9d9946766b836cd538cb`,
  build `EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19`
  (at `~/workspace/details-meter-20261109/build-0.2.0/ExcelDetails/ExcelDetails-0.2.0.jar`;
  repro jar byte-identical alongside it; a second full build run
  reproduced the same sha256).

## Gates (fresh panel downloads before swap, STOP on any mismatch)

- Live jar: `plugins/ExcelDetails-0.1.0.jar` = 28,362 B,
  sha256 `f73e97600ec98863600b2d0b57ce6af4b1cbed943289071343714cae84fa310b`
  (build EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c, installed Nov 9,
  2026, 3:04 AM).
- Live `plugins/ExcelDetails/config.yml` = 871 B (the 0.1.0 bundled
  default, created on first boot) with values refresh-ticks 10,
  combat-timeout-seconds 8, max-rows 6, announce-segments true. The
  0.2.0 bundled config carries IDENTICAL keys/values (only comments
  + the build stamp differ) — the live file stays in place.
- Live `plugins/ExcelDetails/data.yml` = build stamp + per-player
  prefs (enabled/window). No gate hash (it mutates with play); it is
  backed up in the swap and 0.2.0 migrates it on load (a legacy
  window key becomes shown = {that window}).
- Standard hand-window discipline: WINDOW-IN-PROGRESS marker,
  BACKUP FIRST with cap-at-6 (name prunees), emptiness gate.
- No other plugin, config, or spells file rides this packet. The
  Details Settings MENU packet (server_menu slot 39 +
  menus/details_settings.yml) places in this same window AFTER the
  jar boot-verifies, or in its own window right after — its
  placeholders do not exist until 0.2.0 is live.

## Swap

- Rename `ExcelDetails-0.1.0.jar` → `ExcelDetails-0.1.0.jar.bak-0.1.0`;
  place `ExcelDetails-0.2.0.jar`. In-place copies before the boot:
  `config.yml.bak-0.1.0`, `data.yml.bak-0.1.0`. One boot.
- Rollback: remove the 0.2.0 jar, restore the .bak-0.1.0 jar (and
  data.yml.bak-0.1.0 if the migration misbehaves — 0.1.0 ignores
  0.2.0's show-*/overall keys, so a jar-only rollback is safe).

## Boot verify

- `ExcelDetails READY version=0.2.0 build=EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19 refresh=10t timeout=8s rows=6`
- `ExcelDetails: PlaceholderAPI expansion registered (%details_show_damage%, %details_show_taken%, %details_show_healing%).`
  (PlaceholderAPI 2.12.3 is live; absence of this line = investigate
  before player proofs, the menu packet depends on it.)
- Guild LOCKED count unchanged; MagicGate stage line unchanged;
  SEVERE 0 attributable.

## Player proofs (post-install)

1. **Default view:** punch a mob — the box auto-opens with the
   Damage Done section only (0.1.0 look). Stop 8+ s: segment closes;
   the box disappears instead of idling (0.2.0 conformance change).
2. **The mix:** `/details show healing` + `/details show taken` —
   sections stack in canonical order, separated by their headers,
   live. Untick one: its section leaves the box on the next refresh.
3. **All-off:** untick all three — NO sidebar box at all, no empty
   frame; `/details` status still reports numbers; segments still
   tracked (a later tick-on shows the running segment).
4. **EM piggyback with the mix (the owed 0.1.0 proof, now bigger):**
   in an EliteMobs party with all three sections on — party lines
   stay on top and untouched, meter sections render underneath, and
   the combined box never exceeds 15 lines (expect headers + ~2 rows
   per section under a full party box). Leave the party: standalone.
5. **Quick paths:** `/details window taken` collapses to exactly
   Damage Taken; `/details next` cycles exclusive views.
6. **Persistence:** note your overall totals (status + data.yml),
   relog AND ride a restart — totals survive; a player who had a
   0.1.0 window pref (e.g. Damage Taken) finds exactly that section
   ticked after the upgrade.
7. **Churn:** stand idle after login / eat to full — no "(0:00)"
   segment-close lines in console for lone stray events; a real
   fight still announces its close.
8. **Menu placeholders (with the menu packet):** `%details_show_*%`
   read ON/OFF per player; the Details Settings checkboxes flip the
   sections live, menu refreshing in place (Java + Bedrock).

## Same-pass surfaces owed at install

- Guide knowledge + intent updated to the mix model (0.1.0's
  CORRECTED drafts are the wording base —
  surfaces/GUIDE-AND-SITE-DRAFTS-CORRECTED-20261109.md — plus:
  /details show ticks sections, window/next are quick paths,
  overall totals now persist across restarts).
- What's New BOTH surfaces (website + ExcelWhatsNew changelog) —
  registry-first, never hand-edited.
- Website plugin-list line: version 0.2.0 + the mix description.
- Details Settings menu packet (slot 39 + submenu) — see above.
- Rotation tips check; ExcelBedrockMenu check (no Bedrock-specific
  surface; the menu renders through Geyser — proof 8 covers it).
