package com.winkreyes.exceltogether;

/** Pure bonus math, no Bukkit types, so it can be unit tested. */
public final class PartyBonus {
    public static final double DEFAULT_MULTIPLIER = 1.25;
    public static final double DEFAULT_RADIUS = 50.0;

    private final double multiplier;
    private final double radius;

    public PartyBonus(double multiplier, double radius) {
        this.multiplier = (Double.isFinite(multiplier) && multiplier > 0) ? multiplier : DEFAULT_MULTIPLIER;
        this.radius = (Double.isFinite(radius) && radius > 0) ? radius : DEFAULT_RADIUS;
    }

    public double multiplier() { return multiplier; }
    public double radius() { return radius; }

    /** Only positive payouts are boosted. Zero and negative (penalties) are left alone. */
    public double apply(double amount) {
        return amount > 0 ? amount * multiplier : amount;
    }

    /** True when the two points are in the same world and within the radius (inclusive). */
    public boolean within(boolean sameWorld, double dx, double dy, double dz) {
        if (!sameWorld) return false;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
