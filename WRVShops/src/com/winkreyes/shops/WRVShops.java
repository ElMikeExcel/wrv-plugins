// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
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
    private MobileMenuBridge mobileBridge;
    private CheckoutService checkout;
    private ShopAdapter adapter;
    private ShopUi ui;
    private BalanceDisplay balanceDisplay;
    private LegacyShopBridge legacyBridge;
    private LegacyShopUi legacyUi;
    private final Map<BlockPos,PendingSign> placedSigns=new HashMap<>();
    private boolean durabilityFailure;
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
            migrationSession=LegacyDataMigration.open(getDataFolder().toPath().resolveSibling("WRVShops"),getDataFolder().toPath(),getServer().getPluginManager().getPlugin("WRVShops")!=null);
            getLogger().info("ExcelShops data check: "+migrationSession.outcome());
            saveDefaultConfig();
            registry=new ShopRegistry(this,getConfig().getBoolean("require-grief-prevention",true));registry.load();
            RegisteredServiceProvider<Economy> provider=getServer().getServicesManager().getRegistration(Economy.class);
            if(provider==null||provider.getProvider()==null||!provider.getProvider().isEnabled())throw new IllegalStateException("A working Vault economy provider is required");
            File file=new File(getDataFolder(),"spawn-catalog.yml");if(!file.exists())saveResource("spawn-catalog.yml",false);
            catalog=new SpawnCatalog(YamlConfiguration.loadConfiguration(file));
            adapter=new ShopAdapter(this,provider.getProvider());
            balanceDisplay=new BalanceDisplay(this,provider.getProvider());
            long ttl=getConfig().getLong("quote-seconds",60);if(ttl<1||ttl>300)throw new IllegalArgumentException("quote-seconds must be 1..300");
            checkout=new CheckoutService(adapter,new FileJournal(getDataFolder().toPath().resolve("checkout")),Clock.systemUTC(),Duration.ofSeconds(ttl));
            // A missing/unsupported ChestShop must never hide an unfinished legacy trade.
            // Probe independently of the optional adapter; a dirty marker is retained by close().
            try(FileJournal legacyProbe=new FileJournal(getDataFolder().toPath().resolve("legacy-checkout"))){
                if(legacyProbe.requiresRecovery())checkout.pauseExternal("Legacy checkout: "+legacyProbe.recoveryReason());
            }
            try{mobileCatalog=MobileCatalog.load(this);}catch(RuntimeException failure){getLogger().warning("Mobile shop descriptions unavailable: "+failure.getMessage());}
            ui=new ShopUi(this);Objects.requireNonNull(getCommand("excelshop")).setExecutor(this);
            getServer().getPluginManager().registerEvents(ui,this);
            mobileBridge=new MobileMenuBridge(this);getServer().getPluginManager().registerEvents(mobileBridge,this);
            if(getServer().getPluginManager().getPlugin("ChestShop")!=null){
                try{legacyBridge=new LegacyShopBridge(this,provider.getProvider(),ttl);legacyUi=new LegacyShopUi(this,legacyBridge);legacyBridge.setUi(legacyUi);getServer().getPluginManager().registerEvents(legacyUi,this);}
                catch(Exception|LinkageError failure){getLogger().severe("Legacy ChestShop trading paused: "+failure+". Install the matching guarded ChestShop; do not remove its stock protection.");}
            }
            if(checkout.recoveryRequired())getLogger().severe("Purchases paused: "+checkout.recoveryReason()+". Preserve the journal and reconcile before enabling purchases.");
            getLogger().info("ExcelShops enabled. Legacy ChestShop confirmation: "+(legacyBridge!=null?"guard connected":"unavailable/absent")+". Economy: "+provider.getProvider().getName());
        }catch(Exception|LinkageError e){if(checkout!=null)checkout.pauseExternal("Startup validation failed: "+e);getLogger().severe("ExcelShops entered protection-only mode; purchases/setup unavailable: "+e.getMessage());}
    }
    @Override public void onDisable(){
        if(mobileBridge!=null)mobileBridge.closeAll();if(ui!=null)ui.closeAll();if(legacyUi!=null)legacyUi.closeAll();placedSigns.clear();
        if(checkout==null){closeLegacy(false);closeMigration();return;}
        try{
            // Money and world persistence are separate. Only a fully saved clean shutdown may
            // clear the journal's active-session marker. An abrupt crash remains fail-closed.
            if(durabilityFailure)throw new IllegalStateException("A player save failed earlier in this session");
            if(registry!=null)registry.all().forEach(shop->dirtyWorlds.add(shop.chest().worldId()));
            for(UUID id:dirtyPlayers){Player p=getServer().getPlayer(id);if(p!=null&&p.isOnline())p.saveData();}
            for(UUID id:dirtyWorlds){World w=getServer().getWorld(id);if(w==null)throw new IllegalStateException("A dirty shop world disappeared");w.save();}
            if(registry!=null)registry.save();closeLegacy(true);checkout.close();
        }catch(Exception e){getLogger().severe("Clean shop shutdown could not be verified: "+e.getMessage());closeLegacy(false);try{checkout.closeUnclean();}catch(Exception ignored){}}
        finally{closeMigration();}
    }
    private void closeLegacy(boolean clean){
        if(legacyBridge!=null)try{legacyBridge.close(clean);}catch(Exception failure){durabilityFailure=true;getLogger().severe("Legacy checkout journal could not close safely: "+failure);if(checkout!=null)checkout.pauseExternal("Legacy journal close failed");}
    }
    private void closeMigration(){
        if(migrationSession!=null){try{migrationSession.close();}catch(Exception e){getLogger().severe("Could not release migration lock: "+e.getMessage());}finally{migrationSession=null;}}
    }
    public ShopRegistry getRegistry(){return registry;}
    public CheckoutService getCheckout(){return checkout;}
    public ShopAdapter getAdapter(){return adapter;}
    public SpawnCatalog getCatalog(){return catalog;}
    public MobileCatalog getMobileCatalog(){return mobileCatalog;}
    public ShopUi getUi(){return ui;}
    public BalanceDisplay getBalanceDisplay(){return balanceDisplay;}
    public LegacyShopUi getLegacyUi(){return legacyUi;}
    public LegacyShopBridge getLegacyBridge(){return legacyBridge;}
    public void markLegacyDirty(Player p,UUID world){dirtyPlayers.add(p.getUniqueId());dirtyWorlds.add(world);}
    public void markDirty(Player player,ShopRecord shop){dirtyPlayers.add(player.getUniqueId());if(shop!=null)dirtyWorlds.add(shop.chest().worldId());}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
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
        try{registry.validate(s.get());}catch(RegistryException ex){e.setCancelled(true);e.getPlayer().sendMessage(ChatColor.RED+ex.getMessage());}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void stockClick(InventoryClickEvent e){
        if(protectedInventory(e.getView().getTopInventory())&&(checkout==null||checkout.recoveryRequired())){e.setCancelled(true);return;}
        Optional<ShopRecord> shop=inventoryShop(e.getView().getTopInventory());if(shop.isEmpty()){if(protectedInventory(e.getView().getTopInventory()))e.setCancelled(true);return;}
        if(!shop.get().ownerId().equals(e.getWhoClicked().getUniqueId())){e.setCancelled(true);return;}
        try{registry.validate(shop.get());}catch(RegistryException ex){e.setCancelled(true);}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void stockDrag(InventoryDragEvent e){
        if(protectedInventory(e.getView().getTopInventory())&&(checkout==null||checkout.recoveryRequired())){e.setCancelled(true);return;}
        Optional<ShopRecord> shop=inventoryShop(e.getView().getTopInventory());if(shop.isEmpty()){if(protectedInventory(e.getView().getTopInventory()))e.setCancelled(true);return;}
        if(!shop.get().ownerId().equals(e.getWhoClicked().getUniqueId())){e.setCancelled(true);return;}
        try{registry.validate(shop.get());}catch(RegistryException ex){e.setCancelled(true);}
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
    @EventHandler public void quit(PlayerQuitEvent e){if(legacyUi!=null)legacyUi.disconnected(e.getPlayer().getUniqueId());if(ui!=null)ui.disconnected(e.getPlayer().getUniqueId());if(dirtyPlayers.contains(e.getPlayer().getUniqueId()))try{e.getPlayer().saveData();}catch(RuntimeException ex){durabilityFailure=true;getLogger().severe("Player save failed; checkout journal must remain dirty: "+ex.getMessage());}}
    @EventHandler public void changedWorld(PlayerChangedWorldEvent e){if(legacyUi!=null)legacyUi.cancel(e.getPlayer());if(ui!=null)ui.cancel(e.getPlayer());}
    @EventHandler public void teleported(PlayerTeleportEvent e){if(!e.isCancelled()){if(legacyUi!=null)legacyUi.cancel(e.getPlayer());if(ui!=null)ui.cancel(e.getPlayer());}}
}
