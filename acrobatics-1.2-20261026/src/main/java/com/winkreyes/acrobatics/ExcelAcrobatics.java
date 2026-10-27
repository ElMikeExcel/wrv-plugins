package com.winkreyes.acrobatics;

import com.winkreyes.acrobatics.core.PayoutMath;
import com.winkreyes.acrobatics.paper.FallWatcher;
import com.winkreyes.acrobatics.paper.McMMOAward;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ExcelAcrobatics extends JavaPlugin {

    public record Settings(
            PayoutMath.Settings payout,
            double minFallBlocks,
            int repeatRadiusBlocks,
            long repeatWindowMinutes,
            boolean notifyPlayer,
            boolean deathEnabled,
            int deathMinXpLost,
            double deathMinFallDamage) {}

    private Settings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = readSettings(getConfig());
        McMMOAward awardApi = new McMMOAward(getLogger());
        getServer().getScheduler().runTask(this, awardApi::available); // verify the mcMMO API at startup, not at first landing
        getServer().getPluginManager().registerEvents(new FallWatcher(this, awardApi), this);
        getServer().getPluginManager().registerEvents(
                new com.winkreyes.acrobatics.paper.DeathWatcher(this, awardApi), this);
        getLogger().info("ExcelAcrobatics enabled: water landings pay Acrobatics XP "
                + "(base rate " + settings.payout().xpPerDamagePoint() + " per damage point, "
                + "style up to +" + Math.round(settings.payout().styleCap() * 100) + "%).");
    }

    public Settings settings() {
        return settings;
    }

    private Settings readSettings(FileConfiguration config) {
        List<Double> repeats = config.getDoubleList("repeat-multipliers");
        if (repeats.isEmpty()) {
            repeats = List.of(1.0, 0.25);
        }
        PayoutMath.Settings payout = new PayoutMath.Settings(
                config.getDouble("xp-per-damage-point", 600),
                config.getDouble("style-start-blocks", 10),
                config.getDouble("style-per-block", 0.05),
                config.getDouble("style-cap", 1.0),
                List.copyOf(repeats),
                config.getInt("max-xp-per-landing", 25_000),
                config.getDouble("feather-fall-xp-multiplier", 2.0));
        return new Settings(
                payout,
                config.getDouble("min-fall-blocks", 4.0),
                config.getInt("repeat-radius-blocks", 5),
                config.getLong("repeat-window-minutes", 10),
                config.getBoolean("notify-player", true),
                config.getBoolean("death-award-enabled", true),
                config.getInt("death-min-xp-lost", 200),
                config.getDouble("death-min-fall-damage", 5.0));
    }
}

