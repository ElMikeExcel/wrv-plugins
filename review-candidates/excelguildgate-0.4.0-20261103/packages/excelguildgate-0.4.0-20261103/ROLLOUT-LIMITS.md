# Offline candidate rollout limitations

No installation/publication/grant has been authorized or performed in this task.

Candidate recovery receipts are additive sections of the owning state files. Old plugin versions ignore or rewrite those sections. A naive rollback to old JARs after recovery operations have occurred can discard durable operation history; restoring pre-recovery state can also undo reviewed global progress or resurrect cooldowns. Therefore any future rollout must retain full post-operation state and receipts separately, audit whether any recovery has happened, and design a state-preserving rollback/forward repair. Do not simply restore old state.yml after a successful reviewed recovery.

Current pending-intent retry is conservative: if BossTrial restarts before applying, its old boot generation is stale and the immutable request is not silently rebased. If a persisted Boss receipt exists, re-preview/confirm can resume provided selected state still matches and no new selected fight/disable/elder-pending condition intervenes. Stale/unresolved cases require operator review; this is not blanket automatic recovery from every interruption.

Atomic rename plus file and directory force are required; there is no non-atomic fallback. A storage/platform inability to supply these guarantees blocks acknowledged recovery. A write whose outcome is uncertain locks further owner state writes so a normal save cannot erase a possibly committed receipt. Runtime gameplay may continue in memory, so a persistence lock demands prompt operator inspection/restart and should not be left unattended.

Disk fingerprint checks catch an external edit before a checked write. They are not a transaction with an uncooperative process editing the same file between comparison and rename. Existing assistant/operator coordination remains required; do not edit these live state files concurrently.

Available integration environment is Paper26.2, not the user's exact live26.3 stack. Results must be labeled accordingly; Java/Bedrock client acceptance, live plugin interactions and platform filesystem semantics remain separate checks.

This candidate handles only reviewed global seal state and a selected boss cooldown. It awards no gear, currency, XP, loot, potion effect or chest, and does not alter anti-cage checks or difficulty.
