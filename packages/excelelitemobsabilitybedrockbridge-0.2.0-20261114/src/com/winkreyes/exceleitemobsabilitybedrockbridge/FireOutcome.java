package com.winkreyes.exceleitemobsabilitybedrockbridge;

/**
 * The result of one bridge fire attempt, in the bridge's own types.
 *
 * @param successful whether EliteMobs executed the ability
 * @param abilityId  EliteMobs' ability identifier for the attempt, when
 *                   it reported one (diagnostics only; may be null)
 * @param failure    refusal reason; NONE on success
 */
public record FireOutcome(boolean successful, String abilityId, BridgeFailure failure) {

    public static FireOutcome success(String abilityId) {
        return new FireOutcome(true, abilityId, BridgeFailure.NONE);
    }

    public static FireOutcome failure(String abilityId, BridgeFailure failure) {
        return new FireOutcome(false, abilityId,
                failure == null ? BridgeFailure.UNKNOWN : failure);
    }

    /** A fire that never reached EliteMobs (exception in the gateway). */
    public static FireOutcome unreachable() {
        return new FireOutcome(false, null, BridgeFailure.UNKNOWN);
    }
}
