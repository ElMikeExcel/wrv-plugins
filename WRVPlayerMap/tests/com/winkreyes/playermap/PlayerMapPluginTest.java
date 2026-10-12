package com.winkreyes.playermap;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PlayerMapPluginTest {
    PlayerMapPlugin plugin; Player player; PlayerInventory inventory; World world; MapRegistry registry; MapView map; Server server;
    static final NamespacedKey KEY = new NamespacedKey("wrvplayermap","owner");
    @BeforeEach void setup() throws Exception {
        plugin = mock(PlayerMapPlugin.class,CALLS_REAL_METHODS); server = mock(Server.class); doReturn(server).when(plugin).getServer();
        doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger(); registry = mock(MapRegistry.class); set("registry",registry);
        set("ownerKey",KEY); set("renderers",new HashMap<>()); set("replacements",new HashMap<>()); set("warned",new HashSet<>());
        set("positions",List.of()); set("defaultScale",2); set("cooldownNanos",60_000_000_000L);
        player = mock(Player.class); inventory = mock(PlayerInventory.class); world = mock(World.class);
        when(player.getInventory()).thenReturn(inventory); when(player.getUniqueId()).thenReturn(new UUID(0,1)); when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world,0,70,0)); when(player.hasPermission("wrv.playermap.use")).thenReturn(true);
        when(inventory.getContents()).thenReturn(new ItemStack[41]); when(inventory.firstEmpty()).thenReturn(0); when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        map = mock(MapView.class); when(map.getScale()).thenReturn(MapView.Scale.NORMAL); when(map.getId()).thenReturn(100);
        when(registry.getOrCreate(any(),any(),anyInt(),anyDouble(),anyDouble())).thenReturn(map); when(registry.matches(map)).thenReturn(true);
    }
    private void set(String field,Object value) throws Exception { Field f = PlayerMapPlugin.class.getDeclaredField(field); f.setAccessible(true); f.set(plugin,value); }
    private ItemStack item(UUID owner, MapView view) {
        ItemStack item = mock(ItemStack.class); MapMeta meta = mock(MapMeta.class); PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.FILLED_MAP); when(item.getItemMeta()).thenReturn(meta); when(meta.getMapView()).thenReturn(view);
        when(meta.getPersistentDataContainer()).thenReturn(pdc); when(pdc.has(KEY,PersistentDataType.STRING)).thenReturn(true);
        when(pdc.get(KEY,PersistentDataType.STRING)).thenReturn(owner.toString()); return item;
    }
    private MockedConstruction<ItemStack> captureItems() {
        return mockConstruction(ItemStack.class,(item,context) -> {
            MapMeta meta = mock(MapMeta.class); when(item.getItemMeta()).thenReturn(meta); when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        });
    }
    @Test void repeatedCommandReusesExistingOffhandItemAndDoesNotAddOrDrop() throws Exception {
        ItemStack owned = item(player.getUniqueId(),map); ItemStack[] contents = new ItemStack[41]; contents[40] = owned; when(inventory.getContents()).thenReturn(contents);
        plugin.giveOrUpdate(player,null); plugin.giveOrUpdate(player,null);
        verify(inventory,never()).addItem(any(ItemStack[].class)); verify(inventory,times(2)).setItem(40,owned);
        verify(registry,times(2)).getOrCreate(player.getUniqueId(),world,2,0,0);
    }
    @Test void fullInventoryAndCursorMapCannotAllocateOrDrop() {
        when(inventory.firstEmpty()).thenReturn(-1); plugin.giveOrUpdate(player,null); verifyNoInteractions(registry);
        when(inventory.firstEmpty()).thenReturn(0); ItemStack cursor = item(player.getUniqueId(),map); when(player.getItemOnCursor()).thenReturn(cursor); plugin.giveOrUpdate(player,null);
        verifyNoInteractions(registry); verify(inventory,never()).addItem(any(ItemStack[].class));
    }
    @Test void firstGrantIsOneMapWithPersistentOwnerAndReplacementCooldown() {
        try (var created = captureItems()) {
            plugin.giveOrUpdate(player,null); plugin.giveOrUpdate(player,null);
            assertEquals(1,created.constructed().size()); ItemStack item = created.constructed().getFirst();
            verify(inventory,times(1)).addItem(item); verify(item.getItemMeta().getPersistentDataContainer()).set(KEY,PersistentDataType.STRING,player.getUniqueId().toString());
            verify(item).setItemMeta(any(MapMeta.class));
        }
    }
    @Test void explicitScaleDoesNotChangeExistingMapsInPlace() throws Exception {
        ItemStack owned = item(player.getUniqueId(),map); when(inventory.getContents()).thenReturn(new ItemStack[]{owned}); plugin.giveOrUpdate(player,4);
        verify(registry).getOrCreate(player.getUniqueId(),world,4,0,0); verify(map,never()).setScale(any()); verify(map,never()).setCenterX(anyInt());
    }
    @Test void mainAndOffhandRecognizedButUntaggedMapsIgnored() {
        ItemStack held = item(player.getUniqueId(),map); when(inventory.getItemInMainHand()).thenReturn(held); assertTrue(plugin.isHeld(player,100)); assertFalse(plugin.isHeld(player,101));
        when(inventory.getItemInMainHand()).thenReturn(null); when(inventory.getItemInOffHand()).thenReturn(held); assertTrue(plugin.isHeld(player,100));
        when(inventory.getItemInOffHand()).thenReturn(null); assertFalse(plugin.isHeld(player,100));
    }
    @Test void deniedCommandsAndMalformedScaleCannotCreateMaps() {
        when(player.hasPermission("wrv.playermap.use")).thenReturn(false); plugin.onCommand(player,mock(Command.class),"playermap",new String[0]); verifyNoInteractions(registry);
        when(player.hasPermission("wrv.playermap.use")).thenReturn(true); plugin.onCommand(player,mock(Command.class),"playermap",new String[]{"scale","5"}); verifyNoInteractions(registry);
    }
    @Test void scalePrefersHeldMapIfOldDroppedMapWasRecovered() {
        ItemStack old = item(player.getUniqueId(),map), held = item(player.getUniqueId(),map);
        ItemStack[] contents = new ItemStack[41]; contents[2] = old; contents[7] = held;
        when(inventory.getContents()).thenReturn(contents); when(inventory.getHeldItemSlot()).thenReturn(7);
        plugin.giveOrUpdate(player,3); verify(inventory).setItem(7,held); verify(old,never()).setItemMeta(any());
    }
    @Test void borrowedPersonalMapDoesNotRevealTrackedPlayers() {
        ItemStack borrowed = item(UUID.randomUUID(),map); when(inventory.getItemInMainHand()).thenReturn(borrowed);
        assertFalse(plugin.isHeld(player,100));
    }
    @Test void attachingOverlayNeverRemovesNativeRendererAndIgnoresUnknownIds() {
        plugin.attach(map); plugin.attach(map); verify(map,times(1)).addRenderer(any(PlayerMapRenderer.class)); verify(map,never()).removeRenderer(any());
        MapView unrelated = mock(MapView.class); plugin.attach(unrelated); verify(unrelated,never()).addRenderer(any());
    }
}
