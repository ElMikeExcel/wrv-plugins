package cosmeticrig;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Rig-only driver for the ExcelCosmetics scratch server. Two
 * Carpet-style fake ServerPlayers — RigJava (plain UUID) and
 * .RigBeck (Floodgate-prefix UUID) — so both platform doors can be
 * exercised headlessly. Chat is captured by rendering each
 * AsyncChatEvent through its own (post-plugin) renderer and
 * serializing to legacy codes: RIGRENDER lines are the proof.
 * Never ships.
 */
public class CosmeticRigDriver extends JavaPlugin implements Listener {

    static final UUID JAVA_UUID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    static final UUID BECK_UUID = UUID.fromString("00000000-0000-0000-0009-01f0000000c1");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CosmeticRigDriver armed (Excel)");
    }

    // ---------- chat capture ----------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        Player source = event.getPlayer();
        Player viewer = Bukkit.getPlayer(source.getUniqueId().equals(JAVA_UUID) ? BECK_UUID : JAVA_UUID);
        if (viewer == null) viewer = source;
        try {
            Component rendered = event.renderer().render(source, source.displayName(), event.message(), viewer);
            String legacy = LegacyComponentSerializer.legacySection().serialize(rendered);
            getLogger().info("RIGRENDER[" + source.getName() + "] " + legacy);
        } catch (Throwable t) {
            getLogger().warning("RIGRENDER-FAILED " + t);
        }
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        try {
            switch (args[0].toLowerCase()) {
                case "spawn" -> { spawnFakes(); }
                case "chat" -> {
                    Player p = need(args[1]);
                    String msg = joinFrom(args, 2);
                    p.chat(msg);
                }
                case "cmd" -> {
                    Player p = need(args[1]);
                    p.performCommand(joinFrom(args, 2));
                }
                case "inv" -> dumpInventory(need(args[1]));
                case "click" -> click(need(args[1]), Integer.parseInt(args[2]));
                case "bal" -> {
                    Player p = need(args[1]);
                    RegisteredServiceProvider<Economy> rsp =
                            getServer().getServicesManager().getRegistration(Economy.class);
                    double bal = rsp == null ? -1 : rsp.getProvider().getBalance(p);
                    getLogger().info("RIGBAL[" + p.getName() + "] " + bal);
                }
                case "quit" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) p.kickPlayer("rig done");
                }
                default -> sender.sendMessage("unknown rig subcommand");
            }
        } catch (Throwable t) {
            getLogger().warning("RIGCMD-FAILED " + args[0] + ": " + t);
            t.printStackTrace();
        }
        return true;
    }

    private Player need(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) throw new IllegalStateException("no fake player " + name);
        return p;
    }

    private String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void dumpInventory(Player p) {
        InventoryView view = p.getOpenInventory();
        String title = PlainTextComponentSerializer.plainText().serialize(view.title());
        getLogger().info("RIGINV[" + p.getName() + "] title=" + title + " topSize=" + view.getTopInventory().getSize());
        for (int i = 0; i < view.getTopInventory().getSize(); i++) {
            ItemStack item = view.getTopInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            StringBuilder sb = new StringBuilder("RIGINV slot " + i + ": " + item.getType());
            if (item.hasItemMeta() && item.getItemMeta() != null) {
                if (item.getItemMeta().hasDisplayName()) {
                    sb.append(" name=").append(PlainTextComponentSerializer.plainText()
                            .serialize(item.getItemMeta().displayName()));
                }
                if (item.getItemMeta().hasLore() && item.getItemMeta().lore() != null) {
                    for (Component line : item.getItemMeta().lore()) {
                        sb.append(" | ").append(PlainTextComponentSerializer.plainText().serialize(line));
                    }
                }
            }
            getLogger().info(sb.toString());
        }
    }

    private void click(Player p, int slot) {
        InventoryView view = p.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(view,
                slot < view.getTopInventory().getSize() ? InventoryType.SlotType.CONTAINER
                        : InventoryType.SlotType.QUICKBAR,
                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(event);
        getLogger().info("RIGCLICK[" + p.getName() + "] slot=" + slot + " cancelled=" + event.isCancelled());
    }

    // ---------- fake players (Carpet-style, per the TW rig) ----------

    private void spawnFakes() {
        if (Bukkit.getPlayerExact("RigJava") == null) makePlayer(JAVA_UUID, "RigJava");
        if (Bukkit.getPlayerExact(".RigBeck") == null) makePlayer(BECK_UUID, ".RigBeck");
        getLogger().info("RIGSPAWN done");
    }

    private Player makePlayer(UUID uuid, String name) {
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
        Player player = sp.getBukkitEntity();
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation().add(2, 2, 2);
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        server.getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, sp));
        Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(player,
                Component.text(name + " joined the game")));
        return player;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
