package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.core.ItemDefinition;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/**
 * The field-medic module's ExcelItems bridge for the Strong
 * Healing Potion: registration and identification, mirroring
 * {@link BandageItems}. This class is loaded only after
 * {@link FieldMedicModule} has confirmed the ExcelItems plugin
 * is present — the module class itself imports no ExcelItems
 * type, so a server without ExcelItems loads the pack exactly
 * as before (the jobs-dummy-gate softdepend pattern, applied to
 * the D199 item-solution model).
 */
public final class PotionItems {

    public enum Registration {
        /** The definition was not there; we registered it. */
        REGISTERED,
        /** ID 7 already holds our own key (a reload): proceed. */
        ALREADY_OURS,
        /** ID 7 or the key is claimed by something else: stand down. */
        CLASH
    }

    private PotionItems() {}

    /**
     * Register the Strong Healing Potion definition + behavior
     * label with ExcelItems. Fail-closed: an ID or key claimed by
     * another item is never re-pointed — the registry's own
     * duplicate rejection is the backstop, this pre-check is what
     * lets the module log the clash in its own words and stand
     * down cleanly. Fail-closed matters doubly here: the
     * ExcelShops special-products listing names ID 7, so minting
     * anything else under this ID would sell the wrong item
     * under a healing name.
     */
    public static Registration register(ExcelItemsAPI api) {
        Optional<ItemDefinition> byId = api.definition(PotionItem.REGISTRY_ID);
        if (byId.isPresent()) {
            return byId.get().key().equals(PotionItem.KEY)
                    ? Registration.ALREADY_OURS : Registration.CLASH;
        }
        if (api.definition(PotionItem.KEY).isPresent()) {
            return Registration.CLASH;
        }
        api.register(PotionItem.DEFINITION);
        api.registerBehavior(PotionItem.REGISTRY_ID, () -> PotionItem.HANDLER_ID);
        return Registration.REGISTERED;
    }

    /** True when the stack identifies as the Strong Healing Potion (registry ID 7). */
    public static boolean isPotion(ExcelItemsAPI api, ItemStack stack) {
        return stack != null && !stack.getType().isAir()
                && api.isItem(stack, PotionItem.REGISTRY_ID);
    }

    /** Mint one Strong Healing Potion through the registry (buy delivery, admin give). */
    public static ItemStack createPotion(ExcelItemsAPI api) {
        return api.create(PotionItem.REGISTRY_ID);
    }
}
