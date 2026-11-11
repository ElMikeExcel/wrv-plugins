package com.winkreyes.buildmode.paper;

import java.io.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.winkreyes.buildmode.core.RecoveryJournal;

/** Explicit fields only: never copies broad PDC, balance, Jobs progress, health, food or effects. */
record SurvivalSnapshot(ItemStack[] storage, ItemStack[] armor, ItemStack[] offhand, ItemStack[] ender,
                        int level, int totalXp, float progress, int heldSlot,
                        boolean allowFlight, boolean flying, float flySpeed, float walkSpeed) {
    static byte[] capture(Player p) throws IOException {
        if (p.getGameMode() != GameMode.SURVIVAL) throw new IOException("Start from Survival");
        var inv = p.getInventory();
        SurvivalSnapshot s = new SurvivalSnapshot(inv.getStorageContents(), inv.getArmorContents(),
            new ItemStack[]{inv.getItemInOffHand()}, p.getEnderChest().getContents(),
            p.getLevel(), p.getTotalExperience(), p.getExp(), inv.getHeldItemSlot(),
            p.getAllowFlight(), p.isFlying(), p.getFlySpeed(), p.getWalkSpeed());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1); out.writeUTF(GameMode.SURVIVAL.name());
            writeItems(out, s.storage); writeItems(out, s.armor); writeItems(out, s.offhand); writeItems(out, s.ender);
            out.writeInt(s.level); out.writeInt(s.totalXp); out.writeFloat(s.progress); out.writeInt(s.heldSlot);
            out.writeBoolean(s.allowFlight); out.writeBoolean(s.flying); out.writeFloat(s.flySpeed); out.writeFloat(s.walkSpeed);
        }
        return bytes.toByteArray();
    }
    static SurvivalSnapshot decode(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 1 || !in.readUTF().equals("SURVIVAL")) throw new IOException("Unknown snapshot version/mode");
            var result = new SurvivalSnapshot(readItems(in, 36), readItems(in, 4), readItems(in, 1), readItems(in, 27),
                in.readInt(), in.readInt(), in.readFloat(), in.readInt(), in.readBoolean(), in.readBoolean(), in.readFloat(), in.readFloat());
            if (in.read() != -1 || result.level < 0 || result.totalXp < 0 || !Float.isFinite(result.progress)
                || result.progress < 0 || result.progress > 1 || result.heldSlot < 0 || result.heldSlot > 8
                || !validSpeed(result.flySpeed) || !validSpeed(result.walkSpeed) || (result.flying && !result.allowFlight))
                throw new IOException("Invalid snapshot values");
            return result;
        } catch (RuntimeException e) { throw new IOException("Cannot decode recovery items", e); }
    }
    void restore(Player p) {
        p.getInventory().setStorageContents(storage); p.getInventory().setArmorContents(armor);
        p.getInventory().setItemInOffHand(offhand[0]); p.getEnderChest().setContents(ender);
        p.getInventory().setHeldItemSlot(heldSlot);
        p.setExp(0); p.setLevel(0); p.setTotalExperience(0);
        p.setLevel(level); p.setTotalExperience(totalXp); p.setExp(progress);
        p.setAllowFlight(allowFlight); p.setFlying(flying); p.setFlySpeed(flySpeed); p.setWalkSpeed(walkSpeed);
        p.setFallDistance(0); p.updateInventory();
    }
    private static boolean validSpeed(float f) { return Float.isFinite(f) && f >= -1 && f <= 1; }
    private static void writeItems(DataOutputStream out, ItemStack[] items) throws IOException {
        byte[] bytes = ItemStack.serializeItemsAsBytes(items); out.writeInt(bytes.length); out.write(bytes);
    }
    private static ItemStack[] readItems(DataInputStream in, int expected) throws IOException {
        int size = in.readInt();
        if (size < 1 || size > RecoveryJournal.MAX_BYTES || size > in.available()) throw new IOException("Invalid item byte count");
        var items = ItemStack.deserializeItemsFromBytes(in.readNBytes(size));
        if (items.length != expected) throw new IOException("Unexpected inventory size");
        return items;
    }
}
