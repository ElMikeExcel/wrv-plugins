/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Unwired provisioning helper. Only idle, authoritative owned slots; never a shot/event source guess.
 * Does not award history, render lore, add Found by, repair an existing ID, or resolve cloned IDs. */
public final class OwnedSlotIdentity {
    private OwnedSlotIdentity() {}
    public record Result(int assigned, int existing, int refused, boolean skipped) {}
    /** A synchronous inventory view, never an event result, cursor or delayed snapshot. */
    public interface Slots {
        boolean safeNow();
        int size();
        ItemStack get(int slot);
        void set(int slot, ItemStack item);
    }
    static Result provision(Player player, ExcelMakersMark plugin) {
        Slots slots=new Slots() {
            public boolean safeNow() {
                return Bukkit.isPrimaryThread() && player.isOnline()
                    && (player.getGameMode()==GameMode.SURVIVAL || player.getGameMode()==GameMode.ADVENTURE)
                    && !player.hasActiveItem();
            }
            public int size(){return player.getInventory().getSize();}
            public ItemStack get(int slot){return player.getInventory().getItem(slot);}
            public void set(int slot,ItemStack item){player.getInventory().setItem(slot,item);}
        };
        return provision(slots,plugin::eligible,plugin.marks().kId,UUID::randomUUID);
    }
    /** Testable synchronous core. The caller must not call during a firing/consumption transaction.
     * Recheck safety and exact slot contents immediately before replacing with a metadata-only clone. */
    public static Result provision(Slots slots, Predicate<ItemStack> eligible, NamespacedKey idKey, Supplier<UUID> ids) {
        if(!slots.safeNow())return new Result(0,0,0,true);
        int assigned=0,existing=0,refused=0;
        for(int slot=0;slot<slots.size();slot++) {
            if(!slots.safeNow())return new Result(assigned,existing,refused,true);
            ItemStack live=slots.get(slot);
            if(live==null || live.getType().isAir() || live.getAmount()!=1 || !eligible.test(live))continue;
            ItemStack snapshot=live.clone();
            ItemMeta meta=snapshot.getItemMeta();
            // Any preexisting ID tag, even malformed/wrong-type, is evidence, not permission to reidentify.
            if(meta.getPersistentDataContainer().has(idKey)){existing++;continue;}
            // Partial archive tokens are corruption/recovery evidence, never a legacy item to reidentify.
            if(meta.getPersistentDataContainer().has(new NamespacedKey(idKey.getNamespace(),"history_revision_v2"))
                    || meta.getPersistentDataContainer().has(new NamespacedKey(idKey.getNamespace(),"history_receipt_v2"))){refused++;continue;}
            UUID fresh=ids.get();
            if(fresh==null){refused++;continue;}
            meta.getPersistentDataContainer().set(idKey,PersistentDataType.STRING,fresh.toString());
            ItemStack replacement=snapshot.clone();replacement.setItemMeta(meta);
            if(!slots.safeNow() || !snapshot.equals(slots.get(slot))){refused++;continue;}
            slots.set(slot,replacement);assigned++;
        }
        return new Result(assigned,existing,refused,false);
    }
}
