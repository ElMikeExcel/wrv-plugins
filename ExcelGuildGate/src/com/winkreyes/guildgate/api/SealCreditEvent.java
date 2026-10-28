package com.winkreyes.guildgate.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired by ExcelGuildGate when a seal-boss death is ABOUT to be
 * credited as a seal break - before anything is recorded.
 *
 * Since 0.3.1 a boss seal (ender_dragon, wither, elder_guardian,
 * warden) is proposed through this event first; any listener may
 * cancel it to veto the credit (the boss still died and dropped
 * its loot - only the seal credit is withheld). ExcelBossTrial is
 * the veto holder: it cancels credits that fail the valley's
 * share / presence / cooldown rules.
 *
 * The raid seal never passes through here (raid crediting is
 * unchanged since 0.1.0), and admin /guildgate grant never fires
 * this event - an operator's word is final.
 *
 * Backwards compatible by construction: with no listener
 * registered, behavior is byte-identical to 0.3.0.
 */
public class SealCreditEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String sealId;
    private final LivingEntity entity;
    private final String killerName;
    private boolean cancelled;

    public SealCreditEvent(String sealId, LivingEntity entity, String killerName) {
        this.sealId = sealId;
        this.entity = entity;
        this.killerName = killerName;
    }

    /** The seal about to be credited (a boss seal id). */
    public String getSealId() {
        return sealId;
    }

    /** The boss entity that died. */
    public LivingEntity getEntity() {
        return entity;
    }

    /** The credited killer's name, or null when the forgiving
     *  "someone was online" rule supplied the credit. */
    public String getKillerName() {
        return killerName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
