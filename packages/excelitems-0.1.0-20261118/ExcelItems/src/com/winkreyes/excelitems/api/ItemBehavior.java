package com.winkreyes.excelitems.api;

/**
 * A behavior handler registered against one registry ID (design law:
 * an item's behavior is written once in code at creation and
 * registered with the item). The handler's CODE lives in the owning
 * plugin — ExcelItems only keeps the routing table, so the registry
 * stays small, stable and boring. 0.1.0 ships the table and the
 * registration seam; the four builtin behaviors stay in their owning
 * plugins until the migration units wire them here.
 */
@FunctionalInterface
public interface ItemBehavior {

    /** Stable handler id, e.g. "excelmobveil:veil-drink". */
    String handlerId();
}
