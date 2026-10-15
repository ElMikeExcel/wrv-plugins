package com.winkreyes.acrobatics.paper;

import com.winkreyes.acrobatics.ExcelAcrobatics;
import com.winkreyes.acrobatics.core.PayoutMath;
import com.winkreyes.acrobatics.core.SpotMemory;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Watches falls and pays Acrobatics XP when one ends in water.
 *
 * The tracker keeps each falling player's peak fall distance (the game's
 * own counter, sampled on move events). The first move event that finds
 * the player in water with a qualifying peak evaluates the award exactly
 * once per water entry. */
public final class FallWatcher implements Listener {

    private static final class State {
        float peakFall;
        boolean latched;
    }

    private final ExcelAcrobatics plugin;
    private final McMMOAward awardApi;
    private final SpotMemory memory = new SpotMemory();
    private final Map<UUID, State> states = new HashMap<>();

    public FallWatcher(ExcelAcrobatics plugin, McMMOAward awardApi) {
        this.plugin = plugin;
        this.awardApi = awardApi;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        State state = states.computeIfAbsent(player.getUniqueId(), k -> new State());
        if (player.isInWater()) {
            if (!state.latched && state.peakFall >= plugin.settings().minFallBlocks()) {
                state.latched = true;
                evaluate(player, state.peakFall);
            }
            state.peakFall = 0;
            return;
        }
        if (player.isOnGround()) {
            state.peakFall = 0;
            state.latched = false;
            return;
        }
        float fall = player.getFallDistance();
        if (fall > state.peakFall) {
            state.peakFall = fall;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private void evaluate(Player player, float fallDistance) {
        if (!awardApi.available()) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return; // A slow fall pays nothing on land either.
        }
        if (inBuildModeSession(player.getUniqueId())) {
            return;
        }
        PayoutMath.Settings settings = plugin.settings().payout();
        int ffLevel = featherFallLevel(player);
        long now = System.currentTimeMillis();
        int prior = memory.countRecent(player.getUniqueId(), player.getWorld().getName(),
                player.getLocation().getBlockX(), player.getLocation().getBlockZ(),
                plugin.settings().repeatRadiusBlocks(),
                plugin.settings().repeatWindowMinutes() * 60_000L, now);
        int xp = PayoutMath.awardXp(fallDistance, ffLevel, prior, settings);
        if (xp <= 0) {
            return;
        }
        if (awardApi.award(player, xp)) {
            memory.record(player.getUniqueId(), player.getWorld().getName(),
                    player.getLocation().getBlockX(), player.getLocation().getBlockZ(), now);
            if (plugin.settings().notifyPlayer()) {
                double style = PayoutMath.styleMultiplier(fallDistance, settings.styleStartBlocks(),
                        settings.stylePerBlock(), settings.styleCap());
                String message = "§bAcrobatics +" + xp + " XP §7( water landing, "
                        + Math.round(fallDistance) + " blocks";
                if (style > 1.0) {
                    message += ", style +" + Math.round((style - 1.0) * 100) + "%";
                }
                if (prior > 0) {
                    message += ", repeat spot pays less";
                }
                message += " )";
                player.sendMessage(message);
            }
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
