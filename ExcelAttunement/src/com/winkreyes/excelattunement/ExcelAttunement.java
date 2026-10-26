package com.winkreyes.excelattunement;

import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.DamageRule;
import com.winkreyes.excelattunement.core.LockFeedback;
import com.winkreyes.excelattunement.core.LockRule;
import com.winkreyes.excelattunement.core.ProjectileTracker;
import com.winkreyes.excelattunement.core.SpellClassifier;
import com.winkreyes.excelattunement.core.Stance;
import com.winkreyes.excelattunement.core.StanceText;
import com.winkreyes.excelattunement.core.WeaponKind;
import com.winkreyes.excelattunement.core.Weapons;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * ExcelAttunement — Combat Attunement (Mike's design, approved
 * Oct 19, 2026). An EFFECT lock, plus (0.1.4, Mike Oct 24, 2026) a
 * main-hand LOCK on top of it:
 *  - land a hit with a martial weapon -> MARTIAL stance (wand casts
 *    are blocked at Magic's PreCastEvent, see MagicBridge);
 *  - cast a combat spell, or a support spell while in combat ->
 *    CASTER stance (that player's martial-weapon damage is zeroed);
 *  - 30s window (config), refreshed by stance interactions; taking
 *    hostile damage refreshes the active stance;
 *  - utility spells never attune; a support cast out of combat never
 *    attunes (the accepted pre-fight opener).
 *  - 0.1.4: while CASTER a martial weapon cannot be moved into the
 *    main hand (hotbar switch, F swap, inventory click/drag, pickup
 *    into the held slot); while MARTIAL a Magic wand cannot. The
 *    effect locks above stay as the backstop. Live action-bar
 *    countdown, a dull sound and a once-per-stance title explain it.
 *  - 0.1.5 guild ward (Mike, Oct 24, 2026): in the EliteMobs worlds
 *    every Magic cast is blocked (heals too), the wand stays a plain
 *    item, carried-in short Magic buffs are stripped, Magic teleports
 *    into those worlds are refused, and attunement is ignored there
 *    entirely (stance and weapon lock clear on arrival). See WorldWard.
 *  - 0.1.6 arena locks (Mike, Oct 24, 2026): Magic movement spells (glide,
 *    fling incl. leap, hulkleap, lift, blink) are sealed inside the End
 *    main island zone while an Ender Dragon is alive there (first fight,
 *    re-fights and re-summons alike); Lift also cannot target a player
 *    inside that zone. Config rules. See ArenaLocks.
 *  - 0.1.7 scroll fix (Mike, Oct 26, 2026): the stance lock never walls
 *    off a hotbar slot. A one-slot scroll onto a locked item hops over
 *    it to the next unlocked slot; number keys and taps onto it are
 *    still refused. See HotbarSkip.
 * State is in-memory only: a restart clears every stance, by design.
 * This class contains NO Magic imports — the Magic surface lives in
 * MagicBridge, registered only when Magic is enabled.
 */
public final class ExcelAttunement extends JavaPlugin implements Listener {
    private boolean enabledMaster;
    private long windowSeconds;
    private boolean debug;
    private SpellClassifier classifier;
    private final Map<UUID, AttunementState> states = new HashMap<>();
    private final ProjectileTracker projectiles = new ProjectileTracker();
    private final java.util.Set<String> warnedUnknownSpells = new java.util.HashSet<>();
    private final Map<UUID, LockFeedback> feedback = new HashMap<>();
    private boolean weaponLock;
    private boolean wandLock;
    private boolean actionbar;
    private boolean actionbarLive;
    private boolean sounds;
    // Set by MagicBridge when Magic is enabled; without Magic nothing is a wand.
    private Predicate<ItemStack> wandTest = item -> false;
    private Predicate<ItemStack> magicItemTest = item -> false;
    private WorldWard ward;
    private ArenaLocks arenaLocks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        enabledMaster = getConfig().getBoolean("enabled", true);
        windowSeconds = getConfig().getLong("window-seconds", 30L);
        long combatContextSeconds = getConfig().getLong("combat-context-seconds", 10L);
        debug = getConfig().getBoolean("debug", false);
        weaponLock = getConfig().getBoolean("weapon-lock", true);
        wandLock = getConfig().getBoolean("wand-lock", true);
        actionbar = getConfig().getBoolean("actionbar", true);
        actionbarLive = getConfig().getBoolean("actionbar-live", true);
        sounds = getConfig().getBoolean("sounds", true);
        classifier = loadClassifier();
        ward = new WorldWard(this, getConfig().getConfigurationSection("magic-ward"));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(ward, this);
        // isSet ignores the jar's embedded defaults: a live config without the section must get
        // the shipped default rules, not an empty section created from the defaults.
        arenaLocks = new ArenaLocks(this, getConfig().isSet("arena-locks")
                ? getConfig().getConfigurationSection("arena-locks") : null);
        getServer().getPluginManager().registerEvents(arenaLocks, this);
        Bukkit.getScheduler().runTaskTimer(this, arenaLocks::tick, 20L, arenaLocks.checkTicks());
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        if (getServer().getPluginManager().isPluginEnabled("Magic")) {
            getServer().getPluginManager().registerEvents(
                    new MagicBridge(this, classifier), this);
            getLogger().info("Magic found: cast interception ON (PreCastEvent block + CastEvent attunement).");
        } else {
            getLogger().warning("Magic NOT found: cast interception OFF. "
                    + "Martial stance will still track, but nothing can set caster stance "
                    + "and no cast can be blocked. Install alongside Magic 11.2.5.");
        }
        getLogger().info("ExcelAttunement enabled: " + (enabledMaster ? "ON" : "OFF (config)")
                + ", window " + windowSeconds + "s, combat context " + combatContextSeconds
                + "s, spell table " + classifier.size() + " spells.");
        getLogger().info("Stance lock: weapon-lock " + onOff(weaponLock) + ", wand-lock " + onOff(wandLock)
                + ", actionbar " + onOff(actionbar) + (actionbar ? " (live " + onOff(actionbarLive) + ")" : "")
                + ", sounds " + onOff(sounds) + ".");
        getLogger().info("Guild ward (Magic off in the EliteMobs worlds): " + onOff(ward.enabled()) + ".");
        getLogger().info("Arena locks: " + onOff(arenaLocks.enabled()) + " (" + arenaLocks.describe() + ").");
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    /** MagicBridge hands in its wand / Magic-item detectors (Magic API). */
    void setMagicItemTests(Predicate<ItemStack> wand, Predicate<ItemStack> magicItem) {
        this.wandTest = wand;
        this.magicItemTest = magicItem;
    }

    // ---------- shared state access (used by MagicBridge too) ----------

    /** 0.1.5 guild ward (never null after onEnable). */
    WorldWard ward() {
        return ward;
    }

    /** 0.1.6 arena locks (never null after onEnable). */
    ArenaLocks arenaLocks() {
        return arenaLocks;
    }

    /** 0.1.5: attunement is ignored entirely in a warded (EliteMobs) world, for everyone. */
    public boolean ignored(Player player) {
        return ward != null && player != null && ward.isWarded(player.getWorld());
    }

    /** 0.1.5: arrival in a warded world clears stance, weapon/wand lock and combat context at once. */
    void clearForWard(Player player) {
        AttunementState s = states.get(player.getUniqueId());
        boolean had = s != null && s.activeStance(now()) != Stance.NONE;
        if (s != null) s.clear();
        feedback.remove(player.getUniqueId());
        projectiles.forgetShooter(player.getUniqueId());
        if (had && debug) getLogger().info(player.getName() + " stance cleared (entered warded world "
                + player.getWorld().getName() + ")");
    }

    public boolean attunementEnabled() {
        return enabledMaster;
    }

    public boolean debug() {
        return debug;
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public AttunementState state(UUID playerId) {
        return states.computeIfAbsent(playerId,
                id -> new AttunementState(windowSeconds,
                        getConfig().getLong("combat-context-seconds", 10L)));
    }

    public AttunementState stateOrNull(UUID playerId) {
        return states.get(playerId);
    }

    public Stance activeStance(Player player) {
        AttunementState s = states.get(player.getUniqueId());
        return s == null ? Stance.NONE : s.activeStance(now());
    }

    /** Feedback the moment a martial player's cast is blocked. */
    public void onCastBlocked(Player player, String spellKey) {
        lockFeedback(player, LockRule.Block.WAND, true);
        if (debug) getLogger().info("BLOCKED cast '" + spellKey + "' for " + player.getName()
                + " (martial stance, " + state(player.getUniqueId()).remainingSeconds(now()) + "s left)");
    }

    /** Feedback when a stance is SET (refreshes stay silent). */
    public void onStanceSet(Player player, Stance stance) {
        if (stance == Stance.MARTIAL) {
            player.sendMessage("§6Martial stance §7- your wand is stilled for " + windowSeconds + "s.");
        } else if (stance == Stance.CASTER) {
            player.sendMessage("§bCaster stance §7- your weapons are stilled for " + windowSeconds + "s.");
        }
        sendActionBar(player);
        if (debug) getLogger().info(player.getName() + " attuned: " + stance);
    }

    public void noteSpellKey(String key) {
        if (key != null && !classifier.isKnown(key) && warnedUnknownSpells.add(SpellClassifier.normalizeKey(key))) {
            getLogger().warning("Unknown spell key '" + key + "' - not in spell-classes.txt; "
                    + "treated as COMBAT (fail-closed). Regenerate the table with tools/gen_spell_table.py.");
        }
    }

    // ---------- messages / ticker ----------

    /**
     * 0.1.4 stance line with a live countdown. Calm colors while it just
     * ticks (fighter gray, caster dark aqua); bright when something was
     * blocked so the lock is noticed.
     */
    private void sendActionBar(Player player) {
        sendActionBar(player, false);
    }

    private void sendActionBar(Player player, boolean bright) {
        if (!actionbar) return;
        AttunementState s = states.get(player.getUniqueId());
        if (s == null) return;
        long now = now();
        Stance st = s.activeStance(now);
        if (st == Stance.NONE) return;
        NamedTextColor color = st == Stance.MARTIAL
                ? (bright ? NamedTextColor.GOLD : NamedTextColor.GRAY)
                : (bright ? NamedTextColor.AQUA : NamedTextColor.DARK_AQUA);
        player.sendActionBar(Component.text(StanceText.actionBar(st, s.remainingSeconds(now)), color));
    }

    private LockFeedback feedbackOf(Player player) {
        return feedback.computeIfAbsent(player.getUniqueId(), id -> new LockFeedback());
    }

    /**
     * A blocked switch, a blocked cast or a zeroed hit: bright action
     * bar, a dull thunk (throttled) and, the first time in this stance,
     * a small title. {@code loud=false} (pickups) = action bar only.
     */
    void lockFeedback(Player player, LockRule.Block block, boolean loud) {
        AttunementState s = states.get(player.getUniqueId());
        if (s == null) return;
        long now = now();
        Stance st = s.activeStance(now);
        if (st == Stance.NONE) return;
        sendActionBar(player, true);
        if (!loud) return;
        LockFeedback f = feedbackOf(player);
        if (sounds && f.takeSound(now)) {
            player.playSound(player.getLocation(), "minecraft:block.note_block.basedrum", 0.7f, 0.6f);
        }
        if (f.takeTitle(s.epoch())) {
            player.showTitle(Title.title(
                    Component.text(StanceText.title(block), NamedTextColor.RED),
                    Component.text(StanceText.subtitle(st, s.remainingSeconds(now)), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(300))));
        }
    }

    private void tick() {
        long now = now();
        projectiles.sweep(now);
        for (Player player : Bukkit.getOnlinePlayers()) {
            AttunementState s = states.get(player.getUniqueId());
            if (s == null) continue;
            if (s.activeStance(now) != Stance.NONE && ignored(player)) {
                clearForWard(player); // safety net: never a stance inside a warded world
                continue;
            }
            if (s.pollExpired(now)) {
                player.sendMessage("§7Your stance fades - wand and weapon are both free again.");
                if (actionbar) player.sendActionBar(Component.text(StanceText.CLEAR, NamedTextColor.GREEN));
                if (debug) getLogger().info(player.getName() + " stance expired");
            } else if (actionbarLive && s.activeStance(now) != Stance.NONE) {
                sendActionBar(player);
            }
        }
    }

    // ---------- damage pipeline ----------

    private record Attack(Player attacker, WeaponKind kind) {}

    /** Resolve an event's damager to (player, weapon kind), or null. */
    private Attack resolveAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            WeaponKind kind = Weapons.kindOfItemName(
                    player.getInventory().getItemInMainHand().getType().name());
            return new Attack(player, kind);
        }
        if (damager instanceof Projectile projectile) {
            Optional<ProjectileTracker.Shot> shot = projectiles.resolve(projectile.getUniqueId(), now());
            if (shot.isPresent()) {
                Player shooter = Bukkit.getPlayer(shot.get().shooter());
                if (shooter != null) return new Attack(shooter, shot.get().kind());
            }
        }
        return null;
    }

    /** NORMAL: combat-context bookkeeping (dealt + taken). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamageTrack(EntityDamageByEntityEvent event) {
        if (!enabledMaster) return;
        long now = now();
        Attack attack = resolveAttack(event);
        if (attack != null && event.getEntity() instanceof LivingEntity && !ignored(attack.attacker())) {
            state(attack.attacker().getUniqueId()).onDamageDealt(now);
        }
        if (event.getEntity() instanceof Player victim && !ignored(victim)) {
            Entity damager = event.getDamager();
            // Entity-sourced and not self-inflicted = hostile. Pure
            // environmental damage never reaches this event type.
            if (damager != null && !damager.getUniqueId().equals(victim.getUniqueId())) {
                state(victim.getUniqueId()).onHostileDamageTaken(now);
            }
        }
    }

    /** HIGH: zero a caster's martial-weapon damage (never cancel). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageZero(EntityDamageByEntityEvent event) {
        if (!enabledMaster) return;
        Attack attack = resolveAttack(event);
        if (attack == null || ignored(attack.attacker())) return;
        AttunementState s = stateOrNull(attack.attacker().getUniqueId());
        if (s == null) return;
        if (DamageRule.shouldZero(s.activeStance(now()), attack.kind())) {
            event.setDamage(0.0);
            lockFeedback(attack.attacker(), LockRule.Block.WEAPON, true);
            if (debug) getLogger().info("ZEROED " + attack.kind() + " hit by "
                    + attack.attacker().getName() + " (caster stance)");
        }
    }

    /** MONITOR: a landed martial hit (final damage > 0, not cancelled) attunes. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageTrigger(EntityDamageByEntityEvent event) {
        if (!enabledMaster) return;
        if (event.isCancelled()) return;
        Attack attack = resolveAttack(event);
        if (attack == null || ignored(attack.attacker())) return;
        if (!DamageRule.isMartialTrigger(attack.kind(), false, event.getDamage())) return;
        AttunementState s = state(attack.attacker().getUniqueId());
        AttunementState.HitResult result = s.onMartialHit(now());
        if (result == AttunementState.HitResult.SET) {
            onStanceSet(attack.attacker(), Stance.MARTIAL);
        } else if (result == AttunementState.HitResult.REFRESHED) {
            sendActionBar(attack.attacker());
        }
    }

    // ---------- projectile bookkeeping ----------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!enabledMaster) return;
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        if (ignored(shooter)) return;
        String hand = shooter.getInventory().getItemInMainHand().getType().name();
        WeaponKind kind = Weapons.kindOfProjectile(event.getEntityType().name(), hand);
        if (kind.isMartial()) {
            projectiles.record(event.getEntity().getUniqueId(), shooter.getUniqueId(), kind, now());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        // NOT an outright forget: on Paper the hit event fires BEFORE
        // the damage event of the same impact (rig-proven), so the
        // record is only marked spent and stays resolvable through the
        // tracker's short grace window.
        projectiles.markHit(event.getEntity().getUniqueId(), now());
    }

    // ---------- 0.1.4 main-hand lock ----------

    /** What would happen if {@code item} landed in this player's main hand now. */
    private LockRule.Block lockFor(Player player, ItemStack item) {
        if (!enabledMaster || (!weaponLock && !wandLock)) return LockRule.Block.NONE;
        if (item == null || item.getType().isAir()) return LockRule.Block.NONE;
        if (ignored(player)) return LockRule.Block.NONE;
        Stance st = activeStance(player);
        if (st == Stance.NONE) return LockRule.Block.NONE;
        boolean wand = wandTest.test(item);
        boolean magicItem = wand || magicItemTest.test(item);
        return LockRule.check(st, item.getType().name(), magicItem, wand, weaponLock, wandLock);
    }

    private boolean blocked(Player player, ItemStack item, String how) {
        LockRule.Block b = lockFor(player, item);
        if (b == LockRule.Block.NONE) return false;
        if (debug && !"pickup".equals(how)) getLogger().info("LOCKED " + how + " of " + item.getType() + " for " + player.getName()
                + " (" + activeStance(player) + ", " + b + ")");
        return true;
    }

    /**
     * Hotbar switch (scroll, number key, Bedrock tap). Cancel = server resends the old slot.
     * 0.1.7: a one-slot scroll onto a locked item hops OVER it to the next unlocked slot in
     * the same direction, so the lock never walls off the slots behind it (player report, Oct 26:
     * Java scroll stopped at the wand in Fighter stance and he could not reach his potions).
     * A direct jump (number key, touch tap) onto a locked slot is still refused.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack target = player.getInventory().getItem(event.getNewSlot());
        if (!blocked(player, target, "hotbar switch")) return;
        event.setCancelled(true);
        LockRule.Block block = lockFor(player, target);
        final int from = event.getPreviousSlot();
        final int hop = com.winkreyes.excelattunement.core.HotbarSkip.target(from, event.getNewSlot(),
                s -> lockFor(player, player.getInventory().getItem(s)) != LockRule.Block.NONE);
        if (hop == com.winkreyes.excelattunement.core.HotbarSkip.REFUSE) {
            lockFeedback(player, block, true);
            return;
        }
        hops++;
        // Next tick: the cancelled packet has been answered with the old slot by then.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) return;
            if (player.getInventory().getHeldItemSlot() != from) return; // the player moved on already
            if (lockFor(player, player.getInventory().getItem(hop)) != LockRule.Block.NONE) return; // changed meanwhile
            player.getInventory().setHeldItemSlot(hop);
            if (debug) getLogger().info("HOP hotbar " + (from + 1) + " -> " + (hop + 1) + " over a locked item for " + player.getName());
        });
        // Quiet feedback (action bar only): a scroll past the wand is not a mistake worth a title.
        lockFeedback(player, block, false);
    }

    /** 0.1.7: scrolls that hopped over a locked slot (status line). */
    private long hops;

    /** F / offhand swap. Magic owns F while a wand is in the main hand (it cancels first). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack incoming = event.getMainHandItem(); // the item that would end up in the main hand
        if (blocked(player, incoming, "offhand swap")) {
            event.setCancelled(true);
            lockFeedback(player, lockFor(player, incoming), true);
        }
    }

    /** Inventory clicks that would put a locked item into the held slot. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (activeStance(player) == Stance.NONE) return;
        PlayerInventory inv = player.getInventory();
        int held = inv.getHeldItemSlot();
        boolean onHeldSlot = event.getClickedInventory() == inv && event.getSlot() == held;
        ItemStack incoming = null;
        InventoryAction action = event.getAction();
        if (onHeldSlot) {
            switch (action) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> incoming = event.getCursor();
                default -> { }
            }
            if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
                incoming = inv.getItem(event.getHotbarButton());
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                incoming = inv.getItemInOffHand();
            }
        }
        if (incoming == null && event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == held) {
            incoming = event.getCurrentItem(); // number key = held slot: the clicked item moves into the hand
        }
        if (incoming == null && action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack h = inv.getItem(held);
            boolean heldEmpty = h == null || h.getType().isAir();
            boolean fromHotbar = event.getClickedInventory() == inv && event.getSlot() >= 0 && event.getSlot() <= 8;
            if (heldEmpty && !fromHotbar) incoming = event.getCurrentItem(); // shift-click could land in the hand
        }
        if (incoming != null && blocked(player, incoming, "inventory click")) {
            event.setCancelled(true);
            lockFeedback(player, lockFor(player, incoming), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (activeStance(player) == Stance.NONE) return;
        PlayerInventory inv = player.getInventory();
        InventoryView view = event.getView();
        int held = inv.getHeldItemSlot();
        for (int raw : event.getRawSlots()) {
            if (view.getInventory(raw) == inv && view.convertSlot(raw) == held) {
                if (blocked(player, event.getOldCursor(), "inventory drag")) {
                    event.setCancelled(true);
                    lockFeedback(player, lockFor(player, event.getOldCursor()), true);
                }
                return;
            }
        }
    }

    /**
     * A pickup lands in the first empty slot (vanilla order, slot 0 up -
     * rig-checked). When that slot is the held one, the weapon would go
     * straight into the hand: leave it on the ground until the stance
     * ends. Any other free slot = normal pickup (it is not in the hand).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        PlayerInventory inv = player.getInventory();
        if (inv.firstEmpty() != inv.getHeldItemSlot()) return;
        if (blocked(player, item, "pickup")) {
            event.setCancelled(true);
            // Fires every tick while standing on the item: feedback at most once a second.
            if (feedbackOf(player).takeQuiet(now())) {
                if (debug) getLogger().info("LOCKED pickup of " + item.getType() + " for " + player.getName()
                        + " (" + activeStance(player) + ", left on the ground)");
                sendActionBar(player, true);
            }
        }
    }

    // ---------- lifecycle ----------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
        feedback.remove(event.getPlayer().getUniqueId());
        projectiles.forgetShooter(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        AttunementState s = states.get(event.getEntity().getUniqueId());
        if (s != null) s.clear();
        projectiles.forgetShooter(event.getEntity().getUniqueId());
    }

    // ---------- /attunement ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("attunement")) return false;
        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
            long now = now();
            StringBuilder out = new StringBuilder("Attunement: "
                    + (enabledMaster ? "ON" : "OFF") + ", window " + windowSeconds + "s, weapon-lock "
                    + onOff(weaponLock) + ", wand-lock " + onOff(wandLock) + ", scroll hops " + hops + ". Stanced players:");
            boolean any = false;
            for (Player p : Bukkit.getOnlinePlayers()) {
                AttunementState s = states.get(p.getUniqueId());
                if (s == null) continue;
                Stance st = s.activeStance(now);
                if (st == Stance.NONE) continue;
                any = true;
                out.append("\n  ").append(p.getName()).append(": ").append(st)
                        .append(" (").append(s.remainingSeconds(now)).append("s left)");
            }
            if (!any) out.append(" none");
            out.append("\n").append(ward.status(sender instanceof Player p ? p.getWorld() : null));
            out.append("\n").append(arenaLocks.status());
            sender.sendMessage(out.toString());
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("ward")) {
            org.bukkit.World w = args.length >= 2 ? Bukkit.getWorld(args[1])
                    : (sender instanceof Player p ? p.getWorld() : null);
            if (args.length >= 2 && w == null) sender.sendMessage("No loaded world named " + args[1]);
            sender.sendMessage(ward.status(w));
            sender.sendMessage(arenaLocks.status());
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("clear")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("No online player named " + args[1]);
                return true;
            }
            AttunementState s = states.get(target.getUniqueId());
            if (s != null) s.clear();
            projectiles.forgetShooter(target.getUniqueId());
            sender.sendMessage("Cleared attunement for " + target.getName());
            return true;
        }
        sender.sendMessage("Usage: /attunement status | /attunement clear <player> | /attunement ward [world]");
        return true;
    }

    // ---------- spell table ----------

    private SpellClassifier loadClassifier() {
        // A data-folder spell-classes.txt wins over the bundled one, so
        // a future stage's table can be dropped in without a rebuild.
        File override = new File(getDataFolder(), "spell-classes.txt");
        try {
            if (override.isFile()) {
                getLogger().info("Using spell table override: " + override.getPath());
                return SpellClassifier.fromLines(Files.readAllLines(override.toPath(), StandardCharsets.UTF_8));
            }
            try (InputStream in = getResource("spell-classes.txt")) {
                if (in == null) throw new IllegalStateException("spell-classes.txt missing from jar");
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return SpellClassifier.fromLines(text.lines().toList());
            }
        } catch (Exception e) {
            getLogger().severe("Could not load the spell table: " + e.getMessage()
                    + " - falling back to an EMPTY table (every spell classifies COMBAT).");
            return new SpellClassifier(java.util.Map.of());
        }
    }
}
