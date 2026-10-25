package com.winkreyes.excelvaults.core;

/**
 * Pay-to-reopen price.
 * Scaled mode (default): {@code full * left/cooldown}, so a vault that just locked costs {@code full},
 * one with half its cooldown left costs half, and so on. Rounded to the NEAREST multiple of {@code step}
 * (half up), then floored at {@code min} so it is never near-free, and capped at {@code full}.
 * Flat mode: always {@code full}. Returns 0 when nothing is left (the vault is ready, no purchase).
 */
public final class Pricing {
    private Pricing() {}
    public static double price(long leftMs, long cooldownMs, double full, double min, boolean scale, double step) {
        if (leftMs <= 0) return 0;
        if (full < 0) full = 0;
        if (min > full) min = full;
        if (min < 0) min = 0;
        double raw;
        if (!scale || cooldownMs <= 0) raw = full;
        else {
            double f = Math.min(1.0, Math.max(0.0, (double) leftMs / (double) cooldownMs));
            raw = full * f;
            if (step > 0) raw = Math.floor(raw / step + 0.5 + 1e-9) * step;
        }
        return Math.max(min, Math.min(full, raw));
    }
}
