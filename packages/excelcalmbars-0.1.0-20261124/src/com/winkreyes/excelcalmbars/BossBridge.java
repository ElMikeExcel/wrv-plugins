package com.winkreyes.excelcalmbars;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Reflection bridge over the server's boss-bar packet internals.
 *
 * Why reflection: the gate works on the clientbound boss-event
 * packet, an internal class with no Bukkit API surface. This follows
 * the pack's established bridge posture (the ExcelEMFixes glow
 * bridge): resolve every class/field/method once, and if anything
 * fails to resolve on a future server build the bridge reports
 * unavailable and the gate fails OPEN — every bar passes, exactly
 * the pre-plugin behaviour, never a half-gated state.
 *
 * Verified against the live build's server jar (Paper 26.3):
 * ClientboundBossEventPacket carries a UUID id + an Operation whose
 * getType() names ADD / REMOVE / UPDATE_PROGRESS / UPDATE_NAME /
 * UPDATE_STYLE / UPDATE_PROPERTIES; the operation classes hold the
 * display state in same-named fields; ServerBossEvent has a public
 * (UUID, Component, color, overlay) constructor and the packet class
 * has public static create*Packet factories, so a held bar can be
 * rebuilt exactly from its tracked state at release time.
 */
public final class BossBridge {

    private static volatile boolean resolved;
    private static volatile boolean unavailable;

    private static Class<?> packetClass;
    private static Field packetIdField;
    private static Field packetOperationField;
    private static Method operationGetType;
    private static Method componentGetString;
    private static Class<?> bossEventClass;
    private static Constructor<?> serverBossEventConstructor;
    private static Method bossSetName;
    private static Method bossSetProgress;
    private static Method bossSetDarken;
    private static Method bossSetMusic;
    private static Method bossSetFog;
    private static Method createAdd;
    private static Method createRemove;
    private static Method createUpdateProgress;
    private static Method createUpdateName;
    private static Method createUpdateStyle;
    private static Method createUpdateProperties;
    private static Method connectionSend;

    private BossBridge() {}

    public static boolean available() {
        if (unavailable) return false;
        if (!resolved) resolve();
        return !unavailable;
    }

    private static synchronized void resolve() {
        if (resolved || unavailable) return;
        try {
            packetClass = Class.forName(
                    "net.minecraft.network.protocol.game."
                            + "ClientboundBossEventPacket");
            packetIdField = packetClass.getDeclaredField("id");
            packetIdField.setAccessible(true);
            packetOperationField =
                    packetClass.getDeclaredField("operation");
            packetOperationField.setAccessible(true);

            Class<?> operationClass = Class.forName(
                    "net.minecraft.network.protocol.game."
                            + "ClientboundBossEventPacket$Operation");
            operationGetType = operationClass.getMethod("getType");
            // Operation is a package-private interface: its public
            // method still needs an access override from our package.
            operationGetType.setAccessible(true);

            Class<?> componentClass = Class.forName(
                    "net.minecraft.network.chat.Component");
            componentGetString = componentClass.getMethod("getString");

            bossEventClass = Class.forName(
                    "net.minecraft.world.BossEvent");
            Class<?> colorClass = Class.forName(
                    "net.minecraft.world.BossEvent$BossBarColor");
            Class<?> overlayClass = Class.forName(
                    "net.minecraft.world.BossEvent$BossBarOverlay");
            Class<?> serverBossEventClass = Class.forName(
                    "net.minecraft.server.level.ServerBossEvent");
            serverBossEventConstructor =
                    serverBossEventClass.getConstructor(
                            UUID.class, componentClass,
                            colorClass, overlayClass);
            bossSetName = bossEventClass.getMethod(
                    "setName", componentClass);
            bossSetProgress = bossEventClass.getMethod(
                    "setProgress", float.class);
            bossSetDarken = bossEventClass.getMethod(
                    "setDarkenScreen", boolean.class);
            bossSetMusic = bossEventClass.getMethod(
                    "setPlayBossMusic", boolean.class);
            bossSetFog = bossEventClass.getMethod(
                    "setCreateWorldFog", boolean.class);

            createAdd = packetClass.getMethod(
                    "createAddPacket", bossEventClass);
            createRemove = packetClass.getMethod(
                    "createRemovePacket", UUID.class);
            createUpdateProgress = packetClass.getMethod(
                    "createUpdateProgressPacket", bossEventClass);
            createUpdateName = packetClass.getMethod(
                    "createUpdateNamePacket", bossEventClass);
            createUpdateStyle = packetClass.getMethod(
                    "createUpdateStylePacket", bossEventClass);
            createUpdateProperties = packetClass.getMethod(
                    "createUpdatePropertiesPacket", bossEventClass);
            resolved = true;
        } catch (Throwable t) {
            unavailable = true;
        }
    }

    public static boolean isBossPacket(Object msg) {
        return available() && packetClass.isInstance(msg);
    }

    public static UUID barId(Object packet) throws Exception {
        return (UUID) packetIdField.get(packet);
    }

    /** ADD / REMOVE / UPDATE_PROGRESS / UPDATE_NAME / UPDATE_STYLE /
     *  UPDATE_PROPERTIES — the operation's type name. */
    public static String opType(Object packet) throws Exception {
        Object operation = packetOperationField.get(packet);
        Object type = operationGetType.invoke(operation);
        return ((Enum<?>) type).name();
    }

    private static Object operation(Object packet) throws Exception {
        return packetOperationField.get(packet);
    }

    private static Object readField(Object target, String name)
            throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /** Plain-text title carried by this packet, or null when the
     *  packet carries no name (progress/style/properties updates). */
    public static String packetTitle(Object packet, String type)
            throws Exception {
        Object name = packetNameComponent(packet, type);
        return name == null ? null : plainText(name);
    }

    public static Object packetNameComponent(Object packet, String type)
            throws Exception {
        if (!"ADD".equals(type) && !"UPDATE_NAME".equals(type)) {
            return null;
        }
        return readField(operation(packet), "name");
    }

    public static String plainText(Object component) throws Exception {
        Object text = componentGetString.invoke(component);
        return text == null ? null : text.toString();
    }

    /**
     * Merges a packet's display state into the tracked bar state.
     * Called for every boss packet, held or passing, so the tracked
     * state is always the bar's latest known state.
     */
    public static void merge(Object packet, String type, BarState state)
            throws Exception {
        Object operation = operation(packet);
        switch (type) {
            case "ADD" -> {
                state.nameComponent = readField(operation, "name");
                state.plainTitle = plainText(state.nameComponent);
                state.progress =
                        (Float) readField(operation, "progress");
                state.color = readField(operation, "color");
                state.overlay = readField(operation, "overlay");
                state.darkenScreen =
                        (Boolean) readField(operation, "darkenScreen");
                state.playMusic =
                        (Boolean) readField(operation, "playMusic");
                state.createWorldFog =
                        (Boolean) readField(operation, "createWorldFog");
                state.hasFullState = true;
            }
            case "UPDATE_PROGRESS" -> state.progress =
                    (Float) readField(operation, "progress");
            case "UPDATE_NAME" -> {
                state.nameComponent = readField(operation, "name");
                state.plainTitle = plainText(state.nameComponent);
            }
            case "UPDATE_STYLE" -> {
                state.color = readField(operation, "color");
                state.overlay = readField(operation, "overlay");
            }
            case "UPDATE_PROPERTIES" -> {
                state.darkenScreen =
                        (Boolean) readField(operation, "darkenScreen");
                state.playMusic =
                        (Boolean) readField(operation, "playMusic");
                state.createWorldFog =
                        (Boolean) readField(operation, "createWorldFog");
            }
            default -> { /* REMOVE carries no display state */ }
        }
    }

    private static Object fabricateEvent(BarState state)
            throws Exception {
        Object event = serverBossEventConstructor.newInstance(
                state.id, state.nameComponent, state.color,
                state.overlay);
        bossSetProgress.invoke(event, state.progress);
        bossSetDarken.invoke(event, state.darkenScreen);
        bossSetMusic.invoke(event, state.playMusic);
        bossSetFog.invoke(event, state.createWorldFog);
        return event;
    }

    /** A fresh ADD packet rebuilding the bar from tracked state, or
     *  null when the state is not complete enough to rebuild. */
    public static Object buildAdd(BarState state) {
        if (!state.hasFullState || state.nameComponent == null
                || state.color == null || state.overlay == null) {
            return null;
        }
        try {
            return createAdd.invoke(null, fabricateEvent(state));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Update packets refreshing a bar the client already has, or
     *  null when the state cannot be rebuilt. */
    public static List<Object> buildUpdates(BarState state) {
        if (!state.hasFullState || state.nameComponent == null
                || state.color == null || state.overlay == null) {
            return null;
        }
        try {
            Object event = fabricateEvent(state);
            List<Object> packets = new ArrayList<>(4);
            packets.add(createUpdateProgress.invoke(null, event));
            packets.add(createUpdateName.invoke(null, event));
            packets.add(createUpdateStyle.invoke(null, event));
            packets.add(createUpdateProperties.invoke(null, event));
            return packets;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object buildRemove(UUID barId) {
        try {
            return createRemove.invoke(null, barId);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- connection access (the glow bridge's proven route) ----

    private static Object handleOf(Player player) throws Exception {
        Method m = player.getClass().getMethod("getHandle");
        return m.invoke(player);
    }

    private static Object listenerOf(Player player) throws Exception {
        Object handle = handleOf(player);
        if (handle == null) return null;
        Class<?> c = handle.getClass();
        while (c != null) {
            try {
                Field f = c.getField("connection");
                return f.get(handle);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Packets this bridge sends itself (release/retract traffic) must
     * not be re-gated by our own handler when they cross the pipeline
     * — a max-hold release fires while the player is still in combat,
     * and the gate would swallow its own release. Every packet passed
     * to {@link #send} is registered here by identity; the handler
     * consumes the mark and passes the packet unconditionally.
     */
    private static final Set<Object> BYPASS = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    /** True (once) when this packet instance is bridge-sent traffic. */
    public static boolean consumeBypass(Object packet) {
        return BYPASS.remove(packet);
    }

    /** Sends one packet to the player (release/retract path); the
     *  packet is marked to bypass our own gate. Returns false on any
     *  failure. */
    public static boolean send(Player player, Object packet) {
        if (!available() || packet == null) return false;
        try {
            Object listener = listenerOf(player);
            if (listener == null) return false;
            Method send = sendMethod(listener.getClass());
            if (send == null) return false;
            BYPASS.add(packet);
            send.invoke(listener, packet);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Method sendMethod(Class<?> listenerClass) {
        if (connectionSend != null
                && connectionSend.getDeclaringClass()
                        .isAssignableFrom(listenerClass)) {
            return connectionSend;
        }
        try {
            Class<?> packetInterface = Class.forName(
                    "net.minecraft.network.protocol.Packet");
            Method m = listenerClass.getMethod("send", packetInterface);
            connectionSend = m;
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The player's server-side Connection object (the pipeline's
     *  packet handler), or null when unreachable. */
    public static Object connectionOf(Player player) {
        try {
            Object listener = listenerOf(player);
            if (listener == null) return null;
            Field connectionField =
                    findPublicField(listener.getClass(), "connection");
            if (connectionField == null) return null;
            return connectionField.get(listener);
        } catch (Throwable t) {
            return null;
        }
    }

    /** The player's Netty channel, for gate injection. */
    public static Channel channelOf(Player player) {
        try {
            Object connection = connectionOf(player);
            if (connection == null) return null;
            Field channelField =
                    findPublicField(connection.getClass(), "channel");
            if (channelField == null) return null;
            Object channel = channelField.get(connection);
            return channel instanceof Channel ch ? ch : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field findPublicField(Class<?> c, String name) {
        while (c != null) {
            try {
                return c.getField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
