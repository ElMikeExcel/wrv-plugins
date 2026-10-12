package wrv.harness;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import com.earth2me.essentials.Essentials;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.Acrobot.ChestShop.Events.*;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Listeners.PostTransaction.EconomicModule;
import com.Acrobot.ChestShop.Listeners.PostTransaction.ItemManager;
import com.Acrobot.ChestShop.Listeners.Economy.TaxModule;
import com.Acrobot.ChestShop.Listeners.Economy.Plugins.VaultListener;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/** Disposable game-plugin integration only. Synthetic accounts and test inventories; never deploy. */
public final class FullLegacyRuntimeHarness extends JavaPlugin implements Listener {
    private final List<String> results=new ArrayList<>();
    private final Deque<Step> steps=new ArrayDeque<>();
    private int assertions, failed, passed, transactions, preEvents;
    private FakePlayer owner,buyer,other;
    private Plugin shops, chestshop;
    private Object ui,bridge;
    private Economy economy;
    private Essentials essentials;
    private World world;
    private Sign sign;
    private State before,expected;
    private InventoryView oldView;
    private TransactionEvent lastTransaction;
    private String mode;
    private int nextDelay=6;
    private UUID pendingQuote;
    private ItemStack exactItem;
    private boolean injectItemPhaseFailure, prematureReceipt;
    private int messageStart;
    @Override public void onEnable(){Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getScheduler().runTaskLater(this,this::start,40);}
    @EventHandler(priority=EventPriority.MONITOR) public void pre(PreTransactionEvent event){preEvents++;}
    @EventHandler(priority=EventPriority.MONITOR) public void transaction(TransactionEvent event){if(!event.isCancelled()){transactions++;lastTransaction=event;}}
    private void start(){
        try{setup();if(mode.equals("full")||mode.equals("restart"))fullSuite();else if(mode.equals("partial-failure"))failureSuite();else pausedSuite();}
        catch(Throwable e){failed++;results.add("FAIL setup: "+e);e.printStackTrace();}
        next();
    }
    private void setup() throws Exception {
        mode=System.getProperty("legacy.test.mode","full");
        check(Bukkit.getVersion().equals("26.2-123-5001879 (MC: 26.2)"),"exact Paper 26.2 build123");
        check(System.getProperty("java.runtime.version").equals("25.0.4+7-LTS"),"exact Java runtime");
        for(String name:List.of("Vault","Essentials","GriefPrevention","ChestShop")){Plugin p=Bukkit.getPluginManager().getPlugin(name);check(p!=null&&p.isEnabled(),"actual plugin enabled "+name+" "+(p==null?"absent":p.getDescription().getVersion()));}
        chestshop=Bukkit.getPluginManager().getPlugin("ChestShop");shops=Bukkit.getPluginManager().getPlugin("ExcelShops");
        check(mode.equals("missing-excel")?shops==null:shops!=null&&shops.isEnabled(),"expected ExcelShops lifecycle state for "+mode);
        if(shops!=null){ui=invoke(shops,"getLegacyUi");bridge=invoke(shops,"getLegacyBridge");}
        economy=Bukkit.getServicesManager().load(Economy.class);check(economy!=null&&economy.getName().equals("EssentialsX Economy"),"actual EssentialsX provider: "+(economy==null?"absent":economy.getName()));
        results.add("PROVIDER_CLASS "+economy.getClass().getName());
        essentials=(Essentials)Bukkit.getPluginManager().getPlugin("Essentials");world=Bukkit.getWorlds().getFirst();
        owner=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000001"),"LegacyOwner",world);
        buyer=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000002"),"LegacyBuyer",world);
        other=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000003"),"LegacyOther",world);
        installLookup();
        for(FakePlayer player:List.of(owner,buyer,other)){NameManager.getOrCreateAccount(player.uuid,player.name);check(essentials.getUser(player.player)!=null,"actual synthetic Essentials account "+player.name);if(!mode.equals("recovery"))money(player,10000);check(economy.hasAccount(player.player),"actual provider account "+player.name);}
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunkAt(x,z).load();
        var gp=GriefPrevention.instance;check(gp!=null,"actual GP loaded");
        if(gp.dataStore.getClaimAt(new Location(world,0,5,0),false,null)==null)check(gp.dataStore.createClaim(world,-4,8,0,20,-4,8,owner.uuid,null,null,null).succeeded,"actual GP claim created");
        check(gp.dataStore.getClaimAt(new Location(world,0,5,0),false,null).getOwnerID().equals(owner.uuid),"actual GP owner lookup");
        if(mode.equals("full")||mode.equals("restart")||mode.equals("partial-failure")){check(ui!=null&&bridge!=null&&(boolean)invoke(bridge,"available"),"paired confirmation bridge available");check(chestshop.getDescription().getVersion().equals("3.13-pre-1 (build 474)+confirmation.1"),"paired ChestShop version");}
        if(mode.equals("recovery")){sign=currentSign();buyer.z=2.5;check(Files.exists(shops.getDataFolder().toPath().resolve("legacy-checkout/active-session")),"existing interrupted evidence remains before probes");check(economy.getBalance(buyer.player)==9990&&economy.getBalance(owner.player)==10010,"actual incomplete-trade balances preserved across restart");check(inventory().isEmpty(),"actual incomplete-trade chest state preserved across restart");}else fixture();
    }
    @EventHandler(priority=EventPriority.NORMAL) public void betweenMoneyAndItems(TransactionEvent event){
        if(event.isCancelled()||buyer==null||!event.getClient().getUniqueId().equals(buyer.uuid))return;
        if(receiptsSinceStart())prematureReceipt=true;
        if(injectItemPhaseFailure)inventory().clear();
    }
    private boolean receiptsSinceStart(){return buyer.messages.stream().skip(messageStart).map(String::toLowerCase).anyMatch(m->m.contains("you bought")||m.contains("you sold")||m.contains("trade complete"));}
    private void failureSuite(){
        flow("injected-item-phase-failure",false,4,()->{},()->{pendingQuote=quoteId();injectItemPhaseFailure=true;click(11);},()->{
            injectItemPhaseFailure=false;
            check(!prematureReceipt,"no success receipt between money and items");
            check(!receiptsSinceStart(),"failed delivery never emits bought/sold/trade-complete success");
            check((boolean)invoke(invoke(shops,"getCheckout"),"recoveryRequired"),"ambiguous item failure globally pauses checkout");
            check(buyer.count(new ItemStack(Material.DIAMOND))==0,"failed delivery gives no false-success items");
            check(economy.getBalance(buyer.player)==before.buyerMoney-10&&economy.getBalance(owner.player)==before.ownerMoney+10,"test confirms money phase occurred before injected external inventory change");
            State after=capture();Object replay=invoke(bridge,"confirm",buyer.player,pendingQuote);check(!(boolean)invoke(replay,"success"),"ambiguous quote cannot replay");same(after,"ambiguous quote retry has no second mutation");
            Path journal=shops.getDataFolder().toPath().resolve("legacy-checkout/transactions.log");String text=Files.readString(journal);check(text.contains("LEGACY_INTENT")&&text.contains("RECOVERY_REQUIRED"),"durable legacy intent and recovery evidence recorded");
            check(Files.exists(shops.getDataFolder().toPath().resolve("legacy-checkout/active-session")),"recovery marker survives ambiguous outcome");
        });
    }
    private void fullSuite() {
        trade("buy",false,4,()->{},4,10,false);
        trade("sell",true,4,()->buyer.storage.setItem(0,new ItemStack(Material.DIAMOND,12)),4,5,false);
        trade("free-buy",false,4,()->line(2,"B 0:S 0"),4,0,false);
        trade("free-sell",true,4,()->{line(2,"B 0:S 0");buyer.storage.setItem(0,new ItemStack(Material.DIAMOND,12));},4,0,false);
        trade("admin-finite-stock",false,4,()->line(0,"Admin Shop"),4,10,true);
        trade("admin-infinite-stock",false,4,()->{line(0,"Admin Shop");world.getBlockAt(0,5,0).setType(Material.AIR,false);},4,10,true);
        trade("double-chest",false,4,this::doubleChest,4,10,false);
        trade("partial-stock",false,4,()->stock(3),3,7.5,false);
        trade("partial-funds",false,4,()->money(buyer,7.5),3,7.5,false);
        trade("explicit-item-quantity",false,9,()->{},9,22.5,false);
        trade("amount-command",false,4,()->{},4,10,false);
        trade("exact-item-metadata",false,4,()->{exactItem=new ItemStack(Material.DIAMOND,20);ItemMeta meta=exactItem.getItemMeta();meta.setDisplayName("Original named diamond");meta.setLore(List.of("Original item lore","Metadata must survive"));meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING,3,true);exactItem.setItemMeta(meta);line(3,com.Acrobot.ChestShop.Utils.ItemUtil.getSignName(exactItem));inventory().setItem(0,exactItem);},4,10,false);
        flow("right-tap-final",false,4,()->{},()->{InventoryClickEvent e=new InventoryClickEvent(buyer.view,InventoryType.SlotType.CONTAINER,11,ClickType.RIGHT,InventoryAction.PICKUP_HALF);Bukkit.getPluginManager().callEvent(e);check(e.isCancelled(),"right final tap is read-only UI input");},()->{check(economy.getBalance(buyer.player)==before.buyerMoney-10,"right final tap exact charge");check(count(buyer.storage,Material.DIAMOND)==4,"right final tap exact quantity");});
        flow("cancel-final",false,4,()->{},()->{oldView=buyer.view;click(15);},()->{same(before,"cancel final");staleClick();same(before,"stale final after cancel");});
        flow("back-before-confirm",false,4,()->{},()->{oldView=buyer.view;click(13);click(11);},()->{kind("QUANTITY");same(before,"queued Back beats queued Confirm");staleClick();same(before,"stale final after Back");});
        flow("close-before-confirm",false,4,()->{},()->{oldView=buyer.view;click(11);buyer.player.closeInventory();},()->{same(before,"close cancels queued final");staleClick();same(before,"stale final after close");});
        flow("disconnect",false,4,()->{},()->{oldView=buyer.view;Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(buyer.player,"Synthetic quit"));buyer.online=false;click(11);},()->same(before,"disconnect cancels final"));
        rejectChange("changed-price",()->line(2,"B 11:S 5"));
        rejectChange("changed-sign-quantity",()->line(1,"5"));
        rejectChange("changed-sign-item",()->line(3,"Emerald"));
        rejectChange("changed-owner",()->line(0,other.name));
        rejectChange("changed-stock",()->stock(2));
        rejectChange("changed-stock-metadata",()->{ItemStack item=inventory().getItem(0);ItemMeta meta=item.getItemMeta();meta.setDisplayName("Changed diamonds");item.setItemMeta(meta);inventory().setItem(0,item);});
        rejectChange("changed-buyer-funds",()->money(buyer,9));
        rejectChange("changed-owner-funds",()->money(owner,9999));
        rejectChange("changed-capacity",()->fill(buyer.storage));
        rejectChange("changed-chest-shape",this::doubleChest);
        rejectChange("permission-revoked",()->buyer.denied.add("ChestShop.shop.buy"));
        rejectChange("use-permission-revoked",()->buyer.denied.add("wrvshops.use"));
        rejectChange("buyer-moved-away",()->buyer.x=100);
        rejectChange("sign-removed",()->world.getBlockAt(0,5,1).setType(Material.AIR,false));
        rejectChange("container-removed",()->world.getBlockAt(0,5,0).setType(Material.AIR,false));
        flow("sell-capacity-changed",true,4,()->buyer.storage.setItem(0,new ItemStack(Material.DIAMOND,12)),()->{fill(inventory());expected=capture();click(11);},()->same(expected,"full seller chest refuses changed trade"));
        flow("sell-stock-changed",true,4,()->buyer.storage.setItem(0,new ItemStack(Material.DIAMOND,12)),()->{buyer.clear();expected=capture();click(11);},()->same(expected,"changed seller items refuse trade"));
        flow("all-menu-click-types",false,4,()->{},()->{
            for(ClickType type:ClickType.values())if(type!=ClickType.LEFT&&type!=ClickType.RIGHT){InventoryClickEvent e=new InventoryClickEvent(buyer.view,InventoryType.SlotType.CONTAINER,11,type,InventoryAction.UNKNOWN,1);Bukkit.getPluginManager().callEvent(e);check(e.isCancelled(),"blocked menu click "+type);}
            var drag=new InventoryDragEvent(buyer.view,new ItemStack(Material.DIRT),new ItemStack(Material.DIRT,2),false,Map.of(11,new ItemStack(Material.DIRT)));Bukkit.getPluginManager().callEvent(drag);check(drag.isCancelled(),"menu drag cancelled");
            var outside=new InventoryClickEvent(buyer.view,InventoryType.SlotType.OUTSIDE,-999,ClickType.LEFT,InventoryAction.DROP_ALL_CURSOR);Bukkit.getPluginManager().callEvent(outside);check(outside.isCancelled(),"outside click cancelled");
        },()->{same(before,"non-tap menu gestures never commit");kind("BUY_CONFIRM");});
        flow("fresh-final-after-bad-clicks",false,4,()->{},()->{oldView=buyer.view;click(11);click(11);},()->{check(economy.getBalance(buyer.player)==before.buyerMoney-10,"duplicate final charges exactly once");check(buyer.count(new ItemStack(Material.DIAMOND))==4,"duplicate final delivers exactly once");expected=capture();staleClick();same(expected,"replayed final cannot trade again");});
        flow("expired-final",false,4,()->{},()->{oldView=buyer.view;nextDelay=120;},()->{same(before,"waiting for quote timeout is nonmutating");click(11);});
        add("expired-final-rejected",()->same(before,"expired quote cannot trade"));
        flow("wrong-player-quote",false,4,()->{},()->{pendingQuote=quoteId();Object result=invoke(bridge,"confirm",other.player,pendingQuote);check(!(boolean)invoke(result,"success"),"wrong player cannot confirm buyer quote");same(before,"wrong player quote no mutation");click(11);},()->{check(economy.getBalance(buyer.player)==before.buyerMoney-10,"right player can still confirm exactly once");check(count(buyer.storage,Material.DIAMOND)==4,"right player exact delivery after wrong caller");});
        flow("cancelled-quote-direct-replay",false,4,()->{},()->{pendingQuote=quoteId();click(15);},()->{Object result=invoke(bridge,"confirm",buyer.player,pendingQuote);check(!(boolean)invoke(result,"success"),"cancelled quote direct callback rejected");same(before,"cancelled quote direct replay no mutation");});
        flow("all-inventory-actions",false,4,()->{},()->{for(InventoryAction action:InventoryAction.values()){var e=new InventoryClickEvent(buyer.view,InventoryType.SlotType.CONTAINER,11,ClickType.UNKNOWN,action);Bukkit.getPluginManager().callEvent(e);check(e.isCancelled(),"inventory action cancelled "+action);}},()->same(before,"inventory action enum cannot activate final"));
        flow("missing-economy-at-confirm",false,4,()->{},()->{var services=Bukkit.getServicesManager();var registration=services.getRegistration(Economy.class);services.unregister(Economy.class,economy);try{pendingQuote=quoteId();Object outcome=invoke(bridge,"confirm",buyer.player,pendingQuote);check(!(boolean)invoke(outcome,"success"),"missing or changed provider denies final");same(before,"provider removal leaves game money and stock unchanged");}finally{services.register(Economy.class,economy,registration.getPlugin(),registration.getPriority());}},()->same(before,"restoring provider does not replay pending trade"));
        add("ordinary-sign-inputs",()->{fixture();before=capture();for(Action action:List.of(Action.LEFT_CLICK_BLOCK,Action.RIGHT_CLICK_BLOCK))for(EquipmentSlot hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND))for(boolean sneak:List.of(false,true)){buyer.sneaking=sneak;interact(action,hand);same(before,"sign "+action+" "+hand+" sneak="+sneak);}buyer.sneaking=false;});
        add("shift-bulk-settings",()->{fixture();before=capture();boolean stacks=com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_IN_STACKS;boolean all=com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_EVERYTHING;try{com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_IN_STACKS=true;buyer.sneaking=true;interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,"shift stack no immediate trade");com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_EVERYTHING=true;interact(Action.LEFT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,"shift all no immediate trade");}finally{com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_IN_STACKS=stacks;com.Acrobot.ChestShop.Configuration.Properties.SHIFT_SELLS_EVERYTHING=all;buyer.sneaking=false;}});
        add("stock-and-sign-protection",()->{fixture();protect();});
        add("owner-management-no-trade",()->{fixture();before=capture();Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(owner.player,Action.RIGHT_CLICK_BLOCK,null,sign.getBlock(),BlockFace.SOUTH,EquipmentSlot.HAND));same(before,"owner interaction cannot purchase own stock");});
        add("ordinary-command-entrypoints",()->{fixture();before=capture();buyer.target.block=sign.getBlock();for(String name:List.of("shopinfo","shop","sinfo","csaccess","cstoggle","csVersion","chestshop","iteminfo","iinfo")){var command=Bukkit.getPluginCommand(name);check(command!=null,"registered command "+name);try{command.execute(buyer.player,name,new String[0]);}catch(RuntimeException failure){throw new AssertionError("command "+name+": "+failure);}same(before,"command cannot trade "+name);}for(String name:List.of("excelshop","wrvshop")){check(Bukkit.getPluginCommand(name).execute(buyer.player,name,new String[]{"amount","4"}),"amount command accepted "+name);same(before,"amount command cannot trade "+name);}});
        add("direct-plugin-api-no-confirm",()->{fixture();directApis();});
        add("cancelled-stock-counter",()->{fixture();String[] lines=currentSign().getLines();boolean enabled=com.Acrobot.ChestShop.Configuration.Properties.USE_STOCK_COUNTER;try{com.Acrobot.ChestShop.Configuration.Properties.USE_STOCK_COUNTER=true;Bukkit.getPluginManager().callEvent(transactionEvent(TransactionType.BUY));check(Arrays.equals(lines,currentSign().getLines()),"cancelled event cannot alter sign stock counter");}finally{com.Acrobot.ChestShop.Configuration.Properties.USE_STOCK_COUNTER=enabled;}});
        flow("new-offer-invalidates-old",false,4,()->{},()->{oldView=buyer.view;interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);staleClick();},()->{same(before,"new offer invalidates old final");kind("OFFER");});
        add("nonzero-tax-pauses",()->{fixture();before=capture();com.Acrobot.ChestShop.Configuration.Properties.TAX_AMOUNT=5;try{check(!(boolean)invoke(bridge,"available"),"unsupported tax config closes admission");interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,"nonzero tax preview no mutation");directApis();}finally{com.Acrobot.ChestShop.Configuration.Properties.TAX_AMOUNT=0;}});
        flow("disable-excel-final",false,4,()->{},()->{oldView=buyer.view;Bukkit.getPluginManager().disablePlugin(shops);staleClick();},()->{same(before,"disabled Excel closes pending final");check(chestshop.isEnabled(),"ChestShop protection stays enabled");directApis();interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,"disabled Excel sign cannot trade");protect();});
    }
    private void pausedSuite(){
        add("paused-startup-state",()->{check(bridge==null||!(boolean)invoke(bridge,"available"),"confirmation unavailable for "+mode);if(!mode.equals("recovery"))fixture();before=capture();interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,"paused startup sign no mutation");});
        add("paused-direct-apis",()->{if(!mode.equals("no-guard"))directApis();else{TransactionEvent event=transactionEvent(TransactionType.BUY);Bukkit.getPluginManager().callEvent(event);same(before,"original ChestShop event fence denies trade");}});
        add("paused-protection",this::protect);
    }
    private void rejectChange(String name,Task change){flow(name,false,4,()->{},()->{change.run();expected=capture();click(11);},()->same(expected,name+" final safely rejected"));}
    private void trade(String name,boolean sell,int request,Task prepare,int delivered,double price,boolean admin){flow(name,sell,request,prepare,()->{oldView=buyer.view;click(11);click(11);},()->{
        check(economy.getBalance(buyer.player)==before.buyerMoney+(sell?price:-price),name+" exact customer balance");
        check(economy.getBalance(owner.player)==before.ownerMoney+(admin?0:sell?-price:price),name+" exact owner balance");
        check(count(buyer.storage,Material.DIAMOND)==count(before.player,Material.DIAMOND)+(sell?-delivered:delivered),name+" exact customer inventory");
        if(inventory()!=null)check(count(inventory(),Material.DIAMOND)==count(before.shop,Material.DIAMOND)+(sell?delivered:-delivered),name+" exact chest inventory");
        check(transactions==before.transactions+1,name+" exactly one committed transaction event");
        if(name.equals("exact-item-metadata"))check(buyer.storage.getItem(0).isSimilar(exactItem),"actual item name lore enchant metadata preserved");
        check(!(boolean)invoke(invoke(shops,"getCheckout"),"recoveryRequired"),name+" no recovery lock");check(!prematureReceipt,name+" no premature success before verified delivery");expected=capture();staleClick();same(expected,name+" duplicate/stale UI no second trade");
        if(lastTransaction!=null){Bukkit.getPluginManager().callEvent(lastTransaction);EconomicModule.onBuyTransaction(lastTransaction);ItemManager.shopItemRemover(lastTransaction);ItemManager.inventoryItemRemover(lastTransaction);same(expected,name+" consumed event identity cannot replay");}
    });}
    private void flow(String name,boolean sell,int quantity,Task prepare,Task finalAction,Task checkResult){
        add(name+"/open",()->{fixture();prepare.run();before=capture();interact(Action.RIGHT_CLICK_BLOCK,EquipmentSlot.HAND);same(before,name+" opening nonmutating");});
        add(name+"/choose",()->{kind("OFFER");same(before,name+" chooser nonmutating");click(sell?15:11);});
        add(name+"/quantity",()->{kind("QUANTITY");same(before,name+" quantity nonmutating");if(name.equals("amount-command"))check(Bukkit.getPluginCommand("excelshop").execute(buyer.player,"excelshop",new String[]{"amount",Integer.toString(quantity)}),"quantity command accepted");else check((boolean)invoke(ui,"enterNumber",buyer.player,Integer.toString(quantity)),"quantity entry accepted "+quantity);});
        add(name+"/review",()->{kind(sell?"SELL_REVIEW":"BUY_REVIEW");same(before,name+" review nonmutating");String lore=String.join(" ",buyer.view.getTopInventory().getItem(4).getItemMeta().getLore());check(lore.contains("Total")&&lore.contains("Shop:"),"review shows actual total and counterparty");click(11);});
        add(name+"/final",()->{kind(sell?"SELL_CONFIRM":"BUY_CONFIRM");same(before,name+" final preview nonmutating");finalAction.run();});
        add(name+"/result",checkResult);
    }
    private void directApis() throws Exception {
        State state=capture();
        PreTransactionEvent pre=new PreTransactionEvent(inventory(),buyer.inventory,new ItemStack[]{new ItemStack(Material.DIAMOND,4)},BigDecimal.TEN,buyer.player,NameManager.getAccount(owner.uuid),currentSign(),TransactionType.BUY);
        pre.setCancelled(false);check(pre.isCancelled(),"raw pre-event cannot activate validation");Bukkit.getPluginManager().callEvent(pre);same(state,"direct pre-event no mutation");
        for(TransactionType type:TransactionType.values()){
            TransactionEvent event=transactionEvent(type);event.setCancelled(false);check(event.isCancelled(),"fresh "+type+" event is default-deny");
            Bukkit.getPluginManager().callEvent(event);same(state,"direct "+type+" event no mutation");
            event.setCancelled(false);EconomicModule.onBuyTransaction(event);ItemManager.shopItemRemover(event);ItemManager.inventoryItemRemover(event);same(state,"direct "+type+" handlers no mutation");
            TransactionEvent legacyConstructor=new TransactionEvent(type,inventory(),buyer.inventory,buyer.player,NameManager.getAccount(owner.uuid),new ItemStack[]{new ItemStack(Material.DIAMOND,4)},10.0,currentSign());
            legacyConstructor.setCancelled(false);Bukkit.getPluginManager().callEvent(legacyConstructor);EconomicModule.onBuyTransaction(legacyConstructor);ItemManager.shopItemRemover(legacyConstructor);ItemManager.inventoryItemRemover(legacyConstructor);same(state,"legacy double-price event constructor no mutation");
            for(boolean parent:List.of(false,true)){
                CurrencyTransferEvent transfer=new CurrencyTransferEvent(BigDecimal.TEN,buyer.player,owner.uuid,CurrencyTransferEvent.Direction.PARTNER,parent?event:null);
                Bukkit.getPluginManager().callEvent(transfer);TaxModule.onCurrencyTransfer(transfer);invokeVaultTransfer(transfer);same(state,"direct currency event parent="+parent+" no mutation");
            }
        }
    }
    private void invokeVaultTransfer(CurrencyTransferEvent event)throws Exception {Constructor<VaultListener> constructor=VaultListener.class.getDeclaredConstructor();constructor.setAccessible(true);constructor.newInstance().onCurrencyTransfer(event);}
    private TransactionEvent transactionEvent(TransactionType type){return new TransactionEvent(type,inventory(),buyer.inventory,buyer.player,NameManager.getAccount(owner.uuid),new ItemStack[]{new ItemStack(Material.DIAMOND,4)},BigDecimal.TEN,currentSign());}
    private void protect(){
        State state=capture();
        for(Block block:List.of(world.getBlockAt(0,5,0),world.getBlockAt(0,5,1))){BlockBreakEvent event=new BlockBreakEvent(block,buyer.player);Bukkit.getPluginManager().callEvent(event);check(event.isCancelled(),"ordinary buyer cannot break legacy "+block.getType());}
        SignChangeEvent change=new SignChangeEvent(world.getBlockAt(0,5,1),buyer.player,new String[]{buyer.name,"4","B 1:S 1","Diamond"});Bukkit.getPluginManager().callEvent(change);check(change.isCancelled(),"ordinary buyer cannot change owner sign");
        PlayerInteractEvent chestOpen=new PlayerInteractEvent(buyer.player,Action.RIGHT_CLICK_BLOCK,null,world.getBlockAt(0,5,0),BlockFace.SOUTH,EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(chestOpen);check(chestOpen.isCancelled(),"ordinary buyer physical chest opening denied");
        InventoryMoveItemEvent extraction=new InventoryMoveItemEvent(inventory(),new ItemStack(Material.DIAMOND),Bukkit.createInventory(null,9),true);Bukkit.getPluginManager().callEvent(extraction);check(extraction.isCancelled(),"non-block automated inventory extraction denied by existing protection");
        same(state,"protection checks preserve balances and stock");
    }
    private void fixture() throws Exception {
        if(ui!=null&&shops!=null&&shops.isEnabled())invoke(ui,"cancel",buyer.player);
        for(FakePlayer player:List.of(owner,buyer,other)){player.online=true;player.allowed=true;player.denied.clear();player.clear();player.x=.5;player.y=5;player.z=2.5;player.sneaking=false;money(player,10000);player.view=null;}
        for(int x=-1;x<=2;x++)world.getBlockAt(x,5,0).setType(Material.AIR,false);
        world.getBlockAt(0,5,0).setType(Material.CHEST,false);
        world.getBlockAt(0,5,1).setType(Material.OAK_WALL_SIGN,false);WallSign data=(WallSign)world.getBlockAt(0,5,1).getBlockData();data.setFacing(BlockFace.SOUTH);world.getBlockAt(0,5,1).setBlockData(data,false);
        sign=currentSign();String[] lines={owner.name,"4","B 10:S 5","Diamond"};for(int i=0;i<4;i++)sign.setLine(i,lines[i]);sign.update(true,false);stock(20);buyer.target.block=sign.getBlock();
        messageStart=buyer.messages.size();prematureReceipt=false;
        check(com.Acrobot.ChestShop.Signs.ChestShopSign.isValid(sign),"valid real ChestShop sign fixture");
    }
    private void doubleChest(){
        world.getBlockAt(1,5,0).setType(Material.CHEST,false);
        for(int x=0;x<=1;x++){var data=(org.bukkit.block.data.type.Chest)world.getBlockAt(x,5,0).getBlockData();data.setFacing(BlockFace.NORTH);data.setType(x==0?org.bukkit.block.data.type.Chest.Type.LEFT:org.bukkit.block.data.type.Chest.Type.RIGHT);world.getBlockAt(x,5,0).setBlockData(data,false);}
        check(inventory().getSize()==54,"native connected double chest resolved");inventory().clear();inventory().setItem(30,new ItemStack(Material.DIAMOND,20));
    }
    private void money(FakePlayer player,double amount)throws Exception{essentials.getUser(player.player).setMoney(BigDecimal.valueOf(amount));}
    private void line(int n,String value){Sign fresh=currentSign();fresh.setLine(n,value);fresh.update(true,false);sign=fresh;}
    private Sign currentSign(){return (Sign)world.getBlockAt(0,5,1).getState();}
    private Inventory inventory(){BlockState state=world.getBlockAt(0,5,0).getState();return state instanceof Container c?c.getInventory():null;}
    private void stock(int amount){inventory().clear();inventory().setItem(0,new ItemStack(Material.DIAMOND,amount));}
    private void fill(Inventory inventory){for(int i=0;i<inventory.getSize();i++)inventory.setItem(i,new ItemStack(Material.COBBLESTONE,64));}
    private void interact(Action action,EquipmentSlot hand){Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(buyer.player,action,null,world.getBlockAt(0,5,1),BlockFace.SOUTH,hand));}
    private void click(int slot){check(buyer.view!=null,"menu exists for click "+slot);clickView(buyer.view,slot);}
    private void clickView(InventoryView view,int slot){InventoryClickEvent event=new InventoryClickEvent(view,InventoryType.SlotType.CONTAINER,slot,ClickType.LEFT,InventoryAction.PICKUP_ALL);Bukkit.getPluginManager().callEvent(event);if(shops!=null&&shops.isEnabled())check(event.isCancelled(),"UI click does not move menu items");}
    private void staleClick(){if(oldView!=null)clickView(oldView,11);}
    private void kind(String expectedKind)throws Exception{Object kind=invoke(ui,"currentKind",buyer.uuid);check(kind!=null&&kind.toString().equals(expectedKind),"UI expected "+expectedKind+", found "+kind+"; recent messages "+buyer.messages.stream().skip(Math.max(0,buyer.messages.size()-3)).toList());}
    private State capture(){return new State(economy.getBalance(buyer.player),economy.getBalance(owner.player),economy.getBalance(other.player),cloneItems(buyer.storage.getContents()),inventory()==null?new ItemStack[0]:cloneItems(inventory().getStorageContents()),transactions);}
    private void same(State state,String label){State actual=capture();check(state.buyerMoney==actual.buyerMoney,label+": customer balance unchanged");check(state.ownerMoney==actual.ownerMoney,label+": owner balance unchanged");check(state.otherMoney==actual.otherMoney,label+": unrelated account unchanged");check(Arrays.equals(state.player,actual.player),label+": customer inventory unchanged");check(Arrays.equals(state.shop,actual.shop),label+": chest inventory unchanged");check(state.transactions==actual.transactions,label+": no new committed event");}
    private static ItemStack[] cloneItems(ItemStack[] source){ItemStack[] out=source.clone();for(int i=0;i<out.length;i++)if(out[i]!=null)out[i]=out[i].clone();return out;}
    private static int count(Inventory inventory,Material material){return count(inventory.getStorageContents(),material);}
    private static int count(ItemStack[] contents,Material material){int n=0;for(ItemStack item:contents)if(item!=null&&item.getType()==material)n+=item.getAmount();return n;}
    private void installLookup()throws Exception{
        Server real=chestshop.getServer();Server proxy=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class<?>[]{Server.class},(p,m,args)->{
            if(args!=null&&args.length==1&&Set.of("getPlayer","getPlayerExact","getOfflinePlayer").contains(m.getName()))for(FakePlayer f:List.of(owner,buyer,other))if(f.uuid.equals(args[0])||f.name.equals(args[0]))return m.getName().equals("getOfflinePlayer")||f.online?f.player:null;
            if(m.getName().equals("getOnlinePlayers"))return List.of(owner,buyer,other).stream().filter(f->f.online).map(f->f.player).toList();try{return m.invoke(real,args);}catch(InvocationTargetException e){throw e.getCause();}});
        Field server=JavaPlugin.class.getDeclaredField("server");server.setAccessible(true);for(Plugin plugin:List.of(chestshop,essentials))server.set(plugin,proxy);if(shops!=null)server.set(shops,proxy);
    }
    private static Object invoke(Object object,String name,Object...args)throws Exception{if(object==null)throw new IllegalArgumentException("Missing target for "+name);for(Method method:object.getClass().getMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length){boolean fit=true;for(int i=0;i<args.length;i++)if(args[i]!=null&&!method.getParameterTypes()[i].isInstance(args[i]))fit=false;if(fit)try{return method.invoke(object,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception ex)throw ex;throw e;}}throw new NoSuchMethodException(name);}
    private UUID quoteId()throws Exception{Field field=bridge.getClass().getDeclaredField("quotes");field.setAccessible(true);Object state=((Map<?,?>)field.get(bridge)).get(buyer.uuid);Method method=state.getClass().getDeclaredMethod("view");method.setAccessible(true);Object quote=method.invoke(state);return (UUID)invoke(quote,"id");}
    private void add(String name,Task action){steps.add(new Step(name,action));}
    private void next(){if(steps.isEmpty()){finish();return;}Step step=steps.removeFirst();try{step.action.run();passed++;results.add("PASS "+step.name);getLogger().info("LEGACY_FULL_PASS "+step.name);}catch(Throwable e){failed++;results.add("FAIL "+step.name+": "+e);getLogger().severe("LEGACY_FULL_FAIL "+step.name+": "+e);e.printStackTrace();}int delay=nextDelay;nextDelay=6;Bukkit.getScheduler().runTaskLater(this,this::next,delay);}
    private void check(boolean ok,String detail){assertions++;if(!ok)throw new AssertionError(detail);}
    private void finish(){try{Files.createDirectories(getDataFolder().toPath());Files.writeString(getDataFolder().toPath().resolve(mode+"-results.txt"),String.join("\n",results)+"\nSUMMARY passed="+passed+" assertions="+assertions+" failed="+failed+"\n");}catch(Exception e){failed++;e.printStackTrace();}getLogger().info("LEGACY_FULL_SUMMARY passed="+passed+" assertions="+assertions+" failed="+failed);Bukkit.getScheduler().runTaskLater(this,Bukkit::shutdown,10);}
    private record State(double buyerMoney,double ownerMoney,double otherMoney,ItemStack[] player,ItemStack[] shop,int transactions){}
    private record Step(String name,Task action){}
    @FunctionalInterface private interface Task{void run()throws Exception;}
}
