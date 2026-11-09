# ㉚ CODE PHASE — BUILD RECORD (BossTrial 0.4.0 + ExcelProtect 0.1.8)

Built Nov 8, 2026 under Mike's "Fix everything as best as you can" — design of
record `~/workspace/bosstrial-fix-20261108/DESIGN.md`. Build/test/stage only:
no installs, no panel, no sends, no GitHub writes were performed by this pass.

## Candidates

| Artifact | Size | SHA-256 | Base |
|---|---|---|---|
| `ExcelBossTrial-0.4.0.jar` | 401,269 B | `043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3` | live 0.3.7 source tree (`seal-tuning/build-0.3.7/bosstrial/tree`), copied to `build/bosstrial/` — **REBUILT Nov 8, 2026 (see the REBUILD section at the end of this record); the original candidate `0638c158…` / 393,906 B is SUPERSEDED, never install it** |
| `ExcelProtect-0.1.8.jar` | (see protect-src) | `ba8b58c1545f6840064208c530e4a5cdc8cdc622ac7c95eb6639b544188a8601` (this build instance; Protect's local-build does not pin zip dates, so the hash is instance-specific — sources + suite are the verified identity) | live 0.1.7 source (`excelprotect/src/ExcelProtect`), copied to `build/protect-src/` |

Toolchain: JDK 25.0.4.1 (`~/workspace/.jdk25/bin`), Paper-26.3 classpath from
`seal-tuning/build-0.3.6/classpath.txt` — the same toolchain that reproduced
the 0.3.7 baseline byte-for-byte class of build.

## Baselines (before any edit)

- BossTrial pristine copied tree: **PASS, 27,142 checks / 0 failures**
  (`build/base-build.log`) — matches the 0.3.7 record exactly.
- Protect pristine tree: **16 tests / 16 successful / 0 failed** (child's
  verified rerun; the first baseline run's anomalous "52 tests" summary line
  was investigated in HEAL-ATTRIBUTION-FINDINGS.md and did not reproduce).

## Per-item status

| Item | Status | Evidence |
|---|---|---|
| PM-8 fight-record persistence (every ending + 60 s mid-fight snapshots) | **built + tested** (unit) / rig-verified (see spike) | `snapshotFightRecord` refactored into one close-record writer; endings wired: verdict (unchanged path), warden give-up (GIVE_UP), maintenance prune (CLOSED_NO_VERDICT), admin reset (ADMIN_RESET — snapshots ALL open encounters before clearing), seal grant (SEAL_GRANTED), server stop (SERVER_STOP, first thing in onDisable). Non-verdict closes publish only the boss_state close record to Protect, never a fight-table verdict. Mid-fight snapshots write the stable file `fights/<seal>-open-<boss8>.yml` every 60 s per open encounter; the ending's record deletes it. FightTrackerTest pins the new columns + stable name. |
| PM-1 heal-aware reconciliation | **built + tested** (unit) / rig-verified (see spike) | `EncounterLedger.healedPool/netDamage` (share/player/other math untouched — pinned); `HealMath` (effective cap, exact inverse of IntakeFactor, net floor) + HealMathTest (19 checks); `onBossRegainHealth` MONITOR handler (all reasons, bosses only, effective-only); immediate reconcile-with-pending-subtraction + 1 s tick pin to the NET fraction in `pressurePulseTick`'s existing walk (no new task). Dragon: record-only by design — its state machine already judges from entity health, so crystal healing is heal-consistent there by construction (design's PM-3 dragon note discharged as verification, no code). |
| PM-2 VerdictJudge + pool-exhaustion verdict | **built + tested** (unit) / **verdict-path route: GOVERNED LETHAL BLOW — rig-proven (see spike)** | `VerdictJudge.exhausted` + VerdictJudgeTest (11 checks). Verdict fires from the record handler and the tick; per-encounter verdict-pending state makes governor + record handler pass the blow through unscaled/unrecorded; blow is dealt by the ledger's top contributor when online (ties → earliest); a refused blow retries at 250 ms escalating ×4, then a direct health set — all through the real death path (drops, spoils, seal loot, GuildGate SealCreditEvent unchanged). |
| PM-9 unattributed accounting | **built + tested** | FightRecord gains `other-damage` / `healed-pool` / `net-damage` (YAML + log header; ProtectTableShapeTest updated to the new constructor); milestone accounting line (players / other / healed) at every beat; throttled console line for unattributed hits ≥ `fight-tracking.unattributed-log-threshold` (default 10). **The ×21 amplification is closed structurally:** new `GovernorStash` (WeakHashMap keyed by the event instance) — the governor (host + DragonChapter) stashes its governed output in ledger space; the record handler consumes it and only falls back to re-derivation when the governor demonstrably did not run. |
| PM-4 bar truth (named number spaces) | **built + tested** | Default action-bar format is now `"{last} hit · {total} pool damage total"` (config.yml + code default + FeedbackFormat/FeedbackTotals test pins updated); the format's comment names both spaces (entity HP for the hit, pool HP for the total — they coincide only at factor 1). |
| PM-5 bar truth (net bar + FALLING state) | **built + tested** | `FeedbackTotals.fromLedger` derives from `netDamage()` — no-heal fights are byte-identical (FeedbackTotalsTest pins both directions incl. the heal-rises case). New `feedback.exhausted-format` (default `"{name} — FALLING…"`) rendered by FeedbackDisplay while an encounter is verdict-pending (new constructor seam, old constructor delegates). |
| PM-6 sprinkle rework | **built + tested + rig-proven** (milestone waves guaranteed, DEFER unspent, rotation order; the spike caught and this build fixed a first-tick cadence defect — the cadence clock now starts at the encounter's first tick, so the first timed wave lands a full cadence in) | `SprinkleRoller` + its test DELETED (grep-verified: only SprinkleService referenced it). New pure `SprinkleCadence` judge (FIRE/DEFER/SPENT/IDLE) + SprinkleCadenceTest (17 checks); `SprinkleBook` carries owed/lastWave/rotation state (+ tests); `SprinkleSettings` gains `cadence-seconds` (default 150) + `waves` rotation; defaults: caps 3→5 alive, 6→24 per fight, wither rotation [GHAST ×1 aerial] → [ZOMBIFIED_PIGLIN ×3 ring] → [WITHER_SKELETON ×2 ring]; milestone stamps owe a wave (guaranteed), the 1 s host walk calls `cadenceTick`, a blocked wave DEFERs unspent (one log line per block), only the per-fight cap retires it. Aerial placement: ring radius 6–10, height +8–12 over the anchor. Host parses `<chapter>.sprinkle.waves` + `cadence-seconds`. Note: `chance`/`count` fields remain parsed-but-inert for config compatibility. |
| PM-10 signature-aware grantclaim | **built + tested** (pure half) | `grantclaim <player> signature <chapter>` (wither/warden/dragon/elder + aliases, `raid` → RaidStandard shield) builds through the verdict's own path (`SealLoot.perChampionPlan` → `buildLootStack` → `stampProvenance`, base form; dragon adds its per-champion materials) and files via `teamClaims.grantClaim`. GrantClaimPlan gains `isSignatureForm/isRaidSignature/signatureChapter` + 16 test pins. **Every** successful grantclaim (both forms) now sends the recipient a chat alert pointing at /bossrewards. Adapter half is compile-verified only (no rig verb files a grantclaim; the pieces it composes are the verdict path's own, exercised by SignatureGrantTest + the spike's CREDIT kill). |
| PM-12 closeEncounterForSeal | **built + tested** (design route taken: TWO-PLUGIN truth, BossTrial half built; GuildGate half = patch spec below) | GuildGate's admin `grant` fires NO event — verified in the newest GuildGate source in the workspace (`excelguildgate/SOURCE-0.3.4`: grant calls its state store directly; the only callEvent is an unrelated narration proposal). Live GuildGate is 0.4.0 with **no source in the workspace**, so the GuildGate-side event was NOT built from the stale tree. Built instead: (1) public API `ExcelBossTrialAPI.closeEncounterForSeal(sealId, cause)` (provider pattern mirroring outgoingScaleFor, bound at enable) — any future seal-state writer can call it; (2) an internal close (SEAL_GRANTED record, bar + sprinkle close, entity restored to vanilla max-health at its current health fraction); (3) a **maintenance grant watch**: an encounter whose seal stood unbroken at open, whose seal now reads broken, with no verdict running and no sibling encounter of the chapter, closes after two consecutive 30 s passes agree. Elder chapter excluded from the watch (chapter-wide seal vs per-monument encounters would false-close; the API remains its seam). Rig-verified (see spike). |
| PM-11 BossRewards auto-present | **built + tested** (policy) / adapter compile-verified | The first-skip hint is GONE: skips are silent while retries remain; the waiting line (reworded to pure information — `"&eYour boss rewards are waiting in &f/bossrewards&e."` in code fallback + config) is sent exactly once when the retry window closes unpresented. New `InventoryCloseEvent` handler re-attempts immediately for owners with a live retry state. Bedrock fallback: on window exhaustion, a Floodgate player is offered a Cumulus SimpleForm whose button opens the real claims menu — decision = unit-tested `PresentFallback` policy; the send is reflective + fail-soft. **Built-untested-live with reason:** no Floodgate jar exists in any rig asset on this machine, so the Cumulus seam cannot be executed here; it must be proven on the family server rig (a Bedrock player whose present window expires). |
| ㉚.10 Protect healing attribution | **built + tested + rig-proven** (child lane, verified by parent rerun) | Full account: `build/protect-src/HEAL-ATTRIBUTION-FINDINGS.md`. Conviction (4 stacked defects, any one sufficient for the Nov 7 zeros): (1) PRIMARY — `CombatCore.onCast` resolved the caster via `mage.getController().getPlayer()`; Magic 11.2.5's controller has NO `getPlayer()` (javap-verified; parent re-verified the broken chain at pristine source lines 71–72) → every cast threw, swallowed by `catch (Throwable ignored)`, claims map permanently empty, `creditHeal` unreachable. (2) CastEvent fires AFTER the spell performs, so forward-only pairing missed instant heals by construction. (3) The 1 s single-consume window cannot cover the WRV HoT heal (ticks to ~5 s), and `onDamage` ate claims. (4) Credited heals were filed into an area-cell side fight that `closeFight` always discarded. Fix: new pure `HealAttribution` core (effective/overheal split, bidirectional non-consuming claim matching per BossTrial's rules), `onCast` via `Mage.getPlayer()`, buffered regains, fight-anchoring for heals, splash path (below). Suite 16 → **23/23 green** (parent rerun, exit 0). Live rig proof on a scratch Paper + Magic 11.2.5 stage with fake players: HoT heal credited 17.0 effective / 22.0 raw to the casting healer (20 SPELL records), splash Healing II credited once as POTION (effective 6.0 at 0.75 intensity, vanilla regain suppressed — no double count), fight_summary per-player table carries nonzero healing_done — the Nov 7 shape is gone. NOT covered: Bedrock healer (no Floodgate client in this environment — owed player/rig proof), lingering potions (out of scope by task), drinkable potions (no thrower; record generic, as before). |
| ㉚.5 Protect half — item_break capture | **built + tested** (record shape) / live trigger owed | Grep-verified absent in 0.1.7 (no `item_break` anywhere). New `PlayerItemBreakEvent` MONITOR handler in ItemEvents writing `item_break` to the items stream: full item identity (material, count, name, enchants, trim, rid) + `damage` / `max_durability` / `durability_remaining`; gated by `log.items.breaks` (default on); record construction is a pure static pinned by the unit suite. Live trigger (wearing an item to destruction on a rig) was not scripted — handler is boot-verified (registered) only. The **DeathRecord BREAK-cause half of ㉚.5 is phase 2 — not started** (named per task). |
| ㉚.6 DeathRecord L&F menu | **phase 2 — not started** (named per task) | — |
| ㉚.9 fight-stats surface (healing column) | **not built — gate held** | The design gates ㉚.9's healing column on ㉚.10's controlled-heal readback proof. That proof PASSED in the child's rig (see ㉚.10), but the stats-surface build itself was not in this pass's enumerated nine steps and time went to the verdict spike; the gate is now OPEN for the follow-up build — the column may be added on the strength of the ㉚.10 readback recorded above. |
| ㉚.11 Warden readiness package | **not built — out of scope for this pass** | The task's enumerated steps did not include it. THE WARDEN RULE stands unchanged: no Warden attempt until ㉚ installs + readiness audit + earlier-chapter re-acceptance fights. |

## Test counts

- BossTrial full suite (build-test.sh, all main-method harnesses):
  baseline 27,142 → **27,225 checks / 0 failures** (counted the same way
  both runs — the sum of the harnesses' "N passed / N checks" tallies;
  new HealMath / VerdictJudge / SprinkleCadence / PresentFallback suites,
  new pins in EncounterLedger / FightTracker / GrantClaimPlan /
  SprinkleBook / FeedbackTotals / SprinkleSettings / ProtectTableShape /
  PluginYmlParse, PM-4 format-string pins updated, SprinkleRollerTest
  deleted with its subject).
- Protect suite (JUnit via local-build.sh): baseline 16 → **23 tests /
  23 successful / 0 failed** (parent rerun).
- Candidate jar built by package.sh (which re-runs the whole suite):
  exit 0.

## Rig spike (BossTrial 0.4.0 + Protect 0.1.8 on the P5c rig copy)

Two boots of the P5c rig (Paper 26.3-235, GuildGate 0.4.0, EliteMobs
10.9.12, TrainingWheels 0.1.3, RigDriver fake player; wither pool 480,
per-event cap 45, rig config's sprinkle caps 3 alive / 6 per fight).
Logs: `build/rig-spike/spike-console.log`, `build/rig-spike2/spike2-console.log`.

**Spike 1 (scripted phases) — proven:**
- **PM-1**: boss heals recorded in pool space in real time
  (`BossTrial boss heal [The Wither]: +1.0 pool HP (REGEN) - net progress
  is now 44.0 of 480.0`, also `+5.0 pool HP (WITHER)` when sprinkle adds
  died to the wither effect); an idle boss healed 39 pool HP over 70 s
  and its net fell to 6.0 — heal accounting runs both directions.
- **PM-6**: the 75% milestone stamp owed + fired a wave guaranteed
  (ZOMBIFIED_PIGLIN ×2 of 3 — trimmed by the rig config's alive cap 3,
  partial-wave rule); the 50% stamp's wave DEFERRED unspent at a full
  arena (`a wave is owed but the arena is full (alive 3/3) - deferred,
  not spent`). First boot also caught a defect: the cadence clock read
  "due" at fight start and fired a GHAST wave 4 s in. **Fixed before
  the final jar**: the service stamps the cadence clock at the
  encounter's first tick; the first timed wave now belongs a full
  cadence into the fight (SprinkleCadence + service + test pin).
- **PM-8**: ADMIN_RESET records written before the clear (twice,
  complete with healed-pool / net-damage / other-damage); the 60 s
  mid-fight stable snapshot `fights/wither-open-<boss8>.yml` appeared
  during an idle fight; on server stop the SERVER_STOP record replaced
  it (same boss suffix, stable file deleted) as the last BossTrial act
  of the disable.
- **PM-12**: after `guildgate grant wither` over a live encounter, the
  maintenance watch closed it (~30–60 s): SEAL_GRANTED fight record +
  `closed without a verdict: the seal was granted (seal-grant watch) -
  the boss stands down to vanilla strength`.
- Phase A's verdict attempt stalled for a RIG reason, not a code one:
  the wither teleported into an unloaded chunk mid-fight (vanilla
  wither behavior; the fake-player rig loads a tiny area) and the
  ledger froze at 278.2 dealt — the encounter was correctly RETAINED
  (not pruned, not lost) until the scripted reset recorded it. Spike 2
  re-ran the verdict with the driver's proven follow-the-boss loop.

**Spike 2 (slaywither — the verdict): PROVEN, end to end.** The
driver's proven fight loop (party follows the boss, real damage()
calls, health top-ups while the fight is young — the top-ups are
exactly the Nov 7 zombie shape: the entity bar kept refilling
while the ledger climbed). At net 490.0 of the 480 pool (healed
back 5.0): `BossTrial verdict [The Wither]: the pool is spent -
the verdict blow falls` → the boss died the same second through
the real death path → fight record `verdict=CREDIT ... killer=
RigPlayer` (the top dealer dealt the blow) → `BossTrial verdict
CREDIT: The Wither seal earned (103.1% player damage)` →
GuildGate's state.yml records `wither: broken: true, killer:
RigPlayer` (the SealCreditEvent path ran untouched) → signature
claims stored for the champion and auto-presented (review
invisibility applied). Milestone waves fired guaranteed at the
75% and 50% stamps; the 25% stamp owed as the verdict landed;
no cadence wave in a 35 s fight (the fixed clock's first timed
wave belongs at +150 s). **The verdict-path route is the
GOVERNED LETHAL BLOW; the extracted-close-path fallback was not
needed.** The GovernorStash path was exercised on every governed
hit of both spikes (the record handler consumed the stash; the
ledger totals match the governed caps exactly — e.g. the cap
line's 45.0 is the record's per-hit value); the specific ×21
scenario (a second plugin modifying damage at HIGHEST after the
governor) could not be staged — no such modifier plugin exists
in the rig assets — and is covered by the structural argument +
the stash's unit-level design (same event instance, consumed
once), recorded here as the one untested-by-rig seam.

## GuildGate patch spec (PM-12, second half — for a future GuildGate build)

In `GuildGateAdmin`'s grant path (and revoke, symmetrically if desired),
after the state store updates, fire a Bukkit event
`SealGrantedEvent(sealId, actor)` and/or call
`ExcelBossTrialAPI.closeEncounterForSeal(sealId, "guildgate grant")`
reflectively (GuildGate already soft-depends on ExcelBossTrial). Until that
lands, BossTrial 0.4.0's maintenance grant watch covers the admin-grant
shape with ≤ ~60 s latency. Do NOT build this from the 0.3.4 source tree in
the workspace — live GuildGate is 0.4.0 and its source is not on disk; the
patch must be written against the 0.4.0 tree when it is recovered.

## Deviations from the design, with reasons

1. **PM-2 spike route**: the governed-lethal-blow route was proven by the
   rig spike (see above) — the extracted-close-path fallback was NOT needed.
2. **PM-12 is two-plugin in truth** (design flagged this as possible): the
   BossTrial half + state watch shipped; the GuildGate event is a spec
   (above), not code, because the live version's source is absent.
3. **PM-6 wave levels**: waves take their elite level from the chapter's
   existing levels ladder by rotation index (levelFor(rotation)) — the
   design's "wave levels from chapter levelFor" reading; no new per-wave
   level key was invented.
4. **FeedbackTotalsTest/FeedbackFormatTest pins** were updated to the PM-4
   wording (the tests pinned the exact old string the ruling replaced).
5. **Protect jar hash** is build-instance-specific (unpinned zip dates in
   local-build.sh) — recorded as such in the packet; the suite + sources
   are the identity.

## AMENDMENT — Nov 8, 2026, ~6:05 PM CST (sprinkle volume, CONFIG-ONLY; jar untouched)

Mike's ruling (~5:57 PM CST): the automatic Wither waves must exceed the
Nov 7 hand-summoned volume (84 tallied in the console record, ~90 rounded,
75 reached the party). The ㉚ packet's merge-only config step (PACKET-DRAFT
+ MANIFEST entry) now sets, WITHER chapter only: cadence-seconds 120,
waves GHAST ×3 aerial → ZOMBIFIED_PIGLIN ×8 ring → WITHER_SKELETON ×6 ring,
max-alive 16, max-per-fight 48. **No jar rebuild: the shipped candidate is
unchanged — ExcelBossTrial-0.4.0.jar, 393,906 B / sha256
0638c15832d2b6b818f9f6dd2f04b63b703ce2764acd2378b2b92537a1bf4d00
(re-verified on disk at amendment time).** Config surface proven from this
build's source: keys under `wither.sprinkle.` in ExcelBossTrial.java
(`cadence-seconds`, `max-alive`, `max-per-fight`, `waves` as a map list of
{types, count, placement}) parsed into SprinkleSettings.sanitized().
⚠️ CLAMP FINDING: Mike's ruled per-fight target is 140, but sanitized()
hard-clamps maxPerFight to [0, 48] (SprinkleSettings.java; pinned by
SprinkleSettingsTest), per-wave count to [1, 8] and maxAlive to [0, 16] —
the amended alive cap (16) and piglin count (8) sit exactly at their
ceilings and take effect; the per-fight cap delivers 48, not 140. The
packet/MANIFEST/DESIGN records state this openly; closing the gap is a
one-line clamp raise + rebuild (new jar hash; re-opens the 0.4.1 gate) —
a separate follow-up, NOT done here. BossTrial 0.4.1's staged-hash gate
on 0638c158… STANDS (jar byte-identical).

## REBUILD — Nov 8, 2026, evening (clamp raise + Wither taunts + loud sanitize; supersedes the candidate above)

Three of Mike's rulings landed in ONE rebuild of the 0.4.0 tree, so the
jar was built once:

1. **The clamp raise (Mike, ~6:01 PM CST).** The amendment above hit the
   wall it predicted: `SprinkleSettings.sanitized()` hard-clamped
   `maxPerFight` to [0, 48]. The ceiling is now **140** — exactly one
   line (`Math.min(48, …)` → the clamped helper at ceiling 140). Every
   other clamp is untouched by design: maxAlive [0, 16], per-wave count
   [1, 8], cadence [15, 3600]. `SprinkleSettingsTest` re-pins the
   ceiling (over-range 999 now asserts 140) and adds the boundary check
   that exactly 140 loads unsanitized-down.
2. **Wither taunt lines on eats (Mike's direct request, ~6:04 PM CST).**
   During a Wither encounter, when the boss EATS — kills a non-player
   mob and regains health from the kill — it speaks a line in chat to
   the encounter's participants. **Trigger signal (the truth rule):**
   the boss regain handler fires `maybeTaunt` ONLY on
   `RegainReason.WITHER` heals — the game's own fused kill+heal event
   (the Wither regains 5 HP per kill; Bukkit reports exactly that heal
   with reason WITHER; it is the same signal PM-1's heal-aware ledger
   already trusts, first observed in the spike as `+5.0 pool HP
   (WITHER)`). No eat → no line, ever; nothing fires decoratively or on
   a timer. New pure core: `TauntSettings` (lines + cooldown per
   chapter; absent list = chapter default, present-but-empty =
   honored silence; ships lines ONLY for the Wither — the ten settled
   lines, cooldown default 25 s) and `TauntBook` (per-encounter gate:
   the FIRST eat of a fight always speaks; afterwards at most one
   line per `taunt-cooldown-seconds`; the draw never repeats the line
   just spoken when the list has more than one). Delivery: chat to
   `ledger.participants()` only, styled `&5<Name> &8» &d<line>` in the
   plugin's narration idiom; the boss bar is never touched (bar-is-
   the-boss law). Boot awake-line: `Boss taunts: The Wither ARMED
   (10 lines, 25s cooldown), … silent.` Config keys ship in the
   bundled config under `wither.` (`taunts`, `taunt-cooldown-seconds`)
   and ride the packet's merge-only config step.
3. **Loud sanitize (Mike, ~6:05 PM CST — "the deeper defect was the
   SILENCE").** `SprinkleSettings.sanitized()` gains an adjustments-
   collecting overload: whenever clamping or a fail-dark replacement
   actually CHANGES a configured value (chance, count, max-alive,
   max-per-fight, leash-radius, cadence-seconds, waves[i].count), one
   line naming the setting, the configured value, and the applied
   value is recorded; the adapter logs each at boot as a WARNING —
   e.g. `sprinkle config [wither] max-per-fight 999 above ceiling —
   clamped to 140`. Sanitized OUTPUTS are unchanged (pinned by test:
   the loud overload returns byte-identical settings). The pattern,
   now documented in DESIGN: safety ceilings sit far above design
   values, and a clamp that fires is always loud — silence means the
   config loaded untouched.

**Candidate:** `ExcelBossTrial-0.4.0.jar` is now **401,269 B / sha256
`043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`**,
double-built via package.sh (two runs, byte-identical hashes). The
superseded candidates are NOT installable: `0638c158…` (original) and
`5a4c8114…` (an intermediate clamp-only build from this lane, replaced
before any record pointed at it). Class-diff provenance against the
shipped candidate's class output: 180 of 189 classes byte-identical;
changed = `SprinkleSettings` (+ its switch class) and the
`ExcelBossTrial` host (+ its inner classes, which shift with the
outer class); new = `TauntBook`/`TauntSettings` (+ 2 inner/switch
classes). Nothing removed, nothing else touched. (No `EXC-BOSSTRIAL`
fingerprint string exists in this tree — checked at the original
build and re-checked here; the class-diff is the provenance gate,
as before.)

**Suite:** full build-test.sh run, exit 0, **0 failures in every
harness**. Counted by harness summary lines (`N checks / N passed`
tallies summed): pre-rebuild tree 1,975 → clamp-only 1,976 → final
**2,006 checks / 74 harnesses**. The +31 delta is exactly itemized:
SprinkleSettingsTest +7 (1 clamp re-pin + 6 loud-surfacing checks),
new TauntBookTest 15, new TauntSettingsTest 9. NOTE on counting: this
record's original "27,225 checks" figure (and the 27,142 baseline)
came from the original build lane's own tally, which this lane could
not reproduce from the harness summary lines — the same gap exists at
the pristine baseline (27,142 claimed vs 1,975 tallied), so it is a
counting-method difference, not a regression; the 0-failures result
and the itemized delta are the reproducible facts.

**Rig proof (fresh copy of the P5c rig, final jar, Paper 26.3-235;
log `build/rig-final/logs/latest.log`):** the operative wither
sprinkle block was set to max-per-fight **999**, cadence-seconds 120,
max-alive 16. (The rig config carries TWO top-level `wither:` keys;
the LAST one is operative under SnakeYAML — the first rig boot of
this lane proved it by reading the shadowed block's values. The
packet's live-config merge must likewise edit the operative block /
dedupe; flagged in PACKET-DRAFT.)
- Boot: `[WARN] sprinkle config [wither] max-per-fight 999 above
  ceiling — clamped to 140` (the loud surfacing, live) and the
  sprinkle summary **`wither ARMED (cadence 120s, cap 140)`** (the
  packet's boot-verify line, verbatim) and `Boss taunts: The Wither
  ARMED (10 lines, 25s cooldown), An Elder Guardian silent, The
  Warden silent, The Ender Dragon silent.`
- Fight (RigDriver scripted): milestone waves fired guaranteed at
  75% and 50%; the wave counters read **`1/140 spawned this fight`**
  and **`4/140 spawned this fight`** — the 140 cap is operative in
  the fight math, not just the boot line. PM-6 defer/cadence
  behavior unchanged.
- **Taunt, live:** at 00:16:25 UTC, three seconds after the 75%
  wave's ghast spawned, the Wither ate and spoke: `BossTrial taunt
  [The Wither]: "Feed me, champions." (eat #1 this fight)` — the
  first-eat rule firing on a real WITHER-reason heal, in a real
  fight, on the first try.
- **PM-3 proof (the DESIGN's named assertion, previously implied by
  PM-1 — now explicit):** the scripted fight crossed 50% NET at
  00:16:31 UTC (`milestone [The Wither]: 50% of the pool remains
  (245.0 net of 480.0 dealt)`). Console reads of the entity at
  00:16:42 and 00:16:59 UTC: **Health 231.76 / Max Health 480.0 =
  48.3%** — the entity sits in its lower half, i.e. inside vanilla's
  powered band (the Wither's descended/armored phase is DERIVED
  from health ≤ max/2 — `isPowered()` has no separate flag — so the
  crossing hit itself, which pinned the entity to the net fraction,
  put the entity in phase 2 within the same tick window; the pinned
  reading held steady across both queries while the ledger stood
  past halfway). **PM-3: PROVEN on the rig.** The chapter's live
  re-acceptance fight remains owed for the usual reasons (real
  players, real gear), but the phase-2 assertion itself no longer
  rides it as an unproven implication.
- Close: fight record written at reset (pool 480.0, net 248.2,
  healed-pool 30.0), sprinkle closed with its 4 adds removed, server
  stopped clean.

**0.4.1 rebase:** BossTrial 0.4.1 (stats surface) was rebuilt on
this new 0.4.0 tree — see `build-stats/BUILD-RECORD.md` (REBASE
section). Its candidate is now `f7376dc9…` / 411,565 B; the old
candidate `8eb5a004…`, gated on `0638c158…`, is VOID.

**Damage-number verify (Mike's Nov 8, ~6:22 PM re-assertion of the
Nov 2 feel rule — this lane's assigned rig verify-and-report):**
what the PM-4 feedback line shows per hit is the player's own hit in
POOL space (`{last} hit · {total} pool damage total` — the space is
named in the string, per PM-4). Source-verified chain for a melee
hit: the event's final damage (weapon-dependent by construction —
sword tier, Sharpness, crits all arrive inside it) passes
`BossGovernor.govern`, whose ONLY transformations are the per-event
ceiling (45 pool HP for the Wither) and the explosion tick pool;
the ledger then records it through a per-BOSS constant intake
conversion (PM-9's governed stash). Nothing in the chain reads the
weapon, and nothing normalizes a hit toward a house value — every
step is a weapon-independent constant multiplier or the ceiling,
so tier ratios survive intact below the cap. The numbers: vanilla
swords deal 4 (wooden) / 7 (diamond) / 8 (netherite); Sharpness V
makes them 7 / 10 / 11; crits (×1.5) top out at 16.5 — every sword
figure sits far below the 45 ceiling, so NO sword tier is ever
flattened by the cap (the rig's synthetic hits prove the ceiling
itself: a 55 hit logs `trimmed 53.2 -> 45.0`). **Verdict:
progression is visibly distinct — wooden < diamond < netherite on
the feedback line, exactly as Mike's rule requires; not a defect.**
The ceiling's flattening is confined to single events above 45
pool HP (burst stacking), which is its documented design. Method
note: the RigDriver's fake player cannot equip or swing swords
(its hits are synthetic damage events), so the per-tier figures
are vanilla game data carried through the source-verified linear
path, not a live swing test; the player-seat confirmation rides
the Wither re-acceptance fight, where real swords swing.
