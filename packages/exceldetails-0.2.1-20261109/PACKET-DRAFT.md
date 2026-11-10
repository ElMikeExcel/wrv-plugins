# ExcelDetails 0.2.1 — PACKET DRAFT (for the parent to run)

Jar-swap upgrade over LIVE ExcelDetails 0.2.0. Commission: Mike's
party-scoping ruling, Nov 9, 2026 (the meter must show only your
party + you). Build record:
`~/workspace/details-meter-20261109/build-0.2.1/BUILD-RECORD.md`.
Design: `~/workspace/details-meter-20261109/build-0.2.1/DESIGN.md`.

## Artifacts

- **ExcelDetails-0.2.1.jar** — 41,116 B,
  sha256 `7c5d56f3a7e70f147b14101b15d224144ae732735e7b5159efb5aba9d074251c`,
  build `EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a`
  (at `~/workspace/details-meter-20261109/build-0.2.1/ExcelDetails/ExcelDetails-0.2.1.jar`;
  repro jar byte-identical alongside it; a second full build run
  reproduced the same sha256).

## Gates (fresh panel downloads before swap, STOP on any mismatch)

- Live jar: `plugins/ExcelDetails-0.2.0.jar` = 37,130 B,
  sha256 `f216f81eefd8d09cc6abfc14efbd2b884803f6c3717d9d9946766b836cd538cb`
  (build EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19, installed Nov 9,
  2026, parent window 12:30–1:14 PM CST).
- Live `plugins/ExcelDetails/config.yml` STAYS — 0.2.1's bundled
  config is byte-identical to 0.2.0's (sha256 29cb1903…); no
  config change rides this packet.
- Live `plugins/ExcelDetails/data.yml` STAYS — format unchanged in
  0.2.1 (prefs + mix + overall totals load exactly as 0.2.0 wrote
  them); no gate hash (it mutates with play), backed up in the
  swap per standard discipline.
- Standard hand-window discipline: WINDOW-IN-PROGRESS marker,
  BACKUP FIRST with cap-at-6 (name prunees), emptiness gate.
- No other plugin, config, or spells file rides this packet. The
  Details Settings menu (registry entry + submenu, placed Nov 9)
  is unaffected — placeholders behave identically.

## Swap

- Rename the live jar (gate-verified above as
  `plugins/ExcelDetails-0.2.0.jar`) →
  `ExcelDetails-0.2.0.jar.bak-0.2.0`; place the 0.2.1 candidate
  UNDER THE SAME LIVE FILENAME `ExcelDetails-0.2.0.jar` (per the
  packet convention for this plugin — plugin identity comes from
  plugin.yml, not the filename). Verify the placed file's panel
  checksum reads `7c5d56f3…` under that name before boot. In-place
  backups before the boot: `config.yml.bak-0.2.1`,
  `data.yml.bak-0.2.1` (copies; the files themselves stay live).
  ONE restart.
- Rollback: remove the placed jar, restore
  `ExcelDetails-0.2.0.jar.bak-0.2.0` to `ExcelDetails-0.2.0.jar`.
  Jar-only rollback is safe: data.yml format is identical in both
  versions.

## Boot verify

- `ExcelDetails READY version=0.2.1 build=EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a refresh=10t timeout=8s rows=6`
- `ExcelDetails: PlaceholderAPI expansion registered (%details_show_damage%, %details_show_taken%, %details_show_healing%).`
  (absence = the Details Settings menu bridge is down; investigate
  before player proofs.)
- On the first render with a player online (renders are per
  online player on the refresh timer), ONE line per seam:
  `ExcelDetails: EliteMobs party seam BOUND (PartyManager.getParty + Party.getMembers) — display scoping counts EM parties.`
  `ExcelDetails: ExcelTogether party seam BOUND (PartyManager.partyOf + Party.members) — display scoping counts Together parties.`
  An UNBOUND line instead = that party system's rows silently
  absent (meter still correct for own rows + the other system) —
  do NOT call the fix complete; capture the line for a desk pass.
- Guild LOCKED count unchanged; MagicGate stage line unchanged;
  SEVERE 0 attributable.

## Player proofs (post-install)

1. **Mike's original report, re-run:** two players fighting in
   different places at the same time, NOT in a party together —
   each sidebar shows ONLY their own rows (before 0.2.1 both saw
   both). This is the convicting proof; it takes two seats.
2. **Party view:** party up (ExcelTogether) and fight together —
   each member sees their own + party members' rows, ranked, with
   share % computed inside the group (a member's % rises vs the
   old server-wide math when strangers are also fighting).
3. **EM party counts too:** in an EliteMobs party (dungeon party),
   members see each other even without a Together party.
4. **No in-scope rows, no box:** stand in a party whose members
   are not fighting while a stranger fights nearby — no box.
5. **Mix regression (fast):** /details show taken + healing —
   sections stack as 0.2.0; untick-all = no box; window/next quick
   paths unchanged; Details Settings menu checkboxes still flip
   sections live (Java + Bedrock).
6. **Piggyback regression:** in an EM party under the PartySidebar,
   party lines stay on top, meter sections underneath, combined
   box ≤ 15 lines.

## Same-pass surfaces owed at install

- Guide knowledge: the exceldetails-meter chunk gains the
  party-scoping sentence (SURFACES-DRAFT.md).
- What's New BOTH surfaces (website + ExcelWhatsNew changelog) —
  registry-first, never hand-edited (texts in SURFACES-DRAFT.md).
- Website plugin-table line: version 0.2.1 + scoping clause
  (SURFACES-DRAFT.md).
- Rotation tips check (a "your meter only shows your party" tip is
  a candidate — parent's call); ExcelMenu/BedrockMenu check (no
  menu change in this build; placeholders unchanged).
- FEEDBACK-TRACKER: the 0.2.0 rows keep riding; add/annotate the
  scoping proof (proof 1 above) as Mike's item.
