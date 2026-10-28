package com.winkreyes.excelbosstrial.api;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.CooldownBook;
import com.winkreyes.excelbosstrial.core.XpBandTokens;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public, read-only state API for ExcelBossTrial.
 *
 * ExcelGuildGate 0.3.1 reads the credit cooldowns through here (by
 * reflection, so it keeps no hard dependency on this plugin) and
 * appends them to /guildgate status - the settled design says the
 * cooldown is "shown in guild status". Every method is null-safe:
 * before the plugin has loaded (or after it disables), there are
 * simply no cooldown lines.
 *
 * The bind/unbind methods are internal wiring used by the plugin
 * itself; consumers should only call the getters.
 */
public final class BossTrialAPI {

    private static volatile CooldownBook book;

    private BossTrialAPI() {
    }

    /** Internal: called by ExcelBossTrial once its state is loaded. */
    public static void bind(CooldownBook loadedBook) {
        book = loadedBook;
    }

    /** Internal: called by ExcelBossTrial on disable. */
    public static void unbind() {
        book = null;
    }

    /** Milliseconds left on a seal's credit cooldown (0 when none). */
    public static long cooldownRemainingMs(String sealId) {
        CooldownBook b = book;
        return b == null ? 0L : b.remainingMs(sealId, System.currentTimeMillis());
    }

    /** True while a seal's credit cooldown is running. */
    public static boolean cooldownActive(String sealId) {
        return cooldownRemainingMs(sealId) > 0L;
    }

    /**
     * Status lines for the seals currently on credit cooldown, in
     * GuildGate's &amp;-color style (its status renderer translates
     * them). Empty when nothing is cooling down.
     */
    public static List<String> statusLines() {
        List<String> out = new ArrayList<>();
        CooldownBook b = book;
        if (b == null) {
            return out;
        }
        long now = System.currentTimeMillis();
        for (BossKind kind : BossKind.values()) {
            long left = b.remainingMs(kind.sealId(), now);
            if (left > 0L) {
                out.add("&7" + kind.displayName() + " seal credit cooldown: &f"
                        + CooldownBook.formatRemaining(left)
                        + " &7left &8(ExcelBossTrial)");
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // XP band governor feed (0.2.4)
    // ------------------------------------------------------------------

    private static volatile XpBandTokens xpFeed;

    /** Internal: called by ExcelBossTrial once its state is loaded. */
    public static void bindFeed(XpBandTokens feed) {
        xpFeed = feed;
    }

    /** Internal: called by ExcelBossTrial on disable. */
    public static void unbindFeed() {
        xpFeed = null;
    }

    /**
     * The XP band governor feed: consumes the oldest unconsumed band
     * verdict for the player's pending governed hits and returns it -
     * Boolean.TRUE when the hit behind the caller's next XP gain
     * counted toward its seal, Boolean.FALSE when it was out-of-band
     * or otherwise not counted - or null when no fresh verdict
     * applies (no governed hit in flight, the verdict expired, or
     * the feed is not bound). Null always means "no opinion": the
     * consumer pays the gain in full (fail-safe).
     *
     * <p>Contract for consumers (ExcelRewardGuard): call exactly
     * once per McMMOPlayerXpGainEvent with XPGainReason.PVE, for
     * that event's player, passing the CURRENT server tick
     * (Bukkit.getCurrentTick()). Verdicts are consume-on-use, kept
     * per player in hit order, and expire
     * {@link XpBandTokens#TTL_TICKS} ticks after their hit, so a
     * verdict can never scale an unrelated later gain. Party shares
     * (SHARED_PVE) derive from the primary gain after the event and
     * must NOT be polled: they inherit the primary's scaling
     * automatically.
     */
    public static Boolean pollXpBandVerdict(UUID player, long currentTick) {
        XpBandTokens feed = xpFeed;
        return feed == null ? null : feed.poll(player, currentTick);
    }

    /** Fresh verdicts queued for the player (diagnostics; consumes nothing). */
    public static int pendingXpBandVerdicts(UUID player, long currentTick) {
        XpBandTokens feed = xpFeed;
        return feed == null ? 0 : feed.pending(player, currentTick);
    }
}

