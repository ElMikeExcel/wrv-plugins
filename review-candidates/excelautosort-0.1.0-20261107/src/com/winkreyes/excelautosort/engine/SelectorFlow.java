package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.core.SelectorModel;
import com.winkreyes.excelautosort.gui.SelectorSession;
import com.winkreyes.excelautosort.store.ChestRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The selector's shared flow logic (SPEC §7) — ONE implementation of
 * the safety semantics behind both renderers (Java GUI, Bedrock form):
 * contents-seeded entries, live list edits, confirm-gated arming, the
 * itemized sweep, suspension resume. Renderers only draw and report.
 */
public final class SelectorFlow {

    public enum Result { APPLIED, NEEDS_CONFIRM, DENIED, NOOP }

    private final ExcelAutoSort plugin;

    public SelectorFlow(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ session open

    /**
     * Opens a session — or returns null when the player holds no
     * configure rights over this chest (§6.4, Mike's rule of Nov 6,
     * 2026, 11:52 PM CST: no menu, no read-only view, no settings
     * leak — callers answer with the plain no-permission line).
     */
    public SelectorSession open(Player player, List<String> targetKeys) {
        ChestRecord r = recordOf(targetKeys);
        Location loc = locationOf(targetKeys);
        if (!plugin.rights().canConfigure(player, loc, r == null ? null : r.owner)) {
            return null;
        }
        SelectorSession s = new SelectorSession(player.getUniqueId(), targetKeys);
        if (r != null) {
            s.chestId = r.id;
            s.persistedMode = r.mode;
            s.persistedItems.addAll(r.items);
            s.workMode = r.mode;
            s.workItems.addAll(r.items);
        }
        return s;
    }

    public ChestRecord recordOf(SelectorSession s) {
        return recordOf(s.targetKeys);
    }

    public ChestRecord recordOf(List<String> targetKeys) {
        for (String k : targetKeys) {
            ChestRecord r = plugin.store().byKey(k);
            if (r != null) return r;
        }
        return null;
    }

    public Location locationOf(List<String> targetKeys) {
        if (targetKeys == null || targetKeys.isEmpty()) return null;
        ChestKeys.BlockKey bk = ChestKeys.parse(targetKeys.get(0));
        if (bk == null) return null;
        World w = Bukkit.getWorld(bk.world());
        return w == null ? null : new Location(w, bk.x(), bk.y(), bk.z());
    }

    public Inventory inventoryOf(SelectorSession s) {
        ChestRecord r = recordOf(s);
        if (r != null) {
            Inventory inv = plugin.filter().liveInventory(r);
            if (inv != null) return inv;
        }
        return FilterEngine.inventoryOf(s.targetKeys);
    }

    public List<FilterCore.ContentStack> contentsOf(SelectorSession s) {
        return FilterEngine.contentsOf(inventoryOf(s));
    }

    public List<SelectorModel.Entry> entriesOf(SelectorSession s) {
        return SelectorModel.entries(contentsOf(s), s.workItems);
    }

    /** The itemized rejects a mode + list would create in the chest right now (§5.8). */
    public List<FilterCore.RejectLine> rejectsOf(SelectorSession s, Mode mode, Set<String> items) {
        return FilterCore.rejects(mode, items, contentsOf(s));
    }

    // ------------------------------------------------------------ edits

    /** Tap a material on/off the working list (§7.3). */
    public Result toggleItem(SelectorSession s, String material) {
        if (material == null || material.isEmpty()) return Result.NOOP;
        LinkedHashSet<String> candidate = new LinkedHashSet<>(s.workItems);
        if (!candidate.remove(material)) candidate.add(material);
        s.workItems.clear();
        s.workItems.addAll(candidate);
        return settleListEdit(s);
    }

    /** Add a material (hand / inventory paths); same settle rules as a toggle-on. */
    public Result addItem(SelectorSession s, String material) {
        if (material == null || material.isEmpty() || s.workItems.contains(material)) return Result.NOOP;
        s.workItems.add(material);
        return settleListEdit(s);
    }

    /** Remove a material from the working list (staff text path); same settle rules. */
    public Result removeItem(SelectorSession s, String material) {
        if (material == null || !s.workItems.contains(material)) return Result.NOOP;
        s.workItems.remove(material);
        return settleListEdit(s);
    }

    /** Replace the working list at once (the Bedrock form submits a set, §7.5); same settle rules. */
    public Result applyListSet(SelectorSession s, Set<String> items) {
        s.workItems.clear();
        s.workItems.addAll(items);
        return settleListEdit(s);
    }

    /**
     * A working-list edit lands immediately UNLESS it creates rejects
     * among the chest's current contents that the persisted filter
     * would not already remove — those route through the confirm (§5.8).
     */
    private Result settleListEdit(SelectorSession s) {
        if (s.workMode == Mode.OFF) {
            persistList(s);
            return Result.APPLIED;
        }
        List<FilterCore.RejectLine> candidate = rejectsOf(s, s.workMode, s.workItems);
        if (candidate.isEmpty()) {
            persistList(s);
            return Result.APPLIED;
        }
        Set<String> candidateMats = new LinkedHashSet<>();
        candidate.forEach(l -> candidateMats.add(l.material()));
        Set<String> persistedMats = new LinkedHashSet<>();
        rejectsOf(s, s.persistedMode, s.persistedItems).forEach(l -> persistedMats.add(l.material()));
        if (persistedMats.containsAll(candidateMats)) {
            persistList(s);
            return Result.APPLIED;
        }
        s.pendingMode = s.workMode;
        s.pendingItems = new LinkedHashSet<>(s.workItems);
        return Result.NEEDS_CONFIRM;
    }

    private void persistList(SelectorSession s) {
        ChestRecord r = recordOf(s);
        if (r != null) {
            r.items.clear();
            r.items.addAll(s.workItems);
            r.touch(System.currentTimeMillis());
            plugin.store().saveChests();
        }
        s.syncPersisted();
    }

    // ------------------------------------------------------------ arming

    /** A mode-button tap (§7.4): OFF applies at once; a filtering mode always confirms (§6.1). */
    public Result requestMode(SelectorSession s, Mode mode) {
        if (mode == s.workMode && !isSuspended(s)) return Result.NOOP;
        if (mode == Mode.OFF) {
            s.workMode = Mode.OFF;
            ChestRecord r = recordOf(s);
            if (r != null) {
                r.mode = Mode.OFF;
                r.items.clear();
                r.items.addAll(s.workItems);
                r.touch(System.currentTimeMillis());
                plugin.store().saveChests();
            }
            s.syncPersisted();
            return Result.APPLIED;
        }
        s.pendingMode = mode;
        s.pendingItems = new LinkedHashSet<>(s.workItems);
        return Result.NEEDS_CONFIRM;
    }

    public boolean isSuspended(SelectorSession s) {
        ChestRecord r = recordOf(s);
        return r != null && r.suspended;
    }

    public String suspendReason(SelectorSession s) {
        ChestRecord r = recordOf(s);
        return r == null ? "" : r.suspendReason;
    }

    /**
     * The confirm (§5.8, §7.4): create the record if needed, sweep the
     * itemized rejects to the confirmer's buffer, and only then let the
     * mode + list take effect. Returns {stacks, items} swept.
     */
    public int[] confirm(Player player, SelectorSession s) {
        Mode mode = s.pendingMode;
        LinkedHashSet<String> items = s.pendingItems == null ? new LinkedHashSet<>() : s.pendingItems;
        ChestRecord r = recordOf(s);
        boolean resumed = false;
        if (r == null) {
            r = plugin.filter().createRecord(s.targetKeys, player.getUniqueId(), player.getName());
            s.chestId = r.id;
        } else {
            resumed = r.suspended;
        }
        int[] swept = plugin.filter().sweepRejects(r, mode, items, player);
        r.mode = mode;
        r.items.clear();
        r.items.addAll(items);
        r.suspended = false;
        r.suspendReason = "";
        r.touch(System.currentTimeMillis());
        plugin.store().saveChests();
        s.workMode = mode;
        s.workItems.clear();
        s.workItems.addAll(items);
        s.syncPersisted();
        s.clearPending();
        ChestKeys.BlockKey bk = r.canonicalBlock();
        plugin.sink().record(ProtectEvents.FILTER_ARM, ProtectEvents.filterArm(
                bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                r.canonicalKey(), player.getUniqueId().toString(), player.getName(),
                r.owner == null ? null : r.owner.toString(), mode.id(), items.size(),
                swept[0], swept[1], resumed));
        if (resumed) {
            plugin.sink().record(ProtectEvents.FILTER_RESUME, ProtectEvents.suspend(
                    bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                    r.canonicalKey(), r.owner == null ? null : r.owner.toString(), "re-confirmed"));
        }
        return swept;
    }

    /** "Not yet" (§7.4): nothing changes — the working copy reverts. */
    public void cancel(SelectorSession s) {
        s.revertToPersisted();
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
