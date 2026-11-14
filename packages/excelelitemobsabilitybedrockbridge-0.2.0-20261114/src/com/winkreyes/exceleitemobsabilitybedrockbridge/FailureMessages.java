package com.winkreyes.exceleitemobsabilitybedrockbridge;

/**
 * Plain-words chat lines for a refused fire. EliteMobs also sends its
 * own feedback for most refusals; these lines are the bridge's short,
 * always-true gloss of the refusal reason, written so they can never
 * contradict EliteMobs' specific message.
 */
public final class FailureMessages {

    private FailureMessages() {}

    /** The line to show the player for a failure. Never null or blank. */
    public static String line(BridgeFailure failure) {
        if (failure == null) {
            failure = BridgeFailure.UNKNOWN;
        }
        return switch (failure) {
            case NONE -> "That ability didn't fire.";
            case WRONG_THREAD -> "That didn't go through - try again.";
            case INVALID_PLAYER -> "That ability isn't available right now.";
            case INVALID_LEVEL -> "Your class level is too low for that ability.";
            case ABILITY_NOT_REGISTERED -> "That ability isn't set up yet.";
            case NO_VALID_TARGET -> "No valid target for that ability.";
            case NO_CORPSE -> "That ability needs a recent Elite corpse nearby.";
            case PATH_BLOCKED -> "Something is blocking that ability.";
            case UNSAFE_DESTINATION -> "That spot isn't safe for that ability.";
            case ENGINE_CLOSED -> "Class abilities are unavailable right now.";
            case NOT_MOVING -> "That ability needs you to be on the move.";
            case NO_LOW_HEALTH_ALLY -> "No ally nearby is low on health for that ability.";
            case NO_WOUNDED_TARGET -> "No wounded target nearby for that ability.";
            case NO_ELITE_TARGET -> "That ability needs an Elite target nearby.";
            case NO_LARGE_TARGET -> "That ability needs a large target nearby.";
            case NO_BOSS_TARGET -> "That ability needs a boss target nearby.";
            case UNKNOWN -> "That ability didn't fire.";
        };
    }
}
