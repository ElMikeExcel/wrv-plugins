package com.winkreyes.excelwandbars;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
/** Predict local edits before Magic handles them. No inventory writes or item creation. */
final class FlexibleInventoryGuard {
    static boolean empty(ItemStack i) { return i == null || (i.getType() == org.bukkit.Material.AIR || i.getType() == org.bukkit.Material.CAVE_AIR || i.getType() == org.bukkit.Material.VOID_AIR) || i.getAmount() <= 0; }
    static boolean[] occupied(PlayerInventory inv) {
        boolean[] out=new boolean[9]; for(int i=0;i<9;i++)out[i]=!empty(inv.getItem(i)); return out;
    }
    static int emptyCount(boolean[] slots, int wand) {
        int n=0;for(int i=0;i<9;i++)if(i!=wand&&!slots[i])n++;return n;
    }
    static boolean blockClick(InventoryClickEvent e, PlayerInventory inv) {
        InventoryAction action=e.getAction();
        if(action==InventoryAction.NOTHING) return false;
        if(action==InventoryAction.UNKNOWN) return true; // Cannot prove inventory invariant for unknown mutation.
        if(action==InventoryAction.HOTBAR_MOVE_AND_READD) return true; // Re-add destination is not exposed.
        int wand=inv.getHeldItemSlot();
        int clicked=e.getClickedInventory()==inv?e.getSlot():-1;
        boolean hot=clicked>=0&&clicked<9;
        int key=e.getHotbarButton();
        boolean swap=action==InventoryAction.HOTBAR_SWAP;
        if(hot&&clicked==wand || swap&&key==wand) return true;
        boolean[] after=occupied(inv);
        if(swap&&key>=0&&key<9) {
            if(hot){boolean x=after[key];after[key]=after[clicked];after[clicked]=x;}
            else after[key]=!empty(e.getCurrentItem());
        } else if(hot) {
            switch(action) {
                case PLACE_ALL,PLACE_ONE,PLACE_SOME,SWAP_WITH_CURSOR -> after[clicked]=!empty(e.getCursor());
                case PICKUP_ALL,DROP_ALL_SLOT -> after[clicked]=false;
                default -> { } // Partial removal cannot consume the last empty space.
            }
        }
        if(action==InventoryAction.MOVE_TO_OTHER_INVENTORY && !hot && !empty(e.getCurrentItem())) {
            // Unknown shift destination: a stack may occupy several empty positions.
            // Permit only when even the worst case leaves one exit, rather than moving items ourselves.
            return e.getCurrentItem().getAmount() >= emptyCount(after,wand);
        }
        return emptyCount(after,wand)<1;
    }
    static boolean blockDrag(InventoryDragEvent e, PlayerInventory inv) {
        int wand=inv.getHeldItemSlot();boolean[] after=occupied(inv);
        for(var entry:e.getNewItems().entrySet()) {
            int raw=entry.getKey();
            if(e.getView().getInventory(raw)!=inv)continue;
            int slot=e.getView().convertSlot(raw);
            if(slot<0||slot>=9)continue;
            if(slot==wand)return true;
            after[slot]=!empty(entry.getValue());
        }
        return emptyCount(after,wand)<1;
    }
}
