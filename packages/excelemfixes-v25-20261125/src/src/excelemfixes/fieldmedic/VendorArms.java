package excelemfixes.fieldmedic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure purchase-review ledger (no Bukkit): who has an armed
 * review, for what good, at what venue, at what price. Time is
 * injected (millis) so unit tests drive it deterministically.
 *
 * <p>The semantics mirror the summon-mount buy flow, which
 * proved this shape against real Vault money:
 * <ul>
 *   <li>Arming CHARGES NOTHING. It snapshots the venue price, so
 *       the number the player reviewed is the number they pay,
 *       even if the config changes mid-review.</li>
 *   <li>One review per player across BOTH vendor commands
 *       ({@code /fieldmedic} and {@code /guildvendor} share this
 *       ledger through the one {@link VendorBuyFlow}): arming a
 *       new review replaces the old one, so a player can never
 *       hold two armed prices and confirm against the wrong
 *       one.</li>
 *   <li>A review lives 30 seconds. An expired review is dropped
 *       on sight — peek and consume both refuse it — and
 *       confirming against nothing (or against an expired
 *       review) is a clean NOT_ARMED, never a charge.</li>
 * </ul>
 */
public final class VendorArms {

    /** How long an armed review stays confirmable. */
    public static final long REVIEW_WINDOW_MILLIS = 30_000L;

    /** An armed review: the good, the venue, and the price frozen at arm time. */
    public record Arm(String goodKey, VendorGoods.Venue venue,
                      double price, long expiresMillis) {}

    private final Map<UUID, Arm> arms = new HashMap<>();

    /** Arm (or re-arm, replacing any live review) a purchase. No charge. */
    public Arm arm(UUID buyer, VendorGoods.Good good, VendorGoods.Venue venue,
                   double price, long nowMillis) {
        Arm arm = new Arm(good.key(), venue, price,
                nowMillis + REVIEW_WINDOW_MILLIS);
        arms.put(buyer, arm);
        return arm;
    }

    /** The buyer's live review, or null when none is armed or it expired. */
    public Arm peek(UUID buyer, long nowMillis) {
        Arm arm = arms.get(buyer);
        if (arm == null) return null;
        if (nowMillis > arm.expiresMillis()) {
            arms.remove(buyer);
            return null;
        }
        return arm;
    }

    /**
     * Take the buyer's live review for confirmation: removes and
     * returns it, or null when none is armed or it expired (the
     * caller reports NOT_ARMED and charges nothing).
     */
    public Arm consume(UUID buyer, long nowMillis) {
        Arm arm = peek(buyer, nowMillis);
        if (arm != null) arms.remove(buyer);
        return arm;
    }

    /** Drop any review (player gone, module stopping). */
    public void forget(UUID buyer) {
        arms.remove(buyer);
    }
}
