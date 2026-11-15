// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.registry.PlayerMenuPrefs;
import com.winkreyes.excelmenu.core.registry.PrefsCodec;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The per-player rearrange store (ExcelMenu 3.0, DESIGN §3.8):
 * {@code playerdata/<uuid>.yml}, one file per player, lazy-loaded,
 * cached, saved on every change via temp file + atomic rename.
 * A corrupt or foreign-version file logs SEVERE once, is treated
 * as NO prefs (the default order serves), is left in place for
 * diagnosis, and is repaired by the editor's next save. All access
 * is main-thread (menu opens and clicks already are).
 */
final class PrefsStore implements Listener {

    private final JavaPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerMenuPrefs> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loaded = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> severeLogged = new ConcurrentHashMap<>();

    PrefsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "playerdata");
    }

    /** The player's prefs, or null when they use the default order. */
    PlayerMenuPrefs get(UUID uuid) {
        if (loaded.containsKey(uuid)) return cache.get(uuid);
        loaded.put(uuid, Boolean.TRUE);
        File f = file(uuid);
        if (!f.isFile()) return null;
        try {
            PrefsCodec.Result r = PrefsCodec.parse(Files.readString(f.toPath()));
            if (!r.ok()) {
                severeOnce(uuid, f, r.error());
                return null;
            }
            if (!PlayerMenuPrefs.MENU.equals(r.prefs().menu())) {
                // v1 serves one menu; a record for another menu is
                // not this menu's prefs (OrderMerge also guards).
                return null;
            }
            cache.put(uuid, r.prefs());
            return r.prefs();
        } catch (IOException e) {
            severeOnce(uuid, f, "cannot read — " + e.getMessage());
            return null;
        }
    }

    boolean hasPrefs(UUID uuid) {
        return get(uuid) != null;
    }

    /** Save-on-change: temp file + atomic rename in the same folder. */
    void save(UUID uuid, PlayerMenuPrefs prefs) {
        cache.put(uuid, prefs);
        loaded.put(uuid, Boolean.TRUE);
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                plugin.getLogger().severe("ExcelMenu: cannot create " + dir
                        + " for menu prefs");
                return;
            }
            File tmp = new File(dir, uuid + ".yml.tmp");
            Files.writeString(tmp.toPath(), PrefsCodec.toYaml(prefs));
            Files.move(tmp.toPath(), file(uuid).toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("ExcelMenu: cannot save menu prefs for "
                    + uuid + " — " + e.getMessage());
        }
    }

    /** Reset (DESIGN §3.6): delete the record; the default serves. */
    void delete(UUID uuid) {
        cache.remove(uuid);
        loaded.put(uuid, Boolean.TRUE);
        try {
            Files.deleteIfExists(file(uuid).toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("ExcelMenu: cannot delete menu prefs for "
                    + uuid + " — " + e.getMessage());
        }
    }

    void invalidate(UUID uuid) {
        cache.remove(uuid);
        loaded.remove(uuid);
    }

    private File file(UUID uuid) {
        return new File(dir, uuid + ".yml");
    }

    private void severeOnce(UUID uuid, File f, String error) {
        if (severeLogged.putIfAbsent(uuid, Boolean.TRUE) == null) {
            plugin.getLogger().severe("ExcelMenu: menu prefs file " + f.getPath()
                    + " is unusable (" + error + ") — serving the default order;"
                    + " the file is left in place and the editor's next save repairs it.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidate(event.getPlayer().getUniqueId());
        severeLogged.remove(event.getPlayer().getUniqueId());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
