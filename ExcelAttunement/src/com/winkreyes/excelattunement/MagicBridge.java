package com.winkreyes.excelattunement;

import com.elmakers.mine.bukkit.api.batch.Batch;
import com.elmakers.mine.bukkit.api.batch.SpellBatch;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.event.PreLoadEvent;
import com.elmakers.mine.bukkit.api.event.StartCastEvent;
import com.elmakers.mine.bukkit.api.event.WandPreActivateEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.protection.EntityTargetingManager;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.ArenaLock;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.SpellClassifier;
import com.winkreyes.excelattunement.core.Stance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The ONLY class that imports Magic's API — registered solely when
 * the Magic plugin is enabled, so ExcelAttunement loads cleanly with
 * or without Magic present.
 *
 * Interception point, verified against Magic 11.2.5 bytecode
 * (com.elmakers.mine.bukkit.spell.BaseSpell): every cast path — wand
 * click, spell item, API cast — runs through BaseSpell.cast, which
 * fires PreCastEvent BEFORE any spell action executes and aborts the
 * cast entirely (processResult(CANCELLED), cast returns false) when a
 * listener cancels it. Cancelling PreCastEvent is therefore a complete
 * block: no mana spent on effects, no partial cast, no cooldown games.
 *
 * Stance setting listens to CastEvent instead: it fires after the
 * cast with its SpellResult, and only genuine cast results (CAST /
 * CAST_SELF / CAST_TARGET / ALTERNATE* alternate-mode casts) attune.
 * Fizzles, cooldowns, no-target and blocked results never attune —
 * swinging at the air with a wand is not a cast, and neither is a
 * failed one.
 */
final class MagicBridge implements Listener, EntityTargetingManager {
    private static final Set<SpellResult> CAST_RESULTS = EnumSet.of(
            SpellResult.CAST, SpellResult.CAST_SELF, SpellResult.CAST_TARGET,
            SpellResult.ALTERNATE, SpellResult.ALTERNATE_UP, SpellResult.ALTERNATE_DOWN,
            SpellResult.ALTERNATE_SNEAK, SpellResult.ALTERNATE_JUMPING,
            SpellResult.ALTERNATE_SNEAK_UP, SpellResult.ALTERNATE_SNEAK_DOWN,
            SpellResult.ALTERNATE_JUMPING_UP, SpellResult.ALTERNATE_JUMPING_DOWN);

    private final ExcelAttunement plugin;
    private final SpellClassifier classifier;

    MagicBridge(ExcelAttunement plugin, SpellClassifier classifier) {
        this.plugin = plugin;
        this.classifier = classifier;
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        this.api = magic instanceof MagicAPI m ? m : null;
        if (api == null) {
            plugin.getLogger().warning("Magic API not found: wand-lock OFF (cast block still ON).");
        } else {
            plugin.setMagicItemTests(this::isWand, this::isMagicItem);
        }
        WorldWard ward = plugin.ward();
        if (ward != null && api != null) ward.setMagicStopper(this::stopMagic);
        ArenaLocks locks = plugin.arenaLocks();
        if (locks != null && api != null) {
            locks.setSpellStopper(this::stopSpells);
            // 0.1.6 arena locks: Magic asks its targeting managers whether a spell may pick an
            // entity. Registered now (Magic already loaded) and again on every Magic (re)load
            // via PreLoadEvent, because a Magic reload clears its provider list first.
            try {
                api.getController().register(this);
            } catch (Throwable t) {
                plugin.getLogger().warning("Arena locks: could not register the Magic targeting check ("
                        + t + "); the Lift target pre-check at cast time still applies.");
            }
        }
    }

    /** Magic (re)load: hand the targeting check back (Magic cleared its list). */
    @EventHandler
    public void onMagicPreLoad(PreLoadEvent event) {
        if (plugin.arenaLocks() != null) event.registerEntityTargetingManager(this);
    }

    /**
     * 0.1.5 guild ward: on arrival in a warded world, stop everything
     * Magic is running for this player and close the wand (Magic 11.2.5
     * Mage API: cancelPending(force), deactivateAllSpells(force, quiet),
     * getActiveWand().deactivate()). WandPreActivateEvent then keeps the
     * wand closed while the player stays.
     */
    void stopMagic(Player player) {
        if (api == null) return;
        Mage mage = api.getMage(player);
        if (mage == null) return;
        mage.cancelPending(true);
        mage.deactivateAllSpells(true, true);
        Wand wand = mage.getActiveWand();
        if (wand != null && plugin.ward() != null && plugin.ward().blockWand()) wand.deactivate();
    }

    /**
     * 0.1.6 arena locks: stop only the named spells (base keys) for this
     * player: running batches of those spells are cancelled and an
     * active (toggled) one is deactivated. Other spells keep running.
     */
    int stopSpells(Player player, Set<String> baseKeys) {
        if (api == null || baseKeys.isEmpty()) return 0;
        Mage mage = api.getMage(player);
        if (mage == null) return 0;
        int stopped = 0;
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (Batch b : new java.util.ArrayList<>(mage.getPendingBatches())) {
            if (b instanceof SpellBatch sb && sb.getSpell() != null
                    && baseKeys.contains(ArenaLock.baseKey(sb.getSpell().getKey()))) keys.add(sb.getSpell().getKey());
        }
        for (String k : keys) {
            mage.cancelPending(k, true);
            stopped++;
            if (plugin.debug()) plugin.getLogger().info("ARENALOCK stopped running '" + k + "' for " + player.getName()
                    + " in the dragon's zone in " + player.getWorld().getName());
        }
        for (Spell sp : new java.util.ArrayList<>(mage.getActiveSpells())) {
            if (sp != null && baseKeys.contains(ArenaLock.baseKey(sp.getKey()))) {
                mage.deactivateSpell(sp);
                stopped++;
            }
        }
        return stopped;
    }

    /**
     * 0.1.5 guild ward: in a warded world EVERY Magic cast is cancelled,
     * heals and passives included (Mike, Oct 24, 2026). LOWEST so it runs
     * before the stance check; a cancelled PreCastEvent costs no mana
     * and starts no cooldown (checks run after it in BaseSpell).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWardPreCast(PreCastEvent event) {
        WorldWard ward = plugin.ward();
        if (ward == null || !ward.enabled()) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null) return;
        if (ward.blocksCast(player)) {
            event.setCancelled(true);
            ward.onCastBlocked(player, event.getSpell() == null ? "?" : event.getSpell().getKey());
        }
    }

    /**
     * 0.1.6 arena locks: a locked spell (glide, fling incl. leap, hulkleap,
     * lift, blink by default) is cancelled while the caster stands in the
     * dragon's zone and she lives. LOW: after the ward (LOWEST), still
     * before the stance check and before Magic spends mana or starts the
     * cooldown. Skips casts the ward already cancelled (one message, not two).
     *
     * Lift (protect-targets-in-zone) is also cancelled when its target is
     * a player inside an active zone, wherever the caster stands. The
     * target is found with Magic's own targeting (Spell.target(), the
     * same call ActionSpell.onCast makes; BaseSpell.reset() ran before
     * this event, and onCast reuses the found target), so the check sees
     * exactly what the cast would hit and still costs nothing. As a
     * backstop, canTarget below refuses protected players for the rest of
     * that same cast tick.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onArenaLockPreCast(PreCastEvent event) {
        ArenaLocks locks = plugin.arenaLocks();
        if (locks == null || !locks.enabled() || event.isCancelled() || event.getSpell() == null) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null) return;
        Spell spell = event.getSpell();
        String key = spell.getKey();
        ArenaLock.Rule rule = locks.blocking(player, key);
        if (rule != null) {
            event.setCancelled(true);
            locks.onCastBlocked(player, key, rule);
            return;
        }
        ArenaLock.Rule tr = locks.targetRule(player, key);
        if (tr == null) return;
        Entity target = null;
        probing = true;
        try {
            spell.target();
            target = spell.getTargetEntity();
        } catch (Throwable t) {
            if (!probeWarned) {
                probeWarned = true;
                plugin.getLogger().warning("Arena locks: could not pre-check the " + key + " target (" + t
                        + "); the Magic targeting check still applies.");
            }
        } finally {
            probing = false;
        }
        if (locks.protectsTarget(tr, player, target)) {
            event.setCancelled(true);
            locks.onTargetBlocked(player, key, tr, target, "cast cancelled");
            return;
        }
        inFlight.put(player.getUniqueId(), new InFlight(Bukkit.getCurrentTick(), tr, key));
    }

    /** Lift casts in progress this tick (backstop for canTarget). */
    private static final class InFlight {
        final int tick; final ArenaLock.Rule rule; final String key;
        final java.util.Set<java.util.UUID> refused = new java.util.HashSet<>();
        InFlight(int tick, ArenaLock.Rule rule, String key) { this.tick = tick; this.rule = rule; this.key = key; }
    }
    private final Map<java.util.UUID, InFlight> inFlight = new HashMap<>();
    private boolean probing;
    private boolean probeWarned;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStartCast(StartCastEvent event) {
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player != null) inFlight.remove(player.getUniqueId());
    }

    /**
     * Magic EntityTargetingManager: during a protect-targets cast (Lift)
     * started this tick, a player inside an active zone cannot be picked.
     * Everything else (other spells, other ticks, the pre-check probe
     * itself) is left to Magic.
     */
    @Override
    public boolean canTarget(Entity attacker, Entity entity) {
        if (probing || inFlight.isEmpty() || !(attacker instanceof Player caster) || !(entity instanceof Player)) return true;
        InFlight f = inFlight.get(caster.getUniqueId());
        if (f == null) return true;
        if (f.tick != Bukkit.getCurrentTick()) {
            inFlight.remove(caster.getUniqueId());
            return true;
        }
        ArenaLocks locks = plugin.arenaLocks();
        if (locks == null || !locks.protectsTarget(f.rule, caster, entity)) return true;
        // Magic may ask about the same candidate more than once per cast: log / notify once.
        if (f.refused.add(entity.getUniqueId())) locks.onTargetBlocked(caster, f.key, f.rule, entity, "targeting refused");
        return false;
    }

    /** 0.1.5 guild ward: the wand stays a plain item in a warded world (no spell bar, no mana bar). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWardWandActivate(WandPreActivateEvent event) {
        WorldWard ward = plugin.ward();
        if (ward == null || !ward.blockWand()) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null) return;
        if (ward.blocksCast(player)) {
            event.setCancelled(true);
            ward.onWandBlocked(player);
        }
    }

    private final MagicAPI api;

    /** A Magic wand (Magic API, reads the item's Magic data). */
    boolean isWand(ItemStack item) {
        if (api == null || item == null || item.getType().isAir()) return false;
        try {
            return api.isWand(item);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Any Magic item: a wand, or a spell icon from the action-bar casting mode. */
    boolean isMagicItem(ItemStack item) {
        if (api == null || item == null || item.getType().isAir()) return false;
        try {
            return api.isWand(item) || api.isSpell(item);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Martial stance blocks EVERY wand cast, whatever the spell class. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreCast(PreCastEvent event) {
        if (!plugin.attunementEnabled()) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null) return; // mob / console / automata mages are not attuned
        if (plugin.ignored(player)) return; // 0.1.5: attunement is off in warded worlds
        if (plugin.activeStance(player) == Stance.MARTIAL) {
            event.setCancelled(true);
            plugin.onCastBlocked(player, event.getSpell() == null ? "?" : event.getSpell().getKey());
        }
    }

    /** A real cast of a combat spell — or of a support spell while in combat — attunes. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCast(CastEvent event) {
        if (!plugin.attunementEnabled()) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null || event.getSpell() == null) return;
        if (plugin.ignored(player)) return; // 0.1.5: a bypass admin casting in a warded world never attunes
        if (!CAST_RESULTS.contains(event.getSpellResult())) return;
        String key = event.getSpell().getKey();
        plugin.noteSpellKey(key);
        SpellClass cls = classifier.classify(key);
        AttunementState state = plugin.state(player.getUniqueId());
        AttunementState.CastResult result = state.onCast(cls, plugin.now());
        if (result == AttunementState.CastResult.SET) {
            plugin.onStanceSet(player, Stance.CASTER);
        }
        if (plugin.debug()) {
            plugin.getLogger().info(player.getName() + " cast " + key + " (" + cls + ") -> " + result);
        }
    }
}
