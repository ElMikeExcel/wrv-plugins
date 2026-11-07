package com.winkreyes.excelclaims;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/** Shared headless fixtures: an engine over a temp store, proxied Bukkit types. */
public final class TestKit {

    private TestKit() { }

    public static Core newCore(Path dir) throws Exception {
        ClaimsConfig cfg = new ClaimsConfig();
        cfg.worldModes.put("world", ClaimsConfig.WorldMode.SURVIVAL);
        ClaimStore store = new ClaimStore(Logger.getLogger("excelclaims-test"), cfg);
        store.load(dir.toFile());
        return Core.forTests(Logger.getLogger("excelclaims-test"), store);
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(TestKit.class.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    public static Player fakePlayer(UUID id, String name, Set<String> perms) {
        return proxy(Player.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return id;
                case "getName": return name;
                case "isOnline": return true;
                case "hasPermission":
                    if (args[0] instanceof String s) return perms.contains(s);
                    try {
                        Object perm = args[0];
                        String n = (String) perm.getClass().getMethod("getName").invoke(perm);
                        return perms.contains(n);
                    } catch (Exception e) { return false; }
                case "toString": return "FakePlayer[" + name + "]";
                case "hashCode": return id.hashCode();
                case "equals": return proxy == args[0];
                default: return defaultFor(method.getReturnType());
            }
        });
    }

    public static World fakeWorld(String name) {
        UUID uid = UUID.nameUUIDFromBytes(name.getBytes());
        return proxy(World.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getName": return name;
                case "getUID": return uid;
                case "toString": return "FakeWorld[" + name + "]";
                case "hashCode": return uid.hashCode();
                case "equals": return proxy == args[0];
                default: return defaultFor(method.getReturnType());
            }
        });
    }

    static Object defaultFor(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}

// ExcelClaims tests — Iris / Mike
