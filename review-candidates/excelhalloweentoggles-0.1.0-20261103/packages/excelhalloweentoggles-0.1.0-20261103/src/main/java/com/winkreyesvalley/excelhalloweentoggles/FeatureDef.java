package com.winkreyesvalley.excelhalloweentoggles;

import java.util.Objects;

/**
 * ExcelHalloweenToggles — one toggleable Halloween scare, as data.
 *
 * A FeatureDef is built entirely from the companion's config.yml
 * {@code features:} registry (modularity law: the feature list is data,
 * not code). Adding or renaming a scare is a config edit; this class
 * never changes.
 *
 * Two kinds exist:
 *   AMBIENT — one of the Halloween plugin's six scheduled scares. The
 *             companion re-drives it through the plugin's public
 *             triggerEvent(name, ...) using the plugin's own min/max
 *             config paths and its eventDelayTicks() math.
 *   LOGIN   — the login pumpkin. No event name and no min/max; it is
 *             re-driven on join through applyPumpkinIfPossible().
 */
public final class FeatureDef {

    public enum Kind { AMBIENT, LOGIN }

    private final String key;
    private final String displayName;
    private final Kind kind;
    private final String halloweenEvent;   // AMBIENT only, else null
    private final String enabledPath;      // Halloween config flag the surgery turns off
    private final String minPath;          // AMBIENT only, else null
    private final String maxPath;          // AMBIENT only, else null
    private final int fallbackMin;         // used only if the Halloween config lacks the path
    private final int fallbackMax;
    private final String cooldownPath;     // LOGIN only, else null
    private final int fallbackCooldownMinutes;

    public FeatureDef(String key, String displayName, Kind kind,
                      String halloweenEvent, String enabledPath,
                      String minPath, String maxPath, int fallbackMin, int fallbackMax,
                      String cooldownPath, int fallbackCooldownMinutes) {
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.halloweenEvent = halloweenEvent;
        this.enabledPath = Objects.requireNonNull(enabledPath, "enabledPath");
        this.minPath = minPath;
        this.maxPath = maxPath;
        this.fallbackMin = fallbackMin;
        this.fallbackMax = fallbackMax;
        this.cooldownPath = cooldownPath;
        this.fallbackCooldownMinutes = fallbackCooldownMinutes;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public Kind kind() { return kind; }
    public boolean isLogin() { return kind == Kind.LOGIN; }
    public String halloweenEvent() { return halloweenEvent; }
    public String enabledPath() { return enabledPath; }
    public String minPath() { return minPath; }
    public String maxPath() { return maxPath; }
    public int fallbackMin() { return fallbackMin; }
    public int fallbackMax() { return fallbackMax; }
    public String cooldownPath() { return cooldownPath; }
    public int fallbackCooldownMinutes() { return fallbackCooldownMinutes; }
}
