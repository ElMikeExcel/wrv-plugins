package wrv.harness;

import java.util.*;
import java.nio.file.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Deliberately has no compile/runtime reference to Vault or the regular harness. */
public final class ProtectionOnlyHarness extends JavaPlugin {
    private int assertions;
    @Override public void onEnable() { Bukkit.getScheduler().runTaskLater(this, this::run, 20L); }
    private void run() {
        int failed = 0;
        try {
            check(Bukkit.getPluginManager().getPlugin("Vault") == null, "Vault plugin is absent");
            boolean unavailable = false; try { Class.forName("net.milkbowl.vault.economy.Economy"); } catch (ClassNotFoundException expected) { unavailable = true; }
            check(unavailable, "Vault API classes are absent from probe classloader");
            Plugin shops = Bukkit.getPluginManager().getPlugin("WRVShops");
            check(shops != null && shops.isEnabled(), "WRVShops handles missing Economy class and stays loaded for protection");
            World world = Bukkit.getWorlds().getFirst();
            for (int x : new int[]{79, 80, 81}) for (int z : new int[]{-1, 0, 1}) { Chunk chunk = world.getChunkAt(x >> 4, z >> 4); chunk.load(true); chunk.addPluginChunkTicket(this); }
            FakePlayer buyer = new FakePlayer(UUID.fromString("667cf2d7-0000-4000-8000-000000000002"), "NoVaultBuyer", world);
            Chest chest = (Chest) world.getBlockAt(80, 5, 0).getState();
            Block sign = world.getBlockAt(80, 5, 1);
            ItemStack[] before = chest.getBlockInventory().getContents();
            InventoryOpenEvent open = new InventoryOpenEvent(buyer.view(chest.getBlockInventory())); Bukkit.getPluginManager().callEvent(open); check(open.isCancelled(), "foreign physical open blocked without Vault classes");
            for (Block block : List.of(chest.getBlock(), sign)) {
                BlockBreakEvent broken = new BlockBreakEvent(block, buyer.player); Bukkit.getPluginManager().callEvent(broken); check(broken.isCancelled(), "shop block break blocked without Vault classes");
            }
            ItemStack sample = Arrays.stream(before).filter(Objects::nonNull).findFirst().orElseThrow();
            InventoryMoveItemEvent move = new InventoryMoveItemEvent(chest.getBlockInventory(), sample, Bukkit.createInventory(null, 9), true); Bukkit.getPluginManager().callEvent(move); check(move.isCancelled(), "hopper extraction blocked without Vault classes");
            PlayerInteractEvent interact = new PlayerInteractEvent(buyer.player, Action.RIGHT_CLICK_BLOCK, null, chest.getBlock(), BlockFace.SOUTH, EquipmentSlot.HAND); Bukkit.getPluginManager().callEvent(interact); check(interact.isCancelled(), "buyer interaction cannot expose chest without Vault classes");
            check(Bukkit.getPluginCommand("wrvshop").execute(buyer.player, "wrvshop", new String[0]), "shop command remains registered in protection-only mode");
            check(buyer.messages.stream().anyMatch(message -> message.contains("paused")), "shop command gives a truthful paused message");
            check(Arrays.equals(before, chest.getBlockInventory().getContents()), "missing Vault boot leaves physical stock untouched");
            getLogger().info("WRV_SHOPS_HARNESS_NO_VAULT verified");
        } catch (Throwable failure) {
            failed = 1; getLogger().severe("WRV_SHOPS_HARNESS_FAIL provider-plugin-absent: " + failure); failure.printStackTrace();
        }
        getLogger().info("WRV_SHOPS_HARNESS_SUMMARY passed=" + (failed == 0 ? 1 : 0) + " failed=" + failed + " assertions=" + assertions);
        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 10L);
    }
    private void check(boolean condition, String details) { assertions++; if (!condition) throw new AssertionError(details); }
}
