package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Money;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /games (aliases /brain, /puzzles) opens the picker for players.
 * /games admin ... is routed BEFORE any player-only check and works from the
 * console (fleet console-admin rule, SPEC §10). Phase 1 ships the framework
 * subset: status / reset / resetday / setkid / reload; riddle / setriddle
 * answer with the Phase 3 notice rather than a refusal.
 */
public final class BrainGamesCommand implements CommandExecutor {

    private final ExcelBrainGames plugin;

    public BrainGamesCommand(ExcelBrainGames plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return admin(sender, args);
        }
        if (sender instanceof Player p) {
            if (!p.hasPermission("braingames.play")) {
                p.sendMessage(GameService.text("&cYou don't have permission to play Brain Games."));
                return true;
            }
            plugin.service().data(p); // ensure a data file + name index entry exists
            p.openInventory(plugin.views().picker(p, plugin.service().data(p)));
            return true;
        }
        // Console (or other non-player) without "admin": print usage, never a bare refusal.
        sender.sendMessage("Brain Games — console usage:");
        sender.sendMessage("  /games admin status <player>");
        sender.sendMessage("  /games admin reset <player> [wordsearch|memory]");
        sender.sendMessage("  /games admin resetday <player>");
        sender.sendMessage("  /games admin setkid <player> <true|false>");
        sender.sendMessage("  /games admin reload");
        sender.sendMessage("Players open the game menu with /games in game.");
        return true;
    }

    // ------------------------------------------------------------- admin

    private boolean admin(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("braingames.admin")) {
            sender.sendMessage(GameService.text("&cYou need the braingames.admin permission for that."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /games admin <status|reset|resetday|setkid|reload|riddle|setriddle> ...");
            return true;
        }
        GameService svc = plugin.service();
        switch (args[1].toLowerCase()) {
            case "status" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /games admin status <player>"); return true; }
                PlayerData pd = resolve(sender, args[2]);
                if (pd == null) return true;
                GameConfig cfg = plugin.gameConfig();
                String today = svc.today();
                sender.sendMessage("Brain Games — " + displayName(pd));
                sender.sendMessage("  Kid flag: " + cfg.isKid(pd.uuid));
                sender.sendMessage("  Today: " + Money.fmt(pd.ledger.earnedOn(today))
                        + " of " + Money.fmt(cfg.dailyCapCents) + " cap");
                sender.sendMessage("  Streak: " + pd.ledger.streak() + " day(s), last paid day: "
                        + (pd.ledger.lastPaidDate() == null ? "never" : pd.ledger.lastPaidDate()));
                sender.sendMessage("  Word Search round: " + (pd.wordSearch == null ? "none"
                        : pd.wordSearch.themeId() + "/" + pd.wordSearch.tierId() + ", "
                        + pd.wordSearch.found().size() + "/" + pd.wordSearch.placements().size() + " words found"));
                sender.sendMessage("  Memory Match round: " + (pd.memory == null ? "none"
                        : pd.memory.tierId() + ", " + pd.memory.matchedPairs() + "/" + pd.memory.pairs() + " pairs"));
            }
            case "reset" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /games admin reset <player> [wordsearch|memory]"); return true; }
                PlayerData pd = resolve(sender, args[2]);
                if (pd == null) return true;
                String game = args.length >= 4 ? args[3].toLowerCase() : "all";
                switch (game) {
                    case "wordsearch" -> { pd.wordSearch = null; sender.sendMessage("Discarded " + displayName(pd) + "'s Word Search round (unpaid)."); }
                    case "memory" -> { pd.memory = null; sender.sendMessage("Discarded " + displayName(pd) + "'s Memory Match round (unpaid)."); }
                    case "all" -> { pd.wordSearch = null; pd.memory = null; sender.sendMessage("Discarded all of " + displayName(pd) + "'s Brain Games rounds (unpaid)."); }
                    default -> { sender.sendMessage("Unknown game '" + game + "' — use wordsearch, memory, or leave blank for all."); return true; }
                }
                svc.store().save(pd);
            }
            case "resetday" -> {
                if (args.length < 3) { sender.sendMessage("Usage: /games admin resetday <player>"); return true; }
                PlayerData pd = resolve(sender, args[2]);
                if (pd == null) return true;
                pd.ledger.resetDay(svc.today());
                svc.store().save(pd);
                sender.sendMessage("Zeroed " + displayName(pd) + "'s Brain Games earnings for today — cap room restored.");
            }
            case "setkid" -> {
                if (args.length < 4) { sender.sendMessage("Usage: /games admin setkid <player> <true|false>"); return true; }
                boolean flag;
                if (args[3].equalsIgnoreCase("true")) flag = true;
                else if (args[3].equalsIgnoreCase("false")) flag = false;
                else { sender.sendMessage("Usage: /games admin setkid <player> <true|false>"); return true; }
                PlayerData pd = resolve(sender, args[2]);
                if (pd == null) return true;
                setKidFlag(pd, flag);
                sender.sendMessage("Kid flag for " + displayName(pd) + " is now " + flag + ".");
            }
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage("Brain Games reloaded: config + " + plugin.wordLists().fileCount()
                        + " wordlist file(s), " + plugin.gameConfig().wsTiers.size() + " Word Search tiers, "
                        + plugin.gameConfig().memTiers.size() + " Memory tiers. Kid tiers enabled: "
                        + plugin.gameConfig().kidTiersEnabled);
            }
            case "riddle", "setriddle" ->
                    sender.sendMessage("Riddle of the Day arrives in Phase 3 — there is no daily riddle in this build yet.");
            default -> sender.sendMessage("Unknown admin command. Use status, reset, resetday, setkid, reload.");
        }
        return true;
    }

    private void setKidFlag(PlayerData pd, boolean flag) {
        FileConfiguration cfg = plugin.getConfig();
        List<Object> entries = new ArrayList<>();
        List<java.util.Map<?, ?>> raw = cfg.getMapList("kids");
        boolean found = false;
        for (java.util.Map<?, ?> e : raw) {
            Object u = e.get("uuid");
            if (u != null && u.toString().equalsIgnoreCase(pd.uuid.toString())) {
                found = true;
                if (flag) entries.add(e); // keep as is
            } else {
                entries.add(e);
            }
        }
        if (flag && !found) {
            java.util.Map<String, Object> add = new java.util.LinkedHashMap<>();
            add.put("uuid", pd.uuid.toString());
            add.put("name", pd.name);
            entries.add(add);
        }
        cfg.set("kids", entries);
        plugin.saveConfig();
        plugin.reloadAll();
    }

    /** Resolve online, then the data-store name index. Sends its own error line when unknown. */
    private PlayerData resolve(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return plugin.service().data(online);
        }
        UUID uuid = plugin.dataStore().uuidForName(name);
        if (uuid == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
            if (op != null) uuid = op.getUniqueId();
        }
        if (uuid == null) {
            sender.sendMessage("No Brain Games player named '" + name + "' (never played, or name unknown).");
            return null;
        }
        PlayerData pd = plugin.dataStore().peek(uuid, plugin.gameConfig().zone);
        if (pd == null) {
            sender.sendMessage("No Brain Games data for '" + name + "'.");
            return null;
        }
        return pd;
    }

    private String displayName(PlayerData pd) {
        return pd.name == null || pd.name.isBlank() ? pd.uuid.toString() : pd.name;
    }
}
