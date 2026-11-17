package com.winkreyes.excelwitherskulls;

import com.winkreyes.excelwitherskulls.core.SkullMath;
import com.winkreyes.excelwitherskulls.core.SkullSettings;
import com.winkreyes.excelwitherskulls.core.TopUpDecider;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The death hook. It runs at HIGHEST — after vanilla has populated
 * the drops list (the skull roll, Looting included, happens before
 * the event fires) and after EliteMobs' own loot handling (which
 * runs on its EliteMobDeathEvent at default priority, and which
 * for natural elites never touches the vanilla list at all: its
 * clear-the-drops path is gated on isVanillaLoot() == false, a
 * custom-boss-only state — verified in the 10.9.10 bytecode during
 * the Oct 21 rewards audit). So the list seen here is the FINAL
 * vanilla list, and the plugin's only write is appending at most
 * one skull to it. It never removes, reorders or duplicates a
 * drop, and it never fires for a non-player kill: getKiller() is
 * Bukkit's own player-kill attribution (a player-shot projectile
 * counts, a dispenser projectile does not), the same attribution
 * vanilla's killed-by-player skull gate uses.
 */
final class SkullListener implements Listener {

    private final ExcelWitherSkulls plugin;
    private final EmGateway gateway;

    SkullListener(ExcelWitherSkulls plugin, EmGateway gateway) {
        this.plugin = plugin;
        this.gateway = gateway;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.getType() != EntityType.WITHER_SKELETON) return;

        SkullSettings settings = plugin.settings();
        Player killer = dead.getKiller();
        boolean playerKill = killer != null;
        int looting = 0;
        if (playerKill) {
            ItemStack hand = killer.getInventory().getItemInMainHand();
            if (hand != null) looting = hand.getEnchantmentLevel(Enchantment.LOOTING);
        }
        boolean skullPresent = false;
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && drop.getType() == Material.WITHER_SKELETON_SKULL
                    && drop.getAmount() > 0) {
                skullPresent = true;
                break;
            }
        }
        // The one random draw of the plugin, from [0, 100).
        double roll = ThreadLocalRandom.current().nextDouble(100.0);

        boolean elite = gateway.isElite(dead);

        TopUpDecider.Verdict verdict = TopUpDecider.decide(
                settings.enabled(),
                true,
                elite,
                playerKill,
                skullPresent,
                settings.targetChancePercent(),
                looting,
                roll);

        switch (verdict) {
            case ADD_SKULL -> {
                event.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
                debug(settings, killer, dead, looting, roll, "ADDED 1 skull");
            }
            // These three verdicts are only reachable on the elite
            // player-kill path (the decider's order guarantees it),
            // so they always earn a debug line.
            case ROLL_MISS, TARGET_NOT_ABOVE_VANILLA, SKULL_ALREADY_PRESENT ->
                    debug(settings, killer, dead, looting, roll, verdict.name());
            // An elite that died WITHOUT a player kill is worth a
            // debug line (it exercises the player-kill gate);
            // ordinary non-elite deaths — the common case — and the
            // disabled state stay silent even in debug.
            case NOT_PLAYER_KILL -> {
                if (elite) debug(settings, killer, dead, looting, roll, verdict.name());
            }
            default -> {
            }
        }
    }

    private void debug(SkullSettings settings, Player killer, LivingEntity dead,
                       int looting, double roll, String outcome) {
        if (!settings.debugLog()) return;
        double vanilla = SkullMath.vanillaChancePercent(looting);
        double topUp = SkullMath.topUpChancePercent(settings.targetChancePercent(), looting);
        plugin.getLogger().info(String.format(
                "ExcelWitherSkulls: elite wither skeleton (level %d) killed by %s; "
                        + "skull target %.1f%%, vanilla chance %.1f%% (Looting %d), "
                        + "top-up chance %.2f%%, roll %.2f -> %s",
                gateway.eliteLevelOf(dead),
                killer == null ? "?" : killer.getName(),
                settings.targetChancePercent(), vanilla, looting, topUp, roll, outcome));
    }
}
