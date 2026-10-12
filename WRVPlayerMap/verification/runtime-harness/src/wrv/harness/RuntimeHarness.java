package wrv.harness;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Disposable runtime verification only. Never ship this harness to a live server. */
public final class RuntimeHarness extends JavaPlugin {
    private final List<String> results = new ArrayList<>();
    private static int assertions;
    private int passed;
    private int failed;
    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, this::run, 20L);
    }
    private void run() {
        test("exact-server-version", () -> {
            check(Bukkit.getVersion().equals("26.2-123-5001879 (MC: 26.2)"), Bukkit.getVersion());
            check(Bukkit.getBukkitVersion().equals("26.2.build.123-stable"), Bukkit.getBukkitVersion());
        });
        test("native-map-layer-and-transparent-pixels", this::nativeMapAndCanvas);
        test("plugin-enabled", () -> {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("WRVPlayerMap");
            check(plugin != null && plugin.isEnabled(), "WRVPlayerMap enabled");
        });
        pluginTests();
        try {
            Path folder = getDataFolder().toPath(); Files.createDirectories(folder);
            String report = String.join("\n", results) + "\nSUMMARY passed=" + passed + " failed=" + failed + " assertions=" + assertions + "\n";
            Files.writeString(folder.resolve("runtime-results.txt"), report);
            getLogger().info("WRV_HARNESS_SUMMARY passed=" + passed + " failed=" + failed + " assertions=" + assertions);
        } catch (Exception exception) { getLogger().severe(exception.toString()); }
        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 20L);
    }
    private void nativeMapAndCanvas() throws Exception {
        World world = Bukkit.getWorlds().getFirst();
        MapView map = Bukkit.createMap(world);
        check(map.getRenderers().size() == 1, "new maps have one native renderer");
        MapRenderer nativeRenderer = map.getRenderers().getFirst();
        check(nativeRenderer.getClass().getName().endsWith("CraftMapRenderer"), nativeRenderer.getClass().getName());
        MapRenderer overlay = new MapRenderer(true) { @Override public void render(MapView view, MapCanvas canvas, Player player) {} };
        map.addRenderer(overlay);
        check(map.getRenderers().size() == 2 && map.getRenderers().getFirst() == nativeRenderer, "adding contextual layer preserves native renderer and order");
        check(!map.isVirtual(), "native terrain map remains non-virtual");
        Class<?> canvasType = Class.forName("org.bukkit.craftbukkit.map.CraftMapCanvas");
        Constructor<?> constructor = canvasType.getDeclaredConstructor(map.getClass());
        constructor.setAccessible(true);
        MapCanvas canvas = (MapCanvas) constructor.newInstance(map);
        byte[] terrain = new byte[128 * 128]; Arrays.fill(terrain, (byte) 5);
        Method setBase = canvasType.getDeclaredMethod("setBase", byte[].class); setBase.setAccessible(true); setBase.invoke(canvas, (Object) terrain);
        check(canvas.getPixelColor(64, 64) == null, "fresh overlay pixels are null/transparent");
        Color base = canvas.getBasePixelColor(64, 64);
        check(base != null, "native terrain base color exists");
        canvas.setPixelColor(64, 64, Color.WHITE);
        check(canvas.getPixelColor(64, 64) != null, "overlay writes an opaque pixel");
        canvas.setPixelColor(64, 64, null);
        check(canvas.getPixelColor(64, 64) == null, "setting null clears overlay pixel");
        check(base.equals(canvas.getBasePixelColor(64, 64)), "native terrain base is unchanged after overlay clear");
        map.removeRenderer(overlay);
        check(map.getRenderers().size() == 1 && map.getRenderers().getFirst() == nativeRenderer, "removing overlay preserves terrain renderer");
    }
    private void pluginTests() {
        test("managed-map-create-reuse-and-per-world-scale", this::registryTests);
        test("managed-map-overlay-restored-after-restart", this::restartTests);
        test("prototype-delta-writes-and-native-pixel-restore", this::prototypeCanvasTests);
        test("simulated-item-issuance-hands-permission-and-dimension", this::simulatedPlayerTests);
        test("simulated-visibility-and-disconnected-player-filtering", this::visibilityTests);
    }
    private Plugin prototype() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WRVPlayerMap");
        check(plugin != null && plugin.isEnabled(), "prototype must be enabled");
        return plugin;
    }
    private Object registry() throws Exception { return field(prototype(), "registry"); }
    private MapView mapFor(UUID owner, World world, int scale, double x, double z) throws Exception {
        return (MapView) invoke(registry(), "getOrCreate", new Class<?>[]{UUID.class, World.class, int.class, double.class, double.class}, owner, world, scale, x, z);
    }
    private int registrySize() throws Exception { return ((Collection<?>) invoke(registry(), "entries", new Class<?>[]{})).size(); }
    private void attach(MapView map) throws Exception { invoke(prototype(), "attach", new Class<?>[]{MapView.class}, map); }
    private void checkLayers(MapView map) {
        check(map.getRenderers().size() == 2, "managed map has native and overlay renderers, id=" + map.getId());
        check(map.getRenderers().getFirst().getClass().getName().endsWith("CraftMapRenderer"), "native renderer remains first");
        check(map.getRenderers().get(1).getClass().getName().equals("com.winkreyes.playermap.PlayerMapRenderer"), "prototype overlay remains second");
        check(map.getRenderers().get(1).isContextual(), "overlay is per viewer");
        check(!map.isVirtual(), "managed terrain map is non-virtual");
    }
    private void registryTests() throws Exception {
        World world = Bukkit.getWorlds().getFirst();
        UUID owner = UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900001");
        for (int scale = 0; scale <= 4; scale++) {
            MapView view = mapFor(owner, world, scale, -130.0, 257.0);
            check(view.getScale().getValue() == scale, "requested scale respected");
            int size = registrySize();
            MapView again = mapFor(owner, world, scale, 9000.0, -9000.0);
            check(again.getId() == view.getId() && registrySize() == size, "same owner/world/scale reuses map ID");
            check(!view.isTrackingPosition() && !view.isUnlimitedTracking(), "native cursor tracking disabled for pixel markers");
            attach(view); attach(view); checkLayers(view);
        }
        MapView normal = mapFor(owner, world, 2, 0, 0);
        World otherWorld = Bukkit.getWorlds().stream().filter(w -> !w.getUID().equals(world.getUID())).findFirst().orElseThrow();
        MapView other = mapFor(owner, otherWorld, 2, 0, 0);
        check(normal.getId() != other.getId(), "different world gets separate map ID");
        MapView otherOwner = mapFor(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900002"), world, 2, 0, 0);
        check(normal.getId() != otherOwner.getId(), "different owner gets separate map ID");
        MapView unrelated = Bukkit.createMap(world); List<MapRenderer> original = List.copyOf(unrelated.getRenderers());
        attach(unrelated); check(unrelated.getRenderers().equals(original), "unrelated vanilla map is not modified");
        int originalCenter = normal.getCenterX(); normal.setCenterX(originalCenter + 128);
        try {
            check(!(boolean) invoke(registry(), "matches", new Class<?>[]{MapView.class}, normal), "changed map no longer matches registry");
            try { mapFor(owner, world, 2, 0, 0); throw new AssertionError("tampered map should be refused"); }
            catch (InvocationTargetException exception) { check(exception.getCause() instanceof IOException, "changed map fails safely with IOException"); }
        } finally { normal.setCenterX(originalCenter); }
    }
    private void restartTests() throws Exception {
        Path state = getDataFolder().toPath().resolve("restart-state.properties"); Files.createDirectories(state.getParent());
        UUID owner = UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900003");
        World world = Bukkit.getWorlds().getFirst(); Properties properties = new Properties();
        boolean restart = Files.exists(state);
        if (restart) {
            try (var in = Files.newInputStream(state)) { properties.load(in); }
            int id = Integer.parseInt(properties.getProperty("map-id"));
            MapView loaded = Bukkit.getMap(id);
            check(loaded != null, "persisted map ID restored by server");
            checkLayers(loaded); // Before attach(): proves plugin startup reattached the overlay.
            MapView same = mapFor(owner, world, 3, 9000, -9000);
            check(same.getId() == id, "registry reuses same ID after full server restart");
            check(same.getCenterX() == Integer.parseInt(properties.getProperty("center-x")), "map center preserved across restart");
            getLogger().info("WRV_HARNESS_RESTART verified map=" + id);
        } else {
            MapView created = mapFor(owner, world, 3, 321, -654); attach(created); checkLayers(created);
            properties.setProperty("map-id", Integer.toString(created.getId()));
            properties.setProperty("center-x", Integer.toString(created.getCenterX()));
            properties.setProperty("world", world.getUID().toString());
            try (var out = Files.newOutputStream(state)) { properties.store(out, "Isolated runtime persistence fixture"); }
            getLogger().info("WRV_HARNESS_RESTART prepared map=" + created.getId() + "; restart verification pending");
        }
    }
    private MapCanvas realCanvas(MapView map) throws Exception {
        Class<?> type = Class.forName("org.bukkit.craftbukkit.map.CraftMapCanvas");
        Constructor<?> constructor = type.getDeclaredConstructor(map.getClass()); constructor.setAccessible(true);
        MapCanvas canvas = (MapCanvas) constructor.newInstance(map);
        byte[] terrain = new byte[128 * 128]; Arrays.fill(terrain, (byte) 5);
        Method setBase = type.getDeclaredMethod("setBase", byte[].class); setBase.setAccessible(true); setBase.invoke(canvas, (Object) terrain);
        return canvas;
    }
    private void prototypeCanvasTests() throws Exception {
        MapView map = mapFor(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900001"), Bukkit.getWorlds().getFirst(), 2, 0, 0);
        MapCanvas canvas = realCanvas(map); Color base = canvas.getBasePixelColor(64, 64);
        Class<?> renderer = Class.forName("com.winkreyes.playermap.PlayerMapRenderer", true, prototype().getClass().getClassLoader());
        Method apply = renderer.getDeclaredMethod("apply", MapCanvas.class, byte[].class, byte[].class); apply.setAccessible(true);
        byte[] painted = new byte[128 * 128]; painted[64 * 128 + 64] = 2;
        canvas.setPixelColor(63, 64, Color.RED);
        check((int) apply.invoke(null, canvas, null, painted) == 128 * 128, "initial frame clears all potentially stale pixels");
        check(canvas.getPixelColor(64, 64) != null && canvas.getPixelColor(63, 64) == null, "single opaque marker pixel");
        check((int) apply.invoke(null, canvas, painted, painted.clone()) == 0, "unchanged frame writes zero pixels");
        byte[] clear = new byte[128 * 128];
        check((int) apply.invoke(null, canvas, painted, clear) == 1, "removed marker clears exactly one pixel");
        check(canvas.getPixelColor(64, 64) == null, "prototype actually clears to null");
        check(base.equals(canvas.getBasePixelColor(64, 64)), "original native terrain base restored");
        canvas.setPixelColor(10, 10, Color.RED);
        check((int) apply.invoke(null, canvas, null, clear) == 128 * 128 && canvas.getPixelColor(10, 10) == null, "cache reset fully clears stale canvas pixels");
    }
    private void simulatedPlayerTests() throws Exception {
        Plugin plugin = prototype(); World world = Bukkit.getWorlds().getFirst();
        FakePlayer fake = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900010"), "TestOwner", world);
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, null);
        check(fake.itemCount() == 1, "initial command issues one item");
        ItemStack item = fake.items[0]; check(item != null && item.getType() == Material.FILLED_MAP, "issued item is a filled map");
        int firstId = ((MapMeta) item.getItemMeta()).getMapId();
        check(item.getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(key -> key.toString().equals("wrvplayermap:owner")), "owner PDC tag stored");
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, null);
        check(fake.itemCount() == 1 && ((MapMeta) fake.items[0].getItemMeta()).getMapId() == firstId, "repeated command does not duplicate item/map ID");
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, 4);
        check(fake.itemCount() == 1 && ((MapMeta) item.getItemMeta()).getMapView().getScale().getValue() == 4, "scale change updates same item");
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, 2);
        check(((MapMeta) item.getItemMeta()).getMapId() == firstId, "switching scale back reuses prior ID");
        MapView map = ((MapMeta) item.getItemMeta()).getMapView(); checkLayers(map);
        check((boolean) invoke(plugin, "isHeld", new Class<?>[]{Player.class, int.class}, fake.player, firstId), "main-hand detection");
        fake.items[0] = null; fake.items[40] = item;
        check((boolean) invoke(plugin, "isHeld", new Class<?>[]{Player.class, int.class}, fake.player, firstId), "off-hand detection");
        fake.items[40] = null; fake.items[10] = item;
        check(!(boolean) invoke(plugin, "isHeld", new Class<?>[]{Player.class, int.class}, fake.player, firstId), "stored inventory map is not held");
        fake.items[10] = null; fake.items[0] = item;
        FakePlayer borrower = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900012"), "Borrower", world); borrower.items[0] = item;
        check(!(boolean) invoke(plugin, "isHeld", new Class<?>[]{Player.class, int.class}, borrower.player, firstId), "borrowed item does not grant owner overlay");
        MapCanvas borrowedCanvas = realCanvas(map); map.getRenderers().get(1).render(map, borrowedCanvas, borrower.player);
        check(opaquePixels(borrowedCanvas) == 0 && borrowedCanvas.getBasePixelColor(64, 64) != null, "borrowed map remains native terrain only");
        fake.items[1] = item.clone(); fake.selected = 1;
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, 3);
        check(((MapMeta) fake.items[1].getItemMeta()).getMapView().getScale().getValue() == 3 && ((MapMeta) fake.items[0].getItemMeta()).getMapId() == firstId, "selected held item is preferred over stored duplicate");
        fake.items[1] = null; fake.selected = 0;
        List<?> originalPositions = (List<?>) field(plugin, "positions");
        try {
            setPositions(plugin, fake);
            MapCanvas canvas = realCanvas(map); MapRenderer renderer = map.getRenderers().get(1);
            renderer.render(map, canvas, fake.player); check(opaquePixels(canvas) > 0, "held permitted viewer receives pixel overlay");
            fake.items[0] = null;
            renderer.render(map, canvas, fake.player); check(opaquePixels(canvas) == 0, "no longer holding map clears all prior pixels");
            fake.items[0] = item;
            renderer.render(map, canvas, fake.player); check(opaquePixels(canvas) > 0, "holding again repaints overlay");
            fake.use = false;
            renderer.render(map, canvas, fake.player); check(opaquePixels(canvas) == 0, "permission removal clears all pixels without requiring a new snapshot");
            fake.use = true;
        } finally { setField(plugin, "positions", originalPositions); }
        World otherWorld = Bukkit.getWorlds().stream().filter(w -> !w.getUID().equals(world.getUID())).findFirst().orElseThrow();
        fake.world = otherWorld;
        check(nonzero(overlayPixels(plugin, map, fake.player)) == 0, "cross-world viewer receives no overlay before map switch");
        invoke(plugin, "updateHeldWorld", new Class<?>[]{Player.class, ItemStack.class}, fake.player, item);
        MapView other = ((MapMeta) item.getItemMeta()).getMapView();
        check(other.getId() != firstId && other.getWorld().getUID().equals(otherWorld.getUID()), "held map switches world using separate registered ID");
        fake.world = world;
        invoke(plugin, "updateHeldWorld", new Class<?>[]{Player.class, ItemStack.class}, fake.player, item);
        check(((MapMeta) item.getItemMeta()).getMapId() == firstId, "returning world restores prior map ID");
        fake.items[0] = null; fake.cursor = item;
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, null);
        check(fake.itemCount() == 0, "cursor-held owned map prevents duplicate issuance");
        fake.cursor = null;
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, fake.player, null);
        check(fake.itemCount() == 0, "replacement cooldown prevents rapid duplicate issuance");
        FakePlayer full = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900011"), "FullInventory", world);
        Arrays.fill(full.items, new ItemStack(Material.DIRT)); int before = registrySize();
        invoke(plugin, "giveOrUpdate", new Class<?>[]{Player.class, Integer.class}, full.player, null);
        check(registrySize() == before, "full inventory does not allocate a new map");
    }
    private void visibilityTests() throws Exception {
        Plugin plugin = prototype(); World world = Bukkit.getWorlds().getFirst();
        FakePlayer viewer = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900020"), "Viewer", world);
        FakePlayer peer = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900021"), "VisiblePeer", world); peer.x = 50;
        MapView map = mapFor(viewer.uuid, world, 2, 0, 0); attach(map);
        List<?> original = (List<?>) field(plugin, "positions");
        try {
            setPositions(plugin, viewer); byte[] baseline = overlayPixels(plugin, map, viewer.player);
            check(nonzero(baseline) > 0, "viewer has own marker");
            setPositions(plugin, viewer, peer); byte[] withPeer = overlayPixels(plugin, map, viewer.player);
            check(!Arrays.equals(baseline, withPeer), "visible online peer is included");
            peer.visible = false; setPositions(plugin, viewer, peer);
            check(Arrays.equals(baseline, overlayPixels(plugin, map, viewer.player)), "visibility permission excludes peer position and name");
            peer.visible = true; viewer.hidden.add(peer.uuid); setPositions(plugin, viewer, peer);
            check(Arrays.equals(baseline, overlayPixels(plugin, map, viewer.player)), "canSee/vanish excludes peer position and name");
            viewer.hidden.clear(); peer.online = false; setPositions(plugin, viewer, peer);
            check(Arrays.equals(baseline, overlayPixels(plugin, map, viewer.player)), "disconnected peer is excluded even from a stale snapshot");
        } finally { setField(plugin, "positions", original); }
    }
    private byte[] overlayPixels(Plugin plugin, MapView map, Player viewer) throws Exception {
        return (byte[]) field(invoke(plugin, "overlay", new Class<?>[]{MapView.class, Player.class}, map, viewer), "pixels");
    }
    private static int nonzero(byte[] values) { int n = 0; for (byte value : values) if (value != 0) n++; return n; }
    private void setPositions(Plugin plugin, FakePlayer... players) throws Exception {
        Class<?> type = Class.forName("com.winkreyes.playermap.PlayerMapPlugin$Position", true, plugin.getClass().getClassLoader());
        Constructor<?> constructor = type.getDeclaredConstructor(Player.class, UUID.class, String.class, double.class, double.class, float.class); constructor.setAccessible(true);
        List<Object> positions = new ArrayList<>();
        for (FakePlayer p : players) positions.add(constructor.newInstance(p.player, p.world.getUID(), p.world.getName(), p.x, p.z, 0F));
        setField(plugin, "positions", positions);
        Field revision = plugin.getClass().getDeclaredField("revision"); revision.setAccessible(true); revision.setLong(plugin, revision.getLong(plugin) + 1);
    }
    private int opaquePixels(MapCanvas canvas) {
        int count = 0; for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) if (canvas.getPixelColor(x, y) != null) count++;
        return count;
    }
    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static final class FakePlayer {
        final UUID uuid; final String name; World world; int selected; double x = 0, z = 0; boolean online = true, use = true, visible = true;
        final Set<UUID> hidden = new HashSet<>();
        final ItemStack[] items = new ItemStack[41]; ItemStack cursor; final List<String> messages = new ArrayList<>();
        final PlayerInventory inventory; final Player player;
        FakePlayer(UUID uuid, String name, World world) {
            this.uuid = uuid; this.name = name; this.world = world;
            inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getContents": return items;
                    case "getItemInMainHand": return items[selected];
                    case "getHeldItemSlot": return selected;
                    case "setItem": items[(int) args[0]] = (ItemStack) args[1]; return null;
                    case "getItemInOffHand": return items[40];
                    case "firstEmpty": for (int i = 0; i < 36; i++) if (items[i] == null) return i; return -1;
                    case "addItem": {
                        Map<Integer, ItemStack> leftovers = new HashMap<>(); ItemStack[] adding = (ItemStack[]) args[0];
                        for (int n = 0; n < adding.length; n++) {
                            int slot = -1; for (int i = 0; i < 36; i++) if (items[i] == null) { slot = i; break; }
                            if (slot >= 0) items[slot] = adding[n]; else leftovers.put(n, adding[n]);
                        }
                        return leftovers;
                    }
                    default: return defaultValue(method.getReturnType());
                }
            });
            player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getUniqueId": return uuid;
                    case "getName": return name;
                    case "getWorld": return this.world;
                    case "getLocation": return new Location(this.world, x, 64, z);
                    case "getInventory": return inventory;
                    case "getItemOnCursor": return cursor;
                    case "isOnline": return online;
                    case "hasPermission": return args[0].toString().equals("wrv.playermap.visible") ? visible : use;
                    case "canSee": return !hidden.contains(((Player) args[0]).getUniqueId());
                    case "equals": return proxy == args[0];
                    case "hashCode": return uuid.hashCode();
                    case "toString": return name;
                    case "sendMessage": if (args != null) messages.add(Arrays.deepToString(args)); return null;
                    default: return defaultValue(method.getReturnType());
                }
            });
        }
        int itemCount() { int n = 0; for (ItemStack item : items) if (item != null) n += item.getAmount(); return n; }
        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive() || type == void.class) return null;
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            return (char) 0;
        }
    }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameters); method.setAccessible(true); return method.invoke(target, arguments);
    }
    private void test(String name, ThrowingRunnable test) {
        try { test.run(); passed++; results.add("PASS " + name); getLogger().info("WRV_HARNESS_PASS " + name); }
        catch (Throwable exception) {
            failed++; results.add("FAIL " + name + ": " + exception); getLogger().severe("WRV_HARNESS_FAIL " + name + ": " + exception);
            exception.printStackTrace();
        }
    }
    private static void check(boolean condition, String details) { assertions++; if (!condition) throw new AssertionError(details); }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
