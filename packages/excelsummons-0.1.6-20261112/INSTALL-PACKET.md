# INSTALL PACKET — ExcelSummons 0.1.6 (Adopted pets hold no mana + sync clamp)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 12, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Defect of record: the unnumbered item at the end of
`~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
("Summons reserve over-ceiling on re-derivation (Jester report,
Nov 12, 2026)"); binding spec: Mike's ruling the same day
(~3:08 PM CST).

## Why

Jester's mana sat at 35 of 200: his all-adopted menagerie
(5 wolves + 2 cats + 3 familiars + 1 mount, all hand-tamed/bred)
held 85% of his pool, because the 75% ceiling + 20% floor were
enforced at PreCast only while the login/sync re-derivation
applied the raw sum. Mike ruled Nov 12, 2026: **original
(non-summoned) pets hold no mana** — someone took the time to
tame or breed them. 0.1.6 makes adopted-origin records
contribute 0% reserve on every path, and clamps the
re-derivation for cast sets to the same ceiling + floor PreCast
enforces (holds apply in registry order up to the cap; excess
summons stay, holding nothing). Nothing else changes from
0.1.5.

## The artifact (one file, one window)

- ExcelSummons-0.1.6.jar — 66,209 bytes
  SHA-256 08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1
  MD5 937fb7c6498bdfaf26e0a7e523bbaaad
  Build EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a
  Staged at ~/workspace/summons-016-build/ExcelSummons-0.1.6.jar
  (copy at ~/workspace/summons-016-build/ExcelSummons/ExcelSummons-0.1.6.jar)
- **No companion files.** config.yml is byte-identical to the
  installed 0.1.5 config (do NOT replace it), Magic's
  modifiers.yml / messages.yml need no merge (the clamped
  modifier-key set is a subset of the existing 18 templates),
  and summons-data.yml gains no new fields (origin has been
  written since 0.1.4 — rollback is the plain jar swap in both
  directions, no data note).
- **Live-jar gate:** the live jar is ExcelSummons-0.1.5.jar
  (65,253 B, SHA-256 5cdf16a0…f61e — installed Nov 11, 2026).
  Verify before the swap; if it does not hash, STOP — the delta
  was built against exactly this base.
- **Live-premise receipts (G1):** TO BE COMPLETED BY THE
  SUPERVISOR AT FILING — same-day live pulls (rclone, or labeled
  FALLBACK (panel) receipts) for the live jar hash, the live
  ExcelSummons config (reserve-ceiling-percent 75 /
  reserve-floor-percent 20), and the live summons-data.yml
  origin census. This desk packet asserts NO live hashes.

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
2. Rename the live jar to `ExcelSummons-0.1.5.jar.bak-20261112`
   (keep alongside; only one ExcelSummons jar loadable).
3. Upload ExcelSummons-0.1.6.jar as
   plugins/ExcelSummons-0.1.6.jar. Verify by exact size,
   66,209 bytes, in the file listing (and by re-pull hash where
   the route allows: 08da4f10…f70f1).
4. Start the server; run the boot-verify + console smoke.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelSummons v0.1.6" present.
- Enable line names **0.1.6** and build
  **EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a**; registry-restored
  count sane (the live registry carries over untouched).
- Zero SEVERE attributable to the swap; **no "could not be
  applied" reserve warnings**; guildgate LOCKED count
  unchanged; MagicGate stage line unchanged; plugin count
  unchanged from pre-swap.
- Expected, NOT a failure: at the first login sync after
  install, players whose adopted pets held reserve under 0.1.5
  get those modifiers removed and the mana back. That is the
  fix landing.

## Console smoke (from the Shockbyte console, post-boot)

1. `summons admin list NobodyHereXYZ` → the unknown-name line
   (surface unchanged from 0.1.5).
2. `summons list` → "The registry holds N summon(s)."
   (unchanged); `summons admin reload` unchanged.

## Player proof (the defect's named proof — Jester)

One session, whenever Jester is next on:

1. Jester logs in with his menagerie as-is (5 wolves, 2 cats,
   3 familiars, mount — all adopted): his usable mana is back
   at the **full 200 pool** (was ~35 usable / 85% held), and
   `/summons list` shows NO "holds N% mana" clause on any
   adopted pet and no "Mana held by your summons" total line.
2. His animals are all still there and still registered
   (nothing dismissed, nothing trimmed).
3. If he casts a combat/companion summon in the same session,
   THAT summon holds its normal rate (cast holds are
   unchanged) and releases it on despawn as before.
4. Log the result (time, usable mana seen, list output) into
   the install record; mana still held by adopted pets after
   this install reopens the defect (check the server log's
   sync behavior first).

## Rollback

Stop; delete ExcelSummons-0.1.6.jar; rename the .bak jar back;
start; verify the 0.1.5 enable line. No config, Magic-file, or
data rollback exists for this packet — none was touched.
(Rollback restores the 0.1.5 behavior: adopted pets would hold
reserve again at the next sync — name that in the rollback
report if it ever happens.)

## Same-pass surfaces owed at install

Guide knowledge + intents: tamed/bred pets (adopted) no longer
hold mana — only summoned (cast) creatures do, and summoned
holds can never take more than 75% of the pool or leave less
than 20% usable (player-facing facts; the mechanism is bridge
trivia — the Guide states the rules, not the diagnosis).
What's New BOTH surfaces (website section AND the in-game
ExcelWhatsNew changelog), one player-terms line: "Tamed and
bred pets no longer hold your mana — only summoned creatures
do." Rotation tips checked (no summons tip names pet reserve
either way — state the check); in-game menus checked (no menu
change in this packet — state the check in the install
report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), FEEDBACK-TRACKER row for this bug
(tester = Jester, per the backlog item), records package
staged at ~/workspace/summons-016-build/records-package/, and
Jester's player-proof result appended to the install record
when it lands.
