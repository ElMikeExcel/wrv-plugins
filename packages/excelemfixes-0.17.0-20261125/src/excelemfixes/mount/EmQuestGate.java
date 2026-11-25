package excelemfixes.mount;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.playercooldowns.PlayerQuestCooldowns;
import com.magmaguy.elitemobs.quests.playercooldowns.QuestCooldown;
import excelemfixes.mount.core.QuestCensus;
import excelemfixes.mount.core.QuestGateMath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The EliteMobs-backed {@link GhastGate}: the only gate class
 * that touches EliteMobs types, constructed by the module only
 * after EliteMobs is confirmed installed (the EmData pattern).
 *
 * <p>Denominators come from EliteMobs' own content: a scan of
 * its {@code customquests} folder (recursive, {@code .yml}
 * only, backups skipped). A quest file counts as enabled
 * unless it carries an explicit {@code isEnabled: false} —
 * EliteMobs' own default for the field is true. Totals are
 * counted at enable and on reload; quest content changes ride
 * server restarts in EliteMobs, so a purchase-time rescan
 * would read the same files.
 *
 * <p>Numerators come from the player's quest-cooldown
 * registry — the same read the progress-tracker module proves
 * in production: EliteMobs writes a cooldown carrying the
 * permission {@code elitequest.<filename>} every time a
 * player completes a quest, so the registry's permission set
 * is the set of quests that player has finished at least
 * once. Every read is guarded: any EliteMobs hiccup degrades
 * to "unavailable", and unavailable means the sale refuses —
 * the gate fails closed, never open.
 */
public final class EmQuestGate implements GhastGate {

    private final JavaPlugin plugin;
    private final Logger logger;
    private volatile List<QuestCensus.PackageRule> rules;
    private volatile List<String> enabledFiles = List.of();
    private volatile Map<String, Integer> totals = Map.of();
    private volatile boolean censusOk;
    // The gate fraction is part of the reading; the module sets
    // it alongside the rules whenever settings reload.
    private volatile double fraction = 0.5;

    public EmQuestGate(JavaPlugin plugin, Logger logger,
                       List<QuestCensus.PackageRule> rules) {
        this.plugin = plugin;
        this.logger = logger;
        updateRules(rules);
    }

    /** Swap in reloaded rules and recount the content. */
    public void updateRules(List<QuestCensus.PackageRule> newRules) {
        this.rules = newRules == null ? List.of() : newRules;
        rescan();
    }

    private void rescan() {
        try {
            Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null) {
                censusOk = false;
                enabledFiles = List.of();
                totals = Map.of();
                return;
            }
            File root = new File(em.getDataFolder(), "customquests");
            List<String> enabled = new ArrayList<>();
            collect(root, enabled);
            enabledFiles = List.copyOf(enabled);
            totals = QuestCensus.totals(enabledFiles, rules);
            censusOk = true;
            StringBuilder line = new StringBuilder(
                    "summon-mount: ghast quest-gate census —");
            if (totals.isEmpty()) {
                line.append(" no packages configured.");
            }
            for (Map.Entry<String, Integer> e : totals.entrySet()) {
                line.append(" ").append(e.getKey()).append("=")
                        .append(e.getValue()).append(" quests;");
            }
            logger.info(line.toString());
        } catch (Throwable t) {
            censusOk = false;
            logger.warning("summon-mount: ghast quest-gate census"
                    + " failed (" + t + ") — Ghast Charm purchases"
                    + " will refuse until the census reads clean.");
        }
    }

    private void collect(File dir, List<String> out) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".yml")
                    && !f.getName().contains(".bak")) {
                if (isEnabled(f)) out.add(f.getName());
            }
        }
    }

    /** EliteMobs' isEnabled defaults to true; only an explicit
     *  false disables a quest file. Line-based read — quest
     *  files are small, and the census runs at enable/reload,
     *  never per purchase. */
    private boolean isEnabled(File f) throws Exception {
        for (String line : Files.readAllLines(f.toPath())) {
            String trimmed = line.trim();
            if (trimmed.startsWith("isEnabled:")) {
                return !trimmed.substring("isEnabled:".length())
                        .trim().equalsIgnoreCase("false");
            }
        }
        return true;
    }

    @Override
    public Progress check(UUID playerId) {
        if (!censusOk) return Progress.unavailable();
        try {
            Set<String> completedFiles = new HashSet<>();
            PlayerQuestCooldowns cooldowns =
                    PlayerData.getPlayerQuestCooldowns(playerId);
            if (cooldowns != null) {
                for (QuestCooldown cd : cooldowns.getQuestCooldowns()) {
                    String perm = cd.getPermission();
                    if (perm != null && perm.startsWith("elitequest.")) {
                        completedFiles.add(
                                perm.substring("elitequest.".length()));
                    }
                }
            }
            Map<String, Integer> completed = QuestCensus.completed(
                    enabledFiles, completedFiles, rules);
            QuestGateMath.Progress best = QuestGateMath.best(
                    totals, completed, fraction);
            if (best == null) return Progress.unavailable();
            return new Progress(true, best.packageId(), best.completed(),
                    best.total(), best.required());
        } catch (Throwable t) {
            logger.warning("summon-mount: ghast quest-gate read failed"
                    + " for " + playerId + " (" + t + ") — refusing"
                    + " the sale (fail closed).");
            return Progress.unavailable();
        }
    }

    public void updateFraction(double newFraction) {
        this.fraction = newFraction <= 0.0 || newFraction > 1.0
                ? 0.5 : newFraction;
    }
}
