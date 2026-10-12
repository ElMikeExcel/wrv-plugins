/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.winkreyes.bedrockmenu;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class GeyserForms {
    private GeyserForms() {
    }

    static boolean isBedrockPlayer(Player player, Logger logger) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            Class<?> clazz = Class.forName("org.geysermc.geyser.api.GeyserApi", true, classLoader);
            Object object = clazz.getMethod("api", new Class[0]).invoke(null, new Object[0]);
            Object object2 = clazz.getMethod("isBedrockPlayer", UUID.class).invoke(object, player.getUniqueId());
            return Boolean.TRUE.equals(object2);
        }
        catch (Exception exception) {
            return false;
        }
    }

    static boolean sendSimpleForm(Player player, String string, String string2, String[] stringArray, IntConsumer intConsumer, Logger logger) {
        String string3 = "start";
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            UUID uUID = player.getUniqueId();
            string3 = "GeyserApi.api()";
            Class<?> clazz = Class.forName("org.geysermc.geyser.api.GeyserApi", true, classLoader);
            Object object2 = clazz.getMethod("api", new Class[0]).invoke(null, new Object[0]);
            string3 = "connectionByUuid";
            Object object3 = clazz.getMethod("connectionByUuid", UUID.class).invoke(object2, uUID);
            if (object3 == null) {
                return false;
            }
            string3 = "SimpleForm.builder()";
            Class<?> clazz2 = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, classLoader);
            Object object4 = clazz2.getMethod("builder", new Class[0]).invoke(null, new Object[0]);
            Class<?> clazz3 = object4.getClass();
            string3 = "title/content/buttons";
            clazz3.getMethod("title", String.class).invoke(object4, string);
            clazz3.getMethod("content", String.class).invoke(object4, string2);
            Method method2 = clazz3.getMethod("button", String.class);
            for (String string4 : stringArray) {
                method2.invoke(object4, string4);
            }
            string3 = "validResultHandler";
            Class<?> clazz4 = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, classLoader);
            Class<?> clazz5 = Class.forName("java.util.function.BiConsumer", true, classLoader);
            Object object5 = Proxy.newProxyInstance(classLoader, new Class[]{clazz5}, (object, method, objectArray) -> {
                if (method.getName().equals("accept") && objectArray.length == 2) {
                    Object object2 = objectArray[1];
                    int n = (Integer)clazz4.getMethod("clickedButtonId", new Class[0]).invoke(object2, new Object[0]);
                    intConsumer.accept(n);
                }
                return null;
            });
            Method method3 = GeyserForms.findHandler(clazz3, "validResultHandler", object5);
            if (method3 == null) {
                logger.warning("GeyserForms: no compatible validResultHandler (SimpleForm)");
                return false;
            }
            method3.invoke(object4, object5);
            string3 = "build+send";
            Object object6 = clazz3.getMethod("build", new Class[0]).invoke(object4, new Object[0]);
            Method method4 = GeyserForms.findSendForm(object3.getClass(), object6.getClass());
            if (method4 == null) {
                logger.warning("GeyserForms: no compatible sendForm (SimpleForm)");
                return false;
            }
            return (Boolean)method4.invoke(object3, object6);
        }
        catch (Exception exception) {
            logger.warning("GeyserForms SimpleForm '" + string + "' failed at [" + string3 + "]: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    static boolean sendModalForm(Player player, String string, String string2, String string3, String string4, Consumer<Boolean> consumer, Logger logger) {
        String string5 = "start";
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            UUID uUID = player.getUniqueId();
            string5 = "GeyserApi.api()";
            Class<?> clazz = Class.forName("org.geysermc.geyser.api.GeyserApi", true, classLoader);
            Object object2 = clazz.getMethod("api", new Class[0]).invoke(null, new Object[0]);
            string5 = "connectionByUuid";
            Object object3 = clazz.getMethod("connectionByUuid", UUID.class).invoke(object2, uUID);
            if (object3 == null) {
                return false;
            }
            string5 = "ModalForm.builder()";
            Class<?> clazz2 = Class.forName("org.geysermc.cumulus.form.ModalForm", true, classLoader);
            Object object4 = clazz2.getMethod("builder", new Class[0]).invoke(null, new Object[0]);
            Class<?> clazz3 = object4.getClass();
            string5 = "title/content/buttons";
            clazz3.getMethod("title", String.class).invoke(object4, string);
            clazz3.getMethod("content", String.class).invoke(object4, string2);
            clazz3.getMethod("button1", String.class).invoke(object4, string3);
            clazz3.getMethod("button2", String.class).invoke(object4, string4);
            string5 = "validResultHandler";
            Class<?> clazz4 = Class.forName("org.geysermc.cumulus.response.ModalFormResponse", true, classLoader);
            Class<?> clazz5 = Class.forName("java.util.function.BiConsumer", true, classLoader);
            Object object5 = Proxy.newProxyInstance(classLoader, new Class[]{clazz5}, (object, method, objectArray) -> {
                if (method.getName().equals("accept") && objectArray.length == 2) {
                    Object object2 = objectArray[1];
                    boolean bl = (Boolean)clazz4.getMethod("clickedFirst", new Class[0]).invoke(object2, new Object[0]);
                    consumer.accept(bl);
                }
                return null;
            });
            Method method2 = GeyserForms.findHandler(clazz3, "validResultHandler", object5);
            if (method2 == null) {
                logger.warning("GeyserForms: no compatible validResultHandler (ModalForm)");
                return false;
            }
            method2.invoke(object4, object5);
            string5 = "build+send";
            Object object6 = clazz3.getMethod("build", new Class[0]).invoke(object4, new Object[0]);
            Method method3 = GeyserForms.findSendForm(object3.getClass(), object6.getClass());
            if (method3 == null) {
                logger.warning("GeyserForms: no compatible sendForm (ModalForm)");
                return false;
            }
            return (Boolean)method3.invoke(object3, object6);
        }
        catch (Exception exception) {
            logger.warning("GeyserForms ModalForm '" + string + "' failed at [" + string5 + "]: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static Method findHandler(Class<?> clazz, String string, Object object) {
        for (Method method : clazz.getMethods()) {
            Class<?> clazz2;
            if (!method.getName().equals(string) || method.getParameterCount() != 1 || !(clazz2 = method.getParameterTypes()[0]).isAssignableFrom(object.getClass())) continue;
            return method;
        }
        return null;
    }

    private static Method findSendForm(Class<?> clazz, Class<?> clazz2) {
        for (Method method : clazz.getMethods()) {
            Class<?> clazz3;
            if (!method.getName().equals("sendForm") || method.getParameterCount() != 1 || !(clazz3 = method.getParameterTypes()[0]).isAssignableFrom(clazz2)) continue;
            return method;
        }
        return null;
    }
}

