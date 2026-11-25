package excelemfixes.dropguide;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.GeyserForms;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The drop-guide module (D185): /drops opens Places -> Mobs -> the
 * mob's full drop list. Data comes from the generated drops-index.yml
 * (bundled; a copy in the plugin data folder overrides it so the
 * index can be refreshed without a rebuild — see README).
 */
public final class DropGuideModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private DropGuideMenus menus;
    private int javaPageSize = 45;
    private int bedrockPageSize = 40;

    @Override
    public String id() {
        return "drop-guide";
    }

    @Override
    public String displayName() {
        return "Drop Guide";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        if (!loadIndex()) {
            throw new IllegalStateException("drop index failed to load");
        }
        ctx.renderer().registerHandler("drops", this::handleAction);
        ctx.registerCommand("drops", List.of(), "/drops [reload]", this);
        DropIndex idx = menus.index();
        ctx.logger().info("Drop Guide ready — " + idx.places().size()
                + " places, " + idx.mobCount() + " mobs, "
                + idx.dropCount() + " drops.");
    }

    @Override
    public void onDisable() {
        if (ctx != null) ctx.renderer().unregisterHandler("drops");
    }

    @Override
    public void reload() {
        loadSettings();
        loadIndex();
    }

    private void loadSettings() {
        if (ctx.moduleConfig() != null) {
            javaPageSize = Math.max(9, Math.min(45,
                    ctx.moduleConfig().getInt("page-size-java", 45)));
            bedrockPageSize = Math.max(10, Math.min(100,
                    ctx.moduleConfig().getInt("page-size-bedrock", 40)));
        }
    }

    private boolean loadIndex() {
        File external = new File(ctx.plugin().getDataFolder(),
                "drops-index.yml");
        try {
            DropIndex idx;
            if (external.isFile()) {
                try (InputStream in = new FileInputStream(external)) {
                    idx = DropIndex.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
                ctx.logger().info("Drop Guide: data-folder drops-index.yml in use");
            } else {
                try (InputStream in =
                             ctx.plugin().getResource("drops-index.yml")) {
                    if (in == null) {
                        ctx.logger().severe("Drop Guide: bundled drops-index.yml missing");
                        return false;
                    }
                    idx = DropIndex.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
            }
            menus = new DropGuideMenus(idx);
            scanEntryPoints(idx);
            return true;
        } catch (Exception e) {
            ctx.logger().severe("Drop Guide: index load failed: " + e);
            return false;
        }
    }

    /** Glow guidance: resolve entry points from EliteMobs' own
     *  data (boss spawns -> content packages -> wormholes /
     *  teleporter NPCs) and tell the menus which mobs can glow. */
    private void scanEntryPoints(DropIndex idx) {
        org.bukkit.plugin.Plugin em =
                org.bukkit.Bukkit.getPluginManager().getPlugin("EliteMobs");
        entryPoints = em == null ? EntryPoints.empty()
                : EntryPoints.scan(em.getDataFolder(), ctx.logger());
        java.util.Set<String> glow = new java.util.HashSet<>();
        if (excelemfixes.api.GlowApi.isAvailable()) {
            for (DropIndex.Place place : idx.places()) {
                for (DropIndex.Mob mob : place.mobs()) {
                    if (entryPoints.hasEntry(mob.id())) glow.add(mob.id());
                }
            }
        }
        menus.setGlowMobIds(glow);
    }

    /** The runtime entry-point resolver (rig + companion access). */
    public EntryPoints entryPoints() {
        return entryPoints;
    }

    private EntryPoints entryPoints = EntryPoints.empty();

    private void fireGlow(Player player, EntryPoints.Entry entry) {
        if (entry == null) return;
        if (!excelemfixes.api.GlowApi.isAvailable()) {
            player.sendMessage("§7Glow guidance is not available right now.");
            return;
        }
        player.closeInventory();
        if (entry.entityGlow()) {
            excelemfixes.api.GlowApi.glowEntity(player, entry.world(),
                    entry.x(), entry.y(), entry.z(), entry.entityName());
        } else {
            excelemfixes.api.GlowApi.glowMarker(player, entry.world(),
                    entry.x(), entry.y(), entry.z(), entry.label());
        }
        player.sendMessage("§eGlowing the way in: §f" + entry.label()
                + " §7(" + entry.world() + " " + entry.coords() + ")");
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("excelemfixes.admin")) {
                sender.sendMessage("§cYou do not have permission to reload the drop guide.");
                return true;
            }
            if (loadIndex()) {
                DropIndex idx = menus.index();
                sender.sendMessage("§aDrop guide reloaded: " + idx.places().size()
                        + " places, " + idx.mobCount() + " mobs, "
                        + idx.dropCount() + " drops.");
            } else {
                sender.sendMessage("§cDrop guide reload failed — see console.");
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The drop guide menu is for players.");
            return true;
        }
        ctx.renderer().openRoot(player, menus.placesView(0, pageSize(player)));
        return true;
    }

    // ---------------------------------------------------------- navigation

    /** Actions: drops:places:<page> | drops:place:<id>:<page> |
     *  drops:mob:<place>:<mob>:<page>. */
    private void handleAction(Player player, String action) {
        String[] parts = action.split(":");
        if (parts.length < 2) return;
        switch (parts[1]) {
            case "places" -> ctx.renderer().open(player,
                    menus.placesView(parseInt(parts, 2), pageSize(player)));
            case "place" -> ctx.renderer().open(player,
                    menus.mobsView(parts[2], parseInt(parts, 3),
                            pageSize(player)));
            case "mob" -> ctx.renderer().open(player,
                    menus.dropsView(parts[2], parts[3], parseInt(parts, 4),
                            pageSize(player)));
            case "glowmob" -> fireGlow(player,
                    entryPoints.entryForMob(parts[3]));
            case "glowplace" -> {
                DropIndex.Place place = menus.index().placeById(parts[2]);
                EntryPoints.Entry entry = null;
                if (place != null) {
                    for (DropIndex.Mob mob : place.mobs()) {
                        entry = entryPoints.entryForMob(mob.id());
                        if (entry != null) break;
                    }
                }
                fireGlow(player, entry);
            }
            default -> { /* unknown sub-action: ignore */ }
        }
    }

    private int pageSize(Player player) {
        return GeyserForms.isBedrockPlayer(player, ctx.logger())
                ? bedrockPageSize : javaPageSize;
    }

    private static int parseInt(String[] parts, int i) {
        if (i >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Test/rig hook. */
    public DropGuideMenus menus() {
        return menus;
    }
}
