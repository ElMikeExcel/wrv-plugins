package com.winkreyes.excelautosort.store;

import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.Mode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One filter-chest record (SPEC §5.1): a stable id, the block key(s)
 * it covers (two for a double chest), the owner at marking time, the
 * mode, the material list, timestamps, and the suspension flag (§6.4).
 */
public final class ChestRecord {

    public final UUID id;
    public List<String> keys = new ArrayList<>();
    public UUID owner;
    public String ownerName = "";
    public Mode mode = Mode.OFF;
    public final Set<String> items = new LinkedHashSet<>();
    public long createdMs;
    public long updatedMs;
    public boolean suspended;
    public String suspendReason = "";

    public ChestRecord(UUID id) {
        this.id = id;
    }

    /** The canonical key Protect records cite: the first stored key. */
    public String canonicalKey() {
        return keys.isEmpty() ? "" : keys.get(0);
    }

    /** Location parts of the canonical key, for Protect's envelope fields. */
    public ChestKeys.BlockKey canonicalBlock() {
        return keys.isEmpty() ? null : ChestKeys.parse(keys.get(0));
    }

    public boolean covers(String blockKey) {
        return keys.contains(blockKey);
    }

    public void touch(long nowMs) {
        this.updatedMs = nowMs;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
