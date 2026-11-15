// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/**
 * Marks an inventory as a rearrange-editor view (ExcelMenu 3.0,
 * DESIGN §3.6) and remembers what each slot DOES — the click
 * listener trusts this map, never the ItemStack (the RegistryMenus
 * holder rule). Every click in an editor inventory is cancelled;
 * no item ever leaves one.
 */
final class MenuEditorHolder implements InventoryHolder {

    enum Screen { ROOT, GROUP, SECTION_ORDER }

    /** What a slot does when clicked. */
    record Action(Kind kind, String id) {
        enum Kind { GROUP, SECTION_ORDER, RESET_GLOBAL, BACK,
                    ENTRY, MOVE_TOP, MOVE_BOTTOM, RESET_GROUP }
        static Action group(String g) { return new Action(Kind.GROUP, g); }
        static Action entry(String id) { return new Action(Kind.ENTRY, id); }
        static Action of(Kind kind) { return new Action(kind, null); }
    }

    private final UUID player;
    private final Screen screen;
    private final String group; // GROUP screen only
    private String selectedId;  // GROUP / SECTION_ORDER selection
    private boolean resetArmed; // ROOT two-step reset (J4)
    private final Map<Integer, Action> actions;
    private Inventory inventory;

    MenuEditorHolder(UUID player, Screen screen, String group,
                     Map<Integer, Action> actions) {
        this.player = player;
        this.screen = screen;
        this.group = group;
        this.actions = actions;
    }

    UUID player() { return player; }
    Screen screen() { return screen; }
    String group() { return group; }
    String selectedId() { return selectedId; }
    void selectedId(String id) { this.selectedId = id; }
    boolean resetArmed() { return resetArmed; }
    void resetArmed(boolean armed) { this.resetArmed = armed; }
    Action at(int slot) { return actions.get(slot); }
    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
