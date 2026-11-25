package excelemfixes.fieldmedic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure Field Bandage state machine (no Bukkit): one running
 * heal-over-time per player plus that player's bandage cooldown.
 * Time is injected (millis) so unit tests drive it
 * deterministically. The Bukkit layer converts the engine's
 * percent offers into HP against the player's CURRENT max health
 * and delivers them through EntityRegainHealthEvent.
 *
 * <p>Two stores on purpose:
 * <ul>
 *   <li>The heal-over-time is a <em>benefit in progress</em>: it
 *       lives only here, in memory. Quitting ends it (a relog must
 *       not bank a heal), death clears it.</li>
 *   <li>The cooldown is a <em>restriction</em>: it must survive a
 *       relog, so the Bukkit layer mirrors its expiry into the
 *       player's persistent data at use and hands it back through
 *       {@link #restoreCooldown} when the player returns. Death
 *       clears it (a fresh life gets a fresh decision).</li>
 * </ul>
 *
 * <p>The delivery schedule is time-based: at elapsed second k the
 * cumulative due is min(healPercent, perSecond x k), and a tick
 * offers the not-yet-offered difference. A lagged tick therefore
 * catches up instead of losing healing, and the total offered over
 * the window is always exactly healPercent — never more.
 */
public final class FieldMedicEngine {

    /** Tunables (config-driven in the plugin; defaults are the D195 design constants). */
    public record Settings(double healPercent, int durationSeconds, long cooldownMillis) {
        public Settings {
            if (!(healPercent > 0.0) || healPercent > 100.0)
                throw new IllegalArgumentException("healPercent must be in (0, 100]: " + healPercent);
            if (durationSeconds < 1)
                throw new IllegalArgumentException("durationSeconds must be >= 1: " + durationSeconds);
            if (cooldownMillis < 0)
                throw new IllegalArgumentException("cooldownMillis must be >= 0: " + cooldownMillis);
        }

        /** Heal offered per second of the window. */
        public double perSecond() {
            return healPercent / durationSeconds;
        }

        public long durationMillis() {
            return durationSeconds * 1000L;
        }
    }

    public enum UseResult {
        /** The bandage was consumed; the heal-over-time is running. */
        STARTED,
        /** A heal-over-time from an earlier bandage is still running. */
        REFUSED_ACTIVE,
        /** The bandage cooldown from an earlier use is still running. */
        REFUSED_COOLDOWN
    }

    public record UseOutcome(UseResult result, long cooldownRemainingMillis) {}

    private static final class State {
        long hotStartMillis = -1L;   // -1 = no heal-over-time running
        double offeredPercent;       // cumulative % the schedule has offered
        long cooldownUntilMillis;    // 0 = no cooldown
    }

    private final Settings settings;
    private final Map<UUID, State> states = new HashMap<>();

    public FieldMedicEngine(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    /**
     * Attempt to use a bandage. On STARTED the caller consumes the
     * item and mirrors {@link #cooldownUntilMillis} into the
     * player's persistent data. On either refusal the caller
     * consumes NOTHING — a misclick never eats a purchased item.
     */
    public UseOutcome tryUse(UUID player, long nowMillis) {
        State state = states.computeIfAbsent(player, k -> new State());
        if (isActive(player, nowMillis)) {
            return new UseOutcome(UseResult.REFUSED_ACTIVE, cooldownRemainingMillis(player, nowMillis));
        }
        long remaining = state.cooldownUntilMillis - nowMillis;
        if (remaining > 0) {
            return new UseOutcome(UseResult.REFUSED_COOLDOWN, remaining);
        }
        state.hotStartMillis = nowMillis;
        state.offeredPercent = 0.0;
        state.cooldownUntilMillis = nowMillis + settings.cooldownMillis();
        return new UseOutcome(UseResult.STARTED, settings.cooldownMillis());
    }

    /**
     * The percent of max health to deliver on this tick (0 when
     * nothing is due). Call once per second per bandaging player.
     * The final due tick completes the heal: afterwards
     * {@link #isActive} is false and later ticks offer 0.
     */
    public double tick(UUID player, long nowMillis) {
        State state = states.get(player);
        if (state == null || state.hotStartMillis < 0) return 0.0;
        long elapsedMillis = nowMillis - state.hotStartMillis;
        if (elapsedMillis < 1000L) return 0.0;
        long elapsedSeconds = elapsedMillis / 1000L;
        double due = Math.min(settings.healPercent(),
                settings.perSecond() * elapsedSeconds);
        double offer = due - state.offeredPercent;
        if (offer <= 0.0) return 0.0;
        state.offeredPercent = due;
        if (state.offeredPercent >= settings.healPercent() - 1e-9) {
            state.hotStartMillis = -1L; // schedule complete
        }
        return offer;
    }

    /** True while a heal-over-time is inside its delivery window. */
    public boolean isActive(UUID player, long nowMillis) {
        State state = states.get(player);
        if (state == null || state.hotStartMillis < 0) return false;
        if (state.offeredPercent >= settings.healPercent() - 1e-9) return false;
        return nowMillis - state.hotStartMillis < settings.durationMillis();
    }

    /** Players with a heal-over-time state (the tick loop's work set). */
    public Set<UUID> bandagingPlayers() {
        Set<UUID> out = new java.util.HashSet<>();
        for (Map.Entry<UUID, State> e : states.entrySet()) {
            if (e.getValue().hotStartMillis >= 0) out.add(e.getKey());
        }
        return Set.copyOf(out);
    }

    public long cooldownUntilMillis(UUID player) {
        State state = states.get(player);
        return state == null ? 0L : state.cooldownUntilMillis;
    }

    public long cooldownRemainingMillis(UUID player, long nowMillis) {
        return Math.max(0L, cooldownUntilMillis(player) - nowMillis);
    }

    /**
     * Re-apply a cooldown expiry read back from the player's
     * persistent data (the relog path). Only ever extends: a
     * shorter stored value never shortens a live cooldown.
     */
    public void restoreCooldown(UUID player, long cooldownUntilMillis) {
        State state = states.computeIfAbsent(player, k -> new State());
        if (cooldownUntilMillis > state.cooldownUntilMillis) {
            state.cooldownUntilMillis = cooldownUntilMillis;
        }
    }

    /** End the heal-over-time only (quit). The cooldown stands. */
    public void endHeal(UUID player) {
        State state = states.get(player);
        if (state != null) state.hotStartMillis = -1L;
    }

    /** Drop heal and cooldown both (death, or the player is gone for good). */
    public void forget(UUID player) {
        states.remove(player);
    }
}
