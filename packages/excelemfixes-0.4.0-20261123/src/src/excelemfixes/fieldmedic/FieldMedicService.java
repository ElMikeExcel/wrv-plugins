package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import excelemfixes.ModuleContext;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/**
 * The Field Medic module's Bukkit layer: the bandage-use listener,
 * the per-second heal delivery, and the cooldown's persistent-data
 * mirror. Loaded only when ExcelItems is present (see
 * {@link FieldMedicModule}); every ExcelItems touch goes through
 * {@link BandageItems} or the API handle obtained here.
 *
 * <p>Use shape: right-click (air or block, main hand) with the
 * Field Bandage held — one gesture, identical on Java and Bedrock,
 * because item interaction is the one input both platforms share.
 * A refused use (heal already running, or cooldown) consumes
 * nothing and tells the player why on the action bar.
 *
 * <p>Delivery goes through {@link EntityRegainHealthEvent} (reason
 * CUSTOM), mirroring EliteMobs' own ACS trickle and the food-heal
 * module's fixed deliver(): the event is fired with the amount
 * clamped to missing health, cancellation and listener-adjusted
 * amounts are honored, and only the surviving amount is applied.
 * Healing by raw setHealth alone made the food-heal module's heals
 * invisible to the server's heal accounting — ExcelProtect records
 * only regain events — which is how that module's delivery was
 * misread as missing (food-heal stacking investigation,
 * Nov 22, 2026). This module never heals around the event.
 */
public final class FieldMedicService implements Listener {

    private final ModuleContext ctx;
    private final ExcelItemsAPI items;
    private final NamespacedKey cooldownKey;
    private FieldMedicEngine engine;
    private boolean debug;
    private BukkitTask task;

    FieldMedicService(ModuleContext ctx) {
        this.ctx = ctx;
        // Throws IllegalStateException when ExcelItems is present as
        // a plugin but its API service never came up (e.g. it
        // disabled itself at its own enable). The module catches it
        // and stands down with its named-dependency log line.
        this.items = ExcelItemsAPI.get();
        this.cooldownKey = new NamespacedKey(ctx.plugin(), "field_medic_cooldown_until");
        loadSettings();
    }

    void start() {
        BandageItems.Registration registration = BandageItems.register(items);
        switch (registration) {
            case CLASH -> throw new IllegalStateException(
                    "ExcelItems registry id " + BandageItem.REGISTRY_ID + " (or key '"
                            + BandageItem.KEY + "') is already claimed by another item; "
                            + "the Field Bandage keeps its permanent id and will not re-point it");
            case REGISTERED -> ctx.logger().info("Field Medic: Field Bandage registered with "
                    + "ExcelItems as id " + BandageItem.REGISTRY_ID + " (" + BandageItem.KEY + ").");
            case ALREADY_OURS -> ctx.logger().info("Field Medic: Field Bandage already "
                    + "registered as ExcelItems id " + BandageItem.REGISTRY_ID + " (reload).");
        }
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        task = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), this::tick, 20L, 20L);
        FieldMedicEngine.Settings s = engine.settings();
        ctx.logger().info("Field Medic ready: bandage heals " + fmt(s.healPercent())
                + "% of max health over " + s.durationSeconds() + "s, cooldown "
                + (s.cooldownMillis() / 1000L) + "s (debug=" + debug + ").");
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
    }

    void reload() {
        loadSettings();
        ctx.logger().info("Field Medic reloaded (in-flight bandage heals and cooldowns reset).");
    }

    private void loadSettings() {
        ConfigurationSection cfg = ctx.moduleConfig();
        debug = cfg != null && cfg.getBoolean("debug", false);
        double healPercent = cfg == null ? 35.0 : cfg.getDouble("heal-percent", 35.0);
        int durationSeconds = cfg == null ? 5 : cfg.getInt("duration-seconds", 5);
        long cooldownSeconds = cfg == null ? 60L : cfg.getLong("cooldown-seconds", 60L);
        if (!(healPercent > 0.0) || healPercent > 100.0) healPercent = 35.0;
        if (durationSeconds < 1 || durationSeconds > 60) durationSeconds = 5;
        if (cooldownSeconds < 0 || cooldownSeconds > 3600) cooldownSeconds = 60L;
        engine = new FieldMedicEngine(new FieldMedicEngine.Settings(
                healPercent, durationSeconds, cooldownSeconds * 1000L));
    }

    // ------------------------------------------------------------ events

    // ignoreCancelled stays FALSE on purpose: a right-click-AIR
    // interact event reports isCancelled() from birth (there is no
    // block, so useInteractedBlock defaults to DENY), and an
    // ignoreCancelled handler would never see a bandage used on
    // open air — the normal way to use one. The gate that carries
    // the real meaning is useItemInHand: an explicit DENY (which is
    // also what a full cancel sets) means some other plugin has
    // forbidden using the held item here, and the bandage respects
    // it exactly like a cancellation.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!BandageItems.isBandage(items, event.getItem())) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        rehydrateCooldown(player, now);
        FieldMedicEngine.UseOutcome outcome = engine.tryUse(uuid, now);
        switch (outcome.result()) {
            case STARTED -> {
                event.setCancelled(true);
                consumeOneFromMainHand(player);
                player.getPersistentDataContainer().set(cooldownKey,
                        PersistentDataType.LONG, engine.cooldownUntilMillis(uuid));
                player.sendActionBar(Component.text(
                        "Bandaging — healing over "
                                + engine.settings().durationSeconds() + " seconds.",
                        NamedTextColor.GREEN));
                debug("use player=" + player.getName()
                        + " heal=" + fmt(engine.settings().healPercent()) + "%"
                        + " cooldown-until=" + engine.cooldownUntilMillis(uuid));
            }
            case REFUSED_ACTIVE -> {
                player.sendActionBar(Component.text(
                        "Already bandaging — one bandage at a time.", NamedTextColor.RED));
                debug("refuse-active player=" + player.getName());
            }
            case REFUSED_COOLDOWN -> {
                long seconds = (outcome.cooldownRemainingMillis() + 999L) / 1000L;
                player.sendActionBar(Component.text(
                        "Bandage ready in " + seconds + "s.", NamedTextColor.RED));
                debug("refuse-cooldown player=" + player.getName()
                        + " remaining=" + seconds + "s");
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rehydrateCooldown(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        engine.forget(player.getUniqueId());
        player.getPersistentDataContainer().remove(cooldownKey);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // The heal is a benefit in progress: relogging forfeits it.
        // The cooldown is a restriction: it stands (engine + the
        // persistent-data mirror written at use).
        engine.endHeal(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ tick + delivery

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID uuid : engine.bandagingPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.isDead()) continue;
            double percent = engine.tick(uuid, now);
            if (percent <= 0) continue;
            double deliveredHp = deliver(player, percent);
            if (deliveredHp > 0) {
                debug("deliver player=" + player.getName()
                        + " hp=" + fmt(deliveredHp)
                        + " health=" + fmt(player.getHealth())
                        + "/" + fmt(maxHealth(player)));
            }
            if (!engine.isActive(uuid, now)) {
                debug("complete player=" + player.getName());
            }
        }
    }

    /**
     * Heal the player by percent of their current max health,
     * clamped to missing health, THROUGH EntityRegainHealthEvent
     * (reason CUSTOM) — see the class javadoc for why the event is
     * the delivery, not an afterthought. Returns HP actually
     * delivered (0 at full health, while dead, or when a listener
     * cancels the event).
     */
    private double deliver(Player player, double percent) {
        if (percent <= 0 || player.isDead()) return 0.0;
        double maxHp = maxHealth(player);
        if (maxHp <= 0) return 0.0;
        double missing = maxHp - player.getHealth();
        if (missing <= 0) return 0.0;
        double amount = Math.min(percent / 100.0 * maxHp, missing);
        if (amount <= 0) return 0.0;
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(
                player, amount, EntityRegainHealthEvent.RegainReason.CUSTOM);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return 0.0;
        double healed = Math.min(event.getAmount(), maxHp - player.getHealth());
        if (!Double.isFinite(healed) || healed <= 0) return 0.0;
        player.setHealth(player.getHealth() + healed);
        return healed;
    }

    // ------------------------------------------------------------ helpers

    /**
     * Bring a stored cooldown expiry (written to the player's
     * persistent data at use) back into the engine. The mirror is
     * what makes the cooldown survive a relog; an expired stored
     * value is cleaned up instead of carried forever.
     */
    private void rehydrateCooldown(Player player, long now) {
        Long until = player.getPersistentDataContainer()
                .get(cooldownKey, PersistentDataType.LONG);
        if (until == null) return;
        if (until > now) {
            engine.restoreCooldown(player.getUniqueId(), until);
        } else {
            player.getPersistentDataContainer().remove(cooldownKey);
        }
    }

    private void consumeOneFromMainHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    private double maxHealth(Player player) {
        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 0.0 : attribute.getValue();
    }

    private void debug(String message) {
        if (debug) ctx.logger().info("FIELDMEDIC " + message);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
