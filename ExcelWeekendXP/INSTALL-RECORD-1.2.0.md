# ExcelWeekendXP 1.2.0 — install record

Route Sheet: WRV-069. Prepared Oct 17, 2026 (overnight staging). PUBLISH STATE (this PR):
record PR opened Sat Oct 17, 2026 with Mike awake (GitHub approval taps expire
while he sleeps). Target: wrv-plugins OPEN DRAFT PR, branch
record/excelweekendxp-1.2.0-2026-10-17, folder ExcelWeekendXP/.

## Status at a glance
| Status | State |
|---|---|
| Built | Yes, Oct 16, 2026 by Argus from the LIVE 1.1.0 source (~/workspace/renames-2026-10-16/src/ExcelWeekendXP) |
| Automated tests | 31/31 pass (30 unit + Friday boot behavior) |
| Scratch boot | Passed: Friday boot logged "Weekend detected" at the 1.2x rate |
| Installed live | YES, Oct 16, 2026, 11:30 PM CDT via auto-watch (gate 0/10 twice) |
| Java check | UNCONFIRMED (a player's Friday/weekend XP line in real play) |
| Bedrock check | UNCONFIRMED |

## 1. Artifact
- ExcelWeekendXP-1.2.0.jar, 19,252 bytes, SHA-256 b54b3c465b5609f2b7a5df78e0498e1141d4aab6e0aed0bfd9371b0a6d609db8
- ExcelWeekendXP-1.2.0-source.zip, SHA-256 b8892a678ea746674e8e464ea6b3a0a96a08130abe89d2189cc1d3deeac402fa
- Drive deposit DONE Oct 16 (Build files: jar; Source: source zip), before install.
- NOTE: an earlier Grok-built 1.2.0 jar (SHA 67578d0f...) exists in records but
  was NOT installed; Mike directed Argus to rebuild from the live 1.1.0 source
  instead. The installed jar is the Argus rebuild above.

## 2. What changed vs 1.1.0
One behavior change only: WeekendSchedule.isWeekend is now FRIDAY || SATURDAY ||
SUNDAY (America/Chicago), so Friday gets the full weekend treatment exactly as
Sat/Sun already did (mcMMO xprate dispatch 1.2x + vanilla XP bridge 1.2x).
Version bumped to 1.2.0; plugin.yml description updated to Fri-Sun;
VanillaXpBridge version string updated. Nothing else touched.

## 3. Install (auto-watch, Oct 16, 11:18–11:30 PM CDT)
- First attempt 10:37 PM stopped at the gate: .coolgirlkail (Kailey) online.
  Nothing changed.
- Kailey disconnected 11:18:22 PM. Gate passed: console `list` 0/10 at
  11:19:00 PM and 0/10 at 11:23:18 PM (extra 0/10 at 11:19:54 PM).
- Panel Shut Down (never console stop). ExcelWeekendXP-1.1.0.jar renamed to
  ExcelWeekendXP-1.1.0.jar.bak-20261016-wxp12. New jar uploaded (panel size
  matched 19,252 B; the panel has no SHA field, so the on-server checksum is
  the one verification caveat; the jar was SHA-verified locally pre-upload).
  No data folders touched. Panel Start.
- Verification lines: "Enabling ExcelWeekendXP v1.2.0"; "Weekend detected:
  dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier
  1.2x."; "ExcelWeekendXP scheduler enabled: America/Chicago, every 30
  minutes."; Done (17.552s) 11:26:01 PM; no SEVERE naming ExcelWeekendXP; no
  "vanilla XP bridge is NOT confirmed" warning. Probe: `version
  ExcelWeekendXP` = 1.2.0; final `list` 0/10.
- Marker: ~/workspace/weekendxp-1.2.0/INSTALLED.txt

## 4. Rollback
Panel Shut Down, delete ExcelWeekendXP-1.2.0.jar, rename
ExcelWeekendXP-1.1.0.jar.bak-20261016-wxp12 back to ExcelWeekendXP-1.1.0.jar,
Start, confirm 1.1.0 enables. No data migration involved.

## 5. What this release does not do
No rate change (still 1.2x), no new days beyond Friday, no Jobs changes, no
config or data-folder changes.
