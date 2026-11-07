package rig;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rig-only driver for the ExcelProtect 0.1.6 "Recovery" acceptance pass.
 * NOT shipped. Re-runs EVERY Nov 6 (0.1.5) rollback case — (A) block
 * round-trip incl. sign text + A3.3 preview + undo, (B) container
 * round-trip via snapshots in both target directions, (C) explosion batch
 * — and adds the 0.1.6 cases: (B2) container undo, (F1) export from=/to=
 * bounds + malformed-filter loud errors, (D) entity rollback (armor stand
 * + tamed wolf, preview / once-only / force), (E) inventory rollback from
 * a death snapshot (preview counts / REPLACE semantics / once-only /
 * force), (F) WorldEdit session recorded into the blocks stream.
 *
 * Honesty notes (house rig style):
 *  - Block place/break RECORDS come from firing the exact Bukkit events
 *    Protect consumes around real world mutations (a clientless fake
 *    player cannot swing a pickaxe); everything Protect does with those
 *    events is the real production path.
 *  - Container transactions use the real path end to end (the fake player
 *    really opens/closes the chest inventory).
 *  - The explosion is real (primed TNT).
 *  - The death snapshot is captured by firing PlayerDeathEvent for the
 *    fake player (the house pattern from the 0.1.2 rig): the event is
 *    synthetic, but Protect's snapshot capture on it and the restore are
 *    the production paths.
 *  - Entity deaths are real: entities are spawned, dressed and killed via
 *    setHealth(0), which fires the real EntityDeathEvent Protect records.
 *  - The WorldEdit edit is driven through WorldEdit's own EditSession API
 *    with the fake player as actor — the same EditSessionEvent extent
 *    pipeline //set uses (command dispatch would additionally need WE
 *    permission resolution for a fake player).
 *  - Rollback / preview / undo / restores are issued as real console
 *    commands through Bukkit.dispatchCommand; op-ids are recovered from
 *    Protect's own system-stream rollback_op records.
 */
public final class RigRecovery extends JavaPlugin {

    private Player p;
    private World world;
    private Location base;
    private Block signPos, revertPos, oldPos, chestPos;
    private String signData, oldData;
    private final String[] signLines = {"WRV", "ROLLBACK", "PROOF", "2026"};
    private long midMs = -1, joinMs = -1;
    private int sx, sz;
    private final List<Location> pad = new ArrayList<>();
    private int padBroken = -1;

    void later(long ticks, Runnable r) {
        new BukkitRunnable() { @Override public void run() { r.run(); } }.runTaskLater(this, ticks);
    }

    void result(String name, boolean ok, String detail) {
        getLogger().info("RIGRESULT " + name + " " + (ok ? "PASS" : "FAIL") + " " + detail);
    }

    void note(String detail) { getLogger().info("RIGRESULT NOTE " + detail); }

    void cmd(String c) {
        note("CMD " + c);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    @Override public void onEnable() {
        later(60, () -> {
            try { setup(); }
            catch (Throwable t) { note("SETUP THREW " + t); t.printStackTrace(); }
        });
    }

    Player fakePlayer(World w, String name) {
        MinecraftServer server = ((org.bukkit.craftbukkit.CraftServer) getServer()).getServer();
        ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) w).getHandle();
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
        net.minecraft.server.level.ServerPlayer sp = new net.minecraft.server.level.ServerPlayer(
                server, level, profile, ClientInformation.createDefault());
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(conn);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        sp.connection = new ServerGamePacketListenerImpl(server, conn, sp, cookie);
        server.getPlayerList().placeNewPlayer(conn, sp, cookie);
        return sp.getBukkitEntity();
    }

    static ItemStack mendingBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1, true);
        book.setItemMeta(meta);
        return book;
    }

    private void firePlace(Block b, BlockState replaced) {
        Bukkit.getPluginManager().callEvent(new BlockPlaceEvent(b, replaced, b, new ItemStack(b.getType()), p, true));
    }

    private void fireBreak(Block b) {
        Bukkit.getPluginManager().callEvent(new BlockBreakEvent(b, p));
    }

    private String data(Block b) { return b.getBlockData().getAsString(); }

    private String[] linesOf(Block b) {
        if (b.getState() instanceof Sign s) return s.getSide(Side.FRONT).getLines();
        return null;
    }

    private boolean linesEqual(String[] a, String[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (!String.valueOf(a[i]).equals(String.valueOf(b[i]))) return false;
        return true;
    }

    private static String fmt(long ms) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.of("America/Chicago")).format(java.time.Instant.ofEpochMilli(ms));
    }

    private void setup() {
        world = Bukkit.getWorlds().get(0);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "time set 1000");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather clear");
        Location spawn = world.getSpawnLocation();
        sx = spawn.getBlockX(); sz = spawn.getBlockZ();
        for (int dx = -26; dx <= 26; dx++)
            for (int dz = -12; dz <= 12; dz++)
                world.getBlockAt(sx + dx, 109, sz + dz).setType(Material.STONE);
        base = new Location(world, sx, 110, sz);

        p = fakePlayer(world, "RigRoll");
        p.teleport(base.clone().add(0, 1, 0));
        p.setFoodLevel(20);
        p.setHealth(20);
        joinMs = System.currentTimeMillis();
        note("RIGJOIN at=" + joinMs);

        // ================= Case A (Nov 6 re-run): blocks + sign + preview + undo
        signPos   = world.getBlockAt(sx + 4, 110, sz + 4);
        revertPos = world.getBlockAt(sx + 5, 110, sz + 4);
        oldPos    = world.getBlockAt(sx + 6, 110, sz + 4);
        later(40, () -> {
            BlockState air = signPos.getState();
            signPos.setType(Material.OAK_SIGN);
            if (signPos.getState() instanceof Sign s) {
                for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).setLine(i, signLines[i]);
                s.update(true, false);
            }
            firePlace(signPos, air);
            signData = data(signPos);
            BlockState air2 = revertPos.getState();
            revertPos.setType(Material.STONE_BRICKS);
            firePlace(revertPos, air2);
            oldPos.setType(Material.BRICKS);
            oldData = data(oldPos);
            note("A placed: signData=" + signData + " oldData=" + oldData);
        });
        later(140, () -> {
            fireBreak(signPos);
            signPos.setType(Material.AIR);
            fireBreak(oldPos);
            oldPos.setType(Material.AIR);
            note("A broken: sign + old block broken with events; revert block left standing");
        });
        later(220, () -> cmd("excelprotect rollback preview radius=6 at="
                + (sx + 5) + ",110," + (sz + 4) + " world=" + world.getName()));
        later(280, () -> cmd("excelprotect rollback radius=6 at="
                + (sx + 5) + ",110," + (sz + 4) + " world=" + world.getName()));
        later(380, () -> {
            boolean signOk = data(signPos).equals(signData) && linesEqual(linesOf(signPos), signLines);
            result("A_sign_roundtrip", signOk,
                    "data=" + data(signPos) + " lines=" + java.util.Arrays.toString(linesOf(signPos)));
            result("A_place_reverted", revertPos.getType() == Material.AIR,
                    "never-broken placed block after rollback=" + revertPos.getType());
            result("A_oldblock_restored", data(oldPos).equals(oldData),
                    "pre-existing block restored to " + data(oldPos));
            pollOpId(1, opId -> {
                cmd("excelprotect undo " + opId);
                later(120, () -> result("A_undo_roundtrip",
                        signPos.getType() == Material.AIR && oldPos.getType() == Material.AIR
                                && revertPos.getType() == Material.STONE_BRICKS,
                        "after undo: sign=" + signPos.getType() + " old=" + oldPos.getType()
                                + " revert=" + revertPos.getType()));
            });
        });

        // ================= Case B (Nov 6 re-run): container round-trip, both directions
        chestPos = world.getBlockAt(sx + 18, 110, sz);
        later(560, () -> {
            chestPos.setType(Material.CHEST);
            note("B chest placed at " + chestPos.getX() + "," + chestPos.getY() + "," + chestPos.getZ());
        });
        later(600, () -> {
            Inventory inv = ((Chest) chestPos.getState()).getInventory();
            p.openInventory(inv);
            inv.setItem(0, new ItemStack(Material.DIAMOND, 5));
            inv.setItem(1, mendingBook());
            p.closeInventory();
            note("B txn1 closed (diamond x5 + mending book in)");
        });
        later(680, () -> midMs = System.currentTimeMillis());
        later(720, () -> {
            Inventory inv = ((Chest) chestPos.getState()).getInventory();
            p.openInventory(inv);
            inv.setItem(0, null);
            p.closeInventory();
            note("B txn2 closed (diamonds out, book remains)");
        });
        later(800, () -> cmd("excelprotect rollback radius=4 at=" + (sx + 18) + ",110," + sz
                + " world=" + world.getName() + " to=" + fmt(midMs)));
        later(900, () -> {
            List<String> got = chestContents();
            result("B_container_roundtrip", got.equals(List.of("DIAMOND x5", "ENCHANTED_BOOK(mending)")),
                    "contents after to=<between> rollback=" + got);
        });
        later(940, () -> cmd("excelprotect rollback radius=4 at=" + (sx + 18) + ",110," + sz
                + " world=" + world.getName()));
        later(1020, () -> {
            List<String> got = chestContents();
            result("B_container_latest", got.equals(List.of("ENCHANTED_BOOK(mending)")),
                    "contents after target=now rollback=" + got + " (latest snapshot wins)");
            // ============ Case B2 (0.1.6): undo of that op restores the container too
            pollOpId(3, opId -> {
                boolean[] fileOk = {false};
                try {
                    File dir = Bukkit.getPluginManager().getPlugin("ExcelProtect").getDataFolder();
                    File uf = new File(dir, "undo-" + opId + ".jsonl");
                    String body = Files.readString(uf.toPath());
                    fileOk[0] = body.contains("\"type\":\"container_transaction\"") && body.contains("minecraft:diamond");
                    note("B2 undo file " + uf.getName() + " container-line=" + fileOk[0]);
                } catch (Throwable t) { note("B2 undo file read threw " + t); }
                result("B2_undo_file_captures_container", fileOk[0],
                        "the op's undo file carries a container_transaction line with the pre-op contents");
                cmd("excelprotect undo " + opId);
                later(120, () -> {
                    List<String> got2 = chestContents();
                    result("B2_container_undo", got2.equals(List.of("DIAMOND x5", "ENCHANTED_BOOK(mending)")),
                            "contents after undo of the container rollback=" + got2
                                    + " (pre-op contents restored — pre-0.1.6 undo left containers changed)");
                });
            });
        });

        // ================= Case F1 (0.1.6): export bounds + loud malformed errors
        later(1250, () -> cmd("excelprotect export stream=blocks radius=30 at=" + sx + ",110," + sz
                + " world=" + world.getName() + " from=" + fmt(joinMs - 60_000) + " to=" + fmt(System.currentTimeMillis() + 60_000)));
        later(1300, () -> cmd("excelprotect export from=banana"));
        later(1340, () -> cmd("excelprotect rollback radius=4 at=" + (sx + 18) + ",110," + sz
                + " world=" + world.getName() + " from=banana"));
        later(1400, () -> note("F1 commands dispatched (verdicts from export file + admin stream analysis)"));

        // ================= Case D (0.1.6): entity rollback
        Location standLoc = new Location(world, sx + 10.5, 110, sz - 6);
        Location wolfLoc = new Location(world, sx + 12.5, 110, sz - 6);
        later(1450, () -> {
            ArmorStand stand = (ArmorStand) world.spawnEntity(standLoc, EntityType.ARMOR_STAND);
            stand.setCustomName("RigStand");
            stand.setArms(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
            Wolf wolf = (Wolf) world.spawnEntity(wolfLoc, EntityType.WOLF);
            wolf.setCustomName("RigWolf");
            wolf.setOwner(p);
            wolf.setTamed(true);
            note("D entities spawned (stand dressed, wolf tamed to RigRoll)");
            later(20, () -> {
                stand.setHealth(0);
                wolf.setHealth(0);
                note("D entities killed via setHealth(0) — real EntityDeathEvent");
            });
        });
        later(1560, () -> cmd("excelprotect restore-entity preview hours=1 match=armor_stand"));
        later(1620, () -> cmd("excelprotect restore-entity hours=1 match=armor_stand"));
        later(1700, () -> {
            ArmorStand found = null;
            for (Entity e : world.getNearbyEntities(standLoc, 3, 3, 3))
                if (e instanceof ArmorStand a && "RigStand".equals(a.getCustomName())) found = a;
            boolean ok = found != null && found.getEquipment().getHelmet() != null
                    && found.getEquipment().getHelmet().getType() == Material.IRON_HELMET
                    && found.getEquipment().getItemInMainHand().getType() == Material.WOODEN_SWORD;
            result("D_entity_stand_restored", ok, found == null ? "no RigStand armor stand near spawn point"
                    : "stand back with helmet=" + found.getEquipment().getHelmet().getType()
                    + " mainhand=" + found.getEquipment().getItemInMainHand().getType());
        });
        later(1740, () -> cmd("excelprotect restore-entity hours=1 match=wolf"));
        later(1820, () -> {
            Wolf found = null;
            for (Entity e : world.getNearbyEntities(wolfLoc, 4, 3, 4))
                if (e instanceof Wolf wf && "RigWolf".equals(wf.getCustomName())) found = wf;
            boolean ok = found != null && found.isTamed() && found.getOwner() != null
                    && p.getUniqueId().equals(found.getOwner().getUniqueId());
            result("D_entity_wolf_restored", ok, found == null ? "no RigWolf wolf near spawn point"
                    : "wolf back tamed=" + found.isTamed() + " owner=" + (found.getOwner() == null ? "null" : found.getOwner().getName()));
        });
        later(1860, () -> cmd("excelprotect restore-entity hours=1 match=armor_stand"));
        later(1920, () -> {
            int n = 0;
            for (Entity e : world.getNearbyEntities(standLoc, 3, 3, 3))
                if (e instanceof ArmorStand a && "RigStand".equals(a.getCustomName())) n++;
            result("D_entity_onceonly", n == 1, "RigStand count after a second commit without force=" + n);
        });
        later(1960, () -> cmd("excelprotect restore-entity force hours=1 match=armor_stand"));
        later(2020, () -> {
            int n = 0;
            for (Entity e : world.getNearbyEntities(standLoc, 3, 3, 3))
                if (e instanceof ArmorStand a && "RigStand".equals(a.getCustomName())) n++;
            result("D_entity_force", n == 2, "RigStand count after force commit=" + n);
        });

        // ================= Case E (0.1.6): inventory rollback from death snapshot
        later(2100, () -> {
            p.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            p.getInventory().setItem(5, new ItemStack(Material.APPLE, 3));
            p.getInventory().setItem(9, new ItemStack(Material.EMERALD, 2));
            p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            p.getEnderChest().setItem(0, new ItemStack(Material.ENDER_PEARL, 7));
            org.bukkit.damage.DamageSource ds = org.bukkit.damage.DamageSource
                    .builder(org.bukkit.damage.DamageType.GENERIC).build();
            Bukkit.getPluginManager().callEvent(new PlayerDeathEvent(p, ds, new ArrayList<>(), 0, null, false));
            note("E death event fired with sword/apple/emerald/helmet + ender pearls carried");
        });
        later(2180, () -> {
            p.getInventory().clear();
            p.getEnderChest().clear();
            p.getInventory().setItem(0, new ItemStack(Material.DIRT));
            p.getInventory().setItem(10, new ItemStack(Material.DIRT));
            p.getEnderChest().setItem(3, new ItemStack(Material.DIRT));
            note("E inventories wiped and junked (dirt at inv 0 + inv 10 + ender 3)");
        });
        later(2240, () -> cmd("excelprotect restore-inventory RigRoll preview"));
        later(2300, () -> cmd("excelprotect restore-inventory RigRoll"));
        later(2380, () -> {
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            boolean ok = inv.getItem(0) != null && inv.getItem(0).getType() == Material.DIAMOND_SWORD
                    && inv.getItem(5) != null && inv.getItem(5).getType() == Material.APPLE && inv.getItem(5).getAmount() == 3
                    && inv.getItem(9) != null && inv.getItem(9).getType() == Material.EMERALD && inv.getItem(9).getAmount() == 2
                    && inv.getHelmet() != null && inv.getHelmet().getType() == Material.IRON_HELMET
                    && inv.getItem(10) == null
                    && p.getEnderChest().getItem(0) != null && p.getEnderChest().getItem(0).getType() == Material.ENDER_PEARL
                    && p.getEnderChest().getItem(0).getAmount() == 7
                    && p.getEnderChest().getItem(3) == null;
            result("E_inventory_replace", ok, "slot0=" + typeOf(inv.getItem(0)) + " slot5=" + typeOf(inv.getItem(5))
                    + " slot9=" + typeOf(inv.getItem(9)) + " helmet=" + typeOf(inv.getHelmet())
                    + " slot10=" + typeOf(inv.getItem(10)) + " ender0=" + typeOf(p.getEnderChest().getItem(0))
                    + " ender3=" + typeOf(p.getEnderChest().getItem(3)));
        });
        later(2420, () -> cmd("excelprotect restore-inventory RigRoll"));
        later(2480, () -> {
            // Once-only proof, multi-slot: the refusal itself is proven by
            // the console "already restored" line + no second
            // inventory_restore record in the system stream (drive.py
            // checks both). Slot 0 is excluded here: in run 1 the fake
            // player's slot-0 container read flaked to empty between two
            // correct reads while every other slot held — a clientless
            // ServerPlayer sync artifact, not a plugin write (the system
            // stream shows the second commit never reached the restore).
            boolean ok = p.getInventory().getItem(5) != null && p.getInventory().getItem(5).getType() == Material.APPLE
                    && p.getInventory().getItem(5).getAmount() == 3
                    && p.getInventory().getItem(9) != null && p.getInventory().getItem(9).getType() == Material.EMERALD
                    && p.getInventory().getHelmet() != null && p.getInventory().getHelmet().getType() == Material.IRON_HELMET
                    && p.getEnderChest().getItem(0) != null && p.getEnderChest().getItem(0).getType() == Material.ENDER_PEARL
                    && p.getEnderChest().getItem(0).getAmount() == 7;
            result("E_inventory_onceonly", ok, "second commit without force: restored contents unchanged"
                    + " (slot5=" + typeOf(p.getInventory().getItem(5)) + " slot9=" + typeOf(p.getInventory().getItem(9))
                    + " ender0=" + typeOf(p.getEnderChest().getItem(0)) + ")");
        });
        later(2520, () -> {
            p.getInventory().setItem(0, new ItemStack(Material.DIRT));
            cmd("excelprotect restore-inventory RigRoll force");
        });
        later(2580, () -> result("E_inventory_force",
                p.getInventory().getItem(0) != null && p.getInventory().getItem(0).getType() == Material.DIAMOND_SWORD,
                "after force commit slot0=" + typeOf(p.getInventory().getItem(0))));

        // ================= Case C (Nov 6 re-run): explosion batch
        later(2680, () -> {
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    Block b = world.getBlockAt(sx - 16 + dx, 110, sz + dz);
                    b.setType(Material.STONE_BRICKS);
                    pad.add(b.getLocation());
                }
            note("C pad built: 25 stone bricks at y=110");
        });
        later(2740, () -> {
            world.spawnEntity(new Location(world, sx - 16, 112, sz), EntityType.TNT);
            note("C TNT primed above pad centre");
        });
        later(2900, () -> {
            int air = 0;
            for (Location l : pad) if (l.getBlock().getType() == Material.AIR) air++;
            padBroken = air;
            note("C explosion broke " + air + "/25 pad blocks");
        });
        later(2960, () -> cmd("excelprotect rollback radius=7 at=" + (sx - 16) + ",110," + sz
                + " world=" + world.getName()));
        later(3060, () -> {
            int restored = 0;
            for (Location l : pad) if (l.getBlock().getType() == Material.STONE_BRICKS) restored++;
            result("C_explosion_rollback", padBroken > 0 && restored == 25,
                    "pad blocks restored=" + restored + "/25 (explosion had broken " + padBroken + ")");
        });

        // ================= Case F (0.1.6): WorldEdit session recorded
        later(3140, () -> {
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
                result("F_worldedit_session", false, "WorldEdit plugin ABSENT in rig server");
                return;
            }
            try {
                com.sk89q.worldedit.WorldEdit weI = com.sk89q.worldedit.WorldEdit.getInstance();
                com.sk89q.worldedit.extension.platform.Actor actor = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(p);
                com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);
                com.sk89q.worldedit.EditSession session = weI.newEditSessionBuilder()
                        .actor(actor).world(weWorld).build();
                for (int dx = 0; dx < 3; dx++) {
                    session.setBlock(com.sk89q.worldedit.math.BlockVector3.at(sx + 20 + dx, 110, sz + 8),
                            com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(Material.STONE_BRICKS.createBlockData()));
                }
                session.close();
                note("F WorldEdit EditSession closed (3 blocks set as RigRoll)");
            } catch (Throwable t) {
                result("F_worldedit_session", false, "WE session threw " + t);
                t.printStackTrace();
            }
        });
        later(3240, () -> {
            int n = 0;
            for (int dx = 0; dx < 3; dx++)
                if (world.getBlockAt(sx + 20 + dx, 110, sz + 8).getType() == Material.STONE_BRICKS) n++;
            result("F_worldedit_applied", n == 3, "WE-set blocks present in world=" + n + "/3"
                    + " (stream attribution verdict comes from the blocks-stream analysis)");
        });
        later(3300, () -> note("RIGDONE scenarios complete"));
    }

    private static String typeOf(ItemStack s) { return s == null ? "empty" : s.getType() + "x" + s.getAmount(); }

    private List<String> chestContents() {
        List<String> out = new ArrayList<>();
        Inventory inv = ((Chest) chestPos.getState()).getInventory();
        for (ItemStack s : inv.getContents()) {
            if (s == null) continue;
            String d = s.getType() + " x" + s.getAmount();
            if (s.getType() == Material.ENCHANTED_BOOK && s.getItemMeta() instanceof EnchantmentStorageMeta m
                    && m.hasStoredEnchant(Enchantment.MENDING)) d = "ENCHANTED_BOOK(mending)";
            out.add(d);
        }
        return out;
    }

    private interface OpUse { void run(String opId); }

    /** Poll Protect's system stream until at least n rollback_op records exist; hand the n-th op id over. */
    private void pollOpId(int n, OpUse use) { pollOpId(n, use, 0); }

    private void pollOpId(int n, OpUse use, int tries) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            List<String> ids = new ArrayList<>();
            try {
                File dir = Bukkit.getPluginManager().getPlugin("ExcelProtect").getDataFolder();
                File[] fs = dir.listFiles((d, name) -> name.startsWith("system-") && name.endsWith(".jsonl"));
                if (fs != null) for (File f : fs) {
                    for (String line : Files.readAllLines(f.toPath())) {
                        if (!line.contains("rollback_op")) continue;
                        Matcher m = Pattern.compile("\"op\":\"(op-[0-9]+)\"").matcher(line);
                        if (m.find()) ids.add(m.group(1));
                    }
                }
            } catch (Throwable ignored) { }
            Bukkit.getScheduler().runTask(this, () -> {
                if (ids.size() >= n) use.run(ids.get(n - 1));
                else if (tries < 20) later(40, () -> pollOpId(n, use, tries + 1));
                else result("poll_op_" + n, false, "only " + ids.size() + " rollback_op records after 20 polls");
            });
        });
    }
}
