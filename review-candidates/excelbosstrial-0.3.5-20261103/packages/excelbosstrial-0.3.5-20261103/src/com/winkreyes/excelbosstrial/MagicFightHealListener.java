/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * The Magic heal-spell feed (ExcelBossTrial 0.3.1): turns Magic
 * cast events into healing-DONE claims for the fight record.
 *
 * Why this class exists on its own (house build style - a
 * replaceable adapter, fail-soft): ExcelBossTrial's main class
 * must load cleanly when Magic is absent. This listener is the
 * only class that links Magic's API, and the plugin registers it
 * only when the Magic plugin is present, inside a try/catch - a
 * linkage failure leaves the feed off and the rest of the fight
 * record (damage tables, healing received, unattributed buckets)
 * fully working.
 *
 * What it feeds: a completed cast (CAST / CAST_TARGET /
 * CAST_SELF) of a configured heal spell (config
 * fight-tracking.magic-heal-spells, settled default heal +
 * superheal) whose target entity is a player becomes a claim:
 * caster, target, spell, now. The claim moves no numbers by
 * itself - the FightTracker matches it at snapshot time against
 * the CUSTOM-cause regain events Magic's HealAction fires inside
 * the same cast. A claim whose heal never lands (target already
 * full, heal cancelled) simply absorbs nothing.
 */
public final class MagicFightHealListener implements Listener {

    private final ExcelBossTrial plugin;

    public MagicFightHealListener(ExcelBossTrial plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCast(CastEvent event) {
        SpellResult result = event.getSpellResult();
        if (result != SpellResult.CAST && result != SpellResult.CAST_TARGET
                && result != SpellResult.CAST_SELF) {
            return;
        }
        Spell spell = event.getSpell();
        if (spell == null || !plugin.isHealSpellKey(spell.getKey())) {
            return;
        }
        Mage mage = event.getMage();
        Player caster = mage != null ? mage.getPlayer() : null;
        if (caster == null) {
            return;
        }
        Entity target = null;
        CastContext context = event.getContext();
        if (context != null) {
            target = context.getTargetEntity();
        }
        if (target == null) {
            target = spell.getTargetEntity();
        }
        if (target instanceof Player targetPlayer) {
            plugin.noteMagicHealCast(caster.getUniqueId(),
                    targetPlayer.getUniqueId(), spell.getKey());
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
