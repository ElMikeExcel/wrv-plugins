package com.winkreyes.rewardguard.integration;

import com.gamingmesh.jobs.api.JobsExpGainEvent;
import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import com.winkreyes.rewardguard.ExcelRewardGuard;
import com.winkreyes.rewardguard.RewardScale;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Jobs enforcement. Both Jobs events carry the entity the action is about,
 * so the reduction is exact per kill: when the entity's provenance verdict
 * is DENY, the reward is scaled to farm-reward-percent (default 65).
 *
 * The javap verdict against the installed Jobs 5.2.6.3 jar: every component
 * the 1.0 veto covered has a setter on JobsPrePaymentEvent — setAmount
 * (money), setPoints (points) and setExp (XP), each writing the event's
 * amounts map under its CurrencyType — and JobsExpGainEvent has setExp for
 * its XP (that event carries XP only). So at any percent above 0 the
 * amounts are scaled and NOTHING is cancelled; there is no component that
 * needs a cancel fallback. At percent 0 the handlers reproduce 1.0 exactly:
 * the pre-payment event is cancelled, and the XP gain is zeroed and
 * cancelled. At percent 100 the handlers are a no-op pass-through.
 *
 * This class is only loaded when the Jobs plugin is present (the main class
 * checks before registering it), so the Jobs classes are safe to reference.
 */
public final class JobsVeto implements Listener {
    private final ExcelRewardGuard plugin;

    public JobsVeto(ExcelRewardGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrePayment(JobsPrePaymentEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity == null || !plugin.isDenied(entity)) return;
        int percent = plugin.farmRewardPercent();
        if (percent >= 100) return;
        if (percent <= 0) {
            event.setCancelled(true);
            plugin.countJobsIntervention();
            if (plugin.debugLogging()) {
                plugin.getLogger().info("Jobs payment blocked for denied " + entity.getType()
                    + " (job " + event.getJob().getName() + ", farm-reward-percent 0).");
            }
            return;
        }
        double money = event.getAmount();
        double points = event.getPoints();
        double exp = event.getExp();
        event.setAmount(RewardScale.scale(money, percent));
        event.setPoints(RewardScale.scale(points, percent));
        event.setExp(RewardScale.scale(exp, percent));
        plugin.countJobsIntervention();
        if (plugin.debugLogging()) {
            plugin.getLogger().info("Jobs payment scaled to " + percent + "% for denied " + entity.getType()
                + " (job " + event.getJob().getName() + "): money " + money + " -> " + event.getAmount()
                + ", points " + points + " -> " + event.getPoints()
                + ", XP " + exp + " -> " + event.getExp() + ".");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExpGain(JobsExpGainEvent event) {
        LivingEntity entity = event.getLivingEntity();
        if (entity == null || !plugin.isDenied(entity)) return;
        int percent = plugin.farmRewardPercent();
        if (percent >= 100) return;
        if (percent <= 0) {
            event.setExp(0.0);
            event.setCancelled(true);
            plugin.countJobsIntervention();
            if (plugin.debugLogging()) {
                plugin.getLogger().info("Jobs XP blocked for denied " + entity.getType()
                    + " (job " + event.getJob().getName() + ", farm-reward-percent 0).");
            }
            return;
        }
        double exp = event.getExp();
        event.setExp(RewardScale.scale(exp, percent));
        plugin.countJobsIntervention();
        if (plugin.debugLogging()) {
            plugin.getLogger().info("Jobs XP scaled to " + percent + "% for denied " + entity.getType()
                + " (job " + event.getJob().getName() + "): XP " + exp + " -> " + event.getExp() + ".");
        }
    }
}
