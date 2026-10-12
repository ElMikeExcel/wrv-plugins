package com.winkreyes.wealthboard;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.entity.Player;

/** Optional Jobs bridge; no job limits or economy configuration are changed. */
final class JobsEnrollment {
    private JobsEnrollment() { }

    static void enrollAll(Player player) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jobsPlayer == null) {
            return;
        }
        for (Job job : Jobs.getJobs()) {
            if (!jobsPlayer.isInJob(job)) {
                Jobs.getPlayerManager().joinJob(jobsPlayer, job);
            }
        }
    }
}
