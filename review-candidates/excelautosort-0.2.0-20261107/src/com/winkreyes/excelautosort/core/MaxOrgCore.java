package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Max Organization (SPEC §15) — the pure core: entitlement math, the
 * radius rule, the network view model, and the smart-deposit planner.
 * Everything here is plain JDK so the acceptance rules of §15.7 are
 * unit-tested off-server; the Bukkit engines execute these plans
 * against live inventories and re-verify every step (fail-closed).
 *
 * The design law of the feature (§15.4): the view is a WINDOW onto
 * the real chests, never a warehouse. Nothing in this core holds an
 * item; it only describes stacks that live in real inventories.
 */
public final class MaxOrgCore {

    private MaxOrgCore() {}

    public static final long DAY_MS = 86_400_000L;

    // ------------------------------------------------------------ entitlement

    /**
     * One player's Max Organization access state, persisted (§15.2):
     * a permanent flag, or a renter-until timestamp. Survives restarts;
     * not transferable.
     */
    public record Entitlement(boolean owned, long rentUntilMs) {
        public static final Entitlement NONE = new Entitlement(false, 0L);
    }

    public enum Access { NONE, RENTED, OWNED }

    /** Access right now: owned beats rented beats nothing. */
    public static Access access(Entitlement e, long nowMs) {
        if (e == null) return Access.NONE;
        if (e.owned()) return Access.OWNED;
        if (e.rentUntilMs() > nowMs) return Access.RENTED;
        return Access.NONE;
    }

    /** A rental that WAS held and has run out (the unlock screen says so). */
    public static boolean lapsed(Entitlement e, long nowMs) {
        return e != null && !e.owned() && e.rentUntilMs() > 0 && e.rentUntilMs() <= nowMs;
    }

    /** Renting from scratch: the expiry is {@code days} from now. */
    public static long rentExpiry(long nowMs, int days) {
        return nowMs + Math.max(1, days) * DAY_MS;
    }

    /**
     * Renting again: an ACTIVE rental extends from its current expiry
     * (the player bought {@code days} more, and keeps the days already
     * paid for); a lapsed or fresh rental starts from now, so re-rent
     * restores the ability instantly (§15.2). No rent ever converts to
     * equity — buying later is a separate, full-price purchase.
     */
    public static long renewalExpiry(Entitlement e, long nowMs, int days) {
        long base = (e != null && !e.owned() && e.rentUntilMs() > nowMs) ? e.rentUntilMs() : nowMs;
        return base + Math.max(1, days) * DAY_MS;
    }

    public static Entitlement afterRent(Entitlement e, long nowMs, int days) {
        if (e != null && e.owned()) return e; // already permanent; renting is refused upstream
        return new Entitlement(false, renewalExpiry(e, nowMs, days));
    }

    /** Buying ends permanent; the old rent timestamp is spent and cleared. */
    public static Entitlement afterBuy(Entitlement e) {
        return new Entitlement(true, 0L);
    }

    // ------------------------------------------------------------ radius (§15.3)

    /**
     * The radius rule: a chest is in the network when the 3D distance
     * from the player's position to the chest block's CENTER is at most
     * {@code radius}. A chest one block past the radius never appears
     * (§15.7 boundary case); the comparison is on squared distances so
     * the boundary is exact.
     */
    public static boolean withinRadius(double px, double py, double pz,
                                       int bx, int by, int bz, int radius) {
        double dx = (bx + 0.5) - px;
        double dy = (by + 0.5) - py;
        double dz = (bz + 0.5) - pz;
        double r = Math.max(0, radius);
        return dx * dx + dy * dy + dz * dz <= r * r + 1e-9;
    }

    // ------------------------------------------------------------ view model (§15.4)

    /** One physical stack, described — never held — by the view. */
    public record SlotStack(String material, String metaKey, int count, int maxStack) {
        public boolean empty() { return count <= 0 || material == null; }
        /** Similarity: same material AND same meta text (the sort engine's rule). */
        public boolean similar(SlotStack o) {
            return o != null && !empty() && !o.empty()
                    && material.equals(o.material)
                    && String.valueOf(metaKey).equals(String.valueOf(o.metaKey));
        }
    }

    /** One network chest's contents, slot order preserved. */
    public record ChestView(String chestKey, String world, int x, int y, int z,
                            List<SlotStack> slots) {}

    /** One row of the paged view: a real stack in a real chest. */
    public record ViewEntry(String chestKey, int slot, SlotStack stack,
                            String world, int x, int y, int z) {}

    /**
     * Flatten the network into view rows: chests in the order given
     * (the engine sorts by canonical key, so the view is stable),
     * slots ascending, empties skipped. Each row still names its real
     * chest and slot — a take executes against exactly that slot.
     */
    public static List<ViewEntry> flatten(List<ChestView> chests) {
        List<ViewEntry> out = new ArrayList<>();
        if (chests == null) return out;
        for (ChestView c : chests) {
            if (c == null || c.slots() == null) continue;
            for (int i = 0; i < c.slots().size(); i++) {
                SlotStack s = c.slots().get(i);
                if (s == null || s.empty()) continue;
                out.add(new ViewEntry(c.chestKey(), i, s, c.world(), c.x(), c.y(), c.z()));
            }
        }
        return out;
    }

    /** How much of {@code stack} fits into an inventory shaped like {@code slots}. */
    public static int fitsCount(List<SlotStack> slots, SlotStack stack) {
        if (stack == null || stack.empty() || slots == null) return 0;
        int room = 0;
        for (SlotStack s : slots) {
            if (s == null || s.empty()) room += stack.maxStack();
            else if (s.similar(stack)) room += Math.max(0, s.maxStack() - s.count());
        }
        return room;
    }

    // ------------------------------------------------------------ smart deposit (§15.5)

    /** A stack in the player's inventory, by index (the plan cites indexes). */
    public record PlayerStack(int index, SlotStack stack) {}

    /**
     * A destination chest for the planner: its filter shape (mode +
     * list; an unmarked chest is mode OFF, which accepts everything)
     * and its slots (null or count-0 = empty).
     */
    public record DepositChest(String chestKey, Mode mode, Set<String> filterList,
                               List<SlotStack> slots) {}

    /** One planned placement: {@code count} items of the player stack into one chest. */
    public record DepositMove(int playerIndex, String chestKey, int count) {}

    /** The planner's answer: every move, plus what had nowhere to go. */
    public record DepositPlan(List<DepositMove> moves, List<Unplaced> unplaced) {}

    /** A player stack (or part of it) that no network chest would take. */
    public record Unplaced(int playerIndex, String material, int count) {}

    /** §15.5: would this chest take this material under its filter? */
    public static boolean accepts(DepositChest chest, String material) {
        if (chest == null || material == null) return false;
        Mode mode = chest.mode() == null ? Mode.OFF : chest.mode();
        return switch (mode) {
            case OFF -> true;
            case WHITELIST -> chest.filterList() != null && chest.filterList().contains(material);
            case BLACKLIST -> chest.filterList() == null || !chest.filterList().contains(material);
        };
    }

    /**
     * Preference tier (§15.7: "always prefers a whitelist chest that
     * lists it"): 0 = whitelist chest listing the material, 1 = open
     * (unmarked / filter-off) chest, 2 = blacklist chest not listing
     * it, -1 = refuses it.
     */
    public static int tier(DepositChest chest, String material) {
        if (!accepts(chest, material)) return -1;
        Mode mode = chest.mode() == null ? Mode.OFF : chest.mode();
        return switch (mode) {
            case WHITELIST -> 0;
            case OFF -> 1;
            case BLACKLIST -> 2;
        };
    }

    /**
     * Plan a smart deposit. Stacks are placed player-order first; each
     * stack walks the preference tiers, and inside a tier the chests
     * in network order, topping up similar partial stacks before using
     * empty slots. The planner mutates only its own working copy of
     * the chest slots — the engine re-verifies capacity against the
     * live inventories before moving a single item, so a stale plan
     * degrades to "stays with the player", never to a void.
     */
    public static DepositPlan planDeposit(List<PlayerStack> player, List<DepositChest> chests) {
        List<DepositMove> moves = new ArrayList<>();
        List<Unplaced> unplaced = new ArrayList<>();
        if (player == null || player.isEmpty()) return new DepositPlan(moves, unplaced);
        // Working copies of destination slots (lists of mutable stacks).
        List<List<SlotStack>> work = new ArrayList<>();
        List<DepositChest> dest = chests == null ? List.of() : chests;
        for (DepositChest c : dest) {
            List<SlotStack> slots = new ArrayList<>();
            if (c != null && c.slots() != null) slots.addAll(c.slots());
            work.add(slots);
        }
        for (PlayerStack ps : player) {
            if (ps == null || ps.stack() == null || ps.stack().empty()) continue;
            int remaining = ps.stack().count();
            // Chest visit order: tier ascending, network order inside a tier.
            List<Integer> order = new ArrayList<>();
            for (int t = 0; t <= 2 && remaining > 0; t++) {
                for (int ci = 0; ci < dest.size(); ci++) {
                    if (tier(dest.get(ci), ps.stack().material()) == t) order.add(ci);
                }
            }
            Set<Integer> visited = new LinkedHashSet<>(order);
            for (int ci : visited) {
                if (remaining <= 0) break;
                List<SlotStack> slots = work.get(ci);
                int capacity = 0;
                for (SlotStack s : slots) {
                    if (s == null || s.empty()) capacity += ps.stack().maxStack();
                    else if (s.similar(ps.stack())) capacity += Math.max(0, s.maxStack() - s.count());
                }
                if (capacity <= 0) continue;
                int put = Math.min(remaining, capacity);
                applyToSlots(slots, ps.stack(), put);
                moves.add(new DepositMove(ps.index(), dest.get(ci).chestKey(), put));
                remaining -= put;
            }
            if (remaining > 0) {
                unplaced.add(new Unplaced(ps.index(), ps.stack().material(), remaining));
            }
        }
        return new DepositPlan(moves, unplaced);
    }

    /** Fill similar partials first, then empty slots (mirrors vanilla addItem order). */
    private static void applyToSlots(List<SlotStack> slots, SlotStack stack, int count) {
        int left = count;
        for (int i = 0; i < slots.size() && left > 0; i++) {
            SlotStack s = slots.get(i);
            if (s != null && s.similar(stack) && s.count() < s.maxStack()) {
                int add = Math.min(left, s.maxStack() - s.count());
                slots.set(i, new SlotStack(s.material(), s.metaKey(), s.count() + add, s.maxStack()));
                left -= add;
            }
        }
        for (int i = 0; i < slots.size() && left > 0; i++) {
            SlotStack s = slots.get(i);
            if (s == null || s.empty()) {
                int add = Math.min(left, stack.maxStack());
                slots.set(i, new SlotStack(stack.material(), stack.metaKey(), add, stack.maxStack()));
                left -= add;
            }
        }
    }

    // ------------------------------------------------------------ money display

    /** "$1,000" / "$15,000" / "$9.99" — whole dollars carry no cents. */
    public static String money(double amount) {
        long whole = Math.round(amount);
        if (Math.abs(amount - whole) < 0.001) {
            return "$" + String.format("%,d", whole);
        }
        return "$" + String.format("%,.2f", amount);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
