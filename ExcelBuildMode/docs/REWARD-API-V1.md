# Reward bridge v1: implementation contract

The bridge is public instance methods on the Bukkit plugin named `ExcelBuildMode`. Consumers use reflection against that plugin object; signatures use only JDK classes. Do not bundle another copy of its core classes or create a circular plugin dependency. ExcelBuildMode loads before Jobs, mcMMO and EliteMobs. Consumers must validate API version, epoch and the required coverage registration before using a token. Cold-start integration only; hot reload/helper replacement is unsupported.

```
int rewardApiVersion();
boolean rewardIsActorGuarded(java.util.UUID actor);
java.util.UUID rewardApiEpoch();
boolean rewardRegisterProvider(String provider, String revision, String[] coverage);
java.util.UUID rewardBegin(java.util.UUID actor, String provider, String source,
                           java.util.UUID target, boolean sourceAllowed);
boolean rewardIsAllowed(java.util.UUID token);
long rewardGeneration(java.util.UUID token);
boolean rewardCanReceive(java.util.UUID token, java.util.UUID recipient);
java.util.UUID rewardFork(java.util.UUID token, java.util.UUID recipient);
boolean rewardRetain(java.util.UUID token);
void rewardComplete(java.util.UUID token);
```

## Exact readiness manifests

The registered set must equal the required set (order immaterial; duplicates invalid). Revision must be nonblank and repeat registration must match it. Provider IDs are case-sensitive:

- Jobs: `jobs.action.v1`, `jobs.perform.v1`, `jobs.deferred-origin.v1`, `jobs.pending-drain.v1`
- mcMMO: `mcmmo.root-origin.v1`, `mcmmo.personal.v1`, `mcmmo.party-share.v1`, `mcmmo.party-xp.v1`, `mcmmo.deferred-origin.v1`, `mcmmo.pending-drain.v1`
- EliteMobs: `elitemobs.contributor-rewards.v1`, `elitemobs.deferred-origin.v1`, `elitemobs.pending-drain.v1`

Registration is the provider implementation's attestation that ALL listed paths were audited and instrumented. Names alone cannot establish this; artifact review and tests remain required. A provider must not claim the manifest for a partial patch. Loaded unregistered/unsupported providers block Build admission, not ordinary preexisting Survival behavior. An unpatched provider therefore cannot silently allow protected Build entry.

## Cold incomplete-provider state query

`rewardIsActorGuarded(actor)` is an additive pre-release v1 state-only method. It is available before provider registration and does not grant readiness or create a reward token. It returns true for a null actor, before journal initialization, after helper shutdown, and for every authoritative Build/landing/recovery/failed guard. For an enabled helper with a successfully initialized journal, false means that actor is currently unguarded.

An intentionally incomplete provider may use this only during its verified cold-start incomplete mode to preserve native ordinary Survival behavior while excluding known recovery actors. It must remain unregistered for the entire cold lifetime, which blocks all new Build admission. Switching from this untracked cold mode into registered mode requires a full restart; otherwise previously native work would lack pending reservations. It must validate the enabled helper and original epoch; a missing method, unreadable helper, disable or epoch loss must never silently switch an already-active integration into permissive cold mode. A total journal-open/list failure still disables this helper and is an unresolved operational release gate. This query is not proof that an absent/disabled helper has no recovery data.

## Token rules

- `rewardBegin` captures an immutable root decision before scheduling or any reward/quest/sharing mutation. `sourceAllowed` is the provider's independent farm/eligibility veto; false can never be overridden by this API. The optional target is contextual identity, not a permission grant.
- A false origin stays false through exit, rejoin and later sessions within the same API epoch. A token never becomes allowed by checking the actor's current mode later.
- New origins are denied while the actor is in any authoritative Build, landing, recovery or failed session. A valid allowed root reserves pending Survival work for its actor until its final reference completes. Pending work blocks that player's Build admission.
- `rewardCanReceive` is a momentary preview only. Use `rewardFork` to reserve each actual recipient before computing the sharing divisor or member-count bonus. Filter the created child tokens by `rewardIsAllowed`, then compute/distribute using only eligible children. A fork captures recipient eligibility once and reserves allowed pending work for that recipient. The parent's immutable origin denial is inherited. Forking does not consume the parent reference.
- Root and fork start with one reference. Retain BEFORE publishing a deferred callback. Complete exactly once per owned reference in success/failure/cancellation cleanup. Retain/fork on an already closed token fails; a child remains valid after its parent completes. Never close a token while code still intends to apply its reward. `rewardComplete` is a reference release, NOT globally idempotent for a retained live token: each scheduled/cancellation owner must share an atomic once flag so only one release occurs. Repeated completion is harmless only after the token is already fully closed.
- Null/unknown/closed token handles fail closed: allowed/canReceive/retain=false, generation=-1, fork=null, complete is a no-op. A known/open parent always produces an explicit denied child handle when origin/recipient eligibility fails. No unknown token may be replaced with a newly captured current-mode token.
- The API epoch is a random UUID for the helper's cold lifetime. Token handles are UUIDs and must not be translated to a new epoch. If the bridge changes or disappears, the provider must surface the failure and follow its documented safe stop; do not silently replay old work under the new epoch. Persisted provider queues need a separately reviewed origin/pending protocol; this in-memory API does not supply one.
- `rewardBegin` returns null if required registration, identity or source arguments are invalid. It returns a valid denied token for an independently vetoed or guarded origin. Providers must distinguish unavailable integration from an ordinary denied reward and never silently fall back to unguarded payouts while Build sessions may exist.

## Threading/admission

Ledger operations are synchronized and use only immutable JDK values, never Player/GameMode objects. Player/session UI maps are not read by async providers. Build admission and pending registration share that lock. Read-only snapshot capture/atomic journal preparation can briefly hold the admission lock; it MUST be released before any mode change, permission attachment, player save or event-bearing callback. Disk latency is synchronous and can stall the server/provider thread; bounded inventory size and measured runtime behavior are release checks, not an arbitrary timeout that sacrifices durability.

A failed snapshot preparation before the journal commit does not publish a guarded session. A write that reports failure after its commit is conservatively guarded; it may already have a durable recovery record. A committed recovery record is guarded before destructive player mutation. Recovery records are published on startup before providers can receive an eligible handshake. Guard release occurs only after restoration, the vanilla save attempt and durable journal completion. This API does not remove the existing crash boundary between plugin journal and vanilla playerdata.

## Provider obligations

Every earning point and deferred producer represented by the manifest must capture or inherit an origin, account for pending Survival work and complete references. This is a drain contract, not an award replay ledger. No arbitrary cooldown, incomplete buffer count, late personal-event cancellation or current Creative check can substitute for it. Cross-plugin farm rules, unsupported direct XP APIs and other unrelated reward systems are outside this bridge unless explicitly instrumented and tested.

## Failure handling and deployment status

Provider disable and newly hot-loaded providers permanently block new Build admission for the current helper epoch. Existing classified tokens can still be queried, retained, forked and completed so in-flight native work can drain; the bridge never reclassifies a previously allowed token based on current mode. Provider disappearance is not permission to silently create replacement roots or bypass the bridge. A full cold restart is required.

An ambiguous exception after journal completion retains the current-lifetime guard and requires administrator review; a missing journal record is not treated as proof that it is safe to release a failed guard. The ordinary completed cold-start tombstone is not an active session.

The candidate is version 1.0, with plugin name ExcelBuildMode unchanged. The source and test fixtures are offline only. Handshake fixtures are deliberately synthetic and do not establish actual Jobs, mcMMO or EliteMobs mutation coverage. Refer to each provider artifact review and the combined release matrix before deployment.

## Resource ceilings and ownership leaks

Production defaults cap open tokens at 100,000 and remembered session-generation identities at 100,000. A new-token capacity breach throws an explicit `IllegalStateException` subtype and makes new Build admission sticky-failed until administrator review and a full restart. It does not evict, cancel, reclassify or decrement any existing handle; those leases remain valid and drainable. Once room is free, ordinary new Survival roots may resume, while Build admission stays blocked. A provider must never erase an already-earned native award merely because the bridge refuses a new allocation: already-earned deferred work must own an existing retained lease before publication. Resource exceptions must be surfaced and handled as integration failure, not converted to an unguarded recapture.

Session-identity capacity refuses new admission before snapshot preparation. Durable startup/recovery guards are never discarded to enforce that cap; if they exceed it, recovery remains guarded and admission stays blocked. Per-owner exactly-once completion remains mandatory. There is no timeout, periodic eviction or inferred reward completion. A leaked allowed lease deliberately blocks that player's Build entry; leaked denied handles also consume capacity. Monitor provider cleanup and the explicit capacity error, diagnose the leaking producer, and cold restart only after legitimate pending native work has drained or a provider-specific durable recovery procedure has been approved.
