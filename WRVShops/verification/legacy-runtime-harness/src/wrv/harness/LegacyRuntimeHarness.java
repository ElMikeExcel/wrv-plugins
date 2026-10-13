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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import com.earth2me.essentials.Essentials;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.Acrobot.ChestShop.Events.*;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/** Never deploy. Disposable real-plugin integration fixture with synthetic accounts. */
public final class LegacyRuntimeHarness extends JavaPlugin implements Listener {
    private final List<String> results = new ArrayList<>();
    private int assertions, failed, preEvents, transactions;
    private FakePlayer owner, buyer;
    private Plugin shops;
    private Economy economy;
    private World world;
    private Chest chest;
    private Sign sign;
    @Override public void onEnable() { Bukkit.getPluginManager().registerEvents(this,this); Bukkit.getScheduler().runTaskLater(this,this::start,40); }
    @EventHandler(priority=EventPriority.MONITOR) public void pre(PreTransactionEvent e) { preEvents++; getLogger().info("Observed pre-event cancelled="+e.isCancelled()+" price="+e.getExactPrice()); }
    @EventHandler(priority=EventPriority.MONITOR) public void transaction(TransactionEvent e) { transactions++; getLogger().info("Observed transaction cancelled="+e.isCancelled()+" price="+e.getExactPrice()); }
    private void start() {
        try {
            check(Bukkit.getVersion().equals("26.2-123-5001879 (MC: 26.2)"),"exact Paper 26.2 build 123");
            check(System.getProperty("java.runtime.version").equals("25.0.4+7-LTS"),"exact Java runtime");
            for (String name:List.of("Vault","Essentials","GriefPrevention","ChestShop","ExcelShops")) {
                Plugin p=Bukkit.getPluginManager().getPlugin(name); check(p!=null && p.isEnabled(),name+" is enabled: "+(p==null?"absent":p.getDescription().getVersion()));
            }
            check(Bukkit.getPluginManager().getPlugin("ChestShop").getDescription().getVersion().equals("3.13-pre-1 (build 474)"),"exact ChestShop build474");
            economy=Bukkit.getServicesManager().load(Economy.class); check(economy!=null,"real economy service present"); check(economy.getName().equals("EssentialsX Economy"),"actual EssentialsX economy provider: "+economy.getName());
            shops=Bukkit.getPluginManager().getPlugin("ExcelShops");world=Bukkit.getWorlds().getFirst();
            owner=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000001"),"LegacyOwner",world);
            buyer=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000002"),"LegacyBuyer",world);
            installLookup();
            Essentials essentials=(Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
            for(FakePlayer player:List.of(owner,buyer)) {
                NameManager.getOrCreateAccount(player.uuid,player.name);
                var user=essentials.getUser(player.player); check(user!=null,"synthetic Essentials account "+player.name);user.setMoney(BigDecimal.valueOf(10000));
                check(economy.hasAccount(player.player),"real economy recognizes "+player.name);
                check(economy.getBalance(player.player)==10000,"real balance initialized "+player.name);
            }
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunkAt(x,z).load();
            var gp=GriefPrevention.instance;
            check(gp!=null,"actual GP instance exists");
            if(gp.dataStore.getClaimAt(new Location(world,0,5,0),false,null)==null) {
                var claim=gp.dataStore.createClaim(world,-4,8,0,20,-4,8,owner.uuid,null,null,null);
                check(claim.succeeded,"actual GriefPrevention claim created");
            }
            check(gp.dataStore.getClaimAt(new Location(world,0,5,0),false,null).getOwnerID().equals(owner.uuid),"actual GP owner lookup succeeds");
            world.getBlockAt(0,5,0).setType(Material.CHEST,false); chest=(Chest)world.getBlockAt(0,5,0).getState();
            world.getBlockAt(0,5,1).setType(Material.OAK_WALL_SIGN,false);var data=(WallSign)world.getBlockAt(0,5,1).getBlockData();data.setFacing(BlockFace.SOUTH);world.getBlockAt(0,5,1).setBlockData(data,false);
            sign=(Sign)world.getBlockAt(0,5,1).getState();String[] lines={owner.name,"4","B 10:S 5","Diamond"};for(int i=0;i<4;i++)sign.setLine(i,lines[i]);sign.update(true,false);
            chest.getBlockInventory().clear();chest.getBlockInventory().setItem(0,new ItemStack(Material.DIAMOND,20)); buyer.clear();buyer.z=2.5;
            check(com.Acrobot.ChestShop.Signs.ChestShopSign.isValid(sign),"real ChestShop sign recognized");
            check(com.Acrobot.ChestShop.Utils.uBlock.findConnectedContainer(sign)!=null,"real ChestShop container resolution");
            double balance=economy.getBalance(buyer.player);
            var click=new PlayerInteractEvent(buyer.player,Action.RIGHT_CLICK_BLOCK,null,sign.getBlock(),BlockFace.SOUTH,EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(click);
            check(preEvents==1,"real sign click traverses ChestShop PreTransactionEvent");
            if(Boolean.getBoolean("legacy.test.baseline")) {
                check(transactions==1,"baseline bypass reproduced: transaction fires immediately");
                check(economy.getBalance(buyer.player)==balance-10,"baseline immediate purchase charges actual Essentials economy");
                check(buyer.count(new ItemStack(Material.DIAMOND))==4,"baseline exact 4 items delivered");
                check(chest.getBlockInventory().getItem(0).getAmount()==16,"baseline exact real stock decremented");
                check(economy.getBalance(owner.player)==10010,"baseline seller paid actual Essentials economy");
            } else {
                check(transactions==0,"initial click cannot transact before confirmation");
                check(economy.getBalance(buyer.player)==balance,"initial click leaves real balance unchanged");
                check(buyer.count(new ItemStack(Material.DIAMOND))==0,"initial click gives no items");
                check(chest.getBlockInventory().getItem(0).getAmount()==20,"initial click leaves real stock unchanged");
                check(buyer.view!=null,"initial click opens confirmation UI");
            }
        } catch(Throwable error) { failed++;results.add("FAIL "+error);error.printStackTrace(); }
        finish();
    }
    private void installLookup() throws Exception {
        Server real=shops.getServer();
        Server proxy=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class<?>[]{Server.class},(p,m,args)->{
            if(args!=null && args.length==1 && Set.of("getPlayer","getPlayerExact","getOfflinePlayer").contains(m.getName()))for(FakePlayer f:List.of(owner,buyer))if(f.uuid.equals(args[0])||f.name.equals(args[0]))return f.player;
            if(m.getName().equals("getOnlinePlayers"))return List.of(owner.player,buyer.player);
            try{return m.invoke(real,args);}catch(InvocationTargetException e){throw e.getCause();}
        });
        Field server=JavaPlugin.class.getDeclaredField("server");server.setAccessible(true);
        for(String name:List.of("ExcelShops","ChestShop","Essentials"))server.set(Bukkit.getPluginManager().getPlugin(name),proxy);
    }
    private void check(boolean ok,String what){assertions++;if(!ok)throw new AssertionError(what);results.add("PASS "+what);getLogger().info("LEGACY_HARNESS_PASS "+what);}
    private void finish(){try{Files.createDirectories(getDataFolder().toPath());Files.writeString(getDataFolder().toPath().resolve("results.txt"),String.join("\n",results)+"\nSUMMARY assertions="+assertions+" failed="+failed+"\n");}catch(Exception e){failed++;e.printStackTrace();}getLogger().info("LEGACY_HARNESS_SUMMARY assertions="+assertions+" failed="+failed);Bukkit.getScheduler().runTaskLater(this,Bukkit::shutdown,10);}
}
