package com.winkreyes.makersmark.history;
import java.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** PDC codec only. Not an inventory transaction handler and never mutates the caller's item. */
public final class HistoryItemTokens {
    private final NamespacedKey itemId, revision, receipt;
    public record Token(UUID item,long revision,UUID receipt){}
    public HistoryItemTokens(String namespace){
        itemId=new NamespacedKey(namespace,"item_id");
        revision=new NamespacedKey(namespace,"history_revision_v2");
        receipt=new NamespacedKey(namespace,"history_receipt_v2");
    }
    /** Empty only for truly unarchived items; partially written or malformed tokens are errors. */
    public Optional<Token> read(ItemStack item){
        if(item==null||!item.hasItemMeta())return Optional.empty();
        var pdc=item.getItemMeta().getPersistentDataContainer();
        boolean r=pdc.has(revision),t=pdc.has(receipt);
        if(!r&&!t)return Optional.empty();
        if(!r||!t)throw new IllegalStateException("Partial history token");
        String id=pdc.get(itemId,PersistentDataType.STRING),nonce=pdc.get(receipt,PersistentDataType.STRING);
        Long version=pdc.get(revision,PersistentDataType.LONG);
        if(id==null||nonce==null||version==null||version<1)throw new IllegalStateException("Malformed history token");
        try{return Optional.of(new Token(UUID.fromString(id),version,UUID.fromString(nonce)));}
        catch(IllegalArgumentException e){throw new IllegalStateException("Malformed history UUID",e);}
    }
    /** Requires a proven plan from the archive; caller separately checks committed inventory placement. */
    public ItemStack resultCopy(ItemStack current,HistoryLedger.Plan plan){
        if(current==null||current.getType().isAir()||current.getAmount()!=1)throw new IllegalArgumentException("History requires one eligible gear item");
        ItemStack copy=current.clone();ItemMeta meta=copy.getItemMeta();if(meta==null)throw new IllegalArgumentException("Missing item metadata");
        var pdc=meta.getPersistentDataContainer();String prior=pdc.get(itemId,PersistentDataType.STRING);
        // A merge may adopt another known input ID, but must never overwrite an unrelated item identity.
        if(prior!=null&&!prior.equals(plan.output().item().toString())&&plan.inputs().stream().noneMatch(e->e.item().toString().equals(prior)))throw new IllegalStateException("Unrelated item identity");
        pdc.set(itemId,PersistentDataType.STRING,plan.output().item().toString());
        pdc.set(revision,PersistentDataType.LONG,plan.output().revision());pdc.set(receipt,PersistentDataType.STRING,plan.receipt().toString());
        copy.setItemMeta(meta);return copy;
    }
}
