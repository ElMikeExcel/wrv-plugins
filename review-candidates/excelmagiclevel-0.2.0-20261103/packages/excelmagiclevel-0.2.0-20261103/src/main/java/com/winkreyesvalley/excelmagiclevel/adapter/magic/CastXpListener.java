package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.winkreyesvalley.excelmagiclevel.AwardService;
import com.winkreyesvalley.excelmagiclevel.core.WeightTable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExcelMagicLevel — the CastEvent listener (SPEC §4.1). MONITOR priority,
 * strictly read-only: Magic fires this from the same {@code finish()} that
 * pays SP, after the cast is complete, so nothing here can alter the cast.
 * The whole body is wrapped so an exception in our code can NEVER fail a
 * cast (SPEC §7.4) — worst case, one cast pays no XP and a warning lands
 * in the log.
 *
 * <p>Gates, in order: real player → wand in hand → paying result
 * (ResultPolicy) → weight &gt; 0. Boss-fight casts pay like any other
 * (SPEC §4.3: {@code xp.bosstrial-enabled: true} is the settled default —
 * a seal fight is the realest play there is; the off position would need
 * a BossTrial adapter to detect a trial, which this build deliberately
 * does not fake — see the enable-time warning in the plugin).</p>
 */
public final class CastXpListener implements Listener {
    private final Logger logger;
    private final AwardService awards;
    private final WeightTable weights;
    private final String debugThrowOnSpellKey;
    private final boolean verbose;

    public CastXpListener(Logger logger, AwardService awards, WeightTable weights,
                          String debugThrowOnSpellKey, boolean verbose) {
        this.logger = logger;
        this.awards = awards;
        this.weights = weights;
        this.debugThrowOnSpellKey = debugThrowOnSpellKey == null ? "" : debugThrowOnSpellKey;
        this.verbose = verbose;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent event) {
        try {
            handle(event);
        } catch (Throwable t) {
            // By construction this can never reach Magic's cast path.
            logger.log(Level.WARNING, "ExcelMagicLevel: XP evaluation failed for a cast (cast itself unaffected)", t);
        }
    }

    private void handle(CastEvent event) {
        CastContext context = event.getContext();
        Mage mage = event.getMage();
        Player player = mage == null ? null : mage.getPlayer();
        if (player == null) return; // automata / mob mages / console casts earn nothing
        if (context.getWand() == null) {
            if (verbose) {
                logger.info("ExcelMagicLevel cast probe: " + player.getName() + " cast "
                        + event.getSpell().getKey() + " -> " + event.getSpellResult() + " (wandless: pays 0)");
            }
            return; // wandless utility earns nothing (SPEC §7.4)
        }
        if (!ResultPolicy.pays(event.getSpellResult())) {
            if (verbose) {
                logger.info("ExcelMagicLevel cast probe: " + player.getName() + " cast "
                        + event.getSpell().getKey() + " -> " + event.getSpellResult() + " (non-paying result)");
            }
            return;
        }
        String spellKey = event.getSpell().getKey();
        if (!debugThrowOnSpellKey.isEmpty() && debugThrowOnSpellKey.equals(spellKey)) {
            // Rig-only seam (config debug.throw-on-spell-key): proves a
            // failure inside this listener cannot break the cast.
            throw new IllegalStateException("ExcelMagicLevel rig probe: deliberate listener failure on " + spellKey);
        }
        int weight = weights.weightFor(spellKey);
        if (weight <= 0) return;
        Entity target = context.getTargetEntity();
        UUID targetId = target == null ? null : target.getUniqueId();
        int paid = awards.awardOwn(player.getUniqueId(), spellKey, weight, targetId);
        if (verbose) {
            logger.info("ExcelMagicLevel cast probe: " + player.getName() + " cast " + spellKey
                    + " -> " + event.getSpellResult() + ", weight " + weight + ", paid " + paid + " XP");
        }
    }
}
