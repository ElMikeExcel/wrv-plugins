// Excel signature — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelautosortbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelAutoSortBridge — the status feed behind the Server Menu's
 * Auto-sort toggle.
 *
 * AutoSort (third-party, by nothub; live 0.1.17, MIT) exposes no
 * PlaceholderAPI expansion, no public status API and no status
 * subcommand: its only contracts are the /autosort toggle command and
 * its persisted state. This bridge supplies the missing read side —
 * a PlaceholderAPI expansion (%autosort_status% -> ON/OFF) — so the
 * ExcelMenu registry entry can be state-driven exactly like Silk
 * Touch Warnings (%silkwarnings_status%) and Aim Assist
 * (%aimassist_status%). It never writes AutoSort state; toggling
 * stays with the owner's own /autosort command.
 *
 * Read path (fail-soft, in order):
 *  1. The owner's LIVE in-memory actives set, read reflectively from
 *     its Plugin instance (field "actives", Set&lt;UUID&gt; — verified
 *     against the shipped 0.1.17 jar's bytecode). This is the same
 *     set its toggle command flips, so it can never be stale.
 *  2. The owner's state file, plugins/autosort/active.txt (rewritten
 *     synchronously on every toggle, so it agrees with memory).
 *  3. null — "unknown". ExcelMenu resolves the entry to its
 *     "unavailable" default variant; the bridge never guesses a state.
 *
 * Modular by house law: this is a replaceable adapter. If AutoSort
 * ever grows its own expansion, this bridge is deleted and the menu
 * entry is untouched.
 */
public final class ExcelAutoSortBridge extends JavaPlugin {

    private Object activesFieldOwner;
    private Field activesField;

    @Override
    public void onEnable() {
        Plugin owner = Bukkit.getPluginManager().getPlugin("AutoSort");
        if (owner == null || !owner.isEnabled()) {
            getLogger().warning("ExcelAutoSortBridge: AutoSort not found or not enabled; "
                    + "%autosort_status% will read unavailable until it is.");
        } else {
            bindActivesField(owner);
            if (activesField == null) {
                getLogger().warning("ExcelAutoSortBridge: could not bind AutoSort's actives set; "
                        + "falling back to its state file " + AutoSortStatus.ACTIVES_FILE + ".");
            }
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AutoSortExpansion(this).register();
            getLogger().info("ExcelAutoSortBridge enabled: %autosort_status% expansion registered "
                    + "(source: " + (activesField != null ? "live actives set" : "state file") + ").");
        } else {
            getLogger().warning("ExcelAutoSortBridge: PlaceholderAPI not enabled; "
                    + "the menu entry will read unavailable.");
        }
    }

    private void bindActivesField(Plugin owner) {
        Class<?> c = owner.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField("actives");
                f.setAccessible(true);
                activesFieldOwner = owner;
                activesField = f;
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (RuntimeException e) {
                return;
            }
        }
    }

    /**
     * The player's AutoSort state: "ON", "OFF", or null when it cannot
     * be determined (owner absent, both read paths failed).
     */
    public String autoSortStatus(UUID player) {
        if (player == null) return null;
        if (activesField != null) {
            try {
                Object v = activesField.get(activesFieldOwner);
                if (v instanceof Set<?> set) {
                    @SuppressWarnings("unchecked")
                    Set<UUID> actives = (Set<UUID>) set;
                    return AutoSortStatus.statusOf(actives, player);
                }
            } catch (RuntimeException | IllegalAccessException e) {
                // Fall through to the state file.
            }
        }
        Plugin owner = Bukkit.getPluginManager().getPlugin("AutoSort");
        if (owner == null || !owner.isEnabled()) return null;
        return AutoSortStatus.statusOf(
                AutoSortStatus.readActivesFile(AutoSortStatus.ACTIVES_FILE), player);
    }
}

// ExcelAutoSortBridge — Excel (Iris), for Wink Reyes Valley
