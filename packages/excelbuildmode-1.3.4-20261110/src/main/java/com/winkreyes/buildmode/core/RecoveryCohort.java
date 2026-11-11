package com.winkreyes.buildmode.core;

import java.util.*;

/** Safety boundary from private installation data, independent of mutable permission grants. */
public final class RecoveryCohort {
    private final Set<UUID> cohort, eligible;
    public RecoveryCohort(Set<UUID> cohort, Set<UUID> eligible) {
        this.cohort = Set.copyOf(cohort); this.eligible = Set.copyOf(eligible);
        if (this.cohort.isEmpty() || !this.cohort.containsAll(this.eligible)) throw new IllegalArgumentException("A verified nonempty recovery cohort must include every eligible builder");
    }
    public Set<UUID> members() { return cohort; }
    public boolean mayEnter(UUID player) { return player != null && eligible.contains(player); }
    public boolean mustLockWhenJournalUnknown(UUID player) { return player != null && cohort.contains(player); }
}
