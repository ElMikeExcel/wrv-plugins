# INSTALL PACKET — ExcelBossTrial 0.5.9 (join-armed returns)

Lane: ~/workspace/bosstrial-armreturn-20261124/
Build record: ~/workspace/bosstrial-armreturn-20261124/BUILD-RECORD.md
Surfaces: ~/workspace/bosstrial-armreturn-20261124/SURFACES.md
Candidate jar: ~/workspace/bosstrial-armreturn-20261124/ExcelBossTrial-0.5.9.jar
  — 502,634 bytes, sha256 dae545f1a4770b3d2172d85aa81c89fd5c471eca28c6fffa135ed48b1be18a4f
  — build EXC-BOSSTRIAL-0.5.9-WRV-20261124-01-e8be92 (double-build byte-identical)

WHAT IT DOES (one line): adds /bosstrial armreturn / disarmreturn /
listreturns — an admin arms a lost-item return ahead of time (data
only, player offline is fine), and the moment that player next logs
in, BossTrial itself builds the EliteMobs item with the player
present (so EliteMobs soulbinds it), files it into their
/bossrewards, disarms, and tells them in chat + on-screen title
naming the item. It retires the poll-based jester-dagger-credit
hook's reason to exist (Mike's Nov 24, 2026 plugin-signal ruling).

## Provenance gate — STOP on mismatch

The live jar `plugins/ExcelBossTrial-0.4.1.jar` must hash to the
0.5.8 candidate placed Nov 24, 2026, 11:28 AM CT (failsafe run
wrv-install-failsafe-20261124-1121; marker
~/workspace/bosstrial-emloot-20261124/INSTALLED.txt):

- size 481,720 bytes
- sha256 119ecbad189376e12f235c6c9c2a381033579ce0dfbc0497c4e3aa91c89f995b

If the live jar does not match, STOP and re-base this packet
against whatever is actually live. Do not place over an
unidentified jar.

## Placement

JAR SWAP ONLY. No config placement, no data migration.

1. Server stopped, backup snapshot taken per the standing
   window discipline.
2. MOVE the outgoing live jar aside — never leave two
   ExcelBossTrial jars in plugins/:
   `plugins/ExcelBossTrial-0.4.1.jar` →
   `plugins/ExcelBossTrial-0.4.1.jar.bak-<install date>-pre059`
3. Place the candidate at the SAME live filename:
   `plugins/ExcelBossTrial-0.4.1.jar`
   (the 0.5.x line ships under the 0.4.1 filename on this
   server; the jar's internal plugin.yml says 0.5.9).
4. Verify the placed file's sha256 equals the candidate hash
   above before booting.

CONFIG: the live config.yml predates 0.5.9's two additions and
is NOT touched by this install — deliberately. Both additions
are code-defaulted to exactly the shipped behavior:
`commands.armreturn-enabled` defaults true, and the three
`messages.return-*` templates default to the shipped texts
(see BUILD-RECORD.md). The feature is fully on with the live
config as-is; the documented blocks ride in the jar's bundled
config.yml for any future config pass. Do NOT place the
bundled config.yml over the live one (it would clobber live
tuning — the fleet has been bitten by exactly that).

DATA: 0.5.9 writes one new file of its own,
`plugins/ExcelBossTrial/armed-returns.dat`, created on first
arm (absent = no arms; that is normal at install). The claims
ledger (team-rewards.dat) is read and written only through the
unchanged 0.5.8 grant paths. Guild state, encounters and every
other BossTrial store are untouched.

## Boot-verify

- Log line: `Enabling ExcelBossTrial v0.5.9`
- Log line: `Build: EXC-BOSSTRIAL-0.5.9-WRV-20261124-01-e8be92`
- No SEVERE from ExcelBossTrial at enable; no
  `ARMED-RETURN STORE UNREADABLE` line (there is no arm store
  on live yet — its absence is silent by design).
- Guild state unchanged: encounters/claims counts as before
  the window (read back via the ledger or /bosstrial status).

## Smoke (stores nothing)

From console, with nobody armed:

1. `bosstrial listreturns` → "No armed returns waiting."
2. `bosstrial armreturn NobodyHere emloot no_such_loot_file.yml`
   → refusal "No such EliteMobs loot file: no_such_loot_file.yml"
   and a `BossTrial armreturn REFUSED (UNKNOWN_LOOT)` audit line.
3. `bosstrial listreturns` again → still empty (the refusal
   stored nothing).

Do NOT arm a real return as part of the install smoke. The
first real arm (Jester's dagger) is the parent's separate
post-install step — see
~/workspace/jester-dagger-give-20261124/RECORD.md §10.

## Rollback

Stop the server, remove the placed jar, restore
`ExcelBossTrial-0.4.1.jar.bak-<install date>-pre059` to
`plugins/ExcelBossTrial-0.4.1.jar`, restart. No config or data
rollback is needed: 0.5.9 changed no config, and any
`armed-returns.dat` written by 0.5.9 is simply never read by
0.5.8 (inert file, leave it or delete it — deleting loses any
arms recorded under 0.5.9, so prefer leaving it unless a
re-install plan says otherwise). Claims granted by 0.5.9's
join listener before a rollback are ordinary console-grant
claims in the shared ledger and remain valid under 0.5.8.

## Marker

On verified install, write
~/workspace/bosstrial-armreturn-20261124/INSTALLED.txt with the
window, the placed hash read back from the live file, and the
boot-verify lines.
