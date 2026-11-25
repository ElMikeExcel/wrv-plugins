package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.core.ItemDefinition;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/**
 * The module's ONLY bridge to ExcelItems' API: registration and
 * identification. This class is loaded only after
 * {@link FieldMedicModule} has confirmed the ExcelItems plugin is
 * present — the module class itself imports no ExcelItems type, so
 * a server without ExcelItems loads the pack exactly as before
 * (the jobs-dummy-gate softdepend pattern, applied to the D199
 * item-solution model).
 */
public final class BandageItems {

    public enum Registration {
        /** The definition was not there; we registered it. */
        REGISTERED,
        /** ID 6 already holds our own key (a reload): proceed. */
        ALREADY_OURS,
        /** ID 6 or the key is claimed by something else: stand down. */
        CLASH
    }

    private BandageItems() {}

    /**
     * Register the Field Bandage definition + behavior label with
     * ExcelItems. Fail-closed: an ID or key claimed by another item
     * is never re-pointed — the registry's own duplicate rejection
     * is the backstop, this pre-check is what lets the module log
     * the clash in its own words and stand down cleanly.
     */
    public static Registration register(ExcelItemsAPI api) {
        Optional<ItemDefinition> byId = api.definition(BandageItem.REGISTRY_ID);
        if (byId.isPresent()) {
            return byId.get().key().equals(BandageItem.KEY)
                    ? Registration.ALREADY_OURS : Registration.CLASH;
        }
        if (api.definition(BandageItem.KEY).isPresent()) {
            return Registration.CLASH;
        }
        api.register(BandageItem.DEFINITION);
        api.registerBehavior(BandageItem.REGISTRY_ID, () -> BandageItem.HANDLER_ID);
        return Registration.REGISTERED;
    }

    /** True when the stack identifies as the Field Bandage (registry ID 6). */
    public static boolean isBandage(ExcelItemsAPI api, ItemStack stack) {
        return stack != null && !stack.getType().isAir()
                && api.isItem(stack, BandageItem.REGISTRY_ID);
    }
}
