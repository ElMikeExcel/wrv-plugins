package excelemfixes.seal;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Seal Restore module: when a boss whose arena seals itself
 * during the fight dies, put the sealed blocks back.
 *
 * <p>The defect: EliteMobs content powers can place blocks when
 * a boss spawns (sealing arena doors) and are supposed to
 * restore them from the boss's onRemove list — but content
 * exists whose restore list is shorter than its seal list (on
 * the reference server, one boss sealed two doorways and only
 * ever restored one, leaving the second doorway walled up
 * forever after the fight). This module is the generic fix:
 * data rows name a boss file and the blocks its arena should
 * end with; on that boss's death the module restores any block
 * still wearing its seal. Data-driven only — no boss, world or
 * block is hardcoded, so any owner's broken content can be
 * covered by adding a row.
 */
public final class SealRestoreModule implements FixModule, Listener {

    private ModuleContext ctx;
    private SealRestoreData data = SealRestoreData.parse(null);
    private boolean debug;
    private final Set<UUID> recentlyRestored = new HashSet<>();

    @Override
    public String id() {
        return "seal-restore";
    }

    @Override
    public String displayName() {
        return "Seal Restore";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        load();
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Seal Restore ready: " + data.bosses().size() + " boss row(s), "
                + data.totalBlocks() + " block(s) watched.");
        if (data.totalBlocks() == 0) {
            ctx.logger().info("Seal Restore: seal-restore.yml has no rows — add rows for any boss "
                    + "whose arena seal is not fully restored by its content.");
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        load();
    }

    private void load() {
        debug = ctx.moduleConfig() != null && ctx.moduleConfig().getBoolean("debug", false);
        File file = new File(ctx.plugin().getDataFolder(), "seal-restore.yml");
        if (file.isFile()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            data = SealRestoreData.parse(yaml.getList("restores"));
            return;
        }
        // Bundled template (empty until the owner adds rows).
        try (java.io.InputStream in = ctx.plugin().getResource("seal-restore.yml")) {
            if (in == null) {
                data = SealRestoreData.parse(null);
                return;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            data = SealRestoreData.parse(yaml.getList("restores"));
        } catch (Exception e) {
            ctx.logger().warning("Seal Restore: could not read bundled seal-restore.yml: " + e);
            data = SealRestoreData.parse(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEliteDeath(EliteMobDeathEvent event) {
        String filename = null;
        try {
            if (event.getEliteEntity() instanceof RegionalBossEntity regional) {
                filename = regional.getCustomBossesConfigFields().getFilename();
            }
        } catch (Throwable ignored) {
            // EM internals shifted: no filename, no restore.
        }
        if (filename == null) return;
        java.util.List<SealRestoreData.BlockEntry> blocks = data.blocksFor(filename);
        if (blocks.isEmpty()) return;
        UUID entityId = event.getEntity() == null ? null : event.getEntity().getUniqueId();
        if (entityId != null && !recentlyRestored.add(entityId)) return; // duplicate event guard
        if (entityId != null) {
            Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> recentlyRestored.remove(entityId), 100L);
        }
        int restored = 0;
        for (SealRestoreData.BlockEntry entry : blocks) {
            World world = Bukkit.getWorld(entry.world());
            if (world == null) continue;
            Block block = world.getBlockAt(entry.x(), entry.y(), entry.z());
            String current = block.getBlockData().getAsString();
            if (!SealRestoreData.shouldRestore(current, entry)) continue;
            try {
                BlockData target = Bukkit.createBlockData(entry.restore());
                block.setBlockData(target, false);
                restored++;
            } catch (IllegalArgumentException badData) {
                ctx.logger().warning("Seal Restore: bad blockdata '" + entry.restore()
                        + "' in seal-restore.yml for boss " + filename + " — row skipped for that block.");
            }
        }
        if (restored > 0 || debug) {
            ctx.logger().info("Seal Restore: boss " + filename + " died — restored " + restored
                    + " of " + blocks.size() + " registered block(s).");
        }
    }
}
