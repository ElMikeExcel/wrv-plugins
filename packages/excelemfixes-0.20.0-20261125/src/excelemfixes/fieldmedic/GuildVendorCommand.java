package excelemfixes.fieldmedic;

import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /guildvendor — the generic guild-vendor buy flow, registered
 * dynamically by the field-medic module (it does not exist while
 * the module is disabled or standing down). The guild vendor
 * NPC's interaction runs the console form ({@code guildvendor
 * buy <good> <player>}); the buy ARMS a review at the vendor
 * price (25% under the ExcelShops list, D200) and the player
 * confirms with {@code /guildvendor confirm}. EliteMobs' own
 * NPC shops are engine-driven and cannot carry a fixed
 * discounted listing for a vanilla item, which is why the sale
 * rides this flow — the pattern the summon-mount module proved
 * with {@code mountcharm buy %player_name% npc}. Goods are the
 * {@link VendorGoods} table's NPC-venue rows (pearls, empty
 * map); the Strong Healing Potion's vendor sale runs through
 * {@code /fieldmedic buy npc} instead, per its listing.
 */
public final class GuildVendorCommand implements CommandExecutor, TabCompleter {

    private final FieldMedicModule module;

    GuildVendorCommand(FieldMedicModule module) {
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
            default -> usage(sender);
        }
        return true;
    }

    // ------------------------------------------------------------ buy

    private void buy(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender);
            return;
        }
        Optional<VendorGoods.Good> found = VendorGoods.byKey(args[1]);
        if (found.isEmpty() || found.get() == VendorGoods.POTION) {
            sender.sendMessage(ChatColor.RED + "Unknown vendor good '"
                    + args[1] + "'. The guild vendor sells: pearl,"
                    + " emptymap. (Potions: /fieldmedic buy npc)");
            return;
        }
        VendorGoods.Good good = found.get();
        Player buyer;
        if (sender instanceof Player player) {
            buyer = player;
        } else {
            if (args.length < 3) {
                sender.sendMessage("Console: guildvendor buy <good> <player>");
                return;
            }
            buyer = Bukkit.getPlayerExact(args[2]);
            if (buyer == null) {
                sender.sendMessage("Player is not online.");
                return;
            }
        }
        VendorBuyFlow flow = module.buyFlow();
        if (flow == null) {
            FieldMedicCommand.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        VendorBuyFlow.Result result =
                flow.arm(buyer, good, VendorGoods.Venue.NPC);
        switch (result) {
            case ARMED -> {
                VendorArms.Arm review = flow.armedFor(buyer.getUniqueId());
                double price = review != null ? review.price()
                        : flow.priceFor(good, VendorGoods.Venue.NPC);
                FieldMedicCommand.tell(buyer, ChatColor.YELLOW,
                        "Review purchase: " + good.displayName() + " — $"
                                + FieldMedicCommand.money(price)
                                + " (vendor price). Your balance: $"
                                + FieldMedicCommand.money(
                                        module.economy().getBalance(buyer))
                                + ". No charge yet — type "
                                + ChatColor.WHITE + "/guildvendor confirm"
                                + ChatColor.YELLOW + " within 30s to buy.");
            }
            case VENUE_NOT_OFFERED -> FieldMedicCommand.tell(buyer,
                    ChatColor.YELLOW,
                    "The guild vendor does not sell that here.");
            default -> FieldMedicCommand.tell(buyer, ChatColor.RED,
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
            FieldMedicCommand.tell(buyer, ChatColor.RED,
                    "Purchases are unavailable right now. Ask an admin.");
            return;
        }
        VendorArms.Arm review = flow.armedFor(buyer.getUniqueId());
        String goodName = review != null
                ? VendorGoods.byKey(review.goodKey())
                        .map(VendorGoods.Good::displayName).orElse("your purchase")
                : "your purchase";
        double price = review != null ? review.price() : 0;
        switch (flow.confirm(buyer)) {
            case CONFIRMED_PURCHASED -> FieldMedicCommand.tell(buyer,
                    ChatColor.GREEN, goodName + " bought for $"
                            + FieldMedicCommand.money(price)
                            + " at the guild vendor.");
            case NOT_ARMED -> FieldMedicCommand.tell(buyer, ChatColor.YELLOW,
                    "Nothing to confirm. The vendor arms a purchase"
                            + " when you use him.");
            case INELIGIBLE_NO_ROOM -> FieldMedicCommand.tell(buyer,
                    ChatColor.RED, "Make an empty inventory slot and"
                            + " try again. No charge was made.");
            case INSUFFICIENT_FUNDS -> FieldMedicCommand.tell(buyer,
                    ChatColor.RED, "Not enough money. Price $"
                            + FieldMedicCommand.money(price)
                            + ". No charge was made.");
            case DECLINED -> FieldMedicCommand.tell(buyer, ChatColor.RED,
                    "Payment declined. Nothing delivered.");
            case REFUNDED -> FieldMedicCommand.tell(buyer, ChatColor.YELLOW,
                    "Delivery failed; your payment was refunded in"
                            + " full. Try again with a free slot.");
            case REFUND_FAILED -> FieldMedicCommand.tell(buyer, ChatColor.RED,
                    "Something went wrong after payment. Do not pay"
                            + " again — an admin has been notified in"
                            + " the server log.");
            default -> FieldMedicCommand.tell(buyer, ChatColor.RED,
                    "Purchase unavailable. Try later, or ask an admin.");
        }
    }

    // ------------------------------------------------------------ helpers

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Guild Vendor: "
                + ChatColor.GRAY
                + "/guildvendor buy <pearl|emptymap> | confirm");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("buy", "confirm");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return List.of("pearl", "emptymap");
        }
        return List.of();
    }
}
