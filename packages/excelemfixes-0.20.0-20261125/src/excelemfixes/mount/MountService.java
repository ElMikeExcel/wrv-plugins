package excelemfixes.mount;

import excelemfixes.mount.core.HorseVariant;
import excelemfixes.mount.core.WorldRules;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The horse lifecycle: one active horse per player, summoned and
 * dismissed by the Mount Charm (ExcelItems registry ID 5), with
 * the world gate, the combat gate, the loss cooldown and every
 * despawn trigger from the design note. All state is in memory —
 * the horse is non-persistent, so a restart is just "no horses
 * out", which is always a safe state.
 *
 * The horse is spawned programmatically with the CUSTOM spawn
 * reason: EliteMobs' InstanceProtector cancels natural spawns in
 * its worlds, and CUSTOM is the reason it exempts — rig-proven
 * against the real EliteMobs in the module's mirror proof.
 */
public final class MountService implements Listener {

    public enum ToggleResult {
        SUMMONED, DESPAWNED, DISABLED, BLOCKED_WORLD, IN_COMBAT,
        COOLDOWN, RIDING_OTHER, SPAWN_FAILED
    }

    private final JavaPlugin plugin;
    private final CharmCheck charmCheck;
    private final CombatMarks combatMarks;
    private volatile MountSettings settings;
    private final NamespacedKey horseKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, Horse> active = new HashMap<>();
    private final Map<UUID, Long> cooldownUntilMillis = new HashMap<>();
    private BukkitTask sweepTask;

    MountService(JavaPlugin plugin, MountSettings settings,
                 CharmCheck charmCheck, CombatMarks combatMarks) {
        this.plugin = plugin;
        this.settings = settings;
        this.charmCheck = charmCheck;
        this.combatMarks = combatMarks;
        this.horseKey = new NamespacedKey(plugin, "horse");
        this.ownerKey = new NamespacedKey(plugin, "owner");
    }

    /** Swap in reloaded settings (module reload). */
    void updateSettings(MountSettings newSettings) {
        this.settings = newSettings;
    }

    // ------------------------------------------------------------ lifecycle

    void start() {
        sweepTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::sweep, 100L, 100L);
    }

    void stop() {
        if (sweepTask != null) sweepTask.cancel();
        despawnAll();
    }

    /** Remove every active horse (module disable / retirement flip). */
    public void despawnAll() {
        for (UUID owner : java.util.List.copyOf(active.keySet())) {
            despawn(owner);
        }
    }

    /**
     * Crash-orphan cleanup: any entity carrying our mark that this
     * service has no record for is removed. Horses are spawned
     * non-persistent, so there should never be one — this is cheap
     * insurance for the day that assumption breaks.
     */
    void sweepOrphans() {
        int removed = 0;
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Horse horse : world.getEntitiesByClass(Horse.class)) {
                if (isMountHorse(horse) && !active.containsValue(horse)) {
                    horse.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("summon-mount: removed " + removed
                    + " orphaned mount horse(s) at enable.");
        }
    }

    /** Periodic validity check: a horse that vanished without a
     *  despawn trigger (admin kill, chunk glitch) starts the loss
     *  cooldown instead of leaving a stuck "already out" record. */
    private void sweep() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Horse> entry
                : java.util.List.copyOf(active.entrySet())) {
            Horse horse = entry.getValue();
            if (horse == null || horse.isDead() || !horse.isValid()) {
                active.remove(entry.getKey());
                cooldownUntilMillis.put(entry.getKey(),
                        now + settings.cooldownMillis());
                Player owner = plugin.getServer().getPlayer(entry.getKey());
                if (owner != null) {
                    tell(owner, ChatColor.YELLOW,
                            "Your horse was lost. The charm needs "
                                    + settings.cooldownSeconds()
                                    + "s before it can call another.");
                }
                plugin.getLogger().info("summon-mount: mount horse for "
                        + entry.getKey() + " vanished outside a despawn"
                        + " trigger; loss cooldown applied.");
            }
        }
    }

    // ------------------------------------------------------------ the toggle

    /** The one code path behind the charm, /mountcharm summon and
     *  /mountcharm dismiss. Sends the player their feedback. */
    public ToggleResult toggle(Player player) {
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) {
            despawn(id);
            tell(player, ChatColor.GRAY, "Your horse returns to the charm.");
            return ToggleResult.DESPAWNED;
        }
        if (settings.retired()) {
            tell(player, ChatColor.YELLOW, settings.retirementMessage());
            return ToggleResult.DISABLED;
        }
        if (WorldRules.isBlocked(player.getWorld().getName(),
                settings.blockedPrefixes(), settings.blockedNames())) {
            tell(player, ChatColor.RED,
                    "You can't call your horse here.");
            return ToggleResult.BLOCKED_WORLD;
        }
        long now = System.currentTimeMillis();
        if (inCombat(id, now)) {
            tell(player, ChatColor.RED, "You can't call your horse in"
                    + " combat. Give it "
                    + combatMarks.remainingSeconds(id, now,
                            settings.combatWindowMillis())
                    + "s.");
            return ToggleResult.IN_COMBAT;
        }
        Long cooldownUntil = cooldownUntilMillis.get(id);
        if (cooldownUntil != null && now < cooldownUntil) {
            tell(player, ChatColor.RED, "The charm is resting. "
                    + ((cooldownUntil - now + 999) / 1000)
                    + "s left.");
            return ToggleResult.COOLDOWN;
        }
        if (player.getVehicle() != null) {
            tell(player, ChatColor.RED, "Dismount first.");
            return ToggleResult.RIDING_OTHER;
        }
        return summon(player) ? ToggleResult.SUMMONED
                : ToggleResult.SPAWN_FAILED;
    }

    private boolean summon(Player player) {
        UUID id = player.getUniqueId();
        Location loc = player.getLocation();
        Horse horse = player.getWorld().spawn(loc, Horse.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM, false, h -> {
                    h.setTamed(true);
                    h.setOwner(player);
                    h.setAdult();
                    h.setInvulnerable(true);
                    h.setPersistent(false);
                    h.setRemoveWhenFarAway(false);
                    h.setColor(Horse.Color.values()
                            [HorseVariant.colorIndex(id)]);
                    h.setStyle(Horse.Style.values()
                            [HorseVariant.styleIndex(id)]);
                    h.getInventory().setSaddle(
                            new ItemStack(Material.SADDLE));
                    setAttribute(h, Attribute.MAX_HEALTH,
                            settings.horseHealth());
                    h.setHealth(settings.horseHealth());
                    setAttribute(h, Attribute.MOVEMENT_SPEED,
                            settings.horseSpeed());
                    setAttribute(h, Attribute.JUMP_STRENGTH,
                            settings.horseJump());
                    h.setCustomName(player.getName() + "'s Horse");
                    h.setCustomNameVisible(true);
                    h.getPersistentDataContainer().set(horseKey,
                            PersistentDataType.BYTE, (byte) 1);
                    h.getPersistentDataContainer().set(ownerKey,
                            PersistentDataType.STRING, id.toString());
                });
        if (horse == null || !horse.isValid()) {
            // Another plugin cancelled the spawn outright.
            tell(player, ChatColor.RED,
                    "Your horse couldn't come here.");
            plugin.getLogger().warning("summon-mount: mount spawn"
                    + " cancelled for " + player.getName() + " in "
                    + player.getWorld().getName() + ".");
            return false;
        }
        active.put(id, horse);
        tell(player, ChatColor.GREEN, "Your horse is here.");
        return true;
    }

    private static void setAttribute(Horse horse, Attribute attribute,
                                     double baseValue) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(baseValue);
    }

    /** Despawn one player's horse (no cooldown — a normal return). */
    public void despawn(UUID owner) {
        Horse horse = active.remove(owner);
        if (horse != null) {
            horse.eject();
            horse.remove();
        }
    }

    // ------------------------------------------------------------ queries

    public boolean hasActiveHorse(UUID owner) {
        return active.containsKey(owner);
    }

    public Horse activeHorse(UUID owner) {
        return active.get(owner);
    }

    public boolean isMountHorse(Entity entity) {
        return entity instanceof Horse && entity
                .getPersistentDataContainer()
                .has(horseKey, PersistentDataType.BYTE);
    }

    public UUID ownerOf(Entity entity) {
        String raw = entity.getPersistentDataContainer()
                .get(ownerKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    public boolean inCombat(UUID player, long nowMillis) {
        return combatMarks.inCombat(player, nowMillis,
                settings.combatWindowMillis());
    }

    public long cooldownRemainingSeconds(UUID player, long nowMillis) {
        Long until = cooldownUntilMillis.get(player);
        if (until == null || nowMillis >= until) return 0;
        return (until - nowMillis + 999) / 1000;
    }

    // ------------------------------------------------------------ events

    /**
     * The charm's one gesture. Deliberately NOT ignoreCancelled:
     * PlayerInteractEvent.isCancelled() reports the BLOCK result,
     * which is DENY for every air click (there is no block to
     * interact with) — including genuine ones — so an
     * ignoreCancelled handler never sees air clicks at all
     * (rig-proven: the event arrives "cancelled" at LOWEST with
     * useItemInHand untouched). What governs the held item is
     * useItemInHand: a plugin that deliberately blocks item use
     * (including via setCancelled, which sets both results) sets
     * it to DENY, and that is honored here.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !charmCheck.isCharm(item)) {
            return;
        }
        event.setCancelled(true);
        toggle(event.getPlayer());
    }

    /** Combat marks: any entity-caused damage involving the player,
     *  either direction (projectiles unwrapped to their shooter) —
     *  the same notion the food-heal module gates on. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Player victim) {
            combatMarks.mark(victim.getUniqueId(), now);
        }
        Player damager = asPlayer(event.getDamager());
        if (damager != null) {
            combatMarks.mark(damager.getUniqueId(), now);
        }
        // Belt and braces: the horse is invulnerable by flag, but a
        // marked horse never takes damage even if the flag is lost.
        if (isMountHorse(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private static Player asPlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** Owner-only riding: nobody and nothing else gets on. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!isMountHorse(event.getVehicle())) return;
        UUID owner = ownerOf(event.getVehicle());
        if (!(event.getEntered() instanceof Player rider)
                || !rider.getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }

    /** Owner-only handling: no saddle theft, no foreign inventories. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isMountHorse(event.getRightClicked())) return;
        UUID owner = ownerOf(event.getRightClicked());
        if (!event.getPlayer().getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        if (active.containsKey(id)) {
            despawn(id);
            tell(event.getEntity(), ChatColor.GRAY,
                    "Your horse returned to the charm.");
        }
        combatMarks.clear(id);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        despawn(id);
        combatMarks.clear(id);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // A horse can't follow between worlds; it waits in the charm.
        despawn(event.getPlayer().getUniqueId());
    }

    /** Blocked-world arrivals despawn via the world-change event;
     *  same-world teleports (pearls, wormholes) instead trigger the
     *  follow rule so the horse isn't stranded across the map. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!settings.followTeleport()) return;
        Player player = event.getPlayer();
        Horse horse = active.get(player.getUniqueId());
        if (horse == null || event.getTo() == null) return;
        if (!horse.getWorld().equals(event.getTo().getWorld())) return;
        Location to = event.getTo();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Horse current = active.get(player.getUniqueId());
            if (current == null || !current.isValid()) return;
            if (current.getLocation().distanceSquared(to)
                    > settings.followDistanceSquared()) {
                current.teleport(to);
            }
        });
    }

    /** A death that somehow lands despite invulnerability counts as
     *  a loss (cooldown), never as a normal return. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHorseDeath(EntityDeathEvent event) {
        if (!isMountHorse(event.getEntity())) return;
        UUID owner = ownerOf(event.getEntity());
        if (owner != null && active.remove(owner) != null) {
            cooldownUntilMillis.put(owner,
                    System.currentTimeMillis() + settings.cooldownMillis());
            plugin.getLogger().info("summon-mount: mount horse owned by "
                    + owner + " died despite invulnerability; loss"
                    + " cooldown applied.");
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // ------------------------------------------------------------ helpers

    static void tell(Player player, ChatColor color, String text) {
        player.sendMessage(ChatColor.GOLD + "Mount Charm "
                + ChatColor.DARK_GRAY + "» " + color + text);
    }
}
