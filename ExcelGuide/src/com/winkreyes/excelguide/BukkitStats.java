package com.winkreyes.excelguide;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.winkreyes.excelguide.core.StatsView;
import com.winkreyes.guildgate.api.GuildGateAPI;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Live-data adapter for the Guide (SPEC 5.1). Every method reads the
 * owning plugin's own data at ask time; any failure returns null and
 * the engine answers "I can't see that right now" instead of guessing.
 * The asker is online by definition (they just chatted), so summaries
 * read through the online Player where an API requires one.
 *
 * Magic is read by reflection: ExcelGuide must load and run with Magic
 * merely dormant (Stage 0) and its standing answer is only ever called
 * once a seal has broken, so a soft reflective bridge is the right
 * weight here. The fields used are MagicAPI.getMage(player), the
 * mage's active class progression path name, mage level, and spells
 * known - Magic's own numbers, the same ones its menus show.
 */
public final class BukkitStats implements StatsView {

    /** The shared GriefPrevention read (0.2.2); null data = the engine's generic fallback. */
    private final GpClaims gpClaims = new GpClaims();

    @Override
    public long[] claimBlocks(UUID playerId) {
        return gpClaims.claimBlocks(playerId);
    }

    @Override
    public String mcMMOSummary(UUID playerId) {
        try {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || Bukkit.getPluginManager().getPlugin("mcMMO") == null) {
                return null;
            }
            int power = ExperienceAPI.getPowerLevel(player);
            List<int[]> levels = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (PrimarySkillType type : PrimarySkillType.values()) {
                int lvl = ExperienceAPI.getLevel(player, type);
                if (lvl > 0) {
                    levels.add(new int[]{lvl});
                    names.add(display(type.name()));
                }
            }
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) {
                order.add(i);
            }
            order.sort(Comparator.comparingInt(i -> -levels.get(i)[0]));
            StringBuilder top = new StringBuilder();
            for (int i = 0; i < Math.min(2, order.size()); i++) {
                if (i > 0) {
                    top.append(", ");
                }
                int idx = order.get(i);
                top.append(names.get(idx)).append(' ').append(levels.get(idx)[0]);
            }
            return "mcMMO overall " + power + (top.length() == 0 ? "" : " (" + top + ")");
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String mcMMOSkill(UUID playerId, String skillToken) {
        try {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || Bukkit.getPluginManager().getPlugin("mcMMO") == null) {
                return null;
            }
            for (PrimarySkillType type : PrimarySkillType.values()) {
                if (type.name().equalsIgnoreCase(skillToken)) {
                    int lvl = ExperienceAPI.getLevel(player, type);
                    return display(type.name()) + " (mcMMO) level " + lvl;
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String jobsSummary(UUID playerId) {
        try {
            if (Bukkit.getPluginManager().getPlugin("Jobs") == null) {
                return null;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
            JobsPlayer jp = Jobs.getPlayerManager().getJobsPlayer(op.getUniqueId());
            if (jp == null) {
                return null;
            }
            List<String> parts = new ArrayList<>();
            for (JobProgression prog : jp.getJobProgression()) {
                parts.add(prog.getJob().getName() + " " + prog.getLevel());
            }
            if (parts.isEmpty()) {
                return "Jobs: none yet - the Jobs board is in the Server Menu";
            }
            return "Jobs: " + String.join(", ", parts);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String playtimeSummary(UUID playerId) {
        try {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return null;
            }
            long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long minutes = ticks / 20 / 60;
            long hours = minutes / 60;
            long rem = minutes % 60;
            if (hours > 0) {
                return "Played " + hours + " h " + rem + " m";
            }
            return "Played " + minutes + " m";
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String moneySummary(UUID playerId) {
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return null;
            }
            Economy econ = rsp.getProvider();
            double bal = econ.getBalance(Bukkit.getOfflinePlayer(playerId));
            return "Balance: " + econ.format(bal);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public int sealsBroken() {
        try {
            return GuildGateAPI.sealsBrokenCount();
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public int sealsTotal() {
        try {
            int total = GuildGateAPI.sealsTotalCount();
            return total > 0 ? total : 5; // the campaign is five seals
        } catch (Throwable t) {
            return 5;
        }
    }

    @Override
    public String magicStanding(UUID playerId) {
        try {
            org.bukkit.plugin.Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            Player player = Bukkit.getPlayer(playerId);
            if (magic == null || player == null) {
                return null;
            }
            ClassLoader cl = magic.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.elmakers.mine.bukkit.api.magic.MagicAPI", true, cl);
            Object api = apiClass.cast(magic);
            Method getMage = apiClass.getMethod("getMage", org.bukkit.command.CommandSender.class);
            Object mage = getMage.invoke(api, player);
            if (mage == null) {
                return null;
            }
            Class<?> mageClass = mage.getClass();
            Object activeClass = mageClass.getMethod("getActiveClass").invoke(mage);
            if (activeClass == null) {
                return "Magic is flowing again, but you have not taken up a wand yet. "
                        + "The Old Quartermaster has the first one.";
            }
            Object path = activeClass.getClass().getMethod("getPath").invoke(activeClass);
            String pathName = path == null ? null
                    : String.valueOf(path.getClass().getMethod("getName").invoke(path));
            int level = (Integer) mageClass.getMethod("getLevel").invoke(mage);
            Object spells = mageClass.getMethod("getSpells").invoke(mage);
            int spellCount = spells instanceof java.util.Collection<?> c ? c.size() : 0;
            StringBuilder out = new StringBuilder("Magic: ");
            if (pathName != null && !pathName.isBlank()) {
                out.append(pathName).append(" path - ");
            }
            out.append("level ").append(level).append(", ").append(spellCount)
                    .append(spellCount == 1 ? " spell" : " spells").append(" known");
            return out.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String display(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
