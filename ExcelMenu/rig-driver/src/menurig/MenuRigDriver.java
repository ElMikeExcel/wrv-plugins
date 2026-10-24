package menurig;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

/**
 * Rig-only driver for the ExcelMenu scratch server. Two
 * Carpet-style fake ServerPlayers — RigJava (plain UUID) and
 * .RigBeck (Floodgate-format UUID) — and a self-driving sequence
 * (armed by a RUN flag file). The 0.1.3 sequence walks the Safe
 * Blocks change: open the shop root, tap the new Safe Blocks
 * category, verify the shelf's rendered icons and bundle amounts,
 * tap both products (an excelshop probe command records the exact
 * browse dispatches), take Back to the root, open the shelf for
 * the Bedrock-UUID player through the GUI door, regression-open
 * the Food category, and reload. Never ships.
 */
public class MenuRigDriver extends JavaPlugin {

    static final UUID JAVA_UUID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    static final UUID BECK_UUID = UUID.fromString("00000000-0000-0000-0009-01f0000000c1");

    @Override
    public void onEnable() {
        getLogger().info("MenuRigDriver armed (Excel)");
        File flag = new File(getDataFolder(), "RUN");
        if (flag.exists()) {
            getServer().getScheduler().runTaskLater(this, this::sequence, 100L);
        }
    }

    // ---------- probe command: stands in for ExcelCosmetics ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("excelcosmetics")) {
            getLogger().info("RIGPROBE excelcosmetics " + String.join(" ", args)
                    + " (sender=" + sender.getName() + ")");
            return true;
        }
        if (command.getName().equalsIgnoreCase("excelshop")) {
            getLogger().info("RIGPROBE excelshop " + String.join(" ", args)
                    + " (sender=" + sender.getName() + ")");
            return true;
        }
        return false;
    }

    // ---------- the scripted walk ----------

    private int step = 0;

    private void sequence() {
        step++;
        try {
            switch (step) {
                case 1 -> {
                    spawnFakes();
                    console("excelmenu list");
                    later(20);
                }
                case 2 -> {
                    console("excelmenu open spawnshop RigJava");
                    later(10);
                }
                case 3 -> {
                    dumpInv(java(), "shop root (expect [5]=LIGHT Safe Blocks, [6]=PINK_DYE Cosmetics, [8]=ARROW Back)");
                    click(java(), 5, ClickType.LEFT); // Safe Blocks category
                    later(10);
                }
                case 4 -> {
                    dumpInv(java(), "safe blocks shelf (expect [0]=LIGHT x4, [1]=PETRIFIED_OAK_SLAB x64, [2]=GOLD_NUGGET, [3]=ARROW)");
                    click(java(), 0, ClickType.LEFT); // light block -> excelshop browse light_block
                    later(10);
                }
                case 5 -> {
                    click(java(), 1, ClickType.RIGHT); // slab -> excelshop browse petrified_slab
                    later(10);
                }
                case 6 -> {
                    click(java(), 3, ClickType.LEFT); // Back -> spawnshop root
                    later(10);
                }
                case 7 -> {
                    dumpInv(java(), "after shelf back (expect Spawn Shop root again)");
                    java().closeInventory();
                    console("excelmenu open spawnshop_safe_blocks .RigBeck");
                    later(10);
                }
                case 8 -> {
                    dumpInv(beck(), "Beck safe blocks via GUI door (the chest a live Bedrock player gets for this menu)");
                    beck().closeInventory();
                    console("excelmenu open spawnshop_food RigJava");
                    later(10);
                }
                case 9 -> {
                    dumpInv(java(), "food category regression (expect BREAD/STEAK/GOLDEN_CARROT/GOLDEN_APPLE + balance + back)");
                    console("excelmenu reload");
                    later(20);
                }
                case 10 -> {
                    getLogger().info("RIG SEQUENCE COMPLETE");
                    Bukkit.shutdown();
                }
                default -> { /* done */ }
            }
        } catch (Throwable t) {
            getLogger().warning("RIGSTEP " + step + " FAILED: " + t);
            t.printStackTrace();
            Bukkit.shutdown();
        }
    }

    private void later(long ticks) {
        getServer().getScheduler().runTaskLater(this, this::sequence, ticks);
    }

    private void console(String cmd) {
        getLogger().info("RIGCMD console: " + cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private Player java() {
        return Bukkit.getPlayerExact("RigJava");
    }

    private Player beck() {
        return Bukkit.getPlayerExact(".RigBeck");
    }

    private void dumpInv(Player p, String label) {
        if (p == null) {
            getLogger().info("RIGINV[" + label + "] player missing");
            return;
        }
        InventoryView view = p.getOpenInventory();
        String title = PlainTextComponentSerializer.plainText().serialize(view.title());
        StringBuilder sb = new StringBuilder("RIGINV[" + label + "] title=" + title
                + " topSize=" + view.getTopInventory().getSize() + " items=");
        for (int i = 0; i < view.getTopInventory().getSize(); i++) {
            ItemStack item = view.getTopInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            sb.append(" [").append(i).append("]=").append(item.getType());
            if (item.getAmount() != 1) sb.append("x").append(item.getAmount());
            if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                sb.append("(").append(PlainTextComponentSerializer.plainText()
                        .serialize(item.getItemMeta().displayName())).append(")");
            }
        }
        getLogger().info(sb.toString());
    }

    private void click(Player p, int slot, ClickType type) {
        InventoryView view = p.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(view,
                slot < view.getTopInventory().getSize() ? InventoryType.SlotType.CONTAINER
                        : InventoryType.SlotType.QUICKBAR,
                slot, type, InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(event);
        getLogger().info("RIGCLICK[" + p.getName() + "] slot=" + slot + " type=" + type
                + " cancelled=" + event.isCancelled());
    }

    // ---------- fake players (Carpet-style, per the fleet rigs) ----------

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
                net.kyori.adventure.text.Component.text(name + " joined the game")));
        return player;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
