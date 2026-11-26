package excelemfixes.mount;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
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
 * <p>Two products ride this flow: the Mount Charm (registry
 * ID 5) and the Ghast Charm (registry ID 9, D204). They share
 * the charge-and-deliver core ({@link #settle}) so the money
 * discipline exists exactly once; they differ in price, in the
 * item delivered, and in eligibility — the Ghast Charm adds
 * the quest gate (halfway through one quest package), checked
 * at arm AND again at confirm, because an earned item is
 * never sold on a stale reading.
 *
 * <p>Failure honesty mirrors the fleet's PurchaseCoordinator
 * outcomes at this product's scale: a delivery failure after
 * withdrawal refunds automatically, and a failed refund is
 * logged SEVERE with the player told to see an admin — never a
 * silent loss. Funds are checked BEFORE any withdrawal, never
 * inferred from the withdrawal's success: EssentialsX (the
 * Vault economy on the reference server) withdraws into
 * negative balances, so transactionSuccess alone would let a
 * broke buyer pay with money they do not have.
 *
 * <p>This class imports Vault and ExcelItems types; the module
 * constructs it only after both plugins are confirmed present.
 */
public final class BuyFlow {

    public enum Venue { SHOP, NPC }

    public enum Result {
        ARMED, CONFIRMED_PURCHASED, NOT_ARMED, ALREADY_OWNED,
        INELIGIBLE_NO_ROOM, INSUFFICIENT_FUNDS, DECLINED,
        REFUNDED, REFUND_FAILED, NO_ECONOMY, DISABLED,
        GATE_LOCKED, GATE_UNAVAILABLE
    }

    /** An armed review: the venue price, frozen at arm time. */
    public record Armed(double price, Venue venue, long expiresMillis) {}

    private static final long REVIEW_WINDOW_MILLIS = 30_000L;

    private final ExcelItemsAPI items;
    private final Economy economy;
    private final Logger logger;
    private volatile MountSettings settings;
    private volatile GhastSettings ghastSettings;
    private final GhastGate gate;
    private final Map<UUID, Armed> armed = new HashMap<>();
    private final Map<UUID, Armed> ghastArmed = new HashMap<>();
    private final Map<UUID, GhastGate.Progress> lastGateProgress =
            new HashMap<>();

    BuyFlow(ExcelItemsAPI items, Economy economy,
            MountSettings settings, GhastSettings ghastSettings,
            GhastGate gate, Logger logger) {
        this.items = items;
        this.economy = economy;
        this.settings = settings;
        this.ghastSettings = ghastSettings;
        this.gate = gate;
        this.logger = logger;
    }

    /** Swap in reloaded settings (module reload). */
    void updateSettings(MountSettings newSettings,
                        GhastSettings newGhastSettings) {
        this.settings = newSettings;
        this.ghastSettings = newGhastSettings;
    }

    public double priceFor(Venue venue) {
        return venue == Venue.NPC ? settings.npcPrice() : settings.price();
    }

    public double ghastPriceFor(Venue venue) {
        return venue == Venue.NPC ? ghastSettings.npcPrice()
                : ghastSettings.price();
    }

    // ------------------------------------------------------------ horse

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
        return settle(buyer, review,
                () -> MountRegistrar.createCharm(items), "Mount Charm");
    }

    /** True when the player already holds a charm anywhere in
     *  their inventory (storage + offhand): it's permanent, and
     *  nobody should pay twice by accident. */
    public boolean ownsCharm(Player player) {
        return ownsItem(player, MountItem.ID);
    }

    // ------------------------------------------------------------ ghast

    /** Step 1 for the Ghast Charm: the horse's checks plus the
     *  quest gate. A gate that cannot be read refuses the sale
     *  (GATE_UNAVAILABLE) — never waves it through. */
    public Result armGhast(Player buyer, Venue venue) {
        if (settings.retired()) return Result.DISABLED;
        if (economy == null) return Result.NO_ECONOMY;
        if (ownsGhastCharm(buyer)) return Result.ALREADY_OWNED;
        Result block = gateBlock(buyer);
        if (block != null) return block;
        ghastArmed.put(buyer.getUniqueId(),
                new Armed(ghastPriceFor(venue), venue,
                        System.currentTimeMillis() + REVIEW_WINDOW_MILLIS));
        return Result.ARMED;
    }

    public Armed ghastArmedFor(UUID buyer) {
        Armed review = ghastArmed.get(buyer);
        if (review == null) return null;
        if (System.currentTimeMillis() > review.expiresMillis()) {
            ghastArmed.remove(buyer);
            return null;
        }
        return review;
    }

    /** Step 2 for the Ghast Charm: the gate is read again
     *  before any money moves — the arm-time reading is a
     *  courtesy, not the sale's evidence. */
    public Result confirmGhast(Player buyer) {
        Armed review = ghastArmed.remove(buyer.getUniqueId());
        if (review == null
                || System.currentTimeMillis() > review.expiresMillis()) {
            return Result.NOT_ARMED;
        }
        if (settings.retired()) return Result.DISABLED;
        if (economy == null) return Result.NO_ECONOMY;
        if (ownsGhastCharm(buyer)) return Result.ALREADY_OWNED;
        Result block = gateBlock(buyer);
        if (block != null) return block;
        return settle(buyer, review,
                () -> MountRegistrar.createGhastCharm(items), "Ghast Charm");
    }

    public boolean ownsGhastCharm(Player player) {
        return ownsItem(player, GhastItem.ID);
    }

    /** The most recent gate reading for this buyer (arm or
     *  confirm), for refusal messages that quote real progress.
     *  Null when the gate has not been read for them. */
    public GhastGate.Progress gateProgressFor(UUID buyer) {
        return lastGateProgress.get(buyer);
    }

    /** Null = the sale may proceed. Otherwise the refusal. */
    private Result gateBlock(Player buyer) {
        if (!ghastSettings.gateEnabled()) return null;
        if (gate == null) {
            lastGateProgress.put(buyer.getUniqueId(),
                    GhastGate.Progress.unavailable());
            return Result.GATE_UNAVAILABLE;
        }
        GhastGate.Progress progress = gate.check(buyer.getUniqueId());
        lastGateProgress.put(buyer.getUniqueId(), progress);
        if (!progress.available()) return Result.GATE_UNAVAILABLE;
        return progress.passes() ? null : Result.GATE_LOCKED;
    }

    // ------------------------------------------------------------ shared

    private boolean ownsItem(Player player, int registryId) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && items.isItem(stack, registryId)) {
                return true;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand != null
                && items.isItem(offhand, registryId);
    }

    /** The charge-and-deliver core both products share: room
     *  check, funds check BEFORE withdrawal, withdrawal,
     *  delivery, automatic refund on delivery failure. */
    private Result settle(Player buyer, Armed review,
                          Supplier<ItemStack> create, String label) {
        if (buyer.getInventory().firstEmpty() == -1) {
            return Result.INELIGIBLE_NO_ROOM;
        }
        double price = review.price();
        // Funds are checked BEFORE withdrawing, never inferred
        // from the withdrawal's success: EssentialsX (the Vault
        // economy on the reference server) happily withdraws
        // into a negative balance, so transactionSuccess alone
        // would let a broke buyer pay with money they do not
        // have. The mirror rig caught exactly that on the
        // field-medic vendor flow on Nov 23, 2026 (a $50 balance
        // confirmed a $150 potion and ended at -$100); this
        // flow carried the same latent hole at the $7,500
        // price point until the same fix landed here. The
        // post-withdrawal failure mapping below stays as a
        // backstop for economies that do refuse.
        if (economy.getBalance(buyer) < price) {
            return Result.INSUFFICIENT_FUNDS;
        }
        EconomyResponse withdrawal = economy.withdrawPlayer(buyer, price);
        if (!withdrawal.transactionSuccess()) {
            return economy.getBalance(buyer) < price
                    ? Result.INSUFFICIENT_FUNDS : Result.DECLINED;
        }
        ItemStack charm = create.get();
        Map<Integer, ItemStack> leftover =
                buyer.getInventory().addItem(charm);
        if (!leftover.isEmpty()) {
            EconomyResponse refund = economy.depositPlayer(buyer, price);
            if (refund.transactionSuccess()) {
                logger.warning("summon-mount: " + label + " delivery failed"
                        + " for " + buyer.getName() + " after payment;"
                        + " refunded " + price + ".");
                return Result.REFUNDED;
            }
            logger.severe("summon-mount: " + label + " delivery failed for "
                    + buyer.getName() + " after payment of " + price
                    + " AND the refund failed (" + refund.errorMessage
                    + "). Player is owed a charm or " + price
                    + " Excel Bucks — resolve manually.");
            return Result.REFUND_FAILED;
        }
        logger.info("summon-mount: " + buyer.getName()
                + " bought a " + label + " for " + price + " ("
                + review.venue() + " venue).");
        return Result.CONFIRMED_PURCHASED;
    }
}
