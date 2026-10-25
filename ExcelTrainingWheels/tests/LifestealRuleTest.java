import com.winkreyes.exceltrainingwheels.core.LifestealRule;

public class LifestealRuleTest {
    public static void run() {
        System.out.println("LifestealRuleTest");
        double M = 2.0, E = 4.0;
        // who earns it
        Check.close(2.0, LifestealRule.healFor(true, true, false, true, false, false, M, E), "protected kills hostile: 2 HP (1 heart)");
        Check.close(4.0, LifestealRule.healFor(true, true, false, true, true, false, M, E), "protected kills elite: 4 HP (2 hearts)");
        Check.close(4.0, LifestealRule.healFor(true, true, false, false, true, false, M, E), "elite on a non-hostile base type still counts as elite");
        Check.close(0.0, LifestealRule.healFor(true, false, false, true, false, false, M, E), "unprotected killer: nothing");
        Check.close(0.0, LifestealRule.healFor(true, true, true, false, false, false, M, E), "killing a player: nothing");
        Check.close(0.0, LifestealRule.healFor(true, true, true, true, true, false, M, E), "player flagged hostile/elite still nothing");
        Check.close(0.0, LifestealRule.healFor(true, true, false, false, false, false, M, E), "passive animal: nothing");
        Check.close(0.0, LifestealRule.healFor(true, true, false, true, false, true, M, E), "spawner-cage hostile: nothing (farm guard)");
        Check.close(0.0, LifestealRule.healFor(true, true, false, true, true, true, M, E), "spawner-cage elite: nothing");
        Check.close(0.0, LifestealRule.healFor(false, true, false, true, true, false, M, E), "lifesteal disabled: nothing");
        Check.close(0.0, LifestealRule.healFor(true, true, false, true, false, false, -3.0, E), "negative mob-hp never damages");
        // cap
        Check.close(2.0, LifestealRule.cappedGain(10.0, 20.0, 2.0), "10/20 +2 -> gains 2");
        Check.close(1.0, LifestealRule.cappedGain(19.0, 20.0, 2.0), "19/20 +2 -> capped at 1");
        Check.close(0.0, LifestealRule.cappedGain(20.0, 20.0, 2.0), "full health -> gains 0");
        Check.close(0.0, LifestealRule.cappedGain(22.0, 20.0, 4.0), "above max (absorption-ish edge) -> gains 0, never lowers");
        Check.close(2.5, LifestealRule.cappedGain(17.5, 20.0, 4.0), "elite at 17.5/20 -> 2.5");
        Check.close(4.0, LifestealRule.cappedGain(5.0, 40.0, 4.0), "raised max health (40) respected");
        Check.close(0.0, LifestealRule.cappedGain(0.0, 20.0, 2.0), "dead killer (0 HP) gets nothing");
        Check.close(0.0, LifestealRule.cappedGain(10.0, 20.0, 0.0), "0 heal -> 0");
        Check.close(0.0, LifestealRule.cappedGain(10.0, 20.0, Double.NaN), "NaN heal -> 0");
        Check.close(0.0, LifestealRule.cappedGain(10.0, Double.NaN, 2.0), "NaN max -> 0");
        Check.close(20.0, LifestealRule.newHealth(19.0, 20.0, 4.0), "newHealth never exceeds max");
        Check.close(12.0, LifestealRule.newHealth(10.0, 20.0, 2.0), "newHealth 10 + 2 = 12");
        // config sanitizing
        Check.close(2.0, LifestealRule.sanitizeHp(2.0, 2.0), "sanitize keeps 2.0");
        Check.close(0.0, LifestealRule.sanitizeHp(-1.0, 2.0), "sanitize negative -> 0");
        Check.close(20.0, LifestealRule.sanitizeHp(500.0, 2.0), "sanitize clamps to 20");
        Check.close(4.0, LifestealRule.sanitizeHp(Double.NaN, 4.0), "sanitize NaN -> default");
        // labels
        Check.eq("+1 heart", LifestealRule.heartsLabel(2.0), "label 2 HP");
        Check.eq("+2 hearts", LifestealRule.heartsLabel(4.0), "label 4 HP");
        Check.eq("+0.5 heart", LifestealRule.heartsLabel(1.0), "label 1 HP");
        Check.eq("+1.3 hearts", LifestealRule.heartsLabel(2.5), "label 2.5 HP");
    }
}
