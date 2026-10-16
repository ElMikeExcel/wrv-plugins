package com.winkreyes.rewardguard;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.bukkit.event.Cancellable;

/**
 * Pins every third-party API surface the enforcement code relies on, checked
 * reflectively against the ACTUAL jars the server runs (Jobs 5.2.6.3,
 * mcMMO 2.3.003-WRV, EliteMobs 10.9.8), which build.sh places on the test
 * classpath. This is the workflow rule from the Leaderboard 1.0 probe failure:
 * documentation is a lead, the installed jar is proof. If a future plugin
 * update changes one of these signatures, this suite fails before the jar
 * ships instead of the veto failing silently on the live server.
 */
public final class LiveApiSignatureTests {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("check " + assertions); }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("missing class on the live-jar classpath: " + name);
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... params) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException notHere) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new AssertionError("missing method " + name + " on " + type.getName());
    }

    private static boolean cancellable(Class<?> type) {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Class<?> iface : cursor.getInterfaces()) {
                if (iface == Cancellable.class) return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        // Jobs: the pre-payment event is the money/points/XP reduction point.
        // 1.1 scales instead of cancelling, so every component needs BOTH a
        // getter (to read the amount) and a setter (to write the scaled one).
        // javap verdict on the installed 5.2.6.3 jar: all three components
        // (money, points, XP) have both, each backed by the event's amounts
        // map, so no component needs a cancel fallback.
        Class<?> prePayment = load("com.gamingmesh.jobs.api.JobsPrePaymentEvent");
        check(cancellable(prePayment));
        check(method(prePayment, "getLivingEntity").getReturnType() == org.bukkit.entity.LivingEntity.class);
        check(method(prePayment, "setCancelled", boolean.class).getReturnType() == void.class);
        check(method(prePayment, "setAmount", double.class).getReturnType() == void.class);
        check(method(prePayment, "setExp", double.class).getReturnType() == void.class);
        check(method(prePayment, "getJob") != null);
        check(method(prePayment, "getAmount").getReturnType() == double.class);
        check(method(prePayment, "getPoints").getReturnType() == double.class);
        check(method(prePayment, "setPoints", double.class).getReturnType() == void.class);
        check(method(prePayment, "getExp").getReturnType() == double.class);

        // Jobs: the XP gain event is the second, independent XP point. It
        // carries XP only (no money/points), and its XP is settable too.
        Class<?> expGain = load("com.gamingmesh.jobs.api.JobsExpGainEvent");
        check(cancellable(expGain));
        check(method(expGain, "getLivingEntity").getReturnType() == org.bukkit.entity.LivingEntity.class);
        check(method(expGain, "setExp", double.class).getReturnType() == void.class);
        check(method(expGain, "getExp").getReturnType() == double.class);
        check(method(expGain, "setCancelled", boolean.class).getReturnType() == void.class);

        // mcMMO: XP gain events carry player/skill/amount/reason, no entity,
        // and are cancellable through the shared parent class.
        Class<?> xpGain = load("com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent");
        check(cancellable(xpGain));
        check(method(xpGain, "setRawXpGained", float.class).getReturnType() == void.class);
        check(method(xpGain, "getRawXpGained").getReturnType() == float.class);
        Class<?> preXpGain = load("com.gmail.nossr50.events.experience.McMMOPlayerPreXpGainEvent");
        check(cancellable(preXpGain));
        check(method(preXpGain, "setXpGained", int.class).getReturnType() == void.class);
        Class<?> reason = load("com.gmail.nossr50.datatypes.experience.XPGainReason");
        check(reason.isEnum());
        check(Enum.valueOf((Class<Enum>) reason, "PVE") != null);
        check(Enum.valueOf((Class<Enum>) reason, "SHARED_PVE") != null);
        check(method(xpGain, "getXpGainReason").getReturnType() == reason);
        // The entity really is absent from the XP event: this absence is the
        // reason the suppression-window design exists, so pin it.
        boolean exposesEntity = true;
        try {
            method(xpGain, "getEntity");
        } catch (AssertionError absent) {
            exposesEntity = false;
        }
        check(!exposesEntity);

        // EliteMobs: the death event is informational only. Pinning the
        // ABSENCE of Cancellable documents the no-veto limitation as a test:
        // if a future EliteMobs adds a cancellable reward event, this suite
        // fails and the guard can grow a real veto.
        Class<?> eliteDeath = load("com.magmaguy.elitemobs.api.EliteMobDeathEvent");
        check(!cancellable(eliteDeath));
        check(method(eliteDeath, "getEntity").getReturnType() == org.bukkit.entity.Entity.class);
        check(method(eliteDeath, "getEntityDeathEvent") != null);
        check(Modifier.isPublic(eliteDeath.getModifiers()));

        System.out.println("PASS " + assertions + " live-API signature assertions against the installed Jobs/mcMMO/EliteMobs jars");
    }
}
