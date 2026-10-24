package com.winkreyes.excelbosstrial.dragon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 0.1.2 regression test for the flip-crash hotfix. Drives the REAL
 * FlipEffects.play code (the exact method runFlip calls) against
 * Proxy World/Player stubs.
 *
 * The World stub simulates Paper 26.2's CraftParticle contract for
 * DRAGON_BREATH, which the compile-classpath API predates: the
 * particle requires a Float data argument, and the no-data
 * spawnParticle overload throws IllegalArgumentException("missing
 * required data class java.lang.Float") — the exact throw that
 * aborted the flip in 0.1.0/0.1.1 (found by the ExcelTelemetry rig;
 * see BUILD-0.1.2.md). If anyone reverts the call to the no-data
 * overload (or passes the wrong data type), this suite fails.
 */
public final class FlipEffectsTest {

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

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == void.class) {
            return null;
        }
        return 0;
    }

    /** Recorded spawnParticle invocation. */
    private static final class ParticleCall {
        Particle particle;
        int count;
        Object data;
        boolean hadDataParam;
    }

    private static final class WorldStub implements InvocationHandler {
        final List<ParticleCall> particles = new ArrayList<>();
        int sounds;
        int lightning;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "playSound" -> {
                    sounds++;
                    // No Sound-constant comparison here: in a bare JVM
                    // the Sound class cannot initialise (no server
                    // registry), which is also why play() leads with
                    // the particle call.
                    return null;
                }
                case "strikeLightningEffect" -> {
                    lightning++;
                    return null;
                }
                case "spawnParticle" -> {
                    ParticleCall call = new ParticleCall();
                    call.particle = (Particle) args[0];
                    Class<?>[] params = method.getParameterTypes();
                    call.hadDataParam = params[params.length - 1] == Object.class;
                    // Both Location overloads: (Particle, Location, count, ...)
                    if (args[1] instanceof Location) {
                        call.count = (Integer) args[2];
                    }
                    if (call.hadDataParam) {
                        call.data = args[args.length - 1];
                    }
                    // Simulate Paper 26.2 CraftParticle for DRAGON_BREATH.
                    if (call.particle == Particle.DRAGON_BREATH) {
                        if (!call.hadDataParam || call.data == null) {
                            throw new IllegalArgumentException(
                                    "missing required data class java.lang.Float");
                        }
                        if (!(call.data instanceof Float)) {
                            throw new IllegalArgumentException(
                                    "data class must be java.lang.Float");
                        }
                    }
                    particles.add(call);
                    return null;
                }
                case "toString" -> {
                    return "WorldStub";
                }
                default -> {
                    return defaultValue(method.getReturnType());
                }
            }
        }
    }

    private static final class PlayerStub implements InvocationHandler {
        String title;
        String subtitle;
        int fadeIn = -1;
        int stay = -1;
        int fadeOut = -1;
        final List<String> messages = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "sendTitle" -> {
                    title = (String) args[0];
                    subtitle = (String) args[1];
                    fadeIn = (Integer) args[2];
                    stay = (Integer) args[3];
                    fadeOut = (Integer) args[4];
                    return null;
                }
                case "sendMessage" -> {
                    if (args[0] instanceof String s) {
                        messages.add(s);
                    }
                    return null;
                }
                case "getName" -> {
                    return "StubPlayer";
                }
                default -> {
                    return defaultValue(method.getReturnType());
                }
            }
        }
    }

    public static void main(String[] args) {
        WorldStub worldStub = new WorldStub();
        World world = (World) Proxy.newProxyInstance(FlipEffectsTest.class.getClassLoader(),
                new Class<?>[] {World.class}, worldStub);
        PlayerStub p1 = new PlayerStub();
        PlayerStub p2 = new PlayerStub();
        Player player1 = (Player) Proxy.newProxyInstance(FlipEffectsTest.class.getClassLoader(),
                new Class<?>[] {Player.class}, p1);
        Player player2 = (Player) Proxy.newProxyInstance(FlipEffectsTest.class.getClassLoader(),
                new Class<?>[] {Player.class}, p2);

        // Sanity: the stub really models the 26.2 failure mode, so the
        // main checks below are not vacuous — the OLD call shape throws.
        boolean oldShapeThrows = false;
        try {
            world.spawnParticle(Particle.DRAGON_BREATH, new Location(world, 0, 70, 0),
                    200, 4.0, 2.0, 4.0, 0.05);
        } catch (IllegalArgumentException e) {
            oldShapeThrows = "missing required data class java.lang.Float".equals(e.getMessage());
        }
        check(oldShapeThrows, "stub reproduces the 26.2 no-data DRAGON_BREATH throw");
        worldStub.particles.clear();
        worldStub.sounds = 0;
        worldStub.lightning = 0;

        // The real effects path, exactly as runFlip invokes it.
        Location fx = new Location(world, 0, 72, 0);
        Location fountain = new Location(world, 0, 65, 0);
        Throwable thrown = null;
        try {
            FlipEffects.play(world, fx, fountain, List.of(player1, player2),
                    "The Seal Resists", "The dragon refuses to fall.", "chat line");
        } catch (Throwable t) {
            thrown = t;
        }

        // THE regression assertions: the particle call must have been
        // made, in the corrected form, and must not have thrown the
        // 26.2 IllegalArgumentException. (If the call regresses to
        // the no-data overload, the stub throws BEFORE recording, so
        // the particles list stays empty and `thrown` is that IAE.)
        check(!(thrown instanceof IllegalArgumentException),
                "no IllegalArgumentException from the particle call (the 0.1.2 fix)");
        check(worldStub.particles.size() == 1, "exactly one particle burst");
        if (worldStub.particles.size() == 1) {
            ParticleCall call = worldStub.particles.get(0);
            check(call.particle == Particle.DRAGON_BREATH, "burst is DRAGON_BREATH");
            check(call.count == 200, "burst count preserved (200)");
            check(call.hadDataParam, "call uses the data-carrying overload");
            check(call.data instanceof Float && ((Float) call.data) == 1.0f,
                    "Float data argument passed (1.0f, the vanilla breath scale)");
        }

        // In a bare JVM the Sound constants cannot initialise (Paper
        // resolves them through the server registry, which does not
        // exist here), so play() stops at the playSound line — AFTER
        // the particle burst — with an ExceptionInInitializerError
        // whose cause chain names RegistryAccess. That artifact, and
        // only that artifact, is tolerated; the full path (sound,
        // lightning, titles) is proven on the real 26.2 rig
        // (BUILD-0.1.2.md). On any JVM with a registry, everything
        // below is asserted in full.
        boolean registryArtifact = false;
        for (Throwable c = thrown; c != null; c = c.getCause()) {
            if (String.valueOf(c.getMessage()).contains("RegistryAccess")) {
                registryArtifact = true;
            }
        }
        if (thrown == null) {
            check(worldStub.sounds == 1, "exactly one sound effect");
            check(worldStub.lightning == 1, "exactly one lightning effect");
            for (PlayerStub p : new PlayerStub[] {p1, p2}) {
                check("The Seal Resists".equals(p.title), "audience player received the title");
                check("The dragon refuses to fall.".equals(p.subtitle),
                        "audience player received the subtitle");
                check(p.fadeIn == 10 && p.stay == 70 && p.fadeOut == 20,
                        "title timings preserved (10/70/20)");
                check(p.messages.equals(List.of("chat line")),
                        "audience player received the chat line");
            }
        } else {
            check(registryArtifact,
                    "only the bare-JVM Sound/registry artifact may interrupt play() here; got: "
                            + thrown);
            System.out.println("NOTE: bare-JVM registry artifact stopped play() after the "
                    + "particle burst (expected off-server); sound/lightning/title "
                    + "assertions deferred to the 26.2 rig proof.");
        }

        System.out.println("FlipEffectsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
