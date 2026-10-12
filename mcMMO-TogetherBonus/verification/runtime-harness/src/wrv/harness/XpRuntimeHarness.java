package wrv.harness;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerPreXpGainEvent;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import com.gmail.nossr50.util.experience.XpBonusIndicator;
import com.gmail.nossr50.util.player.UserManager;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Test only: synthetic players, native Bukkit bossbars/events, no client connections or production data. */
public final class XpRuntimeHarness extends JavaPlugin implements Listener {
    private final List<String> results = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private mcMMO plugin;
    private XpBonusIndicator indicator;
    private TestPlayer player;
    private McMMOPlayer mmo;
    private McMMOPlayer teammate;
    private Object firstBar;
    private boolean cancelPre;
    private boolean zeroGain;
    private boolean lateZero;
    private int assertions;

    @Override public void onEnable() {
        if (!Boolean.getBoolean("wrv.xp.harness") || Bukkit.getPort() != 25581
                || !"127.0.0.1".equals(Bukkit.getIp())) {
            throw new IllegalStateException("Disposable loopback XP harness only");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            int phase;
            @Override public void run() {
                try {
                    step(phase++);
                    if (phase > 140) { cancel(); finish(null); }
                } catch (Throwable failure) { cancel(); finish(failure); }
            }
        }.runTaskTimer(this, 40L, 1L);
    }

    private void step(int phase) throws Exception {
        switch (phase) {
            case 0 -> {
                check(Bukkit.getVersion().contains("26.2") && Bukkit.getBukkitVersion().equals("26.2.build.123-stable"), "exact Paper API");
                check(System.getProperty("java.version").equals("25.0.4"), "Java 25.0.4");
                plugin = (mcMMO) Bukkit.getPluginManager().getPlugin("mcMMO");
                check(plugin != null && plugin.isEnabled() && plugin.getDescription().getVersion().equals("2.3.003-WRV-XP-2"), "XP-2 enabled");
                var weekend = Bukkit.getPluginManager().getPlugin("WeekendXP");
                check(weekend != null && weekend.isEnabled() && weekend.getDescription().getVersion().equals("1.0.2"), "WeekendXP 1.0.2 enabled");
                check(plugin.isVanillaXpBonusSupported(), "Paper vanilla bridge supported");
                check(plugin.getVanillaXpEventMultiplier() == 1.0D, "Monday WeekendXP reset applied");
                indicator = plugin.getXpBonusIndicator();
                check(indicator != null && field(indicator,"refreshTask") != null, "indicator timer running");
                check(plugin.getGeneralConfig().getXpBonusIndicatorDisplayTicks() == 60L, "default 60 ticks");
                player = person("XpHarnessA",0);
                TestPlayer peer = person("XpHarnessB",4);
                mmo = new McMMOPlayer(player,new PlayerProfile(player.getName(),player.getUniqueId(),true,0));
                teammate = new McMMOPlayer(peer,new PlayerProfile(peer.getName(),peer.getUniqueId(),true,0));
                UserManager.track(mmo); UserManager.track(teammate);
                Party party = new Party("XpHarnessParty"); party.getOnlineMembers().add(player); party.getOnlineMembers().add(peer);
                mmo.setParty(party); teammate.setParty(party);
                check(bars().isEmpty(), "join/proximity does not show bar");
                Bukkit.dispatchCommand(player,"xpbonus show"); Bukkit.dispatchCommand(player,"xpbonus status");
                check(bars().isEmpty(), "show/status do not show bar");
                gain(0F); check(bars().isEmpty(), "zero skill does not show");
                cancelPre=true; gain(10F); cancelPre=false; check(bars().isEmpty(),"cancelled pre-event does not show");
                zeroGain=true; gain(10F); zeroGain=false; check(bars().isEmpty(),"zero final award does not show");
                gain(.25F); check(bars().size()==1,"positive fractional stored skill shows");
                check(mmo.getProfile().getSkillXpLevelRaw(PrimarySkillType.MINING)==.25F,"fraction stored unchanged");
                firstBar=bar(); check(((BossBar)firstBar).getPlayers().size()==1,"native CraftBossBar holds player");
                check(firstBar.getClass().getName().contains("CraftBossBar"),"native Paper bossbar implementation");
            }
            case 10 -> { gain(.25F); check(bar()==firstBar,"repeat gain reuses bar"); }
            case 69 -> check(bars().size()==1,"bar remains 59 ticks after repeat");
            case 70 -> check(bars().isEmpty(),"bar expires at 60 ticks after repeat");
            case 71 -> {
                plugin.setVanillaXpEventMultiplier(1.2D);
                ExperienceConfig.getInstance().setExperienceGainsGlobalMultiplier(1.2D);
                var method=McMMOPlayer.class.getDeclaredMethod("modifyXpGain",PrimarySkillType.class,float.class); method.setAccessible(true);
                float expected=(float)method.invoke(mmo,PrimarySkillType.MINING,10F)*1.25F;
                float before=mmo.getProfile().getSkillXpLevelRaw(PrimarySkillType.MINING);
                mmo.beginXpGain(PrimarySkillType.MINING,10F,XPGainReason.PVE,XPGainSource.SELF);
                float delta=mmo.getProfile().getSkillXpLevelRaw(PrimarySkillType.MINING)-before;
                check(Math.abs(delta-expected)<.0001F,"Together skill factor composes with weekend unchanged");
                indicator.setVisible(player,false); indicator.setVisible(player,true);
                var event=orb(1,ExperienceOrb.SpawnReason.FURNACE);
                check(event.getAmount()==1,"first combined 1 XP pickup retains half fraction");
                check(bars().isEmpty(),"orb notification waits for event completion");
            }
            case 72 -> { check(bars().size()==1,"positive vanilla orb creates native bar next tick"); firstBar=bar(); }
            case 73 -> check(orb(1,ExperienceOrb.SpawnReason.FURNACE).getAmount()==2,"second combined 1 XP pickup awards two points");
            case 74 -> check(bar()==firstBar,"vanilla repeat reuses native bar");
            case 133 -> check(bars().size()==1,"vanilla repeat keeps bar for 59 ticks");
            case 134 -> check(bars().isEmpty(),"vanilla bar expires after latest pickup");
            case 135 -> {
                indicator.setVisible(player,false); gain(2F); orb(2,ExperienceOrb.SpawnReason.FURNACE);
                check(bars().isEmpty(),"hidden preference suppresses skill and vanilla");
                check(!new XpBonusIndicator(plugin).isVisible(player),"hidden preference read by a fresh manager");
                indicator.setVisible(player,true); check(bars().isEmpty(),"show waits for new gain");
                lateZero=true; orb(5,ExperienceOrb.SpawnReason.FURNACE); lateZero=false;
                orb(0,ExperienceOrb.SpawnReason.FURNACE); orb(5,ExperienceOrb.SpawnReason.PLAYER_DEATH);
            }
            case 136 -> {
                check(bars().isEmpty(),"late-zero, zero post-Mending amount and excluded death sources do not show");
                gain(1F); check(bars().size()==1,"gain after re-enable shows");
                indicator.onWorldChange(new PlayerChangedWorldEvent(player,player.getWorld()));
                check(bars().isEmpty(),"world change clears bar");
                orb(5,ExperienceOrb.SpawnReason.FURNACE);
                indicator.onQuit(new PlayerQuitEvent(player,""));
            }
            case 137 -> {
                check(bars().isEmpty(),"quit clears pending pickup");
                gain(1F); check(bars().size()==1,"subsequent gain available");
                indicator.shutdown();
                check(bars().isEmpty() && field(indicator,"refreshTask")==null,"shutdown clears bar and timer");
                check(((Map<?,?>)field(indicator,"pendingPickups")).isEmpty(),"shutdown clears pending events");
            }
            default -> { }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST) public void pre(McMMOPlayerPreXpGainEvent event) {
        if (event.getPlayer()==player && cancelPre) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void gainEvent(McMMOPlayerXpGainEvent event) {
        if (event.getPlayer()==player && zeroGain) event.setRawXpGained(0F);
    }
    @EventHandler(priority=EventPriority.MONITOR) public void lateOrb(PlayerExpChangeEvent event) {
        if (event.getPlayer()==player && lateZero) event.setAmount(0);
    }
    private void gain(float amount) { mmo.applyXpGain(PrimarySkillType.MINING,amount,XPGainReason.PVE,XPGainSource.SELF); }
    private PlayerExpChangeEvent orb(int amount,ExperienceOrb.SpawnReason reason) {
        var level=((CraftWorld)player.getWorld()).getHandle();
        var nativeOrb=new net.minecraft.world.entity.ExperienceOrb(level,0,65,0,amount,reason,null,null);
        var event=new PlayerExpChangeEvent(player,nativeOrb.getBukkitEntity(),amount);
        Bukkit.getPluginManager().callEvent(event); return event;
    }
    private Map<?,?> bars() throws Exception { return (Map<?,?>)field(indicator,"bars"); }
    private Object bar() throws Exception {
        Object entry=bars().get(player.getUniqueId()); if(entry==null) return null;
        Method method=entry.getClass().getDeclaredMethod("bar"); method.setAccessible(true); return method.invoke(entry);
    }
    private Object field(Object object,String name) throws Exception {
        Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    private void check(boolean value,String label) {
        assertions++;if(!value)throw new AssertionError(label); results.add("PASS "+label);getLogger().info("XP_HARNESS_PASS "+label);
    }
    private TestPlayer person(String name,double x) {
        var server=((CraftServer)Bukkit.getServer()).getServer(); var level=((CraftWorld)Bukkit.getWorlds().getFirst()).getHandle();
        var profile=new GameProfile(UUID.randomUUID(),name);
        var entity=new ServerPlayer(server,level,profile,ClientInformation.createDefault()); entity.setPos(x,65,0);
        var connection=new Connection(PacketFlow.SERVERBOUND); connection.channel=new EmbeddedChannel(); connections.add(connection);
        entity.connection=new ServerGamePacketListenerImpl(server,connection,entity,CommonListenerCookie.createInitial(profile,false)) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener) { }
        };
        return new TestPlayer((CraftServer)Bukkit.getServer(),entity);
    }
    private static final class TestPlayer extends CraftPlayer {
        TestPlayer(CraftServer server,ServerPlayer entity) { super(server,entity); }
        @Override public boolean isOnline() { return true; }
        @Override public boolean isValid() { return true; }
        @Override public boolean hasPermission(String permission) { return permission.startsWith("mcmmo.skills.") || permission.equals("mcmmo.commands.xpbonus") || permission.equals("mcmmo.perks.partybonus"); }
        @Override public boolean hasPermission(org.bukkit.permissions.Permission permission) { return hasPermission(permission.getName()); }
        @Override public boolean canSee(org.bukkit.entity.Player player) { return true; }
        @Override public void sendMessage(String message) { }
        @Override public void sendMessage(String... messages) { }
    }
    private void finish(Throwable failure) {
        if(failure!=null) { getLogger().severe("XP_HARNESS_FAIL "+failure);failure.printStackTrace();results.add("FAIL "+failure); }
        String summary="XP_HARNESS_SUMMARY assertions="+assertions+" failed="+(failure==null?0:1);
        getLogger().info(summary);results.add(summary);
        try { getDataFolder().mkdirs();Files.write(getDataFolder().toPath().resolve("results.txt"),results); }catch(Exception e){getLogger().severe(e.toString());}
        if(mmo!=null)UserManager.cleanupPlayer(mmo);if(teammate!=null)UserManager.cleanupPlayer(teammate);
        for(var connection:connections)connection.channel.close();
        Bukkit.shutdown();
    }
}
