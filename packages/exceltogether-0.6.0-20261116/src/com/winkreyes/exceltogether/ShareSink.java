package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.container.Job;
import org.bukkit.OfflinePlayer;

/** Gives a party member their copy of an earner's Jobs gain. Must never fire JobsPrePaymentEvent (no loops). */
public interface ShareSink {
    /** @return true if the receiver was credited (false when they do not have that job, etc.). */
    boolean give(OfflinePlayer receiver, Job job, double money, double xp, String fromName);
}
