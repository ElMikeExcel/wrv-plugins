package com.winkreyes.excelattunement;

import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.event.WandPreActivateEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.SpellClassifier;
import com.winkreyes.excelattunement.core.Stance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
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
final class MagicBridge implements Listener {
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
