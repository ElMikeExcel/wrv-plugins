// Excel: prevent asynchronous analytics replies after plugin disable.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
import com.winkreyes.shops.log.Platform;
import com.winkreyes.shops.log.PurchaseLog;
import com.winkreyes.shops.store.*;
import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class WRVShops extends JavaPlugin implements Listener,CommandExecutor {
    private ShopRegistry registry;
    private LegacyDataMigration.Session migrationSession;
    private SpawnCatalog catalog;
    private MobileCatalog mobileCatalog;
    private CheckoutService checkout;
    private ShopAdapter adapter;
    private ShopUi ui;
    private BalanceDisplay balanceDisplay;
    private LegacyShopBridge legacyBridge;
    private LegacyShopUi legacyUi;
    private final Map<BlockPos,PendingSign> placedSigns=new HashMap<>();
    private boolean durabilityFailure;
    private PurchaseLog purchaseLog;private Platform platform;
    private com.winkreyes.shops.checkout.GlobalSessionGuard sessionGuard;
    private FileJournal nativeJournal, legacyJournal;
    private boolean sessionProofReady;
    private final Set<UUID> dirtyPlayers=new HashSet<>(),dirtyWorlds=new HashSet<>();
    private record PendingSign(UUID placer,BlockPos chest,long at){}
    @Override public void onEnable(){
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("excelshop")).setExecutor(this);
        if(getServer().getPluginManager().getPlugin("ChestShop")!=null){
            try{getServer().getPluginManager().registerEvents(new LegacyTransactionFence(()->legacyBridge),this);}
            catch(LinkageError failure){getLogger().severe("Unsupported ChestShop API; the paired guarded build is required: "+failure);}
        }
        try{
            sessionGuard=com.winkreyes.shops.checkout.GlobalSessionGuard.open(getDataFolder().toPath(),getConfig().getBoolean("session-proof-enabled",false));
            // Missing core files/migration cannot be silently treated as a proven no-op session.
            if(getConfig().getBoolean("session-proof-enabled",false) && (!new File(getDataFolder(),"shops.dat").exists() || !new File(getDataFolder(),"config.yml").exists() || !new File(getDataFolder(),"spawn-catalog.yml").exists()))sessionGuard.markUncertain("startup requires initialization or migration");
            migrationSession=LegacyDataMigration.open(getDataFolder().toPath().resolveSibling("WRVShops"),getDataFolder().toPath(),getServer().getPluginManager().getPlugin("WRVShops")!=null);
            getLogger().info("ExcelShops data check: "+migrationSession.outcome());
            saveDefaultConfig();
            registry=new ShopRegistry(this,getConfig().getBoolean("require-grief-prevention",true));registry.load();registry.setMutationGuard(()->beforeShopMutation("registry-edit"));
            RegisteredServiceProvider<Economy> provider=getServer().getServicesManager().getRegistration(Economy.class);
            if(provider==null||provider.getProvider()==null||!provider.getProvider().isEnabled())throw new IllegalStateException("A working Vault economy provider is required");
            File file=new File(getDataFolder(),"spawn-catalog.yml");if(!file.exists())saveResource("spawn-catalog.yml",false);
            catalog=new SpawnCatalog(YamlConfiguration.loadConfiguration(file),
                SpawnCatalog.requireMaxEntries(getConfig().getInt("max-catalog-offers",SpawnCatalog.DEFAULT_MAX_ENTRIES)));
            adapter=new ShopAdapter(this,provider.getProvider());
            balanceDisplay=new BalanceDisplay(this,provider.getProvider());
            if(getConfig().getBoolean("purchase-log",true)){purchaseLog=new PurchaseLog(getDataFolder().toPath(),java.time.ZoneId.systemDefault(),Clock.systemUTC(),msg->getLogger().warning(msg));org.bukkit.plugin.Plugin fg=getServer().getPluginManager().getPlugin("floodgate");platform=Platform.detect(fg!=null&&fg.isEnabled()?fg.getClass().getClassLoader():null);}
            long ttl=getConfig().getLong("quote-seconds",60);if(ttl<1||ttl>300)throw new IllegalArgumentException("quote-seconds must be 1..300");
            nativeJournal=new FileJournal(getDataFolder().toPath().resolve("checkout"));
            checkout=new CheckoutService(adapter,nativeJournal,Clock.systemUTC(),Duration.ofSeconds(ttl));
            // A missing/unsupported ChestShop must never hide an unfinished legacy trade.
            // Probe independently of the optional adapter; a dirty marker is retained by close().
            legacyJournal=new FileJournal(getDataFolder().toPath().resolve("legacy-checkout"));
            nativeJournal.attachGuard(sessionGuard);legacyJournal.attachGuard(sessionGuard);
            if(getConfig().getBoolean("session-proof-enabled",false)) {
                var proofs=Map.of("checkout",nativeJournal.proof(),"legacy-checkout",legacyJournal.proof());
                var decision=sessionGuard.assess(proofs,nativeJournal.markerPresentOnOpen()||legacyJournal.markerPresentOnOpen());
                // Default off. Unknown mutation routes never qualify merely from unchanged journal bytes.
                boolean coverage=true; // Audited ExcelShops-owned routes; out-of-band administrator edits are outside this proof.
                boolean recover=getConfig().getBoolean("no-activity-auto-recovery",false)&&coverage;
                sessionGuard.recordReady(proofs,recover);
                if(decision==com.winkreyes.shops.checkout.GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE) {
                    if(nativeJournal.markerOnlyRecovery())nativeJournal.acceptNoActivityRecovery();
                    if(legacyJournal.markerOnlyRecovery())legacyJournal.acceptNoActivityRecovery();
                    checkout=new CheckoutService(adapter,nativeJournal,Clock.systemUTC(),Duration.ofSeconds(ttl));
                    getLogger().info("Verified no-activity session recovery: both journals and marker identities unchanged; no mutation fence.");
                }
                sessionProofReady=true;
                if(!coverage)sessionGuard.beforeMutation("unknown mutation coverage; automatic recovery disabled");
            }
            if(legacyJournal.requiresRecovery())checkout.pauseExternal("Legacy checkout: "+legacyJournal.recoveryReason());
            try{mobileCatalog=MobileCatalog.load(this);}catch(RuntimeException failure){getLogger().warning("Mobile shop descriptions unavailable: "+failure.getMessage());}
            ui=new ShopUi(this);Objects.requireNonNull(getCommand("excelshop")).setExecutor(this);
            getServer().getPluginManager().registerEvents(ui,this);
            if(getServer().getPluginManager().getPlugin("ChestShop")!=null){
                try{legacyBridge=new LegacyShopBridge(this,provider.getProvider(),ttl,legacyJournal);legacyUi=new LegacyShopUi(this,legacyBridge);legacyBridge.setUi(legacyUi);getServer().getPluginManager().registerEvents(legacyUi,this);}
                catch(Exception|LinkageError failure){durabilityFailure=true;checkout.pauseExternal("Legacy bridge initialization failed: "+failure);getLogger().severe("Legacy ChestShop trading paused: "+failure+". Install the matching guarded ChestShop; do not remove its stock protection.");}
            }
            if(checkout.recoveryRequired())getLogger().severe("Purchases paused: "+checkout.recoveryReason()+". Preserve the journal and reconcile before enabling purchases.");
            if(!checkout.recoveryRequired())getLogger().info("ExcelShops purchases available.");
            getLogger().info("ExcelShops enabled. Legacy ChestShop confirmation: "+(legacyBridge!=null?"guard connected":"unavailable/absent")+". Economy: "+provider.getProvider().getName());
        }catch(Exception|LinkageError e){if(checkout!=null)checkout.pauseExternal("Startup validation failed: "+e);getLogger().severe("ExcelShops entered protection-only mode; purchases/setup unavailable: "+e.getMessage());}
    }
    @Override public void onDisable(){
        if(purchaseLog!=null){purchaseLog.close();purchaseLog=null;}
        if(ui!=null)ui.closeAll();if(legacyUi!=null)legacyUi.closeAll();placedSigns.clear();
        if(checkout==null){closeLegacy(false);closeOwnedJournals();if(sessionGuard!=null)try{sessionGuard.closeUnclean();}catch(Exception ignored){}closeMigration();return;}
        try{
            // Money and world persistence are separate. Only a fully saved clean shutdown may
            // clear the journal's active-session marker. An abrupt crash remains fail-closed.
            if(durabilityFailure)throw new IllegalStateException("A player save failed earlier in this session");
            beforeShopMutation("shutdown-persistence");
            if(registry!=null)registry.all().forEach(shop->dirtyWorlds.add(shop.chest().worldId()));
            for(UUID id:dirtyPlayers){Player p=getServer().getPlayer(id);if(p!=null&&p.isOnline())p.saveData();}
            for(UUID id:dirtyWorlds){World w=getServer().getWorld(id);if(w==null)throw new IllegalStateException("A dirty shop world disappeared");w.save();}
            if(registry!=null)registry.save();closeLegacy(true);if(legacyJournal!=null)legacyJournal.close();checkout.close();
            if(sessionGuard!=null)sessionGuard.closeClean(sessionProofReady&&!checkout.recoveryRequired()&&!durabilityFailure);
        }catch(Exception e){getLogger().severe("Clean shop shutdown could not be verified: "+e.getMessage());closeLegacy(false);try{checkout.closeUnclean();}catch(Exception ignored){}}
        finally{closeOwnedJournals();if(sessionGuard!=null)try{sessionGuard.closeUnclean();}catch(Exception ignored){}closeMigration();}
    }
    private void closeLegacy(boolean clean){
        if(legacyBridge!=null)try{legacyBridge.close(clean);}catch(Exception failure){durabilityFailure=true;getLogger().severe("Legacy checkout journal could not close safely: "+failure);if(checkout!=null)checkout.pauseExternal("Legacy journal close failed");}
    }
    private void closeOwnedJournals(){
        if(nativeJournal!=null)try{nativeJournal.closeUnclean();}catch(Exception failure){getLogger().severe("Native journal release failed: "+failure);}
        if(legacyJournal!=null)try{legacyJournal.closeUnclean();}catch(Exception failure){getLogger().severe("Legacy journal release failed: "+failure);}
    }
    private void closeMigration(){
        if(migrationSession!=null){try{migrationSession.close();}catch(Exception e){getLogger().severe("Could not release migration lock: "+e.getMessage());}finally{migrationSession=null;}}
    }
    public ShopRegistry getRegistry(){return registry;}
    public PurchaseLog getPurchaseLog(){return purchaseLog;}
    /** Server thread, after a SUCCESS checkout. Reads the post-purchase balance here (Vault is main-thread); the file write is async. */
    public void logPurchase(Player p,ShopAdapter.Selection s,int quantity,long totalMinor){
        PurchaseLog log=purchaseLog;if(log==null)return;
        try{
            Long after=balanceDisplay==null?null:balanceDisplay.balanceMinor(p,false);
            log.record(new PurchaseLog.Purchase(java.time.Instant.now(),p.getName(),p.getUniqueId(),platform==null?"unknown":platform.of(p.getUniqueId()),
                s.offerId().startsWith("spawn:")?"spawn":"player",itemId(s.item()),quantity,s.bundleSize(),s.price(),totalMinor,after,s.offerId()));
        }catch(RuntimeException|LinkageError failure){getLogger().warning("Purchase log skipped: "+failure);}
    }
    static String itemId(ItemStack item){
        String id=item.getType().getKey().toString();
        if(item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta book&&!book.getStoredEnchants().isEmpty()){
            StringBuilder b=new StringBuilder(id).append('[');
            book.getStoredEnchants().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey(java.util.Comparator.comparing(e->e.getKey().getKey()))).forEach(e->b.append(b.charAt(b.length()-1)=='['?"":";").append(e.getKey().getKey().getKey()).append(':').append(e.getValue()));
            id=b.append(']').toString();
        }
        return id;
    }
    private boolean stats(CommandSender sender,String[] args){
        if(!sender.hasPermission("wrvshops.admin")){sender.sendMessage(ChatColor.RED+"You cannot view shop stats.");return true;}
        if(purchaseLog==null){sender.sendMessage(ChatColor.YELLOW+"The purchase log is off (purchase-log: false) or shops are paused.");return true;}
        int days=7;
        if(args.length==2){try{days=Integer.parseInt(args[1]);}catch(NumberFormatException e){days=-1;}}
        if(args.length>2||days<1||days>3650){sender.sendMessage(ChatColor.YELLOW+"Usage: /excelshop stats [days 1-3650]");return true;}
        final int d=days;
        purchaseLog.stats(d,10).whenComplete((st,err)->{
            if(!isEnabled())return;
            try { getServer().getScheduler().runTask(this,()->{
            if(!isEnabled())return;
            if(err!=null){sender.sendMessage(ChatColor.RED+"Could not read purchases.csv: "+err.getMessage());return;}
            sender.sendMessage(ChatColor.GOLD+"Shop sales, last "+d+" day"+(d==1?"":"s")+": "+st.purchases()+" purchases by "+st.buyers()+" players, "+BalanceDisplay.money(st.revenueMinor()));
            sender.sendMessage(ChatColor.GRAY+"Spawn shop "+BalanceDisplay.money(st.spawnRevenueMinor())+" | player shops "+BalanceDisplay.money(st.playerRevenueMinor()));
            int i=1;for(PurchaseLog.ItemStat it:st.top())sender.sendMessage(ChatColor.WHITE+""+(i++)+". "+it.key()+": "+BalanceDisplay.money(it.revenueMinor())+" ("+it.purchases()+" buys, "+it.items()+" items)");
            if(st.badLines()>0)sender.sendMessage(ChatColor.YELLOW+""+st.badLines()+" unreadable line(s) skipped.");
        }); } catch(org.bukkit.plugin.IllegalPluginAccessException disabled) { /* Excel: disable won scheduling race. */ }
        });
        return true;
    }
    public CheckoutService getCheckout(){return checkout;}
    public ShopAdapter getAdapter(){return adapter;}
    public SpawnCatalog getCatalog(){return catalog;}
    public MobileCatalog getMobileCatalog(){return mobileCatalog;}
    public ShopUi getUi(){return ui;}
    public BalanceDisplay getBalanceDisplay(){return balanceDisplay;}
    public LegacyShopUi getLegacyUi(){return legacyUi;}
    public LegacyShopBridge getLegacyBridge(){return legacyBridge;}
    public void beforeShopMutation(String reason) throws java.io.IOException {
        if(sessionGuard!=null)sessionGuard.beforeMutation(reason);
    }
    private boolean fenceEvent(org.bukkit.event.Cancellable event,String reason) {
        try{beforeShopMutation(reason);return true;}catch(java.io.IOException failure){event.setCancelled(true);if(checkout!=null)checkout.pauseExternal("Mutation fence failed: "+failure);return false;}
    }
    public void markLegacyDirty(Player p,UUID world){dirtyPlayers.add(p.getUniqueId());dirtyWorlds.add(world);}
    public void markDirty(Player player,ShopRecord shop){dirtyPlayers.add(player.getUniqueId());if(shop!=null)dirtyWorlds.add(shop.chest().worldId());}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(args.length>=1&&args[0].equalsIgnoreCase("stats"))return stats(sender,args);
        if(ui==null){sender.sendMessage("Shops are paused while an administrator checks startup. Stock remains protected.");return true;}
        if(checkout.recoveryRequired()){sender.sendMessage("Shops are paused for administrator reconciliation. Please do not retry purchases.");return true;}
        if(!(sender instanceof Player p)){sender.sendMessage("Use this in game. Recovery has no automatic replay command; see WRVShops/README.md.");return true;}
        if(!p.hasPermission("wrvshops.use")){p.sendMessage(ChatColor.RED+"You cannot use shops.");return true;}
        try{
            if(args.length==0){ui.showSpawn(p);return true;}
            if(args[0].equalsIgnoreCase("browse")){if(args.length==1)ui.showSpawn(p);else if(args.length==2)ui.quantity(p,adapter.spawn(args[1]),1);else return false;return true;}
            if(args[0].equalsIgnoreCase("amount")&&args.length==2){if(!(legacyUi!=null&&legacyUi.enterNumber(p,args[1]))&&!ui.enterNumber(p,args[1]))p.sendMessage(ChatColor.YELLOW+"Open a quantity or price screen first.");return true;}
            if(args[0].equalsIgnoreCase("setup")&&args.length==1){Block sign=p.getTargetBlockExact(5);if(sign==null||!(sign.getState() instanceof Sign))throw new IllegalArgumentException("Look at the ordinary sign attached to your own single chest.");Block chest=attachedChest(sign);if(chest==null)throw new IllegalArgumentException("That sign must be directly attached to your single chest.");ui.offerSetup(p,registry.propose(p,chest,sign));return true;}
            if(args[0].equalsIgnoreCase("manage")&&args.length==1){Block target=p.getTargetBlockExact(5);if(target==null)throw new IllegalArgumentException("Look at your shop chest or its sign.");ShopRecord shop=registry.findByChest(BlockPos.of(target)).or(()->registry.findBySign(BlockPos.of(target))).orElseThrow(()->new IllegalArgumentException("Look at your shop chest or its sign."));if(!shop.ownerId().equals(p.getUniqueId()))throw new IllegalArgumentException("Only this shop's owner can manage it.");ui.showManage(p,shop);return true;}
            return false;
        }catch(Exception e){p.sendMessage(ChatColor.RED+e.getMessage());return true;}
    }
    /** Both wall signs and a standing sign on top are attached to a chest. */
    public static Block attachedChest(Block sign){
        Block candidate=null;
        if(sign.getBlockData() instanceof org.bukkit.block.data.type.WallSign wall)candidate=sign.getRelative(wall.getFacing().getOppositeFace());
        else if(sign.getBlockData() instanceof org.bukkit.block.data.type.Sign)candidate=sign.getRelative(BlockFace.DOWN);
        return candidate!=null&&candidate.getState() instanceof Chest?candidate:null;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void preventAttach(BlockPlaceEvent e){
        if((e.getBlockPlaced().getState() instanceof Chest || e.getBlockPlaced().getState() instanceof Sign)&&wouldAttachToProtected(e.getBlockPlaced())){e.setCancelled(true);e.getPlayer().sendMessage(ChatColor.YELLOW+"Close the shop before changing its chest shape or attaching another sign.");}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fencePlacement(BlockPlaceEvent e){
        if(e.getBlockPlaced().getState() instanceof Chest || e.getBlockPlaced().getState() instanceof Sign)fenceEvent(e,"block-placement");
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fenceBreak(BlockBreakEvent e){
        if(e.getBlock().getState() instanceof Chest || e.getBlock().getState() instanceof Sign)fenceEvent(e,"block-break");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placed(BlockPlaceEvent e){
        if(registry==null||!registry.healthy()||ui==null)return;
        try{
            if(e.getBlockPlaced().getState() instanceof Chest)registry.recordPlaced(e.getBlockPlaced(),e.getPlayer().getUniqueId());
            if(e.getBlockPlaced().getState() instanceof Sign){registry.noteBlockChanged(e.getBlockPlaced());Block chest=attachedChest(e.getBlockPlaced());if(chest!=null){placedSigns.entrySet().removeIf(x->System.currentTimeMillis()-x.getValue().at>300000);placedSigns.put(BlockPos.of(e.getBlockPlaced()),new PendingSign(e.getPlayer().getUniqueId(),BlockPos.of(chest),System.currentTimeMillis()));}}
        }catch(Exception ex){getLogger().warning("Could not record chest ownership: "+ex.getMessage());}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void protectLabel(SignChangeEvent e){if(protectedBlock(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void signFinished(SignChangeEvent e){
        if(ui==null)return;
        PendingSign pending=placedSigns.remove(BlockPos.of(e.getBlock()));
        if(pending==null||!pending.placer.equals(e.getPlayer().getUniqueId())||System.currentTimeMillis()-pending.at>300000||!e.getPlayer().hasPermission("wrvshops.create"))return;
        Player p=e.getPlayer();BlockPos sign=BlockPos.of(e.getBlock());
        // Let vanilla sign editing and every protection handler finish first.
        getServer().getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!p.getWorld().getUID().equals(pending.chest.worldId()))return;
            try{
                Block chest=pending.chest.resolve(getServer()),signBlock=sign.resolve(getServer());
                if(p.getLocation().distanceSquared(chest.getLocation().add(.5,.5,.5))>36)return;
                SetupProposal proposal=registry.propose(p,chest,signBlock);ui.offerSetup(p,proposal);
            }catch(RegistryException ex){p.sendMessage(ChatColor.GRAY+"Shop setup: "+ex.getMessage());}
        },2L);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){
        if(e.getHand()!=EquipmentSlot.HAND||e.getClickedBlock()==null||e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        BlockPos pos=BlockPos.of(e.getClickedBlock());Optional<ShopRecord> found=registry==null?Optional.empty():registry.findByChest(pos).or(()->registry.findBySign(pos));
        if(found.isEmpty()&&!protectedBlock(e.getClickedBlock()))return;
        // The owner intentionally exposes a read-only purchase catalog. Never un-cancel or
        // open the real container for buyers, and never change GriefPrevention trust.
        e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);
        Player p=e.getPlayer();if(ui==null||found.isEmpty()||checkout.recoveryRequired()){p.sendMessage(ChatColor.YELLOW+"This shop is paused for an administrator check.");return;}if(!p.hasPermission("wrvshops.use"))return;
        getServer().getScheduler().runTask(this,()->{if(p.isOnline())ui.showShop(p,found.get());});
    }
    /**
     * Runs before ExcelMenuJoin's compass handler (default priority), which
     * opens the server menu unless the event is already cancelled. A compass
     * tap on a shop sign is a shop action, so a handled route cancels here.
     * The routing rules live in CompassShortcut; this shell gathers the facts.
     */
    @EventHandler(priority=EventPriority.LOWEST) public void compassShortcut(PlayerInteractEvent e){
        if(e.isCancelled()||e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null)return;
        if(ui==null||registry==null||!registry.healthy()||checkout==null||checkout.recoveryRequired())return;
        Block signBlock=e.getClickedBlock();
        boolean sign=signBlock.getState() instanceof Sign;
        Block chest=sign?attachedChest(signBlock):null;
        Optional<ShopRecord> shop=Optional.empty();
        if(sign){
            shop=registry.findBySign(BlockPos.of(signBlock));
            if(shop.isEmpty()&&chest!=null)shop=registry.findByChest(BlockPos.of(chest));
        }
        Player p=e.getPlayer();
        CompassShortcut.Route route=CompassShortcut.route(
            getConfig().getBoolean("compass-shop-shortcut",true),
            CompassShortcut.isMenuCompass(e.getItem()),sign,shop.isPresent(),
            shop.map(s->s.ownerId().equals(p.getUniqueId())).orElse(false),chest!=null);
        if(route==CompassShortcut.Route.MANAGE){
            e.setCancelled(true);
            // A main-hand tap is still opened by interact() below, which runs
            // regardless of cancellation. The off-hand event never reaches it,
            // so only that case opens the manage screen from here.
            if(e.getHand()!=EquipmentSlot.HAND){ShopRecord target=shop.get();getServer().getScheduler().runTask(this,()->{if(p.isOnline())ui.showManage(p,target);});}
        }else if(route==CompassShortcut.Route.SETUP){
            e.setCancelled(true);
            // The same propose/offerSetup path as /excelshop setup, deferred one
            // tick like every other UI opening in this plugin.
            BlockPos chestPos=BlockPos.of(chest),signPos=BlockPos.of(signBlock);
            getServer().getScheduler().runTask(this,()->{
                if(!p.isOnline())return;
                try{ui.offerSetup(p,registry.propose(p,chestPos.resolve(getServer()),signPos.resolve(getServer())));}
                catch(RegistryException ex){p.sendMessage(ChatColor.GRAY+"Shop setup: "+ex.getMessage());}
            });
        }
    }
    private Optional<ShopRecord> inventoryShop(Inventory inventory){
        if(registry==null)return Optional.empty();
        InventoryHolder holder=inventory.getHolder();
        if(holder instanceof Chest chest)return registry.findByChest(BlockPos.of(chest.getBlock()));
        if(holder instanceof DoubleChest dc){InventoryHolder left=dc.getLeftSide(),right=dc.getRightSide();if(left instanceof Chest c){Optional<ShopRecord> s=registry.findByChest(BlockPos.of(c.getBlock()));if(s.isPresent())return s;}if(right instanceof Chest c)return registry.findByChest(BlockPos.of(c.getBlock()));}
        return Optional.empty();
    }
    private boolean protectedBlock(Block block){
        if(registry!=null&&registry.isProtected(BlockPos.of(block)))return true;
        return block.getState() instanceof TileState tile&&tile.getPersistentDataContainer().has(ShopRegistry.SHOP_ID,org.bukkit.persistence.PersistentDataType.STRING);
    }
    private boolean protectedInventory(Inventory inv){
        if(inventoryShop(inv).isPresent())return true;
        InventoryHolder h=inv.getHolder();
        if(h instanceof Chest c)return protectedBlock(c.getBlock());
        if(h instanceof DoubleChest d)return (d.getLeftSide() instanceof Chest c&&protectedBlock(c.getBlock()))||(d.getRightSide() instanceof Chest r&&protectedBlock(r.getBlock()));
        return false;
    }
    private boolean wouldAttachToProtected(Block block){
        return (registry!=null&&registry.wouldAttachToShop(block))||protectedBlock(block.getRelative(BlockFace.NORTH))||protectedBlock(block.getRelative(BlockFace.SOUTH))||protectedBlock(block.getRelative(BlockFace.EAST))||protectedBlock(block.getRelative(BlockFace.WEST));
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void open(InventoryOpenEvent e){
        if(protectedInventory(e.getInventory())&&(checkout==null||checkout.recoveryRequired())){e.setCancelled(true);return;}
        Optional<ShopRecord> s=inventoryShop(e.getInventory());if(s.isEmpty()){if(protectedInventory(e.getInventory()))e.setCancelled(true);return;}
        if(!s.get().ownerId().equals(e.getPlayer().getUniqueId())){e.setCancelled(true);return;}
        try{registry.validate(s.get());if(!e.isCancelled())fenceEvent(e,"stock-inventory-exposure");}catch(RegistryException ex){e.setCancelled(true);e.getPlayer().sendMessage(ChatColor.RED+ex.getMessage());}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void stockClick(InventoryClickEvent e){
        if(protectedInventory(e.getView().getTopInventory())&&(checkout==null||checkout.recoveryRequired())){e.setCancelled(true);return;}
        Optional<ShopRecord> shop=inventoryShop(e.getView().getTopInventory());if(shop.isEmpty()){if(protectedInventory(e.getView().getTopInventory()))e.setCancelled(true);return;}
        if(!shop.get().ownerId().equals(e.getWhoClicked().getUniqueId())){e.setCancelled(true);return;}
        try{registry.validate(shop.get());if(!e.isCancelled())fenceEvent(e,"owner-stock-edit");}catch(RegistryException ex){e.setCancelled(true);}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void stockDrag(InventoryDragEvent e){
        if(protectedInventory(e.getView().getTopInventory())&&(checkout==null||checkout.recoveryRequired())){e.setCancelled(true);return;}
        Optional<ShopRecord> shop=inventoryShop(e.getView().getTopInventory());if(shop.isEmpty()){if(protectedInventory(e.getView().getTopInventory()))e.setCancelled(true);return;}
        if(!shop.get().ownerId().equals(e.getWhoClicked().getUniqueId())){e.setCancelled(true);return;}
        try{registry.validate(shop.get());if(!e.isCancelled())fenceEvent(e,"owner-stock-edit");}catch(RegistryException ex){e.setCancelled(true);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(InventoryMoveItemEvent e){if(protectedInventory(e.getSource())||protectedInventory(e.getDestination()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(InventoryPickupItemEvent e){if(protectedInventory(e.getInventory()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakShop(BlockBreakEvent e){if(protectedBlock(e.getBlock())){e.setCancelled(true);e.getPlayer().sendMessage(ChatColor.YELLOW+"Close this shop from its owner menu before breaking its chest or sign.");}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e){if(registry==null||!registry.healthy())return;try{registry.forgetPlaced(e.getBlock());placedSigns.remove(BlockPos.of(e.getBlock()));}catch(Exception ex){getLogger().warning("Could not update chest ownership: "+ex.getMessage());}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityExplosion(EntityExplodeEvent e){e.blockList().removeIf(b->protectedBlock(b));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockExplosion(BlockExplodeEvent e){e.blockList().removeIf(b->protectedBlock(b));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e){if(protectedBlock(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->protectedBlock(b)||protectedBlock(b.getRelative(e.getDirection()))||protectedBlock(b.getRelative(e.getDirection().getOppositeFace()))))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->protectedBlock(b)||protectedBlock(b.getRelative(e.getDirection()))||protectedBlock(b.getRelative(e.getDirection().getOppositeFace()))))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){if(legacyUi!=null)legacyUi.disconnected(e.getPlayer().getUniqueId());if(ui!=null)ui.disconnected(e.getPlayer().getUniqueId());if(dirtyPlayers.contains(e.getPlayer().getUniqueId()))try{beforeShopMutation("player-disconnect-save");e.getPlayer().saveData();}catch(Exception ex){durabilityFailure=true;getLogger().severe("Player save failed; checkout journal must remain dirty: "+ex.getMessage());}}
    @EventHandler public void changedWorld(PlayerChangedWorldEvent e){if(legacyUi!=null)legacyUi.cancel(e.getPlayer());if(ui!=null)ui.cancel(e.getPlayer());}
    @EventHandler public void teleported(PlayerTeleportEvent e){if(!e.isCancelled()){if(legacyUi!=null)legacyUi.cancel(e.getPlayer());if(ui!=null)ui.cancel(e.getPlayer());}}
}
