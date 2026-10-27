package com.winkreyes.acrobatics.paper;

import com.winkreyes.acrobatics.ExcelAcrobatics;
import com.winkreyes.acrobatics.core.DeathMath;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Fatal falls: mcMMO itself pays nothing when a fall kills (its roll
 * check returns unprocessed for fatal falls). Mike's rule (Oct 14, 2026)
 * pays Acrobatics XP for a death fall ONLY when the death costs real
 * vanilla XP, so empty-bar suicide jumps can never farm it. The award is
 * granted during the death event so it is in the player's mcMMO profile
 * before respawn, and the player is told on respawn what it paid. */
public final class DeathWatcher implements Listener {

    private final ExcelAcrobatics plugin;
    private final McMMOAward awardApi;
    private final Map<UUID, String> pendingMessages = new HashMap<>();

    public DeathWatcher(ExcelAcrobatics plugin, McMMOAward awardApi) {
        this.plugin = plugin;
        this.awardApi = awardApi;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.settings().deathEnabled()) {
            return;
        }
        Player player = event.getEntity();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        if (!(player.getLastDamageCause() instanceof EntityDamageEvent damage)
                || damage.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (inBuildModeSession(player.getUniqueId())) {
            return; // Build Mode deaths are the pilot's restore path, not a payout.
        }
        double finalDamage = damage.getFinalDamage();
        if (finalDamage < plugin.settings().deathMinFallDamage()) {
            return;
        }
        int lost = DeathMath.xpLostOnDeath(player.getLevel(), player.getExp(), event.getDroppedExp());
        if (!DeathMath.gatePasses(lost, plugin.settings().deathMinXpLost())) {
            return;
        }
        if (!awardApi.available()) {
            return;
        }
        int xp = DeathMath.deathAwardXp(finalDamage, featherFallLevel(player),
                plugin.settings().payout().xpPerDamagePoint(),
                plugin.settings().payout().featherFallXpMultiplier(),
                plugin.settings().payout().maxXpPerLanding());
        if (xp > 0 && awardApi.award(player, xp) && plugin.settings().notifyPlayer()) {
            pendingMessages.put(player.getUniqueId(),
                    "§bAcrobatics +" + xp + " XP §7( that fall cost you " + lost
                            + " XP, so the attempt counts )");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        String message = pendingMessages.remove(event.getPlayer().getUniqueId());
        if (message != null) {
            event.getPlayer().sendMessage(message);
        }
    }

    private int featherFallLevel(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) {
            return 0;
        }
        try {
            return boots.getEnchantmentLevel(Enchantment.FEATHER_FALLING);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private boolean inBuildModeSession(UUID player) {
        Path index = plugin.getDataFolder().toPath().getParent()
                .resolve("ExcelBuildMode").resolve("active-index").resolve(player + ".idx");
        return Files.isRegularFile(index);
    }
}

