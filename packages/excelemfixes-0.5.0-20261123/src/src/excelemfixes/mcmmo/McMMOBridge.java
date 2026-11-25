package excelemfixes.mcmmo;

import com.gmail.nossr50.api.PartyAPI;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import com.gmail.nossr50.util.player.UserManager;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Bridge to mcMMO: the per-damage veto, the ceiling observer,
 * and the award grant. This class is only constructed when the
 * mcMMO plugin is present (the pack's bridge discipline, same
 * as the jobs-dummy-gate's Jobs bridge).
 *
 * <p><b>How the veto works.</b> mcMMO's gain events carry no
 * target entity, so a gain cannot be vetoed "because it came
 * from that elite" directly. The engine plants a per-hit token
 * for every hit on an EliteMobs entity; this listener polls
 * the engine (through {@link XpPort.Hooks}) for every PVE and
 * SHARED_PVE gain and zeroes the ones a token claims. Zeroing
 * is done on {@link McMMOPlayerXpGainEvent} at LOWEST —
 * {@code setRawXpGained(0)} plus cancel — because that event's
 * raw value is what mcMMO's {@code EventUtils.handleXpGainEvent}
 * finally writes to the profile after all listeners run (the
 * same placement ExcelRewardGuard's mcMMO veto uses and
 * verifies). The Pre-gain event is deliberately NOT used: in
 * this mcMMO build its cancellation is never read back by the
 * pipeline (amount-only, and integer-truncated).
 *
 * <p><b>Shares.</b> mcMMO mints party shares synchronously
 * inside the earner's gain call, before the earner's own
 * event, each share through the receiving member's own event
 * with reason SHARED_PVE. There is no single event whose
 * cancellation stops a share — each share is vetoed at its
 * own event, which is why the hooks ask per-recipient.
 *
 * <p><b>The grant.</b> Awards go through
 * {@code McMMOPlayer.beginXpGain} with reason PVE and source
 * CUSTOM — the full public pipeline: party sharing, per-player
 * rate modifiers, and every other plugin's gain listeners
 * (including the house catch-up multiplier, which scales the
 * XP gain event) compose exactly as they do for combat gains.
 * The grant runs synchronously, so a ThreadLocal marks the
 * module's own grant events: the veto must never eat an award,
 * and the MONITOR observer reads the earner's final
 * post-pipeline amount off the event and reports it to the
 * engine's ceiling ledger — the ceiling counts what was
 * actually granted, after multipliers.
 *
 * <p><b>No world gate (Nov 23, 2026).</b> There is no
 * world-based rule anywhere in the pair: the fork's Nov 15
 * blanket world gates were a temporary stopgap and are
 * deleted, and this module deliberately does not re-create
 * one. Its rules are target-based only (the token veto above,
 * kill awards, the award ceiling). Ordinary mcMMO play —
 * mining, fishing, gathering — pays normal rates in every
 * world, EliteMobs worlds included, and awards pay instantly
 * wherever they are earned because nothing drops them.
 *
 * <p><b>The watchdog.</b> The MONITOR observer also reports
 * every written gain to the engine's XP watchdog, which
 * keeps per-player per-skill rolling minute/hour totals:
 * crossing a flag threshold writes a flag record (never
 * blocks), and crossing the hourly cap arms a blocked state
 * this listener enforces — gains in that skill are zeroed
 * until the rolling hour falls back under the cap. That is
 * the safety net for gain paths nobody foresaw: bounded
 * within the hour, and named in the flag record for review.
 */
public final class McMMOBridge implements XpPort, Listener {

    private final Logger logger;
    private final Hooks hooks;

    /**
     * The in-flight award grant on this thread, if any. The
     * grant call is synchronous, so every event fired while
     * this is set was minted by that call's own pipeline —
     * the award's earner event and its party share copies.
     */
    private static final class GrantWindow {
        final UUID earner;
        final String skill;
        boolean earnerEventPassed;

        GrantWindow(UUID earner, String skill) {
            this.earner = earner;
            this.skill = skill;
        }
    }

    private final ThreadLocal<GrantWindow> granting = new ThreadLocal<>();

    public McMMOBridge(Logger logger, Hooks hooks) {
        this.logger = logger;
        this.hooks = hooks;
    }

    // ------------------------------------------------------------ grants

    @Override
    public boolean grant(UUID playerId, String skillName, int amount) {
        if (amount <= 0) return false;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return false;
        final PrimarySkillType skill;
        try {
            skill = PrimarySkillType.valueOf(skillName);
        } catch (IllegalArgumentException e) {
            logger.warning("mcMMO XP Connector: unknown mcMMO skill '"
                    + skillName + "'; award not granted.");
            return false;
        }
        McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) return false;
        granting.set(new GrantWindow(playerId, skillName));
        try {
            mmoPlayer.beginXpGain(skill, amount,
                    XPGainReason.PVE, XPGainSource.CUSTOM);
        } finally {
            granting.remove();
        }
        return true;
    }

    @Override
    public boolean sameParty(UUID first, UUID second) {
        try {
            Player a = Bukkit.getPlayer(first);
            Player b = Bukkit.getPlayer(second);
            if (a == null || b == null) return false;
            return PartyAPI.inSameParty(a, b);
        } catch (Throwable t) {
            return false;
        }
    }

    // -------------------------------------------------------------- veto

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onXpGain(McMMOPlayerXpGainEvent event) {
        XPGainReason reason = event.getXpGainReason();
        UUID player = event.getPlayer().getUniqueId();
        String skill = event.getSkill().name();

        // The watchdog cap is checked before the grant window:
        // a capped player+skill earns nothing at all, awards
        // included (a capped award produces no event, so the
        // engine queues it and it pays after roll-off).
        if (hooks.watchdogCapped(player, skill)) {
            event.setRawXpGained(0.0f);
            event.setCancelled(true);
            return;
        }

        GrantWindow window = granting.get();
        if (window != null) {
            // Inside our own synchronous award grant: pass the
            // events that ARE the award — the earner's first
            // matching event (their PVE event solo, or their
            // SHARED_PVE share copy when a party split it) and
            // party members' SHARED_PVE share copies. Anything
            // else falls through to the normal vetoes below.
            if (player.equals(window.earner)
                    && skill.equals(window.skill)
                    && !window.earnerEventPassed
                    && (reason == XPGainReason.PVE
                        || reason == XPGainReason.SHARED_PVE)) {
                window.earnerEventPassed = true;
                return;
            }
            if (!player.equals(window.earner)
                    && reason == XPGainReason.SHARED_PVE) {
                return;
            }
        }

        boolean veto;
        if (reason == XPGainReason.PVE) {
            veto = hooks.vetoGain(player, skill);
        } else if (reason == XPGainReason.SHARED_PVE) {
            veto = hooks.vetoShare(player, skill);
        } else {
            return;
        }
        if (veto) {
            event.setRawXpGained(0.0f);
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------- ceiling observing

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXpGainMonitor(McMMOPlayerXpGainEvent event) {
        // The watchdog sees every gain the pipeline finally
        // wrote (cancelled events never reach this handler).
        hooks.observedGain(event.getPlayer().getUniqueId(),
                event.getSkill().name(), event.getRawXpGained(),
                event.getPlayer().getWorld().getName());

        GrantWindow window = granting.get();
        if (window == null) return;
        if (!event.getPlayer().getUniqueId().equals(window.earner)) return;
        if (!event.getSkill().name().equals(window.skill)) return;
        // The earner's own event: PVE when solo; when a party
        // split the award, the earner's copy arrives as
        // SHARED_PVE instead — that copy is what the earner
        // actually received, so it is what the ceiling counts.
        XPGainReason reason = event.getXpGainReason();
        if (reason != XPGainReason.PVE
                && reason != XPGainReason.SHARED_PVE) return;
        hooks.grantedFinal(window.earner, event.getSkill().name(),
                event.getRawXpGained());
    }
}
