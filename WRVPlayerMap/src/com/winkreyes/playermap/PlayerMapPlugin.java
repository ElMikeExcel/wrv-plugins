package com.winkreyes.playermap;

import io.papermc.paper.event.player.PlayerMapFilledEvent;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** A native map enhancement, not an alternative map item or terrain implementation. */
public final class PlayerMapPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private record Position(Player player, UUID world, String worldName, double x, double z, float yaw) {}
    private record Activation(int id, UUID world, int x, int z, int scale) {
        boolean same(MapView view) { return view.getId() == id && view.getWorld() != null && view.getWorld().getUID().equals(world)
                && view.getCenterX() == x && view.getCenterZ() == z && view.getScale().getValue() == scale; }
    }
    private NamespacedKey rootKey;
    private MapRegistry registry;
    private final Map<Integer, PlayerMapRenderer> renderers = new HashMap<>();
    private final Map<PlayerMapFilledEvent, Activation> activations = new WeakHashMap<>();
    private final Set<UUID> warned = new HashSet<>();
    private List<Position> positions = List.of();
    private long revision;
    @Override public void onEnable() {
        var manager = getServer().getPluginManager();
        var legacyPlugin = manager.getPlugin("WRVPlayerMap");
        var installedPlugins = manager.getPlugins();
        boolean legacyInstalled = legacyPlugin != null && legacyPlugin != this;
        if (installedPlugins != null) legacyInstalled |= Arrays.stream(installedPlugins)
                .anyMatch(plugin -> plugin != null && plugin != this && "WRVPlayerMap".equalsIgnoreCase(plugin.getName()));
        if (legacyInstalled) {
            getLogger().severe("WRVPlayerMap is also installed. Remove its old JAR before enabling ExcelPlayerMap; keep both data folders and all world map data.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        try {
            if (LegacyStorage.prepare(getDataFolder().toPath()) == LegacyStorage.Result.MIGRATED)
                getLogger().info("Copied WRVPlayerMap storage to ExcelPlayerMap; the legacy folder remains untouched as a backup.");
            saveDefaultConfig();
        } catch (Exception error) {
            getLogger().log(Level.SEVERE,"Cannot safely prepare ExcelPlayerMap storage; disabling without replacing existing data",error);
            getServer().getPluginManager().disablePlugin(this); return;
        }
        // Keep the deployed item tag stable across plugin renames, copies and native zooms.
        rootKey = new NamespacedKey("wrvplayermap","lineage_root");
        registry = new MapRegistry(getDataFolder().toPath().resolve("maps.yml"));
        try { registry.load(); }
        catch (Exception error) { getLogger().log(Level.SEVERE,"Cannot safely load map registry; disabling",error); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("playermap")).setExecutor(this); Objects.requireNonNull(getCommand("playermap")).setTabCompleter(this);
        long interval = Math.max(10,Math.min(200,getConfig().getLong("update-interval-ticks",20)));
        getServer().getScheduler().runTaskTimer(this,this::refresh,1,interval);
        getLogger().info("Native player-map enhancement enabled. Only newly activated ordinary maps and their unlocked zoom descendants are enrolled.");
    }
    @Override public void onDisable() {
        for (var entry : renderers.entrySet()) {
            MapView view = getServer().getMap(entry.getKey()); if (view != null) view.removeRenderer(entry.getValue()); entry.getValue().clearCache();
        }
        renderers.clear(); activations.clear(); positions = List.of(); warned.clear();
    }
    long revision() { return revision; }
    void attach(MapView map) {
        if (!registry.matches(map) || renderers.containsKey(map.getId())) return;
        PlayerMapRenderer renderer = new PlayerMapRenderer(this); map.addRenderer(renderer); renderers.put(map.getId(),renderer);
    }
    // Used ONLY to restore known IDs. This event also fires for old maps loaded from disk.
    @EventHandler public void onMapInitialize(MapInitializeEvent event) { if (registry != null) attach(event.getMap()); }
    @EventHandler(priority = EventPriority.LOWEST) public void captureActivation(PlayerMapFilledEvent event) {
        ItemStack created = event.getCreatedMap(); MapView view = view(created);
        if (event.getOriginalItem().getType() != Material.MAP || !plainOutput(created) || view == null || view.getWorld() == null || view.isLocked()
                || view.isVirtual() || view.getScale() != MapView.Scale.CLOSEST || view.getRenderers().size() != 1) return;
        activations.put(event,new Activation(view.getId(),view.getWorld().getUID(),view.getCenterX(),view.getCenterZ(),view.getScale().getValue()));
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void enhanceActivation(PlayerMapFilledEvent event) {
        Activation captured = activations.remove(event); ItemStack item = event.getCreatedMap(); MapView map = view(item);
        if (captured == null || !plainOutput(item) || map == null || !captured.same(map) || map.isLocked() || map.isVirtual() || map.getRenderers().size() != 1
                || !event.getPlayer().hasPermission("wrv.playermap.use")) return;
        try {
            registry.trackInitial(map);
            MapMeta meta = (MapMeta) item.getItemMeta(); meta.getPersistentDataContainer().set(rootKey,PersistentDataType.INTEGER,map.getId());
            item.setItemMeta(meta); event.setCreatedMap(item); attach(map); revision++;
        } catch (IOException error) { report(event.getPlayer(),error); } // Vanilla map remains intact and usable.
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId(); positions = positions.stream().filter(p -> !p.player.getUniqueId().equals(id)).toList();
        warned.remove(id); renderers.values().forEach(renderer -> renderer.forget(id)); revision++;
    }
    private void refresh() {
        List<Position> fresh = new ArrayList<>();
        for (Player player : getServer().getOnlinePlayers()) {
            Location location = player.getLocation();
            fresh.add(new Position(player,player.getWorld().getUID(),player.getWorld().getName(),location.getX(),location.getZ(),location.getYaw()));
            if (player.hasPermission("wrv.playermap.use")) {
                ensureTracked(player,player.getInventory().getItemInMainHand()); ensureTracked(player,player.getInventory().getItemInOffHand());
            }
        }
        positions = List.copyOf(fresh); revision++;
    }
    /** Vanilla clone/zoom recipes copy the lineage tag; native post-processing replaces only the zoom ID. */
    boolean ensureTracked(Player holder, ItemStack item) {
        MapView map = view(item); if (map == null || map.isLocked()) return false;
        Integer root = item.getItemMeta().getPersistentDataContainer().get(rootKey,PersistentDataType.INTEGER);
        if (root == null) return false;
        MapRegistry.Entry entry = registry.entry(map.getId());
        if (entry != null && entry.root() != root) return false;
        if (registry.matches(map)) { attach(map); return true; }
        if (map.getRenderers().size() != 1 || map.isVirtual()) return false;
        try { if (!registry.trackZoom(map,root)) return false; attach(map); revision++; return true; }
        catch (IOException error) { report(holder,error); return false; }
    }
    private void report(Player player, IOException error) {
        if (warned.add(player.getUniqueId())) {
            player.sendMessage("Player labels could not be saved. Your ordinary map still works; tell the server owner.");
            getLogger().log(Level.WARNING,"Could not register native player-map labels",error);
        }
    }
    private static MapView view(ItemStack item) {
        return item != null && item.getType() == Material.FILLED_MAP && item.getItemMeta() instanceof MapMeta meta ? meta.getMapView() : null;
    }
    private static boolean plainOutput(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        return !meta.hasDisplayName() && !meta.hasLore() && !meta.hasColor() && !meta.hasLocationName() && meta.getPersistentDataContainer().getKeys().isEmpty();
    }
    private boolean matchesHeld(ItemStack item, int id) {
        MapView map = view(item); if (map == null || map.getId() != id || !registry.matches(map)) return false;
        Integer root = item.getItemMeta().getPersistentDataContainer().get(rootKey,PersistentDataType.INTEGER);
        MapRegistry.Entry entry = registry.entry(id); return root != null && entry != null && entry.root() == root;
    }
    boolean isHeld(Player player, int id) {
        return matchesHeld(player.getInventory().getItemInMainHand(),id) || matchesHeld(player.getInventory().getItemInOffHand(),id);
    }
    PixelFrame overlay(MapView view, Player viewer) {
        if (!registry.matches(view) || view.getWorld() == null || !viewer.getWorld().getUID().equals(view.getWorld().getUID())) return new PixelFrame();
        List<MapOverlay.PlayerDot> dots = new ArrayList<>(); int elsewhere = 0;
        for (Position p : positions) {
            if (!p.player.isOnline() || (!p.player.equals(viewer) && (!p.player.hasPermission("wrv.playermap.visible") || !viewer.canSee(p.player)))) continue;
            if (!p.world.equals(view.getWorld().getUID())) { elsewhere++; continue; }
            dots.add(new MapOverlay.PlayerDot(p.player.getUniqueId(),p.player.getName(),p.x,p.z,p.yaw,p.player.equals(viewer)));
        }
        return MapOverlay.render(dots,view.getCenterX(),view.getCenterZ(),view.getScale().getValue(),elsewhere > 0 ? "Other worlds: " + elsewhere : "",revision).frame();
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players activate ordinary empty maps in game. /playermap list lists visible players."); return true; }
        if (!player.hasPermission("wrv.playermap.use")) { player.sendMessage("You do not have permission to use player labels."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            for (Player other : getServer().getOnlinePlayers()) if (other.equals(player) || (other.hasPermission("wrv.playermap.visible") && player.canSee(other)))
                player.sendMessage(other.getName() + " - " + other.getWorld().getName() + (other.equals(player) ? " (you)" : ""));
        } else player.sendMessage("Use an ordinary empty map to add player labels. Hold it in your main hand on phones. Zoom/copy it normally at a cartography table; lock with glass for a plain map. /playermap list lists players.");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return sender.hasPermission("wrv.playermap.use") && args.length == 1 && "list".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("list") : List.of();
    }
}
