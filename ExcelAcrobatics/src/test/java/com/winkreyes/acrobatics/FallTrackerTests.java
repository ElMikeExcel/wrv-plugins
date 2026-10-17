package com.winkreyes.acrobatics;

import com.winkreyes.acrobatics.core.FallTracker;

public final class FallTrackerTests {
    private static int checks = 0;
    private static final double MIN = 4.0;

    private static void ok(boolean cond, String label) {
        checks++;
        if (!cond) throw new AssertionError(label);
    }

    private static void near(float a, double b, String label) {
        checks++;
        if (Math.abs(a - b) > 0.01) throw new AssertionError(label + ": expected " + b + " got " + a);
    }

    public static void main(String[] args) throws Exception {
        // 1. Classic deep-water landing: game counter peaks at 20, then water.
        FallTracker t = new FallTracker();
        float r = -1;
        for (int i = 0; i < 20; i++) r = t.step(false, false, false, i, 100 - i, MIN);
        ok(r < 0, "no award while falling");
        near(t.step(true, false, false, 0f, 79.5, MIN), 20.5, "deep water uses airborne drop");

        // 2. Puddle: counter already zeroed by the engine, landing move says onGround, in water.
        t = new FallTracker();
        t.step(false, false, false, 0f, 70, MIN);
        for (int i = 1; i <= 12; i++) t.step(false, false, false, i, 70 - i, MIN);
        near(t.step(true, true, false, 0f, 57.2, MIN), 12.8, "puddle landing with onGround=true still pays");

        // 3. Latch: second water move in the same dive pays nothing.
        ok(t.step(true, false, false, 0f, 57.0, MIN) < 0, "no double award while in water");

        // 4. Leaving water onto ground rearms.
        t.step(false, true, false, 0f, 58, MIN);
        t.step(false, false, false, 0f, 70, MIN);
        for (int i = 1; i <= 8; i++) t.step(false, false, false, i, 70 - i, MIN);
        near(t.step(true, false, false, 0f, 61.5, MIN), 8.5, "rearmed after ground");

        // 5. Short hop under the minimum: nothing.
        t = new FallTracker();
        t.step(false, false, false, 0f, 70, MIN);
        t.step(false, false, false, 2f, 68, MIN);
        ok(t.step(true, false, false, 0f, 67, MIN) < 0, "3-block hop pays nothing");

        // 6. Swimming up and down in water never pays.
        t = new FallTracker();
        for (int i = 0; i < 10; i++) ok(t.step(true, false, false, 0f, 60 - i * 0.2, MIN) < 0, "swimming");

        // 7. Climbing down a ladder 30 blocks, then into water: fall was cancelled, no award.
        t = new FallTracker();
        t.step(false, false, false, 0f, 100, MIN);
        for (int i = 1; i <= 30; i++) t.step(false, false, true, 0f, 100 - i, MIN);
        ok(t.step(true, false, false, 0f, 69.5, MIN) < 0, "ladder then water pays nothing");

        // 8. Cobweb/slime mid-air counter reset: drop measured only from the reset.
        t = new FallTracker();
        t.step(false, false, false, 0f, 100, MIN);
        for (int i = 1; i <= 15; i++) t.step(false, false, false, i, 100 - i, MIN);
        t.step(false, false, false, 0f, 84, MIN); // counter zeroed mid-air
        t.step(false, false, false, 1f, 83, MIN);
        ok(t.step(true, false, false, 0f, 80.5, MIN) < 0, "web reset: 3.5 below reset is under the minimum");
        t = new FallTracker();
        t.step(false, false, false, 0f, 100, MIN);
        for (int i = 1; i <= 15; i++) t.step(false, false, false, i, 100 - i, MIN);
        t.step(false, false, false, 0f, 84, MIN);
        for (int i = 1; i <= 7; i++) t.step(false, false, false, i, 84 - i, MIN);
        near(t.step(true, false, false, 0f, 76.5, MIN), 7.5, "web reset: only the second drop counts");

        // 9. Teleport/respawn reset forgets a stale fall.
        t = new FallTracker();
        t.step(false, false, false, 0f, 200, MIN);
        t.reset();
        ok(t.step(true, false, false, 0f, 64, MIN) < 0, "teleport reset: no phantom 136-block fall");

        // 10. Real mcMMO API must expose a method McMMOAward probes (1.0 probed a method that does not exist).
        try {
            Class<?> api = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            boolean found = false;
            try { api.getMethod("addModifiedXP", org.bukkit.entity.Player.class, String.class, int.class); found = true; }
            catch (NoSuchMethodException e) { }
            try { api.getMethod("addRawXP", org.bukkit.entity.Player.class, String.class, float.class); found = true; }
            catch (NoSuchMethodException e) { }
            ok(found, "mcMMO ExperienceAPI exposes addModifiedXP or addRawXP");
            boolean oldExists = false;
            for (var m : api.getMethods()) if (m.getName().equals("addExperience")) oldExists = true;
            ok(!oldExists, "1.0's addExperience really does not exist in mcMMO");
        } catch (ClassNotFoundException e) {
            System.out.println("mcMMO jar not on classpath; API check skipped");
        }
        System.out.println("FallTrackerTests passed: " + checks + " checks");
    }
}
