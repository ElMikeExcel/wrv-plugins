package excelemfixes.mount;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.core.ItemDefinition;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * The ExcelItems seam for the summon-mount module (D199: the pack
 * soft-depends on ExcelItems; item-carrying modules require it).
 * This class — and BuyFlow — are the only module classes that
 * import ExcelItems types, and the module loads them only after
 * confirming the ExcelItems plugin is present, so an ExcelItems-
 * less server gets a clean stand-down instead of class-load
 * errors.
 *
 * Registration mirrors the registry law: claim the permanent ID
 * through the API at module enable, fail closed on a clash (IDs
 * are permanent and never reused), and never write ExcelItems'
 * config — while ExcelItems is 0.1.0 a config assignment line
 * for ID 5 would disable the registry at its own enable (proven
 * in the unit tests; see the build RECORD).
 */
public final class MountRegistrar {

    public enum Outcome { REGISTERED, ALREADY_REGISTERED, CLASH }

    private MountRegistrar() {}

    public static ExcelItemsAPI api() {
        return ExcelItemsAPI.get();
    }

    /** Claim registry ID 5 for the Mount Charm, or report why not. */
    public static Outcome ensureRegistered(ExcelItemsAPI items,
                                           Logger logger) {
        Optional<ItemDefinition> existing = items.definition(MountItem.ID);
        if (existing.isPresent()) {
            if (existing.get().key().equals(MountItem.KEY)) {
                items.registerBehavior(MountItem.ID,
                        () -> MountItem.HANDLER_ID);
                return Outcome.ALREADY_REGISTERED;
            }
            logger.severe("summon-mount: registry ID " + MountItem.ID
                    + " is already claimed by '" + existing.get().key()
                    + "'; refusing to register '" + MountItem.KEY
                    + "'. IDs are permanent — module standing down.");
            return Outcome.CLASH;
        }
        items.register(MountItem.DEFINITION);
        items.registerBehavior(MountItem.ID, () -> MountItem.HANDLER_ID);
        return Outcome.REGISTERED;
    }

    /** The CharmCheck the mount service gates its item event on. */
    public static CharmCheck charmCheck(ExcelItemsAPI items) {
        return stack -> stack != null && items.isItem(stack, MountItem.ID);
    }

    /** Mint a fresh charm (buy delivery, admin give). */
    public static ItemStack createCharm(ExcelItemsAPI items) {
        return items.create(MountItem.ID);
    }
}
