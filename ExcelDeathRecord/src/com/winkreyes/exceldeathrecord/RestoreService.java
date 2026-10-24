package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.RecordFormat;
import com.winkreyes.exceldeathrecord.core.BurnFormat;
import com.winkreyes.exceldeathrecord.core.RecordIndex;
import com.winkreyes.exceldeathrecord.core.RestoreCsv;
import com.winkreyes.exceldeathrecord.core.Selection;
import com.winkreyes.exceldeathrecord.core.SlotMatch;
import com.winkreyes.exceldeathrecord.core.SlotPlanner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The one restore path, shared by the typed command and the tap
 * menu. All-or-nothing: the record is re-read, the selection
 * checked against restore marks, every byte-exact copy rebuilt,
 * and the target's free slots planned BEFORE anything changes.
 * Only when everything fits are the marks written (first, so a
 * crash can never allow a double restore), then the items given,
 * then restores.csv appended. A refusal changes nothing.
 */
final class RestoreService {

    /** Where the items come from. */
    static final class Source {
        boolean burned;
        File file;
        String sourceName = "";   // the dead player / burned owner ("" = unknown)
        UUID sourceUuid;          // null for burned/unknown
    }

    /** What a restore would do (or why it can't). */
    static final class Prepared {
        String error;                         // non-null = refused
        final List<String> notes = new ArrayList<>();
        Source src;
        DeathRecord death;                    // when !burned
        DestructionRecord burn;               // when burned
        List<DeathRecord.ItemEntry> list;     // the numbered list
        final List<Integer> numbers = new ArrayList<>();          // will be given
        final List<ItemStack> stacks = new ArrayList<>();
        SlotPlanner.Plan plan;
        boolean samePlayer;
        boolean xpWanted;                     // xp will be given
        boolean xpOffered;                    // an XP option makes sense (confirm screen)
        int xpPoints;
        Player target;
        boolean force;
        boolean ok() { return error == null; }
    }

    private final ExcelDeathRecord plugin;
    private final RecordStore store;

    RestoreService(ExcelDeathRecord plugin, RecordStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    static ItemStack rebuild(DeathRecord.ItemEntry e) {
        if (e.bytesB64 == null || e.bytesB64.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(e.bytesB64));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Display name for messages: custom name, else "Netherite Shovel". */
    static String pretty(DeathRecord.ItemEntry e) {
        String base = e.customName.isEmpty() ? prettyId(e.id) : e.customName;
        return (e.count > 1 ? e.count + "x " : "") + base;
    }

    static String prettyId(String id) {
        String t = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        for (String w : t.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String keyOf(ItemStack s) {
        ItemStack one = s.clone();
        one.setAmount(1);
        try { return Base64.getEncoder().encodeToString(one.serializeAsBytes()); }
        catch (Throwable t) { return s.getType().name() + "#" + one.hashCode(); }
    }

    /** Build the plan without changing anything. selection = Selection text, or null with explicit numbers. */
    Prepared prepare(Source src, String selection, List<Integer> explicit, Player target, boolean force, boolean xp) {
        Prepared p = new Prepared();
        p.src = src; p.target = target; p.force = force;
        if (target == null || !target.isOnline()) { p.error = "The player who gets the items must be online."; return p; }
        try {
            if (src.burned) {
                p.burn = store.readBurnFile(src.file);
                p.list = List.of(p.burn.item);
            } else {
                p.death = store.readDeathFile(src.file);
                p.list = RecordIndex.restorable(p.death);
            }
        } catch (Exception e) {
            p.error = "Could not read " + store.relative(src.file) + ": " + e.getMessage();
            return p;
        }
        p.samePlayer = !src.burned && src.sourceUuid != null && src.sourceUuid.equals(target.getUniqueId());

        List<Integer> wanted;
        if (explicit != null) {
            wanted = explicit;
        } else {
            Selection.Result sel = Selection.parse(selection, p.list.size());
            if (!sel.ok()) { p.error = sel.error; return p; }
            wanted = sel.numbers;
        }
        if (!src.burned && RecordIndex.restorableIsCarried(p.death) && !force && !wanted.isEmpty()) {
            p.error = "Nothing dropped on that death (keepInventory or a plugin kept the items), so they may still have them. Add force to give copies anyway.";
            return p;
        }

        List<String> skipped = new ArrayList<>();
        for (int n : wanted) {
            if (n < 1 || n > p.list.size()) { p.error = "Item " + n + " is out of range: this record has items 1-" + p.list.size() + "."; return p; }
            DeathRecord.ItemEntry e = p.list.get(n - 1);
            if (e.isRestored() && !force) { skipped.add("#" + n + " " + pretty(e) + RecordIndex.restoredNote(e)); continue; }
            ItemStack s = rebuild(e);
            if (s == null || s.getType() == Material.AIR) {
                p.error = "Item #" + n + " (" + pretty(e) + ") has no saved exact copy, so it can't be restored. Nothing was given.";
                return p;
            }
            p.numbers.add(n);
            p.stacks.add(s);
        }

        // XP: only back to the same player, only if the record has some.
        int xpPts = src.burned ? 0 : com.winkreyes.exceldeathrecord.core.XpMath.restorePoints(p.death.xpLevel, p.death.totalExperience);
        if (!src.burned && xpPts > 0 && p.samePlayer) {
            p.xpOffered = force || p.death.xpRestoredTimes == 0;
            p.xpPoints = xpPts;
            if (xp) {
                if (p.xpOffered) p.xpWanted = true;
                else p.notes.add("XP was already restored to " + p.death.xpRestoredTo + " " + p.death.xpRestoredAt + ". Add force to give it again.");
            }
        } else if (xp) {
            p.notes.add(src.burned ? "Burn Ledger records have no XP." : !p.samePlayer
                    ? "XP only goes back to the player who died, so it was left out."
                    : "That death recorded no XP.");
        }

        if (!skipped.isEmpty()) {
            if (p.numbers.isEmpty() && !p.xpWanted) {
                p.error = "Already restored: " + String.join("; ", skipped) + ". Add force to give it again.";
                return p;
            }
            p.notes.add("Skipped (already restored, add force to repeat): " + String.join("; ", skipped));
        }
        if (p.numbers.isEmpty() && !p.xpWanted) { p.error = "Nothing to restore."; return p; }

        // Plan slots, all-or-nothing.
        List<String> origin;
        if (src.burned) origin = List.of("");
        else if (RecordIndex.restorableIsCarried(p.death)) {
            origin = new ArrayList<>();
            for (DeathRecord.ItemEntry e : p.list) origin.add(e.slot);
        } else origin = SlotMatch.originalSlots(p.death.drops, p.death.inventory);

        PlayerInventory inv = target.getInventory();
        List<SlotPlanner.Slot> storage = new ArrayList<>();
        for (ItemStack s : inv.getStorageContents()) {
            if (s == null || s.getType().isAir()) storage.add(SlotPlanner.Slot.empty());
            else storage.add(new SlotPlanner.Slot(keyOf(s), s.getAmount(), s.getMaxStackSize()));
        }
        List<String> emptyEquip = new ArrayList<>();
        if (p.samePlayer) {
            if (isEmpty(inv.getHelmet())) emptyEquip.add("helmet");
            if (isEmpty(inv.getChestplate())) emptyEquip.add("chestplate");
            if (isEmpty(inv.getLeggings())) emptyEquip.add("leggings");
            if (isEmpty(inv.getBoots())) emptyEquip.add("boots");
            if (isEmpty(inv.getItemInOffHand())) emptyEquip.add("offhand");
        }
        List<SlotPlanner.Want> wants = new ArrayList<>();
        for (int i = 0; i < p.numbers.size(); i++) {
            ItemStack s = p.stacks.get(i);
            String slot = origin.size() >= p.numbers.get(i) ? origin.get(p.numbers.get(i) - 1) : "";
            wants.add(new SlotPlanner.Want(keyOf(s), s.getAmount(), s.getMaxStackSize(),
                    SlotMatch.isEquip(slot) ? slot : ""));
        }
        p.plan = SlotPlanner.plan(storage, emptyEquip, wants);
        return p;
    }

    private static boolean isEmpty(ItemStack s) { return s == null || s.getType().isAir(); }

    /** Prepare + (if it fits) do it. Returns the lines to show staff. */
    List<String> restore(String staffName, Source src, String selection, List<Integer> explicit,
                         Player target, boolean force, boolean xp) {
        List<String> out = new ArrayList<>();
        Prepared p = prepare(src, selection, explicit, target, force, xp);
        if (!p.ok()) { out.add(p.error); return out; }
        if (!p.plan.fits()) {
            out.add(SlotPlanner.refusal(target.getName(), p.plan));
            out.add("Nothing was given or marked.");
            return out;
        }

        long now = System.currentTimeMillis();
        String at = RestoreCsv.markTime(now);
        for (int n : p.numbers) {
            DeathRecord.ItemEntry e = p.list.get(n - 1);
            e.restoredTimes++;
            e.restoredTo = target.getName();
            e.restoredToUuid = target.getUniqueId().toString();
            e.restoredBy = staffName;
            e.restoredEpoch = now;
            e.restoredAt = at;
        }
        if (p.xpWanted) {
            p.death.xpRestoredTimes++;
            p.death.xpRestoredTo = target.getName();
            p.death.xpRestoredBy = staffName;
            p.death.xpRestoredAt = at;
        }
        try {
            store.rewrite(src.file, src.burned ? BurnFormat.write(p.burn) : RecordFormat.write(p.death));
        } catch (Exception e) {
            out.add("Could not save the restore mark (" + e.getMessage() + "). Nothing was given.");
            return out;
        }

        // Give: equipment slots first where planned, then storage.
        PlayerInventory inv = target.getInventory();
        Map<Integer, ItemStack> leftovers = new HashMap<>();
        for (int i = 0; i < p.numbers.size(); i++) {
            ItemStack s = p.stacks.get(i);
            String eq = p.plan.equipPlaced.get(i);
            switch (eq) {
                case "helmet" -> inv.setHelmet(s);
                case "chestplate" -> inv.setChestplate(s);
                case "leggings" -> inv.setLeggings(s);
                case "boots" -> inv.setBoots(s);
                case "offhand" -> inv.setItemInOffHand(s);
                default -> leftovers.putAll(inv.addItem(s));
            }
        }
        if (!leftovers.isEmpty()) {
            // The plan said it fits; this is only a safety net.
            for (ItemStack s : leftovers.values()) target.getWorld().dropItemNaturally(target.getLocation(), s);
            plugin.getLogger().warning("ExcelDeathRecord restore: " + leftovers.size()
                    + " stack(s) did not fit despite the plan and were dropped at " + target.getName() + "'s feet.");
            out.add("Warning: " + leftovers.size() + " stack(s) unexpectedly did not fit and were dropped at their feet.");
        }
        if (p.xpWanted) target.giveExp(p.xpPoints);

        String rel = store.relative(src.file);
        String sourceName = src.sourceName.isEmpty() ? "unknown" : src.sourceName;
        List<String> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < p.numbers.size(); i++) {
            DeathRecord.ItemEntry e = p.list.get(p.numbers.get(i) - 1);
            String desc = e.id + (e.enchants.isEmpty() ? "" : " [" + RecordFormat.packEnchantsPublic(e.enchants) + "]")
                    + (e.customName.isEmpty() ? "" : " \"" + e.customName + "\"");
            rows.add(RestoreCsv.row(now, staffName, sourceName, rel, desc, target.getName(),
                    Integer.toString(p.numbers.get(i)), e.count, src.burned ? "burned" : "death", force));
            names.add("#" + p.numbers.get(i) + " " + pretty(e));
        }
        if (p.xpWanted) rows.add(RestoreCsv.row(now, staffName, sourceName, rel, "xp " + p.xpPoints + " points",
                target.getName(), "xp", p.xpPoints, "death", force));
        try {
            store.appendRestores(rows);
        } catch (Exception e) {
            plugin.getLogger().warning("ExcelDeathRecord: could not append restores.csv: " + e.getMessage());
            out.add("Warning: restores.csv could not be written (" + e.getMessage() + "). The record itself is marked.");
        }

        String summary = "Restored " + p.numbers.size() + " item" + (p.numbers.size() == 1 ? "" : "s")
                + (p.xpWanted ? " + " + p.xpPoints + " XP points" : "") + " to " + target.getName()
                + " from " + (src.burned && (sourceName.isEmpty() || sourceName.equalsIgnoreCase("unknown"))
                        ? "an unowned Burn Ledger entry" : sourceName + "'s " + (src.burned ? "Burn Ledger entry" : "death")) + " (" + rel + ")";
        plugin.getLogger().info("ExcelDeathRecord: restore by " + staffName + " — " + summary + ": " + String.join(", ", names)
                + (force ? " [force]" : ""));
        out.add(summary + ":");
        for (String n : names) out.add("  " + n);
        out.addAll(p.notes);
        target.sendMessage("An op gave back " + p.numbers.size() + " item" + (p.numbers.size() == 1 ? "" : "s")
                + (p.xpWanted ? " and your XP" : "") + " from " + (p.samePlayer ? "your" : sourceName + "'s") + " death record.");
        return out;
    }
}
