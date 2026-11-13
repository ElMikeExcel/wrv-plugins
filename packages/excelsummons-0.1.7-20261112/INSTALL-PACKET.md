# INSTALL PACKET — ExcelSummons 0.1.7 (compass Summons page) + ExcelMenu door

Status: STAGED, NOT INSTALLED. Filed by the parent into the
install queue; the installer (failsafe lane or parent hand
window) runs this packet. Commission: Mike, Nov 12, 2026,
~5:55 PM CST + refinements ~6:01 PM (adopted sub-page) and
~6:05 PM (confirm-gated per-pet adopted dismiss).
Build record: `../BUILD-RECORD.md`. Tests: `../TEST-RESULTS.md`
(89/89). Rig: `../RIG-RESULTS.md` (22/22, SEVERE 0, Bedrock
client proof OWED to acceptance below).

## Payload

1. **ExcelSummons-0.1.7.jar** — 90,538 B — SHA-256
   `2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56`
   — build `EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17`
   → placed as `plugins/ExcelSummons-0.1.7.jar` (its own
   filename, the 0.1.6-window pattern); predecessor
   `plugins/ExcelSummons-0.1.6.jar` renamed in place to
   `ExcelSummons-0.1.6.jar.bak-20261112` and held.
2. **ExcelMenu door** — `menu-entry/summons.yml` (staged beside
   this packet's folder) → placed as
   `plugins/ExcelMenu/entries/summons.yml`, then
   `/excelmenu reload`. Registry 93 → 94.
   No config.yml change (byte-identical to live). No other
   plugin's files. No Magic changes.

## G1 — fresh premise pull (STOP on any mismatch)

At window open, pull FRESH via the PC lane (panel File Manager
is the labeled fallback):

- `plugins/ExcelSummons-0.1.6.jar` must be **66,209 B /
  SHA-256 08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1**
  (the live 0.1.6). Any other size/hash = STOP, re-diagnose.
- `plugins/ExcelMenu/entries/` census: count must be **93**
  entries and `summons.yml` must NOT already exist.
- **Entry-weight verification (fresh census, commission §2):**
  the staged door carries order 27 / order-bedrock 62, derived
  from the Nov 12 run-229 census (me group: wandstyle 25/45,
  jobs 30/80). Re-pull the me-group entries at window time; if
  wandstyle/jobs weights moved, RE-DERIVE the free step /
  midpoint after Wand controls and amend the staged file before
  placing it — do not ship the staged numbers blind.
- ExcelSummons data premise: `plugins/ExcelSummons/config.yml`
  unchanged since 0.1.6 (the packet does not touch it).

## Window discipline

- ONE Shockbyte backup at window open (the window's only
  backup), named per house pattern ("Summons 0.1.7 pre-install
  1112"). **Cap-at-6:** prune the OLDEST manual backup first if
  the post-creation total would exceed 6; never delete the
  Daily Backup's newest copy or the two newest manual restore
  points; name any prunee in the install report.
- **Emptiness gate:** `list` = 0 twice before the swap (this
  window carries a jar + an entry file — fully gated, NOT a
  text live-edit). Window marker discipline per the standing
  rules; the failsafe re-checks the marker before Shut Down and
  before Start.
- Lane: PC route primary (rclone/SFTP transfers + hash
  verification), panel only for the backup + Shut Down/Start;
  automatic panel fallback with the one-line fallback notice if
  the PC route is down.
- Swap: place the candidate jar, pull back and hash-verify
  EXACT (2fc1a226…d56) before boot; rename the predecessor to
  its .bak name; place `entries/summons.yml`, pull back exact.

## Boot verification (all required)

- Log: `Enabling ExcelSummons v0.1.7` and the enable line
  "ExcelSummons 0.1.7 enabled (build
  EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17); registry holds
  272 summon(s)." (registry count = the live count at boot;
  272 at packet time — report the actual).
- Plugin count **83**; guild **LOCKED 4 of 5**; **SEVERE 0**.
- `/excelmenu reload` after boot: registry **93 → 94**, the
  Summons door present in the `me` group after Wand controls.
- Console smoke: `/summons menu` from console returns the
  players-only line (no stack trace); registry count unchanged.
- Server left ONLINE.

## Same-pass surfaces (after the install verifies)

- **What's New, BOTH surfaces:** one entry for 0.1.7 — the
  compass Summons page (cast page + adopted sub-page +
  confirm-gated adopted dismiss). Website section AND the
  in-game `ExcelWhatsNew/changelog.yml` (via
  `~/workspace/whatsnew-pipeline/registry/whatsnew.yml`), the
  in-game file verified by live pull, never assumed.
- **Guide:** knowledge + intents gain the Summons page: the
  compass door, `/summons menu`, the adopted sub-page, and the
  confirm-gated adopted dismiss (per-pet, permanent, asks
  first). The Guide must NOT claim command scopes can dismiss
  adopted pets — they cannot; the menu confirm flow is the only
  door. ExcelGuide SRC synced in the same pass.
- **Website:** index.html + ai-knowledge.json Summons section
  updated in the same pass (browser-route PR, merged +
  byte-verified per the standing routing).
- **Records:** wrv-plugins record package for 0.1.7 (source zip
  + records, jar to Drive only per house pattern) as an OPEN
  DRAFT PR; Drive deposit MD5-verified.
- **Tips / menus check:** broadcaster tips — CHECKED at packet
  time, no summons-menu tip required; in-game menus — the door
  IS the menu change (this packet); ExcelMenu + legacy
  ExcelBedrockMenu both read the same registry.
- **Jester note (owed with the landing):** a short Discord note
  that the Summons page is live and that adopted pets CAN now
  be dismissed from it — per-pet, behind a confirm that warns
  it is permanent (this supersedes the Nov 12 "adopted pets are
  not dismissable" answer he was given). Parent/supervisor
  sends; also closes the loop on tracker row 62's surface.

## Rollback

Restore the window's backup, OR (jar-level): stop, remove
`ExcelSummons-0.1.7.jar`, rename
`ExcelSummons-0.1.6.jar.bak-20261112` back to
`ExcelSummons-0.1.6.jar`, remove `entries/summons.yml`,
`/excelmenu reload` (registry back to 93), start, verify
`Enabling ExcelSummons v0.1.6` + 83 plugins + SEVERE 0. The
0.1.7 registry data format is unchanged from 0.1.6 (no data
migration exists in this packet), so a jar rollback loses
nothing.

## Acceptance (post-install, closes the commission)

1. **Mike's Bedrock compass proof:** open the compass → My
   Summons door → cast page renders with his summons + held %;
   Adopted pets button → sub-page renders his adopted pets with
   indicators. (This is also the Bedrock client proof the rig
   explicitly owes.)
2. **Jester uses the page** instead of `/summons list` at his
   next session (his `/summons list` confirmation closes
   tracker row 62; the page is its replacement surface).
3. No player-visible change to `/summons list`, dismiss scopes,
   mana holds, or caps — any report of one is a defect against
   this packet, not a tuning note.
