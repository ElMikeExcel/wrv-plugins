package com.winkreyes.playermap;
import io.papermc.paper.event.player.PlayerMapFilledEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PlayerMapPluginTest {
    PlayerMapPlugin plugin; Player player; PlayerInventory inventory; World world; MapRegistry registry; MapView map; Server server;
    static final NamespacedKey KEY = new NamespacedKey("wrvplayermap","lineage_root");
    @BeforeEach void setup() throws Exception {
        plugin = mock(PlayerMapPlugin.class,CALLS_REAL_METHODS); server = mock(Server.class); doReturn(server).when(plugin).getServer();
        doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger(); registry = mock(MapRegistry.class); set("registry",registry);
        set("rootKey",KEY); set("renderers",new HashMap<>()); set("activations",new WeakHashMap<>()); set("warned",new HashSet<>()); set("positions",List.of());
        player = mock(Player.class); inventory = mock(PlayerInventory.class); world = mock(World.class);
        when(player.getInventory()).thenReturn(inventory); when(player.getUniqueId()).thenReturn(new UUID(0,1)); when(player.getWorld()).thenReturn(world);
        when(world.getUID()).thenReturn(new UUID(0,2)); when(player.hasPermission("wrv.playermap.use")).thenReturn(true);
        map = map(100); when(registry.matches(map)).thenReturn(true); when(registry.entry(100)).thenReturn(new MapRegistry.Entry(100,new UUID(0,2),0,0,0,100));
    }
    private void set(String field,Object value) throws Exception { Field f = PlayerMapPlugin.class.getDeclaredField(field); f.setAccessible(true); f.set(plugin,value); }
    private MapView map(int id) {
        MapView result = mock(MapView.class); when(result.getId()).thenReturn(id); when(result.getWorld()).thenReturn(world);
        when(result.getScale()).thenReturn(MapView.Scale.CLOSEST); when(result.getRenderers()).thenReturn(List.of(mock(MapRenderer.class))); return result;
    }
    private ItemStack item(MapView view,Integer root) {
        ItemStack item = mock(ItemStack.class); MapMeta meta = mock(MapMeta.class); PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.FILLED_MAP); when(item.getItemMeta()).thenReturn(meta); when(meta.getMapView()).thenReturn(view);
        when(meta.getPersistentDataContainer()).thenReturn(pdc); when(pdc.get(KEY,PersistentDataType.INTEGER)).thenReturn(root); return item;
    }
    private PlayerMapFilledEvent event(ItemStack created) {
        PlayerMapFilledEvent event = mock(PlayerMapFilledEvent.class); ItemStack original = mock(ItemStack.class); when(original.getType()).thenReturn(Material.MAP);
        when(event.getOriginalItem()).thenReturn(original); when(event.getCreatedMap()).thenReturn(created); when(event.getPlayer()).thenReturn(player); return event;
    }
    @Test void activationTagsTheActualCreatedItemAndNeverCreatesOrIssuesAnother() throws Exception {
        ItemStack created = item(map,null); PlayerMapFilledEvent event = event(created);
        plugin.captureActivation(event); plugin.enhanceActivation(event);
        verify(registry).trackInitial(map); verify(event).setCreatedMap(created);
        verify(created.getItemMeta().getPersistentDataContainer()).set(KEY,PersistentDataType.INTEGER,100);
        verify(map,never()).setScale(any()); verify(map,never()).setCenterX(anyInt()); verify(map,never()).setWorld(any()); verify(map,never()).setTrackingPosition(anyBoolean());
        verify(server,never()).createMap(any()); verify(inventory,never()).addItem(any(ItemStack[].class));
    }
    @Test void broadMapInitializeOnlyRestoresKnownIdsNeverEnrollsOldMaps() throws Exception {
        MapView old = map(5); MapInitializeEvent event = mock(MapInitializeEvent.class); when(event.getMap()).thenReturn(old); plugin.onMapInitialize(event);
        verify(registry,never()).trackInitial(any()); verify(old,never()).addRenderer(any());
    }
    @Test void otherPluginReplacingActivationOutputIsNotClaimed() throws Exception {
        ItemStack initial = item(map,null), replacement = item(map(5),null); PlayerMapFilledEvent event = event(initial);
        plugin.captureActivation(event); when(event.getCreatedMap()).thenReturn(replacement); plugin.enhanceActivation(event);
        verify(registry,never()).trackInitial(any()); verify(event,never()).setCreatedMap(any());
    }
    @Test void lockedCustomAndPermissionDeniedActivationsStayPlain() throws Exception {
        ItemStack created = item(map,null); PlayerMapFilledEvent event = event(created);
        when(map.isLocked()).thenReturn(true); plugin.captureActivation(event); plugin.enhanceActivation(event); verify(registry,never()).trackInitial(any());
        when(map.isLocked()).thenReturn(false); when(map.isVirtual()).thenReturn(true); plugin.captureActivation(event); plugin.enhanceActivation(event); verify(registry,never()).trackInitial(any());
        when(map.isVirtual()).thenReturn(false); when(player.hasPermission("wrv.playermap.use")).thenReturn(false); plugin.captureActivation(event); plugin.enhanceActivation(event); verify(registry,never()).trackInitial(any());
    }
    @Test void mapSharingAndOffhandWorkWithoutOwnerBinding() {
        ItemStack shared = item(map,100); when(inventory.getItemInMainHand()).thenReturn(shared); assertTrue(plugin.isHeld(player,100));
        when(inventory.getItemInMainHand()).thenReturn(null); when(inventory.getItemInOffHand()).thenReturn(shared); assertTrue(plugin.isHeld(player,100));
        when(inventory.getItemInOffHand()).thenReturn(null); assertFalse(plugin.isHeld(player,100));
    }
    @Test void markedZoomDescendantIsEnrolledButOrdinaryUnmarkedMapIsNot() throws Exception {
        MapView zoom = map(101); ItemStack item = item(zoom,100); when(registry.trackZoom(zoom,100)).thenReturn(true);
        assertTrue(plugin.ensureTracked(player,item)); verify(registry).trackZoom(zoom,100);
        clearInvocations(registry); ItemStack plain = item(map(4),null); assertFalse(plugin.ensureTracked(player,plain)); verify(registry,never()).trackZoom(any(),anyInt());
    }
    @Test void lockedDerivedMapCannotBeEnrolledOrRendered() throws Exception {
        MapView locked = map(101); when(locked.isLocked()).thenReturn(true); assertFalse(plugin.ensureTracked(player,item(locked,100)));
        verify(registry,never()).trackZoom(any(),anyInt());
    }
    @Test void repeatedHeldChecksOnlyAttachOneOverlayAndKeepNativeRenderer() {
        ItemStack shared = item(map,100); plugin.ensureTracked(player,shared); plugin.ensureTracked(player,shared);
        verify(map,times(1)).addRenderer(any(PlayerMapRenderer.class)); verify(map,never()).removeRenderer(any());
    }
    @Test void customNamedOrMarkedActivationOutputsAreNotAdopted() throws Exception {
        ItemStack custom = item(map,null); when(custom.getItemMeta().hasDisplayName()).thenReturn(true);
        PlayerMapFilledEvent event = event(custom); plugin.captureActivation(event); plugin.enhanceActivation(event);
        verify(registry,never()).trackInitial(any()); verify(event,never()).setCreatedMap(any());
    }
    @Test void missingOrMismatchedLineageCannotRenderEvenARegisteredId() {
        ItemStack unmarked = item(map,null); when(inventory.getItemInMainHand()).thenReturn(unmarked);
        assertFalse(plugin.isHeld(player,100)); assertFalse(plugin.ensureTracked(player,unmarked));
        ItemStack wrongRoot = item(map,99); when(inventory.getItemInMainHand()).thenReturn(wrongRoot);
        assertFalse(plugin.isHeld(player,100)); assertFalse(plugin.ensureTracked(player,wrongRoot));
    }
    @Test void oldGetAndScaleCommandsNeverCreateOrModifyMaps() {
        plugin.onCommand(player,mock(Command.class),"playermap",new String[]{"get"}); plugin.onCommand(player,mock(Command.class),"playermap",new String[]{"scale","4"});
        verifyNoInteractions(registry); verify(server,never()).createMap(any()); verify(inventory,never()).addItem(any(ItemStack[].class));
    }
}
