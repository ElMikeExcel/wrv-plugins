// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Confirmation.ConfirmationGate;
import com.Acrobot.ChestShop.Events.*;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
import com.winkreyes.shops.checkout.Money;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;

/** Adapts preserved ChestShop shops to one-use, journaled, final-confirm-only transactions. */
public final class LegacyShopBridge implements LegacyTrading,AutoCloseable {
    private final WRVShops plugin;
    private final Economy economy;
    private final FileJournal journal;
    private final ConfirmationGate.Controller controller;
    private final Duration ttl;
    private final Map<UUID,OfferState> offers=new HashMap<>();
    private final Map<UUID,QuoteState> quotes=new HashMap<>();
    private final Map<UUID,UUID> attempts=new HashMap<>();
    private LegacyShopUi ui;
    private UUID preparingPlayer;
    private String preparingSign;
    private TransactionEvent committing;
    private boolean closed, executing;
    private record OfferState(UUID player,Offer view,Location location,String sign,Instant expires){}
    private record QuoteState(UUID player,OfferState offer,Quote view,Snapshot snapshot){}
    private record Snapshot(String evidence,String fingerprint,long cents,int quantity,BigDecimal clientBalance,BigDecimal ownerBalance,boolean admin){}

    LegacyShopBridge(WRVShops plugin,Economy economy,long quoteSeconds)throws IOException {
        this.plugin=plugin;this.economy=economy;this.ttl=Duration.ofSeconds(quoteSeconds);
        if(ConfirmationGate.apiVersion()!=1||!"chestshop474-confirmation.1".equals(ConfirmationGate.buildId()))
            throw new IllegalStateException("Mismatched ChestShop confirmation guard");
        journal=new FileJournal(plugin.getDataFolder().toPath().resolve("legacy-checkout"));
        try {controller=ConfirmationGate.register(plugin,this::offer);}
        catch(RuntimeException|Error failure){journal.closeUnclean();throw failure;}
        if(journal.requiresRecovery())plugin.getCheckout().pauseExternal("Legacy checkout: "+journal.recoveryReason());
    }
    void setUi(LegacyShopUi ui){this.ui=ui;}
    @Override public boolean available(){return !closed&&plugin.isEnabled()&&economy.isEnabled()&&plugin.getServer().getServicesManager().getRegistration(Economy.class)!=null&&plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider()==economy&&controller.isActive()&&!plugin.getCheckout().recoveryRequired()&&!journal.requiresRecovery()&&ConfirmationGate.unavailableReason()==null;}
    boolean allowsPreparation(PreTransactionEvent e){return Bukkit.isPrimaryThread()&&preparingPlayer!=null&&preparingPlayer.equals(e.getClient().getUniqueId())&&Objects.equals(preparingSign,position(e.getSign()));}
    boolean allowsCommit(TransactionEvent e){return Bukkit.isPrimaryThread()&&e!=null&&e==committing;}
    private static String position(Sign s){return s==null?"":s.getWorld().getUID()+":"+s.getX()+":"+s.getY()+":"+s.getZ();}
    @SuppressWarnings("deprecation") private static String signState(Sign s){return position(s)+"|"+s.getBlockData().getAsString()+"|"+Arrays.toString(s.getLines());}
    private static void requireMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Shop actions require the server thread.");}
    private void requireAvailable(){if(!available())throw new IllegalStateException("Shop confirmation is paused. No trade was made.");}
    private void checkPlayer(Player p,Location location){
        if(!p.isOnline()||!p.hasPermission("wrvshops.use")||!p.getWorld().equals(location.getWorld())||p.getLocation().distanceSquared(location.clone().add(.5,.5,.5))>36)
            throw new IllegalArgumentException("Stay near this shop with shop access, then open it again.");
    }
    private Sign current(Player p,OfferState offer){
        requireAvailable();checkPlayer(p,offer.location());
        if(!offer.player().equals(p.getUniqueId())||!Instant.now().isBefore(offer.expires())||!(offer.location().getBlock().getState() instanceof Sign sign)||!ChestShopSign.isValid(sign)||!offer.sign().equals(signState(sign)))
            throw new IllegalArgumentException("This shop changed or the offer expired. Open it again and review the new offer.");
        return sign;
    }
    private void offer(Player p,Sign sign){
        requireMainThread();requireAvailable();checkPlayer(p,sign.getLocation());
        if(!ChestShopSign.isValid(sign))throw new IllegalArgumentException("This is not a valid ChestShop sign.");
        cancel(p.getUniqueId());if(ui!=null)ui.disconnected(p.getUniqueId());
        if(plugin.getUi()!=null)plugin.getUi().disconnected(p.getUniqueId());
        offers.entrySet().removeIf(e->!Instant.now().isBefore(e.getValue().expires()));
        quotes.entrySet().removeIf(e->!offers.containsKey(e.getKey())||!Instant.now().isBefore(e.getValue().view().expiresAt()));
        attempts.keySet().retainAll(offers.keySet());
        if(offers.size()>=CheckoutService.MAX_ACTIVE_QUOTES)throw new IllegalStateException("Too many open shops. Please try later.");
        UUID attempt=UUID.randomUUID();attempts.put(p.getUniqueId(),attempt);String offeredSign=signState(sign);
        ItemParseEvent parsed=new ItemParseEvent(ChestShopSign.getItem(sign));ChestShop.callEvent(parsed);
        if(!attempt.equals(attempts.get(p.getUniqueId())))return;
        requireAvailable();checkPlayer(p,sign.getLocation());
        if(!(sign.getBlock().getState() instanceof Sign currentSign)||!offeredSign.equals(signState(currentSign)))throw new IllegalArgumentException("The shop changed. Open it again.");
        if(!parsed.hasItem())throw new IllegalArgumentException("This shop's item could not be read.");
        BigDecimal buy=PriceUtil.getExactBuyPrice(ChestShopSign.getPrice(sign)),sell=PriceUtil.getExactSellPrice(ChestShopSign.getPrice(sign));
        Offer view=new Offer(UUID.randomUUID(),ChestShopSign.getOwner(sign),parsed.getItem(),ChestShopSign.getQuantity(sign),priceLabel(buy),priceLabel(sell),buy.signum()>=0,sell.signum()>=0);
        OfferState state=new OfferState(p.getUniqueId(),view,sign.getLocation().clone(),signState(sign),Instant.now().plus(ttl));offers.put(p.getUniqueId(),state);
        plugin.getServer().getScheduler().runTask(plugin,()->{if(ui!=null&&offers.get(p.getUniqueId())==state&&p.isOnline()&&available())ui.showOffer(p,view);});
    }
    private static String priceLabel(BigDecimal value){return value.signum()<0?"Unavailable":"$"+value.toPlainString();}
    private ConfirmationGate.PreparedTrade prepare(Player p,Sign sign,Direction direction,int quantity){
        preparingPlayer=p.getUniqueId();preparingSign=position(sign);
        try{return controller.prepare(sign,p,direction==Direction.BUY?TransactionEvent.TransactionType.BUY:TransactionEvent.TransactionType.SELL,quantity);}
        finally{preparingPlayer=null;preparingSign=null;}
    }
    @Override public Quote quote(Player p,UUID offerId,Direction direction,int requested)throws Exception {
        requireMainThread();quotes.remove(p.getUniqueId());Money.requireQuantity(requested);Objects.requireNonNull(direction);
        OfferState offer=offers.get(p.getUniqueId());if(offer==null||!offer.view().id().equals(offerId))throw new IllegalArgumentException("Open this shop again.");
        UUID attempt=UUID.randomUUID();attempts.put(p.getUniqueId(),attempt);Sign sign=current(p,offer);
        if(direction==Direction.BUY?!offer.view().canBuy():!offer.view().canSell())throw new IllegalArgumentException("That trade direction is unavailable.");
        try(CheckoutService.ExternalLease lease=plugin.getCheckout().tryExternalLease()){
            if(lease==null)throw new IllegalStateException("Another checkout is in progress. Please try again.");
            ConfirmationGate.PreparedTrade prepared=prepare(p,sign,direction,requested);if(prepared==null)throw new IllegalArgumentException("ChestShop declined this offer. Check stock, money, space and permissions.");
            revalidateAfterPreparation(p,offer,attempt);
            Snapshot snapshot=snapshot(prepared.event());
            Instant expires=offer.expires();
            String description=(direction==Direction.BUY?"Buy ":"Sell ")+snapshot.quantity()+" × "+ShopAdapter.name(prepared.event().getStock()[0])+"\nShop: "+offer.view().owner()+"\nTotal "+(direction==Direction.BUY?"to pay: $":"to receive: $")+decimal(snapshot.cents())+"\nEffective price: "+unitPrice(snapshot.cents(),snapshot.quantity())+" per item (total rounded to cents)"+"\nSign bundle: "+offer.view().bundleQuantity()+" for "+(direction==Direction.BUY?offer.view().buyPrice():offer.view().sellPrice())+(requested!=snapshot.quantity()?"\nPartial offer: "+snapshot.quantity()+" of "+requested+" requested":"");
            Quote view=new Quote(UUID.randomUUID(),direction,requested,snapshot.quantity(),snapshot.cents(),prepared.event().getStock()[0],description,expires);
            quotes.put(p.getUniqueId(),new QuoteState(p.getUniqueId(),offer,view,snapshot));return view;
        }
    }
    @Override public Outcome confirm(Player p,UUID quoteId){
        requireMainThread();QuoteState quote=quotes.remove(p.getUniqueId());
        if(quote==null||!quote.view().id().equals(quoteId)||!Instant.now().isBefore(quote.view().expiresAt()))return denied("This confirmation expired or was already used. Open the shop again.");
        UUID attempt=UUID.randomUUID();attempts.put(p.getUniqueId(),attempt);
        CheckoutService.Quote audit=null;TransactionEvent attemptedEvent=null;boolean intent=false;
        try(CheckoutService.ExternalLease lease=plugin.getCheckout().tryExternalLease()){
            if(lease==null)return denied("Another checkout is in progress or purchases are paused.");
            executing=true;
            Sign sign=current(p,quote.offer());
            ConfirmationGate.PreparedTrade prepared=prepare(p,sign,quote.view().direction(),quote.view().requestedQuantity());
            if(prepared==null)return denied("ChestShop declined the final check. No trade was made.");
            revalidateAfterPreparation(p,quote.offer(),attempt);
            TransactionEvent event=prepared.event();attemptedEvent=event;Snapshot fresh=snapshot(event);
            if(!fresh.fingerprint().equals(quote.snapshot().fingerprint()))return denied("Price, stock, money or inventory changed. Open the shop and review a fresh offer.");
            UUID owner=event.getOwnerAccount().getUuid(),client=p.getUniqueId();boolean buy=quote.view().direction()==Direction.BUY;
            audit=new CheckoutService.Quote(quote.view().id(),buy?client:owner,buy?owner:client,"legacy:"+position(sign),0,fresh.quantity(),fresh.cents(),fresh.fingerprint(),quote.view().expiresAt());
            journal.record(audit,CheckoutService.Stage.LEGACY_INTENT,fresh.evidence());intent=true;
            plugin.markLegacyDirty(p,sign.getWorld().getUID());
            committing=event;ConfirmationGate.CommitResult result;
            try{result=controller.execute(prepared);}finally{committing=null;}
            if(result.succeeded()&&balancesMatch(event,fresh)){
                journal.record(audit,CheckoutService.Stage.LEGACY_COMMITTED,"Guard completed exact money and item phases; resulting balances verified.");
                // Receipts are informational, and never replay or revoke a completed trade.
                try{controller.acknowledgeSuccess(prepared);}catch(RuntimeException|LinkageError receiptFailure){plugin.getLogger().warning("Trade completed but its legacy receipt could not be shown: "+receiptFailure);}
                offers.remove(p.getUniqueId());return new Outcome(true,false,"Trade complete: "+quote.view().quantity()+" items for $"+decimal(quote.view().totalCents())+".");
            }
            if(!result.moneyAttempted()&&!result.itemsAttempted()&&fresh.fingerprint().equals(snapshot(event).fingerprint())){
                journal.record(audit,CheckoutService.Stage.LEGACY_NO_CHANGE,"Guard rejected before mutation: "+result.error());return denied("The trade could not be completed. Nothing changed.");
            }
            return recovery(audit,"Legacy trade outcome needs reconciliation: "+result+afterEvidence(event));
        }catch(Exception|LinkageError failure){
            if(intent)return recovery(audit,"Legacy checkout failed after durable intent: "+failure+afterEvidence(attemptedEvent));
            if(journal.requiresRecovery()||plugin.getCheckout().recoveryRequired())return recovery(null,"Legacy checkout journal failed: "+failure);
            return denied(failure.getMessage()==null?"Shop unavailable. No trade was made.":failure.getMessage());
        }finally{executing=false;committing=null;}
    }
    private String afterEvidence(TransactionEvent event){
        if(event==null)return "\nNo transaction event available for after-state capture.";
        try{return "\nObserved after-state (not an instruction to replay):\n"+snapshot(event).evidence();}
        catch(Exception failure){return "\nCould not capture complete after-state: "+failure;}
    }
    private Outcome recovery(CheckoutService.Quote audit,String reason){
        String summary=reason.substring(0,Math.min(reason.length(),2048));
        String detail=reason.getBytes(StandardCharsets.UTF_8).length<=CheckoutService.MAX_EVIDENCE_BYTES?reason:summary+"\nAfter-state exceeded the evidence limit; preserve the original intent and inspect saved inventories/balances.";
        if(audit!=null)try{journal.record(audit,CheckoutService.Stage.RECOVERY_REQUIRED,detail);}catch(Exception ignored){}
        plugin.getCheckout().pauseExternal(summary);plugin.getLogger().severe(summary);
        return new Outcome(false,true,"Shops are paused for an administrator check. Do not retry this trade.");
    }
    private void revalidateAfterPreparation(Player p,OfferState offer,UUID attempt){
        if(offers.get(p.getUniqueId())!=offer||!attempt.equals(attempts.get(p.getUniqueId())))throw new IllegalArgumentException("This offer was cancelled or replaced. Open the shop again.");
        current(p,offer);
    }
    private static Outcome denied(String message){return new Outcome(false,false,message);}
    private BigDecimal balance(UUID id,String world){double value=economy.getBalance(plugin.getServer().getOfflinePlayer(id),world);if(!Double.isFinite(value))throw new IllegalStateException("Invalid economy balance");return BigDecimal.valueOf(value);}
    private Snapshot snapshot(TransactionEvent event)throws Exception{
        long cents=event.getExactPrice().movePointRight(2).longValueExact();if(cents<0||cents>Money.MAX_MINOR_UNITS)throw new IllegalArgumentException("Trade price exceeds the supported currency range or precision.");
        int quantity=0;ItemStack[] stock=event.getStock();
        if(stock==null||stock.length==0||stock[0]==null)throw new IllegalArgumentException("No valid items in this offer.");
        for(ItemStack stack:stock){
            if(stack==null||stack.getAmount()<=0||!stock[0].isSimilar(stack))throw new IllegalArgumentException("This offer mixes item metadata and needs a shop administrator check.");
            quantity=Math.addExact(quantity,stack.getAmount());
        }
        Money.requireQuantity(quantity);
        boolean admin=NameManager.isAdminShop(event.getOwnerAccount().getUuid());String world=event.getSign().getWorld().getName();
        BigDecimal client=balance(event.getClient().getUniqueId(),world),owner=admin?BigDecimal.ZERO:balance(event.getOwnerAccount().getUuid(),world);
        String evidence="sign="+signState(event.getSign())+"\nowner="+event.getOwnerAccount().getUuid()+"\nclient="+event.getClient().getUniqueId()+"\ndirection="+event.getTransactionType()+"\namount="+cents+"\nclientBalance="+client+"\nownerBalance="+owner+"\nadmin="+admin+"\nstock="+items(event.getStock())+"\nownerInventory="+inventory(event.getOwnerInventory())+"\nclientInventory="+inventory(event.getClientInventory());
        if(evidence.getBytes(StandardCharsets.UTF_8).length>CheckoutService.MAX_EVIDENCE_BYTES)throw new IllegalArgumentException("Trade metadata is too large for safe checkout.");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(evidence.getBytes(StandardCharsets.UTF_8)));
        return new Snapshot(evidence,hash,cents,quantity,client,owner,admin);
    }
    private boolean balancesMatch(TransactionEvent event,Snapshot before){
        BigDecimal amount=BigDecimal.valueOf(before.cents(),2),clientDelta=event.getTransactionType()==TransactionEvent.TransactionType.BUY?amount.negate():amount;
        String world=event.getSign().getWorld().getName();
        return balance(event.getClient().getUniqueId(),world).compareTo(before.clientBalance().add(clientDelta))==0&&(before.admin()||balance(event.getOwnerAccount().getUuid(),world).compareTo(before.ownerBalance().subtract(clientDelta))==0);
    }
    private static String inventory(Inventory inventory)throws IOException{return inventory.getType()+"|"+inventory.getLocation()+"|"+items(inventory.getStorageContents());}
    private static String items(ItemStack[] stacks)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(BukkitObjectOutputStream out=new BukkitObjectOutputStream(bytes)){out.writeInt(stacks.length);for(ItemStack stack:stacks)out.writeObject(stack==null?null:stack.clone());}return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
    private static String unitPrice(long cents,int quantity){
        BigDecimal total=BigDecimal.valueOf(cents,2),unit=total.divide(BigDecimal.valueOf(quantity),6,java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        return (unit.multiply(BigDecimal.valueOf(quantity)).compareTo(total)==0?"$":"about $")+unit.toPlainString();
    }
    static String decimal(long cents){return BigDecimal.valueOf(cents,2).toPlainString();}
    @Override public void cancel(UUID player){requireMainThread();offers.remove(player);quotes.remove(player);attempts.remove(player);}
    @Override public void close()throws IOException{close(true);}
    void close(boolean clean)throws IOException{requireMainThread();closed=true;controller.close();offers.clear();quotes.clear();attempts.clear();if(clean&&!executing&&!journal.requiresRecovery())journal.close();else journal.closeUnclean();}
}
