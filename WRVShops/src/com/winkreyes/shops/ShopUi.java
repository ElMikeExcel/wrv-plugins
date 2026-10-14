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
    public enum ViewKind { MOBILE_ROOT, MOBILE_CATEGORY, MOBILE_HUB, MOBILE_ENTRY, NUMBER_ENTRY, CATALOG, QUANTITY, BUY_REVIEW, BUY_CONFIRM, SETUP_PROMPT, SETUP_ITEMS, SETUP_PRICE, SETUP_REVIEW, SETUP_CONFIRM, MANAGE, CLOSE_CONFIRM }
    public static final class Page implements InventoryHolder {
        final UUID player;
        final ViewKind kind;
        final Map<Integer,Runnable> actions=new HashMap<>();
        Inventory inventory;
        Consumer<String> numberInput;
        // Read at callback execution time so queued clicks compose on the current value.
        long numberValue;
        boolean numberDialog;
        MobileOrigin mobileOrigin;
        Page(UUID player,ViewKind kind){this.player=player;this.kind=kind;}
        @Override public Inventory getInventory(){return inventory;}
        public ViewKind kind(){return kind;}
    }
    private record MobileOrigin(String category, String product) {}
    private static final class Draft {
        final SetupProposal proposal;
        final LinkedHashMap<UUID,ShopOffer> offers=new LinkedHashMap<>();
        Draft(SetupProposal proposal){this.proposal=proposal;}
    }
    private final WRVShops plugin;
    private final BedrockForms forms;
    private final JavaNumberDialogs dialogs;
    private final Map<UUID,Page> pages=new HashMap<>();
    public ShopUi(WRVShops plugin){this.plugin=plugin;forms=new BedrockForms(plugin);dialogs=new JavaNumberDialogs(plugin);}
    private static String cash(long minor){return BalanceDisplay.money(minor);}
    private static String stockValue(long unit,int stock){return BalanceDisplay.money(Math.multiplyExact(unit,(long)stock));}
    public ViewKind currentKind(UUID id){Page p=pages.get(id);return p==null?null:p.kind;}
    Object sessionToken(UUID id){return pages.get(id);}
    private String balance(Player p){return plugin.getBalanceDisplay()==null?"Current balance: unavailable":plugin.getBalanceDisplay().current(p,false);}
    private String preview(Player p,long total){return plugin.getBalanceDisplay()==null?balance(p)+"\nPurchase cost: -"+cash(total)+"\nBalance after purchase: unavailable":plugin.getBalanceDisplay().preview(p,total,false,false);}
    private String title(Player p,String title){return plugin.getBalanceDisplay()==null?"Balance unavailable | "+title:plugin.getBalanceDisplay().title(p,title,false);}
    private String withBalance(Player p,String text){return balance(p)+"\n"+text;}
    private Page page(Player p,ViewKind kind,String title,int size){
        if(plugin.getLegacyUi()!=null)plugin.getLegacyUi().disconnected(p.getUniqueId());
        Page previous=pages.get(p.getUniqueId());if(previous!=null&&previous.numberDialog)dialogs.close(p);
        Page page=new Page(p.getUniqueId(),kind);page.inventory=Bukkit.createInventory(page,size,title(p,title));
        pages.put(p.getUniqueId(),page);return page;
    }
    private void balanceIcon(Player p,Page page){button(page,page.inventory.getSize()-2,Material.GOLD_NUGGET,balance(p),null,"Display only. No money moves here.","Balance is rechecked before a trade.");}
    private void open(Player p,Page page){balanceIcon(p,page);p.openInventory(page.inventory);}
    private void button(Page page,int slot,Material material,String title,Runnable action,String... lore){button(page,slot,new ItemStack(material),title,action,lore);}
    private void button(Page page,int slot,ItemStack item,String title,Runnable action,String... lore){
        ItemStack icon=item.clone();icon.setAmount(Math.max(1,Math.min(icon.getAmount(),icon.getMaxStackSize())));
        ItemMeta meta=icon.getItemMeta();meta.setDisplayName(ChatColor.RESET+title);List<String> details=new ArrayList<>();if(meta.hasLore())details.addAll(meta.getLore());if(!details.isEmpty())details.add("");details.addAll(Arrays.stream(lore).flatMap(s->Arrays.stream(s.split("\\R"))).map(s->ChatColor.GRAY+s).toList());meta.setLore(details);icon.setItemMeta(meta);page.inventory.setItem(slot,icon);
        if(action!=null)page.actions.put(slot,action);else page.actions.remove(slot);
    }
    private void later(Player player,Page expected,Runnable action){
        plugin.getServer().getScheduler().runTask(plugin,()->{if(plugin.isEnabled()&&player.isOnline()&&pages.get(player.getUniqueId())==expected){if(plugin.getCheckout().recoveryRequired()){fail(player,"Shops are paused for an administrator check. Please do not retry the purchase.");return;}try{action.run();}catch(Exception e){fail(player,e.getMessage());}}});
    }
    private void fail(Player p,String message){cancel(p);p.sendMessage(ChatColor.RED+(message==null?"That shop is unavailable. Please try again.":message));}
    public void cancel(Player p){Page old=pages.remove(p.getUniqueId());if(old!=null&&old.numberDialog)dialogs.close(p);plugin.getCheckout().invalidateBuyer(p.getUniqueId());p.closeInventory();}
    public void disconnected(UUID id){pages.remove(id);plugin.getCheckout().invalidateBuyer(id);}
    public void closeAll(){for(UUID id:List.copyOf(pages.keySet())){Player p=Bukkit.getPlayer(id);if(p!=null)cancel(p);}pages.clear();}
    public boolean enterNumber(Player p,String value){Page page=pages.get(p.getUniqueId());if(page==null||page.numberInput==null)return false;page.numberInput.accept(value);return true;}
    private static int parseQuantity(String value,int max){
        String text=value==null?"":value.trim();
        if(!text.matches("[0-9]{1,4}"))throw new IllegalArgumentException("Enter a whole number from 1 to "+max+".");
        int quantity=Integer.parseInt(text);
        if(quantity<1||quantity>max)throw new IllegalArgumentException("Enter a whole number from 1 to "+max+".");
        return quantity;
    }
    private static long parsePrice(String value){return Money.parse(value==null?null:value.trim());}
    /** A new token separates typed entry from old queued +/- clicks and stale form responses. */
    private void requestNumber(Player p,String title,java.util.function.Supplier<String> details,String initial,Consumer<String> validate,Consumer<String> accept,Runnable back){
        requestNumber(p,title,details,initial,validate,accept,back,null);
    }
    private void requestNumber(Player p,String title,java.util.function.Supplier<String> details,String initial,Consumer<String> validate,Consumer<String> accept,Runnable back,String error){
        Page entry=page(p,ViewKind.NUMBER_ENTRY,title,9);
        entry.numberInput=value->{
            try{validate.accept(value);}catch(IllegalArgumentException invalid){
                p.sendMessage(ChatColor.RED+invalid.getMessage());
                requestNumber(p,title,details,initial,validate,accept,back,invalid.getMessage());return;
            }
            accept.accept(value);
        };
        p.closeInventory();
        Consumer<String> submit=value->later(p,entry,()->entry.numberInput.accept(value));
        Runnable cancel=()->later(p,entry,back);
        String prompt=error==null?details.get():error+"\n\n"+details.get();
        boolean sent=forms.number(p,title,prompt,initial,submit,cancel);
        if(!sent&&forms.canUseJavaDialog(p)){entry.numberDialog=true;sent=dialogs.number(p,title,prompt,initial,submit,cancel);if(!sent)entry.numberDialog=false;}
        if(!sent){p.sendMessage(ChatColor.YELLOW+"Text entry is unavailable on this client. Use the amount buttons for now.");back.run();return;}
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            if(plugin.isEnabled()&&p.isOnline()&&pages.get(p.getUniqueId())==entry){
                p.sendMessage(ChatColor.YELLOW+"Amount entry expired. Choose Enter Amount to try again.");back.run();
            }
        },1200L);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Page page))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!page.player.equals(p.getUniqueId())||pages.get(p.getUniqueId())!=page)return;
        if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
        if(e.getRawSlot()<0||e.getRawSlot()>=page.inventory.getSize())return;
        Runnable action=page.actions.get(e.getRawSlot());if(action!=null)later(p,page,action);
    }
    @EventHandler public void opened(InventoryOpenEvent e){
        Page current=pages.get(e.getPlayer().getUniqueId());
        if(current!=null&&(current.kind==ViewKind.NUMBER_ENTRY||isMobilePage(current)||current.mobileOrigin!=null)&&e.getInventory()!=current.inventory&&e.getPlayer() instanceof Player p){
            pages.remove(p.getUniqueId());if(current.numberDialog)dialogs.close(p);plugin.getCheckout().invalidateBuyer(p.getUniqueId());
        }
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
    private MobileCatalog mobileCatalog(){
        MobileCatalog catalog=plugin.getMobileCatalog();
        if(catalog==null)throw new IllegalStateException("Mobile shop details are unavailable. Please try again later.");
        return catalog;
    }
    private static boolean isMobilePage(Page page){
        return page.kind==ViewKind.MOBILE_ROOT||page.kind==ViewKind.MOBILE_CATEGORY
                ||page.kind==ViewKind.MOBILE_HUB||page.kind==ViewKind.MOBILE_ENTRY;
    }
    /** Only the verified DeluxeMenus entry bridge calls this; there is no arbitrary route command. */
    public void showMobileMenu(Player p,String menu){
        cancel(p); // Retire older inventory/form tokens and any quote before entering native navigation.
        if(!p.hasPermission("wrvshops.use")){p.sendMessage(ChatColor.RED+"You cannot use shops.");return;}
        if(plugin.getCheckout().recoveryRequired()){fail(p,"Shops are paused for an administrator check.");return;}
        try{
            if(menu.equals("spawnshop"))showMobileRoot(p);
            else if(menu.startsWith("spawnshop_"))showMobileCategory(p,menu.substring("spawnshop_".length()));
            else throw new IllegalArgumentException("Unknown mobile menu.");
        }catch(RuntimeException failure){fail(p,failure.getMessage());}
    }
    private void mobileExpiry(Player p,Page expected){
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            if(plugin.isEnabled()&&p.isOnline()&&pages.get(p.getUniqueId())==expected){
                fail(p,"Mobile shop session expired. Reopen /shop to continue.");
            }
        },1200L);
    }
    private void mobileUnavailable(Player p){fail(p,"Mobile shop forms are unavailable. The shop is closed; no purchase was started. Try /shop later.");}
    private void mobileChoices(Player p,Page page,String title,String text,String[] labels,java.util.function.IntConsumer action){
        p.closeInventory();
        if(!forms.choices(p,title,text,labels,id->later(p,page,()->{
            if(!p.hasPermission("wrvshops.use")){fail(p,"You can no longer use shops.");return;}
            action.accept(id);
        }),()->later(p,page,()->cancel(p)))){mobileUnavailable(p);return;}
        mobileExpiry(p,page);
    }
    private void showMobileRoot(Player p){
        List<MobileCatalog.Category> categories=List.copyOf(mobileCatalog().categories());
        Page page=page(p,ViewKind.MOBILE_ROOT,"Spawn Shop",9);
        String[] labels=new String[categories.size()+2];
        for(int i=0;i<categories.size();i++)labels[i]=categories.get(i).name()+" ("+categories.get(i).products().size()+" products)";
        labels[categories.size()]="Back to Menu";labels[categories.size()+1]="Cancel";
        mobileChoices(p,page,"Spawn Shop",balance(p)+"\nChoose a category. Each item has its own details screen.\nBrowsing never spends money.",labels,
            id->{if(id==categories.size()){cancel(p);if(!p.performCommand("menu"))p.sendMessage(ChatColor.RED+"The main menu could not open.");}
                else if(id==categories.size()+1)cancel(p);else showMobileCategory(p,categories.get(id).id());});
    }
    private String mobileLore(Player p,List<String> lore){
        // Expand only this known balance token. Never evaluate arbitrary placeholders or commands.
        String snapshot=balance(p);String value=snapshot.startsWith("Current balance: ")?snapshot.substring("Current balance: ".length()):"unavailable";
        return String.join("\n",lore.stream().map(MobileCatalog::plain)
            .map(line->line.replace("$%vault_eco_balance_2dp%",value).replace("%vault_eco_balance_2dp%",value)).toList());
    }
    private void showMobileCategory(Player p,String categoryId){
        MobileCatalog.Category category=mobileCatalog().category(categoryId);
        Page page=page(p,ViewKind.MOBILE_CATEGORY,category.name(),9);
        List<MobileCatalog.Product> products=category.products();String[] labels=new String[products.size()+2];
        for(int i=0;i<products.size();i++){
            MobileCatalog.Product product=products.get(i);
            labels[i]=MobileCatalog.labelName(product)+" — view details\n"+product.bundleSize()+(product.bundleSize()>1?" per bundle":" item")+" • "+cash(product.price());
        }
        labels[products.size()]="Back to categories";labels[products.size()+1]="Cancel";
        mobileChoices(p,page,category.name(),balance(p)+"\n"+mobileLore(p,category.lore())+"\nChoose an item to read its own details. No charge yet.",labels,id->{
            if(id==products.size())showMobileRoot(p);
            else if(id==products.size()+1)cancel(p);
            else showMobileHub(p,products.get(id),1); // Every new product selection starts with its own identity and count.
        });
    }
    private ShopAdapter.Selection mobileSelection(MobileCatalog.Product product){
        ShopAdapter.Selection selection=plugin.getAdapter().spawn(product.id());mobileCatalog().validate(product,selection);return selection;
    }
    private String mobileDetails(Player p,MobileCatalog.Product product,int count){
        String details=product.name()+"\n"+mobileLore(p,product.lore());
        if(product.route()==MobileCatalog.Route.ORDINARY){
            ShopAdapter.Selection selection=mobileSelection(product);
            return details+"\n\nSelected quantity: "+count+(product.bundleSize()>1?" bundles = "+Math.multiplyExact(count,product.bundleSize())+" items":" item(s)")
                +"\n"+cash(selection.price())+" × "+count+" = "+cash(Money.multiply(selection.price(),count))
                +"\n"+preview(p,Money.multiply(selection.price(),count))+"\nNo money moves until final Confirm purchase.";
        }
        return details+"\n\nFixed quantity: 1\n"+preview(p,product.price())+"\nOpen the existing guarded review; only its Confirm button can purchase.";
    }
    private void showMobileHub(Player p,MobileCatalog.Product product,int count){
        int max=product.route()==MobileCatalog.Route.ORDINARY?MobileCatalog.maximum(mobileSelection(product)):1;
        if(max<1||count<1||count>max){fail(p,"That quantity is no longer available. Reopen the item to review it again.");return;}
        Page page=page(p,ViewKind.MOBILE_HUB,product.name(),9);page.mobileOrigin=new MobileOrigin(product.category(),product.id());
        String[] labels=product.route()==MobileCatalog.Route.ORDINARY
            ?new String[]{"Enter exact amount","Review this quantity","Back to "+mobileCatalog().category(product.category()).name(),"Cancel"}
            :new String[]{"Open guarded review","Back to "+mobileCatalog().category(product.category()).name(),"Cancel"};
        mobileChoices(p,page,product.name(),mobileDetails(p,product,count),labels,id->{
            if(product.route()==MobileCatalog.Route.ORDINARY){
                if(id==0)mobileExactAmount(p,product,count);
                else if(id==1)buyReview(p,mobileSelection(product),count,new MobileOrigin(product.category(),product.id()));
                else if(id==2)showMobileCategory(p,product.category());else cancel(p);
            }else{
                if(id==0)mobileSpecialReview(p,product);
                else if(id==1)showMobileCategory(p,product.category());else cancel(p);
            }
        });
    }
    private void mobileExactAmount(Player p,MobileCatalog.Product product,int count){mobileExactAmount(p,product,count,null);}
    private void mobileExactAmount(Player p,MobileCatalog.Product product,int count,String error){
        int max=MobileCatalog.maximum(mobileSelection(product));
        Page entry=page(p,ViewKind.MOBILE_ENTRY,"Enter exact amount",9);entry.mobileOrigin=new MobileOrigin(product.category(),product.id());
        p.closeInventory();
        // Details stay in the existing input label. It remains input index zero; no mixed action buttons.
        String details=(error==null?"":error+"\n\n")+mobileDetails(p,product,count)+"\n\nEnter a whole number from 1 to "+max+".\nSubmit returns to this item's details; it does not purchase. Closing cancels.";
        if(!forms.number(p,"Enter exact amount",details,String.valueOf(count),value->later(p,entry,()->{
            if(!p.hasPermission("wrvshops.use")){fail(p,"You can no longer use shops.");return;}
            int currentMaximum=MobileCatalog.maximum(mobileSelection(product));
            final int amount;
            try{amount=MobileCatalog.quantity(value,currentMaximum);}
            catch(IllegalArgumentException invalid){p.sendMessage(ChatColor.RED+invalid.getMessage());mobileExactAmount(p,product,count,invalid.getMessage());return;}
            showMobileHub(p,product,amount);
        }),()->later(p,entry,()->cancel(p)))){mobileUnavailable(p);return;}
        mobileExpiry(p,entry);
    }
    private void mobileSpecialReview(Player p,MobileCatalog.Product product){
        // All strings below are fixed trusted routes. No user input, arbitrary command, OP, or permission change.
        // Floodgate names can contain Unicode. Reject command argument separators/control characters, not alphabets.
        String name=p.getName();
        if(name==null||name.isEmpty()||name.codePoints().anyMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c)||Character.isISOControl(c))){fail(p,"This player name cannot safely open that review.");return;}
        String command=switch(product.route()){
            case VEIL -> "mobveil buy "+name;
            case BASIC_PROTECTION -> "deathprotection buy basic "+name;
            case PROTECTION_POTION -> "deathprotection buy premium "+name;
            default -> throw new IllegalArgumentException("Only a fixed special product may open this review.");
        };
        String dependency=product.route()==MobileCatalog.Route.VEIL?"ExcelMobVeil":"ExcelDeathProtection";
        if(!plugin.getServer().getPluginManager().isPluginEnabled(dependency)){fail(p,"That guarded review is unavailable. No purchase was started.");return;}
        cancel(p); // Consume the native token before dispatch; duplicate or late form responses cannot dispatch twice.
        if(!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),command))
            p.sendMessage(ChatColor.RED+"That guarded review could not open. No purchase was started.");
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
    private void backToCatalog(Player p,ShopAdapter.Selection s){
        if(s.shop()!=null){showShop(p,s.shop());return;}
        // Retire this page and any quote before handing navigation to DeluxeMenus.
        cancel(p);
        if(plugin.getServer().getPluginManager().isPluginEnabled("DeluxeMenus")){
            try{if(p.performCommand("shop"))return;}
            catch(RuntimeException e){plugin.getLogger().warning("Spawn Shop /shop navigation failed; using the built-in catalog.");}
        }
        showSpawn(p); // Keep standalone use available if the launcher is absent or fails.
    }
    public void quantity(Player p,ShopAdapter.Selection s,int count){
        int max=(int)Math.min(Math.min(s.available(),2304/s.bundleSize()),10000000000L/s.price());count=Math.max(1,Math.min(count,max));if(max<1){fail(p,"This item is out of stock.");return;}
        final int n=count;Page page=page(p,ViewKind.QUANTITY,"How many?",45);page.numberValue=n;
        Consumer<String> input=value->{try{int q=parseQuantity(value,max);buyReview(p,s,q);}catch(RuntimeException e){p.sendMessage(ChatColor.RED+"Enter a whole number from 1 to "+max+".");quantity(p,s,(int)page.numberValue);}};
        page.numberInput=input;
        String details=preview(p,Money.multiply(s.price(),n))+"\nShown for "+n+" selected; new amount updates at review.\n"+s.name()+"\n"+cash(s.price())+(s.bundleSize()>1?" per bundle of "+s.bundleSize():" each")+"\nAvailable: "+max+"\nQuantity × price = total. Next: review, then final confirmation.";
        p.closeInventory(); // The new page token prevents closing the old page from cancelling this one.
        if(forms.number(p,"Choose quantity",details,String.valueOf(n),v->later(p,page,()->input.accept(v)),()->later(p,page,()->cancel(p))))return;
        renderQuantity(p,page,s,max);open(p,page);
    }
    /** Updating slots leaves the Java client's current window and cursor position intact. */
    private void renderQuantity(Player p,Page page,ShopAdapter.Selection s,int max){
        int n=(int)page.numberValue;
        balanceIcon(p,page);
        button(page,13,s.item(),s.name(),null,"Quantity: "+n+(s.bundleSize()>1?" bundles ("+(n*s.bundleSize())+" items)":""),cash(s.price())+" × "+n+" = "+cash(Money.multiply(s.price(),n)),"Choose Enter Amount to type a number.","Or use the + / - buttons.",preview(p,Money.multiply(s.price(),n)));
        int[] delta={-64,-8,-1,1,8,64};int[] slots={18,19,20,24,25,26};
        for(int i=0;i<delta.length;i++){final int d=delta[i];button(page,slots[i],d<0?Material.RED_DYE:Material.LIME_DYE,(d>0?"+":"")+d,()->{page.numberValue=Math.max(1,Math.min(max,page.numberValue+d));renderQuantity(p,page,s,max);});}
        button(page,22,Material.NAME_TAG,"Enter Amount",()->requestNumber(p,"Enter quantity",()->preview(p,Money.multiply(s.price(),(int)page.numberValue))+"\nShown for "+page.numberValue+" selected; typed amount updates at review.\n"+s.name()+"\n"+(s.bundleSize()>1?"Quantity is bundles of "+s.bundleSize()+" items.":"Quantity is individual items.")+"\nEnter a whole number from 1 to "+max+".\nNext: review, then final confirmation.",String.valueOf(page.numberValue),v->parseQuantity(v,max),v->buyReview(p,s,parseQuantity(v,max)),()->quantity(p,s,(int)page.numberValue)),"Type the exact quantity, e.g. 19.","No money moves until final confirmation.");
        button(page,31,Material.LIME_CONCRETE,"Review purchase",()->buyReview(p,s,(int)page.numberValue),"Nothing is charged yet.");button(page,36,Material.ARROW,"Back",()->backToCatalog(p,s));button(page,44,Material.BARRIER,"Cancel",()->cancel(p));
    }
    private String purchaseText(Player p,ShopAdapter.Selection s,int quantity){return preview(p,Money.multiply(s.price(),quantity))+"\n"+(s.bundleSize()>1?quantity+" bundles = "+(quantity*s.bundleSize()):String.valueOf(quantity))+" × "+s.name()+"\n"+cash(s.price())+(s.bundleSize()>1?" per bundle":" each")+"\nTotal: "+cash(Money.multiply(s.price(),quantity))+itemDetails(s.item());}
    private String itemDetails(ItemStack item){
        ItemMeta meta=item.getItemMeta();StringBuilder info=new StringBuilder();
        Map<org.bukkit.enchantments.Enchantment,Integer> enchants=new LinkedHashMap<>(meta.getEnchants());
        if(meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta book)enchants.putAll(book.getStoredEnchants());
        enchants.forEach((e,l)->info.append("\n").append(e.getKey().getKey().replace('_',' ')).append(" ").append(l));
        if(meta instanceof org.bukkit.inventory.meta.Damageable d&&d.hasDamage())info.append("\nWear: ").append(d.getDamage());
        if(meta.hasLore())for(String line:meta.getLore())info.append("\n").append(ChatColor.stripColor(line));
        return info.toString();
    }
    private void buyReview(Player p,ShopAdapter.Selection s,int quantity){buyReview(p,s,quantity,null);}
    private void buyReview(Player p,ShopAdapter.Selection s,int quantity,MobileOrigin origin){
        Page page=page(p,ViewKind.BUY_REVIEW,"Review purchase",27);page.mobileOrigin=origin;String text=purchaseText(p,s,quantity);
        p.closeInventory();
        if(forms.choices(p,"Review purchase",text,new String[]{"Continue to confirmation","Change quantity","Cancel"},id->later(p,page,()->{if(id==0)buyConfirm(p,s,quantity,origin);else if(id==1)changePurchaseQuantity(p,s,quantity,origin);else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        if(origin!=null){mobileUnavailable(p);return;}
        button(page,4,s.item(),s.name(),null,text.split("\n"));button(page,11,Material.LIME_CONCRETE,"Continue to confirmation",()->buyConfirm(p,s,quantity,origin),"No money has moved.");button(page,13,Material.ARROW,"Change quantity",()->changePurchaseQuantity(p,s,quantity,origin));button(page,15,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    private void changePurchaseQuantity(Player p,ShopAdapter.Selection s,int quantity,MobileOrigin origin){
        if(origin==null){quantity(p,s,quantity);return;}
        plugin.getCheckout().invalidateBuyer(p.getUniqueId());
        MobileCatalog.Product product=mobileCatalog().product(origin.product());
        if(!product.category().equals(origin.category())||!s.offerId().equals("spawn:"+product.id())){
            fail(p,"That mobile item changed. Please reopen the shop.");return;
        }
        showMobileHub(p,product,quantity);
    }
    private void buyConfirm(Player p,ShopAdapter.Selection s,int quantity){buyConfirm(p,s,quantity,null);}
    private void buyConfirm(Player p,ShopAdapter.Selection s,int quantity,MobileOrigin origin){
        CheckoutService.Quote quote=plugin.getCheckout().quote(p.getUniqueId(),s.seller(),s.offerId(),s.revision(),quantity,Money.multiply(s.price(),quantity),s.fingerprint());
        Page page=page(p,ViewKind.BUY_CONFIRM,"Are you sure?",27);page.mobileOrigin=origin;String text=purchaseText(p,s,quantity)+"\nOnly Confirm purchase spends money.";
        Runnable confirm=()->{
            pages.remove(p.getUniqueId());p.closeInventory();
            CheckoutService.Result result=plugin.getCheckout().confirm(quote.id(),p.getUniqueId());
            if(result.status()==CheckoutService.Status.SUCCESS)p.sendMessage(ChatColor.GREEN+"Bought "+(quantity*s.bundleSize())+" "+s.name()+" for "+cash(quote.amountMinorUnits())+".");
            else p.sendMessage(ChatColor.YELLOW+result.message());
            if(result.status()==CheckoutService.Status.RECOVERY_REQUIRED)plugin.getLogger().severe("Checkout recovery required, transaction "+result.transactionId()+": "+plugin.getCheckout().recoveryReason());
        };
        Runnable back=()->{plugin.getCheckout().cancel(quote.id(),p.getUniqueId());changePurchaseQuantity(p,s,quantity,origin);};
        p.closeInventory();
        if(forms.choices(p,"Are you sure?",text,new String[]{"Confirm purchase: "+cash(quote.amountMinorUnits()),"Back","Cancel"},id->later(p,page,()->{if(id==0)confirm.run();else if(id==1)back.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
        if(origin!=null){mobileUnavailable(p);return;} // cancel() invalidates the freshly created quote.
        button(page,4,s.item(),s.name(),null,purchaseText(p,s,quantity).split("\n"));button(page,11,Material.LIME_CONCRETE,"Confirm purchase: "+cash(quote.amountMinorUnits()),confirm,"This button spends money.");button(page,13,Material.ARROW,"Back",back);button(page,15,Material.BARRIER,"Cancel",()->cancel(p));open(p,page);
    }
    public void offerSetup(Player p,SetupProposal proposal){
        Page page=page(p,ViewKind.SETUP_PROMPT,"Make this a shop?",27);
        Runnable yes=()->{Draft d=new Draft(proposal);plugin.getRegistry().findById(proposal.shopId()).ifPresent(s->s.offers().forEach(o->d.offers.put(o.id(),o)));setupItems(p,d);};
        String text="Make this chest your shop?\nChoose a price for each item, review, then confirm.\nUntil you finish, this is ordinary storage.";
        p.closeInventory();
        if(forms.choices(p,"Make this a shop?",withBalance(p,text),new String[]{"Yes, set up my shop","No, keep ordinary storage"},id->later(p,page,()->{if(id==0)yes.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
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
        Page page=page(p,ViewKind.SETUP_PRICE,"Price per item",54);long safe=Math.max(1,Math.min(10000000000L,price));page.numberValue=safe;
        Consumer<String> input=value->{try{long chosen=parsePrice(value);saveDraftPrice(draft,item,chosen);setupItems(p,draft);}catch(RuntimeException e){p.sendMessage(ChatColor.RED+"Use a positive price with up to 2 decimals.");setupPrice(p,draft,item,page.numberValue);}};page.numberInput=input;
        p.closeInventory();
        if(forms.number(p,"Set unit price",balance(p)+"\n"+ShopAdapter.name(item)+"\nStock in chest: "+InventoryPlan.count(draft.proposal.contents(),item)+"\nEnter the price for ONE item. You will review before publishing.",java.math.BigDecimal.valueOf(safe,2).toPlainString(),v->later(p,page,()->input.accept(v)),()->later(p,page,()->cancel(p))))return;
        renderPrice(p,page,draft,item);open(p,page);
    }
    private void renderPrice(Player p,Page page,Draft draft,ItemStack item){
        long price=page.numberValue;
        balanceIcon(p,page);
        button(page,13,item,ShopAdapter.name(item),null,"Unit price: "+cash(price),"Stock: "+InventoryPlan.count(draft.proposal.contents(),item),"Choose Enter Amount to type a price.","Or use the + / - buttons.");
        long[] deltas={-10000,-1000,-100,-10,-1,1,10,100,1000,10000};int[] slots={18,19,20,21,22,27,28,29,30,31};
        for(int i=0;i<deltas.length;i++){final long d=deltas[i];button(page,slots[i],d<0?Material.RED_DYE:Material.LIME_DYE,(d<0?"-":"+")+cash(Math.abs(d)),()->{page.numberValue=Math.max(1,Math.min(Money.MAX_MINOR_UNITS,page.numberValue+d));renderPrice(p,page,draft,item);});}
        button(page,24,Material.NAME_TAG,"Enter Amount",()->requestNumber(p,"Enter price per item",()->balance(p)+"\n"+ShopAdapter.name(item)+"\nPrice for ONE item, e.g. 0.50.\nUse 0.01 to 100000000.00, up to 2 decimals.\nYou will review before publishing.",Money.format(page.numberValue),v->parsePrice(v),v->{saveDraftPrice(draft,item,parsePrice(v));setupItems(p,draft);},()->setupPrice(p,draft,item,page.numberValue)),"Type the exact unit price, e.g. 0.50.","Saves to this review only.");
        button(page,40,Material.LIME_CONCRETE,"Use "+cash(price)+" per item",()->{saveDraftPrice(draft,item,page.numberValue);setupItems(p,draft);},"Saves to this review only.");button(page,45,Material.ARROW,"Back",()->setupItems(p,draft));button(page,53,Material.BARRIER,"Cancel setup",()->cancel(p));
    }
    private void saveDraftPrice(Draft d,ItemStack item,long price){ShopOffer previous=draftOffer(d,item);UUID id=previous==null?UUID.randomUUID():previous.id();d.offers.put(id,new ShopOffer(id,item,price,2304));}
    private void setupReview(Player p,Draft draft,boolean confirm){
        Page page=page(p,confirm?ViewKind.SETUP_CONFIRM:ViewKind.SETUP_REVIEW,confirm?"Publish this shop?":"Review your shop",54);
        StringBuilder text=new StringBuilder("Owner: "+p.getName()+"\nAll matching chest stock will be for sale.\n");int slot=0;
        for(ShopOffer o:draft.offers.values()){int stock=InventoryPlan.count(draft.proposal.contents(),o.item());text.append(ShopAdapter.name(o.item())).append(": ").append(cash(o.unitPriceCents())).append(" each; stock ").append(stock).append("; total stock value ").append(stockValue(o.unitPriceCents(),stock)).append('\n');button(page,slot++,o.item(),ShopAdapter.name(o.item()),null,cash(o.unitPriceCents())+" each","Stock: "+stock,"Stock value: "+stockValue(o.unitPriceCents(),stock));}
        Runnable next=()->{if(!confirm){setupReview(p,draft,true);return;}try{ShopRecord shop=plugin.getRegistry().commit(p,draft.proposal,List.copyOf(draft.offers.values()));cancel(p);p.sendMessage(ChatColor.GREEN+"Your shop is ready. Players can right-click the chest or its attached sign to browse and confirm purchases.");}catch(RegistryException e){fail(p,e.getMessage());}};
        Runnable back=()->setupItems(p,draft);
        p.closeInventory();
        if(forms.choices(p,confirm?"Publish this shop?":"Review your shop",withBalance(p,text.toString()),new String[]{confirm?"Confirm and publish shop":"Continue to confirmation","Change prices","Cancel"},id->later(p,page,()->{if(id==0)next.run();else if(id==1)back.run();else cancel(p);}),()->later(p,page,()->cancel(p))))return;
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
