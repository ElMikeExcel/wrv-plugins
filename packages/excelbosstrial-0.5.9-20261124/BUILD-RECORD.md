# ExcelBossTrial 0.5.9 — join-armed returns — BUILD RECORD

Built Nov 24, 2026 by the arm-return lane. Implements Mike's
direction of Nov 24, 2026, 11:42 AM CT (Minecraft chat): when a
job can be done by a plugin listening to the server's own events,
build it into the plugin — Paper already broadcasts every player
join to every plugin, so no fleet watcher should burn tokens
polling for a login. First use: Jester_Turtle's Alanus' Dagger
return (see ~/workspace/jester-dagger-give-20261124/RECORD.md).

## What it does

- `/bosstrial armreturn <player> emloot <loot-file>` — stores a
  PENDING RETURN (data only; the player may be offline). The
  loot file is validated against EliteMobs' registry AT ARM
  TIME (a getCustomItem probe that builds nothing): unknown
  file, EliteMobs absent, or a dead build seam = clean refusal,
  nothing stored. One arm per (player, loot-file) pair; a
  second arm of the same pair is a CLEAN REFUSAL ("disarm it
  first if it should change") — chosen over silent replace so
  an arm is never quietly overwritten.
- `/bosstrial disarmreturn <player> [loot-file]` — takes one
  named arm back, or every arm the player holds; a no-match
  disarm is reported plainly and changes nothing.
- `/bosstrial listreturns` — every waiting arm with player,
  UUID, loot file, armed-by and timestamp.
- All three sit in grantclaim's posture family: the
  `excelbosstrial.admin` permission via the existing
  CommandAccess gate (console passes), and ONE config toggle,
  `commands.armreturn-enabled` (default true), gates the whole
  family including the join listener — the same shape as
  `commands.grantclaim-enabled`.
- At the armed player's next join, a PlayerJoinEvent listener
  (NORMAL priority) runs BEFORE TeamClaims' own MONITOR join
  handler schedules the ~3 s auto-presentation, so a granted
  claim is already in the ledger when the chest presents:
  1. LEDGER CHECK FIRST: if the player already holds an
     UNCLAIMED console-grant claim whose payload carries this
     item's `elitemobs:custom_item_id`, an earlier grant
     landed and only its disarm was lost — the arm is removed,
     NO second grant is made, `ARMED-RETURN RECOVERED` is
     logged, and the player is still told their item waits.
  2. Otherwise the stack is built via the 0.5.8 EmLootGateway
     WITH THE PLAYER PRESENT — the construction under which
     EliteMobs applies its Soulbind, the whole reason a return
     must land online — and granted through the existing
     TeamClaims.grantClaim path. ONLY on a successful grant is
     the arm removed.
  3. On ANY failure (EM unavailable, unknown file, build
     failure, store failure) the arm is KEPT for the next
     join, an ERROR line `ARMED-RETURN FAILED for ... the arm
     is KEPT and will retry at the next join.` is logged, and
     the player is NOT sent a success message that didn't
     happen.
- On success the player gets an on-screen title + subtitle AND
  a chat line naming the returned item, from config templates
  (`messages.return-title` / `return-subtitle` / `return-chat`,
  `{item}` = the built stack's real display name). Shipped
  defaults: title "&6Your {item} is back", subtitle "&eIt is
  waiting in your boss rewards", chat "&eA return was waiting
  for you: &f{item}&e is in your &f/bossrewards&e - open it to
  collect."
- Arm store: `armed-returns.dat` in the plugin data folder —
  BossTrial-owned, single-writer (all access synchronized in
  the ArmedReturns service), written with the house
  AtomicStateFile primitive (temp + fsync + atomic move).
  Format: comment header + one tab-separated line per arm
  (player UUID, player name, loot file, armed-by, epoch-ms) +
  a `sha256:<hex>` trailer over every preceding byte — the
  same integrity posture as team-rewards.dat's checksum. A
  file that fails ANY check (checksum, malformed line,
  duplicate pair) is refused whole: SEVERE
  `ARMED-RETURN STORE UNREADABLE (...)`, the bad file is
  renamed to `armed-returns.dat.corrupt-<millis>`, and the
  service starts empty rather than half-reading owed returns.
- README and the config comments carry the REASON in plain
  language (Mike's Nov 24, 2026 ruling), per the standing
  module-documentation rule.

## Design choices made in-lane (and why)

1. **Duplicate arm = clean refusal** (not replace): an arm is
   an owed item; silently replacing one could swap what a
   player is owed without a trace. Disarm-then-arm is two
   explicit, audit-logged steps.
2. **Double-grant defense is belt and braces**: disarm only
   after a successful grant (the grant is one synchronized
   ledger store write), PLUS the join-time ledger check for an
   unclaimed console-grant claim of the same item. A crash in
   the grant→disarm gap therefore recovers instead of
   duplicating (proven on the rig, case R below). DELIVERED
   claims and fight-drop items deliberately do NOT count as a
   return's twin — only an unclaimed console grant does.
3. **Arm-time EM probe**: arming validates the loot file
   against the registry without building, so a bad arm can
   never be stored and fail silently at a future login.
4. **Corrupt arm store = rename aside + start empty + SEVERE**,
   mirroring how the claims ledger treats integrity failure:
   never half-read a store that decides who is owed an item.

## The rig finding that matters for live (EM permission gate)

EliteMobs custom items can carry a `permission:` line — the
dagger's is `elitequest.primis_ice_village_alanus_dagger.yml`
— and EM's construction builds NOTHING for a recipient who
lacks it (build returns no stack; EM's own `/em loot give`
behaves the same). The 0.5.8 rig proof rode on RigTester's OP
status (level 4 = every permission). A fresh unprivileged rig
account reproduced BUILD_FAILED until it was opped, after
which the identical construction succeeded. On LIVE this is
the correct shape, not a blocker: Jester earned the dagger
through the Primis quest line, so he holds that permission,
and his join-time build will succeed. If a future return ever
targets a player lacking the item's permission, 0.5.9's
failure path is the safe one: arm kept, `ARMED-RETURN FAILED`
in the log, no false success — an admin grants the permission
or delivers another way, and the arm retries meanwhile.

## Base gate

The unmodified 0.5.8 tree (copied from
~/workspace/bosstrial-emloot-20261124/bosstrial/) rebuilt the
installed 0.5.8 jar BYTE-IDENTICALLY before any edit:
481,720 bytes, sha256
119ecbad189376e12f235c6c9c2a381033579ce0dfbc0497c4e3aa91c89f995b,
suite green at the 0.5.8 counts. (The tree's package.sh
default output name still says 0.5.8; the lane always passed
OUTPUT_JAR explicitly — cosmetic, noted for the next lane.)

## Suite

House tally (same counting convention as the 0.5.8 record):
**2,935 checks, 0 failed** — the 0.5.8 suite's 2,890 plus 45
new: ArmedReturnStoreTest 28 (round trip, checksum tamper,
checksum-less, malformed-under-valid-checksum, duplicate pair
in file, duplicate add refusal, removes, field discipline),
ArmReturnPlanTest 16 (arm shape, usage/mode/loot-file
refusals, both disarm shapes), PluginYmlParseTest +1 (the
0.5.9 description sentence; its version check moved to 0.5.9).
BuildInfoStampTest green on the new stamp. The ClaimSession
and InventoryPlan bulk harnesses stay outside the check tally
by the house convention, as in every prior record.

## Deterministic build

package.sh run twice into separate outputs: BYTE-IDENTICAL.
Candidate: **502,634 bytes, sha256
dae545f1a4770b3d2172d85aa81c89fd5c471eca28c6fffa135ed48b1be18a4f**,
build EXC-BOSSTRIAL-0.5.9-WRV-20261124-01-e8be92 (fresh tag;
0.5.8's 2c5e05 and all earlier tags never reused), stamped in
all three homes (BuildInfo, excel-build.txt, plugin.yml).
Jar-entry diff vs the 0.5.8 candidate: exactly the expected
set — ExcelBossTrial (+inner classes, +TargetRef),
TeamClaims, EmLootGateway, BuildInfo, config.yml, plugin.yml,
excel-build.txt changed; ArmedReturns, ArmedReturnStore(+Arm),
ArmReturnPlan(+5) new; nothing else.

## Rig proof (scratch rig copied from the 0.5.8 lane; never live)

- **A — arm validations (EM present)**: listreturns empty;
  unknown loot file refused ("No such EliteMobs loot file"),
  nothing stored; arm stored for a never-joined OFFLINE
  account; duplicate arm refused; signature-mode arm refused
  with the usage line; disarm named (1 removed), disarm
  no-match (plain 0-removed), re-arm; listreturns accurate;
  armed-returns.dat on disk in the designed format with a
  valid sha256 trailer.
- **Persistence**: full server restart — both arms survived
  with identical timestamps.
- **B — the join grant (the point of the build)**: the armed
  account joined → server logged `ARMED-RETURN GRANTED: ...
  claim console-grant:20261124-180608-1/...`; the bot received
  the TITLE "Your Alanus' Dagger is back" + subtitle "It is
  waiting in your boss rewards" and the chat line naming the
  item; the claims chest auto-presented at ~3 s WITH the new
  claim; the bot claimed it ("Reward claimed.") and the dagger
  (STONE_SWORD "Alanus' Dagger") was in its inventory. Ledger
  read-back: exactly one claim for the account, DELIVERED.
  Payload scan: `elitemobs:soulbind`, `elitemobs:prestige`,
  `elitemobs:custom_item_id` (= the dagger file) and
  `elite_damage` ALL PRESENT — the soulbound construction,
  impossible for an offline credit.
- **B2 — rejoin**: no ARMED-RETURN line, no return message,
  ledger unchanged. No second grant.
- **R — crash recovery**: an OWED console-grant dagger claim
  was planted (via grantclaim) while an arm for the same item
  was live; at join the listener logged `ARMED-RETURN
  RECOVERED: ... arm removed, no second grant.` — claims
  count unchanged (8), arm gone, player still announced to
  (their dagger genuinely waits). A plain STONE console claim
  held by the same account was correctly NOT mistaken for the
  dagger's twin.
- **C — EM absent**: EM jar parked; plugin enabled clean (no
  SEVERE); armreturn refused ("EliteMobs is not available on
  this server; nothing was stored."); the pre-seeded arm's
  owner joined → ERROR `ARMED-RETURN FAILED for ArmFail ...
  EM_UNAVAILABLE - the arm is KEPT and will retry at the next
  join.`, ZERO success messaging to the bot, arm still listed
  afterwards.
- **D — no-arm join**: a different account joined; no
  ARMED-RETURN activity, no title, no chat.
- **E — 0.5.8 forms unchanged**: grantclaim material (3 x
  STONE granted), grantclaim emloot (granted), grantclaim
  emloot unknown file (refused, nothing stored) — all exactly
  as 0.5.8.
- The failure in the first B attempt (BUILD_FAILED for an
  unprivileged fresh account) is the EM permission gate
  documented above, not a 0.5.9 defect: the identical build
  succeeded for the same account once it held the permission,
  and the arm was kept + logged throughout, as designed.

Rig left stopped, EM jar restored present, 0.5.9 jar in
place, 0.5.8 jar parked as ExcelBossTrial-0.5.8.jar.off.
Rig accounts ArmTester (opped during the proof) and ArmFail
are rig-local only.

## Files

- Candidate jar: ~/workspace/bosstrial-armreturn-20261124/ExcelBossTrial-0.5.9.jar
- Source tree: ~/workspace/bosstrial-armreturn-20261124/bosstrial/
- Install packet: ~/workspace/bosstrial-armreturn-20261124/INSTALL-PACKET.md
- Surfaces: ~/workspace/bosstrial-armreturn-20261124/SURFACES.md
