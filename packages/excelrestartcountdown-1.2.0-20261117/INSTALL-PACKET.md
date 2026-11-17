# INSTALL PACKET — Excel Renames Part 2 (WRV-060)

**Status: STAGED, NOT INSTALLED.** Built, tested, and verified
October 16, 2026 (see `RENAME-REPORT.md`). This packet is the complete,
self-contained install procedure for the combined four-plugin rename.
One install window, one restart, all four plugins.

**What swaps:**

| Out (live now) | In |
|---|---|
| `WealthBoard-1.3.0-mobile.7.jar` | `ExcelWealthBoard-1.4.0.jar` |
| `WeekendXP-1.0.2.jar` | `ExcelWeekendXP-1.1.0.jar` |
| `NoPhantoms-1.0.0.jar` | `ExcelNoPhantoms-1.1.0.jar` |
| `RestartCountdown-1.1.0.jar` | `ExcelRestartCountdown-1.2.0.jar` |

Verify each uploaded jar against these SHA-256 values **before** starting
the server (a mismatch = stop, do not start):

```
f989a82431e0a5fb509bd8cff9f15b9452abbd4dfae332cdf816e5b6517cfee2  ExcelWealthBoard-1.4.0.jar
e9150198dd3260ad5ae829067f84131cc2dabc135e495e6c5d7e41f228417fa1  ExcelWeekendXP-1.1.0.jar
2c8f3a7688df3f11f295b29d220de74d4c08f37b0ba4d6a103f5d53128529609  ExcelNoPhantoms-1.1.0.jar
50dfa7b89b6bad50ce368bda198314d6b3d429522c77c8f811e95124f04530cd  ExcelRestartCountdown-1.2.0.jar
```

And the outgoing jars, for the rollback record:

```
1bcfd3d1679d0fe2551188a2e53905ca42bac0e8a071a1ee4fe995a3d205ddcd  WealthBoard-1.3.0-mobile.7.jar
4452ccd90bea409fca37740d3e4e9967ef6dd56e9280000521eec868cf0254d0  WeekendXP-1.0.2.jar
240e2cf7660a7cf515bf341bd8ed1ed7581099b933060e01c11c5c926b186195  NoPhantoms-1.0.0.jar
5ed80156802ee4303c719dcd7e2dba245786a50b81f2d99384ff0d6bbb2d08d0  RestartCountdown-1.1.0.jar
```

---

## Gate (all must hold before step 1)

1. **Server empty.** Console `list` shows **0 of 10** players, checked
   **twice**, a few minutes apart. One empty `list` is never enough.
   **If Kailey is online, hold and tell Mike** — no exceptions.
2. No other install/restart work in flight.
3. Marker check (auto-watch runs): if
   `~/workspace/renames-2026-10-16/INSTALLED.txt` exists, this packet has
   already run — stand down and report; do not run it twice.

## Steps

Panel: https://panel.shockbyte.com/server/2602e828-7192-4a75-add8-74861e5632d7
(file manager for the jar swaps). **Panel "Shut Down" only** — console
`stop` auto-restarts on this host and must not be used to keep the server
down during the swap.

1. Panel → **Shut Down**. Confirm the server is fully stopped.
2. In `plugins/`, rename the four live jars (this is the rollback set —
   do not delete them):
   - `WealthBoard-1.3.0-mobile.7.jar` → `WealthBoard-1.3.0-mobile.7.jar.bak-20261016`
   - `WeekendXP-1.0.2.jar` → `WeekendXP-1.0.2.jar.bak-20261016`
   - `NoPhantoms-1.0.0.jar` → `NoPhantoms-1.0.0.jar.bak-20261016`
   - `RestartCountdown-1.1.0.jar` → `RestartCountdown-1.1.0.jar.bak-20261016`
3. Upload the four new jars into `plugins/`. Verify each upload's
   SHA-256 against the table above (file-manager hash or a re-download
   check). **All four old jars must already be `.bak-20261016` before
   any new jar lands** — old and new enabled together would double-fire
   WealthBoard's join popup.
4. **Data folders: touch nothing.** None of the four has a live data
   folder today; if any legacy folder (`plugins/WealthBoard`,
   `plugins/WeekendXP`, `plugins/NoPhantoms`,
   `plugins/RestartCountdown`) exists at install time, leave it exactly
   where it is — the new plugin copies it into its new folder on first
   boot and leaves the legacy folder in place.
5. Panel → **Start**.
6. Verify from the console log (all of these, verbatim forms):
   - `[ExcelWealthBoard] Enabling ExcelWealthBoard v1.4.0` →
     `[ExcelWealthBoard] ExcelWealthBoard enabled (join popup mode, no sidebar).`
   - `[ExcelWeekendXP] Enabling ExcelWeekendXP v1.1.0` → a schedule line
     (`Weekday detected: … verified vanilla XP event multiplier 1.0x.` on
     a weekday, or the weekend-applied line on Sat/Sun) →
     `[ExcelWeekendXP] ExcelWeekendXP scheduler enabled: America/Chicago, every 30 minutes.`
     A `vanilla XP bridge is NOT confirmed` warning instead means the
     mcMMO bridge did not verify — flag it to Mike immediately (do not
     roll back on this alone; the scheduler retries every 30 minutes).
   - `[ExcelNoPhantoms] Enabling ExcelNoPhantoms v1.1.0` →
     `[ExcelNoPhantoms] ExcelNoPhantoms enabled: phantoms will not spawn.`
   - `[ExcelRestartCountdown] Enabling ExcelRestartCountdown v1.2.0` →
     `[ExcelRestartCountdown] ExcelRestartCountdown enabled. Use /restartwarn [reason] to start a 2-minute countdown.`
   - If (and only if) legacy data folders existed: one
     `Migrated legacy data folder plugins/<Old> -> plugins/<New> (N files)`
     line per affected plugin.
   - No `SEVERE` and no stack trace naming any of the four plugins.
   - Server reaches `Done`.
7. Console probes (all safe; none starts anything):
   - `version ExcelWealthBoard` → `ExcelWealthBoard version 1.4.0`
   - `version ExcelWeekendXP` → `ExcelWeekendXP version 1.1.0`
   - `version ExcelNoPhantoms` → `ExcelNoPhantoms version 1.1.0`
   - `version ExcelRestartCountdown` → `ExcelRestartCountdown version 1.2.0`
   - `excelrestartwarn cancel` → `No countdown is running.`
   - `restartwarn cancel` → `No countdown is running.` (legacy alias)
   - `plugins` → the four new names present; none of the four old names
     present.
8. Final `list` → still 0/10 (or whoever legitimately joined after).
9. Write the marker `~/workspace/renames-2026-10-16/INSTALLED.txt` with
   the install timestamp, the two gate readings, and the verification
   lines observed.
10. **Publishing, same window** (per the completion gate — a rename is
    not "done" until its record publishes):
    - Push the four records in `records/` to wrv-plugins as ONE open
      **draft** PR (branch `record/excel-renames-part-2-2026-10-16`),
      with the renamed sources.
    - Website: update the four plugins' entries on the server site
      (new names + versions; note the rename on each) + a changelog
      entry; merge and verify the live page byte-for-byte.
    - Update `~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md` (four
      rows) and Route Sheet WRV-060 → Done (keep the row; history is
      never deleted).
    - GitHub writes need Mike awake for the approval taps — if the
      install lands overnight, stage the pushes and shepherd them when
      he is up (approval cards expire; see the Oct 16 lesson).

## In-game checks owed after install (Mike or a player session)

- Join once (any account): ExcelWealthBoard popup renders as before and
  Jobs auto-enroll still fires for a not-yet-enrolled player.
- Weekend boundary: the first Sat 00:00 America/Chicago check after
  install applies the 1.2x rate (log line), Mon 00:00 resets it. (If the
  install happens on a weekend, the applied line should already show at
  boot in step 6.)
- Phantoms: none spawn at night near an insomnia-eligible player
  (passive confirmation over normal play; the handler is unchanged).

## Rollback (complete)

1. Panel → **Shut Down**.
2. Delete the four new jars from `plugins/`.
3. Rename the four `.bak-20261016` jars back to their original names.
4. Leave any `plugins/Excel*` data folders in place — they are harmless
   (the old plugins never read them), and the untouched legacy folders
   are still exactly as they were (migration copies, never moves).
5. Panel → **Start**; verify the four old names enable at their old
   versions, and `restartwarn cancel` answers `No countdown is running.`
6. Note the rollback in the Route Sheet row (status change, never a
   deletion) and tell Mike what was observed.

## Auto-watch wording (for the scheduled watcher)

- **Job:** check the gate (console `list`) on the watch interval. Run
  this packet only when two consecutive checks, minutes apart, both
  read **0 of 10** players online **and** no `INSTALLED.txt` marker
  exists in `~/workspace/renames-2026-10-16/`.
- **Kailey clause:** if `.coolgirlkail` appears in either gate reading,
  hold that cycle entirely and note it; never restart around her.
- **On success:** execute steps 1–9 exactly, write `INSTALLED.txt`,
  report the verification lines to Mike, run step 10's publishing when
  Mike is awake to tap approvals, then remove the watch.
- **Expiry / stand-down:** if the gate has not cleared by **Saturday,
  October 17, 2026, 9:00 AM CDT**, the watch stands down on its own:
  it removes itself and leaves Mike one message — "WRV-060 rename
  packet is still staged and verified; the server never emptied during
  the watch window, so nothing was installed. The packet stays ready
  at ~/workspace/renames-2026-10-16/INSTALL-PACKET.md for the next
  window." No partial steps may be taken at expiry: if the swap has
  begun it always finishes through step 9 or rolls back completely.
