# ExcelTogether 0.3.0 — install record

Route Sheet: WRV-068 (party range indicator, Mike's Option C pick, Oct 17,
2026). Prepared Oct 17, 2026. Target: wrv-plugins OPEN DRAFT PR, branch
record/exceltogether-0.3.0-2026-10-17, folder ExcelTogether/.

## Status at a glance
| Status | State |
|---|---|
| Built | Yes, Oct 17, 2026 by Argus from the SHA-verified 0.2.1 source |
| Automated tests | 492/492 pass (incl. new RangeIndicatorTest, 61 checks) |
| Scratch boot | Passed: enable line "Range indicator: ON (action bar every 2.5s, cue hysteresis 3s)"; config parity with live |
| Installed live | YES, Oct 17, 2026, 8:08 AM CDT via auto-watch (gate 0/10 twice) |
| Java check | UNCONFIRMED (two-player party test of the indicator still owed) |
| Bedrock check | UNCONFIRMED (Geyser rendering of title/sound cues still owed) |

## 1. Artifact
- ExcelTogether-0.3.0.jar, 97,146 bytes, SHA-256 55a6b3eacdbf04754755a3703991a267b18740a5d0f3af363268fdc06768d4b2
- ExcelTogether-0.3.0-source.zip, SHA-256 b51ebe1715ca3d8f7737b40cace7982d5671c305d5fc7bafbc675d9704a3415c
- Rebuild reproduction: a second clean build came out byte-identical (same SHA).
- Drive deposit DONE Oct 17, before install (Build files: jar; Source: source zip).

## 2. What changed vs 0.2.1
Display only. Payout code (PartyListener, PartyBonus, JobsShareSink,
XpShareCore, McmmoXpShare) is untouched, and the payout test suites pass
unmodified.

- Party range indicator (Option C): while a player is in a party, a ticker
  every 2.5 seconds shows an action bar line: green "Party bonus ACTIVE: N in
  range (x1.25)" (N = party members within 50 blocks, same world, not
  counting the viewer) or gray "Party: nobody in range". Not in a party:
  nothing shown.
- The count comes from the SAME range test the bonus pays on
  (PartyMemberFinder.near() + PartyBonus.within(), inclusive at exactly 50
  blocks); the tests assert count > 0 exactly when hasPartner() is true.
  There is no second implementation of the geometry.
- Transition cues: when the debounced state flips, a title flash "Party bonus
  ON" (green) or "Party bonus OFF" (gray). A soft sound
  (ENTITY_EXPERIENCE_ORB_PICKUP, volume 0.5) plays ONLY on turning on.
- Hysteresis ~3 seconds on the cues, so standing at the 50-block edge cannot
  strobe titles/sounds. The action bar itself always shows the live sample.
- Per-player toggle: /excelparty cue on|off (no argument flips it). One
  "cues" package covers title + sound. Default on. The toggle is in-memory,
  matching this plugin's existing per-player state pattern (parties are
  memory-only too): it resets on relog/restart. First sample after joining a
  party or logging in initializes silently (no ON flash for spawning next to
  your party); leaving a party resets silently.
- plugin.yml: version 0.3.0; description notes the indicator; excelparty
  usage gains "cue". config.yml: header + comment block only, no new keys.

## 3. Install (auto-watch, Oct 17, 2026)
- Gate readings (both required 0/10): console `list` 0/10 at 8:00:28 AM and
  0/10 at 8:02:33 AM CDT (confirmatory third reading 0/10 at 8:02:58 AM).
- Panel Shut Down (never console stop) 8:03 AM; header Offline confirmed.
- plugins/ExcelTogether-0.2.1.jar renamed to
  ExcelTogether-0.2.1.jar.bak-20261017-wrv068. ExcelTogether-0.3.0.jar
  uploaded (97,146 bytes; panel listing 94.87 KB; SHA verified locally
  pre-upload — the panel cannot display a hash, the standing caveat).
- Panel Start; boot log from 8:08:00 AM. Verification lines: "Loading server
  plugin ExcelTogether v0.3.0"; "Enabling ExcelTogether v0.3.0"; enable line
  contains "Range indicator: ON (action bar every 2.5s, cue hysteresis 3s)";
  Done (16.836s) at 8:08:17 AM; no SEVERE/Error naming ExcelTogether;
  `version ExcelTogether` = 0.3.0; final `list` 0/10.
- Jar-only swap: no config or data folder changed.
- Marker: ~/workspace/exceltogether-0.3.0/INSTALLED.txt

## 4. Rollback
Panel Shut Down, delete ExcelTogether-0.3.0.jar, rename
ExcelTogether-0.2.1.jar.bak-20261017-wrv068 back to ExcelTogether-0.2.1.jar,
Start, confirm 0.2.1 enables. No data migration involved.

## 5. What this release does not do
No payout, sharing, or party-logic change. The indicator counts party
members only (the live bonus-mode is PARTY; under a hypothetical
nearby/either mode it would still count party members only, per Mike's
spec). Known display tradeoff: Jobs/mcMMO also write to the action bar, so
the range line and payout lines alternate on the 2.5s refresh rather than
stack.
