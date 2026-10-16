// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.CheckoutService.*;
import com.winkreyes.shops.checkout.Money;
import com.winkreyes.shops.store.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ShopAdapter implements Adapter {
    public record Selection(String offerId,long revision,UUID seller,String name,ItemStack item,
                            int bundleSize,long price,int available,String fingerprint,ShopRecord shop) {
        public Selection {item=item.clone();}
        @Override public ItemStack item(){return item.clone();}
    }
    private final WRVShops plugin;
    private final Economy economy;
    public ShopAdapter(WRVShops plugin,Economy economy){this.plugin=plugin;this.economy=economy;}
    @Override public void assertMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Primary server thread required");}
    public Selection spawn(String id){
        SpawnCatalog.Entry e=plugin.getCatalog().get(id);
        if(e==null)throw new IllegalArgumentException("That item is not listed.");
        return new Selection("spawn:"+id,1,null,e.name(),e.item(),e.bundleSize(),e.bundlePrice(),2304/e.bundleSize(),hash(InventoryPlan.fingerprint(e.item())+":"+e.bundleSize()+":"+e.bundlePrice()),null);
    }
    public Selection chest(ShopRecord shop,ShopOffer offer)throws RegistryException {
        Chest chest=plugin.getRegistry().validate(shop);
        int available=Math.min(offer.stockLimit(),InventoryPlan.count(chest.getBlockInventory().getStorageContents(),offer.item()));
        String fingerprint=hash(InventoryPlan.fingerprint(offer.item())+":"+InventoryPlan.snapshot(chest.getBlockInventory().getStorageContents())+":"+shop.ownerId()+":"+shop.revision()+":"+offer.unitPriceCents());
        return new Selection("chest:"+shop.id()+":"+offer.id(),shop.revision(),shop.ownerId(),name(offer.item()),offer.item(),1,offer.unitPriceCents(),available,fingerprint,shop);
    }
    public Selection resolve(String id)throws RegistryException {
        if(id.startsWith("spawn:"))return spawn(id.substring(6));
        String[] p=id.split(":");if(p.length!=3||!p[0].equals("chest"))throw new RegistryException("Invalid listing.");
        ShopRecord shop=plugin.getRegistry().findById(UUID.fromString(p[1])).orElseThrow(()->new RegistryException("This shop has closed."));
        ShopOffer offer=shop.offer(UUID.fromString(p[2])).orElseThrow(()->new RegistryException("This item is no longer listed."));
        return chest(shop,offer);
    }
    @Override public Preparation prepare(Quote quote)throws Exception {
        Player buyer=plugin.getServer().getPlayer(quote.buyerId());
        if(buyer==null||!buyer.isOnline()||!buyer.hasPermission("wrvshops.use"))return Preparation.denied("You can no longer use this shop. No money was taken.");
        if(!economy.isEnabled()||plugin.getServer().getServicesManager().getRegistration(Economy.class)==null||plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider()!=economy)return Preparation.denied("The economy changed. No money was taken.");
        Selection current=resolve(quote.offerId());
        if(!Objects.equals(current.seller(),quote.sellerId())||current.revision()!=quote.revision()||!current.fingerprint().equals(quote.fingerprint())||Money.multiply(current.price(),quote.quantity())!=quote.amountMinorUnits())return Preparation.denied("The price or stock changed. Please review it again. No money was taken.");
        if(quote.quantity()>current.available())return Preparation.denied("There is not enough stock. No money was taken.");
        if(current.seller()!=null&&current.seller().equals(buyer.getUniqueId()))return Preparation.denied("Use Manage to restock your own shop.");
        Chest chest=current.shop()==null?null:plugin.getRegistry().validate(current.shop());
        if(chest!=null&&(!buyer.getWorld().equals(chest.getWorld())||buyer.getLocation().distanceSquared(chest.getLocation().add(.5,.5,.5))>36))return Preparation.denied("Please stay beside the shop while buying. No money was taken.");
        if(!economy.hasAccount(buyer)||(current.seller()!=null&&!economy.hasAccount(Bukkit.getOfflinePlayer(current.seller()))))return Preparation.denied("An economy account is unavailable. No money was taken.");
        int digits=economy.fractionalDigits();
        if((digits==0&&quote.amountMinorUnits()%100!=0)||(digits==1&&quote.amountMinorUnits()%10!=0))return Preparation.denied("The economy cannot handle this price precision. No money was taken.");
        double funds=economy.getBalance(buyer);
        if(!Double.isFinite(funds)||BigDecimal.valueOf(funds).compareTo(BigDecimal.valueOf(quote.amountMinorUnits(),2))<0)return Preparation.denied("You do not have enough money. No money was taken.");
        double sellerFunds=current.seller()==null?0:economy.getBalance(Bukkit.getOfflinePlayer(current.seller()));
        if(!Double.isFinite(sellerFunds)||!Double.isFinite(sellerFunds+Money.toVault(quote.amountMinorUnits())))return Preparation.denied("The seller account needs an administrator check. No money was taken.");
        int amount=Math.multiplyExact(current.bundleSize(),quote.quantity());
        ItemStack[] buyerBefore=InventoryPlan.copy(buyer.getInventory().getStorageContents());
        final ItemStack[] buyerAfter;
        try {buyerAfter=InventoryPlan.add(buyerBefore,current.item(),amount,buyer.getInventory().getMaxStackSize());}
        catch(IllegalArgumentException e){return Preparation.denied(e.getMessage());}
        Inventory stock=chest==null?null:chest.getBlockInventory();
        ItemStack[] stockBefore=stock==null?null:InventoryPlan.copy(stock.getStorageContents());
        ItemStack[] stockAfter=stock==null?null:InventoryPlan.remove(stockBefore,current.item(),amount);
        return Preparation.ready(new Transfer(){
            public String auditEvidence(){return "buyer-balance="+funds+";seller-balance="+sellerFunds+";source="+(current.shop()==null?"unlimited-spawn":current.shop().chest())+";item="+InventoryPlan.fingerprint(current.item())+";buyer-before="+InventoryPlan.snapshot(buyerBefore)+";buyer-after="+InventoryPlan.snapshot(buyerAfter)+";stock-before="+(stockBefore==null?"none":InventoryPlan.snapshot(stockBefore))+";stock-after="+(stockAfter==null?"none":InventoryPlan.snapshot(stockAfter));}
            public MutationResult apply()throws Exception {
                if(!buyer.isOnline()||!Arrays.equals(buyerBefore,buyer.getInventory().getStorageContents())||(stock!=null&&!Arrays.equals(stockBefore,stock.getStorageContents())))return MutationResult.UNCHANGED;
                // Re-entrant economy providers cannot invalidate the source between debit and delivery.
                if(current.shop()!=null)try{plugin.getRegistry().validate(current.shop());}catch(RegistryException rejected){return MutationResult.UNCHANGED;}
                if(stock!=null)stock.setStorageContents(InventoryPlan.copy(stockAfter));
                buyer.getInventory().setStorageContents(InventoryPlan.copy(buyerAfter));
                if(!Arrays.equals(buyerAfter,buyer.getInventory().getStorageContents())||(stock!=null&&!Arrays.equals(stockAfter,stock.getStorageContents())))return MutationResult.UNCERTAIN;
                plugin.markDirty(buyer,current.shop());
                return MutationResult.APPLIED;
            }
            public boolean rollback() {
                if(!buyer.isOnline()||!Arrays.equals(buyerAfter,buyer.getInventory().getStorageContents())||(stock!=null&&!Arrays.equals(stockAfter,stock.getStorageContents())))return false;
                buyer.getInventory().setStorageContents(InventoryPlan.copy(buyerBefore));
                if(stock!=null)stock.setStorageContents(InventoryPlan.copy(stockBefore));
                plugin.markDirty(buyer,current.shop());
                return Arrays.equals(buyerBefore,buyer.getInventory().getStorageContents())&&(stock==null||Arrays.equals(stockBefore,stock.getStorageContents()));
            }
        });
    }
    @Override public MoneyResult withdraw(UUID id,long amount){return result(economy.withdrawPlayer(Bukkit.getOfflinePlayer(id),Money.toVault(amount)),amount);}
    @Override public MoneyResult deposit(UUID id,long amount){return result(economy.depositPlayer(Bukkit.getOfflinePlayer(id),Money.toVault(amount)),amount);}
    static MoneyResult result(EconomyResponse response,long expected){
        if(response==null||!Double.isFinite(response.amount)||!Double.isFinite(response.balance))return MoneyResult.UNCERTAIN;
        if(!response.transactionSuccess())return response.amount==0?MoneyResult.DECLINED:MoneyResult.UNCERTAIN;
        return BigDecimal.valueOf(response.amount).compareTo(BigDecimal.valueOf(expected,2))==0?MoneyResult.SUCCESS:MoneyResult.UNCERTAIN;
    }
    public static String name(ItemStack item){return item.hasItemMeta()&&item.getItemMeta().hasDisplayName()?org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()):item.getType().name().toLowerCase(Locale.ROOT).replace('_',' ');}
    static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
}
