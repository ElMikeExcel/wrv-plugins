package excelemfixes.mount;

import excelemfixes.mount.core.WorldRules;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
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
 * The ghast lifecycle: the flying tier of the summon-mount
 * module (D204) — one active ghast per player, summoned and
 * dismissed by the Ghast Charm (ExcelItems registry ID 9),
 * under the same discipline as the horse (PDC marking,
 * owner-only, invulnerable, non-persistent, combat gate,
 * world gate, loss cooldown, every despawn trigger). The
 * horse's service is the proven pattern; this class mirrors
 * it deliberately rather than generalizing it, so the horse's
 * behavior cannot change under the ghast's weight.
 *
 * <p>The mount entity is a happy ghast — the rideable,
 * harness-wearing ghast variant modern Minecraft ships and
 * the EliteMobs content set already uses in the overworld.
 * The pack compiles against an older Paper API that has no
 * happy-ghast types, so the entity CLASS is resolved at
 * RUNTIME from config ({@code ghast.entity-type}, default
 * HAPPY_GHAST); everything else is plain API — the harness
 * rides the vanilla BODY equipment slot (a happy ghast
 * cannot be ridden or steered without one, and the equip is
 * verified by read-back), flying speed via its attribute
 * when the config asks for one. Every step fails closed:
 * a ghast that cannot be made rideable is never left standing
 * as a broken product — the summon reports failure and the
 * charm is unconsumed, exactly like a cancelled horse spawn.
 *
 * <p>Flight itself is vanilla: a harnessed happy ghast is
 * steered by its rider with the client's own controls, so
 * there is deliberately no plugin steering code here.
 *
 * <p>The spawn uses the CUSTOM spawn reason, the exemption
 * EliteMobs' InstanceProtector honors in its worlds — the
 * same reason the horse rides on, rig-proven against the
 * real EliteMobs.
 */
public final class GhastMountService implements Listener {

    private final JavaPlugin plugin;
    private final CharmCheck charmCheck;
    private final CombatMarks combatMarks;
    private volatile MountSettings settings;
    private volatile GhastSettings ghastSettings;
    private final NamespacedKey ghastKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, Entity> active = new HashMap<>();
    private final Map<UUID, Long> cooldownUntilMillis = new HashMap<>();
    private BukkitTask sweepTask;
    private boolean harnessPathLogged;

    GhastMountService(JavaPlugin plugin, MountSettings settings,
                      GhastSettings ghastSettings, CharmCheck charmCheck,
                      CombatMarks combatMarks) {
        this.plugin = plugin;
        this.settings = settings;
        this.ghastSettings = ghastSettings;
        this.charmCheck = charmCheck;
        this.combatMarks = combatMarks;
        this.ghastKey = new NamespacedKey(plugin, "ghast");
        this.ownerKey = new NamespacedKey(plugin, "owner");
    }

    /** Swap in reloaded settings (module reload). */
    void updateSettings(MountSettings newSettings,
                        GhastSettings newGhastSettings) {
        this.settings = newSettings;
        this.ghastSettings = newGhastSettings;
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

    /** Remove every active ghast (module disable / retirement flip). */
    public void despawnAll() {
        for (UUID owner : java.util.List.copyOf(active.keySet())) {
            despawn(owner);
        }
    }

    /** Crash-orphan cleanup: any entity carrying our mark that
     *  this service has no record for is removed. Ghasts are
     *  spawned non-persistent, so there should never be one —
     *  the same cheap insurance the horse carries. */
    void sweepOrphans() {
        int removed = 0;
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isMountGhast(entity) && !active.containsValue(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("summon-mount: removed " + removed
                    + " orphaned mount ghast(s) at enable.");
        }
    }

    /** Periodic validity check: a ghast that vanished without a
     *  despawn trigger starts the loss cooldown instead of
     *  leaving a stuck "already out" record. */
    private void sweep() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Entity> entry
                : java.util.List.copyOf(active.entrySet())) {
            Entity ghast = entry.getValue();
            if (ghast == null || ghast.isDead() || !ghast.isValid()) {
                active.remove(entry.getKey());
                cooldownUntilMillis.put(entry.getKey(),
                        now + ghastSettings.cooldownMillis());
                Player owner = plugin.getServer().getPlayer(entry.getKey());
                if (owner != null) {
                    tell(owner, ChatColor.YELLOW,
                            "Your ghast was lost. The charm needs "
                                    + ghastSettings.cooldownSeconds()
                                    + "s before it can call another.");
                }
                plugin.getLogger().info("summon-mount: mount ghast for "
                        + entry.getKey() + " vanished outside a despawn"
                        + " trigger; loss cooldown applied.");
            }
        }
    }

    // ------------------------------------------------------------ the toggle

    /** The one code path behind the charm, /ghastcharm summon
     *  and /ghastcharm dismiss. Sends the player their feedback.
     *  Returns the horse service's result vocabulary — one set
     *  of outcomes for the whole module. */
    public MountService.ToggleResult toggle(Player player) {
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) {
            despawn(id);
            tell(player, ChatColor.GRAY, "Your ghast returns to the charm.");
            return MountService.ToggleResult.DESPAWNED;
        }
        if (settings.retired()) {
            tell(player, ChatColor.YELLOW, settings.retirementMessage());
            return MountService.ToggleResult.DISABLED;
        }
        if (WorldRules.isBlocked(player.getWorld().getName(),
                settings.blockedPrefixes(), settings.blockedNames())) {
            tell(player, ChatColor.RED,
                    "You can't call your ghast here.");
            return MountService.ToggleResult.BLOCKED_WORLD;
        }
        long now = System.currentTimeMillis();
        if (combatMarks.inCombat(id, now, settings.combatWindowMillis())) {
            tell(player, ChatColor.RED, "You can't call your ghast in"
                    + " combat. Give it "
                    + combatMarks.remainingSeconds(id, now,
                            settings.combatWindowMillis())
                    + "s.");
            return MountService.ToggleResult.IN_COMBAT;
        }
        Long cooldownUntil = cooldownUntilMillis.get(id);
        if (cooldownUntil != null && now < cooldownUntil) {
            tell(player, ChatColor.RED, "The charm is resting. "
                    + ((cooldownUntil - now + 999) / 1000)
                    + "s left.");
            return MountService.ToggleResult.COOLDOWN;
        }
        if (player.getVehicle() != null) {
            tell(player, ChatColor.RED, "Dismount first.");
            return MountService.ToggleResult.RIDING_OTHER;
        }
        return summon(player) ? MountService.ToggleResult.SUMMONED
                : MountService.ToggleResult.SPAWN_FAILED;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean summon(Player player) {
        UUID id = player.getUniqueId();
        EntityType type;
        try {
            type = EntityType.valueOf(ghastSettings.entityType());
        } catch (IllegalArgumentException bad) {
            plugin.getLogger().severe("summon-mount: ghast entity type '"
                    + ghastSettings.entityType() + "' does not exist on"
                    + " this server version — the Ghast Charm cannot"
                    + " summon. (The pack needs a server with the"
                    + " rideable ghast; check ghast.entity-type.)");
            tell(player, ChatColor.RED, "Your ghast couldn't come here.");
            return false;
        }
        Class<? extends Entity> clazz = type.getEntityClass();
        if (clazz == null) {
            plugin.getLogger().severe("summon-mount: entity type "
                    + type + " has no entity class on this server"
                    + " version — the Ghast Charm cannot summon.");
            tell(player, ChatColor.RED, "Your ghast couldn't come here.");
            return false;
        }
        // The ghast is a big entity (a four-block body): call it
        // in above the player's head so it never materializes
        // inside the ground or a ceiling block.
        Location loc = player.getLocation().add(
                0, ghastSettings.spawnHeightOffset(), 0);
        Entity ghast = player.getWorld().spawn(loc, (Class) clazz,
                CreatureSpawnEvent.SpawnReason.CUSTOM, false, e -> {
                    Entity entity = (Entity) e;
                    entity.setInvulnerable(true);
                    entity.setPersistent(false);
                    if (entity instanceof Mob mob) {
                        mob.setRemoveWhenFarAway(false);
                    }
                    if (entity instanceof Ageable ageable) {
                        ageable.setAdult();
                    }
                    entity.setCustomName(player.getName() + "'s Ghast");
                    entity.setCustomNameVisible(true);
                    entity.getPersistentDataContainer().set(ghastKey,
                            PersistentDataType.BYTE, (byte) 1);
                    entity.getPersistentDataContainer().set(ownerKey,
                            PersistentDataType.STRING, id.toString());
                });
        if (ghast == null || !ghast.isValid()) {
            // Another plugin cancelled the spawn outright.
            tell(player, ChatColor.RED,
                    "Your ghast couldn't come here.");
            plugin.getLogger().warning("summon-mount: ghast spawn"
                    + " cancelled for " + player.getName() + " in "
                    + player.getWorld().getName() + ".");
            return false;
        }
        // The harness is what makes the ghast rideable at all;
        // a ghast we could not dress is removed again rather
        // than left as scenery the player paid to fly.
        if (!equipHarness(ghast)) {
            ghast.remove();
            tell(player, ChatColor.RED,
                    "Your ghast couldn't come here.");
            return false;
        }
        applyFlyingSpeed(ghast);
        active.put(id, ghast);
        tell(player, ChatColor.GREEN, "Your ghast is here.");
        return true;
    }

    /** Dress the ghast with its harness. Vanilla wears the
     *  harness in the entity's BODY equipment slot (the
     *  harness items are body-equippable, the same mechanism
     *  as wolf armor), and a happy ghast can only be ridden
     *  and steered while one is worn — so the equip is
     *  verified by reading the slot back, and any failure
     *  refuses the summon rather than leaving a grounded
     *  mount standing. Plain Bukkit equipment API: the
     *  harness needs no runtime-only surface at all. */
    private boolean equipHarness(Entity ghast) {
        Material material;
        try {
            material = Material.valueOf(ghastSettings.harnessMaterial());
        } catch (IllegalArgumentException bad) {
            plugin.getLogger().severe("summon-mount: harness material '"
                    + ghastSettings.harnessMaterial() + "' does not"
                    + " exist on this server version — ghast summons"
                    + " are refused until ghast.harness-material names"
                    + " a real harness.");
            return false;
        }
        if (!(ghast instanceof LivingEntity living)) {
            plugin.getLogger().severe("summon-mount: ghast entity is"
                    + " not a living entity — cannot equip its harness.");
            return false;
        }
        org.bukkit.inventory.EntityEquipment equipment =
                living.getEquipment();
        if (equipment == null) {
            plugin.getLogger().severe("summon-mount: ghast entity"
                    + " reports no equipment — cannot equip its"
                    + " harness.");
            return false;
        }
        equipment.setItem(EquipmentSlot.BODY, new ItemStack(material));
        ItemStack readBack = equipment.getItem(EquipmentSlot.BODY);
        if (readBack == null || readBack.getType() != material) {
            plugin.getLogger().severe("summon-mount: harness read-back"
                    + " from the body slot is "
                    + (readBack == null ? "empty" : readBack.getType().name())
                    + " after equipping " + material + " — refusing"
                    + " the summon.");
            return false;
        }
        if (!harnessPathLogged) {
            harnessPathLogged = true;
            plugin.getLogger().info("summon-mount: ghast harness"
                    + " equipped in the body equipment slot ("
                    + material + ").");
        }
        return true;
    }

    /** Optional flying-speed override (config 0 = vanilla),
     *  guarded like every runtime seam in this service: a
     *  server without the attribute simply keeps vanilla. */
    private void applyFlyingSpeed(Entity ghast) {
        double speed = ghastSettings.flyingSpeed();
        if (speed <= 0 || !(ghast instanceof LivingEntity living)) return;
        try {
            AttributeInstance instance =
                    living.getAttribute(Attribute.FLYING_SPEED);
            if (instance != null) instance.setBaseValue(speed);
        } catch (Throwable ignored) {
            // Older server without the attribute: vanilla stands.
        }
    }

    /** Despawn one player's ghast (no cooldown — a normal return). */
    public void despawn(UUID owner) {
        Entity ghast = active.remove(owner);
        if (ghast != null) {
            ghast.eject();
            ghast.remove();
        }
    }

    // ------------------------------------------------------------ queries

    public boolean hasActiveGhast(UUID owner) {
        return active.containsKey(owner);
    }

    public Entity activeGhast(UUID owner) {
        return active.get(owner);
    }

    public boolean isMountGhast(Entity entity) {
        return entity != null && entity.getPersistentDataContainer()
                .has(ghastKey, PersistentDataType.BYTE);
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

    public long cooldownRemainingSeconds(UUID player, long nowMillis) {
        Long until = cooldownUntilMillis.get(player);
        if (until == null || nowMillis >= until) return 0;
        return (until - nowMillis + 999) / 1000;
    }

    // ------------------------------------------------------------ events

    /** The charm's one gesture — the same discipline as the
     *  horse's handler (see MountService.onInteract for why
     *  this is deliberately NOT ignoreCancelled). */
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

    /** Combat marks feed the SHARED marks (one combat state
     *  per player across both charms), and a marked ghast
     *  never takes entity damage even if its invulnerability
     *  flag is somehow lost. */
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
        if (isMountGhast(event.getEntity())) {
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
        if (!isMountGhast(event.getVehicle())) return;
        UUID owner = ownerOf(event.getVehicle());
        if (!(event.getEntered() instanceof Player rider)
                || !rider.getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }

    /** Owner-only handling: no foreign inventories, no leashing. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isMountGhast(event.getRightClicked())) return;
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
                    "Your ghast returned to the charm.");
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
        // A ghast can't follow between worlds; it waits in the charm.
        despawn(event.getPlayer().getUniqueId());
    }

    /** Same-world teleports (pearls, wormholes) trigger the
     *  follow rule so the ghast isn't stranded across the map;
     *  it re-forms above the player, as it arrived. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!settings.followTeleport()) return;
        Player player = event.getPlayer();
        Entity ghast = active.get(player.getUniqueId());
        if (ghast == null || event.getTo() == null) return;
        if (!ghast.getWorld().equals(event.getTo().getWorld())) return;
        Location to = event.getTo().clone()
                .add(0, ghastSettings.spawnHeightOffset(), 0);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Entity current = active.get(player.getUniqueId());
            if (current == null || !current.isValid()) return;
            if (current.getPassengers().contains(player)) return;
            if (current.getLocation().distanceSquared(to)
                    > settings.followDistanceSquared()) {
                current.teleport(to);
            }
        });
    }

    /** A death that somehow lands despite invulnerability
     *  counts as a loss (cooldown), never as a normal return. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGhastDeath(EntityDeathEvent event) {
        if (!isMountGhast(event.getEntity())) return;
        UUID owner = ownerOf(event.getEntity());
        if (owner != null && active.remove(owner) != null) {
            cooldownUntilMillis.put(owner,
                    System.currentTimeMillis()
                            + ghastSettings.cooldownMillis());
            plugin.getLogger().info("summon-mount: mount ghast owned by "
                    + owner + " died despite invulnerability; loss"
                    + " cooldown applied.");
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // ------------------------------------------------------------ helpers

    static void tell(Player player, ChatColor color, String text) {
        player.sendMessage(ChatColor.GOLD + "Ghast Charm "
                + ChatColor.DARK_GRAY + "» " + color + text);
    }
}
