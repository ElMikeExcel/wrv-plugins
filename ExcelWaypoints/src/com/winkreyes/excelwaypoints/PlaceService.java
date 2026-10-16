package com.winkreyes.excelwaypoints;

import java.util.*;
import java.util.function.LongSupplier;

/** Rules for saved places. No Bukkit types. */
public final class PlaceService {
    public enum R { OK, EMPTY_NAME, TOO_MANY, DUPLICATE, BLOCKED_WORLD, NOT_FOUND, NOT_OWNER }
    public enum Visit { OK, OWNER_OFFLINE, NOT_SHARED, VISITS_OFF, IS_OWNER, COOLDOWN, NO_ESSENTIALS }

    public static final class Result {
        public final R r; public final Place place;
        Result(R r, Place p) { this.r = r; this.place = p; }
        public boolean ok() { return r == R.OK; }
    }

    private final PlaceStore store;
    private final int limit;
    private final Set<String> blockedWorlds;
    private final LongSupplier clock;
    private final long visitCooldownMs;
    private final Map<UUID, Long> lastVisit = new HashMap<>();

    public PlaceService(PlaceStore store, int limit, Collection<String> blockedWorlds, long visitCooldownMs, LongSupplier clock) {
        this.store = store; this.limit = limit < 1 ? 5 : limit; this.clock = clock; this.visitCooldownMs = Math.max(0, visitCooldownMs);
        this.blockedWorlds = new HashSet<>();
        for (String w : blockedWorlds) this.blockedWorlds.add(w.toLowerCase(Locale.ROOT));
    }
    public int limit() { return limit; }
    public PlaceStore store() { return store; }

    public synchronized Result save(UUID owner, String ownerName, String rawName, Place.Type type, String world,
                                    double x, double y, double z, boolean shareable, boolean allowVisit) {
        String name = Names.clean(rawName);
        if (name.isEmpty()) return new Result(R.EMPTY_NAME, null);
        if (world == null || blockedWorlds.contains(world.toLowerCase(Locale.ROOT))) return new Result(R.BLOCKED_WORLD, null);
        List<Place> mine = store.ownedBy(owner);
        if (mine.size() >= limit) return new Result(R.TOO_MANY, null);
        for (Place p : mine) if (p.name.equalsIgnoreCase(name)) return new Result(R.DUPLICATE, p);
        Place p = new Place(store.newId(), owner, ownerName, name, type == null ? Place.Type.OTHER : type, world, x, y, z,
                shareable, shareable && allowVisit, clock.getAsLong());
        store.add(p);
        store.save();
        return new Result(R.OK, p);
    }

    private Place own(UUID owner, String id) {
        Place p = store.get(id);
        return p != null && p.owner.equals(owner) ? p : null;
    }

    public synchronized Result rename(UUID owner, String id, String rawName) {
        Place p = own(owner, id);
        if (p == null) return new Result(R.NOT_FOUND, null);
        String name = Names.clean(rawName);
        if (name.isEmpty()) return new Result(R.EMPTY_NAME, p);
        for (Place o : store.ownedBy(owner)) if (o != p && o.name.equalsIgnoreCase(name)) return new Result(R.DUPLICATE, o);
        p.name = name; store.save();
        return new Result(R.OK, p);
    }
    public synchronized Result delete(UUID owner, String id) {
        Place p = own(owner, id);
        if (p == null) return new Result(R.NOT_FOUND, null);
        store.remove(p.id); store.save();
        return new Result(R.OK, p);
    }
    public synchronized Result setShare(UUID owner, String id, boolean shareable) {
        Place p = own(owner, id);
        if (p == null) return new Result(R.NOT_FOUND, null);
        p.shareable = shareable;
        if (!shareable) p.allowVisit = false;
        store.save();
        return new Result(R.OK, p);
    }
    public synchronized Result setVisits(UUID owner, String id, boolean on) {
        Place p = own(owner, id);
        if (p == null) return new Result(R.NOT_FOUND, null);
        if (on && !p.shareable) p.shareable = true;   // visits only make sense on a shared place
        p.allowVisit = on;
        store.save();
        return new Result(R.OK, p);
    }
    /** Staff delete, any owner. */
    public synchronized Result adminDelete(String id) {
        Place p = store.get(id);
        if (p == null) return new Result(R.NOT_FOUND, null);
        store.remove(p.id); store.save();
        return new Result(R.OK, p);
    }

    public List<Place> mine(UUID owner) { return store.ownedBy(owner); }

    /** Other players' places that the owner marked shareable. Never your own, never private ones. */
    public List<Place> family(UUID viewer) {
        List<Place> l = new ArrayList<>();
        for (Place p : store.all()) if (!p.owner.equals(viewer) && p.shareable) l.add(p);
        l.sort(Comparator.comparing((Place p) -> p.ownerName.toLowerCase(Locale.ROOT)).thenComparing(p -> p.name.toLowerCase(Locale.ROOT)));
        return l;
    }
    public Place viewable(UUID viewer, String id) {
        Place p = store.get(id);
        if (p == null) return null;
        return p.owner.equals(viewer) || p.shareable ? p : null;
    }
    public void refreshOwnerName(UUID owner, String name) {
        boolean ch = false;
        for (Place p : store.ownedBy(owner)) if (!p.ownerName.equals(name)) { p.ownerName = name; ch = true; }
        if (ch) store.save();
    }

    /** Can this viewer ask this place's owner to visit? Checks only; the /tpa accept still belongs to the owner. */
    public synchronized Visit canAskVisit(UUID requester, Place p, boolean ownerOnline, boolean tpaAvailable) {
        if (p.owner.equals(requester)) return Visit.IS_OWNER;
        if (!p.shareable) return Visit.NOT_SHARED;
        if (!p.allowVisit) return Visit.VISITS_OFF;
        if (!ownerOnline) return Visit.OWNER_OFFLINE;
        if (!tpaAvailable) return Visit.NO_ESSENTIALS;
        Long last = lastVisit.get(requester);
        if (last != null && clock.getAsLong() - last < visitCooldownMs) return Visit.COOLDOWN;
        return Visit.OK;
    }
    public synchronized void markVisitSent(UUID requester) { lastVisit.put(requester, clock.getAsLong()); }
}
