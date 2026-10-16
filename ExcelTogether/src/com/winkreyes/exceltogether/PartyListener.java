package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Multiplies money and XP on the final JobsPrePaymentEvent amounts. Jobs applies its own
 * boosts (weekend etc.) after this event, so the result is base * party * (1 + jobsBoost).
 * Points are not changed.
 */
public final class PartyListener implements Listener {
    private final PartyBonus bonus;
    private final PartyFinder finder;
    public int applied = 0;

    public PartyListener(PartyBonus bonus, PartyFinder finder) {
        this.bonus = bonus;
        this.finder = finder;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPay(JobsPrePaymentEvent e) {
        if (e.getAmount() <= 0 && e.getExp() <= 0) return;
        if (e.getPlayer() == null || !finder.hasPartner(e.getPlayer())) return;
        e.setAmount(bonus.apply(e.getAmount()));
        e.setExp(bonus.apply(e.getExp()));
        applied++;
    }
}
