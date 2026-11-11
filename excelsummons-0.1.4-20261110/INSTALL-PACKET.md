# INSTALL PACKET — ExcelSummons 0.1.4 (Wolf / summons fix)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Design: FIX-PACKET-DESIGN §§2–4
(`~/workspace/wolf-summon-diagnosis-20261110/`) as amended by
Mike's Nov 10, 2026 rulings (companions hold mana, 10:08 AM;
refusals explain themselves, 10:34 AM; rank-scaled cap,
10:35 AM; ladder LOCKED 10:37 AM: Beginner 1 · Student 2 ·
Apprentice 3 · Master 5).

## Why

The Wolf bug (Marvin: Wolf returns only "you cant do that right
now" at full mana) was diagnosed to Magic-internal suspects the
bridge cannot reach — but the bridge's own rules were also
behind Mike's rulings: companions held no mana reserve, the
summon cap was three disconnected per-branch counts, bridge
refusals were number-less, and Magic's own summon failures
could be silent (no_target) or generic (insufficient_permission).
0.1.4 fixes everything the bridge owns and makes every
remaining failure self-diagnosing (§5.3 matrix below convicts
the Magic-side suspects).

## The artifacts (all four ship together, one window)

- ExcelSummons-0.1.4.jar — 63,642 bytes
  SHA-256 a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a
  MD5 5865685388942018f9d4de01e1731d1d
  Build EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19
  Staged at ~/workspace/summons-014-build/ExcelSummons-0.1.4.jar
- modifiers-addition-0.1.4.yml — the +11 reserve templates
  (wolf…wolf5, familiar…familiar3, cat, cat2, mount), appended
  to Magic's modifiers.yml. The 0.1.2 packet's seven combat
  templates must already be there (verify; if absent, append
  those too from
  ~/workspace/spell-audit/packet-summons-20261108/modifiers-addition.yml).
- messages-addition-0.1.4.yml — per-spell refusal overrides,
  merged under a new `spells:` section in Magic's messages.yml
  (the live file is the stock template with no spells section).
  No existing key is edited; spells.default is untouched.
- config.yml (in the jar / build tree) — REPLACE the live
  ExcelSummons config with the shipped one, or hand-merge:
  add `summon-cap-by-rank` (beginner 1, student 2, apprentice
  3, master 5), REMOVE `per-caster-combat-cap`, add the four
  reserve rows (wolf 10, familiar 5, cat 5, mount 10). Do NOT
  skip this: unlike 0.1.3, 0.1.4's behavior lives in the config.
- **Live-jar gate:** if 0.1.3 was installed first, the live jar
  is ExcelSummons-0.1.3.jar (54,707 B, SHA-256 d6dc49d0…c852).
  If it was not, the live jar is ExcelSummons-0.1.2.jar
  (48,944 B, SHA-256 064e0ba5…2473) and 0.1.4 SUPERSEDES the
  pending 0.1.3 outright (0.1.4 contains the whole 0.1.3 delta).
  Verify which is live before the swap; if neither hashes,
  STOP — the delta was built against exactly these bases.
- Data: summons-data.yml gains an `origin` key per record on
  its next save (missing key reads as adopted — safe both
  directions; see Rollback).

## The swap (standard gates)

Preconditions: `list` = 0 twice; guildgate LOCKED count
pre-flight as usual; marker discipline
(~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt at start,
deleted at finish, foreign-marker re-checks before Shut Down
and before Start); BACKUP FIRST with cap-at-6 (name prunees;
never the Daily Backup's newest copy or the two newest manual
restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar to `ExcelSummons-<live>.jar.bak-20261110`
   (keep alongside; only one ExcelSummons jar loadable).
3. Upload ExcelSummons-0.1.4.jar as
   plugins/ExcelSummons-0.1.4.jar. Verify by exact size,
   63,642 bytes, in the file listing.
4. Replace plugins/ExcelSummons/config.yml with the shipped
   0.1.4 config (back the live one up in place first,
   config.yml.bak-20261110).
5. Append modifiers-addition-0.1.4.yml to
   plugins/Magic/modifiers.yml (back up in place first). The
   bridge fail-soft covers a miss with one warning per key,
   but the window ships them together by design.
6. Merge messages-addition-0.1.4.yml's `spells:` section into
   plugins/Magic/messages.yml (back up in place first).
7. Confirm plugins/ExcelMagicGate/earned.yml exists (the rank
   seam reads it; if MagicGate 0.3.0 is not yet installed, caps
   fail to the lowest rung — Beginner 1 — until it is; name
   that state in the install report if so).
8. Start the server; run the boot-verify + console smoke.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelSummons v0.1.4" present.
- Enable line names **0.1.4** and build
  **EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19**; registry-restored
  count sane.
- Zero SEVERE attributable to the swap; **no "could not be
  applied" reserve warnings** (a warning names a missing
  modifiers template — step 5 was missed for that key);
  guildgate LOCKED count unchanged; MagicGate stage line
  unchanged; plugin count unchanged from pre-swap.

## Console smoke (from the Shockbyte console, post-boot)

1. `summons admin list NobodyHereXYZ` → the unknown-name line
   (0.1.3 surface, unchanged).
2. `summons admin dismiss NobodyHereXYZ pets` → the same line
   (the pets scope routes; a bad scope token like `bogus`
   prints the admin usage line, which now reads
   "[all|pets]").
3. `summons list` → "The registry holds N summon(s)." (legacy
   console global count, unchanged); `summons admin reload`
   unchanged.
4. With a volunteer online: `summons admin list <volunteer>`
   shows companion lines WITH their holds (e.g. "- wolf (pack,
   persistent, HP 8/8, holds 10% mana)").

## Player proof + §5.3 controlled-cast matrix (one session)

Run the design's §5.3 matrix in the same window if a volunteer
(Marvin's account or a same-stage stand-in) is available;
otherwise file it as the named post-install proof. Log every
cast with time, /summons list output, and the exact chat text:

1. Baseline: cooldown ≥ 5 min expired, pool full, nothing out,
   no toggles — Wolf at a penned living target, then Wolf at
   empty ground (Suspects 2+4; the penned animal is the
   controlled target until ExcelTrainingDummy lands).
2. Suspect 1: `summons admin list <player>` + a physical sweep
   for owned tamed wolves; compare counts (the §1 rule).
3. Suspect 3: Guardian out (25% hold), bar refilled, Wolf at
   the penned target; dismiss Guardian, repeat. Wolf's own
   10% hold reads in /summons list.
4. Cap proof (rank-scaled): at the volunteer's CURRENT rank
   cap — fill the counted total (e.g. Student: Wolf + Cat) →
   next counted cast gives the TOTAL_CAP text naming rank,
   limit, count, and the summons out; mana unchanged. Mount at
   the cap succeeds (not counted) and shows its 10% hold.
   `dismiss pets` releases Wolf + Cat holds; the next counted
   cast succeeds.
5. Ceiling proof: Guardian + Ghast + Revenant (70%) → Mount
   cast gives the RESERVE_CEILING text naming 10/70/75; mana
   unchanged. Dismiss one combat summon → Mount succeeds.
6. Release proof: kill a reserve-holding summon; let a combat
   summon expire; relog with companions out — after each,
   /summons list's held total matches the registry exactly;
   dismiss everything → no held-total line.
7. Magic-side texts: force a Wolf cooldown re-cast (expect
   "Wolf is still recharging — you must wait …"), a Wolf cast
   at empty ground (expect the no-target text, NOT silence),
   and a cast with a drained pool (expect the mana text naming
   Wolf's cost). Write the matrix results — including whichever
   §1 suspect is convicted — into the install record.

## Rollback

Stop; delete ExcelSummons-0.1.4.jar; rename the .bak jar back;
restore config.yml.bak-20261110; remove the appended modifiers
templates and the messages spells section (or restore the
in-place backups); start; verify the prior version's enable
line. Data note: summons-data.yml written by 0.1.4 carries the
extra `origin` key per record — the 0.1.3/0.1.2 codec reads
explicit fields and ignores it, and on the next 0.1.4-era save
the field is rewritten; no data migration exists in either
direction. The Magic config additions are inert without the
0.1.4 jar (unused templates; per-spell messages remain valid
Magic config and may stay if a rollback is expected to be
temporary — say which was done in the install report).

## Same-pass surfaces owed at install

Guide knowledge + intents for: companion/mount mana holds, the
rank-scaled summon cap (Beginner 1 · Student 2 · Apprentice 3 ·
Master 5), `dismiss pets`, and the refusal texts; What's New
BOTH surfaces (website section AND the in-game ExcelWhatsNew
changelog) in player terms; rotation tips checked (a summons
tip naming the OLD flat cap or the reserve exemption, if one
exists, must be corrected — state the check); in-game menus
checked (ExcelMenu + ExcelBedrockMenu — the Details Settings
precedent suggests a Summons entry check; no menu change is in
this packet, state the check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package staged at
~/workspace/summons-014-build/records-package/, and the §5.3
matrix results appended to the install record when run.
