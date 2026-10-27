package com.winkreyes.exceltelemetry.integration;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * mcMMO integration. Compiled against the WRV mcMMO fork jar (the
 * same route ExcelGuide's build takes), and ONLY instantiated when
 * the mcMMO plugin is present and enabled - class loading stays
 * lazy, so a missing mcMMO never breaks Telemetry (design: every
 * soft-depend fails soft).
 *
 * Skill set and ordering deliberately match the flatfile column map
 * recovered from the fork's FlatFileDatabaseManager, so telemetry
 * snapshots, flatfile decodes and the weekly report all speak the
 * same 17 skill names.
 */
public final class McMmoBridge implements Listener {
    public record LevelUp(String uuid, String name, String skill, int level, int levelsGained) {}

    public static final List<String> SKILLS = List.of(
            "Mining", "Woodcutting", "Repair", "Unarmed", "Herbalism", "Excavation",
            "Archery", "Swords", "Axes", "Acrobatics", "Taming", "Fishing", "Alchemy",
            "Crossbows", "Tridents", "Maces", "Spears");

    public static final List<String> COMBAT_SKILLS = List.of(
            "Swords", "Axes", "Archery", "Crossbows", "Tridents", "Maces", "Spears",
            "Unarmed", "Acrobatics");

    private final Consumer<LevelUp> sink;

    public McMmoBridge(Consumer<LevelUp> sink) {
        this.sink = sink;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(McMMOPlayerLevelUpEvent event) {
        try {
            Player p = event.getPlayer();
            if (p == null) return;
            sink.accept(new LevelUp(p.getUniqueId().toString(), p.getName(),
                    title(event.getSkill().name()), event.getSkillLevel(), event.getLevelsGained()));
        } catch (Throwable ignored) {
            // a bad event must never break mcMMO's own dispatch
        }
    }

    /** Full 17-skill levels + power, or null when mcMMO can't answer for this player. */
    public Map<String, Integer> levels(Player player) {
        try {
            Map<String, Integer> out = new LinkedHashMap<>();
            for (String skill : SKILLS) {
                out.put(skill, ExperienceAPI.getLevel(player, PrimarySkillType.valueOf(skill.toUpperCase())));
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    public int powerLevel(Player player) {
        try {
            return ExperienceAPI.getPowerLevel(player);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Combat subset (for death lines), or null when unreadable. */
    public Map<String, Object> combatLevels(Player player) {
        Map<String, Integer> all = levels(player);
        if (all == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        for (String s : COMBAT_SKILLS) out.put(s, all.getOrDefault(s, 0));
        int power = powerLevel(player);
        if (power >= 0) out.put("power", power);
        return out;
    }

    static String title(String enumName) {
        if (enumName == null || enumName.isEmpty()) return enumName;
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }
}
