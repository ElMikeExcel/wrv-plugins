package excelemfixes.mount;

import org.bukkit.inventory.ItemStack;

/**
 * "Is this stack the Mount Charm?" — the one question the mount
 * service needs answered by the ExcelItems registry. Kept as a
 * Bukkit-only functional interface so MountService never imports
 * an ExcelItems type: the implementation lives in MountRegistrar,
 * the only class (with BuyFlow) that touches the ExcelItems API,
 * and it is only loaded after the module has confirmed ExcelItems
 * is present.
 */
@FunctionalInterface
public interface CharmCheck {
    boolean isCharm(ItemStack stack);
}
