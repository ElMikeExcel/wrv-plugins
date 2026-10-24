package cprig;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import me.ryanhamshire.GriefPrevention.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Method;
import java.util.*;

public class ChildproofRig extends JavaPlugin {
    static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID KID = UUID.fromString("00000000-0000-0000-0009-000000000001");
    int step = 0, pass = 0, fail = 0; Player j, r; Claim claim, claim2; Location base;

    @Override public void onEnable() { getServer().getScheduler().runTaskLater(this, this::seq, 60L); }
    void later(int t) { getServer().getScheduler().runTaskLater(this, this::seq, t); }
    void check(boolean c, String m) { if (c) pass++; else fail++; getLogger().info((c ? "PASS " : "FAIL ") + m); }
    void as(Player p, String cmd) { boolean ok = p.performCommand(cmd); getLogger().info("RUN as " + p.getName() + ": /" + cmd + " -> dispatched=" + ok); }
    String lvl(Claim c, Player p) {
        if (c.checkPermission(p, ClaimPermission.Build, null) == null) return "build";
        if (c.checkPermission(p, ClaimPermission.Container, null) == null) return "container";
        if (c.checkPermission(p, ClaimPermission.Access, null) == null) return "access";
        return "none";
    }
    void dump(String tag) {
        for (Claim c : new Claim[]{claim, claim2}) {
            ArrayList<String> b = new ArrayList<>(), ct = new ArrayList<>(), a = new ArrayList<>(), m = new ArrayList<>();
            c.getPermissions(b, ct, a, m);
            getLogger().info("TRUST[" + tag + "] claim " + c.getID() + " build=" + b + " containers=" + ct + " access=" + a + " managers=" + m + " -> rylie=" + lvl(c, r));
        }
    }
    boolean breakCancelled(Block b) { BlockBreakEvent e = new BlockBreakEvent(b, r); Bukkit.getPluginManager().callEvent(e); return e.isCancelled(); }
    boolean useCancelled(Block b) {
        PlayerInteractEvent e = new PlayerInteractEvent(r, Action.RIGHT_CLICK_BLOCK, null, b, BlockFace.UP, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(e); return e.useInteractedBlock() == org.bukkit.event.Event.Result.DENY;
    }
    Boolean ebmInClaim(Player p) throws Exception {
        Class<?> k = Class.forName("com.winkreyes.excelbedrockmenu.ChildproofMenu", true, Bukkit.getPluginManager().getPlugin("ExcelBedrockMenu").getClass().getClassLoader());
        Method m = k.getDeclaredMethod("inClaim", Player.class); m.setAccessible(true); return (Boolean) m.invoke(null, p);
    }

    String ebmVerify(Player p, String name) throws Exception {
        Class<?> k = Class.forName("com.winkreyes.excelbedrockmenu.ChildproofMenu", true, Bukkit.getPluginManager().getPlugin("ExcelBedrockMenu").getClass().getClassLoader());
        Method m = k.getDeclaredMethod("verify", Player.class, String.class); m.setAccessible(true); return (String) m.invoke(null, p, name);
    }
    void seq() {
        step++;
        try {
            World w = Bukkit.getWorlds().get(0);
            switch (step) {
                case 1 -> {
                    check(Bukkit.getCommandMap().getCommand("childproof") != null, "commands.yml alias /childproof registered");
                    check(Bukkit.getCommandMap().getCommand("unchildproof") != null, "commands.yml alias /unchildproof registered");
                    check(Bukkit.getCommandMap().getCommand("childproofmenu") != null, "EBM 1.5.16 /childproofmenu registered");
                    j = makePlayer(OWNER, "OwnerPlayer"); r = makePlayer(KID, ".KidPlayer");
                    check(j.hasPermission("griefprevention.claims") && r.hasPermission("griefprevention.claims") && !j.isOp(), "non-op default players hold griefprevention.claims (plugin.yml default true)");
                    base = new Location(w, 200, w.getHighestBlockYAt(200, 200) + 1, 200);
                    DataStore ds = GriefPrevention.instance.dataStore;
                    claim = ds.createClaim(w, 190, 210, -64, -64, 190, 210, OWNER, null, null, j).claim;
                    claim2 = ds.createClaim(w, 290, 310, -64, -64, 290, 310, OWNER, null, null, j).claim;
                    check(claim != null && claim2 != null, "two claims created for OwnerPlayer (house + farm)");
                    w.getBlockAt(201, base.getBlockY(), 200).setType(Material.ENCHANTING_TABLE);
                    w.getBlockAt(202, base.getBlockY(), 200).setType(Material.OAK_DOOR);
                    w.getBlockAt(203, base.getBlockY(), 200).setType(Material.CHEST);
                    w.getBlockAt(204, base.getBlockY(), 200).setType(Material.STONE_BUTTON);
                    j.teleport(base); r.teleport(base.clone().add(1, 0, 1));
                    later(10);
                }
                case 2 -> {
                    as(j, "trust .KidPlayer"); // outside nothing: standing in house claim
                    as(j, "trust .KidPlayer");
                    j.teleport(new Location(w, 300, base.getY(), 300)); as(j, "trust .KidPlayer"); j.teleport(base);
                    later(5);
                }
                case 3 -> {
                    dump("before (full trust, today's situation)");
                    check(lvl(claim, r).equals("build"), "BEFORE: Kid has full (build) trust in the house claim");
                    Block t = w.getBlockAt(201, base.getBlockY(), 200);
                    check(!breakCancelled(t), "BEFORE: Kid CAN break the enchanting table (the incident)");
                    as(j, "childproof .KidPlayer");
                    later(5);
                }
                case 4 -> {
                    dump("after /childproof in house");
                    int y = base.getBlockY();
                    check(lvl(claim, r).equals("access"), "AFTER: house claim = access only");
                    check(breakCancelled(w.getBlockAt(201, y, 200)), "AFTER: breaking the enchanting table is BLOCKED");
                    check(useCancelled(w.getBlockAt(203, y, 200)), "AFTER: opening the chest is BLOCKED");
                    check(!useCancelled(w.getBlockAt(202, y, 200)), "AFTER: opening the door is ALLOWED");
                    check(!useCancelled(w.getBlockAt(204, y, 200)), "AFTER: pressing the button is ALLOWED");
                    check(lvl(claim2, r).equals("build"), "AFTER: other claim untouched (still build) - only the claim stood in changed");
                    check(Boolean.TRUE.equals(ebmInClaim(j)), "EBM picker sees Owner inside a claim");
                    check("access".equals(ebmVerify(j, ".KidPlayer")), "EBM confirmation check reads 'access' -> prints Done line");
                    as(j, "childproofmenu"); as(j, "childproofmenu help");
                    as(j, "unchildproof .KidPlayer");
                    later(5);
                }
                case 5 -> {
                    dump("after /unchildproof in house");
                    check(lvl(claim, r).equals("none"), "/unchildproof: house claim = no trust");
                    check(lvl(claim2, r).equals("build"), "/unchildproof: other claim untouched");
                    check("none".equals(ebmVerify(j, ".KidPlayer")), "EBM confirmation check reads 'none' after /unchildproof");
                    as(j, "childproof .KidPlayer");
                    as(j, "childproof .KidPlayer");
                    later(5);
                }
                case 6 -> {
                    dump("after /childproof x2 (repeat is harmless)");
                    check(lvl(claim, r).equals("access"), "repeat /childproof still access only");
                    as(j, "childproof"); // missing name
                    later(5);
                }
                case 7 -> {
                    dump("after /childproof with no name");
                    check(lvl(claim, r).equals("access"), "/childproof with no name changes nothing");
                    as(r, "childproof OwnerPlayer"); // Kid has no claim rights there
                    later(5);
                }
                case 8 -> {
                    dump("after Kid tries /childproof on Owner inside Owner's claim");
                    check(lvl(claim, r).equals("access") && claim.getOwnerID().equals(OWNER), "a non-manager can't change trust in someone else's claim");
                    Location out = new Location(w, 0, base.getY(), 0);
                    j.teleport(out);
                    check(Boolean.FALSE.equals(ebmInClaim(j)), "EBM picker sees Owner OUTSIDE any claim (refuses to open)");
                    check("outside".equals(ebmVerify(j, ".KidPlayer")), "EBM confirmation warns 'changed ALL your claims' when typed outside");
                    as(j, "childproof .KidPlayer");
                    later(5);
                }
                case 9 -> {
                    dump("after typed /childproof while standing OUTSIDE claims");
                    check(lvl(claim, r).equals("access") && lvl(claim2, r).equals("access"), "typed outside a claim: GP applies it to ALL Owner's claims (documented)");
                    getLogger().info("RIG DONE pass=" + pass + " fail=" + fail);
                    Bukkit.shutdown();
                }
                default -> { }
            }
        } catch (Throwable t) { getLogger().severe("RIG ERROR step " + step + ": " + t); t.printStackTrace(); fail++; getLogger().info("RIG DONE pass=" + pass + " fail=" + fail); Bukkit.shutdown(); }
    }

    Player makePlayer(UUID uuid, String name) {
        MinecraftServer server = ((org.bukkit.craftbukkit.CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        GameProfile profile = new GameProfile(uuid, name);
        ServerPlayer sp = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public boolean isConnected() { return true; }
            @Override public boolean isConnecting() { return false; }
        };
        connection.channel = new EmbeddedChannel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        sp.connection = new ServerGamePacketListenerImpl(server, connection, sp, cookie);
        server.getPlayerList().placeNewPlayer(connection, sp, cookie);
        Player p = sp.getBukkitEntity();
        p.setGameMode(GameMode.SURVIVAL);
        Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(p, net.kyori.adventure.text.Component.text(name + " joined")));
        return p;
    }
}
