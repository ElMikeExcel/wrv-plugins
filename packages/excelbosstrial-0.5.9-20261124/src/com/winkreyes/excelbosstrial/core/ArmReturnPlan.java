/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * 0.5.9: the pure half of the join-armed return commands -
 * {@code /bosstrial armreturn <player> emloot <loot-file>},
 * {@code /bosstrial disarmreturn <player> [loot-file]} and
 * {@code /bosstrial listreturns} - in the GrantClaimPlan
 * shape: validate the words here, with NO state change on a
 * refusal, and let the adapter resolve the player, probe
 * EliteMobs and write the arm store. The loot-file rules are
 * GrantClaimPlan's own (a bare custom-item file name; EM's
 * registry is the authority on whether it exists, probed by
 * the adapter at arm time), so an arm can never name a file
 * the grantclaim emloot form would refuse.
 */
public final class ArmReturnPlan {

    /** Why a parse failed (the adapter words the console error). */
    public enum Error { USAGE, MODE, LOOT_FILE }

    /** A validated arm: who the return waits on, and the file. */
    public record ArmArgs(String playerWord, String lootFile) {
    }

    /** Arm parse outcome: exactly one of args / error non-null. */
    public record Result(ArmArgs args, Error error) {
        public boolean ok() {
            return args != null;
        }
    }

    /** A validated disarm: the player word, and the loot file
     *  when the admin named one (null = every arm they hold). */
    public record DisarmArgs(String playerWord, String lootFile) {
    }

    /** Disarm parse outcome: exactly one of args / error. */
    public record DisarmResult(DisarmArgs args, Error error) {
        public boolean ok() {
            return args != null;
        }
    }

    private ArmReturnPlan() {
    }

    /**
     * Validate an armreturn call: exactly the four words
     * {@code armreturn <player> emloot <loot-file>}. Arming is
     * data-only (the item is built at the join, with the
     * player present), so there is deliberately no count word
     * - a return is one owed item.
     */
    public static Result parseArm(String[] args) {
        if (args == null || args.length != 4
                || !"armreturn".equalsIgnoreCase(args[0])
                || args[1] == null || args[1].isBlank()) {
            return new Result(null, Error.USAGE);
        }
        if (!GrantClaimPlan.isEmLootForm(args[2])) {
            return new Result(null, Error.MODE);
        }
        GrantClaimPlan.EmLootResult file = GrantClaimPlan.parseEmLoot(args[3], null);
        if (!file.ok()) {
            return new Result(null, Error.LOOT_FILE);
        }
        return new Result(new ArmArgs(args[1].strip(), file.plan().lootFile()), null);
    }

    /**
     * Validate a disarmreturn call:
     * {@code disarmreturn <player>} (every arm that player
     * holds) or {@code disarmreturn <player> <loot-file>}
     * (just that one). A named file must pass the same
     * bare-name rule an arm was validated under.
     */
    public static DisarmResult parseDisarm(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)
                || !"disarmreturn".equalsIgnoreCase(args[0])
                || args[1] == null || args[1].isBlank()) {
            return new DisarmResult(null, Error.USAGE);
        }
        if (args.length == 2) {
            return new DisarmResult(new DisarmArgs(args[1].strip(), null), null);
        }
        GrantClaimPlan.EmLootResult file = GrantClaimPlan.parseEmLoot(args[2], null);
        if (!file.ok()) {
            return new DisarmResult(null, Error.LOOT_FILE);
        }
        return new DisarmResult(new DisarmArgs(args[1].strip(), file.plan().lootFile()), null);
    }
}
