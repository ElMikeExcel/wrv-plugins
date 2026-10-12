package com.winkreyes.playermap;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import io.papermc.paper.event.player.CartographyItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class PlayerMapPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private record Position(Player player, UUID world, String worldName, double x, double z, float yaw) {}
    private NamespacedKey ownerKey;
    private MapRegistry registry;
    private final Map<Integer, PlayerMapRenderer> renderers = new HashMap<>();
    private final Map<UUID, Long> replacements = new HashMap<>();
    private final Set<UUID> warned = new HashSet<>();
    private List<Position> positions = List.of();
    private long revision;
    private int defaultScale;
    private long cooldownNanos;
    @Override public void onEnable() {
        saveDefaultConfig();
        defaultScale = MapGeometry.clamp(getConfig().getInt("default-scale", 2), 0, 4);
        cooldownNanos = Math.max(10L, Math.min(3600L, getConfig().getLong("replacement-cooldown-seconds", 60))) * 1_000_000_000L;
        ownerKey = new NamespacedKey(this, "owner");
        registry = new MapRegistry(getServer(), getDataFolder().toPath().resolve("maps.yml"));
        try { registry.load(); }
        catch (Exception error) { getLogger().log(Level.SEVERE, "Cannot safely load map registry; disabling", error); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("playermap")).setExecutor(this);
        Objects.requireNonNull(getCommand("playermap")).setTabCompleter(this);
        for (MapRegistry.Entry entry : registry.entries()) {
            MapView view = getServer().getMap(entry.id()); if (view != null) attach(view);
        }
        long interval = Math.max(10, Math.min(200, getConfig().getLong("update-interval-ticks", 20)));
        getServer().getScheduler().runTaskTimer(this, this::refresh, 1, interval);
        getLogger().info("Player-map prototype enabled: native terrain plus pixel labels; no live-client verification implied.");
    }
    @Override public void onDisable() {
        for (var entry : renderers.entrySet()) {
            MapView view = getServer().getMap(entry.getKey());
            if (view != null) view.removeRenderer(entry.getValue());
            entry.getValue().clearCache();
        }
        renderers.clear(); positions = List.of(); replacements.clear(); warned.clear();
    }
    long revision() { return revision; }
    void attach(MapView map) {
        if (!registry.matches(map) || renderers.containsKey(map.getId())) return;
        PlayerMapRenderer renderer = new PlayerMapRenderer(this);
        map.addRenderer(renderer); // Keep the native terrain renderer and normal exploration behavior.
        renderers.put(map.getId(), renderer);
    }
    @EventHandler public void onMapInitialize(MapInitializeEvent event) { if (registry != null) attach(event.getMap()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        positions = positions.stream().filter(p -> !p.player.getUniqueId().equals(id)).toList();
        warned.remove(id); renderers.values().forEach(renderer -> renderer.forget(id)); revision++;
        // Cooldown persists through reconnects in this process to prevent reconnect spam.
    }
    @EventHandler public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) if (isManaged(item)) { event.getInventory().setResult(null); return; }
    }
    @EventHandler public void onCartography(CartographyItemEvent event) {
        if (isManaged(event.getInventory().getItem(0)) || isManaged(event.getInventory().getItem(1))) event.setCancelled(true);
    }
    private void refresh() {
        List<Position> fresh = new ArrayList<>();
        for (Player player : getServer().getOnlinePlayers()) {
            Location location = player.getLocation();
            fresh.add(new Position(player, player.getWorld().getUID(), player.getWorld().getName(), location.getX(), location.getZ(), location.getYaw()));
            if (player.hasPermission("wrv.playermap.use")) {
                updateHeldWorld(player, player.getInventory().getItemInMainHand());
                updateHeldWorld(player, player.getInventory().getItemInOffHand());
            }
        }
        positions = List.copyOf(fresh); revision++;
    }
    private void updateHeldWorld(Player player, ItemStack item) {
        if (!owned(item, player) || !(item.getItemMeta() instanceof MapMeta meta)) return;
        MapView map = meta.getMapView();
        if (map == null || !registry.matches(map)) return;
        if (map.getWorld() != null && map.getWorld().getUID().equals(player.getWorld().getUID())) { attach(map); return; }
        try { setMap(item, player, mapFor(player, map.getScale().getValue())); warned.remove(player.getUniqueId()); }
        catch (IOException error) { if (warned.add(player.getUniqueId())) { player.sendMessage("Player map could not switch worlds. Tell the server owner; your item is unchanged."); getLogger().log(Level.WARNING, "Could not switch a player map", error); } }
    }
    boolean isManaged(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        return meta.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }
    private boolean owned(ItemStack item, Player player) {
        return isManaged(item) && player.getUniqueId().toString().equals(item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
    }
    private boolean matchesHeld(Player player, ItemStack item, int id) {
        if (!owned(item, player)) return false;
        MapView map = ((MapMeta) item.getItemMeta()).getMapView(); return map != null && map.getId() == id && registry.matches(map);
    }
    boolean isHeld(Player player, int id) {
        return matchesHeld(player, player.getInventory().getItemInMainHand(), id) || matchesHeld(player, player.getInventory().getItemInOffHand(), id);
    }
    private MapView mapFor(Player player, int scale) throws IOException {
        Location location = player.getLocation();
        MapView map = registry.getOrCreate(player.getUniqueId(), player.getWorld(), scale, location.getX(), location.getZ()); attach(map); return map;
    }
    private void setMap(ItemStack item, Player owner, MapView map) {
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(map); meta.setDisplayName("WRV Player Map");
        meta.setLore(List.of("Hold in your main hand on phones.", "* = you; names and edge arrows update live.", "North is up. Zoom " + map.getScale().getValue() + "; /playermap scale 0-4", "Full names and worlds: /playermap list"));
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString()); item.setItemMeta(meta);
    }
    PixelFrame overlay(MapView view, Player viewer) {
        if (view.getWorld() == null || !viewer.getWorld().getUID().equals(view.getWorld().getUID())) return new PixelFrame();
        List<MapOverlay.PlayerDot> dots = new ArrayList<>(); int elsewhere = 0;
        for (Position p : positions) {
            if (!p.player.isOnline() || (!p.player.equals(viewer) && (!p.player.hasPermission("wrv.playermap.visible") || !viewer.canSee(p.player)))) continue;
            if (!p.world.equals(view.getWorld().getUID())) { elsewhere++; continue; }
            dots.add(new MapOverlay.PlayerDot(p.player.getUniqueId(), p.player.getName(), p.x, p.z, p.yaw, p.player.equals(viewer)));
        }
        String footer = elsewhere > 0 ? "Other worlds: " + elsewhere : "";
        if (!viewer.getWorld().getUID().equals(view.getWorld().getUID())) footer = "Map: " + view.getWorld().getName();
        return MapOverlay.render(dots, view.getCenterX(), view.getCenterZ(), view.getScale().getValue(), footer, revision).frame();
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Run /playermap in game."); return true; }
        if (!player.hasPermission("wrv.playermap.use")) { player.sendMessage("You do not have permission to use player maps."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            for (Player other : getServer().getOnlinePlayers()) if (other.equals(player) || (other.hasPermission("wrv.playermap.visible") && player.canSee(other)))
                player.sendMessage(other.getName() + " - " + other.getWorld().getName() + (other.equals(player) ? " (you)" : ""));
            return true;
        }
        Integer requestedScale = null;
        if (args.length == 2 && args[0].equalsIgnoreCase("scale")) {
            try { requestedScale = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
            if (requestedScale == null || requestedScale < 0 || requestedScale > 4) { player.sendMessage("Use /playermap scale 0-4."); return true; }
        } else if (args.length > 0 && !(args.length == 1 && args[0].equalsIgnoreCase("get"))) { player.sendMessage("Use /playermap [get|scale 0-4|list]."); return true; }
        giveOrUpdate(player, requestedScale); return true;
    }
    void giveOrUpdate(Player player, Integer requestedScale) {
        ItemStack existing = null; int existingSlot = -1;
        ItemStack[] contents = player.getInventory().getContents();
        int preferred = player.getInventory().getHeldItemSlot();
        if (preferred < contents.length && owned(contents[preferred], player)) existingSlot = preferred;
        else if (contents.length > 40 && owned(contents[40], player)) existingSlot = 40;
        else for (int slot = 0; slot < contents.length; slot++) if (owned(contents[slot], player)) { existingSlot = slot; break; }
        if (existingSlot >= 0) existing = contents[existingSlot];
        if (existing == null && owned(player.getItemOnCursor(), player)) { player.sendMessage("Put your player map into your inventory first."); return; }
        if (existing == null && player.getInventory().firstEmpty() < 0) { player.sendMessage("Make one inventory slot free, then run /playermap."); return; }
        long now = System.nanoTime(); Long last = replacements.get(player.getUniqueId());
        if (existing == null && last != null && now - last < cooldownNanos) { player.sendMessage("You recently received a map. Check your inventory or wait a minute before replacing it."); return; }
        int scale = requestedScale == null ? defaultScale : requestedScale;
        if (requestedScale == null && existing != null && existing.getItemMeta() instanceof MapMeta meta && meta.getMapView() != null)
            scale = meta.getMapView().getScale().getValue();
        try {
            MapView map = mapFor(player, scale);
            if (existing != null) {
                setMap(existing, player, map); player.getInventory().setItem(existingSlot, existing);
                player.sendMessage("Your player map is already in your inventory. Hold it in your main hand on phones. Zoom " + scale + ".");
            } else {
                ItemStack item = new ItemStack(Material.FILLED_MAP); setMap(item, player, map);
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                if (!leftovers.isEmpty()) { player.sendMessage("No map was dropped. Free a slot and try again."); return; }
                replacements.put(player.getUniqueId(), now);
                player.sendMessage("Player map added. Hold it in your main hand on phones. /playermap scale 0-4 changes zoom.");
            }
            revision++;
        } catch (IOException error) { player.sendMessage("Your map could not be loaded safely. Tell the server owner; no replacement was issued."); getLogger().log(Level.WARNING, "Player map creation failed", error); }
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("wrv.playermap.use")) return List.of();
        List<String> options = args.length == 1 ? List.of("get", "scale", "list") : args.length == 2 && args[0].equalsIgnoreCase("scale") ? List.of("0", "1", "2", "3", "4") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT); return options.stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
