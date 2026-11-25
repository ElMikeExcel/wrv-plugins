/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.*;

/** Pure session policy, independent of item metadata and caller-controlled names. */
public final class ClaimSession {
    public static final int PAGE_SIZE = 45;
    private final UUID owner;
    private final List<ClaimGateway.Pending> entries;
    private final int page;
    private boolean live = true;
    public ClaimSession(UUID owner, List<ClaimGateway.Pending> entries, int requestedPage) {
        this.owner = Objects.requireNonNull(owner);
        this.entries = List.copyOf(entries);
        Set<String> ids = new HashSet<>();
        for (var entry : entries) if (!ids.add(entry.id())) throw new IllegalArgumentException("duplicate claim ID");
        this.page = Math.max(0, Math.min(requestedPage, Math.max(0, (entries.size()-1)/PAGE_SIZE)));
    }
    public UUID owner() { return owner; }
    public int page() { return page; }
    public boolean isLive() { return live; }
    public void close() { live = false; }
    public List<ClaimGateway.Pending> visible() {
        int start = page*PAGE_SIZE;
        return entries.subList(start, Math.min(start+PAGE_SIZE, entries.size()));
    }
    public Optional<String> request(UUID actor, int rawSlot, boolean plainLeftClick) {
        if (!live || !owner.equals(actor) || !plainLeftClick || rawSlot<0 || rawSlot>=visible().size()) return Optional.empty();
        // Consume session BEFORE external delivery. A second queued click cannot double-submit.
        live = false;
        return Optional.of(visible().get(rawSlot).id());
    }
}
