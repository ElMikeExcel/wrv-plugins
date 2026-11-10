# ExcelMagicGate 0.3.0 — Rig Verdict

Rig: Paper 26.2 (port 25599), Magic 11.2.5, ExcelWandBars 0.3.11, ExcelGuildGate
0.1.0, LuckPerms; stage 4 applied (ceiling master). ExcelPouch absent, so the
pouch case was proven with the pouch-equivalent serialized-wand state (the same
equivalence the 0.2.0 record accepted). Fake-player driver = FloorRigDriver
(0.2.0 rig driver + lore/castkey/hurt/food/place/spawnzombie/earned/stepup
subcommands). Three boots: capability + credit suite, demotion suite,
persistence. Logs: rig/server/logs/boot1-latest.log, boot2-latest.log,
latest.log (boot 3). Zero SEVERE lines and zero ExcelMagicGate WARN/ERROR
across all three boots.

## HEADLINE — the demotion seam (DESIGN §6, the build's hard gate): PROVEN

The reflective seam — activate wand, per step setPath(lower) on wand + active
properties + mage class, setManaMax/setManaRegeneration written EXPLICITLY on
wand + active properties, mana clamped, path AND frame re-read from the wand —
works end-to-end on the real Magic 11.2.5:

- RigDemote: master -> student (2 steps) on wand activation. Log: "Rank
  reconcile: RigDemote demoted master -> student (2 step(s), wand activation,
  earned target at stage 4) — pool 100, regen 15/s, verified on the wand.
  Credited casts 150; spells untouched." Post-read: path=student, 100/15,
  hotbar_count=2, bound=true, template unchanged, spell list byte-identical
  (18 spells, same keys/tiers as at master).
- .RigBedrock30 (dotted Bedrock name): master -> beginner (3 steps) on
  activation. Post-read: path=beginner, 50/5, hotbar_count=1, spells identical.
- RigPouch30: wand parked in its serialized (pouch-equivalent) state across a
  full sweep — NOT touched while parked (deferred, pending count held); on
  unpark + activation: master -> student (2 steps), verified.
- Persistence: after a clean restart, Magic's saved mage files record
  path "student" / mana_max 100.0 (RigDemote, RigPouch30) and "beginner" /
  50.0 (.RigBedrock30); wands return at the demoted rank with no re-demotion
  needed (boot 3, AT_TARGET).
- WandBars re-read at the shrunken hotbar (frig openwand at hotbar_count=2):
  opened without exception.
- Demote-under-fire: N/A by design (demotion gated on wand activation / quiet
  reconcile legs, never mid-cast) — see DESIGN §6 step 6.

The 0.2.0 framed-upgrade quirk does NOT apply downward: demote re-reads showed
the exact target frame every time (200/35 -> 100/15 -> 50/5 all exact).

## Step-up under earning (0.2.1 behavior, retargeted)

- beginner -> master in 3 upgrade() calls for a 2,500-credit player at ceiling
  master, verified on the wand; dotted-name player stepped on the join leg
  after the not-live deferral; the known Magic post-apply notification NPEs
  appeared and are cosmetic (every step judged by re-read, as in 0.2.0).

## Credit feed (DESIGN §3) — live evidence

Rig earned rows seeded for step-up/demote players; RigCredit started with NO
row and was fallback-seeded from the live Mage API at 0. Final RigCredit row:
credited_total 4 — missile: 2, boon: 1, heal: 1.

- boon|2 (buff, targeted zombie) credited. missile|3 credited twice (one cast
  confirmed targeted; the second cast's projectile was still in flight at
  CastEvent and credited via the context's targeted entity — strict gate held).
- backflip (excluded family): denied-excluded. Full-health heal|2: denied at
  Magic level (result not success) — zero credit. Hurt heal|2 (health 12):
  credited (counter 4 -> 5).
- Threshold crossing: RigCross at 99 casts boon|2 -> "RigCross earned rank
  student (100 credited casts)" -> immediate reconcile beginner -> student,
  pool 100/15 verified; earned.yml persisted credited_total 100 after restart.
- Dedupe: frost|2 fired a base event + a failing sub-context echo — exactly
  ONE credit landed (counters credited +1, denied-not-success +1).

## Wand lore (DESIGN §5) — property seam VERIFIED

Boot log: "Wand lore: property seam VERIFIED — Magic renders the rank block
from the wand lore property on every regeneration." The block survived step-ups,
demotions, casts, and a restart, rendered by Magic itself each time:

- Master: "Rank: Master (ceiling: Master)" / "Credited casts: 2,500" /
  "Top rank earned — Master of the valley's magic"
- After demotion: "Rank: Student (ceiling: Master)" / "Credited casts: 150" /
  "Next: Apprentice — 250 to go"
- After restart: "Credited casts: 151" / "Next: Apprentice — 249 to go"

## Auto-upgrade guard (DESIGN §9.3 / §11 Q6) — PathUpgradeEvent leg proven

Driver bypassed MagicGate and upgraded RigDemote's wand directly via the Magic
API (student -> apprentice). The credit hook observed the PathUpgradeEvent
("Magic auto-upgraded RigDemote to apprentice, above their earned target
student — corrective reconcile scheduled") and the corrective demote landed in
the same second (source "auto-upgrade guard"), verified 100/15 at +3 s. The
60 s sweep remains as backstop (not needed here).

## Open-question evidence gathered for the packet (§11)

- Q3 (tier variants after demotion): missile|3 — an apprentice-band variant on
  the wand — FAILED to cast at Magic level (result not success) twice for the
  demoted student wand at full mana with a live target, while the same spell
  credited twice in boot 1 for a path-UNSET beginner wand. Reading: Magic
  enforces upgrade_required_path at cast time once the wand carries a path;
  an unset path skips the check. Practical consequence for Mike: after the
  placement demotions, already-learned tier variants above a player's earned
  rank will not cast until the rank is re-earned. (One batch of three casts —
  frost|2/arrow|3/heal|2 — could not be fully attributed per-cast; the two
  isolated missile|3 failures + boot-1 contrast are the load-bearing evidence.)
- Q5 (feed's heal signal): the food gate accepts FoodLevelChangeEvent
  increases as well as CUSTOM regains; the rig's fake-player feed casts could
  not isolate which signal Magic's feed produces on a real client (driver casts
  resolve through the console path). Marked rig-inconclusive; the gate credits
  on EITHER signal, so the shipped behavior is safe both ways. Feed family also
  sits behind the 50/day cap.
- Renewal (heal|2, heal-over-time): credited once when hurt (boot 1) and
  denied-heal-not-observed once (boot 2) — its first regain tick can land
  after the CastEvent window closes. Instant heals (mend, the family's main
  spell) are unaffected; Renewal may under-credit occasionally. Recorded as a
  known softness, not a blocker: the daily cap never comes into play for it,
  and the denial is conservative (no false credit).

## Known rig-only noise (not product defects)

- ExcelWandBars 0.3.11 in this rig throws NoClassDefFoundError
  (wrv/magic/level/LearnedLevelResolver$Request, 14 occurrences) in its
  afterCast task and boots with "Rank-up by casting off
  (EffectObserver$Sink missing)" — the old rig's WandBars build lacks its
  wrv.magic.* helper classes on this classpath. Pre-existing in the 0.2.0 rig
  lineage, unrelated to ExcelMagicGate; MagicGate never touches those classes.
- Fake players lose their Bukkit inventory across restarts (driver artifact);
  Magic's mage data persists, and wands were returned with Magic's own
  /wandp <player> restore console path — the same path support would use live.
- Zombie target suffocated in a wall once (spawn placement), re-spawned.
