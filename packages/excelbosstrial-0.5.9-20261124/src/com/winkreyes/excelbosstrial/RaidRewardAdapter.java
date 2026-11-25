/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.winkreyes.guildgate.api.RaidBreakEvent;
import com.winkreyes.guildgate.api.RaidRepeatEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * The BossTrial side of the raid-repeat companion (0.3.0,
 * RAID-COMPANION-SPEC): GuildGate judges and fires; this adapter
 * pays. GuildGate never pays anything itself - the signature and
 * the repeat slate ride BossTrial's claims path, exactly like the
 * boss chapters.
 *
 * Like {@link SealCreditHook}, the class is only LOADED when
 * GuildGate is installed AND its 0.4.0 event classes verify, so
 * BossTrial runs unchanged against an older GuildGate (the raid
 * then credits first-break only, as it always did).
 */
final class RaidRewardAdapter implements Listener {

    private final ExcelBossTrial plugin;

    private RaidRewardAdapter(ExcelBossTrial plugin) {
        this.plugin = plugin;
    }

    /** Verify-then-register: the event classes must exist and
     *  carry the settled accessor shapes before this class is
     *  instantiated. Any failure leaves the raid on its 0.3.4
     *  behavior and says so once in the log. */
    static boolean register(ExcelBossTrial plugin) {
        Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
        if (gg == null) {
            return false;
        }
        // Presence, not isEnabled: BossTrial can enable BEFORE
        // GuildGate (the SealCreditHook precedent) - the event
        // classes load from its classloader either way, and the
        // events themselves only fire once both are live.
        try {
            ClassLoader loader = gg.getClass().getClassLoader();
            Class<?> breakEvent = Class.forName(
                    "com.winkreyes.guildgate.api.RaidBreakEvent", true, loader);
            Class<?> repeatEvent = Class.forName(
                    "com.winkreyes.guildgate.api.RaidRepeatEvent", true, loader);
            breakEvent.getMethod("winners");
            breakEvent.getMethod("omenLevel");
            repeatEvent.getMethod("winners");
            repeatEvent.getMethod("clearCounts");
            repeatEvent.getMethod("windowResetMs");
        } catch (Throwable missing) {
            plugin.getLogger().warning("GuildGate raid repeat events not available ("
                    + missing.getMessage() + "): raid pays first-break only.");
            return false;
        }
        Bukkit.getPluginManager().registerEvents(new RaidRewardAdapter(plugin), plugin);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidBreak(RaidBreakEvent event) {
        plugin.raidBreakPay(event.winners(), event.omenLevel());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidRepeat(RaidRepeatEvent event) {
        plugin.raidRepeatPay(event.winners(), event.clearCounts());
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
