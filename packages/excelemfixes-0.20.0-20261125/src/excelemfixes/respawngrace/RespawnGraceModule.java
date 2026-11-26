package excelemfixes.respawngrace;

import com.magmaguy.elitemobs.dungeons.EliteMobsWorld;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.foodheal.WorldGate;
import excelemfixes.shared.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Respawn Grace module (D195 item 3, module home per D198;
 * design revised Nov 22, 2026 — Mike: the first model, an
 * open-ended shield until the player's first hit, was "too
 * strong").
 *
 * <p>The problem: the corpse-run death spiral — a player dies in
 * an EliteMobs world, drops everything where they fell, and gets
 * killed again on the run back through the same mobs (Jester,
 * twice in 5 minutes, Nov 22, 2026).
 *
 * <p>The fuse model: a death in an EM-scope world arms a one-use
 * return ticket and tags every dropped stack with the owner's
 * UUID and a per-death instance id (PDC on the stack, which the
 * dropped Item entity carries). When the player comes back into
 * EM scope (respawn landing in scope, or crossing into scope
 * with the ticket) they take NO damage — until the fuse burns:
 *
 * <ul>
 *   <li><b>Loot fuse</b> — the first pickup of their OWN tagged
 *   death-drop starts a short countdown (default 10 s, seen on
 *   the action bar and heard as a tick each second with a
 *   distinct final tick). Still protected while it runs; at
 *   zero, protection ends with the "Round start!" flourish.
 *   Further pickups never restart it; untagged items, other
 *   players' drops, and loot from an earlier death never start
 *   it.</li>
 *   <li><b>Attack fuse</b> — dealing damage at ANY point (the
 *   Nether precedent, D132 addendum: the moment the player hits
 *   something they can take damage again) ends protection
 *   immediately, cancels any countdown, and fires the same
 *   flourish. The triggering hit itself lands in full.</li>
 *   <li><b>Cap</b> — pre-loot protection cannot be sat on: a
 *   config cap (default 60 s) ends it plainly, no flourish.</li>
 * </ul>
 *
 * <p>Movement does NOT end grace (a corpse run is movement;
 * D132's move-end was for the login case and is deliberately not
 * carried over). No death, no grace: a plain world-hop grants
 * nothing, one ticket buys exactly one grant, quitting clears
 * all state. Self-contained: loot identity is this module's own
 * PDC tags — no dependency on ExcelProtect or any other plugin.
 *
 * <p>World gate: EliteMobs' registry
 * ({@code EliteMobsWorld.isEliteMobsWorld}) OR the config world
 * list — the same scope rule as the sibling modules (food-heal,
 * jobs-dummy-gate). Server-side only — identical for Java and
 * Bedrock; no commands.
 */
public final class RespawnGraceModule implements FixModule, Listener {

    private ModuleContext ctx;
    private BukkitTask task;
    private GraceLedger ledger;
    private long maxMillis;
    private long returnWindowMillis;
    private long countdownMillis;
    private boolean messages;
    private boolean sounds;
    private boolean debug;
    private String tickSound;
    private String finalTickSound;
    private String flourishSound;
    private String titleText;
    private String subtitleText;
    private long titleFadeInTicks;
    private long titleStayTicks;
    private long titleFadeOutTicks;
    private NamespacedKey lootOwnerKey;
    private NamespacedKey lootDeathKey;
    private Set<String> configWorlds = Set.of();
    private final Map<UUID, Integer> lastCountdownSecond = new HashMap<>();
    private boolean eliteMobsPresent;
    private boolean eliteMobsLinkFailed;

    @Override
    public String id() {
        return "respawn-grace";
    }

    @Override
    public String displayName() {
        return "Respawn Grace";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        lootOwnerKey = new NamespacedKey(ctx.plugin(), "respawn_grace_owner");
        lootDeathKey = new NamespacedKey(ctx.plugin(), "respawn_grace_death");
        loadSettings();
        eliteMobsPresent = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        task = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), this::tick, 20L, 20L);
        ctx.logger().info("Respawn Grace enabled: death-return protection in EliteMobs worlds"
                + " (loot countdown " + (countdownMillis / 1000) + "s, pre-loot cap "
                + (maxMillis / 1000) + "s, return window " + (returnWindowMillis / 1000)
                + "s, debug=" + debug + ").");
        if (!eliteMobsPresent) {
            ctx.logger().warning("EliteMobs not detected: world gate runs on the config world list only.");
        }
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        org.bukkit.configuration.ConfigurationSection cfg = ctx.moduleConfig();
        int maxSeconds = RespawnGracePolicy.clampMaxSeconds(cfg == null
                ? RespawnGracePolicy.DEFAULT_MAX_SECONDS
                : cfg.getInt("max-seconds", RespawnGracePolicy.DEFAULT_MAX_SECONDS));
        int windowSeconds = RespawnGracePolicy.clampReturnWindowSeconds(cfg == null
                ? RespawnGracePolicy.DEFAULT_RETURN_WINDOW_SECONDS
                : cfg.getInt("return-window-seconds",
                        RespawnGracePolicy.DEFAULT_RETURN_WINDOW_SECONDS));
        int countdownSeconds = RespawnGracePolicy.clampCountdownSeconds(cfg == null
                ? RespawnGracePolicy.DEFAULT_COUNTDOWN_SECONDS
                : cfg.getInt("countdown-seconds",
                        RespawnGracePolicy.DEFAULT_COUNTDOWN_SECONDS));
        maxMillis = maxSeconds * 1000L;
        returnWindowMillis = windowSeconds * 1000L;
        countdownMillis = countdownSeconds * 1000L;
        messages = cfg == null || cfg.getBoolean("messages", true);
        sounds = cfg == null || cfg.getBoolean("sounds", true);
        debug = cfg != null && cfg.getBoolean("debug", false);
        tickSound = cfg == null ? "minecraft:block.note_block.hat"
                : cfg.getString("tick-sound", "minecraft:block.note_block.hat");
        finalTickSound = cfg == null ? "minecraft:block.note_block.bell"
                : cfg.getString("final-tick-sound", "minecraft:block.note_block.bell");
        flourishSound = cfg == null ? "minecraft:ui.toast.challenge_complete"
                : cfg.getString("flourish-sound", "minecraft:ui.toast.challenge_complete");
        titleText = cfg == null ? "&6Round start!"
                : cfg.getString("title-text", "&6Round start!");
        subtitleText = cfg == null ? "&eFight!"
                : cfg.getString("subtitle-text", "&eFight!");
        titleFadeInTicks = cfg == null ? 5 : Math.max(0, cfg.getLong("title-fade-in-ticks", 5));
        titleStayTicks = cfg == null ? 30 : Math.max(0, cfg.getLong("title-stay-ticks", 30));
        titleFadeOutTicks = cfg == null ? 10 : Math.max(0, cfg.getLong("title-fade-out-ticks", 10));
        configWorlds = cfg == null ? Set.of() : new HashSet<>(cfg.getStringList("worlds"));
        ledger = new GraceLedger();
        lastCountdownSecond.clear();
    }

    // ------------------------------------------------------------ world gate

    private boolean isEmWorld(World world) {
        boolean registrySays = false;
        if (eliteMobsPresent && !eliteMobsLinkFailed) {
            try {
                registrySays = EliteMobsWorld.isEliteMobsWorld(world.getUID());
            } catch (Throwable t) {
                eliteMobsLinkFailed = true;
                ctx.logger().warning("EliteMobs world registry call failed (" + t + "); "
                        + "falling back to the config world list only.");
            }
        }
        return WorldGate.isEliteMobsWorld(registrySays, configWorlds.contains(world.getName()));
    }

    // ------------------------------------------------------------ triggers

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        ledger.endGrace(uuid);
        lastCountdownSecond.remove(uuid);
        if (RespawnGracePolicy.deathArmsTicket(isEmWorld(player.getWorld()))) {
            UUID deathId = UUID.randomUUID();
            int tagged = tagDrops(event, uuid, deathId);
            ledger.armTicket(uuid, deathId, System.currentTimeMillis(), returnWindowMillis);
            debug("ticket armed player=" + player.getName()
                    + " world=" + player.getWorld().getName()
                    + " death=" + deathId + " taggedStacks=" + tagged);
        } else {
            ledger.clearTicket(uuid);
        }
    }

    /** Stamp every dropped stack with owner + death instance id.
     *  The dropped Item entities carry the stack (PDC included),
     *  so the pickup side can prove the loot is the player's own
     *  from THIS death — self-contained, no other plugin's data. */
    private int tagDrops(PlayerDeathEvent event, UUID owner, UUID deathId) {
        int tagged = 0;
        for (ItemStack stack : event.getDrops()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.getPersistentDataContainer().set(
                    lootOwnerKey, PersistentDataType.STRING, owner.toString());
            meta.getPersistentDataContainer().set(
                    lootDeathKey, PersistentDataType.STRING, deathId.toString());
            stack.setItemMeta(meta);
            tagged++;
        }
        return tagged;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        boolean inScope = event.getRespawnLocation() != null
                && event.getRespawnLocation().getWorld() != null
                && isEmWorld(event.getRespawnLocation().getWorld());
        if (RespawnGracePolicy.grantOnRespawn(inScope)
                && ledger.hasTicket(player.getUniqueId(), now)) {
            tryGrant(player, now, "respawn");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        boolean fromScope = isEmWorld(event.getFrom());
        boolean toScope = isEmWorld(player.getWorld());
        if (RespawnGracePolicy.grantOnWorldChange(fromScope, toScope)
                && ledger.hasTicket(uuid, now)) {
            tryGrant(player, now, "world-return");
            return;
        }
        if (RespawnGracePolicy.endOnWorldChange(fromScope, toScope)
                && ledger.isProtected(uuid, now)) {
            GraceLedger.Phase ended = ledger.endGrace(uuid);
            lastCountdownSecond.remove(uuid);
            debug("grace ended player=" + player.getName()
                    + " phase=" + ended + " (left EliteMobs scope)");
            if (messages) {
                player.sendActionBar(TextUtil.component(
                        "&eRespawn protection ended — you left the EliteMobs area."));
            }
        }
    }

    private void tryGrant(Player player, long now, String via) {
        if (ledger.grantFromTicket(player.getUniqueId(), now, maxMillis)) {
            debug("grace granted player=" + player.getName() + " via=" + via
                    + " cap=" + (maxMillis / 1000) + "s");
            if (messages) {
                player.sendMessage(TextUtil.component(
                        "&aRespawn protection: &fyou can't be hurt while you recover your loot. "
                                + "Picking up your loot starts a " + (countdownMillis / 1000)
                                + "-second countdown; attacking ends it now."));
                player.sendActionBar(TextUtil.component(
                        "&aProtected &7— recover your loot to start the countdown"));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ledger.clear(uuid);
        lastCountdownSecond.remove(uuid);
    }

    // ------------------------------------------------------------ loot fuse

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (ledger.phase(uuid, now) != GraceLedger.Phase.PRELOOT) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        ItemMeta meta = stack == null ? null : stack.getItemMeta();
        if (meta == null) {
            return;
        }
        UUID tagOwner = parseUuid(meta.getPersistentDataContainer()
                .get(lootOwnerKey, PersistentDataType.STRING));
        UUID tagDeath = parseUuid(meta.getPersistentDataContainer()
                .get(lootDeathKey, PersistentDataType.STRING));
        if (!RespawnGracePolicy.isOwnDeathLoot(tagOwner, tagDeath, uuid, ledger.deathId(uuid))) {
            return;
        }
        if (ledger.startCountdown(uuid, now, countdownMillis)) {
            debug("countdown started player=" + player.getName()
                    + " seconds=" + (countdownMillis / 1000));
            if (messages) {
                player.sendActionBar(TextUtil.component(
                        "&eLoot recovered — protection ends in "
                                + (countdownMillis / 1000) + "s!"));
            }
            if (sounds) {
                playSound(player, tickSound, 1.0f);
            }
            lastCountdownSecond.put(uuid, (int) (countdownMillis / 1000));
        }
    }

    private static UUID parseUuid(String text) {
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------ protection

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (RespawnGracePolicy.damageBlocked(
                ledger.damageBlocked(victim.getUniqueId(), now),
                isEmWorld(victim.getWorld()))) {
            event.setCancelled(true);
            debug("blocked " + event.getCause() + " damage to " + victim.getName()
                    + " phase=" + ledger.phase(victim.getUniqueId(), now));
        }
    }

    // ------------------------------------------------------------ attack fuse

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null) {
            return;
        }
        // The attack itself is NOT cancelled: it lands, and from
        // this moment the attacker can take damage again (Nether
        // precedent).
        endByAttack(attacker, "attack");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }
        endByAttack(shooter, "bow shot");
    }

    private void endByAttack(Player player, String how) {
        GraceLedger.Phase ended = ledger.onAttack(
                player.getUniqueId(), System.currentTimeMillis());
        if (ended == GraceLedger.Phase.NONE) {
            return;
        }
        lastCountdownSecond.remove(player.getUniqueId());
        debug("grace ended player=" + player.getName() + " phase=" + ended
                + " (" + how + ") — flourish");
        flourish(player);
    }

    /** Resolve the attacking player: direct melee, or a projectile
     * whose shooter is a player (arrow, trident, snowball, ...). */
    private static Player attackerOf(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    // ------------------------------------------------------------ flourish

    /** The Mortal Kombat play-on-words: protection is over, the
     *  round has started. Title + subtitle + sound sting, all
     *  config-driven. */
    private void flourish(Player player) {
        if (messages) {
            player.showTitle(Title.title(
                    TextUtil.component(titleText),
                    TextUtil.component(subtitleText),
                    Title.Times.times(
                            Duration.ofMillis(titleFadeInTicks * 50),
                            Duration.ofMillis(titleStayTicks * 50),
                            Duration.ofMillis(titleFadeOutTicks * 50))));
        }
        if (sounds) {
            playSound(player, flourishSound, 1.0f);
        }
    }

    private void playSound(Player player, String sound, float pitch) {
        if (sound == null || sound.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        } catch (Throwable t) {
            debug("sound failed name=" + sound + " (" + t + ")");
        }
    }

    // ------------------------------------------------------------ sweep

    private void tick() {
        long now = System.currentTimeMillis();
        for (GraceLedger.Expiry expiry : ledger.purgeExpired(now)) {
            Player player = Bukkit.getPlayer(expiry.playerId());
            lastCountdownSecond.remove(expiry.playerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (expiry.kind() == GraceLedger.EndKind.COUNTDOWN_EXPIRED) {
                debug("countdown expired player=" + player.getName() + " — flourish");
                flourish(player);
            } else {
                debug("grace cap expired player=" + player.getName());
                if (messages) {
                    player.sendMessage(TextUtil.component(
                            "&eYour respawn protection has ended."));
                }
            }
        }
        if (!messages && !sounds) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            GraceLedger.Phase phase = ledger.phase(uuid, now);
            if (phase == GraceLedger.Phase.COUNTDOWN) {
                int seconds = (int) ((ledger.millisRemaining(uuid, now) + 999) / 1000);
                Integer last = lastCountdownSecond.get(uuid);
                if (last == null || last != seconds) {
                    lastCountdownSecond.put(uuid, seconds);
                    if (messages) {
                        player.sendActionBar(TextUtil.component(
                                "&eProtection ends in " + seconds + "s"));
                    }
                    if (sounds) {
                        playSound(player, seconds <= 1 ? finalTickSound : tickSound,
                                seconds <= 1 ? 1.2f : 1.0f);
                    }
                }
            } else if (phase == GraceLedger.Phase.PRELOOT && messages) {
                player.sendActionBar(TextUtil.component(
                        "&aProtected &7— recover your loot to start the countdown"));
            }
        }
    }

    private void debug(String message) {
        if (debug) ctx.logger().info("RESPAWNGRACE " + message);
    }

    // ------------------------------------------------------------ rig hooks

    /** Test/rig hook: is this player currently protected? */
    public boolean gracedForTest(UUID uuid) {
        return ledger != null && ledger.isProtected(uuid, System.currentTimeMillis());
    }

    /** Test/rig hook: does this player hold a live return ticket? */
    public boolean ticketForTest(UUID uuid) {
        return ledger != null && ledger.hasTicket(uuid, System.currentTimeMillis());
    }

    /** Test/rig hook: the player's current phase name. */
    public String phaseForTest(UUID uuid) {
        return ledger == null ? "NOLEDGER"
                : ledger.phase(uuid, System.currentTimeMillis()).name();
    }

    /** Test/rig hook: milliseconds left in the current phase. */
    public long remainingForTest(UUID uuid) {
        return ledger == null ? -1
                : ledger.millisRemaining(uuid, System.currentTimeMillis());
    }
}
