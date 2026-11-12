package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.Hazard;
import com.winkreyes.exceltrainingdummy.core.ReportText;
import com.winkreyes.exceltrainingdummy.core.SpawnLayout;
import com.winkreyes.exceltrainingdummy.core.TestSession;
import com.winkreyes.exceltrainingdummy.core.Tier;
import com.winkreyes.exceltrainingdummy.core.ZoneCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelTrainingDummy — WoW Dome / GW2 training-area dummies for WRV
 * (DESIGN, Nov 10, 2026). One standalone plugin; every control is a
 * menu (Java inventory menu + Bedrock Cumulus form), with the compass
 * menu entry and tap-the-dummy as the two doors.
 *
 * The dummy's health pool is VIRTUAL (see core/TestSession): the
 * entity never takes real damage, so no death event, XP, loot,
 * mcMMO/Jobs/economy or boss credit can ever fire off a dummy —
 * clean by construction (DESIGN §7). The entity is stamped in its
 * PersistentDataContainer (keys `training`, `owner`) and with the
 * scoreboard tag `excelTrainingDummy`, so other house plugins
 * (Protect / Details) can recognise a training target.
 */
public final class ExcelTrainingDummy extends JavaPlugin {

    public static final String TAG = "excelTrainingDummy";

    private final Map<UUID, Dummy> byEntity = new LinkedHashMap<>();
    private final Map<UUID, PlayerSetup> setups = new LinkedHashMap<>();
    private ClaimGate claimGate;
    private NamespacedKey keyTraining;
    private NamespacedKey keyOwner;
    private long tickCount;

    // config values
    private Map<Tier, Double> pools;
    private int perPlayerCap;
    private int serverCap;
    private long idleMillis;
    private int endlessResetSeconds;
    private int hazardPulseTicks;
    private Map<Hazard, Double> hazardDamage;
    private double displayScaleFactor;
    private double hazardRange;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        keyTraining = new NamespacedKey(this, "training");
        keyOwner = new NamespacedKey(this, "owner");
        claimGate = new ClaimGate(getLogger());
        getServer().getPluginManager().registerEvents(new DummyListener(this), this);
        DummyCommands commands = new DummyCommands(this);
        getCommand("dummy").setExecutor(commands);
        getCommand("dummy").setTabCompleter(commands);
        // 20-tick ticker: hazard pulses + idle sweep (periods factored inside).
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("ExcelTrainingDummy " + BuildInfo.VERSION + " enabled (build "
                + BuildInfo.BUILD_ID + "); tiers Practice " + ReportText.num(pool(Tier.PRACTICE))
                + " / Dungeon " + ReportText.num(pool(Tier.DUNGEON))
                + " / Seal " + ReportText.num(pool(Tier.SEAL))
                + "; caps " + perPlayerCap + " per player, " + serverCap + " server.");
    }

    @Override
    public void onDisable() {
        for (Dummy d : new ArrayList<>(byEntity.values())) removeDummy(d, false);
        byEntity.clear();
        getLogger().info("ExcelTrainingDummy disabled; all dummies removed.");
    }

    public void loadSettings() {
        reloadConfig();
        pools = new LinkedHashMap<>();
        for (Tier t : Tier.values()) {
            double v = getConfig().getDouble("tiers." + t.name().toLowerCase(), t.defaultPool);
            pools.put(t, v > 0 ? v : t.defaultPool);
        }
        perPlayerCap = Math.max(1, getConfig().getInt("caps.per-player", 5));
        serverCap = Math.max(1, getConfig().getInt("caps.server", 25));
        idleMillis = Math.max(1, getConfig().getInt("despawn.idle-minutes", 10)) * 60_000L;
        endlessResetSeconds = Math.max(1, getConfig().getInt("endless.reset-seconds", 5));
        hazardPulseTicks = Math.max(20, getConfig().getInt("hazard.pulse-ticks", 40));
        hazardDamage = new LinkedHashMap<>();
        for (Hazard h : Hazard.values()) {
            hazardDamage.put(h, getConfig().getDouble(
                    "hazard.damage." + h.name().toLowerCase(), h.defaultDamagePerPulse));
        }
        displayScaleFactor = getConfig().getDouble("boss-armor.display-scale-factor", 1000);
        if (displayScaleFactor <= 0) displayScaleFactor = 1000;
        hazardRange = getConfig().getDouble("hazard.range-blocks", 20);
    }

    // ---- settings accessors -------------------------------------------

    public double pool(Tier t) { return pools.get(t); }
    public double scaleFactor() { return displayScaleFactor; }
    public ClaimGate claimGate() { return claimGate; }
    public boolean isDummy(org.bukkit.entity.Entity e) {
        return e != null && e.getPersistentDataContainer().has(keyTraining, PersistentDataType.BYTE);
    }
    public Dummy dummyOf(org.bukkit.entity.Entity e) {
        return e == null ? null : byEntity.get(e.getUniqueId());
    }
    public PlayerSetup setup(UUID playerId) {
        return setups.computeIfAbsent(playerId, k -> new PlayerSetup());
    }

    // ---- rig / API surface ---------------------------------------------

    public int activeDummyCount(UUID ownerId) {
        int n = 0;
        for (Dummy d : byEntity.values()) if (d.ownerId.equals(ownerId)) n++;
        return n;
    }

    public int totalDummyCount() { return byEntity.size(); }

    public List<Dummy> dummiesOf(UUID ownerId) {
        List<Dummy> out = new ArrayList<>();
        for (Dummy d : byEntity.values()) if (d.ownerId.equals(ownerId)) out.add(d);
        return out;
    }

    public Dummy firstDummyOf(UUID ownerId) {
        for (Dummy d : byEntity.values()) if (d.ownerId.equals(ownerId)) return d;
        return null;
    }

    // ---- spawning ---------------------------------------------------------

    /**
     * Spawns `count` dummies for the player with their dialed-in setup.
     * All-or-nothing: caps, zone and flat-ground are checked for every
     * spot before the first dummy appears. Returns the number spawned
     * (0 = refused, with the reason already sent to the player).
     */
    public int spawn(Player player, int count) {
        PlayerSetup setup = setup(player.getUniqueId());
        if (!SpawnLayout.isSupportedCount(count)) {
            player.sendMessage(Component.text("[Training Dummy] Pick 1, 3 or 5 dummies.", NamedTextColor.RED));
            return 0;
        }
        if (activeDummyCount(player.getUniqueId()) + count > perPlayerCap) {
            player.sendMessage(Component.text("[Training Dummy] Per-player cap is " + perPlayerCap
                    + " dummies — remove some first (Remove all in the /dummy menu).", NamedTextColor.RED));
            return 0;
        }
        if (byEntity.size() + count > serverCap) {
            player.sendMessage(Component.text("[Training Dummy] The server dummy cap (" + serverCap
                    + ") is reached — try again in a bit.", NamedTextColor.RED));
            return 0;
        }
        List<Location> spots = planSpots(player, count);
        if (spots == null) return 0; // reason already sent
        for (Location spot : spots) {
            ZoneCheck.Verdict v = claimGate.verdictAt(spot, player);
            if (!v.allowed()) {
                player.sendMessage(Component.text(v == ZoneCheck.Verdict.DENIED_ADMIN_CLAIM
                        ? "[Training Dummy] That spot is an admin claim — dummies there are staff-run only."
                        : "[Training Dummy] Dummies can only be spawned in the wilderness, on your own claim, or in an admin training zone.",
                        NamedTextColor.RED));
                return 0;
            }
        }
        for (Location spot : spots) createDummy(player, spot, setup);
        player.sendMessage(Component.text("[Training Dummy] " + count + " "
                + (setup.endless ? "Endless" : setup.tier.displayName) + " "
                + (count == 1 ? "dummy" : "dummies") + " up — hit "
                + (count == 1 ? "it" : "them") + " to start the test. "
                + "Tap a dummy with a compass in hand to edit it.", NamedTextColor.GREEN));
        return count;
    }

    /** Ground-checked spawn spots in front of the player, or null (reason sent). */
    private List<Location> planSpots(Player player, int count) {
        Location base = player.getLocation();
        double yawRad = Math.toRadians(base.getYaw());
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);
        double sx = -fz, sz = fx; // spawner's right-hand side vector
        List<Location> out = new ArrayList<>();
        for (double[] off : SpawnLayout.offsets(count)) {
            double wx = base.getX() + fx * off[0] + sx * off[1];
            double wz = base.getZ() + fz * off[0] + sz * off[1];
            Location ground = findGround(base.getWorld(), wx, base.getBlockY(), wz);
            if (ground == null) {
                player.sendMessage(Component.text("[Training Dummy] No flat ground in front of you "
                        + "for the whole set — face some open, level ground and try again.", NamedTextColor.RED));
                return null;
            }
            out.add(ground);
        }
        return out;
    }

    private Location findGround(World world, double x, int nearY, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        for (int dy = 0; dy >= -2; dy--) {
            int y = nearY + dy;
            Block below = world.getBlockAt(bx, y - 1, bz);
            Block feet = world.getBlockAt(bx, y, bz);
            Block head = world.getBlockAt(bx, y + 1, bz);
            if (below.getType().isSolid() && !feet.getType().isSolid() && !head.getType().isSolid()) {
                return new Location(world, bx + 0.5, y, bz + 0.5);
            }
        }
        for (int dy = 1; dy <= 1; dy++) {
            int y = nearY + dy;
            Block below = world.getBlockAt(bx, y - 1, bz);
            Block feet = world.getBlockAt(bx, y, bz);
            Block head = world.getBlockAt(bx, y + 1, bz);
            if (below.getType().isSolid() && !feet.getType().isSolid() && !head.getType().isSolid()) {
                return new Location(world, bx + 0.5, y, bz + 0.5);
            }
        }
        return null;
    }

    private void createDummy(Player owner, Location spot, PlayerSetup setup) {
        World world = spot.getWorld();
        Zombie z = (Zombie) world.spawnEntity(spot, EntityType.ZOMBIE);
        z.setAI(false);
        z.setSilent(true);
        z.setShouldBurnInDay(false);
        z.setBaby(false);
        z.setRemoveWhenFarAway(false);
        z.setPersistent(true);
        z.setCollidable(false);
        z.setGravity(false);
        z.setCustomNameVisible(true);
        z.getPersistentDataContainer().set(keyTraining, PersistentDataType.BYTE, (byte) 1);
        z.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, owner.getUniqueId().toString());
        z.addScoreboardTag(TAG);
        TestSession session = new TestSession(setup.tier, pool(setup.tier), setup.endless,
                setup.executeStart, setup.checkpoints);
        Dummy d = new Dummy(z.getUniqueId(), owner.getUniqueId(), owner.getName(), session,
                setup.hazard, setup.lethal, setup.bossArmor, setup.endless, System.currentTimeMillis());
        byEntity.put(z.getUniqueId(), d);
        refreshNameplate(d);
    }

    // ---- removal ------------------------------------------------------

    public void removeDummy(Dummy d, boolean announceToOwner) {
        byEntity.remove(d.entityId);
        LivingEntity e = d.entity();
        if (e != null) e.remove();
        if (announceToOwner) {
            Player owner = getServer().getPlayer(d.ownerId);
            if (owner != null) owner.sendMessage(Component.text(
                    "[Training Dummy] Dummy removed.", NamedTextColor.GRAY));
        }
    }

    public int removeAllOf(UUID ownerId, boolean announce) {
        List<Dummy> mine = dummiesOf(ownerId);
        for (Dummy d : mine) removeDummy(d, false);
        if (announce) {
            Player owner = getServer().getPlayer(ownerId);
            if (owner != null) owner.sendMessage(Component.text(
                    "[Training Dummy] Removed " + mine.size() + " "
                            + (mine.size() == 1 ? "dummy" : "dummies") + ".", NamedTextColor.GRAY));
        }
        return mine.size();
    }

    /** Logout despawn (DESIGN §1): the owner's dummies leave with them, silently. */
    public void onOwnerQuit(UUID ownerId) {
        for (Dummy d : dummiesOf(ownerId)) removeDummy(d, false);
        setups.remove(ownerId);
    }

    // ---- test flow ------------------------------------------------------

    /** A test finished (pool consumed). Reports, then removes or resets the dummy. */
    public void onTestFinished(Dummy d) {
        long now = System.currentTimeMillis();
        List<UUID> readers = new ArrayList<>();
        readers.add(d.ownerId);
        for (UUID u : d.session.ledgers().keySet()) if (!u.equals(d.ownerId)) readers.add(u);
        for (UUID readerId : readers) {
            Player reader = getServer().getPlayer(readerId);
            if (reader == null) continue;
            for (String line : ReportText.finalReport(d.session, readerId, now,
                    d.hazard, d.lethal, d.bossArmor, displayScaleFactor)) {
                reader.sendMessage(Component.text(line, NamedTextColor.AQUA));
            }
        }
        if (d.endless) {
            // Report printed; the dummy resets to full after a few seconds (DESIGN §2).
            getServer().getScheduler().runTaskLater(this, () -> {
                if (byEntity.containsKey(d.entityId) && d.session.resetting()) {
                    d.session.resetLap(System.currentTimeMillis());
                    d.lastActivityMillis = System.currentTimeMillis();
                    refreshNameplate(d);
                    Player owner = getServer().getPlayer(d.ownerId);
                    if (owner != null) owner.sendMessage(Component.text(
                            "[Training Dummy] Endless dummy reset — lap " + d.session.lap() + " ready.",
                            NamedTextColor.GREEN));
                }
            }, endlessResetSeconds * 20L);
        } else {
            removeDummy(d, false);
            Player owner = getServer().getPlayer(d.ownerId);
            if (owner != null) owner.sendMessage(Component.text(
                    "[Training Dummy] Dummy down. Spawn another from /dummy whenever you're ready.",
                    NamedTextColor.GRAY));
        }
    }

    public void refreshNameplate(Dummy d) {
        LivingEntity e = d.entity();
        if (e == null) return;
        TestSession s = d.session;
        String name = "Training Dummy · " + ReportText.num(s.remaining()) + " / " + ReportText.num(s.pool());
        if (s.endless()) name = "Endless Dummy · lap " + s.lap() + " · " + ReportText.num(s.remaining()) + " left";
        if (s.resetting()) name = "Training Dummy · resetting…";
        e.customName(Component.text(name, NamedTextColor.YELLOW));
    }

    /** Healing by a player counts toward the HPS of their live hazardous test. */
    public void recordHealing(Player player, double amount) {
        for (Dummy d : dummiesOf(player.getUniqueId())) {
            if (d.hazard != Hazard.OFF && !d.session.finished() && !d.session.resetting()) {
                d.session.recordHealing(amount);
                return;
            }
        }
    }

    // ---- ticker ---------------------------------------------------------

    private void tick() {
        tickCount += 20;
        long now = System.currentTimeMillis();
        // Hazard pulses (their own period, factored against the 20-tick ticker).
        if (tickCount % hazardPulseTicks == 0) {
            for (Dummy d : new ArrayList<>(byEntity.values())) {
                if (d.hazard == Hazard.OFF || d.session.resetting()) continue;
                Player owner = getServer().getPlayer(d.ownerId);
                LivingEntity e = d.entity();
                if (owner == null || e == null || !owner.getWorld().equals(e.getWorld())) continue;
                if (owner.getLocation().distanceSquared(e.getLocation()) > hazardRange * hazardRange) continue;
                double applied = Hazard.pulseDamage(hazardDamage.get(d.hazard), owner.getHealth(), d.lethal);
                if (applied > 0) {
                    d.session.recordHazardDamage(applied);
                    owner.damage(applied);
                }
            }
        }
        // Idle sweep every ~20 s of ticker time.
        if (tickCount % 400 == 0) {
            for (Dummy d : new ArrayList<>(byEntity.values())) {
                if (now - d.lastActivityMillis > idleMillis) {
                    removeDummy(d, false);
                    Player owner = getServer().getPlayer(d.ownerId);
                    if (owner != null) owner.sendMessage(Component.text(
                            "[Training Dummy] A dummy sat idle for "
                                    + (idleMillis / 60_000) + " minutes and was put away.",
                            NamedTextColor.GRAY));
                }
            }
        }
    }
}
