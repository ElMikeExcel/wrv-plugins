/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 0.2.11 (B-2): the Bukkit half of the AUTO-review immunity frames.
 * The tick math lives in ReviewFramesPolicy (pure, unit-tested); this
 * class only moves session notifications from PersonalClaimsUi into
 * policies and enforces the current state on damage/interact events.
 *
 * Frames bind to the LIVE view: a FRAMED policy protects only while
 * the player's top inventory is still that AUTO session's view (the
 * PersonalClaimsUi cross-check), so a policy can never outlive the
 * menu it belongs to. Notifications carry the view's token: a close
 * notice for a view that has already been replaced is stale and is
 * ignored, which is what lets paging inside one session keep its
 * original window.
 */
public final class ReviewFrames implements Listener, PersonalClaimsUi.SessionListener {
    private final PersonalClaimsUi ui;
    private final int maxTicks;
    private final int graceTicks;
    private final Map<UUID, ReviewFramesPolicy> policies = new HashMap<>();
    private final Map<UUID, Long> liveTokens = new HashMap<>();

    public ReviewFrames(PersonalClaimsUi ui, int maxTicks, int graceTicks) {
        this.ui = Objects.requireNonNull(ui);
        this.maxTicks = Math.max(0, maxTicks);
        this.graceTicks = Math.max(0, graceTicks);
    }

    // ---- PersonalClaimsUi.SessionListener ----

    @Override public void opened(UUID owner, PersonalClaimsUi.Origin origin, long token) {
        liveTokens.put(owner, token);
        policy(owner).sessionOpened(origin == PersonalClaimsUi.Origin.AUTO, Bukkit.getCurrentTick());
    }

    @Override public void closed(UUID owner, PersonalClaimsUi.Origin origin, long token) {
        Long live = liveTokens.get(owner);
        if (live == null || live.longValue() != token) {
            return; // Stale close for a replaced view; the live session stands.
        }
        liveTokens.remove(owner);
        policy(owner).sessionClosed(Bukkit.getCurrentTick());
    }

    private ReviewFramesPolicy policy(UUID owner) {
        return policies.computeIfAbsent(owner, k -> new ReviewFramesPolicy(maxTicks, graceTicks));
    }

    /** Framed right now: policy inside its window AND the AUTO view still on top. */
    public boolean framed(Player player) {
        ReviewFramesPolicy policy = policies.get(player.getUniqueId());
        return policy != null && policy.framedAt(Bukkit.getCurrentTick()) && ui.isAutoViewOpen(player);
    }

    /** In the post-close grace: damage-in only, no view required (it is closed). */
    public boolean inGrace(Player player) {
        ReviewFramesPolicy policy = policies.get(player.getUniqueId());
        return policy != null && policy.inGrace(Bukkit.getCurrentTick());
    }

    // ---- Enforcement ----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void damageIn(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && (framed(player) || inGrace(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void damageOut(EntityDamageByEntityEvent event) {
        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            damager = player;
        }
        if (damager == null) {
            return;
        }
        if (framed(damager)) {
            event.setCancelled(true);
            return;
        }
        // Not framed: a hit landed from inside the close grace ends
        // the grace on the spot (the hit itself stands).
        ReviewFramesPolicy policy = policies.get(damager.getUniqueId());
        if (policy != null) {
            policy.damageDealt(Bukkit.getCurrentTick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (framed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
