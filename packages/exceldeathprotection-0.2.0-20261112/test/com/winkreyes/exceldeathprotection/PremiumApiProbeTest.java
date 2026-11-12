package com.winkreyes.exceldeathprotection;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Probe-replica test: copies ExcelBossTrial's reflective adapter
 * predicate verbatim (class names in order, getMethods(), the name
 * set, the parameter checks, invoke(null, ...) shapes) and runs it
 * against the built API class. This is the test that would have
 * caught every seam failure mode: wrong class name, instance
 * method, long parameter, missing overload.
 */
class PremiumApiProbeTest {

    /** BossTrial's probe predicate, replicated from its adapter. */
    private static Method probe(Class<?> api) {
        for (Method m : api.getMethods()) {
            if ((m.getName().equals("creditPremium") || m.getName().equals("grantPremium")
                    || m.getName().equals("addPremium")) && m.getParameterCount() >= 1
                    && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                return m;
            }
        }
        return null;
    }

    @Test void probeFindsTheApiClassByBossTrialsFirstName() throws Exception {
        Class<?> api = Class.forName("com.winkreyes.exceldeathprotection.ExcelDeathProtectionAPI");
        assertNotNull(probe(api), "BossTrial's probe must resolve a credit method");
    }

    @Test void probedMethodIsPublicStaticWithIntSecondParameter() throws Exception {
        Method two = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class, int.class);
        assertTrue(Modifier.isPublic(two.getModifiers()));
        assertTrue(Modifier.isStatic(two.getModifiers()));
        assertEquals(int.class, two.getParameterTypes()[1]);
        assertEquals(boolean.class, two.getReturnType());

        Method one = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class);
        assertTrue(Modifier.isPublic(one.getModifiers()));
        assertTrue(Modifier.isStatic(one.getModifiers()));
        assertEquals(boolean.class, one.getReturnType());
    }

    @Test void bossTrialInvokeShapesReturnFalseCleanlyWhenPluginIsDown() throws Exception {
        // No plugin instance exists in the unit environment: both of
        // BossTrial's invoke shapes must return Boolean.FALSE (a
        // clean skip), never throw — an instance method or a wrong
        // parameter type would fail right here.
        Player stub = (Player) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new UnsupportedOperationException(method.getName()); });
        Method one = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class);
        Method two = ExcelDeathProtectionAPI.class.getMethod("creditPremium", Player.class, int.class);
        assertEquals(Boolean.FALSE, one.invoke(null, stub));
        assertEquals(Boolean.FALSE, two.invoke(null, stub, 1));
        // Null player is likewise a clean false.
        assertEquals(Boolean.FALSE, two.invoke(null, null, 1));
    }
}
