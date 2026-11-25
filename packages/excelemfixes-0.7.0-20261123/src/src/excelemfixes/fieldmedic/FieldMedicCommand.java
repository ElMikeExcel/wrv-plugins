package excelemfixes.fieldmedic;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /fieldmedic — the field-medic module's purchase command,
 * registered dynamically by the module (it does not exist while
 * the module is disabled or standing down). The ExcelShops
 * special-products listing and the guild vendor NPC call the
 * console form ({@code fieldmedic buy <player> [npc]}); the buy
 * ARMS a review at the venue price and the player confirms with
 * {@code /fieldmedic confirm} — ExcelShops never charges for
 * special products itself, it hands the buyer to this flow (the
 * mobveil / deathprotection / mountcharm pattern). Players can
 * also buy directly. {@code give} is the admin form.
 */
public final class FieldMedicCommand implements CommandExecutor, TabCompleter {

    private final FieldMedicModule module;

    FieldMedicCommand(FieldMedicModule module) {
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
            default -> usage(sender);
        }
        return true;
    }

    // ------------------------------------------------------------ buy

    private void buy(CommandSender sender, String[] args) {
        Player buyer;
        VendorGoods.Venue venue = VendorGoods.Venue.SHOP;
        if (sender instanceof Player player) {
            buyer = player;
            if (args.length >= 2 && args[1].equalsIgnoreCase("npc")) {
                venue = VendorGoods.Venue.NPC;
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage("Console: fieldmedic buy <player> [npc]");
                return;
            }
            buyer = Bukkit.getPlayerExact(args[1]);
            if (buyer == null) {
                sender.sendMessage("Player is not online.");
                return;
            }
            if (args.length >= 3 && args[2].equalsIgnoreCase("npc")) {
                venue = VendorGoods.Venue.NPC;
            }
        }
        VendorBuyFlow flow = module.buyFlow();
        if (flow == null) {
            tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        VendorBuyFlow.Result result = flow.arm(buyer, VendorGoods.POTION, venue);
        switch (result) {
            case ARMED -> {
                VendorArms.Arm review = flow.armedFor(buyer.getUniqueId());
                double price = review != null ? review.price()
                        : flow.priceFor(VendorGoods.POTION, venue);
                tell(buyer, ChatColor.YELLOW,
                        "Review purchase: Splash Potion of Strong Healing — $"
                                + money(price) + (venue == VendorGoods.Venue.NPC
                                ? " (vendor price)" : "")
                                + ". Your balance: $"
                                + money(module.economy().getBalance(buyer))
                                + ". No charge yet — type "
                                + ChatColor.WHITE + "/fieldmedic confirm"
                                + ChatColor.YELLOW + " within 30s to buy.");
            }
            default -> tell(buyer, ChatColor.RED,
                    "Purchase unavailable. Try later, or ask an admin.");
        }
    }

    private void confirm(CommandSender sender) {
        if (!(sender instanceof Player buyer)) {
            sender.sendMessage("Only a player can confirm their purchase.");
            return;
        }
        VendorBuyFlow flow = module.buyFlow();
        if (flow == null) {
            tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        VendorArms.Arm review = flow.armedFor(buyer.getUniqueId());
        double price = review != null ? review.price() : 0;
        switch (flow.confirm(buyer)) {
            case CONFIRMED_PURCHASED -> tell(buyer, ChatColor.GREEN,
                    "Strong Healing Potion bought for $" + money(price)
                            + ". Splash it for 8 HP, even mid-fight.");
            case NOT_ARMED -> tell(buyer, ChatColor.YELLOW,
                    "Nothing to confirm. Buy first: /fieldmedic buy.");
            case INELIGIBLE_NO_ROOM -> tell(buyer, ChatColor.RED,
                    "Make an empty inventory slot and try again."
                            + " No charge was made.");
            case INSUFFICIENT_FUNDS -> tell(buyer, ChatColor.RED,
                    "Not enough money. Price $" + money(price)
                            + ". No charge was made.");
            case DECLINED -> tell(buyer, ChatColor.RED,
                    "Payment declined. No potion delivered.");
            case REFUNDED -> tell(buyer, ChatColor.YELLOW,
                    "Delivery failed; your payment was refunded in"
                            + " full. Try again with a free slot.");
            case REFUND_FAILED -> tell(buyer, ChatColor.RED,
                    "Something went wrong after payment. Do not pay"
                            + " again — an admin has been notified in"
                            + " the server log.");
            default -> tell(buyer, ChatColor.RED,
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
            sender.sendMessage("Usage: /fieldmedic give <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player is not online.");
            return;
        }
        target.getInventory().addItem(module.createPotion());
        tell(target, ChatColor.GREEN,
                "You received a Splash Potion of Strong Healing.");
        sender.sendMessage("Gave a Strong Healing Potion to "
                + target.getName() + ".");
    }

    // ------------------------------------------------------------ helpers

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Field Medic: "
                + ChatColor.GRAY
                + "/fieldmedic buy [npc] | confirm"
                + (sender.hasPermission("excelemfixes.admin")
                        ? " | give <player>" : ""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("buy", "confirm");
        }
        return List.of();
    }

    static void tell(Player player, ChatColor color, String message) {
        player.sendMessage(color + message);
    }

    static String money(double amount) {
        return amount == Math.rint(amount)
                ? String.format("%,d", (long) amount)
                : String.format("%,.2f", amount);
    }
}
