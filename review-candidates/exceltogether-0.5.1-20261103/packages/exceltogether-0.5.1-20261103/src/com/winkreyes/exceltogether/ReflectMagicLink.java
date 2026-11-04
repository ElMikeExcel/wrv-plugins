package com.winkreyes.exceltogether;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * ExcelTogether 0.5.1 - registers the two party-repair action classes
 * with Magic, reflectively, in the house style of ReflectMcmmoLink /
 * ReflectEmLink: no Magic type is named outside the action classes
 * themselves, and the action classes are named only as strings here,
 * so nothing in this path can fail to load when Magic is absent.
 *
 * Registration happens in ExcelTogether.onLoad, before any plugin's
 * onEnable: Magic loads its spells (and resolves their action classes)
 * in its own onEnable, so the classes must be in ActionFactory's
 * registry by then. ActionFactory.registerActionClass is a static
 * registry call that needs nothing from Magic's enable lifecycle.
 * Excel - Wink Reyes Valley.
 */
public final class ReflectMagicLink {
    private String failure;

    /** The outcome of the last register() attempt, for the awake line / log. */
    public String failure() { return failure; }

    /**
     * Register ExcelPartyRepair + ExcelRepairItem with Magic's
     * ActionFactory. Returns false (with failure() set) when Magic is
     * absent or its API differs; the caller leaves the link off and
     * ExcelTogether runs exactly as before.
     */
    public boolean register() {
        try {
            Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic == null) { failure = "Magic not installed"; return false; }
            Class<?> factory = Class.forName("com.elmakers.mine.bukkit.action.ActionFactory",
                    true, magic.getClass().getClassLoader());
            Method register = factory.getMethod("registerActionClass", String.class, Class.class);
            register.invoke(null, "ExcelPartyRepair",
                    Class.forName("com.winkreyes.exceltogether.ExcelPartyRepairAction"));
            register.invoke(null, "ExcelRepairItem",
                    Class.forName("com.winkreyes.exceltogether.ExcelRepairItemAction"));
            return true;
        } catch (Throwable t) {
            failure = t.toString();
            return false;
        }
    }
}
