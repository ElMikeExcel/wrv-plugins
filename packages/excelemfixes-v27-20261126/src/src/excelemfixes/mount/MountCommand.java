package excelemfixes.mount;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /mountcharm — the summon-mount module's command, registered
 * dynamically by the module (it does not exist while the module
 * is disabled or standing down). The shop menus and the vendor
 * NPC call the console form ({@code mountcharm buy <player>
 * [npc]}); players can also buy directly. summon / dismiss /
 * status mirror the charm for Java convenience — the item is the
 * product and is never required to be bypassed.
 */
public final class MountCommand implements CommandExecutor, TabCompleter {

    private final SummonMountModule module;

    MountCommand(SummonMountModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "buy" -> buy(sender, args);
            case "confirm" -> confirm(sender);
            case "give" -> give(sender, args);
            case "summon", "dismiss" -> toggle(sender);
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            default -> usage(sender);
        }
        return true;
    }

    // ------------------------------------------------------------ buy

    private void buy(CommandSender sender, String[] args) {
        Player buyer;
        BuyFlow.Venue venue = BuyFlow.Venue.SHOP;
        if (sender instanceof Player player) {
            buyer = player;
            if (args.length >= 2 && args[1].equalsIgnoreCase("npc")) {
                venue = BuyFlow.Venue.NPC;
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage("Console: mountcharm buy <player> [npc]");
                return;
            }
            buyer = Bukkit.getPlayerExact(args[1]);
            if (buyer == null) {
                sender.sendMessage("Player is not online.");
                return;
            }
            if (args.length >= 3 && args[2].equalsIgnoreCase("npc")) {
                venue = BuyFlow.Venue.NPC;
            }
        }
        if (module.buyFlow() == null) {
            MountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        reportArm(buyer, module.buyFlow().arm(buyer, venue), venue);
    }

    private void reportArm(Player buyer, BuyFlow.Result result,
                           BuyFlow.Venue venue) {
        switch (result) {
            case ARMED -> {
                BuyFlow.Armed review =
                        module.buyFlow().armedFor(buyer.getUniqueId());
                double price = review != null ? review.price()
                        : module.buyFlow().priceFor(venue);
                MountService.tell(buyer, ChatColor.YELLOW,
                        "Review purchase: Mount Charm — $"
                                + money(price) + (venue == BuyFlow.Venue.NPC
                                ? " (vendor price)" : "")
                                + ". Your balance: $"
                                + money(module.economy().getBalance(buyer))
                                + ". No charge yet — type "
                                + ChatColor.WHITE + "/mountcharm confirm"
                                + ChatColor.YELLOW + " within 30s to buy.");
            }
            case ALREADY_OWNED -> MountService.tell(buyer, ChatColor.YELLOW,
                    "You already own a Mount Charm. It's permanent —"
                            + " no need to buy another.");
            case NO_ECONOMY -> MountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            case DISABLED -> MountService.tell(buyer, ChatColor.YELLOW,
                    module.settings().retirementMessage());
            default -> MountService.tell(buyer, ChatColor.RED,
                    "Purchase unavailable. Try later, or ask an admin.");
        }
    }

    private void confirm(CommandSender sender) {
        if (!(sender instanceof Player buyer)) {
            sender.sendMessage("Only a player can confirm their purchase.");
            return;
        }
        if (module.buyFlow() == null) {
            MountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        BuyFlow.Armed review =
                module.buyFlow().armedFor(buyer.getUniqueId());
        double price = review != null ? review.price() : 0;
        switch (module.buyFlow().confirm(buyer)) {
            case CONFIRMED_PURCHASED -> MountService.tell(buyer,
                    ChatColor.GREEN, "Mount Charm bought for $"
                            + money(price)
                            + ". Right-click it to call your horse.");
            case NOT_ARMED -> MountService.tell(buyer, ChatColor.YELLOW,
                    "Nothing to confirm. Buy first: /mountcharm buy.");
            case ALREADY_OWNED -> MountService.tell(buyer, ChatColor.YELLOW,
                    "You already own a Mount Charm.");
            case INELIGIBLE_NO_ROOM -> MountService.tell(buyer,
                    ChatColor.RED, "Make an empty inventory slot and"
                            + " try again. No charge was made.");
            case INSUFFICIENT_FUNDS -> MountService.tell(buyer,
                    ChatColor.RED, "Not enough money. Price $"
                            + money(price) + ". No charge was made.");
            case DECLINED -> MountService.tell(buyer, ChatColor.RED,
                    "Payment declined. No charm delivered.");
            case REFUNDED -> MountService.tell(buyer, ChatColor.YELLOW,
                    "Delivery failed; your payment was refunded in"
                            + " full. Try again with a free slot.");
            case REFUND_FAILED -> MountService.tell(buyer, ChatColor.RED,
                    "Something went wrong after payment. Do not pay"
                            + " again — an admin has been notified in"
                            + " the server log.");
            case NO_ECONOMY -> MountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            case DISABLED -> MountService.tell(buyer, ChatColor.YELLOW,
                    module.settings().retirementMessage());
            default -> MountService.tell(buyer, ChatColor.RED,
                    "Purchase unavailable. Try later, or ask an admin.");
        }
    }

    // ------------------------------------------------------------ admin

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("excelemfixes.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("Usage: /mountcharm give <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player is not online.");
            return;
        }
        target.getInventory().addItem(module.createCharm());
        MountService.tell(target, ChatColor.GREEN,
                "You received a Mount Charm. Right-click it to call"
                        + " your horse.");
        sender.sendMessage("Gave a Mount Charm to " + target.getName() + ".");
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("excelemfixes.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        module.reloadSettings();
        sender.sendMessage("summon-mount reloaded. Retired: "
                + module.settings().retired() + ", price $"
                + money(module.settings().price()) + ", NPC price $"
                + money(module.settings().npcPrice()) + ".");
    }

    // ------------------------------------------------------------ mirror

    private void toggle(CommandSender sender) {
        if (sender instanceof Player player) {
            if (module.service() != null) module.service().toggle(player);
        } else {
            sender.sendMessage("Only a player can call a horse.");
        }
    }

    private void status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player has a horse.");
            return;
        }
        if (module.service() == null) {
            MountService.tell(player, ChatColor.GRAY,
                    "The charm is resting right now.");
            return;
        }
        if (module.service().hasActiveHorse(player.getUniqueId())) {
            MountService.tell(player, ChatColor.GRAY,
                    "Your horse is out.");
        } else {
            long cooldown = module.service().cooldownRemainingSeconds(
                    player.getUniqueId(), System.currentTimeMillis());
            MountService.tell(player, ChatColor.GRAY, cooldown > 0
                    ? "Your horse was lost. The charm is resting for "
                            + cooldown + "s."
                    : "Your horse is in the charm, ready when you are.");
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Mount Charm: "
                + ChatColor.GRAY
                + "/mountcharm buy | confirm | summon | dismiss | status"
                + (sender.hasPermission("excelemfixes.admin")
                        ? " | give <player> | reload" : ""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("buy", "confirm", "summon", "dismiss", "status");
        }
        return List.of();
    }

    static String money(double amount) {
        return amount == Math.rint(amount)
                ? String.format("%,d", (long) amount)
                : String.format("%,.2f", amount);
    }
}
