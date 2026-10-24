package com.winkreyes.excelattunement;

import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.SpellClassifier;
import com.winkreyes.excelattunement.core.Stance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
    }

    /** Martial stance blocks EVERY wand cast, whatever the spell class. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreCast(PreCastEvent event) {
        if (!plugin.attunementEnabled()) return;
        Player player = event.getMage() == null ? null : event.getMage().getPlayer();
        if (player == null) return; // mob / console / automata mages are not attuned
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
