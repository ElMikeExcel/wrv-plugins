package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.event.MageLoadEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.store.PlayerRecord;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExcelMagicLevel — reconcile-on-load (SPEC §7.2). MageLoadEvent fires
 * after a mage's data is loaded — including the transient modifiers Magic
 * itself restored from its save — so this is the one place that can tell
 * "Magic restored the boost" apart from "we had to re-apply it", and the
 * log line says which. Also repairs any drift (a curve retune re-derives
 * levels; the next load swaps modifiers to match).
 */
public final class MageLoadListener implements Listener {
    private final Logger logger;
    private final PlayerStore store;
    private final Curve curve;
    private final BoostApplier boosts;
    private final boolean verbose;

    public MageLoadListener(Logger logger, PlayerStore store, Curve curve, BoostApplier boosts, boolean verbose) {
        this.logger = logger;
        this.store = store;
        this.curve = curve;
        this.boosts = boosts;
        this.verbose = verbose;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMageLoad(MageLoadEvent event) {
        try {
            Mage mage = event.getMage();
            Player player = mage == null ? null : mage.getPlayer();
            if (player == null) return;
            PlayerRecord record = store.find(player.getUniqueId());
            int level = curve.levelFor(record == null ? 0 : record.totalXp());
            BoostApplier.ReconcileOutcome outcome = boosts.reconcile(mage, level);
            if (!outcome.removed().isEmpty() || outcome.added() != null) {
                logger.info("ExcelMagicLevel reconcile " + player.getName() + " (Magic Level " + level + "): removed "
                        + outcome.removed() + ", added " + outcome.added());
            } else if (verbose) {
                logger.info("ExcelMagicLevel reconcile " + player.getName() + " (Magic Level " + level
                        + "): modifier state already correct — nothing applied by us"
                        + (level >= 2 ? " (boost present, restored by Magic)" : ""));
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "ExcelMagicLevel: reconcile on mage load failed", t);
        }
    }
}
