package com.winkreyes.excelitems;

import com.winkreyes.excelitems.core.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * /excelitems — the registry's small admin surface:
 * status (count + next free ID), list (every ID + key + kind),
 * give &lt;player&gt; &lt;id|key&gt; [amount] (mint through the API),
 * identify (name the held item's registry ID, primary or legacy).
 * Permission excelitems.admin (default op) on the whole command.
 */
public final class ItemsCommand implements CommandExecutor, TabCompleter {

    private final ExcelItems plugin;

    ItemsCommand(ExcelItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("excelitems.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "status" -> {
                sender.sendMessage(ChatColor.GOLD + "ExcelItems: " + ChatColor.WHITE
                        + plugin.registry().size() + " item(s) registered, next free id "
                        + plugin.registry().nextId() + ".");
                return true;
            }
            case "list" -> {
                for (ItemDefinition def : plugin.registry().all()) {
                    sender.sendMessage(ChatColor.GOLD + "#" + def.id() + " " + ChatColor.WHITE
                            + def.key() + ChatColor.GRAY + " (" + def.kind() + ", "
                            + def.material() + ")");
                }
                return true;
            }
            case "give" -> {
                return give(sender, args);
            }
            case "identify" -> {
                return identify(sender);
            }
            default -> {
                usage(sender);
                return true;
            }
        }
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /excelitems give <player> <id|key> [amount]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[1]);
            return true;
        }
        Optional<ItemDefinition> def = resolve(args[2]);
        if (def.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown registry item: " + args[2]);
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException bad) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }
        }
        ItemStack stack;
        try {
            stack = plugin.create(def.get().id(), amount);
        } catch (IllegalArgumentException bad) {
            sender.sendMessage(ChatColor.RED + bad.getMessage());
            return true;
        }
        target.getInventory().addItem(stack);
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " x " + def.get().key()
                + " (#" + def.get().id() + ") to " + target.getName() + ".");
        return true;
    }

    private boolean identify(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player can identify a held item.");
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        OptionalInt id = plugin.identify(held);
        if (id.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "That item is not a registered custom item.");
        } else {
            ItemDefinition def = plugin.definition(id.getAsInt()).orElseThrow();
            player.sendMessage(ChatColor.GOLD + "Registry #" + def.id() + ": " + ChatColor.WHITE
                    + def.key() + ChatColor.GRAY + " (" + def.kind() + ")");
        }
        return true;
    }

    private Optional<ItemDefinition> resolve(String token) {
        try {
            return plugin.definition(Integer.parseInt(token));
        } catch (NumberFormatException notNumeric) {
            return plugin.definition(token);
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "ExcelItems: " + ChatColor.WHITE
                + "/excelitems <status|list|give <player> <id|key> [amount]|identify>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("status", "list", "give", "identify"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) options.add(player.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (ItemDefinition def : plugin.registry().all()) options.add(def.key());
        }
        return options;
    }
}
