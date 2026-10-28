package com.winkreyes.teamrewards.ui;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import wrv.rewards.TeamRewardLedger;

/** Synchronous inventory-only adapter. No XP/currency/world drops; decoder must reject unsupported payloads. */
public final class LedgerClaimGateway implements ClaimGateway {
    public interface Decoder { List<ItemStack> decode(TeamRewardLedger.Claim claim); }
    /** Must positively confirm normal persistent inventory, excluding Magic/wand and other temporary inventories. */
    public interface InventoryGuard { boolean isAuthoritativeNormalInventory(Player player); }
    private final InventoryGuard guard;
    private final TeamRewardLedger ledger;
    private final Decoder decoder;
    public LedgerClaimGateway(TeamRewardLedger ledger,Decoder decoder,InventoryGuard guard) {
        this.ledger=Objects.requireNonNull(ledger);this.decoder=Objects.requireNonNull(decoder);this.guard=Objects.requireNonNull(guard);
    }
    @Override public List<Pending> pending(UUID owner) {
        return ledger.claims(owner).stream().filter(c->c.state()==TeamRewardLedger.State.OWED && !c.units().isEmpty())
                .map(c->new Pending(c.id(),"Reward from "+c.encounterId())).toList();
    }
    @Override public Result claim(UUID owner,String claimId) {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");
        Player player=Bukkit.getPlayer(owner);
        if(player==null || !player.isOnline())return Result.OFFLINE;
        try { if(!guard.isAuthoritativeNormalInventory(player))return Result.UNSAFE_INVENTORY; }
        catch(RuntimeException unknownInventory){return Result.UNSAFE_INVENTORY;}
        var claim=ledger.claims(owner).stream().filter(c->c.id().equals(claimId)).findFirst();
        if(claim.isEmpty())return Result.NOT_OWNER;
        var current=claim.get();
        if(current.state()==TeamRewardLedger.State.DELIVERED)return Result.ALREADY_CLAIMED;
        if(current.state()!=TeamRewardLedger.State.OWED)return Result.PENDING_REVIEW;
        ItemStack[] before=copy(player.getInventory().getStorageContents());
        ItemStack[] planned;
        try { planned=plan(before,decoder.decode(current)); }
        catch(RuntimeException malformed) { return Result.UNAVAILABLE; }
        if(planned==null)return Result.FULL_INVENTORY; // NEVER creates PENDING for known insufficient space.
        TeamRewardLedger.Claim pending;
        try { pending=ledger.beginDelivery(owner,claimId); }
        catch(Exception persistence) { return Result.UNAVAILABLE; }
        try {
            if(!player.isOnline() || !guard.isAuthoritativeNormalInventory(player) || !Arrays.equals(before,player.getInventory().getStorageContents())) {
                ledger.finishDelivery(owner,claimId,pending.attempt(),TeamRewardLedger.Outcome.NOT_APPLIED);
                return player.isOnline()?Result.UNAVAILABLE:Result.OFFLINE;
            }
            // One mutation, only after durable intent; never drop overflow or repeat after an exception.
            player.getInventory().setStorageContents(planned);
            if(!Arrays.equals(planned,player.getInventory().getStorageContents()))throw new IllegalStateException("Inventory write not confirmed");
            ledger.finishDelivery(owner,claimId,pending.attempt(),TeamRewardLedger.Outcome.DELIVERED);
            return Result.DELIVERED;
        } catch(Exception uncertain) {
            try { ledger.finishDelivery(owner,claimId,pending.attempt(),TeamRewardLedger.Outcome.UNCERTAIN); }
            catch(Exception locked) { /* PENDING remains durable: restart moves it to REVIEW. */ }
            return Result.PENDING_REVIEW;
        }
    }
    private static boolean empty(ItemStack item) {
        return item.getType()==org.bukkit.Material.AIR || item.getType()==org.bukkit.Material.CAVE_AIR
                || item.getType()==org.bukkit.Material.VOID_AIR;
    }
    static ItemStack[] copy(ItemStack[] input) {
        ItemStack[] output=new ItemStack[input.length];
        for(int i=0;i<input.length;i++)output[i]=input[i]==null?null:input[i].clone();
        return output;
    }
    /** Pure allocation on clones. Null means insufficient capacity; never mutates supplied inventory/items. */
    static ItemStack[] plan(ItemStack[] storage,List<ItemStack> rewards) {
        if(rewards==null || rewards.isEmpty())throw new IllegalArgumentException("Empty reward payload");
        ItemStack[] out=copy(storage);
        for(ItemStack reward:rewards) {
            if(reward==null || empty(reward) || reward.getAmount()<=0)throw new IllegalArgumentException("Invalid reward item");
            int left=reward.getAmount();
            for(int i=0;i<out.length&&left>0;i++) {
                ItemStack existing=out[i];
                if(existing!=null&&!empty(existing)&&existing.isSimilar(reward)) {
                    int add=Math.min(left,Math.max(0,Math.min(existing.getMaxStackSize(),reward.getMaxStackSize())-existing.getAmount()));
                    existing.setAmount(existing.getAmount()+add);left-=add;
                }
            }
            for(int i=0;i<out.length&&left>0;i++)if(out[i]==null||empty(out[i])) {
                int add=Math.min(left,reward.getMaxStackSize());out[i]=reward.clone();out[i].setAmount(add);left-=add;
            }
            if(left>0)return null;
        }
        return out;
    }
}
