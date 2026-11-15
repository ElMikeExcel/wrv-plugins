# INSTALL PACKET (DRAFT) — ExcelBossTrial 0.5.6

Lint: 5034 B / max line 120 / non-ASCII 13

**Release condition: release after the Warden attempt, Sat Nov 14, 2026**

Marker: `~/workspace/bosstrial-056-build/INSTALLED.txt`
Proof owed after install: Mike's post-flip stage-5 spot-check.

Status: DRAFT, prepared by the desk build lane Nov 11, 2026.
The board supervisor reviews this packet and files it as
HELD in the install queue; the failsafe installs it only
after the release condition above is met. Live BossTrial
0.5.5 (in `plugins/ExcelBossTrial-0.4.1.jar`) stays untouched
until then — the Sat Nov 14 Warden attempt runs on 0.5.5.

## Artifact

- Jar: `~/workspace/bosstrial-056-build/ExcelBossTrial-0.5.6.jar`
  — 469,839 B / sha256
  `5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c`
- Build ID: `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
- Evidence: `BUILD-RECORD.md` (base proof, tests 2,765/98
  green, deterministic double-build byte-identical, rig
  proof incl. the elder expiry → respawn loop).

## Live-premise receipts (FILING CHECK - G1)

Live-premise receipts are cited, not inlined: see
`~/workspace/bosstrial-056-build/LIVE-PREMISE-RECEIPTS-20261111.md`
SHA-256
`3c4e88f8c783e7b71aa473383d620188328e195f82f10575c6bb74f2d3dd5b0c`
(1,964 B; PC-route pull 2026-11-11 20:45:37 -06:00).
That file carries the receipts for the live jar
(`plugins/ExcelBossTrial-0.4.1.jar`, 455,307 B /
eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb),
live `config.yml`, live `state.yml`, and `usercache.json`
(the counter-seed UUID source for step 5 below).
Fresh re-pull owed at release: at the post-Nov 14 release
the installing lane re-pulls fresh same-day receipts at
its provenance gate (G1) - these HELD receipts do NOT
satisfy an install gate.

## Install steps (standard gates apply)

1. BACKUP FIRST under the house rule (one fresh backup for
   the window; cap-at-6, prunees named).
2. Emptiness gate (verified empty, twice) for the swap.
3. Swap the jar in place at the live jar's existing filename.
4. Config: MERGE, never wholesale-replace. Add exactly:
   - `elder-guardian.progress-expiry-hours: 24`
   - `messages.elder-progress-expired: "&eThe monument's curse re-knits \u2014 the trio must be faced whole, and soon."`
   (Snippets staged in `live-config-merge/` of the build
   folder. Everything else in the live config is untouched;
   the taper table itself is code-settled, documented in the
   bundled config's comment block.)
5. Counter seeding (state.yml): while the server is stopped,
   add the `counter-seeds` section with the LIVE UUIDs of
   Marvin and Jester (from the live usercache) at
   `elder_guardian: 2` each — and nothing else (no wither
   seed: the Nov 7 break was a manual grant). Shape:
   ```yaml
   counter-seeds:
     elder_guardian:
       <Marvin-UUID>: 2
       <Jester-UUID>: 2
   ```
   The plugin applies each seed once at load under a MAX
   merge (raises to at least 2, never lowers, idempotent)
   and logs `Counter seed applied: …`. The section is not
   re-written by the plugin's state saves; the merged counts
   persist in `repeat-clears`.
6. Start the server; verify the boot lines:
   - `Enabling ExcelBossTrial v0.5.6`
   - `Build: EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
   - `Cash taper: ON (… clear #1 warden $10000 / others $5000, clear #2 $5000, clear #3+ $2000 …)`
   - `Seal cycle: cycle 1, 4 of 5 this cycle, guild opened latch unset, …`
     (the live guild stands at 4/5 pre-Warden; after the
     Warden break on 0.5.5, expect the mirror to credit seals
     as they stand in GuildGate at first boot — verify the
     cycle line against `/guildgate status` and note any
     divergence in the install report; the mirror credits
     from BossTrial's own kill paths, so a first boot that
     reads fewer than the live count is a known, safe
     starting state, NOT an error: lifetime counts and the
     taper are unaffected)
   - `Elder progress expiry: ON (24.0h …)`
   - two `Counter seed applied:` lines naming elder_guardian
     at lifetime clear #2.
   - SEVERE 0 from BossTrial.
7. Post-install state read: `repeat-clears.elder_guardian`
   shows Marvin + Jester at 2 (or higher if the live book
   already stood higher — MAX merge); `first-break-personal`
   and `seal-cycle` sections exist in state.yml.

## Rollback

Swap the 0.5.5 jar back at the same filename and restart.
All 0.5.6 state sections are additive (a 0.5.5 rollback
ignores `first-break-personal`, `seal-cycle`, `cycle-clears`,
`counter-seeds`, and the elders' `countedAtMs`); counts the
taper already paid stand. The two merged config keys are
inert under 0.5.5.

## Companion note

The GuildGate half of the seal-cycle design (SealState
latch, cycle-scoped API, `SealCycleEvent`) is a separate
GuildGate cycle build per
`~/workspace/sealcycle-20261110/DESIGN-AND-BUILDSCOPE.md`
§4.1; 0.5.6 is safe and complete on its own (BossTrial
mirror + elder expiry + taper + extras), and pairs with that
build when it lands.
