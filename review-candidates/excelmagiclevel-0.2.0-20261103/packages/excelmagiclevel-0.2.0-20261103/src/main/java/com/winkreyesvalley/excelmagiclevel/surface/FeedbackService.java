package com.winkreyesvalley.excelmagiclevel.surface;

import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Percents;
import com.winkreyesvalley.excelmagiclevel.store.PlayerRecord;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelMagicLevel — player feedback (SPEC §6.1). Gain ticks are throttled:
 * XP accumulates silently until the un-shown total crosses the configured
 * chunk, so a busy fight shows a handful of action-bar ticks, not one per
 * cast. Level-ups name the concrete gain (SPEC §3), with title + sound at
 * milestone levels and chat + sound otherwise. {@code quiet} silences all
 * of it — checked against the store on every call, so the toggle takes
 * effect immediately.
 */
public final class FeedbackService {
    private final PlayerStore store;
    private final int chunk;
    private final Map<UUID, Integer> pending = new HashMap<>();

    public FeedbackService(PlayerStore store, int chunk) {
        this.store = store;
        this.chunk = Math.max(1, chunk);
    }

    private boolean isQuiet(UUID playerId) {
        PlayerRecord record = store.find(playerId);
        return record != null && record.quiet();
    }

    public void onGain(Player player, int amount, boolean shared) {
        if (amount <= 0 || isQuiet(player.getUniqueId())) return;
        int total = pending.merge(player.getUniqueId(), amount, Integer::sum);
        if (total >= chunk) {
            pending.remove(player.getUniqueId());
            player.sendActionBar(Component.text("+" + total + " Magic XP" + (shared ? " (party)" : "")));
        }
    }

    public void onLevelUp(Player player, int newLevel, Boost boost, boolean milestone) {
        if (isQuiet(player.getUniqueId())) return;
        String gains = "mana pool +" + Percents.format(boost.manaMaxBoost())
                + "%, regen +" + Percents.format(boost.manaRegenerationBoost())
                + "%, cast cost -" + Percents.format(boost.costReduction()) + "%";
        player.sendMessage(Component.text("Magic Level " + newLevel + " — " + gains));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        if (milestone) {
            player.showTitle(Title.title(
                    Component.text("Magic Level " + newLevel),
                    Component.text(gains)));
        }
    }

    /** Drop a player's un-shown accumulation (quit hygiene). */
    public void forget(UUID playerId) {
        pending.remove(playerId);
    }
}
