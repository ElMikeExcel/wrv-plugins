package com.winkreyesvalley.excelmagiclevel;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.BoostApplier;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.MagicBridge;
import com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup;
import com.winkreyesvalley.excelmagiclevel.adapter.weekend.WeekendSignal;
import com.winkreyesvalley.excelmagiclevel.core.AntiAbuseWindows;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;
import com.winkreyesvalley.excelmagiclevel.core.ShareDistribution;
import com.winkreyesvalley.excelmagiclevel.core.ShareMath;
import com.winkreyesvalley.excelmagiclevel.core.XpMath;
import com.winkreyesvalley.excelmagiclevel.store.PlayerRecord;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import com.winkreyesvalley.excelmagiclevel.surface.FeedbackService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExcelMagicLevel — the award path: one evaluated cast in, XP + level +
 * boost + feedback out. Order follows SPEC §4.6 exactly (window → target
 * scale → weekend multiplier → floor). A payment of 0 records nothing and
 * resets nothing (Magic's own earn semantics). A level change saves the
 * store immediately and swaps the mage's modifier in the same tick.
 *
 * <p>Party share (SPEC §4.5): after the earner's own award lands, each
 * party member in range receives a copy of the earner's FINAL amount
 * (post-window, post-multiplier), computed by core.ShareMath and gated by
 * core.ShareDistribution (same world, within radius). Copies are applied
 * by {@link #applyShare} — a terminal method with no path back into
 * {@link #awardOwn} or distribution, so a copy can never re-share: the
 * SPEC's re-entry guard is structural, not a flag. The weekend multiplier
 * applies to the earner's own gain only (SPEC §4.6); it is already inside
 * the final amount a copy is taken from, and is never applied again.</p>
 */
public final class AwardService {
    private final Logger logger;
    private final PlayerStore store;
    private final Curve curve;
    private final RewardTable rewards;
    private final AntiAbuseWindows windows;
    private final WeekendSignal weekendSignal;
    private final double weekendRate;
    private final long earnWindowMillis;
    private final long targetWindowMillis;
    private final double targetRepeatScale;
    private final MagicBridge magic;
    private final BoostApplier boosts;
    private final FeedbackService feedback;
    private final PartyLookup partyLookup;
    private final boolean shareEnabled;
    private final int sharePercent;
    private final double shareRadius;

    public AwardService(Logger logger, PlayerStore store, Curve curve, RewardTable rewards,
                        AntiAbuseWindows windows, WeekendSignal weekendSignal, double weekendRate,
                        long earnWindowMillis, long targetWindowMillis, double targetRepeatScale,
                        MagicBridge magic, BoostApplier boosts, FeedbackService feedback,
                        PartyLookup partyLookup, boolean shareEnabled, int sharePercent, double shareRadius) {
        this.logger = logger;
        this.store = store;
        this.curve = curve;
        this.rewards = rewards;
        this.windows = windows;
        this.weekendSignal = weekendSignal;
        this.weekendRate = weekendRate;
        this.earnWindowMillis = earnWindowMillis;
        this.targetWindowMillis = targetWindowMillis;
        this.targetRepeatScale = targetRepeatScale;
        this.magic = magic;
        this.boosts = boosts;
        this.feedback = feedback;
        this.partyLookup = partyLookup;
        this.shareEnabled = shareEnabled;
        this.sharePercent = sharePercent;
        this.shareRadius = shareRadius;
    }

    /**
     * Evaluate and pay one real, successful, wand cast.
     *
     * @return the XP paid (0 = the windows ate it)
     */
    public int awardOwn(UUID playerId, String spellKey, int weight, UUID targetId) {
        PlayerRecord record = store.record(playerId);
        record.addCasts(1);
        store.markDirty();

        long now = System.currentTimeMillis();
        Long elapsed = windows.spellElapsed(playerId, spellKey, now);
        boolean targetRepeat = windows.isTargetRepeat(playerId, targetId, now, targetWindowMillis);
        double multiplier = weekendSignal.isActive(Instant.now()) ? weekendRate : 1.0;
        int pay = XpMath.payment(weight, elapsed, earnWindowMillis, targetRepeat, targetRepeatScale, multiplier);
        if (pay <= 0) return 0;

        windows.recordPay(playerId, spellKey, targetId, now);
        int oldLevel = curve.levelFor(record.totalXp());
        record.addXp(pay);
        store.markDirty();
        int newLevel = curve.levelFor(record.totalXp());

        Player player = Bukkit.getPlayer(playerId);
        if (newLevel != oldLevel) {
            try {
                store.saveNow();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "ExcelMagicLevel: immediate save on level-up failed", ex);
            }
            if (player != null) {
                Mage mage = magic.mage(player);
                if (mage != null) {
                    boosts.reconcile(mage, newLevel);
                }
                feedback.onLevelUp(player, newLevel, rewards.boostAt(newLevel), rewards.isMilestone(newLevel));
            }
        } else if (player != null) {
            feedback.onGain(player, pay, false);
        }

        distributeShare(playerId, pay);
        return pay;
    }

    /**
     * Copy the earner's FINAL amount to every party member the core rules
     * admit (SPEC §4.5). A lookup failure degrades this payment's share to
     * nothing — it never touches the earner's own award, which has already
     * landed (fail-soft, SPEC §9.2).
     */
    private void distributeShare(UUID earnerId, int finalPay) {
        if (!shareEnabled || finalPay <= 0 || partyLookup == null) return;
        int copy = ShareMath.copyAmount(finalPay, sharePercent);
        if (copy <= 0) return;
        Player earner = Bukkit.getPlayer(earnerId);
        if (earner == null) return;

        Set<UUID> members;
        try {
            members = partyLookup.partyMembers(earnerId);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "ExcelMagicLevel: party lookup failed — no share copies for this cast "
                    + "(own XP unaffected)", t);
            return;
        }
        if (members == null || members.size() <= 1) return;

        Location earnerLoc = earner.getLocation();
        ShareDistribution.MemberView earnerView = new ShareDistribution.MemberView(
                earnerId, earner.getWorld().getName(), earnerLoc.getX(), earnerLoc.getY(), earnerLoc.getZ());
        List<ShareDistribution.MemberView> candidates = new ArrayList<>();
        for (UUID memberId : members) {
            if (memberId.equals(earnerId)) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue; // offline members receive nothing
            Location loc = member.getLocation();
            candidates.add(new ShareDistribution.MemberView(
                    memberId, member.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        }
        for (UUID recipientId : ShareDistribution.recipients(earnerView, candidates, shareRadius)) {
            applyShare(recipientId, copy);
        }
    }

    /**
     * Apply one share copy to one receiver. Terminal by construction: no
     * windows are consulted (the copy already carries the earner's
     * diminishing, SPEC §4.5), no cast is counted (a copy is not a cast),
     * and this method never distributes further copies — copies never
     * re-share.
     */
    private void applyShare(UUID receiverId, int amount) {
        PlayerRecord record = store.record(receiverId);
        int oldLevel = curve.levelFor(record.totalXp());
        record.addXp(amount);
        store.markDirty();
        int newLevel = curve.levelFor(record.totalXp());

        Player receiver = Bukkit.getPlayer(receiverId);
        if (newLevel != oldLevel) {
            try {
                store.saveNow();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "ExcelMagicLevel: immediate save on share level-up failed", ex);
            }
            if (receiver != null) {
                Mage mage = magic.mage(receiver);
                if (mage != null) {
                    boosts.reconcile(mage, newLevel);
                }
                feedback.onLevelUp(receiver, newLevel, rewards.boostAt(newLevel), rewards.isMilestone(newLevel));
            }
        } else if (receiver != null) {
            feedback.onGain(receiver, amount, true);
        }
    }
}
