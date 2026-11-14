package com.winkreyes.excelwandbars;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 0.2.7: Bedrock "tap the wand" for Tap bar. A Bedrock player who taps the hotbar slot that is
 * already selected sends the slot again, but Geyser drops it on purpose (GeyserSession.switchHeldSlot
 * returns when the slot is the same, to match Java, where pressing the selected key sends nothing).
 * This wraps Geyser's own translator for that packet: every packet still goes to Geyser's translator
 * unchanged, in the same order; we NOTE main-hotbar selections and validate actual wand state on the main thread.
 * Loaded only when Geyser-Spigot is running (by name, so the plugin starts without Geyser), and
 * Geyser's translator is put back when this plugin stops.
 */
public final class GeyserTapHook {
    private static Wrapper installed;

    private GeyserTapHook() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized String install(BiConsumer<UUID, Integer> onSameSlot) {
        if (installed != null) { installed.callback = onSameSlot; return "already"; }
        PacketTranslator current = Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class);
        if (current == null) return "no Geyser translator";
        if (current.getClass().getName().equals(Wrapper.class.getName())) return "wrapped by an older copy";
        Wrapper w = new Wrapper((PacketTranslator<MobEquipmentPacket>) current, onSameSlot);
        ((org.geysermc.geyser.registry.AbstractMappedRegistry) Registries.BEDROCK_PACKET_TRANSLATORS).register(MobEquipmentPacket.class, w);
        installed = w;
        return "installed";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized String uninstall() {
        if (installed == null) return "none";
        Object now = Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class);
        installed.callback = null;
        if (now == installed) {
            ((org.geysermc.geyser.registry.AbstractMappedRegistry) Registries.BEDROCK_PACKET_TRANSLATORS).register(MobEquipmentPacket.class, installed.original);
            installed = null;
            return "restored";
        }
        installed = null;
        return "replaced by someone else, left alone";
    }

    static final class Wrapper extends PacketTranslator<MobEquipmentPacket> {
        final PacketTranslator<MobEquipmentPacket> original;
        volatile BiConsumer<UUID, Integer> callback;

        Wrapper(PacketTranslator<MobEquipmentPacket> original, BiConsumer<UUID, Integer> callback) {
            this.original = original;
            this.callback = callback;
        }

        @Override
        public void translate(GeyserSession session, MobEquipmentPacket packet) {
            try {
                BiConsumer<UUID, Integer> cb = callback;
                if (cb != null && packet.getContainerId() == 0) {
                    int slot = packet.getHotbarSlot();
                    // Magic can retain the wand slot on Bukkit while Geyser temporarily
                    // retains the spell slot it just forwarded. Observe every main-hotbar
                    // selection; TapBarHook validates the actual active wand and held slot
                    // on the server thread. Filtering by Geyser's cache loses real wand taps.
                    if (slot >= 0 && slot <= 8) {
                        UUID id = session.javaUuid();
                        if (id != null) cb.accept(id, slot);
                    }
                }
            } catch (Throwable ignored) {
                // never let our note break Geyser's own handling
            }
            original.translate(session, packet);
        }

        @Override
        public boolean shouldExecuteInEventLoop() { return original.shouldExecuteInEventLoop(); }
    }
}

