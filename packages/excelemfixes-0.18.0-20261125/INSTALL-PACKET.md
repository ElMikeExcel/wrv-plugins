# INSTALL PACKET — ExcelEMFixes 0.18.0 (quest-chain-clarity: completion signposting)

Lint: 5485 B / max line 84 / non-ASCII 13
Filed: Nov 25, 2026 (recovery lane — the original Nov 25
~2:54 AM build item never started; built, proven and filed
by the recovery lane the same day). Backlog:
P-CHAIN-CLARITY-BUILD. Design source:
~/workspace/quest-guidance-audit-20261125/HOLES.md Group 2,
under Mike's Nov 25 guidance-every-step quest law.

## What ships

ExcelEMFixes 0.17.0 -> 0.18.0. ONE module changes:
quest-chain-clarity (module #22 of 22; module count
unchanged). The module gains its second half:

- COMPLETION SIGNPOSTING: when a quest completes, the
  player is told which quests that completion just
  unlocked — quest name, the NPC who gives it, and the
  place that quest's own text names when it has one:
  "New quest unlocked: 'Population control!' — King
  Nemian gives it."
- WHY (audit evidence): the Nov 25, 2026 full-lifecycle
  guidance audit counted 14 chain starts whose
  prerequisite quest ends 151-531 blocks from the next
  quest's giver, with nothing in the completion text
  pointing onward — a player could finish a quest,
  silently unlock the next one half a map away, and
  never learn it exists.
- Mechanism: the module listens to EliteMobs' own
  QuestCompleteEvent (MONITOR, observe-only) and reads
  the player's completion markers 10 ticks later, once
  EliteMobs' end permissions have landed. Only quests
  this completion directly unblocked are named: a quest
  still missing another link stays silent until its
  turn; a quest already finished once or already being
  carried is not news. Lines are capped by the module's
  existing max-lines knob.
- New config knob (quest-chain-clarity section):
  signposting-enabled (shipped default true). WRV runs
  the shipped default — no WRV delta.
- The module's click explanations are untouched, and
  everything else is byte-identical behavior to 0.17.0:
  the other 21 modules untouched, no data files change,
  no other plugin changes.

## Artifacts (in this packet folder)

- ExcelEMFixes-0.18.0.jar — 511,858 B / SHA-256
  dda8d7a3ffca9ae78befe257a201fabca9b40a71b3eef38336abbfeef5f001c6
  Deterministic: two consecutive builds byte-identical.
  Suites: 1,167 passed / 0 failed (0.17.0's 1,153 + 14 new
  signposting checks on the genuine lighthouse chain).
- deploy-config.yml — 74,747 B / SHA-256
  154815d8a21e3eece3248cd9fd0fdcff33be211f18fb0b0f3409d3a7328b135e
  Strict delta of the 0.17.0 deploy config: +20 lines
  (the signposting comment block + signposting-enabled in
  the quest-chain-clarity section, after max-lines),
  0 lines removed or changed (diff-verified at filing).
- SHA256SUMS — per-file hashes for this folder.
- RECEIPTS.md — live-premise receipts + rig proof evidence.
- SURFACES.md — same-pass surfaces owed at install.

## Live-premise gate (STOP on mismatch)

At install time, live must be ExcelEMFixes 0.17.0:
plugins/ExcelEMFixes-0.17.0.jar = 510,196 B / SHA-256
599a978c0b2fd083554a030e36fbd75fc6d10bee1633cd955b7bb0ceed51d6e7
and plugins/ExcelEMFixes/config.yml = the 0.17.0 deploy
config, 73,626 B / SHA-256
3980a61473150c4f4723bf7f80380e6b7b535be8d44d12e6532a6b99a9f0a7d2
(the 0.17.0 packet's own post-install state — verify from
that packet's INSTALLED.txt / close-out, and re-pull if
anything is ambiguous). If 0.17.0 is not live, STOP:
this packet never installs ahead of it.

## Ordering

CHAINS ON ExcelEMFixes 0.17.0 (packet-0.17.0 in
~/workspace/guidance-builds-recovery-20261125/). Install
0.17.0 first, in the same window if the queue allows.

## Steps

1. Marker discipline per FAILSAFE-FULL-SPEC v2.1 (own v2
   marker; re-check for a foreign marker before Shut Down
   and again before Start).
2. Emptiness: panel/console `list` = 0 players, twice.
3. BACKUP FIRST: snapshot before any change; cap 6, prune
   by CreationTime, never the newest. (If 0.17.0 installed
   in the same window, its snapshot satisfies this leg's
   backup only if taken after 0.17.0's boot verify; when
   in doubt, take another.)
4. Stop the server (panel power stop).
5. Replace plugins/ExcelEMFixes-0.17.0.jar with
   ExcelEMFixes-0.18.0.jar (hash-verify the placed bytes).
   Keep the removed 0.17.0 jar in the window backup.
6. Replace plugins/ExcelEMFixes/config.yml with
   deploy-config.yml (hash-verify). Keep the old config in
   the window backup.
7. Start the server.
8. Boot verify (below). Write INSTALLED.txt in this folder.
9. Same-pass surfaces per SURFACES.md.

## Boot verify

- Console: "ExcelEMFixes 0.18.0 ready" with 22 modules.
- Console: "Quest Chain Clarity census — 112 quests
  indexed" (Primis content; the count follows whatever
  quest content is loaded).
- SEVERE 0 attributable to the swap; guild state
  unchanged; TPS normal after warm-up.

## Rollback

Stop, restore the backed-up ExcelEMFixes-0.17.0.jar and
config.yml, start, verify "ExcelEMFixes 0.17.0 ready".
No data files are touched by this install, so rollback is
a pure jar+config restore.

## Proof owed after install

- Iris live check (staging doctrine): as MikeExcel on the
  PC client, complete a chain quest (The Groves Call via
  King Nemian's dialog) and confirm the unlock line names
  Population Control.
- FEEDBACK-WATCH: any player's next chain completion in
  natural play.
- Upstream notice is NOT part of this packet; the
  quest-chain-clarity module's upstream path follows the
  standing rule (live-tested first) and fires separately.
