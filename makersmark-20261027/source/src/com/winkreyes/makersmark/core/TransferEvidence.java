package com.winkreyes.makersmark.core;

import java.util.List;
import java.util.OptionalInt;

/** Fail-closed proof for one unstackable external item becoming a new player-held item.
 * Fingerprints must contain all metadata, and must be immutable snapshots. */
public final class TransferEvidence {
    private TransferEvidence() {}

    public static <T> int matching(List<T> slots, T wanted) {
        if (wanted == null) return 0;
        int count = 0;
        for (T slot : slots) if (wanted.equals(slot)) count++;
        return count;
    }

    /** Returns the only new matching destination, never an existing identical item.
     * Source counts are snapshots from the real external inventory (or one ground entity).
     * This rejects moved/ambiguous duplicates, disappearing destinations and partial transfers; stationary duplicates are safe. */
    public static <T> OptionalInt uniqueNewSlot(List<T> before, List<T> after, T wanted,
            int sourceBefore, int sourceAfter, boolean finalCancelled, boolean contextValid) {
        if (finalCancelled || !contextValid || wanted == null || sourceBefore != 1 || sourceAfter != 0)
            return OptionalInt.empty();
        if (before.size()!=after.size() || matching(after,wanted)!=matching(before,wanted)+1) return OptionalInt.empty();
        int found=-1;
        for (int i=0;i<after.size();i++) {
            // Existing identical items must stay in their original slots.
            if(wanted.equals(before.get(i)) && !wanted.equals(after.get(i))) return OptionalInt.empty();
            if(wanted.equals(after.get(i)) && !wanted.equals(before.get(i))) {
                if(found>=0) return OptionalInt.empty();
                found=i;
            }
        }
        return found<0 ? OptionalInt.empty() : OptionalInt.of(found);
    }
}
