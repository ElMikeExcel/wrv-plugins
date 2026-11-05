package rig;

import com.mojang.authlib.GameProfile;
import com.winkreyes.excellandmarks.ExcelLandmarks;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.menu.Menus;
import com.winkreyes.excellandmarks.store.Store;
import io.netty.channel.embedded.EmbeddedChannel;
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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rig-only driver for the ExcelLandmarks 0.1.0 proof. NOT shipped.
 *
 * Boot A (Vault + EssentialsX + GriefPrevention + ExcelProtect present):
 * a fake admin registers a PLOT by writing a real [Landmark] sign
 * (SignChangeEvent), a fake renter rents it through the actual sign tap
 * -> menu -> confirm path (real PlayerInteractEvent + InventoryClickEvents,
 * real Vault charge), the GP subdivision must exist and be his, a second
 * player gets a view-only menu, an unregistered sign opens nothing, then
 * the renter builds (real BlockPlaceEvents for ExcelProtect), drops loose
 * items, and the rental is aged through grace into lapse via the console
 * admin commands — the finalize must pack container contents + loose
 * items + the attributed materials into a redemption box, clear exactly
 * the attributed blocks, delete the subdivision, and free the spot.
 *
 * Boot B (GriefPrevention + ExcelProtect ABSENT): the fail-soft proof —
 * register + rent a LANDMARK and a PLOT naming-only (no claim ids), age
 * the plot past grace, finalize runs clean with an empty tally.
 */
public final class LmRig extends JavaPlugin {

    private ExcelLandmarks lm;
    private Economy econ;
    private World world;
    private Player admin, renter, guest;
    private String phase = "A";
    private int SY = 64;   // Boot A sign Y (surface-relative, set in setup)
    private int SY2 = 64;  // Boot B second sign Y
    private long claimId = -1;

    void later(long ticks, Runnable r) {
        new BukkitRunnable() { @Override public void run() { r.run(); } }.runTaskLater(this, ticks);
    }

    void result(String name, boolean ok, String detail) {
        getLogger().info("RIGRESULT " + name + " " + (ok ? "PASS" : "FAIL") + " " + detail);
    }

    void note(String detail) { getLogger().info("RIGRESULT NOTE " + detail); }

    @Override
    public void onEnable() {
        try {
            File f = new File(getDataFolder(), "phase.txt");
            if (f.exists()) phase = Files.readString(f.toPath()).trim();
        } catch (Exception ignored) { }
        later(60, () -> {
            try { setup(); }
            catch (Throwable t) { note("SETUP THREW " + t); t.printStackTrace(); }
        });
    }

    // ------------------------------------------------------------ fake players

    Player fakePlayer(World w, String name) {
        MinecraftServer server = ((org.bukkit.craftbukkit.CraftServer) getServer()).getServer();
        ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) w).getHandle();
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
        ServerPlayer sp = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(conn);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        sp.connection = new ServerGamePacketListenerImpl(server, conn, sp, cookie);
        server.getPlayerList().placeNewPlayer(conn, sp, cookie);
        return sp.getBukkitEntity();
    }

    // ------------------------------------------------------------ helpers

    long balanceCents(Player p) { return Math.round(econ.getBalance(p) * 100.0); }

    void tapSign(Player p, Block b) {
        PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK, null, b,
                BlockFace.NORTH, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(ev);
    }

    void clickSlot(Player p, int slot) {
        InventoryClickEvent ev = new InventoryClickEvent(p.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.NOTHING);
        Bukkit.getPluginManager().callEvent(ev);
    }

    Integer slotWith(Player p, Material mat) {
        Inventory top = p.getOpenInventory().getTopInventory();
        for (int i = 0; i < top.getSize(); i++) {
            ItemStack it = top.getItem(i);
            if (it != null && it.getType() == mat) return i;
        }
        return null;
    }

    boolean topHas(Player p, Material mat) { return slotWith(p, mat) != null; }

    boolean lmOpen(Player p) {
        return p.getOpenInventory().getTopInventory().getHolder() instanceof Menus.LmHolder;
    }

    String signLine(Block b, int idx) {
        if (b.getState() instanceof Sign s) return s.getSide(Side.FRONT).getLine(idx);
        return "<not a sign>";
    }

    Spot spot(String id) { return lm.registry().byId(id); }

    void placeBlock(Player p, int x, int y, int z, Material mat) {
        Block b = world.getBlockAt(x, y, z);
        org.bukkit.block.BlockState replaced = b.getState();
        b.setType(mat);
        BlockPlaceEvent ev = new BlockPlaceEvent(b, replaced, b.getRelative(BlockFace.DOWN),
                new ItemStack(mat), p, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(ev);
    }

    void writeSign(Player p, int x, int y, int z, String[] lines) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(Material.OAK_SIGN);
        SignChangeEvent ev = new SignChangeEvent(b, p, lines.clone(), Side.FRONT);
        Bukkit.getPluginManager().callEvent(ev);
    }

    Object gpCall(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> c = Class.forName("rig.GpRig");
            Method m = c.getMethod(method, types);
            return m.invoke(null, args);
        } catch (Throwable t) {
            note("gpCall " + method + " threw " + t);
            return null;
        }
    }

    void setup() {
        lm = (ExcelLandmarks) Bukkit.getPluginManager().getPlugin("ExcelLandmarks");
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        econ = rsp == null ? null : rsp.getProvider();
        world = Bukkit.getWorlds().get(0);
        SY = world.getHighestBlockYAt(100, 100) + 1;
        SY2 = world.getHighestBlockYAt(108, 100) + 1;
        result("SETUP", lm != null && econ != null,
                "plugin=" + (lm != null) + " economy=" + (econ == null ? "none" : econ.getName()));
        admin = fakePlayer(world, "RigAdmin");
        admin.setOp(true);
        renter = fakePlayer(world, "RigRenter");
        guest = fakePlayer(world, "RigGuest");
        if (phase.equals("B")) later(20, this::bootB1);
        else later(20, this::bootA1);
    }

    // ================================================================ Boot A

    void bootA1() {
        boolean gpOn = lm.gp().available();
        result("A-GP-ON", gpOn, "gp adapter available=" + gpOn);
        result("A-PROTECT-ON", lm.protect().available(), "protect attribution available");
        Object ok = gpCall("createParentClaim", new Class<?>[]{World.class, UUID.class},
                world, admin.getUniqueId());
        result("A-PARENT-CLAIM", Boolean.TRUE.equals(ok), "big admin claim over the build");
        econ.depositPlayer(renter, 2000.0);
        writeSign(admin, 100, SY, 100, new String[]{"[Landmark]", "Rig Plot", "PLOT:SPOT", ""});
        later(10, this::bootA2);
    }

    void bootA2() {
        Spot s = spot("lm-1");
        result("A-REGISTER", s != null && s.isPlot() && s.tier == Spot.Tier.SPOT,
                "spot=" + (s == null ? "none" : s.id + " " + s.type + ":" + s.tier));
        Block sign = world.getBlockAt(100, SY, 100);
        result("A-SIGN-FACE", "Available".equals(signLine(sign, 2)),
                "L3='" + signLine(sign, 2) + "' L4='" + signLine(sign, 3) + "'");
        tapSign(renter, sign);
        later(10, this::bootA3);
    }

    void bootA3() {
        result("A-TAP-OPENS-MENU", lmOpen(renter), "renter tap opened the spot menu");
        Integer rent = slotWith(renter, Material.EMERALD);
        result("A-MENU-HAS-RENT", rent != null, "Rent button present for an available spot");
        if (rent == null) return;
        clickSlot(renter, rent);
        later(10, () -> {
            Integer confirm = slotWith(renter, Material.LIME_CONCRETE);
            result("A-CONFIRM-SCREEN", confirm != null, "confirm step before any charge");
            result("A-NO-CHARGE-YET", balanceCents(renter) == 200_000,
                    "balance untouched before Confirm: " + balanceCents(renter));
            if (confirm == null) return;
            clickSlot(renter, confirm);
            later(10, this::bootA4);
        });
    }

    void bootA4() {
        Spot s = spot("lm-1");
        result("A-CHARGED", balanceCents(renter) == 150_000, "balance=" + balanceCents(renter) + " (rent $500)");
        result("A-OWNER-SET", s != null && renter.getUniqueId().equals(s.ownerId) && s.mode == Spot.Mode.RENTED,
                "owner=" + (s == null ? "none" : s.ownerName));
        Block sign = world.getBlockAt(100, SY, 100);
        result("A-SIGN-OWNER", "RigRenter".equals(signLine(sign, 2)), "L3='" + signLine(sign, 2) + "'");
        claimId = s == null || s.claimId == null ? -1 : s.claimId;
        result("A-CLAIM-ID", claimId > 0, "claimId=" + claimId);
        if (claimId > 0) {
            Object rights = gpCall("claimRights", new Class<?>[]{long.class, UUID.class},
                    claimId, renter.getUniqueId());
            result("A-CLAIM-RIGHTS", "Build".equals(rights),
                    "subdivision build rights for the buyer=" + rights
                            + " (GP subdivisions keep the admin title; rights are trust)");
        }
        // Guest view: view-only menu
        tapSign(guest, sign);
        later(10, this::bootA5);
    }

    void bootA5() {
        boolean viewOnly = lmOpen(guest)
                && !topHas(guest, Material.EMERALD) && !topHas(guest, Material.GOLD_INGOT)
                && !topHas(guest, Material.CLOCK) && !topHas(guest, Material.OAK_DOOR);
        result("A-GUEST-VIEW-ONLY", viewOnly, "guest sees info + Close only, no action buttons");
        guest.closeInventory();
        // Unregistered sign: nothing opens, nothing is cancelled
        Block plain = world.getBlockAt(120, SY, 120);
        plain.setType(Material.OAK_SIGN);
        tapSign(guest, plain);
        later(10, () -> {
            result("A-UNREGISTERED-SIGN", !lmOpen(guest), "unregistered sign opens no menu");
            bootA6();
        });
    }

    void bootA6() {
        // Renter builds inside the plot (real place events feed ExcelProtect)
        placeBlock(renter, 100, SY, 101, Material.GLOWSTONE);
        placeBlock(renter, 101, SY, 101, Material.GLOWSTONE);
        placeBlock(renter, 102, SY, 100, Material.CHEST);
        Block chest = world.getBlockAt(102, SY, 100);
        if (chest.getState() instanceof Container c) {
            c.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        }
        // Loose items: dropped above the chest so they settle inside the region.
        world.dropItem(new org.bukkit.Location(world, 102.5, SY + 2.5, 100.5), new ItemStack(Material.EMERALD, 3));
        waitProtectFlush(0);
    }

    void waitProtectFlush(int tries) {
        File dir = new File(Bukkit.getPluginsFolder(), "ExcelProtect");
        File[] files = dir.listFiles((d, n) -> n.startsWith("blocks-") && n.endsWith(".jsonl"));
        boolean found = false;
        try {
            if (files != null) {
                for (File f : files) {
                    if (Files.readString(f.toPath()).contains("glowstone")) { found = true; break; }
                }
            }
        } catch (Exception ignored) { }
        if (found) {
            result("A-PROTECT-RECORDED", true, "placements flushed to blocks jsonl");
            later(5, this::bootA7);
        } else if (tries > 40) {
            result("A-PROTECT-RECORDED", false, "no glowstone in Protect stream after 40 tries");
            later(5, this::bootA7);
        } else {
            later(20, () -> waitProtectFlush(tries + 1));
        }
    }

    void bootA7() {
        // Age into GRACE (paid-through 3 days ago): sweep must NOT finalize.
        String graceDate = LocalDate.now().minusDays(3).toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin setpaid lm-1 " + graceDate);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin sweep");
        later(10, () -> {
            Spot s = spot("lm-1");
            result("A-GRACE-HOLDS", s != null && !s.isAvailable(), "still rented inside grace");
            Block sign = world.getBlockAt(100, SY, 100);
            result("A-GRACE-SIGN", signLine(sign, 3).startsWith("Grace til"),
                    "L4='" + signLine(sign, 3) + "'");
            // Age past grace (10 days): the next sweep finalizes.
            String lapseDate = LocalDate.now().minusDays(10).toString();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin setpaid lm-1 " + lapseDate);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin sweep");
            later(15, this::bootA8);
        });
    }

    void bootA8() {
        Spot s = spot("lm-1");
        result("A-LAPSE-FREES-SPOT", s != null && s.isAvailable() && s.claimId == null,
                "spot available again, claim id cleared");
        Block sign = world.getBlockAt(100, SY, 100);
        result("A-LAPSE-SIGN", "Available".equals(signLine(sign, 2)), "L3='" + signLine(sign, 2) + "'");
        result("A-BLOCKS-CLEARED",
                world.getBlockAt(100, SY, 101).getType() == Material.AIR
                        && world.getBlockAt(101, SY, 101).getType() == Material.AIR
                        && world.getBlockAt(102, SY, 100).getType() == Material.AIR,
                "attributed blocks (2 glowstone + the chest) cleared");
        Object exists = gpCall("claimExists", new Class<?>[]{long.class}, claimId);
        result("A-CLAIM-DELETED", Boolean.FALSE.equals(exists), "subdivision deleted");
        // Redemption box: diamonds (container) + emeralds (loose) + materials
        // (2 glowstone + 1 chest from attribution), never expiring.
        Map<Material, Integer> contents = new HashMap<>();
        int chestCount = 0;
        for (Store.Box box : lm.store().boxes) {
            if (!renter.getUniqueId().equals(box.owner)) continue;
            for (int[] cell : box.chests) {
                chestCount++;
                Block b = world.getBlockAt(cell[0], cell[1], cell[2]);
                if (b.getState() instanceof Container c) {
                    for (ItemStack it : c.getInventory().getContents()) {
                        if (it != null) contents.merge(it.getType(), it.getAmount(), Integer::sum);
                    }
                }
            }
        }
        result("A-REDEMPTION-BOX", chestCount >= 1, "physical redemption chest(s): " + chestCount);
        result("A-REDEMPTION-CONTENTS",
                contents.getOrDefault(Material.DIAMOND, 0) == 5
                        && contents.getOrDefault(Material.EMERALD, 0) == 3
                        && contents.getOrDefault(Material.GLOWSTONE, 0) == 2
                        && contents.getOrDefault(Material.CHEST, 0) == 1,
                "box=" + contents);
        boolean statusOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin status");
        result("A-CONSOLE-STATUS", statusOk, "console admin surface runs");
        note("RIGREADY STOP-A");
    }

    // ================================================================ Boot B

    void bootB1() {
        result("B-GP-OFF", !lm.gp().available(), "no GriefPrevention: adapter off");
        result("B-PROTECT-OFF", !lm.protect().available(), "no ExcelProtect: attribution off");
        econ.depositPlayer(renter, 10_000.0);
        writeSign(admin, 100, SY, 100, new String[]{"[Landmark]", "Rig Mark", "LANDMARK:SPOT", ""});
        writeSign(admin, 108, SY2, 100, new String[]{"[Landmark]", "Rig Heart", "PLOT:HEART", ""});
        later(10, this::bootB2);
    }

    void bootB2() {
        Spot mark = spot("lm-1"), heart = spot("lm-2");
        result("B-REGISTERED", mark != null && heart != null && !mark.isPlot() && heart.isPlot(),
                "landmark + plot registered without GP");
        rentViaMenu(100, SY, 100, () -> later(5, this::bootB3));
    }

    void rentViaMenu(int x, int y, int z, Runnable done) {
        tapSign(renter, world.getBlockAt(x, y, z));
        later(10, () -> {
            Integer rent = slotWith(renter, Material.EMERALD);
            if (rent == null) { note("rentViaMenu: no Rent button"); done.run(); return; }
            clickSlot(renter, rent);
            later(10, () -> {
                Integer confirm = slotWith(renter, Material.LIME_CONCRETE);
                if (confirm == null) { note("rentViaMenu: no Confirm button"); done.run(); return; }
                clickSlot(renter, confirm);
                later(10, done);
            });
        });
    }

    void bootB3() {
        Spot mark = spot("lm-1");
        result("B-RENT-LANDMARK", mark != null && renter.getUniqueId().equals(mark.ownerId)
                        && mark.claimId == null && balanceCents(renter) == 950_000,
                "naming-only rent, balance=" + balanceCents(renter));
        rentViaMenu(108, SY2, 100, () -> later(5, this::bootB4));
    }

    void bootB4() {
        Spot heart = spot("lm-2");
        result("B-RENT-PLOT-NO-GP", heart != null && renter.getUniqueId().equals(heart.ownerId)
                        && heart.claimId == null && balanceCents(renter) == 650_000,
                "plot rented with NO subdivision (fail-soft), balance=" + balanceCents(renter));
        String lapseDate = LocalDate.now().minusDays(10).toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin setpaid lm-2 " + lapseDate);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "landmarks admin sweep");
        later(15, () -> {
            Spot h2 = spot("lm-2");
            Spot m2 = spot("lm-1");
            result("B-FINALIZE-CLEAN", h2 != null && h2.isAvailable() && m2 != null && !m2.isAvailable(),
                    "plot finalized with empty tally; the landmark rental is untouched");
            result("B-NO-REDEMPTION", lm.store().boxes.isEmpty() && lm.store().virtual.isEmpty(),
                    "nothing placed -> no redemption box, no errors");
            note("RIGREADY STOP-B");
        });
    }
}
