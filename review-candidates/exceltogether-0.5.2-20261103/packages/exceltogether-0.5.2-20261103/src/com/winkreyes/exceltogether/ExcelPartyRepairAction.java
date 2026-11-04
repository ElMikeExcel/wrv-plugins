package com.winkreyes.exceltogether;

import com.elmakers.mine.bukkit.action.CompoundEntityAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

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
 * 0.5.2 - the CONFIRMATION GATE (Mike, Nov 1, 2026: "add a confirmation
 * screen pop up for this spell to combat using it when not wanted").
 * Magic 11.2.5 has no cast-confirmation facility and charges its spell
 * cooldown on any cast, so the spell's Magic cooldown is now a 1-second
 * anti-bounce and the real 4-hour cooldown is RepairGate's persisted
 * lockout, consulted here before the compound runs:
 *   ARM     - the cast repairs NOTHING; the caster gets the prompt with
 *             the live damaged-item count and the window opens.
 *   CONFIRM - a recast inside the window starts the lockout (persisted
 *             at once) and the compound runs exactly as in 0.5.1.
 *   LOCKED  - the cast is refused with the remaining time.
 * The verdict is reached once per cast and cached: whether the engine
 * drives this action through perform() or start(), a confirmed cast is
 * never re-judged mid-flight. With no gate bound (config off, Magic
 * link down), the gate steps aside and the cast repairs at once, the
 * 0.5.1 behaviour.
 *
 * Registered with Magic by ReflectMagicLink when Magic is present;
 * this class is only ever loaded in that case. Excel - Wink Reyes Valley.
 */
public class ExcelPartyRepairAction extends CompoundEntityAction {
    private double maxRange;
    private boolean countArmor;
    private boolean countHeldItem;
    private boolean gateDecided;
    private SpellResult gateBlock;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        maxRange = parameters.getDouble("max_range", 0);
        if (maxRange < 0) maxRange = 0;
        // The gate's damaged-item count uses the spell's own scope (the
        // shipped block carries armor / held_item at spell level too).
        countArmor = parameters.getBoolean("armor", true);
        countHeldItem = parameters.getBoolean("held_item", true);
        gateDecided = false;
        gateBlock = null;
    }

    @Override
    public SpellResult perform(CastContext context) {
        SpellResult block = gateBlock(context);
        if (block != null) return block;
        return super.perform(context);
    }

    @Override
    public SpellResult start(CastContext context) {
        SpellResult block = gateBlock(context);
        if (block != null) return block;
        return super.start(context);
    }

    /**
     * The gate, consulted at most once per cast. Returns the result the
     * cast should end with WITHOUT running the repair, or null when the
     * repair may proceed (confirmed - or no gate bound at all).
     */
    private SpellResult gateBlock(CastContext context) {
        if (gateDecided) return gateBlock;
        gateDecided = true;
        RepairGate gate = PartyRepairBridge.gate();
        if (gate == null) return null;
        Player caster = context.getMage() == null ? null : context.getMage().getPlayer();
        if (caster == null && context.getTargetEntity() instanceof Player p) caster = p;
        if (caster == null) return null;
        UUID id = caster.getUniqueId();
        long now = System.currentTimeMillis();
        Logger log = PartyRepairBridge.logger();
        RepairGate.Verdict verdict = gate.verdict(id, now);
        switch (verdict.decision) {
            case LOCKED -> {
                context.sendMessage(gate.refusedText(verdict.remainingMs));
                if (log != null) {
                    log.info("ExcelTogether party repair: " + caster.getName()
                            + " cast Repair Equipment during the confirmation-gate lockout - refused, "
                            + RepairGate.formatDuration(verdict.remainingMs) + " remaining.");
                }
                gateBlock = SpellResult.NO_TARGET;
            }
            case ARM -> {
                int[] counts = countDamaged(caster);
                gate.arm(id, now);
                context.sendMessage(gate.promptText(counts[0], counts[1], counts[2]));
                if (log != null) {
                    log.info("ExcelTogether party repair: " + caster.getName()
                            + " armed Repair Equipment - " + counts[0] + " damaged item(s) across "
                            + counts[1] + " of " + counts[2] + " party member(s); awaiting confirmation.");
                }
                gateBlock = SpellResult.NO_TARGET;
            }
            case CONFIRM -> {
                gate.confirm(id, now);
                PartyRepairBridge.persistCooldowns();
                if (log != null) {
                    log.info("ExcelTogether party repair: " + caster.getName()
                            + " confirmed Repair Equipment - " + RepairGate.formatDuration(gate.cooldownMs())
                            + " lockout started.");
                }
                gateBlock = null;
            }
        }
        return gateBlock;
    }

    /**
     * The prompt's live count: {repairable items, members owning at least
     * one, members checked} over exactly the roster the repair would
     * touch, judged by EquipmentRepair's own repairable test (wand
     * exclusion included) - so the count the caster confirms against is
     * the repair that will actually happen.
     */
    private int[] countDamaged(Player caster) {
        List<UUID> members = PartyRepairBridge.partyMembersOf(caster.getUniqueId());
        List<Player> selected = PartyRoster.select(caster, members, maxRange, Bukkit::getPlayer);
        int items = 0;
        int withDamage = 0;
        for (Player member : selected) {
            EntityEquipment equipment = member.getEquipment();
            if (equipment == null) continue;
            int mine = 0;
            for (EquipmentSlot slot : EquipmentRepair.slots(countArmor, countHeldItem)) {
                if (EquipmentRepair.repairable(equipment.getItem(slot),
                        ExcelRepairItemAction.IS_WAND, EquipmentRepair.META)) {
                    mine++;
                }
            }
            if (mine > 0) withDamage++;
            items += mine;
        }
        return new int[]{items, withDamage, selected.size()};
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
