// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.api.ExcelMenuApi;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryRegistry;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.RegistryDiff;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.registry.ViewerContext;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the live registry: load, validate (materials against the live
 * Bukkit set), install, reload. The failure posture is ExcelMenu's
 * founding rule — a bad entry file is refused LOUDLY (one SEVERE per
 * problem) while the rest of the registry still serves, and a reload
 * that would empty a non-empty registry is rejected outright with the
 * previous set staying live.
 *
 * Every load/reload prints the census-P4 diff summary (added /
 * removed / changed ids vs the previous set), so a regression is
 * visible in the log the minute it lands.
 */
final class EntryService implements ExcelMenuApi {

    private final JavaPlugin plugin;
    private final EntryRegistry registry = new EntryRegistry();
    private volatile RegistryConfig config = RegistryConfig.defaults();
    private volatile Map<String, List<String>> lastFailures = Map.of();

    EntryService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void configure(RegistryConfig config) {
        this.config = config;
    }

    RegistryConfig config() {
        return config;
    }

    EntryRegistry registry() {
        return registry;
    }

    Map<String, List<String>> lastFailures() {
        return lastFailures;
    }

    /** Load (or reload) the entries folder and install the outcome. */
    synchronized RegistryDiff.Diff reload(CommandSender sender) {
        File dir = new File(plugin.getDataFolder(), "entries");
        Map<String, EntryDef> before = registry.entries();
        EntryRegistry.LoadOutcome outcome = registry.loadDirectory(dir.toPath());

        // Material validation needs the live set; a bad icon refuses
        // the entry, exactly like a bad key.
        Set<String> materials = new HashSet<>();
        for (Material m : Material.values()) materials.add(m.name());
        List<String> materialErrors = EntryRegistry.validateMaterials(
                outcome.loaded(), materials);
        Map<String, EntryDef> loaded = new java.util.LinkedHashMap<>(outcome.loaded());
        Map<String, List<String>> failed =
                new java.util.TreeMap<>(outcome.failed());
        for (String err : materialErrors) {
            String id = err.contains(":") ? err.substring(0, err.indexOf(':')) : err;
            loaded.remove(id);
            failed.computeIfAbsent(id, k -> new ArrayList<>()).add(err);
        }

        if (loaded.isEmpty() && !before.isEmpty()) {
            plugin.getLogger().severe("ExcelMenu registry reload REJECTED: the new set"
                    + " loaded zero entries; the previous " + before.size() + " stay live.");
            if (sender != null) {
                sender.sendMessage("§cExcelMenu registry reload rejected — new set is"
                        + " empty; previous entries stay live. See console.");
            }
            lastFailures = failed;
            return null;
        }

        registry.installLoaded(loaded);
        lastFailures = failed;

        for (Map.Entry<String, List<String>> e : failed.entrySet()) {
            for (String err : e.getValue()) {
                plugin.getLogger().severe("ExcelMenu entry FAILED [" + e.getKey()
                        + "]: " + err);
            }
        }
        RegistryDiff.Diff diff = RegistryDiff.between(before, loaded);
        plugin.getLogger().info("ExcelMenu registry: " + loaded.size() + " entries live"
                + (failed.isEmpty() ? "" : ", " + failed.size() + " file(s) FAILED")
                + " — " + diff.summary());
        if (sender != null) {
            sender.sendMessage("§aExcelMenu registry reloaded: " + loaded.size()
                    + " entries, " + failed.size() + " failed. " + diff.summary());
        }
        return diff;
    }

    // ---------- ExcelMenuApi ----------

    @Override
    public String registerEntry(EntryDef def) {
        return registry.registerApi(def);
    }

    @Override
    public boolean unregisterEntry(String id) {
        return registry.unregisterApi(id);
    }

    @Override
    public EntryDef entry(String id) {
        return registry.entry(id);
    }

    @Override
    public Map<String, EntryDef> entries() {
        return registry.entries();
    }

    @Override
    public List<ResolvedEntry> entriesFor(String menuId, ViewerContext viewer) {
        return EntryResolver.resolve(menuId, registry.entries().values(), viewer,
                Clock.systemUTC(), config);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
