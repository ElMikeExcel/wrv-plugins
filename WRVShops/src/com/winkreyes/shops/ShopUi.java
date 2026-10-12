// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.Money;
import com.winkreyes.shops.store.*;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

/** Server-owned inventory buttons plus native Bedrock number/review forms. */
public final class ShopUi implements Listener {
    public enum ViewKind { CATALOG, QUANTITY, BUY_REVIEW, BUY_CONFIRM, SETUP_PROMPT, SETUP_ITEMS, SETUP_PRICE, SETUP_REVIEW, SETUP_CONFIRM, MANAGE, CLOSE_CONFIRM }
    public static final class Page implements InventoryHolder {
        final UUID player;
        final ViewKind kind;
        final Map<Integer,Runnable> actions=new HashMap<>();
        Inventory inventory;
        Consumer<String> numberInput;
        Page(UUID player,ViewKind kind){this.player=player;this.kind=kind;}
        @Override public Inventory getInventory(){return inventory;}
        public ViewKind kind(){return kind;}
    }
    private static final class Draft {
        final SetupProposal proposal;
        final LinkedHashMap<UUID,ShopOffer> offers=new LinkedHashMap<>();
        Draft(SetupProposal proposal){this.proposal=proposal;}
    }
    private final WRVShops plugin;
    private final BedrockForms forms;
    private final Map<UUID,Page> pages=new HashMap<>();
    public ShopUi(WRVShops plugin){this.plugin=plugin;forms=new BedrockForms(plugin);}
    private static String cash(long minor){return "$"+Money.format(minor);}
    private static String stockValue(long unit,int stock){return "$"+java.math.BigDecimal.valueOf(unit,2).multiply(java.math.BigDecimal.valueOf(stock)).toPlainString();}
    public ViewKind currentKind(UUID id){Page p=pages.get(id);return p==null?null:p.kind;}
    private Page page(Player p,ViewKind kind,String title,int size){
        Page page=new Page(p.getUniqueId(),kind);page.inventory=Bukkit.createInventory(page,size,title);
        pages.put(p.getUniqueId(),page);return page;
    }
    private void open(Player p,Page page){p.openInventory(page.inventory);}
    private void button(Page page,int slot,Material material,String title,Runnable action,String... lore){button(page,slot,new ItemStack(material),title,action,lore);}
    private void button(Page page,int slot,ItemStack item,String title,Runnable action,String... lore){
        ItemStack icon=item.clone();icon.setAmount(Math.max(1,Math.min(icon.getAmount(),icon.getMaxStackSize())));
        ItemMeta meta=icon.getItemMeta();meta.setDisplayName(ChatColor.RESET+title);List<String> details=new ArrayList<>();if(meta.hasLore())details.addAll(meta.getLore());if(!details.isEmpty())details.add("");details.addAll(Arrays.stream(lore).map(s->ChatColor.GRAY+s).toList());meta.setLore(details);icon.setItemMeta(meta);page.inventory.setItem(slot,icon);
        if(action!=null)page.actions.put(slot,action);
    }
    private void later(Player player,Page expected,Runnable action){
        plugin.getServer().getScheduler().runTask(plugin,()->{if(plugin.isEnabled()&&player.isOnline()&&pages.get(player.getUniqueId())==expected){if(plugin.getCheckout().recoveryRequired()){fail(player,"Shops are paused for an administrator check. Please do not retry the purchase.");return;}try{action.run();}catch(Exception e){fail(player,e.getMessage());}}});
    }
    private void fail(Player p,String message){cancel(p);p.sendMessage(ChatColor.RED+(message==null?"That shop is unavailable. Please try again.":message));}
    public void cancel(Player p){pages.remove(p.getUniqueId());plugin.getCheckout().invalidateBuyer(p.getUniqueId());p.closeInventory();}
    public void disconnected(UUID id){pages.remove(id);plugin.getCheckout().invalidateBuyer(id);}
    public void closeAll(){for(UUID id:List.copyOf(pages.keySet())){Player p=Bukkit.getPlayer(id);if(p!=null)cancel(p);}pages.clear();}
    public boolean enterNumber(Player p,String value){Page page=pages.get(p.getUniqueId());if(page==null||page.numberInput==null)return false;page.numberInput.accept(value);return true;}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Page page))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!page.player.equals(p.getUniqueId())||pages.get(p.getUniqueId())!=page)return;
        if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
        if(e.getRawSlot()<0||e.getRawSlot()>=page.inventory.getSize())return;
        Runnable action=page.actions.get(e.getRawSlot());if(action!=null)later(p,page,action);
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Page)e.setCancelled(true);}
    @EventHandler public void close(InventoryCloseEvent e){
        if(e.getInventory().getHolder() instanceof Page page&&pages.get(e.getPlayer().getUniqueId())==page){pages.remove(e.getPlayer().getUniqueId());plugin.getCheckout().invalidateBuyer(e.getPlayer().getUniqueId());}
    }
    public void showSpawn(Player p){
        plugin.getCheckout().invalidateBuyer(p.getUniqueId());Page page=page(p,ViewKind.CATALOG,"Spawn Shop · choose an item",54);int slot=0;
        for(SpawnCatalog.Entry e:plugin.getCatalog().all()){
            ShopAdapter.Selection s=plugin.getAdapter().spawn(e.id());ItemStack icon=e.item();icon.setAmount(Math.min(e.bundleSize(),icon.getMaxStackSize()));
            button(page,slot++,icon,e.name(),()->quantity(p,s,1),e.bundleSize()+" per bundle",cash(e.bundlePrice())+" per bundle","Tap to choose quantity. No charge yet.");
        }
        button(page,49,Material.BARRIER,"Close",()->cancel(p));open(p,page);
    }
    public void showShop(Player p,ShopRecord shop){
        if(shop.ownerId().equals(p.getUniqueId())){showManage(p,shop);return;}
        plugin.getCheckout().invalidateBuyer(p.getUniqueId());
        try{
            plugin.getRegistry().validate(shop);Page page=page(p,ViewKind.CATALOG,shop.ownerName()+"'s shop",54);int slot=0;
            for(ShopOffer offer:shop.offers()){
                ShopAdapter.Selection s=plugin.getAdapter().chest(shop,offer);
                button(page,slot++,s.item(),s.name(),s.available()>0?()->quantity(p,s,1):null,cash(s.price())+" each","In chest: "+s.available(),s.available()>0?"Tap to choose quantity. No charge yet.":"Out of stock");
            }
            button(page,49,Material.BARRIER,"Close",()->cancel(p));open(p,page);
        }catch(RegistryException e){fail(p,e.getMessage());}
    }
    private void backToCatalog(Player p,ShopAdapter.Selection s){if(s.shop()==null)showSpawn(p);else showShop(p,s.shop());}
    public void quantity(Player p,ShopAdapter.Selection s,int count){
        int max=(int)Math.min(Math.min(s.available(),2304/s.bundleSize()),10000000000L/s.price());count=Math.max(1,Math.min(count,max));if(max<1){fail(p,"This item is out of stock.");return;}
        final int n=count;Page page=page(p,ViewKind.QUANTITY,"How many?",45);
        Consumer<String> input=value->{try{int q=Integer.parseInt(value.trim());if(q<1||q>max)throw new IllegalArgumentException();buyReview(p,s,q);}catch(RuntimeException e){p.sendMessage(ChatColor.RED+"Enter a whole number from 1 to "+max+".");quantity(p,s,n);}};
        page.numberInput=input;
        String details=s.name()+"\n"+cash(s.price())+(s.bundleSize()>1?" per bundle of "+s.bundleSize():" each")+"\nAvailable: "+max+"\nQuantity × price = total. Next: review, then final confirmation.";
        p.closeInventory(); // The new page token prevents closing the old page from cancelling this one.
        if(forms.number(p,"Choose quantity",details,String.valueOf(n),v->later(p,page,()->input.accept(v)),()->later(p,page,()->cancel(p))))return;
        button(page,13,s.item(),s.name(),null,"Quantity: "+n+(s.bundleSize()>1?" bundles ("+(n*s.bundleSize())+" items)":""),cash(s.price())+" × "+n+" = "+cash(Money.multiply(s.price(),n)),"Use buttons, or /wrvshop amount <number>");
        int[] delta={-64,-8,-1,1,8,64};int[] slots={18,19,20,24,25,26};
        for(int i=0;i<delta.length;i++){final int d=delta[i];button(page,slots[i],d<0?Material.RED_DYE:Material.LIME_DYE,(d>0?"+":"")+d,()->quantity(p,s,Math.max(1,Math.min(max,n+d))));}
        button(page,31,Material.LIME_CONCRETE,"Review purchase",()->buyReview(p,s,n),"Nothing is charged yet.");button(page,36,Material.ARROW,"Back",()->backToCatalog(p,s));button(page,44,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    private String purchaseText(ShopAdapter.Selection s,int quantity){return (s.bundleSize()>1?quantity+" bundles = "+(quantity*s.bundleSize()):String.valueOf(quantity))+" × "+s.name()+"\n"+cash(s.price())+(s.bundleSize()>1?" per bundle":" each")+"\nTotal: "+cash(Money.multiply(s.price(),quantity))+itemDetails(s.item());}
    private String itemDetails(ItemStack item){
        ItemMeta meta=item.getItemMeta();StringBuilder info=new StringBuilder();
        Map<org.bukkit.enchantments.Enchantment,Integer> enchants=new LinkedHashMap<>(meta.getEnchants());
        if(meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta book)enchants.putAll(book.getStoredEnchants());
        enchants.forEach((e,l)->info.append("\n").append(e.getKey().getKey().replace('_',' ')).append(" ").append(l));
        if(meta instanceof org.bukkit.inventory.meta.Damageable d&&d.hasDamage())info.append("\nWear: ").append(d.getDamage());
        if(meta.hasLore())for(String line:meta.getLore())info.append("\n").append(ChatColor.stripColor(line));
        return info.toString();
    }
    private void buyReview(Player p,ShopAdapter.Selection s,int quantity){
        Page page=page(p,ViewKind.BUY_REVIEW,"Review purchase",27);String text=purchaseText(s,quantity);
        p.closeInventory();
        if(forms.choices(p,"Review purchase",text,new String[]{"Continue to confirmation","Change quantity","Cancel"},id->later(p,page,()->{if(id==0)buyConfirm(p,s,quantity);else if(id==1)quantity(p,s,quantity);else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        button(page,4,s.item(),s.name(),null,text.split("\n"));button(page,11,Material.LIME_CONCRETE,"Continue to confirmation",()->buyConfirm(p,s,quantity),"No money has moved.");button(page,13,Material.ARROW,"Change quantity",()->quantity(p,s,quantity));button(page,15,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    private void buyConfirm(Player p,ShopAdapter.Selection s,int quantity){
        CheckoutService.Quote quote=plugin.getCheckout().quote(p.getUniqueId(),s.seller(),s.offerId(),s.revision(),quantity,Money.multiply(s.price(),quantity),s.fingerprint());
        Page page=page(p,ViewKind.BUY_CONFIRM,"Are you sure?",27);String text="Buy "+purchaseText(s,quantity)+"?\nOnly Confirm purchase spends money.";
        Runnable confirm=()->{
            pages.remove(p.getUniqueId());p.closeInventory();
            CheckoutService.Result result=plugin.getCheckout().confirm(quote.id(),p.getUniqueId());
            if(result.status()==CheckoutService.Status.SUCCESS)p.sendMessage(ChatColor.GREEN+"Bought "+(quantity*s.bundleSize())+" "+s.name()+" for "+cash(quote.amountMinorUnits())+".");
            else p.sendMessage(ChatColor.YELLOW+result.message());
            if(result.status()==CheckoutService.Status.RECOVERY_REQUIRED)plugin.getLogger().severe("Checkout recovery required, transaction "+result.transactionId()+": "+plugin.getCheckout().recoveryReason());
        };
        Runnable back=()->{plugin.getCheckout().cancel(quote.id(),p.getUniqueId());quantity(p,s,quantity);};
        p.closeInventory();
        if(forms.choices(p,"Are you sure?",text,new String[]{"Confirm purchase: "+cash(quote.amountMinorUnits()),"Back","Cancel"},id->later(p,page,()->{if(id==0)confirm.run();else if(id==1)back.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        button(page,4,s.item(),s.name(),null,purchaseText(s,quantity).split("\n"));button(page,11,Material.LIME_CONCRETE,"Confirm purchase: "+cash(quote.amountMinorUnits()),confirm,"This button spends money.");button(page,13,Material.ARROW,"Back",back);button(page,15,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    public void offerSetup(Player p,SetupProposal proposal){
        Page page=page(p,ViewKind.SETUP_PROMPT,"Make this a shop?",27);
        Runnable yes=()->{Draft d=new Draft(proposal);plugin.getRegistry().findById(proposal.shopId()).ifPresent(s->s.offers().forEach(o->d.offers.put(o.id(),o)));setupItems(p,d);};
        String text="Make this chest your shop?\nChoose a price for each item, review, then confirm.\nUntil you finish, this is ordinary storage.";
        p.closeInventory();
        if(forms.choices(p,"Make this a shop?",text,new String[]{"Yes, set up my shop","No, keep ordinary storage"},id->later(p,page,()->{if(id==0)yes.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        button(page,4,Material.CHEST,"Make this a shop?",null,text.split("\n"));button(page,11,Material.LIME_CONCRETE,"Yes, set up my shop",yes);button(page,15,Material.BARRIER,"No, keep ordinary storage",()->cancel(p));open(p,page);
    }
    private List<ItemStack> unique(ItemStack[] contents){List<ItemStack> items=new ArrayList<>();for(ItemStack item:contents)if(item!=null&&!item.getType().isAir()&&items.stream().noneMatch(i->i.isSimilar(item)))items.add(item);return List.copyOf(items);}
    private ShopOffer draftOffer(Draft d,ItemStack item){return d.offers.values().stream().filter(o->o.item().isSimilar(item)).findFirst().orElse(null);}
    private void setupItems(Player p,Draft draft){
        Page page=page(p,ViewKind.SETUP_ITEMS,"Set prices for chest items",45);int slot=0;
        for(ItemStack item:unique(draft.proposal.contents())){ShopOffer o=draftOffer(draft,item);int stock=InventoryPlan.count(draft.proposal.contents(),item);
            button(page,slot++,item,ShopAdapter.name(item),()->setupPrice(p,draft,item,o==null?100:o.unitPriceCents()),"Stock: "+stock,o==null?"Price needed":cash(o.unitPriceCents())+" each","All matching items in this chest will be for sale.");}
        button(page,31,Material.LIME_CONCRETE,"Review shop",()->{for(ItemStack i:unique(draft.proposal.contents()))if(draftOffer(draft,i)==null){p.sendMessage(ChatColor.YELLOW+"Choose a price for each chest item first.");return;}if(draft.offers.isEmpty()){p.sendMessage(ChatColor.YELLOW+"Add items to your chest first.");return;}setupReview(p,draft,false);},"No shop is created until final confirmation.");
        button(page,40,Material.BARRIER,"Cancel setup",()->cancel(p));open(p,page);
    }
    private void setupPrice(Player p,Draft draft,ItemStack item,long price){
        Page page=page(p,ViewKind.SETUP_PRICE,"Price per item",54);long safe=Math.max(1,Math.min(10000000000L,price));
        Consumer<String> input=value->{try{long chosen=Money.parse(value);saveDraftPrice(draft,item,chosen);setupItems(p,draft);}catch(RuntimeException e){p.sendMessage(ChatColor.RED+"Use a positive price with up to 2 decimals.");setupPrice(p,draft,item,safe);}};page.numberInput=input;
        p.closeInventory();
        if(forms.number(p,"Set unit price",ShopAdapter.name(item)+"\nStock in chest: "+InventoryPlan.count(draft.proposal.contents(),item)+"\nEnter the price for ONE item. You will review before publishing.",java.math.BigDecimal.valueOf(safe,2).toPlainString(),v->later(p,page,()->input.accept(v)),()->later(p,page,()->cancel(p))))return;
        button(page,13,item,ShopAdapter.name(item),null,"Unit price: "+cash(safe),"Stock: "+InventoryPlan.count(draft.proposal.contents(),item),"Use buttons, or /wrvshop amount <price>");
        long[] deltas={-10000,-1000,-100,-10,-1,1,10,100,1000,10000};int[] slots={18,19,20,21,22,27,28,29,30,31};
        for(int i=0;i<deltas.length;i++){final long d=deltas[i];button(page,slots[i],d<0?Material.RED_DYE:Material.LIME_DYE,(d<0?"-":"+")+cash(Math.abs(d)),()->setupPrice(p,draft,item,Math.max(1,Math.min(10000000000L,safe+d))));}
        button(page,40,Material.LIME_CONCRETE,"Use "+cash(safe)+" per item",()->{saveDraftPrice(draft,item,safe);setupItems(p,draft);},"Saves to this review only.");button(page,45,Material.ARROW,"Back",()->setupItems(p,draft));button(page,53,Material.BARRIER,"Cancel setup",()->cancel(p));open(p,page);
    }
    private void saveDraftPrice(Draft d,ItemStack item,long price){ShopOffer previous=draftOffer(d,item);UUID id=previous==null?UUID.randomUUID():previous.id();d.offers.put(id,new ShopOffer(id,item,price,2304));}
    private void setupReview(Player p,Draft draft,boolean confirm){
        Page page=page(p,confirm?ViewKind.SETUP_CONFIRM:ViewKind.SETUP_REVIEW,confirm?"Publish this shop?":"Review your shop",54);
        StringBuilder text=new StringBuilder("Owner: "+p.getName()+"\nAll matching chest stock will be for sale.\n");int slot=0;
        for(ShopOffer o:draft.offers.values()){int stock=InventoryPlan.count(draft.proposal.contents(),o.item());text.append(ShopAdapter.name(o.item())).append(": ").append(cash(o.unitPriceCents())).append(" each; stock ").append(stock).append("; total stock value ").append(stockValue(o.unitPriceCents(),stock)).append('\n');button(page,slot++,o.item(),ShopAdapter.name(o.item()),null,cash(o.unitPriceCents())+" each","Stock: "+stock,"Stock value: "+stockValue(o.unitPriceCents(),stock));}
        Runnable next=()->{if(!confirm){setupReview(p,draft,true);return;}try{ShopRecord shop=plugin.getRegistry().commit(p,draft.proposal,List.copyOf(draft.offers.values()));cancel(p);p.sendMessage(ChatColor.GREEN+"Your shop is ready. Players can open the chest to browse and confirm purchases.");}catch(RegistryException e){fail(p,e.getMessage());}};
        Runnable back=()->setupItems(p,draft);
        p.closeInventory();
        if(forms.choices(p,confirm?"Publish this shop?":"Review your shop",text.toString(),new String[]{confirm?"Confirm and publish shop":"Continue to confirmation","Change prices","Cancel"},id->later(p,page,()->{if(id==0)next.run();else if(id==1)back.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        button(page,45,Material.ARROW,"Change prices",back);button(page,49,Material.LIME_CONCRETE,confirm?"Confirm and publish shop":"Continue to confirmation",next);button(page,53,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    private void requireOwnerNear(Player p,ShopRecord shop)throws RegistryException {
        if(!p.isOnline()||!p.hasPermission("wrvshops.create")||!p.getUniqueId().equals(shop.ownerId()))throw new RegistryException("Only the permitted shop owner can do this.");
        org.bukkit.block.Block chest=shop.chest().resolve(plugin.getServer());
        if(!p.getWorld().equals(chest.getWorld())||p.getLocation().distanceSquared(chest.getLocation().add(.5,.5,.5))>36)throw new RegistryException("Stay beside your shop to manage it.");
    }
    public void showManage(Player p,ShopRecord shop){
        if(!shop.ownerId().equals(p.getUniqueId())){showShop(p,shop);return;}
        try{requireOwnerNear(p,shop);plugin.getRegistry().validate(shop);}catch(RegistryException e){fail(p,e.getMessage());return;}
        Page page=page(p,ViewKind.MANAGE,"Manage your shop",27);
        button(page,11,Material.GOLD_NUGGET,"Edit item prices",()->{try{SetupProposal proposal=plugin.getRegistry().propose(p,shop.chest().resolve(Bukkit.getServer()),shop.sign().resolve(Bukkit.getServer()));Draft d=new Draft(proposal);shop.offers().forEach(o->d.offers.put(o.id(),o));setupItems(p,d);}catch(RegistryException e){fail(p,e.getMessage());}});
        button(page,13,Material.CHEST,"Open stock chest",()->{try{requireOwnerNear(p,shop);Chest chest=plugin.getRegistry().validate(shop);pages.remove(p.getUniqueId());plugin.getCheckout().invalidateBuyer(p.getUniqueId());p.openInventory(chest.getBlockInventory());}catch(RegistryException e){fail(p,e.getMessage());}},"Add or remove your stock normally.","New item types need a price before they sell.");
        button(page,15,Material.RED_CONCRETE,"Close shop",()->closeShopConfirm(p,shop),"Keeps every item in the chest.");button(page,22,Material.BARRIER,"Back",()->cancel(p));open(p,page);
    }
    private void closeShopConfirm(Player p,ShopRecord shop){
        Page page=page(p,ViewKind.CLOSE_CONFIRM,"Close this shop?",27);
        button(page,11,Material.LIME_CONCRETE,"Confirm: return to storage",()->{try{plugin.getRegistry().remove(p,shop.id());cancel(p);p.sendMessage(ChatColor.GREEN+"Shop closed. Your chest items are unchanged.");}catch(RegistryException e){fail(p,e.getMessage());}},"All stock stays in the chest.");button(page,15,Material.BARRIER,"Keep shop open",()->showManage(p,shop));open(p,page);
    }
}
