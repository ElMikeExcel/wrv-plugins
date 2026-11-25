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
 * /ghastcharm — the Ghast Charm's command, registered
 * dynamically by the module while the ghast tier is enabled
 * (it does not exist while the module is disabled, standing
 * down, or the ghast is switched off in config). Mirrors
 * /mountcharm's shape exactly: the shop menus and the vendor
 * NPC call the console form ({@code ghastcharm buy <player>
 * [npc]}); players can also buy directly. summon / dismiss /
 * status mirror the charm — the item is the product.
 *
 * <p>The buy path is the gated one (D204): arming a purchase
 * reads the quest gate, and the refusal quotes the player's
 * real progress in their best quest package.
 */
public final class GhastCommand implements CommandExecutor, TabCompleter {

    private final SummonMountModule module;

    GhastCommand(SummonMountModule module) {
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
                sender.sendMessage("Console: ghastcharm buy <player> [npc]");
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
            GhastMountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        reportArm(buyer, module.buyFlow().armGhast(buyer, venue), venue);
    }

    private void reportArm(Player buyer, BuyFlow.Result result,
                           BuyFlow.Venue venue) {
        switch (result) {
            case ARMED -> {
                BuyFlow.Armed review =
                        module.buyFlow().ghastArmedFor(buyer.getUniqueId());
                double price = review != null ? review.price()
                        : module.buyFlow().ghastPriceFor(venue);
                GhastMountService.tell(buyer, ChatColor.YELLOW,
                        "Review purchase: Ghast Charm — $"
                                + MountCommand.money(price)
                                + (venue == BuyFlow.Venue.NPC
                                        ? " (vendor price)" : "")
                                + ". Your balance: $"
                                + MountCommand.money(
                                        module.economy().getBalance(buyer))
                                + ". No charge yet — type "
                                + ChatColor.WHITE + "/ghastcharm confirm"
                                + ChatColor.YELLOW + " within 30s to buy.");
            }
            case ALREADY_OWNED -> GhastMountService.tell(buyer,
                    ChatColor.YELLOW,
                    "You already own a Ghast Charm. It's permanent —"
                            + " no need to buy another.");
            case GATE_LOCKED -> GhastMountService.tell(buyer,
                    ChatColor.YELLOW, gateLockedText(buyer));
            case GATE_UNAVAILABLE -> GhastMountService.tell(buyer,
                    ChatColor.RED,
                    "The quest gate can't be read right now, so the"
                            + " charm can't be sold. Ask an admin.");
            case NO_ECONOMY -> GhastMountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            case DISABLED -> GhastMountService.tell(buyer, ChatColor.YELLOW,
                    module.settings().retirementMessage());
            default -> GhastMountService.tell(buyer, ChatColor.RED,
                    "Purchase unavailable. Try later, or ask an admin.");
        }
    }

    /** The refusal, quoting the player's best package in real
     *  numbers — "how far am I?" is the only useful answer to
     *  a locked gate. */
    private String gateLockedText(Player buyer) {
        GhastGate.Progress progress =
                module.buyFlow().gateProgressFor(buyer.getUniqueId());
        if (progress == null || progress.packageId() == null
                || progress.total() <= 0) {
            return "The Ghast Charm is earned: finish at least half"
                    + " the quests in Primis or any one expansion"
                    + " to buy it.";
        }
        return "The Ghast Charm is earned: finish at least half the"
                + " quests in one adventure pack. Your best progress: "
                + progress.completed() + " of " + progress.total()
                + " quests in " + progress.packageId() + " — "
                + progress.required() + " needed.";
    }

    private void confirm(CommandSender sender) {
        if (!(sender instanceof Player buyer)) {
            sender.sendMessage("Only a player can confirm their purchase.");
            return;
        }
        if (module.buyFlow() == null) {
            GhastMountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        BuyFlow.Armed review =
                module.buyFlow().ghastArmedFor(buyer.getUniqueId());
        double price = review != null ? review.price() : 0;
        switch (module.buyFlow().confirmGhast(buyer)) {
            case CONFIRMED_PURCHASED -> GhastMountService.tell(buyer,
                    ChatColor.GREEN, "Ghast Charm bought for $"
                            + MountCommand.money(price)
                            + ". Right-click it to call your ghast.");
            case NOT_ARMED -> GhastMountService.tell(buyer, ChatColor.YELLOW,
                    "Nothing to confirm. Buy first: /ghastcharm buy.");
            case ALREADY_OWNED -> GhastMountService.tell(buyer,
                    ChatColor.YELLOW, "You already own a Ghast Charm.");
            case GATE_LOCKED -> GhastMountService.tell(buyer,
                    ChatColor.YELLOW, gateLockedText(buyer));
            case GATE_UNAVAILABLE -> GhastMountService.tell(buyer,
                    ChatColor.RED,
                    "The quest gate can't be read right now, so the"
                            + " charm can't be sold. Ask an admin.");
            case INELIGIBLE_NO_ROOM -> GhastMountService.tell(buyer,
                    ChatColor.RED, "Make an empty inventory slot and"
                            + " try again. No charge was made.");
            case INSUFFICIENT_FUNDS -> GhastMountService.tell(buyer,
                    ChatColor.RED, "Not enough money. Price $"
                            + MountCommand.money(price)
                            + ". No charge was made.");
            case DECLINED -> GhastMountService.tell(buyer, ChatColor.RED,
                    "Payment declined. No charm delivered.");
            case REFUNDED -> GhastMountService.tell(buyer, ChatColor.YELLOW,
                    "Delivery failed; your payment was refunded in"
                            + " full. Try again with a free slot.");
            case REFUND_FAILED -> GhastMountService.tell(buyer, ChatColor.RED,
                    "Something went wrong after payment. Do not pay"
                            + " again — an admin has been notified in"
                            + " the server log.");
            case NO_ECONOMY -> GhastMountService.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            case DISABLED -> GhastMountService.tell(buyer, ChatColor.YELLOW,
                    module.settings().retirementMessage());
            default -> GhastMountService.tell(buyer, ChatColor.RED,
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
            sender.sendMessage("Usage: /ghastcharm give <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player is not online.");
            return;
        }
        target.getInventory().addItem(module.createGhastCharm());
        GhastMountService.tell(target, ChatColor.GREEN,
                "You received a Ghast Charm. Right-click it to call"
                        + " your ghast.");
        sender.sendMessage("Gave a Ghast Charm to " + target.getName() + ".");
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("excelemfixes.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        module.reloadSettings();
        sender.sendMessage("summon-mount reloaded. Ghast price $"
                + MountCommand.money(module.ghastSettings().price())
                + ", NPC price $"
                + MountCommand.money(module.ghastSettings().npcPrice())
                + ", gate "
                + (module.ghastSettings().gateEnabled() ? "on" : "off")
                + ".");
    }

    // ------------------------------------------------------------ mirror

    private void toggle(CommandSender sender) {
        if (sender instanceof Player player) {
            if (module.ghastService() != null) {
                module.ghastService().toggle(player);
            }
        } else {
            sender.sendMessage("Only a player can call a ghast.");
        }
    }

    private void status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player has a ghast.");
            return;
        }
        if (module.ghastService() == null) {
            GhastMountService.tell(player, ChatColor.GRAY,
                    "The charm is resting right now.");
            return;
        }
        if (module.ghastService().hasActiveGhast(player.getUniqueId())) {
            GhastMountService.tell(player, ChatColor.GRAY,
                    "Your ghast is out.");
        } else {
            long cooldown = module.ghastService()
                    .cooldownRemainingSeconds(player.getUniqueId(),
                            System.currentTimeMillis());
            GhastMountService.tell(player, ChatColor.GRAY, cooldown > 0
                    ? "Your ghast was lost. The charm is resting for "
                            + cooldown + "s."
                    : "Your ghast is in the charm, ready when you are.");
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Ghast Charm: "
                + ChatColor.GRAY
                + "/ghastcharm buy | confirm | summon | dismiss | status"
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
}
