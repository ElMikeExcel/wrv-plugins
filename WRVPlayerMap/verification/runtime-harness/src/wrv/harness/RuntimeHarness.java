package wrv.harness;

import org.bukkit.Bukkit;
import io.papermc.paper.event.player.PlayerMapFilledEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.core.component.DataComponents;
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
        test("native-empty-map-activation-preserves-id-and-terrain", this::activationTests);
        test("vanilla-clone-zoom-and-lock-provenance", this::cartographyTests);
        test("tracked-map-restored-after-restart", this::restartTests);
        test("delta-pixels-and-native-terrain-restoration", this::prototypeCanvasTests);
        test("sharing-hands-dimensions-and-frame-id-semantics", this::simulatedPlayerTests);
        test("visibility-and-disconnected-player-filtering", this::visibilityTests);
    }
    private Plugin prototype() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WRVPlayerMap");
        check(plugin != null && plugin.isEnabled(), "prototype must be enabled"); return plugin;
    }
    private Object registry() throws Exception { return field(prototype(), "registry"); }
    private int registrySize() throws Exception { return ((Collection<?>) invoke(registry(), "entries", new Class<?>[]{})).size(); }
    private void attach(MapView map) throws Exception { invoke(prototype(), "attach", new Class<?>[]{MapView.class}, map); }
    private static MapView view(ItemStack item) { return ((MapMeta)item.getItemMeta()).getMapView(); }
    private ItemStack ordinaryMap(World world, int x, int z) {
        return CraftItemStack.asBukkitCopy(MapItem.create(((CraftWorld)world).getHandle(),x,z,(byte)0,true,false));
    }
    private ItemStack activated(FakePlayer player) {
        ItemStack created = ordinaryMap(player.world,(int)player.x,(int)player.z);
        PlayerMapFilledEvent event = new PlayerMapFilledEvent(player.player,new ItemStack(Material.MAP),created);
        Bukkit.getPluginManager().callEvent(event); return event.getCreatedMap();
    }
    private boolean ensure(FakePlayer holder,ItemStack item) throws Exception {
        return (boolean)invoke(prototype(),"ensureTracked",new Class<?>[]{Player.class,ItemStack.class},holder.player,item);
    }
    private ItemStack postProcess(ItemStack source,World world,MapPostProcessing operation) {
        net.minecraft.world.item.ItemStack result = CraftItemStack.asNMSCopy(source).copyWithCount(1);
        result.set(DataComponents.MAP_POST_PROCESSING,operation);
        ((MapItem)Items.FILLED_MAP).onCraftedPostProcess(result,((CraftWorld)world).getHandle());
        return CraftItemStack.asBukkitCopy(result);
    }
    private void checkLayers(MapView map) {
        check(map.getRenderers().size() == 2,"tracked map has native and overlay renderers, id="+map.getId());
        check(map.getRenderers().getFirst().getClass().getName().endsWith("CraftMapRenderer"),"native renderer stays first");
        check(map.getRenderers().get(1).getClass().getName().equals("com.winkreyes.playermap.PlayerMapRenderer"),"second renderer is player overlay");
        check(map.getRenderers().get(1).isContextual() && !map.isVirtual(),"per-viewer overlay keeps native map nonvirtual");
    }
    private FakePlayer person(World world,String name) { return new FakePlayer(UUID.randomUUID(),name,world); }
    private void activationTests() throws Exception {
        World world = Bukkit.getWorlds().getFirst(); FakePlayer owner = person(world,"Activator"); owner.x=-130; owner.z=257;
        ItemStack original = ordinaryMap(world,-130,257); MapView nativeView = view(original);
        int id=nativeView.getId(), x=nativeView.getCenterX(), z=nativeView.getCenterZ(); boolean tracking=nativeView.isTrackingPosition();
        var data=MapItem.getSavedData(CraftItemStack.asNMSCopy(original),((CraftWorld)world).getHandle()); data.colors[123]=(byte)21; data.setDirty();
        PlayerMapFilledEvent event=new PlayerMapFilledEvent(owner.player,new ItemStack(Material.MAP,3),original);
        int before=registrySize(); Bukkit.getPluginManager().callEvent(event); ItemStack enhanced=event.getCreatedMap();
        check(registrySize()==before+1,"only activation enrolls exactly one vanilla ID");
        check(view(enhanced).getId()==id && view(enhanced).getCenterX()==x && view(enhanced).getCenterZ()==z,"same native ID and region");
        check(view(enhanced).getScale()==MapView.Scale.CLOSEST && view(enhanced).getWorld().equals(world),"native scale and world preserved");
        check(view(enhanced).isTrackingPosition()==tracking && data.colors[123]==21,"native flags and terrain history preserved");
        check(enhanced.getAmount()==1 && owner.itemCount()==0,"event replaces only its vanilla result, never grants a second inventory item");
        check(enhanced.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("wrvplayermap","lineage_root"),PersistentDataType.INTEGER)==id,"lineage tag uses native root ID");
        checkLayers(nativeView);
        ItemStack old=ordinaryMap(world,0,0); MapView oldView=view(old); int oldBefore=registrySize();
        check(!ensure(owner,old) && registrySize()==oldBefore && oldView.getRenderers().size()==1,"unmarked old/explorer-style map not enrolled by holding or map initialize");
        FakePlayer denied=person(world,"Denied"); denied.use=false; ItemStack plain=activated(denied);
        check(view(plain).getRenderers().size()==1,"denied activation leaves ordinary map usable and plain");
    }
    private void cartographyTests() throws Exception {
        World world=Bukkit.getWorlds().getFirst(); FakePlayer holder=person(world,"Cartographer"); holder.x=-130; holder.z=257;
        ItemStack initial=activated(holder); int root=view(initial).getId();
        ItemStack clone=CraftItemStack.asBukkitCopy(CraftItemStack.asNMSCopy(initial).copyWithCount(2)); int count=registrySize();
        check(view(clone).getId()==root && clone.getAmount()==2 && ensure(holder,clone) && registrySize()==count,"vanilla clone shares map ID and lineage without extra enrollment");
        ItemStack zoom=initial;
        for(int scale=1;scale<=4;scale++) {
            zoom=postProcess(zoom,world,MapPostProcessing.SCALE);
            check(view(zoom).getId()!=root && view(zoom).getScale().getValue()==scale,"native zoom allocates its own expected ID/scale");
            check(ensure(holder,zoom),"marked native zoom descendant enrolled at scale "+scale); checkLayers(view(zoom));
        }
        ItemStack quick=postProcess(postProcess(initial,world,MapPostProcessing.SCALE),world,MapPostProcessing.SCALE);
        check(ensure(holder,quick),"multiple zooms before a held check preserve root ancestry");
        var nativeData=MapItem.getSavedData(CraftItemStack.asNMSCopy(initial),((CraftWorld)world).getHandle()); nativeData.colors[456]=(byte)33; nativeData.setDirty();
        byte[] terrain=nativeData.colors.clone(); holder.items[0]=initial; setPositions(prototype(),holder);
        MapCanvas canvas=realCanvas(view(initial)); view(initial).getRenderers().get(1).render(view(initial),canvas,holder.player);
        check(opaquePixels(canvas)>0,"root map has a live overlay before locking");
        ItemStack locked=postProcess(initial,world,MapPostProcessing.LOCK); int before=registrySize();
        check(view(locked).getId()!=root && view(locked).isLocked(),"vanilla lock creates a separate locked ID");
        check(!ensure(holder,locked) && registrySize()==before && view(locked).getRenderers().size()==1,"locked copy remains plain despite inherited tag");
        var lockedData=MapItem.getSavedData(CraftItemStack.asNMSCopy(locked),((CraftWorld)world).getHandle());
        check(Arrays.equals(terrain,lockedData.colors),"locked art snapshots only native terrain; labels are not baked in");
        ItemStack old=ordinaryMap(world,0,0), oldZoom=postProcess(old,world,MapPostProcessing.SCALE);
        check(!ensure(holder,oldZoom) && view(oldZoom).getRenderers().size()==1,"zooming existing untracked maps does not opt them in");
        setField(prototype(),"positions",List.of());
    }
    private void restartTests() throws Exception {
        Path state=getDataFolder().toPath().resolve("native-restart-item.bin"); Files.createDirectories(state.getParent());
        FakePlayer holder=person(Bukkit.getWorlds().getFirst(),"RestartHolder");
        if(Files.exists(state)) {
            ItemStack item=ItemStack.deserializeBytes(Files.readAllBytes(state)); MapView map=view(item);
            check(map!=null,"native ID restored from serialized ordinary item"); checkLayers(map);
            int before=registrySize(); check(ensure(holder,item) && registrySize()==before,"reload reuses the recorded ID without allocating/re-enrolling");
            getLogger().info("WRV_HARNESS_RESTART verified map="+map.getId());
        } else {
            ItemStack item=postProcess(activated(holder),holder.world,MapPostProcessing.SCALE); check(ensure(holder,item),"restart fixture tracked");
            Files.write(state,item.serializeAsBytes()); getLogger().info("WRV_HARNESS_RESTART prepared map="+view(item).getId());
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
        MapView map = view(activated(person(Bukkit.getWorlds().getFirst(),"CanvasHolder")));
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
        Plugin plugin=prototype(); World world=Bukkit.getWorlds().getFirst(); FakePlayer owner=person(world,"Owner"), borrower=person(world,"Borrower");
        ItemStack item=activated(owner); MapView map=view(item); int id=map.getId(); borrower.items[0]=item;
        check(ensure(borrower,item) && (boolean)invoke(plugin,"isHeld",new Class<?>[]{Player.class,int.class},borrower.player,id),"shared ordinary map works for another player");
        borrower.items[0]=null; borrower.items[40]=item; check((boolean)invoke(plugin,"isHeld",new Class<?>[]{Player.class,int.class},borrower.player,id),"offhand supported");
        borrower.items[40]=null; borrower.items[10]=item; check(!(boolean)invoke(plugin,"isHeld",new Class<?>[]{Player.class,int.class},borrower.player,id),"stored maps do not activate overlay");
        borrower.items[10]=null; borrower.items[0]=item; setPositions(plugin,borrower);
        MapCanvas canvas=realCanvas(map); MapRenderer renderer=map.getRenderers().get(1); renderer.render(map,canvas,borrower.player);
        check(opaquePixels(canvas)>0,"shared permitted holder gets own contextual pixels");
        MapView frameCopy=view(item.clone()); check(frameCopy.getId()==id && frameCopy==map,"held/frame clones resolve one shared map view, not independent display identities");
        borrower.items[0]=null; renderer.render(map,canvas,borrower.player); check(opaquePixels(canvas)==0,"unheld map clears tracking layer");
        borrower.items[0]=item; renderer.render(map,canvas,borrower.player); borrower.use=false; renderer.render(map,canvas,borrower.player);
        check(opaquePixels(canvas)==0,"permission removal clears prior pixels immediately"); borrower.use=true;
        World other=Bukkit.getWorlds().stream().filter(w->!w.getUID().equals(world.getUID())).findFirst().orElseThrow(); borrower.world=other;
        check(ensure(borrower,item) && view(item).getId()==id && view(item).getWorld().equals(world),"changing dimensions never swaps the ordinary map ID/world");
        check(nonzero(overlayPixels(plugin,map,borrower.player))==0,"other-dimension holder sees native terrain without misleading player overlay");
        borrower.world=world; check(nonzero(overlayPixels(plugin,map,borrower.player))>0,"returning to map world restores labels");
        setField(plugin,"positions",List.of());
    }
    private void visibilityTests() throws Exception {
        Plugin plugin = prototype(); World world = Bukkit.getWorlds().getFirst();
        FakePlayer viewer = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900020"), "Viewer", world);
        FakePlayer peer = new FakePlayer(UUID.fromString("3c9b7650-5061-4ce5-a4d0-f90ac7900021"), "VisiblePeer", world); peer.x = 50;
        MapView map = view(activated(viewer));
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
