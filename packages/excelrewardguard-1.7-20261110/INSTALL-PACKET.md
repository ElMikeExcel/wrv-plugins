# INSTALL PACKET — ExcelRewardGuard 1.7 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the supervisor/failsafe installs; this desk packet
installs nothing, and per the supervisor's instruction nothing
was filed into the MANIFEST by the build lane). Commissioned
from the console-admin coverage audit (Nov 9, 2026) under
Mike's Nov 3, 2026 console-admin rule: every Excel plugin
ships console-runnable admin commands. RewardGuard had NO
command at all through 1.6. 1.7 closes it with the audit's
packet, re-scoped to the live version: the plugin's first
command, one read-only status form. Build record:
`BUILD-RECORD-1.7.md` in this folder; base proof:
`BASE-PROOF-1.6.md`.

## Why

Verified from the 1.6 source (audit + the base proof + this
build): the plugin registers no command and no permission;
an operator who wanted the guard's effective rates or its
intervention counters had to read config.yml and the startup
log and had no way to see the live counters at all. 1.7 adds
`/rewardguard status` — the effective config (Jobs / mcMMO
farm rates, boss band rate + feed state, fall threshold) and
the counters the plugin actually tracks (Jobs / mcMMO
interventions, elite verdicts since enable, fall records
tracked, seal-step state), read-only, console always, in-game
gated by the new `excelrewardguard.admin` (default op). No
behaviour change to any reward path; config.yml is
byte-identical to 1.6 (no new keys, do NOT overwrite the live
config); no data change of any kind.

## The artifact

- ExcelRewardGuard-1.7.jar — 60,694 bytes
  SHA-256 d433749336609985d5c6b1fba8302fa476944517b9ac2e3f177f10b845babf89
  Staged at ~/workspace/rewardguard-17-build/ExcelRewardGuard-1.7.jar
  Deterministic double-build byte-identical.
- **Live-jar gate (what must be live before the swap):**
  the live RewardGuard jar is named
  **`ExcelRewardGuard-identity-OFFLINE.jar`** (the Shared-XP
  bundle's live filename — NOT `ExcelRewardGuard-1.6.jar`) —
  54,649 bytes — SHA-256
  dbf08161e773a25d871bc5365c01eda5ee1df88a46eb31ef58ca71d48b423093,
  plugin.yml version 1.6. If the live jar does not hash to
  this, STOP — an unmatched live jar is a lineage question
  (source recovery first), not a swap.
- Delta vs live 1.6 (class-proven): 24 of 25 classes
  byte-identical; the main class differs only by the command
  handler; 2 classes added (RewardGuardStatus + its Snapshot
  record). The full SharedXpProvenance family and the fork
  mcMMO integration (McMmoVeto) ship byte-identical to live.

## The Shared-XP bundle warning, addressed head-on

The Oct 30, 2026 bundle entry (RewardGuard 1.6 +
ExcelTogether 0.4.3 + the mcMMO WRV fork) warns never to swap
any one artifact alone. That warning guards the bundle's
coordinated IDENTITY feature set: the three artifacts were
installed together because the shared-XP provenance feature
spans all three, and swapping in a DIFFERENT-lineage
RewardGuard (e.g. a 1.5-tree build) would silently drop it.
This packet is not that case, and the packet handles it
explicitly:

- 1.7 is a same-plugin version upgrade OF RewardGuard only,
  built on the base-proven 1.6 tree: the identity surface
  (SharedXpProvenance + all five inner classes, McMmoVeto,
  the fork raw-XP accessors it compiles against) is
  byte-identical to live — proven, not asserted
  (`class-compare-1.7/` in the build folder).
- ExcelTogether and the mcMMO fork are NOT touched, and
  nothing they consume changes: plugin name, main class,
  softdepend set, farmDecision API, config keys and data
  files are all unchanged.
- The swap KEEPS the live filename
  `ExcelRewardGuard-identity-OFFLINE.jar` (step 3 below), so
  the bundle's on-disk shape is preserved exactly — one jar,
  same name, new contents.
- If any boot-verify line about the bundle misbehaves (the
  Shared-XP / Together lines, the fork's behaviour), the
  rollback below restores the exact live 1.6 bytes under the
  same filename.

## The swap (one jar, under its live filename; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelRewardGuard-identity-OFFLINE.jar →
   ExcelRewardGuard-identity-OFFLINE.jar.bak-20261110-pre17
   (adjust the date to the install day; keep alongside — only
   one RewardGuard jar loadable at a time).
3. Upload ExcelRewardGuard-1.7.jar AS
   plugins/ExcelRewardGuard-identity-OFFLINE.jar (rename on
   upload / after upload — the live filename does not change).
   Panel has no checksum field: verify by exact size, 60,694
   bytes, in the file listing, plus the v1.7 boot line below.
4. NO config edit, NO data change, NO other plugin change
   (in particular: ExcelTogether and the mcMMO fork stay
   exactly as they are).
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelRewardGuard v1.7" present.
- The 1.6 enable lines keep their shape: provenance marking
  ON; farm rates Jobs 33% / mcMMO 33%; seal step ON line +
  seal step status line with the live seal count; fall
  detection 50%; audit log line matching the live config;
  "Boss band governor ON" with the live BossTrial feed
  connected; Jobs + mcMMO reward-scaling ACTIVE lines.
- Zero SEVERE attributable to the swap; plugin count
  unchanged from pre-swap; GuildGate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `rewardguard status` → the 6 lines: farm rates 33%/33%
   (or the live configured values), boss band 25% with the
   verdict feed connected, fall threshold 50%, interventions
   counters (0/0/0 at fresh boot), seal step with the live
   seal count + par state, audit/debug/window line. (This
   reply read back in the log is the packet's post-install
   live proof.)
2. `rewardguard` → "Usage: /rewardguard status"; mutates
   nothing.
3. `rewardguard bogus` → the same usage line.

## Player proof

None owed — no player surface changes, and reward shaping
stays silent by doctrine. The console status reply above is
the live exercise.

## Rollback

Stop; delete plugins/ExcelRewardGuard-identity-OFFLINE.jar
(the 1.7 contents); rename
ExcelRewardGuard-identity-OFFLINE.jar.bak-20261110-pre17 →
ExcelRewardGuard-identity-OFFLINE.jar; start; verify
"Enabling ExcelRewardGuard v1.6" and the normal 1.6 lines.
Data and config are compatible in both directions — 1.7
wrote no new state of any kind.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only command — no player
teaching owed; confirm no player-facing RewardGuard-admin
claim exists), What's New BOTH surfaces (website section AND
the in-game ExcelWhatsNew changelog — an operator-note line
that console status for the reward guard now exists is the
only owed text; no player-facing change), rotation tips
checked (no tip change — player surface unchanged; state the
check in the install report), in-game menus checked
(ExcelMenu + ExcelBedrockMenu — no menu change; state the
check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelrewardguard-1.7-20261110/,
and a one-line note closing the audit's ExcelRewardGuard row
(console-admin audit §4) once the live console-status proof
above passes.
