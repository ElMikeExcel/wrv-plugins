package com.winkreyes.excelprotect;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * ExcelProtect — mcMMO adapter (fail-soft; this class is only loaded when mcMMO
 * is enabled): level-ups into the players stream (progression) + per-player skill
 * SNAPSHOTS into the samples stream (hourly / join / quit / death). Excel — WRV
 */
public final class McmmoAdapter implements Listener {
    private final Ledger ledger;
    private final Settings cfg;

    public McmmoAdapter(Ledger ledger, Settings cfg) { this.ledger = ledger; this.cfg = cfg; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(McMMOPlayerLevelUpEvent e) {
        if (!cfg.progression()) return;
        Player p = e.getPlayer();
        ledger.emit("players", p.getLocation(), new Json.Obj().put("type", "mcmmo_level_up")
                .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("skill", e.getSkill().getName().toLowerCase())
                .put("level", e.getSkillLevel())
                .put("levels_gained", e.getLevelsGained()));
    }

    public void snapshot(Player p, String reason) {
        if (!cfg.progression()) return;
        try {
            Json.Obj skills = new Json.Obj();
            int power = 0;
            for (PrimarySkillType s : PrimarySkillType.values()) {
                if (s.isChildSkill()) continue;
                int lvl = com.gmail.nossr50.api.ExperienceAPI.getLevel(p, s);
                skills.put(s.getName().toLowerCase(), lvl);
                power += lvl;
            }
            ledger.emit("snapshots", p.getLocation(), new Json.Obj().put("type", "mcmmo_snapshot")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                    .put("reason", reason).put("power_level", power).put("skills", skills));
        } catch (Throwable ignored) { }
    }

    public void snapshotAll(String reason) {
        for (Player p : Bukkit.getOnlinePlayers()) snapshot(p, reason);
    }
}
