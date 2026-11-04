package com.winkreyes.exceltogether;

import com.elmakers.mine.bukkit.action.CompoundEntityAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.Spell;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * ExcelTogether 0.5.1 - Magic action "ExcelPartyRepair": a compound
 * entity action whose entity set is the caster's ExcelTogether party -
 * the true-party variant of the Repair Equipment redesign
 * (REPAIREQUIP-REDESIGN.md §6, Flavor B). Where AreaOfEffect collects
 * whoever stands near the caster (strangers in, absent members out),
 * this collects exactly the roster of record: the caster's party
 * members, online and in the caster's world, the caster included. A
 * solo caster's set is himself, so the spell still repairs him alone.
 *
 * The per-entity step is CompoundEntityAction's own: each collected
 * member is installed as the target in turn and the child actions run
 * (in the shipped spell block, ExcelRepairItem). An optional max_range
 * parameter (blocks, default 0 = no cap) can bound how far a member
 * may be and still be included.
 *
 * Registered with Magic by ReflectMagicLink when Magic is present;
 * this class is only ever loaded in that case. Excel - Wink Reyes Valley.
 */
public class ExcelPartyRepairAction extends CompoundEntityAction {
    private double maxRange;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        maxRange = parameters.getDouble("max_range", 0);
        if (maxRange < 0) maxRange = 0;
    }

    @Override
    public void addEntities(CastContext context, List<WeakReference<Entity>> entities) {
        Player caster = context.getMage() == null ? null : context.getMage().getPlayer();
        if (caster == null && context.getTargetEntity() instanceof Player p) caster = p;
        if (caster == null) return;
        List<UUID> members = PartyRepairBridge.partyMembersOf(caster.getUniqueId());
        for (Player member : PartyRoster.select(caster, members, maxRange, Bukkit::getPlayer)) {
            entities.add(new WeakReference<>(member));
        }
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters) {
        super.getParameterNames(spell, parameters);
        parameters.add("max_range");
    }
}
