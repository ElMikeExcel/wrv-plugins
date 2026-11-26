package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The shared purchase engine behind {@code /fieldmedic buy} and
 * {@code /guildvendor buy}: buy ARMS a review (no charge),
 * confirm charges and delivers. The price is snapshotted at arm
 * time from the venue (see {@link VendorArms}), so the number
 * the player reviewed is the number they pay even if the config
 * changes mid-review. Both venues charge Excel Bucks through
 * Vault — the one balance ExcelShops, EliteMobs vendors and
 * Jobs all share.
 *
 * <p>Failure honesty mirrors the summon-mount buy flow, which
 * proved this shape first: the inventory-room check runs BEFORE
 * any withdrawal (a full inventory is refused with no charge);
 * a delivery failure after withdrawal refunds automatically;
 * and a failed refund is logged SEVERE with the player told to
 * see an admin — never a silent loss.
 *
 * <p>This class imports Vault and ExcelItems types; the module
 * constructs it only after ExcelItems is confirmed present and
 * a Vault economy is hooked, so neither dependency can break
 * the pack's class loading when absent.
 */
public final class VendorBuyFlow {

    public enum Result {
        ARMED, CONFIRMED_PURCHASED, NOT_ARMED, VENUE_NOT_OFFERED,
        INELIGIBLE_NO_ROOM, INSUFFICIENT_FUNDS, DECLINED,
        REFUNDED, REFUND_FAILED
    }

    private final ExcelItemsAPI items;
    private final Economy economy;
    private final Logger logger;
    private volatile VendorGoods.Prices prices;
    private final VendorArms arms = new VendorArms();

    public VendorBuyFlow(ExcelItemsAPI items, Economy economy,
                         VendorGoods.Prices prices, Logger logger) {
        this.items = items;
        this.economy = economy;
        this.prices = prices;
        this.logger = logger;
    }

    /** Swap in reloaded prices (module reload). Armed reviews keep their snapshot. */
    public void updatePrices(VendorGoods.Prices newPrices) {
        this.prices = newPrices;
    }

    public VendorGoods.Prices prices() {
        return prices;
    }

    /** The price of one good at one venue under current config, or -1 when not offered. */
    public double priceFor(VendorGoods.Good good, VendorGoods.Venue venue) {
        return VendorGoods.price(good, venue, prices);
    }

    /** Step 1 (no charge): validate the venue and arm the review. */
    public Result arm(Player buyer, VendorGoods.Good good, VendorGoods.Venue venue) {
        double price = priceFor(good, venue);
        if (price < 0) return Result.VENUE_NOT_OFFERED;
        arms.arm(buyer.getUniqueId(), good, venue, price,
                System.currentTimeMillis());
        return Result.ARMED;
    }

    /** The buyer's live armed review, or null. */
    public VendorArms.Arm armedFor(UUID buyer) {
        return arms.peek(buyer, System.currentTimeMillis());
    }

    /** Step 2: charge the armed price and deliver the good. */
    public Result confirm(Player buyer) {
        VendorArms.Arm review = arms.consume(buyer.getUniqueId(),
                System.currentTimeMillis());
        if (review == null) return Result.NOT_ARMED;
        VendorGoods.Good good = VendorGoods.byKey(review.goodKey()).orElse(null);
        if (good == null) return Result.NOT_ARMED; // table changed under a live arm
        if (buyer.getInventory().firstEmpty() == -1) {
            return Result.INELIGIBLE_NO_ROOM;
        }
        double price = review.price();
        // Funds are checked BEFORE withdrawing, never inferred
        // from the withdrawal's success: EssentialsX (the Vault
        // economy on the reference server) happily withdraws
        // into a negative balance, so transactionSuccess alone
        // would let a broke buyer pay with money they do not
        // have. The mirror rig caught exactly that on
        // Nov 23, 2026 (a $50 balance confirmed a $150 potion
        // and ended at -$100). The post-withdrawal failure
        // mapping below stays as a backstop for economies that
        // do refuse.
        if (economy.getBalance(buyer) < price) {
            return Result.INSUFFICIENT_FUNDS;
        }
        EconomyResponse withdrawal = economy.withdrawPlayer(buyer, price);
        if (!withdrawal.transactionSuccess()) {
            return economy.getBalance(buyer) < price
                    ? Result.INSUFFICIENT_FUNDS : Result.DECLINED;
        }
        ItemStack stack = mint(good);
        Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            EconomyResponse refund = economy.depositPlayer(buyer, price);
            if (refund.transactionSuccess()) {
                logger.warning("field-medic vendor: delivery of "
                        + good.displayName() + " failed for " + buyer.getName()
                        + " after payment; refunded " + price + ".");
                return Result.REFUNDED;
            }
            logger.severe("field-medic vendor: delivery of " + good.displayName()
                    + " failed for " + buyer.getName() + " after payment of "
                    + price + " AND the refund failed (" + refund.errorMessage
                    + "). Player is owed the item or " + price
                    + " Excel Bucks — resolve manually.");
            return Result.REFUND_FAILED;
        }
        logger.info("field-medic vendor: " + buyer.getName() + " bought "
                + good.displayName() + " for " + price + " ("
                + review.venue() + " venue).");
        return Result.CONFIRMED_PURCHASED;
    }

    /** Mint one delivery stack for a good (registry mint or vanilla stack). */
    private ItemStack mint(VendorGoods.Good good) {
        return switch (good.delivery()) {
            case REGISTRY_ITEM -> items.create(good.registryId());
            case VANILLA_STACK -> new ItemStack(
                    Material.valueOf(good.material()), good.amount());
        };
    }
}
