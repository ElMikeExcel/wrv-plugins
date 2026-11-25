package excelemfixes.questglow;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Per-viewer entity glow, sent as raw entity-metadata packets.
 *
 * Why packets: an entity's glowing flag is global in the
 * Bukkit API (a GLOWING potion effect or setGlowing lights the
 * entity for everyone who can see it). The client, however,
 * renders the outline from the metadata IT holds for the
 * entity — and metadata is per-connection. Sending one viewer
 * a metadata packet whose shared-flags byte carries the glow
 * bit (0x40) lights the entity on that viewer's screen only;
 * every other viewer's client keeps the server's true flags.
 * The glow color follows the entity's team on the VIEWER's
 * scoreboard, which the module manages alongside.
 *
 * Mechanism notes:
 * - Only data index 0 (the shared flags byte) is ever sent,
 *   computed from the entity's live server-side flags at send
 *   time, so every other flag (invisible, on fire, ...) is
 *   preserved exactly, in both directions: lighting ORs the
 *   bit in, clearing ANDs it back out of the CURRENT flags —
 *   if the entity is genuinely glowing server-side (say a
 *   guide effect lit it), a clear from this module still
 *   carries the bit, because the server's own flags do.
 * - A later genuine metadata update from the server replaces
 *   the client's index 0 with the true flags; the module's
 *   refresh loop re-sends on its interval, which bounds any
 *   flicker to one interval.
 * - Everything here is reflection over the server's own
 *   classes (the pack's established bridge pattern — the same
 *   posture as the Geyser bridge), resolved once. If anything
 *   fails to resolve on a future server build, the bridge
 *   reports unavailable and the module idles: no glow, no
 *   errors, never a half-lit state.
 */
public final class GlowPackets {

    /** The glowing bit in the shared-flags byte (index 0). */
    public static final byte GLOW_BIT = 0x40;

    private static volatile boolean resolved;
    private static volatile boolean unavailable;

    private static Field sharedFlagsAccessorField;
    private static Object sharedFlagsAccessor;
    private static Method handleGetId;
    private static Method handleGetEntityData;
    private static Method synchedDataGet;
    private static Method dataValueCreate;
    private static Constructor<?> packetConstructor;
    private static Method connectionSend;

    private GlowPackets() {}

    public static boolean available() {
        if (unavailable) return false;
        if (!resolved) resolve();
        return !unavailable;
    }

    private static synchronized void resolve() {
        if (resolved || unavailable) return;
        try {
            Class<?> entityClass = Class.forName(
                    "net.minecraft.world.entity.Entity");
            sharedFlagsAccessorField = entityClass.getDeclaredField(
                    "DATA_SHARED_FLAGS_ID");
            sharedFlagsAccessorField.setAccessible(true);
            sharedFlagsAccessor = sharedFlagsAccessorField.get(null);

            Class<?> synchedDataClass = Class.forName(
                    "net.minecraft.network.syncher.SynchedEntityData");
            Class<?> accessorClass = Class.forName(
                    "net.minecraft.network.syncher.EntityDataAccessor");
            synchedDataGet = synchedDataClass.getMethod(
                    "get", accessorClass);

            Class<?> dataValueClass = Class.forName(
                    "net.minecraft.network.syncher.SynchedEntityData$DataValue");
            dataValueCreate = dataValueClass.getMethod(
                    "create", accessorClass, Object.class);

            Class<?> packetClass = Class.forName(
                    "net.minecraft.network.protocol.game."
                            + "ClientboundSetEntityDataPacket");
            packetConstructor = packetClass.getConstructor(
                    int.class, List.class);

            // getHandle / connection are looked up per object at
            // send time (runtime classes differ per entity type);
            // only the shapes that are class-stable resolve here.
            handleGetId = entityClass.getMethod("getId");
            handleGetEntityData = entityClass.getMethod("getEntityData");
            connectionSend = null;
            resolved = true;
        } catch (Throwable t) {
            unavailable = true;
        }
    }

    /**
     * Sends one viewer a metadata packet setting (on) or
     * clearing (off) the glow bit on one entity, preserving the
     * entity's current server-side flags otherwise. Returns
     * false when the packet could not be built or sent — the
     * caller treats that as "not lit" and retries next cycle.
     */
    public static boolean setGlowing(Player viewer, Entity target,
                                     boolean on) {
        if (!available()) return false;
        try {
            Object handle = handleOf(target);
            if (handle == null) return false;
            Object data = handleGetEntityData.invoke(handle);
            Object current = synchedDataGet.invoke(
                    data, sharedFlagsAccessor);
            if (!(current instanceof Byte flags)) return false;
            byte updated = on
                    ? (byte) (flags | GLOW_BIT)
                    : (byte) (flags & ~GLOW_BIT);
            Object value = dataValueCreate.invoke(
                    null, sharedFlagsAccessor, updated);
            int id = (Integer) handleGetId.invoke(handle);
            Object packet = packetConstructor.newInstance(
                    id, List.of(value));
            Object connection = connectionOf(viewer);
            if (connection == null) return false;
            Method send = sendMethod(connection.getClass());
            if (send == null) return false;
            send.invoke(connection, packet);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** The entity's current server-side shared-flags byte, or
     *  null when unreadable. Exposed for diagnostics/tests. */
    public static Byte currentFlags(Entity target) {
        if (!available()) return null;
        try {
            Object handle = handleOf(target);
            if (handle == null) return null;
            Object data = handleGetEntityData.invoke(handle);
            Object current = synchedDataGet.invoke(
                    data, sharedFlagsAccessor);
            return current instanceof Byte b ? b : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object handleOf(Entity entity) throws Exception {
        Method m = entity.getClass().getMethod("getHandle");
        return m.invoke(entity);
    }

    private static Object connectionOf(Player viewer) throws Exception {
        Object handle = handleOf(viewer);
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

    private static Method sendMethod(Class<?> connectionClass) {
        if (connectionSend != null
                && connectionSend.getDeclaringClass()
                        .isAssignableFrom(connectionClass)) {
            return connectionSend;
        }
        try {
            Class<?> packetInterface = Class.forName(
                    "net.minecraft.network.protocol.Packet");
            Method m = connectionClass.getMethod(
                    "send", packetInterface);
            connectionSend = m;
            return m;
        } catch (Throwable t) {
            return null;
        }
    }
}
