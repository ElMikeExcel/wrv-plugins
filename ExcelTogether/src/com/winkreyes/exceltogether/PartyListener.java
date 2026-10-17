package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/**
 * Multiplies money and XP on the final JobsPrePaymentEvent amounts (Jobs applies its own boosts
 * afterwards, so the result is base * party * (1 + jobsBoost)). Points are not changed.
 * With share-percent above 0, each nearby party member also gets that percent of the boosted gain,
 * in the same job, paid straight through Jobs (never through JobsPrePaymentEvent, and a re-entry
 * guard makes a shared gain impossible to share again).
 */
public final class PartyListener implements Listener {
    public static final long BAR_THROTTLE_MS = 3_000L, CHAT_THROTTLE_MS = 120_000L;
    private final PartyBonus bonus;
    private final PartyFinder finder;
    private final ShareSink sink;
    private final double sharePercent;
    private final BiConsumer<OfflinePlayer, String> bar, chat;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastBar = new HashMap<>(), lastChat = new HashMap<>();
    private boolean sharing = false;
    public int applied = 0, shared = 0;

    public PartyListener(PartyBonus bonus, PartyFinder finder) { this(bonus, finder, null, 0, null, null, System::currentTimeMillis); }

    public PartyListener(PartyBonus bonus, PartyFinder finder, ShareSink sink, double sharePercent,
                         BiConsumer<OfflinePlayer, String> bar, BiConsumer<OfflinePlayer, String> chat, LongSupplier clock) {
        this.bonus = bonus; this.finder = finder; this.sink = sink;
        this.sharePercent = Double.isFinite(sharePercent) ? Math.max(0, Math.min(sharePercent, 100)) : 0;
        this.bar = bar; this.chat = chat; this.clock = clock;
    }

    private static String jobName(JobsPrePaymentEvent e) {
        try { String n = e.getJob().getName(); return n == null ? "Jobs" : n; } catch (Throwable t) { return "Jobs"; }
    }
    private int pct() { return (int) Math.round((bonus.multiplier() - 1.0) * 100.0); }

    private boolean due(Map<UUID, Long> m, UUID id, long gap) {
        long now = clock.getAsLong(); Long t = m.get(id);
        if (t != null && now - t < gap) return false;
        m.put(id, now); return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPay(JobsPrePaymentEvent e) {
        if (sharing) return;                                   // a shared gain never triggers more sharing or bonus
        if (e.getAmount() <= 0 && e.getExp() <= 0) return;
        OfflinePlayer who = e.getPlayer();
        if (who == null || !finder.hasPartner(who)) return;
        e.setAmount(bonus.apply(e.getAmount()));
        e.setExp(bonus.apply(e.getExp()));
        applied++;
        UUID id = who.getUniqueId();
        String with = finder.partnerName(who);
        if (with == null) with = "your party";
        if (bar != null && due(lastBar, id, BAR_THROTTLE_MS)) bar.accept(who, "+" + pct() + "% party bonus (with " + with + ")");
        if (chat != null && due(lastChat, id, CHAT_THROTTLE_MS))
            chat.accept(who, "Party bonus is on: +" + pct() + "% Jobs money and XP with " + with + ". Type /excelparty status to check it any time.");
        if (sink != null && sharePercent > 0 && e.getJob() != null) {
            double f = sharePercent / 100.0, money = e.getAmount() * f, xp = e.getExp() * f;
            List<OfflinePlayer> rs = finder.sharers(who);
            sharing = true;
            try {
                for (OfflinePlayer r : rs) {
                    if (r == null || r.getUniqueId().equals(id)) continue;
                    try {
                        if (sink.give(r, e.getJob(), money, xp, who.getName() == null ? "a party member" : who.getName())) {
                            shared++;
                            if (bar != null && due(lastBar, r.getUniqueId(), BAR_THROTTLE_MS))
                                bar.accept(r, "+" + pct() + "% party bonus: shared " + jobName(e) + " gain from " + who.getName());
                        }
                    } catch (Throwable t) { /* one bad receiver never breaks the earner's payout */ }
                }
            } finally { sharing = false; }
        }
    }
}
