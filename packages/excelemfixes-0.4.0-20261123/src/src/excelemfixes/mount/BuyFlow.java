package excelemfixes.mount;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The two-step purchase: buy ARMS a review (no charge), confirm
 * charges and delivers. The price is snapshotted at arm time from
 * the venue — ExcelShops list price, or the vendor-NPC price when
 * the venue argument is NPC (P-SHOP-TWO-VENUE-20261122) — so the
 * number the player reviewed is the number they pay, even if the
 * config changes mid-review. Both venues charge Excel Bucks
 * through Vault; the NPC venue reaches this flow through a
 * console command (see the design note for why no EliteMobs-
 * native shop can be used).
 *
 * <p>Failure honesty mirrors the fleet's PurchaseCoordinator
 * outcomes at this product's scale: a delivery failure after
 * withdrawal refunds automatically, and a failed refund is
 * logged SEVERE with the player told to see an admin — never a
 * silent loss.
 *
 * <p>This class imports Vault and ExcelItems types; the module
 * constructs it only after both plugins are confirmed present.
 */
public final class BuyFlow {

    public enum Venue { SHOP, NPC }

    public enum Result {
        ARMED, CONFIRMED_PURCHASED, NOT_ARMED, ALREADY_OWNED,
        INELIGIBLE_NO_ROOM, INSUFFICIENT_FUNDS, DECLINED,
        REFUNDED, REFUND_FAILED, NO_ECONOMY, DISABLED
    }

    /** An armed review: the venue price, frozen at arm time. */
    public record Armed(double price, Venue venue, long expiresMillis) {}

    private static final long REVIEW_WINDOW_MILLIS = 30_000L;

    private final ExcelItemsAPI items;
    private final Economy economy;
    private final Logger logger;
    private volatile MountSettings settings;
    private final Map<UUID, Armed> armed = new HashMap<>();

    BuyFlow(ExcelItemsAPI items, Economy economy,
            MountSettings settings, Logger logger) {
        this.items = items;
        this.economy = economy;
        this.settings = settings;
        this.logger = logger;
    }

    /** Swap in reloaded settings (module reload). */
    void updateSettings(MountSettings newSettings) {
        this.settings = newSettings;
    }

    public double priceFor(Venue venue) {
        return venue == Venue.NPC ? settings.npcPrice() : settings.price();
    }

    /** Step 1 (no charge): validate and arm the review. */
    public Result arm(Player buyer, Venue venue) {
        if (settings.retired()) return Result.DISABLED;
        if (economy == null) return Result.NO_ECONOMY;
        if (ownsCharm(buyer)) return Result.ALREADY_OWNED;
        armed.put(buyer.getUniqueId(), new Armed(priceFor(venue), venue,
                System.currentTimeMillis() + REVIEW_WINDOW_MILLIS));
        return Result.ARMED;
    }

    public Armed armedFor(UUID buyer) {
        Armed review = armed.get(buyer);
        if (review == null) return null;
        if (System.currentTimeMillis() > review.expiresMillis()) {
            armed.remove(buyer);
            return null;
        }
        return review;
    }

    /** Step 2: charge the armed price and deliver the charm. */
    public Result confirm(Player buyer) {
        Armed review = armed.remove(buyer.getUniqueId());
        if (review == null
                || System.currentTimeMillis() > review.expiresMillis()) {
            return Result.NOT_ARMED;
        }
        if (settings.retired()) return Result.DISABLED;
        if (economy == null) return Result.NO_ECONOMY;
        if (ownsCharm(buyer)) return Result.ALREADY_OWNED;
        if (buyer.getInventory().firstEmpty() == -1) {
            return Result.INELIGIBLE_NO_ROOM;
        }
        double price = review.price();
        EconomyResponse withdrawal = economy.withdrawPlayer(buyer, price);
        if (!withdrawal.transactionSuccess()) {
            return economy.getBalance(buyer) < price
                    ? Result.INSUFFICIENT_FUNDS : Result.DECLINED;
        }
        ItemStack charm = MountRegistrar.createCharm(items);
        Map<Integer, ItemStack> leftover =
                buyer.getInventory().addItem(charm);
        if (!leftover.isEmpty()) {
            EconomyResponse refund = economy.depositPlayer(buyer, price);
            if (refund.transactionSuccess()) {
                logger.warning("summon-mount: Mount Charm delivery failed"
                        + " for " + buyer.getName() + " after payment;"
                        + " refunded " + price + ".");
                return Result.REFUNDED;
            }
            logger.severe("summon-mount: Mount Charm delivery failed for "
                    + buyer.getName() + " after payment of " + price
                    + " AND the refund failed (" + refund.errorMessage
                    + "). Player is owed a charm or " + price
                    + " Excel Bucks — resolve manually.");
            return Result.REFUND_FAILED;
        }
        logger.info("summon-mount: " + buyer.getName()
                + " bought a Mount Charm for " + price + " ("
                + review.venue() + " venue).");
        return Result.CONFIRMED_PURCHASED;
    }

    /** True when the player already holds a charm anywhere in
     *  their inventory (storage + offhand): it's permanent, and
     *  nobody should pay twice by accident. */
    public boolean ownsCharm(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && items.isItem(stack, MountItem.ID)) {
                return true;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand != null
                && items.isItem(offhand, MountItem.ID);
    }
}
