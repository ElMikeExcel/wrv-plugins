# Reward capacity is a release gate

The current v1 prototype is not approved for a pilot with this behavior.

## Current measurable behavior

- Ledger ceiling: 100,000 simultaneously live token objects; retained references share a token.
- Session identity ceiling: 100,000 identities. Recovery guards are never discarded to satisfy this cap.
- Entry-only journal budgets are separate: 128 MiB retained bytes, 2,048 sessions, 512 MiB filesystem reserve.
- Token exhaustion rejects a new allocation and permanently blocks new Build admission for that helper lifetime. Existing owned tokens are not evicted.
- Provider clients currently treat a reflected allocation exception as an endpoint fault. This can leave future positive Survival earnings denied even after token capacity returns. This must not be silently shipped.
- No persistent user-visible exhaustion diagnostic/recovery protocol has yet been implemented. Reflection access or a test assertion is not an adequate operator notification.

## Smallest separately testable contract increment

Add an explicitly negotiated capacity-outcome capability for begin and recipient-fork operations, using JDK-only immutable result data. Distinguish CREATED, KNOWN_CAPACITY_REJECTION and INVALID_OR_UNAVAILABLE in the same synchronized operation. Keep the v1 ambiguous-failure path fail-closed. A second status query after a null response cannot distinguish the original cause reliably and is not the proposed fix.

A known capacity rejection must not poison the provider's endpoint identity or invalidate its existing owned leases. Keep new Build admission blocked. Once capacity returns, fresh ordinary Survival awards may use the same verified provider epoch again; do not require an invisible global earnings restart. Invalid handles, changed epoch, unknown origin or provider loss remain fail-closed.

Expose bounded diagnostics: configured limit, current/high-water live tokens, pending counts by provider, rejected allocations by provider/type, first/last rejection timestamps, and the sticky Build-admission problem. Emit a first-failure operator warning and rate-limited continued warnings, with a concise affected-player message where an award was actually refused. Never log inventory payloads. Diagnostics must make a failed award visible without pretending it was paid.

This distinction alone does not preserve an award refused at the hard ceiling. Provider-specific handling must either retain an already-earned obligation under a reviewed bounded retry protocol, or explicitly report the failure for operator resolution. Retrying must preserve original amount, recipients, bonuses and exactly-once state; recomputing later may change the legitimate award. Recipient-fork rejection cannot silently change a party divisor or discard an existing parent's pending obligation. No arbitrary timeout may erase earned work.

## Focused tests before any wider rewrite

Use a two-token ledger to exercise exhaustion predictably:

1. Create two allowed owned rewards; a third returns only KNOWN_CAPACITY_REJECTION, with counters/warning.
2. Both existing tokens stay allowed and close once. Completing one frees capacity.
3. A subsequent new Survival award succeeds on the same provider epoch, while new Build entry stays refused pending review.
4. A recipient-fork rejection leaves the parent and existing recipients intact. A tested retry preserves original allocation and settles once.
5. A genuinely invalid token, epoch replacement or unknown origin still fails closed and never takes the capacity recovery path.
6. Concurrent close/allocation cannot mislabel an outcome. Mixed negative costs retain their native semantics.
7. Repeated overload does not create unbounded logs, queues or identity maps. Restart diagnostics explain unresolved obligations rather than promising automatic replay.

## Possible later reduction in token pressure

A verified immutable ever-enrolled cohort can establish that an unrelated actor will never enter Build during this server epoch. Providers could then carry that actor's immutable origin locally without reserving a builder token, reserving only when a recipient belongs to the cohort or is recovery-guarded. This needs a versioned classification/recipient-reservation contract, explicit cold-restart rules for cohort migration, and every consumer's tests. Unknown cohort/origin must not be treated as unrelated safe Survival.

Do not implement one permanent/shared allowed UUID: it defeats token completion, ownership and unknown-handle guarantees. Do not evict pending tokens or reset balances/skills to solve pressure. The current overnight checkpoint intentionally does not add either shortcut or a broad provider rewrite.
