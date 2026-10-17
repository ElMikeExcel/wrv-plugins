# Historical reward investigation — superseded implementation status

This document preserves the early inventory-only investigation. Do not use its old JAR hash, dependency availability or original source pin as the current release record. The inspected Jobs a962e140… tree was later identified as newer 5.2.6.7 source, not installed5.2.6.3. The current Jobs patch is pinned to official5.2.6.3 commit6fe3c272c3c970d4a94fbf7615953803eb722503 and has live-hash-matched vendor binaries. See REWARD-API-V1.md, VERIFICATION-1.0.md and the Jobs maintenance/coverage manifest for current implemented and unimplemented paths. The conclusions that late event-only hooks miss earlier quests/party sharing remain relevant, but old line numbers are historical evidence only.

# Reward integration proposal and test contract

**PROPOSAL ONLY. NOT IMPLEMENTED.** This supplementary review does not change or rebuild the tested ExcelBuildMode JAR. Its SHA-256 remains:

`561a616a605c1b085d59e0ea4cb63f44d99c0470624e51c1a01d1b468da26a8a`

The existing inventory/XP snapshot prototype does not suppress all Jobs or mcMMO rewards. It must not be presented as satisfying that additional deployment requirement. No global configuration, installed plugin, player balance, skill, group, entitlement or live server was changed during this review.

## Decision and recommended scope

The server's saved Jobs configuration allows Creative pay and flying/gliding exploration rewards. `DailyQuests.Enabled` is true, so the prepayment quest-progress gap is active. `add-xp-player` is currently false; the earlier direct vanilla-XP branch is therefore a compatibility/configuration risk rather than a currently enabled path. Creative mode alone is insufficient. Recovery's temporary Survival flight hold must also be covered.

A small external event listener can suppress a useful subset, but it cannot provide both of the requested guarantees:

1. Do not lose legitimate Survival-origin rewards already queued before Build Mode.
2. Do not allow Build-origin delayed rewards to arrive after returning to Survival.

The observed events expose the player and award details, not the originating action's Build/Survival status or session identity. Looking at the player's current mode cannot recover that missing information. A post-exit cooldown, “wait one tick,” timestamp guessed from event arrival, or balance/skill rollback is not a sound substitute.

**Recommended strict path:** use provider-supported early eligibility checks with action-origin information, before reward/party/quest mutations and before work is deferred. A shared mcMMO eligibility integration is being designed separately; this document gives its requirements and tests, not a competing patch. Jobs needs its own early-action coverage. If a required loaded provider lacks those validated hooks, a future strict Build Mode candidate should refuse new entry and explain the provider blocker. The unchanged current JAR does not yet perform this admission check.

A limited external-listener experiment is a separate, explicitly incomplete option. It should not be quietly enabled or described as complete reward isolation.

## Evidence and provenance

### Jobs

Inspected official source at commit `a962e140d432d36db61739b51007c184a86353e3`, supplied through the repository connector for the Jobs5.2.6.3 investigation:

- [Jobs.java](https://github.com/Zrips/Jobs/blob/a962e140d432d36db61739b51007c184a86353e3/src/main/java/com/gamingmesh/jobs/Jobs.java)
- [JobsPrePaymentEvent.java](https://github.com/Zrips/Jobs/blob/a962e140d432d36db61739b51007c184a86353e3/src/main/java/com/gamingmesh/jobs/api/JobsPrePaymentEvent.java)

The local source copies have been read. A cached Jobs5.2.6.2 JAR was available for supplementary API-signature inspection, but **it is not the installed5.2.6.3 artifact and is not sufficient runtime/version proof**. No new dependency was downloaded. The exact installed5.2.6.3 binary still needs signature/source correspondence and runtime tests before integration approval.

Observed order in the pinned Jobs.java:

- `action()` checks daily quests before prepayment: the no-job branch calls `checkDailyQuests` at line1126, and the enrolled-job branch at line1234.
- In the enrolled-job branch, when `addXpPlayer()` is enabled, vanilla player XP is mutated at lines1247–1270, before the prepayment event at lines1276–1278.
- Cancelling that prepayment event sets money, points and job-XP amounts to zero at lines1280–1283. The zero-total check then skips later reward processing at lines1362–1363, before fast-payment caching, `payOut`, progression XP and level-up calls.
- The no-job branch similarly zeros money/points and returns before payout. It has already checked quests.
- `perform()` fires prepayment at lines1463–1464 and returns immediately on cancellation. On this path the later XP event, quest progress, `payOut` and progression mutations have not yet happened.
- The public prepayment event has player, job, currency amounts, block/entity/victim and `ActionInfo`. There is no action timestamp, action/session UUID or captured originating mode. Source objects describe the activity, but do not prove which mode the player was in when delayed work began.

Consequences:

- A prepayment-only listener can suppress money, points and job XP for an event identified as guarded **at event time**. It is earlier than payout submission, so it need not cancel later settlement events or confiscate already queued Survival money.
- On the `action()` path it cannot undo quest progress or the earlier direct vanilla XP mutation. The existing inventory snapshot eventually restores original vanilla XP, but that does not make the intervening direct reward harmless or prove delayed attribution. No quest or balance rollback is proposed.
- A Survival action whose prepayment itself is delayed until Build Mode would be wrongly cancelled by a current-session test. A Build-origin action whose prepayment occurs after exit could pass it.
- Correct handling requires auditing all action entry paths, including `action()`, cached `perform()` and deferred callbacks. The hook must precede each reward/progression mutation. An early hook must preserve intended anti-exploit/block-tracking bookkeeping rather than blindly short-circuiting unrelated tracking behavior.

### mcMMO2.3.003-WRV-XP-2

The locally available historical binary identifies itself as `2.3.003-WRV-XP-2` and hashes to:

`5f6279995c7b30240d2a7e07cb916b6e28b82537826f9ef0f2702b080cf20978`

That hash matches the October12 deployment record. Inspection used its actual bytecode and public signatures, along with the preserved upstream source and WRV patches. This establishes the inspected historical artifact's identity; current live equality still depends on the separate current-server verification.

Upstream source base: `058279c148a65bd708dc85067d3858f6adde1173`, followed by WRV patches0001–0004. The saved XP-2 source/build record is [WRV commit bdcafe0a5449379958ea1aa8476cae1baf756e51](https://github.com/ElMikeExcel/wrv-plugins/commit/bdcafe0a5449379958ea1aa8476cae1baf756e51).

Observed order in the XP-2 bytecode:

1. `beginXpGain()` resolves child-skill paths and the Together factor, then invokes party sharing before an individual's pre-XP event.
2. `beginUnsharedXpGain()` rejects the recipient when currently Creative, calculates modified XP, calls `applyXpGain()`, then may award the party's separate XP afterward.
3. `applyXpGain()` fires `McMMOPlayerPreXpGainEvent`. XP-2 **does** honor cancellation and uses its raw float value. This is a WRV improvement over the inspected upstream source; do not confuse the two.
4. The late `McMMOPlayerXpGainEvent` is dispatched before adding/registering personal profile XP. Cancelling it stops that individual addition. Setting only the amount to zero at an earlier priority is insufficient because SelfListener's normal-priority EarlyGameBoost can add XP again.
5. Cancelling either personal event does not roll back party sharing that happened beforehand and does not prevent the caller's later separate party-XP award. A builder can also be the origin of awards processed for other recipients; checking only each recipient's current session does not identify that origin.
6. `AwardCombatXpTask` defers gain processing and retains player, target, skill, XP and reason, without originating mode or session identity. Its callback invokes `beginXpGain()` later. Other deferred/passive paths must be enumerated before claiming full coverage.

The pre/late event API exposes player, skill, amount and `XPGainReason`; it does not expose an originating session, timestamp, original earner or `XPGainSource`. Cancellation is useful for the recipient's current event but is not a complete early eligibility seam.

## Two distinct implementation options

### Option A: narrow external event-time suppression

This would be a separately labeled limited candidate, not fulfillment of strict reward isolation:

- Register `JobsPrePaymentEvent` cancellation while the player has an active Build/landing/recovery session.
- Register mcMMO pre/late personal-XP cancellation, with positive-gain semantics carefully checked. Use cancellation, not merely setting zero.
- Leave ordinary Survival events and later Jobs settlement events untouched. Never subtract balances or reset skill profiles.
- Validate loaded plugin identity, exact event class, `Cancellable` contract, player accessor, synchronous/asynchronous semantics and registration before allowing entry. Missing/changed APIs must not silently disable protection.
- Use proper concurrency-safe session publication if an audited event can be asynchronous; do not read the current adapter's mutable main-thread HashMap from an async callback or call player mutations from that callback.
- Keep the established immutable vanilla XP snapshot and recovery ordering intact.

Known failures remain: earlier Jobs quest/direct-XP changes, mcMMO party/shared credit and both directions of delayed-origin attribution. The tests below must exhibit these limitations rather than treating them as pass cases.

### Option B: strict provider integration

This is the recommended contract for the shared integration work:

1. Capture eligibility at the actual reward-generating action's entry, before deferral, fan-out, quest progress, vanilla XP, personal/party XP, reward cache or payment submission. Capture the actor's identity and session status once for that action.
2. Carry that immutable eligibility/origin through every supported deferred path and party-derived award. A callback must not reclassify old work using the actor's new mode. A derived party award retains the original action eligibility, while the recipient's own current eligibility can impose additional restrictions.
3. Publish a versioned capability check identifying covered providers/paths. Class existence or matching version text alone does not prove that the early hook precedes all mutations or retains provenance.
4. An active Build/landing/recovery state suppresses newly originating rewards even if the vanilla game mode is temporarily Survival. New normal-Survival actions resume only after successful restoration, vanilla save attempt, completed journal record and release of the session guard.
5. Previously accepted Survival payment entries must remain payable exactly once during Build Mode. Do not cancel an aggregate payout based on current mode or drain it by mutating the economy. Any native provider gate unrelated to this new integration retains its existing behavior; the integration must not force rewards through it.
6. Build-origin deferred work remains denied after voluntary exit, quit, death, reconnection and later sessions. If a provider persists queued work across restart, persist the required origin information with that work or fail closed for that unsupported path. A player-only boolean or an arbitrary time window is insufficient.
7. Provider disable/reload/unavailability must stop new Build admission. If lost during a session, maintain suppression for already classified deferred work and trigger safe inventory recovery; do not drop the origin information. Define the supported restart/disable sequence explicitly rather than relying on hot reload.
8. Do not alter global configs, member groups, skill balances, party membership, paid effects or broad PDC. Do not snapshot/restore economic or progression state.

### Smaller strict-entry option: provider-reported pending-work drain

A separate deferred-award replay ledger is substantial scope and is not required if the providers can prove an idle boundary before entry. Prefer investigating this smaller alternative first:

- Every audited producer registers already-earned Survival work before scheduling it and reports when it settles or is definitively cancelled under normal provider rules.
- Build entry waits or refuses with a plain explanation while that pending work exists. The player remains in normal Survival, with no inventory clearing, snapshot transition or new reward suppression. Awards settle through the existing provider, not through a duplicate payout/replay engine.
- Admission requires an atomic or main-thread-serialized check that all relevant producers are clear and no new unclassified work can enter between the check and session publication. A guessed timeout, an empty payment buffer alone, or a counter missing one producer is not proof.
- Already-accepted Jobs economic settlements may alternatively continue untouched if their queue entries are unambiguously final eligible Survival work. Identify which pending paths require draining and which are safe to settle later; do not cancel an aggregate by current mode.
- Build-origin deferred actions still need immutable denied provenance so exiting cannot make them eligible. Draining prior Survival work does not solve that half of the problem.
- If pending work survives provider/server restart, its pending-status and origin bookkeeping must be correspondingly reliable. If the provider cannot expose a complete pending-work boundary, report the unsupported case and keep strict admission blocked.

This is an option requiring complete producer auditing, not an implemented readiness counter. It avoids a new generic payment ledger but does not remove the need to identify every earning point, delayed callback and persistent queue.

The proposed common seam also checks both original actor and recipient before sharing. If suppressed recipients are removed from a party split, filter them **before** computing the divisor and any member-count-based bonus; make that behavior an explicit reviewed policy. With no guarded participants, normal sharing mathematics must remain byte-for-byte or numerically equivalent to the control. Do not infer this policy from cancelling each recipient afterward.

The common mcMMO seam owner should choose its concrete API and implementation placement. ExcelBuildMode should consume that seam rather than maintain an independent mcMMO patch. External/admin direct balance/XP writes, uninstrumented third-party APIs, and rewards from unrelated plugins remain outside this contract unless explicitly added and tested.

## Acceptance test contract

Run against the actual candidate artifacts, exact Paper26.2 build123/Java25 and the relevant loaded provider versions. Keep baseline/candidate hashes, source commits, configs and logs. Synthetic native players are useful but must be distinguished from real Java/Bedrock clients. These are planned tests, not additional passed assertions.

### Ordinary Survival regression

- With no Build session, compare control and integrated runs for Jobs money, points, XP, quest progress, limits and level-up side effects on each supported action path, including no-job and multiple-job cases.
- Compare mcMMO personal/child/party XP, equal sharing, Together/weekend factors, fractional amounts, EarlyGameBoost and existing cancellations. Preserve each path's native ordering and eligibility.
- Provider absence must not alter ordinary survival; an unsupported loaded provider blocks Build admission only. Check unrelated players continue earning normally.

### Jobs active-session coverage

- Supported Build-origin break/place/explore/flying/gliding and any other audited actions produce no new money, points, job XP, quest advancement, quest completion reward, vanilla-XP direct mutation or level-up command side effect.
- Repeat in landing hold, where current GameMode is Survival and temporary flight is enabled.
- Exercise both `action()` and cached `perform()`, no-job branch, each currency independently, multiple jobs, boosted/limited awards and negative/payment-cost cases. The policy for costs must be explicit; do not silently cancel legitimate existing debts.
- Prove prepayment-only interception's limitations using a configured quest and `addXpPlayer()`. This test should fail the strict contract for OptionA, not be hidden by the eventual snapshot restore.

### mcMMO active-session coverage

- No own personal XP, level-up, separate party XP or distributed party rewards from a denied Build-origin action.
- Repeat with equal-share enabled and a nearby normal-Survival party member. The ordinary member must still earn from their own legitimate actions.
- Exercise all supported source categories/skills, child splitting, passive/deferred awards and direct supported APIs. Include low-level EarlyGameBoost and fractional positive XP.
- Cancelling/zeroing a late personal event must not be accepted as evidence that earlier sharing or party XP was suppressed.

### Cross-transition attribution

For every supported deferred path, explicitly control callback/payment ordering:

1. Survival action accepted and payment already queued → enter Build → batch settles. Existing money is paid once; no cancellation/rollback.
2. Survival action captured but reward processing deferred → enter Build → callback. New integration does not misclassify or newly discard the Survival origin; unrelated native eligibility behavior remains unchanged and separately recorded.
3. Build action captured → exit normally → callback. No Build-origin reward appears.
4. Build action captured → forced landing hold → callback. Still denied despite Survival mode.
5. Build action captured → quit/rejoin/death/restart/later Build session → callback or persisted settlement. Original denial survives whenever the provider supports retaining that work.
6. Two overlapping actions from different origins, including the same action type and player. No time-window heuristic or “last action” slot may merge their eligibility.
7. Builder-origin shared gain delivered to a normal party member after the builder exits. No leakage through recipient-only filtering.
8. Legitimate normal member-origin party activity while the builder is guarded. Ordinary participants retain their native rewards; recipient policy is explicit and tested.

### Pending-work drain variant

- Seed each deferred Survival producer separately, then try entry. Admission remains blocked until that exact producer settles normally; no arbitrary wait makes an unreported producer safe.
- Seed several overlapping producers and a queued Jobs economic batch. Verify the precise pending-work set/count, final settlement exactly once and admission only at the declared safe boundary.
- Introduce a new Survival action at the admission boundary and prove serialization: either it belongs to fully tracked pre-entry work or it receives the new denied origin, never an untracked gap.
- Exercise producer failure/cancellation, logout, target unload/death, provider reload and persisted work. Pending counts must neither clear prematurely nor silently disappear.
- Demonstrate an unaudited producer causes strict admission to remain unsupported, rather than certifying safety from an incomplete count.

### Snapshot and failure safety

- Reward-provider admission failure occurs before any inventory clearing or Creative transition. Existing Survival inventory/armor/offhand/ender chest and XP remain untouched.
- Durable snapshot-write failure does not create an active reward-suppression episode for an otherwise normal player.
- Mode-change cancellation, attachment failure, restoration failure and landing hold preserve the established journal behavior. No provider callback overwrites the immutable Survival snapshot.
- Validate the earliest/latest boundaries of session publication, including audited async callbacks. No main-thread state race, stale session replacement, mixed-session token or Bukkit off-thread mutation.
- Missing methods/classes, disabled provider, unsupported version, registration failure, mismatched early-hook capability and hook loss mid-session produce an explicit blocked-admission or safe-recovery result. Never quietly continue unprotected.
- Restart tests cover the native payment buffer's actual persistence behavior. Do not claim durability for work that the provider itself does not persist.

## Historical design-stage status (superseded by 1.0 implementation)

The source audit and this test contract are complete. No reward interception has been implemented, no new reward tests have run, and the previously tested prototype JAR is unchanged. Exact provider integration remains a release gate; the separate shared mcMMO seam proposal and Jobs path/provenance audit must resolve it before the candidate can claim session-safe reward suppression.

## 1.0 implementation update

The helper ledger, v1 public bridge, durable admission boundary and lifecycle integration are now implemented. See REWARD-API-V1.md, CHANGELOG-1.0.md and VERIFICATION-1.0.md. The earlier source-audit limitations and provider acceptance matrix above remain relevant: a helper handshake cannot establish that an actual provider has instrumented all its earning/deferred paths. Actual provider artifacts are owned and tested separately.
