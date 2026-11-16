package com.winkreyes.exceltogether;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * ExcelTogether 0.5.1 - Magic action "ExcelRepairItem": the write-back
 * twin of Magic's stock RepairItemAction. Same parameters (armor /
 * held_item), same slot scope (four armor slots + main hand, never the
 * offhand), same wand exclusion - but where the stock action mutates
 * the copies Paper 26.3 equipment getters hand out and so repairs
 * nothing at all (REPAIREQUIP-REDESIGN.md §4), this one sets each
 * repaired item back through the equipment setter via EquipmentRepair.
 *
 * Registered with Magic by ReflectMagicLink when Magic is present;
 * this class is only ever loaded in that case. Per cast it tallies
 * what it repaired and reports once, from finish(): a summary line in
 * the server log, and - when a whole cast repaired nothing - an honest
 * note to the caster in game. Excel - Wink Reyes Valley.
 */
public class ExcelRepairItemAction extends BaseSpellAction {
    private boolean armor;
    private boolean heldItem;
    private int castItems;
    private int castMembers;

    /** Shared with ExcelPartyRepairAction's confirmation-gate count (0.5.2). */
    static final Predicate<ItemStack> IS_WAND = stack -> {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        return magic instanceof MagicAPI api && api.isWand(stack);
    };

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        armor = parameters.getBoolean("armor", false);
        heldItem = parameters.getBoolean("held_item", false);
        castItems = 0;
        castMembers = 0;
    }

    @Override
    public SpellResult perform(CastContext context) {
        Entity target = context.getTargetEntity();
        if (!(target instanceof LivingEntity living)) return SpellResult.NO_TARGET;
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return SpellResult.NO_TARGET;
        int repaired = EquipmentRepair.repair(equipment, armor, heldItem, IS_WAND, EquipmentRepair.META);
        if (repaired <= 0) return SpellResult.NO_TARGET;
        castItems += repaired;
        castMembers++;
        return SpellResult.CAST;
    }

    @Override
    public void finish(CastContext context) {
        super.finish(context);
        Logger log = PartyRepairBridge.logger();
        String caster = context.getMage() == null ? "?" : context.getMage().getName();
        if (castItems > 0) {
            if (log != null) {
                log.info("ExcelTogether party repair: " + caster + " repaired " + castItems
                        + " item(s) across " + castMembers + " party member(s).");
            }
        } else {
            if (log != null) {
                log.info("ExcelTogether party repair: " + caster
                        + " cast Repair Equipment - nothing in the party needed repair.");
            }
            context.sendMessage("Repair Equipment: nobody in your party needed repairs.");
        }
    }

    @Override
    public boolean requiresTargetEntity() {
        return true;
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters) {
        super.getParameterNames(spell, parameters);
        parameters.add("armor");
        parameters.add("held_item");
    }
}
