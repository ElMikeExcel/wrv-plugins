package com.winkreyes.excelattunement;

import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.DamageRule;
import com.winkreyes.excelattunement.core.ProjectileTracker;
import com.winkreyes.excelattunement.core.SpellClassifier;
import com.winkreyes.excelattunement.core.Stance;
import com.winkreyes.excelattunement.core.WeaponKind;
import com.winkreyes.excelattunement.core.Weapons;
import net.kyori.adventure.text.Component;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ExcelAttunement — Combat Attunement (Mike's design, approved
 * Oct 19, 2026). An EFFECT lock, never an inventory lock:
 *  - land a hit with a martial weapon -> MARTIAL stance (wand casts
 *    are blocked at Magic's PreCastEvent, see MagicBridge);
 *  - cast a combat spell, or a support spell while in combat ->
 *    CASTER stance (that player's martial-weapon damage is zeroed);
 *  - 30s window (config), refreshed by stance interactions; taking
 *    hostile damage refreshes the active stance;
 *  - utility spells never attune; a support cast out of combat never
 *    attunes (the accepted pre-fight opener).
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        enabledMaster = getConfig().getBoolean("enabled", true);
        windowSeconds = getConfig().getLong("window-seconds", 30L);
        long combatContextSeconds = getConfig().getLong("combat-context-seconds", 10L);
        debug = getConfig().getBoolean("debug", false);
        classifier = loadClassifier();
        getServer().getPluginManager().registerEvents(this, this);
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
    }

    // ---------- shared state access (used by MagicBridge too) ----------

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
        sendActionBar(player);
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

    private void sendActionBar(Player player) {
        AttunementState s = states.get(player.getUniqueId());
        if (s == null) return;
        Stance st = s.activeStance(now());
        if (st == Stance.NONE) return;
        String label = st == Stance.MARTIAL ? "§7Martial stance" : "§3Caster stance";
        player.sendActionBar(Component.text(label));
    }

    private void tick() {
        long now = now();
        projectiles.sweep(now);
        for (Player player : Bukkit.getOnlinePlayers()) {
            AttunementState s = states.get(player.getUniqueId());
            if (s == null) continue;
            if (s.pollExpired(now)) {
                player.sendMessage("§7Your stance fades - wand and weapon are both free again.");
                if (debug) getLogger().info(player.getName() + " stance expired");
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
        if (attack != null && event.getEntity() instanceof LivingEntity) {
            state(attack.attacker().getUniqueId()).onDamageDealt(now);
        }
        if (event.getEntity() instanceof Player victim) {
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
        if (attack == null) return;
        AttunementState s = stateOrNull(attack.attacker().getUniqueId());
        if (s == null) return;
        if (DamageRule.shouldZero(s.activeStance(now()), attack.kind())) {
            event.setDamage(0.0);
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
        if (attack == null) return;
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

    // ---------- lifecycle ----------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
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
                    + (enabledMaster ? "ON" : "OFF") + ", window " + windowSeconds + "s. Stanced players:");
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
            sender.sendMessage(out.toString());
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
        sender.sendMessage("Usage: /attunement status | /attunement clear <player>");
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
