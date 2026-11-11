# INSTALL PACKET — ExcelSummons 0.1.5 (Archer equipment guarantee)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 11, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Diagnosis of record:
`~/workspace/goals/family-minecraft-server-build/hidden_files/archer-bow-diagnosis-20261110.md`
(layer-1 desk check DONE Nov 10, 2026; this packet is its
layer-2 fix route).

## Why

Jester reported Nov 10, 2026 that the archer summon spawns
without a bow. The spell definitions specify the gear at every
tier (bow in hand at all tiers; iron helmet base, chainmail at
tiers 2/3) and the layer-1 desk check verified the applied
spells output carries exactly those values — the config is
correct, but Magic's SpawnEntity layer does not apply
item_in_hand on spawn. 0.1.5 makes the bridge guarantee the
gear itself: when a CAST archer registers, the bridge sets the
bow + tier helmet and verifies them by read-back, in the same
soft-fail shape as the happy ghast's harness. Nothing else
changes from 0.1.4.

## The artifact (one file, one window)

- ExcelSummons-0.1.5.jar — 65,253 bytes
  SHA-256 5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e
  MD5 f19b51d7b75966e799897bc4ebabb4b0
  Build EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315
  Staged at ~/workspace/summons-015-build/ExcelSummons-0.1.5.jar
  (copy at ~/workspace/summons-015-build/ExcelSummons/ExcelSummons-0.1.5.jar)
- **No companion files.** Unlike 0.1.4, this swap ships nothing
  else: config.yml is byte-identical to the installed 0.1.4
  config (do NOT replace it), Magic's modifiers.yml /
  messages.yml need no merge, and summons-data.yml gains no new
  fields (no record change — rollback is the plain jar swap in
  both directions, no data note).
- **Live-jar gate:** the live jar is ExcelSummons-0.1.4.jar
  (63,642 B, SHA-256 a34d7bdf…25a — installed Nov 11, 2026,
  ~2 AM; INSTALLED.txt in the 0.1.4 build folder). Verify
  before the swap; if it does not hash, STOP — the delta was
  built against exactly this base.

## The swap (standard gates)

Preconditions: `list` = 0 twice; guildgate LOCKED count
pre-flight as usual; marker discipline
(~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt at start,
deleted at finish, foreign-marker re-checks before Shut Down
and before Start); BACKUP FIRST with cap-at-6 (name prunees;
never the Daily Backup's newest copy or the two newest manual
restore points). The one-backup-per-window cadence applies.

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar to `ExcelSummons-0.1.4.jar.bak-20261111`
   (keep alongside; only one ExcelSummons jar loadable).
3. Upload ExcelSummons-0.1.5.jar as
   plugins/ExcelSummons-0.1.5.jar. Verify by exact size,
   65,253 bytes, in the file listing (and by re-pull hash where
   the route allows: 5cdf16a0…f61e).
4. Start the server; run the boot-verify + console smoke.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelSummons v0.1.5" present.
- Enable line names **0.1.5** and build
  **EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315**; registry-restored
  count sane (the live registry, ~242 records at the 0.1.4
  install, carries over untouched).
- Zero SEVERE attributable to the swap; **no "could not be
  applied" reserve warnings**; **no "archer equipment skipped"
  or "did not verify" lines at boot** (none are expected at
  boot — the guarantee fires per cast; their appearance on a
  cast is a soft-fail note, not a boot failure, but say so in
  the install report if one appears); guildgate LOCKED count
  unchanged; MagicGate stage line unchanged; plugin count
  unchanged from pre-swap.

## Console smoke (from the Shockbyte console, post-boot)

1. `summons admin list NobodyHereXYZ` → the unknown-name line
   (surface unchanged from 0.1.4).
2. `summons list` → "The registry holds N summon(s)."
   (unchanged); `summons admin reload` unchanged.

## Player proof (the diagnosis's named proof — Jester)

One session, whenever Jester is next on:

1. Cast Archer (base tier) at a living target: the skeleton
   takes the field WITH a bow in hand and an iron helmet.
   Screenshot settles the diagnosis's open half too (whether
   Magic had been applying the helmet).
2. If Jester has the tiers: cast archer|2 / archer|3 — bow +
   chainmail helmet each.
3. Confirm the archer shoots (the bow is functional, not a
   prop): it engages hostiles at range under the bridge's
   target rules, exactly as 0.1.4 archers did — minus the
   bowlessness.
4. Log the result (time, tier, gear seen) into the install
   record; a bowless cast after this install reopens the
   diagnosis (check the server log for the soft-fail lines
   named in Boot-verify first — they say which half failed).

## Rollback

Stop; delete ExcelSummons-0.1.5.jar; rename the .bak jar back;
start; verify the 0.1.4 enable line. No config, Magic-file, or
data rollback exists for this packet — none was touched.

## Same-pass surfaces owed at install

Guide knowledge + intents: the archer now always takes the
field with its bow and tier helmet (player-facing fact; the
mechanism is bridge trivia — the Guide states the guarantee,
not the diagnosis). What's New BOTH surfaces (website section
AND the in-game ExcelWhatsNew changelog), one player-terms
line: "Archers now always summon with their bow and helmet —
a bowless archer bug is fixed." Rotation tips checked (no
summons tip names archer gear either way — state the check);
in-game menus checked (no menu change in this packet — state
the check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package staged at
~/workspace/summons-015-build/records-package/ and
~/workspace/records-catchup-20261103/packages/excelsummons-0.1.5-20261111/,
and Jester's player-proof result appended to the install
record when it lands.
