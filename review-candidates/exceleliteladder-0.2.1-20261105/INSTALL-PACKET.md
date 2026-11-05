# INSTALL PACKET — ExcelEliteLadder 0.2.1 BUNDLE (retune + elite speed) — Nov 5, 2026

(Canonical copy: `~/workspace/eliteladder-retune-20261105/INSTALL-PACKET-BUNDLE.md` — identical content.)

Built by the 0.2.1 build (`~/workspace/eliteladder-speed-build/`); jar of
record `ExcelEliteLadder-0.2.1.jar`, **47,232 B**, SHA-256
`d15c5df2cbb2e8c7b6b15ba6b7543b1b0bac59394463416f4a43cc2593f03dd4`
(double-build byte-identical; 272 plain-JVM checks, 0 failed; rig boots
SEVERE 0 — TEST-REPORT.md in the build folder).

**ONE gated window, three file swaps + one restart.** Fully gated:
BACKUP-FIRST + verified-empty server. This is a jar + config-table +
foreign-config alignment bundle — NOT a text live-edit.

What lands:

1. **Jar** 0.2.0 → 0.2.1 (elite speed buff for natural elites + the
   retuned built-in default table).
2. **ladder.yml** (live `plugins/ExcelEliteLadder/ladder.yml`) → the
   retuned table (spawn 0.10/0.12/0.14/0.17/0.17; scaled-to-elite
   0.50/0.45/0.40/0.35/0.35; scaled-to-player 1.58/1.71/1.85/1.94/1.94).
3. **MobCombatSettings.yml hand-alignment** (EliteMobs file) from the OLD
   S3 row to the NEW S3 row — same window, no exceptions (see §4).

No config.yml swap is needed: the live config.yml lacks the new
`elite-speed:` section, and the plugin applies the documented defaults
(enabled, amplifier 0, permanent) when it is absent. (Swapping in the
0.2.1 config.yml is optional cosmetics — values are identical.)

## 1. Pre-flight gates — any mismatch = STOP, do not improvise

- Server verified EMPTY twice (`list` = 0) before Shut Down.
- Exactly ONE active ExcelEliteLadder jar in `plugins/`, and it hashes
  `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`
  (0.2.0, 40,475 B). Hold it aside as the rollback jar
  (`ExcelEliteLadder-0.2.0.jar.held-20261105-pre021`).
- Live `plugins/ExcelEliteLadder/ladder.yml` = the 0.2.0 default values:
  step spawn 0.07 / 0.09 / 0.11 / 0.11 / 0.11 and scaled-to-player
  1.75 / 1.90 / 2.05 / 2.35 / 2.35. (Mike never hand-tuned it; if it
  differs, STOP — the table-swap step assumes the unedited default.)
- Live `plugins/EliteMobs/settings/MobCombatSettings.yml` = OLD S3-norm,
  the six keys exactly:
  `eliteMobsSpawnPercentage: 0.09`, `naturalEliteMobsLevelCap: 120`,
  `normalizedDamageToEliteMultiplier: 0.5`,
  `normalizedDamageToPlayerMultiplier: 2.0`,
  `scaledDamageToEliteMultiplier: 0.5`,
  `scaledDamageToPlayerMultiplier: 2.05`.
- `plugins/ExcelEliteLadder/state.yml` shows `appliedStep: 3`,
  `pendingStep:` empty.

## 2. Backup-first

1. **Shockbyte backup** (fresh, for this window), under the cap-at-6
   house rule: prune the OLDEST manual backup(s) first so the
   post-creation total stays ≤ 6; never prune the Daily Backup's newest
   copy or the two newest manual restore points; name the prunee in the
   install report. Suggested name: `EliteLadder pre-0.2.1-bundle 20261105`.
2. **`.bak` copies** (in-place, the per-file net):
   - `plugins/ExcelEliteLadder/ladder.yml` → `ladder.yml.bak-20261105-pre021`
   - `plugins/EliteMobs/settings/MobCombatSettings.yml` →
     `MobCombatSettings.yml.bak-20261105-pre021`
   - (the live jar is preserved via the `.held` rename in §1.)

## 3. Sequence

1. Shut Down (server verified empty).
2. Swap the jar: held-rename the 0.2.0 jar per §1; upload
   `ExcelEliteLadder-0.2.1.jar`; verify the uploaded jar's hash =
   `d15c5df2…` before Start.
3. Start. Verify the boot (proof lines in §5): `Enabling
   ExcelEliteLadder v0.2.1`, the enable line ends `…, elite speed: ON
   (amplifier 0, duration permanent)` and reports
   `applied step S3` with the ladder table source noted as the LIVE
   ladder.yml; SEVERE 0; server ONLINE.
   - Expected note: the applied step stays S3 but the live MobCombat
     file no longer matches any table row (it is the OLD S3 against the
     NEW table) — that is the planned intermediate state; §4 resolves
     it in this same window.
4. Swap `plugins/ExcelEliteLadder/ladder.yml` with the packet's retuned
   `ladder.yml` (from `~/workspace/eliteladder-retune-20261105/ladder.yml`,
   the validated candidate — byte-identical to the jar's bundled
   resource).
5. Console: `eliteladder reload` → expect `Ladder table reloaded from
   ladder.yml and is now live (S5: spawn 0.17, cap 150, toElite 0.35,
   toPlayer 1.94, dungeon toElite 0.5, dungeon toPlayer 2.0).` and a
   follow-on line `Pending: align MobCombatSettings.yml to the next
   ladder step (…)` — that pending line is EXPECTED here; §4 clears it.
6. **Hand-align MobCombatSettings.yml** (EliteMobs file), editing ONLY
   these three lines, in place:
   - `eliteMobsSpawnPercentage: 0.09` → `eliteMobsSpawnPercentage: 0.14`
   - `scaledDamageToEliteMultiplier: 0.5` → `scaledDamageToEliteMultiplier: 0.4`
   - `scaledDamageToPlayerMultiplier: 2.05` → `scaledDamageToPlayerMultiplier: 1.85`

   The other three keys stay untouched (`naturalEliteMobsLevelCap: 120`,
   `normalizedDamageToEliteMultiplier: 0.5`,
   `normalizedDamageToPlayerMultiplier: 2.0` — note the normalized
   toElite key ALSO reads 0.5; edit the SCALED line only).
7. Console: `em reload` (EliteMobs picks up the aligned values).
8. Console: `eliteladder status` — the all-rows proof (§5).
9. Report: backup name + prunee named; the three .bak/held artifacts;
   boot lines quoted; status quoted.

## 4. Why the alignment is NOT optional

The plugin only ever applies UP-steps. After the table swap, live
(appliedStep 3, OLD values) vs new S3 row differ — the plugin reads that
as drift. If the window ended before step 6, the future seal-4 apply
would REFUSE on driftCheck (`live MobCombatSettings.yml matches NO
ladder step`) and the ladder would be stuck until someone hand-aligns
it anyway. The alignment is part of this bundle, in this window.

Forward note (unchanged from the 0.2.0 packet): the corner case where a
future step's file would exactly equal the PREVIOUS step's file can
never Curry — adjacent steps always differ (installer note, not a
runtime state). And: **retuning the peak means editing S4 AND S5
together** (they are identical rows; the validator refuses an
inconsistent peak).

## 5. Boot / status proof lines (verbatim targets)

- `Enabling ExcelEliteLadder v0.2.1`
- Enable line contains `applied step S3` AND ends with
  `elite speed: ON (amplifier 0, duration permanent)`
- After §3.5: `Ladder table reloaded from ladder.yml and is now live
  (S5: spawn 0.17, cap 150, toElite 0.35, toPlayer 1.94, dungeon
  toElite 0.5, dungeon toPlayer 2.0).`
- After §3.8, `/eliteladder status`:
  - `Applied step: S3`
  - all five rows read the retuned values (S1 0.10/120/0.50/1.58 …
    S4+S5 0.17/150/0.35/1.94)
  - `Live file: matches S3-norm exactly (spawn 0.14, cap 120, toElite
    0.4, toPlayer 1.85, dungeon toElite 0.5, dungeon toPlayer 2.0)`
  - `Pending: none`
  - `Elite speed: on (Speed I, permanent)`
- SEVERE count 0 across the boot; guild line still `LOCKED 3 of 5`.

## 6. Post-install player proof (owed after the window)

- The first NATURAL elite spawned after the window: console log shows
  `Elite speed applied (spawn): <TYPE> at <world> <x, y, z> — on (Speed
  I, permanent).` and the mob visibly moves ~20% faster. (Console-side:
  any player can report "that elite felt quick"; the log line is the
  hard proof.)
- NOT a failure: a summoned/arena/custom elite shows NO buff and NO log
  line — non-natural elites are excluded by design (and EM's own
  movement-speed power still applies to bosses independently).
- Elite damage/tank feel at step 3 rides the same session: damage dealt
  to players 2.05 → 1.85, damage taken by elites 0.50 → 0.40 (elites
  ~25% longer to kill), natural elite spawn chance 9% → 14%.

## 7. Same-pass surfaces (parent's close-out lane — player-facing change: YES)

- What's New (registry → in-game changelog + website section): elites
  retuned (damage down a step, tankier, more of them) + natural elites
  now move a little faster (Speed I). Self-contained how-to wording.
- Website plugin row: ExcelEliteLadder 0.2.0 → 0.2.1 (+ ai-knowledge.json
  regen + Guide knowledge regen from it).
- Guide /ask retest: the EliteLadder ConfigKnowledge desync noted after
  PR #165 resolves when the Guide next regenerates from the updated
  canon (the Landmarks text landing may already carry the cure — verify
  against the 0.2.1 canon, not the 0.2.0 text).
- Rotation tips: checked — no elite-damage tip exists to correct; no
  tip change owed.
- Route Sheet WRV row + daily log + records package (staged locally at
  `~/workspace/records-catchup-20261103/packages/exceleliteladder-0.2.1-20261105/`;
  publication is the standing pre-approved records workflow).

## 8. Rollback

Restore, in one empty-server window: the held 0.2.0 jar (swap back),
`ladder.yml` from `ladder.yml.bak-20261105-pre021`,
`MobCombatSettings.yml` from `MobCombatSettings.yml.bak-20261105-pre021`;
restart; `em reload` not needed after a restart (EM reads the file at
boot) — verify `/eliteladder status` shows `Applied step: S3`,
`Live file: matches S3-norm exactly (spawn 0.09, …, toPlayer 2.05, …)`,
`Pending: none`. Nothing player-owned is touched by either direction:
the speed effect lives on mobs, never in player data.
