package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.guildgate.api.SealCreditEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * The credit gate between ExcelGuildGate and ExcelBossTrial.
 *
 * ExcelGuildGate 0.3.1 fires SealCreditEvent from its death handler
 * BEFORE crediting a boss seal, and credits only if nobody cancels.
 * This hook cancels every credit ExcelBossTrial does not approve
 * (share gate, presence rule, cooldown, elder trio incomplete).
 *
 * The class lives apart from the plugin main class on purpose: it is
 * only registered when the ExcelGuildGate plugin is actually
 * installed, so ExcelBossTrial loads cleanly without it.
 */
final class SealCreditHook implements Listener {

    private final ExcelBossTrial plugin;

    SealCreditHook(ExcelBossTrial plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSealCredit(SealCreditEvent event) {
        BossKind kind = BossKind.fromSealId(event.getSealId());
        if (kind == null) {
            return; // the raid seal (and anything else) is never ours
        }
        if (!plugin.approvesCredit(kind, event.getEntity())) {
            event.setCancelled(true);
            plugin.getLogger().info("BossTrial VETOED the " + event.getSealId()
                    + " seal credit for killer " + event.getKillerName()
                    + " (share / presence / cooldown / trio rules in the death log).");
        }
    }
}

