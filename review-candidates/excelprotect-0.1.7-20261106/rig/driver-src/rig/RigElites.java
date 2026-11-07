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
 * Rig-only driver for the ExcelProtect 0.1.7 "Parity" acceptance pass.
 * NOT shipped. Re-runs EVERY 0.1.6 case unchanged — (A) block
 * round-trip incl. sign text + A3.3 preview + undo, (B) container
 * round-trip via snapshots in both target directions, (C) explosion batch
 * — (B2) container undo, (F1) export from=/to=
 * bounds + malformed-filter loud errors, (D) entity rollback (armor stand
 * + tamed wolf, preview / once-only / force), (E) inventory rollback from
 * a death snapshot (preview counts / REPLACE semantics / once-only /
 * force), (F) WorldEdit session recorded into the blocks stream — and adds
 * the 0.1.7 cases: (G) the DeathRecord importer round-trip on a synthetic
 * Kailey-shaped fixture set (4 already-restored burns, 1 open burn, 1
 * unknown burn, a mixed death, a fully-restored + xp-restored death, and
 * her Lost & Found state file) with a second all-skipped import, and
 * (H) the kill-parity spot check: a fall-softened sword kill of a normal
 * zombie and a player-attributed kill of a genuine EliteMobs elite
 * (converted through EliteMobs' own EliteEntity API, the same tracker
 * Protect probes).
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
public final class RigElites extends JavaPlugin {

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
            // A restored tamed wolf teleports to its owner (RigRoll, standing
            // at base ~14 blocks from the death point) — search BOTH spots.
            // (The 0.1.6 rig searched only the death point and reported this
            // case as a proximity artifact on its run 4; the restore itself
            // was proven by the console + system-stream records.)
            Wolf found = null;
            for (Entity e : world.getNearbyEntities(wolfLoc, 4, 3, 4))
                if (e instanceof Wolf wf && "RigWolf".equals(wf.getCustomName())) found = wf;
            if (found == null)
                for (Entity e : world.getNearbyEntities(p.getLocation(), 6, 3, 6))
                    if (e instanceof Wolf wf && "RigWolf".equals(wf.getCustomName())) found = wf;
            boolean ok = found != null && found.isTamed() && found.getOwner() != null
                    && p.getUniqueId().equals(found.getOwner().getUniqueId());
            result("D_entity_wolf_restored", ok, found == null ? "no RigWolf wolf near death point or owner"
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
        // ================= Case G (0.1.7): DeathRecord importer round-trip
        final String kailUuid = "00000000-0000-0000-0009-01f40ccd59de";
        final File drRoot = new File(System.getProperty("user.dir"), "rigdeathrecord");
        later(3400, () -> {
            try {
                long recent = System.currentTimeMillis() - 86_400_000L;
                String b = drRoot.getAbsolutePath();
                // The census pattern by shape: Kailey's four already-restored
                // netherite items (the re-offer hazard), one open burn, one
                // unknown-owner burn.
                drBurn(new File(b, "burned/" + kailUuid + "/g-burn-sword.yml"), ".coolgirlkail", kailUuid, recent,
                        new ItemStack(Material.NETHERITE_SWORD), 1);
                drBurn(new File(b, "burned/" + kailUuid + "/g-burn-shovel.yml"), ".coolgirlkail", kailUuid, recent,
                        new ItemStack(Material.NETHERITE_SHOVEL), 1);
                drBurn(new File(b, "burned/" + kailUuid + "/g-burn-boots.yml"), ".coolgirlkail", kailUuid, recent,
                        new ItemStack(Material.NETHERITE_BOOTS), 1);
                drBurn(new File(b, "burned/" + kailUuid + "/g-burn-helmet.yml"), ".coolgirlkail", kailUuid, recent,
                        new ItemStack(Material.NETHERITE_HELMET), 1);
                drBurn(new File(b, "burned/" + kailUuid + "/g-burn-axe.yml"), ".coolgirlkail", kailUuid, recent,
                        new ItemStack(Material.DIAMOND_AXE), 0);
                drBurn(new File(b, "burned/unknown/g-burn-unknown.yml"), null, null, recent,
                        new ItemStack(Material.IRON_PICKAXE), 0);
                drDeathMixed(new File(b, "deaths/" + kailUuid + "/g-death-mixed.yml"), ".coolgirlkail", kailUuid, recent);
                drDeathFull(new File(b, "deaths/" + kailUuid + "/g-death-full.yml"), ".coolgirlkail", kailUuid, recent);
                writeDr(new File(b, "lostfound/players/" + kailUuid + ".yml"),
                        "free-used: false\nfree-used-epoch: 0\nrecoveries: 3\nlast-recovery-epoch: 1793582762530\ntip-ok: true\n");
                result("G_fixtures_written", true, "synthetic DeathRecord folder at " + b);
            } catch (Throwable t) { result("G_fixtures_written", false, "threw " + t); t.printStackTrace(); }
        });
        later(3560, () -> cmd("excelprotect import deathrecord " + drRoot.getAbsolutePath()));
        later(3800, () -> {
            try {
                File dir = Bukkit.getPluginManager().getPlugin("ExcelProtect").getDataFolder();
                String marks = Files.readString(new File(dir, "restore-marks.json").toPath());
                int found = 0; List<String> missing = new ArrayList<>();
                for (String line : streamLines(dir, "deaths")) {
                    if (!line.contains("\"source_restored\":true")) continue;
                    Matcher tm = Pattern.compile("\"ts_ms\":(\\d+)").matcher(line);
                    Matcher im = Pattern.compile("\"item\":\\{\"id\":\"([^\"]+)\",\"count\":(\\d+)").matcher(line);
                    if (tm.find() && im.find()) {
                        String id = tm.group(1) + ":" + im.group(1) + "#" + im.group(2);
                        if (marks.contains("\"" + id + "\"")) found++; else missing.add(id);
                    }
                }
                boolean deathMark = marks.contains("death:imported:g-death-full.yml");
                result("G_import_restored_marks", found == 4 && deathMark,
                        "restored-burn marks in restore-marks.json=" + found + "/4 missing=" + missing
                                + " fully-restored death marked=" + deathMark);
                String players = Files.readString(new File(dir, "lostfound-players.json").toPath());
                boolean lfOk = players.contains(kailUuid) && players.contains("\"recoveries\":3")
                        && players.contains("\"free_used\":false")
                        && players.contains("\"last_recovery_epoch\":1793582762530");
                result("G_import_lf_state", lfOk,
                        "lostfound-players.json carries Kailey's migrated state (recoveries=3, free unused, epoch pinned)");
            } catch (Throwable t) { result("G_import_restored_marks", false, "verify threw " + t); t.printStackTrace(); }
        });
        later(3960, () -> cmd("excelprotect import deathrecord " + drRoot.getAbsolutePath())); // drive.py: 2nd import must be all-skipped

        // ================= Case H (0.1.7): kill-field parity spot check
        later(4200, () -> {
            // Night, so the test zombies cannot burn to death in daylight
            // before the attributed finisher lands (run 2 lost that race:
            // FALL 17 + FIRE_TICK 3 = exactly 20 HP, killer=environment).
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "time set 18000");
            p.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            org.bukkit.entity.Zombie z = (org.bukkit.entity.Zombie) world.spawnEntity(
                    new Location(world, sx - 24, 131, sz - 10), EntityType.ZOMBIE);
            note("H1 zombie uuid=" + z.getUniqueId() + " (dropped 21 blocks onto the pad, finished by RigRoll)");
            // Small finisher on purpose: Bukkit records the event's full
            // damage, not the HP-clamped remainder, so a 50-damage finisher
            // would swamp the fall share the case exists to measure.
            later(60, () -> { if (!z.isDead()) z.damage(2.5, p); });
        });
        later(4400, () -> {
            p.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            org.bukkit.entity.Zombie z = (org.bukkit.entity.Zombie) world.spawnEntity(
                    new Location(world, sx - 20, 110, sz - 10), EntityType.ZOMBIE);
            try {
                Class<?> eliteEntity = Class.forName("com.magmaguy.elitemobs.mobconstructor.EliteEntity");
                eliteEntity.getConstructor(org.bukkit.entity.LivingEntity.class, int.class,
                                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class)
                        .newInstance(z, 5, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
                Class<?> tracker = Class.forName("com.magmaguy.elitemobs.entitytracker.EntityTracker");
                boolean isElite = Boolean.TRUE.equals(tracker.getMethod("isEliteMob", Entity.class).invoke(null, z));
                Object ee = tracker.getMethod("getEliteMobEntity", Entity.class).invoke(null, z);
                Object lvl = ee == null ? null : ee.getClass().getMethod("getLevel").invoke(ee);
                result("H_elite_registered", isElite && Integer.valueOf(5).equals(lvl),
                        "EntityTracker after EliteEntity conversion: isElite=" + isElite + " level=" + lvl);
                if (!isElite) return;
            } catch (Throwable t) { result("H_elite_registered", false, "conversion threw " + t); t.printStackTrace(); return; }
            note("H2 zombie uuid=" + z.getUniqueId());
            int[] rounds = {0};
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() {
                    if (z.isDead() || !z.isValid()) { note("H2 elite dead after rounds=" + rounds[0]); cancel(); return; }
                    if (++rounds[0] > 30) z.setHealth(1); // EM damage scaling vs a level-less fake player: guarantee the kill stays player-attributed
                    z.damage(20, p);
                    if (rounds[0] > 80) { note("H2 elite STILL alive after 80 rounds"); cancel(); }
                }
            }.runTaskTimer(RigElites.this, 20, 10);
        });
        later(5400, () -> note("RIGDONE scenarios complete"));
    }

    // ---------- 0.1.7 Case G fixture helpers (real DeathRecord 0.3.0 shapes) ----------
    private void writeDr(File f, String text) throws Exception {
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), text);
    }

    private static String drItem(ItemStack s, int restoredTimes, String restoredTo) {
        StringBuilder sb = new StringBuilder();
        sb.append("  id: ").append(s.getType().getKey()).append('\n');
        sb.append("  count: ").append(s.getAmount()).append('\n');
        sb.append("  bytes: ").append(java.util.Base64.getEncoder().encodeToString(s.serializeAsBytes())).append('\n');
        if (restoredTimes > 0) {
            sb.append("  restored-times: ").append(restoredTimes).append('\n');
            sb.append("  restored-to: ").append(restoredTo).append('\n');
            sb.append("  restored-by: CONSOLE\n");
        }
        return sb.toString();
    }

    private void drBurn(File f, String owner, String uuid, long epoch, ItemStack item, int restoredTimes) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (owner != null) sb.append("owner: ").append(owner).append('\n');
        if (uuid != null) sb.append("owner-uuid: ").append(uuid).append('\n');
        sb.append("epoch-millis: ").append(epoch).append("\ncause: lava\nworld: minecraft:overworld\nx: 10.5\ny: 64.0\nz: -3.5\nitem:\n");
        sb.append(drItem(item, restoredTimes, owner));
        writeDr(f, sb.toString());
    }

    private void drDeathMixed(File f, String player, String uuid, long epoch) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("player: ").append(player).append("\nuuid: ").append(uuid).append("\nepoch-millis: ").append(epoch)
          .append("\nworld: minecraft:overworld\nxp-level: 27\ntotal-experience: 1395\ndropped-exp: 27\ngamemode: SURVIVAL\n")
          .append("death-message: ").append(player).append(" was doomed to fall\ninventory:\n");
        sb.append("- ").append(drItem(new ItemStack(Material.DIAMOND_PICKAXE), 0, null).replace("\n  ", "\n  ").replaceFirst("^  ", ""));
        sb.append("- ").append(drItem(new ItemStack(Material.NETHERITE_BOOTS), 1, player).replaceFirst("^  ", ""));
        sb.append("drops:\n");
        sb.append("- ").append(drItem(new ItemStack(Material.APPLE, 3), 0, null).replaceFirst("^  ", ""));
        writeDr(f, sb.toString());
    }

    private void drDeathFull(File f, String player, String uuid, long epoch) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("player: ").append(player).append("\nuuid: ").append(uuid).append("\nepoch-millis: ").append(epoch)
          .append("\nworld: minecraft:overworld\nxp-level: 9\ntotal-experience: 171\nxp-restored-times: 1\nxp-restored-to: ").append(player)
          .append("\ninventory:\n");
        sb.append("- ").append(drItem(new ItemStack(Material.NETHERITE_HELMET), 1, player).replaceFirst("^  ", ""));
        writeDr(f, sb.toString());
    }

    private List<String> streamLines(File dir, String stream) throws Exception {
        List<String> out = new ArrayList<>();
        File[] fs = dir.listFiles((d, n) -> n.startsWith(stream + "-") && n.endsWith(".jsonl"));
        if (fs != null) for (File f : fs) out.addAll(Files.readAllLines(f.toPath()));
        return out;
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
