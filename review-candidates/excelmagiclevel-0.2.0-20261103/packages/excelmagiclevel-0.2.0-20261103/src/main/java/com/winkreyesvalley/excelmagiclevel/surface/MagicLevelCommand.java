package com.winkreyesvalley.excelmagiclevel.surface;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.BoostApplier;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.MagicBridge;
import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.ExportRows;
import com.winkreyesvalley.excelmagiclevel.core.Percents;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;
import com.winkreyesvalley.excelmagiclevel.store.PlayerRecord;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelMagicLevel — /magiclevel (SPEC §6.1). Works identically from Java
 * chat and Bedrock-via-Geyser chat — no form dependency for the core
 * surface, so parity is by construction. Player forms: status (with the
 * party-share status line), quiet, top. Admin forms (permission
 * {@code excelmagiclevel.admin}, console welcome): export, strip,
 * strip-all.
 */
public final class MagicLevelCommand implements CommandExecutor, TabCompleter {
    /** SPEC §3's canonical explainer — verbatim, used on every surface. */
    public static final String EXPLAINER =
            "Your path and the seals decide what you can cast. Your Magic Level decides how strong "
            + "your casting is — a bigger mana pool, faster regen, cheaper casts. "
            + "It never locks or unlocks a spell.";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;
    private final PlayerStore store;
    private final Curve curve;
    private final RewardTable rewards;
    private final MagicBridge magic;
    private final BoostApplier boosts;
    private final String shareStatus;

    public MagicLevelCommand(JavaPlugin plugin, PlayerStore store, Curve curve, RewardTable rewards,
                             MagicBridge magic, BoostApplier boosts, String shareStatus) {
        this.plugin = plugin;
        this.store = store;
        this.curve = curve;
        this.rewards = rewards;
        this.magic = magic;
        this.boosts = boosts;
        this.shareStatus = shareStatus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            return admin(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            showTop(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Usage: /magiclevel admin <export|strip <player>|strip-all> (console); players: /magiclevel [quiet|top]");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("quiet")) {
            PlayerRecord record = store.record(player.getUniqueId());
            record.setQuiet(!record.quiet());
            store.markDirty();
            player.sendMessage("Magic Level feedback is now " + (record.quiet() ? "OFF (quiet)" : "ON") + ".");
            return true;
        }
        if (args.length >= 1) {
            player.sendMessage("Usage: /magiclevel [quiet|top]");
            return true;
        }
        showStatus(player);
        return true;
    }

    private void showStatus(Player player) {
        PlayerRecord record = store.find(player.getUniqueId());
        long totalXp = record == null ? 0 : record.totalXp();
        int level = curve.levelFor(totalXp);
        Boost boost = rewards.boostAt(level);
        player.sendMessage("Magic Level " + level + " — " + curve.xpIntoLevel(totalXp) + " XP into this level"
                + (level >= curve.maxLevel()
                        ? ", at the cap (" + totalXp + " XP total)"
                        : ", " + curve.xpToNextLevel(totalXp) + " XP to Magic Level " + (level + 1)));
        if (level >= 2) {
            player.sendMessage("Boosts now: mana pool +" + Percents.format(boost.manaMaxBoost())
                    + "%, mana regen +" + Percents.format(boost.manaRegenerationBoost())
                    + "%, cast cost -" + Percents.format(boost.costReduction()) + "%");
        } else {
            player.sendMessage("Boosts now: none yet — they start at Magic Level 2.");
        }
        player.sendMessage("Party share: " + shareStatus);
        player.sendMessage(EXPLAINER);
    }

    private void showTop(CommandSender sender) {
        List<Map.Entry<UUID, PlayerRecord>> rows = new ArrayList<>(store.snapshot().entrySet());
        rows.sort((a, b) -> {
            int byXp = Long.compare(b.getValue().totalXp(), a.getValue().totalXp());
            return byXp != 0 ? byXp : a.getKey().toString().compareTo(b.getKey().toString());
        });
        if (rows.isEmpty()) {
            sender.sendMessage("No Magic Levels yet — cast something!");
            return;
        }
        sender.sendMessage("Magic Level leaderboard:");
        int shown = Math.min(10, rows.size());
        for (int i = 0; i < shown; i++) {
            Map.Entry<UUID, PlayerRecord> row = rows.get(i);
            sender.sendMessage((i + 1) + ". " + displayName(row.getKey()) + " — Magic Level "
                    + curve.levelFor(row.getValue().totalXp()) + " (" + row.getValue().totalXp() + " XP)");
        }
        if (sender instanceof Player player) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getKey().equals(player.getUniqueId())) {
                    if (i >= shown) {
                        sender.sendMessage("You are #" + (i + 1) + " — Magic Level "
                                + curve.levelFor(rows.get(i).getValue().totalXp())
                                + " (" + rows.get(i).getValue().totalXp() + " XP)");
                    }
                    break;
                }
            }
        }
    }

    private static String displayName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("excelmagiclevel.admin")) {
            sender.sendMessage("You need the excelmagiclevel.admin permission for that.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /magiclevel admin <export|strip <player>|strip-all>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "export" -> export(sender);
            case "strip" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /magiclevel admin strip <player>");
                } else {
                    strip(sender, args[2]);
                }
            }
            case "strip-all" -> stripAll(sender);
            default -> sender.sendMessage("Usage: /magiclevel admin <export|strip <player>|strip-all>");
        }
        return true;
    }

    private void export(CommandSender sender) {
        Map<UUID, Long> totals = new HashMap<>();
        store.snapshot().forEach((uuid, record) -> totals.put(uuid, record.totalXp()));
        String yaml = ExportRows.toYaml(totals, curve, Instant.now());
        File dir = new File(plugin.getDataFolder(), "exports");
        File out = new File(dir, "magiclevel-export-" + LocalDateTime.now().format(STAMP) + ".yml");
        try {
            Files.createDirectories(dir.toPath());
            Files.writeString(out.toPath(), yaml, StandardCharsets.UTF_8);
            sender.sendMessage("Exported " + totals.size() + " mcMMO-shaped rows to " + out.getPath());
        } catch (Exception ex) {
            sender.sendMessage("Export failed: " + ex.getMessage());
            plugin.getLogger().warning("ExcelMagicLevel export failed: " + ex);
        }
    }

    private void strip(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(name + " is not online. Offline mages keep their frozen boost in Magic's "
                    + "save until they next load — strip them while online (SPEC 7.3).");
            return;
        }
        Mage mage = magic.mage(target);
        if (mage == null) {
            sender.sendMessage("No mage found for " + name + ".");
            return;
        }
        int removed = boosts.strip(mage);
        sender.sendMessage("Stripped " + removed + " ExcelMagicLevel modifier(s) from " + name
                + ". (While ExcelMagicLevel is enabled, reconcile will re-apply the earned boost at their next load — "
                + "strip is an uninstall-time tool.)");
    }

    private void stripAll(CommandSender sender) {
        int players = 0;
        int removed = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            Mage mage = magic.mage(online);
            if (mage == null) continue;
            int n = boosts.strip(mage);
            if (n > 0) {
                players++;
                removed += n;
            }
        }
        List<String> residue = new ArrayList<>();
        store.snapshot().forEach((uuid, record) -> {
            if (curve.levelFor(record.totalXp()) >= 2 && Bukkit.getPlayer(uuid) == null) {
                residue.add(uuid.toString());
            }
        });
        sender.sendMessage("Strip-all: removed " + removed + " modifier(s) from " + players + " online player(s).");
        if (!residue.isEmpty()) {
            sender.sendMessage("Residue: " + residue.size() + " OFFLINE player(s) at Magic Level >= 2 still hold a frozen "
                    + "boost in Magic's saves: " + String.join(", ", residue)
                    + " — strip them while online, or accept the residue (SPEC 7.3).");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("quiet", "top"));
            if (sender.hasPermission("excelmagiclevel.admin")) options.add("admin");
            return options.stream().filter(o -> o.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("export", "strip", "strip-all").stream()
                    .filter(o -> o.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("strip")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }
        return List.of();
    }
}
