// Excel signature - WRV maintained source; original authorship and license notices preserved.
package com.winkreyes.shops;
import com.winkreyes.shops.checkout.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;import java.util.*;
import org.bukkit.block.*;import org.bukkit.event.block.*;

/** Tests actual plugin pre-mutation event entry points, not a live player client. */
class SessionMutationEventTest {
 @TempDir Path dir;
 WRVShops plugin;GlobalSessionGuard guard;CheckoutService checkout;
 @BeforeEach void setup()throws Exception{
  guard=GlobalSessionGuard.open(dir,true);plugin=mock(WRVShops.class);checkout=mock(CheckoutService.class);
  var field=WRVShops.class.getDeclaredField("sessionGuard");field.setAccessible(true);field.set(plugin,guard);
  field=WRVShops.class.getDeclaredField("checkout");field.setAccessible(true);field.set(plugin,checkout);
  doCallRealMethod().when(plugin).beforeShopMutation(anyString());doCallRealMethod().when(plugin).fencePlacement(any());doCallRealMethod().when(plugin).fenceBreak(any());
 }
 @AfterEach void close()throws Exception{guard.closeUnclean();}
 void ready()throws Exception{var f=new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true,"b".repeat(64));var proofs=Map.of("checkout",f,"legacy-checkout",f);guard.assess(proofs,false);guard.recordReady(proofs);}
 @Test void startupPlacementIsCancelledBeforeMutation()throws Exception{
  var e=mock(BlockPlaceEvent.class);var b=mock(Block.class);when(e.getBlockPlaced()).thenReturn(b);when(b.getState()).thenReturn(mock(Chest.class));plugin.fencePlacement(e);verify(e).setCancelled(true);verify(checkout).pauseExternal(anyString());assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));
 }
 @Test void startupBreakIsCancelledBeforeMutation()throws Exception{
  var e=mock(BlockBreakEvent.class);var b=mock(Block.class);when(e.getBlock()).thenReturn(b);when(b.getState()).thenReturn(mock(Sign.class));plugin.fenceBreak(e);verify(e).setCancelled(true);verify(checkout).pauseExternal(anyString());
 }
 @Test void readyPlacementForcesFenceWithoutCancelling()throws Exception{
  ready();var e=mock(BlockPlaceEvent.class);var b=mock(Block.class);when(e.getBlockPlaced()).thenReturn(b);when(b.getState()).thenReturn(mock(Chest.class));plugin.fencePlacement(e);verify(e,never()).setCancelled(true);assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));
 }
 @Test void readyBreakForcesFenceWithoutCancelling()throws Exception{
  ready();var e=mock(BlockBreakEvent.class);var b=mock(Block.class);when(e.getBlock()).thenReturn(b);when(b.getState()).thenReturn(mock(Sign.class));plugin.fenceBreak(e);verify(e,never()).setCancelled(true);assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));
 }
 @Test void externalUncertaintyPersistsBeyondObjectLifetime()throws Exception{ready();guard.markUncertain("third-party ambiguity");guard.closeUnclean();try(var next=GlobalSessionGuard.open(dir,true)){var f=new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true,"b".repeat(64));assertEquals(GlobalSessionGuard.Decision.BLOCKED,next.assess(Map.of("checkout",f,"legacy-checkout",f),true));}}
 void field(String name,Object value)throws Exception{var f=WRVShops.class.getDeclaredField(name);f.setAccessible(true);f.set(plugin,value);}
 @Test void shutdownFencePrecedesPlayerAndWorldSave()throws Exception{
  ready();field("placedSigns",new java.util.HashMap<>());field("sessionProofReady",true);
  var player=mock(org.bukkit.entity.Player.class);var world=mock(org.bukkit.World.class);var server=mock(org.bukkit.Server.class);
  UUID pid=UUID.randomUUID(),wid=UUID.randomUUID();field("dirtyPlayers",new java.util.HashSet<>(Set.of(pid)));field("dirtyWorlds",new java.util.HashSet<>(Set.of(wid)));
  when(plugin.getServer()).thenReturn(server);when(server.getPlayer(pid)).thenReturn(player);when(player.isOnline()).thenReturn(true);when(server.getWorld(wid)).thenReturn(world);
  doAnswer(x->{assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));return null;}).when(player).saveData();
  doAnswer(x->{assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));return null;}).when(world).save();
  doCallRealMethod().when(plugin).onDisable();plugin.onDisable();verify(player).saveData();verify(world).save();assertFalse(Files.exists(dir.resolve("global-session-v2")));
 }
 @Test void shutdownFenceFailurePreventsPersistence()throws Exception{
  field("placedSigns",new java.util.HashMap<>());field("dirtyPlayers",new java.util.HashSet<>());field("dirtyWorlds",new java.util.HashSet<>());
  var registry=mock(com.winkreyes.shops.store.ShopRegistry.class);field("registry",registry);when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
  doCallRealMethod().when(plugin).onDisable();plugin.onDisable();verify(registry,never()).save();verify(checkout).closeUnclean();assertTrue(Files.exists(dir.resolve("global-session-v2.fence")));
 }

}
