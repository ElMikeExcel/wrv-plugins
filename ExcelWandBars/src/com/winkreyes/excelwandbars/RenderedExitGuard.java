package com.winkreyes.excelwandbars;
import com.elmakers.mine.bukkit.wand.WandInventory;
import java.util.List;
/** Actual rendered-item predicate used by the cancellable opening listener. */
final class RenderedExitGuard {
    static boolean safe(List<WandInventory> bars) {
        if (bars == null || bars.isEmpty()) return false;
        for (WandInventory bar : bars) {
            if (bar == null || bar.getSize() != 8) return false;
            boolean hasEmpty = false;
            for (int i=0;i<8;i++) {
                var item = bar.getItem(i);
                if (FlexibleInventoryGuard.empty(item)) hasEmpty = true;
            }
            if (!hasEmpty) return false;
        }
        return true;
    }
}
