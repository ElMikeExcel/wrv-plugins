package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.CurrencyType;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Pays a party member their copy straight through Jobs (experience via the player manager so level ups
 * work, money via Jobs' buffered economy). Neither path fires JobsPrePaymentEvent, so no loops.
 * The receiver is credited in the SAME job as the earner and only if they have that job.
 */
public final class JobsShareSink implements ShareSink {
    /** Pure helper: apply a receiver's Jobs boost to a pre-boost money/xp pair. A null boost leaves the amounts alone. */
    static double[] withReceiverBoosts(com.gamingmesh.jobs.container.Boost b, double money, double xp) {
        if (b == null) return new double[]{money, xp};
        return new double[]{b.getFinalAmount(CurrencyType.MONEY, money), b.getFinalAmount(CurrencyType.EXP, xp)};
    }

    @Override public boolean give(OfflinePlayer receiver, Job job, double money, double xp, String fromName) {
        JobsPlayer jp = Jobs.getPlayerManager().getJobsPlayer(receiver.getUniqueId());
        if (jp == null || job == null || jp.getJobProgression(job) == null) return false;
        // The copy is the earner's amount BEFORE any Jobs boost (the earner's own boosts are applied by Jobs to the
        // earner only). Here the RECEIVER's own boosts (global/schedule, permission, item, etc.) go on top, exactly as
        // Jobs would for a normal gain of theirs, so being together on a boosted weekend pays the most.
        double[] f = withReceiverBoosts(Jobs.getPlayerManager().getFinalBonus(jp, job), money, xp);
        money = f[0]; xp = f[1];
        if (xp > 0) Jobs.getPlayerManager().addExperience(jp, job, xp);
        if (money > 0) { Map<CurrencyType, Double> m = new HashMap<>(); m.put(CurrencyType.MONEY, money); Jobs.getEconomy().pay(jp, m); }
        return xp > 0 || money > 0;
    }
}
