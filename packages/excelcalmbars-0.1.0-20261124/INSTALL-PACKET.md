# INSTALL PACKET - ExcelCalmBars 0.1.0 (combat-calm XP bars)

Lint: 5025 B / max line 69 / non-ASCII 0
Filed: Nov 24, 2026 (build lane; NOT installed by the lane, per D212)
Decision: D227 (Mike's ruling, Nov 24, 2026, 6:30 PM CT)
Feedback: FEEDBACK-TRACKER row 92 (Marvin, stacked XP boss bars)
Build record: ~/workspace/combat-calm-bars-20261124/BUILD-RECORD.md
Receipts: ~/workspace/combat-calm-bars-20261124/RECEIPTS.md

## Live-premise receipts (same-day pulls, Nov 24, 2026 ~6:35 PM CST)

- Absence premise: no ExcelCalmBars jar/folder on live - receipt:
  live plugins positive listing, 608 lines, in RECEIPTS.md
  (plugins-listing-clean.txt, SHA-256 2cb77ac0...; grep -ci
  calmbars = 0). Gate re-runs the same listing at install time.
- EliteMobs skills.yml live: showXPBar true + skill title formats -
  RECEIPTS.md (3,059 B, SHA-256 6142cdb8...).
- mcMMO experience.yml live: Experience_Bars.Enable true -
  RECEIPTS.md (20,908 B, SHA-256 aba562a2...).
- Live bar-owner jars (patterns derived from these exact bytes):
  mcMMO 5821026c..., ExcelDeathProtection 0.2.2 aefc05d1... -
  RECEIPTS.md.

## Candidate

- packet/ExcelCalmBars-0.1.0.jar - 19,006 B - SHA-256
  111794a81c830f6886a5fe5881d358ccfc0387b83ab2d7a58c0ca6f877c899e0
- Build: manual javac (build.sh in the build folder), deterministic
  (two consecutive builds byte-identical).
- Tests: BarPatternsTest 22/22 on final sources; mirror proof
  (a)-(e) all PASS - BUILD-RECORD.md.

## Live-jar gate (STOP on mismatch)

- There is NO outgoing jar: this is a new plugin. Gate = the
  absence premise above, re-verified at install: fresh positive
  listing of live plugins/ must still show no ExcelCalmBars entry
  (command: panel/PC directory listing of D:\muse-servers\wrv\
  plugins, expected: no line matches /calmbars/i). If a CalmBars
  jar IS present at gate time, STOP - another lane landed it;
  verify version/hash against this candidate instead of placing.

## Staged artifacts (packet/, SHA256SUMS inside)

- ExcelCalmBars-0.1.0.jar - 19,006 B - 111794a8...
- config.yml - 2,868 B - dede5592... (deploy config: quiet-period-
  seconds 30, max-hold-seconds 120, enabled true)
- SURFACES.md - fc88a349... (What's New in-game entry text +
  website line, staged for the same pass)

## Scope

- PLACE: plugins/ExcelCalmBars-0.1.0.jar (new) and
  plugins/ExcelCalmBars/config.yml (new folder, deploy config).
- Nothing else changes: no existing jar, config, data, or player
  file is touched. The plugin has no hard depend on Jobs, mcMMO,
  EliteMobs, or ExcelDeathProtection and gates nothing if any of
  them is absent.
- Behavior: XP-gain boss bars (positively identified Jobs / mcMMO /
  EliteMobs titles only) are held while the player is in combat and
  for 30 s (config) after the last damage dealt/taken, then
  released. Boss bars, quest bars, the death-protection bar, and
  any unidentified bar are never held. Fail-open on any error.
- Companion, NOT a dependency: the Jobs boss-bar Timer trim 5 to 2
  was filed separately and is already INSTALLED as a TEXT live-edit
  (Nov 24, 2026). This packet installs and works either way.

## Gates

- Standard jar-install discipline: marker discipline with marker
  re-checks immediately before Shut Down and before Start;
  BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or
  the two newest manuals); emptiness `list` = 0/10 twice before
  the swap. Backup covers the plugins folder state per standing
  practice (this install only ADDS files; rollback = delete the
  jar + the ExcelCalmBars folder, restart).

## Boot verify

- Log line, verbatim shape: "ExcelCalmBars 0.1.0 ready - quiet
  period 30s, max hold 120s, bridge ok" (the dash renders as an
  em-dash in the log; match on "ExcelCalmBars 0.1.0 ready" +
  "bridge ok").
- On first join, an INFO line "[calmbars] gate injected for
  <player> before ..." must appear; /calmbars status from a player
  answers with bridge ok, gate on, quiet period 30s.
- Boot Done line present; SEVERE 0 attributable; guild state
  unchanged.

## Same-pass surfaces

- Staged in packet/SURFACES.md: What's New in-game entry
  (2026-11-24-combat-calm-bars) + website What's New line - the
  install window places BOTH in the same pass and verifies the
  in-game changelog from the live file after placement.
- Guide: hibernated (standing) - none owed. Broadcaster tips:
  checked, none describe XP-bar timing - none owed. In-game menus:
  none for this change. GitHub record/website publication: parent
  lane at install per the close-out publication rule.

## Proof owed after install

- Natural-play confirmation: Marvin (the reporter) and/or Jester
  fight back-to-back elites in Primis and confirm XP bars no longer
  stack mid-fight and appear after the fight; Iris bot-verifies
  /calmbars status + a held/released debug pair on live within the
  first day if a player is on. Proof discharges only on the
  player's own report or Iris's live log evidence.

## Marker on install

- ~/workspace/combat-calm-bars-20261124/INSTALLED.txt
