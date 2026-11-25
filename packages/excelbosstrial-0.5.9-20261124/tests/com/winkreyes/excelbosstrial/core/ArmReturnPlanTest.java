/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * 0.5.9: the arm/disarm command-word rules - the shapes the
 * adapter may act on, and the refusals (wrong word count,
 * a non-emloot mode, a path-shaped or empty loot file) that
 * must store nothing. The loot-file rules are GrantClaimPlan's
 * own, reused: an arm can never name a file the grantclaim
 * emloot form would refuse.
 */
public final class ArmReturnPlanTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // The one arm shape.
        ArmReturnPlan.Result ok = ArmReturnPlan.parseArm(
                new String[] {"armreturn", "Jester_Turtle", "emloot",
                        "primis_ice_village_alanus_dagger.yml"});
        check(ok.ok() && ok.args().playerWord().equals("Jester_Turtle")
                        && ok.args().lootFile().equals("primis_ice_village_alanus_dagger.yml"),
                "the arm shape parses");
        check(ArmReturnPlan.parseArm(new String[] {"ARMRETURN", "Jester_Turtle", "EMLOOT",
                        "primis_ice_village_alanus_dagger.yml"}).ok(),
                "the arm words are case-insensitive");

        // Refusals: usage.
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "emloot"})
                        .error() == ArmReturnPlan.Error.USAGE,
                "a missing file word is a usage refusal");
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "emloot",
                        "a.yml", "2"}).error() == ArmReturnPlan.Error.USAGE,
                "a count word is a usage refusal (a return is one item)");
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", " ", "emloot", "a.yml"})
                        .error() == ArmReturnPlan.Error.USAGE,
                "a blank player word is a usage refusal");
        check(ArmReturnPlan.parseArm(null).error() == ArmReturnPlan.Error.USAGE,
                "null args are a usage refusal");

        // Refusals: mode.
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "signature",
                        "warden"}).error() == ArmReturnPlan.Error.MODE,
                "a signature arm is a mode refusal (emloot only)");
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "STONE",
                        "3"}).error() == ArmReturnPlan.Error.MODE,
                "a material arm is a mode refusal (emloot only)");

        // Refusals: loot file (GrantClaimPlan's bare-name rule).
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "emloot",
                        "../evil.yml"}).error() == ArmReturnPlan.Error.LOOT_FILE,
                "a path-shaped file is a loot-file refusal");
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "emloot",
                        "sub/dir.yml"}).error() == ArmReturnPlan.Error.LOOT_FILE,
                "a nested file is a loot-file refusal");
        check(ArmReturnPlan.parseArm(new String[] {"armreturn", "Jester_Turtle", "emloot",
                        "  "}).error() == ArmReturnPlan.Error.LOOT_FILE,
                "a blank file is a loot-file refusal");

        // Disarm shapes.
        ArmReturnPlan.DisarmResult all = ArmReturnPlan.parseDisarm(
                new String[] {"disarmreturn", "Jester_Turtle"});
        check(all.ok() && all.args().lootFile() == null,
                "a bare disarm takes every arm the player holds");
        ArmReturnPlan.DisarmResult one = ArmReturnPlan.parseDisarm(
                new String[] {"disarmreturn", "Jester_Turtle",
                        "primis_ice_village_alanus_dagger.yml"});
        check(one.ok() && one.args().lootFile().equals("primis_ice_village_alanus_dagger.yml"),
                "a named disarm takes just that arm");
        check(ArmReturnPlan.parseDisarm(new String[] {"disarmreturn"})
                        .error() == ArmReturnPlan.Error.USAGE,
                "a player-less disarm is a usage refusal");
        check(ArmReturnPlan.parseDisarm(new String[] {"disarmreturn", "Jester_Turtle",
                        "a.yml", "extra"}).error() == ArmReturnPlan.Error.USAGE,
                "a four-word disarm is a usage refusal");
        check(ArmReturnPlan.parseDisarm(new String[] {"disarmreturn", "Jester_Turtle",
                        "../evil.yml"}).error() == ArmReturnPlan.Error.LOOT_FILE,
                "a path-shaped disarm file is a loot-file refusal");

        System.out.println("ArmReturnPlanTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
