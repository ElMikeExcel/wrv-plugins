# BUILD RECORD — guidance builds recovery lane (Nov 25, 2026)

Items recovered: P-GLOW-R1-BUILD (objective-target glow) and
P-CHAIN-CLARITY-BUILD (quest-chain-clarity completion
signposting). Both dispatched Nov 25 ~2:19–2:54 AM CT; both
sat untouched from 2:53 AM until this lane picked them up
at ~11:07 AM CT. End state: BOTH BUILT, PROVEN, AND FILED.

## 1. Dead-lane verification (Step 1)

- Four overnight trees existed: excelemfixes-glow-r1-20261125,
  excelemfixes-beacon-r2-20261125, excelemfixes-dialog-fix-20261125
  (a combined R1+R2 attempt whose BUILD-RECORD checklist —
  diff review, suites, deterministic build, rig proof,
  filing — was entirely unchecked), and
  excelemfixes-questdest-0190-20261125 (a separate item,
  out of this lane's scope, left untouched).
- No file in any of the four trees was modified after
  08:53:30 UTC Nov 25 (2:53 AM CT); checked at 17:07 UTC —
  8+ hours of silence. No java/gradle processes running.
  subagent.list showed no active builder lanes. The
  MANIFEST had no ExcelEMFixes PENDING entries — none of
  the stalled lanes filed anything.
- Version chaos among the dead trees (none filed, so no
  conflict): glow-r1 = 0.17.0, beacon-r2 mid-bump to
  0.18.0 on the same jar, dialog-fix = a different 0.17.0
  jar with its own unverified integration edits.
  DECISION: do not adopt the dialog-fix tree. This lane
  built from the pristine 0.16.0 base plus the glow-r1
  lane's R1 files, every diff reviewed in full by this
  lane (verdict: clean, 10 files).

## 2. ExcelEMFixes 0.17.0 — objective-target glow (R1)

Base gate: the pristine 0.16.0 tree
(build-root-base0160) rebuilt byte-identical to the live
jar (506,738 B / ae6b53a6...), so the tree is the live
source, not a cousin.

Change (quest-giver-glow module only): a third per-player
glow state OBJECTIVE for the target NPC of a carried
quest's unfinished DIALOG objective; the glow NPC universe
extended to dialog targets (118 glow-capable on the rig vs
76 in 0.16.0); precedence TURNIN > OBJECTIVE > ACCEPT; new
knobs objective-enabled (default true) and objective-color
(default aqua), each with the Jester/Nemian reason in the
config comments and README. Zero WRV hardcoding; WRV runs
the shipped defaults.

- Suites: 1,153 passed / 0 failed (1,142 + 11 new).
- Jar: 510,196 B / SHA-256
  599a978c0b2fd083554a030e36fbd75fc6d10bee1633cd955b7bb0ceed51d6e7
  Deterministic double-build identical — and byte-identical
  to the dead glow-r1 lane's own jar, independently
  confirming its R1 tree was complete.
- Rig proof (full Primis rig, Nov 25, 2026): boot clean,
  22 modules, SEVERE 0, census 73 givers / 118 glow-capable.
  Baseline: non-carrier sees no glow on King Nemian.
  Staged carry of The Groves Call (server-verified:
  DialogObjective completed=false, npc=Nemian) -> carrier's
  packets show Nemian's entity glow flag set + qgobjective
  team join; server debug "GlowBot <-
  primis_groves_nemus_leader.yml = OBJECTIVE". Second bot
  beside the first: no glow metadata for Nemian. Toggle
  control: objective-enabled false + reload -> glow flag
  cleared while the quest stayed carried.

## 3. ExcelEMFixes 0.18.0 — completion signposting

Built on the verified 0.17.0 tree in the same build-root.
Change (quest-chain-clarity module only): on EliteMobs'
QuestCompleteEvent (MONITOR, observe-only), 10 ticks after
completion (end permissions settled), the player is told
which quests that completion directly unlocked — name,
giver, place — via QuestChainCore.unlockedBy (direct
dependents only; still-missing-another-link, already-
completed, and carried quests stay silent; max-lines cap).
New knob signposting-enabled (default true) with the
guidance-audit reason (14 chain starts, prerequisites
ending 151–531 blocks from the next giver) in config
comments and README.

- Suites: 1,167 passed / 0 failed (1,153 + 14 new, on the
  genuine lighthouse chain fixtures).
- Jar: 511,858 B / SHA-256
  dda8d7a3ffca9ae78befe257a201fabca9b40a71b3eef38336abbfeef5f001c6
  Deterministic double-build identical.
- Rig proof: boot clean, census 112 quests indexed. ON:
  a bot completed The Groves Call through EliteMobs' own
  flow (dialog click + /em quest complete) and received
  "New quest unlocked: 'Population control!' — King
  Nemian gives it." plus a second genuine dependent,
  "New quest unlocked: 'Acrobatics' — Posuere Hominem at
  the course end gives it." OFF (A/B control):
  signposting-enabled false + reload, Feeding the Sick
  completed the same way — completion + rewards arrived,
  zero unlock lines.

## 4. Filing (Step 3 — D212 finish line)

Fresh live pull Nov 25, 2026, 11:30 AM CT (ssh-pc.sh scp):
live jar 506,738 B / ae6b53a6...; live config 72,329 B /
ded17ff9... (receipts/ in this folder). The 69,861 B
config figure in an older chain record is superseded —
the fresh pull decides; live matches the 0.16.0 packet's
deploy-config hash.

- packet-0.17.0/: INSTALL-PACKET.md (Lint: 5,190 B / max
  line 102 / non-ASCII 12), the jar, deploy-config.yml
  (73,626 B / 3980a614... = live + 23 lines, 0 removed,
  diff-verified), RECEIPTS.md, SURFACES.md, SHA256SUMS,
  empty INSTALLED.txt.
- packet-0.18.0/: INSTALL-PACKET.md (Lint: 5,485 B / max
  line 84 / non-ASCII 13), the jar, deploy-config.yml
  (74,747 B / 154815d8... = 0.17.0 deploy + 20 lines,
  0 removed), RECEIPTS.md, SURFACES.md, SHA256SUMS,
  empty INSTALLED.txt. Its gate CHAINS ON 0.17.0 with a
  STOP if 0.17.0 is not live.
- MANIFEST: two pointer stubs inserted at the top of
  ## PENDING entries in ~/workspace/wrv-install-queue/MANIFEST.md
  (lines 35 and 43 at filing; read back and verified).

## 5. Finish-and-fire (Step 4 — D228)

Five drops written via python json.dump (forward-slash
paths) to ~/hooks/state/followon-drops/:
drop-1795627966662.json (0.17.0 surfaces at install),
drop-1795627966663.json (0.18.0 surfaces at install),
drop-1795627966664.json (upstream glow notice, gated on
live proof), drop-1795627966665.json (upstream signposting
notice, gated on live proof), drop-1795627966666.json
(R2(b) beacon dialog-destination derivation — still owed;
unverified dead-lane trees named; needs its own
verify/build/file lane as the next version after 0.18.0).

Backlog: P-GLOW-BEACON-DIALOG-FIX-20261125 in
parent-backlog.md updated with a dated RECOVERY note
(R1 filed as 0.17.0; R2(b) still owed). No backlog row
existed for the signposting item under any name this lane
could find; its record is this file + its packet + the
MANIFEST stub.

## What is NOT done (by design)

- Neither version is installed — filing is this lane's
  finish line; the install failsafe takes the PENDING
  stubs from here.
- R2(b) is not built (see drop ...6666).
- Upstream notices wait on live proof (drops ...664/665).
